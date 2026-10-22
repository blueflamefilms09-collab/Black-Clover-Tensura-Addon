"""Fuegoleon Vermillion's Salamander, both forms (docs/magic/fire_casting.md), rebuilt to match the four reference stills: the small red lizard-dragon that CLINGS to the caster's shoulder
(forelimbs gripping the edge, hind legs braced, arched spine, a long plated tail that curls up over the back, a wedge-shaped head with a brow ridge, swept crest and big slit eyes, small bat wings with
finger bones) and the big golden dragon that hangs over the caster while a big page is breathed (upright muscular torso with chest and ribs, long neck, horned head with a frill and an open fanged
mouth, huge veined wings with finger bones, clawed hands, legs tucked, a spiny tail). Models (Bedrock geo, 1/16 block units, y up, front -z), a hand-painted sheet each (tools/gen_salamander_art.py),
animations, three levels of detail per form and a glow-only model:

    python3 -B tools/gen_salamander_geo.py            writes assets/nusmp/{geo,animations,textures}/entity/firecast_{salamander,dragon}[_lod1|_lod2|_fx]*
    python3 -B tools/gen_salamander_geo.py --count    prints the quads of every model

    firecast_salamander      LOD 0 (full)           firecast_dragon      LOD 0
    firecast_salamander_lod1 LOD 1 (about 40 %)     firecast_dragon_lod1 LOD 1
    firecast_salamander_lod2 LOD 2 (low poly)       firecast_dragon_lod2 LOD 2
    firecast_salamander_fx   the glow-only model (eyes, mouth, flame licks), drawn additive with the glow sheet

Geometry goes where the silhouette is (the head, the neck, the wing fingers, the claws, the tail spikes, the chest of the big form); everything else is texture. Silhouette shapes that are not boxes are
FLAT CARDS (one quad, alpha-cut by the picture: spikes, claws, fangs, the wing membrane panels, the frill, the eyes). Every bone has its pivot at a joint and the cubes are laid out in the posed shape
(limbs and tail are chains of boxes laid between joint points), so the one animation file drives every level of detail (a bone a LOD does not have is ignored).
"""

import math
import os
import sys
import zlib

import numpy as np

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import firecast_geo_lib as L  # noqa: E402
import gen_salamander_art as A  # noqa: E402

from firecast_geo_lib import vec  # noqa: E402


def unit(v):
    return L.unit(v)


def lerp(a, b, t):
    return a + (b - a) * t


class Ctx:
    """What one model of a form is built with: the model, the level of detail, whether only the glow parts are wanted, and the texture density."""

    def __init__(self, form, lod, fx, atlas, ident, tex_scale, seed, d):
        self.form, self.lod, self.fx = form, lod, fx
        self.m = L.Model(ident, atlas, seed=seed, tex_scale=tex_scale)
        self.d = d

    def bone(self, name, pivot, parent=None, rotation=None):
        self.m.bone(name, [float(v) for v in pivot], parent, rotation)

    # a cube / limb / card that exists in the colour model only
    def box(self, bone, center, size, R, faces, d=None):
        if not self.fx:
            self.m.box(bone, center, size, R, faces, self.d if d is None else d)

    def limb(self, bone, p0, p1, w, h, faces, up=(0, 1, 0), pad=0.0, roll=0.0, d=None):
        if not self.fx:
            self.m.limb(bone, p0, p1, w, h, faces, up, pad, roll, self.d if d is None else d)

    def card(self, bone, center, right, up, w, h, region):
        if not self.fx:
            self.m.card(bone, center, right, up, w, h, (region, 0))

    def glow_card(self, bone, center, right, up, w, h, region):
        """a card of the glow-only model (drawn additive from the glow sheet)"""
        if self.fx:
            self.m.card(bone, center, right, up, w, h, (region, 0))

    def spike(self, bone, base, tip_dir, height, width, across, region="spike", cross=False):
        """A spike standing on `base`, pointing along tip_dir; the picture's right is `across` (the side the card is seen from). cross adds a second card turned a quarter."""
        td = unit(tip_dir)
        ac = unit(vec(*across) - td * float(np.dot(across, td)))
        self.card(bone, vec(*base) + td * height / 2.0, ac, td, width, height, region)
        if cross:
            third = np.cross(td, ac)
            self.card(bone, vec(*base) + td * height / 2.0, unit(third), td, width, height, region)


def rot_about(v, axis, deg):
    """The vector v turned by deg degrees about axis (Rodrigues)."""
    a = unit(axis)
    t = math.radians(deg)
    return v * math.cos(t) + np.cross(a, v) * math.sin(t) + a * float(np.dot(a, v)) * (1.0 - math.cos(t))


def curl_finger(c, bone, p0, dirv, lens, ws, curl, axis, faces, claw=None, cross=False, d=None):
    """A finger (or toe) of boxes laid end to end, each turned `curl` degrees about `axis` from the one before, so it hooks; a claw card on the end."""
    p = vec(*p0)
    dv = unit(dirv)
    for ln, w in zip(lens, ws):
        q = p + dv * ln
        c.limb(bone, p, q, w, w * 0.9, faces, pad=w * 0.3, d=d)
        p = q
        dv = rot_about(dv, axis, curl)
    if claw:
        c.spike(bone, p - dv * claw[0] * 0.15, dv, claw[0], claw[1], vec(1, 0, 0), "claw", cross=cross)


def tail_path(n, start, heading0, heading1, length):
    """The nodes of a tail: n segments from `start` (y, z) heading0 degrees (0 = straight back +z, 90 = up, 180 = forward) turning to heading1; returns the n + 1 nodes as (y, z) and the headings."""
    pts = [np.array(start, dtype=np.float64)]
    heads = []
    ds = length / n
    for i in range(n):
        a = math.radians(lerp(heading0, heading1, (i + 0.5) / n))
        heads.append(a)
        pts.append(pts[-1] + np.array([math.sin(a), math.cos(a)]) * ds)
    return pts, heads


def scallop(p, q, pull_to, pull=0.28, n=10):
    """A concave edge from p to q, bowed toward `pull_to` (the wrist): the scalloped trailing edge of a membrane between two fingers."""
    p, q, w = np.array(p, float), np.array(q, float), np.array(pull_to, float)
    mid = (p + q) / 2.0
    c = mid + (w - mid) * pull
    return [tuple((1 - t) ** 2 * p + 2 * (1 - t) * t * c + t * t * q) for t in np.linspace(0, 1, n)]


def wing_layout(span, big=False):
    """The wing in its own plane: u outward, c backward (the leading edge is at negative c). The shoulder, the elbow, the wrist, the four finger tips (from the leading edge back) and the hip point."""
    if big:
        k = span / 118.0
        return dict(S=(0.0, 0.0), E=(34.0 * k, 8.0 * k), W=(70.0 * k, -12.0 * k), T=[(122.0 * k, -2.0 * k), (113.0 * k, 33.0 * k), (88.0 * k, 63.0 * k), (52.0 * k, 80.0 * k)], H=(4.0 * k, 38.0 * k))
    k = span / 34.0
    return dict(S=(0.0, 0.0), E=(11.0 * k, -1.6 * k), W=(20.5 * k, -4.4 * k), T=[(34.0 * k, -3.0 * k), (31.6 * k, 8.6 * k), (24.6 * k, 18.6 * k), (13.4 * k, 23.0 * k)], H=(1.0 * k, 17.0 * k))


