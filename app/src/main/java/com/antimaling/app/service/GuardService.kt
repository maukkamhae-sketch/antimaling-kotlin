package com.antimaling.app.service

import android.app.*
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.AudioManager
import android.media.ImageReader
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.antimaling.app.net.Api
import com.antimaling.app.net.Prefs
import com.antimaling.app.receiver.DeviceAdminReceiverImpl
import com.antimaling.app.ui.LockScreenActivity
import com.google.android.gms.location.*
import org.json.JSONObject
import java.util.concurrent.Executors

class GuardService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var alarmPlayer: MediaPlayer? = null
    private lateinit var fusedClient: FusedLocationProviderClient

    private val pollRunnable = object : Runnable {
        override fun run() {
            Thread { pollOnce() }.start()
            handler.postDelayed(this, 15_000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        startForeground(1, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handler.removeCallbacks(pollRunnable)
        handler.post(pollRunnable)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(pollRunnable)
        alarmPlayer?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val channelId = "antimaling_guard"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(NotificationChannel(channelId, "AntiMaling aktif", NotificationManager.IMPORTANCE_LOW))
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("AntiMaling aktif")
            .setContentText("HP ini terlindungi dan bisa dilacak/dikunci dari dashboard.")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()
    }

    private fun pollOnce() {
        if (!Prefs.isPaired(this)) return
        try {
            val res = Api.fetchCommands(this)
            val commands = res.optJSONArray("commands") ?: return
            for (i in 0 until commands.length()) {
                handleCommand(commands.getJSONObject(i))
            }
        } catch (e: Exception) { }
        reportLocationAndBattery()
    }

    private fun handleCommand(cmd: JSONObject) {
        val id = cmd.optString("id")
        val type = cmd.optString("type")
        val result = try {
            when (type) {
                "lock"       -> { lockNow(cmd.optString("pin")); "locked" }
                "alarm"      -> { startAlarm(); "alarm_on" }
                "stop_alarm" -> { stopAlarm(); "alarm_off" }
                "locate"     -> { requestFreshLocation(); "locating" }
                "photo"      -> { captureAndUploadPhoto(); "photo_sent" }
                "wipe"       -> { wipeDevice(); "wiping" }
                else         -> "unknown_command"
            }
        } catch (e: Exception) {
            "error: ${e.message}"
        }
        try { Api.ackCommand(this, id, result) } catch (e: Exception) { }
        showDebugNotif("Command: $type -> $result")
    }

    private fun lockNow(pin: String) {
        if (pin.isBlank()) throw IllegalArgumentException("PIN kosong dari server")
        Prefs.setLockPin(this, pin)
        flashTorch(3)
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(this, DeviceAdminReceiverImpl::class.java)
        if (dpm.isAdminActive(admin)) dpm.lockNow() else throw IllegalStateException("Device Admin belum aktif")
        val intent = Intent(this, LockScreenActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    private fun flashTorch(times: Int) {
        try {
            val cm = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val camId = cm.cameraIdList.firstOrNull {
                cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return
            repeat(times) {
                cm.setTorchMode(camId, true);  Thread.sleep(300)
                cm.setTorchMode(camId, false); Thread.sleep(200)
            }
        } catch (e: Exception) { }
    }

    /** Jepret kamera depan diam-diam lalu upload ke server.
     *  Karena CameraX membutuhkan lifecycle, di sini kita pakai Camera2 langsung
     *  biar bisa dipanggil dari background thread (GuardService). */
    @Suppress("MissingPermission")
    private fun captureAndUploadPhoto() {
        val cm = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        // Cari ID kamera depan
        val frontId = cm.cameraIdList.firstOrNull { id ->
            cm.getCameraCharacteristics(id)
                .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
        } ?: throw IllegalStateException("Tidak ada kamera depan")

        val handlerThread = HandlerThread("CameraCapture").also { it.start() }
        val camHandler = Handler(handlerThread.looper)

        val imageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 1)
        var jpegBytes: ByteArray? = null

        // Tunggu frame pertama dari ImageReader
        val latch = java.util.concurrent.CountDownLatch(1)
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            val buf = image.planes[0].buffer
            jpegBytes = ByteArray(buf.remaining()).also { buf.get(it) }
            image.close()
            latch.countDown()
        }, camHandler)

        var cameraDevice: CameraDevice? = null
        cm.openCamera(frontId, object : CameraDevice.StateCallback() {
            override fun onOpened(cam: CameraDevice) {
                cameraDevice = cam
                val surface = imageReader.surface
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val outConfig = OutputConfiguration(surface)
                    val sessionConfig = SessionConfiguration(
                        SessionConfiguration.SESSION_REGULAR,
                        listOf(outConfig),
                        Executors.newSingleThreadExecutor(),
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                val req = cam.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                                    addTarget(surface)
                                }.build()
                                session.capture(req, null, camHandler)
                            }
                            override fun onConfigureFailed(session: CameraCaptureSession) { latch.countDown() }
                        })
                    cam.createCaptureSession(sessionConfig)
                } else {
                    @Suppress("DEPRECATION")
                    cam.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            val req = cam.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                                addTarget(surface)
                            }.build()
                            session.capture(req, null, camHandler)
                        }
                        override fun onConfigureFailed(session: CameraCaptureSession) { latch.countDown() }
                    }, camHandler)
                }
            }
            override fun onDisconnected(cam: CameraDevice) { cam.close(); latch.countDown() }
            override fun onError(cam: CameraDevice, error: Int) { cam.close(); latch.countDown() }
        }, camHandler)

        // Tunggu maksimal 8 detik buat foto selesai
        latch.await(8, java.util.concurrent.TimeUnit.SECONDS)
        cameraDevice?.close()
        imageReader.close()
        handlerThread.quitSafely()

        val bytes = jpegBytes ?: throw IllegalStateException("Gagal capture foto")
        Api.sendPhoto(this, bytes)
    }

    private fun wipeDevice() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(this, DeviceAdminReceiverImpl::class.java)
        if (dpm.isAdminActive(admin)) dpm.wipeData(0) else throw IllegalStateException("Device Admin belum aktif")
    }

    private fun startAlarm() {
        if (alarmPlayer?.isPlaying == true) return
        val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        alarmPlayer = MediaPlayer().apply {
            setAudioStreamType(AudioManager.STREAM_ALARM)
            setDataSource(this@GuardService, uri)
            isLooping = true
            prepare(); start()
        }
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.setStreamVolume(AudioManager.STREAM_ALARM, am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0)
    }

    private fun stopAlarm() {
        alarmPlayer?.let { if (it.isPlaying) it.stop(); it.release() }
        alarmPlayer = null
    }

    @Suppress("MissingPermission")
    private fun requestFreshLocation() {
        val req = CurrentLocationRequest.Builder().setPriority(Priority.PRIORITY_HIGH_ACCURACY).build()
        fusedClient.getCurrentLocation(req, null).addOnSuccessListener { loc ->
            if (loc != null) {
                try { Api.sendLocation(this, loc.latitude, loc.longitude, loc.accuracy) } catch (e: Exception) { }
            }
        }
    }

    @Suppress("MissingPermission")
    private fun reportLocationAndBattery() {
        try {
            val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val percent = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val charging = bm.isCharging
            Api.sendBattery(this, percent, charging)
        } catch (e: Exception) { }
        try {
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    try { Api.sendLocation(this, loc.latitude, loc.longitude, loc.accuracy) } catch (e: Exception) { }
                }
            }
        } catch (e: Exception) { }
    }

    private fun showDebugNotif(msg: String) {
        val channelId = "antimaling_debug"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(NotificationChannel(channelId, "AntiMaling debug", NotificationManager.IMPORTANCE_HIGH))
        }
        val notif = NotificationCompat.Builder(this, channelId)
            .setContentTitle("AntiMaling debug")
            .setContentText(msg)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(999, notif)
    }
}
