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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

// ------------------------------------------------------------ mundo

private const val WORLD = 5000f
private const val SPEED = 75f
private val YARD_X = listOf(520f, 1450f, 2450f, 3500f, 4450f)

private val CSKIN = listOf(0xFFFFDBB4, 0xFFE8B98A, 0xFFC68642, 0xFF8D5524, 0xFF5C3A1E).map { Color(it) }
private val CHAIR = listOf(0xFF1B1B1B, 0xFF4E342E, 0xFF8D6E63, 0xFFE0B04F, 0xFFB71C1C, 0xFF9E9E9E).map { Color(it) }
private val CSHIRT = listOf(0xFF1E88E5, 0xFFE53935, 0xFF43A047, 0xFFFDD835, 0xFF8E24AA, 0xFFFB8C00, 0xFFECEFF1, 0xFF212121).map { Color(it) }
private val CPANTS = listOf(0xFF283593, 0xFF37474F, 0xFF5D4037, 0xFF212121, 0xFF1565C0).map { Color(it) }

class PlayerProfile(val name: String, val female: Boolean, val skin: Int, val hair: Int, val shirt: Int, val pants: Int) {
    fun look() = BuyerLook(female, CSKIN[skin], CHAIR[hair], CSHIRT[shirt], CPANTS[pants])
}

private fun loadProfile(p: SharedPreferences): PlayerProfile? {
    val n = p.getString("name", null) ?: return null
    return PlayerProfile(
        n, p.getBoolean("female", false), p.getInt("skin", 1).coerceIn(0, CSKIN.lastIndex),
        p.getInt("hair", 0).coerceIn(0, CHAIR.lastIndex), p.getInt("shirt", 0).coerceIn(0, CSHIRT.lastIndex),
        p.getInt("pants", 0).coerceIn(0, CPANTS.lastIndex),
    )
}

private fun saveProfile(p: SharedPreferences, c: PlayerProfile) {
    p.edit().putString("name", c.name).putBoolean("female", c.female).putInt("skin", c.skin)
        .putInt("hair", c.hair).putInt("shirt", c.shirt).putInt("pants", c.pants).apply()
}

class CitySprites(
    val bld: List<ImageBitmap>, val tree: List<ImageBitmap>, val lamp: ImageBitmap,
    val sign: List<ImageBitmap>, val cars: List<ImageBitmap>,
) {
    companion object {
        fun load(ctx: Context): CitySprites {
            val o = BitmapFactory.Options().apply { inScaled = false }
            fun img(n: String) = BitmapFactory.decodeStream(ctx.assets.open("city/$n.png"), null, o)!!.asImageBitmap()
            return CitySprites(
                List(12) { img("b$it") }, List(2) { img("t$it") }, img("lamp"),
                List(3) { img("sign$it") }, List(9) { img("car${it + 1}") },
            )
        }
    }
}

private fun carSpriteFor(b: Body): Int = when (b) {
    Body.BOXY -> 0; Body.COMPACT -> 1; Body.WAGON -> 2; Body.MPV -> 4; Body.HATCH -> 1
    Body.SEDAN -> 5; Body.SUV -> 6; Body.PICKUP -> 8; Body.COUPE -> 3; Body.CLASSIC -> 7
    Body.VAN -> 4; Body.BEETLE -> 1; Body.SUPER -> 3
}

// item da cidade: tipo (0 prédio, 1 árvore, 2 poste, 3 placa), índice do sprite, x
private class Item(val kind: Int, val idx: Int, val x: Float)
private class Ped(val look: BuyerLook, val base: Float, val phase: Float)
private class Traffic(val spr: Int, val x0: Float, val speed: Float, val lane: Int)

