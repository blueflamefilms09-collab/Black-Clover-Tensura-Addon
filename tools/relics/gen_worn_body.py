"""The worn BELT, HANDS, CHARM, CHEST and FEET models. Anchors: body (origin at the neck line), rarm (origin at the right shoulder pivot, the left arm is the mirror),
rleg (origin at the hip pivot, the left leg is the same model). All authored y up, front -z."""
import math

import numpy as np

import gen_worn_lib as L
from gen_worn_lib import (WModel, Part, loft, lathe, sweep, circle, octagon, rect_ring, MAIN, TRIM, GLOW, DARK, ACCENT, NONE, EMIT, SHELL, HOT)

MODELS = {}


def model(fn):
    MODELS[fn.__name__] = fn
    return fn


# ================================================================ belts
def _belt_core(m, y0=-9.0, h=2.2, a=5.0, b=3.0, tile="leather"):
    bd = Part("belt")
    L.band(bd, y0 - h, y0, a, b, 0.22, L.RN, 0.0, 0.0, 3.4)
    m.add(bd, MAIN, tile)
    for yy, nm in ((y0 - h, "lower"), (y0 - 0.25, "upper")):
        tr = Part("edge_" + nm)
        L.band(tr, yy, yy + 0.25, a, b, 0.3, L.RN, 0.0, 0.0, 3.4)
        m.add(tr, TRIM, "gilt")


def _buckle(m, y=-10.1, z=-3.25, w=3.0, h=2.6, gem=True):
    pl = Part("buckle")
    loft(pl, "z", (0, y, z), [(0.0, w / 2, h / 2), (0.55, w / 2, h / 2)], L.octagon(1.0, 1.0, 0.28))
    m.add(pl, TRIM, "gilt", SHELL)
    fr = Part("buckle_frame")
    L.torus(fr, (0, y, z - 0.25), w * 0.46, 0.2, 10, 4, plane="xy", a_scale=1.0, b_scale=h / w)
    m.add(fr, MAIN, "plate")
    if gem:
        g = Part("buckle_gem")
        L.ml.gem(g, (0, y, z - 0.45), 0.7, 0.8, 6, "z")
        m.add(g, GLOW, "gem", EMIT | HOT, uv="fit")


@model
def belt_buckled():
    """A broad belt: a leather band with edged trim, a big buckle plate with a stone, two hanging straps with tassel tips, a pouch at the right hip and a small charm at the left."""
    m = WModel("belt_buckled", "body")
    _belt_core(m)
    _buckle(m)
    g = m.group("straps", "swing", (0, -10.0, -3.0), 6.0, 1.0, 0.0)
    for sx in (-1, 1):
        s = Part("strap%d" % sx)
        loft(s, "-y", (sx * 2.4, -10.5, -3.15), [(0.0, 0.6, 0.18), (3.0, 0.6, 0.18), (3.6, 0.0, 0.0)], L.octagon(1.0, 1.0, 0.25))
        m.add(s, MAIN, "leather", 0, group=g)
        t = Part("tip%d" % sx)
        L.dia(t, (sx * 2.4, -13.9, -3.15), 0.45, 0.6, 0.25)
        m.add(t, TRIM, "gilt", 0, group=g, uv="fit")
    pouch = Part("pouch")
    L.sph(pouch, (-5.4, -11.8, -0.8), 1.5, 3, 8, squash=0.9)
    m.add(pouch, MAIN, "leather")
    tie = Part("pouch_tie")
    L.torus(tie, (-5.4, -10.6, -0.8), 1.1, 0.16, 8, 4, plane="xz")
    m.add(tie, TRIM, "gilt")
    ch = Part("charm")
    L.dia(ch, (5.4, -11.6, -0.8), 0.6, 0.9, 0.6)
    m.add(ch, GLOW, "gem", EMIT | HOT, uv="fit")
    return m


@model
def belt_alchemist():
    """An alchemist's bandolier belt: a wide leather belt with a row of small glass vials in clasps along the front, a mortar and a satchel."""
    m = WModel("belt_alchemist", "body")
    _belt_core(m, tile="leather")
    _buckle(m, gem=False)
    for k in range(4):
        x = -4.6 + k * 1.55 if k < 2 else 0.9 + (k - 2) * 1.55 + 0.6
        if abs(x) < 1.8:
            x += 2.0 * (1 if x >= 0 else -1)
        v = Part("vial%d" % k)
        lathe(v, "y", (x, -9.4, -3.4), [(0.0, 0.55), (0.35, 0.65), (1.4, 0.65), (1.9, 0.38), (2.2, 0.3)], sides=6, caps=(True, True))
        m.add(v, GLOW if k % 2 == 0 else ACCENT, "crystal", EMIT, uv="fit")
        cork = Part("cork%d" % k)
        L.dia(cork, (x, -9.4 + 2.35, -3.4), 0.28, 0.2, 0.28)
        m.add(cork, TRIM, "wood", uv="fit")
    sat = Part("satchel")
    loft(sat, "y", (-5.5, -9.8, -0.4), [(0.0, 1.4, 2.0), (-3.4, 1.4, 2.0)], L.octagon(1.0, 1.0, 0.25))
    m.add(sat, MAIN, "leather")
    fl = Part("flap")
    loft(fl, "y", (-5.5, -9.6, -0.4), [(0.0, 1.5, 2.1), (-1.5, 1.5, 2.1)], L.octagon(1.0, 1.0, 0.25))
    m.add(fl, TRIM, "leather")
    return m


