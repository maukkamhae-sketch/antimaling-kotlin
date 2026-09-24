package com.antimaling.app.ui

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
    }
}
