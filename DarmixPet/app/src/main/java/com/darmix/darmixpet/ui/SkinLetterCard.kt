package com.darmix.darmixpet.ui

import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.darmix.darmixpet.R
import com.darmix.darmixpet.mascota.SkinInfo
import com.darmix.darmixpet.ui.components.sticker
import com.darmix.darmixpet.ui.theme.AppPalettes
import com.darmix.darmixpet.ui.theme.DarmixTheme
import kotlinx.coroutines.delay

private val HandwrittenFamily = FontFamily(Font(R.font.kalam_bold))
private val STICKER_TILTS = floatArrayOf(-3f, 2f, -1.5f, 2.5f)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkinLetterCard(skin: SkinInfo, lore: SkinLore) {
    val scheme = MaterialTheme.colorScheme
    val isDark = DarmixTheme.isDark
    val ink = DarmixTheme.colors.ink
    val skinScheme = AppPalettes.forSkin(skin.id, isDark).scheme
    val accent = Color(skin.accentColor)
    val paper = accent.copy(alpha = if (isDark) 0.10f else 0.07f).compositeOver(scheme.surface)
    val ruleColor = (if (isDark) scheme.onSurface else ink).copy(alpha = 0.07f)

    val seal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        seal.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
    }

    Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .sticker(shape = MaterialTheme.shapes.large, fill = paper, depth = 3.dp)
                .drawBehind {
                    val step = 26.dp.toPx()
                    var y = 46.dp.toPx()
                    while (y < size.height - 8.dp.toPx()) {
                        drawLine(ruleColor, androidx.compose.ui.geometry.Offset(14.dp.toPx(), y),
                            androidx.compose.ui.geometry.Offset(size.width - 14.dp.toPx(), y), strokeWidth = 1.dp.toPx())
                        y += step
                    }
                }
                .padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 16.dp)
        ) {
            Text(
                "UNA NOTA DE ${lore.signature.uppercase()}",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
                color = scheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Text(
                lore.greeting,
                style = MaterialTheme.typography.titleMedium,
                color = skinScheme.primary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                lore.letter,
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                color = scheme.onSurface
            )

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Postmark(
                    origin = lore.origin,
                    element = lore.stampElement,
                    color = skinScheme.primary.copy(alpha = 0.6f)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "— ${lore.signature}",
                    style = TextStyle(fontFamily = HandwrittenFamily, fontSize = 26.sp),
                    color = scheme.onSurface,
                    modifier = Modifier.graphicsLayer { rotationZ = -4f }
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "P.D. ${lore.quote}",
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface
            )

            if (lore.quirks.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    lore.quirks.forEachIndexed { i, quirk ->
                        QuirkSticker(
                            text = quirk,
                            tilt = STICKER_TILTS[i % STICKER_TILTS.size],
                            index = i,
                            fill = if (i % 2 == 0) skinScheme.primaryContainer else scheme.surface,
                            content = if (i % 2 == 0) skinScheme.onPrimaryContainer else scheme.onSurface
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-16).dp, y = (-14).dp)
                .graphicsLayer {
                    val s = 1.6f - 0.6f * seal.value
                    scaleX = s
                    scaleY = s
                    rotationZ = (1f - seal.value) * -25f
                    alpha = seal.value.coerceIn(0f, 1f)
                }
                .size(48.dp)
                .sticker(shape = CircleShape, fill = skinScheme.primary, depth = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                elementIconFor(skin.id),
                contentDescription = null,
                tint = skinScheme.onPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun QuirkSticker(text: String, tilt: Float, index: Int, fill: Color, content: Color) {
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(180L + index * 90L)
        pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
    }
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = content,
        modifier = Modifier
            .graphicsLayer {
                rotationZ = tilt
                scaleX = pop.value
                scaleY = pop.value
                alpha = pop.value.coerceIn(0f, 1f)
            }
            .sticker(shape = RoundedCornerShape(10.dp), fill = fill, depth = 2.dp, borderWidth = 2.dp)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

@Composable
private fun Postmark(origin: String, element: String, color: Color) {
    val context = LocalContext.current
    val typeface = remember { ResourcesCompat.getFont(context, R.font.fredoka_medium) }
    Canvas(modifier = Modifier.size(68.dp).graphicsLayer { rotationZ = -8f }) {
        val c = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        val outer = size.minDimension / 2f - 2.dp.toPx()
        drawCircle(color, outer, c, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
        drawCircle(color, outer * 0.72f, c, style = androidx.compose.ui.graphics.drawscope.Stroke(1.2.dp.toPx()))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color.toArgb()
            this.typeface = typeface
            letterSpacing = 0.12f
        }
        val arcRadius = outer * 0.86f
        val arc = Path().apply {
            addArc(RectF(c.x - arcRadius, c.y - arcRadius, c.x + arcRadius, c.y + arcRadius), 180f, 180f)
        }
        paint.textSize = 7.2.sp.toPx()
        paint.textAlign = Paint.Align.LEFT
        val arcLength = (Math.PI * arcRadius).toFloat()
        val textWidth = paint.measureText(origin)
        val startOffset = ((arcLength - textWidth) / 2f).coerceAtLeast(0f)
        drawContext.canvas.nativeCanvas.drawTextOnPath(origin, arc, startOffset, paint.textSize * 0.35f, paint)

        paint.textSize = 8.5.sp.toPx()
        paint.textAlign = Paint.Align.CENTER
        drawContext.canvas.nativeCanvas.drawText(element, c.x, c.y + paint.textSize * 0.4f, paint)

        val wave = androidx.compose.ui.graphics.Path().apply {
            val y0 = c.y + outer * 0.42f
            moveTo(c.x - outer * 0.55f, y0)
            val w = outer * 1.1f / 4f
            for (k in 0 until 4) {
                relativeQuadraticBezierTo(w / 2f, if (k % 2 == 0) -3.dp.toPx() else 3.dp.toPx(), w, 0f)
            }
        }
        drawPath(wave, color, style = androidx.compose.ui.graphics.drawscope.Stroke(1.2.dp.toPx()))
    }
}