#!/usr/bin/env python3
"""Relics of Many Worlds, part 4, wave 23: the 16x16 inventory icons and item models of the twelve relics.

    python3 -B tools/relics/wave23_icons.py [id ...] [--sheet PATH]       (idempotent)

Writes, per id,
  assets/nusmp/textures/item/<id>.png        the sprite (the inventory icon; the nine hand-held relics are 3D in hand)
  assets/nusmp/models/item/<id>.json         MAIN_HAND relics: builtin/entity (drawn by client/WeaponRenderer from weapon_meshes/<id>.txt), the display
                                             transforms of the existing diagonal / upright weapons; the three charms: item/generated
  assets/nusmp/models/item/<id>_icon.json    MAIN_HAND relics: the flat inventory model of the sprite
Every icon is drawn from primitives on a 16 x 16 canvas, then given a one pixel dark outline; the grade shows as one coloured pixel and the glow as
bright pixels at gems, runes and tips. With --sheet an enlarged contact sheet is written for looking at them.
"""
import json
import math
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from wave1_icons import Canvas, rgb, shade, mix, GRADE, ICONS, MODELS, S, write_models   # noqa: E402


def bow(cv, cx, cy, half, bulge, col, col2=None, thick=1, string=(214, 206, 190), tips=None):
    """A vertical bow: the belly bulges to the left of x = cx, the string is the straight line at x = cx. Returns the limb points (top to bottom)."""
    pts = []
    for i in range(-half, half + 1):
        t = i / half
        x = cx - bulge * (1 - t * t)
        y = cy + i
        pts.append((x, y))
        c = col if col2 is None else mix(col, col2, abs(t))
        for k in range(thick):
            cv.put(x - k, y, c if k == 0 else shade(c, 0.78))
    cv.line(cx, cy - half, cx, cy + half, string)
    return pts


def shield_rows(cv, cx, top, rows, rim, face, face2=None):
    """A shield from a list of half widths, one per row from `top`: a rim pixel on each side, the face inside."""
    for j, hw in enumerate(rows):
        y = top + j
        for x in range(int(cx - hw), int(cx + hw) + 1):
            edge = x <= int(cx - hw) or x >= int(cx + hw) or j == 0 or j == len(rows) - 1
            c = rim if edge else (face if face2 is None else mix(face, face2, (x - (cx - hw)) / max(1, 2 * hw)))
            cv.put(x, y, c)


# ---------------------------------------------------------------- the twelve icons (each returns a Canvas and the outline colour)
def divine_mercy():
    cv = Canvas()
    gold, gold2, red, hi = (240, 190, 80), (200, 130, 50), (200, 60, 40), (255, 238, 170)
    pts = bow(cv, 11, 8, 7, 3.2, gold, gold2, thick=2, string=(236, 224, 200))
    for i, (x, y) in enumerate(pts):
        if i in (1, 3, 5, 9, 11, 13):                                                    # feather tips fanned out of the limbs
            cv.put(x - 2, y + (1 if y < 8 else -1), red if i % 2 else gold2)
        if i in (2, 4, 10, 12):
            cv.put(x - 1, y, hi)
    cv.put(10, 8, (210, 210, 220))                                                       # the small ring grip with red ribbons
    cv.put(9, 7, red)
    cv.put(9, 9, red)
    cv.put(11, 1, red)
    cv.put(11, 15, red)
    cv.put(8, 4, (255, 245, 200))
    return cv, (60, 36, 18)


def embrasure():
    cv = Canvas()
    green, green2, leaf, pink = (112, 160, 84), (70, 112, 56), (200, 226, 160), (232, 90, 112)
    pts = bow(cv, 11, 8, 7, 3.0, green, green2, thick=1, string=(226, 220, 206))
    for i, (x, y) in enumerate(pts):
        cv.put(x - 1, y, shade(green, 0.82))
    for (x, y, c) in ((5, 3, leaf), (4, 4, pink), (6, 2, leaf), (6, 13, leaf), (5, 12, pink), (4, 11, leaf), (9, 1, leaf), (10, 0, pink), (9, 14, leaf), (10, 15, pink)):
        cv.put(x, y, c)
    for y in range(6, 11):                                                              # the red crystal grip
        cv.put(8, y, (216, 52, 84))
        cv.put(9, y, (255, 120, 140) if y in (6, 7) else (176, 32, 66))
    cv.put(8, 6, (255, 170, 184))
    return cv, (26, 44, 22)


