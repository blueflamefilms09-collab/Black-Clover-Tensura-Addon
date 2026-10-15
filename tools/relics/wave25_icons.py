#!/usr/bin/env python3
"""
Relics of Many Worlds part 4, wave 25: the 16x16 inventory sprites and flat item models of twelve relics.

    python3 tools/relics/wave25_icons.py [--preview out.png]

Writes under src/main/resources/assets/nusmp/
  textures/item/<id>.png   the sprite: a dark outline, four shades of the main material and three of the trim lit from the upper left, the relic's own
                           extra colours, a few glow pixels, and one outline pixel in the grade's colour (the grade rim)
  models/item/<id>.json    the flat item model (parent item/generated, layer0 = the sprite): none of the twelve is a hand weapon, so none has a 3D mesh
for ring_of_rock_eating, ring_of_the_game, ring_of_the_mimic, rose_petal, silver_tongued_lyre, soup_spoon_of_improbability, spirit_lantern,
the_forgotten_ring, witchs_ring, bucket_of_disguise, corpse_mask, death_knights_helmet.

Each sprite follows its card's art (copper band with green crystals, plain black band with white runes, a ring with an eye, a thorned halberd, a horned
devil lyre, a fork, a skull lantern with a teal shroud, a silver band with three fangs, a purple dome with a pink spider, a banded bucket, a cracked
mask with purple swirls, a horned skull helm). Idempotent: running it again rewrites the same files.
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
        for dy, row in enumerate(rows):
            assert len(row) == 16, (dy, row, len(row))
            for x, ch in enumerate(row):
                if ch not in ". ":
                    self.put(x, dy, legend[ch])

    def outline(self):
        cells = set(self.px)
        for (x, y) in cells:
            for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                q = (x + dx, y + dy)
                if q not in cells and 0 <= q[0] < 16 and 0 <= q[1] < 16:
                    self.px[q] = OUTLINE

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


def band(cv, lg, cx=8, cy=10, rx=6, ry=4, left=None, right=None):
    """An elliptical ring seen at a slant, two pixels thick, shaded light at the top, dark at the bottom. left/right override the colour of each half."""
    for y in range(cy - ry - 1, cy + ry + 2):
        for x in range(cx - rx - 1, cx + rx + 2):
            d = ((x - cx) / (rx + 0.5)) ** 2 + ((y - cy) / (ry + 0.5)) ** 2
            inner = ((x - cx) / (rx - 1.3)) ** 2 + ((y - cy) / (ry - 1.3)) ** 2
            if d <= 1.0 and inner >= 1.0:
                t = (y - (cy - ry)) / max(1, 2 * ry)
                shade = "a" if t < 0.2 else "b" if t < 0.55 else "c" if t < 0.85 else "d"
                if left and x < cx - 1:
                    cv.put(x, y, lg[left[("a", "b", "c", "d").index(shade)]])
                elif right and x > cx + 1:
                    cv.put(x, y, lg[right[("a", "b", "c", "d").index(shade)]])
                else:
                    cv.put(x, y, lg[shade])


def line(cv, c, x0, y0, x1, y1):
    n = max(abs(x1 - x0), abs(y1 - y0))
    for i in range(n + 1):
        t = i / n if n else 0
        cv.put(round(x0 + (x1 - x0) * t), round(y0 + (y1 - y0) * t), c)


# ------------------------------------------------------------------------------------------------ the twelve
def ring_of_rock_eating():
    """A copper band with olive inlay and a cluster of green crystal prisms rising from the top."""
    lg = legend(0xC87850, 0x7C8C3A, 0xA6D44A, x=0x9AB83A)
    cv = Canvas()
    band(cv, lg, cx=7, cy=11, rx=6, ry=3)
    cv.grid([
        "................",
        ".......x........",
        "......xyx.......",
        "..x...xzyx......",
        ".xyx..xyzyx.x...",
        ".xyzx.xzyzxxyx..",
        "..xyzxxyzyxyzx..",
        "...xyzyzyzyzx...",
        "....xxyzyzxx....",
        "......xxxx......",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ], lg)
    for (x, y) in ((5, 12), (7, 13), (9, 12)):
        cv.put(x, y, lg["g"])
    return cv


def ring_of_the_game():
    """A plain black iron band with a few glowing white runes along the front."""
    lg = legend(0x1E1E26, 0xE8E8F0, 0xFFFFFF)
    cv = Canvas()
    band(cv, lg, cx=8, cy=8, rx=6, ry=4)
    for (x, y) in ((5, 11), (7, 12), (9, 12), (11, 11), (6, 12), (10, 11)):
        cv.put(x, y, lg["s"])
    cv.put(4, 5, lg["T"]); cv.put(5, 4, lg["T"]); cv.put(6, 4, lg["T"])
    return cv


def ring_of_the_mimic():
    """A band that is fur on the left and green scales on the right, with a big slit-pupil eye and two small claws on top."""
    lg = legend(0xB8963A, 0xD8D8D0, 0xE0F050, x=0x3AAE5A, w=0xE8E05A)
    cv = Canvas()
    band(cv, lg, cx=8, cy=11, rx=6, ry=3, right=("x", "y", "z", "z"))
    cv.grid([
        "................",
        "................",
        "......wwww......",
        ".....wwvvww.....",
        "....TwwkvwwT....",
        "....T.wkvww.T...",
        ".....TwwvwT.....",
        "......TTTT......",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ], lg)
    cv.put(4, 6, lg["s"]); cv.put(11, 6, lg["s"])
    return cv


def rose_petal():
    """A slender steel-blue halberd on the slant: a curved blade at the upper left, a rose bud below it, green vines along the shaft, a spiked butt."""
    lg = legend(0x7A94AA, 0x2FBF8A, 0xE8F6F0, x=0xC0D4E4, w=0xD24A66)
    cv = Canvas()
    line(cv, lg["c"], 5, 5, 13, 13)
    line(cv, lg["b"], 6, 5, 14, 13)
    cv.grid([
        "................",
        "..x.............",
        ".xbx............",
        ".xbbx.T.........",
        "..xbbxT.........",
        "..xxbbwv........",
        "...xxbvw........",
        "....xT..........",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ], lg)
    for (x, y) in ((9, 8), (10, 10), (12, 11), (13, 14), (14, 15)):
        cv.put(x, y, lg["T"])
    cv.put(11, 9, lg["U"]); cv.put(8, 7, lg["U"])
    cv.put(2, 2, lg["g"]); cv.put(1, 6, lg["g"])
    return cv


def silver_tongued_lyre():
    """Two curved bone horns as arms, thin strings between them, a red-rimmed devil-face sound box with a silver forked tongue."""
    lg = legend(0xB8A27E, 0xA8B0BC, 0xC03A3A, x=0x9A2A2A, w=0x8E7A58)
    cv = Canvas()
    cv.grid([
        "................",
        "..a..........a..",
        "..ab........ba..",
        "..ab........bc..",
        "..abTs.s.s.sTbc.",
        "..abT.s.s.s.Tbc.",
        "..bcT.s.s.s.Tcc.",
        "...cTTsTsTsTTc..",
        "...xwwwwwwwwwx..",
        "..xwkkwwwwkkwwx.",
        "..xwkgwwwwgkwwx.",
        "..xwwwwwwwwwwwx.",
        "...xwsswwsswwx..",
        "....xwwwwwwx....",
        ".....xxTUTxx....",
        ".......T.T......",
    ], lg)
    return cv


def soup_spoon_of_improbability():
    """A silver four-tined fork on the slant: a fluted handle, a carved pommel, a small flourish where the tines join."""
    lg = legend(0xB8BCC4, 0x6A6A70, 0xFFE070)
    cv = Canvas()
    line(cv, lg["b"], 3, 13, 9, 7)
    line(cv, lg["a"], 4, 13, 10, 7)
    line(cv, lg["c"], 3, 12, 8, 7)
    for (x0, y0, x1, y1) in ((9, 6, 11, 1), (10, 6, 13, 2), (11, 7, 14, 4), (12, 8, 15, 6)):
        line(cv, lg["a"], x0, y0, x1, y1)
    cv.grid([
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "..........TT....",
        ".........TUUT...",
        "................",
        "................",
        "................",
        "................",
        "................",
        "..TTT...........",
        ".TUUUT..........",
        "..TTT...........",
    ], lg)
    cv.put(2, 14, lg["g"]); cv.put(12, 3, lg["g"])
    return cv


def spirit_lantern():
    """A dark iron hexagonal lantern with tiny skulls, a ring hook on top, a glowing cold blue core and a teal shroud draped over the right side."""
    lg = legend(0x2A3040, 0x3AA0B0, 0x7FE8FF, x=0x3AA0B0, w=0xBFF4FF)
    cv = Canvas()
    cv.grid([
        "......kk........",
        ".....k..k.......",
        ".....k..k.......",
        "......kk........",
        ".....dbbd.xx....",
        "....dbbbbdxyx...",
        "....bwwwwbxyyx..",
        "....bwgswbxyxy..",
        "....bwgswbyxyz..",
        "....bwwwwbxyxz..",
        "....dbbbbdyxz...",
        ".....dTTdTzz....",
        "......TTTT.z....",
        "......T..T......",
        ".....T....T.....",
        "................",
    ], lg)
    return cv


def the_forgotten_ring():
    """A smooth silver band with three cream fang-like claws bound by red cord on top and small red diamonds inside."""
    lg = legend(0xC0C4CA, 0xEDE0BF, 0xE04040, x=0xD02A2A)
    cv = Canvas()
    band(cv, lg, cx=8, cy=11, rx=6, ry=3)
    cv.grid([
        "................",
        ".......T........",
        "......TUT.......",
        "..T...TUUT..T...",
        ".TUT..TUUT.TUT..",
        ".TxUT.TUUTxTUT..",
        "..xT..TUUTx.T...",
        "...x..TUVT.x....",
        ".......TV.......",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ], lg)
    cv.put(7, 13, lg["x"]); cv.put(9, 13, lg["x"])
    return cv


def witchs_ring():
    """A dark grey-black band with clawed prongs holding a large purple glass dome with a glowing pink spider and small pale spiders on the band."""
    lg = legend(0x34303C, 0xB04AD0, 0xFF4AB8, x=0x7A3AC8, w=0xFFA0D8)
    cv = Canvas()
    band(cv, lg, cx=8, cy=11, rx=6, ry=3)
    cv.grid([
        "................",
        "................",
        "......xxxx......",
        "....xxyyyyxx....",
        "...xyyyywyyyx...",
        "...xyywwgwwyyx..",
        "..xyyyywgwyyyx..",
        "..xyywwgggwwyx..",
        "...xyywywywyx...",
        "...zxyyyyyyxz...",
        "....zxxxxxxz....",
        "................",
        "................",
        "................",
        "................",
        "................",
    ], lg)
    cv.put(3, 8, lg["d"]); cv.put(12, 8, lg["d"]); cv.put(8, 2, lg["d"])
    for (x, y) in ((13, 11), (11, 13), (14, 12)):
        cv.put(x, y, lg["w"])
    return cv


def bucket_of_disguise():
    """A wooden barrel-like bucket with two black bands and two round dark eye holes."""
    lg = legend(0xC08A4A, 0x20243A, 0xE0B070)
    cv = Canvas()
    cv.grid([
        "................",
        "................",
        "...bbbbbbbbbb...",
        "..abbbbbbbbbbc..",
        "..abbbbbbbbbbc..",
        "..UUUUUUUUUUUU..",
        "..abbbbbbbbbbc..",
        "..abkkbbbbkkbc..",
        "..abkkbbbbkkbc..",
        "..abbbbbbbbbbc..",
        "..UUUUUUUUUUUU..",
        "..abbbbbbbbbbc..",
        "..cbbbbbbbbbcc..",
        "...ccccccccdd...",
        "................",
        "................",
    ], lg)
    cv.put(3, 3, lg["g"])
    return cv


def corpse_mask():
    """A cracked grey stone face mask with purple spiral swirls around the eye holes, dark red lips and two black ribbon ties."""
    lg = legend(0x8E8A92, 0x7A4AA8, 0xA060D0, x=0x6A2A3A, w=0x2A2438)
    cv = Canvas()
    cv.grid([
        "................",
        "....bbbbbbb.....",
        "..vTbbbbbbbbb...",
        "v.TTbbbbbbbbTT..",
        "vv.bbssbbbssbTv.",
        ".vvbbsksbbsksbvv",
        "..vbbssbbbssbbv.",
        "...bbbbcbbbbbb..",
        "....bbbcbbbbb...",
        "....bbxxxxbb....",
        ".....bbxxbb.....",
        "......bbbc......",
        ".......ccc......",
        "................",
        "................",
        "................",
    ], lg)
    cv.put(8, 4, lg["c"]); cv.put(9, 5, lg["d"]); cv.put(6, 3, lg["T"]); cv.put(11, 3, lg["T"])
    return cv


def death_knights_helmet():
    """A steel-blue horned skull helm with a fanged jaw guard, hollow eye sockets and a purple gem on the brow."""
    lg = legend(0x4A6A90, 0xA0B0C8, 0xC050E0, x=0x9A3AA8)
    cv = Canvas()
    cv.grid([
        "................",
        "..a..........a..",
        "..ab........ba..",
        ".abb..bxxb..bbc.",
        ".abb.bbxyxbb.bc.",
        "..abbbbbxbbbbc..",
        "..abbbbbbbbbbc..",
        "..abkkkbbkkkbc..",
        "..abkkkbbkkkbc..",
        "...bbkkbbkkbbc..",
        "...bbbbkkbbbbc..",
        "...bTsTsTsTsbc..",
        "....bTsTsTsTb...",
        ".....bbkkkbb....",
        "......cb..bc....",
        "................",
    ], lg)
    return cv


ICON_DEFS = {
    "ring_of_rock_eating": (ring_of_rock_eating, "RARE"),
    "ring_of_the_game": (ring_of_the_game, "RARE"),
    "ring_of_the_mimic": (ring_of_the_mimic, "SPECIAL"),
    "rose_petal": (rose_petal, "GOD"),
    "silver_tongued_lyre": (silver_tongued_lyre, "RARE"),
    "soup_spoon_of_improbability": (soup_spoon_of_improbability, "SPECIAL"),
    "spirit_lantern": (spirit_lantern, "SPECIAL"),
    "the_forgotten_ring": (the_forgotten_ring, "LEGEND"),
    "witchs_ring": (witchs_ring, "SPECIAL"),
    "bucket_of_disguise": (bucket_of_disguise, "SPECIAL"),
    "corpse_mask": (corpse_mask, "RARE"),
    "death_knights_helmet": (death_knights_helmet, "UNIQUE"),
}


def main():
    os.makedirs(ICONS, exist_ok=True)
    os.makedirs(MODELS, exist_ok=True)
    made = []
    for rid, (fn, grade) in ICON_DEFS.items():
        cv = fn()
        cv.outline()
        cv.grade_rim(GRADE_RGB[grade])
        im = cv.save(os.path.join(ICONS, rid + ".png"))
        made.append((rid, im))
        with open(os.path.join(MODELS, rid + ".json"), "w") as f:
            json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/" + rid}}, f, indent=2)
            f.write("\n")
    if "--preview" in sys.argv:
        out = sys.argv[sys.argv.index("--preview") + 1]
        scale = 12
        sheet = Image.new("RGBA", (4 * 16 * scale, 3 * 16 * scale), (70, 70, 80, 255))
        for i, (rid, im) in enumerate(made):
            big = im.resize((16 * scale, 16 * scale), Image.NEAREST)
            sheet.alpha_composite(big, ((i % 4) * 16 * scale, (i // 4) * 16 * scale))
        sheet.save(out)
    print("wrote", len(made), "icons")


if __name__ == "__main__":
    main()
