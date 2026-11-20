"""Carros 'herói' brasileiros, cada um com silhueta e detalhes próprios (coordenadas em unidades; chão y=22, soleira y=18)."""
import math
from px import *
from carlib import *

def put_arc(L, ctx, cx, cy, r, a0, a1, color, only_paint=True, step=3):
    """desenha arco de pixels (ângulos em graus, 0=direita, anti-horário visual => y para cima negativo)"""
    paint = ctx['paint']
    for a in range(int(a0), int(a1) + 1, 1):
        x = int(round(cx + math.cos(math.radians(a)) * r))
        y = int(round(cy - math.sin(math.radians(a)) * r))
        if 0 <= y < paint.shape[0] and 0 <= x < paint.shape[1] and paint[y, x]:
            L.set(x, y, color)

# ---------------------------------------------------------------- FUSCA
def fusca_extras(L, I, meta, ctx):
    SX, OY, gb, bot = ctx['SX'], ctx['OY'], ctx['gb'], ctx['bot']
    # frisos cromados em volta do vidro
    for x in range(22 + SX, 40 + SX):
        if ctx['paint'][gb, x] and not ctx['out'][gb, x]: L.set(x, gb, CHROME[1])
    # estribo preto com friso cromado
    L.rect(23 + SX, 16 + OY, 16, 2, BLACKP[1]); L.hline(23 + SX, 16 + OY, 16, BLACKP[0]); L.hline(23 + SX, 15 + OY, 16, CHROME[2])
    # costura dos paralamas (arcos em volta das rodas) + brilho
    for (cx, cy) in meta['centers']:
        put_arc(L, ctx, cx, cy, meta['arch_r'] + 1.8, 28, 152, P4)
        put_arc(L, ctx, cx, cy, meta['arch_r'] + 2.8, 40, 140, P0)
    # tampa do motor: venezianas
    for i, (x, y) in enumerate(((4, 11), (5, 12), (6, 13))):
        for k in range(4):
            xx, yy = x + k + SX, y + OY
            if ctx['paint'][yy, xx]: L.set(xx, yy, P4)
    # para-choques: garras
    L.vline(59 + SX, 12 + OY, 6, CHROME[1]); L.vline(60 + SX, 12 + OY, 6, CHROME[2])
    L.vline(0 + SX, 12 + OY, 6, CHROME[1])
    # vigia do motorista: janela traseira (sombra de banco)
    # porta: maçaneta cromada
    L.set(24 + SX, gb + 3, CHROME[0]); L.set(25 + SX, gb + 3, CHROME[1])

FUSCA = dict(
    name='fusca', bodies=['BEETLE'],
    body=[1,18, 0.4,13.6, 1.6,11, 4,9, 7.4,7.2, 11.6,5.2, 16,3.5, 21,2.4, 27,1.8, 32.5,2.2, 36.4,3.6, 39.8,5.8, 42.4,8.0,
          45,8.6, 49,9.2, 53,9.8, 56.4,10.8, 59,12.4, 60.2,14.6, 60.2,18],
    glass=[[13.5,8.4, 15.3,6.2, 19,4.5, 20.6,4.3, 20.6,8.4], [22.4,8.4, 22.4,4.0, 26,3.3, 31.5,3.4, 35.6,4.6, 39.4,8.4]],
    doors=[21.5, 40.5], handle_x=[23.5], wheel_x=[12.5, 47.5], D=9, arch_pad=1.6, nose_y=12.4, tail_y=11, mirror_dx=40,
    lights=[('head', 55.4, 8.8, 3, 3, 'round'), ('tail', 0.6, 11, 2, 3, 'amber')],
    bumpers=[(57.5, 15, 5, 3, 'chrome'), (-1, 15, 5, 3, 'chrome')], extras=fusca_extras, grille=False, head_t=0.30,
)

