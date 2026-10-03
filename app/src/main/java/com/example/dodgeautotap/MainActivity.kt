package com.example.dodgeautotap

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            gravity = Gravity.CENTER_HORIZONTAL

            setPadding(
                35,
                60,
                35,
                40
            )

            setBackgroundColor(
                Color.rgb(9, 11, 16)
            )
        }

        val title =
            TextView(this).apply {

                text = "⚡ Dodge Auto Tap"

                textSize = 30f

                setTextColor(
                    Color.WHITE
                )

                gravity = Gravity.CENTER

                setPadding(
                    0,
                    0,
                    0,
                    20
                )
            }

        val info =
            TextView(this).apply {

                text =
                    "Uygulama hazır.\n\n" +
                    "Önce Erişilebilirlik servisini aç.\n" +
                    "Servis açıldıktan sonra ekranda " +
                    "sürüklenebilir D düğmesi görünecek.\n\n" +
                    "Auto Tap başlangıçta KAPALI'dır."

                textSize = 16f

                setTextColor(
                    Color.rgb(
                        185,
                        190,
                        205
                    )
                )

                gravity = Gravity.CENTER

                setPadding(
                    0,
                    0,
                    0,
                    30
                )
            }

        val settings =
            Button(this).apply {

                text =
                    "♿ Erişilebilirlik Ayarlarını Aç"

                setOnClickListener {

                    startActivity(
                        Intent(
                            Settings.ACTION_ACCESSIBILITY_SETTINGS
                        )
                    )
                }
            }

        root.addView(title)
        root.addView(info)
        root.addView(settings)

        setContentView(root)
    }
}
