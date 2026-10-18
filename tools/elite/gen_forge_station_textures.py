"""Generates the textures of the four forge stations (6 block faces each) and the three station upgrade kits (item icons).

    python3 -B tools/elite/gen_forge_station_textures.py

Output: src/main/resources/assets/nusmp/textures/block/forge_station_<tier>_{side,side_lit,plate,top,top_lit,bottom}.png and
        src/main/resources/assets/nusmp/textures/item/forge_upgrade_kit_<tier>.png   (16x16, deterministic, Pillow only).

Why a script: four tiers share one design and differ in a palette, so the palette table below is the single place the look lives (add a tier = add a row). Each tier
reads as "a better forge": Basic is soot-black iron with a copper trim and an ember glow; Magisteel is blue steel with a silver trim and a pale cyan glow; Mythril is
teal metal with a mythril-white trim and an aqua glow; Dragonforge is black scale with a gold trim and a red-orange fire. The side has a furnace slit that glows (brighter
in the "lit" variant, which the block uses while somebody is forging), the plate is the trim band of the work surface, the top carries a rune circle (dim, or blazing
and with sparks when lit).
"""
import os
import random
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "..", "src", "main", "resources", "assets", "nusmp", "textures")
BLOCK = os.path.join(ROOT, "block")
ITEM = os.path.join(ROOT, "item")

# tier: (body shades x4, dark, trim shades x3, glow shades x3)
TIERS = {
    "basic": ([(58, 54, 52), (66, 60, 56), (74, 66, 60), (46, 42, 42)], (28, 24, 24),
              [(214, 140, 84), (176, 108, 60), (122, 74, 40)], [(255, 214, 120), (255, 150, 50), (200, 80, 24)]),
    "magisteel": ([(82, 96, 120), (92, 108, 132), (102, 118, 142), (66, 78, 100)], (34, 42, 58),
                  [(226, 234, 246), (186, 198, 216), (132, 146, 170)], [(220, 250, 255), (120, 210, 255), (50, 130, 220)]),
    "mythril": ([(52, 108, 116), (60, 120, 128), (70, 132, 140), (40, 88, 96)], (18, 48, 56),
                [(228, 255, 250), (160, 240, 228), (96, 178, 170)], [(220, 255, 244), (110, 255, 214), (30, 190, 160)]),
    "dragonforge": ([(40, 26, 30), (50, 32, 36), (60, 38, 42), (30, 18, 22)], (14, 8, 10),
                    [(255, 224, 120), (226, 176, 66), (150, 108, 36)], [(255, 232, 150), (255, 96, 40), (200, 30, 20)]),
}


def base(body, seed):
    rnd = random.Random(seed)
    im = Image.new("RGB", (16, 16))
    for y in range(16):
        for x in range(16):
            im.putpixel((x, y), body[rnd.randrange(4)])
    return im


def frame(im, trim, dark):
    for i in range(16):
        for p in ((i, 0), (i, 15), (0, i), (15, i)):
            im.putpixel(p, trim[1])
        if 1 < i < 14:
            for p in ((i, 1), (i, 14), (1, i), (14, i)):
                im.putpixel(p, dark)


def rivets(im, col):
    for p in ((2, 2), (13, 2), (2, 13), (13, 13)):
        im.putpixel(p, col)


def save(im, path, name):
    os.makedirs(path, exist_ok=True)
    im.save(os.path.join(path, name + ".png"))


def side(tier, lit):
    body, dark, trim, glow = TIERS[tier]
    im = base(body, 11 + len(tier))
    frame(im, trim, dark)
    rivets(im, trim[0])
    # the furnace slit: a dark mouth with the fire in it
    for y in range(6, 11):
        for x in range(4, 12):
            im.putpixel((x, y), dark)
    for x in range(5, 11):
        im.putpixel((x, 9), glow[1] if not lit else glow[0])
        im.putpixel((x, 8), glow[2] if not lit else glow[1])
    if lit:
        for x in range(5, 11):
            im.putpixel((x, 7), glow[2])
        im.putpixel((7, 6), glow[0])
        im.putpixel((8, 6), glow[1])
    for x in range(3, 13):
        im.putpixel((x, 5), trim[2])
        im.putpixel((x, 11), trim[2])
    return im


