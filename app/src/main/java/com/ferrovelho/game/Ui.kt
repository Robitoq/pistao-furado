package com.ferrovelho.game

import androidx.compose.foundation.Canvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import kotlin.math.roundToInt

private val GREEN = Color(0xFF66BB6A)
private val RED = Color(0xFFEF5350)

private fun condColor(v: Int): Color = when {
    v >= 70 -> GREEN
    v >= 40 -> Color(0xFFFFCA28)
    else -> RED
}

@Composable
fun FerroVelhoApp(g: GameState) {
    LaunchedEffect(Unit) { while (true) { delay(1000); g.clockTick() } }
    Scaffold(topBar = { TopStatus(g) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (g.message.isNotEmpty()) MessageBar(g)
            CityScreen(g)
        }
    }
    if (g.bankrupt) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("🏚️ Você faliu!") },
            text = { Text("As dívidas passaram do limite e o banco levou tudo. Você chegou ao dia ${g.day}, com ${g.sold} carros e ${g.hSold} casas vendidas. Dá pra recomeçar do zero.") },
            confirmButton = { TextButton(onClick = { g.newGame() }) { Text("Recomeçar") } },
        )
    }
}

@Composable
private fun TopStatus(g: GameState) {
    Surface(tonalElevation = 3.dp) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(brl(g.money), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = if (g.money >= 0) GREEN else RED)
                Text(g.dateText(), fontSize = 12.sp)
                Text("Dia ${g.day} • mercado ${(g.marketIndex() * 100).roundToInt()}% ${g.trendArrow()}", fontSize = 12.sp)
                g.event?.let { Text("${it.title} — ${it.text}", fontSize = 11.sp) }
            }
            var snd by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(Music.enabled) }
            val sctx = androidx.compose.ui.platform.LocalContext.current
            Text(
                if (snd) "🔊" else "🔇", fontSize = 22.sp,
                modifier = Modifier.clickable { snd = Music.toggle(sctx.applicationContext) }.padding(end = 10.dp),
            )
            var vz by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(Voice.enabled) }
            Text(
                if (vz) "🗣️" else "🤐", fontSize = 22.sp,
                modifier = Modifier.clickable { vz = Voice.toggle(sctx.applicationContext) }.padding(end = 10.dp),
            )
        }
    }
}

@Composable
private fun MessageBar(g: GameState) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).clickable { g.message = "" },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Text(g.message + "  (toque para fechar)", Modifier.padding(10.dp), fontSize = 13.sp)
    }
}

// ------------------------------------------------------------------ ferro velho

@Composable
private fun YardScreen(g: GameState) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        g.yards.forEachIndexed { yi, list ->
            item(key = "yard-header-$yi") {
                Column(Modifier.padding(top = 6.dp)) {
                    Text(YARD_NAMES[yi], style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(YARD_DESC[yi], fontSize = 12.sp)
                    if (list.isEmpty()) Text("Nada sobrou aqui hoje. Passe o dia!", Modifier.padding(top = 6.dp))
                }
            }
            items(list, key = { it.id }) { y -> YardCard(g, yi, y) }
        }
    }
}

@Composable
private fun YardCard(g: GameState, yi: Int, y: YardCar) {
    val m = CATALOG[y.model]
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(m.fullName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("${y.year} • ${m.cat.label}", fontSize = 12.sp)
                }
                Text(brl(y.price), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
            }
            CarArt(m.body, y.color, emptyList(), y.cond, Modifier.fillMaxWidth().height(110.dp))
            CondBars(y.cond)
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Vale hoje do jeito que está: ${brl(g.yardValue(y))}",
                    Modifier.weight(1f),
                    fontSize = 12.sp,
                )
                Button(
                    onClick = { g.buy(yi, y) },
                    enabled = g.money >= y.price && g.garage.size < g.slots,
                ) { Text("Comprar") }
            }
        }
    }
}

@Composable
private fun CondBars(cond: List<Int>) {
    Column {
        PART_NAMES.forEachIndexed { i, name ->
            val v = cond[i]
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, Modifier.width(78.dp), fontSize = 12.sp)
                LinearProgressIndicator(
                    progress = { v / 100f },
                    modifier = Modifier.weight(1f).height(7.dp),
                    color = condColor(v),
                )
                Text("$v%", Modifier.width(44.dp), fontSize = 11.sp, textAlign = TextAlign.End)
            }
        }
    }
}

