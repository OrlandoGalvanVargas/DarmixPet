package com.darmix.darmixpet.overlay

import android.content.Context
import android.media.AudioManager
import com.darmix.darmixpet.notifications.MessageAlertApps
import com.darmix.darmixpet.notifications.MessageArrival
import com.darmix.darmixpet.notifications.NotificationRepository
import com.darmix.darmixpet.tts.MessageAlertPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MessageAlertCoordinator(
    private val context: Context,
    private val scope: CoroutineScope,
    private val isAppInFront: (String) -> Boolean,
    private val speak: (String) -> Unit,
    private val showBubble: (String) -> Unit
) {

    companion object {
        private const val GROUP_WINDOW_MS = 2500L
    }

    private val pendingArrivals = mutableListOf<MessageArrival>()
    private var listenJob: Job? = null
    private var flushJob: Job? = null

    fun start() {
        listenJob?.cancel()
        listenJob = scope.launch {
            NotificationRepository.arrivals.collect { onArrival(it) }
        }
    }

    fun stop() {
        listenJob?.cancel()
        listenJob = null
        flushJob?.cancel()
        flushJob = null
        pendingArrivals.clear()
    }

    private fun onArrival(arrival: MessageArrival) {
        if (!MessageAlertPreferences.isAppEnabled(context, arrival.packageName)) return
        if (isAppInFront(arrival.packageName)) return
        pendingArrivals += arrival
        if (flushJob?.isActive == true) return
        flushJob = scope.launch {
            delay(GROUP_WINDOW_MS)
            flush()
        }
    }

    private fun flush() {
        val batch = pendingArrivals.toList()
        pendingArrivals.clear()
        if (batch.isEmpty() || !canSpeak()) return
        val text = buildText(batch, MessageAlertPreferences.includeSender(context)) ?: return
        speak(text)
        if (MessageAlertPreferences.showBubble(context)) showBubble(text)
    }

    private fun canSpeak(): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) return false
        val mode = audioManager.mode
        return mode != AudioManager.MODE_IN_CALL && mode != AudioManager.MODE_IN_COMMUNICATION
    }

    private fun buildText(batch: List<MessageArrival>, includeSender: Boolean): String? {
        val byApp = batch.groupBy { it.packageName }
            .mapNotNull { (pkg, items) -> MessageAlertApps.byPackage(pkg)?.let { it to items } }
        if (byApp.isEmpty()) return null

        if (byApp.size > 1) {
            val labels = byApp.map { it.first.label }.distinct()
            val joined = labels.dropLast(1).joinToString(", ") + " y " + labels.last()
            return "¡Tienes mensajes nuevos de $joined!"
        }

        val (app, items) = byApp.first()
        val count = items.size
        val senders = items.mapNotNull { it.sender }.distinct()
        val sender = if (includeSender && senders.size == 1) senders.first() else null

        return when {
            count == 1 && sender == null -> "¡Tienes ${app.singular}!"
            count == 1 && app.isMail -> "¡Tienes un correo nuevo de $sender!"
            count == 1 && app.isSms -> "¡Tienes un mensaje de texto de $sender!"
            count == 1 -> "¡Tienes un mensaje de $sender en ${app.label}!"
            sender == null -> "¡Tienes $count ${app.plural}!"
            app.isMail -> "¡Tienes $count correos nuevos de $sender!"
            app.isSms -> "¡Tienes $count mensajes de texto de $sender!"
            else -> "¡Tienes $count mensajes de $sender en ${app.label}!"
        }
    }
}