"""Draws the Jibril skill icon (32x32): white-gold wings, a halo and the scales of law on a deep blue disc with a gold rim.
Original art, drawn from scratch with Pillow. Run: python tools/gen_jibril_icon.py -> src/main/resources/assets/nusmp/textures/skill/veldanava/jibril.png
(Same family as tools/gen_israfil_icon.py and tools/gen_sandalphon_icon.py.)"""
import math
import os
from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "skill", "veldanava")
S = 32

GOLD = (255, 216, 122, 255)
GOLD_DK = (214, 156, 58, 255)
IVORY = (255, 246, 222, 255)
WHITE = (255, 255, 255, 255)
SKY = (160, 214, 250, 255)
SKY_DK = (96, 156, 214, 255)


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def wing(d, sx, c):
    """One wing: a leading edge arching up from the shoulder, feathers fanning down and out in three overlapping rows (sx = -1 left, +1 right)."""
    def P(x, y):
        return (c + sx * x, y)
    # the long feathers, outermost first so the inner ones lie on top
    rows = [  # (base x, base y, tip x, tip y, colour)
        (7.5, 12.5, 14.6, 17.5, SKY),
        (7.0, 11.8, 14.9, 14.0, IVORY),
        (6.0, 11.0, 14.0, 10.8, WHITE),
        (5.0, 10.4, 12.0, 7.6, IVORY),
        (4.0, 10.0, 9.4, 5.6, WHITE),
    ]
    for bx, by, tx, ty, col in rows:
        mx, my = (bx + tx) / 2, (by + ty) / 2
        nx, ny = -(ty - by), (tx - bx)
        nl = math.hypot(nx, ny) or 1
        nx, ny = nx / nl * 1.5, ny / nl * 1.5
        d.polygon([P(bx, by), P(mx + nx, my + ny), P(tx, ty), P(mx - nx, my - ny)], fill=col)
    # the shorter feathers under the arm
    for k in range(3):
        bx, by = 3.2 + k * 1.6, 11.0 - k * 0.6
        d.polygon([P(bx, by), P(bx + 1.5, by + 1.5), P(bx + 0.4, by + 5.2 - k * 0.8), P(bx - 1.0, by + 1.5)], fill=(IVORY if k % 2 == 0 else SKY))
    # the leading edge
    d.line([P(2.5, 12.0), P(4.5, 8.5), P(8.5, 5.8), P(12.5, 5.4)], fill=GOLD, width=1)
    d.line([P(2.5, 12.0), P(4.5, 9.2), P(8.5, 6.6), P(12.5, 6.2)], fill=GOLD_DK, width=1)


def icon():
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    px = im.load()
    c = (S - 1) / 2.0
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - c, y - c)
            if d <= 15.2:
                px[x, y] = lerp((26, 36, 92), (6, 9, 30), min(1.0, d / 15.2)) + (255,)
            if 14.2 < d <= 15.6:
                px[x, y] = (246, 206, 108, 255)                       # gold rim
    d = ImageDraw.Draw(im)
    wing(d, -1, c + 0.5)
    wing(d, 1, c + 0.5)
    # the halo above, a thin gold ellipse with a bright top
    d.ellipse([c - 5.5, 3.2, c + 6.5, 7.2], outline=GOLD)
    d.line([(c - 2.5, 3.6), (c + 3.5, 3.6)], fill=WHITE)
    # the scales of law: a beam, a pillar and two small pans
    d.line([(c - 5, 15), (c + 6, 15)], fill=GOLD)
    d.line([(c + 0.5, 15), (c + 0.5, 25)], fill=IVORY)
    d.line([(c - 2, 25), (c + 3, 25)], fill=GOLD)
    for sx in (-5, 6):
        d.line([(c + sx, 15), (c + sx - 2, 19)], fill=IVORY)
        d.line([(c + sx, 15), (c + sx + 2, 19)], fill=IVORY)
        d.line([(c + sx - 2, 19), (c + sx + 2, 19)], fill=GOLD)
        d.point([(c + sx - 1, 20), (c + sx, 20), (c + sx + 1, 20)], fill=GOLD_DK)
    d.point([(c + 0.5, 13)], fill=WHITE)
    # keep the corners clear (the disc is round)
    for y in range(S):
        for x in range(S):
            if math.hypot(x - c, y - c) > 15.7:
                px[x, y] = (0, 0, 0, 0)
    return im


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    icon().save(os.path.join(OUT, "jibril.png"))
    print("wrote", os.path.join(OUT, "jibril.png"))
