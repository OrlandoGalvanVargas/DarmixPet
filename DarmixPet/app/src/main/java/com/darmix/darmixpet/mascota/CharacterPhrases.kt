package com.darmix.darmixpet.mascota


object CharacterPhrases {

    private val archi = listOf(
        "Respira. Yo me encargo del ruido.",
        "Un solo enfoque a la vez.",
        "El cosmos también espera su turno.",
        "Tu mente brilla cuando la dejas respirar.",
        "Pequeños pasos, grandes galaxias.",
        "Silencio en las notificaciones, paz en la mente.",
        "Cada minuto bien usado es una estrella.",
        "Hoy tú decides dónde va tu atención.",
        "Concentrarse es una forma de magia.",
        "Estás más cerca de lo que crees."
    )

    private val sylva = listOf(
        "Todo lo bueno crece con calma.",
        "Un descansito hace florecer las ideas.",
        "Respira hondo, como el bosque.",
        "Hasta los árboles se toman su tiempo.",
        "Estira las piernas, ¡las hojas también bailan!",
        "Tomar agua también es cuidarte.",
        "Tu calma es tu mejor raíz.",
        "Paso a paso, hoja a hoja.",
        "Mira algo verde un ratito.",
        "Descansar también es avanzar."
    )

    private val galahad = listOf(
        "¡Firme, valiente! Tú puedes.",
        "Tu tiempo es tu tesoro. Yo lo defiendo.",
        "Un guerrero sabe cuándo parar.",
        "¡Mantén la guardia en alto!",
        "La disciplina es tu mejor armadura.",
        "Hoy ganas tú, no la pantalla.",
        "El límite que pones es tu escudo.",
        "¡Adelante, con honor!",
        "Un paso firme vale más que mil distracciones.",
        "Yo cuido tu espalda."
    )

    fun torchOn(skinId: String): String = when (skinId) {
        "archimago" -> "¡Hágase la luz!"
        "skin2" -> "¡Un rayito de sol para ti!"
        "skin3" -> "¡Antorcha encendida!"
        else -> "¡Linterna encendida!"
    }

    fun torchOff(skinId: String): String = when (skinId) {
        "archimago" -> "Luz apagada. De vuelta a la calma."
        "skin2" -> "La luz descansa un ratito."
        "skin3" -> "Antorcha apagada."
        else -> "Linterna apagada."
    }

    fun random(skinId: String): String = when (skinId) {
        "archimago" -> archi.random()
        "skin2" -> sylva.random()
        "skin3" -> galahad.random()
        else -> MotivationalPhrases.random()
    }
}
