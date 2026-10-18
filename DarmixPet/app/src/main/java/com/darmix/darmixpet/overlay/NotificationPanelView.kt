package com.darmix.darmixpet.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.text.TextUtils
import android.text.format.DateUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
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
import com.darmix.darmixpet.notifications.PetNotification
import kotlin.math.abs
import android.view.MotionEvent

private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

class NotificationPanelView(context: Context) : FrameLayout(context), PetAnchoredElement {

    companion object {
        private const val CARD_WIDTH_DP = 264
        private const val BODY_GAP_DP = 14
        private const val MENU_TAIL_SCALE = 0.8f
        private const val AVATAR_SIZE_DP = 40
        private const val TEXT_MAX_LINES_PORTRAIT = 6
        private const val TEXT_MAX_LINES_LANDSCAPE = 3
        private const val JUST_NOW_MS = 60_000L
    }

    private val d = resources.displayMetrics.density
    private fun dp(v: Float) = (v * d).toInt()

    private val painter = StickerPainter(context)
    private val card = StickerCardLayout(context, painter)
    private var pointerSide = BubblePointerSide.NONE
    private var tailOffsetPx = 0f

    private val fredoka = ResourcesCompat.getFont(context, R.font.fredoka_semibold)
    private val nunito = ResourcesCompat.getFont(context, R.font.nunito_semibold)

    private val appIconImage = ImageView(context)
    private val appIconFallback = MenuIconView(context, MenuIcon.BELL)
    private val appNameLabel = TextView(context)
    private val counterLabel = TextView(context)
    private val counterBg = GradientDrawable()
    private val closeButton = StickerButton(context)

    private val bodyBox = LinearLayout(context)
    private val bodyBg = GradientDrawable()
    private val avatarImage = ImageView(context)
    private val titleLabel = TextView(context)
    private val textLabel = TextView(context)
    private val timeLabel = TextView(context)

    private val dismissButton = StickerButton(context)
    private val nextButton = StickerButton(context)

    private val appIconCache = HashMap<String, Drawable>()
    private var boundKey: String? = null
    private var entered = false
    private var cardTicker: ValueAnimator? = null
    private var lastTick = 0f

    var onClose: (() -> Unit)? = null
    var onDismiss: (() -> Unit)? = null
    var onNext: (() -> Unit)? = null

    var onOpen: (() -> Unit)? = null

    override val anchorInsetPx: Float get() = painter.insetPx
    override val anchorGapPx: Float get() = BODY_GAP_DP * d

