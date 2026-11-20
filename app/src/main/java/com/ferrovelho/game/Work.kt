package com.ferrovelho.game

import android.graphics.Paint
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// =====================================================================================
//  EMPREGOS REAIS
//  Nada de minigame de clicar: o personagem vai até o local de trabalho, bate o ponto e executa o serviço
//  NO MAPA (a pé ou dirigindo o veículo da empresa). Cada serviço é uma sequência de "passos" com um
//  ponto marcado no mundo: chegue lá (parado, se for de carro), fique um tempo trabalhando e o passo
//  conclui. Concluir passos paga, dá experiência (XP) e, com XP, dá pra pedir promoção.
//  O relógio do jogo NUNCA para: a jornada de cada emprego respeita o horário (inclusive de madrugada).
//
//  Pra mexer: pagamentos em planOrder(), horários/cargos em WORK_JOBS, locais em WORK_DEST.
// =====================================================================================

/** Cargo: xp = experiência total necessária pra chegar nele; mult = multiplicador do pagamento. */
class Rank(val name: String, val xp: Int, val mult: Double)

private val RANK_XP = intArrayOf(0, 150, 500, 1200, 2500)
private val RANK_MULT = doubleArrayOf(1.0, 1.15, 1.35, 1.6, 1.9)
private fun ranks(vararg n: String): List<Rank> = n.mapIndexed { i, s -> Rank(s, RANK_XP[i], RANK_MULT[i]) }

/**
 * Profissão. x = local de trabalho no mapa. from/to = jornada em minutos (to < from vira a noite).
 * vehicle = sprite do veículo da empresa (-1 = trabalha a pé).
 */
class WorkJob(
    val id: String, val emoji: String, val title: String, val place: String, val x: Float,
    val from: Int, val to: Int, val vehicle: Int, val ranks: List<Rank>, val blurb: String, val payHint: String,
)

val WORK_JOBS: List<WorkJob> = listOf(
    WorkJob(
        "mecanico", "🔧", "Mecânico", "Oficina", 1000f, 7 * 60, 20 * 60, -1,
        ranks("Ajudante", "Mecânico", "Mecânico sênior", "Chefe de oficina", "Gerente"),
        "Cliente chega com o carro quebrado: diagnostique, busque a peça no estoque, instale e cobre. Na sua Ferrovelho Motors o dinheiro todo é da empresa.",
        "R$ 16 a R$ 32 por peça instalada",
    ),
    WorkJob(
        "taxi", "🚕", "Taxista", "Ponto de táxi", 3720f, 0, 24 * 60, V_TAXI,
        ranks("Taxista novato", "Taxista", "Taxista experiente", "Taxista executivo", "Dono de frota"),
        "Pegue o táxi, busque o passageiro onde ele chamou e leve até o destino. Paga pela distância, com adicional noturno. Trabalha 24h.",
        "R$ 12 + R$ 2 a cada 100 m",
    ),
    WorkJob(
        "motorista", "🚚", "Motorista de frete", "Transportadora Estrada Livre", 5200f, 6 * 60, 22 * 60, V_TRUCK_BAU,
        ranks("Motorista júnior", "Motorista", "Motorista sênior", "Motorista líder", "Gerente de logística"),
        "Pegue o caminhão baú, carregue no galpão, dirija até o endereço da entrega e descarregue.",
        "R$ 25 + R$ 1,8 a cada 100 m",
    ),
    WorkJob(
        "entregador", "🛵", "Entregador", "Trailer do Bigode", 3000f, 10 * 60, 24 * 60, V_MOTO_ENTREGA,
        ranks("Entregador iniciante", "Entregador", "Entregador veterano", "Entregador líder", "Gerente de entregas"),
        "Pegue a moto, retire o pedido no Trailer e entregue no endereço antes do prazo. Entrega rápida rende gorjeta.",
        "R$ 14 + R$ 2,4 a cada 100 m + gorjeta",
    ),
    WorkJob(
        "ferrovelho", "♻️", "Desmontador de ferro-velho", "Ferro Velho do Zé", 520f, 8 * 60, 18 * 60, -1,
        ranks("Ajudante de pátio", "Desmontador", "Desmontador sênior", "Encarregado do pátio", "Sócio do ferro-velho"),
        "Chega uma carcaça: desmonte as peças, separe os materiais e venda na balança.",
        "R$ 12 a R$ 34 por material vendido",
    ),
    WorkJob(
        "loja", "🛒", "Atendente de loja", "Mercadinho 2 Irmãos", 1700f, 6 * 60, 23 * 60 + 30, -1,
        ranks("Repositor", "Atendente", "Caixa", "Encarregado de loja", "Gerente de loja"),
        "Atenda os clientes no caixa e reponha as prateleiras com as caixas do estoque. Respeita o horário do mercadinho.",
        "R$ 13 a R$ 14 por serviço",
    ),
    WorkJob(
        "vigia", "🔦", "Vigia noturno", "Guarita do Parque", 12100f, 22 * 60, 6 * 60, -1,
        ranks("Vigia", "Vigia sênior", "Supervisor de ronda", "Chefe de segurança", "Gerente de segurança"),
        "Das 22h às 6h: faça a ronda a pé pelos pontos de checagem e registre na guarita.",
        "R$ 12 a R$ 16 por ponto",
    ),
)