# ================================================================ hands (right arm; the left is the mirror)
def _bracer(m, y0=-3.0, y1=-8.4, r=2.75, tile="plate", slot=MAIN, flare=0.0, name="bracer"):
    """A bracer lofted round the forearm (the arm box is 4 x 4 about x = -1, z = 0, so r 2.75 clears it and the sleeve)."""
    p = Part(name)
    loft(p, "y", (-1.0, 0.0, 0.0), [(y0, 1.0 + flare, 1.0 + flare), (y0 - 0.4, 1.0, 1.0), (y1 + 0.5, 0.94, 0.94), (y1, 1.0 + flare * 0.6, 1.0 + flare * 0.6)], L.octagon(2 * r, 2 * r, r * 0.45), caps=(False, False))
    m.add(p, slot, tile, SHELL)
    return p


def _wrist_bands(m, y0, y1, r=2.8, tile="gilt"):
    for y in (y0, y1):
        t = Part("band%.1f" % y)
        L.band(t, y - 0.3, y + 0.3, r, r, 0.28, 8, 0.0, 0.0, 4.0)
        t.V = [v + np.array([-1.0, 0, 0]) for v in t.V]
        m.add(t, TRIM, tile)


@model
def bracer_plated():
    """A plated bracer: a tapered forearm shell with a raised ridge, rivets and two trim bands, a gem on the back of the wrist and an upturned elbow guard."""
    m = WModel("bracer_plated", "rarm")
    _bracer(m, -3.2, -8.8, 2.75)
    _wrist_bands(m, -3.4, -8.6)
    rid = Part("ridge")
    loft(rid, "y", (-1.0, -3.8, -2.85), [(0.0, 0.4, 0.5), (-4.4, 0.55, 0.6), (-4.9, 0.0, 0.0)], L.octagon(1.0, 1.0, 0.25))
    m.add(rid, TRIM, "gilt")
    g = Part("gem")
    L.ml.gem(g, (-1.0, -6.0, -3.0), 0.7, 0.9, 6, "z")
    m.add(g, GLOW, "gem", EMIT | HOT, uv="fit")
    for k in range(3):
        rv = Part("rivet%d" % k)
        L.dia(rv, (-1.0 + (k - 1) * 1.8, -4.1, -2.85), 0.22, 0.22, 0.18)
        m.add(rv, TRIM, "gilt", uv="fit")
    eg = Part("elbow_guard")
    L.cone(eg, (-1.0, -3.1, 2.5), (-1.0, -3.9, 5.0), 0.8, 5, mid=0.4, mid_r=0.9)
    m.add(eg, TRIM, "plate")
    return m


@model
def gauntlet_fist():
    """A full gauntlet: a bracer with a flared cuff, a plated back of the hand with knuckle studs, and short finger plates; the cuff carries a glowing stone."""
    m = WModel("gauntlet_fist", "rarm")
    _bracer(m, -3.2, -8.0, 2.75, flare=0.1)
    cuff = Part("cuff")
    loft(cuff, "y", (-1.0, -8.0, 0.0), [(0.0, 1.0, 1.0), (-0.7, 0.8, 0.8)], L.octagon(6.2, 6.2, 2.4), caps=(False, False))
    m.add(cuff, TRIM, "gilt")
    hand = Part("hand")
    loft(hand, "y", (-1.0, -8.4, 0.0), [(0.0, 1.0, 1.0), (-2.4, 1.0, 1.0)], L.octagon(4.7, 4.6, 1.2))
    m.add(hand, MAIN, "plate", SHELL)
    for k in range(4):
        fp = Part("finger%d" % k)
        loft(fp, "y", (-2.5 + k * 1.1, -10.5, -2.3), [(0.0, 0.5, 0.35), (-0.9, 0.5, 0.35)], L.octagon(1.0, 1.0, 0.2))
        m.add(fp, TRIM, "plate")
        st = Part("stud%d" % k)
        L.dia(st, (-2.5 + k * 1.1, -10.0, -2.7), 0.3, 0.3, 0.3)
        m.add(st, ACCENT, "gilt", uv="fit")
    g = Part("gem")
    L.ml.gem(g, (-1.0, -5.0, -3.1), 0.75, 0.9, 6, "z")
    m.add(g, GLOW, "gem", EMIT | HOT, uv="fit")
    return m


