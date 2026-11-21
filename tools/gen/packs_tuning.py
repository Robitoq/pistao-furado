"""Recorta cada elemento do pack de tunagem (poster) em um sprite individual (RGBA, recorte justo).
Caixas manuais (coordenadas do poster 1536x1024). Uso: python3 packs_tuning.py <pasta_saida>
Saída: <pasta>/<nome>.png + index.json (nome -> categoria, tamanho)."""
import os, sys, json
import numpy as np
from PIL import Image
from scipy import ndimage as ndi
HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, 'packs_src', 'tunagem.png')

B = {}  # categoria -> [(x0,y0,x1,y1), ...]  | modo: edge | circle | soft
MODE = {}

def grid(xe, ye): return [(a, c, b, d) for (c, d) in ye for (a, b) in xe]

# 30 rodas (circulares)
B['roda'] = [(int(37 + 63.3 * i - 31), cy - 31, int(37 + 63.3 * i + 31), cy + 31) for cy in (180, 245, 310) for i in range(10)]
MODE['roda'] = 'circle'
B['aerofolio'] = grid([(645, 792), (775, 888), (895, 1032)], [(155, 198), (200, 243), (245, 290), (292, 335)])
B['parachoque'] = grid([(1052, 1182), (1200, 1362), (1380, 1512)], [(155, 200), (202, 248), (250, 296), (298, 340)])
B['suspensao'] = [(c - 21, 372, c + 21, 540) for c in (32, 76, 122, 164, 205, 246, 290)]
B['escape'] = [(345, 385, 447, 427), (455, 378, 568, 428), (572, 385, 630, 420), (345, 428, 447, 472), (456, 430, 568, 484),
               (574, 420, 632, 452), (345, 474, 450, 532), (512, 466, 634, 518)]
B['neon'] = [(645, y - 9, 840, y + 9) for y in (403, 434, 466, 497, 520, 535)]
MODE['neon'] = 'soft'
B['adesivo'] = [(858, 385, 950, 425), (948, 385, 1040, 425), (1035, 380, 1182, 422), (858, 428, 952, 472), (955, 430, 1032, 472),
                (1030, 425, 1180, 472), (858, 478, 1082, 528), (1084, 478, 1182, 530),
                (1188, 378, 1282, 428), (1188, 430, 1282, 462), (1188, 466, 1282, 500), (1210, 500, 1270, 532)]
B['insulfilm'] = grid([(1308, 1405), (1412, 1522)], [(388, 432), (437, 482), (488, 532)])
B['farol'] = grid([(10, 92), (94, 192), (194, 258), (256, 334)], [(572, 618), (622, 674), (678, 730)])
B['som'] = [(745, 575, 822, 652), (824, 578, 902, 652), (908, 575, 982, 652), (978, 575, 1030, 632), (1030, 575, 1084, 642),
            (745, 660, 905, 742), (910, 655, 1085, 742), (548 + 0, 0, 549, 1)]  # último é descartado
B['som'] = B['som'][:7]
B['banco'] = [(1097, 575, 1165, 688), (1162, 575, 1232, 688), (1239, 575, 1306, 688), (1308, 575, 1371, 688), (1379, 575, 1445, 688), (1452, 575, 1514, 688)]
B['volante'] = [(1127, 687, 1187, 755), (1205, 687, 1273, 755), (1292, 687, 1362, 755), (1367, 687, 1447, 755), (1462, 687, 1528, 755)]
B['extra'] = [(22, 790, 134, 832), (138, 784, 246, 810), (254, 786, 313, 816), (254, 820, 302, 862), (18, 836, 122, 860),
              (138, 818, 220, 862), (20, 862, 136, 894), (160, 864, 280, 906), (318, 786, 392, 870), (366, 784, 420, 852),
              (290, 848, 352, 894), (360, 868, 422, 896)]
B['detalhe'] = [(445, 795, 492, 840), (500, 795, 582, 840), (595, 790, 642, 842), (655, 795, 742, 840), (752, 795, 802, 840),
                (445, 860, 502, 886), (515, 850, 577, 886), (588, 860, 627, 886), (640, 855, 684, 890), (688, 860, 772, 886), (775, 790, 838, 902)]
