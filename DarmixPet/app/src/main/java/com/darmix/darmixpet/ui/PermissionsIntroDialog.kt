package com.darmix.darmixpet.ui

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.darmix.darmixpet.mascota.SkinRegistry
import com.darmix.darmixpet.mascota.SpriteSheet
import com.darmix.darmixpet.ui.components.SquishButton
import com.darmix.darmixpet.ui.components.sticker
import com.darmix.darmixpet.ui.theme.DarmixTheme
import com.darmix.darmixpet.ui.theme.rememberSelectedSkinId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

object SetupProgressPrefs {
    private const val PREFS_NAME = "darmixpet_prefs"
    private const val KEY_SETUP_COMPLETED = "permissions_setup_completed"
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    fun wasCompleted(context: Context) = prefs(context).getBoolean(KEY_SETUP_COMPLETED, false)
    fun markCompleted(context: Context) {
        prefs(context).edit().putBoolean(KEY_SETUP_COMPLETED, true).apply()
    }
}

private enum class IntroMood { WELCOME, RESUME, REVOKED }

private data class IntroCopy(
    val kicker: String,
    val title: String,
    val body: String,
    val action: String
)

private const val FRAME_DELAY_MS = 140L
private const val IDLE_FRAMES = 6

private fun introCopy(mood: IntroMood, petName: String, granted: Int, total: Int): IntroCopy {
    val missing = total - granted
    return when (mood) {
        IntroMood.WELCOME -> IntroCopy(
            kicker = "Bienvenido a DarmixPet",
            title = "¡Hola! Soy $petName",
            body = "Para flotar sobre tus apps y cuidar tu tiempo de pantalla, Android me pide unos permisos. Yo te guío paso a paso, es rápido y solo se hace una vez.",
            action = "Empecemos"
        )
        IntroMood.RESUME -> IntroCopy(
            kicker = "Te quedaste a medias",
            title = if (missing == 1) "Te falta 1 permiso" else "Te faltan $missing permisos",
            body = "Sin ellos todavía no puedo acompañarte. Ya vas avanzado, te muestro cuáles faltan.",
            action = "Continuar"
        )
        IntroMood.REVOKED -> IntroCopy(
            kicker = "Algo cambió en tu teléfono",
            title = if (missing == 1) "Se desactivó un permiso" else "Se desactivaron $missing permisos",
            body = if (missing == 1) "Sin él no puedo seguir cuidando tu tiempo. Actívalo de nuevo y listo."
            else "Sin ellos no puedo seguir cuidando tu tiempo. Actívalos de nuevo y listo.",
            action = "Activar de nuevo"
        )
    }
}

@Composable
fun PermissionsIntroDialog(granted: Int, total: Int, onStart: () -> Unit) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme

    val skinId by rememberSelectedSkinId()
    val petName = SkinLore.forId(skinId).shortName
    val setupCompletedBefore = remember { SetupProgressPrefs.wasCompleted(context) }
    val mood = when {
        setupCompletedBefore -> IntroMood.REVOKED
        granted == 0 -> IntroMood.WELCOME
        else -> IntroMood.RESUME
    }
    val copy = introCopy(mood, petName, granted, total)

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        val enter = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            enter.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow))
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    val s = 0.82f + 0.18f * enter.value
                    scaleX = s
                    scaleY = s
                    alpha = enter.value.coerceIn(0f, 1f)
                }
                .sticker(shape = RoundedCornerShape(32.dp), fill = scheme.surface, depth = 5.dp)
                .padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PetStage(skinId = skinId, petName = petName)

            Spacer(Modifier.height(10.dp))
            Text(
                copy.kicker,
                style = MaterialTheme.typography.labelMedium,
                color = scheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                copy.title,
                style = MaterialTheme.typography.headlineMedium,
                color = scheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                copy.body,
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (mood != IntroMood.WELCOME) {
                Spacer(Modifier.height(18.dp))
                IntroProgress(granted = granted, total = total)
            }

            Spacer(Modifier.height(22.dp))
            SquishButton(
                text = copy.action,
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
                fillWidth = true
            )
        }
    }
}

@Composable
private fun PetStage(skinId: String, petName: String) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val colors = DarmixTheme.colors

    var frames by remember(skinId) { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    LaunchedEffect(skinId) {
        frames = withContext(Dispatchers.IO) {
            runCatching {
                SpriteSheet.fromDrawable(context, SkinRegistry.byId(skinId).idleRes, frameCount = IDLE_FRAMES)
                    .frames.map { it.asImageBitmap() }
            }.getOrDefault(emptyList())
        }
    }

    var frameIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(frames) {
        if (frames.isNotEmpty()) {
            while (true) {
                delay(FRAME_DELAY_MS)
                frameIndex = (frameIndex + 1) % frames.size
            }
        }
    }

    val appear = remember { Animatable(0f) }
    LaunchedEffect(frames.isNotEmpty()) {
        if (frames.isNotEmpty()) {
            appear.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow))
        }
    }

    val infinite = rememberInfiniteTransition(label = "introStage")
    val float by infinite.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float"
    )

    val halo = scheme.primaryContainer
    val shadowColor = colors.ink.copy(alpha = 0.18f)

    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            drawCircle(halo, radius = size.minDimension * 0.40f, center = c)
            drawCircle(
                color = halo.copy(alpha = 0.45f),
                radius = size.minDimension * 0.48f,
                center = c
            )
            drawOval(
                color = shadowColor,
                topLeft = Offset(c.x - 34.dp.toPx() + float * 3.dp.toPx(), c.y + 66.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(68.dp.toPx() - float * 6.dp.toPx(), 12.dp.toPx())
            )
        }

        if (frames.isNotEmpty()) {
            Image(
                bitmap = frames[frameIndex % frames.size],
                contentDescription = petName,
                filterQuality = FilterQuality.Medium,
                modifier = Modifier
                    .size(150.dp)
                    .graphicsLayer {
                        translationY = float * 6.dp.toPx() - (1f - appear.value) * 24.dp.toPx()
                        val s = 0.6f + 0.4f * appear.value
                        scaleX = s
                        scaleY = s
                        alpha = appear.value.coerceIn(0f, 1f)
                    }
            )
        }
    }
}

@Composable
private fun IntroProgress(granted: Int, total: Int) {
    val scheme = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(total) { i ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(12.dp)
                        .clip(CircleShape)
                        .background(if (i < granted) scheme.primary else scheme.surfaceVariant)
                        .border(2.dp, DarmixTheme.colors.ink, CircleShape)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "$granted de $total listos",
            style = MaterialTheme.typography.labelLarge,
            color = scheme.onSurfaceVariant
        )
    }
}