package com.ferrovelho.game

import android.graphics.Paint
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sin
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

// =====================================================================================
//  EMPRESAS NO MAPA: terrenos à venda na cidade. 0 = revendedora de carros, 1 = imobiliária
// =====================================================================================

val COMPANY_X = listOf(2780f, 3950f)
val COMPANY_NAMES = listOf("Ferrovelho Revenda", "Imobiliária Casa Nova")
val COMPANY_ICONS = listOf("🚘", "🏠")
val COMPANY_COST = listOf(80_000, 150_000)
val COMPANY_DESC = listOf(
    "Revendedora de carros: traga os carros da sua garagem, escolha o preço de cada um e negocie com os clientes que aparecem de 8h às 20h.",
    "Imobiliária: compre casas baratas do corretor, reforme, anuncie e negocie com os clientes que aparecem de 8h às 20h.",
)

data class House(
    val id: Int,
    val name: String,
    val hood: String,
    val value: Int,      // valor-base de uma casa em perfeito estado
    val cond: Int,       // condição 0..100
    val paid: Int,       // quanto você pagou (na lista do corretor = preço pedido)
    val spent: Int = 0,  // reformas
)

private val HOUSE_KINDS = listOf("Casa térrea", "Sobrado", "Apartamento", "Casa de vila", "Chácara", "Cobertura")
private val HOUSE_HOODS = listOf("Centro", "Jardim das Flores", "Vila Nova", "Bairro Alto", "Praia Azul")
private val HOUSE_BASE = listOf(180_000, 320_000, 240_000, 150_000, 420_000, 650_000)
private val CUST_NAMES = listOf(
    "Seu Marcos", "Dona Lúcia", "Rafael", "Camila", "Seu Antônio", "Juliana",
    "Thiago", "Dona Neide", "Bruno", "Patrícia", "Dr. Henrique", "Marta",
)

fun houseWorth(g: GameState, h: House): Int =
    roundTo50(h.value * (0.55 + 0.45 * h.cond / 100.0) * g.houseIdx)

val COMPANY_UPKEEP = listOf(600, 1200)   // custo fixo por dia (aluguel, funcionários, luz)

fun GameState.dailyBills(): Int = companies.sumOf { COMPANY_UPKEEP[it] }

/** Mercado imobiliário: sobe e desce sozinho, com crises e booms de vez em quando. */
fun GameState.stepHouses(): String {
    val r = Random.Default
    houseTrend = houseTrend * 0.85 + (r.nextDouble() - 0.5) * 0.012
    var news = ""
    if (r.nextInt(100) < 4) {
        houseIdx *= 0.85
        houseTrend = -0.004
        news = "Crise no setor imobiliário: os preços das casas despencaram."
    } else if (r.nextInt(100) < 4) {
        houseIdx *= 1.12
        houseTrend = 0.004
        news = "Boom imobiliário: as casas valorizaram."
    }
    houseIdx = (houseIdx * (1.0 + houseTrend)).coerceIn(0.55, 1.6)
    return news
}

fun renovateCost(h: House): Int = roundTo50(h.value * 0.04)

fun GameState.refreshHouses() {
    val r = Random(System.nanoTime())
    val list = mutableListOf<House>()
    repeat(5) {
        val k = r.nextInt(HOUSE_KINDS.size)
        val value = roundTo50(HOUSE_BASE[k] * r.nextDouble(0.8, 1.3))
        val id = houseSeq
        houseSeq += 1
        val h = House(id, HOUSE_KINDS[k], HOUSE_HOODS[r.nextInt(HOUSE_HOODS.size)], value, r.nextInt(15, 70), 0)
        val ask = roundTo50(houseWorth(this, h) * r.nextDouble(0.62, 0.82))
        list.add(h.copy(paid = ask))
    }
    houseMarket = list
}

fun GameState.buildCompany(idx: Int, nome: String = "") {
    if (idx in companies) return
    if (money < COMPANY_COST[idx]) { message = "Faltam ${brl(COMPANY_COST[idx] - money)} pra construir."; return }
    money -= COMPANY_COST[idx]
    companies = companies + idx
    if (nome.trim().isNotEmpty()) nomes = nomes + ("e$idx" to nome.trim().take(20))
    message = "${empName(idx)} inaugurada! Passa lá pra tocar o negócio."
    save()
}

fun GameState.buyHouse(h: House) {
    if (h.paid > money) { message = "Sem dinheiro pra essa casa (${brl(h.paid)})."; return }
    money -= h.paid
    houseMarket = houseMarket.filter { it.id != h.id }
    houses = houses + h
    message = "Comprou ${h.name} (${h.hood}) por ${brl(h.paid)}."
    save()
}

fun GameState.renovate(h: House) {
    if (h.cond >= 100) return
    val c = renovateCost(h)
    if (c > money) { message = "Sem dinheiro pra reforma (${brl(c)})."; return }
    money -= c
    spendTime(120)
    houses = houses.map { if (it.id == h.id) it.copy(cond = min(100, it.cond + 15), spent = it.spent + c) else it }
    message = "Reforma feita em ${h.name}."
    save()
}

