package com.antimaling.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.antimaling.app.net.Prefs
import com.antimaling.app.ui.BlockedAppActivity

class AppBlockerService : AccessibilityService() {

    override fun onServiceConnected() {
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        // Jangan blokir diri sendiri atau system UI
        if (pkg == applicationContext.packageName) return
        if (pkg == "com.android.systemui" || pkg == "com.android.launcher") return

        val blocked = Prefs.getBlockedApps(applicationContext)
        if (pkg in blocked) {
            performGlobalAction(GLOBAL_ACTION_HOME)
            val intent = Intent(applicationContext, BlockedAppActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("pkg", pkg)
            }
            startActivity(intent)
        }
    }

    override fun onInterrupt() {}
}
