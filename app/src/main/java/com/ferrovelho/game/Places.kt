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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.abs
import kotlin.random.Random

// =====================================================================================
//  LUGARES NOVOS DA CIDADE: oficina, padaria, mercadinho e trailer de lanche
// =====================================================================================

const val GARAGE_X = 1000f                       // oficina
val STORE_X = listOf(760f, 1700f, 3000f)         // padaria, mercadinho, trailer

fun garageOpen(g: GameState): Boolean = g.ownShop || g.minutes in (7 * 60) until (20 * 60)

class Food(
    val emoji: String, val name: String, val price: Int,
    val hunger: Double, val thirst: Double, val minutes: Int = 10,
    val reply: String = "Bom apetite!", val chat: Boolean = false,
)

class Store(
    val title: String, val sign: String, val x: Float, val open: Int, val close: Int,
    val pitch: Float, val clerk: BuyerLook, val greet: List<String>, val items: List<Food>,
)

val STORES: List<Store> by lazy {
    listOf(
    Store(
        "🥖 Padaria Pão Quente", "PADARIA PÃO QUENTE", STORE_X[0], 6 * 60, 21 * 60, 1.3f,
        BuyerLook(true, CSKIN[4], CHAIR[1], Color.White, Color(0xFF6D4C41)),
        listOf("Bom dia! Pão quentinho saindo!", "Entra, freguês! O café tá fresquinho.", "O que vai ser hoje, querido?"),
        listOf(
            Food("☕", "Café com leite + papo", 5, 6.0, 14.0, 15, "Senta aí e toma com calma. Deixa eu te contar...", chat = true),
            Food("🥖", "Pão na chapa", 7, 22.0, 0.0),
            Food("🧀", "Pão de queijo", 6, 18.0, 0.0),
            Food("🥟", "Coxinha", 8, 24.0, 0.0),
            Food("🧃", "Suco de laranja", 9, 4.0, 38.0),
            Food("💧", "Água", 4, 0.0, 40.0, 5, "Geladinha!"),
        ),
    ),
    Store(
        "🛒 Mercadinho 2 Irmãos", "MERCADINHO 2 IRMÃOS", STORE_X[1], 6 * 60, 23 * 60 + 30, 0.8f,
        BuyerLook(false, CSKIN[1], CHAIR[5], Color(0xFF43A047), Color(0xFF37474F)),
        listOf("Boa! Precisando de quê?", "Tá tudo baratinho aqui, pode olhar.", "Fala, freguês!"),
        listOf(
            Food("💧", "Água mineral", 3, 0.0, 40.0, 5, "Tá gelada!"),
            Food("🥤", "Refrigerante", 7, 4.0, 34.0, 5, "Estupidamente gelado."),
            Food("🍫", "Barra de cereal", 5, 14.0, 0.0, 5),
            Food("🍌", "Banana", 3, 10.0, 0.0, 5),
            Food("🥪", "Sanduíche natural", 12, 32.0, 4.0),
            Food("🍱", "Marmita completa", 22, 65.0, 5.0, 25, "Feijão, arroz e bife. Aproveita!"),
        ),
    ),
    Store(
        "🍔 Trailer do Bigode", "TRAILER DO BIGODE", STORE_X[2], 10 * 60, 24 * 60, 0.9f,
        BuyerLook(false, CSKIN[3], CHAIR[0], Color(0xFFE53935), Color(0xFF212121)),
        listOf("Fala, campeão! O X-Tudo tá imbatível!", "Chega mais! Tá na chapa agora.", "Quer o quê, patrão?"),
        listOf(
            Food("🍔", "X-Burguer", 18, 50.0, 0.0, 15),
            Food("🍔", "X-Tudo", 28, 72.0, 0.0, 20, "Esse aí é pra quem tem coragem!"),
            Food("🌭", "Cachorro-quente", 12, 34.0, 0.0),
            Food("🍟", "Batata frita", 14, 30.0, 0.0),
            Food("🥤", "Refri lata", 6, 3.0, 30.0, 5),
            Food("🍇", "Açaí", 16, 25.0, 20.0, 10, "Gelado e com granola."),
        ),
    ),
    )
}

// ------------------------------------------------------------ fome e sede (HUD)

@Composable
fun NeedsBar(g: GameState) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        NeedMeter("🍔", g.hunger, Modifier.weight(1f))
        NeedMeter("🥤", g.thirst, Modifier.weight(1f))
    }
}

