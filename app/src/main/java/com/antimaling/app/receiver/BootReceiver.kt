package com.antimaling.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.antimaling.app.net.Prefs
import com.antimaling.app.service.GuardService

/** Kalau HP dimatikan lalu dinyalakan lagi (misalnya pencuri mencoba restart
 *  untuk mematikan pelacakan), service ini otomatis jalan lagi — tapi HANYA
 *  kalau HP sudah pernah di-pairing sebelumnya oleh pemiliknya. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && Prefs.isPaired(context)) {
            val svc = Intent(context, GuardService::class.java)
            ContextCompat.startForegroundService(context, svc)
        }
    }
}
