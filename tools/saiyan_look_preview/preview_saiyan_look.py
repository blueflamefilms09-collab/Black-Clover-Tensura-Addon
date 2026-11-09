"""Front-view MOCKUP of the Saiyan looks (3.69.0: eyes, flame aura, armour, SS4 fur + tail, the charge-up; NO hair) on a blocky player. Not the game's own renderer.
Reads the look table out of SaiyanLook.java and ports its charge-up maths, so what it shows is what the table says.
usage: python3 tools/saiyan_look_preview/preview_saiyan_look.py forms out.png [form_id ...]
       python3 tools/saiyan_look_preview/preview_saiyan_look.py charge out.png [form_id]"""
import math, os, re, sys
from PIL import Image, ImageChops, ImageDraw, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SRC = open(os.path.join(ROOT, 'src/main/java/com/newuniverse/nusmp/saiyan/SaiyanLook.java')).read()
TEX = os.path.join(ROOT, 'src/main/resources/assets/nusmp/textures/particle')
ROW = re.compile(r'add\(l\("(\w+)",\s*(0x[0-9A-Fa-f]+),\s*(true|false),\s*(0x[0-9A-Fa-f]+),\s*(0x[0-9A-Fa-f]+|0),\s*(true|false),\s*(\d+),\s*(\d+),\s*([\d.]+)f,\s*([\d.]+)f,\s*(true|false)\)\)')
LOOKS = {}
for m in ROW.finditer(SRC):
    g = m.groups()
    LOOKS[g[0]] = dict(eyes=int(g[1], 16), eyeGlow=g[2] == 'true', glow=int(g[3], 16), fur=int(g[4], 16), tail=g[5] == 'true', flames=int(g[6]), bolts=int(g[7]),
                       auraH=float(g[8]), burst=float(g[9]), armour=g[10] == 'true')
GATHER, BURST, CRACK = 22, 46, 78

def rgb(c): return ((c >> 16) & 255, (c >> 8) & 255, c & 255)
def hsh(i):
    h = ((i * 0x9E3779B1) ^ 0x85EBCA6B) & 0xFFFFFFFF
    h ^= h >> 15; h = (h * 0x2C1B3C6D) & 0xFFFFFFFF; h ^= h >> 12
    return (h & 0xFFFF) / 65536.0
def c01(x): return max(0.0, min(1.0, x))
def smooth(x): x = c01(x); return x * x * (3 - 2 * x)
def mix(a, b, t): return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3))

def flash(t):
    if t < 0 or t > GATHER + 10: return 0.0
    if t <= GATHER: return 0.25 * (t / GATHER) ** 2
    u = (t - GATHER) / 10; return 1 - u * u
def pillar(t):
    s = GATHER - 4
    if t < s or t >= BURST: return 0.0
    return (t - s) / 6 if t < GATHER + 2 else 1.0
def pillar_alpha(t):
    if t < GATHER - 4 or t >= BURST: return 0.0
    if t < GATHER + 2: return (t - (GATHER - 4)) / 6
    u = (t - (GATHER + 2)) / (BURST - GATHER - 2); return 1 - u * u
def pillar_w(t, b): return (0.35 + 0.55 * pillar_alpha(t)) * (0.7 + 0.3 * b)
def ring_r(t, k, b):
    s = GATHER + 6 * k; u = (t - s) / (BURST - s)
    return 0.0 if u <= 0 or u >= 1 else 0.6 + (4.2 + 2.6 * b) * u ** 0.55
def ring_a(t, k):
    s = GATHER + 6 * k; u = (t - s) / (BURST - s)
    return 0.0 if u <= 0 or u >= 1 else 1 - u
def crack_r(t, b): return 0.0 if t < 0 or t >= CRACK else (0.4 + 2.6 * b) * smooth(t / (GATHER + 4))
def crack_a(t):
    if t < 0 or t >= CRACK: return 0.0
    return c01(t / 8) * (1.0 if t < GATHER else 1 - c01((t - GATHER) / (CRACK - GATHER)))
def chip(i, t, b):
    a = i * 2.39996 + hsh(i) * 0.6; r0 = 0.9 + 1.5 * hsh(i + 31); top = (0.5 + 1.4 * hsh(i + 77)) * (0.6 + 0.4 * b)
    lift = smooth((t - 2 - hsh(i + 5) * 6) / (GATHER - 4)); h = top * lift; r = r0
    if t > GATHER:
        u = c01((t - GATHER) / (BURST - GATHER + 8)); r = r0 + (0.8 + 1.2 * hsh(i + 9)) * b * u; h = max(0.0, top * (1 - u * u) + 0.6 * u * (1 - u))
    return a, r, h, 0.07 + 0.06 * hsh(i + 13)
