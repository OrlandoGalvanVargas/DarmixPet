package com.darmix.darmixpet.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.ColorUtils
import com.darmix.darmixpet.mascota.BubbleShapeStyle
import com.darmix.darmixpet.mascota.MascotThemeSpec
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin


class StickerSlider(context: Context) : View(context) {

    private val d = resources.displayMetrics.density

    var max: Int = 100
    var progress: Int = 0
        set(value) { field = value.coerceIn(0, max); invalidate() }


    var onProgressChanged: ((Int, Boolean) -> Unit)? = null

    private var shapeStyle = BubbleShapeStyle.ARCANE_GLOW
    private var ink = Color.BLACK
    private var primary = Color.CYAN
    private var accent = Color.YELLOW

    private var grab = 0f
    private var grabAnim: ValueAnimator? = null

    private val padX = 16f * d
    private val trackH = 12f * d
    private val shadowOff = 2f * d

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val rect = RectF()
    private val trackPath = Path()
    private val thumbPath = Path()

    fun setTheme(spec: MascotThemeSpec) {
        shapeStyle = spec.shapeStyle
        ink = spec.secondaryColor.toInt()
        primary = spec.primaryColor.toInt()
        accent = spec.accentColor.toInt()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            resolveSize((140f * d).toInt(), widthMeasureSpec),
            resolveSize((38f * d).toInt(), heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        val cy = height / 2f - shadowOff / 2f
        val left = padX
        val right = width - padX
        val fraction = if (max > 0) progress / max.toFloat() else 0f
        val thumbX = left + (right - left) * fraction
        val corner = if (shapeStyle == BubbleShapeStyle.ANGULAR_SHIELD) 3f * d else trackH / 2f


        rect.set(left, cy - trackH / 2f, right, cy + trackH / 2f)
        trackPath.reset()
        trackPath.addRoundRect(rect, corner, corner, Path.Direction.CW)

        fill.color = ink
        var saved = canvas.save()
        canvas.translate(shadowOff, shadowOff)
        canvas.drawPath(trackPath, fill)
        canvas.restoreToCount(saved)

        fill.color = Color.WHITE
        canvas.drawPath(trackPath, fill)

        saved = canvas.save()
        canvas.clipPath(trackPath)
        fill.color = primary
        canvas.drawRect(left, cy - trackH / 2f, thumbX, cy + trackH / 2f, fill)
        canvas.restoreToCount(saved)

        stroke.color = ink
        stroke.strokeWidth = 2f * d
        canvas.drawPath(trackPath, stroke)


        val scale = 1f + 0.18f * grab
        val r = 11f * d * scale

        if (shapeStyle == BubbleShapeStyle.ARCANE_GLOW && grab > 0f) {
            fill.color = ColorUtils.setAlphaComponent(primary, (90 * grab).toInt())
            canvas.drawCircle(thumbX, cy, r + 6f * d, fill)
        }


        buildThumb(thumbX + shadowOff, cy + shadowOff, r)
        fill.color = ink
        canvas.drawPath(thumbPath, fill)
        stroke.strokeWidth = 2.4f * d
        canvas.drawPath(thumbPath, stroke)


        buildThumb(thumbX, cy, r)
        fill.color = Color.WHITE
        canvas.drawPath(thumbPath, fill)
        stroke.color = ink
        canvas.drawPath(thumbPath, stroke)


        fill.color = accent
        when (shapeStyle) {
            BubbleShapeStyle.ARCANE_GLOW -> canvas.drawCircle(thumbX, cy, 3.6f * d * scale, fill)
            BubbleShapeStyle.ORGANIC_LEAF -> {
                stroke.strokeWidth = 1.6f * d
                saved = canvas.save()
                canvas.translate(thumbX, cy)
                canvas.rotate(-25f)
                canvas.drawLine(-r * 0.55f, 0f, r * 0.55f, 0f, stroke)
                canvas.restoreToCount(saved)
            }
            BubbleShapeStyle.ANGULAR_SHIELD -> {
                val k = 3.6f * d * scale
                thumbPath.reset()
                thumbPath.moveTo(thumbX, cy - k); thumbPath.lineTo(thumbX + k, cy)
                thumbPath.lineTo(thumbX, cy + k); thumbPath.lineTo(thumbX - k, cy); thumbPath.close()
                canvas.drawPath(thumbPath, fill)
            }
        }
    }

    private fun buildThumb(cx: Float, cy: Float, r: Float) {
        thumbPath.reset()
        when (shapeStyle) {
            BubbleShapeStyle.ARCANE_GLOW -> thumbPath.addCircle(cx, cy, r, Path.Direction.CW)
            BubbleShapeStyle.ORGANIC_LEAF -> {

                val a = (-25f * PI / 180f).toFloat()
                val ca = cos(a)
                val sa = sin(a)
                fun px(x: Float, y: Float) = cx + x * ca - y * sa
                fun py(x: Float, y: Float) = cy + x * sa + y * ca
                thumbPath.moveTo(px(-r * 1.05f, 0f), py(-r * 1.05f, 0f))
                thumbPath.quadTo(px(0f, -r * 1.05f), py(0f, -r * 1.05f), px(r * 1.05f, 0f), py(r * 1.05f, 0f))
                thumbPath.quadTo(px(0f, r * 1.05f), py(0f, r * 1.05f), px(-r * 1.05f, 0f), py(-r * 1.05f, 0f))
                thumbPath.close()
            }
            BubbleShapeStyle.ANGULAR_SHIELD -> {
                for (i in 0 until 8) {
                    val a = (22.5f + 45f * i) * PI.toFloat() / 180f
                    val x = cx + r * 1.05f * cos(a)
                    val y = cy + r * 1.05f * sin(a)
                    if (i == 0) thumbPath.moveTo(x, y) else thumbPath.lineTo(x, y)
                }
                thumbPath.close()
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                animateGrab(1f)
                updateFrom(event.x)
            }
            MotionEvent.ACTION_MOVE -> updateFrom(event.x)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> animateGrab(0f)
        }
        return true
    }

    private fun updateFrom(x: Float) {
        val span = width - 2f * padX
        if (span <= 0f) return
        val fraction = ((x - padX) / span).coerceIn(0f, 1f)
        val value = (fraction * max).roundToInt()
        if (value != progress) {
            progress = value
            onProgressChanged?.invoke(value, true)
        }
    }

    private fun animateGrab(target: Float) {
        grabAnim?.cancel()
        grabAnim = ValueAnimator.ofFloat(grab, target).apply {
            duration = 140
            addUpdateListener {
                grab = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        grabAnim?.cancel()
        super.onDetachedFromWindow()
    }
}
