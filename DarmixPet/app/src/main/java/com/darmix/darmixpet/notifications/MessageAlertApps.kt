package com.darmix.darmixpet.notifications

import android.content.Context

data class MessageAlertApp(
    val packageName: String,
    val label: String,
    val singular: String,
    val plural: String,
    val isMail: Boolean = false,
    val isSms: Boolean = false
)

data class MessageArrival(
    val packageName: String,
    val sender: String?
)

object MessageAlertApps {

    val catalog: List<MessageAlertApp> = listOf(
        MessageAlertApp("com.whatsapp", "WhatsApp", "un mensaje de WhatsApp", "mensajes de WhatsApp"),
        MessageAlertApp("com.whatsapp.w4b", "WhatsApp Business", "un mensaje de WhatsApp Business", "mensajes de WhatsApp Business"),
        MessageAlertApp("org.telegram.messenger", "Telegram", "un mensaje de Telegram", "mensajes de Telegram"),
        MessageAlertApp("com.facebook.orca", "Messenger", "un mensaje de Messenger", "mensajes de Messenger"),
        MessageAlertApp("com.instagram.android", "Instagram", "un mensaje de Instagram", "mensajes de Instagram"),
        MessageAlertApp("com.google.android.apps.messaging", "Mensajes", "un mensaje de texto", "mensajes de texto", isSms = true),
        MessageAlertApp("com.samsung.android.messaging", "Mensajes de Samsung", "un mensaje de texto", "mensajes de texto", isSms = true),
        MessageAlertApp("org.thoughtcrime.securesms", "Signal", "un mensaje de Signal", "mensajes de Signal"),
        MessageAlertApp("com.discord", "Discord", "un mensaje de Discord", "mensajes de Discord"),
        MessageAlertApp("com.google.android.gm", "Gmail", "un correo nuevo", "correos nuevos", isMail = true),
        MessageAlertApp("com.microsoft.office.outlook", "Outlook", "un correo nuevo", "correos nuevos", isMail = true)
    )

    private val byPackage = catalog.associateBy { it.packageName }

    fun byPackage(packageName: String): MessageAlertApp? = byPackage[packageName]

    fun installed(context: Context): List<MessageAlertApp> {
        val pm = context.packageManager
        return catalog.filter { pm.getLaunchIntentForPackage(it.packageName) != null }
    }
}