#!/usr/bin/env python3
"""
Relics of Many Worlds, wave 6: the 16x16 inventory sprites and flat item models of the ten chest armours and robes.

    python3 tools/relics/wave6_icons.py [--preview out.png]

Writes under src/main/resources/assets/nusmp/
  textures/item/<id>.png      the sprite: a dark outline, four shades of the main material and three of the trim lit from the upper left, the
                              relic's own extra colours (a skirt, a cloak, slime), a few glow pixels in the glow colour, and one outline pixel in
                              the grade's colour (the grade rim)
  models/item/<id>.json       the flat item model (parent item/generated, layer0 = the sprite): chest relics are worn, so they have no 3D hand mesh
for skycrystal_armor, slime_armour, armor_of_the_defiant_dawn, suit_of_the_astral_overseer, shadow_robes, mistwalker_robe,
nekets_practical_mage_robe, armor_of_plot, zar_sancuis_plate_of_the_red_prophet, skitchs_smuggling_coat.

Each sprite follows the card's art (the notes the card was transcribed with): a navy plate with a feathered crystal collar and a red tasset; a
grey-green cuirass dripping mint slime; a dark tabard with a gold star, a white cloak and a pleated skirt; a headless suit with a starry cape and a woven
white chest; a black sleeveless robe in smoke; a hooded purple robe with gold embroidery and mist; a slate hooded robe with a trailing sash; adventurer's
steel with a mail shirt and belts; a gunmetal plate with spiked shoulders and a crimson cloak; a patchwork coat with things poking from the pockets.
The shapes are drawn as character grids (a letter per shade), mirrored left to right where the art is symmetrical. Idempotent: running it again
rewrites the same files.
"""
import json
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


def sym(*halves):
    """Rows given as their left 8 columns, mirrored into 16."""
    return [h + h[::-1] for h in halves]


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
    """Letters: a b c d the main material light to dark, T U V the trim, g glow, x y z and w v u two extra materials, s a star, k the deep outline."""
    m, t = ramp(rgb(main), 4), ramp(rgb(trim), 3)
    lg = {"a": m[0], "b": m[1], "c": m[2], "d": m[3], "T": t[0], "U": t[1], "V": t[2], "g": rgb(glow), "s": (255, 255, 255), "k": OUTLINE}
    if x:
        e = ramp(rgb(x), 3)
        lg.update({"x": e[0], "y": e[1], "z": e[2]})
    if w:
        e = ramp(rgb(w), 3)
        lg.update({"w": e[0], "v": e[1], "u": e[2]})
    return lg


# ------------------------------------------------------------------------------------------------ the ten sprites
def fill(rows, lg, tweaks=()):
    cv = Canvas()
    cv.grid(rows, lg, top=1)
    for (x, y, c) in tweaks:
        cv.px[(x, y)] = lg[c] if isinstance(c, str) else c
    return cv


def skycrystal_armor():
    """Navy plate, a feathered crystal collar with a horned crest, silver-blue crystal trim, a red-to-pink tasset."""
    lg = legend(0x1B2338, 0xB8D8F0, 0x8FD8FF, x=0xC2506A)
    return fill(sym(
        "....T..g", ".bbbTUTU", ".babbUTU", ".bbbbbUT", ".bbb.bUT", ".bcb.bbg", ".bbb.bbb", ".dbb.bbb",
        ".....bbb", "....UUUU", "...xxxxx", "..xxyyyy", "..yyzzyz", "...zz.zz"), lg)


def slime_armour():
    """A grey-green scale cuirass with swirling silver pauldrons and a breastplate, slime dripping from the hem."""
    lg = legend(0x3A4A44, 0x9FE8C8, 0x6BFFB5, x=0x6BFFB5)
    return fill(sym(
        ".dbbb...", ".dbTUbb.", ".bTbUbbb", ".bUbbTbb", ".bbb.bUb", ".bcb.bTb", ".bbb.bbb", ".dbb.bbb",
        ".....cbb", ".....bxb", "....xxxy", "....xyxy", "....y.zy", ".....z.z"), lg)


