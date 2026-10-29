package com.darmix.darmixpet.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.view.View
import android.view.animation.LinearInterpolator
import java.util.WeakHashMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min

class PetAuraView(context: Context, private val petSizePx: Int) : View(context) {


    companion object {
        private const val AURA_PADDING_DP = 28
        private const val BLUR_RADIUS_DP = 14f
        private const val PULSE_MS = 1600L
        private const val MIN_ALPHA = 0.45f
        private const val NOTIFICATION_AURA_COLOR = 0xFFFF3D3D.toInt()
    }

    private class Glow(val bitmap: Bitmap, val offsetX: Int, val offsetY: Int)

    private val d = resources.displayMetrics.density
    val paddingPx = (AURA_PADDING_DP * d).toInt()
    val windowSizePx: Int get() = petSizePx + 2 * paddingPx

    private val glowCache = WeakHashMap<Bitmap, Glow>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val matrix = Matrix()

    private var frame: Bitmap? = null
    private val color = NOTIFICATION_AURA_COLOR
    private var pulse = 1f
    private var animator: ValueAnimator? = null

    var isActive = false
        private set



    fun setFrame(bitmap: Bitmap) {
        frame = bitmap
        if (isActive) invalidate()
    }

    fun setActive(active: Boolean) {
        if (active == isActive) return
        isActive = active
        if (active) {
            visibility = VISIBLE
            startPulse()
        } else {
            stopPulse()
            visibility = INVISIBLE
        }
    }

    private fun glowFor(bitmap: Bitmap, scale: Float): Glow? {
        glowCache[bitmap]?.let { return it }
        return try {
            val source = if (bitmap.config == Bitmap.Config.HARDWARE) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false)
            } else bitmap
            val radius = (BLUR_RADIUS_DP * d / scale).coerceAtLeast(1f)
            val blurPaint = Paint().apply { maskFilter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL) }
            val offset = IntArray(2)
            val alpha = source.extractAlpha(blurPaint, offset)
            Glow(alpha, offset[0], offset[1]).also { glowCache[bitmap] = it }
        } catch (e: Exception) {
            null
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (!isActive) return
        val bitmap = frame ?: return
        if (bitmap.width <= 0 || bitmap.height <= 0) return

        val scale = min(petSizePx.toFloat() / bitmap.width, petSizePx.toFloat() / bitmap.height)
        val glow = glowFor(bitmap, scale) ?: return
        val dx = paddingPx + (petSizePx - bitmap.width * scale) / 2f
        val dy = paddingPx + (petSizePx - bitmap.height * scale) / 2f

        matrix.setScale(scale, scale)
        matrix.preTranslate(glow.offsetX.toFloat(), glow.offsetY.toFloat())
        matrix.postTranslate(dx, dy)

        paint.color = color
        paint.alpha = (255 * pulse).toInt()
        canvas.drawBitmap(glow.bitmap, matrix, paint)
        canvas.drawBitmap(glow.bitmap, matrix, paint)
    }

    private fun startPulse() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = PULSE_MS
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                val t = it.animatedValue as Float
                val wave = 0.5f - 0.5f * cos(2f * PI.toFloat() * t)
                pulse = MIN_ALPHA + (1f - MIN_ALPHA) * wave
                invalidate()
            }
            start()
        }
    }

    private fun stopPulse() {
        animator?.cancel()
        animator = null
    }

    override fun onDetachedFromWindow() {
        stopPulse()
        super.onDetachedFromWindow()
    }
}