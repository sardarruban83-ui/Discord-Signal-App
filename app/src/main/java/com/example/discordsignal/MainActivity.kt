package com.example.discordsignal

import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

            // existing init code goes here
        } catch (t: Throwable) {
            val sw = StringWriter()
            t.printStackTrace(PrintWriter(sw))
            val crashText = sw.toString()

            try {
                val file = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "tradelinker_crash.txt"
                )
                FileOutputStream(file, false).use { stream ->
                    stream.write(crashText.toByteArray())
                    stream.flush()
                    stream.fd.sync() // ensure it's saved before process ends
                }
            } catch (io: Exception) {
                io.printStackTrace()
            }

            Toast.makeText(
                this,
                "Crash saved to /Download/tradelinker_crash.txt",
                Toast.LENGTH_LONG
            ).show()

            android.util.Log.e("TradeLinker", "CRASH", t)
            throw t
        }
    }
}
