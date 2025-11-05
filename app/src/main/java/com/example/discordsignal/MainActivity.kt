package com.example.discordsignal

import okhttp3.RequestBody.Companion.toRequestBody
import android.os.Bundle
import android.widget.*
import android.content.SharedPreferences
import android.provider.Settings
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var webhookList: LinearLayout
    private lateinit var addWebhookButton: Button
    private lateinit var prefs: SharedPreferences
    private val client = OkHttpClient()

    // new UI
    private lateinit var tvListenerStatus: TextView
    private lateinit var tvLog: TextView
    private lateinit var logScroll: ScrollView

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            if (intent == null) return
            when (intent.action) {
                "com.example.discordsignal.NOTIF_RECEIVED" -> {
                    val pkg = intent.getStringExtra("pkg") ?: "?"
                    val title = intent.getStringExtra("title") ?: ""
                    val text = intent.getStringExtra("text") ?: ""
                    appendLog("RECV from [$pkg]: ${short(title, text)}")
                }
                "com.example.discordsignal.FORWARD_RESULT" -> {
                    val url = intent.getStringExtra("url") ?: ""
                    val forwarded = intent.getBooleanExtra("forwarded", false)
                    val msg = intent.getStringExtra("message") ?: ""
                    val sym = intent.getStringExtra("symbol") ?: ""
                    appendLog("FORWARD ${if (forwarded) "OK" else "FAIL"} $sym -> ${short(url, msg)}")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("webhooks", MODE_PRIVATE)
        webhookList = findViewById(R.id.webhookList)
        addWebhookButton = findViewById(R.id.addWebhook)

        tvListenerStatus = findViewById(R.id.tv_listener_status)
        tvLog = findViewById(R.id.tv_log)
        logScroll = findViewById(R.id.logScroll)

        loadWebhooks()
        updateListenerStatus()

        addWebhookButton.setOnClickListener {
            val input = EditText(this)
            input.hint = "Enter Discord webhook URL"

            val dialog = AlertDialog.Builder(this)
                .setTitle("Add Webhook")
                .setView(input)
                .setPositiveButton("Save") { _, _ ->
                    val url = input.text.toString().trim()
                    if (url.startsWith("https://discord.com/api/webhooks/")) {
                        addWebhook(url)
                    } else {
                        Toast.makeText(this, "Invalid Discord webhook!", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .create()

            dialog.show()
        }

        // register for notification and forward result broadcasts
        val f = IntentFilter()
        f.addAction("com.example.discordsignal.NOTIF_RECEIVED")
        f.addAction("com.example.discordsignal.FORWARD_RESULT")
        registerReceiver(receiver, f)
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(receiver) } catch (_: Exception) {}
    }

    private fun updateListenerStatus() {
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: ""
        val pkg = packageName
        val enabled = enabledListeners.contains(pkg)
        tvListenerStatus.text = if (enabled) "ENABLED" else "DISABLED"
        tvListenerStatus.setTextColor(if (enabled) 0xFF006400.toInt() else 0xFFFF0000.toInt())
        // if disabled, clicking the status opens the notification access settings
        tvListenerStatus.setOnClickListener {
            if (!enabled) {
                startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
            } else {
                Toast.makeText(this, "Notification access is enabled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun appendLog(line: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        runOnUiThread {
            tvLog.append("$time  $line\n")
            // scroll to bottom
            logScroll.post { logScroll.fullScroll(View.FOCUS_DOWN) }
        }
    }

    private fun short(a: String, b: String): String {
        val s = if (a.isNotEmpty()) a else b
        return if (s.length > 60) s.take(60) + "..." else s
    }

    private fun loadWebhooks() {
        webhookList.removeAllViews()
        val allWebhooks = prefs.getStringSet("urls", mutableSetOf()) ?: mutableSetOf()
        for (url in allWebhooks) {
            val layout = LinearLayout(this)
            layout.orientation = LinearLayout.HORIZONTAL

            val textView = TextView(this)
            textView.text = url.take(50) + if (url.length > 50) "..." else ""
            textView.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            val testButton = Button(this)
            testButton.text = "Test"
            testButton.setOnClickListener { sendDiscordMessage(url, "✅ Webhook test successful!") }

            val deleteButton = Button(this)
            deleteButton.text = "❌"
            deleteButton.setOnClickListener {
                removeWebhook(url)
            }

            layout.addView(textView)
            layout.addView(testButton)
            layout.addView(deleteButton)
            webhookList.addView(layout)
        }
    }

    private fun addWebhook(url: String) {
        val allWebhooks = prefs.getStringSet("urls", mutableSetOf()) ?: mutableSetOf()
        allWebhooks.add(url)
        prefs.edit().putStringSet("urls", allWebhooks).apply()
        loadWebhooks()
    }

    private fun removeWebhook(url: String) {
        val allWebhooks = prefs.getStringSet("urls", mutableSetOf()) ?: mutableSetOf()
        allWebhooks.remove(url)
        prefs.edit().putStringSet("urls", allWebhooks).apply()
        loadWebhooks()
    }

    private fun sendDiscordMessage(webhookUrl: String, content: String) {
        val json = JSONObject().apply {
            put("content", content)
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(webhookUrl).post(body).build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@MainActivity, "Sent successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Error: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
