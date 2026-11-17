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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.abs
import kotlin.random.Random

// ------------------------------------------------------------ boneco pixel

class BuyerLook(val female: Boolean, val skin: Color, val hair: Color, val shirt: Color, val pants: Color, val type: String = "", val beard: Boolean = false)

private val SKINS = listOf(0xFFFFDBB4, 0xFFE8B98A, 0xFFC68642, 0xFF8D5524, 0xFF5C3A1E).map { Color(it) }
private val HAIRS = listOf(0xFF1B1B1B, 0xFF4E342E, 0xFF8D6E63, 0xFFE0B04F, 0xFFB71C1C, 0xFF9E9E9E).map { Color(it) }
private val SHIRTS = listOf(0xFF1E88E5, 0xFFE53935, 0xFF43A047, 0xFFFDD835, 0xFF8E24AA, 0xFFFB8C00, 0xFFECEFF1, 0xFF212121).map { Color(it) }
private val PANTS = listOf(0xFF283593, 0xFF37474F, 0xFF5D4037, 0xFF212121, 0xFF1565C0).map { Color(it) }
private val M_NAMES = listOf("Carlos", "João", "Marcos", "Pedro", "Rogério", "Tiago", "Seu Antônio", "Lucas", "Beto", "Wagner")
private val F_NAMES = listOf("Mariana", "Fernanda", "Dona Cida", "Juliana", "Patrícia", "Camila", "Luana", "Rita", "Bianca", "Sandra")

@Composable
fun PixelPerson(look: BuyerLook, modifier: Modifier) {
    Canvas(modifier) {
        val k = minOf(size.width / 64f, size.height / 112f)
        withTransform({ translate((size.width - 64f * k) / 2f, 0f); scale(k, k, Offset.Zero) }) { drawPerson(look) }
    }
}