@model
def glove_cuffed():
    """A soft glove: a flared cloth cuff at the wrist with a trim roll, a seam up the back of the hand, and a small glowing charm on the cuff."""
    m = WModel("glove_cuffed", "rarm")
    cuff = Part("cuff")
    loft(cuff, "y", (-1.0, -6.4, 0.0), [(0.0, 1.0, 1.0), (-1.2, 0.8, 0.8), (-2.6, 0.78, 0.78), (-3.6, 0.96, 0.96)], L.octagon(5.8, 5.8, 1.6), caps=(False, False))
    m.add(cuff, MAIN, "cloth", SHELL)
    roll = Part("roll")
    L.band(roll, -6.6, -6.1, 2.85, 2.85, 0.35, 8, 0.0, 0.0, 4.0)
    roll.V = [v + np.array([-1.0, 0, 0]) for v in roll.V]
    m.add(roll, TRIM, "stripe")
    hand = Part("hand")
    loft(hand, "y", (-1.0, -9.3, 0.0), [(0.0, 1.0, 1.0), (-1.1, 0.95, 0.95)], L.octagon(4.5, 4.5, 1.2))
    m.add(hand, MAIN, "cloth", SHELL)
    g = Part("charm")
    L.dia(g, (-1.0, -7.4, -3.1), 0.5, 0.6, 0.35)
    m.add(g, GLOW, "gem", EMIT | HOT, uv="fit")
    return m


@model
def wraps_hand():
    """Hand wraps: bandage turns spiralling from the elbow to the knuckles, an ember-lit rune ring at the wrist and glowing fingertips."""
    m = WModel("wraps_hand", "rarm")
    for k in range(9):
        y = -3.2 - k * 0.75
        w = Part("wrap%d" % k)
        L.band(w, y - 0.5, y + 0.4, 2.55 + (0.12 if k % 2 else 0), 2.55 + (0.12 if k % 2 else 0), 0.2, 8, 0.0, 0.0, 4.0)
        w.V = [v + np.array([-1.0 + 0.2 * math.sin(k), 0, 0.15 * math.cos(k)]) for v in w.V]
        m.add(w, MAIN, "cloth")
    rr = Part("rune_ring")
    L.band(rr, -6.9, -6.35, 2.85, 2.85, 0.15, 10, 0.0, 0.0, 4.0)
    rr.V = [v + np.array([-1.0, 0, 0]) for v in rr.V]
    m.add(rr, GLOW, "rune", EMIT | HOT, density=3.0)
    for k in range(4):
        t = Part("tip%d" % k)
        L.dia(t, (-2.2 + k * 1.1, -10.4, -1.8), 0.28, 0.4, 0.28)
        m.add(t, GLOW, "soft", EMIT | HOT, uv="fit")
    tie = Part("tie")
    loft(tie, "y", (-1.0, -5.6, 2.55), [(0.0, 0.5, 0.2), (-1.8, 0.4, 0.2), (-2.2, 0.0, 0.0)], L.octagon(1.0, 1.0, 0.25))
    m.add(tie, TRIM, "cloth")
    return m


@model
def bracelet_charms():
    """A slim bracelet at the wrist: a twisted band, a stone in a bezel on the back and three small charms dangling from it that swing as the arm moves."""
    m = WModel("bracelet_charms", "rarm")
    bd = Part("band")
    L.band(bd, -7.1, -6.4, 2.5, 2.5, 0.22, 10, 0.0, 0.0, 4.0)
    bd.V = [v + np.array([-1.0, 0, 0]) for v in bd.V]
    m.add(bd, MAIN, "gilt")
    g = Part("stone")
    L.ml.gem(g, (-1.0, -6.75, -2.95), 0.6, 0.8, 6, "z")
    m.add(g, GLOW, "gem", EMIT | HOT, uv="fit")
    grp = m.group("charms", "swing", (-1.0, -7.0, 0.0), 10.0, 0.9, 0.0)
    for k, ang in enumerate((-0.9, 0.0, 0.9)):
        x, z = -1.0 + 2.7 * math.sin(ang + math.pi), 2.7 * math.cos(ang + math.pi)
        ch = Part("chain%d" % k)
        loft(ch, "-y", (x, -7.0, z), [(0.0, 0.14, 0.14), (1.1, 0.14, 0.14)], L.circle(1.0, 4))
        m.add(ch, TRIM, "chain", 0, group=grp)
        c = Part("charm%d" % k)
        L.dia(c, (x, -8.5, z), 0.5, 0.65, 0.3)
        m.add(c, ACCENT if k != 1 else GLOW, "gem", EMIT if k == 1 else 0, group=grp, uv="fit")
    return m


@model
def ring_gem():
    """A ring on the hand and a thin armlet: a gold band with a raised bezel and a bright stone, and a second small band at the wrist."""
    m = WModel("ring_gem", "rarm")
    rr = Part("armlet")
    L.band(rr, -6.8, -6.2, 2.35, 2.35, 0.18, 10, 0.0, 0.0, 4.0)
    rr.V = [v + np.array([-1.0, 0, 0]) for v in rr.V]
    m.add(rr, MAIN, "gilt")
    fr = Part("ring")
    L.torus(fr, (-0.2, -9.9, -1.5), 0.75, 0.16, 8, 4, plane="xz")
    m.add(fr, MAIN, "gilt")
    g = Part("stone")
    L.dia(g, (-0.2, -9.9, -2.3), 0.4, 0.4, 0.4)
    m.add(g, GLOW, "gem", EMIT | HOT, uv="fit")
    return m


