package com.darmix.darmixpet.ui.theme

import android.app.UiModeManager
import android.content.Context
import android.os.Build

object NightModeSync {

    fun apply(context: Context, mode: ThemeMode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val manager = context.getSystemService(UiModeManager::class.java) ?: return
        val target = when (mode) {
            ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
            ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
            ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
        }
        runCatching { manager.setApplicationNightMode(target) }
    }
}