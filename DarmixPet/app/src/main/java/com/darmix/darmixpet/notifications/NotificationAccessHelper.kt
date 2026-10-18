package com.darmix.darmixpet.notifications

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import android.service.notification.NotificationListenerService

object NotificationAccessHelper {

    fun isNotificationAccessGranted(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)

    fun buildPermissionIntents(context: Context): List<Intent> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            add(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                    Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    ComponentName(context, DarmixNotificationListener::class.java).flattenToString()
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

    fun requestRebindIfNeeded(context: Context) {
        if (!isNotificationAccessGranted(context)) return
        if (NotificationRepository.isConnected.value) return
        try {
            NotificationListenerService.requestRebind(
                ComponentName(context, DarmixNotificationListener::class.java)
            )
        } catch (e: Exception) {
        }
    }
}