# ================================================================ charms at the hip
@model
def charm_orbit():
    """A charm that floats beside the wearer: a faceted gem in two thin rings, circling the hip slowly and bobbing."""
    m = WModel("charm_orbit", "body")
    g = m.group("orbit", "orbit", (0, -8.0, 0), 1.6, 0.0, 0.0)
    c = (9.8, -8.0, 0.0)
    gm = Part("gem")
    L.ml.gem(gm, c, 0.85, 1.4, 6, "y")
    m.add(gm, GLOW, "gem", EMIT | HOT, group=g, uv="fit")
    r1 = Part("ring1")
    L.torus(r1, c, 1.45, 0.14, 12, 4, plane="xz")
    m.add(r1, TRIM, "gilt", 0, group=g)
    r2 = Part("ring2")
    L.torus(r2, c, 1.45, 0.14, 12, 4, plane="xy")
    m.add(r2, TRIM, "gilt", 0, group=g)
    return m


@model
def charm_pouch():
    """A drawstring pouch hanging at the left hip from a cord looped over the belt line: a plump bag, a tied neck, a tassel and a glowing clasp stone."""
    m = WModel("charm_pouch", "body")
    cd = Part("cord")
    pts = L.bez3((4.0, -8.4, 0.9), (5.6, -9.0, -0.4), (5.6, -10.6, -0.8), (5.4, -11.4, -0.8), 6)
    sweep(cd, pts, circle(0.2, 4), scales=[(1.0, 1.0)] * len(pts), caps=(True, True))
    m.add(cd, TRIM, "leather")
    g = m.group("swing", "swing", (4.6, -9.0, 0.0), 5.0, 0.6, 0.0)
    bag = Part("bag")
    L.sph(bag, (5.6, -13.1, -0.4), 1.9, 4, 8, squash=0.95)
    m.add(bag, MAIN, "leather", SHELL, group=g)
    tie = Part("tie")
    L.torus(tie, (5.6, -11.3, -0.4), 0.95, 0.2, 8, 4, plane="xz")
    m.add(tie, TRIM, "gilt", 0, group=g)
    ta = Part("tassel")
    L.cone(ta, (5.6, -15.0, -0.4), (5.6, -17.0, -0.4), 0.45, 4, mid=0.4, mid_r=1.0)
    m.add(ta, TRIM, "silk", 0, group=g)
    st = Part("stone")
    L.dia(st, (5.6, -12.6, -2.1), 0.5, 0.6, 0.35)
    m.add(st, GLOW, "gem", EMIT | HOT, group=g, uv="fit")
    return m


@model
def charm_pouch_pair():
    """Two matching pouches, one at each hip, joined by a cord across the back: a set of bags that share their contents."""
    m = WModel("charm_pouch_pair", "body")
    g = m.group("swing", "swing", (0, -9.0, 0.0), 5.0, 0.6, 0.0)
    for sx in (-1, 1):
        bag = Part("bag%d" % sx)
        L.sph(bag, (sx * 5.6, -12.6, 0.2), 1.8, 4, 8, squash=0.95)
        m.add(bag, MAIN, "leather", SHELL, group=g)
        tie = Part("tie%d" % sx)
        L.torus(tie, (sx * 5.6, -10.9, 0.2), 0.9, 0.2, 8, 4, plane="xz")
        m.add(tie, TRIM, "gilt", 0, group=g)
        st = Part("stone%d" % sx)
        L.dia(st, (sx * 5.6, -12.2, -1.5), 0.45, 0.55, 0.3)
        m.add(st, GLOW, "gem", EMIT | HOT, group=g, uv="fit")
        cd = Part("cord%d" % sx)
        pts = L.bez3((sx * 5.6, -10.9, 0.2), (sx * 5.6, -9.4, 1.6), (sx * 4.0, -9.0, 2.7), (sx * 2.0, -9.2, 3.0), 5)
        sweep(cd, pts, circle(0.2, 4), scales=[(1.0, 1.0)] * len(pts), caps=(True, True))
        m.add(cd, TRIM, "leather")
    return m


@model
def charm_beads():
    """A loop of prayer beads draped from the right shoulder to the left hip: round beads in two sizes, a tassel and a bright centre bead."""
    m = WModel("charm_beads", "body")
    pts = L.bez3((-3.2, 0.4, -2.6), (-4.4, -4.0, -3.0), (2.0, -7.0, -3.2), (4.4, -10.4, -2.8), 12)
    b = Part("beads")
    sweep(b, pts, circle(0.45, 5), scales=[(1.0, 1.0), (0.55, 0.55)] * 7, caps=(True, True))
    m.add(b, MAIN, "pearl", 0, uv="fit")
    cn = Part("centre")
    L.sph(cn, pts[6], 0.8, 3, 8)
    m.add(cn, GLOW, "gem", EMIT | HOT, uv="fit")
    ta = Part("tassel")
    L.cone(ta, (4.4, -10.4, -2.8), (4.6, -13.6, -2.8), 0.5, 4, mid=0.4, mid_r=1.0)
    m.add(ta, TRIM, "silk")
    return m


