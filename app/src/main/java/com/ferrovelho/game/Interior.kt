package com.ferrovelho.game

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** Interiores estilo Game Boy: loja de carros, oficina e casa. Tudo ao vivo, com gente andando e comentando. */
enum class Room { SHOP, WORKSHOP, HOME }

private const val GW = 16f
private const val GH = 12f
private const val DOOR_X = 8f

private class Npc(
    val look: Look, var x: Float, var y: Float, val mechanic: Boolean = false,
) {
    var tx = x; var ty = y
    var state = 0            // 0 andando, 1 olhando, 2 saindo
    var timer = 0f
    var line = ""
    var face = 1
    var visits = 0
    var slot = -1
    var walking = false
}

// paleta Game Boy: escuro -> claro, aplicada em cima da cena inteira
private val GB_MATRIX = ColorMatrix(floatArrayOf(
    0.165f, 0.3245f, 0.0605f, 0f, 15f,
    0.155f, 0.3055f, 0.057f, 0f, 56f,
    0f, 0f, 0f, 0f, 15f,
    0f, 0f, 0f, 1f, 0f,
))

private fun slotX(i: Int) = 3f + (i % 3) * 5f
private fun slotY(i: Int) = if (i < 3) 6.7f else 10.6f

private fun wrap(s: String, n: Int = 17): String {
    val out = StringBuilder(); var cur = 0
    for (w in s.split(" ")) {
        if (cur + w.length > n && cur > 0) { out.append('\n'); cur = 0 } else if (cur > 0) { out.append(' '); cur++ }
        out.append(w); cur += w.length
    }
    return out.toString()
}

private fun DrawScope.tag(text: String, cx: Float, bottom: Float, ts: Float, bg: Color, fg: Color, border: Color) {
    val p = Paint().apply { isAntiAlias = false; textSize = ts; typeface = Typeface.MONOSPACE; isFakeBoldText = true; color = fg.toArgb() }
    val lines = text.split("\n")
    val w = lines.maxOf { p.measureText(it) } + ts * 0.9f
    val h = lines.size * ts * 1.15f + ts * 0.55f
    val x = (cx - w / 2f).coerceIn(3f, max(3f, size.width - w - 3f))
    val y = bottom - h
    drawRect(border, Offset(x - 2f, y - 2f), Size(w + 4f, h + 4f))
    drawRect(bg, Offset(x, y), Size(w, h))
    drawIntoCanvas { c -> lines.forEachIndexed { i, l -> c.nativeCanvas.drawText(l, x + ts * 0.45f, y + ts * 0.3f + (i + 1) * ts * 1.1f, p) } }
}

private fun commentFor(g: GameState, c: OwnedCar, rnd: Random): String {
    val nm = CATALOG[c.model].fullName
    val ask = g.stock["c${c.id}"] ?: 0
    val worth = max(1, g.marketValue(c))
    val r = if (ask > 0) ask.toDouble() / worth else 1.0
    val avg = c.cond.average()
    val pool = mutableListOf<String>()
    if (r < 0.98) pool += listOf("Preço bom nesse $nm!", "Tá barato esse $nm, hein!")
    else if (r > 1.12) pool += listOf("$nm por ${brl(ask)}? Salgado...", "Hmm, tá caro. Dá pra negociar?")
    else pool += listOf("Bonito esse $nm.", "Será que o motor é bom?", "Esse $nm tem cara de novo.")
    if (c.tuning.isNotEmpty()) pool += listOf("Que carro tunado!", "Gostei desse visual esportivo.")
    if (avg < 50) pool += "Tá meio judiado, né?"
    return wrap(pool[rnd.nextInt(pool.size)])
}

