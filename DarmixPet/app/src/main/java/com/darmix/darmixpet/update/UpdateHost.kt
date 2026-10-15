package com.darmix.darmixpet.ui

import android.net.ConnectivityManager
import android.net.Network
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.darmix.darmixpet.BuildConfig
import com.darmix.darmixpet.update.ApkDownloader
import com.darmix.darmixpet.update.UpdateChecker
import com.darmix.darmixpet.update.UpdateManager

@Composable
fun UpdateHost(suppress: Boolean = false) {
    val context = LocalContext.current
    val offer by UpdateManager.offer.collectAsState()
    val request by UpdateManager.dialogRequest.collectAsState()
    val download by ApkDownloader.state.collectAsState()

    var dialogOpen by rememberSaveable { mutableStateOf(false) }
    var autoPrompted by rememberSaveable { mutableStateOf(false) }
    var handledRequest by rememberSaveable { mutableStateOf(0) }
    var autoInstalled by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        UpdateManager.loadCached(context)
        UpdateManager.checkIfDue(context)
    }

    DisposableEffect(Unit) {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                UpdateManager.onNetworkAvailable(context)
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

    LaunchedEffect(offer) {
        if (offer != null && !autoPrompted) {
            autoPrompted = true
            dialogOpen = true
        }
    }

    LaunchedEffect(request) {
        if (request > handledRequest) {
            handledRequest = request
            if (offer != null) dialogOpen = true
        }
    }

    LaunchedEffect(download) {
        val ready = download as? ApkDownloader.State.Ready ?: return@LaunchedEffect
        if (autoInstalled != ready.version) {
            autoInstalled = ready.version
            if (offer != null) dialogOpen = true
            ApkDownloader.install(context, ready.uri)
        }
    }

    val current = offer
    if (dialogOpen && current != null && !suppress) {
        val state = download.takeIf { it.versionOrNull() == current.latestVersion } ?: ApkDownloader.State.Idle
        UpdateDialog(
            latestVersion = current.latestVersion,
            currentVersion = BuildConfig.VERSION_NAME,
            releaseNotes = current.releaseNotes,
            downloadState = state,
            onUpdateClick = {
                if (ApkDownloader.canDownload(current)) {
                    ApkDownloader.start(context, current)
                } else {
                    UpdateChecker.openDownloadUrl(context, current.downloadUrl)
                    dialogOpen = false
                }
            },
            onInstallClick = { ApkDownloader.installReady(context) },
            onDismiss = { dialogOpen = false }
        )
    }
}

private fun ApkDownloader.State.versionOrNull(): String? = when (this) {
    ApkDownloader.State.Idle -> null
    is ApkDownloader.State.Downloading -> version
    is ApkDownloader.State.Ready -> version
    is ApkDownloader.State.Failed -> version
}
