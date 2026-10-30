package com.darmix.darmixpet.tts

import android.content.Context
import android.content.SharedPreferences

object MessageAlertPreferences {
    private const val PREFS_NAME = "darmixpet_prefs"
    const val KEY_APPS = "message_alert_apps"
    const val KEY_SENDER = "message_alert_sender"
    private val DEFAULT_APPS = setOf("com.whatsapp")
    const val KEY_BUBBLE = "message_alert_bubble"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun enabledApps(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_APPS, DEFAULT_APPS)?.toSet() ?: DEFAULT_APPS

    fun isAppEnabled(context: Context, packageName: String): Boolean =
        packageName in enabledApps(context)

    fun setAppEnabled(context: Context, packageName: String, enabled: Boolean) {
        val updated = enabledApps(context).toMutableSet()
        if (enabled) updated.add(packageName) else updated.remove(packageName)
        prefs(context).edit().putStringSet(KEY_APPS, updated).apply()
    }

    fun includeSender(context: Context): Boolean = prefs(context).getBoolean(KEY_SENDER, false)

    fun setIncludeSender(context: Context, include: Boolean) {
        prefs(context).edit().putBoolean(KEY_SENDER, include).apply()
    }

    fun registerListener(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs(context).registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs(context).unregisterOnSharedPreferenceChangeListener(listener)
    }
    fun showBubble(context: Context): Boolean = prefs(context).getBoolean(KEY_BUBBLE, false)

    fun setShowBubble(context: Context, show: Boolean) {
        prefs(context).edit().putBoolean(KEY_BUBBLE, show).apply()
    }
}