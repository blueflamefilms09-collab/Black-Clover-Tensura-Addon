#!/usr/bin/env python3
"""
Textures of the Anime Multiverse UI theme (client/ui/theme, docs/gui_theme.md): the layers of the animated magic circle. Every texture is
white with alpha, 256x256, so that the game tints it (GuiGraphics.setColor) and draws it rotated and, for the glow, additively.
Each layer comes in three size tiers (name.png 256 px, name_128.png, name_64.png with fatter strokes), and the game picks the tier nearest to
the size it draws, so a thin rotating line never shrinks below about a pixel. They live in assets/nusmp/textures/gui/theme/ and each has a .mcmeta asking for linear filtering ("blur"), because the layers are drawn rotated.

    circle_outer.png   double ring, 72 tick marks, a band of 24 rune glyphs
    circle_star.png    ring with a hexagram, vertex nodes and an inner hexagon
    circle_poly.png    an octagon, two squares (an eight-pointed star), a dashed ring, a cross
    circle_inner.png   small ring of 12 arc dashes, an inner ring and four diamonds
    circle_arcs.png    three broken thick arcs (the fast "sweep" layer)
    circle_glow.png    soft radial bloom with a brighter ring (drawn additively)

Idempotent: running it again rewrites the same files (the rune glyphs come from a fixed seed).

    python3 tools/gen_ui_theme_textures.py                 # write the textures
    python3 tools/gen_ui_theme_textures.py --sheet out.png # also a contact sheet of the layers on dark (not part of the build)
"""
import json
import math
import os
import random
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "nusmp", "textures", "gui", "theme")

BASE = 256
SS = 4                      # supersampling
WHITE = (255, 255, 255, 255)
SIZE = BASE
C = R = K = M = 0.0


def setup(size, mult):
    """Sets the canvas for one size tier: 'size' px square; 'mult' thickens strokes (the small tiers need fatter lines to survive minifying)."""
    global SIZE, C, R, K, M
    SIZE = size
    C = SIZE * SS / 2.0
    R = C - 4 * SS * size / BASE
    K = size / float(BASE)
    M = mult


def canvas():
    img = Image.new("RGBA", (SIZE * SS, SIZE * SS), (255, 255, 255, 0))
    return img, ImageDraw.Draw(img)


def sp(v):
    """A spacing / radius in 256-px design units, scaled to the tier."""
    return v * SS * K


def px(v):
    """A stroke width in 256-px design units, scaled to the tier and thickened for the small tiers (never under 1.3 real px)."""
    return max(v * SS * K * M, 1.3 * SS)


def pol(r, deg):
    a = math.radians(deg)
    return (C + r * math.cos(a), C + r * math.sin(a))


def ring(d, r, w, alpha=255):
    col = (255, 255, 255, alpha)
    d.ellipse([C - r, C - r, C + r, C + r], outline=col, width=max(1, int(w)))


def line(d, p, q, w, alpha=255):
    d.line([p, q], fill=(255, 255, 255, alpha), width=max(1, int(w)))
    r = w / 2.0
    for (x, y) in (p, q):                         # round caps
        d.ellipse([x - r, y - r, x + r, y + r], fill=(255, 255, 255, alpha))


def dot(d, p, r, alpha=255):
    d.ellipse([p[0] - r, p[1] - r, p[0] + r, p[1] + r], fill=(255, 255, 255, alpha))


def arc(d, r, a0, a1, w, alpha=255):
    d.arc([C - r, C - r, C + r, C + r], a0, a1, fill=(255, 255, 255, alpha), width=max(1, int(w)))


def polygon(d, r, n, rot, w, alpha=255):
    pts = [pol(r, rot + 360.0 * k / n) for k in range(n)]
    for k in range(n):
        line(d, pts[k], pts[(k + 1) % n], w, alpha)
    return pts


def finish(img):
    return img.resize((SIZE, SIZE), Image.LANCZOS)


