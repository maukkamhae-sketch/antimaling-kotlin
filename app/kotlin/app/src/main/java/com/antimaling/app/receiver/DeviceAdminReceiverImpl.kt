package com.antimaling.app.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/** Wajib ada supaya Android mengizinkan app ini jadi Device Admin. Tanpa ini,
 *  DevicePolicyManager.lockNow() dan wipeData() akan ditolak sistem. User harus
 *  mengaktifkan ini secara manual lewat Settings (dipandu dari MainActivity),
 *  bukan otomatis — supaya jelas ini persetujuan sadar dari pemilik HP. */
class DeviceAdminReceiverImpl : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "AntiMaling: Device Admin diaktifkan.", Toast.LENGTH_SHORT).show()
    }
    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "AntiMaling: Device Admin dimatikan — fitur kunci/wipe tidak aktif.", Toast.LENGTH_LONG).show()
    }
}
