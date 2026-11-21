import random, math
from px import *
from scenery import finalize, face, SKY_GLASS

def poste(transformer=False):
    im = Img(9, 32); conc = [hexc('d3d5d6'), hexc('a9acaf'), hexc('7d8084')]
    im.rect(4, 4, 2, 27, conc[1]); im.vline(4, 4, 27, conc[0]); im.vline(5, 4, 27, conc[2])
    im.rect(3, 29, 4, 3, conc[1]); im.hline(3, 29, 4, conc[0]); im.hline(3, 31, 4, conc[2])
    im.rect(0, 3, 9, 1, hexc('5b4a3a')); im.hline(0, 4, 9, hexc('3a2f26'))                 # cruzeta de madeira
    for x in (0, 2, 6, 8): im.set(x, 2, hexc('6fa8c8')); im.set(x, 3, hexc('3d6f8f'))        # isoladores
    im.rect(3, 0, 3, 2, hexc('fff4c2')); im.hline(3, 0, 3, hexc('ffffff')); im.hline(2, 2, 5, hexc('2b2b30'))
    for y in (10, 16, 22): im.hline(3, y, 4, hexc('4d4f52')) if False else None
    if transformer:
        im.rect(1, 9, 7, 6, hexc('6b7a6e')); im.hline(1, 9, 7, hexc('93a496')); im.hline(1, 14, 7, hexc('445047')); im.vline(1, 9, 6, hexc('445047'))
        im.set(3, 8, hexc('2b2b30')); im.set(5, 8, hexc('2b2b30'))
        im.hline(2, 11, 5, hexc('55645a'))
    # placa de identificação e fios soltos
    im.set(2, 18, hexc('2b2b30')); im.vline(2, 18, 4, hexc('2b2b30')); im.vline(7, 17, 5, hexc('2b2b30'))
    return finalize(im, [], 0)

def coqueiro():
    W, H = 26, 50; im = Img(W, H)
    trunk = [hexc('8a7458'), hexc('6a5640'), hexc('4a3a2a')]
    for y in range(14, H):
        x = 12 + int(2.2 * math.sin(y * 0.11)) 
        im.rect(x, y, 3, 1, trunk[1]); im.set(x, y, trunk[0]); im.set(x + 2, y, trunk[2])
        if y % 4 == 0: im.hline(x, y, 3, trunk[2])
    im.rect(10, H - 3, 8, 3, trunk[1]); im.hline(10, H - 3, 8, trunk[0])
    cx, cy = 13, 14
    leaf = [hexc('5fb04f'), hexc('3f8f3c'), hexc('27652c')]
    for a in (-170, -140, -110, -70, -40, -10, 15, 200):
        L = 11
        for t in range(L):
            px_ = cx + math.cos(math.radians(a)) * t
            py_ = cy - math.sin(math.radians(a)) * t * 0.55 + (t * t) * 0.045
            c = leaf[0] if t < 5 else leaf[1]
            im.set(px_, py_, c); im.set(px_, py_ + 1, leaf[1]); 
            if t % 2 == 0: im.set(px_, py_ + 2, leaf[2])
    for dx, dy in ((-1, 2), (2, 2), (0, 3)): im.rect(cx + dx, cy + dy, 2, 2, hexc('a0713a')); im.set(cx + dx, cy + dy, hexc('d9a35f'))
    return finalize(im, [], 0)

def copa(w, h, cols, trunk_h, seed, dots=None):
    im = Img(w, h); rnd = random.Random(seed)
    tr = [hexc('8b6b4a'), hexc('5e432b')]
    tx = w // 2 - 1
    im.rect(tx, h - trunk_h, 3, trunk_h, tr[0]); im.vline(tx + 2, h - trunk_h, trunk_h, tr[1])
    im.rect(tx - 2, h - 3, 7, 3, tr[0])
    cw, ch = w, h - trunk_h + 4
    for y in range(ch):
        for x in range(w):
            nx = (x + .5 - w / 2) / (w / 2); ny = (y + .5 - ch / 2) / (ch / 2)
            wob = 1 + 0.12 * math.sin(x * 0.9 + seed) * math.cos(y * 0.7)
            if nx * nx + ny * ny <= wob:
                lit = (-nx * .6 - ny * .8)
                c = cols[0] if lit > .45 else cols[1] if lit > -.1 else cols[2]
                if rnd.random() < .12: c = cols[(cols.index(c) + 1) % 3]
                im.set(x, y, c)
    if dots:
        for _ in range(14):
            x, y = rnd.randrange(3, w - 3), rnd.randrange(3, ch - 3)
            if im.get(x, y)[3]: im.set(x, y, rnd.choice(dots))
    return finalize(im, [], 0)