/** Lugares da cidade usados como destino de corridas, fretes e entregas (nome, posição no mapa). */
val WORK_DEST: List<Pair<String, Float>> = listOf(
    "Minha casa" to 260f,
    "Ferro Velho do Zé" to 520f,
    "Padaria Pão Quente" to 760f,
    "Oficina" to 1000f,
    "Mercadinho 2 Irmãos" to 1700f,
    "Encontro de Carros" to 2000f,
    "Desmanche Central" to 2450f,
    "Trailer do Bigode" to 3000f,
    "Pátio Premium" to 3500f,
    "Pista de Arrancada" to 4200f,
    "Sucata do Beto" to 4450f,
    "Café da Esquina" to 5800f,
    "Casinha Azul" to 6400f,
    "Baile" to 7800f,
    "Sorveteria Gelada" to 8300f,
    "Prédio Central" to 9200f,
    "Cobertura Skyline" to 10200f,
    "Mansão da Colina" to 11300f,
    "Parque Central" to 12800f,
    "Quiosque da Praia" to 14500f,
)

/** Veículo da empresa que o personagem dirige durante o turno. */
class WorkVeh(val spr: Int, val maxV: Float, val color: Int, val name: String)

/**
 * Um passo do serviço: ir até x e ficar secs segundos trabalhando.
 *  inVeh = precisa estar dirigindo o veículo da empresa (parado no ponto)   carry = item carregado depois do passo
 *  deadline = minuto absoluto do prazo (gorjeta se cumprir, desconto se atrasar)
 */
class Step(
    val x: Float, val icon: String, val label: String, val secs: Float,
    val pay: Int = 0, val xp: Int = 0, val carry: String? = null,
    val deadline: Int = -1, val tag: String = "", val inVeh: Boolean = false,
)

/** Turno em andamento (não é salvo: se fechar o app no meio, o turno acaba e o que já foi pago fica). */
class Shift(val job: WorkJob, val startAbs: Int) {
    var earned by mutableStateOf(0)
    var tasks by mutableStateOf(0)
    var xpGain by mutableStateOf(0)
    var step by mutableStateOf<Step?>(null)
    var progress by mutableFloatStateOf(0f)
    var carrying by mutableStateOf("")
    var veh by mutableStateOf<WorkVeh?>(null)
    var order by mutableStateOf("")
    val queue = ArrayDeque<Step>()
    // cenário do pedido atual
    var cust by mutableStateOf<Look?>(null)      // cliente / passageiro visível no mapa
    var custX by mutableFloatStateOf(0f)
    var carModel by mutableStateOf(-1)           // carro do cliente (oficina) ou carcaça (ferro-velho); -1 = nenhum
    var carYear by mutableStateOf(2000)
    var carColor by mutableStateOf(0)
    var carCond by mutableStateOf(List(5) { 100 })
    var carX by mutableFloatStateOf(0f)
    var parkX by mutableFloatStateOf(0f)         // onde o veículo da empresa está estacionado
}

val WORK_FULL = List(5) { 100 }

// ------------------------------------------------------------ cargo / experiência

