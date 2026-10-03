package com.example.dodgeautotap

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.text.InputType
import kotlin.math.abs

class DodgeAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile var enabled = false
    }

    private lateinit var windowManager: WindowManager
    private val handler = Handler(Looper.getMainLooper())

    private var floatingButton: TextView? = null
    private var panel: LinearLayout? = null

    private var currentTarget = 0
    private var running = false

    private data class Target(
        var x: Float,
        var y: Float,
        var interval: Long,
        val view: TextView
    )

    private val targets = mutableListOf<Target>()

    override fun onServiceConnected() {
        super.onServiceConnected()

        windowManager =
            getSystemService(WINDOW_SERVICE) as WindowManager

        showFloatingButton()
    }

    override fun onAccessibilityEvent(
        event: AccessibilityEvent?
    ) {
    }

    private fun startAutoClick() {
        if (running || targets.isEmpty()) return

        running = true
        currentTarget = 0
        clickNext()
    }

    private fun stopAutoClick() {
        running = false
        handler.removeCallbacksAndMessages(null)
    }

    private fun clickNext() {

        if (!running || !enabled) return

        if (targets.isEmpty()) {
            running = false
            return
        }

        if (currentTarget >= targets.size)
            currentTarget = 0

        val target = targets[currentTarget]

        clickAt(target.x, target.y)

        currentTarget++

        handler.postDelayed(
            {
                clickNext()
            },
            target.interval.coerceAtLeast(20L)
        )
    }

    private fun clickAt(
        x: Float,
        y: Float
    ) {

        val path = Path().apply {
            moveTo(x, y)
        }

        val gesture =
            GestureDescription.Builder()
                .addStroke(
                    GestureDescription.StrokeDescription(
                        path,
                        0,
                        10
                    )
                )
                .build()

        dispatchGesture(
            gesture,
            null,
            null
        )
    }

    private fun showFloatingButton() {

        if (floatingButton != null) return

        val button = TextView(this).apply {
            text = "▶"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER

            background =
                GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.rgb(35, 120, 255))
                    setStroke(
                        3,
                        Color.rgb(100, 180, 255)
                    )
                }
        }

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

        button.setOnTouchListener(
            FloatingTouchListener(params)
        )

        floatingButton = button

        windowManager.addView(
            button,
            params
        )
    }

    private inner class FloatingTouchListener(
        private val params: WindowManager.LayoutParams
    ) : View.OnTouchListener {

        private var downX = 0f
        private var downY = 0f
        private var startX = 0
        private var startY = 0
        private var moved = false

        override fun onTouch(
            v: View,
            event: MotionEvent
        ): Boolean {

            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {

                    downX = event.rawX
                    downY = event.rawY

                    startX = params.x
                    startY = params.y

                    moved = false

                    return true
                }

                MotionEvent.ACTION_MOVE -> {

                    val dx =
                        (event.rawX - downX).toInt()

                    val dy =
                        (event.rawY - downY).toInt()

                    if (
                        abs(dx) > 8 ||
                        abs(dy) > 8
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

                    if (!moved)
                        togglePanel()

                    return true
                }
            }

            return false
        }
    }

    private fun togglePanel() {

        if (panel != null)
            hidePanel()
        else
            showPanel()
    }

    private fun showPanel() {

        if (panel != null) return

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(18),
                    dp(18),
                    dp(18),
                    dp(18)
                )

                background =
                    GradientDrawable().apply {
                        setColor(
                            Color.rgb(20, 23, 30)
                        )
                        cornerRadius =
                            dp(18).toFloat()
                    }
            }

        val title =
            TextView(this).apply {
                text = "⚡ Multi Auto Clicker"
                textSize = 21f
                setTextColor(Color.WHITE)
            }

        val count =
            TextView(this).apply {
                text =
                    "Hedef sayısı: ${targets.size}"
                textSize = 16f
                setTextColor(Color.LTGRAY)
                tag = "count"
            }

        val add =
            Button(this).apply {

                text = "🎯 Hedef Ekle"

                setOnClickListener {

                    addTarget()

                    count.text =
                        "Hedef sayısı: ${targets.size}"
                }
            }

        val toggle =
            Button(this).apply {

                text =
                    if (enabled)
                        "AUTO CLICK: AÇIK"
                    else
                        "AUTO CLICK: KAPALI"

                setOnClickListener {

                    enabled = !enabled

                    if (enabled) {
                        startAutoClick()
                        text = "AUTO CLICK: AÇIK"
                    } else {
                        stopAutoClick()
                        text = "AUTO CLICK: KAPALI"
                    }
                }
            }

        val clear =
            Button(this).apply {

                text = "🗑️ Hedefleri Temizle"

                setOnClickListener {

                    stopAutoClick()
                    clearTargets()

                    count.text =
                        "Hedef sayısı: 0"
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
        root.addView(count)
        root.addView(add)
        root.addView(toggle)
        root.addView(clear)
        root.addView(close)

        val params =
            WindowManager.LayoutParams(
                dp(300),
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
    }

    private fun addTarget() {

        val number = targets.size + 1

        val targetView =
            TextView(this).apply {

                text = number.toString()
                textSize = 16f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER

                background =
                    GradientDrawable().apply {

                        shape =
                            GradientDrawable.OVAL

                        setColor(
                            Color.rgb(
                                255,
                                70,
                                70
                            )
                        )

                        setStroke(
                            dp(2),
                            Color.WHITE
                        )
                    }
            }

        val size = dp(48)

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

        params.x =
            dp(100 + targets.size * 60)

        params.y = dp(300)

        val target =
            Target(
                params.x + size / 2f,
                params.y + size / 2f,
                100L,
                targetView
            )

        targets.add(target)

        targetView.setOnTouchListener(
            TargetTouchListener(
                target,
                params,
                size
            )
        )

        windowManager.addView(
            targetView,
            params
        )

        showTargetSettings(target)
    }

    private fun showTargetSettings(
        target: Target
    ) {

        val dialog =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(20),
                    dp(20),
                    dp(20),
                    dp(20)
                )

                background =
                    GradientDrawable().apply {
                        setColor(
                            Color.rgb(
                                25,
                                28,
                                36
                            )
                        )
                        cornerRadius =
                            dp(16).toFloat()
                    }
            }

        val title =
            TextView(this).apply {

                text =
                    "🎯 Hedef ${targets.indexOf(target) + 1}"

                textSize = 19f
                setTextColor(Color.WHITE)
            }

        val label =
            TextView(this).apply {

                text =
                    "Tıklama aralığı (ms)"

                textSize = 15f
                setTextColor(Color.LTGRAY)
            }

        val input =
            EditText(this).apply {

                setText(
                    target.interval.toString()
                )

                textSize = 16f
                setTextColor(Color.WHITE)

                inputType =
                    InputType.TYPE_CLASS_NUMBER
            }

        val save =
            Button(this).apply {

                text = "Kaydet"

                setOnClickListener {

                    val value =
                        input.text
                            .toString()
                            .toLongOrNull()

                    if (value != null) {

                        target.interval =
                            value.coerceIn(
                                20L,
                                60000L
                            )
                    }

                    try {
                        windowManager.removeView(
                            dialog
                        )
                    } catch (_: Exception) {
                    }
                }
            }

        dialog.addView(title)
        dialog.addView(label)
        dialog.addView(input)
        dialog.addView(save)

        val params =
            WindowManager.LayoutParams(
                dp(260),
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

        params.gravity = Gravity.CENTER

        windowManager.addView(
            dialog,
            params
        )
    }

    private inner class TargetTouchListener(
        private val target: Target,
        private val params: WindowManager.LayoutParams,
        private val size: Int
    ) : View.OnTouchListener {

        private var downX = 0f
        private var downY = 0f
        private var startX = 0
        private var startY = 0

        override fun onTouch(
            v: View,
            event: MotionEvent
        ): Boolean {

            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {

                    downX = event.rawX
                    downY = event.rawY

                    startX = params.x
                    startY = params.y

                    return true
                }

                MotionEvent.ACTION_MOVE -> {

                    val dx =
                        (event.rawX - downX).toInt()

                    val dy =
                        (event.rawY - downY).toInt()

                    params.x = startX + dx
                    params.y = startY + dy

                    target.x =
                        params.x +
                        size / 2f

                    target.y =
                        params.y +
                        size / 2f

                    windowManager.updateViewLayout(
                        v,
                        params
                    )

                    return true
                }

                MotionEvent.ACTION_UP -> {

                    showTargetSettings(target)

                    return true
                }
            }

            return false
        }
    }

    private fun clearTargets() {

        targets.forEach {

            try {
                windowManager.removeView(
                    it.view
                )
            } catch (_: Exception) {
            }
        }

        targets.clear()
        currentTarget = 0
    }

    private fun hidePanel() {

        val view = panel ?: return

        try {
            windowManager.removeView(view)
        } catch (_: Exception) {
        }

        panel = null
    }

    override fun onDestroy() {

        stopAutoClick()

        clearTargets()

        try {
            floatingButton?.let {
                windowManager.removeView(it)
            }
        } catch (_: Exception) {
        }

        floatingButton = null

        super.onDestroy()
    }

    override fun onInterrupt() {}

    private fun dp(value: Int): Int =
        (
            value *
            resources.displayMetrics.density
        ).toInt()
}
