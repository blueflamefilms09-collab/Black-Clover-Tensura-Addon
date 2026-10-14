#!/usr/bin/env python3
"""Relics of Many Worlds, second card set, wave 17: the 16x16 inventory icons of the notebooks, pouches and charms.

Writes textures/item/<id>.png and models/item/<id>.json (parent item/generated) under src/main/resources/assets/nusmp
for every id of wave 17. Idempotent. Each icon is drawn from primitives (rect, disc, line, ellipse, ascii rows) in its own
function, shaded with a light top-left and a dark bottom-right edge, wrapped in a one pixel dark outline; one rim pixel per
grade star above Normal (rare 1, unique 2, legend 3) is in the grade colour and glow pixels sit on the gems and sparks.
The palettes are the ones of the relic defs (Wave17.java) and the card art.

Run from the repository root:  python tools/relics/wave17_icons.py
(add --sheet <path> to also write an 8x contact sheet for looking at them).
"""
import json
import math
import os
import sys

from PIL import Image

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")

GRADE_RGB = {"NORMAL": 0xAAAAAA, "RARE": 0x55FFFF, "UNIQUE": 0xFF77FF, "LEGEND": 0xFFAA00}
RIM_PIXELS = {"NORMAL": 0, "RARE": 1, "UNIQUE": 2, "LEGEND": 3}


def rgb(v):
    return ((v >> 16) & 255, (v >> 8) & 255, v & 255)


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def lt(c, t=0.34):
    return mix(c, (255, 255, 255), t)


def dk(c, f=0.6):
    return tuple(max(0, min(255, int(round(x * f)))) for x in c)


NEAR_BLACK = (24, 20, 30)
CREAM = (238, 226, 190)


