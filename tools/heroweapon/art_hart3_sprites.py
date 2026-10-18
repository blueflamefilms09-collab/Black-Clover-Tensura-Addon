"""art_hart3_sprites: the five 16 x 16 inventory sprites of the Vassal Scythe, Katana, Fan, Carriage and Book (textures/item/<id>.png).

Why: the owner's rule 7 (the repo's rules file) keeps each weapon's old-style sprite as its inventory icon while the weapon itself is a 3D model in hand, and
mesh_lib places the 3D mesh on the sprite's diagonal (place_on_sprite reads the axis of the opaque pixels). So the sprites are drawn first, by hand, as
pixel maps with a small palette, never downscaled from a render; a recognisable miniature of each weapon in its own colours on a transparent ground.

Each map has 16 rows of 16 characters; '.' is transparent and every other character is a key of the weapon's palette. The diagonal weapons run from the
lower left (the grip) to the upper right (the head), the way every sword sprite of the mod does; the carriage and the book stand upright.
"""
import math
import os

import numpy as np
from PIL import Image

ICON_SIZE = 16

# ---------------------------------------------------------------- a tiny canvas to draw the maps on
class Canvas:
    """A 16 x 16 grid of palette keys: put / line / arc / disc place pixels, outline() adds the one pixel dark rim, rows() gives the map the renderer reads."""

    def __init__(self):
        self.g = [["."] * ICON_SIZE for _ in range(ICON_SIZE)]

    def put(self, x, y, ch):
        x, y = int(round(x)), int(round(y))
        if 0 <= x < ICON_SIZE and 0 <= y < ICON_SIZE:
            self.g[y][x] = ch

    def line(self, x0, y0, x1, y1, ch):
        n = int(max(abs(x1 - x0), abs(y1 - y0)))
        for i in range(n + 1):
            t = i / max(1, n)
            self.put(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, ch)

    def arc(self, cx, cy, r, a0, a1, ch, steps=40):
        for i in range(steps + 1):
            a = math.radians(a0 + (a1 - a0) * i / steps)
            self.put(cx + r * math.cos(a), cy - r * math.sin(a), ch)

    def disc(self, cx, cy, r, ch):
        for y in range(ICON_SIZE):
            for x in range(ICON_SIZE):
                if math.hypot(x - cx, y - cy) <= r:
                    self.g[y][x] = ch

    def outline(self, ch="O", keep=()):
        edge = []
        for y in range(ICON_SIZE):
            for x in range(ICON_SIZE):
                if self.g[y][x] != ".":
                    continue
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < ICON_SIZE and 0 <= ny < ICON_SIZE and self.g[ny][nx] not in (".", ch):
                        edge.append((x, y))
                        break
        for x, y in edge:
            self.g[y][x] = ch

    def rows(self):
        return ["".join(r) for r in self.g]


# ---------------------------------------------------------------- the Vassal Scythe (black shaft, silver rings, crimson wrap, steel blade, violet gem at the blade's root)
SCYTHE_PALETTE = {
    "O": (14, 10, 22),          # the one pixel outline
    "k": (40, 34, 56),          # black lacquer of the snath
    "l": (86, 72, 118),         # its sheen
    "c": (112, 24, 42),         # the crimson wrap of the grip
    "C": (170, 44, 66),         # its lit lacing
    "q": (196, 204, 218),       # silver rings and the butt spike
    "Q": (112, 120, 140),       # silver in shadow
    "g": (170, 132, 72),        # the bronze collar
    "G": (110, 80, 40),         # bronze in shadow
    "b": (92, 100, 120),        # the spine of the blade, dark steel
    "B": (178, 190, 210),       # blade, lit steel
    "s": (244, 248, 255),       # the ground edge
    "v": (178, 98, 255),        # the amethyst
    "V": (240, 214, 255),       # its hot core
    "p": (104, 52, 196),        # its shade
}


