package com.ferrovelho.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// ------------------------------------------------------------ desenhos (sem emoji)

/** Desenha uma bolsinha genérica no espaço do Canvas. */
fun DrawScope.drawBag(body: Color, detail: Color) {
    val w = size.width
    val h = size.height
    // alça
    drawArc(
        color = body, startAngle = 180f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(w * 0.28f, h * 0.06f), size = Size(w * 0.44f, h * 0.44f),
        style = Stroke(w * 0.08f, cap = StrokeCap.Round),
    )
    // corpo
    drawRoundRect(body, Offset(w * 0.10f, h * 0.30f), Size(w * 0.80f, h * 0.64f), CornerRadius(w * 0.16f))
    // aba
    drawLine(detail, Offset(w * 0.10f, h * 0.52f), Offset(w * 0.90f, h * 0.52f), strokeWidth = w * 0.05f)
    // fivela
    drawRoundRect(detail, Offset(w * 0.42f, h * 0.47f), Size(w * 0.16f, h * 0.15f), CornerRadius(w * 0.03f))
    // bolso
    drawRoundRect(
        detail, Offset(w * 0.26f, h * 0.68f), Size(w * 0.48f, h * 0.18f), CornerRadius(w * 0.05f),
        style = Stroke(w * 0.035f),
    )
}

/** Desenha uma chave. */
fun DrawScope.drawKey(col: Color) {
    val w = size.width
    val h = size.height
    val t = w * 0.09f
    drawCircle(col, radius = w * 0.19f, center = Offset(w * 0.24f, h * 0.5f), style = Stroke(t))
    drawLine(col, Offset(w * 0.43f, h * 0.5f), Offset(w * 0.93f, h * 0.5f), strokeWidth = t, cap = StrokeCap.Round)
    drawLine(col, Offset(w * 0.74f, h * 0.5f), Offset(w * 0.74f, h * 0.74f), strokeWidth = t, cap = StrokeCap.Round)
    drawLine(col, Offset(w * 0.88f, h * 0.5f), Offset(w * 0.88f, h * 0.68f), strokeWidth = t, cap = StrokeCap.Round)
}

/** Botão redondo da bolsa (fica na tela de andar pela cidade). */
@Composable
fun InvBagButton(onClick: () -> Unit) {
    Box(
        Modifier.size(42.dp).background(Color(0xFF6D4C41), CircleShape)
            .border(2.dp, Color(0xFFD7B98E), CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(28.dp)) { drawBag(Color(0xFFC8935A), Color(0xFF4E2F14)) }
    }
}

// ------------------------------------------------------------ janela do inventário

@Composable
private fun InvHeader(text: String) {
    Text(text, Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 4.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun InvRow(title: String, sub: String, trailing: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val keyCol = MaterialTheme.colorScheme.onSurfaceVariant
        Canvas(Modifier.size(30.dp)) { drawKey(keyCol) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            if (sub.isNotEmpty()) Text(sub, fontSize = 12.sp)
        }
        if (trailing != null) trailing()
    }
}

@Composable
private fun InvPlain(title: String, sub: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 3.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)).padding(10.dp),
    ) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        if (sub.isNotEmpty()) Text(sub, fontSize = 12.sp)
    }
}

@Composable
private fun InvBar(label: String, value: Double) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("$label  ${value.toInt()}%", fontSize = 12.sp)
        LinearProgressIndicator(progress = { (value / 100.0).toFloat().coerceIn(0f, 1f) }, Modifier.fillMaxWidth().height(8.dp))
    }
}

@Composable
private fun InvEmpty(text: String) {
    Text(text, Modifier.padding(vertical = 8.dp), fontSize = 12.sp)
}

/**
 * Mochila do personagem. Tudo que o jogador tem aparece aqui, organizado em abas.
 * Só mostra o que existe de verdade: quem não comprou casa, loja ou empresa não vê essas seções cheias.
 */
