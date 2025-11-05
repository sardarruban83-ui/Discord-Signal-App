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
import java.util.regex.Pattern

class NotificationListener : NotificationListenerService() {

    private val client = OkHttpClient()

    private val SIGNAL_RE = Pattern.compile(
        "(?is)Buying\\s+\\$?([A-Za-z0-9_]{2,12})[\\s\\S]*?First\\s*buy(?:ing)?\\s*[:\\s]*([0-9.]+)\\s*[-–—]\\s*([0-9.]+)[\\s\\S]*?(?:Second\\s*buy(?:ing)?\\s*[:\\s]*([0-9.]+))?[\\s\\S]*?(?:CMP\\s*[:\\s]*([0-9.]+))?[\\s\\S]*?(?:Targets[\\s\\S]*?)?([0-9]{1,2}%[\\s\\S]*?)?SL\\s*[:\\s]*([0-9.]+)",
        Pattern.CASE_INSENSITIVE
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            val pkg = sbn.packageName ?: "?"
            val extras: Bundle? = sbn.notification.extras
            val title = extras?.getString("android.title") ?: ""
            val textObj = extras?.getCharSequence("android.text")
            val text = textObj?.toString() ?: ""

            // Broadcast for in-app log (unchanged)
            val i = Intent("com.example.discordsignal.NOTIF_RECEIVED")
            i.putExtra("pkg", pkg)
            i.putExtra("title", title)
            i.putExtra("text", text)
            sendBroadcast(i)

            if (!pkg.contains("discord", ignoreCase = true) && !pkg.contains("com.discord", ignoreCase = true)) {
                return
            }
            if (!text.contains("Buying", ignoreCase = true) && !text.contains("First buying", ignoreCase = true)) return

            val m = SIGNAL_RE.matcher(text)
            if (!m.find()) return

            var sym = m.group(1) ?: ""
            sym = sym.replace(Regex("[^A-Za-z0-9_]"), "").uppercase()
            val cleaned = text.replace("@everyone", "").replace("@here", "").trim()

            // TRUNCATE to avoid Discord payload-size rejections (safe default)
            val MAX_LEN = 1800
            val contentToSend = if (cleaned.length > MAX_LEN) cleaned.take(MAX_LEN) + "\n\n[truncated]" else cleaned

            val json = JSONObject()
            json.put("content", contentToSend)
            json.put("author", pkg)
            json.put("parsed_symbol", sym)

            val prefs = getSharedPreferences("webhooks", MODE_PRIVATE)
            val urls = prefs.getStringSet("urls", emptySet()) ?: emptySet()
            if (urls.isEmpty()) return

            val body = RequestBody.create("application/json".toMediaTypeOrNull(), json.toString())

            for (url in urls) {
                var success = false
                var msg = ""
                try {
                    val request = Request.Builder()
                        .url(url)
                        .post(body)
                        .build()
                    val resp = client.newCall(request).execute()
                    val code = resp.code
                    resp.close()
                    success = (code >= 200 && code < 300)
                    msg = "HTTP $code"
                } catch (ex: Exception) {
                    // capture full exception class + message
                    msg = ex.toString()
                } finally {
                    // broadcast forward result (UI reads this)
                    val fb = Intent("com.example.discordsignal.FORWARD_RESULT")
                    fb.putExtra("url", url)
                    fb.putExtra("forwarded", success)
                    fb.putExtra("message", msg)
                    fb.putExtra("symbol", sym)
                    sendBroadcast(fb)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {}
}
