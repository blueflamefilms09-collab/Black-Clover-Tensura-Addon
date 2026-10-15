#!/usr/bin/env python3
"""
Relics of Many Worlds, part 4, wave 30 (the special wave): the 16x16 inventory sprites and flat item models of the Hand of Vecna, the Eye of Vecna,
the Deck of Many Things, the Infinity Gauntlet, the six Infinity Stones, Paradox and Warren's Key.

    python3 tools/relics/wave30_icons.py [--preview out.png]

Writes under src/main/resources/assets/nusmp/
  textures/item/<id>.png   the sprite: a dark outline, shades of the main material and the trim lit from the upper left, a few glow pixels and one
                           outline pixel in the grade's colour (the grade rim)
  models/item/<id>.json    the flat item model (parent item/generated) for every id but Paradox
  models/item/paradox.json (builtin/entity, a copy of the staff display set) and models/item/paradox_icon.json (the sprite): Paradox is a 3D hand model
Idempotent. Each sprite follows the reference art: the Vecna pair is the withered hand in its gold gauntlet and the pink-irised eye on its red cord;
the Deck is a fan of black and gold cards; the Gauntlet a black fist with six gems; the stones are cut gems (a cube, a long crystal, a round gem, an
emerald cut, a kite, an oval); Paradox a slanting thorned staff with a violet crystal crown; the Key a bronze skeleton key with a ringed bow.
"""
import json
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "nusmp")
ICONS = os.path.join(ASSETS, "textures", "item")
MODELS = os.path.join(ASSETS, "models", "item")

OUTLINE = (22, 18, 30)
GRADE_RGB = {"GOD": 0xFF5555, "GENESIS": 0xFFFFFF}


def rgb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def ramp(base, n=4):
    """n shades of a colour from light (index 0) to dark (n-1)."""
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

    def grid(self, rows, legend):
        assert len(rows) == 16, len(rows)
        for dy, row in enumerate(rows):
            assert len(row) == 16, (dy, row, len(row))
            for x, ch in enumerate(row):
                if ch != "." and ch != " ":
                    self.put(x, dy, legend[ch])

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


def poly_canvas(facets):
    """Draws crisp polygons [(points, colour)] onto a 16 x 16 canvas, in order."""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    for pts, col in facets:
        d.polygon(pts, fill=(*col, 255))
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            r, g, b, a = im.getpixel((x, y))
            if a > 128:
                cv.px[(x, y)] = (r, g, b)
    return cv


# ================================================================ Hand of Vecna
def hand_of_vecna(grade):
    """A withered grey hand, four long fingers and a thumb, in a gold gauntlet cuff with ruby settings and a bone-spiked guard."""
    lg = legend(0x8E8A78, 0xC9A24A, 0xE0245E)
    rows = [
        "................",
        "....b..b.b......",
        "...bab.bab.b....",
        "...bcb.bcb.bb...",
        "..bbcbbbcbbcbb..",
        "..bcbbbbbbbcbbb.",
        "..bbbbbbbbbbbcb.",
        ".bbbbbbgbbbbbbc.",
        "..bbbbbbbbbbbc..",
        "..cbbbbbbbbbc...",
        "...TUTUTUTUc....",
        "..TUgUTUgUTU....",
        "..UTUTUTUTUV....",
        "..VUVUVUVUVV....",
        "...VVVVVVVV.....",
        "................",
    ]
    cv = Canvas()
    cv.grid(rows, lg)
    for (x, y) in ((3, 12), (7, 12)):                                  # the ruby settings in the cuff
        cv.put(x, y, rgb(0xE0245E))
    cv.outline()
    cv.sparkles(rgb(0xFF7A9A), [(0, 3), (14, 1), (15, 9)])
    cv.grade_rim(GRADE_RGB[grade])
    return cv


# ================================================================ Eye of Vecna
def eye_of_vecna(grade):
    """A cream eyeball with a pink iris and a black pupil, trailing a red optic cord."""
    lg = legend(0xC9A24A, 0xE0245E, 0xFFD84A, x=0xD86AA0, w=0xEFE3C2)
    rows = [
        "................",
        ".....kkkkkk.....",
        "...kkwwwwwwkk...",
        "..kwwwwwwwwwwk..",
        "..kwwwxxxxwwwk..",
        ".kwwwxxyyxxwwwk.",
        ".kwwxxykkyxxwwk.",
        ".kwwxxykkyxxwwk.",
        ".kwwwxxyyxxwwwk.",
        "..kwwwxxxxwwwk..",
        "..kwwwwwwwwwwk..",
        "...kkwwwwwwkk...",
        ".....kkUUkk.....",
        "......UTU.......",
        ".......UV.......",
        "......VV........",
    ]
    cv = Canvas()
    cv.grid(rows, lg)
    for (x, y) in ((6, 5), (7, 4)):                                    # a glint on the iris
        cv.put(x, y, (255, 255, 255))
    cv.outline()
    cv.sparkles(rgb(0xFFD84A), [(0, 0), (15, 3), (1, 13)])
    cv.grade_rim(GRADE_RGB[grade])
    return cv


