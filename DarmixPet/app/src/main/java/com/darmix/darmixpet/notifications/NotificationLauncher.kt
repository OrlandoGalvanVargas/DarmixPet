package com.darmix.darmixpet.notifications

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log

object NotificationLauncher {

    private const val TAG = "NotificationLauncher"

    fun canOpen(context: Context, item: PetNotification): Boolean =
        item.contentIntent != null || launchIntentFor(context, item.packageName) != null

    fun open(context: Context, item: PetNotification): Boolean {
        val opened = sendContentIntent(context, item.contentIntent) ||
                startLaunchIntent(context, item.packageName)
        if (opened && item.autoCancel) NotificationRepository.dismiss(item.key)
        return opened
    }

    private fun sendContentIntent(context: Context, intent: PendingIntent?): Boolean {
        if (intent == null) return false
        return try {
            intent.send(context, 0, null, null, null, null, backgroundStartOptions())
            true
        } catch (e: PendingIntent.CanceledException) {
            Log.w(TAG, "La acción de la notificación ya no es válida", e)
            false
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo enviar la acción de la notificación", e)
            false
        }
    }

    private fun startLaunchIntent(context: Context, packageName: String): Boolean {
        val intent = launchIntentFor(context, packageName) ?: return false
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo abrir la app $packageName", e)
            false
        }
    }

    private fun launchIntentFor(context: Context, packageName: String): Intent? =
        context.packageManager.getLaunchIntentForPackage(packageName)

    private fun backgroundStartOptions(): Bundle? = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA -> ActivityOptions.makeBasic()
            .setPendingIntentBackgroundActivityStartMode(
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
            )
            .toBundle()
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> @Suppress("DEPRECATION")
        ActivityOptions.makeBasic()
            .setPendingIntentBackgroundActivityStartMode(
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
            )
            .toBundle()
        else -> null
    }
}