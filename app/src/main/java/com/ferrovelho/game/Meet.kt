package com.ferrovelho.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

// =====================================================================================
//  ENCONTRO DE CARROS
//  Todo dia, a partir das 18h, rola um encontro na cidade. Cada carro ganha duas notas:
//    - BELEZA (qual é o mais bonito): lataria, interior e tuning visual
//    - ESTADO (qual está melhor): motor, pneus, suspensão e lataria
//  A nota final junta as duas e define o pódio (1º, 2º e 3º lugar), com prêmio em dinheiro.
//  Mexa à vontade em MEET_PRIZES, VISUAL_TUNE e nos pesos dentro de meetScore().
// =====================================================================================

const val MEET_START = 18 * 60
val MEET_PRIZES = listOf(4000, 2000, 800)

/** Quantos pontos de beleza cada tuning dá (o total é limitado a 45). */
private val VISUAL_TUNE = mapOf(
    "rodas" to 9, "rebaixada" to 9, "neon" to 8, "adesivo" to 6, "aerofolio" to 7, "parachoque" to 7,
    "pelicula" to 4, "farol" to 5, "metalica" to 6, "couro" to 5, "turbo" to 3, "escape" to 3, "som" to 4,
)

private val MEET_NAMES = listOf(
    "Rafa Turbo", "Dani Rebaixada", "Kleber", "Thaís", "Mão de Vaca", "Gui do Som",
    "Dona Lúcia", "Tio Jorge", "Vini Nitro", "Bia Tuning", "Careca", "Juninho",
)

class MeetCar(
    val owner: String,
    val look: Look,
    val isPlayer: Boolean,
    val model: Int,
    val year: Int,
    val color: Int,
    val cond: List<Int>,
    val tuning: List<String>,
)

/** Notas de 0 a 10. */
class MeetScore(val beauty: Double, val state: Double, val total: Double)

class MeetEntry(val car: MeetCar, val score: MeetScore)

/** Os 5 concorrentes do dia (os mesmos o dia todo; muda quando o dia muda). */
fun genMeetField(day: Int): List<MeetCar> {
    val r = Random(day * 7919 + 31)
    val names = MEET_NAMES.shuffled(r).take(5)
    val ids = TUNINGS.map { it.id }
    return names.map { n ->
        val mi = r.nextInt(CATALOG.size)
        val m = CATALOG[mi]
        val year = r.nextInt(m.from, m.to + 1)
        val cond = List(5) { r.nextInt(40, 101) }
        val tune = ids.shuffled(r).take(r.nextInt(0, 7))
        MeetCar(n, randomLook(r), false, mi, year, r.nextInt(PAINTS.size), cond, tune)
    }
}

private fun r1(v: Double): Double = (v * 10.0).roundToInt() / 10.0

private fun nota(v: Double): String = String.format(Locale.forLanguageTag("pt-BR"), "%.1f", v)

/** seed == null: nota "limpa", sem a variação dos jurados (usada na prévia). */
fun meetScore(c: MeetCar, seed: Int?): MeetScore {
    val m = CATALOG[c.model]
    val tune = min(45, c.tuning.sumOf { VISUAL_TUNE[it] ?: 0 })
    val looks = c.cond[1] * 0.40 + c.cond[3] * 0.15 + tune
    val prestige = when (m.cat) {
        Cat.ESPORTIVO -> 5.0
        Cat.CLASSICO -> 6.0
        Cat.SUV -> 2.0
        Cat.SEDAN -> 1.0
        else -> 0.0
    }
    val state = c.cond[0] * 0.40 + c.cond[2] * 0.20 + c.cond[4] * 0.25 + c.cond[1] * 0.15
    val rnd = seed?.let { Random(it) }
    val nb = rnd?.nextDouble(-3.0, 3.0) ?: 0.0
    val ns = rnd?.nextDouble(-3.0, 3.0) ?: 0.0
    val b = (looks + prestige + nb).coerceIn(0.0, 100.0)
    val s = (state + ns).coerceIn(0.0, 100.0)
    val total = b * 0.65 + s * 0.35
    return MeetScore(r1(b / 10.0), r1(s / 10.0), r1(total / 10.0))
}

private fun judgeComment(s: MeetScore, pos: Int): String = when {
    pos == 0 -> "Que máquina! Levou o troféu com folga."
    s.beauty >= s.state + 1.5 -> "Bonito demais, mas a mecânica deixou a desejar."
    s.state >= s.beauty + 1.5 -> "Mecânica em dia, mas faltou capricho no visual (lataria, interior e tuning)."
    s.total < 5.0 -> "Precisa de bastante trabalho antes do próximo encontro."
    else -> "Bom carro. Com mais um pouco de tuning, dá pódio."
}

