package com.darmix.darmixpet.monitor

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class AppWatcherAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            // Ignora ventanas propias (overlays como el Quick Menu): no representan
            // que el usuario cambió de app, y si no las filtramos, el simple hecho
            // de mostrar el Quick Menu dispara un evento que lo cierra a sí mismo.
            if (packageName == applicationContext.packageName) return

            ForegroundAppSignal.emit(packageName)
        }
    }

    override fun onInterrupt() {
        // No requerido para este caso de uso
    }
}