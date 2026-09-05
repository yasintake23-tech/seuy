package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.ChatMessage
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository(private val context: Context) {
    private val tag = "ChatRepository"
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private val realtimeDb by lazy {
        FirebaseDatabase.getInstance("https://ikimiz-7306c-default-rtdb.europe-west1.firebasedatabase.app")
    }

    fun getCoupleId(uid1: String, uid2: String): String =
        if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"

    /**
     * Only the newest [limit] messages are observed continuously.
     * Older messages are fetched on demand with [loadOlderMessages].
     */
    fun observeMessages(coupleId: String, limit: Int = 30): Flow<List<ChatMessage>> = callbackFlow {
        if (coupleId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val deletedIds = mutableSetOf<String>()
        fun normalize(messages: List<ChatMessage>): List<ChatMessage> =
            messages.distinctBy { it.id }.map { message ->
                if (message.isDeleted || deletedIds.contains(message.id)) {
                    message.copy(
                        isDeleted = true,
                        text = "",
                        mediaUrl = null,
                        imageUrl = null,
                        isPhoto = false,
                        reactionEmoji = null
                    )
                } else message
            }.sortedBy { it.timestamp }

        val deletedRegistration = firestore.collection("couples")
            .document(coupleId)
            .collection("deleted_ids")
            .addSnapshotListener { snapshot, _ ->
                deletedIds.clear()
                snapshot?.documents?.forEach { deletedIds.add(it.id) }
            }

        val registration = firestore.collection("couples")
            .document(coupleId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .limitToLast(limit.coerceIn(10, 100))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "Recent message listener error: ${error.message}")
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { ChatMessage.fromMap(it).copy(id = (it["id"] as? String) ?: doc.id) }
                } ?: emptyList()
                trySend(normalize(list))
            }

        awaitClose {
            registration.remove()
            deletedRegistration.remove()
        }
    }

    suspend fun loadOlderMessages(
        coupleId: String,
        beforeTimestamp: Long,
        limit: Int = 30
    ): Result<List<ChatMessage>> = runCatching {
        if (coupleId.isBlank()) return@runCatching emptyList()
        val snapshot = firestore.collection("couples")
            .document(coupleId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .startAfter(beforeTimestamp)
            .limit(limit.coerceIn(10, 60))
            .get()
            .await()

        snapshot.documents.mapNotNull { doc ->
            doc.data?.let { ChatMessage.fromMap(it).copy(id = (it["id"] as? String) ?: doc.id) }
        }.sortedBy { it.timestamp }
    }.onFailure {
        Log.e(tag, "Could not load older messages", it)
    }

    fun observePartnerTyping(coupleId: String, partnerId: String): Flow<Boolean> = callbackFlow {
        if (coupleId.isBlank() || partnerId.isBlank()) {
            trySend(false)
            awaitClose { }
            return@callbackFlow
        }
        val ref = realtimeDb.reference.child("chats").child(coupleId).child("typing").child(partnerId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(
                    when (val value = snapshot.value) {
                        is Boolean -> value
                        is String -> value.equals("true", ignoreCase = true)
                        is Number -> value.toInt() == 1
                        else -> false
                    }
                )
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w(tag, "Typing listener cancelled: ${error.message}")
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun setTypingStatus(coupleId: String, userId: String, isTyping: Boolean) {
        if (coupleId.isBlank() || userId.isBlank()) return
        runCatching {
            realtimeDb.reference.child("chats").child(coupleId)
                .child("typing").child(userId).setValue(isTyping).await()
        }.onFailure { Log.w(tag, "Typing write failed: ${it.message}") }
    }

    suspend fun sendMessage(coupleId: String, message: ChatMessage) {
        if (coupleId.isBlank() || message.id.isBlank()) return
        runCatching {
            val map = message.toMap()
            // Firestore is the canonical notification trigger source.
            firestore.collection("couples").document(coupleId)
                .collection("messages").document(message.id)
                .set(map, SetOptions.merge()).await()

            // RTDB remains a low-latency mirror for typing / existing clients.
            realtimeDb.reference.child("chats").child(coupleId)
                .child("messages").child(message.id).setValue(map).await()

            setTypingStatus(coupleId, message.senderId, false)
        }.onFailure { Log.e(tag, "Send message failed", it) }
    }

    suspend fun markMessagesAsRead(coupleId: String, currentUserId: String) {
        if (coupleId.isBlank() || currentUserId.isBlank()) return
        runCatching {
            val unread = firestore.collection("couples").document(coupleId)
                .collection("messages")
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("isRead", false)
                .limit(100)
                .get().await()

            for (doc in unread.documents) {
                doc.reference.update("isRead", true).await()
                realtimeDb.reference.child("chats").child(coupleId)
                    .child("messages").child(doc.id).child("isRead").setValue(true).await()
            }
        }.onFailure { Log.w(tag, "Mark read failed: ${it.message}") }
    }

    suspend fun deleteMessage(coupleId: String, messageId: String) {
        if (coupleId.isBlank() || messageId.isBlank()) return
        val updates = mapOf<String, Any?>(
            "isDeleted" to true,
            "deleted" to true,
            "text" to "",
            "messageText" to "",
            "isPhoto" to false,
            "mediaUrl" to "",
            "imageUrl" to "",
            "reactionEmoji" to null
        )
        runCatching {
            firestore.collection("couples").document(coupleId).collection("messages")
                .document(messageId).set(updates, SetOptions.merge()).await()
            firestore.collection("couples").document(coupleId).collection("deleted_ids")
                .document(messageId).set(mapOf("deletedAt" to System.currentTimeMillis())).await()

            realtimeDb.reference.child("chats").child(coupleId).child("messages")
                .child(messageId).updateChildren(updates).await()
            realtimeDb.reference.child("chats").child(coupleId).child("deleted_ids")
                .child(messageId).setValue(true).await()
        }.onFailure { Log.w(tag, "Delete message failed: ${it.message}") }
    }

    suspend fun editMessage(coupleId: String, messageId: String, newText: String) {
        if (coupleId.isBlank() || messageId.isBlank()) return
        val updates = mapOf<String, Any?>("text" to newText, "messageText" to newText, "isEdited" to true)
        runCatching {
            firestore.collection("couples").document(coupleId).collection("messages")
                .document(messageId).set(updates, SetOptions.merge()).await()
            realtimeDb.reference.child("chats").child(coupleId).child("messages")
                .child(messageId).updateChildren(updates).await()
        }.onFailure { Log.w(tag, "Edit message failed: ${it.message}") }
    }

    suspend fun reactToMessage(coupleId: String, messageId: String, emoji: String?) {
        if (coupleId.isBlank() || messageId.isBlank()) return
        runCatching {
            firestore.collection("couples").document(coupleId).collection("messages")
                .document(messageId).set(mapOf<String, Any?>("reactionEmoji" to emoji), SetOptions.merge()).await()
            realtimeDb.reference.child("chats").child(coupleId).child("messages")
                .child(messageId).child("reactionEmoji").setValue(emoji).await()
        }.onFailure { Log.w(tag, "Reaction write failed: ${it.message}") }
    }
}
