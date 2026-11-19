package com.ferrovelho.game

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

// =====================================================================================
//  CASA DO PERSONAGEM
//  À noite (a partir das 19h) o personagem pode chamar o táxi pra casa. O táxi leva,
//  ele dorme, o dia passa e ele acorda na manhã seguinte (8h), na porta de casa.
// =====================================================================================

/** A partir desse horário (19h) dá pra dormir. */
const val SLEEP_FROM = 19 * 60

@Composable
fun HomeDialog(g: GameState, onSleep: () -> Unit, onClose: () -> Unit) {
    val night = g.minutes >= SLEEP_FROM
    Dialog(onDismissRequest = onClose) {
        Card {
            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("🏠 Sua casa", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Bedroom(night)
                Text(
                    if (night) "Já é noite. Sua cama tá te chamando..."
                    else "Ainda é dia. Dá pra descansar até anoitecer.",
                    fontSize = 13.sp, textAlign = TextAlign.Center,
                )
                if (night) {
                    Button(onClick = onSleep, Modifier.fillMaxWidth()) { Text("😴 Dormir e acordar amanhã") }
                } else {
                    OutlinedButton(
                        onClick = { g.spendTime(SLEEP_FROM - g.minutes) },
                        Modifier.fillMaxWidth(),
                    ) { Text("⏩ Esperar anoitecer (19h)") }
                }
                TextButton(onClick = onClose) { Text("Sair") }
            }
        }
    }
}

/** Quartinho em pixels: cama, janela (com lua à noite), tapete e abajur. */
@Composable
private fun Bedroom(night: Boolean) {
    Canvas(Modifier.fillMaxWidth().height(120.dp)) {
        val u = size.width / 160f
        fun r(c: Color, x: Float, y: Float, w: Float, h: Float) = drawRect(c, Offset(x * u, y * u), Size(w * u, h * u))
        val hh = size.height / u
        r(if (night) Color(0xFF3B3354) else Color(0xFFF1D9B5), 0f, 0f, 160f, hh * 0.72f)      // parede
        r(Color(0xFF6D4C41), 0f, hh * 0.72f, 160f, hh * 0.28f)                                  // piso
        r(Color(0xFF4E342E), 0f, hh * 0.72f, 160f, 2f)                                           // rodapé
        // janela
        r(Color(0xFF5D4037), 100f, 12f, 40f, 34f)
        r(if (night) Color(0xFF0B1030) else Color(0xFF90CAF9), 102f, 14f, 36f, 30f)
        if (night) {
            drawCircle(Color(0xFFF5F5DC), 5f * u, Offset(126f * u, 24f * u))
            r(Color.White, 108f, 20f, 1.5f, 1.5f); r(Color.White, 114f, 34f, 1.5f, 1.5f)
        }
        r(Color(0xFF5D4037), 119f, 14f, 2f, 30f)
        // cama
        r(Color(0xFF5D4037), 10f, 56f, 70f, 8f)
        r(Color(0xFF5D4037), 10f, 36f, 5f, 28f)
        r(Color(0xFFECEFF1), 16f, 44f, 14f, 8f)                                                   // travesseiro
        r(Color(0xFF1E88E5), 16f, 50f, 62f, 9f)                                                   // coberta
        r(Color(0xFF1565C0), 16f, 50f, 62f, 2f)
        // tapete e abajur
        drawOval(Color(0xFFC62828), Offset(90f * u, (hh * 0.8f) * u), Size(50f * u, 12f * u))
        r(Color(0xFF8D6E63), 84f, 50f, 8f, 14f)
        r(Color(0xFFFFD54F), 83f, 44f, 10f, 7f)
    }
}

/**
 * Cena de ir pra casa de táxi, dormir e acordar. Cobre a tela inteira.
 * Quando termina, o dia seguinte já começou (g.nextDay) e chama onDone.
 */
@Composable
fun SleepScene(g: GameState, pname: String, female: Boolean, fromTaxi: Boolean, onDone: () -> Unit) {
    var step by remember { mutableIntStateOf(if (fromTaxi) 0 else 1) }
    val done by rememberUpdatedState(onDone)

    LaunchedEffect(Unit) {
        if (fromTaxi) {
            Voice.speak("Moço, me leva pra casa, por favor.", if (female) 1.3f else 0.95f)
            delay(2200)
        }
        step = 1
        delay(1600)
        step = 2
        delay(3600)
        g.nextDay()
        step = 3
        delay(2800)
        done()
    }

    val light by animateFloatAsState(if (step == 3) 1f else 0f, tween(1400), label = "light")
    val bg = lerp(Color(0xFF05071A), Color(0xFFFFE0B2), light)
    val fg = lerp(Color.White, Color(0xFF3E2723), light)

    val inf = rememberInfiniteTransition(label = "sleep")
    val pulse by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse), label = "pulse")

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize().background(bg), Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (step) {
                    0 -> {
                        Text("🚕", fontSize = 64.sp, modifier = Modifier.alpha(0.6f + 0.4f * pulse))
                        Text("Chamando o táxi...", fontSize = 20.sp, color = fg, fontWeight = FontWeight.Bold)
                        Text("Indo pra casa", fontSize = 14.sp, color = fg)
                    }
                    1 -> {
                        Text("🏠", fontSize = 64.sp)
                        Text("Chegando em casa...", fontSize = 20.sp, color = fg, fontWeight = FontWeight.Bold)
                    }
                    2 -> {
                        Text("😴", fontSize = 64.sp)
                        Text("💤  Zzz...", fontSize = 28.sp, color = fg, modifier = Modifier.alpha(0.35f + 0.65f * pulse))
                        Text("Dormindo...", fontSize = 14.sp, color = fg)
                    }
                    else -> {
                        Text("☀️", fontSize = 64.sp)
                        Text("Bom dia, $pname!", fontSize = 24.sp, color = fg, fontWeight = FontWeight.Bold)
                        Text("Dia ${g.day} • 08:00", fontSize = 14.sp, color = fg)
                        Text("Você dormiu bem. Os ferros-velhos têm carros novos!", fontSize = 13.sp, color = fg, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

/** Tarifa do táxi (mesma conta dos outros destinos). */
fun taxiFare(dist: Float): Int = (dist / 25f).toInt().coerceIn(10, 120)

/** Minutos de viagem. */
fun taxiMinutes(dist: Float): Int = (dist / 12f).toInt().coerceIn(5, 90)
