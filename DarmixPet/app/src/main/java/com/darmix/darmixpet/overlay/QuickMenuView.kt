package com.darmix.darmixpet.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import com.darmix.darmixpet.R
import com.darmix.darmixpet.mascota.BubbleShapeStyle
import com.darmix.darmixpet.mascota.MascotThemeSpec
import kotlin.math.abs


class QuickMenuView(context: Context) : FrameLayout(context) {

    companion object {
        private const val CARD_WIDTH_DP = 252
        private const val SKIN_PREVIEW_SIZE_DP = 64
        private const val CONTENT_MIN_HEIGHT_DP = 108


        private const val BODY_GAP_DP = 14


        private const val SERVICE_GAP_DP = 4
        private const val MENU_TAIL_SCALE = 0.8f
        private const val SLEEP_ARM_MS = 3000L
    }

    private val d = resources.displayMetrics.density
    private fun dp(v: Float) = (v * d).toInt()

    private val painter = StickerPainter(context)
    private val card = StickerCardLayout(context, painter)
    private var theme: MascotThemeSpec? = null
    private var pointerSide = BubblePointerSide.NONE

    private val fredoka: Typeface? = ResourcesCompat.getFont(context, R.font.fredoka_semibold)
    private val fredokaBold: Typeface? = ResourcesCompat.getFont(context, R.font.fredoka_bold)
    private val nunito: Typeface? = ResourcesCompat.getFont(context, R.font.nunito_semibold)


    private val headerIcon = MenuIconView(context, MenuIcon.CLOCK)
    private val titleLabel = TextView(context)
    private val closeButton = StickerButton(context)


    private val statusBg = GradientDrawable()
    private val statusIcon = MenuIconView(context, MenuIcon.CLOCK)
    private val statusLabel = TextView(context)
    private val timerButton = StickerButton(context)


    private val brightnessValueLabel = TextView(context)
    private val brightnessSlider = StickerSlider(context)
    private val brightnessIconLow = MenuIconView(context, MenuIcon.SUN)
    private val brightnessIconHigh = MenuIconView(context, MenuIcon.SUN)


    private val volumeValueLabel = TextView(context)
    private val volumeSlider = StickerSlider(context)
    private val volumeIconLow = MenuIconView(context, MenuIcon.SPEAKER).also { it.level = 0 }
    private val volumeIconHigh = MenuIconView(context, MenuIcon.SPEAKER)


    private val configButton = StickerButton(context)
    private val sleepButton = StickerButton(context)
    private val sleepHint = TextView(context)


    private val skinStage = FrameLayout(context)
    private val skinGlow = GradientDrawable()
    private val skinPreviewImage = ImageView(context)
    private val skinNameLabel = TextView(context)
    private val skinPrevButton = StickerButton(context)
    private val skinNextButton = StickerButton(context)

    private val pageViews: List<View>
    private val tabs: List<MenuTabView>
    private val pageTitles = listOf("Estado", "Brillo", "Volumen", "Más opciones", "Personaje")
    private val pageIcons = listOf(MenuIcon.CLOCK, MenuIcon.SUN, MenuIcon.SPEAKER, MenuIcon.GEAR, MenuIcon.HAT)
    private val contentFrame = FrameLayout(context)
    private var currentPage = 0

    private var sleepArmed = false
    private val disarmSleepRunnable = Runnable { disarmSleep() }

    private var entered = false
    private var cardTicker: ValueAnimator? = null
    private var lastTick = 0f

    var onOutsideClick: (() -> Unit)? = null
    var onPageChanged: (() -> Unit)? = null

