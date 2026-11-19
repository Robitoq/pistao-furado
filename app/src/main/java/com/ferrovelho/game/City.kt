package com.ferrovelho.game

import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

// ------------------------------------------------------------ mundo

private const val WORLD = WORLD_END
const val VIEW_W = 190f
private val YARD_X = listOf(520f, 1450f, 2450f, 3500f, 4450f)
private const val HOME_X = 260f     // casa do personagem
private const val MEET_X = 2000f    // encontro de carros
private val NEW_PLACES = listOf(HOME_X, MEET_X, GARAGE_X) + STORE_X + COMPANY_X + LOJA_X + DRAG_X
private val PLACE_X = YARD_X + NEW_PLACES

val CSKIN = listOf(0xFFFFDBB4, 0xFFF1C27D, 0xFFE0AC69, 0xFFC68642, 0xFF8D5524, 0xFF6B4423, 0xFF4A2C17).map { Color(it) }
val CHAIR = listOf(0xFF1B1B1B, 0xFF4E342E, 0xFF8D6E63, 0xFFE0B04F, 0xFFB71C1C, 0xFF9E9E9E, 0xFFF5F5F5, 0xFF1E88E5).map { Color(it) }
val CTOP = listOf(0xFF1E88E5, 0xFFE53935, 0xFF43A047, 0xFFFDD835, 0xFF8E24AA, 0xFFFB8C00, 0xFFECEFF1, 0xFF212121, 0xFF00ACC1, 0xFFF06292).map { Color(it) }
val CBOT = listOf(0xFF283593, 0xFF37474F, 0xFF5D4037, 0xFF212121, 0xFF1565C0, 0xFF6B8E23).map { Color(it) }
val CSHOE = listOf(0xFF212121, 0xFFFAFAFA, 0xFFB71C1C, 0xFF5D4037).map { Color(it) }
private val HAIR_STYLES = listOf("Curto", "Longo", "Topete", "Careca", "Rabo", "Boné")
private val TOP_TYPES = listOf("Camiseta", "Regata", "Jaqueta")
private val BOT_TYPES = listOf("Calça", "Bermuda", "Saia")

class Look(
    val female: Boolean, val skin: Int, val hairStyle: Int, val hair: Int,
    val topType: Int, val top: Int, val botType: Int, val bottom: Int, val shoes: Int, val glasses: Boolean,
)

fun randomLook(r: Random) = Look(
    r.nextBoolean(), r.nextInt(CSKIN.size), r.nextInt(HAIR_STYLES.size), r.nextInt(CHAIR.size),
    r.nextInt(3), r.nextInt(CTOP.size), r.nextInt(2), r.nextInt(CBOT.size), r.nextInt(CSHOE.size), r.nextInt(5) == 0,
)

class PlayerProfile(val name: String, val look: Look)

fun loadProfile(p: SharedPreferences): PlayerProfile? {
    val n = p.getString("name", null) ?: return null
    val f = p.getBoolean("female", false)
    return PlayerProfile(n, Look(
        f, p.getInt("skin", 1).coerceIn(0, CSKIN.lastIndex), p.getInt("hairStyle", 0).coerceIn(0, HAIR_STYLES.lastIndex),
        p.getInt("hair", 0).coerceIn(0, CHAIR.lastIndex), p.getInt("topType", 0).coerceIn(0, 2),
        p.getInt("shirt", 0).coerceIn(0, CTOP.lastIndex), p.getInt("botType", 0).coerceIn(0, 2),
        p.getInt("pants", 0).coerceIn(0, CBOT.lastIndex), p.getInt("shoes", 0).coerceIn(0, CSHOE.lastIndex),
        p.getBoolean("glasses", false),
    ))
}

private fun saveProfile(p: SharedPreferences, c: PlayerProfile) {
    val l = c.look
    p.edit().putString("name", c.name).putBoolean("female", l.female).putInt("skin", l.skin)
        .putInt("hairStyle", l.hairStyle).putInt("hair", l.hair).putInt("topType", l.topType).putInt("shirt", l.top)
        .putInt("botType", l.botType).putInt("pants", l.bottom).putInt("shoes", l.shoes)
        .putBoolean("glasses", l.glasses).apply()
}

class CitySprites(
    val bld: List<ImageBitmap>, val tree: List<ImageBitmap>, val lamp: ImageBitmap,
    val sign: List<ImageBitmap>, val cars: List<ImageBitmap>, val win: List<ImageBitmap>,
    val tl: ImageBitmap, val bench: ImageBitmap,
) {
    companion object {
        private var cache: CitySprites? = null
        fun get(ctx: Context): CitySprites = cache ?: load(ctx).also { cache = it }
        fun load(ctx: Context): CitySprites {
            val o = BitmapFactory.Options().apply { inScaled = false }
            fun img(n: String): ImageBitmap = try {
                ctx.assets.open("city/$n.png").use { BitmapFactory.decodeStream(it, null, o)!!.asImageBitmap() }
            } catch (e: Exception) {
                android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888).asImageBitmap()
            }
            return CitySprites(
                List(12) { img("b$it") }, List(2) { img("t$it") }, img("lamp"),
                List(3) { img("sign$it") }, List(9) { img("car${it + 1}") }, List(12) { img("w$it") },
                img("tl"), img("bench"),
            )
        }
    }
}

fun carSpriteFor(b: Body): Int = when (b) {
    Body.BOXY -> 0; Body.COMPACT -> 1; Body.WAGON -> 2; Body.MPV -> 4; Body.HATCH -> 1
    Body.SEDAN -> 5; Body.SUV -> 6; Body.PICKUP -> 8; Body.COUPE -> 3; Body.CLASSIC -> 7
    Body.VAN -> 4; Body.BEETLE -> 1; Body.SUPER -> 3
    Body.UNO -> 0; Body.GOL -> 1; Body.NOTCH -> 5; Body.OPALA -> 7; Body.MUSCLE -> 3
    Body.P911 -> 3; Body.JEEP -> 6; Body.MINI -> 1
    else -> 5
}

// centros das rodas (x1, x2, raio) de cada sprite de carro
val CAR_WHEELS = listOf(
    Triple(16f, 52f, 4.5f), Triple(12f, 43f, 5f), Triple(16f, 52f, 4.5f), Triple(15.5f, 47.5f, 4.5f),
    Triple(16f, 47f, 5f), Triple(18f, 54f, 5f), Triple(12f, 46f, 5f), Triple(17f, 50f, 4.5f), Triple(11f, 38f, 5f),
)

// kind: 0 prédio, 1 árvore, 2 poste, 3 placa, 4 semáforo, 5 banco
private class Item(val kind: Int, val idx: Int, val x: Float)
private class Ped(val look: Look, val base: Float, val phase: Float)
private class Traffic(val spr: Int, val x0: Float, val speed: Float, val lane: Int, val driver: Look)

private fun genCity(spr: CitySprites): List<Item> {
    val r = Random(11)
    val out = ArrayList<Item>()
    var x = 20f
    while (x < CITY_END - 100f) {
        val yx = PLACE_X.firstOrNull { x + 80f > it - 110f && x < it + 110f }
        if (yx != null) { x = yx + 110f; continue }
        if (r.nextInt(100) < 18) {
            out.add(Item(1, r.nextInt(2), x + 10f)); out.add(Item(1, r.nextInt(2), x + 45f))
            if (r.nextBoolean()) out.add(Item(3, r.nextInt(3), x + 70f))
            x += 100f
        } else {
            val i = r.nextInt(12)
            out.add(Item(0, i, x))
            x += spr.bld[i].width + r.nextInt(4, 24)
        }
    }
    var lx = 60f
    while (lx < WORLD) { out.add(Item(2, 0, lx)); lx += 95f + r.nextInt(55) }
    var k = 0
    while (350f + 700f * k < CITY_END) { out.add(Item(4, 0, 350f + 700f * k)); k++ }
    k = 0
    while (200f + 430f * k < WORLD) { out.add(Item(5, 0, 200f + 430f * k)); k++ }
    // lixeira, saco de lixo, gato e vaso espalhados pela calçada
    var pxx = 90f
    while (pxx < CITY_END - 60f) {
        pxx += 55f + r.nextInt(110)
        if (PLACE_X.any { abs(pxx - it) < 100f }) continue
        out.add(Item(6 + r.nextInt(4), r.nextInt(2), pxx))
    }
    // sem banco nem semáforo em cima da casa e do encontro
    out.removeAll { (it.kind == 4 || it.kind == 5) && NEW_PLACES.any { p -> abs(it.x - p) < 75f } }
    return out
}

/** Número "aleatório" estável a partir de um inteiro (pra variar o cenário sem piscar). */
fun hsh(a: Int): Int {
    var x = a * -1640531535
    x = x xor (x ushr 15)
    x *= 0x2c1b3c6d
    x = x xor (x ushr 12)
    return x and 0x7fffffff
}

fun nightF(m: Int): Float = when {
    m < 5 * 60 -> 1f
    m < 7 * 60 -> (7 * 60 - m) / 120f
    m < 18 * 60 -> 0f
    m < 20 * 60 -> (m - 18 * 60) / 120f
    else -> 1f
}

// ------------------------------------------------------------ desenho

fun DrawScope.wr(c: Color, x: Float, y: Float, w: Float, h: Float, cam: Float, s: Float) {
    drawRect(c, Offset((x - cam) * s, y * s), Size(w * s, h * s))
}

fun DrawScope.drawWheels(ci: Int, left: Float, top: Float, u: Float, h: Float, rot: Float, sport: Boolean = false) {
    val wh = CAR_WHEELS[ci.coerceIn(0, CAR_WHEELS.lastIndex)]
    for (cx in listOf(wh.first, wh.second)) {
        val c = Offset(left + cx * u, top + (h - 5.5f) * u)
        val r = wh.third * u
        drawCircle(Color(0xFF15161A), r, c)
        drawCircle(if (sport) Color(0xFFFFC107) else Color(0xFF8E9399), r * (if (sport) 0.75f else 0.62f), c)
        if (sport) drawCircle(Color(0xFF3A2E00), r * 0.58f, c)
        val n = if (sport) 6 else 5
        for (j in 0 until n) {
            val a = rot + j * (6.2832f / n)
            drawLine(if (sport) Color(0xFFFFE082) else Color(0xFF4A4F57), c, Offset(c.x + cos(a) * r * 0.62f, c.y + sin(a) * r * 0.62f), r * (if (sport) 0.2f else 0.16f))
        }
        drawCircle(if (sport) Color(0xFF5D4B00) else Color(0xFFD7DBE0), r * 0.2f, c)
    }
}

// posição da cabeça do motorista em cada sprite (x do centro, y do topo), na janela
val DRIVER_POS_PUBLIC get() = DRIVER_POS
private val DRIVER_POS = listOf(
    33f to 2.8f, 27f to 3f, 34f to 3.2f, 33f to 3.6f, 30f to 3f, 33f to 4f, 30f to 7.5f, 32f to 4f, 25f to 3.2f,
)

fun DrawScope.drawDriver(l: Look, ci: Int, left: Float, top: Float, u: Float) {
    val p = DRIVER_POS[ci.coerceIn(0, DRIVER_POS.lastIndex)]
    val x = left + (p.first - 2f) * u
    val y = top + p.second * u
    val dark = Color(0xFF212121)
    drawRect(CTOP[l.top], Offset(x - 0.8f * u, y + 4f * u), Size(5.6f * u, 2f * u))
    drawRect(CSKIN[l.skin], Offset(x + 1.2f * u, y + 3.6f * u), Size(1.6f * u, 0.6f * u))
    drawRect(CSKIN[l.skin], Offset(x, y), Size(4f * u, 4f * u))
    when (l.hairStyle) {
        3 -> {}
        5 -> {
            drawRect(CTOP[l.top], Offset(x - 0.2f * u, y - 0.8f * u), Size(4.4f * u, 1.6f * u))
            drawRect(dark, Offset(x + 3.4f * u, y + 0.2f * u), Size(1.8f * u, 0.6f * u))
        }
        else -> drawRect(CHAIR[l.hair], Offset(x - 0.2f * u, y - 0.8f * u), Size(4.4f * u, 1.8f * u))
    }
    if (l.hairStyle == 1) drawRect(CHAIR[l.hair], Offset(x - 0.6f * u, y), Size(1f * u, 4.6f * u))
    drawRect(dark, Offset(x + 2.6f * u, y + 1.7f * u), Size(0.8f * u, 1f * u))
    if (l.glasses) drawRect(dark, Offset(x + 2f * u, y + 1.4f * u), Size(2.2f * u, 0.4f * u))
}

