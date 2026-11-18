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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/*
 * =====================================================================================
 *  CARRO DESENHADO EM VETOR (sem foto / sem sprite)  —  ferro velho de verdade
 * =====================================================================================
 *  Tudo é desenhado no código, numa grade de "unidades" (o carro tem ~62 x 22 unidades,
 *  de lado, virado pra direita). A cor da pintura NÃO usa filtro de imagem: ela vira um
 *  degradê de verdade, então não existe mais cor bugada.
 *
 *  O que mexe no visual:
 *   - cond[1] (Lataria) : ferrugem, tinta desbotada, massa/primer, amassados, sujeira,
 *                         porta de outra cor, para-choque pendurado, vidro trincado
 *   - cond[0] (Motor)   : fumaça cinza/preta e faíscas
 *   - cond[2] (Pneus)   : careca, rodas enferrujadas, pneu furado
 *   - cond[3] (Interior): banco rasgado aparecendo pela janela
 *   - cond[4] (Suspensão): traseira arriada
 *   - tuning            : rebaixada, rodas, neon, película, adesivo, escape, turbo,
 *                         som, couro, aerofólio, para-choque
 *
 *  PRA EDITAR:
 *   - Forma de cada carroceria  -> tabela CAR_SHAPES (lista de pontos x,y)
 *   - Quão destruído fica        -> objeto Wear (números no topo)
 *   - Cores da ferrugem          -> objeto RustColors
 *   - Quanto a rebaixada desce   -> Wear.LOWER
 *   - Novo tuning                -> bloco "TUNING" dentro de drawCarUnits
 * =====================================================================================
 */

/** Ajustes de desgaste. Mexa à vontade. */
object Wear {
    const val RUST_MAX = 34        // máximo de manchas de ferrugem com a lataria no zero
    const val DULL = 0.55f         // quanto a tinta desbota (0 = nada, 1 = tudo)
    const val GRIME = 0.55f        // sujeira/barro embaixo
    const val DENTS_MAX = 6        // máximo de amassados
    const val LOWER = 2.0f         // quanto a rebaixada desce a carroceria (unidades)
    const val SAG_MAX = 3.2f       // inclinação máxima (graus) da suspensão arriada
}

/** Paleta de ferrugem e lataria velha. */
object RustColors {
    val ORANGE = Color(0xFFA14F1E)
    val DARK = Color(0xFF5B2F12)
    val LIGHT = Color(0xFFC9772F)
    val PRIMER = Color(0xFF9AA0A6)
    val FILLER = Color(0xFFB7AE9A)
    val GRIME = Color(0xFF3E2A1A)
    val DULL = Color(0xFF7D766A)
}

/**
 * Forma de uma carroceria. Coordenadas em unidades, y pra baixo, chão em y = 22.
 *  body  : contorno da lataria (x1,y1,x2,y2,...) começando no para-choque de trás e indo no sentido horário.
 *          Os dois últimos pontos têm que ser a frente (x≈61.4, y do farol) e (61.2, 18).
 *  glass : vidros (cada um é um polígono)
 *  doors : x das divisões de porta
 *  head  : canto do rosto do motorista
 */
class CarShape(
    val body: FloatArray,
    val glass: List<FloatArray>,
    val doors: FloatArray,
    val rearX: Float,
    val frontX: Float,
    val r: Float,
    val head: Pair<Float, Float>,
    val bed: Boolean = false,
    val chrome: Boolean = false,
) {
    val topY: Float = run { var m = 99f; var i = 1; while (i < body.size) { m = min(m, body[i]); i += 2 }; m }
    val glassBottom: Float = glass.maxOf { g -> var m = 0f; var i = 1; while (i < g.size) { m = max(m, g[i]); i += 2 }; m }
    val glassTop: Float = glass.minOf { g -> var m = 99f; var i = 1; while (i < g.size) { m = min(m, g[i]); i += 2 }; m }
    val noseY: Float get() = body[body.size - 3]
    val hoodY: Float get() = body[body.size - 5]
    val tailY: Float get() = body[3]
    val deckY: Float get() = body[5]
}

private fun f(vararg v: Float) = v

