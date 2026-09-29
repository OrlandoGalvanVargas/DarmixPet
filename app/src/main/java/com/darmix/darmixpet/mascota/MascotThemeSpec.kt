package com.darmix.darmixpet.mascota

/** Estilo visual de los elementos flotantes (bocadillo, alerta, Quick Menu). */
enum class BubbleShapeStyle {
    ARCANE_GLOW,     // Archi: muy redondeado, resplandor suave
    ORGANIC_LEAF,    // Sylva: esquinas asimétricas orgánicas
    ANGULAR_SHIELD   // Sir Galahad: esquinas rectas, borde grueso
}

data class MascotThemeSpec(
    val primaryColor: Long,   // 0xFFRRGGBB — energía/cuerpo
    val secondaryColor: Long, // tono profundo (túnica/armadura)
    val accentColor: Long,    // dorado/gema/detalle
    val shapeStyle: BubbleShapeStyle
)