"""Wave 11 models, blades 1 to 5: Wavebreaker, Encircled Blade, Windcutter, Tripolar Sword, The Dagger of Many Daggers.

Every function returns (mesh, atlas, kwargs for wave1_models.finish). Axis for all: y up from the pommel to the point, x across, z through the blade
(+z toward the viewer in hand). Reference crops come from the cards (scratchpad/relics2); the measured card pixels are in the constants.
"""
import math

import numpy as np

import mesh_lib as ml
import wave1_models as w
from wave11_models import spike, torus, wrap_rings, curve, glow_part, tint_part, scale_pattern

CARD3 = "p3/Relics of the world part 3/"
CARD2 = "p2/Relics of the world part 2/"


# ====================================================================================== 1 Wavebreaker
def wavebreaker():
    # ---- 1 measure: card 640 x 496. The nodachi runs from its tassel (628, 4) down to the point (385, 470): about 530 px for a 150 cm sword.
    K = 150.0 / 530.0                                   # cm per card px
    GRIP_L = 38.0                                       # tsuka: 134 px of the card (the hilt is a long two-hand grip)
    TSUBA_Y = GRIP_L                                    # the guard sits at the end of the grip
    BLADE_Y0, BLADE_L = TSUBA_Y + 2.2, 112.0            # 397 px of blade past the habaki
    W0, W1 = 3.7, 2.7                                   # blade width at the habaki and under the kissaki (slender: about 4 cm)
    T0, T1 = 0.95, 0.6                                  # thickness at the back, and at the kissaki
    SORI = 4.2                                          # how far the point is drawn off the straight line (the card shows a shallow curve)
    KISSAKI = 0.88                                      # the point begins at 88 percent of the blade
    bx = lambda y: SORI * ((y - BLADE_Y0) / BLADE_L) ** 2

    m = ml.Mesh("wavebreaker")
    sec = [(0.5, 0.0), (0.16, 0.5), (-0.36, 0.5), (-0.5, 0.2), (-0.5, -0.2), (-0.36, -0.5), (0.16, -0.5)]     # edge on +x, flat back, shinogi line

    def wid(t):
        return (W0 + (W1 - W0) * t / KISSAKI) if t < KISSAKI else W1 * (1 - ((t - KISSAKI) / (1 - KISSAKI)) ** 1.6)

    def thk(t):
        return (T0 + (T1 - T0) * t / KISSAKI) if t < KISSAKI else T1 * (1 - ((t - KISSAKI) / (1 - KISSAKI)) ** 1.3)

    # ---- 2 the blade: a swept section along the curve
    bl = w.add(m, "blade", "steel", base=(70, 134, 158), smooth=40)
    path = curve(bx, BLADE_Y0, BLADE_Y0 + BLADE_L, 28)
    ml.sweep(bl, path, sec, scales=lambda t: (wid(t), thk(t)))
    # ---- 3 the small raised parts: a raised ridge each side of the shinogi, on both faces
    for nm, sgn in (("ridge_front", 1), ("ridge_back", -1)):
        rd = w.add(m, nm, "steel", base=(150, 206, 222), wear=0.8)
        pts = [(x - 0.55 * wid(i / 22.0), y, sgn * thk(i / 22.0) * 0.5 * 0.95) for i, (x, y, _z) in enumerate(curve(bx, BLADE_Y0 + 2, BLADE_Y0 + BLADE_L * 0.86, 22))]
        ml.sweep(rd, pts, ml.lozenge(1.0, 1.0), scales=lambda t: (0.28, 0.2 * (1 - 0.7 * t)))
    hb = w.add(m, "habaki", "gold", base=(196, 170, 96))
    ml.collar(hb, TSUBA_Y + 0.2, 2.1, 3.4, lip=0.5, sides=12, oval=0.4)
    ts = w.add(m, "tsuba", "iron", base=(52, 58, 82), smooth=50)
    ml.lathe(ts, "y", (0, TSUBA_Y - 0.5, 0), [(0.0, 0.0), (0.05, 5.0), (0.45, 5.35), (0.9, 5.0), (1.0, 0.0)], sides=28)
    sp = w.add(m, "seppa", "gold", base=(190, 168, 100))
    for dy in (-1.0, 0.9):
        ml.collar(sp, TSUBA_Y + dy, 2.5, 0.35, lip=0.1, sides=14, oval=0.7)
    # ---- 4 the long grip and its binding
    gp = w.add(m, "grip", "leather", base=(34, 32, 56), uv_mode="loft")
    ml.loft(gp, "y", (0, 0, 0), [(0.0, 0.92, 0.92), (GRIP_L * 0.45, 1.05, 1.0), (GRIP_L - 0.4, 1.0, 1.0)], ml.octagon(3.0, 2.4, 0.7))
    gw = w.add(m, "grip_binding", "cloth", base=(52, 66, 120), wear=0.3)
    wrap_rings(gw, 2.0, GRIP_L - 3.0, 1.62, 11, h=1.3, sides=12, oval=0.82)
    for yy in (GRIP_L * 0.3, GRIP_L * 0.7):                                       # the menuki under the binding
        mk = w.add(m, "menuki_%d" % int(yy), "gold", base=(206, 180, 100))
        for sg in (1, -1):
            ml.stud(mk, (0, yy, sg * 1.28), 0.5, 0.3, (0, 0, sg), sides=6)
    # ---- 5 the pommel cap and the blue cord with its tassel
    kb = w.add(m, "kashira", "iron", base=(50, 56, 78), smooth=55)
    ml.lathe(kb, "y", (0, 0, 0), [(-1.1, 0.0), (-0.8, 1.55), (0.5, 1.85), (1.8, 1.6)], sides=14, squash=0.85)
    ts_ = w.add(m, "tassel", "cloth", base=(70, 130, 200), wear=0.2)
    for sgn, drop in ((1, 15.0), (-1, 11.0)):
        pts = [(0.0, -0.4, sgn * 1.2), (sgn * 0.8, -3.5, sgn * 1.7), (sgn * 1.9, -7.5, sgn * 1.9), (sgn * 1.4, -drop, sgn * 1.6)]
        ml.sweep(ts_, pts, ml.circle(0.28, 6), scales=lambda t: (1.0 - 0.25 * t, 1.0 - 0.25 * t))
        spike(ts_, (sgn * 1.4, -drop, sgn * 1.6), (0.1, -1, 0), 3.6, 0.55, sides=6, belly=0.5)
    tg = w.add(m, "cord_loops", "cloth", base=(80, 150, 220), wear=0.2)                       # the blue cord looped round the grip under the guard
    for dy in (-5.2, -4.0):
        ml.collar(tg, TSUBA_Y + dy, 1.68, 0.5, lip=0.15, sides=12, oval=0.83)

    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)

    def scaled(c):                                                                # fish scales, purple tint along the lower (spine) edge, a cold sheen at the edge
        scale_pattern(c, 0.85, 0.78, 1.14)
        edge = np.clip((c.A - bx(c.B) - 0.7 * wid(0.0)) * 0.0 + (c.S - 0.8) * 5.0, 0, 1) if c.kind == "loft" else np.zeros(c.img.shape[:2])
        c.img[..., :3] *= (1 + 0.15 * edge)[..., None]
        low = np.clip(1.0 - (c.B - BLADE_Y0) / (BLADE_L * 0.5), 0, 1)
        c.img[..., 0] = np.clip(c.img[..., 0] + 26 * low, 0, 255)
        c.img[..., 2] = np.clip(c.img[..., 2] + 14 * low, 0, 255)
        w.set_glow(c, (110, 220, 255), 0.22, mask=np.clip(np.sin(c.B * 0.9 + c.A * 0.4) * 0.5 + 0.35, 0, 1) * 0.5, core=0.4)
    w.paint_part(atlas, bl, scaled)
    glow_part(atlas, ts_, (90, 170, 255), 0.18)
    ref = w.reference(CARD3 + "bb15f8443f4826df199a8d780a1a285d.jpg", (330, 0, 640, 496), "wavebreaker")
    return m, atlas, dict(real_m=1.45, glow=("FFFFFF", 0.14, 0.45, 1.0), ref=ref)


