#!/usr/bin/env python3
"""Relics of Many Worlds, second card set, wave 10: the 16x16 inventory icons and the item models of the ten weapons.

    python3 -B tools/relics/wave10_icons.py [id ...]       (idempotent)

Writes, per id,
  assets/nusmp/textures/item/<id>.png        the sprite: the old-style inventory icon (kept as the icon; the item itself is 3D in hand)
  assets/nusmp/models/item/<id>_icon.json    the flat inventory model of that sprite
  assets/nusmp/models/item/<id>.json         the in-hand model: builtin/entity, drawn by client/WeaponRenderer from weapon_meshes/<id>.txt
                                             (the display transforms of the Bloodbound Katana, like the other diagonal weapons)
Every icon is drawn from bands along a diagonal (a width profile and a colour function per weapon), then given a one pixel dark outline;
the grade shows as one coloured pixel at the pommel and the glow as bright pixels at runes, gems and edges.
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from wave1_icons import Canvas, rgb, shade, mix, GRADE, ICONS, MODELS, S, write_models   # noqa: E402


def band(cv, p0, p1, wfn, colfn, t0=0.0, t1=1.0, cap=0.0):
    """Paints every pixel whose centre is within wfn(t) of the segment p0-p1 (t0..t1 of it); colfn(t, s) with s in -1..1 across."""
    x0, y0 = p0
    x1, y1 = p1
    dx, dy = x1 - x0, y1 - y0
    L2 = dx * dx + dy * dy
    L = math.sqrt(L2)
    for y in range(S):
        for x in range(S):
            t = ((x - x0) * dx + (y - y0) * dy) / L2
            if t < t0 - cap / L or t > t1 + cap / L:
                continue
            tt = min(max(t, t0), t1)
            d = ((x - x0) * -dy + (y - y0) * dx) / L          # signed distance across (positive = lower right side)
            w = wfn(tt)
            if abs(d) <= w + 1e-6 and (t0 - 1e-9 <= t <= t1 + 1e-9 or math.hypot(x - (x0 + dx * tt), y - (y0 + dy * tt)) <= w):
                cv.put(x, y, colfn(tt, d / max(w, 0.01)))


def noise(x, y, seed=0):
    n = (x * 73856093) ^ (y * 19349663) ^ (seed * 83492791)
    return ((n >> 3) & 255) / 255.0


# ---------------------------------------------------------------- the ten icons (each returns a Canvas and the outline colour)
def krakeblade():
    cv = Canvas()
    moss, grey, teal = (96, 132, 92), (126, 134, 128), (72, 128, 116)
    brown, brown2, pale = (126, 86, 58), (84, 54, 38), (232, 214, 196)

    def slab(t, s):
        n = noise(int(t * 40), int(s * 5 + 9))
        base = moss if n < 0.4 else grey if n < 0.75 else teal
        return shade(base, 1.18 - 0.35 * max(0.0, s)) if s < 0.4 else shade(base, 0.72)
    band(cv, (3, 13), (14, 2), lambda t: (1.75 if t < 0.93 else 1.2) if t > 0.34 else 0.45, lambda t, s: slab(t, s) if t > 0.34 else (brown if int(t * 20) % 2 else brown2), 0.0, 1.0)
    for (x, y) in ((8, 8), (9, 6), (11, 6), (10, 4), (12, 4), (7, 7)):               # the holes and barnacle dots
        cv.put(x, y, (40, 58, 52))
    for (x, y) in ((9, 8), (10, 7), (11, 5), (12, 3)):
        cv.put(x, y, pale)
    for k in range(0, 5):                                                              # the tentacle coiling up the blade
        cv.put(6 + k, 10 - k - (1 if k % 2 else 0), brown)
        cv.put(7 + k, 10 - k - (1 if k % 2 else 0), brown2)
    cv.put(7, 9, pale)
    cv.put(9, 7, pale)
    cv.put(2, 14, brown)                                                               # the tentacle-tip pommel
    cv.put(1, 14, brown2)
    cv.put(1, 13, brown)
    cv.put(13, 3, (150, 255, 235))                                                     # the blue-green glow
    cv.put(8, 9, (120, 240, 220))
    return cv, (22, 36, 34)


def unicorn_dagger():
    cv = Canvas()
    stripes = [(255, 170, 200), (255, 226, 150), (170, 236, 200), (196, 176, 255)]
    gold, gold2 = (240, 196, 84), (176, 128, 40)

    def horn(t, s):
        k = int((t * 7 + (s + 1) * 0.9) * 1.6) % 4                                      # the spiral: stripes slant across the horn
        c = stripes[k]
        return mix(c, (255, 255, 255), 0.45) if s < -0.4 else c
    band(cv, (6, 9), (14, 1), lambda t: 1.9 * (1 - t) + 0.25, horn, 0.0, 1.0)
    cv.put(15, 0, (255, 255, 255))
    band(cv, (2, 13), (5, 10), lambda t: 1.0, lambda t, s: gold if s < 0 else gold2)  # hilt
    for (x, y) in ((4, 7), (5, 8), (3, 8), (7, 11), (6, 11), (8, 12), (7, 12)):       # the filigree wings of the guard
        cv.put(x, y, gold if (x + y) % 2 else gold2)
    cv.put(5, 9, (140, 230, 255))                                                      # crystal
    cv.put(4, 10, (200, 245, 255))
    cv.put(1, 14, (220, 60, 80))                                                       # the red string and its charm
    cv.put(0, 15, (255, 150, 170))
    cv.put(2, 15, (220, 60, 80))
    return cv, (96, 40, 90)


def rune_splitter():
    cv = Canvas()
    red, red2 = (122, 28, 30), (80, 16, 22)
    gold, gold2, glow = (244, 190, 60), (170, 120, 30), (255, 244, 150)
    band(cv, (2, 14), (12, 4), lambda t: 0.7, lambda t, s: red if s < 0.1 else red2, 0.0, 1.0)
    for k in (3, 5, 7):
        cv.put(2 + k, 14 - k, glow)                                                    # the glowing star studs
    cv.put(6, 10, gold)                                                                # collar
    cv.put(7, 9, gold)
    cv.put(6, 9, gold2)
    cv.put(7, 10, gold2)
    cv.put(12, 3, gold2)                                                               # the socket and finial
    cv.put(12, 2, gold)
    cv.put(12, 1, glow)
    for (x, y) in ((11, 2), (10, 2), (9, 2), (9, 3), (8, 3), (8, 4), (8, 5), (9, 6), (10, 6)):      # the curved gold edge of the crescent head
        cv.put(x, y, gold)
    for (x, y) in ((10, 3), (11, 3), (9, 4), (10, 4), (11, 4), (9, 5), (10, 5), (11, 5)):          # its engraved face
        cv.put(x, y, gold2 if (x + y) % 2 else (200, 150, 40))
    cv.put(10, 4, glow)
    cv.put(2, 14, (255, 220, 90))
    return cv, (60, 14, 14)


def junglethorn_whip():
    cv = Canvas()
    vine, vine2, thorn = (110, 120, 52), (150, 92, 44), (214, 72, 36)
    band(cv, (2, 14), (5, 11), lambda t: 0.9, lambda t, s: (84, 140, 60) if int(t * 6) % 2 else (56, 100, 46))
    cv.put(6, 10, (50, 40, 40))
    cv.put(5, 10, (50, 40, 40))
    # a spiral of vine winding out to the tail
    pts = []
    for i in range(0, 44):
        a = i * 0.30
        r = 0.8 + i * 0.12
        pts.append((9.0 + r * math.cos(a), 7.0 + r * math.sin(a) * 0.95))
    for i, (x, y) in enumerate(pts):
        if 0 <= x < 15.5 and 0 <= y < 15.5:
            cv.put(x, y, vine if i % 3 else vine2)
            if i % 6 == 3:
                cv.put(x + (1 if math.cos(i) > 0 else -1) * 0.6, y - 0.8, thorn)
    cv.put(13, 14, (190, 100, 50))                                                     # the claw tip
    cv.put(14, 14, (240, 220, 190))
    cv.put(1, 13, (230, 70, 50))                                                       # feathers
    cv.put(0, 14, (240, 150, 40))
    cv.put(1, 15, (140, 70, 190))
    cv.put(2, 15, (230, 70, 50))
    return cv, (30, 38, 20)


def needler():
    cv = Canvas()
    purple, purple2, steel, steel2 = (112, 42, 178), (70, 24, 128), (190, 194, 208), (112, 116, 136)
    band(cv, (2, 14), (12, 4), lambda t: 0.75, lambda t, s: purple if s < 0.1 else purple2)               # the stock and rail
    cv.put(12, 3, steel)
    cv.put(13, 3, (150, 255, 255))
    cv.put(13, 2, (176, 112, 255))
    # the swept-back bladed limbs (a jagged purple wing on each side of the rail)
    for (x, y) in ((8, 2), (9, 2), (10, 2), (10, 3), (7, 3), (8, 3), (9, 3), (6, 4), (7, 4), (8, 4), (5, 5), (6, 5), (9, 4)):
        cv.put(x, y, purple if (x + y) % 2 else (150, 70, 220))
    for (x, y) in ((14, 6), (15, 7), (14, 7), (13, 7), (13, 8), (14, 8), (12, 9), (13, 9), (11, 10), (12, 10), (10, 8)):
        cv.put(x, y, purple if (x + y) % 2 else (150, 70, 220))
    cv.put(14, 5, steel)
    cv.put(10, 5, steel)                                                               # the metal fittings
    cv.put(5, 9, steel2)
    cv.put(4, 10, steel)
    cv.put(8, 9, steel)
    cv.line(6, 4, 14, 6, (214, 214, 226))                                              # the drawn string
    cv.put(9, 6, (160, 255, 255))
    cv.put(3, 12, (176, 112, 255))
    return cv, (30, 12, 56)


def sidras_swearing_sword():
    cv = Canvas()
    crystal, crystal2, metal, metal2 = (236, 226, 246), (170, 150, 196), (150, 152, 164), (82, 84, 96)
    band(cv, (5, 10), (14, 1), lambda t: 1.7 * (1 - t * 0.5) if t < 0.9 else 0.8, lambda t, s: crystal if s < 0.0 else crystal2, 0.0, 1.0)
    cv.put(15, 0, (255, 255, 255))
    for (x, y) in ((4, 9), (5, 8), (6, 8), (4, 8), (7, 9)):                             # crystal shards growing at the base
        cv.put(x, y, mix(crystal, (255, 255, 255), 0.4) if (x + y) % 2 else crystal2)
    band(cv, (2, 14), (5, 11), lambda t: 0.7, lambda t, s: metal)
    for (x, y) in ((3, 10), (4, 11), (5, 12), (6, 13), (2, 9), (7, 14), (4, 10), (5, 11)):   # the angular cross-guard
        cv.put(x, y, metal2 if (x + y) % 2 else metal)
    cv.put(1, 14, metal)
    cv.put(2, 15, metal2)
    cv.put(8, 7, (226, 190, 255))
    return cv, (60, 40, 76)


def stegosaurus_greatsword():
    cv = Canvas()
    bone, bone2, teal, pink = (238, 232, 208), (176, 168, 140), (52, 100, 100), (236, 110, 120)
    for k in range(0, 10):                                                             # a column of vertebrae with their spikes
        x, y = 5 + k, 10 - k
        cv.put(x, y, bone)
        cv.put(x + 1, y, bone2)
        cv.put(x, y - 1, bone2 if k % 2 else bone)
        if k % 2 == 0 and k > 0:
            cv.put(x - 1, y - 1, bone)
            cv.put(x + 2, y + 1, bone2)
    for (x, y) in ((11, 0), (12, 0), (14, 2), (15, 3)):                                # the curved side spikes at the tip
        cv.put(x, y, (214, 210, 190))
    band(cv, (2, 14), (5, 11), lambda t: 0.8, lambda t, s: teal if int(t * 8) % 2 else (36, 74, 76))
    for (x, y) in ((3, 10), (4, 9), (4, 12), (6, 12), (7, 11), (2, 11)):               # the leaf-like guard
        cv.put(x, y, pink if (x + y) % 2 else (176, 70, 90))
    cv.put(5, 10, (100, 120, 70))
    cv.put(1, 14, bone)
    cv.put(1, 15, bone2)
    cv.put(2, 15, bone)
    cv.put(13, 2, (255, 255, 240))
    return cv, (54, 48, 40)


def jade_xiphos():
    cv = Canvas()
    jade, jade2, gold, gold2 = (74, 214, 120), (30, 140, 76), (244, 200, 90), (170, 122, 40)

    def leaf(t, s):
        if abs(s) < 0.18:
            return (184, 255, 206)                                                     # the central ridge
        return jade if s < 0 else jade2
    band(cv, (5, 10), (14, 1), lambda t: 0.5 + 1.9 * math.sin(math.pi * min(1.0, t * 0.95 + 0.05)) ** 0.8 if t < 0.97 else 0.5, leaf, 0.0, 1.0)
    cv.put(15, 0, (220, 255, 230))
    for (x, y) in ((3, 10), (4, 11), (5, 12), (6, 13), (4, 10), (5, 11)):               # the gold crossguard with curled ends
        cv.put(x, y, gold if (x + y) % 2 else gold2)
    cv.put(2, 9, gold)
    cv.put(7, 14, gold)
    band(cv, (2, 14), (4, 12), lambda t: 0.6, lambda t, s: gold2 if int(t * 5) % 2 else gold)
    cv.disc(1.6, 14.4, 1.0, gold, gold2)
    cv.put(1, 14, (100, 230, 150))
    return cv, (22, 70, 44)


def returning_hand_axe():
    cv = Canvas()
    tan, tan2, dark, steel = (190, 128, 70), (124, 80, 44), (30, 30, 40), (84, 88, 112)
    band(cv, (3, 14), (11, 6), lambda t: 0.8, lambda t, s: tan if s < 0.1 else tan2, 0.0, 1.0)
    for k in range(3):                                                                # the dark lacing under the head
        cv.put(8 + k, 9 - k, (50, 36, 40))
        cv.put(9 + k, 9 - k, (36, 28, 32))
    for (x, y) in ((11, 5), (12, 5), (10, 4), (11, 4), (12, 4), (13, 4), (9, 4), (8, 4), (9, 5), (10, 5), (8, 5), (7, 6), (8, 6), (7, 7), (6, 8), (13, 5), (9, 3), (10, 3)):   # the curved black blade
        cv.put(x, y, dark if (x + y) % 3 else steel)
    cv.put(7, 5, (230, 236, 255))
    cv.put(6, 7, (230, 236, 255))
    for (x, y) in ((6, 9), (5, 10), (13, 7), (14, 8), (7, 10)):                       # drops: it turns to water
        cv.put(x, y, (96, 214, 240))
    cv.put(14, 7, (170, 240, 255))
    cv.put(2, 15, (84, 200, 240))
    return cv, (18, 18, 26)


def dead_blade():
    cv = Canvas()
    pole, pole2, pink, pink2, skull = (98, 84, 112), (58, 48, 72), (255, 64, 208), (190, 30, 150), (255, 190, 240)
    band(cv, (1, 15), (13, 3), lambda t: 0.55, lambda t, s: pole if s < 0 else pole2, 0.0, 1.0)
    # the huge crescent blade of skulls, curving down the pole's upper part
    shape = [(5, 1), (6, 1), (7, 1), (4, 2), (5, 2), (6, 2), (7, 2), (3, 3), (4, 3), (5, 3), (6, 3), (7, 3), (8, 3), (3, 4), (4, 4), (5, 4), (6, 4), (7, 4), (8, 4), (9, 4),
             (3, 5), (4, 5), (5, 5), (6, 5), (7, 5), (8, 5), (4, 6), (5, 6), (6, 6), (7, 6), (8, 6), (5, 7), (6, 7), (7, 7), (6, 8), (7, 8), (8, 0), (6, 0), (4, 1)]
    for (x, y) in shape:
        cv.put(x, y, pink if (x * 3 + y * 5) % 4 else pink2)
    for (x, y) in ((5, 3), (4, 5), (6, 5), (6, 3), (5, 6), (7, 4)):                    # skulls
        cv.put(x, y, skull)
    for (x, y) in ((5, 2), (4, 4), (6, 4), (5, 5)):
        cv.put(x, y, (110, 10, 80))
    cv.put(2, 4, (255, 120, 220))
    cv.put(1, 15, (255, 120, 230))
    return cv, (44, 14, 54)


ICONS_DEF = [
    ("krakeblade", krakeblade, "UNIQUE", False, (0, 15)),
    ("unicorn_dagger", unicorn_dagger, "RARE", False, (2, 15)),
    ("rune_splitter", rune_splitter, "UNIQUE", False, (1, 15)),
    ("junglethorn_whip", junglethorn_whip, "UNIQUE", False, (0, 15)),
    ("needler", needler, "LEGEND", False, (2, 14)),
    ("sidras_swearing_sword", sidras_swearing_sword, "RARE", False, (0, 15)),
    ("stegosaurus_greatsword", stegosaurus_greatsword, "RARE", False, (0, 15)),
    ("jade_xiphos", jade_xiphos, "SPECIAL", False, (0, 15)),
    ("returning_hand_axe", returning_hand_axe, "RARE", False, (3, 15)),
    ("dead_blade", dead_blade, "RARE", False, (2, 15)),
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
