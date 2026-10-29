"""The canon remakes of the Millennium Key, Scale and Necklace (docs/relics/millennium_canon.md, 'How each one LOOKS'): plain gold, the Eye of Wdjat the only motif, the glow limited to
the pupil of that eye. No lapis, no beads, no orbiting motes, halos, wheels or light shafts. The old builders of items_c.py stay untouched (rule 2); items_c.py registers these.

Frame as everywhere in wave 101: model pixels, Y up, the front of the wearer is -Z. Every part uses the TRIM slot (the gold of the palette); only the pupil takes the GLOW slot.
"""
import math

import numpy as np

from common import geo, K, PT, MAIN, TRIM, GLOW, GOLD, GOLD_HI, GOLD_DK, deco_wdjat  # noqa: F401


def wdjat_gold(vm, group, centre, w, name="wdjat", glow_rgb=None):
    """The Eye of Wdjat in relief, all gold: a rim round the almond, a dark-gold iris disc, a lit pupil, the brow, the falcon's tear and its curl. Faces -Z, built in the plane through
    `centre`. Returns the dict of parts."""
    c = np.asarray(centre, float)
    h = w * 0.42
    r_tube = max(0.11, w * 0.07)
    rim = vm.P(name + "_rim", "gold", group=group, base=GOLD_HI, slot=TRIM, lod=False)
    loop = [(x, y, 0.0) for (x, y) in _almond(w, h, vm.S(12, 5), 1.0)] + [(x, y, 0.0) for (x, y) in _almond(w, h * 0.8, vm.S(12, 5), -1.0)][::-1]
    geo.tube_path(rim, [tuple(np.array(p) + c) for p in loop], r_tube, sides=vm.S(4, 3), caps=(True, True))
    iris = vm.P(name + "_iris", "gold", group=group, base=GOLD_DK, slot=TRIM, lod=False)
    prof = [(-0.12, w * 0.33), (0.12, w * 0.32), (0.3, w * 0.24), (0.42, w * 0.1), (0.46, 0.0)]
    geo.merge(iris, geo.prim_lathe(prof, sides=vm.S(12, 6), caps=(True, False)), geo.rot_matrix("x", -90), c)
    pupil = vm.P(name + "_pupil", "crystal", group=group, base=glow_rgb or (255, 236, 160), slot=GLOW, lod=False)
    geo.merge(pupil, geo.prim_octa(w * 0.13, w * 0.17), geo.rot_matrix("x", -90), c + np.array([0.0, 0.0, -0.5]))
    brow = vm.P(name + "_brow", "gold", group=group, base=GOLD_HI, slot=TRIM, lod=False)
    bp = [(-w * 1.05 + 2.1 * w * t, h * 1.7 + h * 0.5 * math.sin(math.pi * t), 0.0) for t in np.linspace(0, 1, vm.S(10, 4))]
    geo.tube_path(brow, [tuple(np.array(p) + c) for p in bp], r_tube * 0.9, sides=vm.S(4, 3), caps=(True, True))
    tear = vm.P(name + "_tear", "gold", group=group, base=GOLD_HI, slot=TRIM, lod=False)
    drop = [(-w * 0.12, -h * 0.9, 0.0), (-w * 0.12, -h * 1.9, 0.0), (-w * 0.12, -h * 2.4, 0.0)]
    geo.tube_path(tear, [tuple(np.array(p) + c) for p in drop], r_tube * 0.9, sides=vm.S(4, 3), caps=(True, True))
    sp = [(w * 0.5 + h * 0.9 * math.cos(a) * (1 - a / 14.0), -h * 2.3 + h * 0.9 * math.sin(a) * (1 - a / 14.0), 0.0) for a in np.linspace(0, 8.0, vm.S(14, 6))]
    geo.tube_path(tear, [tuple(np.array(p) + c) for p in sp], r_tube * 0.8, sides=vm.S(4, 3), caps=(True, True))
    parts = {"rim": rim, "iris": iris, "pupil": pupil, "brow": brow, "tear": tear}
    vm.deco(PT.deco_fill_glow, pupil, (255, 236, 150) if glow_rgb is None else glow_rgb, strength=1.0, seed=1)
    vm.deco(PT.deco_grain, rim, amount=0.1)
    return parts


def _almond(w, h, n, sign):
    return [(-w + 2 * w * (i / n), sign * h * math.sin(math.pi * i / n) ** 0.9) for i in range(n + 1)]


def _ring(part, c, R, r, vm, plane="xy", squash=1.0, n=12, m=4):
    p = geo.prim_torus(R, r, n=vm.S(n, 6), m=m, plane=plane)
    geo.merge(part, p, np.diag([squash, 1.0, 1.0]) if squash != 1.0 else None, c)


