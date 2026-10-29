"""The canon remake of three Millennium Items (docs/relics/millennium_canon.md): the Puzzle, the Eye and the Ring, all plain gold with the Eye of Wdjat as the only motif; the one
colour accent is the glowing pupil. The old builders (build_puzzle in items_a, build_eye / build_ring in items_b) stay in the repo, unused by the registry.

Frames as in common.py: model pixels, Y up, the front of the wearer is -Z; body piece relative to the neck line, head piece to the neck under the head (head box y 0..8), arm piece to the
shoulder pivot (built for the right arm, mirrored for the left).
"""
import math

import numpy as np

from common import (geo, K, PT, MAIN, TRIM, GLOW, GOLD, GOLD_HI, GOLD_DK)


def _tp(path, M, c):
    return [tuple(M @ np.array(p, float) + c) for p in path]


def wdjat_canon(vm, group, centre, w, M=None, tail=1.0, lod=False, name="wdjat", glow_rgb=(255, 236, 160), pupil_scale=1.0):
    """The Eye of Wdjat in gold relief facing -Z in the plane through `centre` (M turns it onto a slanted face): the almond, a raised iris, a glowing pupil, the brow, the tear line and
    its curl (`tail` scales the tear and the curl). Returns the parts."""
    M = np.eye(3) if M is None else M
    c = np.asarray(centre, float)
    h = w * 0.42
    rt = max(0.15, w * 0.1)
    rim = vm.P(name + "_rim", "gold", group=group, base=GOLD_HI, slot=TRIM, lod=False)
    n = vm.S(12, 5)
    loop = [(x, y, 0.0) for (x, y) in
            [(-w + 2 * w * t / n, h * math.sin(math.pi * t / n) ** 0.9) for t in range(n + 1)]]
    loop += [(x, -h * 0.8 * (math.sin(math.pi * (1 - (x + w) / (2 * w))) ** 0.9), 0.0)
             for x in [w - 2 * w * t / n for t in range(1, n)]]
    loop.append(loop[0])
    geo.tube_path(rim, _tp(loop, M, c), rt, sides=vm.S(4, 3), caps=(True, True))
    iris = vm.P(name + "_iris", "gold", group=group, base=GOLD, slot=TRIM, lod=False)
    prof = [(-0.1, w * 0.36), (0.12, w * 0.35), (0.3, w * 0.28), (0.36, w * 0.12), (0.38, 0.0)]
    geo.merge(iris, geo.prim_lathe(prof, sides=vm.S(14, 6), caps=(True, False)), M @ geo.rot_matrix("x", -90), c + M @ np.array([0.0, 0.0, -0.05]))
    pupil = vm.P(name + "_pupil", "crystal", group=group, base=glow_rgb, slot=GLOW, lod=False)
    pr = w * 0.17 * pupil_scale
    pp = [(0.0, pr), (0.14, pr * 0.92), (0.3, pr * 0.55), (0.38, 0.0)]
    geo.merge(pupil, geo.prim_lathe(pp, sides=vm.S(10, 5), caps=(True, False)), M @ geo.rot_matrix("x", -90), c + M @ np.array([0.0, 0.0, -0.36]))
    brow = vm.P(name + "_brow", "gold", group=group, base=GOLD_HI, slot=TRIM, lod=False)
    bp = [(-w * 1.05 + 2.1 * w * t, h * 1.7 + h * 0.5 * math.sin(math.pi * t), 0.0) for t in np.linspace(0, 1, vm.S(10, 4))]
    geo.tube_path(brow, _tp(bp, M, c), rt, sides=vm.S(4, 3), caps=(True, True))
    tear = vm.P(name + "_tear", "gold", group=group, base=GOLD_HI, slot=TRIM, lod=False)
    drop = [(-w * 0.12, -h * 0.8, 0.0), (-w * 0.12, -h * (0.8 + 1.3 * tail), 0.0)]
    geo.tube_path(tear, _tp(drop, M, c), rt, sides=vm.S(4, 3), caps=(True, True))
    sr = h * 0.95 * tail
    sy = -h * (0.8 + 1.1 * tail)
    sp = [(w * 0.42 + sr * math.cos(a) * (1 - a / 12.0), sy + sr * math.sin(a) * (1 - a / 12.0), 0.0) for a in np.linspace(0.6, 8.0, vm.S(14, 6))]
    geo.tube_path(tear, _tp(sp, M, c), rt * 0.9, sides=vm.S(4, 3), caps=(True, True))
    vm.deco(PT.deco_fill_glow, pupil, glow_rgb, strength=1.0, seed=1)
    vm.deco(PT.deco_grain, iris, amount=0.1)
    return {"rim": rim, "iris": iris, "pupil": pupil, "brow": brow, "tear": tear}


