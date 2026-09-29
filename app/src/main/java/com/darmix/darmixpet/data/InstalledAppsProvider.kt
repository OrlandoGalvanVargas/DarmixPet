package com.darmix.darmixpet.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val icon: ImageBitmap
)

object InstalledAppsProvider {

    /**
     * Devuelve las apps instaladas que tienen un ícono de lanzador
     * (excluye componentes de sistema sin UI, servicios, etc.)
     * y excluye la propia app DarmixPet.
     *
     * Debe llamarse desde un hilo de fondo (Dispatchers.IO): decodifica
     * íconos de potencialmente cientos de apps.
     */
    fun getLaunchableApps(context: Context): List<InstalledAppInfo> {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolvedApps = packageManager.queryIntentActivities(intent, 0)
        val selfPackage = context.packageName

        return resolvedApps
            .asSequence()
            .map { resolveInfo -> resolveInfo.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != selfPackage }
            .map { appInfo: ApplicationInfo ->
                InstalledAppInfo(
                    packageName = appInfo.packageName,
                    appName = packageManager.getApplicationLabel(appInfo).toString(),
                    icon = packageManager.getApplicationIcon(appInfo)
                        .toBitmap()
                        .asImageBitmap()
                )
            }
            .sortedBy { it.appName.lowercase() }
            .toList()
    }
}