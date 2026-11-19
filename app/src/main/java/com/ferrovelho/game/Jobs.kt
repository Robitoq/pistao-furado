package com.ferrovelho.game

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

/** Trabalhos: escolha um, faça o minijogo de reflexo e receba conforme o desempenho (0,5x a 1,5x). */
class Job(
    val emoji: String, val title: String, val desc: String, val minutes: Int, val pay: Int,
    val from: Int, val to: Int, val bg: Long, val hungerCost: Int,
    val total: Int = 10, val life: Float = 1.8f, val gap: Float = 1.3f,
)

val JOBS = listOf(
    Job("🍕", "Entregador de pizza", "Acerte as entregas antes que esfriem.", 120, 90, 10 * 60, 23 * 60, 0xFF3E2723, 15),
    Job("🧽", "Lavador no lava-jato", "Estoure as manchas de sujeira dos carros.", 90, 70, 7 * 60, 18 * 60, 0xFF0D47A1, 12),
    Job("🥖", "Caixa da padaria", "Atenda os clientes que chegam no balcão.", 180, 120, 6 * 60, 21 * 60, 0xFF6D4C41, 18),
    Job("🔩", "Ajudante de mecânico", "Pegue as peças certas na hora certa.", 120, 110, 8 * 60, 18 * 60, 0xFF37474F, 15),
    Job("🚕", "Motorista de app", "Leve os passageiros rápido, sem atrasar.", 150, 100, 6 * 60, 22 * 60, 0xFF263238, 14, 12, 1.6f, 1.1f),
    Job("🏗️", "Servente de obra", "Pegue os tijolos antes que caiam.", 240, 150, 7 * 60, 16 * 60, 0xFF795548, 22, 12, 1.7f, 1.2f),
    Job("📦", "Estoquista do mercadinho", "Arrume as caixas nas prateleiras.", 120, 80, 6 * 60, 22 * 60, 0xFF1B5E20, 12),
    Job("🧹", "Faxineiro de prédio", "Cace a sujeira dos corredores.", 90, 75, 8 * 60, 17 * 60, 0xFF4527A0, 10, 8, 2.2f, 1.5f),
    Job("🚛", "Motorista de guincho", "Atenda os chamados de socorro na estrada.", 120, 130, 8 * 60, 20 * 60, 0xFFBF360C, 16, 10, 1.5f, 1.2f),
    Job("🎵", "DJ de festa", "Acerte as batidas no ritmo da pista.", 180, 220, 20 * 60, 4 * 60, 0xFF311B92, 16, 14, 1.2f, 0.9f),
    Job("🌙", "Vigia noturno", "Fique de olho e pegue os vultos suspeitos.", 240, 180, 22 * 60, 6 * 60, 0xFF0B1026, 20),
)

fun jobOpen(j: Job, minutes: Int) = if (j.from < j.to) minutes in j.from until j.to else minutes >= j.from || minutes < j.to

private class Tgt(val x: Float, val y: Float, val born: Float) { var hit = false }

@Composable
fun JobsDialog(g: GameState, onClose: () -> Unit) {
    var sel by remember { mutableStateOf<Job?>(null) }
    var result by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💼 Trabalhos", Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onClose) { Text("✖ Sair") }
                }
                Text("${g.dateText()}  •  ${brl(g.money)}  •  🍔 ${g.hunger.toInt()}%  🥤 ${g.thirst.toInt()}%", fontSize = 12.sp)
                val job = sel
                if (job == null) {
                    if (result.isNotEmpty()) Text(result, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    JOBS.forEach { j ->
                        val open = jobOpen(j, g.minutes)
                        val tired = g.hunger < 20 || g.thirst < 15
                        Card(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(j.emoji, fontSize = 30.sp)
                                Column(Modifier.weight(1f)) {
                                    Text(j.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${j.desc}\n${j.minutes / 60}h de trabalho • até ${brl((j.pay * 1.5).roundToInt())} • ${if (j.from < j.to) "${j.from / 60}h às ${j.to / 60}h" else "${j.from / 60}h às ${j.to / 60}h (noite)"}", fontSize = 11.sp)
                                }
                                Button(onClick = { sel = j; result = "" }, enabled = open && !tired) { Text(if (!open) "Fechado" else if (tired) "Com fome" else "Ir") }
                            }
                        }
                    }
                } else {
                    JobGame(job) { hits, total ->
                        val pay = (job.pay * (0.5 + hits.toDouble() / total)).roundToInt()
                        g.money += pay; g.hunger = max(0.0, g.hunger - job.hungerCost); g.thirst = max(0.0, g.thirst - 10.0)
                        g.spendTime(job.minutes)
                        result = "${job.emoji} Turno de ${job.title} feito: $hits/$total acertos, você ganhou ${brl(pay)}."
                        g.message = result
                        sel = null
                    }
                }
            }
        }
    }
}

@Composable
private fun JobGame(job: Job, onDone: (Int, Int) -> Unit) {
    val total = job.total
    val tg = remember { mutableListOf<Tgt>() }
    var tick by remember { mutableIntStateOf(0) }
    var hits by remember { mutableIntStateOf(0) }
    var t by remember { mutableFloatStateOf(0f) }
    var spawned by remember { mutableIntStateOf(0) }
    val rnd = remember { Random(System.nanoTime()) }
    LaunchedEffect(Unit) {
        var last = 0L; var finished = false
        while (!finished) {
            withFrameNanos { ns ->
                val dt = if (last == 0L) 0f else ((ns - last) / 1e9f).coerceAtMost(0.05f)
                last = ns; t += dt
                if (spawned < total && t >= 0.8f + spawned * job.gap) { tg.add(Tgt(0.12f + rnd.nextFloat() * 0.76f, 0.12f + rnd.nextFloat() * 0.76f, t)); spawned++ }
                tick++
                if (spawned >= total && t >= 0.8f + total * job.gap + 1.2f) { finished = true; onDone(hits, total) }
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("${job.emoji} ${job.title} — acertos: $hits/$total", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Toque nos alvos antes que sumam!", fontSize = 12.sp)
        Canvas(
            Modifier.fillMaxWidth().aspectRatio(0.85f).pointerInput(Unit) {
                detectTapGestures { off ->
                    val w = size.width.toFloat(); val h = size.height.toFloat()
                    val hit = tg.filter { !it.hit && t - it.born < job.life }.minByOrNull { (it.x * w - off.x) * (it.x * w - off.x) + (it.y * h - off.y) * (it.y * h - off.y) }
                    if (hit != null && sqrt((hit.x * w - off.x) * (hit.x * w - off.x) + (hit.y * h - off.y) * (hit.y * h - off.y)) < w * 0.12f) { hit.hit = true; hits++ }
                }
            },
        ) {
            @Suppress("UNUSED_EXPRESSION") tick
            drawRect(Color(job.bg))
            val p = Paint().apply { textSize = size.width * 0.12f; textAlign = Paint.Align.CENTER }
            for (a in tg) {
                val age = t - a.born
                if (a.hit || age > job.life) continue
                val cx = a.x * size.width; val cy = a.y * size.height
                drawCircle(Color(0x33FFFFFF), size.width * 0.09f, Offset(cx, cy))
                drawCircle(Color(0xFFFFEB3B), size.width * 0.09f * (1f - age / job.life) + 4f, Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(5f))
                drawIntoCanvas { it.nativeCanvas.drawText(job.emoji, cx, cy + p.textSize * 0.35f, p) }
            }
        }
    }
}
