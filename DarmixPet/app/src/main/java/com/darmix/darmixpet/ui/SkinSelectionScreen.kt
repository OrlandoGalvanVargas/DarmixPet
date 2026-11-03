package com.darmix.darmixpet.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.darmix.darmixpet.mascota.ConfigurableSkin
import com.darmix.darmixpet.mascota.MascotaAnimState
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue
import com.darmix.darmixpet.ui.theme.AppPalettes

private val SpriteFilterQuality = FilterQuality.Medium

private const val FRAME_DELAY_MS = 140L

private const val AURA_FLICKER_MS = 1400
private const val POWER_UP_MS = 320
private const val POWER_DOWN_MS = 520

private class SkinFrames(val idle: List<ImageBitmap>, val tripleTap: List<ImageBitmap>)

@Composable
fun SkinSelectionScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme
    val skins = SkinRegistry.availableSkins

    var selectedId by remember { mutableStateOf(SkinPreferences.getSelectedSkinId(context)) }


    var sprites by remember { mutableStateOf<Map<String, SkinFrames>>(emptyMap()) }
    LaunchedEffect(Unit) {
        sprites = withContext(Dispatchers.IO) {
            skins.associate { info ->
                val idle = SpriteSheet.fromDrawable(context, info.idleRes, frameCount = 6)
                    .frames.map { it.asImageBitmap() }
                val tripleTap = runCatching {
                    ConfigurableSkin(info).getAnimation(context, MascotaAnimState.TRIPLE_TAP)
                        .frames.map { it.asImageBitmap() }
                }.getOrDefault(emptyList())
                info.id to SkinFrames(idle, tripleTap)
            }
        }
    }

    val pagerState = rememberPagerState(
        initialPage = skins.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
    ) { skins.size }
    val celebrate = remember { MutableSharedFlow<Int>(extraBufferCapacity = 1) }

    val viewedPage = pagerState.currentPage
    val viewed = skins[viewedPage]
    val viewedLore = SkinLore.forId(viewed.id)
    val viewedScheme = AppPalettes.forSkin(viewed.id, DarmixTheme.isDark).scheme
    val chooseContainer by animateColorAsState(viewedScheme.primary, tween(300), label = "chooseContainer")
    val chooseContent by animateColorAsState(viewedScheme.onPrimary, tween(300), label = "chooseContent")

    fun goTo(page: Int) {
        val target = ((page % skins.size) + skins.size) % skins.size
        scope.launch { pagerState.animateScrollToPage(target) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {

        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 14.dp)) {
            Text(
                "Elige tu compañero",
                style = MaterialTheme.typography.headlineMedium,
                color = scheme.onBackground
            )
            Text(
                "Desliza o usa las flechas para conocerlos",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
        }


        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(start = 36.dp, end = 40.dp),
            pageSpacing = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(370.dp)
        ) { page ->
            val skin = skins[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val distance = ((pagerState.currentPage - page) +
                                pagerState.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
                        val s = lerp(0.88f, 1f, 1f - distance)
                        scaleX = s
                        scaleY = s
                        alpha = lerp(0.55f, 1f, 1f - distance)
                    }
                    .padding(end = 6.dp, bottom = 6.dp)
            ) {
                SkinStage(
                    skin = skin,
                    frames = sprites[skin.id],
                    isSelected = skin.id == selectedId,
                    page = page,
                    celebrate = celebrate
                )
            }
        }

        Spacer(Modifier.height(10.dp))


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 23.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SquishIconButton(
                icon = DarmixIcons.ChevronLeft,
                contentDescription = "Personaje anterior",
                onClick = { goTo(viewedPage - 1) },
                container = scheme.surface,
                content = scheme.onSurface
            )
            PageDots(skins = skins, current = viewedPage, onSelect = ::goTo)
            SquishIconButton(
                icon = DarmixIcons.ChevronRight,
                contentDescription = "Personaje siguiente",
                onClick = { goTo(viewedPage + 1) },
                container = scheme.surface,
                content = scheme.onSurface
            )
        }

        Spacer(Modifier.height(8.dp))


        AnimatedContent(
            targetState = viewed.id == selectedId,
            transitionSpec = {
                (scaleIn(
                    initialScale = 0.9f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)
                ) + fadeIn()) togetherWith fadeOut(tween(100))
            },
            modifier = Modifier.padding(start = 20.dp, end = 20.dp),
            label = "chooseArea"
        ) { isCurrent ->
            if (isCurrent) {
                CurrentCompanionBanner()
            } else {
                SquishButton(
                    text = "Elegir a ${viewedLore.shortName}",
                    onClick = {
                        SkinPreferences.setSelectedSkinId(context, viewed.id)
                        selectedId = viewed.id
                        scope.launch { celebrate.emit(viewedPage) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    container = chooseContainer,
                    content = chooseContent,
                    icon = DarmixIcons.Check,
                    fillWidth = true
                )
            }
        }

        Spacer(Modifier.height(20.dp))


        AnimatedContent(
            targetState = viewedPage,
            transitionSpec = {
                (fadeIn(tween(220, delayMillis = 60)) + slideInVertically(tween(260)) { it / 14 }) togetherWith
                        fadeOut(tween(120))
            },
            modifier = Modifier.padding(start = 20.dp, end = 24.dp),
            label = "infoPanel"
        ) { page ->
            SkinInfoPanel(skin = skins[page], lore = SkinLore.forId(skins[page].id))
        }
    }
}



