#!/usr/bin/env python3
"""
Relics of Many Worlds, wave 4: the 16x16 inventory sprites and flat item models of the eight neck relics (amulets and pendants).

    python3 tools/relics/wave4_icons.py [--preview out.png]

Writes under src/main/resources/assets/nusmp/
  textures/item/<id>.png      the sprite: a dark outline, three or four shades per material lit from the upper left, a few glow pixels
                              in the relic's glow colour, and one outline pixel in the grade's colour (the grade rim)
  models/item/<id>.json       the flat item model (parent item/generated, layer0 = the sprite): neck relics are worn, not held, so
                              they have no 3D hand mesh
for amulet_of_the_coven, pendant_of_echoed_souls, amulet_of_the_forest, arcane_glass_of_seeing, voice_of_the_mountain,
amulet_of_the_sea_of_stars, pendant_of_the_dreamer, beast_masters_amulet.

Each sprite follows the card's art: the shapes and colours named in the art notes are measured off the card (eye with ruby iris in a nest
of thorns on a braided cord; a bone-inlaid black medallion on an iron chain; six animal charms on a knotted cord; an oval gold mirror;
a white horn with emerald shards; a shield-cut amethyst on a silver chain; a bronze round pendant with blue gems; a grey disc with a
square hole and animal silhouettes). Idempotent: running it again rewrites the same files.
"""
import json
import math
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "nusmp")
ICONS = os.path.join(ASSETS, "textures", "item")
MODELS = os.path.join(ASSETS, "models", "item")

OUTLINE = (22, 18, 30)
GRADE_RGB = {"NORMAL": 0xAAAAAA, "SPECIAL": 0x55FF55, "RARE": 0x55FFFF, "UNIQUE": 0xFF77FF, "LEGEND": 0xFFAA00, "GOD": 0xFF5555, "GENESIS": 0xFFFFFF}


def rgb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def ramp(base, n=4):
    """n shades of a colour from light (index 0) to dark (n-1)."""
    out = []
    for i in range(n):
        t = i / (n - 1)
        if t < 0.35:
            out.append(mix((255, 255, 255), base, 0.55 + t / 0.35 * 0.45) if t < 0.35 else base)
        else:
            out.append(mix(base, (14, 10, 22), (t - 0.35) / 0.65 * 0.62))
    out[0] = mix(base, (255, 255, 255), 0.45)
    out[1] = base
    return out


