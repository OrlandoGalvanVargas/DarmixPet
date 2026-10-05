package com.darmix.darmixpet.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.AlarmClock
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.darmix.darmixpet.R
import com.darmix.darmixpet.mascota.ConfigurableSkin
import com.darmix.darmixpet.mascota.MascotaAnimState
import com.darmix.darmixpet.mascota.MascotaSkin
import com.darmix.darmixpet.mascota.SkinInfo
import com.darmix.darmixpet.mascota.SkinPreferences
import com.darmix.darmixpet.mascota.SkinRegistry
import android.view.View
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.darmix.darmixpet.data.AppDatabase
import com.darmix.darmixpet.monitor.ForegroundAppSignal
import com.darmix.darmixpet.monitor.ForegroundAppTracker
import android.util.Log
import com.darmix.darmixpet.data.MonitoredAppDao
import com.darmix.darmixpet.data.MonitoredAppEntity
import java.util.Calendar
import com.darmix.darmixpet.tts.TtsManager
import android.view.GestureDetector
import com.darmix.darmixpet.monitor.ContextualStateChecker
import com.darmix.darmixpet.MainActivity
import com.darmix.darmixpet.mascota.CharacterPhrases


private data class Anchored(val x: Int, val y: Int, val side: BubblePointerSide)

class PetOverlayService : LifecycleService() {

    companion object {
        private const val CHANNEL_ID = "pet_overlay_channel"
        private const val NOTIFICATION_ID = 1001
        private const val PET_SIZE_DP = 70
        private const val FOREGROUND_POLL_INTERVAL_MS = 1000L
        private const val CONTEXTUAL_CHECK_INTERVAL_MS = 5_000L
        private const val DAILY_RESET_CHECK_INTERVAL_MS = 60_000L
        private const val BLOCK_OVERLAY_DISMISS_DELAY_MS = 1300L
        private const val BLOCK_DEBOUNCE_MS = 2000L
        private const val BUBBLE_FADE_MS = 250L
        private const val BUBBLE_VISIBLE_MS = 1500L
        private const val BUBBLE_GAP_DP = 4
        private const val SCREEN_MARGIN_DP = 8
        private const val TIMER_APP_PACKAGE = "com.darmix.tiempomio"
        private const val STATUS_REFRESH_INTERVAL_MS = 1000L
        private const val TRIPLE_TAP_WINDOW_MS = 600L
        private const val TAP_SUPPRESSION_WINDOW_MS = 500L
        private const val BATTERY_ALERT_REAPPEAR_INTERVAL_MS = 5 * 60_000L
        private const val TORCH_TOGGLE_COOLDOWN_MS = 1200L

        @Volatile private var userRequestedStop = false

        fun start(context: Context) {
            if (!PetPreferences.isEnabled(context)) return
            userRequestedStop = false
            val intent = Intent(context, PetOverlayService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            userRequestedStop = true
            context.stopService(Intent(context, PetOverlayService::class.java))
        }
    }

    private lateinit var windowManager: WindowManager
    private var overlayView: OverlayPetView? = null
    private lateinit var layoutParams: WindowManager.LayoutParams
    private lateinit var ttsManager: TtsManager
    private lateinit var foregroundTracker: ForegroundAppTracker
    private var petSizePx = 0

    private lateinit var flashlight: FlashlightController
    private var lastTorchToggleAt = 0L

    @Volatile private var isScreenOn = true
    private var screenReceiverRegistered = false

    @Volatile private var lastForegroundPackage: String? = null

    private var currentBaseState: MascotaAnimState = MascotaAnimState.IDLE
    private var nightWarningSpoken = false
    private var lowBatteryWarningSpoken = false

    private var blockOverlayView: BlockOverlayView? = null
    private lateinit var blockOverlayParams: WindowManager.LayoutParams
    private var lastBlockActionTime = 0L

    private var speechBubbleView: SpeechBubbleView? = null
    private lateinit var speechBubbleParams: WindowManager.LayoutParams
    private var bubbleJob: Job? = null

    private var quickMenuView: QuickMenuView? = null
    private lateinit var quickMenuParams: WindowManager.LayoutParams
    private var quickMenuStatusJob: Job? = null

    private val tapTimestamps = mutableListOf<Long>()
    private var suppressTapCallbacksUntil = 0L

    private var batteryAlertView: BatteryAlertView? = null
    private lateinit var batteryAlertParams: WindowManager.LayoutParams
    private var batteryAlertReappearJob: Job? = null

    private lateinit var skin: MascotaSkin
    private lateinit var currentSkinInfo: SkinInfo

    private val skinPrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == SkinPreferences.KEY_SELECTED_SKIN) {
            val newSkinId = SkinPreferences.getSelectedSkinId(this)
            currentSkinInfo = SkinRegistry.byId(newSkinId)
            skin = ConfigurableSkin(currentSkinInfo)
            val stateToShow = if (quickMenuView != null) MascotaAnimState.THINKING else currentBaseState
            overlayView?.playSpriteSheet(skin.getAnimation(this, stateToShow), loop = true)
            applyThemeToViews()
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOn = true
                    updateContextualBaseState()
                }
                Intent.ACTION_SCREEN_OFF -> isScreenOn = false
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        currentSkinInfo = SkinRegistry.byId(SkinPreferences.getSelectedSkinId(this))
        skin = ConfigurableSkin(currentSkinInfo)

