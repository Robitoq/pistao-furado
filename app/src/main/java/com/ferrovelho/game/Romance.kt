package com.ferrovelho.game

import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

// =====================================================================================
//  RELACIONAMENTO: ficar, namorar, noivar, casar e ter filhos (tudo leva dias do jogo)
//  + passeios com a pessoa amada ou com amigos (praia, parque, sorveteria, café, baile)
// =====================================================================================

val NPC_FEMALE = setOf("Camila", "Dani", "Bia", "Nanda", "Vera", "Gabi", "Lu", "Sônia")
val BOY_NAMES = listOf("Davi", "Miguel", "Arthur", "Heitor", "Théo", "Gael")
val GIRL_NAMES = listOf("Helena", "Alice", "Laura", "Valentina", "Sofia", "Manuela")

fun npcFemale(name: String): Boolean = name in NPC_FEMALE || name in GIRL_NAMES

/** Visual do NPC coerente com o nome (antes a "Camila" podia aparecer como homem). */
fun npcLookFor(name: String): Look {
    val b = randomLook(Random(name.hashCode()))
    val f = npcFemale(name)
    val h = name.hashCode() and 0x7fffffff
    val hs = if (f) {
        if (b.female && (b.hairStyle == 1 || b.hairStyle == 4)) b.hairStyle else if (h % 2 == 0) 1 else 4
    } else {
        if (!b.female && b.hairStyle != 1 && b.hairStyle != 4) b.hairStyle else intArrayOf(0, 2, 5)[h % 3]
    }
    val bt = if (f) (if (b.botType == 0) 0 else 2) else (if (b.botType == 2) 0 else b.botType)
    val tt = if (f && b.topType == 2) 0 else b.topType
    return Look(f, b.skin, hs, b.hair, tt, b.top, bt, b.bottom, b.shoes, b.glasses)
}

data class Romance(
    val partner: String = "",
    val stage: Int = 0,          // 0 solteiro, 1 ficando, 2 namorando, 3 noivos, 4 casados
    val love: Int = 0,           // 0..100
    val since: Int = 0,          // dia em que começou a fase atual
    val dates: Int = 0,          // passeios juntos
    val last: Int = 0,           // último dia em que o amor foi atualizado
    val preg: Int = 0,           // dia em que a gravidez começou (0 = nenhuma)
    val lastKid: Int = 0,        // dia do último filho (ou do casamento)
    val msgDay: Int = -1,
    val giftDay: Int = -1,
    val kids: List<Pair<String, Int>> = emptyList(),   // nome -> dia do nascimento
)

fun loadRomance(p: SharedPreferences): Romance = Romance(
    partner = p.getString("rm_partner", "") ?: "",
    stage = p.getInt("rm_stage", 0),
    love = p.getInt("rm_love", 0),
    since = p.getInt("rm_since", 0),
    dates = p.getInt("rm_dates", 0),
    last = p.getInt("rm_last", 0),
    preg = p.getInt("rm_preg", 0),
    lastKid = p.getInt("rm_lastkid", 0),
    msgDay = p.getInt("rm_msg", -1),
    giftDay = p.getInt("rm_gift", -1),
    kids = (p.getString("rm_kids", "") ?: "").split(";").mapNotNull {
        val a = it.split(":")
        if (a.size == 2) a[0] to (a[1].toIntOrNull() ?: 0) else null
    },
)

fun saveRomance(p: SharedPreferences, r: Romance) {
    p.edit()
        .putString("rm_partner", r.partner).putInt("rm_stage", r.stage).putInt("rm_love", r.love)
        .putInt("rm_since", r.since).putInt("rm_dates", r.dates).putInt("rm_last", r.last)
        .putInt("rm_preg", r.preg).putInt("rm_lastkid", r.lastKid).putInt("rm_msg", r.msgDay).putInt("rm_gift", r.giftDay)
        .putString("rm_kids", r.kids.joinToString(";") { "${it.first}:${it.second}" })
        .apply()
}

