"""Relics of Many Worlds, wave 13: the 16x16 inventory icons and the item models of the nine blades.

    python3 -B tools/relics/wave13_icons.py [id ...]            (idempotent)

Per id it writes assets/nusmp/textures/item/<id>.png (the sprite, kept as the inventory icon), models/item/<id>_icon.json (the flat inventory model)
and models/item/<id>.json (the in-hand model: builtin/entity, drawn by client/WeaponRenderer from weapon_meshes/<id>.txt, with the display transforms of
the Bloodbound Katana). Icons are drawn from primitives, given a one pixel dark outline; the grade shows as a coloured pixel at the pommel and the glow
as bright pixels at gems, runes and edges.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import wave1_icons as w1                                         # noqa: E402
from wave1_icons import Canvas, rgb, mix                         # noqa: E402

ICONS, MODELS = w1.ICONS, w1.MODELS


def dream_glass():
    cv = Canvas()
    gl, hi, lo = (126, 202, 240), (214, 248, 255), (70, 140, 200)
    for k in range(0, 10):                                         # the translucent blue blade: light edge above, deep blue below, filigree dots
        x, y = 5 + k, 10 - k
        cv.put(x, y, gl)
        cv.put(x, y - 1, hi)
        if k < 9:
            cv.put(x + 1, y, lo)
    cv.put(14, 0, (240, 252, 255))
    cv.put(8, 7, (255, 255, 255))
    cv.put(10, 5, (255, 255, 255))
    gold, gold2 = (150, 140, 60), (200, 190, 96)
    cv.line(3, 8, 7, 12, gold, gold2)                              # the olive-gold crossguard with a blue gem
    cv.put(5, 10, (70, 140, 255))
    cv.line(2, 13, 4, 11, (128, 48, 58), (170, 74, 84))            # the maroon grip
    cv.disc(1.5, 14.5, 1.3, (180, 170, 70), (120, 110, 40))        # the ring pommel
    return cv, (18, 52, 96)


def axe_of_the_crimson_saint():
    cv = Canvas()
    wood, wood2 = (96, 60, 36), (52, 32, 22)
    cv.line(2, 14, 11, 5, wood, wood2)                             # the dark haft
    cv.line(3, 14, 12, 5, wood2)
    gold = (216, 168, 56)
    cv.put(9, 7, gold)
    cv.put(8, 8, (210, 60, 50))                                    # the red-streaked bands
    cv.put(6, 10, gold)
    cv.put(1, 15, (236, 226, 190))                                 # the cream wrap
    cv.put(2, 15, (200, 190, 150))
    steel, hi, lo = (150, 154, 164), (214, 218, 226), (86, 90, 104)
    for (x, y) in ((7, 2), (8, 2), (9, 2), (6, 3), (7, 3), (8, 3), (9, 3), (10, 3), (6, 4), (7, 4), (8, 4), (9, 4), (10, 4), (11, 4), (7, 5), (8, 5), (9, 5), (10, 5),
                   (8, 6), (9, 6)):
        cv.put(x, y, steel)
    for (x, y) in ((7, 2), (6, 3), (6, 4)):
        cv.put(x, y, hi)
    for (x, y) in ((10, 5), (9, 6), (11, 4)):
        cv.put(x, y, lo)
    for (x, y) in ((6, 5), (5, 6), (4, 7), (5, 5), (6, 6)):         # the long swept blade below the head
        cv.put(x, y, steel if (x + y) % 2 else hi)
    for (x, y) in ((8, 3), (7, 4), (9, 4), (6, 5), (8, 5), (5, 6)):  # blood cracks
        cv.put(x, y, (220, 40, 40))
    cv.put(11, 3, (180, 30, 30))
    return cv, (30, 18, 18)


def palladion():
    cv = Canvas()
    st, hi, lo = (176, 184, 204), (240, 244, 255), (106, 114, 144)
    for k in range(0, 10):                                         # the long silver-steel blade
        x, y = 6 + k, 9 - k
        cv.put(x, y, st)
        cv.put(x, y - 1, hi)
        if k < 9:
            cv.put(x + 1, y, lo)
    cv.put(15, 0, (255, 255, 255))
    gold = (222, 178, 70)
    for (x, y) in ((1, 7), (2, 8), (3, 9), (4, 10)):                # the grey feathered wing on one side of the guard
        cv.put(x, y, (190, 196, 210))
    for (x, y) in ((8, 13), (9, 14), (10, 15), (7, 12)):            # the gold bat-like wing on the other
        cv.put(x, y, gold)
    cv.line(4, 10, 7, 13, (40, 52, 110), (60, 74, 140))
    cv.put(5, 11, (255, 244, 200))                                 # the little angel at the centre
    cv.line(3, 14, 5, 12, (36, 46, 100), gold)                      # the navy-and-gold grip
    cv.put(2, 15, gold)
    return cv, (30, 36, 70)


def dagger_of_betrayal():
    cv = Canvas()
    pur, pur2, pink = (130, 70, 160), (84, 40, 110), (200, 110, 190)
    for k in range(0, 7):                                          # the tri-bladed spiral: three interleaved purple strands
        x, y = 7 + k, 9 - k
        cv.put(x, y, pur if k % 2 == 0 else pink)
        cv.put(x + 1, y, pur2)
        cv.put(x, y - 1, pink if k % 2 == 0 else pur)
    cv.put(13, 3, (255, 70, 80))
    cv.put(10, 6, (255, 60, 70))                                   # the crystals
    cv.put(9, 7, (255, 110, 110))
    cv.put(15, 1, (255, 130, 130))
    cv.put(14, 0, (255, 60, 70))
    cv.line(5, 8, 8, 11, (214, 170, 70), (160, 120, 40))           # the curved gold guard
    cv.line(3, 13, 6, 10, (120, 62, 44), (84, 40, 30))             # the braided leather grip
    cv.put(4, 12, (214, 170, 70))
    cv.put(1, 14, (255, 70, 90))                                   # the red crystal pommel
    cv.put(2, 14, (210, 40, 60))
    cv.put(1, 13, (255, 130, 140))
    return cv, (40, 14, 40)


def humbling_longsword():
    cv = Canvas()
    st, hi, lo = (170, 174, 184), (230, 234, 244), (100, 104, 118)
    for k in range(0, 9):
        x, y = 6 + k, 9 - k
        cv.put(x, y, st)
        cv.put(x, y - 1, hi)
        if k < 8:
            cv.put(x + 1, y, lo)
    cv.put(15, 0, hi)
    cv.line(4, 8, 8, 12, (150, 154, 164), (110, 114, 126))         # the plain straight guard
    cv.line(3, 13, 5, 11, (36, 130, 220), (24, 90, 170))           # the blue wrapped grip
    cv.disc(1.6, 14.4, 1.2, (150, 154, 164), (100, 104, 116))      # the round pommel
    return cv, (30, 34, 50)


def mountainguards_greataxe():
    cv = Canvas()
    wood, wood2 = (84, 54, 36), (50, 32, 24)
    cv.line(1, 15, 11, 5, wood, wood2)
    cv.line(2, 15, 12, 6, wood2)
    gold = (216, 150, 40)
    cv.put(1, 14, gold)
    cv.put(0, 15, gold)
    cv.put(2, 15, (130, 134, 146))
    steel, hi, lo = (156, 162, 174), (224, 228, 238), (96, 100, 114)
    for (x, y) in ((10, 1), (11, 1), (12, 1), (13, 1), (14, 2), (14, 3), (13, 3), (12, 3), (11, 3), (11, 2), (12, 2)):      # the upper crescent blade
        cv.put(x, y, steel)
    for (x, y) in ((10, 1), (11, 1), (12, 1), (13, 1)):
        cv.put(x, y, hi)
    for (x, y) in ((14, 3), (13, 3)):
        cv.put(x, y, lo)
    for (x, y) in ((11, 7), (12, 7), (13, 7), (14, 6), (14, 5), (13, 5), (12, 5), (11, 6), (12, 6), (13, 6)):               # the lower crescent blade
        cv.put(x, y, steel)
    for (x, y) in ((11, 7), (12, 7), (13, 7), (14, 6)):
        cv.put(x, y, lo)
    for (x, y) in ((11, 4), (12, 4), (13, 4), (10, 3), (10, 5), (12, 2), (12, 6)):                                         # the gold filigree between
        cv.put(x, y, gold)
    cv.put(10, 4, (110, 114, 126))
    return cv, (30, 28, 30)


def eschaton():
    cv = Canvas()
    wh, hi, lo = (214, 224, 236), (255, 255, 255), (140, 160, 190)
    for k in range(0, 10):                                         # the slender silvery rapier blade
        x, y = 6 + k, 9 - k
        cv.put(x, y, wh)
        if k % 2 == 1:
            cv.put(x, y, (96, 235, 224))                           # the cyan diamond studs
        if k < 9:
            cv.put(x + 1, y, lo)
    cv.put(15, 0, hi)
    cv.put(14, 1, hi)
    g, g2 = (62, 154, 74), (36, 100, 52)
    cv.line(3, 8, 7, 12, g, g2)                                    # the green leaf guard
    cv.put(3, 7, g)
    cv.put(8, 13, g)
    cv.put(5, 10, (120, 255, 230))
    cv.line(3, 13, 5, 11, (50, 120, 60), g)
    cv.put(2, 14, (120, 255, 230))
    return cv, (18, 52, 40)


def contingency_plan():
    cv = Canvas()
    cop, cop2 = (176, 82, 68), (84, 40, 90)
    for k in range(0, 8):                                          # the copper-red and purple-black blade
        x, y = 7 + k, 9 - k
        cv.put(x, y, cop if k % 3 else (230, 120, 90))
        cv.put(x + 1, y, cop2)
        cv.put(x, y - 1, (210, 110, 100))
    cv.put(15, 1, (255, 190, 170))
    pink = (226, 160, 200)
    cv.line(3, 7, 9, 13, pink, (150, 90, 140))                      # the winged guard
    cv.put(3, 6, pink)
    cv.put(10, 14, pink)
    cv.disc(6, 10, 1.1, (150, 236, 255), (60, 170, 230))            # the pale-blue orb
    cv.line(3, 13, 5, 11, (200, 90, 60), (230, 150, 70))
    cv.put(1, 15, (226, 160, 90))
    cv.put(2, 15, (200, 90, 60))
    cv.put(0, 14, (226, 160, 90))
    return cv, (44, 18, 44)


def time_bender_dadao():
    cv = Canvas()
    bl, bl2 = (28, 30, 38), (58, 62, 78)
    for k in range(0, 10):                                         # the broad curved black blade (a bend at two thirds) with a cyan vein
        x, y = 5 + k, 10 - k + (1 if k > 5 else 0)
        cv.put(x, y, bl)
        cv.put(x, y - 1, bl2)
        cv.put(x + 1, y, bl)
        if k > 1:
            cv.put(x, y, (63, 208, 255) if k % 2 == 0 else (150, 236, 255))
    cv.put(14, 2, bl)
    cv.put(15, 1, bl2)
    cv.line(4, 11, 6, 9, (240, 240, 246), (200, 204, 214))         # the white wrapped handle
    cv.line(2, 13, 4, 11, (240, 240, 246), (200, 204, 214))
    cv.put(3, 12, (30, 30, 40))
    cv.disc(1.3, 14.7, 1.2, (60, 62, 74), (30, 30, 40))
    for (x, y) in ((4, 8), (2, 10), (7, 12), (9, 11), (8, 5)):      # the ring of blue sparks round the guard
        cv.put(x, y, (96, 220, 255))
    return cv, (6, 8, 14)


ICONS_DEF = [
    ("dream_glass", dream_glass, "RARE", (1, 15)),
    ("axe_of_the_crimson_saint", axe_of_the_crimson_saint, "UNIQUE", (0, 15)),
    ("palladion", palladion, "LEGEND", (1, 15)),
    ("dagger_of_betrayal", dagger_of_betrayal, "RARE", (0, 15)),
    ("humbling_longsword", humbling_longsword, "RARE", (0, 15)),
    ("mountainguards_greataxe", mountainguards_greataxe, "RARE", (0, 14)),
    ("eschaton", eschaton, "LEGEND", (1, 15)),
    ("contingency_plan", contingency_plan, "UNIQUE", (0, 15)),
    ("time_bender_dadao", time_bender_dadao, "RARE", (0, 15)),
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
