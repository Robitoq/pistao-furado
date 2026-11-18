package com.ferrovelho.game

import android.graphics.Paint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// =====================================================================================
//  MAPA MAIOR: cidade (0–5000), parque (5000–6700) e praia (6700–8600)
// =====================================================================================

const val CITY_END = 5000f
const val BEACH_START = 6700f
const val WORLD_END = 8600f
const val PARK_X = 5800f     // fonte do parque
const val BEACH_X = 7500f    // quiosque da praia

private val FLOWERS = listOf(Color(0xFFEF5350), Color(0xFFFFEE58), Color(0xFFFFFFFF), Color(0xFFAB47BC))
private val UMBRELLAS = listOf(Color(0xFFE53935), Color(0xFFFDD835), Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFEC407A))
private val SEA = listOf(Color(0xFF81D4FA), Color(0xFF4FC3F7), Color(0xFF29B6F6), Color(0xFF039BE5), Color(0xFF0288D1), Color(0xFF0277BD))

private inline fun cells(a: Float, b: Float, step: Float, body: (Int, Float) -> Unit) {
    var k = Math.floor((a / step).toDouble()).toInt() - 1
    while (k * step < b + step) {
        body(k, k * step)
        k++
    }
}

fun DrawScope.drawZones(cam: Float, s: Float, t: Float, night: Float, skyFrac: Float, p: Paint) {
    val topC = lerp(Color(0xFF5DB4EA), Color(0xFF080D2B), night)
    val botC = lerp(Color(0xFFD6EEFA), Color(0xFF1B2A55), night)
    val c0 = lerp(topC, botC, skyFrac.coerceIn(0f, 1f))

    // ------------------------------------------------------------ PARQUE
    val a = max(cam, CITY_END)
    val b = min(cam + VIEW_W, BEACH_START)
    if (a < b) {
        drawRect(Brush.verticalGradient(listOf(c0, botC), -12f * s, 104f * s), Offset((a - cam) * s, -12f * s), Size((b - a) * s, 116f * s))
        // colinas ao fundo
        cells(a, b, 40f) { k, x ->
            val h = hsh(k * 31 + 5) % 18
            wr(Color(0xFFA5D6A7), x, 104f - (30f + h), 41f, 30f + h, cam, s)
        }
        cells(a, b, 52f) { k, x ->
            val h = hsh(k * 17 + 11) % 12
            wr(Color(0xFF81C784), x, 104f - (14f + h), 53f, 14f + h, cam, s)
        }
        // lago com patinhos
        drawOval(Color(0xFF4FC3F7), Offset((5380f - cam) * s, 94f * s), Size(220f * s, 11f * s))
        drawOval(Color(0x55FFFFFF), Offset((5400f - cam) * s, 96f * s), Size(60f * s, 2f * s))
        val dx = 5460f + sin(t * 0.5f) * 40f
        wr(Color(0xFFFFFFFF), dx, 97f, 6f, 3f, cam, s)
        wr(Color(0xFFFFB300), dx + (if (cos2(t * 0.5f) > 0f) 6f else -2f), 97.6f, 2f, 1f, cam, s)
        wr(Color(0xFFFFFFFF), dx + 40f, 99f, 6f, 3f, cam, s)
        // árvores
        cells(a, b, 64f) { k, x0 ->
            val h = hsh(k * 17 + 9)
            val x = x0 + 8f + (h / 100 % 30)
            if (h % 100 < 62 && abs(x - PARK_X) > 60f && abs(x - (PARK_X + 330f)) > 40f && (x < 5370f || x > 5610f)) {
                wr(Color(0xFF6D4C41), x, 82f, 5f, 26f, cam, s)
                drawCircle(Color(0xFF2E7D32), 15f * s, Offset((x + 2.5f - cam) * s, 74f * s))
                drawCircle(Color(0xFF388E3C), 11f * s, Offset((x - 6f - cam) * s, 80f * s))
                drawCircle(Color(0xFF43A047), 11f * s, Offset((x + 11f - cam) * s, 80f * s))
            }
        }
        // chão: grama + caminho
        wr(Color(0xFF7CB342), a, 104f, b - a, 14f, cam, s)
        wr(Color(0xFFE6D3A3), a, 110.5f, b - a, 7.5f, cam, s)
        wr(Color(0xFFD2BE8E), a, 110f, b - a, 0.8f, cam, s)
        cells(a, b, 17f) { k, x ->
            val h = hsh(k * 13 + 3)
            if (h % 100 < 45) wr(FLOWERS[h / 7 % 4], x + h % 9, 105f + (h / 11 % 4), 1.6f, 1.6f, cam, s)
        }
        // bancos
        cells(a, b, 190f) { k, x0 ->
            val x = x0 + 20f
            wr(Color(0xFF8D6E63), x, 101.5f, 22f, 2f, cam, s)
            wr(Color(0xFF6D4C41), x, 106f, 22f, 2.4f, cam, s)
            wr(Color(0xFF37474F), x + 2f, 108f, 1.5f, 3f, cam, s)
            wr(Color(0xFF37474F), x + 18.5f, 108f, 1.5f, 3f, cam, s)
        }
        // portal do parque
        val gx = 5040f
        wr(Color(0xFF6D4C41), gx - 26f, 70f, 5f, 38f, cam, s)
        wr(Color(0xFF6D4C41), gx + 21f, 70f, 5f, 38f, cam, s)
        wr(Color(0xFF2E7D32), gx - 28f, 62f, 56f, 14f, cam, s)
        label(p, "PARQUE CENTRAL", gx, 72f, cam, s, 4.6f, 0xFFFFFFFF.toInt())
        // fonte
        val fx = PARK_X
        wr(Color(0x33000000), fx - 30f, 106f, 60f, 3f, cam, s)
        wr(Color(0xFFB0BEC5), fx - 26f, 101f, 52f, 6f, cam, s)
        wr(Color(0xFF4FC3F7), fx - 24f, 99.5f, 48f, 3f, cam, s)
        wr(Color(0xFFCFD8DC), fx - 2.5f, 84f, 5f, 17f, cam, s)
        wr(Color(0xFFCFD8DC), fx - 9f, 90f, 18f, 3f, cam, s)
        for (k in 0..4) {
            wr(Color(0xAAFFFFFF), fx - 12f + k * 6f, 86f + sin(t * 4f + k) * 2f, 1.2f, 9f, cam, s)
        }
        // parquinho: balanço e escorregador
        val px0 = PARK_X + 330f
        wr(Color(0xFFE53935), px0 - 24f, 80f, 3f, 28f, cam, s)
        wr(Color(0xFFE53935), px0 + 21f, 80f, 3f, 28f, cam, s)
        wr(Color(0xFFE53935), px0 - 24f, 80f, 48f, 3f, cam, s)
        val sw = sin(t * 1.6f) * 5f
        wr(Color(0xFF5D4037), px0 - 12f + sw, 83f, 0.8f, 16f, cam, s)
        wr(Color(0xFF5D4037), px0 - 6f + sw, 83f, 0.8f, 16f, cam, s)
        wr(Color(0xFFFDD835), px0 - 13f + sw, 99f, 8.5f, 2f, cam, s)
        wr(Color(0xFF1E88E5), px0 + 8f, 90f, 12f, 3f, cam, s)
        wr(Color(0xFF1E88E5), px0 + 12f, 94f, 10f, 3f, cam, s)
        wr(Color(0xFF1E88E5), px0 + 16f, 98f, 10f, 3f, cam, s)
    }

    // ------------------------------------------------------------ PRAIA
    val a2 = max(cam, BEACH_START)
    val b2 = min(cam + VIEW_W, WORLD_END)
    if (a2 < b2) {
        drawRect(Brush.verticalGradient(listOf(c0, botC), -12f * s, 104f * s), Offset((a2 - cam) * s, -12f * s), Size((b2 - a2) * s, 116f * s))
        // mar em faixas
        for (i in SEA.indices) wr(SEA[i], a2, 50f + i * 7f, b2 - a2, 7.2f, cam, s)
        wr(Color(0x66FFFFFF), a2, 49.4f, b2 - a2, 0.8f, cam, s)
        cells(a2, b2, 36f) { k, x0 ->
            val x = x0 + sin(t * 0.8f + k) * 6f
            wr(Color(0x59FFFFFF), x, 54f + (hsh(k * 7 + 1) % 36), 14f, 0.9f, cam, s)
        }
        // ilha ao longe e veleiro
        wr(Color(0xFF5E8F6B), 7150f, 44f, 90f, 6f, cam, s)
        wr(Color(0xFF6FA27B), 7168f, 40f, 50f, 5f, cam, s)
        val bx = 7900f + sin(t * 0.15f) * 50f
        wr(Color(0xFF6D4C41), bx, 70f, 22f, 4f, cam, s)
        wr(Color(0xFF4E342E), bx + 10f, 54f, 1f, 16f, cam, s)
        wr(Color(0xFFFFFFFF), bx + 11.5f, 55f, 8f, 12f, cam, s)
        wr(Color(0xFFEF5350), bx + 2f, 58f, 8f, 9f, cam, s)
        // espuma, areia molhada e areia
        cells(a2, b2, 20f) { k, x ->
            wr(Color(0xCCFFFFFF), x, 91.5f + sin(t * 1.6f + k * 0.7f) * 1.2f, 20.5f, 1.6f, cam, s)
        }
        wr(Color(0xFFC9AE7C), a2, 93f, b2 - a2, 11f, cam, s)
        wr(Color(0xFFEBD9A0), a2, 104f, b2 - a2, 14f, cam, s)
        cells(a2, b2, 23f) { k, x ->
            val h = hsh(k * 19 + 7)
            if (h % 100 < 40) wr(Color(0x22000000), x + h % 11, 106f + (h / 13 % 10), 1.4f, 1f, cam, s)
        }
        // coqueiros, guarda-sóis e castelinhos
        cells(a2, b2, 80f) { k, x0 ->
            val h = hsh(k * 23 + 4)
            val x = x0 + 10f + (h / 100 % 40)
            if (x < BEACH_START + 50f || abs(x - BEACH_X) < 70f) return@cells
            val r = h % 100
            if (r < 32) {
                val swp = sin(t * 1.2f + k) * 1.2f
                wr(Color(0xFF8D6E63), x, 70f, 4f, 38f, cam, s)
                wr(Color(0xFF2E7D32), x - 14f + swp, 68f, 16f, 3f, cam, s)
                wr(Color(0xFF2E7D32), x + 2f + swp, 68f, 16f, 3f, cam, s)
                wr(Color(0xFF388E3C), x - 8f + swp, 64f, 10f, 3f, cam, s)
                wr(Color(0xFF388E3C), x + 2f + swp, 64f, 10f, 3f, cam, s)
                wr(Color(0xFF43A047), x - 3f + swp, 60f, 8f, 3f, cam, s)
                wr(Color(0xFF6D4C41), x + 1f, 71f, 2.4f, 2.4f, cam, s)
            } else if (r < 62) {
                val c = UMBRELLAS[h / 7 % UMBRELLAS.size]
                wr(Color(0xFF5D4037), x, 94f, 1.2f, 16f, cam, s)
                wr(c, x - 12f, 94f, 25f, 2f, cam, s)
                wr(c, x - 9f, 92f, 19f, 2f, cam, s)
                wr(c, x - 5f, 90.5f, 11f, 2f, cam, s)
                wr(Color(0x88FFFFFF), x - 3f, 94f, 4f, 2f, cam, s)
                wr(Color(0xFFFFFFFF), x - 11f, 112f, 22f, 4f, cam, s)
                wr(c, x - 11f, 113f, 22f, 1.4f, cam, s)
            } else if (r < 72) {
                wr(Color(0xFFD8B97A), x, 104f, 14f, 5f, cam, s)
                wr(Color(0xFFD8B97A), x + 2f, 101f, 10f, 3f, cam, s)
                wr(Color(0xFFE53935), x + 6f, 97f, 0.8f, 4f, cam, s)
                wr(Color(0xFFFFFFFF), x + 6.8f, 97f, 3f, 2f, cam, s)
            }
        }
        // portal da praia
        val gx = BEACH_START + 40f
        wr(Color(0xFF8D6E63), gx - 24f, 70f, 4f, 38f, cam, s)
        wr(Color(0xFF8D6E63), gx + 20f, 70f, 4f, 38f, cam, s)
        wr(Color(0xFF0288D1), gx - 26f, 62f, 52f, 14f, cam, s)
        label(p, "PRAIA", gx, 72f, cam, s, 6f, 0xFFFFFFFF.toInt())
        // quiosque
        val kx = BEACH_X
        wr(Color(0x33000000), kx - 42f, 106f, 84f, 3f, cam, s)
        wr(Color(0xFF8D6E63), kx - 34f, 82f, 3f, 26f, cam, s)
        wr(Color(0xFF8D6E63), kx + 31f, 82f, 3f, 26f, cam, s)
        wr(Color(0xFF6D4C41), kx - 32f, 96f, 66f, 12f, cam, s)
        wr(Color(0xFFFFE0B2), kx - 32f, 96f, 66f, 2f, cam, s)
        wr(Color(0xFFC8A951), kx - 42f, 80f, 84f, 5f, cam, s)
        wr(Color(0xFFD4B95E), kx - 36f, 76f, 72f, 5f, cam, s)
        wr(Color(0xFFB8963F), kx - 28f, 72f, 56f, 5f, cam, s)
        wr(Color(0xFF212121), kx - 24f, 86f, 48f, 8f, cam, s)
        label(p, "QUIOSQUE", kx, 92f, cam, s, 5f, 0xFFFFFFFF.toInt())
        wr(Color(0xFFE53935), kx - 28f, 110f, 6f, 4f, cam, s)
        wr(Color(0xFFE53935), kx + 22f, 110f, 6f, 4f, cam, s)
    }
}

