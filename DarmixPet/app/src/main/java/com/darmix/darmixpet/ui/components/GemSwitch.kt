package com.darmix.darmixpet.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.darmix.darmixpet.ui.icons.DarmixIcons
import com.darmix.darmixpet.ui.theme.DarmixTheme


@Composable
fun GemSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val scheme = MaterialTheme.colorScheme
    val colors = DarmixTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val travel = 46.dp
    val thumbWidthTarget = if (pressed) 30.dp else 22.dp
    val bouncy = spring<androidx.compose.ui.unit.Dp>(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)

    val thumbWidth by animateDpAsState(thumbWidthTarget, bouncy, label = "gemWidth")
    val thumbX by animateDpAsState(
        targetValue = if (checked) travel - thumbWidthTarget else 0.dp,
        animationSpec = bouncy,
        label = "gemX"
    )
    val track by animateColorAsState(
        targetValue = if (checked) scheme.tertiary else scheme.surfaceVariant,
        animationSpec = tween(250),
        label = "gemTrack"
    )
    val thumb by animateColorAsState(
        targetValue = if (checked) colors.gold else scheme.surface,
        animationSpec = tween(250),
        label = "gemThumb"
    )

    Box(
        modifier = modifier
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
            .minimumInteractiveComponentSize()
            .toggleable(
                value = checked,
                enabled = enabled && onCheckedChange != null,
                role = Role.Switch,
                interactionSource = interaction,
                indication = null,
                onValueChange = { onCheckedChange?.invoke(it) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 32.dp)
                .sticker(shape = CircleShape, fill = track, depth = 2.dp)
                .padding(5.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbX)
                    .size(width = thumbWidth, height = 22.dp)
                    .clip(CircleShape)
                    .background(thumb)
                    .border(2.dp, colors.ink, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = checked,
                    enter = scaleIn(spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium)),
                    exit = scaleOut()
                ) {
                    Icon(
                        DarmixIcons.Check, contentDescription = null,
                        tint = colors.onGold, modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
