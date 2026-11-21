package com.ferrovelho.game

import android.content.SharedPreferences
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sin

// =====================================================================================
//  CIDADE GRANDE: banco, loja de roupas, casas, obras com operários e terrenos pra construir
// =====================================================================================

const val BANK_X = 5250f      // Banco Pistão
const val ROUPA_X = 6850f     // Moda Pistão (loja de roupas)

/** Terrenos à venda: compre, construa (com operários trabalhando) e receba aluguel ou venda. */
val LOT_X = listOf(8750f, 12600f, 14000f, 15500f, 17000f, 18600f, 20200f, 21800f, 23400f, 25000f, 26700f, 28300f)

/** Posições novas (a cidade não gera prédio em cima delas). */
val EXPANSION_X: List<Float> = listOf(BANK_X, ROUPA_X) + LOT_X

class HouseType(
    val name: String, val cost: Int, val days: Int, val rent: Int,
    val floors: Int, val w: Float, val wall: Color, val roof: Color,
)

val HOUSE_TYPES = listOf(
    HouseType("Casinha", 60_000, 4, 90, 1, 54f, Color(0xFFFFCC80), Color(0xFFD84315)),
    HouseType("Casa média", 120_000, 7, 190, 1, 62f, Color(0xFF90CAF9), Color(0xFF37474F)),
    HouseType("Sobrado", 220_000, 10, 360, 2, 62f, Color(0xFFA5D6A7), Color(0xFF4E342E)),
)

/** Estado de um terreno: tipo da casa, quando a obra começou e até quando o aluguel já foi coletado (em dias do jogo). */
class LotState(val type: Int, val start: Double, val collected: Double)

fun lotNow(g: GameState): Double = g.day + g.minutes / 1440.0

fun loadLots(p: SharedPreferences, now: Double): List<LotState?> = List(LOT_X.size) { i ->
    val parts = p.getString("lot$i", null)?.split(";")
    if (parts == null || parts.size < 3) {
        null
    } else {
        val ty = parts[0].toIntOrNull()
        val st = parts[1].toDoubleOrNull()
        val co = parts[2].toDoubleOrNull()
        if (ty == null || st == null || co == null || ty !in HOUSE_TYPES.indices || st > now + 0.01) null else LotState(ty, st, co)
    }
}

fun saveLot(p: SharedPreferences, i: Int, l: LotState?) {
    val e = p.edit()
    if (l == null) e.remove("lot$i") else e.putString("lot$i", "${l.type};${l.start};${l.collected}")
    e.apply()
}

fun lotProg(l: LotState, now: Double): Float =
    ((now - l.start) / HOUSE_TYPES[l.type].days).coerceIn(0.0, 1.0).toFloat()

private fun brlL(v: Long): String = brl(v.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt())

// -------------------------------------------------------------------------------------
//  DESENHO: casas
// -------------------------------------------------------------------------------------

private val H_WALL = listOf(0xFFFFCC80, 0xFF90CAF9, 0xFFA5D6A7, 0xFFF8BBD0, 0xFFFFF59D, 0xFFE1BEE7, 0xFFBCAAA4, 0xFFB2DFDB).map { Color(it) }
private val H_ROOF = listOf(0xFFD84315, 0xFF37474F, 0xFF4E342E, 0xFF880E4F, 0xFF6D4C41, 0xFF1A237E, 0xFFBF360C, 0xFF00695C).map { Color(it) }

/** Corpo de uma casa (chão em y = 104). */
fun DrawScope.houseBody(x: Float, w: Float, floors: Int, wall: Color, roof: Color, cam: Float, s: Float) {
    if (x + w + 8f < cam || x - 8f > cam + VIEW_W) return
    val h = 26f * floors + 4f
    wr(Color(0x33000000), x - 2f, 101f, w + 4f, 3f, cam, s)
    wr(wall, x, 104f - h, w, h, cam, s)
    wr(Color(0x14000000), x, 104f - h, 3f, h, cam, s)
    val n = 8
    for (k in 0 until n) {
        val lw = (w + 8f) * (1f - k / (n + 1f))
        wr(roof, x - 4f + (w + 8f - lw) / 2f, 104f - h - (k + 1) * 2.4f, lw, 2.5f, cam, s)
    }
    for (f in 0 until floors) {
        val wy = 104f - h + 7f + f * 26f
        wr(Color(0xFF455A64), x + 5f, wy - 1f, 12f, 11f, cam, s)
        wr(Color(0xFFB3E5FC), x + 6f, wy, 10f, 9f, cam, s)
        wr(Color(0xFF455A64), x + w - 17f, wy - 1f, 12f, 11f, cam, s)
        wr(Color(0xFFB3E5FC), x + w - 16f, wy, 10f, 9f, cam, s)
        wr(Color(0xFF455A64), x + 10.6f, wy, 0.8f, 9f, cam, s)
        wr(Color(0xFF455A64), x + w - 11.4f, wy, 0.8f, 9f, cam, s)
    }
    wr(Color(0xFF5D4037), x + w * 0.5f - 4f, 89f, 8f, 15f, cam, s)
    wr(Color(0xFFFFD54F), x + w * 0.5f + 2f, 96f, 1f, 1f, cam, s)
    wr(Color(0xFF9E9E9E), x + w * 0.5f - 5f, 103f, 10f, 1.4f, cam, s)
}

