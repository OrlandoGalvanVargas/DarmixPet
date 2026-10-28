package com.darmix.darmixpet.notifications

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.core.app.NotificationManagerCompat

object NotificationAccessHelper {

    private const val TAG = "NotifAccessHelper"

    fun isNotificationAccessGranted(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)

    fun buildPermissionIntents(context: Context): List<Intent> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            add(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                    Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    listenerComponent(context).flattenToString()
                )
            )
        }
        add(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        add(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}")
            )
        )
    }

    fun requestRebind(context: Context) {
        if (!isNotificationAccessGranted(context)) return
        try {
            NotificationListenerService.requestRebind(listenerComponent(context))
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo solicitar rebind", e)
        }
    }

    fun forceRebind(context: Context) {
        if (!isNotificationAccessGranted(context)) return
        val component = listenerComponent(context)
        val pm = context.packageManager
        try {
            pm.setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            pm.setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo reiniciar el componente del listener", e)
        }
        requestRebind(context)
    }

    private fun listenerComponent(context: Context) =
        ComponentName(context, DarmixNotificationListener::class.java)
}