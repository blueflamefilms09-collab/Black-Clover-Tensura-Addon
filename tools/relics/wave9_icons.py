"""Relics of Many Worlds, second card set, wave 9: the 16x16 inventory icons and the item models of the ten hand-held weapons.

    python3 -B tools/relics/wave9_icons.py [id ...] [--sheet PATH]        (idempotent)

Per id it writes
  assets/nusmp/textures/item/<id>.png         the sprite (kept as the inventory icon; the weapon in hand is the 3D mesh)
  assets/nusmp/models/item/<id>_icon.json     the flat inventory model of that sprite
  assets/nusmp/models/item/<id>.json          the in-hand model: builtin/entity, drawn by client/WeaponRenderer from weapon_meshes/<id>.txt
                                              (the display transforms of the Bloodbound Katana, as every diagonal weapon of the mod)
The meshes (wave9_models.py) are placed on the diagonal of these sprites, so every icon runs from its grip at the lower left to its tip at
the upper right. Drawn from primitives with a one pixel dark outline, a grade-coloured pixel at the grip and glow pixels on gems and runes.
"""
import json
import math
import os
import sys

import wave1_icons as w1
from wave1_icons import Canvas, rgb, shade, mix, diag_blade, GRADE, ASSETS, ICONS, MODELS

HERE = os.path.dirname(os.path.abspath(__file__))


def bez(p0, p1, p2, n=14):
    out = []
    for i in range(n + 1):
        t = i / n
        out.append(((1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0], (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1]))
    return out


def poly_line(cv, pts, c, c2=None):
    for i in range(len(pts) - 1):
        cv.line(pts[i][0], pts[i][1], pts[i + 1][0], pts[i + 1][1], c, c2)


# ---------------------------------------------------------------- the ten icons (each returns a Canvas and the outline colour)
def hyperborean_crossbow():
    cv = Canvas()
    grey, hi, lo = (150, 156, 170), (214, 220, 232), (84, 90, 106)
    limb, limb2 = (56, 60, 72), (92, 98, 112)
    blue, glow = (90, 170, 255), (190, 235, 255)
    cv.line(2, 14, 11, 5, grey, hi)                                    # the long silvery stock
    cv.line(3, 14, 12, 5, lo)
    cv.line(1, 15, 2, 14, (110, 116, 132))                             # faceted pommel
    cv.put(0, 15, lo)
    poly_line(cv, bez((7, 1), (15, 1), (15, 9), 12), limb, limb2)       # the curved limbs, swept forward
    cv.put(12, 4, hi)
    cv.line(7, 1, 15, 9, blue)                                         # the glowing string
    cv.line(7, 8, 10, 5, glow)                                         # the ice-crystal bolt
    cv.put(11, 4, (255, 255, 255))
    cv.put(5, 10, blue)
    return cv, (18, 24, 40)


def druidic_blade():
    cv = Canvas()
    teal, hi, lo = (46, 160, 176), (120, 224, 232), (20, 84, 110)
    cv.line(1, 14, 3, 12, (34, 34, 44))                                # the dark grip
    cv.put(1, 15, (230, 238, 248))                                     # the white crystal pommel
    cv.put(0, 14, (200, 214, 232))
    cv.line(2, 11, 4, 13, lo, teal)                                    # the swept guard
    cv.line(4, 11, 13, 2, teal, hi)                                    # the leaf-shaped blade, widest in the middle
    cv.line(5, 11, 13, 3, lo)
    cv.line(3, 10, 11, 2, hi)
    cv.line(6, 8, 11, 3, (80, 255, 236))                               # the glowing rune strip
    cv.put(8, 5, (220, 255, 250))
    cv.put(13, 2, (230, 255, 252))
    cv.put(2, 5, (60, 190, 70))                                        # the grass round it
    cv.put(2, 6, (60, 190, 70))
    cv.put(3, 4, (40, 150, 60))
    cv.put(14, 11, (60, 190, 70))
    cv.put(14, 12, (40, 150, 60))
    cv.put(13, 13, (60, 190, 70))
    return cv, (8, 22, 34)