fun GameState.sellHouse(h: House, price: Int) {
    val profit = price - h.paid - h.spent
    money += price
    hSold += 1
    totalProfit += profit
    houses = houses.filter { it.id != h.id }
    spendTime(60)
    message = if (profit >= 0) "Vendeu ${h.name} por ${brl(price)} — lucro de ${brl(profit)}"
    else "Vendeu ${h.name} por ${brl(price)} — prejuízo de ${brl(-profit)} 😬"
    save()
}

fun GameState.setAsk(key: String, price: Int) {
    stock = stock + (key to max(50, price))
    save()
}

fun GameState.unstock(key: String) {
    stock = stock - key
    save()
}

class Customer(val key: String, val who: String, val what: String, startOffer: Int, val limit: Int, val ask: Int) {
    var offer by mutableIntStateOf(startOffer)
    var msg by mutableStateOf("")
    var myLine by mutableStateOf("")
    var round by mutableIntStateOf(0)
}

/** Clientes do dia: sempre os mesmos pro mesmo dia e preço (reabrir a loja não rerrola). */
fun genCustomers(g: GameState): List<Customer> {
    val out = mutableListOf<Customer>()
    for ((key, ask) in g.stock) {
        val info: Pair<Int, String>
        if (key.startsWith("c")) {
            val car = g.garage.firstOrNull { "c${it.id}" == key } ?: continue
            info = Pair(g.marketValue(car), "${CATALOG[car.model].fullName} ${car.year}")
        } else {
            val h = g.houses.firstOrNull { "h${it.id}" == key } ?: continue
            info = Pair(houseWorth(g, h), "${h.name} • ${h.hood}")
        }
        val worth = max(1, info.first)
        val ratio = ask.toDouble() / worth
        val chance = (0.85 - (ratio - 0.9) * 1.6).coerceIn(0.05, 0.9)
        val r = Random(g.day * 7919L + key.hashCode())
        for (k in 0..1) {
            val roll = r.nextDouble()
            val valuation = worth * r.nextDouble(0.92, 1.18)
            val start = r.nextDouble(0.82, 0.93)
            val name = CUST_NAMES[r.nextInt(CUST_NAMES.size)]
            if (roll > chance) continue
            val limit = roundTo50(min(ask.toDouble(), valuation))
            val first = min(limit, roundTo50(min(ask.toDouble(), valuation * start)))
            out.add(Customer(key, name, info.second, first, limit, ask))
        }
    }
    return out
}

fun closeDeal(g: GameState, c: Customer, price: Int) {
    val id = c.key.substring(1).toIntOrNull() ?: return
    if (c.key.startsWith("c")) {
        g.sell(id, Offer(c.who, "", price))
    } else {
        val h = g.houses.firstOrNull { it.id == id } ?: return
        g.sellHouse(h, price)
    }
    g.stock = g.stock - c.key
    g.save()
}

// ------------------------------------------------------------ desenho no mapa