def _scythe():
    c = Canvas()
    for t in range(0, 11):                                   # the snath: two pixels wide on the diagonal from the lower left
        c.put(2 + t, 14 - t, "k")
        c.put(3 + t, 14 - t, "l" if t % 2 == 0 else "k")
    for t in range(1, 5):                                     # the crimson wrap over the lower third
        c.put(2 + t, 14 - t, "c" if t % 2 else "C")
        c.put(3 + t, 14 - t, "C" if t % 2 else "c")
    for (x, y) in ((6, 10), (9, 7)):                         # two silver rings
        c.put(x, y, "q")
        c.put(x + 1, y, "Q")
    c.put(1, 15, "Q")                                         # the steel butt spike
    c.put(2, 15, "q")
    cx, cy = 7.0, 9.4                                         # the blade: a crescent between two arcs, wide at the root, thin at the point
    for y in range(ICON_SIZE):
        for x in range(ICON_SIZE):
            dx, dy = x - cx, cy - y
            th = math.degrees(math.atan2(dy, dx))
            if th < 0:
                th += 360.0
            if not 52.0 <= th <= 172.0:
                continue
            t = (th - 52.0) / 120.0
            ro = 6.6 + 1.2 * (1 - t) ** 0.7
            wd = 3.4 * (1 - t) + 0.9 * t
            r = math.hypot(dx, dy)
            if ro - wd <= r <= ro + 0.3:
                u = (r - (ro - wd)) / max(wd, 0.1)
                c.put(x, y, "s" if u < 0.34 else ("B" if u < 0.70 else "b"))
    for (x, y, ch) in ((13, 5, "G"), (12, 6, "g"), (13, 4, "g"), (12, 4, "G")):      # the bronze collar at the root
        c.put(x, y, ch)
    for (x, y, ch) in ((12, 5, "p"), (11, 4, "p"), (11, 5, "v"), (12, 3, "p")):      # the amethyst in it
        c.put(x, y, ch)
    c.put(11, 4, "V")
    c.outline()
    return c.rows()


SCYTHE = _scythe()

# ---------------------------------------------------------------- the Vassal Katana (steel, gold guard, indigo lacing, a sakura pink stone at the guard)
KATANA_PALETTE = {
    "O": (14, 12, 28),
    "s": (244, 247, 253),       # the ground edge
    "B": (186, 196, 218),       # blade, lit
    "b": (120, 130, 154),       # blade, shade
    "g": (214, 172, 80),        # gold habaki and guard
    "G": (140, 100, 44),
    "y": (255, 234, 164),
    "r": (28, 44, 92),          # the hilt wrap, dark indigo
    "R": (58, 90, 164),         # its lit lacing
    "w": (238, 232, 220),       # the ray skin between the lacing
    "m": (255, 124, 178),       # the sakura pink stone
    "M": (255, 228, 240),       # its hot core
}


def _katana():
    c = Canvas()
    for t in range(0, 10):                                   # the blade: two pixels and an edge line, bending a little toward the edge (up and left)
        x, y = 6 + t, 9 - t
        c.put(x, y - 1, "s")                                  # the ground edge, on the upper left
        c.put(x, y, "B")
        c.put(x + 1 - 0, y, "b")
    c.put(15, 0, "s")
    c.put(14, 0, "s")
    c.put(15, 1, "B")
    for (x, y, ch) in ((5, 10, "m"), (6, 9, "y"), (4, 9, "g"), (5, 8, "G"), (6, 11, "g"), (7, 10, "G")):       # the guard, the blade collar and the pink stone at its centre
        c.put(x, y, ch)
    c.put(5, 9, "M")
    for t in range(0, 5):                                     # the hilt: indigo lacing over white ray skin
        x, y = 4 - t, 11 + t
        c.put(x, y, "R" if t % 2 == 0 else "r")
        c.put(x + 1, y, "w" if t % 2 == 0 else "R")
        c.put(x - 1 if x - 1 >= 0 else x, y, "r")
    c.put(0, 15, "m")                                         # the pommel with its small stone
    c.put(1, 15, "m")
    c.put(0, 14, "M")
    c.put(1, 14, "G")
    c.outline()
    return c.rows()


