package com.darmix.darmixpet.mascota

object CharacterPhrases {

    private val archi = listOf(
        "Tú enfócate, yo vigilo.",
        "Una cosa a la vez. Así sale mejor.",
        "¿Otra vez revisando el celular?",
        "Termina eso y luego me cuentas.",
        "Las notificaciones pueden esperar.",
        "Shh… estoy cuidando tu concentración.",
        "Allá en el cielo no hay notificaciones.",
        "Vas bien. No te distraigas ahora.",
        "Cinco minutitos más de enfoque y listo.",
        "Me gusta verte concentrado."
    )

    private val sylva = listOf(
        "Despacio también se llega.",
        "¿Y si te tomas un vasito de agua?",
        "Estírate tantito, te va a caer bien.",
        "Tus ojos también necesitan descanso.",
        "Mira por la ventana un ratito.",
        "Respira hondo. Así, muy bien.",
        "Descansar también es avanzar.",
        "Hasta las plantas se toman su tiempo.",
        "¡Ay, me hiciste cosquillas!",
        "Un respiro y seguimos, ¿va?"
    )

    private val galahad = listOf(
        "Tu tiempo no se negocia.",
        "Aquí sigo, cuidando tu tiempo.",
        "Tú pon el límite, yo lo hago cumplir.",
        "¿Seguro que necesitas abrir esa app?",
        "Hoy ganas tú, no la pantalla.",
        "Palabra de caballero: tú puedes.",
        "Un buen guerrero sabe cuándo parar.",
        "¡Firme! Ya casi lo logras.",
        "Yo te cubro la espalda.",
        "Sin excusas: tú puedes con esto."
    )

    fun torchOn(skinId: String): String = when (skinId) {
        "archimago" -> "Que se haga la luz."
        "skin2" -> "¡Un rayito de sol para ti!"
        "skin3" -> "¡Antorcha encendida!"
        else -> "¡Linterna encendida!"
    }

    fun torchOff(skinId: String): String = when (skinId) {
        "archimago" -> "Listo, volvemos a la calma."
        "skin2" -> "La luz se va a descansar."
        "skin3" -> "Antorcha apagada. Misión cumplida."
        else -> "Linterna apagada."
    }

    fun random(skinId: String): String = when (skinId) {
        "archimago" -> archi.random()
        "skin2" -> sylva.random()
        "skin3" -> galahad.random()
        else -> MotivationalPhrases.random()
    }
}