package com.ferrovelho.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/*
 * =====================================================================================
 *  VEÍCULOS EM PIXEL ART (sprites em camadas)
 * =====================================================================================
 *  Cada veículo é composto por camadas, desenhadas SEMPRE nesta ordem:
 *     sombra -> interior (vidro por dentro) + banco + motorista -> poço da roda -> RODAS -> carroceria
 *  A carroceria (vb<i>.png) tem os arcos das rodas recortados e vidros translúcidos, então a roda
 *  "entra" no paralama e o motorista aparece pela janela. As rodas (vw9/11/13.png) são um atlas com
 *  6 quadros de giro + 1 quadro borrado (alta velocidade) e variantes (aço, esportiva, enferrujada,
 *  pneu novo/gasto, furado). As posições de roda, farol, lanterna, motorista etc. vêm de VehicleData.kt
 *  (gerado junto com a arte), então roda NUNCA mais fica desalinhada.
 *
 *  A COR: a pintura do PNG usa 5 tons-marcadores (magenta). Ao carregar, trocamos os marcadores por
 *  um degradê da cor escolhida (brilho/claro/base/sombra/escuro) -> sem filtro lavado.
 *  O DESGASTE (ferrugem, tinta desbotada, massa, amassados, riscos, porta trocada, farol quebrado,
 *  vidro trincado, sujeira) é "assado" no próprio sprite pixel a pixel, em cache por
 *  (modelo, cor, nível de desgaste, adesivo).
 *
 *  Espaço de desenho: "unidades" = pixels do sprite. Chão em y = 22, centro do carro em x = 31.
 * =====================================================================================
 */

private fun lerpI(a: Int, b: Int, t: Float): Int = (a + (b - a) * t).roundToInt().coerceIn(0, 255)

private fun mixRgb(a: Int, b: Int, t: Float): Int {
    val r = lerpI((a shr 16) and 255, (b shr 16) and 255, t)
    val g = lerpI((a shr 8) and 255, (b shr 8) and 255, t)
    val bl = lerpI(a and 255, b and 255, t)
    return (r shl 16) or (g shl 8) or bl
}

