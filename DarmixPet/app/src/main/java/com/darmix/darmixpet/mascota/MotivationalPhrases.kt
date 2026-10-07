package com.darmix.darmixpet.mascota

object MotivationalPhrases {

    private val phrases = listOf(
        "¡Tú puedes con todo hoy! 💪",
        "Recuerda tomar agua 💧",
        "Un descanso también es productivo 🌿",
        "¡Sigue así, vas muy bien! ✨",
        "No olvides estirarte un poco 🧘",
        "Hoy es un buen día para lograr algo nuevo",
        "Respira profundo, todo va a estar bien",
        "Tu tiempo es valioso, úsalo con calma",
        "¡Una sonrisa no cuesta nada! 😊",
        "Pequeños pasos también cuentan"
    )

    fun random(): String = phrases.random()
}