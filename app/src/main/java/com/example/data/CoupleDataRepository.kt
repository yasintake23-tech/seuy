package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.BucketItem
import com.example.model.ChatMessage
import com.example.model.CoupleMemory
import com.example.model.DailyCoupleQuestion
import com.example.model.MemoryPin
import com.example.model.PartnerStatus
import com.example.model.SecretLoveNote
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

class CoupleDataRepository(private val context: Context) {
    private val TAG = "CoupleDataRepo"
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    // Firebase Realtime Database (europe-west1 region)
    private val realtimeDb: FirebaseDatabase by lazy {
        val possibleUrls = listOf(
            "https://ikimiz-7306c-default-rtdb.europe-west1.firebasedatabase.app",
            "https://ikimiz-7306c-europe-west1.firebasedatabase.app"
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

    fun getCoupleDocId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }

    // ==========================================
    // 1. LIVE REALTIME CHAT (Firestore + Realtime DB)
    // ==========================================

    fun observeChatMessages(coupleId: String): Flow<List<ChatMessage>> = callbackFlow {
        if (coupleId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        // Primary live listener via Firestore collection /couples/{coupleId}/messages
        val firestoreListener = firestore.collection("couples")
            .document(coupleId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Firestore Chat listener error: ${error.message}")
                    return@addSnapshotListener
                }
                val messagesList = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { data ->
                        ChatMessage.fromMap(data).copy(
                            id = (data["id"] as? String)?.takeIf { it.isNotBlank() } ?: doc.id
                        )
                    }
                } ?: emptyList()
                trySend(messagesList)
            }

        // Secondary fallback listener via Realtime Database
        val chatRef = try {
            realtimeDb.reference.child("chats").child(coupleId).child("messages")
        } catch (e: Exception) {
            null
        }

        val rtdbListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                val messagesList = mutableListOf<ChatMessage>()
                for (child in snapshot.children) {
                    val map = child.value as? Map<*, *>
                    if (map != null) {
                        @Suppress("UNCHECKED_CAST")
                        val strMap = map as Map<String, Any?>
                        val msg = ChatMessage.fromMap(strMap).copy(
                            id = (strMap["id"] as? String)?.takeIf { it.isNotBlank() } ?: (child.key ?: "")
                        )
                        messagesList.add(msg)
                    }
                }
                if (messagesList.isNotEmpty()) {
                    messagesList.sortBy { it.timestamp }
                    trySend(messagesList)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "RTDB Chat listener error: ${error.message}")
            }
        }

        chatRef?.addValueEventListener(rtdbListener)

        awaitClose {
            firestoreListener.remove()
            chatRef?.removeEventListener(rtdbListener)
        }
    }

    suspend fun sendChatMessage(coupleId: String, message: ChatMessage) {
        if (coupleId.isBlank()) return
        try {
            // Save to Firestore
            firestore.collection("couples")
                .document(coupleId)
                .collection("messages")
                .document(message.id)
                .set(message.toMap(), SetOptions.merge())
                .await()

            // Also mirror to Realtime Database
            realtimeDb.reference
                .child("chats")
                .child(coupleId)
                .child("messages")
                .child(message.id)
                .setValue(message.toMap())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error pushing chat message", e)
        }
    }

    suspend fun deleteChatMessage(coupleId: String, messageId: String) {
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

            // Update in Firestore
            firestore.collection("couples")
                .document(coupleId)
                .collection("messages")
                .document(messageId)
                .set(updates, SetOptions.merge())
                .await()

            // Update in Realtime Database
            realtimeDb.reference
                .child("chats")
                .child(coupleId)
                .child("messages")
                .child(messageId)
                .updateChildren(updates)
                .await()

            Log.d(TAG, "Successfully marked chat message $messageId as deleted in Firestore and RTDB")
        } catch (e: Exception) {
            Log.e(TAG, "Error marking chat message as deleted", e)
        }
    }

    suspend fun editChatMessage(coupleId: String, messageId: String, newText: String) {
        if (coupleId.isBlank() || messageId.isBlank()) return
        try {
            val updates = mapOf(
                "text" to newText,
                "messageText" to newText,
                "isEdited" to true
            )
            firestore.collection("couples")
                .document(coupleId)
                .collection("messages")
                .document(messageId)
                .set(updates, SetOptions.merge())
                .await()

            realtimeDb.reference
                .child("chats")
                .child(coupleId)
                .child("messages")
                .child(messageId)
                .updateChildren(updates)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error editing chat message", e)
        }
    }

    suspend fun reactToChatMessage(coupleId: String, messageId: String, emoji: String?) {
        if (coupleId.isBlank() || messageId.isBlank()) return
        try {
            val updates = mapOf<String, Any?>("reactionEmoji" to emoji)
            firestore.collection("couples")
                .document(coupleId)
                .collection("messages")
                .document(messageId)
                .set(updates, SetOptions.merge())
                .await()

            realtimeDb.reference
                .child("chats")
                .child(coupleId)
                .child("messages")
                .child(messageId)
                .child("reactionEmoji")
                .setValue(emoji)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting reaction emoji", e)
        }
    }

    // ==========================================
    // 2. LIVE BUCKET LIST (Firestore /couples/{coupleId}/bucket_items)
    // ==========================================

    fun observeBucketList(coupleId: String): Flow<List<BucketItem>> = callbackFlow {
        if (coupleId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val listener = firestore.collection("couples")
            .document(coupleId)
            .collection("bucket_items")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Bucket list listener error", error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { BucketItem.fromMap(it) }
                } ?: emptyList()
                trySend(items)
            }

        awaitClose { listener.remove() }
    }

    suspend fun saveBucketItem(coupleId: String, item: BucketItem) {
        if (coupleId.isBlank()) return
        try {
            firestore.collection("couples")
                .document(coupleId)
                .collection("bucket_items")
                .document(item.id)
                .set(item.toMap(), SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving bucket item", e)
        }
    }

    // ==========================================
    // 3. LIVE SECRET LOVE NOTES (Firestore /couples/{coupleId}/secret_notes)
    // ==========================================

    fun observeSecretNotes(coupleId: String): Flow<List<SecretLoveNote>> = callbackFlow {
        if (coupleId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val listener = firestore.collection("couples")
            .document(coupleId)
            .collection("secret_notes")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Secret notes listener error", error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { SecretLoveNote.fromMap(it) }
                } ?: emptyList()
                trySend(items)
            }

        awaitClose { listener.remove() }
    }

    suspend fun saveSecretNote(coupleId: String, note: SecretLoveNote) {
        if (coupleId.isBlank()) return
        try {
            firestore.collection("couples")
                .document(coupleId)
                .collection("secret_notes")
                .document(note.id)
                .set(note.toMap(), SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving secret note", e)
        }
    }

    // ==========================================
    // 4. LIVE MEMORY PINS & MAP (Firestore /couples/{coupleId}/memory_pins)
    // ==========================================

    fun observeMemoryPins(coupleId: String): Flow<List<MemoryPin>> = callbackFlow {
        if (coupleId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val listener = firestore.collection("couples")
            .document(coupleId)
            .collection("memory_pins")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Memory pins listener error", error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { MemoryPin.fromMap(it) }
                } ?: emptyList()
                trySend(items)
            }

        awaitClose { listener.remove() }
    }

    suspend fun saveMemoryPin(coupleId: String, pin: MemoryPin) {
        if (coupleId.isBlank()) return
        try {
            firestore.collection("couples")
                .document(coupleId)
                .collection("memory_pins")
                .document(pin.id)
                .set(pin.toMap(), SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving memory pin", e)
        }
    }

    // ==========================================
    // 5. LIVE COUPLE MEMORIES / GALLERY (Firestore /couples/{coupleId}/memories)
    // ==========================================

    fun observeCoupleMemories(coupleId: String): Flow<List<CoupleMemory>> = callbackFlow {
        if (coupleId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val listener = firestore.collection("couples")
            .document(coupleId)
            .collection("memories")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Memories listener error", error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { CoupleMemory.fromMap(it) }
                } ?: emptyList()
                trySend(items)
            }

        awaitClose { listener.remove() }
    }

    suspend fun saveCoupleMemory(coupleId: String, memory: CoupleMemory) {
        if (coupleId.isBlank()) return
        try {
            firestore.collection("couples")
                .document(coupleId)
                .collection("memories")
                .document(memory.id)
                .set(memory.toMap(), SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving memory", e)
        }
    }

    // ==========================================
    // 6. LIVE STATUS & LOCATION (/couples/{coupleId}/statuses/{userId})
    // ==========================================

    fun observePartnerStatus(coupleId: String, partnerUserId: String): Flow<PartnerStatus?> = callbackFlow {
        if (coupleId.isBlank() || partnerUserId.isBlank()) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }

        val listener = firestore.collection("couples")
            .document(coupleId)
            .collection("statuses")
            .document(partnerUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val status = snapshot.data?.let { PartnerStatus.fromMap(it) }
                trySend(status)
            }

        awaitClose { listener.remove() }
    }

    suspend fun updateMyStatus(coupleId: String, userId: String, status: PartnerStatus) {
        if (coupleId.isBlank() || userId.isBlank()) return
        try {
            firestore.collection("couples")
                .document(coupleId)
                .collection("statuses")
                .document(userId)
                .set(status.toMap(), SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating status", e)
        }
    }

    // ==========================================
    // 7. DAILY QUESTIONS (/couples/{coupleId}/daily_questions/{dateKey})
    // ==========================================

    fun observeDailyQuestions(coupleId: String): Flow<List<DailyCoupleQuestion>> = callbackFlow {
        if (coupleId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val listener = firestore.collection("couples")
            .document(coupleId)
            .collection("daily_questions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Daily questions error", error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { DailyCoupleQuestion.fromMap(it) }
                } ?: emptyList()
                trySend(items)
            }

        awaitClose { listener.remove() }
    }

    suspend fun saveDailyQuestionAnswer(coupleId: String, question: DailyCoupleQuestion) {
        if (coupleId.isBlank()) return
        try {
            firestore.collection("couples")
                .document(coupleId)
                .collection("daily_questions")
                .document(question.id)
                .set(question.toMap(), SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving daily question answer", e)
        }
    }
}
