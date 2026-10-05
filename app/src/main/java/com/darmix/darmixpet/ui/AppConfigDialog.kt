package com.darmix.darmixpet.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.darmix.darmixpet.ui.components.HatBadge
import com.darmix.darmixpet.ui.components.SquishButton
import com.darmix.darmixpet.ui.components.SquishIconButton
import com.darmix.darmixpet.ui.components.sticker
import com.darmix.darmixpet.ui.icons.DarmixIcons
import com.darmix.darmixpet.ui.theme.DarmixTheme

@Composable
fun AppConfigDialog(
    appName: String,
    initialSessionMinutes: Int = 30,
    initialCooldownMinutes: Int = 60,
    initialMaxSessions: Int = 3,
    appIcon: ImageBitmap? = null,
    onDismiss: () -> Unit,
    onConfirm: (sessionMinutes: Int, cooldownMinutes: Int, maxSessions: Int) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val colors = DarmixTheme.colors

    var sessionText by remember { mutableStateOf(initialSessionMinutes.toString()) }
    var cooldownText by remember { mutableStateOf(initialCooldownMinutes.toString()) }
    var maxSessionsText by remember { mutableStateOf(initialMaxSessions.toString()) }

    val session = sessionText.toIntOrNull()?.takeIf { it > 0 }
    val cooldown = cooldownText.toIntOrNull()?.takeIf { it > 0 }
    val maxSessions = maxSessionsText.toIntOrNull()?.takeIf { it > 0 }
    val isValid = session != null && cooldown != null && maxSessions != null

    val summary = if (isValid) {
        "Podrás usarla ${durationText(session!!)} por sesión, hasta $maxSessions " +
                "${if (maxSessions == 1) "vez" else "veces"} al día, con ${durationText(cooldown!!)} " +
                "de descanso entre sesiones."
    } else {
        "Completa los tres valores para poder guardar."
    }

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
                .padding(start = 20.dp, end = 24.dp, top = 24.dp, bottom = 28.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .graphicsLayer {
                    val s = 0.85f + 0.15f * enter.value
                    scaleX = s
                    scaleY = s
                    alpha = enter.value.coerceIn(0f, 1f)
                }
                .sticker(shape = RoundedCornerShape(30.dp), fill = scheme.surface, depth = 4.dp)
                .padding(20.dp)
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (appIcon != null) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .sticker(
                                shape = RoundedCornerShape(16.dp),
                                fill = scheme.surfaceVariant,
                                depth = 0.dp,
                                borderWidth = 1.5.dp
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(bitmap = appIcon, contentDescription = null, modifier = Modifier.size(34.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Configurar",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant
                    )
                    Text(
                        appName,
                        style = MaterialTheme.typography.titleLarge,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(16.dp))


            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ConfigSection(
                    icon = DarmixIcons.Clock,
                    label = "Tiempo por sesión",
                    hint = "Cuánto puedes usarla de corrido",
                    unit = "min",
                    value = sessionText,
                    onValueChange = { sessionText = it },
                    step = 5,
                    maxValue = 999,
                    presets = listOf(15, 30, 45, 60),
                    presetLabel = ::minutesShort,
                    accent = scheme.primaryContainer,
                    onAccent = scheme.onPrimaryContainer
                )
                ConfigSection(
                    icon = DarmixIcons.Snowflake,
                    label = "Enfriamiento",
                    hint = "Descanso obligatorio entre sesiones",
                    unit = "min",
                    value = cooldownText,
                    onValueChange = { cooldownText = it },
                    step = 5,
                    maxValue = 999,
                    presets = listOf(15, 30, 60, 120),
                    presetLabel = ::minutesShort,
                    accent = scheme.secondaryContainer,
                    onAccent = scheme.onSecondaryContainer
                )
                ConfigSection(
                    icon = DarmixIcons.Ticket,
                    label = "Sesiones al día",
                    hint = "Cuántas veces al día puedes abrirla",
                    unit = "por día",
                    value = maxSessionsText,
                    onValueChange = { maxSessionsText = it },
                    step = 1,
                    maxValue = 99,
                    presets = listOf(1, 2, 3, 5),
                    presetLabel = { it.toString() },
                    accent = scheme.tertiaryContainer,
                    onAccent = scheme.onTertiaryContainer
                )
            }

            Spacer(Modifier.height(16.dp))


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .sticker(
                        shape = RoundedCornerShape(22.dp),
                        fill = colors.goldContainer,
                        depth = 0.dp,
                        borderWidth = 1.5.dp
                    )
                    .padding(start = 12.dp, top = 12.dp, bottom = 16.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HatBadge(size = 44.dp)
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.animateContentSize()) {
                    Text(
                        "Archi dice",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onGoldContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onGoldContainer
                    )
                }
            }

            Spacer(Modifier.height(16.dp))


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
                    text = "Guardar",
                    onClick = {
                        onConfirm(
                            sessionText.toIntOrNull() ?: initialSessionMinutes,
                            cooldownText.toIntOrNull() ?: initialCooldownMinutes,
                            maxSessionsText.toIntOrNull() ?: initialMaxSessions
                        )
                    },
                    modifier = Modifier.weight(1f),
                    icon = DarmixIcons.Check,
                    enabled = isValid,
                    fillWidth = true
                )
            }
        }
    }
}

