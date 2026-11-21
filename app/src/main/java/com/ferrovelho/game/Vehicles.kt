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

/** cores do neon do pack de tunagem (azul, roxo, vermelho, verde, amarelo) */
private val NEON_COLORS = longArrayOf(0xFF3F8CFF, 0xFFB040FF, 0xFFFF3030, 0xFF30FF40, 0xFFFFD030)

private fun lerpI(a: Int, b: Int, t: Float): Int = (a + (b - a) * t).roundToInt().coerceIn(0, 255)

private fun mixRgb(a: Int, b: Int, t: Float): Int {
    val r = lerpI((a shr 16) and 255, (b shr 16) and 255, t)
    val g = lerpI((a shr 8) and 255, (b shr 8) and 255, t)
    val bl = lerpI(a and 255, b and 255, t)
    return (r shl 16) or (g shl 8) or bl
}

/** medidas do sprite i (0..26 = originais, 27.. = packs) */
fun vmeta(i: Int): VMeta = if (i < V_COUNT) VMETA[i.coerceAtLeast(0)] else PMETA[(i - V_COUNT).coerceIn(0, P_COUNT - 1)]

/** curva de sombreado da pintura dos sprites dos packs: v = luminância relativa codificada (128 = tom base) */
private fun shadePack(base: Int, v: Int): Int {
    val r = v / 128f
    return if (r <= 1f) {
        val rr = (((base shr 16) and 255) * r).roundToInt().coerceIn(0, 255)
        val gg = (((base shr 8) and 255) * r).roundToInt().coerceIn(0, 255)
        val bb = ((base and 255) * r).roundToInt().coerceIn(0, 255)
        (rr shl 16) or (gg shl 8) or bb
    } else mixRgb(base, 0xFFFFFF, ((r - 1f) * 0.9f).coerceAtMost(1f))
}

