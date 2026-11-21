"""Testes de consistência dos packs (rodar da raiz: python3 tools/gen/packs_selftest.py).
Confere: arquivos de sprite, medidas (rodas/vidro/farol dentro do sprite), Body.P_* usados no catálogo existem,
cada veículo do pack tem pintura codificada e todas as variações de rodas."""
import os, re, json, sys
import numpy as np
from PIL import Image
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
A = f'{ROOT}/app/src/main/assets/packs/'; G = f'{ROOT}/app/src/main/java/com/ferrovelho/game/'
metas = json.load(open(f'{ROOT}/tools/gen/packs_out/cut/metas.json'))
err = []
def need(c, msg):
    if not c: err.append(msg)
need(len(metas) == 72, f'esperava 72 veículos, tem {len(metas)}')
for k, m in enumerate(metas):
    n = m['name']
    for p in ('pb', 'pi', 'pw'): need(os.path.exists(f'{A}{p}{k}.png'), f'falta {p}{k}.png ({n})')
    b = np.asarray(Image.open(f'{A}pb{k}.png').convert('RGBA')); h, w = b.shape[:2]
    need((w, h) == (m['w'], m['h']), f'{n}: tamanho do sprite != meta')
    d = m['d']; need(8 <= d <= 14, f'{n}: diâmetro de roda fora de 8..14 ({d})')
    for x, y in zip(m['wx'], m['wy']):
        need(0 <= x and x + d <= w and 0 <= y and y + d <= h, f'{n}: roda fora do sprite')
        # o recorte da roda tem que estar transparente (a roda vem do atlas)
        yy, xx = np.mgrid[:d, :d]; disc = np.hypot(yy + .5 - d / 2, xx + .5 - d / 2) <= d / 2
        need(b[y:y + d, x:x + d, 3][disc].max() == 0, f'{n}: lataria ainda cobre a roda')
    isp = (b[..., 3] == 255) & (b[..., 1] == 0) & (b[..., 0] == b[..., 2]) & (b[..., 0] > 0)
    need(isp.sum() > 40, f'{n}: pouca pintura codificada ({isp.sum()})')
    g = m['glass']
    if m['cab']: need(g[2] > g[0] and 0 <= g[0] and g[2] < w and g[3] < h, f'{n}: caixa de vidro inválida {g}')
    aw = np.asarray(Image.open(f'{A}pw{k}.png')); need(aw.shape[:2] == (4 * d, 7 * d), f'{n}: atlas de rodas {aw.shape} != {(4 * d, 7 * d)}')
    need(m['front'] > m['rear'] + 20, f'{n}: comprimento estranho')
for d in range(8, 15):
    a = Image.open(f'{A}pws{d}.png'); need(a.size == (7 * d, 6 * d), f'pws{d} tamanho {a.size}')
kt = open(G + 'Cars.kt').read(); en = open(f'{ROOT}/tools/gen/packs_out/body_enum.txt').read()
ents = set(re.findall(r'P_[A-Z0-9_]+(?=\()', en))
used = set(re.findall(r'Body\.(P_[A-Z0-9_]+)', kt))
need(used <= ents, f'Body.P_* inexistentes: {sorted(used - ents)}')
need(all(x in kt for x in ents), 'enum Body não tem todos os P_* gerados')
unused = sorted(ents - used)
print('veículos do pack sem modelo no catálogo (só trânsito/trabalho):', ', '.join(unused))
print('OK' if not err else 'FALHOU:\n  ' + '\n  '.join(err))
sys.exit(1 if err else 0)
