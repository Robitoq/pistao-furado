package com.ferrovelho.game

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

const val START_MONEY = 15000
const val MAX_COND = 100

val PART_NAMES = listOf("Motor", "Lataria", "Pneus", "Interior", "Suspensão")
val PART_WEIGHT = listOf(0.30, 0.25, 0.10, 0.15, 0.20)

/** Custo de reparo por ponto de condição, como fração do valor do carro. */
val PART_COST = listOf(0.0018, 0.0014, 0.0004, 0.0009, 0.0011)

val PAINTS: List<Pair<String, Long>> = listOf(
    "Branco" to 0xFFF5F5F5,
    "Preto" to 0xFF212121,
    "Prata" to 0xFFB0BEC5,
    "Cinza" to 0xFF616161,
    "Vermelho" to 0xFFC62828,
    "Azul" to 0xFF1565C0,
    "Verde" to 0xFF2E7D32,
    "Amarelo" to 0xFFF9A825,
    "Laranja" to 0xFFEF6C00,
    "Roxo" to 0xFF6A1B9A,
    "Rosa" to 0xFFD81B60,
    "Bege" to 0xFFD7B98E,
    "Vinho" to 0xFF6D1B2A,
    "Azul bebê" to 0xFF4FC3F7,
)

class Tuning(val id: String, val name: String, val minPrice: Int, val pricePct: Double, val valuePct: Double)

val TUNINGS = listOf(
    Tuning("som", "🔊 Som potente", 1500, 0.030, 0.025),
    Tuning("rodas", "🛞 Rodas esportivas", 2000, 0.050, 0.040),
    Tuning("rebaixada", "⬇️ Suspensão rebaixada", 1200, 0.030, 0.025),
    Tuning("pelicula", "🕶️ Película fumê", 500, 0.010, 0.008),
    Tuning("adesivo", "🏁 Adesivos / faixa", 400, 0.010, 0.010),
    Tuning("escape", "💨 Escape esportivo", 1500, 0.030, 0.025),
    Tuning("couro", "💺 Bancos de couro", 2500, 0.040, 0.035),
    Tuning("neon", "💡 Neon embaixo", 300, 0.008, 0.005),
    Tuning("turbo", "🚀 Kit turbo", 6000, 0.120, 0.100),
    Tuning("aerofolio", "🏎️ Aerofólio", 1800, 0.035, 0.030),
    Tuning("parachoque", "🛡️ Kit para-choque esportivo", 2200, 0.040, 0.035),
)

class Buyer(
    val name: String,
    val note: String,
    val tuneMult: Double,
    val mult: Double,
    val ok: (CarModel) -> Boolean,
)

val BUYERS = listOf(
    Buyer("Comprador comum", "preço de mercado", 0.8, 0.95) { true },
    Buyer("Pai de família", "gosta de carro inteiro, sem frescura", 0.3, 1.02) {
        it.cat == Cat.POPULAR || it.cat == Cat.SEDAN || it.cat == Cat.SUV
    },
    Buyer("Tunador", "paga bem por tuning", 2.0, 0.90) { it.cat != Cat.PICAPE },
    Buyer("Revendedor", "quer margem, oferta baixa", 0.4, 0.82) { true },
    Buyer("Colecionador", "paga ágio por clássico", 1.0, 1.30) { it.cat == Cat.CLASSICO },
    Buyer("Fazendeiro", "procura picape e SUV", 0.3, 1.10) { it.cat == Cat.PICAPE || it.cat == Cat.SUV },
    Buyer("Fã de esportivos", "paga bem por esportivo tunado", 1.5, 1.08) { it.cat == Cat.ESPORTIVO },
)

class MarketEvent(val title: String, val text: String, val all: Double, val cats: Map<Cat, Double> = emptyMap())

val EVENTS = listOf(
    MarketEvent("📉 Crise de crédito", "financiamento travou, ninguém compra", 0.88),
    MarketEvent("📈 Feirão de carros", "movimento forte nas lojas", 1.08),
    MarketEvent("⛽ Gasolina nas alturas", "SUV e picape caem, popular sobe", 1.0,
        mapOf(Cat.SUV to 0.88, Cat.PICAPE to 0.90, Cat.POPULAR to 1.06)),
    MarketEvent("🌾 Safra recorde", "picape e SUV em alta no campo", 1.0, mapOf(Cat.PICAPE to 1.15, Cat.SUV to 1.06)),
    MarketEvent("💵 Dólar disparou", "esportivos e importados mais caros", 1.0, mapOf(Cat.ESPORTIVO to 1.15, Cat.SUV to 1.05)),
    MarketEvent("🚗 Encontro de antigos", "clássicos valorizados", 1.0, mapOf(Cat.CLASSICO to 1.18)),
    MarketEvent("🧾 Época de IPVA", "muita gente vendendo, preços caem", 0.94),
)

