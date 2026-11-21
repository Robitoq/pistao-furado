package com.ferrovelho.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.ceil

// =====================================================================================
//  TRANSPORTE PÚBLICO: pontos, linhas de verdade, horários e ônibus que chegam no ponto
// =====================================================================================

class BusStop(val name: String, val x: Float)

/** 12 pontos: 10 na cidade, 1 no parque e 1 na praia. O índice é o mesmo do mapa (Item.idx). */
val BUS_STOPS = listOf(
    BusStop("Vila Pistão", 620f),
    BusStop("Praça do Encontro", 2350f),
    BusStop("Arrancada", 4300f),
    BusStop("Moda Pistão", 6900f),
    BusStop("Bairro Novo", 9800f),
    BusStop("Av. Comercial", 13200f),
    BusStop("Jardim das Flores", 16800f),
    BusStop("Vila Nova", 20500f),
    BusStop("Bairro Alto", 24400f),
    BusStop("Terminal Norte", 28100f),
    BusStop("Parque Central", CITY_END + 140f),
    BusStop("Praia", BEACH_X - 260f),
)

/** paint = índice em PAINTS (cor do ônibus). period = segundos reais entre um ônibus e o próximo (1 s real = 2 min no jogo). */
class BusLine(
    val short: String, val name: String, val route: String,
    val color: Color, val paint: Int, val stops: List<Int>, val period: Float, val flatFare: Int, val express: Boolean,
)

val BUS_LINES = listOf(
    BusLine("L1", "Linha 1", "Vila ↔ Bairro Novo", Color(0xFFC62828), 4, listOf(0, 1, 2, 3, 4), 20f, 0, false),
    BusLine("L2", "Linha 2", "Bairro Novo ↔ Terminal Norte", Color(0xFF1565C0), 5, listOf(4, 5, 6, 7, 8, 9), 24f, 0, false),
    BusLine("L3", "Linha 3 Expresso", "Vila ↔ Terminal Norte (poucas paradas)", Color(0xFFEF6C00), 8, listOf(0, 3, 4, 9), 36f, 8, true),
    BusLine("L4", "Linha 4 Orla", "Terminal Norte ↔ Parque ↔ Praia", Color(0xFF2E7D32), 6, listOf(9, 10, 11), 30f, 0, false),
)

fun linesAt(stop: Int): List<Int> = BUS_LINES.indices.filter { stop in BUS_LINES[it].stops }

fun stopLabel(stop: Int): String = linesAt(stop).joinToString(" ") { BUS_LINES[it].short }

fun busOpen(g: GameState): Boolean = g.minutes in (5 * 60) until (23 * 60)

const val BUS_APPROACH = 5f   // segundos chegando
const val BUS_DWELL = 6f      // segundos parado no ponto
const val BUS_LEAVE = 5f      // segundos saindo

/** Segundo atual dentro do ciclo desta linha NESTE ponto (os pontos não ficam todos sincronizados). */
private fun busCycle(line: Int, stop: Int, t: Float): Float {
    val p = BUS_LINES[line].period
    val v = (t + stop * 5.3f + line * 7.1f) % p
    return if (v < 0f) v + p else v
}

/** Estado do ônibus da linha neste ponto: 0 = longe, 1 = chegando, 2 = parado, 3 = saindo. prog = progresso 0..1 dentro do estado. */
class BusAt(val state: Int, val prog: Float)

fun busAt(line: Int, stop: Int, t: Float): BusAt {
    val c = busCycle(line, stop, t)
    return when {
        c < BUS_APPROACH -> BusAt(1, c / BUS_APPROACH)
        c < BUS_APPROACH + BUS_DWELL -> BusAt(2, (c - BUS_APPROACH) / BUS_DWELL)
        c < BUS_APPROACH + BUS_DWELL + BUS_LEAVE -> BusAt(3, (c - BUS_APPROACH - BUS_DWELL) / BUS_LEAVE)
        else -> BusAt(0, 0f)
    }
}

/** Minutos de jogo até o ônibus parar no ponto (0 = está parado agora, dá pra embarcar). */
fun busWaitMin(line: Int, stop: Int, t: Float): Int {
    val c = busCycle(line, stop, t)
    val p = BUS_LINES[line].period
    val sec = when {
        c >= BUS_APPROACH && c < BUS_APPROACH + BUS_DWELL -> 0f
        c < BUS_APPROACH -> BUS_APPROACH - c
        else -> p - c + BUS_APPROACH
    }
    return ceil(sec * 2f).toInt()
}

fun busFare(line: Int, from: Int, to: Int): Int {
    val l = BUS_LINES[line]
    if (l.flatFare > 0) return l.flatFare
    return 3 + abs(l.stops.indexOf(from) - l.stops.indexOf(to))
}

fun busRideMin(line: Int, from: Int, to: Int): Int {
    val l = BUS_LINES[line]
    val dx = abs(BUS_STOPS[from].x - BUS_STOPS[to].x)
    val speed = if (l.express) 380f else 220f
    return (dx / speed).toInt().coerceAtLeast(6)
}

fun busWaitText(min: Int): String = if (min <= 0) "no ponto agora" else "chega em ~$min min"

@Composable
fun BusStopDialog(
    g: GameState,
    stop: Int,
    nowSec: Float,
    onClose: () -> Unit,
    onRide: (dest: Int, fare: Int, minutes: Int) -> Unit,
) {
    val open = busOpen(g)
    val here = BUS_STOPS[stop]
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Ponto: ${here.name}") },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    if (open) "Funciona das 5h às 23h • você tem ${brl(g.money)}"
                    else "O transporte funciona das 5h às 23h. Volte mais tarde.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                linesAt(stop).forEach { li ->
                    val line = BUS_LINES[li]
                    val wait = busWaitMin(li, stop, nowSec)
                    Column(
                        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)).padding(8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                line.short,
                                Modifier.background(line.color, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White,
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(line.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(line.route, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text("Próximo ônibus: ${busWaitText(wait)}", Modifier.padding(top = 4.dp), fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        line.stops.filter { it != stop }.forEach { dest ->
                            val fare = busFare(li, stop, dest)
                            val ride = busRideMin(li, stop, dest)
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f).padding(end = 6.dp)) {
                                    Text(BUS_STOPS[dest].name, fontSize = 13.sp)
                                    Text("Viagem ~$ride min" + (if (wait > 0) " + espera" else ""), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(
                                    onClick = { onRide(dest, fare, wait + ride) },
                                    enabled = open && g.money >= fare,
                                ) { Text(brl(fare)) }
                            }
                        }
                    }
                }
                if (open && g.money < 3) Text("Você precisa de dinheiro pra pagar a passagem.", fontSize = 12.sp)
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Fechar") } },
    )
}