def armor_of_the_defiant_dawn():
    """A dark tabard with a big gold eight-pointed star, a belt with a sun buckle, a pleated red-brown skirt, a white cloak over the shoulders."""
    lg = legend(0x2B211D, 0xE6B94A, 0xFFE9A0, x=0x7A3A2A, w=0xF2F0EA)
    return fill(sym(
        "..wwww..", ".wwwwwb.", ".wvwbbbb", ".wvbbTbb", ".wvbTTTb", ".wvbbTbb", ".wvbTbTb", ".wvUUUUg",
        ".wvxxyxx", ".wvxyxyx", "..vxyxyx", "..wyzyzy", "...zyzyz", "...z.z.z"), lg)


def suit_of_the_astral_overseer():
    """A headless suit: a diamond hood, a woven white chest with shoulder gems and a belt gem, purple sleeves and shorts, white boots, a long starry purple cape."""
    lg = legend(0x5B2E91, 0xE6E8F5, 0xB070FF, x=0x2A1550, w=0xE6E8F5)
    cv = fill(sym(
        ".......b", "......bb", ".....bbb", ".xgwwTUb", ".xzwTUTU", ".xyzUTUT", ".xyzTUTU", ".xyzUTUg",
        ".xyybbbb", ".xyybbbb", ".xyy.ww.", ".xyz.wv.", ".xz..wv.", "......uu"), lg)
    for (x, y) in ((2, 6), (3, 9), (2, 11), (13, 7), (12, 10), (13, 12)):
        if (x, y) in cv.px:
            cv.px[(x, y)] = (255, 255, 255)
    return cv


def shadow_robes():
    """A black sleeveless robe with short caped shoulders, thin silver seams, little four-point stars and a wrapped waist sash, in smoke."""
    lg = legend(0x14141C, 0x9A9AAE, 0x8A7CFF, x=0x3A3A4C)
    cv = fill(sym(
        "......bb", "....bbbb", "..bbbbbb", ".bbUbbbb", ".bUbbsbb", "..dbsssb", "..dbbsbb", "..dUUUUU",
        "..dbbbbU", ".dbbbUbb", ".dbbUbbb", ".dbbUbbb", ".dbbUbbb", ".dUUUUUU"), lg)
    for (x, y) in ((1, 3), (1, 7), (2, 11), (14, 5), (13, 9), (14, 12), (1, 14), (13, 14)):
        if (x, y) not in cv.px:
            cv.px[(x, y)] = lg["x"]
    return cv


def mistwalker_robe():
    """A hooded purple robe with gold filigree on the hood edge, sleeves and hem, teal gems on the cuffs and grey mist swirling from the hood."""
    lg = legend(0x5A3A7A, 0xC9A94F, 0xA8C8D8, x=0x2FB0A8, w=0xB8B8C0)
    return fill(sym(
        "....wwvw", ".....bbT", "....bbdd", "....bTdd", "..bbbbbb", ".bbTbbbb", ".xTbbbUb", ".bTbbbbb",
        ".bTb.bUb", "..bbbUbb", "..bbTbbb", ".bbbTbbb", ".bTbTbTb", ".UTUTUTU"), lg)


def nekets_practical_mage_robe():
    """A slate blue-purple hooded long robe with pale lavender trim, a wide sash with trailing straps and two side pockets."""
    lg = legend(0x4C4F8C, 0xC9C6EC, 0x9AA4FF)
    return fill(sym(
        "....bbbb", "...bbaab", "...bbddd", "...bbddd", "..bUbbbb", ".bbUbbbb", ".bbbbbbb", ".bTTTTTT",
        ".bbbbbTU", ".bcbbbbT", ".bcbbbbb", ".bbbbUbb", ".bbbbUbb", ".UUUUUUU"), lg,
        tweaks=((11, 10, "T"), (12, 11, "T"), (12, 12, "U"), (13, 13, "T"), (13, 14, "U")))


