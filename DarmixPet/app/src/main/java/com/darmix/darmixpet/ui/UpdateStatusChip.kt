package com.darmix.darmixpet.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.darmix.darmixpet.ui.components.HatBadge
import com.darmix.darmixpet.ui.components.SquishButton
import com.darmix.darmixpet.ui.components.sticker
import com.darmix.darmixpet.ui.icons.DarmixIcons
import com.darmix.darmixpet.ui.theme.DarmixTheme
import com.darmix.darmixpet.update.UpdateInfo
import com.darmix.darmixpet.update.UpdateManager.CheckState
import kotlinx.coroutines.launch

private data class ChipSpec(val label: String, val icon: ImageVector, val fill: Color, val content: Color)

@Composable
fun UpdateStatusChip(
    offer: UpdateInfo?,
    checkState: CheckState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val colors = DarmixTheme.colors
    val hasUpdate = offer != null
    val checking = !hasUpdate && checkState == CheckState.CHECKING

    val spec = when {
        hasUpdate -> ChipSpec("Actualizar", DarmixIcons.Download, colors.gold, colors.onGold)
        checking -> ChipSpec("Buscando…", DarmixIcons.Sparkle, scheme.surface, scheme.onSurface)
        checkState == CheckState.UP_TO_DATE ->
            ChipSpec("Al día", DarmixIcons.Check, colors.availableContainer, colors.onAvailableContainer)
        else -> ChipSpec("Revisar", DarmixIcons.Sparkle, scheme.surface, scheme.onSurface)
    }
    val fill by animateColorAsState(spec.fill, tween(300), label = "chipFill")

    val transition = rememberInfiniteTransition(label = "updateChip")
    val pulse = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val spin = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "spin"
    )

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "chipPress"
    )

    Row(
        modifier = modifier
            .graphicsLayer {
                val s = pressScale * (if (hasUpdate) 1f + 0.06f * pulse.value else 1f)
                scaleX = s
                scaleY = s
            }
            .sticker(shape = CircleShape, fill = fill, depth = 3.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = !checking,
                role = Role.Button,
                onClickLabel = if (hasUpdate) "Ver la actualización" else "Buscar actualizaciones",
                onClick = onClick
            )
            .heightIn(min = 44.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            spec.icon, contentDescription = null, tint = spec.content,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer { rotationZ = if (checking) spin.value else 0f }
        )
        Spacer(Modifier.width(6.dp))
        Text(spec.label, style = MaterialTheme.typography.labelLarge, color = spec.content)
    }
}

@Composable
fun UpdateStatusDialog(
    state: CheckState,
    installedVersion: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shown = if (state == CheckState.IDLE) CheckState.CHECKING else state

    val enter = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    val handleDismiss = {
        coroutineScope.launch {
            enter.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium))
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        enter.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow))
    }

    Dialog(
        onDismissRequest = { handleDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val enter = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            enter.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow))
        }

        Column(
            modifier = Modifier
                .padding(start = 24.dp, end = 28.dp, top = 24.dp, bottom = 28.dp)
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    val s = 0.85f + 0.15f * enter.value
                    scaleX = s
                    scaleY = s
                    alpha = enter.value.coerceIn(0f, 1f)
                }
                .sticker(shape = RoundedCornerShape(30.dp), fill = scheme.surface, depth = 4.dp)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(
                targetState = shown,
                transitionSpec = {
                    (scaleIn(spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow)) + fadeIn()) togetherWith fadeOut()
                },
                label = "updateStatus"
            ) { s ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (s == CheckState.UP_TO_DATE) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(scheme.tertiary)
                                .border(2.dp, DarmixTheme.colors.ink, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                DarmixIcons.Check, null,
                                tint = scheme.onTertiary, modifier = Modifier.size(40.dp)
                            )
                        }
                    } else {
                        HatBadge(size = 72.dp)
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        when (s) {
                            CheckState.UP_TO_DATE -> "¡Estás al día!"
                            CheckState.FAILED -> "No pude comprobar"
                            else -> "Buscando actualizaciones…"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = scheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        when (s) {
                            CheckState.UP_TO_DATE -> "Tienes la última versión de DarmixPet (v$installedVersion)."
                            CheckState.FAILED -> "Revisa tu conexión a internet e inténtalo de nuevo."
                            else -> "Un momentito…"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            if (shown == CheckState.FAILED) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SquishButton(
                        text = "Cerrar",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        container = scheme.surface,
                        content = scheme.onSurface,
                        fillWidth = true
                    )
                    SquishButton(
                        text = "Reintentar",
                        onClick = onRetry,
                        modifier = Modifier.weight(1f),
                        fillWidth = true
                    )
                }
            } else {
                SquishButton(
                    text = if (shown == CheckState.UP_TO_DATE) "¡Genial!" else "Cerrar",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    container = if (shown == CheckState.UP_TO_DATE) scheme.primary else scheme.surface,
                    content = if (shown == CheckState.UP_TO_DATE) scheme.onPrimary else scheme.onSurface,
                    fillWidth = true
                )
            }
        }
    }
}
