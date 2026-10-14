"""Wave 12 models, first five: Giant Jawblade, The Lumberjack, White Whip of Welting, Whirling Blades, Warmage's Longsword (the owner's model method).

Conventions as in mesh_lib: centimetres, y along the weapon (grip below, head above), x across (+x is the edge side the sprite shows lower right), z
through (+z toward the viewer). Each builder: measure the card -> named constants, then parts, then the skin and the glow, then wave12_models.finish().
"""
import math

import numpy as np

import mesh_lib as ml
import wave1_models as w
import wave12_models as W


def build(item_id):
    BUILDERS[item_id]()


# ================================================================ Warmage's Longsword
def warmages_longsword():
    # ---- 1 measure: the card is 736 x 736; the sword runs from the pommel gem (70, 262) to the point (672, 268): s = px along that line
    TOTAL_M = 1.12                                       # a longsword with its hilt
    S_TIP = 602.0                                        # card px from the pommel end to the point
    K = w.px_to_cm(TOTAL_M * 100.0, S_TIP)               # cm per card px (0.186)
    y = lambda s: s * K
    S_POMMEL, S_CAP = (0.0, 30.0), (22.0, 40.0)          # the violet pommel gem, the gold cap it sits in
    S_GRIP = (40.0, 132.0)                               # the blue-wrapped grip (card x 110 to 202)
    R_GRIP = 9.0 * K                                     # grip radius: 18 px tall on the card
    S_GUARD, GUARD_HALF = 146.0, 68.0 * K                # the crossguard: centre at card x 216, 136 px tall (half 68)
    GUARD_T, GUARD_W = 30.0 * K, 14.0 * K                # guard thickness along the blade and through it
    S_BLADE0 = 162.0                                     # where the blade starts (the ricasso collar ends)
    BLADE_W0, BLADE_W1 = 28.0 * K, 12.0 * K              # blade width at the guard and near the point (28 px, 12 px)
    BLADE_T0, BLADE_T1 = 0.9, 0.55                       # blade thickness, cm
    GEMS = [272, 322, 372, 422, 470]                     # centres of the five inlaid violet diamonds (card x minus 70)
    GEM_L, GEM_R = 15.0 * K, 10.0 * K                    # a diamond: 30 px long, 20 px tall
    GEM_S = [g - 70 + 4 for g in GEMS]
    m = ml.Mesh("warmages_longsword")

    pg = w.add(m, "pommel_gem", "crystal", base=(150, 90, 230), smooth=60)
    ml.gem(pg, (0, y(15), 0), 1.9, long=1.25, sides=6)
    pc = w.add(m, "pommel_cap", "gold", smooth=50)
    ml.lathe(pc, "y", (0, 0, 0), [(y(S_CAP[0]), 1.2), (y(S_CAP[0]) + 0.5, 2.05), (y(S_CAP[1]) - 1.2, 1.9), (y(S_CAP[1]), R_GRIP * 1.15)], sides=12)
    gp = w.add(m, "grip", "leather", base=(44, 74, 150), uv_mode="loft", smooth=50)
    ml.loft(gp, "y", (0, 0, 0), [(y(S_GRIP[0]), 1.0, 1.0), (y(80), 1.12, 1.12), (y(S_GRIP[1]), 1.0, 1.0)], ml.circle(R_GRIP, 12))
    gw = w.add(m, "grip_wrap", "silver", base=(206, 210, 224), wear=0.2)
    W.wrap_rings(gw, y(S_GRIP[0]) + 0.3, y(S_GRIP[1]) - 0.3, R_GRIP * 1.1, 9, h=0.5, sides=12)
    gd = w.add(m, "guard", "gold", smooth=45)
    ml.loft(gd, "x", (0, y(S_GUARD), 0), [(-GUARD_HALF, 0.35, 0.35), (-GUARD_HALF * 0.78, 0.6, 0.7), (-GUARD_HALF * 0.4, 0.8, 0.9), (0, 1.0, 1.0),
                                          (GUARD_HALF * 0.4, 0.8, 0.9), (GUARD_HALF * 0.78, 0.6, 0.7), (GUARD_HALF, 0.35, 0.35)], ml.octagon(GUARD_T, GUARD_W, 0.6))
    gs = w.add(m, "guard_spades", "gold", smooth=70, base=(236, 190, 80))
    for sgn in (-1, 1):                                                  # the diamond finials pointing away from the blade's line
        ml.lathe(gs, "x", (sgn * GUARD_HALF, y(S_GUARD), 0), [(0.0, 0.5), (sgn * 1.1, 1.5), (sgn * 2.6, 0.9), (sgn * 3.9, 0.0)], sides=4, squash=0.8)
    gb = w.add(m, "guard_boss", "gold", base=(230, 184, 70), smooth=60)
    w.ellipsoid(gb, (0, y(S_GUARD), 0.0), 2.3, 3.0, 1.55, sides=12, stacks=8)
    gg = w.add(m, "guard_gem", "crystal", base=(150, 90, 230), smooth=60)
    ml.lathe(gg, "z", (0, y(S_GUARD), 1.2), [(0.0, 0.0), (0.4, 1.0), (0.9, 0.6), (1.1, 0.0)], sides=6)
    rc = w.add(m, "ricasso_collar", "gold", smooth=40)
    ml.collar(rc, y(S_BLADE0) - 2.2, 2.4, 2.4, lip=0.5, sides=12, oval=0.5)
    bl = w.add(m, "blade", "steel", base=(204, 210, 224), smooth=40, uv_mode="loft")
    ml.loft(bl, "y", (0, 0, 0), [(y(S_BLADE0) - 0.4, BLADE_W0, BLADE_T0), (y(300), BLADE_W0 * 0.92, BLADE_T0 * 0.96), (y(480), (BLADE_W0 + BLADE_W1) * 0.5, BLADE_T0 * 0.85),
                                  (y(S_TIP - 70), BLADE_W1, BLADE_T1), (y(S_TIP - 22), BLADE_W1 * 0.55, BLADE_T1 * 0.7), (y(S_TIP), 0.0, 0.0)], ml.lozenge(1.0, 1.0, 0.25))
    fu = w.add(m, "fuller", "steel", base=(224, 228, 240), wear=0.8)
    for face in (1, -1):                                                 # two engraved lines per face, each two raised ridges
        for off in (-1.0, 1.0):
            ml.ridge_pair(fu, (off, y(S_BLADE0 + 4), face * BLADE_T0 * 0.5), (off * 0.6, y(S_TIP - 60), face * BLADE_T1 * 0.5), (0, 0, face), 0.34, 0.12, 0.06, 1.2)
    bg = w.add(m, "blade_gems", "crystal", base=(160, 100, 235), smooth=70)
    bz = w.add(m, "gem_bezels", "gold", base=(228, 184, 72), smooth=60)
    for s in GEM_S:
        t = BLADE_T0 * (0.96 - 0.11 * (s - 300) / 300.0)
        ml.lathe(bg, "y", (0, y(s), t * 0.5 - 0.05), [(-GEM_L, 0.0), (0.0, GEM_R), (GEM_L, 0.0)], sides=4, caps=(False, False), squash=0.28)
        W.extrude(bz, [(GEM_R * 1.25, 0), (0, GEM_L * 1.18), (-GEM_R * 1.25, 0), (0, -GEM_L * 1.18)], t * 0.5 - 0.02, t * 0.5 + 0.07, 0.04, at=(0.0, y(s)))
    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    for nm, rgbv, st in (("blade_gems", (176, 120, 255), 0.7), ("pommel_gem", (176, 120, 255), 0.6), ("guard_gem", (176, 120, 255), 0.6)):
        W.glow_part(atlas, m.part(nm), rgbv, st, core=0.08)
    ref = w.reference(W.P2 + "c2120f07d27ca2dcb3e4c7cf728438e3.jpg", (40, 180, 700, 350), "warmages_longsword")
    W.finish("warmages_longsword", m, atlas, TOTAL_M, glow=("FFFFFF", 0.18, 0.5, 1.0), ref=ref)




