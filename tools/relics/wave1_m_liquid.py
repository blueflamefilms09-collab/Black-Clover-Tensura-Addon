"""Liquid Sword (card: Mr James Gifford, "Liquid Sword", rare, +1 sword): a sword whose crystal-water blade twists and drips like liquid, on a fluted
pale-green glass hilt with a pearl orb pommel and a small white wing-like crossguard. The 3D model, built with the owner's method.

Reference: the card, 620 x 585, the sword on the diagonal from the pommel (63, 437) to the point (529, 78) (588 px). The axis of the model is that
straight line; the blade's wave is read off as its lateral offset from it. s = card px along the axis from the pommel, l = card px across it.
"""
import math

import numpy as np

import mesh_lib as ml
import wave1_models as w

# ---------------------------------------------------------------- 1 measure
TOTAL_M = 1.05                          # a one-handed sword, pommel to point
S_TIP = 588.0                           # card px from the pommel to the point
K = w.px_to_cm(TOTAL_M * 100.0, S_TIP)  # cm per card px
BLADE_K = 1.0                           # the card's blade is drawn broad: widths are scaled down to this share (the length stays)
# hilt stations along the axis (card s)
S_POMMEL_C, R_POMMEL = 6.0, 13.0        # the pearl pommel: centre and radius
S_COLLAR_LO = (13.0, 24.0)              # the flared base of the fluted grip
S_GRIP = (24.0, 90.0)                   # the fluted glass grip, 22 px wide
R_GRIP = 10.5
S_CAPITAL = (90.0, 102.0)               # the column's capital under the guard orb
S_ORB_C, R_ORB = 123.0, 19.0            # the pearl orb of the guard (centre (161, 363), radius about 20)
S_SOCKET = (141.0, 158.0)               # the pearl cap the blade is seated in
S_BLADE0 = 150.0                        # the blade starts (its base at card (190, 341))
WING_SPAN, WING_S = 62.0, 133.0         # the wing tips: lateral +-51 px at s 133
# the blade: centre line l(s) (px across the axis), full width and thickness (px) along it. Landmarks read off the card:
#   crystal base s 159 l 1;  crystal end s 244 l -12;  the dip s 305 l 55;  the rise s 353 l 41;  l -16 at s 404;  crystal again from s 467 l -21;  the point
CENTRE = w.hermite([(150, 0), (200, -6), (244, -12), (272, 12), (305, 50), (335, 52), (353, 40), (380, 5), (404, -16), (440, -22), (467, -20), (520, -9), (588, 0)])
WIDTH = w.lerp_table([(150, 40), (200, 42), (244, 38), (262, 30), (290, 26), (330, 26), (360, 28), (385, 34), (404, 38), (440, 40), (500, 34), (550, 18), (588, 0)])
THICK = w.lerp_table([(150, 12), (244, 12), (262, 5), (380, 5), (404, 10), (500, 10), (550, 6), (588, 0)])
RIBBON = (258.0, 408.0)                 # where the blade is liquid (a thin flowing ribbon)
LOWER_CRYSTAL = (156.0, 246.0)          # the faceted crystal block at the base
UPPER_CRYSTAL = (402.0, 575.0)


def sph(r, n=10):
    """Sphere profile (s, radius) bottom to top."""
    return [(-r * math.cos(math.pi * i / n), r * math.sin(math.pi * i / n)) for i in range(n + 1)]


