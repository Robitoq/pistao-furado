"""Pré-visualização (referência em Python) do desenho dos veículos do pack, espelhando drawPackVeh() de Vehicles.kt.
Uso: python3 packs_preview.py <assets_dir> <metas.json> <saida.png> [ids...]"""
import sys, json, math
import numpy as np
from PIL import Image, ImageDraw

def mix(a, b, t): return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))

def shade(T, v):
    r = v / 128.0
    if r <= 1: return tuple(int(c * r) for c in T)
    f = min((r - 1) * 0.9, 1.0)
    return tuple(int(c + (255 - c) * f) for c in T)

class Pack:
    def __init__(self, assets, metas):
        self.A = assets; self.metas = json.load(open(metas)); self.cache = {}
    def img(self, n):
        if n not in self.cache: self.cache[n] = Image.open(f'{self.A}/{n}.png').convert('RGBA')
        return self.cache[n]

    def baked(self, k, paint, bucket, deco, film):
        m = self.metas[k]; body = np.array(self.img(f'pb{k}'))
        out = body.copy(); h, w = body.shape[:2]
        sev = bucket / 10.0
        T = mix(paint, (0x7D, 0x76, 0x6A), sev * 0.55)
        isp = (body[..., 3] == 255) & (body[..., 1] == 0) & (body[..., 0] == body[..., 2]) & (body[..., 0] > 0)
        for y, x in zip(*np.nonzero(isp)): out[y, x, :3] = shade(T, int(body[y, x, 0]))
        if deco >= 2:   # adesivo do pack
            j = deco - 2; best = min((16, 22, 28), key=lambda s: abs(s - (m['door'][2] - m['door'][0])))
            dc = np.array(self.img(f'decal{j}_{best}')); dh, dw = dc.shape[:2]
            x0 = (m['door'][0] + m['door'][2] - dw) // 2; y0 = (m['door'][1] + m['door'][3] - dh) // 2 + 1
            for yy in range(dh):
                for xx in range(dw):
                    X, Y = x0 + xx, y0 + yy
                    if 0 <= X < w and 0 <= Y < h and isp[Y, X] and dc[yy, xx, 3] > 0: out[Y, X, :3] = dc[yy, xx, :3]
        elif deco == 1:
            sy = (m['door'][1] + m['door'][3]) // 2
            for y, x in zip(*np.nonzero(isp)):
                if y in (sy, sy + 1): out[y, x, :3] = (0xE5, 0x39, 0x35)
                elif y == sy + 2: out[y, x, :3] = (0xF5, 0xF5, 0xF5)
        if film:
            g = (body[..., 3] > 0) & (body[..., 3] < 255)
            out[g, :3] = (0x0B, 0x0E, 0x14); out[g, 3] = 235
        return Image.fromarray(out)

    def draw(self, k, paint, tuning=(), cond=(100,) * 5, t=0.0, rot=0.0, speed=0.0, bg=(90, 150, 200), pad=14, scale=1):
        m = self.metas[k]; W, H = m['w'], m['h']
        can = Image.new('RGBA', (W + 2 * pad, H + 2 * pad), bg + (255,)); dr = ImageDraw.Draw(can)
        ox = oy = pad
        def P(img, x, y): can.alpha_composite(img, (ox + int(x), oy + int(y)))
        def R(c, x, y, w, h, a=255):
            lay = Image.new('RGBA', (can.width, can.height), (0, 0, 0, 0)); ImageDraw.Draw(lay).rectangle((ox + x, oy + y, ox + x + w - 1, oy + y + h - 1), fill=tuple(c) + (a,))
            can.alpha_composite(lay)
        motor, lat, tire, inter, susp = cond
        sev = min(1, max(0, (100 - lat) / 100.0)); bucket = int(round(sev * 10))
        flat = tire < 25; sag = max(0, 65 - susp) / 65.0
        dy = 0
        if 'rebaixada' in tuning: dy += 2
        if sag > 0.5: dy += 2
        elif sag > 0.15: dy += 1
        if flat: dy += 1
        dy = min(dy, 4)
        gy = m['gy']
        if 'neon' in tuning:
            nc = NEON[k % len(NEON)]
            for i in range(10, 0, -1):
                R(nc, m['rear'] - 2 + i // 2, gy - 1 - 0, m['front'] - m['rear'] + 4 - i, 3, int(10 + 5 * (10 - i)))
            R(nc, m['rear'] + 5, gy - 1, m['front'] - m['rear'] - 10, 1)
        R((0, 0, 0), m['rear'] + 1, gy, m['front'] - m['rear'] - 1, 1, 85)
        deco = 0
        if 'adesivo' in tuning: deco = 2 + (k % 6)
        film = 'pelicula' in tuning
        if m['cab']:
            P(self.img(f'pi{k}'), 0, dy)
            g = m['glass']; hx, hy = m['head']
            seat = (0x6D, 0x4C, 0x41) if 'couro' in tuning else (0x37, 0x47, 0x4F)
            def CR(c, x, y, w, h):
                ax, ay, bx, by = max(x, g[0]), max(y, g[1] + dy), min(x + w, g[2] + 1), min(y + h, g[3] + dy + 1)
                if bx > ax and by > ay: R(c, ax, ay, bx - ax, by - ay)
            CR(seat, hx - 4, hy + 1 + dy, 3, 8)
            CR((0xC8, 0x96, 0x6E), hx, hy + dy, 3, 3); CR((0x21, 0x21, 0x21), hx, hy - 1 + dy, 3, 1); CR((0x30, 0x6A, 0xA0), hx - 1, hy + 3 + dy, 5, 6)
            if film: pass
        # poço + rodas
        d = m['d']
        if m.get('kind') != 'moto':
            for wx, wy in zip(m['wx'], m['wy']):
                lay = Image.new('RGBA', can.size, (0, 0, 0, 0)); ImageDraw.Draw(lay).ellipse((ox + wx - 0.5, oy + wy + dy - 0.5, ox + wx + d - 0.5, oy + wy + dy + d - 0.5), fill=(0x0C, 0x0D, 0x10, 255)); can.alpha_composite(lay)
        sport = 'rodas' in tuning
        fr = 6 if speed > 62 else int(rot / 0.95) % 6
        wd = min(14, max(8, d))
        if sport:
            at = self.img(f'pws{wd}'); row = (k % 6) * wd
        else:
            at = self.img(f'pw{k}'); row = (0 if not tire < 55 else 1) * d
            if sev > 0.5: row = 2 * d
        for i, (wx, wy) in enumerate(zip(m['wx'], m['wy'])):
            isflat = flat and i == 0 and not sport
            r_ = 3 * d if isflat else row
            f = 0 if isflat else fr
            P(at.crop((f * wd if sport else f * d, r_, (f + 1) * (wd if sport else d), r_ + (wd if sport else d))), wx, wy)
        if 'rebaixada' in tuning:   # mola colorida aparecendo no arco
            cc = COIL[k % len(COIL)]
            for wx, wy in zip(m['wx'], m['wy']):
                R(cc, wx + d // 2, wy + dy - 1, 2, max(2, dy + 1))
        body = self.baked(k, PAINT_RGB(paint), bucket, deco, film)
        P(body, 0, dy)
        bt = m['bot'] - 4 + dy
        if 'metalica' in tuning:
            pass
        if 'parachoque' in tuning:
            R((0x1B, 0x1B, 0x1B), m['front'] - 4, bt + 1, 6, 3); R((0xD3, 0x2F, 0x2F), m['front'] - 4, bt + 4, 6, 1)
            R((0x1B, 0x1B, 0x1B), m['rear'] - 1, bt + 1, 5, 3)
        if 'farol' in tuning:
            hr = m['headRect']; lp = self.img('lamp2'); P(lp.resize((max(4, hr[2] + 2), max(3, hr[3] + 1)), Image.NEAREST), hr[0] - 1, hr[1] + dy)
        if 'aerofolio' in tuning:
            L = m['front'] - m['rear']; sz = min(SPOIL_W, key=lambda s: abs(s - 0.21 * L))
            sp = self.img(f'spoil{k % 6}_{sz}'); P(sp, m['rear'] - 1, m['wing'][1] + dy - sp.height + 2)
        if 'escape' in tuning or 'turbo' in tuning:
            tp = self.img(f'tip{k % 3}_{7 if (m['front'] - m['rear']) < 62 else 10}'); P(tp, m['rear'] - tp.width + 3, m['bot'] + dy - 2)
        if m.get('kind') == 'moto':   # piloto (espelha drawVeh)
            sx = m['rear'] + int((m['front'] - m['rear']) * 0.38); ty = m['hoodY'] + dy
            R((0x26, 0x32, 0x38), sx, ty + 3, 5, 5); R((0x26, 0x32, 0x38), sx + 4, ty + 6, 2, 4)
            R((0x15, 0x65, 0xC0), sx - 1, ty - 4, 4, 7); R((0x15, 0x65, 0xC0), sx + 2, ty - 3, max(4, m['front'] - sx - 14), 2)
            R((0xC8, 0x96, 0x6E), sx, ty - 7, 3, 3); R((0xB7, 0x1C, 0x1C), sx - 1, ty - 8, 4, 2)
        if 'turbo' in tuning:
            fl = 4; R((0x29, 0xB6, 0xF6), m['rear'] - 6 - fl, bt + 2, fl, 2)
        return can.resize((can.width * scale, can.height * scale), Image.NEAREST)

SPOIL_W = (11, 14, 17)
NEON = [(63, 140, 255), (176, 64, 255), (255, 48, 48), (48, 255, 64), (255, 208, 48)]
COIL = [(239, 24, 46), (26, 117, 242), (243, 179, 7), (235, 41, 34)]
def PAINT_RGB(p): return p

if __name__ == '__main__':
    A, M, out = sys.argv[1:4]; ids = sys.argv[4:]
    pk = Pack(A, M); names = [m['name'] for m in pk.metas]
    sel = [names.index(i) for i in ids] if ids else list(range(len(names)))
    sc = 4
    tiles = []
    combos = [(), ('rodas', 'rebaixada', 'aerofolio', 'neon', 'escape'), ('adesivo', 'farol', 'pelicula', 'parachoque', 'couro')]
    for k in sel:
        for c in combos: tiles.append(pk.draw(k, (198, 40, 40) if c != combos[1] else (21, 101, 192), c, scale=sc))
    tw = max(t.width for t in tiles); th = max(t.height for t in tiles)
    cols = 3; rows = (len(tiles) + cols - 1) // cols
    sheet = Image.new('RGB', (cols * tw, rows * th), (90, 150, 200))
    for i, t in enumerate(tiles): sheet.paste(t.convert('RGB'), ((i % cols) * tw, (i // cols) * th))
    sheet.save(out); print(sheet.size)
