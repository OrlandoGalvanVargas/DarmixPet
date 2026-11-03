package com.darmix.darmixpet.ui

data class SkinLore(
    val shortName: String,
    val signature: String,
    val greeting: String,
    val letter: String,
    val quote: String,
    val origin: String,
    val stampElement: String,
    val quirks: List<String>
) {
    companion object {
        fun forId(id: String): SkinLore = when (id) {
            "archimago" -> SkinLore(
                shortName = "Archi",
                signature = "Archi",
                greeting = "Para ti, que te distraes fácil:",
                letter = "Vengo de un rincón del cielo donde no existe el ruido. Allá aprendí " +
                        "que la mente rinde más haciendo una sola cosa. Déjame cuidar eso por ti.",
                quote = "Tú enfócate, yo vigilo.",
                origin = "CIELO SIN RUIDO",
                stampElement = "ÉTER",
                quirks = listOf("Detesta las notificaciones", "Habla poco, observa mucho", "Termina lo que empieza")
            )

            "skin2" -> SkinLore(
                shortName = "Sylva",
                signature = "Sylva",
                greeting = "Para ti, que siempre dices \"cinco minutitos más\":",
                letter = "Crecí en un bosque donde nadie corre. Ahí entendí que hasta el árbol " +
                        "más grande necesita descansar. Soltar el celular un rato también es avanzar.",
                quote = "Despacio también se llega.",
                origin = "BOSQUE ANTIGUO",
                stampElement = "NATURA",
                quirks = listOf("Siempre trae hojas en el sombrero", "Pide pausas sin culpa", "Se ríe de todo")
            )

            "skin3" -> SkinLore(
                shortName = "Galahad",
                signature = "Sir Galahad",
                greeting = "Para ti, que te pones límites y siempre los rompes:",
                letter = "Hice un juramento: proteger a quien me eligiera. Mi armadura aguantó " +
                        "mil batallas y ahora la uso contra las horas que se van en la pantalla.",
                quote = "Tu tiempo no se negocia.",
                origin = "FORTALEZA DE PIEDRA",
                stampElement = "TIERRA",
                quirks = listOf("Nunca rompe una promesa", "No acepta excusas", "Saluda con el escudo")
            )

            else -> SkinLore(
                shortName = "Tu compañero",
                signature = "Tu compañero",
                greeting = "Para ti:",
                letter = "Todavía guardo muchos secretos, pero ya estoy listo para cuidar tu tiempo.",
                quote = "Vamos juntos.",
                origin = "LUGAR MISTERIOSO",
                stampElement = "???",
                quirks = emptyList()
            )
        }
    }
}