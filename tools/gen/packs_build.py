"""Converte os recortes (packs_cars.py) em sprites do jogo:
   pb<k>.png  carroceria (rodas recortadas, pintura codificada em (v,0,v), vidro semitransparente)
   pi<k>.png  interior (fundo escuro do vidro)
   pw<k>.png  atlas das rodas originais do carro: 7 colunas (6 giros + borrado) x 4 linhas (normal, gasta, enferrujada, furada)
 e gera PackData.kt com as medidas. Uso: python3 packs_build.py <cut_dir> <assets_dir> <PackData.kt>"""
import os, sys, json, math
import numpy as np, cv2
from PIL import Image
from packs_cars import cells

SCALE = {'br': 0.36, 'lx': 0.30}
FRAMES = 6

def lum(a): return a[..., 0] * 0.299 + a[..., 1] * 0.587 + a[..., 2] * 0.114

def classify(rgb, mask, wheels, kind):
    """Retorna (paint, glass) booleanos (alta resolução). rgb float 0..255, já virado pra direita."""
    h, w = mask.shape
    hsv = cv2.cvtColor(rgb.astype(np.uint8), cv2.COLOR_RGB2HSV).astype(np.float32)
    L = lum(rgb)
    ys, xs = np.nonzero(mask)
    y0, y1, x0, x1 = ys.min(), ys.max(), xs.min(), xs.max()
    bh = y1 - y0
    # referência = flanco (faixa baixa, entre as rodas)
    wx = sorted(c[0] for c in wheels)
    ry0 = int(y0 + 0.52 * bh); ry1 = int(y0 + 0.72 * bh)
    if kind == 'moto': ry0, ry1 = int(y0 + 0.25 * bh), int(y0 + 0.55 * bh)
    band = np.zeros_like(mask, bool); band[ry0:ry1, int(wx[0] + 8):int(wx[-1] - 8)] = True
    band &= mask & (L > 28)
    if band.sum() < 30: band = mask & (L > 28)
    ref = np.median(rgb[band], axis=0)
    if lum(ref[None, None])[0, 0] < 60:   # carro quase preto: o filtro L>28 jogou fora a própria lataria
        band2 = np.zeros_like(mask, bool); band2[ry0:ry1, int(wx[0] + 8):int(wx[-1] - 8)] = True
        band2 &= mask & (L > 5)
        if band2.sum() >= 30: ref = np.median(rgb[band2], axis=0)
    ref_hsv = cv2.cvtColor(np.uint8([[ref]]), cv2.COLOR_RGB2HSV)[0, 0].astype(np.float32)
    Lref = float(lum(ref[None, None])[0, 0]); sat_ref = ref_hsv[1] / 255.0
    dh = np.minimum(np.abs(hsv[..., 0] - ref_hsv[0]), 180 - np.abs(hsv[..., 0] - ref_hsv[0])) * 2  # graus
    s = hsv[..., 1] / 255.0
    if sat_ref > 0.28:
        paint = ((dh < 34) & (s > 0.30 * sat_ref) & (L > 0.14 * Lref)) | ((dh < 50) & (L > 1.0 * Lref) & (s > 0.08))
    else:
        paint = (s < 0.32) & (L > 0.38 * Lref) & (L < 1.9 * Lref + 25)
        if Lref < 70: paint = (s < 0.35) & (L > 0.55 * Lref) & (L < 2.4 * Lref + 18)
    paint |= mask & (L > 1.05 * Lref) & (L < 250) & (np.arange(h)[:, None] < y0 + 0.8 * bh) & (sat_ref > 0.28)
    paint &= mask
    paint &= ~((np.arange(h)[:, None] > (np.mean([c[1] for c in wheels]) + 0.45 * np.mean([c[2] for c in wheels]))) & (L < 0.5 * Lref))
    # vidro: região da cabine (metade de cima), escura ou azulada e que não é pintura "clara"
    cab = np.zeros_like(mask, bool)
    cab[y0:int(y0 + 0.62 * bh), x0:x1 + 1] = True
    if kind == 'moto':
        return paint, np.zeros_like(mask, bool), Lref
    blue = (rgb[..., 2] > rgb[..., 0] + 6)
    dark = L < max(80.0, 0.62 * Lref)
    glass = cab & mask & dark & (~paint | (Lref < 90) & blue)
    if Lref < 90:  # carro escuro: vidro = azulado/mais escuro que a lataria
        glass = cab & mask & (L < Lref * 0.85 + 6) & blue
        paint = paint & ~glass
    glass = ndi_clean(glass, 2)
    # só componentes grandes (janelas), nada de pixels soltos
    n, lab, st, _ = cv2.connectedComponentsWithStats(glass.astype(np.uint8), 8)
    g2 = np.zeros_like(glass)
    for i in range(1, n):
        if st[i, cv2.CC_STAT_AREA] < 0.0016 * mask.sum() or st[i, cv2.CC_STAT_TOP] >= y0 + 0.34 * bh: continue
        # janela de verdade tem lataria (teto) acima; caçamba aberta não
        cx0, cw, ct = st[i, cv2.CC_STAT_LEFT], st[i, cv2.CC_STAT_WIDTH], st[i, cv2.CC_STAT_TOP]
        above = mask[max(0, ct - 5):max(1, ct - 1), cx0:cx0 + cw]
        if above.size and above.mean() > 0.55: g2 |= (lab == i)
    if g2.sum() < 12:   # fallback: afrouxa o critério (sem exigir teto acima, vidro menos escuro)
        g3 = cab & mask & (L < max(110.0, 0.8 * Lref)) & ~(paint & (L > 0.9 * Lref))
        g3 = ndi_clean(g3, 2)
        n, lab, st, _ = cv2.connectedComponentsWithStats(g3.astype(np.uint8), 8)
        for i in range(1, n):
            if st[i, cv2.CC_STAT_AREA] >= 0.0008 * mask.sum() and st[i, cv2.CC_STAT_TOP] < y0 + 0.4 * bh: g2 |= (lab == i)
    paint &= ~g2
    return paint, g2, Lref

