package com.darmix.darmixpet.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.darmix.darmixpet.data.AppDatabase
import com.darmix.darmixpet.data.InstalledAppInfo
import com.darmix.darmixpet.data.InstalledAppsProvider
import com.darmix.darmixpet.data.MonitoredAppEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun MonitoredAppEntity.hasActiveSession(): Boolean = usedInSessionMillis > 0L

@Composable
fun AppListScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { AppDatabase.getInstance(context).monitoredAppDao() }

    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val monitoredApps by dao.getAllMonitoredApps().collectAsState(initial = emptyList())

    var appPendingConfig by remember { mutableStateOf<InstalledAppInfo?>(null) }
    var existingConfigForDialog by remember { mutableStateOf<MonitoredAppEntity?>(null) }

    LaunchedEffect(Unit) {
        installedApps = withContext(Dispatchers.IO) {
            InstalledAppsProvider.getLaunchableApps(context)
        }
        isLoading = false
    }

    val monitoredMap = monitoredApps.associateBy { it.packageName }

    if (isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    fun notifyLocked(appName: String) {
        Toast.makeText(
            context,
            "No puedes modificar \"$appName\" mientras tiene una sesión activa.",
            Toast.LENGTH_SHORT
        ).show()
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text("Tus apps", style = MaterialTheme.typography.titleLarge)
            val monitoredCount = monitoredApps.size
            Text(
                if (monitoredCount == 0) "Ninguna app monitoreada todavía"
                else "$monitoredCount app${if (monitoredCount == 1) "" else "s"} bajo vigilancia de Archi",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(installedApps, key = { it.packageName }) { app ->
                val monitored = monitoredMap[app.packageName]
                val locked = monitored?.hasActiveSession() == true

                AppRow(
                    app = app,
                    monitored = monitored,
                    locked = locked,
                    onRowClick = {
                        if (monitored == null) return@AppRow
                        if (locked) notifyLocked(app.appName)
                        else {
                            appPendingConfig = app
                            existingConfigForDialog = monitored
                        }
                    },
                    onSwitchChange = { checked ->
                        if (checked) {
                            appPendingConfig = app
                            existingConfigForDialog = null
                        } else {
                            scope.launch { dao.deleteByPackageName(app.packageName) }
                        }
                    }
                )
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    appPendingConfig?.let { app ->
        val existing = existingConfigForDialog
        AppConfigDialog(
            appName = app.appName,
            initialSessionMinutes = existing?.sessionLimitMinutes ?: 30,
            initialCooldownMinutes = existing?.cooldownMinutes ?: 60,
            initialMaxSessions = existing?.maxSessionsPerDay ?: 3,
            onDismiss = {
                appPendingConfig = null
                existingConfigForDialog = null
            },
            onConfirm = { sessionMinutes, cooldownMinutes, maxSessions ->
                scope.launch {
                    dao.upsert(
                        (existing ?: MonitoredAppEntity(
                            packageName = app.packageName,
                            appName = app.appName,
                            sessionLimitMinutes = sessionMinutes,
                            cooldownMinutes = cooldownMinutes,
                            maxSessionsPerDay = maxSessions
                        )).copy(
                            sessionLimitMinutes = sessionMinutes,
                            cooldownMinutes = cooldownMinutes,
                            maxSessionsPerDay = maxSessions
                        )
                    )
                }
                appPendingConfig = null
                existingConfigForDialog = null
            }
        )
    }
}

@Composable
private fun AppRow(
    app: InstalledAppInfo,
    monitored: MonitoredAppEntity?,
    locked: Boolean,
    onRowClick: () -> Unit,
    onSwitchChange: (Boolean) -> Unit
) {
    val isActive = monitored != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isActive) { onRowClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 1.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Image(bitmap = app.icon, contentDescription = app.appName, modifier = Modifier.size(30.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(app.appName, style = MaterialTheme.typography.bodyLarge)
                if (monitored != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InfoChip("${monitored.sessionLimitMinutes}m")
                        Spacer(modifier = Modifier.width(6.dp))
                        InfoChip("❄️${monitored.cooldownMinutes}m")
                        Spacer(modifier = Modifier.width(6.dp))
                        InfoChip("${monitored.sessionsUsedToday}/${monitored.maxSessionsPerDay}")
                        if (locked) {
                            Spacer(modifier = Modifier.width(6.dp))
                            InfoChip("🔒", tint = MaterialTheme.colorScheme.errorContainer, textColor = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            Switch(
                checked = isActive,
                enabled = !locked,
                onCheckedChange = onSwitchChange
            )
        }
    }
}

@Composable
private fun InfoChip(
    text: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.secondaryContainer,
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(tint)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = textColor)
    }
}