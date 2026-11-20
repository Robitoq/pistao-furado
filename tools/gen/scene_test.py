from compose import *
from models import *
from props import *
from scenery import build_all
import numpy as np
from PIL import Image
A='/home/claude/out/pistao-furado-main/app/src/main/assets/city/'
def ld(n):
    im=Image.open(A+n+'.png').convert('RGBA'); o=Img(im.width,im.height); o.a=np.array(im); return o
W,H=190,150
cv=Img(W,H)
# céu
for y in range(104): cv.a[y,:]=(int(120+y*0.6),int(185+y*0.3),235,255)
# calçada, meio-fio, asfalto (como no Kotlin)
cv.rect(0,104,W,14,(185,195,201,255))
for i in range(-1,7): paste(cv, ld('walk1' if i%3==0 else 'walk0'), i*32, 104)
cv.rect(0,117,W,2,(138,154,163,255))
for cb in range(0,W,40): cv.rect(cb+3,117,14,1,(232,236,238,255))
for i in range(-1,5): paste(cv, ld('road0'), i*48, 119)
cv.rect(0,121,W,1,(150,160,165,255))
for x in range(0,W,26): cv.rect(x+2,140,11,1,(242,194,48,255))
# zebra
for zi in range(6): cv.rect(120+zi*6,122,3,26,(240,240,240,255))
# prédios
x=2
for i in (0,3,7,9,2):
    im=ld('b%d'%i); paste(cv,im,x,104-im.h); x+=im.w+3
paste(cv, ld('t0'), 160, 112-ld('t0').h)
paste(cv, ld('lamp1'), 100, 116-32); paste(cv, ld('tl'), 112, 116-30)
paste(cv, ld('sign0'), 90, 112-40); paste(cv, ld('phone'), 70,112-24)
# veículos: (sprite idx, ground, centro x, flip, cor)
from models import ORDER
names = list(ORDER)+['moto','moto_delivery','truck_bau','truck_wood','bus']
cache={}
def spr(n):
    if n in cache: return cache[n]
    allspecs = {s['name']: s for s in HEROES + GENERIC + [TAXI]}
    if n=='moto': r=moto(False)
    elif n=='moto_delivery': r=moto(True)
    elif n=='truck_bau': r=render_car(truck('bau'))
    elif n=='truck_wood': r=render_car(truck('wood'))
    elif n=='bus': r=render_car(bus())
    else: r=render_car(allspecs[n])
    cache[n]=r; return r
def put(n, cx, ground, flip, col):
    b,it,m=spr(n)
    tmp=Img(m['W']+4,m['H']+4)
    draw_car(tmp,2,2,b,it,m,col)
    if flip: tmp.a=tmp.a[:, ::-1].copy()
    ox = cx-(m['cx'] if not flip else m['W']-m['cx'])-2  # centro -> x esquerdo
    paste(cv,tmp,int(ox),int(ground-m['gy'])-2)
C=lambda s: hexpaint(s)
# faixa de cima (133): vai p/ direita; mid (140): jogador; baixo (148): esquerda
put('taxi',40,133,False,C('F9A825')); put('bus',150,133,False,C('2E7D32'))
put('fusca',95,140,False,C('1565C0'))     # jogador ultrapassando
put('moto_delivery',20,148,True,C('C62828')); put('kombi',70,148,True,C('F5F5F5')); put('opala',150,148,True,C('6D1B2A'))
scale_nn(cv,5).save('prev_scene.png')
im=Image.open('prev_scene.png').convert('RGB'); print(im.size)
