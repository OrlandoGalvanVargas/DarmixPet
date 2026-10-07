package com.darmix.darmixpet.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.core.graphics.ColorUtils
import com.darmix.darmixpet.mascota.BubbleShapeStyle
import com.darmix.darmixpet.mascota.MascotThemeSpec
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin


class StickerPainter(context: Context) {

    private val d = context.resources.displayMetrics.density

    var shapeStyle: BubbleShapeStyle = BubbleShapeStyle.ARCANE_GLOW
        private set
    var ink: Int = Color.BLACK
        private set
    private var primary: Int = Color.CYAN
    private var accent: Int = Color.YELLOW
    private var fillColor: Int = Color.WHITE


    val insetPx: Float
        get() = if (insetOverridePx > 0f) insetOverridePx
        else (if (shapeStyle == BubbleShapeStyle.ARCANE_GLOW) 18f else 14f) * d


    val extraLeftPaddingPx: Float
        get() = if (shapeStyle == BubbleShapeStyle.ANGULAR_SHIELD) 8f * d else 0f


    var tailScale: Float = 1f
        set(value) {
            field = value
            cachedStyle = null
        }


    var insetOverridePx: Float = 0f
        set(value) {
            field = value
            cachedStyle = null
        }


    val tailReachPx: Float
        get() = (when (shapeStyle) {
            BubbleShapeStyle.ARCANE_GLOW -> 19f
            BubbleShapeStyle.ORGANIC_LEAF -> 13f
            BubbleShapeStyle.ANGULAR_SHIELD -> 12f
        }) * tailScale * d