class VehSet(
    private val raw: List<Bitmap>,
    val interior: List<ImageBitmap>,
    val wheels: List<ImageBitmap>,
) {
    private val cache = HashMap<Long, ImageBitmap>()

    /** Sprite recolorido (+ desgaste) com cache. paint = ARGB da cor da pintura. bucket = 0..10 (desgaste da lataria). */
    fun painted(idx: Int, paint: Int, bucket: Int, deco: Int): ImageBitmap {
        val i = idx.coerceIn(0, raw.size - 1)
        val rgb = paint and 0xFFFFFF
        val b = bucket.coerceIn(0, 10)
        val key = (((i.toLong() * 16777216L + rgb.toLong()) * 16L) + b.toLong()) * 4L + deco.toLong()
        val hit = cache[key]
        if (hit != null) return hit
        val made = bake(i, rgb, b, deco)
        cache[key] = made
        return made
    }

    private fun bake(i: Int, rgbIn: Int, bucket: Int, deco: Int): ImageBitmap {
        val src = raw[i]
        val w = src.width
        val h = src.height
        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)
        val m = VMETA[i]
        val sev = bucket / 10f

        // ---- degradê da pintura (envelhece junto com a lataria)
        val base = mixRgb(rgbIn, 0x7D766A, sev * 0.55f)
        val p0 = mixRgb(base, 0xFFF8E2, 0.50f)
        val p1 = mixRgb(base, 0xFFF8E2, 0.20f)
        val p3 = mixRgb(mixRgb(base, 0x000000, 0.30f), 0x242C68, 0.14f)
        val p4 = mixRgb(mixRgb(base, 0x000000, 0.55f), 0x242C68, 0.12f)

        val isPaint = BooleanArray(w * h)
        val paintIdx = ArrayList<Int>()
        for (k in px.indices) {
            val c = px[k]
            if ((c ushr 24) != 255) continue
            val r = (c shr 16) and 255
            val g = (c shr 8) and 255
            val bl = c and 255
            if (g != 0 || r != bl) continue
            val rgb = when (r) {
                255 -> p0
                230 -> p1
                200 -> base
                160 -> p3
                110 -> p4
                else -> -1
            }
            if (rgb == -1) continue
            px[k] = (0xFF shl 24) or rgb
            isPaint[k] = true
            paintIdx.add(k)
        }

        fun cur(x: Int, y: Int): Int = px[y * w + x] and 0xFFFFFF
        fun put(x: Int, y: Int, rgb: Int) {
            if (x < 0 || y < 0 || x >= w || y >= h) return
            if (!isPaint[y * w + x]) return
            px[y * w + x] = (0xFF shl 24) or rgb
        }
        fun rawPut(x: Int, y: Int, argb: Int) {
            if (x < 0 || y < 0 || x >= w || y >= h) return
            px[y * w + x] = argb
        }

        // ---- adesivo (faixa vermelha e branca na lateral)
        if (deco == 1 && m.door[2] > m.door[0]) {
            val sy = (m.door[1] + m.door[3]) / 2
            for (k in paintIdx) {
                val y = k / w
                if (y == sy || y == sy + 1) px[k] = (0xFF shl 24) or 0xE53935
                else if (y == sy + 2) px[k] = (0xFF shl 24) or 0xF5F5F5
            }
        }

        if (bucket > 0 && paintIdx.isNotEmpty()) {
            val rnd = Random(i * 131 + rgbIn)
            // ---- sujeira embaixo
            val gy0 = m.bot - 7
            for (k in paintIdx) {
                val y = k / w
                if (y >= gy0) {
                    val tt = sev * 0.55f * ((y - gy0 + 1) / 8f)
                    px[k] = (0xFF shl 24) or mixRgb(px[k] and 0xFFFFFF, 0x3E2A1A, tt.coerceIn(0f, 1f))
                }
            }
            // ---- porta trocada (outra cor, quando está bem judiada)
            if (sev > 0.5f && m.door[2] > m.door[0] + 2) {
                val d = m.door
                for (y in d[1]..d[3]) {
                    val shade = ((y - d[1]) / max(1f, (d[3] - d[1]).toFloat())) * 0.35f
                    for (x in (d[0] + 1) until d[2]) {
                        if (x in 0 until w && y in 0 until h && isPaint[y * w + x]) {
                            px[y * w + x] = (0xFF shl 24) or mixRgb(0xB7AE9A, 0x000000, shade)
                        }
                    }
                }
            }
            val low = ArrayList<Int>()
            for (k in paintIdx) if (k / w >= m.bot - 8) low.add(k)
            // ---- ferrugem
            val rustN = (sev * 34f).toInt()
            for (n in 0 until rustN) {
                val pool = if (low.isNotEmpty() && rnd.nextFloat() < 0.6f) low else paintIdx
                val k = pool[rnd.nextInt(pool.size)]
                val x = k % w
                val y = k / w
                val col = when (n % 4) { 0 -> 0xA14F1E; 1 -> 0x5B2F12; 2 -> 0xC9772F; else -> 0xA14F1E }
                put(x, y, col)
                if (sev > 0.3f) put(x + 1, y, col)
                if (n % 3 == 0) put(x, y + 1, 0x5B2F12)
                if (sev > 0.6f && n % 2 == 0) put(x - 1, y, 0x5B2F12)
            }
            // ---- tinta saiu: massa / primer
            if (sev > 0.4f) {
                val n2 = ((sev - 0.3f) * 9f).toInt()
                for (n in 0 until n2) {
                    val k = paintIdx[rnd.nextInt(paintIdx.size)]
                    val x = k % w
                    val y = k / w
                    val col = if (n % 2 == 0) 0x9AA0A6 else 0xB7AE9A
                    put(x, y, col); put(x + 1, y, col); put(x + 2, y, col); put(x, y + 1, col); put(x + 1, y + 1, col)
                }
            }
            // ---- amassados
            if (sev > 0.3f) {
                val n3 = min(6, ((sev - 0.2f) * 7f).toInt())
                for (n in 0 until n3) {
                    val k = paintIdx[rnd.nextInt(paintIdx.size)]
                    val x = k % w
                    val y = k / w
                    if (y < 1) continue
                    put(x, y, mixRgb(cur(x, y), 0x000000, 0.30f))
                    put(x + 1, y, mixRgb(cur(x + 1, y), 0x000000, 0.20f))
                    put(x, y - 1, mixRgb(cur(x, y - 1), 0xFFFFFF, 0.25f))
                }
            }
            // ---- riscos
            if (sev > 0.12f) {
                val n4 = ((sev + 0.1f) * 6f).toInt()
                for (n in 0 until n4) {
                    val k = paintIdx[rnd.nextInt(paintIdx.size)]
                    val x = k % w
                    val y = k / w
                    put(x, y, mixRgb(cur(x, y), 0xFFFFFF, 0.5f))
                    put(x + 1, y, mixRgb(cur(x + 1, y), 0xFFFFFF, 0.4f))
                    put(x + 2, y, mixRgb(cur(x + 2, y), 0xFFFFFF, 0.3f))
                }
            }
            // ---- farol quebrado
            val hr = m.headRect
            if (sev > 0.65f && hr[2] > 0) {
                for (yy in hr[1] until (hr[1] + hr[3])) for (xx in hr[0] until (hr[0] + hr[2])) rawPut(xx, yy, (0xFF shl 24) or 0x3A3A3A)
                rawPut(hr[0], hr[1] + hr[3] - 1, (0xFF shl 24) or 0xBDBDBD)
                rawPut(hr[0] + 1, hr[1] + hr[3] - 1, (0xFF shl 24) or 0xBDBDBD)
            }
            // ---- vidro trincado
            if (sev > 0.55f && m.glass[2] > m.glass[0]) {
                val cx = m.glass[2] - 3
                val cy = m.glass[1] + 2
                val cr = 0xCCFFFFFF.toInt()
                rawPut(cx, cy, cr); rawPut(cx + 1, cy + 1, cr); rawPut(cx + 2, cy + 1, cr); rawPut(cx + 3, cy + 2, cr)
                rawPut(cx - 1, cy + 1, cr); rawPut(cx - 2, cy + 2, cr); rawPut(cx, cy + 2, cr); rawPut(cx, cy - 1, cr)
            }
        }

        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.setPixels(px, 0, w, 0, 0, w, h)
        return bmp.asImageBitmap()
    }
}

