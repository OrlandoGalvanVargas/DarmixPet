package com.darmix.darmixpet.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.darmix.darmixpet.mascota.BubbleShapeStyle
import com.darmix.darmixpet.mascota.CharacterPhrases
import com.darmix.darmixpet.mascota.SkinInfo
import com.darmix.darmixpet.mascota.SkinPreferences
import com.darmix.darmixpet.mascota.SkinRegistry
import com.darmix.darmixpet.mascota.SpriteSheet
import com.darmix.darmixpet.ui.components.HatBadge
import com.darmix.darmixpet.ui.components.SquishButton
import com.darmix.darmixpet.ui.components.SquishIconButton
import com.darmix.darmixpet.ui.components.StatusBadge
import com.darmix.darmixpet.ui.components.sticker
import com.darmix.darmixpet.ui.icons.DarmixIcons
import com.darmix.darmixpet.ui.theme.DarmixTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private enum class GestureKind { TAP1, TAP2, TAP3, HOLD, DRAG, AUTO }

private class GuideStep(
    val kind: GestureKind,
    val chip: String,
    val title: String,
    val body: String
)

private fun buildSteps(name: String) = listOf(
    GuideStep(
        GestureKind.TAP1, "1 toque", "Un toque",
        "Toca a $name una vez y te dirá una frase para animarte. Cada personaje tiene su propia voz."
    ),
    GuideStep(
        GestureKind.TAP2, "2 toques", "Doble toque",
        "Dos toques rápidos y $name hace una reacción divertida."
    ),
    GuideStep(
        GestureKind.TAP3, "3 toques", "Triple toque: linterna",
        "Tres toques seguidos y $name hace una reacción especial… ¡y enciende la linterna! " +
                "Repite los tres toques para apagarla."
    ),
    GuideStep(
        GestureKind.HOLD, "Mantener", "Mantenla presionada",
        "Abre el menú rápido: temporizador, brillo, volumen, más opciones y cambio de personaje. " +
                "Se cierra con la ✕ o tocando fuera."
    ),
    GuideStep(
        GestureKind.DRAG, "Arrastrar", "Arrástrala",
        "Llévala adonde no estorbe: se queda justo donde la sueltes."
    ),
    GuideStep(
        GestureKind.AUTO, "Automático", "También avisa sola",
        "$name está pendiente de ti aunque no la toques. Cuando suena música, se pone a escuchar."
    )
)




@Composable
fun GuideDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme

    val skin = remember { SkinRegistry.byId(SkinPreferences.getSelectedSkinId(context)) }
    val lore = remember(skin.id) { SkinLore.forId(skin.id) }
    val accent = Color(skin.accentColor)
    val steps = remember(lore.shortName) { buildSteps(lore.shortName) }
    val phrase = remember(skin.id) { CharacterPhrases.random(skin.id) }

    var frames by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    LaunchedEffect(skin.id) {
        frames = withContext(Dispatchers.IO) {
            SpriteSheet.fromDrawable(context, skin.idleRes, frameCount = 6)
                .frames.map { it.asImageBitmap() }
        }
    }

    val pagerState = rememberPagerState { steps.size }
    val page = pagerState.currentPage
    val isLast = page == steps.size - 1

    fun finish() {
        GuidePreferences.markSeen(context)
        onDismiss()
    }

    Dialog(
        onDismissRequest = { finish() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val enter = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            enter.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow))
        }

        Column(
            modifier = Modifier
                .padding(start = 16.dp, end = 20.dp, top = 24.dp, bottom = 28.dp)
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .heightIn(max = 720.dp)
                .graphicsLayer {
                    val s = 0.85f + 0.15f * enter.value
                    scaleX = s
                    scaleY = s
                    alpha = enter.value.coerceIn(0f, 1f)
                }
                .sticker(shape = RoundedCornerShape(30.dp), fill = scheme.surface, depth = 4.dp)
                .padding(18.dp)
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Guía",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant
                    )
                    Text(
                        "Cómo jugar con ${lore.shortName}",
                        style = MaterialTheme.typography.titleLarge,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                SquishIconButton(
                    icon = DarmixIcons.Close,
                    contentDescription = "Cerrar guía",
                    onClick = { finish() },
                    container = scheme.surface,
                    content = scheme.onSurface,
                    size = 40.dp
                )
            }

            Spacer(Modifier.height(10.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
            ) { index ->
                GuidePage(
                    step = steps[index],
                    frames = frames,
                    skin = skin,
                    accent = accent,
                    phrase = phrase
                )
            }

            Spacer(Modifier.height(12.dp))

            GuideDots(count = steps.size, current = page, accent = accent)

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SquishButton(
                    text = if (page == 0) "Omitir" else "Atrás",
                    onClick = {
                        if (page == 0) finish()
                        else scope.launch { pagerState.animateScrollToPage(page - 1) }
                    },
                    modifier = Modifier.weight(1f),
                    container = scheme.surface,
                    content = scheme.onSurface,
                    fillWidth = true
                )
                SquishButton(
                    text = if (isLast) "¡Entendido!" else "Siguiente",
                    onClick = {
                        if (isLast) finish()
                        else scope.launch { pagerState.animateScrollToPage(page + 1) }
                    },
                    modifier = Modifier.weight(1.3f),
                    icon = if (isLast) DarmixIcons.Check else null,
                    fillWidth = true
                )
            }
        }
    }
}

