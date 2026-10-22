"""Fuegoleon Vermillion's Salamander, both forms (docs/magic/fire_casting.md): the small red lizard-dragon that perches on the caster's shoulder and the big golden dragon that hangs over the caster
while a big page is breathed. Models (Bedrock geo, 1/16 block units, y up, front -z), a hand-painted sheet each (tools/gen_salamander_art.py), animations, three levels of detail per form and a
glow-only model:

    python3 -B tools/gen_salamander_geo.py            writes assets/nusmp/{geo,animations,textures}/entity/firecast_{salamander,dragon}[_lod1|_lod2|_fx]*
    python3 -B tools/gen_salamander_geo.py --count    prints the quads of every model

    firecast_salamander      LOD 0 (full)           firecast_dragon      LOD 0
    firecast_salamander_lod1 LOD 1 (about half)     firecast_dragon_lod1 LOD 1
    firecast_salamander_lod2 LOD 2 (low poly)       firecast_dragon_lod2 LOD 2
    firecast_salamander_fx   the glow-only model (eyes, mouth), drawn additive with the glow sheet

The detail is in the texture (scales, ring spots, belly plates, eyes, teeth, wing veins), not in the geometry: a face that is not seen is not given a uv and costs nothing. All LODs and the glow-only
model use the one sheet of their form, so a LOD change never changes a colour. Bones are the same names in every model (the animation file of the full model drives them all; a bone a LOD does
not have is ignored). Animations: idle, perch, fly, breathe (loops except breathe).
"""
import math
import os
import sys

import numpy as np

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import firecast_geo_lib as L  # noqa: E402
import gen_salamander_art as A  # noqa: E402

FORMS = {
    "salamander": dict(pal=A.SMALL, k=1.0, S=1, seed=1500, d=6.0, muscular=False, wk=1.0, neck3=False, neck_up=2.0),
    "dragon": dict(pal=A.BIG, k=1.9, S=2, seed=1600, d=7.0, muscular=True, wk=1.5, neck3=True, neck_up=9.0),
}
WING_D = 7.2                                           # texels per unit of the wing sheet at S = 1
WING_SPAN, WING_INNER, WING_CHORD = 44.2, 14.2, 22.0


# ====================================================================================================================== the sheet
def make_atlas(form):
    f = FORMS[form]
    S, pal, seed = f["S"], f["pal"], f["seed"]
    at = L.Atlas(512 * S, 512 * S)
    at.region("skin", 224 * S, 224 * S, lambda w, h: (A.scales(w, h, pal, seed, cell=pal["cell"] * S), np.zeros((h, w, 4), np.float32)))
    at.region("belly", 96 * S, 96 * S, lambda w, h: (A.belly(w, h, pal, seed + 1), np.zeros((h, w, 4), np.float32)))
    at.region("head_r", 96 * S, 96 * S, lambda w, h: A.head_side(w, h, pal, seed + 2, mirror=False))
    at.region("head_l", 96 * S, 96 * S, lambda w, h: A.head_side(w, h, pal, seed + 3, mirror=True))
    ww, wh = int(WING_SPAN * WING_D * S), int(WING_CHORD * WING_D * S)
    sheet, _ = A.wing_sheet(ww, wh, pal, seed + 4, WING_INNER / WING_SPAN)
    sheet = np.flipud(sheet).copy()                      # the leading edge is the front (-z), and the picture's bottom is the front on the up and down faces
    cut = int(round(WING_INNER * WING_D * S))
    at.region("wing_in", cut, wh, lambda w, h: (sheet[:, :cut], np.zeros((h, w, 4), np.float32)))
    at.region("wing_out", ww - cut, wh, lambda w, h: (sheet[:, cut:], np.zeros((h, w, 4), np.float32)))
    at.region("head_top", 80 * S, 80 * S, lambda w, h: A.head_top(w, h, pal, seed + 5))
    at.region("snout_front", 56 * S, 56 * S, lambda w, h: A.snout_front(w, h, pal, seed + 6))
    at.region("snout_r", 80 * S, 56 * S, lambda w, h: A.snout_side(w, h, pal, seed + 7, mirror=False))
    at.region("snout_l", 80 * S, 56 * S, lambda w, h: A.snout_side(w, h, pal, seed + 8, mirror=True))
    at.region("jaw_r", 80 * S, 32 * S, lambda w, h: A.jaw_side(w, h, pal, seed + 9, mirror=False))
    at.region("jaw_l", 80 * S, 32 * S, lambda w, h: A.jaw_side(w, h, pal, seed + 10, mirror=True))
    at.region("jaw_in", 56 * S, 80 * S, lambda w, h: A.jaw_in(w, h, pal, seed + 11))
    at.region("spike", 36 * S, 80 * S, lambda w, h: (A.spike(w, h, pal, seed + 12), np.zeros((h, w, 4), np.float32)))
    at.region("claw", 24 * S, 48 * S, lambda w, h: (A.claw(w, h, pal, seed + 13), np.zeros((h, w, 4), np.float32)))
    at.region("bone", 72 * S, 24 * S, lambda w, h: (A.bone_bar(w, h, pal, seed + 14), np.zeros((h, w, 4), np.float32)))
    at.region("flame", 32 * S, 48 * S, lambda w, h: A.flame_lick(w, h, pal, seed + 15))
    return at


