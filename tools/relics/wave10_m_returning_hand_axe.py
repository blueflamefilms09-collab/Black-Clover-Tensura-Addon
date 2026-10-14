"""Returning Hand Axe (card: Natural 1 Official, @jaaamesperrett, "Returning Hand Axe"): a small hand axe with a black curved blade carved with pale swirl
lines, dark leather lacing wrapped under the head and a long straight tan wooden haft; it turns to water when recalled. The 3D model, built with the
owner's method.

Reference: the card screenshot, 1080 x 2400 (it shows 900 x 2000; figures below are in the 900-wide view, x 1.2 for the file): the axe runs from the haft's
butt (200, 1412) to the head's top (190, 738): 674 px. A throwing axe this long is 62 cm, so K = 0.092 cm per px. Axis: y up from the butt; the
blade's belly curves toward -x (the card's left); the poll is on +x.
"""
import math

import numpy as np

import mesh_lib as ml
import wave1_models as w
from wave10_models import card, tube, glow_all

# ---------------------------------------------------------------- 1 measure
TOTAL = 62.0
S_TOT = 674.0
K = TOTAL / S_TOT
T = TOTAL - 4.0                                         # y of the head's top (card y 738 -> the haft's top is 4 cm below the very top of the horn)
HAFT_W, HAFT_T = 3.2, 2.2                               # the haft is 38 px wide on the card (3.5 cm) and thin front to back
Y_LACE0, Y_LACE1 = T - 16.5, T - 6.0                    # the dark lacing under the head, card y 880..1000
SOCKET_Y = (T - 8.0, T + 1.5)                           # the iron socket the blade is set in (card y 745..800)
BLADE_EDGE = [(-12.8, T + 3.6), (-14.8, T + 0.6), (-14.0, T - 4.0), (-11.0, T - 8.8), (-6.8, T - 13.2), (-3.2, T - 16.0)]      # the cutting edge, top horn to bottom tip (card x 30..180, y 710..910)
BLADE_HALF_W = [0.3, 3.0, 6.0, 6.4, 4.4, 0.8]            # how far the blade's body reaches in from its edge toward the haft
BLADE_T = 0.5                                           # half thickness


