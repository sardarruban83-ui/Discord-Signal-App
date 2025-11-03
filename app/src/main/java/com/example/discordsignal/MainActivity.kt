package com.example.discordsignal

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.content.SharedPreferences
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var webhookList: LinearLayout
    private lateinit var addWebhookButton: Button
    private lateinit var prefs: SharedPreferences
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("webhooks", MODE_PRIVATE)
        webhookList = findViewById(R.id.webhookList)
        addWebhookButton = findViewById(R.id.addWebhook)

        loadWebhooks()

        addWebhookButton.setOnClickListener {
            val input = EditText(this)
            input.hint = "Enter Discord webhook URL"

            AlertDialog.Builder(this)
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
                .show()
        }
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
        val json = JSONObject()
        json.put("content", content)

        val body = RequestBody.create(MediaType.parse("application/json"), json.toString())
        val request = Request.Builder().url(webhookUrl).post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Failed: ${e.message}", Toast.LENGTH_SHORT).show() }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@MainActivity, "Sent successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    // future: connect with your notification filter here
}
