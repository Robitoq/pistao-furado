"""Espelha em Python o que o Kotlin faz: recolor por marcadores + ordem de camadas."""
import numpy as np
from px import *
from carlib import *
from wheels import make_atlas

_ATL = {D: make_atlas(D) for D in (9, 11, 13)}

def lerp(a, b, t): return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))

def ramp(paint):
    warm = (255, 248, 226); cool = (36, 44, 104)
    base = paint[:3]
    return {'P0': lerp(base, warm, 0.50), 'P1': lerp(base, warm, 0.20), 'P2': base,
            'P3': lerp(lerp(base, (0, 0, 0), 0.30), cool, 0.14), 'P4': lerp(lerp(base, (0, 0, 0), 0.55), cool, 0.12)}

def recolor(img, paint):
    r = ramp(paint)
    a = img.a.copy()
    for key, mk in zip(['P0', 'P1', 'P2', 'P3', 'P4'], PAINT):
        m = (a[:, :, 0] == mk[0]) & (a[:, :, 1] == mk[1]) & (a[:, :, 2] == mk[2]) & (a[:, :, 3] == 255)
        a[m, 0], a[m, 1], a[m, 2] = r[key]
    o = Img(img.w, img.h); o.a = a
    return o

def hexpaint(s): return hexc(s)[:3]

def paste(dst, src, x, y):
    for yy in range(src.h):
        for xx in range(src.w):
            p = src.a[yy, xx]; al = int(p[3])
            if al == 0: continue
            X, Y = x + xx, y + yy
            if not (0 <= X < dst.w and 0 <= Y < dst.h): continue
            if al == 255: dst.a[Y, X] = p
            else:
                d = dst.a[Y, X].astype(float); a = al / 255.0
                dst.a[Y, X, :3] = (p[:3] * a + d[:3] * (1 - a)).astype(np.uint8)
                dst.a[Y, X, 3] = max(int(d[3]), al)

def fillr(cv, x, y, w, h, c):
    x0, y0 = max(0, x), max(0, y)
    cv.a[y0:max(y0, y + h), x0:max(x0, x + w)] = c

def draw_car(canvas, ox, gy, body, interior, meta, paint, kind=0, worn=False, frame=0, driver=True,
             skin=(224, 172, 105), shirt=(30, 136, 229), dy=0, flip=False):
    """ox, gy = canto sup. esq. do sprite no canvas."""
    b = recolor(body, paint)
    # sombra (duas linhas)
    fillr(canvas, ox + 3, gy + meta['gy'], meta['W'] - 6, 1, (0, 0, 0, 80))
    if meta.get('cab', True):
        paste(canvas, interior, ox, gy + dy)
        if driver:
            hx, hy = meta['head_xy']
            fillr(canvas, ox + hx - 4, gy + dy + hy + 1, 3, 8, (55, 71, 79, 255))          # banco
            fillr(canvas, ox + hx - 1, gy + dy + hy + 3, 5, 6, shirt + (255,))             # tronco
            fillr(canvas, ox + hx, gy + dy + hy, 3, 3, skin + (255,))                      # cabeça
            fillr(canvas, ox + hx, gy + dy + hy - 1, 3, 1, (40, 30, 24, 255))              # cabelo
    for (cx, cy) in meta['centers']:
        r = int(meta['arch_r'])
        fillr(canvas, ox + int(cx - r), gy + dy + int(cy - r), 2 * r + 1, meta['bot'] - int(cy - r), (12, 13, 16, 255))
    D = meta['D']; atl = _ATL[D]
    rowi = {(0, False): 0, (0, True): 1, (1, False): 2, (1, True): 3, (2, False): 4, (2, True): 5}[(kind, worn)]
    for (wx, wy) in meta['wheels']:
        sub = Img(D, D); sub.a = atl.a[rowi * D:(rowi + 1) * D, frame * D:(frame + 1) * D].copy()
        paste(canvas, sub, ox + wx, gy + wy)
    paste(canvas, b, ox, gy + dy)
