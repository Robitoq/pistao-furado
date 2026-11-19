package com.ferrovelho.game

import android.content.Context
import android.graphics.Paint
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.min

/** Imóveis do bairro novo: compre, entre, durma e cobre aluguel. */
class Prop(val name: String, val x: Float, val price: Int, val rent: Int, val floors: Int, val wall: Long, val roof: Long, val desc: String)

val PROPS = listOf(
    Prop("Casinha Azul", 6400f, 45_000, 80, 2, 0xFF64B5F6, 0xFF1565C0, "Simples e aconchegante. Boa pra começar."),
    Prop("Sobrado Verde", 7300f, 90_000, 160, 3, 0xFF81C784, 0xFF2E7D32, "Três andares e quintal. Rende um bom aluguel."),
    Prop("Prédio Central", 9200f, 180_000, 320, 5, 0xFFBCAAA4, 0xFF4E342E, "Cinco andares no coração do bairro."),
    Prop("Cobertura Skyline", 10200f, 400_000, 700, 7, 0xFF90A4AE, 0xFF263238, "Cobertura de luxo com vista pra cidade toda."),
    Prop("Mansão da Colina", 11300f, 900_000, 1500, 4, 0xFFFFF59D, 0xFF8D6E63, "Pra quem já fez fortuna no ferro-velho."),
)
val PROP_X = PROPS.map { it.x }

fun loadProps(c: Context): Set<Int> =
    (c.getSharedPreferences("city", Context.MODE_PRIVATE).getString("props", "") ?: "").split(",").mapNotNull { it.toIntOrNull() }.toSet()

fun saveProps(c: Context, s: Set<Int>) { c.getSharedPreferences("city", Context.MODE_PRIVATE).edit().putString("props", s.joinToString(",")).apply() }

fun DrawScope.drawProps(owned: Set<Int>, cam: Float, s: Float, t: Float, p: Paint) {
    PROPS.forEachIndexed { i, pr ->
        if (pr.x + 60f < cam || pr.x - 60f > cam + VIEW_W) return@forEachIndexed
        val mine = i in owned
        val w = 64f; val x0 = pr.x - w / 2f
        val h = 18f + pr.floors * 12f
        val top = 104f - h
        wr(Color(0x33000000), x0, 101f, w, 3f, cam, s)
        wr(Color(pr.wall), x0, top, w, h, cam, s)
        wr(Color(pr.roof), x0 - 2f, top - 4f, w + 4f, 5f, cam, s)
        for (f in 0 until pr.floors) for (k in 0..3) {
            val lit = mine || (k + f + i) % 3 == 0
            wr(if (lit) Color(0xFFFFE082) else Color(0xFF90CAF9), x0 + 6f + k * 14f, top + 6f + f * 12f, 8f, 7f, cam, s)
        }
        wr(Color(0xFF4E342E), pr.x - 5f, 104f - 14f, 10f, 14f, cam, s)
        wr(Color(0xFFFFD54F), pr.x + 2f, 104f - 8f, 1.5f, 1.5f, cam, s)
        // placa
        val bg = if (mine) Color(0xFF2E7D32) else Color(0xFFC62828)
        wr(Color(0xFF212121), pr.x - 23f, top - 16f, 46f, 11f, cam, s)
        wr(bg, pr.x - 21.5f, top - 14.5f, 43f, 8f, cam, s)
        label(p, if (mine) "MEU" else "À VENDA", pr.x, top - 8.4f, cam, s, 4.6f, android.graphics.Color.WHITE)
    }
}

@Composable
fun PropDialog(g: GameState, idx: Int, owned: Set<Int>, onOwned: (Set<Int>) -> Unit, onEnter: () -> Unit, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("city", Context.MODE_PRIVATE) }
    val pr = PROPS[idx]
    val mine = idx in owned
    var msg by remember { mutableStateOf("") }
    val last = prefs.getInt("rent$idx", g.day)
    Dialog(onDismissRequest = onClose) {
        Card {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🏢 ${pr.name}", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(pr.desc, fontSize = 13.sp)
                Text("${pr.floors} andares • aluguel de ${brl(pr.rent)} por dia", fontSize = 12.sp)
                Text("Seu dinheiro: ${brl(g.money)}", fontSize = 12.sp)
                if (msg.isNotEmpty()) Text(msg, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                if (!mine) {
                    Button(onClick = {
                        g.money -= pr.price
                        val n = owned + idx; saveProps(ctx, n); prefs.edit().putInt("rent$idx", g.day).apply(); onOwned(n)
                        msg = "Parabéns! ${pr.name} agora é seu 🎉"
                    }, Modifier.fillMaxWidth(), enabled = g.money >= pr.price) { Text("💰 Comprar por ${brl(pr.price)}") }
                } else {
                    Button(onClick = onEnter, Modifier.fillMaxWidth()) { Text("🚪 Entrar") }
                    val days = min(10, (g.day - last).coerceAtLeast(0))
                    OutlinedButton(onClick = {
                        val v = days * pr.rent
                        g.money += v; prefs.edit().putInt("rent$idx", g.day).apply(); msg = "Você recebeu ${brl(v)} de aluguel 💵"
                    }, Modifier.fillMaxWidth(), enabled = days > 0) { Text("💵 Cobrar aluguel (${days} dia(s) · ${brl(days * pr.rent)})") }
                    OutlinedButton(onClick = {
                        val v = pr.price * 6 / 10
                        g.money += v; val n = owned - idx; saveProps(ctx, n); onOwned(n); msg = "Vendido por ${brl(v)}"
                    }, Modifier.fillMaxWidth()) { Text("Vender por ${brl(pr.price * 6 / 10)}") }
                }
                TextButton(onClick = onClose) { Text("Fechar") }
            }
        }
    }
}
