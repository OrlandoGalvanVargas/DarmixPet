package com.darmix.darmixpet.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.darmix.darmixpet.monitor.AccessibilityPermissionHelper
import com.darmix.darmixpet.monitor.BatteryOptimizationHelper
import com.darmix.darmixpet.monitor.BrightnessPermissionHelper
import com.darmix.darmixpet.monitor.UsageStatsPermissionHelper
import com.darmix.darmixpet.overlay.NotificationPermissionHelper
import com.darmix.darmixpet.overlay.OverlayPermissionHelper
import com.darmix.darmixpet.ui.components.DotGridBackground
import com.darmix.darmixpet.ui.components.HatBadge
import com.darmix.darmixpet.ui.components.SquishButton
import com.darmix.darmixpet.ui.components.StatusBadge
import com.darmix.darmixpet.ui.components.sticker
import com.darmix.darmixpet.ui.icons.DarmixIcons
import com.darmix.darmixpet.ui.theme.DarmixTheme
import kotlinx.coroutines.delay



private enum class PermKey { RESTRICTED, OVERLAY, USAGE, NOTIFICATIONS, BATTERY, ACCESSIBILITY, WRITE_SETTINGS }

private data class PermItem(
    val key: PermKey,
    val icon: ImageVector,
    val title: String,
    val why: String,
    val granted: Boolean,
    val actionLabel: String = "Activar",
    val badge: String? = null
)

private enum class NoteKind { TIP, WARNING }
private data class GuideNote(val kind: NoteKind, val text: String)


private data class PermGuide(
    val icon: ImageVector,
    val title: String,
    val intro: String,
    val steps: List<String>,
    val notes: List<GuideNote>,
    val primaryLabel: String,
    val secondaryLabel: String? = null
)


private object RestrictedStepPrefs {
    private const val PREFS_NAME = "darmixpet_prefs"
    private const val KEY = "restricted_step_done"
    fun isDone(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY, false)
    fun setDone(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY, true).apply()
    }
}

private object PermissionIntents {
    fun appDetails(context: Context) = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")
    )

    fun appNotifications(context: Context) = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)


    fun usageAccess(context: Context) = Intent(
        Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:${context.packageName}")
    )
}

