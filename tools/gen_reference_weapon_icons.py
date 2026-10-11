"""0.100: 16x16 inventory icons for the weapons of "Custom weapon i want added" (diagonal pixel swords, same pose as the older icons).

    python tools/gen_reference_weapon_icons.py
Writes assets/nusmp/textures/item/<id>.png. The 3D in-hand models come from tools/gen_weapon_models.py.
"""
import colorsys
import os

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "item")
S = 16
GOLD = ((150, 104, 30), (226, 178, 62), (255, 224, 122))


def rgba(c):
    return (c[0], c[1], c[2], 255)


def sword(item_id, outline, dark, mid, light, spark, guard=GOLD, grip=(30, 30, 44), gem=None, thick=2, spectrum=False, tip=14):
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    px = im.load()

    def put(x, y, c):
        if 0 <= x < S and 0 <= y < S:
            px[x, y] = rgba(c) if len(c) == 3 else c

    cells = [(4 + k, 11 - k) for k in range(0, tip - 3)]
    for (x, y) in cells:
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                put(x + dx, y + dy, outline)
        if thick == 3:
            for (ex, ey) in ((x + 1, y), (x, y + 1), (x - 1, y), (x, y - 1)):
                for dx in (-1, 0, 1):
                    for dy in (-1, 0, 1):
                        put(ex + dx, ey + dy, outline)
    for i, (x, y) in enumerate(cells):
        if spectrum:
            h = i / max(1, len(cells) - 1) * 0.85
            r, g, b = colorsys.hsv_to_rgb(h, 0.55, 1.0)
            c = (int(r * 255), int(g * 255), int(b * 255))
            d = tuple(int(v * 0.7) for v in c)
            put(x, y, c); put(x + 1, y, d); put(x, y + 1, d); put(x, y - 1, tuple(min(255, v + 50) for v in c))
        else:
            put(x, y, mid); put(x + 1, y, dark); put(x, y + 1, dark); put(x, y - 1, light)
        if thick == 3:
            put(x - 1, y, mid); put(x, y + 1, dark); put(x + 1, y + 1, dark)
        if i % 3 == 1:
            put(x, y, spark)
    put(15, 0, spark)
    # guard across the diagonal at (3,12)
    g0, g1, g2 = guard
    for (x, y, c) in [(1, 10, g0), (2, 11, g1), (3, 12, g2), (4, 13, g1), (5, 14, g0), (2, 12, g1), (3, 11, g1), (4, 12, g1), (3, 13, g1)]:
        put(x, y, c)
    if gem:
        put(3, 12, gem)
    put(2, 13, grip); put(1, 14, grip); put(0, 15, g1); put(1, 15, g0); put(0, 14, g0)
    im.save(os.path.join(OUT, item_id + ".png"))


def main():
    sword("magebane_longsword", (10, 40, 44), (30, 110, 112), (60, 170, 168), (150, 235, 225), (255, 190, 100), guard=((40, 34, 40), (80, 70, 82), (130, 120, 130)), gem=(90, 235, 225))
    sword("prism_blade", (30, 30, 80), (80, 80, 160), (150, 150, 230), (230, 230, 255), (255, 255, 255), guard=GOLD, grip=(26, 36, 88), gem=(255, 150, 225), spectrum=True)
    sword("thundercleave", (10, 24, 34), (34, 54, 68), (62, 92, 108), (120, 235, 255), (230, 255, 255), guard=GOLD, grip=(30, 40, 56), gem=(100, 220, 255), thick=3, tip=15)
    sword("bloodbound_katana", (8, 4, 10), (22, 14, 24), (44, 30, 48), (200, 24, 50), (255, 70, 90), guard=((70, 14, 24), (130, 24, 40), (210, 40, 60)), grip=(40, 20, 30), gem=(255, 60, 90))
    sword("quicklings_ivy", (18, 60, 22), (44, 110, 44), (96, 180, 74), (190, 255, 130), (240, 255, 200), guard=((60, 110, 40), (210, 170, 64), (255, 224, 122)), grip=(50, 100, 40), gem=(150, 255, 120))
    sword("permafrost_sabre", (30, 70, 110), (110, 170, 215), (170, 220, 245), (240, 252, 255), (255, 255, 255), guard=GOLD, grip=(30, 60, 110), gem=(170, 240, 255))
    sword("cyclone_rapier", (40, 60, 70), (140, 160, 175), (190, 206, 220), (240, 246, 252), (110, 245, 232), guard=GOLD, grip=(40, 70, 80), gem=(120, 250, 235))


if __name__ == "__main__":
    main()
