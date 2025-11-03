package com.example.discordsignal

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var logReceiver: android.content.BroadcastReceiver
    private lateinit var tvLog: android.widget.TextView
  

    private lateinit var statusTv: TextView
    private lateinit var btnOpenSettings: Button
    private lateinit var btnRefresh: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        // keep crash-catcher from your previous version
        try {
            super.onCreate(savedInstanceState)
registerReceiver(NotifLogReceiver(findViewById(R.id.tv_log), findViewById(R.id.scroll_log)), android.content.IntentFilter("com.example.discordsignal.NOTIF_RECEIVED"))            setContentView(R.layout.activity_main)

            statusTv = findViewById(R.id.statusText)
            btnOpenSettings = findViewById(R.id.btnOpenSettings)
            btnRefresh = findViewById(R.id.btnRefresh)

            btnOpenSettings.setOnClickListener {
                // Open Notification access settings
                try {
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                } catch (e: Exception) {
                    Toast.makeText(this, "Cannot open settings: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            btnRefresh.setOnClickListener {
                updateStatus()
            }

            // initial status
            updateStatus()

        } catch (t: Throwable) {
            // write minimal crash log to internal storage and rethrow
            try {
                openFileOutput("crash_log.txt", MODE_PRIVATE).use { it.write(t.stackTraceToString().toByteArray()) }
            } catch (_: Exception) { /* ignore */ }
            Toast.makeText(this, "App crashed on start — crash log saved", Toast.LENGTH_LONG).show()
            throw t
        }
    }

    private fun updateStatus() {
        val enabled = isNotificationListenerEnabled()
        if (enabled) {
            statusTv.text = "Notification access: ✅ Registered"
        } else {
            statusTv.text = "Notification access: ❌ Not registered"
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        return try {
            val pkgName = packageName
            val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: ""
            flat.split(":").any { it.contains(pkgName) }
        } catch (e: Exception) {
            false
        }
    }
}