def wing_panels(span, big=False):
    """The four membrane panels of a wing as polygons in the wing's plane (u, c) with the bone lines (kind: arm, fore, finger) and the vein lines of each; used by the sheet painter and by the model."""
    P = wing_layout(span, big)
    S, E, W, H = [np.array(P[k], float) for k in ("S", "E", "W", "H")]
    T = [np.array(t, float) for t in P["T"]]
    pull = 0.40 if not big else 0.26
    panels = {}
    for k in range(3):                                                           # between finger k and finger k + 1
        edge = scallop(T[k], T[k + 1], W, pull)
        vs = [(tuple(W + (T[k] - W) * 0.45), tuple(lerp(edge[3], W, 0.12))), (tuple(W + (T[k + 1] - W) * 0.45), tuple(lerp(edge[6], W, 0.12))),
              (tuple(W + (T[k] - W) * 0.25), tuple(lerp(edge[5], W, 0.40))), (tuple(lerp(edge[2], W, 0.2)), tuple(lerp(edge[7], W, 0.2)))]
        panels["q%d" % (k + 1)] = dict(poly=[tuple(W)] + edge, bones=[(tuple(W), tuple(T[k]), "finger"), (tuple(W), tuple(T[k + 1]), "finger")], edge=edge, veins=vs)
    edge0 = scallop(T[3], H, W, pull * 0.8)
    panels["q0"] = dict(poly=[tuple(S), tuple(E), tuple(W), tuple(T[3])] + edge0[1:] + [tuple(H)],
                        bones=[(tuple(S), tuple(E), "arm"), (tuple(E), tuple(W), "fore"), (tuple(W), tuple(T[3]), "finger")], edge=edge0,
                        veins=[(tuple(lerp(E, H, 0.35)), tuple(lerp(W, H, 0.72))), (tuple(lerp(S, H, 0.5)), tuple(lerp(E, T[3], 0.55))), (tuple(lerp(W, T[3], 0.45)), tuple(lerp(H, W, 0.3))),
                               (tuple(lerp(E, T[3], 0.25)), tuple(lerp(H, E, 0.3)))])
    return panels


def panel_box(panel, margin=1.2):
    pts = np.array(panel["poly"], float)
    return pts[:, 0].min() - margin, pts[:, 0].max() + margin, pts[:, 1].min() - margin, pts[:, 1].max() + margin


ARM_W = 2.3                                                                       # the arm bone's thickness at bar_k = 1; the finger is half of it


def add_wing_regions(at, pal, seed, span, dpx, big, bar_k):
    """The sheet regions of the four membrane panels (picture: outward = right, backward = down), painted from the same polygons the model's cards are cut to."""
    bw = {"arm": ARM_W * bar_k, "fore": ARM_W * 0.82 * bar_k, "finger": ARM_W * 0.5 * bar_k}
    for name, pn in wing_panels(span, big).items():
        u0, u1, c0, c1 = panel_box(pn)
        w, h = int(math.ceil((u1 - u0) * dpx)), int(math.ceil((c1 - c0) * dpx))

        def paint(w_, h_, pn=pn, u0=u0, c0=c0, nm=name):
            f = lambda p: ((p[0] - u0) * dpx, (p[1] - c0) * dpx)
            poly_px = [f(p) for p in pn["poly"]]
            bones = [(f(a), f(b), max(1.8, bw[kind] * dpx)) for a, b, kind in pn["bones"]]
            veins = [[f(a), f(b)] for a, b in pn["veins"]]
            edge = [f(p) for p in pn["edge"]]
            return A.wing_panel(w_, h_, pal, seed + zlib.crc32(nm.encode()) % 97, poly_px, bones, veins, edge)
        at.region(name, w, h, paint)


def add_wing(ctx, side, S_pt, span, bones, lod, big=False, wk=1.0, bar_k=1.0):
    """One wing: the arm bone (shoulder to elbow, elbow to wrist) and four finger bones are boxes, the membrane is four flat cards (one per panel, each moving with its finger). The cubes are laid out flat
    (the wing's plane is x, z) and the bone's rest rotation raises and sweeps the wing. `side` is -1 for the model's right (-x). bones = (wing, wingout, [finger bones]). Returns the points."""
    wing, out, fingers = bones
    s = side
    P = wing_layout(span, big)
    Sx, Sy, Sz = S_pt
    pt = lambda u, c, y=0.0: vec(Sx + s * u, Sy + y, Sz + c)
    E, Wr = pt(*P["E"]), pt(*P["W"])
    arm = ARM_W * bar_k
    bar = {"up": ("bone", 0), "down": ("bone", 0), "north": ("bone", 0), "south": ("bone", 0)}
    ctx.limb(wing, pt(0, 0), E, arm, arm * 0.9, bar if lod < 2 else {"up": ("bone", 0), "west": ("bone", 0), "east": ("bone", 0)}, pad=arm * 0.35, d=0)
    if lod < 2:
        ctx.limb(out, E, Wr, arm * 0.82, arm * 0.78, bar, pad=arm * 0.35, d=0)
    T = [pt(*t) for t in P["T"]]
    fw = arm * 0.5
    for k in range(4):
        if lod == 0:
            ctx.limb(fingers[k], Wr, T[k], fw, fw * 0.9, bar, pad=fw * 0.5, d=0)
        elif lod == 1:
            ctx.limb(fingers[k], Wr, T[k], fw * 1.2, fw * 1.1, {"up": ("bone", 0), "down": ("bone", 0)}, pad=fw * 0.5, d=0)
    if lod == 0:                                                                     # a claw on each finger tip (the three inner fingers)
        for k in (1, 2, 3):
            fd = unit(T[k] - Wr)
            ctx.spike(fingers[k], T[k] - fd * 0.2 * wk, fd, 3.0 * wk, 1.5 * wk, vec(0, 1, 0), "claw", cross=True)
    if lod <= 1:                                                                     # the thumb claw standing at the wrist
        ctx.spike(out, Wr + vec(0, arm * 0.2, -arm * 0.1), vec(0.0, 0.8, -0.7), 4.6 * wk, 2.6 * wk, vec(s, 0, 0), "claw")
    panels = wing_panels(span, big)
    order = [("q0", wing, 0.0), ("q1", fingers[0], 0.04 * wk), ("q2", fingers[1], 0.08 * wk), ("q3", fingers[2], 0.04 * wk)]
    if lod == 2:
        order = order[:2]
    for name, bone, dy in order:
        u0, u1, c0, c1 = panel_box(panels[name])
        ctx.card(bone, pt((u0 + u1) / 2, (c0 + c1) / 2, dy), vec(s, 0, 0), vec(0, 0, -1), u1 - u0, c1 - c0, name)
    return E, Wr, T


