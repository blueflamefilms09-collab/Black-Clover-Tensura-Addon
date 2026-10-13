"""Astralis - The Eye of the Universe (card: Bonus Action, legendary staff, requires attunement by a spellcaster): a slender silver staff with a purple
wrapped grip, topped by a swept silver filigree crescent (wings) cradling a large faceted purple gem in a ring; a small silver foot cap.
The 3D model, built with the owner's method.

Reference: the card, 554 x 554; the staff leans from its foot (324, 497) to the gem (447, 133). s = card px along that line (384 to the gem's centre).
Axis: y up from the foot to the head; the head faces +z.
"""
import math

import numpy as np

import mesh_lib as ml
import wave1_models as w

# ---------------------------------------------------------------- 1 measure
TOTAL_M = 1.85
S_GEM = 384.0                                        # card px from the foot to the gem's centre (card (447, 133))
S_TOP = S_GEM + 30.0                                 # the head's top (ring radius 28 px, plus a margin)
K = w.px_to_cm(TOTAL_M * 100.0, S_TOP + 8.0)         # cm per card px (the foot cap hangs 8 px below s = 0): 0.44
HK = 0.85                                            # the card draws the head large; it is scaled to this share of the measured size
S0 = 8.0                                             # model y = (s + S0) * K
R_SHAFT = 3.3 * K                                    # card shaft width 6.7 px
S_FOOT = (-8.0, 12.0)                                # the foot cap: point at -8, flare at 0, neck to 12
S_BLUE_COLLAR = 262.0                                # the small blue ring where the silver shaft meets the grip
S_GRIP = (268.0, 318.0)                              # the purple beaded grip, 6 beads
R_BEAD, R_BEAD_NECK = 5.6 * K, 4.2 * K
S_FERRULE = (318.0, 340.0)                           # the silver socket flaring up into the head
RING_R, RING_T = 28.0 * K * HK, 2.6 * K * HK         # the ring round the gem: radius 28 px, tube 2.6 px
GEM_R = 15.0 * K * HK                                # the faceted gem
WING_REACH, WING_W = 62.0 * K * HK, 26.0 * K * HK    # a wing: how far it sweeps from the ring's edge, how wide at the root
CURL_R = 6.0 * K * HK                                # the two spirals under the wings
Z_T = 1.1                                            # the filigree's thickness, cm


def y(s):
    return (s + S0) * K


