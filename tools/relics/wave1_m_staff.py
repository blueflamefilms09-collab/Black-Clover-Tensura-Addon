"""Staff of Moonlight (card: Catilus, "Staff of Moonlight", very rare): a silver staff, a crescent of runes cradling a pale full moon, silver
diamond joints, a star-speckled blue wrapped grip with two long ribbons. The 3D model, built with the owner's method.

Reference: the card, 736 x 1019, the staff down the right (x about 548 at the head); the card px measured are the P_* constants below.
Axis: y up the staff from the foot (0) to the crescent's tip; the crescent faces +z.
"""
import math

import numpy as np

import mesh_lib as ml
import wave1_models as w

# ---------------------------------------------------------------- 1 measure: card pixels -> cm
K = w.px_to_cm(185.0, 948 - 172)       # a staff is 1.85 m: the crescent's top (card y 172) to the foot's point (card y 948) is 776 px
AX = 548.0                             # card x of the shaft's axis at the head


def Y(py):
    return (948.0 - py) * K


def X(px):
    return (px - AX) * K


SHAFT_K = 0.85                         # the card draws the shaft heavier than a hand holds: radii are scaled down to this share
# foot (card y): the point, the widest of the foot diamond, the neck above it, the lower joint
P_FOOT_TIP, P_FOOT_WIDE, P_FOOT_NECK, P_JOINT_LO_BOT, P_JOINT_LO_TOP = 948, 922, 915, 890, 868
R_FOOT, R_NECK, R_JOINT_LO = 12 * K * SHAFT_K, 6.5 * K, 11 * K * SHAFT_K            # card half-widths 12 / 6.5 / 11 px
# the wrapped grip and the silver neck above it
P_WRAP_BOT, P_WRAP_TOP = 868, 440
R_WRAP = 10.5 * K * SHAFT_K                                                          # card width 21 px
P_COLLAR_BOT, P_COLLAR_TOP = 440, 428                                                # the silver cap that closes the wrap
P_NECK_BOT, P_NECK_TOP = 428, 352
R_NECK_UP = 8.5 * K * SHAFT_K
P_SWELL1, R_SWELL1, H_SWELL1 = 416, 14 * K * SHAFT_K, 15                              # the large lozenge joint above the wrap (centre, radius, half height px)
P_SWELL2, R_SWELL2, H_SWELL2 = 368, 11 * K * SHAFT_K, 13                              # the second lozenge, higher
P_BAND_Y = (394, 346)                                                                # two thin bands
P_CRADLE_BOT, P_CRADLE_TOP, R_CRADLE = 357, 336, 17 * K                              # the diamond cradle under the crescent (card y; half width 17 px)
# the crescent
CRES_CY = Y(252)                                                                     # centre of its outer circle (card (548, 252))
CRES_R_OUT = 80 * K                                                                  # outer radius 80 px (top y 172, bottom y 335)
CRES_W_MAX = 32 * K                                                                  # band width on the right (x 600..632)
CRES_HALF = 129.0                                                                    # the tips sit 129 degrees round from the right, left of centre
CRES_T = 1.5                                                                         # thickness through the card plane (not measurable: 6 px of rim)
# the moon and what holds it
ORB_CX, ORB_CY, ORB_R = X(545), Y(255), 33 * K                                       # the pale orb: centre (545, 255), radius 33 px
ORB_RING_R = 36 * K                                                                  # the dark rim ring round it
PIN_R = 0.55                                                                         # the thin pin that holds the moon to the cradle
# ribbons: card (x, y) control points and widths
RIB_A = [(554, 438), (562, 472), (576, 508), (592, 542), (606, 574), (612, 608), (604, 642), (586, 672), (562, 688)]
RIB_B = [(548, 628), (540, 672), (520, 722), (508, 770), (516, 812), (540, 840), (574, 846), (604, 846), (618, 862), (600, 884), (580, 880)]
RIB_W0, RIB_W1, RIB_T = 18 * K, 7 * K, 0.14