# ====================================================================== MILLENNIUM PUZZLE (neck)
def build_puzzle_canon(vm, info):
    """An inverted gold pyramid (flat square top, a point below), a loop and a thin gold chain over the neck, the Eye of Wdjat in relief on the front face, raised ridges along the corner
    edges and engraved ledges round the faces."""
    pal = info[4]
    glow = K.rgb(pal[2])
    g_p = vm.grp("puzzle", "bob", (0, -3.0, -4.0), 0.25, 0.04, 0.0)
    g_eye = vm.grp("wdjat_glow", "pulse", (0, -4.7, -4.9), 0.1, 0.1, 0.0)
    g_chain = vm.grp("chain", "sway", (0, 0.5, 0.0), 1.2, 0.05, 0.0)
    CZ, YT = -4.0, -2.7
    W, D, H, CH = 6.0, 3.6, 6.2, 0.5
    sec = geo.octagon(W, D, CH)

    def sc(s):
        return max(0.05, 1.0 + s / H)

    body = vm.P("pyramid", "gold", group=g_p, base=GOLD, slot=TRIM, lod=True)
    geo.loft(body, "y", (0, YT, CZ), [(-H, 0.05, 0.05), (0.0, 1.0, 1.0)], sec, caps=(True, True))
    lid = vm.P("top_lid", "gold", group=g_p, base=GOLD_HI, slot=TRIM, lod=True)
    geo.merge(lid, geo.prim_box(W + 0.5, 0.45, D + 0.5, chamfer=0.45), None, (0, YT + 0.12, CZ))
    frame = vm.P("top_frame", "gold", group=g_p, base=GOLD_DK, slot=TRIM, lod=False)
    fx, fz = W / 2 - 0.6, D / 2 - 0.5
    geo.tube_path(frame, [(-fx, YT + 0.4, CZ - fz), (fx, YT + 0.4, CZ - fz), (fx, YT + 0.4, CZ + fz), (-fx, YT + 0.4, CZ + fz), (-fx, YT + 0.4, CZ - fz)], 0.13, sides=3, caps=(True, True))
    # the ridges along the four corner edges, and the ledges round the faces
    ridge = vm.P("ridges", "gold", group=g_p, base=GOLD_HI, slot=TRIM, lod=False)
    for sx in (-1, 1):
        for sz in (-1, 1):
            geo.tube_path(ridge, [(sx * (W / 2 - CH / 2), YT, CZ + sz * (D / 2 - CH / 2)), (sx * 0.12, YT - H + 0.3, CZ + sz * 0.08)], 0.2, sides=vm.S(4, 3), taper=(1.0, 0.7), caps=(True, True))
    ledge = vm.P("ledges", "gold", group=g_p, base=GOLD_DK, slot=TRIM, lod=False)
    for s0, hh in ((-0.55, 0.22), (-4.3, 0.2)):
        geo.loft(ledge, "y", (0, YT, CZ), [(s0 - hh, sc(s0 - hh) * 1.045, sc(s0 - hh) * 1.045), (s0 + hh, sc(s0 + hh) * 1.045, sc(s0 + hh) * 1.045)], sec, caps=(False, False))
    # the loop on the top edge, and the chain over the neck
    loop = vm.P("loop", "gold", group=g_p, base=GOLD_HI, slot=TRIM, lod=True)
    geo.merge(loop, geo.prim_torus(0.8, 0.24, n=vm.S(14, 6), m=4, plane="xy"), None, (0, YT + 0.35 + 0.8, CZ))
    collar = vm.P("loop_collar", "gold", group=g_p, base=GOLD, slot=TRIM, lod=False)
    geo.merge(collar, geo.prim_box(1.1, 0.5, 0.8, chamfer=0.15), None, (0, YT + 0.3, CZ))
    chain = vm.P("chain", "gold", group=g_chain, base=GOLD_HI, slot=TRIM, lod=False)
    yr = YT + 0.35 + 1.6
    for sx in (-1, 1):
        path = geo.bez3((0.0, yr, CZ), (sx * 1.6, yr + 1.4, CZ + 0.4), (sx * 4.4, 0.8, -2.8), (sx * 3.7, 0.5, 0.0), vm.S(7, 3))
        path += geo.bez3((sx * 3.7, 0.5, 0.0), (sx * 3.4, 0.4, 1.6), (sx * 2.6, 0.5, 2.7), (0.0, 0.5, 2.9), vm.S(6, 3))[1:]
        geo.tube_path(chain, path, 0.14, sides=3, caps=(True, True))
    # the Eye of Wdjat on the slanted front face
    s_eye = -2.15
    zf = CZ - (D / 2) * sc(s_eye)
    slant = math.degrees(math.atan((D / 2) / H))
    wdjat_canon(vm, g_eye, (0, YT + s_eye, zf - 0.12), 1.12, M=geo.rot_matrix("x", -slant), tail=0.8, glow_rgb=glow)
    vm.deco(PT.deco_grain, body, amount=0.14)
    vm.deco(PT.deco_grain, lid, amount=0.1)


