import math
from px import *
from carlib import *
from heroes import *
from shapes_parse import parse_shapes

SH = parse_shapes()

def pickup_extras(L, I, meta, ctx):
    SX, OY = ctx['SX'], ctx['OY']
    L.rect(2 + SX, 10 + OY, 24, 2, OUT); L.hline(2 + SX, 10 + OY, 24, hexc('5a5d66'))
    L.vline(1 + SX, 10 + OY, 8, P4)

def generic(name, key, vs=1.25, extras=None, **kw):
    s = SH[key]
    sp = dict(body=s['body'], glass=s['glass'], doors=s['doors'], wheel_x=[s['rear'], s['front']],
              D=11 if s['r'] >= 4.9 else 9)
    sp = stretch_spec(sp, vs)
    nose = sp['body'][-3]; tail = sp['body'][3]
    ch = 'chrome' if s['chrome'] else 'black'
    sp.update(name=name, bodies=[key], nose_y=nose, tail_y=tail,
              lights=[('head', 58.2, nose + 0.4, 3, 2), ('tail', 0.4, tail + 1.2, 2, 3, 'amber')],
              bumpers=[(58.5, 15, 5, 3, ch), (-1.5, 15, 5, 3, ch)], head_t=0.46)
    if s['bed']: sp['extras'] = pickup_extras
    if extras: sp['extras'] = extras
    sp.update(kw)
    return sp

GENERIC = [
    generic('boxy', 'BOXY'), generic('wagon', 'WAGON'), generic('mpv', 'MPV'), generic('hatch', 'HATCH'),
    generic('sedan', 'SEDAN'), generic('suv', 'SUV'), generic('pickup', 'PICKUP'), generic('coupe', 'COUPE', 1.15),
    generic('classic', 'CLASSIC'), generic('super', 'SUPER', 1.1), generic('notch', 'NOTCH'),
    generic('muscle', 'MUSCLE', 1.15), generic('p911', 'P911', 1.15), generic('jeep', 'JEEP', 1.2), generic('mini', 'MINI'),
]
# ordem final dos sprites (índice no Kotlin)
ORDER = ['fusca', 'kombi', 'uno', 'gol', 'corsa', 'opala', 'boxy', 'wagon', 'mpv', 'hatch', 'sedan', 'suv', 'pickup',
         'coupe', 'classic', 'super', 'notch', 'muscle', 'p911', 'jeep', 'mini', 'taxi']
BODY_TO = {'BEETLE': 'fusca', 'VAN': 'kombi', 'UNO': 'uno', 'GOL': 'gol', 'COMPACT': 'corsa', 'OPALA': 'opala', 'BOXY': 'boxy',
           'WAGON': 'wagon', 'MPV': 'mpv', 'HATCH': 'hatch', 'SEDAN': 'sedan', 'SUV': 'suv', 'PICKUP': 'pickup', 'COUPE': 'coupe',
           'CLASSIC': 'classic', 'SUPER': 'super', 'NOTCH': 'notch', 'MUSCLE': 'muscle', 'P911': 'p911', 'JEEP': 'jeep', 'MINI': 'mini'}

