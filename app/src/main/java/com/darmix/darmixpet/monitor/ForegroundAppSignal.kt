package com.darmix.darmixpet.monitor

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow


object ForegroundAppSignal {
    private val _events = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<String> = _events

    fun emit(packageName: String) {
        _events.tryEmit(packageName)
    }
}