        createNotificationChannel()
        startForegroundNotification()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        currentBaseState = computeDesiredBaseState()
        flashlight = FlashlightController(this)
        addOverlayView()

        ttsManager = TtsManager(this)
        foregroundTracker = ForegroundAppTracker(this)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        isScreenOn = powerManager.isInteractive
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, filter)
        screenReceiverRegistered = true

        SkinPreferences.registerListener(this, skinPrefsListener)

        if (currentBaseState == MascotaAnimState.LOW_BATTERY) {
            showBatteryAlert()
        }

        startMonitoringLoop()
        startContextualStateLoop()
        startDailyResetWatcherLoop()
        startInstantGateListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (userRequestedStop) return
        val restartIntent = Intent(applicationContext, PetOverlayService::class.java)
        ContextCompat.startForegroundService(applicationContext, restartIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (screenReceiverRegistered) {
            unregisterReceiver(screenReceiver)
            screenReceiverRegistered = false
        }
        SkinPreferences.unregisterListener(this, skinPrefsListener)
        if (::flashlight.isInitialized) flashlight.release()
        bubbleJob?.cancel()
        quickMenuStatusJob?.cancel()
        batteryAlertReappearJob?.cancel()
        ttsManager.shutdown()
        overlayView?.stopAnimation()
        overlayView?.let { windowManager.removeView(it) }
        overlayView = null
        blockOverlayView?.let { windowManager.removeView(it) }
        blockOverlayView = null
        speechBubbleView?.let { windowManager.removeView(it) }
        speechBubbleView = null
        quickMenuView?.let { windowManager.removeView(it) }
        quickMenuView = null
        batteryAlertView?.let { windowManager.removeView(it) }
        batteryAlertView = null
    }

    private fun applyThemeToViews() {
        val spec = currentSkinInfo.theme
        speechBubbleView?.setTheme(spec)
        batteryAlertView?.setTheme(spec)
        quickMenuView?.setTheme(spec)
    }



    private fun startInstantGateListener() {
        lifecycleScope.launch {
            val dao = AppDatabase.getInstance(this@PetOverlayService).monitoredAppDao()
            ForegroundAppSignal.events.collect { packageName ->
                if (quickMenuView != null) hideQuickMenu()
                lastForegroundPackage = packageName
                instantGateCheck(dao, packageName)
            }
        }
    }

    private suspend fun instantGateCheck(dao: MonitoredAppDao, packageName: String) {
        val app = dao.getByPackageName(packageName) ?: return
        val now = System.currentTimeMillis()
        val current = applyDailyResetIfNeeded(app, now)
        if (current != app) dao.update(current)

        val cooldownMillis = current.cooldownMinutes * 60_000L
        val inCooldown = current.lastBlockedTimestamp != 0L &&
                (now - current.lastBlockedTimestamp) < cooldownMillis

        if (inCooldown) {
            blockAndRedirectHome("Todavía estás en tiempo de descanso ⏳")
            return
        }
        if (current.sessionsUsedToday >= current.maxSessionsPerDay) {
            blockAndRedirectHome("Ya usaste todas tus sesiones de hoy 🚫")
        }
    }



    private fun startMonitoringLoop() {
        lifecycleScope.launch {
            val dao = AppDatabase.getInstance(this@PetOverlayService).monitoredAppDao()
            while (true) {
                if (isScreenOn) {
                    val foregroundPackage = foregroundTracker.poll()
                    if (foregroundPackage != null) {
                        lastForegroundPackage = foregroundPackage
                        val monitoredApp = dao.getByPackageName(foregroundPackage)
                        if (monitoredApp != null) handleMonitoredApp(dao, monitoredApp)
                    }
                }
                delay(FOREGROUND_POLL_INTERVAL_MS)
            }
        }
    }

    private fun applyDailyResetIfNeeded(app: MonitoredAppEntity, now: Long): MonitoredAppEntity {
        return if (!isSameDay(app.lastUsageResetTimestamp, now)) {
            app.copy(
                sessionsUsedToday = 0,
                usedInSessionMillis = 0L,
                halfwayAlertGiven = false,
                lastUsageResetTimestamp = now
            )
        } else app
    }

    private suspend fun handleMonitoredApp(dao: MonitoredAppDao, app: MonitoredAppEntity) {
        val now = System.currentTimeMillis()
        val current = applyDailyResetIfNeeded(app, now)

        val cooldownMillis = current.cooldownMinutes * 60_000L
        val inCooldown = current.lastBlockedTimestamp != 0L &&
                (now - current.lastBlockedTimestamp) < cooldownMillis

        if (inCooldown) {
            if (current != app) dao.update(current)
            blockAndRedirectHome("Todavía estás en tiempo de descanso ⏳")
            return
        }
        if (current.sessionsUsedToday >= current.maxSessionsPerDay) {
            if (current != app) dao.update(current)
            blockAndRedirectHome("Ya usaste todas tus sesiones de hoy 🚫")
            return
        }

        val sessionLimitMillis = current.sessionLimitMinutes * 60_000L
        var updated = current.copy(usedInSessionMillis = current.usedInSessionMillis + FOREGROUND_POLL_INTERVAL_MS)

        if (!updated.halfwayAlertGiven && updated.usedInSessionMillis >= sessionLimitMillis / 2) {
            ttsManager.speak("Vas a la mitad de tu tiempo en ${updated.appName}")
            updated = updated.copy(halfwayAlertGiven = true)
        }

        if (updated.usedInSessionMillis >= sessionLimitMillis) {
            val blocked = updated.copy(
                usedInSessionMillis = 0L,
                halfwayAlertGiven = false,
                sessionsUsedToday = updated.sessionsUsedToday + 1,
                lastBlockedTimestamp = now
            )
            ttsManager.speak("Se acabó tu tiempo en ${blocked.appName}")
            dao.update(blocked)
            blockAndRedirectHome("Se acabó tu tiempo ⏰")
        } else {
            dao.update(updated)
        }
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        if (t1 == 0L) return false
        val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun sendToHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
    }



    private fun startDailyResetWatcherLoop() {
        lifecycleScope.launch {
            val dao = AppDatabase.getInstance(this@PetOverlayService).monitoredAppDao()
            while (true) {
                val now = System.currentTimeMillis()
                dao.getAllMonitoredAppsOnce().forEach { app ->
                    if (app.lastUsageResetTimestamp != 0L && !isSameDay(app.lastUsageResetTimestamp, now)) {
                        dao.update(
                            app.copy(
                                sessionsUsedToday = 0,
                                usedInSessionMillis = 0L,
                                halfwayAlertGiven = false,
                                lastUsageResetTimestamp = now
                            )
                        )
                    }
                }
                delay(DAILY_RESET_CHECK_INTERVAL_MS)
            }
        }
    }



    private fun blockAndRedirectHome(message: String) {
        val now = System.currentTimeMillis()
        if (blockOverlayView != null || (now - lastBlockActionTime) < BLOCK_DEBOUNCE_MS) return
        lastBlockActionTime = now

        showBlockOverlay(message)
        sendToHome()
        foregroundTracker.reset()

        lifecycleScope.launch {
            delay(BLOCK_OVERLAY_DISMISS_DELAY_MS)
            hideBlockOverlay()
        }
    }

    private fun showBlockOverlay(message: String) {
        if (blockOverlayView != null) return
        val view = BlockOverlayView(this).apply {
            setTheme(currentSkinInfo.theme)
            setMessage(message)
        }
        blockOverlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        windowManager.addView(view, blockOverlayParams)
        view.playIn()
        blockOverlayView = view
    }

    private fun hideBlockOverlay() {
        val view = blockOverlayView ?: return
        blockOverlayView = null
        view.playOut {
            runCatching { windowManager.removeView(view) }
        }
    }



    private fun ensureSpeechBubble(): SpeechBubbleView {
        speechBubbleView?.let { return it }
        val bubble = SpeechBubbleView(this)
        speechBubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }
        windowManager.addView(bubble, speechBubbleParams)
        speechBubbleView = bubble
        return bubble
    }

    private fun computeAnchoredPosition(elementWidth: Int, elementHeight: Int): Anchored {
        val density = resources.displayMetrics.density
        val gapPx = (BUBBLE_GAP_DP * density).toInt()
        val marginPx = (SCREEN_MARGIN_DP * density).toInt()
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        val petLeft = layoutParams.x
        val petRight = layoutParams.x + petSizePx
        val petTop = layoutParams.y

        val fitsOnRight = petRight + gapPx + elementWidth <= screenWidth - marginPx
        val fitsOnLeft = petLeft - gapPx - elementWidth >= marginPx

        return when {
            fitsOnRight -> Anchored(petRight + gapPx, petTop, BubblePointerSide.LEFT)
            fitsOnLeft -> Anchored(petLeft - gapPx - elementWidth, petTop, BubblePointerSide.RIGHT)
            else -> {
                val spaceAbove = petTop
                val spaceBelow = screenHeight - (petTop + petSizePx)
                val y = if (spaceAbove >= spaceBelow) {
                    (petTop - gapPx - elementHeight).coerceAtLeast(marginPx)
                } else {
                    petTop + petSizePx + gapPx
                }
                val centeredX = petLeft + (petSizePx / 2) - (elementWidth / 2)
                val side = if (spaceAbove >= spaceBelow) BubblePointerSide.BOTTOM else BubblePointerSide.TOP
                Anchored(centeredX.coerceIn(marginPx, screenWidth - elementWidth - marginPx), y, side)
            }
        }
    }

    private fun showSpeechBubble(text: String) {
        bubbleJob?.cancel()
        val bubble = ensureSpeechBubble()
        bubble.setTheme(currentSkinInfo.theme)
        bubble.setMessage(text)

        bubble.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val anchored = computeAnchoredPosition(bubble.measuredWidth, bubble.measuredHeight)
        bubble.setPointerSide(anchored.side)
        speechBubbleParams.x = anchored.x
        speechBubbleParams.y = anchored.y
        windowManager.updateViewLayout(bubble, speechBubbleParams)

        bubble.popIn(BUBBLE_FADE_MS)

        bubbleJob = lifecycleScope.launch {
            delay(BUBBLE_FADE_MS + BUBBLE_VISIBLE_MS)
            bubble.popOut(BUBBLE_FADE_MS)
        }
    }

    private fun repositionBubbleIfVisible() {
        val bubble = speechBubbleView ?: return
        if (bubble.alpha <= 0f) return
        val anchored = computeAnchoredPosition(bubble.width, bubble.height)
        bubble.setPointerSide(anchored.side)
        speechBubbleParams.x = anchored.x
        speechBubbleParams.y = anchored.y
        windowManager.updateViewLayout(bubble, speechBubbleParams)
    }



    private fun ensureBatteryAlertView(): BatteryAlertView {
        batteryAlertView?.let { return it }
        val view = BatteryAlertView(this) { onBatteryAlertDismissed() }
        batteryAlertParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }
        windowManager.addView(view, batteryAlertParams)
        batteryAlertView = view
        return view
    }

    private fun showBatteryAlert() {
        batteryAlertReappearJob?.cancel()
        val view = ensureBatteryAlertView()
        view.setTheme(currentSkinInfo.theme)
        view.setPercentText(ContextualStateChecker.batteryPercent(this))

        val (w, h) = view.measureSelf()
        val anchored = computeAnchoredPosition(w, h)
        view.setPointerSide(anchored.side)
        val (nx, ny) = view.nudgedPosition(anchored.x, anchored.y, anchored.side)
        batteryAlertParams.x = nx
        batteryAlertParams.y = ny
        windowManager.updateViewLayout(view, batteryAlertParams)

        view.startBlink()
    }

    private fun refreshBatteryAlertIfVisible() {
        val view = batteryAlertView ?: return
        if (view.visibility != View.VISIBLE) return
        view.setPercentText(ContextualStateChecker.batteryPercent(this))
    }

    private fun repositionBatteryAlertIfVisible() {
        val view = batteryAlertView ?: return
        if (view.visibility != View.VISIBLE) return
        val (w, h) = view.measureSelf()
        val anchored = computeAnchoredPosition(w, h)
        view.setPointerSide(anchored.side)

        val (nx, ny) = view.nudgedPosition(anchored.x, anchored.y, anchored.side)
        batteryAlertParams.x = nx
        batteryAlertParams.y = ny

        windowManager.updateViewLayout(view, batteryAlertParams)
    }

    private fun onBatteryAlertDismissed() {
        val view = batteryAlertView ?: return
        view.stopBlink()
        view.visibility = View.GONE

        batteryAlertReappearJob?.cancel()
        batteryAlertReappearJob = lifecycleScope.launch {
            delay(BATTERY_ALERT_REAPPEAR_INTERVAL_MS)
            if (currentBaseState == MascotaAnimState.LOW_BATTERY) showBatteryAlert()
        }
    }

    private fun hideBatteryAlertPermanently() {
        batteryAlertReappearJob?.cancel()
        batteryAlertReappearJob = null
        batteryAlertView?.stopBlink()
        batteryAlertView?.let { windowManager.removeView(it) }
        batteryAlertView = null
    }



    private fun ensureQuickMenu(): QuickMenuView {
        quickMenuView?.let { return it }
        val menu = QuickMenuView(this).apply {
            onOutsideClick = { hideQuickMenu() }
            onPageChanged = { repositionQuickMenu(this) }
        }
        quickMenuParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            0,
            PixelFormat.TRANSLUCENT
        )
        windowManager.addView(menu, quickMenuParams)
        menu.requestFocus()
        quickMenuView = menu
        return menu
    }

    private fun repositionQuickMenu(menu: QuickMenuView) {
        val (w, h) = menu.measureCard()
        val anchored = computeAnchoredPosition(w, h)
        menu.setPointerSide(anchored.side)
        menu.positionCard(anchored.x, anchored.y)
    }

    private fun showQuickMenu() {
        val menu = ensureQuickMenu()
        menu.resetToFirstPage()
        menu.setTheme(currentSkinInfo.theme)

        overlayView?.playSpriteSheet(skin.getAnimation(this, MascotaAnimState.THINKING), loop = true)

        menu.configureStatusPage("Cargando…") { openTimerApp() }
        menu.configureBrightnessPage(getCurrentBrightnessPercent()) { percent -> setBrightnessPercent(percent) }
        menu.configureVolumePage(getCurrentVolumePercent()) { percent -> setVolumePercent(percent) }
        menu.configureSleepPage { PetController.sleep(this@PetOverlayService) }
        menu.configureConfigButton { openConfigApp() }
        refreshSkinMenuPage(menu)
        repositionQuickMenu(menu)

        quickMenuStatusJob?.cancel()
        quickMenuStatusJob = lifecycleScope.launch {
            while (true) {
                menu.setStatusText(buildQuickMenuStatusText())
                repositionQuickMenu(menu)
                delay(STATUS_REFRESH_INTERVAL_MS)
            }
        }
    }

    private fun hideQuickMenu() {
        quickMenuStatusJob?.cancel()
        quickMenuStatusJob = null
        quickMenuView?.let { windowManager.removeView(it) }
        quickMenuView = null
        overlayView?.playSpriteSheet(skin.getAnimation(this, currentBaseState), loop = true)
    }

    private fun refreshSkinMenuPage(menu: QuickMenuView) {
        val previewFrame = skin.getAnimation(this, MascotaAnimState.IDLE).frames.firstOrNull()
        menu.configureSkinPage(
            name = skin.displayName,
            previewFrame = previewFrame,
            onPrev = { cycleSkin(-1, menu) },
            onNext = { cycleSkin(1, menu) }
        )
    }

    private fun cycleSkin(direction: Int, menu: QuickMenuView) {
        val skins = SkinRegistry.availableSkins
        if (skins.isEmpty()) return
        val currentIndex = skins.indexOfFirst { it.id == skin.id }.coerceAtLeast(0)
        val nextIndex = (currentIndex + direction + skins.size) % skins.size
        SkinPreferences.setSelectedSkinId(this, skins[nextIndex].id)
        refreshSkinMenuPage(menu)
        repositionQuickMenu(menu)
    }

    private suspend fun buildQuickMenuStatusText(): String {
        val packageName = lastForegroundPackage ?: return "No hay ninguna app monitoreada abierta"
        val dao = AppDatabase.getInstance(this).monitoredAppDao()
        val app = dao.getByPackageName(packageName) ?: return "No hay ninguna app monitoreada abierta"

        val now = System.currentTimeMillis()
        val current = applyDailyResetIfNeeded(app, now)

        val cooldownMillis = current.cooldownMinutes * 60_000L
        val inCooldown = current.lastBlockedTimestamp != 0L &&
                (now - current.lastBlockedTimestamp) < cooldownMillis

        if (inCooldown) {
            val remainingMs = cooldownMillis - (now - current.lastBlockedTimestamp)
            return "${current.appName}\nEn enfriamiento: ${formatDuration(remainingMs)} restantes"
        }
        if (current.sessionsUsedToday >= current.maxSessionsPerDay) {
            return "${current.appName}\nSesiones agotadas por hoy"
        }
        val sessionLimitMillis = current.sessionLimitMinutes * 60_000L
        val remainingMs = (sessionLimitMillis - current.usedInSessionMillis).coerceAtLeast(0L)
        return "${current.appName}\nRestante: ${formatDuration(remainingMs)}"
    }

    private fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return if (minutes > 0) "${minutes} min ${seconds}s" else "${seconds}s"
    }

    private fun openTimerApp() {
        hideQuickMenu()
        val launchIntent = packageManager.getLaunchIntentForPackage(TIMER_APP_PACKAGE)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
            return
        }
        try {
            val systemTimerIntent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(systemTimerIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No se encontró ninguna app de temporizador", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openConfigApp() {
        hideQuickMenu()
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }



    private fun getCurrentBrightnessPercent(): Int {
        return try {
            val value = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            (value * 100 / 255).coerceIn(0, 100)
        } catch (e: Settings.SettingNotFoundException) {
            50
        }
    }

    private fun setBrightnessPercent(percent: Int) {
        if (!Settings.System.canWrite(this)) {
            Toast.makeText(this, "Activa el permiso para modificar ajustes del sistema", Toast.LENGTH_SHORT).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:$packageName")
            ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            startActivity(intent)
            return
        }
        try {
            Settings.System.putInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            val value = (percent * 255 / 100).coerceIn(0, 255)
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
        } catch (e: SecurityException) {
            Log.w("PetOverlayService", "No se pudo cambiar el brillo", e)
        }
    }



    private fun getCurrentVolumePercent(): Int {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (max == 0) return 0
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return (current * 100 / max).coerceIn(0, 100)
    }

    private fun setVolumePercent(percent: Int) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val value = (percent * max / 100).coerceIn(0, max)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0)
    }



    private fun startContextualStateLoop() {
        lifecycleScope.launch {
            while (true) {
                if (isScreenOn) {
                    updateContextualBaseState()
                    refreshBatteryAlertIfVisible()
                }
                delay(CONTEXTUAL_CHECK_INTERVAL_MS)
            }
        }
    }

    private fun computeDesiredBaseState(): MascotaAnimState {
        return when {
            ContextualStateChecker.isBatteryLow(this) -> MascotaAnimState.LOW_BATTERY
            ContextualStateChecker.isAudioPlaying(this) -> MascotaAnimState.LISTENING
            ContextualStateChecker.isNightTime() -> MascotaAnimState.NIGHT_WARNING
            else -> MascotaAnimState.IDLE
        }
    }

    private fun updateContextualBaseState() {
        val desiredState = computeDesiredBaseState()

        if (desiredState != MascotaAnimState.LOW_BATTERY) lowBatteryWarningSpoken = false
        if (desiredState != MascotaAnimState.NIGHT_WARNING) nightWarningSpoken = false

        if (desiredState == currentBaseState) return

        val wasLowBattery = currentBaseState == MascotaAnimState.LOW_BATTERY
        currentBaseState = desiredState

        if (quickMenuView == null) {
            overlayView?.playSpriteSheet(skin.getAnimation(this, desiredState), loop = true)
        }

        when {
            desiredState == MascotaAnimState.LOW_BATTERY && !lowBatteryWarningSpoken -> {
                lowBatteryWarningSpoken = true
                ttsManager.speak("Tu batería está baja, cuídala un poco")
            }
            desiredState == MascotaAnimState.NIGHT_WARNING && !nightWarningSpoken -> {
                nightWarningSpoken = true
                ttsManager.speak("Es muy tarde, ¿no deberías estar durmiendo?")
            }
        }

        if (desiredState == MascotaAnimState.LOW_BATTERY && !wasLowBattery) {
            showBatteryAlert()
        } else if (desiredState != MascotaAnimState.LOW_BATTERY && wasLowBattery) {
            hideBatteryAlertPermanently()
        }
    }



    private fun addOverlayView() {
        val sizePx = (PET_SIZE_DP * resources.displayMetrics.density).toInt()
        petSizePx = sizePx
        val overlayType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        layoutParams = WindowManager.LayoutParams(
            sizePx, sizePx, overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 100
        }

        val petView = OverlayPetView(this)
        petView.playSpriteSheet(skin.getAnimation(this, currentBaseState), loop = true)
        petView.setOnTouchListener(createDragTouchListener(petView))

        windowManager.addView(petView, layoutParams)
        overlayView = petView
    }

    private fun createDragTouchListener(petView: OverlayPetView): View.OnTouchListener {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (System.currentTimeMillis() < suppressTapCallbacksUntil) return true
                playReaction(petView, MascotaAnimState.TAP_SIMPLE)
                showSpeechBubble(CharacterPhrases.random(currentSkinInfo.id))
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (System.currentTimeMillis() < suppressTapCallbacksUntil) return true
                playReaction(petView, MascotaAnimState.TAP_DOBLE)
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                showQuickMenu()
            }
        })

        return View.OnTouchListener { view, event ->
            gestureDetector.onTouchEvent(event)
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY

                    val now = System.currentTimeMillis()
                    tapTimestamps.add(now)
                    tapTimestamps.removeAll { now - it > TRIPLE_TAP_WINDOW_MS }
                    if (tapTimestamps.size >= 3) {
                        tapTimestamps.clear()
                        suppressTapCallbacksUntil = now + TAP_SUPPRESSION_WINDOW_MS
                        playReaction(petView, MascotaAnimState.TRIPLE_TAP)
                        toggleFlashlight()
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(view, layoutParams)
                    repositionBubbleIfVisible()
                    repositionBatteryAlertIfVisible()
                    true
                }
                else -> false
            }
        }
    }

    private fun playReaction(petView: OverlayPetView, state: MascotaAnimState) {
        petView.playSpriteSheet(skin.getAnimation(this, state), loop = false) {
            petView.playSpriteSheet(skin.getAnimation(this, currentBaseState), loop = true)
        }
    }

    private fun startForegroundNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DarmixPet activo")
            .setContentText("Archi está vigilando tu tiempo de pantalla")
            .setSmallIcon(R.drawable.ic_stat_darmixpet)
            .setOngoing(true)
            .build()

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "DarmixPet Overlay", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService<NotificationManager>()
            manager?.createNotificationChannel(channel)
        }
    }

    private fun toggleFlashlight() {
        val now = System.currentTimeMillis()
        if (now - lastTorchToggleAt < TORCH_TOGGLE_COOLDOWN_MS) return
        lastTorchToggleAt = now
        val message = when (flashlight.toggle()) {
            FlashlightController.Result.ON -> CharacterPhrases.torchOn(currentSkinInfo.id)
            FlashlightController.Result.OFF -> CharacterPhrases.torchOff(currentSkinInfo.id)
            FlashlightController.Result.NO_FLASH -> "Este teléfono no tiene linterna"
            FlashlightController.Result.IN_USE -> "La cámara está en uso, no puedo encender la luz"
        }
        showSpeechBubble(message)
    }
}
