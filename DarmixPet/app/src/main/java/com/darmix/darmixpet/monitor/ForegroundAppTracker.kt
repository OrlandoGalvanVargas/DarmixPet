package com.darmix.darmixpet.monitor

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

class ForegroundAppTracker(private val context: Context) {

    companion object {
        private const val INITIAL_LOOKBACK_MS = 30 * 60_000L
        private const val EVENT_RESUMED = 1
        private const val EVENT_PAUSED = 2
        private const val EVENT_STOPPED = 23
        private const val EVENT_DESTROYED = 24
    }

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    private var lastQueryTime = System.currentTimeMillis() - INITIAL_LOOKBACK_MS
    private val resumedActivities = LinkedHashMap<String, String>()
    private var homePackages: Set<String> = emptySet()

    init {
        homePackages = findHomePackages()
    }

    fun poll(): String? {
        val now = System.currentTimeMillis()
        val events = try {
            usageStatsManager.queryEvents(lastQueryTime, now)
        } catch (e: Exception) {
            null
        }
        if (events != null) {
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                handle(event)
            }
            lastQueryTime = now
        }
        return resumedActivities.values.lastOrNull()
    }

    fun reset() {
        resumedActivities.clear()
        lastQueryTime = System.currentTimeMillis()
    }

    private fun handle(event: UsageEvents.Event) {
        val packageName = event.packageName ?: return
        val key = "$packageName/${event.className.orEmpty()}"
        when (event.eventType) {
            EVENT_RESUMED -> {
                if (packageName in homePackages) resumedActivities.clear()
                resumedActivities.remove(key)
                resumedActivities[key] = packageName
            }
            EVENT_PAUSED, EVENT_STOPPED, EVENT_DESTROYED -> resumedActivities.remove(key)
        }
    }

    private fun findHomePackages(): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return try {
            context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                .mapNotNull { it.activityInfo?.packageName }
                .toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }
}