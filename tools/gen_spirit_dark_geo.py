"""The Spirit of Darkness (docs/magic/spirit_dark.md), both forms: the smoky shadow-wisp COMPANION (about 0.9 block, hovers by the owner's shoulder) and the POWERED form, a miniature aristocratic
demon (about 2.3 blocks: horns, bat wings, a spaded tail, a black-violet tailcoat, glowing crimson eyes). Models are Bedrock geo (1/16 block units, y up, front -z, +x the model's left), the skins are
hand-tuned cel-shaded sheets painted by tools/spirit_dark_art.py, three levels of detail per form and a glow-only model (the additive pass: eyes, grin, runes, wing seams, smoke wisps):

    python3 -B tools/gen_spirit_dark_geo.py            writes assets/nusmp/{geo,animations,textures}/entity/spirit_dark_{companion,powered}[_lod1|_lod2|_fx]*
    python3 -B tools/gen_spirit_dark_geo.py --count    prints the quads of every model

All levels and the glow-only model use the one sheet of their form (a lower level reads a smaller copy of the same layout), the same bones, so one animation file drives them all. Wings, tail, coat,
tendrils and smoke move by bone rotation only (no per-frame geometry). Clips: companion idle, cast, appear, vanish; powered idle, flap, cast, appear, vanish.
"""
import math
import os
import sys

import numpy as np

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import spirit_dark_geo_lib as L  # noqa: E402
import spirit_dark_art as A  # noqa: E402

FACES = ("up", "front", "side", "back", "down")
MATS = ("coat", "hem", "skin", "horn", "boot", "violet", "bone", "lace", "ink")
TILE = 40
V = np.array


def mat_faces(name, skip=(), only=None):
    """The six faces of a box from the tiles of a material: up/down by their light, north = front (-z), south = back, west and east = side."""
    m = {"up": (name + "_up", 0), "down": (name + "_down", 0), "north": (name + "_front", 0), "south": (name + "_back", 0), "west": (name + "_side", 0), "east": (name + "_side", 0)}
    if only is not None:
        m = {k: v for k, v in m.items() if k in only}
    for k in skip:
        m.pop(k, None)
    return m


def add_mats(at, names, S=1, extra_seed=0):
    for n in names:
        for i, f in enumerate(FACES):
            at.region("%s_%s" % (n, f), TILE * S, TILE * S, (lambda nn, ff, sd: (lambda w, h: A.mat(nn, w, h, ff, sd)))(n, f, 100 + extra_seed + i * 7 + len(n)))


# ====================================================================================================================== the powered form
# the left wing's skeleton (the right is the mirror): shoulder root, elbow, wrist, three finger tips and the anchor on the hip
WR, WE, WW = V((3.2, 22.0, 3.2)), V((9.5, 27.0, 5.0)), V((17.5, 31.5, 6.0))
WF = [V((29.0, 28.5, 9.5)), V((26.5, 17.5, 12.0)), V((18.0, 9.0, 11.0))]
WH = V((4.0, 13.5, 4.2))
PANELS = [(WW, WF[0], WF[1]), (WW, WF[1], WF[2]), (WW, WF[2], WH), (WW, WH, WR)]


def panel_frame(p0, a, b):
    """Plate geometry of the membrane triangle (apex p0, tips a and b) and the triangle's uv in the card: (centre, u, v, w, h, tri)."""
    u = L.unit(a - p0)
    d = b - p0
    vv = d - u * np.dot(d, u)
    v = L.unit(vv)
    la, bu, bv = float(np.linalg.norm(a - p0)), float(np.dot(d, u)), float(np.dot(d, v))
    x0, x1 = min(0.0, bu), max(la, bu)
    w, h = x1 - x0, bv
    centre = p0 + u * (x0 + x1) / 2 + v * bv / 2
    tri = [((0 - x0) / w, 1.0), ((la - x0) / w, 1.0), ((bu - x0) / w, 1.0 - bv / h)]
    return centre, u, v, w, h, tri


def powered_atlas():
    at = L.Atlas(512, 512)
    add_mats(at, MATS)
    at.region("head_side_l", 64, 64, lambda w, h: A.head_side(w, h, 22, False))
    at.region("head_side_r", 64, 64, lambda w, h: A.head_side(w, h, 23, True))
    at.region("hair_top", 48, 48, lambda w, h: A.hair_top(w, h, 24))
    at.region("chest_front", 96, 88, lambda w, h: A.chest_front(w, h, 25))
    at.region("back_coat", 96, 96, lambda w, h: A.back_coat(w, h, 26))
    at.region("jabot", 48, 56, lambda w, h: A.jabot(w, h, 27))
    at.region("rune_arm", 56, 40, lambda w, h: A.rune_arm(w, h, 28))
    for i, (p0, a, b) in enumerate(PANELS):
        tri = panel_frame(p0, a, b)[5]
        at.region("mem_%d" % i, 72, 72, (lambda t, sd: (lambda w, h: A.membrane(w, h, t, sd)))(tri, 30 + i))
    at.region("puff", 64, 64, lambda w, h: A.puff_glow(w, h, A.GLOW_V, 170.0, 41))
    at.region("tendril", 32, 64, lambda w, h: A.tendril_glow(w, h, A.GLOW_V, 210.0))
    at.region("ring", 96, 96, lambda w, h: ring_glow(w, h))
    at.region("hemline", 48, 12, lambda w, h: A.hemline(w, h))
    at.region("eye_card", 32, 16, lambda w, h: A.eye_glow(w, h, A.GLOW_R, (255, 200, 220)))
    return at


def ring_glow(w, h):
    """The ground ring of smoke the powered form hovers over: a soft violet torus with a few rune ticks, alpha 0 at the border."""
    c, g = A.blank(w, h)
    x, y = A.grid(w, h)
    u, v = (x + 0.5) / w * 2 - 1, (y + 0.5) / h * 2 - 1
    r = np.sqrt(u * u + v * v)
    a = A.smooth(1 - np.abs(r - 0.68) / 0.22) * A.smooth(1 - r)
    ang = np.arctan2(v, u)
    ticks = (np.cos(ang * 12) > 0.6) & (np.abs(r - 0.5) < 0.06)
    a = np.clip(a * 0.8 + ticks * 0.6, 0, 1)
    g[..., :3] = A.GLOW_V
    g[..., 3] = a * 170
    g[g[..., 3] < 1.0] = 0
    return c, g


