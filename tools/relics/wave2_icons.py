#!/usr/bin/env python3
"""
Relics of Many Worlds, wave 2: the 16x16 inventory icons of the twelve charms and cloaks, drawn to the mod's look (a dark outline, light on the
top-left edges, shade on the bottom-right ones, a few bright details and glints), each with a rim pixel in the colour of its Tensura grade.

    python3 tools/relics/wave2_icons.py            # writes the icons and the flat item models
    python3 tools/relics/wave2_icons.py --sheet P  # also writes a contact sheet (scaled 12x) to the path P

Writes assets/nusmp/textures/item/<id>.png and assets/nusmp/models/item/<id>.json (parent item/generated, layer0 the icon) for the 12 ids of
wave 2 only. Idempotent: running it again rewrites the same files.
"""
import json
import math
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "nusmp")
TEX = os.path.join(ASSETS, "textures", "item")
MODELS = os.path.join(ASSETS, "models", "item")

OUTLINE = (22, 18, 30)
GRADE = {"NORMAL": 0xAAAAAA, "SPECIAL": 0x55FF55, "RARE": 0x55FFFF, "UNIQUE": 0xFF77FF, "LEGEND": 0xFFAA00}   # Grade.rgb()


def rgb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def light(c, t=0.28):
    return mix(c, (255, 255, 255), t)


def dark(c, t=0.32):
    return mix(c, (0, 0, 0), t)


class Icon:
    """A 16x16 RGBA canvas with the drawing steps every icon shares: body, bevel, outline, details, rim and glints."""

    def __init__(self):
        self.img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.img)

    # ---- drawing
    def rect(self, x0, y0, x1, y1, c, a=255):
        self.d.rectangle([x0, y0, x1, y1], fill=c + (a,))

    def poly(self, pts, c, a=255):
        self.d.polygon(pts, fill=c + (a,))

    def ellipse(self, x0, y0, x1, y1, c, a=255):
        self.d.ellipse([x0, y0, x1, y1], fill=c + (a,))

    def line(self, pts, c, a=255):
        self.d.line(pts, fill=c + (a,), width=1)

    def px(self, x, y, c, a=255):
        if 0 <= x < 16 and 0 <= y < 16:
            self.img.putpixel((x, y), c + (a,))

    def clear(self, x, y):
        self.img.putpixel((x, y), (0, 0, 0, 0))

    def alpha(self, x, y):
        return self.img.getpixel((x, y))[3]

    # ---- the shared passes
    def bevel(self):
        """Light on the top and left edges of every shape, shade on the bottom and right ones."""
        src = self.img.copy()
        for y in range(16):
            for x in range(16):
                r, g, b, a = src.getpixel((x, y))
                if a == 0:
                    continue
                up = y == 0 or src.getpixel((x, y - 1))[3] == 0
                left = x == 0 or src.getpixel((x - 1, y))[3] == 0
                down = y == 15 or src.getpixel((x, y + 1))[3] == 0
                right = x == 15 or src.getpixel((x + 1, y))[3] == 0
                c = (r, g, b)
                if up or left:
                    c = light(c, 0.22)
                elif down or right:
                    c = dark(c, 0.26)
                self.img.putpixel((x, y), c + (a,))

    def outline(self):
        src = self.img.copy()
        for y in range(16):
            for x in range(16):
                if src.getpixel((x, y))[3] != 0:
                    continue
                for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < 16 and 0 <= ny < 16 and src.getpixel((nx, ny))[3] > 120:
                        self.img.putpixel((x, y), OUTLINE + (255,))
                        break

    def rim(self, grade):
        """The first outline pixel from the top-left corner takes the colour of the grade."""
        col = rgb(GRADE[grade])
        for s in range(0, 31):
            for x in range(0, 16):
                y = s - x
                if 0 <= y < 16 and self.img.getpixel((x, y)) == OUTLINE + (255,):
                    self.img.putpixel((x, y), col + (255,))
                    return

    def glints(self, pts, c, a=190):
        for (x, y) in pts:
            if self.alpha(x, y) == 0:
                self.px(x, y, c, a)

    def finish(self, grade):
        self.bevel()
        self.outline()
        self.rim(grade)
        return self.img