def the_dragons_spirit():
    cv = Canvas()
    dark, dark2, teal, tealhi, brown = (74, 66, 76), (44, 40, 50), (62, 190, 182), (160, 255, 244), (170, 124, 58)
    pts = bow(cv, 11, 8, 7, 3.4, dark, dark2, thick=2, string=(222, 218, 206))
    for i in (1, 2, 13, 14):                                                            # the jagged teal dragon claws at both tips
        x, y = pts[i]
        cv.put(x - 1, y, teal)
        cv.put(x - 2, y + (-1 if y < 8 else 1), tealhi if i in (1, 14) else teal)
    cv.put(pts[0][0] + 1, pts[0][1] - 1, tealhi)
    cv.put(pts[-1][0] + 1, pts[-1][1] + 1, tealhi)
    for y in range(6, 11):                                                              # the gold-brown grip wrap
        cv.put(8, y, brown if y % 2 else shade(brown, 0.7))
    cv.put(6, 5, (110, 100, 124))
    cv.put(6, 11, (110, 100, 124))
    return cv, (18, 16, 24)


def the_reaver():
    cv = Canvas()
    black, black2, red, redhi = (44, 38, 52), (22, 18, 28), (206, 44, 44), (255, 120, 90)
    pts = bow(cv, 11, 8, 7, 3.4, black, black2, thick=2, string=(184, 170, 170))
    for i in (2, 5, 9, 12):                                                             # glowing red cracks
        cv.put(pts[i][0] - 1, pts[i][1], red)
    for (x, y) in ((7, 4), (6, 5), (6, 11), (7, 12)):                                   # thorns
        cv.put(x, y, black2)
    cv.put(3, 14, red)                                                                  # the spike on the lower limb
    cv.put(4, 14, (196, 190, 200))
    cv.put(5, 13, (196, 190, 200))
    for y in range(6, 11):                                                              # the red-wrapped grip
        cv.put(8, y, red if y % 2 else shade(red, 0.7))
    cv.put(1, 3, (120, 120, 132))                                                       # the chain trailing off, and the barbed arrow's head
    cv.put(2, 2, (120, 120, 132))
    cv.put(2, 4, redhi)
    cv.put(3, 3, (96, 96, 108))
    return cv, (14, 10, 20)


def warlute():
    cv = Canvas()
    crim, crim2, silver, silver2, dark = (176, 36, 52), (110, 20, 34), (206, 210, 224), (140, 146, 164), (46, 44, 54)
    cv.disc(5, 11, 4, crim, crim2)
    cv.ring(5, 11, 3.6, 4.4, silver)
    cv.line(7, 9, 14, 2, dark)                                                          # the neck with the strings
    cv.line(8, 9, 14, 3, (190, 186, 200))
    cv.line(13, 1, 15, 3, silver)                                                       # the crossbow prod at the head
    cv.put(12, 3, silver2)
    cv.put(14, 4, silver2)
    for (x, y) in ((0, 13), (1, 12)):                                                   # the spike at the other end
        cv.put(x, y, silver)
    for (x, y) in ((2, 15), (3, 15), (4, 15), (5, 15), (6, 14), (7, 13)):               # the curved blade under the body
        cv.put(x, y, silver if (x + y) % 2 else silver2)
    cv.put(5, 11, (255, 210, 120))
    return cv, (30, 12, 22)


def aegis_of_the_abyss():
    cv = Canvas()
    shield_rows(cv, 8, 1, [5, 6, 6, 6, 6, 6, 6, 5, 5, 4, 4, 3, 2, 1], (170, 172, 186), (56, 56, 66), (40, 40, 50))
    for (x, y) in ((6, 3), (10, 3)):                                                    # the bulging eyes
        cv.put(x, y, (220, 220, 230))
        cv.put(x, y + 1, (30, 30, 40))
    for y in range(7, 11):                                                              # the mouth full of galaxy
        for x in range(5, 12):
            if (x - 8) ** 2 / 9.0 + (y - 8.5) ** 2 / 4.0 <= 1.0:
                cv.put(x, y, (130, 70, 220) if (x + y) % 3 else (86, 40, 160))
    for (x, y) in ((6, 8), (9, 7), (10, 9), (8, 9)):
        cv.put(x, y, (255, 255, 255))
    for (x, y) in ((6, 7), (10, 7), (7, 10), (9, 10)):                                  # fangs
        cv.put(x, y, (236, 236, 244))
    return cv, (22, 20, 30)