@Composable
private fun SkinStage(
    skin: SkinInfo,
    frames: SkinFrames?,
    isSelected: Boolean,
    page: Int,
    celebrate: SharedFlow<Int>
) {
    val scheme = MaterialTheme.colorScheme
    val colors = DarmixTheme.colors
    val skinScheme = AppPalettes.forSkin(skin.id, DarmixTheme.isDark).scheme
    val isDark = DarmixTheme.isDark
    val accent = Color(skin.accentColor)
    val stageFill = accent.copy(alpha = 0.20f).compositeOver(scheme.surface)


    var frameIndex by remember { mutableIntStateOf(0) }
    var reaction by remember { mutableStateOf<List<ImageBitmap>?>(null) }
    var reactionIndex by remember { mutableIntStateOf(0) }
    val idleFrames = frames?.idle.orEmpty()
    LaunchedEffect(idleFrames) {
        if (idleFrames.isNotEmpty()) {
            while (true) {
                delay(FRAME_DELAY_MS)
                frameIndex = (frameIndex + 1) % idleFrames.size
            }
        }
    }


    val infinite = rememberInfiniteTransition(label = "stage")
    val float by infinite.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )
    val flame by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(AURA_FLICKER_MS, easing = LinearEasing)),
        label = "flame"
    )


    val auraColors = remember(skin.id) { auraColorsFor(skin.id) }
    val power = remember { Animatable(0f) }
    LaunchedEffect(page, frames) {
        celebrate.collect { target ->
            val tripleTap = frames?.tripleTap.orEmpty()
            if (target == page && tripleTap.isNotEmpty() && reaction == null) {
                reaction = tripleTap
                val charge = launch {
                    power.animateTo(1f, tween(POWER_UP_MS, easing = FastOutSlowInEasing))
                }
                for (i in tripleTap.indices) {
                    reactionIndex = i
                    delay(FRAME_DELAY_MS)
                }
                charge.cancel()
                reaction = null
                power.animateTo(0f, tween(POWER_DOWN_MS, easing = FastOutSlowInEasing))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .sticker(shape = RoundedCornerShape(34.dp), fill = stageFill, depth = 4.dp)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .drawBehind {
                    val cx = size.width / 2f
                    val cy = size.height / 2f


                    val k = 1f + 0.12f * float
                    val w = 100.dp.toPx() * k
                    val h = 16.dp.toPx() * k
                    drawOval(
                        color = if (isDark) Color.Black.copy(alpha = 0.35f) else colors.ink.copy(alpha = 0.28f),
                        topLeft = Offset(cx - w / 2f, cy + 88.dp.toPx() - h / 2f),
                        size = Size(w, h)
                    )

                    drawEnergyAura(
                        center = Offset(cx, cy + float * 7.dp.toPx() + 4.dp.toPx()),
                        radiusX = 60.dp.toPx(),
                        radiusY = 90.dp.toPx(),
                        t = flame,
                        breath = float,
                        colors = auraColors,
                        power = power.value
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            val playing = reaction
            val bitmap = when {
                playing != null -> playing[reactionIndex.coerceIn(0, playing.lastIndex)]
                idleFrames.isNotEmpty() -> idleFrames[frameIndex % idleFrames.size]
                else -> null
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = skin.displayName,
                    filterQuality = SpriteFilterQuality,
                    modifier = Modifier
                        .size(170.dp)
                        .graphicsLayer {
                            translationY = float * 7.dp.toPx()
                        }
                )
            } else {
                HatBadge(size = 72.dp)
            }


            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                this@Column.AnimatedVisibility(
                    visible = isSelected,
                    enter = scaleIn(spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium)),
                    exit = scaleOut()
                ) {
                    StatusBadge("Tu compañero", DarmixIcons.Check, skinScheme.primary, skinScheme.onPrimary)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            skin.displayName,
            style = MaterialTheme.typography.titleLarge,
            color = scheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        StatusBadge(skin.subtitle, elementIconFor(skin.id), accent, readableOn(accent))
    }
}


private fun readableOn(background: Color): Color =
    if (background.luminance() > 0.45f) Color(0xFF1B1F27) else Color.White



@Composable
private fun PageDots(skins: List<SkinInfo>, current: Int, onSelect: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val ink = DarmixTheme.colors.ink
    Row(verticalAlignment = Alignment.CenterVertically) {
        skins.forEachIndexed { index, skin ->
            val selected = index == current
            val width by animateDpAsState(
                targetValue = if (selected) 30.dp else 14.dp,
                animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
                label = "dotWidth"
            )
            val color by animateColorAsState(
                targetValue = if (selected) Color(skin.accentColor) else scheme.surfaceVariant,
                animationSpec = tween(250),
                label = "dotColor"
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(role = Role.Tab, onClickLabel = "Ver a ${skin.displayName}") { onSelect(index) }
                    .padding(horizontal = 5.dp, vertical = 13.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width, 14.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(2.dp, ink, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun CurrentCompanionBanner() {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 4.dp, bottom = 4.dp)
            .sticker(
                shape = RoundedCornerShape(20.dp),
                fill = scheme.primaryContainer,
                depth = 3.dp
            )
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            DarmixIcons.Check, contentDescription = null,
            tint = scheme.onPrimaryContainer, modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "Es tu compañero actual",
            style = MaterialTheme.typography.labelLarge,
            color = scheme.onPrimaryContainer
        )
    }
}



@Composable
private fun SkinInfoPanel(skin: SkinInfo, lore: SkinLore) {
    val scheme = MaterialTheme.colorScheme
    val colors = DarmixTheme.colors
    val skinColors = AppPalettes.forSkin(skin.id, DarmixTheme.isDark).extras
    val skinScheme = AppPalettes.forSkin(skin.id, DarmixTheme.isDark).scheme
    val accent = Color(skin.accentColor)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .sticker(shape = MaterialTheme.shapes.large, depth = 3.dp)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Column {
            Text(
                "${lore.shortName} dice",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
            Text(
                "“${lore.quote}”",
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .sticker(
                        shape = RoundedCornerShape(
                            topStart = 22.dp, topEnd = 22.dp, bottomEnd = 22.dp, bottomStart = 6.dp
                        ),
                        fill = accent.copy(alpha = 0.18f).compositeOver(scheme.surface),
                        depth = 0.dp,
                        borderWidth = 1.5.dp
                    )
                    .padding(14.dp)
            )
        }

        Text(
            skin.description,
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onSurface
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FactTile("Personalidad", lore.personality, Modifier.weight(1f))
            FactTile("Elemento", lore.element, Modifier.weight(1f))
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionLabel("Su historia")
            Text(
                lore.story,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurface
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .sticker(
                    shape = RoundedCornerShape(20.dp),
                    fill = skinScheme.primaryContainer,
                    depth = 0.dp,
                    borderWidth = 1.5.dp
                )
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                elementIconFor(skin.id), contentDescription = null,
                tint = skinScheme.onPrimaryContainer, modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    "Su misión en la app",
                    style = MaterialTheme.typography.labelMedium,
                    color = skinScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    lore.mission,
                    style = MaterialTheme.typography.bodyMedium,
                    color = skinScheme.onPrimaryContainer
                )
            }
        }

        if (lore.palette.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Su paleta")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    lore.palette.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(
                                    2.dp,
                                    if (DarmixTheme.isDark) scheme.onSurfaceVariant.copy(alpha = 0.6f) else colors.ink,
                                    CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun FactTile(label: String, value: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .sticker(
                shape = RoundedCornerShape(18.dp),
                fill = scheme.surfaceVariant,
                depth = 0.dp,
                borderWidth = 1.5.dp
            )
            .padding(12.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
    }
}