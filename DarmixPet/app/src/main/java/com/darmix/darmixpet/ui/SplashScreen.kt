package com.darmix.darmixpet.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darmix.darmixpet.mascota.CharacterPhrases
import com.darmix.darmixpet.mascota.SkinPreferences
import com.darmix.darmixpet.mascota.SkinRegistry
import com.darmix.darmixpet.mascota.SpriteSheet
import com.darmix.darmixpet.ui.components.HatBadge
import com.darmix.darmixpet.ui.components.StatusBadge
import com.darmix.darmixpet.ui.icons.DarmixIcons
import com.darmix.darmixpet.ui.theme.DarmixTheme
import com.darmix.darmixpet.ui.theme.Fredoka
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

private const val SPLASH_TOTAL_MS = 2450f
private const val MATERIALIZE_START = 150f
private const val MATERIALIZE_END = 700f
private const val ACTION_START = 750f
private const val ACTION_LENGTH = 800f
private const val LETTERS_START = 1050f
private const val TAGLINE_START = 1400f
private const val PHRASE_START = 1600f
private const val EXIT_START = 2200f
private const val EXIT_LENGTH = 250f
private const val FRAME_MS = 120f

private enum class SplashAction { ARCANE, NATURE, GUARDIAN }

private fun splashActionFor(skinId: String): SplashAction = when (skinId) {
    "skin2" -> SplashAction.NATURE
    "skin3" -> SplashAction.GUARDIAN
    else -> SplashAction.ARCANE
}

private fun progress(t: Float, start: Float, length: Float): Float =
    ((t - start) / length).coerceIn(0f, 1f)

private fun easeOut(v: Float): Float = 1f - (1f - v).pow(3)

private val LeafSeeds = FloatArray(14) { i -> ((sin(i * 12.9898f) * 43758.547f) % 1f + 1f) % 1f }

