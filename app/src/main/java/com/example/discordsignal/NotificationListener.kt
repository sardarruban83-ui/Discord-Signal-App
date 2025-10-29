package com.example.discordsignal

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter

class NotificationListener : NotificationListenerService() {
    private val TAG = "NotificationListener"

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            val pkg = sbn.packageName ?: return
            val notif = sbn.notification
            val title = notif.extras?.getString("android.title") ?: ""
            val text = notif.extras?.getCharSequence("android.text")?.toString() ?: ""
            Log.i(TAG, "Notif from=$pkg title=$title text=$text")

            // Forward minimal payload asynchronously (replace FORWARDER_URL if needed)
            val forwarderUrl = "http://72.61.145.142:5000/forward" // adjust if your forwarder endpoint differs
            val payload = "{\"package\":\"$pkg\",\"title\":${escapeJson(title)},\"text\":${escapeJson(text)}}"

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val url = URL(forwarderUrl)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doOutput = true
                    OutputStreamWriter(conn.outputStream).use { it.write(payload) }
                    val code = conn.responseCode
                    Log.i(TAG, "Forward status=$code")
                    conn.disconnect()
                } catch (e: Exception) {
                    Log.e(TAG, "Forward failed: ${e.message}", e)
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "onNotificationPosted crash: ${t.message}", t)
        }
    }

    override fun onListenerConnected() {
        Log.i(TAG, "NotificationListener connected")
    }

    override fun onListenerDisconnected() {
        Log.i(TAG, "NotificationListener disconnected")
    }

    private fun escapeJson(s: String): String {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""
    }
}