private fun safeStart(context: Context, primary: Intent, fallback: Intent? = null) {
    try {
        context.startActivity(primary)
    } catch (e: Exception) {
        try {
            if (fallback != null) context.startActivity(fallback)
            else Toast.makeText(context, "No pude abrir los ajustes. Ábrelos manualmente.", Toast.LENGTH_LONG).show()
        } catch (e2: Exception) {
            Toast.makeText(context, "No pude abrir los ajustes. Ábrelos manualmente.", Toast.LENGTH_LONG).show()
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}


private val TRUSTED_STORES = setOf(
    "com.android.vending",
    "com.sec.android.app.samsungapps",
    "com.amazon.venezia",
    "com.huawei.appmarket",
    "com.heytap.market",
    "com.oppo.market",
    "com.vivo.appstore",
    "com.xiaomi.mipicks"
)

private fun isInstalledFromTrustedStore(context: Context): Boolean {
    val installer = try {
        if (Build.VERSION.SDK_INT >= 30) {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getInstallerPackageName(context.packageName)
        }
    } catch (e: Exception) {
        null
    }
    return installer in TRUSTED_STORES
}



private const val RESTRICTED_WARNING =
    "¿El interruptor sale gris o ves el aviso «Ajuste restringido»? Completa antes el paso " +
            "«Permitir ajustes restringidos» (arriba en esta pantalla)."

private fun buildGuide(key: PermKey, restrictedNeeded: Boolean, notifBlocked: Boolean): PermGuide {
    val restrictedNote = if (restrictedNeeded) listOf(GuideNote(NoteKind.WARNING, RESTRICTED_WARNING)) else emptyList()
    return when (key) {
        PermKey.RESTRICTED -> PermGuide(
            icon = DarmixIcons.Lock,
            title = "Permitir ajustes restringidos",
            intro = "En Android 13 o superior, si instalaste DarmixPet fuera de Google Play, el teléfono puede " +
                    "bloquear algunos permisos hasta que tú lo autorices. Es una sola vez.",
            steps = listOf(
                "Se abrirá la pantalla «Información de la app» de DarmixPet.",
                "Toca el menú ⋮ (arriba a la derecha) y elige «Permitir ajustes restringidos». " +
                        "En algunos teléfonos esta opción está al final de la pantalla.",
                "Confirma con tu huella, PIN o patrón si te lo pide.",
                "Vuelve a DarmixPet y toca «Ya lo hice»."
            ),
            notes = listOf(
                GuideNote(
                    NoteKind.TIP,
                    "¿No ves la opción? Android a veces solo la muestra después de intentar activar un permiso " +
                            "bloqueado. Prueba primero con «Acceso a datos de uso»: saldrá un aviso, tócalo, " +
                            "acepta y regresa a la Información de la app."
                ),
                GuideNote(
                    NoteKind.TIP,
                    "No podemos comprobar si ya lo permitiste; por eso te pedimos que nos avises."
                )
            ),
            primaryLabel = "Abrir Info. de la app",
            secondaryLabel = "Ya lo hice"
        )

        PermKey.OVERLAY -> PermGuide(
            icon = DarmixIcons.Overlay,
            title = "Mostrar sobre otras apps",
            intro = "Permite que Archi aparezca flotando sobre tus apps.",
            steps = listOf(
                "Se abrirá la pantalla de DarmixPet.",
                "Activa «Permitir mostrar sobre otras apps».",
                "Vuelve a DarmixPet con el botón Atrás."
            ),
            notes = restrictedNote + GuideNote(
                NoteKind.TIP,
                "Después, Android puede mostrar un aviso «DarmixPet se muestra sobre otras apps». " +
                        "Es normal: lo pone el sistema, no la app, y no afecta nada."
            ),
            primaryLabel = "Abrir ajustes"
        )

        PermKey.USAGE -> PermGuide(
            icon = DarmixIcons.Hourglass,
            title = "Acceso a datos de uso",
            intro = "Permite a DarmixPet saber cuánto tiempo pasas en cada app para aplicar tus límites.",
            steps = listOf(
                "Se abrirá la pantalla de DarmixPet. Si ves una lista de apps, busca «DarmixPet» y tócalo.",
                "Activa «Permitir acceso al uso».",
                "Si Android muestra un aviso de seguridad, léelo y acepta. En algunos teléfonos el botón " +
                        "tarda unos segundos en habilitarse.",
                "Vuelve a DarmixPet."
            ),
            notes = restrictedNote,
            primaryLabel = "Abrir ajustes"
        )

        PermKey.NOTIFICATIONS -> PermGuide(
            icon = DarmixIcons.Bell,
            title = "Notificaciones",
            intro = "Sirven para avisarte cuando una app entra en descanso y para mostrar que Archi está activo.",
            steps = if (notifBlocked) listOf(
                "Como rechazaste el permiso antes, Android ya no muestra la ventana de aviso.",
                "Se abrirán los ajustes de notificaciones de DarmixPet.",
                "Activa «Permitir notificaciones» y vuelve a DarmixPet."
            ) else listOf(
                "Android mostrará una ventana: toca «Permitir».",
                "Si la rechazas, puedes volver a tocar «Activar» para intentarlo otra vez."
            ),
            notes = emptyList(),
            primaryLabel = if (notifBlocked) "Abrir ajustes" else "Continuar"
        )

        PermKey.BATTERY -> PermGuide(
            icon = DarmixIcons.Battery,
            title = "Ignorar optimización de batería",
            intro = "Para que Archi no se cierre solo por ahorro de batería. Si se cierra, dejas de estar protegido.",
            steps = listOf(
                "Se abrirá la configuración de batería de DarmixPet.",
                "Elige «Sin restricciones». Según tu teléfono puede llamarse «No optimizar» o «Permitir».",
                "Vuelve a DarmixPet."
            ),
            notes = listOf(
                GuideNote(
                    NoteKind.TIP,
                    "Normalmente viene en «Optimizado» o «Ahorro de batería»: ese es el que hay que cambiar. " +
                            "En Xiaomi, Huawei u otras marcas puede haber también «Inicio automático»; actívalo si lo ves."
                )
            ),
            primaryLabel = "Abrir ajustes"
        )

        PermKey.ACCESSIBILITY -> PermGuide(
            icon = DarmixIcons.Eye,
            title = "Detección instantánea",
            intro = "Permite saber al instante cuándo abres una app vigilada, para bloquearla a tiempo. " +
                    "DarmixPet solo lo usa para detectar qué app está abierta.",
            steps = listOf(
                "Se abrirá Accesibilidad.",
                "Entra a «Apps descargadas» (o «Servicios instalados» / «Apps instaladas») y toca DarmixPet.",
                "Activa «Usar DarmixPet».",
                "Android mostrará un aviso de seguridad sobre lo que la app podrá ver en pantalla. Acepta: " +
                        "en algunos teléfonos el botón tarda unos 10 segundos en habilitarse.",
                "Vuelve a DarmixPet."
            ),
            notes = restrictedNote,
            primaryLabel = "Abrir ajustes"
        )

        PermKey.WRITE_SETTINGS -> PermGuide(
            icon = DarmixIcons.Sun,
            title = "Modificar ajustes del sistema",
            intro = "Para ajustar el brillo desde el menú rápido de la mascota.",
            steps = listOf(
                "Se abrirá la pantalla de DarmixPet.",
                "Activa «Permitir modificar los ajustes del sistema».",
                "Vuelve a DarmixPet."
            ),
            notes = emptyList(),
            primaryLabel = "Abrir ajustes"
        )
    }
}



@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun PermissionsScreen(onAllGranted: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = remember(context) { context.findActivity() }

    var hasOverlay by remember { mutableStateOf(OverlayPermissionHelper.hasOverlayPermission(context)) }
    var hasUsageStats by remember { mutableStateOf(UsageStatsPermissionHelper.hasUsageStatsPermission(context)) }
    var hasNotifications by remember { mutableStateOf(NotificationPermissionHelper.hasNotificationPermission(context)) }
    var hasBatteryExemption by remember { mutableStateOf(BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)) }
    var hasAccessibility by remember { mutableStateOf(AccessibilityPermissionHelper.isAccessibilityServiceEnabled(context)) }
    var hasWriteSettings by remember { mutableStateOf(BrightnessPermissionHelper.hasWriteSettingsPermission(context)) }

    var restrictedDone by remember { mutableStateOf(RestrictedStepPrefs.isDone(context)) }
    var restrictedOpened by rememberSaveable { mutableStateOf(false) }
    var notifBlocked by remember { mutableStateOf(false) }
    var openGuide by remember { mutableStateOf<PermKey?>(null) }

    val fromStore = remember { isInstalledFromTrustedStore(context) }
    val restrictedPossible = Build.VERSION.SDK_INT >= 33 && !fromStore

    val restrictedProven = hasAccessibility || (Build.VERSION.SDK_INT >= 35 && hasUsageStats)
    val restrictedGranted = restrictedDone || restrictedProven
    val restrictedVisible = restrictedPossible && !(hasUsageStats && hasAccessibility)
    val restrictedNeeded = restrictedVisible && !restrictedGranted

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifications = granted

        notifBlocked = !granted && activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlay = OverlayPermissionHelper.hasOverlayPermission(context)
                hasUsageStats = UsageStatsPermissionHelper.hasUsageStatsPermission(context)
                hasNotifications = NotificationPermissionHelper.hasNotificationPermission(context)
                hasBatteryExemption = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
                hasAccessibility = AccessibilityPermissionHelper.isAccessibilityServiceEnabled(context)
                hasWriteSettings = BrightnessPermissionHelper.hasWriteSettingsPermission(context)
                restrictedDone = RestrictedStepPrefs.isDone(context)
                if (hasNotifications) notifBlocked = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(hasOverlay, hasUsageStats, hasNotifications, hasBatteryExemption, hasAccessibility, hasWriteSettings) {
        if (hasOverlay && hasUsageStats && hasNotifications && hasBatteryExemption && hasAccessibility && hasWriteSettings) {
            onAllGranted()
        }
    }

    fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            safeStart(context, PermissionIntents.appNotifications(context))
        }
    }

    fun launchFor(key: PermKey) {
        when (key) {
            PermKey.RESTRICTED -> {
                restrictedOpened = true
                safeStart(context, PermissionIntents.appDetails(context))
            }
            PermKey.OVERLAY -> safeStart(context, OverlayPermissionHelper.buildPermissionIntent(context))
            PermKey.USAGE -> safeStart(
                context, PermissionIntents.usageAccess(context), UsageStatsPermissionHelper.buildPermissionIntent()
            )
            PermKey.NOTIFICATIONS ->
                if (notifBlocked) safeStart(context, PermissionIntents.appNotifications(context))
                else requestNotifications()
            PermKey.BATTERY -> safeStart(context, BatteryOptimizationHelper.buildPermissionIntent(context))
            PermKey.ACCESSIBILITY -> safeStart(context, AccessibilityPermissionHelper.buildPermissionIntent())
            PermKey.WRITE_SETTINGS -> safeStart(context, BrightnessPermissionHelper.buildPermissionIntent(context))
        }
    }

    fun markRestrictedDone() {
        RestrictedStepPrefs.setDone(context)
        restrictedDone = true
    }

    val restrictedItem = PermItem(
        key = PermKey.RESTRICTED,
        icon = DarmixIcons.Lock,
        title = "Permitir ajustes restringidos",
        why = "Android bloquea algunos permisos en apps instaladas fuera de la tienda. Esto los desbloquea.",
        granted = restrictedGranted,
        actionLabel = if (restrictedOpened) "Ya lo hice" else "Activar",
        badge = "Paso previo"
    )

    val items = listOf(
        PermItem(PermKey.OVERLAY, DarmixIcons.Overlay, "Mostrar sobre otras apps",
            "Para que Archi pueda pasear sobre tus apps.", hasOverlay),
        PermItem(PermKey.USAGE, DarmixIcons.Hourglass, "Acceso a datos de uso",
            "Para saber cuánto tiempo pasas en cada app.", hasUsageStats),
        PermItem(PermKey.NOTIFICATIONS, DarmixIcons.Bell, "Notificaciones",
            "Para avisarte cuando una app entra en descanso.", hasNotifications,
            actionLabel = if (notifBlocked) "Abrir ajustes" else "Activar"),
        PermItem(PermKey.BATTERY, DarmixIcons.Battery, "Ignorar optimización de batería",
            "Para que Archi no desaparezca por ahorro de batería.", hasBatteryExemption),
        PermItem(PermKey.ACCESSIBILITY, DarmixIcons.Eye, "Detección instantánea",
            "Para saber al instante cuándo abres una app vigilada.", hasAccessibility),
        PermItem(PermKey.WRITE_SETTINGS, DarmixIcons.Sun, "Modificar ajustes del sistema",
            "Para ajustar el brillo desde el menú rápido.", hasWriteSettings)
    )

    val grantedCount = items.count { it.granted }
    val firstPending = (if (restrictedNeeded) listOf(restrictedItem) else emptyList<PermItem>())
        .plus(items).firstOrNull { !it.granted }?.key

    DotGridBackground(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HatBadge(size = 68.dp)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        "Antes de empezar",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "DarmixPet necesita estos permisos para cuidar tu tiempo de pantalla. " +
                                "Te mostramos qué hacer en cada uno.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            ProgressCard(done = grantedCount, total = items.size)
            Spacer(Modifier.height(20.dp))

            var index = 0
            if (restrictedVisible) {
                PermissionCard(
                    item = restrictedItem,
                    index = index++,
                    highlight = firstPending == PermKey.RESTRICTED,
                    onActivate = {
                        if (restrictedOpened) markRestrictedDone() else openGuide = PermKey.RESTRICTED
                    }
                )
                Spacer(Modifier.height(14.dp))
            }
            items.forEach { item ->
                PermissionCard(
                    item = item,
                    index = index++,
                    highlight = firstPending == item.key,
                    onActivate = { openGuide = item.key }
                )
                Spacer(Modifier.height(14.dp))
            }

            FooterNote()
        }
    }

    openGuide?.let { key ->
        val guide = buildGuide(key, restrictedNeeded, notifBlocked)
        PermissionGuideDialog(
            guide = guide,
            onDismiss = { openGuide = null },
            onPrimary = {
                openGuide = null
                launchFor(key)
            },
            onSecondary = if (key == PermKey.RESTRICTED) {
                { markRestrictedDone(); openGuide = null }
            } else null
        )
    }
}