private fun genCity(spr: CitySprites): List<Item> {
    val r = Random(11)
    val out = ArrayList<Item>()
    var x = 20f
    while (x < WORLD - 100f) {
        val yx = YARD_X.firstOrNull { x + 80f > it - 110f && x < it + 110f }
        if (yx != null) { x = yx + 110f; continue }
        if (r.nextInt(100) < 18) { // pracinha
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
    return out
}

// ------------------------------------------------------------ desenho

private fun DrawScope.wr(c: Color, x: Float, y: Float, w: Float, h: Float, cam: Float, s: Float) {
    drawRect(c, Offset((x - cam) * s, y * s), Size(w * s, h * s))
}

private fun DrawScope.sp(
    img: ImageBitmap, wx: Float, by: Float, cam: Float, s: Float,
    k: Float = 1f, flip: Boolean = false, tint: Color? = null,
) {
    val w = img.width * k * s
    val h = img.height * k * s
    val dx = (wx - cam) * s
    val dy = by * s - h
    if (dx + w < 0f || dx > size.width) return
    val filter = tint?.let { ColorFilter.tint(it, BlendMode.SrcAtop) }
    if (flip) {
        withTransform({ scale(-1f, 1f, Offset(dx + w / 2f, dy)) }) {
            drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), IntOffset(dx.toInt(), dy.toInt()),
                IntSize(w.toInt().coerceAtLeast(1), h.toInt().coerceAtLeast(1)), 1f,
                androidx.compose.ui.graphics.drawscope.Fill, filter, BlendMode.SrcOver, FilterQuality.None)
        }
    } else {
        drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), IntOffset(dx.toInt(), dy.toInt()),
            IntSize(w.toInt().coerceAtLeast(1), h.toInt().coerceAtLeast(1)), 1f,
            androidx.compose.ui.graphics.drawscope.Fill, filter, BlendMode.SrcOver, FilterQuality.None)
    }
}

private val CMAN: List<String> = listOf(
    "..hhhh..", ".hhhhhh.", ".hssssh.", ".sesses.", ".ssssss.", "..ssss..", ".cccccc.",
    "sccccccs", "sccccccs", "sccccccs", ".pppppp.", ".pp..pp.", ".pp..pp.", ".oo..oo.",
)
private val CWOMAN: List<String> = listOf(
    "..hhhh..", ".hhhhhh.", "hhsssshh", "hsessesh", "hssssssh", "h.ssss.h", "hcccccch",
    "sccccccs", ".cccccc.", "..cccc..", ".pppppp.", ".pppppp.", ".ss..ss.", ".oo..oo.",
)

private fun DrawScope.person(look: BuyerLook, wx: Float, feetY: Float, cam: Float, s: Float, faceLeft: Boolean, bob: Float) {
    val rows: List<String> = if (look.female) CWOMAN else CMAN
    val cell = 1.8f * s
    val left = (wx - cam) * s - 4f * cell
    if (left < -cell * 10 || left > size.width) return
    val top = feetY * s - rows.size * cell - bob * s
    drawOval(Color(0x44000000), Offset(left, feetY * s - cell * 0.6f), Size(8 * cell, cell * 1.2f))
    rows.forEachIndexed { y: Int, row: String ->
        for (xi in 0 until 8) {
            val c = row[if (faceLeft) 7 - xi else xi]
            val col = when (c) {
                'h' -> look.hair
                's' -> look.skin
                'e' -> Color(0xFF212121)
                'c' -> look.shirt
                'p' -> look.pants
                'o' -> Color(0xFF3E2723)
                else -> null
            }
            if (col != null) drawRect(col, Offset(left + xi * cell, top + y * cell), Size(cell + 0.5f, cell + 0.5f))
        }
    }
}

private fun DrawScope.label(p: Paint, txt: String, wx: Float, y: Float, cam: Float, s: Float, sz: Float, col: Int) {
    p.color = col
    p.textSize = sz * s
    p.textAlign = Paint.Align.CENTER
    drawContext.canvas.nativeCanvas.drawText(txt, (wx - cam) * s, y * s, p)
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
        val spr = remember { CitySprites.load(ctx) }
        CityWorld(g, p, spr, prefs) { profile = null }
    }
}