def sign_pare():
    im = Img(20, 32)
    im.rect(9, 16, 2, 16, hexc('9aa3ab')); im.vline(10, 16, 16, hexc('5d666e'))
    for y in range(0, 18):
        for x in range(20):
            dx, dy = abs(x - 9.5), abs(y - 8.5)
            if dx <= 9.2 and dy <= 9.2 and dx + dy <= 13.8: im.set(x, y, hexc('d32f2f'))
    draw_text(im, 2, 6, 'PARE', hexc('ffffff'))
    return finalize(im, [], 0)

def sign_p():
    im = Img(20, 32)
    im.rect(9, 15, 2, 17, hexc('9aa3ab')); im.vline(10, 15, 17, hexc('5d666e'))
    im.rect(3, 0, 14, 16, hexc('1565c0')); w = hexc('ffffff')
    im.hline(4, 1, 12, w); im.hline(4, 14, 12, w); im.vline(4, 1, 14, w); im.vline(15, 1, 14, w)
    im.rect(8, 4, 2, 8, w); im.rect(10, 4, 3, 2, w); im.rect(12, 5, 1, 3, w); im.rect(10, 8, 3, 1, w)
    return finalize(im, [], 0)

def sign_lombada():
    im = Img(20, 32)
    im.rect(9, 17, 2, 15, hexc('9aa3ab')); im.vline(10, 17, 15, hexc('5d666e'))
    for y in range(0, 18):
        for x in range(20):
            if abs(x - 9.5) + abs(y - 8.5) * 1.1 <= 9.6: im.set(x, y, hexc('fdd835'))
    for x in range(5, 15): im.set(x, 10 - int(3 * math.sin((x - 5) / 10 * math.pi)), hexc('15161a'))
    im.hline(4, 11, 12, hexc('15161a'))
    return finalize(im, [], 0)

def semaforo():
    im = Img(8, 30)
    im.rect(3, 14, 2, 16, hexc('3a3d45')); im.vline(3, 14, 16, hexc('5a5d66'))
    im.rect(1, 1, 6, 13, hexc('1d1e24')); im.vline(1, 1, 13, hexc('3a3d45')); im.hline(1, 1, 6, hexc('3a3d45'))
    for k in range(3):
        im.rect(3, 3 + k * 4, 2, 2, hexc('3a2a2a'))
        im.hline(2, 2 + k * 4, 4, hexc('f2b705'))      # viseira amarela
    im.rect(0, 0, 8, 1, hexc('f2b705'))
    return im

def bench():
    im = Img(32, 16); wood = [hexc('9ccc65'), hexc('6a9a3a'), hexc('3f6a22')]
    for y in (3, 6, 9):
        im.rect(1, y, 30, 2, wood[1]); im.hline(1, y, 30, wood[0]); im.hline(1, y + 1, 30, wood[2])
    im.rect(3, 11, 3, 5, hexc('9a9ea3')); im.rect(26, 11, 3, 5, hexc('9a9ea3'))
    im.vline(3, 11, 5, hexc('d3d5d6')); im.vline(28, 11, 5, hexc('6f7378'))
    im.rect(2, 0, 2, 4, hexc('9a9ea3')); im.rect(27, 0, 2, 4, hexc('9a9ea3'))
    return finalize(im, [], 0)

def ponto_onibus():
    W, H = 44, 36; im = Img(W, H)
    im.rect(2, 4, 40, 3, hexc('1565c0')); im.hline(2, 4, 40, hexc('64a0ff')); im.hline(2, 6, 40, hexc('0d3f82'))
    im.rect(3, 7, 2, 29, hexc('9aa3ab')); im.rect(38, 7, 2, 29, hexc('9aa3ab'))
    im.rect(5, 8, 33, 18, (150, 200, 235, 90)); im.hline(5, 8, 33, hexc('cfe9f9'))
    im.rect(8, 27, 28, 2, hexc('6a9a3a')); im.hline(8, 27, 28, hexc('9ccc65')); im.rect(10, 29, 2, 6, hexc('555')); im.rect(32, 29, 2, 6, hexc('555'))
    # placa
    im.rect(21, 0, 2, 4, hexc('9aa3ab'))
    im.rect(14, 0, 16, 1, hexc('0d3f82'))
    draw_text(im, 7, 11, 'ONIBUS', hexc('ffffff'))
    im.rect(6, 10, 25, 7, hexc('0d3f82')); draw_text(im, 7, 11, 'ONIBUS', hexc('ffffff'))
    return finalize(im, [], 0)