def armor_of_plot():
    """Adventurer's steel: pauldrons with gold trim and a gem at the neck, a chainmail torso, brown belts and a sheathed dagger, an orange-brown right sleeve."""
    lg = legend(0x6E7280, 0xC9A24A, 0x7FB4FF, x=0x4A4D58, w=0xB5702E)
    lg.update({"r": rgb(0x7A4A2A), "q": rgb(0x4E2E18), "m": rgb(0x9A9EAA)})
    return fill(sym(
        ".bbbb...", ".bbbbTg.", ".baabbbT", ".bbbbbbb", ".bUb.xmx", ".bbb.mxm", ".bbb.xmx", ".dbb.mxm",
        ".....rrr", ".....rUr", "....xmxm", "....rrrr", ".....rr.", "........"), lg,
        tweaks=((12, 5, "w"), (13, 5, "v"), (13, 6, "w"), (12, 6, "v"), (13, 7, "v"), (12, 7, "w"), (13, 8, "g"), (3, 12, "q")))


def zar_sancuis_plate_of_the_red_prophet():
    """A full dark gunmetal plate with gold-trimmed etching, spiked layered pauldrons with gold rivets, a crimson cloak hanging from the waist with a gold border."""
    lg = legend(0x3B3E46, 0xC9A24A, 0x9E1F2A, x=0x9E1F2A)
    return fill(sym(
        ".T..T...", ".bbbbbd.", ".bTbbbbd", ".bbbbUbb", ".bbb.bbb", ".bUb.bbb", ".bbb.bbb", ".dbb.bUU",
        ".....bxx", ".....bxy", "....Uxyy", "....Uxyy", "....Uyzz", ".....Uzz"), lg)


def skitchs_smuggling_coat():
    """A patchwork brown leather long coat stitched from mismatched patches, a sword, a chain, a green bottle and a jug poking out of the pockets."""
    lg = legend(0x6B4A2E, 0x9A7248, 0xC8A060, x=0x8A6240, w=0x4E3522)
    lg.update({"e": rgb(0x4FA05A), "f": rgb(0xBFC4CC), "h": rgb(0xB88A4A)})
    cv = fill(sym(
        "...ddb..", "..dbbbU.", ".dbbwUbb", ".dbxbUbb", ".dwbbUbx", ".dbbxUbb", ".dxbbUbw", ".dbwbUbb",
        ".dbbxUhb", ".dxbbUbb", ".dbwbUbx", ".dbbxUbb", ".dwbbUbb", ".dddddUd"), lg)
    for (x, y) in list(cv.px):
        if cv.px[(x, y)] in (lg["x"], lg["w"]) and x >= 8 and (x * 7 + y * 3) % 4 == 0:
            cv.px[(x, y)] = lg["w"] if cv.px[(x, y)] == lg["x"] else lg["x"]
    for (x, y, c) in ((2, 4, "e"), (2, 3, "e"), (1, 10, "f"), (2, 9, "f"), (13, 10, "h"), (14, 10, "h"), (14, 11, "h"), (13, 12, "f"), (14, 13, "f")):
        cv.px[(x, y)] = lg[c]
    return cv


SPRITES = [
    ("skycrystal_armor", skycrystal_armor, "LEGEND", 0x8FD8FF),
    ("slime_armour", slime_armour, "UNIQUE", 0x6BFFB5),
    ("armor_of_the_defiant_dawn", armor_of_the_defiant_dawn, "UNIQUE", 0xFFE9A0),
    ("suit_of_the_astral_overseer", suit_of_the_astral_overseer, "LEGEND", 0xB070FF),
    ("shadow_robes", shadow_robes, "UNIQUE", 0x8A7CFF),
    ("mistwalker_robe", mistwalker_robe, "RARE", 0xA8C8D8),
    ("nekets_practical_mage_robe", nekets_practical_mage_robe, "RARE", 0x9AA4FF),
    ("armor_of_plot", armor_of_plot, "LEGEND", 0x7FB4FF),
    ("zar_sancuis_plate_of_the_red_prophet", zar_sancuis_plate_of_the_red_prophet, "LEGEND", 0xE04050),
    ("skitchs_smuggling_coat", skitchs_smuggling_coat, "UNIQUE", 0xC8A060),
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
        S = 14
        sheet = Image.new("RGBA", (len(ims) * (16 * S + 10) + 10, 16 * S + 20), (58, 58, 70, 255))
        for i, im in enumerate(ims):
            big = im.resize((16 * S, 16 * S), Image.NEAREST)
            sheet.paste(big, (10 + i * (16 * S + 10), 10), big)
        sheet.save(out)
    print("wrote", len(SPRITES), "sprites and models")


if __name__ == "__main__":
    main()