def chip_alpha(t):
    if t < 1: return 0.0
    return c01(t / 4) * (1.0 if t < BURST else 1 - c01((t - BURST) / 8))
def streak(i, t):
    a = i * 2.39996 + 0.7 * hsh(i + 2); ph = (t / 14 + i / 12) % 1.0; u = ph * ph; r = 3.4 * (1 - u) + 0.15
    h = 0.1 + (0.2 + 1.2 * hsh(i + 17)) * (1 - u) + 0.9 * u
    live = 1.0 if t < GATHER + 4 else 1 - c01((t - GATHER - 4) / 6)
    return math.cos(a) * r, h, math.sin(a) * r, live * c01(ph / 0.15) * c01((1 - ph) / 0.1)

def tex(name): return Image.open(os.path.join(TEX, 'saiyan_%s.png' % name)).convert('RGBA')
def tinted(im, col, alpha=1.0):
    r, g, b, a = im.split(); lum = im.convert('L')
    out = Image.new('RGBA', im.size, col + (0,)); out.putalpha(ImageChops.multiply(a, lum).point(lambda v: int(min(255, v * 1.15) * alpha))); return out
def add(img, layer):
    a = layer.split()[3]; col = Image.merge('RGB', layer.split()[:3]); col = ImageChops.multiply(col, Image.merge('RGB', (a, a, a)))
    base = img.convert('RGB'); out = ImageChops.add(base, col); out.putalpha(255); return out

