package com.example.integrations.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TtsHelper(context: Context) {
    private var tts: TextToSpeech? = null
    private var initialized = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            initialized = status == TextToSpeech.SUCCESS
        }
    }

    fun speak(text: String, isHindi: Boolean) {
        val engine = tts ?: return
        if (!initialized) return
        val locale = if (isHindi) Locale.forLanguageTag("hi-IN") else Locale.forLanguageTag("en-IN")
        val langResult = engine.setLanguage(locale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.setLanguage(if (isHindi) Locale.forLanguageTag("hi") else Locale.US)
        }
        engine.setSpeechRate(if (isHindi) 0.90f else 0.92f)
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "rakshakai_tts_${System.currentTimeMillis()}")
    }

    fun stop() {
        runCatching { tts?.stop() }
    }

    fun shutdown() {
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
        initialized = false
    }
}
