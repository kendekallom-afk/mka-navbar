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
    private var triggerView: View? = null

    private val handler = Handler(Looper.getMainLooper())

    // Delay auto-hide: 2000ms
    private val hideDelay = 2000L
    private var isNavbarVisible = false

    private val hideRunnable = Runnable {
        hideNavbar()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            Settings.canDrawOverlays(this)
        ) {
            showTrigger()
            showNavbar() // tampilkan sekali di awal, lalu auto-hide
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Tidak perlu memproses event aplikasi.
    }

    override fun onInterrupt() {
        // Tidak ada tindakan khusus.
    }

    // =========================
    // AREA TRIGGER (SWIPE UP)
    // =========================

    private fun showTrigger() {
        if (triggerView != null) return

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val trigger = View(this)
        trigger.setBackgroundColor(Color.TRANSPARENT)

        val windowType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            dpToPx(20), // area sensitif swipe di bagian bawah layar
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL

        var startY = 0f
        var startX = 0f
        var swiping = false

        trigger.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startY = event.rawY
                    startX = event.rawX
                    swiping = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (swiping) {
                        val deltaY = startY - event.rawY
                        val deltaX = Math.abs(event.rawX - startX)

                        // Deteksi swipe ke atas minimal 30dp, hampir vertikal
                        if (deltaY > dpToPx(30) && deltaX < dpToPx(60)) {
                            swiping = false
                            showNavbar()
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    swiping = false
                    true
                }
                else -> false
            }
        }

        triggerView = trigger

        try {
            windowManager?.addView(trigger, params)
        } catch (e: Exception) {
            triggerView = null
        }
    }

    // =========================
    // NAVBAR
    // =========================

    private fun showNavbar() {

        // Batalkan jadwal hide sebelumnya (misal swipe berulang)
        handler.removeCallbacks(hideRunnable)

        if (navbarView != null) {
            navbarView?.visibility = View.VISIBLE
            isNavbarVisible = true
            scheduleHide()
            return
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val navbar = LinearLayout(this)

        navbar.orientation = LinearLayout.HORIZONTAL
        navbar.setBackgroundColor(Color.parseColor("#CC222222")) // sedikit transparan
        navbar.setPadding(8, 4, 8, 4)

        // =========================
        // TOMBOL RECENT APPS
        // =========================

        val recentButton = Button(this)

        recentButton.text = "□"
        recentButton.textSize = 20f
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

        homeButton.text = "○"
        homeButton.textSize = 20f
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

        backButton.text = "<"
        backButton.textSize = 20f
        backButton.setTextColor(Color.WHITE)
        backButton.setBackgroundColor(Color.TRANSPARENT)
        backButton.background = null
        backButton.contentDescription = "Back"

        backButton.setOnClickListener {
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

        // Swipe ke atas pada navbar sendiri juga memperpanjang waktu tampil
        navbar.setOnTouchListener(SwipeUpListener {
            scheduleHide()
        })

        val windowType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            dpToPx(40),
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL

        navbarView = navbar
        isNavbarVisible = true

        try {
            windowManager?.addView(navbar, params)
        } catch (e: Exception) {
            navbarView = null
            isNavbarVisible = false
        }

        scheduleHide()
    }

    private fun scheduleHide() {
        handler.removeCallbacks(hideRunnable)
        handler.postDelayed(hideRunnable, hideDelay)
    }

    private fun hideNavbar() {
        navbarView?.visibility = View.GONE
        isNavbarVisible = false
    }

    // =========================
    // DETEKSI SWIPE KE ATAS
    // =========================

    private inner class SwipeUpListener(
        private val onSwipeUp: () -> Unit
    ) : View.OnTouchListener {

        private var startY = 0f
        private var startX = 0f
        private var swiping = false

        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            if (event == null) return false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startY = event.rawY
                    startX = event.rawX
                    swiping = true
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (swiping) {
                        val deltaY = startY - event.rawY
                        val deltaX = Math.abs(event.rawX - startX)

                        if (deltaY > dpToPx(30) && deltaX < dpToPx(60)) {
                            swiping = false
                            onSwipeUp()
                        }
                    }
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    swiping = false
                    return false // biarkan klik tombol tetap jalan
                }
            }
            return false
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
            if (navbarView != null) {
                windowManager?.removeView(navbarView)
            }
            if (triggerView != null) {
                windowManager?.removeView(triggerView)
            }
        } catch (e: Exception) {
            // Abaikan jika view sudah tidak ada
        }

        navbarView = null
        triggerView = null
        windowManager = null
    }
}
