package com.darmix.darmixpet.monitor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object BrightnessPermissionHelper {

    fun hasWriteSettingsPermission(context: Context): Boolean {
        return Settings.System.canWrite(context)
    }

    fun buildPermissionIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_WRITE_SETTINGS,
            Uri.parse("package:${context.packageName}")
        )
    }
}