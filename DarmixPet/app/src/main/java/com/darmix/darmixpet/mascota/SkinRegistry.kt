package com.darmix.darmixpet.mascota

import com.darmix.darmixpet.R

object SkinRegistry {

    val availableSkins: List<SkinInfo> = listOf(
        SkinInfo(
            id = "archimago",
            displayName = "Archi el Archimago",
            subtitle = "Éter · Magia Arcana",
            description = "Analítico, sereno y enfocado. Canaliza un escudo para aislarte del ruido.",
            accentColor = 0xFF22D3EE,
            theme = MascotThemeSpec(
                primaryColor = 0xFF22D3EE,
                secondaryColor = 0xFF1E3A8A,
                accentColor = 0xFFEAB308,
                shapeStyle = BubbleShapeStyle.ARCANE_GLOW
            ),
            idleRes = R.drawable.archimago_idle,
            tapSimpleRes = R.drawable.archimago_tap_simple,
            tapDobleRes = R.drawable.archimago_tap_doble,
            tripleTapRes = R.drawable.archimago_triple_tap,
            nightRes = R.drawable.archimago_night,
            lowBatteryRes = R.drawable.archimago_low_battery,
            listeningRes = R.drawable.archimago_listening,
            thinkingRes = R.drawable.archimago_thinking
        ),
        SkinInfo(
            id = "skin2",
            displayName = "Sylva el Sabio",
            subtitle = "Naturaleza · Vitalidad",
            description = "Paciente, alegre y reconfortante. Cuida tus pausas y tu descanso.",
            accentColor = 0xFF4ADE80,
            theme = MascotThemeSpec(
                primaryColor = 0xFF4ADE80,
                secondaryColor = 0xFF14532D,
                accentColor = 0xFFCA8A04,
                shapeStyle = BubbleShapeStyle.ORGANIC_LEAF
            ),
            idleRes = R.drawable.skin2_idle,
            tapSimpleRes = R.drawable.skin2_tap_simple,
            tapDobleRes = R.drawable.skin2_tap_doble,
            tripleTapRes = R.drawable.skin2_triple_tap,
            nightRes = R.drawable.skin2_night,
            lowBatteryRes = R.drawable.skin2_low_battery,
            listeningRes = R.drawable.skin2_listening,
            thinkingRes = R.drawable.skin2_thinking
        ),
        SkinInfo(
            id = "skin3",
            displayName = "Sir Galahad el Guardián",
            subtitle = "Tierra · Disciplina",
            description = "Valiente, firme y leal. Defiende tus límites cuando se agota tu tiempo.",
            accentColor = 0xFF0D9488,
            theme = MascotThemeSpec(
                primaryColor = 0xFF9A3412,
                secondaryColor = 0xFF334155,
                accentColor = 0xFF9F1239,
                shapeStyle = BubbleShapeStyle.ANGULAR_SHIELD
            ),
            idleRes = R.drawable.skin3_idle,
            tapSimpleRes = R.drawable.skin3_tap_simple,
            tapDobleRes = R.drawable.skin3_tap_doble,
            tripleTapRes = R.drawable.skin3_triple_tap,
            nightRes = R.drawable.skin3_night,
            lowBatteryRes = R.drawable.skin3_low_battery,
            listeningRes = R.drawable.skin3_listening,
            thinkingRes = R.drawable.skin3_thinking
        )
    )

    fun byId(id: String): SkinInfo {
        return availableSkins.find { it.id == id } ?: availableSkins.first()
    }
}