/** Casa decorativa da rua (8 cores, algumas com 2 andares e chaminé). */
fun DrawScope.drawHouseSprite(x: Float, v: Int, cam: Float, s: Float) {
    val i = v.mod(H_WALL.size)
    val floors = if (v % 5 == 4) 2 else 1
    val w = 54f + (v % 3) * 4f
    houseBody(x, w, floors, H_WALL[i], H_ROOF[(i + v / 8) % H_ROOF.size], cam, s)
    val h = 26f * floors + 4f
    if (v % 2 == 0) {
        wr(Color(0xFF8D6E63), x + w - 14f, 104f - h - 15f, 5f, 11f, cam, s)
        wr(Color(0xFF6D4C41), x + w - 15f, 104f - h - 16f, 7f, 1.6f, cam, s)
    }
    wr(Color(0xFF388E3C), x + w + 1f, 99f, 5f, 5f, cam, s)
    wr(Color(0xFF2E7D32), x - 5f, 100f, 4.5f, 4f, cam, s)
    if (v % 3 == 0) {
        wr(Color(0xFFFFFFFF), x - 3f, 98.5f, w + 6f, 0.9f, cam, s)
        wr(Color(0xFFFFFFFF), x - 3f, 101.5f, w + 6f, 0.9f, cam, s)
    }
}

// -------------------------------------------------------------------------------------
//  DESENHO: obras com operários
// -------------------------------------------------------------------------------------

private fun DrawScope.worker(wx: Float, by: Float, t: Float, k: Int, cam: Float, s: Float) {
    val skin = CSKIN[(k * 2 + 1) % CSKIN.size]
    val step = sin(t * 7f + k)
    val sw = sin(t * 6f + k * 1.7f)
    wr(Color(0x33000000), wx - 0.5f, by - 0.4f, 6f, 1f, cam, s)
    wr(Color(0xFF1565C0), wx + 0.2f, by - 8f, 1.7f, 7f + max(0f, step) * 0.8f, cam, s)
    wr(Color(0xFF1565C0), wx + 2.4f, by - 8f, 1.7f, 7f + max(0f, -step) * 0.8f, cam, s)
    wr(Color(0xFF3E2723), wx, by - 1.2f, 2.1f, 1.2f, cam, s)
    wr(Color(0xFF3E2723), wx + 2.3f, by - 1.2f, 2.1f, 1.2f, cam, s)
    wr(Color(0xFFFF6D00), wx, by - 14.5f, 4.2f, 6.8f, cam, s)
    wr(Color(0xFFFFF59D), wx, by - 11.5f, 4.2f, 0.7f, cam, s)
    wr(skin, wx + 0.5f, by - 18.2f, 3.2f, 3.6f, cam, s)
    wr(Color(0xFFFFEB3B), wx + 0.1f, by - 19.8f, 4f, 1.8f, cam, s)
    wr(Color(0xFFFFEB3B), wx - 0.4f, by - 18.3f, 5f, 0.6f, cam, s)
    if (k == 1) {
        wr(Color(0xFFFF6D00), wx + 3.4f, by - 14f, 1.4f, 3.5f, cam, s)
        wr(Color(0xFFC1623F), wx + 3.2f, by - 16f, 4f, 2.2f, cam, s)
    } else {
        wr(Color(0xFFFF6D00), wx + 3.6f, by - 14f + sw * 0.8f, 1.4f, 4.5f, cam, s)
        wr(Color(0xFF5D4037), wx + 4.6f, by - 17.5f + sw * 1.6f, 0.6f, 4.2f, cam, s)
        wr(Color(0xFF616161), wx + 3.8f, by - 18.1f + sw * 1.6f, 2.2f, 1.2f, cam, s)
    }
}

