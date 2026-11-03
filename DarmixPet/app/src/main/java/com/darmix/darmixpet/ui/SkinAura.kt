package com.darmix.darmixpet.ui

import android.graphics.BlurMaskFilter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import com.darmix.darmixpet.ui.icons.DarmixIcons
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class AuraColors(
    val outer: Color,
    val glow: Color,
    val core: Color
)

fun auraColorsFor(skinId: String): AuraColors = when (skinId) {
    "skin2" -> AuraColors(
        outer = Color(0xFF4ADE80),
        glow = Color(0xFF86EFAC),
        core = Color(0xFFF0FFF4)
    )
    "skin3" -> AuraColors(
        outer = Color(0xFFF5B83D),
        glow = Color(0xFFFFD978),
        core = Color(0xFFFFF7DE)
    )
    else -> AuraColors(
        outer = Color(0xFF22D3EE),
        glow = Color(0xFF67E8F9),
        core = Color(0xFFF0FDFF)
    )
}

fun elementIconFor(skinId: String): ImageVector = when (skinId) {
    "skin2" -> DarmixIcons.Sprout
    "skin3" -> DarmixIcons.Shield
    else -> DarmixIcons.Moon
}

private class Bolt(
    val x: Float,
    val y: Float,
    val angleDeg: Float,
    val length: Float,
    val curve: Float,
    val phase: Float
)

private val BOLTS = listOf(
    Bolt(-38f, -40f, -62f, 46f, 0.25f, 0.00f),
    Bolt(34f, -50f, -115f, 38f, -0.30f, 0.41f),
    Bolt(-46f, 22f, -80f, 38f, 0.20f, 0.17f),
    Bolt(46f, 8f, -100f, 44f, -0.20f, 0.73f),
    Bolt(-8f, 58f, -15f, 50f, 0.35f, 0.29f),
    Bolt(18f, -14f, -70f, 30f, 0.20f, 0.58f)
)

private const val BOLT_SEGMENTS = 7
private val BOLT_LIFT_DP = floatArrayOf(0f, 10f, 18f)
private val BOLT_STRETCH = floatArrayOf(1f, 1f, 1.3f)

private fun hash(n: Int): Float {
    var x = n
    x = (x shl 13) xor x
    val v = (x * (x * x * 15731 + 789221) + 1376312589) and 0x7fffffff
    return 1f - v / 1073741824f
}

private fun boltVisible(u: Float): Boolean = u < 0.10f || (u >= 0.16f && u < 0.30f)

fun DrawScope.drawLightning(center: Offset, t: Float, colors: AuraColors, visibility: Float) {
    val v = visibility.coerceIn(0f, 1f)
    if (v <= 0.01f) return
    val dp = density
    val glowPaint = Paint().apply {
        isAntiAlias = true
        style = androidx.compose.ui.graphics.PaintingStyle.Stroke
        strokeJoin = androidx.compose.ui.graphics.StrokeJoin.Miter
        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        asFrameworkPaint().maskFilter = BlurMaskFilter(3.5f * dp, BlurMaskFilter.Blur.NORMAL)
    }
    BOLTS.forEachIndexed { index, bolt ->
        val local = (t + bolt.phase) % 1f
        val step = (local * 3f).toInt().coerceIn(0, 2)
        val u = local * 3f - step
        if (!boltVisible(u)) return@forEachIndexed

        val length = bolt.length * dp * BOLT_STRETCH[step]
        val start = Offset(center.x + bolt.x * dp, center.y + (bolt.y - BOLT_LIFT_DP[step]) * dp)
        val angle = bolt.angleDeg * PI.toFloat() / 180f
        val dx = cos(angle)
        val dy = sin(angle)
        val px = -dy
        val py = dx

        val points = (0..BOLT_SEGMENTS).map { k ->
            val f = k.toFloat() / BOLT_SEGMENTS
            val jitter = if (k == 0 || k == BOLT_SEGMENTS) 0f else hash(index * 97 + step * 13 + k) * 0.16f * length
            val bend = bolt.curve * length * sin(PI.toFloat() * f)
            Offset(start.x + dx * length * f + px * (jitter + bend), start.y + dy * length * f + py * (jitter + bend))
        }
        val main = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (k in 1 until points.size) lineTo(points[k].x, points[k].y)
        }
        val branchStart = points[3]
        val branchAngle = angle + if (hash(index * 7 + step) >= 0f) 0.8f else -0.8f
        val branchLength = length * 0.32f
        val branch = Path().apply {
            moveTo(branchStart.x, branchStart.y)
            lineTo(
                branchStart.x + cos(branchAngle) * branchLength * 0.5f + px * branchLength * 0.12f,
                branchStart.y + sin(branchAngle) * branchLength * 0.5f + py * branchLength * 0.12f
            )
            lineTo(branchStart.x + cos(branchAngle) * branchLength, branchStart.y + sin(branchAngle) * branchLength)
        }

        glowPaint.color = colors.outer.copy(alpha = 0.9f * v)
        glowPaint.strokeWidth = 7f * dp
        drawIntoCanvas { it.drawPath(main, glowPaint) }
        glowPaint.strokeWidth = 5f * dp
        drawIntoCanvas { it.drawPath(branch, glowPaint) }

        val core = colors.core.copy(alpha = v)
        drawPath(main, core, style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 2.4f * dp,
            join = androidx.compose.ui.graphics.StrokeJoin.Miter,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        ))
        drawPath(branch, core, style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 1.8f * dp,
            join = androidx.compose.ui.graphics.StrokeJoin.Miter,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        ))
    }
}