package com.antimaling.app.net

import android.content.Context
import android.util.Base64
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

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
            JSONObject().put("lat", lat).put("lng", lng).put("accuracy", accuracy), useApiKey = true)
    }

    fun sendBattery(ctx: Context, percent: Int, charging: Boolean) {
        call(ctx, "/api/antimaling/device/battery", "POST",
            JSONObject().put("percent", percent).put("charging", charging), useApiKey = true)
    }

    /** Upload foto dari kamera (base64 JPEG) ke server. */
    fun sendPhoto(ctx: Context, jpegBytes: ByteArray) {
        val b64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
        call(ctx, "/api/antimaling/device/photo", "POST",
            JSONObject().put("photo", b64), useApiKey = true)
    }
}