fun DrawScope.sp(
    img: ImageBitmap, wx: Float, by: Float, cam: Float, s: Float,
    k: Float = 1f, flip: Boolean = false, tint: Color? = null, alpha: Float = 1f,
    carIdx: Int = -1, rot: Float = 0f, driver: Look? = null,
) {
    val w = img.width * k * s
    val h = img.height * k * s
    val dx = (wx - cam) * s
    val dy = by * s - h
    if (dx + w < 0f || dx > size.width) return
    val filter = tint?.let { ColorFilter.tint(it, BlendMode.SrcAtop) }
    val body: DrawScope.() -> Unit = {
        drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), IntOffset(dx.toInt(), dy.toInt()),
            IntSize(w.toInt().coerceAtLeast(1), h.toInt().coerceAtLeast(1)), alpha,
            androidx.compose.ui.graphics.drawscope.Fill, filter, BlendMode.SrcOver, FilterQuality.None)
        if (carIdx >= 0) drawWheels(carIdx, dx, dy, k * s, img.height.toFloat(), rot)
        if (driver != null && carIdx >= 0) drawDriver(driver, carIdx, dx, dy, k * s)
    }
    if (flip) withTransform({ scale(-1f, 1f, Offset(dx + w / 2f, dy)) }) { body() } else body()
}

/** Boneco 12x26 unidades. left/top em px, c = px por unidade. walk 0..1 (andando), ph = fase da animação. */
fun DrawScope.drawChar(l: Look, left: Float, top: Float, c: Float, faceLeft: Boolean, walk: Float, ph: Float) {
    val skin = CSKIN[l.skin]
    val hairC = CHAIR[l.hair]
    val topC = CTOP[l.top]
    val botC = CBOT[l.bottom]
    val shoeC = CSHOE[l.shoes]
    val sw = sin(ph) * walk
    val bob = if (walk > 0.1f) abs(sin(ph)) * 0.9f else sin(ph * 0.3f) * 0.25f
    val liftL = max(0f, sw) * 2.2f
    val liftR = max(0f, -sw) * 2.2f
    fun r(col: Color, x: Float, y: Float, w: Float, h: Float, b: Float = bob) {
        val xx = if (faceLeft) 12f - x - w else x
        drawRect(col, Offset(left + xx * c, top + (y - b) * c), Size(w * c + 0.6f, h * c + 0.6f))
    }
    drawOval(Color(0x44000000), Offset(left + 1f * c, top + 24.3f * c), Size(10f * c, 2.2f * c))
    val bot = if (!l.female && l.botType == 2) 0 else l.botType
    // cabelo de trás
    if (l.hairStyle == 1) r(hairC, 2.2f, 1f, 7.6f, 11f)
    if (l.hairStyle == 4) r(hairC, 1f, 2.5f, 1.8f, 6f)
    // pernas
    val legs = listOf(3.2f to liftL, 6f to liftR)
    for ((lx, lf) in legs) {
        when (bot) {
            0 -> r(botC, lx, 17f, 2.8f, 6f - lf, 0f)
            1 -> { r(botC, lx, 17f, 2.8f, 3.5f, 0f); r(skin, lx, 20.5f, 2.8f, 2.5f - lf, 0f) }
            else -> r(skin, lx, 19f, 2.8f, 4f - lf, 0f)
        }
        r(shoeC, lx - 0.2f, 23f - lf, 3.2f, 2f, 0f)
    }
    if (bot == 2) r(botC, 2.6f, 16.5f, 6.8f, 4f)
    // braços
    val sleeve = when (l.topType) { 0 -> 3f; 1 -> 0f; else -> 7f }
    for ((ax, off) in listOf(1.2f to sw * 1.6f, 9f to -sw * 1.6f)) {
        r(skin, ax, 9.3f + off, 1.8f, 7.5f)
        if (sleeve > 0f) r(topC, ax, 9.3f + off, 1.8f, sleeve)
    }
    // tronco
    val tx = if (l.female) 3.3f else 3f
    val tw = if (l.female) 5.4f else 6f
    r(topC, tx, 9f, tw, 8f)
    if (l.topType == 2) { r(Color(0x55000000), 5.8f, 9f, 0.4f, 8f); r(Color(0x33FFFFFF), tx, 9f, tw, 1.2f) }
    if (bot != 2) r(Color(0xFF212121), tx, 16.3f, tw, 0.8f)
    r(skin, 5f, 8f, 2f, 1.3f)
    // cabeça
    r(skin, 3f, 2f, 6f, 6.2f)
    r(Color(0xFF212121), 4.5f, 4.7f, 1.1f, 1.4f)
    r(Color(0xFF212121), 6.9f, 4.7f, 1.1f, 1.4f)
    r(if (l.female) Color(0xFFC2185B) else Color(0xFF8D4B3B), 5.6f, 6.7f, 1.8f, 0.5f)
    when (l.hairStyle) {
        0, 4 -> { r(hairC, 2.6f, 0.8f, 6.8f, 2.4f); r(hairC, 2.6f, 2.6f, 1f, 2.2f); r(hairC, 8.4f, 2.6f, 1f, 2.2f) }
        1 -> r(hairC, 2.6f, 0.8f, 6.8f, 2.6f)
        2 -> { r(hairC, 4f, -1.5f, 4f, 3.5f); r(hairC, 2.8f, 1.6f, 6.4f, 1.4f) }
        5 -> {
            r(hairC, 2.8f, 3f, 0.9f, 1.6f)
            r(topC, 2.6f, 0.8f, 6.8f, 2.4f)
            r(Color(0xFF212121), 7.5f, 2.4f, 3.2f, 0.9f)
        }
        else -> {}
    }
    if (l.glasses) {
        r(Color(0xFF212121), 4f, 4.4f, 5f, 0.5f)
        r(Color(0x5590CAF9), 4.2f, 4.9f, 2.2f, 1.3f)
        r(Color(0x5590CAF9), 6.7f, 4.9f, 2.2f, 1.3f)
    }
}

fun DrawScope.label(p: Paint, txt: String, wx: Float, y: Float, cam: Float, s: Float, sz: Float, col: Int) {
    p.textSize = sz * s
    p.textAlign = Paint.Align.CENTER
    val nc = drawContext.canvas.nativeCanvas
    if (col == 0xFFFFFFFF.toInt()) {   // texto branco ganha sombra, senão some em cima de prédio claro
        p.color = 0xCC000000.toInt()
        nc.drawText(txt, (wx - cam) * s + 0.5f * s, y * s + 0.5f * s, p)
    }
    p.color = col
    nc.drawText(txt, (wx - cam) * s, y * s, p)
}

private val IDLE_LINES = listOf(
    "Será que hoje acho um carro bom?", "Preciso de grana pra oficina...",
    "Esse bairro tem cada carro...", "Dizem que o Seu Zé pechincha fácil.",
    "Um carro baratinho e eu faço fortuna!", "Tá um dia bonito pra negociar.",
    "Se esse motor pegar no tranco, tá no lucro.", "Que cheiro de pão quentinho...",
    "Será que aquele Opala ainda tá lá?", "Hoje eu fecho negócio, só falta fé.",
)
private val PED_LINES = listOf(
    "Oi, {n}!", "Bom dia! Já tomou café?", "Cuidado na rua, tem moto voando!", "Tá procurando carro?",
    "Fala, {n}! Firmeza?", "Eita, que calor, hein?", "Passa lá no Zé, ele tá com uns bons.",
    "Tá tudo caro hoje em dia...", "Ô {n}, vê se não compra lata velha!", "Tô atrasado! Licença!",
)

private fun DrawScope.bubble(p: Paint, txt: String, wx: Float, bottomY: Float, cam: Float, s: Float) {
    p.textSize = 5.2f * s
    val tw = p.measureText(txt) / s
    val bw = tw + 6f
    // o balão não sai da tela; o biquinho continua apontando pra quem fala
    val cxw = wx.coerceIn(cam + bw / 2f + 1.5f, cam + VIEW_W - bw / 2f - 1.5f)
    val x = cxw - bw / 2f
    val tail = wx.coerceIn(x + 3f, x + bw - 3f)
    wr(Color(0xFF212121), x - 0.6f, bottomY - 10.6f, bw + 1.2f, 10.2f, cam, s)
    wr(Color(0xFFFFFFFF), x, bottomY - 10f, bw, 9f, cam, s)
    wr(Color(0xFFFFFFFF), tail - 1.5f, bottomY - 1f, 3f, 2f, cam, s)
    label(p, txt, cxw, bottomY - 3.4f, cam, s, 5.2f, 0xFF212121.toInt())
}

fun DrawScope.glow(center: Offset, radius: Float, c: Color) {
    drawCircle(
        Brush.radialGradient(listOf(c, c.copy(alpha = 0f)), center, radius), radius, center,
        1f, androidx.compose.ui.graphics.drawscope.Fill, null, BlendMode.Plus,
    )
}

// ------------------------------------------------------------ casa e encontro de carros

/** Casa do personagem (desenhada no mundo, chão em y = 104). */
private fun DrawScope.drawHome(cam: Float, s: Float, t: Float, night: Float, p: Paint) {
    val cx = HOME_X
    if (cx + 70f < cam || cx - 70f > cam + VIEW_W) return
    val hx = cx - 36f
    wr(Color(0x33000000), hx, 101f, 72f, 3f, cam, s)
    // chaminé com fumaça
    wr(Color(0xFF8D6E63), cx + 14f, 36f, 7f, 18f, cam, s)
    wr(Color(0xFF6D4C41), cx + 13f, 35f, 9f, 2f, cam, s)
    for (k in 0..2) {
        val ph = (t * 0.5f + k * 0.33f) % 1f
        drawCircle(Color.White.copy(alpha = 0.5f * (1f - ph)), (1.6f + ph * 2.4f) * s, Offset((cx + 17.5f + ph * 4f - cam) * s, (33f - ph * 16f) * s))
    }
    // paredes
    wr(Color(0xFFF2E3C9), hx, 66f, 72f, 38f, cam, s)
    wr(Color(0xFFD9C3A0), hx, 98f, 72f, 6f, cam, s)
    wr(Color(0x14000000), hx, 66f, 72f, 3f, cam, s)
    // telhado
    val roof = Path().apply {
        moveTo((hx - 6f - cam) * s, 66f * s)
        lineTo((cx - cam) * s, 42f * s)
        lineTo((hx + 78f - cam) * s, 66f * s)
        close()
    }
    drawPath(roof, Color(0xFFB23A2E))
    wr(Color(0xFF7F2A21), hx - 6f, 65f, 84f, 2.4f, cam, s)
    // janelas
    for (wx in listOf(hx + 7f, hx + 49f)) {
        wr(Color(0xFF5D4037), wx - 1f, 75f, 18f, 16f, cam, s)
        wr(lerp(Color(0xFF90CAF9), Color(0xFF2A3350), night), wx, 76f, 16f, 14f, cam, s)
        wr(Color(0xFF5D4037), wx + 7.5f, 76f, 1f, 14f, cam, s)
        wr(Color(0xFF5D4037), wx, 82.5f, 16f, 1f, cam, s)
        wr(Color(0xFF8D6E63), wx - 2f, 91f, 20f, 2f, cam, s)
    }
    // porta e plaquinha
    wr(Color(0xFF3E2723), cx - 7f, 79f, 14f, 25f, cam, s)
    wr(Color(0xFF6D4C41), cx - 6f, 80f, 12f, 24f, cam, s)
    wr(Color(0xFF5D4037), cx - 4f, 82f, 8f, 8f, cam, s)
    wr(Color(0xFFFFD54F), cx + 3f, 92f, 1.4f, 1.4f, cam, s)
    wr(Color(0xFF9E9E9E), cx - 10f, 102.5f, 20f, 1.5f, cam, s)
    label(p, "CASA", cx, 76.5f, cam, s, 5.5f, 0xFF5D4037.toInt())
    // arbustos com flor e caixa de correio
    wr(Color(0xFF2E7D32), hx - 8f, 97f, 9f, 7f, cam, s)
    wr(Color(0xFF43A047), hx - 6f, 95f, 5f, 4f, cam, s)
    wr(Color(0xFFE91E63), hx - 5f, 97f, 1.2f, 1.2f, cam, s)
    wr(Color(0xFF2E7D32), hx + 71f, 97f, 9f, 7f, cam, s)
    wr(Color(0xFFFFEB3B), hx + 75f, 96f, 1.2f, 1.2f, cam, s)
    wr(Color(0xFF5D4037), cx + 49f, 95f, 1.2f, 9f, cam, s)
    wr(Color(0xFFC62828), cx + 46.5f, 91f, 6.5f, 4.5f, cam, s)
}

