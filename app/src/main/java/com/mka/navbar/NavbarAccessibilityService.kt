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

class NavbarAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var navbarView: View? = null
    private var gestureStrip: View? = null

    private val handler = Handler(Looper.getMainLooper())
    private val AUTO_HIDE_DELAY = 2000L
    private var isVisible = true

    private val hideRunnable = Runnable { hideNavbar() }

    // untuk deteksi swipe
    private var startY = 0f
    private val SWIPE_THRESHOLD = 80f

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

        // =========================
        // NAVBAR UTAMA
        // =========================
        val navbar = LinearLayout(this)
        navbar.orientation = LinearLayout.HORIZONTAL
        navbar.setBackgroundColor(Color.TRANSPARENT)
        navbar.setPadding(8, 4, 8, 4)

        // TOMBOL RECENT
        val recentButton = Button(this)
        recentButton.text = "□"
        recentButton.textSize = 20f
        recentButton.setTextColor(Color.WHITE)
        recentButton.setBackgroundColor(Color.TRANSPARENT)
        recentButton.minimumWidth = 0
        recentButton.minimumHeight = 0
        recentButton.contentDescription = "Recent Apps"
        recentButton.setOnClickListener {
            resetHideTimer()
            goRecent()
        }

        // TOMBOL HOME
        val homeButton = Button(this)
        homeButton.text = "○"
        homeButton.textSize = 20f
        homeButton.setTextColor(Color.WHITE)
        homeButton.setBackgroundColor(Color.TRANSPARENT)
        homeButton.minimumWidth = 0
        homeButton.minimumHeight = 0
        homeButton.contentDescription = "Home"
        homeButton.setOnClickListener {
            resetHideTimer()
            goHome()
        }

        // TOMBOL BACK
        val backButton = Button(this)
        backButton.text = "<"
        backButton.textSize = 20f
        backButton.setTextColor(Color.WHITE)
        backButton.setBackgroundColor(Color.TRANSPARENT)
        backButton.minimumWidth = 0
        backButton.minimumHeight = 0
        backButton.background = null
        backButton.contentDescription = "Back"
        backButton.setOnClickListener {
            resetHideTimer()
            goBack()
        }

        val buttonParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.MATCH_PARENT,
            1f
        )

        navbar.addView(recentButton, buttonParams)
        navbar.addView(homeButton, buttonParams)
        navbar.addView(backButton, buttonParams)

        // =========================
        // STRIP DETEKSI SWIPE (selalu ada)
        // =========================
        val strip = View(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val deltaY = startY - event.rawY
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

        // Parameter navbar
        val navbarParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            dpToPx(40),
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        }

        // Parameter strip (tinggi kecil di paling bawah)
        val stripParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            dpToPx(16),
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        }

        navbarView = navbar
        gestureStrip = strip

        try {
            windowManager?.addView(strip, stripParams)
            windowManager?.addView(navbar, navbarParams)
            isVisible = true
            startHideTimer()
        } catch (e: Exception) {
            navbarView = null
            gestureStrip = null
        }
    }

    private fun showNavbarFull() {
        if (isVisible) {
            resetHideTimer()
            return
        }

        navbarView?.let { view ->
            view.visibility = View.VISIBLE
            view.translationY = view.height.toFloat()
            view.animate()
                .translationY(0f)
                .setDuration(200)
                .withEndAction {
                    isVisible = true
                    startHideTimer()
                }
                .start()
        }
    }

    private fun hideNavbar() {
        if (!isVisible) return

        navbarView?.let { view ->
            view.animate()
                .translationY(view.height.toFloat())
                .setDuration(200)
                .withEndAction {
                    view.visibility = View.GONE
                    isVisible = false
                }
                .start()
        }
    }

    private fun startHideTimer() {
        handler.removeCallbacks(hideRunnable)
        handler.postDelayed(hideRunnable, AUTO_HIDE_DELAY)
    }

    private fun resetHideTimer() {
        if (isVisible) {
            startHideTimer()
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