/** Un ajuste: encabezado, [ − valor + ] y atajos rápidos. El valor también se puede escribir. */
@Composable
private fun ConfigSection(
    icon: ImageVector,
    label: String,
    hint: String,
    unit: String,
    value: String,
    onValueChange: (String) -> Unit,
    step: Int,
    maxValue: Int,
    presets: List<Int>,
    presetLabel: (Int) -> String,
    accent: Color,
    onAccent: Color
) {
    val scheme = MaterialTheme.colorScheme
    val colors = DarmixTheme.colors
    val current = value.toIntOrNull()
    fun setValue(v: Int) = onValueChange(v.coerceIn(1, maxValue).toString())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .sticker(
                shape = RoundedCornerShape(22.dp),
                fill = accent,
                depth = 0.dp,
                borderWidth = 1.5.dp
            )
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(scheme.surface)
                    .border(2.dp, colors.ink, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = onAccent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(label, style = MaterialTheme.typography.titleSmall, color = onAccent)
                Text(
                    hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = onAccent.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SquishIconButton(
                icon = DarmixIcons.Minus,
                contentDescription = "Disminuir $label",
                onClick = { setValue((current ?: 0) - step) },
                enabled = (current ?: 0) > 1,
                container = scheme.surface,
                content = onAccent,
                repeatWhileHeld = true
            )
            ValueField(
                value = value,
                onValueChange = { onValueChange(it.filter { c -> c.isDigit() }.take(maxValue.toString().length)) },
                unit = unit,
                label = label
            )
            SquishIconButton(
                icon = DarmixIcons.Plus,
                contentDescription = "Aumentar $label",
                onClick = { setValue((current ?: 0) + step) },
                enabled = (current ?: 0) < maxValue,
                container = scheme.surface,
                content = onAccent,
                repeatWhileHeld = true
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                PresetChip(
                    text = presetLabel(preset),
                    selected = current == preset,
                    accent = accent,
                    onAccent = onAccent,
                    modifier = Modifier.weight(1f),
                    onClick = { setValue(preset) }
                )
            }
        }
    }
}

@Composable
private fun ValueField(
    value: String,
    onValueChange: (String) -> Unit,
    unit: String,
    label: String
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val depth by animateDpAsState(
        targetValue = if (focused) 3.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "fieldDepth"
    )

    Box(
        modifier = Modifier
            .size(width = 92.dp, height = 62.dp)
            .sticker(
                shape = RoundedCornerShape(18.dp),
                fill = scheme.surface,
                depth = depth,
                shadow = scheme.primary
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    color = scheme.onSurface,
                    textAlign = TextAlign.Center
                ),
                cursorBrush = SolidColor(scheme.primary),
                interactionSource = interaction,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier
                    .width(64.dp)
                    .semantics { contentDescription = label }
            )
            Text(
                unit,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PresetChip(
    text: String,
    selected: Boolean,
    accent: Color,
    onAccent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val fill by animateColorAsState(if (selected) onAccent else scheme.surface, tween(200), label = "chipFill")
    val label by animateColorAsState(if (selected) accent else scheme.onSurface, tween(200), label = "chipText")

    Box(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(CircleShape)
            .background(fill)
            .border(1.5.dp, DarmixTheme.colors.ink, CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = label)
    }
}

private fun minutesShort(min: Int): String =
    if (min >= 60 && min % 60 == 0) "${min / 60}h" else "${min}m"

private fun durationText(min: Int): String = when {
    min < 60 -> "$min min"
    min % 60 == 0 -> "${min / 60} h"
    else -> "${min / 60} h ${min % 60} min"
}
