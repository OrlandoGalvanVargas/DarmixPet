package com.darmix.darmixpet.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.lang.ref.WeakReference
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object NotificationRepository {

    private val _notifications = MutableStateFlow<List<PetNotification>>(emptyList())
    val notifications: StateFlow<List<PetNotification>> = _notifications.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _arrivals = MutableSharedFlow<MessageArrival>(extraBufferCapacity = 16)
    val arrivals: SharedFlow<MessageArrival> = _arrivals.asSharedFlow()

    internal fun emitArrival(arrival: MessageArrival) {
        _arrivals.tryEmit(arrival)
    }

    private var listenerRef: WeakReference<DarmixNotificationListener>? = null

    internal fun onConnected(listener: DarmixNotificationListener) {
        listenerRef = WeakReference(listener)
        _isConnected.value = true
    }

    internal fun onDisconnected() {
        listenerRef = null
        _isConnected.value = false
        _notifications.value = emptyList()
    }

    internal fun publish(list: List<PetNotification>) {
        _notifications.value = list
    }

    fun dismiss(key: String): Boolean {
        val listener = listenerRef?.get() ?: return false
        _notifications.update { list -> list.filterNot { it.key == key } }
        return listener.dismiss(key)
    }

    fun isHealthy(): Boolean = listenerRef?.get()?.isResponsive() == true

    fun refreshNow(): Boolean {
        val listener = listenerRef?.get() ?: return false
        listener.syncNow()
        return true
    }
}