fun DrawScope.drawCompanies(g: GameState, cam: Float, s: Float, t: Float, p: Paint) {
    COMPANY_X.forEachIndexed { i, cx ->
        if (cx + 80f < cam || cx - 80f > cam + VIEW_W) return@forEachIndexed
        val x0 = cx - 66f
        wr(Color(0x33000000), x0, 101f, 132f, 3f, cam, s)
        if (i !in g.companies) {
            wr(Color(0x556D8B4F), x0, 96f, 132f, 8f, cam, s)
            wr(Color(0xFF8D6E63), x0, 88f, 132f, 3f, cam, s)
            wr(Color(0xFF8D6E63), x0, 96f, 132f, 3f, cam, s)
            for (k in 0..10) wr(Color(0xFF8D6E63), x0 + k * 13f, 84f, 3f, 20f, cam, s)
            wr(Color(0xFF5D4037), cx - 2f, 74f, 4f, 14f, cam, s)
            wr(Color(0xFFFFC107), cx - 36f, 50f, 72f, 24f, cam, s)
            label(p, "TERRENO", cx, 60f, cam, s, 6f, 0xFF212121.toInt())
            label(p, "À VENDA", cx, 69f, cam, s, 6f, 0xFFB71C1C.toInt())
        } else {
            val wall = if (i == 0) Color(0xFF1565C0) else Color(0xFF6A1B9A)
            wr(wall, x0, 58f, 132f, 46f, cam, s)
            wr(Color(0x22000000), x0, 98f, 132f, 6f, cam, s)
            wr(Color(0xFFECEFF1), x0 - 2f, 54f, 136f, 5f, cam, s)
            wr(Color(0xFF212121), cx - 58f, 36f, 116f, 16f, cam, s)
            val en = g.empName(i).uppercase()
            label(p, en, cx, 47.5f, cam, s, if (en.length > 14) 4.2f else 5.6f, 0xFFFFFFFF.toInt())
            // vitrine
            wr(Color(0xFF263238), x0 + 8f, 68f, 70f, 30f, cam, s)
            wr(Color(0xFF90CAF9), x0 + 9.5f, 69.5f, 67f, 27f, cam, s)
            if (i == 0) {
                val cols = listOf(Color(0xFFE53935), Color(0xFFFDD835), Color(0xFF43A047))
                for (k in 0..2) {
                    val bx = x0 + 13f + k * 21f
                    wr(cols[k], bx, 86f, 17f, 6f, cam, s)
                    wr(cols[k], bx + 4f, 81f, 9f, 5f, cam, s)
                    drawCircle(Color(0xFF111111), 2.2f * s, Offset((bx + 3.5f - cam) * s, 92.5f * s))
                    drawCircle(Color(0xFF111111), 2.2f * s, Offset((bx + 13.5f - cam) * s, 92.5f * s))
                }
            } else {
                for (k in 0..2) {
                    wr(Color(0xFFFFF59D), x0 + 14f + k * 21f, 74f, 15f, 18f, cam, s)
                    wr(Color(0xFFD32F2F), x0 + 14f + k * 21f, 74f, 15f, 4f, cam, s)
                }
            }
            // porta
            wr(Color(0xFF3E2723), cx + 28f, 70f, 22f, 34f, cam, s)
            wr(Color(0xFFFFC107), cx + 46f, 88f, 2f, 2f, cam, s)
            // placa aberto/fechado
            val open = g.minutes in (8 * 60) until (20 * 60)
            wr(if (open) Color(0xFF2E7D32) else Color(0xFFC62828), cx + 30f, 60f, 20f, 8f, cam, s)
            label(p, if (open) "ABERTO" else "FECHADO", cx + 40f, 66f, cam, s, 4.2f, 0xFFFFFFFF.toInt())
        }
    }
}

fun DrawScope.drawCompaniesLit(g: GameState, cam: Float, s: Float, night: Float, p: Paint) {
    COMPANY_X.forEachIndexed { i, cx ->
        if (cx + 80f < cam || cx - 80f > cam + VIEW_W) return@forEachIndexed
        if (i in g.companies) {
            glow(Offset((cx - cam) * s, 44f * s), 55f * s, Color(0xFFFFFFFF).copy(alpha = 0.22f * night))
            wr(Color(0xFFFFF3C4).copy(alpha = 0.3f * night), cx - 58f, 68f, 70f, 30f, cam, s)
        }
    }
}

// ------------------------------------------------------------ telas (janelas)

@Composable
fun GarageDialog(g: GameState, onClose: () -> Unit) {
    var openCarId by remember { mutableStateOf<Int?>(null) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Minha garagem • ${brl(g.money)}", Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onClose) { Text("Sair") }
                }
                if (g.message.isNotEmpty()) {
                    Text(g.message, Modifier.fillMaxWidth().padding(horizontal = 12.dp).clickable { g.message = "" }, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
                Box(Modifier.weight(1f)) {
                    val car = g.garage.firstOrNull { it.id == openCarId }
                    if (car != null) CarScreen(g, car, onCity = onClose) { openCarId = null }
                    else GarageScreen(g) { openCarId = it }
                }
            }
        }
    }
}

