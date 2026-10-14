#!/usr/bin/env python3
"""Relics of Many Worlds, second card set, wave 8: the 16x16 inventory icons and the item models of the ten blades and axes.

    python3 -B tools/relics/wave8_icons.py            (idempotent)

Per id it writes assets/nusmp/textures/item/<id>.png (the sprite, kept as the inventory icon), models/item/<id>_icon.json (the flat model of the
sprite) and models/item/<id>.json (builtin/entity, drawn by client/WeaponRenderer from weapon_meshes/<id>.txt; the Bloodbound Katana's display
transforms). The shapes are drawn from primitives, given a one pixel dark outline, a grade-coloured rim pixel and glow pixels.
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import wave1_icons as w1                                      # noqa: E402  (Canvas, colour helpers, the model writer)
from wave1_icons import Canvas, rgb, mix, shade               # noqa: E402

ICONS, MODELS, GRADE = w1.ICONS, w1.MODELS, w1.GRADE


def thick(cv, x0, y0, x1, y1, c, c2=None, hi=None, lo=None):
    """A two pixel wide diagonal bar with an optional light upper edge and dark lower edge."""
    n = int(max(abs(x1 - x0), abs(y1 - y0)))
    for i in range(n + 1):
        t = i / max(1, n)
        x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
        col = c if c2 is None else mix(c, c2, t)
        cv.put(x, y, col)
        cv.put(x + 1, y, lo or shade(col, 0.8))
        if hi:
            cv.put(x, y - 1, hi)


def spotlight():
    cv = Canvas()
    wood, wood2 = (150, 100, 56), (104, 66, 36)
    cv.line(2, 14, 5, 11, wood, wood2)
    cv.put(1, 15, (60, 60, 66))
    for (x, y) in ((5, 10), (6, 10), (5, 9), (6, 9), (7, 10), (6, 11)):          # the black lamp housing
        cv.put(x, y, (34, 34, 40))
    cv.put(5, 9, (90, 90, 98))
    cv.put(7, 8, (60, 60, 68))
    for i in range(0, 8):                                                       # the beam: a bright blade of light
        t = i / 7
        x, y = 7 + 7 * t, 8 - 7 * t
        cv.put(x, y, (255, 246, 190))
        cv.put(x + 1, y, (255, 226, 130))
        cv.put(x, y - 1, (255, 252, 226))
    for (x, y) in ((13, 5), (10, 2), (4, 6), (14, 9)):
        cv.put(x, y, (255, 255, 255))                                           # sparkles
    return cv, (60, 44, 18)


def duality_blade():
    cv = Canvas()
    black, hi = (30, 28, 38), (70, 64, 84)
    thick(cv, 5, 11, 13, 3, black, (46, 42, 58), hi)
    cv.line(2, 14, 4, 12, (36, 32, 44), (22, 20, 28))
    cv.put(3, 12, (60, 54, 74)); cv.put(5, 13, (60, 54, 74)); cv.put(4, 10, (60, 54, 74)); cv.put(6, 12, (60, 54, 74))   # curved wing guard
    for (x, y) in ((7, 8), (8, 9), (9, 6), (10, 7), (11, 4), (12, 5), (8, 6), (10, 9)):                                    # the lilac streamers
        cv.put(x, y, (226, 212, 255) if (x + y) % 2 else (176, 150, 240))
    cv.put(14, 2, (236, 226, 255))
    return cv, (22, 16, 40)


def frostfang():
    cv = Canvas()
    ice, ice2, hi = (120, 190, 214), (60, 120, 160), (226, 248, 255)
    cv.line(2, 14, 9, 8, (96, 130, 150), (60, 86, 112))                          # the long curved haft
    cv.line(1, 15, 2, 14, (200, 230, 245))
    for (x, y) in ((8, 4), (9, 3), (10, 3), (11, 3), (12, 4), (13, 5), (13, 6), (12, 7), (11, 8), (10, 8), (9, 7), (10, 5), (11, 5), (11, 6), (12, 6), (9, 5), (10, 4), (11, 4), (12, 5)):
        cv.put(x, y, ice if (x + y) % 3 else ice2)                              # the skull-shaped head
    for (x, y) in ((8, 2), (10, 1), (12, 2), (14, 4), (14, 7)):
        cv.put(x, y, hi)                                                         # spikes
    cv.put(11, 5, (30, 60, 90)); cv.put(10, 6, (30, 60, 90))                    # eye socket
    cv.put(13, 3, hi); cv.put(9, 9, hi)
    return cv, (20, 44, 70)


def soul_shatter_blade():
    cv = Canvas()
    dark, red, glow = (60, 14, 22), (160, 36, 34), (255, 120, 60)
    thick(cv, 4, 12, 13, 2, dark, red, (230, 80, 50))
    cv.line(2, 14, 4, 12, (50, 20, 24), (30, 10, 14))
    for (x, y) in ((3, 10), (4, 10), (5, 11), (6, 13), (7, 14), (5, 9), (6, 10), (7, 12)):                     # thorny guard
        cv.put(x, y, (110, 24, 28))
    cv.put(3, 9, glow); cv.put(8, 14, glow)
    for (x, y) in ((8, 8), (10, 6), (12, 4)):
        cv.put(x, y, glow)
    cv.put(14, 1, (255, 190, 120))
    return cv, (30, 6, 10)


def buster_sword():
    cv = Canvas()
    steel, hi, lo = (176, 182, 190), (230, 234, 240), (108, 114, 124)
    pts = [(3, 15), (5, 15), (6, 11), (4, 12)]
    for k in range(0, 10):                                                       # a wide slab along the diagonal
        x, y = 5 + k, 10 - k
        for j in range(-1, 3):
            cv.put(x + j, y + (1 if j > 0 else 0), steel)
        cv.put(x, y - 1, hi)
        cv.put(x + 2, y + 2, lo)
    cv.put(4, 12, (140, 146, 156)); cv.put(3, 13, (140, 146, 156))
    cv.line(1, 15, 4, 12, (150, 36, 44), (100, 20, 30))                           # the red wrapped grip
    cv.put(8, 9, (44, 48, 56)); cv.put(9, 8, (44, 48, 56))                        # the materia holes
    for (x, y) in ((5, 12), (6, 11), (4, 11)):
        cv.put(x, y, (200, 204, 212))
    cv.put(14, 1, (236, 240, 246))
    return cv, (30, 34, 44)


def dragon_tooth_blade_red():
    cv = Canvas()
    bone, hi = (236, 232, 220), (255, 255, 250)
    path = [(5, 11), (6, 9), (7, 8), (8, 7), (9, 5), (10, 4), (11, 3), (12, 2), (13, 1), (14, 1)]     # a thin, strongly curved fang
    for (x, y) in path:
        cv.put(x, y, bone); cv.put(x, y + 1, (186, 180, 168))
    for (x, y) in path[2:]:
        cv.put(x - 1, y, hi)
    cv.line(2, 14, 5, 11, (96, 70, 60), (60, 40, 36))
    cv.put(1, 15, (160, 40, 40)); cv.put(2, 15, (120, 28, 28))
    for (x, y) in ((4, 12), (5, 12), (4, 11)):
        cv.put(x, y, (190, 40, 40))                                                # the red cord knots
    cv.put(12, 3, (255, 90, 50))
    return cv, (40, 40, 44)


def corpse_eater():
    cv = Canvas()
    black, edge, red = (34, 26, 30), (240, 240, 244), (170, 30, 40)
    thick(cv, 5, 11, 14, 2, black, (50, 40, 46))
    for i in range(0, 9):                                                         # the jagged white edge, split open to red
        x, y = 6 + i, 10 - i
        cv.put(x, y - 1, edge if i % 2 else (200, 200, 208))
        if i % 3 == 0:
            cv.put(x, y, red)
    cv.put(9, 6, (255, 255, 255)); cv.put(10, 6, (230, 40, 50)); cv.put(9, 7, (170, 30, 40))   # the eye
    cv.line(2, 14, 4, 12, (200, 40, 50), (240, 240, 240))                       # the diamond-wrapped hilt
    cv.put(5, 11, (214, 176, 80)); cv.put(4, 11, (214, 176, 80)); cv.put(5, 12, (214, 176, 80))   # tsuba
    return cv, (24, 10, 16)


def random_axe_of_charity():
    cv = Canvas()
    cv.line(2, 14, 11, 5, (150, 96, 54), (110, 70, 40))
    for (x, y) in ((8, 3), (9, 3), (10, 3), (7, 4), (8, 4), (9, 4), (10, 4), (6, 5), (7, 5), (8, 5), (9, 5), (7, 6), (8, 6)):
        cv.put(x, y, (156, 160, 168))
    for (x, y) in ((6, 5), (7, 6), (7, 5), (8, 6), (6, 6)):
        cv.put(x, y, (190, 36, 44))                                              # the blood-red stain
    cv.put(9, 3, (226, 230, 236))
    for (x, y) in ((13, 9), (14, 6), (12, 12), (4, 6), (3, 9), (10, 12), (14, 11)):
        cv.put(x, y, (212, 128, 66))                                             # copper pieces
    return cv, (60, 36, 20)


def leviathan_axe():
    cv = Canvas()
    cv.line(2, 14, 10, 6, (120, 140, 160), (70, 90, 120))
    cv.put(1, 15, (200, 232, 248))
    for (x, y) in ((8, 3), (9, 2), (10, 2), (11, 3), (12, 4), (12, 5), (11, 6), (10, 7), (9, 6), (8, 5), (9, 4), (10, 4), (10, 5), (11, 4), (9, 3), (7, 4), (11, 5)):
        cv.put(x, y, (176, 216, 240) if (x + y) % 2 else (120, 176, 214))       # the single frosty blade
    cv.put(13, 3, (236, 250, 255)); cv.put(9, 8, (236, 250, 255))
    for (x, y) in ((10, 4), (10, 5), (9, 4)):
        cv.put(x, y, (90, 230, 255))                                             # glowing runes
    cv.put(6, 11, (110, 230, 255)); cv.put(4, 13, (110, 230, 255))
    return cv, (22, 50, 82)


def shadow_burn():
    cv = Canvas()
    steel, hi = (66, 70, 88), (110, 116, 140)
    for k in range(0, 9):                                                         # a broad slab of dark steel
        x, y = 6 + k, 9 - k
        for j in range(0, 3):
            cv.put(x + j, y + j // 2, steel)
        cv.put(x, y - 1, hi)
    for (x, y) in ((8, 7), (9, 6), (10, 5), (9, 7)):
        cv.put(x, y, (255, 120, 50))                                              # the orange-red runes
    for (x, y) in ((6, 6), (5, 7), (12, 9), (11, 10), (13, 4)):
        cv.put(x, y, (150, 110, 255))                                             # shadow wisps
    cv.put(4, 10, (200, 160, 70)); cv.put(5, 11, (200, 160, 70)); cv.put(6, 12, (200, 160, 70)); cv.put(5, 10, (230, 196, 100))   # gold guard
    cv.line(2, 14, 5, 11, (150, 30, 40), (100, 16, 28))
    cv.put(1, 15, (170, 120, 255)); cv.put(2, 15, (130, 80, 220))                 # the purple pommel orb
    return cv, (24, 20, 36)


ICONS_DEF = [
    # id, drawer, grade, the pixel for the grade rim (x, y)
    ("spotlight", spotlight, "RARE", (0, 15)),
    ("duality_blade", duality_blade, "RARE", (1, 15)),
    ("frostfang", frostfang, "LEGEND", (0, 14)),
    ("soul_shatter_blade", soul_shatter_blade, "RARE", (1, 14)),
    ("buster_sword", buster_sword, "RARE", (0, 14)),
    ("dragon_tooth_blade_red", dragon_tooth_blade_red, "RARE", (0, 14)),
    ("corpse_eater", corpse_eater, "RARE", (1, 15)),
    ("random_axe_of_charity", random_axe_of_charity, "RARE", (1, 15)),
    ("leviathan_axe", leviathan_axe, "LEGEND", (0, 14)),
    ("shadow_burn", shadow_burn, "UNIQUE", (0, 14)),
]


def main(only=None):
    os.makedirs(ICONS, exist_ok=True)
    for item_id, fn, grade, gp in ICONS_DEF:
        if only and item_id not in only:
            continue
        cv, outline = fn()
        cv.outline(outline)
        cv.put(gp[0], gp[1], rgb(GRADE[grade]))
        cv.image().save(os.path.join(ICONS, item_id + ".png"))
        w1.write_models(item_id, False)
        print("icon", item_id)


if __name__ == "__main__":
    main(sys.argv[1:])