KATANA = _katana()

# ---------------------------------------------------------------- the Vassal Fan (jet black ribs, a pale silver blue face, steel blades at the sides, a cyan jewel)
FAN_PALETTE = {
    "O": (14, 14, 22),          # the one pixel outline
    "k": (30, 30, 40),          # jet black lacquer
    "l": (86, 88, 108),         # its sheen
    "f": (218, 230, 238),       # the face, pale silver blue
    "F": (168, 186, 204),       # the face in a fold
    "s": (172, 180, 192),       # steel
    "S": (236, 242, 250),       # the sharpened edge of the steel
    "v": (200, 210, 222),       # the silver rim
    "r": (190, 40, 50),         # the red tassel
    "g": (222, 200, 150),       # the pivot plate
    "t": (92, 225, 255),        # the cyan jewel
    "T": (226, 250, 255),       # its hot core
    "h": (38, 150, 200),        # its shade
}


def _fan():
    c = Canvas()
    px, py = 4.0, 11.0                                        # the pivot, at the foot of a quarter disc that opens up and to the right
    R = 10.6
    for y in range(ICON_SIZE):
        for x in range(ICON_SIZE):
            dx, dy = x - px, py - y
            r = math.hypot(dx, dy)
            if r > R or r < 1.2 or dx < 0 or dy < -0.5:
                continue
            th = math.degrees(math.atan2(dy, dx))
            c.put(x, y, "f")                                   # the pale face
            if r > R - 0.95:
                c.put(x, y, "v")                               # the silver rim of the leaf
            if th < 4.5 or th > 85.5:
                c.put(x, y, "s")                               # the two steel blades at the sides
    for a in (19.0, 35.0, 51.0, 67.0):                         # the black ribs run from the pivot to the rim, one pixel wide
        c.line(int(round(px + 1.6 * math.cos(math.radians(a)))), int(round(py - 1.6 * math.sin(math.radians(a)))),
               int(round(px + (R - 1.6) * math.cos(math.radians(a)))), int(round(py - (R - 1.6) * math.sin(math.radians(a)))), "k")
    for (x, y) in ((5, 3), (6, 2), (4, 5)):                   # the sharpened edge of the upper blade catches the light
        c.put(x, y, "S")
    for (x, y) in ((9, 11), (11, 11), (13, 11)):              # and the lower one
        c.put(x, y, "S")
    for (x, y) in ((9, 6), (10, 5), (8, 8), (11, 8)):         # a few petals of the silver plum branch
        c.put(x, y, "T")
    c.put(3, 12, "k")                                         # the handle
    c.put(2, 13, "l")
    c.put(3, 13, "k")
    c.put(2, 14, "k")
    c.put(1, 14, "l")
    for (x, y, ch) in ((3, 11, "g"), (4, 12, "g"), (4, 11, "T"), (5, 11, "t"), (5, 12, "h"), (4, 10, "g")):        # the pivot plate and the cyan jewel
        c.put(x, y, ch)
    c.put(1, 15, "r")                                         # the red tassel with its two beads
    c.put(0, 15, "r")
    c.put(0, 14, "r")
    c.outline()
    return c.rows()


FAN = _fan()

# ---------------------------------------------------------------- the Vassal Carriage (ivory and gold, a blue canopy, an amber jewel), facing right
CARRIAGE_PALETTE = {
    "O": (22, 24, 44),          # the one pixel outline
    "w": (242, 235, 214),       # ivory body
    "W": (190, 178, 152),       # its shade
    "g": (222, 176, 62),        # gold
    "G": (150, 106, 36),        # gold in shadow
    "y": (255, 232, 150),       # gold highlight
    "b": (62, 98, 178),         # the canopy, royal blue
    "B": (36, 58, 120),         # deep blue: lining and upholstery
    "l": (127, 182, 232),       # sky blue: sheen of the canopy and the plume tip
    "a": (255, 194, 71),        # the amber jewel
    "A": (255, 246, 196),       # its hot core
    "m": (150, 98, 58),         # walnut spokes
    "M": (74, 46, 30),          # the dark walnut between them
    "f": (250, 251, 255),       # the plume
}