# ====================================================================================== 2 Encircled Blade
def encircled_blade():
    # 1.34.0: this build has its POINT at y = 0 and its hilt on top (the hand held the blade, owner screenshot 68): tools/relics/weapon_fixes.json has `flip_y` for this id,
    # which write_txt applies after writing (apply_weapon_fixes.op_flip_y). Do not also flip it here (it would be turned twice).
    # ---- 1 measure: card 735 x 826, the sword vertical at x 175 from the pommel (y 20) to the point (y 805): 785 px for a 66 cm short sword.
    K = 66.0 / 785.0
    cm = lambda px: px * K
    L_POMMEL = 0.0
    Y_GRIP0, Y_GRIP1 = cm(785 - 215), cm(785 - 40)           # the blue wrapped grip: card y 40 to 215 (the point is y 805 = 0 at the tip; here y runs up from the pommel)
    # y from the point: tip at 0; we build upward from the point and flip: y_model = 66 - (card_y - 20) * K  (pommel at the top in the card, at the bottom here)
    Y = lambda cy: 66.0 - (cy - 20) * K
    m = ml.Mesh("encircled_blade")
    blade_top, blade_tip = Y(345), Y(805)
    W_BLADE = cm(78)                                          # the leaf blade is about 78 px wide below the guard
    bl = w.add(m, "blade", "steel", base=(60, 196, 200), smooth=40)
    prof = [(blade_tip, 0.0, 0.0), (blade_tip + 2.0, 0.28, 0.5), (blade_tip + 9.0, 0.55, 0.85), (blade_tip + 20, 0.82, 1.0), (blade_tip + 30, 1.0, 1.0), (blade_top + 0.6, 0.92, 1.0)]
    ml.loft(bl, "y", (0, 0, 0), [(s, W_BLADE * a, 1.0 * b) for (s, a, b) in prof], ml.lozenge(1.0, 1.0, 0.4), caps=(False, True))
    # the midrib: two raised ridges the whole length of the blade, on both faces, with the five dark oval holes between them
    for nm, sg in (("rib_front", 1), ("rib_back", -1)):
        rb = w.add(m, nm, "steel", base=(150, 240, 236), wear=0.8)
        ml.fuller(rb, (0, blade_tip + 4.0, sg * 0.5), (0, blade_top - 1.0, sg * 0.5), (0, 0, sg), 1.5, 0.3, 0.16, taper=2.0)
    holes = w.add(m, "holes", "obsidian", base=(10, 26, 40), smooth=60)
    for cy in (410, 485, 560, 640, 720):                      # the card's dark oval holes along the fuller
        yy = Y(cy)
        for sg in (1, -1):
            ml.lathe(holes, "z", (0, yy, sg * 0.5), [(0.0, 0.0), (sg * 0.18, 0.85), (sg * 0.3, 0.7), (sg * 0.35, 0.0)], sides=10, squash=1.5)
    gm = w.add(m, "gem", "crystal", base=(40, 200, 140), smooth=60)
    ml.gem(gm, (0, Y(375), 0.7), 0.85, long=1.4, sides=6, axis="y")          # the green gem under the guard (card (172, 375))
    # ---- 3 the black orb in the guard and its rim, the blue collar
    ob = w.add(m, "orb", "obsidian", base=(12, 12, 24), smooth=70)
    ml.lathe(ob, "y", (0, Y(305), 0), [(-3.4, 0.0), (-2.7, 2.2), (-1.4, 3.1), (0.0, 3.4), (1.4, 3.1), (2.7, 2.2), (3.4, 0.0)], sides=18)
    rim = w.add(m, "orb_rim", "steel", base=(200, 220, 236), smooth=45)
    torus(rim, (0, Y(305), 0.0), 3.55, 0.35, plane="y", n=18, sides=6)
    torus(rim, (0, Y(305), 0.0), 3.55, 0.35, plane="z", n=18, sides=6)
    co = w.add(m, "collar", "cloth", base=(30, 70, 200), smooth=40)
    ml.loft(co, "y", (0, 0, 0), [(Y(265), 0.8, 0.8), (Y(265) + 0.4, 1.0, 1.0), (Y(215) - 0.3, 1.0, 1.0), (Y(215), 0.9, 0.9)], ml.octagon(cm(80), cm(60), 0.7))
    # ---- 4 the grip with its binding rings and the pommel
    gp = w.add(m, "grip", "leather", base=(30, 66, 190), uv_mode="loft")
    ml.loft(gp, "y", (0, 0, 0), [(Y(215), 0.9, 0.9), (Y(130), 1.0, 1.0), (Y(40), 0.92, 0.92)], ml.circle(cm(22), 12))
    gb = w.add(m, "grip_bands", "iron", base=(20, 28, 60), wear=0.3)
    for cy in (75, 100, 125, 150, 175, 200):
        ml.collar(gb, Y(cy), cm(24), 0.5, lip=0.15, sides=12)
    pm = w.add(m, "pommel", "cloth", base=(26, 56, 180), smooth=60)
    ml.lathe(pm, "y", (0, 0, 0), [(Y(20) - 0.2, 0.0), (Y(20), cm(28)), (Y(32), cm(36)), (Y(48), cm(22))], sides=14, squash=0.7)

    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    glow_part(atlas, gm, (60, 255, 170), 0.9)
    glow_part(atlas, rim, (120, 255, 240), 0.35)
    w.paint_part(atlas, bl, lambda c: w.set_glow(c, (60, 255, 235), 0.22, mask=np.clip((np.abs(c.S - 0.5) - 0.34) * 6, 0, 1), core=0.4))
    ref = w.reference(CARD2 + "14c193e14db05cf0df09e2447ec0220d.jpg", (110, 0, 260, 826), "encircled_blade")
    return m, atlas, dict(real_m=0.78, glow=("FFFFFF", 0.14, 0.45, 1.0), ref=ref)


