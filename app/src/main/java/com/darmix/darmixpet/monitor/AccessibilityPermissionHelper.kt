package com.darmix.darmixpet.monitor

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils

object AccessibilityPermissionHelper {

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponent = ComponentName(context, AppWatcherAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            val component = ComponentName.unflattenFromString(splitter.next())
            if (component == expectedComponent) return true
        }
        return false
    }

    fun buildPermissionIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }
}