package com.darmix.darmixpet.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
class ThemePalette(val scheme: ColorScheme, val extras: DarmixColors)

private class StatusTones(
    val available: Color,
    val onAvailable: Color,
    val availableContainer: Color,
    val onAvailableContainer: Color,
    val cooldown: Color,
    val onCooldown: Color,
    val cooldownContainer: Color,
    val onCooldownContainer: Color,
    val blocked: Color,
    val onBlocked: Color,
    val blockedContainer: Color,
    val onBlockedContainer: Color
)

private class BaseTones(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val ink: Color,
    val shadow: Color,
    val text: Color,
    val textVariant: Color,
    val outline: Color
)

private class BrandTones(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val accent: Color,
    val onAccent: Color,
    val accentContainer: Color,
    val onAccentContainer: Color
)

private val LightStatus = StatusTones(
    available = Color(0xFF3F8F55),
    onAvailable = Color(0xFFFFFFFF),
    availableContainer = Color(0xFFDDEFD9),
    onAvailableContainer = Color(0xFF1E4D2B),
    cooldown = Color(0xFF3B7FC0),
    onCooldown = Color(0xFFFFFFFF),
    cooldownContainer = Color(0xFFDCEBF8),
    onCooldownContainer = Color(0xFF1F4A70),
    blocked = Color(0xFFC8463E),
    onBlocked = Color(0xFFFFFFFF),
    blockedContainer = Color(0xFFF9DEDA),
    onBlockedContainer = Color(0xFF7A2520)
)

private val DarkStatus = StatusTones(
    available = Color(0xFF8FD19A),
    onAvailable = Color(0xFF0F2A16),
    availableContainer = Color(0xFF1E2E24),
    onAvailableContainer = Color(0xFFCDEFD3),
    cooldown = Color(0xFF8CC3F2),
    onCooldown = Color(0xFF0B2540),
    cooldownContainer = Color(0xFF1A2A3D),
    onCooldownContainer = Color(0xFFD3E7FA),
    blocked = Color(0xFFF2897A),
    onBlocked = Color(0xFF4A1510),
    blockedContainer = Color(0xFF3A1F1D),
    onBlockedContainer = Color(0xFFFBD9D3)
)

private val DarkBase = BaseTones(
    background = Color(0xFF2B2D31),
    surface = Color(0xFF313338),
    surfaceVariant = Color(0xFF383A40),
    ink = Color(0xFF46484F),
    shadow = Color(0xFF1E1F22),
    text = Color(0xFFF2F3F5),
    textVariant = Color(0xFFB5BAC1),
    outline = Color(0xFF46484F)
)

private val ArchiLightBase = BaseTones(
    background = Color(0xFFF3F7FC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE6EEF8),
    ink = Color(0xFF12305C),
    shadow = Color(0xFF12305C),
    text = Color(0xFF12305C),
    textVariant = Color(0xFF5A6B85),
    outline = Color(0xFFC9D6E8)
)

private val ArchiLightBrand = BrandTones(
    primary = Color(0xFF1E63D6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCEBFC),
    onPrimaryContainer = Color(0xFF12305C),
    accent = Color(0xFF22B8D8),
    onAccent = Color(0xFF062A33),
    accentContainer = Color(0xFFDDF6FB),
    onAccentContainer = Color(0xFF0B4A57)
)

private val ArchiDarkBrand = BrandTones(
    primary = Color(0xFF2F6FE0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF31405D),
    onPrimaryContainer = Color(0xFFD3E2FF),
    accent = Color(0xFF7FD6EA),
    onAccent = Color(0xFF062A33),
    accentContainer = Color(0xFF2E4049),
    onAccentContainer = Color(0xFFCDF3FB)
)

private val SylvaLightBase = BaseTones(
    background = Color(0xFFF3F6EE),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE5ECDC),
    ink = Color(0xFF1F3A2A),
    shadow = Color(0xFF1F3A2A),
    text = Color(0xFF1F3A2A),
    textVariant = Color(0xFF63705E),
    outline = Color(0xFFCFD8C2)
)

private val SylvaLightBrand = BrandTones(
    primary = Color(0xFF457D22),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE4F1D6),
    onPrimaryContainer = Color(0xFF1F3A2A),
    accent = Color(0xFF3FBF7A),
    onAccent = Color(0xFF0B2E1C),
    accentContainer = Color(0xFFDCF0E4),
    onAccentContainer = Color(0xFF14532D)
)