def render(fid, t=None, S=110, W=None, H=None, armour=True):
    k = LOOKS.get(fid); b = k['burst'] if k else 1.0
    W = W or int(5.2 * S); H = H or int(5.2 * S)
    img = Image.new('RGBA', (W, H), (20, 22, 32, 255)); d = ImageDraw.Draw(img)
    for y in range(H): d.line([(0, y), (W, y)], fill=(20 + y // 46, 22 + y // 46, 32 + y // 30, 255))
    cx, ground = W // 2, int(H * 0.86)
    Y = lambda hgt: ground - hgt * S                       # height in blocks above the feet
    X = lambda x: cx + x * S
    squash = 0.3
    def ellipse_tex(im, radius, ang=0.0):
        w = max(2, int(2 * radius * S)); h = max(2, int(w * squash)); im = im.rotate(ang, resample=Image.BICUBIC).resize((w, h)); return im, (int(cx - w / 2), int(ground - h / 2))
    glow = (255, 214, 60)
    col = rgb(k['glow']) if k else glow
    layer = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    def put(im, pos): layer.alpha_composite(im, pos)
    ch = t is not None
    # --- ground: cracks and rings
    if ch:
        cr, ca = crack_r(t, b), crack_a(t)
        if cr > 0.05 and ca > 0.01:
            im, p = ellipse_tex(tinted(tex('crack'), mix(col, (255, 255, 255), flash(t)), 0.95 * ca), cr); put(im, p)
        for q in range(2):
            r, a = ring_r(t, q, b), ring_a(t, q)
            if r > 0.01 and a > 0.01:
                im, p = ellipse_tex(tinted(tex('ring'), col if q == 0 else mix(col, (255, 255, 255), 0.55), 0.85 * a), r); put(im, p)
    steady = smooth((t - GATHER) / 20) if ch else 1.0
    if steady > 0.01 and k:
        im, p = ellipse_tex(tinted(tex('ring'), col, 0.4 * steady), 0.95); put(im, p)
    # --- the column of light
    if ch:
        pa = pillar_alpha(t)
        if pa > 0.01:
            hh = (7 + 9 * b) * pillar(t); w = pillar_w(t, b)
            for ww, cc, al in ((w, col, 0.65), (w * 0.42, mix(col, (255, 255, 255), 0.55), 0.95)):
                im = tinted(tex('pillar'), cc, al * pa).resize((max(2, int(2 * ww * S)), max(2, int(hh * S)))); put(im, (int(cx - im.width / 2), int(ground - im.height)))
    # --- aura flames
    if k:
        n = k['flames']; grow = 0.35 + 0.65 * smooth(t / GATHER) if ch else 1.0
        hh = (k['auraH'] + 0.5) * grow; hz = tinted(tex('pillar'), col, 0.26).resize((int(2 * 0.85 * S), max(2, int(hh * S)))); put(hz, (int(cx - hz.width / 2), int(ground - hz.height)))
        fl = tinted(tex('flame'), col, 0.8)
        for i in range(n):
            a = i * 2 * math.pi / n + hsh(i) * 0.5; r = 0.52 if i % 2 == 0 else 0.80; hgt = k['auraH'] * (0.7 + 0.5 * hsh(i + 3)) * (0.8 + 0.2 * hsh(i + 41)) * grow
            w = int(2 * (0.26 + 0.07 * hsh(i + 7)) * S); f = fl.resize((w, max(2, int(hgt * S))))
            put(f, (int(X(math.cos(a) * r) - f.width / 2), int(ground - f.height - 0.02 * S + math.sin(a) * 0.12 * S)))
        bo = tinted(tex('bolt'), mix(col, (255, 255, 255), 0.5), 1.0)
        for i in range(k['bolts']):
            if hsh(i + 50) > 0.6: continue
            a = hsh(i + 60) * 6.28; r = 0.5 + 0.5 * hsh(i + 61); ln = 0.9 + 0.7 * hsh(i + 63)
            bb = bo.resize((int(0.4 * S), int(ln * S))); put(bb, (int(X(math.cos(a) * r) - bb.width / 2), int(Y(0.4 + 1.2 * hsh(i + 62)) - bb.height)))
    # --- charge-up: streaks, chips, flash
    if ch:
        st = tinted(tex('streak'), mix(col, (255, 255, 255), 0.35), 1.0)
        for i in range(12):
            x, hy, z, al = streak(i, t)
            if al < 0.02: continue
            ln = 0.9 + 0.5 * hsh(i + 70); r = math.hypot(x, z) + 1e-4
            tx, ty = x + x / r * ln * 0.8, hy + ln * 0.45
            dx, dy = (X(x) - X(tx)), (Y(hy) - Y(ty)); L = max(2, int(math.hypot(dx, dy)))
            im = st.resize((max(2, int(0.14 * S)), L)); ang = math.degrees(math.atan2(dx, dy))
            im = im.rotate(ang, expand=True, resample=Image.BICUBIC); im.putalpha(im.split()[3].point(lambda v: int(v * al)))
            put(im, (int((X(x) + X(tx)) / 2 - im.width / 2), int((Y(hy) + Y(ty)) / 2 - im.height / 2)))
    img = add(img, layer); layer = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # --- the player (back to front: legs, body, arms, head)
    skin, shirt, pant = (224, 172, 128), (232, 112, 28), (40, 60, 150)
    fur = rgb(k['fur']) if k and k['fur'] else None
    arm = bool(k and k['armour'] and armour)
    if fur: shirt = fur; pant = mix(fur, (20, 10, 20), 0.5)
    if arm: shirt = pant = (30, 34, 62)              # the black-navy undersuit under the plates
    def B(x0, y0, x1, y1, c, e=True):
        d.rectangle([(X(x0), Y(y1)), (X(x1), Y(y0))], fill=c, outline=tuple(max(0, v - 45) for v in c) if e else None)
    WH, GD, NV = (234, 238, 246), (222, 178, 60), (38, 54, 128)
    B(-0.25, 0.0, 0.0, 0.75, pant); B(0.0, 0.0, 0.25, 0.75, pant)
    B(-0.25, 0.75, 0.25, 1.5, shirt)
    B(-0.5, 0.75, -0.25, 1.5, shirt); B(0.25, 0.75, 0.5, 1.5, shirt)
    B(-0.5, 0.75, -0.25, 1.0, skin); B(0.25, 0.75, 0.5, 1.0, skin)
    if arm:
        for s in (-1, 1):
            B(0.0 if s > 0 else -0.27, 0.0, 0.27 if s > 0 else 0.0, 0.28, WH); B(0.0 if s > 0 else -0.27, 0.26, 0.27 if s > 0 else 0.0, 0.31, GD)       # boots + trim
            x0, x1 = (0.25, 0.52) if s > 0 else (-0.52, -0.25)
            B(x0, 0.74, x1, 1.0, WH); B(x0, 0.99, x1, 1.03, GD)                                                                                      # gloves
            B(x0 - 0.02 * s, 1.34, x1 + 0.1 * s, 1.5, WH); B(x0 - 0.02 * s, 1.5, x1 + 0.08 * s, 1.56, GD)                                              # pauldron steps
            B(x0 + 0.02, 1.56, x1 + 0.04, 1.62, (200, 224, 240))
        B(-0.28, 0.95, 0.28, 1.12, GD); B(-0.28, 0.78, 0.28, 0.95, NV)                                                                              # waist
        B(-0.26, 1.1, 0.26, 1.46, WH); B(-0.26, 1.1, 0.26, 1.2, NV); B(-0.05, 1.28, 0.05, 1.38, GD); B(-0.26, 1.44, 0.26, 1.48, GD)                       # breastplate
    elif k and fur:
        for s in (-1, 1):
            B(0.0 if s > 0 else -0.26, 0.2, 0.26 if s > 0 else 0.0, 0.75, fur)
    B(-0.25, 1.5, 0.25, 2.0, skin)
    ec = rgb(k['eyes']) if k else (60, 90, 190)
    for sx in (-0.16, 0.06):
        B(sx, 1.68, sx + 0.1, 1.76, (255, 255, 255), False); B(sx + 0.02, 1.68, sx + 0.08, 1.76, ec, False)
    if arm: B(0.06, 1.62, 0.2, 1.84, (110, 120, 138)); B(0.1, 1.68, 0.2, 1.8, (150, 238, 255)); B(0.25, 1.7, 0.29, 2.06, (110, 120, 138))
    hairless = (40, 30, 26)
    B(-0.27, 1.95, 0.27, 2.02, hairless, False)                         # NO hair: a plain cap line only
    if k and k['tail']:
        pts = [(X(0.1), Y(0.8)), (X(0.52), Y(0.62)), (X(0.78), Y(0.9)), (X(0.72), Y(1.25))]
        d.line(pts, fill=fur, width=int(0.1 * S), joint='curve'); d.line(pts[2:], fill=(255, 240, 230), width=int(0.04 * S))
    # --- eye glow + flash on top (additive)
    if k and k['eyeGlow']:
        for sx in (-0.11, 0.11):
            p = (X(sx), Y(1.72)); gd = ImageDraw.Draw(layer); gd.ellipse([p[0] - 8, p[1] - 5, p[0] + 8, p[1] + 5], fill=ec + (200,))
    if ch:
        fl = flash(t)
        if fl > 0.01:
            sp = tinted(tex('spark'), mix(col, (255, 255, 255), 0.6), min(1.0, fl)); sz = int((0.45 + 1.8 * fl) * (0.7 + 0.3 * b) * S * 2)
            sp = sp.resize((sz, sz)); layer.alpha_composite(sp, (int(cx - sz / 2), int(Y(1.15) - sz / 2)))
        ca_ = chip_alpha(t)
        if ca_ > 0.01:
            ch_ = tex('chip')
            for i in range(10):
                a, r, hgt, sz = chip(i, t, b); w = max(3, int(2 * sz * S)); im = ch_.resize((w, w)); im = tinted_chip(im, col, ca_)
                img.alpha_composite(im, (int(X(math.cos(a) * r) - w / 2), int(Y(hgt) - w / 2 + math.sin(a) * r * 0.3 * S)))
    glow_l = layer.filter(ImageFilter.GaussianBlur(7)); img = add(img, glow_l); img = add(img, layer)
    return img

def tinted_chip(im, col, a):
    r, g, b, al = im.split(); base = Image.new('RGBA', im.size, mix((140, 132, 120), col, 0.2) + (0,))
    lum = im.convert('L').point(lambda v: int(60 + v * 0.75)); base = Image.merge('RGBA', [lum.point(lambda v, c=col: int(v * 0.7 + c[0] * 0.1))] * 1 + [lum.point(lambda v, c=col: int(v * 0.65 + c[1] * 0.1)), lum.point(lambda v, c=col: int(v * 0.6 + c[2] * 0.1)), al.point(lambda v: int(v * a))]); return base

def sheet(ims, labels, cols):
    rows = (len(ims) + cols - 1) // cols; out = Image.new('RGBA', (ims[0].width * cols, ims[0].height * rows)); d = ImageDraw.Draw(out)
    for n, (im, lb) in enumerate(zip(ims, labels)):
        x, y = (n % cols) * im.width, (n // cols) * im.height; out.paste(im, (x, y)); d.text((x + 8, y + 6), lb, fill=(255, 255, 255, 255))
    return out

def main():
    mode = sys.argv[1] if len(sys.argv) > 1 else 'forms'; out = sys.argv[2] if len(sys.argv) > 2 else 'look_preview.png'
    if mode == 'charge':
        fid = sys.argv[3] if len(sys.argv) > 3 else 'super_saiyan'; ts = [0, 8, 16, 22, 26, 32, 40, 52]
        ims = [render(fid, t) for t in ts]; sheet(ims, ['%s  t=%d ticks%s' % (fid, t, '  (burst)' if t == 22 else '') for t in ts], 4).save(out)
    else:
        ids = sys.argv[3:] or ['super_saiyan', 'super_saiyan_2', 'super_saiyan_3', 'super_saiyan_4', 'super_saiyan_god', 'super_saiyan_blue', 'perfected_ultra_instinct', 'true_ultra_instinct']
        ims = [render(i, None) for i in ids]; sheet(ims, ids, 4).save(out)
    print(out, len(LOOKS), 'looks parsed')

if __name__ == '__main__':
    main()