@Composable
private fun NeedMeter(icon: String, v: Double, modifier: Modifier) {
    val col = if (v < 15) Color(0xFFEF5350) else if (v < 40) Color(0xFFFFA726) else Color(0xFF66BB6A)
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(icon, fontSize = 13.sp)
        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF14121A))) {
            Box(Modifier.fillMaxWidth((v / 100.0).toFloat().coerceIn(0f, 1f)).fillMaxHeight().background(col))
        }
        Text("${v.toInt()}%", fontSize = 10.sp, color = col)
    }
}

// ------------------------------------------------------------ desenho: oficina

private fun DrawScope.garageSign(own: Boolean, cx: Float, cam: Float, s: Float, p: Paint) {
    val trim = if (own) Color(0xFFD32F2F) else Color(0xFFFFC107)
    wr(Color(0xFF212121), cx - 50f, 34f, 100f, 16f, cam, s)
    wr(trim, cx - 48f, 36f, 96f, 12f, cam, s)
    label(p, if (own) garageSignText else "OFICINA DO TIÃO", cx, 45.5f, cam, s, if (own && garageSignText.length > 14) 4.6f else 6.2f,
        if (own) 0xFFFFFFFF.toInt() else 0xFF212121.toInt())
}

fun DrawScope.drawGarageShop(own: Boolean, open: Boolean, cam: Float, s: Float, t: Float, p: Paint) {
    val cx = GARAGE_X
    if (cx + 75f < cam || cx - 75f > cam + VIEW_W) return
    val x0 = cx - 66f
    val wall = if (own) Color(0xFF37474F) else Color(0xFF8D6E63)
    val trim = if (own) Color(0xFFD32F2F) else Color(0xFFFFC107)
    wr(Color(0x33000000), x0, 101f, 132f, 3f, cam, s)
    wr(wall, x0, 54f, 132f, 50f, cam, s)
    wr(Color(0x22000000), x0, 98f, 132f, 6f, cam, s)
    wr(trim, x0 - 2f, 50f, 136f, 5f, cam, s)
    wr(Color(0x33000000), x0 - 2f, 55f, 136f, 1.2f, cam, s)
    // portão aberto com o elevador e um carro em cima
    wr(Color(0xFF1C2327), cx - 30f, 66f, 60f, 38f, cam, s)
    wr(Color(0xFF455A64), cx - 30f, 96f, 60f, 8f, cam, s)
    wr(Color(0xFF78909C), cx - 22f, 70f, 3f, 28f, cam, s)
    wr(Color(0xFF78909C), cx + 19f, 70f, 3f, 28f, cam, s)
    val cc = if (own) Color(0xFFE53935) else Color(0xFF1E88E5)
    wr(cc, cx - 18f, 82f, 36f, 7f, cam, s)
    wr(cc, cx - 10f, 76f, 20f, 6f, cam, s)
    wr(Color(0xFFB3E5FC), cx - 8f, 77f, 7f, 4f, cam, s)
    wr(Color(0xFFB3E5FC), cx + 1f, 77f, 7f, 4f, cam, s)
    drawCircle(Color(0xFF111111), 3.5f * s, Offset((cx - 11f - cam) * s, 90f * s))
    drawCircle(Color(0xFF111111), 3.5f * s, Offset((cx + 11f - cam) * s, 90f * s))
    if (open && (t * 5f).toInt() % 4 == 0) {   // faísca de solda
        wr(Color(0xFFFFF176), cx - 6f + (t * 13f) % 8f, 92f, 1.4f, 1.4f, cam, s)
        wr(Color(0xFFFFFFFF), cx - 5f + (t * 9f) % 8f, 90f, 1f, 1f, cam, s)
    }
    // porta de enrolar semi-aberta
    for (k in 0..3) wr(if (k % 2 == 0) Color(0xFFB0BEC5) else Color(0xFF90A4AE), cx - 30f, 62f + k * 1.6f, 60f, 1.6f, cam, s)
    // janelas
    for (wx in listOf(x0 + 6f, x0 + 104f)) {
        wr(Color(0xFF263238), wx, 66f, 22f, 18f, cam, s)
        wr(Color(0xFF90CAF9), wx + 1.5f, 67.5f, 19f, 15f, cam, s)
        wr(Color(0xFF263238), wx + 10.5f, 67.5f, 1f, 15f, cam, s)
    }
    garageSign(own, cx, cam, s, p)
    // placa aberto/fechado
    wr(if (open) Color(0xFF2E7D32) else Color(0xFFC62828), cx + 34f, 90f, 20f, 8f, cam, s)
    label(p, if (open) "ABERTO" else "FECHADO", cx + 44f, 96f, cam, s, 4.2f, 0xFFFFFFFF.toInt())
    // pneus, cone e o mecânico
    for (k in 0..1) {
        drawCircle(Color(0xFF1B1B1B), 4f * s, Offset((cx + 60f - cam) * s, (111f - k * 7f) * s))
        drawCircle(Color(0xFF6B6B6B), 1.6f * s, Offset((cx + 60f - cam) * s, (111f - k * 7f) * s))
    }
    wr(Color(0xFFFF6D00), cx - 60f, 108f, 4f, 6f, cam, s)
    wr(Color.White, cx - 59.4f, 110f, 2.8f, 1f, cam, s)
    if (open) drawChar(Look(false, 3, 5, 0, 2, 1, 0, 1, 0, false), (cx - 46f - 6f - cam) * s, (114f - 26f) * s, s, false, 0f, t * 3f)
}