val CAR_SHAPES: Map<Body, CarShape> = mapOf(
    Body.BOXY to CarShape(
        f(1f, 18f, 0.6f, 9f, 2.5f, 7.8f, 16f, 7.2f, 17f, 3.6f, 41f, 3.6f, 47f, 8f, 59f, 9.6f, 61.6f, 12f, 61.4f, 18f),
        listOf(f(18.6f, 5f, 29f, 5f, 29f, 8.4f, 18.6f, 8.4f), f(30f, 5f, 40.5f, 5f, 45.2f, 8.4f, 30f, 8.4f)),
        f(19f, 29.5f, 45f), 12f, 49f, 4.6f, 33f to 4.9f,
    ),
    Body.COMPACT to CarShape(
        f(1f, 18f, 0.8f, 11f, 3f, 9.5f, 15f, 8.6f, 19f, 4.4f, 38f, 4.2f, 46f, 9f, 58f, 10.2f, 61.4f, 13f, 61.2f, 18f),
        listOf(f(20.2f, 5.6f, 29f, 5.6f, 29f, 9.2f, 20.2f, 9.2f), f(30f, 5.5f, 37.4f, 5.5f, 43.2f, 9.2f, 30f, 9.2f)),
        f(20f, 29.5f, 45f), 12f, 48f, 4.6f, 29.5f to 6f,
    ),
    Body.WAGON to CarShape(
        f(1f, 18f, 0.6f, 8f, 2f, 6f, 36f, 4.4f, 44f, 9f, 58f, 10.4f, 61.4f, 13f, 61.2f, 18f),
        listOf(f(3.4f, 5.8f, 16f, 5.8f, 16f, 9.2f, 3.4f, 9.2f), f(17f, 5.7f, 27f, 5.7f, 27f, 9.2f, 17f, 9.2f), f(28f, 5.6f, 35f, 5.6f, 41.6f, 9.2f, 28f, 9.2f)),
        f(16.5f, 27.5f, 44f), 12f, 49f, 4.6f, 29f to 6f,
    ),
    Body.MPV to CarShape(
        f(1f, 18f, 0.6f, 8f, 4f, 4f, 38f, 3.4f, 48f, 10f, 59f, 11.5f, 61.4f, 14f, 61.2f, 18f),
        listOf(f(5f, 5f, 22f, 5f, 22f, 9.6f, 5f, 9.6f), f(23f, 4.8f, 37f, 4.8f, 44f, 9.6f, 23f, 9.6f)),
        f(22.5f, 46f), 12f, 49f, 4.6f, 32f to 5.4f,
    ),
    Body.HATCH to CarShape(
        f(1f, 18f, 0.8f, 11.5f, 4f, 9.6f, 18f, 5.2f, 36f, 4.6f, 45f, 9.2f, 58f, 10.6f, 61.4f, 13.2f, 61.2f, 18f),
        listOf(f(17.6f, 6.2f, 28.5f, 6f, 28.5f, 9.4f, 11f, 9.4f), f(29.5f, 6f, 36f, 5.8f, 42f, 9.4f, 29.5f, 9.4f)),
        f(19f, 29f, 45f), 12f, 49f, 4.6f, 31f to 6.2f,
    ),
    Body.SEDAN to CarShape(
        f(1f, 18f, 0.6f, 11.4f, 5f, 10f, 15f, 9.6f, 21f, 5f, 38f, 4.8f, 46f, 9.4f, 58f, 10.6f, 61.4f, 13f, 61.2f, 18f),
        listOf(f(22.4f, 6.2f, 30.5f, 6.1f, 30.5f, 9.4f, 20f, 9.4f), f(31.5f, 6.1f, 37f, 6f, 43f, 9.4f, 31.5f, 9.4f)),
        f(21f, 31f, 46f), 13f, 50f, 4.6f, 32f to 6.2f,
    ),
    Body.SUV to CarShape(
        f(1f, 18f, 0.6f, 7f, 3f, 4.2f, 40f, 4f, 48f, 9f, 59f, 10.4f, 61.4f, 13f, 61.2f, 18f),
        listOf(f(4.6f, 5.4f, 21f, 5.3f, 21f, 9.2f, 4.6f, 9.2f), f(22f, 5.3f, 39f, 5.2f, 45f, 9.2f, 22f, 9.2f)),
        f(21.5f, 46f), 12f, 49f, 5f, 32f to 5.4f,
    ),
    Body.PICKUP to CarShape(
        f(1f, 18f, 0.6f, 10.4f, 26f, 10.4f, 27f, 6f, 30f, 4.4f, 46f, 4.4f, 52f, 9.4f, 59f, 10.6f, 61.4f, 13f, 61.2f, 18f),
        listOf(f(31f, 5.6f, 45f, 5.6f, 49.4f, 9.2f, 31f, 9.2f)),
        f(30f, 52f), 11f, 50f, 4.8f, 37f to 5.8f, bed = true,
    ),
    Body.COUPE to CarShape(
        f(1f, 18f, 0.6f, 12.5f, 5f, 11.4f, 22f, 6.8f, 34f, 5.6f, 45f, 10.2f, 58f, 11.6f, 61.4f, 14f, 61.2f, 18f),
        listOf(f(23f, 7.6f, 33.6f, 6.6f, 42f, 10.2f, 22f, 10.2f)),
        f(24f, 44f), 13f, 50f, 4.7f, 30f to 7.2f,
    ),
    Body.CLASSIC to CarShape(
        f(1f, 18f, 0.6f, 11f, 4f, 9.6f, 14f, 9.2f, 19f, 4.8f, 38f, 4.6f, 44f, 9f, 56f, 9.6f, 61.4f, 12.4f, 61.2f, 18f),
        listOf(f(20.4f, 5.9f, 29.6f, 5.8f, 29.6f, 9.2f, 17f, 9.2f), f(30.6f, 5.8f, 37f, 5.7f, 41.6f, 9.2f, 30.6f, 9.2f)),
        f(18f, 30f, 46f), 13f, 49f, 4.6f, 31.5f to 6f, chrome = true,
    ),
    Body.VAN to CarShape(   // Kombi: frente reta, teto chato e parabrisa quase em pé
        f(1f, 18f, 0.4f, 6f, 1.4f, 3.6f, 3f, 2.8f, 55f, 2.8f, 59.5f, 5f, 60.8f, 10f, 61.4f, 12.6f, 61.2f, 18f),
        listOf(f(4.6f, 4.4f, 16.6f, 4.4f, 16.6f, 9f, 4.6f, 9f), f(17.6f, 4.4f, 29.6f, 4.4f, 29.6f, 9f, 17.6f, 9f), f(30.6f, 4.4f, 52f, 4.4f, 58.2f, 9.2f, 30.6f, 9.2f)),
        f(17.5f, 30.5f, 52f), 12f, 50f, 4.4f, 45f to 5.6f, chrome = true,
    ),
    Body.BEETLE to CarShape(
        f(1f, 18f, 1f, 13f, 6f, 9f, 20f, 5.2f, 34f, 5f, 46f, 9.4f, 58f, 12f, 61.4f, 14.5f, 61.2f, 18f),
        listOf(f(21f, 6.4f, 33f, 6.2f, 33f, 9.6f, 14f, 9.6f), f(34f, 6.2f, 38f, 6.4f, 43.6f, 9.8f, 34f, 9.8f)),
        f(22f, 46f), 13f, 49f, 4.6f, 30f to 6.6f, chrome = true,
    ),
    Body.UNO to CarShape(   // Uno: bem quadradão, traseira reta e teto alto
        f(1f, 18f, 0.6f, 10f, 1.2f, 7.6f, 3.2f, 3.8f, 37f, 3.6f, 45.5f, 9.2f, 58f, 10.6f, 61.4f, 13f, 61.2f, 18f),
        listOf(f(5f, 4.9f, 17.5f, 4.9f, 17.5f, 9f, 5f, 9f), f(18.5f, 4.9f, 36f, 4.8f, 42f, 9f, 18.5f, 9f)),
        f(18f, 44f), 11f, 49f, 4.4f, 29f to 5.6f,
    ),
    Body.GOL to CarShape(   // Gol: porta-malas inclinado, capô reto e frente vertical
        f(1f, 18f, 0.6f, 11f, 3f, 9.4f, 16f, 5f, 37f, 4.4f, 45f, 9.2f, 59f, 10.2f, 61.4f, 12.6f, 61.2f, 18f),
        listOf(f(16.5f, 6f, 28f, 5.8f, 28f, 9.2f, 10.5f, 9.2f), f(29f, 5.8f, 37f, 5.6f, 42.4f, 9.2f, 29f, 9.2f)),
        f(19f, 29f, 45f), 12f, 49f, 4.5f, 31f to 6f,
    ),
    Body.NOTCH to CarShape(   // Jetta: sedã de 3 volumes, porta-malas alto e capô longo
        f(1f, 18f, 0.6f, 10.6f, 2f, 9f, 17f, 8.8f, 23f, 4.8f, 37f, 4.6f, 46f, 9f, 59f, 9.8f, 61.4f, 11.8f, 61.2f, 18f),
        listOf(f(23.4f, 5.9f, 31f, 5.8f, 31f, 9f, 21f, 9f), f(32f, 5.8f, 37f, 5.7f, 44f, 9f, 32f, 9f)),
        f(21f, 31f, 46f), 13f, 50f, 4.7f, 32f to 6f,
    ),
    Body.OPALA to CarShape(   // Opala: sedã longo e baixo, cromado
        f(1f, 18f, 0.6f, 11f, 3f, 9.4f, 18f, 9.2f, 25f, 5.2f, 37f, 5f, 47f, 9.2f, 59f, 9.8f, 61.4f, 12f, 61.2f, 18f),
        listOf(f(25.4f, 6.2f, 33f, 6.1f, 33f, 9.2f, 22f, 9.2f), f(34f, 6.1f, 38f, 6f, 44.6f, 9.2f, 34f, 9.2f)),
        f(22f, 33.5f, 47f), 13f, 49f, 4.8f, 33f to 6.4f, chrome = true,
    ),
    Body.MUSCLE to CarShape(   // muscle car: capô comprido, cabine recuada e traseira curta
        f(1f, 18f, 0.6f, 11.4f, 2f, 9.4f, 14f, 9f, 23f, 6.2f, 33f, 5.8f, 41f, 9.6f, 59f, 10.2f, 61.4f, 12.6f, 61.2f, 18f),
        listOf(f(23.6f, 7f, 32.6f, 6.8f, 32.6f, 9.4f, 17f, 9.4f), f(33.6f, 6.8f, 35f, 6.7f, 40f, 9.4f, 33.6f, 9.4f)),
        f(24f, 40f), 13f, 50f, 5f, 29f to 7.4f,
    ),
    Body.P911 to CarShape(   // 911: teto em arco descendo até a traseira
        f(1f, 18f, 0.6f, 12.2f, 3f, 10.4f, 14f, 8f, 28f, 5.8f, 34f, 5.8f, 43f, 10f, 58f, 11.2f, 61.4f, 13.4f, 61.2f, 18f),
        listOf(f(17f, 8f, 28.2f, 6.6f, 34f, 6.6f, 40.4f, 9.6f, 17f, 9.6f)),
        f(26f, 42f), 13f, 50f, 4.9f, 30f to 7f,
    ),
    Body.JEEP to CarShape(   // Jeep: tudo reto, parabrisa em pé e capô plano
        f(1f, 18f, 0.6f, 8.4f, 2f, 6.4f, 4f, 4.6f, 36f, 4.6f, 38f, 8.2f, 55f, 8.6f, 61.4f, 11.6f, 61.2f, 18f),
        listOf(f(6f, 5.6f, 20f, 5.6f, 20f, 8.2f, 6f, 8.2f), f(21f, 5.6f, 35.6f, 5.6f, 37.6f, 8.2f, 21f, 8.2f)),
        f(20.5f, 36f), 12f, 50f, 5.4f, 29f to 6f,
    ),
    Body.MINI to CarShape(   // Mini: curtinho, teto alto e frente reta
        f(1f, 18f, 0.6f, 10.5f, 1.4f, 8f, 4f, 4.6f, 38f, 4.4f, 44f, 8.8f, 56f, 9.6f, 60.6f, 10.4f, 61.4f, 12.8f, 61.2f, 18f),
        listOf(f(6f, 5.4f, 24f, 5.4f, 24f, 8.8f, 6f, 8.8f), f(25f, 5.4f, 38f, 5.3f, 42.6f, 8.8f, 25f, 8.8f)),
        f(24.5f, 44f), 11.5f, 49f, 4.8f, 31f to 6f,
    ),
    Body.SUPER to CarShape(
        f(1f, 18f, 0.6f, 14f, 8f, 12f, 26f, 8f, 38f, 7f, 50f, 11.4f, 61.4f, 14.2f, 61.2f, 18f),
        listOf(f(27f, 8.9f, 37.4f, 8f, 45f, 11.4f, 26f, 11.4f)),
        f(28f, 47f), 13f, 50f, 4.8f, 31f to 8.6f,
    ),
)

