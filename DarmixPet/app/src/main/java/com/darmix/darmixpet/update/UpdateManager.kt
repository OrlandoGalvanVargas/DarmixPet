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

    enum class CheckState { IDLE, CHECKING, UP_TO_DATE, FAILED }

    private const val CHECK_INTERVAL_MS = 30L * 60 * 1000
    private const val MANUAL_COOLDOWN_MS = 20_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _offer = MutableStateFlow<UpdateInfo?>(null)
    val offer: StateFlow<UpdateInfo?> = _offer.asStateFlow()

    private val _checkState = MutableStateFlow(CheckState.IDLE)
    val checkState: StateFlow<CheckState> = _checkState.asStateFlow()

    private val _dialogRequest = MutableStateFlow(0)
    val dialogRequest: StateFlow<Int> = _dialogRequest.asStateFlow()

    @Volatile private var needsRetry = false
    private var checking = false
    private var lastManualAt = 0L

    fun loadCached(context: Context) {
        val app = context.applicationContext
        val cached = UpdateStore.cachedOffer(app)
        if (cached != null) {
            if (UpdateChecker.isNewerThanInstalled(cached.latestVersion)) {
                if (_offer.value == null) _offer.value = cached
            } else {
                UpdateStore.clearOffer(app)
            }
        }
        if (_offer.value == null && _checkState.value == CheckState.IDLE && UpdateStore.lastCheck(app) > 0L) {
            _checkState.value = CheckState.UP_TO_DATE
        }
    }

    fun checkIfDue(context: Context) {
        val app = context.applicationContext
        val elapsed = System.currentTimeMillis() - UpdateStore.lastCheck(app)

        if (_offer.value == null || elapsed > CHECK_INTERVAL_MS) {
            runCheck(app, manual = false)
        }
    }

    fun checkNow(context: Context) {
        val now = System.currentTimeMillis()
        if (_checkState.value == CheckState.UP_TO_DATE && _offer.value == null &&
            now - lastManualAt < MANUAL_COOLDOWN_MS
        ) return
        lastManualAt = now
        runCheck(context.applicationContext, manual = true)
    }

    fun onNetworkAvailable(context: Context) {
        if (!needsRetry) return
        val app = context.applicationContext
        scope.launch {
            delay(2_000)
            if (needsRetry) runCheck(app, manual = false)
        }
    }

    fun requestDialog() {
        if (_offer.value != null) _dialogRequest.value += 1
    }

    private fun runCheck(app: Context, manual: Boolean) {
        if (checking) return
        checking = true
        if (manual) _checkState.value = CheckState.CHECKING
        scope.launch {
            when (val result = UpdateChecker.check()) {
                is CheckResult.Update -> {
                    UpdateStore.saveOffer(app, result.info)
                    UpdateStore.markChecked(app)
                    needsRetry = false
                    _offer.value = result.info
                    _checkState.value = CheckState.IDLE
                    if (manual) requestDialog()
                }
                CheckResult.UpToDate -> {
                    UpdateStore.clearOffer(app)
                    UpdateStore.markChecked(app)
                    needsRetry = false
                    _offer.value = null
                    _checkState.value = CheckState.UP_TO_DATE
                }
                CheckResult.Failed -> {
                    needsRetry = true
                    if (manual) _checkState.value = CheckState.FAILED
                }
            }
            checking = false
        }
    }
}
