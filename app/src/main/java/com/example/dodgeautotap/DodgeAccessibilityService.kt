package com.example.dodgeautotap

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.view.animation.DecelerateInterpolator

class DodgeAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile var enabled = false
        private var lastTap = 0L
    }

    private lateinit var windowManager: WindowManager

    private var floatingButton: TextView? = null
    private var panel: LinearLayout? = null
    private var buttonParams: WindowManager.LayoutParams? = null

    private val handler = Handler(Looper.getMainLooper())

    private val autoTapRunnable = object : Runnable {
        override fun run() {
            if (enabled) {
                tryFindAndTapDodge()
            }

            handler.postDelayed(this, 100)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        windowManager =
            getSystemService(WINDOW_SERVICE) as WindowManager

        showFloatingButton()

        handler.removeCallbacks(autoTapRunnable)
        handler.post(autoTapRunnable)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (enabled) {
            tryFindAndTapDodge()
        }
    }

    private fun tryFindAndTapDodge() {

        val now = SystemClock.uptimeMillis()

        if (now - lastTap < 180)
            return

        val root = rootInActiveWindow ?: return

        val node = findDodge(root) ?: return

        val rect = Rect()
        node.getBoundsInScreen(rect)

        if (rect.width() <= 0 || rect.height() <= 0)
            return

        /*
         * Önce normal Accessibility click deniyoruz.
         */
        if (node.isClickable) {

            val clicked =
                node.performAction(
                    AccessibilityNodeInfo.ACTION_CLICK
                )

            if (clicked) {
                lastTap = now
                return
            }
        }

        /*
         * ACTION_CLICK çalışmazsa koordinata
         * gesture gönderiyoruz.
         */
        val x = rect.centerX().toFloat()
        val y = rect.centerY().toFloat()

        val path = Path().apply {
            moveTo(x, y)
        }

        val gesture =
            GestureDescription.Builder()
                .addStroke(
                    GestureDescription.StrokeDescription(
                        path,
                        0,
                        40
                    )
                )
                .build()

        lastTap = now

        dispatchGesture(
            gesture,
            null,
            null
        )
    }

    private fun findDodge(
        node: AccessibilityNodeInfo
    ): AccessibilityNodeInfo? {

        val text =
            node.text?.toString()?.trim()

        val desc =
            node.contentDescription?.toString()?.trim()

        if (
            text.equals("Dodge", ignoreCase = true) ||
            desc.equals("Dodge", ignoreCase = true)
        ) {
            return node
        }

        for (i in 0 until node.childCount) {

            val child =
                node.getChild(i) ?: continue

            val result =
                findDodge(child)

            if (result != null)
                return result
        }

        return null
    }

    private fun showFloatingButton() {

        if (floatingButton != null)
            return

        val button = TextView(this)

        button.text = "D"
        button.textSize = 20f
        button.setTextColor(Color.WHITE)
        button.gravity = Gravity.CENTER

        button.background =
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(35, 120, 255))
                setStroke(
                    3,
                    Color.rgb(100, 180, 255)
                )
            }

        button.elevation = 20f

        val size = dp(58)

        val params =
            WindowManager.LayoutParams(
                size,
                size,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

        params.gravity =
            Gravity.TOP or Gravity.START

        params.x = dp(20)
        params.y = dp(180)

        buttonParams = params

        button.setOnTouchListener(
            FloatingTouchListener()
        )

        floatingButton = button

        windowManager.addView(
            button,
            params
        )

        button.scaleX = 0f
        button.scaleY = 0f
        button.alpha = 0f

        button.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(350)
            .setInterpolator(
                DecelerateInterpolator()
            )
            .start()
    }

    private inner class FloatingTouchListener :
        View.OnTouchListener {

        private var downX = 0f
        private var downY = 0f

        private var startX = 0
        private var startY = 0

        private var moved = false

        override fun onTouch(
            v: View,
            event: MotionEvent
        ): Boolean {

            val params =
                buttonParams ?: return false

            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {

                    downX = event.rawX
                    downY = event.rawY

                    startX = params.x
                    startY = params.y

                    moved = false

                    v.animate()
                        .scaleX(.88f)
                        .scaleY(.88f)
                        .setDuration(80)
                        .start()

                    return true
                }

                MotionEvent.ACTION_MOVE -> {

                    val dx =
                        (event.rawX - downX).toInt()

                    val dy =
                        (event.rawY - downY).toInt()

                    if (
                        kotlin.math.abs(dx) > 8 ||
                        kotlin.math.abs(dy) > 8
                    ) {
                        moved = true
                    }

                    params.x = startX + dx
                    params.y = startY + dy

                    windowManager.updateViewLayout(
                        v,
                        params
                    )

                    return true
                }

                MotionEvent.ACTION_UP -> {

                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()

                    if (!moved) {
                        togglePanel()
                    }

                    return true
                }
            }

            return false
        }
    }

    private fun togglePanel() {

        if (panel != null) {
            hidePanel()
        } else {
            showPanel()
        }
    }

    private fun showPanel() {

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(18),
                    dp(16),
                    dp(18),
                    dp(16)
                )

                background =
                    GradientDrawable().apply {

                        setColor(
                            Color.rgb(18, 21, 29)
                        )

                        cornerRadius =
                            dp(18).toFloat()

                        setStroke(
                            dp(1),
                            Color.rgb(65, 140, 255)
                        )
                    }

                elevation = 25f
            }

        val title =
            TextView(this).apply {

                text =
                    "⚡ Dodge Auto Tap"

                textSize = 18f

                setTextColor(
                    Color.WHITE
                )

                setPadding(
                    0,
                    0,
                    0,
                    dp(10)
                )
            }

        val status =
            TextView(this).apply {

                text =
                    if (enabled)
                        "● AUTO TAP: AÇIK"
                    else
                        "● AUTO TAP: KAPALI"

                textSize = 14f

                setTextColor(
                    if (enabled)
                        Color.rgb(70, 220, 130)
                    else
                        Color.rgb(230, 80, 90)
                )
            }

        val toggle =
            Button(this).apply {

                text =
                    if (enabled)
                        "AUTO TAP'I KAPAT"
                    else
                        "AUTO TAP'I AÇ"

                setOnClickListener {

                    enabled = !enabled

                    status.text =
                        if (enabled)
                            "● AUTO TAP: AÇIK"
                        else
                            "● AUTO TAP: KAPALI"

                    status.setTextColor(
                        if (enabled)
                            Color.rgb(70, 220, 130)
                        else
                            Color.rgb(230, 80, 90)
                    )

                    text =
                        if (enabled)
                            "AUTO TAP'I KAPAT"
                        else
                            "AUTO TAP'I AÇ"
                }
            }

        val settings =
            Button(this).apply {

                text =
                    "♿ Erişilebilirlik Ayarları"

                setOnClickListener {

                    startActivity(
                        android.content.Intent(
                            android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS
                        )
                    )
                }
            }

        val close =
            Button(this).apply {

                text = "Kapat"

                setOnClickListener {
                    hidePanel()
                }
            }

        root.addView(title)
        root.addView(status)
        root.addView(toggle)
        root.addView(settings)
        root.addView(close)

        val params =
            WindowManager.LayoutParams(
                dp(270),
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

        params.gravity = Gravity.CENTER

        panel = root

        windowManager.addView(
            root,
            params
        )

        root.scaleX = .7f
        root.scaleY = .7f
        root.alpha = 0f

        root.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(220)
            .setInterpolator(
                DecelerateInterpolator()
            )
            .start()
    }

    private fun hidePanel() {

        val view = panel ?: return

        view.animate()
            .scaleX(.7f)
            .scaleY(.7f)
            .alpha(0f)
            .setDuration(160)
            .withEndAction {

                try {
                    windowManager.removeView(view)
                } catch (_: Exception) {
                }

                panel = null
            }
            .start()
    }

    override fun onDestroy() {

        handler.removeCallbacks(
            autoTapRunnable
        )

        try {
            panel?.let {
                windowManager.removeView(it)
            }
        } catch (_: Exception) {
        }

        try {
            floatingButton?.let {
                windowManager.removeView(it)
            }
        } catch (_: Exception) {
        }

        panel = null
        floatingButton = null

        super.onDestroy()
    }

    override fun onInterrupt() {}

    private fun dp(value: Int): Int {
        return (
            value *
                resources.displayMetrics.density
            ).toInt()
    }
}
