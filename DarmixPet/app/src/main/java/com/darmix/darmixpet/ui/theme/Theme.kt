package com.darmix.darmixpet.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

private const val PALETTE_ANIMATION_MS = 450

@Composable
fun DarmixPetTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    skinId: String = AppPalettes.DEFAULT_SKIN_ID,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val palette = animatePalette(AppPalettes.forSkin(skinId, useDarkTheme))

    CompositionLocalProvider(
        LocalDarmixColors provides palette.extras,
        LocalIsDarkTheme provides useDarkTheme
    ) {
        MaterialTheme(
            colorScheme = palette.scheme,
            typography = Typography,
            shapes = DarmixShapes,
            content = content
        )
    }
}

@Composable
private fun animatePalette(target: ThemePalette): ThemePalette {
    val from = remember { mutableStateOf(target) }
    val to = remember { mutableStateOf(target) }
    val progress = remember { Animatable(1f) }

    LaunchedEffect(target) {
        if (target === to.value) return@LaunchedEffect
        from.value = lerpPalette(from.value, to.value, progress.value)
        to.value = target
        progress.snapTo(0f)
        progress.animateTo(1f, tween(PALETTE_ANIMATION_MS))
    }

    val t = progress.value
    return if (t >= 1f) to.value else lerpPalette(from.value, to.value, t)
}

private fun lerpPalette(a: ThemePalette, b: ThemePalette, t: Float): ThemePalette {
    fun mix(x: Color, y: Color) = lerp(x, y, t)
    val sa = a.scheme
    val sb = b.scheme
    val ea = a.extras
    val eb = b.extras
    return ThemePalette(
        scheme = sb.copy(
            primary = mix(sa.primary, sb.primary),
            onPrimary = mix(sa.onPrimary, sb.onPrimary),
            primaryContainer = mix(sa.primaryContainer, sb.primaryContainer),
            onPrimaryContainer = mix(sa.onPrimaryContainer, sb.onPrimaryContainer),
            secondary = mix(sa.secondary, sb.secondary),
            onSecondary = mix(sa.onSecondary, sb.onSecondary),
            secondaryContainer = mix(sa.secondaryContainer, sb.secondaryContainer),
            onSecondaryContainer = mix(sa.onSecondaryContainer, sb.onSecondaryContainer),
            tertiary = mix(sa.tertiary, sb.tertiary),
            onTertiary = mix(sa.onTertiary, sb.onTertiary),
            tertiaryContainer = mix(sa.tertiaryContainer, sb.tertiaryContainer),
            onTertiaryContainer = mix(sa.onTertiaryContainer, sb.onTertiaryContainer),
            error = mix(sa.error, sb.error),
            onError = mix(sa.onError, sb.onError),
            errorContainer = mix(sa.errorContainer, sb.errorContainer),
            onErrorContainer = mix(sa.onErrorContainer, sb.onErrorContainer),
            background = mix(sa.background, sb.background),
            onBackground = mix(sa.onBackground, sb.onBackground),
            surface = mix(sa.surface, sb.surface),
            onSurface = mix(sa.onSurface, sb.onSurface),
            surfaceVariant = mix(sa.surfaceVariant, sb.surfaceVariant),
            onSurfaceVariant = mix(sa.onSurfaceVariant, sb.onSurfaceVariant),
            outline = mix(sa.outline, sb.outline),
            surfaceTint = mix(sa.surfaceTint, sb.surfaceTint)
        ),
        extras = DarmixColors(
            ink = mix(ea.ink, eb.ink),
            shadow = mix(ea.shadow, eb.shadow),
            accent = mix(ea.accent, eb.accent),
            onAccent = mix(ea.onAccent, eb.onAccent),
            accentContainer = mix(ea.accentContainer, eb.accentContainer),
            onAccentContainer = mix(ea.onAccentContainer, eb.onAccentContainer),
            available = mix(ea.available, eb.available),
            availableContainer = mix(ea.availableContainer, eb.availableContainer),
            onAvailableContainer = mix(ea.onAvailableContainer, eb.onAvailableContainer),
            cooldown = mix(ea.cooldown, eb.cooldown),
            cooldownContainer = mix(ea.cooldownContainer, eb.cooldownContainer),
            onCooldownContainer = mix(ea.onCooldownContainer, eb.onCooldownContainer),
            blocked = mix(ea.blocked, eb.blocked),
            blockedContainer = mix(ea.blockedContainer, eb.blockedContainer),
            onBlockedContainer = mix(ea.onBlockedContainer, eb.onBlockedContainer)
        )
    )
}