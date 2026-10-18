package com.darmix.darmixpet.overlay

import android.graphics.Rect
import kotlin.math.max
import kotlin.math.roundToInt

interface PetAnchoredElement {
    val anchorInsetPx: Float
    val anchorGapPx: Float
    fun applyAnchor(side: BubblePointerSide, tailOffsetPx: Float)
}

data class AnchorPlacement(
    val x: Int,
    val y: Int,
    val side: BubblePointerSide,
    val tailOffsetPx: Float
)

object AnchorPlacer {

    fun place(
        petX: Int,
        petY: Int,
        petSize: Int,
        area: Rect,
        width: Int,
        height: Int,
        insetPx: Float,
        gapPx: Float,
        tailClearancePx: Float
    ): AnchorPlacement {
        val w = width.toFloat()
        val h = height.toFloat()
        val left = area.left.toFloat()
        val top = area.top.toFloat()
        val right = area.right.toFloat()
        val bottom = area.bottom.toFloat()
        val maxX = max(left, right - w)
        val maxY = max(top, bottom - h)

        val petLeft = petX.toFloat()
        val petTop = petY.toFloat()
        val petRight = petLeft + petSize
        val petBottom = petTop + petSize
        val petCenterX = petLeft + petSize / 2f
        val petCenterY = petTop + petSize / 2f

        val tailMin = insetPx + tailClearancePx

        val sideY = petTop.coerceIn(top, maxY)
        val sideTail = petCenterY - sideY
        val sideFits = h <= bottom - top && sideTail >= tailMin && sideTail <= h - tailMin

        val rightX = petRight + gapPx - insetPx
        if (sideFits && rightX + w <= right) {
            return placement(rightX, sideY, BubblePointerSide.LEFT, sideTail)
        }

        val leftX = petLeft - gapPx - w + insetPx
        if (sideFits && leftX >= left) {
            return placement(leftX, sideY, BubblePointerSide.RIGHT, sideTail)
        }

        val vertX = (petCenterX - w / 2f).coerceIn(left, maxX)
        val vertTail = petCenterX - vertX
        val vertFits = w <= right - left && vertTail >= tailMin && vertTail <= w - tailMin

        val aboveY = petTop - gapPx - h + insetPx
        val belowY = petBottom + gapPx - insetPx
        val fitsAbove = aboveY >= top
        val fitsBelow = belowY + h <= bottom
        val preferAbove = petTop - top >= bottom - petBottom

        if (vertFits && (fitsAbove || fitsBelow)) {
            val useAbove = if (fitsAbove && fitsBelow) preferAbove else fitsAbove
            return if (useAbove) placement(vertX, aboveY, BubblePointerSide.BOTTOM, vertTail)
            else placement(vertX, belowY, BubblePointerSide.TOP, vertTail)
        }

        val fallbackTail = clampTail(vertTail, w, tailMin)
        return if (preferAbove) {
            placement(vertX, aboveY.coerceIn(top, maxY), BubblePointerSide.BOTTOM, fallbackTail)
        } else {
            placement(vertX, belowY.coerceIn(top, maxY), BubblePointerSide.TOP, fallbackTail)
        }
    }

    private fun clampTail(value: Float, length: Float, min: Float): Float =
        if (length >= 2f * min) value.coerceIn(min, length - min) else length / 2f

    private fun placement(x: Float, y: Float, side: BubblePointerSide, tail: Float) =
        AnchorPlacement(x.roundToInt(), y.roundToInt(), side, tail)
}