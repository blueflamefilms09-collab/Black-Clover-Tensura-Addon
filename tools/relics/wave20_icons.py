"""Relics of Many Worlds, part 4, wave 20: the 16x16 inventory icons and the item models of the twelve weapons.

    python3 -B tools/relics/wave20_icons.py [id ...]            (idempotent)

Per id it writes assets/nusmp/textures/item/<id>.png (the sprite, kept as the inventory icon), models/item/<id>_icon.json (the flat inventory model)
and models/item/<id>.json (the in-hand model: builtin/entity, drawn by client/WeaponRenderer from weapon_meshes/<id>.txt, with the display transforms of
the Bloodbound Katana). Icons are drawn from primitives with a one pixel dark outline; the grade shows as a coloured pixel at the butt, the glow as
bright pixels at blades, gems and runes. Every weapon runs along the sprite's diagonal, grip at the lower left, head at the upper right.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import wave1_icons as w1                                         # noqa: E402
from wave1_icons import Canvas, rgb, mix                         # noqa: E402

ICONS = w1.ICONS


def demon_shovel():
    cv = Canvas()
    cv.line(4, 11, 9, 6, (150, 78, 44), (180, 100, 56))            # the brown shaft
    cv.line(5, 11, 9, 7, (112, 56, 34))
    cv.line(2, 14, 4, 12, (236, 236, 244), (200, 204, 220))        # the white D handle
    cv.put(1, 13, (236, 236, 244))
    cv.put(3, 14, (236, 236, 244))
    cv.put(9, 6, (160, 164, 176))                                  # the steel ferrule
    for (x, y) in ((10, 5), (11, 5), (12, 4), (11, 4), (12, 3), (13, 3), (14, 2), (13, 2), (12, 2), (11, 3), (10, 4), (13, 4), (14, 3), (13, 1), (12, 1)):
        cv.put(x, y, (190, 110, 236))                              # the lavender spade blade
    for (x, y) in ((13, 3), (14, 2), (12, 2), (13, 2)):
        cv.put(x, y, (232, 170, 255))
    for (x, y) in ((14, 4), (15, 3), (15, 1), (14, 0)):            # the dark jagged teeth
        cv.put(x, y, (46, 20, 60))
    return cv, (30, 10, 44)


def dragoons_halberd():
    cv = Canvas()
    cv.line(1, 15, 13, 3, (36, 38, 48), (60, 62, 76))              # the black pole
    cv.put(0, 15, (214, 176, 60))                                  # the gold butt cap
    cv.put(1, 14, (70, 110, 200))
    cv.put(9, 7, (220, 180, 64))                                   # the gold bands
    for (x, y) in ((7, 8), (6, 8), (8, 9), (7, 9), (9, 9), (8, 10)):  # the gold crescent axe blade below the head
        cv.put(x, y, (226, 186, 70))
    cv.put(10, 9, (200, 160, 50))
    for (x, y) in ((11, 4), (10, 3), (10, 2)):                     # the upper gold spur
        cv.put(x, y, (226, 186, 70))
    cv.put(12, 4, (60, 100, 190))                                  # the blue dragon head
    cv.put(13, 3, (80, 130, 220))
    cv.put(14, 2, (210, 216, 228))                                 # the steel tip
    cv.put(15, 1, (240, 244, 255))
    cv.put(15, 0, (255, 255, 255))
    return cv, (18, 18, 26)


def efreetis_kusarigama():
    cv = Canvas()
    cv.line(4, 12, 9, 7, (120, 36, 36), (80, 24, 28))              # the dark red handle
    for (x, y) in ((10, 6), (10, 5), (11, 3), (12, 2), (11, 2), (13, 2), (14, 3), (14, 4), (15, 5), (13, 3)):   # the curved flame sickle
        cv.put(x, y, (255, 140, 30))
    for (x, y) in ((12, 2), (13, 3), (14, 4)):
        cv.put(x, y, (255, 214, 90))
    cv.put(10, 4, (60, 40, 40))                                    # the dark back of the blade
    for (x, y) in ((3, 12), (2, 12), (1, 11), (1, 10), (2, 9), (3, 9), (3, 10), (2, 13), (1, 14)):   # the grey chain
        cv.put(x, y, (110, 112, 124) if (x + y) % 2 else (70, 72, 84))
    cv.put(0, 15, (255, 120, 20))                                  # the ember counterweight
    cv.put(0, 14, (255, 190, 70))
    return cv, (24, 8, 8)


def ilmare_gin_the_starlight_silver():
    cv = Canvas()
    for k in range(0, 11):                                         # the slim midnight-blue blade with a curve at the tip
        x, y = 5 + k, 10 - k
        cv.put(x, y, (24, 34, 86))
        cv.put(x, y - 1, (60, 84, 170))
        if k < 10:
            cv.put(x + 1, y, (14, 18, 52))
    for (x, y) in ((7, 8), (9, 6), (10, 4), (12, 3), (13, 1), (8, 6), (11, 5)):   # the starlight points
        cv.put(x, y, (220, 240, 255))
    cv.put(15, 0, (150, 190, 255))
    cv.line(2, 13, 5, 10, (28, 28, 34), (50, 50, 60))              # the plain black hilt
    cv.put(5, 9, (60, 60, 70))
    cv.put(1, 14, (40, 40, 50))
    return cv, (8, 10, 24)


def katana_of_carnage():
    cv = Canvas()
    for k in range(0, 10):                                         # the grey blade
        x, y = 6 + k, 9 - k
        cv.put(x, y, (170, 176, 188))
        cv.put(x, y - 1, (230, 234, 244))
        if k < 9:
            cv.put(x + 1, y, (104, 110, 124))
    cv.put(15, 0, (246, 248, 255))
    cv.line(4, 11, 6, 9, (30, 30, 36), (60, 60, 70))               # the black guard
    cv.line(3, 12, 1, 14, (190, 24, 52), (130, 14, 36))            # the crimson diamond wrap
    cv.put(2, 12, (60, 12, 24))
    cv.put(3, 14, (200, 40, 60))                                   # the cord and blood charm
    cv.put(4, 15, (230, 60, 70))
    cv.put(5, 15, (150, 20, 30))
    return cv, (24, 10, 16)


def katana_of_the_crimson_shogun():
    cv = Canvas()
    for k in range(0, 11):                                         # the long straight folded-steel blade
        x, y = 5 + k, 10 - k
        cv.put(x, y, (176, 182, 194))
        cv.put(x, y - 1, (236, 240, 250))
        if k < 10:
            cv.put(x + 1, y, (110, 116, 130))
    cv.put(15, 0, (255, 255, 255))
    cv.line(4, 11, 6, 9, (216, 170, 60), (180, 134, 40))           # the gold fittings
    cv.line(2, 13, 4, 11, (150, 30, 54), (96, 18, 38))             # the crimson diamond wrap
    cv.put(3, 12, (40, 8, 20))
    cv.put(1, 14, (216, 170, 60))
    return cv, (26, 20, 24)


def lightworkers_scepter():
    cv = Canvas()
    cv.line(2, 14, 11, 5, (232, 232, 240), (176, 180, 196))        # the white-wrapped shaft
    cv.put(4, 12, (196, 130, 60))                                  # the bronze collars
    cv.put(9, 7, (196, 130, 60))
    cv.put(1, 15, (210, 240, 255))
    for (x, y) in ((10, 3), (11, 2), (12, 3), (12, 4), (11, 4), (13, 5), (9, 3), (10, 2)):   # the pale-blue feathered wings
        cv.put(x, y, (120, 206, 236))
    for (x, y) in ((11, 3), (12, 4)):
        cv.put(x, y, (200, 240, 255))
    for (x, y) in ((13, 2), (14, 1), (14, 3), (15, 2), (13, 1)):   # the crystal shards
        cv.put(x, y, (230, 248, 255))
    cv.put(15, 0, (255, 255, 255))
    return cv, (30, 56, 80)


def mjolnir():
    cv = Canvas()
    cv.line(2, 14, 9, 7, (120, 70, 50), (150, 92, 64))             # the brown leather handle
    cv.put(4, 12, (70, 40, 34))
    cv.put(6, 10, (70, 40, 34))
    for y in range(1, 7):                                          # the grey knotwork block
        for x in range(8, 15):
            cv.put(x, y, (150, 160, 172) if (x + y) % 3 else (110, 120, 134))
    for x in range(8, 15):
        cv.put(x, 1, (196, 206, 216))
    for y in range(1, 7):
        cv.put(14, y, (88, 96, 110))
    cv.put(11, 3, (214, 224, 236))
    cv.put(10, 4, (214, 224, 236))
    return cv, (30, 34, 40)


def overprotective_halberd():
    cv = Canvas()
    cv.line(1, 15, 12, 4, (92, 70, 66), (64, 46, 46))              # the dark wood shaft
    cv.put(0, 15, (190, 194, 204))                                 # the silver bands
    cv.put(4, 12, (200, 204, 214))
    cv.put(9, 7, (200, 204, 214))
    for (x, y) in ((11, 7), (12, 6), (13, 5), (13, 6), (12, 7), (14, 6), (13, 7), (14, 5), (12, 8)):   # the ragged axe blade
        cv.put(x, y, (170, 176, 188))
    cv.put(13, 6, (60, 64, 76))
    for (x, y) in ((10, 3), (9, 3), (9, 2)):
        cv.put(x, y, (176, 182, 194))
    cv.put(13, 3, (210, 216, 228))                                 # the spear tip
    cv.put(14, 2, (236, 240, 250))
    cv.put(15, 1, (255, 255, 255))
    for (x, y) in ((6, 6), (5, 5), (4, 5), (3, 6), (2, 7), (3, 8), (2, 9), (2, 10)):   # the long red ribbons
        cv.put(x, y, (214, 36, 44))
    return cv, (30, 24, 26)


def phoenix_flame():
    cv = Canvas()
    cv.line(1, 15, 9, 7, (150, 60, 40), (190, 80, 48))             # the slim copper-red shaft
    cv.put(4, 12, (140, 70, 150))
    cv.put(7, 9, (140, 70, 150))
    for (x, y) in ((9, 6), (10, 5), (11, 4), (11, 5), (12, 4), (10, 4), (9, 5), (12, 3), (13, 3), (11, 3), (14, 4)):   # the phoenix of flame
        cv.put(x, y, (255, 130, 30))
    for (x, y) in ((10, 2), (11, 2), (12, 1), (13, 1), (9, 3), (8, 4)):
        cv.put(x, y, (255, 190, 70))
    cv.put(12, 2, (255, 240, 150))
    cv.put(14, 0, (255, 220, 120))
    cv.put(10, 7, (255, 100, 30))
    return cv, (40, 10, 8)


def primal_chaos():
    cv = Canvas()
    for k in range(0, 11):                                         # the thin glowing blue-white blade
        x, y = 5 + k, 10 - k
        cv.put(x, y, (170, 214, 255) if k % 2 else (230, 244, 255))
    cv.put(15, 0, (255, 255, 255))
    cv.put(8, 5, (120, 170, 255))
    cv.put(10, 4, (120, 170, 255))
    cv.line(3, 8, 7, 12, (150, 190, 255), (90, 130, 230))          # the small guard
    cv.line(2, 13, 4, 11, (70, 100, 200), (110, 150, 240))
    for (x, y) in ((4, 14), (5, 14), (3, 15), (6, 15), (5, 15)):   # the bow-like knot
        cv.put(x, y, (250, 252, 255))
    cv.put(1, 14, (250, 252, 255))
    return cv, (10, 18, 56)


def ravens_wing():
    cv = Canvas()
    cv.line(2, 13, 12, 3, (60, 70, 96), (90, 100, 130))            # the dark-steel shaft
    for (x, y) in ((11, 4), (12, 4), (13, 3), (12, 2), (13, 2), (14, 2), (14, 1), (15, 1), (13, 1), (12, 3), (14, 3), (15, 0), (11, 3)):   # the raven's wing blade
        cv.put(x, y, (80, 96, 140))
    for (x, y) in ((14, 1), (13, 2), (15, 0)):
        cv.put(x, y, (170, 190, 230))
    for (x, y) in ((6, 9), (7, 9), (6, 10), (7, 10), (5, 11)):     # the black cloth straps
        cv.put(x, y, (18, 18, 24))
    for (x, y) in ((1, 13), (0, 13), (0, 14), (1, 15), (2, 15)):   # the crescent hook at the butt
        cv.put(x, y, (130, 140, 170))
    return cv, (12, 14, 22)


ICONS_DEF = [
    ("demon_shovel", demon_shovel, "RARE", (2, 15)),
    ("dragoons_halberd", dragoons_halberd, "RARE", (0, 14)),
    ("efreetis_kusarigama", efreetis_kusarigama, "RARE", (1, 15)),
    ("ilmare_gin_the_starlight_silver", ilmare_gin_the_starlight_silver, "RARE", (0, 15)),
    ("katana_of_carnage", katana_of_carnage, "RARE", (2, 14)),
    ("katana_of_the_crimson_shogun", katana_of_the_crimson_shogun, "UNIQUE", (0, 15)),
    ("lightworkers_scepter", lightworkers_scepter, "RARE", (0, 15)),
    ("mjolnir", mjolnir, "RARE", (1, 15)),
    ("overprotective_halberd", overprotective_halberd, "UNIQUE", (0, 14)),
    ("phoenix_flame", phoenix_flame, "RARE", (0, 15)),
    ("primal_chaos", primal_chaos, "RARE", (0, 15)),
    ("ravens_wing", ravens_wing, "RARE", (1, 14)),
]


def main(only=None):
    os.makedirs(ICONS, exist_ok=True)
    for item_id, fn, grade, gp in ICONS_DEF:
        if only and item_id not in only:
            continue
        cv, outline = fn()
        cv.outline(outline)
        cv.put(gp[0], gp[1], rgb(w1.GRADE[grade]))                  # the grade-coloured rim pixel
        cv.image().save(os.path.join(ICONS, item_id + ".png"))
        w1.write_models(item_id, False)
        print("icon", item_id)


if __name__ == "__main__":
    main(sys.argv[1:])
