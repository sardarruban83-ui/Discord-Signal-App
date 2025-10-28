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
        // Catch any startup crash and write it to a file for inspection
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

            // existing init code (if any) goes after this line
            // Example: findViewById<Button>(R.id.myButton)?.setOnClickListener { ... }

        } catch (t: Throwable) {
            // get full stacktrace
            val sw = StringWriter()
            t.printStackTrace(PrintWriter(sw))
            val stack = sw.toString()

            // 1) Write to internal app file (original)
            try {
                openFileOutput("crash_log.txt", MODE_PRIVATE).use {
                    it.write(stack.toByteArray())
                }
            } catch (io: Exception) {
                // ignore write errors
            }

            // 2) ALSO try to write to public Downloads so you can fetch it easily
            try {
                val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val outFile = File(downloads, "tradelinker_crash_log.txt")
                outFile.writeText(stack)
            } catch (io: Exception) {
                // ignore; this may fail on some Android versions, but usually works for debug builds
            }

            // show toast so you know it crashed and log written
            Toast.makeText(
                this,
                "App crashed on start — crash log saved (crash_log.txt or tradelinker_crash_log.txt)",
                Toast.LENGTH_LONG
            ).show()

            // rethrow so behavior is same as before (optional)
            throw t
        }
    }
}