# ====================================================================================== 3 Windcutter
def windcutter():
    # 1.34.0: this build has its POINT at y = 0 and its hilt on top (the hand held the blade, owner screenshot 68): tools/relics/weapon_fixes.json has `flip_y` for this id,
    # which write_txt applies after writing (apply_weapon_fixes.op_flip_y). Do not also flip it here (it would be turned twice).
    # ---- 1 measure: card 736 x 1019, the sword vertical at x 575; pommel gem (160) to the point (750): 590 px for a 112 cm longsword.
    K = 112.0 / 590.0
    Y = lambda cy: (750 - cy) * K                       # card y -> model y (point at 0)
    # y runs up from the point here; the pommel is at the top as in the card
    BLADE_TOP = Y(322)                                  # the blade leaves the guard at card y 322
    W_BL = 31 * K                                       # 31 px wide blade
    m = ml.Mesh("windcutter")
    bl = w.add(m, "blade", "silver", base=(206, 212, 232), smooth=40)
    st = [(0.0, 0.0, 0.0), (1.8, 0.45, 0.6), (9.0, 0.78, 0.9), (30.0, 0.9, 1.0), (BLADE_TOP * 0.75, 0.98, 1.0), (BLADE_TOP + 0.5, 1.0, 1.0)]
    ml.loft(bl, "y", (0, 0, 0), [(s, W_BL * a, 0.9 * b) for (s, a, b) in st], ml.lozenge(1.0, 1.0, 0.0), caps=(False, True))
    rg = w.add(m, "ridge", "silver", base=(232, 236, 250), wear=0.8)
    for sg in (1, -1):
        ml.fuller(rg, (0, 4.0, sg * 0.4), (0, BLADE_TOP - 2.0, sg * 0.4), (0, 0, sg), 0.9, 0.2, 0.1, taper=3.0)
    # ---- 2 the winged guard: three feathers fanned each side upward and two downward, a swelling core, the teal gem
    wings = w.add(m, "wing_feathers", "silver", base=(214, 220, 238), smooth=45)
    GY = Y(300)                                         # the guard centre (card y 300)
    for sx in (1, -1):
        for ang, ln, wd in ((66, 13.0, 3.3), (44, 16.0, 3.7), (20, 18.0, 3.6), (-8, 15.5, 3.2), (-34, 11.5, 2.8)):
            a = math.radians(ang)
            d = np.array([sx * math.cos(a), math.sin(a), 0.0])
            ml.loft(wings, d, (sx * 1.3, GY, 0.0), [(0.0, wd * 0.45, 0.5), (ln * 0.25, wd, 0.45), (ln * 0.7, wd * 0.8, 0.3), (ln, 0.0, 0.0)],
                    ml.lozenge(1.0, 0.5, 0.3), up=(0, 0, 1))
    teal = w.add(m, "wing_inlays", "crystal", base=(50, 190, 180), smooth=60)               # the teal feather veins
    for sx in (1, -1):
        for ang, ln in ((44, 17.0), (20, 18.5), (66, 13.5)):
            a = math.radians(ang)
            p0 = (sx * (2.6 * math.cos(a) + 1.3), GY + 2.6 * math.sin(a), 0.32)
            p1 = (sx * (ln * 0.8 * math.cos(a) + 1.3), GY + ln * 0.8 * math.sin(a), 0.32)
            ml.bar(teal, p0, p1, 0.45, 0.18, up=(0, 0, 1), taper=1.0)
    core = w.add(m, "guard_core", "silver", base=(200, 206, 226), smooth=55)
    ml.lathe(core, "y", (0, GY, 0), [(-4.2, 0.0), (-2.6, 1.7), (0.0, 2.3), (2.6, 1.7), (4.2, 0.0)], sides=10, squash=0.6)
    gem = w.add(m, "gem", "crystal", base=(60, 200, 196), smooth=70)
    ml.gem(gem, (0, GY, 1.0), 1.25, long=1.6, sides=6, axis="y")
    # ---- 3 grip, its bands, the pommel with the gem
    gp = w.add(m, "grip", "silver", base=(170, 176, 200), uv_mode="loft")
    ml.loft(gp, "y", (0, 0, 0), [(Y(285), 1.0, 1.0), (Y(230), 1.05, 1.05), (Y(185), 1.0, 1.0)], ml.circle(1.15, 12))
    gb = w.add(m, "grip_bands", "silver", base=(222, 226, 244), wear=0.3)
    for cy in (275, 255, 235, 215, 195):
        ml.collar(gb, Y(cy), 1.27, 0.5, lip=0.15, sides=12)
    pm = w.add(m, "pommel", "silver", base=(196, 202, 224), smooth=60)
    ml.lathe(pm, "y", (0, 0, 0), [(Y(185) - 0.2, 1.0), (Y(178), 1.6), (Y(172), 1.2), (Y(166), 1.7), (Y(160), 0.9), (Y(156), 0.0)], sides=12)
    pg = w.add(m, "pommel_gem", "crystal", base=(60, 210, 200), smooth=70)
    ml.gem(pg, (0, Y(163), 0), 0.9, long=1.5, sides=6, axis="y")
    # ---- 4 the grey-lavender ribbon winding round the blade: a flat strip on a helix with two loose tails
    rb = w.add(m, "ribbon", "cloth", base=(172, 170, 208), wear=0.2, uv_mode="loft")
    pts = []
    for i in range(0, 41):
        t = i / 40
        ang = 2 * math.pi * 1.4 * t + 0.4
        pts.append((math.cos(ang) * (2.4 + 3.0 * t), BLADE_TOP - 4 - (BLADE_TOP - 22) * t, math.sin(ang) * (2.4 + 3.0 * t)))
    ml.sweep(rb, pts, ml.rect_ring(1.0, 1.0), scales=lambda t: (2.3 * (1 - 0.55 * t), 0.1))

    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    glow_part(atlas, gem, (80, 255, 240), 1.0)
    glow_part(atlas, pg, (80, 255, 240), 0.8)
    glow_part(atlas, teal, (60, 230, 210), 0.45)
    w.paint_part(atlas, bl, lambda c: w.set_glow(c, (170, 255, 250), 0.18, mask=np.clip((np.abs(c.S - 0.5) - 0.36) * 6, 0, 1), core=0.5))
    ref = w.reference(CARD2 + "37f90252f93e4647b8977e4989545751.jpg", (460, 140, 700, 770), "windcutter")
    return m, atlas, dict(real_m=1.2, glow=("FFFFFF", 0.12, 0.4, 1.0), ref=ref)


