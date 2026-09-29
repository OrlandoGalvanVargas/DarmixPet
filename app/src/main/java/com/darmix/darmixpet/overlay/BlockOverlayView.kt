package com.darmix.darmixpet.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView

/**
 * Overlay de pantalla completa que se muestra brevemente al bloquear una app:
 * cubre toda la pantalla e intercepta los toques, evitando que el usuario
 * interactúe con la app bloqueada mientras se completa la redirección a Home.
 */
class BlockOverlayView(context: Context) : FrameLayout(context) {

    private val label: TextView

    init {
        setBackgroundColor(Color.parseColor("#DD000000"))

        label = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            val paddingPx = (32 * resources.displayMetrics.density).toInt()
            setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
        }

        addView(
            label,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        )
    }

    fun setMessage(text: String) {
        label.text = text
    }
}