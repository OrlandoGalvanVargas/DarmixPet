package com.darmix.darmixpet.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.PowerManager
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.darmix.darmixpet.mascota.CharacterPhrases
import com.darmix.darmixpet.mascota.ConfigurableSkin
import com.darmix.darmixpet.mascota.MascotaAnimState
import com.darmix.darmixpet.mascota.MascotaSkin
import com.darmix.darmixpet.mascota.SkinInfo
import com.darmix.darmixpet.mascota.SkinPreferences
import com.darmix.darmixpet.mascota.SkinRegistry
import com.darmix.darmixpet.monitor.ContextualStateChecker
import com.darmix.darmixpet.monitor.ForegroundAppTracker
import com.darmix.darmixpet.notifications.NotificationLauncher
import com.darmix.darmixpet.notifications.NotificationListenerGuard
import com.darmix.darmixpet.notifications.NotificationRepository
import com.darmix.darmixpet.notifications.PetNotification
import com.darmix.darmixpet.tts.TtsManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PetOverlayService : LifecycleService() {

    companion object {
        private const val PET_SIZE_DP = 70
        private const val CONTEXTUAL_CHECK_INTERVAL_MS = 5_000L
        private const val BLOCK_OVERLAY_DISMISS_DELAY_MS = 1300L
        private const val BLOCK_DEBOUNCE_MS = 2000L
        private const val BUBBLE_FADE_MS = 250L
        private const val BUBBLE_VISIBLE_MS = 1500L
        private const val TAIL_CLEARANCE_DP = 12
        private const val ROTATION_SETTLE_MS = 300L
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
    private var petAuraView: PetAuraView? = null
    private lateinit var petAuraParams: WindowManager.LayoutParams
    private lateinit var layoutParams: WindowManager.LayoutParams
    private lateinit var ttsManager: TtsManager
    private lateinit var foregroundTracker: ForegroundAppTracker
    private lateinit var usageMonitor: AppUsageMonitor
    private lateinit var messageAlerts: MessageAlertCoordinator
    private lateinit var deviceControls: DeviceControls
    private var petSizePx = 0

    private val overlayArea = Rect()
    private val visibleFrame = Rect()

    private lateinit var flashlight: FlashlightController
    private var lastTorchToggleAt = 0L

    @Volatile private var isScreenOn = true
    private var screenReceiverRegistered = false

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
    private var notificationPanelView: NotificationPanelView? = null
    private lateinit var notificationPanelParams: WindowManager.LayoutParams
    private var notificationPanelJob: Job? = null
    private var currentNotificationKey: String? = null
    private var currentNotificationIndex = 0

    private val notificationGuard by lazy { NotificationListenerGuard(this, lifecycleScope) }

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
            val stateToShow = if (isPanelOpen()) MascotaAnimState.THINKING else currentBaseState
            overlayView?.playSpriteSheet(skin.getAnimation(this, stateToShow), loop = true)
            applyThemeToViews()
            PetForegroundNotification.refresh(this, currentSkinInfo.id)
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOn = true
                    updateContextualBaseState()
                    updateAura()
                    notificationGuard.check()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOn = false
                    updateAura()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        currentSkinInfo = SkinRegistry.byId(SkinPreferences.getSelectedSkinId(this))
        skin = ConfigurableSkin(currentSkinInfo)

        PetForegroundNotification.start(this, currentSkinInfo.id)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        currentBaseState = computeDesiredBaseState()
        flashlight = FlashlightController(this)
        deviceControls = DeviceControls(this)
        addAuraView()
        addOverlayView()

        ttsManager = TtsManager(this)
        foregroundTracker = ForegroundAppTracker(this)
        usageMonitor = AppUsageMonitor(
            context = this,
            scope = lifecycleScope,
            tracker = foregroundTracker,
            isScreenOn = { isScreenOn },
            speak = { ttsManager.speak(it) },
            onBlock = { blockAndRedirectHome(it) },
            onAppSwitched = {
                if (quickMenuView != null) hideQuickMenu()
                if (notificationPanelView != null) hideNotificationPanel()
            }
        )
        messageAlerts = MessageAlertCoordinator(
            context = this,
            scope = lifecycleScope,
            isAppInFront = { isScreenOn && usageMonitor.lastForegroundPackage == it },
            speak = { ttsManager.speak(it, interrupt = false) },
            showBubble = { if (isScreenOn && !isPanelOpen()) showSpeechBubble(it) }
        )

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

        usageMonitor.start()
        startContextualStateLoop()
        notificationGuard.start()
        startAuraListener()
        messageAlerts.start()
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
        notificationGuard.stop()
        if (::messageAlerts.isInitialized) messageAlerts.stop()
        ttsManager.shutdown()
        overlayView?.stopAnimation()
        overlayView?.let { windowManager.removeView(it) }
        overlayView = null
        petAuraView?.let { windowManager.removeView(it) }
        petAuraView = null
        blockOverlayView?.let { windowManager.removeView(it) }
        blockOverlayView = null
        speechBubbleView?.let { windowManager.removeView(it) }
        speechBubbleView = null
        quickMenuView?.let { windowManager.removeView(it) }
        quickMenuView = null
        notificationPanelJob?.cancel()
        notificationPanelView?.let { windowManager.removeView(it) }
        notificationPanelView = null
        batteryAlertView?.let { windowManager.removeView(it) }
        batteryAlertView = null
    }

    private fun applyThemeToViews() {
        val spec = currentSkinInfo.theme
        speechBubbleView?.setTheme(spec)
        batteryAlertView?.setTheme(spec)
        quickMenuView?.setTheme(spec)
        notificationPanelView?.let {
            it.setTheme(spec)
            repositionNotificationPanel(it)
        }
    }

    private fun sendToHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
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

    private fun refreshOverlayArea(): Rect {
        val pet = overlayView
        if (pet != null && pet.isAttachedToWindow) {
            pet.getWindowVisibleDisplayFrame(visibleFrame)
            if (!visibleFrame.isEmpty) {
                overlayArea.set(0, 0, visibleFrame.width(), visibleFrame.height())
                return overlayArea
            }
        }
        val metrics = resources.displayMetrics
        overlayArea.set(0, 0, metrics.widthPixels, metrics.heightPixels)
        return overlayArea
    }

    private fun placeNearPet(
        element: PetAnchoredElement,
        width: Int,
        height: Int,
        refreshArea: Boolean = true
    ): AnchorPlacement {
        val area = if (refreshArea || overlayArea.isEmpty) refreshOverlayArea() else overlayArea
        val placement = AnchorPlacer.place(
            petX = layoutParams.x,
            petY = layoutParams.y,
            petSize = petSizePx,
            area = area,
            width = width,
            height = height,
            insetPx = element.anchorInsetPx,
            gapPx = element.anchorGapPx,
            tailClearancePx = TAIL_CLEARANCE_DP * resources.displayMetrics.density
        )
        element.applyAnchor(placement.side, placement.tailOffsetPx)
        return placement
    }

    private fun clampPetPosition() {
        if (overlayArea.isEmpty) return
        val maxX = (overlayArea.right - petSizePx).coerceAtLeast(overlayArea.left)
        val maxY = (overlayArea.bottom - petSizePx).coerceAtLeast(overlayArea.top)
        layoutParams.x = layoutParams.x.coerceIn(overlayArea.left, maxX)
        layoutParams.y = layoutParams.y.coerceIn(overlayArea.top, maxY)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        overlayView?.postDelayed({ realignAfterRotation() }, ROTATION_SETTLE_MS)
    }

    private fun realignAfterRotation() {
        val pet = overlayView ?: return
        refreshOverlayArea()
        clampPetPosition()
        windowManager.updateViewLayout(pet, layoutParams)
        syncAuraPosition()
        repositionBubbleIfVisible(refreshArea = false)
        repositionBatteryAlertIfVisible(refreshArea = false)
        quickMenuView?.let { repositionQuickMenu(it) }
        notificationPanelView?.let { repositionNotificationPanel(it) }
    }

    private fun showSpeechBubble(text: String) {
        bubbleJob?.cancel()
        val bubble = ensureSpeechBubble()
        bubble.setTheme(currentSkinInfo.theme)
        bubble.setMessage(text)

        bubble.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val placement = placeNearPet(bubble, bubble.measuredWidth, bubble.measuredHeight)
        speechBubbleParams.x = placement.x
        speechBubbleParams.y = placement.y
        windowManager.updateViewLayout(bubble, speechBubbleParams)

        bubble.popIn(BUBBLE_FADE_MS)

        bubbleJob = lifecycleScope.launch {
            delay(BUBBLE_FADE_MS + BUBBLE_VISIBLE_MS)
            bubble.popOut(BUBBLE_FADE_MS)
        }
    }

    private fun repositionBubbleIfVisible(refreshArea: Boolean = true) {
        val bubble = speechBubbleView ?: return
        if (bubble.alpha <= 0f) return
        val placement = placeNearPet(bubble, bubble.width, bubble.height, refreshArea)
        speechBubbleParams.x = placement.x
        speechBubbleParams.y = placement.y
        windowManager.updateViewLayout(bubble, speechBubbleParams)
    }

    private fun hideSpeechBubbleNow() {
        bubbleJob?.cancel()
        speechBubbleView?.let { if (it.alpha > 0f) it.popOut(BUBBLE_FADE_MS) }
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
        val placement = placeNearPet(view, w, h)
        batteryAlertParams.x = placement.x
        batteryAlertParams.y = placement.y
        windowManager.updateViewLayout(view, batteryAlertParams)

        view.startBlink()
    }

    private fun refreshBatteryAlertIfVisible() {
        val view = batteryAlertView ?: return
        if (view.visibility != View.VISIBLE) return
        view.setPercentText(ContextualStateChecker.batteryPercent(this))
    }

    private fun repositionBatteryAlertIfVisible(refreshArea: Boolean = true) {
        val view = batteryAlertView ?: return
        if (view.visibility != View.VISIBLE) return
        val (w, h) = view.measureSelf()
        val placement = placeNearPet(view, w, h, refreshArea)
        batteryAlertParams.x = placement.x
        batteryAlertParams.y = placement.y
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
        val placement = placeNearPet(menu, w, h)
        menu.positionCard(placement.x, placement.y)
    }

    private fun showQuickMenu() {
        val menu = ensureQuickMenu()
        updateAura()
        menu.resetToFirstPage()
        menu.setTheme(currentSkinInfo.theme)

        overlayView?.playSpriteSheet(skin.getAnimation(this, MascotaAnimState.THINKING), loop = true)

        menu.configureStatusPage("Cargando…") {
            hideQuickMenu()
            deviceControls.openTimerApp()
        }
        menu.configureBrightnessPage(deviceControls.brightnessPercent()) { percent ->
            deviceControls.setBrightnessPercent(percent)
        }
        menu.configureVolumePage(deviceControls.volumePercent()) { percent ->
            deviceControls.setVolumePercent(percent)
        }
        menu.configureSleepPage { PetController.sleep(this@PetOverlayService) }
        menu.configureConfigButton {
            hideQuickMenu()
            deviceControls.openDarmixPet()
        }
        refreshSkinMenuPage(menu)
        repositionQuickMenu(menu)

        quickMenuStatusJob?.cancel()
        quickMenuStatusJob = lifecycleScope.launch {
            while (true) {
                menu.setStatusText(usageMonitor.statusText())
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
        updateAura()
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

    private fun isPanelOpen(): Boolean = quickMenuView != null || notificationPanelView != null

    private fun ensureNotificationPanel(): NotificationPanelView {
        notificationPanelView?.let { return it }
        val panel = NotificationPanelView(this).apply {
            onClose = { hideNotificationPanel() }
            onNext = { showNextNotification() }
            onDismiss = { dismissCurrentNotification() }
            onOpen = { openCurrentNotification() }
        }
        notificationPanelParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            0,
            PixelFormat.TRANSLUCENT
        )
        windowManager.addView(panel, notificationPanelParams)
        panel.requestFocus()
        notificationPanelView = panel
        return panel
    }

    private fun showNotificationPanel() {
        val items = NotificationRepository.notifications.value
        if (items.isEmpty()) return
        hideSpeechBubbleNow()

        currentNotificationIndex = 0
        currentNotificationKey = items.first().key

        val panel = ensureNotificationPanel()
        updateAura()
        panel.setTheme(currentSkinInfo.theme)
        overlayView?.playSpriteSheet(skin.getAnimation(this, MascotaAnimState.THINKING), loop = true)
        renderNotification(panel, items)

        notificationPanelJob?.cancel()
        notificationPanelJob = lifecycleScope.launch {
            NotificationRepository.notifications.collect { list -> onNotificationsChanged(list) }
        }
    }

    private fun onNotificationsChanged(list: List<PetNotification>) {
        val panel = notificationPanelView ?: return
        if (list.isEmpty()) {
            hideNotificationPanel()
            return
        }
        val keyIndex = list.indexOfFirst { it.key == currentNotificationKey }
        if (keyIndex >= 0) {
            currentNotificationIndex = keyIndex
        } else if (currentNotificationIndex >= list.size) {
            hideNotificationPanel()
            return
        }
        renderNotification(panel, list)
    }

    private fun renderNotification(panel: NotificationPanelView, list: List<PetNotification>) {
        val item = list[currentNotificationIndex]
        currentNotificationKey = item.key
        panel.bind(item, currentNotificationIndex, list.size, NotificationLauncher.canOpen(this, item))
        repositionNotificationPanel(panel)
    }

    private fun repositionNotificationPanel(panel: NotificationPanelView) {
        val (w, h) = panel.measureCard()
        val placement = placeNearPet(panel, w, h)
        panel.positionCard(placement.x, placement.y)
    }

    private fun showNextNotification() {
        val panel = notificationPanelView ?: return
        val list = NotificationRepository.notifications.value
        val next = currentNotificationIndex + 1
        if (next >= list.size) {
            hideNotificationPanel()
            return
        }
        currentNotificationIndex = next
        renderNotification(panel, list)
    }

    private fun dismissCurrentNotification() {
        val key = currentNotificationKey ?: return
        if (!NotificationRepository.dismiss(key)) {
            Toast.makeText(this, "No pude eliminar la notificación", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCurrentNotification() {
        val key = currentNotificationKey ?: return
        val item = NotificationRepository.notifications.value.firstOrNull { it.key == key } ?: return
        hideNotificationPanel()
        if (!NotificationLauncher.open(this, item)) {
            Toast.makeText(this, "No pude abrir esta notificación", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hideNotificationPanel() {
        notificationPanelJob?.cancel()
        notificationPanelJob = null
        currentNotificationKey = null
        notificationPanelView?.let { windowManager.removeView(it) }
        notificationPanelView = null
        overlayView?.playSpriteSheet(skin.getAnimation(this, currentBaseState), loop = true)
        updateAura()
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

        if (!isPanelOpen()) {
            overlayView?.playSpriteSheet(skin.getAnimation(this, desiredState), loop = true)
        }

        when {
            desiredState == MascotaAnimState.LOW_BATTERY && !lowBatteryWarningSpoken -> {
                lowBatteryWarningSpoken = true
                ttsManager.speak("Tu batería está baja, cuídala un poco")
            }
            desiredState == MascotaAnimState.NIGHT_WARNING && !nightWarningSpoken -> {
                nightWarningSpoken = true
                ttsManager.speak("Es muy tarde, deberías estar durmiendo")
            }
        }

        if (desiredState == MascotaAnimState.LOW_BATTERY && !wasLowBattery) {
            showBatteryAlert()
        } else if (desiredState != MascotaAnimState.LOW_BATTERY && wasLowBattery) {
            hideBatteryAlertPermanently()
        }
    }

    private fun addAuraView() {
        val sizePx = (PET_SIZE_DP * resources.displayMetrics.density).toInt()
        val aura = PetAuraView(this, sizePx).apply {
            visibility = View.INVISIBLE
        }
        petAuraParams = WindowManager.LayoutParams(
            aura.windowSizePx,
            aura.windowSizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }
        windowManager.addView(aura, petAuraParams)
        petAuraView = aura
    }

    private fun syncAuraPosition() {
        val aura = petAuraView ?: return
        petAuraParams.x = layoutParams.x - aura.paddingPx
        petAuraParams.y = layoutParams.y - aura.paddingPx
        windowManager.updateViewLayout(aura, petAuraParams)
    }

    private fun updateAura() {
        val active = isScreenOn &&
                NotificationRepository.notifications.value.isNotEmpty() &&
                !isPanelOpen()
        petAuraView?.setActive(active)
    }

    private fun startAuraListener() {
        lifecycleScope.launch {
            NotificationRepository.notifications.collect { updateAura() }
        }
    }

    private fun addOverlayView() {
        val sizePx = (PET_SIZE_DP * resources.displayMetrics.density).toInt()
        petSizePx = sizePx

        layoutParams = WindowManager.LayoutParams(
            sizePx, sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 100
        }

        val petView = OverlayPetView(this)
        petView.onFrameChanged = { frame -> petAuraView?.setFrame(frame) }
        petView.playSpriteSheet(skin.getAnimation(this, currentBaseState), loop = true)
        petView.setOnTouchListener(createDragTouchListener(petView))

        windowManager.addView(petView, layoutParams)
        overlayView = petView
        syncAuraPosition()
    }

    private fun createDragTouchListener(petView: OverlayPetView): View.OnTouchListener {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (System.currentTimeMillis() < suppressTapCallbacksUntil) return true
                if (!NotificationRepository.refreshNow()) notificationGuard.check()
                if (NotificationRepository.notifications.value.isNotEmpty()) {
                    showNotificationPanel()
                } else {
                    playReaction(petView, MascotaAnimState.TAP_SIMPLE)
                    showSpeechBubble(CharacterPhrases.random(currentSkinInfo.id))
                }
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
                    refreshOverlayArea()

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
                    clampPetPosition()
                    windowManager.updateViewLayout(view, layoutParams)
                    syncAuraPosition()
                    repositionBubbleIfVisible(refreshArea = false)
                    repositionBatteryAlertIfVisible(refreshArea = false)
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