@Composable
private fun GuidePage(
    step: GuideStep,
    frames: List<ImageBitmap>,
    skin: SkinInfo,
    accent: Color,
    phrase: String
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(end = 4.dp, bottom = 4.dp)
    ) {
        GuideScene(step.kind, step.chip, frames, skin, accent, phrase)

        Spacer(Modifier.height(14.dp))
        Text(step.title, style = MaterialTheme.typography.titleLarge, color = scheme.onSurface)
        Spacer(Modifier.height(4.dp))
        Text(step.body, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)

        if (step.kind == GestureKind.AUTO) {
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AutoRow(
                    DarmixIcons.Battery, "Batería baja",
                    "Con menos de 20% aparece un aviso. Tócalo para cerrarlo; volverá a recordártelo.",
                    accent
                )
                AutoRow(
                    DarmixIcons.Moon, "De noche",
                    "Te recuerda que ya es hora de descansar.",
                    accent
                )
                AutoRow(
                    DarmixIcons.Lock, "Se acabó el tiempo",
                    "Si una app vigilada llega a su límite, te manda al inicio.",
                    accent
                )
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun AutoRow(icon: ImageVector, title: String, text: String, accent: Color) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.22f).compositeOver(scheme.surface))
                .border(2.dp, DarmixTheme.colors.ink, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = scheme.onSurface, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
            Text(text, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GuideDots(count: Int, current: Int, accent: Color) {
    val scheme = MaterialTheme.colorScheme
    val ink = DarmixTheme.colors.ink
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { i ->
            val selected = i == current
            val width by animateDpAsState(
                targetValue = if (selected) 26.dp else 12.dp,
                animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
                label = "guideDot"
            )
            val color by animateColorAsState(
                targetValue = if (selected) accent else scheme.surfaceVariant,
                animationSpec = tween(250),
                label = "guideDotColor"
            )
            Box(
                modifier = Modifier
                    .size(width, 12.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(2.dp, ink, CircleShape)
            )
        }
    }
}



private const val FRAME_DELAY_MS = 140L


@Composable
private fun GuideScene(
    kind: GestureKind,
    chip: String,
    frames: List<ImageBitmap>,
    skin: SkinInfo,
    accent: Color,
    phrase: String
) {
    val scheme = MaterialTheme.colorScheme
    val colors = DarmixTheme.colors
    val ink = Color(skin.theme.secondaryColor)
    val stageFill = accent.copy(alpha = 0.20f).compositeOver(scheme.surface)


    val transition = rememberInfiniteTransition(label = "guide")
    val tState = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing)),
        label = "guideT"
    )

    var frameIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(frames) {
        if (frames.isNotEmpty()) {
            while (true) {
                delay(FRAME_DELAY_MS)
                frameIndex = (frameIndex + 1) % frames.size
            }
        }
    }

    val centered = kind == GestureKind.DRAG

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .sticker(shape = RoundedCornerShape(28.dp), fill = stageFill, depth = 3.dp)
            .clip(RoundedCornerShape(28.dp))
            .drawBehind {
                val t = tState.value
                val cx = petCenterX(kind, size.width, t, this)
                val glow = 96.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.5f), Color.Transparent),
                        center = Offset(cx, size.height / 2f),
                        radius = glow
                    ),
                    radius = glow,
                    center = Offset(cx, size.height / 2f)
                )
                if (kind == GestureKind.DRAG) {

                    val y = size.height / 2f + 50.dp.toPx()
                    drawLine(
                        color = ink.copy(alpha = 0.35f),
                        start = Offset(size.width / 2f - 90.dp.toPx(), y),
                        end = Offset(size.width / 2f + 90.dp.toPx(), y),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 14f))
                    )
                }
            }
    ) {

        Box(modifier = Modifier.align(Alignment.TopStart).padding(12.dp)) {
            StatusBadge(chip, DarmixIcons.Sparkle, colors.gold, colors.onGold)
        }


        Box(
            modifier = Modifier
                .align(if (centered) Alignment.Center else Alignment.CenterStart)
                .padding(start = if (centered) 0.dp else 32.dp)
                .size(84.dp)
                .graphicsLayer {
                    val t = tState.value
                    translationX = if (kind == GestureKind.DRAG) dragOffsetDp(t).dp.toPx() else 0f
                    translationY = -hopDp(kind, t).dp.toPx()
                    rotationZ = spinDegrees(kind, t)
                },
            contentAlignment = Alignment.Center
        ) {
            if (frames.isNotEmpty()) {
                Image(
                    bitmap = frames[frameIndex % frames.size],
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                HatBadge(size = 64.dp)
            }
        }


        Canvas(modifier = Modifier.fillMaxSize()) {
            val t = tState.value
            val center = Offset(petCenterX(kind, size.width, t, this), size.height / 2f)
            val ring = accent
            when (kind) {
                GestureKind.TAP1 -> tapEffect(t, 0.15f, center, ring, ink)
                GestureKind.TAP2 -> {
                    tapEffect(t, 0.15f, center, ring, ink)
                    tapEffect(t, 0.30f, center, ring, ink)
                    sparkleBurst(t, 0.36f, 0.26f, center, colors.gold, accent, 34f, 0.7f)
                }
                GestureKind.TAP3 -> {
                    tapEffect(t, 0.10f, center, ring, ink)
                    tapEffect(t, 0.22f, center, ring, ink)
                    tapEffect(t, 0.34f, center, ring, ink)
                    sparkleBurst(t, 0.40f, 0.34f, center, colors.gold, accent, 52f, 1f)
                }
                GestureKind.HOLD -> {
                    if (t in 0.10f..0.56f) {
                        val p = ((t - 0.10f) / 0.40f).coerceIn(0f, 1f)
                        val r = 46.dp.toPx()
                        drawCircle(
                            ring.copy(alpha = 0.25f), r, center,
                            style = Stroke(width = 4.dp.toPx())
                        )
                        drawArc(
                            color = ring,
                            startAngle = -90f,
                            sweepAngle = 360f * p,
                            useCenter = false,
                            topLeft = Offset(center.x - r, center.y - r),
                            size = Size(r * 2f, r * 2f),
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                        touchDot(center, ink, 1f)
                    }
                }
                GestureKind.DRAG -> {
                    val a = envelope(t, 0f, 1f, 0.02f)
                    touchDot(center + Offset(0f, 26.dp.toPx()), ink, a)
                }
                GestureKind.AUTO -> Unit
            }
        }


        when (kind) {
            GestureKind.TAP1 -> Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 14.dp)
                    .graphicsLayer {
                        val e = envelope(tState.value, 0.28f, 0.92f, 0.08f)
                        alpha = e
                        val s = 0.7f + 0.3f * e
                        scaleX = s
                        scaleY = s
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
            ) { BubbleMock(phrase, skin) }

            GestureKind.HOLD -> Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 14.dp)
                    .graphicsLayer {
                        val e = envelope(tState.value, 0.56f, 0.95f, 0.06f)
                        alpha = e
                        val s = 0.7f + 0.3f * e
                        scaleX = s
                        scaleY = s
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
            ) { MenuMock(skin, accent) }

            GestureKind.AUTO -> {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 14.dp)
                        .graphicsLayer {
                            val pulse = 0.5f - 0.5f * cos(2f * PI.toFloat() * tState.value * 2f)
                            val s = 1f + 0.05f * pulse
                            scaleX = s
                            scaleY = s
                        }
                ) { BatteryMock(skin) }
                FloatingChip(
                    icon = DarmixIcons.Moon,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 14.dp),
                    tState = tState, phase = 0f, accent = accent
                )
                FloatingChip(
                    icon = DarmixIcons.Lock,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 12.dp, end = 14.dp),
                    tState = tState, phase = 0.5f, accent = accent
                )
            }

            GestureKind.TAP3 -> Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 14.dp)
                    .graphicsLayer {
                        val e = envelope(tState.value, 0.44f, 0.96f, 0.08f)
                        alpha = e
                        val s = 0.7f + 0.3f * e
                        scaleX = s
                        scaleY = s
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
            ) { FlashMock(skin) }

            else -> Unit
        }
    }
}