# ====================================================================================================================== the head (both forms)
def head_parts(c, JH, hs, big, lod, d):
    """The head, local to the head bone's pivot JH (the snout points to -z), scaled by hs. The small form's head is short and wide (a cat-like dragon face with a blunt snout), the big form's long with a
    long jaw; both have a skull, a two-step sloping snout, cheeks, a lower jaw (its own bone) with a furnace on its top, big slit eyes on cards, slanted brow ridges with a spike each, cheek frills, a
    jagged jaw line, fangs and a swept crest; the big form adds long swept horns. All the detail that is not a silhouette is texture."""
    SK = ("skin", d)
    BE = ("belly", d)
    sl, ws = (1.0, 1.0) if big else (0.58, 1.2)

    def H(x, y, z):
        return JH + vec(x, y, z) * hs
    I3 = np.eye(3)
    rx = lambda a: L.rot_matrix(a, 0, 0)
    L1, L2 = 4.6 * sl, 3.8 * sl
    zs1 = -(6.9 + L1 / 2 - 0.4)
    zs2 = -(6.9 + L1 + L2 / 2 - 0.8)
    zend = -(6.9 + L1 + L2 - 0.8)
    jlen = -zend - 1.2
    if lod == 2:
        c.box("head", H(0, 0.3, zend / 2), (7.4 * ws * hs, 6.4 * hs, -zend * hs), I3, {"up": ("head_top", 0), "west": ("snout_w", 0), "east": ("snout_e", 0), "south": SK, "down": ("jaw_in", 0)}, 0)
    else:
        c.box("head", H(0, 0.5, -3.4), (7.6 * ws * hs, 6.8 * hs, 7.0 * hs), I3, {"up": ("head_top", 0), "west": ("skull_w", 0), "east": ("skull_e", 0), "south": SK, "down": SK}, 0)
        sn1 = {"up": SK, "west": ("snout_w", 0), "east": ("snout_e", 0), "down": ("jaw_in", 0)}
        c.box("head", H(0, 0.3, zs1), (6.2 * ws * hs, 5.0 * hs, L1 * hs), rx(-7), sn1, 0)
        sn2 = {"up": SK, "west": ("snout_w", 0), "east": ("snout_e", 0), "down": ("jaw_in", 0), "north": ("snout_front", 0)}
        c.box("head", H(0, -0.7, zs2), (4.8 * ws * hs, 3.9 * hs, L2 * hs), rx(-13), sn2, 0)
    jaw = {"up": ("jaw_in", 0), "west": ("jaw_w", 0), "east": ("jaw_e", 0), "down": SK, "north": SK}
    c.box("jaw", H(0, -2.9, -1.2 - jlen / 2), (4.9 * ws * hs, 2.3 * hs, jlen * hs), rx(-2), jaw, 0)
    for sx in (-1, 1):                                                           # eyes: a bulge and a big slit eye on a card
        if lod < 2:
            c.box("head", H(sx * 3.55 * ws, 1.5, -4.4), (1.6 * hs, 3.4 * hs, 3.6 * hs), I3, {"up": SK, "north": SK, "south": SK, "west": SK, "east": SK})
        reg = "eye_w" if sx < 0 else "eye_e"
        right = vec(0, 0, -1) if sx < 0 else vec(0, 0, 1)
        es = (3.3 if big else 3.9) * hs
        ex = sx * (3.55 * ws + 0.82) if lod < 2 else sx * (3.7 * ws + 0.05)
        c.card("head", H(ex, 1.5, -4.7), right, vec(0, 1, 0), es, es, reg)
        c.glow_card("head", H(ex + sx * 0.02, 1.5, -4.7), right, vec(0, 1, 0), es, es, reg)
    if lod == 0:
        for sx in (-1, 1):
            c.box("head", H(sx * 3.1 * ws, 3.9, -4.2), (2.4 * hs, 1.8 * hs, 6.4 * hs), L.rot_matrix(-8, 0, sx * 22), {"up": SK, "west": SK, "east": SK, "north": SK}, d)       # brow ridge, slanted (the angry look)
            c.spike("head", H(sx * 3.5 * ws, 4.6, -2.6), vec(sx * 0.35, 0.8, 0.5), 3.4 * hs, 1.9 * hs, vec(0, 0, -1), "spike")
            c.box("head", H(sx * 4.1 * ws, -1.7, -1.0), (1.8 * hs, 3.2 * hs, 4.2 * hs), I3, {"up": SK, "west": SK, "east": SK, "north": SK})                                     # the cheek
            right = unit(vec(sx * 0.78, 0, 0.62))                                    # cheek frill: a fan of swept spines (two cards at a slant for volume)
            fk = 1.15 if big else 1.0
            c.card("head", H(sx * 6.8 * ws, -0.2, -0.6), right, unit(vec(-right[2], 0, right[0])), 8.0 * hs * fk, 6.0 * hs * fk, "frill")
            c.card("head", H(sx * 6.4 * ws, 0.6, -0.4), unit(right + vec(0, 0.5, 0)), unit(vec(-right[2], 0, right[0])), 6.4 * hs * fk, 4.8 * hs * fk, "frill")
            for k, z in enumerate((-3.8, -5.8, -7.8)[: 3 if big else 2]):          # the jagged jaw line: small spikes down and back
                c.spike("jaw", H(sx * 2.7 * ws, -3.6, z * (1.0 if big else 0.85)), vec(sx * 0.25, -0.55, 0.8), 2.6 * hs, 1.5 * hs, vec(0, 0, -1), "spike")
            nf = 3 if big else 2
            for k in range(nf):                                                      # fangs standing up from the lower jaw and hanging from the lip
                z = -(6.2 + k * (4.2 if big else 3.0) * sl)
                c.spike("jaw", H(sx * 2.3 * ws, -2.2, z - 0.8), vec(0, 1, -0.2), 2.0 * hs, 1.2 * hs, vec(0, 0, 1), "fang")
                c.spike("head", H(sx * 2.4 * ws, -2.3, z + 1.2), vec(0, -1, 0.1), 2.3 * hs, 1.3 * hs, vec(0, 0, 1), "fang")
    elif lod == 1:
        for sx in (-1, 1):
            c.box("head", H(sx * 3.1 * ws, 3.9, -4.2), (2.4 * hs, 1.8 * hs, 6.4 * hs), L.rot_matrix(-8, 0, sx * 22), {"up": SK, "west": SK, "east": SK})
            right = unit(vec(sx * 0.78, 0, 0.62))
            c.card("head", H(sx * 6.8 * ws, -0.2, -0.6), right, unit(vec(-right[2], 0, right[0])), 8.0 * hs, 6.0 * hs, "frill")
            c.spike("jaw", H(sx * 2.3 * ws, -2.2, -7.0), vec(0, 1, -0.2), 2.0 * hs, 1.2 * hs, vec(0, 0, 1), "fang")
    crest = [(-5.0, 3.6), (-2.6, 4.6), (-0.2, 5.4), (2.0, 4.8)] + ([(4.2, 4.2), (6.4, 3.6)] if big else [])
    for i, (z, hgt) in enumerate(crest):                                              # the swept crest on the skull's top
        if (lod == 2 and i % 2 == 0) or (lod == 1 and i == 0):
            continue
        ang = math.radians(25 + 12 * min(i, 3))
        c.spike("head", H(0, 3.6 - 0.2 * min(i, 3), z), vec(0, math.cos(ang), math.sin(ang)), hgt * hs, 3.0 * hs, vec(0, 0, -1), "spike", cross=lod == 0)
    if big and lod < 2:
        for sx in (-1, 1):                                                           # the long swept horns
            c.spike("head", H(sx * 2.8, 3.0, -0.8), vec(sx * 0.22, 0.72, 0.66), 8.6 * hs, 3.4 * hs, vec(0, 0, -1) * (1 if sx < 0 else -1), "horn", cross=lod == 0)
            if lod == 0:
                c.spike("head", H(sx * 4.2, 1.0, 0.8), vec(sx * 0.7, 0.3, 0.64), 5.2 * hs, 2.6 * hs, vec(0, 0, -1), "horn")
    if c.fx:                                                                        # the furnace in the mouth
        c.m.box("jaw", H(0, -1.72, -1.2 - jlen / 2), (4.7 * ws * hs, 0.0, jlen * hs), I3, {"up": ("jaw_in", 0)}, 0)


# ====================================================================================================================== the small form
SMALL_SPAN = 25.0
SMALL = dict(pal=A.SMALL, k=1.0, S=1, seed=1500, d=6.0)
BIGF = dict(pal=A.BIG, k=1.0, S=2, seed=1600, d=3.4)
FORMS = {"salamander": SMALL, "dragon": BIGF}
SMALL_BARK = 0.68
BIG_SPAN, BIG_BARK = 124.0, 2.3
BIG_HS = 2.4                                                                       # the big head is the small head at this scale


def head_regions(at, pal, seed, S, big):
    z = lambda w, h: np.zeros((h, w, 4), np.float32)
    at.region("head_top", 56 * S, 56 * S, lambda w, h: A.head_top(w, h, pal, seed + 5))
    at.region("skull_w", 64 * S, 48 * S, lambda w, h: A.skull_side(w, h, pal, seed + 2, mirror=True))
    at.region("skull_e", 64 * S, 48 * S, lambda w, h: A.skull_side(w, h, pal, seed + 3, mirror=False))
    at.region("snout_w", 72 * S, 36 * S, lambda w, h: A.snout_side(w, h, pal, seed + 7, mirror=True))
    at.region("snout_e", 72 * S, 36 * S, lambda w, h: A.snout_side(w, h, pal, seed + 8, mirror=False))
    at.region("snout_front", 32 * S, 32 * S, lambda w, h: A.snout_front(w, h, pal, seed + 6))
    at.region("jaw_w", 72 * S, 28 * S, lambda w, h: A.jaw_side(w, h, pal, seed + 9, mirror=True))
    at.region("jaw_e", 72 * S, 28 * S, lambda w, h: A.jaw_side(w, h, pal, seed + 10, mirror=False))
    at.region("jaw_in", 40 * S, 72 * S, lambda w, h: A.jaw_in(w, h, pal, seed + 11))
    at.region("eye_w", 40 * S, 40 * S, lambda w, h: A.eye_card(w, h, pal, seed + 12, mirror=True))
    at.region("eye_e", 40 * S, 40 * S, lambda w, h: A.eye_card(w, h, pal, seed + 13, mirror=False))
    at.region("spike", 32 * S, 64 * S, lambda w, h: A.spike(w, h, pal, seed + 14))
    at.region("claw", 24 * S, 40 * S, lambda w, h: A.claw(w, h, pal, seed + 15))
    at.region("fang", 16 * S, 28 * S, lambda w, h: A.fang(w, h, pal, seed + 16))
    at.region("bone", 64 * S, 16 * S, lambda w, h: (A.bone_bar(w, h, pal, seed + 17), z(w, h)))
    at.region("frill", 64 * S, 48 * S, lambda w, h: A.frill(w, h, pal, seed + 18))
    at.region("flame", 32 * S, 48 * S, lambda w, h: A.flame_lick(w, h, pal, seed + 19))
    if big:
        at.region("horn", 40 * S, 80 * S, lambda w, h: A.horn(w, h, pal, seed + 21))


