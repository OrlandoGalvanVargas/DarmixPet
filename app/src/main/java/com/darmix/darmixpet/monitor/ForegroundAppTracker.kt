package com.darmix.darmixpet.monitor

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

/**
 * Mantiene el estado de la app en primer plano entre consultas.
 * Debe reutilizarse la misma instancia entre llamadas a poll() —
 * no crear una nueva cada vez, o se pierde el rastro del último estado conocido.
 */
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

    /**
     * Limpia el paquete recordado como "en primer plano". Se llama justo después
     * de forzar una redirección a Home, para que el siguiente poll() no siga
     * reportando la app bloqueada mientras UsageStatsManager aún no registra
     * el evento real de cambio a Home (evita bloqueos duplicados).
     */
    fun reset() {
        currentForegroundApp = null
        lastQueryTime = System.currentTimeMillis()
    }
}