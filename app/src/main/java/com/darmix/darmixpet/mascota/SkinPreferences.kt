package com.darmix.darmixpet.mascota

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit


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
        prefs(context).edit { putString(KEY_SELECTED_SKIN, skinId) }
    }

    fun registerListener(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs(context).registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs(context).unregisterOnSharedPreferenceChangeListener(listener)
    }
}
