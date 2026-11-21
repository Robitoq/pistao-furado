package com.ferrovelho.game

import android.graphics.Paint
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

// =====================================================================================
//  VILA SOSSEGO: a zona rural à esquerda da sua casa (x de WORLD_START até 0).
//  Rua de terra, fazendas, casas antigas, bar, vendinha, igreja e campinho de futebol.
// =====================================================================================

const val RURAL_GATE_X = -60f     // porteira "Vila Sossego" (início da vila)
const val VENDA_X = -1300f        // Venda da Dona Cida
const val BAR_X = -2400f          // Bar do Zé
const val IGREJA_X = -3600f
const val CAMPO_X = -5300f
const val FAZ1_X = -6600f
const val FAZ2_X = -8000f

fun ruralBarOpen(minutes: Int): Boolean = minutes >= 10 * 60
fun ruralVendaOpen(minutes: Int): Boolean = minutes in (6 * 60) until (19 * 60)

class RItem(val kind: Int, val v: Int, val x: Float, val w: Float)

private val RURAL_FIXED = listOf(
    RItem(15, 0, RURAL_GATE_X - 35f, 70f),
    RItem(11, 0, VENDA_X - 35f, 70f),
    RItem(10, 0, BAR_X - 42f, 84f),
    RItem(12, 0, IGREJA_X - 45f, 90f),
    RItem(13, 0, CAMPO_X - 200f, 400f),
    RItem(14, 0, FAZ1_X - 115f, 230f),
    RItem(14, 1, FAZ2_X - 115f, 230f),
)

private fun genRural(): List<RItem> {
    val r = Random(2024)
    val out = ArrayList<RItem>(RURAL_FIXED)
    var c = -110f
    val end = WORLD_START + 170f
    while (c > end) {
        val roll = r.nextInt(100)
        val (kind, w) = when {
            roll < 22 -> 0 to 56f
            roll < 46 -> 2 to 30f
            roll < 56 -> 3 to 48f
            roll < 66 -> 4 to 150f
            roll < 76 -> 6 to 110f
            roll < 82 -> 5 to 36f
            roll < 86 -> 7 to 34f
            roll < 89 -> 8 to 24f
            roll < 93 -> 9 to 50f
            roll < 96 -> 17 to 24f
            else -> 18 to 60f
        }
        val hit = RURAL_FIXED.firstOrNull { c > it.x - 26f && c - w < it.x + it.w + 26f }
        if (hit != null) { c = hit.x - 26f; continue }
        out.add(RItem(kind, r.nextInt(100), c - w, w))
        c -= w + 6f + r.nextInt(40)
    }
    return out
}

private val RURAL_ITEMS: List<RItem> = genRural()

// -------------------------------------------------------------------------------------
//  FUNDO, CHÃO E LUZES
// -------------------------------------------------------------------------------------

/** Morros distantes no lugar dos prédios (só onde o mundo é rural). */
fun DrawScope.drawRuralBack(cam: Float, s: Float, oy: Float, night: Float) {
    if (cam >= 0f) return
    val dark = Color(0xFF0F1A30)
    for (layer in 0..1) {
        val par = if (layer == 0) 0.25f else 0.45f
        val base = if (layer == 0) Color(0xFF9CC4A6) else Color(0xFF6FA777)
        val col = lerp(base, dark, night * 0.75f)
        var sx = 0f
        while (sx < VIEW_W) {
            if (sx + cam < 0f) {
                val fx = sx + cam * par
                val h = (if (layer == 0) 42f else 26f) + sin(fx * 0.021f + layer) * 12f + sin(fx * 0.057f + 2f) * 6f + sin(fx * 0.011f) * 8f
                drawRect(col, Offset(sx * s, oy + (104f - h) * s), androidx.compose.ui.geometry.Size(4.6f * s, h * s))
            }
            sx += 4f
        }
    }
}

/** Grama e rua de terra no lugar da calçada e do asfalto (x < 0). */
fun DrawScope.drawRuralGround(cam: Float, s: Float, t: Float, night: Float) {
    if (cam >= 0f) return
    val xa = cam
    val xb = min(0f, cam + VIEW_W)
    val w = xb - xa
    if (w <= 0f) return
    val dark = Color(0xFF101A2A)
    fun c(v: Long) = lerp(Color(v), dark, night * 0.5f)
    wr(c(0xFF6E9441), xa, 104f, w, 17f, cam, s)
    wr(c(0xFFB98F5C), xa, 121f, w, 29f, cam, s)
    wr(c(0xFFC9A06A), xa, 133f, w, 6f, cam, s)
    wr(c(0xFF9A7447), xa, 127f, w, 2.2f, cam, s)
    wr(c(0xFF9A7447), xa, 142f, w, 2.2f, cam, s)
    wr(c(0xFF8A9F4A), xa, 119f, w, 2.6f, cam, s)
    val k0 = (xa / 14f).toInt() - 1
    val k1 = (xb / 14f).toInt() + 1
    for (k in k0..k1) {
        val h = hsh(k * 31 + 7)
        val gx = k * 14f + (h % 9)
        if (gx < xa || gx > xb) continue
        wr(c(0xFF4F7A2C), gx, 108f + (h / 9 % 8), 1.2f, 3f, cam, s)
        wr(c(0xFF86B04F), gx + 3f, 112f + (h / 13 % 6), 1.1f, 2.4f, cam, s)
        when ((h / 7) % 8) {
            0 -> wr(c(0xFF8D7B5A), gx, 125f + (h / 5 % 20), 3f, 1.4f, cam, s)
            1 -> wr(c(0xFFFFEB3B), gx + 1f, 110f + (h / 3 % 6), 1f, 1f, cam, s)
            2 -> wr(c(0xFF8C6A3E), gx, 130f + (h / 5 % 14), 6f, 2f, cam, s)
            3 -> drawOval(c(0x558FB8D6), Offset((gx - cam) * s, (144f + (h / 3 % 4)) * s), androidx.compose.ui.geometry.Size(11f * s, 2.4f * s))
            else -> {}
        }
    }
}

