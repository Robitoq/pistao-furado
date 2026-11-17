package com.ferrovelho.game

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Voz dos personagens (síntese de fala do Android, pt-BR). Nunca derruba o app. */
object Voice {
    private var tts: TextToSpeech? = null
    private var ready = false
    var enabled = true
        private set

    fun init(ctx: Context) {
        enabled = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE).getBoolean("voice", true)
        if (tts != null) return
        try {
            tts = TextToSpeech(ctx.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val r = tts?.setLanguage(Locale("pt", "BR"))
                    ready = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
                }
            }
        } catch (e: Throwable) {
            tts = null
        }
    }

    private fun clean(s: String) = s.filter { it.isLetterOrDigit() || it in " .,!?'-$:%" }.replace("R$", "reais ")

    fun speak(text: String, pitch: Float = 1f, rate: Float = 1f, add: Boolean = false) {
        if (!enabled || !ready) return
        try {
            tts?.setPitch(pitch)
            tts?.setSpeechRate(rate)
            tts?.speak(clean(text), if (add) TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH, null, "v" + System.nanoTime())
        } catch (_: Throwable) {}
    }

    fun stop() {
        try { tts?.stop() } catch (_: Throwable) {}
    }

    fun toggle(ctx: Context): Boolean {
        enabled = !enabled
        ctx.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putBoolean("voice", enabled).apply()
        if (!enabled) stop()
        return enabled
    }

    fun shutdown() {
        try { tts?.shutdown() } catch (_: Throwable) {}
        tts = null
        ready = false
    }
}
