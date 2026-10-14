"""Wave 9 swords as 3D models: Caesar's Gladius, the Druidic Blade, the Twin Swords of Deflection and the Null Blade (the owner's model method).

Axis of every model: y runs from the pommel (0) to the point, x is across the blade (its width), z through it (the flat toward the viewer in hand).
"""
import math

import numpy as np

import mesh_lib as ml
import wave9_models as w9
from wave9_models import w

# ================================================================ Caesar's Gladius
# 1 measure. Card 736 x 736 (p2/0bf4947b...): the gladius lies horizontal from the pommel point (68, 297) to the blade's point (665, 297): 597 px.
G_TOTAL_CM = 62.7                                  # a gladius with its crest pommel: blade about 41 cm, hilt about 21 cm
G_PX = 597.0
GK = G_TOTAL_CM / G_PX                             # cm per card px (0.105)
G_POMMEL = (0.0, 67.0)                             # the helmet-crest pommel runs from the point (68) to x = 135
G_GRIP = (67.0, 147.0)                             # the cord-wrapped grip, x 135 to 215, with four white lacing bands
G_CORE = (147.0, 262.0)                            # the brown scaled guard block, x 215 to 330
G_FAN_S = 187.0                                    # the fan of feathers is centred on x 255
G_BLADE0, G_TIP = 215.0, 597.0                     # the blade is seated inside the core; the point is the card's right end
G_BLADE_W = 7.0                                    # the card draws the blade 85 px broad (8.9 cm); scaled down to a gladius
G_BLADE_T = 0.95
G_TAPER = 62.0                                     # the last 62 px close to the point
G_FEATHER_LEN = (6.4, 8.0, 9.2, 9.8, 9.2, 8.0, 6.4)  # seven feathers per side: the fan spans about 9 cm either way (card 80 px)
G_FEATHER_ANG = (-62.0, -42.0, -21.0, 0.0, 21.0, 42.0, 62.0)    # fan angles about the x axis, degrees, positive toward the point
G_FEATHER_W, G_FEATHER_T = 3.0, 0.34


def gy(s):
    return s * GK


