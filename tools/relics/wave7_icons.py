#!/usr/bin/env python3
"""Relics of Many Worlds, wave 7: the 16x16 inventory icons of the ten chest armours.

Writes textures/item/<id>.png and models/item/<id>.json (parent item/generated) under
src/main/resources/assets/nusmp for every id of wave 7. Idempotent. Shapes are drawn per look (plate, leather,
coat, suit...) as fill masks; the outline, the light and dark edges are derived from the mask, then each armour
paints its own details (cracks, studs, lapels, gems...) from its card art. One rim pixel per grade star
(rare 1, unique 2, legend 3) is in the grade colour; glow pixels sit on the gems and veins.

Run from the repository root:  python tools/relics/wave7_icons.py
"""
import json
import os

from PIL import Image

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")

GRADE_RGB = {"RARE": 0x55FFFF, "UNIQUE": 0xFF77FF, "LEGEND": 0xFFAA00}
RIM_PIXELS = {"RARE": 1, "UNIQUE": 2, "LEGEND": 3}

# ---------------------------------------------------------------- fill masks (16 x 16; the outline is added around them)
PLATE = [
    "................",
    "..mmmm....mmmm..",
    "..mmmmm..mmmmm..",
    "..mmmmmmmmmmmm..",
    "..mmmmmmmmmmmm..",
    "...mmmmmmmmmm...",
    "...mmmmmmmmmm...",
    "...mmmmmmmmmm...",
    "....mmmmmmmm....",
    "....mmmmmmmm....",
    "....mmmmmmmm....",
    "...mmmmmmmmmm...",
    "..mmmmmmmmmmmm..",
    "................",
    "................",
    "................",
]
LEATHER = [
    "................",
    "...mmm....mmm...",
    "..mmmmm..mmmmm..",
    "..mmmmmmmmmmmm..",
    "..mmmmmmmmmmmm..",
    "...mmmmmmmmmm...",
    "...mmmmmmmmmm...",
    "...mmmmmmmmmm...",
    "...mmmmmmmmmm...",
    "...mmmmmmmmmm...",
    "...mmmmmmmmmm...",
    "...mmmmmmmmmm...",
    "....mmmmmmmm....",
    "................",
    "................",
    "................",
]
COAT = [
    "................",
    "..mmmm....mmmm..",
    ".mmmmmm..mmmmmm.",
    ".mmmmmmmmmmmmmm.",
    ".mmmmmmmmmmmmmm.",
    ".mmm.mmmmmm.mmm.",
    ".mmm.mmmmmm.mmm.",
    ".mmm.mmmmmm.mmm.",
    ".mmm.mmmmmm.mmm.",
    ".mmm.mmmmmm.mmm.",
    ".mmm.mmmmmm.mmm.",
    "...mmmmmmmmmm...",
    ".mmmmmm..mmmmmm.",
    ".mmmmmm..mmmmmm.",
    "..mmmmm..mmmmm..",
    "................",
]
SUIT = [
    "................",
    "..mmmm....mmmm..",
    ".mmmmmm..mmmmmm.",
    ".mmmmmmmmmmmmmm.",
    ".mmmmmmmmmmmmmm.",
    ".mm.mmmmmmmm.mm.",
    ".mm.mmmmmmmm.mm.",
    ".mm.mmmmmmmm.mm.",
    ".mm.mmmmmmmm.mm.",
    ".mm.mmmmmmmm.mm.",
    ".mm.mmmmmmmm.mm.",
    ".mm.mmmmmmmm.mm.",
    ".mm.mmmmmmmm.mm.",
    ".mm..mmmmmm..mm.",
    ".mm..mmmmmm..mm.",
    "................",
]


def rgb(v):
    return ((v >> 16) & 255, (v >> 8) & 255, v & 255)


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def shade(c, f):
    return tuple(max(0, min(255, int(round(x * f)))) for x in c)