def build_powered(lod, atlas, fx=False):
    ident = "spirit_dark_powered" + ("_fx" if fx else "" if lod == 0 else "_lod%d" % lod)
    m = L.Model(ident, atlas, seed=3000 + lod, tex_scale=0.5 if fx else {0: 1.0, 1: 0.5, 2: 0.25}[lod])
    full, mid, low = lod == 0, lod <= 1, lod == 2
    NS = ("north", "south")                                          # the end caps of a laid segment (its length runs along local z)

    def B(name, pivot, parent="body", rotation=None):
        m.bone(name, pivot, parent, rotation)

    def C(bone, o, s, faces, rot=None, pivot=None):
        if fx:
            return None
        return m.cube(bone, o, s, faces, rot, pivot, d=0)

    def S(bone, p0, p1, t, faces, t2=None, up=(0, 1, 0), roll=0.0, grow=0.3):
        if fx:
            return None
        p0, p1 = V(p0, dtype=float), V(p1, dtype=float)
        dv = L.unit(p1 - p0) * grow
        return m.seg(bone, p0 - dv, p1 + dv, t, faces, t2, up, d=0, roll=roll)

    def X(bone, o, s, faces, rot=None, pivot=None):
        """a glow card (the fx model only)"""
        if fx:
            return m.cube(bone, o, s, faces, rot, pivot, d=0)
        return None

    def XP(bone, centre, u, v, w, h, faces, t=0.1):
        if fx:
            return m.plate(bone, centre, u, v, w, h, t, faces, d=0)
        return None

    # ---------------------------------------------------------------- the skeleton (every model has all the bones)
    m.bone("body", (0, 14, 0), "root")
    B("hips", (0, 14, 0))
    B("chest", (0, 18, 0), "hips")
    B("neck", (0, 24, 0), "chest")
    B("head", (0, 26, 0), "neck")
    B("orbit", (0, 20, 0), "body")
    for s, t in ((1, "L"), (-1, "R")):
        B("arm_" + t, (s * 6.6, 22.4, 0), "chest")
        B("fore_" + t, (s * 8.0, 16.8, 0.4), "arm_" + t)
        B("hand_" + t, (s * 7.2, 12.4, -3.2), "fore_" + t)
        B("leg_" + t, (s * 2.6, 11.5, 0), "hips")
        B("shin_" + t, (s * 2.6, 6.4, 0.3), "leg_" + t)
        B("wing_" + t, (s * WR[0], WR[1], WR[2]), "chest")
        B("wing2_" + t, (s * WE[0], WE[1], WE[2]), "wing_" + t)
        B("coatF_" + t, (s * 2.6, 14.8, -3.0), "hips")
        B("coatB_" + t, (s * 2.4, 14.8, 2.8), "hips")
        B("coatS_" + t, (s * 4.4, 14.0, 0.0), "hips")
        B("wisp_" + t, (s * 5.0, 6.0, 0.0), "hips")
    tail_pts = [V(p) for p in ((0, 13.2, 3.0), (0, 11.6, 6.2), (0, 9.2, 9.0), (0, 6.8, 10.8), (0, 5.0, 13.4), (0, 4.4, 16.6), (0, 5.8, 19.0), (0, 8.0, 20.2))]
    parent = "hips"
    for i in range(7):
        B("tail%d" % i, tail_pts[i], parent)
        parent = "tail%d" % i
    B("tailtip", tail_pts[7], parent)
    B("ring", (0, -7.0, 0), "root")

    # ---------------------------------------------------------------- hips, coat, legs
    C("hips", (-4.6, 11.0, -2.6), (9.2, 3.8, 5.2), mat_faces("coat"))
    if not low:
        C("hips", (-4.9, 14.0, -2.9), (9.8, 1.0, 5.8), mat_faces("violet"))                              # the sash
    if full:
        C("hips", (-1.0, 13.7, -3.3), (2.0, 1.6, 0.5), mat_faces("lace", only=("north", "up")))          # the buckle
    if low:
        C("coatF_L", (-5.2, 2.0, -3.8), (10.4, 12.0, 7.6), mat_faces("hem"))
        for s, t in ((1, "L"), (-1, "R")):
            C("leg_" + t, (s * 2.6 - 1.5, 0.0, -2.0), (3.0, 11.5, 4.0), mat_faces("boot"))
    else:
        for s, t in ((1, "L"), (-1, "R")):
            sx = s * 2.6
            C("coatF_" + t, (sx - 2.7, 4.0, -3.6), (5.4, 10.8, 1.2), mat_faces("hem"), rot=(-6, 0, s * 3), pivot=(sx, 14.8, -3.0))
            C("coatB_" + t, (s * 2.4 - 2.3, 2.6, 2.2), (4.6, 12.2, 1.2), {**mat_faces("hem"), "south": ("back_coat", 0)}, rot=(8, 0, s * 3), pivot=(s * 2.4, 14.8, 2.8))
            C("leg_" + t, (sx - 1.8, 6.4, -1.8), (3.6, 5.4, 3.6), mat_faces("coat"))
            C("shin_" + t, (sx - 1.55, 2.2, -1.55), (3.1, 4.4, 3.2), mat_faces("boot"))
            C("shin_" + t, (sx - 1.9, 1.0, -5.2), (3.8, 2.6, 6.8), mat_faces("boot"))
            S("shin_" + t, (sx, 1.4, -5.0), (sx, 1.1, -8.6), 1.8, mat_faces("boot", skip=NS), t2=1.4)           # the pointed toe
            if full:
                C("coatF_" + t, (sx - 2.2, 4.4, -3.1), (4.4, 10.2, 0.5), mat_faces("violet", only=("north", "south")), rot=(-3, 0, s * 2), pivot=(sx, 14.8, -2.6))     # the lining showing at the opening
                C("coatS_" + t, (s * 4.4 - 0.5, 5.4, -2.2), (1.0, 8.6, 4.8), mat_faces("hem"), rot=(0, 0, s * 9), pivot=(s * 4.4, 14.0, 0))
                C("shin_" + t, (sx - 1.7, 5.4, -1.8), (3.4, 1.0, 3.6), mat_faces("violet"))                     # the boot cuff
                for j in (-0.9, 0.9):
                    S("shin_" + t, (sx + j, 0.8, -7.6), (sx + j, 0.5, -9.6), 0.7, mat_faces("horn", skip=NS))   # toe claws
                S("shin_" + t, (sx, 1.6, 1.6), (sx, 0.6, 3.4), 0.9, mat_faces("horn", skip=NS))                # the heel spur
                for k, (dx, dy, ln) in enumerate(((-1.4, 0.0, 3.4), (0.4, -0.6, 4.4), (1.6, 0.2, 3.0))):       # ragged smoky hem fragments under the coat
                    C("wisp_" + t, (s * 5.0 + dx * s - 0.8, 1.6 + dy, -1.2 + 1.6 * k - 1.8), (1.6, ln, 1.6), mat_faces("hem", only=NS + ("west", "east")), rot=(0, 20 * k, s * (6 + 5 * k)), pivot=(s * 5.0, 6.0, 0))

    # ---------------------------------------------------------------- waist, chest, collar, jabot, shoulders
    if not low:
        C("chest", (-3.7, 14.6, -2.3), (7.4, 3.4, 4.6), mat_faces("coat"))
    cf = mat_faces("coat")
    cf["north"] = ("chest_front", 0)
    C("chest", (-5.2, 17.8, -3.2), (10.4, 6.4, 6.4), cf)
    if mid:
        C("chest", (-5.5, 23.4, -2.6), (11.0, 1.0, 5.6), mat_faces("violet"))                           # the yoke
        for s in (1, -1):
            C("chest", (s * 2.6 - 1.4, 19.0, -3.7), (2.8, 5.4, 0.7), mat_faces("violet", only=("north", "up", "west", "east")), rot=(0, s * 8, s * -10), pivot=(s * 2.6, 24.0, -3.4))    # lapels
        C("chest", (-1.3, 19.4, -3.9), (2.6, 4.2, 0.6), {"north": ("jabot", 0), "up": ("lace_up", 0), "west": ("lace_side", 0), "east": ("lace_side", 0)})
        C("chest", (-3.9, 23.8, 0.8), (7.8, 4.4, 1.4), mat_faces("coat"), rot=(-12, 0, 0), pivot=(0, 23.8, 0.8))      # the standing collar behind the neck
        C("neck", (-1.5, 24.0, -1.5), (3.0, 2.2, 3.0), mat_faces("skin"))
    if full:
        for s in (1, -1):
            C("chest", (s * 3.2 - 1.0, 23.6, -1.4), (2.0, 3.6, 2.2), mat_faces("coat"), rot=(0, 0, s * -18), pivot=(s * 3.2, 23.6, -0.4))   # collar points
    for s, t in ((1, "L"), (-1, "R")):
        if low:
            break
        C("arm_" + t, (s * 5.4 - 1.7 if s > 0 else s * 5.4 - 1.7, 21.8, -3.1), (3.4, 2.6, 6.2), mat_faces("violet"))     # the pauldron
        S("arm_" + t, (s * 7.2, 24.0, -0.4), (s * 9.4, 27.6, 1.6), 1.7, mat_faces("horn", skip=NS))                    # the shoulder spike
        if full:
            S("arm_" + t, (s * 8.6, 21.6, -2.4), (s * 9.2, 17.6, -2.4), 0.8, mat_faces("lace", only=("north", "south", "west", "east")))      # the tassel
            C("arm_" + t, (s * 8.9 - 0.7, 16.4, -3.1), (1.4, 1.4, 1.4), mat_faces("violet"))

    # ---------------------------------------------------------------- arms and hands
    for s, t in ((1, "L"), (-1, "R")):
        sh, el, wr = V((s * 6.6, 22.0, 0.0)), V((s * 8.0, 16.8, 0.4)), V((s * 7.2, 12.4, -3.2))
        if low:
            S("arm_" + t, sh, wr, 3.0, mat_faces("coat", skip=NS), t2=3.0, grow=0.4)
            continue
        S("arm_" + t, sh, el, 3.2, mat_faces("coat", skip=NS), t2=3.2, grow=0.4)
        S("fore_" + t, el, wr, 2.9, mat_faces("coat", skip=NS), t2=2.9, grow=0.4)
        cuff = V((s * 7.3, 13.2, -2.8))
        cuff_faces = {**mat_faces("violet", skip=NS), "up": ("rune_arm", 0), "down": ("rune_arm", 0)}
        S("fore_" + t, cuff - V((0, 0.3, -0.2)), cuff + V((0, 1.3, -0.6)), 3.6, cuff_faces, t2=3.6, grow=0.0)
        if fx:
            m.seg("fore_" + t, cuff - V((0, 0.3, -0.2)), cuff + V((0, 1.3, -0.6)), 3.9, {"up": ("rune_arm", 0), "down": ("rune_arm", 0)}, 3.9, (0, 1, 0), d=0)
        hp = V((s * 7.2, 11.2, -3.8))
        C("hand_" + t, (hp[0] - 1.4, hp[1] - 1.2, hp[2] - 1.5), (2.8, 2.4, 3.0), mat_faces("skin"))
        for dx in (-1.0, -0.33, 0.33, 1.0)[: 4 if full else 3]:
            S("hand_" + t, hp + V((dx, -0.6, -1.6)), hp + V((dx * 1.5, -1.9, -5.6)), 0.8, mat_faces("horn", skip=NS), t2=0.8, grow=0.0)
        S("hand_" + t, hp + V((-s * 1.6, -0.4, -0.6)), hp + V((-s * 2.6, -1.4, -3.0)), 0.8, mat_faces("horn", skip=NS), grow=0.0)    # the thumb

    # ---------------------------------------------------------------- head
    face = {"up": ("hair_top", 0), "down": ("skin_down", 0), "south": ("hair_top", 0), "west": ("head_side_l", 0), "east": ("head_side_r", 0)}      # the north faces are the face model's (the mood sheets)
    C("head", (-3.5, 28.0, -3.2), (7.0, 3.8, 6.4), face)
    C("head", (-2.6, 25.4, -2.9), (5.2, 2.7, 5.6), {"down": ("skin_down", 0), "west": ("head_side_l", 0), "east": ("head_side_r", 0), "south": ("hair_top", 0)})
    if not low:
        for s in (1, -1):
            S("head", (s * 3.3, 28.0, -0.2), (s * 6.4, 28.8, 1.6), 1.8, mat_faces("horn", skip=NS), t2=0.6, grow=0.0)             # pointed ears
        S("head", (0, 31.6, -2.4), (0, 34.4, 1.4), 2.6, mat_faces("horn", skip=NS), t2=1.6, grow=0.2)                          # the swept-back hair
    if full:
        for s in (1, -1):
            S("head", (s * 1.9, 31.6, -2.6), (s * 3.0, 33.6, 0.6), 1.8, mat_faces("horn", skip=NS), grow=0.2)
            S("head", (s * 1.8, 29.2, -3.5), (s * 4.0, 29.6, -3.6), 1.0, mat_faces("horn", skip=("up", "down")), t2=0.8, grow=0.0)    # brow ridge
            S("head", (s * 1.1, 31.9, -3.2), (s * 1.9, 31.0, -3.8), 1.1, mat_faces("horn", skip=NS), t2=0.8, grow=0.0)             # the fringe
        S("head", (0, 31.9, -3.2), (0, 31.1, -3.9), 1.2, mat_faces("horn", skip=NS), t2=0.8, grow=0.0)
    # the horns: four segments sweeping out and back, the tips curving up (a second small pair behind on the full model)
    HP = [V(p) for p in ((2.2, 31.0, -1.4), (4.4, 32.8, -0.6), (6.2, 34.4, 1.4), (6.8, 36.0, 3.8), (6.2, 37.0, 6.0))]
    hth = (2.2, 1.9, 1.5, 1.1)
    for s in (1, -1):
        pts = [V((p[0] * s, p[1], p[2])) for p in HP]
        if full:
            for i in range(4):
                S("head", pts[i], pts[i + 1], hth[i], mat_faces("horn", skip=NS), grow=0.25)
            S("head", V((s * 1.2, 31.8, 0.6)), V((s * 2.0, 34.6, 3.2)), 1.3, mat_faces("horn", skip=NS), grow=0.2)           # a small second horn behind the great one
            S("head", V((s * 2.0, 34.6, 3.2)), V((s * 1.6, 36.2, 5.4)), 0.9, mat_faces("horn", skip=NS), grow=0.2)
        elif mid:
            S("head", pts[0], pts[2], 2.0, mat_faces("horn", skip=NS), grow=0.25)
            S("head", pts[2], pts[3], 1.4, mat_faces("horn", skip=NS), grow=0.25)
            S("head", pts[3], pts[4], 1.0, mat_faces("horn", skip=NS), grow=0.2)
        else:
            S("head", pts[0], pts[3], 1.8, mat_faces("horn", skip=NS), grow=0.2)
            S("head", pts[3], pts[4], 1.0, mat_faces("horn", skip=NS), grow=0.2)

    # ---------------------------------------------------------------- tail
    tth = [2.6, 2.4, 2.2, 2.0, 1.8, 1.5, 1.2]
    idx = list(range(7)) if full else ([0, 1, 3, 5, 6] if lod == 1 else [0, 3, 6])
    for k, i in enumerate(idx):
        a = tail_pts[i]
        b = tail_pts[idx[k + 1]] if k + 1 < len(idx) else tail_pts[7]
        S("tail%d" % i, a, b, tth[i], mat_faces("violet", skip=NS), grow=0.3)
        if full and i in (1, 2, 3, 4, 5):
            c = (a + b) / 2
            S("tail%d" % i, c + V((0, tth[i] / 2 - 0.2, 0)), c + V((0, tth[i] / 2 + 1.8, 0.2)), 0.8, mat_faces("horn", skip=NS + ("up", "down")), t2=0.6, grow=0.0)   # a spine on the back
    tip = tail_pts[7]
    spade = {"north": ("violet_front", 0), "south": ("violet_back", 0)}
    if low:
        C("tailtip", (tip[0] - 2.4, tip[1] - 1.0, tip[2] - 0.4), (4.8, 4.8, 0.7), spade, rot=(35, 0, 0), pivot=tuple(tip))
    else:
        if not fx:
            m.plate("tailtip", tip + V((0, 0.8, 0.8)), (1, 0, 0), (0, 0.7, 0.6), 4.8, 4.8, 0.7, {**spade, "up": ("ink_up", 0), "down": ("ink_up", 0), "west": ("ink_up", 0), "east": ("ink_up", 0)}, d=0)
            m.plate("tailtip", tip + V((0, 0.8, 0.8)), (0, 1, 0), (0, 0.6, -0.7), 4.8, 4.8, 0.7, spade, d=0)

    # ---------------------------------------------------------------- wings
    for s, t in ((1, "L"), (-1, "R")):
        mir = lambda p: V((p[0] * s, p[1], p[2]))
        r, e, w = mir(WR), mir(WE), mir(WW)
        fingers = [mir(f) for f in WF]
        if low:
            S("wing_" + t, r, w, 1.8, mat_faces("bone", skip=NS), grow=0.4)
            for k in range(2):
                S("wing2_" + t, w, fingers[k], 1.0, mat_faces("bone", skip=NS), grow=0.2)
        else:
            S("wing_" + t, r, e, 2.0, mat_faces("bone", skip=NS), grow=0.4)
            S("wing2_" + t, e, w, 1.7, mat_faces("bone", skip=NS), grow=0.4)
            for k in range(3):
                S("wing2_" + t, w, fingers[k], 1.0, mat_faces("bone", skip=NS), grow=0.2)
                if full:
                    S("wing2_" + t, fingers[k], fingers[k] + L.unit(fingers[k] - w) * 1.8, 0.7, mat_faces("horn", skip=NS), grow=0.0)          # the finger claw
                    c = w + (fingers[k] - w) * 0.55
                    C("wing2_" + t, (c[0] - 0.8, c[1] - 0.8, c[2] - 0.8), (1.6, 1.6, 1.6), mat_faces("horn"))                                  # a knuckle
            S("wing2_" + t, w, w + V((-s * 1.2, 2.8, -0.6)), 1.1, mat_faces("horn", skip=NS), grow=0.0)                                         # the thumb claw
            if full:
                for k in range(3):                                                                         # spikes along the forearm
                    c = e + (w - e) * (0.25 + 0.3 * k)
                    S("wing2_" + t, c, c + V((0, 1.8, -0.6)), 0.8, mat_faces("horn", skip=NS), grow=0.0)
        for i, (p0, a, b) in enumerate(PANELS):
            if low and i > 1:
                continue
            ce, u, v, pw, ph, tri = panel_frame(*[mir(p0), mir(a), mir(b)])
            faces = {"north": ("mem_%d" % i, 0), "south": ("mem_%d" % i, 0)}
            if fx:
                m.plate("wing2_" + t, ce, u, v, pw, ph, 0.5, faces, d=0)
            else:
                m.plate("wing2_" + t, ce, u, v, pw, ph, 0.3, faces, d=0)

    # ---------------------------------------------------------------- orbiting dark orbs (full model only)
    if full:
        for k in range(3):
            a = k * math.tau / 3
            c = V((math.cos(a) * 10.5, 20.0, math.sin(a) * 10.5))
            C("orbit", (c[0] - 1.2, c[1] - 1.2, c[2] - 1.2), (2.4, 2.4, 2.4), mat_faces("violet"), rot=(30, 45, 0), pivot=tuple(c))
    if fx:
        for k in range(3):
            a = k * math.tau / 3
            c = V((math.cos(a) * 10.5, 20.0, math.sin(a) * 10.5))
            XP("orbit", c, (1, 0, 0), (0, 1, 0), 6.0, 6.0, {"north": ("puff", 0), "south": ("puff", 0)})
            XP("orbit", c, (0, 0, 1), (0, 1, 0), 6.0, 6.0, {"west": ("puff", 0), "east": ("puff", 0)})

    # ---------------------------------------------------------------- glow-only extras: runes on the chest, wisps trailing the coat, the ground ring
    if fx:
        X("chest", (-5.2, 17.8, -3.3), (10.4, 6.4, 0.1), {"north": ("chest_front", 0)})
        for s, t in ((1, "L"), (-1, "R")):                                                              # the glowing hem of the tailcoat, a hair proud of the panels' bottom edges
            m.cube("coatF_" + t, (s * 2.6 - 2.8, 3.9, -3.7), (5.6, 0.9, 1.5), {"north": ("hemline", 0), "south": ("hemline", 0)}, rot=(-6, 0, s * 3), pivot=(s * 2.6, 14.8, -3.0), d=0)
            m.cube("coatB_" + t, (s * 2.4 - 2.4, 2.5, 2.1), (4.8, 0.9, 1.4), {"north": ("hemline", 0), "south": ("hemline", 0)}, rot=(8, 0, s * 3), pivot=(s * 2.4, 14.8, 2.8), d=0)
        for s, t in ((1, "L"), (-1, "R")):
            for j in range(2):
                XP("wisp_" + t, (s * (5.4 + 1.6 * j), 3.2 - 1.2 * j, 1.0 * j - 0.6), (1, 0, 0.4 * s), (0, 1, 0), 3.6, 7.0, {"north": ("tendril", 0), "south": ("tendril", 0)})
        XP("tailtip", tip + V((0, 0.8, 0.8)), (1, 0, 0), (0, 0.7, 0.6), 7.5, 7.5, {"north": ("puff", 0), "south": ("puff", 0)})
        m.cube("ring", (-11, -7.0, -11), (22, 0.1, 22), {"up": ("ring", 0), "down": ("ring", 0)}, d=0)
        XP("head", (0, 29.2, 0.6), (1, 0, 0), (0, 1, 0), 15, 15, {"north": ("puff", 0)}, 0.05)
    return m


