package com.example.discordsignal

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.*
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class NotificationListener : NotificationListenerService() {
    private val TAG = "TradeLinkerNL"
    // Put your webhook here (or update later in code / UI)
    private val WEBHOOK_URL = "YOUR_WEBHOOK_URL_HERE"

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "NotificationListener connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            val pkg = sbn.packageName ?: "unknown"
            val notif = sbn.notification
            val extras = notif.extras
            val title = extras.getString("android.title") ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            Log.i(TAG, "Received: pkg=$pkg title='$title' text='$text'")

            // Build JSON payload
            val payload = JSONObject()
            payload.put("package", pkg)
            payload.put("title", title)
            payload.put("text", text)
            payload.put("timestamp", System.currentTimeMillis())

            // Send to webhook off the main thread
            if (WEBHOOK_URL.isNotBlank() && WEBHOOK_URL != "YOUR_WEBHOOK_URL_HERE") {
                CoroutineScope(Dispatchers.IO).launch {
                    postJson(WEBHOOK_URL, payload.toString())
                }
            } else {
                Log.w(TAG, "Webhook URL not set. Skipping network post.")
            }
        } catch (t: Throwable) {
            Log.e(TAG, "onNotificationPosted error", t)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // optionally handle removals
    }

    private fun postJson(urlStr: String, jsonBody: String) {
        try {
            val url = URL(urlStr)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 10_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(jsonBody) }
            val code = conn.responseCode
            Log.i(TAG, "Webhook POST responded: $code")
            conn.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Webhook POST failed", e)
        }
    }
}
