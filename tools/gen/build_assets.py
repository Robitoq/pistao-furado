import os, glob, shutil
from px import *
from carlib import render_car
from models import *
from wheels import make_atlas
from props import *
from scenery import build_all, lit_overlay

PROJ = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..'))
ASSET = f'{PROJ}/app/src/main/assets/city'
KT = f'{PROJ}/app/src/main/java/com/ferrovelho/game/VehicleData.kt'

# limpa o que era de terceiros / será substituído
for f in glob.glob(f'{ASSET}/*.png'): os.remove(f)

allspecs = {s['name']: s for s in HEROES + GENERIC + [TAXI]}
names = list(ORDER) + ['moto', 'moto_delivery', 'truck_bau', 'truck_wood', 'bus']
metas = []
for i, n in enumerate(names):
    if n == 'moto': L, I, m = moto(False)
    elif n == 'moto_delivery': L, I, m = moto(True)
    elif n == 'truck_bau': L, I, m = render_car(truck('bau'))
    elif n == 'truck_wood': L, I, m = render_car(truck('wood'))
    elif n == 'bus': L, I, m = render_car(bus())
    else: L, I, m = render_car(allspecs[n])
    L.save(f'{ASSET}/vb{i}.png'); I.save(f'{ASSET}/vi{i}.png')
    m['name'] = n; metas.append(m)
for D in (9, 11, 13): make_atlas(D).save(f'{ASSET}/vw{D}.png')

def ia(v): return 'intArrayOf(' + ', '.join(str(int(x)) for x in v) + ')'
lines = ['package com.ferrovelho.game', '', '// GERADO por tools/gen/build_assets.py — não edite à mão (as medidas vêm da arte).', '',
 'class VMeta(',
 '    val name: String, val w: Int, val h: Int, val cx: Int, val gy: Int, val d: Int,',
 '    val wx: IntArray, val wy: IntArray, val archR: Float, val bot: Int,',
 '    val head: IntArray, val glass: IntArray, val door: IntArray, val wing: IntArray, val exhaust: IntArray,',
 '    val front: Int, val rear: Int, val hl: IntArray, val tl: IntArray, val headRect: IntArray, val tailRect: IntArray,',
 '    val hoodY: Int, val cab: Boolean,',
 '    val pack: Boolean = false,   // true = sprite dos packs (ver PackData.kt / Vehicles.kt)',
 ')', '', 'const val V_TAXI = 21', 'const val V_MOTO = 22', 'const val V_MOTO_ENTREGA = 23', 'const val V_TRUCK_BAU = 24',
 'const val V_TRUCK_MADEIRA = 25', 'const val V_BUS = 26', 'const val V_COUNT = 27', 'const val V_GARAGE = 21', '',
 'val VMETA: List<VMeta> = listOf(']
for m in metas:
    wx = [w[0] for w in m['wheels']]; wy = [w[1] for w in m['wheels']]
    hr = m.get('head_rect') or (0, 0, 0, 0); tr = m.get('tail_rect') or (0, 0, 0, 0)
    lines.append(f'    VMeta("{m["name"]}", {m["W"]}, {m["H"]}, {m["cx"]}, {m["gy"]}, {m["D"]}, {ia(wx)}, {ia(wy)}, {float(m["arch_r"])}f, {m["bot"]},')
    lines.append(f'        {ia(m["head_xy"])}, {ia(m["glass_box"])}, {ia(m["door"])}, {ia(m["wing"])}, {ia(m["exhaust"])},')
    lines.append(f'        {m["front_x"]}, {m["rear_x"]}, {ia(m["hl"])}, {ia(m["tl"])}, {ia(hr)}, {ia(tr)}, {m["hood_y"]}, {"true" if m["cab"] else "false"}),')
lines.append(')')
open(KT, 'w').write('\n'.join(lines) + '\n')

# ---------------- cenário
bs = build_all()
for i, (img, wins) in enumerate(bs):
    img.save(f'{ASSET}/b{i}.png')
    lit_overlay(img.w, img.h, wins, 100 + i).save(f'{ASSET}/w{i}.png')
poste(False).save(f'{ASSET}/lamp0.png'); poste(True).save(f'{ASSET}/lamp1.png')
coqueiro().save(f'{ASSET}/t0.png')
copa(30, 38, [hexc('4fae4a'), hexc('2f8a3a'), hexc('1d5f2b')], 10, 3, [hexc('f5a623'), hexc('e8742a')]).save(f'{ASSET}/t1.png')
copa(28, 34, [hexc('ffe14a'), hexc('f2b705'), hexc('6aa84f')], 10, 7, [hexc('ffd21f'), hexc('ffffff')]).save(f'{ASSET}/t2.png')
sign_pare().save(f'{ASSET}/sign0.png'); sign_p().save(f'{ASSET}/sign1.png'); sign_lombada().save(f'{ASSET}/sign2.png')
semaforo().save(f'{ASSET}/tl.png'); bench().save(f'{ASSET}/bench.png')
ponto_onibus().save(f'{ASSET}/busstop.png'); orelhao().save(f'{ASSET}/phone.png'); carrinho().save(f'{ASSET}/cart.png')
calcada(0).save(f'{ASSET}/walk0.png'); calcada(1).save(f'{ASSET}/walk1.png'); asfalto().save(f'{ASSET}/road0.png')
open(f'{ASSET}/CREDITS.txt', 'w').write('Todos os sprites (veículos, prédios, adereços) foram gerados proceduralmente por tools/gen/*.py. Sem assets de terceiros.\n')
print('buildings', len(bs), 'vehicles', len(names))
print([ (m['name'], m['W'], m['H']) for m in metas][-6:])
