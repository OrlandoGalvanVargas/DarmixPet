package com.darmix.darmixpet.mascota

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import android.content.Context

/**
 * Representa una animación como un sprite sheet horizontal:
 * todos los frames en una sola fila, mismo ancho y alto cada uno.
 */
data class SpriteSheet(
    val frames: List<Bitmap>,
    val frameDurationMs: Long = 120L // ~8 fps por defecto, ajustable por animación
) {
    companion object {
        /**
         * Carga un drawable como sprite sheet y lo recorta en [frameCount] frames
         * iguales, de izquierda a derecha. Libera el bitmap original tras recortar,
         * ya que Bitmap.createBitmap genera copias independientes de los píxeles.
         */
        fun fromDrawable(
            context: Context,
            @DrawableRes resId: Int,
            frameCount: Int,
            frameDurationMs: Long = 120L
        ): SpriteSheet {
            val original = BitmapFactory.decodeResource(context.resources, resId)
            val frameWidth = original.width / frameCount
            val frameHeight = original.height

            val frames = (0 until frameCount).map { index ->
                Bitmap.createBitmap(
                    original,
                    index * frameWidth,
                    0,
                    frameWidth,
                    frameHeight
                )
            }

            original.recycle()

            return SpriteSheet(frames = frames, frameDurationMs = frameDurationMs)
        }
    }
}