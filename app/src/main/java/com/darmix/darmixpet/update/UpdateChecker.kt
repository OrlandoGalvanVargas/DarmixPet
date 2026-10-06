package com.darmix.darmixpet.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.darmix.darmixpet.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val latestVersion: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val releaseUrl: String
)

sealed interface CheckResult {
    data class Update(val info: UpdateInfo) : CheckResult
    data object UpToDate : CheckResult
    data object Failed : CheckResult
}

object UpdateChecker {

    private const val TAG = "UpdateChecker"

    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/OrlandoGalvanVargas/DarmixPet/releases/latest"

    suspend fun check(): CheckResult = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6_000
                readTimeout = 6_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                setRequestProperty("User-Agent", "DarmixPet/${BuildConfig.VERSION_NAME}")
            }
            when (connection.responseCode) {
                200 -> parse(connection.inputStream.bufferedReader().use { it.readText() })
                404 -> CheckResult.UpToDate
                else -> CheckResult.Failed
            }
        } catch (e: Exception) {
            Log.d(TAG, "No se pudo comprobar actualizaciones: ${e.javaClass.simpleName}")
            CheckResult.Failed
        } finally {
            connection?.disconnect()
        }
    }

    private fun parse(text: String): CheckResult {
        val json = JSONObject(text)
        if (json.optBoolean("draft") || json.optBoolean("prerelease")) return CheckResult.UpToDate

        val latest = json.optString("tag_name", "").trim().removePrefix("v").removePrefix("V")
        if (latest.isEmpty() || !isNewerThanInstalled(latest)) return CheckResult.UpToDate

        val releaseUrl = json.optString("html_url", "")
        var downloadUrl = releaseUrl
        val assets = json.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.optString("name", "").endsWith(".apk", ignoreCase = true)) {
                    downloadUrl = asset.optString("browser_download_url", downloadUrl)
                    break
                }
            }
        }

        return CheckResult.Update(
            UpdateInfo(
                latestVersion = latest,
                releaseNotes = json.optString("body", "").trim(),
                downloadUrl = downloadUrl,
                releaseUrl = releaseUrl
            )
        )
    }

    fun isNewerThanInstalled(latest: String): Boolean = isNewer(latest, BuildConfig.VERSION_NAME)

    private fun isNewer(latest: String, current: String): Boolean {
        val a = numericParts(latest)
        val b = numericParts(current)
        if (a.isEmpty() || b.isEmpty()) return false
        for (i in 0 until maxOf(a.size, b.size)) {
            val l = a.getOrElse(i) { 0 }
            val c = b.getOrElse(i) { 0 }
            if (l != c) return l > c
        }
        return false
    }

    private fun numericParts(version: String): List<Int> =
        Regex("""^\d+(\.\d+)*""").find(version.trim())?.value
            ?.split(".")?.mapNotNull { it.toIntOrNull() }
            ?: emptyList()

    fun openDownloadUrl(context: Context, url: String) {
        if (url.isBlank()) return
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            Log.d(TAG, "No hay navegador para abrir $url")
        }
    }
}
