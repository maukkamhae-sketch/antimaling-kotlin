package com.antimaling.app.ui

import android.os.Build
import android.os.Bundle
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
 *  Catatan keterbatasan (skeleton, bukan Device Owner/kiosk penuh):
 *  tombol Back sudah diblok di sini, tapi tanpa mode Device Owner,
 *  tombol Home/Recents tetap bisa dipakai OS untuk keluar activity ini.
 *  Untuk anti-bypass penuh, app perlu didaftarkan sebagai Device Owner
 *  lewat provisioning (QR code) dan pakai startLockTask() (kiosk mode). */
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

        // Kalau ternyata belum pernah dapat PIN sama sekali (kasus aneh/bug),
        // jangan kunci pengguna tanpa jalan keluar — langsung tutup saja.
        val expectedPin = Prefs.lockPin(this)
        if (expectedPin.isNullOrBlank()) { finish(); return }

        setContentView(R.layout.activity_lock_screen)

        val pinInput = findViewById<EditText>(R.id.pinInput)
        val errorText = findViewById<TextView>(R.id.lockError)
        val unlockButton = findViewById<Button>(R.id.unlockButton)

        unlockButton.setOnClickListener {
            val typed = pinInput.text.toString().trim()
            if (typed.isNotEmpty() && typed == expectedPin) {
                finish()
            } else {
                errorText.visibility = TextView.VISIBLE
                pinInput.text.clear()
            }
        }
    }

    /** Back sengaja diblok: HP ini lagi mode "hilang", jangan biarkan orang
     *  yang pegang HP kabur dari layar ini cuma dengan tombol Back. */
    override fun onBackPressed() {
        // no-op dengan sengaja
    }
}
