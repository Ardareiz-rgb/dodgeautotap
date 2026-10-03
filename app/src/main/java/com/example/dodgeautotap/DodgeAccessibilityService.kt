package com.example.dodgeautotap

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.graphics.Rect

class DodgeAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile var enabled = false
        private var lastTap = 0L
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!enabled) return

        val now = SystemClock.uptimeMillis()
        if (now - lastTap < 180) return

        val root = rootInActiveWindow ?: return
        val node = findDodge(root) ?: return

        val r = Rect()
        node.getBoundsInScreen(r)
        if (r.width() <= 0 || r.height() <= 0) return

        val x = r.centerX().toFloat()
        val y = r.centerY().toFloat()

        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 40))
            .build()

        lastTap = now
        dispatchGesture(gesture, null, null)
    }

    private fun findDodge(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()

        if (text.equals("Dodge", ignoreCase = true) ||
            desc.equals("Dodge", ignoreCase = true)) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findDodge(child)
            if (result != null) return result
        }
        return null
    }

    override fun onInterrupt() {}
}
