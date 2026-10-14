"""Wave 12 models, last five: Sword Crystal, The Stabshroom, Twin Hook Swords, Bottle Blade, Disintegration Dagger (the owner's model method).

Same conventions as wave12_ma.py: centimetres, y along the weapon (grip below, point above), x across, +z toward the viewer.
"""
import math

import numpy as np

import mesh_lib as ml
import wave1_models as w
import wave12_models as W


def build(item_id):
    BUILDERS[item_id]()


def prism(part, base, direction, length, r, sides=6, squash=1.0):
    """A crystal: a hexagonal prism with a blunt pointed top, standing at `base` along `direction`."""
    st = [(0.0, r, r * squash), (length * 0.8, r * 0.92, r * 0.92 * squash), (length, r * 0.2, r * 0.2 * squash)]
    return ml.loft(part, np.asarray(direction, float), base, st, ml.circle(1.0, sides))


# ================================================================ Sword Crystal
def sword_crystal():
    # ---- 1 measure: the card is 736 x 736; the crystal runs from its stopper (630, 40) to its point (25, 585): 814 px. s = card px from the stopper
    TOTAL_M = 0.9
    S_TOT = 814.0
    K = w.px_to_cm(TOTAL_M * 100.0, S_TOT)               # cm per card px (0.110)
    y = lambda s: (S_TOT - s) * K                        # the model has the stopper at the bottom (y = 0 at the stopper) and the point at the top
    WID = w.lerp_table([(0.0, 80.0), (60.0, 130.0), (120.0, 170.0), (250.0, 210.0), (400.0, 200.0), (550.0, 150.0), (700.0, 70.0), (S_TOT, 0.0)])   # blade width in card px
    CURVE = 7.0                                          # the point bends off the axis, cm
    ARM_END = 300.0                                      # the encased arm, card px from the stopper
    rng = np.random.default_rng(1212)
    LEN = TOTAL_M * 100.0
    half = lambda yv: WID(max(0.0, min(1.0, (yv - 12.0) / (LEN - 12.0))) * S_TOT) * K * 0.5          # half the blade's width at height yv
    bend = lambda yv: CURVE * (max(0.0, min(1.0, (yv - 12.0) / (LEN - 12.0)))) ** 2.0                # how far the axis has bent at height yv
    m = ml.Mesh("sword_crystal")

    st = w.add(m, "stopper", "leather", base=(226, 168, 148), smooth=55, wear=0.3)
    ml.lathe(st, "y", (0, 0, 0), [(-1.2, 0.0), (-0.9, 3.3), (0.2, 4.3), (4.2, 4.1), (5.0, 3.5)], sides=14)
    cf = w.add(m, "cuff", "crystal", base=(80, 170, 220), smooth=50)
    ml.lathe(cf, "y", (0, 0, 0), [(4.6, 4.4), (6.0, 5.9), (9.0, 6.6), (12.0, 6.2)], sides=10, squash=0.85)
    ac = w.add(m, "arm_core", "crystal", base=(70, 160, 214), smooth=45)
    ml.loft(ac, "y", (0, 0, 0), [(11.0, 8.0, 6.6), (22.0, 11.0, 8.2), (32.0, 12.0, 7.4), (44.0, 10.0, 5.4)], ml.octagon(1.0, 1.0, 0.3))
    bl = w.add(m, "blade", "crystal", base=(92, 196, 236), smooth=38, uv_mode="loft")
    path = [(CURVE * (t ** 2.0), 12.0 + t * (TOTAL_M * 100.0 - 12.0), 0.0) for t in np.linspace(0, 1, 22)]
    ml.sweep(bl, path, ml.lozenge(1.0, 1.0, 0.3), scales=lambda t: (WID(t * S_TOT) * K + 0.3 if t < 0.985 else 0.0, 7.0 * (1.0 - 0.82 * t) if t < 0.985 else 0.0), up=(0, 0, 1))
    fr = w.add(m, "facet_ridges", "crystal", base=(190, 240, 255), wear=0.8)
    for face in (1, -1):                                                 # the cut facets of the crystal: raised ridges running diagonally over both faces
        for (x0, y0, x1, y1) in ((-5, 20, -8, 52), (3, 24, 2, 62), (-6, 44, -3, 70), (6, 50, 9, 74), (0, 62, 3, 82)):
            ml.bar(fr, (x0, y0, face * 1.7 * (1 - y0 / 100.0)), (x1, y1, face * 1.7 * (1 - y1 / 100.0)), 0.9, 0.35, up=(0, 0, face), taper=3.0)
    sb = w.add(m, "shards_back", "crystal", base=(120, 214, 246), smooth=70)
    for i in range(9):                                                   # the jagged upper edge: prisms leaning outward, larger toward the arm
        yb = 24.0 + i * 7.0
        xb = -half(yb) * 0.9 + bend(yb)
        prism(sb, (xb, yb, rng.uniform(-0.8, 0.8)), (-0.7, 0.7 + rng.uniform(0, 0.3), rng.uniform(-0.3, 0.3)), 6.5 - 0.3 * i + rng.uniform(0, 1.5), 1.6 - 0.07 * i)
    sa = w.add(m, "shards_arm", "crystal", base=(150, 228, 252), smooth=70)
    for i in range(8):                                                   # the bulky cluster round the arm: prisms standing off the faces
        yb = 16.0 + i * 3.6
        face = 1 if i % 2 == 0 else -1
        prism(sa, (rng.uniform(-3, 3), yb, face * 3.0), (rng.uniform(-0.4, 0.4), 0.5, face * 0.9), 4.0 + rng.uniform(0, 2.5), 1.6 + rng.uniform(0, 0.6))
    se = w.add(m, "shards_edge", "crystal", base=(110, 206, 240), smooth=70)
    for i in range(6):                                                   # small facets along the long curved lower edge
        yb = 34.0 + i * 9.0
        xb = half(yb) * 0.9 + bend(yb)
        prism(se, (xb, yb, 0.0), (0.9, 0.35, rng.uniform(-0.3, 0.3)), 3.0 + rng.uniform(0, 1.5), 1.0)
    ch = w.add(m, "chips", "crystal", base=(190, 244, 255), smooth=70)
    for (cx, cy, cz) in ((-9.0, 70.0, 2.0), (8.0, 78.0, -1.5), (-6.0, 86.0, -2.0), (10.0, 58.0, 2.5)):   # splinters hanging off the broken-glass edge
        prism(ch, (cx, cy, cz), (rng.uniform(-0.5, 0.5), 1.0, rng.uniform(-0.4, 0.4)), 2.4, 0.7, sides=5)
    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    for nm, stg in (("blade", 0.3), ("shards_back", 0.55), ("shards_arm", 0.5), ("shards_edge", 0.5), ("chips", 0.6), ("facet_ridges", 0.5), ("arm_core", 0.2), ("cuff", 0.3)):
        W.glow_part(atlas, m.part(nm), (120, 225, 255), stg, core=0.25)
    ref = w.reference(W.P3 + "db343bf4fa03edd9a9cca685aea89418.jpg", (10, 20, 700, 620), "sword_crystal")
    W.finish("sword_crystal", m, atlas, TOTAL_M, glow=("FFFFFF", 0.16, 0.5, 1.0), ref=ref)


