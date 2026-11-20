import random, math
from px import *

def ramp3(base):
    b = base[:3]
    mk = lambda c, t, tgt: mix(c + (255,), tgt + (255,), t)
    return dict(base=base, hi=mk(b, .22, (255, 255, 255)), lo=mk(b, .22, (30, 20, 60)), dk=mk(b, .5, (20, 14, 40)))

PASTEL = {k: hexc(v) for k, v in dict(salmao='e9a08a', mostarda='e6b24a', verde_agua='56b3a8', rosa='e892ac', lilas='a99bd8',
          limao='b0cc62', ceu='72b6e2', creme='f0e2bd', terracota='cb6a42', concreto='a9acaf', branco='eceff1', azul='4f78c4').items()}
ROOF = [hexc('d0693a'), hexc('b2502d'), hexc('8c3a20')]
SKY_GLASS = [hexc('9fd3f0'), hexc('5aa0d6'), hexc('2e6aa8')]
LIT = hexc('ffd76a')

def face(img, x, y, w, h, col, texture=True, seed=0):
    r = ramp3(col)
    img.rect(x, y, w, h, r['base'])
    img.rect(x, y, w, 1, r['hi'])
    img.rect(x, y + h - 3, w, 3, r['lo'])
    img.vline(x, y, h, r['hi']); img.vline(x + w - 1, y, h, r['lo'])
    img.rect(x, y, w, 1, r['hi'])
    rnd = random.Random(seed + x * 7 + y)
    if texture:
        for _ in range(w * h // 40):
            xx, yy = rnd.randrange(x + 1, x + w - 1), rnd.randrange(y + 2, y + h - 3)
            img.set(xx, yy, r['lo'] if rnd.random() < .6 else r['hi'])
        # mancha de umidade embaixo
        for xx in range(x + 1, x + w - 1):
            if rnd.random() < .5: img.set(xx, y + h - 4, r['lo'])
    return r

def outline_rect(img, x, y, w, h, c):
    img.hline(x, y, w, c); img.hline(x, y + h - 1, w, c); img.vline(x, y, h, c); img.vline(x + w - 1, y, h, c)

def window(img, wins, x, y, w, h, frame=hexc('f5f5f5'), grille=False, shutter=None, lit_ok=True):
    if shutter:
        img.rect(x - 2, y, 2, h, shutter); img.rect(x + w, y, 2, h, shutter)
        img.vline(x - 2, y, h, mix(shutter, (0, 0, 0, 255), .35)); img.vline(x + w + 1, y, h, mix(shutter, (0, 0, 0, 255), .35))
    img.rect(x - 1, y - 1, w + 2, h + 2, frame)
    img.rect(x, y, w, h, SKY_GLASS[1])
    img.rect(x, y, w, 1, SKY_GLASS[0])
    for k in range(0, w + h, 4):
        xx, yy = x + k, y
        for t in range(h):
            if x <= xx - t < x + w: img.set(xx - t, y + t, SKY_GLASS[0] if (t + k) % 8 < 2 else SKY_GLASS[1])
    img.hline(x - 1, y + h, w + 2, mix(frame, (0, 0, 0, 255), .35))
    if grille:
        for gx in range(x + 1, x + w, 2): img.vline(gx, y, h, hexc('2d2f36'))
        img.hline(x, y + h // 2, w, hexc('2d2f36'))
    if lit_ok: wins.append((x, y, w, h))

def door(img, x, y, w, h, col=hexc('7a4a2b'), glass=False):
    img.rect(x - 1, y - 1, w + 2, h + 1, hexc('2c2118'))
    img.rect(x, y, w, h, col)
    img.vline(x, y, h, mix(col, (255, 255, 255, 255), .2)); img.vline(x + w - 1, y, h, mix(col, (0, 0, 0, 255), .3))
    if glass:
        img.rect(x + 1, y + 1, w - 2, h // 2, SKY_GLASS[1]); img.hline(x + 1, y + 1, w - 2, SKY_GLASS[0])
    else:
        img.rect(x + 2, y + 2, w - 4, h // 2 - 2, mix(col, (0, 0, 0, 255), .18))
    img.set(x + w - 3, y + h // 2 + 1, hexc('e8c14a'))

def awning(img, x, y, w, h, c1, c2):
    for xx in range(w):
        c = c1 if (xx // 3) % 2 == 0 else c2
        img.vline(x + xx, y, h, c)
    img.hline(x, y, w, mix(c1, (255, 255, 255, 255), .3))
    for xx in range(0, w, 3): img.set(x + xx + 1, y + h, c1 if (xx // 3) % 2 == 0 else c2)
    img.hline(x, y + h - 1, w, mix(c1, (0, 0, 0, 255), .4))

def sign(img, x, y, text, bg, fg, w=None):
    w = w or (text_w(text) + 4)
    img.rect(x - 1, y - 1, w + 2, 9, hexc('1e1f25'))
    img.rect(x, y, w, 7, bg)
    img.hline(x, y, w, mix(bg, (255, 255, 255, 255), .3))
    draw_text(img, x + 2, y + 1, text, fg)
    return w

def roof_tiles(img, x, y, w, h):
    for yy in range(h):
        inset = yy // 2 if h > 3 else 0
        for xx in range(x + inset, x + w - inset):
            c = ROOF[0] if (xx + yy * 2) % 4 else ROOF[1]
            img.set(xx, y + yy, c)
        img.hline(x + inset, y + yy, 1, ROOF[2]); img.hline(x + w - inset - 1, y + yy, 1, ROOF[2])
    img.hline(x, y, w, ROOF[0]); img.hline(x, y + h - 1, w, ROOF[2])

def parapet(img, x, y, w, col):
    r = ramp3(col)
    img.rect(x - 1, y, w + 2, 4, r['hi']); img.hline(x - 1, y + 3, w + 2, r['lo']); img.hline(x - 1, y, w + 2, r['hi'])

def caixa_dagua(img, x, y):
    img.rect(x, y + 2, 9, 8, hexc('3f7fd0')); img.hline(x, y + 2, 9, hexc('8cc0ff')); img.vline(x + 8, y + 2, 8, hexc('2a58a0'))
    img.rect(x + 1, y, 7, 2, hexc('2a58a0')); img.hline(x + 1, y, 7, hexc('5b95e0')); img.hline(x, y + 6, 9, hexc('2a58a0'))
    img.vline(x + 1, y + 10, 2, hexc('555')); img.vline(x + 7, y + 10, 2, hexc('555'))

def ac_unit(img, x, y):
    img.rect(x, y, 6, 4, hexc('eceff1')); img.hline(x, y, 6, hexc('ffffff')); img.hline(x, y + 3, 6, hexc('9aa3ab'))
    for i in range(3): img.set(x + 1 + i * 2, y + 2, hexc('6b747c'))
    img.vline(x + 5, y, 4, hexc('b9c2c8'))

def antenna(img, x, y):
    img.vline(x, y, 8, hexc('2b2b30')); img.hline(x - 3, y + 1, 7, hexc('2b2b30')); img.hline(x - 2, y + 3, 5, hexc('2b2b30'))

def parabolica(img, x, y):
    img.rect(x, y, 5, 3, hexc('d5dadf')); img.hline(x, y, 5, hexc('ffffff')); img.hline(x + 1, y + 3, 3, hexc('8a9299')); img.vline(x + 2, y + 3, 2, hexc('555'))

def finalize(img, wins, seed):
    """contorno escuro externo fino em volta de tudo (estilo do jogo) + lista de janelas"""
    from px import edge_mask
    a = img.a[:, :, 3] > 0
    p = np.pad(a, 1, constant_values=False)
    nb = np.zeros_like(a)
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        nb |= p[1 + dy:1 + dy + a.shape[0], 1 + dx:1 + dx + a.shape[1]]
    outline = nb & ~a
    for y, x in zip(*np.where(outline)):
        if y < img.h - 0: img.a[y, x] = hexc('2a2430')
    return img

def lit_overlay(w, h, wins, seed, ratio=0.55):
    o = Img(w, h)
    rnd = random.Random(seed)
    for (x, y, ww, hh) in wins:
        if rnd.random() < ratio:
            o.rect(x, y, ww, hh, LIT); o.hline(x, y, ww, hexc('fff3b0'))
            o.hline(x, y + hh - 1, ww, hexc('e6b13a'))
    return o

# ---------------------------------------------------------------- PRÉDIOS
def sobrado(col, shutter, text=None, seed=1, W=50):
    H = 62; img = Img(W + 2, H + 2); wins = []
    x0 = 1; y0 = 1
    face(img, x0, y0 + 12, W, H - 12, col, seed=seed)
    parapet(img, x0 + 1, y0 + 8, W - 2, col)
    roof_tiles(img, x0 + 3, y0 + 1, W - 6, 8)
    # 2º andar: janelas com veneziana e sacada
    window(img, wins, x0 + 6, y0 + 20, 9, 11, shutter=shutter); window(img, wins, x0 + W - 15, y0 + 20, 9, 11, shutter=shutter)
    door(img, x0 + W // 2 - 4, y0 + 20, 8, 12, glass=True)
    img.rect(x0 + W // 2 - 8, y0 + 32, 16, 2, hexc('d6d6d6')); img.hline(x0 + W // 2 - 8, y0 + 32, 16, hexc('ffffff'))
    for gx in range(x0 + W // 2 - 8, x0 + W // 2 + 8, 2): img.vline(gx, y0 + 28, 4, hexc('2d2f36'))
    img.hline(x0 + W // 2 - 8, y0 + 28, 16, hexc('2d2f36'))
    # térreo: portão de garagem + porta
    img.rect(x0 + 4, y0 + 40, 17, 19, hexc('596069'))
    for yy in range(y0 + 41, y0 + 59, 2): img.hline(x0 + 4, yy, 17, hexc('6f7882'))
    outline_rect(img, x0 + 4, y0 + 40, 17, 19, hexc('2c2f35'))
    door(img, x0 + W - 22, y0 + 41, 9, 18, col=hexc('2f7d5a'))
    window(img, wins, x0 + W - 12, y0 + 44, 7, 8, grille=True)
    img.rect(x0 + 12, y0 + 36, 6, 3, hexc('f5f5f5')); img.rect(x0 + 13, y0 + 37, 4, 1, hexc('555'))
    if text: sign(img, x0 + 6, y0 + 14, text, hexc('1f4e8c'), hexc('ffffff'))
    return finalize(img, wins, seed), wins

def casa_laje(col, seed=2, W=44):
    H = 52; img = Img(W + 2, H + 2); wins = []
    x0, y0 = 1, 1
    # 2º piso inacabado (tijolo + vergalhões)
    brick = hexc('b4543a')
    img.rect(x0 + 3, y0 + 4, W - 8, 12, brick)
    for yy in range(y0 + 4, y0 + 16, 3):
        img.hline(x0 + 3, yy, W - 8, hexc('8c3d29'))
        for xx in range(x0 + 3 + (yy // 3 % 2) * 3, x0 + W - 5, 6): img.vline(xx, yy, 3, hexc('8c3d29'))
    img.hline(x0 + 3, y0 + 4, W - 8, hexc('d9775a'))
    for xx in range(x0 + 5, x0 + W - 6, 8):
        img.vline(xx, y0, 4, hexc('50403a')); img.set(xx, y0, hexc('8a5a3a'))
    caixa_dagua(img, x0 + W - 18, y0 - 1 if False else y0 + 0)
    window(img, wins, x0 + 8, y0 + 7, 8, 6, frame=hexc('c7c7c7'), grille=True)
    # laje
    img.rect(x0 + 1, y0 + 16, W - 2, 4, hexc('b9bcbe')); img.hline(x0 + 1, y0 + 16, W - 2, hexc('e5e7e8')); img.hline(x0 + 1, y0 + 19, W - 2, hexc('7d8084'))
    face(img, x0, y0 + 20, W, H - 20, col, seed=seed)
    window(img, wins, x0 + 6, y0 + 27, 11, 9, grille=True)
    door(img, x0 + 24, y0 + 28, 9, 19, col=hexc('6d4c41'))
    img.rect(x0 + 35, y0 + 30, 5, 17, hexc('596069'))
    for yy in range(y0 + 31, y0 + 47, 2): img.hline(x0 + 35, yy, 5, hexc('7b8590'))
    # muro / calçada de entrada
    parabolica(img, x0 + 6, y0 + 12)
    return finalize(img, wins, seed), wins

def predio_resid(col, floors=6, seed=3, W=48):
    H = 14 + floors * 14 + 14; img = Img(W + 2, H + 2); wins = []
    x0, y0 = 1, 1
    face(img, x0, y0 + 6, W, H - 6, col, seed=seed)
    parapet(img, x0, y0 + 2, W, mix(col, (255, 255, 255, 255), .25)[:3] + (255,))
    caixa_dagua(img, x0 + 8, y0 - 4 if False else y0 - 0)
    antenna(img, x0 + W - 10, y0 - 2 if False else y0)
    for f in range(floors):
        fy = y0 + 12 + f * 14
        for k in range(3):
            wx = x0 + 5 + k * 14
            # varanda com guarda-corpo
            img.rect(wx - 1, fy + 8, 12, 5, hexc('e8e8e8')); img.hline(wx - 1, fy + 8, 12, hexc('ffffff')); img.hline(wx - 1, fy + 12, 12, hexc('9aa0a6'))
            window(img, wins, wx + 1, fy + 1, 8, 8, frame=hexc('f0f0f0'))
            for gx in range(wx, wx + 11, 2): img.vline(gx, fy + 9, 3, hexc('7a8088'))
            if (f * 3 + k + seed) % 5 == 0: ac_unit(img, wx + 3, fy + 10 if False else fy - 0 + 8 - 0 if False else fy + 9)
    # térreo
    ty = y0 + H - 17
    img.rect(x0, ty, W, 17, hexc('8c8f94')); img.hline(x0, ty, W, hexc('b9bcc1'))
    door(img, x0 + W // 2 - 6, ty + 3, 12, 14, col=hexc('3b4b5a'), glass=True)
    img.rect(x0 + 3, ty + 4, 10, 8, hexc('2c2f35')); img.rect(x0 + W - 13, ty + 4, 10, 8, hexc('2c2f35'))
    return finalize(img, wins, seed), wins

def loja(name, col, awn, seed=4, W=56, kind='padaria'):
    H = 46; img = Img(W + 2, H + 2); wins = []
    x0, y0 = 1, 1
    face(img, x0, y0 + 4, W, H - 4, col, seed=seed)
    parapet(img, x0 + 1, y0, W - 2, col)
    sign(img, x0 + (W - text_w(name) - 4) // 2, y0 + 7, name, hexc('c62828') if kind != 'farm' else hexc('1b8a4b'), hexc('fff3c4'))
    awning(img, x0 + 2, y0 + 19, W - 4, 6, awn, hexc('ffffff'))
    # vitrine + porta
    img.rect(x0 + 4, y0 + 28, W - 24, 16, hexc('23303a'))
    img.rect(x0 + 5, y0 + 29, W - 26, 14, SKY_GLASS[1]); img.hline(x0 + 5, y0 + 29, W - 26, SKY_GLASS[0])
    for xx in range(x0 + 6, x0 + W - 22, 4):
        for k in range(3): img.set(xx + k, y0 + 33 + k, SKY_GLASS[0])
    wins.append((x0 + 5, y0 + 29, W - 26, 14))
    # produtos na vitrine
    for xx in range(x0 + 7, x0 + W - 22, 6): img.rect(xx, y0 + 39, 4, 4, [hexc('e8b04a'), hexc('d9573f'), hexc('f3e3b3')][(xx // 6) % 3])
    door(img, x0 + W - 18, y0 + 28, 9, 16, col=hexc('2f6f9f'), glass=True)
    img.rect(x0 + W - 8, y0 + 30, 6, 14, hexc('7a828a'))   # porta de enrolar semi-aberta
    for yy in range(y0 + 31, y0 + 44, 2): img.hline(x0 + W - 8, yy, 6, hexc('9aa3ab'))
    return finalize(img, wins, seed), wins

def predio_comercial(seed=5, W=40, floors=7):
    H = 12 + floors * 11 + 12; img = Img(W + 2, H + 2); wins = []
    x0, y0 = 1, 1
    face(img, x0, y0, W, H, hexc('9fb4c8'), texture=False, seed=seed)
    img.rect(x0, y0, W, 5, hexc('566678')); img.hline(x0, y0, W, hexc('7e90a3'))
    for f in range(floors):
        for k in range(3):
            x, y = x0 + 3 + k * 12, y0 + 8 + f * 11
            img.rect(x, y, 10, 8, SKY_GLASS[1]); img.hline(x, y, 10, SKY_GLASS[0]); img.hline(x, y + 7, 10, SKY_GLASS[2])
            for t in range(8): img.set(x + 2 + t, y + 7 - t if t < 8 else y, SKY_GLASS[0]) if (x + 2 + t) < x + 10 else None
            img.vline(x + 5, y, 8, hexc('6d8196'))
            wins.append((x, y, 10, 8))
    ty = y0 + H - 12
    img.rect(x0, ty, W, 12, hexc('4a5766'))
    door(img, x0 + W // 2 - 6, ty + 1, 12, 11, col=hexc('2c3a48'), glass=True)
    sign(img, x0 + 8, ty - 1, 'ESCRITORIOS', hexc('0f2b4a'), hexc('9fe0ff'), 23) if False else None
    return finalize(img, wins, seed), wins

def igreja(seed=6, W=54):
    H = 78; img = Img(W + 2, H + 2); wins = []
    x0, y0 = 1, 1
    cream = hexc('f2ead2')
    face(img, x0 + 14, y0 + 30, W - 28, H - 30, cream, seed=seed)
    face(img, x0, y0 + 40, 16, H - 40, cream, seed=seed + 1); face(img, x0 + W - 16, y0 + 40, 16, H - 40, cream, seed=seed + 2)
    # torre central e cruz
    face(img, x0 + 20, y0 + 10, 14, 24, cream, seed=seed + 3)
    img.rect(x0 + 24, y0 + 14, 6, 10, hexc('3a3330')); img.rect(x0 + 25, y0 + 15, 4, 8, hexc('5a4f48'))
    img.rect(x0 + 27, y0 + 17, 1, 3, hexc('e0b84a'))
    img.rect(x0 + 18, y0 + 8, 18, 3, hexc('d8cfb4'))
    for k in range(5): img.hline(x0 + 21 + k // 2, y0 + 4 + (4 - k) if False else y0 + 7 - k, 12 - k, ROOF[k % 2])
    img.vline(x0 + 27, y0 - 1, 6, hexc('f4f4f4')); img.hline(x0 + 25, y0 + 1, 5, hexc('f4f4f4'))
    # frontão e porta em arco
    roof_tiles(img, x0 + 14, y0 + 30, W - 28, 5)
    door(img, x0 + W // 2 - 5, y0 + H - 22, 10, 21, col=hexc('5a3a1e'))
    img.hline(x0 + W // 2 - 4, y0 + H - 23, 8, hexc('2c2118')); img.hline(x0 + W // 2 - 3, y0 + H - 24, 6, hexc('2c2118'))
    window(img, wins, x0 + 4, y0 + 52, 6, 9, frame=hexc('d8cfb4')); window(img, wins, x0 + W - 10, y0 + 52, 6, 9, frame=hexc('d8cfb4'))
    img.rect(x0 + W // 2 - 4, y0 + 37, 8, 8, hexc('4a7fb5')); img.set(x0 + W // 2, y0 + 38, hexc('ffd76a'))
    img.vline(x0 + W // 2, y0 + 39, 5, hexc('ffd76a')); img.hline(x0 + W // 2 - 2, y0 + 41, 5, hexc('ffd76a'))
    wins.append((x0 + W // 2 - 4, y0 + 37, 8, 8))
    return finalize(img, wins, seed), wins

def boteco(seed=7, W=40):
    H = 40; img = Img(W + 2, H + 2); wins = []
    x0, y0 = 1, 1
    face(img, x0, y0 + 4, W, H - 4, hexc('f0d56a'), seed=seed)
    parapet(img, x0 + 1, y0, W - 2, hexc('f0d56a'))
    sign(img, x0 + (W - text_w('BAR DO ZE') - 4) // 2, y0 + 6, 'BAR DO ZE', hexc('1b8a4b'), hexc('ffee58'))
    # bandeirinhas verde-amarelo-azul
    for i in range(0, W, 4):
        c = [hexc('1b8a4b'), hexc('ffd600'), hexc('1565c0')][(i // 4) % 3]
        img.set(x0 + i, y0 + 16 + (1 if i % 8 else 0), c); img.set(x0 + i, y0 + 17 + (1 if i % 8 else 0), c)
    img.hline(x0, y0 + 16, W, hexc('2b2b30'))
    # porta de enrolar aberta, balcão e mesinhas
    img.rect(x0 + 4, y0 + 20, W - 8, 19, hexc('2a1d14'))
    img.rect(x0 + 4, y0 + 20, W - 8, 3, hexc('7a828a'))
    for yy in range(y0 + 20, y0 + 23, 1): img.hline(x0 + 4, yy, W - 8, hexc('8f98a0') if yy % 2 else hexc('6b747c'))
    img.rect(x0 + 6, y0 + 31, W - 12, 4, hexc('8d6e63')); img.hline(x0 + 6, y0 + 31, W - 12, hexc('c4a089'))
    for k in range(4): img.rect(x0 + 8 + k * 7, y0 + 26, 2, 5, [hexc('7cb342'), hexc('e53935'), hexc('fdd835'), hexc('1e88e5')][k])
    wins.append((x0 + 6, y0 + 23, W - 12, 8))
    return finalize(img, wins, seed), wins

def casa_colonial(col, seed=8, W=60):
    H = 44; img = Img(W + 2, H + 2); wins = []
    x0, y0 = 1, 1
    roof_tiles(img, x0 + 1, y0 + 2, W - 2, 12)
    face(img, x0 + 2, y0 + 14, W - 4, H - 14, col, seed=seed)
    for k in range(3):
        window(img, wins, x0 + 7 + k * 17, y0 + 20, 8, 12, shutter=hexc('2f7d5a'))
    door(img, x0 + 25, y0 + 22, 9, 20, col=hexc('2f7d5a'))
    img.rect(x0 + 2, y0 + H - 4, W - 4, 3, hexc('9a8f7a'))
    return finalize(img, wins, seed), wins

def kitnet(col, seed=9, W=44):
    """sobrado de 3 andares com escada externa"""
    H = 74; img = Img(W + 2, H + 2); wins = []
    x0, y0 = 1, 1
    face(img, x0, y0 + 6, W, H - 6, col, seed=seed)
    parapet(img, x0, y0 + 2, W, col)
    for f in range(3):
        fy = y0 + 10 + f * 20
        window(img, wins, x0 + 5, fy + 3, 8, 10, grille=True); window(img, wins, x0 + 18, fy + 3, 8, 10, grille=True)
        door(img, x0 + 31, fy + 2, 8, 17, col=hexc('c9a227'))
        img.rect(x0 + 29, fy + 19, 12, 1, hexc('8c8f94'))
    img.rect(x0 + 2, y0 + H - 12, 14, 11, hexc('596069'))
    for yy in range(y0 + H - 11, y0 + H - 1, 2): img.hline(x0 + 2, yy, 14, hexc('6f7882'))
    ac_unit(img, x0 + 20, y0 + 36); ac_unit(img, x0 + 8, y0 + 56)
    return finalize(img, wins, seed), wins

def mercadinho(seed=10, W=60):
    img, wins = loja('MERCADINHO', hexc('f4a8a0'), hexc('d84315'), seed=seed, W=W)
    return img, wins

BUILDINGS = []
def build_all():
    out = []
    out.append(sobrado(PASTEL['salmao'], hexc('2f7d5a'), 'LANCHES', 11))
    out.append(casa_laje(PASTEL['verde_agua'], 12))
    out.append(predio_resid(PASTEL['creme'], 6, 13))
    out.append(loja('PADARIA', PASTEL['mostarda'], hexc('c62828'), 14))
    out.append(sobrado(PASTEL['rosa'], hexc('1f4e8c'), None, 15, 46))
    out.append(casa_laje(PASTEL['lilas'], 16, 40))
    out.append(predio_resid(PASTEL['ceu'], 4, 17, 44))
    out.append(loja('FARMACIA', PASTEL['branco'], hexc('1b8a4b'), 18, 54, 'farm'))
    out.append(igreja(19))
    out.append(boteco(20))
    out.append(casa_colonial(PASTEL['creme'], 21))
    out.append(kitnet(PASTEL['limao'], 22))
    out.append(predio_comercial(23))
    out.append(loja('MERCADO', PASTEL['terracota'], hexc('1565c0'), 24, 58))
    out.append(casa_laje(PASTEL['salmao'], 25, 42))
    out.append(predio_resid(PASTEL['rosa'], 8, 26, 46))
    return out

if __name__ == '__main__':
    bs = build_all()
    from PIL import Image
    S = 3
    W = sum(b[0].w * S + 8 for b in bs[:8]) + 8
    sheetimg = Image.new('RGBA', (W, 2 * (max(b[0].h for b in bs) * S + 12)), (135, 190, 235, 255))
    x = 8; row = 0
    for i, (im, wins) in enumerate(bs):
        if i == 8: x = 8; row = 1
        t = scale_nn(im, S)
        sheetimg.alpha_composite(t, (x, row * (max(b[0].h for b in bs) * S + 12) + (max(b[0].h for b in bs) * S - t.height) + 6)); x += t.width + 8
    sheetimg.convert('RGB').save('prev_buildings.png'); print(sheetimg.size)