class Canvas:
    """A 16x16 pixel canvas. Colours are (r, g, b) tuples. Edges are shaded and the outline added by finish()."""

    def __init__(self, grade):
        self.px = {}
        self.grade = grade
        self.shadeable = set()

    # -- primitives
    def p(self, x, y, c, shade=False):
        if 0 <= x < 16 and 0 <= y < 16:
            self.px[(x, y)] = c
            if shade:
                self.shadeable.add((x, y))
            else:
                self.shadeable.discard((x, y))

    def rect(self, x0, y0, x1, y1, c, shade=False):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.p(x, y, c, shade)

    def disc(self, cx, cy, r, c, shade=False):
        for y in range(16):
            for x in range(16):
                if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                    self.p(x, y, c, shade)

    def ellipse(self, cx, cy, rx, ry, c, shade=False):
        for y in range(16):
            for x in range(16):
                if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1.0:
                    self.p(x, y, c, shade)

    def line(self, x0, y0, x1, y1, c, shade=False):
        n = max(abs(x1 - x0), abs(y1 - y0), 1)
        for i in range(n + 1):
            self.p(round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n), c, shade)

    def rows(self, x0, y0, art, pal, shade=()):
        """Ascii art: '.' is empty, any other char looks up pal; chars in `shade` get the edge shading."""
        for dy, row in enumerate(art):
            for dx, ch in enumerate(row):
                if ch != "." and ch in pal:
                    self.p(x0 + dx, y0 + dy, pal[ch], ch in shade)

    def erase(self, x, y):
        self.px.pop((x, y), None)
        self.shadeable.discard((x, y))

    # -- finishing
    def finish(self):
        src = dict(self.px)

        def solid(x, y):
            return (x, y) in src

        for (x, y) in list(self.shadeable):
            c = src[(x, y)]
            if not solid(x - 1, y) or not solid(x, y - 1):
                self.px[(x, y)] = lt(c, 0.30)
            elif not solid(x + 1, y) or not solid(x, y + 1):
                self.px[(x, y)] = dk(c, 0.68)
        outline = []
        for y in range(16):
            for x in range(16):
                if (x, y) in src:
                    continue
                if any(solid(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    outline.append((x, y))
        for x, y in outline:
            self.px[(x, y)] = (22, 18, 30)
        n = RIM_PIXELS.get(self.grade, 0)
        if n and outline:
            picks = [outline[0], outline[-1], outline[len(outline) // 2]][:n]
            for x, y in picks:
                self.px[(x, y)] = rgb(GRADE_RGB[self.grade])
        return self

    def image(self):
        im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for (x, y), c in self.px.items():
            im.putpixel((x, y), tuple(c) + (255,))
        return im


# ================================================================ the icons
def beads():
    main, trim, glow = rgb(0x6A2A5E), rgb(0x241A28), rgb(0xC06BFF)
    c = Canvas("RARE")
    cord = (120, 104, 130)
    main = rgb(0x9A3584)
    pts = []
    for x in range(1, 15):
        u = (x - 7.5) / 6.5
        y = 2 + round(8 * (1 - u * u))
        pts.append((x, y))
    for x, y in pts:
        c.p(x, y, cord)
    c.p(1, 1, cord)
    c.p(14, 1, cord)
    c.p(7, 11, cord)
    c.p(8, 11, cord)                                    # the knot under the lowest bead
    for bx in (2, 4, 6, 8, 10, 12):
        by = [y for x, y in pts if x == bx][0]
        c.rect(bx, by, bx + 1, by + 1, main, True)
        c.p(bx, by, lt(main, 0.55))
        c.p(bx + 1, by + 1, dk(main, 0.6))
        if bx in (4, 10):
            c.p(bx + 1, by, glow)                      # a star in the bead
    c.rect(1, 13, 2, 14, main, True)                    # the loose beads beside the chain
    c.p(1, 13, lt(main, 0.5))
    c.p(2, 14, glow)
    c.rect(13, 12, 14, 13, main, True)
    c.p(13, 12, lt(main, 0.5))
    return c.finish()


def notebook(main, trim, glow, emblem, grade="NORMAL"):
    main, trim, glow = rgb(main), rgb(trim), rgb(glow)
    c = Canvas(grade)
    c.rect(2, 1, 12, 13, main, True)                    # the cover
    c.rect(2, 1, 3, 13, dk(main, 0.7))                  # the spine
    c.rect(13, 2, 13, 14, CREAM)                        # the pages (right edge)
    c.rect(3, 14, 13, 14, CREAM)                        # and the bottom edge
    c.p(13, 2, lt(CREAM, 0.3))
    for y in (3, 5, 7, 9, 11):                          # raised bands on the spine
        c.p(3, y, dk(main, 0.5))
    # trim corners
    for (x, y) in ((4, 2), (12, 2), (4, 13), (12, 13)):
        c.p(x, y, trim)
    c.p(11, 2, trim)
    c.p(12, 3, trim)
    c.p(11, 13, trim)
    c.p(12, 12, trim)
    # the strap and its clasp
    c.rect(4, 9, 12, 9, dk(main, 0.5))
    c.rect(4, 10, 12, 10, main)
    c.rect(7, 8, 9, 11, trim, True)
    c.p(8, 9, glow)
    c.p(8, 10, dk(glow, 0.7))
    emblem(c, trim, glow)
    return c.finish()


def emblem_arcane(c, trim, glow):
    for p in ((8, 3), (7, 4), (9, 4), (6, 5), (10, 5), (7, 6), (9, 6), (8, 7)):
        c.p(p[0], p[1], trim)
    c.p(8, 5, glow)


def emblem_leaf(c, trim, glow):
    for p in ((8, 3), (7, 4), (8, 4), (9, 4), (7, 5), (8, 5), (9, 5), (8, 6), (8, 7)):
        c.p(p[0], p[1], (120, 200, 90) if p != (8, 7) else trim)
    c.p(8, 5, glow)


def emblem_scroll(c, trim, glow):
    c.rect(6, 3, 10, 3, trim)
    c.rect(6, 5, 10, 5, trim)
    c.rect(6, 7, 10, 7, trim)
    c.p(10, 4, glow)
    c.p(6, 6, glow)


def emblem_cross(c, trim, glow):
    c.rect(8, 3, 8, 7, (236, 232, 224))
    c.rect(6, 5, 10, 5, (236, 232, 224))
    c.p(8, 5, glow)


def emblem_sun(c, trim, glow):
    c.disc(8, 5, 1, glow)
    for p in ((8, 2), (8, 8), (5, 5), (11, 5), (6, 3), (10, 3), (6, 7), (10, 7)):
        if 0 <= p[1] <= 7:
            c.p(p[0], p[1], trim)


def cupcake():
    main, trim, glow = rgb(0x8B5A3C), rgb(0xF4EAF8), rgb(0xC9A0FF)
    c = Canvas("RARE")
    # the fluted paper cup
    c.rows(3, 9, [
        "mmmmmmmmmm",
        ".mdmdmdmdm",
        ".mmmmmmmmm",
        "..mdmdmdm.",
        "..mmmmmmm.",
        "...mmmmm..",
    ], {"m": main, "d": dk(main, 0.72)}, shade="m")
    # the swirl of frosting in three tiers, streaked with lavender
    c.rect(3, 7, 12, 8, trim, True)
    c.rect(4, 5, 11, 6, trim, True)
    c.rect(5, 3, 10, 4, trim, True)
    c.rect(6, 2, 9, 2, trim, True)
    c.p(8, 1, trim)
    c.p(9, 1, lt(trim, 0.0))
    lav = rgb(0xB48CE8)
    for p in ((5, 8), (7, 7), (10, 8), (6, 5), (9, 6), (8, 3), (7, 4), (11, 7)):
        c.p(p[0], p[1], lav)
    c.p(7, 2, glow)
    c.p(13, 3, glow)
    c.p(2, 5, glow)
    c.p(12, 1, lt(glow, 0.4))
    return c.finish()


def transferrence():
    main, trim, glow = rgb(0xC8A66A), rgb(0x4A3320), rgb(0xFFD27F)
    c = Canvas("LEGEND")
    sack = [
        ".m.m.m.",
        ".mmmmm.",
        "..mmm..",
        ".mmmmm.",
        "mmmmmmm",
        "mmmmmmm",
        "mmmmmmm",
        "mmmmmmm",
        "mmmmmmm",
        ".mmmmm.",
    ]
    pal = {"m": main}
    c.rows(0, 5, sack, pal, shade="m")
    c.rows(9, 5, sack, pal, shade="m")
    for ox in (0, 9):
        c.rect(ox + 2, 7, ox + 4, 7, trim)               # the drawstring tied over the folded top
        c.p(ox + 3, 8, trim)
        c.p(ox + 3, 9, trim)
        c.p(ox + 2, 10, dk(main, 0.7))
        c.p(ox + 5, 12, dk(main, 0.7))
        c.p(ox + 1, 12, lt(main, 0.3))
    # the two bags are one: a ribbon of light runs between them
    c.rect(7, 10, 8, 10, glow)
    c.p(7, 9, lt(glow, 0.5))
    c.p(8, 11, lt(glow, 0.5))
    c.p(7, 2, glow)
    c.p(8, 1, lt(glow, 0.6))
    c.p(6, 3, glow)
    c.p(9, 3, glow)
    return c.finish()


def lifes_emergency():
    main, trim, glow = rgb(0x2A2018), rgb(0xD8B04A), rgb(0xE0364A)
    c = Canvas("UNIQUE")
    # an open-ended cuff seen from the front: a wide band
    for y in range(16):
        for x in range(16):
            ox, oy = (x - 7.5) / 7.6, (y - 8.5) / 6.2
            ix, iy = (x - 7.5) / 4.4, (y - 8.0) / 2.6
            outer = ox * ox + oy * oy <= 1.0
            inner = ix * ix + iy * iy <= 1.0
            if outer and not inner and not (y < 6 and abs(x - 7.5) < 2.5):
                c.p(x, y, main, True)
    # gold knot-work lines along the band
    for x in range(1, 15):
        for y in range(16):
            if (x, y) in c.px and (x + y) % 3 == 0 and (x < 5 or x > 10 or y > 10):
                c.p(x, y, trim)
    # the red four-petal gem on the front
    for p in ((7, 11), (8, 11), (6, 12), (7, 12), (8, 12), (9, 12), (5, 13), (6, 13), (7, 13), (8, 13), (9, 13), (10, 13), (6, 14), (7, 14), (8, 14), (9, 14)):
        c.p(p[0], p[1], glow, True)
    c.p(7, 12, lt(glow, 0.6))
    c.p(8, 13, dk(glow, 0.6))
    c.p(7, 11, trim)
    c.p(8, 14, trim)
    c.p(5, 13, trim)
    c.p(10, 13, trim)
    c.p(1, 5, lt(glow, 0.2))
    return c.finish()


def realistic_pouch():
    main, trim, glow = rgb(0xC9A89A), rgb(0x8A6A5A), rgb(0xFFE0D0)
    c = Canvas("RARE")
    c.rows(3, 3, [
        "...mmmmmm...",
        "..mmmmmmmm..",
        ".mmmmmmmmmm.",
        "mmmmmmmmmmmm",
        "mmmmmmmmmmmm",
        "mmmmmmmmmmmm",
        "mmmmmmmmmmmm",
        "mmmmmmmmmmmm",
        ".mmmmmmmmmm.",
        "..mmmmmmmm..",
    ], {"m": main}, shade="m")
    # the flap, its stitching and the round button
    for x in range(4, 14):
        c.p(x, 8, dk(main, 0.78))
    for x in range(5, 13, 2):
        c.p(x, 9, trim)
    c.p(8, 8, trim)
    c.p(9, 8, trim)
    c.p(8, 9, dk(trim, 0.7))
    c.p(8, 7, lt(trim, 0.3))
    # the neck cord
    c.p(6, 2, trim)
    c.p(10, 2, trim)
    c.p(7, 3, trim)
    c.p(9, 3, trim)
    c.p(5, 5, glow)
    return c.finish()


def sheriff_badge():
    main, trim, glow = rgb(0xB8BCC4), rgb(0x6A6E78), rgb(0xFFFFFF)
    c = Canvas("RARE")
    # two triangles make the six-pointed star
    for y in range(1, 11):
        half = (y - 1) * 0.72
        for x in range(16):
            if abs(x - 7.5) <= half:
                c.p(x, y, main, True)
    for y in range(5, 15):
        half = (14 - y) * 0.72
        for x in range(16):
            if abs(x - 7.5) <= half:
                c.p(x, y, main, True)
    # ball tips on the six points
    for (x, y) in ((7, 0), (8, 0), (1, 11), (14, 11), (1, 4), (14, 4), (7, 15), (8, 15)):
        pass
    for (x, y) in ((7, 1), (8, 1), (7, 14), (8, 14), (1, 11), (14, 11), (1, 5), (14, 5)):
        c.p(x, y, lt(main, 0.5))
    # the centre disc with the word as a dark bar
    c.disc(7.5, 8, 3, dk(main, 0.78))
    c.rect(5, 8, 10, 8, trim)
    c.p(6, 7, glow)
    c.p(9, 9, glow)
    c.p(4, 3, glow)
    return c.finish()


def urn():
    main, trim, glow = rgb(0x2C7A6E), rgb(0xC9983A), rgb(0xFF9A3A)
    c = Canvas("UNIQUE")
    c.ellipse(8, 10.5, 4.6, 4.2, main, True)           # the body
    c.rect(5, 6, 10, 7, main, True)                    # the neck
    c.rect(4, 6, 11, 6, trim)                          # the red-gold rim
    c.rect(5, 7, 10, 7, dk(trim, 0.7))
    # handles
    for (x, y) in ((2, 8), (2, 9), (3, 10), (13, 8), (13, 9), (12, 10)):
        c.p(x, y, trim)
    # red triangles and diamonds, gold dots
    for (x, y) in ((6, 10), (8, 11), (10, 10), (7, 12), (9, 12)):
        c.p(x, y, (190, 50, 44))
    for (x, y) in ((5, 9), (11, 9), (8, 9), (6, 13), (10, 13)):
        c.p(x, y, trim)
    # the black pudding oozing out of the lid, with two glowing eyes
    c.ellipse(8, 3.6, 3.3, 2.6, (22, 18, 28), True)
    c.p(10, 6, (22, 18, 28))
    c.p(5, 5, (22, 18, 28))
    c.p(7, 3, glow)
    c.p(9, 3, glow)
    c.p(7, 4, dk(glow, 0.7))
    c.p(9, 4, dk(glow, 0.7))
    c.p(6, 2, (80, 70, 96))
    return c.finish()


def crystal_dragon():
    main, trim, glow = rgb(0x3CC9C0), rgb(0x9FF0E8), rgb(0x7FFFF0)
    c = Canvas("RARE")
    art = [
        ".....w...w......",
        "....ww..www.....",
        "....www.wwww....",
        "hh..wwwwwwww....",
        "hhh.wwwwwwwww...",
        "hhhhwwwbbbbww...",
        ".hhhbbbbbbbbbw..",
        "..bbbbbbbbbbbb..",
        "..bbbbbbbbbbbbt.",
        "...bbbbbbbbbbbt.",
        "...bbbbbbbbbbtt.",
        "....bbbbbbbbbt..",
        "..ffbbbbbbbbff..",
        ".ssssssssssssss.",
        ".ssssssssssssss.",
    ]
    pal = {"w": lt(main, 0.35), "b": main, "h": trim, "t": dk(main, 0.7), "f": dk(main, 0.55), "s": (96, 130, 108)}
    c.rows(0, 0, art, pal, shade="wbh")
    for (x, y) in ((4, 14), (8, 13), (11, 14), (6, 13)):
        c.p(x, y, (60, 88, 70))                         # cracks in the green stone
    c.p(2, 4, NEAR_BLACK)                              # the eye
    c.p(6, 3, glow)
    c.p(9, 6, glow)
    c.p(7, 9, lt(glow, 0.5))
    return c.finish()


def shared_pouches():
    main, trim, glow = rgb(0x5E9E5A), rgb(0x3A8F8A), rgb(0xCFFFB0)
    c = Canvas("RARE")
    body = [
        ".mmmmm.",
        "mmmmmmm",
        "mmmmmmm",
        "mmmmmmm",
        "mmmmmmm",
        "mmmmmmm",
        ".mmmmm.",
    ]
    for ox in (0, 9):
        c.rows(ox, 7, body, {"m": main}, shade="m")
        c.rect(ox, 7, ox + 6, 8, trim, True)            # the teal striped cuff
        c.p(ox + 2, 7, lt(trim, 0.4))
        c.p(ox + 4, 7, lt(trim, 0.4))
        c.rect(ox + 1, 11, ox + 2, 11, NEAR_BLACK)      # the sleepy closed eyes
        c.rect(ox + 4, 11, ox + 5, 11, NEAR_BLACK)
        c.p(ox + 3, 13, dk(main, 0.5))                  # the little mouth
        c.p(ox + 3, 5, (70, 48, 44))                    # the cord up to its bead
        c.p(ox + 3, 6, (70, 48, 44))
        c.p(ox + 2, 4, (70, 48, 44))
    c.p(1, 3, glow)                                    # the bead ends of the cords
    c.p(10, 3, glow)
    # the one pocket dimension both open onto
    c.p(7, 10, glow)
    c.p(8, 10, lt(glow, 0.4))
    c.p(7, 11, lt(glow, 0.4))
    c.p(8, 11, glow)
    return c.finish()


def candy_bag():
    main, trim, glow = rgb(0xE8E8F0), rgb(0xE8782A), rgb(0xFF9AD0)
    c = Canvas("RARE")
    c.rows(2, 3, [
        "...mmmm...",
        "..mmmmmm..",
        ".mmmmmmmm.",
        "mmmmmmmmmm",
        "mmmmmmmmmm",
        "mmmmmmmmmm",
        "mmmmmmmmmm",
        "mmmmmmmmmm",
        "mmmmmmmmmm",
        ".mmmmmmmm.",
    ], {"m": (214, 220, 234)}, shade="m")
    c.rect(5, 2, 10, 2, (222, 226, 238))              # the gathered neck
    c.rect(6, 3, 9, 3, (180, 150, 100))               # the twine
    c.p(7, 4, (150, 120, 80))
    c.p(8, 4, (150, 120, 80))
    sweets = [(4, 7, (240, 90, 140)), (6, 6, (250, 190, 60)), (9, 7, (110, 200, 120)), (11, 6, (110, 160, 240)),
              (5, 9, (150, 110, 230)), (10, 9, (240, 90, 140)), (7, 8, (250, 190, 60)), (11, 11, (110, 200, 120)), (4, 11, (110, 160, 240))]
    for x, y, col in sweets:
        c.rect(x, y, x + 1, y, col)
        c.p(x, y + 1, dk(col, 0.7))
    # the big orange cursive M
    for p in ((6, 12), (6, 11), (7, 12), (8, 11), (9, 12), (10, 12), (10, 11)):
        c.p(p[0], p[1], trim)
    c.p(8, 12, trim)
    c.p(3, 5, glow)
    c.p(12, 4, glow)
    return c.finish()


def devils_due():
    main, trim, glow = rgb(0x9A5A36), rgb(0xC8794A), rgb(0xFF5A1A)
    c = Canvas("RARE")
    c.disc(7.5, 7.5, 6.5, main, True)
    # the raised rim: light on the top-left, scorched at the bottom-right
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if 5.2 <= d <= 6.6 and (x, y) in c.px:
                c.p(x, y, lt(main, 0.30) if (x + y) < 15 else dk(main, 0.62))
    # the horned devil with a trident and a long tail
    dev = (50, 24, 18)
    for p in ((6, 4), (5, 3), (9, 4), (10, 3), (7, 5), (8, 5), (7, 6), (8, 6), (7, 7), (8, 7), (6, 8), (9, 8), (7, 9), (8, 9)):
        c.p(p[0], p[1], dev)
    for p in ((11, 5), (11, 6), (11, 7), (11, 8), (11, 9), (10, 5), (12, 5), (11, 4)):
        c.p(p[0], p[1], dev)                              # the trident
    for p in ((6, 10), (5, 11), (4, 11), (3, 10), (3, 9)):
        c.p(p[0], p[1], dev)                              # the tail
    c.p(7, 5, glow)
    c.p(2, 12, glow)
    c.p(13, 3, glow)
    c.p(12, 12, dk(glow, 0.7))
    return c.finish()


def fury():
    main, trim, glow = rgb(0x201626), rgb(0x6A3C7E), rgb(0xFF3A5A)
    c = Canvas("RARE")
    c.disc(7.5, 8.5, 5.6, main, True)
    # flame / leaf points around the top and the sides
    for p in ((7, 1), (8, 1), (7, 2), (8, 2), (3, 3), (4, 3), (4, 4), (12, 3), (11, 3), (11, 4), (1, 7), (2, 7), (2, 8), (14, 7), (13, 7), (13, 8), (5, 2), (10, 2)):
        c.p(p[0], p[1], main, True)
    # purple veins
    for p in ((6, 5), (9, 5), (5, 11), (10, 11), (7, 13), (8, 13)):
        c.p(p[0], p[1], trim)
    # the two glowing slit eyes
    for p in ((4, 8), (5, 8), (6, 9), (11, 8), (10, 8), (9, 9)):
        c.p(p[0], p[1], glow)
    c.p(5, 7, lt(glow, 0.5))
    c.p(10, 7, lt(glow, 0.5))
    c.p(1, 11, dk(glow, 0.8))
    return c.finish()


ICONS = {
    "fast_travel_beads": beads,
    "arcanists_notebook": lambda: notebook(0x8A5A2B, 0xC4C8D4, 0x58D68D, emblem_arcane),
    "botanists_notebook": lambda: notebook(0x4F6B34, 0xB8C97A, 0x7CE36B, emblem_leaf),
    "historians_notebook": lambda: notebook(0x7A5B2A, 0xD8C58A, 0xFFD36B, emblem_scroll),
    "physicians_notebook": lambda: notebook(0x7A3A3A, 0xE8E4DC, 0xFF6B6B, emblem_cross),
    "theologians_notebook": lambda: notebook(0x6A5A8A, 0xF0D890, 0xFFF0B0, emblem_sun),
    "madama_cupcake": cupcake,
    "bags_of_transferrence": transferrence,
    "lifes_emergency": lifes_emergency,
    "bag_of_realistic_capacity": realistic_pouch,
    "plastic_sheriff_badge": sheriff_badge,
    "urn_of_black_pudding": urn,
    "crystal_dragon": crystal_dragon,
    "shared_pouches_of_transfer": shared_pouches,
    "martels_magical_bag_of_candy": candy_bag,
    "the_devils_due": devils_due,
    "fury_of_the_fallen": fury,
}


def main():
    tex = os.path.join(ASSETS, "textures", "item")
    mod = os.path.join(ASSETS, "models", "item")
    os.makedirs(tex, exist_ok=True)
    os.makedirs(mod, exist_ok=True)
    images = []
    for rid, fn in ICONS.items():
        im = fn().image()
        im.save(os.path.join(tex, rid + ".png"))
        with open(os.path.join(mod, rid + ".json"), "w", newline="\n") as f:
            json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/" + rid}}, f, indent=2)
            f.write("\n")
        images.append(im)
        print("wrote", rid)
    if "--sheet" in sys.argv:
        out = sys.argv[sys.argv.index("--sheet") + 1]
        cols = 6
        rows = (len(images) + cols - 1) // cols
        sheet = Image.new("RGBA", (cols * 136, rows * 136), (70, 74, 86, 255))
        for i, im in enumerate(images):
            big = im.resize((16 * 8, 16 * 8), Image.NEAREST)
            sheet.alpha_composite(big, ((i % cols) * 136 + 4, (i // cols) * 136 + 4))
        sheet.save(out)


if __name__ == "__main__":
    main()