def axe_of_the_exile():
    cv = Canvas()
    bone, bonehi, bonelo = (222, 214, 196), (248, 244, 232), (150, 140, 124)
    head, head2 = (104, 56, 40), (150, 86, 56)
    cord = (110, 70, 44)
    cv.line(2, 14, 11, 5, bone, bonehi)                                # the pale carved haft
    cv.line(3, 14, 12, 5, bonelo)
    for k in range(4, 9, 2):                                           # the criss-cross leather binding
        cv.put(k, 15 - k + 1, cord)
        cv.put(k + 1, 15 - k - 1, cord)
    for (x, y) in ((10, 1), (11, 1), (12, 2), (11, 2), (12, 3), (13, 4), (11, 3), (10, 2), (10, 3), (10, 4), (12, 4), (11, 4)):   # the leather-wrapped head
        cv.put(x, y, head)
    for (x, y) in ((9, 1), (9, 2), (8, 3), (9, 3), (8, 4), (8, 5)):
        cv.put(x, y, head2)
    cv.put(8, 6, head2)
    cv.put(10, 5, head)
    cv.put(2, 15, (96, 60, 40))                                        # feathers, claws and cords on the pommel
    cv.put(1, 14, (150, 106, 70))
    cv.put(0, 13, (96, 60, 40))
    cv.put(1, 13, (230, 226, 214))
    cv.put(3, 15, (200, 196, 186))
    return cv, (36, 22, 18)


def astralaria():
    cv = Canvas()
    shaft, shaft2 = (36, 38, 66), (70, 72, 120)
    brass, brasshi, brasslo = (192, 142, 62), (240, 210, 130), (120, 84, 36)
    sky, sky2 = (40, 44, 130), (110, 80, 210)
    cv.line(1, 14, 10, 5, shaft, shaft2)                               # the long dark shaft, filled with the night
    cv.put(4, 11, (255, 255, 255))
    cv.put(7, 8, (200, 190, 255))
    cv.line(10, 1, 14, 5, brass, brasshi)                              # the brass head: a lens ring and the beak-like blade
    cv.line(11, 1, 14, 4, brasslo)
    cv.disc(10.5, 4.5, 1.8, sky, sky2)                                 # the lens
    cv.ring(10.5, 4.5, 1.7, 2.6, brass)
    cv.put(10, 4, (255, 255, 255))
    for (x, y) in ((12, 5), (12, 6), (13, 6), (13, 7), (12, 7), (11, 7), (11, 6)):                  # the cosmic blade
        cv.put(x, y, sky)
    cv.put(13, 5, sky2)
    cv.put(14, 6, (150, 130, 255))
    cv.put(13, 8, (150, 130, 255))
    cv.line(2, 15, 3, 13, brass)                                       # the gear at the butt
    cv.put(2, 13, brasshi)
    return cv, (14, 14, 44)


def null_blade():
    cv = Canvas()
    black, hi, lo = (30, 28, 40), (84, 88, 108), (14, 12, 20)
    silver = (190, 200, 214)
    cv.line(1, 14, 3, 12, (40, 36, 52))                                # the grip
    cv.put(0, 15, silver)
    cv.line(4, 14, 2, 12, black)                                       # the thorned cross, horns sweeping up
    cv.line(1, 11, 5, 15, black, hi)
    cv.put(1, 10, silver)
    cv.put(6, 15, silver)
    cv.put(3, 9, black)
    cv.put(7, 13, black)
    cv.line(4, 11, 14, 1, black, hi)                                   # the jagged black blade, silver filigree down the middle
    cv.line(5, 11, 14, 2, lo)
    cv.line(5, 10, 13, 2, hi)
    for k in range(7, 13, 2):
        cv.put(k, 15 - k - 1, silver)
    cv.put(4, 12, (230, 40, 56))                                       # the eye in the cross
    cv.put(3, 12, (170, 20, 40))
    cv.put(14, 1, (210, 224, 240))
    return cv, (6, 8, 14)