# ====================================================================================== 4 Tripolar Sword
def tripolar_sword():
    # 1.34.0: this build has its POINT at y = 0 and its hilt on top (the hand held the blade, owner screenshot 68): tools/relics/weapon_fixes.json has `flip_y` for this id,
    # which write_txt applies after writing (apply_weapon_fixes.op_flip_y). Do not also flip it here (it would be turned twice).
    # ---- 1 measure: card 640 wide, art 600 high; the sword runs from the point (45, 590) to the pommel spike (580, 50): 760 px for a 110 cm sword.
    K = 110.0 / 760.0
    L_TOTAL = 110.0
    GUARD_S = 545 * K                                   # the star guard sits 545 px from the point
    m = ml.Mesh("tripolar_sword")
    # the blade, widest by the guard, with a faceted ridge down the middle
    bl = w.add(m, "blade", "iron", base=(120, 122, 138), smooth=40)
    prof = [(0.0, 0.0), (3.0, 0.35), (18.0, 0.62), (45.0, 0.84), (GUARD_S * 0.8, 0.95), (GUARD_S, 1.0)]
    W0 = 9.0
    ml.loft(bl, "y", (0, 0, 0), [(s, W0 * a, 1.0 * (0.6 + 0.4 * a)) for (s, a) in prof], ml.lozenge(1.0, 1.0, 0.18), caps=(False, True))
    fl = w.add(m, "flame_edges", "steel", base=(160, 164, 182), smooth=25)             # the jagged flame licks along both edges
    for side in (1, -1):
        for i, s in enumerate(np.linspace(16.0, GUARD_S - 10.0, 6)):
            frac = s / GUARD_S
            hw = W0 * (0.35 + 0.65 * min(1.0, frac * 1.15)) * 0.5
            ln = 3.0 + 3.0 * frac
            d = np.array([side * 0.55, 0.85, 0.0])
            ml.loft(fl, d, (side * hw * 0.8, s, 0.0), [(0.0, 1.0, 0.45), (ln * 0.4, 1.1, 0.4), (ln, 0.0, 0.0)], ml.lozenge(1.0, 0.45, 0.2), up=(0, 0, 1))
    rb = w.add(m, "ridge_lines", "steel", base=(210, 214, 230), wear=0.9)
    for sg in (1, -1):
        ml.fuller(rb, (0.0, 8.0, sg * 0.35), (0.0, GUARD_S - 3.0, sg * 0.35), (0, 0, sg), 1.6, 0.3, 0.16, taper=4.0)
    # ---- 3 the four-pointed star guard: two long arms across, two short along the blade, a faceted boss, a rim
    GY = GUARD_S + 2.2
    ga = w.add(m, "guard_arms", "iron", base=(96, 98, 114), smooth=40)
    ml.loft(ga, "x", (0, GY, 0), [(-17.0, 0.0, 0.0), (-13.0, 2.4, 1.3), (-6.0, 6.2, 2.0), (0.0, 8.5, 2.4), (6.0, 6.2, 2.0), (13.0, 2.4, 1.3), (17.0, 0.0, 0.0)], ml.lozenge(1.0, 1.0, 0.2))
    ml.loft(ga, "y", (0, GY, 0), [(-11.0, 0.0, 0.0), (-6.0, 3.6, 1.5), (0.0, 6.0, 2.2), (6.0, 3.6, 1.5), (11.0, 0.0, 0.0)], ml.lozenge(1.0, 1.0, 0.2))
    gb = w.add(m, "guard_boss", "steel", base=(176, 180, 198), smooth=50)
    ml.lathe(gb, "z", (0, GY, 0), [(-1.4, 0.0), (-1.0, 2.0), (0.0, 2.6), (1.0, 2.0), (1.4, 0.0)], sides=8)
    gr = w.add(m, "guard_inlay", "silver", base=(210, 214, 232), wear=0.8)
    for sx in (1, -1):
        ml.bar(gr, (sx * 1.8, GY, 1.2), (sx * 12.0, GY, 0.8), 0.45, 0.14, up=(0, 0, 1), taper=1.5)
        ml.bar(gr, (0, GY + 1.8 * sx, 1.2), (0, GY + 6.0 * sx, 0.9), 0.4, 0.14, up=(0, 0, 1), taper=1.0)
    # ---- 4 grip, wrap, angular pommel spike
    G0 = GY + 2.8
    gp = w.add(m, "grip", "leather", base=(36, 36, 52), uv_mode="loft")
    ml.loft(gp, "y", (0, 0, 0), [(G0, 1.0, 1.0), (G0 + 7.0, 1.12, 1.12), (G0 + 14.0, 1.0, 1.0)], ml.octagon(2.4, 2.4, 0.6))
    gw = w.add(m, "grip_rings", "iron", base=(120, 124, 142), wear=0.4)
    for i in range(5):
        ml.collar(gw, G0 + 1.0 + i * 2.8, 1.5, 0.7, lip=0.2, sides=8)
    pm = w.add(m, "pommel", "steel", base=(150, 154, 172), smooth=30)
    ml.lathe(pm, "y", (0, G0 + 14.0, 0), [(-0.2, 1.1), (1.0, 2.0), (3.0, 1.9), (7.0, 1.0), (11.0, 0.0)], sides=4)       # a four-sided spiked pommel
    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)

    def marble(c):                                                                      # grey-black marble with pale veins
        n = w.noise2(c.img.shape[:2], 7.0, int(c.B.mean() * 13) % 977)
        n2 = w.noise2(c.img.shape[:2], 3.0, 5)
        vein = np.clip(1.0 - np.abs(n - 0.5) * 9.0, 0, 1)
        c.img[..., :3] = np.clip(c.img[..., :3] * (0.8 + 0.3 * n2[..., None]) + 70 * vein[..., None], 0, 255)
        c.img[..., 2] = np.clip(c.img[..., 2] + 14, 0, 255)
        w.set_glow(c, (150, 170, 255), 0.16, mask=vein * 0.7, core=0.4)
    w.paint_part(atlas, bl, marble)
    glow_part(atlas, gr, (150, 170, 255), 0.4)
    ref = w.reference(CARD3 + "982ecd505ab99658806750f2dbf3bb0e.jpg", (20, 20, 620, 620), "tripolar_sword")
    return m, atlas, dict(real_m=1.15, glow=("FFFFFF", 0.1, 0.4, 1.0), ref=ref)


