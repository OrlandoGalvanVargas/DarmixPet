package com.darmix.darmixpet.ui

import android.content.SharedPreferences
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.darmix.darmixpet.tts.VoicePreferences
import com.darmix.darmixpet.ui.components.GemSwitch
import com.darmix.darmixpet.ui.components.StatusBadge
import com.darmix.darmixpet.ui.components.sticker
import com.darmix.darmixpet.ui.icons.DarmixIcons
import com.darmix.darmixpet.ui.theme.DarmixTheme

@Composable
fun rememberVoiceEnabled(): State<Boolean> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(VoicePreferences.isEnabled(context)) }
    DisposableEffect(context) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == VoicePreferences.KEY_VOICE_ENABLED) state.value = VoicePreferences.isEnabled(context)
        }
        VoicePreferences.registerListener(context, listener)
        onDispose { VoicePreferences.unregisterListener(context, listener) }
    }
    return state
}

@Composable
fun VoiceCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val colors = DarmixTheme.colors
    val enabled by rememberVoiceEnabled()

    val fill by animateColorAsState(
        targetValue = if (enabled) scheme.surface else scheme.surfaceVariant,
        animationSpec = tween(300),
        label = "voiceCardFill"
    )
    val bubbleFill by animateColorAsState(
        targetValue = if (enabled) scheme.primaryContainer else scheme.surface,
        animationSpec = tween(300),
        label = "voiceBubbleFill"
    )
    val iconTint = if (enabled) scheme.onPrimaryContainer else scheme.onSurfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .sticker(shape = MaterialTheme.shapes.large, fill = fill, depth = 4.dp)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .sticker(shape = CircleShape, fill = bubbleFill, depth = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (enabled) DarmixIcons.Speaker else DarmixIcons.SpeakerOff,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Voz de la mascota",
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface
            )
            Text(
                if (enabled) "Te avisa en voz alta de la batería baja, la noche y tus límites de tiempo"
                else "Seguirá avisándote con animaciones y mensajes, pero sin hablar",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurface.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(6.dp))
            if (enabled) {
                StatusBadge("Con voz", DarmixIcons.Speaker, colors.available, scheme.onTertiary)
            } else {
                StatusBadge("Silenciada", DarmixIcons.SpeakerOff, colors.cooldown, scheme.onSecondary)
            }
        }
        Spacer(Modifier.width(8.dp))
        GemSwitch(
            checked = enabled,
            onCheckedChange = { VoicePreferences.setEnabled(context, it) },
            modifier = Modifier.semantics { contentDescription = "Voz de la mascota" }
        )
    }
}