@Composable
private fun CityWorld(g: GameState, me: PlayerProfile, spr: CitySprites, prefs: SharedPreferences, onEdit: () -> Unit) {
    val city = remember(spr) { genCity(spr) }
    val peds = remember {
        List(16) {
            val r = Random(it + 5)
            Ped(BuyerLook(r.nextBoolean(), CSKIN[r.nextInt(CSKIN.size)], CHAIR[r.nextInt(CHAIR.size)],
                CSHIRT[r.nextInt(CSHIRT.size)], CPANTS[r.nextInt(CPANTS.size)]), r.nextFloat() * WORLD, r.nextFloat() * 6f)
        }
    }
    val traffic = remember {
        List(12) {
            val r = Random(it + 40)
            val lane = it % 2
            Traffic(r.nextInt(9), r.nextFloat() * WORLD, (25 + r.nextInt(25)) * (if (lane == 0) 1f else -1f), lane)
        }
    }
    val paint = remember { Paint().apply { isAntiAlias = false; typeface = android.graphics.Typeface.DEFAULT_BOLD } }
    val look = remember(me) { me.look() }

    var px by remember { mutableFloatStateOf(prefs.getFloat("x", 400f)) }
    var time by remember { mutableFloatStateOf(0f) }
    var dir by remember { mutableIntStateOf(0) }
    var face by remember { mutableIntStateOf(1) }
    var mask by remember { mutableIntStateOf(prefs.getInt("mask", 0)) }
    var talk by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }
    var taxi by remember { mutableStateOf(false) }
    val near by remember { derivedStateOf { YARD_X.indexOfFirst { abs(px - it) < 30f } } }

    DisposableEffect(Unit) { onDispose { prefs.edit().putFloat("x", px).apply() } }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { n ->
                if (last != 0L) {
                    val dt = ((n - last) / 1e9f).coerceAtMost(0.05f)
                    time += dt
                    if (dir != 0 && talk == null && !taxi) {
                        px = (px + dir * SPEED * dt).coerceIn(20f, WORLD - 20f)
                        face = dir
                    }
                }
                last = n
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val s = size.height / 150f
            val viewW = size.width / s
            val cam = (px - viewW / 2f).coerceIn(0f, WORLD - viewW)
            val t = time
            drawRect(Brush.verticalGradient(listOf(Color(0xFF6EC1EE), Color(0xFFD6EEFA)), 0f, 110f * s), size = Size(size.width, 110f * s))
            // skyline distante (parallax)
            val farCam = cam * 0.5f
            var fx = (farCam / 34f).toInt() * 34f - 34f
            while (fx < farCam + viewW + 40f) {
                val hh = 35f + ((fx / 34f).toInt() * 37 % 7 + 7) % 7 * 7f
                drawRect(Color(0xFF9DB7CC), Offset((fx - farCam) * s, (104f - hh) * s), Size(30f * s, hh * s))
                fx += 34f
            }
            // chão
            wr(Color(0xFFB0BEC5), cam, 104f, viewW, 14f, cam, s)
            wr(Color(0xFF90A4AE), cam, 117f, viewW, 1.5f, cam, s)
            wr(Color(0xFF455A64), cam, 118.5f, viewW, 32f, cam, s)
            var dx = (cam / 26f).toInt() * 26f
            while (dx < cam + viewW + 26f) { wr(Color(0xFFECEFF1), dx, 133.5f, 13f, 1.2f, cam, s); dx += 26f }
            // cidade
            for (it in city) {
                if (it.x > cam + viewW + 20f || it.x < cam - 110f) continue
                when (it.kind) {
                    0 -> sp(spr.bld[it.idx], it.x, 104f, cam, s)
                    1 -> sp(spr.tree[it.idx], it.x, 112f, cam, s)
                    2 -> sp(spr.lamp, it.x, 116f, cam, s)
                    else -> sp(spr.sign[it.idx], it.x, 112f, cam, s, 0.6f)
                }
            }
            // ferros velhos
            YARD_X.forEachIndexed { i, yx ->
                if (yx + 100f < cam || yx - 100f > cam + viewW) return@forEachIndexed
                wr(Color(0xFF5D4037), yx - 85f, 72f, 170f, 32f, cam, s)
                val tint = Color(0x88665544)
                sp(spr.cars[(i * 2) % 9], yx - 82f, 104f, cam, s, 1f, false, tint)
                sp(spr.cars[(i * 3 + 1) % 9], yx - 76f, 84f, cam, s, 1f, true, tint)
                sp(spr.cars[(i * 5 + 2) % 9], yx + 24f, 104f, cam, s, 1f, i % 2 == 0, tint)
                sp(spr.cars[(i * 7 + 4) % 9], yx + 30f, 84f, cam, s, 0.9f, false, tint)
                wr(Color(0xFF9E9E9E), yx - 85f, 70f, 170f, 2f, cam, s)
                for (k in 0..16) wr(Color(0xFF757575), yx - 85f + k * 10.6f, 70f, 1.5f, 34f, cam, s)
                wr(Color(0xFF3E2723), yx - 13f, 76f, 26f, 28f, cam, s)
                wr(Color(0xFF6D4C41), yx - 11f, 78f, 22f, 26f, cam, s)
                wr(Color(0xFF212121), yx - 50f, 42f, 100f, 20f, cam, s)
                wr(Color(0xFFFFEB3B), yx - 48f, 44f, 96f, 16f, cam, s)
                label(paint, YARD_NAMES[i], yx, 56f, cam, s, 8f, 0xFF212121.toInt())
                wr(Color(0xFF212121), yx - 40f, 62f, 2f, 8f, cam, s)
                wr(Color(0xFF212121), yx + 38f, 62f, 2f, 8f, cam, s)
            }
            // pedestres
            for (pd in peds) {
                val ph = t * 0.4f + pd.phase
                val wx = pd.base + sin(ph) * 60f
                val cosv = kotlin.math.cos(ph)
                person(pd.look, wx, 114f, cam, s, cosv < 0f, abs(sin(t * 6f + pd.phase)) * 0.9f)
            }
            // jogador
            person(look, px, 115f, cam, s, face < 0, if (dir != 0 && talk == null) abs(sin(t * 10f)) * 1.2f else 0f)
            // trânsito
            for (c in traffic) {
                val x = (((c.x0 + t * c.speed) % WORLD) + WORLD) % WORLD
                val img = spr.cars[c.spr]
                if (c.lane == 0) sp(img, x, 133f, cam, s, 1.1f, false) else sp(img, x, 148f, cam, s, 1.1f, true)
            }
            // dia/noite
            val m = g.minutes
            val a = when {
                m < 5 * 60 -> 0.5f
                m < 7 * 60 -> 0.5f * (7 * 60 - m) / 120f
                m < 18 * 60 -> 0f
                m < 20 * 60 -> 0.5f * (m - 18 * 60) / 120f
                else -> 0.5f
            }
            if (a > 0f) drawRect(Color(0xFF0B1030).copy(alpha = a), size = size)
            label(paint, me.name, px, 84f, cam, s, 6f, 0xFFFFFFFF.toInt())
        }
        Row(Modifier.fillMaxWidth().padding(8.dp), Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
            HoldButton("◀", { dir = if (it) -1 else 0 }, Modifier.weight(1f).height(64.dp))
            Column(Modifier.weight(1.3f), horizontalAlignment = Alignment.CenterHorizontally) {
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
                    TextButton(onClick = { taxi = true }) { Text("🚕 Táxi", fontSize = 12.sp) }
                    TextButton(onClick = onEdit) { Text("🧍 Visual", fontSize = 12.sp) }
                }
            }
            HoldButton("▶", { dir = if (it) 1 else 0 }, Modifier.weight(1f).height(64.dp))
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

