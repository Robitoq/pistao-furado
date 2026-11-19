package com.ferrovelho.game

import android.content.Context
import android.media.MediaPlayer

/** Música de fundo em loop: "chill" no jogo todo e "club" dentro do baile. Nunca derruba o app. */
object Music {
    private var player: MediaPlayer? = null
    private var track = "chill.mp3"
    var enabled = true
        private set

    fun init(ctx: Context) {
        enabled = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE).getBoolean("music", true)
    }

    private fun load(ctx: Context) {
        try { player?.release() } catch (_: Throwable) {}
        player = null
        val names = listOf(track, "lofi.mp3")
        for (n in names) {
            try {
                val fd = ctx.assets.openFd("music/$n")
                player = MediaPlayer().apply {
                    setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                    isLooping = true
                    setVolume(if (n == "club.mp3") 0.6f else 0.42f, if (n == "club.mp3") 0.6f else 0.42f)
                    prepare()
                }
                fd.close()
                return
            } catch (e: Throwable) { player = null }
        }
    }

    fun start(ctx: Context) {
        if (!enabled) return
        try {
            if (player == null) load(ctx)
            player?.start()
        } catch (e: Throwable) { player = null }
    }

    /** Troca a faixa ("club.mp3" no baile, "chill.mp3" no resto). */
    fun setTrack(ctx: Context, name: String) {
        if (name == track) return
        track = name
        if (enabled) { load(ctx); try { player?.start() } catch (_: Throwable) {} } else player = null
    }

    fun pause() { try { player?.pause() } catch (_: Throwable) {} }

    fun release() {
        try { player?.release() } catch (_: Throwable) {}
        player = null
    }

    fun toggle(ctx: Context): Boolean {
        enabled = !enabled
        ctx.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putBoolean("music", enabled).apply()
        if (enabled) start(ctx) else pause()
        return enabled
    }
}
