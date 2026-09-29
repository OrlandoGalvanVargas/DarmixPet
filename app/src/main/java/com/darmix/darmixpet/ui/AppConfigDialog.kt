package com.darmix.darmixpet.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppConfigDialog(
    appName: String,
    initialSessionMinutes: Int = 30,
    initialCooldownMinutes: Int = 60,
    initialMaxSessions: Int = 3,
    onDismiss: () -> Unit,
    onConfirm: (sessionMinutes: Int, cooldownMinutes: Int, maxSessions: Int) -> Unit
) {
    var sessionText by remember { mutableStateOf(initialSessionMinutes.toString()) }
    var cooldownText by remember { mutableStateOf(initialCooldownMinutes.toString()) }
    var maxSessionsText by remember { mutableStateOf(initialMaxSessions.toString()) }

    val isValid = sessionText.toIntOrNull()?.let { it > 0 } == true &&
            cooldownText.toIntOrNull()?.let { it > 0 } == true &&
            maxSessionsText.toIntOrNull()?.let { it > 0 } == true

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column {
                Text("Configurar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(appName, style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ConfigField("⏱", "Minutos por sesión", sessionText) { sessionText = it.filter { c -> c.isDigit() } }
                ConfigField("❄️", "Minutos de enfriamiento", cooldownText) { cooldownText = it.filter { c -> c.isDigit() } }
                ConfigField("🔁", "Sesiones máximas al día", maxSessionsText) { maxSessionsText = it.filter { c -> c.isDigit() } }
            }
        },
        confirmButton = {
            Button(
                enabled = isValid,
                onClick = {
                    onConfirm(
                        sessionText.toIntOrNull() ?: initialSessionMinutes,
                        cooldownText.toIntOrNull() ?: initialCooldownMinutes,
                        maxSessionsText.toIntOrNull() ?: initialMaxSessions
                    )
                }
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun ConfigField(icon: String, label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        leadingIcon = { Text(icon) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    )
}