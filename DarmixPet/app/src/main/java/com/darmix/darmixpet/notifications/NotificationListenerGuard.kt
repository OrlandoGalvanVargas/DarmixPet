package com.darmix.darmixpet.notifications

import android.content.Context
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NotificationListenerGuard(
    private val context: Context,
    private val scope: CoroutineScope
) {

    private companion object {
        const val TAG = "NotifListenerGuard"
        const val INITIAL_DELAY_MS = 3_000L
        const val CHECK_INTERVAL_MS = 5 * 60_000L
        const val REBIND_WAIT_MS = 4_000L
        const val FORCE_REBIND_COOLDOWN_MS = 10 * 60_000L
    }

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var periodicJob: Job? = null
    private var repairJob: Job? = null
    private var lastForceRebindAt = 0L

    fun start() {
        periodicJob?.cancel()
        periodicJob = scope.launch {
            delay(INITIAL_DELAY_MS)
            while (isActive) {
                if (powerManager.isInteractive) check()
                delay(CHECK_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        periodicJob?.cancel()
        repairJob?.cancel()
        periodicJob = null
        repairJob = null
    }

    fun check() {
        if (!NotificationAccessHelper.isNotificationAccessGranted(context)) return
        if (NotificationRepository.isHealthy()) {
            NotificationRepository.refreshNow()
            return
        }
        if (repairJob?.isActive == true) return
        repairJob = scope.launch { repair() }
    }

    private suspend fun repair() {
        Log.w(TAG, "Listener sin respuesta, solicitando rebind")
        NotificationAccessHelper.requestRebind(context)
        delay(REBIND_WAIT_MS)
        if (NotificationRepository.isHealthy()) {
            Log.d(TAG, "Listener recuperado con rebind")
            return
        }

        val now = SystemClock.elapsedRealtime()
        if (lastForceRebindAt != 0L && now - lastForceRebindAt < FORCE_REBIND_COOLDOWN_MS) return
        lastForceRebindAt = now

        Log.w(TAG, "Rebind sin efecto, reiniciando el componente del listener")
        NotificationAccessHelper.forceRebind(context)
        delay(REBIND_WAIT_MS)
        Log.d(TAG, if (NotificationRepository.isHealthy()) "Listener recuperado" else "Listener aún sin respuesta")
    }
}