# ====================================================================================== 5 The Dagger of Many Daggers
def the_dagger_of_many_daggers():
    # ---- 1 measure: card 600 x 1056, the main dagger vertical at x 297: point (128) to the pommel ring bottom (600): 472 px for a 40 cm dagger.
    K = 40.0 / 472.0
    Y = lambda cy: (600 - cy) * K                       # card y -> model y (pommel ring bottom at 0)
    m = ml.Mesh("the_dagger_of_many_daggers")
    top = Y(128)
    bl = w.add(m, "blade", "silver", base=(212, 218, 232), smooth=40)
    W = 42 * K                                          # the leaf blade is 42 px at its widest (card y 250)
    st = [(top, 0.0, 0.0), (top - 1.6, 0.22, 0.5), (Y(180), 0.7, 0.9), (Y(250), 1.0, 1.0), (Y(320), 0.8, 1.0), (Y(380), 0.5, 1.0), (Y(390), 0.38, 1.0)]
    st.sort()
    ml.loft(bl, "y", (0, 0, 0), [(s, W * a, 0.62 * b) for (s, a, b) in st], ml.lozenge(1.0, 1.0, 0.0), caps=(True, True))
    rb = w.add(m, "midrib", "silver", base=(240, 244, 252), wear=0.8)
    for sg in (1, -1):
        ml.fuller(rb, (0, Y(380), sg * 0.28), (0, Y(190), sg * 0.28), (0, 0, sg), 0.7, 0.14, 0.08, taper=1.5)
    # ---- 3 the gold ring guard round the blade's root, the collar below it with its studs
    rg = w.add(m, "ring_guard", "gold", base=(226, 172, 52), smooth=40)
    torus(rg, (0, Y(383), 0), 55 * K * 0.5, 0.42, plane="z", n=20, sides=6)
    cl = w.add(m, "collar", "gold", base=(214, 164, 50), smooth=45)
    ml.loft(cl, "y", (0, 0, 0), [(Y(440), 0.9, 0.9), (Y(425), 1.0, 1.0), (Y(398), 1.1, 1.1), (Y(386), 0.9, 0.9)], ml.octagon(2.4, 1.8, 0.5))
    sd = w.add(m, "studs", "silver", base=(240, 232, 200), wear=0.2)
    for cy in (410, 428):
        for sg in (1, -1):
            ml.stud(sd, (0, Y(cy), sg * 0.92), 0.28, 0.22, (0, 0, sg), sides=6)
    # ---- 4 the blue wrapped grip: a round loft with diagonal wrap bands, the ring pommel
    gp = w.add(m, "grip", "cloth", base=(40, 78, 170), uv_mode="loft")
    ml.loft(gp, "y", (0, 0, 0), [(Y(575), 0.92, 0.92), (Y(500), 1.04, 1.04), (Y(442), 0.95, 0.95)], ml.circle(1.05, 12))
    gb = w.add(m, "grip_wrap", "iron", base=(20, 36, 90), wear=0.3)
    for cy in (456, 474, 492, 510, 528, 546, 562):
        ml.collar(gb, Y(cy), 1.1, 0.45, lip=0.14, sides=12)
    pr = w.add(m, "pommel_ring", "gold", base=(226, 172, 52), smooth=40)
    torus(pr, (0, Y(590) + 0.4, 0), 1.7, 0.4, plane="z", n=16, sides=6)
    pb = w.add(m, "pommel_bead", "gold", base=(236, 190, 70), smooth=55)
    ml.lathe(pb, "y", (0, 0, 0), [(Y(578), 0.9), (Y(590), 1.2), (Y(600), 0.7), (Y(604), 0.0)], sides=10)

    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    glow_part(atlas, rg, (255, 210, 90), 0.5)
    glow_part(atlas, pr, (255, 210, 90), 0.4)
    w.paint_part(atlas, bl, lambda c: w.set_glow(c, (170, 190, 255), 0.18, mask=np.clip((np.abs(c.S - 0.5) - 0.38) * 6, 0, 1), core=0.4))
    ref = w.reference(CARD3 + "bbc420f36d9e35428a2d6fb3eb1daf49.jpg", (180, 100, 420, 620), "dagger_many")
    return m, atlas, dict(real_m=0.5, glow=("FFFFFF", 0.2, 0.45, 1.0), ref=ref)
