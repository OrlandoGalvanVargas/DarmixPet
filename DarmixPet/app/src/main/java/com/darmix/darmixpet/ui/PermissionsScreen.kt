package com.darmix.darmixpet.ui

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.darmix.darmixpet.monitor.AccessibilityPermissionHelper
import com.darmix.darmixpet.monitor.AppWatcherAccessibilityService
import com.darmix.darmixpet.monitor.BatteryOptimizationHelper
import com.darmix.darmixpet.monitor.BrightnessPermissionHelper
import com.darmix.darmixpet.monitor.UsageStatsPermissionHelper
import com.darmix.darmixpet.notifications.NotificationAccessHelper
import com.darmix.darmixpet.overlay.NotificationPermissionHelper
import com.darmix.darmixpet.overlay.OverlayPermissionHelper
import com.darmix.darmixpet.ui.components.DotGridBackground
import com.darmix.darmixpet.ui.components.HatBadge
import com.darmix.darmixpet.ui.components.SquishButton
import com.darmix.darmixpet.ui.components.StatusBadge
import com.darmix.darmixpet.ui.components.sticker
import com.darmix.darmixpet.ui.icons.DarmixIcons
import com.darmix.darmixpet.ui.theme.DarmixTheme
import com.darmix.darmixpet.ui.theme.rememberSelectedSkinId

private enum class PermKey { ACCESSIBILITY, NOTIFICATION_ACCESS, USAGE, OVERLAY, NOTIFICATIONS, BATTERY, WRITE_SETTINGS }

private val PERMISSION_ORDER = listOf(
    PermKey.ACCESSIBILITY,
    PermKey.NOTIFICATION_ACCESS,
    PermKey.USAGE,
    PermKey.OVERLAY,
    PermKey.NOTIFICATIONS,
    PermKey.BATTERY,
    PermKey.WRITE_SETTINGS
)

private data class StepContent(
    val icon: ImageVector,
    val systemName: String,
    val title: String,
    val why: String,
    val steps: List<String>,
    val note: String? = null,
    val badge: String? = null,
    val actionLabel: String = "Activar",
    val secondaryLabel: String? = null
)

private object RestrictedStepPrefs {
    private const val PREFS_NAME = "darmixpet_prefs"
    private const val KEY = "restricted_step_done"
    private const val KEY_BLOCKED = "restricted_step_blocked"
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    fun isDone(context: Context) = prefs(context).getBoolean(KEY, false)
    fun setDone(context: Context) {
        prefs(context).edit().putBoolean(KEY, true).apply()
    }
    fun clearDone(context: Context) {
        prefs(context).edit().putBoolean(KEY, false).apply()
    }
    fun wasBlocked(context: Context) = prefs(context).getBoolean(KEY_BLOCKED, false)
    fun setBlocked(context: Context) {
        prefs(context).edit().putBoolean(KEY_BLOCKED, true).apply()
    }
}

private enum class RestrictedSettingsStyle { MENU, SWITCH }

private enum class PhoneFamily { SAMSUNG, XIAOMI, OPPO_GROUP, HUAWEI, HONOR, VIVO, OTHER }

private fun systemProperty(name: String): String = try {
    val clazz = Class.forName("android.os.SystemProperties")
    clazz.getMethod("get", String::class.java).invoke(null, name) as? String ?: ""
} catch (e: Exception) {
    ""
}

private fun phoneFamily(): PhoneFamily {
    val names = listOf(Build.MANUFACTURER, Build.BRAND).map { it.lowercase() }
    fun any(vararg values: String) = names.any { it in values }
    return when {
        any("samsung") -> PhoneFamily.SAMSUNG
        any("xiaomi", "redmi", "poco") -> PhoneFamily.XIAOMI
        any("oppo", "realme", "oneplus") -> PhoneFamily.OPPO_GROUP
        any("huawei") -> PhoneFamily.HUAWEI
        any("honor") -> PhoneFamily.HONOR
        any("vivo", "iqoo") -> PhoneFamily.VIVO
        else -> PhoneFamily.OTHER
    }
}