/** Atualiza o que depende do tempo: o amor esfria se você some, o bebê nasce. Devolve o estado e um aviso (se houver). */
fun refreshRomance(p: SharedPreferences, today: Int): Pair<Romance, String?> {
    var r = loadRomance(p)
    if (r.stage == 0 || r.partner.isEmpty()) return r to null
    if (r.last > today || r.since > today) {      // jogo reiniciado
        r = Romance()
        saveRomance(p, r)
        return r to null
    }
    var msg: String? = null
    val d = today - r.last
    if (r.stage in 1..3 && d > 6) r = r.copy(love = r.love - (d - 6) * 2)
    if (r.stage == 4 && d > 6) r = r.copy(love = max(40, r.love - (d - 6)))
    if (r.stage in 1..2 && r.love <= 0) {
        msg = "${r.partner} terminou com você: faltou atenção."
        r = Romance()
        saveRomance(p, r)
        return r to msg
    }
    if (r.stage == 3 && r.love < 20) r = r.copy(love = 20)
    if (r.preg > 0 && today - r.preg >= 7 && r.kids.size < 3) {
        val girl = Random.nextBoolean()
        val pool = (if (girl) GIRL_NAMES else BOY_NAMES).filter { n -> r.kids.none { it.first == n } }
        val name = pool.random()
        r = r.copy(kids = r.kids + (name to today), preg = 0, lastKid = today)
        msg = "Nasceu ${if (girl) "a" else "o"} $name! 👶 Parabéns, papai e mamãe!"
    }
    r = r.copy(last = today)
    saveRomance(p, r)
    return r to msg
}

/** Chamado quando um passeio acaba: amizade sobe e, se a pessoa amada foi junto, o amor também. */
fun outingDone(g: GameState, prefs: SharedPreferences, companions: List<String>, elapsed: Int, shared: Int) {
    if (companions.isEmpty() || (elapsed < 20 && shared == 0)) return
    var friends = loadFriends(prefs)
    companions.forEach { nm -> friends = friends + (nm to min(5, (friends[nm] ?: 0) + 1)) }
    saveFriends(prefs, friends)
    val r0 = refreshRomance(prefs, g.day).first
    var msg = "Foi um passeio e tanto! A amizade ficou mais forte."
    if (r0.partner.isNotEmpty() && r0.partner in companions && r0.stage in 1..4) {
        val gain = 5 + 3 * min(shared, 3)
        val r = r0.copy(love = min(100, r0.love + gain), dates = r0.dates + 1, last = g.day)
        saveRomance(prefs, r)
        msg = "❤ ${r.partner} adorou o passeio! (+$gain de amor)"
    }
    g.message = msg
    g.save()
}

fun companionLine(partner: Boolean, minutes: Int, room: Room, r: Random): String {
    val pool = mutableListOf<String>()
    if (partner) {
        pool += listOf("Adoro estar com você.", "Que passeio gostoso, amor.", "Fica pertinho de mim.", "Você me faz rir.")
        if (room == Room.BEACH) pool += listOf("Que mar lindo, hein?", "Vem nadar comigo!")
        if (nightF(minutes) > 0.6f) pool += "Olha quantas estrelas..."
    } else {
        pool += listOf("Bom demais sair com você!", "Faz tempo que a gente não saía assim.", "Tô me divertindo!")
        if (room == Room.BEACH) pool += listOf("Bora dar um mergulho?", "Esse sol tá demais!")
        if (room == Room.STORE) pool += "Me paga um sorvete? Brinks."
    }
    return pool[r.nextInt(pool.size)]
}