fun DrawScope.drawRuralLit(cam: Float, s: Float, night: Float, minutes: Int) {
    if (night <= 0.02f || cam > 40f) return
    val warm = Color(0xFFFFC76B)
    for (ri in RURAL_ITEMS) {
        if (ri.x > cam + VIEW_W + 20f || ri.x + ri.w < cam - 20f) continue
        when (ri.kind) {
            0 -> if (hsh(ri.v) % 3 != 0) {
                glow(Offset((ri.x + 11f - cam) * s, 90f * s), 14f * s, warm.copy(alpha = 0.4f * night))
                glow(Offset((ri.x + ri.w - 11f - cam) * s, 90f * s), 14f * s, warm.copy(alpha = 0.4f * night))
            }
            10 -> if (ruralBarOpen(minutes)) {
                glow(Offset((ri.x + 53f - cam) * s, 92f * s), 24f * s, warm.copy(alpha = 0.45f * night))
                glow(Offset((ri.x + 22f - cam) * s, 86f * s), 22f * s, warm.copy(alpha = 0.35f * night))
                for (i in 0..9) glow(Offset((ri.x + 5f + i * 8f - cam) * s, 74f * s), 6f * s, Color(0xFFFFE08A).copy(alpha = 0.5f * night))
            }
            11 -> if (ruralVendaOpen(minutes)) glow(Offset((ri.x + 35f - cam) * s, 92f * s), 26f * s, warm.copy(alpha = 0.3f * night))
            12 -> glow(Offset((ri.x + 45f - cam) * s, 78f * s), 18f * s, warm.copy(alpha = 0.3f * night))
            14 -> glow(Offset((ri.x + 57f - cam) * s, 74f * s), 22f * s, warm.copy(alpha = 0.3f * night))
            15 -> {
                glow(Offset((ri.x + 10f - cam) * s, 80f * s), 12f * s, warm.copy(alpha = 0.5f * night))
                glow(Offset((ri.x + 60f - cam) * s, 80f * s), 12f * s, warm.copy(alpha = 0.5f * night))
            }
            else -> {}
        }
    }
}

// -------------------------------------------------------------------------------------
//  CENÁRIO
// -------------------------------------------------------------------------------------

fun DrawScope.drawRural(cam: Float, s: Float, t: Float, minutes: Int, p: Paint) {
    if (cam > 40f) return
    val barOpen = ruralBarOpen(minutes)
    val vendaOpen = ruralVendaOpen(minutes)
    for (ri in RURAL_ITEMS) {
        if (ri.x > cam + VIEW_W + 12f || ri.x + ri.w < cam - 12f) continue
        when (ri.kind) {
            0 -> rHouse(ri.x, ri.v, cam, s, t)
            2 -> rTree(ri.x, ri.v, cam, s, t)
            3 -> rFence(ri.x, cam, s)
            4 -> rPasture(ri.x, ri.v, cam, s, t)
            5 -> rHay(ri.x, cam, s)
            6 -> rCorn(ri.x, ri.v, cam, s, t)
            7 -> rWindmill(ri.x, cam, s, t)
            8 -> rWell(ri.x, cam, s)
            9 -> rCoop(ri.x, ri.v, cam, s, t)
            10 -> rBar(ri.x, barOpen, cam, s, t, p)
            11 -> rVenda(ri.x, vendaOpen, cam, s, p)
            12 -> rChurch(ri.x, cam, s)
            13 -> rField(ri.x, cam, s, t)
            14 -> rFarm(ri.x, ri.v, cam, s, t, p)
            15 -> rGate(ri.x, cam, s, p)
            17 -> rSign(ri.x, ri.v, cam, s, p)
            18 -> rTractor(ri.x, cam, s, t)
            else -> {}
        }
    }
    rEdge(cam, s, t, p)
}

private val RH_WALL = listOf(0xFFC9A66BL, 0xFFA1887FL, 0xFFF5F0E1L, 0xFFE3B65AL, 0xFFB0613DL)
private val RH_ROOF = listOf(0xFF8D3B2FL, 0xFF90A4AEL, 0xFFB5532EL, 0xFFB5532EL, 0xFF4E342EL)
private val RH_DOOR = listOf(0xFF5D4037L, 0xFF6D4C41L, 0xFF1E6FA8L, 0xFF2E7D32L, 0xFF3E2723L)

