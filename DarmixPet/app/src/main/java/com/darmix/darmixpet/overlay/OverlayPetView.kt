package com.darmix.darmixpet.overlay

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import com.darmix.darmixpet.mascota.SpriteSheet

class OverlayPetView(context: Context) : ImageView(context) {

    private val handler = Handler(Looper.getMainLooper())
    private var currentFrame = 0
    private var spriteSheet: SpriteSheet? = null
    private var loop = true
    private var onAnimationEnd: (() -> Unit)? = null

    var onFrameChanged: ((Bitmap) -> Unit)? = null

    private val frameRunnable = object : Runnable {
        override fun run() {
            val sheet = spriteSheet ?: return
            currentFrame++
            if (currentFrame >= sheet.frames.size) {
                if (loop) {
                    currentFrame = 0
                } else {
                    onAnimationEnd?.invoke()
                    return
                }
            }
            showFrame(sheet.frames[currentFrame])
            handler.postDelayed(this, sheet.frameDurationMs)
        }
    }

    fun playSpriteSheet(sheet: SpriteSheet, loop: Boolean = true, onEnd: (() -> Unit)? = null) {
        stopAnimation()
        spriteSheet = sheet
        this.loop = loop
        this.onAnimationEnd = onEnd
        currentFrame = 0
        showFrame(sheet.frames[0])
        handler.postDelayed(frameRunnable, sheet.frameDurationMs)
    }

    fun stopAnimation() {
        handler.removeCallbacks(frameRunnable)
    }

    private fun showFrame(bitmap: Bitmap) {
        setImageBitmap(bitmap)
        onFrameChanged?.invoke(bitmap)
    }
}