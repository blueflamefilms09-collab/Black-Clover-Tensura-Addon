#!/usr/bin/env python3
"""Relics of Many Worlds, part 4, wave 21: the 16x16 inventory icons and the item models of the twelve weapons (Rift Breaker, Scimitar of the Seasons,
Silver-Wind Mountain Glaive, Spider Sword, Stick of Youth, Sun Gift, The Last Horseman, The Legacy, The Librarian's Maul, Time Hammer, Vanguard Hammer,
Willbreaker).

    python3 -B tools/relics/wave21_icons.py [id ...]       (idempotent)

Writes, per id,
  assets/nusmp/textures/item/<id>.png        the sprite: the old-style inventory icon (kept as the icon; the item itself is 3D in hand)
  assets/nusmp/models/item/<id>_icon.json    the flat inventory model of that sprite
  assets/nusmp/models/item/<id>.json         the in-hand model: builtin/entity, drawn by client/WeaponRenderer from weapon_meshes/<id>.txt
Every icon is bands along a diagonal (a width profile and a colour function per weapon) plus hand-placed pixels for the heads, then a one pixel dark
outline; the grade shows as one coloured pixel at the butt and the glow as bright pixels at runes, gems and edges.
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from wave1_icons import Canvas, rgb, shade, mix, GRADE, ICONS, write_models   # noqa: E402
from wave10_icons import band, noise                                         # noqa: E402


def dots(cv, pts, c, c2=None):
    """Paints the pixels; with a second colour every other one (a checker) gives a little texture."""
    for (x, y) in pts:
        cv.put(x, y, c if (c2 is None or (x + y) % 2 == 0) else c2)


def rift_breaker():
    cv = Canvas()
    purple, purple2, pink, gold, cord = (112, 62, 150), (70, 36, 104), (255, 90, 190), (255, 224, 120), (36, 30, 40)
    band(cv, (2, 14), (10, 6), lambda t: 0.6, lambda t, s: purple if s < 0.1 else purple2, 0.0, 1.0)
    dots(cv, [(4, 12), (6, 10), (8, 8)], pink)                                           # the runes
    dots(cv, [(9, 6), (10, 7)], cord)                                                   # the cord wrapping
    dots(cv, [(11, 5), (12, 4), (13, 3), (11, 4), (12, 3), (12, 5), (14, 2)], pink, (255, 150, 220))   # the crystal leaf
    cv.put(13, 3, (255, 210, 240))
    dots(cv, [(1, 15), (0, 14), (1, 13), (2, 15), (0, 15)], pink, gold)                  # the vortex at the butt
    cv.put(1, 14, (255, 250, 200))
    return cv, (46, 20, 62)


def scimitar_of_the_seasons():
    cv = Canvas()
    jade, jade2, jade3, wood, bud, leaf = (96, 224, 184), (60, 176, 148), (190, 255, 236), (84, 52, 48), (210, 40, 80), (84, 170, 80)
    pts = [(6, 9), (7, 8), (8, 7), (9, 6), (10, 5), (10, 4), (11, 3), (11, 2), (12, 1), (7, 7), (8, 6), (9, 5), (9, 4), (10, 3), (10, 2)]
    for (x, y) in pts:
        cv.put(x, y, jade if (x + y) % 3 else jade2)
    for (x, y) in ((8, 7), (9, 5), (10, 3), (11, 2)):
        cv.put(x, y, jade3)
    dots(cv, [(5, 10), (4, 11), (3, 12), (2, 13), (3, 13), (2, 14)], wood, shade(wood, 0.7))
    dots(cv, [(5, 9), (6, 10), (4, 10)], bud, leaf)
    cv.put(6, 8, (255, 90, 120))
    cv.put(5, 11, leaf)
    cv.put(2, 15, rgb(GRADE["UNIQUE"]))
    cv.put(13, 4, (210, 255, 240))
    return cv, (22, 70, 66)


def silver_wind_mountain_glaive():
    cv = Canvas()
    steel, steel2, red, red2, pearl, rune = (150, 156, 170), (84, 88, 104), (150, 30, 38), (100, 18, 26), (250, 244, 250), (200, 210, 230)
    band(cv, (2, 14), (13, 3), lambda t: 0.55, lambda t, s: red if (int(t * 22) % 2) else red2, 0.12, 0.88)
    dots(cv, [(13, 2), (14, 1), (14, 2), (12, 2), (13, 1), (15, 0), (11, 3), (14, 3)], steel, steel2)    # upper blade, curling left
    dots(cv, [(2, 14), (1, 15), (1, 14), (2, 15), (0, 15), (3, 15), (0, 14), (2, 13)], steel, steel2)  # lower blade, curling right
    cv.put(12, 3, pearl)
    cv.put(3, 13, pearl)
    cv.put(13, 1, rune)
    cv.put(1, 14, rune)
    dots(cv, [(11, 4), (10, 4), (4, 12), (5, 12)], (240, 240, 250))                                      # the white ribbons
    return cv, (36, 38, 52)


def spider_sword():
    cv = Canvas()
    steel, steel2, resin, resin2, hilt = (74, 78, 92), (40, 42, 54), (190, 40, 34), (120, 20, 24), (60, 44, 44)
    pts = [(7, 9), (8, 8), (9, 7), (10, 6), (11, 5), (12, 4), (12, 3), (13, 2), (8, 9), (9, 8), (10, 7), (11, 6), (12, 5), (13, 4), (13, 3), (14, 2)]
    for (x, y) in pts:
        cv.put(x, y, steel if (x + y) % 2 else steel2)
    dots(cv, [(8, 8), (10, 7), (11, 5), (12, 4), (13, 3), (9, 8), (12, 3)], resin, resin2)              # the web of resin across the blade
    dots(cv, [(6, 10), (7, 11), (5, 10), (6, 9)], resin, resin2)                                         # the flared cup guard
    band(cv, (2, 14), (5, 11), lambda t: 0.6, lambda t, s: hilt if s < 0 else shade(hilt, 0.7), 0.0, 1.0)
    cv.put(2, 14, rgb(GRADE["RARE"]))
    cv.put(1, 15, resin)
    return cv, (22, 20, 28)


def stick_of_youth():
    cv = Canvas()
    bark, bark2, light, hole = (150, 104, 62), (100, 66, 40), (200, 150, 96), (60, 38, 24)
    band(cv, (2, 14), (13, 3), lambda t: 0.85 + 0.35 * t, lambda t, s: light if s < -0.3 else (bark if s < 0.4 else bark2), 0.0, 1.0)
    dots(cv, [(12, 3), (13, 3), (12, 2), (13, 2)], hole)                                                 # the hollow end
    dots(cv, [(7, 9), (8, 9)], hole)                                                                     # the knot hole
    dots(cv, [(10, 8), (11, 8), (11, 9)], bark2)                                                         # the broken stub
    for k in range(3, 12, 3):
        cv.put(k, 15 - k, bark2)
    cv.put(2, 14, rgb(GRADE["RARE"]))
    return cv, (50, 30, 20)


def sun_gift():
    cv = Canvas()
    brass, brass2, silver, silver2, ruby, glow = (196, 160, 70), (120, 92, 36), (222, 226, 236), (140, 146, 164), (232, 50, 40), (255, 240, 170)
    band(cv, (3, 13), (12, 4), lambda t: 0.55, lambda t, s: brass if int(t * 24) % 2 else brass2, 0.1, 0.9)
    dots(cv, [(13, 3), (14, 2), (14, 1), (13, 2), (12, 3), (15, 0), (14, 3)], silver, silver2)           # upper leaf blade
    dots(cv, [(2, 14), (1, 15), (1, 14), (2, 15), (0, 15), (3, 15), (0, 14)], silver, silver2)           # lower leaf blade
    cv.put(12, 4, ruby)
    cv.put(3, 13, ruby)
    cv.put(14, 2, glow)
    cv.put(1, 15, rgb(GRADE["RARE"]))
    return cv, (60, 46, 24)


def the_last_horseman():
    cv = Canvas()
    shaft, shaft2, brass, purple, purple2, flame = (210, 214, 226), (130, 134, 150), (200, 160, 60), (130, 50, 190), (84, 28, 130), (220, 130, 255)
    band(cv, (2, 14), (11, 5), lambda t: 0.5, lambda t, s: shaft if s < 0 else shaft2, 0.0, 1.0)
    dots(cv, [(5, 11), (8, 8), (11, 5)], brass)
    pts = [(12, 4), (11, 3), (11, 2), (12, 2), (13, 2), (13, 3), (14, 3), (14, 4), (15, 4), (15, 5), (12, 3), (13, 4), (14, 5), (15, 6), (14, 6)]
    for (x, y) in pts:
        cv.put(x, y, purple if (x + y) % 3 else purple2)
    dots(cv, [(11, 1), (13, 1), (12, 0), (14, 1), (10, 2)], flame)                                       # the violet flame
    cv.put(12, 4, (255, 90, 90))
    cv.put(2, 14, rgb(GRADE["RARE"]))
    return cv, (36, 18, 56)


def the_legacy():
    cv = Canvas()
    steel, steel2, teal, teal2, gold, crystal = (120, 124, 136), (66, 68, 80), (40, 170, 160), (20, 110, 110), (214, 176, 70), (110, 220, 255)
    band(cv, (2, 14), (9, 7), lambda t: 0.6, lambda t, s: teal if int(t * 14 + s) % 2 else teal2, 0.15, 0.95)
    dots(cv, [(4, 12), (7, 9), (2, 14)], gold)
    dots(cv, [(10, 6), (11, 5), (12, 4), (13, 3), (12, 6), (13, 5), (14, 4), (9, 5), (10, 4), (11, 3), (14, 6), (15, 5), (8, 4), (9, 3)], steel, steel2)   # the floating head
    dots(cv, [(11, 4), (12, 5), (10, 5), (13, 4)], steel2, steel)
    cv.put(11, 4, crystal)
    cv.put(12, 5, crystal)
    cv.put(2, 14, rgb(GRADE["RARE"]))
    return cv, (30, 32, 44)


def the_librarians_maul():
    cv = Canvas()
    pole, pole2, cover, cover2, pages = (176, 140, 90), (120, 90, 56), (74, 160, 164), (40, 104, 112), (212, 214, 206)
    band(cv, (1, 15), (10, 6), lambda t: 0.45, lambda t, s: pole if s < 0 else pole2, 0.0, 1.0)
    for (x, y) in ((9, 4), (10, 4), (11, 4), (12, 4), (13, 4), (9, 3), (10, 3), (11, 3), (12, 3), (13, 3), (14, 3), (10, 2), (11, 2), (12, 2), (13, 2), (14, 2), (11, 1), (12, 1), (13, 1), (14, 1)):
        cv.put(x, y, cover if (x + y) % 2 else cover2)
    for (x, y) in ((9, 5), (10, 5), (11, 5), (12, 5), (13, 5), (14, 4), (14, 5), (15, 3), (15, 4), (10, 6), (11, 6), (12, 6), (13, 6)):
        cv.put(x, y, pages if (x + y) % 3 else shade(pages, 0.82))
    cv.put(2, 14, rgb(GRADE["RARE"]))
    return cv, (20, 52, 60)


def time_hammer():
    cv = Canvas()
    stone, stone2, crack, rune = (190, 186, 178), (112, 108, 104), (255, 96, 40), (255, 150, 70)
    band(cv, (2, 14), (9, 7), lambda t: 0.4, lambda t, s: stone if s < 0 else stone2, 0.0, 1.0)
    dots(cv, [(6, 10), (4, 12)], rune)
    for (x, y) in ((8, 6), (9, 5), (10, 4), (11, 3), (12, 2), (7, 5), (8, 4), (9, 3), (10, 2), (9, 6), (10, 5), (11, 4), (12, 3), (13, 2),
                   (11, 6), (12, 5), (13, 4), (14, 3), (12, 7), (13, 6), (14, 5), (15, 4), (10, 7), (11, 7)):
        cv.put(x, y, stone if (x + y) % 3 else stone2)
    dots(cv, [(10, 4), (11, 5), (12, 4), (9, 5), (13, 5)], crack)                                       # the glowing fissures
    cv.put(11, 4, (255, 224, 150))
    dots(cv, [(1, 15), (3, 15), (0, 13)], rune)                                                         # the floating fragments at the pommel
    cv.put(2, 14, rgb(GRADE["LEGEND"]))
    return cv, (44, 40, 38)


def vanguard_hammer():
    cv = Canvas()
    steel, steel2, red, red2 = (150, 154, 164), (84, 88, 100), (200, 30, 56), (130, 18, 40)
    band(cv, (1, 15), (10, 6), lambda t: 0.45, lambda t, s: steel if s < 0 else steel2, 0.0, 1.0)
    for (x, y) in ((9, 6), (10, 5), (11, 4), (12, 3), (13, 2), (10, 6), (11, 5), (12, 4), (13, 3), (14, 2), (11, 6), (12, 5), (13, 4), (14, 3), (15, 2), (10, 4), (11, 3), (12, 2), (9, 5)):
        cv.put(x, y, steel if (x + y) % 3 else steel2)
    dots(cv, [(11, 5), (12, 4), (13, 3), (12, 5)], red, red2)                                           # the red stripe
    cv.put(15, 4, steel)
    cv.put(14, 5, steel2)                                                                               # the spike
    dots(cv, [(4, 12), (3, 11), (5, 13), (6, 12), (7, 9), (8, 10), (5, 11)], red, red2)                 # the ribbon spiralling down the haft
    cv.put(1, 15, rgb(GRADE["RARE"]))
    return cv, (40, 40, 52)


def willbreaker():
    cv = Canvas()
    iron, iron2, red, grip = (96, 98, 108), (50, 52, 62), (210, 36, 44), (46, 40, 44)
    band(cv, (1, 15), (11, 5), lambda t: 0.4, lambda t, s: iron if s < 0 else iron2, 0.0, 1.0)
    dots(cv, [(2, 14), (3, 13), (3, 14), (2, 15)], grip, shade(grip, 1.4))
    for (x, y) in ((10, 4), (11, 3), (12, 2), (13, 1), (11, 5), (12, 4), (13, 3), (14, 2), (12, 6), (13, 5), (14, 4), (15, 3), (9, 5), (10, 6), (11, 6), (14, 6)):
        cv.put(x, y, iron if (x + y) % 3 else iron2)
    cv.put(12, 4, (30, 10, 14))
    cv.put(13, 4, (255, 90, 80))                                                                         # the eye
    dots(cv, [(4, 12), (4, 13), (7, 9), (7, 10), (9, 8), (10, 9)], red)                                  # the red tassels
    cv.put(1, 15, rgb(GRADE["RARE"]))
    return cv, (30, 20, 28)


ICONS_DEF = [
    ("rift_breaker", rift_breaker, "RARE", False, (0, 15)),
    ("scimitar_of_the_seasons", scimitar_of_the_seasons, "UNIQUE", False, (1, 15)),
    ("silver_wind_mountain_glaive", silver_wind_mountain_glaive, "RARE", False, (0, 13)),
    ("spider_sword", spider_sword, "RARE", False, (1, 14)),
    ("stick_of_youth", stick_of_youth, "RARE", False, (1, 15)),
    ("sun_gift", sun_gift, "RARE", False, (0, 14)),
    ("the_last_horseman", the_last_horseman, "RARE", False, (1, 15)),
    ("the_legacy", the_legacy, "RARE", False, (1, 15)),
    ("the_librarians_maul", the_librarians_maul, "RARE", False, (1, 14)),
    ("time_hammer", time_hammer, "LEGEND", False, (1, 14)),
    ("vanguard_hammer", vanguard_hammer, "RARE", False, (0, 15)),
    ("willbreaker", willbreaker, "RARE", False, (0, 15)),
]


def main(only=None):
    os.makedirs(ICONS, exist_ok=True)
    for item_id, fn, grade, upright, gp in ICONS_DEF:
        if only and item_id not in only:
            continue
        cv, outline = fn()
        cv.outline(outline)
        gx, gy = gp
        cv.put(gx, gy, rgb(GRADE[grade]))                              # the grade-coloured rim pixel
        cv.image().save(os.path.join(ICONS, item_id + ".png"))
        write_models(item_id, upright)
        print("icon", item_id)


if __name__ == "__main__":
    main(sys.argv[1:])