private fun DrawScope.rHouse(x: Float, v: Int, cam: Float, s: Float, t: Float) {
    val k = v % 5
    val w = 52f + (v % 3) * 4f
    val h = 26f
    fun r(c: Long, ax: Float, ay: Float, aw: Float, ah: Float) = wr(Color(c), ax, ay, aw, ah, cam, s)
    r(0x33000000, x - 2f, 101f, w + 4f, 3f)
    r(RH_WALL[k], x, 104f - h, w, h)
    r(0x22000000, x, 104f - h, 3f, h)
    if (k == 0) {
        r(0xFF6D4C41, x, 104f - h, w, 1.2f)
        r(0xFF6D4C41, x, 103f, w, 1f)
        var gx = x + 8f
        while (gx < x + w - 2f) { r(0xFF6D4C41, gx, 104f - h, 1f, h); gx += 12f }
    }
    if (k == 1) {
        var gx = x + 4f
        while (gx < x + w) { r(0x33000000, gx, 104f - h, 0.5f, h); gx += 4f }
    }
    if (k == 4) {
        for (row in 0 until 6) {
            r(0x30000000, x, 104f - h + row * 4.4f, w, 0.4f)
            var gx = x + (if (row % 2 == 0) 0f else 3f)
            while (gx < x + w) { r(0x30000000, gx, 104f - h + row * 4.4f, 0.5f, 4.4f); gx += 6f }
        }
    }
    val steps = 7
    for (i in 0 until steps) {
        val lw = (w + 8f) * (1f - i / (steps + 1f))
        r(RH_ROOF[k], x - 4f + (w + 8f - lw) / 2f, 104f - h - (i + 1) * 2.5f, lw, 2.6f)
    }
    r(RH_DOOR[k], x + w * 0.5f - 4f, 89f, 8f, 15f)
    r(0xFFFFD54F, x + w * 0.5f + 2f, 96f, 1f, 1f)
    for (side in 0..1) {
        val wx = if (side == 0) x + 7f else x + w - 17f
        r(0xFF3E2723, wx - 0.6f, 85f, 11.2f, 10.2f)
        r(0xFF9FC5D6, wx, 85.6f, 10f, 9f)
        r(RH_DOOR[k], wx - 2.6f, 85f, 2.4f, 10f)
        r(RH_DOOR[k], wx + 10.2f, 85f, 2.4f, 10f)
    }
    if (k == 0 || k == 1 || k == 4) {
        r(0xFF8D6E63, x + w - 12f, 104f - h - 16f, 5f, 12f)
        for (i in 0..2) {
            val ph = (t * 0.35f + i * 0.34f + x * 0.013f) % 1f
            drawCircle(Color(0xFFE0E0E0).copy(alpha = 0.5f * (1f - ph)), (1.3f + ph * 2.4f) * s, Offset((x + w - 9.5f + ph * 5f - cam) * s, (104f - h - 18f - ph * 16f) * s))
        }
    }
    if (k == 3) {
        r(0xFF8D6E63, x + w + 2f, 90f, 0.8f, 14f)
        r(0xFF8D6E63, x + w + 18f, 90f, 0.8f, 14f)
        r(0xFF5D4037, x + w + 2f, 91f, 17f, 0.4f)
        val sw = sin(t * 1.6f + x) * 0.5f
        r(0xFFEF5350, x + w + 4f, 91.4f, 3f, 4f + sw)
        r(0xFF42A5F5, x + w + 9f, 91.4f, 3f, 5f - sw)
        r(0xFFFFFFFF, x + w + 14f, 91.4f, 3f, 4f + sw)
    }
    r(0xFF558B2F, x - 4f, 99f, 5f, 5f)
    r(0xFFE57373, x - 3f, 98f, 1.2f, 1.2f)
}

private fun DrawScope.rTree(x: Float, v: Int, cam: Float, s: Float, t: Float) {
    val k = v % 3
    val sw = sin(t * 1.1f + x * 0.05f) * 0.8f
    fun r(c: Long, ax: Float, ay: Float, aw: Float, ah: Float) = wr(Color(c), ax, ay, aw, ah, cam, s)
    r(0x33000000, x + 6f, 107f, 18f, 2f)
    when (k) {
        0 -> {
            r(0xFF5D4037, x + 13f, 90f, 4f, 18f)
            drawCircle(Color(0xFF2E7D32), 13f * s, Offset((x + 15f + sw * 0.4f - cam) * s, 84f * s))
            drawCircle(Color(0xFF388E3C), 10f * s, Offset((x + 8f + sw * 0.5f - cam) * s, 90f * s))
            drawCircle(Color(0xFF43A047), 9f * s, Offset((x + 22f + sw * 0.5f - cam) * s, 90f * s))
            if (v % 2 == 0) {
                r(0xFFFFC107, x + 9f, 88f, 1.6f, 2f)
                r(0xFFFFC107, x + 20f, 92f, 1.6f, 2f)
                r(0xFFFFC107, x + 15f, 80f, 1.6f, 2f)
            }
        }
        1 -> {
            for (i in 0 until 8) r(0xFF8D6E63, x + 14f + i * 0.35f + sw * i / 8f, 105f - i * 3f, 2.6f, 3.2f)
            val tx = x + 16.6f + sw * 0.9f
            val ty = 81f
            for (a in 0 until 6) {
                val ang = (a * 60f - 90f) * (PI.toFloat() / 180f)
                drawLine(Color(0xFF2E7D32), Offset((tx - cam) * s, ty * s), Offset((tx + cos(ang) * 11f - cam) * s, (ty + sin(ang) * 6f + 3f) * s), 2f * s)
            }
        }
        else -> {
            r(0xFF7CB342, x + 14f, 94f, 3f, 14f)
            for (a in 0 until 5) {
                val ang = (a * 50f - 120f + sw * 4f) * (PI.toFloat() / 180f)
                drawLine(Color(0xFF558B2F), Offset((x + 15.5f - cam) * s, 94f * s), Offset((x + 15.5f + cos(ang) * 12f - cam) * s, (94f + sin(ang) * 9f + 2f) * s), 3f * s)
            }
            r(0xFFFFEB3B, x + 17f, 96f, 2.4f, 3f)
        }
    }
}

private fun DrawScope.rFence(x: Float, cam: Float, s: Float) {
    for (i in 0..4) wr(Color(0xFF8D6E63), x + i * 12f, 100f, 1.6f, 9f, cam, s)
    wr(Color(0xFF8D6E63), x, 102f, 48f, 1.2f, cam, s)
    wr(Color(0xFF8D6E63), x, 105.5f, 48f, 1.2f, cam, s)
}

