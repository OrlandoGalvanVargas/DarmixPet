package com.darmix.darmixpet.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.darmix.darmixpet.R
import com.darmix.darmixpet.mascota.BubbleShapeStyle
import com.darmix.darmixpet.mascota.MascotThemeSpec
import kotlin.math.max


class SpeechBubbleView(context: Context) : FrameLayout(context) {

    private val label: TextView
    private val painter = StickerPainter(context)
    private val density = resources.displayMetrics.density

    private var theme: MascotThemeSpec? = null
    private var pointerSide: BubblePointerSide = BubblePointerSide.NONE

    private var tick = 0f
    private var ticker: ValueAnimator? = null

    init {
        alpha = 0f
        setWillNotDraw(false)
        label = TextView(context).apply {
            textSize = 14f
            setLineSpacing(0f, 1.05f)
            maxWidth = (200 * density).toInt()
        }
        addView(label, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
    }

    fun setTheme(spec: MascotThemeSpec) {
        if (spec == theme) return
        theme = spec
        painter.setTheme(spec)

        val inset = painter.insetPx.toInt()
        setPadding(inset, inset, inset, inset)

        val h = (11 * density).toInt()
        val v = (9 * density).toInt()
        label.setPadding(h + painter.extraLeftPaddingPx.toInt(), v, h, v)
        label.setTextColor(painter.ink)
        val font = if (spec.shapeStyle == BubbleShapeStyle.ANGULAR_SHIELD) R.font.fredoka_semibold
        else R.font.nunito_semibold
        ResourcesCompat.getFont(context, font)?.let { label.typeface = it }

        requestLayout()
        invalidate()
    }

    fun setPointerSide(side: BubblePointerSide) {
        if (pointerSide == side) return
        pointerSide = side
        invalidate()
    }

    fun setMessage(text: String) {
        label.text = text
        requestLayout()
    }


    fun popIn(durationMs: Long) {
        animate().cancel()
        applyPivot()
        alpha = 0f
        scaleX = 0.6f
        scaleY = 0.6f
        animate()
            .alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(durationMs + 120)
            .setInterpolator(OvershootInterpolator(1.5f))
            .start()
        startTicker()
    }

    fun popOut(durationMs: Long) {
        animate().cancel()
        animate()
            .alpha(0f).scaleX(0.92f).scaleY(0.92f)
            .setDuration(durationMs)
            .setInterpolator(null)
            .withEndAction { if (alpha <= 0.01f) stopTicker() }
            .start()
    }

    private fun applyPivot() {
        val w = max(width, measuredWidth).toFloat()
        val h = max(height, measuredHeight).toFloat()
        val inset = painter.insetPx
        when (pointerSide) {
            BubblePointerSide.LEFT -> { pivotX = inset; pivotY = h / 2f }
            BubblePointerSide.RIGHT -> { pivotX = w - inset; pivotY = h / 2f }
            BubblePointerSide.TOP -> { pivotX = w / 2f; pivotY = inset }
            BubblePointerSide.BOTTOM -> { pivotX = w / 2f; pivotY = h - inset }
            BubblePointerSide.NONE -> { pivotX = w / 2f; pivotY = h / 2f }
        }
    }

    private fun startTicker() {
        if (ticker?.isRunning == true) return
        ticker = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2200
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                tick = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    override fun onDetachedFromWindow() {
        stopTicker()
        super.onDetachedFromWindow()
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (theme != null) {
            val inset = painter.insetPx
            painter.draw(
                canvas = canvas,
                left = inset,
                top = inset,
                w = width - 2 * inset,
                h = height - 2 * inset,
                side = pointerSide,
                t = tick
            )
        }
        super.dispatchDraw(canvas)
    }
}
