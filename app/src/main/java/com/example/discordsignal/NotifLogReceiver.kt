package com.example.discordsignal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.ScrollView
import android.widget.TextView
import java.lang.ref.WeakReference

/**
 * NotifLogReceiver
 * A small BroadcastReceiver that appends a one-line timestamped log to a TextView (tv_log).
 * Register with IntentFilter("com.example.discordsignal.NOTIF_RECEIVED")
 *
 * Construct with weak references to UI elements to avoid leaks.
 */
class NotifLogReceiver(
    tv: TextView?,
    scroll: ScrollView?
) : BroadcastReceiver() {

    private val tvRef = WeakReference(tv)
    private val scrollRef = WeakReference(scroll)

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return
        val pkg = intent.getStringExtra("pkg") ?: "?"
        val title = intent.getStringExtra("title") ?: ""
        val text = intent.getStringExtra("text") ?: ""
        val short = if (title.isNotEmpty()) title else text.take(100)
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val line = "$time  [$pkg]  $short\n"

        val tv = tvRef.get()
        val scroll = scrollRef.get()

        // safely post to UI thread if view still exists
        tv?.post {
            try {
                tv.append(line)
                // keep scroll at bottom
                scroll?.post { scroll.fullScroll(android.view.View.FOCUS_DOWN) }
            } catch (_: Exception) { /* ignore */ }
        }
    }
}