fun DrawScope.drawGarageLit(own: Boolean, open: Boolean, cam: Float, s: Float, night: Float, p: Paint) {
    val cx = GARAGE_X
    if (cx + 75f < cam || cx - 75f > cam + VIEW_W) return
    val trim = if (own) Color(0xFFD32F2F) else Color(0xFFFFC107)
    glow(Offset((cx - cam) * s, 43f * s), 55f * s, trim.copy(alpha = 0.28f * night))
    garageSign(own, cx, cam, s, p)
    if (open) {
        wr(Color(0xFFFFF3C4).copy(alpha = 0.3f * night), cx - 30f, 66f, 60f, 38f, cam, s)
        glow(Offset((cx - cam) * s, 90f * s), 45f * s, Color(0xFFFFE9A8).copy(alpha = 0.32f * night))
    }
}

// ------------------------------------------------------------ desenho: lojinhas

private fun DrawScope.awning(x0: Float, w: Float, y: Float, a: Color, b: Color, cam: Float, s: Float) {
    var k = 0
    while (k * 7f < w) {
        wr(if (k % 2 == 0) a else b, x0 + k * 7f, y, 7f, 7f, cam, s)
        wr(if (k % 2 == 0) a else b, x0 + k * 7f + 1f, y + 7f, 5f, 1.5f, cam, s)
        k++
    }
}

private fun DrawScope.storeSign(st: Store, bg: Color, fg: Int, y: Float, cam: Float, s: Float, p: Paint) {
    wr(Color(0xFF212121), st.x - 36f, y, 72f, 14f, cam, s)
    wr(bg, st.x - 34f, y + 2f, 68f, 10f, cam, s)
    label(p, st.sign, st.x, y + 9.2f, cam, s, 4.8f, fg)
}

