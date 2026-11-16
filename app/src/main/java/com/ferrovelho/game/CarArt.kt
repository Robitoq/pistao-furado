package com.ferrovelho.game

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Espaço de desenho: 200 x 80. Frente do carro à direita, chão em y = 72.
 * Formas inspiradas nos carros reais (não cópias exatas). Para ajustar uma carroceria,
 * mexa nas strings SVG abaixo (body = silhueta, glass = vidros, doors = portas).
 */
private class Def(
    val body: String, val glass: List<String>, val doors: String,
    val wx: Float, val wx2: Float, val r: Float,
    val lampY: Float, val hoodX: Float, val hoodY: Float,
    val spX: Float, val spY: Float,
    val bed: String? = null, val chrome: Boolean = false,
)

private class Shape(val d: Def, val body: Path, val glass: List<Path>, val doors: Path, val bed: Path?)

private fun p(s: String): Path = PathParser().parsePathString(s).toPath()

private fun defOf(b: Body): Def = when (b) {
    Body.HATCH -> Def(
        "M6,62 L6,40 Q6,36 12,34 L30,28 C34,25 38,24 44,24 L110,24 C126,24 138,36 150,43 L178,47 Q195,49 196,56 L196,62 Q196,64 192,64 L10,64 Q6,64 6,62 Z",
        listOf("M16,36 L32,29 Q36,27 42,27 L61,27 L61,42 L14,42 Z", "M66,27 L110,27 C124,27 134,37 143,42 L66,42 Z"),
        "M63,42 L62,63 M108,42 L110,63", 42f, 148f, 11f, 53f, 168f, 46f, 12f, 35f,
    )
    Body.SEDAN -> Def(
        "M6,62 L6,50 Q6,45 14,44 L50,43 C58,30 68,25 82,24 L116,24 C130,24 138,33 148,43 L178,47 Q195,49 196,56 L196,62 Q196,64 192,64 L10,64 Q6,64 6,62 Z",
        listOf("M55,42 C62,31 70,27 78,26.5 L78,42 Z", "M83,26.5 L115,26.5 C127,26.5 134,33 142,42 L83,42 Z"),
        "M80,42 L79,63 M118,42 L120,63", 46f, 150f, 11f, 53f, 166f, 46f, 14f, 44f,
    )
    Body.SUV -> Def(
        "M6,62 L6,36 Q6,30 12,29 L24,26 L136,26 C146,26 150,32 158,40 L180,45 Q195,47 196,55 L196,62 Q196,64 192,64 L10,64 Q6,64 6,62 Z",
        listOf("M13,32 L23,28.5 L51,28.5 L51,42 L13,42 Z", "M56,28.5 L95,28.5 L95,42 L56,42 Z", "M100,28.5 L134,28.5 C141,28.5 146,33 152,42 L100,42 Z"),
        "M53.5,42 L53.5,63 M97.5,42 L97.5,63", 44f, 152f, 13f, 52f, 170f, 44f, 16f, 28f,
    )
    Body.PICKUP -> Def(
        "M6,62 L6,44 L92,44 L92,40 L98,40 C102,28 108,25 118,25 L138,25 C148,25 154,36 160,42 L180,46 Q195,48 196,56 L196,62 Q196,64 192,64 L10,64 Q6,64 6,62 Z",
        listOf("M101,40 C104,29 108,27 116,27 L122,27 L122,40 Z", "M126,27 L137,27 C145,27 150,34 154,40 L126,40 Z"),
        "M124,40 L123,63", 42f, 152f, 13f, 53f, 170f, 45f, 8f, 44f, "M10,44 L90,44 L90,47.5 L10,47.5 Z",
    )
    Body.COUPE -> Def(
        "M6,62 L6,52 Q6,48 14,47 L44,46 C60,32 74,29 90,29 L114,29 C128,29 140,38 152,46 L182,50 Q196,52 196,58 L196,62 Q196,64 192,64 L10,64 Q6,64 6,62 Z",
        listOf("M56,45 C66,35 76,31 88,31 L108,31 C120,31 130,38 142,45 Z"),
        "M100,45 L102,63", 44f, 150f, 11f, 56f, 168f, 49f, 14f, 47f,
    )
    Body.CLASSIC -> Def(
        "M8,62 L8,50 Q8,46 16,45 L46,45 C54,34 62,28 76,27 L112,27 C126,27 134,36 144,45 L180,49 Q194,51 195,57 L195,62 Q195,64 191,64 L12,64 Q8,64 8,62 Z",
        listOf("M50,44 C57,34 64,30 74,29 L77,29 L77,44 Z", "M82,29 L110,29 C122,29 130,36 138,44 L82,44 Z"),
        "M79,44 L79,63 M114,44 L116,63", 46f, 148f, 11f, 54f, 164f, 48f, 16f, 45f, chrome = true,
    )
    Body.VAN -> Def(
        "M6,62 L6,30 Q6,26 12,26 L150,26 Q160,26 164,34 L172,50 L190,52 Q196,54 196,58 L196,62 Q196,64 192,64 L10,64 Q6,64 6,62 Z",
        listOf("M12,30 L50,30 L50,44 L12,44 Z", "M54,30 L96,30 L96,44 L54,44 Z", "M100,30 L146,30 Q152,30 155,36 L160,44 L100,44 Z"),
        "M52,30 L52,63 M98,30 L98,63", 40f, 152f, 12f, 55f, 178f, 51f, 12f, 26f, chrome = true,
    )
    Body.BEETLE -> Def(
        "M8,62 L8,52 C8,46 14,44 24,43 C36,28 52,24 74,24 C96,24 112,30 124,42 L158,46 C176,48 190,50 194,56 L194,62 Q194,64 190,64 L12,64 Q8,64 8,62 Z",
        listOf("M40,42 C48,31 58,27 72,27 L74,27 L74,42 Z", "M78,27 C94,27 106,32 116,42 L78,42 Z"),
        "M76,42 L76,63", 42f, 150f, 11f, 54f, 150f, 45f, 18f, 44f, chrome = true,
    )
    Body.SUPER -> Def(
        "M6,62 L6,54 Q6,50 14,49 L56,47 C72,36 90,32 110,32 C126,32 136,40 146,46 L186,52 Q196,54 196,60 L196,62 Q196,64 192,64 L10,64 Q6,64 6,62 Z",
        listOf("M70,46 C82,37 94,34 108,34 C120,34 128,39 138,46 Z"),
        "M104,46 L104,63", 46f, 150f, 10f, 57f, 168f, 51f, 14f, 49f,
    )
}