def build_caesars_gladius():
    m = ml.Mesh("caesars_gladius")
    # ---- 1 the blade: a broad lozenge-section leaf, parallel edges, closing to a point
    bl = w.add(m, "blade", "steel", base=(176, 180, 190), smooth=40, uv_mode="loft")
    ml.loft(bl, "y", (0, 0, 0), [(gy(G_BLADE0), 1.0, 1.0), (gy(G_TIP - G_TAPER - 20), 1.0, 1.0), (gy(G_TIP - G_TAPER), 0.96, 0.95),
                                (gy(G_TIP - 24), 0.55, 0.7), (gy(G_TIP), 0.0, 0.0)], ml.lozenge(G_BLADE_W, G_BLADE_T, 0.45))
    # ---- 2 the midrib: a long raised bar on each face, tapering at both ends
    for sgn, nm in ((1, "midrib_f"), (-1, "midrib_b")):
        r = w.add(m, nm, "steel", base=(204, 208, 220), wear=0.8)
        ml.bar(r, (0, gy(G_BLADE0 + 70), sgn * G_BLADE_T * 0.42), (0, gy(G_TIP - 90), sgn * G_BLADE_T * 0.40), 1.0, 0.16, up=(0, 0, sgn), taper=3.0)
    # ---- 3 the three short dashes engraved in the blade on the card (two raised ridges each, the groove painted)
    dash = w.add(m, "blade_dashes", "steel", base=(120, 124, 140), wear=0.8)
    for s0, s1, x in ((330, 352, 1.6), (420, 447, -1.5), (500, 524, 1.7)):
        ml.ridge_pair(dash, (x, gy(s0), G_BLADE_T * 0.44), (x, gy(s1), G_BLADE_T * 0.44), (0, 0, 1), 0.3, 0.1, 0.06, 0.4)
    # ---- 4 the collar where the blade leaves the guard
    col = w.add(m, "blade_collar", "bronze", base=(150, 98, 70))
    ml.loft(col, "y", (0, 0, 0), [gy(G_CORE[1]) - 0.2, gy(G_CORE[1]) + 1.4], ml.octagon(G_BLADE_W + 0.9, 2.0, 0.5))
    # ---- 5 the guard core: a brown scaled block, swelling at the middle
    core = w.add(m, "guard_core", "leather", base=(112, 58, 52), smooth=45, uv_mode="loft")
    ml.loft(core, "y", (0, 0, 0), [(gy(G_CORE[0]), 0.78, 0.8), (gy(G_CORE[0] + 22), 1.0, 1.0), (gy(G_CORE[1] - 30), 1.0, 1.0), (gy(G_CORE[1]), 0.84, 0.85)], ml.octagon(4.7, 2.4, 0.8))
    # ---- 6/7 the fan: seven overlapping feathers above and seven below, the quills' tips bent back a little
    for sgn, nm in ((1, "feathers_up"), (-1, "feathers_dn")):
        f = w.add(m, nm, "leather", base=(124, 62, 56), wear=0.4, smooth=50)
        for i, (ang, ln) in enumerate(zip(G_FEATHER_ANG, G_FEATHER_LEN)):
            a = math.radians(ang)
            d = (sgn * math.cos(a), math.sin(a) * 0.9 - 0.1, 0.0)
            w9.leaf(f, (sgn * 1.0, gy(G_FAN_S) + 0.2 * i - 0.4, (i % 2) * 0.28 - 0.14), d, ln, G_FEATHER_W, G_FEATHER_T, up=(0, 0, 1))
    # ---- 8 the grip and 9 its lacing: white cord crossing the leather
    gp = w.add(m, "grip", "leather", base=(96, 70, 52), uv_mode="loft")
    ml.loft(gp, "y", (0, 0, 0), [(gy(G_GRIP[0]), 1.2, 1.2), (gy(G_GRIP[0] + 30), 1.5, 1.45), (gy(G_GRIP[1]), 1.35, 1.35)], ml.circle(1.0, 10))
    lace = w.add(m, "grip_lacing", "bone", base=(232, 226, 204), wear=0.2)
    for k, s in enumerate((150, 168, 186, 204)):
        ml.collar(lace, gy(G_POMMEL[1] + 4 + (s - 150) * 0.78), 1.65, 0.55, lip=0.14, sides=10)
    # ---- 10 the pommel: a helmet dome, with a point at the very end (the card's left tip)
    pm = w.add(m, "pommel", "bronze", base=(124, 62, 56), smooth=55)
    prof = [(0.0, 0.0), (gy(7), 0.9), (gy(24), 2.4), (gy(40), 3.0), (gy(55), 2.7), (gy(G_POMMEL[1]), 1.6)]
    ml.lathe(pm, "y", (0, 0, 0), prof, sides=14, squash=0.8)
    # ---- 11 the crest: a thin fin along the helmet, like a Roman crest
    cr = w.add(m, "pommel_crest", "bronze", base=(150, 98, 70), smooth=50)
    ml.loft(cr, "y", (0, 0, 2.2), [(gy(14), 0.1, 0.1), (gy(30), 0.5, 1.9), (gy(52), 0.5, 1.5), (gy(66), 0.1, 0.1)], ml.lozenge(1.0, 1.0, 0.3), caps=(True, True))
    # ---- 12 the rim ring where the grip meets the helmet
    rim = w.add(m, "pommel_rim", "silver", base=(196, 200, 212))
    ml.collar(rim, gy(G_POMMEL[1]) - 0.5, 1.9, 0.7, lip=0.18, sides=12)

    atlas = ml.Atlas(w9.ATLAS_PX)
    atlas.layout(m)
    atlas.paint(m)

    def marble(c):                                                  # the card's wavy grey veins, darker lines on a light steel
        n = w.noise2(c.img.shape[:2], 16, 7 + c.g.rect[0])
        vein = np.clip(1.0 - np.abs(n - 0.5) / 0.035, 0, 1)[..., None]
        c.img[..., :3] = c.img[..., :3] * (1 - 0.55 * vein) + np.array([70, 74, 92], np.float32) * 0.55 * vein
    w.paint_part(atlas, m.part("blade"), marble)

    def quills(c):                                                  # each feather: a lighter quill line down the middle, a warm sheen on the glow skin
        w.set_glow(c, (255, 214, 140), 0.16, mask=(np.abs(c.S - 0.5) < 0.06), core=0.2)
    for nm in ("feathers_up", "feathers_dn"):
        w.paint_part(atlas, m.part(nm), quills)
    w.paint_part(atlas, m.part("grip_lacing"), lambda c: w.set_glow(c, (255, 240, 200), 0.10, core=0.1))
    r = w9.ref("p2/Relics of the world part 2/0bf4947b14cd329b109f31f2977879db.jpg", (40, 190, 700, 400), "caesars_gladius")
    w9.finish("caesars_gladius", m, atlas, real_m=0.627, glow=("FFFFFF", 0.10, 0.5, 1.0), ref_png=r)


