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

    // Regex: matches your exact signal pattern (tolerant to minor spacing/variants)
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

            // show in-app log (existing behavior)
            val i = Intent("com.example.discordsignal.NOTIF_RECEIVED")
            i.putExtra("pkg", pkg)
            i.putExtra("title", title)
            i.putExtra("text", text)
            sendBroadcast(i)

            // Only consider Discord notifications (you already had this filter)
            if (!pkg.contains("discord", ignoreCase = true) && !pkg.contains("com.discord", ignoreCase = true)) {
                return
            }

            // Quick guard: skip if clearly not signal-ish
            if (!text.contains("Buying", ignoreCase = true) && !text.contains("First buying", ignoreCase = true)) {
                return
            }

            // Attempt robust parse using regex
            val m = SIGNAL_RE.matcher(text)
            if (!m.find()) {
                // not a valid structured signal — do not forward
                return
            }

            // Extract symbol (group 1 per regex) and sanitize
            var sym = m.group(1) ?: ""
            sym = sym.replace(Regex("[^A-Za-z0-9_]"), "").uppercase()

            // Build cleaned payload (strip @everyone/@here)
            val cleaned = text.replace("@everyone", "").replace("@here", "").trim()

            // Build JSON payload exactly like your forwarder expects
            val json = JSONObject()
            json.put("content", cleaned)
            json.put("author", pkg)
            json.put("parsed_symbol", sym)

            // send to webhooks from SharedPreferences (same storage your UI uses)
            val prefs = getSharedPreferences("webhooks", MODE_PRIVATE)
            val urls = prefs.getStringSet("urls", emptySet()) ?: emptySet()
            if (urls.isEmpty()) {
                // no targets configured, keep log only
                return
            }

            val body = RequestBody.create("application/json".toMediaTypeOrNull(), json.toString())

            for (url in urls) {
                try {
                    val request = Request.Builder()
                        .url(url)
                        .post(body)
                        .build()
                    val resp = client.newCall(request).execute()
                    resp.close()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    // continue to next webhook (do not crash)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {}
}