    init {
        setBackgroundColor(Color.parseColor("#33000000"))
        isClickable = true
        setOnClickListener { onOutsideClick?.invoke() }
        isFocusable = true
        isFocusableInTouchMode = true

        card.orientation = LinearLayout.VERTICAL
        card.setPadding(dp(30f), dp(30f), dp(30f), dp(30f))

        card.addView(buildHeader())
        val page0 = buildStatusPage()
        val page1 = buildSliderPage(brightnessValueLabel, brightnessSlider, brightnessIconLow, brightnessIconHigh)
        val page2 = buildSliderPage(volumeValueLabel, volumeSlider, volumeIconLow, volumeIconHigh)
        val page3 = buildMorePage()
        val page4 = buildSkinPage()
        pageViews = listOf(page0, page1, page2, page3, page4)

        contentFrame.minimumHeight = dp(CONTENT_MIN_HEIGHT_DP.toFloat())
        pageViews.forEachIndexed { i, page ->
            page.visibility = if (i == 0) View.VISIBLE else View.GONE
            contentFrame.addView(
                page,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL)
            )
        }
        card.addView(
            contentFrame,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(10f) }
        )

        tabs = buildTabs()
        addView(card, LayoutParams(dp(CARD_WIDTH_DP.toFloat()), LayoutParams.WRAP_CONTENT))

        updateHeader()
        updateTabs()
    }



    private fun buildHeader(): View {
        titleLabel.apply {
            textSize = 15f
            typeface = fredoka
            gravity = Gravity.CENTER_VERTICAL
        }
        closeButton.apply {
            circle = true
            circleSizeDp = 28f
            icon = MenuIcon.CLOSE
            contentDescription = "Cerrar menú"
            setOnClickListener { onOutsideClick?.invoke() }
        }
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(headerIcon, LinearLayout.LayoutParams(dp(22f), dp(22f)))
            addView(
                titleLabel,
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = dp(8f) }
            )
            addView(
                closeButton,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            )
        }
    }

    private fun buildStatusPage(): View {
        statusLabel.apply {
            textSize = 13f
            typeface = nunito
        }
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = statusBg
            setPadding(dp(10f), dp(9f), dp(10f), dp(9f))
            addView(statusIcon, LinearLayout.LayoutParams(dp(20f), dp(20f)))
            addView(
                statusLabel,
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = dp(8f) }
            )
        }
        timerButton.apply {
            label = "Temporizador"
            icon = MenuIcon.CLOCK
        }
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                box,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    .apply { rightMargin = dp(3.5f) }
            )
            addView(
                timerButton,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    .apply { topMargin = dp(8f) }
            )
        }
    }

    private fun buildSliderPage(
        valueLabel: TextView,
        slider: StickerSlider,
        iconLow: MenuIconView,
        iconHigh: MenuIconView
    ): View {
        valueLabel.apply {
            textSize = 26f
            typeface = fredokaBold
            gravity = Gravity.CENTER
        }
        iconLow.alpha = 0.55f
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(iconLow, LinearLayout.LayoutParams(dp(22f), dp(22f)))
            addView(slider, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(iconHigh, LinearLayout.LayoutParams(dp(22f), dp(22f)))
        }
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                valueLabel,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            )
            addView(
                row,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    .apply { topMargin = dp(4f) }
            )
        }
    }

    private fun buildMorePage(): View {
        configButton.apply {
            label = "Configuración"
            icon = MenuIcon.GEAR
        }
        sleepButton.apply {
            label = "Dormir mascota"
            icon = MenuIcon.MOON
        }
        sleepHint.apply {
            text = "Dejará de vigilar hasta que la despiertes desde Ajustes."
            textSize = 11.5f
            typeface = nunito
            gravity = Gravity.CENTER
        }
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                configButton,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            )
            addView(
                sleepButton,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    .apply { topMargin = dp(6f) }
            )
            addView(
                sleepHint,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    .apply { topMargin = dp(6f); rightMargin = dp(3.5f) }
            )
        }
    }

    private fun buildSkinPage(): View {
        skinPrevButton.apply {
            circle = true
            icon = MenuIcon.CHEVRON_LEFT
            contentDescription = "Personaje anterior"
        }
        skinNextButton.apply {
            circle = true
            icon = MenuIcon.CHEVRON_RIGHT
            contentDescription = "Personaje siguiente"
        }
        skinGlow.apply {
            shape = GradientDrawable.OVAL
            gradientType = GradientDrawable.RADIAL_GRADIENT
            gradientRadius = 44f * d
        }
        skinStage.background = skinGlow
        skinPreviewImage.scaleType = ImageView.ScaleType.FIT_CENTER
        skinStage.addView(
            skinPreviewImage,
            LayoutParams(dp(SKIN_PREVIEW_SIZE_DP.toFloat()), dp(SKIN_PREVIEW_SIZE_DP.toFloat()), Gravity.CENTER)
        )
        skinNameLabel.apply {
            textSize = 13f
            typeface = fredoka
            gravity = Gravity.CENTER
        }
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(skinPrevButton, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(
                skinStage,
                LinearLayout.LayoutParams(dp(78f), dp(78f)).apply { leftMargin = dp(8f); rightMargin = dp(8f) }
            )
            addView(skinNextButton, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(row)
            addView(
                skinNameLabel,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    .apply { topMargin = dp(4f) }
            )
        }
    }

    private fun buildTabs(): List<MenuTabView> {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val list = pageIcons.mapIndexed { index, icon ->
            MenuTabView(context, icon).apply {
                contentDescription = pageTitles[index]
                setOnClickListener { switchPage(index) }
            }
        }
        list.forEach { row.addView(it, LinearLayout.LayoutParams(0, dp(38f), 1f)) }
        card.addView(
            row,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(8f) }
        )
        return list
    }




    fun setTheme(spec: MascotThemeSpec) {
        theme = spec
        painter.setTheme(spec)
        painter.tailScale = MENU_TAIL_SCALE
        painter.insetOverridePx = painter.tailReachPx + 4f * d

        val ink = painter.ink
        val primary = spec.primaryColor.toInt()
        val accent = spec.accentColor.toInt()
        val inset = painter.insetPx.toInt()
        val content = dp(12f)
        card.setPadding(
            inset + content + painter.extraLeftPaddingPx.toInt(),
            inset + content,
            inset + content,
            inset + content
        )

        titleLabel.setTextColor(ink)
        headerIcon.tint = ink
        closeButton.setTheme(spec, Color.WHITE, ink)

        tabs.forEach { it.setTheme(ink, ColorUtils.blendARGB(Color.WHITE, primary, 0.38f)) }


        val radii = when (spec.shapeStyle) {
            BubbleShapeStyle.ARCANE_GLOW -> FloatArray(8) { 16f * d }
            BubbleShapeStyle.ORGANIC_LEAF -> floatArrayOf(
                16f * d, 16f * d, 5f * d, 5f * d, 16f * d, 16f * d, 5f * d, 5f * d
            )
            BubbleShapeStyle.ANGULAR_SHIELD -> FloatArray(8) { 4f * d }
        }
        statusBg.cornerRadii = radii
        statusBg.setColor(Color.WHITE)
        statusBg.setStroke(dp(1.5f), ink)
        statusLabel.setTextColor(ink)
        statusIcon.tint = ink

        timerButton.setTheme(spec, primary, readableOn(primary, ink))
        configButton.setTheme(spec, accent, readableOn(accent, ink))
        if (!sleepArmed) sleepButton.setTheme(spec, Color.WHITE, ink) else applySleepArmedColors(spec)
        sleepHint.setTextColor(ColorUtils.setAlphaComponent(ink, 190))

        brightnessSlider.setTheme(spec)
        volumeSlider.setTheme(spec)
        brightnessValueLabel.setTextColor(ink)
        volumeValueLabel.setTextColor(ink)
        brightnessIconLow.tint = ink
        brightnessIconHigh.tint = ink
        volumeIconLow.tint = ink
        volumeIconHigh.tint = ink

        skinPrevButton.setTheme(spec, Color.WHITE, ink)
        skinNextButton.setTheme(spec, Color.WHITE, ink)
        skinNameLabel.setTextColor(ink)
        skinGlow.colors = intArrayOf(ColorUtils.setAlphaComponent(primary, 110), Color.TRANSPARENT)

        card.invalidate()
        requestLayout()
    }

    private fun readableOn(bg: Int, ink: Int): Int =
        if (ColorUtils.calculateLuminance(bg) > 0.45) ink else Color.WHITE


    fun setPointerSide(side: BubblePointerSide) {
        if (pointerSide == side) return
        pointerSide = side
        card.side = side
        card.invalidate()
    }



    private fun switchPage(page: Int) {
        if (page !in pageViews.indices || page == currentPage) return
        val forward = page > currentPage
        val old = pageViews[currentPage]
        old.animate().cancel()
        old.visibility = View.GONE
        old.alpha = 1f
        old.translationX = 0f

        currentPage = page
        val incoming = pageViews[page]
        incoming.visibility = View.VISIBLE
        incoming.alpha = 0f
        incoming.translationX = (if (forward) 14f else -14f) * d
        incoming.animate().alpha(1f).translationX(0f).setDuration(180).start()

        disarmSleep()
        updateHeader()
        updateTabs()
        onPageChanged?.invoke()
    }

    fun resetToFirstPage() {
        pageViews.forEachIndexed { i, page ->
            page.animate().cancel()
            page.alpha = 1f
            page.translationX = 0f
            page.visibility = if (i == 0) View.VISIBLE else View.GONE
        }
        currentPage = 0
        disarmSleep()
        updateHeader()
        updateTabs()
    }

    private fun updateHeader() {
        titleLabel.text = pageTitles[currentPage]
        headerIcon.icon = pageIcons[currentPage]
    }

    private fun updateTabs() {
        tabs.forEachIndexed { i, tab -> tab.isCurrent = i == currentPage }
    }



    fun setStatusText(text: String) {
        val newline = text.indexOf('\n')
        if (newline > 0) {
            val span = SpannableString(text)
            span.setSpan(StyleSpan(Typeface.BOLD), 0, newline, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            span.setSpan(RelativeSizeSpan(1.08f), 0, newline, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            statusLabel.text = span
        } else {
            statusLabel.text = text
        }
    }

    fun configureStatusPage(statusText: String, onTimerClick: () -> Unit) {
        setStatusText(statusText)
        timerButton.setOnClickListener { onTimerClick() }
    }

    fun configureBrightnessPage(initialPercent: Int, onChange: (Int) -> Unit) {
        brightnessValueLabel.text = "$initialPercent%"
        brightnessSlider.progress = initialPercent
        brightnessSlider.onProgressChanged = { percent, fromUser ->
            brightnessValueLabel.text = "$percent%"
            if (fromUser) onChange(percent)
        }
    }

    fun configureVolumePage(initialPercent: Int, onChange: (Int) -> Unit) {
        volumeValueLabel.text = "$initialPercent%"
        volumeSlider.progress = initialPercent
        volumeSlider.onProgressChanged = { percent, fromUser ->
            volumeValueLabel.text = "$percent%"
            if (fromUser) onChange(percent)
        }
    }


    fun configureSleepPage(onSleepClick: () -> Unit) {
        sleepButton.setOnClickListener {
            if (!sleepArmed) {
                armSleep()
            } else {
                removeCallbacks(disarmSleepRunnable)
                onSleepClick()
            }
        }
    }

    fun configureConfigButton(onClick: () -> Unit) {
        configButton.setOnClickListener { onClick() }
    }

    fun configureSkinPage(name: String, previewFrame: Bitmap?, onPrev: () -> Unit, onNext: () -> Unit) {
        skinNameLabel.text = name
        skinPreviewImage.setImageBitmap(previewFrame)
        skinPreviewImage.animate().cancel()
        skinPreviewImage.scaleX = 0.75f
        skinPreviewImage.scaleY = 0.75f
        skinPreviewImage.animate().scaleX(1f).scaleY(1f).setDuration(280)
            .setInterpolator(OvershootInterpolator(2.6f)).start()
        skinPrevButton.setOnClickListener { onPrev() }
        skinNextButton.setOnClickListener { onNext() }
    }


    fun positionCard(x: Int, y: Int) {
        val nudge = (painter.insetPx + SERVICE_GAP_DP * d - BODY_GAP_DP * d).coerceAtLeast(0f)
        var tx = x.toFloat()
        var ty = y.toFloat()
        when (pointerSide) {
            BubblePointerSide.LEFT -> tx -= nudge
            BubblePointerSide.RIGHT -> tx += nudge
            BubblePointerSide.TOP -> ty -= nudge
            BubblePointerSide.BOTTOM -> ty += nudge
            BubblePointerSide.NONE -> {}
        }
        card.translationX = tx
        card.translationY = ty

        if (!entered) {
            entered = true
            playEnter()
        }
    }

    fun measureCard(): Pair<Int, Int> {
        val cardWidthPx = dp(CARD_WIDTH_DP.toFloat())
        card.measure(
            View.MeasureSpec.makeMeasureSpec(cardWidthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.UNSPECIFIED
        )
        return card.measuredWidth to card.measuredHeight
    }




    private fun playEnter() {
        val w = card.measuredWidth.toFloat()
        val h = card.measuredHeight.toFloat()
        val tip = 4f * d
        when (pointerSide) {
            BubblePointerSide.LEFT -> { card.pivotX = tip; card.pivotY = 35f * d }
            BubblePointerSide.RIGHT -> { card.pivotX = w - tip; card.pivotY = 35f * d }
            BubblePointerSide.TOP -> { card.pivotX = w / 2f; card.pivotY = tip }
            BubblePointerSide.BOTTOM -> { card.pivotX = w / 2f; card.pivotY = h - tip }
            BubblePointerSide.NONE -> { card.pivotX = w / 2f; card.pivotY = h / 2f }
        }
        card.alpha = 0f
        card.scaleX = 0.7f
        card.scaleY = 0.7f
        card.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(300).setInterpolator(OvershootInterpolator(1.5f)).start()
        alpha = 0f
        animate().alpha(1f).setDuration(160).start()
    }

    private fun armSleep() {
        sleepArmed = true
        sleepButton.label = "¿Seguro? Toca otra vez"
        theme?.let { applySleepArmedColors(it) }
        removeCallbacks(disarmSleepRunnable)
        postDelayed(disarmSleepRunnable, SLEEP_ARM_MS)
    }

    private fun applySleepArmedColors(spec: MascotThemeSpec) {
        sleepButton.setColors(Color.parseColor("#C96262"), Color.WHITE)
    }

    private fun disarmSleep() {
        removeCallbacks(disarmSleepRunnable)
        if (!sleepArmed) return
        sleepArmed = false
        sleepButton.label = "Dormir mascota"
        theme?.let { sleepButton.setTheme(it, Color.WHITE, painter.ink) }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        cardTicker = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2600
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                val t = it.animatedValue as Float

                if (abs(t - lastTick) > 0.02f) {
                    lastTick = t
                    card.tick = t
                    card.invalidate()
                }
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        cardTicker?.cancel()
        cardTicker = null
        removeCallbacks(disarmSleepRunnable)
        super.onDetachedFromWindow()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            onOutsideClick?.invoke()
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}


private class StickerCardLayout(
    context: Context,
    private val painter: StickerPainter
) : LinearLayout(context) {

    var side: BubblePointerSide = BubblePointerSide.NONE
    var tick: Float = 0f

    init {
        setWillNotDraw(false)
        isClickable = true
    }

    override fun dispatchDraw(canvas: Canvas) {
        val inset = painter.insetPx
        painter.draw(
            canvas = canvas,
            left = inset,
            top = inset,
            w = width - 2 * inset,
            h = height - 2 * inset,
            side = side,
            t = tick
        )
        super.dispatchDraw(canvas)
    }
}
