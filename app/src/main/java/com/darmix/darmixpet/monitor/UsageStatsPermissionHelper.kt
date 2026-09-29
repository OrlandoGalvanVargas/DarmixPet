package com.darmix.darmixpet.monitor

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings

object UsageStatsPermissionHelper {

    // unsafeCheckOpNoThrow está disponible desde API 23, no requiere @RequiresApi(Q)
    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun buildPermissionIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    }
}