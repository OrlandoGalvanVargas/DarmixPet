package com.darmix.darmixpet.mascota

import android.content.Context
import android.content.SharedPreferences

/**
 * Guarda y recupera qué skin eligió el usuario, y permite escuchar cambios
 * en tiempo real (para que PetOverlayService actualice la mascota sin
 * necesidad de reiniciar el servicio).
 */
object SkinPreferences {
    private const val PREFS_NAME = "darmixpet_prefs"
    const val KEY_SELECTED_SKIN = "selected_skin_id"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSelectedSkinId(context: Context): String {
        val defaultId = SkinRegistry.availableSkins.first().id
        return prefs(context).getString(KEY_SELECTED_SKIN, defaultId) ?: defaultId
    }

    fun setSelectedSkinId(context: Context, skinId: String) {
        prefs(context).edit().putString(KEY_SELECTED_SKIN, skinId).apply()
    }

    fun registerListener(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs(context).registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs(context).unregisterOnSharedPreferenceChangeListener(listener)
    }
}