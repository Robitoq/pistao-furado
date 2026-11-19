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
enum class Room { SHOP, WORKSHOP, HOME, STORE, PARK, BEACH }

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
    var name = ""
    var wander = false
    var friend = false
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
fun InteriorDialog(g: GameState, room: Room, onSleep: () -> Unit = {}, storeIdx: Int = 0, title: String? = null, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("city", android.content.Context.MODE_PRIVATE) }
    val profile = remember { loadProfile(prefs) }
    val myLook = profile?.look ?: remember { randomLook(Random(1)) }
    val myName = profile?.name ?: "amigo"
    var friends by remember { mutableStateOf(loadFriends(prefs)) }
    var chatNpc by remember { mutableStateOf<Npc?>(null) }
    var pendNpc by remember { mutableStateOf<Npc?>(null) }
    var zonePanel by remember { mutableStateOf(false) }
    var storePanel by remember { mutableStateOf(false) }
    var callDlg by remember { mutableStateOf(false) }
    var act by remember { mutableIntStateOf(0) }          // 1 mergulho, 2 sol, 3 vento, 4 banho
    var actPhase by remember { mutableIntStateOf(0) }
    var actT by remember { mutableFloatStateOf(0f) }
    val store = if (room == Room.STORE) STORES[storeIdx] else null
    val AMBIENT = listOf("Que dia bonito!", "Tá um calorão...", "Será que vai chover?", "Adoro esse lugar.", "Hmm, que fome...", "Me paga um sorvete?")
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
        if (room == Room.PARK || room == Room.BEACH || room == Room.STORE) {
            val rr = Random(System.nanoTime())
            val area = if (room == Room.BEACH) 4.8f else 7.4f
            val fr = friends.filter { it.value >= FRIEND_LVL }.keys.shuffled(rr).take(2)
            val others = NPC_NAMES.filter { it !in friends.keys }.shuffled(rr).take(if (room == Room.STORE) 2 else 4)
            (fr + others).forEach { nm ->
                val n = Npc(randomLook(Random(nm.hashCode())), 2f + rr.nextFloat() * 12f, area + rr.nextFloat() * (11.5f - area))
                n.name = nm; n.wander = true; n.friend = nm in fr; n.tx = n.x; n.ty = n.y; n.state = 1; n.timer = rr.nextFloat() * 3f
                npcs.add(n)
            }
        }
        if (room == Room.STORE && store != null) {
            val c = Npc(randomLook(Random(storeIdx * 7 + 1)), 8f, 4.7f); c.name = "Vendedor"; c.state = 1; c.timer = 1e9f
            c.line = store.greet.random(); npcs.add(c)
        }
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
                                room == Room.STORE && pending == 300 -> storePanel = true
                                (room == Room.PARK || room == Room.BEACH) && pending == 301 -> zonePanel = true
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
                val pn = pendNpc
                if (pn != null) {
                    val dd = sqrt((pn.x - me.x) * (pn.x - me.x) + (pn.y - me.y) * (pn.y - me.y))
                    if (dd < 1.7f) { target = null; pendNpc = null; if (pn.name == "Vendedor") storePanel = true else chatNpc = pn; pn.face = if (me.x < pn.x) -1 else 1 }
                    else target = Offset(pn.x - 1.3f * (if (me.x < pn.x) 1 else -1), pn.y)
                }
                if (act != 0) {
                    if (actPhase == 1 && target == null) { actPhase = 2; actT = 0f }
                    if (actPhase == 2) {
                        actT += dt
                        if (actT > 3.2f) {
                            when (act) {
                                1 -> { g.thirst = max(0.0, g.thirst - 5.0); g.spendTime(45); g.message = "Mergulho refrescante! 🌊" }
                                2 -> { g.spendTime(30); g.message = "Hora de relaxar no sol ☀️" }
                                3 -> { g.spendTime(20); g.message = "O vento passou. Dá pra voltar a negociar 💨" }
                                4 -> { g.spendTime(10); g.message = "Banho tomado, novo em folha 🚿" }
                            }
                            act = 0; actPhase = 0
                        }
                    }
                }
                if (!closed && (room == Room.SHOP || room == Room.WORKSHOP || room == Room.HOME || room == Room.STORE) && me.y > 11.55f && abs(me.x - DOOR_X) < 1.5f) { closed = true; onClose() }
                // NPCs
                if (room == Room.SHOP && shopOpen && cars.isNotEmpty()) {
                    spawnT -= dt
                    if (spawnT <= 0f) { if (npcs.size < 4) spawnCustomer(false); spawnT = 4f + rnd.nextFloat() * 5f }
                }
                val it = npcs.iterator()
                while (it.hasNext()) {
                    val n = it.next()
                    n.walking = false
                    if (n.wander) {
                        if (n.state == 1) {
                            n.timer -= dt
                            if (n.timer <= 0f) {
                                n.line = ""
                                val lo = if (room == Room.BEACH) 4.8f else if (room == Room.HOME) 7f else 7.2f
                                n.tx = 0.8f + rnd.nextFloat() * 14.4f; n.ty = lo + rnd.nextFloat() * (11.4f - lo); n.state = 0
                            }
                        } else {
                            val dx = n.tx - n.x; val dy = n.ty - n.y
                            val d = sqrt(dx * dx + dy * dy)
                            if (d < 0.12f) {
                                n.state = 1; n.timer = 2.5f + rnd.nextFloat() * 3f
                                n.line = if (n.friend) "Fala, $myName!" else if (rnd.nextInt(3) == 0) AMBIENT.random(rnd) else ""
                            } else {
                                val st = min(d, 2.2f * dt)
                                n.x += dx / d * st; n.y += dy / d * st
                                if (abs(dx) > 0.03f) n.face = if (dx < 0) -1 else 1
                                n.walking = true
                            }
                        }
                        continue
                    }
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
                        when (room) { Room.SHOP -> "🏬 ${g.empName(0)}"; Room.WORKSHOP -> "🔧 ${g.motorsName()}"; Room.STORE -> store!!.title; Room.PARK -> "🌳 Parque Central"; Room.BEACH -> "🏖️ Praia"; else -> title ?: "🏠 Minha casa" },
                        Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    )
                    TextButton(onClick = { gb = !gb }) { Text(if (gb) "🎮 GB" else "🎨 Cor", fontSize = 12.sp) }
                    TextButton(onClick = onClose) { Text("✖ Sair") }
                }
                Text("${g.dateText()}  •  ${brl(g.money)}  •  🍔 ${g.hunger.toInt()}%  🥤 ${g.thirst.toInt()}%  •  👥 ${friends.count { it.value >= FRIEND_LVL }} amigos", fontSize = 11.sp)
                if (g.message.isNotEmpty()) Text(g.message, Modifier.fillMaxWidth(), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                Canvas(
                    Modifier.fillMaxWidth().aspectRatio(GW / GH).pointerInput(room, cars.size) {
                        detectTapGestures { off ->
                            val u = size.width / GW
                            val wx = off.x / u; val wy = off.y / u
                            var hit = -1
                            val tn = npcs.filter { it.name.isNotEmpty() && abs(it.x - wx) < 1.2f && wy in (it.y - 2.8f)..(it.y + 0.4f) }.minByOrNull { abs(it.x - wx) }
                            if (act != 0) return@detectTapGestures
                            if (tn != null) { pendNpc = tn; pending = -1; return@detectTapGestures }
                            pendNpc = null
                            if (room == Room.STORE) {
                                if (wx in 4.5f..11.5f && wy in 2.4f..6f) { hit = 300; target = Offset(8f, 6.6f) }
                            } else if (room == Room.PARK) {
                                if ((wx in 6f..10f && wy in 3.2f..6.6f) || (wx in 12f..15.5f && wy in 3.2f..6.4f)) { hit = 301; target = Offset(wx.coerceIn(6.5f, 14f), 7.4f) }
                            } else if (room == Room.BEACH) {
                                if (wx in 11.5f..15.5f && wy in 3.2f..6.8f) { hit = 301; target = Offset(13f, 7.2f) }
                            } else if (room == Room.HOME) {
                                if (abs(wx - 3.3f) < 2.3f && wy in 2.8f..6.4f) { hit = 100; target = Offset(5.9f, 6.2f) }
                            } else {
                                for (i in 0 until (if (room == Room.WORKSHOP) cars.size else cars.size)) {
                                    if (abs(wx - slotX(i)) < 2.8f && wy in (slotY(i) - 2.6f)..(slotY(i) + 0.4f)) {
                                        hit = i; target = Offset(slotX(i) + 1.3f, min(11.3f, slotY(i) + 0.9f)); break
                                    }
                                }
                            }
                            pending = hit
                            if (hit < 0) target = Offset(wx.coerceIn(0.8f, 15.2f), wy.coerceIn(if (room == Room.HOME) 5.2f else if (room == Room.BEACH) 4.8f else 7.0f, 11.9f))
                        }
                    },
                ) {
                    @Suppress("UNUSED_EXPRESSION") tick
                    val u = size.width / GW
                    drawIntoCanvas { cv ->
                        val layer = androidx.compose.ui.graphics.Paint().apply { if (gb) colorFilter = ColorFilter.colorMatrix(GB_MATRIX) }
                        cv.saveLayer(Rect(0f, 0f, size.width, size.height), layer)
                        drawRoom(g, room, u, time, cars.size, store?.sign ?: "")
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
                        if (act != 0 && actPhase == 2) {
                            val bob = sin(time * 4f) * 0.15f
                            when (act) {
                                1 -> items.add(me.y to {
                                    drawChar(myLook, (me.x - 6f * cu / u) * u, (me.y + bob) * u - 24.8f * cu, cu, me.face < 0, 1f, time * 6f)
                                    drawRect(Color(0xAA3FA9F5), Offset((me.x - 1.3f) * u, (me.y - 1.2f + bob) * u), Size(2.6f * u, 1.5f * u))
                                })
                                2 -> items.add(me.y to {
                                    drawRect(Color(0xFFE53935), Offset((me.x - 2f) * u, (me.y - 0.9f) * u), Size(4f * u, 1.1f * u))
                                    drawRect(Color(0xFFFFFFFF), Offset((me.x - 2f) * u, (me.y - 0.55f) * u), Size(4f * u, 0.2f * u))
                                    drawCircle(Color(CSKIN[myLook.skin].value), 0.45f * u, Offset((me.x + 2.2f) * u, (me.y - 0.5f) * u))
                                    drawRect(Color(CTOP[myLook.top].value), Offset((me.x - 1.2f) * u, (me.y - 0.8f) * u), Size(2.4f * u, 0.55f * u))
                                    drawCircle(Color(0xFFFFEB3B), 0.55f * u, Offset((me.x - 2.5f) * u, (me.y - 3.4f) * u))
                                })
                                3 -> items.add(me.y to {
                                    drawChar(myLook, (me.x - 6f * cu / u) * u + sin(time * 12f) * u * 0.08f, me.y * u - 24.8f * cu, cu, false, 0f, time * 3f)
                                })
                                4 -> items.add(me.y to {
                                    drawChar(myLook, (me.x - 6f * cu / u) * u, me.y * u - 24.8f * cu, cu, me.face < 0, 0f, 0f)
                                    for (k in 0 until 6) drawRect(Color(0xCC4FC3F7), Offset((me.x - 1f + k * 0.4f) * u, ((me.y - 3.4f) + ((time * 5f + k * 0.37f) % 1f) * 3f) * u), Size(0.1f * u, 0.45f * u))
                                })
                            }
                        } else person(me)
                        if (act == 3 && actPhase == 2) for (k in 0 until 9) {
                            val yy = (4.5f + k * 0.85f) * u; val xx = (((time * 9f + k * 2.3f) % 18f) - 1f) * u
                            drawRect(Color(0xCCFFFFFF), Offset(xx, yy), Size(2.2f * u, 0.07f * u))
                        }
                        items.sortBy { it.first }
                        items.forEach { it.second() }
                        // balões por cima de tudo
                        if (act != 0 && actPhase == 2) tag(when (act) { 1 -> "Que água gelada!"; 2 -> "Ahh, que sol bom..."; 3 -> "Esperando o vento passar..."; else -> "Banho quentinho!" }, me.x * u, (me.y - 2.8f) * u, u * 0.46f, Color.White, Color.Black, Color.Black)
                        npcs.filter { it.friend && it.line.isEmpty() }.forEach { n -> tag("⭐" + n.name, n.x * u, (n.y - 2.6f) * u, u * 0.4f, Color(0xFFFFF9C4), Color.Black, Color.Black) }
                        npcs.forEach { n ->
                            if (n.state == 1 && n.line.isNotEmpty()) tag(n.line, n.x * u, (n.y - 2.7f) * u, u * 0.46f, Color.White, Color.Black, Color.Black)
                        }
                        cv.restore()
                    }
                }
                val hint = when (room) {
                    Room.SHOP -> if (cars.isEmpty()) "Nenhum carro à venda. Ponha carros da garagem à venda no escritório." else if (!shopOpen) "Fechado (8h às 20h). Os clientes voltam no horário." else "Toque no chão pra andar • toque num carro pra mudar o preço • saia pelo tapete da porta."
                    Room.WORKSHOP -> "Toque num carro seu pra ir até ele e consertar (de graça) • saia pelo tapete da porta."
                    Room.STORE -> "Toque no balcão pra falar com o vendedor • toque nas pessoas pra conversar • saia pelo tapete."
                    Room.PARK -> "Toque na fonte ou no carrinho de pipoca • toque nas pessoas pra conversar e fazer amigos."
                    Room.BEACH -> "Toque no quiosque • toque nas pessoas pra conversar • ou use os botões abaixo."
                    else -> "Toque na cama pra dormir • toque numa pessoa pra conversar • saia pelo tapete da porta."
                }
                Text(hint, fontSize = 12.sp)
                if (room == Room.BEACH) {
                    fun go(a: Int, x: Float, y: Float) { if (act == 0) { act = a; actPhase = 1; target = Offset(x, y); pending = -1; pendNpc = null } }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { go(1, 8f, 5.1f) }, Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) { Text("🌊 Mergulho", fontSize = 11.sp) }
                        OutlinedButton(onClick = { go(2, 4.5f, 9f) }, Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) { Text("☀️ Sol", fontSize = 11.sp) }
                        OutlinedButton(onClick = { go(3, 8f, 8f) }, Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) { Text("💨 Vento", fontSize = 11.sp) }
                        OutlinedButton(onClick = { go(4, 10.4f, 7.6f) }, Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) { Text("🚿 Banho", fontSize = 11.sp) }
                    }
                }
                if (room == Room.HOME && friends.any { it.value >= FRIEND_LVL }) OutlinedButton(onClick = { callDlg = true }, Modifier.fillMaxWidth()) { Text("📞 Chamar um amigo pra vir aqui") }
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
    chatNpc?.let { n ->
        FriendChat(g, n.name, friends[n.name] ?: 0, myName, onLevel = { lv -> friends = friends + (n.name to lv); saveFriends(prefs, friends); n.friend = lv >= FRIEND_LVL }, onClose = { chatNpc = null })
    }
    if (zonePanel) ZoneDialog(g, if (room == Room.PARK) 0 else 1) { zonePanel = false }
    if (storePanel && store != null) StoreDialog(g, store) { storePanel = false }
    if (callDlg) AlertDialog(
        onDismissRequest = { callDlg = false }, confirmButton = { TextButton(onClick = { callDlg = false }) { Text("Fechar") } },
        title = { Text("📞 Chamar amigo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                friends.filter { it.value >= FRIEND_LVL }.keys.forEach { nm ->
                    OutlinedButton(onClick = {
                        if (npcs.none { it.name == nm }) {
                            val n = Npc(randomLook(Random(nm.hashCode())), DOOR_X, 11.8f)
                            n.name = nm; n.wander = true; n.friend = true; n.tx = 6f; n.ty = 8.5f; n.state = 0; npcs.add(n)
                        }
                        g.message = "$nm tá chegando! 🏠"; callDlg = false
                    }, Modifier.fillMaxWidth()) { Text("⭐ $nm") }
                }
            }
        },
    )
    if (homeSub) HomeDialog(g, onSleep = { homeSub = false; onSleep() }, onClose = { homeSub = false })
}

