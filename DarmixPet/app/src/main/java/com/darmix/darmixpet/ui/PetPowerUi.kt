package com.darmix.darmixpet.ui

import android.content.SharedPreferences
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.darmix.darmixpet.overlay.PetController
import com.darmix.darmixpet.overlay.PetPreferences
import com.darmix.darmixpet.ui.components.GemSwitch
import com.darmix.darmixpet.ui.components.HatBadge
import com.darmix.darmixpet.ui.components.SquishButton
import com.darmix.darmixpet.ui.components.StatusBadge
import com.darmix.darmixpet.ui.components.sticker
import com.darmix.darmixpet.ui.icons.DarmixIcons
import com.darmix.darmixpet.ui.theme.DarmixTheme
import kotlin.math.PI
import kotlin.math.sin


@Composable
fun rememberPetEnabled(): State<Boolean> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(PetPreferences.isEnabled(context)) }
    DisposableEffect(context) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PetPreferences.KEY_PET_ENABLED) state.value = PetPreferences.isEnabled(context)
        }
        PetPreferences.registerListener(context, listener)
        onDispose { PetPreferences.unregisterListener(context, listener) }
    }
    return state
}




@Composable
fun PetPowerCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val colors = DarmixTheme.colors
    val enabled by rememberPetEnabled()
    var confirmSleep by remember { mutableStateOf(false) }

    val fill by animateColorAsState(
        targetValue = if (enabled) scheme.tertiaryContainer else scheme.surfaceVariant,
        animationSpec = tween(300),
        label = "petCardFill"
    )
    val onFill = if (enabled) scheme.onTertiaryContainer else scheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .sticker(shape = MaterialTheme.shapes.large, fill = fill, depth = 4.dp)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PetStateBadge(enabled)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (enabled) "Mascota despierta" else "Mascota dormida",
                style = MaterialTheme.typography.titleSmall,
                color = onFill
            )
            Text(
                if (enabled) "Vigilando tus apps y tus límites"
                else "Tus límites no se aplican mientras duerme",
                style = MaterialTheme.typography.bodySmall,
                color = onFill.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(6.dp))
            if (enabled) {
                StatusBadge("Vigilando", DarmixIcons.Check, colors.available, scheme.onTertiary)
            } else {
                StatusBadge("En pausa", DarmixIcons.Moon, colors.cooldown, scheme.onSecondary)
            }
        }
        Spacer(Modifier.width(8.dp))
        GemSwitch(
            checked = enabled,
            onCheckedChange = { wantEnabled ->
                if (wantEnabled) PetController.wake(context) else confirmSleep = true
            },
            modifier = Modifier.semantics { contentDescription = "Mascota activa" }
        )
    }

    if (confirmSleep) {
        SleepConfirmDialog(
            onConfirm = {
                confirmSleep = false
                PetController.sleep(context)
            },
            onDismiss = { confirmSleep = false }
        )
    }
}


@Composable
private fun PetStateBadge(enabled: Boolean) {
    val scheme = MaterialTheme.colorScheme
    if (enabled) {
        HatBadge(size = 56.dp)
        return
    }
    val transition = rememberInfiniteTransition(label = "zzz")
    val tState = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "zzzT"
    )
    Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .sticker(shape = CircleShape, fill = scheme.surface, depth = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                DarmixIcons.Moon, contentDescription = null,
                tint = scheme.onSurfaceVariant, modifier = Modifier.size(30.dp)
            )
        }
        for (i in 0..1) {
            Text(
                "z",
                fontSize = (11 + 4 * i).sp,
                color = scheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .graphicsLayer {
                        val phase = (tState.value + i * 0.5f) % 1f
                        alpha = sin(PI.toFloat() * phase)
                        translationX = phase * 14.dp.toPx()
                        translationY = -phase * 16.dp.toPx()
                    }
            )
        }
    }
}



@Composable
private fun SleepConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val colors = DarmixTheme.colors

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val enter = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            enter.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow))
        }
        Column(
            modifier = Modifier
                .padding(start = 24.dp, end = 28.dp, top = 24.dp, bottom = 28.dp)
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    val s = 0.85f + 0.15f * enter.value
                    scaleX = s
                    scaleY = s
                    alpha = enter.value.coerceIn(0f, 1f)
                }
                .sticker(shape = RoundedCornerShape(30.dp), fill = scheme.surface, depth = 4.dp)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HatBadge(size = 72.dp)
            Spacer(Modifier.height(14.dp))
            Text(
                "¿Dormir a la mascota?",
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Mientras duerma, DarmixPet no vigilará el tiempo de tus apps ni las bloqueará. " +
                        "Puedes despertarla cuando quieras desde Ajustes.",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SquishButton(
                    text = "Cancelar",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    container = scheme.surface,
                    content = scheme.onSurface,
                    fillWidth = true
                )
                SquishButton(
                    text = "Dormir",
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    container = colors.blocked,
                    content = scheme.onError,
                    icon = DarmixIcons.Moon,
                    fillWidth = true
                )
            }
        }
    }
}




@Composable
fun PetSleepBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val colors = DarmixTheme.colors
    val enabled by rememberPetEnabled()

    AnimatedVisibility(
        visible = !enabled,
        enter = expandVertically(spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        Box(modifier = Modifier.padding(start = 20.dp, end = 24.dp, bottom = 14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .sticker(shape = MaterialTheme.shapes.large, fill = colors.cooldownContainer, depth = 3.dp)
                    .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(scheme.surface)
                        .border(2.dp, colors.ink, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        DarmixIcons.Moon, contentDescription = null,
                        tint = colors.onCooldownContainer, modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Mascota dormida",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onCooldownContainer
                    )
                    Text(
                        "Tus límites están en pausa",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onCooldownContainer.copy(alpha = 0.85f)
                    )
                }
                SquishButton(
                    text = "Despertar",
                    onClick = { PetController.wake(context) },
                    contentDescription = "Despertar a la mascota"
                )
            }
        }
    }
}
