"""Gera PackData.kt (medidas dos veículos dos packs, paletas extras) e imprime o trecho do enum Body.
Uso: python3 packs_kotlin.py <metas.json> <tun_index.json> <tune.json> <PackData.kt>"""
import json, sys

metas = json.load(open(sys.argv[1])); tidx = json.load(open(sys.argv[2])); tune = json.load(open(sys.argv[3]))
out_kt = sys.argv[4]

# tintas extras (do pack de tunagem): indice do swatch -> nome
PAINT_NAMES = {3: 'Vermelho vivo', 4: 'Azul elétrico', 5: 'Azul céu', 6: 'Amarelo vivo', 7: 'Verde neon', 8: 'Azul gelo',
               9: 'Prata azulada', 11: 'Bronze', 12: 'Magenta', 13: 'Laranja vivo', 15: 'Verde bandeira', 16: 'Azul royal',
               17: 'Cobre', 18: 'Marrom', 19: 'Dourado', 20: 'Chumbo', 21: 'Azul cobalto', 22: 'Preto piano', 23: 'Laranja queimado'}

def ia(v): return 'intArrayOf(' + ', '.join(str(int(x)) for x in v) + ')'
def up(s): return s.upper()

L = ['package com.ferrovelho.game', '', '// GERADO por tools/gen/packs_kotlin.py a partir dos packs (brasileiros + luxo + tunagem) — não edite à mão.', '',
     'const val P_BASE = 27            // primeiro índice de sprite dos packs (0..26 = sprites originais)',
     f'const val P_COUNT = {len(metas)}', 'const val V_TOTAL = P_BASE + P_COUNT', '']
for m in metas:
    L.append(f'const val PK_{up(m["name"])} = {27 + m["idx"]}')
L += ['', 'val PACK_NAMES: List<String> = listOf(' + ', '.join('"%s"' % m['disp'] for m in metas) + ')',
      'val PACK_KIND: List<String> = listOf(' + ', '.join('"%s"' % m['kind'] for m in metas) + ')', '',
      'val PMETA: List<VMeta> = listOf(']
for m in metas:
    L.append(f'    VMeta("{m["name"]}", {m["w"]}, {m["h"]}, {m["cx"]}, {m["gy"]}, {m["d"]}, {ia(m["wx"])}, {ia(m["wy"])}, {m["archR"]}f, {m["bot"]},')
    L.append(f'        {ia(m["head"])}, {ia(m["glass"])}, {ia(m["door"])}, {ia(m["wing"])}, {ia(m["exhaust"])},')
    L.append(f'        {m["front"]}, {m["rear"]}, {ia(m["hl"])}, {ia(m["tl"])}, {ia(m["headRect"])}, {ia(m["tailRect"])}, {m["hoodY"]}, {"true" if m["cab"] else "false"}, true),')
L.append(')')
extra = [(PAINT_NAMES[i], tidx['tinta'][i]) for i in sorted(PAINT_NAMES)]
L += ['', '/** Tintas do pack de tunagem (anexadas ao fim de PAINTS: os índices salvos continuam valendo). */',
      'val PAINTS_PACK: List<Pair<String, Long>> = listOf(']
for n, (r, g, b) in extra: L.append(f'    "{n}" to 0xFF{r:02X}{g:02X}{b:02X},')
L += [')', '', '/** Cores das molas dos coilovers do pack (suspensão rebaixada). */',
      'val COIL_COLORS: LongArray = longArrayOf(' + ', '.join(f'0xFF{r:02X}{g:02X}{b:02X}' for r, g, b in tune['coil'][:4]) + ')', '']
open(out_kt, 'w').write('\n'.join(L) + '\n')
print('Body entries:')
print(', '.join(f'P_{up(m["name"])}({27 + m["idx"]})' for m in metas))
