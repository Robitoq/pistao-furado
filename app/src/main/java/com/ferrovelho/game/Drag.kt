package com.ferrovelho.game

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

// =====================================================================================
//  DIRIGIR NA RUA + ARRANCADA (corrida de 400 m) + SOM DE MOTOR
// =====================================================================================

/** Velocidade do carro andando pela cidade (unidades do mapa por segundo). */
fun carStreetSpeed(c: OwnedCar): Float = 150f + c.cond[0] * 0.9f + c.tuning.size * 15f

/** Potência "em cv": depende do modelo, do motor (condição) e do tuning. */
fun carPower(c: OwnedCar): Double {
    val base = CATALOG[c.model].base
    val hp = 60.0 + (base / 1000.0).pow(0.55) * 14.0
    return hp * (0.6 + 0.4 * c.cond[0] / 100.0) * (1.0 + 0.04 * c.tuning.size)
}

/** Motor sintetizado: frequência acompanha o giro, volume acompanha o acelerador. */
class EngineSound {
    @Volatile var freq = 40f
    @Volatile var vol = 0f
    @Volatile private var running = false
    private var track: AudioTrack? = null
    private var thread: Thread? = null

    fun set(f: Float, v: Float) { freq = f; vol = v }

    fun start() {
        if (running) return
        try {
            val rate = 22050
            val minBuf = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val t = AudioTrack(
                AudioManager.STREAM_MUSIC, rate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, max(minBuf, 4096), AudioTrack.MODE_STREAM,
            )
            track = t
            running = true
            t.play()
            thread = Thread {
                val buf = ShortArray(512)
                var ph = 0.0
                var ph2 = 0.0
                var sv = 0f
                val rnd = Random(7)
                while (running) {
                    val f = freq.toDouble()
                    for (i in buf.indices) {
                        sv += (vol - sv) * 0.0008f
                        ph += f / rate
                        if (ph >= 1.0) ph -= 1.0
                        ph2 += f * 2.0 / rate
                        if (ph2 >= 1.0) ph2 -= 1.0
                        val saw = 2.0 * ph - 1.0
                        val sq = if (ph < 0.5) 1.0 else -1.0
                        val noise = rnd.nextDouble() - 0.5
                        val sample = (0.5 * saw + 0.25 * sq + 0.2 * sin(2.0 * PI * ph2) + 0.15 * noise) * sv * 9000.0
                        buf[i] = sample.toInt().toShort()
                    }
                    try { t.write(buf, 0, buf.size) } catch (_: Throwable) { break }
                }
            }.also { it.start() }
        } catch (_: Throwable) {
            running = false
        }
    }

    fun stop() {
        if (!running && track == null) return
        running = false
        try { thread?.join(150) } catch (_: Throwable) {}
        try { track?.stop() } catch (_: Throwable) {}
        try { track?.release() } catch (_: Throwable) {}
        track = null
        thread = null
    }

    fun release() = stop()
}

@Composable
fun CarPickDialog(g: GameState, onPick: (Int) -> Unit, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Card {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🚘 Qual carro vai dirigir?", fontWeight = FontWeight.Bold)
                if (g.garage.isEmpty()) Text("Sua garagem está vazia. Compre um carro num ferro-velho primeiro.", fontSize = 13.sp)
                g.garage.forEach { car ->
                    OutlinedButton(onClick = { onPick(car.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text("${CATALOG[car.model].fullName} ${car.year} • ${(carStreetSpeed(car) * 0.5f).toInt()} km/h", fontSize = 13.sp)
                    }
                }
                TextButton(onClick = onClose) { Text("Cancelar") }
            }
        }
    }
}

// ------------------------------------------------------------ arrancada

private enum class DragStage { SETUP, LIGHTS, RACE, RESULT }

private val GEAR_V = floatArrayOf(0.28f, 0.45f, 0.65f, 0.84f, 1.0f)   // velocidade máxima de cada marcha (fração do topo)
private val GEAR_A = floatArrayOf(1.0f, 0.8f, 0.62f, 0.5f, 0.42f)     // força de cada marcha
private const val TRACK = 402f                                         // 1/4 de milha
private val BETS = listOf(0, 1000, 5000, 20000)

@Composable
private fun Pick(selected: Boolean, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(8.dp)) { Text(label, fontSize = 12.sp) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(8.dp)) { Text(label, fontSize = 12.sp) }
    }
}

@Composable
private fun BarFill(frac: Float, color: Color) {
    Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)).background(Color(0xFF37474F))) {
        Box(Modifier.fillMaxWidth(frac.coerceIn(0.001f, 1f)).fillMaxHeight().background(color))
    }
}