private fun DrawScope.cow(cx: Float, by: Float, face: Int, graze: Float, cam: Float, s: Float) {
    fun r(c: Long, ax: Float, ay: Float, aw: Float, ah: Float) = wr(Color(c), ax, ay, aw, ah, cam, s)
    r(0x33000000, cx - 1f, by - 0.6f, 16f, 1.4f)
    r(0xFFFFFFFF, cx, by - 9f, 14f, 7f)
    r(0xFF212121, cx + 2f, by - 8.6f, 4f, 3f)
    r(0xFF212121, cx + 8f, by - 6f, 3f, 3f)
    r(0xFF3E2723, cx + 1f, by - 2f, 1.6f, 2f)
    r(0xFF3E2723, cx + 3.6f, by - 2f, 1.6f, 2f)
    r(0xFF3E2723, cx + 9f, by - 2f, 1.6f, 2f)
    r(0xFF3E2723, cx + 11.6f, by - 2f, 1.6f, 2f)
    val hx = if (face > 0) cx + 13f else cx - 4.5f
    r(0xFFFFFFFF, hx, by - 8f + graze * 1.6f, 4.5f, 4.5f)
    r(0xFFF8BBD0, hx + (if (face > 0) 2.8f else 0f), by - 5.6f + graze * 1.6f, 1.8f, 1.6f)
    r(0xFFEFEBE9, hx + (if (face > 0) 0.3f else 3.2f), by - 9f + graze * 1.6f, 1f, 1.4f)
    r(0xFF212121, if (face > 0) cx - 1.2f else cx + 14f, by - 8.5f, 1f, 5f)
}

private fun DrawScope.rPasture(x: Float, v: Int, cam: Float, s: Float, t: Float) {
    wr(Color(0xFF5F8A37), x, 106f, 150f, 12f, cam, s)
    for (i in 0..8) wr(Color(0xFF8D6E63), x + i * 18.5f, 98f, 1.6f, 10f, cam, s)
    wr(Color(0xFF8D6E63), x, 100f, 150f, 1.2f, cam, s)
    wr(Color(0xFF8D6E63), x, 103.5f, 150f, 1.2f, cam, s)
    for (i in 0..2) {
        val ph = t * 0.15f + i * 2f + v
        val cx = x + 20f + i * 48f + sin(ph) * 6f
        cow(cx, 112f + (i % 2) * 2f, if (cos(ph) >= 0f) 1 else -1, sin(t * 1.4f + i), cam, s)
    }
}

private fun DrawScope.rCorn(x: Float, v: Int, cam: Float, s: Float, t: Float) {
    wr(Color(0xFF7A5A36), x, 105f, 110f, 4f, cam, s)
    for (i in 0 until 22) {
        val sx = x + i * 5f
        val ht = 14f + hsh(i * 7 + v) % 6
        val sway = sin(t * 1.3f + i * 0.7f + x * 0.02f) * 0.7f
        wr(Color(0xFF4C8C2B), sx, 106f - ht, 1.2f, ht, cam, s)
        wr(Color(0xFF66A83A), sx + sway, 106f - ht - 2f, 1.4f, 3f, cam, s)
        wr(Color(0xFF5E9A33), sx - 1.5f, 106f - ht * 0.6f, 4.2f, 0.9f, cam, s)
        if (i % 2 == 0) wr(Color(0xFFE8C547), sx + 0.8f, 106f - ht * 0.55f, 1.4f, 3f, cam, s)
    }
}

private fun DrawScope.rHay(x: Float, cam: Float, s: Float) {
    for ((bx, by) in listOf(8f to 101f, 26f to 101f, 17f to 90f)) {
        drawCircle(Color(0xFFD9B44A), 7f * s, Offset((x + bx - cam) * s, by * s))
        drawCircle(Color(0xFFB8942F), 4.5f * s, Offset((x + bx - cam) * s, by * s))
        drawCircle(Color(0xFFD9B44A), 1.8f * s, Offset((x + bx - cam) * s, by * s))
    }
}

private fun DrawScope.rWindmill(x: Float, cam: Float, s: Float, t: Float) {
    fun r(c: Long, ax: Float, ay: Float, aw: Float, ah: Float) = wr(Color(c), ax, ay, aw, ah, cam, s)
    r(0xFF78909C, x + 10f, 60f, 1.4f, 46f)
    r(0xFF78909C, x + 22f, 60f, 1.4f, 46f)
    for (i in 0..4) r(0xFF90A4AE, x + 10f, 64f + i * 9f, 13.4f, 0.8f)
    r(0xFFB0BEC5, x + 14f, 96f, 6f, 8f)
    val cx = x + 16.5f
    val cy = 58f
    for (k in 0 until 8) {
        val ang = t * 1.2f + k * (PI.toFloat() / 4f)
        drawLine(Color(0xFFECEFF1), Offset((cx - cam) * s, cy * s), Offset((cx + cos(ang) * 11f - cam) * s, (cy + sin(ang) * 11f) * s), 1.6f * s)
    }
    drawCircle(Color(0xFF455A64), 1.8f * s, Offset((cx - cam) * s, cy * s))
    r(0xFFEF5350, cx + 2f, cy - 1f, 12f, 3f)
}

private fun DrawScope.rWell(x: Float, cam: Float, s: Float) {
    fun r(c: Long, ax: Float, ay: Float, aw: Float, ah: Float) = wr(Color(c), ax, ay, aw, ah, cam, s)
    r(0xFF9E9E9E, x + 4f, 98f, 16f, 8f)
    r(0xFF757575, x + 4f, 101f, 16f, 0.6f)
    r(0xFF6D4C41, x + 5f, 84f, 1.6f, 14f)
    r(0xFF6D4C41, x + 17f, 84f, 1.6f, 14f)
    r(0xFFA1887F, x + 2f, 82f, 20f, 3f)
    r(0xFF5D4037, x + 11f, 85f, 0.5f, 8f)
    r(0xFF795548, x + 9.5f, 93f, 3f, 3f)
}