fun DrawScope.drawStores(open: List<Boolean>, cam: Float, s: Float, t: Float, p: Paint) {
    STORES.forEachIndexed { i, st ->
        val cx = st.x
        if (cx + 60f < cam || cx - 60f > cam + VIEW_W) return@forEachIndexed
        val x0 = cx - 42f
        val on = open[i]
        wr(Color(0x33000000), x0, 101f, 84f, 3f, cam, s)
        when (i) {
            0 -> {   // padaria
                wr(Color(0xFFFFCC80), x0, 56f, 84f, 48f, cam, s)
                wr(Color(0xFF6D4C41), x0 - 2f, 52f, 88f, 5f, cam, s)
                wr(Color(0xFF6D4C41), x0 + 66f, 42f, 8f, 10f, cam, s)
                for (k in 0..2) {   // fumacinha da chaminé
                    val ph = (t * 0.5f + k * 0.33f) % 1f
                    drawCircle(Color.White.copy(alpha = 0.55f * (1f - ph)), (2f + ph * 3f) * s, Offset((x0 + 70f + ph * 6f - cam) * s, (42f - ph * 14f) * s))
                }
                storeSign(st, Color(0xFFFFF3E0), 0xFF4E342E.toInt(), 38f, cam, s, p)
                awning(x0, 84f, 70f, Color(0xFFD32F2F), Color.White, cam, s)
                wr(Color(0xFF5D4037), x0 + 5f, 79f, 36f, 22f, cam, s)
                wr(Color(0xFFB3E5FC), x0 + 6.5f, 80.5f, 33f, 19f, cam, s)
                wr(Color(0xFF5D4037), x0 + 6.5f, 95f, 33f, 1.6f, cam, s)
                for (k in 0..3) drawOval(Color(0xFFC68642), Offset((x0 + 9f + k * 8f - cam) * s, 90f * s), Size(7f * s, 4.5f * s))
            }
            1 -> {   // mercadinho
                wr(Color(0xFFA5D6A7), x0, 56f, 84f, 48f, cam, s)
                wr(Color(0xFF1B5E20), x0 - 2f, 52f, 88f, 5f, cam, s)
                storeSign(st, Color(0xFF1565C0), 0xFFFFFFFF.toInt(), 38f, cam, s, p)
                awning(x0, 84f, 70f, Color(0xFF2E7D32), Color.White, cam, s)
                wr(Color(0xFF37474F), x0 + 5f, 79f, 36f, 22f, cam, s)
                wr(Color(0xFFB3E5FC), x0 + 6.5f, 80.5f, 33f, 19f, cam, s)
                for (k in 0..4) wr(listOf(Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFFFDD835), Color(0xFF43A047), Color(0xFF8E24AA))[k], x0 + 8f + k * 6f, 92f, 4.5f, 6f, cam, s)
                wr(Color(0xFFD7B98E), x0 - 4f, 108f, 12f, 6f, cam, s)   // caixote de frutas na calçada
                for (k in 0..2) drawCircle(listOf(Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFF7CB342))[k], 1.6f * s, Offset((x0 - 1.5f + k * 3.6f - cam) * s, 108f * s))
            }
            else -> {   // trailer de lanche
                val tx = cx - 35f
                wr(Color(0xFFFFF176), tx, 78f, 70f, 26f, cam, s)
                wr(Color(0xFFD32F2F), tx, 93f, 70f, 3f, cam, s)
                wr(Color(0xFF263238), tx + 8f, 82f, 30f, 11f, cam, s)
                wr(if (on) Color(0xFFFFE082) else Color(0xFF546E7A), tx + 9.5f, 83.5f, 27f, 8f, cam, s)
                wr(Color(0xFF8D6E63), tx + 6f, 93f, 34f, 2f, cam, s)
                awning(tx + 4f, 38f, 76f, Color(0xFFD32F2F), Color.White, cam, s)
                wr(Color(0xFF263238), tx + 44f, 82f, 20f, 13f, cam, s)
                for (k in 0..2) wr(Color(0xCCFFFFFF), tx + 46f, 85f + k * 3f, 12f - k * 2f, 0.8f, cam, s)
                wr(Color(0xFF9E9E9E), tx - 8f, 100f, 9f, 2f, cam, s)
                for (wx in listOf(tx + 14f, tx + 54f)) {
                    drawCircle(Color(0xFF1B1B1B), 5f * s, Offset((wx - cam) * s, 104f * s))
                    drawCircle(Color(0xFF9E9E9E), 2f * s, Offset((wx - cam) * s, 104f * s))
                }
                storeSign(st, Color(0xFFFFEB3B), 0xFF212121.toInt(), 60f, cam, s, p)
                wr(Color(0xFF424242), cx - 20f, 74f, 1.5f, 4f, cam, s); wr(Color(0xFF424242), cx + 18f, 74f, 1.5f, 4f, cam, s)
                // mesinha com guarda-sol
                wr(Color(0xFF8D6E63), cx + 40f, 96f, 1f, 16f, cam, s)
                wr(Color(0xFFE53935), cx + 33f, 93f, 15f, 3f, cam, s)
                wr(Color(0xFFE53935), cx + 36f, 91f, 9f, 2f, cam, s)
                wr(Color.White, cx + 35f, 108f, 11f, 1.5f, cam, s)
            }
        }
        if (i != 2) {
            wr(Color(0xFF4E342E), x0 + 52f, 78f, 24f, 26f, cam, s)
            wr(Color(0xFFB3E5FC), x0 + 55f, 81f, 18f, 12f, cam, s)
            wr(Color(0xFFFFD54F), x0 + 57f, 96f, 1.5f, 4f, cam, s)
        }
        // lâmpada de aberto/fechado
        wr(if (on) Color(0xFF66BB6A) else Color(0xFFEF5350), x0 + 78f, 72f, 4f, 4f, cam, s)
    }
}

