package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.model.ChatMessage
import com.example.util.NotificationHelper
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class CoupleMessageForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var rtdbListener: ValueEventListener? = null
    private var firestoreListener: ListenerRegistration? = null
    private var currentCoupleId: String? = null
    private var currentUserId: String? = null
    private var partnerId: String? = null

    private val notifiedMessageIds = mutableSetOf<String>()
    private var isFirstLoad = true

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "CoupleMessageForegroundService onCreate")
        NotificationHelper.createNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }

        val uid = intent?.getStringExtra(EXTRA_CURRENT_USER_ID)
            ?: getSharedPreferences("ikimiz_service_prefs", Context.MODE_PRIVATE).getString("cached_uid", null)
        val pId = intent?.getStringExtra(EXTRA_PARTNER_ID)
            ?: getSharedPreferences("ikimiz_service_prefs", Context.MODE_PRIVATE).getString("cached_partner_id", null)

        if (!uid.isNullOrBlank() && !pId.isNullOrBlank()) {
            currentUserId = uid
            partnerId = pId
            // Save in prefs for restart on reboot
            getSharedPreferences("ikimiz_service_prefs", Context.MODE_PRIVATE).edit()
                .putString("cached_uid", uid)
                .putString("cached_partner_id", pId)
                .apply()

            currentCoupleId = if (uid < pId) "${uid}_${pId}" else "${pId}_${uid}"

            startForegroundNotification()
            startListeningForMessages(currentCoupleId!!, uid)
        } else {
            Log.w(TAG, "No valid user or partner ID provided, stopping foreground service")
            stopSelf()
            return START_NOT_STICKY
        }

        return START_STICKY
    }

    private fun startForegroundNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("extra_tab", "chat")
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            pendingIntentFlags
        )

        val notification: Notification = NotificationCompat.Builder(this, NotificationHelper.SERVICE_CHANNEL_ID)
            .setContentTitle("İkimiz • Arka Planda Bağlantıda")
            .setContentText("Sevgilinizden gelen mesajlar anında iletilir ❤️")
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NotificationHelper.SERVICE_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NotificationHelper.SERVICE_NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground service notification", e)
        }
    }

    private fun startListeningForMessages(coupleId: String, myUserId: String) {
        removeListeners()
        isFirstLoad = true

        serviceScope.launch {
            try {
                // 1. Listen to Realtime Database
                val realtimeDb = try {
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
                            Log.w(TAG, "RTDB url init error: ${e.message}")
                        }
                    }
                    instance ?: FirebaseDatabase.getInstance()
                } catch (e: Exception) {
                    FirebaseDatabase.getInstance()
                }

                val messagesRef = realtimeDb.reference
                    .child("chats")
                    .child(coupleId)
                    .child("messages")

                rtdbListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!snapshot.exists()) return

                        val incomingMessages = mutableListOf<ChatMessage>()
                        for (child in snapshot.children) {
                            val map = child.value as? Map<*, *> ?: continue
                            @Suppress("UNCHECKED_CAST")
                            val strMap = map as Map<String, Any?>
                            val msgId = (strMap["id"] as? String)?.takeIf { it.isNotBlank() } ?: (child.key ?: "")
                            val msg = ChatMessage.fromMap(strMap).copy(id = msgId)
                            if (msg.id.isNotBlank()) {
                                incomingMessages.add(msg)
                            }
                        }

                        if (isFirstLoad) {
                            incomingMessages.forEach { m -> notifiedMessageIds.add(m.id) }
                            isFirstLoad = false
                        } else {
                            incomingMessages.forEach { msg ->
                                val isFromPartner = (msg.senderId == partnerId || (msg.senderId != myUserId && msg.senderId != "me"))
                                if (isFromPartner && !msg.isDeleted && !notifiedMessageIds.contains(msg.id)) {
                                    notifiedMessageIds.add(msg.id)
                                    val partnerName = msg.senderName.takeIf { it.isNotBlank() } ?: "Sevgilin"
                                    NotificationHelper.showChatNotification(
                                        context = applicationContext,
                                        senderName = partnerName,
                                        messageText = msg.text,
                                        messageId = msg.id,
                                        imageUrl = msg.effectiveMediaUrl
                                    )
                                }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.w(TAG, "RTDB background listener error: ${error.message}")
                    }
                }

                messagesRef.addValueEventListener(rtdbListener!!)

                // 2. Firestore Secondary Backup Listener
                firestoreListener = FirebaseFirestore.getInstance()
                    .collection("couples")
                    .document(coupleId)
                    .collection("messages")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null || snapshot == null) return@addSnapshotListener
                        if (!isFirstLoad) {
                            snapshot.documents.forEach { doc ->
                                doc.data?.let { data ->
                                    val msgId = (data["id"] as? String)?.takeIf { it.isNotBlank() } ?: doc.id
                                    val msg = ChatMessage.fromMap(data).copy(id = msgId)
                                    val isFromPartner = (msg.senderId == partnerId || (msg.senderId != myUserId && msg.senderId != "me"))
                                    if (isFromPartner && !msg.isDeleted && !notifiedMessageIds.contains(msg.id)) {
                                        notifiedMessageIds.add(msg.id)
                                        val partnerName = msg.senderName.takeIf { it.isNotBlank() } ?: "Sevgilin"
                                        NotificationHelper.showChatNotification(
                                            context = applicationContext,
                                            senderName = partnerName,
                                            messageText = msg.text,
                                            messageId = msg.id,
                                            imageUrl = msg.effectiveMediaUrl
                                        )
                                    }
                                }
                            }
                        }
                    }

            } catch (e: Exception) {
                Log.e(TAG, "Error attaching background listeners", e)
            }
        }
    }

    private fun removeListeners() {
        try {
            rtdbListener = null
            firestoreListener?.remove()
            firestoreListener = null
        } catch (e: Exception) {
            Log.e(TAG, "Error removing listeners", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeListeners()
        serviceScope.cancel()
        Log.d(TAG, "CoupleMessageForegroundService onDestroy")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "CoupleMessageService"
        const val ACTION_START_SERVICE = "com.example.service.START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.service.STOP_SERVICE"
        const val EXTRA_CURRENT_USER_ID = "extra_current_user_id"
        const val EXTRA_PARTNER_ID = "extra_partner_id"

        fun start(context: Context, currentUserId: String, partnerId: String) {
            try {
                val intent = Intent(context, CoupleMessageForegroundService::class.java).apply {
                    action = ACTION_START_SERVICE
                    putExtra(EXTRA_CURRENT_USER_ID, currentUserId)
                    putExtra(EXTRA_PARTNER_ID, partnerId)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start CoupleMessageForegroundService", e)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, CoupleMessageForegroundService::class.java).apply {
                    action = ACTION_STOP_SERVICE
                }
                context.stopService(intent)
                context.getSharedPreferences("ikimiz_service_prefs", Context.MODE_PRIVATE).edit().clear().apply()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop CoupleMessageForegroundService", e)
            }
        }
    }
}
