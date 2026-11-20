import numpy as np
from px import *

DARK = hexc('15161c')
OUT = hexc('1a1b22')
CHROME = [hexc('f4f8fa'), hexc('c9d3d8'), hexc('8a98a1'), hexc('4d5a63')]
BLACKP = [hexc('555961'), hexc('33363d'), hexc('1f2126')]
WHITE_RAMP = [hexc('ffffff'), hexc('f0f4f6'), hexc('dde4e8'), hexc('a9b5be'), hexc('6f7c88')]
VINYL_RAMP = [hexc('4a4d55'), hexc('363941'), hexc('2a2c33'), hexc('202228'), hexc('15161a')]

def wheel_geom(px_x, D, GY):
    x0 = int(round(px_x - D / 2.0))
    cy = GY - D / 2.0
    return x0, GY - D, x0 + D / 2.0, cy

def head_light(img, x, y, w, h, shape='rect'):
    lens = hexc('fff2b0'); lens_lo = hexc('e2b84a')
    if shape == 'round':
        img.rect(x - 1, y, w + 2, h, OUT); img.rect(x, y - 1, w, h + 2, OUT)
        img.rect(x, y, w, h, hexc('8a98a1'))
        img.rect(x + 1, y + 1 if h > 2 else y, max(1, w - 2), max(1, h - 2) if h > 2 else h, lens)
        img.set(x, y, hexc('c9d3d8'))
        return
    img.rect(x - 1, y - 1, w + 2, h + 2, OUT)
    img.rect(x, y, w, h, lens)
    img.hline(x, y + h - 1, w, lens_lo)
    img.set(x, y, hexc('ffffff'))
    if w >= 3: img.set(x + 1, y, hexc('fffbe6'))

def tail_light(img, x, y, w, h, amber=False):
    img.rect(x - 1, y - 1, w + 2, h + 2, OUT)
    img.rect(x, y, w, h, hexc('d32727'))
    if w > 1: img.vline(x, y, h, hexc('ff6b5a'))
    img.hline(x, y + h - 1, w, hexc('7d1414'))
    if amber and h >= 4:
        img.rect(x, y, w, 1, hexc('ffb13b'))

def blinker(img, x, y, w, h):
    img.rect(x, y, w, h, hexc('ff9d1c'))
    img.set(x, y, hexc('ffd27a'))

def bumper(img, x, y, w, h, style='black'):
    if style == 'chrome':
        img.rect(x, y, w, h, CHROME[1])
        img.hline(x, y, w, CHROME[0])
        if h > 2: img.hline(x, y + h - 2, w, CHROME[2])
        img.hline(x, y + h - 1, w, CHROME[3])
        img.set(x + 1, y + 1 if h > 2 else y, hexc('ffffff'))
    elif style == 'body':
        img.rect(x, y, w, h, P2)
        img.hline(x, y, w, P0)
        img.hline(x, y + h - 1, w, P4)
    else:
        img.rect(x, y, w, h, BLACKP[1])
        img.hline(x, y, w, BLACKP[0])
        img.hline(x, y + h - 1, w, BLACKP[2])

def stretch_spec(spec, vs, min_top=-3.0):
    """estica verticalmente acima da soleira (y=18) pra dar proporção de carro de verdade."""
    top = min(spec['body'][1::2])
    vs = min(vs, (18 - min_top) / max(0.1, (18 - top)))
    def sy(y): return 18 - (18 - y) * vs if y < 18 else y
    def tf(p): return [v if i % 2 == 0 else sy(v) for i, v in enumerate(p)]
    s = dict(spec)
    s['body'] = tf(spec['body']); s['glass'] = [tf(g) for g in spec['glass']]
    return s

