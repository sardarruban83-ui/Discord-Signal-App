package com.example.discordsignal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.widget.ScrollView
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var btnOpenSettings: Button
    private lateinit var btnRefresh: Button
    private lateinit var tvLog: TextView
    private lateinit var scrollLog: ScrollView

    private val logReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            val pkg = intent.getStringExtra("pkg") ?: "?"
            val title = intent.getStringExtra("title") ?: ""
            val text = intent.getStringExtra("text") ?: ""
            val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            val line = "$time  [$pkg]  ${if (title.isNotEmpty()) title else text.take(60)}\n"
            runOnUiThread {
                tvLog.append(line)
                scrollLog.post { scrollLog.fullScroll(android.view.View.FOCUS_DOWN) }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // make sure this layout file exists and has the IDs used below
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        btnOpenSettings = findViewById(R.id.btnOpenSettings)
        btnRefresh = findViewById(R.id.btnRefresh)
        tvLog = findViewById(R.id.tv_log)
        scrollLog = findViewById(R.id.scroll_log)

        statusText.text = "App started"

        btnOpenSettings.setOnClickListener {
            // open notification access settings
            try {
                startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            } catch (e: Exception) {
                Toast.makeText(this, "Cannot open settings", Toast.LENGTH_SHORT).show()
            }
        }

        btnRefresh.setOnClickListener {
            // simple refresh text — you can replace with any action
            statusText.text = "Refreshed at " + java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        }

        // Register receiver — we'll register when activity starts
        val filter = android.content.IntentFilter("com.example.discordsignal.NOTIF_RECEIVED")
        registerReceiver(logReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(logReceiver)
        } catch (e: Exception) {
            // ignore
        }
    }
}