def _carriage():
    c = Canvas()
    for (x, y, ch) in ((7, 0, "a"), (8, 0, "A"), (7, 1, "a"), (8, 1, "a")):                      # the jewel on the roof
        c.put(x, y, ch)
    for y, (x0, x1) in ((2, (5, 10)), (3, (3, 12)), (4, (2, 13))):        # the canopy: a shallow dome of royal blue
        for x in range(x0, x1 + 1):
            c.put(x, y, "b")
    for (x, y) in ((5, 2), (6, 2), (4, 3), (5, 3), (3, 4)):                  # the sky blue sheen on its shoulder
        c.put(x, y, "l")
    for x in range(1, 14):                                                 # its gold rim
        c.put(x, 5, "g")
    c.put(1, 5, "G")
    c.put(13, 5, "y")
    for y in (6, 7):                                                       # two slim gold posts, deep blue lining between them
        c.put(2, y, "G")
        c.put(12, y, "G")
        for x in range(3, 12):
            c.put(x, y, "B")
    for x in range(1, 14):                                                 # the ivory body: a long tray with a gold edge
        c.put(x, 8, "y" if x in (1, 13) else "g")
        c.put(x, 9, "w")
        c.put(x, 10, "W" if x < 3 else "w")
    for x in range(2, 13):
        c.put(x, 11, "W")
    c.put(13, 7, "w")                                                      # the curved dash rises at the front
    c.put(13, 6, "w")
    wheel = ["..gggg..",
             ".gMMMMG.",
             "gMMmmMMG",
             "gMmyymMG",
             "gMmygmMG",
             "gMMmmMMG",
             ".gMMMMG.",
             "..GGGG.."]
    for j, row in enumerate(wheel):                                        # the wheel: a gold rim, dark walnut inside, lighter spokes, a gold hub
        for i, ch in enumerate(row):
            if ch != ".":
                c.put(3 + i, 8 + j, ch)
    c.put(3 + 1, 8 + 1, "y")
    c.put(3 + 2, 8, "y")
    for (x, y, ch) in ((14, 10, "g"), (15, 10, "y"), (15, 9, "g")):                      # the draught pole, with the yoke's ball at its end
        c.put(x, y, ch)
    c.outline()
    return c.rows()


CARRIAGE = _carriage()

# ---------------------------------------------------------------- the Vassal Book (deep teal leather, gold fittings, loose pages, a sapphire in a gold star)
BOOK_PALETTE = {
    "O": (8, 16, 26),           # the one pixel outline
    "n": (38, 84, 112),         # teal leather
    "N": (22, 50, 70),          # its shade
    "m": (64, 120, 148),        # its sheen
    "g": (224, 184, 86),        # gold
    "G": (150, 110, 44),        # gold in shadow
    "y": (255, 234, 156),       # gold highlight
    "p": (240, 232, 208),       # pages
    "P": (196, 184, 150),
    "b": (70, 160, 255),        # the sapphire
    "B": (214, 240, 255),       # its hot core
    "h": (34, 92, 224),         # its shade
    "r": (182, 40, 54),         # the ribbon
    "k": (104, 72, 50),         # the open clasp strap
}


