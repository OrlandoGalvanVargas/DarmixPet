package com.darmix.darmixpet.update

import android.content.Context

internal object UpdateStore {
    private const val PREFS_NAME = "darmixpet_prefs"
    private const val KEY_LAST_CHECK = "update_last_check"
    private const val KEY_VERSION = "update_offer_version"
    private const val KEY_NOTES = "update_offer_notes"
    private const val KEY_DOWNLOAD = "update_offer_download"
    private const val KEY_RELEASE = "update_offer_release"
    private const val MAX_NOTES = 6_000

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun lastCheck(context: Context): Long = prefs(context).getLong(KEY_LAST_CHECK, 0L)

    fun markChecked(context: Context) {
        prefs(context).edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()
    }

    fun saveOffer(context: Context, info: UpdateInfo) {
        prefs(context).edit()
            .putString(KEY_VERSION, info.latestVersion)
            .putString(KEY_NOTES, info.releaseNotes.take(MAX_NOTES))
            .putString(KEY_DOWNLOAD, info.downloadUrl)
            .putString(KEY_RELEASE, info.releaseUrl)
            .apply()
    }

    fun clearOffer(context: Context) {
        prefs(context).edit()
            .remove(KEY_VERSION).remove(KEY_NOTES).remove(KEY_DOWNLOAD).remove(KEY_RELEASE)
            .apply()
    }

    fun cachedOffer(context: Context): UpdateInfo? {
        val p = prefs(context)
        val version = p.getString(KEY_VERSION, null) ?: return null
        return UpdateInfo(
            latestVersion = version,
            releaseNotes = p.getString(KEY_NOTES, "") ?: "",
            downloadUrl = p.getString(KEY_DOWNLOAD, "") ?: "",
            releaseUrl = p.getString(KEY_RELEASE, "") ?: ""
        )
    }
}