private fun DrawScope.rCoop(x: Float, v: Int, cam: Float, s: Float, t: Float) {
    fun r(c: Long, ax: Float, ay: Float, aw: Float, ah: Float) = wr(Color(c), ax, ay, aw, ah, cam, s)
    r(0xFF8D6E63, x + 4f, 90f, 24f, 14f)
    r(0xFF6D4C41, x + 2f, 87f, 28f, 3.5f)
    r(0xFF3E2723, x + 13f, 96f, 6f, 8f)
    for (i in 0..2) {
        val face = if (cos(t * 0.3f + i * 2f + v) >= 0f) 1 else -1
        val cx = x + 32f + i * 5f + sin(t * 0.3f + i * 2f + v) * 3f
        val peck = if (sin(t * 5f + i * 1.7f) > 0.5f) 1.2f else 0f
        val col = if (i == 1) 0xFFB5651DL else 0xFFFFFFFFL
        r(col, cx, 104f, 4f, 3f)
        r(col, cx + (if (face > 0) 3.2f else -0.8f), 102.5f + peck, 1.6f, 1.8f)
        r(0xFFFFA000, cx + (if (face > 0) 4.8f else -1.6f), 103.2f + peck, 1f, 0.6f)
        r(0xFFFFA000, cx + 1.5f, 107f, 0.5f, 1.2f)
    }
}

private val SIGN_TEXT = listOf(
    listOf("FAZENDA", "BOA VISTA"), listOf("VENDE-SE", "OVOS"), listOf("LEITE", "FRESCO"),
    listOf("CUIDADO:", "GADO NA PISTA"), listOf("BAR DO ZÉ", "SEGUINDO..."), listOf("PROIBIDO", "PESCAR"),
)

private fun DrawScope.rSign(x: Float, v: Int, cam: Float, s: Float, p: Paint) {
    fun r(c: Long, ax: Float, ay: Float, aw: Float, ah: Float) = wr(Color(c), ax, ay, aw, ah, cam, s)
    r(0xFF6D4C41, x + 11f, 92f, 1.8f, 16f)
    r(0xFF5D4037, x, 83.5f, 24f, 12f)
    r(0xFFE8D5A6, x + 0.8f, 84.3f, 22.4f, 10.4f)
    val tx = SIGN_TEXT[v % SIGN_TEXT.size]
    label(p, tx[0], x + 12f, 89f, cam, s, 3.4f, 0xFF3E2723.toInt())
    label(p, tx[1], x + 12f, 93f, cam, s, 3.0f, 0xFFB71C1C.toInt())
}

private fun DrawScope.rTractor(x: Float, cam: Float, s: Float, t: Float) {
    val ph = t * 0.15f + x * 0.01f
    val tx = x + 20f + sin(ph) * 24f
    val m = if (cos(ph) < 0f) -1f else 1f
    fun b(c: Long, o: Float, y: Float, wd: Float, ht: Float) {
        val a = tx + o * m
        wr(Color(c), if (m > 0f) a else a - wd, y, wd, ht, cam, s)
    }
    b(0x33000000, 0f, 107f, 26f, 1.6f)
    b(0xFF2E7D32, 4f, 98f, 18f, 7f)
    b(0xFF2E7D32, 16f, 96f, 10f, 6f)
    b(0xFF1B5E20, 4f, 89f, 10f, 9f)
    b(0xFFB3E5FC, 5.5f, 90.5f, 7f, 6f)
    b(0xFF424242, 22f, 87f, 1f, 9f)
    drawCircle(Color(0xFF212121), 7f * s, Offset((tx + 8f * m - cam) * s, 103f * s))
    drawCircle(Color(0xFF616161), 3f * s, Offset((tx + 8f * m - cam) * s, 103f * s))
    drawCircle(Color(0xFF212121), 4.5f * s, Offset((tx + 22f * m - cam) * s, 105.5f * s))
    drawCircle(Color(0xFF616161), 1.8f * s, Offset((tx + 22f * m - cam) * s, 105.5f * s))
    for (i in 0..1) {
        val pp = (t * 0.9f + i * 0.5f) % 1f
        drawCircle(Color(0xFF9E9E9E).copy(alpha = 0.55f * (1f - pp)), (1.2f + pp * 2f) * s, Offset((tx + 22.5f * m - cam) * s, (86f - pp * 12f) * s))
    }
}

