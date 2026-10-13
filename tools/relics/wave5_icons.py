#!/usr/bin/env python3
"""
Relics of Many Worlds, wave 5: the 16x16 inventory sprites and flat item models of the seven neck relics.

    python3 tools/relics/wave5_icons.py [--preview out.png]

Writes under src/main/resources/assets/nusmp/
  textures/item/<id>.png   the sprite: a dark outline, three or four shades per material lit from the upper left, a few glow pixels in the
                           relic's glow colour, and one outline pixel in the grade's colour (the grade rim)
  models/item/<id>.json    the flat item model (parent item/generated, layer0 = the sprite): neck relics are worn, not held, so they have
                           no 3D hand mesh
for amulet_of_the_warmage, hemlocks_amulet, amulet_of_safeguarding, amulet_of_the_huntress, the_amulet_of_lost_souls, lovelorn_locket,
amulet_of_dragon_rage.

Each sprite follows its card's art (measured off the card images): a dark steel chain with a faceted blue-violet leaf in a silver frame; a
teal chain and thorn ring round a purple crystal heart with a hanging shard; a celtic disc behind hanging metal strips crossed by a pink
crystal rod in three bands; a gold lioness head on a fine gold chain; a bronze heart-shield with a purple gem, crystal wings and dripping
purple light; a bronze-and-silver heart locket with a white enamel centre and a ruby heart; a black-iron heart with a blue orb under a
spiked dragon with red bat wings. Idempotent: running it again rewrites the same files.
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


def shades(base):
    """Four shades of a colour: light, mid (the colour), shadow, deep."""
    return [mix(base, (255, 255, 255), 0.5), base, mix(base, (14, 10, 22), 0.38), mix(base, (14, 10, 22), 0.65)]


class Canvas:
    def __init__(self):
        self.px = {}

    def put(self, x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            self.px[(x, y)] = c

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

    def grid(self, rows, pal, ox=0, oy=0):
        """Draw a picture from rows of characters (a '.' is empty; every other character is a key of pal)."""
        for j, row in enumerate(rows):
            for i, ch in enumerate(row):
                if ch != "." and ch in pal:
                    self.put(ox + i, oy + j, pal[ch])

    def chain(self, c, c2, y0=0, y1=4, left=3, right=12):
        """The two halves of a chain meeting above the pendant, links alternating light and dark."""
        n = y1 - y0
        for k in range(n + 1):
            t = k / max(1, n)
            x_l = int(round(left + (7 - left) * t))
            x_r = int(round(right + (8 - right) * t))
            col = c if k % 2 == 0 else c2
            self.put(x_l, y0 + k, col)
            self.put(x_r, y0 + k, col)

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


def shape_rows(widths, top, cx2=15):
    """Rows of a symmetric shape: widths[i] pixels wide, centred on the pair of columns 7 and 8."""
    out = []
    for i, w in enumerate(widths):
        lo = 8 - w // 2 if w % 2 == 0 else 7 - w // 2
        out.append((top + i, lo, lo + w - 1))
    return out


def heart_fn(cx, cy, size):
    """A heart: True for the pixels (centres at x+0.5, y+0.5) inside a heart of this size centred on (cx, cy)."""
    def inside(px, py):
        x = (px - cx) / size
        y = -(py - cy) / size
        return (x * x + y * y - 1.0) ** 3 - x * x * y ** 3 <= 0.0
    return inside


def fill_lit(c, fn, cx, cy, cols, rim=None, rim_cols=None):
    """Fill the pixels of fn with a shade by the lighting (light from the upper left); the outermost pixels take the rim colours."""
    cells = [(x, y) for y in range(16) for x in range(16) if fn(x + 0.5, y + 0.5)]
    cellset = set(cells)
    for (x, y) in cells:
        dx, dy = x + 0.5 - cx, y + 0.5 - cy
        d = math.hypot(dx, dy) + 1e-6
        lit = (-dx - dy) / (d * 1.4142)
        col = cols[0] if lit > 0.55 else cols[1] if lit > -0.1 else cols[2] if lit > -0.6 else cols[3]
        edge = any((x + ddx, y + ddy) not in cellset for ddx, ddy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if edge and rim_cols:
            col = rim_cols[0] if lit > -0.1 else rim_cols[1]
        c.put(x, y, col)
    return cellset


# ---------------------------------------------------------------------------------------------------------------- the sprites
def warmage(g):
    """Dark steel chain; a leaf-shaped pendant: a silver frame round faceted blue-violet panels."""
    steel = shades(rgb(0x4A4D5C))
    silver = shades(rgb(0xC4C8D6))
    gem = shades(rgb(0x7A6CE6))
    c = Canvas()
    c.chain(steel[1], steel[2])
    for y, lo, hi in shape_rows([2, 4, 6, 8, 8, 8, 6, 4, 2], 5):
        for x in range(lo, hi + 1):
            edge = x in (lo, hi) or y in (5, 13)
            c.put(x, y, silver[0] if (edge and x + y < 14) else silver[2] if edge else gem[1])
    # the facets inside the frame: a bright lozenge in the middle, a light left half, a dark right half
    for (x, y, k) in [(7, 8, 0), (8, 8, 0), (7, 9, 0), (8, 9, 0), (6, 9, 0), (9, 9, 0), (7, 10, 0), (8, 10, 0),
                      (6, 7, 0), (5, 9, 0), (5, 10, 0), (6, 11, 0), (7, 11, 1), (6, 8, 0), (7, 7, 1), (8, 7, 1),
                      (9, 8, 2), (10, 9, 2), (10, 10, 2), (9, 10, 2), (9, 11, 2), (8, 11, 2), (9, 7, 2), (7, 12, 1), (8, 12, 2)]:
        c.put(x, y, gem[k] if k != 0 else mix(gem[0], (255, 255, 255), 0.35))
    c.put(7, 8, (255, 255, 255))
    c.outline()
    c.sparkles(rgb(g["glow"]), [(2, 6), (13, 12), (3, 12), (12, 5)])
    return c


def hemlock(g):
    """A teal chain; a purple crystal heart ringed by teal thorns; a jagged shard hanging below."""
    teal = shades(rgb(0x3E8F96))
    crystal = shades(rgb(0xA24BC8))
    c = Canvas()
    c.chain(teal[1], teal[0], 0, 3, 3, 12)
    cx, cy = 7.5, 7.4
    heart = heart_fn(cx, cy, 4.7)
    cells = fill_lit(c, heart, cx, cy, crystal, rim=True, rim_cols=(teal[0], teal[2]))
    # facets of the crystal
    for (x, y) in [(5, 6), (6, 6), (5, 7), (9, 8), (10, 8), (8, 9), (7, 10)]:
        if (x, y) in cells:
            c.put(x, y, crystal[0] if x < 8 else crystal[2])
    c.put(5, 5, (255, 235, 255)) if (5, 5) in cells else None
    # thorns: a spike pushed out from some pixels of the heart's edge
    for (x, y) in sorted(cells):
        if (x * 7 + y * 3) % 5 == 0:
            for dx, dy in ((-1, 0), (1, 0), (0, -1)):
                q = (x + dx, y + dy)
                if q not in cells and (q[0] < 6 or q[0] > 9 or q[1] < 5):
                    c.put(q[0], q[1], teal[0])
                    break
    # the jagged shard below the heart
    for (x, y, k) in [(7, 12, 1), (8, 12, 2), (7, 13, 1), (8, 13, 2), (7, 14, 0), (8, 14, 2), (7, 15, 2), (6, 13, 2)]:
        c.put(x, y, crystal[k])
    c.outline()
    c.sparkles(rgb(g["glow"]), [(1, 12), (14, 13), (2, 2), (13, 1)])
    return c


def safeguarding(g):
    """A braided chain; a round celtic disc behind hanging metal strips, crossed by a pink crystal rod held by three bands."""
    metal = shades(rgb(0xA9ACB4))
    pink = shades(rgb(0xFF7FCB))
    c = Canvas()
    c.chain(metal[2], metal[0], 0, 4, 3, 12)
    cx, cy, r = 7.5, 8.2, 5.2
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if d <= r:
                ring = d > r - 1.1
                inner = 2.3 < d <= r - 1.1
                col = (metal[0] if x + y < 15 else metal[2]) if ring else (metal[1] if inner and (x + y) % 2 == 0 else metal[3] if inner else metal[2])
                c.put(x, y, col)
    # the hanging strips: four flat bars, fanned a little
    for k, (x0, x1) in enumerate([(5, 4), (7, 7), (8, 9), (10, 12)]):
        for j in range(8):
            t = j / 7
            x = int(round(x0 + (x1 - x0) * t))
            c.put(x, 7 + j, metal[0] if k % 2 == 0 else metal[1])
            if k % 2 == 0:
                c.put(x + 1, 7 + j, metal[2])
    # the pink rod crossing, three dark bands
    for x in range(1, 15):
        c.put(x, 7, pink[0])
        c.put(x, 8, pink[1] if x < 8 else pink[2])
    for x in (5, 7, 9):
        c.put(x, 7, metal[3])
        c.put(x, 8, metal[3])
    c.put(2, 7, (255, 225, 242))
    c.outline()
    c.sparkles(rgb(g["glow"]), [(0, 4), (15, 5), (14, 14), (1, 13)])
    return c


def huntress(g):
    """A fine gold chain; a gold lioness head with pale slit eyes."""
    gold = shades(rgb(0xD9A62E))
    eye = (238, 244, 196)
    c = Canvas()
    c.chain(gold[1], gold[0], 0, 3, 3, 12)
    # the ears: two points on the top corners
    for (x, y, k) in [(3, 4, 1), (4, 4, 2), (3, 5, 0), (4, 5, 1), (5, 5, 1), (12, 4, 1), (11, 4, 2), (12, 5, 2), (11, 5, 1), (10, 5, 1)]:
        c.put(x, y, gold[k])
    # the face: a wide rounded head narrowing to the chin
    cx, cy = 7.5, 9.0
    face = lambda x, y: ((x - cx) / 5.6) ** 2 + ((y - cy) / 4.5) ** 2 <= 1.0
    cells = fill_lit(c, face, cx, cy, gold, rim=True, rim_cols=(gold[1], gold[2]))
    # the pale slit eyes with a dark line through each, the brow above
    for (x, y) in [(4, 8), (5, 8), (6, 8), (9, 8), (10, 8), (11, 8)]:
        c.put(x, y, eye)
    for (x, y) in [(5, 8), (10, 8)]:
        c.put(x, y, gold[3])
    for (x, y) in [(4, 7), (5, 7), (6, 7), (9, 7), (10, 7), (11, 7)]:
        c.put(x, y, gold[2])
    # the muzzle: lighter, with a dark nose and mouth
    for (x, y) in [(6, 10), (7, 10), (8, 10), (9, 10), (6, 11), (7, 11), (8, 11), (9, 11)]:
        c.put(x, y, gold[0])
    c.put(7, 10, gold[3]); c.put(8, 10, gold[3])
    c.put(7, 12, gold[3]); c.put(8, 12, gold[3])
    c.put(6, 12, gold[2]); c.put(9, 12, gold[2])
    c.outline()
    c.sparkles(rgb(g["glow"]), [(0, 8), (15, 10), (1, 2), (14, 1)])
    return c


def lost_souls(g):
    """A dark chain; a bronze heart-shield round a big purple gem, crystal wings, purple light dripping below."""
    bronze = shades(rgb(0xA67C3E))
    violet = shades(rgb(0xA855F0))
    c = Canvas()
    c.chain(bronze[2], bronze[3], 0, 3, 3, 12)
    for y, lo, hi in shape_rows([8, 10, 10, 10, 8, 6, 4, 2], 4):
        for x in range(lo, hi + 1):
            edge = x in (lo, hi) or y == 4
            c.put(x, y, bronze[0] if (edge and x + y < 13) else bronze[2] if edge else bronze[1])
    # the crystal wings at the sides, swept up
    for (x, y, k) in [(0, 4, 1), (1, 5, 0), (2, 5, 1), (0, 6, 2), (1, 6, 1), (2, 6, 0), (1, 7, 2), (2, 7, 1), (3, 7, 0),
                      (15, 4, 1), (14, 5, 0), (13, 5, 1), (15, 6, 2), (14, 6, 1), (13, 6, 0), (14, 7, 2), (13, 7, 1), (12, 7, 0)]:
        c.put(x, y, violet[k])
    # the big gem: a ring of dark bronze, the violet gem inside with a glint
    for (x, y) in [(6, 6), (7, 6), (8, 6), (9, 6), (5, 7), (10, 7), (5, 8), (10, 8), (6, 9), (7, 9), (8, 9), (9, 9)]:
        c.put(x, y, bronze[3])
    for (x, y, k) in [(6, 7, 1), (7, 7, 0), (8, 7, 1), (9, 7, 1), (6, 8, 1), (7, 8, 1), (8, 8, 2), (9, 8, 2)]:
        c.put(x, y, violet[k])
    c.put(7, 7, (255, 245, 255))
    for (x, y) in [(4, 5), (11, 5), (7, 11), (8, 11)]:
        c.put(x, y, violet[0])
    # the purple light dripping from the point
    for (x, y, k) in [(7, 12, 0), (8, 12, 1), (7, 13, 1), (6, 13, 2), (8, 14, 1), (9, 13, 2), (7, 14, 2), (7, 15, 1)]:
        c.put(x, y, violet[k])
    c.outline()
    c.sparkles(rgb(g["glow"]), [(0, 10), (15, 11), (4, 14), (11, 15)])
    return c


def locket(g):
    """A short gold chain; a bronze-and-silver heart locket with a white enamel centre and a ruby heart."""
    bronze = shades(rgb(0xB08A4A))
    silver = shades(rgb(0x8A8F99))
    ruby = shades(rgb(0xE02A3A))
    c = Canvas()
    c.chain(bronze[1], bronze[0], 0, 3, 5, 10)
    c.put(7, 3, bronze[1]); c.put(8, 3, bronze[2]); c.put(7, 4, bronze[0]); c.put(8, 4, bronze[1])
    cx, cy = 7.5, 9.2
    cells = fill_lit(c, heart_fn(cx, cy, 5.5), cx, cy, bronze, rim=True, rim_cols=(bronze[0], bronze[3]))
    for (x, y) in list(cells):                               # a little silver filigree on the bronze
        if (x * 3 + y) % 7 == 0:
            c.put(x, y, silver[0])
    enamel = fill_lit(c, heart_fn(cx, cy + 0.2, 3.6), cx, cy, [(255, 255, 250), (244, 242, 236), (214, 210, 202), (190, 186, 176)])
    ruby_cells = fill_lit(c, heart_fn(cx, cy + 0.4, 2.2), cx, cy, [(255, 120, 120), (224, 42, 58), (170, 20, 40), (120, 10, 30)])
    c.put(6, 8, (255, 205, 205))
    c.outline()
    c.sparkles(rgb(g["glow"]), [(0, 6), (15, 8), (2, 15), (13, 15)])
    return c


def dragon_rage(g):
    """A black-iron heart with a blue orb; a spiked black dragon with red bat wings perched on top; the chain in its claws."""
    iron = shades(rgb(0x2B2630))
    red = shades(rgb(0x9A1F2B))
    orb = shades(rgb(0x39B6FF))
    c = Canvas()
    c.chain(iron[1], iron[0], 0, 1, 6, 9)
    c.put(7, 2, iron[0]); c.put(8, 2, iron[1])
    # the wings: red membranes on black bones, spread to both sides (the left one drawn, the right one its mirror)
    wing = ["b.....",
            "bb....",
            "brb...",
            "brrb..",
            "bRrrb.",
            ".bRrrb",
            "..bbbb"]
    for j, row in enumerate(wing):
        for i, ch in enumerate(row):
            if ch == ".":
                continue
            col = {"b": iron[2], "r": red[1], "R": red[2]}[ch]
            c.put(i, 1 + j, col)
            c.put(15 - i, 1 + j, col)
    # the dragon: two horns, a head with a pale eye, a neck and shoulders
    for (x, y, k) in [(6, 2, 2), (9, 2, 2), (6, 3, 1), (7, 3, 1), (8, 3, 1), (9, 3, 1), (6, 4, 1), (7, 4, 0), (8, 4, 1), (9, 4, 2),
                      (7, 5, 1), (8, 5, 2), (6, 6, 1), (7, 6, 1), (8, 6, 2), (9, 6, 2)]:
        c.put(x, y, iron[k])
    c.put(7, 4, (255, 120, 90)); c.put(8, 4, iron[3])
    # the heart below, the blue orb in its middle, thorns round it
    cx, cy = 7.5, 11.2
    cells = fill_lit(c, heart_fn(cx, cy, 4.1), cx, cy, [(196, 56, 66), (154, 31, 43), (112, 20, 32), (72, 12, 24)], rim=True, rim_cols=(iron[0], iron[2]))
    for (x, y, k) in [(7, 10, 0), (8, 10, 1), (6, 11, 1), (7, 11, 0), (8, 11, 1), (9, 11, 2), (7, 12, 1), (8, 12, 2)]:
        c.put(x, y, orb[k])
    c.put(7, 10, (240, 250, 255))
    c.outline()
    c.sparkles(rgb(g["glow"]), [(0, 12), (15, 13), (3, 14), (12, 15)])
    return c


RELICS = [
    ("amulet_of_the_warmage", "RARE", 0x8A7DFF, warmage),
    ("hemlocks_amulet", "LEGEND", 0xB44FD8, hemlock),
    ("amulet_of_safeguarding", "UNIQUE", 0xFF7FCB, safeguarding),
    ("amulet_of_the_huntress", "RARE", 0xF0F0A8, huntress),
    ("the_amulet_of_lost_souls", "LEGEND", 0xB05CFF, lost_souls),
    ("lovelorn_locket", "SPECIAL", 0xE02A3A, locket),
    ("amulet_of_dragon_rage", "LEGEND", 0x39B6FF, dragon_rage),
]


def main():
    os.makedirs(ICONS, exist_ok=True)
    os.makedirs(MODELS, exist_ok=True)
    sprites = []
    for rid, grade, glow, fn in RELICS:
        c = fn({"glow": glow})
        c.grade_rim(GRADE_RGB[grade])
        im = c.save(os.path.join(ICONS, rid + ".png"))
        sprites.append(im)
        with open(os.path.join(MODELS, rid + ".json"), "w") as f:
            json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/" + rid}}, f, indent=2)
            f.write("\n")
        print("wrote", rid)
    if "--preview" in sys.argv:
        out = sys.argv[sys.argv.index("--preview") + 1]
        scale = 10
        w = Image.new("RGBA", (len(sprites) * 16 * scale + (len(sprites) + 1) * 10, 2 * 16 * scale + 30), (58, 58, 70, 255))
        for i, im in enumerate(sprites):
            big = im.resize((16 * scale, 16 * scale), Image.NEAREST)
            w.paste(big, (10 + i * (16 * scale + 10), 10), big)
            light = Image.new("RGBA", (16 * scale, 16 * scale), (200, 200, 205, 255))
            light.paste(big, (0, 0), big)
            w.paste(light, (10 + i * (16 * scale + 10), 20 + 16 * scale))
        w.save(out)


if __name__ == "__main__":
    main()