@model
def charm_flask():
    """A small flask on a cord at the hip: a round glass bottle of glowing liquid, a cork and a sealing cord, swinging as the wearer moves."""
    m = WModel("charm_flask", "body")
    g = m.group("swing", "swing", (4.8, -9.0, 0.0), 6.0, 0.7, 0.0)
    fl = Part("flask")
    lathe(fl, "y", (5.6, -16.6, -0.6), [(0.0, 0.5), (0.5, 1.5), (1.4, 1.9), (2.6, 1.7), (3.4, 0.9), (4.0, 0.6), (4.6, 0.65)], sides=8, caps=(True, True))
    m.add(fl, GLOW, "crystal", EMIT | HOT, group=g, uv="fit")
    gl = Part("glass")
    lathe(gl, "y", (5.6, -16.6, -0.6), [(0.0, 0.5), (0.5, 1.5), (1.4, 1.9), (2.6, 1.7), (3.4, 0.9), (4.0, 0.6), (4.6, 0.65)], sides=8, caps=(True, True))
    gl.V = [v + (v - np.array([5.6, -14.1, -0.6])) * 0.06 for v in gl.V]
    m.add(gl, ACCENT, "crystal", SHELL, group=g, uv="fit")
    ck = Part("cork")
    L.dia(ck, (5.6, -11.9, -0.6), 0.5, 0.4, 0.5)
    m.add(ck, TRIM, "wood", 0, group=g, uv="fit")
    cd = Part("cord")
    pts = L.bez((3.9, -8.6, 0.6), (6.4, -9.8, -0.8), (5.6, -11.9, -0.6), 4)
    sweep(cd, pts, circle(0.16, 4), scales=[(1.0, 1.0)] * len(pts), caps=(True, True))
    m.add(cd, TRIM, "leather", 0, group=g)
    return m


@model
def charm_tome():
    """A pocket tome on a chain at the left hip: thick covers with a gilt corner and clasp, a ribbon marker and a glowing glyph on the front."""
    m = WModel("charm_tome", "body")
    g = m.group("swing", "swing", (4.6, -9.0, 0.0), 5.0, 0.6, 0.0)
    ch = Part("chain")
    pts = L.bez((3.9, -8.6, 0.8), (6.2, -10.0, -0.3), (5.7, -12.6, -0.8), 5)
    sweep(ch, pts, circle(0.18, 4), scales=[(1.0, 1.0), (0.6, 0.6)] * 3, caps=(True, True))
    m.add(ch, TRIM, "chain", 0, group=g)
    cv = Part("cover")
    loft(cv, "y", (5.7, -12.6, -0.9), [(0.0, 1.5, 0.7), (-4.2, 1.5, 0.7)], L.octagon(1.0, 1.0, 0.14))
    m.add(cv, MAIN, "leather", SHELL, group=g)
    pg = Part("pages")
    loft(pg, "y", (5.75, -12.8, -0.9), [(0.0, 1.35, 0.62), (-3.8, 1.35, 0.62)], L.octagon(1.0, 1.0, 0.05))
    m.add(pg, NONE, "paper", 0, group=g)
    cl = Part("clasp")
    loft(cl, "y", (5.7, -14.0, -1.62), [(0.0, 0.55, 0.12), (-1.0, 0.55, 0.12)], L.octagon(1.0, 1.0, 0.1))
    m.add(cl, TRIM, "gilt", 0, group=g)
    gl = Part("glyph")
    L.dia(gl, (5.7, -14.8, -1.8), 0.55, 0.55, 0.12)
    m.add(gl, GLOW, "gem", EMIT | HOT, group=g, uv="fit")
    return m


# ================================================================ chest pieces
def _pauldron(m, spikes=True, tile="plate"):
    """A layered pauldron over the right shoulder (arm anchor): three overlapping arched plates that follow the arm."""
    for k, (y, rr, hh) in enumerate(((1.2, 3.6, 1.4), (-0.6, 3.4, 1.3), (-2.3, 3.0, 1.2))):
        pl = Part("plate%d" % k)
        loft(pl, "y", (-1.0, y, 0.0), [(0.0, 0.88, 0.88), (-hh * 0.4, 1.0, 1.0), (-hh, 0.95, 0.95)], L.octagon(2 * rr, 2 * rr * 0.92, rr * 0.55), caps=(True, False))
        m.add(pl, MAIN if k != 1 else TRIM, tile if k != 1 else "gilt", SHELL)
    if spikes:
        sp = Part("spike")
        L.cone(sp, (-3.9, 2.2, 0.0), (-5.0, 5.6, -0.4), 0.85, 5, mid=0.4, mid_r=0.8)
        m.add(sp, TRIM, "gilt")


