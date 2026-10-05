package com.darmix.darmixpet.monitor

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class AppWatcherAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return




            if (packageName == applicationContext.packageName) return

            ForegroundAppSignal.emit(packageName)
        }
    }

    override fun onInterrupt() {

    }
}
