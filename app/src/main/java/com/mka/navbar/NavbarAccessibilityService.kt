package com.mka.navbar

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import kotlin.math.abs

class NavbarAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var navbarView: View? = null
    private var gestureStrip: View? = null          // area tipis untuk deteksi swipe

    private val handler = Handler(Looper.getMainLooper())
    private val AUTO_HIDE_DELAY = 2000L
    private var isNavbarVisible = false

    private val hideRunnable = Runnable { hideNavbar() }

    // untuk deteksi swipe
    private var startY = 0f
    private val SWIPE_THRESHOLD = 80f               // minimal jarak swipe ke atas

    override fun onServiceConnected() {
        super.onServiceConnected()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            Settings.canDrawOverlays(this)
        ) {
            showNavbar()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    private fun showNavbar() {
        if (navbarView != null) return

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // ========== NAVBAR UTAMA ==========
        val navbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#CC000000"))
            setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
            elevation = dpToPx(8).toFloat()
        }

        val backButton = createNavButton("<") {
            resetHideTimer()
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
        val homeButton = createNavButton("○") {
            resetHideTimer()
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
        val recentButton = createNavButton("□") {
            resetHideTimer()
            performGlobalAction(GLOBAL_ACTION_RECENTS)
        }

        val buttonParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
        navbar.addView(backButton, buttonParams)
        navbar.addView(homeButton, buttonParams)
        navbar.addView(recentButton, buttonParams)

        // ========== GESTURE STRIP (selalu ada di paling bawah) ==========
        val strip = View(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val deltaY = startY - event.rawY   // positif = swipe ke atas
                        if (deltaY > SWIPE_THRESHOLD) {
                            showNavbarFull()
                        }
                        true
                    }
                    else -> false
                }
            }
        }

        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // Parameter untuk navbar utama
        val navbarParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            dpToPx(48),
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        }

        // Parameter untuk strip deteksi swipe (tinggi sangat kecil)
        val stripParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            dpToPx(16),                             // area sensitif swipe
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        }

        navbarView = navbar
        gestureStrip = strip

        try {
            // Strip dulu (di belakang), lalu navbar
            windowManager?.addView(strip, stripParams)
            windowManager?.addView(navbar, navbarParams)
            isNavbarVisible = true
            startHideTimer()
        } catch (e: Exception) {
            navbarView = null
            gestureStrip = null
            e.printStackTrace()
        }
    }

    private fun showNavbarFull() {
        if (isNavbarVisible) {
            resetHideTimer()
            return
        }

        navbarView?.let { view ->
            view.visibility = View.VISIBLE
            view.translationY = view.height.toFloat()   // mulai dari bawah
            view.animate()
                .translationY(0f)
                .setDuration(220)
                .withEndAction {
                    isNavbarVisible = true
                    startHideTimer()
                }
                .start()
        }
    }

    private fun hideNavbar() {
        if (!isNavbarVisible) return

        navbarView?.let { view ->
            view.animate()
                .translationY(view.height.toFloat())
                .setDuration(220)
                .withEndAction {
                    view.visibility = View.GONE
                    isNavbarVisible = false
                }
                .start()
        }
    }

    private fun startHideTimer() {
        handler.removeCallbacks(hideRunnable)
        handler.postDelayed(hideRunnable, AUTO_HIDE_DELAY)
    }

    private fun resetHideTimer() {
        if (isNavbarVisible) {
            startHideTimer()
        }
    }

    private fun createNavButton(symbol: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = symbol
            textSize = 22f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            contentDescription = when (symbol) {
                "<" -> "Back"
                "○" -> "Home"
                else -> "Recent Apps"
            }
            setOnClickListener { onClick() }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(hideRunnable)
        try {
            navbarView?.let { windowManager?.removeView(it) }
            gestureStrip?.let { windowManager?.removeView(it) }
        } catch (_: Exception) {}
        navbarView = null
        gestureStrip = null
        windowManager = null
    }
}
