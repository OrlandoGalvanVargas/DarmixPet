package com.darmix.darmixpet.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.darmix.darmixpet.ui.theme.DarmixTheme
import kotlinx.coroutines.delay
import kotlin.random.Random

private object GhostShape {
    const val VIEW_X = 20f
    const val VIEW_Y = 13f
    const val VIEW_W = 68f
    const val VIEW_H = 76f

    val body: Path = PathParser().parsePathString(
        "M33,66 C33,57.94 42.6,53 54,53 C65.4,53 75,57.94 75,66 L75,78 " +
                "C75,81.87 71.87,85 68,85 C64.13,85 61,81.87 61,78 " +
                "C61,81.87 57.87,85 54,85 C50.13,85 47,81.87 47,78 " +
                "C47,81.87 43.87,85 40,85 C36.13,85 33,81.87 33,78 Z"
    ).toPath()

    val cone: Path = PathParser().parsePathString(
        "M34,45 C38,34 48,24 59,17.5 C62,27 70,36 74,45 C66,48 42,48 34,45 Z"
    ).toPath()

    val star: Path = PathParser().parsePathString(
        "M55.5,26.5 Q55.5,31.5 60.5,31.5 Q55.5,31.5 55.5,36.5 " +
                "Q55.5,31.5 50.5,31.5 Q55.5,31.5 55.5,26.5 Z"
    ).toPath()

    val smile: Path = Path().apply {
        moveTo(49.5f, 71f)
        quadraticTo(54f, 75.4f, 58.5f, 71f)
    }

    val winkArc: Path = Path().apply {
        moveTo(58.8f, 65f)
        quadraticTo(62.5f, 60.2f, 66.2f, 65f)
    }
}

@Composable
fun HatBadge(
    modifier: Modifier = Modifier,
    size: Dp = 68.dp,
    fill: Color = MaterialTheme.colorScheme.primaryContainer,
    tint: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    val wink = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(Random.nextLong(600L, 1800L))
        while (true) {
            wink.animateTo(1f, tween(110, easing = FastOutSlowInEasing))
            delay(260L)
            wink.animateTo(0f, tween(150, easing = FastOutSlowInEasing))
            delay(3200L)
        }
    }

    val solid = DarmixTheme.isDark

    Box(
        modifier = modifier
            .size(size)
            .sticker(
                shape = CircleShape,
                fill = fill,
                depth = 4.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.66f)) {
            drawGhost(tint = tint, cutout = fill, wink = wink.value, solid = solid)
        }
    }
}

private fun DrawScope.drawGhost(tint: Color, cutout: Color, wink: Float, solid: Boolean) {
    val s = minOf(size.width / GhostShape.VIEW_W, size.height / GhostShape.VIEW_H)
    val dx = (size.width - GhostShape.VIEW_W * s) / 2f
    val dy = (size.height - GhostShape.VIEW_H * s) / 2f

    val outline = Stroke(width = 4.2f, join = StrokeJoin.Round, cap = StrokeCap.Round)
    val line = Stroke(width = 3.6f, join = StrokeJoin.Round, cap = StrokeCap.Round)
    val wash = if (solid) tint.copy(alpha = 0.9f) else tint.copy(alpha = 0.28f)
    val face = if (solid) cutout else tint
    val hatFill = tint.copy(alpha = 0.55f)

    translate(dx, dy) {
        scale(s, s, pivot = Offset.Zero) {
            translate(-GhostShape.VIEW_X, -GhostShape.VIEW_Y) {
                drawPath(GhostShape.body, wash)
                drawPath(GhostShape.body, tint, style = outline)

                drawOval(face, topLeft = Offset(42.3f, 59.8f), size = Size(6.4f, 8.4f))

                val openHeight = 8.4f * (1f - wink)
                if (openHeight > 0.4f) {
                    drawOval(
                        face,
                        topLeft = Offset(59.3f, 64f - openHeight / 2f),
                        size = Size(6.4f, openHeight)
                    )
                }
                if (wink > 0.5f) {
                    drawPath(
                        GhostShape.winkArc,
                        face.copy(alpha = ((wink - 0.5f) * 2f).coerceIn(0f, 1f)),
                        style = line
                    )
                }

                drawPath(GhostShape.smile, face, style = line)

                drawOval(hatFill, topLeft = Offset(28f, 39.4f), size = Size(52f, 11.2f))
                drawOval(tint, topLeft = Offset(28f, 39.4f), size = Size(52f, 11.2f), style = outline)

                drawPath(GhostShape.cone, hatFill)
                drawPath(GhostShape.cone, tint, style = outline)

                drawPath(GhostShape.star, cutout)
            }
        }
    }
}