# ====================================================================== MILLENNIUM EYE (head)
def build_eye_canon(vm, info):
    """A flat round gold disc with a raised rim, a raised eyeball with a glowing pupil, the brow and tear flourish and two little curls on top, worn on the forehead on a thin gold band
    round the head."""
    pal = info[4]
    glow = K.rgb(pal[2])
    g_c = vm.grp("circlet", "bob", (0, 8, 0), 0.18, 0.04, 0.0)
    g_eye = vm.grp("wdjat_glow", "pulse", (0, 6.3, -5.1), 0.1, 0.12, 0.0)
    YC, ZB = 6.3, -4.5                     # the disc centre height, and the outer face of the band at the brow
    # the thin band round the head (a rounded square, so the corners of the head box stay inside)
    band = vm.P("band", "gold", group=g_c, base=GOLD, slot=TRIM, lod=True)
    geo.loft(band, "y", (0, 0, 0), [(YC - 0.55, 0.97, 0.97), (YC - 0.42, 1.0, 1.0), (YC + 0.42, 1.0, 1.0), (YC + 0.55, 0.97, 0.97)], geo.octagon(9.0, 9.0, 1.3), caps=(False, False))
    edge = vm.P("band_edges", "gold", group=g_c, base=GOLD_HI, slot=TRIM, lod=False)
    for yy in (YC - 0.5, YC + 0.5):
        geo.loft(edge, "y", (0, 0, 0), [(yy - 0.07, 1.01, 1.01), (yy + 0.07, 1.01, 1.01)], geo.octagon(9.0, 9.0, 1.3), caps=(False, False))
    # the disc on the brow, facing forward
    R = 2.25
    face = geo.rot_matrix("x", -90)                       # the +y axis of a lathe turned onto -z (forward)
    disc = vm.P("disc", "gold", group=g_c, base=GOLD, slot=TRIM, lod=True)
    prof = [(-0.1, R * 0.97), (0.05, R), (0.3, R), (0.38, R - 0.3), (0.38, 0.0)]
    geo.merge(disc, geo.prim_lathe(prof, sides=vm.S(24, 10), caps=(True, False)), face, (0, YC, ZB - 0.05))
    rim = vm.P("disc_rim", "gold", group=g_c, base=GOLD_HI, slot=TRIM, lod=False)
    geo.merge(rim, geo.prim_torus(R - 0.2, 0.26, n=vm.S(26, 10), m=4, plane="xy"), None, (0, YC, ZB - 0.4))
    field = vm.P("disc_field", "gold", group=g_c, base=GOLD_DK, slot=TRIM, lod=False)
    geo.merge(field, geo.prim_lathe([(0.0, R - 0.45), (0.1, R - 0.55), (0.1, 0.0)], sides=vm.S(20, 8), caps=(True, False)), face, (0, YC, ZB - 0.4))
    # the two little curls on top of the disc
    curl = vm.P("curls", "gold", group=g_c, base=GOLD_HI, slot=TRIM, lod=False)
    for sx in (-1, 1):
        geo.merge(curl, geo.prim_torus(0.42, 0.12, n=vm.S(10, 5), m=4, plane="xy"), None, (sx * 0.75, YC + R + 0.25, ZB - 0.15))
    # the Eye of Wdjat on the disc
    wdjat_canon(vm, g_eye, (0, YC + 0.25, ZB - 0.5), 1.25, tail=0.62, glow_rgb=glow)
    vm.deco(PT.deco_grain, disc, amount=0.1)
    vm.deco(PT.deco_grain, band, amount=0.08)


