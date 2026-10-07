package com.darmix.darmixpet.overlay

import android.content.Context
import android.content.SharedPreferences


object PetPreferences {
    private const val PREFS_NAME = "darmixpet_prefs"
    const val KEY_PET_ENABLED = "pet_enabled"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_PET_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PET_ENABLED, enabled).apply()
    }

    fun registerListener(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs(context).registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs(context).unregisterOnSharedPreferenceChangeListener(listener)
    }
}


object PetController {

    fun wake(context: Context) {
        PetPreferences.setEnabled(context, true)
        PetOverlayService.start(context)
    }

    fun sleep(context: Context) {
        PetPreferences.setEnabled(context, false)
        PetOverlayService.stop(context)
    }
}
