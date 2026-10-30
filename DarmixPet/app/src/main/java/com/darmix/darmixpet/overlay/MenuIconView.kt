package com.darmix.darmixpet.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.core.graphics.ColorUtils
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import androidx.core.graphics.PathParser

enum class MenuIcon { CLOCK, SUN, SPEAKER, HAT, GEAR, MOON, CHEVRON_LEFT, CHEVRON_RIGHT, CLOSE, LOCK, HOURGLASS, BELL, TRASH }
private object GhostPaths {
    val body: Path by lazy {
        PathParser.createPathFromPathData(
            "M33,66 C33,57.94 42.6,53 54,53 C65.4,53 75,57.94 75,66 L75,78 " +
                    "C75,81.87 71.87,85 68,85 C64.13,85 61,81.87 61,78 " +
                    "C61,81.87 57.87,85 54,85 C50.13,85 47,81.87 47,78 " +
                    "C47,81.87 43.87,85 40,85 C36.13,85 33,81.87 33,78 Z"
        )
    }
    val eyes: Path by lazy {
        PathParser.createPathFromPathData(
            "M42.3,64 a3.2,4.2 0 1,0 6.4,0 a3.2,4.2 0 1,0 -6.4,0 Z " +
                    "M59.3,64 a3.2,4.2 0 1,0 6.4,0 a3.2,4.2 0 1,0 -6.4,0 Z"
        )
    }
    val smile: Path by lazy { PathParser.createPathFromPathData("M49.5,71 Q54,75.4 58.5,71") }
    val brim: Path by lazy {
        PathParser.createPathFromPathData(
            "M28,45 C28,41.91 39.64,39.4 54,39.4 C68.36,39.4 80,41.91 80,45 " +
                    "C80,48.09 68.36,50.6 54,50.6 C39.64,50.6 28,48.09 28,45 Z"
        )
    }
    val hat: Path by lazy {
        PathParser.createPathFromPathData("M34,45 C38,34 48,24 59,17.5 C62,27 70,36 74,45 C66,48 42,48 34,45 Z")
    }
    val hatWithStar: Path by lazy {
        PathParser.createPathFromPathData(
            "M34,45 C38,34 48,24 59,17.5 C62,27 70,36 74,45 C66,48 42,48 34,45 Z " +
                    "M55.5,26.5 Q55.5,31.5 60.5,31.5 Q55.5,31.5 55.5,36.5 " +
                    "Q55.5,31.5 50.5,31.5 Q55.5,31.5 55.5,26.5 Z"
        ).apply { fillType = Path.FillType.EVEN_ODD }
    }
}
object MenuIcons {
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val wash = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val path = Path()
    private val tmp = Path()
    private val solid = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val hatWash = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    fun draw(canvas: Canvas, icon: MenuIcon, left: Float, top: Float, size: Float, color: Int, level: Int = 100) {
        val saved = canvas.save()
        canvas.translate(left, top)
        val s = size / 24f
        canvas.scale(s, s)
        stroke.color = color
        stroke.strokeWidth = 1.9f
        wash.color = color
        wash.alpha = (Color.alpha(color) * 0.27f).toInt()

        when (icon) {
            MenuIcon.CLOCK -> {
                path.reset(); path.addCircle(12f, 13f, 8f, Path.Direction.CW); shape(canvas, true)
                path.reset(); path.moveTo(12f, 8.6f); path.lineTo(12f, 13f); path.lineTo(15f, 14.8f); shape(canvas, false)
                path.reset()
                path.moveTo(10f, 2.8f); path.lineTo(14f, 2.8f)
                path.moveTo(12f, 2.8f); path.lineTo(12f, 5f)
                shape(canvas, false)
            }
            MenuIcon.SUN -> {
                path.reset(); path.addCircle(12f, 12f, 4.2f, Path.Direction.CW); shape(canvas, true)
                path.reset()
                for (i in 0 until 8) {
                    val a = i * PI.toFloat() / 4f
                    path.moveTo(12f + 7.4f * cos(a), 12f + 7.4f * sin(a))
                    path.lineTo(12f + 9.6f * cos(a), 12f + 9.6f * sin(a))
                }
                shape(canvas, false)
            }
            MenuIcon.SPEAKER -> {
                path.reset()
                path.moveTo(4f, 9.5f); path.lineTo(8f, 9.5f); path.lineTo(13f, 5.5f)
                path.lineTo(13f, 18.5f); path.lineTo(8f, 14.5f); path.lineTo(4f, 14.5f); path.close()
                shape(canvas, true)
                path.reset()
                when {
                    level <= 0 -> {
                        path.moveTo(16.2f, 9.6f); path.lineTo(21f, 14.4f)
                        path.moveTo(21f, 9.6f); path.lineTo(16.2f, 14.4f)
                    }
                    else -> {
                        path.moveTo(16f, 9.5f); path.quadTo(18.4f, 12f, 16f, 14.5f)
                        if (level > 45) { path.moveTo(18.6f, 7f); path.quadTo(22.4f, 12f, 18.6f, 17f) }
                    }
                }
                shape(canvas, false)
            }
            MenuIcon.HAT -> drawGhost(canvas, color)
            MenuIcon.GEAR -> {
                path.reset()
                for (i in 0 until 8) {
                    val c = i * (PI.toFloat() / 4f)
                    val a = floatArrayOf(c - 0.30f, c - 0.20f, c + 0.20f, c + 0.30f)
                    val r = floatArrayOf(6.6f, 9.3f, 9.3f, 6.6f)
                    for (j in 0..3) {
                        val x = 12f + r[j] * cos(a[j])
                        val y = 12f + r[j] * sin(a[j])
                        if (i == 0 && j == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                }
                path.close()
                shape(canvas, true)
                path.reset(); path.addCircle(12f, 12f, 3.2f, Path.Direction.CW); shape(canvas, false)
            }
            MenuIcon.MOON -> {
                path.reset(); path.addCircle(12f, 12f, 8.5f, Path.Direction.CW)
                tmp.reset(); tmp.addCircle(16.5f, 9f, 7f, Path.Direction.CW)
                path.op(tmp, Path.Op.DIFFERENCE)
                shape(canvas, true)
            }
            MenuIcon.CHEVRON_LEFT -> {
                stroke.strokeWidth = 2.8f
                path.reset(); path.moveTo(15f, 5f); path.lineTo(8f, 12f); path.lineTo(15f, 19f); shape(canvas, false)
            }
            MenuIcon.CHEVRON_RIGHT -> {
                stroke.strokeWidth = 2.8f
                path.reset(); path.moveTo(9f, 5f); path.lineTo(16f, 12f); path.lineTo(9f, 19f); shape(canvas, false)
            }


            MenuIcon.LOCK -> {

                path.reset()
                path.addRoundRect(5.5f, 10.5f, 18.5f, 20.5f, 3f, 3f, Path.Direction.CW)
                shape(canvas, true)

                path.reset()
                path.moveTo(8.5f, 10.5f)
                path.lineTo(8.5f, 7.5f)
                path.cubicTo(8.5f, 4f, 15.5f, 4f, 15.5f, 7.5f)
                path.lineTo(15.5f, 10.5f)
                shape(canvas, false)

                path.reset()
                path.moveTo(12f, 14.5f); path.lineTo(12f, 17f)
                shape(canvas, false)
            }
            MenuIcon.HOURGLASS -> {

                path.reset()
                path.moveTo(8.5f, 20.5f); path.lineTo(8.5f, 18f)
                path.cubicTo(8.5f, 16.8f, 10.5f, 15.6f, 12f, 14.5f)
                path.cubicTo(13.5f, 15.6f, 15.5f, 16.8f, 15.5f, 18f)
                path.lineTo(15.5f, 20.5f); path.close()
                shape(canvas, true)

                path.reset()
                path.moveTo(6.5f, 3.5f); path.lineTo(17.5f, 3.5f)
                path.moveTo(6.5f, 20.5f); path.lineTo(17.5f, 20.5f)
                shape(canvas, false)

                path.reset()
                path.moveTo(7.5f, 3.5f); path.lineTo(7.5f, 7f)
                path.cubicTo(7.5f, 9f, 10f, 10.5f, 12f, 12f)
                path.cubicTo(10f, 13.5f, 7.5f, 15f, 7.5f, 17f)
                path.lineTo(7.5f, 20.5f)
                shape(canvas, false)
                path.reset()
                path.moveTo(16.5f, 3.5f); path.lineTo(16.5f, 7f)
                path.cubicTo(16.5f, 9f, 14f, 10.5f, 12f, 12f)
                path.cubicTo(14f, 13.5f, 16.5f, 15f, 16.5f, 17f)
                path.lineTo(16.5f, 20.5f)
                shape(canvas, false)
            }

            MenuIcon.BELL -> {
                path.reset()
                path.moveTo(12f, 3.5f)
                path.cubicTo(8.7f, 3.5f, 7f, 6f, 7f, 9f)
                path.lineTo(7f, 12.5f)
                path.cubicTo(7f, 14.5f, 5.8f, 15.6f, 5f, 16.5f)
                path.lineTo(19f, 16.5f)
                path.cubicTo(18.2f, 15.6f, 17f, 14.5f, 17f, 12.5f)
                path.lineTo(17f, 9f)
                path.cubicTo(17f, 6f, 15.3f, 3.5f, 12f, 3.5f)
                path.close()
                shape(canvas, true)
                path.reset()
                path.moveTo(10f, 19.5f)
                path.cubicTo(10.4f, 20.6f, 11.1f, 21f, 12f, 21f)
                path.cubicTo(12.9f, 21f, 13.6f, 20.6f, 14f, 19.5f)
                shape(canvas, false)
            }
            MenuIcon.TRASH -> {
                path.reset()
                path.moveTo(6.5f, 7.5f); path.lineTo(7.6f, 19f)
                path.cubicTo(7.7f, 20f, 8.4f, 20.5f, 9.3f, 20.5f)
                path.lineTo(14.7f, 20.5f)
                path.cubicTo(15.6f, 20.5f, 16.3f, 20f, 16.4f, 19f)
                path.lineTo(17.5f, 7.5f); path.close()
                shape(canvas, true)
                path.reset()
                path.moveTo(4.5f, 7.5f); path.lineTo(19.5f, 7.5f)
                path.moveTo(9.5f, 7.5f); path.lineTo(9.5f, 5f)
                path.cubicTo(9.5f, 4.2f, 10f, 3.5f, 10.8f, 3.5f)
                path.lineTo(13.2f, 3.5f)
                path.cubicTo(14f, 3.5f, 14.5f, 4.2f, 14.5f, 5f)
                path.lineTo(14.5f, 7.5f)
                path.moveTo(10.5f, 11f); path.lineTo(10.7f, 17f)
                path.moveTo(13.5f, 11f); path.lineTo(13.3f, 17f)
                shape(canvas, false)
            }

            MenuIcon.CLOSE -> {
                stroke.strokeWidth = 2.8f
                path.reset()
                path.moveTo(6f, 6f); path.lineTo(18f, 18f)
                path.moveTo(18f, 6f); path.lineTo(6f, 18f)
                shape(canvas, false)
            }
        }
        canvas.restoreToCount(saved)
    }

    private fun shape(canvas: Canvas, withWash: Boolean) {
        if (withWash) canvas.drawPath(path, wash)
        canvas.drawPath(path, stroke)
    }

    private fun drawGhost(canvas: Canvas, color: Int) {
        val saved = canvas.save()
        canvas.scale(24f / 76f, 24f / 76f)
        canvas.translate(-16f, -13f)

        solid.color = color
        hatWash.color = color
        hatWash.alpha = (Color.alpha(color) * 0.55f).toInt()

        stroke.strokeWidth = 5f
        canvas.drawPath(GhostPaths.body, wash)
        canvas.drawPath(GhostPaths.body, stroke)
        canvas.drawPath(GhostPaths.eyes, solid)

        stroke.strokeWidth = 4.4f
        canvas.drawPath(GhostPaths.smile, stroke)

        stroke.strokeWidth = 5f
        canvas.drawPath(GhostPaths.brim, hatWash)
        canvas.drawPath(GhostPaths.brim, stroke)
        canvas.drawPath(GhostPaths.hatWithStar, hatWash)
        canvas.drawPath(GhostPaths.hat, stroke)

        canvas.restoreToCount(saved)
    }
}


class MenuIconView(context: Context, icon: MenuIcon) : View(context) {
    var icon: MenuIcon = icon
        set(value) { field = value; invalidate() }
    var tint: Int = Color.BLACK
        set(value) { field = value; invalidate() }
    var level: Int = 100
        set(value) { field = value; invalidate() }

    override fun onDraw(canvas: Canvas) {
        val size = min(width, height).toFloat()
        MenuIcons.draw(canvas, icon, (width - size) / 2f, (height - size) / 2f, size, tint, level)
    }
}


class MenuTabView(context: Context, private val icon: MenuIcon) : View(context) {
    private val d = resources.displayMetrics.density
    private var ink = Color.BLACK
    private var bubble = Color.WHITE
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    var isCurrent: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            if (value) pop()
            invalidate()
        }

    init { isClickable = true }

    fun setTheme(ink: Int, bubble: Int) {
        this.ink = ink
        this.bubble = bubble
        invalidate()
    }

    private fun pop() {
        animate().cancel()
        scaleX = 0.8f
        scaleY = 0.8f
        animate().scaleX(1f).scaleY(1f).setDuration(260).setInterpolator(OvershootInterpolator(3f)).start()
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = min(width, height) / 2f - 3f * d
        if (isCurrent) {
            fillPaint.color = ink
            canvas.drawCircle(cx + 1.5f * d, cy + 1.5f * d, r, fillPaint)
            fillPaint.color = bubble
            canvas.drawCircle(cx, cy, r, fillPaint)
            strokePaint.color = ink
            strokePaint.strokeWidth = 2f * d
            canvas.drawCircle(cx, cy, r, strokePaint)
        }
        val size = 20f * d
        val color = if (isCurrent) ink else ColorUtils.setAlphaComponent(ink, 150)
        MenuIcons.draw(canvas, icon, cx - size / 2f, cy - size / 2f, size, color)
    }
}
