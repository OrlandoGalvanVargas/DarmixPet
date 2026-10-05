package com.darmix.darmixpet.ui

import android.content.Context


object GuidePreferences {
    private const val PREFS_NAME = "darmixpet_prefs"
    private const val KEY_GUIDE_SEEN = "guide_seen"

    fun hasSeen(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_GUIDE_SEEN, false)

    fun markSeen(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_GUIDE_SEEN, true).apply()
    }
}