/** Obra: esqueleto de concreto, tijolos subindo, andaime, guindaste e operários (quando [working]). Largura ~100. */
fun DrawScope.drawSite(x: Float, prog: Float, floors: Int, working: Boolean, cam: Float, s: Float, t: Float, p: Paint) {
    if (x + 104f < cam || x - 6f > cam + VIEW_W) return
    val fl = floors.coerceIn(1, 3)
    val totalH = 26f * fl + 4f
    val wallH = 6f + prog * (totalH - 6f)
    val colH = totalH * (0.55f + 0.45f * prog)
    wr(Color(0xFFA1887F), x - 4f, 102f, 100f, 3f, cam, s)
    for (k in 0..3) wr(Color(0xFF9E9E9E), x + 6f + k * 24f, 104f - colH, 4f, colH, cam, s)
    for (f in 1..fl) {
        val y = 104f - f * 26f
        if (104f - y <= colH) wr(Color(0xFFBDBDBD), x + 4f, y, 80f, 2.2f, cam, s)
    }
    wr(Color(0xFFC1623F), x + 8f, 104f - wallH, 72f, wallH, cam, s)
    var yy = 101f
    while (yy > 104f - wallH) {
        wr(Color(0xFFA14F33), x + 8f, yy, 72f, 0.4f, cam, s)
        yy -= 3f
    }
    val sh = wallH + 12f
    wr(Color(0xFF78909C), x - 2f, 104f - sh, 1.2f, sh, cam, s)
    wr(Color(0xFF78909C), x + 86f, 104f - sh, 1.2f, sh, cam, s)
    var py = 91f
    while (py > 104f - sh) {
        wr(Color(0xFFFFB300), x - 5f, py, 14f, 1.4f, cam, s)
        wr(Color(0xFFFFB300), x + 80f, py, 14f, 1.4f, cam, s)
        py -= 13f
    }
    // guindaste
    val sw = if (working) sin(t * 0.6f) * 6f else 0f
    wr(Color(0xFFFFB300), x + 90f, 30f, 3f, 74f, cam, s)
    wr(Color(0xFFFFB300), x + 34f, 30f, 62f, 2.4f, cam, s)
    wr(Color(0xFF757575), x + 95f, 29f, 6f, 4f, cam, s)
    wr(Color(0xFF455A64), x + 88.5f, 26f, 6f, 4f, cam, s)
    val tx = x + 58f + sw
    val ly = 56f + (if (working) sin(t * 0.9f) * 3f else 0f)
    wr(Color(0xFF212121), tx, 32.4f, 0.5f, ly - 32.4f, cam, s)
    wr(Color(0xFFD7B98E), tx - 3f, ly, 7f, 4f, cam, s)
    // operários e poeira
    if (working) {
        for (k in 0 until 3) {
            val wx = x + 14f + k * 26f + sin(t * 0.6f + k * 2.1f) * 6f
            worker(wx, 104f, t, k, cam, s)
            val ph = (t * 0.8f + k * 0.35f) % 1f
            drawCircle(Color(0xFFD7CCC8).copy(alpha = 0.5f * (1f - ph)), (1.5f + ph * 2f) * s, Offset((x + 30f + k * 24f - cam) * s, (98f - ph * 12f) * s))
        }
    }
    // cerca de obra e placa
    for (k in 0..8) wr(Color(0xFF6D4C41), x - 2f + k * 12f, 96f, 1.2f, 8f, cam, s)
    wr(Color(0xFFFF6F00), x - 2f, 98f, 98f, 1.8f, cam, s)
    for (k in 0..14) wr(Color(0xFFFFFFFF), x - 1f + k * 6.5f, 98f, 2.4f, 1.8f, cam, s)
    wr(Color(0xFFFFEB3B), x + 28f, 87f, 38f, 8f, cam, s)
    wr(Color(0xFF212121), x + 28f, 87f, 38f, 0.7f, cam, s)
    label(p, "EM OBRAS", x + 47f, 93.2f, cam, s, 4.8f, 0xFF212121.toInt())
}

