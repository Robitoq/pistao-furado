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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

/*
 * =====================================================================================
 *  CarArt: o carro mostrado nas telas (garagem, oficina, loja, empresas, encontro...).
 *  Agora é PIXEL ART de verdade (sprites em camadas, ver Vehicles.kt) e usa exatamente o
 *  mesmo desenho do carro que anda na cidade. A escala é inteira quando possível, então
 *  cada pixel do sprite fica com o mesmo tamanho (nada de pixel torto / deformado).
 *
 *  O que mexe no visual:
 *   - cond[1] (Lataria)  : ferrugem, tinta desbotada, massa/primer, amassados, sujeira,
 *                          porta de outra cor, farol quebrado, vidro trincado
 *   - cond[0] (Motor)    : fumaça cinza/preta e faíscas
 *   - cond[2] (Pneus)    : gasto, roda enferrujada, pneu furado
 *   - cond[3] (Interior) : banco rasgado aparecendo pela janela
 *   - cond[4] (Suspensão): traseira arriada
 *   - tuning             : rebaixada, rodas, neon, película, adesivo, escape, turbo, som, couro,
 *                          aerofólio, para-choque
 *
 *  PRA EDITAR A ARTE: rode tools/gen/build_assets.py (gera os PNG + VehicleData.kt).
 * =====================================================================================
 */
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
    val set = remember { CitySprites.get(ctx).veh }
    val paint = Color(PAINTS[colorIdx.coerceIn(0, PAINTS.lastIndex)].second)
    val anim = rememberInfiniteTransition(label = "fx")
    val t by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart), label = "t")
    val driver = remember(driverOverride) { driverOverride ?: loadProfile(ctx.getSharedPreferences("city", android.content.Context.MODE_PRIVATE))?.look }
    val idx = vehicleFor(body)
    Canvas(modifier) {
        try {
            val raw = min(size.width / 72f, size.height / 31f)
            val s = (if (raw >= 2f) floor(raw) else raw).coerceAtLeast(0.5f)
            val ox = ((size.width - 62f * s) / 2f).roundToInt().toFloat()
            val oy = ((size.height - 22f * s) / 2f + 1.5f * s).roundToInt().toFloat()
            withTransform({
                translate(ox, oy)
                scale(s, s, Offset.Zero)
            }) {
                drawVeh(set, idx, paint, tuning, cond, t, driver, 0f, 0f)
            }
        } catch (e: Throwable) {
        }
    }
}
