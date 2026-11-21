"""Mini biblioteca de pixel art (numpy). Tudo em coordenadas inteiras; nada de antialias."""
import numpy as np
from PIL import Image

def hexc(s, a=255):
    s = s.lstrip('#')
    if len(s) == 3: s = ''.join(ch * 2 for ch in s)
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), a)

def mix(c1, c2, t):
    return tuple(int(round(c1[i] + (c2[i] - c1[i]) * t)) for i in range(3)) + (255,)

# marcadores de pintura (o Kotlin troca por degradê da cor escolhida)
P0 = (255, 0, 255, 255)   # brilho
P1 = (230, 0, 230, 255)   # claro
P2 = (200, 0, 200, 255)   # base
P3 = (160, 0, 160, 255)   # sombra
P4 = (110, 0, 110, 255)   # escuro / contorno da pintura
PAINT = [P0, P1, P2, P3, P4]

class Img:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.a = np.zeros((h, w, 4), np.uint8)

    def set(self, x, y, c):
        x = int(x); y = int(y)
        if 0 <= x < self.w and 0 <= y < self.h:
            self.a[y, x] = c

    def get(self, x, y):
        if 0 <= x < self.w and 0 <= y < self.h:
            return tuple(int(v) for v in self.a[y, x])
        return (0, 0, 0, 0)

    def rect(self, x, y, w, h, c):
        x0, y0 = max(0, int(x)), max(0, int(y))
        x1, y1 = min(self.w, int(x + w)), min(self.h, int(y + h))
        if x1 > x0 and y1 > y0:
            self.a[y0:y1, x0:x1] = c

    def hline(self, x, y, w, c): self.rect(x, y, w, 1, c)
    def vline(self, x, y, h, c): self.rect(x, y, 1, h, c)

    def line(self, x0, y0, x1, y1, c):
        x0, y0, x1, y1 = int(x0), int(y0), int(x1), int(y1)
        dx, dy = abs(x1 - x0), -abs(y1 - y0)
        sx = 1 if x0 < x1 else -1
        sy = 1 if y0 < y1 else -1
        err = dx + dy
        while True:
            self.set(x0, y0, c)
            if x0 == x1 and y0 == y1: break
            e2 = 2 * err
            if e2 >= dy: err += dy; x0 += sx
            if e2 <= dx: err += dx; y0 += sy

    def fill_mask(self, m, c):
        self.a[m] = c

    def blit(self, other, x, y):
        for yy in range(other.h):
            for xx in range(other.w):
                p = other.a[yy, xx]
                if p[3] > 0:
                    self.set(x + xx, y + yy, tuple(int(v) for v in p))

    def save(self, path):
        Image.fromarray(self.a, 'RGBA').save(path)

def poly_mask(w, h, pts, ox=0.0, oy=0.0):
    """Preenche polígono (even-odd) testando o centro de cada pixel."""
    ys, xs = np.mgrid[0:h, 0:w]
    px = xs + 0.5 - ox
    py = ys + 0.5 - oy
    inside = np.zeros((h, w), bool)
    n = len(pts) // 2
    for i in range(n):
        x1, y1 = pts[2 * i], pts[2 * i + 1]
        x2, y2 = pts[2 * ((i + 1) % n)], pts[2 * ((i + 1) % n) + 1]
        if y1 == y2: continue
        cond = ((y1 > py) != (y2 > py))
        xint = x1 + (py - y1) * (x2 - x1) / (y2 - y1)
        inside ^= cond & (px < xint)
    return inside

def circle_mask(w, h, cx, cy, r):
    ys, xs = np.mgrid[0:h, 0:w]
    return (xs + 0.5 - cx) ** 2 + (ys + 0.5 - cy) ** 2 <= r * r

def edge_mask(m):
    """pixels de m que tocam fora (4-vizinhança)."""
    p = np.pad(m, 1, constant_values=False)
    nb = p[:-2, 1:-1] & p[2:, 1:-1] & p[1:-1, :-2] & p[1:-1, 2:]
    return m & ~nb

def shift(m, dx, dy):
    out = np.zeros_like(m)
    h, w = m.shape
    ys0, ys1 = max(0, dy), min(h, h + dy)
    xs0, xs1 = max(0, dx), min(w, w + dx)
    out[ys0:ys1, xs0:xs1] = m[ys0 - dy:ys1 - dy, xs0 - dx:xs1 - dx]
    return out

def scale_nn(img, k):
    return Image.fromarray(img.a, 'RGBA').resize((img.w * k, img.h * k), Image.NEAREST)

# fonte 3x5 (maiúsculas, números e alguns símbolos) para letreiros
FONT = {
 'A':["010","101","111","101","101"],'B':["110","101","110","101","110"],'C':["011","100","100","100","011"],
 'D':["110","101","101","101","110"],'E':["111","100","110","100","111"],'F':["111","100","110","100","100"],
 'G':["011","100","101","101","011"],'H':["101","101","111","101","101"],'I':["111","010","010","010","111"],
 'J':["001","001","001","101","010"],'K':["101","101","110","101","101"],'L':["100","100","100","100","111"],
 'M':["101","111","111","101","101"],'N':["110","101","101","101","101"],'O':["010","101","101","101","010"],
 'P':["110","101","110","100","100"],'Q':["010","101","101","110","011"],'R':["110","101","110","101","101"],
 'S':["011","100","010","001","110"],'T':["111","010","010","010","010"],'U':["101","101","101","101","111"],
 'V':["101","101","101","101","010"],'W':["101","101","111","111","101"],'X':["101","101","010","101","101"],
 'Y':["101","101","010","010","010"],'Z':["111","001","010","100","111"],
 '0':["111","101","101","101","111"],'1':["010","110","010","010","111"],'2':["110","001","010","100","111"],
 '3':["110","001","010","001","110"],'4':["101","101","111","001","001"],'5':["111","100","110","001","110"],
 '6':["011","100","110","101","010"],'7':["111","001","010","010","010"],'8':["010","101","010","101","010"],
 '9':["010","101","011","001","110"],' ':["000","000","000","000","000"],'-':["000","000","111","000","000"],
 '.':["000","000","000","000","010"],'!':["010","010","010","000","010"],'/':["001","001","010","100","100"],
 '&':["010","101","010","101","011"],
}

def text_w(s): return len(s) * 4 - 1

def draw_text(img, x, y, s, c, accent=False):
    """3x5. 'Á','Ã','É','Ç' etc. viram a letra base + pixel de acento acima."""
    base = {'Á':'A','Ã':'A','Â':'A','É':'E','Ê':'E','Í':'I','Ó':'O','Ô':'O','Õ':'O','Ú':'U','Ç':'C'}
    cx = x
    for ch in s:
        ch = ch.upper()
        acc = ch in base
        g = FONT.get(base.get(ch, ch), FONT[' '])
        for ry, row in enumerate(g):
            for rx, v in enumerate(row):
                if v == '1': img.set(cx + rx, y + ry, c)
        if acc: img.set(cx + 1, y - 1, c)
        cx += 4
    return cx
