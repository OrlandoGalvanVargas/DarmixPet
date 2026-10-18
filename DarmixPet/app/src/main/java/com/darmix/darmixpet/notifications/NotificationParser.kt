package com.darmix.darmixpet.notifications

import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat

internal class NotificationParser(private val context: Context) {

    private val labelCache = HashMap<String, String>()

    fun parse(sbn: StatusBarNotification): PetNotification? {
        val notification = sbn.notification
        val extras = notification.extras

        val title: String?
        val text: String?

        val messaging = NotificationCompat.MessagingStyle
            .extractMessagingStyleFromNotification(notification)

        if (messaging != null && messaging.messages.isNotEmpty()) {
            title = messaging.conversationTitle.clean()
                ?: extras.getCharSequence(Notification.EXTRA_TITLE).clean()
            val isGroup = messaging.isGroupConversation
            text = messaging.messages
                .takeLast(MAX_MESSAGES)
                .mapNotNull { message ->
                    val body = message.text.clean() ?: return@mapNotNull null
                    val sender = message.person?.name.clean()
                    when {
                        sender == null -> "Tú: $body"
                        isGroup -> "$sender: $body"
                        else -> body
                    }
                }
                .joinToString("\n")
                .ifBlank { null }
        } else {
            title = (extras.getCharSequence(Notification.EXTRA_TITLE_BIG)
                ?: extras.getCharSequence(Notification.EXTRA_TITLE)).clean()
            text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT).clean()
                ?: extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                    ?.mapNotNull { it.clean() }
                    ?.joinToString("\n")
                    ?.ifBlank { null }
                        ?: extras.getCharSequence(Notification.EXTRA_TEXT).clean()
        }

        if (title == null && text == null) return null

        return PetNotification(
            key = sbn.key,
            packageName = sbn.packageName,
            appName = appLabel(sbn.packageName),
            title = title,
            text = text,
            postTime = sbn.postTime,
            largeIcon = notification.getLargeIcon(),
            contentIntent = notification.contentIntent,
            autoCancel = (notification.flags and Notification.FLAG_AUTO_CANCEL) != 0
        )
    }

    private fun appLabel(packageName: String): String = labelCache.getOrPut(packageName) {
        val pm = context.packageManager
        try {
            val info = if (Build.VERSION.SDK_INT >= 33) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(info).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    private fun CharSequence?.clean(): String? =
        this?.toString()?.trim()?.takeIf { it.isNotEmpty() }

    private companion object {
        const val MAX_MESSAGES = 4
    }
}