@Composable
fun InventoryDialog(
    g: GameState,
    me: PlayerProfile,
    ownedProps: Set<Int>,
    lots: List<LotState?>,
    driving: Int?,
    onDrive: (Int?) -> Unit,
    onClose: () -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Chaves", "Estoque", "Pessoal")
    val lotsBuilt = lots.withIndex().filter { it.value != null }
    val keyCount = g.garage.size + 1 + ownedProps.size + lotsBuilt.size + g.companies.size + g.lojas.size + (if (g.ownShop) 1 else 0)

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.size(34.dp)) { drawBag(Color(0xFFC8935A), Color(0xFF4E2F14)) }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Mochila de ${me.name}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("$keyCount chaves • ${brl(g.money)}", fontSize = 12.sp)
                    }
                    TextButton(onClick = onClose) { Text("Fechar") }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tabs.forEachIndexed { i, n ->
                        FilterChip(selected = tab == i, onClick = { tab = i }, label = { Text(n, fontSize = 13.sp) })
                    }
                }
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp)) {
                    when (tab) {
                        0 -> {
                            InvHeader("Carros (${g.garage.size}/${g.slots})")
                            if (g.garage.isEmpty()) InvEmpty("Nenhuma chave de carro ainda. Compre um carro nos ferros-velhos.")
                            g.garage.forEach { car ->
                                val m = CATALOG[car.model]
                                val mods = if (car.tuning.isEmpty()) "original" else "${car.tuning.distinct().size} modificações"
                                InvRow(m.fullName, "${car.year} • $mods") {
                                    if (driving == car.id) {
                                        TextButton(onClick = { onDrive(null) }) { Text("Descer") }
                                    } else {
                                        TextButton(onClick = { onDrive(car.id) }, enabled = g.shift?.veh == null) { Text("Dirigir") }
                                    }
                                }
                            }
                            InvHeader("Casas e imóveis")
                            InvRow("Minha casa", "Chave da casa do personagem")
                            ownedProps.sorted().forEach { pi ->
                                val p = PROPS.getOrNull(pi) ?: return@forEach
                                InvRow(p.name, "Aluguel ${brl(p.rent)} por dia")
                            }
                            lotsBuilt.forEach { (li, ls) ->
                                if (ls == null) return@forEach
                                val pr = lotProg(ls, lotNow(g))
                                val ht = HOUSE_TYPES[ls.type]
                                InvRow(ht.name + " (terreno ${li + 1})", if (pr < 1f) "Em obras: ${(pr * 100).toInt()}%" else "Pronta • aluguel ${brl(ht.rent)} por dia")
                            }
                            if (g.ownShop || g.companies.isNotEmpty() || g.lojas.isNotEmpty()) {
                                InvHeader("Negócios")
                                if (g.ownShop) InvRow(g.motorsName(), "Oficina própria")
                                g.companies.sorted().forEach { ci -> InvRow(g.empName(ci), "Empresa") }
                                g.lojas.keys.sorted().forEach { li -> InvRow(g.lojaName(li), "Loja") }
                            }
                        }
                        1 -> {
                            InvHeader("Casas da imobiliária")
                            if (g.houses.isEmpty()) InvEmpty("Nenhuma casa em estoque. Compre casas do corretor na imobiliária.")
                            g.houses.forEach { h ->
                                InvPlain(h.name + " • " + h.hood, "Condição ${h.cond}% • pagou ${brl(h.paid)} • vale ${brl(houseWorth(g, h))}")
                            }
                            InvHeader("Carros na garagem")
                            if (g.garage.isEmpty()) InvEmpty("Garagem vazia.")
                            g.garage.forEach { car ->
                                InvPlain(CATALOG[car.model].fullName + " " + car.year, "Investido ${brl(car.paid + car.spent)} • vale hoje ${brl(g.marketValue(car))}")
                            }
                        }
                        else -> {
                            InvHeader("Carteira")
                            InvPlain(brl(g.money), "Dinheiro na mão")
                            InvHeader("Como você está")
                            InvBar("Fome (100 = satisfeito)", g.hunger)
                            InvBar("Sede (100 = hidratado)", g.thirst)
                            InvBar("Energia", g.energy)
                            InvHeader("Resumo")
                            InvPlain("Carros vendidos: ${g.sold}", "Lucro total: ${brl(g.totalProfit)}")
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}