# ====================================================================== MILLENNIUM KEY (belt): a flat golden ankh hung on a thin gold cord belt
def build_key_canon(vm, info):
    glow = K.rgb(info[4][2])
    BY, SQ = -10.8, 0.56                                              # belt line, oval of the waist
    KZ = -3.45                                                        # the key hangs in front of the belt
    g_b = vm.grp("belt", "bob", (0, BY, 0), 0.15, 0.05, 0.0)
    g_key = vm.grp("ankh_key", "swing", (0, BY - 0.9, KZ), 4.0, 0.07, 0.0)
    g_eye = vm.grp("wdjat_glow", "pulse", (0, BY - 7.0, KZ - 0.5), 0.1, 0.14, 0.0)
    # the belt: a thin plain gold cord round the waist with one small loop at the front for the key
    band = vm.P("belt_cord", "gold", group=g_b, base=GOLD_DK, slot=TRIM, lod=False)
    geo.lathe(band, "y", (0, 0, 0), [(BY - 0.4, 4.6), (BY - 0.25, 4.75), (BY + 0.25, 4.75), (BY + 0.4, 4.6)], sides=vm.S(24, 10), caps=(False, False), squash=SQ)
    hang = vm.P("belt_loop", "gold", group=g_b, base=GOLD, slot=TRIM, lod=False)
    geo.merge(hang, geo.prim_box(1.3, 1.5, 0.5, chamfer=0.2), None, (0, BY, -2.85))
    # the ankh: loop (ellipse ribbon), long straight shaft, short crossbar, a little bit at the foot; flat, 0.8 thick
    top = BY - 1.6                                                    # the top of the loop (under the cord ring)
    LH, LW = 4.4, 3.5                                                 # loop outer height, width
    lc = top - LH / 2                                                 # loop centre
    ink = vm.P("ankh_loop", "gold", group=g_key, base=GOLD_HI, slot=TRIM, lod=True)
    n = vm.S(22, 6)
    lp = [(math.sin(a) * (LW / 2 - 0.45), lc + math.cos(a) * (LH / 2 - 0.45), KZ) for a in np.linspace(0.0, 2 * math.pi, n + 1)]
    geo.ribbon(ink, lp, 0.9, thick=0.8, up=(0, 0, -1), taper=0.0)
    cb_y = top - LH + 0.55                                            # the crossbar sits just below the loop
    bottom = top - 11.4
    sh = vm.P("ankh_shaft", "gold", group=g_key, base=GOLD_HI, slot=TRIM, lod=True)
    geo.merge(sh, geo.prim_box(1.5, cb_y - bottom, 0.8, chamfer=0.0 if vm.lod else 0.25), None, (0, (cb_y + bottom) / 2, KZ))
    cb = vm.P("ankh_crossbar", "gold", group=g_key, base=GOLD_HI, slot=TRIM, lod=True)
    geo.merge(cb, geo.prim_box(7.0, 0.95, 0.8, chamfer=0.0 if vm.lod else 0.25), None, (0, cb_y, KZ))
    for sx in (-1, 1):                                                # a small flare at the arm ends
        geo.merge(cb, geo.prim_box(0.7, 1.35, 0.8, chamfer=0.2), None, (sx * 3.45, cb_y, KZ))
    bit = vm.P("key_bit", "gold", group=g_key, base=GOLD_DK, slot=TRIM, lod=False)
    for k, yy in enumerate((bottom + 0.6, bottom + 1.7)):
        geo.merge(bit, geo.prim_box(1.3 - 0.25 * k, 0.55, 0.7), None, (1.35 - 0.1 * k, yy, KZ))
    # the small ring at the very top, for the cord: it passes through the loop of the belt
    ring = vm.P("cord_ring", "gold", group=g_key, base=GOLD_HI, slot=TRIM, lod=False)
    _ring(ring, (0, top + 0.55, KZ), 0.62, 0.17, vm, plane="yz", n=12)
    # the Eye of Wdjat on a small plate in the upper shaft, just under the crossbar
    ey = cb_y - 2.1
    plate = vm.P("eye_plate", "gold", group=g_key, base=GOLD_HI, slot=TRIM, lod=False)
    geo.merge(plate, geo.prim_box(2.0, 2.3, 0.5, chamfer=0.3), None, (0, ey, KZ - 0.05))
    eye = wdjat_gold(vm, g_eye, (0, ey + 0.1, KZ - 0.42), 0.82, name="wdjat", glow_rgb=glow)
    vm.deco(PT.deco_grain, sh, amount=0.1)
    vm.deco(PT.deco_grain, ink, amount=0.1)
    return eye