@model
def pauldron_plate():
    """Layered shoulder plates with a spike over the outer edge, trimmed, with a small stone on the top plate."""
    m = WModel("pauldron_plate", "rarm")
    _pauldron(m)
    g = Part("stone")
    L.dia(g, (-1.0, 2.2, -2.4), 0.45, 0.45, 0.35)
    m.add(g, GLOW, "gem", EMIT | HOT, uv="fit")
    return m


@model
def pauldron_leather():
    """A single rolled leather shoulder pad with a stitched edge and a strap round the arm, a metal stud and a hanging tassel."""
    m = WModel("pauldron_leather", "rarm")
    pad = Part("pad")
    L.sph(pad, (-1.0, 0.9, 0.0), 3.5, 3, 10, squash=0.5)
    m.add(pad, MAIN, "leather", SHELL)
    st = Part("strap")
    L.band(st, -1.6, -0.9, 2.65, 2.65, 0.25, 8, 0.0, 0.0, 4.0)
    st.V = [v + np.array([-1.0, 0, 0]) for v in st.V]
    m.add(st, TRIM, "leather")
    s2 = Part("stud")
    L.dia(s2, (-1.0, 3.0, 0.0), 0.5, 0.45, 0.5)
    m.add(s2, GLOW, "gem", EMIT, uv="fit")
    return m


@model
def pauldron_lion():
    """A lion-mane pauldron: a ring of mane tufts round the shoulder, a gilt plate with a small lion mask on the outer side, trailing tufts."""
    m = WModel("pauldron_lion", "rarm")
    pl = Part("plate")
    L.sph(pl, (-1.0, 1.0, 0.0), 3.4, 3, 8, squash=0.55)
    m.add(pl, MAIN, "plate", SHELL)
    for k in range(10):
        a = 2 * math.pi * k / 10
        x, z = -1.0 + 3.4 * math.cos(a), 3.4 * math.sin(a)
        t = Part("tuft%d" % k)
        L.cone(t, (x, 1.4, z), (x + 1.7 * math.cos(a), -0.8 - (k % 3) * 0.6, z + 1.7 * math.sin(a)), 0.7, 4, mid=0.35, mid_r=0.9)
        m.add(t, TRIM, "fur", SHELL)
    mk = Part("mask")
    L.sph(mk, (-4.7, 0.8, 0.0), 1.4, 3, 8, squash=0.8)
    m.add(mk, MAIN, "gilt")
    for sz in (-1, 1):
        e = Part("eye%d" % sz)
        L.dia(e, (-5.4, 1.1, sz * 0.6), 0.2, 0.2, 0.3)
        m.add(e, GLOW, "gem", EMIT, uv="fit")
    return m


@model
def chest_collar():
    """A gorget and chest crest: a high plated collar round the neck, a diamond crest plate on the sternum with a stone, fine trim along the lower edge."""
    m = WModel("chest_collar", "body")
    col = Part("gorget")
    L.band(col, -2.6, 0.5, 5.15, 3.1, 0.55, L.RN, 0.15, 0.5, 3.4)
    m.add(col, MAIN, "plate", SHELL)
    tr = Part("trim")
    L.band(tr, -2.8, -2.35, 5.2, 3.15, 0.6, L.RN, 0.0, 0.0, 3.4)
    m.add(tr, TRIM, "gilt")
    cr = Part("crest")
    loft(cr, "z", (0, -4.6, -3.2), [(0.0, 1.0, 1.0), (0.5, 1.0, 1.0)], [(0.0, 2.2), (1.9, 0.0), (0.0, -2.2), (-1.9, 0.0)])
    m.add(cr, TRIM, "gilt", SHELL, uv="fit")
    g = Part("stone")
    L.ml.gem(g, (0, -4.6, -3.75), 0.85, 0.9, 6, "z")
    m.add(g, GLOW, "gem", EMIT | HOT, uv="fit")
    return m


@model
def coat_tails():
    """A long coat's skirts: front panels and a split back flap falling from the waist to the knee, wide lapels, a stand collar and a row of buttons."""
    m = WModel("coat_tails", "body")
    col = Part("collar")
    L.band(col, -1.8, 0.8, 4.9, 2.95, 0.5, L.RN, 0.1, 0.35, 3.4)
    m.add(col, MAIN, "cloth", SHELL)
    # the skirt: two back panels and two front panels
    g = m.group("skirt", "swing", (0, -11.0, 0.0), 6.0, 0.9, 0.0)
    for (sx, zs) in ((-1, 1), (1, 1), (-1, -1), (1, -1)):
        def fn(u, v, sx=sx, zs=zs):
            x = sx * (0.35 + u * 4.65)
            y = -10.6 - v * 10.0
            z = zs * (2.7 + 0.5 * v + 1.8 * v * v * (u if zs > 0 else 0.4 * u)) + zs * 0.0
            return (x, y, z)
        p = Part("panel%d%d" % (sx, zs))
        L.sheet(p, fn, 3, 4, 0.35 * zs * sx)
        m.add(p, MAIN, "cloth", SHELL, group=g)
    for sx in (-1, 1):
        for zs in (-1, 1):
            h = Part("hem%d%d" % (sx, zs))
            L.sheet(h, lambda u, v, sx=sx, zs=zs: (sx * (0.35 + u * 4.65), -20.3 - 0.3 * v, zs * (2.7 + 0.5 + 1.8 * (u if zs > 0 else 0.4 * u))), 3, 1, 0.35)
            m.add(h, TRIM, "gilt", 0, group=g)
    for k in range(4):
        b = Part("button%d" % k)
        L.dia(b, (0.0, -4.6 - k * 1.8, -3.2), 0.28, 0.28, 0.2)
        m.add(b, GLOW, "gem", EMIT, uv="fit")
    return m