/** Pessoa sentada com as pernas esticadas (areia, banco). x/y em unidades da cena, u = pixels por unidade. */
fun DrawScope.drawSitter(look: Look, x: Float, y: Float, u: Float, faceLeft: Boolean, towel: Boolean) {
    val d = if (faceLeft) -1f else 1f
    fun box(c: Color, bx: Float, by: Float, bw: Float, bh: Float) {
        val l = if (bw >= 0f) bx else bx + bw
        drawRect(c, Offset(l * u, by * u), Size(abs(bw) * u, bh * u))
    }
    if (towel) {
        box(Color(0xFFFFFFFF), x - 1.3f, y - 0.25f, 2.6f, 0.7f)
        box(Color(0xFFE53935), x - 1.3f, y - 0.25f, 2.6f, 0.15f)
    }
    box(CBOT[look.bottom], x, y - 0.55f, d * 1.9f, 0.5f)
    box(CSHOE[look.shoes], x + d * 1.7f, y - 0.6f, d * 0.45f, 0.6f)
    box(CTOP[look.top], x - 0.45f, y - 1.75f, 0.9f, 1.25f)
    box(CSKIN[look.skin], x - 0.38f, y - 2.35f, 0.76f, 0.7f)
    box(CHAIR[look.hair], x - 0.42f, y - 2.5f, 0.84f, 0.35f)
    if (look.female && (look.hairStyle == 1 || look.hairStyle == 4)) box(CHAIR[look.hair], x - d * 0.45f - 0.2f, y - 2.4f, 0.4f, 1.2f)
}

// -------------------------------------------------------------------------------------
//  TELA
// -------------------------------------------------------------------------------------

class Trip(val room: Room, val storeIdx: Int, val companions: List<String>)

private val STAGE_NAMES = listOf("Solteiro(a)", "Ficando", "Namorando", "Noivos", "Casados")

private fun hearts(love: Int): String {
    val f = (love / 20).coerceIn(0, 5)
    return "❤".repeat(f) + "🤍".repeat(5 - f)
}