/** Luzes da casa à noite (por cima da escuridão). */
private fun DrawScope.drawHomeLit(cam: Float, s: Float, night: Float) {
    val cx = HOME_X
    if (cx + 70f < cam || cx - 70f > cam + VIEW_W) return
    val hx = cx - 36f
    for (wx in listOf(hx + 7f, hx + 49f)) {
        wr(Color(0xFFFFD27A).copy(alpha = night), wx, 76f, 16f, 14f, cam, s)
        wr(Color(0xFFFFB74D).copy(alpha = 0.55f * night), wx, 76f, 16f, 5f, cam, s)
        wr(Color(0xFF5D4037).copy(alpha = night), wx + 7.5f, 76f, 1f, 14f, cam, s)
        wr(Color(0xFF5D4037).copy(alpha = night), wx, 82.5f, 16f, 1f, cam, s)
        glow(Offset((wx + 8f - cam) * s, 83f * s), 22f * s, Color(0xFFFFD27A).copy(alpha = 0.35f * night))
    }
    // luz da varanda
    wr(Color(0xFFFFF59D).copy(alpha = night), cx - 12f, 80f, 2f, 3f, cam, s)
    glow(Offset((cx - 11f - cam) * s, 82f * s), 12f * s, Color(0xFFFFF59D).copy(alpha = 0.6f * night))
    glow(Offset((cx - cam) * s, 106f * s), 18f * s, Color(0xFFFFE082).copy(alpha = 0.3f * night))
}

private val BULB_COLORS = listOf(0xFFFF5252, 0xFFFFEB3B, 0xFF69F0AE, 0xFF40C4FF)

/** Terreno do encontro de carros: muro com grafite, vagas, placa e pisca-pisca. */
private fun DrawScope.drawMeetLot(cam: Float, s: Float, t: Float, night: Float, open: Boolean, p: Paint) {
    val cx = MEET_X
    if (cx + 110f < cam || cx - 110f > cam + VIEW_W) return
    // muro de fundo com grafite
    wr(Color(0xFF455A64), cx - 100f, 74f, 200f, 30f, cam, s)
    wr(Color(0xFF37474F), cx - 100f, 74f, 200f, 3f, cam, s)
    val gr = listOf(0xFFEF5350, 0xFFFFCA28, 0xFF42A5F5, 0xFF66BB6A, 0xFFAB47BC)
    for (k in 0..9) wr(Color(gr[k % 5]).copy(alpha = 0.55f), cx - 96f + k * 20f, 84f + (k % 3) * 3f, 8f + (k % 4) * 2f, 5f, cam, s)
    // piso com vagas
    wr(Color(0xFF8E9DA5), cx - 100f, 104f, 200f, 14f, cam, s)
    for (k in 0..3) wr(Color(0xFFFAFAFA).copy(alpha = 0.7f), cx - 98f + k * 66f, 105f, 1.2f, 12f, cam, s)
    // placa
    wr(Color(0xFF212121), cx - 40f, 62f, 2.5f, 13f, cam, s)
    wr(Color(0xFF212121), cx + 37.5f, 62f, 2.5f, 13f, cam, s)
    wr(Color(0xFF212121), cx - 56f, 34f, 112f, 30f, cam, s)
    wr(Color(0xFFFF5722), cx - 54f, 36f, 108f, 26f, cam, s)
    label(p, "ENCONTRO DE CARROS", cx, 48f, cam, s, 7.5f, 0xFFFFFFFF.toInt())
    label(p, if (open) "ABERTO AGORA!" else "todo dia às 18h", cx, 58f, cam, s, 5.5f, 0xFFFFEB3B.toInt())
    // postes e fio de luzinhas
    wr(Color(0xFF424242), cx - 99f, 66f, 2f, 38f, cam, s)
    wr(Color(0xFF424242), cx + 97f, 66f, 2f, 38f, cam, s)
    val x0 = (cx - 98f - cam) * s
    val x1 = (cx + 97f - cam) * s
    val wire = Path().apply {
        moveTo(x0, 67f * s)
        quadraticBezierTo((x0 + x1) / 2f, 79f * s, x1, 67f * s)
    }
    drawPath(wire, Color(0xFF2B2B33), style = Stroke(width = 0.4f * s))
    for (k in 1..14) {
        val tt = k / 15f
        val bx = x0 + (x1 - x0) * tt
        val by = (((1f - tt) * (1f - tt) + tt * tt) * 67f + 2f * (1f - tt) * tt * 79f) * s
        val on = 0.45f + 0.55f * night
        drawRect(Color(BULB_COLORS[k % 4]).copy(alpha = on), Offset(bx - 0.7f * s, by), Size(1.4f * s, 1.8f * s))
    }
}

/** Brilho do encontro à noite (placa e luzinhas acesas por cima da escuridão). */
private fun DrawScope.drawMeetLit(cam: Float, s: Float, t: Float, night: Float, open: Boolean, p: Paint) {
    val cx = MEET_X
    if (cx + 110f < cam || cx - 110f > cam + VIEW_W) return
    wr(Color(0xFFFF7043).copy(alpha = night), cx - 54f, 36f, 108f, 26f, cam, s)
    label(p, "ENCONTRO DE CARROS", cx, 48f, cam, s, 7.5f, 0xFFFFFFFF.toInt())
    label(p, if (open) "ABERTO AGORA!" else "todo dia às 18h", cx, 58f, cam, s, 5.5f, 0xFFFFEB3B.toInt())
    glow(Offset((cx - cam) * s, 50f * s), 70f * s, Color(0xFFFF8A50).copy(alpha = 0.25f * night))
    val x0 = (cx - 98f - cam) * s
    val x1 = (cx + 97f - cam) * s
    for (k in 1..14) {
        val tt = k / 15f
        val bx = x0 + (x1 - x0) * tt
        val by = (((1f - tt) * (1f - tt) + tt * tt) * 67f + 2f * (1f - tt) * tt * 79f) * s
        val tw = 0.6f + 0.4f * sin(t * 3f + k)
        glow(Offset(bx, by + 1f * s), 5f * s, Color(BULB_COLORS[k % 4]).copy(alpha = 0.55f * night * tw))
    }
    glow(Offset((cx - cam) * s, 108f * s), 90f * s, Color(0xFFFFE082).copy(alpha = 0.12f * night))
}

// ------------------------------------------------------------ tela principal

@Composable
fun CityScreen(g: GameState) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("city", Context.MODE_PRIVATE) }
    var profile by remember { mutableStateOf(loadProfile(prefs)) }
    val p = profile
    if (p == null) {
        CharacterCreator { profile = it; saveProfile(prefs, it) }
    } else {
        val spr = remember { CitySprites.get(ctx) }
        CityWorld(g, p, spr, prefs) { profile = null }
    }
}