@Composable
fun CarMeetDialog(g: GameState, pname: String, look: Look, onClose: () -> Unit) {
    val field = remember { genMeetField(g.day) }
    var sel by remember { mutableStateOf<Int?>(null) }
    var chosen by remember { mutableStateOf<OwnedCar?>(null) }
    var results by remember { mutableStateOf<List<MeetEntry>?>(null) }
    var prize by remember { mutableStateOf(0) }
    var myPos by remember { mutableStateOf(-1) }

    val alreadyToday = g.meetDay == g.day && results == null
    val isOpen = g.minutes >= MEET_START

    // Os jurados avaliam (o encontro só "vale" no fim: se fechar antes, nada é gasto)
    LaunchedEffect(chosen) {
        val car = chosen ?: return@LaunchedEffect
        Voice.speak("Os jurados estão avaliando os carros.", 1f, 1f)
        delay(2600)
        val me = MeetCar(pname, look, true, car.model, car.year, car.color, car.cond, car.tuning)
        val all = field + me
        val sorted = all.mapIndexed { i, mc ->
            MeetEntry(mc, meetScore(mc, g.day * 131 + i * 17 + (if (mc.isPlayer) car.id else 0)))
        }.sortedWith(compareByDescending<MeetEntry> { it.score.total }.thenByDescending { it.score.beauty })
        val pos = sorted.indexOfFirst { it.car.isPlayer }
        val pz = MEET_PRIZES.getOrElse(pos) { 0 }
        g.meetDay = g.day
        g.spendTime(120)
        g.money += pz
        if (pos == 0) g.meetWins += 1
        if (g.meetBest == 0 || pos + 1 < g.meetBest) g.meetBest = pos + 1
        g.message = if (pz > 0) "Encontro de carros: você ficou em ${pos + 1}º lugar e ganhou ${brl(pz)}!"
        else "🏁 Encontro de carros: você ficou em ${pos + 1}º lugar. Capricha no visual e volta amanhã!"
        g.save()
        prize = pz
        myPos = pos
        results = sorted
        Voice.speak(
            if (pos == 0) "E o campeão do encontro de hoje é $pname! Parabéns!"
            else "O campeão de hoje é ${sorted[0].car.owner}. Você ficou em ${pos + 1} lugar.",
            1.1f, 1.05f,
        )
    }

    val busy = chosen != null && results == null

    Dialog(
        onDismissRequest = { if (!busy) onClose() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(Modifier.fillMaxWidth(0.94f)) {
            Column(
                Modifier.padding(14.dp).heightIn(max = 640.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Encontro de Carros", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Duas notas de 0 a 10: BELEZA (qual é o mais bonito: lataria, interior e tuning) e " +
                        "ESTADO (qual está melhor: motor, pneus, suspensão). Pódio: 1º ${brl(MEET_PRIZES[0])} • " +
                        "2º ${brl(MEET_PRIZES[1])} • 3º ${brl(MEET_PRIZES[2])}.",
                    fontSize = 12.sp,
                )
                if (g.meetWins > 0) Text("Vitórias: ${g.meetWins}", fontSize = 12.sp)

                val res = results
                when {
                    res != null -> MeetResults(res, myPos, prize, pname, onClose)

                    busy -> {
                        Text("Os jurados estão avaliando os carros...", fontWeight = FontWeight.Bold)
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text("Concorrentes de hoje:", fontSize = 12.sp)
                        field.forEach {
                            Text("• ${it.owner} — ${CATALOG[it.model].fullName} ${it.year}", fontSize = 12.sp)
                        }
                    }

                    alreadyToday -> {
                        Text("Você já participou do encontro de hoje. Volte amanhã!")
                        TextButton(onClick = onClose) { Text("Sair") }
                    }

                    !isOpen -> {
                        Text("O encontro começa às 18h. Os carros ainda estão chegando...")
                        OutlinedButton(
                            onClick = { g.spendTime(MEET_START - g.minutes) },
                            Modifier.fillMaxWidth(),
                        ) { Text("Esperar até as 18h") }
                        TextButton(onClick = onClose) { Text("Sair") }
                    }

                    g.garage.isEmpty() -> {
                        Text("Você não tem nenhum carro na garagem. Compre um no ferro velho, arrume e volte pra competir!")
                        TextButton(onClick = onClose) { Text("Sair") }
                    }

                    else -> {
                        Text("Escolha o carro que vai pro encontro:", fontWeight = FontWeight.Bold)
                        g.garage.forEach { car ->
                            val m = CATALOG[car.model]
                            val prev = meetScore(MeetCar(pname, look, true, car.model, car.year, car.color, car.cond, car.tuning), null)
                            val on = sel == car.id
                            Row(
                                Modifier.fillMaxWidth()
                                    .border(if (on) 2.dp else 1.dp, if (on) Color(0xFF66BB6A) else Color.Gray, RoundedCornerShape(8.dp))
                                    .clickable { sel = car.id }
                                    .padding(6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CarArt(m.body, car.color, car.tuning, car.cond, Modifier.width(96.dp).height(40.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("${m.fullName} ${car.year}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("Beleza ~${nota(prev.beauty)} • Estado ~${nota(prev.state)}", fontSize = 11.sp)
                                }
                            }
                        }
                        Text("Participar leva umas 2 horas.", fontSize = 11.sp)
                        Button(
                            onClick = { chosen = g.garage.firstOrNull { it.id == sel } },
                            Modifier.fillMaxWidth(),
                            enabled = sel != null,
                        ) { Text("Participar do encontro") }
                        TextButton(onClick = onClose) { Text("Sair") }
                    }
                }
            }
        }
    }
}

@Composable
private fun MeetResults(res: List<MeetEntry>, myPos: Int, prize: Int, pname: String, onClose: () -> Unit) {
    // ---- pódio (2º, 1º, 3º)
    Text("Pódio", fontWeight = FontWeight.Bold, fontSize = 16.sp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
        listOf(1, 0, 2).forEach { i ->
            val e = res[i]
            val medal = listOf("🥇", "🥈", "🥉")[i]
            val h = listOf(70, 52, 38)[i]
            val col = listOf(Color(0xFFFFC107), Color(0xFFB0BEC5), Color(0xFFBF7B45))[i]
            Column(Modifier.width(100.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(medal, fontSize = 26.sp)
                Text(
                    e.car.owner + if (e.car.isPlayer) " (você)" else "",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                )
                Text(CATALOG[e.car.model].fullName, fontSize = 10.sp, textAlign = TextAlign.Center)
                Text(nota(e.score.total), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.fillMaxWidth().height(h.dp).background(col, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)), Alignment.Center) {
                    Text("${i + 1}º", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }
    }

    // ---- prêmios de beleza e estado
    val pretty = res.maxByOrNull { it.score.beauty }
    val best = res.maxByOrNull { it.score.state }
    if (pretty != null) Text("Carro mais bonito: ${pretty.car.owner} — ${CATALOG[pretty.car.model].fullName} (${nota(pretty.score.beauty)})", fontSize = 12.sp)
    if (best != null) Text("Carro em melhor estado: ${best.car.owner} — ${CATALOG[best.car.model].fullName} (${nota(best.score.state)})", fontSize = 12.sp)

    // ---- classificação completa
    Text("Classificação", fontWeight = FontWeight.Bold, fontSize = 14.sp)
    res.forEachIndexed { i, e ->
        val m = CATALOG[e.car.model]
        Row(
            Modifier.fillMaxWidth()
                .background(if (e.car.isPlayer) Color(0x3366BB6A) else Color.Transparent, RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${i + 1}º", Modifier.width(28.dp), fontWeight = FontWeight.Bold)
            CarArt(m.body, e.car.color, e.car.tuning, e.car.cond, Modifier.width(80.dp).height(34.dp), e.car.look)
            Column(Modifier.weight(1f)) {
                Text(e.car.owner + if (e.car.isPlayer) " (você)" else "", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("${m.fullName} ${e.car.year}", fontSize = 11.sp)
                Text("Beleza ${nota(e.score.beauty)} • Estado ${nota(e.score.state)}", fontSize = 11.sp)
            }
            Text(nota(e.score.total), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }

    // ---- recado dos jurados + prêmio
    if (myPos >= 0) {
        Text("Jurados: ${judgeComment(res[myPos].score, myPos)}", fontSize = 13.sp)
        Text(
            if (prize > 0) "💰 Prêmio: ${brl(prize)}" else "Sem prêmio dessa vez, $pname.",
            fontWeight = FontWeight.Bold, fontSize = 14.sp,
        )
    }
    Button(onClick = onClose, Modifier.fillMaxWidth()) { Text("Fechar") }
}