# ====================================================================================================================== the body
def build(form, lod, atlas, fx=False):
    """One model of a form. lod 0 = full, 1 = about half, 2 = low poly; fx = only the glow parts (eyes and the inside of the mouth), same bones."""
    f = FORMS[form]
    k, d = f["k"], f["d"]
    ident = "firecast_" + form + ("_fx" if fx else "" if lod == 0 else "_lod%d" % lod)
    m = L.Model(ident, atlas, seed=f["seed"] + lod, tex_scale=0.5 if fx else {0: 1.0, 1: 0.5, 2: 0.25}[lod])
    sk = ("skin", d)

    DY = -5.0                                                         # the whole creature sits lower: a crouch, belly near the ground

    ZO = [0.0]                                                        # z offset of the head subtree: a longer neck pushes the head forward

    def sc(p):
        return [p[0] * k, (p[1] + DY) * k, (p[2] + ZO[0]) * k]

    def B(name, pivot, parent=None, rotation=None):
        m.bone(name, sc(pivot), parent, rotation)

    def C(bone, o, s, faces, rot=None, pivot=None, dens=None):
        if fx:
            return
        m.cube(bone, sc(o), [v * k for v in s], faces, rot, None if pivot is None else sc(pivot), dens if dens else d)

    def X(bone, o, s, faces, rot=None, pivot=None):
        """a glow cube (the fx model only)"""
        if fx:
            m.cube(bone, sc(o), [v * k for v in s], faces, rot, None if pivot is None else sc(pivot), 0)

    sp = ("spike", 0)
    cl = ("claw", 0)

    # ------------------------------------------------------------------ torso
    B("body", (0, 14, 4))
    if lod == 2:
        C("body", (-6.5, 9, -12), (13, 13, 17), {"up": sk, "west": sk, "east": sk, "down": ("belly", d), "north": sk})
        C("body", (-6, 9, 5), (12, 12, 17), {"up": sk, "west": sk, "east": sk, "down": ("belly", d), "south": sk})
    else:
        C("body", (-6.5, 9, -12), (13, 13, 15), {"up": sk, "west": sk, "east": sk, "down": ("belly", d), "north": sk})
        C("body", (-5.5, 9.5, 3), (11, 11.5, 10), {"up": sk, "west": sk, "east": sk, "down": ("belly", d)})
        C("body", (-6, 9, 13), (12, 12, 9), {"up": sk, "west": sk, "east": sk, "down": ("belly", d), "south": sk})
    if f["muscular"] and lod < 2:
        C("body", (-8.5, 12, -11), (6, 9, 8), {"up": sk, "west": sk, "north": sk})                 # the big form's chest: two heavy pectorals and shoulders
        C("body", (2.5, 12, -11), (6, 9, 8), {"up": sk, "east": sk, "north": sk})
    ridge = [(-10.0, 4.0), (-5.0, 4.4), (0.0, 4.6), (5.0, 4.2), (10.0, 3.8), (15.0, 3.4)]
    for i, (z, hgt) in enumerate(ridge):
        if lod == 2 or (lod == 1 and i % 2 == 1):
            continue
        C("body", (-0.5, 22.0, z), (1.0, hgt, 3.0), {"west": sp, "east": sp})

    # ------------------------------------------------------------------ neck and head
    nup = f["neck_up"]
    B("neck", (0, 18, -10), "body", rotation=(nup, 0, 0))
    C("neck", (-4.6, 14.4, -17), (9.2, 9, 7.4), {"up": sk, "west": sk, "east": sk, "down": ("belly", d)})
    head_parent = "neck"
    if lod < 2:
        B("neck2", (0, 19, -16), "neck", rotation=(nup, 0, 0))
        C("neck2", (-4.0, 15.0, -22), (8.0, 8.2, 6.2), {"up": sk, "west": sk, "east": sk, "down": ("belly", d)})
        head_parent = "neck2"
        if f["neck3"]:
            B("neck3", (0, 19, -22), "neck2", rotation=(nup, 0, 0))
            C("neck3", (-3.6, 15.2, -28.4), (7.2, 7.6, 6.6), {"up": sk, "west": sk, "east": sk, "down": ("belly", d)})
            head_parent = "neck3"
            ZO[0] = -6.4
    B("head", (0, 19, -21.5), head_parent)
    C("head", (-5.2, 14.2, -30), (10.4, 9, 9), {"up": ("head_top", 0), "west": ("head_r", 0), "east": ("head_l", 0)})
    C("head", (-3.6, 14.4, -34.2), (7.2, 5.4, 4.6), {"up": ("skin", d), "west": ("snout_r", 0), "east": ("snout_l", 0)})                    # the snout tapers: a base...
    C("head", (-3.0, 14.6, -38.8), (6.0, 4.8, 4.8), {"up": ("skin", d), "west": ("snout_r", 0), "east": ("snout_l", 0), "north": ("snout_front", 0)})      # ...and a blunt tip
    if lod == 0:
        C("head", (-6.2, 22.2, -29.4), (2.6, 1.8, 6.0), {"up": sk, "west": sk, "north": sk})          # the brow ridges
        C("head", (3.6, 22.2, -29.4), (2.6, 1.8, 6.0), {"up": sk, "east": sk, "north": sk})
    B("jaw", (0, 16, -27), "head")
    C("jaw", (-3.3, 12.4, -36.9), (6.6, 3.0, 9.9), {"up": ("jaw_in", 0), "down": ("belly", d), "west": ("jaw_r", 0), "east": ("jaw_l", 0), "north": ("belly", d)})
    crest = [(-26.0, 5.6), (-23.6, 5.0), (-21.2, 4.2), (-18.8, 3.4)]
    for i, (z, hgt) in enumerate(crest):
        if (lod == 2 and i % 2 == 1) or (lod == 1 and i == 4):
            continue
        C("head", (-0.5, 22.6, z), (1.0, hgt, 2.6), {"west": sp, "east": sp}, rot=(-14 - 8 * i, 0, 0), pivot=(0, 22.6, z + 1.3))
    if lod < 2:
        for side in (-1, 1):
            for j in range(3 if lod == 0 else 2):                                                          # frill spines swept back from the cheek, three a side
                z = -23.0 + 1.2 * j
                y = 17.2 + j * 2.4
                ln = 4.6 - 0.5 * j
                ox = -5.4 - ln if side < 0 else 5.4
                C("head", (ox, y, z), (ln, 0.7, 2.4), {"up": sp, "down": sp}, rot=(0, side * (-24 - 10 * j), 0), pivot=(side * 5.4, y, z))
    if f["muscular"] and lod < 2:
        for side in (-1, 1):                                                                              # two swept horns on the big form
            C("head", (side * 3.4 - 0.7, 22.6, -26.0), (1.4, 6.6, 2.2), {"west": sp, "east": sp}, rot=(-48, 0, side * 10), pivot=(side * 3.4, 22.6, -25.0))

    # glow: the slit eyes (a thin slab just proud of each side of the skull) and the furnace in the mouth
    X("head", (-5.3, 14.2, -30), (0.1, 9, 9), {"west": ("head_r", 0)})
    X("head", (5.2, 14.2, -30), (0.1, 9, 9), {"east": ("head_l", 0)})
    X("jaw", (-3.2, 15.0, -37.2), (6.4, 0.1, 9.8), {"up": ("jaw_in", 0)})
    ZO[0] = 0.0

    # ------------------------------------------------------------------ legs: the arms (front) and the haunches; every segment is a bone, so the chain bends as one
    for side, tag in ((-1, "R"), (1, "L")):
        sx = side
        B("arm_" + tag, (sx * 7.6, 15, -6), "body", rotation=(-6, 0, -sx * 26))
        C("arm_" + tag, (sx * 7.6 - 2.3, 9.0, -8.5), (4.6, 7.5, 5.0), {"up": sk, "west": sk, "east": sk, "north": sk, "south": sk})
        if lod < 2:
            B("fore_" + tag, (sx * 7.6, 9.2, -6), "arm_" + tag, rotation=(16, 0, sx * 46))
            C("fore_" + tag, (sx * 7.6 - 1.9, 3.0, -8.0), (3.8, 6.6, 4.0), {"up": sk, "west": sk, "east": sk, "north": sk, "south": sk})
            B("hand_" + tag, (sx * 7.6, 3.6, -6), "fore_" + tag, rotation=(-10, 0, -sx * 20))
            C("hand_" + tag, (sx * 7.6 - 2.4, 0.0, -11.6), (4.8, 2.2, 6.4), {"up": sk, "west": sk, "east": sk, "north": sk})
            if lod == 0:
                for j in range(3):
                    C("hand_" + tag, (sx * 7.6 - 2.0 + j * 1.6, 0.0, -14.6), (0.9, 1.9, 3.4), {"west": cl, "east": cl, "up": cl})
        B("leg_" + tag, (sx * 7, 14, 17), "body", rotation=(-4, 0, -sx * 20))
        C("leg_" + tag, (sx * 7 - 3.0, 6.0, 14.2), (6.0, 9.5, 7.4), {"up": sk, "west": sk, "east": sk, "north": sk, "south": sk})
        if lod < 2:
            B("shin_" + tag, (sx * 7, 6.4, 18.0), "leg_" + tag, rotation=(30, 0, sx * 36))
            C("shin_" + tag, (sx * 7 - 1.9, 1.0, 16.0), (3.8, 6.4, 4.2), {"up": sk, "west": sk, "east": sk, "south": sk})
            B("foot_" + tag, (sx * 7, 1.8, 16.8), "shin_" + tag, rotation=(-22, 0, -sx * 16))
            C("foot_" + tag, (sx * 7 - 2.4, 0.0, 10.8), (4.8, 1.9, 8.6), {"up": sk, "west": sk, "east": sk, "north": sk})
            if lod == 0:
                for j in range(3):
                    C("foot_" + tag, (sx * 7 - 2.0 + j * 1.6, 0.0, 8.4), (0.9, 1.7, 3.0), {"west": cl, "east": cl, "up": cl})

    # ------------------------------------------------------------------ the tail: a chain that curls up, a spike on every segment
    segs = {0: 8, 1: 5, 2: 3}[lod]
    seg_len = 7.0 * 8 / segs
    parent, y, z, w = "body", 17.0, 21.0, 9.2
    for i in range(segs):
        nm = "t%d" % i
        B(nm, (0, y, z), parent, rotation=(-3.4 * 8.0 / segs, 0, 0))
        wi = max(1.6, w * (0.80 ** (i * 8.0 / segs)))
        C(nm, (-wi / 2, y - wi / 2, z), (wi, wi, seg_len), {"up": sk, "west": sk, "east": sk, "down": ("belly", d), "south": sk if i == segs - 1 else None})
        if lod < 2 or i == 1:
            C(nm, (-0.5, y + wi / 2 - 0.2, z + seg_len * 0.15), (1.0, 4.6 * (0.86 ** (i * 8.0 / segs)) + 1.2, seg_len * 0.7), {"west": sp, "east": sp})
        parent = nm
        z += seg_len
        y += 0.0
    # the tail's last spike (the barb)
    B("tail_tip", (0, y, z), parent)
    C("tail_tip", (-0.7, y - 1.0, z - 0.5), (1.4, 5.0, 6.5), {"west": sp, "east": sp}, rot=(-70, 0, 0), pivot=(0, y, z))
    X("tail_tip", (-3.0, y - 1.0, z + 1.5), (6.0, 11.0, 0.2), {"north": ("flame", 0), "south": ("flame", 0)})                                  # a lick of flame off the tail's barb
    X("tail_tip", (-0.1, y - 1.0, z - 1.5), (0.2, 11.0, 6.0), {"west": ("flame", 0), "east": ("flame", 0)})

    # ------------------------------------------------------------------ the wings (wk scales the whole wing: the big form's span is much wider than its body)
    wk = f["wk"]
    hum, fore, chord = 14.0 * wk, 17.0 * wk, WING_CHORD * wk
    wrist = 5.0 + hum
    bar = {"up": ("bone", 0), "down": ("bone", 0), "north": ("bone", 0), "south": ("bone", 0)}
    for side, tag in ((-1, "R"), (1, "L")):
        sx = side
        B("wing_" + tag, (sx * 5, 21.0, -3.0), "body")
        C("wing_" + tag, (5.0, 20.8, -4.1) if sx > 0 else (-5.0 - hum, 20.8, -4.1), (hum, 1.9 * min(wk, 1.3), 1.9 * min(wk, 1.3)), bar)
        B("wingout_" + tag, (sx * wrist, 21.5, -3.0), "wing_" + tag)
        if lod < 2:
            C("wingout_" + tag, (wrist, 20.9, -3.9) if sx > 0 else (-wrist - fore, 20.9, -3.9), (fore, 1.6 * min(wk, 1.3), 1.6 * min(wk, 1.3)), bar)
        rin = ("wing_in", 0, sx < 0)
        rout = ("wing_out", 0, sx < 0)
        inner_o = (5.0, 20.9, -3.4) if sx > 0 else (-5.0 - WING_INNER * wk, 20.9, -3.4)
        outer_o = (wrist, 20.9, -3.4) if sx > 0 else (-wrist - 30.0 * wk, 20.9, -3.4)
        C("wing_" + tag, inner_o, (WING_INNER * wk, 0.25, chord), {"up": rin, "down": rin})
        C("wingout_" + tag, outer_o, (30.0 * wk, 0.25, chord), {"up": rout, "down": rout})
        for fx_, fz_, fs_ in ((0.985, 0.10, 1.0), (0.88, 0.44, 0.7)):                                  # the glow-only model: a lick of flame at the tips of the two outer finger spars
            tx = (5.0 + fx_ * WING_SPAN * wk) * sx
            tz = -3.4 + fz_ * chord
            fw, fh = 7.0 * fs_ * min(wk, 1.2), 12.0 * fs_ * min(wk, 1.2)
            X("wingout_" + tag, (tx - fw / 2, 21.0, tz - 0.1), (fw, fh, 0.2), {"north": ("flame", 0), "south": ("flame", 0)})
            X("wingout_" + tag, (tx - 0.1, 21.0, tz - fw / 2), (0.2, fh, fw), {"west": ("flame", 0), "east": ("flame", 0)})
    return m