def build():
    m = ml.Mesh("staff_of_moonlight")
    # ---- 1 the foot: a diamond cap, its neck
    foot = w.add(m, "foot_cap", "silver", smooth=40)
    ml.lathe(foot, "y", (0, 0, 0), [(Y(P_FOOT_TIP), 0.0), (Y(P_FOOT_WIDE) + 0.5, R_FOOT * 0.8), (Y(P_FOOT_WIDE), R_FOOT), (Y(P_FOOT_NECK), R_NECK * 1.3),
                                    (Y(P_JOINT_LO_BOT), R_NECK)], sides=8)
    # ---- 2 the lower joint: a lozenge
    jl = w.add(m, "lower_joint", "silver", smooth=40)
    mid = (Y(P_JOINT_LO_BOT) + Y(P_JOINT_LO_TOP)) / 2
    ml.lathe(jl, "y", (0, 0, 0), [(Y(P_JOINT_LO_BOT) - 0.3, R_NECK), (mid, R_JOINT_LO), (Y(P_JOINT_LO_TOP) + 0.3, R_WRAP * 0.95)], sides=8)
    # ---- 3 the wrapped grip: a round loft with a slow swell, wrapped in blue cloth
    grip = w.add(m, "grip_wrap", "cloth", base=(62, 76, 142), uv_mode="loft")
    y0, y1 = Y(P_WRAP_BOT), Y(P_WRAP_TOP)
    ml.loft(grip, "y", (0, 0, 0), [(y0, 0.96, 0.96), (y0 + 8, 1.0, 1.0), ((y0 + y1) / 2, 1.03, 1.03), (y1 - 8, 1.0, 1.0), (y1, 0.97, 0.97)], ml.circle(R_WRAP, 14))
    # ---- 4 the wrap's cord rings: thin raised rings every ~3.4 cm following the card's curved wrap lines
    bands = w.add(m, "wrap_bands", "cloth", base=(96, 112, 180), wear=0.3)
    n_bands = 30
    for i in range(n_bands):
        yb = y0 + 2.4 + (y1 - y0 - 4.8) * i / (n_bands - 1)
        ml.collar(bands, yb, R_WRAP * 1.04, 0.5, lip=0.14, sides=14)
    # ---- 5/6 the silver caps at the two ends of the wrap
    for nm, ya, yb in (("wrap_cap_low", Y(P_WRAP_BOT) - 0.9, Y(P_WRAP_BOT) + 0.5), ("wrap_cap_high", Y(P_COLLAR_BOT) - 0.5, Y(P_COLLAR_TOP) + 0.3)):
        cap = w.add(m, nm, "silver")
        ml.collar(cap, ya, R_WRAP * 1.12, yb - ya, lip=0.2, sides=14)
    # ---- 7 the silver neck between the wrap and the cradle
    neck = w.add(m, "neck", "silver", uv_mode="loft")
    ml.loft(neck, "y", (0, 0, 0), [Y(P_NECK_BOT), Y(P_NECK_TOP)], ml.circle(R_NECK_UP, 10))
    # ---- 8/9 the two lozenge joints and the thin bands
    for nm, pc, r, hh in (("joint_swell_a", P_SWELL1, R_SWELL1, H_SWELL1), ("joint_swell_b", P_SWELL2, R_SWELL2, H_SWELL2)):
        j = w.add(m, nm, "silver", smooth=35)
        yc = Y(pc)
        ml.lathe(j, "y", (0, 0, 0), [(yc - hh * K, R_NECK_UP * 0.9), (yc - hh * K * 0.45, r * 0.92), (yc, r), (yc + hh * K * 0.45, r * 0.92), (yc + hh * K, R_NECK_UP * 0.9)], sides=8)
    nb = w.add(m, "neck_bands", "silver")
    for pb in P_BAND_Y:
        ml.collar(nb, Y(pb), R_NECK_UP * 1.25, 0.6, lip=0.18, sides=12)
    # ---- 10 the cradle diamond under the crescent: a flat lozenge
    cr = w.add(m, "cradle", "silver", smooth=40)
    ml.lathe(cr, "y", (0, 0, 0), [(Y(P_CRADLE_BOT), R_NECK_UP), (Y(P_CRADLE_BOT) + (Y(P_CRADLE_TOP) - Y(P_CRADLE_BOT)) * 0.55, R_CRADLE), (Y(P_CRADLE_TOP) + 0.4, R_CRADLE * 0.35)],
              sides=8, squash=0.55)
    # ---- 11 the crescent: swept along an arc, widest on the right, closing to points at both tips
    def width(th):
        return CRES_W_MAX * max(0.0, 1.0 - (abs(th) / CRES_HALF) ** 2)

    def r_mid(th):
        return CRES_R_OUT - width(th) / 2
    cres = w.add(m, "crescent", "silver", smooth=40)
    n = 48
    path = w.arc_path(0.0, CRES_CY, r_mid, -CRES_HALF, CRES_HALF, n)
    ml.sweep(cres, path, ml.bevelled(1.0, 1.0, 0.55),
             scales=lambda t: (max(width(-CRES_HALF + 2 * CRES_HALF * t), 0.0) if 0 < t < 1 else 0.0, CRES_T * (math.sqrt(max(width(-CRES_HALF + 2 * CRES_HALF * t), 0.0) / CRES_W_MAX) * 0.9 + 0.1) if 0 < t < 1 else 0.0))
    # ---- 12/13 raised rims along the crescent's outer and inner edge
    for nm, off in (("crescent_rim_out", 0.0), ("crescent_rim_in", 1.0)):
        rim = w.add(m, nm, "silver", smooth=40)
        pts = []
        for i in range(n + 1):
            th = -CRES_HALF + 2 * CRES_HALF * i / n
            wd = width(th)
            r = CRES_R_OUT - 0.12 if off == 0.0 else CRES_R_OUT - wd + 0.12
            pts.append((r * math.cos(math.radians(th)), CRES_CY + r * math.sin(math.radians(th)), CRES_T * 0.42))
        ml.sweep(rim, pts[3:-3], ml.circle(0.22, 6), scales=lambda t: (1.0, 1.0) if 0.04 < t < 0.96 else (0.4, 0.4))
    # ---- 14 the rune ridges: pairs of raised lines along the broad part of the band (the engraved glyph ring)
    runes = w.add(m, "rune_ridges", "silver", wear=0.7)
    for i in range(22):
        th = 118 - 236 * i / 21
        wd = width(th)
        if wd < 3.0:
            continue
        r = CRES_R_OUT - wd / 2
        a = math.radians(th)
        tang = np.array([-math.sin(a), math.cos(a), 0.0])
        c = np.array([r * math.cos(a), CRES_CY + r * math.sin(a), CRES_T * 0.5 * 0.5])
        rad = np.array([math.cos(a), math.sin(a), 0.0])
        glyph = 0.9
        ml.bar(runes, c - tang * glyph - rad * wd * 0.2, c + tang * glyph + rad * wd * 0.2, 0.26, 0.08, up=(0, 0, 1), taper=0.2)
    # ---- 15/16 finials: small domes on the two tips
    for sgn, nm in ((1, "finial_top"), (-1, "finial_bottom")):
        fin = w.add(m, nm, "silver")
        a = math.radians(sgn * CRES_HALF)
        tip = (CRES_R_OUT * 0.0 + (CRES_R_OUT - 0.2) * math.cos(a), CRES_CY + (CRES_R_OUT - 0.2) * math.sin(a), 0.0)
        ml.lathe(fin, "z", tip, [(-0.2, 0.0), (0.1, 0.5), (0.5, 0.45), (0.9, 0.0)], sides=8)
    # ---- 17 the pin that holds the moon to the cradle (inside the crescent)
    pin = w.add(m, "moon_pin", "silver")
    ml.loft(pin, "y", (0, 0, 0), [Y(P_CRADLE_TOP) - 0.2, ORB_CY - ORB_R + 0.4], ml.circle(PIN_R, 8))
    # ---- 18 the moon: a sphere with a rim ring
    orb = w.add(m, "moon_orb", "stone", smooth=60, uv_mode="loft")
    prof = [(ORB_CY - ORB_R + (2 * ORB_R) * (1 - math.cos(math.pi * i / 16)) / 2, ORB_R * math.sin(math.pi * i / 16)) for i in range(17)]
    ml.lathe(orb, "y", (ORB_CX, 0, 0), prof, sides=20)
    ring = w.add(m, "moon_ring", "silver", smooth=40)
    rpath = [(ORB_CX + ORB_RING_R * math.cos(math.radians(a)), ORB_CY + ORB_RING_R * math.sin(math.radians(a)), 0.0) for a in range(0, 361, 15)]
    ml.sweep(ring, rpath, ml.circle(0.28, 6), caps=(False, False))
    # ---- 19/20 the two ribbons: flat swept strips, the second wound once round the shaft
    for nm, pts, zf in (("ribbon_a", RIB_A, lambda i, n_: 0.6 + 1.6 * math.sin(i / n_ * math.pi)), ("ribbon_b", RIB_B, lambda i, n_: 3.0 * math.sin(i / n_ * math.pi * 1.2) - 0.4)):
        rb = w.add(m, nm, "cloth", base=(100, 116, 184), wear=0.2, uv_mode="loft")
        path3 = [(X(px), Y(py), zf(i, len(pts) - 1)) for i, (px, py) in enumerate(pts)]
        ml.sweep(rb, path3, ml.rect_ring(1.0, 1.0), scales=lambda t: (w_rib(t), RIB_T))

    # ---- 5 painted skin + glow
    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    _paint(m, atlas)
    ref = w.reference("8472e8739b17849ba9ecf592ce0821e0.jpg", (470, 150, 680, 970), "staff_of_moonlight")
    w.finish("staff_of_moonlight", m, atlas, real_m=1.85, glow=("FFFFFF", 0.10, 0.55, 1.0), ref=ref)