# ====================================================================================================================== the companion
def companion_atlas():
    at = L.Atlas(256, 256)
    for i, f in enumerate(FACES):
        at.region("smoke_" + f, 40, 40, (lambda ff, sd: (lambda w, h: A.smoke(w, h, ff, sd)))(f, 200 + i * 5))
        at.region("shade_" + f, 40, 40, (lambda ff, sd: (lambda w, h: A.smoke(w, h, ff, sd, -0.5)))(f, 230 + i * 5))
        at.region("hornc_" + f, 24, 24, (lambda ff, sd: (lambda w, h: A.mat("horn", w, h, ff, sd)))(f, 260 + i * 3))
    at.region("memc", 64, 64, lambda w, h: A.membrane(w, h, [(0.04, 0.96), (0.96, 0.9), (0.5, 0.08)], 77))
    at.region("puff", 56, 56, lambda w, h: A.puff_glow(w, h, A.GLOW_V, 190.0, 51))
    at.region("tendril", 32, 56, lambda w, h: A.tendril_glow(w, h, A.GLOW_V, 220.0))
    return at


def face_companion(w, h):
    """The wisp's face: a dark mask with slanted slit-eye sockets and a jagged grin, all of it glowing violet in the glow array."""
    c, g = A.cel(w, h, np.array([20, 13, 34], np.float32), "front", 71, bands=(0.2, 0.85))
    x, y = A.grid(w, h)
    u, v = x / (w - 1), y / (h - 1)
    for s in (-1, 1):
        cx = 0.5 + s * 0.2
        ex = (u - cx) / 0.15
        ey = (v - (0.42 + s * (u - cx) * -0.30)) / 0.12
        sock = ex * ex + ey * ey < 1.0
        c[sock, :3] = A.INK
        core = ex * ex + ey * ey * 1.5 < 0.6
        g[core, :3] = A.GLOW_V
        g[core, 3] = 255.0
        hot = ex * ex + ey * ey * 3.0 < 0.2
        g[hot, :3] = np.array([255, 230, 255], np.float32)
        halo = (ex * ex + ey * ey < 3.0) & ~sock
        g[halo, :3] = A.GLOW_V
        g[halo, 3] = (A.smooth(1 - (ex * ex + ey * ey) / 3.0) * 60)[halo]
    zig = 0.78 + 0.07 * np.abs(((u * 9) % 2) - 1)
    line = (np.abs(v - zig) < 0.05) & (np.abs(u - 0.5) < 0.3)
    c[line, :3] = A.INK
    seam = (np.abs(v - zig) < 0.02) & (np.abs(u - 0.5) < 0.28)
    g[seam, :3] = A.GLOW_V
    g[seam, 3] = 220.0
    A.outline(c, w, h, 2)
    return c, g


