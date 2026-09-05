package com.example.util

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onCreate() {
        super.onCreate()
        syncCurrentToken()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        saveTokenToUserDocument(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data
        val senderName = data["senderName"] ?: remoteMessage.notification?.title ?: "Sevgilin"
        val messageText = data["text"] ?: remoteMessage.notification?.body ?: "Sana yeni bir mesaj gönderdi!"
        val messageId = data["messageId"] ?: data["id"] ?: remoteMessage.messageId.orEmpty()

        // In the foreground FCM delivers the callback; in the background
        // notification messages are already rendered by Android's system tray.
        if (isChatOpen()) return

        NotificationHelper.showChatNotification(
            context = applicationContext,
            senderName = senderName.removeSuffix(" ❤️"),
            messageText = messageText,
            messageId = messageId,
            imageUrl = data["imageUrl"] ?: data["mediaUrl"]
        )
    }

    private fun isChatOpen(): Boolean =
        getSharedPreferences("ikimiz_runtime", Context.MODE_PRIVATE).getBoolean("chat_open", false)

    private fun saveTokenToUserDocument(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid).set(
            mapOf(
                "fcmToken" to token,
                "fcmTokenUpdatedAt" to System.currentTimeMillis()
            ),
            SetOptions.merge()
        )
    }

    companion object {
        private const val TAG = "MyFCMService"

        fun syncCurrentToken() {
            FirebaseAuth.getInstance().currentUser?.uid ?: return
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                FirebaseFirestore.getInstance().collection("users")
                    .document(FirebaseAuth.getInstance().currentUser!!.uid)
                    .set(
                        mapOf(
                            "fcmToken" to token,
                            "fcmTokenUpdatedAt" to System.currentTimeMillis()
                        ),
                        SetOptions.merge()
                    )
            }.addOnFailureListener {
                Log.w(TAG, "Could not refresh FCM token: ${it.message}")
            }
        }
    }
}
