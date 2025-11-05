package com.example.discordsignal

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.content.Intent
import android.os.Bundle
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.util.regex.Pattern
import java.lang.Thread.sleep

class NotificationListener : NotificationListenerService() {

    private val client = OkHttpClient.Builder().build()

    // Regex tuned to the format you provided (case-insensitive, robust whitespace)
    private val SIGNAL_RE = Pattern.compile(
        "(?is)Buying\\s+\\$?([A-Za-z0-9_]{1,20})\\b[\\s\\S]*?First\\s*buy(?:ing)?\\s*[:\\s]*([0-9.]+)\\s*[-–—]\\s*([0-9.]+).*?Second\\s*buy(?:ing)?\\s*[:\\s]*([0-9.]+)?[\\s\\S]*?CMP\\s*[:\\s]*([0-9.]+)?[\\s\\S]*?SL\\s*[:\\s]*([0-9.]+)",
        Pattern.CASE_INSENSITIVE
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            val pkg = sbn.packageName ?: "?"
            val extras: Bundle? = sbn.notification.extras
            val title = extras?.getString("android.title") ?: ""
            val textObj = extras?.getCharSequence("android.text")
            val text = textObj?.toString() ?: ""

            // Always broadcast raw notification to UI log (so app shows everything)
            val i = Intent("com.example.discordsignal.NOTIF_RECEIVED")
            i.putExtra("pkg", pkg)
            i.putExtra("title", title)
            i.putExtra("text", text)
            sendBroadcast(i)

            // Only parse/forward signals coming from Discord packages
            if (!pkg.contains("discord", ignoreCase = true) && !pkg.contains("com.discord", ignoreCase = true)) {
                return
            }

            // Quick check for keyword to reduce regex attempts
            if (!text.contains("Buying", ignoreCase = true) && !text.contains("First buying", ignoreCase = true)) {
                return
            }

            val m = SIGNAL_RE.matcher(text)
            if (!m.find()) {
                // not a full signal in expected format
                return
            }

            // Extract symbol and other fields (if present)
            var symbol = m.group(1) ?: ""
            symbol = symbol.replace(Regex("[^A-Za-z0-9_]"), "").uppercase()

            // prepare cleaned content (remove mentions)
            val cleaned = text.replace("@everyone", "").replace("@here", "").trim()

            // truncate to safe limit (Discord ~2000 chars per message; we use margin)
            val MAX_LEN = 1800
            val contentToSend = if (cleaned.length > MAX_LEN) cleaned.take(MAX_LEN) + "\n\n[truncated]" else cleaned

            // prepare JSON payload
            val json = JSONObject()
            json.put("content", contentToSend)
            json.put("parsed_symbol", symbol)
            json.put("source_pkg", pkg)

            val prefs = getSharedPreferences("webhooks", MODE_PRIVATE)
            val urls = prefs.getStringSet("urls", emptySet()) ?: emptySet()
            if (urls.isEmpty()) return

            val body = RequestBody.create("application/json".toMediaTypeOrNull(), json.toString())

            // Forward to every webhook with small retry logic
            for (url in urls) {
                var success = false
                var message = ""
                var attempts = 0
                while (attempts < 3 && !success) { // 1 initial + up to 2 retries
                    attempts++
                    try {
                        val req = Request.Builder().url(url).post(body).build()
                        val resp = client.newCall(req).execute()
                        val code = resp.code
                        resp.close()
                        success = (code in 200..299)
                        message = "HTTP $code"
                        if (!success && (code == 429)) {
                            // rate limited -> brief wait then retry
                            Thread.sleep(600L)
                        }
                    } catch (ex: Exception) {
                        message = ex.toString()
                        // backoff slightly before retry
                        try { Thread.sleep(300L) } catch (_: InterruptedException) {}
                    }
                }

                // broadcast result to UI (MainActivity will append to log)
                val fb = Intent("com.example.discordsignal.FORWARD_RESULT")
                fb.putExtra("url", url)
                fb.putExtra("forwarded", success)
                fb.putExtra("message", message)
                fb.putExtra("symbol", symbol)
                sendBroadcast(fb)
            }

        } catch (e: Exception) {
            // don't crash the service; just print
            e.printStackTrace()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {}
}