class Icon:
    """A 16x16 char grid: '.' empty, 'o' outline, 'm' main, 'l'/'d' main light/dark, 't'/'u' trim/dark trim, 'g'/'G' glow/dim glow,
    'x'/'y' the armour's accent and its dark, 'k' near black, 'w' pale metal."""

    def __init__(self, mask, main, trim, glow, accent=0x808080, accent_dark=None, grade="RARE"):
        assert len(mask) == 16 and all(len(r) == 16 for r in mask), "mask must be 16x16"
        self.g = [list(r) for r in mask]
        self.grade = grade
        m, t, gl, ax = rgb(main), rgb(trim), rgb(glow), rgb(accent)
        self.pal = {
            "m": m, "l": mix(m, (255, 255, 255), 0.32), "d": shade(m, 0.62),
            "o": mix(shade(m, 0.22), (14, 14, 22), 0.5),
            "t": t, "u": shade(t, 0.62),
            "g": mix(gl, (255, 255, 255), 0.3), "G": shade(gl, 0.72),
            "x": ax, "y": rgb(accent_dark) if accent_dark is not None else shade(ax, 0.6),
            "k": (22, 22, 30), "w": (214, 218, 226),
        }
        self.fill = {"m"}
        self._edges()

    # -- helpers
    def at(self, x, y):
        return self.g[y][x] if 0 <= x < 16 and 0 <= y < 16 else "."

    def put(self, x, y, ch):
        if 0 <= x < 16 and 0 <= y < 16 and self.g[y][x] != ".":
            self.g[y][x] = ch

    def force(self, x, y, ch):
        if 0 <= x < 16 and 0 <= y < 16:
            self.g[y][x] = ch

    def rect(self, x0, y0, x1, y1, ch):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.put(x, y, ch)

    def paint_fill(self, x0, y0, x1, y1, ch):
        """Fill even the empty cells (used for accents that extend the silhouette), then re-edge nothing."""
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.force(x, y, ch)

    def _solid(self, x, y):
        return self.at(x, y) not in (".", "o")

    def _edges(self):
        """Light top-left edges, dark bottom-right edges and a one pixel outline around everything."""
        src = [row[:] for row in self.g]
        for y in range(16):
            for x in range(16):
                if src[y][x] != "m":
                    continue
                solid = lambda a, b: 0 <= a < 16 and 0 <= b < 16 and src[b][a] != "."
                if not solid(x - 1, y) or not solid(x, y - 1):
                    self.g[y][x] = "l"
                elif not solid(x + 1, y) or not solid(x, y + 1):
                    self.g[y][x] = "d"
        for y in range(16):
            for x in range(16):
                if src[y][x] != ".":
                    continue
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    a, b = x + dx, y + dy
                    if 0 <= a < 16 and 0 <= b < 16 and src[b][a] != ".":
                        self.g[y][x] = "o"
                        break

    def rim(self):
        """The grade colour on the outline: the first outline pixel from the top left, the last from the bottom right, one in the middle."""
        n = RIM_PIXELS[self.grade]
        cells = [(x, y) for y in range(16) for x in range(16) if self.g[y][x] == "o"]
        picks = [cells[0], cells[-1], cells[len(cells) // 2]][:n]
        self.pal["r"] = rgb(GRADE_RGB[self.grade])
        for x, y in picks:
            self.g[y][x] = "r"

    def image(self):
        self.rim()
        im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for y in range(16):
            for x in range(16):
                ch = self.g[y][x]
                if ch != ".":
                    im.putpixel((x, y), self.pal[ch] + (255,))
        return im


# ---------------------------------------------------------------- the ten armours
def stone_sentinel():
    ic = Icon(PLATE, 0x9A9EA3, 0xC9C6B8, 0x2F8F94, accent=0x2D6670, accent_dark=0x173B44, grade="RARE")
    # the teal rocky growth over the right shoulder and chest
    ic.rect(10, 1, 13, 3, "x")
    for p in ((11, 1), (13, 2), (10, 3), (12, 3)):
        ic.put(p[0], p[1], "y")
    for p in ((9, 4), (10, 4), (9, 5), (8, 6), (8, 7), (7, 8), (6, 9)):
        ic.put(p[0], p[1], "x")
    for p in ((10, 4), (9, 5)):
        ic.put(p[0], p[1], "y")
    ic.put(8, 5, "g")
    ic.put(7, 7, "G")
    # cracks on the pale plate
    for p in ((4, 3), (4, 4), (5, 5), (4, 6), (5, 7), (3, 8)):
        ic.put(p[0], p[1], "d")
    # the bandage wraps at the waist, the flared hem plate
    for x in range(4, 12):
        ic.put(x, 9, "w")
        ic.put(x, 10, "u" if x % 2 else "w")
    ic.rect(3, 11, 12, 11, "l")
    ic.rect(2, 12, 13, 12, "d")
    return ic


def starstudded():
    ic = Icon(LEATHER, 0x1D2F7A, 0xC9D3EE, 0x8FB8FF, accent=0x7AD0D4, accent_dark=0x3A2A5A, grade="UNIQUE")
    # quilted diamonds
    for y in range(3, 12):
        for x in range(3, 13):
            if (x + y) % 4 == 0 or (x - y) % 4 == 0:
                ic.put(x, y, "d")
    # polished star studs at the diamond centres
    for y in range(3, 12):
        for x in range(3, 13):
            if x % 2 == 0 and (x + y) % 4 == 2 and (x - y) % 4 == 2:
                ic.put(x, y, "t")
    ic.put(6, 4, "g")
    ic.put(10, 8, "g")
    ic.put(4, 10, "G")
    # the dark purple belt and its buckle
    ic.rect(3, 10, 12, 10, "y")
    ic.put(7, 10, "t")
    ic.put(8, 10, "t")
    # the pale iridescent cloth at the side
    ic.paint_fill(12, 8, 14, 12, "x")
    ic.put(13, 9, "l")
    ic.put(13, 11, "g")
    ic.rect(4, 1, 5, 1, "u")
    ic.rect(10, 1, 11, 1, "u")
    return ic


def duelist():
    ic = Icon(COAT, 0x1A1A22, 0xB8BCC8, 0xC03048, accent=0x6E0F1E, accent_dark=0x4A0A14, grade="LEGEND")
    # the crimson inside of the open front
    for y in range(3, 12):
        ic.rect(6, y, 9, y, "x")
    ic.rect(7, 2, 8, 2, "x")
    for p in ((6, 5), (9, 5), (6, 8), (9, 8)):
        ic.put(p[0], p[1], "y")
    # silver piping of the lapels, turned-back cuffs, button bars
    for y in range(2, 11):
        ic.put(5, y, "t" if y % 2 == 0 else "m")
        ic.put(10, y, "t" if y % 2 == 0 else "m")
    ic.rect(1, 9, 3, 9, "t")
    ic.rect(12, 9, 14, 9, "t")
    ic.rect(1, 10, 3, 10, "u")
    ic.rect(12, 10, 14, 10, "u")
    ic.put(3, 3, "t")
    ic.put(12, 3, "t")
    # the long split tails with the lining showing
    for y in range(12, 15):
        ic.put(6, y, "x")
        ic.put(9, y, "x")
    ic.put(6, 12, "g")
    return ic


def golden_shadow():
    ic = Icon(SUIT, 0x2B2D1C, 0xD4A537, 0xFFA040, accent=0x15160D, grade="LEGEND")
    # gold trimmed pauldrons and the gold edge of the long cape
    ic.rect(2, 1, 5, 1, "t")
    ic.rect(10, 1, 13, 1, "t")
    for y in range(3, 15):
        ic.put(1, y, "t")
        ic.put(14, y, "t")
    for y in range(5, 13):
        ic.put(2, y, "x")
        ic.put(13, y, "x")
    # gold crackle through the scales
    for p in ((5, 4), (6, 6), (5, 7), (9, 4), (10, 6), (9, 7), (8, 8), (6, 11), (9, 12), (7, 13)):
        ic.put(p[0], p[1], "t")
    # glowing hex gems: chest, belt and shoulders
    for p in ((7, 5), (8, 5), (7, 6), (8, 6)):
        ic.put(p[0], p[1], "g")
    ic.put(7, 5, "G")
    ic.rect(4, 9, 11, 9, "u")
    ic.put(7, 9, "g")
    ic.put(8, 9, "g")
    ic.put(3, 2, "g")
    ic.put(12, 2, "g")
    return ic


def lionheart():
    mask = [r for r in PLATE]
    mask[0] = "......mmmm......"
    mask[1] = "..mmm......mmm.."
    mask[2] = "..mmmmmmmmmmmm.."
    mask[13] = ".....mmmmmm....."
    mask[14] = ".....mmmmmm....."
    ic = Icon(mask, 0xC4C8D0, 0xD9A441, 0xD9622A, accent=0xD9622A, accent_dark=0x8A2A12, grade="LEGEND")
    # the lion's mane around the neck
    ic.rect(6, 0, 9, 0, "x")
    ic.rect(6, 1, 9, 2, "x")
    for p in ((6, 1), (9, 2), (7, 0)):
        ic.put(p[0], p[1], "y")
    # gold lion panels on the chest
    ic.rect(4, 4, 6, 7, "t")
    ic.rect(9, 4, 11, 7, "t")
    for p in ((5, 5), (10, 5)):
        ic.put(p[0], p[1], "u")
    ic.put(5, 6, "g")
    ic.put(10, 6, "g")
    # brown belt and the red tabard
    ic.rect(4, 9, 11, 9, "k")
    ic.put(7, 9, "t")
    ic.put(8, 9, "t")
    ic.rect(5, 11, 10, 14, "y")
    ic.rect(6, 11, 9, 14, "x")
    ic.rect(4, 3, 4, 3, "l")
    ic.rect(3, 1, 5, 1, "t")
    ic.rect(10, 1, 12, 1, "t")
    return ic


def beheaded():
    mask = [r for r in PLATE]
    mask[1] = "..mmm......mmm.."
    mask[2] = "..mmmm....mmmm.."
    mask[3] = "mmmmmm....mmmmmm"
    mask[4] = "..mmmmmmmmmmmm.."
    mask[5] = "m..mmmmmmmmmm..m"
    ic = Icon(mask, 0x8D949C, 0x2B2B30, 0x9FD8FF, accent=0x1B1B22, grade="UNIQUE")
    # the empty neck: dark hollow with a cold glow
    ic.paint_fill(6, 1, 9, 3, "k")
    ic.put(7, 2, "G")
    ic.put(8, 3, "g")
    ic.put(6, 1, "G")
    # spiked pauldrons and gauntlets
    for p in ((0, 3), (15, 3), (0, 5), (15, 5)):
        ic.put(p[0], p[1], "l")
    # dark underlayer between the plates
    ic.rect(4, 8, 11, 8, "u")
    ic.rect(4, 10, 11, 10, "u")
    for p in ((6, 5), (9, 5), (7, 6), (8, 6)):
        ic.put(p[0], p[1], "l")
    ic.put(4, 11, "k")
    ic.put(11, 11, "k")
    return ic


def false_dragon():
    mask = [r for r in PLATE]
    mask[13] = ".....mmmmmm....."
    mask[14] = ".....mmmmmm....."
    ic = Icon(mask, 0x8A1A1A, 0x9B9A3A, 0xFFD23A, accent=0x1B7A7A, accent_dark=0x0E4A4A, grade="UNIQUE")
    # gold-olive pauldrons with filigree
    ic.rect(2, 1, 5, 3, "t")
    ic.rect(10, 1, 13, 3, "t")
    for p in ((3, 2), (12, 2), (4, 3), (11, 3)):
        ic.put(p[0], p[1], "u")
    # silver chest plates
    ic.rect(5, 5, 10, 8, "w")
    for x in range(5, 11):
        ic.put(x, 6, "l" if x % 2 else "w")
    for p in ((5, 8), (10, 8)):
        ic.put(p[0], p[1], "u")
    # ruby with the narrow yellow iris
    ic.rect(7, 3, 8, 4, "g")
    ic.put(8, 3, "t")
    ic.put(8, 4, "t")
    ic.put(7, 3, "G")
    # belt gem and the teal and yellow tabard
    ic.rect(4, 9, 11, 9, "u")
    ic.put(7, 9, "g")
    ic.put(8, 9, "G")
    ic.rect(6, 11, 9, 14, "x")
    ic.rect(7, 11, 8, 14, "t")
    return ic


def soul_armor():
    mask = [r for r in PLATE]
    mask[13] = "....mmmmmmmm...."
    mask[14] = "....mmmmmmmm...."
    ic = Icon(mask, 0xC8D0DA, 0x2A2C33, 0x3FB4FF, accent=0x1E2026, accent_dark=0x121318, grade="RARE")
    # the headless neck
    ic.paint_fill(6, 1, 9, 2, "k")
    ic.put(7, 2, "G")
    # black cloth tassets
    ic.rect(4, 10, 11, 14, "x")
    for x in range(4, 12, 2):
        ic.rect(x, 11, x, 14, "y")
    ic.rect(4, 10, 11, 10, "u")
    # glowing blue orbs: chest, shoulders, belt, and cracks between them
    ic.rect(7, 4, 8, 5, "g")
    ic.put(7, 4, "G")
    ic.put(3, 2, "g")
    ic.put(12, 2, "g")
    ic.put(7, 9, "g")
    for p in ((6, 5), (5, 6), (9, 6), (10, 7), (6, 7), (7, 7), (8, 8)):
        ic.put(p[0], p[1], "G")
    ic.rect(2, 1, 5, 1, "l")
    ic.rect(10, 1, 13, 1, "l")
    return ic


def arcane_conduction():
    ic = Icon(LEATHER, 0x1B1D26, 0x5A7FC0, 0x58A8FF, accent=0x2A4A8E, accent_dark=0x182C58, grade="RARE")
    # blue-steel pauldrons
    ic.rect(3, 1, 5, 1, "t")
    ic.rect(10, 1, 12, 1, "t")
    ic.rect(2, 2, 3, 3, "t")
    ic.rect(12, 2, 13, 3, "t")
    # the sash from the left shoulder to the right hip
    for i in range(0, 8):
        ic.put(4 + i, 2 + i, "x")
        ic.put(5 + i, 2 + i, "x")
    for i in range(0, 8, 2):
        ic.put(5 + i, 3 + i, "g")
    # segmented plates
    for y in (5, 7, 9):
        for x in range(4, 12):
            if ic.at(x, y) in ("m", "l", "d"):
                ic.put(x, y, "d")
    # the long dark-blue cape / tabard
    ic.paint_fill(4, 13, 11, 15, "y")
    ic.paint_fill(5, 13, 10, 15, "x")
    for y in (13, 15):
        ic.put(7, y, "g")
    ic.rect(3, 11, 12, 11, "t")
    ic.put(7, 11, "g")
    return ic


def delphyne():
    mask = [r for r in PLATE]
    mask[0] = "..m........m...."
    mask[13] = "....mmmmmmmm...."
    mask[14] = "....mmmmmmmm...."
    ic = Icon(mask, 0x2C3036, 0x6A7078, 0x8FB4C8, accent=0xA8BCC8, accent_dark=0x6E8294, grade="LEGEND")
    # dragon scales: staggered dark rows with light edges
    for y in range(4, 13):
        for x in range(3, 13):
            if ic.at(x, y) in ("m", "l", "d"):
                if (x + y) % 3 == 0:
                    ic.put(x, y, "d")
                elif (x + 2 * y) % 5 == 0:
                    ic.put(x, y, "l")
    # horn, spiked pauldrons
    for p in ((2, 1), (13, 1), (3, 0), (4, 1)):
        ic.put(p[0], p[1], "t")
    ic.rect(2, 1, 5, 1, "u")
    ic.rect(10, 1, 13, 1, "u")
    # the draconic emblem with pale eyes
    ic.put(6, 6, "g")
    ic.put(9, 6, "g")
    ic.rect(7, 7, 8, 7, "u")
    ic.rect(4, 9, 11, 9, "u")
    # the long pale blue-grey tail curling away at the right
    tail = [(12, 12), (13, 12), (14, 12), (14, 11), (14, 10), (14, 9), (13, 8)]
    for x, y in tail:
        ic.force(x, y, "x")
    for x, y in ((15, 12), (15, 11), (15, 10), (15, 9), (14, 8), (13, 7), (12, 13), (13, 13), (14, 13)):
        if ic.at(x, y) in (".", "o"):
            ic.force(x, y, "o")
    ic.force(13, 7, "y")
    return ic


ICONS = {
    "armour_of_the_stone_sentinel": stone_sentinel,
    "starstudded_leathers": starstudded,
    "coat_of_the_duelist": duelist,
    "suit_of_the_golden_shadow": golden_shadow,
    "armor_of_the_lionheart": lionheart,
    "armor_of_the_beheaded": beheaded,
    "false_dragon_armor": false_dragon,
    "soul_armor": soul_armor,
    "arcane_conduction_armor": arcane_conduction,
    "delphynes_cursed_armor": delphyne,
}


def main():
    tex = os.path.join(ASSETS, "textures", "item")
    mod = os.path.join(ASSETS, "models", "item")
    os.makedirs(tex, exist_ok=True)
    os.makedirs(mod, exist_ok=True)
    for rid, fn in ICONS.items():
        fn().image().save(os.path.join(tex, rid + ".png"))
        with open(os.path.join(mod, rid + ".json"), "w", newline="\n") as f:
            json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/" + rid}}, f, indent=2)
            f.write("\n")
        print("wrote", rid)


if __name__ == "__main__":
    main()