@Composable
private fun Speedo(kmh: Float, gear: Int) {
    Box(Modifier.size(170.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2f
            val c = Offset(this.size.width / 2f, this.size.height / 2f)
            val tl = Offset(c.x - r * 0.88f, c.y - r * 0.88f)
            val sz = Size(r * 1.76f, r * 1.76f)
            val frac = (kmh / 320f).coerceIn(0f, 1f)
            drawArc(Color(0xFF37474F), 135f, 270f, false, topLeft = tl, size = sz, style = Stroke(width = 16f))
            drawArc(if (frac > 0.8f) Color(0xFFEF5350) else Color(0xFF66BB6A), 135f, 270f * frac, false, topLeft = tl, size = sz, style = Stroke(width = 16f))
            val ang = (135f + 270f * frac) * PI / 180.0
            drawLine(
                Color.White, c,
                Offset(c.x + (r * 0.72f * cos(ang)).toFloat(), c.y + (r * 0.72f * sin(ang)).toFloat()),
                strokeWidth = 6f,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(54.dp))
            Text("${kmh.toInt()}", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("km/h  •  ${gear + 1}ª", fontSize = 12.sp)
        }
    }
}

@Composable
fun DragDialog(g: GameState, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val engine = remember { EngineSound() }
    var stage by remember { mutableStateOf(DragStage.SETUP) }
    var carId by remember { mutableStateOf<Int?>(g.garage.firstOrNull()?.id) }
    var bet by remember { mutableIntStateOf(0) }
    var t by remember { mutableFloatStateOf(0f) }
    var speed by remember { mutableFloatStateOf(0f) }
    var dist by remember { mutableFloatStateOf(0f) }
    var gear by remember { mutableIntStateOf(0) }
    var rpm by remember { mutableFloatStateOf(0.2f) }
    var gas by remember { mutableStateOf(false) }
    var boost by remember { mutableFloatStateOf(0f) }
    var shiftMsg by remember { mutableStateOf("") }
    var light by remember { mutableIntStateOf(3) }
    var oppT by remember { mutableFloatStateOf(14f) }
    var myT by remember { mutableFloatStateOf(0f) }
    var won by remember { mutableStateOf(false) }
    val car = g.garage.firstOrNull { it.id == carId }

    DisposableEffect(Unit) {
        Music.pause()
        onDispose {
            engine.release()
            Music.start(ctx.applicationContext)
        }
    }

    fun settle(win: Boolean) {
        val id = carId
        if (win) g.money += bet else g.money -= bet
        if (id != null) {
            g.garage = g.garage.map {
                if (it.id == id) it.copy(cond = it.cond.mapIndexed { i, v -> if (i == 0) max(0, v - 2) else v }) else it
            }
        }
        g.spendTime(20)
        g.save()
    }

    fun startRace() {
        val c = car ?: return
        if (bet > g.money) { g.message = "Sem dinheiro pra essa aposta."; return }
        val mult = when {
            bet == 0 -> Random.nextDouble(0.85, 1.05)
            bet <= 1000 -> Random.nextDouble(0.9, 1.15)
            bet <= 5000 -> Random.nextDouble(1.0, 1.3)
            else -> Random.nextDouble(1.1, 1.4)
        }
        oppT = (39.0 * (carPower(c) * mult).pow(-0.21) * Random.nextDouble(0.97, 1.05)).toFloat()
        t = 0f; speed = 0f; dist = 0f; gear = 0; rpm = 0.2f; boost = 0f; shiftMsg = ""; gas = false
        light = 3
        stage = DragStage.LIGHTS
    }

    LaunchedEffect(stage) {
        if (stage == DragStage.LIGHTS) {
            if (Music.enabled) engine.start()
            for (l in 3 downTo 1) {
                light = l
                repeat(14) {
                    engine.set(35f + (if (gas) 110f else 0f), 0.4f)
                    delay(50)
                }
            }
            light = 0
            stage = DragStage.RACE
        } else if (stage == DragStage.RACE) {
            val c = car ?: return@LaunchedEffect
            val pw = carPower(c)
            val grip = 0.7f + 0.3f * c.cond[2] / 100f
            val vTop = (45.0 + pw / 12.0).toFloat()
            val a0 = (5.0 + pw / 60.0).toFloat()
            var last = 0L
            while (stage == DragStage.RACE) {
                withFrameNanos { n ->
                    if (last != 0L) {
                        val dt = ((n - last) / 1e9f).coerceAtMost(0.05f)
                        t += dt
                        val vmax = vTop * GEAR_V[gear]
                        rpm = min(1.05f, max(0.2f, speed / vmax))
                        if (gas) {
                            val tc = if (rpm < 0.95f) 0.6f + 0.4f * rpm else 0.3f
                            var a = a0 * grip * tc * GEAR_A[gear] * (if (boost > 0f) 1.12f else 1f)
                            a -= 0.0006f * speed * speed
                            if (speed >= vmax) a = 0f
                            speed += a * dt
                        } else {
                            speed = max(0f, speed - (3f + 0.002f * speed * speed) * dt)
                        }
                        boost = max(0f, boost - dt)
                        dist += speed * dt
                        engine.set(35f + rpm * 160f, if (gas) 0.9f else 0.4f)
                        if (dist >= TRACK) {
                            myT = t
                            won = myT < oppT
                            settle(won)
                            engine.stop()
                            stage = DragStage.RESULT
                        } else if (t > 40f) {
                            myT = t
                            won = false
                            settle(false)
                            engine.stop()
                            stage = DragStage.RESULT
                        }
                    }
                    last = n
                }
            }
        }
    }

    Dialog(
        onDismissRequest = { if (stage == DragStage.SETUP || stage == DragStage.RESULT) onClose() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🏁 Arrancada • 400 m  •  ${brl(g.money)}", Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    if (stage == DragStage.SETUP || stage == DragStage.RESULT) TextButton(onClick = onClose) { Text("✖ Sair") }
                }
                when (stage) {
                    DragStage.SETUP -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Escolha o carro:", fontWeight = FontWeight.Bold)
                        if (g.garage.isEmpty()) Text("Sua garagem está vazia.", fontSize = 13.sp)
                        g.garage.forEach { c ->
                            Pick(carId == c.id, "${CATALOG[c.model].fullName} ${c.year} • 💪 ${carPower(c).toInt()} cv", Modifier.fillMaxWidth()) { carId = c.id }
                        }
                        Text("Aposta:", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            BETS.forEach { b -> Pick(bet == b, if (b == 0) "Treino" else brl(b), Modifier.weight(1f)) { bet = b } }
                        }
                        Text(
                            "Segure ⛽ ACELERAR pra largar e toque em ⬆ MARCHA quando o giro chegar na faixa verde (perto do fim). " +
                                "Troca perfeita dá um empurrão. Motor e pneus em bom estado ajudam. Apostas maiores têm rival mais forte, " +
                                "e cada corrida desgasta um pouco o motor.",
                            fontSize = 12.sp,
                        )
                        Button(onClick = { startRace() }, enabled = car != null && bet <= g.money, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                            Text("🏁 LARGAR", fontWeight = FontWeight.Bold)
                        }
                        if (g.message.isNotEmpty()) Text(g.message, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    DragStage.LIGHTS, DragStage.RACE -> {
                        val oppDist = min(TRACK, TRACK * (t / oppT).pow(2))
                        val kmh = speed * 3.6f
                        Text(
                            when {
                                stage == DragStage.LIGHTS && light == 3 -> "🔴 🔴 🔴"
                                stage == DragStage.LIGHTS && light == 2 -> "🔴 🔴 ⚫"
                                stage == DragStage.LIGHTS -> "🔴 ⚫ ⚫"
                                t < 1f -> "🟢 VAI!"
                                else -> shiftMsg
                            },
                            Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        )
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Speedo(kmh, gear) }
                        Text("Giro do motor", fontSize = 12.sp)
                        BarFill(rpm, if (rpm in 0.85f..0.98f) Color(0xFF66BB6A) else if (rpm > 0.98f) Color(0xFFEF5350) else Color(0xFFFFCA28))
                        Text("Você  •  ${dist.toInt()} m  •  ${"%.2f".format(t)} s", fontSize = 12.sp)
                        BarFill(dist / TRACK, Color(0xFF42A5F5))
                        Text("Rival  •  ${oppDist.toInt()} m", fontSize = 12.sp)
                        BarFill(oppDist / TRACK, Color(0xFFEF5350))
                        Spacer(Modifier.weight(1f))
                        Row(Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                Modifier.weight(1.4f).fillMaxHeight().clip(RoundedCornerShape(16.dp))
                                    .background(if (gas) Color(0xFF2E7D32) else Color(0xFF455A64))
                                    .pointerInput(Unit) {
                                        detectTapGestures(onPress = { gas = true; tryAwaitRelease(); gas = false })
                                    },
                                contentAlignment = Alignment.Center,
                            ) { Text("⛽ ACELERAR\n(segure)", textAlign = TextAlign.Center, fontWeight = FontWeight.Bold) }
                            Box(
                                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF6A1B9A))
                                    .pointerInput(Unit) {
                                        detectTapGestures(onPress = {
                                            if (stage == DragStage.RACE && gear < 4) {
                                                if (rpm in 0.85f..0.98f) { boost = 0.8f; shiftMsg = "✨ Troca perfeita!" }
                                                else if (rpm < 0.7f) { speed *= 0.97f; shiftMsg = "😬 Cedo demais" }
                                                else if (rpm > 0.98f) shiftMsg = "🐢 Tarde demais"
                                                else shiftMsg = "👍 Boa"
                                                gear += 1
                                            }
                                        })
                                    },
                                contentAlignment = Alignment.Center,
                            ) { Text("⬆ MARCHA", textAlign = TextAlign.Center, fontWeight = FontWeight.Bold) }
                        }
                    }
                    DragStage.RESULT -> Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (won) "🏆 Você venceu!" else "😬 Você perdeu...", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("Seu tempo: ${"%.2f".format(myT)} s  •  Rival: ${"%.2f".format(oppT)} s", fontSize = 14.sp)
                        Text(
                            if (bet == 0) "Corrida de treino, sem dinheiro em jogo."
                            else if (won) "Você ganhou ${brl(bet)}." else "Você pagou ${brl(bet)} ao rival.",
                            fontSize = 14.sp,
                        )
                        Text("O motor desgastou um pouco (−2%).", fontSize = 12.sp)
                        Button(onClick = { stage = DragStage.SETUP }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("🔁 Correr de novo") }
                    }
                    else -> {}
                }
            }
        }
    }
}