# ================================================================ Giant Jawblade
def giant_jawblade():
    # ---- 1 measure: the card is 736 x 920; the weapon runs from the pommel (35, 440) to the slab's far tip (705, 105): 749 px. s = card px along that line
    TOTAL_M = 1.7                                        # "immense size and weight": a greatsword of 1.7 m
    S_TIP = 749.0
    K = w.px_to_cm(TOTAL_M * 100.0, S_TIP)               # cm per card px (0.227)
    y = lambda s: s * K
    S_POMMEL, S_GRIP = (0.0, 34.0), (34.0, 168.0)        # the round pommel; the thin dark grip (card (175, 360) is its far end)
    R_GRIP = 8.0 * K                                     # grip radius: about 16 px across on the card
    S_PLATE = (150.0, 335.0)                             # the iron-plated base: from the end of the grip to the knuckle bone
    PLATE_W = [(150.0, 30.0), (200.0, 56.0), (250.0, 72.0), (300.0, 68.0), (335.0, 60.0)]   # plate width (card px) along s
    S_RUNE = 232.0                                       # the rune disc, card (232, 347)
    R_RUNE = 28.0 * K                                    # the rune disc radius: 56 px wide
    S_BONE = (318.0, S_TIP)                              # the slab of jawbone
    BONE_W = [(318.0, 46.0), (370.0, 76.0), (430.0, 98.0), (520.0, 120.0), (620.0, 128.0), (690.0, 108.0), (722.0, 54.0), (S_TIP, 0.0)]   # slab width (card px)
    BONE_T = 5.0                                         # slab thickness at the middle, cm (a jaw is thick)
    TEETH = [380.0, 430.0, 480.0, 530.0, 580.0, 630.0, 678.0]   # tooth positions along the lower edge (card (505-650, 205-250) shows seven)
    m = ml.Mesh("giant_jawblade")
    wid = w.lerp_table(BONE_W)
    pw = w.lerp_table(PLATE_W)

    pb = w.add(m, "pommel", "iron", smooth=60)
    w.ellipsoid(pb, (0, y(14), 0), 2.9, 3.4, 2.9, sides=12, stacks=8)
    gp = w.add(m, "grip", "leather", base=(52, 40, 36), uv_mode="loft", smooth=45)
    ml.loft(gp, "y", (0, 0, 0), [(y(S_GRIP[0]), 1.0, 1.0), (y(90), 1.15, 1.15), (y(S_GRIP[1]), 1.0, 1.0)], ml.circle(R_GRIP, 10))
    gb = w.add(m, "grip_binding", "iron", base=(96, 84, 70), wear=0.3)
    W.wrap_rings(gb, y(S_GRIP[0]) + 0.5, y(S_GRIP[1]) - 1.0, R_GRIP * 1.12, 7, h=0.6, sides=10)
    ml.collar(gb, y(S_GRIP[1]) - 1.2, R_GRIP * 1.5, 1.2, lip=0.3, sides=10)
    ip = w.add(m, "iron_plate", "iron", base=(66, 62, 70), smooth=40)
    ml.loft(ip, "y", (0, 0, 0), [(y(s), pw(s) * K, 3.2 - 0.5 * (s - 150) / 185.0, -0.0, 0.0) for s in (150, 175, 200, 250, 300, S_PLATE[1])], ml.octagon(1.0, 1.0, 0.18))
    pr = w.add(m, "plate_rim", "iron", base=(96, 90, 100), wear=0.9)
    for sgn in (-1, 1):                                                  # raised edge bands along both sides of the plate, both faces
        for face in (1, -1):
            ml.bar(pr, (sgn * pw(175) * K * 0.42, y(172), face * 1.45), (sgn * pw(320) * K * 0.42, y(330), face * 1.45), 0.5, 0.2, up=(0, 0, face), taper=2.0)
    rv = w.add(m, "plate_rivets", "iron", base=(140, 130, 126), smooth=70)
    for s in (185, 215, 245, 275, 305):
        for sgn in (-1, 1):
            ml.rivet(rv, (sgn * pw(s) * K * 0.36, y(s), 3.2 * 0.5 - 0.25 * (s - 150) / 185.0 + 0.02), 0.42, (0, 0, 1), 8)
    rd = w.add(m, "rune_disc", "iron", base=(40, 38, 46), smooth=55)
    ml.lathe(rd, "z", (0.6, y(S_RUNE), 1.2), [(0.0, R_RUNE), (0.55, R_RUNE), (0.8, R_RUNE * 0.92), (0.95, R_RUNE * 0.82)], sides=14, caps=(True, True))
    rm = w.add(m, "rune_marks", "silver", base=(160, 220, 245), wear=0.1)
    for a in (0, 45, 90, 135):                                           # an engraved cross-and-star: four lines through the disc's centre
        ca, sa = math.cos(math.radians(a)), math.sin(math.radians(a))
        ml.ridge_pair(rm, (0.6 - ca * R_RUNE * 0.7, y(S_RUNE) - sa * R_RUNE * 0.7, 2.2), (0.6 + ca * R_RUNE * 0.7, y(S_RUNE) + sa * R_RUNE * 0.7, 2.2), (0, 0, 1), 0.3, 0.1, 0.05, 0.6)
    kb = w.add(m, "knuckle_bone", "bone", base=(214, 190, 140), smooth=70)
    w.ellipsoid(kb, (-2.0, y(325), 0.0), 4.6, 5.2, 3.4, sides=12, stacks=8)
    bn = w.add(m, "blade", "bone", base=(226, 196, 132), smooth=45, uv_mode="loft")
    stations = []
    for s in (318, 350, 400, 450, 520, 580, 640, 690, 722, 740, S_TIP):
        wd = wid(s) * K
        sv = BONE_T * (0.7 + 0.3 * math.sin(math.pi * min(1.0, (s - 318) / 330.0))) if s < 690 else BONE_T * max(0.0, 0.8 - 0.8 * (s - 690) / 59.0)
        du = 0.0 if s < 690 else -(s - 690) / 59.0 * wd * 0.9 - (s - 690) / 59.0 * 6.0   # the far end is cut aslant: the spine side runs on past the tooth side
        stations.append((y(s), max(wd, 0.0), sv, du, 0.0))
    ml.loft(bn, "y", (0, 0, 0), stations, ml.bevelled(1.0, 1.0, 0.55))
    rg = w.add(m, "blade_ridge", "bone", base=(240, 214, 160), wear=0.6)
    for face in (1, -1):                                                 # a raised midrib on each face, tapering at both ends
        ml.bar(rg, (-1.0, y(340), face * BONE_T * 0.45), (-3.5, y(700), face * BONE_T * 0.3), 1.4, 0.45, up=(0, 0, face), taper=6.0)
    cr = w.add(m, "blade_cracks", "iron", base=(120, 92, 60), wear=0.9)
    for (x0, s0, x1, s1) in ((-4, 410, 2, 470), (4, 520, -2, 580), (-8, 560, -3, 640), (6, 600, 2, 660), (-9, 450, -5, 500)):   # the weathered cracks of old bone
        ml.ridge_pair(cr, (x0, y(s0), BONE_T * 0.42), (x1, y(s1), BONE_T * 0.38), (0, 0, 1), 0.35, 0.12, 0.06, 1.5)
    sp = w.add(m, "spine_spikes", "bone", base=(240, 220, 170), smooth=70)
    for i, s in enumerate(range(360, 700, 28)):                          # the bristled spine: a fringe of small cones on the upper edge
        edge = -wid(s) * K * 0.5
        W.spike(sp, (edge + 0.4, y(s), 0.0), (-0.7, 0.55, 0.0), 2.0 + 0.8 * ((i * 5) % 3), 0.55, sides=5)
    th = w.add(m, "teeth", "bone", base=(246, 232, 190), smooth=70)
    for s in TEETH:                                                      # the seven serrated teeth along the lower edge, leaning toward the hilt
        edge = wid(s) * K * 0.5
        W.spike(th, (edge - 0.6, y(s), 0.0), (0.82, -0.42, 0.0), 6.5 + 1.2 * math.sin(s * 0.05), 1.35, sides=6, belly=0.1)
    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    W.glow_part(atlas, m.part("rune_marks"), (110, 210, 255), 0.9)
    ref = w.reference(W.P3 + "477887ca9e1b2beccb09f4ee8f06effa.jpg", (20, 40, 730, 500), "giant_jawblade")
    W.finish("giant_jawblade", m, atlas, TOTAL_M, glow=("FFFFFF", 0.2, 0.55, 1.0), ref=ref)