def small_atlas():
    f = SMALL
    pal, seed = f["pal"], f["seed"]
    at = L.Atlas(512, 512)
    z = lambda w, h: np.zeros((h, w, 4), np.float32)
    back = dict(pal, base="#B0361F", light="#DE5A3E", dark="#6A1C14")                  # the shadowed back: darker plates on the top faces
    at.region("skin", 128, 128, lambda w, h: (A.skin(w, h, pal, seed, cell=18), z(w, h)))
    at.region("skin_back", 128, 128, lambda w, h: (A.skin(w, h, back, seed + 3, cell=18), z(w, h)))
    at.region("tail_skin", 96, 96, lambda w, h: (A.plates(w, h, pal, seed + 20, cell=26, squash=0.95), z(w, h)))
    at.region("tail_back", 96, 96, lambda w, h: (A.plates(w, h, back, seed + 21, cell=26, squash=0.95), z(w, h)))
    at.region("belly", 64, 64, lambda w, h: (A.belly(w, h, pal, seed + 1), z(w, h)))
    head_regions(at, pal, seed, 1, False)
    add_wing_regions(at, pal, seed + 40, SMALL_SPAN, 3.6, False, SMALL_BARK)
    return at


def spine_at(nodes, z):
    """Linear interpolation of (y, half height) along the spine nodes (z decreasing)."""
    for a, b in zip(nodes[:-1], nodes[1:]):
        if b[2] <= z <= a[2]:
            t = (a[2] - z) / (a[2] - b[2])
            return lerp(a[1], b[1], t), lerp(a[3], b[3], t)
    return nodes[0][1], nodes[0][3]


def build_small(lod, atlas, fx=False):
    f = SMALL
    ident = "firecast_salamander" + ("_fx" if fx else "" if lod == 0 else "_lod%d" % lod)
    c = Ctx("salamander", lod, fx, atlas, ident, 0.5 if fx else {0: 1.0, 1: 0.5, 2: 0.25}[lod], f["seed"] + lod, f["d"])
    d = f["d"]
    SK = ("skin", d)
    SB = ("skin_back", d)
    BE = ("belly", d)
    sk = {"up": SB, "west": SK, "east": SK, "down": BE}
    L4 = {"up": SB, "down": SK, "west": SK, "east": SK}

    # ------------------------------------------------------------------ bones
    c.bone("body", (0, 12, 2), "root")
    c.bone("neck1", (0, 11.5, -12.5), "body")
    if lod < 2:
        c.bone("neck2", (0, 10.0, -16.2), "neck1")
    head_parent = "neck2" if lod < 2 else "neck1"
    JH = vec(0, 8.8, -19.5)
    c.bone("head", JH, head_parent, (-7, 0, 0))
    c.bone("jaw", JH + vec(0, -2.0, -1.2), "head")

    # ------------------------------------------------------------------ torso: an arched spine, a deep chest
    nodes = [(0, 10.6, 13.5, 4.1), (0, 12.2, 10.0, 4.4), (0, 16.4, 2.0, 4.6), (0, 14.4, -6.0, 5.2), (0, 11.8, -12.5, 4.2)]     # x, y, z, half height
    widths = [9.0, 9.8, 10.8, 9.6]
    for i in range(4):
        a, b = nodes[i], nodes[i + 1]
        faces = dict(sk)
        if i == 3:
            faces["north"] = SK
        c.limb("body", vec(a[0], a[1], a[2]), vec(b[0], b[1], b[2]), widths[i], (a[3] + b[3]), faces, pad=1.2)
    if lod < 2:
        for sx in (-1, 1):
            c.box("body", vec(sx * 6.4, 14.0, -6.4), (3.6, 7.0, 7.4), np.eye(3), {"up": SK, "west": SK, "east": SK, "north": SK})          # shoulder
            c.box("body", vec(sx * 6.0, 12.6, 9.0), (3.4, 7.0, 7.8), np.eye(3), {"up": SK, "west": SK, "east": SK, "south": SK})          # haunch
    zs = [11.0, 8.0, 5.0, 2.0, -1.0, -4.0, -7.0, -10.0]                                  # spine spikes
    for i, z in enumerate(zs):
        if lod == 2 or (lod == 1 and i % 2):
            continue
        y, hh = spine_at(nodes, z)
        hgt = 3.0 + 1.8 * math.sin((i + 0.5) / len(zs) * math.pi)
        c.spike("body", vec(0, y + hh - 0.5, z), vec(0, 0.9, 0.42), hgt, 2.5, vec(0, 0, -1), "spike", cross=lod == 0)
        if lod == 0:
            for sxx in (-1, 1):                                                      # a second, lower row on each side of the ridge
                c.spike("body", vec(sxx * 2.8, y + hh - 1.2, z - 1.4), vec(sxx * 0.45, 0.8, 0.4), hgt * 0.55, 1.8, vec(0, 0, -1), "spike")

    # ------------------------------------------------------------------ neck
    N = [vec(0, 11.6, -12.5), vec(0, 10.2, -16.4), vec(0, 8.8, -19.8)]
    c.limb("neck1", N[0], N[1], 7.0, 6.6, sk, pad=1.2)
    if lod < 2:
        c.limb("neck2", N[1], N[2], 6.2, 5.8, sk, pad=1.2)
        for i, z in enumerate([-14.0, -17.2]):                                       # the neck's crest: short spikes
            y = lerp(11.6, 8.8, (-z - 12.5) / 7.3) + 3.4
            if lod == 0 or i == 0:
                c.spike("neck1" if i == 0 else "neck2", vec(0, y, z), vec(0, 0.85, 0.5), 3.0, 2.2, vec(0, 0, -1), "spike")

    head_parts(c, JH, 1.0, False, lod, d)

    # ------------------------------------------------------------------ the limbs: forelimbs grip the shoulder's edge, the hind legs are braced
    for sx, tag in ((-1, "R"), (1, "L")):
        Sh, El, Wr = vec(sx * 6.4, 12.8, -6.6), vec(sx * 11.6, 8.4, -9.8), vec(sx * 10.4, 6.4, -14.2)
        Kn = vec(sx * 10.2, 6.0, -17.4)
        c.bone("arm_" + tag, Sh, "body")
        c.limb("arm_" + tag, Sh, El, 4.4, 4.2, L4, pad=1.6)
        c.bone("fore_" + tag, El, "arm_" + tag)
        if lod == 0:
            c.spike("fore_" + tag, El + vec(sx * 1.6, 0.4, 0.8), vec(sx * 0.8, 0.35, 0.45), 3.0, 1.6, vec(0, 0, -1), "spike")                  # a spike on each elbow
        if lod < 2:
            c.box("fore_" + tag, El, (3.6, 3.6, 3.6), L.rot_matrix(0, 45, 0), {"*": SK})                                                    # the elbow knob
        c.limb("fore_" + tag, El, Wr, 3.4, 3.2, L4, pad=1.2)
        if lod < 2:
            c.bone("hand_" + tag, Wr, "fore_" + tag)
            c.limb("hand_" + tag, Wr, Kn, 3.8, 2.2, L4, pad=1.0)
            for j, off in enumerate((-2.6, 0.0, 2.6)):
                st = Kn + vec(off * 0.8, 0.0, 0.0)
                f = L4 if lod == 0 else {"up": SK, "west": SK, "east": SK}
                curl_finger(c, "hand_" + tag, st, vec(off * 0.12, -0.38, -0.9), (2.6, 2.4) if lod == 0 else (4.6,), (1.35, 1.15) if lod == 0 else (1.4,), -30, (1, 0, 0), f, (2.8, 1.5) if lod == 0 else (2.4, 1.4))
        Hp, Kne, Ank = vec(sx * 5.9, 11.6, 9.0), vec(sx * 10.8, 8.6, 12.2), vec(sx * 10.0, 6.0, 8.6)
        c.bone("leg_" + tag, Hp, "body")
        c.limb("leg_" + tag, Hp, Kne, 5.2, 5.4, L4, pad=1.4)
        c.bone("shin_" + tag, Kne, "leg_" + tag)
        c.limb("shin_" + tag, Kne, Ank, 3.4, 3.2, L4, pad=1.2)
        if lod == 0:
            c.spike("leg_" + tag, Kne + vec(sx * 2.4, 0.8, 0.6), vec(sx * 0.8, 0.3, 0.5), 3.4, 1.8, vec(0, 0, -1), "spike")                   # a spike on each knee
        if lod < 2:
            c.bone("foot_" + tag, Ank, "shin_" + tag)
            c.limb("foot_" + tag, Ank, Ank + vec(0, -0.3, -5.6), 3.6, 2.0, L4, pad=0.8)
            for j, off in enumerate((-2.4, 0.0, 2.4)):
                st = Ank + vec(off * 0.8, -0.4, -5.2)
                curl_finger(c, "foot_" + tag, st, vec(off * 0.1, -0.3, -0.95), (2.0, 1.8) if lod == 0 else (3.6,), (1.2, 1.05) if lod == 0 else (1.3,), -32, (1, 0, 0), L4, (2.4, 1.4) if lod == 0 else (2.0, 1.2))

    # ------------------------------------------------------------------ the tail: a column of plates that curls up over the back, a spike on each
    tail_chain(c, d, ("tail_back", "tail_skin"), {0: 12, 1: 6, 2: 4}[lod], 12, (10.6, 15.0), 14.0, 196.0, 58.0, 5.8, 0.93, 4.4, 3.0, lod, barb=(6.0, 3.0))

    # ------------------------------------------------------------------ the wings: raised and swept back on the shoulder blades, four finger bones, scalloped membranes
    for sx, tag in ((-1, "R"), (1, "L")):
        S_pt = vec(sx * 3.8, 15.6, -3.6)
        names = ("wing_" + tag, "wingout_" + tag, ["wf%d_%s" % (k + 1, tag) for k in range(4)])
        c.bone(names[0], S_pt, "body", (22, -sx * 30, sx * 64))
        P = wing_layout(SMALL_SPAN)
        c.bone(names[1], S_pt + vec(sx * P["E"][0], 0, P["E"][1]), names[0], (0, 0, 0))
        for k in range(4):
            c.bone(names[2][k], S_pt + vec(sx * P["W"][0], 0, P["W"][1]), names[1])
        E, Wr, T = add_wing(c, sx, S_pt, SMALL_SPAN, names, lod, False, 1.0, SMALL_BARK)
        if fx:
            for k in (0, 1):
                tip = T[k]
                c.m.card(names[2][k], tip + vec(0, 3.2, 0), vec(sx, 0, 0), vec(0, 1, 0), 5.6, 8.0, ("flame", 0))
                c.m.card(names[2][k], tip + vec(0, 3.2, 0), vec(0, 0, 1), vec(0, 1, 0), 5.6, 8.0, ("flame", 0))
    return c.m