@Composable
private fun CityWorld(g: GameState, me: PlayerProfile, spr: CitySprites, prefs: SharedPreferences, onEdit: () -> Unit) {
    val city = remember(spr) { genCity(spr) }
    val lamps = remember(city) { city.filter { it.kind == 2 }.map { it.x } }
    val peds = remember {
        List(34) { val r = Random(it + 5); Ped(randomLook(r), r.nextFloat() * WORLD, r.nextFloat() * 6f) }
    }
    val traffic = remember {
        List(22) {
            val r = Random(it + 40)
            val lane = it % 2
            Traffic(r.nextInt(9), r.nextFloat() * WORLD, (25 + r.nextInt(25)) * (if (lane == 0) 1f else -1f), lane, randomLook(r))
        }
    }
    val stars = remember { val r = Random(3); List(70) { Offset(r.nextFloat(), r.nextFloat()) } }
    val paint = remember { Paint().apply { isAntiAlias = false; typeface = android.graphics.Typeface.DEFAULT_BOLD } }
    val look = me.look

    var px by remember { mutableFloatStateOf(prefs.getFloat("x", 400f)) }
    var time by remember { mutableFloatStateOf(0f) }
    var dir by remember { mutableIntStateOf(0) }
    var face by remember { mutableIntStateOf(1) }
    var run by remember { mutableStateOf(false) }
    var mask by remember { mutableIntStateOf(prefs.getInt("mask", 0)) }
    var talk by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }
    var taxi by remember { mutableStateOf(false) }
    var meetDlg by remember { mutableStateOf(false) }
    var homeDlg by remember { mutableStateOf(false) }
    var sleeping by remember { mutableStateOf(false) }
    var sleepTaxi by remember { mutableStateOf(false) }
    var lateAsk by remember { mutableStateOf(false) }
    var garageDlg by remember { mutableStateOf(false) }
    var garageListDlg by remember { mutableStateOf(false) }
    var summaryDlg by remember { mutableStateOf(false) }
    var bizDlg by remember { mutableStateOf<Int?>(null) }
    var driveId by remember { mutableStateOf<Int?>(null) }
    var zoneDlg by remember { mutableStateOf<Int?>(null) }
    var lojaDlg by remember { mutableStateOf<Int?>(null) }
    var driveDlg by remember { mutableStateOf(false) }
    var dragDlg by remember { mutableStateOf(false) }
    var vel by remember { mutableFloatStateOf(0f) }
    var odo by remember { mutableFloatStateOf(0f) }
    val engine = remember { EngineSound() }
    DisposableEffect(Unit) { onDispose { engine.release() } }
    var shopDlg by remember { mutableStateOf<Int?>(null) }
    var chatPed by remember { mutableStateOf<Int?>(null) }
    var sayHungry by remember { mutableStateOf(false) }
    var sayThirsty by remember { mutableStateOf(false) }
    var lateDay by remember { mutableIntStateOf(-1) }
    val meetField = remember(g.day) { genMeetField(g.day) }
    var say by remember { mutableStateOf<String?>("Bora achar uma pechincha!") }
    var sayUntil by remember { mutableFloatStateOf(4f) }
    val near by remember { derivedStateOf { YARD_X.indexOfFirst { abs(px - it) < 30f } } }
    val nearMeet by remember { derivedStateOf { abs(px - MEET_X) < 70f } }
    val nearHome by remember { derivedStateOf { abs(px - HOME_X) < 55f } }
    val nearGarage by remember { derivedStateOf { abs(px - GARAGE_X) < 60f } }
    val nearStore by remember { derivedStateOf { STORE_X.indexOfFirst { abs(px - it) < 40f } } }
    val nearLoja by remember { derivedStateOf { LOJA_X.indexOfFirst { abs(px - it) < 60f } } }
    val nearZone by remember { derivedStateOf { if (abs(px - PARK_X) < 60f) 0 else if (abs(px - BEACH_X) < 60f) 1 else -1 } }
    val nearDrag by remember { derivedStateOf { abs(px - DRAG_X) < 70f } }
    val nearBiz by remember { derivedStateOf { COMPANY_X.indexOfFirst { abs(px - it) < 60f } } }
    val nearPed by remember {
        derivedStateOf { peds.indices.firstOrNull { abs(peds[it].base + sin(time * 0.4f + peds[it].phase) * 60f - px) < 20f } ?: -1 }
    }

    // true enquanto alguma janela/cena está aberta (o personagem não anda)
    fun isBusy() = talk != null || taxi || meetDlg || homeDlg || sleeping || lateAsk || garageDlg || zoneDlg != null || lojaDlg != null || driveDlg || dragDlg || garageListDlg || summaryDlg || bizDlg != null || shopDlg != null || chatPed != null

    /** Táxi pra casa: se for de noite, já vai dormir. Sem dinheiro, o motorista leva mesmo assim. */
    fun rideHome() {
        val dist = abs(px - HOME_X)
        val fare = taxiFare(dist)
        val paid = minOf(fare, g.money)
        g.money -= paid
        g.spendTime(taxiMinutes(dist))
        px = HOME_X + 30f
        face = -1
        prefs.edit().putFloat("x", px).apply()
        if (paid < fare) g.message = "Sem dinheiro pro táxi? O motorista te deixou em casa mesmo assim."
        g.save()
        if (g.minutes >= SLEEP_FROM) {
            sleepTaxi = true
            sleeping = true
        } else {
            say = "Cheguei em casa!"
            sayUntil = time + 2.5f
        }
    }

    LaunchedEffect(say) { say?.let { Voice.speak(it, if (look.female) 1.3f else 0.95f) } }
    DisposableEffect(Unit) { onDispose { prefs.edit().putFloat("x", px).apply(); Voice.stop() } }
    LaunchedEffect(Unit) {
        var last = 0L
        var idle = 0f
        var lastNear = -1
        var lastPl = 0
        var saidNight = false
        while (true) {
            withFrameNanos { n ->
                if (last != 0L) {
                    val dt = ((n - last) / 1e9f).coerceAtMost(0.05f)
                    time += dt
                    idle = if (dir == 0) idle + dt else 0f
                    if (say != null && time > sayUntil) say = null
                    if (say == null && idle > 7f) { say = IDLE_LINES[Random.nextInt(IDLE_LINES.size)]; sayUntil = time + 3.5f; idle = 0f }
                    val nn = YARD_X.indexOfFirst { abs(px - it) < 45f }
                    if (nn != lastNear) {
                        lastNear = nn
                        if (nn >= 0) { say = "Olha o ${YARD_NAMES[nn]}!"; sayUntil = time + 3f }
                    }
                    val pl = if (abs(px - MEET_X) < 60f) 1 else if (abs(px - HOME_X) < 55f) 2 else if (abs(px - GARAGE_X) < 70f) 3 else if (STORE_X.any { abs(px - it) < 50f }) 4 else 0
                    if (pl != lastPl) {
                        lastPl = pl
                        if (pl == 1) {
                            say = if (g.minutes >= MEET_START) "O encontro tá rolando! Bora mostrar meu carro?" else "Encontro de carros só às 18h."
                            sayUntil = time + 4f
                        }
                        if (pl == 2) { say = "Minha casinha."; sayUntil = time + 2.5f }
                        if (pl == 3) { say = if (g.ownShop) "Minha empresa! Orgulho." else "Bora ver o que dá pra consertar!"; sayUntil = time + 3f }
                        if (pl == 4) { say = "Que cheirinho bom..."; sayUntil = time + 2.5f }
                    }
                    if (g.hunger < 30.0 && !sayHungry) { sayHungry = true; say = "Tô com fome... preciso comer algo."; sayUntil = time + 4f }
                    if (g.hunger > 50.0) sayHungry = false
                    if (g.thirst < 30.0 && !sayThirsty) { sayThirsty = true; say = "Que sede! Cadê uma água?"; sayUntil = time + 4f }
                    if (g.thirst > 50.0) sayThirsty = false
                    val nf = nightF(g.minutes)
                    if (nf > 0.6f && !saidNight) { saidNight = true; say = "Já escureceu! Encontro de carros ou casa?"; sayUntil = time + 4f }
                    if (nf < 0.2f) saidNight = false
                    // madrugada: o personagem apaga de sono e oferece o táxi pra casa
                    if (g.minutes >= 23 * 60 + 20 && lateDay != g.day && !isBusy()) { lateDay = g.day; lateAsk = true }
                    val dc = g.garage.firstOrNull { it.id == driveId }
                    if (dc != null) {
                        val maxV = carStreetSpeed(dc)
                        val busy = isBusy()
                        val go = dir != 0 && !busy
                        val tgt = if (go) dir * maxV else 0f
                        vel += (tgt - vel) * minOf(1f, 2.2f * dt)
                        if (busy) vel = 0f
                        if (abs(vel) > 2f) {
                            px = (px + vel * dt).coerceIn(20f, WORLD - 20f)
                            face = if (vel > 0f) 1 else -1
                            odo += abs(vel) * dt
                        }
                        if (Music.enabled && !busy) {
                            engine.start()
                            engine.set(38f + (abs(vel) / maxV).coerceIn(0f, 1f) * 130f, if (go) 0.8f else 0.35f)
                        } else {
                            engine.stop()
                        }
                    } else {
                        if (vel != 0f) vel = 0f
                        engine.stop()
                        if (dir != 0 && !isBusy()) {
                            px = (px + dir * (if (run) 135f else 75f) * (if (g.starving) 0.55f else 1f) * dt).coerceIn(20f, WORLD - 20f)
                            face = dir
                        }
                    }
                }
                last = n
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        NeedsBar(g)
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val s = size.width / VIEW_W
            val oy = size.height - 150f * s
            val cam = (px - VIEW_W / 2f).coerceIn(0f, WORLD - VIEW_W)
            val t = time
            val night = nightF(g.minutes)
            val moving = dir != 0 && !isBusy()
            val skyH = oy + 104f * s
            val meetOn = g.minutes >= 17 * 60
            val storesOpen = STORES.map { g.minutes in it.open until it.close }

            // prédios do fundo: larguras/alturas irregulares, antena, caixa d'água e janelas (algumas acesas)
            fun skyline() {
                for (layer in 0..1) {
                    val par = if (layer == 0) 0.3f else 0.5f
                    val step = if (layer == 0) 52f else 36f
                    val fc = cam * par
                    val baseCol = if (layer == 0) Color(0xFFBBD0E4) else Color(0xFF9DB7CC)
                    var i0 = (fc / step).toInt() - 1
                    while (i0 * step < fc + VIEW_W + step) {
                        val h1 = hsh(i0 * 7 + layer * 1000 + 3)
                        val bw = step - 5f - (h1 % 9)
                        val bx = i0 * step + (h1 / 9 % 6)
                        val hh = (if (layer == 0) 48f else 30f) + (h1 / 53 % 56)
                        val x0 = bx - fc
                        val top = 104f - hh
                        val col = lerp(baseCol, Color(0xFF8FA3B8), ((h1 / 7) % 3) * 0.12f)
                        drawRect(col, Offset(x0 * s, oy + top * s), Size(bw * s, hh * s))
                        when ((h1 / 3) % 5) {
                            0 -> drawRect(col, Offset((x0 + bw * 0.3f) * s, oy + (top - 5f) * s), Size(bw * 0.4f * s, 5f * s))
                            1 -> drawRect(col, Offset((x0 + bw * 0.6f) * s, oy + (top - 9f) * s), Size(0.8f * s, 9f * s))
                            else -> {}
                        }
                        val cols = (bw / 6f).toInt()
                        val rows = ((hh - 6f) / 7f).toInt()
                        for (cx in 0 until cols) for (ry in 0 until rows) {
                            val on = hsh(i0 * 131 + cx * 17 + ry * 29 + layer * 7) % 100 < 36
                            val wpx = (x0 + 2f + cx * 6f) * s
                            val wpy = oy + (top + 3f + ry * 7f) * s
                            drawRect(Color(0x22000000), Offset(wpx, wpy), Size(3f * s, 3.5f * s))
                            if (on && night > 0.05f) drawRect(Color(0xFFFFE08A).copy(alpha = night), Offset(wpx, wpy), Size(3f * s, 3.5f * s))
                        }
                        i0++
                    }
                }
            }

            // ---- céu
            drawRect(
                Brush.verticalGradient(listOf(lerp(Color(0xFF5DB4EA), Color(0xFF080D2B), night), lerp(Color(0xFFD6EEFA), Color(0xFF1B2A55), night)), 0f, skyH),
                size = Size(size.width, skyH),
            )
            if (night < 0.7f) drawCircle(Color(0xFFFFF59D).copy(alpha = 1f - night), 9f * s, Offset(size.width * 0.78f, oy * 0.45f))
            // estrelas e lua ficam no céu (antes dos prédios), então não aparecem por cima deles
            if (night > 0f) {
                for (i in stars.indices) {
                    val tw2 = 0.5f + 0.5f * sin(t * 2f + i)
                    val big = if (i % 11 == 0) 2f else 1.3f
                    drawRect(Color.White.copy(alpha = night * tw2), Offset(stars[i].x * size.width, stars[i].y * oy * 0.95f), Size(big * s, big * s))
                }
                val mc = Offset(size.width * 0.25f, oy * 0.4f)
                glow(mc, 26f * s, Color(0xFFCFD8FF).copy(alpha = 0.25f * night))
                drawCircle(Color(0xFFF5F5DC).copy(alpha = night), 7f * s, mc)
                drawCircle(Color(0xFFDADAC0).copy(alpha = 0.7f * night), 1.6f * s, Offset(mc.x - 2f * s, mc.y - 1.5f * s))
                drawCircle(Color(0xFFDADAC0).copy(alpha = 0.7f * night), 1.1f * s, Offset(mc.x + 2.5f * s, mc.y + 2f * s))
            }
            for (i in 0 until 6) {
                val cx = (i * 83f + t * (2f + (i % 3) * 1.2f)) % (VIEW_W + 80f) - 40f
                val cy = oy * 0.12f + (i % 3) * 22f * s
                val cw = 22f + (i * 7 % 19)
                val ca = Color.White.copy(alpha = 0.9f * (1f - night * 0.8f))
                drawRect(ca, Offset(cx * s, cy), Size(cw * s, 5f * s))
                drawRect(ca, Offset((cx + 6f) * s, cy - 4f * s), Size((cw - 12f) * s, 4f * s))
                drawRect(ca, Offset((cx + 12f) * s, cy - 7f * s), Size(8f * s, 3f * s))
            }
            skyline()

            // ================= MUNDO (durante o dia: cenário parado) =================
            withTransform({ translate(0f, oy) }) {
                // calçada com juntas irregulares
                wr(Color(0xFFB7C4CB), cam, 104f, VIEW_W, 14f, cam, s)
                var si = (cam / 14f).toInt() - 1
                while (si * 14f < cam + VIEW_W + 14f) {
                    wr(Color(0xFFA2B1B9), si * 14f + (hsh(si) % 3), 104f, 0.6f, 14f, cam, s)
                    si++
                }
                wr(Color(0xFF8A9AA3), cam, 117f, VIEW_W, 1.6f, cam, s)
                wr(Color(0xFF4A5E6A), cam, 118.6f, VIEW_W, 31.4f, cam, s)
                wr(Color(0xFF3E505B), cam, 140f, VIEW_W, 10f, cam, s)
                // marcas de uso: faixa gasta, bueiro, remendo, poça, rachadura, papel no chão
                val k0 = (cam / 26f).toInt() - 1
                val k1 = ((cam + VIEW_W) / 26f).toInt() + 2
                for (k in k0..k1) {
                    val h = hsh(k * 977 + 13)
                    val cx = k * 26f + (h % 9)
                    wr(Color(0xFFECEFF1).copy(alpha = 0.55f + (h / 11 % 4) * 0.1f), cx + 2f, 133.5f, 10f + (h / 3 % 5), 1.2f, cam, s)
                    when ((h / 7) % 9) {
                        0 -> {
                            wr(Color(0xFF2B363D), cx + 6f, 126f, 8f, 3f, cam, s)
                            wr(Color(0xFF3F4D56), cx + 7f, 126.6f, 6f, 0.6f, cam, s)
                            wr(Color(0xFF3F4D56), cx + 7f, 127.8f, 6f, 0.6f, cam, s)
                        }
                        1 -> wr(Color(0xFF38484F), cx + 3f, 121f + (h / 5 % 6), 12f, 5f, cam, s)
                        2 -> drawOval(Color(0x3390B8D4), Offset((cx + 4f - cam) * s, 143f * s), Size(14f * s, 3f * s))
                        3 -> {
                            wr(Color(0x55000000), cx + 4f, 106f, 0.5f, 3f, cam, s)
                            wr(Color(0x55000000), cx + 4.5f, 108.5f, 0.5f, 3f, cam, s)
                            wr(Color(0x55000000), cx + 5f, 111f, 0.5f, 2.5f, cam, s)
                        }
                        4 -> wr(Color(0xFFE0C07A), cx + 8f, 114.5f, 1.2f, 1f, cam, s)
                        5 -> drawOval(Color(0x22000000), Offset((cx + 6f - cam) * s, 112f * s), Size(5f * s, 1.6f * s))
                        else -> {}
                    }
                }

                drawZones(cam, s, t, night, ((oy - 12f * s) / skyH), paint)
                // cidade
                for (ci in city) {
                    if (ci.x > cam + VIEW_W + 20f || ci.x < cam - 110f) continue
                    when (ci.kind) {
                        0 -> {
                            val bw = spr.bld[ci.idx].width.toFloat()
                            val bh = spr.bld[ci.idx].height.toFloat()
                            wr(Color(0x33000000), ci.x, 101f, bw, 3f, cam, s)
                            sp(spr.bld[ci.idx], ci.x, 104f, cam, s)
                            wr(Color(0x14000000), ci.x, 98f, bw, 6f, cam, s)
                            val hv = hsh(ci.idx * 31 + ci.x.toInt())
                            val topY = 104f - bh
                            when (hv % 4) {
                                0 -> {
                                    val tx = ci.x + 3f + (hv / 4 % (bw - 14f).toInt().coerceAtLeast(1))
                                    wr(Color(0xFF6D6D72), tx, topY - 6f, 9f, 6f, cam, s)
                                    wr(Color(0xFF55555A), tx - 0.4f, topY - 7f, 9.8f, 1.2f, cam, s)
                                }
                                1 -> {
                                    val ax = ci.x + bw * 0.3f
                                    wr(Color(0xFF2B2B2F), ax, topY - 9f, 0.7f, 9f, cam, s)
                                    wr(Color(0xFF2B2B2F), ax - 3f, topY - 8f, 6.6f, 0.6f, cam, s)
                                    wr(Color(0xFF2B2B2F), ax - 2f, topY - 5.5f, 4.6f, 0.6f, cam, s)
                                }
                                else -> {}
                            }
                        }
                        1 -> sp(spr.tree[ci.idx], ci.x, 112f, cam, s)
                        2 -> sp(spr.lamp, ci.x, 116f, cam, s)
                        3 -> sp(spr.sign[ci.idx], ci.x, 112f, cam, s, 0.6f)
                        4 -> {
                            sp(spr.tl, ci.x, 116f, cam, s)
                            val ph = (t * 0.12f + ci.x * 0.003f) % 1f
                            val li = if (ph < 0.45f) 2 else if (ph < 0.55f) 1 else 0
                            val lc = when (li) { 0 -> Color(0xFFFF1744); 1 -> Color(0xFFFFEA00); else -> Color(0xFF00E676) }
                            wr(lc, ci.x + 3f, 116f - 30f + 3f + li * 4f, 2f, 2f, cam, s)
                        }
                        5 -> sp(spr.bench, ci.x, 113f, cam, s)
                        6 -> {   // lixeira
                            val by = 113.5f
                            wr(Color(0x33000000), ci.x - 0.5f, by - 0.5f, 6.5f, 1.2f, cam, s)
                            wr(Color(0xFF2E7D32), ci.x, by - 7f, 5.5f, 7f, cam, s)
                            wr(Color(0xFF1B5E20), ci.x - 0.4f, by - 7.8f, 6.3f, 1.1f, cam, s)
                            wr(Color(0xFF1B5E20), ci.x + 1.2f, by - 5.5f, 0.6f, 4f, cam, s)
                            wr(Color(0xFFEEEEEE), ci.x + 3.4f, by - 8.8f, 1.6f, 1.2f, cam, s)
                        }
                        7 -> {   // saco de lixo
                            val by = 113.5f
                            wr(Color(0x33000000), ci.x - 0.5f, by - 0.5f, 7f, 1.2f, cam, s)
                            wr(Color(0xFF1A1A1F), ci.x, by - 4f, 5.5f, 4f, cam, s)
                            wr(Color(0xFF26262C), ci.x + 1f, by - 5.2f, 3.2f, 1.4f, cam, s)
                            wr(Color(0xFF3A3A42), ci.x + 1.6f, by - 5.8f, 1.2f, 0.8f, cam, s)
                        }
                        8 -> {   // gato (o rabo balança)
                            val fur = if (ci.idx == 0) Color(0xFFCC7A3A) else Color(0xFF55555A)
                            val by = 113.5f
                            val sw = sin(t * 2.2f + ci.x) * 0.9f
                            wr(Color(0x33000000), ci.x - 1.5f, by - 0.5f, 9f, 1.2f, cam, s)
                            wr(fur, ci.x, by - 4f, 5f, 4f, cam, s)
                            wr(fur, ci.x + 3.4f, by - 6.2f, 3f, 2.8f, cam, s)
                            wr(fur, ci.x + 3.4f, by - 7f, 0.9f, 1f, cam, s)
                            wr(fur, ci.x + 5.5f, by - 7f, 0.9f, 1f, cam, s)
                            wr(Color(0xFF212121), ci.x + 4.1f, by - 5.1f, 0.5f, 0.6f, cam, s)
                            wr(Color(0xFF212121), ci.x + 5.4f, by - 5.1f, 0.5f, 0.6f, cam, s)
                            wr(fur, ci.x - 2f, by - 2.2f + sw, 2.2f, 0.9f, cam, s)
                        }
                        else -> {   // vaso de planta
                            val by = 113.5f
                            wr(Color(0xFFB0643A), ci.x, by - 3.6f, 4.4f, 3.6f, cam, s)
                            wr(Color(0xFF8E4D2B), ci.x - 0.3f, by - 4.2f, 5f, 0.9f, cam, s)
                            wr(Color(0xFF2E8B3A), ci.x - 0.6f, by - 7.6f, 5.6f, 3.6f, cam, s)
                            wr(Color(0xFF3FA84C), ci.x + 1f, by - 9f, 2.6f, 2f, cam, s)
                        }
                    }
                }

                // fios entre os postes (com passarinho de vez em quando)
                for (i in 0 until lamps.size - 1) {
                    val a = lamps[i]; val b = lamps[i + 1]
                    if (b < cam - 10f || a > cam + VIEW_W + 10f) continue
                    val x0 = (a + 4.5f - cam) * s
                    val x1 = (b + 4.5f - cam) * s
                    val wire = Path().apply {
                        moveTo(x0, 86.5f * s)
                        quadraticBezierTo((x0 + x1) / 2f, 93f * s, x1, 86.5f * s)
                    }
                    drawPath(wire, Color(0xFF2B2B33).copy(alpha = 0.8f), style = Stroke(width = 0.4f * s))
                    if (night < 0.5f && hsh(i * 5 + 1) % 3 == 0) {
                        val tt = 0.3f + (hsh(i) % 40) / 100f
                        val bx = (1f - tt) * (1f - tt) * x0 + 2f * (1f - tt) * tt * ((x0 + x1) / 2f) + tt * tt * x1
                        val by = (1f - tt) * (1f - tt) * 86.5f * s + 2f * (1f - tt) * tt * 93f * s + tt * tt * 86.5f * s
                        drawRect(Color(0xFF2B2B33), Offset(bx - 1f * s, by - 1.6f * s), Size(2f * s, 1.2f * s))
                        drawRect(Color(0xFF2B2B33), Offset(bx + 0.8f * s, by - 2.2f * s), Size(0.9f * s, 0.9f * s))
                        drawRect(Color(0xFF2B2B33), Offset(bx - 2f * s, by - 1.3f * s), Size(1.1f * s, 0.4f * s))
                    }
                }

                // ferros velhos
                YARD_X.forEachIndexed { i, yx ->
                    if (yx + 100f < cam || yx - 100f > cam + VIEW_W) return@forEachIndexed
                    wr(Color(0xFF5D4037), yx - 85f, 72f, 170f, 32f, cam, s)
                    wr(Color(0xFF4E342E), yx - 85f, 96f, 170f, 8f, cam, s)
                    val tint = Color(0x66554433)
                    sp(spr.cars[(i * 2) % 9], yx - 82f, 104f, cam, s, 1f, false, tint)
                    sp(spr.cars[(i * 3 + 1) % 9], yx - 76f, 84f, cam, s, 1f, true, tint)
                    sp(spr.cars[(i * 5 + 2) % 9], yx + 24f, 104f, cam, s, 1f, i % 2 == 0, tint)
                    sp(spr.cars[(i * 7 + 4) % 9], yx + 30f, 84f, cam, s, 0.9f, false, tint)
                    for (k in 0..2) {
                        drawCircle(Color(0xFF1B1B1B), 4f * s, Offset((yx - 30f + k * 0f - cam) * s, (101f - k * 7f) * s))
                        drawCircle(Color(0xFF6B6B6B), 1.6f * s, Offset((yx - 30f - cam) * s, (101f - k * 7f) * s))
                    }
                    wr(Color(0xFFC62828), yx + 8f, 94f, 6f, 10f, cam, s)
                    wr(Color(0xFF8E0000), yx + 8f, 97f, 6f, 1.2f, cam, s)
                    wr(Color(0xFF9E9E9E), yx - 85f, 70f, 170f, 2f, cam, s)
                    for (k in 0..16) wr(Color(0xFF757575), yx - 85f + k * 10.6f, 70f, 1.5f, 34f, cam, s)
                    wr(Color(0xFF3E2723), yx - 13f, 76f, 26f, 28f, cam, s)
                    wr(Color(0xFF6D4C41), yx - 11f, 78f, 22f, 26f, cam, s)
                    wr(Color(0xFF212121), yx - 50f, 42f, 100f, 20f, cam, s)
                    wr(Color(0xFFFFEB3B), yx - 48f, 44f, 96f, 16f, cam, s)
                    label(paint, YARD_NAMES[i], yx, 56f, cam, s, 8f, 0xFF212121.toInt())
                    wr(Color(0xFF212121), yx - 40f, 62f, 2f, 8f, cam, s)
                    wr(Color(0xFF212121), yx + 38f, 62f, 2f, 8f, cam, s)
                    for (k in 0..8) wr(if (k % 2 == 0) Color(0xFFFF5252) else Color(0xFFFFEE58), yx - 46f + k * 11.5f, 63f + (k % 2) * 1.5f, 2f, 2f, cam, s)
                }
                drawHome(cam, s, t, night, paint)
                drawMeetLot(cam, s, t, night, g.minutes >= MEET_START, paint)
                garageSignText = g.motorsName().uppercase()
                drawGarageShop(g.ownShop, garageOpen(g), cam, s, t, paint)
                drawStores(storesOpen, cam, s, t, paint)
                drawCompanies(g, cam, s, t, paint)
                drawLojas(g, cam, s, t, paint)
                drawDragStrip(cam, s, t, paint)
            }

            // ---- escuridão da noite (só no mundo, o céu já tem a própria cor) e pôr do sol
            val tw = 4f * night * (1f - night)
            if (tw > 0.02f) {
                drawRect(
                    Brush.verticalGradient(listOf(Color.Transparent, Color(0xFFFF8F3D).copy(alpha = 0.5f * tw)), oy * 0.2f, oy + 104f * s),
                    size = Size(size.width, oy + 104f * s), blendMode = BlendMode.Plus,
                )
            }
            if (night > 0f) {
                clipRect(0f, oy, size.width, size.height) {
                    drawRect(
                        Brush.verticalGradient(listOf(Color(0xFF050924).copy(alpha = 0.72f * night), Color(0xFF141B4A).copy(alpha = 0.55f * night)), oy, size.height),
                        Offset(0f, oy), Size(size.width, size.height - oy),
                    )
                }
                // janelas acesas e letreiro (por cima da escuridão, mas ATRÁS das pessoas e carros)
                withTransform({ translate(0f, oy) }) {
                    for (ci in city) {
                        if (ci.x > cam + VIEW_W + 20f || ci.x < cam - 110f) continue
                        if (ci.kind == 0) {
                            sp(spr.win[ci.idx], ci.x, 104f, cam, s, 1f, false, null, night)
                            val bw = spr.bld[ci.idx].width.toFloat()
                            val bh = spr.bld[ci.idx].height.toFloat()
                            glow(Offset((ci.x + bw / 2f - cam) * s, (104f - bh * 0.5f) * s), max(bw, bh) * 0.55f * s, Color(0xFFFFD27A).copy(alpha = 0.16f * night))
                        }
                    }
                    YARD_X.forEachIndexed { i, yx ->
                        if (yx + 100f < cam || yx - 100f > cam + VIEW_W) return@forEachIndexed
                        wr(Color(0xFFFFEB3B).copy(alpha = night), yx - 48f, 44f, 96f, 16f, cam, s)
                        label(paint, YARD_NAMES[i], yx, 56f, cam, s, 8f, 0xFF212121.toInt())
                        glow(Offset((yx - cam) * s, 52f * s), 60f * s, Color(0xFFFFEB3B).copy(alpha = 0.3f * night))
                        glow(Offset((yx - cam) * s, 100f * s), 34f * s, Color(0xFFFFB74D).copy(alpha = 0.3f * night))
                    }
                    drawHomeLit(cam, s, night)
                    drawMeetLit(cam, s, t, night, g.minutes >= MEET_START, paint)
                    drawGarageLit(g.ownShop, garageOpen(g), cam, s, night, paint)
                    drawStoresLit(storesOpen, cam, s, night, paint)
                    drawCompaniesLit(g, cam, s, night, paint)
                    drawLojasLit(g, cam, s, night, paint)
                    drawZonesLit(cam, s, night)
                }
            }

            // ================= GENTE E CARROS (sempre na frente das janelas) =================
            withTransform({ translate(0f, oy) }) {
                for (pd in peds) {
                    val a = t * 0.4f + pd.phase
                    val wx = pd.base + sin(a) * 60f
                    if (wx < cam - 20f || wx > cam + VIEW_W + 20f) continue
                    drawChar(pd.look, (wx - 6f - cam) * s, (114f - 26f) * s, s, cos(a) < 0f, 1f, t * 7f + pd.phase)
                }
                if (meetOn && MEET_X + 110f > cam && MEET_X - 110f < cam + VIEW_W) {
                    meetField.take(3).forEachIndexed { i, mc ->
                        val si = carSpriteFor(CATALOG[mc.model].body)
                        val tintC = Color(PAINTS[mc.color].second).copy(alpha = 0.35f)
                        sp(spr.cars[si], MEET_X - 100f + i * 66f, 112f, cam, s, 0.95f, i == 1, tintC, 1f, si, 0f, null)
                    }
                    meetField.forEachIndexed { i, mc ->
                        val ox = MEET_X - 88f + i * 42f
                        drawChar(mc.look, (ox - 6f - cam) * s, (114f - 26f) * s, s, i % 2 == 1, 0f, t * 2f + i)
                    }
                }
                val dcar = g.garage.firstOrNull { it.id == driveId }
                if (dcar != null) {
                    val csi = carSpriteFor(CATALOG[dcar.model].body)
                    val cimg = spr.cars[csi]
                    val ctint = Color(PAINTS[dcar.color].second).copy(alpha = 0.35f)
                    val laneY = if (face < 0) 148f else 133f
                    val ck = 1.1f
                    val cshape = shapeFor(CATALOG[dcar.model].body)
                    val cpaint = Color(PAINTS[dcar.color.coerceIn(0, PAINTS.lastIndex)].second)
                    val cseed = CATALOG[dcar.model].body.ordinal * 17 + dcar.color * 5 + 3
                    val left = (px - 31f * ck - cam) * s
                    val topY = laneY * s - 22.5f * ck * s
                    withTransform({
                        if (face < 0) scale(-1f, 1f, Offset((px - cam) * s, topY))
                        translate(left, topY)
                        scale(ck * s, ck * s, Offset.Zero)
                    }) {
                        drawCarUnits(cshape, cseed, cpaint, dcar.tuning, dcar.cond, t, look)
                    }
                } else {
                    drawChar(look, (px - 6f - cam) * s, (115f - 26f) * s, s, face < 0, if (moving) 1f else 0f,
                        t * (if (run) 16f else 10f))
                }
                for (c in traffic) {
                    val x = (((c.x0 + t * c.speed) % WORLD) + WORLD) % WORLD
                    if (x < cam - 90f || x > cam + VIEW_W + 10f) continue
                    val img = spr.cars[c.spr]
                    val rot = (t * abs(c.speed) + c.x0) / 4.5f
                    if (c.lane == 0) sp(img, x, 133f, cam, s, 1.1f, false, null, 1f, c.spr, rot, c.driver)
                    else sp(img, x, 148f, cam, s, 1.1f, true, null, 1f, c.spr, rot, c.driver)
                }
            }
            // à noite, rua e gente escurecem um pouco (pra não parecer recortado em cima da escuridão)
            if (night > 0f) {
                drawRect(
                    Brush.verticalGradient(listOf(Color(0xFF0A1030).copy(alpha = 0f), Color(0xFF0A1030).copy(alpha = 0.34f * night)), oy + 80f * s, oy + 150f * s),
                    Offset(0f, oy + 80f * s), Size(size.width, 70f * s),
                )
            }

            // ================= LUZES (postes, faróis, olho de gato) =================
            if (night > 0f) {
                withTransform({ translate(0f, oy) }) {
                    for (ci in city) {
                        if (ci.x > cam + VIEW_W + 20f || ci.x < cam - 110f) continue
                        if (ci.kind == 8) {
                            wr(Color(0xFFFFEB3B).copy(alpha = night), ci.x + 4.1f, 113.5f - 5.1f, 0.5f, 0.6f, cam, s)
                            wr(Color(0xFFFFEB3B).copy(alpha = night), ci.x + 5.4f, 113.5f - 5.1f, 0.5f, 0.6f, cam, s)
                        }
                        if (ci.kind == 2) {
                            val lc = Offset((ci.x + 4.5f - cam) * s, 86f * s)
                            val cone = Path().apply {
                                moveTo(lc.x - 2f * s, lc.y); lineTo(lc.x + 2f * s, lc.y)
                                lineTo(lc.x + 27f * s, 117f * s); lineTo(lc.x - 27f * s, 117f * s); close()
                            }
                            drawPath(
                                cone,
                                Brush.verticalGradient(listOf(Color(0xFFFFD27A).copy(alpha = 0.42f * night), Color(0xFFFFD27A).copy(alpha = 0.06f * night)), lc.y, 117f * s),
                                blendMode = BlendMode.Plus,
                            )
                            glow(Offset(lc.x, 114f * s), 26f * s, Color(0xFFFFC66E).copy(alpha = 0.35f * night))
                            glow(lc, 7f * s, Color(0xFFFFF1C2).copy(alpha = 0.95f * night))
                        }
                    }
                    for (c in traffic) {
                        val x = (((c.x0 + t * c.speed) % WORLD) + WORLD) % WORLD
                        if (x < cam - 90f || x > cam + VIEW_W + 60f) continue
                        val wcar = spr.cars[c.spr].width * 1.1f
                        val by = if (c.lane == 0) 133f else 148f
                        val sgn = if (c.lane == 0) 1f else -1f
                        val fx = if (c.lane == 0) x + wcar - 1f else x + 1f
                        val fp = Offset((fx - cam) * s, (by - 6.5f) * s)
                        val beam = Path().apply {
                            moveTo(fp.x, fp.y - 1.2f * s); lineTo(fp.x + sgn * 48f * s, fp.y - 8f * s)
                            lineTo(fp.x + sgn * 48f * s, fp.y + 6f * s); lineTo(fp.x, fp.y + 1.2f * s); close()
                        }
                        drawPath(
                            beam,
                            Brush.horizontalGradient(listOf(Color(0xFFFFF3B0).copy(alpha = 0.55f * night), Color(0xFFFFF3B0).copy(alpha = 0f)), fp.x, fp.x + sgn * 48f * s),
                            blendMode = BlendMode.Plus,
                        )
                        glow(fp, 5f * s, Color(0xFFFFFFFF).copy(alpha = 0.95f * night))
                        val rx = if (c.lane == 0) x + 1f else x + wcar - 3f
                        wr(Color(0xFFFF1744).copy(alpha = night), rx, by - 9f, 2f, 2f, cam, s)
                        glow(Offset((rx + 1f - cam) * s, (by - 8f) * s), 5f * s, Color(0xFFFF1744).copy(alpha = 0.4f * night))
                    }
                }
            }

            // ================= BALÕES E NOMES (por último: nada fica por cima do texto) =================
            withTransform({ translate(0f, oy) }) {
                val sayNow = say
                if (sayNow != null) bubble(paint, sayNow, px, 84f, cam, s)
                else label(paint, me.name, px, 84f, cam, s, 6f, 0xFFFFFFFF.toInt())
                // só um pedestre fala por vez (o mais perto), sem empilhar balão
                var bestPd = -1
                var bestD = 32f
                for ((pi, pd) in peds.withIndex()) {
                    val wx = pd.base + sin(t * 0.4f + pd.phase) * 60f
                    val dd = abs(wx - px)
                    if (dd < bestD && ((t * 0.25f + pd.phase).toInt() % 3 == 0)) { bestD = dd; bestPd = pi }
                }
                if (bestPd >= 0) {
                    val pd = peds[bestPd]
                    val wx = pd.base + sin(t * 0.4f + pd.phase) * 60f
                    val line = PED_LINES[(bestPd + (t * 0.25f + pd.phase).toInt() / 3) % PED_LINES.size].replace("{n}", me.name)
                    bubble(paint, line, wx, if (sayNow != null) 70f else 84f, cam, s)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(8.dp), Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
            RoundHold(-1, { dir = if (it) -1 else 0 })
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                if (near >= 0) {
                    Button(onClick = {
                        val first = (mask shr near and 1) == 0
                        mask = mask or (1 shl near)
                        prefs.edit().putInt("mask", mask).apply()
                        talk = near to first
                    }) { Text("Entrar", fontSize = 13.sp) }
                } else if (nearMeet) {
                    Button(onClick = { meetDlg = true }) { Text("🏁 Encontro de carros", fontSize = 13.sp) }
                } else if (nearHome) {
                    Button(onClick = { homeDlg = true }) { Text("🏠 Entrar em casa", fontSize = 13.sp) }
                } else if (nearGarage) {
                    Button(onClick = { garageDlg = true }) { Text(if (g.ownShop) "🏢 ${g.motorsName()}" else "🔧 Entrar na oficina", fontSize = 13.sp) }
                } else if (nearZone >= 0) {
                    Button(onClick = { zoneDlg = nearZone }) { Text(if (nearZone == 0) "🌳 Parque Central" else "🏖️ Quiosque da Praia", fontSize = 13.sp) }
                } else if (nearDrag) {
                    Button(onClick = { dragDlg = true }) { Text("🏁 Pista de Arrancada", fontSize = 13.sp) }
                } else if (nearLoja >= 0) {
                    Button(onClick = { lojaDlg = nearLoja }) { Text(lojaLabel(g, nearLoja), fontSize = 13.sp) }
                } else if (nearBiz >= 0) {
                    Button(onClick = { bizDlg = nearBiz }) {
                        Text(if (nearBiz in g.companies) "${COMPANY_ICONS[nearBiz]} ${g.empName(nearBiz)}" else "📍 Terreno à venda", fontSize = 13.sp)
                    }
                } else if (nearStore >= 0) {
                    Button(onClick = { shopDlg = nearStore }) { Text(STORES[nearStore].title.substringBefore(" ") + " Entrar", fontSize = 13.sp) }
                } else if (nearPed >= 0) {
                    Button(onClick = { chatPed = nearPed }) { Text("💬 Conversar com ${pedName(nearPed)}", fontSize = 13.sp) }
                } else {
                    Text(if (driveId != null) "🚘 ${(abs(vel) * 0.5f).toInt()} km/h" else "Explore a cidade", fontSize = 12.sp)
                }
                Row {
                    TextButton(onClick = { if (driveId != null) driveId = null else driveDlg = true }, contentPadding = PaddingValues(6.dp)) { Text(if (driveId != null) "🚶 Descer" else "🚘 Dirigir", fontSize = 12.sp) }
                }
                Row {
                    TextButton(onClick = { garageListDlg = true }, contentPadding = PaddingValues(6.dp)) { Text("🚗 Garagem", fontSize = 12.sp) }
                    TextButton(onClick = { summaryDlg = true }, contentPadding = PaddingValues(6.dp)) { Text("📊 Resumo", fontSize = 12.sp) }
                }
                Row {
                    TextButton(onClick = { taxi = true }, contentPadding = PaddingValues(6.dp)) { Text("🚕 Táxi", fontSize = 12.sp) }
                    TextButton(onClick = { run = !run }, contentPadding = PaddingValues(6.dp)) { Text(if (run) "🏃 Correndo" else "🚶 Andando", fontSize = 12.sp) }
                    TextButton(onClick = onEdit, contentPadding = PaddingValues(6.dp)) { Text("🧍 Visual", fontSize = 12.sp) }
                }
            }
            RoundHold(1, { dir = if (it) 1 else 0 })
        }
    }

    talk?.let { (yi, first) ->
        YardTalk(g, yi, me.name, me.look.female, first, mask == 31, spr) { talk = null }
    }
    if (meetDlg) CarMeetDialog(g, me.name, me.look) { meetDlg = false }
    if (garageDlg) { if (g.ownShop) InteriorDialog(g, Room.WORKSHOP) { garageDlg = false } else WorkshopDialog(g) { garageDlg = false } }
    if (garageListDlg) GarageDialog(g) { garageListDlg = false }
    if (summaryDlg) SummaryDialog(g) { summaryDlg = false }
    if (driveDlg) CarPickDialog(g, onPick = { driveId = it; driveDlg = false }, onClose = { driveDlg = false })
    if (dragDlg) DragDialog(g, spr) { dragDlg = false }
    zoneDlg?.let { zi -> ZoneDialog(g, zi) { zoneDlg = null } }
    lojaDlg?.let { li -> LojaDialog(g, li) { lojaDlg = null } }
    bizDlg?.let { bi -> if (bi == 0 && 0 in g.companies) InteriorDialog(g, Room.SHOP) { bizDlg = null } else CompanyDialog(g, bi) { bizDlg = null } }
    shopDlg?.let { si -> StoreDialog(g, STORES[si]) { shopDlg = null } }
    chatPed?.let { pi -> StreetChat(g, peds[pi].look, pedName(pi), me.name) { chatPed = null } }
    if (homeDlg) {
        InteriorDialog(g, Room.HOME, onSleep = { homeDlg = false; sleepTaxi = false; sleeping = true }) { homeDlg = false }
    }
    if (sleeping) {
        SleepScene(g, me.name, me.look.female, sleepTaxi) {
            sleeping = false
            px = HOME_X + 30f
            face = 1
            prefs.edit().putFloat("x", px).apply()
            say = "Bom dia! Hoje eu fecho negócio."
            sayUntil = time + 4f
        }
    }
    if (lateAsk) {
        Dialog(onDismissRequest = { lateAsk = false }) {
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🌙 Já é madrugada!", fontWeight = FontWeight.Bold)
                    Text("${me.name} tá morrendo de sono. Melhor chamar um táxi e ir dormir em casa.", fontSize = 13.sp)
                    Button(onClick = { lateAsk = false; rideHome() }, Modifier.fillMaxWidth()) { Text("🚕 Ir pra casa e dormir") }
                    TextButton(onClick = { lateAsk = false }) { Text("Mais um pouquinho") }
                }
            }
        }
    }
    if (taxi) {
        Dialog(onDismissRequest = { taxi = false }) {
            Card {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🚕 Táxi - pra onde?", fontWeight = FontWeight.Bold)
                    YARD_X.forEachIndexed { i, yx ->
                        val dist = abs(px - yx)
                        val fare = (dist / 25f).toInt().coerceAtLeast(10)
                        val mins = (dist / 12f).toInt().coerceAtLeast(5)
                        OutlinedButton(
                            onClick = {
                                if (g.money < fare) {
                                    g.message = "Sem dinheiro pro táxi."
                                } else {
                                    g.money -= fare
                                    g.spendTime(mins)
                                    px = (yx - 40f).coerceAtLeast(20f)
                                    prefs.edit().putFloat("x", px).apply()
                                    g.save()
                                }
                                taxi = false
                            },
                            Modifier.fillMaxWidth(),
                        ) { Text("${YARD_NAMES[i]} - ${brl(fare)}", fontSize = 13.sp) }
                    }
                    val distM = abs(px - MEET_X)
                    val fareM = taxiFare(distM)
                    OutlinedButton(
                        onClick = {
                            if (g.money < fareM) {
                                g.message = "Sem dinheiro pro táxi."
                            } else {
                                g.money -= fareM
                                g.spendTime(taxiMinutes(distM))
                                px = MEET_X - 55f
                                prefs.edit().putFloat("x", px).apply()
                                g.save()
                            }
                            taxi = false
                        },
                        Modifier.fillMaxWidth(),
                    ) { Text("🏁 Encontro de Carros - ${brl(fareM)}", fontSize = 13.sp) }
                    val distG = abs(px - GARAGE_X)
                    val fareG = taxiFare(distG)
                    OutlinedButton(
                        onClick = {
                            if (g.money < fareG) {
                                g.message = "Sem dinheiro pro táxi."
                            } else {
                                g.money -= fareG
                                g.spendTime(taxiMinutes(distG))
                                px = GARAGE_X - 75f
                                prefs.edit().putFloat("x", px).apply()
                                g.save()
                            }
                            taxi = false
                        },
                        Modifier.fillMaxWidth(),
                    ) { Text("🔧 Oficina - ${brl(fareG)}", fontSize = 13.sp) }
                    COMPANY_X.forEachIndexed { ci, cxx ->
                        if (ci in g.companies) {
                            val distC = abs(px - cxx)
                            val fareC = taxiFare(distC)
                            OutlinedButton(
                                onClick = {
                                    if (g.money < fareC) {
                                        g.message = "Sem dinheiro pro táxi."
                                    } else {
                                        g.money -= fareC
                                        g.spendTime(taxiMinutes(distC))
                                        px = cxx - 75f
                                        prefs.edit().putFloat("x", px).apply()
                                        g.save()
                                    }
                                    taxi = false
                                },
                                Modifier.fillMaxWidth(),
                            ) { Text("${COMPANY_ICONS[ci]} ${g.empName(ci)} - ${brl(fareC)}", fontSize = 13.sp) }
                        }
                    }
                    LOJA_X.forEachIndexed { li, lxx ->
                        val lt = g.lojas[li]
                        if (lt != null) {
                            val distL = abs(px - lxx)
                            val fareL = taxiFare(distL)
                            OutlinedButton(
                                onClick = {
                                    if (g.money < fareL) {
                                        g.message = "Sem dinheiro pro táxi."
                                    } else {
                                        g.money -= fareL
                                        g.spendTime(taxiMinutes(distL))
                                        px = lxx - 75f
                                        prefs.edit().putFloat("x", px).apply()
                                        g.save()
                                    }
                                    taxi = false
                                },
                                Modifier.fillMaxWidth(),
                            ) { Text("${LOJA_ICONS[lt]} ${g.lojaName(li)} - ${brl(fareL)}", fontSize = 13.sp) }
                        }
                    }
                    val distD = abs(px - DRAG_X)
                    val fareD = taxiFare(distD)
                    OutlinedButton(
                        onClick = {
                            if (g.money < fareD) {
                                g.message = "Sem dinheiro pro táxi."
                            } else {
                                g.money -= fareD
                                g.spendTime(taxiMinutes(distD))
                                px = DRAG_X - 75f
                                prefs.edit().putFloat("x", px).apply()
                                g.save()
                            }
                            taxi = false
                        },
                        Modifier.fillMaxWidth(),
                    ) { Text("🏁 Pista de Arrancada - ${brl(fareD)}", fontSize = 13.sp) }
                    val distP = abs(px - PARK_X)
                    OutlinedButton(
                        onClick = {
                            val fareP = taxiFare(distP)
                            if (g.money < fareP) {
                                g.message = "Sem dinheiro pro táxi."
                            } else {
                                g.money -= fareP
                                g.spendTime(taxiMinutes(distP))
                                px = PARK_X - 75f
                                prefs.edit().putFloat("x", px).apply()
                                g.save()
                            }
                            taxi = false
                        },
                        Modifier.fillMaxWidth(),
                    ) { Text("🌳 Parque Central - ${brl(taxiFare(distP))}", fontSize = 13.sp) }
                    val distB = abs(px - BEACH_X)
                    OutlinedButton(
                        onClick = {
                            val fareB = taxiFare(distB)
                            if (g.money < fareB) {
                                g.message = "Sem dinheiro pro táxi."
                            } else {
                                g.money -= fareB
                                g.spendTime(taxiMinutes(distB))
                                px = BEACH_X - 75f
                                prefs.edit().putFloat("x", px).apply()
                                g.save()
                            }
                            taxi = false
                        },
                        Modifier.fillMaxWidth(),
                    ) { Text("🏖️ Praia - ${brl(taxiFare(distB))}", fontSize = 13.sp) }
                    val fareH = taxiFare(abs(px - HOME_X))
                    OutlinedButton(
                        onClick = { taxi = false; rideHome() },
                        Modifier.fillMaxWidth(),
                    ) { Text((if (g.minutes >= SLEEP_FROM) "🏠 Casa (ir dormir) - " else "🏠 Minha casa - ") + brl(fareH), fontSize = 13.sp) }
                    TextButton(onClick = { taxi = false }) { Text("Cancelar") }
                }
            }
        }
    }
}