fun loadVehicles(ctx: Context): VehSet {
    val o = BitmapFactory.Options().apply { inScaled = false }
    fun bmp(n: String): Bitmap = try {
        ctx.assets.open("city/$n.png").use { BitmapFactory.decodeStream(it, null, o)!! }
    } catch (e: Exception) {
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }
    val raw = List(V_COUNT) { bmp("vb$it") }
    val inter = List(V_COUNT) { bmp("vi$it").asImageBitmap() }
    val wh = listOf(9, 11, 13).map { bmp("vw$it").asImageBitmap() }
    return VehSet(raw, inter, wh)
}

/** Carroceria do catálogo -> sprite (0..20). */
fun vehicleFor(b: Body): Int = when (b) {
    Body.BEETLE -> 0; Body.VAN -> 1; Body.UNO -> 2; Body.GOL -> 3; Body.COMPACT -> 4; Body.OPALA -> 5
    Body.BOXY -> 6; Body.WAGON -> 7; Body.MPV -> 8; Body.HATCH -> 9; Body.SEDAN -> 10; Body.SUV -> 11
    Body.PICKUP -> 12; Body.COUPE -> 13; Body.CLASSIC -> 14; Body.SUPER -> 15; Body.NOTCH -> 16
    Body.MUSCLE -> 17; Body.P911 -> 18; Body.JEEP -> 19; Body.MINI -> 20
    else -> 9
}

private fun DrawScope.img(im: ImageBitmap, sx: Int, sy: Int, sw: Int, sh: Int, dx: Int, dy: Int, filter: ColorFilter? = null) {
    drawImage(
        im, IntOffset(sx, sy), IntSize(sw, sh), IntOffset(dx, dy), IntSize(sw, sh), 1f,
        androidx.compose.ui.graphics.drawscope.Fill, filter, BlendMode.SrcOver, FilterQuality.None,
    )
}

