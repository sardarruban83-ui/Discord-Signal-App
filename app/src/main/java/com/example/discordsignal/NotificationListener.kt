package com.example.discordsignal

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.content.Intent
import android.util.Log

class NotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        Log.d("TradeLinker", "Notification listener connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn?.let {
            val packageName = it.packageName
            val extras = it.notification.extras
            val title = extras.getString("android.title") ?: "(no title)"
            val text = extras.getCharSequence("android.text")?.toString() ?: "(no text)"

            Log.d("TradeLinker", "Notification from $packageName: $title - $text")

            // Send broadcast to MainActivity
            val intent = Intent("com.example.discordsignal.NEW_NOTIFICATION")
            intent.putExtra("text", "$packageName: $title - $text")
            sendBroadcast(intent)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn?.let {
            Log.d("TradeLinker", "Notification removed: ${it.packageName}")
        }
    }
}
