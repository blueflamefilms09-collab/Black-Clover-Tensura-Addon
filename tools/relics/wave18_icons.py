#!/usr/bin/env python3
"""
Relics of Many Worlds, wave 18: the 16x16 inventory sprites and flat item models of the seven Bountiful Bags and ten other charms.

    python3 tools/relics/wave18_icons.py [--preview out.png]

Writes under src/main/resources/assets/nusmp/
  textures/item/<id>.png   the sprite: a dark outline, four shades of the main material and three of the trim lit from the upper left, the relic's own
                           extra colours, a few glow pixels, and one outline pixel in the grade's colour (the grade rim)
  models/item/<id>.json    the flat item model (parent item/generated, layer0 = the sprite): charms are worn, so none has a 3D hand mesh
for bag_of_holding, bag_of_devouring, bag_of_colding, bag_of_golding, bag_of_molding, bag_of_scolding, bag_of_unfolding, old_masters_spellbook, tiny_orbit,
the_lovers_divide, coin_of_fortune, defenders_dial, elephant_of_the_room, bag_of_kittens, the_pocket_campsite, mirror_of_spell_reflection, dragon_satchel.

Each sprite follows its card's art: the seven bags share one leather satchel (strap, flap with a zig-zag edge, tassel) and differ by the badge on the flap
(a green mask, a toothed mouth, a snowflake, a coin, mould spots, a speech bubble, a key); the others are drawn as character grids (a letter per shade).
Idempotent: running it again rewrites the same files.
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


# ------------------------------------------------------------------------------------------------ the satchel of the seven bags
SATCHEL = [
    "................",
    ".....dddddd.....",
    "....d......d....",
    "....d......d....",
    "..bbbbbbbbbbbb..",
    ".abbbbbbbbbbbbc.",
    ".abbbbbbbbbbbbc.",
    ".abbbbbbbbbbbbc.",
    ".abbbbbbbbbbbbc.",
    ".aTUTUTUTUTUTUc.",
    ".ab.UT.UT.UT.bc.",
    ".abbbbbbbbbbbbc.",
    ".bbbbbbbbbbbbcc.",
    "..ccccccccccdd..",
    "................",
    "................",
]

# the clasp and the tassel hanging from the lower right, as {(x, y): letter}
TASSEL = {(7, 10): "T", (8, 10): "T", (7, 11): "U", (8, 11): "U", (12, 12): "U", (12, 13): "U", (12, 14): "V", (13, 14): "V", (11, 14): "V"}


def badge(rows, left=5, top=6):
    """Rows of letters placed with their top-left corner at (left, top); '.' leaves the leather."""
    out = {}
    for dy, row in enumerate(rows):
        for dx, ch in enumerate(row):
            if ch != ".":
                out[(left + dx, top + dy)] = ch
    return out


BADGES = {
    # a green mask-like face with dark eyes
    "mask": badge([".xxxx.", "xkxxkx", ".xyyx."]),
    # a toothed mouth with a red tongue
    "mouth": badge(["kkkkkk", "ksksgk", "kkkkkk"]),
    # a snowflake
    "snow": badge(["g.g.g.", ".ggg..", "g.g.g."], left=5),
    # a coin
    "coin": badge(["..TT..", ".TUUT.", "..TT.."]),
    # mould spots
    "mould": badge(["x...yx", "..y...", ".y.x.y"]),
    # a speech bubble with a scolding mouth
    "bubble": badge(["ssssss", "sk..ks", ".sskss"]),
    # a key
    "key": badge(["TTT...", "T.TTTT", "TTT.T."]),
}


def bag(main, trim, glow, motif, extra=None):
    lg = legend(main, trim, glow, x=extra if extra is not None else 0x6FBF5A)
    cv = Canvas()
    cv.grid(SATCHEL, lg)
    for (x, y), ch in list(TASSEL.items()) + list(BADGES[motif].items()):
        cv.put(x, y, lg[ch])
    return cv


# ------------------------------------------------------------------------------------------------ the ten other charms
def old_masters_spellbook():
    """A thick brown book: gold corner caps and spine bands, a red dragon and a gold pentagram on the cover, cream page edges, four ribbons hanging out."""
    lg = legend(0x6B4220, 0xD2A24A, 0xFF6A3A, x=0xC24A2A, w=0xEFE3C2)
    rows = [
        "................",
        "..TUddddddddTU..",
        "..UbbbbbbbbbbbV.",
        "..dbbbbbbbbTbwV.",
        "..dbbxbbbbTUTwV.",
        "..dbxyxbbbbTbwV.",
        "..dTxyyxxbbbbwV.",
        "..dbbxxyxxbbbwV.",
        "..dTbbxxyybbbwV.",
        "..dbbbbbxbbbbwV.",
        "..dbbbbbbbbbbwV.",
        "..UTddddddddTUV.",
        "...wwwwwwwwwww..",
        "...gvuwvwu.vu...",
        "...v.u.v.u..v...",
        "................",
    ]
    cv = Canvas()
    cv.grid(rows, lg)
    for (x, y, c) in [(4, 13, (214, 120, 40)), (6, 13, (130, 70, 160)), (8, 13, (60, 160, 150)), (10, 13, (200, 80, 130)),
                      (4, 14, (214, 120, 40)), (6, 14, (130, 70, 160)), (8, 14, (60, 160, 150)), (10, 14, (200, 80, 130))]:
        cv.px[(x, y)] = c
    return cv


def tiny_orbit():
    """A thin silver chain circlet seen at a slant with a little cratered grey-cream moon orbiting it."""
    lg = legend(0xC8CCD4, 0xEFEFEA, 0xDAD8C8, x=0xD8D2BC, w=0x8E8A82)
    rows = [
        "................",
        "..........xxx...",
        ".........xxyxx..",
        ".........xyxxx..",
        ".........xxxyx..",
        "..........zxz...",
        "................",
        "....bbbbbbbb....",
        "..bba......abb..",
        ".bc..........cb.",
        ".b............b.",
        ".bc..........cb.",
        "..bbd......dbb..",
        "....bbbbbbbb....",
        "................",
        "................",
    ]
    cv = Canvas()
    cv.grid(rows, lg)
    for (x, y) in [(4, 7), (7, 7), (10, 7), (2, 9), (13, 9), (2, 11), (13, 11), (5, 13), (9, 13)]:
        cv.px[(x, y)] = lg["a"]
    cv.px[(3, 5)] = lg["g"]
    cv.px[(6, 3)] = lg["g"]
    return cv


def the_lovers_divide():
    """Two golden-yellow open bangles with orange wavy bands, each in two halves joined by three short dark chains."""
    lg = legend(0xE8B83A, 0xE0663A, 0xFFD27A, x=0x5A5560)
    cv = Canvas()

    def ring(cx, cy, rx, ry):
        pts = []
        for y in range(cy - ry - 1, cy + ry + 2):
            for x in range(cx - rx - 1, cx + rx + 2):
                d = ((x - cx) / (rx + 0.5)) ** 2 + ((y - cy) / (ry + 0.5)) ** 2
                inner = ((x - cx) / (rx - 1.2)) ** 2 + ((y - cy) / (ry - 1.2)) ** 2
                if d <= 1.0 and inner >= 1.0:
                    pts.append((x, y))
        for (x, y) in pts:
            t = (y - (cy - ry)) / max(1, 2 * ry)
            cv.put(x, y, lg["a"] if t < 0.2 else lg["b"] if t < 0.55 else lg["c"] if t < 0.85 else lg["d"])
        # the gap on the right, closed by three dark chain links
        for y in range(cy - 1, cy + 2):
            for x in range(cx + rx - 1, cx + rx + 2):
                cv.px.pop((x, y), None)
        for i, y in enumerate((cy - 1, cy, cy + 1)):
            cv.put(cx + rx - 1, y, lg["x"])
            cv.put(cx + rx, y, lg["y"] if i != 1 else lg["z"])
            cv.put(cx + rx + 1, y, lg["x"])
        # the orange wavy band along the front
        for (x, y) in ((cx - 4, cy + 1), (cx - 3, cy), (cx - 2, cy + 1), (cx - 1, cy + 2), (cx, cy + 1)):
            if (x, y) in cv.px:
                cv.px[(x, y)] = lg["U"]

    ring(6, 4, 5, 3)
    ring(8, 11, 6, 3)
    return cv


def coin_of_fortune():
    """Two scalloped oval coins: a gold-bronze one with green enamel and a clover, a violet one with dark red behind it."""
    lg = legend(0xD8A838, 0x3E8B3A, 0xFFE08A, x=0x7A3A9A, w=0x7A1F2A)
    rows = [
        "................",
        "..........xxxx..",
        ".........xyyyyx.",
        "........xyywwyyx",
        "........xyywvyyx",
        "...bbbb.xyywwyyx",
        "..bTbbTbxyyyyyxz",
        ".bbTUTTbbxxxxxz.",
        ".bTTUTTUb.zzzz..",
        ".bbTUTTbb.......",
        ".bTbbbbTb.......",
        "..bbbbbbc.......",
        "...cccc.........",
        "................",
        "................",
        "................",
    ]
    cv = Canvas()
    cv.grid(rows, lg)
    cv.px[(3, 7)] = lg["a"]
    cv.px[(3, 8)] = lg["a"]
    cv.px[(5, 6)] = lg["g"]
    return cv


def defenders_dial():
    """A coiled brown leather belt with a silver rectangular buckle: a flame, a snowflake and a bolt above a rotary dial with a pointer."""
    lg = legend(0x6A4A2E, 0xC0C4CC, 0xFF8A3A, x=0xE4E8EE)
    rows = [
        "................",
        "....dddddddd....",
        "..ddbbbbbbbbdd..",
        ".dbbbbbbbbbbbbd.",
        ".bbbbbbbbbbbbbc.",
        ".bbTTTTTTTTTTbc.",
        ".bbTrTsTgTzTTbc.",
        ".bbTTTTTTTTTTbc.",
        ".bbTTTTUUTTTTbc.",
        ".bbTTTUxxUTTTbc.",
        ".bbTTTUxUUTTTbc.",
        ".dbTTTTUUTTTVbc.",
        "..dbVVVVVVVVbc..",
        "...dcccccccdc...",
        "................",
        "................",
    ]
    lg["r"] = rgb(0xFF7A2E)
    lg["z"] = rgb(0xFFE95A)
    cv = Canvas()
    cv.grid(rows, lg)
    return cv


def elephant_of_the_room():
    """A small black silhouette elephant figurine facing left: a big ear edged in white, a white swirl on its flank, trunk curled up, a small tusk."""
    lg = legend(0x34343F, 0xF2F2F2, 0xBFBFFF, x=0x5A5A6C)
    rows = [
        "................",
        "..bb............",
        ".bbbb...........",
        ".bb.b...........",
        ".b..b..aaabbb...",
        ".bssbaabbbbbbb..",
        ".bbbbbbbbbbbbbb.",
        "sbbbssbbbbbbbbb.",
        "sbbsbbbsssbbbbb.",
        ".bbsbbsbbsbbbbc.",
        ".bbbsssbssbbbbc.",
        "..bbbbbbbbbbbcc.",
        "..bbd.bbb.dbbc..",
        "..bbd.bbc.dbbc..",
        "..ddd.ddd.ddd...",
        "................",
    ]
    cv = Canvas()
    cv.grid(rows, lg)
    return cv


def bag_of_kittens():
    """A tan satchel with teal-green trim and pink cat-face patches; a cream, a grey and an orange kitten peek out of the top."""
    lg = legend(0xA89870, 0x5AB09A, 0xFFB0C8, x=0xE8DEC0, w=0x8A8E9A)
    rows = [
        "................",
        "..........ww....",
        "...xx.xx.wvvw...",
        "..xxxxxxxwvvw.k.",
        "..xkxxkx.wkvkw..",
        "..xxxxxx.wvvvw..",
        ".TTTTTTTTTTTTTT.",
        ".TbbbbbbbbbbbbT.",
        ".TbbbbbbbbbbbbT.",
        ".TbbbpbbpbbbbbT.",
        ".TbbppbppbbbbbT.",
        ".TbbbbbbbbbbbbT.",
        ".UbbbbbbbbbbbbU.",
        "..UUUUUUUUUUUU..",
        "................",
        "................",
    ]
    lg["p"] = rgb(0xF08AB0)
    cv = Canvas()
    cv.grid(rows, lg)
    for (x, y) in [(11, 4), (10, 3)]:
        pass
    cv.px[(13, 2)] = rgb(0xD8782A)
    cv.px[(13, 3)] = rgb(0xD8782A)
    cv.px[(14, 3)] = rgb(0xB85A1A)
    return cv


def the_pocket_campsite():
    """A small rust-brown leather belt pouch with orange glowing swirl runes, a pink snake-patterned strap with a buckle and a belt loop."""
    lg = legend(0x8A4A2A, 0xD8803A, 0xFF9A3A, x=0xC9788A)
    rows = [
        "................",
        "..xxxxxxxxxxxx..",
        ".xyzzzzzzzzzzyx.",
        ".xz..........zx.",
        "..xzz.xxxx.zz...",
        "...bbbxyyxbbb...",
        "..abbTbxyxbbcb..",
        ".abTgbTbxxTbbbc.",
        ".abgTgbTUbgTbbc.",
        ".abTgbbbxbTgbbc.",
        ".abbbTgbxbbTbbc.",
        ".abbbbbbxbbbbbc.",
        "..bbTgbbxbgTbc..",
        "...ccbbbxbbcc...",
        "....cccccccc....",
        "................",
    ]
    cv = Canvas()
    cv.grid(rows, lg)
    cv.px[(8, 8)] = rgb(0xE8E0C8)
    cv.px[(8, 9)] = rgb(0xE8E0C8)
    return cv


def mirror_of_spell_reflection():
    """An ornate oval hand mirror: a dark purple-grey twisted baroque frame and handle, polished silvery glass."""
    lg = legend(0x4A3A5A, 0xC8D0E0, 0xDCE8FF, x=0xB6C4D8)
    rows = [
        "................",
        "....dddddddd....",
        "...dbaabbaabd...",
        "..dbxxxxxxxxbd..",
        "..bxsxxxxxxxxb..",
        ".dbxxsxxxxxxxxbd",
        ".dbxxxxxyxxxxxbd",
        ".dbxxxxxxxyxxxbd",
        ".dbxxxxxxxxxxxbd",
        "..bxxxxxxxxxyxb.",
        "..dbyxxxxxxxxbd.",
        "...dbbabbabbd...",
        "....ddbccbdd....",
        ".....dbbcd......",
        "......dbd.......",
        "......ddd.......",
    ]
    lg["y"] = lg["y"]
    cv = Canvas()
    cv.grid(rows, lg)
    return cv


def dragon_satchel():
    """A small reddish-brown leather satchel whose flap is a golden dragon head with swept horns and amber eyes, a twisted red rope strap and a brass ring."""
    lg = legend(0x7A3A2A, 0xD8A838, 0xFFC060, x=0xA8222A)
    rows = [
        "................",
        "..T..T.T..T.....",
        "..TT.TUT.TT..xx.",
        "...TTTUTTT..x..x",
        "...TUTTTUT..x..x",
        "..bTgTTTgTb.xxx.",
        ".abTTTTTTTbbx.x.",
        ".abbTTUUTTbbbx..",
        ".abbbTTTTbbbbc..",
        ".abbbbTUbbbbbc..",
        ".abbbbbbbbbbbc..",
        ".abbbbbbbbbbbc..",
        ".bbbbbbbbbbbcc..",
        "..cccccccccdd...",
        "................",
        "................",
    ]
    cv = Canvas()
    cv.grid(rows, lg)
    cv.px[(5, 5)] = rgb(0xFFB020)
    cv.px[(8, 5)] = rgb(0xFFB020)
    cv.px[(13, 4)] = rgb(0xE8D878)
    return cv


SPRITES = [
    ("bag_of_holding", "SPECIAL", lambda: bag(0x7A4B2A, 0x2FA39A, 0xE05A4A, "mask")),
    ("bag_of_devouring", "UNIQUE", lambda: bag(0x4A3A30, 0x8B2A2A, 0xFF4A4A, "mouth")),
    ("bag_of_colding", "RARE", lambda: bag(0x5A5F70, 0x8FD8FF, 0xBFEFFF, "snow")),
    ("bag_of_golding", "RARE", lambda: bag(0x7A5A2A, 0xE6B94A, 0xFFE08A, "coin")),
    ("bag_of_molding", "RARE", lambda: bag(0x4C5A34, 0x8AA64A, 0xB8E060, "mould", extra=0x6FBF5A)),
    ("bag_of_scolding", "RARE", lambda: bag(0x6A3A3A, 0xE0A040, 0xFFC070, "bubble")),
    ("bag_of_unfolding", "RARE", lambda: bag(0x5A4A7A, 0xC8C0E0, 0xD8B8FF, "key")),
    ("old_masters_spellbook", "UNIQUE", old_masters_spellbook),
    ("tiny_orbit", "SPECIAL", tiny_orbit),
    ("the_lovers_divide", "RARE", the_lovers_divide),
    ("coin_of_fortune", "SPECIAL", coin_of_fortune),
    ("defenders_dial", "RARE", defenders_dial),
    ("elephant_of_the_room", "RARE", elephant_of_the_room),
    ("bag_of_kittens", "UNIQUE", bag_of_kittens),
    ("the_pocket_campsite", "RARE", the_pocket_campsite),
    ("mirror_of_spell_reflection", "UNIQUE", mirror_of_spell_reflection),
    ("dragon_satchel", "RARE", dragon_satchel),
]

GLOW_SPOTS = {
    "bag_of_holding": [(2, 4), (13, 5)], "bag_of_devouring": [(1, 5), (14, 8)], "bag_of_colding": [(1, 4), (14, 6)],
    "bag_of_golding": [(1, 6), (14, 4)], "bag_of_molding": [(14, 6), (1, 9)], "bag_of_scolding": [(1, 5), (14, 9)],
    "bag_of_unfolding": [(1, 4), (14, 7)], "old_masters_spellbook": [(1, 3), (15, 8)], "tiny_orbit": [(14, 1), (1, 6)],
    "the_lovers_divide": [(14, 6), (1, 1)], "coin_of_fortune": [(1, 3), (9, 1)], "defenders_dial": [(1, 2), (14, 2)],
    "elephant_of_the_room": [(1, 2), (14, 13)], "bag_of_kittens": [(0, 2), (15, 11)], "the_pocket_campsite": [(0, 6), (15, 6)],
    "mirror_of_spell_reflection": [(1, 1), (14, 1)], "dragon_satchel": [(0, 3), (15, 12)],
}


def build(preview=None):
    os.makedirs(ICONS, exist_ok=True)
    os.makedirs(MODELS, exist_ok=True)
    ims = []
    for rid, grade, make in SPRITES:
        cv = make()
        cv.outline()
        cv.sparkles(rgb({"bag_of_holding": 0xE05A4A}.get(rid, 0xFFE9A0)), GLOW_SPOTS.get(rid, []))
        cv.grade_rim(GRADE_RGB[grade])
        ims.append(cv.save(os.path.join(ICONS, rid + ".png")))
        with open(os.path.join(MODELS, rid + ".json"), "w", encoding="utf-8") as f:
            json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/" + rid}}, f, indent=2)
            f.write("\n")
    if preview:
        scale, cols = 10, 6
        rows = (len(ims) + cols - 1) // cols
        sheet = Image.new("RGBA", (cols * 16 * scale + (cols + 1) * 8, rows * 16 * scale + (rows + 1) * 8), (70, 74, 86, 255))
        for i, im in enumerate(ims):
            big = im.resize((16 * scale, 16 * scale), Image.NEAREST)
            sheet.alpha_composite(big, (8 + (i % cols) * (16 * scale + 8), 8 + (i // cols) * (16 * scale + 8)))
        sheet.save(preview)


if __name__ == "__main__":
    out = None
    if "--preview" in sys.argv:
        out = sys.argv[sys.argv.index("--preview") + 1]
    build(out)
    print("wrote %d sprites" % len(SPRITES))
