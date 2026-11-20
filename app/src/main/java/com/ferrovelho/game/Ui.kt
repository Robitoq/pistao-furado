package com.ferrovelho.game

import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
    var tab by remember { mutableIntStateOf(0) }
    var openCarId by remember { mutableStateOf<Int?>(null) }
    var askReset by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopStatus(g) },
        bottomBar = {
            NavigationBar {
                val tabs = listOf("🚗" to "Ferro Velho", "🔧" to "Garagem", "📊" to "Resumo")
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i; openCarId = null },
                        icon = { Text(t.first, fontSize = 20.sp) },
                        label = { Text(t.second) },
                    )
                }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (g.message.isNotEmpty()) MessageBar(g)
            when (tab) {
                0 -> YardScreen(g)
                1 -> {
                    val car = g.garage.firstOrNull { it.id == openCarId }
                    if (car != null) {
                        CarScreen(g, car) { openCarId = null }
                    } else {
                        GarageScreen(g) { openCarId = it }
                    }
                }
                else -> SummaryScreen(g) { askReset = true }
            }
        }
    }

    if (askReset) {
        AlertDialog(
            onDismissRequest = { askReset = false },
            title = { Text("Reiniciar o jogo?") },
            text = { Text("Você perde o dinheiro, os carros e o progresso atual.") },
            confirmButton = {
                TextButton(onClick = { askReset = false; openCarId = null; g.newGame() }) { Text("Reiniciar") }
            },
            dismissButton = { TextButton(onClick = { askReset = false }) { Text("Cancelar") } },
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
                Text(brl(g.money), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GREEN)
                Text(g.dateText(), fontSize = 12.sp)
                Text("Dia ${g.day} • mercado ${(g.marketIndex() * 100).roundToInt()}% ${g.trendArrow()}", fontSize = 12.sp)
                g.event?.let { Text("${it.title} — ${it.text}", fontSize = 11.sp) }
            }
            Button(onClick = { g.nextDay() }) { Text("Próximo dia ▶") }
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
private fun GarageScreen(g: GameState, onOpen: (Int) -> Unit) {
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
private fun CarScreen(g: GameState, car: OwnedCar, onBack: () -> Unit) {
    val m = CATALOG[car.model]
    val invested = car.paid + car.spent
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

        SectionTitle("🔧 Reparos")
        PART_NAMES.forEachIndexed { i, name ->
            val v = car.cond[i]
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, Modifier.width(84.dp), fontSize = 13.sp)
                    LinearProgressIndicator(
                        progress = { v / 100f },
                        modifier = Modifier.weight(1f).height(8.dp),
                        color = condColor(v),
                    )
                    Text("$v%", Modifier.width(44.dp), fontSize = 12.sp, textAlign = TextAlign.End)
                }
                if (v < MAX_COND) {
                    Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val up = min(MAX_COND, v + 10)
                        OutlinedButton(onClick = { g.repair(car.id, i, up) }) {
                            Text("+${up - v} · ${brl(g.repairCost(car, i, up))}", fontSize = 12.sp)
                        }
                        FilledTonalButton(onClick = { g.repair(car.id, i, MAX_COND) }) {
                            Text("100% · ${brl(g.repairCost(car, i, MAX_COND))}", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        if (g.repairAllCost(car) > 0) {
            Button(onClick = { g.repairAll(car.id) }, modifier = Modifier.fillMaxWidth()) {
                Text("Reparar tudo · ${brl(g.repairAllCost(car))}")
            }
        }

        SectionTitle("🎛️ Tuning")
        TUNINGS.forEach { t ->
            val has = t.id in car.tuning
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t.name, Modifier.weight(1f), fontSize = 14.sp)
                if (has) {
                    Text("instalado ✓", color = GREEN, fontSize = 13.sp)
                } else {
                    OutlinedButton(onClick = { g.install(car.id, t) }) {
                        Text(brl(g.tuningPrice(car, t)), fontSize = 12.sp)
                    }
                }
            }
        }

        SectionTitle("🎨 Pintura (${brl(g.paintCost(car))})")
        PAINTS.chunked(7).forEachIndexed { row, chunk ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                chunk.forEachIndexed { j, p ->
                    val idx = row * 7 + j
                    val selected = idx == car.color
                    Spacer(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(p.second))
                            .border(if (selected) 3.dp else 1.dp, if (selected) Color.White else Color.Gray, CircleShape)
                            .clickable { g.paint(car.id, idx) },
                    )
                }
            }
        }

        SectionTitle("💰 Vender (as ofertas mudam a cada dia)")
        g.offers(car).forEach { o ->
            val profit = o.price - invested
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(o.buyer, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(o.note, fontSize = 11.sp)
                        Text(
                            if (profit >= 0) "lucro ${brl(profit)}" else "prejuízo ${brl(-profit)}",
                            fontSize = 12.sp,
                            color = if (profit >= 0) GREEN else RED,
                        )
                    }
                    Button(onClick = { g.sell(car.id, o) }) { Text("Vender ${brl(o.price)}", fontSize = 13.sp) }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ------------------------------------------------------------------ resumo

@Composable
private fun SummaryScreen(g: GameState, onReset: () -> Unit) {
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
            "1. Compre carros baratos nos ferros-velhos (olhe quanto ele \"vale hoje\").\n" +
                "2. Na garagem, abra a oficina e conserte as peças. Cada peça pesa diferente no valor.\n" +
                "3. Tune e pinte: cada comprador valoriza uma coisa (tunador paga por tuning, " +
                "colecionador paga por clássico).\n" +
                "4. Compare as ofertas e venda. Se nenhuma agradar, passe o dia e veja novas ofertas.\n" +
                "5. Junte grana e vá pro Pátio Premium comprar esportivo.",
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