private fun isHyperOs(): Boolean = systemProperty("ro.mi.os.version.name").isNotBlank()

private fun restrictedSettingsStyle(family: PhoneFamily): RestrictedSettingsStyle =
    if (family == PhoneFamily.XIAOMI && isHyperOs() && Build.VERSION.SDK_INT >= 35) RestrictedSettingsStyle.SWITCH
    else RestrictedSettingsStyle.MENU

private fun showsAutostart(family: PhoneFamily): Boolean = family in setOf(
    PhoneFamily.XIAOMI, PhoneFamily.OPPO_GROUP, PhoneFamily.HUAWEI, PhoneFamily.HONOR, PhoneFamily.VIVO
)

private object PermissionIntents {
    private const val ACCESSIBILITY_DETAILS = "android.settings.ACCESSIBILITY_DETAILS_SETTINGS"

    fun freshTask(intent: Intent): Intent = intent.addFlags(
        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    )

    fun appDetails(context: Context) = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")
    )

    fun restrictedAppDetails(context: Context) = freshTask(appDetails(context))

    fun restrictedUnlock(context: Context, route: UnlockRoute): List<Intent> = when (route) {
        UnlockRoute.APP_INFO -> listOf(restrictedAppDetails(context))
        UnlockRoute.APP_LIST -> listOf(
            freshTask(Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)),
            freshTask(Intent(Settings.ACTION_APPLICATION_SETTINGS)),
            restrictedAppDetails(context)
        )
    }

    fun accessibility(context: Context, direct: Boolean): List<Intent> = buildList {
        if (direct && Build.VERSION.SDK_INT >= 33) {
            add(
                Intent(ACCESSIBILITY_DETAILS).putExtra(
                    Intent.EXTRA_COMPONENT_NAME,
                    ComponentName(context, AppWatcherAccessibilityService::class.java).flattenToString()
                )
            )
        }
        add(AccessibilityPermissionHelper.buildPermissionIntent())
    }

    fun notificationAccess(context: Context): List<Intent> =
        NotificationAccessHelper.buildPermissionIntents(context)

    fun usageAccess(context: Context): List<Intent> = listOf(
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:${context.packageName}")),
        UsageStatsPermissionHelper.buildPermissionIntent()
    )

    fun overlay(context: Context): List<Intent> = listOf(
        OverlayPermissionHelper.buildPermissionIntent(context),
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION),
        appDetails(context)
    )

    fun appNotifications(context: Context): List<Intent> = listOf(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
        appDetails(context)
    )

    fun battery(context: Context): List<Intent> = listOf(
        BatteryOptimizationHelper.buildPermissionIntent(context),
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
        appDetails(context)
    )

    fun writeSettings(context: Context): List<Intent> = listOf(
        BrightnessPermissionHelper.buildPermissionIntent(context),
        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS),
        appDetails(context)
    )
}

private fun safeStart(context: Context, intents: List<Intent>) {
    for (intent in intents) {
        try {
            context.startActivity(intent)
            return
        } catch (e: Exception) {
        }
    }
    Toast.makeText(context, "No pude abrir los ajustes. Ábrelos manualmente.", Toast.LENGTH_LONG).show()
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
    if (installer in TRUSTED_STORES) return true
    if (Build.VERSION.SDK_INT >= 33) {
        val source = try {
            context.packageManager.getInstallSourceInfo(context.packageName).packageSource
        } catch (e: Exception) {
            PackageInstaller.PACKAGE_SOURCE_UNSPECIFIED
        }
        if (source == PackageInstaller.PACKAGE_SOURCE_STORE) return true
    }
    return false
}

private fun accessibilityListHint(family: PhoneFamily): String = when (family) {
    PhoneFamily.SAMSUNG -> "Baja casi hasta el final y toca «Apps instaladas»."
    PhoneFamily.XIAOMI -> "Baja hasta el final y toca «Apps descargadas»."
    PhoneFamily.OPPO_GROUP -> "Baja hasta encontrar «Apps descargadas» y tócalo."
    PhoneFamily.HUAWEI -> "Baja hasta encontrar «Servicios instalados» y tócalo."
    PhoneFamily.HONOR, PhoneFamily.VIVO -> "Baja hasta encontrar «Servicios descargados»."
    PhoneFamily.OTHER -> "Baja hasta la sección «Apps descargadas»."
}

