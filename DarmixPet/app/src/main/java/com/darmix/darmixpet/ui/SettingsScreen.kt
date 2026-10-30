package com.darmix.darmixpet.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.darmix.darmixpet.update.UpdateManager
import com.darmix.darmixpet.ui.components.StatusBadge
import com.darmix.darmixpet.ui.components.sticker
import com.darmix.darmixpet.ui.icons.DarmixIcons

import com.darmix.darmixpet.ui.theme.DarmixTheme
import com.darmix.darmixpet.ui.theme.AppPalettes
import com.darmix.darmixpet.ui.theme.ThemePalette
import com.darmix.darmixpet.ui.theme.rememberSelectedSkinId
import com.darmix.darmixpet.ui.theme.ThemeMode
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.text.style.TextDecoration
import com.darmix.darmixpet.update.UpdateChecker

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    currentThemeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var showGuide by remember { mutableStateOf(false) }

    val updateOffer by UpdateManager.offer.collectAsState()

    val context = LocalContext.current
    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        }.getOrDefault("")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 24.dp, top = 16.dp, bottom = 20.dp)
        ) {
            Text(
                "Ajustes",
                style = MaterialTheme.typography.headlineMedium,
                color = scheme.onBackground
            )
            Text(
                "Personaliza a DarmixPet a tu gusto",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
        }

        UpdateBanner(
            latestVersion = updateOffer?.latestVersion,
            onClick = { UpdateManager.requestDialog() },
            modifier = Modifier.padding(start = 20.dp, end = 24.dp, bottom = 24.dp)
        )

        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp)) {
            Text("Tu mascota", style = MaterialTheme.typography.titleMedium, color = scheme.onBackground)
        }
        PetPowerCard(modifier = Modifier.padding(start = 20.dp, end = 24.dp))
        Spacer(Modifier.height(14.dp))
        VoiceCard(modifier = Modifier.padding(start = 20.dp, end = 24.dp))
        MessageAlertsCard(modifier = Modifier.padding(start = 20.dp, end = 24.dp, top = 14.dp))

        Spacer(Modifier.height(28.dp))

        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp)) {
            Text("Apariencia", style = MaterialTheme.typography.titleMedium, color = scheme.onBackground)
            Text(
                when (currentThemeMode) {
                    ThemeMode.LIGHT -> "Siempre en modo claro"
                    ThemeMode.DARK -> "Siempre en modo oscuro"
                    ThemeMode.SYSTEM -> "Sigue el modo de tu teléfono"
                },
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ThemeOptionCard(
                label = "Claro", icon = DarmixIcons.Sun, mode = ThemeMode.LIGHT,
                selected = currentThemeMode == ThemeMode.LIGHT,
                onSelect = onThemeModeChange, modifier = Modifier.weight(1f)
            )
            ThemeOptionCard(
                label = "Oscuro", icon = DarmixIcons.Moon, mode = ThemeMode.DARK,
                selected = currentThemeMode == ThemeMode.DARK,
                onSelect = onThemeModeChange, modifier = Modifier.weight(1f)
            )
            ThemeOptionCard(
                label = "Sistema", icon = DarmixIcons.HalfCircle, mode = ThemeMode.SYSTEM,
                selected = currentThemeMode == ThemeMode.SYSTEM,
                onSelect = onThemeModeChange, modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(28.dp))

        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp)) {
            Text("Ayuda", style = MaterialTheme.typography.titleMedium, color = scheme.onBackground)
        }
        GuideEntryCard(
            onClick = { showGuide = true },
            modifier = Modifier.padding(start = 20.dp, end = 24.dp)
        )

        Spacer(Modifier.height(14.dp))
        OverlayNoticeHelpCard(modifier = Modifier.padding(start = 20.dp, end = 24.dp))

        AboutFooter(
            version = version,
            modifier = Modifier.padding(top = 28.dp, start = 20.dp, end = 24.dp)
        )
    }

    if (showGuide) GuideDialog(onDismiss = { showGuide = false })

}