    init {
        setBackgroundColor(Color.parseColor("#33000000"))
        isClickable = true
        setOnClickListener { onClose?.invoke() }
        isFocusable = true
        isFocusableInTouchMode = true

        card.orientation = LinearLayout.VERTICAL
        card.addView(buildHeader())
        card.addView(
            buildBody(),
            LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(10f); rightMargin = dp(3.5f) }
        )
        card.addView(
            buildActions(),
            LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(10f) }
        )
        addView(card, LayoutParams(dp(CARD_WIDTH_DP.toFloat()), WRAP))
    }

    private fun buildHeader(): View {
        appIconImage.scaleType = ImageView.ScaleType.FIT_CENTER
        val iconFrame = FrameLayout(context).apply {
            addView(appIconImage, LayoutParams(MATCH, MATCH))
            addView(appIconFallback, LayoutParams(MATCH, MATCH))
        }
        appNameLabel.apply {
            textSize = 15f
            typeface = fredoka
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            gravity = Gravity.CENTER_VERTICAL
        }
        counterBg.cornerRadius = 50f * d
        counterLabel.apply {
            textSize = 12f
            typeface = fredoka
            gravity = Gravity.CENTER
            background = counterBg
            setPadding(dp(8f), dp(2f), dp(8f), dp(2f))
        }
        closeButton.apply {
            circle = true
            circleSizeDp = 28f
            icon = MenuIcon.CLOSE
            contentDescription = "Cerrar notificaciones"
            setOnClickListener { onClose?.invoke() }
        }
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(iconFrame, LinearLayout.LayoutParams(dp(22f), dp(22f)))
            addView(
                appNameLabel,
                LinearLayout.LayoutParams(0, WRAP, 1f).apply { leftMargin = dp(8f); rightMargin = dp(6f) }
            )
            addView(counterLabel, LinearLayout.LayoutParams(WRAP, WRAP).apply { rightMargin = dp(6f) })
            addView(closeButton, LinearLayout.LayoutParams(WRAP, WRAP))
        }
    }

    private fun buildBody(): View {
        avatarImage.apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
        }
        titleLabel.apply {
            textSize = 14f
            typeface = fredoka
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
        }
        textLabel.apply {
            textSize = 13f
            typeface = nunito
            maxLines = TEXT_MAX_LINES_PORTRAIT
            ellipsize = TextUtils.TruncateAt.END
            setLineSpacing(0f, 1.05f)
        }
        timeLabel.apply {
            textSize = 11f
            typeface = nunito
        }
        val texts = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(titleLabel, LinearLayout.LayoutParams(MATCH, WRAP))
            addView(textLabel, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(2f) })
            addView(timeLabel, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(6f) })
        }
        return bodyBox.apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            background = bodyBg
            setPadding(dp(10f), dp(9f), dp(10f), dp(9f))
            addView(
                avatarImage,
                LinearLayout.LayoutParams(dp(AVATAR_SIZE_DP.toFloat()), dp(AVATAR_SIZE_DP.toFloat()))
                    .apply { rightMargin = dp(10f) }
            )
            addView(texts, LinearLayout.LayoutParams(0, WRAP, 1f))
            setOnClickListener { onOpen?.invoke() }
            setOnTouchListener { view, event ->
                if (view.isClickable) {
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN ->
                            view.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).start()
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                            view.animate().scaleX(1f).scaleY(1f).setDuration(200)
                                .setInterpolator(OvershootInterpolator(3f)).start()
                    }
                }
                false
            }
        }
    }

    private fun buildActions(): View {
        dismissButton.apply {
            label = "Eliminar"
            icon = MenuIcon.TRASH
            setOnClickListener { onDismiss?.invoke() }
        }
        nextButton.apply {
            label = "Siguiente"
            icon = MenuIcon.CHEVRON_RIGHT
            setOnClickListener { onNext?.invoke() }
        }
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(dismissButton, LinearLayout.LayoutParams(0, WRAP, 1f).apply { rightMargin = dp(4f) })
            addView(nextButton, LinearLayout.LayoutParams(0, WRAP, 1f).apply { leftMargin = dp(4f) })
        }
    }

    fun setTheme(spec: MascotThemeSpec) {
        painter.setTheme(spec)
        painter.tailScale = MENU_TAIL_SCALE
        painter.insetOverridePx = painter.tailReachPx + 4f * d

        val ink = painter.ink
        val primary = spec.primaryColor.toInt()
        val inset = painter.insetPx.toInt()
        val content = dp(12f)
        card.setPadding(
            inset + content + painter.extraLeftPaddingPx.toInt(),
            inset + content,
            inset + content,
            inset + content
        )

        appNameLabel.setTextColor(ink)
        appIconFallback.tint = ink
        counterLabel.setTextColor(ink)
        counterBg.setColor(ColorUtils.blendARGB(Color.WHITE, primary, 0.38f))
        counterBg.setStroke(dp(1.5f), ink)
        closeButton.setTheme(spec, Color.WHITE, ink)

        bodyBg.cornerRadii = when (spec.shapeStyle) {
            BubbleShapeStyle.ARCANE_GLOW -> FloatArray(8) { 16f * d }
            BubbleShapeStyle.ORGANIC_LEAF -> floatArrayOf(
                16f * d, 16f * d, 5f * d, 5f * d, 16f * d, 16f * d, 5f * d, 5f * d
            )
            BubbleShapeStyle.ANGULAR_SHIELD -> FloatArray(8) { 4f * d }
        }
        bodyBg.setColor(Color.WHITE)
        bodyBg.setStroke(dp(1.5f), ink)
        titleLabel.setTextColor(ink)
        textLabel.setTextColor(ColorUtils.setAlphaComponent(ink, 225))
        timeLabel.setTextColor(ColorUtils.setAlphaComponent(ink, 160))

        dismissButton.setTheme(spec, Color.WHITE, ink)
        nextButton.setTheme(spec, primary, readableOn(primary, ink))

        card.invalidate()
        requestLayout()
    }

    private fun readableOn(bg: Int, ink: Int): Int =
        if (ColorUtils.calculateLuminance(bg) > 0.45) ink else Color.WHITE

    fun bind(item: PetNotification, position: Int, total: Int, canOpen: Boolean) {
        val changed = item.key != boundKey
        boundKey = item.key
        val isLast = position >= total - 1

        appNameLabel.text = item.appName
        counterLabel.text = "${position + 1}/$total"
        bindAppIcon(item.packageName)
        bindAvatar(item.largeIcon)

        titleLabel.text = item.title.orEmpty()
        titleLabel.visibility = if (item.title != null) View.VISIBLE else View.GONE
        textLabel.text = item.text.orEmpty()
        textLabel.visibility = if (item.text != null) View.VISIBLE else View.GONE
        val time = relativeTime(item.postTime)
        timeLabel.text = if (canOpen) "$time · Toca para abrir" else time
        bodyBox.isClickable = canOpen
        bodyBox.contentDescription = if (canOpen) "Abrir notificación de ${item.appName}" else null

        nextButton.label = if (isLast) "Cerrar" else "Siguiente"
        nextButton.icon = if (isLast) MenuIcon.CLOSE else MenuIcon.CHEVRON_RIGHT

        if (changed && entered) animateBodySwap()
    }

    private fun bindAppIcon(packageName: String) {
        val drawable = appIconCache[packageName] ?: try {
            context.packageManager.getApplicationIcon(packageName).also { appIconCache[packageName] = it }
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
        appIconImage.setImageDrawable(drawable)
        appIconImage.visibility = if (drawable != null) View.VISIBLE else View.GONE
        appIconFallback.visibility = if (drawable != null) View.GONE else View.VISIBLE
    }

    private fun bindAvatar(icon: Icon?) {
        val drawable = icon?.let { runCatching { it.loadDrawable(context) }.getOrNull() }
        avatarImage.setImageDrawable(drawable)
        avatarImage.visibility = if (drawable != null) View.VISIBLE else View.GONE
    }

    private fun relativeTime(postTime: Long): CharSequence {
        val now = System.currentTimeMillis()
        return if (now - postTime < JUST_NOW_MS) "Ahora"
        else DateUtils.getRelativeTimeSpanString(postTime, now, DateUtils.MINUTE_IN_MILLIS)
    }

    private fun animateBodySwap() {
        bodyBox.animate().cancel()
        bodyBox.alpha = 0f
        bodyBox.translationX = 14f * d
        bodyBox.animate().alpha(1f).translationX(0f).setDuration(180).start()
    }

    override fun applyAnchor(side: BubblePointerSide, tailOffsetPx: Float) {
        this.tailOffsetPx = tailOffsetPx
        pointerSide = side
        card.side = side
        painter.tailAnchorPx = tailOffsetPx - painter.insetPx
        card.invalidate()
    }

    fun measureCard(): Pair<Int, Int> {
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        textLabel.maxLines = if (landscape) TEXT_MAX_LINES_LANDSCAPE else TEXT_MAX_LINES_PORTRAIT
        card.measure(
            View.MeasureSpec.makeMeasureSpec(dp(CARD_WIDTH_DP.toFloat()), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.UNSPECIFIED
        )
        return card.measuredWidth to card.measuredHeight
    }

    fun positionCard(x: Int, y: Int) {
        card.translationX = x.toFloat()
        card.translationY = y.toFloat()
        if (!entered) {
            entered = true
            playEnter()
        }
    }

    private fun playEnter() {
        val w = card.measuredWidth.toFloat()
        val h = card.measuredHeight.toFloat()
        val tip = 4f * d
        when (pointerSide) {
            BubblePointerSide.LEFT -> { card.pivotX = tip; card.pivotY = tailOffsetPx }
            BubblePointerSide.RIGHT -> { card.pivotX = w - tip; card.pivotY = tailOffsetPx }
            BubblePointerSide.TOP -> { card.pivotX = tailOffsetPx; card.pivotY = tip }
            BubblePointerSide.BOTTOM -> { card.pivotX = tailOffsetPx; card.pivotY = h - tip }
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
        super.onDetachedFromWindow()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            onClose?.invoke()
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}