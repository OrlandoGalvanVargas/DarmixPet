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
    val gold: Color,
    val onGold: Color,
    val goldContainer: Color,
    val onGoldContainer: Color,
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

val LightDarmixColors = DarmixColors(
    ink = Color(0xFF3D3350),
    shadow = Color(0xFF3D3350),
    gold = Color(0xFFE8B84A),
    onGold = Color(0xFF4A3410),
    goldContainer = Color(0xFFFDF0CC),
    onGoldContainer = Color(0xFF5C4310),
    available = Color(0xFF5C8A57),
    availableContainer = Color(0xFFDDEEDA),
    onAvailableContainer = Color(0xFF23361F),
    cooldown = Color(0xFFC98A4B),
    cooldownContainer = Color(0xFFFBEBD8),
    onCooldownContainer = Color(0xFF5C3D1A),
    blocked = Color(0xFFC96262),
    blockedContainer = Color(0xFFF7DEDE),
    onBlockedContainer = Color(0xFF5C2323)
)

val DarkDarmixColors = DarmixColors(
    ink = Color(0xFF7A6D94),
    shadow = Color(0xFF140F19),
    gold = Color(0xFFF5D07A),
    onGold = Color(0xFF4A3410),
    goldContainer = Color(0xFF4D4123),
    onGoldContainer = Color(0xFFFBEBC0),
    available = Color(0xFFA8D1A3),
    availableContainer = Color(0xFF35482F),
    onAvailableContainer = Color(0xFFDDEEDA),
    cooldown = Color(0xFFF0C08A),
    cooldownContainer = Color(0xFF4A3B2B),
    onCooldownContainer = Color(0xFFFBEBD8),
    blocked = Color(0xFFEDA3A3),
    blockedContainer = Color(0xFF5C2323),
    onBlockedContainer = Color(0xFFF7DEDE)
)

val LocalDarmixColors = staticCompositionLocalOf { LightDarmixColors }

object DarmixTheme {
    val colors: DarmixColors
        @Composable
        @ReadOnlyComposable
        get() = LocalDarmixColors.current
}
