package com.darmix.darmixpet.update

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object UpdateManager {

    private const val CHECK_INTERVAL_MS = 30L * 60 * 1000

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _offer = MutableStateFlow<UpdateInfo?>(null)
    val offer: StateFlow<UpdateInfo?> = _offer.asStateFlow()

    private val _dialogRequest = MutableStateFlow(0)
    val dialogRequest: StateFlow<Int> = _dialogRequest.asStateFlow()

    @Volatile private var needsRetry = false
    private var checking = false

    fun loadCached(context: Context) {
        val app = context.applicationContext
        val cached = UpdateStore.cachedOffer(app) ?: return
        if (UpdateChecker.isNewerThanInstalled(cached.latestVersion)) {
            if (_offer.value == null) _offer.value = cached
        } else {
            UpdateStore.clearOffer(app)
        }
    }

    fun checkIfDue(context: Context) {
        val app = context.applicationContext
        val elapsed = System.currentTimeMillis() - UpdateStore.lastCheck(app)
        if (_offer.value == null || elapsed > CHECK_INTERVAL_MS) runCheck(app)
    }

    fun onNetworkAvailable(context: Context) {
        if (!needsRetry) return
        val app = context.applicationContext
        scope.launch {
            delay(2_000)
            if (needsRetry) runCheck(app)
        }
    }

    fun requestDialog() {
        if (_offer.value != null) _dialogRequest.value += 1
    }

    private fun runCheck(app: Context) {
        if (checking) return
        checking = true
        scope.launch {
            when (val result = UpdateChecker.check()) {
                is CheckResult.Update -> {
                    UpdateStore.saveOffer(app, result.info)
                    UpdateStore.markChecked(app)
                    needsRetry = false
                    _offer.value = result.info
                }
                CheckResult.UpToDate -> {
                    UpdateStore.clearOffer(app)
                    UpdateStore.markChecked(app)
                    needsRetry = false
                    _offer.value = null
                }
                CheckResult.Failed -> needsRetry = true
            }
            checking = false
        }
    }
}