# ---------------------------------------------------------------- rune glyphs
def rune(rng):
    """Strokes on a 3x5 grid, in unit coordinates (x in -1..1, y in 0..1): an invented script, no real alphabet."""
    cols = [-1.0, 0.0, 1.0]
    rows = [0.0, 0.25, 0.5, 0.75, 1.0]
    nodes = [(c, r) for c in cols for r in rows]
    strokes = []
    spine = rng.random() < 0.6
    if spine:
        strokes.append(((0.0, 0.0), (0.0, 1.0)))
    cur = rng.choice(nodes)
    for _ in range(rng.randint(2, 4)):
        nxt = rng.choice(nodes)
        if nxt == cur:
            continue
        strokes.append((cur, nxt))
        cur = nxt if rng.random() < 0.7 else rng.choice(nodes)
    if rng.random() < 0.35:
        strokes.append(((-1.0, rng.choice(rows)), (1.0, rng.choice(rows))))
    return strokes


def draw_rune(d, strokes, r_in, r_out, deg, width_deg, w):
    """Draw one glyph radially: y from r_in to r_out, x across width_deg degrees, centred at angle deg."""
    for (a, b) in strokes:
        pts = []
        for (x, y) in (a, b):
            rr = r_in + (r_out - r_in) * y
            ang = deg + x * width_deg / 2.0
            pts.append(pol(rr, ang))
        line(d, pts[0], pts[1], w)


# ---------------------------------------------------------------- the layers
def make_outer():
    img, d = canvas()
    ring(d, R, px(2.4))
    ring(d, R - sp(7), px(1.2), 200)
    for k in range(72):                                     # tick marks between the two outer rings
        a = k * 5.0
        long_ = k % 6 == 0
        line(d, pol(R - sp(1.5), a), pol(R - sp(1.5) - sp(5.5 if long_ else 2.8), a), px(1.4 if long_ else 0.9), 255 if long_ else 190)
    r_band_out, r_band_in = R - sp(17), R - sp(47)
    ring(d, r_band_out, px(1.6))
    ring(d, r_band_in, px(1.6))
    rng = random.Random(0x4D554C54)
    for k in range(24):                                     # 24 glyphs in the band, with a divider between them
        base = k * 15.0
        draw_rune(d, rune(rng), r_band_in + sp(5), r_band_out - sp(5), base + 7.5, 9.0, px(2.2))
        line(d, pol(r_band_in, base), pol(r_band_out, base), px(1.1), 160)
    return finish(img)


def make_star():
    img, d = canvas()
    r = R - sp(6)
    ring(d, r, px(2.2))
    ring(d, r - sp(5), px(1.0), 170)
    a = polygon(d, r - sp(5), 3, -90, px(2.2))
    b = polygon(d, r - sp(5), 3, 90, px(2.2))
    for (x, y) in a + b:                                    # nodes at the six points
        dot(d, (x, y), sp(5.0))
        d.ellipse([x - sp(9), y - sp(9), x + sp(9), y + sp(9)], outline=WHITE, width=int(px(1.3)))
    inner = (r - sp(5)) * 0.5                               # the hexagram's inner hexagon
    polygon(d, inner, 6, 0, px(1.6), 220)
    ring(d, inner * 0.62, px(1.3), 200)
    for k in range(6):
        line(d, pol(inner, k * 60), pol(inner * 0.62, k * 60), px(1.1), 170)
    dot(d, (C, C), sp(3.2))
    return finish(img)


def make_poly():
    img, d = canvas()
    r = R - sp(14)
    polygon(d, r, 8, 22.5, px(2.2))
    sq1 = polygon(d, r, 4, 0, px(1.8))
    sq2 = polygon(d, r, 4, 45, px(1.8))
    for (x, y) in sq1 + sq2:
        dot(d, (x, y), sp(3.6))
    ring(d, r * 0.72, px(1.4), 210)
    for k in range(32):                                     # dashed ring
        arc(d, r * 0.62, k * 11.25, k * 11.25 + 6.0, px(2.0), 220)
    for k in range(4):
        line(d, pol(r * 0.56, k * 90 + 45), pol(r * 0.18, k * 90 + 45), px(1.3), 190)
        line(d, pol(r * 0.5, k * 90), pol(r * 0.22, k * 90), px(1.0), 150)
    ring(d, r * 0.2, px(1.4))
    return finish(img)


