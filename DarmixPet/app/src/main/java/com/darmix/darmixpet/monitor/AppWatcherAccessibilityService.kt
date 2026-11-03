package com.darmix.darmixpet.monitor

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager

class AppWatcherAccessibilityService : AccessibilityService() {

    companion object {
        private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    }

    private val activityCache = HashMap<String, Boolean>()
    private var keyboardPackages: Set<String> = emptySet()

    override fun onServiceConnected() {
        super.onServiceConnected()
        refreshKeyboardPackages()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName == applicationContext.packageName) return
        if (packageName == SYSTEM_UI_PACKAGE) return
        if (packageName in keyboardPackages) return
        val className = event.className?.toString() ?: return
        if (!isActivity(packageName, className)) return

        ForegroundAppSignal.emit(packageName)
    }

    override fun onInterrupt() {
    }

    private fun isActivity(packageName: String, className: String): Boolean {
        val key = "$packageName/$className"
        activityCache[key]?.let { return it }
        val result = try {
            packageManager.getActivityInfo(ComponentName(packageName, className), 0)
            true
        } catch (e: Exception) {
            false
        }
        if (activityCache.size > 300) activityCache.clear()
        activityCache[key] = result
        return result
    }

    private fun refreshKeyboardPackages() {
        keyboardPackages = try {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.enabledInputMethodList.map { it.packageName }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }
}