@Composable
fun InteriorDialog(g: GameState, room: Room, onSleep: () -> Unit = {}, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val myLook = remember { loadProfile(ctx.getSharedPreferences("city", android.content.Context.MODE_PRIVATE))?.look ?: randomLook(Random(1)) }
    val rnd = remember { Random(System.nanoTime()) }
    var gb by remember { mutableStateOf(true) }
    var tick by remember { mutableIntStateOf(0) }
    var selCar by remember { mutableStateOf<OwnedCar?>(null) }
    var panel by remember { mutableStateOf(false) }
    var homeSub by remember { mutableStateOf(false) }

    // carros mostrados
    val cars: List<OwnedCar> = when (room) {
        Room.SHOP -> g.garage.filter { g.stock.containsKey("c${it.id}") }.take(6)
        Room.WORKSHOP -> g.garage.take(5)
        else -> emptyList()
    }
    val npcs = remember { mutableListOf<Npc>() }
    val me = remember { Npc(myLook, 8f, 10.2f) }
    var target by remember { mutableStateOf<Offset?>(null) }
    var pending by remember { mutableIntStateOf(-1) }       // índice do carro (ou 100 = cama) a abrir ao chegar
    var spawnT by remember { mutableFloatStateOf(1.5f) }
    var time by remember { mutableFloatStateOf(0f) }
    var closed by remember { mutableStateOf(false) }
    val shopOpen = g.minutes in (8 * 60) until (20 * 60)

    fun spawnCustomer(inside: Boolean) {
        if (cars.isEmpty()) return
        val n = Npc(randomLook(Random(rnd.nextInt())), DOOR_X, 11.8f)
        n.slot = rnd.nextInt(cars.size)
        n.tx = slotX(n.slot) + (if (rnd.nextBoolean()) 1.3f else -1.3f)
        n.ty = min(11.3f, slotY(n.slot) + 0.9f)
        if (inside) { n.x = n.tx + rnd.nextFloat() * 0.01f; n.y = n.ty; n.state = 1; n.timer = 1f + rnd.nextFloat() * 3f; n.line = commentFor(g, cars[n.slot], rnd) }
        npcs.add(n)
    }

    LaunchedEffect(room, cars.size) {
        npcs.clear()
        if (room == Room.SHOP && shopOpen) repeat(min(2, cars.size)) { spawnCustomer(true) }
        if (room == Room.WORKSHOP) {
            val m = Npc(randomLook(Random(77)), 12f, 9f, mechanic = true)
            m.slot = 0; m.tx = slotX(0) + 1.3f; m.ty = slotY(0) + 0.9f; npcs.add(m)
        }
    }

    LaunchedEffect(room) {
        var last = 0L
        while (true) {
            withFrameNanos { ns ->
                val dt = if (last == 0L) 0f else ((ns - last) / 1e9f).coerceAtMost(0.05f)
                last = ns
                time += dt
                // jogador
                val tg = target
                me.walking = false
                if (tg != null) {
                    val dx = tg.x - me.x; val dy = tg.y - me.y
                    val d = sqrt(dx * dx + dy * dy)
                    if (d < 0.15f) {
                        target = null
                        if (pending >= 0) {
                            when {
                                room == Room.HOME && pending == 100 -> homeSub = true
                                room == Room.SHOP -> selCar = cars.getOrNull(pending)
                                room == Room.WORKSHOP -> panel = true
                            }
                            pending = -1
                        }
                    } else {
                        val st = min(d, 5.5f * dt)
                        me.x += dx / d * st; me.y += dy / d * st
                        if (abs(dx) > 0.05f) me.face = if (dx < 0) -1 else 1
                        me.walking = true
                    }
                }
                if (!closed && me.y > 11.55f && abs(me.x - DOOR_X) < 1.5f) { closed = true; onClose() }
                // NPCs
                if (room == Room.SHOP && shopOpen && cars.isNotEmpty()) {
                    spawnT -= dt
                    if (spawnT <= 0f) { if (npcs.size < 4) spawnCustomer(false); spawnT = 4f + rnd.nextFloat() * 5f }
                }
                val it = npcs.iterator()
                while (it.hasNext()) {
                    val n = it.next()
                    n.walking = false
                    if (n.state == 0 || n.state == 2) {
                        val dx = n.tx - n.x; val dy = n.ty - n.y
                        val d = sqrt(dx * dx + dy * dy)
                        if (d < 0.12f) {
                            if (n.state == 2) { it.remove(); continue }
                            n.state = 1; n.timer = 3.5f + rnd.nextFloat() * 2f
                            val c = cars.getOrNull(n.slot)
                            n.line = if (n.mechanic) listOf("Esse aí tá pedindo óleo!", "Tá quase pronto, chefe!", "Tem ferrugem aqui...", "Chefe, cadê o café?").random(rnd)
                            else if (c != null) commentFor(g, c, rnd) else ""
                        } else {
                            val st = min(d, 3.2f * dt)
                            n.x += dx / d * st; n.y += dy / d * st
                            if (abs(dx) > 0.03f) n.face = if (dx < 0) -1 else 1
                            n.walking = true
                        }
                    } else {
                        n.timer -= dt
                        if (n.timer <= 0f) {
                            n.line = ""; n.visits++
                            if (n.mechanic) {
                                n.slot = rnd.nextInt(max(1, cars.size + (if (room == Room.WORKSHOP) 1 else 0)))
                                n.tx = slotX(n.slot) + 1.3f; n.ty = min(11.3f, slotY(n.slot) + 0.9f); n.state = 0
                            } else if (n.visits < 2 && rnd.nextBoolean() && cars.isNotEmpty()) {
                                n.slot = rnd.nextInt(cars.size)
                                n.tx = slotX(n.slot) + (if (rnd.nextBoolean()) 1.3f else -1.3f); n.ty = min(11.3f, slotY(n.slot) + 0.9f); n.state = 0
                            } else { n.state = 2; n.tx = DOOR_X; n.ty = 11.9f }
                        }
                    }
                }
                tick++
            }
        }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when (room) { Room.SHOP -> "🏬 ${g.empName(0)}"; Room.WORKSHOP -> "🔧 ${g.motorsName()}"; else -> "🏠 Minha casa" },
                        Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    )
                    TextButton(onClick = { gb = !gb }) { Text(if (gb) "🎮 GB" else "🎨 Cor", fontSize = 12.sp) }
                    TextButton(onClick = onClose) { Text("✖ Sair") }
                }
                Text("${g.dateText()}  •  ${brl(g.money)}", fontSize = 11.sp)
                if (g.message.isNotEmpty()) Text(g.message, Modifier.fillMaxWidth(), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                Canvas(
                    Modifier.fillMaxWidth().aspectRatio(GW / GH).pointerInput(room, cars.size) {
                        detectTapGestures { off ->
                            val u = size.width / GW
                            val wx = off.x / u; val wy = off.y / u
                            var hit = -1
                            if (room == Room.HOME) {
                                if (abs(wx - 3.3f) < 2.3f && wy in 2.8f..6.4f) { hit = 100; target = Offset(5.9f, 6.2f) }
                            } else {
                                for (i in 0 until (if (room == Room.WORKSHOP) cars.size else cars.size)) {
                                    if (abs(wx - slotX(i)) < 2.8f && wy in (slotY(i) - 2.6f)..(slotY(i) + 0.4f)) {
                                        hit = i; target = Offset(slotX(i) + 1.3f, min(11.3f, slotY(i) + 0.9f)); break
                                    }
                                }
                            }
                            pending = hit
                            if (hit < 0) target = Offset(wx.coerceIn(0.8f, 15.2f), wy.coerceIn(if (room == Room.HOME) 5.2f else 7.0f, 11.9f))
                        }
                    },
                ) {
                    @Suppress("UNUSED_EXPRESSION") tick
                    val u = size.width / GW
                    drawIntoCanvas { cv ->
                        val layer = androidx.compose.ui.graphics.Paint().apply { if (gb) colorFilter = ColorFilter.colorMatrix(GB_MATRIX) }
                        cv.saveLayer(Rect(0f, 0f, size.width, size.height), layer)
                        drawRoom(g, room, u, time, cars.size)
                        // coisas ordenadas por profundidade (y)
                        val items = mutableListOf<Pair<Float, () -> Unit>>()
                        val ku = u * 5.2f / 62f
                        val cu = u * 2.5f / 26f
                        fun drawCarAt(c: OwnedCar?, i: Int, body: Body? = null, label: String? = null) {
                            val cx = slotX(i); val by = slotY(i)
                            items.add(by to {
                                val b = body ?: CATALOG[c!!.model].body
                                val paint = Color(PAINTS[(c?.color ?: 3).coerceIn(0, PAINTS.lastIndex)].second)
                                withTransform({ translate((cx - 31f * 5.2f / 62f * 0f) * u - 31f * ku, by * u - 22.5f * ku); scale(ku, ku, Offset.Zero) }) {
                                    drawCarUnits(shapeFor(b), b.ordinal * 17 + 3, paint, c?.tuning ?: emptyList(), c?.cond ?: List(5) { 45 }, time % 1.6f / 1.6f, null)
                                }
                                val txt = label ?: run {
                                    val car = c!!
                                    if (room == Room.SHOP) "${CATALOG[car.model].fullName.take(15)}\n${brl(g.stock["c${car.id}"] ?: 0)}"
                                    else "${CATALOG[car.model].fullName.take(15)}\nCond ${car.cond.average().toInt()}%"
                                }
                                tag(txt, cx * u, (by - 2.3f) * u, u * 0.5f, Color(0xFFFFFFE0), Color(0xFF202020), Color(0xFF202020))
                            })
                        }
                        cars.forEachIndexed { i, c -> drawCarAt(c, i) }
                        if (room == Room.WORKSHOP) drawCarAt(null, 5, Body.UNO, "Cliente\nem serviço")
                        fun person(n: Npc) = items.add(n.y to {
                            val ph = time * (if (n.walking) 10f else 1.5f)
                            drawChar(n.look, (n.x - 6f * cu / u) * u, (n.y * u) - 24.8f * cu, cu, n.face < 0, if (n.walking) 1f else 0f, ph)
                            if (n.mechanic && n.state == 1 && sin(time * 18f) > 0f) {
                                val sx = (n.x + 0.8f * n.face) * u; val sy = (n.y - 0.9f) * u
                                drawRect(Color(0xFFFFF176), Offset(sx, sy), Size(u * 0.18f, u * 0.18f))
                                drawRect(Color(0xFFFF9800), Offset(sx + u * 0.3f, sy - u * 0.2f), Size(u * 0.12f, u * 0.12f))
                            }
                        })
                        npcs.forEach { person(it) }
                        person(me)
                        items.sortBy { it.first }
                        items.forEach { it.second() }
                        // balões por cima de tudo
                        npcs.forEach { n ->
                            if (n.state == 1 && n.line.isNotEmpty()) tag(n.line, n.x * u, (n.y - 2.7f) * u, u * 0.46f, Color.White, Color.Black, Color.Black)
                        }
                        cv.restore()
                    }
                }
                val hint = when (room) {
                    Room.SHOP -> if (cars.isEmpty()) "Nenhum carro à venda. Ponha carros da garagem à venda no escritório." else if (!shopOpen) "Fechado (8h às 20h). Os clientes voltam no horário." else "Toque no chão pra andar • toque num carro pra mudar o preço • saia pelo tapete da porta."
                    Room.WORKSHOP -> "Toque num carro seu pra ir até ele e consertar (de graça) • saia pelo tapete da porta."
                    else -> "Toque na cama pra dormir • saia pelo tapete da porta."
                }
                Text(hint, fontSize = 12.sp)
                if (room == Room.SHOP) Button(onClick = { panel = true }, Modifier.fillMaxWidth()) { Text("📋 Escritório / atender clientes") }
                if (room == Room.WORKSHOP) Button(onClick = { panel = true }, Modifier.fillMaxWidth()) { Text("🔧 Abrir painel da oficina") }
            }
        }
    }

    selCar?.let { car ->
        AlertDialog(
            onDismissRequest = { selCar = null },
            confirmButton = { TextButton(onClick = { selCar = null }) { Text("Fechar") } },
            title = { Text("${CATALOG[car.model].fullName} ${car.year}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CarArt(CATALOG[car.model].body, car.color, car.tuning, car.cond, Modifier.fillMaxWidth().height(90.dp))
                    Text("Investido ${brl(car.paid + car.spent)} • vale hoje ${brl(g.marketValue(car))}", fontSize = 12.sp)
                    PriceRow(g, "c${car.id}", roundTo50(g.marketValue(car) * 1.12))
                }
            },
        )
    }
    if (panel) {
        if (room == Room.SHOP) CompanyDialog(g, 0) { panel = false } else WorkshopDialog(g) { panel = false }
    }
    if (homeSub) HomeDialog(g, onSleep = { homeSub = false; onSleep() }, onClose = { homeSub = false })
}