class VehSet(
    private val raw: List<Bitmap>,
    val interior: List<ImageBitmap>,
    val wheels: List<ImageBitmap>,
    private val assets: android.content.res.AssetManager? = null,
) {
    private val cache = HashMap<Long, ImageBitmap>()

    // ---- packs (carregados só quando o carro aparece)
    private val opts = BitmapFactory.Options().apply { inScaled = false }
    private val rawBmp = HashMap<String, Bitmap?>()
    private val rawImg = HashMap<String, ImageBitmap?>()

    /** PNG de assets/packs/<nome>.png (null se não existir). */
    fun partBmp(name: String): Bitmap? {
        if (rawBmp.containsKey(name)) return rawBmp[name]
        val b = try { assets?.open("packs/$name.png")?.use { BitmapFactory.decodeStream(it, null, opts) } } catch (e: Exception) { null }
        rawBmp[name] = b
        return b
    }
    fun part(name: String): ImageBitmap? {
        if (rawImg.containsKey(name)) return rawImg[name]
        val im = partBmp(name)?.asImageBitmap()
        rawImg[name] = im
        return im
    }
    fun packInterior(k: Int): ImageBitmap? = part("pi$k")
    fun packWheels(k: Int): ImageBitmap? = part("pw$k")
    /** atlas das rodas esportivas do pack de tunagem para o diâmetro d (8..14) */
    fun sportWheels(d: Int): ImageBitmap? = part("pws${d.coerceIn(8, 14)}")

    /** Sprite recolorido (+ desgaste) com cache. paint = ARGB da cor da pintura. bucket = 0..10 (desgaste da lataria). */
    fun painted(idx: Int, paint: Int, bucket: Int, deco: Int): ImageBitmap {
        val i = idx.coerceIn(0, V_TOTAL - 1)
        val rgb = paint and 0xFFFFFF
        val b = bucket.coerceIn(0, 10)
        // deco: 0 nada, 1 faixa, 2..7 adesivos do pack; +8 = insulfilm (vidro escuro nos sprites do pack)
        val key = (((i.toLong() * 16777216L + rgb.toLong()) * 16L) + b.toLong()) * 16L + (deco and 15).toLong()
        val hit = cache[key]
        if (hit != null) return hit
        val made = bake(i, rgb, b, deco)
        cache[key] = made
        return made
    }

    private fun bake(i: Int, rgbIn: Int, bucket: Int, decoIn: Int): ImageBitmap {
        val m = vmeta(i)
        val src = if (m.pack) (partBmp("pb${i - P_BASE}") ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)) else raw[i]
        val w = src.width
        val h = src.height
        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)
        val film = (decoIn and 8) != 0
        val deco = decoIn and 7
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
            val rgb = if (m.pack) { if (r > 0) shadePack(base, r) else -1 } else when (r) {
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

        // ---- insulfilm (sprites do pack): vidro semitransparente fica escuro e quase opaco
        if (m.pack && film) {
            for (k in px.indices) {
                val a = px[k] ushr 24
                if (a in 1..254) px[k] = (0xEB shl 24) or 0x0B0E14
            }
        }
        // ---- adesivo do pack de tunagem (chamas, xadrez, tribal...): só pinta em cima da lataria
        if (m.pack && deco >= 2) {
            val dw0 = m.door[2] - m.door[0]
            val size = intArrayOf(16, 22, 28).minByOrNull { kotlin.math.abs(it - dw0) } ?: 22
            val dc = partBmp("decal${deco - 2}_$size")
            if (dc != null) {
                val dp = IntArray(dc.width * dc.height)
                dc.getPixels(dp, 0, dc.width, 0, 0, dc.width, dc.height)
                val x0 = (m.door[0] + m.door[2] - dc.width) / 2
                val y0 = (m.door[1] + m.door[3] - dc.height) / 2 + 1
                for (yy in 0 until dc.height) for (xx in 0 until dc.width) {
                    val c = dp[yy * dc.width + xx]
                    if ((c ushr 24) < 128) continue
                    val X = x0 + xx
                    val Y = y0 + yy
                    if (X in 0 until w && Y in 0 until h && isPaint[Y * w + X]) px[Y * w + X] = (0xFF shl 24) or (c and 0xFFFFFF)
                }
            }
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
    return VehSet(raw, inter, wh, ctx.assets)
}

/** Carroceria do catálogo -> sprite (0..20). */
fun vehicleFor(b: Body): Int = if (b.pk >= 0) b.pk else when (b) {
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
        val i = idx.coerceIn(0, V_TOTAL - 1)
        val m = vmeta(i)
        val k = i - P_BASE          // índice dentro dos packs (só vale se m.pack)
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
        val deco = (if ("adesivo" in tuning) (if (m.pack) 2 + (k % 6) else 1) else 0) +
            (if (m.pack && "pelicula" in tuning) 8 else 0)
        val body = set.painted(i, paintColor.toArgb(), bucket, deco)
        val w = m.w
        val h = m.h
        val gy = m.gy.toFloat()
        val fdy = dy.toFloat()

        withTransform({ translate((31 - m.cx).toFloat(), (22 - m.gy).toFloat()) }) {
            // ---- neon + sombra no chão
            if ("neon" in tuning) {
                // cor do neon alterna devagar entre ciano e magenta; luz espalhada no chão embaixo do carro
                val mix = (0.5f + 0.5f * sin(t * 2f * PI.toFloat() * 0.35f)).coerceIn(0f, 1f)
                val nc = if (m.pack) {
                    // pack: cor do neon escolhida no pack de tunagem (por carro), com um brilho pulsando
                    val pn = Color(NEON_COLORS[k.coerceAtLeast(0) % NEON_COLORS.size])
                    Color(pn.red + (1f - pn.red) * mix * 0.35f, pn.green + (1f - pn.green) * mix * 0.35f, pn.blue + (1f - pn.blue) * mix * 0.35f, 1f)
                } else Color(0xFF00E5FF).let { a -> Color(a.red + (1f - a.red) * mix, a.green * (1f - mix * 0.8f), a.blue + (0.6f - a.blue) * mix * 0.5f, 1f) }
                glow(Offset(m.cx.toFloat(), gy - 1f), 34f, nc.copy(alpha = 0.55f))
                drawRect(nc.copy(alpha = 0.28f), Offset(m.rear + 1f, gy - 1f), Size((m.front - m.rear - 2).toFloat(), 4f))
                drawRect(nc, Offset(m.rear + 5f, gy - 1f), Size((m.front - m.rear - 10).toFloat(), 1f))
            }
            drawRect(Color(0x55000000), Offset(m.rear + 1f, gy), Size((m.front - m.rear - 1).toFloat(), 1f))
            drawRect(Color(0x2E000000), Offset(m.rear + 4f, gy + 1f), Size((m.front - m.rear - 7).toFloat(), 1f))

            // ---- interior, banco e motorista (atrás da carroceria: só aparecem pelo vidro)
            if (m.cab) {
                val interIm = if (m.pack) set.packInterior(k) else set.interior[i]
                if (interIm != null) img(interIm, 0, 0, w, h, 0, dy)
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
                if ("couro" in tuning) {
                    crect(Color(0xFF8D6E63), hx - 4, hy + 1, 3, 1, g0, gt, g1, gb)
                    crect(Color(0xFF4E342E), hx - 4, hy + 5, 3, 1, g0, gt, g1, gb)          // costura do encosto
                    crect(Color(0xFF6D4C41), hx - 12, hy + 2, 3, 7, g0, gt, g1, gb)         // banco traseiro
                    crect(Color(0xFF8D6E63), hx - 12, hy + 2, 3, 1, g0, gt, g1, gb)
                    crect(Color(0xFF5D4037), hx + 3, hy + 6, 4, 3, g0, gt, g1, gb)          // painel de couro
                }
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
                if ("pelicula" in tuning && interIm != null) {
                    img(interIm, 0, 0, w, h, 0, dy, ColorFilter.tint(Color(0xD005080F), BlendMode.SrcAtop))
                }
            }

            // ---- poço das rodas (escuro, atrás da carroceria: aparece pelo arco recortado)
            if (m.pack) {
                // sprites do pack: o recorte na lataria é um disco do tamanho da roda; atrás dele, um disco escuro (moto não tem poço)
                if (PACK_KIND[k] != "moto") {
                    for (wi in m.wx.indices) {
                        drawCircle(Color(0xFF0C0D10), m.d / 2f + 0.5f, Offset(m.wx[wi] + m.d / 2f, m.wy[wi] + m.d / 2f + dy))
                    }
                }
            } else if (m.archR > 0f) {
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

            // ---- RODAS: quadro de giro, borrado em alta velocidade, furado, gasto, esportiva, enferrujada
            val sport = "rodas" in tuning
            val fr = if (speed > 62f) 6 else (((rot / 0.95f).toInt() % 6) + 6) % 6
            val worn = tire < 55
            if (m.pack) {
                // pack: atlas próprio do carro (4 linhas: normal, gasta, enferrujada, furada) ou rodas esportivas do pack de tunagem
                val d = m.d
                val wd = d.coerceIn(8, 14)
                val atlas = if (sport) set.sportWheels(d) else set.packWheels(k)
                if (atlas != null) {
                    for (wi in m.wx.indices) {
                        val isFlat = flat && wi == 0 && !sport
                        if (sport) {
                            img(atlas, fr * wd, (k % 6) * wd, wd, wd, m.wx[wi] + (d - wd) / 2, m.wy[wi] + (d - wd) / 2)
                        } else {
                            val row = if (isFlat) 3 else if (sev > 0.5f) 2 else if (worn) 1 else 0
                            img(atlas, (if (isFlat) 0 else fr) * d, row * d, d, d, m.wx[wi], m.wy[wi])
                        }
                    }
                }
                if ("rebaixada" in tuning) {
                    // mola colorida do coilover aparecendo no vão da roda
                    val cc = Color(COIL_COLORS[k % COIL_COLORS.size])
                    for (wi in m.wx.indices) drawRect(cc, Offset(m.wx[wi] + d / 2f, (m.wy[wi] + dy - 1).toFloat()), Size(2f, max(2, dy + 1).toFloat()))
                }
            } else {
                val ai = when (m.d) { 9 -> 0; 11 -> 1; else -> 2 }
                val atlas = set.wheels[ai]
                val d = m.d
                val kind = if (sport) 1 else if (sev > 0.5f) 2 else 0
                for (wi in m.wx.indices) {
                    val isFlat = flat && wi == 0
                    val row = if (isFlat) 6 + kind else kind * 2 + (if (worn) 1 else 0)
                    img(atlas, (if (isFlat) 0 else fr) * d, row * d, d, d, m.wx[wi], m.wy[wi])
                }
            }

            // ---- carroceria
            img(body, 0, 0, w, h, 0, dy)

            // ---- tuning por cima
            val bt = (m.bot - 4 + dy).toFloat()
            if ("metalica" in tuning) {
                // verniz metálico: brilho por cima da pintura (só onde há carroceria) + partículas correndo na lataria
                img(body, 0, 0, w, h, 0, dy, ColorFilter.tint(Color(0x2EFFFFFF), BlendMode.SrcAtop))
                val dx0 = m.door[0]
                val dw = max(4, m.door[2] - m.door[0])
                for (k in 0..3) {
                    val p = (t * 0.7f + k * 0.27f) % 1f
                    val sx = (dx0 + p * dw).roundToInt().toFloat()
                    val sy = (m.door[1] + 1 + (k * 2) % max(2, m.door[3] - m.door[1] - 1) + dy).toFloat()
                    drawRect(Color.White.copy(alpha = 0.85f * (1f - kotlin.math.abs(p - 0.5f) * 1.6f).coerceIn(0f, 1f)), Offset(sx, sy), Size(1f, 1f))
                }
            }
            if ("parachoque" in tuning && m.pack) {
                // pack: lábio dianteiro escuro com friso vermelho e difusor traseiro (escala menor, acompanha a lataria)
                drawRect(Color(0xFF1B1B1B), Offset(m.front - 4f, bt + 1f), Size(6f, 3f))
                drawRect(Color(0xFF424242), Offset(m.front - 3f, bt + 2f), Size(1f, 1f))
                drawRect(Color(0xFFD32F2F), Offset(m.front - 4f, bt + 4f), Size(6f, 1f))
                drawRect(Color(0xFF1B1B1B), Offset(m.rear - 1f, bt + 1f), Size(5f, 3f))
            }
            if ("parachoque" in tuning && !m.pack) {
                // dianteiro: lábio inferior escuro com friso vermelho + entradas de ar; traseiro: difusor
                drawRect(Color(0xFF1B1B1B), Offset(m.front - 6f, bt), Size(8f, 4f))
                drawRect(Color(0xFF424242), Offset(m.front - 6f, bt + 1f), Size(1f, 2f))
                drawRect(Color(0xFF424242), Offset(m.front - 3f, bt + 1f), Size(1f, 2f))
                drawRect(Color(0xFFD32F2F), Offset(m.front - 6f, bt + 4f), Size(8f, 1f))
                drawRect(Color(0xFF1B1B1B), Offset(m.front - 5f, bt + 5f), Size(6f, 1f))
                drawRect(Color(0xFF1B1B1B), Offset(m.rear - 3f, bt), Size(7f, 4f))
                drawRect(Color(0xFF424242), Offset(m.rear - 3f, bt + 4f), Size(7f, 1f))
                drawRect(Color(0xFF1B1B1B), Offset(m.rear - 2f, bt + 5f), Size(5f, 1f))
            }
            if ("farol" in tuning && m.pack && m.headRect.size >= 4) {
                // pack: farol do pack de tunagem (lente azul) esticado na caixa do farol + brilho
                val hrx = m.headRect[0] - 1
                val hry = m.headRect[1] + dy
                val lamp = set.part("lamp2")
                if (lamp != null) {
                    drawImage(
                        lamp, IntOffset(0, 0), IntSize(lamp.width, lamp.height),
                        IntOffset(hrx, hry), IntSize(max(4, m.headRect[2] + 2), max(3, m.headRect[3] + 1)), 1f,
                        androidx.compose.ui.graphics.drawscope.Fill, null, BlendMode.SrcOver, FilterQuality.None,
                    )
                }
                glow(Offset(m.headRect[0] + m.headRect[2].toFloat(), hry + m.headRect[3] / 2f), 6f, Color(0xFF40C4FF).copy(alpha = 0.55f))
            }
            if ("farol" in tuning && !m.pack && m.headRect.size >= 4) {
                // faróis LED personalizados: bloco azul-gelo com faixa de luz de direção (DRL) e brilho
                val hrx = m.headRect[0].toFloat()
                val hry = (m.headRect[1] + dy).toFloat()
                val hrw = max(2, m.headRect[2]).toFloat()
                val hrh = max(2, m.headRect[3]).toFloat()
                drawRect(Color(0xFF0D47A1), Offset(hrx, hry), Size(hrw, hrh))
                drawRect(Color(0xFFB3E5FC), Offset(hrx + 1f, hry), Size(max(1f, hrw - 1f), 1f))
                drawRect(Color(0xFFFFFFFF), Offset(hrx + hrw - 1f, hry + 1f), Size(1f, max(1f, hrh - 1f)))
                glow(Offset(hrx + hrw, hry + hrh / 2f), 6f, Color(0xFF40C4FF).copy(alpha = 0.55f))
            }
            if ("aerofolio" in tuning && m.pack) {
                // pack: aerofólio do pack de tunagem (6 desenhos, 3 tamanhos conforme o comprimento do carro)
                val len = m.front - m.rear
                val sz = intArrayOf(11, 14, 17).minByOrNull { kotlin.math.abs(it - 0.21f * len) } ?: 14
                val sp = set.part("spoil${k % 6}_$sz")
                if (sp != null) {
                    img(sp, 0, 0, sp.width, sp.height, m.rear - 1, m.wing[1] + dy - sp.height + 2)
                }
            }
            if ("aerofolio" in tuning && !m.pack) {
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
            if (("escape" in tuning || "turbo" in tuning) && m.pack) {
                // pack: ponteira/escapamento do pack de tunagem + fumacinha
                val tw = if (m.front - m.rear < 62) 7 else 10
                val tp = set.part("tip${k % 3}_$tw")
                if (tp != null) img(tp, 0, 0, tp.width, tp.height, m.rear - tp.width + 3, m.bot + dy - 2)
                if (speed > 4f && "turbo" !in tuning) {
                    val pp = (t * 2.5f) % 1f
                    drawRect(Color(0xFFBDBDBD).copy(alpha = 0.5f * (1f - pp)), Offset(m.rear - tw - pp * 6f, bt + 2f - pp * 2f), Size(2f, 2f))
                }
            }
            if (("escape" in tuning || "turbo" in tuning) && !m.pack) {
                // ponteira cromada grossa + fumacinha quando o carro anda
                drawRect(Color(0xFFB0BEC5), Offset(m.rear - 3f, bt + 1f), Size(4f, 2f))
                drawRect(Color(0xFF546E7A), Offset(m.rear - 3f, bt + 2f), Size(4f, 1f))
                drawRect(Color(0xFFECEFF1), Offset(m.rear - 3f, bt + 1f), Size(1f, 1f))
                if (speed > 4f && "turbo" !in tuning) {
                    val pp = (t * 2.5f) % 1f
                    drawRect(Color(0xFFBDBDBD).copy(alpha = 0.5f * (1f - pp)), Offset(m.rear - 4f - pp * 6f, bt + 1f - pp * 2f), Size(2f, 2f))
                }
            }
            if ("turbo" in tuning) {
                // entrada de ar (scoop) no capô
                drawRect(Color(0xFF1B1B1B), Offset(m.front - 22f, m.hoodY.toFloat() - 1f), Size(8f, 2f))
                drawRect(Color(0xFF616161), Offset(m.front - 22f, m.hoodY.toFloat() - 1f), Size(8f, 1f))
            }
            if ("turbo" in tuning) {
                val fl = (1.5f + 3.5f * (0.5f + 0.5f * sin(t * 2f * PI.toFloat() * 3f))).roundToInt().toFloat()
                drawRect(Color(0xFF29B6F6), Offset(m.rear - 3f - fl - (if (m.pack) 3f else 0f), bt + 1f), Size(fl, 2f))
                drawRect(Color(0xFFFFF59D), Offset(m.rear - 3f - fl / 2f, bt + 1f), Size(max(1f, fl / 2f), 1f))
                glow(Offset(m.rear - 4f, bt + 2f), 5f, Color(0xFF29B6F6).copy(alpha = 0.5f))
            }

            // ---- piloto da moto (os sprites de moto do pack não trazem piloto)
            if (m.pack && PACK_KIND[k] == "moto") {
                val d0 = driver
                val seatX = m.rear + ((m.front - m.rear) * 0.38f).roundToInt()
                val topY = m.hoodY + dy
                val helmet = Color(0xFFB71C1C)
                val shirt = if (d0 != null) CTOP[d0.top] else Color(0xFF1565C0)
                val skin = if (d0 != null) CSKIN[d0.skin] else Color(0xFFC8966E)
                drawRect(Color(0xFF263238), Offset(seatX.toFloat(), topY + 3f), Size(5f, 5f))               // perna
                drawRect(Color(0xFF263238), Offset(seatX + 4f, topY + 6f), Size(2f, 4f))                    // canela
                drawRect(shirt, Offset(seatX - 1f, topY - 4f), Size(4f, 7f))                                // tronco
                drawRect(shirt, Offset(seatX + 2f, topY - 3f), Size((m.front - seatX - 14).coerceAtLeast(4).toFloat(), 2f)) // braço
                drawRect(skin, Offset(seatX, topY - 7f), Size(3f, 3f))                                      // cabeça
                drawRect(helmet, Offset(seatX - 1f, topY - 8f), Size(4f, 2f))                               // capacete
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
    val m = vmeta(idx.coerceIn(0, V_TOTAL - 1))
    return (m.front - m.rear + 2).toFloat()
}
