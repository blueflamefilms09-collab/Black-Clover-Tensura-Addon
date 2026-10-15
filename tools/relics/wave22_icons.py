#!/usr/bin/env python3
"""Relics of Many Worlds, part 4, wave 22: the 16x16 inventory icons and the item models of the twelve staves and wands.

    python3 -B tools/relics/wave22_icons.py [id ...]       (idempotent)

Writes, per id,
  assets/nusmp/textures/item/<id>.png        the sprite: the old-style inventory icon (kept as the icon; the item itself is 3D in hand)
  assets/nusmp/models/item/<id>_icon.json    the flat inventory model of that sprite
  assets/nusmp/models/item/<id>.json         the in-hand model: builtin/entity, drawn by client/WeaponRenderer from weapon_meshes/<id>.txt
                                             (the display transforms of the Bloodbound Katana, like the other diagonal weapons)
Every icon is drawn along the diagonal from the lower left (the butt) to the upper right (the head); the grade shows as one coloured pixel at the
butt, the glow as bright pixels at the crystals, flames and gems; then a one pixel dark outline.
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from wave1_icons import Canvas, rgb, shade, mix, GRADE, ICONS, MODELS, S, write_models   # noqa: E402
from wave10_icons import band                                                              # noqa: E402


def shaft(cv, p0, p1, w, light, dark, t0=0.0, t1=1.0):
    """A straight shaft of half width w: the light side on the upper left, the dark side on the lower right."""
    band(cv, p0, p1, lambda t: w, lambda t, s: light if s < 0.15 else dark, t0, t1)


def dreamcore_woven_wand():
    cv = Canvas()
    wood, wood2, red, red2 = (122, 84, 60), (84, 56, 40), (170, 40, 70), (118, 24, 50)
    shaft(cv, (2, 14), (12, 4), 0.55, wood, wood2)
    band(cv, (6, 10), (9, 7), lambda t: 1.05, lambda t, s: red if (int(t * 6) % 2 == 0) else red2, 0.0, 1.0)       # the crimson wrap
    for (x, y) in ((11, 5), (10, 4), (12, 6), (13, 3), (11, 2)):                                                      # twig tendrils round the crystal
        cv.put(x, y, wood2)
    cv.put(2, 14, wood2)
    cv.put(12, 4, (255, 150, 200))                                                                                    # the crystal: pink, blue, white
    cv.put(12, 3, (140, 200, 255))
    cv.put(13, 4, (255, 255, 255))
    cv.put(13, 2, (255, 170, 210))
    cv.put(12, 2, (150, 190, 255))
    cv.put(11, 3, (255, 255, 255))
    return cv, (40, 24, 28)


def lich_wand():
    cv = Canvas()
    bone, bone2, skull = (160, 148, 190), (104, 92, 128), (40, 36, 44)
    flame, flame2 = (140, 255, 160), (70, 200, 110)
    shaft(cv, (5, 11), (13, 3), 0.75, bone, bone2)
    cv.put(13, 2, (200, 190, 224))                                                                                    # the bone knob
    cv.put(14, 3, (200, 190, 224))
    cv.put(14, 2, bone)
    for (x, y) in ((5, 11), (4, 12), (3, 11), (4, 10)):                                                               # dark skull beads
        cv.put(x, y, skull)
    cv.put(4, 11, (210, 214, 170))
    for k, (x, y) in enumerate(((2, 13), (1, 14), (3, 14), (2, 15), (1, 12))):                                        # the clawed hand
        cv.put(x, y, (60, 52, 66) if k % 2 else (96, 84, 108))
    for (x, y) in ((9, 3), (11, 2), (7, 7), (13, 5), (10, 9), (12, 7), (6, 6)):                                       # pale green flame licking it
        cv.put(x, y, flame if (x + y) % 2 else flame2)
    return cv, (22, 30, 28)


def scepter_of_the_double_moon():
    cv = Canvas()
    wood, wood2, gold, gold2 = (150, 100, 66), (96, 62, 40), (240, 200, 90), (170, 120, 44)
    shaft(cv, (2, 14), (10, 6), 0.6, wood, wood2)
    for k in (1, 3, 5):
        cv.put(2 + k, 14 - k, gold2)                                                                                  # the collars
    cv.disc(11.5, 4.5, 2.3, (176, 192, 220), (116, 132, 168))                                                         # the marble orb
    cv.put(10, 3, (230, 238, 252))
    for (x, y) in ((10, 6), (11, 6), (12, 6), (13, 5)):
        cv.put(x, y, (210, 50, 60))                                                                                   # the ruby band
    for (x, y) in ((9, 2), (9, 1), (10, 0), (11, 0)):                                                                 # the two brass crescents
        cv.put(x, y, gold)
    for (x, y) in ((14, 1), (15, 2), (15, 3), (13, 0)):
        cv.put(x, y, gold2)
    cv.put(12, 1, (255, 240, 170))
    return cv, (48, 36, 30)


def serpant_head_staff():
    cv = Canvas()
    sil, sil2, cy = (190, 196, 206), (108, 114, 128), (110, 230, 255)
    shaft(cv, (2, 14), (11, 5), 0.7, sil, sil2)
    for k in range(2, 9, 2):
        cv.put(2 + k, 14 - k, sil2)                                                                                   # scale rings
    cv.put(8, 8, cy)                                                                                                  # the central gem
    for (x, y) in ((11, 4), (12, 3), (13, 2), (10, 3), (9, 2), (13, 4), (14, 5), (11, 2)):                            # forking snake heads
        cv.put(x, y, sil if (x + y) % 2 else sil2)
    for (x, y) in ((10, 1), (14, 3), (12, 1), (15, 4)):                                                               # an icy crystal in each mouth
        cv.put(x, y, cy)
    cv.put(2, 14, sil2)
    cv.put(1, 13, sil2)
    cv.put(1, 14, sil)
    return cv, (30, 36, 52)


def staff_of_blood_magic():
    cv = Canvas()
    bone, bone2, red = (170, 168, 176), (96, 94, 104), (255, 60, 60)
    shaft(cv, (2, 14), (7, 9), 0.7, bone, bone2)
    shaft(cv, (8, 8), (11, 5), 0.8, bone, bone2)
    cv.put(7, 9, bone2)
    cv.disc(12.2, 3.8, 1.5, red, (200, 20, 30))                                                                       # the red orb
    cv.put(11, 3, (255, 200, 190))
    for (x, y) in ((10, 1), (11, 0), (12, 0), (14, 1), (14, 2), (15, 4), (10, 5), (9, 4)):                            # broken crescent shards
        cv.put(x, y, bone)
    cv.put(13, 6, bone2)
    cv.put(15, 6, bone2)
    return cv, (36, 20, 28)


def staff_of_ember_visions():
    cv = Canvas()
    blk, blk2, ember, ember2, rib = (56, 48, 62), (30, 26, 36), (255, 150, 56), (224, 90, 30), (190, 40, 60)
    shaft(cv, (2, 14), (11, 5), 0.5, blk, blk2)
    cv.disc(12.0, 3.6, 1.7, ember, ember2)                                                                            # the ember bud
    cv.put(11, 2, (255, 230, 150))
    cv.put(13, 5, blk2)
    for (x, y) in ((13, 2), (14, 1), (15, 1), (15, 2), (15, 3), (14, 4), (14, 5)):                                    # the long crimson ribbon
        cv.put(x, y, rib)
    for (x, y) in ((8, 3), (7, 3), (9, 4)):                                                                           # the pale flame-bird
        cv.put(x, y, (250, 236, 210))
    cv.put(7, 2, (250, 236, 210))
    cv.put(2, 14, blk2)
    return cv, (30, 20, 26)


def staff_of_the_apprentice():
    cv = Canvas()
    wood, wood2, blue, vio = (130, 112, 96), (80, 66, 58), (120, 170, 255), (190, 140, 255)
    shaft(cv, (2, 14), (11, 5), 0.6, wood, wood2)
    for (x, y) in ((6, 10), (7, 9), (5, 11)):                                                                         # the cord and charms
        cv.put(x, y, (210, 200, 170))
    cv.put(6, 12, (230, 220, 190))
    for (x, y) in ((11, 4), (12, 3), (11, 2), (13, 4), (12, 5)):                                                      # curling wood around the crystals
        cv.put(x, y, wood2)
    for (x, y, c) in ((12, 2, blue), (13, 1, vio), (13, 3, vio), (12, 1, (230, 240, 255)), (14, 2, blue), (11, 3, blue)):
        cv.put(x, y, c)
    for (x, y) in ((10, 1), (14, 0), (15, 3)):
        cv.put(x, y, (200, 240, 255))                                                                                 # sparkles
    return cv, (44, 36, 40)


def superlunary_staff():
    cv = Canvas()
    bronze, bronze2, gold, gold2 = (150, 100, 52), (96, 62, 32), (250, 214, 100), (180, 136, 50)
    shaft(cv, (2, 14), (10, 6), 0.5, bronze, bronze2)
    for k in (2, 4):
        cv.put(2 + k, 14 - k, gold2)
    cv.ring(12.0, 4.0, 2.3, 3.1, gold)                                                                                # the armillary ring
    for (x, y) in ((10, 3), (14, 5), (12, 1), (12, 7)):
        cv.put(x, y, gold2)
    cv.put(12, 4, (255, 170, 70))                                                                                     # the little sun
    cv.put(11, 4, (255, 220, 120))
    cv.put(12, 3, (255, 130, 50))
    cv.put(13, 4, (255, 200, 90))
    return cv, (44, 30, 22)


def the_bifrost():
    cv = Canvas()
    wh, wh2, gold = (244, 240, 228), (170, 166, 150), (240, 200, 96)
    shaft(cv, (2, 14), (12, 4), 0.5, wh, wh2)
    for k in (2, 5, 8):
        cv.put(2 + k, 14 - k, gold)
    cv.put(12, 3, (214, 220, 232))                                                                                    # the silver finial
    cv.put(13, 2, (240, 244, 252))
    rain = [(255, 90, 90), (255, 170, 70), (255, 240, 100), (110, 230, 120), (100, 190, 255), (180, 120, 255)]
    for i, (x, y) in enumerate(((11, 5), (10, 6), (9, 7), (7, 9), (6, 10), (5, 11), (4, 12), (3, 13), (12, 6), (8, 10), (6, 8), (10, 3))):
        cv.put(x, y, rain[i % 6])
    return cv, (60, 54, 56)


def wand_o_wisp():
    cv = Canvas()
    sil, sil2, teal, teal2 = (214, 220, 228), (130, 140, 154), (96, 220, 212), (40, 150, 150)
    shaft(cv, (9, 7), (14, 2), 0.35, sil, sil2)
    band(cv, (3, 13), (9, 7), lambda t: 1.15, lambda t, s: teal if s < 0.1 else teal2, 0.0, 1.0)                      # the hexagonal teal grip
    cv.put(2, 14, sil2)
    cv.put(3, 14, sil)
    cv.put(2, 13, sil)
    cv.put(9, 6, sil)
    for (x, y) in ((6, 5), (5, 4), (7, 4), (11, 9), (12, 10), (11, 10)):                                             # two little wisps
        cv.put(x, y, (200, 240, 255))
    cv.put(6, 4, (255, 255, 255))
    cv.put(11, 11, (160, 220, 250))
    return cv, (30, 50, 60)


def wand_of_spider_summoning():
    cv = Canvas()
    blk, blk2, pink, pink2 = (50, 44, 58), (24, 20, 30), (255, 90, 210), (190, 40, 150)
    band(cv, (3, 13), (8, 8), lambda t: 0.9, lambda t, s: blk if s < 0.1 else blk2, 0.0, 1.0)                       # the spider-leg grip
    shaft(cv, (8, 8), (14, 2), 0.45, pink, pink2)                                                                    # the long thorny blade
    for (x, y) in ((9, 7), (11, 5), (13, 3), (10, 5), (12, 3)):
        cv.put(x, y, (255, 200, 245))
    for (x, y) in ((8, 6), (7, 6), (9, 9), (10, 9), (8, 10)):                                                        # the claws of the guard
        cv.put(x, y, blk2)
    for (x, y) in ((2, 14), (1, 13), (1, 14), (2, 15)):                                                              # the curved claw pommel
        cv.put(x, y, pink if (x + y) % 2 else pink2)
    cv.put(8, 8, (255, 120, 220))
    return cv, (36, 14, 40)


def windwheel_staff():
    cv = Canvas()
    red, red2, steel, steel2, gold = (150, 60, 50), (96, 34, 30), (90, 104, 140), (50, 60, 90), (240, 200, 90)
    shaft(cv, (2, 14), (10, 6), 0.55, red, red2)
    cv.put(3, 13, gold)
    cv.put(4, 12, gold)
    cv.ring(11.5, 4.5, 2.6, 3.4, steel)                                                                               # the blue-steel ring
    for (x, y) in ((8, 5), (15, 4), (11, 1), (12, 8)):
        cv.put(x, y, steel2)
    for (x, y, c) in ((11, 4, (255, 210, 70)), (12, 5, (255, 170, 50)), (12, 3, (255, 230, 120)), (10, 5, (255, 150, 40)), (11, 5, (255, 240, 160))):
        cv.put(x, y, c)                                                                                               # the spinning pinwheel
    for (x, y) in ((8, 7), (9, 8), (7, 8)):
        cv.put(x, y, gold)                                                                                            # the hanging gold rings
    return cv, (30, 30, 46)


ICONS_DEF = [
    ("dreamcore_woven_wand", dreamcore_woven_wand, "SPECIAL", False, (1, 15)),
    ("lich_wand", lich_wand, "RARE", False, (0, 15)),
    ("scepter_of_the_double_moon", scepter_of_the_double_moon, "LEGEND", False, (1, 15)),
    ("serpant_head_staff", serpant_head_staff, "UNIQUE", False, (0, 14)),
    ("staff_of_blood_magic", staff_of_blood_magic, "UNIQUE", False, (1, 15)),
    ("staff_of_ember_visions", staff_of_ember_visions, "RARE", False, (1, 15)),
    ("staff_of_the_apprentice", staff_of_the_apprentice, "SPECIAL", False, (1, 15)),
    ("superlunary_staff", superlunary_staff, "RARE", False, (1, 15)),
    ("the_bifrost", the_bifrost, "LEGEND", False, (1, 15)),
    ("wand_o_wisp", wand_o_wisp, "RARE", False, (1, 14)),
    ("wand_of_spider_summoning", wand_of_spider_summoning, "SPECIAL", False, (0, 14)),
    ("windwheel_staff", windwheel_staff, "RARE", False, (1, 15)),
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
