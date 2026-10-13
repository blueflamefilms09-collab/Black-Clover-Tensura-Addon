"""Deliverance (card: Flail Snail's Mail, "Paladin Magic Items", weapon (longsword), very rare, requires attunement, +2 longsword): the teal-green
longsword on the card, with a lattice-wrapped grip, a dark teal guard with four curving claws, and a serpent of green energy winding up the blade.
The 3D model, built with the owner's method. (A new sword: it shares only the sheet with the Weapon of Resurgence.)

Reference: the card, 593 x 767; the sword at the top right. The pommel top is at card (500, 48), the point at about (537, 362): 316 px.
s = card px from the pommel. Axis: y up from the pommel to the point; faces +z.
"""
import math

import numpy as np

import mesh_lib as ml
import wave1_models as w

# ---------------------------------------------------------------- 1 measure
TOTAL_M = 1.15                                       # a longsword
S_TIP = 316.0                                        # card px from the pommel top (card y 48) to the point
K = w.px_to_cm(TOTAL_M * 100.0, S_TIP)               # cm per card px (0.364)
S_POMMEL = (0.0, 12.0)                               # the flat pommel cap with its flared rim
S_GRIP = (12.0, 80.0)                                # the lattice-wrapped grip, about 10 px wide (card y 55..107)
R_GRIP = 5.2 * K
S_COLLAR = (79.0, 95.0)                             # the guard's bulb (card y 127..143), 32 px wide
R_COLLAR = 13.0 * K
S_BLADE0 = 95.0                                     # the blade starts under the collar (card y 143)
BLADE_W, BLADE_T = 24.0 * K * 0.85, 8.0 * K          # the card draws the blade broad: 24 px wide, scaled to 85 percent; thick 8 px
S_SERPENT = (200.0, 305.0)                           # the green energy winds over the lower half of the blade
SERP_RX, SERP_RZ, SERP_TUBE = 17.0 * K, 7.0 * K, 3.4 * K
# the claws of the guard: (x px, s px) control points, the outer long one and the inner short one, mirrored
CLAW_OUT = [(13, 92), (22, 93), (30, 87), (33, 76), (31, 63)]
CLAW_IN = [(8, 92), (14, 89), (18, 81), (19, 72)]


def y(s):
    return s * K