def celestial_support():
    cv = Canvas()
    shield_rows(cv, 8, 1, [5, 5, 5, 5, 5, 5, 5, 4, 4, 3, 3, 2, 2, 1], (74, 130, 214), (150, 156, 168), (126, 132, 146))
    for (x, y) in ((4, 2), (12, 2), (8, 14)):                                           # the gold corner studs
        cv.put(x, y, (240, 196, 70))
    blue = (70, 140, 230)
    for k in range(6):                                                                  # the blue wings of the emblem
        cv.put(6 - (k // 3), 4 + k, blue)
        cv.put(10 + (k // 3), 4 + k, blue)
    cv.put(8, 7, (230, 240, 255))
    cv.put(8, 8, blue)
    for (x, y) in ((1, 4), (2, 3), (14, 4), (13, 3), (1, 6), (14, 6)):                  # the golden glow behind
        cv.put(x, y, (255, 224, 120))
    return cv, (30, 36, 60)


def heros_shield():
    cv = Canvas()
    shield_rows(cv, 8, 1, [6, 6, 6, 6, 6, 6, 5, 5, 4, 4, 3, 3, 2, 1], (216, 222, 232), (176, 224, 236), (206, 238, 246))
    for (x, y) in ((4, 3), (5, 3), (6, 3), (5, 4), (4, 5), (4, 6), (5, 7), (6, 8), (7, 9), (9, 9), (10, 8), (11, 6), (11, 4), (10, 3), (9, 4), (9, 5), (11, 11), (7, 11), (8, 12)):
        cv.put(x, y, (232, 236, 244))                                                   # the silver maze
    cv.put(8, 6, (90, 220, 255))                                                        # the glowing gem
    cv.put(8, 7, (40, 160, 230))
    cv.put(8, 5, (180, 245, 255))
    return cv, (36, 66, 90)


def thunder_shield():
    cv = Canvas()
    cv.disc(8, 8, 6.4, (62, 78, 112), (38, 48, 76))
    cv.ring(8, 8, 5.6, 6.6, (96, 118, 160))
    for (x, y) in ((8, 8), (8, 7), (9, 8)):                                             # the boss
        cv.put(x, y, (150, 160, 180))
    bolt = (130, 220, 255)
    for (x, y) in ((8, 3), (7, 4), (8, 5), (6, 6), (7, 7), (9, 9), (10, 10), (9, 11), (10, 12), (11, 8), (12, 7), (4, 9), (3, 10), (5, 11)):
        cv.put(x, y, bolt)
    for (x, y) in ((8, 4), (7, 6), (10, 11), (12, 8)):
        cv.put(x, y, (240, 252, 255))
    return cv, (18, 24, 44)


def bewitching_lyre():
    cv = Canvas()
    pink, pink2, silver = (222, 70, 110), (156, 36, 72), (214, 218, 230)
    path = [(2, 2), (2, 3), (3, 4), (4, 5), (5, 6), (6, 7), (7, 8), (8, 9), (9, 9), (10, 9)]
    for i, (x, y) in enumerate(path):
        cv.put(x, y, pink)
        cv.put(x + 1, y, pink2 if i % 2 else pink)
        cv.put(x, y + 1, pink2)
    cv.disc(11.5, 10.5, 3.6, pink, pink2)
    cv.ring(11.5, 10.5, 1.4, 2.0, (255, 150, 176))
    for (x, y) in ((1, 1), (2, 1), (3, 1)):
        cv.put(x, y, silver)
    cv.line(3, 2, 11, 9, (230, 230, 240))
    cv.line(2, 3, 10, 10, (190, 192, 206))
    cv.put(11, 10, (140, 146, 160))
    return cv, (60, 12, 32)


def eye_of_the_deep_one():
    cv = Canvas()
    green, green2, sucker = (44, 120, 84), (24, 72, 56), (112, 190, 140)
    cv.ring(8, 9, 4.4, 6.2, green)
    for (x, y) in ((8, 15), (6, 14), (10, 14), (3, 12), (13, 12), (2, 9), (14, 9)):     # suckers
        cv.put(x, y, sucker)
    for k in range(7):                                                                  # the tentacle tips curling over the eye
        cv.put(3 + k, 5 - (1 if k in (2, 3, 4) else 0), green2 if k % 2 else green)
        cv.put(13 - k, 4 + (1 if k > 3 else 0), green if k % 2 else green2)
    cv.disc(8, 5.5, 1.9, (60, 150, 255), (160, 220, 255))
    cv.put(7, 5, (230, 248, 255))
    cv.put(8, 6, (30, 90, 200))
    return cv, (10, 30, 24)


def horn_of_leroy():
    cv = Canvas()
    dark, dark2, hi, heart = (54, 54, 66), (26, 26, 36), (122, 124, 140), (236, 90, 120)
    cv.ring(6, 9, 3.0, 5.2, dark)                                                       # the coiled body
    cv.erase_disc(6, 9, 2.2)
    for k in range(6):                                                                  # the bell flaring up and to the right
        cv.line(10 + k // 2, 5 - k // 2, 11 + k // 2, 4 - k // 2, dark2)
    cv.disc(12, 5, 3.0, dark, dark2)
    cv.put(2, 5, dark)                                                                  # the mouthpiece
    cv.put(1, 4, dark2)
    cv.put(3, 4, dark)
    for (x, y) in ((4, 12), (5, 13), (6, 13), (8, 12)):
        cv.put(x, y, hi)
    for (x, y) in ((11, 4), (13, 4), (11, 5), (12, 5), (13, 5), (12, 6)):               # the heart cut out of the bell
        cv.put(x, y, heart)
    cv.put(12, 3, (255, 160, 180))
    return cv, (14, 14, 22)


ICONS_DEF = [
    ("divine_mercy", divine_mercy, "LEGEND", True, (14, 1), False),
    ("embrasure", embrasure, "SPECIAL", True, (14, 0), False),
    ("the_dragons_spirit", the_dragons_spirit, "RARE", True, (14, 1), False),
    ("the_reaver", the_reaver, "UNIQUE", True, (14, 1), False),
    ("warlute", warlute, "RARE", False, (15, 5), False),
    ("aegis_of_the_abyss", aegis_of_the_abyss, "LEGEND", True, (14, 1), False),
    ("celestial_support", celestial_support, "UNIQUE", True, (14, 1), False),
    ("heros_shield", heros_shield, "LEGEND", True, (14, 1), False),
    ("thunder_shield", thunder_shield, "RARE", True, (14, 2), False),
    ("bewitching_lyre", bewitching_lyre, "RARE", True, (14, 1), True),
    ("eye_of_the_deep_one", eye_of_the_deep_one, "RARE", True, (14, 1), True),
    ("horn_of_leroy", horn_of_leroy, "LEGEND", True, (14, 1), True),
]


def flat_model(item_id):
    with open(os.path.join(MODELS, item_id + ".json"), "w", newline="\n") as f:
        json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/" + item_id}}, f, indent=2)


def main(argv):
    sheet = None
    only = []
    i = 0
    while i < len(argv):
        if argv[i] == "--sheet":
            sheet = argv[i + 1]
            i += 2
        else:
            only.append(argv[i])
            i += 1
    os.makedirs(ICONS, exist_ok=True)
    made = []
    for item_id, fn, grade, upright, gp, flat in ICONS_DEF:
        if only and item_id not in only:
            continue
        cv, outline = fn()
        cv.outline(outline)
        gx, gy = gp
        if (gx, gy) not in cv.px:
            cv.put(gx, gy, rgb(GRADE[grade]))                                           # the grade-coloured rim pixel (where the corner is clear)
        im = cv.image()
        im.save(os.path.join(ICONS, item_id + ".png"))
        if flat:
            flat_model(item_id)
        else:
            write_models(item_id, upright)
        made.append((item_id, im))
        print("icon", item_id)
    if sheet:
        z, cols = 12, 6
        rows = (len(made) + cols - 1) // cols
        sh = Image.new("RGBA", (cols * (16 * z + 8) + 8, rows * (16 * z + 8) + 8), (74, 76, 92, 255))
        for k, (_, im) in enumerate(made):
            big = im.resize((16 * z, 16 * z), Image.NEAREST)
            sh.paste(big, (8 + (k % cols) * (16 * z + 8), 8 + (k // cols) * (16 * z + 8)), big)
        sh.save(sheet)


if __name__ == "__main__":
    main(sys.argv[1:])
