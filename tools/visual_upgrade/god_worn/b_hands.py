"""HANDS relics: the Hand of Vecna, the Aerie courier gloves, the Bloodbound gauntlets, the Incinerator arm.

Arm frame (rarm, y up): origin at the shoulder pivot, the arm hangs from y +2 to -10, its outer side is -x (the left arm is the mirror: outer +x). Everything is authored at the
arm axis x = out * 1.18 (the piece is drawn at 0.85, which puts it back on the arm), a half width of 2.65 clears the skin's sleeve layer, and the hand ends at y -10 (-11.8 authored).
"""
import _paths  # noqa: F401
import math

import numpy as np

import gen_worn_lib as L
import mesh_lib as ml
from mesh_lib import Part, loft, lathe, sweep, circle
from gw_lib import MAIN, TRIM, GLOW, DARK, ACCENT, EMIT, SHELL, HOT, LIT, ADD
import kit as K
from kit import A
from b_back import crest_disc
from b_back2 import veins_overlay

CX = 1.18


def forearm(gs, D, out, y0=-3.6, y1=-10.0, a0=2.9, a1=2.7, tile="plate", slot=MAIN, name="vambrace", flare=0.0, flags=SHELL, lo=0.0):
    """A lofted guard round the forearm from y0 (elbow end) down to y1 (wrist end), ellipse sections a0 -> a1 wide (x) and 0.97 times that deep (z)."""
    p = Part(name)
    ln = y0 - y1
    st = [(0.0, a0, a0 * 0.97), (ln * 0.4, (a0 + a1) / 2 + flare * 0.3, (a0 + a1) / 2 * 0.97), (ln * 0.85, a1 + flare, a1 * 0.97), (ln, a1 + flare * 1.2, a1 * 0.97)]
    loft(p, (0, -1, 0), (out * CX, y0, 0), [(s, 1.0, 1.0) for (s, _x, _z) in st], lambda s: ml.ellipse(np.interp(s, [q[0] for q in st], [q[1] for q in st]), np.interp(s, [q[0] for q in st], [q[2] for q in st]), D.sides(16, 8)),
         caps=(False, False))
    return gs.add(p, slot, tile, flags, 0, "tile", 7.0)


def finger(gs, D, root, direction, lens, r, slot=MAIN, tile="bone", curl=0.35, name="finger", group=0, nail=None, side=(1, 0, 0), flags=SHELL):
    """A finger / claw of joints along direction, curling by `curl` radians a joint toward -z, tapering; a tube per segment with a knuckle bead at each joint."""
    root = A(root, float)
    d = K.unit(direction)
    pts = [root]
    ang = 0.0
    cur = d.copy()
    for ln in lens:
        ang += curl
        cur = K.unit(d * math.cos(ang) + A((0, 0, -1.0)) * math.sin(ang))
        pts.append(pts[-1] + cur * ln)
    part = Part(name)
    K.tube(part, pts, r, D.sides(5, 3), taper=lambda t: 1.0 - 0.45 * t)
    for i, pnt in enumerate(pts[:-1]):
        L.sph(part, tuple(pnt), r * 1.25, 3, D.sides(6, 4))
    gs.add(part, slot, tile, flags, group, "tile", 8.0)
    if nail:
        cl = Part(name + "_nail")
        K.spike(cl, pts[-1] - cur * 0.2, pts[-1] + cur * 0.9, r * 0.8, 4)
        gs.add(cl, nail, "horn", 0, group)
    return pts


