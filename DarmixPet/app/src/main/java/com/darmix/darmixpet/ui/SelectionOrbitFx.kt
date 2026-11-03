package com.darmix.darmixpet.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

enum class OrbitKind { BUBBLE, LEAF, ROCK }

fun orbitKindFor(skinId: String): OrbitKind = when (skinId) {
    "skin2" -> OrbitKind.LEAF
    "skin3" -> OrbitKind.ROCK
    else -> OrbitKind.BUBBLE
}

const val ORBIT_FX_TOTAL_MS = 1100f

private const val ORBIT_COUNT = 14
private const val ORBIT_STAGGER_MS = 30f
private const val ORBIT_LIFE_MS = 650f
private const val ORBIT_RADIUS_DP = 78f
private const val ORBIT_RISE_DP = 215f
private const val ORBIT_GROUND_DP = 80f
private const val ORBIT_TURNS = 1.6f

private val OrbitSeeds = FloatArray(ORBIT_COUNT) { i -> ((sin(i * 12.9898f) * 43758.547f) % 1f + 1f) % 1f }

private fun progress(t: Float, start: Float, length: Float): Float =
    ((t - start) / length).coerceIn(0f, 1f)

private fun easeOut(v: Float): Float = 1f - (1f - v).pow(3)

fun orbitGlow(local: Float): Float {
    if (local < 0f) return 0f
    val appear = easeOut(progress(local, 0f, 200f))
    val vanish = 1f - progress(local, ORBIT_FX_TOTAL_MS - 350f, 350f)
    return (appear * vanish).coerceIn(0f, 1f)
}

fun DrawScope.drawSelectionOrbit(
    local: Float,
    kind: OrbitKind,
    center: Offset,
    front: Boolean,
    colors: AuraColors,
    accent: Color,
    primary: Color,
    ink: Color
) {
    if (local < 0f || local > ORBIT_FX_TOTAL_MS) return
    val dp = density
    val ground = center.y + ORBIT_GROUND_DP * dp
    for (i in 0 until ORBIT_COUNT) {
        val p = progress(local, i * ORBIT_STAGGER_MS, ORBIT_LIFE_MS)
        if (p <= 0f || p >= 1f) continue
        val angle = OrbitSeeds[i] * 6f + p * PI.toFloat() * 2f * ORBIT_TURNS
        val depth = sin(angle)
        if ((depth >= 0f) != front) continue

        val radius = ORBIT_RADIUS_DP * dp * (1f - p * 0.4f)
        val pos = Offset(center.x + radius * cos(angle), ground - p * ORBIT_RISE_DP * dp)
        val alpha = (1f - p) * (if (front) 1f else 0.8f)
        val depthScale = 0.85f + 0.15f * depth
        val variety = 0.8f + 0.4f * OrbitSeeds[(i + 5) % ORBIT_COUNT]

        when (kind) {
            OrbitKind.BUBBLE -> drawBubble(pos, 13f * dp * variety * depthScale, colors, ink, alpha)
            OrbitKind.LEAF -> drawSplashLeaf(
                center = pos,
                rotation = Math.toDegrees(angle.toDouble()).toFloat(),
                width = 26f * dp * variety * depthScale,
                height = 11f * dp * variety * depthScale,
                color = (if (i % 2 == 0) accent else primary).copy(alpha = alpha)
            )
            OrbitKind.ROCK -> drawRock(
                pos,
                10f * dp * variety * depthScale,
                local * 0.5f * (if (i % 2 == 0) 1f else -1f) + OrbitSeeds[i] * 360f,
                i,
                ink,
                alpha
            )
        }
    }
}

private fun DrawScope.drawSplashLeaf(center: Offset, rotation: Float, width: Float, height: Float, color: Color) {
    withTransform({ rotate(rotation, pivot = center) }) {
        drawOval(
            color = color,
            topLeft = Offset(center.x - width / 2f, center.y - height / 2f),
            size = Size(width, height)
        )
    }
}

