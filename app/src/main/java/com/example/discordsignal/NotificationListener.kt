package com.example.discordsignal

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.app.Notification

class NotificationListener : NotificationListenerService() {
    companion object {
        const val TAG = "TLNotificationListener"
    }

    private fun publishLog(context: Context, pkg: String, title: String?, text: String?) {
        val intent = android.content.Intent("com.example.discordsignal.NOTIF_RECEIVED")
        intent.putExtra("pkg", pkg)
        intent.putExtra("title", title)
        intent.putExtra("text", text)
        context.sendBroadcast(intent)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            val pkg = sbn.packageName ?: "unknown"
            val extras = sbn.notification.extras
            val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
            val textObj = extras.getCharSequence(Notification.EXTRA_TEXT)
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            val text = when {
                !bigText.isNullOrEmpty() -> bigText.toString()
                !textObj.isNullOrEmpty() -> textObj.toString()
                else -> ""
            }

            Log.i(TAG, "notif from=$pkg title='$title' text='$text'")

            publishLog(applicationContext, pkg, title, text)

        } catch (e: Exception) {
            Log.e(TAG, "error reading notification", e)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // optional: handle removal
    }
}