/** retângulo recortado pela caixa do vidro (assim banco/motorista nunca vazam pra fora da janela) */
private fun DrawScope.crect(c: Color, x: Int, y: Int, w: Int, h: Int, x0: Int, y0: Int, x1: Int, y1: Int) {
    val ax = max(x, x0)
    val ay = max(y, y0)
    val bx = min(x + w, x1 + 1)
    val by = min(y + h, y1 + 1)
    if (bx > ax && by > ay) drawRect(c, Offset(ax.toFloat(), ay.toFloat()), Size((bx - ax).toFloat(), (by - ay).toFloat()))
}

/**
 * Desenha UM veículo no espaço de unidades (chão em y=22, centro em x=31), virado pra direita.
 *  rot   = distância andada pra frente (gira as rodas); speed = velocidade (acima de ~60 usa roda borrada)
 */
fun DrawScope.drawVeh(
    set: VehSet, idx: Int, paintColor: Color, tuning: List<String>, cond: List<Int>,
    t: Float, driver: Look?, rot: Float, speed: Float,
) {
    try {
        val i = idx.coerceIn(0, V_COUNT - 1)
        val m = VMETA[i]
        fun part(k: Int): Int = cond.getOrElse(k) { 100 }
        val motor = part(0)
        val lat = part(1)
        val tire = part(2)
        val inter = part(3)
        val susp = part(4)
        val sev = ((100 - lat) / 100f).coerceIn(0f, 1f)
        val bucket = (sev * 10f).roundToInt().coerceIn(0, 10)
        val flat = tire < 25
        val sagK = max(0, 65 - susp) / 65f
        var dy = 0
        if ("rebaixada" in tuning) dy += 2
        if (sagK > 0.5f) dy += 2 else if (sagK > 0.15f) dy += 1
        if (flat) dy += 1
        dy = min(dy, 4)
        val deco = if ("adesivo" in tuning) 1 else 0
        val body = set.painted(i, paintColor.toArgb(), bucket, deco)
        val w = m.w
        val h = m.h
        val gy = m.gy.toFloat()
        val fdy = dy.toFloat()

        withTransform({ translate((31 - m.cx).toFloat(), (22 - m.gy).toFloat()) }) {
            // ---- neon + sombra no chão
            if ("neon" in tuning) {
                glow(Offset(m.cx.toFloat(), gy - 1f), 30f, Color(0xFF00E5FF).copy(alpha = 0.55f))
                drawRect(Color(0xFF9CF3FF), Offset(m.rear + 5f, gy - 1f), Size((m.front - m.rear - 10).toFloat(), 1f))
            }
            drawRect(Color(0x55000000), Offset(m.rear + 1f, gy), Size((m.front - m.rear - 1).toFloat(), 1f))
            drawRect(Color(0x2E000000), Offset(m.rear + 4f, gy + 1f), Size((m.front - m.rear - 7).toFloat(), 1f))

            // ---- interior, banco e motorista (atrás da carroceria: só aparecem pelo vidro)
            if (m.cab) {
                img(set.interior[i], 0, 0, w, h, 0, dy)
                val hx = m.head[0]
                val hy = m.head[1] + dy
                val g0 = m.glass[0]
                val g1 = m.glass[2]
                val gt = m.glass[1] + dy
                val gb = m.glass[3] + dy
                val seatCol = when {
                    "couro" in tuning -> Color(0xFF6D4C41)
                    inter < 45 -> Color(0xFF6E6E6E)
                    else -> Color(0xFF37474F)
                }
                crect(seatCol, hx - 4, hy + 1, 3, 8, g0, gt, g1, gb)
                if ("couro" in tuning) crect(Color(0xFF8D6E63), hx - 4, hy + 1, 3, 1, g0, gt, g1, gb)
                if (inter < 45 && "couro" !in tuning) {
                    crect(Color(0xFFF2E6C9), hx - 3, hy + 2, 2, 2, g0, gt, g1, gb)
                    crect(Color(0xFF2B2B2B), hx - 2, hy + 4, 1, 2, g0, gt, g1, gb)
                }
                if (driver != null) {
                    val dark = Color(0xFF212121)
                    crect(CTOP[driver.top], hx - 1, hy + 3, 5, 6, g0, gt, g1, gb)
                    crect(CSKIN[driver.skin], hx, hy, 3, 3, g0, gt, g1, gb)
                    if (driver.hairStyle != 3) crect(CHAIR[driver.hair], hx, hy - 1, 3, 1, g0, gt, g1, gb)
                    crect(dark, hx + 2, hy + 1, 1, 1, g0, gt, g1, gb)
                    if (driver.glasses) crect(dark, hx + 1, hy + 1, 2, 1, g0, gt, g1, gb)
                    crect(Color(0xFF1B1B1B), hx + 4, hy + 3, 1, 3, g0, gt, g1, gb)
                }
                if ("pelicula" in tuning) {
                    img(set.interior[i], 0, 0, w, h, 0, dy, ColorFilter.tint(Color(0xD005080F), BlendMode.SrcAtop))
                }
            }

            // ---- poço das rodas (escuro, atrás da carroceria: aparece pelo arco recortado)
            if (m.archR > 0f) {
                val r = m.archR
                for (wi in m.wx.indices) {
                    val cxw = m.wx[wi] + m.d / 2f
                    val cyw = m.wy[wi] + m.d / 2f
                    val x0 = (cxw - r).toInt()
                    val yTop = (cyw - r).toInt()
                    drawRect(
                        Color(0xFF0C0D10), Offset(x0.toFloat(), (yTop + dy).toFloat()),
                        Size((2 * r).toInt() + 1f, (m.bot + dy - yTop).toFloat()),
                    )
                }
            }

            // ---- RODAS (atlas): quadro de giro, borrado em alta velocidade, furado, gasto, esportiva, enferrujada
            val ai = when (m.d) { 9 -> 0; 11 -> 1; else -> 2 }
            val atlas = set.wheels[ai]
            val d = m.d
            val sport = "rodas" in tuning
            val kind = if (sport) 1 else if (sev > 0.5f) 2 else 0
            val worn = tire < 55
            val fr = if (speed > 62f) 6 else (((rot / 0.95f).toInt() % 6) + 6) % 6
            for (wi in m.wx.indices) {
                val isFlat = flat && wi == 0
                val row = if (isFlat) 6 + kind else kind * 2 + (if (worn) 1 else 0)
                img(atlas, (if (isFlat) 0 else fr) * d, row * d, d, d, m.wx[wi], m.wy[wi])
            }

            // ---- carroceria
            img(body, 0, 0, w, h, 0, dy)

            // ---- tuning por cima
            val bt = (m.bot - 4 + dy).toFloat()
            if ("parachoque" in tuning) {
                drawRect(Color(0xFF1B1B1B), Offset(m.front - 5f, bt), Size(7f, 4f))
                drawRect(Color(0xFF424242), Offset(m.front - 5f, bt + 4f), Size(7f, 1f))
                drawRect(Color(0xFF1B1B1B), Offset(m.rear - 2f, bt), Size(6f, 4f))
            }
            if ("aerofolio" in tuning) {
                val wx = m.wing[0].toFloat()
                val wy = (m.wing[1] + dy).toFloat()
                val dk = Color(0xFF1B1B1B)
                drawRect(dk, Offset(wx + 1f, wy - 3f), Size(1f, 4f))
                drawRect(dk, Offset(wx + 6f, wy - 3f), Size(1f, 4f))
                drawRect(dk, Offset(wx - 1f, wy - 5f), Size(10f, 2f))
                drawRect(Color(0xFF6B6F76), Offset(wx - 1f, wy - 5f), Size(10f, 1f))
                drawRect(paintColor, Offset(wx - 1f, wy - 7f), Size(1f, 3f))
                drawRect(paintColor, Offset(wx + 8f, wy - 7f), Size(1f, 3f))
            }
            if ("escape" in tuning || "turbo" in tuning) {
                drawRect(Color(0xFFB0BEC5), Offset(m.rear - 3f, bt + 1f), Size(4f, 2f))
                drawRect(Color(0xFF546E7A), Offset(m.rear - 3f, bt + 2f), Size(4f, 1f))
            }
            if ("turbo" in tuning) {
                val fl = (1.5f + 3.5f * (0.5f + 0.5f * sin(t * 2f * PI.toFloat() * 3f))).roundToInt().toFloat()
                drawRect(Color(0xFF29B6F6), Offset(m.rear - 3f - fl, bt + 1f), Size(fl, 2f))
                drawRect(Color(0xFFFFF59D), Offset(m.rear - 3f - fl / 2f, bt + 1f), Size(max(1f, fl / 2f), 1f))
                glow(Offset(m.rear - 4f, bt + 2f), 5f, Color(0xFF29B6F6).copy(alpha = 0.5f))
            }

            // ---- motor ruim: fumaça e faíscas
            if (motor < 45) {
                val smokeX = m.front - 14f
                val smokeY = m.hoodY.toFloat() - 1f
                val black = motor < 25
                val puffs = if (black) 5 else 3
                for (k in 0 until puffs) {
                    val p = (t + k / puffs.toFloat()) % 1f
                    val col = if (black) Color(0xFF1B1B1B) else Color(0xFFBDBDBD)
                    val sz = (1.5f + p * 3f).roundToInt().toFloat()
                    drawRect(
                        col.copy(alpha = 0.6f * (1f - p)),
                        Offset((smokeX + sin((p + k) * 5f) * 1.2f).roundToInt().toFloat(), (smokeY - p * 10f).roundToInt().toFloat()),
                        Size(sz, sz),
                    )
                }
                if (motor < 12 && ((t * 6f).toInt() % 2) == 0) {
                    drawRect(Color(0xFFFFEB3B), Offset(smokeX + 1f, smokeY + 1f), Size(1f, 1f))
                    drawRect(Color(0xFFFF9800), Offset(smokeX + 3f, smokeY), Size(1f, 1f))
                }
            }

            // ---- som potente: ondas em pixel saindo da janela
            if ("som" in tuning && m.glass[2] > m.glass[0]) {
                for (k in 0..2) {
                    val p = (t * 1.5f + k / 3f) % 1f
                    val xx = (m.glass[0] + (m.glass[2] - m.glass[0]) / 2 + 2 + p * 10f).roundToInt().toFloat()
                    drawRect(
                        Color.White.copy(alpha = 0.7f * (1f - p)),
                        Offset(xx, m.glass[1].toFloat() - 1f - k), Size(1f, (3 + k * 2).toFloat()),
                    )
                }
            }
        }
    } catch (e: Throwable) {
    }
}

