package com.example.util

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.UUID

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM token: $token")
        saveTokenToUserDocument(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}, data: ${remoteMessage.data}")

        val data = remoteMessage.data
        val senderName = data["senderName"]
            ?: data["sender_name"]
            ?: remoteMessage.notification?.title
            ?: "Sevgilin"
        val messageText = data["text"]
            ?: data["messageText"]
            ?: remoteMessage.notification?.body
            ?: "Sana yeni bir mesaj gönderdi!"
        val messageId = data["id"]
            ?: data["messageId"]
            ?: UUID.randomUUID().toString()
        val imageUrl = data["mediaUrl"] ?: data["imageUrl"]

        NotificationHelper.showChatNotification(
            context = applicationContext,
            senderName = senderName,
            messageText = messageText,
            messageId = messageId,
            imageUrl = imageUrl
        )
    }

    private fun saveTokenToUserDocument(token: String) {
        try {
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            if (!currentUid.isNullOrBlank()) {
                val db = FirebaseFirestore.getInstance()
                db.collection("users").document(currentUid).set(
                    mapOf(
                        "fcmToken" to token,
                        "fcmTokenUpdatedAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving FCM token to Firestore", e)
        }
    }

    companion object {
        private const val TAG = "MyFCMService"
    }
}