def smoke_faces(skip=(), only=None, kind="smoke"):
    m = {"up": (kind + "_up", 0), "down": (kind + "_down", 0), "north": (kind + "_front", 0), "south": (kind + "_back", 0), "west": (kind + "_side", 0), "east": (kind + "_side", 0)}
    if only is not None:
        m = {k: v for k, v in m.items() if k in only}
    for k in skip:
        m.pop(k, None)
    return m


def build_companion(lod, atlas, fx=False):
    ident = "spirit_dark_companion" + ("_fx" if fx else "" if lod == 0 else "_lod%d" % lod)
    m = L.Model(ident, atlas, seed=4000 + lod, tex_scale=0.5 if fx else {0: 1.0, 1: 0.5, 2: 0.25}[lod])
    full, mid, low = lod == 0, lod <= 1, lod == 2
    NS = ("north", "south")

    def C(bone, o, s, faces, rot=None, pivot=None):
        if fx:
            return None
        return m.cube(bone, o, s, faces, rot, pivot, d=0)

    def S(bone, p0, p1, t, faces, t2=None, grow=0.25, roll=0.0):
        if fx:
            return None
        p0, p1 = V(p0, dtype=float), V(p1, dtype=float)
        dv = L.unit(p1 - p0) * grow
        return m.seg(bone, p0 - dv, p1 + dv, t, faces, t2, (0, 1, 0), d=0, roll=roll)

    CY = 17.0                                                                                       # the head blob's centre height; the cloak and the tail hang below it
    m.bone("body", (0, CY, 0), "root")
    m.bone("core", (0, CY, 0), "body")
    m.bone("head", (0, CY, 0), "core")
    m.bone("cloak", (0, CY - 3.0, 0), "core")
    m.bone("cloak2", (0, CY - 6.4, 0), "cloak")
    tail_pts = [V((0.0, CY - 9.0, 0.4))]
    for i in range(1, 9):
        tail_pts.append(tail_pts[-1] + V((math.sin(i * 0.8) * 0.8, -1.5 + 0.12 * i, 0.5 + 0.22 * i)))
    parent = "cloak2"
    for i in range(8):
        m.bone("tail%d" % i, tail_pts[i], parent)
        parent = "tail%d" % i
    for s, t in ((1, "L"), (-1, "R")):
        m.bone("wing_" + t, (s * 3.0, CY + 1.2, 2.0), "core")
        m.bone("wing2_" + t, (s * 6.6, CY + 3.2, 2.8), "wing_" + t)
        m.bone("arm_" + t, (s * 3.2, CY - 2.4, -1.6), "core")
        m.bone("cheek_" + t, (s * 3.0, CY - 1.4, -0.6), "head")
    TUFTS = ((0.0, 0.2, 0.0), (1.5, 0.0, 14.0), (-1.5, 0.0, -14.0))
    for i, (x, z, ang) in enumerate(TUFTS):
        m.bone("tuft%d" % i, (x, CY + 3.0, z), "head")
        m.bone("tuft%db" % i, (x + ang * 0.05, CY + 5.0, z), "tuft%d" % i)
    ten = []
    for i, deg in enumerate((0, 32, 64, 96, 128, 160, 180)):
        ang = math.radians(deg)
        base = V((math.cos(ang) * 3.4, CY - 3.0 - 0.9 * (i % 2), math.sin(ang) * 3.0 + 0.4))
        ten.append((i, base, ang))
        m.bone("ten%d" % i, base, "cloak")
        m.bone("ten%db" % i, base + V((math.cos(ang) * 2.2, -2.2, math.sin(ang) * 1.6)), "ten%d" % i)
    # head blob: interlocked cubes make a round shape; the face plate sits on the front
    C("core", (-3.4, CY - 3.1, -3.1), (6.8, 6.2, 6.2), smoke_faces(kind="shade"))
    if not low:
        C("core", (-3.2, CY - 2.9, -2.9), (6.4, 5.8, 5.8), smoke_faces(kind="shade"), rot=(0, 45, 0), pivot=(0, CY, 0))
        C("core", (-3.0, CY - 2.7, -2.7), (6.0, 5.4, 5.4), smoke_faces(kind="shade"), rot=(35, 0, 35), pivot=(0, CY, 0))
    if full:
        C("core", (-2.8, CY - 2.9, -2.8), (5.6, 5.8, 5.6), smoke_faces(kind="shade"), rot=(-35, 0, -35), pivot=(0, CY, 0))
    C("head", (-3.0, CY - 1.8, -4.8), (6.0, 4.0, 0.8), {"up": ("shade_up", 0), "down": ("shade_down", 0), "west": ("shade_side", 0), "east": ("shade_side", 0)})      # the north face is the face model's
    # the cloak: a hanging taper of smoke
    if low:
        C("cloak", (-3.0, CY - 8.6, -2.6), (6.0, 6.0, 5.2), smoke_faces())
    else:
        C("cloak", (-3.1, CY - 6.3, -2.8), (6.2, 3.8, 5.4), smoke_faces(), rot=(0, 8, 0), pivot=(0, CY - 4.4, 0))
        C("cloak2", (-2.3, CY - 9.2, -2.2), (4.6, 3.4, 4.2), smoke_faces(), rot=(0, -14, 0), pivot=(0, CY - 7.5, 0))
        if mid:
            for k, (dx, dz, sz) in enumerate(((-3.0, -1.4, 2.4), (3.0, -1.0, 2.2), (-2.6, 2.0, 2.6), (2.8, 2.2, 2.4), (0.0, 3.2, 2.8), (-3.4, 0.6, 2.0), (3.4, 0.8, 2.0), (0.4, -2.6, 2.0))[: 8 if full else 4]):
                C("cloak", (dx - sz / 2, CY - 6.8 - 0.6 * (k % 4), dz - sz / 2), (sz, sz * 1.3, sz), smoke_faces(), rot=(18 * k, 33 * k, 12 * k), pivot=(dx, CY - 6.0, dz))
            for k in range(4 if full else 2):                                                        # a ragged belt of smoke round the waist
                a = k * math.tau / 4 + 0.4
                dx, dz = math.cos(a) * 3.6, math.sin(a) * 3.4
                C("cloak", (dx - 1.1, CY - 4.4, dz - 1.1), (2.2, 1.6, 2.2), smoke_faces(), rot=(0, 40 * k, 0), pivot=(dx, CY - 4.0, dz))
    # horns: two small curled ones
    for s in (1, -1):
        pts = [V((s * 1.9, CY + 2.6, -0.6)), V((s * 3.1, CY + 4.4, -0.2)), V((s * 3.7, CY + 6.0, 0.8)), V((s * 3.2, CY + 7.2, 2.2))]
        hf = {"up": ("hornc_up", 0), "down": ("hornc_down", 0), "west": ("hornc_side", 0), "east": ("hornc_side", 0)}
        if mid:
            for i in range(3):
                S("head", pts[i], pts[i + 1], (1.7, 1.4, 1.0)[i], hf, grow=0.2)
        else:
            S("head", pts[0], pts[3], 1.5, hf, grow=0.2)
    # smoky tufts on the crown and at the cheeks: curls of smoke that sway
    for i, (x, z, ang) in enumerate(TUFTS):
        if low:
            continue
        p0 = V((x, CY + 3.0, z))
        p1 = V((x + ang * 0.05, CY + 5.0, z))
        p2 = p1 + V((ang * 0.08, 1.6, 0.4))
        S("tuft%d" % i, p0, p1, 1.5, smoke_faces(skip=NS, kind="shade"), grow=0.2, roll=ang)
        S("tuft%db" % i, p1, p2, 0.9, smoke_faces(skip=NS, kind="shade"), grow=0.2, roll=ang)
    if mid:
        for s, t in ((1, "L"), (-1, "R")):
            S("cheek_" + t, V((s * 3.0, CY - 1.4, -0.6)), V((s * 4.8, CY - 2.8, 0.6)), 1.4, smoke_faces(skip=NS, kind="shade"), grow=0.2)
            S("cheek_" + t, V((s * 4.8, CY - 2.8, 0.6)), V((s * 5.8, CY - 4.4, 1.4)), 0.9, smoke_faces(skip=NS, kind="shade"), grow=0.2)
    # ragged little wings: two bones and two torn membranes
    for s, t in ((1, "L"), (-1, "R")):
        r, e, w = V((s * 3.0, CY + 1.2, 2.0)), V((s * 6.6, CY + 3.2, 2.8)), V((s * 10.4, CY + 5.2, 3.4))
        if mid:
            S("wing_" + t, r, e, 1.1, smoke_faces(skip=NS), grow=0.2)
            S("wing2_" + t, e, w, 0.9, smoke_faces(skip=NS), grow=0.2)
        tips = [V((s * 13.0, CY + 1.4, 4.4)), V((s * 10.6, CY - 1.8, 5.2)), V((s * 5.2, CY - 2.6, 3.4))]
        pan = [(w, tips[0], tips[1]), (w, tips[1], tips[2])] if not low else [(w, tips[0], tips[2])]
        for (p0, a, b) in pan:
            ce, u, v, pw, ph, tri = panel_frame(p0, a, b)
            if fx:
                continue
            m.plate("wing2_" + t, ce, u, v, pw, ph, 0.25, {"north": ("memc", 0), "south": ("memc", 0)}, d=0)
        if mid:
            for k in range(2):                                                                        # finger claws at the wing's wrist
                S("wing2_" + t, w, tips[k], 0.7, smoke_faces(skip=NS), grow=0.1)
    # arms: two small clawed stubs
    if mid:
        for s, t in ((1, "L"), (-1, "R")):
            p0, p1 = V((s * 3.2, CY - 2.4, -1.6)), V((s * 4.4, CY - 4.4, -3.0))
            S("arm_" + t, p0, p1, 1.5, smoke_faces(skip=NS), grow=0.2)
            if mid:
                C("arm_" + t, (p1[0] - 0.9, p1[1] - 0.9, p1[2] - 0.9), (1.8, 1.8, 1.8), smoke_faces(kind="shade"))
                for j in (-0.5, 0.5):
                    S("arm_" + t, p1, p1 + V((j + s * 0.4, -1.2, -1.6)), 0.6, {"up": ("hornc_up", 0), "west": ("hornc_side", 0), "east": ("hornc_side", 0)}, grow=0.0)
    # tail
    idx = list(range(8)) if full else ([0, 1, 3, 4, 6, 7] if lod == 1 else [0, 3, 6])
    for k, i in enumerate(idx):
        a = tail_pts[i]
        b = tail_pts[idx[k + 1]] if k + 1 < len(idx) else tail_pts[8]
        th = max(0.8, 2.8 - 0.3 * i)
        S("tail%d" % i, a, b, th, smoke_faces(skip=NS), grow=0.3, roll=i * 25.0)
    if full:
        for k, i in enumerate((1, 3, 5, 7)):                                                          # puffs of smoke along the tail
            c = tail_pts[i]
            sz = 2.6 - 0.3 * k
            C("tail%d" % i, (c[0] - sz / 2 + (1.2 if k % 2 else -1.2), c[1] - sz / 2, c[2] - sz / 2), (sz, sz, sz), smoke_faces(), rot=(20 * k, 40 * k, 10 * k), pivot=tuple(c))
    # tendrils: short curls of smoke at the sides and the back of the cloak (never in front of the face)
    for (i, base, ang) in ten:
        out = V((math.cos(ang), 0.0, math.sin(ang)))
        p1 = base + out * 2.2 + V((0, -2.2, 0))
        p2 = p1 + out * 1.6 + V((0, -2.4, 0))
        if low:
            continue
        S("ten%d" % i, base, p1, 1.4, smoke_faces(skip=NS), grow=0.2, roll=ang * 20)
        S("ten%db" % i, p1, p2, 1.0, smoke_faces(skip=NS), grow=0.2, roll=ang * 20)
        if full:
            S("ten%db" % i, p2, p2 + out * 1.0 + V((0, -1.8, 0)), 0.6, smoke_faces(skip=NS + ("west", "east")), grow=0.0)
    # ---- the glow-only parts: eye cards, wisps trailing the tail and round the body
    if fx:
        for k, i in enumerate((1, 3, 5, 7)):
            c = tail_pts[i]
            m.plate("tail%d" % i, c + V((0, -0.4, 0.4)), (1, 0, 0), (0, 1, 0), 5.0 - 0.4 * k, 5.0 - 0.4 * k, 0.1, {"north": ("puff", 0), "south": ("puff", 0)}, d=0)
            m.plate("tail%d" % i, c + V((0, -0.4, 0.4)), (0, 0, 1), (0, 1, 0), 5.0 - 0.4 * k, 5.0 - 0.4 * k, 0.1, {"west": ("puff", 0), "east": ("puff", 0)}, d=0)
        for (i, base, ang) in ten[1:6]:
            out = V((math.cos(ang), 0.0, math.sin(ang)))
            m.plate("ten%db" % i, base + out * 3.6 + V((0, -3.6, 0)), (math.cos(ang + 1.57), 0, math.sin(ang + 1.57)), (0, 1, 0), 3.0, 6.0, 0.1, {"north": ("tendril", 0), "south": ("tendril", 0)}, d=0)
        for s, t in ((1, "L"), (-1, "R")):
            m.plate("wing2_" + t, V((s * 9.6, CY + 2.4, 4.0)), (s, 0, 0.3), (0, 1, 0), 6.4, 6.4, 0.1, {"north": ("puff", 0), "south": ("puff", 0)}, d=0)
        m.plate("core", (0, CY - 1.0, 2.6), (1, 0, 0), (0, 1, 0), 12, 12, 0.1, {"north": ("puff", 0), "south": ("puff", 0)}, d=0)
        m.plate("tuft0b", (0, CY + 6.2, 0.4), (1, 0, 0), (0, 1, 0), 4.4, 5.4, 0.1, {"north": ("tendril", 0), "south": ("tendril", 0)}, d=0)
    return m


