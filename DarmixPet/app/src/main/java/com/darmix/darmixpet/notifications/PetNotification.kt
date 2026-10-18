package com.darmix.darmixpet.notifications

import android.app.PendingIntent
import android.graphics.drawable.Icon

data class PetNotification(
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String?,
    val text: String?,
    val postTime: Long,
    val largeIcon: Icon?,
    val contentIntent: PendingIntent?,
    val autoCancel: Boolean
)