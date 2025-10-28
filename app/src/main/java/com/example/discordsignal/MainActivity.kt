package com.example.discordsignal

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

            try {
                // write to internal file so you can read it from Files app => Android/data/<package>/files/
                openFileOutput("crash_log.txt", MODE_PRIVATE).use {
                    it.write(stack.toByteArray())
                }
            } catch (io: Exception) {
                // ignore write errors
            }

            // show toast so you know it crashed and log written
            Toast.makeText(
                this,
                "App crashed on start — crash log saved to internal file (crash_log.txt)",
                Toast.LENGTH_LONG
            ).show()

            // rethrow so behavior is same as before (optional)
            throw t
        }
    }
}