/** Sazonalidade por mês (jan = IPVA, dez = 13º salário). */
val SEASON = listOf(0.95, 0.98, 1.0, 1.0, 1.0, 1.0, 1.02, 1.0, 1.0, 1.0, 1.01, 1.04)

val YARD_NAMES = listOf("Ferro Velho do Zé", "Desmanche Central", "Pátio Premium", "Sucata do Beto", "Pátio do Rodrigo")
val YARD_DESC = listOf(
    "Carros baratos e batidos (até ~R$ 60 mil de valor)",
    "De tudo um pouco (R$ 20 mil a R$ 160 mil)",
    "Esportivos, SUVs e importados (a partir de R$ 100 mil)",
    "Lataria barata e história esquisita (até ~R$ 40 mil)",
    "Carros honestos de meio de faixa (R$ 40 mil a R$ 130 mil)",
)

data class OwnedCar(
    val id: Int,
    val model: Int,
    val year: Int,
    val color: Int,
    val cond: List<Int>,
    val tuning: List<String>,
    val paid: Int,
    val spent: Int = 0,
)

data class YardCar(
    val id: Int,
    val model: Int,
    val year: Int,
    val color: Int,
    val cond: List<Int>,
    val price: Int,
)

class Offer(val buyer: String, val note: String, val price: Int)

private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")

fun brl(v: Int): String = "R$ " + String.format(PT_BR, "%,d", v)

fun roundTo50(v: Double): Int = max(50, (v / 50.0).roundToInt() * 50)

fun baseValue(model: Int, year: Int): Double {
    val m = CATALOG[model]
    val f = if (m.cat == Cat.CLASSICO) 1.0 else max(0.55, 1.0 - 0.03 * (m.to - year))
    return m.base * f
}

fun condFactor(cond: List<Int>): Double {
    var w = 0.0
    for (i in cond.indices) w += cond[i] * PART_WEIGHT[i]
    // curva convexa: carro ruim vale muito pouco; carro inteiro vale o preço cheio
    var f = 0.10 + 0.90 * (w / 100.0).pow(1.6)
    if (cond[0] < 20) f *= 0.75 // motor fundido: só comprador de sucata
    return f
}

fun tuningPct(ids: List<String>): Double =
    ids.sumOf { id -> TUNINGS.firstOrNull { it.id == id }?.valuePct ?: 0.0 }