# ================================================================ Druidic Blade
# 1 measure. Card 736 x 736 (p2/6f0877aec...): the blade stands upright, pommel top (130, 25) to the point (135, 695): 670 px.
D_TOTAL_CM = 72.0                                  # a short elven blade
D_PX = 670.0
DK = D_TOTAL_CM / D_PX                             # cm per card px (0.107)
D_POMMEL = (0.0, 3.6)                              # the white crystal pommel, card y 25 to 62, then its collar
D_COLLAR = (3.0, 4.6)
D_GRIP = (4.6, 16.2)                               # the dark grip, card y 62 to 170 (the card's grip is drawn broad, scaled to 2.3 cm across)
D_GUARD_Y = 16.8                                   # the swept guard at card y 175 to 190
D_GUARD_HALF = 6.7                                 # the guard spans x 72 to 197 on the card: 125 px = 13.4 cm
D_BLADE = (17.6, 72.0)                             # the blade: base at card y 185, point at y 695 (510 px = 54.6 cm)
D_BLADE_W = w.lerp_table([(0.0, 6.4), (0.08, 7.6), (0.2, 7.8), (0.45, 7.2), (0.7, 5.6), (0.88, 3.4), (0.97, 1.4), (1.0, 0.0)])     # width by share of blade length: leaf-shaped
D_BLADE_T = 0.95
D_RUNE = (0.02, 0.34)                              # the glowing rune strip: the card's top third of the blade, 123 to 145 px wide
D_RUNES = 7                                        # notches in the strip


