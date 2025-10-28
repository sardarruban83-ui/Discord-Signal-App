package com.example.discordsignal

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class NotificationForwarderService : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val forwarderUrl = "http://72.61.145.142:5000/signal"  // <- Change this!

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val pkg = sbn?.packageName ?: return
        if (pkg != "com.discord") return

        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        if (!text.contains("Buying", true) && !text.contains("Selling", true)) return

        val payload = JSONObject()
        payload.put("title", title)
        payload.put("text", text)
        payload.put("timestamp", System.currentTimeMillis())

        scope.launch {
            try {
                val conn = URL(forwarderUrl).openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.outputStream.use { it.write(payload.toString().toByteArray()) }
                conn.responseCode
                Log.i("Forwarder", "✅ Sent: $title -> $text")
            } catch (e: Exception) {
                Log.e("Forwarder", "❌ Error: ${e.message}")
            }
        }
    }
}
