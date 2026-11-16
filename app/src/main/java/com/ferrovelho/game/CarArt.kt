package com.ferrovelho.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

private typealias Pt = Pair<Float, Float>

private class BodyShape(val lower: List<Pt>, val cabin: List<Pt>, val wheelR: Float)

private fun shapeOf(b: Body): BodyShape = when (b) {
    Body.HATCH -> BodyShape(
        lower = listOf(6f to 70f, 6f to 52f, 30f to 48f, 150f to 48f, 192f to 56f, 194f to 70f),
        cabin = listOf(30f to 48f, 42f to 28f, 120f to 28f, 148f to 48f),
        wheelR = 11f,
    )
    Body.SEDAN -> BodyShape(
        lower = listOf(6f to 70f, 6f to 52f, 40f to 49f, 150f to 49f, 192f to 57f, 194f to 70f),
        cabin = listOf(44f to 49f, 62f to 29f, 116f to 29f, 144f to 49f),
        wheelR = 11f,
    )
    Body.SUV -> BodyShape(
        lower = listOf(6f to 72f, 6f to 48f, 30f to 44f, 160f to 44f, 192f to 54f, 194f to 72f),
        cabin = listOf(14f to 44f, 20f to 24f, 138f to 24f, 166f to 44f),
        wheelR = 13f,
    )
    Body.PICKUP -> BodyShape(
        lower = listOf(6f to 72f, 6f to 50f, 98f to 50f, 98f to 46f, 160f to 46f, 192f to 56f, 194f to 72f),
        cabin = listOf(98f to 46f, 108f to 26f, 140f to 26f, 166f to 46f),
        wheelR = 13f,
    )
    Body.COUPE -> BodyShape(
        lower = listOf(6f to 70f, 6f to 56f, 30f to 52f, 150f to 52f, 192f to 60f, 194f to 70f),
        cabin = listOf(56f to 52f, 86f to 34f, 124f to 34f, 156f to 52f),
        wheelR = 11f,
    )
    Body.CLASSIC -> BodyShape(
        lower = listOf(10f to 70f, 10f to 56f, 40f to 52f, 150f to 52f, 186f to 60f, 188f to 70f),
        cabin = listOf(36f to 52f, 54f to 30f, 104f to 28f, 140f to 52f),
        wheelR = 11f,
    )
}

private fun DrawScope.poly(pts: List<Pt>, color: Color, sx: Float, sy: Float, outline: Boolean = true) {
    val p = Path()
    pts.forEachIndexed { i, pt ->
        if (i == 0) p.moveTo(pt.first * sx, pt.second * sy) else p.lineTo(pt.first * sx, pt.second * sy)
    }
    p.close()
    drawPath(p, color)
    if (outline) drawPath(p, Color(0x66000000), style = Stroke(width = 2f))
}

private fun inset(pts: List<Pt>, f: Float): List<Pt> {
    val cx = pts.map { it.first }.average().toFloat()
    val cy = pts.map { it.second }.average().toFloat()
    return pts.map { (cx + (it.first - cx) * f) to (cy + (it.second - cy) * f + 1.5f) }
}

private val RUST_SPOTS = listOf(
    30f to 62f, 70f to 58f, 120f to 60f, 160f to 56f, 95f to 64f, 55f to 56f,
)

/** Carro visto de lado. Reage à pintura, ao tuning e ao estado (ferrugem, pneu murcho). */
@Composable
fun CarArt(
    body: Body,
    colorIdx: Int,
    tuning: List<String>,
    cond: List<Int>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val sx = size.width / 200f
        val sy = size.height / 100f
        val shape = shapeOf(body)
        val paint = Color(PAINTS[colorIdx.coerceIn(0, PAINTS.lastIndex)].second)
        val dy = if ("rebaixada" in tuning) 4f else 0f
        val r = shape.wheelR
        val groundY = 72f + r

        // neon
        if ("neon" in tuning) {
            for (i in 0..5) {
                drawRect(
                    color = Color(0xFF00E5FF).copy(alpha = 0.10f + 0.05f * i),
                    topLeft = Offset(20f * sx, (groundY - 3f + i * 1.2f) * sy),
                    size = Size(160f * sx, 1.6f * sy),
                )
            }
        }
        drawLine(Color(0x44FFFFFF), Offset(0f, groundY * sy), Offset(size.width, groundY * sy), strokeWidth = 2f)

        val lower = shape.lower.map { it.first to it.second + dy }
        val cabin = shape.cabin.map { it.first to it.second + dy }

        // cabine e vidros
        poly(cabin, paint, sx, sy)
        val glass = if ("pelicula" in tuning) Color(0xFF1C2A33) else Color(0xFF9EC9E8)
        poly(inset(cabin, 0.80f), glass, sx, sy, outline = false)
        val minY = cabin.minOf { it.second }
        val maxY = cabin.maxOf { it.second }
        val midX = cabin.map { it.first }.average().toFloat()
        drawLine(paint, Offset(midX * sx, (minY + 2f) * sy), Offset(midX * sx, maxY * sy), strokeWidth = 3f * sx)

        // carroceria
        poly(lower, paint, sx, sy)

        // adesivo / faixa
        if ("adesivo" in tuning) {
            drawRect(Color(0xCCFFFFFF), Offset(14f * sx, (59f + dy) * sy), Size(150f * sx, 3f * sy))
        }
        // entrada de ar do turbo
        if ("turbo" in tuning) {
            poly(
                listOf(112f to 50f + dy, 120f to 46f + dy, 140f to 46f + dy, 148f to 50f + dy),
                Color(0x99000000), sx, sy, outline = false,
            )
        }
        // escape
        if ("escape" in tuning) {
            drawRect(Color(0xFF78909C), Offset(0f, (66f + dy) * sy), Size(7f * sx, 3f * sy))
        }
        // ferrugem
        val lataria = cond.getOrElse(1) { 100 }
        if (lataria < 55) {
            val n = ((55 - lataria) / 9 + 1).coerceAtMost(RUST_SPOTS.size)
            for (i in 0 until n) {
                val (x, y) = RUST_SPOTS[i]
                drawCircle(Color(0xFF8D4A1B), (2.5f + i % 3) * sx, Offset(x * sx, (y + dy) * sy))
            }
        }
        // faróis
        drawCircle(Color(0xFFFFF59D), 3.5f * sx, Offset(188f * sx, (58f + dy) * sy))
        drawCircle(Color(0xFFE53935), 3f * sx, Offset(9f * sx, (58f + dy) * sy))

        // rodas
        val flat = cond.getOrElse(2) { 100 } < 30
        val rr = if (flat) r * 0.88f else r
        val cy = 72f + (r - rr)
        val rim = if ("rodas" in tuning) Color(0xFFFFC107) else Color(0xFFB0BEC5)
        for (wx in listOf(46f, 150f)) {
            drawCircle(Color(0xFF1A1A1A), (r + 2.5f) * sx, Offset(wx * sx, 72f * sy))
            drawCircle(Color(0xFF111111), rr * sx, Offset(wx * sx, cy * sy))
            drawCircle(rim, rr * 0.58f * sx, Offset(wx * sx, cy * sy))
            drawCircle(Color(0xFF37474F), rr * 0.20f * sx, Offset(wx * sx, cy * sy))
        }
    }
}