def build_druidic_blade():
    m = ml.Mesh("druidic_blade")
    L = D_BLADE[1] - D_BLADE[0]
    # ---- 1 the leaf-shaped blade, lozenge section so that both faces slope to the edge
    bl = w.add(m, "blade", "steel", base=(70, 160, 178), smooth=40, uv_mode="loft")
    ts = [0.0, 0.08, 0.2, 0.34, 0.45, 0.58, 0.7, 0.8, 0.88, 0.94, 0.985, 1.0]
    st = [(D_BLADE[0] + L * t, max(0.0, D_BLADE_W(t)), D_BLADE_T * (1.0 if t < 0.9 else 0.5)) for t in ts]
    st[-1] = (st[-1][0], 0.0, 0.0)
    ml.loft(bl, "y", (0, 0, 0), st, ml.lozenge(1.0, 1.0, 0.3))
    # ---- 2 the dark seam down the lower blade: two raised ridges with the groove between (painted dark)
    seam = w.add(m, "blade_seam", "iron", base=(30, 74, 92), wear=0.6)
    ml.ridge_pair(seam, (0, D_BLADE[0] + L * 0.36, D_BLADE_T * 0.40), (0, D_BLADE[1] - 4.0, D_BLADE_T * 0.12), (0, 0, 1), 0.28, 0.12, 0.06, 2.0)
    ml.ridge_pair(seam, (0, D_BLADE[0] + L * 0.36, -D_BLADE_T * 0.40), (0, D_BLADE[1] - 4.0, -D_BLADE_T * 0.12), (0, 0, -1), 0.28, 0.12, 0.06, 2.0)
    # ---- 3 the glowing rune strip: a slim raised plate on each face, notched with seven cuts
    for sgn, nm in ((1, "rune_strip_f"), (-1, "rune_strip_b")):
        rs = w.add(m, nm, "crystal", base=(80, 230, 220), wear=0.2)
        y0, y1 = D_BLADE[0] + L * D_RUNE[0], D_BLADE[0] + L * D_RUNE[1]
        for i in range(D_RUNES):
            a = y0 + (y1 - y0) * i / D_RUNES
            b = a + (y1 - y0) / D_RUNES * 0.62
            ml.bar(rs, (0, a, sgn * D_BLADE_T * 0.46), (0, b, sgn * D_BLADE_T * 0.46), 1.4, 0.12, up=(0, 0, sgn), taper=0.3, section="octagon")
    # ---- 4 the guard: a swept bar whose tips hook down toward the grip
    g = w.add(m, "guard", "iron", base=(40, 120, 140), smooth=45, uv_mode="loft")
    pts = [(x, D_GUARD_Y - 0.075 * x * x * (1.0 if abs(x) > 2.0 else 0.4), 0.0) for x in np.linspace(-D_GUARD_HALF, D_GUARD_HALF, 15)]
    sc = [(1.0 - 0.7 * (abs(x) / D_GUARD_HALF) ** 2.5, 1.0 - 0.6 * (abs(x) / D_GUARD_HALF) ** 2.5) for (x, _y, _z) in pts]
    ml.sweep(g, pts, ml.ellipse(0.9, 0.7, 8), scales=sc)
    # ---- 5 the guard's centre block over the blade's base
    gb = w.add(m, "guard_block", "bronze", base=(40, 120, 140))
    ml.loft(gb, "y", (0, 0, 0), [D_GUARD_Y - 1.0, D_GUARD_Y + 0.9], ml.octagon(3.6, 1.9, 0.6))
    # ---- 6 the grip: dark and ribbed, swelling a little in the hand
    gp = w.add(m, "grip", "leather", base=(34, 34, 44), uv_mode="loft")
    ml.loft(gp, "y", (0, 0, 0), [(D_GRIP[0], 1.05, 1.05), (D_GRIP[0] + 4, 1.25, 1.25), (D_GRIP[1] - 3, 1.2, 1.2), (D_GRIP[1], 1.0, 1.0)], ml.circle(1.0, 10))
    # ---- 7 the wrap rings
    wr = w.add(m, "grip_rings", "bronze", base=(60, 150, 160), wear=0.3)
    for i in range(5):
        ml.collar(wr, D_GRIP[0] + 1.4 + i * 2.1, 1.35, 0.5, lip=0.12, sides=10)
    # ---- 8 the pommel crystal: a cut white gem held in a cap
    pc = w.add(m, "pommel_crystal", "crystal", base=(218, 232, 246), smooth=20)
    ml.gem(pc, (0, 2.3, 0), 1.9, long=1.0, sides=6, axis="y")
    cap = w.add(m, "pommel_cap", "iron", base=(52, 58, 74))
    ml.collar(cap, D_COLLAR[0] + 1.0, 1.35, 1.0, lip=0.26, sides=10)
    # ---- 9 the blade's collar (ricasso) and 10 the two small barbs near the base, like a leaf's stem
    rc = w.add(m, "ricasso", "bronze", base=(60, 150, 160))
    ml.loft(rc, "y", (0, 0, 0), [D_BLADE[0] - 0.6, D_BLADE[0] + 1.2], ml.octagon(6.4, 1.6, 0.5))

    atlas = ml.Atlas(w9.ATLAS_PX)
    atlas.layout(m)
    atlas.paint(m)

    def teal(c):                                                    # the blade: deep teal at the point, lighter, wet-looking near the base; faint glow on the edges
        t = np.clip(c.T, 0, 1)[..., None] if c.kind == "loft" else 0.5
        c.img[..., :3] = c.img[..., :3] * 0.8 + np.array([20, 80, 96], np.float32) * 0.3 * (1 - t) + np.array([90, 230, 236], np.float32) * 0.18 * t
        edge = (np.abs(c.S - 0.5) > 0.42)
        w.set_glow(c, (60, 220, 230), 0.22, mask=edge, core=0.2)
    w.paint_part(atlas, m.part("blade"), teal)
    w.paint_part(atlas, m.part("rune_strip_f"), lambda c: w.set_glow(c, (80, 255, 236), 1.0, core=0.5))
    w.paint_part(atlas, m.part("rune_strip_b"), lambda c: w.set_glow(c, (80, 255, 236), 1.0, core=0.5))
    w.paint_part(atlas, m.part("pommel_crystal"), lambda c: w.set_glow(c, (200, 225, 255), 0.5, core=0.4))
    w.paint_part(atlas, m.part("blade_seam"), lambda c: w.set_glow(c, (40, 180, 190), 0.25, core=0.1))
    r = w9.ref("p2/Relics of the world part 2/6f0877aec288f287cb07fee891dd8c7d.jpg", (20, 0, 290, 736), "druidic_blade")
    w9.finish("druidic_blade", m, atlas, real_m=0.72, glow=("FFFFFF", 0.14, 0.5, 1.0), ref_png=r)