def render_car(spec):
    W = spec.get('W', 80); H = spec.get('H', 28); GY = spec.get('GY', 26)
    OY = GY - 22
    xs_ = spec['body'][0::2]
    SX = int(round(W / 2.0 - (min(xs_) + max(xs_)) / 2.0))
    body_poly = spec['body']; glass_polys = spec['glass']
    D = spec.get('D', 9)
    m_body = poly_mask(W, H, body_poly, SX, OY)
    m_glass = np.zeros((H, W), bool)
    for g in glass_polys:
        m_glass |= poly_mask(W, H, g, SX, OY)
    m_glass &= m_body

    wheels = []; centers = []
    arch = np.zeros((H, W), bool)
    ra = D / 2.0 + spec.get('arch_pad', 1.3)
    for ux in spec['wheel_x']:
        x0, y0, cx, cy = wheel_geom(ux + SX, D, GY)
        wheels.append((x0, y0)); centers.append((cx, cy))
        arch |= circle_mask(W, H, cx, cy, ra)
    ys, xs = np.mgrid[0:H, 0:W]
    m_sil = m_body & ~(arch & (ys >= 8 + OY))
    paint = m_sil & ~m_glass
    out = edge_mask(m_sil)

    gb = int(round(max(max(g[1::2]) for g in glass_polys))) + OY
    gt = int(round(min(min(g[1::2]) for g in glass_polys))) + OY
    bot = 18 + OY

    L = Img(W, H); I = Img(W, H)
    up_out = shift(out, 0, 1)
    up_out2 = shift(out, 0, 2)
    ramp_main = PAINT
    two_tone = spec.get('two_tone_y')          # y (unit) acima do qual usa a cor 2 (teto)
    top_ramp = spec.get('top_ramp')            # rampa fixa (branco, vinil)
    tt_y = (two_tone + OY) if two_tone is not None else None

    def pick(idx, y):
        if tt_y is not None and y < tt_y and top_ramp is not None:
            return top_ramp[idx]
        return ramp_main[idx]

    for y in range(H):
        for x in range(W):
            if not paint[y, x]: continue
            if out[y, x]:
                if y >= bot - 2: c = DARK
                else:
                    c = pick(4, y)
                    if tt_y is not None and y >= tt_y: c = P4
                L.set(x, y, c); continue
            rel = y - gb
            if y < gb:
                idx = 2
                if up_out[y, x]: idx = 0
                elif up_out2[y, x]: idx = 1
            else:
                if rel == 0: idx = 2
                elif rel == 1: idx = 0
                elif rel == 2: idx = 1
                elif y >= bot - 2: idx = 4
                elif y >= bot - 4: idx = 3
                else: idx = 2
                if up_out[y, x]: idx = 0
                elif up_out2[y, x] and rel > 2: idx = 1
            L.set(x, y, pick(idx, y))

    # anel escuro nos arcos + lábio de luz
    for y in range(H):
        for x in range(W):
            if m_sil[y, x] and not m_glass[y, x]:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    xx, yy = x + dx, y + dy
                    if 0 <= xx < W and 0 <= yy < H and arch[yy, xx] and m_body[yy, xx] and yy >= 8 + OY:
                        L.set(x, y, DARK); break
    for y in range(1, H):
        for x in range(W):
            if L.get(x, y) == DARK and y < bot - 3 and L.get(x, y - 1) in (P2, P1, P0, WHITE_RAMP[2], VINYL_RAMP[2]):
                L.set(x, y - 1, P1 if L.get(x, y - 1) in (P2, P1, P0) else L.get(x, y - 1))

    # vidros
    gedge = edge_mask(m_glass)
    for y in range(H):
        for x in range(W):
            if not m_glass[y, x]: continue
            r = y - gt
            if gedge[y, x]:
                L.set(x, y, (22, 26, 34, 255)); continue
            if r <= 1: c = (96, 150, 178, 120)
            elif r <= 2: c = (60, 100, 130, 140)
            else: c = (14, 24, 34, 150)
            if ((x - 2 * y) % 13) in (0, 1) and r > 1: c = (255, 255, 255, 78)
            elif ((x - 2 * y) % 13) == 6 and r > 1: c = (255, 255, 255, 36)
            L.set(x, y, c)
            I.set(x, y, hexc('222d3a') if y < gb - 1 else hexc('141b24'))

    # portas (costuras + maçaneta)
    doors = [d + SX for d in spec['doors']]
    if not spec.get('no_doors'):
        for i, xx in enumerate(doors):
            xx = int(round(xx))
            for y in range(gb + 1, bot - 1):
                if 0 <= xx < W and paint[y, xx] and not out[y, xx]:
                    L.set(xx, y, P4)
                    if xx + 1 < W and paint[y, xx + 1] and L.get(xx + 1, y) in (P2, P1) and 1 < y - gb < bot - gb - 3:
                        L.set(xx + 1, y, P1)
        hx = spec.get('handle_x')
        for hxu in (hx if hx else [doors[0] + 2 - SX]):
            hxx = int(round(hxu + SX))
            if 0 <= hxx < W - 1 and paint[gb + 3, hxx] and paint[gb + 3, hxx + 1]:
                L.set(hxx, gb + 3, hexc('d6dde1')); L.set(hxx + 1, gb + 3, hexc('9ba7ae'))
                L.set(hxx, gb + 4, P4); L.set(hxx + 1, gb + 4, P4)

    # espelho
    fg = max(glass_polys, key=lambda g: max(g[0::2]))
    mx = int(round(max(fg[0::2] ) + SX)) - 2 + spec.get('mirror_dx', 0)
    L.rect(mx, gb - 1, 3, 2, P2); L.hline(mx, gb - 1, 3, P0); L.hline(mx, gb, 3, OUT)

    nose_y = int(round(spec['nose_y'] + OY)); tail_y = int(round(spec['tail_y'] + OY))
    front_x = int(round(max(xs_) + SX)); rear_x = int(round(min(xs_) + SX))

    # luzes e para-choques via spec
    for ln in spec.get('lights', []):
        kind = ln[0]; lx, ly, lw, lh = [int(round(v)) for v in ln[1:5]]
        shape = ln[5] if len(ln) > 5 else 'rect'
        lx += SX; ly += OY
        if kind == 'head': head_light(L, lx, ly, lw, lh, shape)
        elif kind == 'tail': tail_light(L, lx, ly, lw, lh, shape == 'amber')
        elif kind == 'blink': blinker(L, lx, ly, lw, lh)
        elif kind == 'rect':
            L.rect(lx, ly, lw, lh, shape if isinstance(shape, tuple) else OUT)
    for bm in spec.get('bumpers', []):
        bx, by, bw, bh, bs = bm
        bumper(L, int(round(bx + SX)), int(round(by + OY)), int(bw), int(bh), bs)

    # motorista: cabeça no vidro da frente
    fgm = poly_mask(W, H, fg, SX, OY)
    ys_f, xs_f = np.where(fgm)
    gx0, gx1 = xs_f.min(), xs_f.max()
    head_x = int(gx0 + (gx1 - gx0) * spec.get('head_t', 0.46)) - 1
    head_y = int(ys_f.min()) + 1
    if spec.get('head_y') is not None: head_y = int(spec['head_y'] + OY)

    glass_box = (int(xs_f.min()), int(ys_f.min()), int(xs_f.max()), int(ys_f.max()))
    head_rect = None; tail_rect = None
    for ln in spec.get('lights', []):
        r_ = (int(round(ln[1])) + SX, int(round(ln[2])) + OY, int(round(ln[3])), int(round(ln[4])))
        if ln[0] == 'head' and head_rect is None: head_rect = r_
        if ln[0] == 'tail' and tail_rect is None: tail_rect = r_
    Lx = max(xs_) - min(xs_)
    cand = [(body_poly[i] + SX, body_poly[i + 1] + OY) for i in range(0, len(body_poly), 2) if body_poly[i] < min(xs_) + 0.3 * Lx and body_poly[i + 1] < 17]
    wing = min(cand, key=lambda p: p[1]) if cand else (rear_x + 4, bot - 8)
    meta = dict(
        W=W, H=H, cx=W // 2, gy=GY, D=D,
        wheels=wheels, arch_r=ra, centers=centers, bot=bot, gb=gb, gt=gt,
        head_xy=(head_x, head_y), glass_box=glass_box,
        door=(int(round(doors[0])), gb + 2, int(round(doors[1])) if len(doors) > 1 else int(round(doors[0])) + 8, bot - 2),
        deck=(int(round(body_poly[4] + SX)), int(round(body_poly[5] + OY))),
        hood_y=int(round(body_poly[-5] + OY)), front_x=front_x, rear_x=rear_x,
        nose_y=nose_y, tail_y=tail_y, SX=SX, OY=OY, cab=True,
        hl=(front_x - 2, nose_y + 2), tl=(rear_x + 1, tail_y + 2),
        head_rect=head_rect, tail_rect=tail_rect, wing=(int(round(wing[0])), int(round(wing[1]))),
        exhaust=(rear_x, bot - 1), body_top=int(min(body_poly[1::2]) + OY),
    )
    if spec.get('extras'):
        spec['extras'](L, I, meta, dict(paint=paint, out=out, m_glass=m_glass, SX=SX, OY=OY, gb=gb, bot=bot, spec=spec))
    return L, I, meta

def sheet(items, path, k=5, bg=(120, 150, 185, 255), cols=3, pad=6):
    n = len(items)
    rows = (n + cols - 1) // cols
    cw = max(i.w for i, _ in items) * k + pad
    ch = max(i.h for i, _ in items) * k + pad
    out = Image.new('RGBA', (cw * cols + pad, ch * rows + pad), bg)
    for idx, (im, label) in enumerate(items):
        r, c = divmod(idx, cols)
        out.alpha_composite(scale_nn(im, k), (pad + c * cw, pad + r * ch))
    out.convert('RGB').save(path)
