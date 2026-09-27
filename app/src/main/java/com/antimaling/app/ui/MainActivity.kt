package com.antimaling.app.ui

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.antimaling.app.databinding.ActivityMainBinding
import com.antimaling.app.net.Api
import com.antimaling.app.net.Prefs
import com.antimaling.app.receiver.DeviceAdminReceiverImpl
import com.antimaling.app.service.AppBlockerService
import com.antimaling.app.service.GuardService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requestLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            requestBackgroundLocationIfNeeded()
        } else {
            toast("Izin lokasi wajib diberikan supaya HP bisa dilacak kalau hilang.")
        }
    }

    private val requestDeviceAdmin = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refreshStatus() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPair.setOnClickListener { doPairing() }
        binding.btnGrantLocation.setOnClickListener { requestLocationPermissions() }
        binding.btnGrantAdmin.setOnClickListener { requestDeviceAdminPrompt() }
        binding.btnGrantAccessibility.setOnClickListener { openAccessibilitySettings() }
        binding.btnStartService.setOnClickListener { startGuardService() }

        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun doPairing() {
        val code = binding.inputPairCode.text.toString().trim()
        if (code.isBlank()) { toast("Isi kode pairing dulu."); return }
        binding.btnPair.isEnabled = false
        Thread {
            try {
                val res = Api.pairClaim(this, code)
                Prefs.savePairing(this, res.getString("apiKey"), res.optString("name", "HP saya"))
                runOnUiThread { toast("Pairing berhasil! Lanjut kasih izin di bawah."); refreshStatus() }
            } catch (e: Exception) {
                runOnUiThread { toast("Gagal pairing: ${e.message}") }
            } finally {
                runOnUiThread { binding.btnPair.isEnabled = true }
            }
        }.start()
    }

    private fun requestLocationPermissions() {
        requestLocationPermission.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    private fun requestBackgroundLocationIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), 100)
            }
        }
        refreshStatus()
    }

    private fun requestDeviceAdminPrompt() {
        val admin = ComponentName(this, DeviceAdminReceiverImpl::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "AntiMaling butuh ini supaya bisa mengunci atau menghapus data HP ini kalau hilang/dicuri.")
        }
        requestDeviceAdmin.launch(intent)
    }

    private fun openAccessibilitySettings() {
        toast("Cari 'AntiMaling' → 'AntiMaling Blokir App', lalu aktifkan.")
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun isAccessibilityEnabled(): Boolean {
        return try {
            val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
            enabled.contains(packageName + "/" + AppBlockerService::class.java.name)
        } catch (e: Exception) { false }
    }

    private fun startGuardService() {
        if (!Prefs.isPaired(this)) { toast("Pairing dulu sebelum menyalakan perlindungan."); return }
        ContextCompat.startForegroundService(this, Intent(this, GuardService::class.java))
        toast("AntiMaling aktif melindungi HP ini.")
        refreshStatus()
    }

    private fun refreshStatus() {
        val paired = Prefs.isPaired(this)
        val hasLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val dpm = getSystemService(DevicePolicyManager::class.java)
        val hasAdmin = dpm.isAdminActive(ComponentName(this, DeviceAdminReceiverImpl::class.java))
        val hasAccessibility = isAccessibilityEnabled()

        binding.statusPairing.text = if (paired) "✅ Sudah pairing (${Prefs.deviceName(this)})" else "⬜ Belum pairing"
        binding.statusLocation.text = if (hasLocation) "✅ Izin lokasi diberikan" else "⬜ Izin lokasi belum diberikan"
        binding.statusAdmin.text = if (hasAdmin) "✅ Device Admin aktif" else "⬜ Device Admin belum aktif"
        binding.statusAccessibility.text = if (hasAccessibility) "✅ Blokir app aktif" else "⬜ Aksesibilitas blokir app belum aktif"
        binding.btnStartService.isEnabled = paired && hasLocation && hasAdmin
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
