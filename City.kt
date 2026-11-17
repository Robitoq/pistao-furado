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

private const val WORLD = 5000f
private const val VIEW_W = 190f
private val YARD_X = listOf(520f, 1450f, 2450f, 3500f, 4450f)

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

private fun randomLook(r: Random) = Look(
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
    while (x < WORLD - 100f) {
        val yx = YARD_X.firstOrNull { x + 80f > it - 110f && x < it + 110f }
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
    while (lx < WORLD) { out.add(Item(2, 0, lx)); lx += 120f }
    var k = 0
    while (350f + 700f * k < WORLD) { out.add(Item(4, 0, 350f + 700f * k)); k++ }
    k = 0
    while (200f + 430f * k < WORLD) { out.add(Item(5, 0, 200f + 430f * k)); k++ }
    return out
}

private fun nightF(m: Int): Float = when {
    m < 5 * 60 -> 1f
    m < 7 * 60 -> (7 * 60 - m) / 120f
    m < 18 * 60 -> 0f
    m < 20 * 60 -> (m - 18 * 60) / 120f
    else -> 1f
}

// ------------------------------------------------------------ desenho

private fun DrawScope.wr(c: Color, x: Float, y: Float, w: Float, h: Float, cam: Float, s: Float) {
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

fun DrawScope.drawDriver(l: Look, left: Float, top: Float, u: Float, w: Float) {
    val x = left + w * 0.55f * u
    val y = top + 3.4f * u
    drawRect(CTOP[l.top], Offset(x - 0.3f * u, y + 3.2f * u), Size(4f * u, 1.6f * u))
    drawRect(CSKIN[l.skin], Offset(x, y), Size(3.4f * u, 3.2f * u))
    if (l.hairStyle != 3) drawRect(CHAIR[l.hair], Offset(x, y - 0.3f * u), Size(3.4f * u, 1.3f * u))
    drawRect(Color(0xFF212121), Offset(x + 2.3f * u, y + 1.5f * u), Size(0.7f * u, 0.8f * u))
}

private fun DrawScope.sp(
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
        if (driver != null) drawDriver(driver, dx, dy, k * s, img.width.toFloat())
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

private fun DrawScope.label(p: Paint, txt: String, wx: Float, y: Float, cam: Float, s: Float, sz: Float, col: Int) {
    p.color = col
    p.textSize = sz * s
    p.textAlign = Paint.Align.CENTER
    drawContext.canvas.nativeCanvas.drawText(txt, (wx - cam) * s, y * s, p)
}

private val IDLE_LINES = listOf(
    "Será que hoje acho um carro bom?", "Preciso de dinheiro pra uma oficina...",
    "Esse bairro tem cada carro...", "Dizem que o Seu Zé pechincha fácil.",
    "Um carro bem barato e eu faço fortuna!", "Tá um dia bonito pra negociar.",
)
private val PED_LINES = listOf("Oi, {n}!", "Bom dia!", "Cuidado na rua!", "Tá procurando carro?", "Fala, {n}!", "Tá quente hoje, hein?")

private fun DrawScope.bubble(p: Paint, txt: String, wx: Float, bottomY: Float, cam: Float, s: Float) {
    p.textSize = 5.2f * s
    val tw = p.measureText(txt) / s
    val bw = tw + 6f
    val x = wx - bw / 2f
    wr(Color(0xFF212121), x - 0.6f, bottomY - 10.6f, bw + 1.2f, 10.2f, cam, s)
    wr(Color(0xFFFFFFFF), x, bottomY - 10f, bw, 9f, cam, s)
    wr(Color(0xFFFFFFFF), wx - 1.5f, bottomY - 1f, 3f, 2f, cam, s)
    label(p, txt, wx, bottomY - 3.4f, cam, s, 5.2f, 0xFF212121.toInt())
}

fun DrawScope.glow(center: Offset, radius: Float, c: Color) {
    drawCircle(
        Brush.radialGradient(listOf(c, c.copy(alpha = 0f)), center, radius), radius, center,
        1f, androidx.compose.ui.graphics.drawscope.Fill, null, BlendMode.Plus,
    )
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
    val peds = remember {
        List(22) { val r = Random(it + 5); Ped(randomLook(r), r.nextFloat() * WORLD, r.nextFloat() * 6f) }
    }
    val traffic = remember {
        List(14) {
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
    var say by remember { mutableStateOf<String?>("Bora achar uma pechincha!") }
    var sayUntil by remember { mutableFloatStateOf(4f) }
    val near by remember { derivedStateOf { YARD_X.indexOfFirst { abs(px - it) < 30f } } }

    DisposableEffect(Unit) { onDispose { prefs.edit().putFloat("x", px).apply() } }
    LaunchedEffect(Unit) {
        var last = 0L
        var idle = 0f
        var lastNear = -1
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
                    val nf = nightF(g.minutes)
                    if (nf > 0.6f && !saidNight) { saidNight = true; say = "Já escureceu... será que ainda dá tempo?"; sayUntil = time + 4f }
                    if (nf < 0.2f) saidNight = false
                    if (dir != 0 && talk == null && !taxi) {
                        px = (px + dir * (if (run) 135f else 75f) * dt).coerceIn(20f, WORLD - 20f)
                        face = dir
                    }
                }
                last = n
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val s = size.width / VIEW_W
            val oy = size.height - 150f * s
            val cam = (px - VIEW_W / 2f).coerceIn(0f, WORLD - VIEW_W)
            val t = time
            val night = nightF(g.minutes)
            val moving = dir != 0 && talk == null && !taxi

            // ---- céu
            val skyH = oy + 104f * s
            drawRect(
                Brush.verticalGradient(listOf(lerp(Color(0xFF5DB4EA), Color(0xFF080D2B), night), lerp(Color(0xFFD6EEFA), Color(0xFF1B2A55), night)), 0f, skyH),
                size = Size(size.width, skyH),
            )
            if (night < 0.7f) drawCircle(Color(0xFFFFF59D).copy(alpha = 1f - night), 9f * s, Offset(size.width * 0.78f, oy * 0.45f))
            for (i in 0 until 6) {
                val cx = (i * 83f + t * 3f) % (VIEW_W + 80f) - 40f
                val cy = oy * 0.12f + (i % 3) * 22f * s
                val ca = Color.White.copy(alpha = 0.9f * (1f - night * 0.8f))
                drawRect(ca, Offset(cx * s, cy), Size(30f * s, 5f * s))
                drawRect(ca, Offset((cx + 6f) * s, cy - 4f * s), Size(18f * s, 4f * s))
                drawRect(ca, Offset((cx + 12f) * s, cy - 7f * s), Size(8f * s, 3f * s))
            }

            // ---- mundo
            withTransform({ translate(0f, oy) }) {
                // skyline em duas camadas
                for (layer in 0..1) {
                    val par = if (layer == 0) 0.3f else 0.5f
                    val step = if (layer == 0) 52f else 36f
                    val fc = cam * par
                    var fx = (fc / step).toInt() * step - step
                    while (fx < fc + VIEW_W + step) {
                        val idx = (fx / step).toInt()
                        val hh = (if (layer == 0) 55f else 38f) + ((idx * 37) % 7 + 7) % 7 * 8f
                        drawRect(if (layer == 0) Color(0xFFBBD0E4) else Color(0xFF9DB7CC), Offset((fx - fc) * s, (104f - hh) * s), Size((step - 4f) * s, hh * s))
                        fx += step
                    }
                }
                // calçada, meio-fio, rua
                wr(Color(0xFFB7C4CB), cam, 104f, VIEW_W, 14f, cam, s)
                var sx = (cam / 14f).toInt() * 14f
                while (sx < cam + VIEW_W + 14f) { wr(Color(0xFFA2B1B9), sx, 104f, 0.6f, 14f, cam, s); sx += 14f }
                wr(Color(0xFF8A9AA3), cam, 117f, VIEW_W, 1.6f, cam, s)
                wr(Color(0xFF4A5E6A), cam, 118.6f, VIEW_W, 31.4f, cam, s)
                wr(Color(0xFF3E505B), cam, 140f, VIEW_W, 10f, cam, s)
                var dx = (cam / 26f).toInt() * 26f
                while (dx < cam + VIEW_W + 26f) { wr(Color(0xFFECEFF1), dx, 133.5f, 13f, 1.2f, cam, s); dx += 26f }

                // cidade
                for (it in city) {
                    if (it.x > cam + VIEW_W + 20f || it.x < cam - 110f) continue
                    when (it.kind) {
                        0 -> {
                            wr(Color(0x33000000), it.x, 101f, spr.bld[it.idx].width.toFloat(), 3f, cam, s)
                            sp(spr.bld[it.idx], it.x, 104f, cam, s)
                        }
                        1 -> sp(spr.tree[it.idx], it.x, 112f, cam, s)
                        2 -> sp(spr.lamp, it.x, 116f, cam, s)
                        3 -> sp(spr.sign[it.idx], it.x, 112f, cam, s, 0.6f)
                        4 -> {
                            sp(spr.tl, it.x, 116f, cam, s)
                            val ph = (t * 0.12f + it.x * 0.003f) % 1f
                            val li = if (ph < 0.45f) 2 else if (ph < 0.55f) 1 else 0
                            val lc = when (li) { 0 -> Color(0xFFFF1744); 1 -> Color(0xFFFFEA00); else -> Color(0xFF00E676) }
                            wr(lc, it.x + 3f, 116f - 30f + 3f + li * 4f, 2f, 2f, cam, s)
                        }
                        else -> sp(spr.bench, it.x, 113f, cam, s)
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

                // pedestres
                for (pd in peds) {
                    val a = t * 0.4f + pd.phase
                    val wx = pd.base + sin(a) * 60f
                    if (wx < cam - 20f || wx > cam + VIEW_W + 20f) continue
                    drawChar(pd.look, (wx - 6f - cam) * s, (114f - 26f) * s, s, cos(a) < 0f, 1f, t * 7f + pd.phase)
                }
                // jogador
                drawChar(look, (px - 6f - cam) * s, (115f - 26f) * s, s, face < 0, if (moving) 1f else 0f,
                    t * (if (run) 16f else 10f))
                val sayNow = say
                if (sayNow != null) bubble(paint, sayNow, px, 84f, cam, s)
                else label(paint, me.name, px, 84f, cam, s, 6f, 0xFFFFFFFF.toInt())
                for ((pi, pd) in peds.withIndex()) {
                    val a = t * 0.4f + pd.phase
                    val wx = pd.base + sin(a) * 60f
                    if (abs(wx - px) < 32f && ((t * 0.25f + pd.phase).toInt() % 3 == 0)) {
                        bubble(paint, PED_LINES[(pi + (t * 0.25f + pd.phase).toInt() / 3) % PED_LINES.size].replace("{n}", me.name), wx, 84f, cam, s)
                    }
                }

                // trânsito
                for (c in traffic) {
                    val x = (((c.x0 + t * c.speed) % WORLD) + WORLD) % WORLD
                    if (x < cam - 90f || x > cam + VIEW_W + 10f) continue
                    val img = spr.cars[c.spr]
                    val rot = (t * abs(c.speed) + c.x0) / 4.5f
                    if (c.lane == 0) sp(img, x, 133f, cam, s, 1.1f, false, null, 1f, c.spr, rot, c.driver)
                    else sp(img, x, 148f, cam, s, 1.1f, true, null, 1f, c.spr, rot, c.driver)
                }
            }

            // ---- noite
            if (night > 0f) {
                drawRect(Color(0xFF070B2A).copy(alpha = 0.62f * night), size = size)
                for (i in stars.indices) {
                    val tw = 0.5f + 0.5f * sin(t * 2f + i)
                    drawRect(Color.White.copy(alpha = night * tw), Offset(stars[i].x * size.width, stars[i].y * (oy + 60f * s)), Size(1.3f * s, 1.3f * s))
                }
                drawCircle(Color(0xFFF5F5DC).copy(alpha = night), 7f * s, Offset(size.width * 0.25f, oy * 0.4f))
                withTransform({ translate(0f, oy) }) {
                    for (it in city) {
                        if (it.x > cam + VIEW_W + 20f || it.x < cam - 110f) continue
                        if (it.kind == 0) sp(spr.win[it.idx], it.x, 104f, cam, s, 1f, false, null, night)
                        if (it.kind == 2) {
                            val c = Offset((it.x + 4.5f - cam) * s, 86f * s)
                            glow(c, 30f * s, Color(0x88FFD27A).copy(alpha = 0.55f * night))
                            glow(Offset(c.x, 114f * s), 22f * s, Color(0x55FFD27A).copy(alpha = 0.4f * night))
                        }
                    }
                    YARD_X.forEachIndexed { i, yx ->
                        if (yx + 100f < cam || yx - 100f > cam + VIEW_W) return@forEachIndexed
                        wr(Color(0xFFFFEB3B).copy(alpha = night), yx - 48f, 44f, 96f, 16f, cam, s)
                        label(paint, YARD_NAMES[i], yx, 56f, cam, s, 8f, 0xFF212121.toInt())
                        glow(Offset((yx - cam) * s, 52f * s), 60f * s, Color(0x66FFEB3B).copy(alpha = 0.5f * night))
                    }
                    for (c in traffic) {
                        val x = (((c.x0 + t * c.speed) % WORLD) + WORLD) % WORLD
                        if (x < cam - 90f || x > cam + VIEW_W + 10f) continue
                        val wcar = spr.cars[c.spr].width * 1.1f
                        val by = if (c.lane == 0) 133f else 148f
                        val fx = if (c.lane == 0) x + wcar - 2f else x + 2f
                        val sgn = if (c.lane == 0) 1f else -1f
                        glow(Offset((fx + sgn * 16f - cam) * s, (by - 6f) * s), 26f * s, Color(0xAAFFF3B0).copy(alpha = 0.8f * night))
                        glow(Offset((fx + sgn * 5f - cam) * s, (by - 6f) * s), 9f * s, Color(0xFFFFFFFF).copy(alpha = night))
                        val rx = if (c.lane == 0) x + 1f else x + wcar - 3f
                        wr(Color(0xFFFF1744).copy(alpha = night), rx, by - 9f, 2f, 2f, cam, s)
                    }
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
                } else {
                    Text("Explore a cidade", fontSize = 12.sp)
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
        YardTalk(g, yi, me.name, first, mask == 31, spr) { talk = null }
    }
    if (taxi) {
        Dialog(onDismissRequest = { taxi = false }) {
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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

private val ENDING = listOf(
    "Você conheceu todos os pátios da cidade, {n}. O Chico tinha razão sobre você.",
    "Toma a chave. É da oficina dele no centro. Agora ela é sua.",
    "Ele dizia que ferro velho não vende carro, vende segunda chance. Faz bom uso!",
)

@Composable
private fun YardTalk(g: GameState, yi: Int, pname: String, first: Boolean, allDone: Boolean, spr: CitySprites, onClose: () -> Unit) {
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
