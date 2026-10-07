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

/**
 * Anfitrión del aviso de actualización. Ponlo una sola vez por encima de las pantallas.
 *
 *  - Al abrir la app comprueba en silencio (como máximo cada 6 h) y, si hay versión nueva, abre el
 *    diálogo UNA vez por apertura; "Más tarde" lo cierra hasta la próxima apertura.
 *  - También abre el diálogo cuando se lo piden desde Ajustes (UpdateManager.requestDialog()).
 *  - Sin internet no muestra nada; reintenta solo cuando vuelve la conexión.
 *
 * @param suppress true para esconder el diálogo un momento (por ejemplo, mientras se ve la guía).
 */
@Composable
fun UpdateHost(suppress: Boolean = false) {
    val context = LocalContext.current
    val offer by UpdateManager.offer.collectAsState()
    val request by UpdateManager.dialogRequest.collectAsState()

    var dialogOpen by rememberSaveable { mutableStateOf(false) }
    var autoPrompted by rememberSaveable { mutableStateOf(false) }
    var handledRequest by rememberSaveable { mutableStateOf(0) }

    // Al abrir: lo guardado primero (instantáneo, funciona sin internet) y luego, si toca, GitHub.
    LaunchedEffect(Unit) {
        UpdateManager.loadCached(context)
        UpdateManager.checkIfDue(context)
    }

    // Cuando vuelve el internet, el administrador reintenta una sola vez y sin avisar de nada.
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
            false // sin ACCESS_NETWORK_STATE simplemente se reintenta en la próxima apertura
        }
        onDispose {
            if (registered) {
                try { cm?.unregisterNetworkCallback(callback) } catch (e: Exception) { /* nada */ }
            }
        }
    }

    // Primera vez que hay oferta en esta apertura: se muestra sola.
    LaunchedEffect(offer) {
        if (offer != null && !autoPrompted) {
            autoPrompted = true
            dialogOpen = true
        }
    }

    // Petición explícita desde Ajustes (chip o tarjeta "Acerca de").
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