def w_rib(t):
    return (RIB_W0 + (RIB_W1 - RIB_W0) * t) * (1.0 - 0.8 * max(0.0, t - 0.88) / 0.12)


def _paint(m, atlas):
    # the moon: pale with soft maria and a pearl glow
    def moon(c):
        n1 = w.noise2(c.img.shape[:2], 9, 11)
        n2 = w.noise2(c.img.shape[:2], 3.5, 12)
        mare = np.clip((n1 - 0.52) * 3.2, 0, 1) * 0.55 + np.clip((n2 - 0.7) * 3.0, 0, 1) * 0.25
        base = np.array([236, 240, 252], np.float32)
        shade = np.array([168, 180, 214], np.float32)
        c.img[..., :3] = base * (1 - mare[..., None]) + shade * mare[..., None]
        c.img[..., 3] = 255
        w.set_glow(c, (170, 196, 245), 0.22, core=0.15)
    w.paint_part(atlas, m.part("moon_orb"), moon)

    # the cloth: star speckles (a few glow faintly) on the wrap and the ribbons
    def stars(c):
        rng = np.random.default_rng(zlib_seed(c.part.name) + c.g.rect[0])
        h, wd = c.img.shape[:2]
        n = max(4, int(h * wd / 90))
        for _ in range(n):
            y, x = int(rng.integers(0, h)), int(rng.integers(0, wd))
            c.img[y, x, :3] = (232, 240, 255)
            if rng.random() < 0.35:
                c.glow[y, x] = (200, 220, 255, 190)
        if c.part.name.startswith("ribbon"):                               # the ribbons run from deep blue to a paler tip
            k = np.clip(c.T, 0, 1)[..., None] * 0.35
            c.img[..., :3] = c.img[..., :3] * (1 - k) + np.array([120, 140, 205], np.float32) * k
    for nm in ("grip_wrap", "ribbon_a", "ribbon_b"):
        w.paint_part(atlas, m.part(nm), stars)

    # the crescent: engraved dark rune dashes along the band's face, glowing pale
    cres = m.part("crescent")
    wd = lambda th: CRES_W_MAX * max(0.0, 1.0 - (abs(th) / CRES_HALF) ** 2)

    for i in range(36):
        th = 124 - 248 * i / 35
        if wd(th) < 4:
            continue
        a = math.radians(th)
        r = CRES_R_OUT - wd(th) / 2
        tang = np.array([-math.sin(a), math.cos(a)])
        rad = np.array([math.cos(a), math.sin(a)])
        c0 = np.array([r * math.cos(a), CRES_CY + r * math.sin(a)])
        p0 = c0 - tang * 0.9 - rad * wd(th) * 0.18
        p1 = c0 + tang * 0.9 + rad * wd(th) * 0.18
        atlas.line(cres, (p0[0], p0[1], CRES_T / 2), (p1[0], p1[1], CRES_T / 2), 0.28, "z", 1, dark=0.55, glow_rgb=(170, 200, 255), glow_strength=0.9)
    # the cradle and the neck joints take a faint blue glow on their lozenge faces
    for nm in ("joint_swell_a", "joint_swell_b", "cradle"):
        def faint(c):
            w.set_glow(c, (150, 180, 240), 0.22, core=0.1)
        w.paint_part(atlas, m.part(nm), faint)
    # the ring round the moon is blued steel
    w.paint_part(atlas, m.part("moon_ring"), lambda c: w.tint(c.img, (110, 124, 170), 0.55))


def zlib_seed(name):
    import zlib
    return zlib.crc32(name.encode()) % 9973
