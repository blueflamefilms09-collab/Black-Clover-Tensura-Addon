#!/usr/bin/env python3
"""Hero weapons (1.62.0), acquisition: the 16x16 icons and the item models of the nineteen Calling Gem items.

    python3 -B tools/heroweapon/gen_gem_icons.py [--sheet PATH]        (idempotent)

Writes, per item id (the list is derived from the weapon catalogue in HeroKind.java, never typed here),
  assets/nusmp/textures/item/<id>.png     the 16 x 16 sprite
  assets/nusmp/models/item/<id>.json      the flat inventory model (minecraft:item/generated), like the Djinn Seals
The gems are small items, not weapons, so they have no 3D mesh. The look, in the owner's icon method (tools/relics/wave1_icons.py: primitives on a
16 x 16 canvas, a one pixel dark outline, light from the upper left):
  calling_gem          the Holy Calling Gem: a cut jewel in the four colours of the four Holy Weapons (green Shield, blue Sword, red Spear, yellow Bow)
  gem_fragment         a shard of that jewel: amethyst violet with a gold chip, the thing every Seal is made from
  vassal_seal_<kind>   a rune disc, dark inside with a bright rim: amber for the seven Seven Star Weapons, aqua for the ten other Vassal Weapons, and in the
                       middle a small pictogram of the weapon in cream, so that the seventeen can be told apart in a chest
The colours are read from GemCatalog.java so the icons and the effects always agree. With --sheet an enlarged contact sheet is written for looking at them.
"""
import json
import math
import os
import re
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "relics"))
from wave1_icons import Canvas, rgb, shade, mix, ICONS, MODELS, ROOT, S   # noqa: E402

JAVA_DIR = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "heroweapon")
HOLY_ID = "calling_gem"
FRAGMENT_ID = "gem_fragment"
SEAL_PREFIX = "vassal_seal_"

CREAM = (255, 246, 222)       # the pictogram: the bright pixels
CREAM_MID = (214, 198, 158)   # its shaded pixels
DISC_DARK = (22, 18, 40)      # inside of a seal disc, at the rim
DISC_LIGHT = (50, 40, 84)     # inside of a seal disc, in the middle
OUTLINE = (14, 10, 26)


# ---------------------------------------------------------------- what the catalogue says
def read_colours():
    """The jewel colours of GemCatalog.java as {NAME: (r, g, b)}."""
    text = open(os.path.join(JAVA_DIR, "gem", "GemCatalog.java"), encoding="utf-8").read()
    out = {}
    for name, hexa in re.findall(r'public static final int ([A-Z_]+) = 0x([0-9A-Fa-f]{6});', text):
        out[name] = rgb(int(hexa, 16))
    for need in ("SHIELD_GREEN", "SWORD_BLUE", "SPEAR_RED", "BOW_YELLOW", "HOLY_WHITE", "SEVEN_STAR_AMBER", "VASSAL_AQUA"):
        if need not in out:
            sys.exit("GemCatalog.java does not define %s" % need)
    return out


def read_seal_kinds():
    """[(ENUM_NAME, class name)] of the weapons that have a Seal (not Holy), in catalogue order, from HeroKind.java."""
    text = open(os.path.join(JAVA_DIR, "HeroKind.java"), encoding="utf-8").read()
    kinds = re.findall(r'^\s*([A-Z_]+)\("[a-z_]+",\s*"[^"]+",\s*HeroClass\.([A-Z_]+),', text, re.M)
    if len(kinds) < 21:
        sys.exit("could not read the weapon catalogue from HeroKind.java (found %d)" % len(kinds))
    return [(n, c) for n, c in kinds if c != "HOLY"]


