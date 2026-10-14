#!/usr/bin/env python3
"""
Relics of Many Worlds, wave 16: the 16x16 inventory sprites and flat item models of the seventeen necklace pieces and charms.

    python3 tools/relics/wave16_icons.py [--preview out.png]

Writes under src/main/resources/assets/nusmp/
  textures/item/<id>.png   the sprite: a dark outline, four shades of the main material and three of the trim lit from the upper left, the relic's
                           own extra colours, a few glow pixels in the glow colour, and one outline pixel in the grade's colour (the grade rim)
  models/item/<id>.json    the flat item model (parent item/generated, layer0 = the sprite): none of these is a weapon, so there is no 3D hand mesh
for sigil_of_the_faint_flame, true_heart_clasp, brooch_of_poeur, holy_handgrenade, snugglable_doll, paralyzing_perfume, satchel_of_unmet_needs,
drow_sunscreen, portable_god, jar_of_bats, simple_blue_runechalk, dryads_wedding_band, weaversong, coin_of_draconic_luck, alabaster_white_runechalk,
strange_idol, wallet_of_the_slayer.

Each sprite follows the card's art (the notes the card was transcribed with): a gold ring of eight sapphire spirals with a small flame; a pink heart gem
in a studded bronze frame; an ice-blue snowflake of cut gems; a riveted maroon bomb with pink crystals and a rope fuse; a blue stitched cat with
X eyes and a grin; a fluted grey bottle with a gold nozzle and a purple bulb; a buckled brown satchel with glowing glyphs; an orange tube with a
drow in sunglasses; a crouched stone statuette with red eyes; a teal jar of green bats; a teal chalk stick with a glowing tip; a ring of twined vines;
a dark badge wrapped in silk; a gold dragon coin; a white chalk stick; a golden cat on a plinth with a green scar; a singed wallet with a skull clasp.
Shapes are drawn as character grids (a letter per shade) or from circles. Idempotent: running it again rewrites the same files.
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
    """n shades of a colour from light (index 0) to dark (n-1): a lit tint, the base, then ever darker."""
    out = [mix(base, (255, 255, 255), 0.42), base]
    for i in range(2, n):
        out.append(mix(base, (14, 10, 22), (i - 1) / (n - 1) * 0.62))
    return out


class Canvas:
    def __init__(self):
        self.px = {}

    def put(self, x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            self.px[(x, y)] = c

    def grid(self, rows, legend, top=0):
        for dy, row in enumerate(rows):
            assert len(row) == 16, (dy, row, len(row))
            for x, ch in enumerate(row):
                if ch != "." and ch != " ":
                    self.put(x, top + dy, legend[ch])

    def disc(self, cx, cy, r, shades, inner=None):
        """A lit disc (or ring when inner is given): light on the upper left, dark on the lower right."""
        for y in range(16):
            for x in range(16):
                d = math.hypot(x - cx, y - cy)
                if d <= r and (inner is None or d >= inner):
                    t = ((x - cx) + (y - cy)) / (2 * r)
                    self.put(x, y, shades[0 if t < -0.4 else 1 if t < 0.12 else 2 if t < 0.55 else 3])

    def line(self, x0, y0, x1, y1, c):
        n = max(abs(x1 - x0), abs(y1 - y0), 1)
        for i in range(n + 1):
            self.put(round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n), c)

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


def legend(main, trim, glow, x=None, w=None):
    """Letters: a b c d the main material light to dark, T U V the trim, g glow, x y z and w v u two extra materials, s white, k the deep outline."""
    m, t = ramp(rgb(main), 4), ramp(rgb(trim), 3)
    lg = {"a": m[0], "b": m[1], "c": m[2], "d": m[3], "T": t[0], "U": t[1], "V": t[2], "g": rgb(glow), "s": (255, 255, 255), "k": OUTLINE}
    if x is not None:
        e = ramp(rgb(x), 3)
        lg.update({"x": e[0], "y": e[1], "z": e[2]})
    if w is not None:
        e = ramp(rgb(w), 3)
        lg.update({"w": e[0], "v": e[1], "u": e[2]})
    return lg


def fill(rows, lg, tweaks=(), top=1):
    cv = Canvas()
    cv.grid(rows, lg, top=top)
    for (x, y, c) in tweaks:
        cv.px[(x, y)] = lg[c] if isinstance(c, str) else c
    return cv


# ------------------------------------------------------------------------------------------------ the seventeen sprites
def sigil_of_the_faint_flame():
    """A gold ring medallion with eight sapphire spirals, a small flame in the middle, a hoop on top and a thin cyan wisp above."""
    lg = legend(0xD9A628, 0x2D5FB5, 0x58E8F0, x=0xF08A28, w=0xFFE9A0)
    cv = Canvas()
    cv.disc(8, 9.5, 5.8, ramp(rgb(0xD9A628)), inner=3.0)
    cv.disc(8, 9.5, 3.0, ramp(rgb(0x1E3A78)))
    for k in range(8):
        a = k * math.pi / 4
        cv.put(round(8 + 4.4 * math.cos(a)), round(9.5 + 4.4 * math.sin(a)), lg["U"] if k % 2 == 0 else lg["T"])
    for (x, y, c) in ((8, 8, "x"), (8, 9, "x"), (7, 10, "y"), (8, 10, "y"), (9, 10, "y"), (8, 11, "z"), (8, 7, "w")):
        cv.put(x, y, lg[c])
    for (x, y) in ((7, 3), (8, 3), (7, 2), (8, 2)):
        cv.put(x, y, lg["b"] if (x, y) in ((7, 3), (8, 3)) else lg["c"])
    cv.put(8, 1, lg["g"])
    cv.put(9, 0, lg["g"])
    return cv


def true_heart_clasp():
    """A faceted pink-red heart gem in a studded bronze frame with a small crown on top and light blue cabochons above and below."""
    lg = legend(0xD94A6B, 0xB07A3A, 0x8FD8FF, x=0x8FD8FF, w=0xE86A8A)
    return fill([
        "......UTUT......",
        ".....UxwwxU.....",
        "..UUUUUTTUUUUU..",
        ".UTbbbbUbbbbTU..",
        ".UbaabbbbbbbbU..",
        ".UbabbbbbbbbbU..",
        ".UbbbbbbbbbcbU..",
        "..UbbbbbbbbcU...",
        "...UbbbbbbcU....",
        "....UbbbccU.....",
        ".....UbbcU......",
        "......UcU.......",
        ".....UxwxU......",
        "......UUU.......",
    ], lg)


def brooch_of_poeur():
    """A snowflake of pale ice-blue and white gems: a big square stone in the middle, round stones around it, long crystal shards in eight directions."""
    lg = legend(0xCFE8F5, 0x9BC4E6, 0xBFEFFF, x=0xFFFFFF, w=0x6FA8D8)
    cv = Canvas()
    for dx, dy in ((0, -1), (0, 1), (-1, 0), (1, 0), (-1, -1), (1, -1), (-1, 1), (1, 1)):
        n = 7 if dx == 0 or dy == 0 else 5
        cv.line(8 + dx * 2, 8 + dy * 2, 8 + dx * n, 8 + dy * n, lg["T"] if dx + dy in (-1, -2) else lg["U"])
    for dx, dy in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
        cv.put(8 + dx * 3, 8 + dy * 3, lg["w"])
        cv.put(8 + dx * 3 + (dx if dx > 0 else 0), 8 + dy * 3, lg["x"])
    for x in range(6, 10):
        for y in range(6, 10):
            cv.put(x, y, lg["x"] if (x + y) % 2 == 0 and x < 8 and y < 8 else lg["a"] if x < 8 or y < 8 else lg["b"])
    for (x, y) in ((8, 1), (8, 14), (1, 8), (14, 8)):
        cv.put(x, y, lg["x"])
    return cv


def holy_handgrenade():
    """A round riveted maroon iron bomb with a plated seam, a short twisted rope fuse and clusters of pale pink-white crystals growing from it."""
    lg = legend(0x6B2230, 0xD8A0B8, 0xFFE0F0, x=0xC8A070, w=0xF4D8E8)
    cv = Canvas()
    cv.disc(8, 9.5, 5.4, ramp(rgb(0x6B2230)))
    for y in range(5, 15):
        cv.put(8, y, lg["d"])
    for x in range(4, 13):
        cv.put(x, 9, lg["d"] if x != 8 else lg["c"])
    for (x, y) in ((6, 7), (10, 7), (6, 12), (10, 12), (8, 5)):
        cv.put(x, y, lg["T"])
    for (x, y) in ((8, 3), (8, 4), (9, 2), (9, 1)):
        cv.put(x, y, lg["x"] if (x + y) % 2 == 0 else lg["y"])
    cv.put(10, 1, lg["g"])
    cv.put(9, 0, lg["s"])
    for (x, y, c) in ((11, 4, "w"), (12, 3, "s"), (12, 4, "w"), (13, 2, "s"), (13, 3, "w"), (13, 5, "v"), (12, 5, "w"), (3, 5, "w"), (2, 4, "s"), (2, 5, "w"), (1, 3, "s"),
                      (3, 4, "w"), (11, 14, "w"), (12, 13, "s"), (12, 14, "v"), (4, 14, "w"), (3, 13, "s")):
        cv.put(x, y, lg[c])
    return cv


def snugglable_doll():
    """A blue stitched plush cat: big cyan X-button eyes, a wide toothy stitched grin, ears and a curled tail, a grey ghostly cat looming behind."""
    lg = legend(0x4A7FD0, 0x7FE8F0, 0xB0C4D8, x=0xB8BCC8, w=0x2A3A70)
    return fill([
        "..x.........x...",
        ".xyx.......xyx..",
        ".xyzxxxxxxxxzyx.",
        "..zbbbbbbbbbbz..",
        "..bbaTbbbbTabb..",
        "..bTUTbbbbUTUb..",
        "..bbTbbbbbbTbb..",
        "..bsbsbsbsbsbb..",
        "..bbsssssssbbb..",
        "..cbbbbbbbbbcb.w",
        "...cbbbbbbbbc.wv",
        "...cbbccbbbbc.wv",
        "...ccbbbbbcc.vw.",
        "....cc..cc....w.",
    ], lg)


def paralyzing_perfume():
    """A fluted grey glass perfume bottle on a pedestal with a gold sprayer nozzle and a purple knitted squeeze bulb."""
    lg = legend(0x8E93A0, 0xD4B24C, 0xC080FF, x=0x7A3FA8, w=0xE6E8F5)
    return fill([
        "......xxxx......",
        ".....xyyyyx.....",
        "....xyyxxyyx....",
        "......UUUU......",
        ".....TUUUUU.....",
        "......dccd......",
        ".....dabbbd.....",
        "....dabwbbbd....",
        "....dabwbbbc....",
        "....dabwbbcc....",
        "....dabbbbcc....",
        ".....dbbbcc.....",
        "....UUUUUUUV....",
        "...UTUUUUUUUV...",
    ], lg)


def satchel_of_unmet_needs():
    """A rugged reddish-brown leather satchel with a buckled flap and straps, golden-yellow swirl glyphs glowing on it."""
    lg = legend(0x8B4A2B, 0xE8B83C, 0xFFD060, x=0xB8703E, w=0x5A2E1A)
    return fill([
        "....dbbbbbbd....",
        "...dabbbbbbbc...",
        "..dabbbbbbbbbc..",
        "..dbbxxxxxxbbc..",
        "..dbxyyyyyyxbc..",
        "..dbxyUUUUyxbc..",
        "..dbxyUTTUyxbc..",
        "..dbbxyUUyxbbc..",
        "..dbbbxxxxbbbc..",
        "..dbbggbbggbbc..",
        "..dbbgbbbbgbbc..",
        "..dbbbggggbbbc..",
        "..dcbbbbbbbbcc..",
        "...cccccccccc...",
    ], lg)


def drow_sunscreen():
    """An orange and yellow squeeze tube with a yellow-rimmed orange screw cap, a purple drow face in sunglasses and a small sun on the label."""
    lg = legend(0xF08C1E, 0xF2D030, 0xFFE070, x=0x8040A0, w=0xC8A0E8)
    return fill([
        ".......UU.......",
        "......UTTU......",
        "......bbbb......",
        ".....baabbb.....",
        "....bbaTTTbb....",
        "...bbbTxxxTbb...",
        "...bbTxdddxTb...",
        "...bbTxwxwxTb...",
        "...bbTxxxxxTb...",
        "...bcbTxwwTbc...",
        "...cbbbTTTbbc...",
        "...cbbbbbbbbc...",
        "...UUUUUUUUUU...",
        "...VVVVVVVVVV...",
    ], lg)


def portable_god():
    """A small grey stone statuette of a crouched, monkey-like god with glowing red eyes and a curved blade, on a stone pedestal."""
    lg = legend(0x7B7F8C, 0xB8BCC8, 0xFF3030, x=0xFF3030, w=0x55596A)
    return fill([
        "......aaaa......",
        ".....abbbbb.....",
        ".....abxbxb.....",
        ".....abbbbb.....",
        "......bccb......",
        "....abbbbbbT....",
        "...abbbbbbbbTT..",
        "...abbwbbbbcbUT.",
        "....bbwbbbbbcU..",
        "....bbbbbbbbcT..",
        "...bbbccbbccbb..",
        "..dTTTTTTTTTTTd.",
        "..dwwwwwwwwwwwd.",
        "..dddddddddddd..",
    ], lg)


def jar_of_bats():
    """A tall teal-green glass jar with a lid tied with a red ribbon, a twig across the inside and small green bats with glowing yellow-green eyes."""
    lg = legend(0x2F9E8A, 0xC03030, 0xCCFF40, x=0x4FD060, w=0x8A6A3A)
    return fill([
        "......wwww......",
        "....UUUUUUUU....",
        "....UTUVUVUT....",
        ".....UUUUUU.....",
        "....abbbbbbc....",
        "...abbwwwwbbc...",
        "...abxxbbxxbc...",
        "...abxgxbxgxc...",
        "...abbbwbbbbc...",
        "...abxxbbxxbc...",
        "...abxgxbxgxc...",
        "...abbbbbbbbc...",
        "....bbbbbbbc....",
        ".....cccccc.....",
    ], lg)


def simple_blue_runechalk():
    """A short cylinder of teal-blue chalk, slightly chipped, its tip glowing cyan with a thin trail of blue smoke rising."""
    lg = legend(0x3FB6C8, 0xA8F0FF, 0x70E8FF, x=0xA8D8E8)
    cv = fill([
        "............g...",
        "...........g....",
        "...........g.g..",
        "..........Tgs...",
        ".........gTs....",
        "........abbb....",
        ".......abbbc....",
        "......abbbc.....",
        ".....abbbc......",
        "....abbbc.......",
        "...abbbc........",
        "..abbbc.........",
        "..bbcc..........",
        "..cc............",
    ], lg)
    cv.px[(5, 12)] = lg["a"]
    return cv


def dryads_wedding_band():
    """A delicate ring of intertwined twisting vines in silver-grey and gold-bronze strands with tiny thorns and green leaf tips."""
    lg = legend(0x9AA0A8, 0xB8863A, 0x80D860, x=0x58B048, w=0xD8F0A0)
    cv = Canvas()
    cv.disc(8, 8.5, 6.0, ramp(rgb(0x9AA0A8)), inner=3.4)
    for y in range(16):
        for x in range(16):
            if (x, y) in cv.px and ((x * 2 + y * 3) % 5 in (0, 1)):
                cv.px[(x, y)] = lg["U"] if (x + y) % 3 else lg["T"]
    for (x, y, c) in ((3, 4, "x"), (2, 4, "y"), (4, 3, "w"), (13, 6, "x"), (14, 6, "y"), (13, 12, "x"), (12, 13, "y"), (7, 2, "w"), (8, 2, "x"), (4, 13, "x"), (3, 13, "y")):
        cv.put(x, y, lg[c])
    return cv


def weaversong():
    """A round dark navy-black badge split by a vertical seam, leaf-shaped cut-outs in two columns, wrapped in pale white-lilac silk threads that glow white-pink."""
    lg = legend(0x15182B, 0xE8E0F5, 0xF0C8F8, x=0xF4D8F8)
    cv = Canvas()
    cv.disc(8, 8.5, 6.0, ramp(rgb(0x2A3050)))
    for y in range(3, 15):
        cv.put(8, y, lg["a"])
    for (x, y) in ((5, 5), (6, 6), (5, 8), (6, 9), (5, 11), (6, 12), (11, 5), (10, 6), (11, 8), (10, 9), (11, 11), (10, 12)):
        cv.put(x, y, lg["k"])
    cv.line(2, 6, 13, 4, lg["T"])
    cv.line(3, 11, 14, 8, lg["U"])
    cv.line(4, 13, 12, 3, lg["T"])
    cv.put(2, 7, lg["x"])
    cv.put(13, 12, lg["x"])
    return cv


def coin_of_draconic_luck():
    """A large round gold coin with a thick rim and an embossed grinning dragon head in bronze-gold with horns, fangs and scales."""
    lg = legend(0xD8A82A, 0x8A5A18, 0xFFD870, x=0xFFE9A0)
    cv = Canvas()
    cv.disc(8, 8.5, 6.2, ramp(rgb(0xD8A82A)))
    cv.disc(8, 8.5, 4.6, ramp(rgb(0xB88A22)))
    for (x, y, c) in ((5, 5, "U"), (4, 4, "U"), (11, 5, "U"), (12, 4, "U"), (6, 6, "T"), (7, 6, "T"), (9, 6, "T"), (10, 6, "T"), (6, 7, "U"), (10, 7, "U"),
                      (7, 7, "V"), (9, 7, "V"), (7, 9, "U"), (8, 9, "U"), (9, 9, "U"), (6, 10, "x"), (10, 10, "x"), (7, 11, "s"), (9, 11, "s"), (8, 8, "V"), (8, 10, "V")):
        cv.put(x, y, lg[c])
    return cv


def alabaster_white_runechalk():
    """A short, slightly rough stick of bright white chalk set diagonally, a soft pale-blue glow behind it."""
    lg = legend(0xF0F0F0, 0xBFD8F0, 0xA0D8FF, x=0xA0D8FF)
    cv = Canvas()
    cv.disc(8, 8, 6.4, [mix(rgb(0xA0D8FF), (0, 0, 0), 0.0)] * 4)
    for k in list(cv.px):
        cv.px[k] = mix(rgb(0xA0D8FF), (40, 60, 90), 0.55) if (k[0] + k[1]) % 2 else mix(rgb(0xA0D8FF), (40, 60, 90), 0.4)
    stick = fill([
        "................",
        "..............a.",
        "............aab.",
        "...........aabb.",
        "..........aabbc.",
        ".........aabbc..",
        "........aabbc...",
        ".......aabbc....",
        "......aabbc.....",
        ".....aabbc......",
        "....aabbc.......",
        "....abbc........",
        "....bcc.........",
        "................",
    ], lg)
    cv.px.update(stick.px)
    cv.px[(9, 7)] = lg["T"]
    cv.px[(6, 10)] = lg["U"]
    return cv


def strange_idol():
    """A golden seated cat on a flat square golden plinth: glowing green eyes, a green crown, a green stitched scar down the chest and green rune rings on the body."""
    lg = legend(0xD8B040, 0x58D868, 0x70FF80, x=0x58D868, w=0x8A6A20)
    return fill([
        "....x.x.x.......",
        "....xxxxx.......",
        "...abbbbbc......",
        "..abxbbbxbc.....",
        "..abbbbbbbc.....",
        "..abbbwbbbc.....",
        "...abxxxbc......",
        "..abbbxbbbc.....",
        "..abxbxbxbbc.c..",
        "..abbbxbbbbcc...",
        "..bbbbxbbbbc....",
        ".dddddddddddd...",
        ".dbbbbbbbbbbd...",
        ".dcccccccccc....",
    ], lg)


def wallet_of_the_slayer():
    """A small tan and golden-brown leather wallet with a flap, a brown patch, a golden skull clasp and a singed, frayed corner, wreathed in dark grey smoke."""
    lg = legend(0xA77B4A, 0xD8B040, 0x606068, x=0x6A6A74, w=0x8A5A32)
    return fill([
        "........x.x.....",
        ".......x..x.x...",
        "..bbbbbbbbbbbbd.",
        ".abbbbbbbbbbbbc.",
        ".abbbbbTTbbbbbc.",
        ".abbbbTsTsTbbbc.",
        ".abbbbTTTTTbbbc.",
        ".abbwwbbTUbbbbc.",
        ".abbwvbbbbbbbbc.",
        ".abbbbbbbbbbbcc.",
        ".acbbbbbbbbbc.c.",
        ".cdbbbbbbbbc..d.",
        "..cccccccccc.xx.",
        "............x.x.",
    ], lg)


SPRITES = [
    ("sigil_of_the_faint_flame", sigil_of_the_faint_flame, "UNIQUE", 0x58E8F0),
    ("true_heart_clasp", true_heart_clasp, "RARE", 0xFF8FA8),
    ("brooch_of_poeur", brooch_of_poeur, "LEGEND", 0xBFEFFF),
    ("holy_handgrenade", holy_handgrenade, "LEGEND", 0xFFE0F0),
    ("snugglable_doll", snugglable_doll, "RARE", 0xB0C4D8),
    ("paralyzing_perfume", paralyzing_perfume, "RARE", 0xC080FF),
    ("satchel_of_unmet_needs", satchel_of_unmet_needs, "SPECIAL", 0xFFD060),
    ("drow_sunscreen", drow_sunscreen, "SPECIAL", 0xFFE070),
    ("portable_god", portable_god, "RARE", 0xFF3030),
    ("jar_of_bats", jar_of_bats, "RARE", 0xCCFF40),
    ("simple_blue_runechalk", simple_blue_runechalk, "NORMAL", 0x70E8FF),
    ("dryads_wedding_band", dryads_wedding_band, "UNIQUE", 0x80D860),
    ("weaversong", weaversong, "RARE", 0xF0C8F8),
    ("coin_of_draconic_luck", coin_of_draconic_luck, "SPECIAL", 0xFFD870),
    ("alabaster_white_runechalk", alabaster_white_runechalk, "RARE", 0xA0D8FF),
    ("strange_idol", strange_idol, "RARE", 0x70FF80),
    ("wallet_of_the_slayer", wallet_of_the_slayer, "LEGEND", 0x909098),
]


def model_json(rid):
    return {"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/" + rid}}


def main():
    os.makedirs(ICONS, exist_ok=True)
    os.makedirs(MODELS, exist_ok=True)
    ims = []
    for rid, fn, grade, glow in SPRITES:
        cv = fn()
        cv.outline()
        cv.sparkles(rgb(glow), [(0, 1), (15, 2), (15, 14), (0, 15)])
        cv.grade_rim(GRADE_RGB[grade])
        ims.append(cv.save(os.path.join(ICONS, rid + ".png")))
        with open(os.path.join(MODELS, rid + ".json"), "w", newline="\n") as f:
            json.dump(model_json(rid), f, indent=2)
            f.write("\n")
    if "--preview" in sys.argv:
        out = sys.argv[sys.argv.index("--preview") + 1]
        S = 12
        cols = 9
        rows = (len(ims) + cols - 1) // cols
        sheet = Image.new("RGBA", (cols * (16 * S + 10) + 10, rows * (16 * S + 10) + 10), (58, 58, 70, 255))
        for i, im in enumerate(ims):
            big = im.resize((16 * S, 16 * S), Image.NEAREST)
            sheet.paste(big, (10 + (i % cols) * (16 * S + 10), 10 + (i // cols) * (16 * S + 10)), big)
        sheet.save(out)
    print("wrote", len(SPRITES), "sprites and models")


if __name__ == "__main__":
    main()