def ndi_clean(m, it):
    k = np.ones((3, 3), np.uint8)
    m = cv2.morphologyEx(m.astype(np.uint8), cv2.MORPH_OPEN, k, iterations=1)
    return cv2.morphologyEx(m, cv2.MORPH_CLOSE, k, iterations=it).astype(bool)

def rot_frames(wimg, d, n=FRAMES, step=12.0):
    """wimg: RGBA hi-res quadrado da roda. Retorna lista de d x d RGBA (uint8) giradas."""
    hh, ww = wimg.shape[:2]; c = (ww / 2, hh / 2)
    out = []
    for k in range(n):
        M = cv2.getRotationMatrix2D(c, -k * step, 1.0)
        r = cv2.warpAffine(wimg, M, (ww, hh), flags=cv2.INTER_CUBIC, borderMode=cv2.BORDER_REPLICATE)
        out.append(r)
    return out

def disc_mask(d):
    yy, xx = np.mgrid[:d, :d]
    return (np.hypot(yy + 0.5 - d / 2, xx + 0.5 - d / 2) <= d / 2)

def to_lo(img, d):
    return cv2.resize(img, (d, d), interpolation=cv2.INTER_AREA)

def clean_wheel(crop):
    """Fora do miolo, o pneu é escuro e neutro: tira o brilho colorido do fundo do poster."""
    n = crop.shape[0]; c = (n - 1) / 2.0
    yy, xx = np.mgrid[:n, :n]; dist = np.hypot(yy - c, xx - c) / (n / 2.0)
    L = lum(crop)[..., None]
    out = crop.copy()
    ring = (dist > 0.78)[..., None]
    out = np.where(ring, np.minimum(L, 70) * np.ones(3), out)
    return out