# ====================================================================================================================== animations
def animations(form):
    f = FORMS[form]
    big = form == "dragon"
    T = 3.2 if big else 2.4
    clips = {}

    def tail(clip_len, amp, lag, bias):
        tr = {}
        for i in range(8):
            tr["t%d" % i] = {"rotation": L.merge(L.sine(clip_len, amp * (0.6 + 0.18 * i), 1, -lag * i), L.sine(clip_len, amp * 0.25, 0, -lag * i - 0.1), {t: (bias, 0, 0) for t in L.sine(clip_len, 0, 0)})}
        tr["tail_tip"] = {"rotation": L.sine(clip_len, amp, 1, -lag * 8)}
        return tr

    # idle: slow breathing, the tail sways, the wings flutter a little, the head bobs
    bones = {"body": {"scale": {t: (1.0, 1.0 + 0.018 * math.sin(t / T * math.tau), 1.0 + 0.012 * math.sin(t / T * math.tau)) for t in L.sine(T, 0, 0, steps=12)}},
             "head": {"rotation": L.sine(T, 3.0, 0, 0.1, steps=12)}, "jaw": {"rotation": L.sine(T, 2.0, 0, 0.3, steps=12, bias=2.0)},
             "wing_L": {"rotation": L.sine(T, 3.5, 2, 0.0, steps=12)}, "wing_R": {"rotation": L.sine(T, -3.5, 2, 0.0, steps=12)},
             "wingout_L": {"rotation": L.sine(T, 4.5, 2, -0.12, steps=12)}, "wingout_R": {"rotation": L.sine(T, -4.5, 2, -0.12, steps=12)}}
    bones.update(tail(T, 7.0, 0.07, 0.0))
    clips["idle"] = {"loop": True, "length": T, "bones": bones}

    # perch: wings folded up and back on the shoulder, the head turning to look about, the tail curled up behind
    PT = 3.6
    fold = {"wing_L": {"rotation": {t: (0, 0, 0) for t in L.sine(PT, 0, 0, steps=12)}}}
    bones = {"head": {"rotation": L.merge(L.sine(PT, 9.0, 1, 0.0, steps=12), L.sine(PT, 2.5, 0, 0.2, steps=12))},
             "jaw": {"rotation": L.sine(PT, 2.0, 0, 0.3, steps=12, bias=2.0)}, "neck": {"rotation": L.sine(PT, 3.0, 1, 0.1, steps=12)},
             "wing_L": {"rotation": L.merge(L.sine(PT, 1.5, 2, 0.0, steps=12), {t: (0, -42, 64) for t in L.sine(PT, 0, 0, steps=12)})},
             "wing_R": {"rotation": L.merge(L.sine(PT, -1.5, 2, 0.0, steps=12), {t: (0, 42, -64) for t in L.sine(PT, 0, 0, steps=12)})},
             "wingout_L": {"rotation": {t: (0, 52, 60) for t in L.sine(PT, 0, 0, steps=12)}}, "wingout_R": {"rotation": {t: (0, -52, -60) for t in L.sine(PT, 0, 0, steps=12)}},
             "body": {"scale": {t: (1.0, 1.0 + 0.015 * math.sin(t / PT * math.tau), 1.0) for t in L.sine(PT, 0, 0, steps=12)}}}
    bones.update(tail(PT, 4.0, 0.05, -2.0))
    clips["perch"] = {"loop": True, "length": PT, "bones": bones}

    # fly: the wings beat, the body bobs, the legs tuck, the tail streams
    FT = 1.4 if big else 0.8
    bones = {"wing_L": {"rotation": L.sine(FT, 38.0, 2, 0.0)}, "wing_R": {"rotation": L.sine(FT, -38.0, 2, 0.0)},
             "wingout_L": {"rotation": L.sine(FT, 28.0, 2, -0.16)}, "wingout_R": {"rotation": L.sine(FT, -28.0, 2, -0.16)},
             "body": {"position": {t: (0, 0.9 * math.sin((t / FT) * math.tau + 0.6) * f["k"], 0) for t in L.sine(FT, 0, 0)}},
             "head": {"rotation": L.sine(FT, 4.0, 0, 0.2)}, "jaw": {"rotation": {t: (3.0, 0, 0) for t in L.sine(FT, 0, 0)}},
             "arm_L": {"rotation": {t: (-34, 0, 0) for t in L.sine(FT, 0, 0)}}, "arm_R": {"rotation": {t: (-34, 0, 0) for t in L.sine(FT, 0, 0)}},
             "leg_L": {"rotation": {t: (30, 0, 0) for t in L.sine(FT, 0, 0)}}, "leg_R": {"rotation": {t: (30, 0, 0) for t in L.sine(FT, 0, 0)}}}
    bones.update(tail(FT, 8.0, 0.06, 0.0))
    clips["fly"] = {"loop": True, "length": FT, "bones": bones}

    # breathe: the head draws back, the jaw opens wide, the wings spread, then the strike forward; plays once
    BT = 2.0 if big else 1.6
    tt = [0.0, 0.25 * BT, 0.45 * BT, 0.55 * BT, 0.8 * BT, BT]
    bones = {"neck": {"rotation": dict(zip(tt, [(0, 0, 0), (16, 0, 0), (-4, 0, 0), (-12, 0, 0), (-6, 0, 0), (0, 0, 0)]))},
             "head": {"rotation": dict(zip(tt, [(0, 0, 0), (18, 0, 0), (-8, 0, 0), (-14, 0, 0), (-5, 0, 0), (0, 0, 0)]))},
             "jaw": {"rotation": dict(zip(tt, [(2, 0, 0), (6, 0, 0), (30, 0, 0), (32, 0, 0), (14, 0, 0), (2, 0, 0)]))},
             "body": {"rotation": dict(zip(tt, [(0, 0, 0), (-5, 0, 0), (4, 0, 0), (5, 0, 0), (2, 0, 0), (0, 0, 0)]))},
             "wing_L": {"rotation": dict(zip(tt, [(0, 0, 0), (0, 0, 24), (0, 0, 10), (0, 0, 6), (0, 0, 14), (0, 0, 0)]))},
             "wing_R": {"rotation": dict(zip(tt, [(0, 0, 0), (0, 0, -24), (0, 0, -10), (0, 0, -6), (0, 0, -14), (0, 0, 0)]))},
             "wingout_L": {"rotation": dict(zip(tt, [(0, 0, 0), (0, 0, 14), (0, 0, 6), (0, 0, 4), (0, 0, 8), (0, 0, 0)]))},
             "wingout_R": {"rotation": dict(zip(tt, [(0, 0, 0), (0, 0, -14), (0, 0, -6), (0, 0, -4), (0, 0, -8), (0, 0, 0)]))}}
    bones.update({"t%d" % i: {"rotation": dict(zip(tt, [(-4, 0, 0), (-4, 6 + i, 0), (-4, -6 - i, 0), (-4, 6, 0), (-4, 0, 0), (-4, 0, 0)]))} for i in range(8)})
    clips["breathe"] = {"loop": False, "length": BT, "bones": bones}
    return clips


# ====================================================================================================================== main
def main():
    count_only = "--count" in sys.argv
    only = [a for a in sys.argv[1:] if not a.startswith("--")]
    for form in FORMS:
        if only and form not in only:
            continue
        at = make_atlas(form)
        rows = []
        for lod, fx in ((0, False), (1, False), (2, False), (0, True)):
            m = build(form, lod, at, fx)
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


if __name__ == "__main__":
    main()