    private val strokePx: Float
        get() = (if (shapeStyle == BubbleShapeStyle.ANGULAR_SHIELD) 3.5f else 2.5f) * d
    private val shadowPx: Float get() = 3f * d

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL_AND_STROKE
        strokeJoin = Paint.Join.ROUND
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }
    private val decoFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val decoStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val rect = RectF()
    private val bodyPath = Path()
    private val tailPath = Path()
    private val outlinePath = Path()
    private val starPath = Path()
    private val leafPath = Path()
    private val matrix = Matrix()

    private var cachedW = -1f
    private var cachedH = -1f
    private var cachedStyle: BubbleShapeStyle? = null
    private var cachedSide: BubblePointerSide? = null
    private var cachedScale = -1f
    private val circlePts = FloatArray(4)
    private var hasCircles = false

    fun setTheme(spec: MascotThemeSpec, fill: Int = defaultFill(spec)) {
        shapeStyle = spec.shapeStyle
        ink = spec.secondaryColor.toInt()
        primary = spec.primaryColor.toInt()
        accent = spec.accentColor.toInt()
        fillColor = fill
        shadowPaint.strokeWidth = strokePx
        strokePaint.strokeWidth = strokePx
        cachedStyle = null
    }


    fun draw(
        canvas: Canvas,
        left: Float,
        top: Float,
        w: Float,
        h: Float,
        side: BubblePointerSide,
        t: Float,
        haloColor: Int = 0,
        haloAlpha: Float = 0f
    ) {
        if (w <= 0f || h <= 0f) return
        ensurePaths(w, h, side)

        val saved = canvas.save()
        canvas.translate(left, top)
        val wave = sin(2f * PI.toFloat() * t)


        if (haloColor != 0 && haloAlpha > 0f) {
            glowPaint.color = haloColor
            glowPaint.strokeWidth = 14f * d
            glowPaint.alpha = (55 * haloAlpha).toInt()
            canvas.drawPath(outlinePath, glowPaint)
            glowPaint.strokeWidth = 8f * d
            glowPaint.alpha = (95 * haloAlpha).toInt()
            canvas.drawPath(outlinePath, glowPaint)
        }


        if (shapeStyle == BubbleShapeStyle.ARCANE_GLOW) {
            val pulse = 0.75f + 0.25f * wave
            glowPaint.color = primary
            for (i in 0..2) {
                glowPaint.strokeWidth = (13f - i * 4f) * d
                glowPaint.alpha = ((45 + i * 28) * pulse).toInt()
                canvas.drawPath(outlinePath, glowPaint)
            }
        }


        val s = canvas.save()
        canvas.translate(shadowPx, shadowPx)
        shadowPaint.color = ink
        canvas.drawPath(outlinePath, shadowPaint)
        canvas.restoreToCount(s)


        fillPaint.color = fillColor
        canvas.drawPath(outlinePath, fillPaint)


        if (shapeStyle == BubbleShapeStyle.ANGULAR_SHIELD) {
            val c = canvas.save()
            canvas.clipPath(bodyPath)
            decoFill.color = accent
            canvas.drawRect(0f, 0f, 9f * d, h, decoFill)
            decoStroke.color = ink
            decoStroke.strokeWidth = 2f * d
            canvas.drawLine(9f * d, 0f, 9f * d, h, decoStroke)
            canvas.restoreToCount(c)
        }


        strokePaint.color = ink
        strokePaint.strokeWidth = strokePx
        canvas.drawPath(outlinePath, strokePaint)


        when (shapeStyle) {
            BubbleShapeStyle.ARCANE_GLOW -> drawArcaneExtras(canvas, w, h, t, wave)
            BubbleShapeStyle.ORGANIC_LEAF -> drawLeaves(canvas, w, h, wave)
            BubbleShapeStyle.ANGULAR_SHIELD -> drawRivets(canvas, w, h)
        }

        canvas.restoreToCount(saved)
    }



    private fun ensurePaths(w: Float, h: Float, side: BubblePointerSide) {
        if (w == cachedW && h == cachedH && shapeStyle == cachedStyle &&
            side == cachedSide && tailScale == cachedScale
        ) return
        cachedW = w; cachedH = h; cachedStyle = shapeStyle; cachedSide = side; cachedScale = tailScale

        buildBody(w, h)
        outlinePath.set(bodyPath)
        hasCircles = false

        if (side != BubblePointerSide.NONE) {
            setupTailMatrix(w, h, side)
            val td = d * tailScale
            when (shapeStyle) {
                BubbleShapeStyle.ARCANE_GLOW -> {

                    circlePts[0] = -8f * td; circlePts[1] = 0f
                    circlePts[2] = -15.5f * td; circlePts[3] = 0f
                    matrix.mapPoints(circlePts)
                    hasCircles = true
                }
                BubbleShapeStyle.ORGANIC_LEAF -> {
                    tailPath.reset()
                    tailPath.moveTo(1.5f * td, -6f * td)
                    tailPath.quadTo(-5f * td, -3f * td, -13f * td, 3f * td)
                    tailPath.quadTo(-5f * td, 5f * td, 1.5f * td, 6f * td)
                    tailPath.close()
                    tailPath.transform(matrix)
                    outlinePath.op(tailPath, Path.Op.UNION)
                }
                BubbleShapeStyle.ANGULAR_SHIELD -> {
                    tailPath.reset()
                    tailPath.moveTo(1.5f * td, -8f * td)
                    tailPath.lineTo(-12f * td, 0f)
                    tailPath.lineTo(1.5f * td, 8f * td)
                    tailPath.close()
                    tailPath.transform(matrix)
                    outlinePath.op(tailPath, Path.Op.UNION)
                }
            }
        }
    }

    private fun buildBody(w: Float, h: Float) {
        bodyPath.reset()
        rect.set(0f, 0f, w, h)
        when (shapeStyle) {
            BubbleShapeStyle.ARCANE_GLOW -> {
                val r = min(h / 2f, 22f * d)
                bodyPath.addRoundRect(rect, r, r, Path.Direction.CW)
            }
            BubbleShapeStyle.ORGANIC_LEAF -> {
                val big = min(h * 0.5f, 24f * d)
                val small = min(h * 0.16f, 7f * d)
                bodyPath.addRoundRect(
                    rect,
                    floatArrayOf(big, big, small, small, big, big, small, small),
                    Path.Direction.CW
                )
            }
            BubbleShapeStyle.ANGULAR_SHIELD -> {
                val c = 7f * d
                bodyPath.moveTo(c, 0f)
                bodyPath.lineTo(w - c, 0f)
                bodyPath.lineTo(w, c)
                bodyPath.lineTo(w, h - c)
                bodyPath.lineTo(w - c, h)
                bodyPath.lineTo(c, h)
                bodyPath.lineTo(0f, h - c)
                bodyPath.lineTo(0f, c)
                bodyPath.close()
            }
        }
    }


    private fun setupTailMatrix(w: Float, h: Float, side: BubblePointerSide) {
        val petCenter = 35f * d - insetPx
        val ty = min(h / 2f, petCenter).coerceAtLeast(12f * d).coerceAtMost(max(12f * d, h - 12f * d))
        when (side) {
            BubblePointerSide.LEFT -> matrix.setTranslate(0f, ty)
            BubblePointerSide.RIGHT -> { matrix.setScale(-1f, 1f); matrix.postTranslate(w, ty) }
            BubblePointerSide.TOP -> { matrix.setRotate(90f); matrix.postTranslate(w / 2f, 0f) }
            BubblePointerSide.BOTTOM -> { matrix.setRotate(-90f); matrix.postTranslate(w / 2f, h) }
            BubblePointerSide.NONE -> matrix.reset()
        }
    }



    private fun drawArcaneExtras(canvas: Canvas, w: Float, h: Float, t: Float, wave: Float) {

        if (hasCircles) {
            val radii = floatArrayOf(4.6f * d * tailScale, 2.8f * d * tailScale)
            strokePaint.strokeWidth = 2f * d
            fillPaint.color = fillColor
            for (i in 0..1) {
                canvas.drawCircle(circlePts[i * 2], circlePts[i * 2 + 1], radii[i], fillPaint)
                canvas.drawCircle(circlePts[i * 2], circlePts[i * 2 + 1], radii[i], strokePaint)
            }
        }

        val big = (6f + 1.8f * wave) * d
        val small = (3.6f + 1.4f * sin(2f * PI.toFloat() * (t + 0.5f))) * d
        drawStar(canvas, w - 5f * d, 3f * d, big)
        drawStar(canvas, 6f * d, h + 2f * d, small)
    }

    private fun drawStar(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        starPath.reset()
        val inner = r * 0.36f
        for (i in 0 until 8) {
            val rr = if (i % 2 == 0) r else inner
            val a = (PI / 4.0 * i - PI / 2.0).toFloat()
            val x = cx + rr * cos(a)
            val y = cy + rr * sin(a)
            if (i == 0) starPath.moveTo(x, y) else starPath.lineTo(x, y)
        }
        starPath.close()
        decoFill.color = accent
        canvas.drawPath(starPath, decoFill)
        decoStroke.color = ink
        decoStroke.strokeWidth = 1.3f * d
        canvas.drawPath(starPath, decoStroke)
    }

    private fun drawLeaves(canvas: Canvas, w: Float, h: Float, wave: Float) {
        drawLeaf(canvas, 14f * d, 1f * d, -42f + 7f * wave, 17f * d, 6.5f * d)
        drawLeaf(canvas, w - 12f * d, h - 1f * d, 138f + 7f * wave, 12f * d, 5f * d)
    }

    private fun drawLeaf(canvas: Canvas, x: Float, y: Float, angle: Float, len: Float, wd: Float) {
        val c = canvas.save()
        canvas.translate(x, y)
        canvas.rotate(angle)
        leafPath.reset()
        leafPath.moveTo(0f, 0f)
        leafPath.quadTo(len * 0.5f, -wd, len, 0f)
        leafPath.quadTo(len * 0.5f, wd, 0f, 0f)
        leafPath.close()
        decoFill.color = primary
        canvas.drawPath(leafPath, decoFill)
        decoStroke.color = ink
        decoStroke.strokeWidth = 1.6f * d
        canvas.drawPath(leafPath, decoStroke)
        decoStroke.strokeWidth = 1f * d
        canvas.drawLine(len * 0.18f, 0f, len * 0.78f, 0f, decoStroke)
        canvas.restoreToCount(c)
    }

    private fun drawRivets(canvas: Canvas, w: Float, h: Float) {
        val r = 1.8f * d
        decoFill.color = ColorUtils.setAlphaComponent(ink, 150)
        canvas.drawCircle(w - 7f * d, 7f * d, r, decoFill)
        canvas.drawCircle(w - 7f * d, h - 7f * d, r, decoFill)
        decoFill.color = Color.parseColor("#FFE8B0")
        canvas.drawCircle(4.5f * d, 7f * d, r, decoFill)
        canvas.drawCircle(4.5f * d, h - 7f * d, r, decoFill)
    }

    companion object {

        fun defaultFill(spec: MascotThemeSpec): Int =
            ColorUtils.blendARGB(Color.WHITE, spec.primaryColor.toInt(), 0.12f)
    }
}
