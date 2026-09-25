package com.antimaling.app.net

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Semua panggilan ke backend AntiMaling. Sengaja pakai HttpURLConnection bawaan
 *  Android saja (tanpa Retrofit/OkHttp) supaya skeleton ini ringan dan gampang
 *  dibaca — silakan ganti ke Retrofit kalau proyeknya berkembang. */
object Api {

    private fun call(ctx: Context, path: String, method: String, body: JSONObject?, useApiKey: Boolean): JSONObject {
        val base = Prefs.serverUrl(ctx).trim().trimEnd('/')
        require(base.isNotBlank()) { "Server URL belum diatur." }
        val fullUrl = base + path
        val conn = URL(fullUrl).openConnection() as HttpURLConnection
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
        if (code !in 200..299) throw RuntimeException(json.optString("error", "HTTP $code di $fullUrl"))
        return json
    }

    /** Dipanggil sekali dari layar Pairing, pakai kode 6 digit dari dashboard. */
    fun pairClaim(ctx: Context, code: String): JSONObject =
        call(ctx, "/api/antimaling/pair/claim", "POST", JSONObject().put("code", code), useApiKey = false)

    /** Dipanggil berkala oleh GuardService untuk cek ada perintah baru atau tidak. */
    fun fetchCommands(ctx: Context): JSONObject =
        call(ctx, "/api/antimaling/device/commands", "GET", null, useApiKey = true)

    fun ackCommand(ctx: Context, commandId: String, result: String) {
        call(ctx, "/api/antimaling/device/ack", "POST", JSONObject().put("commandId", commandId).put("result", result), useApiKey = true)
    }

    fun sendLocation(ctx: Context, lat: Double, lng: Double, accuracy: Float) {
        call(ctx, "/api/antimaling/device/location", "POST",
            JSONObject().put("lat", lat).put("lng", lng).put("accuracy", accuracy), useApiKey = true)
    }

    fun sendBattery(ctx: Context, percent: Int, charging: Boolean) {
        call(ctx, "/api/antimaling/device/battery", "POST",
            JSONObject().put("percent", percent).put("charging", charging), useApiKey = true)
    }
}
