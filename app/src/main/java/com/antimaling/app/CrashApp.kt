package com.antimaling.app

import android.app.Application
import android.content.Intent
import android.os.Process
import android.util.Log
import com.antimaling.app.ui.CrashViewActivity
import java.io.File
import kotlin.system.exitProcess

/** Perekam crash sementara: kalau app force close, simpan isi error ke file
 *  lalu tampilkan di layar khusus (proses terpisah) supaya bisa disalin. */
class CrashApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            try {
                File(filesDir, "crash.txt").writeText(Log.getStackTraceString(e))
                startActivity(
                    Intent(this, CrashViewActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
                Thread.sleep(600)
            } catch (_: Throwable) { }
            Process.killProcess(Process.myPid())
            exitProcess(10)
        }
    }
}