# ================================================================ Twin Swords of Deflection
# 1 measure. Card 736 x 736 (p2/6fde728e...): the hilts are at the lower corners (pommel shells at (110, 485) and (610, 500)), each blade runs in from its guard,
# crosses the other and hooks over in a big S. One sword's centre line is about 1.5 times its hilt-to-guard run; a short sword of 85 cm overall is the target.
T_BLADE_SCALE = 0.7                                # the card's blade is drawn far longer than a short sword: the S is kept, its length scaled
T_PATH_A = [(0.0, 0.0), (0.0, 20.0), (13.0, 22.0), (13.0, 40.0)]            # the first bend of the S (cm, x across, y along; blade base at the origin)
T_PATH_B = [(13.0, 40.0), (13.0, 54.0), (24.0, 58.0), (28.0, 46.0)]          # the hook: up, over and out, the tip flaring away from the other sword (the card)
T_BLADE_W = w.lerp_table([(0.0, 3.4), (0.15, 4.4), (0.5, 4.8), (0.8, 3.4), (0.95, 1.6), (1.0, 0.0)])    # width by share of the length (card: 40 px at the base, 55 px mid)
T_BLADE_T = 0.8
T_INLAY_W = 1.9                                    # the lavender scalloped band, card 25 px
T_GRIP = (-17.0, -4.6)                             # grip run (card: dark navy, 105 px)
T_GUARD_Y, T_GUARD_HALF = -3.4, 7.2                # the guard plate with the swept quillon
T_POMMEL = (-20.0, -16.0)                          # the round scalloped shell, about 4 cm
T_SPLAY = 5.0                                      # each sword leans this many degrees outward
T_GAP_Z = 1.7                                      # the two swords are this far apart in depth


def _t_path():
    pts = [(x * T_BLADE_SCALE, y * T_BLADE_SCALE, 0.0) for (x, y, _z) in w9.bez3((*T_PATH_A[0], 0), (*T_PATH_A[1], 0), (*T_PATH_A[2], 0), (*T_PATH_A[3], 0), 14)]
    pts += [(x * T_BLADE_SCALE, y * T_BLADE_SCALE, 0.0) for (x, y, _z) in w9.bez3((*T_PATH_B[0], 0), (*T_PATH_B[1], 0), (*T_PATH_B[2], 0), (*T_PATH_B[3], 0), 14)[1:]]
    return [(p[0], p[1], 0.0) for p in pts]


def _t_sword(sfx):
    """One of the pair, built on its own axis; returns the parts (names end in sfx)."""
    out = []
    path = _t_path()
    n = len(path)
    ts = [i / (n - 1) for i in range(n)]
    bl = ml.Part("blade" + sfx, material="steel", base=(222, 226, 240), smooth=40, uv_mode="loft")
    sc = [(max(0.0, T_BLADE_W(t)), T_BLADE_T * (1.0 if t < 0.93 else 0.5)) for t in ts]
    sc[-1] = (0.0, 0.0)
    ml.sweep(bl, path, ml.lozenge(1.0, 1.0, 0.3), scales=sc)
    out.append(bl)
    inl = ml.Part("inlay" + sfx, material="cloth", base=(122, 108, 196), wear=0.2, uv_mode="loft")
    ip = [(x, y, T_BLADE_T * 0.38) for (x, y, _z) in path[1:-3]]
    ml.sweep(inl, ip, ml.rect_ring(1.0, 1.0), scales=lambda t: (T_INLAY_W * max(0.2, T_BLADE_W(t * 0.9 + 0.05) / 4.4), 0.14))
    out.append(inl)
    sc2 = ml.Part("scallops" + sfx, material="silver", base=(236, 238, 250), smooth=60)
    for i in range(3, n - 3, 2):
        x, y, _z = path[i]
        tx, ty = path[i + 1][0] - path[i - 1][0], path[i + 1][1] - path[i - 1][1]
        ln = math.hypot(tx, ty) or 1.0
        nx, ny = ty / ln, -tx / ln                            # to the outside of the curve (the card's scalloped edge)
        half = T_BLADE_W(ts[i]) * 0.5
        ml.stud(sc2, (x + nx * half * 0.92, y + ny * half * 0.92, 0.0), 0.28, 0.28, (nx, ny, 0.0), sides=6)
    out.append(sc2)
    g = ml.Part("guard" + sfx, material="iron", base=(150, 154, 168), smooth=45, uv_mode="loft")
    gp = [(x, T_GUARD_Y - 0.055 * x * x, 0.0) for x in np.linspace(-T_GUARD_HALF, T_GUARD_HALF, 13)]
    gs = [(1.0 - 0.65 * (abs(x) / T_GUARD_HALF) ** 2.5, 1.0 - 0.5 * (abs(x) / T_GUARD_HALF) ** 2.5) for (x, _y, _z) in gp]
    ml.sweep(g, gp, ml.ellipse(0.8, 0.6, 8), scales=gs)
    out.append(g)
    gpl = ml.Part("guard_plate" + sfx, material="iron", base=(170, 174, 188), smooth=50)
    ml.loft(gpl, "y", (0, 0, 0), [(T_GUARD_Y - 1.6, 0.8, 0.9), (T_GUARD_Y, 1.0, 1.0), (T_GUARD_Y + 1.8, 0.7, 0.8)], ml.octagon(4.2, 1.5, 0.5))
    out.append(gpl)
    gr = ml.Part("grip" + sfx, material="leather", base=(34, 38, 84), uv_mode="loft")
    ml.loft(gr, "y", (0, 0, 0), [(T_GRIP[0], 0.95, 0.95), (T_GRIP[0] + 4, 1.2, 1.2), (T_GRIP[1], 1.05, 1.05)], ml.circle(1.0, 10))
    out.append(gr)
    pm = ml.Part("pommel" + sfx, material="silver", base=(196, 200, 214), smooth=50)
    ml.loft(pm, "y", (0, 0, 0), [(T_POMMEL[0], 0.0, 0.0), (T_POMMEL[0] + 0.6, 1.2, 1.2), (T_POMMEL[0] + 1.8, 2.1, 2.1), (T_POMMEL[0] + 3.0, 2.3, 2.3), (T_POMMEL[1], 1.2, 1.2)], w.scallop(1.0, 10, 0.12, 40))
    out.append(pm)
    return out


