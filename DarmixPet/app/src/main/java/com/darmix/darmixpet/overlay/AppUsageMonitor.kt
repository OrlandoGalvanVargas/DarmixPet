package com.darmix.darmixpet.overlay

import android.app.KeyguardManager
import android.content.Context
import android.os.SystemClock
import com.darmix.darmixpet.data.AppDatabase
import com.darmix.darmixpet.data.MonitoredAppDao
import com.darmix.darmixpet.data.MonitoredAppEntity
import com.darmix.darmixpet.monitor.ForegroundAppSignal
import com.darmix.darmixpet.monitor.ForegroundAppTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

class AppUsageMonitor(
    context: Context,
    private val scope: CoroutineScope,
    private val tracker: ForegroundAppTracker,
    private val isScreenOn: () -> Boolean,
    private val speak: (String) -> Unit,
    private val onBlock: (String) -> Unit,
    private val onAppSwitched: () -> Unit
) {

    companion object {
        private const val FOREGROUND_POLL_INTERVAL_MS = 1000L
        private const val MAX_COUNTED_STEP_MS = 2000L
        private const val DAILY_RESET_CHECK_INTERVAL_MS = 60_000L
        private const val NO_MONITORED_APP_TEXT = "No hay ninguna app monitoreada abierta"
        private const val COOLDOWN_MESSAGE = "Todavía estás en tiempo de descanso ⏳"
        private const val NO_SESSIONS_MESSAGE = "Ya usaste todas tus sesiones de hoy 🚫"
        private const val TIME_UP_MESSAGE = "Se acabó tu tiempo ⏰"
    }

    private val dao: MonitoredAppDao = AppDatabase.getInstance(context).monitoredAppDao()
    private val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    private var lastCountedAt = 0L

    @Volatile
    var lastForegroundPackage: String? = null
        private set

    fun start() {
        startMonitoringLoop()
        startDailyResetWatcherLoop()
        startInstantGateListener()
    }

    suspend fun statusText(): String {
        val packageName = lastForegroundPackage ?: return NO_MONITORED_APP_TEXT
        val app = dao.getByPackageName(packageName) ?: return NO_MONITORED_APP_TEXT

        val now = System.currentTimeMillis()
        val current = applyDailyResetIfNeeded(app, now)

        if (isInCooldown(current, now)) {
            val remainingMs = cooldownMillis(current) - (now - current.lastBlockedTimestamp)
            return "${current.appName}\nEn enfriamiento: ${formatDuration(remainingMs)} restantes"
        }
        if (current.sessionsUsedToday >= current.maxSessionsPerDay) {
            return "${current.appName}\nSesiones agotadas por hoy"
        }
        val sessionLimitMillis = current.sessionLimitMinutes * 60_000L
        val remainingMs = (sessionLimitMillis - current.usedInSessionMillis).coerceAtLeast(0L)
        return "${current.appName}\nRestante: ${formatDuration(remainingMs)}"
    }

    private fun startInstantGateListener() {
        scope.launch {
            ForegroundAppSignal.events.collect { packageName ->
                onAppSwitched()
                lastForegroundPackage = packageName
                instantGateCheck(packageName)
            }
        }
    }

    private suspend fun instantGateCheck(packageName: String) {
        val app = dao.getByPackageName(packageName) ?: return
        val now = System.currentTimeMillis()
        val current = applyDailyResetIfNeeded(app, now)
        if (current != app) dao.update(current)

        if (isInCooldown(current, now)) {
            onBlock(COOLDOWN_MESSAGE)
            return
        }
        if (current.sessionsUsedToday >= current.maxSessionsPerDay) {
            onBlock(NO_SESSIONS_MESSAGE)
        }
    }

    private fun startMonitoringLoop() {
        scope.launch {
            while (true) {
                var counted = false
                if (canCount()) {
                    val foregroundPackage = tracker.poll()
                    if (foregroundPackage != null) {
                        lastForegroundPackage = foregroundPackage
                        val monitoredApp = dao.getByPackageName(foregroundPackage)
                        if (monitoredApp != null) {
                            handleMonitoredApp(monitoredApp, nextStep())
                            counted = true
                        }
                    }
                }
                if (!counted) lastCountedAt = 0L
                delay(FOREGROUND_POLL_INTERVAL_MS)
            }
        }
    }

    private fun canCount(): Boolean = isScreenOn() && !keyguardManager.isKeyguardLocked

    private fun nextStep(): Long {
        val now = SystemClock.elapsedRealtime()
        val step = if (lastCountedAt == 0L) FOREGROUND_POLL_INTERVAL_MS
        else (now - lastCountedAt).coerceIn(0L, MAX_COUNTED_STEP_MS)
        lastCountedAt = now
        return step
    }

    private suspend fun handleMonitoredApp(app: MonitoredAppEntity, stepMs: Long) {
        val now = System.currentTimeMillis()
        val current = applyDailyResetIfNeeded(app, now)

        if (isInCooldown(current, now)) {
            if (current != app) dao.update(current)
            onBlock(COOLDOWN_MESSAGE)
            return
        }
        if (current.sessionsUsedToday >= current.maxSessionsPerDay) {
            if (current != app) dao.update(current)
            onBlock(NO_SESSIONS_MESSAGE)
            return
        }

        val sessionLimitMillis = current.sessionLimitMinutes * 60_000L
        var updated = current.copy(usedInSessionMillis = current.usedInSessionMillis + stepMs)

        if (!updated.halfwayAlertGiven && updated.usedInSessionMillis >= sessionLimitMillis / 2) {
            speak("Vas a la mitad de tu tiempo en ${updated.appName}")
            updated = updated.copy(halfwayAlertGiven = true)
        }

        if (updated.usedInSessionMillis >= sessionLimitMillis) {
            val blocked = updated.copy(
                usedInSessionMillis = 0L,
                halfwayAlertGiven = false,
                sessionsUsedToday = updated.sessionsUsedToday + 1,
                lastBlockedTimestamp = now
            )
            speak("Se acabó tu tiempo en ${blocked.appName}")
            dao.update(blocked)
            onBlock(TIME_UP_MESSAGE)
        } else {
            dao.update(updated)
        }
    }

    private fun startDailyResetWatcherLoop() {
        scope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                dao.getAllMonitoredAppsOnce().forEach { app ->
                    if (app.lastUsageResetTimestamp != 0L && !isSameDay(app.lastUsageResetTimestamp, now)) {
                        dao.update(resetUsage(app, now))
                    }
                }
                delay(DAILY_RESET_CHECK_INTERVAL_MS)
            }
        }
    }

    private fun applyDailyResetIfNeeded(app: MonitoredAppEntity, now: Long): MonitoredAppEntity {
        return if (!isSameDay(app.lastUsageResetTimestamp, now)) resetUsage(app, now) else app
    }

    private fun resetUsage(app: MonitoredAppEntity, now: Long): MonitoredAppEntity {
        return app.copy(
            sessionsUsedToday = 0,
            usedInSessionMillis = 0L,
            halfwayAlertGiven = false,
            lastUsageResetTimestamp = now
        )
    }

    private fun cooldownMillis(app: MonitoredAppEntity): Long = app.cooldownMinutes * 60_000L

    private fun isInCooldown(app: MonitoredAppEntity, now: Long): Boolean {
        return app.lastBlockedTimestamp != 0L && (now - app.lastBlockedTimestamp) < cooldownMillis(app)
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        if (t1 == 0L) return false
        val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return if (minutes > 0) "${minutes} min ${seconds}s" else "${seconds}s"
    }
}