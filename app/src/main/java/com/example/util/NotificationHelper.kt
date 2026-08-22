package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object NotificationHelper {
    private const val TAG = "NotificationHelper"
    const val CHANNEL_ID = "ikimiz_chat_notifications"
    const val CHANNEL_NAME = "İkimiz Aşk ve Sohbet Bildirimleri"
    const val SERVICE_CHANNEL_ID = "ikimiz_background_service"
    const val SERVICE_CHANNEL_NAME = "İkimiz Arka Plan Bağlantı Servisi"
    const val NOTIFICATION_ID_BASE = 1001
    const val SERVICE_NOTIFICATION_ID = 2001

    fun areNotificationsEnabled(context: Context): Boolean {
        val notificationManager = NotificationManagerCompat.from(context)
        return notificationManager.areNotificationsEnabled()
    }

    fun openNotificationSettings(context: Context) {
        try {
            val intent = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
                    Intent("android.settings.APP_NOTIFICATION_SETTINGS").apply {
                        putExtra("app_package", context.packageName)
                        putExtra("app_uid", context.applicationInfo.uid)
                    }
                }
                else -> {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                }
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open notification settings, falling back to app details", e)
            try {
                val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (fallbackError: Exception) {
                Log.e(TAG, "Failed to open app details settings", fallbackError)
            }
        }
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val chatChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Partnerinizden gelen anlık mesajlar ve aşk bildirimleri"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200)
            }

            val serviceChannel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                SERVICE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Sevgilinizden arka planda anlık mesaj alabilmek için çalışan servis"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(chatChannel)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    fun showChatNotification(
        context: Context,
        senderName: String,
        messageText: String,
        messageId: String = "",
        imageUrl: String? = null
    ) {
        try {
            createNotificationChannel(context)

            val notificationManager = NotificationManagerCompat.from(context)
            if (!notificationManager.areNotificationsEnabled()) {
                Log.w(TAG, "Notifications are disabled by the user.")
                return
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("extra_tab", "chat")
                putExtra("extra_message_id", messageId)
            }

            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                pendingIntentFlags
            )

            val displayTitle = if (senderName.isNotBlank() && senderName != "Sevgilin") {
                "$senderName ❤️"
            } else {
                "Sevgilinden yeni bir mesaj var! ❤️"
            }

            val displayText = when {
                !imageUrl.isNullOrBlank() && messageText.isBlank() -> "📸 Sana bir fotoğraf gönderdi"
                !imageUrl.isNullOrBlank() -> "📸 Fotoğraf: $messageText"
                messageText.isNotBlank() -> messageText
                else -> "Tatlı bir mesaj gönderdi ✨"
            }

            val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle(displayTitle)
                .setContentText(displayText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(displayText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setSound(defaultSound)
                .setVibrate(longArrayOf(0, 200, 100, 200))
                .setContentIntent(pendingIntent)
                .build()

            val notifId = if (messageId.isNotBlank()) messageId.hashCode() else NOTIFICATION_ID_BASE
            notificationManager.notify(notifId, notification)
            Log.d(TAG, "Chat notification shown successfully for message: $messageId")
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException while showing notification (POST_NOTIFICATIONS permission not granted): ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing chat notification", e)
        }
    }
}