# ---------------------------------------------------------------- the Holy Calling Gem
# The cut of the jewel: the half width in pixels of each row (the row runs from x = 8 - hw to x = 7 + hw, so it is symmetric about the middle of the canvas),
# a table on top, the girdle at y = 5, then the pavilion narrowing to the point.
GEM_ROWS = {2: 3, 3: 5, 4: 6, 5: 7, 6: 6, 7: 5, 8: 4, 9: 4, 10: 3, 11: 2, 12: 1}
GIRDLE = 5
WHITE = (255, 255, 255)


def holy_gem(col):
    cv = Canvas()
    quad = {(0, 0): col["SHIELD_GREEN"], (1, 0): col["SWORD_BLUE"], (0, 1): col["SPEAR_RED"], (1, 1): col["BOW_YELLOW"]}
    for y, hw in GEM_ROWS.items():
        for x in range(8 - hw, 8 + hw):
            base = quad[(0 if x < 8 else 1, 0 if y <= GIRDLE else 1)]
            if y <= GIRDLE:                                           # the crown: the table brightest, the left facet light, the right facet dark
                if y == 2:
                    k = 1.28
                elif x < 8 - hw + 2:
                    k = 1.14
                elif x >= 8 + hw - 2:
                    k = 0.78
                else:
                    k = 1.0
            else:                                                     # the pavilion: three facets running down to the point
                left, right = 4 + (y - 6) * 0.45, 11 - (y - 6) * 0.45
                k = 1.16 if x < left else (0.74 if x > right else 0.96)
            cv.put(x, y, shade(base, k))
    for y in range(2, 13):                                            # the seam where the four colours meet
        for x, amount in ((7, 0.42), (8, 0.3)):
            if (x, y) in cv.px:
                cv.put(x, y, mix(cv.px[(x, y)], WHITE, amount))
    for x in range(1, 15):                                            # the girdle
        if (x, GIRDLE) in cv.px:
            cv.put(x, GIRDLE, mix(cv.px[(x, GIRDLE)], WHITE, 0.32))
    for (x, y) in ((4, 3), (3, 4), (4, 4), (5, 4), (4, 5)):          # a sparkle on the upper left, a glint on the lower right
        cv.put(x, y, WHITE)
    cv.put(11, 8, WHITE)
    cv.outline(OUTLINE)
    return cv


# ---------------------------------------------------------------- the Gem Fragment
def fragment():
    cv = Canvas()
    violet, light, dark, gold = (170, 118, 220), (226, 196, 255), (92, 58, 150), (240, 200, 92)
    big = {1: (7, 8), 2: (6, 9), 3: (5, 10), 4: (5, 10), 5: (5, 10), 6: (5, 10), 7: (6, 9), 8: (6, 9)}       # the crystal: x range per row
    chip = {5: (2, 3), 6: (1, 3), 7: (1, 3), 8: (2, 3)}                                                     # a smaller one beside it
    for shard in (chip, big):
        for y, (x0, x1) in shard.items():
            for x in range(x0, x1 + 1):
                if x == x0 and x1 - x0 >= 1:
                    c = light
                elif x == x1 and x1 - x0 >= 2:
                    c = dark
                else:
                    c = violet
                cv.put(x, y, c)
    for (x, y) in ((7, 1), (6, 2), (5, 3), (5, 4)):                   # the lit edge
        cv.put(x, y, (255, 250, 255))
    for y in range(3, 7):                                             # a lighter facet down the middle
        cv.put(8, y, mix(violet, light, 0.45))
    for (x, y) in ((9, 2), (11, 3)):                                  # a sparkle to the upper right
        cv.put(x, y, WHITE)
    for (x, y) in ((10, 3), (12, 3), (11, 2), (11, 4)):
        cv.put(x, y, (230, 210, 255))
    for x in range(5, 11):                                            # the gold collar it was set in
        cv.put(x, 9, gold if x % 3 else (255, 232, 150))
    for x in range(6, 10):
        cv.put(x, 10, shade(gold, 0.72))
    for x in (1, 2, 3):
        cv.put(x, 9, shade(gold, 0.85))
    cv.outline(OUTLINE)
    return cv


