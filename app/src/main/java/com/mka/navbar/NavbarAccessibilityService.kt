package com.mka.navbar

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.LinearLayout

class NavbarAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var navbarView: View? = null

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Tampilkan navbar jika izin overlay sudah diberikan
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            Settings.canDrawOverlays(this)
        ) {
            showNavbar()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Tidak perlu memproses event aplikasi.
    }

    override fun onInterrupt() {
        // Tidak ada tindakan khusus.
    }

    private fun showNavbar() {

        // Jangan membuat navbar dua kali
        if (navbarView != null) {
            return
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val navbar = LinearLayout(this)

        navbar.orientation = LinearLayout.HORIZONTAL
        navbar.setBackgroundColor(Color.rgb(30, 30, 30))
        navbar.setPadding(8, 4, 8, 4)

        // =========================
        // TOMBOL RECENT APPS
        // =========================

        val recentButton = Button(this)

        recentButton.text = "▣"
        recentButton.textSize = 24f
        recentButton.setTextColor(Color.WHITE)
        recentButton.setBackgroundColor(Color.TRANSPARENT)
        recentButton.contentDescription = "Recent Apps"

        recentButton.setOnClickListener {
            goRecent()
        }

        // =========================
        // TOMBOL HOME
        // =========================

        val homeButton = Button(this)

        homeButton.text = "●"
        homeButton.textSize = 24f
        homeButton.setTextColor(Color.WHITE)
        homeButton.setBackgroundColor(Color.TRANSPARENT)
        homeButton.contentDescription = "Home"

        homeButton.setOnClickListener {
            goHome()
        }

        // =========================
        // TOMBOL BACK
        // =========================

        val backButton = Button(this)

        backButton.text = "◀"
        backButton.textSize = 24f
        backButton.setTextColor(Color.WHITE)
        backButton.setBackgroundColor(Color.TRANSPARENT)
        backButton.contentDescription = "Back"

        backButton.setOnClickListener {
            goBack()
        }

        // Setiap tombol mendapat lebar yang sama
        val buttonParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.MATCH_PARENT,
            1f
        )

        navbar.addView(recentButton, buttonParams)
        navbar.addView(homeButton, buttonParams)
        navbar.addView(backButton, buttonParams)

        // =========================
        // PENGATURAN WINDOW
        // =========================

        val windowType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            dpToPx(60),
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL

        navbarView = navbar

        try {
            windowManager?.addView(navbar, params)
        } catch (e: Exception) {
            navbarView = null
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
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

    override fun onDestroy() {
        super.onDestroy()

        try {
            if (navbarView != null) {
                windowManager?.removeView(navbarView)
            }
        } catch (e: Exception) {
            // Abaikan jika view sudah tidak ada
        }

        navbarView = null
        windowManager = null
    }
}
