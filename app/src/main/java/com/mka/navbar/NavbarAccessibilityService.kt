package com.mka.navbar

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class NavbarAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Tidak perlu memproses event aplikasi.
    }

    override fun onInterrupt() {
        // Tidak ada tindakan khusus.
    }

    fun goBack() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun goHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun goRecent() {
        performGlobalAction(GLOBAL_ACTION_RECENTS)
    }
}