private fun DrawScope.rBar(x: Float, open: Boolean, cam: Float, s: Float, t: Float, p: Paint) {
    fun r(c: Long, ax: Float, ay: Float, aw: Float, ah: Float) = wr(Color(c), ax, ay, aw, ah, cam, s)
    r(0x33000000, x - 2f, 101f, 88f, 3f)
    r(0xFFE8DCC0, x, 70f, 84f, 34f)
    r(0xFF6FA96A, x, 94f, 84f, 10f)
    r(0xFF90A4AE, x - 3f, 64f, 90f, 7f)
    r(0xFF78909C, x - 3f, 69f, 90f, 2f)
    r(0xFF9FB0B8, x + 6f, 59f, 72f, 5.5f)
    r(0xFF6D4C41, x + 18f, 60f, 1.2f, 4f)
    r(0xFF6D4C41, x + 64f, 60f, 1.2f, 4f)
    r(0xFF5D4037, x + 13f, 47.5f, 58f, 15f)
    r(0xFFFFD54F, x + 14f, 48.5f, 56f, 13f)
    label(p, "BAR DO ZÉ", x + 42f, 58.2f, cam, s, 7.2f, 0xFFB71C1C.toInt())
    drawCircle(Color(0xFFD32F2F), 6.5f * s, Offset((x + 12f - cam) * s, 82f * s))
    label(p, "GELADA", x + 12f, 83f, cam, s, 3.2f, 0xFFFFFFFF.toInt())
    r(0xFF3E2723, x + 45f, 75f, 17f, 29f)
    r(if (open) 0xFFFFE0A3L else 0xFF2B2118L, x + 46.5f, 76.5f, 14f, 27.5f)
    if (open) {
        r(0xFF6D4C41, x + 46.5f, 92f, 14f, 3f)
        r(0xFF2E7D32, x + 48f, 86f, 1.6f, 6f)
        r(0xFFFFB300, x + 51f, 86.5f, 1.6f, 5.5f)
        r(0xFF1565C0, x + 54f, 86f, 1.6f, 6f)
        r(0xFF3E2723, x + 56.5f, 80f, 3f, 8f)
    }
    r(0xFF3E2723, x + 8f, 78f, 29f, 16f)
    r(if (open) 0xFFFFE9B0L else 0xFF3A4A52L, x + 9.5f, 79.5f, 26f, 13f)
    for (i in 0..5) r(0xFF212121, x + 9.5f + i * 5.1f, 79.5f, 0.6f, 13f)
    r(0xFF424242, x, 71f, 84f, 0.4f)
    for (i in 0..9) r(if (i % 3 == 0) 0xFFFF5252L else if (i % 3 == 1) 0xFFFFEB3BL else 0xFF69F0AEL, x + 4f + i * 8f, 72f + (i % 2) * 1.5f, 2f, 2f)
    // mesas de plástico
    for (tb in listOf(3f, 66f)) {
        r(0xFFEF5350, x + tb, 99f, 14f, 1.8f)
        r(0xFF9E9E9E, x + tb + 6.4f, 100.8f, 1.2f, 7f)
        r(0xFFFFFFFF, x + tb - 3f, 100f, 3f, 8f)
        r(0xFFFFFFFF, x + tb + 14f, 100f, 3f, 8f)
    }
    // cachorro dormindo
    r(0xFF8D6E63, x + 33f, 105.5f, 8f, 3.2f)
    r(0xFF6D4C41, x + 31f, 105f, 3.2f, 3f)
    val z = (t * 0.8f) % 1f
    label(p, "z", x + 38f, 103f - z * 6f, cam, s, 4f, 0xFF455A64.toInt())
    label(p, if (open) "ABERTO" else "FECHADO", x + 53f, 73.2f, cam, s, 3f, if (open) 0xFF1B5E20.toInt() else 0xFFB71C1C.toInt())
}

private fun DrawScope.rVenda(x: Float, open: Boolean, cam: Float, s: Float, p: Paint) {
    fun r(c: Long, ax: Float, ay: Float, aw: Float, ah: Float) = wr(Color(c), ax, ay, aw, ah, cam, s)
    r(0x33000000, x - 2f, 101f, 74f, 3f)
    r(0xFFB08968, x, 74f, 70f, 30f)
    var gx = x + 5f
    while (gx < x + 70f) { r(0x33000000, gx, 74f, 0.5f, 30f); gx += 5f }
    r(0xFF6D4C41, x - 2f, 68f, 74f, 7f)
    for (i in 0 until 12) r(if (i % 2 == 0) 0xFF2E7D32L else 0xFFFFFFFFL, x + i * 5.8f, 81f, 5.8f, 5f)
    r(0xFFFFFFFF, x + 8f, 57f, 54f, 11f)
    r(0xFF2E7D32, x + 8f, 57f, 54f, 1f)
    label(p, "VENDA DA CIDA", x + 35f, 65.4f, cam, s, 5.4f, 0xFF1B5E20.toInt())
    r(0xFF4E342E, x + 47f, 84f, 14f, 20f)
    r(if (open) 0xFFFFE0A3L else 0xFF2B2118L, x + 48.5f, 85.5f, 11f, 18.5f)
    r(0xFF4E342E, x + 6f, 88f, 22f, 12f)
    r(0xFFB3E5FC, x + 7.4f, 89.4f, 19.2f, 9.2f)
    r(0xFF8D6E63, x + 4f, 97f, 14f, 7f)
    for (i in 0..3) r(0xFFFB8C00, x + 5f + i * 3.2f, 95.4f, 2.6f, 2.6f)
    r(0xFFD7CCC8, x + 21f, 95f, 9f, 9f)
    r(0xFF795548, x + 32f, 96f, 8f, 8f)
    label(p, "FIADO NÃO", x + 36f, 79.5f, cam, s, 3.2f, 0xFFFFFFFF.toInt())
}

private fun DrawScope.rChurch(x: Float, cam: Float, s: Float) {
    fun r(c: Long, ax: Float, ay: Float, aw: Float, ah: Float) = wr(Color(c), ax, ay, aw, ah, cam, s)
    r(0x33000000, x + 8f, 101f, 76f, 3f)
    r(0xFFF5F5F5, x + 10f, 72f, 70f, 32f)
    for (i in 0 until 7) {
        val lw = 76f * (1f - i / 8f)
        r(0xFFB5532E, x + 7f + (76f - lw) / 2f, 70f - i * 2.4f, lw, 2.6f)
    }
    r(0xFFF5F5F5, x + 33f, 38f, 24f, 40f)
    for (i in 0 until 5) {
        val lw = 28f * (1f - i / 6f)
        r(0xFFB5532E, x + 31f + (28f - lw) / 2f, 37f - i * 2.4f, lw, 2.6f)
    }
    r(0xFF3E2723, x + 40f, 48f, 10f, 14f)
    r(0xFFFFC107, x + 43f, 54f, 4f, 5f)
    r(0xFF5D4037, x + 44.4f, 20f, 1.3f, 11f)
    r(0xFF5D4037, x + 42f, 23f, 6f, 1.3f)
    r(0xFF1E6FA8, x + 38f, 84f, 14f, 20f)
    r(0xFF144E78, x + 44.4f, 84f, 0.8f, 20f)
    r(0xFF90CAF9, x + 17f, 82f, 8f, 12f)
    r(0xFF90CAF9, x + 65f, 82f, 8f, 12f)
    r(0xFFBDBDBD, x + 34f, 103f, 22f, 1.6f)
}