fun workRankIdx(g: GameState, j: WorkJob): Int = (g.careerRank[j.id] ?: 0).coerceIn(0, j.ranks.lastIndex)
fun workRank(g: GameState, j: WorkJob): Rank = j.ranks[workRankIdx(g, j)]
fun workXp(g: GameState, j: WorkJob): Int = g.careerXp[j.id] ?: 0
fun canPromote(g: GameState, j: WorkJob): Boolean {
    val i = workRankIdx(g, j)
    return i < j.ranks.lastIndex && workXp(g, j) >= j.ranks[i + 1].xp
}

/** Pede a promoção: sobe de cargo (paga mais em todos os serviços) e ganha um bônus. */
fun promote(g: GameState, j: WorkJob) {
    if (!canPromote(g, j)) return
    val ni = workRankIdx(g, j) + 1
    g.careerRank = g.careerRank + (j.id to ni)
    val bonus = 150 * ni
    g.money += bonus
    g.careerEarned += bonus
    g.message = "🎉 Promoção! ${j.emoji} Agora você é ${j.ranks[ni].name} (paga ${((j.ranks[ni].mult - 1.0) * 100).roundToInt()}% a mais). Bônus de ${brl(bonus)}."
    g.save()
}

// ------------------------------------------------------------ horário

/** A jornada está aberta agora? (usa o relógio contínuo: funciona de madrugada). */
fun workOpen(j: WorkJob, g: GameState): Boolean {
    if (j.id == "mecanico") return garageOpen(g)
    val m = g.minutes
    return if (j.from < j.to) m in j.from until j.to else (m >= j.from || m < j.to)
}

private fun hhmm(m: Int): String = String.format("%02d:%02d", (m / 60) % 24, m % 60)

fun workHours(j: WorkJob, g: GameState): String {
    if (j.id == "mecanico" && g.ownShop) return "24h (empresa própria)"
    if (j.from == 0 && j.to == DAY_MIN) return "24h"
    return "${hhmm(j.from)} às ${hhmm(j.to)}"
}

/** Quantos minutos faltam pro fim da jornada (0 se fechada). */
fun workMinutesLeft(j: WorkJob, g: GameState): Int {
    if (!workOpen(j, g)) return 0
    if (j.id == "mecanico" && g.ownShop) return DAY_MIN
    val m = g.minutes
    return if (j.from < j.to) j.to - m else if (m >= j.from) DAY_MIN - m + j.to else j.to - m
}

// ------------------------------------------------------------ início / fim do turno

fun workBlock(g: GameState, j: WorkJob): String? = when {
    g.shift != null -> "Você já está em turno."
    !workOpen(j, g) -> "Fechado agora. Jornada: ${workHours(j, g)}."
    g.hunger < 20.0 || g.thirst < 15.0 -> "Com muita fome ou sede. Coma e beba antes de trabalhar."
    else -> null
}

fun startShift(g: GameState, j: WorkJob) {
    workBlock(g, j)?.let { g.message = it; return }
    val sh = Shift(j, g.absMinutes())
    if (j.vehicle >= 0) {
        sh.parkX = j.x + 70f
        sh.queue.addLast(Step(sh.parkX, "🔑", "Pegar as chaves do veículo", 1.4f, tag = "keys"))
    }
    g.shift = sh
    g.heading = null
    g.message = "${j.emoji} Ponto batido! Jornada de ${j.title} até ${hhmm(if (j.to == DAY_MIN) 0 else j.to)}."
}

fun endShift(g: GameState, why: String) {
    val sh = g.shift ?: return
    val j = sh.job
    var bonus = 0
    val hours = (g.absMinutes() - sh.startAbs) / 60
    if (sh.tasks >= 3 && hours >= 3) {
        bonus = (sh.earned * 0.10).roundToInt()
        g.money += bonus
        g.careerEarned += bonus
    }
    g.shift = null
    g.heading = null
    g.message = "${j.emoji} Fim do turno: ${sh.tasks} serviço(s), ${brl(sh.earned + bonus)} ganhos" +
        (if (bonus > 0) " (com bônus de jornada ${brl(bonus)})" else "") + " e +${sh.xpGain} XP. $why"
    g.save()
}

// ------------------------------------------------------------ pedidos (o que fazer em cada profissão)