# ---------------------------------------------------------------- the pictograms (8 x 8; '#' bright, '+' shaded, 'o' the tint, '.' empty)
GLYPHS = {
    "HAMMER": [
        ".######.",
        "########",
        ".++++++.",
        "...##...",
        "...##...",
        "...##...",
        "...##...",
        "...++...",
    ],
    "CLAW": [                       # three slashes of a beast
        "...##...",
        ".#..##..",
        "..#..#..",
        "##.#..#.",
        ".##.#..#",
        "..#..#..",
        "...#..#.",
        "....#...",
    ],
    "AXE": [                        # a double-bladed battle axe
        "...##...",
        ".#.##.#.",
        "########",
        "########",
        ".#.##.#.",
        "...##...",
        "...##...",
        "...++...",
    ],
    "WHIP": [                       # the lash in an S and the handle
        "....###.",
        "...#...#",
        "...#....",
        "....#...",
        "...#....",
        "..#.....",
        ".###....",
        "##+.....",
    ],
    "STAFF": [                      # a gem in a claw on a pole
        "...oo...",
        "..oooo..",
        ".#oooo#.",
        "..#oo#..",
        "...##...",
        "...##...",
        "...##...",
        "...++...",
    ],
    "GAUNTLET": [                   # an armoured fist
        "#.#.#.#.",
        "########",
        "########",
        "#######.",
        ".######.",
        "..####..",
        ".######.",
        ".++++++.",
    ],
    "PROJECTILE": [                 # a throwing star with a hole in it
        "...##...",
        "...##...",
        "..####..",
        "###..###",
        "###..###",
        "..####..",
        "...##...",
        "...##...",
    ],
    "CARRIAGE": [                   # a coach wheel with the gem at its hub
        "..####..",
        ".#.##.#.",
        "#..##..#",
        "###oo###",
        "###oo###",
        "#..##..#",
        ".#.##.#.",
        "..####..",
    ],
    "FAN": [                        # a folding fan, scalloped, its ribs closing on the pivot
        ".######.",
        "########",
        "#.#..#.#",
        ".#.##.#.",
        "..#..#..",
        "..####..",
        "...##...",
        "...++...",
    ],
    "SCYTHE": [                     # the sickle blade at the head of a long pole
        "..#####.",
        ".####.#.",
        "###...#.",
        "##....#.",
        "#.....#.",
        "......#.",
        "......#.",
        "......+.",
    ],
    "KATANA": [                     # a long blade with its guard and the wrapped hilt
        "......##",
        ".....##.",
        "....##..",
        "...##...",
        ".+.#....",
        "..+.....",
        ".#+.....",
        "#+......",
    ],
    "BOOK": [                       # a book with its spine on the left and a gem on the cover
        ".#######",
        "++#####+",
        "++#oo##+",
        "++#oo##+",
        "++#####+",
        "++#####+",
        ".#######",
        "........",
    ],
    "MIRROR": [                     # a hand mirror: a bright frame, dark glass with a streak of light
        "..####..",
        ".#gggg#.",
        "#gg##gg#",
        "#g##ggg#",
        "#gggggg#",
        ".#gggg#.",
        "..####..",
        "...##...",
    ],
    "INSTRUMENT": [                 # a musical note
        "....###.",
        "....#..#",
        "....#...",
        "....#...",
        "....#...",
        "..###...",
        ".####...",
        "..##....",
    ],
    "HARPOON": [                    # a barbed head on a shaft, the rope coiled at its foot
        "...##...",
        "..####..",
        ".######.",
        "#..##..#",
        "...##...",
        "+..##...",
        "+++##...",
        "..+##...",
    ],
    "SEWING_SET": [                 # a spool of thread
        ".######.",
        ".######.",
        "..+#+#..",
        "..#+#+..",
        "..+#+#..",
        "..#+#+.+",
        ".######+",
        ".######.",
    ],
    "CHAIN": [                      # two linked rings
        ".###....",
        "#...#...",
        "#...#...",
        "#...###.",
        ".###...#",
        "...#...#",
        "...#...#",
        "....###.",
    ],
}