@Composable
fun SummaryDialog(g: GameState, onClose: () -> Unit) {
    var askReset by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(end = 4.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onClose) { Text("Sair") }
                }
                Box(Modifier.weight(1f)) { SummaryScreen(g) { askReset = true } }
            }
        }
    }
    if (askReset) {
        AlertDialog(
            onDismissRequest = { askReset = false },
            title = { Text("Reiniciar o jogo?") },
            text = { Text("Você perde o dinheiro, os carros, as casas, as empresas e o progresso atual.") },
            confirmButton = { TextButton(onClick = { askReset = false; g.newGame(); onClose() }) { Text("Reiniciar") } },
            dismissButton = { TextButton(onClick = { askReset = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
fun CompanyDialog(g: GameState, idx: Int, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${COMPANY_ICONS[idx]} ${g.empName(idx)}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("${g.dateText()}  •  ${brl(g.money)}" + if (idx in g.companies) "  •  custo ${brl(COMPANY_UPKEEP[idx])}/dia" else "", fontSize = 11.sp)
                    }
                    TextButton(onClick = onClose) { Text("Sair") }
                }
                if (g.message.isNotEmpty()) {
                    Text(g.message, Modifier.fillMaxWidth().clickable { g.message = "" }, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
                if (idx in g.companies) RenameButton(g, "e$idx", g.empName(idx))
                if (idx !in g.companies) BuyLot(g, idx)
                else if (idx == 0) DealerBody(g)
                else RealtyBody(g)
            }
        }
    }
}

@Composable
private fun BuyLot(g: GameState, idx: Int) {
    var nome by remember { mutableStateOf("") }
    Text("Terreno à venda", fontSize = 16.sp, fontWeight = FontWeight.Bold)
    Text(COMPANY_DESC[idx], fontSize = 13.sp)
    Text("Custo fixo: ${brl(COMPANY_UPKEEP[idx])} por dia (aluguel e funcionários), vendendo ou não.", fontSize = 13.sp)
    OutlinedTextField(value = nome, onValueChange = { nome = it.take(20) }, singleLine = true, label = { Text("Nome da empresa (opcional)") }, modifier = Modifier.fillMaxWidth())
    Button(
        onClick = { g.buildCompany(idx, nome) },
        enabled = g.money >= COMPANY_COST[idx],
        modifier = Modifier.fillMaxWidth().height(50.dp),
    ) { Text("Construir · ${brl(COMPANY_COST[idx])}", fontWeight = FontWeight.Bold) }
    if (g.money < COMPANY_COST[idx]) {
        Text("Faltam ${brl(COMPANY_COST[idx] - g.money)}.", fontSize = 12.sp, color = Color(0xFFEF5350))
    }
}

@Composable
internal fun PriceRow(g: GameState, key: String, suggested: Int) {
    val ask = g.stock[key]
    if (ask == null) {
        Button(onClick = { g.setAsk(key, suggested) }, modifier = Modifier.fillMaxWidth()) {
            Text("Pôr à venda por ${brl(suggested)}", fontSize = 13.sp)
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = { g.setAsk(key, roundTo50(ask * 0.95)) }, contentPadding = PaddingValues(8.dp)) { Text("−5%") }
            Text(brl(ask), Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = { g.setAsk(key, roundTo50(ask * 1.05)) }, contentPadding = PaddingValues(8.dp)) { Text("+5%") }
            TextButton(onClick = { g.unstock(key) }) { Text("Tirar") }
        }
    }
}

@Composable
private fun ColumnScope.DealerBody(g: GameState) {
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Estoque da loja", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        if (g.garage.isEmpty()) {
            Text("Sua garagem está vazia. Compre carros nos ferros-velhos da cidade e traga pra cá.", fontSize = 13.sp)
        }
        g.garage.forEach { car ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${CATALOG[car.model].fullName} ${car.year}", fontWeight = FontWeight.Bold)
                    CarArt(CATALOG[car.model].body, car.color, car.tuning, car.cond, Modifier.fillMaxWidth().height(80.dp))
                    val inv = car.paid + car.spent
                    val worth = g.marketValue(car)
                    Text("Investido ${brl(inv)} • vale hoje ${brl(worth)}", fontSize = 12.sp)
                    Text("Condição média ${car.cond.average().roundToInt()}% • ${car.tuning.size} upgrades • lucro estimado a preço de tabela: ${brl(worth - inv)}", fontSize = 11.sp)
                    PriceRow(g, "c${car.id}", roundTo50(g.marketValue(car) * 1.12))
                }
            }
        }
        CustomerSection(g)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ColumnScope.RealtyBody(g: GameState) {
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val arrow = if (g.houseTrend > 0.001) "▲" else if (g.houseTrend < -0.001) "▼" else "▬"
        Text("Mercado imobiliário: ${(g.houseIdx * 100).roundToInt()}% $arrow", fontSize = 13.sp)
        Text("Casas à venda hoje (corretor)", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        if (g.houseMarket.isEmpty()) Text("Nenhuma casa hoje. Volte amanhã.", fontSize = 13.sp)
        g.houseMarket.forEach { h ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${h.name} • ${h.hood}", fontWeight = FontWeight.Bold)
                    Text("Condição ${h.cond}% • vale hoje ${brl(houseWorth(g, h))}", fontSize = 12.sp)
                    Button(onClick = { g.buyHouse(h) }, enabled = g.money >= h.paid, modifier = Modifier.fillMaxWidth()) {
                        Text("Comprar por ${brl(h.paid)}", fontSize = 13.sp)
                    }
                }
            }
        }
        Text("Meus imóveis", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        if (g.houses.isEmpty()) Text("Você ainda não tem imóveis.", fontSize = 13.sp)
        g.houses.forEach { h ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${h.name} • ${h.hood}", fontWeight = FontWeight.Bold)
                    Text("Pago ${brl(h.paid)} • reformas ${brl(h.spent)} • vale hoje ${brl(houseWorth(g, h))} • condição ${h.cond}%", fontSize = 12.sp)
                    OutlinedButton(onClick = { g.renovate(h) }, enabled = h.cond < 100, modifier = Modifier.fillMaxWidth()) {
                        Text("Reformar +15% · ${brl(renovateCost(h))}", fontSize = 13.sp)
                    }
                    PriceRow(g, "h${h.id}", roundTo50(houseWorth(g, h) * 1.10))
                }
            }
        }
        CustomerSection(g)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DealScene(c: Customer, me: Look, buyer: Look, home: Boolean) {
    val anim = rememberInfiniteTransition(label = "deal")
    val t by anim.animateFloat(0f, 1000f, infiniteRepeatable(tween(1_000_000, easing = LinearEasing), RepeatMode.Restart), label = "t")
    var meTalks by remember { mutableStateOf(false) }
    LaunchedEffect(c.round) { meTalks = c.round > 0; delay(1100); meTalks = false }
    val greet = "Boa tarde! Gostei do ${c.what}. Faço ${brl(c.offer)}, topa?"
    val buyerLine = if (c.msg.isNotEmpty()) c.msg else greet
    val myLine = if (c.myLine.isNotEmpty()) c.myLine else "Olha, esse é o melhor que tenho! Peço ${brl(c.ask)}."
    Box(
        Modifier.fillMaxWidth().height(170.dp).background(
            Brush.verticalGradient(listOf(Color(0xFF2B3A55), Color(0xFF55657F), Color(0xFF3A3A44))), RoundedCornerShape(12.dp),
        ),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val floor = size.height * 0.94f
            val cu = size.height * 0.5f / 26f
            drawRect(Color(0xFF2A2B33), Offset(0f, floor - cu * 1.2f), androidx.compose.ui.geometry.Size(size.width, size.height - floor + cu * 1.2f))
            val hop = { talk: Boolean -> if (talk) abs(sin(t * 9f)) * cu * 0.9f else 0f }
            val lunge = if (meTalks) sin(t * 5f) * cu * 1.2f + cu else 0f
            val lunge2 = if (!meTalks) -(sin(t * 5f) * cu * 1.2f + cu) else 0f
            val top = floor - 24.5f * cu
            drawChar(me, size.width * 0.12f + lunge, top - hop(meTalks), cu, false, 0f, t * 2f)
            drawChar(buyer, size.width * 0.88f - 12f * cu + lunge2, top - hop(!meTalks), cu, true, 0f, t * 2f + 1.7f)
        }
        Text(
            buyerLine, Modifier.align(Alignment.TopEnd).padding(8.dp).fillMaxWidth(0.62f)
                .background(Color.White.copy(alpha = if (meTalks) 0.7f else 1f), RoundedCornerShape(10.dp)).padding(7.dp),
            fontSize = 11.sp, color = Color.Black,
        )
        Text(
            myLine, Modifier.align(Alignment.CenterStart).padding(start = 8.dp, top = 28.dp).fillMaxWidth(0.56f)
                .background(Color(0xFFD1C4FF).copy(alpha = if (meTalks) 1f else 0.7f), RoundedCornerShape(10.dp)).padding(7.dp),
            fontSize = 11.sp, color = Color.Black,
        )
    }
}

