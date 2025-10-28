package com.example.discordsignal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var actionButton: Button

    private val newNotificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val text = intent?.getStringExtra("text") ?: "New notification received"
            Toast.makeText(this@MainActivity, text, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        actionButton = findViewById(R.id.actionButton)

        actionButton.setOnClickListener {
            if (!isNotificationListenerEnabled()) {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Already allowed — app is listening", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateUiState()
        registerReceiver(newNotificationReceiver, IntentFilter("com.example.discordsignal.NEW_NOTIFICATION"))
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(newNotificationReceiver)
        } catch (e: IllegalArgumentException) { }
    }

    private fun updateUiState() {
        if (isNotificationListenerEnabled()) {
            statusText.text = "Listening for notifications..."
            actionButton.text = "Notification access granted"
            actionButton.isEnabled = false
        } else {
            statusText.text = "This app needs notification access to work"
            actionButton.text = "GRANT NOTIFICATION ACCESS"
            actionButton.isEnabled = true
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(this)
        return enabledPackages.contains(packageName)
    }
}
