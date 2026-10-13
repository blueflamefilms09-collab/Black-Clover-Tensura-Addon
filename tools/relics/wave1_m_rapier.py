"""Rainbow Rapier (card: Mr James Gifford, "Rainbow Rapier", rare, +1 rapier, 1d8+1, finesse): a rapier with a blade shading through rainbow colours,
a silver swept guard and cup hilt with a small blue/purple ribbon or feather. The 3D model, built with the owner's method.

Reference: the card is only 240 x 240; the rainbow rapier runs from its pommel (40, 135) to its point (202, 47), 184 px. s = card px along that line.
Axis: y up from the pommel to the point; the cup opens toward the blade; the knuckle bow curves on +x.
"""
import math

import numpy as np

import mesh_lib as ml
import wave1_models as w

# ---------------------------------------------------------------- 1 measure
TOTAL_M = 1.12                                       # a rapier: blade about 90 cm, hilt about 22 cm
S_TIP = 184.3                                        # card px from the pommel to the point
K = w.px_to_cm(TOTAL_M * 100.0, S_TIP)               # cm per card px (0.61)
S_POMMEL = (0.0, 9.0)                                # the pommel nut (a ball at the lower left, about 12 px)
S_GRIP = (9.0, 25.0)                                 # the wire-wrapped grip
S_CUP_BASE, S_CUP_MOUTH = 24.0, 41.0                 # the cup guard: its base and mouth (card px, about 17 px deep, 22 px across)
R_CUP = 11.0 * K                                     # cup radius at the mouth
S_BLADE0 = 38.0                                      # the blade starts where the cup's mouth is (card (70, 112))
BLADE_W0, BLADE_W1 = 3.3 * K, 1.4 * K                # blade width at the forte and near the point (1.6 cm to 0.7 cm)
BLADE_T0 = 0.75                                      # blade thickness at the forte, cm
QUILLON_S, QUILLON_HALF = 27.0, 15.0 * K             # the swept bar across the guard, half span 9 cm
GRIP_R = 1.0


