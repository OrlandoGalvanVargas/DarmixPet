package com.darmix.darmixpet.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.darmix.darmixpet.monitor.AccessibilityPermissionHelper
import com.darmix.darmixpet.monitor.BatteryOptimizationHelper
import com.darmix.darmixpet.monitor.BrightnessPermissionHelper
import com.darmix.darmixpet.overlay.NotificationPermissionHelper
import com.darmix.darmixpet.overlay.OverlayPermissionHelper
import com.darmix.darmixpet.monitor.UsageStatsPermissionHelper

private data class PermItem(val icon: String, val label: String, val granted: Boolean, val helper: String?, val onClick: () -> Unit)

@Composable
fun PermissionsScreen(onAllGranted: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasOverlay by remember { mutableStateOf(OverlayPermissionHelper.hasOverlayPermission(context)) }
    var hasUsageStats by remember { mutableStateOf(UsageStatsPermissionHelper.hasUsageStatsPermission(context)) }
    var hasNotifications by remember { mutableStateOf(NotificationPermissionHelper.hasNotificationPermission(context)) }
    var hasBatteryExemption by remember { mutableStateOf(BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)) }
    var hasAccessibility by remember { mutableStateOf(AccessibilityPermissionHelper.isAccessibilityServiceEnabled(context)) }
    var hasWriteSettings by remember { mutableStateOf(BrightnessPermissionHelper.hasWriteSettingsPermission(context)) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasNotifications = granted }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlay = OverlayPermissionHelper.hasOverlayPermission(context)
                hasUsageStats = UsageStatsPermissionHelper.hasUsageStatsPermission(context)
                hasNotifications = NotificationPermissionHelper.hasNotificationPermission(context)
                hasBatteryExemption = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
                hasAccessibility = AccessibilityPermissionHelper.isAccessibilityServiceEnabled(context)
                hasWriteSettings = BrightnessPermissionHelper.hasWriteSettingsPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(hasOverlay, hasUsageStats, hasNotifications, hasBatteryExemption, hasAccessibility, hasWriteSettings) {
        if (hasOverlay && hasUsageStats && hasNotifications && hasBatteryExemption && hasAccessibility && hasWriteSettings) {
            onAllGranted()
        }
    }

    val items = listOf(
        PermItem("🪟", "Mostrar sobre otras apps", hasOverlay, null) {
            context.startActivity(OverlayPermissionHelper.buildPermissionIntent(context))
        },
        PermItem("📊", "Acceso a datos de uso", hasUsageStats, "Busca \"DarmixPet\" en la lista y actívalo") {
            context.startActivity(UsageStatsPermissionHelper.buildPermissionIntent())
        },
        PermItem("🔔", "Notificaciones", hasNotifications, null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        PermItem("🔋", "Ignorar optimización de batería", hasBatteryExemption, "Para que Archi no desaparezca por ahorro de batería") {
            context.startActivity(BatteryOptimizationHelper.buildPermissionIntent(context))
        },
        PermItem("♿", "Detección instantánea", hasAccessibility, "Si el interruptor está bloqueado: Ajustes de la app → ⋮ → Permitir configuración restringida") {
            context.startActivity(AccessibilityPermissionHelper.buildPermissionIntent())
        },
        PermItem("🎨", "Modificar ajustes del sistema", hasWriteSettings, "Para ajustar el brillo desde el menú rápido") {
            context.startActivity(BrightnessPermissionHelper.buildPermissionIntent(context))
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text("🧙", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Antes de empezar", style = MaterialTheme.typography.titleLarge)
        Text(
            "DarmixPet necesita estos permisos para cuidar tu tiempo de pantalla",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        items.forEach { item ->
            PermissionCard(item)
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun PermissionCard(item: PermItem) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (item.granted) MaterialTheme.colorScheme.tertiaryContainer
                            else MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.icon, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.label, style = MaterialTheme.typography.bodyLarge)
                    if (item.granted) {
                        Text("Activado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
                if (!item.granted) {
                    Button(
                        onClick = item.onClick,
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Activar") }
                }
            }
            if (!item.granted && item.helper != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(item.helper, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}