private fun shapeOf(b: Body): Shape {
    val d = defOf(b)
    return Shape(d, p(d.body), d.glass.map { p(it) }, p(d.doors), d.bed?.let { p(it) })
}

private val DENTS = listOf(62f to 52f, 120f to 50f, 90f to 56f, 150f to 53f, 30f to 49f, 172f to 54f)
private val RUST = listOf(
    Triple(20f, 60f, 3f), Triple(68f, 61f, 2.5f), Triple(118f, 62f, 3.2f),
    Triple(160f, 58f, 2.6f), Triple(95f, 52f, 2f), Triple(40f, 52f, 2.2f), Triple(140f, 48f, 2f),
)

/**
 * Carro visto de lado. Mostra pintura, tuning (aerofólio, para-choque, rodas, rebaixada...)
 * e o estado de cada peça: amassados, ferrugem, tinta descascada, vidro trincado,
 * farol quebrado, para-choque solto, pneu careca/murcho, fumaça do motor, banco rasgado
 * e suspensão arriada. Consertou -> o dano some sozinho (cond sobe).
 */
@Composable
fun CarArt(
    body: Body,
    colorIdx: Int,
    tuning: List<String>,
    cond: List<Int>,
    modifier: Modifier = Modifier,
) {
    val sh = remember(body) { shapeOf(body) }
    val anim = rememberInfiniteTransition(label = "smoke")
    val t by anim.animateFloat(
        0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart), label = "t",
    )
    val paint = Color(PAINTS[colorIdx.coerceIn(0, PAINTS.lastIndex)].second)
    Canvas(modifier) {
        val s = min(size.width / 200f, size.height / 80f)
        val ox = (size.width - 200f * s) / 2f
        val oy = (size.height - 80f * s) / 2f
        withTransform({ translate(ox, oy); scale(s, s, Offset.Zero) }) {
            drawCar(sh, paint, tuning, cond, t)
        }
    }
}

