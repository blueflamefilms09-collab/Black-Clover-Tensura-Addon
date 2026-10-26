"""Draws the Israfil skill icon (32x32): a cyan maze spiral and a brass trumpet on a deep indigo disc.
Original art, drawn from scratch with Pillow. Run: python tools/gen_israfil_icon.py -> src/main/resources/assets/nusmp/textures/skill/veldanava/israfil.png
(Same family as tools/gen_sandalphon_icon.py; the next three skills add their own builders.)"""
import math
import os
from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "skill", "veldanava")
S = 32


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def icon():
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    px = im.load()
    c = (S - 1) / 2.0
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - c, y - c)
            if d <= 15.2:
                px[x, y] = lerp((24, 22, 70), (6, 8, 26), min(1.0, d / 15.2)) + (255,)
            if 14.2 < d <= 15.6:
                px[x, y] = (96, 224, 236, 255)                        # cyan rim
    d = ImageDraw.Draw(im)
    # the labyrinth: concentric square-cornered rings, each with a gap, as maze walls
    for k, r in enumerate((12, 9, 6)):
        gap = (k * 2) % 4
        box = [c - r, c - r, c + r, c + r]
        d.rectangle(box, outline=(70, 170, 190, 255))
        # open one side of each ring so the maze can be walked
        if gap == 0:
            d.line([(c - 2, c - r), (c + 2, c - r)], fill=(24, 22, 70, 255))
        elif gap == 2:
            d.line([(c - 2, c + r), (c + 2, c + r)], fill=(24, 22, 70, 255))
    # the trumpet: a brass bell opening to the upper right, a thin tube back to the mouthpiece
    d.line([(7, 24), (17, 17)], fill=(214, 166, 72, 255), width=2)
    d.polygon([(17, 14), (17, 20), (26, 25), (26, 9)], fill=(232, 192, 96, 255))
    d.line([(26, 9), (26, 25)], fill=(255, 238, 170, 255))
    d.line([(17, 14), (26, 9)], fill=(150, 108, 40, 255))
    d.line([(17, 20), (26, 25)], fill=(150, 108, 40, 255))
    # sound rings leaving the bell
    for r in (3, 5):
        d.arc([26 - r, 17 - r, 26 + r, 17 + r], -60, 60, fill=(200, 250, 255, 255))
    return im


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    icon().save(os.path.join(OUT, "israfil.png"))
    print("wrote", os.path.join(OUT, "israfil.png"))