def hand_block(gs, D, out, y=-10.0, w=2.6, d=1.6, tile="leather", slot=MAIN, name="palm", fingers=True, finger_lens=(0.9, 0.8, 0.7), r=0.5, nail=None, curl=0.3, ftile=None, group=0):
    """Palm, four fingers and a thumb at the end of the arm."""
    cx = out * CX
    p = Part(name)
    loft(p, (0, -1, 0), (cx, y, 0), [0.0, 1.8], ml.ellipse(w, d, D.sides(10, 6)), caps=(True, True))
    gs.add(p, slot, tile, SHELL, group, "tile", 7.0)
    if fingers and not D.lod:
        for k in range(4):
            x = cx + (k - 1.5) * (w * 0.5)
            finger(gs, D, (x, y - 1.7, -0.2 - 0.0), (0, -1, 0), finger_lens, r * (1.0 - 0.06 * abs(k - 1.5)), slot, ftile or tile, curl, "%s_f%d" % (name, k), group, nail)
        finger(gs, D, (cx - out * -1.0 * 0 - out * w * 0.2, y - 0.4, -d * 0.9), (out * 0.3, -1, -0.6), (0.9, 0.8), r * 1.1, slot, ftile or tile, curl * 0.5, name + "_thumb", group, nail)


def knuckle_spikes(gs, D, out, y=-11.0, n=4, ln=1.5, r=0.4, slot=TRIM, tile="metal"):
    if D.lod:
        return
    p = Part("knuckle_spikes")
    cx = out * CX
    for k in range(n):
        x = cx + (k - (n - 1) / 2.0) * 1.3
        K.spike(p, A((x, y, -2.1)), A((x, y - 0.4, -2.1 - ln)), r, 4)
    gs.add(p, slot, tile)


def elbow_cop(gs, D, out, y=-3.2, r=2.2, tile="plate", slot=MAIN, spike=2.8):
    cx = out * CX
    p = Part("elbow")
    L.sph(p, (cx + out * 0.6, y, 0.0), r, 4, D.sides(10, 6), squash=0.8)
    gs.add(p, slot, tile, SHELL, 0, "tile", 7.0)
    if spike:
        sp = Part("elbow_spike")
        K.spike(sp, A((cx + out * 2.4, y, 0)), A((cx + out * (2.4 + spike), y + 0.8, 0.2)), 0.8, 5)
        gs.add(sp, TRIM, "horn")