fun shapeFor(b: Body): CarShape = CAR_SHAPES[b] ?: CAR_SHAPES.getValue(Body.HATCH)

private fun poly(p: FloatArray): Path = Path().apply {
    moveTo(p[0], p[1])
    var i = 2
    while (i < p.size - 1) { lineTo(p[i], p[i + 1]); i += 2 }
    close()
}

@Composable
fun CarArt(
    body: Body,
    colorIdx: Int,
    tuning: List<String>,
    cond: List<Int>,
    modifier: Modifier = Modifier,
    driverOverride: Look? = null,
) {
    val ctx = LocalContext.current
    val sh = remember(body) { shapeFor(body) }
    val paint = Color(PAINTS[colorIdx.coerceIn(0, PAINTS.lastIndex)].second)
    val anim = rememberInfiniteTransition(label = "fx")
    val t by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart), label = "t")
    val driver = remember(driverOverride) { driverOverride ?: loadProfile(ctx.getSharedPreferences("city", android.content.Context.MODE_PRIVATE))?.look }
    val seed = body.ordinal * 17 + colorIdx * 5 + 3
    Canvas(modifier) {
        try {
            val s = min(size.width / 72f, size.height / 31f).coerceAtLeast(0.5f)
            val ox = (size.width - 62f * s) / 2f
            val oy = (size.height - 22f * s) / 2f + 1.5f * s
            withTransform({
                translate(ox, oy)
                scale(s, s, Offset.Zero)
            }) {
                drawCarUnits(sh, seed, paint, tuning, cond, t, driver)
            }
        } catch (e: Throwable) {
        }
    }
}

