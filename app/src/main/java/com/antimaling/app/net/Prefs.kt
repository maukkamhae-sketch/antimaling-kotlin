package com.antimaling.app.net

import android.content.Context
<<<<<<< HEAD

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
=======
import org.json.JSONArray

object Prefs {
    private const val NAME = "antimaling_prefs"

    fun serverUrl(ctx: Context): String = "https://alltools-backend-production.up.railway.app"
>>>>>>> dbc921da1a0a5d4da748f1f5eca9bffc4f7ffc7b

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
<<<<<<< HEAD
=======

    fun setLockPin(ctx: Context, pin: String) {
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putString("lock_pin", pin).apply()
    }

    fun lockPin(ctx: Context): String? =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString("lock_pin", null)

    fun getBlockedApps(ctx: Context): Set<String> {
        val json = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getString("blocked_apps", "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }.toSet()
        } catch (e: Exception) { emptySet() }
    }

    fun setBlockedApps(ctx: Context, packages: Set<String>) {
        val arr = JSONArray(packages.toList())
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit()
            .putString("blocked_apps", arr.toString()).apply()
    }
>>>>>>> dbc921da1a0a5d4da748f1f5eca9bffc4f7ffc7b
}