# ====================================================================================================================== the faces (seven moods on the same plates)
FACE_SIZES = {"powered": (160, 48), "companion": (72, 48)}


def face_atlas(form, mood):
    import spirit_dark_faces as F
    w, h = FACE_SIZES[form]
    at = L.Atlas(w, h)
    if form == "powered":
        at.region("face_upper", 88, 48, lambda ww, hh: F.upper(ww, hh, mood))
        at.region("face_lower", 64, 44, lambda ww, hh: F.lower(ww, hh, mood))
    else:
        at.region("face_c", 72, 48, lambda ww, hh: F.companion(ww, hh, mood))
    return at


def build_face(form, glow, atlas):
    """The face plates of a form as a model of their own: the same bones as the body down to the head (so every clip moves it), one north face per plate. glow = the additive copy, a hair proud of the
    colour plates so the two passes never fight for the same depth."""
    ident = "spirit_dark_%s_face%s" % (form, "glow" if glow else "")
    m = L.Model(ident, atlas, seed=5000, tex_scale=1.0)
    p = 0.1 if glow else 0.0
    if form == "powered":
        m.bone("body", (0, 14, 0), "root")
        m.bone("hips", (0, 14, 0), "body")
        m.bone("chest", (0, 18, 0), "hips")
        m.bone("neck", (0, 24, 0), "chest")
        m.bone("head", (0, 26, 0), "neck")
        m.cube("head", (-3.5, 28.0, -3.2 - p), (7.0, 3.8, 0.05), {"north": ("face_upper", 0)}, d=0)
        m.cube("head", (-2.6, 25.4, -2.9 - p), (5.2, 2.7, 0.05), {"north": ("face_lower", 0)}, d=0)
    else:
        CY = 17.0
        m.bone("body", (0, CY, 0), "root")
        m.bone("core", (0, CY, 0), "body")
        m.bone("head", (0, CY, 0), "core")
        m.cube("head", (-3.0, CY - 1.8, -4.8 - p), (6.0, 4.0, 0.05), {"north": ("face_c", 0)}, d=0)
    return m