/** Terrenos: à venda, em obra (operários trabalhando das 7h às 17h) ou com a casa pronta. */
fun DrawScope.drawLots(lots: List<LotState?>, now: Double, working: Boolean, cam: Float, s: Float, t: Float, p: Paint) {
    for (i in LOT_X.indices) {
        val x = LOT_X[i]
        if (x + 70f < cam || x - 70f > cam + VIEW_W) continue
        val l = lots.getOrNull(i)
        if (l == null) {
            wr(Color(0xFF9CAA6B), x - 50f, 101f, 100f, 3f, cam, s)
            for (k in 0..7) wr(Color(0xFF8D6E63), x - 48f + k * 13.5f, 92f, 1.4f, 11f, cam, s)
            wr(Color(0xFF8D6E63), x - 48f, 94f, 94f, 1.2f, cam, s)
            wr(Color(0xFF8D6E63), x - 48f, 99f, 94f, 1.2f, cam, s)
            wr(Color(0xFF5D4037), x - 14f, 74f, 1.4f, 20f, cam, s)
            wr(Color(0xFF5D4037), x + 13f, 74f, 1.4f, 20f, cam, s)
            wr(Color(0xFFFFFFFF), x - 19f, 60f, 38f, 16f, cam, s)
            wr(Color(0xFFD32F2F), x - 19f, 60f, 38f, 2f, cam, s)
            label(p, "TERRENO", x, 68f, cam, s, 4.8f, 0xFFD32F2F.toInt())
            label(p, "À VENDA", x, 74.2f, cam, s, 4.8f, 0xFFD32F2F.toInt())
        } else {
            val ht = HOUSE_TYPES[l.type]
            val prog = lotProg(l, now)
            if (prog < 1f) {
                drawSite(x - 48f, prog, ht.floors, working, cam, s, t, p)
            } else {
                houseBody(x - ht.w / 2f, ht.w, ht.floors, ht.wall, ht.roof, cam, s)
                val h = 26f * ht.floors + 4f
                val top = 104f - h - 19.2f
                wr(Color(0xFF607D8B), x - 0.5f, top - 9f, 1f, 9f, cam, s)
                wr(Color(0xFF43A047), x + 0.5f, top - 9f, 6f, 4f, cam, s)
                wr(Color(0xFF388E3C), x + ht.w / 2f + 1f, 99f, 5f, 5f, cam, s)
            }
        }
    }
}

// -------------------------------------------------------------------------------------
//  DESENHO: banco e loja de roupas
// -------------------------------------------------------------------------------------

fun bankOpen(g: GameState): Boolean = g.minutes in (8 * 60) until (18 * 60)
fun roupaOpen(g: GameState): Boolean = g.minutes in (9 * 60) until (20 * 60)

fun DrawScope.drawBank(open: Boolean, cam: Float, s: Float, p: Paint) {
    val cx = BANK_X
    if (cx + 60f < cam || cx - 60f > cam + VIEW_W) return
    val x0 = cx - 50f
    wr(Color(0x33000000), x0, 101f, 100f, 3f, cam, s)
    wr(Color(0xFFCFD8DC), x0, 52f, 100f, 52f, cam, s)
    wr(Color(0xFF78909C), x0 - 4f, 46f, 108f, 7f, cam, s)
    wr(Color(0xFF90A4AE), x0 + 10f, 40f, 80f, 7f, cam, s)
    wr(Color(0xFFB0BEC5), x0 + 28f, 34f, 44f, 7f, cam, s)
    wr(Color(0xFF1B5E20), cx - 27f, 41f, 54f, 10f, cam, s)
    label(p, "BANCO PISTÃO", cx, 48.4f, cam, s, 6f, 0xFFFFFFFF.toInt())
    for (k in 0..4) {
        val colX = x0 + 6f + k * 20f
        if (k == 2) continue
        wr(Color(0xFFECEFF1), colX, 53f, 7f, 49f, cam, s)
        wr(Color(0xFFB0BEC5), colX + 5f, 53f, 2f, 49f, cam, s)
    }
    wr(Color(0xFF90A4AE), x0 - 2f, 101f, 104f, 3f, cam, s)
    wr(Color(0xFF37474F), cx - 9f, 72f, 18f, 29f, cam, s)
    wr(if (open) Color(0xFF81D4FA) else Color(0xFF546E7A), cx - 7.5f, 73.5f, 15f, 27f, cam, s)
    wr(Color(0xFF37474F), cx - 0.4f, 73.5f, 0.8f, 27f, cam, s)
    // relógio
    wr(Color(0xFFFFFFFF), cx - 3.5f, 55f, 7f, 7f, cam, s)
    wr(Color(0xFF212121), cx - 0.3f, 56f, 0.6f, 3.4f, cam, s)
    wr(Color(0xFF212121), cx - 0.3f, 58.8f, 2.4f, 0.6f, cam, s)
    // caixa eletrônico
    wr(Color(0xFF37474F), x0 + 83f, 80f, 11f, 21f, cam, s)
    wr(Color(0xFF69F0AE), x0 + 85f, 82f, 7f, 5f, cam, s)
    wr(Color(0xFF212121), x0 + 85f, 89f, 7f, 1.5f, cam, s)
    label(p, "24h", x0 + 88.5f, 98f, cam, s, 3.4f, 0xFFFFFFFF.toInt())
}

