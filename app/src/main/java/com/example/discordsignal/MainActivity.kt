package com.example.discordsignal

import android.Manifest
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : AppCompatActivity() {
    private val TAG = "TradeLinkerMain"

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

            // 1) write to internal file (app internal storage)
            try {
                openFileOutput("crash_log.txt", MODE_PRIVATE).use {
                    it.write(stack.toByteArray())
                }
            } catch (io: Exception) {
                // ignore
                Log.e(TAG, "Failed writing internal crash_log: ${io.message}")
            }

            // 2) attempt to write to public Download folder (best-effort - may be blocked on newer Android)
            try {
                val download = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val outFile = File(download, "tradelinker_crash_log.txt")
                FileOutputStream(outFile, true).use { fos ->
                    fos.write(stack.toByteArray())
                }
            } catch (io: Exception) {
                Log.e(TAG, "Failed writing to /sdcard/Download (may need permission or restricted on this Android): ${io.message}")
            }

            // 3) also log to logcat (so adb/logcat will show)
            Log.e(TAG, "Startup crash — stacktrace:\n$stack")

            // show toast (user-visible)
            Toast.makeText(
                this,
                "App crashed on start — crash log saved (best-effort). Check logcat or Downloads.",
                Toast.LENGTH_LONG
            ).show()

            // rethrow so behavior is same as before (optional)
            throw t
        }
    }
}
