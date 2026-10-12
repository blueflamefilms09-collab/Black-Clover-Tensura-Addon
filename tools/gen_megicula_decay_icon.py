"""The HUD icon of Megicula's Decay (the brand of Vanica Zogratis's Curse of Decay): a blood drop split by a violet curse mark.

    python3 -B tools/gen_megicula_decay_icon.py

Writes assets/nusmp/textures/mob_effect/megicula_decay.png (18 x 18, the size of the other effect icons). Original pixel art.
"""
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "nusmp", "textures", "mob_effect", "megicula_decay.png")

# half width of the drop on each row (row 1 is the tip, row 16 the round bottom)
HALF = {1: 0.5, 2: 1.0, 3: 1.6, 4: 2.1, 5: 2.7, 6: 3.3, 7: 3.9, 8: 4.4, 9: 4.9, 10: 5.2, 11: 5.4, 12: 5.4, 13: 5.2, 14: 4.7, 15: 3.8, 16: 2.5}
CX = 8.5


def inside(x, y):
    return y in HALF and abs(x - CX) <= HALF[y]


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def main():
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    px = img.load()
    dark, mid, bright, edge = (92, 10, 20), (170, 24, 38), (214, 52, 62), (36, 3, 8)
    for y in range(18):
        for x in range(18):
            if not inside(x, y):
                continue
            border = not (inside(x - 1, y) and inside(x + 1, y) and inside(x, y - 1) and inside(x, y + 1))
            if border:
                px[x, y] = edge + (255,)
                continue
            d = abs(x + 0.5 - (CX + 0.5)) / max(0.5, HALF[y])                 # 0 at the middle, 1 at the side
            t = 0.25 + 0.75 * (1 - d)
            col = lerp(dark, mid, min(1.0, t * 1.2)) if t < 0.8 else lerp(mid, bright, (t - 0.8) / 0.2)
            px[x, y] = col + (255,)
    # a wet highlight on the upper left of the bulge
    for (x, y) in ((6, 8), (6, 9), (7, 8), (6, 10)):
        px[x, y] = (255, 138, 148, 255)
    # the curse mark: a jagged violet cross, burned black at the edges
    violet, glow, black = (138, 75, 214, 255), (206, 160, 255, 255), (14, 2, 18, 255)
    for (x, y) in ((6, 7), (7, 8), (8, 9), (9, 10), (10, 11), (11, 12), (11, 7), (10, 8), (9, 9), (8, 10), (7, 11), (6, 12), (8, 5), (9, 6), (8, 13), (9, 14)):
        if inside(x, y):
            px[x, y] = violet
    for (x, y) in ((8, 9), (9, 9), (8, 10), (9, 10)):
        px[x, y] = glow
    for (x, y) in ((5, 7), (12, 12), (12, 7), (5, 12), (7, 13), (10, 5)):
        if inside(x, y):
            px[x, y] = black
    # decay: a few cracks of nothing running down from the cross
    for (x, y) in ((7, 14), (10, 14), (9, 15), (8, 15)):
        if inside(x, y):
            px[x, y] = black
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    img.save(OUT)
    print("wrote", os.path.normpath(OUT))


if __name__ == "__main__":
    main()