fun DrawScope.drawBankLit(open: Boolean, cam: Float, s: Float, night: Float) {
    val cx = BANK_X
    if (cx + 60f < cam || cx - 60f > cam + VIEW_W) return
    glow(Offset((cx - cam) * s, 50f * s), 46f * s, Color(0xFF80DEEA).copy(alpha = 0.18f * night))
    if (open) glow(Offset((cx - cam) * s, 88f * s), 30f * s, Color(0xFFB2EBF2).copy(alpha = 0.32f * night))
    glow(Offset((cx + 38f - cam) * s, 84f * s), 14f * s, Color(0xFF69F0AE).copy(alpha = 0.35f * night))
}

fun DrawScope.drawRoupa(open: Boolean, cam: Float, s: Float, p: Paint) {
    val cx = ROUPA_X
    if (cx + 60f < cam || cx - 60f > cam + VIEW_W) return
    val x0 = cx - 42f
    wr(Color(0x33000000), x0, 101f, 84f, 3f, cam, s)
    wr(Color(0xFFF8BBD0), x0, 56f, 84f, 48f, cam, s)
    wr(Color(0xFFAD1457), x0 - 2f, 52f, 88f, 5f, cam, s)
    wr(Color(0xFFFFFFFF), cx - 31f, 38f, 62f, 12f, cam, s)
    wr(Color(0xFFAD1457), cx - 31f, 38f, 62f, 1.2f, cam, s)
    wr(Color(0xFFAD1457), cx - 31f, 48.8f, 62f, 1.2f, cam, s)
    label(p, "MODA PISTÃO", cx, 46.3f, cam, s, 6f, 0xFFAD1457.toInt())
    for (k in 0 until 14) wr(if (k % 2 == 0) Color(0xFFAD1457) else Color(0xFFFFFFFF), x0 + 2f + k * 5f, 70f, 5f, 6f, cam, s)
    // vitrine com manequins
    wr(Color(0xFF5D4037), x0 + 4f, 79f, 40f, 22f, cam, s)
    wr(if (open) Color(0xFFFFF8E1) else Color(0xFFB3E5FC), x0 + 5.5f, 80.5f, 37f, 19f, cam, s)
    val mc = listOf(Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFFFDD835))
    for (k in 0..2) {
        val mx = x0 + 9f + k * 11f
        wr(Color(0xFFD7CCC8), mx + 1.2f, 83f, 3f, 3f, cam, s)
        wr(mc[k], mx, 86f, 5.4f, 7f, cam, s)
        wr(Color(0xFF283593), mx + 0.6f, 93f, 4.2f, 6f, cam, s)
    }
    // porta
    wr(Color(0xFF4E342E), x0 + 54f, 79f, 20f, 22f, cam, s)
    wr(if (open) Color(0xFFFFF8E1) else Color(0xFF90A4AE), x0 + 55.5f, 80.5f, 17f, 20f, cam, s)
    wr(Color(0xFFFFD54F), x0 + 69f, 91f, 1f, 2f, cam, s)
    // arara de roupas na calçada
    wr(Color(0xFF8D6E63), x0 - 12f, 94f, 9f, 1f, cam, s)
    wr(Color(0xFF8D6E63), x0 - 12f, 94f, 1f, 10f, cam, s)
    wr(Color(0xFF8D6E63), x0 - 4f, 94f, 1f, 10f, cam, s)
    wr(Color(0xFFEC407A), x0 - 10f, 95f, 2f, 5f, cam, s)
    wr(Color(0xFF42A5F5), x0 - 7f, 95f, 2f, 5f, cam, s)
}

