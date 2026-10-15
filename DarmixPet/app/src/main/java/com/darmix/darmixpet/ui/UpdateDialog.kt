package com.darmix.darmixpet.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.darmix.darmixpet.ui.components.HatBadge
import com.darmix.darmixpet.ui.components.SquishButton
import com.darmix.darmixpet.ui.components.StatusBadge
import com.darmix.darmixpet.ui.components.sticker
import com.darmix.darmixpet.ui.icons.DarmixIcons
import com.darmix.darmixpet.ui.theme.DarmixTheme
import com.darmix.darmixpet.update.ApkDownloader
import com.darmix.darmixpet.update.UpdateChecker
import kotlinx.coroutines.launch

private sealed interface NoteLine {
    data class Heading(val text: String) : NoteLine
    data class Bullet(val text: String) : NoteLine
    data class Paragraph(val text: String) : NoteLine
}

private fun parseReleaseNotes(raw: String): List<NoteLine> {
    val result = mutableListOf<NoteLine>()
    val imageRegex = Regex("""!\[[^\]]*\]\([^)]*\)""")
    val linkRegex = Regex("""\[([^\]]+)\]\([^)]*\)""")
    val numberedRegex = Regex("""^\d+[.)]\s+""")
    val ruleRegex = Regex("""^[-*_]{3,}$""")

    for (rawLine in raw.replace("\r\n", "\n").lines()) {
        var line = rawLine.trim()
        if (line.isEmpty() || line.startsWith("<!--") || ruleRegex.matches(line)) continue
        line = line.replace(imageRegex, "")
            .replace(linkRegex, "\$1")
            .replace("**", "").replace("__", "").replace("`", "")
            .trim()
        if (line.isEmpty()) continue

        result += when {
            line.startsWith("#") -> NoteLine.Heading(line.trimStart('#').trim())
            line.startsWith("- ") || line.startsWith("* ") || line.startsWith("+ ") ->
                NoteLine.Bullet(line.drop(2).trim())
            numberedRegex.containsMatchIn(line) -> NoteLine.Bullet(line.replaceFirst(numberedRegex, ""))
            else -> NoteLine.Paragraph(line)
        }
        if (result.size >= 40) break
    }
    return result
}

@Composable
fun UpdateDialog(
    latestVersion: String,
    currentVersion: String,
    releaseNotes: String,
    onUpdateClick: () -> Unit,
    onDismiss: () -> Unit,
    downloadState: ApkDownloader.State = ApkDownloader.State.Idle,
    onInstallClick: () -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme
    val colors = DarmixTheme.colors
    val notes = remember(releaseNotes) { parseReleaseNotes(releaseNotes) }
    val context = LocalContext.current
    val enter = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    val handleDismiss = {
        coroutineScope.launch {
            enter.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium))
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        enter.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow))
    }
    Dialog(
        onDismissRequest = { handleDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .padding(start = 20.dp, end = 24.dp, top = 24.dp, bottom = 28.dp)
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .heightIn(max = 700.dp)
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
            Spacer(Modifier.height(12.dp))
            Text(
                "¡Nueva versión disponible!",
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            StatusBadge(
                text = "v$currentVersion  →  v$latestVersion",
                icon = DarmixIcons.Sparkle,
                container = colors.gold,
                content = colors.onGold
            )

            Spacer(Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .sticker(
                        shape = RoundedCornerShape(20.dp),
                        fill = scheme.surfaceVariant,
                        depth = 0.dp,
                        borderWidth = 1.5.dp
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Novedades", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                if (notes.isEmpty()) {
                    Text(
                        "Esta versión trae mejoras y correcciones.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurface
                    )
                } else {
                    notes.forEach { line ->
                        when (line) {
                            is NoteLine.Heading -> Text(
                                line.text,
                                style = MaterialTheme.typography.titleSmall,
                                color = scheme.onSurface
                            )
                            is NoteLine.Bullet -> Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    DarmixIcons.Sparkle, contentDescription = null,
                                    tint = scheme.secondary,
                                    modifier = Modifier.padding(top = 3.dp).size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    line.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = scheme.onSurface
                                )
                            }
                            is NoteLine.Paragraph -> Text(
                                line.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurface
                            )
                        }
                    }
                }
            }

            if (downloadState !is ApkDownloader.State.Idle) {
                Spacer(Modifier.height(14.dp))
                DownloadStatus(downloadState)
            }

            Spacer(Modifier.height(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "¿No se descarga o tienes problemas?",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Ir a la web",
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onPrimaryContainer,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Abrir la página oficial de DarmixPet"
                        ) { UpdateChecker.openUrl(context, UpdateChecker.OFFICIAL_PAGE_URL) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SquishButton(
                    text = if (downloadState is ApkDownloader.State.Downloading) "Ocultar" else "Más tarde",
                    onClick = { handleDismiss() },
                    modifier = Modifier.weight(1f),
                    container = scheme.surface,
                    content = scheme.onSurface,
                    fillWidth = true
                )
                when (downloadState) {
                    is ApkDownloader.State.Downloading -> SquishButton(
                        text = downloadState.progress?.let { "${(it * 100).toInt()}%" } ?: "Descargando…",
                        onClick = {},
                        modifier = Modifier.weight(1.3f),
                        icon = DarmixIcons.Download,
                        fillWidth = true
                    )

                    is ApkDownloader.State.Ready -> SquishButton(
                        text = "Instalar",
                        onClick = onInstallClick,
                        modifier = Modifier.weight(1.3f),
                        icon = DarmixIcons.Sparkle,
                        fillWidth = true
                    )

                    is ApkDownloader.State.Failed -> SquishButton(
                        text = "Reintentar",
                        onClick = onUpdateClick,
                        modifier = Modifier.weight(1.3f),
                        icon = DarmixIcons.Download,
                        fillWidth = true
                    )

                    ApkDownloader.State.Idle -> SquishButton(
                        text = "Descargar",
                        onClick = onUpdateClick,
                        modifier = Modifier.weight(1.3f),
                        icon = DarmixIcons.Download,
                        fillWidth = true
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadStatus(state: ApkDownloader.State) {
    val scheme = MaterialTheme.colorScheme
    val (title, detail) = when (state) {
        is ApkDownloader.State.Downloading -> "Descargando actualización…" to
                "Puedes ocultar este aviso, la descarga sigue en segundo plano."
        is ApkDownloader.State.Ready -> "¡Descarga lista!" to
                "Toca Instalar y confirma en la pantalla de Android."
        is ApkDownloader.State.Failed -> "No se pudo descargar" to
                "Revisa tu conexión e inténtalo de nuevo."
        ApkDownloader.State.Idle -> return
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
        when (state) {
            is ApkDownloader.State.Downloading -> {
                val barModifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                val progress = state.progress
                if (progress != null) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = barModifier,
                        color = scheme.primary,
                        trackColor = scheme.surfaceVariant
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = barModifier,
                        color = scheme.primary,
                        trackColor = scheme.surfaceVariant
                    )
                }
            }
            is ApkDownloader.State.Ready -> LinearProgressIndicator(
                progress = { 1f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = scheme.primary,
                trackColor = scheme.surfaceVariant
            )
            else -> Unit
        }
        Text(detail, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
    }
}