private fun pickDest(rnd: Random, from: Float, minDist: Float): Pair<String, Float> {
    val far = WORK_DEST.filter { abs(it.second - from) >= minDist }
    return (far.ifEmpty { WORK_DEST }).random(rnd)
}

private val SVC_PRICE = intArrayOf(70, 55, 40, 45, 50)              // quanto o cliente paga por peça (Motor, Lataria, Pneus, Interior, Suspensão)
private val SVC_X = floatArrayOf(28f, 12f, -34f, -4f, -20f)          // onde fica cada serviço em volta do carro
private val DIS_NAMES = listOf("Porta", "Motor", "Rodas", "Banco", "Para-choque", "Bateria")
private val DIS_ICONS = listOf("🚪", "⚙️", "🛞", "💺", "🛡️", "🔋")
private val DIS_PAY = intArrayOf(14, 34, 20, 12, 12, 16)             // valor do material
private val DIS_X = floatArrayOf(12f, 30f, -12f, -28f, -42f, 4f)
private val CARGOS = listOf("caixas de refrigerante", "sacos de cimento", "pallets de tijolo", "fardos de arroz", "peças de reposição", "móveis")

/** Monta o próximo pedido do turno e coloca os passos na fila. */
private fun planOrder(g: GameState, sh: Shift, px: Float) {
    val j = sh.job
    val rnd = Random(System.nanoTime())
    val rm = workRank(g, j).mult
    val rIdx = workRankIdx(g, j)
    sh.cust = null
    when (j.id) {
        "taxi" -> {
            val from = (px + (if (rnd.nextBoolean()) 1 else -1) * rnd.nextInt(350, 1700)).coerceIn(150f, WORLD_END - 150f)
            val dest = pickDest(rnd, from, 800f)
            val dist = abs(dest.second - from)
            val night = g.minutes >= 22 * 60 || g.minutes < 5 * 60
            val fare = ((12 + dist * 0.02) * rm * (if (night) 1.25 else 1.0)).roundToInt()
            sh.cust = randomLook(rnd); sh.custX = from
            sh.order = "Corrida até ${dest.first}" + (if (night) " (adicional noturno)" else "")
            sh.queue.addLast(Step(from, "🧍", "Buscar passageiro", 2.2f, xp = 3, carry = "Passageiro", tag = "pax", inVeh = true))
            sh.queue.addLast(Step(dest.second, "📍", "Deixar em: ${dest.first}", 1.6f, pay = fare, xp = 12, carry = "", tag = "drop", inVeh = true))
        }
        "motorista" -> {
            val dest = pickDest(rnd, j.x, 900f)
            val dist = abs(dest.second - j.x)
            val pay = ((25 + dist * 0.018) * rm).roundToInt()
            val cargo = CARGOS.random(rnd)
            sh.order = "Frete: $cargo para ${dest.first}"
            sh.queue.addLast(Step(j.x - 50f, "📦", "Carregar o baú", 3f, xp = 3, carry = "Carga: $cargo", tag = "load", inVeh = true))
            sh.queue.addLast(Step(dest.second, "🏁", "Descarregar em: ${dest.first}", 2.2f, pay = pay, xp = 14, carry = "", tag = "unload", inVeh = true))
        }
        "entregador" -> {
            val dest = pickDest(rnd, j.x, 600f)
            val dist = abs(dest.second - j.x)
            val pay = ((14 + dist * 0.024) * rm).roundToInt()
            val limit = (20 + dist / 260f * 2f * 1.6f).roundToInt()
            sh.order = "Entrega: ${dest.first}"
            sh.queue.addLast(Step(j.x, "🍔", "Retirar o pedido", 2f, xp = 2, carry = "Pedido quentinho", tag = "pick", inVeh = true))
            sh.queue.addLast(Step(dest.second, "🏠", "Entregar: ${dest.first}", 1.5f, pay = pay, xp = 10, carry = "", deadline = g.absMinutes() + limit + 2, tag = "deliver", inVeh = true))
        }
        "mecanico" -> {
            val pool = CATALOG.indices.filter { CATALOG[it].base <= 120000 }
            val mi = pool.random(rnd)
            val m = CATALOG[mi]
            val share = if (g.ownShop) 1.0 else 0.45
            val n = (2 + (if (rIdx >= 2) 1 else 0) + rnd.nextInt(2)).coerceAtMost(4)
            val parts = (0..4).shuffled(rnd).take(n)
            val cond = MutableList(5) { 100 }
            for (p in parts) cond[p] = rnd.nextInt(10, 36)
            sh.carModel = mi; sh.carYear = rnd.nextInt(m.from, m.to + 1); sh.carColor = rnd.nextInt(PAINTS.size)
            sh.carCond = cond.toList(); sh.carX = j.x - 40f
            sh.cust = randomLook(rnd); sh.custX = sh.carX - 62f
            sh.order = "${m.fullName}: " + parts.joinToString(", ") { PART_NAMES[it] }
            sh.queue.addLast(Step(sh.carX + 4f, "🔍", "Diagnosticar o carro", 2f, xp = 3, tag = "diag"))
            for (p in parts) {
                sh.queue.addLast(Step(sh.carX + 85f, "📦", "Pegar no estoque: ${PART_NAMES[p]}", 1.3f, carry = "Peça: ${PART_NAMES[p]}", tag = "get"))
                sh.queue.addLast(Step(sh.carX + SVC_X[p], PART_ICONS[p], "Instalar: ${PART_NAMES[p]}", 3.5f,
                    pay = (SVC_PRICE[p] * share * rm).roundToInt(), xp = 8, carry = "", tag = "fix:$p"))
            }
            sh.queue.addLast(Step(sh.carX - 85f, "💵", "Cobrar o cliente", 1.4f, pay = (40 * share * rm).roundToInt(), xp = 6, tag = "cash"))
        }
        "ferrovelho" -> {
            val pool = CATALOG.indices.filter { CATALOG[it].base <= 45000 }
            val mi = pool.random(rnd)
            val m = CATALOG[mi]
            sh.carModel = mi; sh.carYear = rnd.nextInt(m.from, m.to + 1); sh.carColor = rnd.nextInt(PAINTS.size)
            sh.carCond = List(5) { rnd.nextInt(4, 22) }; sh.carX = j.x + 80f
            val scale = sh.carX + 80f
            val n = (3 + rnd.nextInt(2)).coerceAtMost(DIS_NAMES.size)
            val parts = DIS_NAMES.indices.shuffled(rnd).take(n)
            sh.order = "Desmontar ${m.fullName}"
            parts.chunked(2).forEach { grp ->
                var total = 0
                for (p in grp) {
                    total += DIS_PAY[p]
                    sh.queue.addLast(Step(sh.carX + DIS_X[p], DIS_ICONS[p], "Desmontar: ${DIS_NAMES[p]}", 3.2f, xp = 2, carry = "Sucata: ${DIS_NAMES[p]}", tag = "dis"))
                }
                sh.queue.addLast(Step(scale, "⚖️", "Vender materiais na balança", 1.4f, pay = (total * rm).roundToInt(), xp = 8, carry = "", tag = "sell"))
            }
            sh.queue.addLast(Step(sh.carX, "🏗️", "Vender o casco", 2.2f, pay = (22 * rm).roundToInt(), xp = 5, tag = "tow"))
        }
        "loja" -> {
            val first = rnd.nextBoolean()
            val cx = j.x + 26f
            fun atender() {
                sh.queue.addLast(Step(cx, "🛒", "Atender o cliente no caixa", 3f, pay = (14 * rm).roundToInt(), xp = 7, tag = "cust"))
            }
            fun repor() {
                sh.queue.addLast(Step(j.x - 55f, "📦", "Pegar caixa no estoque", 1.3f, carry = "Caixa de mercadoria", tag = "box"))
                sh.queue.addLast(Step(j.x + 60f, "🧃", "Abastecer a prateleira", 2.4f, pay = (13 * rm).roundToInt(), xp = 6, carry = "", tag = "shelf"))
            }
            sh.order = "Atendimento e reposição"
            if (first) { atender(); repor() } else { repor(); atender() }
            sh.cust = randomLook(rnd); sh.custX = cx
        }
        else -> { // vigia
            val xs = listOf(-260f, -90f, 110f, 300f)
            sh.order = "Ronda noturna"
            for ((i, dx) in xs.withIndex()) {
                sh.queue.addLast(Step(j.x + dx, "🔦", "Ronda: ponto ${i + 1} de ${xs.size}", 2.5f, pay = (16 * rm).roundToInt(), xp = 8, tag = "patrol"))
            }
            sh.queue.addLast(Step(j.x, "📝", "Registrar a ronda na guarita", 1.6f, pay = (12 * rm).roundToInt(), xp = 5, tag = "log"))
        }
    }
}

