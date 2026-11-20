package com.ferrovelho.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.abs
import kotlin.random.Random

// ------------------------------------------------------------ boneco pixel

class BuyerLook(val female: Boolean, val skin: Color, val hair: Color, val shirt: Color, val pants: Color)

// boneco 16x28: cada linha tem só a metade esquerda (8 colunas); o espelho é montado na hora
// k contorno, h cabelo, H cabelo escuro, s pele, S pele escura, e olho, w branco do olho, m boca,
// c camisa, C camisa escura, p calça/saia, P calça escura, o sapato
private val MAN = listOf(
    "....kkkk", "...khhhh", "..khhhhh", "..khhhhh", "..khhhhh", "..khhsss", "..kssHss", "..kswess",
    "..kssssS", "..kssssm", "...kssss", "....kSSS",
    "..kccccc", ".kccCccc", ".kccCccc", ".ksSCccc", ".ksSCccc", ".kssCccc", "..kkCccc", "...kkkkk",
    "...kpppp", "...kpppp", "...kpppk", "...kpppk", "...kpppk", "...kpppk", "..kooook", "..kkkkkk",
)
private val WOMAN = listOf(
    "....kkkk", "...khhhh", "..khhhhh", "..khhhhh", ".khhhhhh", ".khhhsss", ".khhsHss", ".khhwess",
    ".khhsssS", ".khhsssm", "..khssss", "..khkSSS",
    ".khhcccc", ".khhCccc", ".kccCccc", ".ksSCccc", ".ksSCccc", ".kssCccc", "..kkCccc", "...kkkkk",
    "..kppppp", ".kpppppp", ".kppPppp", "...kssSk", "...ksssk", "...ksssk", "..kooook", "..kkkkkk",
)

private fun shade(c: Color, f: Float) = Color(c.red * f, c.green * f, c.blue * f, 1f)

private val SKINS = listOf(0xFFFFDBB4, 0xFFE8B98A, 0xFFC68642, 0xFF8D5524, 0xFF5C3A1E).map { Color(it) }
private val HAIRS = listOf(0xFF1B1B1B, 0xFF4E342E, 0xFF8D6E63, 0xFFE0B04F, 0xFFB71C1C, 0xFF9E9E9E).map { Color(it) }
private val SHIRTS = listOf(0xFF1E88E5, 0xFFE53935, 0xFF43A047, 0xFFFDD835, 0xFF8E24AA, 0xFFFB8C00, 0xFFECEFF1, 0xFF212121).map { Color(it) }
private val PANTS = listOf(0xFF283593, 0xFF37474F, 0xFF5D4037, 0xFF212121, 0xFF1565C0).map { Color(it) }
private val M_NAMES = listOf("Carlos", "João", "Marcos", "Pedro", "Rogério", "Tiago", "Seu Antônio", "Lucas", "Beto", "Wagner")
private val F_NAMES = listOf("Mariana", "Fernanda", "Dona Cida", "Juliana", "Patrícia", "Camila", "Luana", "Rita", "Bianca", "Sandra")

@Composable
fun PixelPerson(look: BuyerLook, modifier: Modifier) {
    Canvas(modifier) {
        val half = if (look.female) WOMAN else MAN
        val cw = size.width / 16f
        val ch = size.height / half.size
        val outline = Color(0xFF1E181C)
        half.forEachIndexed { y, hrow ->
            val row = hrow + hrow.reversed()
            row.forEachIndexed { x, c ->
                val col = when (c) {
                    'k' -> outline
                    'h' -> look.hair
                    'H' -> shade(look.hair, 0.7f)
                    's' -> look.skin
                    'S' -> shade(look.skin, 0.82f)
                    'e' -> Color(0xFF1E181C)
                    'w' -> Color.White
                    'm' -> Color(0xFFB04A4A)
                    'c' -> look.shirt
                    'C' -> shade(look.shirt, 0.75f)
                    'p' -> look.pants
                    'P' -> shade(look.pants, 0.75f)
                    'o' -> Color(0xFF5A3C2D)
                    else -> null
                }
                if (col != null) drawRect(col, Offset(x * cw, y * ch), Size(cw + 0.5f, ch + 0.5f))
            }
        }
    }
}

// ------------------------------------------------------------ conversa

