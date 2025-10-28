package com.example.discordsignal

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class NotificationListener : NotificationListenerService() {
    private val TAG = "NotifListener"

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            val pkg = sbn.packageName ?: return
            val extras = sbn.notification.extras
            val title = extras.getString("android.title") ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            val full = (title + "\n" + text).trim()

            Log.d(TAG, "Got notif from $pkg | title=$title text=$text")

            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            val webhook = prefs.getString("webhook_url", "") ?: ""
            val allowCsv = prefs.getString("allow_packages", "") ?: ""
            val blockCsv = prefs.getString("block_packages", "") ?: ""
            val keywordsCsv = prefs.getString("keywords", "") ?: ""
            val regexStr = prefs.getString("regex", "") ?: ""

            // package block check
            if (blockCsv.isNotBlank()) {
                val blocked = blockCsv.split(",").map { it.trim() }.any { it.isNotEmpty() && pkg.contains(it) }
                if (blocked) {
                    Log.d(TAG, "Package $pkg blocked by config")
                    return
                }
            }

            // package allow check (if set)
            if (allowCsv.isNotBlank()) {
                val allowed = allowCsv.split(",").map { it.trim() }.any { it.isNotEmpty() && pkg.contains(it) }
                if (!allowed) {
                    Log.d(TAG, "Package $pkg not in allow list, ignoring")
                    return
                }
            }

            // keywords (if set)
            if (keywordsCsv.isNotBlank()) {
                val keywords = keywordsCsv.split(",").map { it.trim().lowercase() }
                val found = keywords.any { it.isNotEmpty() && full.lowercase().contains(it) }
                if (!found) {
                    Log.d(TAG, "No keyword matched, ignoring")
                    return
                }
            }

            // regex (if set)
            if (regexStr.isNotBlank()) {
                try {
                    val regex = Regex(regexStr, RegexOption.IGNORE_CASE)
                    if (!regex.containsMatchIn(full)) {
                        Log.d(TAG, "Regex did not match, ignoring")
                        return
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Bad regex: $e")
                }
            }

            val parsed = parseNotificationText(full, pkg)

            if (webhook.isNotBlank()) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        postJson(webhook, parsed)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed send webhook: ${e.message}")
                    }
                }
            } else {
                Log.d(TAG, "Webhook not configured; parsed: $parsed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "onNotificationPosted error: ${e.message}")
        }
    }

    private fun parseNotificationText(full: String, pkg: String): JSONObject {
        val obj = JSONObject()
        obj.put("package", pkg)
        obj.put("raw", full)

        // find $SYMBOL style token
        val symbolRegex = Regex("""\$[A-Za-z0-9_.-]+""")
        val m = symbolRegex.find(full)
        if (m != null) obj.put("symbol", m.value)

        // find numbers that look like prices
        val priceRegex = Regex("""\d+(\.\d+)?""")
        val priceMatches = priceRegex.findAll(full).map { it.value }.toList()
        if (priceMatches.isNotEmpty()) obj.put("numbers", priceMatches)

        return obj
    }

    private fun postJson(webhookUrl: String, json: JSONObject) {
        val url = URL(webhookUrl)
        (url.openConnection() as? HttpURLConnection)?.run {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 15_000
            val out = OutputStreamWriter(outputStream, "UTF-8")
            out.write(json.toString())
            out.flush()
            out.close()
            val code = responseCode
            Log.d(TAG, "Webhook POST result: $code")
            inputStream?.close()
            disconnect()
        }
    }
}