fun DrawScope.drawRoupaLit(open: Boolean, cam: Float, s: Float, night: Float) {
    val cx = ROUPA_X
    if (cx + 60f < cam || cx - 60f > cam + VIEW_W) return
    glow(Offset((cx - cam) * s, 45f * s), 42f * s, Color(0xFFF48FB1).copy(alpha = 0.2f * night))
    if (open) glow(Offset((cx - 22f - cam) * s, 90f * s), 36f * s, Color(0xFFFFE08A).copy(alpha = 0.38f * night))
}

// -------------------------------------------------------------------------------------
//  TELAS
// -------------------------------------------------------------------------------------

@Composable
fun FullDialog(title: String, g: GameState, onClose: () -> Unit, body: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("${g.dateText()}  •  ${brl(g.money)}", fontSize = 11.sp)
                    }
                    TextButton(onClick = onClose) { Text("Sair") }
                }
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = body,
                )
            }
        }
    }
}

// ---------------------------------------- Banco

private const val BANK_LIMIT = 100_000L

private fun bankLoad(p: SharedPreferences, today: Int): Pair<Long, Long> {
    var sav = p.getLong("bank_sav", 0L)
    var loan = p.getLong("bank_loan", 0L)
    val last = p.getInt("bank_day", today)
    if (today < last) {
        sav = 0L
        loan = 0L
    } else {
        repeat(minOf(today - last, 400)) {
            sav += sav * 5 / 1000
            loan += loan * 15 / 1000
        }
    }
    p.edit().putLong("bank_sav", sav).putLong("bank_loan", loan).putInt("bank_day", today).apply()
    return sav to loan
}

@Composable
fun BankDialog(g: GameState, prefs: SharedPreferences, onClose: () -> Unit) {
    val init = remember { bankLoad(prefs, g.day) }
    var sav by remember { mutableLongStateOf(init.first) }
    var loan by remember { mutableLongStateOf(init.second) }
    var info by remember { mutableStateOf("") }
    val open = bankOpen(g)

    fun persist() {
        prefs.edit().putLong("bank_sav", sav).putLong("bank_loan", loan).putInt("bank_day", g.day).apply()
        g.save()
    }
    fun deposit(v: Long) {
        val a = minOf(v, g.money.toLong())
        if (a <= 0L) { info = "Você não tem dinheiro pra depositar."; return }
        g.money -= a.toInt(); sav += a; persist(); info = "Depositou ${brlL(a)} na poupança."
    }
    fun withdraw(v: Long) {
        val a = minOf(v, sav)
        if (a <= 0L) { info = "Não há saldo na poupança."; return }
        sav -= a; g.money += a.toInt(); persist(); info = "Sacou ${brlL(a)}."
    }
    fun borrow(v: Long) {
        val a = minOf(v, BANK_LIMIT - loan)
        if (a <= 0L) { info = "Limite de crédito atingido."; return }
        loan += a; g.money += a.toInt(); persist(); info = "Empréstimo de ${brlL(a)} liberado. Os juros correm todo dia!"
    }
    fun repay(v: Long) {
        val a = minOf(minOf(v, loan), g.money.toLong())
        if (a <= 0L) { info = if (loan <= 0L) "Você não tem dívida." else "Sem dinheiro pra pagar."; return }
        loan -= a; g.money -= a.toInt(); persist(); info = "Pagou ${brlL(a)} da dívida."
    }

    FullDialog("🏦 Banco Pistão", g, onClose) {
        if (!open) Text("Agência fechada. Atendimento das 8h às 18h.", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
        if (info.isNotEmpty()) Text(info, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Poupança: ${brlL(sav)}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Rende 0,5% ao dia. Fica guardado e seguro.", fontSize = 12.sp)
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp)) {
                    OutlinedButton({ deposit(1_000) }, Modifier.weight(1f), open, contentPadding = PaddingValues(4.dp)) { Text("Depositar 1 mil", fontSize = 11.sp) }
                    OutlinedButton({ deposit(10_000) }, Modifier.weight(1f), open, contentPadding = PaddingValues(4.dp)) { Text("Depositar 10 mil", fontSize = 11.sp) }
                    OutlinedButton({ deposit(Long.MAX_VALUE) }, Modifier.weight(1f), open, contentPadding = PaddingValues(4.dp)) { Text("Tudo", fontSize = 11.sp) }
                }
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp)) {
                    OutlinedButton({ withdraw(1_000) }, Modifier.weight(1f), open, contentPadding = PaddingValues(4.dp)) { Text("Sacar 1 mil", fontSize = 11.sp) }
                    OutlinedButton({ withdraw(10_000) }, Modifier.weight(1f), open, contentPadding = PaddingValues(4.dp)) { Text("Sacar 10 mil", fontSize = 11.sp) }
                    OutlinedButton({ withdraw(Long.MAX_VALUE) }, Modifier.weight(1f), open, contentPadding = PaddingValues(4.dp)) { Text("Tudo", fontSize = 11.sp) }
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Empréstimo: ${brlL(loan)}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Juros de 1,5% ao dia. Limite total: ${brlL(BANK_LIMIT)} (disponível ${brlL(BANK_LIMIT - loan)}).", fontSize = 12.sp)
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp)) {
                    OutlinedButton({ borrow(10_000) }, Modifier.weight(1f), open, contentPadding = PaddingValues(4.dp)) { Text("Pegar 10 mil", fontSize = 11.sp) }
                    OutlinedButton({ borrow(50_000) }, Modifier.weight(1f), open, contentPadding = PaddingValues(4.dp)) { Text("Pegar 50 mil", fontSize = 11.sp) }
                }
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp)) {
                    OutlinedButton({ repay(10_000) }, Modifier.weight(1f), open, contentPadding = PaddingValues(4.dp)) { Text("Pagar 10 mil", fontSize = 11.sp) }
                    OutlinedButton({ repay(Long.MAX_VALUE) }, Modifier.weight(1f), open, contentPadding = PaddingValues(4.dp)) { Text("Quitar tudo", fontSize = 11.sp) }
                }
            }
        }
        Text("Dica: o dinheiro da poupança não some se o negócio der errado.", fontSize = 11.sp)
    }
}

