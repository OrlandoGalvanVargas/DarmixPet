package com.darmix.darmixpet.ui

import androidx.compose.ui.graphics.vector.ImageVector
import com.darmix.darmixpet.ui.icons.DarmixIcons

enum class AppScreen(val label: String, val icon: ImageVector) {
    APPS("Apps", DarmixIcons.AppsGrid),
    MASCOT("Mascota", DarmixIcons.Paw),
    SETTINGS("Ajustes", DarmixIcons.Cog)
}