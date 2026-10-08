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
import com.darmix.darmixpet.update.UpdateChecker
import com.darmix.darmixpet.update.UpdateManager

@Composable
fun UpdateHost(suppress: Boolean = false) {
    val context = LocalContext.current
    val offer by UpdateManager.offer.collectAsState()
    val request by UpdateManager.dialogRequest.collectAsState()

    var dialogOpen by rememberSaveable { mutableStateOf(false) }
    var autoPrompted by rememberSaveable { mutableStateOf(false) }
    var handledRequest by rememberSaveable { mutableStateOf(0) }

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

    val current = offer
    if (dialogOpen && current != null && !suppress) {
        UpdateDialog(
            latestVersion = current.latestVersion,
            currentVersion = BuildConfig.VERSION_NAME,
            releaseNotes = current.releaseNotes,
            onUpdateClick = {
                UpdateChecker.openDownloadUrl(context, current.downloadUrl)
                dialogOpen = false
            },
            onDismiss = { dialogOpen = false }
        )
    }
}
