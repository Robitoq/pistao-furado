package com.ferrovelho.game

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.max
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

val YARD_NAMES = listOf("Ferro Velho do Zé", "Desmanche Central", "Pátio Premium")
val YARD_DESC = listOf(
    "Carros baratos e batidos (até ~R$ 60 mil de valor)",
    "De tudo um pouco (R$ 20 mil a R$ 160 mil)",
    "Esportivos, SUVs e importados (a partir de R$ 100 mil)",
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
    return 0.30 + 0.70 * (w / 100.0)
}

fun tuningPct(ids: List<String>): Double =
    ids.sumOf { id -> TUNINGS.firstOrNull { it.id == id }?.valuePct ?: 0.0 }

class GameState(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("ferrovelho", Context.MODE_PRIVATE)

    var money by mutableStateOf(START_MONEY)
    var day by mutableStateOf(1)
    var market by mutableStateOf(1.0)
    var slots by mutableStateOf(4)
    var garage by mutableStateOf(listOf<OwnedCar>())
    var yards by mutableStateOf(listOf<List<YardCar>>())
    var message by mutableStateOf("")
    var sold by mutableStateOf(0)
    var totalProfit by mutableStateOf(0)

    private var nextId = 1
    private var seed = Random.nextInt()

    val slotCost: Int get() = 15000 * (slots - 3)

    // ------------------------------------------------------------ novo jogo / dia

    fun newGame() {
        money = START_MONEY
        day = 1
        market = 1.0
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
        market = 0.92 + Random.nextDouble() * 0.16
        yards = genYards(Random(System.nanoTime()))
        var msg = "Dia $day: chegaram carros novos nos ferros-velhos."
        if (garage.isEmpty() && money < 5000) {
            money = 5000
            msg += " Seu tio emprestou R$ 5.000 pra você recomeçar."
        }
        message = msg
        save()
    }

    private fun genYards(rnd: Random): List<List<YardCar>> = List(3) { yi ->
        val lo: Int
        val hi: Int
        val n: Int
        when (yi) {
            0 -> { lo = 0; hi = 60000; n = 5 }
            1 -> { lo = 20000; hi = 160000; n = 5 }
            else -> { lo = 100000; hi = Int.MAX_VALUE; n = 4 }
        }
        val pool = CATALOG.indices.filter { CATALOG[it].base in lo..hi }
        List(n) {
            val mi = pool[rnd.nextInt(pool.size)]
            val m = CATALOG[mi]
            val year = rnd.nextInt(m.from, m.to + 1)
            val condMin = if (yi == 2) 10 else 5
            val condMax = if (yi == 2) 56 else 51
            val cond = List(5) { rnd.nextInt(condMin, condMax) }
            val price = roundTo50(baseValue(mi, year) * condFactor(cond) * rnd.nextDouble(0.55, 0.80))
            YardCar(nextId++, mi, year, rnd.nextInt(PAINTS.size), cond, price)
        }
    }

    // ------------------------------------------------------------ compra

    fun yardValue(y: YardCar): Int =
        (baseValue(y.model, y.year) * condFactor(y.cond) * market).toInt()

    fun buy(yardIdx: Int, y: YardCar) {
        if (garage.size >= slots) { message = "Garagem cheia! Venda um carro ou compre mais uma vaga."; return }
        if (y.price > money) { message = "Dinheiro insuficiente."; return }
        money -= y.price
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
        update(carId) { it.copy(color = colorIdx, spent = it.spent + cost) }
        message = "Pintou de ${PAINTS[colorIdx].first} por ${brl(cost)}."
        save()
    }

    // ------------------------------------------------------------ venda

    fun marketValue(car: OwnedCar): Int =
        (baseValue(car.model, car.year) * condFactor(car.cond) * (1.0 + tuningPct(car.tuning) * 0.8) * market).toInt()

    fun offers(car: OwnedCar): List<Offer> {
        val m = CATALOG[car.model]
        val rnd = Random(seed.toLong() * 1_000_003L + day * 7_919L + car.id)
        val bv = baseValue(car.model, car.year)
        val cf = condFactor(car.cond)
        val tp = tuningPct(car.tuning)
        val buyers = BUYERS.filter { it.ok(m) }.shuffled(rnd).take(3)
        return buyers.map { b ->
            val v = bv * cf * (1.0 + tp * b.tuneMult) * b.mult * market * (0.96 + rnd.nextDouble() * 0.08)
            Offer(b.name, b.note, roundTo50(v))
        }.sortedByDescending { it.price }
    }

    fun sell(carId: Int, o: Offer) {
        val car = garage.firstOrNull { it.id == carId } ?: return
        val profit = o.price - car.paid - car.spent
        money += o.price
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
            val g = o.getJSONArray("garage")
            garage = List(g.length()) { g.getJSONObject(it).toOwned() }
            val ys = o.getJSONArray("yards")
            yards = List(ys.length()) { yi ->
                val a = ys.getJSONArray(yi)
                List(a.length()) { a.getJSONObject(it).toYard() }
            }
            if (yards.size != 3) throw IllegalStateException("save antigo")
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