# ================================================================ The Stabshroom
def the_stabshroom():
    # ---- 1 measure: the card is 640 x 800; the dagger runs from the pommel's top (180, 110) to the point (105, 775): 667 px
    TOTAL_M = 0.37
    S_TOT = 667.0
    K = w.px_to_cm(TOTAL_M * 100.0, S_TOT)               # cm per card px (0.0555)
    y = lambda s: s * K
    S_POM, S_GRIP, S_CAP, S_BLADE = (0.0, 48.0), (46.0, 205.0), (205.0, 288.0), (284.0, S_TOT)
    R_POM, R_GRIP, R_CAP = 58.0 * K, 35.0 * K, 132.0 * K   # pommel shelf 115 px wide, grip 70 px, red cap 265 px wide
    BLADE_W = w.lerp_table([(284.0, 105.0), (400.0, 100.0), (520.0, 76.0), (620.0, 42.0), (S_TOT, 0.0)])
    m = ml.Mesh("the_stabshroom")
    pm = w.add(m, "pommel_shelf", "bone", base=(214, 190, 142), smooth=60)
    ml.lathe(pm, "y", (0, 0, 0), [(y(0), 0.0), (y(4), R_POM * 0.7), (y(12), R_POM), (y(24), R_POM * 0.92), (y(34), R_POM * 0.5), (y(40), R_POM * 0.62), (y(47), R_POM * 0.32)], sides=14)
    ml.lathe(pm, "y", (0.4, 0, 0.0), [(y(14), 0.0), (y(20), R_POM * 0.8), (y(26), R_POM * 0.55)], sides=12, caps=(False, True))
    gp = w.add(m, "grip", "leather", base=(46, 38, 40), uv_mode="loft", smooth=45)
    ml.loft(gp, "y", (0, 0, 0), [(y(S_GRIP[0]), 0.85, 0.85), (y(120), 1.1, 1.1), (y(S_GRIP[1]), 1.0, 1.0)], ml.circle(R_GRIP, 12))
    gf = w.add(m, "grip_fibres", "cloth", base=(192, 40, 48), smooth=70, wear=0.2)
    for k in range(5):                                                   # the red fibrous strands that wrap the grip
        hp = w.helix(lambda t: R_GRIP * 1.08, y(S_GRIP[0]) + 0.3, y(S_GRIP[1]) - 0.3, 1.2, 20, phase=k * 2 * math.pi / 5)
        ml.sweep(gf, hp, ml.circle(0.22, 5), scales=lambda t: (1.0, 1.0))
    cap = w.add(m, "mushroom_cap", "leather", base=(190, 36, 34), smooth=60, wear=0.2)
    prof = [(y(S_CAP[0]), 0.0), (y(S_CAP[0] + 5), R_CAP * 0.42), (y(S_CAP[0] + 16), R_CAP * 0.72), (y(S_CAP[0] + 34), R_CAP * 0.93), (y(S_CAP[0] + 56), R_CAP * 1.0),
            (y(S_CAP[1] - 2), R_CAP * 0.96), (y(S_CAP[1]), R_CAP * 0.6), (y(S_CAP[1]), R_CAP * 0.3), (y(S_CAP[1]) + 0.2, R_GRIP * 1.3)]
    ml.lathe(cap, "y", (0, 0, 0), prof, sides=18, caps=(True, True), squash=0.78)
    rm = w.add(m, "cap_rim", "leather", base=(150, 24, 24), smooth=70)
    ml.collar(rm, y(S_CAP[1]) - 0.9, R_CAP * 0.97, 0.9, lip=0.25, sides=18, oval=0.78)
    sp = w.add(m, "cap_spots", "bone", base=(246, 238, 226), smooth=75, wear=0.1)
    rng = np.random.default_rng(77)
    for i in range(16):                                                  # the white spots: studs standing out of the dome
        a = rng.uniform(0, 2 * math.pi)
        fs = rng.uniform(8.0, 0.8 * (S_CAP[1] - S_CAP[0]))
        s = S_CAP[0] + fs
        ys_, rs_ = [p[0] for p in prof], [p[1] for p in prof]
        r = float(np.interp(y(s), ys_, rs_))
        dr = float(np.interp(y(s) + 0.05, ys_, rs_)) - r                    # the dome's slope there gives its outward normal (dy, -dr) in the (r, y) plane
        nl = math.hypot(0.05, dr)
        nr, ny = 0.05 / nl, -dr / nl
        ml.stud(sp, (r * math.cos(a), y(s), r * math.sin(a) * 0.78), 0.34 + 0.22 * rng.random(), 0.34, (nr * math.cos(a), ny, nr * math.sin(a) * 0.78), sides=7)
    bl = w.add(m, "blade", "crystal", base=(112, 232, 64), smooth=40, uv_mode="loft")
    ml.loft(bl, "y", (0, 0, 0), [(y(S_BLADE[0]) - 0.3, BLADE_W(284) * K, 0.95), (y(400), BLADE_W(400) * K, 0.95), (y(520), BLADE_W(520) * K, 0.8),
                                  (y(620), BLADE_W(620) * K, 0.6), (y(S_TOT) - 0.6, 0.35, 0.2), (y(S_TOT), 0.0, 0.0)], ml.lozenge(1.0, 1.0, 0.25))
    br = w.add(m, "blade_ridge", "crystal", base=(190, 255, 130), wear=0.7)
    for face in (1, -1):
        ml.bar(br, (0.0, y(300), face * 0.5), (0.0, y(610), face * 0.3), 0.55, 0.22, up=(0, 0, face), taper=2.0)
    vn = w.add(m, "blade_veins", "crystal", base=(240, 150, 50), wear=0.5)
    for (x0, s0, x1, s1) in ((-1.4, 295, -0.9, 350), (1.3, 300, 0.7, 365), (-0.2, 340, 0.5, 400)):   # the orange veins where the poison runs near the guard
        ml.bar(vn, (x0, y(s0), 0.55), (x1, y(s1), 0.5), 0.3, 0.16, up=(0, 0, 1), taper=0.6)
    rt = w.add(m, "root_fibres", "cloth", base=(176, 38, 40), smooth=80)
    for i in range(8):
        a = 2 * math.pi * i / 8
        W.spike(rt, (R_GRIP * 1.1 * math.cos(a), y(S_CAP[0]) - 0.1, R_GRIP * 1.1 * math.sin(a)), (math.cos(a) * 0.5, -0.9, math.sin(a) * 0.5), 1.4, 0.2, sides=4)
    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    W.glow_part(atlas, m.part("blade"), (130, 255, 60), 0.55, core=0.12)
    W.glow_part(atlas, m.part("blade_ridge"), (210, 255, 150), 0.9)
    W.glow_part(atlas, m.part("blade_veins"), (255, 160, 60), 0.9)
    W.glow_part(atlas, m.part("cap_spots"), (255, 255, 230), 0.25, core=0.1)
    ref = w.reference(W.P3 + "ee5c57167bb8b2d368fb4a3fee2805db.jpg", (10, 70, 330, 790), "the_stabshroom")
    W.finish("the_stabshroom", m, atlas, TOTAL_M, glow=("FFFFFF", 0.2, 0.55, 1.0), ref=ref)