def make_inner():
    img, d = canvas()
    r = R - sp(30)
    ring(d, r, px(1.6), 220)
    for k in range(12):                                     # arc dashes of two lengths
        a0 = k * 30.0
        arc(d, r * 0.82, a0 + 3, a0 + (22 if k % 2 == 0 else 12), px(3.4))
    ring(d, r * 0.55, px(1.3), 210)
    for k in range(4):                                      # four diamonds
        p = pol(r * 0.55, k * 90 + 45)
        s = sp(7)
        d.polygon([(p[0], p[1] - s), (p[0] + s * 0.7, p[1]), (p[0], p[1] + s), (p[0] - s * 0.7, p[1])], fill=WHITE)
    for k in range(12):
        line(d, pol(r * 0.95, k * 30 + 15), pol(r * 0.88, k * 30 + 15), px(1.0), 160)
    dot(d, (C, C), sp(2.6))
    return finish(img)


def make_arcs():
    img, d = canvas()
    r = R - sp(3)
    for (a0, a1, w, al) in ((10, 78, 3.4, 255), (130, 175, 2.2, 220), (222, 318, 3.0, 255), (338, 352, 2.0, 200)):
        arc(d, r, a0, a1, px(w), al)
        arc(d, r - sp(6), a0 + 6, a1 - 8, px(1.2), 150)
    return finish(img)


def make_glow():
    setup(BASE, 1.0)
    img = Image.new("RGBA", (SIZE, SIZE), (255, 255, 255, 0))
    px_ = img.load()
    c = (SIZE - 1) / 2.0
    for y in range(SIZE):
        for x in range(SIZE):
            r = math.hypot(x - c, y - c) / (SIZE / 2.0)
            if r >= 1.0:
                continue
            a = (1.0 - r) ** 2.4 * 0.55                          # core bloom
            a += math.exp(-((r - 0.86) / 0.07) ** 2) * 0.55      # the ring's own halo
            a *= min(1.0, (1.0 - r) * 8.0)                       # no hard edge at the border
            px_[x, y] = (255, 255, 255, int(max(0.0, min(1.0, a)) * 255))
    return img


LAYERS = {
    "circle_outer": make_outer,
    "circle_star": make_star,
    "circle_poly": make_poly,
    "circle_inner": make_inner,
    "circle_arcs": make_arcs,
}
# size tiers: the game picks the one nearest the drawn size so a rotating line never shrinks below about a pixel
TIERS = ((256, 1.0, ""), (128, 1.45, "_128"), (64, 2.2, "_64"))


def write(img, name):
    img.save(os.path.join(OUT, name), optimize=True)
    with open(os.path.join(OUT, name + ".mcmeta"), "w", newline="\n") as fh:
        json.dump({"texture": {"blur": True, "clamp": True}}, fh)
        fh.write("\n")
    print("wrote", name)


def main():
    os.makedirs(OUT, exist_ok=True)
    imgs = {}
    for size, mult, suffix in TIERS:
        for base, fn in LAYERS.items():
            setup(size, mult)
            img = fn()
            write(img, base + suffix + ".png")
            if not suffix:
                imgs[base] = img
    glow = make_glow()
    write(glow, "circle_glow.png")
    imgs["circle_glow"] = glow
    if "--sheet" in sys.argv:
        out = sys.argv[sys.argv.index("--sheet") + 1]
        sheet = Image.new("RGBA", (BASE * 3, BASE * 2), (10, 10, 18, 255))
        for i, name in enumerate(imgs):
            layer = imgs[name]
            a = layer.split()[3]
            tinted = Image.merge("RGBA", (*Image.new("RGBA", (BASE, BASE), (150, 190, 255, 255)).split()[:3], a))
            sheet.alpha_composite(tinted, ((i % 3) * BASE, (i // 3) * BASE))
        sheet.save(out)
        print("sheet", out)


if __name__ == "__main__":
    main()