def wheel_atlas(crop_rgb, d):
    """crop_rgb: hi-res quadrado (RGB) com a roda centrada. 7 colunas x 4 linhas."""
    crop_rgb = clean_wheel(crop_rgb)
    dm = disc_mask(d)
    base = rot_frames(crop_rgb.astype(np.float32), d)
    fr = [to_lo(f, d) for f in base]
    blur = np.mean([to_lo(f, d) for f in rot_frames(crop_rgb.astype(np.float32), d, 12, 30.0)], axis=0)
    # roda borrada = pneu + miolo médio
    cols = fr + [blur]
    atlas = np.zeros((4 * d, 7 * d, 4), np.uint8)
    for r in range(4):
        for c, f in enumerate(cols):
            f = f.copy()
            if r == 1:    # pneu gasto: mais claro/acinzentado, rodas sujas
                f = f * 0.80 + np.array([40, 36, 30]) * 0.20
            elif r == 2:  # roda enferrujada
                L = lum(f)[..., None]; rust = np.array([150, 80, 34])
                f = np.where(L > 70, f * 0.45 + rust * 0.55, f)
            sprite = np.zeros((d, d, 4), np.uint8)
            sprite[..., :3] = np.clip(f, 1, 255).astype(np.uint8); sprite[..., 3] = dm * 255
            if r == 3:    # furada: achatada (1px mais baixa) com a borda do pneu mais larga
                sh = np.zeros_like(sprite)
                sq = cv2.resize(sprite[..., :3], (d, d - max(1, d // 6)), interpolation=cv2.INTER_AREA)
                al = (cv2.resize(sprite[..., 3], (d, d - max(1, d // 6)), interpolation=cv2.INTER_AREA) > 100) * 255
                sh[d - sq.shape[0]:, :, :3] = sq; sh[d - sq.shape[0]:, :, 3] = al
                sprite = sh
            atlas[r * d:(r + 1) * d, c * d:(c + 1) * d] = sprite
    return atlas

def top_y(alpha, x):
    col = alpha[:, x] > 0
    ys = np.nonzero(col)[0]
    return int(ys.min()) if len(ys) else -1

def build_one(id_, nm, kind, pk, rgba, wheels):
    S = SCALE[pk]
    h, w = rgba.shape[:2]
    # vira pra direita
    rgba = rgba[:, ::-1].copy()
    wheels = [(w - cx, cy, r) for cx, cy, r, _ in wheels]
    wheels.sort()
    mask = rgba[..., 3] > 0
    rgb = rgba[..., :3].astype(np.float32)
    r = float(np.mean([x[2] for x in wheels])); cy = float(np.mean([x[1] for x in wheels]))
    gy_hi = int(round(cy + r))
    mask[gy_hi + 1:] = False                       # corta sombra do chão
    if kind != 'moto':                             # e a sombra que sobra embaixo da lataria
        k = 0.88 if kind in ('bus', 'truck', 'van') else 0.78
        mask[int(cy + k * r):] = False
    mask = cv2.erode(mask.astype(np.uint8), np.ones((3, 3), np.uint8)).astype(bool)
    paint, glass, Lref = classify(rgb, mask, wheels, kind)
    # ---- reduz
    wl, hl = max(8, round(w * S)), max(8, round(h * S))
    sx, sy = wl / w, hl / h
    def rs(a): return cv2.resize(a.astype(np.float32), (wl, hl), interpolation=cv2.INTER_AREA)
    m_f = rs(mask); 
    pre = rs(rgb * mask[..., None])
    alpha = m_f > 0.55
    col = np.where(m_f[..., None] > 0.01, pre / np.maximum(m_f[..., None], 0.01), 0)
    pl = rs(paint) > 0.5 * np.maximum(m_f, 0.01)
    gl = rs(glass) > 0.5 * np.maximum(m_f, 0.01)
    pl &= alpha; gl &= alpha; pl &= ~gl
    # ---- rodas (lo-res)
    d = int(round(2 * r * sx))
    d = max(8, d)
    wheels_lo = []
    for cx, cyy, rr in wheels[:2]:
        lx, ly = cx * sx, cyy * sy
        wheels_lo.append((int(round(lx - d / 2)), int(round(ly - d / 2))))
    atlas_src = []
    for cx, cyy, rr in wheels[:2]:
        R = int(math.ceil(r)) + 1
        pad = cv2.copyMakeBorder(rgb, R + 2, R + 2, R + 2, R + 2, cv2.BORDER_REPLICATE)
        icx, icy = int(round(cx)) + R + 2, int(round(cyy)) + R + 2
        atlas_src.append(pad[icy - R:icy + R + 1, icx - R:icx + R + 1])
    atlas = wheel_atlas(atlas_src[1], d)   # roda de trás-esquerda costuma ter sombra do pára-lama; usa a da frente
    dm = disc_mask(d)
    for (wx0, wy0) in wheels_lo:
        for yy in range(d):
            for xx in range(d):
                if dm[yy, xx]:
                    X, Y = wx0 + xx, wy0 + yy
                    if 0 <= X < wl and 0 <= Y < hl: alpha[Y, X] = False
    pl &= alpha; gl &= alpha
    # ---- recorta o canvas
    ys, xs = np.nonzero(alpha)
    ax0, ax1 = min(xs.min(), min(p[0] for p in wheels_lo)), max(xs.max() + 1, max(p[0] for p in wheels_lo) + d)
    ay0, ay1 = ys.min(), max(ys.max() + 1, max(p[1] for p in wheels_lo) + d)
    MX, MT, MB = 3, 3, 3
    W = ax1 - ax0 + 2 * MX; H = ay1 - ay0 + MT + MB
    def crop(a, fill=0):
        out = np.full((H, W) + a.shape[2:], fill, a.dtype)
        sy0, sx0 = ay0, ax0
        sub = a[sy0:ay1, sx0:ax1]
        out[MT:MT + sub.shape[0], MX:MX + sub.shape[1]] = sub
        return out
    alpha_c, pl_c, gl_c, col_c = crop(alpha), crop(pl), crop(gl), crop(col)
    wheels_c = [(x - ax0 + MX, y - ay0 + MT) for x, y in wheels_lo]
    gy = wheels_c[0][1] + d
    # ---- codifica
    Lc = lum(col_c)
    out = np.zeros((H, W, 4), np.uint8)
    v = np.clip(np.round(128 * Lc / max(Lref, 1)), 1, 255)
    pix = np.clip(col_c, 0, 255).astype(np.uint8)
    pix[..., 1] = np.maximum(pix[..., 1], 1)
    out[..., :3] = pix
    pm = pl_c & alpha_c
    out[pm, 0] = v[pm].astype(np.uint8); out[pm, 1] = 0; out[pm, 2] = v[pm].astype(np.uint8)
    out[..., 3] = np.where(alpha_c, 255, 0)
    out[gl_c & alpha_c, 3] = 125
    # contorno externo da pintura mais escuro (dá "pixel art" depois do recolor)
    # ---- interior
    inter = np.zeros((H, W, 4), np.uint8)
    gys, gxs = np.nonzero(gl_c)
    synth = None
    if kind != 'moto':
        _ys, _xs = np.nonzero(alpha_c)
        _L = _xs.max() - _xs.min(); _top = _ys.min()
        synth = (int(_xs.min() + 0.30 * _L), int(_top + 1), int(_xs.max() - 0.30 * _L), int(_top + 0.30 * (_ys.max() - _top)))
    if len(gxs) and kind != 'moto':
        gbox = (int(gxs.min()), int(gys.min()), int(gxs.max()), int(gys.max()))
        # preenche TODA a caixa do vidro com cor escura (interior) onde a carroceria for vidro; o resto fica transparente
        for (yy, xx) in zip(gys, gxs):
            t = (yy - gbox[1]) / max(1, gbox[3] - gbox[1])
            inter[yy, xx] = (int(54 - 26 * t), int(66 - 30 * t), int(80 - 36 * t), 255)
    else:
        gbox = (0, 0, 0, 0)
    if synth and (gbox[2] - gbox[0] < 10 or gbox[3] - gbox[1] < 2): gbox = synth
    # ---- metadados
    ys2, xs2 = np.nonzero(alpha_c)
    front, rear = int(xs2.max()), int(xs2.min())
    bodyL = front - rear
    # fundo da carroceria entre as rodas
    x_a = wheels_c[0][0] + d; x_b = wheels_c[-1][0]
    bot = gy - 1
    if x_b > x_a:
        cols = alpha_c[:, x_a:x_b]
        yb = [np.nonzero(cols[:, i])[0].max() for i in range(cols.shape[1]) if cols[:, i].any()]
        if yb: bot = int(np.percentile(yb, 50))
    def topy(x):
        x = int(np.clip(x, 0, W - 1)); c = np.nonzero(alpha_c[:, x])[0]
        return int(c.min()) if len(c) else MT
    hood_y = topy(front - 0.12 * bodyL)
    deck_y = topy(rear + 0.08 * bodyL)
    roofy = int(ys2.min())
    gh = gbox[3] - gbox[1]
    head = (gbox[0] + int(0.40 * (gbox[2] - gbox[0])), gbox[1] + 1)
    door = (gbox[0], min(bot - 6, gbox[3] + 2), gbox[2], max(min(bot - 4, gbox[3] + 2 + int(0.3 * (bot - gbox[3]))), gbox[3] + 4))
    if kind == 'moto': door = (rear + 8, hood_y + 2, front - 8, bot - 2)
    # faróis / lanternas
    L_c = lum(col_c); rr_, gg_, bb_ = col_c[..., 0], col_c[..., 1], col_c[..., 2]
    hy0 = hood_y; hy1 = min(bot, hood_y + int(0.55 * (bot - hood_y)) + 2)
    reg = np.zeros_like(alpha_c); reg[hy0:hy1, max(rear, front - 7):front + 1] = True
    hl_mask = reg & alpha_c & (L_c > 150) & (rr_ > 150)
    if hl_mask.sum() >= 2:
        yy, xx = np.nonzero(hl_mask); hr = (int(xx.min()), int(yy.min()), int(xx.max() - xx.min() + 1), int(yy.max() - yy.min() + 1))
    else:
        hr = (front - 3, hood_y + int(0.30 * (bot - hood_y)), 3, 2)
    hr = (hr[0], hr[1], min(max(hr[2], 2), 4), min(max(hr[3], 2), 3))
    reg = np.zeros_like(alpha_c); reg[hy0:hy1, rear:rear + 5] = True
    tl_mask = reg & alpha_c & (rr_ > 150) & (gg_ < 110) & ~pm
    if tl_mask.sum() >= 2:
        yy, xx = np.nonzero(tl_mask); tr = (int(xx.min()), int(yy.min()), int(xx.max() - xx.min() + 1), int(yy.max() - yy.min() + 1))
    else:
        tr = (rear, deck_y + 3, 2, 3)
    tr = (tr[0], tr[1], min(max(tr[2], 2), 3), min(max(tr[3], 2), 4))
    meta = dict(name=id_, disp=nm, kind=kind, w=W, h=H, cx=W // 2, gy=gy, d=d,
        wx=[p[0] for p in wheels_c], wy=[p[1] for p in wheels_c], archR=d / 2 + 0.5, bot=bot,
        head=head, glass=gbox, door=door, wing=(rear + 3, deck_y), exhaust=(rear, bot - 2),
        front=front, rear=rear, hl=(front - 1, hr[1] + hr[3] // 2), tl=(rear + 1, tr[1] + tr[3] // 2),
        headRect=hr, tailRect=tr, hoodY=hood_y, cab=(kind != 'moto'), roof=roofy)
    return out, inter, atlas, meta

if __name__ == '__main__':
    cut, assets, kt = sys.argv[1:4]
    os.makedirs(assets, exist_ok=True)
    metas = []
    for id_, nm, kind, im, box, lux, pk in cells():
        rgba = np.asarray(Image.open(f'{cut}/{id_}.png'))
        wh = json.load(open(f'{cut}/{id_}.json'))['wheels']
        k = len(metas)
        body, inter, atlas, meta = build_one(id_, nm, kind, pk, rgba, wh)
        Image.fromarray(body).save(f'{assets}/pb{k}.png'); Image.fromarray(inter).save(f'{assets}/pi{k}.png')
        Image.fromarray(atlas).save(f'{assets}/pw{k}.png')
        meta['idx'] = k; metas.append(meta)
    json.dump(metas, open(f'{cut}/metas.json', 'w'), default=int)
    print(len(metas), 'veículos')