# ================================================================ 1. Hand of Vecna
def hand_of_vecna(b, D):
    # right arm: the ornate gold gauntlet that holds the grafted hand's twin, a gold armguard with filigree, a red eye gem and spiked cuffs
    ra = b.set("rarm", "rarm")
    forearm(ra, D, -1, -3.4, -9.4, 3.0, 2.7, "hammered", TRIM, "armguard")
    for yy, rr in ((-3.6, 3.15), (-6.3, 2.95), (-9.2, 2.85)):
        cuff = Part("cuff%.0f" % -yy)
        L.torus(cuff, (-CX, yy, 0), rr, 0.28, D.dim(18, 8), 4)
        ra.add(cuff, MAIN, "plate")
    with ra.lvl(2):
        crest_disc(ra, D, (-CX - 0.0, -6.4, -2.9), 1.7, "crest", normal=(0, 0, -1), name="eyeplate")
        K.gem(ra, D, (-CX, -6.4, -3.5), 0.8, 0.9, 6, "z", slot=GLOW, name="rubyeye")
        elbow_cop(ra, D, -1, tile="hammered", slot=TRIM)
        K.filigree(ra, D, (-CX, -5.0, 2.9), (0, 0, 1), 2.6, turns=1.8, r=0.13, segs=28, arms=3, name="gfil", plane_up=(0, 1, 0))
        K.filigree(ra, D, (-CX - 2.9, -6.4, 0.0), (-1, 0, 0), 2.6, turns=1.8, r=0.13, segs=28, arms=3, name="gfil2", plane_up=(0, 1, 0))
    with ra.lvl(3):
        for k in range(D.cnt(6, 3)):
            sp = Part("cspike%d" % k)
            a = 2 * math.pi * k / D.cnt(6, 3)
            base = A((-CX + 3.0 * math.cos(a), -3.6, 3.0 * math.sin(a)))
            K.spike(sp, base, base + A((0.9 * math.cos(a), 1.5, 0.9 * math.sin(a))), 0.38, 4)
            ra.add(sp, TRIM, "gilt")
        K.shards(ra, D, (-CX, -6.5, 0.0), 4.4, 5, 0.35, y_amp=3.0, kind="orbit", speed=2.8, tile="spark", groups=1, name="redmote")
    # left arm: the Hand itself, grey and withered inside a gold cuff, runes crawling up the forearm, a red cold mist
    la = b.set("larm", "larm")
    forearm(la, D, 1, -3.8, -8.6, 2.75, 2.6, "bone", MAIN, "wither")
    cuf = Part("goldcuff")
    forearm(la, D, 1, -7.2, -9.2, 3.0, 2.95, "gilt", TRIM, "goldcuff")
    hand_block(la, D, 1, y=-10.2, w=2.5, d=1.5, tile="bone", slot=MAIN, name="vecna_hand", finger_lens=(1.0, 0.9, 0.8), r=0.5, nail=ACCENT, curl=0.42)
    with la.lvl(2):
        for k in range(D.cnt(5, 3)):                                                    # runes up the forearm
            rp = Part("vrune%d" % k)
            K.disc(rp, A((CX, -4.0 - 0.9 * k, -2.75)), A((1, 0, 0)), A((0, 1, 0)), 0.9, 0.35, 4, 0.2)
            la.add(rp, GLOW, "rune_band", EMIT | LIT | HOT, 0, "fit")
        K.gem(la, D, (CX, -8.3, -2.9), 0.7, 0.8, 6, "z", slot=GLOW, name="palmruby")
        veins_overlay(la, D, lambda u, v: (CX + 3.0 * math.cos(2 * math.pi * u), -4.0 - 4.2 * v, 2.9 * math.sin(2 * math.pi * u)), 0, nu=14, nv=8, dz=0.0, slot=MAIN, tile="vein")
    with la.lvl(3):
        wsp = la.group("mist", "bob", (CX, -11.0, 0), 0.7, 0.07, 0.0)
        K.wisp(la, D, (CX, -11.2, 0.0), (0.0, -1.0, -0.2), 5.0, 0.55, 5, wsp, GLOW, "shadow", EMIT | LIT | ADD, "coldmist", seed=15)
        mc = la.group("circle", "spin", (CX, -13.0, 0), 2.0, 0.0, 0.0)
        cp = Part("circle")
        K.link(cp, A((CX, -13.0, 0)), A((1, 0, 0)), A((0, 0, 1)), 3.6, 3.6, 0.12, D.dim(26, 10), 4)
        K.link(cp, A((CX, -13.0, 0)), A((1, 0, 0)), A((0, 0, 1)), 2.4, 2.4, 0.1, D.dim(22, 8), 4)
        la.add(cp, GLOW, "ring_glow", EMIT | HOT | ADD, mc, "fit")
        rr = Part("circle_runes")
        L.band(rr, -13.1, -12.9, 3.0, 3.0, 0.4, D.dim(28, 10), 0.0, 0.0, 2.0)
        for i in range(len(rr.V)):
            v = rr.V[i]
            rr.V[i] = A((v[0] + CX, v[1], v[2]), float)
        la.add(rr, GLOW, "rune_band", EMIT | LIT, mc, "fit")