# ---------------------------------------------------------------- KOMBI
def kombi_extras(L, I, meta, ctx):
    SX, OY, gb, bot = ctx['SX'], ctx['OY'], ctx['gb'], ctx['bot']
    ty = int(round(ctx['spec']['two_tone_y'])) + OY
    # friso cromado na divisão das duas cores
    for x in range(1 + SX, 62 + SX):
        if 0 <= x < L.w and ctx['paint'][ty, x] and not ctx['out'][ty, x]:
            L.set(x, ty, CHROME[1]); L.set(x, ty + 1, CHROME[3] if x % 2 else CHROME[2])
    # brânquias do motor (traseira)
    for k in range(4):
        for x in range(3 + SX, 9 + SX):
            y = 12 + k * 2 + OY
            if ctx['paint'][y, x] and not ctx['out'][y, x]: L.set(x, y, P4)
    # emblema VW na frente
    for (dx, dy, c) in ((0,0,CHROME[0]),(1,0,CHROME[1]),(0,1,CHROME[1]),(1,1,CHROME[2])):
        L.set(60 + SX + dx, 12 + OY + dy, c)
    # vinco em V do nariz
    for k in range(6):
        x, y = 56 + k + SX, 10 + OY + k
        if ctx['paint'][y, x]: L.set(x, y, P3)
    # porta de correr: trilho
    for x in range(30 + SX, 45 + SX):
        y = 15 + OY
        if ctx['paint'][y, x] and not ctx['out'][y, x]: L.set(x, y, P3)
    L.set(47 + SX, gb + 3, CHROME[0]); L.set(48 + SX, gb + 3, CHROME[1])

KOMBI = dict(
    name='kombi', bodies=['VAN'],
    body=[1,18, 0.4,14, 0.2,3.5, 1.0,0.2, 2.4,-1.0, 55,-1.0, 57.4,0.2, 58.2,1.6, 59.4,9.2, 61.2,9.8, 62.2,11.4, 62.2,18],
    glass=[[3.8,9, 3.8,1.2, 14.2,1.2, 14.2,9], [15.4,9, 15.4,1.2, 28.2,1.2, 28.2,9], [29.6,9, 29.6,1.2, 44.8,1.2, 44.8,9],
           [46.6,9, 46.6,1.2, 55.4,1.2, 57.6,9]],
    doors=[29, 45.8], handle_x=[47], wheel_x=[12, 49.5], D=9, nose_y=11.4, tail_y=10, two_tone_y=9.4, top_ramp=WHITE_RAMP,
    lights=[('head', 59.6, 11.4, 2, 2, 'round'), ('blink', 60, 14, 2, 1), ('tail', 0.6, 10, 2, 3, 'amber')],
    bumpers=[(59.5, 15, 5, 3, 'chrome'), (-2, 15, 5, 3, 'chrome')], extras=kombi_extras, grille=False, mirror_dx=1, head_t=0.35,
)

# ---------------------------------------------------------------- UNO MILLE
def uno_extras(L, I, meta, ctx):
    SX, OY, gb, bot = ctx['SX'], ctx['OY'], ctx['gb'], ctx['bot']
    # friso lateral preto de proteção
    for x in range(12 + SX, 47 + SX):
        y = 13 + OY
        if ctx['paint'][y, x] and not ctx['out'][y, x]:
            L.set(x, y, BLACKP[1]); L.set(x, y - 1, P1)
    # coluna B preta
    for y in range(3 + OY, gb):
        x = 17 + SX
        if ctx['paint'][y, x] and not ctx['out'][y, x]: L.set(x, y, BLACKP[2])
    # grade/escudo
    L.rect(54 + SX, 13 + OY, 2, 2, OUT)
    L.set(54 + SX, 13 + OY, hexc('4a4d55'))

UNO = dict(
    name='uno', bodies=['UNO'],
    body=[1,18, 0.3,12, 0.5,6.5, 2,2.2, 4,1.6, 34,1.6, 36.5,2.6, 43.5,8.4, 45,9.0, 52,9.8, 54.6,11, 55.6,13.5, 55.6,18],
    glass=[[3.6,8.2, 2.9,3.7, 16.4,3.3, 16.4,8.2], [18.2,8.2, 18.2,3.3, 34.4,3.1, 40.8,8.2]],
    doors=[17.4, 43], handle_x=[19.5], wheel_x=[10.5, 44.5], D=9, nose_y=11, tail_y=6.5,
    lights=[('head', 52.4, 10.2, 3, 2, 'rect'), ('blink', 52.6, 12.6, 2, 1), ('tail', 0.5, 6.5, 2, 6, 'amber')],
    bumpers=[(51, 15, 6, 3, 'black'), (-1, 15, 5, 3, 'black')], extras=uno_extras, head_t=0.42,
)