/** Botão redondo que anda enquanto está pressionado (-1 = esquerda, 1 = direita). */
@Composable
private fun RoundHold(side: Int, onHold: (Boolean) -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val bg = if (pressed) Color(0xFF7E57C2) else Color(0xFF4A3A8C)
    Box(
        Modifier.size(78.dp).background(bg, CircleShape).border(3.dp, Color(0xFFD1C4E9), CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(onPress = { pressed = true; onHold(true); tryAwaitRelease(); pressed = false; onHold(false) })
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(30.dp)) {
            val p = Path()
            if (side < 0) { p.moveTo(size.width, 0f); p.lineTo(0f, size.height / 2f); p.lineTo(size.width, size.height) }
            else { p.moveTo(0f, 0f); p.lineTo(size.width, size.height / 2f); p.lineTo(0f, size.height) }
            p.close()
            drawPath(p, Color.White)
        }
    }
}

// ------------------------------------------------------------ criação de personagem

@Composable
private fun CharacterCreator(onDone: (PlayerProfile) -> Unit) {
    var name by remember { mutableStateOf("") }
    var female by remember { mutableStateOf(false) }
    var skin by remember { mutableIntStateOf(1) }
    var hairStyle by remember { mutableIntStateOf(0) }
    var hair by remember { mutableIntStateOf(0) }
    var topType by remember { mutableIntStateOf(0) }
    var top by remember { mutableIntStateOf(0) }
    var botType by remember { mutableIntStateOf(0) }
    var bottom by remember { mutableIntStateOf(0) }
    var shoes by remember { mutableIntStateOf(0) }
    var glasses by remember { mutableStateOf(false) }
    var walking by remember { mutableStateOf(true) }
    var tm by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { while (true) withFrameNanos { tm = (it / 1_000_000L) / 1000f } }
    val look = Look(female, skin, hairStyle, hair, topType, top, if (!female && botType == 2) 0 else botType, bottom, shoes, glasses)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Crie seu personagem", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Canvas(Modifier.size(130.dp, 270.dp)) {
            val c = size.height / 28f
            drawChar(look, (size.width - 12f * c) / 2f, c, c, false, if (walking) 1f else 0f, tm * 8f)
        }
        FilterChip(walking, { walking = !walking }, { Text(if (walking) "Andando" else "Parado") })
        OutlinedTextField(name, { name = it.take(14) }, label = { Text("Nome") }, singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(!female, { female = false }, { Text("Homem") })
            FilterChip(female, { female = true }, { Text("Mulher") })
        }
        Chooser("Cabelo", HAIR_STYLES, hairStyle) { hairStyle = it }
        Swatches("Cor do cabelo", CHAIR, hair) { hair = it }
        Swatches("Pele", CSKIN, skin) { skin = it }
        Chooser("Roupa", TOP_TYPES, topType) { topType = it }
        Swatches("Cor da roupa", CTOP, top) { top = it }
        Chooser("Baixo", if (female) BOT_TYPES else BOT_TYPES.take(2), if (!female && botType == 2) 0 else botType) { botType = it }
        Swatches("Cor do baixo", CBOT, bottom) { bottom = it }
        Swatches("Tênis", CSHOE, shoes) { shoes = it }
        FilterChip(glasses, { glasses = !glasses }, { Text("Óculos") })
        Button(onClick = { onDone(PlayerProfile(name.trim().ifBlank { "Você" }, look)) }) { Text("Começar") }
    }
}

