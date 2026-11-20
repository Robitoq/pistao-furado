package com.ferrovelho.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    private lateinit var game: GameState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        game = GameState(applicationContext)
        game.load()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize()) {
                    FerroVelhoApp(game)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        game.save()
    }
}