# ====================================================================== MILLENNIUM SCALE (charm): a golden balance, no feather
def build_scale_canon(vm, info):
    glow = K.rgb(info[4][2])
    g_base = vm.grp("scale_base", "bob", (0, 0, 0), 0.35, 0.05, 0.0)
    g_beam = vm.grp("balance_beam", "sway", (0, 11.0, 0), 2.4, 0.07, 0.0)
    g_eye = vm.grp("wdjat_glow", "pulse", (0, 14.0, -1.0), 0.1, 0.14, 0.0)
    BEAM_Y = 10.4
    BX = 8.6                                                          # half the span between the chain hooks
    # base: a flat round foot and a short step
    foot = vm.P("foot", "gold", group=g_base, base=GOLD, slot=TRIM, lod=True)
    geo.lathe(foot, "y", (0, 0, 0), [(-8.6, 0.0), (-8.6, 5.2), (-8.1, 5.6), (-7.5, 5.0), (-7.2, 3.8), (-6.6, 2.4), (-5.0, 1.6)] if not vm.lod else [(-8.6, 0.0), (-8.6, 5.2), (-7.2, 3.8), (-5.0, 1.6)], sides=vm.S(16, 6), caps=(True, False))
    # the post: a slim turned stem with two rings, up to the beam
    post = vm.P("post", "gold", group=g_base, base=GOLD_HI, slot=TRIM, lod=True)
    geo.lathe(post, "y", (0, 0, 0), [(-5.2, 1.6), (-4.4, 0.95), (-1.0, 0.8), (3.0, 0.7), (7.0, 0.7), (9.4, 0.8), (10.0, 1.1), (11.2, 0.9)] if not vm.lod else [(-5.2, 1.6), (-4.4, 0.95), (7.0, 0.7), (11.2, 0.9)], sides=vm.S(10, 5), caps=(False, False))
    knobs = vm.P("post_rings", "gold", group=g_base, base=GOLD, slot=TRIM, lod=False)
    for yy, r in ((-2.4, 1.15), (1.4, 1.0), (5.2, 1.0)):
        geo.lathe(knobs, "y", (0, 0, 0), [(yy - 0.5, 0.7), (yy - 0.2, r), (yy + 0.2, r), (yy + 0.5, 0.7)], sides=vm.S(10, 6), caps=(False, False))
    # the ornament at the top of the post: a small round gold plate carrying the Eye of Wdjat, over the beam
    orn = vm.P("eye_plate", "gold", group=g_base, base=GOLD, slot=TRIM, lod=True)
    geo.merge(orn, geo.prim_lathe([(-0.2, 1.9), (0.0, 2.05), (0.3, 1.85), (0.5, 0.0)], sides=vm.S(14, 7), caps=(True, False)), geo.rot_matrix("x", -90), (0, 14.0, -0.65))
    fin = vm.P("finial", "gold", group=g_base, base=GOLD_HI, slot=TRIM, lod=False)
    geo.merge(fin, geo.prim_cone(0.55, 1.8, sides=4), None, (0, 16.4, -0.65))
    eye = wdjat_gold(vm, g_eye, (0, 14.1, -1.4), 1.15, name="wdjat", glow_rgb=glow)
    # the beam with a boss at each end, and a hook ring at each end for the chains
    beam = vm.P("beam", "gold", group=g_beam, base=GOLD_HI, slot=TRIM, lod=True)
    geo.merge(beam, geo.prim_box(2 * (BX + 1.0), 0.8, 0.8, chamfer=0.2), None, (0, BEAM_Y, 0))
    geo.merge(beam, geo.prim_box(2.2, 1.3, 1.3, chamfer=0.3), None, (0, BEAM_Y, 0))
    bosses = vm.P("beam_ends", "gold", group=g_beam, base=GOLD, slot=TRIM, lod=False)
    for sx in (-1, 1):
        geo.merge(bosses, geo.prim_octa(0.7, 0.95), None, (sx * (BX + 1.15), BEAM_Y, 0))
        _ring(bosses, (sx * BX, BEAM_Y - 0.7, 0), 0.45, 0.13, vm, plane="xy", n=10)
    # each dish hangs by three short chains from the hook, over a shallow round dish
    DY = 2.0                                                          # the dish rim line
    for sx in (-1, 1):
        px = sx * BX
        ch = vm.P("chains_%d" % sx, "gold", group=g_beam, base=GOLD, slot=TRIM, lod=False)
        for k in range(3):
            a = 2 * math.pi * k / 3 + math.pi / 2
            rx, rz = 3.5 * math.cos(a), 3.5 * math.sin(a)
            geo.tube_path(ch, [(px, BEAM_Y - 1.1, 0.0), (px + rx, DY + 0.2, rz)], 0.14, sides=3, caps=(True, True))
        pan = vm.P("dish_%d" % sx, "gold", group=g_beam, base=GOLD, slot=TRIM, lod=True)
        prof = [(DY - 1.1, 0.0), (DY - 1.0, 1.5), (DY - 0.7, 2.9), (DY - 0.25, 3.7), (DY, 4.0), (DY + 0.22, 3.9), (DY + 0.2, 3.6),
                (DY - 0.2, 3.5), (DY - 0.55, 2.7), (DY - 0.8, 1.4), (DY - 0.85, 0.0)]
        if vm.lod:
            prof = [prof[0], prof[3], prof[4], prof[5], prof[7], prof[10]]
        geo.lathe(pan, "y", (px, 0, 0), prof, sides=vm.S(18, 7), caps=(False, False))
        rim = vm.P("dish_rim_%d" % sx, "gold", group=g_beam, base=GOLD_HI, slot=TRIM, lod=False)
        geo.merge(rim, geo.prim_torus(3.75, 0.2, n=vm.S(18, 8), m=3), None, (px, DY + 0.1, 0))
    vm.deco(PT.deco_grain, post, amount=0.1)
    vm.deco(PT.deco_grain, foot, amount=0.1)
    return eye