def build():
    m = ml.Mesh("rainbow_rapier")
    y = lambda s: s * K
    # ---- 1 the blade: a long loft closing to a point, a hexagonal section with a ridge
    bl = w.add(m, "blade", "steel", base=(210, 214, 232), smooth=40, uv_mode="loft")
    stations = [(y(S_BLADE0) - 1.0, 1.0, 1.0), (y(S_BLADE0 + 30), 0.93, 0.95), (y(S_BLADE0 + 90), 0.62, 0.8), (y(S_TIP - 14), 0.5, 0.7), (y(S_TIP - 5), 0.28, 0.45), (y(S_TIP), 0.0, 0.0)]
    ml.loft(bl, "y", (0, 0, 0), stations, ml.lozenge(BLADE_W0, BLADE_T0, 0.25))
    # ---- 2/3 the fuller: two raised ridges per face, tapering at both ends
    for face, nm in ((1, "fuller_f"), (-1, "fuller_b")):
        f = w.add(m, nm, "steel", base=(220, 224, 240), wear=0.8)
        ml.fuller(f, (0, y(S_BLADE0 + 4), face * BLADE_T0 * 0.48), (0, y(S_BLADE0 + 100), face * BLADE_T0 * 0.40), (0, 0, face), 0.5, 0.12, 0.05, taper=2.0)
    # ---- 4 the collar the blade rises from
    col = w.add(m, "blade_collar", "silver")
    ml.collar(col, y(S_CUP_MOUTH) - 1.4, 1.5, 1.6, lip=0.3, sides=10, oval=0.55)
    # ---- 5 the cup: a bowl opening toward the blade, built as a closed shell (outer and inner wall)
    cup = w.add(m, "cup", "silver", base=(200, 206, 222), smooth=50)
    depth = (S_CUP_MOUTH - S_CUP_BASE) * K
    ml.lathe(cup, "y", (0, 0, 0), _shell(y(S_CUP_BASE), depth), sides=20, caps=(False, False))
    rim = w.add(m, "cup_rim", "silver", base=(214, 220, 232))
    ml.collar(rim, y(S_CUP_MOUTH) - 0.7, R_CUP * 1.0, 0.9, lip=0.25, sides=20)
    # ---- 6 rivets round the cup, 7 the quillon bar
    rv = w.add(m, "cup_rivets", "gold", base=(226, 196, 120))
    for i in range(8):
        a = 2 * math.pi * i / 8
        ys = y(S_CUP_BASE) + depth * 0.62
        r = R_CUP * math.sin(0.62 * math.pi / 2) ** 0.8
        ml.rivet(rv, (r * math.cos(a), ys, r * math.sin(a)), 0.22, (math.cos(a), 0.0, math.sin(a)), 8)
    qb = w.add(m, "quillon", "silver", base=(196, 202, 218), smooth=35)
    ml.loft(qb, "x", (0, y(QUILLON_S), 0), [(-QUILLON_HALF, 0.4, 0.4), (-QUILLON_HALF + 1.2, 0.8, 0.8), (-2.0, 0.7, 0.7), (0.0, 0.85, 0.85), (2.0, 0.7, 0.7), (QUILLON_HALF - 1.2, 0.8, 0.8), (QUILLON_HALF, 0.4, 0.4)], ml.octagon(1.3, 1.3, 0.35))
    for sgn, nm in ((-1, "quillon_tip_l"), (1, "quillon_tip_r")):
        t = w.add(m, nm, "silver", base=(222, 226, 238))
        ml.lathe(t, "x", (sgn * QUILLON_HALF, y(QUILLON_S), 0), [(0.0, 0.5), (sgn * 0.5, 0.75), (sgn * 1.2, 0.4), (sgn * 1.7, 0.0)], sides=8)
    # ---- 9 the knuckle bow: a swept loop from the quillon's end round to the pommel
    kb = w.add(m, "knuckle_bow", "silver", base=(196, 202, 220), smooth=45)
    pts = []
    for i in range(0, 13):
        t = i / 12
        ang = math.pi * 0.5 * t
        pts.append((QUILLON_HALF * 0.9 - (QUILLON_HALF * 0.9 - 0.9) * math.sin(ang) ** 1.0, y(QUILLON_S) - (y(QUILLON_S) - y(S_POMMEL[1] - 1.5)) * (1 - math.cos(ang)), 0.0))
    ml.sweep(kb, pts, ml.circle(0.42, 8), scales=lambda t: (1.0, 1.0))
    fr = w.add(m, "finger_ring", "silver", base=(204, 210, 226))
    rp = [(1.5 + 2.2 * math.cos(math.radians(a)), y(S_GRIP[1] - 3) + 2.2 * math.sin(math.radians(a)) * 0.9, 0.0) for a in range(-60, 241, 20)]
    ml.sweep(fr, rp, ml.circle(0.28, 6), scales=lambda t: (1.0, 1.0))
    # ---- 10 the grip and 11 its wire wrap
    gp = w.add(m, "grip", "leather", base=(70, 60, 110), uv_mode="loft")
    ml.loft(gp, "y", (0, 0, 0), [(y(S_GRIP[0]), 0.9, 0.9), (y(S_GRIP[0] + 8), 1.1, 1.1), (y(S_GRIP[1]), 0.95, 0.95)], ml.circle(GRIP_R, 10))
    gw = w.add(m, "grip_wire", "silver", base=(208, 214, 230), wear=0.3)
    for i in range(7):
        ml.collar(gw, y(S_GRIP[0]) + 0.4 + (y(S_GRIP[1]) - y(S_GRIP[0]) - 1.2) * i / 6, GRIP_R * 1.1, 0.35, lip=0.08, sides=10)
    # ---- 12/13 the pommel: a ball on a short neck
    pb = w.add(m, "pommel", "silver", base=(206, 212, 228), smooth=60)
    ml.lathe(pb, "y", (0, 0, 0), [(y(S_POMMEL[0]) - 0.0, 0.0), (y(S_POMMEL[0]) + 0.6, 1.5), (y(S_POMMEL[0]) + 2.4, 2.2), (y(S_POMMEL[0]) + 4.2, 1.7), (y(S_POMMEL[1]) - 1.0, 1.0)], sides=12)
    pn = w.add(m, "pommel_nut", "silver", base=(224, 228, 240))
    ml.collar(pn, y(S_POMMEL[1]) - 1.4, 1.35, 0.9, lip=0.2, sides=10)
    # ---- 14/15 the feather and the ribbon tied at the cup: swept flat strips curling up and away
    for nm, base_col, path, wid in (("feather", (110, 130, 230), [(-1.0, 0.0, 0.0), (-3.5, 3.0, 0.6), (-6.0, 6.0, 1.4), (-6.8, 9.6, 2.0), (-5.0, 12.5, 2.2), (-2.5, 13.8, 2.0)], 2.4),
                                    ("ribbon", (160, 90, 210), [(-0.5, 0.0, 0.0), (-3.0, -2.5, 0.8), (-6.0, -3.8, 1.2), (-8.5, -3.0, 1.4), (-9.5, -0.6, 1.2)], 1.6)):
        f = w.add(m, nm, "cloth", base=base_col, wear=0.2, uv_mode="loft")
        pts = [(px, y(S_CUP_BASE + 4) + py * 0.5 + 0.0, pz) for (px, py, pz) in path]
        ml.sweep(f, pts, ml.rect_ring(1.0, 1.0), scales=lambda t, wid=wid: (wid * (1.0 - 0.7 * t) if t < 0.95 else 0.0, 0.12 if t < 0.95 else 0.0))

    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    _paint(m, atlas)
    ref = w.reference("90b3a7c2262881124d167ba39d7ccde4.jpg", (20, 30, 220, 150), "rainbow_rapier")
    w.finish("rainbow_rapier", m, atlas, real_m=TOTAL_M, glow=("FFFFFF", 0.20, 0.55, 1.0), ref=ref)


def _shell(y0, depth):
    """Profile (s along y, radius) of the cup as a thin closed shell."""
    outer = [(y0 + depth * t, R_CUP * math.sin(t * math.pi / 2) ** 0.8 + 0.04) for t in np.linspace(0, 1, 9)]
    inner = [(y0 + depth * t - 0.0, max(0.02, R_CUP * math.sin(t * math.pi / 2) ** 0.8 - 0.35)) for t in np.linspace(1, 0.08, 8)]
    return outer + inner


def _paint(m, atlas):
    y0, y1 = S_BLADE0 * K, S_TIP * K

    def rainbow(c):                                                    # red at the point through yellow, green and blue to violet at the hilt
        t = c.T if c.kind == "loft" else (c.B - y0) / (y1 - y0)
        t = np.clip(t, 0, 1)
        col = w.hsv_rows(1.0 - t, 0.62, 1.0)
        lum = c.img[..., :3].mean(axis=2, keepdims=True) / 200.0
        c.img[..., :3] = np.clip(col * (0.55 + 0.45 * lum), 0, 255)
        w.set_glow(c, (255, 255, 255), 0.55, core=0.0)
        c.glow[..., :3] = np.clip(w.hsv_rows(1.0 - t, 0.85, 1.0), 0, 255)
    for nm in ("blade", "fuller_f", "fuller_b"):
        w.paint_part(atlas, m.part(nm), rainbow)