private enum class UnlockRoute { APP_INFO, APP_LIST }

private fun unlockRoute(family: PhoneFamily): UnlockRoute =
    if (family == PhoneFamily.OPPO_GROUP) UnlockRoute.APP_LIST else UnlockRoute.APP_INFO

private fun unlockContent(style: RestrictedSettingsStyle, route: UnlockRoute): StepContent {
    val allowStep = when (style) {
        RestrictedSettingsStyle.SWITCH -> listOf(
            "Baja hasta el final de la pantalla.",
            "Activa «Permitir ajustes restringidos» y confirma con tu huella o PIN."
        )
        RestrictedSettingsStyle.MENU -> listOf(
            "Toca los tres puntos «⋮» de arriba a la derecha.",
            "Elige «Permitir ajustes restringidos» y confirma con tu huella o PIN."
        )
    }
    val steps = buildList {
        if (route == UnlockRoute.APP_LIST) add("Busca «DarmixPet» en la lista de apps y tócalo.")
        addAll(allowStep)
        add("Vuelve aquí y toca «Ya lo hice».")
    }
    val note = when {
        style == RestrictedSettingsStyle.SWITCH -> null
        route == UnlockRoute.APP_LIST -> null
        else -> "¿No ves los tres puntos? Busca la opción al final de la pantalla o entra desde Ajustes → Apps → DarmixPet."
    }
    return StepContent(
        icon = DarmixIcons.Lock,
        systemName = "Ajustes restringidos",
        title = "Desbloquea DarmixPet",
        why = "Android bloquea este permiso en las apps que no vienen de una tienda. Solo hay que desbloquearlo una vez.",
        steps = steps,
        note = note,
        badge = "Bloqueado por Android",
        actionLabel = if (route == UnlockRoute.APP_LIST) "Abrir lista de apps" else "Abrir info de la app",
        secondaryLabel = "Ya lo hice"
    )
}

