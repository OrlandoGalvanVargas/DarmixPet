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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import com.darmix.darmixpet.notifications.MessageAlertApp
import com.darmix.darmixpet.notifications.MessageAlertApps
import com.darmix.darmixpet.tts.MessageAlertPreferences
import com.darmix.darmixpet.ui.components.SquishButton

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
        targetValue = if (enabled) scheme.primaryContainer else scheme.surfaceVariant,
        animationSpec = tween(300),
        label = "voiceCardFill"
    )
    val onFill = if (enabled) scheme.onPrimaryContainer else scheme.onSurface
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
                color = onFill
            )
            Text(
                if (enabled) "Te avisa en voz alta de la batería baja, la noche, tus límites de tiempo y tus mensajes"
                else "Seguirá avisándote con animaciones y mensajes, pero sin hablar",
                style = MaterialTheme.typography.bodySmall,
                color = onFill.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(6.dp))
            if (enabled) {
                StatusBadge("Con voz", DarmixIcons.Speaker, scheme.primary, scheme.onPrimary)
            } else {
                StatusBadge("Silenciada", DarmixIcons.SpeakerOff, scheme.onSurfaceVariant, scheme.surface)
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

private data class MessageAlertState(
    val apps: Set<String>,
    val includeSender: Boolean,
    val showBubble: Boolean
)

private fun readMessageAlertState(context: android.content.Context) = MessageAlertState(
    MessageAlertPreferences.enabledApps(context),
    MessageAlertPreferences.includeSender(context),
    MessageAlertPreferences.showBubble(context)
)

@Composable
private fun rememberMessageAlertState(): State<MessageAlertState> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(readMessageAlertState(context)) }
    DisposableEffect(context) {
        val keys = setOf(
            MessageAlertPreferences.KEY_APPS,
            MessageAlertPreferences.KEY_SENDER,
            MessageAlertPreferences.KEY_BUBBLE
        )
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key in keys) state.value = readMessageAlertState(context)
        }
        MessageAlertPreferences.registerListener(context, listener)
        onDispose { MessageAlertPreferences.unregisterListener(context, listener) }
    }
    return state
}

@Composable
fun MessageAlertsCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val voiceEnabled by rememberVoiceEnabled()
    val alertState by rememberMessageAlertState()
    val installed = remember { MessageAlertApps.installed(context) }
    var showDialog by remember { mutableStateOf(false) }

    val activeNames = installed.filter { it.packageName in alertState.apps }.map { it.label }
    val summary = when {
        activeNames.isEmpty() -> "Desactivados"
        activeNames.size == 1 -> activeNames.first()
        else -> "${activeNames.first()} y ${activeNames.size - 1} más"
    }

    AnimatedVisibility(
        visible = voiceEnabled,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .sticker(shape = MaterialTheme.shapes.large, fill = scheme.surface, depth = 3.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClickLabel = "Elegir apps"
                ) { showDialog = true }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(DarmixIcons.Mail, null, tint = scheme.onSurface, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Avisos de mensajes", style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                Text(summary, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            Icon(
                DarmixIcons.ChevronRight, null,
                tint = scheme.onSurfaceVariant, modifier = Modifier.size(22.dp)
            )
        }
    }

    if (showDialog) {
        MessageAlertsDialog(
            installed = installed,
            state = alertState,
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun MessageAlertsDialog(
    installed: List<MessageAlertApp>,
    state: MessageAlertState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .padding(start = 20.dp, end = 24.dp, top = 24.dp, bottom = 28.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .sticker(shape = RoundedCornerShape(30.dp), fill = scheme.surface, depth = 4.dp)
                .padding(20.dp)
        ) {
            Text("Avisos de mensajes", style = MaterialTheme.typography.titleLarge, color = scheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Text(
                "Tu mascota te avisará en voz alta cuando llegue un mensaje de estas apps.",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (installed.isEmpty()) {
                    Text(
                        "No encontré apps de mensajes compatibles en tu teléfono.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                }
                installed.forEach { app ->
                    MessageAppRow(
                        app = app,
                        checked = app.packageName in state.apps,
                        onCheckedChange = { MessageAlertPreferences.setAppEnabled(context, app.packageName, it) }
                    )
                }

                Spacer(Modifier.height(4.dp))
                AlertOptionRow(
                    title = "Decir quién lo envía",
                    description = "Por privacidad viene apagado: cualquiera cerca podría escuchar el nombre.",
                    checked = state.includeSender,
                    onCheckedChange = { MessageAlertPreferences.setIncludeSender(context, it) },
                    accessibilityLabel = "Decir quién envía el mensaje"
                )
                AlertOptionRow(
                    title = "Mostrar el aviso en el globito",
                    description = "Además de decirlo en voz alta, tu mascota lo muestra escrito junto a ella.",
                    checked = state.showBubble,
                    onCheckedChange = { MessageAlertPreferences.setShowBubble(context, it) },
                    accessibilityLabel = "Mostrar el aviso en el globito de la mascota"
                )
            }

            Spacer(Modifier.height(16.dp))
            SquishButton(
                text = "Listo",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                icon = DarmixIcons.Check,
                fillWidth = true
            )
        }
    }
}

@Composable
private fun MessageAppRow(app: MessageAlertApp, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val icon: ImageBitmap? = remember(app.packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(app.packageName).toBitmap(96, 96).asImageBitmap()
        }.getOrNull()
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .sticker(
                    shape = RoundedCornerShape(14.dp),
                    fill = scheme.surfaceVariant,
                    depth = 0.dp,
                    borderWidth = 1.5.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            app.label,
            style = MaterialTheme.typography.titleSmall,
            color = scheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        GemSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.semantics { contentDescription = "Avisos de ${app.label}" }
        )
    }
}

@Composable
private fun AlertOptionRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accessibilityLabel: String
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sticker(
                shape = RoundedCornerShape(20.dp),
                fill = scheme.surfaceVariant,
                depth = 0.dp,
                borderWidth = 1.5.dp
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
            Text(description, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        GemSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.semantics { contentDescription = accessibilityLabel }
        )
    }
}