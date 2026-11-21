import math
from px import *

TIRE = hexc('15161a'); TIRE_HI = hexc('3a3d45'); TIRE_WORN = hexc('474b51'); TIRE_WORN_HI = hexc('6a6f76')
STEEL = hexc('a7b0b7'); STEEL_D = hexc('59616a'); STEEL_L = hexc('e3e8ec')
SPORT = hexc('ffc107'); SPORT_L = hexc('ffe9a0'); SPORT_D = hexc('7a5a00'); SPORT_C = hexc('3a2e00')
RUST = hexc('8a5f45'); RUST_D = hexc('4a3326'); RUST_L = hexc('b98a60')

def wheel_frame(D, kind, worn, frame, blur=False):
    """kind: 0 aço, 1 esportiva, 2 enferrujada. frame 0..5 (12 graus cada)."""
    im = Img(D, D)
    c = D / 2.0
    R = D / 2.0
    tire = TIRE_WORN if worn else TIRE
    tire_hi = TIRE_WORN_HI if worn else TIRE_HI
    rim_r = {0: 0.62, 1: 0.74, 2: 0.62}[kind] * R
    hub = [STEEL, SPORT, RUST][kind]
    hub_l = [STEEL_L, SPORT_L, RUST_L][kind]
    hub_d = [STEEL_D, SPORT_D, RUST_D][kind]
    for y in range(D):
        for x in range(D):
            d = math.hypot(x + 0.5 - c, y + 0.5 - c)
            if d <= R:
                im.set(x, y, tire)
                if d > R - 1.1 and x + y < D - 1: im.set(x, y, tire_hi)   # luz no canto superior esquerdo
            if d <= rim_r:
                im.set(x, y, hub)
    n = 6 if kind == 1 else 5
    if blur:
        for y in range(D):
            for x in range(D):
                d = math.hypot(x + 0.5 - c, y + 0.5 - c)
                if d <= rim_r: im.set(x, y, hub_d if (x + y) % 2 == 0 else hub)
                if d <= rim_r * 0.45: im.set(x, y, hub_l)
        return im
    ang0 = math.radians(frame * (360.0 / n) / 6.0 + 18)
    for k in range(n):
        a = ang0 + k * 2 * math.pi / n
        steps = int(rim_r * 2)
        for s in range(1, steps + 1):
            t = s / (steps + 0.0) * (rim_r - 0.2)
            px_ = int(c + math.cos(a) * t - 0.0)
            py_ = int(c + math.sin(a) * t - 0.0)
            im.set(px_, py_, hub_d)
    # miolo
    im.set(int(c), int(c), hub_l)
    if D >= 11:
        im.set(int(c) - 1, int(c), hub_l); im.set(int(c), int(c) - 1, hub_l)
    return im

def flat_frame(D, kind):
    im = Img(D, D)
    h = D - 3
    for y in range(D - h, D):
        for x in range(D):
            nx = (x + 0.5 - D / 2.0) / (D / 2.0 + 0.4)
            ny = (y + 0.5 - (D - h / 2.0)) / (h / 2.0)
            if nx * nx + ny * ny <= 1.0:
                im.set(x, y, TIRE_WORN)
    hub = [STEEL, SPORT, RUST][kind]
    for y in range(D - 4, D - 1):
        for x in range(int(D / 2) - 2, int(D / 2) + 2):
            im.set(x, y, hub)
    im.set(int(D / 2), D - 3, [STEEL_L, SPORT_L, RUST_L][kind])
    im.hline(1, D - 1, D - 2, hexc('2a2b30'))
    return im

# linhas do atlas: (kind, worn)
ROWS = [(0, False), (0, True), (1, False), (1, True), (2, False), (2, True), 'flat0', 'flat1', 'flat2']

def make_atlas(D):
    cols = 7
    atlas = Img(D * cols, D * len(ROWS))
    for r, spec in enumerate(ROWS):
        for cidx in range(cols):
            if isinstance(spec, str):
                fr = flat_frame(D, int(spec[-1]))
            else:
                kind, worn = spec
                fr = wheel_frame(D, kind, worn, cidx % 6, blur=(cidx == 6))
            atlas.blit(fr, cidx * D, r * D)
    return atlas

if __name__ == '__main__':
    import sys
    for D in (9, 11, 13):
        a = make_atlas(D)
        a.save(f'/home/claude/gen/out_wheels_{D}.png')
        scale_nn(a, 6).save(f'/home/claude/gen/prev_wheels_{D}.png')
    print('ok')
