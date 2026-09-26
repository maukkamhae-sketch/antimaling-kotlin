package com.antimaling.app.net

import android.content.Context
<<<<<<< HEAD
=======
import android.util.Base64
import org.json.JSONArray
>>>>>>> dbc921da1a0a5d4da748f1f5eca9bffc4f7ffc7b
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

<<<<<<< HEAD
/** Semua panggilan ke backend AntiMaling. Sengaja pakai HttpURLConnection bawaan
 *  Android saja (tanpa Retrofit/OkHttp) supaya skeleton ini ringan dan gampang
 *  dibaca — silakan ganti ke Retrofit kalau proyeknya berkembang. */
=======
>>>>>>> dbc921da1a0a5d4da748f1f5eca9bffc4f7ffc7b
object Api {

    private fun call(ctx: Context, path: String, method: String, body: JSONObject?, useApiKey: Boolean): JSONObject {
        val base = Prefs.serverUrl(ctx)
        require(base.isNotBlank()) { "Server URL belum diatur." }
        val conn = URL(base + path).openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.setRequestProperty("Content-Type", "application/json")
        if (useApiKey) {
            val key = Prefs.apiKey(ctx) ?: throw IllegalStateException("Belum pairing dengan server.")
            conn.setRequestProperty("X-Api-Key", key)
        }
        if (body != null) {
            conn.doOutput = true
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
        }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.readText() ?: "{}"
        val json = try { JSONObject(text) } catch (e: Exception) { JSONObject() }
        if (code !in 200..299) throw RuntimeException(json.optString("error", "HTTP $code"))
        return json
    }

<<<<<<< HEAD
    /** Dipanggil sekali dari layar Pairing, pakai kode 6 digit dari dashboard. */
    fun pairClaim(ctx: Context, code: String): JSONObject =
        call(ctx, "/api/pair/claim", "POST", JSONObject().put("code", code), useApiKey = false)

    /** Dipanggil berkala oleh GuardService untuk cek ada perintah baru atau tidak. */
    fun fetchCommands(ctx: Context): JSONObject =
        call(ctx, "/api/device/commands", "GET", null, useApiKey = true)

    fun ackCommand(ctx: Context, commandId: String, result: String) {
        call(ctx, "/api/device/ack", "POST", JSONObject().put("commandId", commandId).put("result", result), useApiKey = true)
    }

    fun sendLocation(ctx: Context, lat: Double, lng: Double, accuracy: Float) {
        call(ctx, "/api/device/location", "POST",
=======
    fun pairClaim(ctx: Context, code: String): JSONObject =
        call(ctx, "/api/antimaling/pair/claim", "POST", JSONObject().put("code", code), useApiKey = false)

    fun fetchCommands(ctx: Context): JSONObject =
        call(ctx, "/api/antimaling/device/commands", "GET", null, useApiKey = true)

    fun ackCommand(ctx: Context, commandId: String, result: String) {
        call(ctx, "/api/antimaling/device/ack", "POST",
            JSONObject().put("commandId", commandId).put("result", result), useApiKey = true)
    }

    fun sendLocation(ctx: Context, lat: Double, lng: Double, accuracy: Float) {
        call(ctx, "/api/antimaling/device/location", "POST",
>>>>>>> dbc921da1a0a5d4da748f1f5eca9bffc4f7ffc7b
            JSONObject().put("lat", lat).put("lng", lng).put("accuracy", accuracy), useApiKey = true)
    }

    fun sendBattery(ctx: Context, percent: Int, charging: Boolean) {
<<<<<<< HEAD
        call(ctx, "/api/device/battery", "POST",
            JSONObject().put("percent", percent).put("charging", charging), useApiKey = true)
    }
=======
        call(ctx, "/api/antimaling/device/battery", "POST",
            JSONObject().put("percent", percent).put("charging", charging), useApiKey = true)
    }

    fun sendPhoto(ctx: Context, jpegBytes: ByteArray) {
        val b64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
        call(ctx, "/api/antimaling/device/photo", "POST",
            JSONObject().put("photo", b64), useApiKey = true)
    }

    fun fetchBlockedApps(ctx: Context): List<String> {
        val res = call(ctx, "/api/antimaling/device/blocked-apps", "GET", null, useApiKey = true)
        val arr = res.optJSONArray("packages") ?: return emptyList()
        return (0 until arr.length()).map { arr.getString(it) }
    }
>>>>>>> dbc921da1a0a5d4da748f1f5eca9bffc4f7ffc7b
}
