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
        val crashPrefs = getSharedPreferences("crash", Context.MODE_PRIVATE)
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try { crashPrefs.edit().putString("log", Log.getStackTraceString(e).take(4000)).commit() } catch (_: Throwable) {}
            prev?.uncaughtException(t, e)
        }
        val lastCrash = crashPrefs.getString("log", null)
        if (lastCrash != null) {
            // mostra o erro da última vez que o app fechou, em vez de abrir o jogo
            setContent {
                GameTheme {
                    Surface(Modifier.fillMaxSize()) {
                        val clip = LocalClipboardManager.current
                        var show by remember { mutableStateOf(true) }
                        if (show) {
                            Column(Modifier.fillMaxSize().statusBarsPadding().padding(12.dp).verticalScroll(rememberScrollState())) {
                                Text("O jogo fechou por um erro. Tire print desta tela e mande pro suporte:", fontSize = 14.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(lastCrash, fontSize = 10.sp)
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = { clip.setText(AnnotatedString(lastCrash)) }) { Text("Copiar erro") }
                                Button(onClick = {
                                    crashPrefs.edit().remove("log").commit()
                                    show = false
                                    recreate()
                                }) { Text("Apagar erro e tentar abrir") }
                            }
                        }
                    }
                }
            }
            return
        }
        val g = GameState(applicationContext)
        game = g
        g.load()
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
