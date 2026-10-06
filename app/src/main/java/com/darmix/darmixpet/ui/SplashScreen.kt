package com.darmix.darmixpet.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darmix.darmixpet.mascota.CharacterPhrases
import com.darmix.darmixpet.mascota.SkinPreferences
import com.darmix.darmixpet.mascota.SkinRegistry
import com.darmix.darmixpet.ui.components.HatBadge
import com.darmix.darmixpet.ui.components.StatusBadge
import com.darmix.darmixpet.ui.icons.DarmixIcons
import com.darmix.darmixpet.ui.theme.DarmixTheme
import com.darmix.darmixpet.ui.theme.Fredoka
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DarmixSplashScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val colors = DarmixTheme.colors

    val skin = remember { SkinRegistry.byId(SkinPreferences.getSelectedSkinId(context)) }
    val accent = remember(skin.id) { Color(skin.accentColor) }
    val phrase = remember(skin.id) { CharacterPhrases.random(skin.id) }
    val shortName = remember(skin.id) {
        skin.displayName.split(" ")
            .firstOrNull { it.length > 2 && it !in setOf("Sir", "el", "la", "los", "las") }
            ?: skin.displayName
    }
    val version = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        } catch (e: Exception) {
            ""
        }
    }
    val dots = remember { Animatable(0f) }
    val shadow = remember { Animatable(0f) }
    val hatFall = remember { Animatable(0f) }
    val hatSquash = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }
    val letters = remember { Animatable(0f) }
    val tagline = remember { Animatable(0f) }
    val phraseAlpha = remember { Animatable(0f) }
    val exit = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { dots.animateTo(1f, tween(250)) }
            launch { delay(70);  shadow.animateTo(1f, spring(dampingRatio = 0.6f)) }
            launch {
                delay(150)
                hatFall.animateTo(
                    1f,
                    spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMediumLow)
                )
            }
            launch {
                delay(480)
                hatSquash.animateTo(1f, tween(60))
                hatSquash.animateTo(0f, spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessMedium))
            }
            launch { delay(540);  burst.animateTo(1f, tween(500)) }
            launch { delay(540);  letters.animateTo(1f, tween(450)) }
            launch { delay(850);  tagline.animateTo(1f, tween(280)) }
            launch { delay(1000); phraseAlpha.animateTo(1f, tween(280)) }
        }
        delay(1300)
        exit.animateTo(0f, tween(280))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = exit.value }
            .background(scheme.background)
            .drawBehind {
                val baseAlpha = dots.value * 0.55f
                if (baseAlpha <= 0f) return@drawBehind
                val step = 26.dp.toPx()
                val radius = 1.5.dp.toPx()
                val dotColor = scheme.outline.copy(alpha = baseAlpha)
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val t = shadow.value
                    if (t <= 0f) return@Canvas
                    val w = 100.dp.toPx() * t
                    val h = 14.dp.toPx() * t
                    val cx = size.width / 2f
                    val cy = size.height * 0.70f
                    drawOval(
                        color = colors.ink.copy(alpha = 0.28f * t),
                        topLeft = Offset(cx - w / 2f, cy - h / 2f),
                        size = Size(w, h)
                    )
                }

                Box(
                    modifier = Modifier.graphicsLayer {
                        val fall = 1f - hatFall.value
                        translationY = -320.dp.toPx() * fall
                        val squash = hatSquash.value
                        val sx = 1f + 0.18f * squash
                        val sy = 1f - 0.14f * squash
                        scaleX = sx
                        scaleY = sy
                    }
                ) {
                    HatBadge(size = 130.dp)
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val t = burst.value
                    if (t !in 0.01f..0.99f) return@Canvas
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    for (i in 0 until 8) {
                        val a = (i * 45f + 22.5f) * (PI / 180f).toFloat()
                        val r = 62.dp.toPx() + t * 78.dp.toPx()
                        val starR = 13.dp.toPx() * (1f - t) + 3.dp.toPx()
                        val c = if (i % 2 == 0) colors.gold else accent
                        drawSparkle(
                            center = Offset(cx + r * cos(a), cy + r * sin(a)),
                            radius = starR,
                            color = c.copy(alpha = 1f - t)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            val name = "DarmixPet"
            Row {
                name.forEachIndexed { i, c ->
                    val p = ((letters.value * (name.length + 1)) - i).coerceIn(0f, 1f)
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
                    alpha = tagline.value
                    translationY = (1f - tagline.value) * 12.dp.toPx()
                }
            )

            Spacer(Modifier.height(36.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = phraseAlpha.value
                        translationY = (1f - phraseAlpha.value) * 10.dp.toPx()
                    }
                    .padding(horizontal = 32.dp)
            ) {
                StatusBadge(
                    text = shortName,
                    icon = DarmixIcons.Sparkle,
                    container = accent.copy(alpha = 0.20f).compositeOver(scheme.surface),
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
                    .graphicsLayer { alpha = tagline.value * 0.75f }
            )
        }
    }
}

private fun DrawScope.drawSparkle(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        val inner = radius * 0.34f
        for (i in 0 until 8) {
            val r = if (i % 2 == 0) radius else inner
            val a = (PI / 4.0 * i - PI / 2.0).toFloat()
            val x = center.x + r * cos(a)
            val y = center.y + r * sin(a)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path, color)
}
