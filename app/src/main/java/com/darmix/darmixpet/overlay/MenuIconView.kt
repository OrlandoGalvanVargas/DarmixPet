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

enum class MenuIcon { CLOCK, SUN, SPEAKER, HAT, GEAR, MOON, CHEVRON_LEFT, CHEVRON_RIGHT, CLOSE,LOCK, HOURGLASS  }


object MenuIcons {
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val wash = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val path = Path()
    private val tmp = Path()


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
            MenuIcon.HAT -> {
                path.reset()
                path.moveTo(12f, 3f)
                path.cubicTo(11f, 7f, 9.4f, 11f, 7.3f, 16.2f)
                path.cubicTo(9.6f, 17f, 14.4f, 17f, 16.7f, 16.2f)
                path.cubicTo(15f, 12f, 14f, 8f, 12f, 3f)
                path.close()
                shape(canvas, true)
                path.reset(); path.moveTo(8.3f, 13.4f); path.cubicTo(10.5f, 14.3f, 13.5f, 14.3f, 15.7f, 13.4f); shape(canvas, false)
                path.reset(); path.moveTo(3.2f, 17f); path.cubicTo(6f, 19.8f, 18f, 19.8f, 20.8f, 17f); shape(canvas, false)
            }
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
