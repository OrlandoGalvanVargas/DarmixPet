package com.darmix.darmixpet.ui.theme

import android.content.Context
import android.content.SharedPreferences

/**
 * Guarda y recupera la preferencia de tema (Claro / Oscuro / Seguir al
 * sistema), y permite escuchar cambios en tiempo real. Usa el mismo archivo
 * de SharedPreferences que SkinPreferences, con su propia clave.
 */
object ThemePreferences {
    private const val PREFS_NAME = "darmixpet_prefs"
    private const val KEY_THEME_MODE = "theme_mode"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getThemeMode(context: Context): ThemeMode {
        val stored = prefs(context).getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return try {
            ThemeMode.valueOf(stored ?: ThemeMode.SYSTEM.name)
        } catch (e: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        prefs(context).edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun registerListener(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs(context).registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs(context).unregisterOnSharedPreferenceChangeListener(listener)
    }
}