def write_faces(count_only):
    import spirit_dark_faces as F
    rows = []
    for form in ("powered", "companion"):
        for mood in F.MOODS:
            at = face_atlas(form, mood)
            for glow in (False, True):
                m = build_face(form, glow, at)
                if mood == F.MOODS[0]:
                    rows.append((m.id, m.cubes(), m.quads()))
                    if not count_only:
                        m.save()
                if not count_only:
                    if glow:
                        at.save_glow("spirit_dark_%s_faceglow_%s" % (form, mood), 1.0)
                        if mood == F.MOODS[0]:
                            at.save_glow("spirit_dark_%s_faceglow" % form, 1.0)               # the model's own default sheet (the rule that every model has a sheet of its name): the smug one
                    else:
                        at.save_color("spirit_dark_%s_face_%s" % (form, mood), 1.0)
                        if mood == F.MOODS[0]:
                            at.save_color("spirit_dark_%s_face" % form, 1.0)
    for r in rows:
        print("%-34s cubes %4d  quads %5d" % r)


# ====================================================================================================================== animations
import spirit_dark_poses as PO  # noqa: E402


def powered_clips():
    return PO.clips(PO.powered_poses(), PO.powered_sway)


def companion_clips():
    return PO.clips(PO.companion_poses(), PO.companion_sway)


# ====================================================================================================================== main
def main():
    count_only = "--count" in sys.argv
    only = [a for a in sys.argv[1:] if not a.startswith("--")]
    for form, atlas_fn, build, clips in (("powered", powered_atlas, build_powered, powered_clips), ("companion", companion_atlas, build_companion, companion_clips)):
        if only and form not in only:
            continue
        at = atlas_fn()
        rows = []
        for lod, fx in ((0, False), (1, False), (2, False), (0, True)):
            m = build(lod, at, fx)
            rows.append((m.id, m.cubes(), m.quads()))
            if not count_only:
                m.save()
                if fx:
                    at.save_glow(m.id, 0.5)
                else:
                    at.save_color(m.id, m.tex_scale)
        if not count_only:
            L.write_animations("spirit_dark_" + form, clips())
        for r in rows:
            print("%-34s cubes %4d  quads %5d" % r)
    if not only or "faces" in only:
        write_faces(count_only)


if __name__ == "__main__":
    main()
