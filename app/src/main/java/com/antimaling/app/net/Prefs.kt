package com.antimaling.app.net

import android.content.Context
import org.json.JSONArray

object Prefs {
    private const val NAME = "antimaling_prefs"

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
}
