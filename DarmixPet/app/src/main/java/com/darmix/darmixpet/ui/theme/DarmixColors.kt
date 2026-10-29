package com.darmix.darmixpet.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
class DarmixColors(
    val ink: Color,
    val shadow: Color,
    val accent: Color,
    val onAccent: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    val available: Color,
    val availableContainer: Color,
    val onAvailableContainer: Color,
    val cooldown: Color,
    val cooldownContainer: Color,
    val onCooldownContainer: Color,
    val blocked: Color,
    val blockedContainer: Color,
    val onBlockedContainer: Color
)

val LocalDarmixColors = staticCompositionLocalOf {
    AppPalettes.forSkin(AppPalettes.DEFAULT_SKIN_ID, dark = false).extras
}

val LocalIsDarkTheme = staticCompositionLocalOf { false }

object DarmixTheme {
    val colors: DarmixColors
        @Composable
        @ReadOnlyComposable
        get() = LocalDarmixColors.current

    val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalIsDarkTheme.current
}