def build():
    m = ml.Mesh("liquid_sword")
    # ---- 1 the blade: one sweep along the wavy centre line, lozenge section, closing to a point
    blade = w.add(m, "blade", "crystal", base=(104, 198, 206), smooth=40, uv_mode="loft")
    ss = np.arange(S_BLADE0, S_TIP + 0.01, 8.0)
    if ss[-1] < S_TIP:
        ss = np.append(ss, S_TIP)
    path = [(CENTRE(s) * K, s * K, 1.6 * K * math.sin((s - 250.0) / 55.0) * (1.0 if RIBBON[0] - 30 < s < RIBBON[1] + 30 else 0.0)) for s in ss]
    sc = [(WIDTH(s) * BLADE_K * K, THICK(s) * K) for s in ss]
    sc[-1] = (0.0, 0.0)
    ml.sweep(blade, path, ml.lozenge(1.0, 1.0, 0.22), scales=sc)
    # ---- 2/3 wet beads along both edges of the liquid ribbon
    for nm, sgn in (("ribbon_bead_l", -1), ("ribbon_bead_r", 1)):
        b = w.add(m, nm, "crystal", base=(150, 226, 226), smooth=60)
        pts = [(CENTRE(s) * K + sgn * (WIDTH(s) * BLADE_K * 0.5) * K, s * K, 1.6 * K * math.sin((s - 250.0) / 55.0)) for s in np.arange(RIBBON[0], RIBBON[1], 8.0)]
        ml.sweep(b, pts, ml.circle(0.34, 6), scales=lambda t: (1.0, 1.0) if 0.06 < t < 0.94 else (0.5, 0.5))
    # ---- 4 facet seams on the lower crystal block (raised thin bars across the faces)
    seams = w.add(m, "facet_seams", "crystal", base=(190, 238, 238), wear=0.2)
    for i, (sa, sb, la, lb) in enumerate(((160, 200, -0.5, 0.35), (176, 236, 0.1, -0.4), (200, 240, -0.2, 0.5), (168, 228, 0.5, 0.0))):
        sg = 1 if i % 2 == 0 else -1
        for face in (1, -1):
            xa, xb = (CENTRE(sa) + la * WIDTH(sa) * BLADE_K * 0.5) * K, (CENTRE(sb) + lb * WIDTH(sb) * BLADE_K * 0.5) * K
            za, zb = face * THICK(sa) * K * 0.42, face * THICK(sb) * K * 0.42
            ml.bar(seams, (xa, sa * K, za), (xb, sb * K, zb), 0.22, 0.07, up=(0, 0, face), taper=0.4)
    # ---- 5 the fuller of the upper blade: two ridges per face
    ful = w.add(m, "upper_fuller", "crystal", base=(180, 236, 236), wear=0.2)
    for face in (1, -1):
        sa, sb = 430.0, 548.0
        ml.fuller(ful, (CENTRE(sa) * K, sa * K, face * THICK(sa) * K * 0.46), (CENTRE(sb) * K, sb * K, face * THICK(sb) * K * 0.44), (0, 0, face), 0.85, 0.16, 0.06, taper=3.0)
    # ---- 6-10 drips hanging off the ribbon (teardrops pointing up the blade)
    for i, (s, side, r) in enumerate(((285, 1, 0.55), (318, 1, 0.7), (346, 1, 0.5), (376, 1, 0.45), (398, -1, 0.6))):
        d = w.add(m, f"drip_{i}", "crystal", base=(150, 226, 232), smooth=70)
        x = CENTRE(s) * K + side * (WIDTH(s) * BLADE_K * 0.5 + 3 + 6 * i % 5) * K
        y = s * K - (4 + 3 * (i % 3))
        ml.lathe(d, "y", (x, y, 0.0), [(-r, 0.0), (-0.82 * r, 0.58 * r), (-0.2 * r, 0.98 * r), (0.5 * r, 0.8 * r), (1.5 * r, 0.34 * r), (2.6 * r, 0.0)], sides=8)
    # ---- 11 the pommel orb, 12 its flared collar
    po = w.add(m, "pommel_orb", "bone", base=(232, 246, 240), smooth=60)
    ml.lathe(po, "y", (0, S_POMMEL_C * K, 0), [(a * K, b * K) for a, b in sph(R_POMMEL, 10)], sides=14)
    pc = w.add(m, "pommel_collar", "silver", base=(196, 224, 220))
    a0, a1 = S_COLLAR_LO
    ml.lathe(pc, "y", (0, 0, 0), [(a0 * K, 8.5 * K), ((a0 + 3) * K, 12.5 * K), ((a0 + 6) * K, 11.0 * K), (a1 * K, 9.5 * K)], sides=14)
    # ---- 13 the fluted glass grip
    grip = w.add(m, "grip", "crystal", base=(196, 238, 220), smooth=25, uv_mode="loft")
    ml.loft(grip, "y", (0, 0, 0), [(S_GRIP[0] * K, 0.95, 0.95), ((S_GRIP[0] + 20) * K, 1.0, 1.0), ((S_GRIP[1] - 22) * K, 1.04, 1.04), (S_GRIP[1] * K, 0.96, 0.96)], w.scallop(R_GRIP * K, 12, 0.09, 48))
    # ---- 14 a ring at the grip's middle, 15 the capital under the orb
    gr = w.add(m, "grip_ring", "silver", base=(204, 228, 224))
    ml.collar(gr, 54 * K, R_GRIP * K * 1.12, 4 * K, lip=0.3, sides=14)
    cap = w.add(m, "capital", "silver", base=(200, 226, 222))
    c0, c1 = S_CAPITAL
    ml.lathe(cap, "y", (0, 0, 0), [(c0 * K, R_GRIP * K), ((c0 + 3) * K, 14 * K), ((c1 - 3) * K, 14.5 * K), (c1 * K, 9 * K)], sides=14)
    # ---- 16 the guard orb with 17 its equator band
    orb = w.add(m, "guard_orb", "bone", base=(228, 244, 236), smooth=60)
    ml.lathe(orb, "y", (0, S_ORB_C * K, 0), [(a * K, b * K) for a, b in sph(R_ORB, 12)], sides=18)
    band = w.add(m, "orb_band", "silver", base=(210, 232, 228))
    ml.collar(band, (S_ORB_C - 1.6) * K, R_ORB * K * 1.04, 3.2 * K, lip=0.25, sides=18)
    # ---- 18 the socket the blade is seated in
    sk = w.add(m, "blade_socket", "bone", base=(236, 246, 242))
    a0, a1 = S_SOCKET
    ml.lathe(sk, "y", (0, 0, 0), [(a0 * K, 11 * K), ((a0 + 4) * K, 16.5 * K), ((a1 - 3) * K, 17.5 * K), (a1 * K, 12 * K)], sides=12, squash=0.5)
    # ---- 19-22 the wings: two swept feathers each side, curving up toward the blade
    for sgn, nm in ((-1, "l"), (1, "r")):
        for k, (span, curl, wid) in enumerate(((WING_SPAN, 1.0, 9.0), (WING_SPAN * 0.72, 0.6, 6.5))):
            wing = w.add(m, f"wing_{nm}{k}", "bone", base=(238, 246, 244), smooth=45, uv_mode="loft")
            pts = []
            for i in range(9):
                t = i / 8
                x = sgn * (R_ORB * 0.7 + (span - R_ORB * 0.7) * math.sin(t * math.pi / 2)) * K
                s = (S_ORB_C - 6 * k + 14 * curl * t * t - 5 * t) * K
                pts.append((x, s, (1.0 + 1.5 * k) * K * math.sin(t * math.pi) * 3))
            ml.sweep(wing, pts, ml.rect_ring(1.0, 1.0), scales=lambda t, wid=wid: (wid * K * (1.0 - 0.55 * t) if t < 0.96 else 0.0, 1.6 * K if t < 0.96 else 0.0))

    # ---- 5 the painted skin
    atlas = ml.Atlas(1024)
    atlas.layout(m)
    atlas.paint(m)
    _paint(m, atlas)
    ref = w.reference("86f6e5e808458542139072e455e2e497.jpg", (30, 60, 560, 450), "liquid_sword")
    w.finish("liquid_sword", m, atlas, real_m=TOTAL_M, glow=("FFFFFF", 0.16, 0.5, 1.0), ref=ref)


