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
        enabled = false   // voz removida do jogo
    }

    private fun clean(s: String) = s.filter { it.isLetterOrDigit() || it in " .,!?'-$:%" }.replace("R$", "reais ")

    @Suppress("UNUSED_PARAMETER")
    fun speak(text: String, pitch: Float = 1f, rate: Float = 1f, add: Boolean = false) {
        // sem fala: a voz foi removida
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