@model
def chest_straps():
    """Crossed leather straps over the chest with a round buckle, a shoulder pauldron's worth of studded pad on the left and a bandolier of small gems."""
    m = WModel("chest_straps", "body")
    for sx in (-1, 1):
        s = Part("strap%d" % sx)
        pts = L.bez((sx * 3.6, 0.0, -3.1), (0.0, -6.0, -3.45), (-sx * 3.6, -12.0, -3.1), 6)
        sweep(s, pts, L.octagon(1.2, 0.3, 0.12), scales=[(1.0, 1.0)] * len(pts), caps=(True, True), up=(0, 0, 1))
        m.add(s, MAIN, "leather")
    bk = Part("buckle")
    L.torus(bk, (0.0, -6.0, -3.65), 0.95, 0.2, 10, 4, plane="xy")
    m.add(bk, TRIM, "gilt")
    g = Part("gem")
    L.ml.gem(g, (0.0, -6.0, -3.7), 0.55, 0.9, 6, "z")
    m.add(g, GLOW, "gem", EMIT | HOT, uv="fit")
    for k in range(5):
        t = k / 4.0
        x = 3.6 - 7.2 * t
        y = -0.4 - t * 11.0
        st = Part("stud%d" % k)
        L.dia(st, (x, y, -3.65), 0.26, 0.26, 0.22)
        m.add(st, TRIM, "gilt", uv="fit")
    return m


@model
def sleeves_flared():
    """Wide trumpet sleeves: the cuffs of a robe flare out over the hands, with a trim border and an embroidered stone, swaying with the arm."""
    m = WModel("sleeves_flared", "rarm")
    g = m.group("cuff", "sway", (-1.0, -6.5, 0.0), 4.0, 0.1, 0.0)
    p = Part("cuff")
    loft(p, "y", (-1.0, -6.5, 0.0), [(0.0, 1.0, 1.0), (-1.4, 1.12, 1.12), (-3.2, 1.55, 1.55), (-4.6, 1.85, 1.85)], L.octagon(5.2, 5.2, 1.5), caps=(False, False))
    m.add(p, MAIN, "silk", SHELL, group=g)
    tr = Part("border")
    L.band(tr, -11.2, -10.7, 4.6, 4.6, 0.3, 10, 0.0, 0.0, 3.0)
    tr.V = [v + np.array([-1.0, 0, 0]) for v in tr.V]
    m.add(tr, TRIM, "gilt", 0, group=g)
    s = Part("stone")
    L.dia(s, (-1.0, -8.6, -3.7), 0.5, 0.6, 0.3)
    m.add(s, GLOW, "gem", EMIT | HOT, group=g, uv="fit")
    return m


# ================================================================ feet (leg anchor; both legs use the same model)
@model
def boots_cuffed():
    """Boots: a shaft that hugs the whole shin to the ankle, a turned-over cuff, a rounded toe that reaches forward, a heel and a dark sole, a front strap with a buckle
    and a small stone at the ankle."""
    m = WModel("boots_cuffed", "rleg")
    shaft = Part("shaft")
    loft(shaft, "y", (0.0, -5.6, 0.0), [(0.0, 1.0, 1.0), (-3.2, 0.98, 0.98), (-5.6, 1.02, 1.02), (-6.7, 1.0, 1.0)], L.octagon(4.7, 4.7, 1.1), caps=(False, True))
    m.add(shaft, MAIN, "leather", SHELL)
    cuff = Part("cuff")
    loft(cuff, "y", (0.0, -5.2, 0.0), [(0.0, 1.0, 1.0), (-0.4, 1.1, 1.1), (-2.0, 1.1, 1.1), (-2.4, 1.0, 1.0)], L.octagon(5.1, 5.1, 1.2), caps=(False, False))
    m.add(cuff, TRIM, "leather", SHELL)
    toe = Part("toe")
    loft(toe, "-z", (0.0, -11.2, -2.0), [(0.0, 1.0, 1.0), (1.2, 0.98, 0.9), (2.2, 0.8, 0.6), (2.7, 0.0, 0.0)], L.octagon(4.7, 2.6, 0.9), caps=(False, True))
    m.add(toe, MAIN, "leather", SHELL)
    cap = Part("toe_cap")
    loft(cap, "-z", (0.0, -11.3, -2.3), [(0.0, 1.0, 1.0), (1.2, 0.95, 0.85), (2.0, 0.75, 0.55), (2.5, 0.0, 0.0)], L.octagon(4.9, 2.3, 0.8), caps=(False, True))
    m.add(cap, TRIM, "plate", SHELL)
    sole = Part("sole")
    loft(sole, "y", (0.0, -11.9, -0.4), [(0.0, 1.0, 1.0), (-0.7, 1.0, 1.0)], L.octagon(4.9, 7.4, 0.9))
    m.add(sole, DARK, "leather")
    heel = Part("heel")
    loft(heel, "y", (0.0, -11.2, 2.3), [(0.0, 1.0, 1.0), (-1.2, 0.9, 0.9)], L.octagon(4.7, 1.2, 0.4))
    m.add(heel, TRIM, "leather")
    st = Part("strap")
    L.band(st, -8.0, -7.2, 2.5, 2.5, 0.2, 10, 0.0, 0.0, 3.4)
    m.add(st, TRIM, "gilt")
    bk = Part("buckle")
    L.torus(bk, (0.0, -7.6, -2.8), 0.7, 0.16, 6, 4, plane="xy")
    m.add(bk, TRIM, "gilt")
    g = Part("stone")
    L.dia(g, (-2.6, -7.4, 0.0), 0.35, 0.5, 0.5)
    m.add(g, GLOW, "gem", EMIT | HOT, uv="fit")
    return m


