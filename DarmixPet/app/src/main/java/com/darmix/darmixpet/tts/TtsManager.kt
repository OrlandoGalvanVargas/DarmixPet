package com.darmix.darmixpet.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TtsManager(context: Context) {

    private var isReady = false

    private val tts: TextToSpeech = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            onEngineReady()
        } else {
            Log.w("TtsManager", "Falló la inicialización del motor TTS (status=$status)")
        }
    }

    private fun onEngineReady() {
        val result = tts.setLanguage(Locale("es", "MX"))
        isReady = result != TextToSpeech.LANG_MISSING_DATA &&
                result != TextToSpeech.LANG_NOT_SUPPORTED
        if (!isReady) {
            Log.w("TtsManager", "Idioma es-MX no disponible en este dispositivo (result=$result)")
        }
    }

    fun speak(text: String) {
        if (!isReady) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}