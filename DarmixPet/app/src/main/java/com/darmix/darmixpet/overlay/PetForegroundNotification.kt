package com.darmix.darmixpet.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.getSystemService
import com.darmix.darmixpet.R
import com.darmix.darmixpet.ui.SkinLore

object PetForegroundNotification {

    private const val CHANNEL_ID = "pet_overlay_channel"
    private const val CHANNEL_NAME = "DarmixPet Overlay"
    private const val NOTIFICATION_ID = 1001

    fun start(service: Service, skinId: String) {
        createChannel(service)
        ServiceCompat.startForeground(
            service,
            NOTIFICATION_ID,
            build(service, skinId),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
    }

    fun refresh(context: Context, skinId: String) {
        runCatching {
            context.getSystemService<NotificationManager>()?.notify(NOTIFICATION_ID, build(context, skinId))
        }
    }

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
        context.getSystemService<NotificationManager>()?.createNotificationChannel(channel)
    }

    private fun build(context: Context, skinId: String): Notification {
        val petName = SkinLore.forId(skinId).shortName
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("DarmixPet activo")
            .setContentText("$petName está vigilando tu tiempo de pantalla")
            .setSmallIcon(R.drawable.ic_stat_darmixpet)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }
}