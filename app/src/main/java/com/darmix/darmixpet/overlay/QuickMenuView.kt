package com.darmix.darmixpet.overlay

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.darmix.darmixpet.mascota.BubbleShapeStyle
import com.darmix.darmixpet.mascota.MascotThemeSpec

class QuickMenuView(context: Context) : FrameLayout(context) {

    companion object {
        private const val CARD_WIDTH_DP = 230
        private const val SKIN_PREVIEW_SIZE_DP = 56
    }

    private val cardBackground: GradientDrawable
    private val card: LinearLayout
    private val contentFrame: FrameLayout
    private val prevArrow: TextView
    private val nextArrow: TextView

    private val statusLabel: TextView
    private val timerButton: Button
    private val brightnessValueLabel: TextView
    private val brightnessSeekBar: SeekBar
    private val volumeValueLabel: TextView
    private val volumeSeekBar: SeekBar
    private val sleepButton: Button
    private val skinPreviewImage: ImageView
    private val skinNameLabel: TextView
    private val skinPrevArrow: TextView
    private val skinNextArrow: TextView

    private val pageViews: List<View>
    private var currentPage = 0

    var onOutsideClick: (() -> Unit)? = null
    var onPageChanged: (() -> Unit)? = null

    init {
        setBackgroundColor(Color.parseColor("#33000000"))
        isClickable = true
        setOnClickListener { onOutsideClick?.invoke() }

        isFocusable = true
        isFocusableInTouchMode = true

        val density = resources.displayMetrics.density
        val pad = (16 * density).toInt()
        val cardWidthPx = (CARD_WIDTH_DP * density).toInt()

        cardBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 18f * density
            setColor(Color.WHITE)
        }