# ================================================================ Deck of Many Things
def deck_of_many_things(grade):
    """A fan of three black cards with gold borders and a gold star on the front one."""
    gold, gold_d, black = ramp(rgb(0xE6B94A), 3), None, rgb(0x1A1A22)
    cards = [
        ([(1, 5), (7, 2), (10, 11), (4, 14)], [(2, 6), (7, 3), (9, 10), (4, 13)]),
        ([(4, 3), (10, 2), (13, 11), (7, 14)], [(5, 4), (10, 3), (12, 10), (7, 13)]),
        ([(7, 2), (13, 4), (14, 13), (8, 14)], [(8, 3), (12, 5), (13, 12), (8, 13)]),
    ]
    facets = []
    for k, (outer, inner) in enumerate(cards):
        facets.append((outer, gold[1] if k == 2 else gold[2]))
        facets.append((inner, mix(black, (60, 50, 90), 0.0) if k == 2 else black))
    cv = poly_canvas(facets)
    for (x, y) in ((10, 8), (10, 7), (10, 9), (9, 8), (11, 8)):        # a cross-shaped star on the front card
        cv.put(x, y, rgb(0xFFF2B0))
    cv.put(10, 5, rgb(0xE0245E))
    cv.outline()
    cv.sparkles(rgb(0xFFD27A), [(0, 1), (15, 1), (0, 14)])
    cv.grade_rim(GRADE_RGB[grade])
    return cv


# ================================================================ the Infinity Gauntlet
def the_infinity_gauntlet(grade):
    """A black gauntlet with a ribcage palm and six gems: orange, purple, blue and red across the knuckles, green on the thumb, yellow in the palm."""
    lg = legend(0x2A2A36, 0xC9A24A, 0xFFD27A)
    rows = [
        "................",
        "..dbbdbbdbbdbb..",
        ".dbbbbbbbbbbbbd.",
        ".dbGbbHbbIbbJbd.",
        ".dbbbbbbbbbbbbd.",
        ".dabbbbbbbbbbcd.",
        "..dbbbbKbbbbcd..",
        ".KGbbbcbcbbbcd..",
        ".dbbbbbbbbbbd...",
        "..dbcbcbcbcbd...",
        "..dTbbbbbbbTd...",
        "...dTUTUTUTd....",
        "...dUVUVUVUd....",
        "...dVVVVVVVd....",
        "................",
        "................",
    ]
    gems = {"G": 0xFF8A2A, "H": 0xA04AF0, "I": 0x3A8BFF, "J": 0xE03A3A, "K": 0xFFE23A}
    cv = Canvas()
    for ch, col in gems.items():
        lg[ch] = rgb(col)
    lg["G"] = rgb(0xFF8A2A)
    cv.grid(rows, lg)
    cv.put(1, 7, rgb(0x3AE07A))                                         # the thumb stone (green)
    cv.outline()
    cv.sparkles(rgb(0xFFD27A), [(0, 1), (15, 4), (14, 14)])
    cv.grade_rim(GRADE_RGB[grade])
    return cv


# ================================================================ the six stones (cut gems, drawn as polygons)
def stone_space(grade):
    """The Tesseract: a blue cube, top face light, left face blue, right face dark."""
    c = ramp(rgb(0x3A8BFF), 4)
    cv = poly_canvas([([(8, 1), (14, 4), (8, 7), (2, 4)], c[0]), ([(2, 4), (8, 7), (8, 14), (2, 11)], c[1]), ([(14, 4), (8, 7), (8, 14), (14, 11)], c[2])])
    for p in ((8, 3), (7, 4), (9, 4)):
        cv.put(*p, (210, 235, 255))
    return finish_stone(cv, grade, 0x8FD0FF)