class Canvas:
    def __init__(self):
        self.px = {}

    def put(self, x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            self.px[(x, y)] = c

    def get(self, x, y):
        return self.px.get((x, y))

    def line(self, x0, y0, x1, y1, c):
        dx, dy = abs(x1 - x0), -abs(y1 - y0)
        sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
        err = dx + dy
        while True:
            self.put(x0, y0, c)
            if x0 == x1 and y0 == y1:
                break
            e2 = 2 * err
            if e2 >= dy:
                err += dy
                x0 += sx
            if e2 <= dx:
                err += dx
                y0 += sy

    def shaded(self, cells_fn, cx, cy, shades, edge_dark=True):
        """Fill every pixel for which cells_fn(x, y) is truthy with a shade picked by the lighting at that pixel (light from the upper left)."""
        for y in range(16):
            for x in range(16):
                if not cells_fn(x + 0.5, y + 0.5):
                    continue
                dx, dy = x + 0.5 - cx, y + 0.5 - cy
                d = math.hypot(dx, dy) + 1e-6
                lit = (-dx - dy) / (d * 1.4142)
                if lit > 0.55:
                    c = shades[0]
                elif lit > -0.1:
                    c = shades[1]
                elif lit > -0.6:
                    c = shades[2]
                else:
                    c = shades[3]
                self.put(x, y, c)

    def outline(self, rgbc=OUTLINE):
        cells = set(self.px)
        for (x, y) in cells:
            for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                q = (x + dx, y + dy)
                if q not in cells and 0 <= q[0] < 16 and 0 <= q[1] < 16:
                    self.px[q] = rgbc

    def sparkles(self, glow, spots):
        for (x, y) in spots:
            if (x, y) not in self.px and all((x + dx, y + dy) not in self.px for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                self.px[(x, y)] = glow

    def grade_rim(self, grade_hex):
        """The outline pixel furthest to the lower right takes the grade's colour."""
        best, at = -1, None
        for (x, y), c in self.px.items():
            if c == OUTLINE and x + y > best:
                best, at = x + y, (x, y)
        if at:
            self.px[at] = rgb(grade_hex)

    def save(self, path):
        im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for (x, y), c in self.px.items():
            im.putpixel((x, y), (*c, 255))
        im.save(path)
        return im


def disc(r, cx, cy):
    return lambda x, y: (x - cx) ** 2 + (y - cy) ** 2 <= r * r


def ellipse(rx, ry, cx, cy):
    return lambda x, y: ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1.0


def v_chain(cv, left_top, right_top, meet_l, meet_r, c1, c2):
    """A cord or chain hanging in a V from two top points to the bail, alternating two tones for links or braid."""
    for i, (a, b) in enumerate(((left_top, meet_l), (right_top, meet_r))):
        steps = max(abs(b[0] - a[0]), abs(b[1] - a[1]))
        for k in range(steps + 1):
            t = k / steps if steps else 0
            x = int(round(a[0] + (b[0] - a[0]) * t))
            y = int(round(a[1] + (b[1] - a[1]) * t))
            cv.put(x, y, c1 if (k + i) % 2 == 0 else c2)


# ------------------------------------------------------------------------------------------------ the eight sprites
def amulet_of_the_coven(glow):
    cv = Canvas()
    tan, tan_d = (226, 212, 170), (150, 128, 84)
    leaf = ramp((70, 132, 52), 4)
    # the braided cord of white hair and green vine: two strands from the top corners down to the nest
    v_chain(cv, (2, 0), (13, 0), (4, 5), (11, 5), tan, tan_d)
    # the nest of thorns: a ragged ring round the eye, with little thorn spikes pointing out
    cv.shaded(ellipse(6.6, 5.4, 8, 10), 8, 10, leaf)
    for (x, y) in ((1, 9), (2, 7), (4, 4), (6, 4), (10, 4), (12, 5), (14, 8), (14, 11), (13, 14), (10, 15), (5, 15), (2, 13), (1, 11)):
        cv.put(x, y, leaf[1] if (x + y) % 2 else leaf[2])
    # the eye: a bright yellow-white sclera, the ruby iris, a black pupil and a glint
    sclera = [(255, 252, 214), (240, 232, 150), (206, 192, 104), (150, 132, 66)]
    cv.shaded(ellipse(5.0, 3.9, 8, 10), 8, 10, sclera)
    iris = ramp((198, 30, 44), 4)
    cv.shaded(disc(2.55, 8.6, 10), 8.6, 10, iris)
    for (x, y) in ((8, 10), (9, 10), (8, 11), (9, 11)):
        cv.put(x, y, (16, 6, 12))
    cv.put(8, 9, (255, 226, 226))
    # vines wrapped across the lids
    for (x, y) in ((4, 8), (5, 7), (5, 12), (6, 13), (11, 8), (11, 12), (10, 13)):
        cv.put(x, y, leaf[2])
    cv.outline()
    cv.sparkles(glow, [(14, 2), (0, 4), (15, 15)])
    return cv


def pendant_of_echoed_souls(glow):
    cv = Canvas()
    iron, iron_d = (118, 120, 132), (62, 64, 74)
    # a dark iron chain from the two top corners to the bail (a link every other pixel)
    v_chain(cv, (1, 0), (14, 0), (7, 5), (8, 5), iron, iron_d)
    for (x, y, c) in ((7, 4, iron), (8, 4, iron_d), (7, 5, iron_d), (8, 5, iron)):
        cv.put(x, y, c)
    cv.put(7, 6, iron); cv.put(8, 6, iron_d)
    # the medallion: a blackened silver rim round a dark, speckled disc
    rim = ramp((150, 152, 166), 4)
    cv.shaded(disc(5.5, 8, 11), 8, 11, rim)
    for y in range(16):
        for x in range(16):
            if (x + 0.5 - 8) ** 2 + (y + 0.5 - 11) ** 2 <= 4.2 ** 2:
                cv.put(x, y, (24, 22, 30) if (x * 3 + y * 5) % 7 else (46, 42, 56))
    # the bone fragments, pale with a shadowed underside: a long one slanting up, a short one standing, one lying, a splinter below
    bone, bone_d = (240, 232, 204), (176, 164, 132)
    for (x, y) in ((6, 12), (7, 11), (8, 10), (9, 9)):
        cv.put(x, y, bone)
    for (x, y) in ((7, 12), (8, 11), (9, 10)):
        cv.put(x, y, bone_d)
    for (x, y) in ((6, 9), (6, 10)):
        cv.put(x, y, bone)
    cv.put(7, 10, bone_d)
    for (x, y) in ((10, 11), (11, 11), (11, 12)):
        cv.put(x, y, bone)
    cv.put(10, 12, bone_d)
    cv.put(8, 13, bone)
    for (x, y) in ((7, 14), (8, 14), (9, 14)):
        cv.put(x, y, bone_d)
    cv.outline()
    cv.sparkles(glow, [(13, 3), (2, 4), (14, 14)])
    return cv


CHARMS = {
    # three rows of three pixels per animal charm; '.' is clear. Colours: light, mid, dark, accent
    "bear": (["D.D", "BLB", ".D."], {"D": (84, 52, 30), "B": (138, 92, 54), "L": (172, 126, 82)}),
    "cat": (["k.k", "kyk", ".k."], {"k": (78, 72, 98), "y": (240, 214, 64)}),
    "beetle": ([".R.", "rHr", ".R."], {"R": (178, 82, 40), "r": (122, 52, 26), "H": (226, 140, 80)}),
    "raccoon": (["gGg", "dwd", ".G."], {"g": (150, 152, 164), "G": (112, 114, 128), "d": (40, 38, 48), "w": (226, 226, 234)}),
    "owl": (["pPp", "wpw", ".p."], {"p": (150, 104, 204), "P": (110, 72, 160), "w": (246, 238, 160)}),
    "peacock": (["gTg", "TGT", ".G."], {"g": (60, 190, 142), "T": (36, 140, 160), "G": (24, 100, 110)}),
}


def charm(cv, name, x, y):
    rows, legend = CHARMS[name]
    for dy, row in enumerate(rows):
        for dx, ch in enumerate(row):
            if ch != ".":
                cv.put(x + dx, y + dy, legend[ch])


def amulet_of_the_forest(glow):
    cv = Canvas()
    cord, cord_d = (184, 166, 124), (118, 100, 68)
    # the cord, tied in a bow at the top, runs down to the six figurines (two rows of three)
    for (a, b) in (((8, 3), (2, 5)), ((8, 3), (7, 5)), ((8, 3), (12, 5)), ((2, 8), (2, 10)), ((7, 8), (7, 10)), ((12, 8), (12, 10))):
        cv.line(a[0], a[1], b[0], b[1], cord)
    for (x, y) in ((2, 9), (7, 9), (12, 9), (5, 4), (10, 4)):
        cv.put(x, y, cord_d)
    for (x, y, c) in ((5, 0, cord), (6, 0, cord_d), (6, 1, cord), (7, 1, cord_d), (8, 2, cord_d), (9, 1, cord_d), (10, 1, cord), (10, 0, cord_d), (11, 0, cord), (7, 2, cord), (9, 2, cord), (8, 1, cord)):
        cv.put(x, y, c)
    # the six animals, 3x3 each: bear, cat, beetle above; raccoon, owl, peacock below
    charm(cv, "bear", 1, 5)
    charm(cv, "cat", 6, 5)
    charm(cv, "beetle", 11, 5)
    charm(cv, "raccoon", 1, 10)
    charm(cv, "owl", 6, 10)
    charm(cv, "peacock", 11, 10)
    cv.outline()
    cv.sparkles(glow, [(14, 1), (3, 1), (4, 15), (9, 15)])
    return cv


def arcane_glass_of_seeing(glow):
    cv = Canvas()
    cord = (150, 128, 96)
    cv.line(3, 0, 8, 3, cord); cv.line(12, 0, 8, 3, cord)
    gold = ramp((214, 168, 58), 4)
    # the frame: an oval ring with scrollwork nubs
    cv.shaded(ellipse(4.9, 6.0, 8, 9), 8, 9, gold)
    # the glass: smoky silver-grey with a lit upper left
    mirror = [(188, 192, 204), (136, 140, 156), (96, 100, 118), (66, 70, 88)]
    cv.shaded(ellipse(3.4, 4.6, 8, 9), 8, 9, mirror)
    # faint concentric arcs near the bottom of the glass
    for (x, y) in ((6, 12), (7, 12), (8, 12), (9, 12)):
        cv.put(x, y, (170, 176, 196))
    for (x, y) in ((6, 11), (9, 11)):
        cv.put(x, y, (150, 156, 176))
    cv.put(6, 6, (232, 236, 246)); cv.put(7, 6, (214, 218, 232)); cv.put(6, 7, (214, 218, 232))
    # scrollwork flourishes: top crest and bottom drop
    for (x, y, c) in ((8, 2, gold[1]), (7, 3, gold[0]), (9, 3, gold[2]), (8, 14, gold[2]), (8, 15, gold[3])):
        cv.put(x, y, c)
    cv.put(3, 5, gold[0]); cv.put(13, 5, gold[2]); cv.put(3, 12, gold[2]); cv.put(13, 12, gold[3])
    cv.outline()
    cv.sparkles(glow, [(1, 2), (15, 8), (1, 14)])
    return cv


def voice_of_the_mountain(glow):
    cv = Canvas()
    gold_c, gold_d = (232, 204, 72), (170, 142, 42)
    # the horn: stamp discs along an arc (centre (12, 2), radius 9), from a fine tip at the upper left to a thick mouth at the lower right
    horn = ramp((244, 240, 228), 4)
    stamps = []
    for k in range(0, 15):
        th = math.radians(180 - k * 6.4)
        r = 0.55 + 2.55 * (k / 14.0) ** 1.15
        stamps.append((12 + 9.0 * math.cos(th), 2 + 9.0 * math.sin(th), r))
    cv.shaded(lambda x, y: any((x - sx) ** 2 + (y - sy) ** 2 <= r * r for sx, sy, r in stamps), 7.5, 7.0, horn)
    # growth rings across the horn, darker bands
    for (x, y) in ((4, 5), (5, 5), (6, 7), (7, 7), (8, 8), (9, 9)):
        if (x, y) in cv.px:
            cv.put(x, y, horn[2])
    # the silver cap on the mouth, a tall ellipse with a rim
    cap = ramp((186, 190, 204), 4)
    cv.shaded(ellipse(1.7, 3.0, 12.6, 10.6), 12.6, 10.6, cap)
    cv.put(13, 9, (96, 100, 114)); cv.put(13, 10, (96, 100, 114)); cv.put(13, 11, (96, 100, 114))
    # emerald crystal shards wrapped round the lower half of the horn
    em = ramp((46, 176, 108), 4)
    for (x, y, i) in ((7, 10, 1), (8, 11, 2), (9, 11, 1), (9, 12, 3), (10, 12, 2), (10, 8, 0), (11, 8, 1), (11, 9, 2), (8, 9, 2), (10, 10, 1), (9, 10, 0), (11, 11, 3)):
        cv.put(x, y, em[i])
    cv.put(10, 8, (176, 255, 214)); cv.put(8, 11, (176, 255, 214))
    # the cord: a gold-and-black braid looping from the tip
    for (x, y) in ((3, 1), (3, 0), (4, 0), (5, 0), (6, 0), (7, 1), (8, 2), (9, 3)):
        cv.put(x, y, gold_c if (x + y) % 2 else gold_d)
    cv.outline()
    cv.sparkles(glow, [(14, 4), (0, 6), (4, 14), (15, 14)])
    return cv


def amulet_of_the_sea_of_stars(glow):
    cv = Canvas()
    silver, silver_d = (214, 218, 228), (130, 136, 152)
    v_chain(cv, (2, 0), (13, 0), (5, 7), (10, 7), silver, silver_d)
    # the setting: silver tendrils that curl round the gem, with a pearl at each end
    for (x, y, c) in ((4, 8, silver), (5, 9, silver_d), (4, 9, silver), (11, 8, silver), (10, 9, silver_d), (11, 9, silver)):
        cv.put(x, y, c)
    pearl = (246, 240, 236)
    cv.put(4, 7, pearl); cv.put(11, 7, pearl)
    # the gem: a shield-cut amethyst, facets lit from the upper left
    am = {"L": (214, 166, 255), "M": (160, 90, 232), "D": (104, 52, 176), "S": (62, 28, 110), "W": (250, 236, 255)}
    rows = {
        8: "..LLLLLL..",
        9: ".LWLMMMMD.",
        10: ".LLLMMMDD.",
        11: ".LLMMMMDD.",
        12: "..LMMMDDS.",
        13: "...MMDDS..",
        14: "....DDS...",
    }
    for y, row in rows.items():
        for i, ch in enumerate(row):
            if ch != ".":
                cv.put(3 + i, y, am[ch])
    cv.put(8, 15, silver_d)
    cv.outline()
    cv.sparkles(glow, [(1, 5), (14, 5), (14, 13), (1, 13)])
    return cv


def pendant_of_the_dreamer(glow):
    cv = Canvas()
    cord, cord_d = (62, 48, 40), (30, 24, 22)
    v_chain(cv, (1, 0), (14, 0), (5, 3), (10, 3), cord, cord_d)
    bronze = ramp((158, 110, 58), 4)
    # the ornate round pendant: a bronze disc with a dark inner ring and a beaded edge
    cv.shaded(disc(5.3, 8, 7), 8, 7, bronze)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 7)
            if 3.2 <= d <= 4.1:
                cv.put(x, y, (58, 40, 26) if (x + y) % 2 else (78, 54, 32))
    for (x, y) in ((8, 1), (3, 4), (13, 4), (2, 7), (14, 7), (3, 10), (13, 10), (5, 12), (11, 12)):
        cv.put(x, y, bronze[0] if (x + y) % 2 else bronze[1])
    # the great blue gem in the centre, a glowing heart
    blue = ramp((52, 150, 255), 4)
    cv.shaded(disc(2.9, 8, 7), 8, 7, blue)
    cv.put(7, 6, (236, 248, 255)); cv.put(8, 6, (196, 228, 255)); cv.put(7, 7, (196, 228, 255))
    cv.put(9, 8, (30, 90, 190))
    # the teardrops hanging below: two small, one great
    drop = (70, 160, 255)
    for (x, y) in ((4, 12), (4, 13), (12, 12), (12, 13)):
        cv.put(x, y, drop)
    cv.put(4, 14, (40, 100, 200)); cv.put(12, 14, (40, 100, 200))
    cv.put(8, 12, bronze[2]); cv.put(8, 13, bronze[3])
    for (x, y, c) in ((8, 14, blue[0]), (7, 15, blue[1]), (8, 15, blue[0]), (9, 15, blue[2])):
        cv.put(x, y, c)
    cv.outline()
    cv.sparkles(glow, [(0, 5), (15, 5), (1, 11), (15, 11)])
    return cv


def beast_masters_amulet(glow):
    cv = Canvas()
    grey = ramp((124, 128, 140), 4)
    # the loop on top, a small square eye
    for (x, y, c) in ((6, 1, grey[1]), (7, 1, grey[0]), (8, 1, grey[0]), (9, 1, grey[2]), (6, 2, grey[2]), (9, 2, grey[3])):
        cv.put(x, y, c)
    # the disc, glossy, lit from the upper left
    cv.shaded(disc(6.4, 8, 9), 8, 9, grey)
    # the animal silhouettes pressed round the hole (darker than the metal): otter, owl, seahorse, kitten, wolf
    dark = (60, 64, 76)
    for (x, y) in ((5, 4), (6, 4), (7, 4), (4, 5), (5, 5), (6, 5), (7, 6), (4, 6)):          # otter, top
        cv.put(x, y, dark)
    for (x, y) in ((10, 4), (11, 4), (10, 5), (11, 5), (12, 5), (11, 6), (12, 6), (11, 7)):  # owl, upper right
        cv.put(x, y, dark)
    for (x, y) in ((3, 8), (4, 8), (3, 9), (3, 10), (4, 11), (3, 12), (4, 12)):              # seahorse, left
        cv.put(x, y, dark)
    for (x, y) in ((5, 12), (6, 12), (6, 11), (5, 13), (6, 13), (7, 13)):                    # kitten, bottom
        cv.put(x, y, dark)
    for (x, y) in ((11, 9), (11, 10), (10, 11), (11, 11), (12, 11), (10, 12), (12, 12), (9, 13)):  # wolf, lower right
        cv.put(x, y, dark)
    # the square hole in the middle: a frame of dark with clear air inside
    frame = (30, 28, 38)
    for x in range(6, 10):
        for y in range(7, 11):
            cv.px.pop((x, y), None)
    for x in range(6, 10):
        for y in range(7, 11):
            if x in (6, 9) or y in (7, 10):
                cv.put(x, y, frame)
    # the sheen: a pale diagonal streak from the top right
    for (x, y) in ((9, 3), (10, 3), (10, 4), (11, 5)):
        if (x, y) in cv.px and cv.px[(x, y)] != dark:
            cv.put(x, y, grey[0])
    cv.outline()
    for x in (7, 8):                                                  # the clear air inside the hole's frame
        for y in (8, 9):
            cv.px.pop((x, y), None)
    cv.sparkles(glow, [(14, 2), (1, 4), (14, 15), (0, 13)])
    return cv


SPRITES = [
    ("amulet_of_the_coven", amulet_of_the_coven, "LEGEND", 0xC1272D),
    ("pendant_of_echoed_souls", pendant_of_echoed_souls, "UNIQUE", 0xD9D2BC),
    ("amulet_of_the_forest", amulet_of_the_forest, "LEGEND", 0x7CE0A0),
    ("arcane_glass_of_seeing", arcane_glass_of_seeing, "RARE", 0xC8D2E6),
    ("voice_of_the_mountain", voice_of_the_mountain, "SPECIAL", 0x2EC27E),
    ("amulet_of_the_sea_of_stars", amulet_of_the_sea_of_stars, "UNIQUE", 0xA855F7),
    ("pendant_of_the_dreamer", pendant_of_the_dreamer, "RARE", 0x3FA9FF),
    ("beast_masters_amulet", beast_masters_amulet, "RARE", 0xB8C0CC),
]


def model_json(rid):
    return {"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/" + rid}}


def main():
    os.makedirs(ICONS, exist_ok=True)
    os.makedirs(MODELS, exist_ok=True)
    ims = []
    for rid, fn, grade, glow in SPRITES:
        cv = fn(rgb(glow))
        cv.grade_rim(GRADE_RGB[grade])
        ims.append(cv.save(os.path.join(ICONS, rid + ".png")))
        with open(os.path.join(MODELS, rid + ".json"), "w") as f:
            json.dump(model_json(rid), f, indent=2)
            f.write("\n")
    if "--preview" in sys.argv:
        out = sys.argv[sys.argv.index("--preview") + 1]
        S = 14
        sheet = Image.new("RGBA", (len(ims) * (16 * S + 10) + 10, 16 * S + 20), (58, 58, 70, 255))
        for i, im in enumerate(ims):
            big = im.resize((16 * S, 16 * S), Image.NEAREST)
            sheet.paste(big, (10 + i * (16 * S + 10), 10), big)
        sheet.save(out)
    print("wrote", len(SPRITES), "sprites and models")


if __name__ == "__main__":
    main()
