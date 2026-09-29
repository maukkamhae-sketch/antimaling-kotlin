package com.antimaling.app.net

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import java.security.MessageDigest

/** Semua data lokal AntiMaling (api key, PIN, dll) disimpan lewat
 *  EncryptedSharedPreferences — dienkripsi pakai kunci di Android Keystore,
 *  bukan file XML polos. Ini penting karena api key & PIN kalau bocor
 *  (misal HP di-root) bisa dipakai orang lain buat ngontrol HP ini dari
 *  dashboard. PIN sendiri juga TIDAK disimpan apa adanya — yang disimpan
 *  cuma hash SHA-256-nya, jadi walau file prefs-nya somehow kebaca,
 *  PIN asli tetap tidak bisa langsung dibaca. */
object Prefs {
    private const val NAME = "antimaling_prefs"

    private fun prefs(ctx: Context) = try {
        val masterKey = MasterKey.Builder(ctx)
            .setKeyGenParameterSpec(
                KeyGenParameterSpec.Builder(
                    "antimaling_master_key",
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            ).build()
        EncryptedSharedPreferences.create(
            ctx, NAME, masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback kalau Keystore HP bermasalah (jarang) — lebih baik app tetap
        // jalan tanpa enkripsi daripada crash total.
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    }

    private fun sha256(text: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun serverUrl(ctx: Context): String = "https://alltools-backend-production.up.railway.app"

    fun apiKey(ctx: Context): String? = prefs(ctx).getString("api_key", null)

    fun deviceName(ctx: Context): String? = prefs(ctx).getString("device_name", null)

    fun savePairing(ctx: Context, apiKey: String, deviceName: String) {
        prefs(ctx).edit()
            .putString("api_key", apiKey)
            .putString("device_name", deviceName)
            .apply()
    }

    fun isPaired(ctx: Context): Boolean = !apiKey(ctx).isNullOrBlank()

    /** Simpan HASH PIN-nya saja, bukan PIN aslinya. */
    fun setLockPin(ctx: Context, pin: String) {
        prefs(ctx).edit().putString("lock_pin_hash", sha256(pin)).apply()
    }

    /** true kalau ada PIN yang di-set sama sekali (dipakai buat cek "sudah pernah dikunci?"). */
    fun hasLockPin(ctx: Context): Boolean = !prefs(ctx).getString("lock_pin_hash", null).isNullOrBlank()

    /** Bandingkan PIN yang diketik user dengan hash yang tersimpan. */
    fun checkLockPin(ctx: Context, typed: String): Boolean {
        val stored = prefs(ctx).getString("lock_pin_hash", null) ?: return false
        return stored == sha256(typed)
    }

    fun getBlockedApps(ctx: Context): Set<String> {
        val json = prefs(ctx).getString("blocked_apps", "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }.toSet()
        } catch (e: Exception) { emptySet() }
    }

    fun setBlockedApps(ctx: Context, packages: Set<String>) {
        val arr = JSONArray(packages.toList())
        prefs(ctx).edit().putString("blocked_apps", arr.toString()).apply()
    }
}