def plate(tier):
    body, dark, trim, glow = TIERS[tier]
    im = base(body, 23 + len(tier))
    for x in range(16):
        im.putpixel((x, 0), trim[0])
        im.putpixel((x, 1), trim[1])
        im.putpixel((x, 3), dark)
        im.putpixel((x, 15), dark)
    for x in range(1, 16, 4):
        im.putpixel((x, 2), trim[2])
    return im


def top(tier, lit):
    body, dark, trim, glow = TIERS[tier]
    im = base(body, 37 + len(tier))
    frame(im, trim, dark)
    cx, cy = 7.5, 7.5
    for y in range(16):
        for x in range(16):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if 4.6 <= d <= 5.6:
                im.putpixel((x, y), glow[1] if lit else glow[2])
            elif 2.0 <= d <= 2.7:
                im.putpixel((x, y), glow[2] if not lit else glow[1])
    # a cross of runes through the circle
    for i in range(3, 13):
        if i not in (7, 8):
            im.putpixel((i, 7), glow[2] if not lit else glow[0])
            im.putpixel((7, i), glow[2] if not lit else glow[0])
    im.putpixel((7, 7), glow[0] if lit else glow[1])
    im.putpixel((8, 8), glow[0] if lit else glow[1])
    if lit:
        rnd = random.Random(7)
        for _ in range(7):
            im.putpixel((rnd.randrange(2, 14), rnd.randrange(2, 14)), glow[0])
    return im


def bottom(tier):
    body, dark, trim, glow = TIERS[tier]
    im = base([dark, body[3], dark, body[3]], 51 + len(tier))
    for i in range(16):
        im.putpixel((i, 0), trim[2])
        im.putpixel((0, i), trim[2])
    return im


def kit(tier):
    """A 16x16 item icon: a small anvil over a bordered plate, in the tier's colours."""
    body, dark, trim, glow = TIERS[tier]
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(2, 15):
        for x in range(2, 14):
            edge = x in (2, 13) or y in (2, 14)
            im.putpixel((x, y), trim[1] + (255,) if edge else dark + (255,))
    for x in range(3, 13):
        im.putpixel((x, 3), trim[0] + (255,))
    anvil = {  # row: (x0, x1)
        5: (4, 12), 6: (3, 12), 7: (5, 11), 8: (6, 10), 9: (6, 10), 10: (5, 11), 11: (4, 12),
    }
    for y, (a, b) in anvil.items():
        for x in range(a, b):
            im.putpixel((x, y), body[1 + (x + y) % 3] + (255,))
    for x in range(4, 12):
        im.putpixel((x, 5), trim[0] + (255,))
    im.putpixel((8, 4), glow[0] + (255,))
    im.putpixel((7, 4), glow[1] + (255,))
    im.putpixel((9, 4), glow[1] + (255,))
    for x in range(5, 11):
        im.putpixel((x, 12), glow[1] + (255,))
    return im


def build():
    for tier in TIERS:
        save(side(tier, False), BLOCK, "forge_station_%s_side" % tier)
        save(side(tier, True), BLOCK, "forge_station_%s_side_lit" % tier)
        save(plate(tier), BLOCK, "forge_station_%s_plate" % tier)
        save(top(tier, False), BLOCK, "forge_station_%s_top" % tier)
        save(top(tier, True), BLOCK, "forge_station_%s_top_lit" % tier)
        save(bottom(tier), BLOCK, "forge_station_%s_bottom" % tier)
        if tier != "basic":
            save(kit(tier), ITEM, "forge_upgrade_kit_%s" % tier)
        print("wrote", tier)


if __name__ == "__main__":
    build()