private fun DrawScope.drawCar(sh: Shape, paintC: Color, tuning: List<String>, cond: List<Int>, t: Float) {
    val d = sh.d
    val motor = cond.getOrElse(0) { 100 }
    val lat = cond.getOrElse(1) { 100 }
    val pn = cond.getOrElse(2) { 100 }
    val inter = cond.getOrElse(3) { 100 }
    val susp = cond.getOrElse(4) { 100 }

    val sag = if (susp < 60) 1f - susp / 60f else 0f
    val dy = (if ("rebaixada" in tuning) 3f else 0f) + sag * 3.5f
    val light = lerp(paintC, Color.White, 0.35f)
    val dark = lerp(paintC, Color.Black, 0.35f)
    val dark2 = lerp(paintC, Color.Black, 0.6f)
    val groundCy = 72f - d.r

    // sombra e neon
    drawOval(Color(0x55000000), Offset(12f, 69f), Size(184f, 6f))
    if ("neon" in tuning) {
        for (i in 0..5) drawRect(Color(0xFF00E5FF).copy(alpha = 0.10f + 0.05f * i), Offset(18f, 68f + i * 0.9f), Size(164f, 1.4f))
    }

    withTransform({ translate(0f, dy); rotate(sag * 2f, Offset(100f, 62f)) }) {
        // escapamento
        val esc = "escape" in tuning
        drawRect(if (esc) Color(0xFFCFD8DC) else Color(0xFF546E7A), Offset(0f, 60f), Size(if (esc) 9f else 5f, if (esc) 3.4f else 2f))

        // carroceria
        drawPath(sh.body, Brush.verticalGradient(listOf(light, paintC, dark), 22f, 64f))
        drawPath(sh.body, Color(0x66000000), style = Stroke(0.8f))
        sh.bed?.let { drawPath(it, Color(0xFF263238)) }

        // vidros + bancos
        sh.glass.forEachIndexed { i, g ->
            val b = g.getBounds()
            if (b.width > 9f) {
                val sc = when {
                    "couro" in tuning -> Color(0xFF9C6B3F)
                    inter < 40 -> Color(0xFF3E3A39)
                    else -> Color(0xFF546E7A)
                }
                val sx = b.left + b.width * 0.35f
                drawRoundRect(sc, Offset(sx, b.top + b.height * 0.3f), Size(7f, b.height * 0.7f + 1f), CornerRadius(2.5f))
                drawCircle(sc, 2.6f, Offset(sx + 3.5f, b.top + b.height * 0.28f))
                if (inter < 40) { // banco rasgado: espuma aparecendo
                    drawRect(Color(0xFFE6C875), Offset(sx + 1.5f, b.top + b.height * 0.5f), Size(3f, 2.5f))
                    drawLine(Color(0xFF111111), Offset(sx, b.top + b.height * 0.65f), Offset(sx + 7f, b.top + b.height * 0.55f), 0.8f)
                }
                if (i == 0 && "som" in tuning) {
                    drawRect(Color(0xFF111111), Offset(b.left + 1.5f, b.bottom - 6f), Size(7f, 6f))
                    drawCircle(Color(0xFFB0BEC5), 2f, Offset(b.left + 5f, b.bottom - 3f), style = Stroke(0.7f))
                }
            }
            if ("pelicula" in tuning) drawPath(g, Color(0xE60E1A22))
            else drawPath(g, Brush.verticalGradient(listOf(Color(0xB0CBE9F8), Color(0xB07FB2D6)), b.top, b.bottom))
        }
        // vidro trincado
        if (lat < 40) {
            val g = sh.glass.last()
            val b = g.getBounds()
            clipPath(g) {
                val c = Offset(b.left + b.width * 0.55f, b.top + b.height * 0.5f)
                for (k in 0 until 7) {
                    val a = k * PI.toFloat() / 3.5f + 0.4f
                    drawLine(Color(0xCCFFFFFF), c, Offset(c.x + cos(a) * 14f, c.y + sin(a) * 14f), 0.6f)
                }
                drawCircle(Color(0x99FFFFFF), 4f, c, style = Stroke(0.5f))
            }
        }
        drawPath(sh.doors, Color(0x88000000), style = Stroke(0.7f))

        clipPath(sh.body) {
            drawRect(Color(0x22000000), Offset(0f, 59f), Size(200f, 6f))
            if ("adesivo" in tuning) {
                drawRect(Color(0xCCFFFFFF), Offset(0f, 54f), Size(200f, 2.4f))
                drawRect(Color(0xCCFFFFFF), Offset(0f, 58f), Size(200f, 1.2f))
            }
            // amassados
            if (lat < 70) {
                val n = ((70 - lat) / 10 + 1).coerceAtMost(DENTS.size)
                for (i in 0 until n) {
                    val (x, y) = DENTS[i]
                    drawOval(Color(0x44000000), Offset(x - 5f, y - 2.5f), Size(10f, 5f))
                    drawOval(Color(0x44FFFFFF), Offset(x - 4f, y - 3.4f), Size(8f, 3f))
                }
            }
            // tinta descascada (primer)
            if (lat < 35) {
                drawRect(Color(0xFF9E9E9E), Offset(100f, 44f), Size(16f, 7f))
                drawRect(Color(0xFF8D8D8D), Offset(26f, 51f), Size(12f, 6f))
                if (lat < 20) drawRect(Color(0xFFA5A5A5), Offset(130f, 52f), Size(14f, 6f))
            }
            // ferrugem
            if (lat < 55) {
                val n = ((55 - lat) / 7 + 1).coerceAtMost(RUST.size)
                for (i in 0 until n) {
                    val (x, y, r) = RUST[i]
                    drawCircle(Color(0xFF5D2E0C), r * 1.3f, Offset(x, y))
                    drawCircle(Color(0xFF8D4A1B), r, Offset(x + 0.6f, y - 0.4f))
                    drawCircle(Color(0xFFB5651D), r * 0.5f, Offset(x - r * 0.7f, y + 0.8f))
                }
            }
            // frente batida
            if (lat < 30) {
                val j = Path().apply {
                    moveTo(186f, 46f); lineTo(190f, 51f); lineTo(185f, 54f); lineTo(190f, 58f)
                    lineTo(186f, 62f); lineTo(200f, 64f); lineTo(200f, 46f); close()
                }
                drawPath(j, Color(0x99000000))
            }
        }

        // para-choques
        val bump = if (d.chrome) Color(0xFFCFD8DC) else Color(0xFF37474F)
        drawRoundRect(bump, Offset(4f, 56f), Size(10f, 7f), CornerRadius(2f))
        if (lat < 20) { // para-choque dianteiro solto, arrastando
            withTransform({ rotate(22f, Offset(197f, 63f)) }) {
                drawRoundRect(bump, Offset(186f, 57f), Size(11f, 6f), CornerRadius(2f))
            }
        } else {
            drawRoundRect(bump, Offset(186f, 56f), Size(11f, 7f), CornerRadius(2f))
        }
        if ("parachoque" in tuning) {
            val kit = Color(0xFF111111)
            if (lat >= 20) {
                drawRoundRect(kit, Offset(187f, 58f), Size(9f, 4.5f), CornerRadius(1f))
                drawPath(p("M184,64 L198,64 L198,61 Q191,60 184,62 Z"), dark2)
            }
            drawRect(dark2, Offset(52f, 62.4f), Size(92f, 2f))
            drawPath(p("M3,64 L16,64 L14,61 L3,61 Z"), kit)
        }

        // faróis e lanternas
        val hx = 191f
        if (lat < 30) {
            drawCircle(Color(0xFF263238), 3.4f, Offset(hx, d.lampY))
            drawLine(Color(0xCCFFFFFF), Offset(hx - 2f, d.lampY - 2f), Offset(hx + 2f, d.lampY + 2f), 0.5f)
        } else {
            drawCircle(Color(0x55FFF59D), 5.5f, Offset(hx, d.lampY))
            drawCircle(Color(0xFFFFF59D), 3.4f, Offset(hx, d.lampY))
        }
        drawRoundRect(Color(0xFFE53935), Offset(6f, d.lampY - 3f), Size(4f, 5f), CornerRadius(1f))

        // entrada de ar do turbo
        if ("turbo" in tuning) {
            val sc = Path().apply {
                moveTo(d.hoodX - 22f, d.hoodY); lineTo(d.hoodX - 17f, d.hoodY - 3.6f)
                lineTo(d.hoodX - 5f, d.hoodY - 3.6f); lineTo(d.hoodX, d.hoodY); close()
            }
            drawPath(sc, Color(0xDD000000))
            drawLine(Color(0x66FFFFFF), Offset(d.hoodX - 15f, d.hoodY - 2f), Offset(d.hoodX - 7f, d.hoodY - 2f), 0.6f)
        }
    }

    // aerofólio (acompanha a carroceria)
    if ("aerofolio" in tuning) {
        withTransform({ translate(0f, dy); rotate(sag * 2f, Offset(100f, 62f)) }) {
            val ax = d.spX
            val ay = d.spY
            drawRect(Color(0xFF263238), Offset(ax + 1f, ay - 6f), Size(1.8f, 6f))
            drawRect(Color(0xFF263238), Offset(ax + 11f, ay - 6f), Size(1.8f, 6f))
            drawRoundRect(dark2, Offset(ax - 4f, ay - 9f), Size(23f, 2.8f), CornerRadius(1f))
            drawRect(Color(0xFF111111), Offset(ax - 4f, ay - 10.5f), Size(1.4f, 5.5f))
            drawRect(Color(0xFF111111), Offset(ax + 17.6f, ay - 10.5f), Size(1.4f, 5.5f))
        }
    }

    // fumaça do motor
    if (motor < 40) {
        val n = if (motor < 15) 5 else 3
        val sc = if (motor < 20) Color(0xFF212121) else Color(0xFFB0BEC5)
        for (i in 0 until n) {
            val ph = (t + i / n.toFloat()) % 1f
            drawCircle(sc.copy(alpha = (1f - ph) * 0.6f), 2.5f + ph * 7f, Offset(d.hoodX - ph * 14f, d.hoodY + dy - 2f - ph * 26f))
        }
    }

    // rodas
    val flat = pn < 30
    val worn = pn < 55
    val gold = "rodas" in tuning
    val rimC = if (gold) Color(0xFFFFC107) else Color(0xFFB0BEC5)
    for (wx in listOf(d.wx, d.wx2)) {
        val r = d.r
        drawArc(Color(0xFF0D0D0D), 180f, 180f, true, Offset(wx - r - 3f, groundCy - r - 3f), Size((r + 3f) * 2, (r + 3f) * 2))
        val rr = if (flat) r * 0.86f else r
        val cy = 72f - rr
        drawCircle(Color(0xFF141414), rr, Offset(wx, cy))
        if (worn) drawCircle(Color(0xFF4A4A4A), rr * 0.9f, Offset(wx, cy), style = Stroke(1.1f))
        drawCircle(rimC, rr * 0.62f, Offset(wx, cy))
        if (gold) {
            for (k in 0 until 5) {
                val a = k * 2f * PI.toFloat() / 5f
                drawLine(Color(0xFF8A6A00), Offset(wx, cy), Offset(wx + cos(a) * rr * 0.6f, cy + sin(a) * rr * 0.6f), 1f)
            }
        } else {
            for (k in 0 until 4) {
                val a = k * PI.toFloat() / 2f + 0.4f
                drawCircle(Color(0xFF78909C), 0.8f, Offset(wx + cos(a) * rr * 0.36f, cy + sin(a) * rr * 0.36f))
            }
        }
        drawCircle(Color(0xFF37474F), rr * 0.18f, Offset(wx, cy))
    }
}