# ---------------------------------------------------------------- GOL QUADRADO
def gol_extras(L, I, meta, ctx):
    SX, OY, gb, bot = ctx['SX'], ctx['OY'], ctx['gb'], ctx['bot']
    for x in range(10 + SX, 46 + SX):
        y = 13 + OY
        if ctx['paint'][y, x] and not ctx['out'][y, x]:
            L.set(x, y, BLACKP[1]); L.set(x, y - 1, P1)
    # coluna B/C preta
    for y in range(3 + OY, gb):
        for x in (15 + SX, ):
            if ctx['paint'][y, x] and not ctx['out'][y, x]: L.set(x, y, BLACKP[2])
    # grade preta com emblema
    L.rect(52 + SX, 13 + OY, 3, 2, OUT); L.set(53 + SX, 13 + OY, CHROME[1])

GOL = dict(
    name='gol', bodies=['GOL'],
    body=[1,18, 0.3,12, 1.0,10.2, 9.2,3.2, 12,2.6, 33,2.4, 35.5,3.4, 42,8.6, 44,9.2, 51,9.8, 53.6,11, 54.6,13.2, 54.6,18],
    glass=[[7,8.2, 10.6,3.9, 15.2,3.6, 15.2,8.2], [16.6,8.2, 16.6,3.6, 32.4,3.4, 39.2,8.2]],
    doors=[16, 41.5], handle_x=[18.5], wheel_x=[10.5, 44], D=9, nose_y=11, tail_y=9.6,
    lights=[('head', 50, 10.2, 4, 2, 'rect'), ('blink', 51, 12.6, 2, 1), ('tail', 0.4, 9.6, 2, 3, 'amber')],
    bumpers=[(50, 15, 6, 3, 'black'), (-1, 15, 5, 3, 'black')], extras=gol_extras, head_t=0.42,
)

# ---------------------------------------------------------------- CORSA
def corsa_extras(L, I, meta, ctx):
    SX, OY, gb, bot = ctx['SX'], ctx['OY'], ctx['gb'], ctx['bot']
    for x in range(11 + SX, 45 + SX):
        y = 13 + OY
        if ctx['paint'][y, x] and not ctx['out'][y, x]: L.set(x, y, P3); L.set(x, y + 1, P1)
    # faixa preta no para-choque
    L.hline(50 + SX, 16 + OY, 5, BLACKP[1]); L.hline(-1 + SX, 16 + OY, 5, BLACKP[1])
    L.rect(51 + SX, 12 + OY, 3, 1, OUT)

CORSA = dict(
    name='corsa', bodies=['COMPACT'],
    body=[1,18, 0.2,12.5, 0.8,9.8, 2.4,8.2, 5.2,5.4, 9.6,3.4, 14,2.5, 31,2.3, 36.8,3.2, 42.5,6.4, 44.4,8.0, 48.4,8.8, 51.6,9.6, 53.4,11.2, 54.2,13.4, 54.2,18],
    glass=[[7.6,8.0, 10.4,4.8, 14.4,3.6, 14.4,8.0], [15.6,8.0, 15.6,3.4, 31.4,3.3, 36,4.6, 41.4,8.0]],
    doors=[15, 41], handle_x=[17.5], wheel_x=[10.5, 43.5], D=9, nose_y=11.2, tail_y=9.4,
    lights=[('head', 49.4, 9.8, 4, 2, 'rect'), ('tail', 0.4, 9.4, 2, 4, 'amber')],
    bumpers=[(50, 15, 5, 3, 'body'), (-1, 15, 5, 3, 'body')], extras=corsa_extras, head_t=0.40,
)

