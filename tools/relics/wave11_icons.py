"""Relics of Many Worlds, second set, wave 11: the 16x16 inventory icons and the item models of the ten blades.

    python3 -B tools/relics/wave11_icons.py [id ...]       (idempotent)

Same method as wave1_icons.py (primitives on a 16 x 16 canvas, one pixel dark outline, a grade-coloured pixel at the pommel, glow pixels at gems
and edges). Writes, per id, assets/nusmp/textures/item/<id>.png (the old-style sprite, kept as the inventory icon),
models/item/<id>_icon.json (the flat model of the sprite) and models/item/<id>.json (the in-hand model: builtin/entity drawn by
client/WeaponRenderer from weapon_meshes/<id>.txt, with the display transforms of the Bloodbound Katana).
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import wave1_icons as b                                                        # noqa: E402

Canvas, diag_blade, rgb, GRADE = b.Canvas, b.diag_blade, b.rgb, b.GRADE


def wavebreaker():
    cv = Canvas()
    teal, hi, lo = (70, 150, 170), (150, 214, 226), (36, 86, 112)
    pts = [(5, 10), (6, 9), (7, 8), (8, 7), (9, 6), (10, 5), (11, 3), (12, 2), (13, 1), (14, 0)]          # the very long, slightly curved blade
    for k, (x, y) in enumerate(pts):
        cv.put(x, y, teal if k % 3 else hi)
        cv.put(x + 1, y, lo)
        cv.put(x, y - 1, hi)
    cv.put(15, 0, (210, 244, 255))
    cv.put(8, 8, (90, 60, 120))                                                  # the purple tint along the lower edge
    cv.put(10, 6, (90, 60, 120))
    cv.line(2, 13, 5, 10, (40, 40, 70), (70, 70, 110))                         # dark wrapped handle
    cv.line(4, 10, 6, 12, (60, 80, 120), (150, 170, 210))                      # tsuba
    cv.put(1, 14, (96, 160, 226))                                                # the blue cord and tassel
    cv.put(0, 15, (96, 160, 226))
    cv.put(1, 15, (60, 110, 190))
    return cv, (16, 40, 66)


def encircled_blade():
    cv = Canvas()
    cy, cy_hi, cy_lo = (46, 196, 200), (170, 250, 244), (20, 120, 130)
    diag_blade(cv, 6, 9, 14, 1, cy_lo, cy, cy_hi, width=3)
    cv.put(15, 0, (220, 255, 250))
    for (x, y) in ((8, 7), (10, 5), (12, 3)):                                    # the dark oval holes along the fuller
        cv.put(x, y, (14, 40, 60))
    cv.put(11, 4, (40, 200, 140))                                                # the green gem
    cv.disc(5.0, 10.0, 1.7, (14, 14, 22), (40, 44, 64))                         # the black orb in the guard
    cv.put(4, 9, (120, 140, 190))
    cv.line(3, 12, 1, 14, (40, 80, 200), (26, 52, 150))                        # blue grip
    cv.put(0, 15, (36, 70, 190))
    return cv, (8, 36, 70)


def windcutter():
    cv = Canvas()
    sil, hi, lo = (206, 212, 232), (250, 252, 255), (130, 138, 170)
    diag_blade(cv, 6, 9, 15, 0, lo, sil, hi, width=2)
    teal = (40, 200, 190)
    for (x, y) in ((3, 9), (4, 8), (5, 7), (8, 12), (7, 13), (6, 12), (3, 10)):    # the winged guard of feather blades
        cv.put(x, y, sil if (x + y) % 2 else hi)
    cv.put(5, 10, teal)                                                          # the teal gem
    cv.put(6, 11, (200, 255, 250))
    cv.line(2, 13, 4, 11, (150, 156, 190), (200, 204, 226))                    # the wrapped grip
    cv.put(1, 14, teal)
    for (x, y) in ((9, 11), (11, 10), (12, 8), (13, 6)):                         # the ribbon winding round the blade
        cv.put(x, y, (186, 182, 220))
    return cv, (60, 64, 100)


def tripolar_sword():
    cv = Canvas()
    gr, hi, lo = (120, 122, 134), (214, 216, 230), (52, 54, 66)
    diag_blade(cv, 5, 10, 15, 0, lo, gr, hi, width=3)
    for (x, y) in ((8, 8), (11, 5), (13, 3)):                                    # the flame-like jagged edges
        cv.put(x - 1, y + 1, lo)
        cv.put(x + 1, y + 1, gr)
    for (x, y) in ((2, 9), (4, 7), (7, 12), (9, 10), (3, 12), (8, 7)):          # the four-pointed star crossguard
        cv.put(x, y, hi if (x + y) % 2 else gr)
    cv.put(5, 9, (150, 164, 220))
    cv.put(6, 10, (150, 164, 220))
    cv.line(3, 13, 1, 15, (40, 40, 54), (70, 70, 90))
    return cv, (22, 24, 36)


def the_dagger_of_many_daggers():
    cv = Canvas()
    ghost, ghost2 = (60, 74, 110), (90, 110, 150)
    for k in range(0, 6):                                                        # the ghostly fan behind
        cv.put(6 + k, 7 - k, ghost)
        cv.put(5 + k, 6 - k, ghost2) if k % 2 else None
    for (x, y) in ((4, 4), (5, 3), (3, 6), (6, 2), (9, 6), (11, 5), (12, 3)):
        cv.put(x, y, ghost)
    sil, hi, lo = (206, 212, 226), (250, 252, 255), (120, 130, 160)
    diag_blade(cv, 6, 9, 13, 2, lo, sil, hi, width=2)
    cv.put(14, 1, hi)
    gold, gold_hi = (224, 168, 48), (255, 224, 120)
    cv.ring(5.5, 10.5, 1.0, 1.9, gold)                                           # the gold ring guard
    cv.line(3, 12, 5, 10, (46, 80, 170), (80, 120, 210))                       # the blue wrapped grip
    cv.disc(2.0, 13.4, 1.3, gold, gold_hi)                                       # the ring pommel
    return cv, (22, 26, 50)


def flamecutter():
    cv = Canvas()
    red, hi, lo = (200, 70, 30), (255, 170, 60), (120, 30, 20)
    diag_blade(cv, 6, 9, 14, 1, lo, red, hi, width=3)
    cv.put(15, 0, (255, 220, 120))
    cv.put(14, 0, (255, 150, 40))                                                # a lick of fire at the tip
    cv.put(9, 5, (255, 220, 120))
    gold, gold_hi = (210, 160, 50), (255, 224, 120)
    for (x, y) in ((3, 8), (4, 7), (4, 9), (8, 13), (7, 12), (9, 12)):          # the golden horned guard
        cv.put(x, y, gold if (x + y) % 2 else gold_hi)
    cv.put(5, 10, gold)
    cv.put(6, 11, gold_hi)
    cv.line(3, 12, 1, 14, (110, 34, 40), (150, 60, 50))                        # dark red wrapped grip
    cv.put(1, 13, (255, 150, 40))                                                # the glowing gem at the pommel
    cv.put(0, 14, gold_hi)
    return cv, (60, 14, 10)


def heart_of_the_flame():
    cv = Canvas()
    y1, y2, y3 = (250, 190, 40), (255, 150, 20), (255, 232, 120)
    for (x0, y0, x1, y1_) in ((5, 10, 7, 8), (8, 7, 10, 5), (11, 4, 12, 3), (14, 1, 15, 0)):     # the floating golden plates
        cv.line(x0, y0, x1, y1_, y1, y3)
        cv.line(x0 + 1, y0, x1 + 1, y1_, y2)
    cv.put(13, 2, y3)
    cv.line(3, 12, 1, 14, (70, 74, 84), (110, 114, 126))                       # the dark chevron hilt
    for (x, y) in ((4, 9), (3, 10), (6, 11), (7, 12)):                           # silver spiked crossguard
        cv.put(x, y, (200, 206, 220))
    cv.put(5, 10, (255, 120, 40))
    cv.put(0, 15, (255, 120, 40))
    return cv, (80, 40, 10)


def bianca():
    cv = Canvas()
    wood, wood_hi, wood_lo = (140, 84, 44), (190, 126, 70), (86, 50, 26)
    cv.line(1, 14, 6, 9, wood, wood_hi)                                          # the stock
    cv.line(2, 14, 7, 9, wood_lo)
    cv.line(6, 9, 13, 2, wood, wood_hi)                                          # the body
    cv.line(7, 9, 14, 2, wood_lo)
    steel, steel_hi = (176, 182, 196), (240, 244, 252)
    for (x, y) in ((8, 2), (9, 3), (10, 3), (11, 4), (12, 5), (13, 6), (14, 7), (15, 8)):    # the fanned steel limbs
        cv.put(x, y, steel if (x + y) % 2 else steel_hi)
    for (x, y) in ((6, 2), (7, 3), (5, 4), (4, 5)):
        cv.put(x, y, steel)
    gold, gold_hi = (214, 164, 56), (255, 226, 130)
    for (x, y) in ((9, 6), (10, 5), (8, 7)):                                     # brass fittings and the coil spring
        cv.put(x, y, gold_hi if (x + y) % 2 else gold)
    cv.disc(10.5, 9.5, 1.5, (60, 64, 76), (100, 106, 120))                      # the ammo drum
    cv.put(15, 0, (230, 232, 240))                                               # the bayonet
    cv.put(14, 1, (200, 204, 214))
    return cv, (36, 22, 12)


def sorrows_edge():
    cv = Canvas()
    red, red_hi = (112, 20, 28), (160, 44, 48)
    cv.line(2, 14, 9, 7, red, red_hi)                                            # the long dark red haft
    cv.line(3, 14, 10, 7, (70, 12, 20))
    cv.put(1, 15, (60, 44, 40))                                                  # the fur tuft pommel
    cv.put(0, 15, (40, 30, 30))
    dark, star = (22, 22, 32), (110, 120, 170)
    cv.disc(10.8, 4.2, 3.3, dark, (40, 42, 60))                                  # the crescent black starry blade
    cv.erase_disc(13.4, 6.4, 2.4)
    for (x, y) in ((11, 2), (9, 4), (12, 5), (10, 3)):
        cv.put(x, y, star)
    for (x, y) in ((13, 2), (14, 3), (14, 4), (13, 6), (12, 7)):                 # the white and blue-grey edge band
        cv.put(x, y, (226, 230, 244) if (x + y) % 2 else (130, 150, 200))
    gold = (208, 170, 70)
    cv.put(8, 5, gold)                                                           # the gold filigree head
    cv.put(8, 7, gold)
    for (x, y) in ((6, 4), (5, 6), (6, 7)):                                      # the three claw spikes on the back
        cv.put(x, y, (50, 44, 40))
        cv.put(x - 1, y, (90, 80, 70))
    return cv, (14, 12, 20)


def the_dragons_truth():
    cv = Canvas()
    belt, belt_hi, belt_lo = (126, 56, 40), (176, 90, 60), (74, 30, 24)
    cv.line(1, 12, 14, 12, belt, belt_hi)                                        # the stitched leather belt, laid flat
    cv.line(1, 13, 14, 13, belt_lo)
    cv.ring(8.0, 12.5, 1.0, 1.8, (230, 180, 60))                                 # the brass ring buckle
    for x in (2, 4, 6, 10, 12, 14):                                              # the dragon-tooth daggers standing in their loops
        cv.put(x, 11, (90, 60, 40))
        cv.put(x, 10, (40, 36, 40))
        cv.put(x, 9, (40, 36, 40))
        cv.put(x, 8, (236, 230, 210))
        cv.put(x, 7, (250, 248, 236))
        cv.put(x, 6, (214, 208, 186))
        cv.put(x, 5, (236, 230, 210)) if x in (4, 10) else None
    return cv, (36, 16, 12)


ICONS_DEF = [
    ("wavebreaker", wavebreaker, "UNIQUE", False, (0, 14)),
    ("encircled_blade", encircled_blade, "RARE", False, (0, 14)),
    ("windcutter", windcutter, "LEGEND", False, (1, 14)),
    ("tripolar_sword", tripolar_sword, "LEGEND", False, (1, 14)),
    ("the_dagger_of_many_daggers", the_dagger_of_many_daggers, "UNIQUE", False, (1, 14)),
    ("flamecutter", flamecutter, "LEGEND", False, (0, 15)),
    ("heart_of_the_flame", heart_of_the_flame, "RARE", False, (1, 14)),
    ("bianca", bianca, "RARE", False, (0, 15)),
    ("sorrows_edge", sorrows_edge, "RARE", False, (1, 15)),
    ("the_dragons_truth", the_dragons_truth, "RARE", True, (1, 13)),
]


def main(only=None):
    os.makedirs(b.ICONS, exist_ok=True)
    for item_id, fn, grade, upright, gp in ICONS_DEF:
        if only and item_id not in only:
            continue
        cv, outline = fn()
        cv.outline(outline)
        cv.put(gp[0], gp[1], rgb(GRADE[grade]))                                  # the grade-coloured rim pixel
        cv.image().save(os.path.join(b.ICONS, item_id + ".png"))
        b.write_models(item_id, upright)
        print("icon", item_id)


if __name__ == "__main__":
    main(sys.argv[1:])