@Composable
private fun CustomerSection(g: GameState) {
    Text("Clientes na loja", fontSize = 15.sp, fontWeight = FontWeight.Bold)
    if (g.minutes !in (8 * 60) until (20 * 60)) {
        Text("Fora do horário (8h às 20h). Os clientes voltam no horário de funcionamento.", fontSize = 13.sp)
        return
    }
    val custs = remember(g.day, g.stock) { mutableStateListOf<Customer>().also { it.addAll(genCustomers(g)) } }
    if (custs.isEmpty()) {
        Text("Ninguém por aqui agora. Ponha itens à venda com preço justo pra atrair clientes.", fontSize = 13.sp)
    }
    val ctx = LocalContext.current
    val myLook = remember { loadProfile(ctx.getSharedPreferences("city", android.content.Context.MODE_PRIVATE))?.look ?: randomLook(Random(1)) }
    custs.toList().forEach { c ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DealScene(c, myLook, remember(c.who, c.key) { randomLook(Random(c.who.hashCode() * 31 + c.key.hashCode())) }, !c.key.startsWith("c"))
                Text("${c.who} quer: ${c.what}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Seu preço: ${brl(c.ask)} • Oferta dele(a): ${brl(c.offer)}", fontSize = 13.sp)
                Button(
                    onClick = { closeDeal(g, c, c.offer); custs.removeAll { it.key == c.key } },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Aceitar ${brl(c.offer)}") }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    for (pct in listOf(5, 10, 15)) {
                        OutlinedButton(
                            onClick = {
                                val p = min(c.ask, roundTo50(c.offer * (1.0 + pct / 100.0)))
                                c.myLine = when (pct) {
                                    5 -> "Tá muito bem cuidado. Dá pra chegar em ${brl(p)}?"
                                    10 -> "Olha o estado dele! Por ${brl(p)} a gente fecha agora."
                                    else -> "Última cartada: ${brl(p)} e levo você pra tomar um café!"
                                }
                                c.round++
                                if (p <= c.limit) {
                                    closeDeal(g, c, p)
                                    custs.removeAll { it.key == c.key }
                                } else if (p <= c.limit * 1.08) {
                                    c.offer = max(c.offer, roundTo50(c.limit * 0.97))
                                    c.msg = "Hmm, tá puxado... chego em ${brl(c.offer)}, não passo disso."
                                } else {
                                    g.message = "${c.who} achou caro e foi embora"
                                    custs.remove(c)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(4.dp),
                        ) { Text("+$pct%", fontSize = 12.sp) }
                    }
                    TextButton(onClick = { custs.remove(c) }) { Text("Dispensar", fontSize = 12.sp) }
                }
            }
        }
    }
}