# ================================================================ 2. Aerie Courier Gloves
def aerie_courier_gloves(b, D):
    ar = b.set("arm", "rarm", pair="larm")
    # long flared leather cuff with a gilt rim, a stitched glove, a gem on the back of the hand, wings at the wrist
    forearm(ar, D, -1, -3.8, -9.0, 2.85, 2.7, "leather", MAIN, "cuff", flare=0.9)
    for yy, rr in ((-3.9, 3.0), (-9.1, 3.55)):
        rim = Part("rim%.0f" % -yy)
        L.torus(rim, (-CX, yy, 0), rr, 0.3, D.dim(20, 8), 4)
        ar.add(rim, TRIM, "gilt")
    hand_block(ar, D, -1, y=-10.0, w=2.5, d=1.5, tile="leather", slot=MAIN, name="glove", finger_lens=(1.0, 0.85, 0.7), r=0.52, curl=0.25)
    with ar.lvl(2):
        crest_disc(ar, D, (-CX, -6.0, -3.0), 1.5, "crest", normal=(0, 0, -1), name="courierbadge")
        K.gem(ar, D, (-CX, -9.6, -1.7), 0.55, 0.8, 6, "z", slot=GLOW, name="handgem")
        for k in range(D.cnt(4, 2)):                                                    # strap buckles up the forearm
            st = Part("strap%d" % k)
            L.torus(st, (-CX, -4.6 - 1.2 * k, 0), 2.95 - 0.04 * k, 0.2, D.dim(18, 8), 4)
            ar.add(st, TRIM, "leather")
    wg = ar.group("wristwing", "sway", (-CX - 2.4, -8.0, 0.4), 6.0, 0.1, 0.0)
    K.feather_fan(ar, D, (-CX - 2.6, -8.0, 0.2), (-1.0, 0.2, 0.55), 7, 8.0, 1.4, 1.2, slot=ACCENT, tile="feather", group=wg, name="winga", ranks=2, curl=0.1)
    wg2 = ar.group("wristwing2", "sway", (-CX - 2.4, -8.0, 0.4), 7.0, 0.1, 1.5)
    K.feather_fan(ar, D, (-CX - 2.6, -8.4, 0.6), (-0.8, -0.4, 0.8), 5, 6.0, 1.3, 1.0, slot=MAIN, tile="feather", group=wg2, name="wingb")
    with ar.lvl(3):
        rb = ar.group("windtail", "wave", (-CX, -9.0, 0), 1.6, 0.12, 0.4)
        for j in range(D.cnt(4, 2)):
            a = 2 * math.pi * j / D.cnt(4, 2)
            pts = [(-CX + 2.9 * math.cos(a), -9.2 - 0.9 * i, 2.9 * math.sin(a) + 0.2 * i) for i in range(D.dim(7, 3))]
            K.ribbon(ar, D, pts, 1.5, GLOW, "soft", EMIT | ADD, rb, "windtail%d" % j, side=(0, 0, 1))
        g = ar.group("windring", "spin", (-CX, -8.0, 0), 6.0, 0.0, 0.0)
        rp = Part("windring")
        L.torus(rp, (-CX, -8.0, 0), 4.2, 0.12, D.dim(26, 10), 4)
        ar.add(rp, GLOW, "ring_glow", EMIT | HOT | ADD, g, "fit")
        K.filigree(ar, D, (-CX, -5.0, 2.9), (0, 0, 1), 2.4, turns=1.6, r=0.11, segs=22, arms=3, name="gfil", plane_up=(0, 1, 0))
        K.shards(ar, D, (-CX, -7.0, 0.0), 4.8, 6, 0.3, y_amp=2.5, kind="orbit", speed=3.0, tile="spark", groups=1, name="draft")


