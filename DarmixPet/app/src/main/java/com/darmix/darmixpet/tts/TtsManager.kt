package com.darmix.darmixpet.tts

import android.content.Context
import android.content.SharedPreferences
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TtsManager(context: Context) {

    private val appContext = context.applicationContext
    private var isReady = false

    private val tts: TextToSpeech = TextToSpeech(appContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            onEngineReady()
        } else {
            Log.w("TtsManager", "Falló la inicialización del motor TTS (status=$status)")
        }
    }

    private val voiceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == VoicePreferences.KEY_VOICE_ENABLED && !VoicePreferences.isEnabled(appContext)) {
            tts.stop()
        }
    }

    init {
        VoicePreferences.registerListener(appContext, voiceListener)
    }

    private fun onEngineReady() {
        val result = tts.setLanguage(Locale("es", "MX"))
        isReady = result != TextToSpeech.LANG_MISSING_DATA &&
                result != TextToSpeech.LANG_NOT_SUPPORTED
        if (!isReady) {
            Log.w("TtsManager", "Idioma es-MX no disponible en este dispositivo (result=$result)")
        }
    }

    fun speak(text: String, interrupt: Boolean = true) {
        if (!isReady || !VoicePreferences.isEnabled(appContext)) return
        val mode = if (interrupt) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts.speak(text, mode, null, null)
    }

    fun shutdown() {
        VoicePreferences.unregisterListener(appContext, voiceListener)
        tts.stop()
        tts.shutdown()
    }
}