private fun DrawScope.drawPerson(l: BuyerLook) {
    val skin = l.skin
    val skinD = lerp(skin, Color.Black, 0.18f)
    val shirtD = lerp(l.shirt, Color.Black, 0.25f)
    val ink = Color(0xFF1B1B1B)
    drawOval(Color(0x44000000), Offset(10f, 104f), Size(44f, 7f))
    // cabelo comprido atrás
    if (l.female) drawRoundRect(l.hair, Offset(15f, 12f), Size(34f, 52f), CornerRadius(14f))
    // pernas / calça / sapatos
    if (l.female && l.type != "Fazendeiro") {
        drawRect(skin, Offset(21f, 84f), Size(9f, 18f)); drawRect(skin, Offset(34f, 84f), Size(9f, 18f))
        drawRoundRect(l.pants, Offset(15f, 66f), Size(34f, 26f), CornerRadius(4f))
    } else {
        drawRoundRect(l.pants, Offset(18f, 68f), Size(13f, 35f), CornerRadius(2f))
        drawRoundRect(l.pants, Offset(33f, 68f), Size(13f, 35f), CornerRadius(2f))
        drawLine(Color(0x55000000), Offset(32f, 74f), Offset(32f, 102f), 1f)
    }
    drawRoundRect(Color(0xFF3E2723), Offset(16f, 101f), Size(16f, 7f), CornerRadius(3f))
    drawRoundRect(Color(0xFF3E2723), Offset(32f, 101f), Size(16f, 7f), CornerRadius(3f))
    // braços
    drawRoundRect(skin, Offset(6f, 50f), Size(8f, 26f), CornerRadius(4f))
    drawRoundRect(skin, Offset(50f, 50f), Size(8f, 26f), CornerRadius(4f))
    drawCircle(skin, 4.6f, Offset(10f, 78f)); drawCircle(skin, 4.6f, Offset(54f, 78f))
    // pescoço e tronco
    drawRect(skinD, Offset(27f, 38f), Size(10f, 10f))
    drawRoundRect(l.shirt, Offset(12f, 44f), Size(40f, 30f), CornerRadius(8f))
    drawRoundRect(l.shirt, Offset(4f, 46f), Size(12f, 16f), CornerRadius(5f))
    drawRoundRect(l.shirt, Offset(48f, 46f), Size(12f, 16f), CornerRadius(5f))
    drawPath(Path().apply { moveTo(25f, 44f); lineTo(32f, 54f); lineTo(39f, 44f); close() }, skinD)
    drawLine(shirtD, Offset(25f, 44f), Offset(32f, 54f), 1.4f); drawLine(shirtD, Offset(39f, 44f), Offset(32f, 54f), 1.4f)
    drawLine(shirtD, Offset(32f, 54f), Offset(32f, 72f), 0.8f)
    if (!l.female) drawRect(Color(0xFF2B1B12), Offset(18f, 68f), Size(28f, 3f))
    if (l.type == "Colecionador") drawRoundRect(Color(0xFF263238), Offset(12f, 48f), Size(13f, 26f), CornerRadius(4f))
    if (l.type == "Colecionador") drawRoundRect(Color(0xFF263238), Offset(39f, 48f), Size(13f, 26f), CornerRadius(4f))
    if (l.type == "Revendedor") { // corrente de ouro
        drawArc(Color(0xFFFFC107), 0f, 180f, false, Offset(24f, 42f), Size(16f, 14f), style = Stroke(1.3f))
    }
    // cabeça
    drawCircle(skin, 4f, Offset(17.5f, 25f)); drawCircle(skin, 4f, Offset(46.5f, 25f))
    drawRoundRect(skin, Offset(18f, 8f), Size(28f, 32f), CornerRadius(13f))
    drawArc(Color(0x22000000), 20f, 140f, false, Offset(20f, 22f), Size(24f, 18f), style = Stroke(2f))
    // cabelo
    if (l.female) {
        drawArc(l.hair, 180f, 180f, true, Offset(17f, 5f), Size(30f, 26f))
        drawPath(Path().apply { moveTo(18f, 20f); lineTo(32f, 9f); lineTo(46f, 20f); lineTo(40f, 16f); lineTo(26f, 15f); close() }, l.hair)
    } else {
        drawArc(l.hair, 180f, 180f, true, Offset(17f, 4f), Size(30f, 24f))
        drawRect(l.hair, Offset(17f, 15f), Size(3f, 8f)); drawRect(l.hair, Offset(44f, 15f), Size(3f, 8f))
    }
    // rosto
    val eyeY = 26f
    for (ex in listOf(25f, 39f)) {
        drawOval(Color.White, Offset(ex - 3.4f, eyeY - 2.4f), Size(6.8f, 4.8f))
        drawCircle(Color(0xFF2B1B12), 1.8f, Offset(ex + 0.4f, eyeY))
        drawLine(l.hair, Offset(ex - 3.8f, eyeY - 4.6f), Offset(ex + 3.4f, eyeY - 5f), 1.4f)
        if (l.female) drawLine(ink, Offset(ex - 3.4f, eyeY - 2.2f), Offset(ex + 3.6f, eyeY - 2.4f), 1f)
    }
    drawOval(skinD, Offset(30.2f, 28f), Size(3.6f, 5f))
    if (l.beard && !l.female) {
        drawArc(l.hair, 0f, 180f, true, Offset(20f, 22f), Size(24f, 20f))
        drawArc(skin, 20f, 140f, true, Offset(27f, 31f), Size(10f, 5f))
    }
    if (l.female) drawArc(Color(0xFFC62828), 10f, 160f, false, Offset(27f, 32f), Size(10f, 5f), style = Stroke(2.2f))
    else drawArc(Color(0xFF5D2E22), 15f, 150f, false, Offset(27f, 32f), Size(10f, 5f), style = Stroke(1.5f))
    if (l.female) { drawCircle(Color(0xFFFFD54F), 1.5f, Offset(17f, 31f)); drawCircle(Color(0xFFFFD54F), 1.5f, Offset(47f, 31f)) }
    // acessórios por tipo
    when (l.type) {
        "Fazendeiro" -> {
            drawOval(Color(0xFFD7B56D), Offset(8f, 8f), Size(48f, 9f))
            drawArc(Color(0xFFE6C87F), 180f, 180f, true, Offset(18f, 0f), Size(28f, 18f))
            drawRect(Color(0xFF6D4C41), Offset(19f, 9f), Size(26f, 2.5f))
        }
        "Tunador" -> {
            drawArc(Color(0xFFE53935), 180f, 180f, true, Offset(17f, 2f), Size(30f, 22f))
            drawRect(Color(0xFFB71C1C), Offset(17f, 12.5f), Size(30f, 2.5f))
            drawRoundRect(Color(0xFF111111), Offset(13f, 20f), Size(6f, 11f), CornerRadius(3f))
            drawRoundRect(Color(0xFF111111), Offset(45f, 20f), Size(6f, 11f), CornerRadius(3f))
        }
        "Colecionador" -> {
            drawCircle(Color(0xFF455A64), 5f, Offset(25f, eyeY), style = Stroke(1.2f))
            drawCircle(Color(0xFF455A64), 5f, Offset(39f, eyeY), style = Stroke(1.2f))
            drawLine(Color(0xFF455A64), Offset(30f, eyeY), Offset(34f, eyeY), 1.2f)
        }
        "Fã de esportivos" -> {
            drawRoundRect(Color(0xFF111111), Offset(19f, eyeY - 4f), Size(12f, 7f), CornerRadius(3f))
            drawRoundRect(Color(0xFF111111), Offset(33f, eyeY - 4f), Size(12f, 7f), CornerRadius(3f))
            drawLine(Color(0xFF111111), Offset(30f, eyeY - 2f), Offset(34f, eyeY - 2f), 1.4f)
            drawLine(Color(0x66FFFFFF), Offset(21f, eyeY - 3f), Offset(25f, eyeY - 3f), 0.9f)
        }
        "Revendedor" -> {
            drawRoundRect(Color(0xFF263238), Offset(16f, 4f), Size(32f, 7f), CornerRadius(3f))
            drawRect(Color(0xFF263238), Offset(30f, 9f), Size(22f, 2.5f))
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
    val look = BuyerLook(female, SKINS.random(r), HAIRS.random(r), SHIRTS.random(r), PANTS.random(r), o.buyer, r.nextInt(3) == 0 || o.buyer == "Pai de família")
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
                            Text("💬 " + h.line, Modifier.padding(10.dp), fontSize = 14.sp)
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