fun DrawScope.drawStoresLit(open: List<Boolean>, cam: Float, s: Float, night: Float, p: Paint) {
    STORES.forEachIndexed { i, st ->
        val cx = st.x
        if (cx + 60f < cam || cx - 60f > cam + VIEW_W) return@forEachIndexed
        glow(Offset((cx - cam) * s, 45f * s), 42f * s, Color(0xFFFFEB3B).copy(alpha = 0.18f * night))
        if (open[i]) {
            val wx = if (i == 2) cx - 20f else cx - 24f
            glow(Offset((wx - cam) * s, 90f * s), 36f * s, Color(0xFFFFE08A).copy(alpha = 0.38f * night))
            wr(Color(0xFFFFE9A8).copy(alpha = 0.35f * night), wx - 14f, 81f, 30f, 18f, cam, s)
        }
    }
}

// =====================================================================================
//  LOJINHAS: comprar comida e bebida (e bater papo no café)
// =====================================================================================

@Composable
fun StoreDialog(g: GameState, st: Store, onClose: () -> Unit) {
    val open = g.minutes in st.open until st.close
    var line by remember { mutableStateOf(if (open) st.greet.random() else "Tá fechado agora, volta mais tarde!") }
    LaunchedEffect(Unit) { Voice.speak(line, st.pitch) }
    Dialog(onDismissRequest = onClose) {
        Card {
            Column(Modifier.padding(14.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(st.title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PixelPerson(st.clerk, Modifier.size(48.dp, 84.dp))
                    Text("“$line”", fontSize = 14.sp, modifier = Modifier.weight(1f))
                }
                Text("🍔 ${g.hunger.toInt()}%   🥤 ${g.thirst.toInt()}%   •   ${brl(g.money)}", fontSize = 12.sp)
                if (open) {
                    st.items.forEach { f ->
                        OutlinedButton(
                            onClick = {
                                if (g.eat(f)) {
                                    line = if (f.chat) gossip(g) else f.reply
                                    Voice.speak(line, st.pitch)
                                } else {
                                    line = "Ih, falta dinheiro, hein?"
                                }
                            },
                            Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("${f.emoji} ${f.name}", fontSize = 13.sp)
                                val fx = buildString {
                                    if (f.hunger > 0) append("🍔+${f.hunger.toInt()} ")
                                    if (f.thirst > 0) append("🥤+${f.thirst.toInt()}")
                                }
                                Text(fx, fontSize = 10.sp, color = Color(0xFF9E9E9E))
                            }
                            Text(brl(f.price), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Text("Horário: ${st.open / 60}h às ${if (st.close >= 24 * 60) 24 else st.close / 60}h", fontSize = 12.sp)
                }
                TextButton(onClick = onClose, Modifier.align(Alignment.CenterHorizontally)) { Text("Sair") }
            }
        }
    }
}

// ------------------------------------------------------------ conversa (café e rua)

private val CAFE_TALK = listOf(
    "Ontem um rapaz comprou um Fusca e chorou de emoção.",
    "Meu cunhado só compra carro batido e diz que é investimento. Vive duro.",
    "Dizem que o Seu Zé do ferro velho tem um faro pra pechincha.",
    "Cuidado com carro de leilão: nem tudo que brilha é ouro.",
    "Tá chovendo muito pouco, o povo só fala de preço de carro.",
    "Esse bairro era só mato, hoje só tem oficina e padaria!",
)

private val SMALL_TALK = listOf(
    "Tá um calorzão hoje, hein?", "Domingo tem churrasco lá em casa, aparece!",
    "Meu filho quer ser mecânico. Eu digo que é melhor vender carro.",
    "Tô economizando pra trocar de carro. Daqui uns 10 anos, quem sabe.",
    "Já viu o encontro de carros à noite? Tem cada máquina...",
    "A vida tá corrida, mas a gente vai levando.",
    "Dizem que quem compra carro bom sai no lucro. Quem é que não sabe?",
)

private val PED_NAMES = listOf(
    "Seu Nilton", "Dona Marta", "Jorginho", "Cláudia", "Rogério", "Bia",
    "Seu Joaquim", "Lúcia", "Tatá", "Marquinhos", "Vera", "Dudu",
)

fun pedName(i: Int): String = PED_NAMES[i % PED_NAMES.size]

private var lastTipDay = -1   // só 1 dica de carro barato por dia (só em memória)

fun marketTalk(g: GameState): String {
    val opts = mutableListOf<String>()
    g.event?.let { opts += "Tão falando de ${it.title.dropWhile { c -> !c.isLetter() }.lowercase()}: ${it.text}." }
    val cats = Cat.values()
    val best = cats.maxByOrNull { g.marketFor(it) }!!
    val worst = cats.minByOrNull { g.marketFor(it) }!!
    opts += "O povo tá pagando mais por ${best.label.lowercase()} agora."
    opts += "${worst.label} tá encalhado, ninguém quer pagar o que vale."
    opts += when (g.trendArrow()) {
        "▲" -> "O mercado tá aquecido, hein. Boa hora pra vender."
        "▼" -> "O mercado tá fraco. Quem tem paciência espera."
        else -> "O mercado tá parado, nem pra cima nem pra baixo."
    }
    return opts.random()
}

fun yardTip(g: GameState): String {
    val all = g.yards.withIndex().flatMap { (yi, l) -> l.map { yi to it } }
    val b = all.maxByOrNull { g.yardValue(it.second) - it.second.price }
    if (b == null || g.yardValue(b.second) - b.second.price < 800) return "Hoje tá fraco, não ouvi falar de nada bom."
    val y = b.second
    return "Um primo viu um ${CATALOG[y.model].fullName} ${y.year} no ${YARD_NAMES[b.first]} por ${brl(y.price)}. Dizem que vale bem mais!"
}

fun gossip(g: GameState): String = if (Random.nextInt(100) < 55) marketTalk(g) else CAFE_TALK.random()

@Composable
fun StreetChat(g: GameState, look: Look, name: String, me: String, onClose: () -> Unit) {
    var line by remember { mutableStateOf(listOf("Oi, $me! Tudo bem?", "Fala, $me! Em que posso ajudar?", "Opa! Você é aquele do ferro velho, né?").random()) }
    LaunchedEffect(Unit) { Voice.speak(line, if (look.female) 1.3f else 0.95f) }
    fun say(t: String) { line = t; Voice.speak(t, if (look.female) 1.3f else 0.95f) }
    Dialog(onDismissRequest = onClose) {
        Card {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PixelPerson(BuyerLook(look.female, CSKIN[look.skin], CHAIR[look.hair], CTOP[look.top], CBOT[look.bottom]), Modifier.size(48.dp, 84.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, fontWeight = FontWeight.Bold)
                        Text("“$line”", fontSize = 14.sp)
                    }
                }
                OutlinedButton(onClick = { g.spendTime(5); say(marketTalk(g)) }, Modifier.fillMaxWidth()) { Text("📈 Como tá o mercado de carros?", fontSize = 13.sp) }
                OutlinedButton(
                    onClick = {
                        g.spendTime(5)
                        if (lastTipDay == g.day) say("Já te contei o que sabia hoje, $me. Volta amanhã!")
                        else { lastTipDay = g.day; say(yardTip(g)) }
                    },
                    Modifier.fillMaxWidth(),
                ) { Text("🔎 Sabe de algum carro barato por aí?", fontSize = 13.sp) }
                OutlinedButton(
                    onClick = {
                        g.spendTime(5)
                        val pool = SMALL_TALK.toMutableList()
                        if (g.hunger < 35 || g.thirst < 35) pool += "Você tá com uma cara de quem precisa comer algo. Tem padaria logo ali!"
                        say(pool.random())
                    },
                    Modifier.fillMaxWidth(),
                ) { Text("💬 Bater um papo", fontSize = 13.sp) }
                TextButton(onClick = onClose, Modifier.align(Alignment.CenterHorizontally)) { Text("Até mais!") }
            }
        }
    }
}

// =====================================================================================
//  OFICINA: consertar (com minigame), tuning, pintura e empresa própria
// =====================================================================================

@Composable
fun WorkshopDialog(g: GameState, onClose: () -> Unit) {
    val open = garageOpen(g)
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (g.ownShop) "🏢 ${g.motorsName()}" else "🔧 Oficina do Tião", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("${g.dateText()}  •  ${brl(g.money)}", fontSize = 11.sp)
                    }
                    TextButton(onClick = onClose) { Text("✖ Sair") }
                }
                if (!open) {
                    Text("A oficina está fechada. Abre das 7h às 20h.\nMonte sua própria empresa pra ter oficina 24h!", fontSize = 14.sp)
                    return@Column
                }
                WorkshopBody(g)
            }
        }
    }
}

