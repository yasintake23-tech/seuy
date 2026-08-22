package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.service.CoupleMessageForegroundService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d("BootReceiver", "Device boot or package replaced, checking for active couple session...")
            val prefs = context.getSharedPreferences("ikimiz_service_prefs", Context.MODE_PRIVATE)
            val uid = prefs.getString("cached_uid", null)
            val partnerId = prefs.getString("cached_partner_id", null)

            if (!uid.isNullOrBlank() && !partnerId.isNullOrBlank()) {
                Log.d("BootReceiver", "Restarting CoupleMessageForegroundService for $uid and $partnerId")
                CoupleMessageForegroundService.start(context, uid, partnerId)
            }
        }
    }
}
