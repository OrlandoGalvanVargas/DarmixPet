package com.darmix.darmixpet.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.onClick as semanticsOnClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.darmix.darmixpet.ui.theme.DarmixTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun SquishIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.primary,
    content: Color = MaterialTheme.colorScheme.onPrimary,
    enabled: Boolean = true,
    repeatWhileHeld: Boolean = false,
    size: Dp = 48.dp,
    depth: Dp = 3.dp
) {
    var pressed by remember { mutableStateOf(false) }
    val currentClick by rememberUpdatedState(onClick)
    val press by animateFloatAsState(
        targetValue = if (pressed && enabled) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "iconPress"
    )
    val ink = DarmixTheme.colors.ink
    val shadow = DarmixTheme.colors.shadow
    val face = if (enabled) container else MaterialTheme.colorScheme.surfaceVariant
    val tint = if (enabled) content else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .padding(end = depth, bottom = depth)
            .size(size)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                this.contentDescription = contentDescription
                if (enabled) semanticsOnClick { currentClick(); true } else disabled()
            }
            .pointerInput(enabled, repeatWhileHeld) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        pressed = true
                        var repeating = false
                        coroutineScope {
                            val job = if (repeatWhileHeld) launch {
                                delay(420)
                                repeating = true
                                while (true) {
                                    currentClick()
                                    delay(80)
                                }
                            } else null
                            val released = tryAwaitRelease()
                            job?.cancel()
                            if (released && !repeating) currentClick()
                        }
                        pressed = false
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val shift = press * depth.toPx()
                    translationX = shift
                    translationY = shift
                }
                .drawBehind {
                    val d = (depth.toPx() * (1f - press)).coerceAtLeast(0f)
                    translate(d, d) { drawCircle(shadow) }
                }
                .background(face, CircleShape)
                .border(2.dp, ink, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.46f))
        }
    }
}
