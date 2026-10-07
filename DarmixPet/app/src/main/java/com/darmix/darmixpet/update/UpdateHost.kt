package com.darmix.darmixpet.ui

import android.net.ConnectivityManager
import android.net.Network
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.darmix.darmixpet.BuildConfig
import com.darmix.darmixpet.update.CheckResult
import com.darmix.darmixpet.update.UpdateChecker
import com.darmix.darmixpet.update.UpdateInfo
import com.darmix.darmixpet.update.UpdateStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val CHECK_INTERVAL_MS = 6L * 60 * 60 * 1000

@Composable
fun UpdateHost(suppress: Boolean = false) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var offer by remember { mutableStateOf<UpdateInfo?>(null) }
    var dismissed by rememberSaveable { mutableStateOf(false) }
    var needsRetry by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }

    fun runCheck() {
        if (checking) return
        checking = true
        scope.launch {
            when (val result = UpdateChecker.check()) {
                is CheckResult.Update -> {
                    UpdateStore.saveOffer(context, result.info)
                    UpdateStore.markChecked(context)
                    offer = result.info
                    needsRetry = false
                }
                CheckResult.UpToDate -> {
                    UpdateStore.clearOffer(context)
                    UpdateStore.markChecked(context)
                    offer = null
                    needsRetry = false
                }
                CheckResult.Failed -> needsRetry = true
            }
            checking = false
        }
    }

    LaunchedEffect(Unit) {
        val cached = UpdateStore.cachedOffer(context)
        if (cached != null) {
            if (UpdateChecker.isNewerThanInstalled(cached.latestVersion)) offer = cached
            else UpdateStore.clearOffer(context)
        }
        if (System.currentTimeMillis() - UpdateStore.lastCheck(context) > CHECK_INTERVAL_MS) runCheck()
    }

    DisposableEffect(Unit) {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (needsRetry) {
                    scope.launch {
                        delay(2_000)
                        if (needsRetry) runCheck()
                    }
                }
            }
        }
        val registered = try {
            cm?.registerDefaultNetworkCallback(callback)
            true
        } catch (e: Exception) {
            false
        }
        onDispose {
            if (registered) {
                try { cm?.unregisterNetworkCallback(callback) } catch (e: Exception) { }
            }
        }
    }

    val current = offer
    if (current != null && !dismissed && !suppress) {
        UpdateDialog(
            latestVersion = current.latestVersion,
            currentVersion = BuildConfig.VERSION_NAME,
            releaseNotes = current.releaseNotes,
            onUpdateClick = {
                UpdateChecker.openDownloadUrl(context, current.downloadUrl)
                dismissed = true
            },
            onDismiss = { dismissed = true }
        )
    }
}