# ================================================================ The Lumberjack
def the_lumberjack():
    # ---- 1 measure: the card is a 1080 x 2400 screenshot; the haft runs from its ferrule (640, 1405) to the wooden top (215, 790): 747 px = the haft
    BUT, TOP = np.array([640.0, 1405.0]), np.array([215.0, 790.0])
    HAFT_CM = 82.0                                       # a battleaxe haft
    S_HAFT = float(np.linalg.norm(TOP - BUT))
    K = HAFT_CM / S_HAFT                                 # cm per card px (0.110)
    U = (TOP - BUT) / S_HAFT                             # along the haft, butt to top
    V = np.array([-U[1], U[0]])                          # across it: the head side is negative
    V = V if np.dot(V, np.array([66.0, 898.0]) - TOP) < 0 else -V

    def local(px, py):
        d = np.array([px, py]) - TOP
        return float(np.dot(d, V)) * K, HAFT_CM + float(np.dot(d, U)) * K
    HEAD_PX = [(278, 793), (205, 800), (66, 898), (182, 1068), (214, 1050), (208, 985), (222, 930), (252, 885), (278, 868)]   # the head's outline on the card
    HEAD_T = 3.2                                         # head thickness at the poll, cm
    R_HAFT = 1.9                                         # haft radius (about 4 cm across)
    S_WRAP = 0.62                                        # the grey leather wrap covers the lower 62 percent of the haft
    S_RING = 0.60 * HAFT_CM                              # the iron ring at the wrap's top end, card (455, 1135)
    m = ml.Mesh("the_lumberjack")
    outline = [local(px, py) for (px, py) in HEAD_PX]
    # make the outline counter-clockwise for the extrusion
    area = 0.5 * sum(outline[i][0] * outline[(i + 1) % len(outline)][1] - outline[(i + 1) % len(outline)][0] * outline[i][1] for i in range(len(outline)))
    if area < 0:
        outline = outline[::-1]
    hf = w.add(m, "haft", "wood", base=(138, 92, 52), uv_mode="loft", smooth=45)
    ml.loft(hf, "y", (0, 0, 0), [(0.0, 0.9, 0.9), (HAFT_CM * 0.3, 1.0, 1.0), (HAFT_CM * 0.8, 1.06, 1.06), (HAFT_CM + 0.5, 1.1, 1.1)], ml.circle(R_HAFT, 12))
    for nm, ph in (("wrap_a", 0.0), ("wrap_b", math.pi)):                # two flat grey leather strips wound in opposite senses make the crossed wrap of the card
        wr = w.add(m, nm, "leather", base=(112, 114, 122), uv_mode="loft", smooth=60)
        pts = w.helix(lambda t: R_HAFT * 1.12, 2.0, HAFT_CM * S_WRAP, 7.0 * (1 if ph == 0 else -1), 120, phase=ph)
        ml.sweep(wr, pts, ml.rect_ring(1.0, 1.0), scales=lambda t: (3.4, 0.32))
    rb = w.add(m, "haft_ring", "iron", base=(120, 124, 132), wear=0.5)
    ml.collar(rb, S_RING, R_HAFT * 1.35, 2.2, lip=0.5, sides=12)
    fe = w.add(m, "ferrule", "iron", base=(108, 112, 120), smooth=40)
    ml.lathe(fe, "y", (0, 0, 0), [(-0.2, R_HAFT * 0.9), (0.4, R_HAFT * 1.35), (2.4, R_HAFT * 1.25), (2.9, R_HAFT * 1.5)], sides=12)
    ml.box(fe, (-R_HAFT * 1.4, -0.9, -0.5), (R_HAFT * 1.4, -0.2, 0.5), 0.2)
    hd = w.add(m, "head", "steel", base=(170, 176, 182), smooth=40, wear=0.9)
    W.extrude(hd, outline, -HEAD_T / 2, HEAD_T / 2, 0.55)
    eb = w.add(m, "edge_bevel", "steel", base=(226, 232, 238), wear=0.4, smooth=50)
    e0, e1 = local(66, 898), local(182, 1068)                            # the cutting edge: the line from the top corner to the beard
    ml.bar(eb, (e0[0] + 0.1, e0[1], 0.0), (e1[0] + 0.1, e1[1], 0.0), 0.9, 1.5, up=(0, 0, 1), taper=1.5, section="lozenge")
    ml.bar(eb, (e0[0] + 0.1, e0[1], 0.0), (e1[0] + 0.1, e1[1], 0.0), 0.5, 1.6, up=(0, 1, 0), taper=1.5, section="lozenge")
    pl = w.add(m, "poll_band", "iron", base=(130, 134, 142), smooth=45)
    a0, a1 = local(278, 793), local(278, 868)                            # the steel band that holds the wood's top end at the back of the head
    ml.box(pl, (a0[0] - 1.1, a1[1] - 0.1, -HEAD_T / 2 - 0.35), (a0[0] + 0.6, a0[1] + 0.2, HEAD_T / 2 + 0.35), 0.3)
    tp = w.add(m, "top_cap", "wood", base=(176, 124, 70), smooth=60)
    ml.lathe(tp, "y", (0, 0, 0), [(HAFT_CM - 0.5, R_HAFT * 1.15), (HAFT_CM + 1.2, R_HAFT * 1.2), (HAFT_CM + 2.6, R_HAFT * 0.9), (HAFT_CM + 3.2, R_HAFT * 0.4)], sides=12)
    rv = w.add(m, "head_rivets", "iron", base=(190, 194, 200), smooth=70)
    for (px, py) in ((255, 835), (240, 900), (225, 965)):                # three rivets along the cheek
        lx, ly = local(px, py)
        for face in (1, -1):
            ml.rivet(rv, (lx, ly, face * HEAD_T * 0.5), 0.36, (0, 0, face), 8)
    nk = w.add(m, "head_nicks", "iron", base=(60, 64, 72), wear=0.9)
    for (px0, py0, px1, py1) in ((90, 905, 150, 912), (110, 950, 168, 975), (128, 1010, 176, 1035)):   # the hammered chips and scratches on the card's head
        a, b = local(px0, py0), local(px1, py1)
        ml.ridge_pair(nk, (a[0], a[1], HEAD_T * 0.38), (b[0], b[1], HEAD_T * 0.34), (0, 0, 1), 0.3, 0.1, 0.05, 1.0)
    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    ref = w.reference(W.P3 + "Screenshot_20261013_223153_Opera.jpg", (40, 760, 700, 1450), "the_lumberjack")
    W.finish("the_lumberjack", m, atlas, 1.0, glow=("FFFFFF", 0.14, 0.45, 0.8), ref=ref)


