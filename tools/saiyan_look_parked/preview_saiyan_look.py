"""Front-view MOCKUP of the parked Saiyan looks on a blocky player (not the game's own renderer).
Reads the look table out of SaiyanLook.java and uses the same hair maths, so what it shows is what the table says.
usage: python3 preview_saiyan_look.py out.png [form_id ...]"""
import math, re, sys, os
from PIL import Image, ImageDraw, ImageChops, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = open(os.path.join(HERE, 'SaiyanLook.java')).read()
ROW = re.compile(r'add\(l\("(\w+)",\s*(0x[0-9A-Fa-f]+),\s*(0x[0-9A-Fa-f]+),\s*(true|false),\s*(0x[0-9A-Fa-f]+),\s*(0x[0-9A-Fa-f]+|0),\s*(true|false),\s*(\d+),\s*(\d+),\s*(\d+),\s*([\d.]+)f,\s*([\d.]+)f,\s*([\d.]+)f,\s*([\d.]+)f,\s*([\d.]+)f,\s*(\d+),\s*(\d+),\s*([\d.]+)f\)\)')
LOOKS = {}
for m in ROW.finditer(SRC):
    g = m.groups()
    LOOKS[g[0]] = dict(hair=int(g[1], 16), eyes=int(g[2], 16), eyeGlow=g[3] == 'true', glow=int(g[4], 16), fur=int(g[5], 16), tail=g[6] == 'true',
                       spikes=int(g[7]), bangs=int(g[8]), segs=int(g[9]), length=float(g[10]), lift=float(g[11]), sweep=float(g[12]), droop=float(g[13]),
                       width=float(g[14]), flames=int(g[15]), bolts=int(g[16]), auraH=float(g[17]))

def rgb(c): return ((c >> 16) & 255, (c >> 8) & 255, c & 255)
def hsh(i):
    h = (i * 0x9E3779B1 ^ 0x85EBCA6B) & 0xFFFFFFFF
    h ^= h >> 15; h = (h * 0x2C1B3C6D) & 0xFFFFFFFF; h ^= h >> 12
    return (h & 0xFFFF) / 65536.0

def strand(k, i):
    n = k['spikes']; bangs = min(k['bangs'], n); ln = k['length'] / max(1, k['segs'])
    if i < bangs:
        x = (i - (bangs - 1) / 2) * 0.13
        b = (x, -0.47, -0.24); d = [x * 0.5, 0.45, -0.75]; ln *= 0.7
    else:
        j = i - bangs; m = max(1, n - bangs)
        a = j * 2.39996 + 0.5; rr = 0.20 * math.sqrt((j + 0.5) / m)
        hx, hz = math.cos(a), math.sin(a)
        b = (hx * rr, -0.5, hz * rr); d = [hx * 0.55, -k['lift'], hz * 0.55 + k['sweep']]
        ln *= 0.85 + 0.3 * hsh(j)
    l = math.sqrt(sum(v * v for v in d)); return b, [v / l for v in d], ln

def bend(d, droop):
    d[1] += droop * 0.55; l = math.sqrt(sum(v * v for v in d)); return [v / l for v in d]

def tex(name, size=None):
    im = Image.open(os.path.join(HERE, 'textures', 'saiyan_%s.png' % name)).convert('RGBA')
    return im

def tint(im, col):
    r, g, b, a = im.split(); lum = im.convert('L')
    out = Image.new('RGBA', im.size, col + (0,)); out.putalpha(ImageChops.multiply(a, lum.point(lambda v: min(255, int(v * 1.3 + 40)))))
    return out

