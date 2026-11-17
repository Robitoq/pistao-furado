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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.min
import kotlin.random.Random

/** Desenha o carro com os sprites pixelados (pacote 2DPixelCars), pintado na cor escolhida. */
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
    val driver = remember { loadProfile(ctx.getSharedPreferences("city", android.content.Context.MODE_PRIVATE))?.look }
    val paint = Color(PAINTS[colorIdx.coerceIn(0, PAINTS.lastIndex)].second)
    val geo = remember(img) {
        val b = img.asAndroidBitmap()
        fun first(x: Int): Int {
            for (y in 0 until b.height) if (android.graphics.Color.alpha(b.getPixel(x.coerceIn(0, b.width - 1), y)) > 0) return y
            return b.height / 2
        }
        var top = b.height
        for (x in 0 until b.width step 2) top = minOf(top, first(x))
        intArrayOf(top, first((b.width * 0.07f).toInt()))
    }
    val anim = rememberInfiniteTransition(label = "smoke")
    val t by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart), label = "t")
    val k = 1.3f
    val m = ColorMatrix(floatArrayOf(
        paint.red * k * 0.299f, paint.red * k * 0.587f, paint.red * k * 0.114f, 0f, 0f,
        paint.green * k * 0.299f, paint.green * k * 0.587f, paint.green * k * 0.114f, 0f, 0f,
        paint.blue * k * 0.299f, paint.blue * k * 0.587f, paint.blue * k * 0.114f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    ))
    Canvas(modifier) {
        val w = img.width.toFloat()
        val h = img.height.toFloat()
        val s = min(size.width / (w + 8f), size.height / (h + 10f)).coerceAtLeast(1f)
        val ox = (size.width - w * s) / 2f
        val low = if ("rebaixada" in tuning) 1f else 0f
        val oy = (size.height - h * s) / 2f + low * s
        fun r(c: Color, x: Float, y: Float, ww: Float, hh: Float) =
            drawRect(c, Offset(ox + x * s, oy + y * s), Size(ww * s, hh * s))
        if ("neon" in tuning) {
            glow(Offset(ox + w / 2f * s, oy + (h - 0.5f) * s), w * 0.62f * s, Color(0xFF00E5FF).copy(alpha = 0.55f))
            r(Color(0xFF9CF3FF), 5f, h - 0.6f, w - 10f, 0.9f)
        }
        drawOval(Color(0x55000000), Offset(ox + 2f * s, oy + (h - 2f) * s), Size((w - 4f) * s, 4f * s))
        drawImage(
            img, IntOffset.Zero, IntSize(img.width, img.height),
            IntOffset(ox.toInt(), oy.toInt()), IntSize((w * s).toInt(), (h * s).toInt()),
            1f, Fill, ColorFilter.colorMatrix(m), BlendMode.SrcOver, FilterQuality.None,
        )
        if ("pelicula" in tuning) {
            clipRect(ox + w * 0.18f * s, oy + (geo[0] + 1f) * s, ox + w * 0.80f * s, oy + (geo[0] + h * 0.32f) * s) {
                drawImage(
                    img, IntOffset.Zero, IntSize(img.width, img.height),
                    IntOffset(ox.toInt(), oy.toInt()), IntSize((w * s).toInt(), (h * s).toInt()),
                    1f, Fill, ColorFilter.tint(Color(0xB005080F), BlendMode.SrcAtop), BlendMode.SrcOver, FilterQuality.None,
                )
            }
        }
        drawWheels(ci, ox, oy, s, h, t * 12.5f, "rodas" in tuning)
        if (driver != null) drawDriver(driver, ox, oy, s, w)
        if ("adesivo" in tuning) {
            r(Color(0xFFE53935), w * 0.2f, h * 0.55f, w * 0.62f, 1.2f)
            r(Color(0xFFFFFFFF), w * 0.2f, h * 0.55f + 1.6f, w * 0.62f, 0.5f)
        }
        if ("aerofolio" in tuning) {
            val d = geo[1].toFloat()
            r(Color(0xFF616161), 0f, d - 3.2f, w * 0.15f, 0.6f)
            r(Color(0xFF1B1B1B), 0f, d - 2.6f, w * 0.15f, 1.2f)
            r(Color(0xFF1B1B1B), w * 0.04f, d - 1.4f, 1.1f, 1.6f)
            r(Color(0xFF1B1B1B), w * 0.11f, d - 1.4f, 1.1f, 1.6f)
        }
        if ("parachoque" in tuning) {
            r(Color(0xFF1B1B1B), w - 5.5f, h - 6.5f, 5.5f, 2.6f)
            r(Color(0xFF424242), w - 6f, h - 4f, 6f, 0.8f)
        }
        if ("escape" in tuning) r(Color(0xFFB0BEC5), -1.2f, h - 4.5f, 3f, 1.3f)
        val body = cond.getOrElse(1) { 100 }
        if (body < 45) {
            val rnd = Random(7)
            repeat(((45 - body) / 4).coerceIn(1, 10)) {
                r(Color(0xFF8D4B1F), rnd.nextFloat() * (w - 8f) + 3f, rnd.nextFloat() * (h * 0.4f) + h * 0.35f, 2f, 1.5f)
            }
        }
        if (cond.getOrElse(0) { 100 } < 30) {
            for (i in 0..2) {
                val p = (t + i / 3f) % 1f
                drawCircle(Color(0x66AAAAAA).copy(alpha = 0.5f * (1f - p)), (2f + p * 3f) * s, Offset(ox + (w - 6f) * s, oy + (6f - p * 12f) * s))
            }
        }
    }
}
