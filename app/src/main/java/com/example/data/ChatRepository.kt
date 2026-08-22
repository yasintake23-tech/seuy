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
        val deletedIdsSet = mutableSetOf<String>()

        fun emitSortedList() {
            val list = inMemoryMap.values.map { msg ->
                if (deletedIdsSet.contains(msg.id) || msg.isDeleted) {
                    msg.copy(
                        isDeleted = true,
                        text = "",
                        mediaUrl = null,
                        imageUrl = null,
                        isPhoto = false,
                        reactionEmoji = null
                    )
                } else {
                    msg
                }
            }.sortedBy { it.timestamp }
            trySend(list)
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

        // 1.1 Realtime Deleted IDs Listener (/chats/{coupleId}/deleted_ids)
        val rtdbDeletedRef = try {
            realtimeDb.reference.child("chats").child(coupleId).child("deleted_ids")
        } catch (e: Exception) {
            null
        }

        val rtdbDeletedListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        val key = child.key
                        if (!key.isNullOrBlank()) {
                            deletedIdsSet.add(key)
                        }
                    }
                    emitSortedList()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        rtdbDeletedRef?.addValueEventListener(rtdbDeletedListener)

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

        // 2.1 Secondary Deleted IDs from Firestore (/couples/{coupleId}/deleted_ids)
        val firestoreDeletedListener = firestore.collection("couples")
            .document(coupleId)
            .collection("deleted_ids")
            .addSnapshotListener { snapshot, _ ->
                snapshot?.documents?.forEach { doc ->
                    deletedIdsSet.add(doc.id)
                }
                emitSortedList()
            }

        awaitClose {
            rtdbRef?.removeEventListener(rtdbListener)
            rtdbDeletedRef?.removeEventListener(rtdbDeletedListener)
            firestoreListener.remove()
            firestoreDeletedListener.remove()
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
                "isPhoto" to false,
                "mediaUrl" to "",
                "imageUrl" to "",
                "reactionEmoji" to null
            )

            // 1. Update RTDB message node
            try {
                realtimeDb.reference
                    .child("chats")
                    .child(coupleId)
                    .child("messages")
                    .child(messageId)
                    .updateChildren(updates)
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "RTDB delete message update error: ${e.message}")
            }

            // 2. Mark in RTDB deleted_ids
            try {
                realtimeDb.reference
                    .child("chats")
                    .child(coupleId)
                    .child("deleted_ids")
                    .child(messageId)
                    .setValue(true)
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "RTDB deleted_ids write error: ${e.message}")
            }

            // 3. Update Firestore message
            try {
                firestore.collection("couples")
                    .document(coupleId)
                    .collection("messages")
                    .document(messageId)
                    .set(
                        mapOf(
                            "isDeleted" to true,
                            "deleted" to true,
                            "text" to "",
                            "messageText" to "",
                            "isPhoto" to false,
                            "reactionEmoji" to null
                        ),
                        SetOptions.merge()
                    )
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore delete message update error: ${e.message}")
            }

            // 4. Mark in Firestore deleted_ids
            try {
                firestore.collection("couples")
                    .document(coupleId)
                    .collection("deleted_ids")
                    .document(messageId)
                    .set(
                        mapOf(
                            "deletedAt" to System.currentTimeMillis()
                        )
                    )
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore deleted_ids write error: ${e.message}")
            }
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
