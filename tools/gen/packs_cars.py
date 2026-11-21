"""Recorta os veículos dos posters (brasileiros + luxo) em sprites individuais (alta resolução, virados pra DIREITA).
Gera cut/<id>.png + cut/<id>.json (rodas detectadas). Uso: python3 packs_cars.py <pasta_saida>"""
import os, sys, json
import numpy as np, cv2
from PIL import Image
from scipy import ndimage as ndi

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, 'packs_src')

# (id, nome exibido, tipo, poster, caixa x0,y0,x1,y1)
BR_XC = [115, 313, 500, 681, 858, 1043, 1217, 1413]
BR_ROWS = [(150, 258), (288, 375), (415, 498), (540, 628), (668, 765)]
BR_NAMES = [
 ("fusca","Fusca","beetle"),("kombi","Kombi","van"),("uno","Uno Mille","hatch"),("gol_q","Gol Quadrado","hatch"),
 ("gol_g4","Gol G4","hatch"),("corsa","Corsa","hatch"),("palio","Palio","hatch"),("strada","Strada","pickup"),
 ("siena","Siena","sedan"),("civic","Civic","sedan"),("corolla","Corolla","sedan"),("etios","Etios","sedan"),
 ("opala","Opala","sedan"),("chevette","Chevette","hatch"),("maverick","Maverick","coupe"),("delrey","Del Rey","sedan"),
 ("voyage","Voyage","sedan"),("santana","Santana","sedan"),("monza","Monza","sedan"),("omega","Omega","sedan"),
 ("kadett","Kadett","hatch"),("logus","Logus","hatch"),("tempra","Tempra","sedan"),("p206","Peugeot 206","hatch"),
 ("hb20","HB20","hatch"),("sandero","Sandero","hatch"),("duster","Duster","suv"),("ecosport","EcoSport","suv"),
 ("tracker","Tracker","suv"),("creta","Creta","suv"),("compass","Compass","suv"),("renegade","Renegade","suv"),
 ("f1000","F-1000","pickup"),("c10","C10","pickup"),("d20","D-20","pickup"),("ranger","Ranger","pickup"),
 ("s10","S10","pickup"),("amarok","Amarok","pickup"),("hilux","Hilux","pickup"),("mb1113","Mercedes-Benz 1113","truck"),
]
BR_LAST = [  # última linha: caixas manuais
 ("onibus","Ônibus Urbano","bus",(8,800,276,910)),
 ("escolar","Ônibus Escolar","bus",(286,800,548,910)),
 ("moto_cg","Honda CG","moto",(580,805,692,905)),
 ("moto_titan","Titan 150","moto",(716,805,832,905)),
 ("moto_fan","Fan 160","moto",(868,805,984,905)),
 ("moto_biz","Biz","moto",(1014,805,1126,905)),
 ("moto_entrega","Moto Delivery","moto",(1168,800,1290,905)),
 ("van","Van","van",(1328,800,1518,910)),
]
LX_COLS = [(18,262),(280,518),(536,766),(783,1016),(1033,1268),(1286,1520)]
LX_ROWS = [(238,332),(396,490),(552,646),(712,806)]
LX_NAMES = [
 ("l_porsche","Porsche 911 Turbo","super"),("l_ferrari","Ferrari","super"),("l_lambo","Lamborghini","super"),
 ("l_bugatti","Bugatti","super"),("l_mclaren","McLaren","super"),("l_rolls","Rolls-Royce","sedan"),
 ("l_bmw","BMW M5","sedan"),("l_mercedes","Mercedes-Benz","coupe"),("l_audi","Audi R8","super"),
 ("l_lexus","Lexus","coupe"),("l_maserati","Maserati","sedan"),("l_bentley","Bentley","sedan"),
 ("l_jaguar","Jaguar","coupe"),("l_tesla","Tesla","sedan"),("l_rangerover","Range Rover","suv"),
 ("l_cadillac","Cadillac","sedan"),("l_corvette","Corvette","super"),("l_viper","Dodge Viper","super"),
 ("l_mclaren_f1","McLaren F1","super"),("l_koenigsegg","Koenigsegg","super"),("l_pagani","Pagani","super"),
 ("l_aston","Aston Martin","coupe"),("l_senna","McLaren Senna","super"),("l_revuelto","Lamborghini Revuelto","super"),
]

def load(name, mode='RGB'):
    return np.asarray(Image.open(os.path.join(SRC, name)).convert(mode)).astype(np.float32)

def seg(crop, lux):
    sm = ndi.gaussian_filter(crop, (0.8, 0.8, 0)) if lux else crop
    gx = ndi.sobel(sm, axis=1); gy = ndi.sobel(sm, axis=0)
    mag = np.sqrt((gx**2 + gy**2).sum(2))
    if lux:
        e = (mag > 110) | (sm.mean(2) > 70)
        e = ndi.binary_closing(e, iterations=2)
    else:
        e = mag > 320                      # só o contorno nítido do carro (o brilho colorido do fundo é suave)
        e = ndi.binary_closing(e, iterations=3)
    f = ndi.binary_fill_holes(e)
    f = ndi.binary_opening(f, iterations=3)
    lab, n = ndi.label(f)
    if n == 0: return f
    # componente "do centro": a que mais cobre a faixa central do recorte
    h, w = f.shape
    c0, c1 = int(w * 0.35), int(w * 0.65)
    cover = ndi.sum(f[:, c0:c1], lab[:, c0:c1], range(1, n + 1))
    return lab == (np.argmax(cover) + 1)

