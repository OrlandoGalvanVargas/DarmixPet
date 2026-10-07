package com.darmix.darmixpet.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.OvershootInterpolator
import androidx.core.content.res.ResourcesCompat
import com.darmix.darmixpet.R
import com.darmix.darmixpet.mascota.BubbleShapeStyle
import com.darmix.darmixpet.mascota.MascotThemeSpec


class StickerButton(context: Context) : View(context) {

    private val d = resources.displayMetrics.density

    var label: String = ""
        set(value) { field = value; contentDescription = value; invalidate() }
    var icon: MenuIcon? = null
        set(value) { field = value; invalidate() }


    var circle: Boolean = false
        set(value) { field = value; requestLayout(); rebuildShape() }
    var circleSizeDp: Float = 34f
        set(value) { field = value; requestLayout() }

    private var shapeStyle = BubbleShapeStyle.ARCANE_GLOW
    private var ink = Color.BLACK
    private var faceColor = Color.WHITE
    private var contentColor = Color.BLACK
    private val depthPx = 3.5f * d

    private var press = 0f
    private var pressAnim: ValueAnimator? = null

    private val shape = Path()
    private val rect = RectF()
    private val facePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL_AND_STROKE
        strokeJoin = Paint.Join.ROUND
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, resources.displayMetrics)
        ResourcesCompat.getFont(context, R.font.fredoka_semibold)?.let { typeface = it }
    }

    init { isClickable = true }

    fun setTheme(spec: MascotThemeSpec, fill: Int, textColor: Int) {
        shapeStyle = spec.shapeStyle
        ink = spec.secondaryColor.toInt()
        faceColor = fill
        contentColor = textColor
        rebuildShape()
        invalidate()
    }


    fun setColors(fill: Int, textColor: Int) {
        faceColor = fill
        contentColor = textColor
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val hDesired = ((if (circle) circleSizeDp else 44f) * d + depthPx).toInt()
        val wDesired = if (circle) hDesired else (160f * d).toInt()
        setMeasuredDimension(
            resolveSize(wDesired, widthMeasureSpec),
            resolveSize(hDesired, heightMeasureSpec)
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildShape()
    }

    private fun rebuildShape() {
        val bw = width - depthPx
        val bh = height - depthPx
        if (bw <= 0f || bh <= 0f) return
        shape.reset()
        rect.set(0f, 0f, bw, bh)
        if (circle) {
            shape.addOval(rect, Path.Direction.CW)
        } else when (shapeStyle) {
            BubbleShapeStyle.ARCANE_GLOW -> shape.addRoundRect(rect, bh / 2f, bh / 2f, Path.Direction.CW)
            BubbleShapeStyle.ORGANIC_LEAF -> {
                val big = bh * 0.5f
                val small = bh * 0.16f
                shape.addRoundRect(
                    rect,
                    floatArrayOf(big, big, small, small, big, big, small, small),
                    Path.Direction.CW
                )
            }
            BubbleShapeStyle.ANGULAR_SHIELD -> {
                val c = 6f * d
                shape.moveTo(c, 0f); shape.lineTo(bw - c, 0f); shape.lineTo(bw, c)
                shape.lineTo(bw, bh - c); shape.lineTo(bw - c, bh); shape.lineTo(c, bh)
                shape.lineTo(0f, bh - c); shape.lineTo(0f, c); shape.close()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (shape.isEmpty) rebuildShape()
        val bw = width - depthPx
        val bh = height - depthPx
        val strokeW = (if (shapeStyle == BubbleShapeStyle.ANGULAR_SHIELD) 3f else 2.2f) * d
        val shift = press * depthPx


        shadowPaint.color = ink
        shadowPaint.strokeWidth = strokeW
        var saved = canvas.save()
        canvas.translate(depthPx, depthPx)
        canvas.drawPath(shape, shadowPaint)
        canvas.restoreToCount(saved)


        saved = canvas.save()
        canvas.translate(shift, shift)
        facePaint.color = faceColor
        canvas.drawPath(shape, facePaint)
        strokePaint.color = ink
        strokePaint.strokeWidth = strokeW
        canvas.drawPath(shape, strokePaint)


        val iconSize = (if (circle) 16f else 20f) * d
        val gap = 8f * d
        val hasIcon = icon != null
        var textW = if (label.isEmpty()) 0f else textPaint.measureText(label)
        val reserved = (if (hasIcon) iconSize + gap else 0f) + 24f * d
        textPaint.textScaleX = 1f
        if (textW > bw - reserved && textW > 0f) {
            textPaint.textScaleX = ((bw - reserved) / textW).coerceAtLeast(0.7f)
            textW = textPaint.measureText(label)
        }
        val total = (if (hasIcon) iconSize else 0f) + (if (hasIcon && textW > 0f) gap else 0f) + textW
        var x = (bw - total) / 2f
        icon?.let {
            MenuIcons.draw(canvas, it, x, (bh - iconSize) / 2f, iconSize, contentColor)
            x += iconSize + gap
        }
        if (textW > 0f) {
            textPaint.color = contentColor
            val fm = textPaint.fontMetrics
            canvas.drawText(label, x, bh / 2f - (fm.ascent + fm.descent) / 2f, textPaint)
        }
        canvas.restoreToCount(saved)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                animatePress(1f, 80L, DecelerateInterpolator())
                return true
            }
            MotionEvent.ACTION_UP -> {
                animatePress(0f, 240L, OvershootInterpolator(3f))
                if (event.x in 0f..width.toFloat() && event.y in 0f..height.toFloat()) performClick()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                animatePress(0f, 160L, DecelerateInterpolator())
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun animatePress(target: Float, durationMs: Long, interpolator: Interpolator) {
        pressAnim?.cancel()
        pressAnim = ValueAnimator.ofFloat(press, target).apply {
            duration = durationMs
            this.interpolator = interpolator
            addUpdateListener {
                press = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        pressAnim?.cancel()
        super.onDetachedFromWindow()
    }
}