def tail_chain(c, d, region, n, full, start, h0, h1, length, w0, taper, spike_h, spike_w, lod, barb):
    """A tail as a chain of bones t<i> (i = 0, step, 2 step ...) each carrying a box laid between two nodes of the curve, a spike on its outer side, and a barb on the tip bone."""
    pts, heads = tail_path(full, start, h0, h1, length)
    step = full // n
    prev = "body"
    for k in range(n):
        i0, i1 = k * step, (k + 1) * step
        a, b = pts[i0], pts[i1]
        nm = "t%d" % i0
        c.bone(nm, vec(0, a[0], a[1]), prev)
        w = w0 * (taper ** i0)
        w1 = w0 * (taper ** i1)
        h_ = 0.88 * (w + w1) / 2
        av = lerp(heads[i0], heads[min(i1, full) - 1], 0.5)
        nrm = vec(0, math.cos(av), -math.sin(av))
        ru, rs = (region if isinstance(region, tuple) else (region, region))
        sd = {"up": (ru, d), "west": (rs, d), "east": (rs, d), "down": (rs, d)}
        c.limb(nm, vec(0, a[0], a[1]), vec(0, b[0], b[1]), (w + w1) / 2, h_, sd, up=nrm, pad=w0 * 0.2, d=d)
        mid = (a + b) / 2
        sp_dir = unit(0.55 * vec(0, 1, 0) + 0.45 * (-nrm))
        sh = max(2.0, spike_h * (1.0 - 0.45 * i0 / full)) * (1.0 if lod == 0 else 1.15)
        top = vec(0, mid[0], mid[1]) + (-nrm) * (h_ / 2 - 0.2 * spike_h * 0.1)
        c.spike(nm, top, sp_dir, sh, spike_w * (0.9 + 0.1 * (1 - i0 / full)), vec(0, 0, -1), "spike", cross=lod == 0 and k < n * 2 // 3)
        if lod == 0 and k < n * 3 // 4:                                              # side spines: a pair on each segment, swept out and back
            hv = vec(0, math.sin(av), math.cos(av))
            for sxx in (-1, 1):
                sdir = unit(vec(sxx * 0.85, 0.0, 0.0) + 0.3 * hv + 0.25 * (-nrm))
                c.spike(nm, vec(0, mid[0], mid[1]) + vec(sxx * (w + w1) / 4.0, 0, 0), sdir, max(1.6, sh * 0.5), spike_w * 0.7, hv, "spike")
        prev = nm
    last = pts[full]
    c.bone("tail_tip", vec(0, last[0], last[1]), prev)
    av = heads[-1]
    barb_dir = unit(vec(0, math.sin(av), math.cos(av)) * 0.8 + vec(0, 0.7, 0))
    c.spike("tail_tip", vec(0, last[0], last[1]) - barb_dir * 0.5, barb_dir, barb[0], barb[1], vec(0, 0, -1), "claw", cross=lod < 2)
    if c.fx:                                                                         # a lick of flame off the barb
        up = vec(0, 1, 0)
        base = vec(0, last[0], last[1]) + barb_dir * barb[0] * 0.5
        c.m.card("tail_tip", base + up * barb[0] * 0.7, vec(0, 0, 1), up, barb[1] * 2, barb[0] * 1.5, ("flame", 0))
        c.m.card("tail_tip", base + up * barb[0] * 0.7, vec(1, 0, 0), up, barb[1] * 2, barb[0] * 1.5, ("flame", 0))
    return pts


# ====================================================================================================================== the big form
def big_atlas():
    f = BIGF
    pal, seed = f["pal"], f["seed"]
    at = L.Atlas(1024, 1024)
    z = lambda w, h: np.zeros((h, w, 4), np.float32)
    at.region("skin", 256, 256, lambda w, h: (A.skin(w, h, pal, seed), z(w, h)))
    at.region("belly", 96, 96, lambda w, h: (A.belly(w, h, pal, seed + 1), z(w, h)))
    at.region("torso_front", 216, 248, lambda w, h: A.torso_front(w, h, pal, seed + 30))
    at.region("muscle_side", 96, 128, lambda w, h: A.muscle_side(w, h, pal, seed + 32))
    at.region("neck_front", 64, 160, lambda w, h: A.neck_front(w, h, pal, seed + 33))
    head_regions(at, pal, seed, 2, True)
    add_wing_regions(at, pal, seed + 40, BIG_SPAN, 1.7, True, BIG_BARK)
    return at


def build_dragon(lod, atlas, fx=False):
    f = BIGF
    ident = "firecast_dragon" + ("_fx" if fx else "" if lod == 0 else "_lod%d" % lod)
    c = Ctx("dragon", lod, fx, atlas, ident, 0.5 if fx else {0: 1.0, 1: 0.5, 2: 0.25}[lod], f["seed"] + lod, f["d"])
    d = f["d"]
    SK = ("skin", d)
    BE = ("belly", d)
    MU = ("muscle_side", 0)
    I3 = np.eye(3)
    sk = {"up": SK, "west": SK, "east": SK, "down": SK}
    L4 = {"up": SK, "down": SK, "west": SK, "east": SK}

    c.bone("body", (0, 26, 0), "root")
    c.bone("chest", (0, 44, -5), "body")
    N = [vec(0, 60, -8), vec(0, 73, -12), vec(0, 87, -18), vec(0, 98, -27)]
    JH = vec(0, 103, -36)
    nb = ["neck1", "neck2", "neck3", "neck4"]
    chain = {0: [0, 1, 2, 3], 1: [0, 2], 2: [0]}[lod]
    prev = "chest"
    pts = N + [JH]
    for k in chain:
        c.bone(nb[k], N[k], prev)
        prev = nb[k]
    c.bone("head", JH, prev, (-16, 0, 0))
    c.bone("jaw", JH + vec(0, -2.0, -1.2) * BIG_HS, "head", (-9, 0, 0))

    # ------------------------------------------------------------------ torso: a stack of slices (a hips-to-collar-bone profile: narrow waist, broad chest) wearing ONE painted front (chest, ribs, abs)
    def slices(rows, lean):
        for (yc, hgt, wd, dp) in rows:
            v0, v1 = (60.0 - (yc + hgt / 2)) / 62.0, (60.0 - (yc - hgt / 2)) / 62.0
            u0, u1 = (27.0 - wd / 2) / 54.0, (27.0 + wd / 2) / 54.0
            u0, u1 = max(0.0, u0), min(1.0, u1)
            bone = "chest" if yc >= 36 else "body"
            zc = 1.0 - 0.15 * (yc - 2.0)
            faces = {"west": SK, "east": SK, "south": SK, "north": ("torso_front", 0, False, (u0, max(0.0, v0), u1, min(1.0, v1)))}
            if yc < 6:
                faces["down"] = BE
            if yc > 50:
                faces["up"] = SK
            c.box(bone, vec(0, yc, zc), (wd, hgt, dp), rx(lean), faces)
    rx = lambda a: L.rot_matrix(a, 0, 0)
    if lod == 0:
        slices([(3, 14, 28, 21), (12, 10.5, 26, 19), (20, 10.5, 25, 19), (28, 10.5, 28, 20), (36, 10.5, 34, 22), (44, 10.5, 42, 23.5), (52, 10.5, 52, 25)], -6)
    elif lod == 1:
        slices([(4, 16, 27, 20), (18, 15, 25.5, 19), (32, 15, 32, 21), (45, 15, 44, 24), (54, 8, 52, 25)], -6)
    else:
        c.box("body", vec(0, 22, -2.2), (28, 30, 20), rx(-5), {"west": SK, "east": SK, "south": SK, "north": ("torso_front", 0, False, (0.1, 0.35, 0.9, 1.0))})
        c.box("chest", vec(0, 47, -6.8), (44, 26, 23), rx(-9), {"up": SK, "west": SK, "east": SK, "south": SK, "north": ("torso_front", 0, False, (0.0, 0.0, 1.0, 0.4))})
    if lod == 0:                                                                        # relief: heavy pectorals and six blocks of abs standing proud of the slices, each wearing its piece of the painted front
        def zf(y):
            dep = 21.0 + (y - 20.0) * 0.1
            return 1.0 - 0.15 * (y - 2.0) - dep / 2.0
        for sx in (-1, 1):
            xs = (1.0, 21.5) if sx > 0 else (-21.5, -1.0)
            u0, u1 = (27.0 + xs[0]) / 54.0, (27.0 + xs[1]) / 54.0
            y0, y1 = 43.2, 57.0
            c.box("chest", vec(sx * 11.2, (y0 + y1) / 2, zf(50) + 0.6), (xs[1] - xs[0], y1 - y0, 6.0), L.rot_matrix(-10, 0, sx * 7),
                  {"up": SK, "down": SK, "west": SK, "east": SK, "north": ("torso_front", 0, False, (u0, (60.0 - y1) / 62.0, u1, (60.0 - y0) / 62.0))})
            xs2 = (1.2, 10.6) if sx > 0 else (-10.6, -1.2)
            ua, ub = (27.0 + xs2[0]) / 54.0, (27.0 + xs2[1]) / 54.0
            for k in range(3):
                ya, yb = 29.0 - k * 7.2 - 6.2, 29.0 - k * 7.2
                c.box("body", vec(sx * 5.9, (ya + yb) / 2, zf((ya + yb) / 2) + 0.9), (xs2[1] - xs2[0], yb - ya, 3.4), rx(-6),
                      {"up": SK, "down": SK, "west": SK, "east": SK, "north": ("torso_front", 0, False, (ua, (60.0 - yb) / 62.0, ub, (60.0 - ya) / 62.0))})
    if lod < 2:
        for sx in (-1, 1):
            c.box("chest", vec(sx * 30.5, 55, -6.5), (18, 17, 19), L.rot_matrix(0, 0, sx * 18), {"up": SK, "west": MU, "east": MU, "north": SK, "south": SK})        # the deltoids
            c.box("chest", vec(sx * 13, 61.5, -7.5), (24, 9, 18), L.rot_matrix(-8, 0, -sx * 16), {"up": SK, "north": SK, "west": SK, "east": SK})                        # the trapezius

    # ------------------------------------------------------------------ the long neck: boxes laid between the nodes, the throat toward the front, spines down the back
    widths = [(25.0, 21.0), (20.0, 18.0), (17.0, 15.0), (15.0, 13.5)]
    nf = {"up": SK, "west": SK, "east": SK, "down": ("neck_front", 0)}
    segs = {0: [(0, 1, 0), (1, 2, 1), (2, 3, 2), (3, 4, 3)], 1: [(0, 2, 0), (2, 4, 2)], 2: [(0, 4, 0)]}[lod]
    for a, b, wi in segs:
        w, h = widths[wi]
        if lod == 1:
            w, h = w * 0.96, h * 0.96
        c.limb(nb[a], pts[a], pts[b], w, h, nf, pad=2.0, d=d)
    for i, k in enumerate([0, 0.33, 0.66, 1.0, 1.33, 1.66, 2.0, 2.33, 2.66, 3.33]):
        if (lod == 2) or (lod == 1 and i % 2):
            continue
        a = int(min(3, k))
        t = k - a
        pos = lerp(pts[a], pts[a + 1], t)
        bn = nb[a] if nb[a] in nb and a in chain else nb[max(x for x in chain if x <= a)]
        c.spike(bn, vec(pos[0], pos[1] + 7.2 - 0.7 * i * 0.3, pos[2]), vec(0, 0.85, 0.55), 8.6 - 0.45 * i, 5.0, vec(0, 0, -1), "spike", cross=lod == 0)

    head_parts(c, JH, BIG_HS, True, lod, d)

    # ------------------------------------------------------------------ arms: heavy upper arms, forearms forward, big hands with red claws
    for sx, tag in ((-1, "R"), (1, "L")):
        Sh, El, Wr = vec(sx * 30, 52, -6), vec(sx * 36, 34, -12), vec(sx * 25, 22, -27)
        Pa = vec(sx * 23, 18.0, -35)
        c.bone("arm_" + tag, Sh, "chest")
        c.limb("arm_" + tag, Sh, El, 13.5, 13.0, {"up": SK, "down": SK, "west": MU, "east": MU}, pad=3.0, d=d)
        c.bone("fore_" + tag, El, "arm_" + tag)
        if lod < 2:
            c.box("fore_" + tag, El, (10, 10, 10), L.rot_matrix(0, 45, 0), {"*": SK})
        c.limb("fore_" + tag, El, Wr, 10.5, 10.0, {"up": SK, "down": SK, "west": MU, "east": MU}, pad=2.4, d=d)
        if lod == 0:
            fd = unit(Wr - El)
            for t_ in (0.32, 0.56, 0.8):                                               # blade spines along the outer edge of the forearm
                c.spike("fore_" + tag, El + (Wr - El) * t_ + vec(sx * 5.0, 0.4, 0), unit(vec(sx * 0.55, 0.25, 0.25) + fd * 0.45), 7.5 - 2.0 * t_, 3.4, fd, "spike")
            c.spike("chest", vec(sx * 31, 62.5, -6.0), vec(sx * 0.35, 0.9, 0.2), 9.0, 4.6, vec(0, 0, 1), "spike", cross=True)       # a spike on each shoulder
        if lod < 2:
            c.bone("hand_" + tag, Wr, "fore_" + tag)
            c.limb("hand_" + tag, Wr, Pa, 10.0, 5.0, L4, pad=2.0, d=d)
            for j, off in enumerate((-4.5, -1.5, 1.5, 4.5)):
                st = Pa + vec(off * 0.7, 0.0, -1.0)
                dv0 = vec(off * 0.07, -0.55, -0.83)
                f = L4 if lod == 0 else {"up": SK, "west": SK, "east": SK}
                curl_finger(c, "hand_" + tag, st, dv0, (6.0, 5.4) if lod == 0 else (10.5,), (3.2, 2.7) if lod == 0 else (3.4,), -24, (1, 0, 0), f, (6.4, 3.4) if lod == 0 else (5.4, 3.0), cross=lod == 0, d=d)

    # ------------------------------------------------------------------ legs: tucked, thighs forward, shins down, clawed feet
    for sx, tag in ((-1, "R"), (1, "L")):
        Hp, Kne, Ank = vec(sx * 11, -1, 1), vec(sx * 18, -7, -22), vec(sx * 15, -27, -14)
        c.bone("leg_" + tag, Hp, "body")
        c.limb("leg_" + tag, Hp, Kne, 16, 16, {"up": SK, "down": SK, "west": MU, "east": MU}, pad=4.0, d=d)
        c.bone("shin_" + tag, Kne, "leg_" + tag)
        if lod < 2:
            c.box("shin_" + tag, Kne, (11, 11, 11), L.rot_matrix(0, 45, 0), {"*": SK})
        c.limb("shin_" + tag, Kne, Ank, 11.0, 10.0, {"up": SK, "down": SK, "west": MU, "east": MU}, pad=2.4, d=d)
        if lod == 0:
            c.spike("leg_" + tag, Kne + vec(sx * 8.0, 1.0, -1.0), vec(sx * 0.75, 0.25, -0.4), 8.0, 4.0, vec(0, 0, 1), "spike")           # a spike on each knee
            c.spike("shin_" + tag, Ank + vec(0, 3.0, 5.0), vec(0, 0.3, 1.0), 6.0, 3.4, vec(1, 0, 0), "spike")                           # a spur on each heel
        if lod < 2:
            c.bone("foot_" + tag, Ank, "shin_" + tag)
            c.limb("foot_" + tag, Ank, Ank + vec(0, -4.0, -15.0), 10.5, 5.0, L4, pad=1.6, d=d)
            for j, off in enumerate((-3.4, 0.0, 3.4)):
                st = Ank + vec(off * 0.8, -4.0, -14.0)
                curl_finger(c, "foot_" + tag, st, vec(off * 0.05, -0.3, -0.95), (4.6, 4.0) if lod == 0 else (8.0,), (3.3, 2.9) if lod == 0 else (3.2,), -30, (1, 0, 0), L4, (5.4, 3.2) if lod == 0 else (4.4, 2.8), cross=lod == 0, d=d)

    # ------------------------------------------------------------------ the tail: a long spiny chain that trails down and sweeps up
    tail_chain(c, d, ("skin", "skin"), {0: 14, 1: 7, 2: 4}[lod], 14, (-1.0, 9.0), -22.0, 26.0, 150.0, 16.0, 0.915, 9.0, 5.0, lod, barb=(11.0, 5.5))

    # ------------------------------------------------------------------ the wings: huge, four long finger bones, veined membrane
    for sx, tag in ((-1, "R"), (1, "L")):
        S_pt = vec(sx * 14, 56, 3.0)
        names = ("wing_" + tag, "wingout_" + tag, ["wf%d_%s" % (k + 1, tag) for k in range(4)])
        c.bone(names[0], S_pt, "chest", (40, -sx * 10, sx * 46))
        P = wing_layout(BIG_SPAN, True)
        c.bone(names[1], S_pt + vec(sx * P["E"][0], 0, P["E"][1]), names[0], (0, 0, 0))
        for k in range(4):
            c.bone(names[2][k], S_pt + vec(sx * P["W"][0], 0, P["W"][1]), names[1])
        E, Wr, T = add_wing(c, sx, S_pt, BIG_SPAN, names, lod, True, 2.2, BIG_BARK)
        if c.fx:
            for k in (0, 1, 2):
                tip = T[k]
                c.m.card(names[2][k], tip + vec(0, 7.0, 0), vec(sx, 0, 0), vec(0, 1, 0), 12.0, 17.0, ("flame", 0))
                c.m.card(names[2][k], tip + vec(0, 7.0, 0), vec(0, 0, 1), vec(0, 1, 0), 12.0, 17.0, ("flame", 0))
    return c.m


# ====================================================================================================================== animations
def animations(form):
    """The four clips of a form (idle, perch, fly: loops; breathe: once). Rotations are ADDED to the bones' rest rotations: the wings flap about the body axis (z), the fingers fan about the wing's own normal,
    the tail and the neck travel as waves down their chains. The wings and tail move by bone rotation only (no per-frame geometry)."""
    big = form == "dragon"
    ntail = 14 if big else 12
    necks = ["neck1", "neck2", "neck3", "neck4"] if big else ["neck1", "neck2"]
    K = 2.1 if big else 1.0
    sine = lambda length, amp, axis=0, phase=0.0, st=12, bias=0.0: L.sine(length, amp, axis, phase, st, bias=bias)
    zero = lambda length, st=12: {t: (0, 0, 0) for t in L.sine(length, 0, 0, 0.0, st)}

    def tail(length, amp, lag, st=12):
        tr = {}
        for i in range(ntail):
            tr["t%d" % i] = {"rotation": L.merge(sine(length, amp * (0.5 + 0.7 * i / ntail), 1, -lag * i, st), sine(length, amp * 0.3, 0, -lag * i - 0.12, st))}
        tr["tail_tip"] = {"rotation": sine(length, amp, 1, -lag * ntail, st)}
        return tr

    def wings(length, flap, offset=0.0, fin=3.0, lag=0.10, st=12):
        tr = {}
        for sx, tag in ((-1, "R"), (1, "L")):
            tr["wing_" + tag] = {"rotation": L.merge(sine(length, -sx * flap, 2, 0.0, st), {t: (0, 0, sx * offset) for t in zero(length, st)})}
            tr["wingout_" + tag] = {"rotation": sine(length, -sx * flap * 0.55, 2, -lag, st)}
            for k in range(4):
                tr["wf%d_%s" % (k + 1, tag)] = {"rotation": sine(length, sx * fin * (0.5 + 0.35 * k), 1, -lag * (k + 1), st)}
        return tr

    def neck(length, amp, lag, st=12):
        return {nb: {"rotation": sine(length, amp, 1, -lag * i, st)} for i, nb in enumerate(necks)}

    clips = {}
    # idle: slow breathing, the tail sways, the wings twitch, the head turns
    T = 3.2 if big else 2.6
    bones = {"body": {"scale": {t: (1.0, 1.0 + 0.02 * math.sin(t / T * math.tau), 1.0 + 0.012 * math.sin(t / T * math.tau)) for t in sine(T, 0, 0)}},
             "head": {"rotation": L.merge(sine(T, 2.5, 0, 0.1), sine(T, 3.5, 1, 0.0))}, "jaw": {"rotation": sine(T, 1.5, 0, 0.3, bias=-2.0 if not big else -0.0)}}
    bones.update(neck(T, 1.8, 0.04))
    bones.update(tail(T, 3.0, 0.05))
    bones.update(wings(T, 3.0 if big else 2.5, 0.0, 2.0))
    clips["idle"] = {"loop": True, "length": T, "bones": bones}

    # perch: small form clinging to the shoulder (the hands tighten, the head looks about); the big form's slower hover
    PT = 3.6 if not big else 4.0
    bones = {"head": {"rotation": L.merge(sine(PT, 12.0 if not big else 6.0, 1, 0.0), sine(PT, 3.0, 0, 0.2))}, "jaw": {"rotation": sine(PT, 1.5, 0, 0.3, bias=-2.0)},
             "body": {"scale": {t: (1.0, 1.0 + 0.018 * math.sin(t / PT * math.tau), 1.0) for t in sine(PT, 0, 0)}, "rotation": sine(PT, 1.2, 2, 0.0)}}
    bones.update(neck(PT, 4.0, 0.05))
    for sx, tag in ((-1, "R"), (1, "L")):
        bones["hand_" + tag] = {"rotation": sine(PT, 3.0, 0, 0.2 + 0.1 * sx)}
        bones["fore_" + tag] = {"rotation": sine(PT, 1.5, 0, 0.1 + 0.05 * sx)}
    bones.update(tail(PT, 3.0, 0.05))
    bones.update(wings(PT, 3.0 if not big else 4.0, 0.0, 2.5))
    clips["perch"] = {"loop": True, "length": PT, "bones": bones}

    # fly: the wings beat (the small form's open flat first), the legs dangle, the body bobs, the tail streams
    FT = 1.4 if big else 0.8
    bones = {"body": {"position": {t: (0, 0.9 * K * math.sin((t / FT) * math.tau + 0.6), 0) for t in sine(FT, 0, 0)}},
             "head": {"rotation": sine(FT, 4.0, 0, 0.2)}, "jaw": {"rotation": {t: (-3.0 - (6.0 if big else 0.0), 0, 0) for t in zero(FT)}}}
    bones.update(neck(FT, 2.5, 0.08))
    for sx, tag in ((-1, "R"), (1, "L")):
        if big:
            bones["leg_" + tag] = {"rotation": sine(FT, 4.0, 0, 0.1 * sx)}
            bones["arm_" + tag] = {"rotation": sine(FT, 3.0, 0, 0.3)}
        else:
            for nm, v in (("arm_", (-30, 0, 0)), ("leg_", (-28, 0, 0))):
                bones[nm + tag] = {"rotation": {t: v for t in zero(FT)}}
    bones.update(tail(FT, 6.0 if not big else 5.0, 0.05))
    bones.update(wings(FT, 34.0 if not big else 26.0, -58.0 if not big else 0.0, 5.0 if not big else 4.0, 0.14))
    clips["fly"] = {"loop": True, "length": FT, "bones": bones}

    # breathe: the head draws back, the jaw opens wide, the wings spread, then the strike forward; plays once
    BT = 2.0 if big else 1.6
    tt = [0.0, 0.25 * BT, 0.45 * BT, 0.55 * BT, 0.8 * BT, BT]
    z6 = lambda vals: dict(zip(tt, vals))
    bones = {"head": {"rotation": z6([(0, 0, 0), (14, 0, 0), (-8, 0, 0), (-12, 0, 0), (-4, 0, 0), (0, 0, 0)])},
             "jaw": {"rotation": z6([(-1, 0, 0), (-6, 0, 0), (-34, 0, 0), (-36, 0, 0), (-14, 0, 0), (-1, 0, 0)])},
             "body": {"rotation": z6([(0, 0, 0), (-4, 0, 0), (3, 0, 0), (4, 0, 0), (2, 0, 0), (0, 0, 0)])}}
    for i, nb in enumerate(necks):
        bones[nb] = {"rotation": z6([(0, 0, 0), (9 / (1 + 0.3 * i), 0, 0), (-4, 0, 0), (-7, 0, 0), (-3, 0, 0), (0, 0, 0)])}
    if big:
        bones["chest"] = {"scale": z6([(1, 1, 1), (1.06, 1.06, 1.04), (1.0, 1.0, 1.0), (0.98, 0.98, 1.0), (1.0, 1, 1), (1, 1, 1)])}
        for sx, tag in ((-1, "R"), (1, "L")):
            bones["arm_" + tag] = {"rotation": z6([(0, 0, 0), (-10, 0, sx * -8), (-24, 0, sx * -14), (-24, 0, sx * -14), (-10, 0, sx * -6), (0, 0, 0)])}
    for sx, tag in ((-1, "R"), (1, "L")):
        bones["wing_" + tag] = {"rotation": z6([(0, 0, 0), (0, 0, sx * 36), (0, 0, sx * 22), (0, 0, sx * 18), (0, 0, sx * 28), (0, 0, 0)])}
        bones["wingout_" + tag] = {"rotation": z6([(0, 0, 0), (0, 0, sx * 14), (0, 0, sx * 6), (0, 0, sx * 4), (0, 0, sx * 8), (0, 0, 0)])}
        for k in range(4):
            bones["wf%d_%s" % (k + 1, tag)] = {"rotation": z6([(0, 0, 0), (0, sx * -8 * (k + 1) / 2, 0), (0, sx * -4, 0), (0, sx * -3, 0), (0, sx * -5, 0), (0, 0, 0)])}
    bones.update({"t%d" % i: {"rotation": z6([(0, 0, 0), (0, 6 + i * 0.5, 0), (0, -6 - i * 0.5, 0), (0, 6, 0), (0, 0, 0), (0, 0, 0)])} for i in range(ntail)})
    clips["breathe"] = {"loop": False, "length": BT, "bones": bones}
    return clips


# ====================================================================================================================== main
LOD_JAVA = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "java", "com", "newuniverse", "nusmp", "firecast", "FireCastLod.java")