def viper_whip():
    cv = Canvas()
    scale, hi, lo = (92, 100, 52), (140, 150, 84), (46, 52, 30)
    handle = (50, 38, 34)
    cv.line(1, 14, 4, 11, handle, (84, 66, 56))                        # the wrapped handle
    cv.put(0, 15, (70, 220, 110))                                      # the green pommel gem
    for ring_r, n, phase in ((5.2, 22, 0.0), (3.3, 14, 1.1)):          # two loops of the lash round an empty centre, a diamond lattice of light and dark
        for i in range(n):
            a = phase + i * 2 * math.pi / n
            cv.put(9 + ring_r * math.cos(a), 7.5 + ring_r * math.sin(a) * 0.9, hi if i % 4 == 0 else scale if i % 2 else lo)
    cv.line(4, 11, 7, 11, lo, scale)                                   # the lash leaves the handle
    cv.disc(12.4, 2.6, 1.3, (76, 196, 100), (40, 140, 70))             # the viper's head
    cv.put(13, 1, (255, 60, 70))                                       # the tongue
    cv.put(11, 2, (230, 250, 230))
    return cv, (22, 28, 14)


def caesars_gladius():
    cv = Canvas()
    steel, hi, lo = (176, 180, 190), (232, 236, 244), (100, 104, 118)
    red, red2 = (116, 56, 52), (168, 96, 84)
    cv.line(1, 14, 4, 11, (230, 226, 214), (140, 130, 110))            # the cord-wrapped grip
    cv.put(0, 15, (130, 60, 56))                                       # the helmet-crest pommel
    cv.put(0, 14, (90, 40, 40))
    for (x, y) in ((3, 9), (4, 9), (5, 9), (5, 10), (6, 10), (4, 8), (5, 8), (6, 9), (7, 11), (6, 11), (7, 12), (6, 12), (5, 12), (8, 12), (4, 10)):   # the fanned feather guard
        cv.put(x, y, red if (x + y) % 2 else red2)
    cv.line(6, 10, 13, 3, steel, hi)                                   # the broad marbled blade
    cv.line(7, 10, 13, 4, steel)
    cv.line(7, 11, 14, 4, lo)
    cv.line(5, 9, 12, 2, hi)
    cv.put(9, 6, lo)
    cv.put(11, 4, lo)
    cv.put(14, 3, hi)
    return cv, (30, 30, 44)


def twin_swords_of_deflection():
    cv = Canvas()
    white, hi = (226, 228, 240), (252, 252, 255)
    lav, lav2 = (112, 100, 188), (160, 148, 226)
    navy = (36, 40, 90)
    for (ox, oy) in ((0, 0), (3, -3)):                                 # a matched pair, side by side, the blades curling like hooks
        cv.line(1 + ox, 15 + oy, 3 + ox, 13 + oy, navy)
        cv.put(0 + ox, 15 + oy if oy == 0 else 15, (190, 194, 206))
        cv.line(3 + ox, 12 + oy, 5 + ox, 14 + oy, (130, 134, 150))      # the guard
        pts = bez((4 + ox, 12 + oy), (9 + ox, 7 + oy), (11 + ox, 3 + oy), 8)
        poly_line(cv, pts, white, hi)
        poly_line(cv, [(x + 1, y + 1) for (x, y) in pts], lav, lav2)    # the lavender inlay
    cv.put(13, 2, white)
    cv.put(12, 1, hi)
    cv.put(11, 1, lav2)
    return cv, (24, 30, 64)


def hunger():
    cv = Canvas()
    bone, bonehi, bonelo = (206, 208, 214), (244, 246, 250), (120, 122, 134)
    wood, wood2 = (110, 70, 46), (160, 110, 74)
    cv.line(1, 14, 10, 5, wood, wood2)                                 # the long brown haft with grey bindings
    cv.put(3, 12, bonelo)
    cv.put(4, 12, bone)
    cv.put(5, 10, bonelo)
    cv.put(2, 15, bone)                                                # the pointed butt
    cv.put(1, 15, bonelo)
    for (x, y) in ((9, 4), (10, 3), (11, 2), (10, 4), (11, 3), (12, 2), (11, 4), (12, 3), (13, 3), (12, 4), (13, 4), (14, 4)):          # the head: pale bone ...
        cv.put(x, y, bone if (x + y) % 3 else bonehi)
    for (x, y) in ((10, 0), (11, 1), (12, 1), (9, 2), (8, 3)):         # ... crown spikes on top
        cv.put(x, y, bonehi)
    for (x, y) in ((11, 5), (12, 5), (13, 5), (12, 6), (13, 6), (14, 6), (13, 7), (14, 7), (15, 8)):  # the toothed blade that hangs below
        cv.put(x, y, bone if (x + y) % 2 else bonelo)
    cv.erase_disc(11.6, 5.6, 0.7)                                      # the bite
    cv.put(11, 4, (208, 50, 56))                                       # a drop of what it eats
    return cv, (36, 36, 46)


