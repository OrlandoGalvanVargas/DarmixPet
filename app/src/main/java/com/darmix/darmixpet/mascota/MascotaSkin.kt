package com.darmix.darmixpet.mascota

import android.content.Context

enum class MascotaAnimState {
    IDLE,
    TAP_SIMPLE,
    TAP_DOBLE,
    TRIPLE_TAP,
    NIGHT_WARNING,
    LOW_BATTERY,
    LISTENING,
    THINKING
}

interface MascotaSkin {
    val id: String
    val displayName: String
    fun getAnimation(context: Context, state: MascotaAnimState): SpriteSheet
}