def stone_mind(grade):
    """The Mind Stone: a long yellow crystal, pointed at both ends."""
    c = ramp(rgb(0xFFD93A), 4)
    cv = poly_canvas([([(8, 0), (12, 4), (8, 7), (4, 4)], c[0]), ([(4, 4), (8, 7), (8, 15), (4, 11)], c[1]), ([(12, 4), (8, 7), (8, 15), (12, 11)], c[2])])
    for p in ((7, 3), (8, 2)):
        cv.put(*p, (255, 250, 210))
    return finish_stone(cv, grade, 0xFFF0A0)


def stone_power(grade):
    """The Power Stone: a round purple gem with a lighter table."""
    c = ramp(rgb(0xA04AF0), 4)
    cv = poly_canvas([([(5, 1), (11, 1), (14, 5), (14, 10), (11, 14), (5, 14), (2, 10), (2, 5)], c[2]),
                      ([(5, 2), (11, 2), (13, 5), (13, 9), (10, 13), (5, 13), (3, 9), (3, 5)], c[1]),
                      ([(6, 4), (10, 4), (11, 7), (10, 10), (6, 10), (5, 7)], c[0])])
    for p in ((6, 5), (7, 4)):
        cv.put(*p, (240, 220, 255))
    return finish_stone(cv, grade, 0xD0A0FF)


def stone_time(grade):
    """The Time Stone: a green emerald cut."""
    c = ramp(rgb(0x3AE07A), 4)
    cv = poly_canvas([([(4, 1), (11, 1), (13, 3), (13, 12), (11, 14), (4, 14), (2, 12), (2, 3)], c[2]),
                      ([(4, 2), (11, 2), (12, 4), (12, 11), (10, 13), (5, 13), (3, 11), (3, 4)], c[1]),
                      ([(5, 4), (10, 4), (10, 11), (5, 11)], c[0])])
    for p in ((6, 5), (6, 6)):
        cv.put(*p, (220, 255, 235))
    return finish_stone(cv, grade, 0xA0FFC0)


def stone_reality(grade):
    """The Reality Stone: a red kite-cut gem."""
    c = ramp(rgb(0xE03A3A), 4)
    cv = poly_canvas([([(8, 0), (14, 6), (8, 15), (2, 6)], c[2]), ([(8, 1), (13, 6), (8, 14), (3, 6)], c[1]), ([(8, 2), (9, 6), (8, 11), (7, 6)], c[0]),
                      ([(3, 6), (8, 2), (8, 6)], c[0])])
    for p in ((6, 5), (7, 4)):
        cv.put(*p, (255, 220, 215))
    return finish_stone(cv, grade, 0xFF9A9A)


def stone_soul(grade):
    """The Soul Stone: an orange oval gem."""
    c = ramp(rgb(0xFF8A2A), 4)
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.ellipse([3, 1, 12, 14], fill=(*c[2], 255))
    d.ellipse([3, 1, 11, 13], fill=(*c[1], 255))
    d.ellipse([5, 3, 9, 9], fill=(*c[0], 255))
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            r, g, b, a = im.getpixel((x, y))
            if a > 128:
                cv.px[(x, y)] = (r, g, b)
    for p in ((6, 4), (6, 5)):
        cv.put(*p, (255, 235, 210))
    return finish_stone(cv, grade, 0xFFC080)


def finish_stone(cv, grade, glow):
    cv.outline()
    cv.sparkles(rgb(glow), [(0, 1), (15, 2), (14, 14)])
    cv.grade_rim(GRADE_RGB[grade])
    return cv


# ================================================================ Paradox (the 16 x 16 sprite stays the inventory icon of the 3D staff)
def paradox(grade):
    """A slanting staff of dark thorned crystal: a violet crystal crown top right, a green-wrapped grip in the middle, claws at the foot."""
    cv = Canvas()
    shaft = [(2, 13), (3, 12), (4, 11), (5, 10), (6, 9), (7, 8), (8, 7), (9, 6), (10, 5)]
    for i, (x, y) in enumerate(shaft):
        cv.put(x, y, rgb(0x5A3A9A) if i % 2 == 0 else rgb(0x3E2A70))
    for (x, y) in ((5, 10), (6, 9), (7, 8)):                          # the green wrap
        cv.put(x, y, rgb(0x4E8A52))
        cv.put(x, y + 1, rgb(0x3A6A40))
    for (x, y) in ((4, 9), (7, 11), (9, 8), (6, 6)):                  # thorns
        cv.put(x, y, rgb(0x7A5AC0))
    for (x, y) in ((1, 14), (2, 15), (0, 12), (3, 14), (1, 11)):      # the foot's claws
        cv.put(x, y, rgb(0x6A4AB0))
    crown = {(11, 4): 0xC8A0FF, (12, 3): 0xE0C8FF, (13, 2): 0xF4E8FF, (13, 1): 0xFFFFFF, (11, 3): 0x9A6AE0, (11, 2): 0xB88AF0, (12, 5): 0x9A6AE0,
             (13, 4): 0xB88AF0, (14, 3): 0xC8A0FF, (12, 1): 0xE0C8FF, (10, 4): 0x7A4AC8, (12, 4): 0xFFFFFF, (14, 2): 0xE0C8FF, (11, 5): 0x7A4AC8}
    for p, col in crown.items():
        cv.put(p[0], p[1], rgb(col))
    cv.outline()
    cv.sparkles(rgb(0xC8A0FF), [(15, 0), (9, 2), (15, 6), (0, 9)])
    cv.grade_rim(GRADE_RGB[grade])
    return cv