// ------------------------------------------------------------ andamento (chamado a cada quadro)

private fun finishStep(g: GameState, sh: Shift, st: Step) {
    val j = sh.job
    var pay = st.pay
    var note = ""
    if (pay > 0) {
        if (st.deadline >= 0) {
            if (g.absMinutes() <= st.deadline) {
                val tip = (pay * 0.3).roundToInt()
                pay += tip
                note = " (+${brl(tip)} de gorjeta pela rapidez)"
            } else {
                pay = (pay * 0.75).roundToInt()
                note = " (atrasou: -25%)"
            }
        }
        if (g.starving || g.tired) {
            pay = (pay * 0.8).roundToInt()
            note += " (cansado/com fome rende menos)"
        }
        g.money += pay
        g.careerEarned += pay
        sh.earned += pay
        if (j.id == "mecanico" && g.ownShop) g.shopRevenue += pay
    }
    if (st.xp > 0) {
        g.careerXp = g.careerXp + (j.id to workXp(g, j) + st.xp)
        sh.xpGain += st.xp
    }
    st.carry?.let { sh.carrying = it }
    when {
        st.tag == "keys" -> {
            val v = when (j.vehicle) {
                V_TAXI -> WorkVeh(V_TAXI, 215f, 7, "táxi")
                V_MOTO_ENTREGA -> WorkVeh(V_MOTO_ENTREGA, 265f, 4, "moto")
                else -> WorkVeh(j.vehicle, 150f, 0, "caminhão baú")
            }
            sh.veh = v
            g.message = "Você entrou no ${v.name}. Bom trabalho!"
        }
        st.tag == "pax" -> { sh.cust = null; g.message = "Passageiro a bordo." }
        st.tag == "cust" -> { sh.cust = null }
        st.tag.startsWith("fix:") -> {
            val p = st.tag.removePrefix("fix:").toIntOrNull()
            if (p != null && p in 0..4) sh.carCond = sh.carCond.toMutableList().also { it[p] = 100 }.toList()
        }
        st.tag == "cash" || st.tag == "tow" -> { sh.carModel = -1; sh.cust = null }
    }
    if (pay > 0) g.message = "${j.emoji} +${brl(pay)}$note"
    sh.progress = 0f
    sh.step = null
    if (sh.queue.isEmpty() && st.tag != "keys") {
        sh.tasks += 1
        g.careerTasks = g.careerTasks + (j.id to (g.careerTasks[j.id] ?: 0) + 1)
        if (canPromote(g, j)) g.message = "${g.message} ⭐ Você já pode pedir promoção (menu Trabalhar)."
        g.save()
    }
}

