package com.darmix.darmixpet.ui

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.darmix.darmixpet.data.AppDatabase
import com.darmix.darmixpet.data.InstalledAppInfo
import com.darmix.darmixpet.data.InstalledAppsProvider
import com.darmix.darmixpet.data.MonitoredAppEntity
import com.darmix.darmixpet.ui.components.GemSwitch
import com.darmix.darmixpet.ui.components.HatBadge
import com.darmix.darmixpet.ui.components.StatusBadge
import com.darmix.darmixpet.ui.components.sticker
import com.darmix.darmixpet.ui.icons.DarmixIcons
import com.darmix.darmixpet.ui.theme.DarmixTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class AppFilter { ALL, WATCHED }


private enum class AppStatus { IDLE, AVAILABLE, IN_SESSION, NO_USES, COOLDOWN }

private fun MonitoredAppEntity.hasActiveSession(): Boolean = usedInSessionMillis > 0L


private fun MonitoredAppEntity.cooldownRemainingMs(now: Long): Long =
    if (lastBlockedTimestamp > 0L)
        (lastBlockedTimestamp + cooldownMinutes * 60_000L - now).coerceAtLeast(0L)
    else 0L

private fun MonitoredAppEntity.status(now: Long): AppStatus = when {
    hasActiveSession() -> AppStatus.IN_SESSION
    sessionsUsedToday >= maxSessionsPerDay -> AppStatus.NO_USES
    cooldownRemainingMs(now) > 0L -> AppStatus.COOLDOWN
    else -> AppStatus.AVAILABLE
}

private fun formatRemaining(ms: Long): String {
    val totalSeconds = (ms + 999) / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val sec = totalSeconds % 60
    return if (h > 0) "${h}h ${m}m" else "%d:%02d".format(m, sec)
}