# ====================================================================== MILLENNIUM NECKLACE (chest): a thin open gold collar with the eye on a plate
def build_necklace_canon(vm, info):
    glow = K.rgb(info[4][2])
    CZ = -2.95                                                        # the front of the chest
    g_c = vm.grp("collar", "bob", (0, 0, CZ), 0.15, 0.05, 0.0)
    g_eye = vm.grp("wdjat_glow", "pulse", (0, -3.6, CZ - 0.5), 0.1, 0.14, 0.0)
    g_chain = vm.grp("neck_chain", "sway", (0, 0.5, 2.8), 1.5, 0.05, 0.0)
    RX, RY, CY = 4.3, 3.6, 0.5                                        # the torc is a U on the chest: an ellipse arc about (0, CY)
    a0, a1 = math.radians(205), math.radians(335)
    torc = vm.P("collar_torc", "gold", group=g_c, base=GOLD_HI, slot=TRIM, lod=True)
    pts = [(RX * math.cos(t), CY + RY * math.sin(t), CZ) for t in np.linspace(a0, a1, vm.S(20, 8))]
    geo.tube_path(torc, pts, 0.4, sides=vm.S(6, 4), caps=(True, True))
    rope = vm.P("collar_wire", "gold", group=g_c, base=GOLD, slot=TRIM, lod=False)
    pts2 = [(RX * math.cos(t), CY + RY * math.sin(t) - 0.0, CZ - 0.16) for t in np.linspace(a0 + 0.05, a1 - 0.05, vm.S(20, 8))]
    geo.tube_path(rope, pts2, 0.12, sides=3, caps=(True, True))
    # the curled ends: a small outward curl at each end of the torc, a loop where the back chain hooks in
    ends = vm.P("collar_curls", "gold", group=g_c, base=GOLD_HI, slot=TRIM, lod=False)
    for sx in (-1, 1):
        t = a0 if sx < 0 else a1
        ex, ey = RX * math.cos(t), CY + RY * math.sin(t)
        _ring(ends, (ex + sx * 0.5, ey + 0.35, CZ), 0.55, 0.15, vm, plane="xy", n=10)           # the small curl: an open loop at each end, the chain hooks into it
        geo.merge(ends, geo.prim_octa(0.3, 0.34), None, (ex, ey, CZ))
    # the plate at the middle of the U with the Eye of Wdjat
    plate = vm.P("eye_plate", "gold", group=g_c, base=GOLD, slot=TRIM, lod=True)
    py = CY - RY - 0.05
    geo.merge(plate, geo.prim_lathe([(-0.16, 1.6), (0.0, 1.72), (0.28, 1.5), (0.45, 0.0)], sides=vm.S(16, 7), caps=(True, False)), geo.rot_matrix("x", -90), (0, py, CZ - 0.12))
    eye = wdjat_gold(vm, g_eye, (0, py + 0.3, CZ - 0.7), 1.15, name="wdjat", glow_rgb=glow)
    # the thin chain behind the neck, from end to end over the shoulders and the nape
    chain = vm.P("neck_chain", "gold", group=g_chain, base=GOLD, slot=TRIM, lod=False)
    for sx in (-1, 1):
        t = a0 if sx < 0 else a1
        ex, ey = RX * math.cos(t) + sx * 1.0, CY + RY * math.sin(t) + 0.35
        geo.chain(chain, [(ex, ey, CZ), (sx * 4.5, 0.0, -1.3), (sx * 4.4, 0.25, 0.9), (sx * 3.2, 0.3, 2.7), (sx * 1.1, 0.3, 3.0)], link_r=0.42, wire=0.1, every=1.9, n=6, m=3)
    vm.deco(PT.deco_grain, torc, amount=0.1)
    return eye