// =====================================================================================
//  LOJAS: terrenos no mapa onde você escolhe o negócio (mercado, padaria, lanchonete, café, barraquinha)
// =====================================================================================

val LOJA_X = listOf(1230f, 2225f, 3250f, 4700f)
val DRAG_X = 4200f   // pista de arrancada
val LOJA_NAMES = listOf("Mercado", "Padaria", "Lanchonete", "Café", "Barraquinha")
val LOJA_ICONS = listOf("🛒", "🥖", "🍔", "☕", "🌭")
val LOJA_COST = listOf(120_000, 60_000, 70_000, 45_000, 12_000)
val LOJA_UPKEEP = listOf(900, 450, 520, 350, 120)          // custo fixo por dia
val LOJA_SALES = listOf(8000.0, 2400.0, 2600.0, 1500.0, 700.0)   // vendas médias por dia
val LOJA_MARGIN = listOf(0.22, 0.5, 0.5, 0.6, 0.45)        // lucro bruto sobre as vendas
private val LOJA_COLORS = listOf(Color(0xFF2E7D32), Color(0xFFBF8040), Color(0xFFD84315), Color(0xFF5D4037), Color(0xFFF9A825))
private val PRICE_NAMES = listOf("Barato", "Normal", "Caro")

var garageSignText = "FERROVELHO MOTORS"

fun GameState.empName(i: Int): String = nomes["e$i"] ?: COMPANY_NAMES[i]
fun GameState.lojaName(lot: Int): String = nomes["l$lot"] ?: LOJA_NAMES[lojas[lot] ?: 0]
fun GameState.motorsName(): String = nomes["o"] ?: "Ferrovelho Motors"

fun GameState.setNome(key: String, name: String) {
    val n = name.trim().take(20)
    nomes = if (n.isEmpty()) nomes - key else nomes + (key to n)
    save()
}

@Composable
fun RenameButton(g: GameState, key: String, current: String) {
    var open by remember { mutableStateOf(false) }
    var txt by remember { mutableStateOf(current) }
    OutlinedButton(onClick = { txt = current; open = true }, modifier = Modifier.fillMaxWidth()) { Text("Renomear", fontSize = 13.sp) }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("Novo nome") },
            text = {
                Column {
                    OutlinedTextField(value = txt, onValueChange = { txt = it.take(20) }, singleLine = true)
                    Text("Deixe vazio pra voltar ao nome original.", fontSize = 11.sp)
                }
            },
            confirmButton = { TextButton(onClick = { g.setNome(key, txt); open = false }) { Text("Salvar") } },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancelar") } },
        )
    }
}

fun lojaLabel(g: GameState, lot: Int): String {
    val type = g.lojas[lot] ?: return "📍 Terreno à venda"
    return "${LOJA_ICONS[type]} ${g.lojaName(lot)}"
}

fun GameState.buildLoja(lot: Int, type: Int, nome: String = "") {
    if (lot in lojas) return
    if (money < LOJA_COST[type]) { message = "Faltam ${brl(LOJA_COST[type] - money)} pra construir."; return }
    money -= LOJA_COST[type]
    lojas = lojas + (lot to type)
    lojaPreco = lojaPreco + (lot to 1)
    if (nome.trim().isNotEmpty()) nomes = nomes + ("l$lot" to nome.trim().take(20))
    message = "${lojaName(lot)} inaugurado! O lucro (ou prejuízo) sai todo dia à meia-noite."
    save()
}

fun GameState.sellLoja(lot: Int) {
    val type = lojas[lot] ?: return
    val v = (LOJA_COST[type] * 0.6).toInt()
    money += v
    lojas = lojas - lot
    lojaPreco = lojaPreco - lot
    lojaUltimo = lojaUltimo - lot
    message = "Você vendeu ${nomes["l$lot"] ?: LOJA_NAMES[type]} por ${brl(v)}."
    nomes = nomes - "l$lot"
    save()
}

/** Roda a cada noite: vendas variam com o dia, a economia e o preço que você pratica. */
fun GameState.stepLojas(): String {
    if (lojas.isEmpty()) return ""
    val r = Random.Default
    val last = mutableMapOf<Int, Int>()
    var total = 0
    var extra = ""
    for ((lot, type) in lojas) {
        val lvl = lojaPreco[lot] ?: 1
        var sales = LOJA_SALES[type] * marketIndex().coerceIn(0.7, 1.3) * r.nextDouble(0.6, 1.35)
        sales *= when (lvl) { 0 -> 1.25; 2 -> 0.75; else -> 1.0 }
        if (r.nextInt(100) < 10) sales *= 0.7   // dia de chuva
        val margin = LOJA_MARGIN[type] * when (lvl) { 0 -> 0.75; 2 -> 1.25; else -> 1.0 }
        var net = (sales * margin).toInt() - LOJA_UPKEEP[type]
        if (r.nextInt(100) < 3) { net -= 3000; extra = " 🚨 Fiscalização multou uma loja em R$ 3.000." }
        last[lot] = net
        total += net
    }
    lojaUltimo = last
    money += total
    totalProfit += total
    return "🏪 Lojas: ${if (total >= 0) "lucro" else "prejuízo"} de ${brl(kotlin.math.abs(total))}.$extra"
}