@Composable
private fun HoldButton(label: String, onHold: (Boolean) -> Unit, modifier: Modifier) {
    Box(
        modifier
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTapGestures(onPress = { onHold(true); tryAwaitRelease(); onHold(false) })
            },
        contentAlignment = Alignment.Center,
    ) { Text(label, fontSize = 28.sp) }
}

// ------------------------------------------------------------ criação de personagem

@Composable
private fun CharacterCreator(onDone: (PlayerProfile) -> Unit) {
    var name by remember { mutableStateOf("") }
    var female by remember { mutableStateOf(false) }
    var skin by remember { mutableIntStateOf(1) }
    var hair by remember { mutableIntStateOf(0) }
    var shirt by remember { mutableIntStateOf(0) }
    var pants by remember { mutableIntStateOf(0) }
    val look = BuyerLook(female, CSKIN[skin], CHAIR[hair], CSHIRT[shirt], CPANTS[pants])
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Crie seu personagem", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        PixelPerson(look, Modifier.size(96.dp, 168.dp))
        OutlinedTextField(name, { name = it.take(14) }, label = { Text("Nome") }, singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(!female, { female = false }, { Text("Homem") })
            FilterChip(female, { female = true }, { Text("Mulher") })
        }
        Swatches("Pele", CSKIN, skin) { skin = it }
        Swatches("Cabelo", CHAIR, hair) { hair = it }
        Swatches("Camisa", CSHIRT, shirt) { shirt = it }
        Swatches("Calça", CPANTS, pants) { pants = it }
        Button(onClick = { onDone(PlayerProfile(name.trim().ifBlank { "Você" }, female, skin, hair, shirt, pants)) }) {
            Text("Começar")
        }
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
                    Modifier.size(30.dp).background(c, shape)
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
    val lines = remember {
        (if (yi == 4 && allDone) ENDING else if (first) o.intro else listOf(o.again)).map { it.replace("{n}", pname) }
    }
    var idx by remember { mutableIntStateOf(0) }
    var speech by remember { mutableStateOf(lines[0]) }
    var picked by remember { mutableStateOf<YardCar?>(null) }
    val y = picked
    var cur by remember(y?.id) { mutableIntStateOf(y?.price ?: 0) }
    var patience by remember(y?.id) { mutableIntStateOf(3) }

    fun offer(pct: Int) {
        val c = picked ?: return
        if (patience <= 0) { speech = "Já deu, $pname. Esse preço é final."; return }
        val want = roundTo50(c.price * (100 - pct) / 100.0)
        val floor = roundTo50(c.price * (1 - o.maxDisc))
        if (want >= cur) { speech = "Isso é mais do que eu pedi. Pode pagar à vontade!"; return }
        if (want >= floor && Random.nextDouble() < o.easy + 0.35) {
            cur = want
            speech = "Tá bom, tá bom... fecho em ${brl(want)}."
        } else {
            patience -= 1
            if (cur > floor) cur = maxOf(floor, roundTo50(cur - c.price * 0.03))
            speech = if (patience <= 0) "Chega! Última oferta: ${brl(cur)}." else "Muito baixo! Faço por ${brl(cur)}."
        }
    }

    Dialog(onDismissRequest = onClose) {
        Card {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PixelPerson(o.look, Modifier.size(48.dp, 84.dp))
                    Column {
                        Text(o.name, fontWeight = FontWeight.Bold)
                        Text(speech, fontSize = 14.sp)
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
                            FilledTonalButton(onClick = { offer(pct) }, Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) { Text("-$pct%", fontSize = 13.sp) }
                        }
                    }
                    OutlinedButton(onClick = {
                        val w = y.cond.indices.minByOrNull { y.cond[it] } ?: 0
                        speech = "O pior dele é ${PART_NAMES[w].lowercase()}. O resto dá pro gasto."
                    }, Modifier.fillMaxWidth()) { Text("Perguntar do carro") }
                    Button(onClick = {
                        g.buy(yi, y.copy(price = cur))
                        if (g.yards.getOrNull(yi).orEmpty().any { it.id == y.id }) {
                            speech = g.message
                        } else {
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
