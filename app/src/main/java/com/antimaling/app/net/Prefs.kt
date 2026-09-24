package com.antimaling.app.net

import android.content.Context

/** Penyimpanan sederhana pakai SharedPreferences: alamat server dan kredensial
 *  hasil pairing (apiKey). Sekali pairing berhasil, HP ini terus terhubung ke
 *  server itu tanpa perlu isi ulang. */
object Prefs {
    private const val NAME = "antimaling_prefs"

    fun serverUrl(ctx: Context): String =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString("server_url", "") ?: ""

    fun setServerUrl(ctx: Context, url: String) {
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putString("server_url", url.trimEnd('/')).apply()
    }

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
}