def build():
    m = ml.Mesh("deliverance")
    # ---- 1 blade: a diamond section (the card's two-tone ridge), long taper to the point
    bl = w.add(m, "blade", "steel", base=(104, 168, 156), smooth=35, uv_mode="loft")
    ml.loft(bl, "y", (0, 0, 0), [(y(S_BLADE0) - 1.0, 1.0, 1.0), (y(S_BLADE0 + 60), 0.98, 0.98), (y(S_BLADE0 + 140), 0.9, 0.9), (y(S_TIP - 55), 0.7, 0.72), (y(S_TIP - 14), 0.34, 0.5), (y(S_TIP), 0.0, 0.0)],
            ml.lozenge(BLADE_W, BLADE_T, 0.12))
    # ---- 2/3 a fuller of two ridges each face
    for face, nm in ((1, "fuller_f"), (-1, "fuller_b")):
        f = w.add(m, nm, "steel", base=(150, 214, 196), wear=0.8)
        ml.fuller(f, (BLADE_W * 0.12, y(S_BLADE0 + 6), face * BLADE_T * 0.40), (BLADE_W * 0.08, y(S_BLADE0 + 150), face * BLADE_T * 0.34), (0, 0, face), 0.85, 0.15, 0.06, taper=3.5)
    # ---- 4 the guard's bulb, 5 a gem set in it
    co = w.add(m, "guard_collar", "obsidian", base=(24, 62, 66), smooth=45)
    a, b = S_COLLAR
    ml.lathe(co, "y", (0, 0, 0), [(y(a), R_COLLAR * 0.6), (y(a) + 1.0, R_COLLAR * 0.95), (y((a + b) / 2), R_COLLAR * 1.18), (y(b) - 1.0, R_COLLAR), (y(b), R_COLLAR * 0.55)], sides=12, squash=0.75)
    gm = w.add(m, "guard_gem", "crystal", base=(120, 255, 190), glow=True, smooth=0)
    ml.gem(gm, (0, y((a + b) / 2), R_COLLAR * 0.8), 0.55, long=1.3, sides=6, axis="z")
    # ---- 6-9 the four claws: swept, tapering, curving back toward the pommel
    for sgn, nm in ((-1, "l"), (1, "r")):
        for pts, tag, wid, thick in ((CLAW_OUT, "out", 9.0, 3.4), (CLAW_IN, "in", 7.0, 3.0)):
            cl = w.add(m, f"claw_{nm}_{tag}", "obsidian", base=(28, 70, 74), smooth=45)
            path = [(sgn * px * K, y(ps), 0.0) for (px, ps) in pts]
            ml.sweep(cl, path, ml.lozenge(1.0, 1.0, 0.0), scales=lambda t, wid=wid, thick=thick: (wid * K * (1.0 - t) ** 0.7 if t < 0.97 else 0.0, thick * K * (1.0 - 0.5 * t) if t < 0.97 else 0.0), up=(0, 0, 1))
    # ---- 10 the grip, 11/12 its lattice (two counter-wound cords), 13 the end bands
    gp = w.add(m, "grip", "leather", base=(30, 56, 56), uv_mode="loft")
    ml.loft(gp, "y", (0, 0, 0), [(y(S_GRIP[0]), 0.95, 0.95), (y(S_GRIP[0] + 20), 1.05, 1.05), (y(S_GRIP[1]), 1.0, 1.0)], ml.circle(R_GRIP, 12))
    for nm, ph, direction in (("grip_lattice_a", 0.0, 1), ("grip_lattice_b", 1.2, -1)):
        lat = w.add(m, nm, "bronze", base=(70, 120, 110), wear=0.4, smooth=60)
        pts = w.helix(lambda t: R_GRIP * 1.05, y(S_GRIP[0]) + 0.3, y(S_GRIP[1]) - 0.3, 3.5 * direction, 60, phase=ph)
        ml.sweep(lat, pts, ml.circle(0.16, 6), scales=lambda t: (1.0, 1.0))
    gb = w.add(m, "grip_bands", "bronze", base=(120, 160, 140))
    for s in (S_GRIP[0] + 1.0, S_GRIP[1] - 4.0):
        ml.collar(gb, y(s), R_GRIP * 1.22, 1.3, lip=0.3, sides=12)
    # ---- 14/15 the pommel cap and its flared rim
    pm = w.add(m, "pommel", "obsidian", base=(26, 66, 70), smooth=45)
    ml.lathe(pm, "y", (0, 0, 0), [(y(S_POMMEL[0]), R_GRIP * 1.1), (y(S_POMMEL[0]) + 0.5, R_GRIP * 1.7), (y(8), R_GRIP * 1.55), (y(S_POMMEL[1]), R_GRIP * 1.05)], sides=12, squash=0.8)
    pr = w.add(m, "pommel_rim", "bronze", base=(120, 160, 140))
    ml.collar(pr, y(S_POMMEL[0]) + 0.1, R_GRIP * 1.75, 0.5, lip=0.14, sides=12, oval=0.8)
    # ---- 16 the serpent of green energy: a tube winding up the blade's lower half, 17 its head, 18 the eyes, 19 the trailing wisp
    s0, s1 = S_SERPENT
    sp = w.add(m, "serpent", "crystal", base=(60, 200, 110), glow=True, smooth=60, uv_mode="loft")
    pts = []
    for i in range(0, 61):
        t = i / 60
        ang = 2 * math.pi * 1.7 * t + 0.4
        sl = s1 - (s1 - s0) * t
        pts.append((SERP_RX * (1.0 - 0.25 * t) * math.cos(ang), y(sl), SERP_RZ * math.sin(ang)))
    ml.sweep(sp, pts, ml.circle(1.0, 8), scales=lambda t: (SERP_TUBE * (0.55 + 0.45 * math.sin(math.pi * min(1.0, t * 1.2 + 0.1))), SERP_TUBE * (0.55 + 0.45 * math.sin(math.pi * min(1.0, t * 1.2 + 0.1)))))
    hd = w.add(m, "serpent_head", "crystal", base=(90, 230, 130), glow=True, smooth=60)
    hx, hy, hz = pts[-1]
    w.ellipsoid(hd, (hx, hy + 0.8, hz), SERP_TUBE * 0.85, SERP_TUBE * 1.5, SERP_TUBE * 0.75, sides=10, stacks=8)
    for sgn, nm in ((-1, "l"), (1, "r")):
        ey = w.add(m, f"serpent_eye_{nm}", "crystal", base=(255, 255, 200), glow=True, smooth=0)
        w.ellipsoid(ey, (hx + sgn * 0.5, hy + 1.8, hz + SERP_TUBE * 0.45), 0.22, 0.22, 0.22, sides=6, stacks=4)
    tl = w.add(m, "serpent_tail", "crystal", base=(70, 210, 120), glow=True, smooth=60, uv_mode="loft")
    tp = [(SERP_RX * 0.75, y(s1) + 1.0, 0.0), (SERP_RX * 1.1, y(s1) + 3.5, SERP_RZ * 0.6), (SERP_RX * 1.0, y(s1) + 7.5, SERP_RZ * 0.8), (SERP_RX * 0.2, y(s1) + 10.0, 0.0)]
    ml.sweep(tl, tp, ml.circle(1.0, 8), scales=lambda t: (SERP_TUBE * 0.7 * (1.0 - t) if t < 0.97 else 0.0, SERP_TUBE * 0.7 * (1.0 - t) if t < 0.97 else 0.0))

    atlas = ml.Atlas(1024)
    atlas.layout(m)
    atlas.paint(m)
    for nm in ("serpent", "serpent_head", "serpent_tail"):
        w.paint_part(atlas, m.part(nm), lambda c: (w.tint(c.img, (110, 255, 160), 0.5), w.set_glow(c, (80, 255, 140), 0.85, core=0.35)))
    for nm in ("serpent_eye_l", "serpent_eye_r"):
        w.paint_part(atlas, m.part(nm), lambda c: w.set_glow(c, (255, 255, 200), 1.0, core=0.7))
    w.paint_part(atlas, m.part("guard_gem"), lambda c: w.set_glow(c, (110, 255, 190), 1.0, core=0.4))
    for nm in ("fuller_f", "fuller_b"):
        w.paint_part(atlas, m.part(nm), lambda c: w.set_glow(c, (120, 255, 190), 0.5, core=0.2))
    w.paint_part(atlas, m.part("blade"), lambda c: w.set_glow(c, (100, 240, 170), 0.10, core=0.0))
    ref = w.reference("a9fb2b6b302ae781db2cba2fba934509.jpg", (455, 30, 593, 440), "deliverance")
    w.finish("deliverance", m, atlas, real_m=TOTAL_M, glow=("FFFFFF", 0.18, 0.5, 1.0), ref=ref)
