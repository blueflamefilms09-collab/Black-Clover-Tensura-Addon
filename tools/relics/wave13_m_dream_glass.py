"""Dream Glass (card sheet part 2, "+3 Greatsword (Heavy, two-handed), 2d6 slashing", requires attunement): a transparent two-handed sword of blue
stained glass with metallic filigree inside the blade, an olive-gold crossguard with a blue gem, a maroon grip and a gold ring pommel.

Reference: the card, 736 x 736. The sword runs from the tip (28, 20) to the far edge of the ring pommel (708, 716): 975 px on the diagonal.
s = card px from the pommel. Axis: y up from the pommel ring to the point; faces +z.
"""
import math

import mesh_lib as ml
import wave1_models as w

# ---------------------------------------------------------------- 1 measure
TOTAL_M = 1.50                                       # a greatsword in hand
S_TIP = 975.0                                        # card px from the ring's far edge to the point
K = w.px_to_cm(TOTAL_M * 100.0, S_TIP)               # cm per card px (0.154)
S_RING = (0.0, 62.0)                                 # the ring pommel: outer diameter about 62 px (card ring at (688, 693))
S_GRIP = (62.0, 270.0)                               # the maroon grip, 208 px = 32 cm, 18 px wide at the swell
R_GRIP = 9.0 * K * 1.5                               # the grip is drawn about 18 px thick: radius 9 px, scaled up a little for the hand
S_GUARD = 285.0                                      # the gem sits at card (507, 517): 285 px from the pommel
GUARD_HALF = 70.0 * K                                # the crossguard spans about 124 px
S_BLADE0 = 292.0                                     # the glass starts right above the guard
BLADE_W0, BLADE_W1 = 62.0 * K, 54.0 * K              # blade width at the guard and just before the taper (card: 62 px wide, nearly parallel)
S_TAPER = 840.0                                      # the point starts to close here (card (150, 120))
BLADE_T = 1.5                                        # glass is thick: 1.5 cm


def y(s):
    return s * K