private fun DrawScope.rField(x: Float, cam: Float, s: Float, t: Float) {
    fun r(c: Long, ax: Float, ay: Float, aw: Float, ah: Float) = wr(Color(c), ax, ay, aw, ah, cam, s)
    r(0xFF4E8B3A, x, 103f, 400f, 16f)
    for (i in 0 until 10) r(0xFF57973F, x + i * 40f, 103f, 20f, 16f)
    r(0xFFFFFFFF, x, 103f, 400f, 0.8f)
    r(0xFFFFFFFF, x, 118.2f, 400f, 0.8f)
    r(0xFFFFFFFF, x + 199.6f, 103f, 0.8f, 16f)
    drawCircle(Color(0xFFFFFFFF), 6f * s, Offset((x + 200f - cam) * s, 111f * s), style = Stroke(width = 0.8f * s))
    for (gxx in listOf(x + 2f, x + 390f)) {
        r(0xFFFFFFFF, gxx, 90f, 1.2f, 16f)
        r(0xFFFFFFFF, gxx + (if (gxx > x + 200f) 0f else 8f), 90f, 1.2f, 16f)
        r(0xFFFFFFFF, gxx, 90f, 9.2f, 1.2f)
        r(0x33FFFFFF, gxx, 91f, 9.2f, 15f)
    }
    val look = listOf("Léo", "Cadu", "Beto", "Paulinho")
    for (i in 0..3) {
        val ph = t * 0.5f + i * 1.7f
        val kx = x + 200f + sin(ph) * 120f + (i - 1.5f) * 12f
        drawChar(npcLookFor(look[i]), (kx - 4f - cam) * s, (114f - 26f * 0.7f) * s, s * 0.7f, cos(ph) < 0f, 1f, t * 6f + i)
    }
    val bx = x + 200f + sin(t * 0.5f + 0.9f) * 124f
    val by = 115f - kotlin.math.abs(sin(t * 3f)) * 4f
    drawCircle(Color(0xFFFFFFFF), 1.6f * s, Offset((bx - cam) * s, by * s))
    drawCircle(Color(0xFF212121), 0.6f * s, Offset((bx - cam) * s, by * s))
}

private fun DrawScope.rFarm(x: Float, v: Int, cam: Float, s: Float, t: Float, p: Paint) {
    fun r(c: Long, ax: Float, ay: Float, aw: Float, ah: Float) = wr(Color(c), ax, ay, aw, ah, cam, s)
    r(0x33000000, x + 8f, 101f, 100f, 3f)
    r(0xFFB23B2E, x + 10f, 64f, 90f, 40f)
    r(0xFFFFFFFF, x + 10f, 64f, 90f, 1.2f)
    for (i in 0 until 6) {
        val lw = 98f * (1f - i / 7f)
        r(0xFF5D4037, x + 6f + (98f - lw) / 2f, 63f - i * 3f, lw, 3.2f)
    }
    r(0xFFFFE0B2, x + 49f, 68f, 14f, 9f)
    r(0xFF8E2A20, x + 43f, 80f, 26f, 24f)
    drawLine(Color(0xFFFFFFFF), Offset((x + 43f - cam) * s, 80f * s), Offset((x + 69f - cam) * s, 104f * s), 1.2f * s)
    drawLine(Color(0xFFFFFFFF), Offset((x + 69f - cam) * s, 80f * s), Offset((x + 43f - cam) * s, 104f * s), 1.2f * s)
    r(0xFFCFD8DC, x + 108f, 46f, 22f, 58f)
    for (i in 0..3) r(0xFFB0BEC5, x + 108f, 54f + i * 12f, 22f, 1.2f)
    r(0xFF90A4AE, x + 110f, 42f, 18f, 5f)
    r(0xFF78909C, x + 114f, 38f, 10f, 5f)
    for (i in 0..4) r(0xFF8D6E63, x + 140f + i * 20f, 96f, 1.6f, 12f)
    r(0xFF8D6E63, x + 140f, 99f, 82f, 1.2f)
    r(0xFF8D6E63, x + 140f, 103.5f, 82f, 1.2f)
    r(0xFF5D4037, x + 150f, 74f, 3f, 30f)
    r(0xFF5D4037, x + 188f, 74f, 3f, 30f)
    r(0xFF5D4037, x + 147f, 71f, 47f, 3.5f)
    r(0xFFE8D5A6, x + 154f, 75f, 32f, 9f)
    label(p, if (v == 0) "FAZENDA BOA VISTA" else "FAZENDA SOL NASCENTE", x + 170f, 80.5f, cam, s, 3.1f, 0xFF3E2723.toInt())
    label(p, "ENTRADA PROIBIDA", x + 170f, 83.4f, cam, s, 2.4f, 0xFFB71C1C.toInt())
    for ((bx, by) in listOf(116f to 101f, 134f to 101f)) {
        drawCircle(Color(0xFFD9B44A), 6f * s, Offset((x + bx - cam) * s, by * s))
        drawCircle(Color(0xFFB8942F), 3.8f * s, Offset((x + bx - cam) * s, by * s))
    }
    val ph = t * 0.2f + v * 2f
    val fx = x + 150f + sin(ph) * 60f
    drawChar(npcLookFor(if (v == 0) "Seu Jorge" else "Marcos"), (fx - 6f - cam) * s, (114f - 26f) * s, s, cos(ph) < 0f, 1f, t * 5f)
}

private fun DrawScope.rGate(x: Float, cam: Float, s: Float, p: Paint) {
    fun r(c: Long, ax: Float, ay: Float, aw: Float, ah: Float) = wr(Color(c), ax, ay, aw, ah, cam, s)
    r(0xFF6D4C41, x + 4f, 60f, 4f, 44f)
    r(0xFF6D4C41, x + 62f, 60f, 4f, 44f)
    r(0xFF5D4037, x, 56f, 70f, 5f)
    r(0xFFE8D5A6, x + 9f, 62f, 52f, 14f)
    r(0xFF5D4037, x + 9f, 62f, 52f, 0.8f)
    label(p, "VILA SOSSEGO", x + 35f, 69.4f, cam, s, 5f, 0xFF3E2723.toInt())
    label(p, "Bem-vindo!", x + 35f, 74.4f, cam, s, 3.2f, 0xFFB71C1C.toInt())
    r(0xFFFFC107, x + 9f, 77f, 3f, 4f)
    r(0xFFFFC107, x + 58f, 77f, 3f, 4f)
}