@Composable
private fun Chooser(title: String, names: List<String>, sel: Int, onSel: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 13.sp, modifier = Modifier.width(64.dp))
        OutlinedButton(onClick = { onSel((sel + names.size - 1) % names.size) }, contentPadding = PaddingValues(8.dp)) { Text("◀") }
        Text(names[sel.coerceIn(0, names.lastIndex)], Modifier.width(100.dp), textAlign = TextAlign.Center)
        OutlinedButton(onClick = { onSel((sel + 1) % names.size) }, contentPadding = PaddingValues(8.dp)) { Text("▶") }
    }
}

@Composable
private fun Swatches(title: String, colors: List<Color>, sel: Int, onSel: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            colors.forEachIndexed { i, c ->
                val shape = RoundedCornerShape(6.dp)
                Box(
                    Modifier.size(28.dp).background(c, shape)
                        .border(if (i == sel) 3.dp else 1.dp, if (i == sel) Color.White else Color.Gray, shape)
                        .clickable { onSel(i) },
                )
            }
        }
    }
}

// ------------------------------------------------------------ história e negociação

private class YardOwner(
    val name: String, val look: BuyerLook, val easy: Double, val maxDisc: Double,
    val intro: List<String>, val again: String,
)

private val OWNERS = listOf(
    YardOwner("Seu Zé", BuyerLook(false, Color(0xFFC68642), Color(0xFF9E9E9E), Color(0xFFFB8C00), Color(0xFF5D4037)), 0.7, 0.30,
        listOf("Opa! Você deve ser {n}, sobrinho do Chico, né? A cara dele!",
            "O Chico começou aqui comigo: comprava lata velha, arrumava e vendia pelo triplo.",
            "Dá uma olhada nos carros. Pechincha com respeito que eu gosto!"),
        "Voltou, {n}! Qual vai ser hoje?"),
    YardOwner("Dona Neusa", BuyerLook(true, Color(0xFFE8B98A), Color(0xFF4E342E), Color(0xFF8E24AA), Color(0xFF212121)), 0.35, 0.15,
        listOf("Desmanche Central. Aqui ninguém dá nada de graça.",
            "Conheci o Chico. Ele me devia um favor, mas negócio é negócio, {n}.",
            "Pode olhar. E não encosta em nada se não for comprar."),
        "Hum... {n}. Vai comprar ou só olhar?"),
    YardOwner("Dr. Valdemar", BuyerLook(false, Color(0xFFFFDBB4), Color(0xFF1B1B1B), Color(0xFFECEFF1), Color(0xFF283593)), 0.25, 0.10,
        listOf("Isto aqui não é ferro velho, é um Pátio. Limpe os pés.",
            "O Chico já sentou nessa cadeira... e saiu com o melhor negócio do ano. Irritante.",
            "Importados e esportivos. Preço firme, {n}, mas hoje estou de bom humor."),
        "{n}. Espero que tenha trazido dinheiro de verdade."),
    YardOwner("Beto Maluco", BuyerLook(false, Color(0xFF8D5524), Color(0xFFB71C1C), Color(0xFFFDD835), Color(0xFF37474F)), 0.6, 0.35,
        listOf("EEEEI! Cuidado com o cachorro! Ele não morde, só late. E morde.",
            "O Chico dizia: 'todo carro tem uma história'. O azul ali teve três donos e um padre!",
            "Pode pechinchar, {n}. Eu invento o preço na hora mesmo."),
        "Fala, {n}! Hoje o preço tá pelo humor!"),
    YardOwner("Seu Rodrigo", BuyerLook(false, Color(0xFFC68642), Color(0xFF9E9E9E), Color(0xFF43A047), Color(0xFF5D4037)), 0.5, 0.20,
        listOf("Então é você, {n}. O Chico falou muito de você.",
            "Ele deixou uma chave comigo e um recado: só entrego quando você conhecer todos os ferros velhos da cidade.",
            "Continua explorando. Eu espero. Enquanto isso, olha os carros."),
        "Continua explorando, {n}. A chave tá guardada."),
)