def build():
    m = ml.Mesh("astralis")
    # ---- 1 foot cap: a flared cup with a point
    ft = w.add(m, "foot_cap", "silver", base=(200, 206, 222), smooth=40)
    ml.lathe(ft, "y", (0, 0, 0), [(y(S_FOOT[0]), 0.0), (y(S_FOOT[0]) + 1.2, 1.9), (y(0), 2.6), (y(5), 1.7), (y(S_FOOT[1]), R_SHAFT)], sides=10)
    # ---- 2 the long silver shaft
    sh = w.add(m, "shaft", "silver", base=(196, 202, 216), uv_mode="loft")
    ml.loft(sh, "y", (0, 0, 0), [y(S_FOOT[1] - 2), y(S_BLUE_COLLAR)], ml.circle(R_SHAFT, 12))
    # ---- 3 the blue collar and 4 the beaded purple grip
    bc = w.add(m, "blue_collar", "bronze", base=(80, 110, 190))
    ml.collar(bc, y(S_BLUE_COLLAR), R_SHAFT * 1.5, 2.2, lip=0.4, sides=12)
    gr = w.add(m, "grip_beads", "crystal", base=(112, 56, 190), smooth=60)
    prof = [(y(S_GRIP[0]), R_BEAD_NECK)]
    nb = 6
    for i in range(nb):
        a = y(S_GRIP[0]) + (y(S_GRIP[1]) - y(S_GRIP[0])) * i / nb
        b = y(S_GRIP[0]) + (y(S_GRIP[1]) - y(S_GRIP[0])) * (i + 1) / nb
        prof += [(a + (b - a) * 0.2, R_BEAD * 0.92), ((a + b) / 2, R_BEAD), (b - (b - a) * 0.2, R_BEAD * 0.92), (b, R_BEAD_NECK)]
    ml.lathe(gr, "y", (0, 0, 0), prof, sides=12)
    # ---- 5 the ferrule flaring into the head
    fe = w.add(m, "ferrule", "silver", base=(206, 212, 228), smooth=45)
    ml.lathe(fe, "y", (0, 0, 0), [(y(S_FERRULE[0]), R_BEAD_NECK), (y(S_FERRULE[0]) + 1.2, R_BEAD * 1.15), (y(S_FERRULE[0]) + 3.0, R_BEAD * 0.9), (y(S_FERRULE[1]), RING_T * 1.8)], sides=12)
    fb = w.add(m, "ferrule_band", "gold", base=(214, 190, 120))
    ml.collar(fb, y(S_FERRULE[0]) + 1.5, R_BEAD * 1.18, 0.7, lip=0.18, sides=12)
    # ---- 6 the ring round the gem (a torus swept in the xy plane), 7 its bezel studs
    cy = y(S_GEM)
    ring = w.add(m, "gem_ring", "silver", base=(208, 214, 230), smooth=45)
    rp = [(RING_R * math.cos(math.radians(a)), cy + RING_R * math.sin(math.radians(a)), 0.0) for a in range(0, 361, 12)]
    ml.sweep(ring, rp, ml.circle(RING_T, 8), caps=(False, False))
    studs = w.add(m, "ring_studs", "silver", base=(226, 230, 244))
    for i in range(12):
        a = math.radians(i * 30 + 15)
        ml.stud(studs, (RING_R * math.cos(a), cy + RING_R * math.sin(a), RING_T * 0.9), 0.28, 0.3, (0, 0, 1), 6)
    # ---- 8 the gem and 9 its bezel prongs
    gem = w.add(m, "gem", "crystal", base=(138, 70, 224), glow=True, smooth=0)
    ml.gem(gem, (0, cy, 0), GEM_R, long=1.0, sides=6, axis="z")
    pr = w.add(m, "gem_prongs", "silver", base=(214, 220, 236))
    for i in range(6):
        a = math.radians(i * 60 + 30)
        ml.stud(pr, (GEM_R * 1.05 * math.cos(a), cy + GEM_R * 1.05 * math.sin(a), 0.7), 0.3, 0.45, (math.cos(a), math.sin(a), 0.6), 6)
    # ---- 10/11 the swept wings: filigree plates curving away from the ring
    for sgn, nm in ((-1, "l"), (1, "r")):
        wing = w.add(m, f"wing_{nm}", "silver", base=(204, 210, 228), smooth=40, uv_mode="loft")
        pts = []
        for i in range(0, 15):
            t = i / 14
            x = sgn * (RING_R * 0.85 + WING_REACH * t)
            yy = cy - 0.25 * WING_REACH * math.sin(t * math.pi * 0.9) + 0.35 * WING_REACH * t ** 3
            pts.append((x, yy, 0.0))
        ml.sweep(wing, pts, ml.bevelled(1.0, 1.0, 0.5), scales=lambda t: (WING_W * (1.0 - t * 0.9) if t < 0.97 else 0.0, Z_T if t < 0.97 else 0.0), up=(0, 0, 1))
        lat = w.add(m, f"lattice_{nm}", "silver", base=(232, 236, 248), wear=0.8)         # the diamond lattice on the wing: crossed raised bars
        for k in range(5):
            t0, t1 = 0.12 + 0.15 * k, 0.12 + 0.15 * k + 0.12
            def pt(t, side):
                x = sgn * (RING_R * 0.85 + WING_REACH * t)
                yy = cy - 0.25 * WING_REACH * math.sin(t * math.pi * 0.9) + 0.35 * WING_REACH * t ** 3
                return (x, yy + side * WING_W * (1.0 - t * 0.9) * 0.26, Z_T * 0.5 + 0.02)
            ml.bar(lat, pt(t0, -1), pt(t1, 1), 0.18, 0.06, up=(0, 0, 1), taper=0.2)
            ml.bar(lat, pt(t0, 1), pt(t1, -1), 0.18, 0.06, up=(0, 0, 1), taper=0.2)
    # ---- 12/13 the two spirals hanging under the wing roots
    for sgn, nm in ((-1, "l"), (1, "r")):
        cu = w.add(m, f"curl_{nm}", "silver", base=(210, 216, 232), smooth=45)
        pts = []
        for i in range(0, 20):
            a = math.radians(i * 20)
            r = CURL_R * (1.0 - 0.7 * i / 19) + 0.2
            pts.append((sgn * (RING_R * 0.95 + CURL_R * 0.4) + sgn * r * math.sin(a) * 0.9, cy - RING_R * 0.55 - CURL_R - r * math.cos(a), 0.0))
        ml.sweep(cu, pts, ml.circle(0.34, 6), scales=lambda t: (1.0 - 0.5 * t, 1.0 - 0.5 * t))
    # ---- 14 the strut from the ferrule up to the ring's foot
    st = w.add(m, "ring_strut", "silver", base=(204, 210, 228), smooth=45)
    ml.loft(st, "y", (0, 0, 0), [y(S_FERRULE[1]) - 0.5, cy - RING_R + 0.2], ml.circle(RING_T * 0.9, 8))

    atlas = ml.Atlas(1024)
    atlas.layout(m)
    atlas.paint(m)
    w.paint_part(atlas, m.part("gem"), lambda c: w.set_glow(c, (150, 70, 255), 0.5, core=0.12))
    w.paint_part(atlas, m.part("grip_beads"), lambda c: w.set_glow(c, (150, 90, 240), 0.18, core=0.1))
    for nm in ("wing_l", "wing_r", "gem_ring"):
        w.paint_part(atlas, m.part(nm), lambda c: w.set_glow(c, (200, 170, 255), 0.10, core=0.0))
    ref = w.reference("9befefa21a9a4213f6ebc0fd08940785.jpg", (280, 60, 530, 520), "astralis")
    w.finish("astralis_the_eye_of_the_universe", m, atlas, real_m=TOTAL_M, glow=("FFFFFF", 0.14, 0.55, 1.0), ref=ref)
