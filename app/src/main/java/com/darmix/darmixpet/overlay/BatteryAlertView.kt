package com.darmix.darmixpet.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import com.darmix.darmixpet.R
import com.darmix.darmixpet.mascota.BubbleShapeStyle
import com.darmix.darmixpet.mascota.MascotThemeSpec
import kotlin.math.PI
import kotlin.math.cos

/**
 * Alerta persistente de batería baja. Mantiene la API anterior (setTheme, setPercentText,
 * measureSelf, startBlink, stopBlink) pero:
 *  - en vez de parpadear entera (poco legible), late suavemente con un halo de aviso;
 *  - muestra una pila dibujada que se llena según el porcentaje, con un rayito;
 *  - habla con la voz del personaje y se vuelve más urgente por debajo del 10%;
 *  - tiene una "x" para indicar que se puede cerrar tocándola;
 *  - apunta a la mascota (setPointerSide).
 */
class BatteryAlertView(
    context: Context,
    private val onDismiss: () -> Unit
) : FrameLayout(context) {

    private val d = resources.displayMetrics.density
    private val painter = StickerPainter(context)

    private companion object {
        /** Distancia deseada entre la mascota y el cuerpo de la alerta (el Quick Menu usa 14). */
        const val BODY_GAP_DP = 10

        /** Hueco que ya deja computeAnchoredPosition en el servicio (BUBBLE_GAP_DP). */
        const val SERVICE_GAP_DP = 4
        const val TAIL_SCALE = 0.75f
    }

    private val percentLabel: TextView
    private val messageLabel: TextView
    private val column: LinearLayout

    private var theme: MascotThemeSpec? = null
    private var pointerSide: BubblePointerSide = BubblePointerSide.NONE
    private var percent = 100

    private var tick = 0f
    private var animator: ValueAnimator? = null

    private val iconFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val iconStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val rect = RectF()
    private val bolt = Path()

    init {
        setWillNotDraw(false)

        percentLabel = TextView(context).apply { textSize = 21f }
        messageLabel = TextView(context).apply { textSize = 12.5f }
        column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(percentLabel)
            addView(messageLabel)
        }
        addView(column, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))

        isClickable = true
        setOnClickListener { onDismiss() }
    }

    fun setTheme(spec: MascotThemeSpec) {
        val changed = spec != theme
        theme = spec
        if (changed) {
            painter.setTheme(
                spec,
                fill = ColorUtils.blendARGB(Color.parseColor("#FFF3E0"), spec.primaryColor.toInt(), 0.10f)
            )
            // Pico más corto y margen justo: el aviso queda pegado a la mascota.
            painter.tailScale = TAIL_SCALE
            painter.insetOverridePx = painter.tailReachPx + 3f * d
            val inset = painter.insetPx.toInt()
            setPadding(inset, inset, inset, inset)

            column.setPadding(
                ((12 + 31 + 9) * d + painter.extraLeftPaddingPx).toInt(),
                (9 * d).toInt(),
                (16 * d).toInt(),
                (9 * d).toInt()
            )
            percentLabel.setTextColor(painter.ink)
            messageLabel.setTextColor(ColorUtils.setAlphaComponent(painter.ink, 225))
            ResourcesCompat.getFont(context, R.font.fredoka_bold)?.let { percentLabel.typeface = it }
            ResourcesCompat.getFont(context, R.font.nunito_semibold)?.let { messageLabel.typeface = it }
        }
        updateTexts()
        requestLayout()
        invalidate()
    }

    fun setPointerSide(side: BubblePointerSide) {
        if (pointerSide == side) return
        pointerSide = side
        invalidate()
    }

    fun setPercentText(percent: Int) {
        this.percent = percent.coerceIn(0, 100)
        updateTexts()
        animator?.duration = if (isCritical()) 1000L else 1600L
        requestLayout()
        invalidate()
    }

    fun measureSelf(): Pair<Int, Int> {
        measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        return measuredWidth to measuredHeight
    }

    fun startBlink() {
        visibility = View.VISIBLE
        alpha = 1f
        if (animator?.isRunning == true) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = if (isCritical()) 1000L else 1600L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                tick = it.animatedValue as Float
                val pulse = 0.5f - 0.5f * cos(2f * PI.toFloat() * tick)
                val s = 1f + 0.035f * pulse
                pivotX = width / 2f
                pivotY = height / 2f
                scaleX = s
                scaleY = s
                invalidate()
            }
            start()
        }
    }

    fun stopBlink() {
        animator?.cancel()
        animator = null
        scaleX = 1f
        scaleY = 1f
        alpha = 1f
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    /**
     * Posición final de la ventana: acerca el cuerpo de la alerta a la mascota (BODY_GAP_DP)
     * compensando el margen transparente del pico. Úsala en lugar de anchored.x / anchored.y.
     */
    fun nudgedPosition(x: Int, y: Int, side: BubblePointerSide): Pair<Int, Int> {
        val nudge = (painter.insetPx + SERVICE_GAP_DP * d - BODY_GAP_DP * d).coerceAtLeast(0f).toInt()
        return when (side) {
            BubblePointerSide.LEFT -> (x - nudge) to y
            BubblePointerSide.RIGHT -> (x + nudge) to y
            BubblePointerSide.TOP -> x to (y - nudge)
            BubblePointerSide.BOTTOM -> x to (y + nudge)
            BubblePointerSide.NONE -> x to y
        }
    }

    // ───────────────────────── Contenido ─────────────────────────

    private fun isCritical() = percent <= 10

    private fun warnColor(): Int =
        if (isCritical()) Color.parseColor("#C96262") else Color.parseColor("#E8A33D")

    private fun updateTexts() {
        val style = theme?.shapeStyle ?: BubbleShapeStyle.ARCANE_GLOW
        percentLabel.text = "$percent%"
        messageLabel.text = when (style) {
            BubbleShapeStyle.ARCANE_GLOW -> if (isCritical()) "¡Energía crítica!" else "Recarga tu energía"
            BubbleShapeStyle.ORGANIC_LEAF -> if (isCritical()) "¡Recarga urgente!" else "Hora de recargar"
            BubbleShapeStyle.ANGULAR_SHIELD -> if (isCritical()) "¡Batería crítica!" else "¡A cargar, valiente!"
        }
        contentDescription = "Batería baja, $percent por ciento. Toca para cerrar."
    }

    // ───────────────────────── Dibujo ─────────────────────────

    override fun dispatchDraw(canvas: Canvas) {
        val t = theme
        val inset = painter.insetPx
        val bw = width - 2 * inset
        val bh = height - 2 * inset

        if (t != null) {
            val pulse = 0.5f - 0.5f * cos(2f * PI.toFloat() * tick)
            painter.draw(
                canvas = canvas,
                left = inset,
                top = inset,
                w = bw,
                h = bh,
                side = pointerSide,
                t = tick,
                haloColor = warnColor(),
                haloAlpha = 0.45f + 0.55f * pulse
            )
        }
        super.dispatchDraw(canvas)

        if (t != null) {
            drawBatteryIcon(canvas, inset + 12f * d + painter.extraLeftPaddingPx, inset + bh / 2f)
            drawCloseBadge(canvas, inset + bw - 1f * d, inset + 1f * d)
        }
    }

    /** Pila horizontal con nivel de carga y un rayito que titila. */
    private fun drawBatteryIcon(canvas: Canvas, left: Float, cy: Float) {
        val w = 28f * d
        val h = 16f * d
        val ink = painter.ink

        // Cuerpo
        rect.set(left, cy - h / 2f, left + w, cy + h / 2f)
        iconFill.color = Color.WHITE
        canvas.drawRoundRect(rect, 4f * d, 4f * d, iconFill)

        // Nivel
        val innerW = (w - 5f * d) * (percent / 100f).coerceAtLeast(0.12f)
        rect.set(left + 2.5f * d, cy - h / 2f + 2.5f * d, left + 2.5f * d + innerW, cy + h / 2f - 2.5f * d)
        iconFill.color = warnColor()
        canvas.drawRoundRect(rect, 2f * d, 2f * d, iconFill)

        // Contorno y terminal
        iconStroke.color = ink
        iconStroke.strokeWidth = 2f * d
        rect.set(left, cy - h / 2f, left + w, cy + h / 2f)
        canvas.drawRoundRect(rect, 4f * d, 4f * d, iconStroke)
        canvas.drawLine(left + w + 2.5f * d, cy - 3f * d, left + w + 2.5f * d, cy + 3f * d, iconStroke)

        // Rayito
        val pulse = 0.5f - 0.5f * cos(2f * PI.toFloat() * tick)
        val bx = left + w / 2f
        bolt.reset()
        bolt.moveTo(bx + 1.5f * d, cy - 6f * d)
        bolt.lineTo(bx - 3.5f * d, cy + 0.8f * d)
        bolt.lineTo(bx - 0.3f * d, cy + 0.8f * d)
        bolt.lineTo(bx - 1.5f * d, cy + 6f * d)
        bolt.lineTo(bx + 3.5f * d, cy - 1f * d)
        bolt.lineTo(bx + 0.3f * d, cy - 1f * d)
        bolt.close()
        iconFill.color = ColorUtils.setAlphaComponent(Color.parseColor("#FFD54F"), (140 + 115 * pulse).toInt())
        canvas.drawPath(bolt, iconFill)
        iconStroke.strokeWidth = 1.1f * d
        iconStroke.color = ColorUtils.setAlphaComponent(ink, (120 + 135 * pulse).toInt())
        canvas.drawPath(bolt, iconStroke)
    }

    /** Botoncito "x" en la esquina: indica que se cierra tocando. */
    private fun drawCloseBadge(canvas: Canvas, cx: Float, cy: Float) {
        val r = 8f * d
        iconFill.color = Color.WHITE
        canvas.drawCircle(cx, cy, r, iconFill)
        iconStroke.color = painter.ink
        iconStroke.strokeWidth = 2f * d
        canvas.drawCircle(cx, cy, r, iconStroke)
        val a = 3.2f * d
        iconStroke.strokeWidth = 2f * d
        canvas.drawLine(cx - a, cy - a, cx + a, cy + a, iconStroke)
        canvas.drawLine(cx + a, cy - a, cx - a, cy + a, iconStroke)
    }
}