/**
 * Desenha um veículo no MUNDO do jogo.  wx = x do CENTRO do carro (mundo), ground = y do chão (mundo).
 * flip = virado pra esquerda.  k = escala (use 1: assim cada pixel do sprite = 1 pixel do cenário).
 */
fun DrawScope.drawVehWorld(
    set: VehSet, idx: Int, paint: Color, tuning: List<String>, cond: List<Int>,
    wx: Float, ground: Float, cam: Float, s: Float, flip: Boolean,
    t: Float, driver: Look?, rot: Float, speed: Float, k: Float = 1f,
) {
    val cxs = (wx - cam) * s
    if (cxs < -150f * s * k || cxs > size.width + 150f * s * k) return
    val topY = (ground - 22f * k) * s
    val left = cxs - 31f * k * s
    withTransform({
        if (flip) scale(-1f, 1f, Offset(cxs, topY))
        translate(left, topY)
        scale(k * s, k * s, Offset.Zero)
    }) {
        drawVeh(set, idx, paint, tuning, cond, t, driver, rot, speed)
    }
}

/** comprimento útil (unidades) do veículo: pra manter distância no trânsito */
fun vehLen(idx: Int): Float {
    val m = VMETA[idx.coerceIn(0, V_COUNT - 1)]
    return (m.front - m.rear + 2).toFloat()
}