# ================================================================ Twin Hook Swords
def twin_hook_swords_one_mesh():   # the 1.28 two-sword single mesh, kept for reference; the game uses pair_models.py since 1.29.0
    # ---- 1 measure: the card is 736 x 752; the upper sword runs from its spear point (120, 215) to the end of its hook (600, 215): 480 px
    TOTAL_M = 0.95
    S_TOT = 480.0
    K = w.px_to_cm(TOTAL_M * 100.0, S_TOT)               # cm per card px (0.198)
    y = lambda s: s * K
    S_LEAF, S_GRIP, S_BLADE = (0.0, 112.0), (112.0, 198.0), (198.0, 462.0)
    LEAF_W = w.lerp_table([(0.0, 0.0), (30.0, 18.0), (78.0, 24.0), (112.0, 10.0)])    # the spear-like point is 22 px wide at its fullest
    R_GRIP = 15.0 * K                                    # the teal wrap, about 30 px across
    BLADE_W, BLADE_T = 11.0 * K, 7.0 * K                 # the straight blade: 11 px wide
    S_CRES = (96.0, 214.0)                               # the crescent guard spans 118 px along the sword
    CRES_REACH, CRES_W = 62.0 * K, 17.0 * K              # it stands 62 px off the blade line and is 17 px thick at the belly
    R_HOOK = 14.0 * K                                    # the hook at the far end: 28 px across
    GAP = 3.6                                            # the two swords lie 3.6 cm either side of the centre line
    m = ml.Mesh("twin_hook_swords")
    teal = (30, 128, 108)

    def sword(tag, side):
        x0 = side * GAP
        lf = w.add(m, tag + "_leaf", "steel", base=(196, 204, 214), smooth=45, uv_mode="loft")
        ml.loft(lf, "y", (x0, 0, 0), [(0.0, 0.0, 0.0), (y(30), LEAF_W(30) * K, 1.0), (y(78), LEAF_W(78) * K, 1.1), (y(S_LEAF[1]) + 0.2, LEAF_W(112) * K, 0.9)], ml.lozenge(1.0, 1.0, 0.25))
        gp = w.add(m, tag + "_grip", "leather", base=teal, uv_mode="loft", smooth=50)
        ml.loft(gp, "y", (x0, 0, 0), [(y(S_GRIP[0]), 0.9, 0.9), (y(150), 1.05, 1.05), (y(S_GRIP[1]), 0.92, 0.92)], ml.circle(R_GRIP, 10))
        cd = w.add(m, tag + "_cord", "cloth", base=(20, 92, 78), wear=0.2)
        W.wrap_rings(cd, y(S_GRIP[0]) + 0.3, y(S_GRIP[1]) - 0.3, R_GRIP * 1.08, 9, h=0.55, sides=10, x=x0)
        ft = w.add(m, tag + "_fittings", "steel", base=(176, 184, 196), smooth=45)
        ml.collar(ft, y(S_GRIP[0]) - 0.7, R_GRIP * 1.2, 0.9, lip=0.2, sides=10, origin=(x0, 0, 0))
        ml.collar(ft, y(S_GRIP[1]) - 0.2, R_GRIP * 1.2, 0.9, lip=0.2, sides=10, origin=(x0, 0, 0))
        for sp in (130.0, 182.0):                                        # the two posts that join the guard to the grip
            ml.lathe(ft, "x", (x0, y(sp), 0), [(0.0, 0.55), (side * (CRES_REACH * 0.34 + R_GRIP), 0.55)], sides=8)
        cr = w.add(m, tag + "_crescent", "steel", base=(204, 210, 222), smooth=50)
        pts = []
        for i in range(0, 21):                                           # the crescent: a bowl whose belly touches the posts and whose tips rise away from the grip
            t = i / 20.0
            pts.append((x0 + side * (R_GRIP + 1.4 + CRES_REACH * (1.0 - math.sin(math.pi * t)) * 0.9), y(S_CRES[0]) + t * (y(S_CRES[1]) - y(S_CRES[0])), 0.0))
        ml.sweep(cr, pts, ml.lozenge(1.0, 1.0, 0.35), scales=lambda t: (CRES_W * (0.12 + 0.88 * math.sin(math.pi * t) ** 0.8), 1.5 * (0.35 + 0.65 * math.sin(math.pi * t) ** 0.5)), up=(0, 0, 1))
        bd = w.add(m, tag + "_blade", "steel", base=(184, 192, 204), smooth=45, uv_mode="loft")
        ml.loft(bd, "y", (x0, 0, 0), [(y(S_BLADE[0]) - 0.4, BLADE_W * 1.3, BLADE_T * 1.2), (y(260), BLADE_W * 1.05, BLADE_T * 1.05), (y(400), BLADE_W * 0.9, BLADE_T * 0.95), (y(S_BLADE[1]), BLADE_W * 0.8, BLADE_T * 0.9)], ml.octagon(1.0, 1.0, 0.28))
        hp = []
        for i in range(0, 12):                                           # the hook: the blade's end curls round toward the other sword
            a = math.radians(i * 18.5)
            hp.append((x0 - side * R_HOOK * (1 - math.cos(a)), y(S_BLADE[1]) + R_HOOK * math.sin(a), 0.0))
        ml.sweep(bd, hp, ml.octagon(1.0, 1.0, 0.28), scales=lambda t: (BLADE_W * (0.8 - 0.55 * t), BLADE_T * (0.9 - 0.5 * t)))

    sword("hook_a", 1)
    sword("hook_b", -1)
    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    for tg in ("hook_a", "hook_b"):
        W.glow_part(atlas, m.part(tg + "_blade"), (210, 235, 235), 0.12, core=0.1)
    ref = w.reference(W.P3 + "faca332834adf1adfeccbb405016e64a.jpg", (80, 120, 640, 340), "twin_hook_swords")
    W.finish("twin_hook_swords", m, atlas, TOTAL_M, glow=("FFFFFF", 0.14, 0.45, 0.8), ref=ref)