# ================================================================ Warren's Key
def warrens_key(grade):
    """A long bronze skeleton key: a round bow of interlocked rings with a pale gem, a slim shaft with a flange, a labyrinth bit."""
    lg = legend(0x8A5A2A, 0xD8B060, 0xE8D8A0, x=0xE8E0D0)
    rows = [
        "................",
        "........TTTT....",
        ".......TUUUUT...",
        "......TUxxxxUT..",
        "......TUxsxxUV..",
        "......TUxxxxUV..",
        ".......TUUUUV...",
        "........bVVV....",
        ".......bb.......",
        "......bb........",
        ".....bb.........",
        "....bbTT........",
        "...bb.TUT.......",
        "..bbT.TVT.......",
        "..bTUT.TT.......",
        "...TT...........",
    ]
    cv = Canvas()
    cv.grid(rows, lg)
    cv.outline()
    cv.sparkles(rgb(0xFFF0B0), [(0, 1), (15, 8), (1, 8)])
    cv.grade_rim(GRADE_RGB[grade])
    return cv


ITEMS = [
    ("hand_of_vecna", hand_of_vecna, "GOD"), ("eye_of_vecna", eye_of_vecna, "GOD"), ("deck_of_many_things", deck_of_many_things, "GOD"),
    ("the_infinity_gauntlet", the_infinity_gauntlet, "GENESIS"), ("the_space_stone_or_the_tesseract", stone_space, "GOD"),
    ("the_mind_stone", stone_mind, "GOD"), ("the_power_stone", stone_power, "GOD"), ("the_time_stone_or_the_eye_of_agamotto", stone_time, "GOD"),
    ("the_reality_stone", stone_reality, "GOD"), ("the_soul_stone", stone_soul, "GOD"), ("paradox", paradox, "GOD"), ("warrens_key", warrens_key, "GOD"),
]


def write_models():
    os.makedirs(MODELS, exist_ok=True)
    for item_id, _fn, _g in ITEMS:
        if item_id == "paradox":
            continue
        with open(os.path.join(MODELS, item_id + ".json"), "w") as f:
            json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/" + item_id}}, f, indent=2)
    # Paradox: the 3D staff (builtin/entity, the display set of a staff; the lead's display-preset tool refits it) and the sprite as its inventory icon
    src = os.path.join(MODELS, "staff_of_moonlight.json")
    if os.path.exists(src):
        with open(src) as f:
            d = json.load(f)
        d["textures"] = {"particle": "nusmp:item/paradox"}
        d.pop("nusmp_display_preset", None)
        with open(os.path.join(MODELS, "paradox.json"), "w") as f:
            json.dump(d, f, indent=2)
    with open(os.path.join(MODELS, "paradox_icon.json"), "w") as f:
        json.dump({"parent": "minecraft:item/handheld", "textures": {"layer0": "nusmp:item/paradox"}}, f, indent=2)


def main():
    os.makedirs(ICONS, exist_ok=True)
    sheet = Image.new("RGBA", (16 * len(ITEMS) * 4, 64), (60, 60, 70, 255))
    for i, (item_id, fn, grade) in enumerate(ITEMS):
        im = fn(grade).save(os.path.join(ICONS, item_id + ".png"))
        sheet.paste(im.resize((64, 64), Image.NEAREST), (i * 64, 0), im.resize((64, 64), Image.NEAREST))
    write_models()
    if "--preview" in sys.argv:
        out = sys.argv[sys.argv.index("--preview") + 1]
        sheet.save(out)
        print("preview", out)
    print("wrote", len(ITEMS), "icons")


if __name__ == "__main__":
    main()