private val OPEN = mapOf(
    "Comprador comum" to listOf("Oi! Vi o {c} e gostei. Pago {p}, que acha?", "Bom dia! Tô atrás de um {c}. Faço {p}."),
    "Pai de família" to listOf("Preciso de um carro seguro pra família. {p} no {c}?", "Esse {c} serve pra levar as crianças. Pago {p}."),
    "Tunador" to listOf("Esse {c} tem potencial! Te dou {p} agora.", "Opa, {c}! Dá pra fazer um projeto. {p}?"),
    "Revendedor" to listOf("Vou ser sincero: {p} e levo hoje, sem enrolação.", "Compro {c} toda semana. Pago {p} à vista."),
    "Colecionador" to listOf("Que belezura de {c}! Ofereço {p}.", "Procuro um {c} há anos. {p}, topa?"),
    "Fazendeiro" to listOf("Esse {c} aguenta a estrada de terra? Pago {p}.", "Preciso de um {c} pra fazenda. {p}."),
    "Fã de esportivos" to listOf("Uau, um {c}! Pago {p} sem pensar muito.", "Sempre sonhei com um {c}. {p}?"),
)
private val AGREE = listOf("Hmm... beleza, fechado por {p}!", "Tá bom, você convenceu. {p}!", "Ok, {p} e não se fala mais nisso.")
private val COUNTER = listOf("Muito caro! Chego em {p}, no máximo.", "Aí pesou. Que tal {p}?", "Quase lá... {p} e fechamos?")
private val LEAVE = listOf("Assim não dá. Vou ver outro carro, tchau!", "Tá fora da minha realidade. Valeu mesmo assim!")

class Haggle(val name: String, val look: BuyerLook, val type: String, val maxPrice: Int, startPrice: Int) {
    var cur by mutableStateOf(startPrice)
    var rounds by mutableStateOf(3)
    var state by mutableStateOf(0) // 0 negociando, 1 acordo, 2 foi embora
    var line by mutableStateOf("")
    var said by mutableStateOf("")
}

fun newHaggle(g: GameState, car: OwnedCar): Haggle {
    val r = Random(System.nanoTime())
    val o = g.offers(car).random(r)
    val female = r.nextBoolean()
    val look = BuyerLook(female, SKINS.random(r), HAIRS.random(r), SHIRTS.random(r), PANTS.random(r))
    val carName = CATALOG[car.model].fullName
    val h = Haggle((if (female) F_NAMES else M_NAMES).random(r), look, o.buyer, roundTo50(o.price * r.nextDouble(1.04, 1.22)), o.price)
    h.line = (OPEN[o.buyer] ?: listOf("Quanto é o {c}? Pago {p}.")).random(r)
        .replace("{c}", carName).replace("{p}", brl(o.price))
    return h
}

private fun Haggle.ask(pct: Double, text: String) {
    val ask = roundTo50(cur * pct)
    said = "Você: $text"
    if (ask <= maxPrice) {
        cur = ask
        state = 1
        line = AGREE.random().replace("{p}", brl(ask))
        return
    }
    rounds -= 1
    if (rounds <= 0 || Random.nextDouble() < 0.2) {
        state = 2
        line = LEAVE.random()
        return
    }
    cur = roundTo50(cur + (maxPrice - cur) * 0.5).coerceAtLeast(cur)
    line = COUNTER.random().replace("{p}", brl(cur))
}

@Composable
fun BuyerDialog(h: Haggle, invested: Int, onSell: (Int) -> Unit, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PixelPerson(h.look, Modifier.width(72.dp).height(126.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(h.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(h.type, fontSize = 11.sp)
                        Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 6.dp) {
                            Text("" + h.line, Modifier.padding(10.dp), fontSize = 14.sp)
                        }
                        if (h.said.isNotEmpty()) Text(h.said, fontSize = 12.sp)
                    }
                }
                val diff = h.cur - invested
                Text(
                    "Oferta: ${brl(h.cur)} • " + (if (diff >= 0) "lucro " else "prejuízo ") + brl(abs(diff)),
                    fontSize = 12.sp,
                )
                when (h.state) {
                    0 -> {
                        Button({ onSell(h.cur) }, Modifier.fillMaxWidth()) { Text("Fechado por ${brl(h.cur)}!") }
                        FilledTonalButton({ h.ask(1.08, "Dá pra fazer ${brl(roundTo50(h.cur * 1.08))}?") }, Modifier.fillMaxWidth()) {
                            Text("Dá pra fazer ${brl(roundTo50(h.cur * 1.08))}?")
                        }
                        FilledTonalButton({ h.ask(1.20, "Vale mais, quero ${brl(roundTo50(h.cur * 1.20))}.") }, Modifier.fillMaxWidth()) {
                            Text("Vale mais: ${brl(roundTo50(h.cur * 1.20))}")
                        }
                        TextButton(onClose, Modifier.fillMaxWidth()) { Text("Não, obrigado") }
                    }
                    1 -> {
                        Button({ onSell(h.cur) }, Modifier.fillMaxWidth()) { Text("Vender por ${brl(h.cur)}") }
                        TextButton(onClose, Modifier.fillMaxWidth()) { Text("Deixa pra lá") }
                    }
                    else -> Button(onClose, Modifier.fillMaxWidth()) { Text("Fechar") }
                }
            }
        }
    }
}