@Composable
private fun PermissionGuideDialog(
    guide: PermGuide,
    onDismiss: () -> Unit,
    onPrimary: () -> Unit,
    onSecondary: (() -> Unit)?
) {
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
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(scheme.primaryContainer)
                        .border(2.dp, colors.ink, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(guide.icon, null, tint = scheme.onPrimaryContainer, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Antes de abrir ajustes",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant
                    )
                    Text(guide.title, style = MaterialTheme.typography.titleLarge, color = scheme.onSurface)
                }
            }

            Spacer(Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(guide.intro, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
                Spacer(Modifier.height(14.dp))
                guide.steps.forEachIndexed { i, step ->
                    StepRow(number = i + 1, text = step)
                    Spacer(Modifier.height(10.dp))
                }
                guide.notes.forEach { note ->
                    NoteBox(note)
                    Spacer(Modifier.height(10.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SquishButton(
                    text = if (onSecondary != null) guide.secondaryLabel ?: "Ya lo hice" else "Ahora no",
                    onClick = { if (onSecondary != null) onSecondary() else onDismiss() },
                    modifier = Modifier.weight(1f),
                    container = scheme.surface,
                    content = scheme.onSurface,
                    fillWidth = true
                )
                SquishButton(
                    text = guide.primaryLabel,
                    onClick = onPrimary,
                    modifier = Modifier.weight(1.4f),
                    fillWidth = true
                )
            }
        }
    }
}

@Composable
private fun StepRow(number: Int, text: String) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(scheme.primary)
                .border(2.dp, DarmixTheme.colors.ink, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("$number", style = MaterialTheme.typography.labelLarge, color = scheme.onPrimary)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(top = 3.dp)
        )
    }
}

