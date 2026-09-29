package com.darmix.darmixpet.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.widget.FrameLayout
import android.widget.TextView
import com.darmix.darmixpet.mascota.BubbleShapeStyle
import com.darmix.darmixpet.mascota.MascotThemeSpec

/**
 * Bocadillo de texto temático: su forma, borde y (para Archi) resplandor
 * cambian según el MascotThemeSpec del personaje activo. El relleno se
 * mantiene blanco para que el texto siga siendo legible sin importar el tema.
 */
class SpeechBubbleView(context: Context) : FrameLayout(context) {

    private val label: TextView
    private var theme: MascotThemeSpec? = null
    private var pointerSide: BubblePointerSide = BubblePointerSide.NONE

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private var shapePath = Path()
    private val insetPx: Float

    init {
        alpha = 0f
        setWillNotDraw(false)
        val density = resources.displayMetrics.density
        insetPx = 9f * density
        setPadding(insetPx.toInt(), insetPx.toInt(), insetPx.toInt(), insetPx.toInt())

        label = TextView(context).apply {
            setTextColor(Color.parseColor("#3A3341"))
            textSize = 14f
            maxWidth = (200 * density).toInt()
            val h = (10 * density).toInt()
            val v = (8 * density).toInt()
            setPadding(h, v, h, v)
        }
        addView(label, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
    }

    fun setTheme(spec: MascotThemeSpec) {
        theme = spec
        val density = resources.displayMetrics.density
        strokePaint.strokeWidth = (if (spec.shapeStyle == BubbleShapeStyle.ANGULAR_SHIELD) 2.5f else 2f) * density
        if (spec.shapeStyle == BubbleShapeStyle.ARCANE_GLOW) {
            setLayerType(LAYER_TYPE_SOFTWARE, null)
            strokePaint.setShadowLayer(6f * density, 0f, 0f, spec.primaryColor.toInt())
        } else {
            setLayerType(LAYER_TYPE_NONE, null)
            strokePaint.clearShadowLayer()
        }
        shapePath = Path()
        invalidate()
    }

    fun setPointerSide(side: BubblePointerSide) {
        if (pointerSide == side) return
        pointerSide = side
        shapePath = Path()
        invalidate()
    }

    fun setMessage(text: String) {
        label.text = text
        requestLayout()
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
        val path = ThemedBubbleShape.buildPath(w, h, density, t.shapeStyle, pointerSide)
        path.offset(insetPx, insetPx)
        shapePath = path
    }

    override fun dispatchDraw(canvas: Canvas) {
        val t = theme
        if (t != null) {
            if (shapePath.isEmpty) rebuildPath()
            fillPaint.color = Color.WHITE
            canvas.drawPath(shapePath, fillPaint)
            strokePaint.color = t.primaryColor.toInt()
            canvas.drawPath(shapePath, strokePaint)
        }
        super.dispatchDraw(canvas)
    }
}