private fun stepContent(
    key: PermKey,
    petName: String,
    family: PhoneFamily,
    accessibilityDirect: Boolean,
    probingRestricted: Boolean,
    notifBlocked: Boolean
): StepContent = when (key) {
    PermKey.ACCESSIBILITY -> StepContent(
        icon = DarmixIcons.Eye,
        systemName = "Accesibilidad",
        title = "Detección instantánea",
        why = "Así $petName sabe en el momento qué app abriste y puede frenarla a tiempo.",
        steps = when {
            accessibilityDirect -> listOf(
                "Activa el interruptor de «DarmixPet».",
                "Toca «Permitir» en el aviso. A veces el botón tarda unos segundos en activarse.",
                "Vuelve a DarmixPet."
            )
            probingRestricted -> listOf(
                accessibilityListHint(family),
                "Toca «DarmixPet» e intenta activarlo.",
                "Si Android dice que es un «ajuste restringido», acepta el aviso y vuelve aquí. Te diremos cómo desbloquearlo."
            )
            else -> listOf(
                accessibilityListHint(family),
                "Toca «DarmixPet», activa el interruptor y toca «Permitir».",
                "Vuelve a DarmixPet."
            )
        },
        note = if (accessibilityDirect) {
            "¿Se abrió la pantalla general de Accesibilidad? ${accessibilityListHint(family)}"
        } else {
            "DarmixPet solo lo usa para saber qué app está abierta."
        }
    )

    PermKey.NOTIFICATION_ACCESS -> StepContent(
        icon = DarmixIcons.Mail,
        systemName = "Acceso a notificaciones",
        title = "Ver tus notificaciones",
        why = "Para que $petName te enseñe tus notificaciones con un toque, sin bajar la barra.",
        steps = if (Build.VERSION.SDK_INT >= 30) listOf(
            "Activa el interruptor.",
            "Toca «Permitir» en el aviso.",
            "Vuelve a DarmixPet."
        ) else listOf(
            "Busca «DarmixPet» en la lista y activa su interruptor.",
            "Toca «Permitir» en el aviso.",
            "Vuelve a DarmixPet."
        ),
        note = "Solo te las muestra. No las guarda ni las manda a ningún lado."
    )

    PermKey.USAGE -> StepContent(
        icon = DarmixIcons.Hourglass,
        systemName = "Acceso a datos de uso",
        title = "Tiempo en cada app",
        why = "Para medir cuánto tiempo pasas en las apps que vigilas.",
        steps = listOf(
            "Si ves una lista de apps, busca «DarmixPet» y tócalo.",
            "Activa el interruptor.",
            "Vuelve a DarmixPet."
        )
    )

    PermKey.OVERLAY -> StepContent(
        icon = DarmixIcons.Overlay,
        systemName = "Mostrar sobre otras apps",
        title = "Flotar sobre tus apps",
        why = "Para que $petName pueda aparecer encima de las apps que estés usando.",
        steps = if (Build.VERSION.SDK_INT >= 30) listOf(
            "Busca «DarmixPet» en la lista de apps y tócalo.",
            "Activa el interruptor.",
            "Vuelve a DarmixPet."
        ) else listOf(
            "Activa el interruptor.",
            "Vuelve a DarmixPet."
        ),
        note = if (Build.VERSION.SDK_INT >= 30) {
            "Android ya no deja abrir esta opción directo, por eso primero aparece la lista."
        } else null
    )

    PermKey.NOTIFICATIONS -> {
        val runtime = Build.VERSION.SDK_INT >= 33 && !notifBlocked
        StepContent(
            icon = DarmixIcons.Bell,
            systemName = "Notificaciones",
            title = "Avisos de $petName",
            why = "Para avisarte cuando una app entra en descanso.",
            steps = if (runtime) listOf(
                "Toca «Permitir» en la ventana que va a aparecer."
            ) else listOf(
                "Activa el interruptor de las notificaciones.",
                "Vuelve a DarmixPet."
            ),
            note = if (notifBlocked) "Como antes elegiste no permitirlas, ahora se activan desde ajustes." else null,
            actionLabel = if (runtime) "Permitir" else "Abrir ajustes"
        )
    }

    PermKey.BATTERY -> StepContent(
        icon = DarmixIcons.Battery,
        systemName = "Optimización de batería",
        title = "Siempre activo",
        why = "Para que el ahorro de batería no cierre a $petName.",
        steps = listOf(
            "Toca «Permitir» en la ventana que va a aparecer.",
            "Si en su lugar se abre una lista, busca «DarmixPet» y elige la opción sin restricciones."
        ),
        note = if (showsAutostart(family)) {
            "En tu teléfono también puede haber «Inicio automático» en la info de DarmixPet. Si lo ves, actívalo."
        } else null,
        actionLabel = "Permitir"
    )

    PermKey.WRITE_SETTINGS -> StepContent(
        icon = DarmixIcons.Sun,
        systemName = "Modificar ajustes del sistema",
        title = "Control de brillo",
        why = "Para que puedas subir o bajar el brillo desde el menú rápido de $petName.",
        steps = listOf(
            "Activa el interruptor.",
            "Vuelve a DarmixPet."
        )
    )
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun PermissionsScreen(onAllGranted: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = remember(context) { context.findActivity() }

    val skinId by rememberSelectedSkinId()
    val petName = SkinLore.forId(skinId).shortName

    var hasOverlay by remember { mutableStateOf(OverlayPermissionHelper.hasOverlayPermission(context)) }
    var hasUsageStats by remember { mutableStateOf(UsageStatsPermissionHelper.hasUsageStatsPermission(context)) }
    var hasNotifications by remember { mutableStateOf(NotificationPermissionHelper.hasNotificationPermission(context)) }
    var hasBatteryExemption by remember { mutableStateOf(BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)) }
    var hasAccessibility by remember { mutableStateOf(AccessibilityPermissionHelper.isAccessibilityServiceEnabled(context)) }
    var hasWriteSettings by remember { mutableStateOf(BrightnessPermissionHelper.hasWriteSettingsPermission(context)) }
    var hasNotificationAccess by remember { mutableStateOf(NotificationAccessHelper.isNotificationAccessGranted(context)) }

    var restrictedDone by remember { mutableStateOf(RestrictedStepPrefs.isDone(context)) }
    var restrictedBlocked by remember { mutableStateOf(RestrictedStepPrefs.wasBlocked(context)) }
    var awaitingRestrictedProbe by rememberSaveable { mutableStateOf(false) }
    var notifBlocked by remember { mutableStateOf(false) }

    val family = remember { phoneFamily() }
    val restrictedStyle = remember { restrictedSettingsStyle(family) }
    val restrictedRoute = remember { unlockRoute(family) }
    val fromStore = remember { isInstalledFromTrustedStore(context) }
    val restrictedPossible = Build.VERSION.SDK_INT >= 33 && !fromStore

    val restrictedProven = hasAccessibility || hasNotificationAccess
    val restrictedGranted = restrictedDone || restrictedProven
    val showUnlock = restrictedPossible && restrictedBlocked && !restrictedGranted
    val accessibilityDirect = !restrictedPossible || restrictedGranted
    val probingRestricted = restrictedPossible && !restrictedGranted

    val grantedMap = mapOf(
        PermKey.ACCESSIBILITY to hasAccessibility,
        PermKey.NOTIFICATION_ACCESS to hasNotificationAccess,
        PermKey.USAGE to hasUsageStats,
        PermKey.OVERLAY to hasOverlay,
        PermKey.NOTIFICATIONS to hasNotifications,
        PermKey.BATTERY to hasBatteryExemption,
        PermKey.WRITE_SETTINGS to hasWriteSettings
    )
    val current = PERMISSION_ORDER.firstOrNull { grantedMap[it] != true }
    val grantedCount = PERMISSION_ORDER.count { grantedMap[it] == true }

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
                hasNotificationAccess = NotificationAccessHelper.isNotificationAccessGranted(context)
                restrictedDone = RestrictedStepPrefs.isDone(context)
                if (hasNotifications) notifBlocked = false
                if (awaitingRestrictedProbe) {
                    awaitingRestrictedProbe = false
                    if (!hasAccessibility && !hasNotificationAccess) {
                        RestrictedStepPrefs.setBlocked(context)
                        RestrictedStepPrefs.clearDone(context)
                        restrictedBlocked = true
                        restrictedDone = false
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showIntro by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(current) {
        if (current == null) {
            SetupProgressPrefs.markCompleted(context)
            onAllGranted()
        }
    }

    fun launchFor(key: PermKey) {
        val probesRestricted = key == PermKey.ACCESSIBILITY || key == PermKey.NOTIFICATION_ACCESS
        if (probesRestricted && restrictedPossible && !restrictedProven) awaitingRestrictedProbe = true
        when (key) {
            PermKey.ACCESSIBILITY -> safeStart(context, PermissionIntents.accessibility(context, accessibilityDirect))
            PermKey.NOTIFICATION_ACCESS -> safeStart(context, PermissionIntents.notificationAccess(context))
            PermKey.USAGE -> safeStart(context, PermissionIntents.usageAccess(context))
            PermKey.OVERLAY -> safeStart(context, PermissionIntents.overlay(context))
            PermKey.NOTIFICATIONS ->
                if (Build.VERSION.SDK_INT >= 33 && !notifBlocked) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    safeStart(context, PermissionIntents.appNotifications(context))
                }
            PermKey.BATTERY -> safeStart(context, PermissionIntents.battery(context))
            PermKey.WRITE_SETTINGS -> safeStart(context, PermissionIntents.writeSettings(context))
        }
    }

    fun markRestrictedDone() {
        RestrictedStepPrefs.setDone(context)
        restrictedDone = true
    }

    val unlocking = current == PermKey.ACCESSIBILITY && showUnlock
    val stepIndex = current?.let { PERMISSION_ORDER.indexOf(it) } ?: (PERMISSION_ORDER.size - 1)

    DotGridBackground(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HatBadge(size = 52.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "Antes de empezar",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "Paso ${stepIndex + 1} de ${PERMISSION_ORDER.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            StepGems(done = grantedCount, total = PERMISSION_ORDER.size, currentIndex = stepIndex)
            Spacer(Modifier.height(20.dp))

            if (current != null) {
                AnimatedContent(
                    targetState = current to unlocking,
                    transitionSpec = {
                        (slideInHorizontally(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)) { it / 2 } + fadeIn(tween(220))) togetherWith
                                (slideOutHorizontally(tween(220)) { -it / 2 } + fadeOut(tween(180)))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    label = "permissionStep"
                ) { (key, unlock) ->
                    val content = if (unlock) unlockContent(restrictedStyle, restrictedRoute)
                    else stepContent(key, petName, family, accessibilityDirect, probingRestricted, notifBlocked)
                    StepCard(
                        content = content,
                        onPrimary = {
                            if (unlock) safeStart(context, PermissionIntents.restrictedUnlock(context, restrictedRoute))
                            else launchFor(key)
                        },
                        onSecondary = if (unlock) ({ markRestrictedDone() }) else null
                    )
                }
            }
        }
    }

    if (showIntro && current != null) {
        PermissionsIntroDialog(
            granted = grantedCount,
            total = PERMISSION_ORDER.size,
            onStart = { showIntro = false }
        )
    }
}

@Composable
private fun StepGems(done: Int, total: Int, currentIndex: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i -> Gem(filled = i < done, active = i == currentIndex, modifier = Modifier.weight(1f)) }
    }
}