class GameState(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("ferrovelho", Context.MODE_PRIVATE)

    var money by mutableStateOf(START_MONEY)
    var day by mutableStateOf(1)
    var market by mutableStateOf(1.0)
    var minutes by mutableStateOf(8 * 60)
    var startMillis by mutableStateOf(System.currentTimeMillis())
    var trend by mutableStateOf(0.0)
    var catMkt by mutableStateOf(mapOf<String, Double>())
    var eventIdx by mutableStateOf(-1)
    var eventLeft by mutableStateOf(0)
    var history by mutableStateOf(listOf(1.0))
    var slots by mutableStateOf(4)
    var garage by mutableStateOf(listOf<OwnedCar>())
    var yards by mutableStateOf(listOf<List<YardCar>>())
    var message by mutableStateOf("")
    var sold by mutableStateOf(0)
    var totalProfit by mutableStateOf(0)

    private var nextId = 1
    private var seed = Random.nextInt()

    val slotCost: Int get() = 15000 * (slots - 3)

    // ------------------------------------------------------------ data / hora / mercado

    private fun cal(): Calendar = Calendar.getInstance().apply {
        timeInMillis = startMillis
        add(Calendar.DAY_OF_YEAR, day - 1)
    }

    fun dateText(): String =
        (if (minutes < 6 * 60 || minutes >= 19 * 60) "🌙 " else "☀️ ") +
            SimpleDateFormat("EEE, dd/MM/yyyy", PT_BR).format(cal().time) +
            " • " + String.format(PT_BR, "%02d:%02d", minutes / 60, minutes % 60)

    private fun tick(min: Int) { minutes = (minutes + min).coerceAtMost(23 * 60 + 59) }

    fun spendTime(min: Int) = tick(min)

    /** Relógio andando sozinho: chamado a cada segundo pela UI. */
    fun clockTick() { if (minutes < 23 * 60 + 59) tick(2) }

    val event: MarketEvent? get() = EVENTS.getOrNull(eventIdx)

    fun marketIndex(): Double = market * SEASON[cal().get(Calendar.MONTH)] * (event?.all ?: 1.0)

    fun marketFor(cat: Cat): Double =
        marketIndex() * (catMkt[cat.name] ?: 1.0) * (event?.cats?.get(cat) ?: 1.0)

    fun trendArrow(): String {
        val h = history
        if (h.size < 2) return "▬"
        val d = h.last() - h[h.size - 2]
        return if (d > 0.004) "▲" else if (d < -0.004) "▼" else "▬"
    }

    private fun stepMarket(): String {
        val r = Random.Default
        trend = trend * 0.8 + (r.nextDouble() - 0.5) * 0.04
        market = (market + (1.0 - market) * 0.04 + trend).coerceIn(0.75, 1.30)
        catMkt = Cat.values().associate { c ->
            val v = catMkt[c.name] ?: 1.0
            c.name to (v + (1.0 - v) * 0.08 + (r.nextDouble() - 0.5) * 0.05).coerceIn(0.8, 1.25)
        }
        var news = ""
        if (eventIdx >= 0) {
            eventLeft -= 1
            if (eventLeft <= 0) { eventIdx = -1; news = "O mercado se acalmou." }
        }
        if (eventIdx < 0 && r.nextDouble() < 0.07) {
            eventIdx = r.nextInt(EVENTS.size)
            eventLeft = r.nextInt(4, 10)
            news = "${EVENTS[eventIdx].title}: ${EVENTS[eventIdx].text}"
        }
        history = (history + marketIndex()).takeLast(30)
        return news
    }

    // ------------------------------------------------------------ novo jogo / dia

    fun newGame() {
        money = START_MONEY
        day = 1
        market = 1.0
        minutes = 8 * 60
        startMillis = System.currentTimeMillis()
        trend = 0.0
        catMkt = emptyMap()
        eventIdx = -1
        eventLeft = 0
        history = listOf(1.0)
        slots = 4
        garage = emptyList()
        sold = 0
        totalProfit = 0
        nextId = 1
        seed = Random.nextInt()
        yards = genYards(Random(System.nanoTime()))
        message = "Novo jogo! Você tem ${brl(START_MONEY)}. Dá uma olhada no ferro velho 🚗"
        save()
    }

    fun nextDay() {
        day += 1
        minutes = 8 * 60
        val news = stepMarket()
        yards = genYards(Random(System.nanoTime()))
        var msg = "Dia $day: chegaram carros novos nos ferros-velhos."
        if (news.isNotEmpty()) msg += " 📰 $news"
        if (garage.isEmpty() && money < 5000) {
            money = 5000
            msg += " Seu tio emprestou R$ 5.000 pra você recomeçar."
        }
        message = msg
        save()
    }

    private fun genYards(rnd: Random): List<List<YardCar>> = List(5) { yi ->
        val lo: Int
        val hi: Int
        val n: Int
        when (yi) {
            0 -> { lo = 0; hi = 60000; n = 5 }
            1 -> { lo = 20000; hi = 160000; n = 5 }
            2 -> { lo = 100000; hi = Int.MAX_VALUE; n = 4 }
            3 -> { lo = 0; hi = 40000; n = 4 }
            else -> { lo = 40000; hi = 130000; n = 4 }
        }
        val pool = CATALOG.indices.filter { CATALOG[it].base in lo..hi }
        List(n) {
            val mi = pool[rnd.nextInt(pool.size)]
            val m = CATALOG[mi]
            val year = rnd.nextInt(m.from, m.to + 1)
            val condMin = if (yi == 2) 10 else 5
            val condMax = if (yi == 2) 56 else 51
            val cond = List(5) { rnd.nextInt(condMin, condMax) }
            val price = roundTo50(baseValue(mi, year) * condFactor(cond) * marketFor(m.cat) * rnd.nextDouble(0.55, 0.80))
            YardCar(nextId++, mi, year, rnd.nextInt(PAINTS.size), cond, price)
        }
    }

    // ------------------------------------------------------------ compra

    fun yardValue(y: YardCar): Int =
        (baseValue(y.model, y.year) * condFactor(y.cond) * marketFor(CATALOG[y.model].cat)).toInt()

    fun buy(yardIdx: Int, y: YardCar) {
        if (garage.size >= slots) { message = "Garagem cheia! Venda um carro ou compre mais uma vaga."; return }
        if (y.price > money) { message = "Dinheiro insuficiente."; return }
        money -= y.price
        tick(30)
        garage = garage + OwnedCar(y.id, y.model, y.year, y.color, y.cond, emptyList(), y.price)
        yards = yards.mapIndexed { i, l -> if (i == yardIdx) l.filter { it.id != y.id } else l }
        message = "Comprou ${CATALOG[y.model].fullName} ${y.year} por ${brl(y.price)}. Bora arrumar!"
        save()
    }

    fun buySlot() {
        val c = slotCost
        if (c > money) { message = "Uma vaga nova custa ${brl(c)}."; return }
        money -= c
        slots += 1
        message = "Mais uma vaga na garagem! Agora são $slots."
        save()
    }

    // ------------------------------------------------------------ oficina

    private fun update(id: Int, f: (OwnedCar) -> OwnedCar) {
        garage = garage.map { if (it.id == id) f(it) else it }
    }

    fun repairCost(car: OwnedCar, part: Int, to: Int): Int {
        val pts = max(0, to.coerceAtMost(MAX_COND) - car.cond[part])
        return (pts * baseValue(car.model, car.year) * PART_COST[part]).roundToInt()
    }

    fun repair(carId: Int, part: Int, to: Int) {
        val car = garage.firstOrNull { it.id == carId } ?: return
        val target = to.coerceAtMost(MAX_COND)
        if (target <= car.cond[part]) return
        val cost = repairCost(car, part, target)
        if (cost > money) { message = "Dinheiro insuficiente."; return }
        money -= cost
        tick(30)
        update(carId) {
            val l = it.cond.toMutableList()
            l[part] = target
            it.copy(cond = l, spent = it.spent + cost)
        }
        save()
    }

    fun repairAllCost(car: OwnedCar): Int = PART_NAMES.indices.sumOf { repairCost(car, it, MAX_COND) }

    fun repairAll(carId: Int) {
        val car = garage.firstOrNull { it.id == carId } ?: return
        val cost = repairAllCost(car)
        if (cost == 0) return
        if (cost > money) { message = "Dinheiro insuficiente pra reparar tudo (${brl(cost)})."; return }
        money -= cost
        tick(120)
        update(carId) { it.copy(cond = List(5) { MAX_COND }, spent = it.spent + cost) }
        message = "Carro 100% revisado por ${brl(cost)}."
        save()
    }

    fun tuningPrice(car: OwnedCar, t: Tuning): Int =
        roundTo50(max(t.minPrice.toDouble(), baseValue(car.model, car.year) * t.pricePct))

    fun install(carId: Int, t: Tuning) {
        val car = garage.firstOrNull { it.id == carId } ?: return
        if (t.id in car.tuning) return
        val price = tuningPrice(car, t)
        if (price > money) { message = "Dinheiro insuficiente."; return }
        money -= price
        tick(90)
        update(carId) { it.copy(tuning = it.tuning + t.id, spent = it.spent + price) }
        message = "Instalou ${t.name} por ${brl(price)}."
        save()
    }

    fun paintCost(car: OwnedCar): Int = roundTo50(max(1200.0, baseValue(car.model, car.year) * 0.025))

    fun paint(carId: Int, colorIdx: Int) {
        val car = garage.firstOrNull { it.id == carId } ?: return
        if (car.color == colorIdx) return
        val cost = paintCost(car)
        if (cost > money) { message = "Pintura custa ${brl(cost)}."; return }
        money -= cost
        tick(120)
        update(carId) { it.copy(color = colorIdx, spent = it.spent + cost) }
        message = "Pintou de ${PAINTS[colorIdx].first} por ${brl(cost)}."
        save()
    }

    // ------------------------------------------------------------ venda

    fun marketValue(car: OwnedCar): Int =
        (baseValue(car.model, car.year) * condFactor(car.cond) * (1.0 + tuningPct(car.tuning) * 0.8) * marketFor(CATALOG[car.model].cat)).toInt()

    fun offers(car: OwnedCar): List<Offer> {
        val m = CATALOG[car.model]
        val rnd = Random(seed.toLong() * 1_000_003L + day * 7_919L + car.id)
        val bv = baseValue(car.model, car.year)
        val cf = condFactor(car.cond)
        val tp = tuningPct(car.tuning)
        val mk = marketFor(m.cat)
        val buyers = BUYERS.filter { it.ok(m) && !(cf < 0.45 && it.name == "Pai de família") }.shuffled(rnd).take(3)
        return buyers.map { b ->
            val v = bv * cf * (1.0 + tp * b.tuneMult) * b.mult * mk * (0.96 + rnd.nextDouble() * 0.08)
            Offer(b.name, b.note, roundTo50(v))
        }.sortedByDescending { it.price }
    }

    fun sell(carId: Int, o: Offer) {
        val car = garage.firstOrNull { it.id == carId } ?: return
        val profit = o.price - car.paid - car.spent
        money += o.price
        tick(30)
        sold += 1
        totalProfit += profit
        garage = garage.filter { it.id != carId }
        val name = CATALOG[car.model].fullName
        message = if (profit >= 0) {
            "Vendeu $name por ${brl(o.price)} — lucro de ${brl(profit)} 💰"
        } else {
            "Vendeu $name por ${brl(o.price)} — prejuízo de ${brl(-profit)} 😬"
        }
        save()
    }

    // ------------------------------------------------------------ salvar / carregar

    fun save() {
        val o = JSONObject()
        o.put("money", money)
        o.put("day", day)
        o.put("market", market)
        o.put("slots", slots)
        o.put("sold", sold)
        o.put("profit", totalProfit)
        o.put("nextId", nextId)
        o.put("seed", seed)
        o.put("minutes", minutes)
        o.put("start", startMillis)
        o.put("trend", trend)
        o.put("eventIdx", eventIdx)
        o.put("eventLeft", eventLeft)
        o.put("history", JSONArray().also { a -> history.forEach { a.put(it) } })
        o.put("cat", JSONObject().also { j -> catMkt.forEach { (k, v) -> j.put(k, v) } })
        val g = JSONArray()
        garage.forEach { g.put(it.toJson()) }
        o.put("garage", g)
        val ys = JSONArray()
        yards.forEach { list ->
            val a = JSONArray()
            list.forEach { a.put(it.toJson()) }
            ys.put(a)
        }
        o.put("yards", ys)
        prefs.edit().putString("save", o.toString()).apply()
    }

    fun load() {
        val s = prefs.getString("save", null)
        if (s == null) {
            newGame()
            return
        }
        try {
            val o = JSONObject(s)
            money = o.getInt("money")
            day = o.getInt("day")
            market = o.getDouble("market")
            slots = o.getInt("slots")
            sold = o.getInt("sold")
            totalProfit = o.getInt("profit")
            nextId = o.getInt("nextId")
            seed = o.getInt("seed")
            minutes = o.optInt("minutes", 8 * 60)
            startMillis = o.optLong("start", System.currentTimeMillis())
            trend = o.optDouble("trend", 0.0)
            eventIdx = o.optInt("eventIdx", -1)
            eventLeft = o.optInt("eventLeft", 0)
            history = o.optJSONArray("history")?.let { a -> List(a.length()) { a.getDouble(it) } } ?: listOf(market)
            catMkt = o.optJSONObject("cat")?.let { j -> j.keys().asSequence().associateWith { j.getDouble(it) } } ?: emptyMap()
            val g = o.getJSONArray("garage")
            garage = List(g.length()) { g.getJSONObject(it).toOwned() }
            val ys = o.getJSONArray("yards")
            yards = List(ys.length()) { yi ->
                val a = ys.getJSONArray(yi)
                List(a.length()) { a.getJSONObject(it).toYard() }
            }
            if (yards.size < 3) throw IllegalStateException("save antigo")
            if (yards.size < 5) yards = yards + genYards(Random(System.nanoTime())).drop(yards.size)
            message = ""
        } catch (e: Exception) {
            newGame()
        }
    }
}

