package com.ferrovelho.game

import android.graphics.Paint
import androidx.compose.foundation.clickable
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

fun GameState.buildCompany(idx: Int) {
    if (idx in companies) return
    if (money < COMPANY_COST[idx]) { message = "Faltam ${brl(COMPANY_COST[idx] - money)} pra construir."; return }
    money -= COMPANY_COST[idx]
    companies = companies + idx
    message = "🎉 ${COMPANY_NAMES[idx]} inaugurada! Passa lá pra tocar o negócio."
    save()
}

fun GameState.buyHouse(h: House) {
    if (h.paid > money) { message = "Sem dinheiro pra essa casa (${brl(h.paid)})."; return }
    money -= h.paid
    houseMarket = houseMarket.filter { it.id != h.id }
    houses = houses + h
    message = "🏠 Comprou ${h.name} (${h.hood}) por ${brl(h.paid)}."
    save()
}

fun GameState.renovate(h: House) {
    if (h.cond >= 100) return
    val c = renovateCost(h)
    if (c > money) { message = "Sem dinheiro pra reforma (${brl(c)})."; return }
    money -= c
    spendTime(120)
    houses = houses.map { if (it.id == h.id) it.copy(cond = min(100, it.cond + 15), spent = it.spent + c) else it }
    message = "🔨 Reforma feita em ${h.name}."
    save()
}

fun GameState.sellHouse(h: House, price: Int) {
    val profit = price - h.paid - h.spent
    money += price
    hSold += 1
    totalProfit += profit
    houses = houses.filter { it.id != h.id }
    spendTime(60)
    message = if (profit >= 0) "Vendeu ${h.name} por ${brl(price)} — lucro de ${brl(profit)} 💰"
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
            label(p, COMPANY_NAMES[i].uppercase(), cx, 47.5f, cam, s, 5.6f, 0xFFFFFFFF.toInt())
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
                    Text("🚗 Minha garagem  •  ${brl(g.money)}", Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onClose) { Text("✖ Sair") }
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
                    TextButton(onClick = onClose) { Text("✖ Sair") }
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
                        Text("${COMPANY_ICONS[idx]} ${COMPANY_NAMES[idx]}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("${g.dateText()}  •  ${brl(g.money)}" + if (idx in g.companies) "  •  custo ${brl(COMPANY_UPKEEP[idx])}/dia" else "", fontSize = 11.sp)
                    }
                    TextButton(onClick = onClose) { Text("✖ Sair") }
                }
                if (g.message.isNotEmpty()) {
                    Text(g.message, Modifier.fillMaxWidth().clickable { g.message = "" }, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
                if (idx !in g.companies) BuyLot(g, idx)
                else if (idx == 0) DealerBody(g)
                else RealtyBody(g)
            }
        }
    }
}

@Composable
private fun BuyLot(g: GameState, idx: Int) {
    Text("📍 Terreno à venda", fontSize = 16.sp, fontWeight = FontWeight.Bold)
    Text(COMPANY_DESC[idx], fontSize = 13.sp)
    Text("Custo fixo: ${brl(COMPANY_UPKEEP[idx])} por dia (aluguel e funcionários), vendendo ou não.", fontSize = 13.sp)
    Button(
        onClick = { g.buildCompany(idx) },
        enabled = g.money >= COMPANY_COST[idx],
        modifier = Modifier.fillMaxWidth().height(50.dp),
    ) { Text("🏗️ Construir · ${brl(COMPANY_COST[idx])}", fontWeight = FontWeight.Bold) }
    if (g.money < COMPANY_COST[idx]) {
        Text("Faltam ${brl(COMPANY_COST[idx] - g.money)}.", fontSize = 12.sp, color = Color(0xFFEF5350))
    }
}

@Composable
private fun PriceRow(g: GameState, key: String, suggested: Int) {
    val ask = g.stock[key]
    if (ask == null) {
        Button(onClick = { g.setAsk(key, suggested) }, modifier = Modifier.fillMaxWidth()) {
            Text("🏷️ Pôr à venda por ${brl(suggested)}", fontSize = 13.sp)
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
        Text("🏷️ Estoque da loja", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        if (g.garage.isEmpty()) {
            Text("Sua garagem está vazia. Compre carros nos ferros-velhos da cidade e traga pra cá.", fontSize = 13.sp)
        }
        g.garage.forEach { car ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${CATALOG[car.model].fullName} ${car.year}", fontWeight = FontWeight.Bold)
                    Text("Investido ${brl(car.paid + car.spent)} • vale hoje ${brl(g.marketValue(car))}", fontSize = 12.sp)
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
        Text("📈 Mercado imobiliário: ${(g.houseIdx * 100).roundToInt()}% $arrow", fontSize = 13.sp)
        Text("🏘️ Casas à venda hoje (corretor)", fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
        Text("🏠 Meus imóveis", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        if (g.houses.isEmpty()) Text("Você ainda não tem imóveis.", fontSize = 13.sp)
        g.houses.forEach { h ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${h.name} • ${h.hood}", fontWeight = FontWeight.Bold)
                    Text("Pago ${brl(h.paid)} • reformas ${brl(h.spent)} • vale hoje ${brl(houseWorth(g, h))} • condição ${h.cond}%", fontSize = 12.sp)
                    OutlinedButton(onClick = { g.renovate(h) }, enabled = h.cond < 100, modifier = Modifier.fillMaxWidth()) {
                        Text("🔨 Reformar +15% · ${brl(renovateCost(h))}", fontSize = 13.sp)
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
private fun CustomerSection(g: GameState) {
    Text("👥 Clientes na loja", fontSize = 15.sp, fontWeight = FontWeight.Bold)
    if (g.minutes !in (8 * 60) until (20 * 60)) {
        Text("Fora do horário (8h às 20h). Os clientes voltam no horário de funcionamento.", fontSize = 13.sp)
        return
    }
    val custs = remember(g.day, g.stock) { mutableStateListOf<Customer>().also { it.addAll(genCustomers(g)) } }
    if (custs.isEmpty()) {
        Text("Ninguém por aqui agora. Ponha itens à venda com preço justo pra atrair clientes.", fontSize = 13.sp)
    }
    custs.toList().forEach { c ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("🙋 ${c.who} quer: ${c.what}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Seu preço: ${brl(c.ask)} • Oferta dele(a): ${brl(c.offer)}", fontSize = 13.sp)
                if (c.msg.isNotEmpty()) Text("💬 ${c.msg}", fontSize = 12.sp)
                Button(
                    onClick = { closeDeal(g, c, c.offer); custs.removeAll { it.key == c.key } },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("✅ Aceitar ${brl(c.offer)}") }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    for (pct in listOf(5, 10, 15)) {
                        OutlinedButton(
                            onClick = {
                                val p = min(c.ask, roundTo50(c.offer * (1.0 + pct / 100.0)))
                                if (p <= c.limit) {
                                    closeDeal(g, c, p)
                                    custs.removeAll { it.key == c.key }
                                } else if (p <= c.limit * 1.08) {
                                    c.offer = max(c.offer, roundTo50(c.limit * 0.97))
                                    c.msg = "Tá puxado... chego em ${brl(c.offer)}."
                                } else {
                                    g.message = "${c.who} achou caro e foi embora 😒"
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
