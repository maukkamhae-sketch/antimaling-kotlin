package com.antimaling.app.ui

<<<<<<< HEAD
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/** Placeholder: DevicePolicyManager.lockNow() sudah cukup untuk mengunci layar
 *  memakai lock screen bawaan sistem (PIN/pola/sidik jari milik pemilik HP).
 *  Activity ini disediakan sebagai tempat kalau nanti mau menambah layar kustom
 *  (misalnya menampilkan pesan "HP ini dilaporkan hilang, hubungi ..."). */
class LockScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
=======
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.antimaling.app.R
import com.antimaling.app.net.Prefs

/** Layar kunci kustom AntiMaling.
 *
 *  Dipicu GuardService saat menerima perintah "lock" dari dashboard.
 *  Beda dari dpm.lockNow() (yang cuma memakai lock method bawaan HP milik
 *  pemilik), activity ini adalah lapisan TAMBAHAN: begitu HP dibuka lagi,
 *  layar ini nongol di atas semuanya dan minta PIN yang di-set dari
 *  dashboard AntiMaling (bukan PIN asli HP).
 *
 *  Anti-bypass: pakai startLockTask() (screen pinning bawaan Android) supaya
 *  tombol Home/Recents ikut ke-blok selama layar ini aktif — biasanya orang
 *  gak tau cara lepas pin-nya (harus tahan Back + Recents bareng), jadi ini
 *  jauh lebih susah dihindari dibanding sebelumnya.
 *
 *  Catatan keterbatasan (skeleton, bukan Device Owner/kiosk penuh):
 *  screen pinning ini masih bisa dilepas paksa lewat kombinasi tombol Back +
 *  Recents ditahan bareng (fitur bawaan OS). Untuk anti-bypass 100% (gak ada
 *  cara keluar sama sekali kecuali PIN benar), app perlu didaftarkan sebagai
 *  Device Owner lewat provisioning (QR code) saat HP baru/abis factory reset,
 *  lalu pakai setLockTaskFeatures() buat matiin kombinasi lepas-pin itu juga. */
class LockScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Tampil di atas lock screen & nyalakan layar walau HP lagi tidur.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        // Sembunyikan status bar & nav bar (immersive) biar gak gampang
        // ditarik buat buka notification shade / quick settings.
        hideSystemBars()

        // Kalau ternyata belum pernah dapat PIN sama sekali (kasus aneh/bug),
        // jangan kunci pengguna tanpa jalan keluar — langsung tutup saja.
        val expectedPin = Prefs.lockPin(this)
        if (expectedPin.isNullOrBlank()) { finish(); return }

        setContentView(R.layout.activity_lock_screen)

        // Pin activity ini di layar: Home/Recents jadi diblok selama aktif.
        startLockTaskIfPossible()

        val pinInput = findViewById<EditText>(R.id.pinInput)
        val errorText = findViewById<TextView>(R.id.lockError)
        val unlockButton = findViewById<Button>(R.id.unlockButton)

        unlockButton.setOnClickListener {
            val typed = pinInput.text.toString().trim()
            if (typed.isNotEmpty() && typed == expectedPin) {
                stopLockTaskIfPossible()
                finish()
            } else {
                errorText.visibility = TextView.VISIBLE
                pinInput.text.clear()
            }
        }
    }

    private fun hideSystemBars() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN
        )
    }

    private fun startLockTaskIfPossible() {
        try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            // Hindari mulai lockTask dua kali kalau activity di-recreate.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || am.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) {
                startLockTask()
            }
        } catch (e: Exception) {
            // Kalau OS/OEM tertentu menolak (jarang), tetap lanjut — layar
            // PIN masih tampil, cuma tanpa proteksi anti Home/Recents.
        }
    }

    private fun stopLockTaskIfPossible() {
        try { stopLockTask() } catch (e: Exception) { }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    /** Back sengaja diblok: HP ini lagi mode "hilang", jangan biarkan orang
     *  yang pegang HP kabur dari layar ini cuma dengan tombol Back. */
    override fun onBackPressed() {
        // no-op dengan sengaja
>>>>>>> dbc921da1a0a5d4da748f1f5eca9bffc4f7ffc7b
    }
}