# ====================================================================== the twelve icons
def sand_pouch():
    ic = Icon()
    sand = rgb(0xC9B073)
    ic.ellipse(4, 6, 12, 14, sand)
    ic.rect(6, 4, 10, 7, dark(sand, 0.08))
    ic.rect(5, 2, 11, 4, light(sand, 0.1))
    img = ic.finish("RARE")
    cord = rgb(0x7A4A28)
    for x in range(5, 12):
        ic.px(x, 6, cord)
    ic.px(11, 7, cord); ic.px(12, 8, cord)
    for (x, y) in ((6, 9), (9, 11), (8, 8), (7, 12), (10, 9), (5, 11)):
        ic.px(x, y, rgb(0xA58B4C))
    for (x, y) in ((7, 10), (9, 8), (6, 12), (8, 13)):
        ic.px(x, y, rgb(0xE8D6A0))
    for (x, y, a) in ((13, 3, 230), (14, 2, 200), (14, 4, 230), (15, 3, 170), (13, 5, 200), (15, 5, 150), (14, 6, 150), (15, 1, 120)):
        ic.px(x, y, rgb(0xE8D6A0), a)
    return img


def tome():
    ic = Icon()
    teal = rgb(0x2F5A55)
    for (x, c) in ((5, rgb(0xE8D080)), (8, rgb(0xD05050)), (10, rgb(0xF0D890))):
        ic.rect(x, 1, x + 1, 3, c)
    ic.rect(3, 3, 11, 13, teal)
    ic.rect(11, 4, 13, 12, rgb(0xE8D9A8))
    for (x, c) in ((4, rgb(0xD05050)), (7, rgb(0x5CB85C)), (10, rgb(0x9060C0))):
        ic.rect(x, 14, x + 1, 15, c)
    ic.rect(14, 5, 14, 6, rgb(0xE08A40))
    ic.rect(14, 9, 14, 10, rgb(0x4A90D0))
    img = ic.finish("RARE")
    brown = rgb(0x8B5A2B)
    ic.rect(8, 10, 13, 11, brown)
    ic.px(12, 10, rgb(0xB8B8C0)); ic.px(12, 11, rgb(0x8A8A92))
    ic.rect(4, 11, 7, 12, rgb(0xC8A060))
    ic.px(5, 11, rgb(0x6A4A20)); ic.px(6, 12, rgb(0x6A4A20))
    cyan = rgb(0x4DE8E8)
    for (x, y) in ((6, 4), (5, 5), (7, 5), (4, 6), (8, 6), (5, 7), (7, 7), (6, 8)):
        ic.px(x, y, cyan)
    ic.px(6, 6, rgb(0xC8FFFF))
    for (x, y, a) in ((2, 4, 200), (1, 5, 170), (2, 6, 200), (1, 7, 150), (2, 2, 120)):
        ic.px(x, y, rgb(0x6FE8D0), a)
    return img


def holy_chains():
    ic = Icon()
    gold = rgb(0xD4A82A)
    path = [(2, 2, "v"), (3, 5, "h"), (5, 8, "v"), (8, 10, "h"), (11, 8, "v"), (13, 5, "h"), (14, 2, "v")]
    for (cx, cy, o) in path:
        if o == "h":
            ic.rect(cx - 2, cy - 1, cx + 1, cy + 1, gold)
        else:
            ic.rect(cx - 1, cy - 2, cx + 1, cy + 1, gold)
    img = ic.finish("NORMAL")
    for (cx, cy, o) in path:                                                  # the hollow of each link, and a glint on its top edge
        if o == "h":
            ic.px(cx - 1, cy, OUTLINE); ic.px(cx, cy, OUTLINE)
            ic.px(cx - 1, cy - 1, rgb(0xFFF1A0))
        else:
            ic.px(cx, cy - 1, OUTLINE); ic.px(cx, cy, OUTLINE)
            ic.px(cx - 1, cy - 2, rgb(0xFFF1A0))
    ic.glints([(8, 13), (1, 7), (15, 7), (8, 6)], rgb(0xFFF1A0), 170)
    return img