def thornblight():
    cv = Canvas()
    vine, vine2 = (96, 70, 46), (140, 104, 66)
    green, glow = (44, 190, 120), (170, 255, 210)
    blade, blade2 = (34, 36, 44), (70, 74, 88)
    cv.line(2, 14, 11, 5, vine, vine2)                                 # the twisted thorny haft
    cv.line(3, 14, 12, 5, vine)
    for (x, y) in ((4, 11), (6, 9), (8, 7), (9, 5), (5, 12), (7, 10)):                              # glowing thorns
        cv.put(x, y, green)
    cv.put(3, 10, glow)
    cv.put(9, 9, green)
    cv.put(12, 8, glow)
    for (x, y) in ((8, 1), (9, 1), (10, 1), (7, 2), (8, 2), (9, 2), (10, 2), (11, 2), (8, 3), (9, 3), (10, 3), (11, 3), (9, 4), (10, 4), (11, 4), (12, 3), (12, 4)):   # the ragged leaf blade
        cv.put(x, y, blade if (x + y) % 3 else blade2)
    for (x, y) in ((6, 2), (7, 3), (8, 4), (8, 0), (11, 0), (13, 3), (13, 4), (12, 5)):               # its green edge glow
        cv.put(x, y, green)
    cv.put(1, 15, vine)                                                # the bark-like butt
    cv.put(2, 15, (70, 50, 34))
    return cv, (10, 22, 18)


ICONS_DEF = [
    # id, drawer, grade, the pixel for the grade (x, y)
    ("hyperborean_crossbow", hyperborean_crossbow, "UNIQUE", (0, 14)),
    ("druidic_blade", druidic_blade, "RARE", (0, 13)),
    ("axe_of_the_exile", axe_of_the_exile, "SPECIAL", (1, 15)),
    ("astralaria", astralaria, "LEGEND", (1, 15)),
    ("null_blade", null_blade, "LEGEND", (1, 15)),
    ("viper_whip", viper_whip, "RARE", (0, 14)),
    ("caesars_gladius", caesars_gladius, "RARE", (1, 15)),
    ("twin_swords_of_deflection", twin_swords_of_deflection, "RARE", (0, 14)),
    ("hunger", hunger, "LEGEND", (0, 14)),
    ("thornblight", thornblight, "UNIQUE", (0, 15)),
]


def main(args):
    sheet = None
    if "--sheet" in args:
        i = args.index("--sheet")
        sheet = args[i + 1]
        args = args[:i] + args[i + 2:]
    only = set(args)
    os.makedirs(ICONS, exist_ok=True)
    made = []
    for item_id, fn, grade, gp in ICONS_DEF:
        if only and item_id not in only:
            continue
        cv, outline = fn()
        cv.outline(outline)
        gx, gy = gp
        cv.put(gx, gy, rgb(GRADE[grade]))                              # the grade-coloured rim pixel
        img = cv.image()
        img.save(os.path.join(ICONS, item_id + ".png"))
        w1.write_models(item_id, False)                                # the diagonal display of the Bloodbound Katana
        made.append((item_id, img))
        print("icon", item_id)
    if sheet:
        from PIL import Image
        k = 12
        sh = Image.new("RGBA", (5 * (16 * k + 8) + 8, 2 * (16 * k + 8) + 8), (70, 72, 82, 255))
        for n, (item_id, img) in enumerate(made):
            sh.alpha_composite(img.resize((16 * k, 16 * k), Image.NEAREST), (8 + (n % 5) * (16 * k + 8), 8 + (n // 5) * (16 * k + 8)))
        sh.save(sheet)


if __name__ == "__main__":
    main(sys.argv[1:])
