package com.darmix.darmixpet.mascota

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import android.content.Context


data class SpriteSheet(
    val frames: List<Bitmap>,
    val frameDurationMs: Long = 120L
) {
    companion object {

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