private val OWNER_PITCH = listOf(0.7f, 1.15f, 0.85f, 1.5f, 0.8f)
private val OWNER_RATE = listOf(0.85f, 1.0f, 0.9f, 1.25f, 0.85f)

private val ENDING = listOf(
    "Você conheceu todos os pátios da cidade, {n}. O Chico tinha razão sobre você.",
    "Toma a chave. É da oficina dele no centro. Agora ela é sua.",
    "Ele dizia que ferro velho não vende carro, vende segunda chance. Faz bom uso!",
)

@Composable
private fun YardTalk(g: GameState, yi: Int, pname: String, female: Boolean, first: Boolean, allDone: Boolean, spr: CitySprites, onClose: () -> Unit) {
    val o = OWNERS[yi]
    // humor do dono muda a cada dia: 0 = fechado pra negociar, 1 = normal, 2 = de bom humor
    val mood = remember { Random(g.day * 131 + yi * 17).nextInt(100).let { if (it < 25) 0 else if (it < 75) 1 else 2 } }
    val lines = remember {
        val base = if (yi == 4 && allDone) ENDING else if (first) o.intro else listOf(o.again)
        val extra = when (mood) {
            0 -> listOf("Hoje eu tô com a cabeça quente. Não negocio nada, {n}.")
            2 -> listOf("Hoje acordei de bom humor, vai que rola um desconto!")
            else -> emptyList()
        }
        (base + extra).map { it.replace("{n}", pname) }
    }
    var idx by remember { mutableIntStateOf(0) }
    var speech by remember { mutableStateOf(lines[0]) }
    var picked by remember { mutableStateOf<YardCar?>(null) }
    val y = picked
    var you by remember { mutableStateOf<String?>(null) }
    var cur by remember(y?.id) { mutableIntStateOf(y?.price ?: 0) }
    var patience by remember(y?.id) { mutableIntStateOf(if (mood == 2) 3 else 2) }
    val chance = (o.easy * 0.7 + (if (mood == 2) 0.25 else 0.0) + 0.1).coerceIn(0.05, 0.95)

    var lastYou by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(speech, you) {
        val yy = you
        val pi = OWNER_PITCH[yi.coerceIn(0, 4)]; val ra = OWNER_RATE[yi.coerceIn(0, 4)]
        if (yy != null && yy != lastYou) {
            lastYou = yy
            Voice.speak(yy, if (female) 1.3f else 0.95f)
            Voice.speak(speech, pi, ra, add = true)
        } else Voice.speak(speech, pi, ra)
    }
    DisposableEffect(Unit) { onDispose { Voice.stop() } }

    fun offer(pct: Int) {
        val c = picked ?: return
        g.spendTime(10)
        if (mood == 0) { speech = "Já falei: hoje não negocio. O preço é ${brl(c.price)}."; return }
        if (patience <= 0) { speech = "Já deu, $pname. Esse preço é final."; return }
        val want = roundTo50(c.price * (100 - pct) / 100.0)
        you = "E se eu pagar ${brl(want)}?"
        val floor = roundTo50(c.price * (1 - o.maxDisc * 0.8))
        if (want >= cur) { speech = "Isso é mais do que eu pedi. Pode pagar à vontade!"; return }
        if (want >= floor && Random.nextDouble() < chance) {
            cur = want
            speech = "Tá bom, tá bom... fecho em ${brl(want)}."
        } else {
            patience -= 1
            if (cur > floor) cur = maxOf(floor, roundTo50(cur - c.price * 0.02))
            speech = if (patience <= 0) "Chega! Última oferta: ${brl(cur)}." else "Muito baixo! Faço por ${brl(cur)}."
        }
    }

    Dialog(onDismissRequest = onClose) {
        Card {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PixelPerson(o.look, Modifier.size(48.dp, 84.dp))
                    Column {
                        Text(o.name + when (mood) { 0 -> "  😠"; 2 -> "  😄"; else -> "  😐" }, fontWeight = FontWeight.Bold)
                        Text(speech, fontSize = 14.sp)
                        you?.let { Text("$pname: $it", fontSize = 13.sp, color = Color(0xFF90CAF9)) }
                    }
                }
                if (idx < lines.size - 1 && y == null) {
                    Button(onClick = { idx += 1; speech = lines[idx] }, Modifier.align(Alignment.End)) { Text("Continuar ▶") }
                } else if (y == null) {
                    Text("Seu dinheiro: ${brl(g.money)}", fontSize = 12.sp)
                    val list = g.yards.getOrNull(yi).orEmpty()
                    if (list.isEmpty()) Text("Nada sobrou aqui hoje. Passe o dia!")
                    Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        list.forEach { c ->
                            val m = CATALOG[c.model]
                            Row(
                                Modifier.fillMaxWidth().clickable { picked = c; speech = "Esse aí? Boa escolha. Quanto você paga?" }.padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Image(spr.cars[carSpriteFor(m.body)], null, Modifier.width(64.dp).height(24.dp), filterQuality = FilterQuality.None)
                                Column { Text("${m.fullName} ${c.year}", fontSize = 13.sp); Text(brl(c.price), fontSize = 12.sp) }
                            }
                        }
                    }
                    TextButton(onClick = onClose) { Text("Sair") }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Image(spr.cars[carSpriteFor(CATALOG[y.model].body)], null, Modifier.width(64.dp).height(24.dp), filterQuality = FilterQuality.None)
                        Column { Text("${CATALOG[y.model].fullName} ${y.year}", fontSize = 13.sp); Text("Preço agora: ${brl(cur)}", fontWeight = FontWeight.Bold) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(10, 20, 30).forEach { pct ->
                            FilledTonalButton(onClick = { offer(pct) }, Modifier.weight(1f), enabled = mood != 0 && patience > 0, contentPadding = PaddingValues(4.dp)) { Text("-$pct%", fontSize = 13.sp) }
                        }
                    }
                    OutlinedButton(onClick = {
                        val w = y.cond.indices.minByOrNull { y.cond[it] } ?: 0
                        you = "Qual é o defeito dele?"
                        speech = "O pior dele é ${PART_NAMES[w].lowercase()}. O resto dá pro gasto."
                    }, Modifier.fillMaxWidth()) { Text("Perguntar do carro") }
                    Button(onClick = {
                        g.buy(yi, y.copy(price = cur))
                        if (g.yards.getOrNull(yi).orEmpty().any { it.id == y.id }) {
                            speech = g.message
                        } else {
                            you = "Fechado! Valeu, ${o.name}."
                            speech = "Fechado, $pname! Cuida bem dele."
                            picked = null
                            idx = lines.size - 1
                        }
                    }, Modifier.fillMaxWidth()) { Text("Comprar por ${brl(cur)}") }
                    TextButton(onClick = { picked = null; speech = "Pensa melhor, $pname. Tem mais carro aí." }) { Text("Voltar") }
                }
            }
        }
    }
}