# ================================================================ Bottle Blade
def bottle_blade():
    # ---- 1 measure: the card is 736 x 920; the bottle runs from its base (190, 590) to its mouth (352, 345): 295 px; the stream goes on to (575, 0): 410 px more
    TOTAL_M = 0.62
    S_BOTTLE, S_TOT = 295.0, 705.0
    K = w.px_to_cm(TOTAL_M * 100.0, S_TOT)               # cm per card px (0.088)
    y = lambda s: s * K
    R_BODY, R_NECK = 45.0 * K, 17.0 * K                  # body 90 px across, neck 33 px
    BAND = (50.0, 120.0)                                 # the silver band, card px from the base
    STREAM_W = 28.0 * K                                  # the stream is about 28 px wide near the mouth
    m = ml.Mesh("bottle_blade")
    gl = w.add(m, "glass", "crystal", base=(52, 150, 112), smooth=55)
    prof = [(y(0), 0.0), (y(6), R_BODY * 0.55), (y(18), R_BODY * 0.9), (y(34), R_BODY), (y(150), R_BODY), (y(185), R_BODY * 0.8), (y(215), R_NECK * 1.5), (y(238), R_NECK * 1.08),
            (y(S_BOTTLE - 12), R_NECK), (y(S_BOTTLE - 4), R_NECK * 1.12), (y(S_BOTTLE), R_NECK * 1.08)]
    ml.lathe(gl, "y", (0, 0, 0), prof, sides=16, caps=(True, False))
    lp = w.add(m, "lip", "crystal", base=(86, 186, 146), smooth=60)
    ml.collar(lp, y(S_BOTTLE) - 0.7, R_NECK * 1.22, 0.9, lip=0.25, sides=16)
    bd = w.add(m, "band", "silver", base=(176, 184, 192), smooth=45)
    ml.lathe(bd, "y", (0, 0, 0), [(y(BAND[0]), R_BODY * 1.0), (y(BAND[0]) + 0.25, R_BODY * 1.06), (y(BAND[1]) - 0.25, R_BODY * 1.06), (y(BAND[1]), R_BODY * 1.0)], sides=16, caps=(False, False))
    be = w.add(m, "band_engraving", "silver", base=(230, 236, 242), wear=0.1)
    zc = R_BODY * 1.06
    yc = y((BAND[0] + BAND[1]) / 2)
    ml.bar(be, (0.0, yc - 2.2, zc), (0.0, yc + 2.4, zc), 0.34, 0.16, up=(0, 0, 1), taper=0.5)                  # the little sword engraved on the band: blade ...
    ml.bar(be, (-0.9, yc - 0.6, zc), (0.9, yc - 0.6, zc), 0.3, 0.16, up=(0, 0, 1))                              # ... and cross-guard
    for side in (-1, 1):                                                                                        # ... and a leaf either side
        ml.bar(be, (side * 1.0, yc - 2.0, zc * 0.99), (side * 2.1, yc + 0.6, zc * 0.96), 0.3, 0.14, up=(side * 0.4, 0, 1), taper=0.5)
        ml.bar(be, (side * 2.1, yc + 0.6, zc * 0.96), (side * 1.2, yc + 2.6, zc * 0.98), 0.3, 0.14, up=(side * 0.4, 0, 1), taper=0.5)
    st = w.add(m, "stream_blade", "crystal", base=(128, 232, 255), smooth=40, uv_mode="loft")
    s0 = y(S_BOTTLE)
    spath = [(0.9 * math.sin(i * 0.5) * (i / 24.0), s0 + (y(S_TOT) - s0) * i / 24.0, 0.0) for i in range(25)]
    ml.sweep(st, spath, ml.lozenge(1.0, 1.0, 0.2), scales=lambda t: ((STREAM_W * (0.55 + 0.45 * math.sin(math.pi * min(1.0, 0.25 + t * 0.75)) * (1.0 - t ** 3)) * (1.0 + 0.1 * math.sin(t * 25))) if t < 0.985 else 0.0, 0.9 * (1.0 - 0.6 * t) if t < 0.985 else 0.0), up=(0, 0, 1))
    sc = w.add(m, "stream_core", "crystal", base=(210, 252, 255), smooth=60)
    ml.sweep(sc, [(p[0], p[1], 0.0) for p in spath[1:-2]], ml.circle(0.28, 5), scales=lambda t: (1.0 - 0.7 * t, 1.0 - 0.7 * t))
    dr = w.add(m, "droplets", "crystal", base=(190, 244, 255), smooth=75)
    for (dx, dy, r) in ((-3.0, 4.0, 0.7), (3.2, 9.0, 0.6), (-2.6, 15.0, 0.55), (2.8, 20.0, 0.7), (-3.4, 25.0, 0.5), (3.0, 29.0, 0.45)):
        w.ellipsoid(dr, (dx, s0 + dy, 0.4 * dx), r * 0.8, r * 1.25, r * 0.8, sides=8, stacks=6)
    sp = w.add(m, "splash", "crystal", base=(160, 240, 255), smooth=80)
    for i in range(8):                                                   # the crown of water where the stream leaves the neck
        a = 2 * math.pi * i / 8
        W.spike(sp, (R_NECK * 0.9 * math.cos(a), s0 - 0.2, R_NECK * 0.9 * math.sin(a)), (math.cos(a) * 0.9, 0.6, math.sin(a) * 0.9), 1.9, 0.28, sides=5)
    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    for nm, stg in (("stream_blade", 0.55), ("stream_core", 0.95), ("droplets", 0.7), ("splash", 0.7)):
        W.glow_part(atlas, m.part(nm), (110, 235, 255), stg, core=0.35)
    W.glow_part(atlas, m.part("band_engraving"), (190, 255, 240), 0.45, core=0.1)
    ref = w.reference(W.P2 + "20e984f2f5e093a9454b4fa68d47de1c.jpg", (150, 0, 600, 640), "bottle_blade")
    W.finish("bottle_blade", m, atlas, TOTAL_M, glow=("FFFFFF", 0.22, 0.5, 1.0), ref=ref)


