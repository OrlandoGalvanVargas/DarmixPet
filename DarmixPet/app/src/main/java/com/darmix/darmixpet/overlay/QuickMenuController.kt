package com.darmix.darmixpet.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.WindowManager
import com.darmix.darmixpet.mascota.MascotThemeSpec
import com.darmix.darmixpet.mascota.MascotaAnimState
import com.darmix.darmixpet.mascota.MascotaSkin
import com.darmix.darmixpet.mascota.SkinPreferences
import com.darmix.darmixpet.mascota.SkinRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class QuickMenuController(
    private val context: Context,
    private val windowManager: WindowManager,
    private val scope: CoroutineScope,
    private val deviceControls: DeviceControls,
    private val statusText: suspend () -> String,
    private val placeNearPet: (PetAnchoredElement, Int, Int) -> AnchorPlacement,
    private val currentTheme: () -> MascotThemeSpec,
    private val currentSkin: () -> MascotaSkin,
    private val onOpened: () -> Unit,
    private val onClosed: () -> Unit,
    private val onSleep: () -> Unit
) {

    companion object {
        private const val STATUS_REFRESH_INTERVAL_MS = 1000L
    }

    private var menuView: QuickMenuView? = null
    private var statusJob: Job? = null

    val isOpen: Boolean get() = menuView != null

    fun show() {
        val menu = ensureMenu()
        onOpened()
        menu.resetToFirstPage()
        menu.setTheme(currentTheme())

        menu.configureStatusPage("Cargando…") {
            hide()
            deviceControls.openTimerApp()
        }
        menu.configureBrightnessPage(deviceControls.brightnessPercent()) { percent ->
            deviceControls.setBrightnessPercent(percent)
        }
        menu.configureVolumePage(deviceControls.volumePercent()) { percent ->
            deviceControls.setVolumePercent(percent)
        }
        menu.configureSleepPage { onSleep() }
        menu.configureConfigButton {
            hide()
            deviceControls.openDarmixPet()
        }
        refreshSkinPage(menu)
        reposition(menu)

        statusJob?.cancel()
        statusJob = scope.launch {
            while (true) {
                menu.setStatusText(statusText())
                reposition(menu)
                delay(STATUS_REFRESH_INTERVAL_MS)
            }
        }
    }

    fun hide() {
        val menu = menuView ?: return
        statusJob?.cancel()
        statusJob = null
        runCatching { windowManager.removeView(menu) }
        menuView = null
        onClosed()
    }

    fun applyTheme(spec: MascotThemeSpec) {
        menuView?.setTheme(spec)
    }

    fun reposition() {
        menuView?.let { reposition(it) }
    }

    fun destroy() {
        statusJob?.cancel()
        statusJob = null
        menuView?.let { runCatching { windowManager.removeView(it) } }
        menuView = null
    }

    private fun ensureMenu(): QuickMenuView {
        menuView?.let { return it }
        val menu = QuickMenuView(context).apply {
            onOutsideClick = { hide() }
            onPageChanged = { reposition(this) }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            0,
            PixelFormat.TRANSLUCENT
        )
        windowManager.addView(menu, params)
        menu.requestFocus()
        menuView = menu
        return menu
    }

    private fun reposition(menu: QuickMenuView) {
        val (w, h) = menu.measureCard()
        val placement = placeNearPet(menu, w, h)
        menu.positionCard(placement.x, placement.y)
    }

    private fun refreshSkinPage(menu: QuickMenuView) {
        val skin = currentSkin()
        val previewFrame = skin.getAnimation(context, MascotaAnimState.IDLE).frames.firstOrNull()
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
        val currentId = currentSkin().id
        val currentIndex = skins.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
        val nextIndex = (currentIndex + direction + skins.size) % skins.size
        SkinPreferences.setSelectedSkinId(context, skins[nextIndex].id)
        refreshSkinPage(menu)
        reposition(menu)
    }
}