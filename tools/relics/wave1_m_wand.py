"""Wand of Bubbles (card: Mr James Gifford, "Wand of Bubbles", common): a long thin pale lavender iridescent rod with a brown spiral-wrapped grip, a
bubble pommel, a small ring loop at the tip with a soap bubble blown from it, and translucent rainbow bubbles around. The 3D model, built with the
owner's method.

Reference: the card, 1000 x 943; the wand runs from its cap (237, 584) to its tip loop (755, 204): 642 px. s = card px from the cap along that line.
Axis: y up from the pommel to the tip; the blown bubble hangs to -x of the loop.
"""
import math

import numpy as np

import mesh_lib as ml
import wave1_models as w

# ---------------------------------------------------------------- 1 measure
TOTAL_M = 0.52                                       # a wand: grip, rod and the loop, about half a metre
S_TIP = 660.0                                        # the loop's far side
S_POMMEL_R = 21.0                                    # the bubble pommel, radius in px (it sits just beyond the cap)
K = w.px_to_cm(TOTAL_M * 100.0, S_TIP + 2 * S_POMMEL_R)  # cm per card px (0.074)
S_CAP = (0.0, 12.0)                                  # the flared cap at the base of the grip
S_GRIP = (12.0, 200.0)                               # the wrapped grip (card (237, 584) to the collar at (405, 471))
R_GRIP = 13.0 * K                                    # grip radius: card width about 26 px at the swell
S_COLLAR = (198.0, 216.0)                            # the collar with its flange (card width 23 px)
R_COLLAR = 12.5 * K
S_ROD = (214.0, 650.0)                               # the rod to the loop
R_ROD = 4.0 * K                                      # card width 8 px
LOOP_R = 10.0 * K                                    # the tip loop, 20 px across
BLOB = (6.5, 2.8, 2.8)                               # the blown bubble: half extents in cm (lateral, along, through), hung to -x of the loop


def y(s):
    return (s + 2 * S_POMMEL_R) * K