private val BUBBLE_RIM = Color(0xFF1D6FD8)

private fun DrawScope.drawBubble(c: Offset, r: Float, colors: AuraColors, ink: Color, alpha: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to colors.glow.copy(alpha = 0.18f * alpha),
                0.55f to colors.glow.copy(alpha = 0.32f * alpha),
                0.85f to colors.outer.copy(alpha = 0.70f * alpha),
                1f to BUBBLE_RIM.copy(alpha = 0.85f * alpha)
            ),
            center = c,
            radius = r
        ),
        radius = r,
        center = c
    )
    drawCircle(BUBBLE_RIM.copy(alpha = 0.95f * alpha), r * 0.95f, c, style = Stroke(r * 0.10f))
    drawCircle(ink.copy(alpha = 0.45f * alpha), r, c, style = Stroke(maxOf(1f, r * 0.05f)))
    val highlight = r * 0.72f
    drawArc(
        color = Color.White.copy(alpha = 0.92f * alpha),
        startAngle = 200f,
        sweepAngle = 55f,
        useCenter = false,
        topLeft = Offset(c.x - highlight, c.y - highlight),
        size = Size(highlight * 2f, highlight * 2f),
        style = Stroke(r * 0.15f, cap = StrokeCap.Round)
    )
    val rim = r * 0.9f
    drawArc(
        color = Color.White.copy(alpha = 0.45f * alpha),
        startAngle = 20f,
        sweepAngle = 60f,
        useCenter = false,
        topLeft = Offset(c.x - rim, c.y - rim),
        size = Size(rim * 2f, rim * 2f),
        style = Stroke(r * 0.05f, cap = StrokeCap.Round)
    )
    drawCircle(Color.White.copy(alpha = 0.75f * alpha), r * 0.07f, Offset(c.x + r * 0.39f, c.y + r * 0.41f))
}

private val ROCK_BASE = Color(0xFF9C6B47)
private val ROCK_LIGHT = Color(0xFFD8B67E)
private val ROCK_DARK = Color(0xFF5E4028)

private val ROCK_SHAPE = floatArrayOf(1.0f, 0.82f, 0.95f, 0.74f, 0.92f, 0.80f, 0.97f)

private fun DrawScope.drawRock(c: Offset, r: Float, rotation: Float, seed: Int, ink: Color, alpha: Float) {
    val points = ROCK_SHAPE.mapIndexed { i, k ->
        val jitter = 0.9f + 0.2f * (((seed * 7 + i * 3) % 5) / 5f)
        val a = i.toFloat() / ROCK_SHAPE.size * 2f * PI.toFloat()
        Offset(c.x + r * k * jitter * cos(a), c.y + r * k * jitter * sin(a))
    }
    fun polygon(pts: List<Offset>) = Path().apply {
        moveTo(pts[0].x, pts[0].y)
        for (p in pts.drop(1)) lineTo(p.x, p.y)
        close()
    }
    val body = polygon(points)
    val top = polygon(listOf(points[4], points[5], points[6], Offset(c.x - r * 0.1f, c.y - r * 0.15f)))
    val bottom = polygon(listOf(points[0], points[1], points[2], Offset(c.x + r * 0.05f, c.y + r * 0.2f)))

    withTransform({ rotate(rotation, pivot = c) }) {
        drawPath(body, ROCK_BASE.copy(alpha = alpha))
        drawPath(top, ROCK_LIGHT.copy(alpha = alpha))
        drawPath(bottom, ROCK_DARK.copy(alpha = alpha))
        val crack = Path().apply {
            moveTo(c.x - r * 0.2f, c.y - r * 0.05f)
            lineTo(c.x + r * 0.15f, c.y + r * 0.12f)
            lineTo(c.x + r * 0.3f, c.y + r * 0.05f)
        }
        drawPath(crack, ink.copy(alpha = 0.65f * alpha), style = Stroke(r * 0.1f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(body, ink.copy(alpha = alpha), style = Stroke(r * 0.16f, join = StrokeJoin.Round))
    }
}