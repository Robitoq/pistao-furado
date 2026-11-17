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
    Body.BOXY -> Def(
        "M6,62 L6,30 Q6,26 12,25 L112,23 C120,23 124,26 130,33 L140,43 L178,46 Q195,48 196,56 L196,62 Q196,64 192,64 L10,64 Q6,64 6,62 Z",
        listOf("M12,30 L58,28 L58,42 L11,42 Z", "M63,28 L112,26.5 C118,26.5 122,29 126,34 L133,42 L63,42 Z"),
        "M60,42 L60,63 M110,42 L112,63", 42f, 150f, 10.5f, 53f, 166f, 45f, 12f, 28f,
    )
    Body.COMPACT -> Def(
        "M6,62 L6,44 Q6,40 12,38 L28,32 C36,26 44,24 56,24 L100,24 C114,24 126,30 138,40 L152,44 L180,47 Q195,49 196,56 L196,62 Q196,64 192,64 L10,64 Q6,64 6,62 Z",
        listOf("M18,38 L34,30 C38,27 44,26.5 52,26.5 L66,26.5 L66,42 L16,42 Z", "M70,26.5 L100,26.5 C112,26.5 122,31 132,40 L134,42 L70,42 Z"),
        "M68,42 L67,63 M112,42 L114,63", 40f, 146f, 10.5f, 53f, 166f, 45f, 10f, 34f,
    )
    Body.WAGON -> Def(
        "M6,62 L6,36 Q6,32 12,31 L24,26 L112,25 C124,25 134,34 146,42 L178,46 Q195,48 196,56 L196,62 Q196,64 192,64 L10,64 Q6,64 6,62 Z",
        listOf("M14,34 L24,28 L50,28 L50,42 L12,42 Z", "M55,28 L96,28 L96,42 L55,42 Z", "M100,28 L112,27.5 C122,27.5 130,34 140,42 L100,42 Z"),
        "M52.5,42 L52.5,63 M98,42 L99,63", 40f, 148f, 11f, 53f, 168f, 46f, 12f, 32f,
    )
    Body.MPV -> Def(
        "M6,62 L6,34 Q6,28 14,27 L30,24 L108,22 C120,22 128,26 134,32 L146,44 L176,47 Q195,49 196,56 L196,62 Q196,64 192,64 L10,64 Q6,64 6,62 Z",
        listOf("M14,32 L28,27 L50,26.5 L50,42 L13,42 Z", "M55,26.5 L92,26 L92,42 L55,42 Z", "M97,26 L108,25.5 C118,25.5 124,29 130,34 L138,42 L97,42 Z"),
        "M52.5,42 L52.5,63 M94.5,42 L94.5,63", 42f, 148f, 11f, 53f, 166f, 46f, 12f, 28f,
    )
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
    Body.RETRO, Body.BOXSEDAN -> Def(
        "M6,62 L6,46 Q6,43 11,43 L50,42 L64,28 Q66,26 70,26 L114,26 Q118,26 120,28 L142,43 L180,47 Q195,49 196,56 L196,62 Q196,64 192,64 L10,64 Q6,64 6,62 Z",
        listOf("M54,41 L66,29 L78,29 L78,41 Z", "M83,29 L114,29 L118,29.5 L135,41 L83,41 Z"),
        "M80,41 L79,63 M120,41 L122,63", 44f, 150f, 11f, 54f, 166f, 48f, 14f, 43f, chrome = b == Body.RETRO,
    )
    Body.CREW -> Def(
        "M6,62 L6,40 L70,40 L70,34 L76,34 L80,26 L136,26 C146,26 152,32 158,40 L182,44 Q195,46 196,56 L196,62 Q196,64 192,64 L10,64 Q6,64 6,62 Z",
        listOf("M83,29 L104,29 L104,38.5 L83,38.5 Z", "M108,29 L134,29 C142,29 147,33 152,38.5 L108,38.5 Z"),
        "M106,39 L105,63 M81,39 L80,63", 38f, 150f, 13f, 52f, 170f, 44f, 10f, 40f, "M8,40 L68,40 L68,43.5 L8,43.5 Z",
    )
    Body.JEEP -> Def(
        "M6,62 L6,34 Q6,30 12,30 L40,30 L42,26 L124,26 L130,38 L134,40 L184,40 Q196,42 196,54 L196,62 Q196,64 192,64 L10,64 Q6,64 6,62 Z",
        listOf("M46,29 L76,29 L76,39 L46,39 Z", "M80,29 L116,29 L122,39 L80,39 Z"),
        "M78,39 L78,63", 44f, 150f, 13f, 50f, 172f, 44f, 10f, 30f,
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
        val neon = Color(0xFF18E8FF)
        for (i in 0..5) drawOval(neon.copy(alpha = 0.06f + i * 0.04f), Offset(12f + i * 5f, 66.5f + i * 0.4f), Size(176f - i * 10f, 9f - i * 1.2f))
        drawRoundRect(neon.copy(alpha = 0.55f), Offset(30f, 63.1f), Size(140f, 2f), CornerRadius(1f))
        drawRoundRect(Color(0xFFE6FDFF), Offset(34f, 63.6f), Size(132f, 0.8f), CornerRadius(0.4f))
    }

    withTransform({ translate(0f, dy); rotate(sag * 2f, Offset(100f, 62f)) }) {
        // escapamento
        val esc = "escape" in tuning
        if (esc) {
            val inox = Brush.verticalGradient(listOf(Color(0xFFF1F4F5), Color(0xFF8A9BA3), Color(0xFF36454C)), 63.5f, 68f)
            drawRoundRect(Color(0xFF55646B), Offset(38f, 64f), Size(22f, 2f), CornerRadius(1f))          // tubo
            drawRoundRect(inox, Offset(10f, 63.5f), Size(30f, 4.6f), CornerRadius(2.3f))                 // abafador
            drawLine(Color(0x55000000), Offset(18f, 63.8f), Offset(18f, 67.8f), 0.5f)
            drawLine(Color(0x55000000), Offset(32f, 63.8f), Offset(32f, 67.8f), 0.5f)
            drawRoundRect(Color(0xFF6D7C83), Offset(3f, 64.6f), Size(8f, 2f), CornerRadius(1f))
            drawOval(Color(0xFFDDE3E6), Offset(0.6f, 62.6f), Size(6.2f, 5.6f))                           // ponteira
            drawOval(Color(0xFF3F5FA8), Offset(0.6f, 62.6f), Size(6.2f, 5.6f), style = Stroke(0.7f))     // titânio azulado
            drawOval(Color(0xFF0B0D0E), Offset(1.9f, 63.8f), Size(3.4f, 3.2f))
        } else drawRect(Color(0xFF546E7A), Offset(0f, 60f), Size(5f, 2f))

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
            if ("pelicula" in tuning) {
                drawPath(g, Color(0xE60E1A22))
                clipPath(g) { val gb = g.getBounds(); drawLine(Color(0x22FFFFFF), Offset(gb.left + 6f, gb.bottom), Offset(gb.left + 16f, gb.top), 3f) }
            }
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
        sh.glass.dropLast(1).forEach { g -> val gb = g.getBounds(); drawRect(Color(0xFF0E1216), Offset(gb.right, gb.top), Size(2.2f, gb.height)) }

        // realismo: moldura dos vidros, reflexo, maçanetas e retrovisor
        sh.glass.forEach { g ->
            val b = g.getBounds()
            drawPath(g, Color(0xFF0E1216), style = Stroke(1.4f))
            if (b.width > 9f) {
                clipPath(g) {
                    val q = Path().apply {
                        moveTo(b.left, b.bottom); lineTo(b.left + b.width * 0.35f, b.top)
                        lineTo(b.left + b.width * 0.5f, b.top); lineTo(b.left + b.width * 0.15f, b.bottom); close()
                    }
                    drawPath(q, Color(0x33FFFFFF))
                }
                drawRoundRect(dark2, Offset(b.right - 11f, b.bottom + 3f), Size(5f, 1.6f), CornerRadius(0.8f))
                drawRoundRect(Color(0x88FFFFFF), Offset(b.right - 11f, b.bottom + 3f), Size(5f, 0.6f), CornerRadius(0.3f))
            }
        }
        sh.glass.lastOrNull()?.getBounds()?.let { b ->
            drawRoundRect(dark, Offset(b.right - 2f, b.bottom - 3f), Size(5.5f, 3.4f), CornerRadius(1.4f))
        }

        clipPath(sh.body) {
            drawRect(Color(0x22000000), Offset(0f, 59f), Size(200f, 6f))
            drawRect(Color(0x26FFFFFF), Offset(0f, 44f), Size(200f, 3f))      // reflexo da lateral
            drawLine(Color(0x44FFFFFF), Offset(10f, 50f), Offset(184f, 50f), 0.6f) // vinco
            drawLine(Color(0x33000000), Offset(10f, 51f), Offset(184f, 51f), 0.6f)
            if ("adesivo" in tuning) {
                drawPath(Path().apply { moveTo(0f, 53.4f); lineTo(178f, 53.4f); lineTo(169f, 57.6f); lineTo(0f, 57.6f); close() }, Color(0xE8F5F5F5))
                drawPath(Path().apply { moveTo(0f, 58.6f); lineTo(165f, 58.6f); lineTo(162f, 59.9f); lineTo(0f, 59.9f); close() }, Color(0xFFE53935))
                drawRect(Color(0x30000000), Offset(0f, 57.1f), Size(170f, 0.5f))
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
            val kit = Color(0xFF15181B)
            val acc = Color(0xFFE53935)
            if (lat >= 20) {
                drawPath(p("M183,64.6 L198,64.6 L198,58 Q192,57 186,59.5 Z"), kit)
                drawRoundRect(Color(0xFF05070A), Offset(187f, 59.6f), Size(8f, 3.2f), CornerRadius(0.8f))
                for (i in 0..2) drawLine(Color(0xFF455A64), Offset(188.4f + i * 2.4f, 59.8f), Offset(188.4f + i * 2.4f, 62.6f), 0.5f)
                drawRect(acc, Offset(184f, 63.4f), Size(14f, 0.9f))
            }
            drawPath(p("M52,62.2 L146,62.2 L150,64.6 L48,64.6 Z"), kit)
            drawRect(acc, Offset(52f, 62.2f), Size(94f, 0.6f))
            drawPath(p("M3,64.6 L24,64.6 L20,60.4 L3,60.4 Z"), kit)
            for (i in 0..3) drawLine(Color(0xFF455A64), Offset(6.5f + i * 4f, 61f), Offset(5f + i * 4.2f, 64.2f), 0.5f)
        }

        // faróis e lanternas
        val hx = 191f
        val hl = Path().apply {
            moveTo(hx - 7f, d.lampY - 3f); lineTo(hx + 4f, d.lampY - 3.6f)
            lineTo(hx + 5f, d.lampY + 2.6f); lineTo(hx - 7f, d.lampY + 2.4f); close()
        }
        if (lat < 30) {
            drawPath(hl, Color(0xFF263238))
            drawLine(Color(0xCCFFFFFF), Offset(hx - 4f, d.lampY - 2f), Offset(hx + 2f, d.lampY + 2f), 0.5f)
        } else {
            drawCircle(Color(0x44FFF59D), 7f, Offset(hx, d.lampY))
            drawPath(hl, Color(0xFFECEFF1))
            drawCircle(Color(0xFFFFF59D), 2.4f, Offset(hx - 0.5f, d.lampY - 0.2f))
            drawPath(hl, Color(0xFF90A4AE), style = Stroke(0.6f))
        }
        // pisca, placa e detalhes de porta
        drawRect(Color(0xFFFFB300), Offset(hx - 6f, d.lampY + 2.8f), Size(3.5f, 1.6f))
        drawRoundRect(Color(0xFFF5F5F5), Offset(176f, 56.5f), Size(9f, 4.2f), CornerRadius(0.6f))
        drawRect(Color(0xFF1565C0), Offset(176f, 56.5f), Size(9f, 1f))
        drawRoundRect(Color(0xFFF5F5F5), Offset(10f, 57f), Size(8f, 3.8f), CornerRadius(0.6f))
        for (dx in sh.doors.getBounds().let { listOf(it.left + 3f) }) {
            drawRoundRect(dark2, Offset(dx, 46.5f), Size(5.5f, 1.5f), CornerRadius(0.7f))
            drawRoundRect(Color(0x88FFFFFF), Offset(dx, 46.5f), Size(5.5f, 0.5f), CornerRadius(0.3f))
        }
        // soleira e caixa de roda (arco em volta de cada pneu)
        drawRect(dark2, Offset(14f, 62f), Size(172f, 2f))
        for (wx in listOf(d.wx, d.wx2)) {
            drawArc(Color(0x66000000), 180f, 180f, false, Offset(wx - d.r - 4f, 72f - d.r * 2f - 4f + 0.5f), Size((d.r + 4f) * 2, (d.r + 4f) * 2), style = Stroke(1.4f))
        }
        drawRoundRect(Color(0xFFE53935), Offset(6f, d.lampY - 3f), Size(4f, 5f), CornerRadius(1f))
        drawRoundRect(Color(0xFF1B1B1B), Offset(182f, d.lampY + 4.6f), Size(12f, 4f), CornerRadius(1f)) // grade
        for (gy in 0..2) drawLine(Color(0xFF78909C), Offset(183f, d.lampY + 5.5f + gy * 1.1f), Offset(193f, d.lampY + 5.5f + gy * 1.1f), 0.4f)
        drawRoundRect(Color(0xFFFF8A65), Offset(6.5f, d.lampY - 2f), Size(1.5f, 3f), CornerRadius(0.5f)) // brilho lanterna

        // entrada de ar do turbo
        if ("turbo" in tuning) {
            val sc = Path().apply {
                moveTo(d.hoodX - 25f, d.hoodY + 0.6f); lineTo(d.hoodX - 20f, d.hoodY - 3.8f)
                lineTo(d.hoodX - 6f, d.hoodY - 3.8f); lineTo(d.hoodX - 2f, d.hoodY + 0.6f); close()
            }
            drawPath(sc, Color(0xFF14181B))
            drawPath(sc, Color(0x55FFFFFF), style = Stroke(0.5f))
            for (i in 0..3) drawLine(Color(0xFF3B4A53), Offset(d.hoodX - 19f + i * 3.8f, d.hoodY - 3.2f), Offset(d.hoodX - 20.5f + i * 3.8f, d.hoodY + 0.2f), 0.7f)
        }
    }

    // aerofólio (acompanha a carroceria)
    if ("aerofolio" in tuning) {
        withTransform({ translate(0f, dy); rotate(sag * 2f, Offset(100f, 62f)) }) {
            val ax = d.spX
            val ay = d.spY
            val carbon = Color(0xFF14181B)
            for (sx in listOf(ax + 2.2f, ax + 12.6f)) { // pescoço de cisne
                drawPath(Path().apply { moveTo(sx, ay + 1f); lineTo(sx + 1.2f, ay - 6.4f); lineTo(sx + 3f, ay - 6.4f); lineTo(sx + 2f, ay + 1f); close() }, carbon)
            }
            val wing = Path().apply {
                moveTo(ax - 5f, ay - 9.2f); lineTo(ax + 21f, ay - 11f)
                quadraticBezierTo(ax + 23.6f, ay - 10.6f, ax + 22.6f, ay - 8.6f)
                lineTo(ax - 3.4f, ay - 6.6f); quadraticBezierTo(ax - 6.2f, ay - 7.2f, ax - 5f, ay - 9.2f); close()
            }
            drawPath(wing, Brush.verticalGradient(listOf(Color(0xFF4A555C), Color(0xFF0E1113)), ay - 11f, ay - 6.6f))
            drawLine(Color(0x99FFFFFF), Offset(ax - 3.4f, ay - 8.9f), Offset(ax + 20f, ay - 10.6f), 0.5f)
            drawLine(Color(0xFFD32F2F), Offset(ax - 3f, ay - 7f), Offset(ax + 21.4f, ay - 8.9f), 0.7f)
            for (ex in listOf(ax - 5.4f, ax + 21.6f)) { // laterais
                drawPath(Path().apply { moveTo(ex, ay - 13f); lineTo(ex + 1.9f, ay - 13.2f); lineTo(ex + 1.9f, ay - 6f); lineTo(ex, ay - 6.4f); close() }, carbon)
                drawLine(Color(0xFFD32F2F), Offset(ex + 0.95f, ay - 12.4f), Offset(ex + 0.95f, ay - 7.4f), 0.5f)
            }
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
    val sport = "rodas" in tuning
    val rimC = if (sport) Color(0xFF2A2F33) else Color(0xFFB0BEC5)
    for (wx in listOf(d.wx, d.wx2)) {
        val r = d.r
        drawArc(Color(0xFF0D0D0D), 180f, 180f, true, Offset(wx - r - 3f, groundCy - r - 3f), Size((r + 3f) * 2, (r + 3f) * 2))
        val rr = if (flat) r * 0.86f else r
        val cy = 72f - rr
        drawCircle(Color(0xFF141414), rr, Offset(wx, cy))
        if (worn) drawCircle(Color(0xFF4A4A4A), rr * 0.9f, Offset(wx, cy), style = Stroke(1.1f))
        drawCircle(Color(0xFF262626), rr * 0.8f, Offset(wx, cy)) // lateral do pneu
        drawCircle(rimC, rr * 0.62f, Offset(wx, cy))
        drawCircle(Color(0x66FFFFFF), rr * 0.62f, Offset(wx, cy), style = Stroke(0.5f))
        if (sport) {
            drawCircle(Color(0xFFDDE3E6), rr * 0.64f, Offset(wx, cy))                                   // aro polido
            drawCircle(Color(0xFF1B1F22), rr * 0.57f, Offset(wx, cy))
            drawCircle(Color(0xFF9AA3A8), rr * 0.46f, Offset(wx, cy))                                   // disco de freio
            drawCircle(Color(0xFF454C50), rr * 0.46f, Offset(wx, cy), style = Stroke(0.5f))
            drawArc(Color(0xFFD32F2F), 200f, 72f, false, Offset(wx - rr * 0.5f, cy - rr * 0.5f), Size(rr, rr), style = Stroke(rr * 0.2f)) // pinça
            for (k in 0 until 5) {
                val a = k * 2f * PI.toFloat() / 5f + 0.2f
                for (o in listOf(-0.16f, 0.16f)) {
                    val a2 = a + o
                    drawLine(Color(0xFF2C3236), Offset(wx + cos(a) * rr * 0.1f, cy + sin(a) * rr * 0.1f), Offset(wx + cos(a2) * rr * 0.57f, cy + sin(a2) * rr * 0.57f), rr * 0.13f)
                    drawLine(Color(0xFF8E999F), Offset(wx + cos(a) * rr * 0.1f, cy + sin(a) * rr * 0.1f), Offset(wx + cos(a2) * rr * 0.57f, cy + sin(a2) * rr * 0.57f), 0.5f)
                }
            }
            drawCircle(Color(0xFFCFD8DC), rr * 0.2f, Offset(wx, cy))
            for (k in 0 until 5) {
                val a = k * 2f * PI.toFloat() / 5f
                drawCircle(Color(0xFF37474F), rr * 0.03f, Offset(wx + cos(a) * rr * 0.12f, cy + sin(a) * rr * 0.12f))
            }
        } else {
            drawCircle(Color(0xFF546E7A), rr * 0.48f, Offset(wx, cy))
            for (k in 0 until 5) {
                val a = k * 2f * PI.toFloat() / 5f + 0.3f
                drawLine(Color(0xFFECEFF1), Offset(wx, cy), Offset(wx + cos(a) * rr * 0.58f, cy + sin(a) * rr * 0.58f), 1.5f)
            }
        }
        drawCircle(Color(0xFF37474F), rr * 0.18f, Offset(wx, cy))
    }
}
