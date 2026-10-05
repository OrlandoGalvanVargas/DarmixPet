package com.darmix.darmixpet.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.Gravity
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import com.darmix.darmixpet.R
import com.darmix.darmixpet.mascota.MascotThemeSpec
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin


class BlockOverlayView(context: Context) : FrameLayout(context) {

    private val d = resources.displayMetrics.density
    private val painter = StickerPainter(context)
    private var theme: MascotThemeSpec? = null

    private var backdropAlpha = 0f

    private val card: BlockCard
    private val content: LinearLayout
    private val iconBubble: IconBubble
    private val iconView: MenuIconView
    private val title: TextView
    private val body: TextView

    private val textWidthPx: Int by lazy {
        val screenW = resources.displayMetrics.widthPixels
        val desired = (270 * d).toInt()
        val cap = (screenW * 0.82f).toInt()
        minOf(desired, cap).coerceAtLeast((230 * d).toInt())
    }

    init {
        setWillNotDraw(false)
        clipChildren = false
        clipToPadding = false

        iconView = MenuIconView(context, MenuIcon.LOCK)

        iconBubble = IconBubble(context).apply {
            addView(
                iconView,
                FrameLayout.LayoutParams((46 * d).toInt(), (46 * d).toInt(), Gravity.CENTER)
            )
        }

        title = TextView(context).apply {
            textSize = 22f
            gravity = Gravity.CENTER
            includeFontPadding = false

        }
        body = TextView(context).apply {
            textSize = 15f
            gravity = Gravity.CENTER
            setLineSpacing(0f, 1.15f)
            includeFontPadding = false
        }

        content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            clipChildren = false
            clipToPadding = false

            addView(iconBubble, LinearLayout.LayoutParams((84 * d).toInt(), (84 * d).toInt()))
            addView(space(14))


            addView(
                title,
                LinearLayout.LayoutParams(textWidthPx, LinearLayout.LayoutParams.WRAP_CONTENT)
            )
            addView(space(6))
            addView(
                body,
                LinearLayout.LayoutParams(textWidthPx, LinearLayout.LayoutParams.WRAP_CONTENT)
            )
        }

        card = BlockCard(context, painter)
        card.addView(
            content,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )

