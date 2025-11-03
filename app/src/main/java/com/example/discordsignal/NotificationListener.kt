package com.example.discordsignal

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.content.Intent
import android.os.Bundle
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject

class NotificationListener : NotificationListenerService() {

    private val client = OkHttpClient()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            val pkg = sbn.packageName ?: "?"
            val extras: Bundle? = sbn.notification.extras
            val title = extras?.getString("android.title") ?: ""
            val textObj = extras?.getCharSequence("android.text")
            val text = textObj?.toString() ?: ""

            // Send broadcast to show in app (log)
            val i = Intent("com.example.discordsignal.NOTIF_RECEIVED")
            i.putExtra("pkg", pkg)
            i.putExtra("title", title)
            i.putExtra("text", text)
            sendBroadcast(i)

            // Only forward Discord signal-style messages
            if (pkg.contains("discord", ignoreCase = true) && text.contains("Buying $", ignoreCase = true)) {
                forwardToWebhook("$title\n$text")
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun forwardToWebhook(message: String) {
        try {
            val prefs = getSharedPreferences("webhooks", MODE_PRIVATE)
            val urls = prefs.getStringSet("urls", emptySet()) ?: emptySet()

            if (urls.isEmpty()) return

            val json = JSONObject()
            json.put("content", message)

            val body = RequestBody.create("application/json".toMediaTypeOrNull(), json.toString())

            for (url in urls) {
                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                client.newCall(request).execute().close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {}
}
