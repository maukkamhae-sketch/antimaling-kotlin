package com.antimaling.app.net

import android.content.Context

/** Penyimpanan sederhana pakai SharedPreferences: alamat server dan kredensial
 *  hasil pairing (apiKey). Sekali pairing berhasil, HP ini terus terhubung ke
 *  server itu tanpa perlu isi ulang. */
object Prefs {
    private const val NAME = "antimaling_prefs"

    // Server sudah tetap (server AllTools), jadi user tidak perlu isi alamat lagi —
    // cukup kode pairing dari halaman AntiMaling di app AllTools.
    fun serverUrl(ctx: Context): String = "https://alltools-backend-production.up.railway.app"

    fun apiKey(ctx: Context): String? =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString("api_key", null)

    fun deviceName(ctx: Context): String? =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString("device_name", null)

    fun savePairing(ctx: Context, apiKey: String, deviceName: String) {
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit()
            .putString("api_key", apiKey)
            .putString("device_name", deviceName)
            .apply()
    }

    fun isPaired(ctx: Context): Boolean = !apiKey(ctx).isNullOrBlank()

    /** PIN untuk buka LockScreenActivity, dikirim server tiap kali dashboard
     *  mengirim perintah "lock". Disimpan lokal supaya layar kunci bisa
     *  memverifikasinya walau HP sedang offline saat dibuka. */
    fun setLockPin(ctx: Context, pin: String) {
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putString("lock_pin", pin).apply()
    }

    fun lockPin(ctx: Context): String? =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString("lock_pin", null)
}