fun DrawScope.drawLojas(g: GameState, cam: Float, s: Float, t: Float, p: Paint) {
    LOJA_X.forEachIndexed { i, cx ->
        if (cx + 80f < cam || cx - 80f > cam + VIEW_W) return@forEachIndexed
        val x0 = cx - 66f
        wr(Color(0x33000000), x0, 101f, 132f, 3f, cam, s)
        val type = g.lojas[i]
        if (type == null) {
            wr(Color(0x556D8B4F), x0, 96f, 132f, 8f, cam, s)
            wr(Color(0xFF8D6E63), x0, 88f, 132f, 3f, cam, s)
            wr(Color(0xFF8D6E63), x0, 96f, 132f, 3f, cam, s)
            for (k in 0..10) wr(Color(0xFF8D6E63), x0 + k * 13f, 84f, 3f, 20f, cam, s)
            wr(Color(0xFF5D4037), cx - 2f, 74f, 4f, 14f, cam, s)
            wr(Color(0xFFFFC107), cx - 36f, 50f, 72f, 24f, cam, s)
            label(p, "TERRENO", cx, 60f, cam, s, 6f, 0xFF212121.toInt())
            label(p, "À VENDA", cx, 69f, cam, s, 6f, 0xFFB71C1C.toInt())
        } else if (type == 4) {
            // barraquinha: balcão com toldo listrado
            wr(Color(0xFF6D4C41), cx - 34f, 78f, 68f, 26f, cam, s)
            wr(Color(0xFFFFE0B2), cx - 30f, 82f, 60f, 8f, cam, s)
            for (k in 0..5) wr(if (k % 2 == 0) Color(0xFFE53935) else Color(0xFFFFFFFF), cx - 38f + k * 13f, 62f, 13f, 12f, cam, s)
            wr(Color(0xFF4E342E), cx - 36f, 62f, 3f, 42f, cam, s)
            wr(Color(0xFF4E342E), cx + 33f, 62f, 3f, 42f, cam, s)
            wr(Color(0xFF212121), cx - 30f, 46f, 60f, 13f, cam, s)
            val bn = g.lojaName(i).uppercase()
            label(p, bn, cx, 55.5f, cam, s, if (bn.length > 12) 3.8f else 5f, 0xFFFFFFFF.toInt())
        } else {
            wr(LOJA_COLORS[type], x0, 58f, 132f, 46f, cam, s)
            wr(Color(0x22000000), x0, 98f, 132f, 6f, cam, s)
            wr(Color(0xFFECEFF1), x0 - 2f, 54f, 136f, 5f, cam, s)
            wr(Color(0xFF212121), cx - 50f, 36f, 100f, 16f, cam, s)
            val ln = g.lojaName(i).uppercase()
            label(p, ln, cx, 47.5f, cam, s, if (ln.length > 12) 4.4f else 6f, 0xFFFFFFFF.toInt())
            wr(Color(0xFF263238), x0 + 8f, 68f, 70f, 30f, cam, s)
            wr(Color(0xFFFFF3C4), x0 + 9.5f, 69.5f, 67f, 27f, cam, s)
            for (k in 0..3) wr(Color(0xFF8D6E63), x0 + 14f + k * 16f, 88f, 11f, 8f, cam, s)
            wr(Color(0xFF3E2723), cx + 28f, 70f, 22f, 34f, cam, s)
            for (k in 0..6) wr(if (k % 2 == 0) Color(0xFFE53935) else Color(0xFFFFFFFF), x0 + 2f + k * 18f, 61f, 18f, 5f, cam, s)
        }
    }
}

fun DrawScope.drawLojasLit(g: GameState, cam: Float, s: Float, night: Float, p: Paint) {
    LOJA_X.forEachIndexed { i, cx ->
        if (cx + 80f < cam || cx - 80f > cam + VIEW_W) return@forEachIndexed
        if (i in g.lojas) {
            glow(Offset((cx - cam) * s, 52f * s), 50f * s, Color(0xFFFFE082).copy(alpha = 0.25f * night))
        }
    }
}