# ====================================================================== MILLENNIUM RING (hands: built for the right arm, worn on the left through the mirror)
def build_ring_canon(vm, info):
    """A golden hoop with an inverted pyramid carrying the Eye of Wdjat inside it, a small loop on top, five needle spikes hanging from the lower hoop (the centre one longest), hung on a thin
    gold wrist band."""
    pal = info[4]
    glow = K.rgb(pal[2])
    AX = -1.0
    g_a = vm.grp("ring_arm", "swing", (0, 0, 0), 0.0, 0.0, 0.0)
    g_need = vm.grp("needles", "sway", (AX, -8.0, -4.4), 5.0, 0.12, 0.0)
    g_eye = vm.grp("ring_glow", "pulse", (AX, -6.4, -4.7), 0.1, 0.12, 0.0)
    CZ, YC, R = -4.4, -6.1, 2.1
    # the thin wrist band
    cuff = vm.P("cuff", "gold", group=g_a, base=GOLD, slot=TRIM, lod=False)
    geo.lathe(cuff, "y", (AX, 0, 0), [(-2.0, 3.1), (-2.12, 3.2), (-2.78, 3.2), (-2.9, 3.1)], sides=vm.S(16, 8), caps=(False, False))
    # the hoop, its loop, and the little link to the band
    hoop = vm.P("hoop", "gold", group=g_a, base=GOLD_HI, slot=TRIM, lod=True)
    geo.merge(hoop, geo.prim_torus(R, 0.32, n=vm.S(26, 10), m=5, plane="xy"), None, (AX, YC, CZ))
    lp = vm.P("hoop_loop", "gold", group=g_a, base=GOLD_HI, slot=TRIM, lod=False)
    geo.merge(lp, geo.prim_torus(0.5, 0.15, n=vm.S(12, 6), m=4, plane="xy"), None, (AX, YC + R + 0.5, CZ))
    link = vm.P("link", "gold", group=g_a, base=GOLD, slot=TRIM, lod=False)
    geo.tube_path(link, [(AX, YC + R + 0.95, CZ), (AX, -2.6, -3.7), (AX, -2.5, -3.0)], 0.1, sides=3, caps=(True, True))
    # the inverted pyramid inside the hoop, point down, its corners on the hoop
    ya, yb, hw = YC + 1.1, YC - 1.7, 1.45
    pyr = vm.P("pyramid", "gold", group=g_a, base=GOLD, slot=TRIM, lod=True)
    geo.loft(pyr, "y", (AX, ya, CZ), [(yb - ya, 0.04, 0.04), (0.0, 1.0, 1.0)], geo.octagon(2 * hw, 0.9, 0.22), caps=(True, True))
    pfr = vm.P("pyramid_edge", "gold", group=g_a, base=GOLD_HI, slot=TRIM, lod=False)
    geo.tube_path(pfr, [(AX - hw, ya, CZ - 0.5), (AX, yb + 0.1, CZ - 0.5), (AX + hw, ya, CZ - 0.5), (AX - hw, ya, CZ - 0.5)], 0.13, sides=3, caps=(True, True))
    wdjat_canon(vm, g_eye, (AX, YC - 0.05, CZ - 0.5), 0.72, tail=0.45, glow_rgb=glow)
    # the five needles, hung from the lower hoop: the middle one longest, the outer ones shorter, tapering to points
    nd = vm.P("needles", "gold", group=g_need, base=GOLD_HI, slot=TRIM, lod=True)
    bead = vm.P("needle_caps", "gold", group=g_need, base=GOLD, slot=TRIM, lod=False)
    for k, ln in zip(range(-2, 3), (2.0, 2.8, 3.8, 2.8, 2.0)):
        a = math.radians(k * 32)
        base = np.array([AX + R * math.sin(a), YC - R * math.cos(a), CZ])
        geo.merge(bead, geo.prim_sphere(0.34, 3, 6), None, base + np.array([0.0, -0.1, 0.0]))
        geo.merge(nd, geo.prim_spike(0.38, ln, sides=vm.S(5, 3)), geo.rot_matrix("x", 180), base + np.array([0.0, -0.1, 0.0]))
    vm.deco(PT.deco_grain, hoop, amount=0.1)
    vm.deco(PT.deco_grain, pyr, amount=0.1)