def find_wheels(rgb, mask, kind, lux):
    """Acha as rodas por correlação com um anel escuro (pneu). Retorna [(cx,cy,r,0)] ordenado por x."""
    g = cv2.cvtColor(rgb.astype(np.uint8), cv2.COLOR_RGB2GRAY).astype(np.float32)
    h, w = g.shape
    dark = ((g < 85) & (mask > 0)).astype(np.float32)
    edge = cv2.Canny(g.astype(np.uint8), 80, 160).astype(np.float32) / 255.0
    ys, xs = np.nonzero(mask)
    bot = ys.max()
    if kind == 'moto': rr = range(11, 20)
    elif kind in ('bus', 'truck'): rr = range(12, 21)
    elif lux: rr = range(17, 25)
    else: rr = range(10, 16)
    best = []
    for r in rr:
        k = 2 * r + 5
        yy, xx = np.mgrid[:k, :k]; d = np.hypot(yy - k // 2, xx - k // 2)
        ring = ((d <= r + 0.5) & (d >= r - 3.0)).astype(np.float32); ring /= ring.sum()
        disc = (d <= r - 4).astype(np.float32); disc /= max(1, disc.sum())
        rim = ((d <= r + 1.0) & (d >= r - 1.0)).astype(np.float32); rim /= rim.sum()
        sc = cv2.filter2D(dark, -1, ring) * 1.0 + cv2.filter2D(edge, -1, rim) * 0.6
        # centro tem que ser: base da roda perto do fundo da máscara
        for cy in range(max(0, bot - r - 14), min(h, bot - r + 3)):
            row = sc[cy]
            for cx in range(r, w - r):
                if row[cx] > 0.35:
                    fx = cx / w
                    prior = max(np.exp(-((fx - 0.21) / 0.07) ** 2), np.exp(-((fx - 0.79) / 0.07) ** 2))
                    best.append((row[cx] + 0.01 * r + 0.5 * prior, cx, cy, r))
    best.sort(reverse=True)
    top = best[:150]
    pair, ps = None, -1
    for i, (s1, x1, y1, r1) in enumerate(top):
        for s2, x2, y2, r2 in top[i + 1:]:
            if abs(r1 - r2) <= 1 and abs(y1 - y2) <= 2 and abs(x1 - x2) > 3.0 * min(r1, r2):
                if s1 + s2 > ps: ps, pair = s1 + s2, ((x1, y1, r1), (x2, y2, r2))
    if pair is None:
        pair = tuple((x, y, r) for _, x, y, r in top[:2])
    pair = sorted(pair)
    # afina: usa o mesmo raio/linha de centro nas duas
    r = (pair[0][2] + pair[1][2]) / 2.0; cy = (pair[0][1] + pair[1][1]) / 2.0
    return [(float(x), float(cy), float(r), 0) for x, _, _ in pair]

def cells():
    br = load('brasileiros.png'); lx = load('luxo.jpg')
    for i, (id_, nm, kind) in enumerate(BR_NAMES):
        r, c = divmod(i, 8)
        cx = BR_XC[c]
        lo = max(0, cx - 125)
        hi = min(1536, cx + 125)
        yield id_, nm, kind, br, (lo, BR_ROWS[r][0], hi, BR_ROWS[r][1]), False, 'br'
    for id_, nm, kind, box in BR_LAST:
        yield id_, nm, kind, br, box, False, 'br'
    for i, (id_, nm, kind) in enumerate(LX_NAMES):
        r, c = divmod(i, 6)
        yield id_, nm, kind, lx, (LX_COLS[c][0], LX_ROWS[r][0], LX_COLS[c][1], LX_ROWS[r][1]), True, 'lx'

if __name__ == '__main__':
    out = sys.argv[1]; os.makedirs(out, exist_ok=True)
    for id_, nm, kind, im, (x0, y0, x1, y1), lux, pk in cells():
        crop = im[y0:y1, x0:x1]
        m = seg(crop, lux)
        rgba = np.zeros(crop.shape[:2] + (4,), np.uint8)
        rgba[..., :3] = crop.astype(np.uint8); rgba[..., 3] = m * 255
        ys, xs = np.nonzero(m)
        bx0, bx1, by0, by1 = xs.min(), xs.max() + 1, ys.min(), ys.max() + 1
        rgba = rgba[by0:by1, bx0:bx1]; mm = m[by0:by1, bx0:bx1]
        whs = find_wheels(rgba[..., :3], mm, kind, lux)
        Image.fromarray(rgba).save(f'{out}/{id_}.png')
        json.dump({'id': id_, 'name': nm, 'kind': kind, 'pack': pk, 'wheels': whs}, open(f'{out}/{id_}.json', 'w'))
        print(id_, rgba.shape[1], rgba.shape[0], len(whs))