private fun DrawScope.rEdge(cam: Float, s: Float, t: Float, p: Paint) {
    val ex = WORLD_START
    if (cam > ex + 260f) return
    wr(Color(0xFF1E5226), ex - 60f, 62f, 215f, 46f, cam, s)
    for (i in 0 until 13) rTree(ex - 18f + i * 12f, i * 3, cam, s, t)
    val bx = ex + 168f
    wr(Color(0xFF6D4C41), bx + 11f, 92f, 1.8f, 16f, cam, s)
    wr(Color(0xFFE8D5A6), bx - 6f, 82f, 36f, 14f, cam, s)
    wr(Color(0xFF5D4037), bx - 6f, 82f, 36f, 0.8f, cam, s)
    label(p, "ESTRADA FECHADA", bx + 12f, 88f, cam, s, 3.4f, 0xFFB71C1C.toInt())
    label(p, "MATA DENSA", bx + 12f, 92.6f, cam, s, 3.4f, 0xFF3E2723.toInt())
}

// -------------------------------------------------------------------------------------
//  BAR DO ZÉ E VENDA DA DONA CIDA
// -------------------------------------------------------------------------------------

class MenuItem(val name: String, val price: Int, val hunger: Double, val thirst: Double, val minutes: Int, val say: String)

private val BAR_MENU = listOf(
    MenuItem("Guaraná gelado", 5, 0.0, 25.0, 10, "Guaraná gelado, daquele que arde o nariz."),
    MenuItem("Cerveja gelada", 8, 0.0, 15.0, 15, "Uma gelada com os amigos... tá liberado."),
    MenuItem("Pastel de carne", 7, 20.0, 0.0, 10, "Pastel quentinho, feito agora."),
    MenuItem("Coxinha", 6, 15.0, 0.0, 10, "A coxinha do Zé é famosa na vila."),
    MenuItem("Pão com ovo", 6, 18.0, 0.0, 10, "Simples e bom."),
    MenuItem("Porção de torresmo", 18, 35.0, -5.0, 25, "Torresmo sequinho. Dá uma sede, hein!"),
)

private val VENDA_MENU = listOf(
    MenuItem("Pão francês", 3, 10.0, 0.0, 5, "Pão fresquinho da manhã."),
    MenuItem("Banana", 2, 8.0, 0.0, 5, "Banana do quintal da Dona Cida."),
    MenuItem("Leite fresco", 4, 5.0, 15.0, 5, "Leite tirado hoje cedo."),
    MenuItem("Queijo minas", 9, 20.0, 0.0, 5, "Queijo caseiro, só aqui."),
    MenuItem("Ovo cozido", 5, 12.0, 0.0, 5, "Ovo da galinha caipira."),
    MenuItem("Água de moringa", 3, 0.0, 25.0, 5, "Geladinha, da moringa de barro."),
)

private val BAR_TALK = listOf(
    "Dizem que o carro velho aqui do interior sai bem mais barato.",
    "O trator do Seu Jorge já passou de 40 anos e não morre nunca.",
    "Quando chove, a estrada de terra vira um lamaçal. Só trator passa.",
    "Tem festa junina na igrejinha mês que vem.",
    "A vila é pequena, mas o povo é bom. Todo mundo se conhece.",
)

@Composable
fun RuralShopDialog(g: GameState, title: String, items: List<MenuItem>, open: Boolean, hours: String, bar: Boolean, onClose: () -> Unit) {
    var info by remember { mutableStateOf(if (bar) "Fala, freguês! O que vai ser hoje?" else "Bom dia! Pode olhar à vontade.") }
    FullDialog(title, g, onClose) {
        if (!open) Text("Fechado agora. Atendimento: $hours.", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
        Text("“$info”", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
        Text("Fome ${g.hunger.toInt()}%   Sede ${g.thirst.toInt()}%", fontSize = 12.sp)
        for (it2 in items) {
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(it2.name, Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = {
                            g.money -= it2.price
                            g.hunger = min(100.0, g.hunger + it2.hunger)
                            g.thirst = (g.thirst + it2.thirst).coerceIn(0.0, 100.0)
                            g.spendTime(it2.minutes)
                            g.save()
                            info = it2.say
                        },
                        enabled = open && g.money >= it2.price,
                    ) { Text(brl(it2.price)) }
                }
            }
        }
        if (bar) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Joguinhos do bar", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    OutlinedButton(
                        onClick = {
                            g.money -= 20
                            g.spendTime(30)
                            if (Random.nextInt(100) < 45) { g.money += 40; info = "Você ganhou na sinuca! +${brl(20)} de lucro." }
                            else info = "Perdeu na sinuca... foi só ${brl(20)}."
                            g.save()
                        },
                        modifier = Modifier.fillMaxWidth(), enabled = open && g.money >= 20,
                    ) { Text("🎱 Sinuca valendo ${brl(20)}") }
                    OutlinedButton(
                        onClick = { g.spendTime(15); info = BAR_TALK.random() },
                        modifier = Modifier.fillMaxWidth(), enabled = open,
                    ) { Text("🗣 Bater um papo com o pessoal (15 min)") }
                }
            }
        }
    }
}

val BAR_MENU_ITEMS: List<MenuItem> get() = BAR_MENU
val VENDA_MENU_ITEMS: List<MenuItem> get() = VENDA_MENU