def build():
    m = ml.Mesh("returning_hand_axe")
    # ---- 1 the haft: a long oval loft, slightly swelling toward the butt, ending in a flared foot
    hf = w.add(m, "haft", "wood", base=(196, 134, 74), smooth=45, uv_mode="loft")
    ml.loft(hf, "y", (0, 0, 0), [(0.0, 0.7, 0.7), (1.5, 0.86, 0.85), (8, 0.88, 0.86), (30, 0.8, 0.8), (T - 8.0, 0.86, 0.86), (T + 0.5, 0.9, 0.9)], ml.rounded_rect(HAFT_W, HAFT_T, 0.7, 2))
    # ---- 2 the dark leather lacing: two counter-wound cords crossing under the head, 3 their knot bands
    lc = w.add(m, "lacing", "leather", base=(52, 36, 40), wear=0.3, smooth=60)
    for sgn in (-1, 1):
        pts = []
        for i in range(0, 25):
            t = i / 24
            a = sgn * 2 * math.pi * 3.0 * t
            pts.append(((HAFT_W * 0.5 + 0.25) * math.cos(a), Y_LACE0 + (Y_LACE1 - Y_LACE0) * t, (HAFT_T * 0.5 + 0.25) * math.sin(a)))
        ml.sweep(lc, pts, ml.circle(1.0, 5), scales=lambda t: (0.28, 0.28))
    for yy in (Y_LACE0 - 0.2, Y_LACE1 + 0.3):
        ml.collar(lc, yy, HAFT_W * 0.62, 0.7, lip=0.2, sides=10, oval=0.72, caps=(False, False))
    # ---- 4 the socket: an iron block where blade and poll meet the haft
    sk = w.add(m, "head_socket", "iron", base=(40, 40, 52), smooth=30)
    ml.loft(sk, "y", (0, 0, 0), [(SOCKET_Y[0], 0.55, 0.55), (SOCKET_Y[0] + 1.2, 0.6, 0.62), (SOCKET_Y[1] - 1.0, 0.6, 0.62), (SOCKET_Y[1], 0.5, 0.5)], ml.octagon(HAFT_W + 1.2, HAFT_T + 1.2, 0.6))
    # ---- 5 the blade: a swept black plate, wide in the belly, thin at the two horns (its edge is the path, the body reaches in toward the haft)
    bl = w.add(m, "blade", "iron", base=(26, 26, 36), smooth=40, uv_mode="loft")
    cx, cy = -1.5, T - 6.0
    pts = []
    for (x, y), hw in zip(BLADE_EDGE, BLADE_HALF_W):
        d = math.hypot(cx - x, cy - y) or 1.0
        pts.append((x + (cx - x) / d * hw, y + (cy - y) / d * hw, 0.0))
    ml.sweep(bl, pts, ml.bevelled(2.0, 1.0, 0.5), scales=lambda t: (BLADE_HALF_W[min(5, int(t * 5 + 0.5))], BLADE_T) if t < 0.98 else (0.0, 0.0), up=(0, 0, 1))
    # ---- 6 the poll (the small block behind the eye), 7 the pale swirl lines carved into the blade
    pl = w.add(m, "poll", "iron", base=(34, 34, 44), smooth=25)
    ml.loft(pl, "x", (0, T - 3.0, 0), [1.0, 2.4, 4.6, 5.6], ml.octagon(3.0, HAFT_T + 0.2, 0.5))
    sw = w.add(m, "swirl_ridges", "silver", base=(216, 220, 232), wear=0.3, smooth=60)
    for face in (1, -1):                                                                          # a carved line parallel to the edge, and a curling branch off it
        line = []
        for (x, y), hw in list(zip(BLADE_EDGE, BLADE_HALF_W))[:5]:
            d = math.hypot(cx - x, cy - y) or 1.0
            line.append((x + (cx - x) / d * 1.5, y + (cy - y) / d * 1.5, face * BLADE_T * 0.95))
        ml.sweep(sw, line, ml.circle(1.0, 4), scales=lambda t: (0.12 * (1 - 0.4 * t),) * 2)
        br = [(-9.0 + 4.0 * t, T - 2.0 - 3.5 * t + 0.9 * math.sin(6 * t), face * BLADE_T * 0.95) for t in np.linspace(0, 1, 9)]
        ml.sweep(sw, br, ml.circle(1.0, 4), scales=lambda t: (0.1 * (1 - 0.5 * t),) * 2)
    # ---- 8 the butt: a small flared cap (the card's haft ends in a ragged foot)
    bc = w.add(m, "butt_cap", "wood", base=(176, 118, 64), smooth=45)
    ml.lathe(bc, "y", (0, 0, 0), [(-0.8, HAFT_W * 0.4), (-0.2, HAFT_W * 0.6), (0.6, HAFT_W * 0.55), (1.4, HAFT_W * 0.45)], sides=8, squash=0.7)
    # ---- 9 the drops of water: it turns to water when recalled; a few beads cling to the blade and haft
    dr = w.add(m, "water_drops", "crystal", base=(120, 214, 255), glow=True, smooth=0)
    for (x, y, z, r) in ((-13.6, T - 10.0, 0.2, 0.55), (-10.0, T - 14.4, 0.0, 0.42), (-15.6, T - 2.0, 0.3, 0.38), (1.4, Y_LACE0 - 4.0, 1.4, 0.4), (-2.0, T + 4.6, 0.4, 0.4)):
        w.ellipsoid(dr, (x, y, z), r, r * 1.5, r, sides=6, stacks=4)

    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    _paint(m, atlas)
    ref = card("p2/Relics of the world part 2/Screenshot_20261013_223025_Opera.jpg", (30, 840, 360, 1700), "returning_hand_axe")
    w.finish("returning_hand_axe", m, atlas, real_m=TOTAL / 100.0, glow=("FFFFFF", 0.14, 0.3, 0.9), ref=ref)


def _paint(m, atlas):
    def blade(c):
        sh = c.img.shape[:2]
        n = w.noise2(sh, 6, 5)
        c.img[..., :3] = np.clip(c.img[..., :3] * (0.8 + 0.5 * n[..., None]) + np.array([4, 4, 10]), 0, 255)
        edge = np.clip((np.abs(c.S - 0.5) - 0.38) * 9, 0, 1)
        c.img[..., :3] = c.img[..., :3] * (1 - edge[..., None]) + np.array([150, 156, 176], np.float32) * edge[..., None]
        w.set_glow(c, (110, 210, 255), 0.22, mask=edge, core=0.2)
    w.paint_part(atlas, m.part("blade"), blade)
    glow_all(atlas, m.part("swirl_ridges"), (150, 225, 255), 0.5, 0.3)
    glow_all(atlas, m.part("water_drops"), (140, 220, 255), 0.8, 0.4)