# ================================================================ Disintegration Dagger
def disintegration_dagger():
    # ---- 1 measure: the card is 736 x 952; the dagger runs from its pommel spike (580, 30) to the point (355, 470): 494 px
    TOTAL_M = 0.42
    S_TOT = 494.0
    K = w.px_to_cm(TOTAL_M * 100.0, S_TOT)               # cm per card px (0.085)
    y = lambda s: s * K
    S_GRIP, S_GUARD, S_BLADE = (46.0, 150.0), (150.0, 186.0), (184.0, S_TOT)
    R_GRIP = 11.0 * K                                    # a slim grip, about 22 px across
    GUARD_HALF = 75.0 * K                                # the guard is 150 px wide
    BLADE_W = w.lerp_table([(184.0, 66.0), (260.0, 66.0), (350.0, 54.0), (430.0, 30.0), (S_TOT, 0.0)])
    CURVE = -2.5                                         # the blade bows slightly toward its edge
    m = ml.Mesh("disintegration_dagger")
    pk = w.add(m, "pommel_spikes", "iron", base=(70, 54, 50), smooth=60)
    ml.lathe(pk, "y", (0, 0, 0), [(y(10), 0.0), (y(14), 1.5), (y(40), 1.35), (y(S_GRIP[0]), 1.15)], sides=8)
    W.spike(pk, (0.0, y(12), 0.0), (-0.28, -1.0, 0.0), 6.0, 1.0, sides=6, belly=0.15)                  # the two long spikes standing at the pommel end
    W.spike(pk, (0.2, y(12), 0.0), (0.22, -1.0, 0.0), 4.2, 0.85, sides=6, belly=0.15)
    gp = w.add(m, "grip", "leather", base=(40, 30, 32), uv_mode="loft", smooth=45)
    ml.loft(gp, "y", (0, 0, 0), [(y(S_GRIP[0]), 0.9, 0.9), (y(100), 1.1, 1.1), (y(S_GRIP[1]), 1.0, 1.0)], ml.circle(R_GRIP, 10))
    gr = w.add(m, "grip_rings", "bronze", base=(110, 76, 58), wear=0.5)
    W.wrap_rings(gr, y(S_GRIP[0]) + 0.4, y(S_GRIP[1]) - 0.8, R_GRIP * 1.12, 6, h=0.5, sides=10)
    sr = w.add(m, "straps", "leather", base=(32, 28, 38), wear=0.6, smooth=60)
    for (ang, ln) in ((0.5, 15.0), (2.4, 11.0)):                         # the two leather straps knotted round the grip, hanging down
        sp = [(R_GRIP * 1.2 * math.cos(ang) + 0.9 * k / 8.0 * math.cos(ang), y(100) - ln * k / 8.0, R_GRIP * 1.2 * math.sin(ang) + 0.9 * k / 8.0 * math.sin(ang)) for k in range(9)]
        ml.sweep(sr, sp, ml.rect_ring(1.0, 1.0), scales=lambda t: (1.1 * (1.0 - 0.25 * t), 0.2))
    gd = w.add(m, "guard", "iron", base=(60, 46, 44), smooth=45)
    W.extrude(gd, [(2.6, 0.0), (1.2, 2.1), (-1.2, 2.1), (-2.6, 0.0), (-1.2, -2.1), (1.2, -2.1)], -1.1, 1.1, 0.4, at=(0.0, y(168)))
    for side in (-1, 1):                                                 # the two horns sweeping out and then up toward the pommel
        hp = []
        for i in range(0, 13):
            t = i / 12.0
            hp.append((side * (2.0 + GUARD_HALF * 0.9 * math.sin(t * math.pi * 0.5)), y(168) + 1.4 * (t ** 2) * -4.0 + 3.0 * t ** 2.5 * 4.0 - 0.0, 0.0))
        ml.sweep(gd, hp, ml.octagon(1.0, 1.0, 0.3), scales=lambda t: (1.6 * (1.0 - 0.7 * t), 1.5 * (1.0 - 0.7 * t)))
    gs = w.add(m, "guard_spikes", "iron", base=(84, 62, 56), smooth=70)
    for side in (-1, 1):
        W.spike(gs, (side * GUARD_HALF * 0.92, y(168) + 3.0, 0.0), (side * 0.3, 1.0, 0.0), 3.4, 0.55, sides=5)
        W.spike(gs, (side * GUARD_HALF * 0.55, y(168) + 0.6, 0.0), (side * 0.5, 0.8, 0.0), 2.2, 0.45, sides=5)
        W.spike(gs, (side * 2.0, y(168), 1.0), (side * 0.2, 0.2, 1.0), 1.8, 0.5, sides=5)
    bl = w.add(m, "blade", "iron", base=(34, 28, 30), smooth=40, uv_mode="loft")
    path = [(CURVE * (t ** 2), y(S_BLADE[0]) - 0.4 + t * (y(S_TOT) - y(S_BLADE[0]) + 0.4), 0.0) for t in np.linspace(0, 1, 20)]
    ml.sweep(bl, path, ml.lozenge(1.0, 1.0, 0.3), scales=lambda t: (BLADE_W(S_BLADE[0] + t * (S_TOT - S_BLADE[0])) * K + 0.02 if t < 0.985 else 0.0, 1.0 * (1.0 - 0.55 * t) if t < 0.985 else 0.0), up=(0, 0, 1))
    sc = w.add(m, "scale_plates", "bronze", base=(78, 52, 44), smooth=50, wear=0.8)
    for face in (1, -1):                                                 # the layered, scale-like ridges: seven chevrons down each face
        for i in range(7):
            s = S_BLADE[0] + 28.0 + i * 42.0
            hw = BLADE_W(s) * K * 0.36
            zf = face * 0.46 * (1.0 - 0.45 * (s - S_BLADE[0]) / (S_TOT - S_BLADE[0]))
            cx = CURVE * ((s - S_BLADE[0]) / (S_TOT - S_BLADE[0])) ** 2
            ml.bar(sc, (cx - hw, y(s) + 1.0, zf), (cx, y(s) - 0.4, zf), 0.7, 0.18, up=(0, 0, face), taper=0.6)
            ml.bar(sc, (cx, y(s) - 0.4, zf), (cx + hw, y(s) + 1.0, zf), 0.7, 0.18, up=(0, 0, face), taper=0.6)
    sp = w.add(m, "spine_ridge", "iron", base=(90, 70, 62), wear=0.7)
    for face in (1, -1):
        ml.bar(sp, (CURVE * 0.0, y(S_BLADE[0]) + 0.5, face * 0.4), (CURVE * 0.8, y(S_TOT) - 4.0, face * 0.18), 0.55, 0.22, up=(0, 0, face), taper=4.0)
    rn = w.add(m, "rune", "silver", base=(150, 214, 255), wear=0.1)
    ry = y(S_BLADE[0] + 70)
    for (x0, y0, x1, y1) in ((0.7, 1.8, -0.6, 0.6), (-0.6, 0.6, 0.6, -0.3), (0.6, -0.3, -0.5, -1.6)):   # a small jagged bolt: the blue rune glowing on the blade
        ml.bar(rn, (x0, ry + y0, 0.62), (x1, ry + y1, 0.62), 0.32, 0.12, up=(0, 0, 1))
    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    W.glow_part(atlas, m.part("rune"), (70, 170, 255), 1.0, core=0.12)
    W.glow_part(atlas, m.part("scale_plates"), (255, 120, 60), 0.12, core=0.0)
    ref = w.reference(W.P2 + "6b5e5e1d211b01e6d3ef6c141c531a49.jpg", (300, 10, 720, 500), "disintegration_dagger")
    W.finish("disintegration_dagger", m, atlas, TOTAL_M, glow=("FFFFFF", 0.18, 0.5, 1.0), ref=ref)


BUILDERS = {"sword_crystal": sword_crystal, "the_stabshroom": the_stabshroom, "twin_hook_swords": lambda: __import__("pair_models").build("twin_hook_swords"), "bottle_blade": bottle_blade,
            "disintegration_dagger": disintegration_dagger}