        card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            isClickable = true
            setOnClickListener { /* swallow: no cierra el menú */ }
            background = cardBackground
            setPadding(pad, pad, pad, pad)
        }

        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val headerSpacer = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
        }
        val closeButton = TextView(context).apply {
            text = "✕"
            textSize = 16f
            setTextColor(Color.parseColor("#888888"))
            setPadding((6 * density).toInt(), 0, 0, (8 * density).toInt())
            setOnClickListener { onOutsideClick?.invoke() }
        }
        headerRow.addView(headerSpacer)
        headerRow.addView(closeButton)
        card.addView(headerRow)

        // Página 0: estado + temporizador
        statusLabel = TextView(context).apply {
            setTextColor(Color.parseColor("#333333"))
            textSize = 14f
            setPadding(0, 0, 0, (10 * density).toInt())
        }
        timerButton = Button(context).apply { text = "⏱ Temporizador" }
        val page0 = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            addView(statusLabel)
            addView(timerButton)
        }

        // Página 1: brillo
        val brightnessTitle = TextView(context).apply {
            text = "☀ Brillo de pantalla"
            setTextColor(Color.parseColor("#333333"))
            textSize = 14f
            setPadding(0, 0, 0, (8 * density).toInt())
        }
        brightnessValueLabel = TextView(context).apply {
            setTextColor(Color.parseColor("#888888"))
            textSize = 12f
        }
        brightnessSeekBar = SeekBar(context).apply { max = 100 }
        val page1 = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            visibility = View.GONE
            addView(brightnessTitle)
            addView(brightnessSeekBar)
            addView(brightnessValueLabel)
        }

        // Página 2: volumen
        val volumeTitle = TextView(context).apply {
            text = "🔊 Volumen"
            setTextColor(Color.parseColor("#333333"))
            textSize = 14f
            setPadding(0, 0, 0, (8 * density).toInt())
        }
        volumeValueLabel = TextView(context).apply {
            setTextColor(Color.parseColor("#888888"))
            textSize = 12f
        }
        volumeSeekBar = SeekBar(context).apply { max = 100 }
        val page2 = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            visibility = View.GONE
            addView(volumeTitle)
            addView(volumeSeekBar)
            addView(volumeValueLabel)
        }

        // Página 3: apagar mascota
        val sleepTitle = TextView(context).apply {
            text = "😴 Apagar temporalmente"
            setTextColor(Color.parseColor("#333333"))
            textSize = 14f
            setPadding(0, 0, 0, (6 * density).toInt())
        }
        val sleepSubtitle = TextView(context).apply {
            text = "Se detiene el monitoreo hasta que la vuelvas a activar desde la app"
            setTextColor(Color.parseColor("#888888"))
            textSize = 11f
            setPadding(0, 0, 0, (10 * density).toInt())
        }
        sleepButton = Button(context).apply { text = "Dormir mascota" }
        val page3 = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            visibility = View.GONE
            addView(sleepTitle)
            addView(sleepSubtitle)
            addView(sleepButton)
        }

        // Página 4: cambiar personaje
        val skinTitle = TextView(context).apply {
            text = "🎭 Personaje"
            setTextColor(Color.parseColor("#333333"))
            textSize = 14f
            setPadding(0, 0, 0, (8 * density).toInt())
        }
        val skinPreviewSizePx = (SKIN_PREVIEW_SIZE_DP * density).toInt()
        skinPreviewImage = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(skinPreviewSizePx, skinPreviewSizePx)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        skinPrevArrow = TextView(context).apply {
            text = "◀"
            textSize = 18f
            setTextColor(Color.parseColor("#555555"))
            setPadding((12 * density).toInt(), 0, (12 * density).toInt(), 0)
        }
        skinNextArrow = TextView(context).apply {
            text = "▶"
            textSize = 18f
            setTextColor(Color.parseColor("#555555"))
            setPadding((12 * density).toInt(), 0, (12 * density).toInt(), 0)
        }
        val skinSelectorRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            addView(skinPrevArrow)
            addView(skinPreviewImage)
            addView(skinNextArrow)
        }
        skinNameLabel = TextView(context).apply {
            setTextColor(Color.parseColor("#888888"))
            textSize = 12f
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, (8 * density).toInt(), 0, 0)
        }
        val page4 = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            visibility = View.GONE
            addView(skinTitle)
            addView(skinSelectorRow)
            addView(skinNameLabel)
        }

        pageViews = listOf(page0, page1, page2, page3, page4)

        contentFrame = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        pageViews.forEach { contentFrame.addView(it) }
        card.addView(contentFrame)

        val navRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, (8 * density).toInt(), 0, 0)
        }
        prevArrow = TextView(context).apply {
            text = "‹"
            textSize = 22f
            setPadding((10 * density).toInt(), 0, (10 * density).toInt(), 0)
            setOnClickListener { switchPage(currentPage - 1) }
        }
        val navSpacer = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
        }
        nextArrow = TextView(context).apply {
            text = "›"
            textSize = 22f
            setPadding((10 * density).toInt(), 0, (10 * density).toInt(), 0)
            setOnClickListener { switchPage(currentPage + 1) }
        }
        navRow.addView(prevArrow)
        navRow.addView(navSpacer)
        navRow.addView(nextArrow)
        card.addView(navRow)

        addView(card, LayoutParams(cardWidthPx, LayoutParams.WRAP_CONTENT))

        updateArrowsVisibility()
    }

    /** Aplica la forma, borde y colores de acento del personaje activo a la tarjeta. */
    fun setTheme(spec: MascotThemeSpec) {
        val density = resources.displayMetrics.density
        val radii: FloatArray = when (spec.shapeStyle) {
            BubbleShapeStyle.ARCANE_GLOW -> FloatArray(8) { 26f * density }
            BubbleShapeStyle.ORGANIC_LEAF -> floatArrayOf(
                30f * density, 30f * density, 10f * density, 10f * density,
                30f * density, 30f * density, 10f * density, 10f * density
            )
            BubbleShapeStyle.ANGULAR_SHIELD -> FloatArray(8) { 8f * density }
        }
        cardBackground.cornerRadii = radii
        cardBackground.setStroke((2f * density).toInt(), spec.accentColor.toInt())

        val tint = ColorStateList.valueOf(spec.primaryColor.toInt())
        timerButton.backgroundTintList = tint
        sleepButton.backgroundTintList = tint
        brightnessSeekBar.progressTintList = tint
        brightnessSeekBar.thumbTintList = tint
        volumeSeekBar.progressTintList = tint
        volumeSeekBar.thumbTintList = tint
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            onOutsideClick?.invoke()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun switchPage(page: Int) {
        if (page !in pageViews.indices || page == currentPage) return
        pageViews[currentPage].visibility = View.GONE
        currentPage = page
        pageViews[currentPage].visibility = View.VISIBLE
        updateArrowsVisibility()
        onPageChanged?.invoke()
    }

    private fun updateArrowsVisibility() {
        prevArrow.visibility = if (currentPage > 0) View.VISIBLE else View.INVISIBLE
        nextArrow.visibility = if (currentPage < pageViews.size - 1) View.VISIBLE else View.INVISIBLE
    }

    fun resetToFirstPage() {
        pageViews[currentPage].visibility = View.GONE
        currentPage = 0
        pageViews[0].visibility = View.VISIBLE
        updateArrowsVisibility()
    }

    fun setStatusText(text: String) { statusLabel.text = text }

    fun configureStatusPage(statusText: String, onTimerClick: () -> Unit) {
        statusLabel.text = statusText
        timerButton.setOnClickListener { onTimerClick() }
    }

    fun configureBrightnessPage(initialPercent: Int, onChange: (Int) -> Unit) {
        brightnessValueLabel.text = "$initialPercent%"
        brightnessSeekBar.progress = initialPercent
        brightnessSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                brightnessValueLabel.text = "$progress%"
                if (fromUser) onChange(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    fun configureVolumePage(initialPercent: Int, onChange: (Int) -> Unit) {
        volumeValueLabel.text = "$initialPercent%"
        volumeSeekBar.progress = initialPercent
        volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                volumeValueLabel.text = "$progress%"
                if (fromUser) onChange(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    fun configureSleepPage(onSleepClick: () -> Unit) {
        sleepButton.setOnClickListener { onSleepClick() }
    }

    fun configureSkinPage(name: String, previewFrame: Bitmap?, onPrev: () -> Unit, onNext: () -> Unit) {
        skinNameLabel.text = name
        skinPreviewImage.setImageBitmap(previewFrame)
        skinPrevArrow.setOnClickListener { onPrev() }
        skinNextArrow.setOnClickListener { onNext() }
    }

    fun positionCard(x: Int, y: Int) {
        card.translationX = x.toFloat()
        card.translationY = y.toFloat()
    }

    fun measureCard(): Pair<Int, Int> {
        val density = resources.displayMetrics.density
        val cardWidthPx = (CARD_WIDTH_DP * density).toInt()
        card.measure(
            View.MeasureSpec.makeMeasureSpec(cardWidthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.UNSPECIFIED
        )
        return card.measuredWidth to card.measuredHeight
    }
}