def blit_glyph(cv, rows, tint):
    """Draws an 8 x 8 pictogram in the middle of the canvas: '#' cream, '+' shaded cream, 'o' a light tone of the tint (a gem), 'g' a dark tone of it (glass)."""
    h, w = len(rows), len(rows[0])
    x0, y0 = 8 - w // 2, 8 - h // 2
    light = mix(tint, WHITE, 0.55)
    glass = shade(tint, 0.62)
    for j, row in enumerate(rows):
        for i, ch in enumerate(row):
            if ch == "#":
                cv.put(x0 + i, y0 + j, CREAM)
            elif ch == "+":
                cv.put(x0 + i, y0 + j, CREAM_MID)
            elif ch == "o":
                cv.put(x0 + i, y0 + j, light)
            elif ch == "g":
                cv.put(x0 + i, y0 + j, glass)


# ---------------------------------------------------------------- a Vassal Seal
def seal(name, klass, col):
    tint = col["SEVEN_STAR_AMBER"] if klass == "SEVEN_STAR" else col["VASSAL_AQUA"]
    cv = Canvas()
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, y - cy
            d = math.hypot(dx, dy)
            if d > 7.1:
                continue
            if d >= 5.7:                                                                    # the rim: lit from the upper left
                lit = (-dx - dy) / (max(d, 0.001) * 1.4142)
                cv.put(x, y, shade(tint, 0.86 + 0.34 * lit))
            else:                                                                           # the dark rune plate
                cv.put(x, y, mix(DISC_LIGHT, DISC_DARK, min(1.0, d / 5.7) ** 1.6))
    # studs on the rim tell the families apart without colour: four for the Seven Star Weapons, two for the others
    studs = ((7, 1), (8, 1), (7, 14), (8, 14), (1, 7), (1, 8), (14, 7), (14, 8)) if klass == "SEVEN_STAR" else ((7, 1), (8, 1), (7, 14), (8, 14))
    for (x, y) in studs:
        cv.put(x, y, mix(tint, (255, 255, 255), 0.7))
    blit_glyph(cv, GLYPHS[name], tint)
    cv.outline(OUTLINE)
    return cv


# ---------------------------------------------------------------- files
def model_json(item_id):
    return {"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/" + item_id}}


def write_all():
    col = read_colours()
    items = [(HOLY_ID, holy_gem(col)), (FRAGMENT_ID, fragment())]
    for name, klass in read_seal_kinds():
        if name not in GLYPHS:
            sys.exit("no pictogram for %s: add one to GLYPHS" % name)
        items.append((SEAL_PREFIX + name.lower(), seal(name, klass, col)))
    os.makedirs(ICONS, exist_ok=True)
    os.makedirs(MODELS, exist_ok=True)
    for item_id, cv in items:
        cv.image().save(os.path.join(ICONS, item_id + ".png"))
        with open(os.path.join(MODELS, item_id + ".json"), "w", encoding="utf-8", newline="\n") as f:
            f.write(json.dumps(model_json(item_id), indent=2) + "\n")
    return items


def contact_sheet(items, path, scale=10):
    cols = 7
    rows = (len(items) + cols - 1) // cols
    pad = 6
    cell = 16 * scale + pad
    sheet = Image.new("RGBA", (cols * cell + pad, rows * cell + pad), (58, 58, 64, 255))
    for i, (item_id, cv) in enumerate(items):
        im = cv.image().resize((16 * scale, 16 * scale), Image.NEAREST)
        sheet.alpha_composite(im, (pad + (i % cols) * cell, pad + (i // cols) * cell))
    sheet.save(path)


def main(argv):
    items = write_all()
    print("wrote %d icons and models" % len(items))
    if "--sheet" in argv:
        contact_sheet(items, argv[argv.index("--sheet") + 1])
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