def deck():
    ic = Icon()
    pivot = (8.0, 16.0)
    for ang in (-50, -25, 0, 25, 50):
        a = math.radians(ang)
        pts = []
        for (px_, py_) in ((-2.0, -10.5), (2.0, -10.5), (2.0, -2.0), (-2.0, -2.0)):
            x = pivot[0] + px_ * math.cos(a) - py_ * math.sin(a)
            y = pivot[1] + px_ * math.sin(a) + py_ * math.cos(a)
            pts.append((x, y))
        face = rgb(0xF6F2E6) if ang == 0 else mix(rgb(0xF6F2E6), rgb(0xB8B4C0), 0.18 * (abs(ang) // 26))
        ic.d.polygon(pts, fill=face + (255,), outline=rgb(0x4A465A) + (255,))
    img = ic.finish("SPECIAL")
    red = rgb(0xC0303A)
    for (x, y) in ((8, 4), (7, 5), (9, 5), (8, 6)):
        ic.px(x, y, red)
    ic.px(8, 5, rgb(0x8A1C28))
    gold = rgb(0xFFE9A0)
    ic.glints([(1, 6), (14, 6), (0, 10), (15, 10), (4, 1), (12, 1), (8, 0)], gold, 200)
    return img


def phantom_steps():
    ic = Icon()
    purple = rgb(0x3A2A5E)
    ic.rect(6, 2, 10, 9, purple)
    ic.rect(5, 1, 11, 3, rgb(0x6C7088))
    ic.poly([(5, 9), (10, 9), (13, 10), (14, 12), (14, 13), (5, 13)], purple)
    ic.px(6, 0, rgb(0xC0C4D8)); ic.px(10, 0, rgb(0xC0C4D8))
    ic.bevel()
    for y in range(7, 14):                                                    # fading into the ethereal plane towards the foot
        for x in range(16):
            r, g, b, a = ic.img.getpixel((x, y))
            if a == 0:
                continue
            t = min(1.0, (y - 6) / 6.0)
            c = mix((r, g, b), rgb(0x7A7CFF), t * 0.8)
            ic.img.putpixel((x, y), c + (int(255 - 100 * t),))
    ic.outline()
    ic.rim("RARE")
    silver = rgb(0xC0C4D8)
    for (x, y) in ((7, 2), (9, 2), (8, 3)):
        ic.px(x, y, silver)
    for (x, y) in ((7, 5), (7, 6), (8, 7)):
        ic.px(x, y, rgb(0x6A4AA0))
    ic.glints([(3, 12), (15, 11), (8, 15), (4, 10)], rgb(0x8C7CFF), 150)
    return ic.img


def cloak_depths():
    ic = Icon()
    teal = rgb(0x4B7F78)
    ic.ellipse(5, 1, 11, 7, teal)
    ic.poly([(5, 6), (11, 6), (14, 12), (2, 12)], teal)
    ic.rect(2, 12, 14, 13, rgb(0x1F2F5C))
    purple = rgb(0x6A4C93)
    ic.line([(3, 12), (2, 14), (0, 13), (0, 11)], purple)
    ic.line([(12, 12), (13, 14), (15, 13), (15, 11)], purple)
    ic.line([(2, 14), (3, 15)], purple); ic.line([(13, 14), (12, 15)], purple)
    img = ic.finish("RARE")
    ic.ellipse(6, 2, 10, 5, rgb(0x0E1530))
    ic.rect(7, 7, 8, 8, rgb(0xE8C040)); ic.px(7, 7, rgb(0xFFEA80))
    ic.px(9, 8, rgb(0x9A9AA8)); ic.px(10, 9, rgb(0x9A9AA8)); ic.px(10, 10, rgb(0x8A5A30))
    for (x, y) in ((5, 3), (10, 1), (4, 9), (11, 10), (7, 10)):
        ic.px(x, y, rgb(0xE8F0F0))
    for (x, y) in ((3, 13), (12, 13), (14, 12)):
        ic.px(x, y, rgb(0xBFEFFF))
    ic.glints([(1, 6), (14, 6)], rgb(0x8FD6E8), 160)
    return img


def cloak_moth():
    ic = Icon()
    russet = rgb(0x8B4A2A)
    ic.poly([(4, 5), (12, 5), (14, 14), (8, 15), (2, 14)], russet)
    ic.rect(6, 2, 10, 4, rgb(0x5A2E1A))
    ic.ellipse(3, 3, 13, 7, rgb(0xF1E6D2))
    orange = rgb(0xE8A050)
    ic.line([(6, 2), (5, 1), (4, 0)], orange); ic.px(3, 0, orange)
    ic.line([(10, 2), (11, 1), (12, 0)], orange); ic.px(13, 0, orange)
    img = ic.finish("RARE")
    ic.line([(8, 5), (8, 14)], rgb(0x2A1A14))
    for (cx, cy) in ((5, 10), (11, 10)):
        ic.rect(cx - 1, cy - 1, cx + 1, cy + 1, rgb(0x3A1E12))
        ic.px(cx, cy, rgb(0xF1E6D2))
    for (x, y) in ((6, 8), (10, 8), (6, 12), (10, 12), (4, 13), (12, 13)):
        ic.px(x, y, rgb(0xD98A4A))
    ic.px(7, 4, rgb(0x2A1A14)); ic.px(9, 4, rgb(0x2A1A14))
    ic.glints([(2, 2), (14, 2)], rgb(0xFF9A3C), 150)
    return img


def mantle_bat():
    ic = Icon()
    ic.rect(6, 1, 10, 3, rgb(0x1C1418))
    ic.poly([(3, 3), (13, 3), (15, 8), (14, 15), (11, 10), (8, 15), (5, 10), (2, 15), (1, 8)], rgb(0x2A1C1C))
    img = ic.finish("UNIQUE")
    ic.poly([(5, 4), (11, 4), (12, 9), (11, 10), (8, 13), (5, 10), (4, 9)], rgb(0x6B4328))
    ic.poly([(6, 5), (10, 5), (10, 8), (8, 10), (6, 8)], rgb(0x8A5A3A))
    for (x, y) in ((3, 4), (2, 6), (13, 4), (14, 6), (4, 12), (12, 12)):
        ic.px(x, y, rgb(0x4A3030))
    bone = rgb(0x1C1418)
    ic.line([(4, 4), (2, 13)], bone); ic.line([(12, 4), (14, 13)], bone); ic.line([(8, 11), (8, 14)], bone)
    ic.line([(5, 5), (5, 9)], rgb(0x4A3020)); ic.line([(11, 5), (11, 9)], rgb(0x4A3020))
    ic.px(7, 2, rgb(0x3A2A2A)); ic.px(9, 2, rgb(0x3A2A2A))
    ic.glints([(1, 12), (15, 12), (8, 0)], rgb(0xB06A4A), 150)
    return img


def cape_nine():
    ic = Icon()
    grey = rgb(0x33323A)
    ic.poly([(4, 7), (12, 7), (15, 14), (1, 14)], grey)
    ic.ellipse(4, 2, 12, 8, grey)
    ic.poly([(4, 3), (4, 0), (7, 2)], grey)
    ic.poly([(12, 3), (12, 0), (9, 2)], grey)
    img = ic.finish("LEGEND")
    ic.ellipse(5, 3, 11, 8, rgb(0xA01C58))
    ic.rect(6, 6, 10, 8, rgb(0x70103C))
    silver = rgb(0xC0C4CC)
    ic.d.arc([4, 2, 12, 9], 0, 360, fill=silver + (255,))
    ic.px(5, 1, rgb(0xE890B0)); ic.px(11, 1, rgb(0xE890B0))
    ic.px(7, 9, silver); ic.px(9, 9, silver); ic.px(8, 10, silver)
    for (x, y) in ((6, 11), (10, 11)):
        ic.rect(x, y, x + 1, y + 1, rgb(0x8A8A92))
    for x in range(3, 14):
        ic.px(x, 13, rgb(0xC03070))
    ic.glints([(2, 3), (14, 3), (8, 15)], rgb(0xFF4C9A), 150)
    return img


def cloak_comfort():
    ic = Icon()
    ic.poly([(5, 3), (11, 3), (13, 7), (12, 14), (8, 15), (4, 14), (3, 7)], rgb(0xF4F1EA))
    img = ic.finish("SPECIAL")
    gold = rgb(0xD8B24A)
    ic.rect(6, 3, 10, 5, rgb(0x6E4A9E))
    ic.line([(3, 7), (4, 14)], gold); ic.line([(13, 7), (12, 14)], gold)
    ic.line([(5, 3), (3, 7)], gold); ic.line([(11, 3), (13, 7)], gold)
    ic.ellipse(7, 5, 9, 7, gold)
    ic.px(8, 6, rgb(0x6E4A9E)); ic.px(7, 5, rgb(0xFFF2C8))
    ic.poly([(7, 8), (9, 8), (9, 13), (8, 15), (7, 13)], rgb(0xFFF2C8))
    ic.line([(8, 8), (8, 14)], gold)
    ic.glints([(2, 4), (14, 4), (1, 9), (15, 9)], rgb(0xB58AE0), 150)
    return img


def cloak_dichotomy():
    ic = Icon()
    shape = [(5, 1), (10, 1), (12, 4), (14, 10), (13, 15), (11, 12), (9, 15), (7, 12), (5, 15), (3, 13), (2, 10), (4, 4)]
    ic.poly(shape, rgb(0x888888))
    for y in range(16):                                                       # white at the top, fading to ink black
        for x in range(16):
            if ic.alpha(x, y) == 0:
                continue
            t = max(0.0, min(1.0, (y - 2) / 10.0))
            c = mix(rgb(0xF4F4F6), rgb(0x151518), t)
            ic.img.putpixel((x, y), c + (255,))
    img = ic.finish("RARE")
    ic.line([(8, 4), (8, 13)], rgb(0x8A8A94) if False else mix(rgb(0xB0B0B8), rgb(0x30303A), 0.55))
    for y in (3, 5):
        ic.px(7, y, rgb(0x9A9AA4))
    ic.px(7, 2, rgb(0xFFFFFF))
    ic.glints([(1, 4), (15, 4)], rgb(0xFFFFFF), 150)
    return img


def cloak_mimic():
    ic = Icon()
    ic.poly([(4, 4), (11, 4), (15, 10), (14, 15), (2, 15), (1, 10)], rgb(0xB01820))
    ic.rect(5, 2, 10, 4, rgb(0x1E3FA0))
    ic.poly([(9, 4), (11, 4), (15, 10), (14, 15), (9, 15)], rgb(0x1E3FA0))
    img = ic.finish("UNIQUE")
    gold = rgb(0xE8B830)
    ic.line([(9, 14), (14, 14)], gold); ic.line([(10, 5), (15, 11)], gold)
    white = rgb(0xF8F8F8)
    for (x, y) in ((8, 5), (9, 6), (8, 7), (9, 8), (8, 9), (9, 10), (8, 11), (9, 12), (8, 13), (9, 14), (2, 12), (3, 13), (1, 10), (4, 14)):
        ic.px(x, y, white)
    yellow = rgb(0xFFE020)
    for (x, y) in ((11, 7), (11, 8), (13, 11), (13, 12)):
        ic.px(x, y, yellow)
    blue = rgb(0x60B0F0)
    for (x, y) in ((6, 4), (5, 5), (7, 5), (5, 3)):
        ic.px(x, y, blue)
    ic.glints([(0, 5), (15, 5)], rgb(0xF0C020), 140)
    return img


ICONS = {
    "sand_of_the_pocket": sand_pouch,
    "the_tome_of_wait_thats_illegal": tome,
    "holy_chains": holy_chains,
    "pact_of_the_deck": deck,
    "phantom_steps": phantom_steps,
    "cloak_of_the_depths": cloak_depths,
    "cloak_of_the_moth_rider": cloak_moth,
    "nightstalkers_mantle": mantle_bat,
    "cape_of_nine_lives": cape_nine,
    "cloak_of_comfort": cloak_comfort,
    "cloak_of_dichotomy": cloak_dichotomy,
    "cloak_mimic_tame": cloak_mimic,
}


def main():
    os.makedirs(TEX, exist_ok=True)
    os.makedirs(MODELS, exist_ok=True)
    made = {}
    for rid, fn in ICONS.items():
        img = fn()
        img.save(os.path.join(TEX, rid + ".png"))
        with open(os.path.join(MODELS, rid + ".json"), "w", newline="\n") as f:
            json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/" + rid}}, f, indent=2)
            f.write("\n")
        made[rid] = img
    if "--sheet" in sys.argv:
        path = sys.argv[sys.argv.index("--sheet") + 1]
        scale = 12
        sheet = Image.new("RGBA", (4 * 16 * scale + 40, 3 * 16 * scale + 40), (58, 58, 66, 255))
        for i, (rid, img) in enumerate(made.items()):
            big = img.resize((16 * scale, 16 * scale), Image.NEAREST)
            sheet.alpha_composite(big, (10 + (i % 4) * (16 * scale + 6), 10 + (i // 4) * (16 * scale + 10)))
        sheet.save(path)
    print("wave 2: wrote %d icons" % len(made))


if __name__ == "__main__":
    main()
