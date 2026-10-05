package com.darmix.darmixpet.ui


data class SkinLore(
    val shortName: String,
    val quote: String,
    val element: String,
    val personality: String,
    val story: String,
    val mission: String,
    val palette: List<Long>
) {
    companion object {
        fun forId(id: String): SkinLore = when (id) {
            "archimago" -> SkinLore(
                shortName = "Archi",
                quote = "Respira. Yo me encargo del ruido.",
                element = "Éter · Magia Arcana",
                personality = "Analítico, sereno y metódico",
                story = "Un espíritu etéreo nacido del cosmos, envuelto en una capa azul " +
                        "medianoche. Su cuerpo de energía neón flota sin prisa y sus ojos " +
                        "amarillos brillan de curiosidad. Un broche dorado con una gema " +
                        "azul cierra su túnica.",
                mission = "Cuando activas un temporizador, canaliza un escudo azul que " +
                        "aísla tu mente del ruido y de las notificaciones.",
                palette = listOf(
                    0xFF00F0FF, 0xFF00A3E0, 0xFFFFD500, 0xFF0A1172,
                    0xFF1F3A93, 0xFFFFC82D, 0xFF6AE5F9
                )
            )

            "skin2" -> SkinLore(
                shortName = "Sylva",
                quote = "Todo lo bueno crece con calma.",
                element = "Naturaleza · Vitalidad",
                personality = "Paciente, alegre y equilibrado",
                story = "Un espíritu místico de los bosques, de cuerpo esmeralda y ojos " +
                        "brillantes. Lleva sombrero de ala ancha y una capa verde bosque " +
                        "adornada con hojas frescas; a su alrededor flotan pequeñas hojas " +
                        "que llenan el aire de frescura.",
                mission = "Acompaña tus enfriamientos y pausas para evitar la fatiga " +
                        "digital y recordarte que todo progreso necesita calma.",
                palette = listOf(
                    0xFF80FF85, 0xFFA3FF47, 0xFFA0E000, 0xFF0F3822,
                    0xFF1D5C3A, 0xFF5BD638, 0xFFD4A237, 0xFF32C768
                )
            )

            "skin3" -> SkinLore(
                shortName = "Galahad",
                quote = "Tu tiempo es tu tesoro. Yo lo defiendo.",
                element = "Tierra · Acero",
                personality = "Valiente, firme y leal",
                story = "Un espíritu guerrero de tono terracota con armadura completa: " +
                        "yelmo oscuro con visera, penacho rojo vino, capa color arena, " +
                        "escudo y espada al cinto. Sus ojos dorados asoman decididos " +
                        "tras la visera.",
                mission = "Entra en acción cuando alcanzas la cuota diaria de una app: " +
                        "despliega su escudo y bloquea el acceso para mantener tu " +
                        "autocontrol intacto.",
                palette = listOf(
                    0xFF2F3337, 0xFF8B1E3F, 0xFFA88258, 0xFFE2A021,
                    0xFFCFA76E, 0xFF337375, 0xFF8B5738
                )
            )

            else -> SkinLore(
                shortName = "Tu compañero",
                quote = "¡Vamos a cuidar tu tiempo!",
                element = "Misterio",
                personality = "Leal y curioso",
                story = "Un compañero que todavía guarda muchos secretos.",
                mission = "Cuidar tu tiempo de pantalla.",
                palette = emptyList()
            )
        }
    }
}
