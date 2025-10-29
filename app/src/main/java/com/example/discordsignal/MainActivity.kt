package com.example.discordsignal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

            findViewById<TextView>(R.id.statusText)?.text = "TradeLinker ready — open app settings to enable Notification Access"

            // provide quick button to notification access settings
            val open = Button(this).apply { text = "Open Notification Access" }
            (findViewById(android.R.id.content) as? android.view.ViewGroup)?.addView(open)
            open.setOnClickListener {
                try {
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                } catch (e: Exception) {
                    Toast.makeText(this, "Cannot open settings: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (t: Throwable) {
            val sw = StringWriter()
            t.printStackTrace(PrintWriter(sw))
            val stack = sw.toString()
            try {
                openFileOutput("crash_log.txt", MODE_PRIVATE).use {
                    it.write(stack.toByteArray())
                }
            } catch (_: Exception) {}
            Toast.makeText(this, "Startup crash logged", Toast.LENGTH_LONG).show()
            throw t
        }
    }
}