private fun JSONArray.ints(): List<Int> = List(length()) { getInt(it) }
private fun JSONArray.strings(): List<String> = List(length()) { getString(it) }
private fun List<Int>.toJsonArray(): JSONArray = JSONArray().also { a -> forEach { a.put(it) } }
private fun List<String>.toJsonArrayS(): JSONArray = JSONArray().also { a -> forEach { a.put(it) } }

private fun OwnedCar.toJson(): JSONObject = JSONObject()
    .put("id", id).put("model", model).put("year", year).put("color", color)
    .put("cond", cond.toJsonArray()).put("tuning", tuning.toJsonArrayS())
    .put("paid", paid).put("spent", spent)

private fun JSONObject.toOwned(): OwnedCar = OwnedCar(
    getInt("id"), getInt("model"), getInt("year"), getInt("color"),
    getJSONArray("cond").ints(), getJSONArray("tuning").strings(),
    getInt("paid"), getInt("spent"),
)

private fun YardCar.toJson(): JSONObject = JSONObject()
    .put("id", id).put("model", model).put("year", year).put("color", color)
    .put("cond", cond.toJsonArray()).put("price", price)

private fun JSONObject.toYard(): YardCar = YardCar(
    getInt("id"), getInt("model"), getInt("year"), getInt("color"),
    getJSONArray("cond").ints(), getInt("price"),
)