# ================================================================ White Whip of Welting
def white_whip_of_welting():
    # ---- 1 measure: the card is 736 x 736; the towel's far loop spans card (270, 140) to (520, 400); a snapping towel is rolled to about 70 cm
    TOTAL_M = 0.85
    ROLL_LEN = 64.0                                      # the rolled body, cm
    R_ROLL = 2.6                                         # a tightly wound hand towel: 5 cm across
    GRIP_LEN, R_GRIP = 16.0, 1.9                         # the end the hand holds, wound tighter
    TAIL_LEN, TAIL_W = 15.0, 7.5                         # the loose end with its trim, card (205-290, 380-460) is 90 x 60 px
    m = ml.Mesh("white_whip_of_welting")
    xs = lambda y: 1.6 * math.sin(y * 0.07) + 0.9 * math.sin(y * 0.19 + 1.0)        # a soft, rolled-up wobble
    gr = w.add(m, "grip_roll", "cloth", base=(222, 222, 216), uv_mode="loft", smooth=60, wear=0.3)
    ml.loft(gr, "y", (0, 0, 0), [(0.0, 0.85, 0.85), (4.0, 1.0, 1.0), (GRIP_LEN, 1.02, 1.02)], ml.circle(R_GRIP, 12))
    gw = w.add(m, "grip_turns", "cloth", base=(196, 196, 190), wear=0.2)
    W.wrap_rings(gw, 0.8, GRIP_LEN - 0.8, R_GRIP * 1.06, 8, h=0.6, sides=12)
    bd = w.add(m, "body_roll", "cloth", base=(232, 232, 226), uv_mode="loft", smooth=60, wear=0.3)
    pts = [(xs(y), y, 0.0) for y in np.linspace(GRIP_LEN - 0.5, GRIP_LEN + ROLL_LEN, 26)]
    ml.sweep(bd, pts, ml.circle(R_ROLL, 12), scales=lambda t: (1.0 - 0.18 * t + 0.08 * math.sin(t * 9), 1.0 - 0.18 * t + 0.08 * math.sin(t * 9)))
    for nm, ph in (("twist_a", 0.0), ("twist_b", math.pi)):               # two soft ridges spiralling along the roll: the towel is twisted as it was wound
        tw = w.add(m, nm, "cloth", base=(206, 206, 200), smooth=70, wear=0.2)
        hp = w.helix(lambda t: R_ROLL * 1.0 * (1.0 - 0.18 * t), GRIP_LEN + 1.0, GRIP_LEN + ROLL_LEN - 1.0, 5.0, 56, phase=ph)
        hp = [(p[0] + xs(p[1]), p[1], p[2]) for p in hp]
        ml.sweep(tw, hp, ml.circle(0.45, 5), scales=lambda t: (1.0, 1.0))
    tk = w.add(m, "end_knot", "cloth", base=(220, 220, 214), smooth=70)
    w.ellipsoid(tk, (xs(GRIP_LEN + ROLL_LEN), GRIP_LEN + ROLL_LEN + 2.0, 0.0), 2.6, 3.2, 2.4, sides=12, stacks=8)
    tl = w.add(m, "tail_flat", "cloth", base=(228, 228, 222), uv_mode="loft", smooth=45, wear=0.2)
    tpath = [(-0.4 - 0.45 * i, -0.5 - 1.25 * i, 0.5 * math.sin(i * 0.6)) for i in range(12)]    # the loose end hangs past the grip, curling a little
    ml.sweep(tl, tpath, ml.rect_ring(1.0, 1.0), scales=lambda t: (TAIL_W * (0.55 + 0.45 * t), 0.7))
    red = w.add(m, "trim_red", "cloth", base=(204, 54, 56), wear=0.1)
    blue = w.add(m, "trim_blue", "cloth", base=(64, 92, 196), wear=0.1)
    for part, d in ((red, 0.0), (blue, 1.5)):                            # the red and blue embroidered bands across the tail's end
        for k in range(6):
            i = 8 + d / 1.5 * 0
            cx = -0.4 - 0.45 * (9.0 + d / 1.5) + (k - 2.5) * 0.0
            yy = -0.5 - 1.25 * (9.0 + d / 1.5)
            ml.bar(part, (cx - TAIL_W * 0.42 + k * (TAIL_W * 0.84 / 5), yy, 0.4), (cx - TAIL_W * 0.42 + k * (TAIL_W * 0.84 / 5) + 0.6, yy - 0.8, 0.4), 0.7, 0.18, up=(0, 0, 1))
    fr = w.add(m, "tail_fringe", "cloth", base=(236, 236, 230), smooth=80)
    for k in range(9):
        W.spike(fr, (-0.4 - 0.45 * 11 + (k - 4) * TAIL_W * 0.095, -0.5 - 1.25 * 11 - 0.2, 0.3), (0.0, -1.0, 0.0), 1.9, 0.28, sides=4)
    lp = w.add(m, "loop", "cloth", base=(226, 226, 220), uv_mode="loft", smooth=50, wear=0.2)
    lpts = []
    for i in range(0, 21):                                               # the draped loop of the card: the towel looped once over the roll
        a = -math.pi * 0.5 + 2 * math.pi * i / 20
        lpts.append((xs(36.0) + 4.8 * math.cos(a) * 0.7, 36.0 + 4.8 * math.sin(a) * 1.4, 3.0 + 1.8 * math.sin(a * 2)))
    ml.sweep(lp, lpts, ml.circle(1.15, 10), caps=(False, False))
    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    W.glow_part(atlas, m.part("trim_blue"), (110, 150, 255), 0.35, core=0.05)
    W.glow_part(atlas, m.part("trim_red"), (255, 110, 110), 0.3, core=0.05)
    ref = w.reference(W.P3 + "bda12a9468f96da917f51cc41bacb0bd.jpg", (180, 130, 560, 470), "white_whip_of_welting")
    W.finish("white_whip_of_welting", m, atlas, TOTAL_M, glow=("FFFFFF", 0.14, 0.4, 0.8), ref=ref)