def sync_java(counts):
    """--sync: writes the quad counts of the generated models into the table of firecast/FireCastLod.java (FireCastLodTest holds the two equal)."""
    import re
    with open(LOD_JAVA) as f:
        src = f.read()
    sal, dra = counts["salamander"], counts["dragon"]
    src = re.sub(r"SALAMANDER_QUADS = \{[^}]*\}, DRAGON_QUADS = \{[^}]*\};", "SALAMANDER_QUADS = {%d, %d, %d}, DRAGON_QUADS = {%d, %d, %d};" % (sal[0], sal[1], sal[2], dra[0], dra[1], dra[2]), src)
    src = re.sub(r"SALAMANDER_FX_QUADS = \d+, DRAGON_FX_QUADS = \d+;", "SALAMANDER_FX_QUADS = %d, DRAGON_FX_QUADS = %d;" % (sal[3], dra[3]), src)
    with open(LOD_JAVA, "w", newline="\n") as f:
        f.write(src)
    print("synced", LOD_JAVA)


def main():
    count_only = "--count" in sys.argv
    only = [a for a in sys.argv[1:] if not a.startswith("--")]
    counts = {}
    for form, atlas, builder in (("salamander", small_atlas, build_small), ("dragon", big_atlas, build_dragon)):
        if only and form not in only:
            continue
        at = atlas()
        rows = []
        for lod, fx in ((0, False), (1, False), (2, False), (0, True)):
            m = builder(lod, at, fx)
            rows.append((m.id, m.cubes(), m.quads()))
            if not count_only:
                m.save()
                if fx:
                    at.save_glow(m.id, 0.5)
                else:
                    at.save_color(m.id, m.tex_scale)
        if not count_only:
            L.write_animations("firecast_" + form, animations(form))
        for r in rows:
            print("%-28s cubes %4d  quads %5d" % r)
        counts[form] = [r[2] for r in rows]                                           # full, half, low, glow-only
    if "--sync" in sys.argv and len(counts) == 2:
        sync_java(counts)


if __name__ == "__main__":
    main()
