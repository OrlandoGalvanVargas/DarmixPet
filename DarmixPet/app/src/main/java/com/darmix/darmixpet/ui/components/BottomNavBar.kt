package com.darmix.darmixpet.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.darmix.darmixpet.ui.AppScreen
import com.darmix.darmixpet.ui.theme.DarmixTheme


@Composable
fun BottomNavBar(current: AppScreen, onSelect: (AppScreen) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 20.dp, top = 8.dp, bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .sticker(shape = RoundedCornerShape(32.dp), depth = 4.dp)
                .padding(6.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppScreen.entries.forEach { screen ->
                NavItem(
                    screen = screen,
                    selected = screen == current,
                    onClick = { onSelect(screen) }
                )
            }
        }
    }
}

@Composable
private fun NavItem(screen: AppScreen, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val colors = DarmixTheme.colors

    val (bubble, onBubble) = when (screen) {
        AppScreen.APPS -> scheme.primaryContainer to scheme.onPrimaryContainer
        AppScreen.MASCOT -> scheme.secondaryContainer to scheme.onSecondaryContainer
        AppScreen.SETTINGS -> colors.goldContainer to colors.onGoldContainer
    }

    val fill by animateColorAsState(if (selected) bubble else Color.Transparent, tween(250), label = "navFill")
    val edge by animateColorAsState(if (selected) colors.ink else Color.Transparent, tween(250), label = "navEdge")
    val tint by animateColorAsState(if (selected) onBubble else scheme.onSurfaceVariant, tween(250), label = "navTint")
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.14f else 1f,
        animationSpec = spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium),
        label = "navScale"
    )

    Row(
        modifier = Modifier
            .animateContentSize(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium))
            .background(fill, CircleShape)
            .border(2.dp, edge, CircleShape)
            .selectable(
                selected = selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .heightIn(min = 48.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = screen.icon,
            contentDescription = if (selected) null else screen.label,
            tint = tint,
            modifier = Modifier
                .size(26.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
        )
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(tween(200)) + expandHorizontally(),
            exit = fadeOut(tween(100)) + shrinkHorizontally()
        ) {
            Text(
                text = screen.label,
                style = MaterialTheme.typography.labelLarge,
                color = onBubble,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