def _book():
    c = Canvas()
    for y in range(2, 13):                                    # the cover: teal with a darker border
        for x in range(2, 14):
            edge = x in (2, 13) or y in (2, 12)
            c.put(x, y, "N" if edge else ("m" if (x + y) % 7 == 0 else "n"))
    for x in range(3, 13):                                    # the page block along the foot
        c.put(x, 13, "p" if x % 3 else "P")
    for y in (3, 5, 8, 10):                                   # the four raised gold bands of the spine
        c.put(1, y, "G")
        c.put(2, y, "g")
    for (x, y) in ((3, 3), (4, 3), (3, 4), (11, 3), (12, 3), (12, 4), (3, 11), (3, 10), (4, 11), (12, 11), (11, 11), (12, 10)):      # the gold corner caps
        c.put(x, y, "g")
    for (x, y) in ((8, 4), (8, 10), (5, 7), (11, 7), (6, 5), (10, 5), (6, 9), (10, 9)):           # the eight points of the star round the jewel
        c.put(x, y, "g")
    for (x, y) in ((7, 5), (9, 5), (7, 9), (9, 9), (6, 6), (10, 6), (6, 8), (10, 8)):
        c.put(x, y, "G")
    for (x, y) in ((7, 6), (8, 6), (9, 6), (7, 7), (8, 7), (9, 7), (7, 8), (8, 8), (9, 8)):        # the sapphire
        c.put(x, y, "b")
    for (x, y) in ((8, 6), (7, 6), (8, 7)):
        c.put(x, y, "B")
    for (x, y) in ((9, 8), (9, 7), (8, 8)):
        c.put(x, y, "h")
    for (x, y) in ((4, 0), (5, 0), (4, 1), (5, 1), (7, 0), (8, 0), (7, 1), (8, 1), (10, 1), (11, 1), (11, 0)):     # three loose pages standing out of the top
        c.put(x, y, "p" if (x + y) % 3 else "P")
    for y in range(4, 9):                                      # the clasp strap hangs open from the fore-edge, its buckle at the end
        c.put(14, y, "k")
    c.put(14, 9, "g")
    c.put(14, 10, "G")
    for (x, y) in ((5, 14), (5, 15), (6, 15)):                 # the red ribbon
        c.put(x, y, "r")
    c.outline()
    return c.rows()


BOOK = _book()

SPRITES = {
    "vassal_scythe": (SCYTHE, SCYTHE_PALETTE),
    "vassal_katana": (KATANA, KATANA_PALETTE),
    "vassal_fan": (FAN, FAN_PALETTE),
    "vassal_carriage": (CARRIAGE, CARRIAGE_PALETTE),
    "vassal_book": (BOOK, BOOK_PALETTE),
}


def render(rows, palette):
    """The 16 x 16 RGBA image of a pixel map; raises on a wrong shape or an unknown key (a typo must never ship as a blank pixel)."""
    if len(rows) != ICON_SIZE:
        raise ValueError("a sprite has %d rows, not %d" % (len(rows), ICON_SIZE))
    im = Image.new("RGBA", (ICON_SIZE, ICON_SIZE), (0, 0, 0, 0))
    px = im.load()
    for y, row in enumerate(rows):
        if len(row) != ICON_SIZE:
            raise ValueError("sprite row %d has %d characters: %r" % (y, len(row), row))
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            if ch not in palette:
                raise ValueError("sprite pixel (%d, %d): unknown palette key %r" % (x, y, ch))
            r, g, b = palette[ch]
            px[x, y] = (r, g, b, 255)
    return im


def write(item_id, icon_dir):
    rows, palette = SPRITES[item_id]
    im = render(rows, palette)
    os.makedirs(icon_dir, exist_ok=True)
    path = os.path.join(icon_dir, item_id + ".png")
    im.save(path, optimize=True)
    return path


def contact_sheet(out_png, zoom=16, ids=None):
    """All sprites enlarged on a slot-coloured ground, for looking at them."""
    ids = ids or list(SPRITES)
    sheet = Image.new("RGBA", (len(ids) * (ICON_SIZE * zoom + 12) + 12, ICON_SIZE * zoom + 24), (84, 89, 107, 255))
    for k, i in enumerate(ids):
        rows, palette = SPRITES[i]
        big = render(rows, palette).resize((ICON_SIZE * zoom, ICON_SIZE * zoom), Image.NEAREST)
        sheet.alpha_composite(big, (12 + k * (ICON_SIZE * zoom + 12), 12))
    os.makedirs(os.path.dirname(os.path.abspath(out_png)), exist_ok=True)
    sheet.convert("RGB").save(out_png)
    return out_png
