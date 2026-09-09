package com.barbie.ai

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityEvent

/**
 * User-enabled accessibility bridge for explicit Barbie actions.
 * It never runs by itself: Android requires the user to enable this service.
 */
class BarbieActionService : AccessibilityService() {
    companion object {
        @Volatile var instance: BarbieActionService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    fun pressBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)

    fun pressHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)

    fun clickText(text: String): Boolean {
        if (text.isBlank()) return false
        val root = rootInActiveWindow ?: return false
        return clickNode(root, text.trim())
    }

    private fun clickNode(node: AccessibilityNodeInfo, wanted: String): Boolean {
        val label = node.text?.toString().orEmpty()
        val description = node.contentDescription?.toString().orEmpty()
        if ((label.equals(wanted, ignoreCase = true) || description.equals(wanted, ignoreCase = true)) && node.isClickable) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (clickNode(child, wanted)) return true
        }
        return false
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }
}