@Composable
private fun ThemeOptionCard(
    label: String,
    icon: ImageVector,
    mode: ThemeMode,
    selected: Boolean,
    onSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val colors = DarmixTheme.colors
    val spec = spring<Dp>(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium)
    val depth by animateDpAsState(if (selected) 0.dp else 4.dp, spec, label = "themeDepth")
    val shift by animateDpAsState(if (selected) 4.dp else 0.dp, spec, label = "themeShift")
    val fill by animateColorAsState(
        targetValue = if (selected) scheme.primaryContainer else scheme.surface,
        animationSpec = tween(250),
        label = "themeFill"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) scheme.onPrimaryContainer else scheme.onSurface,
        animationSpec = tween(250),
        label = "themeText"
    )
    val previewShape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .padding(end = 4.dp, bottom = 4.dp)
            .selectable(
                selected = selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.RadioButton,
                onClick = { onSelect(mode) }
            )
    ) {
        Column(
            modifier = Modifier
                .offset(x = shift, y = shift)
                .sticker(shape = RoundedCornerShape(22.dp), fill = fill, depth = depth)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(previewShape)
                    .border(1.5.dp, colors.ink, previewShape)
            ) {
                ThemePreview(mode = mode, modifier = Modifier.fillMaxSize())
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(5.dp)) {
                    this@Column.AnimatedVisibility(
                        visible = selected,
                        enter = scaleIn(spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium)),
                        exit = scaleOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(scheme.primary)
                                .border(2.dp, colors.ink, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                DarmixIcons.Check, contentDescription = null,
                                tint = scheme.onPrimary, modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text(label, style = MaterialTheme.typography.labelLarge, color = textColor)
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}

private class MiniPalette(
    val bg: Color,
    val surface: Color,
    val primary: Color,
    val ink: Color,
    val text: Color,
    val ok: Color
)

private fun miniOf(palette: ThemePalette) = MiniPalette(
    bg = palette.scheme.background,
    surface = palette.scheme.surface,
    primary = palette.scheme.primary,
    ink = palette.extras.ink,
    text = palette.scheme.onSurface,
    ok = palette.scheme.primary
)

@Composable
private fun ThemePreview(mode: ThemeMode, modifier: Modifier = Modifier) {
    val skinId by rememberSelectedSkinId()
    val light = remember(skinId) { miniOf(AppPalettes.forSkin(skinId, dark = false)) }
    val dark = remember(skinId) { miniOf(AppPalettes.forSkin(skinId, dark = true)) }
    Canvas(modifier = modifier) {
        when (mode) {
            ThemeMode.LIGHT -> drawMiniScreen(light)
            ThemeMode.DARK -> drawMiniScreen(dark)
            ThemeMode.SYSTEM -> {
                drawMiniScreen(light)
                clipRect(left = size.width / 2f) { drawMiniScreen(dark) }
            }
        }
    }
}

private fun DrawScope.drawMiniScreen(p: MiniPalette) {
    val w = size.width
    val h = size.height
    drawRect(p.bg)

    drawRoundRect(
        color = p.text.copy(alpha = 0.75f),
        topLeft = Offset(w * 0.10f, h * 0.09f),
        size = Size(w * 0.38f, h * 0.07f),
        cornerRadius = CornerRadius(h * 0.035f)
    )

    for (i in 0..1) {
        val left = w * 0.08f
        val top = h * (0.27f + i * 0.30f)
        val cw = w * 0.84f - 2.dp.toPx()
        val ch = h * 0.24f
        val corner = CornerRadius(6.dp.toPx())

        drawRoundRect(p.ink, Offset(left + 2.dp.toPx(), top + 2.dp.toPx()), Size(cw, ch), corner)
        drawRoundRect(p.surface, Offset(left, top), Size(cw, ch), corner)
        drawRoundRect(p.ink, Offset(left, top), Size(cw, ch), corner, style = Stroke(width = 1.2.dp.toPx()))

        drawCircle(p.primary, radius = ch * 0.26f, center = Offset(left + ch * 0.55f, top + ch / 2f))
        drawRoundRect(
            color = p.text.copy(alpha = 0.6f),
            topLeft = Offset(left + ch * 1.05f, top + ch * 0.40f),
            size = Size(cw * 0.32f, ch * 0.18f),
            cornerRadius = CornerRadius(ch * 0.09f)
        )
        drawRoundRect(
            color = if (i == 0) p.ok else p.ink.copy(alpha = 0.25f),
            topLeft = Offset(left + cw * 0.72f, top + ch * 0.30f),
            size = Size(cw * 0.20f, ch * 0.40f),
            cornerRadius = CornerRadius(ch * 0.2f)
        )
    }
}


@Composable
private fun GuideEntryCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val colors = DarmixTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "guidePress"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .sticker(shape = MaterialTheme.shapes.large, fill = scheme.primaryContainer, depth = 4.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClickLabel = "Abrir la guía",
                onClick = onClick
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(scheme.surface)
                .border(2.dp, colors.ink, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                DarmixIcons.Paw, contentDescription = null,
                tint = scheme.onPrimaryContainer, modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Cómo jugar con tu mascota",
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onPrimaryContainer
            )
            Text(
                "Toques, mantener presionado, arrastrar y más",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onPrimaryContainer.copy(alpha = 0.85f)
            )
        }
        Icon(
            DarmixIcons.ChevronRight, contentDescription = null,
            tint = scheme.onPrimaryContainer, modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun UpdateBanner(
    latestVersion: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    AnimatedVisibility(
        visible = latestVersion != null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .sticker(shape = MaterialTheme.shapes.large, fill = scheme.primaryContainer, depth = 4.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClickLabel = "Ver la nueva versión"
                ) { onClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .sticker(shape = CircleShape, fill = scheme.primary, depth = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(DarmixIcons.Download, null, tint = scheme.onPrimary, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Nueva versión v${latestVersion.orEmpty()}",
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onPrimaryContainer
                )
                Text(
                    "Toca para ver las novedades",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
            }
            Spacer(Modifier.width(8.dp))
            StatusBadge("Actualizar", DarmixIcons.Sparkle, scheme.primary, scheme.onPrimary)
        }
    }
}

@Composable
private fun AboutFooter(version: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (version.isNotBlank()) "DarmixPet v$version · Hecho con cariño" else "DarmixPet · Hecho con cariño",
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant
            )
            Spacer(Modifier.width(4.dp))
            Icon(DarmixIcons.Heart, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
        }
        Text(
            "darmixista.dev",
            style = MaterialTheme.typography.labelLarge,
            color = scheme.onSurface,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .clickable(
                    role = Role.Button,
                    onClickLabel = "Abrir la página oficial de DarmixPet"
                ) { UpdateChecker.openUrl(context, UpdateChecker.OFFICIAL_PAGE_URL) }
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}