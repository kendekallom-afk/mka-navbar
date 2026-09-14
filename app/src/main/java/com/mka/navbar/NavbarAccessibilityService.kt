package com.mka.navbar

import android.accessibilityservice.AccessibilityService
import android.animation.ValueAnimator
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
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.LinearLayout

class NavbarAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var navbarView: View? = null
    private var hotspotView: View? = null
    private var params: WindowManager.LayoutParams? = null

    private var isNavbarVisible = false
    private val hideHandler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { hideNavbar() }

    // Tinggi navbar dalam dp
    private val navbarHeightDp = 40
    // Tinggi area hotspot di bawah layar (dp)
    private val hotspotHeightDp = 20
    // Waktu tunggu sebelum navbar sembunyi otomatis (2 detik)
    private val autoHideDelay = 2000L

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

        // =========================
        // MEMBUAT NAVBAR
        // =========================

        val navbar = LinearLayout(this)

        navbar.orientation = LinearLayout.HORIZONTAL
        navbar.setBackgroundColor(Color.TRANSPARENT)
        navbar.setPadding(8, 4, 8, 4)

        // Tombol Recent
        val recentButton = Button(this)
        recentButton.text = "□"
        recentButton.textSize = 24f
        recentButton.setTextColor(Color.WHITE)
        recentButton.setBackgroundColor(Color.TRANSPARENT)
        recentButton.background = null
        recentButton.contentDescription = "Recent Apps"
        recentButton.setOnClickListener { goRecent() }

        // Tombol Home
        val homeButton = Button(this)
        homeButton.text = "○"
        homeButton.textSize = 24f
        homeButton.setTextColor(Color.WHITE)
        homeButton.setBackgroundColor(Color.TRANSPARENT)
        homeButton.background = null
        homeButton.contentDescription = "Home"
        homeButton.setOnClickListener { goHome() }

        // Tombol Back
        val backButton = Button(this)
        backButton.text = "<"
        backButton.textSize = 24f
        backButton.setTextColor(Color.WHITE)
        backButton.setBackgroundColor(Color.TRANSPARENT)
        backButton.background = null
        backButton.contentDescription = "Back"
        backButton.setOnClickListener { goBack() }

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
        // PENGATURAN WINDOW NAVBAR
        // =========================

        val windowType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            dpToPx(navbarHeightDp),
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            // Mulai dalam keadaan tersembunyi penuh di bawah layar
            y = dpToPx(navbarHeightDp)
        }

        navbarView = navbar

        try {
            windowManager?.addView(navbar, params)
        } catch (e: Exception) {
            navbarView = null
        }

        // =========================
        // MEMBUAT HOTSPOT OVERLAY
        // =========================

        val hotspot = View(this)
        hotspot.setBackgroundColor(Color.TRANSPARENT)

        val hotspotParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            dpToPx(hotspotHeightDp),
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        }

        hotspotView = hotspot

        // =========================
        // AREA SENTUH HOTSPOT UNTUK SWIPE
        // =========================
        hotspot.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Batalkan auto-hide saat disentuh
                    hideHandler.removeCallbacks(hideRunnable)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    // Jika swipe ke atas, tampilkan navbar
                    if (event.rawY < resources.displayMetrics.heightPixels - dpToPx(100)) {
                        showNavbarAnimated()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Jadwalkan auto-hide
                    scheduleAutoHide()
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(hotspot, hotspotParams)
        } catch (e: Exception) {
            hotspotView = null
        }
    }

    // =========================
    // ANIMASI SHOW / HIDE
    // =========================

    private fun showNavbarAnimated() {
        if (isNavbarVisible) return
        isNavbarVisible = true

        val startY = dpToPx(navbarHeightDp)
        val endY = 0

        ValueAnimator.ofInt(startY, endY).apply {
            duration = 250
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                params?.y = animator.animatedValue as Int
                navbarView?.let { windowManager?.updateViewLayout(it, params) }
            }
            start()
        }

        scheduleAutoHide()
    }

    private fun hideNavbar() {
        if (!isNavbarVisible) return
        isNavbarVisible = false

        val startY = 0
        val endY = dpToPx(navbarHeightDp)

        ValueAnimator.ofInt(startY, endY).apply {
            duration = 250
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                params?.y = animator.animatedValue as Int
                navbarView?.let { windowManager?.updateViewLayout(it, params) }
            }
            start()
        }
    }

    private fun scheduleAutoHide() {
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, autoHideDelay)
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

        hideHandler.removeCallbacks(hideRunnable)

        try {
            if (navbarView != null) {
                windowManager?.removeView(navbarView)
            }
        } catch (e: Exception) {
            // Abaikan jika view sudah tidak ada
        }

        try {
            if (hotspotView != null) {
                windowManager?.removeView(hotspotView)
            }
        } catch (e: Exception) {
            // Abaikan jika view sudah tidak ada
        }

        navbarView = null
        hotspotView = null
        windowManager = null
        params = null
    }
}
