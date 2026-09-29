package com.darmix.darmixpet.overlay

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.darmix.darmixpet.mascota.MascotThemeSpec

class BatteryAlertView(
    context: Context,
    private val onDismiss: () -> Unit
) : FrameLayout(context) {

    private val label: TextView
    private var theme: MascotThemeSpec? = null
    private var pulseAnimator: ObjectAnimator? = null

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private var shapePath = Path()
    private val insetPx: Float

    init {
        setWillNotDraw(false)
        val density = resources.displayMetrics.density
        insetPx = 6f * density
        setPadding(insetPx.toInt(), insetPx.toInt(), insetPx.toInt(), insetPx.toInt())

        label = TextView(context).apply {
            setTextColor(Color.parseColor("#5C2323"))
            textSize = 15f
            val h = (14 * density).toInt()
            val v = (10 * density).toInt()
            setPadding(h, v, h, v)
        }
        addView(label, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))

        isClickable = true
        setOnClickListener { onDismiss() }
    }

    fun setTheme(spec: MascotThemeSpec) {
        theme = spec
        strokePaint.strokeWidth = 2f * resources.displayMetrics.density
        shapePath = Path()
        invalidate()
    }

    fun setPercentText(percent: Int) {
        label.text = "🔋 $percent%"
        requestLayout()
    }

    fun measureSelf(): Pair<Int, Int> {
        measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        return measuredWidth to measuredHeight
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        shapePath = Path()
    }

    private fun rebuildPath() {
        val t = theme ?: return
        val density = resources.displayMetrics.density
        val w = (width - 2 * insetPx).coerceAtLeast(0f)
        val h = (height - 2 * insetPx).coerceAtLeast(0f)
        val path = ThemedBubbleShape.buildPath(w, h, density, t.shapeStyle, BubblePointerSide.NONE)
        path.offset(insetPx, insetPx)
        shapePath = path
    }

    override fun dispatchDraw(canvas: Canvas) {
        val t = theme
        if (t != null) {
            if (shapePath.isEmpty) rebuildPath()
            fillPaint.color = Color.parseColor("#FFF3E0")
            canvas.drawPath(shapePath, fillPaint)
            strokePaint.color = t.accentColor.toInt()
            canvas.drawPath(shapePath, strokePaint)
        }
        super.dispatchDraw(canvas)
    }

    fun startBlink() {
        visibility = View.VISIBLE
        pulseAnimator?.cancel()
        pulseAnimator = ObjectAnimator.ofFloat(this, "alpha", 1f, 0.35f).apply {
            duration = 700
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
        }
    }

    fun stopBlink() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        alpha = 1f
    }
}