@model
def greaves_plated():
    """Plated greaves: a flared knee cop with a spike, a shin plate with a centre ridge, flanged side fins at the calf and an armoured toe, with a stone on the knee."""
    m = WModel("greaves_plated", "rleg")
    knee = Part("knee")
    L.sph(knee, (0.0, -1.6, -1.2), 2.7, 3, 8, squash=0.7)
    m.add(knee, MAIN, "plate", SHELL)
    ks = Part("knee_spike")
    L.cone(ks, (0.0, -1.6, -3.3), (0.0, -1.8, -5.4), 0.8, 5, mid=0.4, mid_r=0.8)
    m.add(ks, TRIM, "gilt")
    shin = Part("shin")
    loft(shin, "y", (0.0, -3.4, 0.0), [(0.0, 1.0, 1.0), (-1.0, 1.1, 1.1), (-5.5, 0.95, 0.95), (-7.0, 0.88, 0.88)], L.octagon(4.5, 4.5, 1.3), caps=(False, False))
    m.add(shin, MAIN, "plate", SHELL)
    rg = Part("ridge")
    loft(rg, "y", (0.0, -3.6, -2.4), [(0.0, 0.5, 0.45), (-5.0, 0.5, 0.45), (-6.0, 0.0, 0.0)], L.octagon(1.0, 1.0, 0.25))
    m.add(rg, TRIM, "gilt")
    for sx in (-1, 1):
        fin = Part("fin%d" % sx)
        loft(fin, "y", (sx * 2.5, -5.0, 0.4), [(0.0, 0.2, 1.2), (-3.0, 0.2, 1.7), (-5.2, 0.1, 0.4)], L.octagon(1.0, 1.0, 0.2))
        m.add(fin, TRIM, "plate")
    toe = Part("toe")
    loft(toe, "y", (0.0, -10.4, -1.6), [(0.0, 1.0, 1.0), (-1.9, 1.0, 1.0)], L.octagon(4.4, 3.2, 1.0))
    m.add(toe, MAIN, "plate")
    g = Part("stone")
    L.ml.gem(g, (0.0, -1.6, -3.5), 0.55, 0.9, 6, "z")
    m.add(g, GLOW, "gem", EMIT | HOT, uv="fit")
    return m


@model
def sandals_winged():
    """Sandals with winged anklets: criss-cross straps up the shin and a pair of small feathered wings at each ankle that flutter at rest."""
    m = WModel("sandals_winged", "rleg")
    for k, y in enumerate((-8.2, -9.3, -10.3)):
        s = Part("strap%d" % k)
        L.band(s, y, y + 0.45, 2.4, 2.4, 0.15, 8, 0.0, 0.0, 4.0)
        m.add(s, MAIN, "leather")
    g = m.group("wings", "sway", (0.0, -8.4, 0.0), 6.0, 0.22, 0.0)
    for sx in (-1, 1):
        for k in range(3):
            f = Part("feather%d_%d" % (sx, k))
            L.feather(f, (sx * 2.5, -8.2 - 0.4 * k, 0.4), (sx * (5.6 - 0.6 * k), -6.4 - 1.2 * k + 0.0, 1.6 + 0.4 * k), 0.75, 0.22, 4, curl=0.0)
            m.add(f, MAIN if k != 1 else ACCENT, "feather", SHELL, group=g, uv="fit")
    cl = Part("clasp")
    L.dia(cl, (0.0, -8.7, -2.5), 0.4, 0.4, 0.3)
    m.add(cl, GLOW, "gem", EMIT | HOT, uv="fit")
    return m
