package com.darmix.darmixpet.mascota

import android.content.Context

class ConfigurableSkin(private val info: SkinInfo) : MascotaSkin {
    override val id = info.id
    override val displayName = info.displayName

    private val cache = mutableMapOf<MascotaAnimState, SpriteSheet>()

    override fun getAnimation(context: Context, state: MascotaAnimState): SpriteSheet {
        return cache.getOrPut(state) {
            val resId = when (state) {
                MascotaAnimState.IDLE -> info.idleRes
                MascotaAnimState.TAP_SIMPLE -> info.tapSimpleRes
                MascotaAnimState.TAP_DOBLE -> info.tapDobleRes
                MascotaAnimState.TRIPLE_TAP -> info.tripleTapRes
                MascotaAnimState.NIGHT_WARNING -> info.nightRes
                MascotaAnimState.LOW_BATTERY -> info.lowBatteryRes
                MascotaAnimState.LISTENING -> info.listeningRes
                MascotaAnimState.THINKING -> info.thinkingRes
            }
            SpriteSheet.fromDrawable(context, resId, frameCount = 6)
        }
    }
}