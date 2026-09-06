package com.mka.navbar

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import android.net.Uri
import android.widget.Button
import android.widget.LinearLayout

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 40, 40, 40)

        val title = Button(this)
        title.text = "MKA NAVBAR"

        val accessibilityButton = Button(this)
        accessibilityButton.text = "Aktifkan Accessibility Service"

        accessibilityButton.setOnClickListener {
            startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            )
        }

        val overlayButton = Button(this)
        overlayButton.text = "Izinkan Tampil di Atas Aplikasi"

        overlayButton.setOnClickListener {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }

        layout.addView(title)
        layout.addView(accessibilityButton)
        layout.addView(overlayButton)

        setContentView(layout)
    }
}