# ------------------------------------------------------------------ CAMINHÃO / ÔNIBUS (via mesmo shader)
def truck_extras(L, I, meta, ctx):
    SX, OY, gb, bot = ctx['SX'], ctx['OY'], ctx['gb'], ctx['bot']
    pm, out = ctx['paint'], ctx['out']
    kind = ctx['spec']['kind']
    if kind == 'bau':
        for x in range(8 + SX, 69 + SX, 8):          # nervuras do baú
            for y in range(-9 + OY, 16 + OY):
                if pm[y, x] and not out[y, x]: L.set(x, y, P3)
        for y in range(-4 + OY, 3 + OY):              # faixa + letreiro
            for x in range(2 + SX, 69 + SX):
                if pm[y, x] and not out[y, x]: L.set(x, y, hexc('f4f6f8') if y in (-4 + OY, 2 + OY) else hexc('d7dde2'))
        draw_text(L, 20 + SX, -2 + OY, 'FRETE RAPIDO', hexc('c62828'))
        L.hline(2 + SX, 16 + OY, 68, hexc('23252b'))   # chassi
        L.rect(2 + SX, 14 + OY, 3, 4, BLACKP[1])      # para-lama traseiro
    else:
        madeira = [hexc('c98d4e'), hexc('a9703a'), hexc('86542a'), hexc('5e3a1c')]
        for y in range(-1 + OY, 17 + OY):
            for x in range(1 + SX, 71 + SX):
                if pm[y, x] and not (y >= 0 + OY and out[y, x] and False):
                    L.set(x, y, madeira[(y - OY + 1) // 3 % 3 if (y - OY) % 3 else 3] if not out[y, x] else madeira[3])
        for x in range(10 + SX, 70 + SX, 12): L.vline(x, -1 + OY, 18, madeira[3])
        # carga: sacos
        sack = [hexc('efe2bd'), hexc('cdbd90'), hexc('9d8a5e')]
        for row, y0 in enumerate((-4, -9)):
            n = 9 - row
            for i in range(n):
                x0 = 4 + SX + i * 7 + (3 if row else 0); yy = y0 + OY
                L.rect(x0, yy, 7, 5, sack[1]); L.hline(x0, yy, 7, sack[0]); L.hline(x0, yy + 4, 7, sack[2]); L.vline(x0 + 6, yy, 5, sack[2])
                L.set(x0 + 3, yy + 2, sack[2])
    # cabine: grade e escapamento vertical
    L.rect(99 + SX, 11 + OY, 2, 5, OUT); L.vline(99 + SX, 12 + OY, 3, CHROME[2])
    L.vline(72 + SX, -6 + OY, 8, CHROME[1]); L.vline(73 + SX, -6 + OY, 8, CHROME[3]); L.hline(72 + SX, -7 + OY, 2, CHROME[0])
    L.set(80 + SX, gb + 3, CHROME[0])

def truck(kind):
    top = -10 if kind == 'bau' else 4
    body = [1,18, 0.5,top+1, 1.5,top, 69,top, 70,top+1, 70,2.5, 71.5,0.8, 73,0, 94,0, 97.5,3, 99,9, 101,10.5, 101,18]
    return dict(name='truck_' + kind, kind=kind, W=124, H=40, GY=36, bodies=[], body=body,
                glass=[[79.5,9.4, 79.5,2.4, 92.5,2.4, 96.2,9.4]], doors=[78, 97], handle_x=[80.5], wheel_x=[13, 26, 88], D=11, arch_pad=1.5,
                nose_y=10.5, tail_y=12, lights=[('head', 98.4, 11, 2, 2), ('tail', 0.5, 12.4, 2, 3, 'amber')],
                bumpers=[(98.5, 15, 5, 3, 'black'), (-1.5, 15, 4, 3, 'black')], extras=truck_extras, head_t=0.35, grille=False)

def bus_extras(L, I, meta, ctx):
    SX, OY, gb, bot = ctx['SX'], ctx['OY'], ctx['gb'], ctx['bot']
    pm, out = ctx['paint'], ctx['out']
    # faixa branca + listra
    for x in range(1 + SX, 96 + SX):
        for k, y in enumerate((8 + OY, 9 + OY, 12 + OY)):
            if pm[y, x] and not out[y, x]: L.set(x, y, hexc('f4f6f8') if k < 2 else hexc('ffc107'))
    # letreiro de destino
    sx0, sy0 = 68 + SX, -8 + OY
    L.rect(sx0 - 1, sy0 - 1, 26, 7, OUT); L.rect(sx0, sy0, 24, 5, hexc('1b1405'))
    draw_text(L, sx0 + 1, sy0, 'CENTRO', hexc('ffb300'))
    # porta dupla
    for y in range(-3 + OY, 17 + OY):
        if pm[y, 79 + SX] : L.set(79 + SX, y, P4)
        if pm[y, 86 + SX] : L.set(86 + SX, y, P4)
    for x in range(1 + SX, 78 + SX, 14):
        for y in range(-2 + OY, 6 + OY):
            if pm[y, x] and not m_glass_at(ctx, x, y): L.set(x, y, P3)
    # entrada de ar do motor
    for k in range(4): L.hline(4 + SX, 11 + OY + k * 2, 8, P4)

def m_glass_at(ctx, x, y): return ctx['m_glass'][y, x]

def bus():
    glass = []
    for x0 in (5, 19, 33, 47, 61):
        glass.append([x0, 4, x0, -3.4, x0 + 12, -3.4, x0 + 12, 4])
    glass.append([79.4, 14.5, 79.4, -3.4, 85.6, -3.4, 85.6, 14.5])   # porta
    glass.append([87, 4, 87, -3.4, 94.6, -3.4, 95.6, 4])
    body = [1,18, 0.4,-5, 1.6,-9, 91,-9, 94.5,-8.4, 96.4,-2, 97.4,8, 97.4,18]
    return dict(name='bus', W=124, H=36, GY=32, bodies=[], body=body, glass=glass, doors=[78.4, 86.2], handle_x=[88], wheel_x=[19, 77], D=13, arch_pad=1.6,
                nose_y=9, tail_y=6, lights=[('head', 95.5, 10, 2, 2), ('tail', 0.4, 8, 2, 3, 'amber')],
                bumpers=[(95, 15, 5, 3, 'black'), (-1.5, 15, 4, 3, 'black')], extras=bus_extras, head_t=0.5, grille=False, head_y=-2.0, mirror_dx=2)

# ------------------------------------------------------------------ MOTOS (desenho direto)
def moto(delivery=False):
    W, H, GY = 40, 32, 26
    L = Img(W, H); I = Img(W, H)
    D = 9
    cxs = [(7, 21.5), (30, 21.5)]
    wheels = [(int(c - D / 2.0 + 0), GY - D) for c, _ in cxs]
    wheels = [(3, GY - D), (26, GY - D)]
    centers = [(3 + 4.5, GY - 4.5), (26 + 4.5, GY - 4.5)]
    dk = hexc('2a2c33'); mt = hexc('8e969e'); mt2 = hexc('c5ccd2')
    # braço oscilante / motor
    L.rect(11, 18, 11, 6, dk); L.hline(11, 18, 11, hexc('474b54')); L.rect(13, 20, 4, 3, mt); L.hline(13, 20, 4, mt2)
    L.line(11, 21, 7, 22, dk); L.line(11, 20, 7, 21, dk)
    # escapamento
    L.rect(2, 22, 11, 2, mt); L.hline(2, 22, 11, mt2); L.hline(2, 23, 11, hexc('5b636b')); L.rect(1, 22, 2, 2, dk)
    # garfo
    L.line(31, 21, 28, 11, hexc('9aa3ab')); L.line(32, 21, 29, 11, mt)
    # paralama dianteiro
    for x in range(26, 35): L.set(x, 15 - (1 if 28 <= x <= 32 else 0), P2)
    for x in range(27, 34): L.set(x, 14, P0)
    # tanque
    L.rect(14, 11, 9, 5, P2); L.hline(14, 11, 9, P0); L.hline(14, 12, 8, P1); L.hline(13, 15, 10, P3); L.hline(14, 16, 8, P4)
    L.vline(23, 12, 3, P3)
    # banco
    L.rect(6, 11, 9, 2, hexc('1c1d22')); L.hline(6, 11, 9, hexc('3c3e47'))
    # paralama / lanterna traseira
    L.rect(2, 12, 5, 2, P2); L.hline(2, 12, 5, P0); L.rect(1, 12, 1, 2, hexc('d32727')); L.set(1, 12, hexc('ff6b5a'))
    # guidão, farol, retrovisor
    L.line(27, 10, 24, 9, dk); L.rect(25, 8, 3, 1, dk)
    L.rect(29, 10, 3, 3, OUT); L.rect(30, 10, 2, 2, hexc('fff2b0')); L.set(30, 10, hexc('ffffff'))
    L.line(26, 8, 26, 5, dk); L.rect(25, 4, 3, 2, mt)
    # piloto
    jacket = hexc('ef6c00') if delivery else hexc('1e88e5')
    jacket_d = hexc('b04e00') if delivery else hexc('1565c0')
    skin = hexc('c68642'); pant = hexc('283593')
    L.rect(15, 5, 6, 7, jacket); L.vline(15, 5, 7, jacket_d); L.hline(15, 5, 6, mix(jacket, (255, 255, 255, 255), .25))
    L.rect(13, 7, 3, 5, jacket)       # costas inclinadas
    L.line(21, 7, 25, 9, jacket); L.line(21, 8, 25, 10, jacket_d); L.set(26, 9, skin)    # braço
    L.rect(12, 12, 6, 3, pant); L.rect(16, 14, 3, 5, pant); L.rect(16, 19, 4, 2, hexc('15161a'))  # perna + bota
    L.hline(12, 12, 6, hexc('3f51b5'))
    # capacete
    L.rect(15, 0, 6, 5, hexc('f5f5f5') if not delivery else hexc('c62828')); L.hline(15, 0, 6, hexc('ffffff') if not delivery else hexc('ef5350'))
    L.rect(19, 2, 3, 2, hexc('1b2430')); L.set(20, 2, hexc('6fa8c8')); L.hline(15, 4, 7, hexc('b0b0b0') if not delivery else hexc('8e0000'))
    L.vline(14, 1, 3, hexc('d0d0d0') if not delivery else hexc('a31515'))
    # baú de entrega
    if delivery:
        L.rect(1, 3, 12, 9, hexc('ffc107')); L.hline(1, 3, 12, hexc('fff0a0')); L.hline(1, 11, 12, hexc('c79100')); L.vline(1, 3, 9, hexc('e0a800'))
        L.vline(12, 3, 9, hexc('c79100'))
        L.rect(1, 3, 12, 1, hexc('ffe27a')); L.hline(1, 7, 12, hexc('d99f00'))
        draw_text(L, 3, 8, 'LAN', hexc('b71c1c'))
        L.rect(12, 5, 2, 5, dk)
    # contorno escuro nas partes pintadas
    meta = dict(W=W, H=H, cx=20, gy=GY, D=D, wheels=wheels, arch_r=0.0, centers=centers, bot=GY - 8, gb=9, gt=2, head_xy=(15, 2),
                glass_box=(0, 0, 0, 0), door=(0, 0, 0, 0), deck=(4, 12), hood_y=11, front_x=34, rear_x=1, nose_y=11, tail_y=12,
                SX=0, OY=GY - 22, cab=False, hl=(32, 11), tl=(1, 13), head_rect=(30, 10, 2, 2), tail_rect=(1, 12, 1, 2),
                wing=(3, 11), exhaust=(2, 22), body_top=0)
    return L, I, meta
