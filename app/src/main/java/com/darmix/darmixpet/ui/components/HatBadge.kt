package com.darmix.darmixpet.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.darmix.darmixpet.ui.icons.DarmixIcons


@Composable
fun HatBadge(modifier: Modifier = Modifier, size: Dp = 68.dp) {
    val transition = rememberInfiniteTransition(label = "hatWobble")
    val angle by transition.animateFloat(
        initialValue = -7f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hatAngle"
    )
    Box(
        modifier = modifier
            .size(size)
            .sticker(
                shape = CircleShape,
                fill = MaterialTheme.colorScheme.primaryContainer,
                depth = 4.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = DarmixIcons.Hat,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .size(size * 0.58f)
                .graphicsLayer {
                    rotationZ = angle
                    transformOrigin = TransformOrigin(0.5f, 0.9f)
                }
        )
    }
}
