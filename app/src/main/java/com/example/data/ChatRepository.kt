package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.ChatMessage
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository(private val context: Context) {
    private val TAG = "ChatRepository"
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    // Firebase Realtime Database with multi-region fallback
    private val realtimeDb: FirebaseDatabase by lazy {
        val possibleUrls = listOf(
            "https://ikimiz-7306c-default-rtdb.europe-west1.firebasedatabase.app",
            "https://ikimiz-7306c-europe-west1.firebasedatabase.app",
            "https://ikimiz-7306c-default-rtdb.firebaseio.com"
        )
        var instance: FirebaseDatabase? = null
        for (url in possibleUrls) {
            try {
                instance = FirebaseDatabase.getInstance(url)
                break
            } catch (e: Exception) {
                Log.w(TAG, "Could not initialize RTDB with URL $url: ${e.message}")
            }
        }
        instance ?: FirebaseDatabase.getInstance()
    }

    /**
     * Symmetrical coupleId generation: Always sorts uid1 and uid2 alphabetically
     * ensuring both partners connect to the EXACT SAME path: /chats/{coupleId}/...
     */
    fun getCoupleId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }

    // ==============================================================
    // 1. REALTIME MESSAGE FLOW (/chats/{coupleId}/messages)
    // ==============================================================

    fun observeMessages(coupleId: String): Flow<List<ChatMessage>> = callbackFlow {
        if (coupleId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val inMemoryMap = mutableMapOf<String, ChatMessage>()

        fun emitSortedList() {
            val sortedList = inMemoryMap.values.sortedBy { it.timestamp }
            trySend(sortedList)
        }

        // 1. Primary Live Listener: Firebase Realtime Database (/chats/{coupleId}/messages)
        val rtdbRef = try {
            realtimeDb.reference.child("chats").child(coupleId).child("messages")
        } catch (e: Exception) {
            Log.e(TAG, "RTDB reference error", e)
            null
        }

        val rtdbListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    if (inMemoryMap.isEmpty()) trySend(emptyList())
                    return
                }
                for (child in snapshot.children) {
                    val map = child.value as? Map<*, *>
                    if (map != null) {
                        @Suppress("UNCHECKED_CAST")
                        val strMap = map as Map<String, Any?>
                        val msgId = (strMap["id"] as? String)?.takeIf { it.isNotBlank() } ?: (child.key ?: "")
                        val msg = ChatMessage.fromMap(strMap).copy(id = msgId)
                        if (msgId.isNotBlank()) {
                            inMemoryMap[msgId] = msg
                        }
                    }
                }
                emitSortedList()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "RTDB Messages listener cancelled: ${error.message}")
            }
        }

        rtdbRef?.addValueEventListener(rtdbListener)

        // 2. Secondary Synchronizer: Firestore Collection /couples/{coupleId}/messages
        val firestoreListener = firestore.collection("couples")
            .document(coupleId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Firestore Chat listener error: ${error.message}")
                    return@addSnapshotListener
                }
                snapshot?.documents?.forEach { doc ->
                    doc.data?.let { data ->
                        val msgId = (data["id"] as? String)?.takeIf { it.isNotBlank() } ?: doc.id
                        val msg = ChatMessage.fromMap(data).copy(id = msgId)
                        if (msgId.isNotBlank()) {
                            inMemoryMap[msgId] = msg
                        }
                    }
                }
                emitSortedList()
            }

        awaitClose {
            rtdbRef?.removeEventListener(rtdbListener)
            firestoreListener.remove()
        }
    }

    // ==============================================================
    // 2. REALTIME TYPING INDICATOR (/chats/{coupleId}/typing/{userId})
    // ==============================================================

    fun observePartnerTyping(coupleId: String, partnerId: String): Flow<Boolean> = callbackFlow {
        if (coupleId.isBlank() || partnerId.isBlank()) {
            trySend(false)
            awaitClose { }
            return@callbackFlow
        }

        val typingRef = try {
            realtimeDb.reference.child("chats").child(coupleId).child("typing").child(partnerId)
        } catch (e: Exception) {
            Log.e(TAG, "RTDB Typing ref error", e)
            null
        }

        val typingListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isTyping = when (val v = snapshot.value) {
                    is Boolean -> v
                    is String -> v.equals("true", ignoreCase = true) || v == "1"
                    is Number -> v.toInt() == 1
                    else -> false
                }
                trySend(isTyping)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "RTDB Typing listener cancelled: ${error.message}")
            }
        }

        typingRef?.addValueEventListener(typingListener)

        awaitClose {
            typingRef?.removeEventListener(typingListener)
        }
    }

    suspend fun setTypingStatus(coupleId: String, userId: String, isTyping: Boolean) {
        if (coupleId.isBlank() || userId.isBlank()) return
        try {
            realtimeDb.reference
                .child("chats")
                .child(coupleId)
                .child("typing")
                .child(userId)
                .setValue(isTyping)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting typing status for $userId: ${e.message}")
        }
    }

    // ==============================================================
    // 3. SEND, EDIT, DELETE & READ STATUS ACTIONS
    // ==============================================================

    suspend fun sendMessage(coupleId: String, message: ChatMessage) {
        if (coupleId.isBlank() || message.id.isBlank()) return
        try {
            val msgMap = message.toMap()

            // 1. Realtime Database immediate push
            realtimeDb.reference
                .child("chats")
                .child(coupleId)
                .child("messages")
                .child(message.id)
                .setValue(msgMap)
                .await()

            // 2. Firestore persistent mirror
            firestore.collection("couples")
                .document(coupleId)
                .collection("messages")
                .document(message.id)
                .set(msgMap, SetOptions.merge())
                .await()

            // Reset typing status on send
            setTypingStatus(coupleId, message.senderId, false)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending chat message", e)
        }
    }

    suspend fun markMessagesAsRead(coupleId: String, currentUserId: String) {
        if (coupleId.isBlank() || currentUserId.isBlank()) return
        try {
            val rtdbRef = realtimeDb.reference.child("chats").child(coupleId).child("messages")
            val snapshot = rtdbRef.get().await()

            val updates = mutableMapOf<String, Any?>()
            for (child in snapshot.children) {
                val map = child.value as? Map<*, *>
                if (map != null) {
                    val receiverId = map["receiverId"] as? String
                    val senderId = map["senderId"] as? String
                    val isRead = map["isRead"] as? Boolean ?: false

                    // If message is meant for current user and is not yet read
                    if ((receiverId == currentUserId || (receiverId == null && senderId != currentUserId)) && !isRead) {
                        val key = child.key ?: continue
                        updates["$key/isRead"] = true
                    }
                }
            }

            if (updates.isNotEmpty()) {
                rtdbRef.updateChildren(updates).await()
            }

            // Also update in Firestore in background
            val unreadDocs = firestore.collection("couples")
                .document(coupleId)
                .collection("messages")
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("isRead", false)
                .get()
                .await()

            for (doc in unreadDocs.documents) {
                doc.reference.update("isRead", true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error marking messages as read: ${e.message}")
        }
    }

    suspend fun deleteMessage(coupleId: String, messageId: String) {
        if (coupleId.isBlank() || messageId.isBlank()) return
        try {
            val updates: Map<String, Any?> = mapOf(
                "isDeleted" to true,
                "deleted" to true,
                "text" to "",
                "messageText" to "",
                "imageUrl" to null,
                "reactionEmoji" to null
            )

            realtimeDb.reference
                .child("chats")
                .child(coupleId)
                .child("messages")
                .child(messageId)
                .updateChildren(updates)
                .await()

            firestore.collection("couples")
                .document(coupleId)
                .collection("messages")
                .document(messageId)
                .set(updates, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting chat message", e)
        }
    }

    suspend fun editMessage(coupleId: String, messageId: String, newText: String) {
        if (coupleId.isBlank() || messageId.isBlank()) return
        try {
            val updates = mapOf(
                "text" to newText,
                "messageText" to newText,
                "isEdited" to true
            )

            realtimeDb.reference
                .child("chats")
                .child(coupleId)
                .child("messages")
                .child(messageId)
                .updateChildren(updates)
                .await()

            firestore.collection("couples")
                .document(coupleId)
                .collection("messages")
                .document(messageId)
                .set(updates, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error editing chat message", e)
        }
    }

    suspend fun reactToMessage(coupleId: String, messageId: String, emoji: String?) {
        if (coupleId.isBlank() || messageId.isBlank()) return
        try {
            realtimeDb.reference
                .child("chats")
                .child(coupleId)
                .child("messages")
                .child(messageId)
                .child("reactionEmoji")
                .setValue(emoji)
                .await()

            firestore.collection("couples")
                .document(coupleId)
                .collection("messages")
                .document(messageId)
                .set(mapOf<String, Any?>("reactionEmoji" to emoji), SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting reaction emoji", e)
        }
    }
}