# ================================================================ Whirling Blades
def whirling_blades():
    # ---- 1 measure: the card is 736 x 920. The cyan sword runs from its point (95, 495) to the end of its spiked grip (400, 150): 460 px; blade 332 px, grip 133 px
    TOTAL_M = 0.95
    S_TOTAL = 460.0
    K = w.px_to_cm(TOTAL_M * 100.0, S_TOTAL)             # cm per card px (0.207)
    S_POM, S_GRIP, S_GUARD, S_BLADE = (0.0, 18.0), (18.0, 128.0), (122.0, 140.0), (138.0, 460.0)   # card px from the pommel spike to the point
    R_GRIP = 11.0 * K                                    # the twisted grip is about 22 px across
    BLADE_W = w.lerp_table([(138.0, 24.0), (190.0, 32.0), (260.0, 34.0), (340.0, 27.0), (410.0, 15.0), (460.0, 0.0)])   # the leaf-shaped blade: widest in the lower half
    CURVE = 11.0                                         # how far the point lies off the axis, cm (the blades curve outward like scimitars)
    SPLAY = 1.5                                          # degrees each sword leans away from the pair's centre line
    SIDE = 4.6                                           # half the gap between the two swords at the grip, cm

    def sword(tag, col, sigil):
        y = lambda s: s * K
        parts = []
        bl = w.add(m, tag + "_blade", "steel", base=col, smooth=40, uv_mode="loft")
        path = [(-CURVE * (max(0.0, s - S_BLADE[0]) / (S_BLADE[1] - S_BLADE[0])) ** 2.2, y(s), 0.0) for s in np.linspace(S_BLADE[0] - 4, S_BLADE[1], 20)]
        ml.sweep(bl, path, ml.lozenge(1.0, 1.0, 0.2), scales=lambda t: (max(BLADE_W(S_BLADE[0] - 4 + t * (S_BLADE[1] - S_BLADE[0] + 4)) * K, 0.0), 0.85 * (1.0 - 0.55 * t) if t < 0.97 else 0.0), up=(0, 0, 1))
        sb = w.add(m, tag + "_sigil", "bone", base=(236, 240, 244), wear=0.1)
        W.extrude(sb, [(1.9, -1.1), (1.9, 1.1), (-1.9, 1.1), (-1.9, -1.1)], 0.3, 0.55, 0.05, at=(0.0, y(S_BLADE[0] + 36)))
        for a in (0, 90, 45, 135):                                       # a sigil of crossed lines on the band
            ca, sa = math.cos(math.radians(a)), math.sin(math.radians(a))
            ml.ridge_pair(sb, (-ca * 1.5, y(S_BLADE[0] + 36) - sa * 0.9, 0.55), (ca * 1.5, y(S_BLADE[0] + 36) + sa * 0.9, 0.55), (0, 0, 1), 0.22, 0.07, 0.04, 0.4)
        gc = w.add(m, tag + "_grip", "iron", base=(52, 50, 58), uv_mode="loft", smooth=45)
        ml.loft(gc, "y", (0, 0, 0), [(y(S_GRIP[0]), 0.8, 0.8), (y(60), 1.0, 1.0), (y(100), 1.05, 1.05), (y(S_GRIP[1]), 0.9, 0.9)], ml.circle(R_GRIP, 10))
        gs = w.add(m, tag + "_twist", "iron", base=(110, 106, 118), smooth=60, wear=0.8)
        for ph in (0.0, math.pi):                                        # the twisted ridges of the grip
            hp = w.helix(lambda t: R_GRIP * 1.0, y(S_GRIP[0]) + 0.5, y(S_GRIP[1]) - 0.5, 4.0, 40, phase=ph)
            ml.sweep(gs, hp, ml.circle(0.34, 5), scales=lambda t: (1.0, 1.0))
        sk = w.add(m, tag + "_spikes", "iron", base=(60, 56, 66), smooth=70)
        for i in range(6):                                               # the thorns at the guard collar and the pommel
            a = i * math.pi / 3
            W.spike(sk, (R_GRIP * 1.1 * math.cos(a), y(S_GUARD[0] + 8), R_GRIP * 1.1 * math.sin(a)), (math.cos(a) * 0.8, 0.5, math.sin(a) * 0.8), 2.6, 0.5, sides=5)
        W.spike(sk, (0, y(S_POM[0]) + 0.5, 0), (0, -1, 0), 3.8, 1.0, sides=6)
        W.spike(sk, (0, y(S_GUARD[1]) - 0.6, 0), (0, 1, 0), 1.2, R_GRIP * 1.35, sides=10)
        return [bl, sb, gc, gs, sk]
    m = ml.Mesh("whirling_blades")
    parts_a = sword("cold", (150, 230, 224), True)
    parts_b = sword("hot", (236, 150, 118), True)
    # place: A (cold) at -x leaning left, B (hot) at +x leaning right, B mirrored so its blade curves the other way
    for p in list(m.parts):
        if p.name.startswith("cold_"):
            q = p.translated(-SIDE, 0, 0, name=p.name).rotated("z", SPLAY, about=(-SIDE, 0, 0), name=p.name)
        else:
            q = p.mirrored("x", name=p.name).translated(SIDE, 0, 0, name=p.name).rotated("z", -SPLAY, about=(SIDE, 0, 0), name=p.name)
        p.V, p.F, p.tags = q.V, q.F, q.tags
    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    W.glow_part(atlas, m.part("cold_blade"), (90, 255, 235), 0.55, core=0.15)
    W.glow_part(atlas, m.part("hot_blade"), (255, 110, 60), 0.55, core=0.15)
    W.glow_part(atlas, m.part("cold_sigil"), (150, 255, 245), 0.9)
    W.glow_part(atlas, m.part("hot_sigil"), (255, 150, 90), 0.9)
    W.tint_part(atlas, m.part("cold_blade"), (120, 240, 230), 0.55)
    W.tint_part(atlas, m.part("hot_blade"), (240, 120, 84), 0.55)
    ref = w.reference(W.P3 + "ee57452c45139177e31fe4550eff205a.jpg", (60, 90, 700, 540), "whirling_blades")
    W.finish("whirling_blades", m, atlas, TOTAL_M, glow=("FFFFFF", 0.22, 0.5, 1.0), ref=ref)


BUILDERS = {"warmages_longsword": warmages_longsword, "giant_jawblade": giant_jawblade, "the_lumberjack": the_lumberjack,
            "white_whip_of_welting": white_whip_of_welting, "whirling_blades": whirling_blades}
