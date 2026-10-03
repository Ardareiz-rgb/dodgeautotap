package com.example.dodgeautotap

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 60, 40, 40)
        }

        val title = TextView(this).apply {
            text = "Dodge Auto Tap"
            textSize = 28f
        }

        val info = TextView(this).apply {
            text = "\n1. Accessibility Service'i aç.\n2. Oyuna dön.\n3. Dodge düğmesi görünürken otomatik dokunmayı kullan.\n"
            textSize = 16f
        }

        val openSettings = TextView(this).apply {
            text = "♿ Erişilebilirlik ayarlarını aç"
            textSize = 18f
            setPadding(0, 30, 0, 30)
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        val toggle = Switch(this).apply {
            text = "Dodge Auto Tap"
            textSize = 18f
            isChecked = DodgeAccessibilityService.enabled
            setOnCheckedChangeListener { _, checked ->
                DodgeAccessibilityService.enabled = checked
            }
        }

        layout.addView(title)
        layout.addView(info)
        layout.addView(openSettings)
        layout.addView(toggle)
        setContentView(layout)
    }
}