// ------------------------------------------------------------------ cenário

private fun DrawScope.drawRoom(g: GameState, room: Room, u: Float, t: Float, nCars: Int) {
    fun r(c: Long, x: Float, y: Float, w: Float, h: Float) = drawRect(Color(c), Offset(x * u, y * u), Size(w * u + 1f, h * u + 1f))
    when (room) {
        Room.SHOP -> {
            r(0xFFE9DDB5, 0f, 0f, GW, 4f)
            r(0xFFC9B98A, 0f, 3.5f, GW, 0.5f)
            for (i in 0 until 3) { r(0xFF8EC9F0, 1.2f + i * 5.2f, 0.7f, 3.2f, 1.9f); r(0xFFFFFFFF, 1.2f + i * 5.2f, 1.6f, 3.2f, 0.12f) }
            for (iy in 4 until 12) for (ix in 0 until 16) r(if ((ix + iy) % 2 == 0) 0xFFD9D9D9 else 0xFFB9BEC4, ix.toFloat(), iy.toFloat(), 1f, 1f)
            r(0xFFC62828, 6.8f, 11.4f, 2.4f, 0.6f)
            tag("FERROVELHO MOTORS", GW / 2f * u, 0.65f * u, u * 0.6f, Color(0xFFD32F2F), Color.White, Color(0xFF3E2723))
        }
        Room.WORKSHOP -> {
            r(0xFF7A828C, 0f, 0f, GW, 4f)
            r(0xFF5E656E, 0f, 3.5f, GW, 0.5f)
            // painel de ferramentas
            r(0xFF3B3F45, 1f, 0.7f, 6f, 2.4f)
            listOf(0xFFE53935, 0xFFFDD835, 0xFF90A4AE, 0xFF1E88E5, 0xFF43A047).forEachIndexed { i, c -> r(c, 1.4f + i * 1.1f, 1f, 0.5f, 1.8f) }
            r(0xFFC62828, 9.5f, 0.8f, 3.5f, 2.3f); r(0xFFB71C1C, 9.5f, 1.9f, 3.5f, 0.15f)
            for (iy in 4 until 12) for (ix in 0 until 16) r(if ((ix + iy) % 2 == 0) 0xFF8A8F96 else 0xFF7C8189, ix.toFloat(), iy.toFloat(), 1f, 1f)
            for (i in 0 until 6) { r(0xFFFFD600, slotX(i) - 2.7f, slotY(i) + 0.05f, 5.4f, 0.12f) }
            r(0xFFFFD600, 6.8f, 11.4f, 2.4f, 0.6f)
        }
        Room.HOME -> {
            val night = g.minutes < 6 * 60 || g.minutes >= 18 * 60
            r(0xFFF1D9C4, 0f, 0f, GW, 4.6f)
            r(0xFFD2B79F, 0f, 4.1f, GW, 0.5f)
            r(0xFF5D4037, 10.4f, 0.7f, 4.2f, 2.8f)
            r(if (night) 0xFF14204A else 0xFF9AD7FF, 10.6f, 0.9f, 3.8f, 2.4f)
            if (night) { r(0xFFFFFFFF, 11.2f, 1.4f, 0.15f, 0.15f); r(0xFFFFFFFF, 13f, 2f, 0.15f, 0.15f); r(0xFFFFF59D, 13.1f, 1.1f, 0.6f, 0.6f) }
            r(0xFF5D4037, 12.4f, 0.9f, 0.2f, 2.4f)
            for (iy in 5 until 12) for (ix in 0 until 16) r(if (ix % 2 == 0) 0xFFB5835A else 0xFFA97550, ix.toFloat(), iy.toFloat(), 1f, 1f)
            r(0xFF6D4C41, 1f, 2.4f, 4.6f, 3.6f)                 // cama
            r(0xFF1E88E5, 1.2f, 3.4f, 4.2f, 2.5f)
            r(0xFFFFFFFF, 1.4f, 2.6f, 1.6f, 1f)
            r(0xFFCFD8DC, 7.6f, 1.2f, 1.6f, 3.3f)               // geladeira
            r(0xFF90A4AE, 7.6f, 2.6f, 1.6f, 0.1f)
            r(0xFF263238, 6.9f, 7.2f, 4f, 2.2f)                 // tapete + sofá
            r(0xFFEF5350, 6f, 8.9f, 6f, 2.2f)
            r(0xFF212121, 12.2f, 5.1f, 2.6f, 1.8f)              // tv
            r(0xFF4FC3F7, 12.4f, 5.3f, 2.2f, 1.4f)
            r(0xFF8D6E63, 12f, 6.9f, 3f, 0.4f)
            r(0xFFC62828, 6.8f, 11.4f, 2.4f, 0.6f)
        }
    }
}
