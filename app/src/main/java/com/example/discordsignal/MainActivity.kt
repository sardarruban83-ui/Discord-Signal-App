package com.example.discordsignal

import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

            // Your existing initialization code here
        } catch (t: Throwable) {
            val sw = StringWriter()
            t.printStackTrace(PrintWriter(sw))
            val crashText = sw.toString()

            try {
                val file = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "tradelinker_crash.txt"
                )
                file.writeText(crashText)
            } catch (io: Exception) {
                io.printStackTrace()
            }

            Toast.makeText(
                this,
                "Startup crash logged to Download/tradelinker_crash.txt",
                Toast.LENGTH_LONG
            ).show()

            throw t
        }
    }
}
