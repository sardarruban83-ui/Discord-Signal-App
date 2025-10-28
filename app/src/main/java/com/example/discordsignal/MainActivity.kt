package com.example.discordsignal

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val webhookInput = findViewById<EditText>(R.id.webhook_input)
        val keywordsInput = findViewById<EditText>(R.id.keywords_input)
        val allowPackages = findViewById<EditText>(R.id.allow_input)
        val saveBtn = findViewById<Button>(R.id.save_button)
        val openNotifAccess = findViewById<Button>(R.id.open_notif_button)

        webhookInput.setText(prefs.getString("webhook_url", ""))
        keywordsInput.setText(prefs.getString("keywords", ""))
        allowPackages.setText(prefs.getString("allow_packages", ""))

        saveBtn.setOnClickListener {
            prefs.edit()
                .putString("webhook_url", webhookInput.text.toString().trim())
                .putString("keywords", keywordsInput.text.toString().trim())
                .putString("allow_packages", allowPackages.text.toString().trim())
                .apply()
        }

        openNotifAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }
}