def build():
    m = ml.Mesh("dream_glass")
    # ---- 1 the glass blade: a lozenge with a flat ridge, long parallel run then a point
    bl = w.add(m, "blade", "crystal", base=(104, 186, 232), smooth=40, uv_mode="loft")
    ml.loft(bl, "y", (0, 0, 0), [(y(S_BLADE0) - 0.5, 1.0, 1.0), (y(S_BLADE0 + 200), 0.985, 0.97), (y(S_TAPER - 150), BLADE_W1 / BLADE_W0, 0.92), (y(S_TAPER), 0.9 * BLADE_W1 / BLADE_W0, 0.8),
                                  (y(S_TIP - 40), 0.45, 0.5), (y(S_TIP), 0.0, 0.0)], ml.lozenge(BLADE_W0, BLADE_T, 0.22))
    # ---- 2/3 the two fuller ridges of each face (the card's long inner line), 4 an inlaid metallic spine inside the glass
    for face, nm in ((1, "fuller_f"), (-1, "fuller_b")):
        f = w.add(m, nm, "silver", base=(150, 214, 245), wear=0.5)
        ml.fuller(f, (0, y(S_BLADE0 + 20), face * BLADE_T * 0.46), (0, y(S_TAPER - 40), face * BLADE_T * 0.40), (0, 0, face), 1.3, 0.18, 0.08, taper=6.0)
    sp = w.add(m, "inlay_spine", "gold", base=(214, 190, 96), smooth=60)
    ml.loft(sp, "y", (0, 0, 0), [(y(S_BLADE0 + 40), 0.0, 0.0), (y(S_BLADE0 + 60), 1.0, 1.0), (y(S_TAPER - 120), 0.9, 0.9), (y(S_TAPER - 60), 0.0, 0.0)], ml.lozenge(0.55, 1.1))
    # ---- 5 the guard: a swept bar angled down at both ends, 6/7 the plates on it, 8 the gem
    gb = w.add(m, "guard_bar", "bronze", base=(150, 140, 66), smooth=50)
    ml.loft(gb, "x", (0, y(S_GUARD), 0), [(-GUARD_HALF, 0.55, 0.55), (-GUARD_HALF * 0.9, 0.95, 0.9), (-GUARD_HALF * 0.35, 1.0, 1.0), (0.0, 1.05, 1.05),
                                          (GUARD_HALF * 0.35, 1.0, 1.0), (GUARD_HALF * 0.9, 0.95, 0.9), (GUARD_HALF, 0.55, 0.55)], ml.octagon(3.4, 2.8, 0.7))
    for sgn, nm in ((-1, "guard_plate_l"), (1, "guard_plate_r")):
        gp = w.add(m, nm, "bronze", base=(176, 164, 80), smooth=40)
        x0, x1 = sgn * GUARD_HALF * 0.30, sgn * GUARD_HALF * 0.98
        ml.loft(gp, "x", (0, y(S_GUARD), 0), [(x0, 1.0, 1.0), ((x0 + x1) / 2, 0.85, 1.0), (x1, 0.35, 0.8)], ml.rounded_rect(5.2, 1.5, 0.45), up=(0, 0, 1))
    gm = w.add(m, "guard_gem", "crystal", base=(70, 150, 255), glow=True, smooth=0)
    ml.gem(gm, (0, y(S_GUARD), 1.25), 0.65, long=1.0, sides=8, axis="z")
    # ---- 9 the guard collar and 10 the grip's two bands
    co = w.add(m, "guard_collar", "gold", base=(190, 170, 76), smooth=45)
    ml.lathe(co, "y", (0, 0, 0), [(y(S_GUARD) - 1.9, R_GRIP * 1.0), (y(S_GUARD) - 1.4, R_GRIP * 1.35), (y(S_GUARD) - 0.3, R_GRIP * 1.3), (y(S_GUARD) + 1.9, R_GRIP * 0.9), (y(S_GUARD) + 2.6, R_GRIP * 0.55)], sides=10, squash=0.85)
    gp2 = w.add(m, "grip", "leather", base=(110, 38, 52), uv_mode="loft")
    ml.loft(gp2, "y", (0, 0, 0), [(y(S_GRIP[0]), 0.9, 0.9), (y(S_GRIP[0] + 60), 1.0, 1.0), (y(S_GRIP[0] + 150), 1.12, 1.12), (y(S_GRIP[1]), 1.0, 1.0)], ml.circle(R_GRIP, 12))
    bn = w.add(m, "grip_bands", "gold", base=(190, 170, 76), wear=0.3)
    for s in (S_GRIP[0] + 2, S_GRIP[0] + 100, S_GRIP[1] - 8):
        ml.collar(bn, y(s), R_GRIP * 1.2, 0.9, lip=0.2, sides=12)
    # ---- 11 the ring pommel: a torus swept in the sword's plane, 12 the neck it hangs from
    rg = w.add(m, "pommel_ring", "gold", base=(150, 140, 60), smooth=55)
    rc, rr = y(S_RING[0] + 31.0), 31.0 * K * 0.78
    pts = [(rr * math.cos(a), rc + rr * math.sin(a), 0.0) for a in [2 * math.pi * i / 24 for i in range(24)]]
    pts.append(pts[0])
    ml.sweep(rg, pts, ml.circle(0.62, 8), caps=(False, False))
    nk = w.add(m, "pommel_neck", "gold", base=(176, 160, 70), smooth=50)
    ml.lathe(nk, "y", (0, 0, 0), [(y(S_RING[1] - 8), 0.55), (y(S_GRIP[0]) - 0.1, R_GRIP * 0.8), (y(S_GRIP[0]) + 0.5, R_GRIP * 1.15)], sides=10)

    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    _paint(m, atlas)
    ref = w.reference("p2/Relics of the world part 2/92d3cd418fc274a8251334939df8a334.jpg", (20, 10, 736, 736), "dream_glass")
    w.finish("dream_glass", m, atlas, real_m=TOTAL_M, glow=("FFFFFF", 0.16, 0.45, 1.0), ref=ref)


def _paint(m, atlas):
    import numpy as np

    def glass(c):                                            # transparent blue glass: lighter toward the middle, an etched filigree of fine lines
        S = c.S
        a = c.A if c.kind == "loft" else c.A
        b = c.B
        etch = (np.abs(np.sin(b * 0.9 + np.sin(a * 2.6) * 1.8)) < 0.10) & (b > y(S_BLADE0 + 60)) & (b < y(S_BLADE0 + 520))
        c.img[..., :3] = np.clip(c.img[..., :3] * (0.82 + 0.35 * (1.0 - np.abs(S - 0.5) * 2.0))[..., None], 0, 255)
        c.img[..., :3] = np.where(etch[..., None], c.img[..., :3] * 0.55 + np.array([90, 130, 150], np.float32) * 0.45, c.img[..., :3])
        w.set_glow(c, (110, 225, 255), 0.30, core=0.2)
        c.glow[..., 3] = np.where(etch, 150, c.glow[..., 3])
    w.paint_part(atlas, m.part("blade"), glass)
    w.paint_part(atlas, m.part("guard_gem"), lambda c: w.set_glow(c, (120, 190, 255), 1.0, core=0.5))
    for nm in ("fuller_f", "fuller_b"):
        w.paint_part(atlas, m.part(nm), lambda c: w.set_glow(c, (140, 235, 255), 0.55, core=0.3))
    w.paint_part(atlas, m.part("inlay_spine"), lambda c: w.set_glow(c, (255, 230, 150), 0.30, core=0.2))