@Composable
fun RelationshipDialog(g: GameState, prefs: SharedPreferences, me: PlayerProfile, onGo: (Trip) -> Unit, onClose: () -> Unit) {
    val first = remember { refreshRomance(prefs, g.day) }
    var rom by remember { mutableStateOf(first.first) }
    var info by remember { mutableStateOf(first.second ?: "") }
    var friends by remember { mutableStateOf(loadFriends(prefs)) }
    var sel by remember { mutableStateOf(setOf<String>()) }
    var planning by remember { mutableStateOf(false) }
    val today = g.day
    val myFemale = me.look.female

    fun commit(r: Romance) { rom = r; saveRomance(prefs, r); g.save() }
    fun pron(): String = if (npcFemale(rom.partner)) "ela" else "ele"

    fun start(nm: String) {
        friends = friends + (nm to max(4, friends[nm] ?: 0))
        saveFriends(prefs, friends)
        commit(Romance(partner = nm, stage = 1, love = 20, since = today, last = today, lastKid = today))
        info = "Você chamou $nm pra sair. Agora vocês estão se conhecendo."
    }
    fun carinho(kind: Int) {
        if (kind == 0) {
            if (rom.msgDay == today) return
            g.spendTime(5)
            commit(rom.copy(love = min(100, rom.love + 2), msgDay = today, last = today))
            info = "Você mandou uma mensagem carinhosa. (+2)"
        } else {
            if (rom.giftDay == today) return
            val cost = if (kind == 1) 60 else 400
            val gain = if (kind == 1) 8 else 22
            if (g.money < cost) { info = "Faltam ${brl(cost - g.money)}."; return }
            g.money -= cost
            commit(rom.copy(love = min(100, rom.love + gain), giftDay = today, last = today))
            info = (if (kind == 1) "Você deu flores 💐" else "Você deu um presente caro 🎁") + " (+$gain)"
        }
    }

    val daysIn = today - rom.since
    val need: List<String> = when (rom.stage) {
        1 -> buildList {
            if (rom.dates < 3) add("mais ${3 - rom.dates} passeio(s)")
            if (rom.love < 55) add("amor ${rom.love}/55")
            if (daysIn < 4) add("${4 - daysIn} dia(s) de convivência")
        }
        2 -> buildList {
            if (daysIn < 10) add("${10 - daysIn} dia(s) de namoro")
            if (rom.love < 75) add("amor ${rom.love}/75")
            if (g.money < 6000) add("${brl(6000 - g.money)} pro anel")
        }
        3 -> buildList {
            if (daysIn < 8) add("${8 - daysIn} dia(s) de noivado")
            if (rom.love < 80) add("amor ${rom.love}/80")
            if (g.money < 12000) add("${brl(12000 - g.money)} pra festa")
        }
        4 -> buildList {
            if (rom.kids.size >= 3) add("família completa")
            if (rom.preg > 0) add("bebê a caminho")
            if (daysIn < 10) add("${10 - daysIn} dia(s) de casados")
            if (today - rom.lastKid < 12) add("${12 - (today - rom.lastKid)} dia(s) desde o último bebê")
            if (rom.love < 70) add("amor ${rom.love}/70")
            if (g.money < 3000) add("${brl(3000 - g.money)} pro enxoval")
        }
        else -> emptyList()
    }
    val advLabel = when (rom.stage) {
        1 -> "💘 Pedir em namoro"
        2 -> "💍 Pedir em casamento · R$ 6.000"
        3 -> "⛪ Casar · R$ 12.000"
        else -> "👶 Ter um filho · R$ 3.000"
    }
    fun advance() {
        when (rom.stage) {
            1 -> {
                commit(rom.copy(stage = 2, since = today, love = max(rom.love, 60), last = today))
                info = "${rom.partner} disse SIM! Agora vocês namoram ❤"
            }
            2 -> {
                g.money -= 6000
                commit(rom.copy(stage = 3, since = today, love = min(100, rom.love + 10), last = today))
                info = "Você pediu em casamento e ${pron()} aceitou! 💍"
            }
            3 -> {
                g.money -= 12000
                g.spendTime(180)
                friends = friends + (rom.partner to 5)
                saveFriends(prefs, friends)
                commit(rom.copy(stage = 4, since = today, lastKid = today, love = 100, last = today))
                info = "Vocês se casaram! A festa foi linda 🎉 Agora ${pron()} mora com você."
            }
            4 -> {
                g.money -= 3000
                commit(rom.copy(preg = today, last = today))
                info = "Vem bebê aí! 🤰 Nasce em 7 dias."
            }
        }
    }

    val cands = friends.filter { it.value >= FRIEND_LVL && it.key != "Seu Jorge" && npcFemale(it.key) != myFemale }.keys.toList()
    val buddies = friends.filter { it.value >= FRIEND_LVL }.keys.toList().sortedBy { if (it == rom.partner) 0 else 1 }

    fun go(room: Room, si: Int) {
        if (sel.isEmpty()) { info = "Escolha quem vai com você."; return }
        if (room == Room.STORE) {
            val st = STORES.getOrNull(si)
            if (st == null || g.minutes !in st.open until st.close) { info = "Tá fechado agora."; return }
        }
        if (room == Room.CLUB) {
            val fee = CLUB_FEE * (1 + sel.size)
            if (!clubOpen(g.minutes)) { info = "O baile só abre às 20h."; return }
            if (g.money < fee) { info = "Faltam ${brl(fee - g.money)} pros ingressos."; return }
            g.money -= fee
        }
        onGo(Trip(room, si, sel.toList()))
    }

    FullDialog("❤ Relacionamento", g, onClose) {
        if (info.isNotEmpty()) Text(info, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
        if (rom.partner.isEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Você está solteiro(a).", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Faça amizade com alguém (nível 3 ou mais) no parque, na praia ou no baile: converse e pague um refri. Depois é só chamar pra sair!", fontSize = 12.sp)
                    if (cands.isEmpty()) Text("Ainda não tem ninguém por perto. Vá conversar com as pessoas!", fontSize = 12.sp)
                    cands.forEach { nm ->
                        OutlinedButton({ start(nm) }, Modifier.fillMaxWidth()) { Text("💘 Chamar $nm pra sair") }
                    }
                }
            }
        } else {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${rom.partner} • ${STAGE_NAMES[rom.stage.coerceIn(0, 4)]}", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("${hearts(rom.love)}  ${rom.love}% de amor", fontSize = 14.sp)
                    Text("Nessa fase há $daysIn dia(s) • ${rom.dates} passeio(s) juntos", fontSize = 12.sp)
                    if (rom.preg > 0) Text("🤰 Bebê a caminho! Nasce em ${(7 - (today - rom.preg)).coerceAtLeast(1)} dia(s).", fontSize = 13.sp)
                    Text("O amor esfria se você ficar muitos dias sem ver ${pron()}.", fontSize = 11.sp)
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Carinho (um por dia)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp)) {
                        OutlinedButton({ carinho(0) }, Modifier.weight(1f), rom.msgDay != today, contentPadding = PaddingValues(4.dp)) { Text("💬 Mensagem", fontSize = 11.sp) }
                        OutlinedButton({ carinho(1) }, Modifier.weight(1f), rom.giftDay != today, contentPadding = PaddingValues(4.dp)) { Text("💐 Flores · R$ 60", fontSize = 11.sp) }
                        OutlinedButton({ carinho(2) }, Modifier.weight(1f), rom.giftDay != today, contentPadding = PaddingValues(4.dp)) { Text("🎁 Joia · R$ 400", fontSize = 11.sp) }
                    }
                }
            }
            if (!(rom.stage == 4 && rom.kids.size >= 3 && rom.preg == 0)) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Próximo passo", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Button({ advance() }, Modifier.fillMaxWidth(), need.isEmpty()) { Text(advLabel) }
                        if (need.isNotEmpty()) Text("Falta: " + need.joinToString(", "), fontSize = 12.sp)
                    }
                }
            }
        }

        Button({ planning = !planning }, Modifier.fillMaxWidth()) { Text(if (planning) "Fechar passeio" else "🚶 Sair pra passear (com amor ou amigos)") }
        if (planning) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Quem vai com você? (até 3)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    if (buddies.isEmpty()) Text("Você ainda não tem amigos. Converse com as pessoas no parque ou na praia.", fontSize = 12.sp)
                    buddies.forEach { nm ->
                        val on = nm in sel
                        val label = (if (nm == rom.partner) "❤ " else "") + nm + (if (on) "  ✓" else "")
                        val click = { sel = if (on) sel - nm else if (sel.size < 3) sel + nm else sel }
                        if (on) Button(click, Modifier.fillMaxWidth()) { Text(label) }
                        else OutlinedButton(click, Modifier.fillMaxWidth()) { Text(label) }
                    }
                    Text("Pra onde?", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp)) {
                        OutlinedButton({ go(Room.BEACH, 0) }, Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) { Text("🏖 Praia", fontSize = 12.sp) }
                        OutlinedButton({ go(Room.PARK, 0) }, Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) { Text("🌳 Parque", fontSize = 12.sp) }
                    }
                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp)) {
                        OutlinedButton({ go(Room.STORE, 4) }, Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) { Text("🍦 Sorveteria", fontSize = 12.sp) }
                        OutlinedButton({ go(Room.STORE, 3) }, Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) { Text("☕ Café", fontSize = 12.sp) }
                    }
                    OutlinedButton({ go(Room.CLUB, 0) }, Modifier.fillMaxWidth(), contentPadding = PaddingValues(4.dp)) { Text("🎶 Baile (abre às 20h) · ${brl(CLUB_FEE)} por pessoa", fontSize = 12.sp) }
                    Text("Passear junto aumenta a amizade, e o amor se for a pessoa amada. Na praia dá pra nadar, tomar banho, sentar na areia e ver o tempo passar.", fontSize = 11.sp)
                }
            }
        }
        if (rom.kids.isNotEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Família 👨‍👩‍👧", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    rom.kids.forEach { k -> Text("👶 ${k.first} • ${today - k.second} dia(s) de vida", fontSize = 13.sp) }
                    Text("Eles ficam em casa. Passe lá pra ver a família.", fontSize = 11.sp)
                }
            }
        }
        Text("O tempo aqui conta em dias do jogo. Casamento e filhos levam um bom tempo!", fontSize = 11.sp)
    }
}