def build_twin_swords_of_deflection():
    m = ml.Mesh("twin_swords_of_deflection")
    for part in _t_sword("_a"):
        m.add(part.rotated("z", T_SPLAY).translated(-1.0, 0.0, T_GAP_Z / 2))
    for part in _t_sword("_b"):
        b = part.mirrored("x").rotated("z", -T_SPLAY).translated(1.0, 0.0, -T_GAP_Z / 2)
        m.add(b)
    atlas = ml.Atlas(w9.ATLAS_PX)
    atlas.layout(m)
    atlas.paint(m)

    def lav(c):                                                     # the inlay: a scalloped lace in lighter lavender on the band, glowing softly
        n = w.noise2(c.img.shape[:2], 5, 3 + c.g.rect[0])
        c.img[..., :3] = c.img[..., :3] * (0.8 + 0.5 * n[..., None])
        w.set_glow(c, (170, 150, 255), 0.35, mask=(n > 0.5), core=0.2)
    for nm in ("inlay_a", "inlay_b"):
        w.paint_part(atlas, m.part(nm), lav)

    def blade(c):                                                   # white steel with a bright edge
        edge = (np.abs(c.S - 0.5) > 0.44)
        c.img[..., :3] = np.where(edge[..., None], np.minimum(255, c.img[..., :3] * 1.15), c.img[..., :3])
        w.set_glow(c, (190, 180, 255), 0.18, mask=edge, core=0.3)
    for nm in ("blade_a", "blade_b"):
        w.paint_part(atlas, m.part(nm), blade)
    r = w9.ref("p2/Relics of the world part 2/6fde728ebde26f0db7f226ac7424e2f6.jpg", (20, 190, 720, 560), "twin_swords_of_deflection")
    w9.finish("twin_swords_of_deflection", m, atlas, real_m=0.9, glow=("FFFFFF", 0.14, 0.5, 1.0), ref_png=r)


# ================================================================ Null Blade
# 1 measure. Card 540 x 699 (p3/7074c8f9...): the black greatsword stands upright in the art panel, pommel top y 5, the crossguard's red eye at y 160, the wide shoulder
# plate under it to y 263, then the blade running down out of frame (the card cuts it at the bottom of the panel). The art is cropped, so the proportions of a
# greatsword are kept and the visible part (hilt, cross, shoulder) read off: 340 px for pommel to the start of the blade proper.
N_TOTAL_CM = 150.0                                 # a greatsword
N_PX_HILT = 263.0                                  # card px from pommel to the shoulder plate's end
NK = 90.0 / N_PX_HILT                              # cm per card px: the visible hilt part (pommel to shoulder end) is taken as 90 cm
N_POMMEL = (0.0, 9.0)                              # the stepped pommel cap and its spike (card y 5 to 30)
N_GRIP = (9.0, 41.0)                               # the black wrapped grip, card y 30 to 120, with a ring at y 105
N_RING = (39.0, 43.0)
N_CROSS_Y, N_CROSS_HALF = 48.0, 22.0               # the horns: tip to tip 168 px in the art (stylised wide), 44 cm here
N_EYE_R = 4.2                                      # the red eye in the cross: 60 px across in the art
N_SHOULDER = (52.0, 88.0)                          # the wide filigreed shoulder plate under the cross, to card y 263
N_SHOULDER_W = 11.0                                # 40 px wide in the art next to a 168 px cross
N_BLADE = (86.0, 150.0)                            # the blade proper, tapering from 9 cm to the point
N_BLADE_W = w.lerp_table([(0.0, 9.0), (0.15, 9.4), (0.6, 8.0), (0.88, 5.0), (0.97, 2.0), (1.0, 0.0)])
N_BLADE_T = 1.5
N_TEETH = 6                                        # jagged spikes per edge along the upper blade