@Composable
private fun NoteBox(note: GuideNote) {
    val scheme = MaterialTheme.colorScheme
    val colors = DarmixTheme.colors
    val warning = note.kind == NoteKind.WARNING
    val fill = if (warning) colors.cooldownContainer else scheme.primaryContainer
    val onFill = if (warning) colors.onCooldownContainer else scheme.onPrimaryContainer
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sticker(
                shape = RoundedCornerShape(18.dp),
                fill = fill,
                depth = 0.dp,
                borderWidth = 1.5.dp
            )
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            if (warning) DarmixIcons.Lock else DarmixIcons.Sparkle,
            contentDescription = null,
            tint = onFill,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(note.text, style = MaterialTheme.typography.bodySmall, color = onFill)
    }
}



@Composable
private fun ProgressCard(done: Int, total: Int) {
    val colors = DarmixTheme.colors
    val message = when {
        done == 0 -> "¡Empecemos!"
        done == total -> "¡Todo listo!"
        done == total - 1 -> "¡Solo falta uno!"
        else -> "¡Vas muy bien!"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .sticker(fill = MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Archi dice",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(message, style = MaterialTheme.typography.titleMedium)
            }
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.goldContainer)
                    .border(2.dp, colors.ink, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(DarmixIcons.Sparkle, null, tint = colors.onGoldContainer, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("$done/$total", style = MaterialTheme.typography.labelLarge, color = colors.onGoldContainer)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(total) { i -> Gem(filled = i < done, modifier = Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun Gem(filled: Boolean, modifier: Modifier = Modifier) {
    val colors = DarmixTheme.colors
    val fill by animateColorAsState(
        targetValue = if (filled) colors.gold else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(350),
        label = "gemFill"
    )
    val scale by animateFloatAsState(
        targetValue = if (filled) 1f else 0.92f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "gemScale"
    )
    Box(
        modifier = modifier
            .height(14.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(fill)
            .border(2.dp, colors.ink, CircleShape)
    )
}

@Composable
private fun PermissionCard(item: PermItem, index: Int, highlight: Boolean, onActivate: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val colors = DarmixTheme.colors

    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 70L)
        appear.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow))
    }

    val depth by animateDpAsState(
        targetValue = if (item.granted) 0.dp else 3.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "cardDepth"
    )
    val pendingFill = if (item.key == PermKey.RESTRICTED) colors.goldContainer else scheme.surface
    val fill by animateColorAsState(
        targetValue = if (item.granted) scheme.tertiaryContainer else pendingFill,
        animationSpec = tween(300),
        label = "cardFill"
    )

    val bubbleTarget = if (item.granted) scheme.tertiary else when {
        item.key == PermKey.RESTRICTED -> colors.gold
        index % 3 == 0 -> scheme.primaryContainer
        index % 3 == 1 -> scheme.secondaryContainer
        else -> colors.goldContainer
    }
    val glyphTarget = if (item.granted) scheme.onTertiary else when {
        item.key == PermKey.RESTRICTED -> colors.onGold
        index % 3 == 0 -> scheme.onPrimaryContainer
        index % 3 == 1 -> scheme.onSecondaryContainer
        else -> colors.onGoldContainer
    }
    val bubble by animateColorAsState(bubbleTarget, tween(300), label = "bubble")
    val glyph by animateColorAsState(glyphTarget, tween(300), label = "glyph")
    val onFill = if (item.granted) scheme.onTertiaryContainer
    else if (item.key == PermKey.RESTRICTED) colors.onGoldContainer else scheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = appear.value.coerceIn(0f, 1f)
                translationY = (1f - appear.value) * 48.dp.toPx()
            }
            .animateContentSize()
            .sticker(fill = fill, depth = depth)
            .padding(14.dp)
    ) {
        if (item.badge != null && !item.granted) {
            StatusBadge(item.badge, DarmixIcons.Sparkle, colors.gold, colors.onGold)
            Spacer(Modifier.height(10.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(bubble)
                    .border(2.dp, colors.ink, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.icon, contentDescription = null, tint = glyph, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall, color = onFill)
                Text(
                    item.why,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.granted) scheme.onTertiaryContainer else scheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            AnimatedContent(
                targetState = item.granted,
                transitionSpec = {
                    (scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + fadeIn()) togetherWith
                            (scaleOut() + fadeOut())
                },
                label = "trailing"
            ) { granted ->
                if (granted) {
                    CheckStamp()
                } else {
                    SquishButton(
                        text = item.actionLabel,
                        onClick = onActivate,
                        contentDescription = "${item.actionLabel}: ${item.title}"
                    )
                }
            }
        }
        if (highlight && !item.granted) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(DarmixIcons.Sparkle, null, tint = scheme.secondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Te mostramos los pasos antes de abrir los ajustes.",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CheckStamp() {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.tertiary)
            .border(2.dp, DarmixTheme.colors.ink, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            DarmixIcons.Check, null,
            tint = MaterialTheme.colorScheme.onTertiary,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun FooterNote() {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(scheme.surfaceVariant)
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(DarmixIcons.Bell, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "Es normal que Android muestre un aviso como «DarmixPet se muestra sobre otras apps». " +
                    "Lo pone el sistema, no la app, y no afecta nada. Puedes ocultarlo desde Ajustes → Ayuda.",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant
        )
    }
}
