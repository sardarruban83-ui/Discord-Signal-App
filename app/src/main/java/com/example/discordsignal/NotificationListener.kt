package com.example.discordsignal

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class NotificationListener : NotificationListenerService() {

    private val client = OkHttpClient()

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        try {
            val packageName = sbn?.packageName ?: return
            val extras = sbn.notification.extras
            val title = extras.getString("android.title") ?: ""
            val text = extras.getString("android.text") ?: ""

            // Combine multi-line messages if possible
            val fullMessage = "$title $text".trim()

            // Filter: only send if it's a trading signal (contains "Buying $" and "Targets")
            if (fullMessage.contains("Buying $", ignoreCase = true) &&
                (fullMessage.contains("Targets", ignoreCase = true) ||
                 fullMessage.contains("Sl:", ignoreCase = true))
            ) {
                Log.d("SignalDetector", "Trading signal detected: $fullMessage")
                sendToAllWebhooks(fullMessage)
            }

        } catch (e: Exception) {
            Log.e("SignalError", "Error processing notification: ${e.message}")
        }
    }

    private fun sendToAllWebhooks(signal: String) {
        val prefs = getSharedPreferences("webhooks", MODE_PRIVATE)
        val allWebhooks = prefs.getStringSet("urls", mutableSetOf()) ?: mutableSetOf()

        for (url in allWebhooks) {
            try {
                val json = JSONObject().apply {
                    put("content", signal)
                }

                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(url).post(body).build()
                client.newCall(request).execute().use { response ->
                    Log.d("WebhookSend", "Sent to $url (${response.code})")
                }
            } catch (e: Exception) {
                Log.e("WebhookError", "Failed to send to $url: ${e.message}")
            }
        }
    }
}
