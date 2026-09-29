package com.darmix.darmixpet.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.darmix.darmixpet.ui.AppScreen

@Composable
fun BottomNavBar(current: AppScreen, onSelect: (AppScreen) -> Unit) {
    NavigationBar {
        AppScreen.entries.forEach { screen ->
            NavigationBarItem(
                selected = screen == current,
                onClick = { onSelect(screen) },
                icon = { Text(screen.icon, style = MaterialTheme.typography.titleMedium) },
                label = { Text(screen.label) }
            )
        }
    }
}