private val SylvaDarkBrand = BrandTones(
    primary = Color(0xFF2A7F50),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF2F443D),
    onPrimaryContainer = Color(0xFFCFEFD9),
    accent = Color(0xFF7FD3A4),
    onAccent = Color(0xFF0B2E1C),
    accentContainer = Color(0xFF2E3F3D),
    onAccentContainer = Color(0xFFCFF2DE)
)

private val GalahadLightBase = BaseTones(
    background = Color(0xFFF7F2EA),
    surface = Color(0xFFFFFDF9),
    surfaceVariant = Color(0xFFEEE6D8),
    ink = Color(0xFF3A3631),
    shadow = Color(0xFF3A3631),
    text = Color(0xFF2E2B27),
    textVariant = Color(0xFF6F675C),
    outline = Color(0xFFDDD3C2)
)

private val GalahadLightBrand = BrandTones(
    primary = Color(0xFFD8B67E),
    onPrimary = Color(0xFF2B1E0E),
    primaryContainer = Color(0xFFF4E8D2),
    onPrimaryContainer = Color(0xFF4A3412),
    accent = Color(0xFF6B6F78),
    onAccent = Color(0xFFFFFFFF),
    accentContainer = Color(0xFFE7E4DE),
    onAccentContainer = Color(0xFF3A3D44)
)

private val GalahadDarkBrand = BrandTones(
    primary = Color(0xFF9C6B47),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF483F3B),
    onPrimaryContainer = Color(0xFFF2DCC9),
    accent = Color(0xFFB8BEC8),
    onAccent = Color(0xFF1E232A),
    accentContainer = Color(0xFF2C3036),
    onAccentContainer = Color(0xFFE3E6EA)
)

private fun buildPalette(dark: Boolean, base: BaseTones, brand: BrandTones): ThemePalette {
    val status = if (dark) DarkStatus else LightStatus
    val defaults = if (dark) darkColorScheme() else lightColorScheme()
    val scheme = defaults.copy(
        primary = brand.primary,
        onPrimary = brand.onPrimary,
        primaryContainer = brand.primaryContainer,
        onPrimaryContainer = brand.onPrimaryContainer,
        secondary = status.cooldown,
        onSecondary = status.onCooldown,
        secondaryContainer = status.cooldownContainer,
        onSecondaryContainer = status.onCooldownContainer,
        tertiary = status.available,
        onTertiary = status.onAvailable,
        tertiaryContainer = status.availableContainer,
        onTertiaryContainer = status.onAvailableContainer,
        error = status.blocked,
        onError = status.onBlocked,
        errorContainer = status.blockedContainer,
        onErrorContainer = status.onBlockedContainer,
        background = base.background,
        onBackground = base.text,
        surface = base.surface,
        onSurface = base.text,
        surfaceVariant = base.surfaceVariant,
        onSurfaceVariant = base.textVariant,
        outline = base.outline,
        surfaceTint = brand.primary
    )
    val extras = DarmixColors(
        ink = base.ink,
        shadow = base.shadow,
        accent = brand.accent,
        onAccent = brand.onAccent,
        accentContainer = brand.accentContainer,
        onAccentContainer = brand.onAccentContainer,
        available = status.available,
        availableContainer = status.availableContainer,
        onAvailableContainer = status.onAvailableContainer,
        cooldown = status.cooldown,
        cooldownContainer = status.cooldownContainer,
        onCooldownContainer = status.onCooldownContainer,
        blocked = status.blocked,
        blockedContainer = status.blockedContainer,
        onBlockedContainer = status.onBlockedContainer
    )
    return ThemePalette(scheme, extras)
}

object AppPalettes {

    const val DEFAULT_SKIN_ID = "archimago"

    private val lightPalettes: Map<String, ThemePalette> = mapOf(
        "archimago" to buildPalette(false, ArchiLightBase, ArchiLightBrand),
        "skin2" to buildPalette(false, SylvaLightBase, SylvaLightBrand),
        "skin3" to buildPalette(false, GalahadLightBase, GalahadLightBrand)
    )

    private val darkPalettes: Map<String, ThemePalette> = mapOf(
        "archimago" to buildPalette(true, DarkBase, ArchiDarkBrand),
        "skin2" to buildPalette(true, DarkBase, SylvaDarkBrand),
        "skin3" to buildPalette(true, DarkBase, GalahadDarkBrand)
    )

    fun forSkin(skinId: String, dark: Boolean): ThemePalette {
        val source = if (dark) darkPalettes else lightPalettes
        return source[skinId] ?: source.getValue(DEFAULT_SKIN_ID)
    }
}