/**
 * Anda o turno: confere a jornada, pega o próximo passo e conta o tempo de trabalho quando o personagem
 * está no ponto (parado, e dentro do veículo da empresa quando o passo pede).
 */
fun workFrame(g: GameState, sh: Shift, px: Float, speed: Float, dt: Float) {
    val j = sh.job
    if (!workOpen(j, g)) {
        endShift(g, "A jornada acabou (${workHours(j, g)}).")
        return
    }
    var st = sh.step
    if (st == null) {
        if (sh.queue.isEmpty()) planOrder(g, sh, px)
        st = sh.queue.removeFirstOrNull()
        sh.step = st
        sh.progress = 0f
    }
    if (st == null) return
    val reach = if (st.inVeh) 34f else 15f
    val here = abs(px - st.x) < reach && abs(speed) < 38f && (!st.inVeh || sh.veh != null)
    if (here) {
        sh.progress = (sh.progress + dt / max(0.3f, st.secs)).coerceAtMost(1f)
        if (sh.progress >= 1f) finishStep(g, sh, st)
    } else if (sh.progress > 0f) {
        sh.progress = max(0f, sh.progress - dt * 0.6f)
    }
}

// ------------------------------------------------------------ desenho no mundo

/**
 * Desenha o serviço no cenário (chame dentro do translate do chão): bandeira do local de trabalho, veículo da empresa
 * estacionado, carro do cliente / carcaça, cliente ou passageiro e o ponto marcado do passo atual.
 */
