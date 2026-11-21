"""Gera os sprites de tuning já nos tamanhos do jogo (a partir dos recortes de packs_tuning.py).
Uso: python3 packs_assets.py <tun_dir> <assets_dir>"""
import os, sys, json
import numpy as np, cv2
from PIL import Image
from packs_build import rot_frames, disc_mask, to_lo, lum

SPORT = [0, 4, 1, 8, 3, 12]            # índices (em roda_N) dos desenhos de roda esportiva usados
SPOIL = [0, 2, 3, 4, 6, 10]            # aerofólios
SPOIL_W = (11, 14, 17)
TIP = [0, 4, 6]; TIP_W = (7, 10)
DECAL = [0, 1, 2, 3, 5, 4]; DECAL_W = (16, 22, 28)
LAMP = [1, 5, 9, 10]; LAMP_W = 6
WHEEL_D = range(8, 15)

def load(tun, name): return np.asarray(Image.open(f'{tun}/{name}.png').convert('RGBA'))

def fit(rgba, w, h=None):
    """redimensiona com área, alpha com limiar (pixel art) mantendo proporção."""
    H, W = rgba.shape[:2]
    if h is None: h = max(2, round(H * w / W))
    pre = rgba.astype(np.float32); a = pre[..., 3:4] / 255.0
    pm = cv2.resize(pre[..., :3] * a, (w, h), interpolation=cv2.INTER_AREA)
    al = cv2.resize(a[..., 0], (w, h), interpolation=cv2.INTER_AREA)
    col = np.where(al[..., None] > 0.01, pm / np.maximum(al[..., None], 0.01), 0)
    out = np.zeros((h, w, 4), np.uint8)
    out[..., :3] = np.clip(col, 0, 255); out[..., 3] = np.where(al > 0.45, 255, 0)
    return out

def sport_atlas(tun, d):
    rows = []
    dm = disc_mask(d)
    for idx in SPORT:
        w = load(tun, f'roda_{idx}')
        rgb = w[..., :3].astype(np.float32)
        n = min(rgb.shape[:2]); rgb = rgb[:n, :n]
        frames = [to_lo(f, d) for f in rot_frames(rgb, d)]
        blur = np.mean([to_lo(f, d) for f in rot_frames(rgb, d, 12, 30.0)], axis=0)
        row = np.zeros((d, 7 * d, 4), np.uint8)
        for c, f in enumerate(frames + [blur]):
            row[:, c * d:(c + 1) * d, :3] = np.clip(f, 1, 255); row[:, c * d:(c + 1) * d, 3] = dm * 255
        rows.append(row)
    return np.vstack(rows)

def save(a, path): Image.fromarray(a).save(path)

if __name__ == '__main__':
    tun, out = sys.argv[1:3]
    os.makedirs(out, exist_ok=True)
    for d in WHEEL_D: save(sport_atlas(tun, d), f'{out}/pws{d}.png')
    for j, idx in enumerate(SPOIL):
        a = load(tun, f'aerofolio_{idx}')
        for w in SPOIL_W: save(fit(a, w), f'{out}/spoil{j}_{w}.png')
    for j, idx in enumerate(TIP):
        a = load(tun, f'escape_{idx}')
        for w in TIP_W: save(fit(a, w), f'{out}/tip{j}_{w}.png')
    for j, idx in enumerate(DECAL):
        a = load(tun, f'adesivo_{idx}')
        for w in DECAL_W: save(fit(a, w), f'{out}/decal{j}_{w}.png')
    for j, idx in enumerate(LAMP):
        save(fit(load(tun, f'farol_{idx}'), LAMP_W), f'{out}/lamp{j}.png')
    # cores das molas de suspensão (amostra do meio da mola de cada coilover)
    coil = []
    for i in range(7):
        a = load(tun, f'suspensao_{i}'); h, w = a.shape[:2]
        p = a[int(h * 0.12):int(h * 0.40), int(w * 0.3):int(w * 0.7), :3].reshape(-1, 3)
        sat = p.max(1).astype(int) - p.min(1).astype(int)
        q = p[sat >= np.percentile(sat, 60)]
        coil.append([int(v) for v in np.median(q, 0)])
    json.dump(dict(coil=coil), open(f'{out}/tune.json', 'w'))
    print('ok', coil)
