package com.ferrovelho.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.abs
import kotlin.math.roundToInt

/*
 * =====================================================================================
 *  TRABALHAR: painel das profissões + HUD do turno.
 *  A lógica dos empregos (turnos, pedidos, cargos, pagamento) está em Work.kt.
 *  Aqui o jogador escolhe a profissão: o jogo marca o local de trabalho no mapa e ele precisa IR até lá,
 *  bater o ponto e fazer o serviço no cenário. Não existe mais minigame de clicar.
 * =====================================================================================
 */

private fun distText(from: Float, to: Float): String {
    val d = to - from
    val m = abs(d).roundToInt()
    val s = if (m >= 1000) String.format("%.1f km", m / 1000f) else "$m m"
    return if (d < 0f) "◀ $s" else "$s ▶"
}

private fun leftText(min: Int): String =
    if (min >= DAY_MIN) "sem hora pra acabar" else "${min / 60}h${String.format("%02d", min % 60)} de jornada restante"

@Composable
fun JobsDialog(g: GameState, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Trabalhar", Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onClose) { Text("Sair") }
                }
                Text("${g.dateText()} • ${brl(g.money)} • 🍔${g.hunger.toInt()}% 🥤${g.thirst.toInt()}% 😴${g.energy.toInt()}%", fontSize = 12.sp)
                Text(
                    "Empregos de verdade: escolha a profissão, vá até o local de trabalho no mapa, bata o ponto e faça o serviço no cenário. " +
                        "O relógio não para: dá pra trabalhar de dia ou de madrugada, dentro da jornada de cada emprego.",
                    fontSize = 12.sp,
                )
                val sh = g.shift
                if (sh != null) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20))) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${sh.job.emoji} Em turno: ${sh.job.title}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                            Text("${sh.tasks} serviço(s) • ${brl(sh.earned)} ganhos • +${sh.xpGain} XP", fontSize = 12.sp, color = Color.White)
                            OutlinedButton(onClick = { endShift(g, "Você encerrou o expediente."); onClose() }, Modifier.fillMaxWidth()) { Text("Encerrar turno") }
                        }
                    }
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WORK_JOBS.forEach { j -> JobCard(g, j, onClose) }
                    Text("Total ganho trabalhando: ${brl(g.careerEarned)}", fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun JobCard(g: GameState, j: WorkJob, onClose: () -> Unit) {
    val open = workOpen(j, g)
    val block = workBlock(g, j)
    val idx = workRankIdx(g, j)
    val rank = j.ranks[idx]
    val xp = workXp(g, j)
    val next = j.ranks.getOrNull(idx + 1)
    val frac = if (next == null) 1f else ((xp - rank.xp).toFloat() / (next.xp - rank.xp).toFloat()).coerceIn(0f, 1f)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(j.emoji, fontSize = 30.sp)
                Column(Modifier.weight(1f)) {
                    Text(j.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Cargo: ${rank.name} • paga ×${String.format("%.2f", rank.mult)}", fontSize = 12.sp)
                }
                Text(if (open) "ABERTO" else "FECHADO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (open) Color(0xFF66BB6A) else Color(0xFFEF5350))
            }
            Text(j.blurb, fontSize = 12.sp)
            Text("📍 ${j.place} • 🕒 ${workHours(j, g)} • 💵 ${j.payHint}", fontSize = 11.sp)
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF14121A))) {
                Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(Color(0xFF42A5F5)))
            }
            Text(
                if (next == null) "Cargo máximo • $xp XP • ${g.careerTasks[j.id] ?: 0} serviços"
                else "XP $xp / ${next.xp} para ${next.name} • ${g.careerTasks[j.id] ?: 0} serviços feitos",
                fontSize = 11.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (g.shift == null) {
                            g.heading = j.id
                            g.message = "${j.emoji} Vá até ${j.place} (marcado no mapa) e bata o ponto."
                            onClose()
                        }
                    },
                    Modifier.weight(1f), enabled = block == null && g.heading != j.id,
                ) { Text(if (g.heading == j.id) "Marcado no mapa" else if (block == null) "Ir ao trabalho" else if (!open) "Fechado" else "Indisponível", fontSize = 12.sp) }
                if (canPromote(g, j)) {
                    FilledTonalButton(onClick = { promote(g, j) }, Modifier.weight(1f)) { Text("Pedir promoção ⭐", fontSize = 12.sp) }
                }
            }
            if (block != null && open && g.shift == null) Text(block, fontSize = 11.sp, color = Color(0xFFFFA726))
        }
    }
}

/** Faixa no topo do mapa: pra onde ir, o que fazer agora e quanto falta. Só aparece com profissão marcada ou turno ativo. */
@Composable
fun WorkHud(g: GameState, px: Float) {
    val sh = g.shift
    val hj = g.heading?.let { id -> WORK_JOBS.firstOrNull { it.id == id } }
    if (sh == null && hj == null) return
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xEE1E1B26)),
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            if (sh == null && hj != null) {
                val near = abs(px - hj.x) < 45f
                val canStart = workBlock(g, hj) == null
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${hj.emoji} Ir para: ${hj.place}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        Text(if (near) "Você chegou! Bata o ponto." else distText(px, hj.x), fontSize = 12.sp, color = Color(0xFFFFD54F))
                    }
                    if (near) Button(onClick = { startShift(g, hj) }, enabled = canStart, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) { Text("Bater ponto", fontSize = 12.sp) }
                    TextButton(onClick = { g.heading = null }, contentPadding = PaddingValues(6.dp)) { Text("Cancelar", fontSize = 12.sp) }
                }
                if (near && !canStart) workBlock(g, hj)?.let { Text(it, fontSize = 11.sp, color = Color(0xFFFFA726)) }
            } else if (sh != null) {
                val j = sh.job
                val st = sh.step
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${j.emoji} ${j.title} • ${workRank(g, j).name} • ${brl(sh.earned)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        Text("${leftText(workMinutesLeft(j, g))}" + (if (sh.order.isNotEmpty()) " • ${sh.order}" else ""), fontSize = 10.sp, color = Color(0xFFB0BEC5), maxLines = 1)
                    }
                    TextButton(onClick = { endShift(g, "Você encerrou o expediente.") }, contentPadding = PaddingValues(6.dp)) { Text("Encerrar", fontSize = 12.sp) }
                }
                if (st != null) {
                    val needVeh = st.inVeh && sh.veh == null
                    Text(
                        "${st.icon} ${st.label} • ${distText(px, st.x)}" + (if (needVeh) " (pegue o veículo)" else ""),
                        fontSize = 12.sp, color = Color(0xFFFFD54F), maxLines = 1,
                    )
                    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF14121A))) {
                        Box(Modifier.fillMaxWidth(sh.progress.coerceIn(0f, 1f)).fillMaxHeight().background(Color(0xFF66BB6A)))
                    }
                    val extra = buildString {
                        if (sh.carrying.isNotEmpty()) append("Carregando: ${sh.carrying}")
                        if (st.deadline >= 0) {
                            val left = st.deadline - g.absMinutes()
                            if (isNotEmpty()) append(" • ")
                            append(if (left >= 0) "prazo: ${left} min" else "atrasado!")
                        }
                        if (st.inVeh && sh.veh != null && abs(px - st.x) < 34f) {
                            if (isNotEmpty()) append(" • ")
                            append("pare no ponto")
                        }
                    }
                    if (extra.isNotEmpty()) Text(extra, fontSize = 10.sp, color = Color(0xFFB0BEC5), maxLines = 1)
                }
            }
        }
    }
}
