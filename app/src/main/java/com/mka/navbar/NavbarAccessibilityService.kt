package com.mka.navbar

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams

class NavbarAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var navbarView: LinearLayout? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("MKA_NAVBAR", "onServiceConnected → mencoba menampilkan navbar")
        showNavbar()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    private fun showNavbar() {
        if (navbarView != null) return

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val navbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#CC000000"))
            setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
        }

        val backBtn = createButton("<") { performGlobalAction(GLOBAL_ACTION_BACK) }
        val homeBtn = createButton("○") { performGlobalAction(GLOBAL_ACTION_HOME) }
        val recentBtn = createButton("□") { performGlobalAction(GLOBAL_ACTION_RECENTS) }

        val params = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
        navbar.addView(backBtn, params)
        navbar.addView(homeBtn, params)
        navbar.addView(recentBtn, params)

        // ===== INI YANG PENTING =====
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            dpToPx(52),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,   // ← wajib pakai ini
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        }

        try {
            windowManager?.addView(navbar, layoutParams)
            navbarView = navbar
            Log.d("MKA_NAVBAR", "Navbar berhasil ditambahkan")
        } catch (e: Exception) {
            Log.e("MKA_NAVBAR", "Gagal menambahkan navbar", e)
        }
    }

    private fun createButton(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            textSize = 22f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { onClick() }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            navbarView?.let { windowManager?.removeView(it) }
        } catch (_: Exception) {}
        navbarView = null
        windowManager = null
    }
}
