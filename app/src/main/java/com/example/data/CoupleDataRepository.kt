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

    private val chatRepo: ChatRepository by lazy { ChatRepository(context) }

    fun getCoupleDocId(uid1: String, uid2: String): String {
        return chatRepo.getCoupleId(uid1, uid2)
    }

    // ==========================================
    // 1. LIVE REALTIME CHAT (Firestore + Realtime DB)
    // ==========================================

    fun observeChatMessages(coupleId: String): Flow<List<ChatMessage>> {
        return chatRepo.observeMessages(coupleId)
    }

    fun observePartnerTyping(coupleId: String, partnerId: String): Flow<Boolean> {
        return chatRepo.observePartnerTyping(coupleId, partnerId)
    }

    suspend fun setTypingStatus(coupleId: String, userId: String, isTyping: Boolean) {
        chatRepo.setTypingStatus(coupleId, userId, isTyping)
    }

    suspend fun sendChatMessage(coupleId: String, message: ChatMessage) {
        chatRepo.sendMessage(coupleId, message)
    }

    suspend fun markMessagesAsRead(coupleId: String, currentUserId: String) {
        chatRepo.markMessagesAsRead(coupleId, currentUserId)
    }

    suspend fun deleteChatMessage(coupleId: String, messageId: String) {
        chatRepo.deleteMessage(coupleId, messageId)
    }

    suspend fun editChatMessage(coupleId: String, messageId: String, newText: String) {
        chatRepo.editMessage(coupleId, messageId, newText)
    }

    suspend fun reactToChatMessage(coupleId: String, messageId: String, emoji: String?) {
        chatRepo.reactToMessage(coupleId, messageId, emoji)
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