fun DrawScope.drawWork(g: GameState, spr: CitySprites, cam: Float, s: Float, t: Float, p: Paint) {
    val sh = g.shift
    val job = sh?.job ?: g.heading?.let { id -> WORK_JOBS.firstOrNull { it.id == id } } ?: return
    val pulse = 0.5f + 0.5f * sin(t * 5f)

    // bandeira do local de trabalho
    if (job.x > cam - 30f && job.x < cam + VIEW_W + 30f) {
        wr(Color(0xFF424242), job.x - 0.5f, 74f, 1f, 40f, cam, s)
        wr(Color(0xFFFFD54F), job.x + 0.5f, 74f, 22f, 10f, cam, s)
        label(p, job.emoji, job.x + 11.5f, 82.5f, cam, s, 7f, 0xFF212121.toInt())
        label(p, job.place, job.x, 70f, cam, s, 5f, 0xFFFFFFFF.toInt())
    }
    if (sh == null) return

    // veículo da empresa estacionado (some quando o personagem pega as chaves)
    if (job.vehicle >= 0 && sh.veh == null) {
        val col = when (job.vehicle) { V_TAXI -> 7; V_MOTO_ENTREGA -> 4; else -> 0 }
        drawVehWorld(spr.veh, job.vehicle, Color(PAINTS[col].second), emptyList(), WORK_FULL, sh.parkX, 112f, cam, s, false, t, null, 0f, 0f)
    }
    // carro do cliente (oficina) / carcaça (ferro-velho)
    if (sh.carModel >= 0) {
        val col = Color(PAINTS[sh.carColor.coerceIn(0, PAINTS.lastIndex)].second)
        drawVehWorld(spr.veh, carSpriteFor(CATALOG[sh.carModel].body), col, emptyList(), sh.carCond, sh.carX, 112f, cam, s, false, t, null, 0f, 0f)
    }
    // cliente / passageiro esperando
    val cl = sh.cust
    if (cl != null && sh.custX > cam - 20f && sh.custX < cam + VIEW_W + 20f) {
        drawChar(cl, (sh.custX - 6f - cam) * s, (114f - 26f) * s, s, sh.custX > (sh.step?.x ?: 0f) , 0f, t * 2f)
    }
    // ponto marcado do passo atual
    val st = sh.step ?: return
    if (st.x < cam - 30f || st.x > cam + VIEW_W + 30f) return
    val cx = (st.x - cam) * s
    val gy = (if (st.inVeh) 141f else 113f) * s
    val rad = (if (st.inVeh) 17f else 9f) * s
    val ringCol = if (st.pay > 0) Color(0xFF69F0AE) else Color(0xFFFFD54F)
    drawOval(
        ringCol.copy(alpha = 0.28f + 0.25f * pulse),
        Offset(cx - rad, gy - rad * 0.38f), Size(rad * 2f, rad * 0.76f),
    )
    drawOval(
        ringCol, Offset(cx - rad, gy - rad * 0.38f), Size(rad * 2f, rad * 0.76f),
        style = Stroke(width = 1.4f * s),
    )
    if (sh.progress > 0f) {
        drawArc(
            Color.White, -90f, 360f * sh.progress, false,
            Offset(cx - rad, gy - rad * 0.38f), Size(rad * 2f, rad * 0.76f), style = Stroke(width = 2.4f * s),
        )
    }
    wr(ringCol.copy(alpha = 0.5f), st.x - 0.5f, if (st.inVeh) 100f else 82f, 1f, if (st.inVeh) 41f else 31f, cam, s)
    label(p, st.icon, st.x, (if (st.inVeh) 98f else 80f) - 2f * pulse, cam, s, 9f, 0xFFFFFFFF.toInt())
    label(p, st.label, st.x, (if (st.inVeh) 90f else 72f), cam, s, 4.8f, 0xFFFFFFFF.toInt())
}