@Composable
private fun Gem(filled: Boolean, active: Boolean, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val fill by animateColorAsState(
        targetValue = when {
            filled -> scheme.primary
            active -> scheme.primaryContainer
            else -> scheme.surfaceVariant
        },
        animationSpec = tween(350),
        label = "gemFill"
    )
    val scale by animateFloatAsState(
        targetValue = if (filled || active) 1f else 0.9f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "gemScale"
    )
    Box(
        modifier = modifier
            .height(if (active) 14.dp else 12.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(fill)
            .border(2.dp, DarmixTheme.colors.ink, CircleShape)
    )
}

@Composable
private fun StepCard(content: StepContent, onPrimary: () -> Unit, onSecondary: (() -> Unit)?) {
    val scheme = MaterialTheme.colorScheme
    val colors = DarmixTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .sticker(fill = scheme.surface)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (content.badge != null) {
                StatusBadge(content.badge, DarmixIcons.Lock, colors.blocked, scheme.onPrimary)
                Spacer(Modifier.height(14.dp))
            }
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(scheme.primaryContainer)
                    .border(2.5.dp, colors.ink, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(content.icon, null, tint = scheme.onPrimaryContainer, modifier = Modifier.size(42.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "En Ajustes: ${content.systemName}",
                style = MaterialTheme.typography.labelMedium,
                color = scheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(2.dp))
            Text(
                content.title,
                style = MaterialTheme.typography.headlineSmall,
                color = scheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                highlighted(content.why),
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(22.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                content.steps.forEachIndexed { i, step ->
                    StepRow(number = i + 1, text = step)
                    Spacer(Modifier.height(12.dp))
                }
            }
            if (content.note != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    highlighted(content.note),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (onSecondary != null && content.secondaryLabel != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SquishButton(
                    text = content.secondaryLabel,
                    onClick = onSecondary,
                    modifier = Modifier.weight(1f),
                    container = scheme.surface,
                    content = scheme.onSurface,
                    fillWidth = true
                )
                SquishButton(
                    text = content.actionLabel,
                    onClick = onPrimary,
                    modifier = Modifier.weight(1.4f),
                    fillWidth = true
                )
            }
        } else {
            SquishButton(
                text = content.actionLabel,
                onClick = onPrimary,
                modifier = Modifier.fillMaxWidth(),
                fillWidth = true,
                contentDescription = "${content.actionLabel}: ${content.title}"
            )
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
            highlighted(text),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(top = 3.dp)
        )
    }
}