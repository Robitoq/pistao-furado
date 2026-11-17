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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Carro em pixel art. Mostra de verdade a condição (motor, lataria, pneus, interior, suspensão)
 * e o tuning instalado (aerofólio, rodas, neon, película, adesivo, escape, turbo, som, couro...).
 */
@Composable
fun CarArt(
    body: Body,
    colorIdx: Int,
    tuning: List<String>,
    cond: List<Int>,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val spr = remember { CitySprites.get(ctx) }
    val ci = carSpriteFor(body)
    val img = spr.cars[ci]
    val paint = Color(PAINTS[colorIdx.coerceIn(0, PAINTS.lastIndex)].second)
    val geo = remember(img) {
        try {
            val b = img.asAndroidBitmap()
            fun first(x: Int): Int {
                for (y in 0 until b.height) if (android.graphics.Color.alpha(b.getPixel(x.coerceIn(0, b.width - 1), y)) > 0) return y
                return b.height / 2
            }
            var top = b.height
            for (x in 0 until b.width step 2) top = min(top, first(x))
            intArrayOf(top, first((b.width * 0.07f).toInt()))
        } catch (e: Throwable) { intArrayOf(2, 4) }
    }
    val anim = rememberInfiniteTransition(label = "fx")
    val t by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart), label = "t")
    val k = 1.3f
    val m = ColorMatrix(floatArrayOf(
        paint.red * k * 0.299f, paint.red * k * 0.587f, paint.red * k * 0.114f, 0f, 0f,
        paint.green * k * 0.299f, paint.green * k * 0.587f, paint.green * k * 0.114f, 0f, 0f,
        paint.blue * k * 0.299f, paint.blue * k * 0.587f, paint.blue * k * 0.114f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    ))
    fun part(i: Int) = cond.getOrElse(i) { 100 }
    Canvas(modifier) {
        try {
            val w = img.width.toFloat()
            val h = img.height.toFloat()
            val s = min(size.width / (w + 10f), size.height / (h + 14f)).coerceAtLeast(1f)
            val ox = (size.width - w * s) / 2f
            val low = if ("rebaixada" in tuning) 1.3f else 0f
            val oy = (size.height - h * s) / 2f + low * s
            fun r(c: Color, x: Float, y: Float, ww: Float, hh: Float) =
                drawRect(c, Offset(ox + x * s, oy + y * s), Size(ww * s, hh * s))
            fun sprite(filter: ColorFilter?) = drawImage(
                img, IntOffset.Zero, IntSize(img.width, img.height),
                IntOffset(ox.toInt(), oy.toInt()), IntSize((w * s).toInt(), (h * s).toInt()),
                1f, Fill, filter, BlendMode.SrcOver, FilterQuality.None,
            )
            // faixa do sprite repintada (respeita o contorno do carro)
            fun band(x0: Float, y0: Float, x1: Float, y1: Float, c: Color) =
                clipRect(ox + x0 * s, oy + y0 * s, ox + x1 * s, oy + y1 * s) {
                    sprite(ColorFilter.tint(c, BlendMode.SrcAtop))
                }
            val top = geo[0].toFloat()
            val motor = part(0); val lat = part(1); val tire = part(2); val inter = part(3); val susp = part(4)

            // suspensão ruim: traseira arriada; pneu furado: inclina mais
            val flat = tire < 25
            val sag = (max(0, 65 - susp) / 65f) * 3.5f + (if (flat) 2.2f else 0f)

            // neon / sombra (fora da rotação)
            if ("neon" in tuning) {
                glow(Offset(ox + w / 2f * s, oy + (h - 0.5f) * s), w * 0.62f * s, Color(0xFF00E5FF).copy(alpha = 0.55f))
                r(Color(0xFF9CF3FF), 5f, h - 0.6f, w - 10f, 0.9f)
            }
            drawOval(Color(0x55000000), Offset(ox + 2f * s, oy + (h - 2f) * s), Size((w - 4f) * s, 4f * s))

            withTransform({ rotate(-sag, Offset(ox + w * 0.82f * s, oy + h * s)) }) {
                sprite(ColorFilter.colorMatrix(m))

                // ---- lataria: ferrugem, tinta descascada, amassados, vidro trincado
                if (lat < 75) {
                    val rnd = Random(ci * 97 + 13)
                    val n = ((75 - lat) / 5).coerceIn(1, 14)
                    repeat(n) { i ->
                        val px = rnd.nextFloat() * (w * 0.84f) + w * 0.06f
                        val py = rnd.nextFloat() * (h * 0.30f) + h * 0.36f
                        val pw = 2f + rnd.nextFloat() * 4f
                        val ph = 1.4f + rnd.nextFloat() * 2.4f
                        val c = when {
                            i % 3 == 0 && lat < 55 -> Color(0xFFB0B4B8)   // tinta saiu, massa cinza
                            i % 2 == 0 -> Color(0xFF8D4B1F)               // ferrugem
                            else -> Color(0xFF5E3410)
                        }
                        band(px, py, px + pw, py + ph, c)
                        if (i % 4 == 0 && lat < 60) band(px + 1f, py + ph, px + pw - 1f, py + ph + 1f, Color(0x88000000)) // amassado
                    }
                }
                if (lat < 30) {
                    val x0 = ox + w * 0.6f * s
                    val y0 = oy + (top + 1.2f) * s
                    for (d in listOf(Offset(4f, 4.5f), Offset(-1f, 5f), Offset(5f, 1.5f))) {
                        drawLine(Color(0xCCFFFFFF), Offset(x0, y0), Offset(x0 + d.x * s, y0 + d.y * s), 0.35f * s)
                    }
                }

                // ---- interior (banco aparece pela janela)
                val seatX = w * 0.46f
                if ("couro" in tuning) {
                    r(Color(0xFF6D4C41), seatX, top + 2.4f, 3.4f, 4.2f)
                    r(Color(0xFF8D6E63), seatX, top + 2.4f, 3.4f, 0.8f)
                } else if (inter < 45) {
                    r(Color(0xFF7A7A7A), seatX, top + 2.4f, 3.4f, 4.2f)
                    r(Color(0xFFF2E6C9), seatX + 0.6f, top + 3.2f, 1.6f, 1.4f)   // espuma saindo
                    r(Color(0xFF2B2B2B), seatX + 2.0f, top + 4.4f, 1.0f, 1.2f)   // rasgo
                }

                // ---- tuning no corpo
                if ("pelicula" in tuning) band(w * 0.18f, top + 1f, w * 0.80f, top + h * 0.32f, Color(0xB005080F))
                if ("adesivo" in tuning) {
                    band(0f, h * 0.40f, w, h * 0.40f + 1.6f, Color(0xFFE53935))
                    band(0f, h * 0.40f + 1.9f, w, h * 0.40f + 2.4f, Color(0xFFFFFFFF))
                }
                if ("parachoque" in tuning) {
                    r(Color(0xFF1B1B1B), w - 5.5f, h - 6.5f, 5.5f, 2.6f)
                    r(Color(0xFF424242), w - 6f, h - 4f, 6f, 0.8f)
                }
                if ("aerofolio" in tuning) {
                    val d = geo[1].toFloat()
                    val wl = w * 0.2f
                    r(Color(0xFF1B1B1B), w * 0.02f, d - 1.2f, 1.4f, 1.6f)
                    r(Color(0xFF1B1B1B), w * 0.02f + wl - 2f, d - 1.2f, 1.4f, 1.6f)
                    r(Color(0xFF111111), -1.5f, d - 4f, wl + 1.5f, 2.6f)
                    r(Color(0xFF616161), -1.5f, d - 4f, wl + 1.5f, 0.7f)
                    r(Color(0xFFE53935), -1.5f, d - 4.4f, 0.9f, 3.4f)
                    r(Color(0xFFE53935), wl - 1.1f, d - 4.4f, 0.9f, 3.4f)
                }
                if ("escape" in tuning || "turbo" in tuning) {
                    r(Color(0xFFB0BEC5), -1.6f, h - 4.6f, 3.2f, 1.5f)
                    r(Color(0xFF546E7A), -1.6f, h - 3.4f, 3.2f, 0.4f)
                }
                if ("turbo" in tuning) {
                    val fl = 1.5f + 3.5f * (0.5f + 0.5f * sin(t * 2f * PI.toFloat() * 3f))
                    r(Color(0xFF29B6F6), -1.6f - fl, h - 4.4f, fl, 1.1f)
                    r(Color(0xFFFFF59D), -1.6f - fl * 0.5f, h - 4.2f, fl * 0.5f, 0.7f)
                    glow(Offset(ox - 3f * s, oy + (h - 3.9f) * s), 5f * s, Color(0xFF29B6F6).copy(alpha = 0.5f))
                }

                // ---- pneus: gastos/furados por cima; rodas esportivas
                val sport = "rodas" in tuning
                if (sport || tire < 55) {
                    val wh = CAR_WHEELS[ci.coerceIn(0, CAR_WHEELS.lastIndex)]
                    listOf(wh.first, wh.second).forEachIndexed { idx, cx ->
                        val rad = wh.third
                        val cy = h - 5.5f
                        val rear = idx == 0
                        val isFlat = flat && rear
                        val c = Offset(ox + cx * s, oy + cy * s)
                        drawCircle(if (tire < 55) Color(0xFF3F4246) else Color(0xFF15161A), rad * s, c)
                        val rimC = if (sport) Color(0xFFFFC107) else Color(0xFF8E9399)
                        val rc = if (isFlat) Offset(c.x, c.y + rad * 0.32f * s) else c
                        val rr = rad * (if (isFlat) 0.52f else if (sport) 0.72f else 0.6f) * s
                        drawCircle(rimC, rr, rc)
                        if (sport) drawCircle(Color(0xFF3A2E00), rr * 0.74f, rc)
                        val n = if (sport) 6 else 5
                        for (j in 0 until n) {
                            val a = 0.4f + j * (2f * PI.toFloat() / n)
                            drawLine(if (sport) Color(0xFFFFE082) else Color(0xFF4A4F57), rc,
                                Offset(rc.x + kotlin.math.cos(a) * rr * 0.9f, rc.y + sin(a) * rr * 0.9f), rr * 0.16f)
                        }
                        drawCircle(if (sport) Color(0xFF5D4B00) else Color(0xFFD7DBE0), rr * 0.2f, rc)
                        if (tire < 55 && !isFlat) {
                            // careca: sem desenho de banda
                            drawCircle(Color(0x55FFFFFF), rad * s, c, style = Stroke(width = 0.5f * s))
                        }
                    }
                }

                // ---- motor: fumaça (e faíscas se estiver morrendo)
                if (motor < 45) {
                    val smokeX = ox + (w * 0.80f) * s
                    val smokeY = oy + (top + h * 0.26f) * s
                    val black = motor < 25
                    val puffs = if (black) 5 else 3
                    for (i in 0 until puffs) {
                        val p = (t + i / puffs.toFloat()) % 1f
                        val col = if (black) Color(0xFF1B1B1B) else Color(0xFFBDBDBD)
                        drawCircle(col.copy(alpha = 0.6f * (1f - p)), (1.6f + p * 3.4f) * s,
                            Offset(smokeX + sin((p + i) * 5f) * 1.5f * s, smokeY - p * 13f * s))
                    }
                    if (motor < 12) {
                        val on = ((t * 6f).toInt() % 2) == 0
                        if (on) {
                            r(Color(0xFFFFEB3B), w * 0.84f, top + h * 0.3f, 0.9f, 0.9f)
                            r(Color(0xFFFF9800), w * 0.88f, top + h * 0.22f, 0.9f, 0.9f)
                        }
                    }
                }

                // ---- som potente: ondas saindo do carro
                if ("som" in tuning) {
                    for (i in 0..2) {
                        val p = (t * 1.5f + i / 3f) % 1f
                        val rad = (3f + p * 7f) * s
                        drawArc(
                            Color.White.copy(alpha = 0.7f * (1f - p)), -50f, 100f, false,
                            Offset(ox + (w * 0.55f) * s - rad, oy + (top + 2f) * s - rad), Size(rad * 2f, rad * 2f),
                            style = Stroke(width = 0.5f * s),
                        )
                    }
                }
            }
        } catch (e: Throwable) { }
    }
}