// ---------------------------------------- Loja de roupas

private val TOPS = listOf("Camiseta", "Regata", "Jaqueta")
private val BOTS = listOf("Calça", "Bermuda", "Saia")

@Composable
private fun OptRow(title: String, names: List<String>, sel: Int, onSel: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            names.forEachIndexed { i, n ->
                if (i == sel) Button({ onSel(i) }, contentPadding = PaddingValues(horizontal = 10.dp)) { Text(n, fontSize = 12.sp) }
                else OutlinedButton({ onSel(i) }, contentPadding = PaddingValues(horizontal = 10.dp)) { Text(n, fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun ColorRow(title: String, colors: List<Color>, sel: Int, onSel: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontSize = 12.sp)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            colors.forEachIndexed { i, c ->
                val shape = RoundedCornerShape(6.dp)
                Box(
                    Modifier.size(30.dp).background(c, shape)
                        .border(if (i == sel) 3.dp else 1.dp, if (i == sel) Color.White else Color.Gray, shape)
                        .clickable { onSel(i) },
                )
            }
        }
    }
}

@Composable
fun ClothesDialog(g: GameState, me: PlayerProfile, onBuy: (PlayerProfile) -> Unit, onClose: () -> Unit) {
    val o = me.look
    var topType by remember(me) { mutableIntStateOf(o.topType) }
    var top by remember(me) { mutableIntStateOf(o.top) }
    var botType by remember(me) { mutableIntStateOf(o.botType) }
    var bottom by remember(me) { mutableIntStateOf(o.bottom) }
    var shoes by remember(me) { mutableIntStateOf(o.shoes) }
    var glasses by remember(me) { mutableStateOf(o.glasses) }
    var info by remember { mutableStateOf("") }
    var tm by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { while (true) withFrameNanos { tm = (it / 1_000_000L) / 1000f } }
    val open = roupaOpen(g)
    val look = Look(o.female, o.skin, o.hairStyle, o.hair, topType, top, if (!o.female && botType == 2) 0 else botType, bottom, shoes, glasses)
    val cTop = if (topType != o.topType || top != o.top) 90 else 0
    val cBot = if (botType != o.botType || bottom != o.bottom) 80 else 0
    val cShoe = if (shoes != o.shoes) 120 else 0
    val cGl = if (glasses && !o.glasses) 150 else 0
    val cost = cTop + cBot + cShoe + cGl

    FullDialog("👗 Moda Pistão", g, onClose) {
        if (!open) Text("Loja fechada. Funciona das 9h às 20h.", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
        if (info.isNotEmpty()) Text(info, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Canvas(Modifier.size(110.dp, 230.dp)) {
                val c = size.height / 28f
                drawChar(look, (size.width - 12f * c) / 2f, c, c, false, 1f, tm * 8f)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OptRow("Parte de cima (R$ 90)", TOPS, topType) { topType = it }
                ColorRow("Cor", CTOP, top) { top = it }
                OptRow("Parte de baixo (R$ 80)", if (o.female) BOTS else BOTS.take(2), if (!o.female && botType == 2) 0 else botType) { botType = it }
                ColorRow("Cor", CBOT, bottom) { bottom = it }
                ColorRow("Tênis (R$ 120)", CSHOE, shoes) { shoes = it }
                if (o.glasses) Text("Você já tem óculos.", fontSize = 12.sp)
                else OptRow("Óculos (R$ 150)", listOf("Sem", "Com"), if (glasses) 1 else 0) { glasses = it == 1 }
            }
        }
        Text(if (cost > 0) "Total: ${brl(cost)}" else "Escolha as peças pra provar. Só paga o que mudar.", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Button(
            onClick = {
                g.money -= cost
                onBuy(PlayerProfile(me.name, look))
                g.save()
                info = "Comprou! Tá com visual novo."
            },
            enabled = open && cost > 0 && g.money >= cost,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (cost > 0 && g.money < cost) "Falta dinheiro" else "Comprar e vestir") }
    }
}

// ---------------------------------------- Terrenos e obras

@Composable
fun LotDialog(g: GameState, idx: Int, lots: List<LotState?>, prefs: SharedPreferences, onLots: (List<LotState?>) -> Unit, onClose: () -> Unit) {
    var info by remember { mutableStateOf("") }
    val now = lotNow(g)
    val lot = lots.getOrNull(idx)

    fun setLot(l: LotState?) {
        saveLot(prefs, idx, l)
        onLots(lots.toMutableList().also { it[idx] = l })
        g.save()
    }

    FullDialog(if (lot == null) "Terreno à venda" else "Obra / Casa", g, onClose) {
        if (info.isNotEmpty()) Text(info, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
        if (lot == null) {
            Text("Escolha a casa e já começa a obra. Os operários trabalham das 7h às 17h.", fontSize = 13.sp)
            for ((ti, ht) in HOUSE_TYPES.withIndex()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(ht.name, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("Obra de ${ht.days} dias • aluguel ${brl(ht.rent)}/dia • revenda ~${brl(roundTo50(ht.cost * 1.25))}", fontSize = 12.sp)
                        Button(
                            onClick = {
                                g.money -= ht.cost
                                setLot(LotState(ti, now, now + ht.days))
                                info = "Obra iniciada! Fica pronta em ${ht.days} dias."
                            },
                            enabled = g.money >= ht.cost,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(if (g.money >= ht.cost) "Construir · ${brl(ht.cost)}" else "Faltam ${brl(ht.cost - g.money)}") }
                    }
                }
            }
        } else {
            val ht = HOUSE_TYPES[lot.type]
            val prog = lotProg(lot, now)
            if (prog < 1f) {
                val left = ceil((1f - prog) * ht.days).toInt().coerceAtLeast(1)
                Text("${ht.name} em construção: ${(prog * 100).toInt()}%", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.fillMaxWidth().height(10.dp).background(Color(0x33888888), RoundedCornerShape(5.dp))) {
                    Box(Modifier.fillMaxWidth(prog.coerceAtLeast(0.02f)).height(10.dp).background(Color(0xFF43A047), RoundedCornerShape(5.dp)))
                }
                Text("Faltam uns $left dia(s). Os operários trabalham das 7h às 17h.", fontSize = 13.sp)
            } else {
                val from = max(lot.collected, lot.start + ht.days)
                val rent = ((now - from) * ht.rent).toInt().coerceAtLeast(0)
                val price = roundTo50(ht.cost * 1.25 * g.houseIdx)
                Text("${ht.name} pronta! 🏡", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Aluguel acumulado: ${brl(rent)}", fontSize = 13.sp)
                Button(
                    onClick = {
                        g.money += rent
                        setLot(LotState(lot.type, lot.start, now))
                        info = "Recebeu ${brl(rent)} de aluguel."
                    },
                    enabled = rent >= 1,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Coletar aluguel") }
                Button(
                    onClick = {
                        g.money += price + rent
                        g.totalProfit += price + rent - ht.cost
                        setLot(null)
                        info = "Vendeu a casa por ${brl(price)}."
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Vender casa · ${brl(price)}") }
                Text("O preço de venda acompanha o mercado imobiliário.", fontSize = 11.sp)
            }
        }
    }
}
