package com.darmix.darmixpet.update

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object ApkDownloader {

    sealed interface State {
        data object Idle : State
        data class Downloading(val version: String, val progress: Float?) : State
        data class Ready(val version: String, val uri: Uri) : State
        data class Failed(val version: String) : State
    }

    private const val TAG = "ApkDownloader"
    private const val MIME = "application/vnd.android.package-archive"
    private const val PREFIX = "darmixpet-"
    private const val POLL_MS = 400L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private var job: Job? = null
    private var downloadId: Long? = null

    fun canDownload(info: UpdateInfo): Boolean =
        info.downloadUrl.substringBefore('?').endsWith(".apk", ignoreCase = true)

    fun start(context: Context, info: UpdateInfo) {
        val app = context.applicationContext
        val version = info.latestVersion
        when (val current = _state.value) {
            is State.Downloading -> if (current.version == version) return
            is State.Ready -> if (current.version == version) {
                install(app, current.uri)
                return
            }
            else -> Unit
        }

        job?.cancel()
        downloadId?.let { id -> runCatching { manager(app).remove(id) } }
        downloadId = null
        _state.value = State.Downloading(version, null)

        job = scope.launch {
            val id = try {
                withContext(Dispatchers.IO) { enqueue(app, info) }
            } catch (e: Exception) {
                Log.d(TAG, "No se pudo iniciar la descarga: ${e.javaClass.simpleName}")
                _state.value = State.Failed(version)
                return@launch
            }
            downloadId = id
            while (isActive) {
                val snapshot = withContext(Dispatchers.IO) { query(app, id) }
                when (snapshot?.status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        val uri = withContext(Dispatchers.IO) { manager(app).getUriForDownloadedFile(id) }
                        downloadId = null
                        _state.value = if (uri != null) State.Ready(version, uri) else State.Failed(version)
                        return@launch
                    }

                    DownloadManager.STATUS_FAILED, null -> {
                        downloadId = null
                        _state.value = State.Failed(version)
                        return@launch
                    }

                    else -> _state.value = State.Downloading(version, snapshot.fraction)
                }
                delay(POLL_MS)
            }
        }
    }

    fun installReady(context: Context) {
        val ready = _state.value as? State.Ready ?: return
        install(context.applicationContext, ready.uri)
    }

    fun install(context: Context, uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            val settings = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(settings)
            } catch (e: ActivityNotFoundException) {
                Log.d(TAG, "No se pudo abrir el permiso de instalación")
            }
            return
        }
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, MIME)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.d(TAG, "No hay instalador disponible")
        }
    }

    private data class Snapshot(val status: Int, val downloaded: Long, val total: Long) {
        val fraction: Float? get() = if (total > 0) (downloaded.toFloat() / total).coerceIn(0f, 1f) else null
    }

    private fun enqueue(context: Context, info: UpdateInfo): Long {
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?.listFiles { file -> file.name.startsWith(PREFIX) && file.name.endsWith(".apk") }
            ?.forEach { it.delete() }
        val request = DownloadManager.Request(Uri.parse(info.downloadUrl))
            .setTitle("DarmixPet ${info.latestVersion}")
            .setDescription("Descargando actualización")
            .setMimeType(MIME)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                "$PREFIX${info.latestVersion}.apk"
            )
        return manager(context).enqueue(request)
    }

    private fun query(context: Context, id: Long): Snapshot? {
        manager(context).query(DownloadManager.Query().setFilterById(id))?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            return Snapshot(
                status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
                downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            )
        }
        return null
    }

    private fun manager(context: Context): DownloadManager =
        context.getSystemService(DownloadManager::class.java)
}
