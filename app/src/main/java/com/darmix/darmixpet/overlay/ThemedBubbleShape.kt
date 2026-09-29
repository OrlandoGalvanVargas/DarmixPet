package com.darmix.darmixpet.overlay

import android.graphics.Path
import android.graphics.RectF
import com.darmix.darmixpet.mascota.BubbleShapeStyle

enum class BubblePointerSide { NONE, LEFT, RIGHT, TOP, BOTTOM }

/** Construye el contorno (fondo + pico opcional) según el estilo del personaje activo. */
object ThemedBubbleShape {

    fun buildPath(
        width: Float,
        height: Float,
        density: Float,
        style: BubbleShapeStyle,
        pointerSide: BubblePointerSide
    ): Path {
        val radii: FloatArray = when (style) {
            BubbleShapeStyle.ARCANE_GLOW -> {
                val r = height / 2f
                floatArrayOf(r, r, r, r)
            }
            BubbleShapeStyle.ORGANIC_LEAF -> floatArrayOf(
                height * 0.55f, height * 0.18f, height * 0.55f, height * 0.18f
            )
            BubbleShapeStyle.ANGULAR_SHIELD -> {
                val r = 6f * density
                floatArrayOf(r, r, r, r)
            }
        }

        val path = Path()
        addRoundRectVariable(path, RectF(0f, 0f, width, height), radii)

        if (pointerSide != BubblePointerSide.NONE) {
            addTail(path, width, height, 9f * density, pointerSide)
        }
        return path
    }

    private fun addRoundRectVariable(path: Path, rect: RectF, radii: FloatArray) {
        val (tl, tr, br, bl) = radii
        path.moveTo(rect.left + tl, rect.top)
        path.lineTo(rect.right - tr, rect.top)
        path.quadTo(rect.right, rect.top, rect.right, rect.top + tr)
        path.lineTo(rect.right, rect.bottom - br)
        path.quadTo(rect.right, rect.bottom, rect.right - br, rect.bottom)
        path.lineTo(rect.left + bl, rect.bottom)
        path.quadTo(rect.left, rect.bottom, rect.left, rect.bottom - bl)
        path.lineTo(rect.left, rect.top + tl)
        path.quadTo(rect.left, rect.top, rect.left + tl, rect.top)
        path.close()
    }

    private fun addTail(path: Path, width: Float, height: Float, size: Float, side: BubblePointerSide) {
        val tail = Path()
        when (side) {
            BubblePointerSide.LEFT -> {
                val cy = height / 2f
                tail.moveTo(0f, cy - size); tail.lineTo(-size, cy); tail.lineTo(0f, cy + size); tail.close()
            }
            BubblePointerSide.RIGHT -> {
                val cy = height / 2f
                tail.moveTo(width, cy - size); tail.lineTo(width + size, cy); tail.lineTo(width, cy + size); tail.close()
            }
            BubblePointerSide.TOP -> {
                val cx = width / 2f
                tail.moveTo(cx - size, 0f); tail.lineTo(cx, -size); tail.lineTo(cx + size, 0f); tail.close()
            }
            BubblePointerSide.BOTTOM -> {
                val cx = width / 2f
                tail.moveTo(cx - size, height); tail.lineTo(cx, height + size); tail.lineTo(cx + size, height); tail.close()
            }
            BubblePointerSide.NONE -> {}
        }
        path.op(tail, Path.Op.UNION)
    }
}