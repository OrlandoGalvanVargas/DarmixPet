package com.darmix.darmixpet.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.darmix.darmixpet.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DarmixNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var syncJob: Job? = null
    private lateinit var parser: NotificationParser

    override fun onCreate() {
        super.onCreate()
        parser = NotificationParser(applicationContext)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Listener conectado")
        NotificationRepository.onConnected(this)
        requestSync(immediate = true)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Listener desconectado")
        syncJob?.cancel()
        NotificationRepository.onDisconnected()
        NotificationAccessHelper.requestRebind(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = requestSync()

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = requestSync()

    override fun onNotificationRankingUpdate(rankingMap: RankingMap?) = requestSync()

    override fun onDestroy() {
        scope.cancel()
        NotificationRepository.onDisconnected()
        super.onDestroy()
    }

    internal fun isResponsive(): Boolean = try {
        activeNotifications != null
    } catch (e: Exception) {
        false
    }

    internal fun syncNow() = sync()

    internal fun dismiss(key: String): Boolean = try {
        val active = activeNotifications ?: emptyArray()
        val target = active.firstOrNull { it.key == key }
        cancelNotification(key)
        if (target != null) cancelOrphanSummary(target, active)
        true
    } catch (e: Exception) {
        Log.w(TAG, "No se pudo eliminar la notificación", e)
        false
    }

    private fun cancelOrphanSummary(target: StatusBarNotification, active: Array<StatusBarNotification>) {
        if (!target.isGroup || NotificationFilter.isGroupSummary(target)) return
        val siblings = active.filter { it.key != target.key && it.groupKey == target.groupKey }
        if (siblings.any { !NotificationFilter.isGroupSummary(it) }) return
        siblings
            .filter { NotificationFilter.isGroupSummary(it) && !NotificationFilter.isPersistent(it) }
            .forEach { cancelNotification(it.key) }
    }

    private fun requestSync(immediate: Boolean = false) {
        syncJob?.cancel()
        syncJob = scope.launch {
            if (!immediate) delay(SYNC_DEBOUNCE_MS)
            sync()
        }
    }

    @Synchronized
    private fun sync() {
        val active = try {
            activeNotifications ?: emptyArray()
        } catch (e: Exception) {
            Log.w(TAG, "No se pudieron leer las notificaciones activas", e)
            return
        }

        val rankingMap = try { currentRanking } catch (e: Exception) { null }
        val ranking = Ranking()

        val list = NotificationFilter.filter(active, packageName)
            .map { sbn ->
                val rank = if (rankingMap?.getRanking(sbn.key, ranking) == true) ranking.rank else Int.MAX_VALUE
                sbn to rank
            }
            .sortedWith(compareBy<Pair<StatusBarNotification, Int>> { it.second }
                .thenByDescending { it.first.postTime })
            .mapNotNull { (sbn, _) -> parser.parse(sbn) }

        NotificationRepository.publish(list)

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Notificaciones visibles: ${list.size} → ${list.map { "${it.appName}: ${it.title}" }}")
        }
    }

    companion object {
        private const val TAG = "DarmixNotifListener"
        private const val SYNC_DEBOUNCE_MS = 250L
    }
}