@Composable
fun DarmixSplashScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val colors = DarmixTheme.colors
    val isDark = DarmixTheme.isDark

    val skin = remember { SkinRegistry.byId(SkinPreferences.getSelectedSkinId(context)) }
    val action = remember(skin.id) { splashActionFor(skin.id) }
    val glow = remember(skin.id) { Color(skin.accentColor) }
    val phrase = remember(skin.id) { CharacterPhrases.random(skin.id) }
    val shortName = remember(skin.id) { SkinLore.forId(skin.id).shortName }
    val version = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        } catch (e: Exception) {
            ""
        }
    }

    var idleFrames by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var actionFrames by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var elapsed by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(skin.id) {
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                val idle = SpriteSheet.fromDrawable(context, skin.idleRes, frameCount = 6)
                    .frames.map { it.asImageBitmap() }
                val reaction = SpriteSheet.fromDrawable(context, skin.tripleTapRes, frameCount = 6)
                    .frames.map { it.asImageBitmap() }
                idle to reaction
            }.getOrNull()
        }
        if (loaded != null) {
            idleFrames = loaded.first
            actionFrames = loaded.second
        }
        val origin = withFrameMillis { it }
        while (elapsed < SPLASH_TOTAL_MS) {
            elapsed = (withFrameMillis { it } - origin).toFloat()
        }
        onFinished()
    }

    val t = elapsed
    val primary = scheme.primary
    val shadowColor = if (isDark) Color.Black.copy(alpha = 0.35f) else colors.ink.copy(alpha = 0.22f)

    val materialize = easeOut(progress(t, MATERIALIZE_START, MATERIALIZE_END - MATERIALIZE_START))
    val flicker = if (materialize < 1f) 0.55f + 0.45f * sin(t / 22f) else 1f
    val local = t - ACTION_START
    val actionOn = local in 0f..ACTION_LENGTH

    var petOffsetY = 0f
    var petScale = 0.82f + 0.18f * materialize
    var petRotation = 0f
    var shakeX = 0f
    if (local > 0f) {
        when (action) {
            SplashAction.ARCANE ->
                petOffsetY = -sin(progress(t, ACTION_START, 700f) * PI.toFloat()) * 16f
            SplashAction.NATURE ->
                petRotation = sin(local / 260f) * 3.5f * (1f - progress(t, ACTION_START, ACTION_LENGTH) * 0.5f)
            SplashAction.GUARDIAN -> {
                val impact = progress(t, ACTION_START, 320f)
                if (impact < 1f) {
                    val bump = sin(impact * PI.toFloat())
                    petOffsetY = bump * 14f
                    petScale *= 1f + 0.06f * bump
                }
                if (local < 300f) shakeX = sin(local / 18f) * 4f * (1f - local / 300f)
            }
        }
    }

    val useAction = actionOn && actionFrames.isNotEmpty()
    val frame: ImageBitmap? = when {
        useAction -> actionFrames[minOf((local / FRAME_MS).toInt(), actionFrames.lastIndex)]
        idleFrames.isNotEmpty() -> idleFrames[(t / FRAME_MS).toInt() % idleFrames.size]
        else -> null
    }

    val contentAlpha = 1f - progress(t, EXIT_START, EXIT_LENGTH)
    val dotsAlpha = progress(t, 0f, 250f) * 0.55f
    val letters = progress(t, LETTERS_START, 400f)
    val tagline = progress(t, TAGLINE_START, 250f)
    val phraseAlpha = progress(t, PHRASE_START, 250f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .drawBehind {
                if (dotsAlpha <= 0f) return@drawBehind
                val step = 26.dp.toPx()
                val radius = 1.5.dp.toPx()
                val dotColor = scheme.outline.copy(alpha = dotsAlpha * contentAlpha)
                val cols = (size.width / step).toInt() + 1
                val rows = (size.height / step).toInt() + 1
                for (row in 0..rows) {
                    for (col in 0..cols) {
                        drawCircle(
                            color = dotColor,
                            radius = radius,
                            center = Offset(col * step + step / 2f, row * step + step / 2f)
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.graphicsLayer { alpha = contentAlpha },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .graphicsLayer { translationX = shakeX.dp.toPx() }
                    .drawBehind { drawStageBack(t, action, materialize, glow, primary, shadowColor) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .graphicsLayer {
                            alpha = materialize * flicker
                            scaleX = petScale
                            scaleY = petScale
                            translationY = petOffsetY.dp.toPx()
                            rotationZ = petRotation
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (frame != null) {
                        Image(
                            bitmap = frame,
                            contentDescription = skin.displayName,
                            filterQuality = FilterQuality.Medium,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        HatBadge(size = 110.dp)
                    }
                }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawStageFront(t, action, glow, primary)
                }
            }

            Spacer(Modifier.height(16.dp))

            val name = "DarmixPet"
            Row {
                name.forEachIndexed { i, c ->
                    val p = ((letters * (name.length + 1)) - i).coerceIn(0f, 1f)
                    Text(
                        text = c.toString(),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = Fredoka,
                            fontWeight = FontWeight.Bold,
                            fontSize = 44.sp
                        ),
                        color = scheme.onBackground,
                        modifier = Modifier.graphicsLayer {
                            alpha = p
                            translationY = (1f - p) * 26.dp.toPx()
                            val s = 0.7f + 0.3f * p
                            scaleX = s
                            scaleY = s
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Tu compañero de tiempo de pantalla",
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer {
                    alpha = tagline
                    translationY = (1f - tagline) * 12.dp.toPx()
                }
            )

            Spacer(Modifier.height(32.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = phraseAlpha
                        translationY = (1f - phraseAlpha) * 10.dp.toPx()
                    }
                    .padding(horizontal = 32.dp)
            ) {
                StatusBadge(
                    text = shortName,
                    icon = DarmixIcons.Sparkle,
                    container = glow.copy(alpha = 0.20f).compositeOver(scheme.surface),
                    content = scheme.onSurface
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "\u201C$phrase\u201D",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (version.isNotBlank()) {
            Text(
                text = "v$version",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 44.dp)
                    .graphicsLayer { alpha = tagline * 0.75f * contentAlpha }
            )
        }
    }
}

private fun DrawScope.drawStageBack(
    t: Float,
    action: SplashAction,
    materialize: Float,
    glow: Color,
    primary: Color,
    shadow: Color
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val ground = center.y + 80.dp.toPx()
    if (materialize > 0f) {
        val radius = 120.dp.toPx()
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glow.copy(alpha = 0.5f * materialize), Color.Transparent),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
        val w = 130.dp.toPx() * materialize
        val h = 16.dp.toPx() * materialize
        drawOval(shadow, Offset(center.x - w / 2f, ground - h / 2f), Size(w, h))
    }
    if (t <= ACTION_START) return
    when (action) {
        SplashAction.ARCANE -> drawArcaneCircle(t, Offset(center.x, ground), primary)
        SplashAction.NATURE -> drawSprouts(t, center.x, ground, glow, primary)
        SplashAction.GUARDIAN -> {
            drawShield(t, center, glow)
            drawDust(t, center.x, ground, glow)
        }
    }
}

private fun DrawScope.drawStageFront(t: Float, action: SplashAction, glow: Color, primary: Color) {
    val center = Offset(size.width / 2f, size.height / 2f)
    if (t in MATERIALIZE_START..ACTION_START) {
        val p = progress(t, MATERIALIZE_START, ACTION_START - MATERIALIZE_START)
        for (i in 0 until 14) {
            val angle = i / 14f * 2f * PI.toFloat() + p * 2f
            val r = 125.dp.toPx() * (1f - easeOut(p))
            drawSparkle(
                center = Offset(center.x + r * cos(angle), center.y + r * sin(angle)),
                radius = (6f * (1f - p) + 2f).dp.toPx(),
                color = glow.copy(alpha = 1f - p * 0.6f)
            )
        }
    }
    if (t <= ACTION_START) return
    when (action) {
        SplashAction.ARCANE -> drawOrbitStars(t, center, glow, primary)
        SplashAction.NATURE -> drawLeaves(t, center, glow, primary)
        SplashAction.GUARDIAN -> Unit
    }
}

private fun DrawScope.drawArcaneCircle(t: Float, center: Offset, color: Color) {
    val fade = 1f - progress(t, ACTION_START + 550f, 350f)
    if (fade <= 0f) return
    val local = t - ACTION_START
    val r = (26f + 74f * easeOut(progress(t, ACTION_START, 300f))).dp.toPx()
    val c = color.copy(alpha = 0.75f * fade)
    withTransform({
        translate(center.x, center.y)
        scale(1f, 0.32f, pivot = Offset.Zero)
        rotate(local / 700f * 57.3f, pivot = Offset.Zero)
    }) {
        drawCircle(c, r, Offset.Zero, style = Stroke(3.dp.toPx()))
        drawCircle(c, r * 0.75f, Offset.Zero, style = Stroke(2.dp.toPx()))
        for (i in 0 until 12) {
            val a = i / 12f * 2f * PI.toFloat()
            drawLine(
                color = c,
                start = Offset(r * 0.75f * cos(a), r * 0.75f * sin(a)),
                end = Offset(r * cos(a), r * sin(a)),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

private fun DrawScope.drawOrbitStars(t: Float, center: Offset, glow: Color, primary: Color) {
    val burst = progress(t, ACTION_START + 450f, 350f)
    if (burst >= 1f) return
    val local = t - ACTION_START
    val appear = progress(t, ACTION_START, 200f)
    for (i in 0 until 6) {
        val a = i / 6f * 2f * PI.toFloat() + local / 320f
        val r = (70f + burst * 100f).dp.toPx()
        drawSparkle(
            center = Offset(center.x + r * cos(a), center.y - 6.dp.toPx() + r * 0.55f * sin(a)),
            radius = (7f * (1f - burst * 0.6f)).dp.toPx(),
            color = (if (i % 2 == 0) primary else glow).copy(alpha = (1f - burst) * appear)
        )
    }
}

private fun DrawScope.drawLeaves(t: Float, center: Offset, glow: Color, primary: Color) {
    val ground = center.y + 80.dp.toPx()
    val w = 18.dp.toPx()
    val h = 8.dp.toPx()
    for (i in 0 until 14) {
        val p = progress(t, ACTION_START + i * 30f, 650f)
        if (p <= 0f || p >= 1f) continue
        val a = LeafSeeds[i] * 6f + p * PI.toFloat() * 3.2f
        val r = 78.dp.toPx() * (1f - p * 0.4f)
        val pos = Offset(center.x + r * cos(a), ground - p * 215.dp.toPx())
        withTransform({ rotate(Math.toDegrees(a.toDouble()).toFloat(), pivot = pos) }) {
            drawOval(
                color = (if (i % 2 == 0) glow else primary).copy(alpha = 1f - p),
                topLeft = Offset(pos.x - w / 2f, pos.y - h / 2f),
                size = Size(w, h)
            )
        }
    }
}

private fun DrawScope.drawSprouts(t: Float, centerX: Float, ground: Float, glow: Color, primary: Color) {
    for (i in -1..1) {
        val g = easeOut(progress(t, ACTION_START + 100f + abs(i) * 80f, 350f))
        if (g <= 0f) continue
        val x = centerX + i * 78.dp.toPx()
        val stem = 24.dp.toPx() * g
        val lw = 13.dp.toPx() * g
        val lh = 6.dp.toPx() * g
        drawLine(primary, Offset(x, ground), Offset(x, ground - stem), 3.dp.toPx(), cap = StrokeCap.Round)
        withTransform({ rotate(-35f, pivot = Offset(x, ground - stem * 0.8f)) }) {
            drawOval(glow, Offset(x - lw, ground - stem * 0.8f - lh / 2f), Size(lw, lh))
        }
        withTransform({ rotate(35f, pivot = Offset(x, ground - stem)) }) {
            drawOval(glow, Offset(x, ground - stem - lh / 2f), Size(lw, lh))
        }
    }
}

private fun DrawScope.drawShield(t: Float, center: Offset, color: Color) {
    val p = progress(t, ACTION_START, 450f)
    if (p >= 1f) return
    val s = (0.6f + p * 0.9f) * 1.dp.toPx()
    val c = Offset(center.x, center.y - 6.dp.toPx())
    val path = Path().apply {
        moveTo(c.x, c.y - 70f * s)
        lineTo(c.x + 57f * s, c.y - 48f * s)
        lineTo(c.x + 57f * s, c.y + 6f * s)
        quadraticTo(c.x + 51f * s, c.y + 51f * s, c.x, c.y + 76f * s)
        quadraticTo(c.x - 51f * s, c.y + 51f * s, c.x - 57f * s, c.y + 6f * s)
        lineTo(c.x - 57f * s, c.y - 48f * s)
        close()
    }
    drawPath(path, color.copy(alpha = (1f - p) * 0.7f), style = Stroke(5.dp.toPx()))
}

private fun DrawScope.drawDust(t: Float, centerX: Float, ground: Float, color: Color) {
    val q = progress(t, ACTION_START + 25f, 420f)
    if (q <= 0f || q >= 1f) return
    for (i in 0 until 10) {
        val dir = if (i < 5) -1f else 1f
        val k = i % 5
        val x = centerX + dir * (38f + q * (52f + k * 18f)).dp.toPx()
        val y = ground - (q * (6f + k * 5f)).dp.toPx()
        drawCircle(color.copy(alpha = (1f - q) * 0.6f), (6f + q * 9f).dp.toPx(), Offset(x, y))
    }
}

private fun DrawScope.drawSparkle(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        val inner = radius * 0.34f
        for (i in 0 until 8) {
            val r = if (i % 2 == 0) radius else inner
            val a = (PI / 4.0 * i - PI / 2.0).toFloat()
            val px = center.x + r * cos(a)
            val py = center.y + r * sin(a)
            if (i == 0) moveTo(px, py) else lineTo(px, py)
        }
        close()
    }
    drawPath(path, color)
}