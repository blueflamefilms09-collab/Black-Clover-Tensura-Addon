"""Draws the Sandalphon skill icon (32x32): a pale-gold guillotine blade falling in front of a cross of light on a deep blue-black disc.
Original art, drawn from scratch with Pillow. Run: python tools/gen_sandalphon_icon.py -> src/main/resources/assets/nusmp/textures/skill/veldanava/sandalphon.png
(The next four skills of the Veldanava drop add their own icon builders next to this one.)"""
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
                t = min(1.0, d / 15.2)
                px[x, y] = lerp((22, 28, 58), (6, 8, 20), t) + (255,)
            if 14.2 < d <= 15.6:
                px[x, y] = (232, 196, 104, 255)                       # gold rim
    d = ImageDraw.Draw(im)
    # cross of light: vertical beam and a shorter horizontal bar
    for y in range(3, 29):
        px[15, y] = (255, 246, 214, 255)
        px[16, y] = (255, 246, 214, 255)
        px[14, y] = (150, 128, 70, 255) if px[14, y][3] else px[14, y]
        px[17, y] = (150, 128, 70, 255) if px[17, y][3] else px[17, y]
    for x in range(8, 24):
        px[x, 11] = (255, 246, 214, 255)
        px[x, 12] = (255, 246, 214, 255)
    # the falling blade: a slanted pale steel wedge with a black spine
    blade = [(7, 5), (25, 5), (25, 8), (11, 20), (7, 20)]
    d.polygon(blade, fill=(214, 222, 238, 255))
    d.line([(7, 5), (25, 5)], fill=(255, 255, 255, 255))
    d.line([(25, 5), (25, 8)], fill=(120, 130, 160, 255))
    d.line([(7, 5), (7, 20)], fill=(40, 44, 70, 255))
    d.line([(11, 20), (25, 8)], fill=(255, 255, 255, 255))
    # the tally ticks of the debt under the blade
    for i in range(5):
        x = 7 + i * 4
        d.line([(x, 24), (x, 27)], fill=(232, 196, 104, 255))
    return im


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    icon().save(os.path.join(OUT, "sandalphon.png"))
    print("wrote", os.path.join(OUT, "sandalphon.png"))