B['efeito'] = [(866, 784, 1002, 832), (1018, 784, 1142, 832), (1158, 784, 1272, 832), (1286, 786, 1372, 842), (1374, 784, 1442, 822),
               (1444, 784, 1522, 822), (852, 858, 925, 906), (925, 862, 1066, 902), (1104, 850, 1172, 896), (1165, 850, 1272, 896),
               (1282, 858, 1360, 876), (1282, 878, 1360, 896), (1374, 836, 1442, 852), (1374, 858, 1442, 876), (1374, 878, 1442, 898),
               (1446, 836, 1522, 852), (1446, 858, 1522, 876), (1446, 878, 1522, 898)]
MODE['efeito'] = 'soft'

def tight_edge(crop, thr=130, close=2):
    c = crop.astype(np.float32)
    gx = ndi.sobel(c, axis=1); gy = ndi.sobel(c, axis=0)
    mag = np.sqrt((gx**2 + gy**2).sum(2))
    e = ndi.binary_closing(mag > thr, iterations=close)
    f = ndi.binary_fill_holes(e)
    f = ndi.binary_opening(f, iterations=1)
    lab, n = ndi.label(f)
    if n == 0: return np.ones(f.shape, bool)
    h, w = f.shape
    cov = ndi.sum(f[h // 4:3 * h // 4, w // 4:3 * w // 4], lab[h // 4:3 * h // 4, w // 4:3 * w // 4], range(1, n + 1))
    k = int(np.argmax(cov)) + 1
    m = lab == k
    # junta pedaços grandes próximos (peças com várias partes)
    sz = ndi.sum(f, lab, range(1, n + 1))
    for i in range(1, n + 1):
        if sz[i - 1] > 0.12 * sz[k - 1]: m |= lab == i
    return m

def soft_alpha(crop):
    c = crop.astype(np.float32)
    border = np.concatenate([c[0], c[-1], c[:, 0], c[:, -1]], 0)
    bg = np.median(border, 0)
    d = np.sqrt(((c - bg) ** 2).sum(2))
    a = np.clip((d - 22) / 70, 0, 1)
    return a

def extract(img, cat, box):
    x0, y0, x1, y1 = box
    crop = img[y0:y1, x0:x1]
    mode = MODE.get(cat, 'edge')
    h, w = crop.shape[:2]
    if mode == 'circle':
        yy, xx = np.mgrid[:h, :w]; r = min(h, w) / 2 - 0.5
        a = (np.hypot(yy - (h - 1) / 2, xx - (w - 1) / 2) <= r).astype(np.float32)
    elif mode == 'soft':
        a = soft_alpha(crop)
    else:
        a = tight_edge(crop).astype(np.float32)
    ys, xs = np.nonzero(a > 0.05)
    if len(ys) == 0: return None
    sl = (slice(ys.min(), ys.max() + 1), slice(xs.min(), xs.max() + 1))
    rgba = np.dstack([crop[sl], (a[sl] * 255).astype(np.uint8)])
    return rgba

if __name__ == '__main__':
    out = sys.argv[1]; os.makedirs(out, exist_ok=True)
    img = np.asarray(Image.open(SRC).convert('RGB'))
    index = {}
    for cat, boxes in B.items():
        index[cat] = []
        for n, box in enumerate(boxes):
            rgba = extract(img, cat, box)
            if rgba is None: continue
            name = f'{cat}_{n}'
            Image.fromarray(rgba).save(f'{out}/{name}.png')
            index[cat].append(dict(name=name, w=rgba.shape[1], h=rgba.shape[0], box=list(box)))
        print(cat, len(index[cat]))
    # pinturas: amostra a cor central de cada amostra
    sw = []
    for r, cy in enumerate((600, 660, 717)):
        for c in range(8):
            cx = (380, 431, 479, 529, 577, 619, 660, 701)[c]
            p = img[int(cy) - 8:int(cy) + 8, int(cx) - 8:int(cx) + 8].reshape(-1, 3)
            sw.append([int(v) for v in np.median(p, 0)])
    index['tinta'] = sw
    json.dump(index, open(f'{out}/index.json', 'w'))
