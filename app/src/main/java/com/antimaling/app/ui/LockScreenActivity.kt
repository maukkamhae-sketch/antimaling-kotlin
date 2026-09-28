package com.antimaling.app.ui

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.antimaling.app.R
import com.antimaling.app.net.Api
import com.antimaling.app.net.Prefs
import java.io.ByteArrayOutputStream
import android.graphics.BitmapFactory
import android.graphics.Matrix

/** Layar kunci kustom AntiMaling.
 *
 *  Dipicu GuardService saat menerima perintah "lock" dari dashboard.
 *  Beda dari dpm.lockNow() (yang cuma memakai lock method bawaan HP milik
 *  pemilik), activity ini adalah lapisan TAMBAHAN: begitu HP dibuka lagi,
 *  layar ini nongol di atas semuanya dan minta PIN yang di-set dari
 *  dashboard AntiMaling (bukan PIN asli HP).
 *
 *  Anti-bypass: dua lapis.
 *  1) startLockTask() (screen pinning bawaan Android) — blokir Home/Recents
 *     di OS yang menghormatinya secara penuh.
 *  2) Jaga-jaga kalau screen pinning ditembus (beberapa OEM/skin custom
 *     seperti MIUI kadang masih bisa dilepas lewat Recents): onPause() di
 *     bawah langsung menjadwalkan activity ini kebuka lagi dalam waktu
 *     sangat singkat selama belum di-unlock dengan PIN yang benar.
 *
 *  Catatan keterbatasan (skeleton, bukan Device Owner/kiosk penuh):
 *  kombinasi ini membuat sangat sulit (praktis tidak mungkin bagi orang awam)
 *  buat kabur dari layar ini, tapi belum 100% seperti Device Owner kiosk
 *  mode. Untuk anti-bypass total, app perlu didaftarkan sebagai Device Owner
 *  lewat provisioning (QR code) saat HP baru/abis factory reset. */
class LockScreenActivity : AppCompatActivity() {

    private var unlocked = false
    private val relaunchHandler = Handler(Looper.getMainLooper())
    private var wrongAttempts = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        hideSystemBars()

        val expectedPin = Prefs.lockPin(this)
        if (expectedPin.isNullOrBlank()) { unlocked = true; finish(); return }

        setContentView(R.layout.activity_lock_screen)
        startLockTaskIfPossible()

        val pinInput = findViewById<EditText>(R.id.pinInput)
        val errorText = findViewById<TextView>(R.id.lockError)
        val unlockButton = findViewById<Button>(R.id.unlockButton)

        unlockButton.setOnClickListener {
            val typed = pinInput.text.toString().trim()
            if (typed.isNotEmpty() && typed == expectedPin) {
                unlocked = true
                stopLockTaskIfPossible()
                finish()
            } else {
                errorText.visibility = TextView.VISIBLE
                pinInput.text.clear()
                wrongAttempts++
                captureIntruderPhoto()
            }
        }
    }

    private fun hideSystemBars() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN
        )
    }

    private fun startLockTaskIfPossible() {
        try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || am.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) {
                startLockTask()
            }
        } catch (e: Exception) { /* lanjut tanpa pinning kalau OS menolak */ }
    }

    private fun stopLockTaskIfPossible() {
        try { stopLockTask() } catch (e: Exception) { }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    /** Lapis kedua anti-bypass: kalau activity ini kehilangan fokus (misal
     *  ke-switch lewat Recents di HP yang screen pinning-nya gampang ditembus)
     *  padahal belum di-unlock dengan benar, langsung jadwalkan diri sendiri
     *  buka lagi secepat mungkin. */
    override fun onPause() {
        super.onPause()
        if (!unlocked) {
            relaunchHandler.postDelayed({
                if (!unlocked) {
                    val i = Intent(this, LockScreenActivity::class.java)
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    startActivity(i)
                }
            }, 250)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        relaunchHandler.removeCallbacksAndMessages(null)
    }

    /** Jepret diam-diam dari kamera depan, tanpa preview, lalu upload sebagai
     *  base64 JPEG ke backend. Gagal (misal kamera dipakai app lain, atau HP
     *  gak punya kamera depan) sengaja diredam — jangan sampai bikin lock
     *  screen crash cuma gara-gara ini. */
    private fun captureIntruderPhoto() {
        try {
            val providerFuture = ProcessCameraProvider.getInstance(this)
            providerFuture.addListener({
                try {
                    val provider = providerFuture.get()
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    provider.unbindAll()
                    provider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, capture)
                    capture.takePicture(
                        ContextCompat.getMainExecutor(this),
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                val base64 = imageProxyToBase64Jpeg(image)
                                image.close()
                                provider.unbindAll()
                                if (base64 != null) {
                                    Thread {
                                        try { Api.sendIntruderPhoto(this@LockScreenActivity, base64) }
                                        catch (e: Exception) { Log.w("LockScreen", "Gagal kirim foto: ${e.message}") }
                                    }.start()
                                }
                            }
                            override fun onError(exc: ImageCaptureException) {
                                provider.unbindAll()
                            }
                        }
                    )
                } catch (e: Exception) { /* kamera gak bisa dipakai, biarkan */ }
            }, ContextCompat.getMainExecutor(this))
        } catch (e: Exception) { /* CameraX gak tersedia, biarkan */ }
    }

    private fun imageProxyToBase64Jpeg(image: ImageProxy): String? {
        return try {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val rotation = image.imageInfo.rotationDegrees
            if (rotation != 0) {
                val m = Matrix().apply { postRotate(rotation.toFloat()) }
                bitmap = android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
            }
            val out = ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, out)
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) { null }
    }

    /** Back sengaja diblok: HP ini lagi mode "hilang", jangan biarkan orang
     *  yang pegang HP kabur dari layar ini cuma dengan tombol Back. */
    override fun onBackPressed() {
        // no-op dengan sengaja
    }
}