# ================================================================ 3. Bloodbound Gauntlets
def bloodbound_gauntlets(b, D):
    ar = b.set("arm", "rarm", pair="larm")
    forearm(ar, D, -1, -3.4, -9.6, 3.0, 2.75, "rust", MAIN, "vambrace", flare=0.2)
    elbow_cop(ar, D, -1, tile="rust", slot=MAIN, spike=3.4)
    for yy, rr in ((-5.0, 3.0), (-7.2, 2.95), (-9.4, 2.9)):                             # riveted bands
        bnd = Part("bnd%.0f" % -yy)
        L.torus(bnd, (-CX, yy, 0), rr, 0.3, D.dim(20, 8), 4)
        ar.add(bnd, TRIM, "hammered")
    # the fist: plated fingers, a thumb guard and knuckle spikes
    hand_block(ar, D, -1, y=-10.2, w=2.6, d=1.6, tile="plate", slot=MAIN, name="fist", finger_lens=(1.0, 0.9, 0.8), r=0.62, curl=0.55, nail=TRIM)
    knuckle_spikes(ar, D, -1, y=-11.6, n=4, ln=1.6, r=0.45)
    with ar.lvl(2):
        crest_disc(ar, D, (-CX, -6.4, -3.0), 1.7, "crest", normal=(0, 0, -1), name="dropsigil")
        for k in range(D.cnt(3, 2)):                                                    # blood vials set into the vambrace
            v = Part("vial%d" % k)
            L.sph(v, (-CX + (k - 1) * 1.7, -4.2, -3.0), 0.7, 4, 8)
            ar.add(v, GLOW, "gem", EMIT | HOT, 0, "fit")
        veins_overlay(ar, D, lambda u, v: (-CX + 3.05 * math.cos(2 * math.pi * u), -3.6 - 5.8 * v, 3.0 * math.sin(2 * math.pi * u)), 0, nu=16, nv=10, dz=0.0, slot=MAIN, tile="vein")
    with ar.lvl(3):
        for k in range(D.cnt(5, 2)):                                                    # dripping tassels
            top = A((-CX - 2.2 + 1.1 * k, -9.6, 2.6))
            g = ar.group("tassel%d" % k, "swing", tuple(top), 9.0, 1.6, k * 0.7)
            tp = Part("tassel%d" % k)
            K.tube(tp, [tuple(top - A((0, 1.0 * i, 0))) for i in range(5)], 0.3, 4, taper=lambda t: 1.0 - 0.6 * t)
            ar.add(tp, TRIM, "silk", 0, g)
            dp = Part("drop%d" % k)
            L.sph(dp, tuple(top - A((0, 4.6, 0))), 0.5, 3, 6)
            ar.add(dp, GLOW, "gem", EMIT | HOT, g, "fit")
        K.shards(ar, D, (-CX, -8.0, 0.0), 4.6, 6, 0.32, y_amp=3.0, kind="orbit", speed=2.6, tile="ember", groups=1, name="mist")
        K.filigree(ar, D, (-CX, -6.0, 2.95), (0, 0, 1), 2.6, turns=1.5, r=0.12, segs=22, arms=3, name="fil", plane_up=(0, 1, 0))