/** Brilho à noite na fonte, no parquinho e no quiosque. */
fun DrawScope.drawZonesLit(cam: Float, s: Float, night: Float) {
    if (night <= 0f) return
    for ((x, y) in listOf(PARK_X to 90f, PARK_X + 330f to 92f, BEACH_X to 88f)) {
        if (x < cam - 60f || x > cam + VIEW_W + 60f) continue
        glow(Offset((x - cam) * s, y * s), 42f * s, Color(0xFFFFE082).copy(alpha = 0.3f * night))
    }
}

private fun cos2(v: Float): Float = kotlin.math.cos(v)

// ------------------------------------------------------------ o que fazer no parque e na praia

@Composable
fun ZoneDialog(g: GameState, kind: Int, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Card {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (kind == 0) "🌳 Parque Central" else "🏖️ Quiosque da Praia", fontWeight = FontWeight.Bold)
                Text("${brl(g.money)}", fontSize = 12.sp)
                if (g.message.isNotEmpty()) {
                    Text(g.message, Modifier.fillMaxWidth().clickable { g.message = "" }, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
                if (kind == 0) {
                    ZoneOption(g, "💧 Beber água da fonte (grátis)", 0) {
                        g.thirst = min(100.0, g.thirst + 25.0); g.spendTime(5); g.message = "Água fresquinha da fonte 💧"
                    }
                    ZoneOption(g, "🍿 Pipoca do carrinho · ${brl(8)}", 8) {
                        g.money -= 8; g.hunger = min(100.0, g.hunger + 12.0); g.spendTime(10); g.message = "Pipoca quentinha 🍿"
                    }
                    ZoneOption(g, "🪑 Descansar no banco (30 min)", 0) {
                        g.spendTime(30); g.message = "Você relaxou um pouco no parque 🌳"
                    }
                } else {
                    ZoneOption(g, "🥥 Água de coco · ${brl(12)}", 12) {
                        g.money -= 12; g.thirst = min(100.0, g.thirst + 35.0); g.spendTime(10); g.message = "Água de coco geladinha 🥥"
                    }
                    ZoneOption(g, "🍤 Porção de camarão · ${brl(45)}", 45) {
                        g.money -= 45; g.hunger = min(100.0, g.hunger + 40.0); g.spendTime(30); g.message = "Camarão fresquinho 🍤"
                    }
                    ZoneOption(g, "🌊 Dar um mergulho (45 min)", 0) {
                        g.thirst = max(0.0, g.thirst - 5.0); g.spendTime(45); g.message = "Mergulho refrescante! 🌊"
                    }
                    ZoneOption(g, "☀️ Tomar sol (30 min)", 0) {
                        g.spendTime(30); g.message = "Hora de relaxar no sol ☀️"
                    }
                }
                TextButton(onClick = onClose) { Text("Fechar") }
            }
        }
    }
}

@Composable
private fun ZoneOption(g: GameState, label: String, cost: Int, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = g.money >= cost, modifier = Modifier.fillMaxWidth()) { Text(label, fontSize = 13.sp) }
}
