package com.darmix.darmixpet.mascota


enum class BubbleShapeStyle {
    ARCANE_GLOW,
    ORGANIC_LEAF,
    ANGULAR_SHIELD
}

data class MascotThemeSpec(
    val primaryColor: Long,
    val secondaryColor: Long,
    val accentColor: Long,
    val shapeStyle: BubbleShapeStyle
)
