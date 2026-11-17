package com.ferrovelho.game

import android.content.Context
import android.media.MediaPlayer

/** Música de fundo em loop (assets/music/lofi.mp3). Nunca derruba o app se algo falhar. */
object Music {
    private var player: MediaPlayer? = null
    var enabled = true
        private set

    fun init(ctx: Context) {
        enabled = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE).getBoolean("music", true)
    }

    fun start(ctx: Context) {
        if (!enabled) return
        try {
            if (player == null) {
                val fd = ctx.assets.openFd("music/lofi.mp3")
                player = MediaPlayer().apply {
                    setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                    isLooping = true
                    setVolume(0.45f, 0.45f)
                    prepare()
                }
                fd.close()
            }
            player?.start()
        } catch (e: Throwable) {
            player = null
        }
    }

    fun pause() {
        try { player?.pause() } catch (_: Throwable) {}
    }

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