def render(fid, S=120):
    W, H = 4 * S, int(3.9 * S)
    img = Image.new('RGBA', (W, H), (22, 24, 34, 255))
    d = ImageDraw.Draw(img)
    for y in range(H): d.line([(0, y), (W, y)], fill=(22 + y // 40, 24 + y // 40, 34 + y // 26, 255))
    cx, ground = W // 2, int(3.55 * S)
    k = LOOKS.get(fid)
    P = lambda x, y: (cx + x * S, ground - (2.0 - y) * S)   # y in blocks from the neck down is +; head origin at neck
    skin, shirt, pant = (224, 172, 128), (232, 112, 28), (40, 60, 150)
    if k and k['fur']:
        fur = rgb(k['fur']); shirt = fur; skin = (min(255, fur[0] + 10), fur[1], fur[2]); pant = (70, 30, 40)
    # aura behind
    aura = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    if k:
        col = rgb(k['glow']); fl = tint(tex('flame'), col); bo = tint(tex('bolt'), col)
        for i in range(k['flames']):
            a = (i / max(1, k['flames']) - 0.5) * 2.2 + (hsh(i) - 0.5) * 0.3
            h = k['auraH'] * (0.65 + 0.5 * hsh(i + 9)); w = int(0.55 * S)
            f = fl.resize((w, int(h * S))).rotate(-a * 12, expand=True)
            aura.alpha_composite(f, (int(cx + a * 0.55 * S - f.width / 2), int(ground - f.height + 0.05 * S)))
        aura = aura.filter(ImageFilter.GaussianBlur(1.2))
        for i in range(k['bolts']):
            a = (hsh(i + 30) - 0.5) * 2.0; h = int(k['auraH'] * 0.8 * S)
            b = bo.resize((int(0.3 * S), h)); aura.alpha_composite(b, (int(cx + a * 0.6 * S - b.width / 2), int(ground - h)))
        glow = aura.filter(ImageFilter.GaussianBlur(10)); img.alpha_composite(glow); img.alpha_composite(aura)
    d = ImageDraw.Draw(img)
    def box(x0, y0, x1, y1, c, edge=True):
        a, b = P(x0, y0), P(x1, y1); d.rectangle([a, b], fill=c, outline=tuple(max(0, v - 40) for v in c) if edge else None)
    # y here: 0 = neck line; body 0..0.75; legs 0.75..1.5; ground at 1.5. Shift so ground at y=2.0-... use offset
    off = 0.5  # head sits above neck line: head y -0.5..0 -> on screen we add 0.5
    def B(x0, y0, x1, y1, c): box(x0, y0 + off, x1, y1 + off, c)
    B(-0.25, 0.75, 0.0, 1.5, pant); B(0.0, 0.75, 0.25, 1.5, pant)
    B(-0.25, 0.0, 0.25, 0.75, shirt)
    B(-0.5, 0.0, -0.25, 0.75, shirt if k and k['fur'] else shirt); B(0.25, 0.0, 0.5, 0.75, shirt)
    B(-0.5, 0.5, -0.25, 0.75, skin); B(0.25, 0.5, 0.5, 0.75, skin)
    B(-0.25, -0.5, 0.25, 0.0, skin)
    if k and k['fur']: B(-0.25, -0.5, 0.25, 0.0, skin)
    # eyes
    ec = rgb(k['eyes']) if k else (60, 90, 190)
    for sx in (-0.15, 0.06):
        B(sx, -0.22, sx + 0.09, -0.14, (255, 255, 255)); B(sx + 0.02, -0.22, sx + 0.08, -0.14, ec)
        if k and k['eyeGlow']:
            g = Image.new('RGBA', (W, H), (0, 0, 0, 0)); gd = ImageDraw.Draw(g); p = P(sx + 0.05, -0.18 + off)
            gd.ellipse([p[0] - 9, p[1] - 9, p[0] + 9, p[1] + 9], fill=ec + (150,)); img.alpha_composite(g.filter(ImageFilter.GaussianBlur(5)))
    d = ImageDraw.Draw(img)
    B(-0.1, -0.08, 0.1, -0.05, (150, 90, 80))
    # hair
    hc = rgb(k['hair']) if k else (40, 28, 24)
    if not k:   # Base: short black hair
        B(-0.27, -0.55, 0.27, -0.42, hc); B(-0.27, -0.55, -0.2, -0.3, hc); B(0.2, -0.55, 0.27, -0.3, hc)
        for i in range(6): d.polygon([P(-0.25 + i * 0.09, -0.5 + off), P(-0.2 + i * 0.09, -0.75 + off), P(-0.12 + i * 0.09, -0.5 + off)], fill=hc)
    else:
        order = sorted(range(k['spikes']), key=lambda i: -strand(k, i)[0][2])
        for i in order:
            b, dr, ln = strand(k, i); p = [b[0], b[1], b[2]]; dd = list(dr); pts = [(p[0], p[1])]; ws = [0]
            for s in range(k['segs']):
                dd = bend(dd, k['droop'] * (s + 1) / k['segs'] * 1.4) if k['droop'] else dd
                p = [p[j] + dd[j] * ln for j in range(3)]; pts.append((p[0], p[1]))
            left, right = [], []
            for s, (x, y) in enumerate(pts):
                t = s / max(1, k['segs']); hw = k['width'] * (1 - 0.88 * t) * 0.5 * 1.5
                left.append(P(x - hw, y + off)); right.append(P(x + hw, y + off))
            poly = left + right[::-1]
            shade = 0.82 + 0.3 * hsh(i + 5); c = tuple(min(255, int(v * shade)) for v in hc)
            d.polygon(poly, fill=c, outline=tuple(max(0, v - 50) for v in c))
        for sx in (-0.27, 0.19): B(sx, -0.52, sx + 0.08, -0.3, hc)
        B(-0.27, -0.55, 0.27, -0.46, hc)
    # tail
    if k and k['tail']:
        pts = [P(0.1, 0.7 + off), P(0.5, 0.9 + off), P(0.75, 0.6 + off), P(0.7, 0.3 + off)]
        d.line(pts, fill=rgb(k['fur']), width=int(0.1 * S), joint='curve'); d.line(pts[2:], fill=(255, 240, 230), width=int(0.04 * S))
    return img

def main():
    out = sys.argv[1] if len(sys.argv) > 1 else 'look_preview.png'
    ids = sys.argv[2:] or [None, 'super_saiyan', 'super_saiyan_2', 'super_saiyan_3', 'super_saiyan_4', 'super_saiyan_god', 'super_saiyan_blue', 'true_ultra_instinct']
    ims = [render(i) for i in ids]; cols = 4; rows = (len(ims) + cols - 1) // cols
    sheet = Image.new('RGBA', (ims[0].width * cols, ims[0].height * rows))
    dr = ImageDraw.Draw(sheet)
    for n, (i, im) in enumerate(zip(ids, ims)):
        x, y = (n % cols) * im.width, (n // cols) * im.height; sheet.paste(im, (x, y)); dr.text((x + 8, y + 6), i or 'base (no look)', fill=(255, 255, 255, 255))
    sheet.save(out); print(out, len(ims), 'looks;', len(LOOKS), 'parsed')

if __name__ == '__main__':
    main()