@Composable
fun AppListScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { AppDatabase.getInstance(context).monitoredAppDao() }

    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val monitoredApps by dao.getAllMonitoredApps().collectAsState(initial = emptyList())

    var appPendingConfig by remember { mutableStateOf<InstalledAppInfo?>(null) }
    var existingConfigForDialog by remember { mutableStateOf<MonitoredAppEntity?>(null) }

    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(AppFilter.ALL) }

    val seenPackages = remember { mutableSetOf<String>() }

    LaunchedEffect(Unit) {
        installedApps = withContext(Dispatchers.IO) {
            InstalledAppsProvider.getLaunchableApps(context)
        }
        isLoading = false
    }

    val monitoredMap = remember(monitoredApps) { monitoredApps.associateBy { it.packageName } }
    val visibleApps = remember(installedApps, monitoredMap, query, filter) {
        val q = query.trim()
        installedApps.filter { app ->
            (filter == AppFilter.ALL || monitoredMap.containsKey(app.packageName)) &&
                    (q.isEmpty() || app.appName.contains(q, ignoreCase = true))
        }
    }

    fun notifyLocked(appName: String) {
        Toast.makeText(
            context,
            "No puedes modificar \"$appName\" mientras tiene una sesión activa.",
            Toast.LENGTH_SHORT
        ).show()
    }

    Column(modifier = modifier.fillMaxSize()) {
        ListHeader(watched = monitoredApps.size)

        PetSleepBanner()

        SearchField(
            query = query,
            onQueryChange = { query = it },
            modifier = Modifier.padding(start = 20.dp, end = 23.dp)
        )

        Row(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilterPill("Todas", installedApps.size, filter == AppFilter.ALL) { filter = AppFilter.ALL }
            FilterPill("Vigiladas", monitoredApps.size, filter == AppFilter.WATCHED) { filter = AppFilter.WATCHED }
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                isLoading -> CenteredMessage(
                    title = "Archi está buscando tus apps…",
                    body = "Un momentito"
                )

                visibleApps.isEmpty() -> {
                    val (title, body) = when {
                        query.isNotBlank() -> "No encontré esa app" to "Prueba con otro nombre"
                        filter == AppFilter.WATCHED ->
                            "Aún no vigilas ninguna app" to "Activa el interruptor de una app para que Archi la cuide"
                        else -> "No hay apps para mostrar" to "Vuelve a intentarlo en un momento"
                    }
                    CenteredMessage(title, body)
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 20.dp, top = 6.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(visibleApps, key = { _, app -> app.packageName }) { index, app ->
                        val monitored = monitoredMap[app.packageName]
                        val playEntrance = remember { seenPackages.add(app.packageName) }

                        AppRow(
                            app = app,
                            monitored = monitored,
                            index = index,
                            playEntrance = playEntrance,
                            onRowClick = {
                                if (monitored == null) return@AppRow
                                if (monitored.hasActiveSession()) notifyLocked(app.appName)
                                else {
                                    appPendingConfig = app
                                    existingConfigForDialog = monitored
                                }
                            },
                            onSwitchChange = { checked ->
                                if (checked) {
                                    appPendingConfig = app
                                    existingConfigForDialog = null
                                } else {
                                    scope.launch { dao.deleteByPackageName(app.packageName) }
                                }
                            },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }

    appPendingConfig?.let { app ->
        val existing = existingConfigForDialog
        AppConfigDialog(
            appName = app.appName,
            appIcon = app.icon,
            initialSessionMinutes = existing?.sessionLimitMinutes ?: 30,
            initialCooldownMinutes = existing?.cooldownMinutes ?: 60,
            initialMaxSessions = existing?.maxSessionsPerDay ?: 3,
            onDismiss = {
                appPendingConfig = null
                existingConfigForDialog = null
            },
            onConfirm = { sessionMinutes, cooldownMinutes, maxSessions ->
                scope.launch {
                    dao.upsert(
                        (existing ?: MonitoredAppEntity(
                            packageName = app.packageName,
                            appName = app.appName,
                            sessionLimitMinutes = sessionMinutes,
                            cooldownMinutes = cooldownMinutes,
                            maxSessionsPerDay = maxSessions
                        )).copy(
                            sessionLimitMinutes = sessionMinutes,
                            cooldownMinutes = cooldownMinutes,
                            maxSessionsPerDay = maxSessions
                        )
                    )
                }
                appPendingConfig = null
                existingConfigForDialog = null
            }
        )
    }
}



@Composable
private fun ListHeader(watched: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Tus apps",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                if (watched == 0) "Ninguna app vigilada todavía"
                else "$watched app${if (watched == 1) "" else "s"} bajo vigilancia de Archi",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (watched > 0) WatchBadge(watched)
    }
}


@Composable
private fun WatchBadge(count: Int) {
    val colors = DarmixTheme.colors
    val pop = remember { Animatable(1f) }
    LaunchedEffect(count) {
        pop.snapTo(1.25f)
        pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium))
    }
    Row(
        modifier = Modifier
            .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
            .sticker(shape = CircleShape, fill = colors.goldContainer, depth = 3.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .semantics { contentDescription = "$count apps vigiladas" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(DarmixIcons.Eye, null, tint = colors.onGoldContainer, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text("$count", style = MaterialTheme.typography.titleSmall, color = colors.onGoldContainer)
    }
}



@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val colors = DarmixTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val focusManager = LocalFocusManager.current
    val shadow by animateColorAsState(
        targetValue = if (focused) scheme.primary else colors.shadow,
        animationSpec = tween(200),
        label = "searchShadow"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .sticker(shape = RoundedCornerShape(20.dp), depth = 3.dp, shadow = shadow)
            .heightIn(min = 52.dp)
            .padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(DarmixIcons.Search, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(
                    "Buscar una app…",
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                cursorBrush = SolidColor(scheme.primary),
                interactionSource = interaction,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClickLabel = "Borrar búsqueda") { onQueryChange("") },
                contentAlignment = Alignment.Center
            ) {
                Icon(DarmixIcons.Close, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
    }
}


@Composable
private fun FilterPill(text: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val spec = spring<androidx.compose.ui.unit.Dp>(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium)
    val fill by animateColorAsState(if (selected) scheme.primary else scheme.surface, tween(200), label = "pillFill")
    val content by animateColorAsState(if (selected) scheme.onPrimary else scheme.onSurface, tween(200), label = "pillContent")
    val depth by animateDpAsState(if (selected) 0.dp else 3.dp, spec, label = "pillDepth")
    val shift by animateDpAsState(if (selected) 3.dp else 0.dp, spec, label = "pillShift")

    Box(
        modifier = Modifier
            .padding(end = 3.dp, bottom = 3.dp)
            .selectable(
                selected = selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .offset(x = shift, y = shift)
                .sticker(shape = CircleShape, fill = fill, depth = depth)
                .heightIn(min = 44.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text, style = MaterialTheme.typography.labelLarge, color = content)
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(content.copy(alpha = 0.18f))
                    .padding(horizontal = 8.dp, vertical = 1.dp)
            ) {
                Text("$count", style = MaterialTheme.typography.labelSmall, color = content)
            }
        }
    }
}



@Composable
private fun AppRow(
    app: InstalledAppInfo,
    monitored: MonitoredAppEntity?,
    index: Int,
    playEntrance: Boolean,
    onRowClick: () -> Unit,
    onSwitchChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val colors = DarmixTheme.colors

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val inCooldownWindow = monitored != null && monitored.cooldownRemainingMs(now) > 0L
    LaunchedEffect(inCooldownWindow, monitored?.lastBlockedTimestamp) {
        if (inCooldownWindow) {
            now = System.currentTimeMillis()
            while (true) {
                delay(1000)
                now = System.currentTimeMillis()
            }
        }
    }
    val status = monitored?.status(now) ?: AppStatus.IDLE
    val isActive = monitored != null
    val locked = status == AppStatus.IN_SESSION


    val appear = remember { Animatable(if (playEntrance) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (playEntrance) {
            delay(minOf(index, 8) * 45L)
            appear.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow))
        }
    }


    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "rowPress"
    )

    val fill by animateColorAsState(
        targetValue = when (status) {
            AppStatus.IDLE -> scheme.surface.copy(alpha = 0.55f)
            AppStatus.AVAILABLE -> scheme.surface
            AppStatus.IN_SESSION -> colors.goldContainer
            AppStatus.NO_USES -> colors.blockedContainer
            AppStatus.COOLDOWN -> colors.cooldownContainer
        },
        animationSpec = tween(300),
        label = "rowFill"
    )
    val shape = MaterialTheme.shapes.large

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = appear.value.coerceIn(0f, 1f)
                translationY = (1f - appear.value) * 40.dp.toPx()
                scaleX = pressScale
                scaleY = pressScale
            }
            .then(
                if (isActive) Modifier.sticker(shape = shape, fill = fill, depth = 3.dp)
                else Modifier.dashedCard(shape, fill, scheme.onSurfaceVariant.copy(alpha = 0.45f))
            )
            .clickable(
                enabled = isActive,
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onRowClick
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIconTile(app, isActive)
        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                app.appName,
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (monitored == null) {
                Text(
                    "Sin vigilar",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            } else {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatChip(
                        DarmixIcons.Clock, "${monitored.sessionLimitMinutes}m",
                        scheme.primaryContainer, scheme.onPrimaryContainer
                    )
                    StatChip(
                        DarmixIcons.Snowflake, "${monitored.cooldownMinutes}m",
                        scheme.secondaryContainer, scheme.onSecondaryContainer
                    )
                    if (status == AppStatus.NO_USES) {
                        StatChip(
                            DarmixIcons.Ticket, "${monitored.sessionsUsedToday}/${monitored.maxSessionsPerDay}",
                            colors.blocked, scheme.onError
                        )
                    } else {
                        StatChip(
                            DarmixIcons.Ticket, "${monitored.sessionsUsedToday}/${monitored.maxSessionsPerDay}",
                            scheme.tertiaryContainer, scheme.onTertiaryContainer
                        )
                    }
                }
                when (status) {
                    AppStatus.IN_SESSION -> {
                        Spacer(Modifier.height(6.dp))
                        StatusBadge("En sesión", DarmixIcons.Lock, colors.gold, colors.onGold)
                    }
                    AppStatus.NO_USES -> {
                        Spacer(Modifier.height(6.dp))
                        StatusBadge("Sin usos hoy", DarmixIcons.Ticket, colors.blocked, scheme.onError)
                    }
                    AppStatus.COOLDOWN -> {
                        Spacer(Modifier.height(6.dp))
                        StatusBadge(
                            "Enfriando ${formatRemaining(monitored.cooldownRemainingMs(now))}",
                            DarmixIcons.Snowflake, colors.cooldown, scheme.onSecondary
                        )
                    }
                    else -> Unit
                }
            }
        }

        Spacer(Modifier.width(8.dp))
        GemSwitch(
            checked = isActive,
            enabled = !locked,
            onCheckedChange = onSwitchChange,
            modifier = Modifier.semantics { contentDescription = "Vigilar ${app.appName}" }
        )
    }
}

@Composable
private fun AppIconTile(app: InstalledAppInfo, isActive: Boolean) {

    val filter = remember(isActive) {
        if (isActive) null
        else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.4f) })
    }
    Box(
        modifier = Modifier
            .size(52.dp)
            .sticker(
                shape = RoundedCornerShape(16.dp),
                fill = MaterialTheme.colorScheme.surfaceVariant,
                depth = 0.dp,
                borderWidth = 1.5.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = app.icon,
            contentDescription = null,
            colorFilter = filter,
            modifier = Modifier.size(34.dp)
        )
    }
}

@Composable
private fun StatChip(icon: ImageVector, text: String, container: Color, content: Color) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(container)
            .padding(start = 6.dp, end = 8.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(3.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = content)
    }
}


private fun Modifier.dashedCard(shape: Shape, fill: Color, stroke: Color): Modifier = this
    .background(fill, shape)
    .drawBehind {
        val outline = shape.createOutline(size, layoutDirection, this)
        drawOutline(
            outline = outline,
            color = stroke,
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f))
            )
        )
    }



@Composable
private fun CenteredMessage(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        HatBadge(size = 84.dp)
        Spacer(Modifier.height(20.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
