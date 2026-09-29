package com.darmix.darmixpet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.darmix.darmixpet.ui.theme.ThemeMode
import androidx.compose.foundation.clickable

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    currentThemeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text("Ajustes", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(24.dp))

        Text("Apariencia", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Elige cómo se ve DarmixPet",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp)
        ) {
            ThemeOption("☀", "Claro", ThemeMode.LIGHT, currentThemeMode, onThemeModeChange, Modifier.weight(1f))
            ThemeOption("🌙", "Oscuro", ThemeMode.DARK, currentThemeMode, onThemeModeChange, Modifier.weight(1f))
            ThemeOption("🌗", "Sistema", ThemeMode.SYSTEM, currentThemeMode, onThemeModeChange, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(20.dp))

        Text("Acerca de", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "DarmixPet — tu compañero de tiempo de pantalla",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ThemeOption(
    icon: String,
    label: String,
    mode: ThemeMode,
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = mode == current
    val bg = if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable { onSelect(mode) }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, style = MaterialTheme.typography.titleMedium, color = fg)
        Text(label, style = MaterialTheme.typography.labelSmall, color = fg)
    }
}