def _paint(m, atlas):
    def water(c):
        n = w.noise2(c.img.shape[:2], 14, 31)
        k = (0.25 + 0.55 * n)[..., None]
        c.img[..., :3] = c.img[..., :3] * (1 - 0.45 * k) + np.array([235, 252, 250], np.float32) * (0.45 * k)
        ribbon = np.clip(1.0 - np.abs(c.T * (S_TIP - S_BLADE0) + S_BLADE0 - 335.0) / 90.0, 0, 1)
        w.set_glow(c, (110, 240, 235), 0.20 + 0.28 * ribbon.mean(), core=0.25)
    w.paint_part(atlas, m.part("blade"), water)
    for nm in ("ribbon_bead_l", "ribbon_bead_r") + tuple(f"drip_{i}" for i in range(5)):
        w.paint_part(atlas, m.part(nm), lambda c: w.set_glow(c, (130, 240, 236), 0.30, core=0.3))
    w.paint_part(atlas, m.part("guard_orb"), lambda c: (w.tint(c.img, (232, 250, 244), 0.4), w.set_glow(c, (170, 255, 235), 0.28, core=0.4)))
    w.paint_part(atlas, m.part("pommel_orb"), lambda c: w.set_glow(c, (170, 255, 235), 0.2, core=0.4))
    w.paint_part(atlas, m.part("grip"), lambda c: w.set_glow(c, (150, 240, 215), 0.10, core=0.1))