# ---------------------------------------------------------------- OPALA
def opala_extras(L, I, meta, ctx):
    SX, OY, gb, bot = ctx['SX'], ctx['OY'], ctx['gb'], ctx['bot']
    # friso cromado da cintura e do meio da porta
    for x in range(2 + SX, 64 + SX):
        if ctx['paint'][gb, x] and not ctx['out'][gb, x]: L.set(x, gb, CHROME[1])
        y = 13 + OY
        if ctx['paint'][y, x] and not ctx['out'][y, x]: L.set(x, y, CHROME[2]); L.set(x, y - 1, P0)
    # grade cromada
    L.rect(62 + SX, 12 + OY, 4, 4, OUT)
    for k in range(3): L.hline(62 + SX, 12 + OY + k * 1 + (1 if k else 0), 4, CHROME[1] if k % 2 == 0 else CHROME[3])
    # alça da maçaneta traseira
    L.set(20 + SX, gb + 3, CHROME[0]); L.set(21 + SX, gb + 3, CHROME[1])
    # garras do para-choque
    L.vline(61 + SX, 14 + OY, 4, CHROME[1]); L.vline(1 + SX, 14 + OY, 4, CHROME[1])

OPALA = dict(
    name='opala', bodies=['OPALA'],
    body=[1,18, 0.4,12, 0.9,10.2, 1.8,9.4, 12,9.0, 18.6,4.0, 38,3.5, 46,8.8, 47,9.2, 62,10.0, 64.4,10.8, 65.4,13, 65.4,18],
    glass=[[13.8,8.4, 18.8,4.6, 26.6,4.2, 26.6,8.4], [28.2,8.4, 28.2,4.2, 37.2,4.0, 44.0,8.4]],
    doors=[27.4, 46.6], handle_x=[29.6], wheel_x=[15, 53], D=9, nose_y=10.8, tail_y=9.4, two_tone_y=8.4, top_ramp=VINYL_RAMP,
    lights=[('head', 62, 10.2, 3, 2, 'rect'), ('blink', 62, 12.6, 3, 1), ('tail', 0.6, 9.6, 3, 3, 'amber')],
    bumpers=[(60, 15, 7, 3, 'chrome'), (-1, 15, 6, 3, 'chrome')], extras=opala_extras, grille=False, head_t=0.50,
)

# ---------------------------------------------------------------- TÁXI (sedã tipo Voyage/Siena), amarelo e azul do Rio
def taxi_extras(L, I, meta, ctx):
    SX, OY, gb, bot = ctx['SX'], ctx['OY'], ctx['gb'], ctx['bot']
    # faixa azul na lateral
    blue = [hexc('4f8cff'), hexc('1f5bd6'), hexc('14388f')]
    for x in range(2 + SX, 58 + SX):
        for k, y in enumerate((gb + 6, gb + 7)):
            if ctx['paint'][y, x] and not ctx['out'][y, x]: L.set(x, y, blue[k])
    # luminoso "TÁXI" no teto
    tx, ty = 25 + SX, ctx['spec']['sign_y'] + OY
    L.rect(tx - 1, ty - 1, 10, 4, OUT)
    L.rect(tx, ty, 8, 2, hexc('fff6c2'))
    L.hline(tx, ty, 8, hexc('ffffff'))
    for i in range(4):
        L.set(tx + 1 + i * 2, ty + 1, hexc('b8860b'))
    L.vline(tx + 2, ty + 2, 1, OUT); L.vline(tx + 5, ty + 2, 1, OUT)

TAXI = dict(
    name='taxi', bodies=[],
    body=[1,18, 0.4,11.4, 1.6,9.4, 12.5,8.9, 18.5,3.6, 36,3.2, 44.5,8.6, 47,9.1, 55,9.8, 58.4,11.4, 59.4,13.4, 59.4,18],
    glass=[[15.5,8.3, 19.3,4.2, 26.6,4.0, 26.6,8.3], [28,8.3, 28,4.0, 36,3.8, 42.4,8.3]],
    doors=[27.3, 44.8], handle_x=[29.5], wheel_x=[14, 48], D=9, nose_y=11.4, tail_y=9.6, sign_y=1.6,
    lights=[('head', 55.4, 10.2, 3, 2, 'rect'), ('tail', 0.4, 9.6, 2, 3, 'amber')],
    bumpers=[(55.5, 15, 5, 3, 'black'), (-1, 15, 5, 3, 'black')], extras=taxi_extras, head_t=0.45,
)

HEROES = [FUSCA, KOMBI, UNO, GOL, CORSA, OPALA]