// ------------------------------------------------------------------ cenário

private fun DrawScope.drawRoom(g: GameState, room: Room, u: Float, t: Float, nCars: Int, sign: String) {
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
        Room.STORE -> {
            r(0xFFF0E6C8, 0f, 0f, GW, 4.6f); r(0xFFBFA77A, 0f, 4.1f, GW, 0.5f)
            for (i in 0 until 3) { r(0xFF6D4C41, 0.6f + i * 0.0f, 0.9f, 3.2f, 2.8f).also { }; }
            listOf(0xFFE53935, 0xFFFDD835, 0xFF43A047, 0xFF1E88E5).forEachIndexed { i, c -> r(c, 0.9f + i * 0.7f, 1.2f, 0.5f, 0.5f); r(c, 0.9f + i * 0.7f, 2.3f, 0.5f, 0.5f) }
            r(0xFF6D4C41, 12.6f, 0.9f, 3f, 2.8f); listOf(0xFFFB8C00, 0xFF8E24AA, 0xFF00ACC1).forEachIndexed { i, c -> r(c, 12.9f + i * 0.8f, 1.2f, 0.6f, 0.6f); r(c, 12.9f + i * 0.8f, 2.3f, 0.6f, 0.6f) }
            for (iy in 5 until 12) for (ix in 0 until 16) r(if ((ix + iy) % 2 == 0) 0xFFE2D3B0 else 0xFFD2BF96, ix.toFloat(), iy.toFloat(), 1f, 1f)
            r(0xFF8D6E63, 4.8f, 4.9f, 6.4f, 1.3f); r(0xFFA1887F, 4.8f, 4.9f, 6.4f, 0.3f)
            r(0xFFC62828, 6.8f, 11.4f, 2.4f, 0.6f)
            tag(sign, GW / 2f * u, 0.8f * u, u * 0.5f, Color(0xFFD32F2F), Color.White, Color(0xFF3E2723))
        }
        Room.PARK -> {
            val night = g.minutes < 6 * 60 || g.minutes >= 19 * 60
            r(if (night) 0xFF14204A else 0xFF9AD7FF, 0f, 0f, GW, 3.2f)
            r(0xFF4CAF50, 0f, 3.2f, GW, 8.8f)
            for (k in 0 until 40) r(0xFF43A047, ((k * 37) % 160) / 10f, 3.4f + ((k * 53) % 82) / 10f, 0.15f, 0.3f)
            r(0xFFD7C49E, 0f, 8.4f, GW, 1.4f)                         // caminho
            listOf(1.5f, 4.2f, 13.8f).forEach { x -> r(0xFF6D4C41, x - 0.2f, 2.2f, 0.4f, 1.5f); drawCircle(Color(0xFF2E7D32), 1.1f * u, Offset(x * u, 1.8f * u)); drawCircle(Color(0xFF388E3C), 0.8f * u, Offset((x - 0.3f) * u, 1.5f * u)) }
            drawCircle(Color(0xFF90A4AE), 2.2f * u, Offset(8f * u, 5.2f * u)); drawCircle(Color(0xFF4FC3F7), 1.8f * u, Offset(8f * u, 5.2f * u))
            r(0xFFB0BEC5, 7.8f, 3.6f, 0.4f, 1.6f); val w = ((t * 3f) % 1f) * 0.5f; r(0xFFE1F5FE, 7.5f, 3.6f + w, 1f, 0.2f)
            r(0xFF8D6E63, 1.5f, 6.2f, 3f, 0.5f); r(0xFF5D4037, 1.7f, 6.7f, 0.2f, 0.6f); r(0xFF5D4037, 4.1f, 6.7f, 0.2f, 0.6f) // banco
            r(0xFFEF5350, 12.2f, 4.4f, 3f, 1.8f); r(0xFFFFFFFF, 12.2f, 4.4f, 3f, 0.4f); r(0xFF455A64, 12.5f, 6.2f, 0.3f, 0.5f); r(0xFF455A64, 14.6f, 6.2f, 0.3f, 0.5f)
            tag("🍿 PIPOCA", 13.7f * u, 4.2f * u, u * 0.4f, Color.White, Color.Black, Color.Black)
        }
        Room.BEACH -> {
            r(0xFF9AD7FF, 0f, 0f, GW, 1.2f)
            r(0xFF1E88E5, 0f, 1.2f, GW, 3f)
            for (k in 0 until 7) r(0xFFBBDEFB, ((k * 2.6f + t * 1.4f) % 17f) - 1f, 1.7f + (k % 3) * 0.9f, 1.4f, 0.12f)
            r(0xFFFFFFFF, 0f, 4.1f + sin(t * 1.5f) * 0.1f, GW, 0.25f)
            r(0xFFF2D49B, 0f, 4.4f, GW, 7.6f)
            for (k in 0 until 30) r(0xFFE0BE7C, ((k * 41) % 155) / 10f, 4.8f + ((k * 29) % 70) / 10f, 0.15f, 0.1f)
            r(0xFFFFFFFF, 3f, 8.4f, 3f, 1.3f); r(0xFFE53935, 3.5f, 8.4f, 0.6f, 1.3f); r(0xFFE53935, 4.8f, 8.4f, 0.6f, 1.3f)   // toalha
            r(0xFF5D4037, 4.4f, 5.6f, 0.2f, 2.8f); drawCircle(Color(0xFFFDD835), 1.6f * u, Offset(4.5f * u, 5.6f * u)); r(0xFFF2D49B, 2.8f, 5.6f, 3.5f, 0.1f)
            r(0xFF90A4AE, 10.3f, 4.6f, 0.2f, 2.2f); r(0xFF90A4AE, 10f, 4.6f, 0.9f, 0.15f)   // chuveiro
            r(0xFF8D6E63, 12f, 4.6f, 3.6f, 2.4f); r(0xFF00ACC1, 11.8f, 4.3f, 4f, 0.5f)       // quiosque
            tag("🍹 QUIOSQUE", 13.8f * u, 4.2f * u, u * 0.4f, Color.White, Color.Black, Color.Black)
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