@Composable
private fun ColumnScope.WorkshopBody(g: GameState) {
    var sel by remember { mutableStateOf(g.garage.firstOrNull()?.id) }
    var tab by remember { mutableIntStateOf(0) }
    var game by remember { mutableStateOf<Int?>(null) }
    val car = g.garage.firstOrNull { it.id == sel } ?: g.garage.firstOrNull()

    if (g.garage.isNotEmpty()) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            g.garage.forEach { c ->
                val label = "${CATALOG[c.model].fullName} ${c.year}"
                if (c.id == car?.id) Button(onClick = { sel = c.id }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)) { Text(label, fontSize = 12.sp) }
                else OutlinedButton(onClick = { sel = c.id }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)) { Text(label, fontSize = 12.sp) }
            }
        }
    }
    if (car != null) {
        Box(Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp))) {
            Canvas(Modifier.matchParentSize()) {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF455A64), Color(0xFF263238))), size = Size(size.width, size.height * 0.72f))
                drawRect(Color(0xFF1C2327), Offset(0f, size.height * 0.72f), Size(size.width, size.height * 0.28f))
                drawRect(Color(0xFFFFC107), Offset(0f, size.height * 0.72f), Size(size.width, 4f))
                // painel de ferramentas na parede
                val u = size.width / 160f
                drawRect(Color(0xFF37474F), Offset(8f * u, 10f * u), Size(44f * u, 24f * u))
                for (k in 0..4) drawRect(listOf(Color(0xFFE53935), Color(0xFFFFC107), Color(0xFF90A4AE), Color(0xFF1E88E5), Color(0xFF43A047))[k], Offset((11f + k * 8f) * u, 13f * u), Size(3f * u, 16f * u))
                drawRect(Color(0xFFD32F2F), Offset(120f * u, 12f * u), Size(30f * u, 26f * u))
                drawRect(Color(0xFFB71C1C), Offset(120f * u, 22f * u), Size(30f * u, 2f * u))
            }
            CarArt(CATALOG[car.model].body, car.color, car.tuning, car.cond, Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.75f).height(100.dp).padding(bottom = 4.dp))
        }
        val invested = car.paid + car.spent
        Text("Pago ${brl(car.paid)} • Oficina ${brl(car.spent)} • Total ${brl(invested)} • Vale ${brl(g.marketValue(car))}", fontSize = 11.sp)
    }
    if (g.message.isNotEmpty()) Text(g.message, fontSize = 12.sp, color = Color(0xFFFFCA28))

    TabRow(selectedTabIndex = tab) {
        listOf("🔧 Reparos", "🎛️ Tuning", "🎨 Pintura", "🏢 Empresa").forEachIndexed { i, t ->
            Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t, fontSize = 11.sp, maxLines = 1) })
        }
    }

    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (tab == 3) {
            CompanyTab(g)
        } else if (car == null) {
            Text("Você não tem carro na garagem. Compre um no ferro velho e traga aqui! 🚗", fontSize = 14.sp)
        } else when (tab) {
            0 -> {
                Text("Conserte na mão (minigame): as peças custam metade do preço, mas se errar a peça é perdida.", fontSize = 12.sp)
                PART_NAMES.forEachIndexed { i, name ->
                    RepairCard(g, car, i, name)
                    if (car.cond[i] < MAX_COND) {
                        OutlinedButton(onClick = { game = i }, Modifier.fillMaxWidth()) {
                            Text("🎮 Consertar na mão · peças ${brl(g.mechanicCost(car, i))}/tentativa", fontSize = 12.sp)
                        }
                    }
                }
                if (car.cond.any { it < MAX_COND }) {
                    Button(onClick = { g.repairAll(car.id) }, Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp)) {
                        Text("🛠️ Mandar o Tião reparar tudo · ${if (g.ownShop) "GRÁTIS" else brl(g.repairAllCost(car))}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
            1 -> TUNINGS.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { t -> TuningCard(g, car, t, Modifier.weight(1f)) }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            else -> {
                Text("Pintura completa: ${brl(g.paintCost(car))}", fontWeight = FontWeight.Bold)
                PAINTS.chunked(7).forEachIndexed { row, chunk ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        chunk.forEachIndexed { j, p ->
                            val idx = row * 7 + j
                            val selected = idx == car.color
                            Spacer(
                                Modifier.size(38.dp).clip(CircleShape).background(Color(p.second))
                                    .border(if (selected) 3.dp else 1.dp, if (selected) Color.White else Color.Gray, CircleShape)
                                    .clickable { g.paint(car.id, idx) },
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    val gp = game
    if (gp != null && car != null) RepairMinigame(g, car.id, gp) { game = null }
}

@Composable
private fun CompanyTab(g: GameState) {
    if (g.ownShop) {
        Text("🏢 ${g.motorsName()} — sua empresa", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        RenameButton(g, "o", g.motorsName())
        Text("• Oficina aberta 24 horas\n• peças, tuning e pintura de graça pra você\n• Rende dinheiro todo dia com serviços de clientes", fontSize = 13.sp)
        Text("Carros vendidos: ${g.sold}\nLucro total: ${brl(g.totalProfit)}", fontSize = 13.sp)
    } else {
        Text("🏗️ Montar minha própria empresa", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(
            "Chega de pagar oficina dos outros. Compre, conserte e revenda com a sua marca:\n" +
                "• Placa nova: Ferrovelho Motors\n• Oficina aberta 24 horas\n• peças, tuning e pintura de graça pra você\n• Renda extra todo dia com serviços de clientes",
            fontSize = 13.sp,
        )
        Button(onClick = { g.buildShop() }, enabled = g.money >= SHOP_COST, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text("🏗️ Construir · ${brl(SHOP_COST)}", fontWeight = FontWeight.Bold)
        }
        if (g.money < SHOP_COST) Text("Faltam ${brl(SHOP_COST - g.money)}.", fontSize = 12.sp, color = Color(0xFFEF5350))
    }
}

// ------------------------------------------------------------ minigame de reparo

private val MG_DUR = listOf(850, 1100, 1100, 1300, 950)
private val MG_ZONE = listOf(0.12f, 0.18f, 0.18f, 0.22f, 0.15f)
private val MG_TITLE = listOf("Ajustar o motor", "Desamassar a lataria", "Calibrar os pneus", "Costurar o estofado", "Regular a suspensão")
private const val MG_ROUNDS = 3

@Composable
fun RepairMinigame(g: GameState, carId: Int, part: Int, onClose: () -> Unit) {
    val car = g.garage.firstOrNull { it.id == carId }
    if (car == null) { LaunchedEffect(Unit) { onClose() }; return }
    var round by remember { mutableIntStateOf(0) }
    var center by remember { mutableFloatStateOf(0.2f + Random.nextFloat() * 0.6f) }
    var gained by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf("Pare a barra na zona verde!") }
    val inf = rememberInfiniteTransition(label = "mg")
    val pos by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(MG_DUR[part], easing = LinearEasing), RepeatMode.Reverse), label = "pos")
    val zone = MG_ZONE[part]
    val finished = round >= MG_ROUNDS || car.cond[part] >= MAX_COND

    Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${PART_ICONS[part]} ${MG_TITLE[part]}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("${PART_NAMES[part]}: ${car.cond[part]}%   •   Tentativa ${minOf(round + 1, MG_ROUNDS)}/$MG_ROUNDS", fontSize = 13.sp)
                Canvas(Modifier.fillMaxWidth().height(36.dp)) {
                    drawRoundRect(Color(0xFF14121A), size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f))
                    if (!finished) {
                        drawRect(Color(0x66FFCA28), Offset((center - zone) * size.width, 0f), Size(zone * 2f * size.width, size.height))
                        drawRect(Color(0xFF66BB6A), Offset((center - zone / 2f) * size.width, 0f), Size(zone * size.width, size.height))
                        drawRect(Color.White, Offset(pos * size.width - 4f, 0f), Size(8f, size.height))
                    }
                }
                Text(result, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                if (!finished) {
                    Button(
                        onClick = {
                            val d = abs(pos - center)
                            val pts = if (d < zone / 2f) 10 else if (d < zone) 5 else 0
                            if (g.mechanicTry(carId, part, pts)) {
                                gained += pts
                                result = when (pts) { 10 -> "🎯 Perfeito! +10"; 5 -> "👍 Quase! +5"; else -> "💥 Errou! Peça perdida." }
                                round += 1
                                center = 0.2f + Random.nextFloat() * 0.6f
                            } else {
                                result = "Sem dinheiro pras peças."
                                round = MG_ROUNDS
                            }
                        },
                        Modifier.fillMaxWidth().height(56.dp),
                    ) { Text("🔧 APERTAR!", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                    Text("Peças: ${brl(g.mechanicCost(car, part))} por tentativa", fontSize = 11.sp)
                } else {
                    Text(if (gained > 0) "Você recuperou +$gained pontos em ${PART_NAMES[part]}!" else "Dessa vez não deu. Tenta de novo!", fontSize = 13.sp, textAlign = TextAlign.Center)
                    Button(onClick = onClose, Modifier.fillMaxWidth()) { Text("Concluir") }
                }
            }
        }
    }
}