private fun mockShape(style: BubbleShapeStyle): Shape = when (style) {
    BubbleShapeStyle.ARCANE_GLOW -> RoundedCornerShape(22.dp)
    BubbleShapeStyle.ORGANIC_LEAF ->
        RoundedCornerShape(topStart = 22.dp, topEnd = 6.dp, bottomEnd = 22.dp, bottomStart = 6.dp)
    BubbleShapeStyle.ANGULAR_SHIELD -> CutCornerShape(7.dp)
}

@Composable
private fun BubbleMock(text: String, skin: SkinInfo) {
    val ink = Color(skin.theme.secondaryColor)
    val fill = Color(skin.theme.primaryColor).copy(alpha = 0.12f).compositeOver(Color.White)
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = ink,
        maxLines = 4,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .widthIn(max = 138.dp)
            .sticker(
                shape = mockShape(skin.theme.shapeStyle),
                fill = fill,
                depth = 3.dp,
                border = ink,
                shadow = ink
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    )
}

@Composable
private fun MenuMock(skin: SkinInfo, accent: Color) {
    val ink = Color(skin.theme.secondaryColor)
    val fill = Color(skin.theme.primaryColor).copy(alpha = 0.12f).compositeOver(Color.White)
    val icons = listOf(
        DarmixIcons.Clock, DarmixIcons.Sun, DarmixIcons.Speaker, DarmixIcons.Cog, DarmixIcons.Hat
    )
    Column(
        modifier = Modifier
            .widthIn(max = 148.dp)
            .sticker(
                shape = mockShape(skin.theme.shapeStyle),
                fill = fill,
                depth = 3.dp,
                border = ink,
                shadow = ink
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Menú rápido",
            style = MaterialTheme.typography.labelMedium,
            color = ink
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            icons.forEachIndexed { i, icon ->
                Box(
                    modifier = Modifier
                        .size(23.dp)
                        .clip(CircleShape)
                        .background(if (i == 0) accent.copy(alpha = 0.55f) else Color.Transparent)
                        .then(if (i == 0) Modifier.border(1.5.dp, ink, CircleShape) else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

@Composable
private fun BatteryMock(skin: SkinInfo) {
    val ink = Color(skin.theme.secondaryColor)
    val fill = Color(0xFFFFF3E0).copy(alpha = 1f)
    Row(
        modifier = Modifier
            .sticker(
                shape = mockShape(skin.theme.shapeStyle),
                fill = fill,
                depth = 3.dp,
                border = ink,
                shadow = ink
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            DarmixIcons.Battery, contentDescription = null,
            tint = Color(0xFFC96262), modifier = Modifier.size(26.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text("18%", style = MaterialTheme.typography.titleMedium, color = ink)
            Text("Batería baja", style = MaterialTheme.typography.labelSmall, color = ink)
        }
    }
}

@Composable
private fun FlashMock(skin: SkinInfo) {
    val ink = Color(skin.theme.secondaryColor)
    Row(
        modifier = Modifier
            .sticker(
                shape = mockShape(skin.theme.shapeStyle),
                fill = Color(0xFFFFF3C4),
                depth = 3.dp,
                border = ink,
                shadow = ink
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            DarmixIcons.Flashlight, contentDescription = null,
            tint = Color(0xFFB7791F), modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text("Linterna", style = MaterialTheme.typography.titleSmall, color = ink)
            Text("Encendida", style = MaterialTheme.typography.labelSmall, color = ink)
        }
    }
}

@Composable
private fun FloatingChip(
    icon: ImageVector,
    modifier: Modifier,
    tState: androidx.compose.runtime.State<Float>,
    phase: Float,
    accent: Color
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(34.dp)
            .graphicsLayer {
                translationY = sin(2f * PI.toFloat() * (tState.value + phase)) * 3.dp.toPx()
            }
            .sticker(
                shape = CircleShape,
                fill = accent.copy(alpha = 0.25f).compositeOver(scheme.surface),
                depth = 2.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = scheme.onSurface, modifier = Modifier.size(18.dp))
    }
}



private fun envelope(t: Float, start: Float, end: Float, ramp: Float = 0.06f): Float =
    min((t - start) / ramp, (end - t) / ramp).coerceIn(0f, 1f)

private fun hop(t: Float, start: Float, dur: Float, height: Float): Float =
    if (t in start..(start + dur)) sin(PI.toFloat() * (t - start) / dur) * height else 0f

private fun hopDp(kind: GestureKind, t: Float): Float = when (kind) {
    GestureKind.TAP1 -> hop(t, 0.15f, 0.14f, 8f)
    GestureKind.TAP2 -> hop(t, 0.15f, 0.12f, 9f) + hop(t, 0.30f, 0.12f, 9f)
    GestureKind.TAP3 -> hop(t, 0.10f, 0.10f, 6f) + hop(t, 0.22f, 0.10f, 6f) + hop(t, 0.36f, 0.22f, 26f)
    else -> 0f
}

private fun spinDegrees(kind: GestureKind, t: Float): Float =
    if (kind == GestureKind.TAP3 && t in 0.36f..0.58f) 360f * (t - 0.36f) / 0.22f else 0f

private fun dragOffsetDp(t: Float): Float = sin(2f * PI.toFloat() * t) * 90f


private fun petCenterX(kind: GestureKind, width: Float, t: Float, density: androidx.compose.ui.unit.Density): Float =
    if (kind == GestureKind.DRAG) width / 2f + with(density) { dragOffsetDp(t).dp.toPx() }
    else with(density) { 74.dp.toPx() }

private fun DrawScope.tapEffect(t: Float, start: Float, center: Offset, ring: Color, ink: Color) {
    val p = (t - start) / 0.18f
    if (p in 0f..1f) {
        drawCircle(
            color = ring.copy(alpha = (1f - p) * 0.9f),
            radius = (12f + 36f * p).dp.toPx(),
            center = center,
            style = Stroke(width = 3.dp.toPx())
        )
    }
    if (t in start..(start + 0.05f)) touchDot(center, ink, 1f)
}


private fun DrawScope.touchDot(center: Offset, ink: Color, alpha: Float) {
    drawCircle(ink.copy(alpha = 0.22f * alpha), 17.dp.toPx(), center)
    drawCircle(Color.White.copy(alpha = 0.9f * alpha), 11.dp.toPx(), center)
    drawCircle(ink.copy(alpha = alpha), 11.dp.toPx(), center, style = Stroke(2.dp.toPx()))
}

private fun DrawScope.sparkleBurst(
    t: Float,
    start: Float,
    dur: Float,
    center: Offset,
    gold: Color,
    accent: Color,
    maxRadiusDp: Float,
    sizeScale: Float
) {
    val p = (t - start) / dur
    if (p !in 0f..1f) return
    for (i in 0 until 8) {
        val a = (i * 45f + 10f) * PI.toFloat() / 180f
        val r = (34f + maxRadiusDp * p).dp.toPx()
        val c = if (i % 2 == 0) gold else accent
        drawSparkle(
            Offset(center.x + r * cos(a), center.y + r * sin(a)),
            ((1f - p) * 9f * sizeScale + 2f).dp.toPx(),
            c.copy(alpha = 1f - p)
        )
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
