

package com.example.discordsignal

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import kotlin.concurrent.thread
import android.content.Context

/**
 * NotificationListener: listens for notifications, detects "signal" messages,
 * broadcasts them to the app UI (action: "com.example.discordsignal.NOTIF_RECEIVED")
 * and forwards the detected full signal text to all saved webhook URLs (stored as JSON array
 * in SharedPreferences under key "webhooks").
 *
 * Filtering logic: basic, looks for keywords typical to your signals:
 *  - "Buying" or "Buy" and a coin token like "$MLN" or "MLN"
 *  - presence of "First buying", "Targets", "CMP", "Sl" or "Targets:" lines
 *
 * We keep forwarding simple and fast: an async thread posts to each webhook with
 * a small JSON payload { "content": "<signal text>" } (Discord webhook).
 */

class NotificationListener : NotificationListenerService() {
    private val TAG = "NotifListener"
    private val ACTION_BROADCAST = "com.example.discordsignal.NOTIF_RECEIVED"
    private val PREFS = "discordsignal_prefs"
    private val PREF_WEBHOOKS = "webhooks" // JSON array string

    private val client = OkHttpClient()

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "NotificationListener connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            val pkg = sbn.packageName ?: "?"
            val notif = sbn.notification
            val extras = notif.extras
            var title = extras.getString(Notification.EXTRA_TITLE) ?: ""
            var text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            // Some apps put long text in SUMMARY_TEXT or BIG_TEXT
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
            if (bigText.isNotEmpty() && bigText.length > text.length) text = bigText

            // Combine to a single content string (preserve newlines)
            val content = (listOf(title, text)).filter { it.isNotBlank() }.joinToString("\n").trim()

            if (content.isBlank()) return

            Log.d(TAG, "notif from=$pkg title='$title' text='${text.take(120)}'")

            // detect if this notification looks like a trading signal
            if (looksLikeSignal(content)) {
                // 1) broadcast to app so UI can show it immediately
                val b = Intent(ACTION_BROADCAST)
                b.putExtra("pkg", pkg)
                b.putExtra("title", title)
                b.putExtra("text", text)
                sendBroadcast(b)

                // 2) forward to webhooks (async)
                forwardToWebhooks(applicationContext, content)
            }
        } catch (ex: Exception) {
            Log.e(TAG, "onNotificationPosted error", ex)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Nothing needed here
    }

    private fun looksLikeSignal(content: String): Boolean {
        // Lowercase for simple checks
        val s = content.lowercase()

        // quick keyword checks
        val hasBuy = s.contains("buying") || s.contains("first buying") || s.contains("buy in")
        val hasTargets = s.contains("targets") || s.contains("targets:")
        val hasSl = s.contains("sl:") || s.contains("stop loss") || s.contains("sl ")
        val hasCmp = s.contains("cmp:") || s.contains("cmp")
        val hasPct = s.contains("%") && (s.contains("tp") || s.contains("targets"))

        // coin token detection: e.g. "$rlc" or "rlc/usdt" or coin with 2-6 letters
        val coinRegex = Regex("""\b\$?[A-Za-z]{2,6}\b""")
        val coinFound = coinRegex.find(s) != null

        // consider it a signal if we have core pieces: buy + (targets or sl) + a coin
        val condition = hasBuy && (hasTargets || hasSl || hasCmp || hasPct) && coinFound

        // small extra: if content contains "first buying" or "first buy" stronger match
        val strong = s.contains("first buying") || s.contains("first buy") || s.contains("first buying range")

        return condition || strong
    }

    private fun forwardToWebhooks(ctx: Context, content: String) {
        thread {
            try {
                val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val hooksJson = prefs.getString(PREF_WEBHOOKS, "[]") ?: "[]"

                val arr = JSONArray(hooksJson)
                if (arr.length() == 0) {
                    Log.i(TAG, "No webhooks configured, skipping forward")
                    return@thread
                }

                // Prepare JSON payload for Discord webhook: { "content": "..." }
                val payloadObj = JSONObject()
                // keep message short if too long
                val message = if (content.length > 1900) content.take(1900) + "..." else content
                payloadObj.put("content", message)

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = RequestBody.create(mediaType, payloadObj.toString())

                // iterate webhooks
                for (i in 0 until arr.length()) {
                    try {
                        val url = arr.optString(i, "").trim()
                        if (url.isEmpty()) continue

                        val req = Request.Builder()
                            .url(url)
                            .post(body)
                            .build()

                        // do synchronous call (in dedicated thread) and just log
                        client.newCall(req).execute().use { resp ->
                            val code = resp.code
                            if (code in 200..299) {
                                Log.i(TAG, "Webhook forwarded OK to $url (code=$code)")
                            } else {
                                Log.w(TAG, "Webhook failed $url code=$code body=${resp.body?.string()}")
                            }
                        }
                    } catch (we: IOException) {
                        Log.e(TAG, "Webhook send IO error", we)
                    } catch (ee: Exception) {
                        Log.e(TAG, "Webhook send error", ee)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "forwardToWebhooks error", e)
            }
        }
    }
}