def orelhao():
    im = Img(14, 24); 
    im.rect(6, 12, 3, 12, hexc('596069')); im.vline(6, 12, 12, hexc('8a929a'))
    for y in range(0, 14):
        for x in range(14):
            nx = (x + .5 - 7) / 7.0; ny = (y + .5 - 8) / 8.0
            if nx * nx + ny * ny <= 1 and y <= 12:
                c = hexc('ff8a3d') if nx * .6 + ny * .5 < .1 else hexc('c9561a')
                if nx * .6 + ny * .5 < -.35: c = hexc('ffb870')
                im.set(x, y, c)
    im.rect(3, 6, 8, 6, hexc('3a2a22')); im.rect(4, 7, 6, 4, hexc('f2e6d4')); im.set(5, 8, hexc('555')); im.set(8, 8, hexc('555'))
    return finalize(im, [], 0)

def carrinho():
    W, H = 30, 28; im = Img(W, H)
    for y in range(0, 7):
        for x in range(W):
            hw = (W / 2 - 1) * (1 - (6 - y) / 8.0)
            if abs(x - W / 2) <= hw: im.set(x, y, hexc('e53935') if (x // 3) % 2 == 0 else hexc('fafafa'))
    im.vline(15, 7, 6, hexc('555'))
    im.rect(4, 13, 22, 8, hexc('eceff1')); im.hline(4, 13, 22, hexc('ffffff')); im.hline(4, 20, 22, hexc('a9b5be'))
    im.rect(5, 14, 20, 3, hexc('ffcc80')); im.hline(5, 14, 20, hexc('ffe0b2'))
    draw_text(im, 7, 15, 'MILHO', hexc('b71c1c')) if False else None
    for x in (7, 21):
        im.rect(x, 21, 2, 5, hexc('555'))
    for cx in (8, 21):
        for y in range(22, 28):
            for x in range(cx - 3, cx + 4):
                if (x - cx) ** 2 + (y - 24.5) ** 2 <= 9: im.set(x, y, hexc('20232a'))
        im.set(cx, 25, hexc('9aa3ab'))
    return finalize(im, [], 0)

def calcada(kind):
    if kind == 0:
        im = Img(32, 14); base = hexc('b9c3c9')
        im.rect(0, 0, 32, 14, base); im.hline(0, 0, 32, hexc('d3dbe0'))
        for x in (0, 16): im.vline(x, 0, 14, hexc('9aa6ad'))
        rnd = random.Random(5)
        for _ in range(30): im.set(rnd.randrange(32), rnd.randrange(1, 14), hexc('a6b1b8') if rnd.random() < .6 else hexc('cad2d7'))
        im.line(18, 3, 21, 8, hexc('8f9ba2')); im.line(21, 8, 20, 12, hexc('8f9ba2'))
    else:
        im = Img(32, 14); im.rect(0, 0, 32, 14, hexc('e8e4da'))
        for x in range(32):
            for y in range(14):
                wave = 7 + 3.2 * math.sin(x * 2 * math.pi / 16)
                band = int((y - wave + 40) // 3.0) % 2
                c = hexc('2a2a30') if band == 0 else hexc('f0ece2')
                # cantos das pedras
                if (x + y) % 3 == 0: c = mix(c, (150, 150, 150, 255), .25)
                im.set(x, y, c)
        im.hline(0, 0, 32, hexc('f7f4ec'))
    return im

def asfalto():
    W, H = 48, 31; im = Img(W, H); rnd = random.Random(9)
    base = hexc('4a5e6a')
    im.rect(0, 0, W, H, base)
    im.rect(0, 20, W, 11, hexc('435663'))
    for _ in range(140):
        x, y = rnd.randrange(W), rnd.randrange(H)
        im.set(x, y, hexc('566b78') if rnd.random() < .5 else hexc('3b4d58'))
    for _ in range(5):
        x, y = rnd.randrange(W - 6), rnd.randrange(H - 3)
        for k in range(rnd.randrange(3, 6)): im.set(x + k, y + rnd.randrange(2), hexc('2e3d47'))
    return im
