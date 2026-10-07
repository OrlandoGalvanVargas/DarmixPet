package com.darmix.darmixpet.monitor

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context


class ForegroundAppTracker(context: Context) {

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    private var lastQueryTime = System.currentTimeMillis() - 5_000
    private var currentForegroundApp: String? = null

    fun poll(): String? {
        val now = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(lastQueryTime, now)
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                currentForegroundApp = event.packageName
            }
        }

        lastQueryTime = now
        return currentForegroundApp
    }


    fun reset() {
        currentForegroundApp = null
        lastQueryTime = System.currentTimeMillis()
    }
}
