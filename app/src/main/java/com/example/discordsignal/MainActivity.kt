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
    private var tvLog: TextView? = null
    private var scrollLog: ScrollView? = null
    private var btnOpenSettings: Button? = null
    private var btnRefresh: Button? = null
    private var statusText: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // set your layout (ensure activity_main.xml exists)
        setContentView(R.layout.activity_main)

        // runtime-safe id lookup: avoids compile-time dependency on missing R.id fields
        fun id(name: String) : Int {
            val resId = resources.getIdentifier(name, "id", packageName)
            return if (resId != 0) resId else View.NO_ID
        }

        try {
            val tvId = id("tv_log")
            if (tvId != View.NO_ID) tvLog = findViewById(tvId)
        } catch (_: Exception) {}

        try {
            val scrollId = id("scroll_log")
            if (scrollId != View.NO_ID) scrollLog = findViewById(scrollId)
        } catch (_: Exception) {}

        try {
            val b1 = id("btn_open_settings")
            if (b1 != View.NO_ID) btnOpenSettings = findViewById(b1)
        } catch (_: Exception) {}

        try {
            val b2 = id("btn_refresh")
            if (b2 != View.NO_ID) btnRefresh = findViewById(b2)
        } catch (_: Exception) {}

        try {
            val s = id("statusText")
            if (s != View.NO_ID) statusText = findViewById(s)
        } catch (_: Exception) {}

        // Register receiver to show live notifications forwarded by NotificationListener
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
                        tvLog?.append(line)
                        scrollLog?.post { scrollLog?.fullScroll(View.FOCUS_DOWN) }
                    }
                } catch (_: Exception) {}
            }
        }
        registerReceiver(logReceiver, IntentFilter("com.example.discordsignal.NOTIF_RECEIVED"))
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(logReceiver) } catch (_: Exception) {}
    }
}