        addView(
            card,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )
    }

    private fun space(dp: Int) = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(1, (dp * d).toInt())
    }



    fun setTheme(spec: MascotThemeSpec) {
        if (spec == theme) return
        theme = spec
        painter.setTheme(spec, fill = blockFill(spec))


        val inset = painter.insetPx.toInt()
        card.setPadding(inset, inset, inset, inset)


        val extra = painter.extraLeftPaddingPx.toInt()
        val hPad = (28 * d).toInt()
        content.setPadding(hPad + extra, (26 * d).toInt(), hPad, (30 * d).toInt())


        title.setTextColor(painter.ink)
        body.setTextColor(ColorUtils.setAlphaComponent(painter.ink, 215))
        ResourcesCompat.getFont(context, R.font.fredoka_bold)?.let { title.typeface = it }
        ResourcesCompat.getFont(context, R.font.nunito_semibold)?.let { body.typeface = it }

        iconBubble.applyTheme(
            fill = ColorUtils.blendARGB(Color.WHITE, painter.ink, 0.06f),
            border = painter.ink
        )
        iconView.tint = painter.ink

        requestLayout()
        invalidate()
    }

    fun setMessage(text: String) {
        val clean = text.replace(Regex("[⏳🚫⏰]"), "").trim()
        iconView.icon = inferIcon(text)
        title.text = inferTitle(text)
        body.text = clean
        contentDescription = "$clean. Bloqueado por DarmixPet."
    }


    fun playIn() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 220
            addUpdateListener { backdropAlpha = it.animatedValue as Float; invalidate() }
            start()
        }

        card.post {
            card.pivotX = card.width / 2f
            card.pivotY = card.height / 2f
            card.alpha = 0f
            card.scaleX = 0.7f
            card.scaleY = 0.7f
            card.animate()
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(340)
                .setInterpolator(OvershootInterpolator(1.6f))
                .start()

            iconBubble.postDelayed({ iconBubble.playBurst() }, 220)
        }
    }


    fun playOut(onEnd: () -> Unit = {}) {
        card.animate()
            .alpha(0f).scaleX(0.94f).scaleY(0.94f)
            .setDuration(200)
            .withEndAction(onEnd)
            .start()
        ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 220
            addUpdateListener { backdropAlpha = it.animatedValue as Float; invalidate() }
            start()
        }
    }



    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val a = (backdropAlpha * 168).toInt()
        if (a > 0) canvas.drawColor(Color.argb(a, 0, 0, 0))
    }



    private fun inferIcon(text: String): MenuIcon = when {
        text.contains("descanso", true) || text.contains("⏳") -> MenuIcon.HOURGLASS
        text.contains("sesiones", true) || text.contains("🚫") -> MenuIcon.LOCK
        text.contains("tiempo", true)   || text.contains("⏰") -> MenuIcon.CLOCK
        else -> MenuIcon.LOCK
    }

    private fun inferTitle(text: String): String = when {
        text.contains("descanso", true) -> "Tómate un respiro"
        text.contains("sesiones", true) -> "Por hoy, hasta aquí"
        text.contains("tiempo", true)   -> "Se acabó el tiempo"
        else -> "Alto ahí"
    }


    private fun blockFill(spec: MascotThemeSpec): Int =
        ColorUtils.blendARGB(
            Color.parseColor("#F7DEDE"),
            spec.primaryColor.toInt(),
            0.14f
        )




    private class IconBubble(context: Context) : FrameLayout(context) {

        private val d = resources.displayMetrics.density
        private var fillColor = Color.WHITE
        private var borderColor = Color.BLACK

        private var burstTick = 0f
        private var burstAnimator: ValueAnimator? = null

        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
        }
        private val starPath = Path()

        init {
            setWillNotDraw(false)
            clipChildren = false
            clipToPadding = false
        }

        fun applyTheme(fill: Int, border: Int) {
            fillColor = fill
            borderColor = border
            invalidate()
        }

        fun playBurst() {
            burstAnimator?.cancel()
            burstTick = 0f
            burstAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 700
                addUpdateListener {
                    burstTick = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }

        override fun onDetachedFromWindow() {
            burstAnimator?.cancel()
            burstAnimator = null
            super.onDetachedFromWindow()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            val r = min(width, height) / 2f - 3f * d


            fillPaint.color = borderColor
            canvas.drawCircle(cx + 2.5f * d, cy + 2.5f * d, r, fillPaint)

            fillPaint.color = fillColor
            canvas.drawCircle(cx, cy, r, fillPaint)

            strokePaint.color = borderColor
            strokePaint.strokeWidth = 2f * d
            canvas.drawCircle(cx, cy, r, strokePaint)


            val t = burstTick
            if (t in 0.01f..0.99f) {
                for (i in 0 until 6) {
                    val a = (i * 60f + 15f) * (PI / 180f).toFloat()
                    val radius = r + (2f + 16f * t) * d
                    val starR = (5f * (1f - t) + 1.5f) * d
                    drawStar(
                        canvas,
                        cx + radius * cos(a),
                        cy + radius * sin(a),
                        starR,
                        Color.parseColor("#E8B84A"),
                        1f - t
                    )
                }
            }
        }

        private fun drawStar(canvas: Canvas, cx: Float, cy: Float, radius: Float, color: Int, alpha: Float) {
            starPath.reset()
            val inner = radius * 0.34f
            for (i in 0 until 8) {
                val rr = if (i % 2 == 0) radius else inner
                val a = (PI / 4.0 * i - PI / 2.0).toFloat()
                val x = cx + rr * cos(a)
                val y = cy + rr * sin(a)
                if (i == 0) starPath.moveTo(x, y) else starPath.lineTo(x, y)
            }
            starPath.close()
            fillPaint.color = ColorUtils.setAlphaComponent(color, (255 * alpha).toInt())
            canvas.drawPath(starPath, fillPaint)
            strokePaint.color = ColorUtils.setAlphaComponent(borderColor, (180 * alpha).toInt())
            strokePaint.strokeWidth = 1.2f * d
            canvas.drawPath(starPath, strokePaint)
        }
    }


    private class BlockCard(
        context: Context,
        private val painter: StickerPainter
    ) : FrameLayout(context) {

        init {
            setWillNotDraw(false)
            clipChildren = false
            clipToPadding = false
        }

        override fun dispatchDraw(canvas: Canvas) {
            val inset = painter.insetPx
            painter.draw(
                canvas = canvas,
                left = inset,
                top = inset,
                w = width - 2 * inset,
                h = height - 2 * inset,
                side = BubblePointerSide.NONE,
                t = 0f
            )
            super.dispatchDraw(canvas)
        }
    }
}