def build():
    m = ml.Mesh("wand_of_bubbles")
    # ---- 1 the bubble pommel
    pb = w.add(m, "pommel_bubble", "crystal", base=(205, 232, 244), smooth=70)
    w.ellipsoid(pb, (0, S_POMMEL_R * K, 0), S_POMMEL_R * K, S_POMMEL_R * K, S_POMMEL_R * K, sides=16, stacks=12)
    # ---- 2 the flared cap
    cap = w.add(m, "cap", "bronze", base=(122, 84, 54), smooth=45)
    ml.lathe(cap, "y", (0, 0, 0), [(y(S_CAP[0]) - 0.3, 0.6), (y(S_CAP[0]), R_GRIP * 1.35), (y(S_CAP[0]) + 0.5, R_GRIP * 1.25), (y(S_CAP[1]), R_GRIP * 0.95)], sides=12)
    # ---- 3 the grip: a swelling brown loft, 4/5 two pale swirl cords winding over it
    gp = w.add(m, "grip", "wood", base=(112, 76, 50), uv_mode="loft")
    ml.loft(gp, "y", (0, 0, 0), [(y(S_GRIP[0]), 0.95, 0.95), (y(80), 1.05, 1.05), (y(150), 1.0, 1.0), (y(S_GRIP[1]), 0.9, 0.9)], ml.circle(R_GRIP, 12))
    for nm, ph, d in (("swirl_a", 0.0, 1), ("swirl_b", math.pi, 1)):
        sw = w.add(m, nm, "bone", base=(200, 186, 236), wear=0.2, smooth=60)
        pts = w.helix(lambda t: R_GRIP * 1.06, y(S_GRIP[0]) + 0.4, y(S_GRIP[1]) - 0.4, 3.2 * d, 70, phase=ph)
        ml.sweep(sw, pts, ml.circle(0.2, 6), scales=lambda t: (1.0, 1.0) if 0.04 < t < 0.96 else (0.5, 0.5))
    # ---- 6 a ring in the grip's middle
    gr = w.add(m, "grip_ring", "bronze", base=(150, 110, 74))
    ml.collar(gr, y(100), R_GRIP * 1.1, 0.5, lip=0.12, sides=12)
    # ---- 7 the collar with its flange, 8 the collar's rim
    co = w.add(m, "collar", "bronze", base=(118, 80, 52), smooth=45)
    ml.lathe(co, "y", (0, 0, 0), [(y(S_COLLAR[0]), R_GRIP * 0.9), (y(S_COLLAR[0]) + 0.3, R_COLLAR), (y(205), R_COLLAR * 0.9), (y(S_COLLAR[1]) - 0.4, R_COLLAR * 1.18), (y(S_COLLAR[1]), R_ROD * 1.6)], sides=12)
    cr = w.add(m, "collar_rim", "silver", base=(206, 196, 230))
    ml.collar(cr, y(S_COLLAR[1]) - 0.8, R_COLLAR * 1.2, 0.5, lip=0.14, sides=12)
    # ---- 9 the rod: a long thin iridescent loft with a tiny taper
    rd = w.add(m, "rod", "crystal", base=(190, 178, 238), uv_mode="loft", smooth=40)
    ml.loft(rd, "y", (0, 0, 0), [(y(S_ROD[0]), 1.15, 1.15), (y(S_ROD[0] + 120), 1.0, 1.0), (y(S_ROD[1]), 0.8, 0.8)], ml.circle(R_ROD, 8))
    # ---- 10 the loop at the tip: a swept ring; 11 the blown bubble; 12/13 two small bubbles clinging to it
    cy = y(S_TIP) - LOOP_R
    lp = w.add(m, "tip_loop", "silver", base=(206, 196, 230), smooth=45)
    ml.sweep(lp, [(-LOOP_R * math.cos(math.radians(a)) - LOOP_R, cy + LOOP_R * math.sin(math.radians(a)), 0.0) for a in range(0, 361, 20)], ml.circle(0.16, 6), caps=(False, False))
    bb = w.add(m, "bubble_blob", "crystal", base=(206, 232, 244), smooth=70)
    w.ellipsoid(bb, (-LOOP_R * 2 - BLOB[0] * 0.85, cy + 0.6, 0.0), BLOB[0], BLOB[1], BLOB[2], sides=18, stacks=12)
    for nm, (dx, dy, r) in (("bubble_s1", (-4.0, 2.2, 1.5)), ("bubble_s2", (-7.5, -1.4, 1.1))):
        sb = w.add(m, nm, "crystal", base=(210, 236, 246), smooth=70)
        w.ellipsoid(sb, (-LOOP_R * 2 + dx, cy + dy, 1.4), r, r, r, sides=12, stacks=8)
    # ---- 14-16 three free bubbles floating near the rod and grip
    for i, (px, ps, r) in enumerate(((3.4, 420.0, 2.0), (-3.0, 300.0, 1.5), (4.0, 120.0, 1.2))):
        fb = w.add(m, f"bubble_free_{i}", "crystal", base=(206, 234, 246), smooth=70)
        w.ellipsoid(fb, (px, y(ps), 0.6 * i), r, r, r, sides=12, stacks=8)

    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    _paint(m, atlas)
    ref = w.reference("c7684be5cf4942642676e18da0345be4.jpg", (190, 110, 800, 640), "wand_of_bubbles")
    w.finish("wand_of_bubbles", m, atlas, real_m=TOTAL_M, glow=("FFFFFF", 0.22, 0.45, 1.0), ref=ref)


def _paint(m, atlas):
    def pearl(c):                                                       # a soap film: pale body, a rainbow sheen drifting over it
        n = w.noise2(c.img.shape[:2], 16, 41)
        hue = w.hsv_rows(np.clip(n * 1.1, 0, 1), 0.28, 1.0)
        c.img[..., :3] = c.img[..., :3] * 0.72 + hue * 0.28
        c.glow[..., :3] = w.hsv_rows(np.clip(n * 1.1 + 0.1, 0, 1), 0.4, 1.0)
        c.glow[..., 3] = 80
    for nm in ["pommel_bubble", "bubble_blob", "bubble_s1", "bubble_s2", "bubble_free_0", "bubble_free_1", "bubble_free_2"]:
        w.paint_part(atlas, m.part(nm), pearl)

    def rod(c):                                                         # the rod's sheen shifts from lavender to blue-green along its length
        t = np.clip(c.T, 0, 1)
        c.img[..., :3] = c.img[..., :3] * 0.7 + w.hsv_rows(0.62 + 0.18 * t, 0.35, 1.0) * 0.3
        c.glow[..., :3] = w.hsv_rows(0.55 + 0.3 * t, 0.6, 1.0)
        c.glow[..., 3] = 70
    w.paint_part(atlas, m.part("rod"), rod)