private fun DrawScope.drawCarUnits(
    sh: CarShape, seed: Int, paint: Color, tuning: List<String>, cond: List<Int>, t: Float, driver: Look?,
) {
    fun part(i: Int) = cond.getOrElse(i) { 100 }
    val motor = part(0); val lat = part(1); val tire = part(2); val inter = part(3); val susp = part(4)
    val sev = ((100 - lat) / 100f).coerceIn(0f, 1f)          // 0 = lataria nova, 1 = destruída
    val flat = tire < 25
    val sagK = max(0, 65 - susp) / 65f
    val sag = sagK * Wear.SAG_MAX + (if (flat) 2f else 0f)
    val dy = (if ("rebaixada" in tuning) Wear.LOWER else 0f) + sagK * 0.8f
    val wheelY = 17.2f
    val rnd = Random(seed)

    // cores da pintura (degradê de verdade; envelhece com a lataria)
    val paintC = lerp(paint, RustColors.DULL, sev * Wear.DULL)
    val hi = lerp(paintC, Color.White, 0.24f)
    val lo = lerp(paintC, Color.Black, 0.36f)
    val bodyBrush = Brush.verticalGradient(0f to hi, 0.4f to paintC, 1f to lo, startY = sh.topY, endY = 18.5f)
    val bodyPath = poly(sh.body)

    // sombra no chão + neon
    drawOval(Color(0x66000000), Offset(1.5f, 20.4f), Size(59f, 3.2f))
    if ("neon" in tuning) {
        glow(Offset(31f, 21.6f), 30f, Color(0xFF00E5FF).copy(alpha = 0.55f))
        drawRect(Color(0xFF9CF3FF), Offset(6f, 21.4f), Size(50f, 0.6f))
    }

    withTransform({ rotate(-sag, Offset(51f, 22f)) }) {

        // ============================ CARROCERIA ============================
        withTransform({ translate(0f, dy) }) {
            drawPath(bodyPath, bodyBrush)
            drawPath(bodyPath, bodyBrush, style = Stroke(width = 1.3f, join = StrokeJoin.Round))
            drawPath(bodyPath, Color(0x99000000), style = Stroke(width = 0.35f, join = StrokeJoin.Round))

            clipPath(bodyPath) {
                // soleira / parte de baixo mais escura
                drawRect(Color(0x33000000), Offset(0f, 15.4f), Size(63f, 4f))
                drawRect(Color(0x66000000), Offset(0f, 17.6f), Size(63f, 0.6f))
                // vinco de luz na lateral
                drawRect(Color.White.copy(alpha = 0.16f * (1f - sev)), Offset(0f, sh.hoodY + 1.6f), Size(63f, 0.35f))

                // divisões de porta + maçanetas
                val gb = sh.glassBottom
                sh.doors.forEachIndexed { i, x ->
                    drawLine(Color(0x73000000), Offset(x, gb + 0.2f), Offset(x, 17.6f), 0.28f)
                    if (i < sh.doors.size - 1) drawRect(Color(0x99000000), Offset(x + 1.4f, gb + 1.5f), Size(2.4f, 0.55f))
                }

                // ----- porta trocada (de outra cor) quando já está bem judiada
                if (sev > 0.5f && sh.doors.size >= 2) {
                    val x0 = sh.doors[0]; val x1 = sh.doors[1]
                    drawRect(RustColors.FILLER.copy(alpha = 0.92f), Offset(x0 + 0.15f, gb + 0.3f), Size(x1 - x0 - 0.3f, 17.3f - gb))
                    drawRect(Color(0x55000000), Offset(x0 + 0.15f, 16.6f), Size(x1 - x0 - 0.3f, 0.7f))
                }

                // ----- sujeira embaixo
                if (sev > 0.05f) {
                    drawRect(
                        Brush.verticalGradient(listOf(Color.Transparent, RustColors.GRIME.copy(alpha = sev * Wear.GRIME)), startY = 10.5f, endY = 18.6f),
                        Offset(0f, 0f), Size(63f, 20f),
                    )
                }

                // ----- ferrugem
                val rustN = (sev * Wear.RUST_MAX).toInt()
                repeat(rustN) { i ->
                    val z = rnd.nextFloat()
                    val px: Float; val py: Float
                    when {
                        z < 0.38f -> { px = 1f + rnd.nextFloat() * 60f; py = 15.2f + rnd.nextFloat() * 3f }                    // soleira
                        z < 0.66f -> {                                                                                         // ao redor das rodas
                            val cx = if (rnd.nextBoolean()) sh.rearX else sh.frontX
                            val a = rnd.nextFloat() * PI.toFloat()
                            px = cx + cos(a + PI.toFloat()) * (sh.r + 1.2f); py = wheelY + sin(a + PI.toFloat()) * (sh.r + 1.2f)
                        }
                        z < 0.82f -> { px = 4f + rnd.nextFloat() * 54f; py = sh.hoodY + rnd.nextFloat() * 2f }                // capô/teto/porta-malas
                        else -> { px = 1f + rnd.nextFloat() * 60f; py = sh.glassBottom + rnd.nextFloat() * 6f }               // lateral
                    }
                    val rad = (0.8f + rnd.nextFloat() * 2.3f) * (0.6f + sev)
                    val base = when (i % 4) { 0 -> RustColors.ORANGE; 1 -> RustColors.DARK; 2 -> RustColors.LIGHT; else -> RustColors.ORANGE }
                    drawOval(base.copy(alpha = 0.50f + 0.4f * sev), Offset(px - rad, py - rad * 0.6f), Size(rad * 2f, rad * 1.2f))
                    drawOval(RustColors.DARK.copy(alpha = 0.55f), Offset(px - rad * 0.45f, py - rad * 0.3f), Size(rad * 0.9f, rad * 0.6f))
                    if (i % 3 == 0) drawLine(RustColors.ORANGE.copy(alpha = 0.55f), Offset(px, py + rad * 0.4f), Offset(px + rnd.nextFloat() * 0.6f - 0.3f, py + rad * 0.4f + 1.4f + rnd.nextFloat() * 2.6f), 0.3f)
                }

                // ----- tinta saiu: massa / primer cinza
                if (sev > 0.4f) {
                    repeat(((sev - 0.3f) * 9f).toInt()) {
                        val px = 3f + rnd.nextFloat() * 56f
                        val py = sh.glassBottom + 0.5f + rnd.nextFloat() * 5.5f
                        val ww = 1.4f + rnd.nextFloat() * 3.2f
                        drawOval((if (it % 2 == 0) RustColors.PRIMER else RustColors.FILLER).copy(alpha = 0.85f), Offset(px, py), Size(ww, ww * 0.5f))
                    }
                }

                // ----- amassados
                if (sev > 0.3f) {
                    repeat(min(Wear.DENTS_MAX, ((sev - 0.2f) * 7f).toInt())) {
                        val px = 4f + rnd.nextFloat() * 54f
                        val py = sh.glassBottom + 1f + rnd.nextFloat() * 5f
                        val rw = 1.6f + rnd.nextFloat() * 2.4f
                        drawOval(Color(0x30000000), Offset(px, py), Size(rw * 2f, rw))
                        drawOval(Color(0x40FFFFFF), Offset(px + 0.3f, py - 0.35f), Size(rw * 1.6f, rw * 0.4f))
                    }
                }

                // ----- riscos
                if (sev > 0.12f) {
                    repeat(((sev + 0.1f) * 6f).toInt()) {
                        val px = 3f + rnd.nextFloat() * 54f
                        val py = sh.glassBottom + 1f + rnd.nextFloat() * 6f
                        drawLine(Color(0x44FFFFFF), Offset(px, py), Offset(px + 2f + rnd.nextFloat() * 4f, py + rnd.nextFloat() * 1.2f - 0.6f), 0.2f)
                    }
                }

                // ----- adesivo / faixa (tuning)
                if ("adesivo" in tuning) {
                    drawRect(Color(0xFFE53935), Offset(0f, sh.hoodY + 3.4f), Size(63f, 1.2f))
                    drawRect(Color.White, Offset(0f, sh.hoodY + 4.8f), Size(63f, 0.45f))
                }

                // ----- caixa da picape
                if (sh.bed) {
                    drawRect(Color(0xFF1B1B1B), Offset(1f, 10.4f), Size(24.5f, 1.5f))
                    drawRect(Color(0x33FFFFFF), Offset(1f, 10.4f), Size(24.5f, 0.3f))
                }

                // ----- arcos das rodas (recorte escuro, acompanha a rebaixada)
                listOf(sh.rearX, sh.frontX).forEach { cx ->
                    drawCircle(Color(0xFF0E0F12), sh.r + 1.3f, Offset(cx, wheelY - dy))
                    drawCircle(
                        if (sev > 0.3f) lerp(paintC, RustColors.ORANGE, (sev + 0.2f).coerceAtMost(1f)) else paintC,
                        sh.r + 1.3f, Offset(cx, wheelY - dy), style = Stroke(width = 0.5f),
                    )
                }
            }

            // ============================ VIDROS ============================
            val tint = "pelicula" in tuning
            sh.glass.forEach { gp ->
                val gpath = poly(gp)
                drawPath(gpath, Brush.verticalGradient(listOf(Color(0xFF3B4E5C), Color(0xFF16222C)), startY = sh.glassTop, endY = sh.glassBottom))
                clipPath(gpath) {
                    // banco (aparece pela janela)
                    val hx = sh.head.first; val hy = sh.head.second
                    val seatX = hx - 5.8f; val seatY = hy + 1.4f
                    when {
                        "couro" in tuning -> {
                            drawRect(Color(0xFF6D4C41), Offset(seatX, seatY), Size(2.8f, 7f))
                            drawRect(Color(0xFF8D6E63), Offset(seatX, seatY), Size(2.8f, 0.9f))
                        }
                        inter < 45 -> {
                            drawRect(Color(0xFF6E6E6E), Offset(seatX, seatY), Size(2.8f, 7f))
                            drawRect(Color(0xFFF2E6C9), Offset(seatX + 0.5f, seatY + 1f), Size(1.6f, 1.5f))   // espuma saindo
                            drawRect(Color(0xFF2B2B2B), Offset(seatX + 1.4f, seatY + 3f), Size(1f, 1.2f))     // rasgo
                        }
                        else -> drawRect(Color(0xFF37474F), Offset(seatX, seatY), Size(2.8f, 7f))
                    }
                    // motorista
                    if (driver != null) {
                        val dark = Color(0xFF212121)
                        drawRect(CTOP[driver.top], Offset(hx - 0.6f, hy + 3.4f), Size(5f, 4.5f))
                        drawRect(CSKIN[driver.skin], Offset(hx, hy), Size(3.6f, 3.6f))
                        if (driver.hairStyle != 3) drawRect(CHAIR[driver.hair], Offset(hx - 0.2f, hy - 0.7f), Size(4f, 1.6f))
                        drawRect(dark, Offset(hx + 2.3f, hy + 1.5f), Size(0.7f, 0.9f))
                        if (driver.glasses) drawRect(dark, Offset(hx + 1.8f, hy + 1.3f), Size(2f, 0.35f))
                    }
                    if (tint) drawRect(Color(0xD005080F), Offset(0f, 0f), Size(63f, 20f))
                    // reflexo
                    drawLine(Color.White.copy(alpha = 0.14f), Offset(gp[0] + 2f, sh.glassBottom), Offset(gp[0] + 5f, sh.glassTop), 0.9f)
                }
                drawPath(gpath, Color(0xAA000000), style = Stroke(width = 0.3f))
            }
            // vidro trincado
            if (sev > 0.55f) {
                val lg = sh.glass.last()
                val cx = lg[2] - 2.6f; val cy = sh.glassTop + 2.2f
                for (d in listOf(Offset(3.6f, 3.2f), Offset(-3f, 3.4f), Offset(1.4f, 4.4f), Offset(-1f, -1.6f), Offset(4f, 0.6f))) {
                    drawLine(Color(0xCCFFFFFF), Offset(cx, cy), Offset(cx + d.x, cy + d.y), 0.2f)
                }
            }

            // ============================ FARÓIS / LANTERNAS ============================
            val noseY = sh.noseY
            val brokenLamp = sev > 0.65f
            drawRect(if (brokenLamp) Color(0xFF3A3A3A) else Color(0xFFFFF59D), Offset(58.6f, noseY + 0.2f), Size(2.8f, 1.9f))
            if (brokenLamp) drawLine(Color(0xFFBDBDBD), Offset(58.8f, noseY + 0.3f), Offset(61.2f, noseY + 2f), 0.2f)
            drawRect(Color(0xFFB71C1C).copy(alpha = 1f - sev * 0.35f), Offset(0.4f, sh.tailY + 1.2f), Size(1.8f, 2.4f))

            // ============================ PARA-CHOQUES ============================
            val chromeC = if (sh.chrome) lerp(Color(0xFFCFD8DC), Color(0xFF8A5A3A), sev * 0.9f) else Color(0xFF2A2C30)
            // dianteiro (pendurado quando está destruído)
            withTransform({ if (sev > 0.8f) rotate(9f, Offset(60.5f, 15.8f)) }) {
                drawRect(chromeC, Offset(58.4f, 15.7f), Size(4.4f, 2.3f))
                if (sev > 0.4f) drawRect(RustColors.ORANGE.copy(alpha = 0.7f), Offset(59f, 16.6f), Size(2.4f, 0.8f))
            }
            // traseiro
            drawRect(chromeC, Offset(-1.6f, 15.7f), Size(4f, 2.3f))
            if (sev > 0.4f) drawRect(RustColors.ORANGE.copy(alpha = 0.7f), Offset(-0.8f, 16.6f), Size(2.4f, 0.8f))

            // ============================ TUNING ============================
            if ("parachoque" in tuning) {
                drawRect(Color(0xFF1B1B1B), Offset(57.8f, 15.2f), Size(5.4f, 2.9f))
                drawRect(Color(0xFF424242), Offset(57.4f, 18f), Size(5.8f, 0.7f))
                drawRect(Color(0xFF1B1B1B), Offset(-2.2f, 15.2f), Size(5f, 2.9f))
            }
            if ("aerofolio" in tuning) {
                val d = sh.deckY
                drawRect(Color(0xFF1B1B1B), Offset(1.4f, d - 1.6f), Size(1.1f, 1.8f))
                drawRect(Color(0xFF1B1B1B), Offset(8f, d - 1.6f), Size(1.1f, 1.8f))
                drawRect(Color(0xFF111111), Offset(-1.8f, d - 4.2f), Size(11.8f, 2.4f))
                drawRect(Color(0xFF616161), Offset(-1.8f, d - 4.2f), Size(11.8f, 0.6f))
                drawRect(Color(0xFFE53935), Offset(-1.8f, d - 4.6f), Size(0.9f, 3.2f))
                drawRect(Color(0xFFE53935), Offset(9.1f, d - 4.6f), Size(0.9f, 3.2f))
            }
            if ("escape" in tuning || "turbo" in tuning) {
                drawRect(Color(0xFFB0BEC5), Offset(-2.6f, 16.3f), Size(3.4f, 1.5f))
                drawRect(Color(0xFF546E7A), Offset(-2.6f, 17.4f), Size(3.4f, 0.4f))
            }
            if ("turbo" in tuning) {
                val fl = 1.5f + 3.5f * (0.5f + 0.5f * sin(t * 2f * PI.toFloat() * 3f))
                drawRect(Color(0xFF29B6F6), Offset(-2.6f - fl, 16.5f), Size(fl, 1.1f))
                drawRect(Color(0xFFFFF59D), Offset(-2.6f - fl * 0.5f, 16.7f), Size(fl * 0.5f, 0.7f))
                glow(Offset(-4f, 17.1f), 5f, Color(0xFF29B6F6).copy(alpha = 0.5f))
            }
        }

        // ============================ RODAS / PNEUS ============================
        val sport = "rodas" in tuning
        val tireC = if (tire < 55) Color(0xFF3F4246) else Color(0xFF15161A)
        val rimC = when {
            sport -> Color(0xFFFFC107)
            sev > 0.5f -> Color(0xFF7A5A44)             // calota enferrujada
            else -> Color(0xFF8E9399)
        }
        listOf(sh.rearX, sh.frontX).forEachIndexed { idx, cx ->
            val rad = sh.r
            val isFlat = flat && idx == 0
            val c = Offset(cx, wheelY)
            if (isFlat) {
                drawOval(tireC, Offset(cx - rad * 1.12f, wheelY + rad * 0.28f - rad * 0.78f), Size(rad * 2.24f, rad * 1.56f))
            } else drawCircle(tireC, rad, c)
            val rc = if (isFlat) Offset(cx, wheelY + rad * 0.3f) else c
            val rr = rad * (if (isFlat) 0.5f else if (sport) 0.74f else 0.62f)
            drawCircle(rimC, rr, rc)
            if (sport) drawCircle(Color(0xFF3A2E00), rr * 0.74f, rc)
            val n = if (sport) 6 else 5
            for (j in 0 until n) {
                val a = 0.4f + j * (2f * PI.toFloat() / n)
                drawLine(
                    if (sport) Color(0xFFFFE082) else Color(0xFF4A4F57), rc,
                    Offset(rc.x + cos(a) * rr * 0.9f, rc.y + sin(a) * rr * 0.9f), rr * 0.16f,
                )
            }
            drawCircle(if (sport) Color(0xFF5D4B00) else Color(0xFFD7DBE0), rr * 0.2f, rc)
            if (tire < 55 && !isFlat) drawCircle(Color(0x55FFFFFF), rad, c, style = Stroke(width = 0.4f))   // careca
        }

        // ============================ MOTOR: FUMAÇA / FAÍSCAS ============================
        if (motor < 45) {
            val smokeX = 52f
            val smokeY = sh.hoodY - 0.8f
            val black = motor < 25
            val puffs = if (black) 5 else 3
            for (i in 0 until puffs) {
                val p = (t + i / puffs.toFloat()) % 1f
                val col = if (black) Color(0xFF1B1B1B) else Color(0xFFBDBDBD)
                drawCircle(col.copy(alpha = 0.6f * (1f - p)), 1.1f + p * 2.4f,
                    Offset(smokeX + sin((p + i) * 5f) * 1.2f, smokeY - p * 10f))
            }
            if (motor < 12 && ((t * 6f).toInt() % 2) == 0) {
                drawRect(Color(0xFFFFEB3B), Offset(smokeX + 1f, smokeY + 0.4f), Size(0.8f, 0.8f))
                drawRect(Color(0xFFFF9800), Offset(smokeX + 3f, smokeY - 0.4f), Size(0.8f, 0.8f))
            }
        }

        // ============================ SOM POTENTE ============================
        if ("som" in tuning) {
            for (i in 0..2) {
                val p = (t * 1.5f + i / 3f) % 1f
                val rad = 3f + p * 8f
                drawArc(
                    Color.White.copy(alpha = 0.7f * (1f - p)), -50f, 100f, false,
                    Offset(34f - rad, sh.glassTop + 1f - rad), Size(rad * 2f, rad * 2f),
                    style = Stroke(width = 0.5f),
                )
            }
        }
    }
}