/** Pista de arrancada: arco com bandeira quadriculada e semáforo. */
fun DrawScope.drawDragStrip(cam: Float, s: Float, t: Float, p: Paint) {
    val cx = DRAG_X
    if (cx + 90f < cam || cx - 90f > cam + VIEW_W) return
    wr(Color(0x33000000), cx - 76f, 101f, 152f, 3f, cam, s)
    wr(Color(0xFF455A64), cx - 70f, 36f, 8f, 68f, cam, s)
    wr(Color(0xFF455A64), cx + 62f, 36f, 8f, 68f, cam, s)
    wr(Color(0xFF212121), cx - 70f, 26f, 140f, 22f, cam, s)
    label(p, "PISTA DE ARRANCADA", cx, 40f, cam, s, 6.2f, 0xFFFFEB3B.toInt())
    for (k in 0..17) {
        wr(if (k % 2 == 0) Color(0xFFFFFFFF) else Color(0xFF212121), cx - 70f + k * 7.8f, 48f, 7.8f, 5f, cam, s)
        wr(if (k % 2 == 1) Color(0xFFFFFFFF) else Color(0xFF212121), cx - 70f + k * 7.8f, 53f, 7.8f, 5f, cam, s)
    }
    val on = (t * 1.5f).toInt() % 3
    wr(Color(0xFF212121), cx - 14f, 60f, 28f, 10f, cam, s)
    wr(if (on == 0) Color(0xFFE53935) else Color(0xFF5D1F1F), cx - 11f, 62f, 6f, 6f, cam, s)
    wr(if (on == 1) Color(0xFFFFEB3B) else Color(0xFF5D5A1F), cx - 3f, 62f, 6f, 6f, cam, s)
    wr(if (on == 2) Color(0xFF43A047) else Color(0xFF1F5D2A), cx + 5f, 62f, 6f, 6f, cam, s)
    // faixas da pista no chão
    for (k in 0..6) wr(Color(0xFFFFFFFF), cx - 62f + k * 20f, 112f, 10f, 2f, cam, s)
}

@Composable
fun LojaDialog(g: GameState, lot: Int, onClose: () -> Unit) {
    var nome by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(lojaLabel(g, lot), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("${g.dateText()}  •  ${brl(g.money)}", fontSize = 11.sp)
                    }
                    TextButton(onClick = onClose) { Text("Sair") }
                }
                if (g.message.isNotEmpty()) {
                    Text(g.message, Modifier.fillMaxWidth().clickable { g.message = "" }, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
                val type = g.lojas[lot]
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (type == null) {
                        Text("Terreno à venda. Escolha o negócio:", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = nome, onValueChange = { nome = it.take(20) }, singleLine = true, label = { Text("Nome da loja (opcional)") }, modifier = Modifier.fillMaxWidth())
                        for (ty in LOJA_NAMES.indices) {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("${LOJA_ICONS[ty]} ${LOJA_NAMES[ty]}", fontWeight = FontWeight.Bold)
                                    Text("Custo fixo ${brl(LOJA_UPKEEP[ty])}/dia • vendas médias ${brl(LOJA_SALES[ty].toInt())}/dia", fontSize = 12.sp)
                                    Button(onClick = { g.buildLoja(lot, ty, nome) }, enabled = g.money >= LOJA_COST[ty], modifier = Modifier.fillMaxWidth()) {
                                        Text("Construir · ${brl(LOJA_COST[ty])}", fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        RenameButton(g, "l$lot", g.lojaName(lot))
                        val lvl = g.lojaPreco[lot] ?: 1
                        Text("Custo fixo: ${brl(LOJA_UPKEEP[type])} por dia. O resultado do dia sai à meia-noite.", fontSize = 13.sp)
                        val last = g.lojaUltimo[lot]
                        if (last != null) {
                            Text(
                                if (last >= 0) "📈 Ontem: lucro de ${brl(last)}" else "📉 Ontem: prejuízo de ${brl(-last)}",
                                fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            )
                        }
                        Text("Preço praticado:", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (i in 0..2) {
                                val label = PRICE_NAMES[i]
                                if (i == lvl) {
                                    Button(onClick = {}, modifier = Modifier.weight(1f), contentPadding = PaddingValues(8.dp)) { Text(label, fontSize = 13.sp) }
                                } else {
                                    OutlinedButton(
                                        onClick = { g.lojaPreco = g.lojaPreco + (lot to i); g.save() },
                                        modifier = Modifier.weight(1f), contentPadding = PaddingValues(8.dp),
                                    ) { Text(label, fontSize = 13.sp) }
                                }
                            }
                        }
                        Text("Barato atrai mais gente mas lucra menos por venda. Caro lucra mais por venda mas vende menos. A economia e a chuva também mexem nas vendas.", fontSize = 12.sp)
                        OutlinedButton(
                            onClick = {
                                g.hunger = min(100.0, g.hunger + 45.0)
                                g.thirst = min(100.0, g.thirst + 35.0)
                                g.spendTime(15)
                                g.message = "Você comeu e bebeu na sua loja, de graça."
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Comer e beber aqui (grátis)") }
                        OutlinedButton(onClick = { g.sellLoja(lot); onClose() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Vender a loja por ${brl((LOJA_COST[type] * 0.6).toInt())}", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
