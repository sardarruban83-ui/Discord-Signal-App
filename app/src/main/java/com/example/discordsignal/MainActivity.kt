package com.example.discordsignal

import android.os.Bundle
import android.content.IntentFilter
import android.widget.TextView
import android.widget.Button
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.View

class MainActivity : AppCompatActivity() {

    private lateinit var logReceiver: BroadcastReceiver
    private lateinit var tvLog: TextView
    private lateinit var scrollLog: ScrollView
    private lateinit var btnOpenSettings: Button
    private lateinit var btnRefresh: Button
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ensure activity_main exists in res/layout — keep your layout filename
        setContentView(R.layout.activity_main)

        // safe findViewById — if view ids are missing this won't crash at compile time
        try {
            tvLog = findViewById(R.id.tv_log)
        } catch (e: Exception) { /* ignore if missing in layout */ }
        try {
            scrollLog = findViewById(R.id.scroll_log)
        } catch (e: Exception) { /* ignore */ }
        try {
            btnOpenSettings = findViewById(R.id.btn_open_settings)
        } catch (e: Exception) { /* ignore */ }
        try {
            btnRefresh = findViewById(R.id.btn_refresh)
        } catch (e: Exception) { /* ignore */ }
        try {
            statusText = findViewById(R.id.statusText)
        } catch (e: Exception) { /* ignore */ }

        // register a small receiver so your app can display live notifications if you broadcast them
        logReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null) return
                val pkg = intent.getStringExtra("pkg") ?: "?"
                val title = intent.getStringExtra("title") ?: ""
                val text = intent.getStringExtra("text") ?: ""
                val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                val line = "$time  [$pkg]  ${if (title.isNotEmpty()) title else text.take(60)}\n"
                try {
                    runOnUiThread {
                        if (::tvLog.isInitialized) {
                            tvLog.append(line)
                            if (::scrollLog.isInitialized) {
                                scrollLog.post { scrollLog.fullScroll(View.FOCUS_DOWN) }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
        registerReceiver(logReceiver, IntentFilter("com.example.discordsignal.NOTIF_RECEIVED"))
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(logReceiver) } catch (e: Exception) { /* ignore */ }
    }
}