def build_null_blade():
    m = ml.Mesh("null_blade")
    L = N_BLADE[1] - N_BLADE[0]
    # ---- 1 the blade: black, lozenge section with a flat central band for the silver filigree
    bl = w.add(m, "blade", "iron", base=(24, 22, 34), smooth=40, uv_mode="loft")
    ts = [0.0, 0.1, 0.3, 0.55, 0.75, 0.88, 0.96, 1.0]
    st = [(N_BLADE[0] + L * t, N_BLADE_W(t), N_BLADE_T * (1.0 if t < 0.9 else 0.5)) for t in ts]
    st[-1] = (st[-1][0], 0.0, 0.0)
    ml.loft(bl, "y", (0, 0, 0), st, ml.lozenge(1.0, 1.0, 0.45))
    # ---- 2 the jagged teeth along both edges of the upper blade, each a thorn leaning toward the point
    th = w.add(m, "blade_teeth", "iron", base=(40, 36, 52), smooth=60)
    for sgn in (-1, 1):
        for i in range(N_TEETH):
            y = N_BLADE[0] + 4.0 + i * (L * 0.5 / N_TEETH)
            half = N_BLADE_W((y - N_BLADE[0]) / L) * 0.5
            w9.spike(th, (sgn * half * 0.96, y, 0.0), (sgn * 0.55, 0.83, 0.0), 3.4 - i * 0.28, 0.62, sides=5, belly=0.7)
    # ---- 3 the silver filigree down both faces: a raised centre ridge pair and a trailing vine of short curls
    fi = w.add(m, "filigree", "silver", base=(176, 190, 206), wear=0.9)
    for sgn in (1, -1):
        ml.ridge_pair(fi, (0, N_BLADE[0] + 3.0, sgn * N_BLADE_T * 0.40), (0, N_BLADE[1] - 8.0, sgn * N_BLADE_T * 0.16), (0, 0, sgn), 0.9, 0.18, 0.08, 3.0)
        for k in range(5):
            y = N_BLADE[0] + 8.0 + k * 9.0
            ml.bar(fi, (-1.9, y, sgn * N_BLADE_T * 0.42), (-0.5, y + 3.2, sgn * N_BLADE_T * 0.40), 0.32, 0.1, up=(0, 0, sgn), taper=0.6)
            ml.bar(fi, (1.9, y, sgn * N_BLADE_T * 0.42), (0.5, y + 3.2, sgn * N_BLADE_T * 0.40), 0.32, 0.1, up=(0, 0, sgn), taper=0.6)
    # ---- 4 the shoulder plate: a broad black ricasso, pinched at both ends like the art's cross-shaped plate
    sh = w.add(m, "shoulder", "iron", base=(20, 18, 30), smooth=45, uv_mode="loft")
    ml.loft(sh, "y", (0, 0, 0), [(N_SHOULDER[0], 0.35, 0.8), (N_SHOULDER[0] + 6, 0.9, 1.0), (N_SHOULDER[0] + 22, 1.0, 1.1), (N_SHOULDER[1] - 6, 0.75, 1.0), (N_SHOULDER[1], 0.55, 0.9)],
            ml.octagon(N_SHOULDER_W, 2.6, 1.0))
    # ---- 5 the crossguard: two horns sweeping out and drooping, thick at the eye and razor-thin at the tips
    cb = w.add(m, "crossguard", "iron", base=(28, 26, 40), smooth=45, uv_mode="loft")
    pts = [(x, N_CROSS_Y - 0.0125 * x * x + 0.6 * math.sin(abs(x) * 0.28), 0.0) for x in np.linspace(-N_CROSS_HALF, N_CROSS_HALF, 17)]
    sc = [(6.4 * (1.0 - 0.94 * (abs(x) / N_CROSS_HALF) ** 1.5) + 0.25, 2.8 * (1.0 - 0.9 * (abs(x) / N_CROSS_HALF) ** 1.2) + 0.2) for (x, _y, _z) in pts]
    ml.sweep(cb, pts, ml.lozenge(1.0, 1.0, 0.3), scales=sc)
    # ---- 6 the lower thorns: a second, smaller pair curling out and down below the cross
    lt = w.add(m, "lower_thorns", "iron", base=(36, 32, 48), smooth=45, uv_mode="loft")
    for sgn in (-1, 1):
        p = [(sgn * x, N_CROSS_Y - 5.5 - 0.02 * x * x * 2.0 - 0.5 * x * 0.3, 0.0) for x in np.linspace(1.5, 13.0, 10)]
        ml.sweep(lt, p, ml.ellipse(1.0, 1.0, 8), scales=lambda t: (2.8 * (1 - 0.92 * t), 1.1 * (1 - 0.9 * t)))
    # ---- 7/8 the eye: a socket ring of black claws and a red glass eye with a slit pupil
    sk = w.add(m, "eye_socket", "iron", base=(30, 28, 44), smooth=55)
    ml.collar(sk, -0.0, N_EYE_R * 1.25, 3.2, lip=0.8, sides=14, axis="z", origin=(0, N_CROSS_Y, -1.6))
    eye = w.add(m, "eye", "crystal", base=(182, 40, 54), smooth=70)
    ml.lathe(eye, "z", (0, N_CROSS_Y, 0.4), [(-0.4, 0.0), (0.0, N_EYE_R * 0.95), (1.2, N_EYE_R * 0.85), (2.2, N_EYE_R * 0.4), (2.6, 0.0)], sides=14)
    # ---- 9 the grip: black leather wound tight
    gp = w.add(m, "grip", "leather", base=(24, 22, 30), uv_mode="loft")
    ml.loft(gp, "y", (0, 0, 0), [(N_GRIP[0], 1.5, 1.5), (N_GRIP[0] + 12, 1.8, 1.8), (N_GRIP[1], 1.6, 1.6)], ml.circle(1.0, 10))
    # ---- 10 the rings: one under the cross like the art's ring, two on the grip
    rg = w.add(m, "grip_rings", "silver", base=(84, 92, 104), wear=0.7)
    ml.collar(rg, N_RING[0], 2.3, N_RING[1] - N_RING[0], lip=0.5, sides=12)
    ml.collar(rg, N_GRIP[0] + 9.0, 2.0, 1.3, lip=0.3, sides=12)
    ml.collar(rg, N_GRIP[0] + 20.0, 2.0, 1.3, lip=0.3, sides=12)
    # ---- 11 the pommel: a stepped cap with a short spike
    pm = w.add(m, "pommel", "iron", base=(32, 30, 44), smooth=45)
    ml.lathe(pm, "y", (0, 0, 0), [(0.0, 0.0), (0.4, 1.4), (2.2, 2.4), (4.0, 2.0), (5.0, 2.4), (6.4, 2.4), (7.4, 1.6), (9.0, 1.5)], sides=12)
    pk = w.add(m, "pommel_spike", "silver", base=(150, 160, 176))
    ml.loft(pk, "y", (0, 0, 0), [(-4.0, 0.0), (-0.2, 0.9), (0.6, 0.5)], ml.circle(1.0, 8))

    atlas = ml.Atlas(w9.ATLAS_PX)
    atlas.layout(m)
    atlas.paint(m)

    def black(c):                                                   # the cold sheen on the black: a faint teal in the edge band, as in the art's rim light
        edge = (np.abs(c.S - 0.5) > 0.4)
        c.img[..., :3] = np.where(edge[..., None], c.img[..., :3] * 0.6 + np.array([40, 90, 96], np.float32) * 0.5, c.img[..., :3])
        w.set_glow(c, (60, 190, 190), 0.16, mask=edge, core=0.1)
    for nm in ("blade", "shoulder", "crossguard", "lower_thorns"):
        w.paint_part(atlas, m.part(nm), black)
    w.paint_part(atlas, m.part("filigree"), lambda c: w.set_glow(c, (150, 230, 230), 0.55, core=0.3))
    w.paint_part(atlas, m.part("eye"), lambda c: w.set_glow(c, (255, 40, 60), 1.0, core=0.35))
    r = w9.ref("p3/Relics of the world part 3/7074c8f96f34fcf0b778a04557d5f169.jpg", (290, 0, 540, 360), "null_blade")
    w9.finish("null_blade", m, atlas, real_m=1.5, glow=("FFFFFF", 0.10, 0.45, 1.0), ref_png=r)