# ================================================================ 4. Incinerator Arm
def incinerator_arm(b, D):
    # right: the cannon arm: a heavy black housing, vents, pipes, a coil, and a long muzzle past the hand
    ra = b.set("rarm", "rarm")
    hs = Part("housing")
    lathe(hs, "y", (-CX, -9.8, 0), [(0.0, 2.9), (1.0, 3.35), (3.0, 3.5), (5.0, 3.35), (6.2, 3.0), (6.8, 2.6)], sides=D.sides(18, 8))
    ra.add(hs, MAIN, "rust", SHELL, 0, "tile", 6.0)
    with ra.lvl(1):
        bar = Part("barrel")
        lathe(bar, "y", (-CX, -9.8, 0), [(0.0, 1.9), (-2.0, 2.1), (-4.2, 1.5), (-6.0, 1.35), (-7.2, 1.7), (-7.6, 1.2)], sides=D.sides(14, 6))
        ra.add(bar, TRIM, "hammered", SHELL, 0, "tile", 6.0)
    with ra.lvl(2):
        for k in range(D.cnt(6, 3)):                                                    # vent slits, each lit from inside
            vv = Part("vent%d" % k)
            K.beam(vv, A((-CX - 3.0, -4.0 - 0.8 * k, -0.8)), A((-CX - 3.0, -4.0 - 0.8 * k, 0.8)), 0.45, 0.3)
            ra.add(vv, GLOW, "ember", EMIT | HOT | LIT, 0, "fit")
        for k in range(3):                                                              # exhaust pipes along the back of the arm
            pp = Part("pipe%d" % k)
            pts = K.bezier(A((-CX - 1.0 + 1.0 * k, -4.0, 3.2)), A((-CX - 1.0 + 1.0 * k, -2.0, 4.6)), A((-CX - 1.4 + 1.0 * k, 1.0, 5.0)), A((-CX - 1.6 + 1.1 * k, 4.0, 4.4)), D.dim(7, 3))
            K.tube(pp, pts, 0.55, D.sides(6, 4), taper=lambda t: 1.0 + 0.5 * t)
            ra.add(pp, DARK, "rust", SHELL, 0, "tile", 6.0)
        for yy in (-4.2, -6.0, -7.8):
            cl = Part("coil%.0f" % -yy)
            L.torus(cl, (-CX, yy, 0), 3.65, 0.3, D.dim(22, 8), 4)
            ra.add(cl, TRIM, "gilt")
        crest_disc(ra, D, (-CX, -6.0, -3.7), 1.4, "crest", normal=(0, 0, -1), name="gauge")
    with ra.lvl(3):
        sh = ra.group("shoulderpipe", "pulse", (-CX, -4.0, 0), 0.04, 0.08, 0.0)
        core = Part("furnace")
        lathe(core, "y", (-CX, -9.0, 0), [(0.0, 0.3), (1.0, 1.4), (3.0, 1.6), (5.0, 1.4), (6.0, 0.4)], sides=D.sides(12, 6))
        ra.add(core, GLOW, "ember", EMIT | HOT | ADD, sh, "fit")
        wsp = ra.group("heat", "bob", (-CX, -4.0, 0), 0.6, 0.09, 0.0)
        K.wisp(ra, D, (-CX, 1.0, 4.4), (0.0, 1.0, 0.3), 6.0, 0.6, 4, wsp, GLOW, "flame", EMIT | LIT | ADD, "exhaust", seed=21)
        gm = ra.group("ring", "spin", (-CX, -12.0, 0), 5.0, 0.0, 0.0)
        mr = Part("muzzle_ring")
        L.torus(mr, (-CX, -16.0, 0), 2.6, 0.18, D.dim(22, 8), 4, plane="xz")
        ra.add(mr, GLOW, "ring_glow", EMIT | HOT | ADD, gm, "fit")
        for k in range(D.cnt(10, 4)):
            rp = Part("rivet%d" % k)
            a = 2 * math.pi * k / D.cnt(10, 4)
            L.sph(rp, (-CX + 3.5 * math.cos(a), -3.6, 3.5 * math.sin(a)), 0.28, 3, 6)
            ra.add(rp, TRIM, "metal")
    # left: a smaller servo bracer with gauges and a lit coil
    la = b.set("larm", "larm")
    forearm(la, D, 1, -3.8, -9.0, 2.9, 2.75, "rust", MAIN, "servo")
    with la.lvl(2):
        for yy in (-4.4, -6.4, -8.4):
            cl = Part("lcoil%.0f" % -yy)
            L.torus(cl, (CX, yy, 0), 3.0, 0.28, D.dim(20, 8), 4)
            la.add(cl, TRIM, "gilt")
        crest_disc(la, D, (CX, -6.2, -3.0), 1.5, "crest", normal=(0, 0, -1), name="gear")
        for k in range(D.cnt(4, 2)):
            gv = Part("lvent%d" % k)
            K.beam(gv, A((CX + 3.0, -4.6 - 0.9 * k, -0.8)), A((CX + 3.0, -4.6 - 0.9 * k, 0.8)), 0.45, 0.3)
            la.add(gv, GLOW, "ember", EMIT | HOT | LIT, 0, "fit")
    with la.lvl(3):
        hand_block(la, D, 1, y=-10.0, w=2.5, d=1.5, tile="rust", slot=MAIN, name="servofist", finger_lens=(1.0, 0.85, 0.7), r=0.6, curl=0.4, nail=TRIM)
        K.wisp(la, D, (CX, -4.0, 3.4), (0.0, 1.0, 0.2), 4.0, 0.45, 3, 0, GLOW, "flame", EMIT | LIT | ADD, "lexhaust", seed=31)


BUILDERS = {"hand_of_vecna": hand_of_vecna, "aerie_courier_gloves": aerie_courier_gloves, "bloodbound_gauntlets": bloodbound_gauntlets, "incinerator_arm": incinerator_arm}