// ------------------------------------------------------------------ garagem

@Composable
fun GarageScreen(g: GameState, onOpen: (Int) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Sua garagem", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Vagas: ${g.garage.size}/${g.slots}", fontSize = 12.sp)
                }
                OutlinedButton(onClick = { g.buySlot() }, enabled = g.money >= g.slotCost) {
                    Text("+ vaga ${brl(g.slotCost)}", fontSize = 12.sp)
                }
            }
        }
        if (g.garage.isEmpty()) {
            item { Text("Garagem vazia. Vai no Ferro Velho comprar um carro! 🚗") }
        }
        items(g.garage, key = { it.id }) { car ->
            val m = CATALOG[car.model]
            val invested = car.paid + car.spent
            val value = g.marketValue(car)
            Card(Modifier.fillMaxWidth().clickable { onOpen(car.id) }) {
                Column(Modifier.padding(12.dp)) {
                    Text("${m.fullName} ${car.year}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    CarArt(m.body, car.color, car.tuning, car.cond, Modifier.fillMaxWidth().height(110.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Investido: ${brl(invested)}", fontSize = 12.sp)
                            Text(
                                "Vale hoje: ${brl(value)}",
                                fontSize = 12.sp,
                                color = if (value >= invested) GREEN else RED,
                            )
                        }
                        FilledTonalButton(onClick = { onOpen(car.id) }) { Text("Oficina / Vender") }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ oficina + venda

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
}

@Composable
fun CarScreen(g: GameState, car: OwnedCar, onCity: () -> Unit, onBack: () -> Unit) {
    val m = CATALOG[car.model]
    val invested = car.paid + car.spent
    var haggle by remember { mutableStateOf<Haggle?>(null) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextButton(onClick = onBack) { Text("← Voltar à garagem") }
        Text("${m.fullName} ${car.year}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        CarArt(m.body, car.color, car.tuning, car.cond, Modifier.fillMaxWidth().height(170.dp))
        Text(
            "Pago: ${brl(car.paid)} • Oficina: ${brl(car.spent)} • Total: ${brl(invested)}\n" +
                "Vale hoje (comprador comum): ${brl(g.marketValue(car))}",
            fontSize = 13.sp,
        )

        SectionTitle("🔧 Oficina")
        Text(
            "Conserto, tuning e pintura agora são feitos na oficina, lá na cidade.",
            fontSize = 13.sp,
        )
        Button(onClick = onCity, modifier = Modifier.fillMaxWidth()) { Text("🏙️ Ir até a oficina") }

        SectionTitle("💰 Vender")
        Button(
            onClick = {
                if (g.minutes >= 22 * 60) g.message = "Já é noite, ninguém aparece. Passe pro próximo dia 🌙"
                else { g.spendTime(30); haggle = newHaggle(g, car) }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("👥 Atender um comprador") }
        haggle?.let { h ->
            BuyerDialog(h, invested, { p -> g.sell(car.id, Offer(h.type, "", p)); haggle = null }, { haggle = null })
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ------------------------------------------------------------------ resumo

@Composable
fun SummaryScreen(g: GameState, onReset: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Resumo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(g.dateText())
        Text("Dia: ${g.day}")
        SectionTitle("Mercado (últimos 30 dias)")
        MarketChart(g.history)
        Cat.values().forEach { c ->
            Text("${c.label}: ${(g.marketFor(c) * 100).roundToInt()}%", fontSize = 13.sp)
        }
        Text("Dinheiro: ${brl(g.money)}")
        Text("Carros vendidos: ${g.sold}")
        Text("Lucro total: ${brl(g.totalProfit)}", color = if (g.totalProfit >= 0) GREEN else RED)
        Text("Modelos no catálogo: ${CATALOG.size}")

        SectionTitle("Como jogar")
        Text(
            "1. Ande pela cidade e fale com os vendedores dos ferros-velhos pra comprar carros baratos.\n" +
                "2. Conserte, tune e pinte na oficina da cidade. Cada peça pesa diferente no valor.\n" +
                "3. Venda pela garagem (botão 🚗 Garagem) ou monte sua própria revendedora.\n" +
                "4. Compre os terrenos à venda no mapa e abra empresas: revendedora de carros e imobiliária.\n" +
                "5. Durma em casa pra passar o dia: o mercado e os ferros-velhos renovam.",
            fontSize = 14.sp,
        )

        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onReset) { Text("Reiniciar jogo") }
    }
}

@Composable
private fun MarketChart(h: List<Double>) {
    val line = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(90.dp)) {
        if (h.size >= 2) {
            val lo = minOf(h.minOrNull() ?: 1.0, 0.9)
            val hi = maxOf(h.maxOrNull() ?: 1.0, 1.1)
            val path = Path()
            h.forEachIndexed { i, v ->
                val x = size.width * i / (h.size - 1)
                val y = size.height * (1f - ((v - lo) / (hi - lo)).toFloat())
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, line, style = Stroke(4f))
        }
    }
}


// ------------------------------------------------------------------ cards bonitos (reparo / tuning)

val PART_ICONS = listOf("⚙️", "🚪", "🛞", "💺", "🔩")

@Composable
fun RepairCard(g: GameState, car: OwnedCar, i: Int, name: String) {
    val v = car.cond[i]
    val (label, col) = when {
        v < 25 -> "Péssimo" to RED
        v < 50 -> "Ruim" to Color(0xFFFF9800)
        v < 75 -> "Regular" to Color(0xFFFFCA28)
        v < 95 -> "Bom" to Color(0xFF9CCC65)
        else -> "Ótimo" to GREEN
    }
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    androidx.compose.material3.Card(
        shape = shape,
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF2A2733)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(col.copy(alpha = 0.18f)).border(1.5.dp, col, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text(PART_ICONS.getOrElse(i) { "🔧" }, fontSize = 18.sp) }
                Column(Modifier.weight(1f)) {
                    Text(name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(label, color = col, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Text("$v%", color = col, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Box(Modifier.fillMaxWidth().height(10.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(5.dp)).background(Color(0xFF14121A))) {
                Box(
                    Modifier.fillMaxWidth(v.coerceIn(0, 100) / 100f).fillMaxHeight()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(5.dp))
                        .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(col.copy(alpha = 0.7f), col))),
                )
            }
            if (v < MAX_COND) {
                val up = min(MAX_COND, v + 10)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { g.repair(car.id, i, up) }, Modifier.weight(1f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    ) { Text("+${up - v}  ${brl(g.repairCost(car, i, up))}", fontSize = 12.sp) }
                    Button(
                        onClick = { g.repair(car.id, i, MAX_COND) }, Modifier.weight(1f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    ) { Text("100%  ${brl(g.repairCost(car, i, MAX_COND))}", fontSize = 12.sp) }
                }
            } else {
                Text("✓ Perfeito", color = GREEN, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun TuningCard(g: GameState, car: OwnedCar, t: Tuning, modifier: Modifier) {
    val has = t.id in car.tuning
    val block = tuneBlock(CATALOG[car.model], t)
    val accent = if (has) GREEN else Color(0xFFB39DDB)
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    val emoji = t.name.takeWhile { !it.isLetter() }.trim()
    val title = t.name.dropWhile { !it.isLetter() }
    androidx.compose.material3.Card(
        shape = shape,
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF2A2733)),
        modifier = modifier.border(1.5.dp, if (has) GREEN else Color(0x33FFFFFF), shape),
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(emoji, fontSize = 26.sp)
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center, minLines = 2, maxLines = 2)
            Text("+${(t.valuePct * 100).roundToInt()}% valor", color = accent, fontSize = 11.sp)
            if (has) {
                Text("✓ Instalado", color = GREEN, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            } else if (block != null) {
                Text("🚫 $block", color = Color(0xFFEF9A9A), fontSize = 11.sp, textAlign = TextAlign.Center)
            } else {
                FilledTonalButton(
                    onClick = { g.install(car.id, t) },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                ) { Text(brl(g.tuningPrice(car, t)), fontSize = 12.sp) }
            }
        }
    }
}
