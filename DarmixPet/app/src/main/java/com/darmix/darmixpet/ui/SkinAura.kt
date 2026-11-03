package com.darmix.darmixpet.ui

import android.graphics.BlurMaskFilter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import com.darmix.darmixpet.ui.icons.DarmixIcons
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class AuraColors(
    val outer: Color,
    val glow: Color,
    val core: Color,
    val powerOuter: Color,
    val powerGlow: Color
)

fun auraColorsFor(skinId: String): AuraColors = when (skinId) {
    "skin2" -> AuraColors(
        outer = Color(0xFF4ADE80),
        glow = Color(0xFF86EFAC),
        core = Color(0xFFF0FFF4),
        powerOuter = Color(0xFF16A34A),
        powerGlow = Color(0xFF4ADE80)
    )
    "skin3" -> AuraColors(
        outer = Color(0xFFF5B83D),
        glow = Color(0xFFFFD978),
        core = Color(0xFFFFF7DE),
        powerOuter = Color(0xFFE8890C),
        powerGlow = Color(0xFFFFC233)
    )
    else -> AuraColors(
        outer = Color(0xFF22D3EE),
        glow = Color(0xFF67E8F9),
        core = Color(0xFFF0FDFF),
        powerOuter = Color(0xFF0284C7),
        powerGlow = Color(0xFF22D3EE)
    )
}

fun elementIconFor(skinId: String): ImageVector = when (skinId) {
    "skin2" -> DarmixIcons.Sprout
    "skin3" -> DarmixIcons.Shield
    else -> DarmixIcons.Moon
}

private const val TONGUES = 9
private const val ARC_START = 0.62f
private val ARC_END = -PI.toFloat() - 0.62f
private const val VALLEY = 0.84f
private val TWO_PI = 2f * PI.toFloat()

private val TONGUE_SPEED = intArrayOf(2, 3, 1, 2, 3, 2, 1, 3, 2)
private val TONGUE_PHASE = floatArrayOf(0.00f, 0.37f, 0.71f, 0.15f, 0.52f, 0.88f, 0.26f, 0.63f, 0.05f)

private class AuraLayer(
    val scaleX: Float,
    val scaleY: Float,
    val lift: Float,
    val flare: Float,
    val blurDp: Float,
    val phaseShift: Float
)

private val OUTER = AuraLayer(1.14f, 1.10f, 0.40f, 0.12f, 10f, 0.33f)
private val MAIN = AuraLayer(1.00f, 1.00f, 0.34f, 0.10f, 2f, 0.00f)
private val CORE = AuraLayer(0.58f, 0.66f, 0.18f, 0.06f, 7f, 0.61f)

fun DrawScope.drawEnergyAura(
    center: Offset,
    radiusX: Float,
    radiusY: Float,
    t: Float,
    breath: Float,
    colors: AuraColors,
    power: Float = 0f
) {
    val p = power.coerceIn(0f, 1f)
    val pulse = (1f + 0.035f * breath) * (1f + 0.30f * p)
    val rx = radiusX * pulse
    val ry = radiusY * pulse
    val color = lerp(colors.outer, colors.powerOuter, p)
    val glow = lerp(colors.glow, colors.powerGlow, p)
    val core = lerp(colors.core, Color.White, p)
    val boost = 1f + 0.6f * p

    drawLayer(OUTER, center, rx, ry, t, density, boost) { paint, _ ->
        paint.color = color.copy(alpha = 0.40f + 0.35f * p)
    }
    drawLayer(MAIN, center, rx, ry, t, density, boost) { paint, layerRy ->
        paint.shader = RadialGradientShader(
            center = Offset(center.x, center.y + layerRy * 0.18f),
            radius = layerRy * 1.18f,
            colors = listOf(
                core.copy(alpha = 0.95f),
                core.copy(alpha = 0.80f),
                glow.copy(alpha = 0.92f),
                color.copy(alpha = 0.85f),
                color.copy(alpha = 0f)
            ),
            colorStops = listOf(0f, 0.38f, 0.66f, 0.84f, 1f),
            tileMode = androidx.compose.ui.graphics.TileMode.Clamp
        )
    }
    drawLayer(CORE, center, rx, ry, t, density, boost) { paint, _ ->
        paint.color = core.copy(alpha = 0.55f + 0.30f * p)
    }
}

private inline fun DrawScope.drawLayer(
    layer: AuraLayer,
    center: Offset,
    rx: Float,
    ry: Float,
    t: Float,
    density: Float,
    boost: Float,
    style: (Paint, Float) -> Unit
) {
    val layerRx = rx * layer.scaleX
    val layerRy = ry * layer.scaleY
    val path = auraPath(
        center, layerRx, layerRy, (t + layer.phaseShift) % 1f,
        layer.lift * boost, layer.flare * boost
    )
    val paint = Paint().apply { isAntiAlias = true }
    style(paint, layerRy)
    if (layer.blurDp > 0f) {
        paint.asFrameworkPaint().maskFilter = BlurMaskFilter(layer.blurDp * density, BlurMaskFilter.Blur.NORMAL)
    }
    drawIntoCanvas { it.drawPath(path, paint) }
}

private fun flicker(t: Float, i: Int): Float {
    val a = (t * TONGUE_SPEED[i] + TONGUE_PHASE[i]) * TWO_PI
    val v = 0.65f * sin(a) + 0.35f * sin(2f * a + i)
    return (0.5f + 0.5f * v).coerceIn(0f, 1f)
}

private fun auraPath(
    c: Offset,
    rx: Float,
    ry: Float,
    t: Float,
    lift: Float,
    flare: Float
): Path {
    fun phi(u: Float) = ARC_START + (ARC_END - ARC_START) * u
    fun point(angle: Float, k: Float) = Offset(c.x + rx * k * cos(angle), c.y + ry * k * sin(angle))

    val bottom = Offset(c.x, c.y + ry * 0.62f)
    val step = 1f / TONGUES
    return Path().apply {
        moveTo(bottom.x, bottom.y)
        val first = point(phi(0f), VALLEY)
        quadraticBezierTo(c.x + rx * 0.45f, c.y + ry * 0.70f, first.x, first.y)
        for (i in 0 until TONGUES) {
            val f = flicker(t, i)
            val um = (i + 0.5f) * step
            val angle = phi(um)
            val tipBase = point(angle, 1f + flare * f)
            val side = kotlin.math.abs(cos(angle))
            val up = ry * lift * (0.45f + 0.55f * f) * (0.7f + 0.3f * side)
            val outward = if (cos(angle) >= 0f) rx * 0.06f * side else -rx * 0.06f * side
            val tip = Offset(tipBase.x + outward, tipBase.y - up)
            val leadBase = point(phi(um - step * 0.5f), VALLEY + 0.06f)
            val valley = point(phi((i + 1) * step), VALLEY + 0.04f * flicker(t, (i + 3) % TONGUES))
            val midX = (tip.x + valley.x) / 2f
            val trailX = midX + (c.x - midX) * 0.04f
            val trailY = (tip.y + valley.y) / 2f + up * 0.05f
            quadraticBezierTo(leadBase.x, leadBase.y - up * 0.35f, tip.x, tip.y)
            quadraticBezierTo(trailX, trailY, valley.x, valley.y)
        }
        quadraticBezierTo(c.x - rx * 0.45f, c.y + ry * 0.70f, bottom.x, bottom.y)
        close()
    }
}