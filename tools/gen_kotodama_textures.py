"""Textures for 0.47 Kotodama Magic (Word Soul), Zagred's. Deterministic, procedural (no fonts, no input images).

    python tools/gen_kotodama_textures.py

  textures/particle/koto_{glyphs,smoke,clover,ring,crack,sword,trident,sludge}.png   the VFX (greyscale + alpha, tinted per quad)
  textures/block/underworld_matter{,_glow}.png (+ .mcmeta)                         the sludge block, animated; the glow layer is full-bright
  textures/item/otherworld_trident.png                                              the spoken trident
  textures/entity/zagred{,_glow}.png                                                the boss skin (64x64 humanoid) and its glowing eyes / runes
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_arcane_vfx_textures import fbm, grey, save  # noqa: E402

SS = 4
ASSETS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures")


def put(im, sub, name):
    d = os.path.join(ASSETS, sub)
    os.makedirs(d, exist_ok=True)
    im.save(os.path.join(d, name + ".png"), optimize=True)
    print("wrote", sub + "/" + name, im.size)


def mask(w, h, fn, blur=0.0):
    im = Image.new("L", (w * SS, h * SS), 0)
    fn(ImageDraw.Draw(im), SS)
    im = im.resize((w, h), Image.LANCZOS)
    if blur:
        im = im.filter(ImageFilter.GaussianBlur(blur))
    return np.asarray(im).astype(np.float32) / 255


def uv(w, h):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    return (xx + 0.5) / w, (yy + 0.5) / h


def blur(a, r):
    return np.asarray(Image.fromarray((np.clip(a, 0, 1) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(r))).astype(np.float32) / 255


# ================================================================ the demonic glyph atlas (4 x 4)
def glyph_strokes(rng):
    """One demonic letter: a spine, hooked arms, a diamond or crescent, a few ticks (unit square coordinates)."""
    s = []
    lean = rng.uniform(-0.12, 0.12)
    s.append(("line", [(0.5 + lean, 0.12), (0.5, 0.5), (0.5 - lean, 0.88)], 0.075))         # the spine
    for k in range(rng.integers(2, 4)):                                                    # hooked arms
        y = rng.uniform(0.2, 0.8)
        side = 1 if rng.random() < 0.5 else -1
        x1 = 0.5 + side * rng.uniform(0.22, 0.36)
        hook = y + rng.choice([-1, 1]) * rng.uniform(0.08, 0.16)
        s.append(("line", [(0.5, y), (x1, y + rng.uniform(-0.05, 0.05)), (x1 + side * 0.06, hook)], 0.06))
    kind = rng.integers(0, 3)
    cy = rng.uniform(0.25, 0.75)
    if kind == 0:
        s.append(("diamond", (0.5, cy), 0.09))
    elif kind == 1:
        s.append(("crescent", (0.5, cy), 0.12))
    else:
        s.append(("line", [(0.3, cy), (0.7, cy)], 0.05))
    for k in range(rng.integers(1, 3)):                                                    # ticks
        x, y = rng.uniform(0.2, 0.8), rng.uniform(0.1, 0.9)
        s.append(("line", [(x, y), (x + rng.uniform(-0.06, 0.06), y + 0.08)], 0.045))
    return s


def glyphs():
    S, C = 256, 64
    rng = np.random.default_rng(470)

    def draw(d, k):
        for cell in range(16):
            ox, oy = (cell % 4) * C * k, (cell // 4) * C * k
            pad = 0.12

            def P(x, y):
                return ox + (pad + x * (1 - 2 * pad)) * C * k, oy + (pad + y * (1 - 2 * pad)) * C * k
            for kind, a, b in glyph_strokes(rng):
                if kind == "line":
                    pts = [P(*p) for p in a]
                    d.line(pts, fill=255, width=max(1, int(b * C * k)), joint="curve")
                    for p in (pts[0], pts[-1]):
                        r = b * C * k * 0.5
                        d.ellipse((p[0] - r, p[1] - r, p[0] + r, p[1] + r), fill=255)
                elif kind == "diamond":
                    (x, y), r = P(*a), b * C * k
                    d.polygon([(x, y - r * 1.4), (x + r, y), (x, y + r * 1.4), (x - r, y)], fill=255)
                else:
                    (x, y), r = P(*a), b * C * k
                    d.ellipse((x - r, y - r, x + r, y + r), fill=255)
                    d.ellipse((x - r * 0.6 + r * 0.45, y - r * 0.8, x + r * 0.6 + r * 0.45, y + r * 0.8), fill=0)
    core = mask(S, S, draw, 0.4)
    glow = blur(core, 3.0)
    save(grey(np.clip(core + glow * 0.55, 0, 1), np.clip(0.8 + 0.2 * core, 0, 1)), "koto_glyphs")


# ================================================================ smoke, clover, ring, crack
def smoke():
    s = 128
    u, v = uv(s, s)
    x, y = u * 2 - 1, v * 2 - 1
    n = fbm(s, s, 32, 471)
    lobes = fbm(s, s, 64, 472)
    r = np.hypot(x, y) * (0.8 + 0.45 * lobes)
    a = np.clip(1 - r, 0, 1) ** 1.6 * (0.45 + 0.75 * n)
    save(grey(np.clip(a, 0, 1), np.clip(0.55 + 0.45 * n, 0, 1)), "koto_smoke")


def heart(d, cx, cy, size, angle, fill):
    pts = []
    for k in range(90):
        t = k / 90 * 2 * math.pi
        x = 16 * math.sin(t) ** 3
        y = 13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t)
        x, y = x / 17 * size, -(y + 17) / 17 * size
        ca, sa = math.cos(angle), math.sin(angle)
        pts.append((cx + x * ca - y * sa, cy + x * sa + y * ca))
    d.polygon(pts, fill=fill)


def clover():
    """The five-leaf clover emblem: five heart leaves round the centre, a rune ring, outer ticks."""
    S = 256

    def leaves(d, k):
        c = S * k / 2
        for i in range(5):
            ang = -math.pi / 2 + i * 2 * math.pi / 5
            heart(d, c, c, S * k * 0.15, ang + math.pi / 2, 255 if i != 4 else 200)         # the fifth leaf (the devil's) a shade darker
        for i in range(5):
            ang = -math.pi / 2 + i * 2 * math.pi / 5
            tip = (c + math.cos(ang) * S * k * 0.24, c + math.sin(ang) * S * k * 0.24)
            d.line([(c, c), tip], fill=110, width=int(2 * k))                                # the leaf's vein (its heart notch)
            gap = ang + math.pi / 5
            d.line([(c, c), (c + math.cos(gap) * S * k * 0.4, c + math.sin(gap) * S * k * 0.4)], fill=0, width=int(5 * k))   # seams between leaves
        d.ellipse((c - S * k * 0.035, c - S * k * 0.035, c + S * k * 0.035, c + S * k * 0.035), fill=0)

    def ring(d, k):
        c = S * k / 2
        d.ellipse((c - S * k * 0.47, c - S * k * 0.47, c + S * k * 0.47, c + S * k * 0.47), outline=255, width=int(3 * k))
        d.ellipse((c - S * k * 0.42, c - S * k * 0.42, c + S * k * 0.42, c + S * k * 0.42), outline=200, width=int(2 * k))
        for i in range(30):
            a = i * math.tau / 30
            r0, r1 = S * k * (0.42 if i % 3 else 0.39), S * k * 0.47
            d.line([(c + math.cos(a) * r0, c + math.sin(a) * r0), (c + math.cos(a) * r1, c + math.sin(a) * r1)], fill=255, width=int(2 * k))
    lf = mask(S, S, leaves, 0.5)
    rg = mask(S, S, ring, 0.4)
    u, v = uv(S, S)
    r = np.hypot(u * 2 - 1, v * 2 - 1)
    edge = np.clip(lf - blur(lf, 2.0) * 0.9, 0, 1) * 2                                      # bright leaf rims
    halo = blur(lf, 6.0) * 0.5
    a = np.clip(lf * 0.7 + edge + rg + halo, 0, 1) * (r < 1)
    save(grey(a, np.clip(0.75 + 0.25 * (edge + rg), 0, 1)), "koto_clover")


def ring():
    """A band of jagged demonic runes (U wraps round the ring, V across it)."""
    w, h = 256, 32
    rng = np.random.default_rng(473)

    def draw(d, k):
        d.line([(0, 3 * k), (w * k, 3 * k)], fill=255, width=int(2 * k))
        d.line([(0, (h - 3) * k), (w * k, (h - 3) * k)], fill=255, width=int(2 * k))
        x = 4
        while x < w - 8:
            gw = rng.uniform(8, 14)
            pts = [(x, h * 0.25), (x + gw * 0.5, h * rng.uniform(0.35, 0.65)), (x + gw, h * 0.75)]
            d.line([(p[0] * k, p[1] * k) for p in pts], fill=255, width=int(2 * k))
            if rng.random() < 0.6:
                cx = x + gw * 0.5
                d.polygon([(cx * k, h * 0.2 * k), ((cx + 2) * k, h * 0.5 * k), (cx * k, h * 0.8 * k), ((cx - 2) * k, h * 0.5 * k)], fill=200)
            x += gw + rng.uniform(3, 6)
    a = mask(w, h, draw, 0.3)
    save(grey(np.clip(a + blur(a, 1.5) * 0.5, 0, 1)), "koto_ring")


def crack():
    s = 128
    rng = np.random.default_rng(474)

    def draw(d, k):
        c = s * k / 2
        for i in range(9):
            a = i * math.tau / 9 + rng.uniform(-0.2, 0.2)
            x, y, r = c, c, 0.0
            pts = [(x, y)]
            while r < s * k * 0.48:
                r += rng.uniform(5, 10) * k
                a += rng.uniform(-0.35, 0.35)
                pts.append((c + math.cos(a) * r, c + math.sin(a) * r))
            d.line(pts, fill=255, width=max(1, int((2.4 - i % 3 * 0.5) * k)))
    a = mask(s, s, draw, 0.3)
    u, v = uv(s, s)
    r = np.hypot(u * 2 - 1, v * 2 - 1)
    flash = np.exp(-(r / 0.18) ** 2)
    save(grey(np.clip(a * np.clip(1.2 - r, 0, 1) + flash + blur(a, 2) * 0.4, 0, 1)), "koto_crack")


# ================================================================ demon sword, trident, sludge pool
def sword():
    """A demon sword, U = 0 at the point -> U = 1 at the pommel: a serrated black blade, a horned guard, a wrapped grip."""
    w, h = 256, 64

    def draw(d, k):
        top, bot = [], []
        for i in range(15):
            x = w * (0.02 + i * 0.045)
            half = h * (0.04 + 0.2 * min(1, i / 4))
            notch = (h * 0.06) if i % 2 else 0
            top.append((x * k, (h / 2 - half + notch) * k))
            bot.append((x * k, (h / 2 + half - notch) * k))
        d.polygon(top + bot[::-1], fill=230)
        gx = w * 0.69
        d.polygon([(gx * k, h * 0.05 * k), ((gx + 8) * k, h * 0.3 * k), ((gx + 8) * k, h * 0.7 * k), (gx * k, h * 0.95 * k),
                   ((gx - 6) * k, h * 0.75 * k), ((gx - 6) * k, h * 0.25 * k)], fill=255)                                  # horned guard
        d.rectangle(((gx + 8) * k, h * 0.42 * k, w * 0.94 * k, h * 0.58 * k), fill=170)                                      # grip
        for x in range(int(gx + 12), int(w * 0.94), 6):
            d.line([(x * k, h * 0.42 * k), ((x + 4) * k, h * 0.58 * k)], fill=120, width=int(1.5 * k))
        d.ellipse((w * 0.93 * k, h * 0.36 * k, w * 0.99 * k, h * 0.64 * k), fill=255)                                      # pommel
    a = mask(w, h, draw, 0.4)
    u, v = uv(w, h)
    edge = np.clip(a - blur(a, 1.2), 0, 1) * 2.5
    fuller = np.exp(-((v - 0.5) / 0.03) ** 2) * (u < 0.66) * (u > 0.08)
    save(grey(np.clip(a + blur(a, 2.5) * 0.35, 0, 1), np.clip(0.55 + 0.45 * edge + 0.3 * fuller, 0, 1)), "koto_sword")


def trident():
    """The otherworldly trident, U = 0 at the butt -> U = 1 at the prongs: a long shaft, a crescent collar, three barbed prongs."""
    w, h = 256, 64

    def draw(d, k):
        d.rectangle((w * 0.02 * k, h * 0.46 * k, w * 0.72 * k, h * 0.54 * k), fill=200)
        for x in np.arange(0.1, 0.7, 0.12):
            d.ellipse(((w * x - 2) * k, h * 0.42 * k, (w * x + 2) * k, h * 0.58 * k), fill=255)                             # rings on the shaft
        cx = w * 0.72
        d.polygon([(cx * k, h * 0.12 * k), ((cx + 6) * k, h * 0.2 * k), ((cx + 6) * k, h * 0.8 * k), (cx * k, h * 0.88 * k),
                   ((cx + 3) * k, h * 0.5 * k)], fill=255)                                                                      # the collar
        for y0, length in ((0.2, 0.22), (0.5, 0.26), (0.8, 0.22)):
            y = h * y0
            x0, x1 = cx + 4, cx + 4 + w * length
            d.polygon([(x0 * k, (y - 3) * k), (x1 * k, y * k), (x0 * k, (y + 3) * k)], fill=255)
            bx = x0 + (x1 - x0) * 0.55
            d.polygon([(bx * k, (y - 2) * k), ((bx - 8) * k, (y - 7 if y0 < 0.5 else y + 7) * k), ((bx + 3) * k, y * k)], fill=230)   # barb
        d.line([(cx + 4) * k, h * 0.2 * k, (cx + 4) * k, h * 0.8 * k], fill=255, width=int(4 * k))
    a = mask(w, h, draw, 0.4)
    edge = np.clip(a - blur(a, 1.2), 0, 1) * 2.5
    save(grey(np.clip(a + blur(a, 2.5) * 0.4, 0, 1), np.clip(0.6 + 0.4 * edge, 0, 1)), "koto_trident")


def sludge():
    s = 256
    u, v = uv(s, s)
    x, y = u * 2 - 1, v * 2 - 1
    r, ang = np.hypot(x, y), np.arctan2(y, x)
    wob = 0.82 + 0.1 * np.sin(ang * 5 + 1.3) + 0.06 * np.sin(ang * 11) + 0.08 * (fbm(s, s, 32, 475) - 0.5)
    body = np.clip((wob - r) / 0.06, 0, 1)
    veins = np.abs(fbm(s, s, 24, 476) - 0.5) < 0.035
    bubbles = np.zeros_like(r)
    rng = np.random.default_rng(477)
    for _ in range(18):
        bx, by, br = rng.uniform(-0.6, 0.6), rng.uniform(-0.6, 0.6), rng.uniform(0.02, 0.06)
        d = np.hypot(x - bx, y - by)
        bubbles = np.maximum(bubbles, np.clip(1 - np.abs(d - br) / 0.012, 0, 1))
    g = np.clip(0.35 + 0.4 * fbm(s, s, 16, 478) + 0.5 * veins + 0.6 * bubbles, 0, 1)
    save(grey(body * (0.85 + 0.15 * fbm(s, s, 8, 479)), g), "koto_sludge")


# ================================================================ the block (animated), the item, the boss
def block():
    """underworld_matter: 8 frames of ink-violet sludge with slowly swirling veins; the _glow layer is just the veins."""
    n, s = 8, 16
    base, glow = [], []
    for f in range(n):
        t = f / n * math.tau
        u, v = uv(s, s)
        swirl = np.sin((u + 0.15 * np.sin(v * 6 + t)) * 9 + t) * np.cos((v + 0.15 * np.cos(u * 6 - t)) * 7 - t)
        n1 = fbm(s, s, 4, 480 + f % 2)
        veins = np.clip(1 - np.abs(swirl) / 0.18, 0, 1) * (n1 > 0.35)
        col = np.dstack([18 + 30 * n1 + 70 * veins, 10 + 14 * n1 + 25 * veins, 28 + 40 * n1 + 120 * veins])
        base.append(np.dstack([np.clip(col, 0, 255), np.full((s, s), 255)]).astype(np.uint8))
        gl = np.dstack([180 * veins + 20, 110 * veins + 10, 255 * veins + 20, 255 * np.clip(veins * 1.2, 0, 1)])
        glow.append(np.clip(gl, 0, 255).astype(np.uint8))
    for name, frames in (("underworld_matter", base), ("underworld_matter_glow", glow)):
        put(Image.fromarray(np.concatenate(frames, axis=0), "RGBA"), "block", name)
        with open(os.path.join(ASSETS, "block", name + ".png.mcmeta"), "w", newline="\n") as fh:
            fh.write('{\n  "animation": {\n    "frametime": 4,\n    "interpolate": true\n  }\n}\n')


def item():
    """otherworld_trident: 32x32, diagonal (butt bottom-left, prongs top-right), black metal with violet glow."""
    s = 32
    im = Image.new("RGBA", (s * SS, s * SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    k = SS

    def P(x, y):
        return x * k, y * k
    d.line([P(3, 29), P(22, 10)], fill=(40, 30, 58, 255), width=3 * k)                       # shaft
    d.line([P(3, 29), P(22, 10)], fill=(120, 80, 200, 255), width=1 * k)
    d.polygon([P(19, 9), P(23, 13), P(25, 11), P(21, 7)], fill=(150, 110, 230, 255))          # collar
    for (tx, ty) in ((22, 2), (30, 10), (29, 3)):                                              # prongs
        d.line([P(22, 10), P(tx, ty)], fill=(26, 18, 40, 255), width=2 * k)
        d.polygon([P(tx - 1.2, ty + 1.2), P(tx + 0.4, ty - 0.4), P(tx + 0.8, ty + 0.8)], fill=(200, 160, 255, 255))
    d.ellipse((P(1.5, 27.5)[0], P(1.5, 27.5)[1], P(4.5, 30.5)[0], P(4.5, 30.5)[1]), fill=(150, 110, 230, 255))
    im = im.resize((s, s), Image.LANCZOS)
    halo = im.filter(ImageFilter.GaussianBlur(1.2))
    a = np.asarray(halo).astype(np.float32)
    a[..., 0], a[..., 1], a[..., 2] = 120, 60, 220
    a[..., 3] *= 0.6
    out = Image.alpha_composite(Image.fromarray(a.astype(np.uint8), "RGBA"), im)
    put(out, "item", "otherworld_trident")


def zagred():
    """64x64 humanoid skin: ink-black skin with violet rune lines, bone-dark horns, violet eyes; _glow = only eyes and runes."""
    W = 64
    n = fbm(W, W, 8, 481)
    skin = np.dstack([14 + 18 * n, 10 + 10 * n, 22 + 26 * n, np.full((W, W), 255.0)])
    glow = np.zeros((W, W, 4), np.float32)
    hat = (np.mgrid[0:W, 0:W][1] >= 32) & (np.mgrid[0:W, 0:W][0] < 16)                     # the hat layer stays empty
    skin[hat, 3] = 0

    def rune(x0, y0, x1, y1, seed):
        """A violet rune line zig-zagging through a face rectangle."""
        rng = np.random.default_rng(seed)
        x, y = x0 + (x1 - x0) // 2, y0
        while y < y1:
            skin[y, x, :3] = (110, 60, 200)
            glow[y, x] = (170, 100, 255, 255)
            y += 1
            x = int(np.clip(x + rng.integers(-1, 2), x0, x1 - 1))

    # face: eyes, a jagged mouth
    for ex in (9, 10, 13, 14):
        skin[12, ex, :3] = (210, 150, 255)
        glow[12, ex] = (230, 180, 255, 255)
    for mx, my in ((10, 14), (11, 15), (12, 14), (13, 15)):
        skin[my, mx, :3] = (4, 2, 8)
    # body front (20..28 x 20..32): a five-leaf mark and rune lines
    for (x, y) in ((24, 22), (23, 23), (25, 23), (22, 24), (26, 24), (24, 24), (23, 25), (25, 25)):
        skin[y, x, :3] = (150, 90, 240)
        glow[y, x] = (200, 140, 255, 255)
    rune(20, 26, 28, 32, 1)
    rune(32, 20, 40, 32, 2)                   # back
    for x0 in (40, 44, 48, 52):               # arms: a rune line on every face
        rune(x0, 20, x0 + 4, 32, 10 + x0)
    for x0 in (0, 4, 8, 12):                  # legs
        rune(x0, 20, x0 + 4, 32, 20 + x0)
    # horns (0..18 x 32..40): bone grey to black, violet tips
    yy, xx = np.mgrid[0:W, 0:W]
    horn = (yy >= 32) & (yy < 40) & (xx < 18)
    hs = np.clip((xx - 0) / 18, 0, 1)
    skin[horn, 0] = 70 - 50 * hs[horn]
    skin[horn, 1] = 62 - 46 * hs[horn]
    skin[horn, 2] = 74 - 50 * hs[horn]
    tip = (yy >= 32) & (yy < 36) & (xx >= 14) & (xx < 18)
    skin[tip, :3] = (140, 90, 230)
    glow[tip] = (160, 110, 255, 255)
    # tail (0..44 x 40..50): black with a violet spade at the tip
    spade = (yy >= 40) & (yy < 44) & (xx >= 34) & (xx < 44)
    skin[spade, :3] = (120, 70, 210)
    glow[spade] = (150, 90, 240, 255)
    put(Image.fromarray(np.clip(skin, 0, 255).astype(np.uint8), "RGBA"), "entity", "zagred")
    put(Image.fromarray(np.clip(glow, 0, 255).astype(np.uint8), "RGBA"), "entity", "zagred_glow")


def main():
    glyphs()
    smoke()
    clover()
    ring()
    crack()
    sword()
    trident()
    sludge()
    block()
    item()
    zagred()


if __name__ == "__main__":
    main()
