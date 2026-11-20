package com.ferrovelho.game

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private var game: GameState? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Music.init(applicationContext)
        Voice.init(applicationContext)
        // se o app fechar por erro, limpa qualquer log antigo e abre o jogo normalmente (sem tela de erro)
        try { getSharedPreferences("crash", Context.MODE_PRIVATE).edit().clear().apply() } catch (_: Throwable) {}
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try { Log.e("PistaoFurado", "Erro fatal", e) } catch (_: Throwable) {}
            try { game?.save() } catch (_: Throwable) {}
            prev?.uncaughtException(t, e)
        }
        val g = GameState(applicationContext)
        game = g
        try { g.load() } catch (e: Throwable) { Log.e("PistaoFurado", "Falha ao carregar save", e) }
        setContent {
            GameTheme {
                Surface(Modifier.fillMaxSize()) {
                    FerroVelhoApp(g)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Music.start(applicationContext)
    }

    override fun onPause() {
        super.onPause()
        Music.pause()
        Voice.stop()
        game?.save()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) { Music.release(); Voice.shutdown() }
    }
}
