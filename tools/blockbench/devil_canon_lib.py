"""Builders for the canon looks of the contract devils (docs/devil_trial_model.md, section 'Canon looks').

A script fallback, said plainly (rule 15): Blockbench and Blender could not run in the session. This library writes cubes into the same lists as gen_devil_trial.py (bones and cubes with per-face UV),
and gen_devil_canon.py exports each devil to Blockbench's source format, the game's geo / animation files and a painted sheet with a glow mask. Everything is deterministic.

Parts (all take the right side, the left is mirrored by pair()): a parametric HUMANOID body (dims: leg length, chest width, head size, limb thickness ...) with feature builders (horn chains, wings of five
kinds, tails with six tips, hair, outfits, lower bodies), and five custom bodies for devils that are not humanoid (fox, two-headed skeleton, tree, flower giant, six-armed statue).
Bone names are shared (root body chest head jaw arm_r arm_r_low arm_r_hand leg_r leg_r_low leg_r_foot wing_r wing_r_tip tail1..3 horn_r mane lock_r) so one set of clips drives every humanoid; a clip
only keeps the bones the devil has.
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import gen_devil_trial as T  # noqa: E402
import numpy as np  # noqa: E402

BONES, CUBES = T.BONES, T.CUBES
cube, bone, pair = T.cube, T.bone, T.pair
mix, tones, hexrgb, lum = T.mix, T.tones, T.hexrgb, T.lum


def reset():
    del BONES[:]
    del CUBES[:]


# ---------------------------------------------------------------- chains (horns, tails, tentacles, vines)
def chain(pfx, parent, sd, n, pivot, segs, mat, axis="y", rot0=(0, 0, 0), paint_last=None, mats=None):
    """A chain of bones, each with one cube along its axis. segs: (length, width, depth, (rx, ry, rz)); the rotation of a segment is relative to the one before. axis: 'y' up, 'ny' down, 'z' back, 'nz' forward.
    Coordinates are in right-side convention (x negative on the right); pivot is the base of the first segment."""
    px, py, pz = pivot
    parent_name = parent
    for i, (ln, w, d, rot) in enumerate(segs):
        name = (pfx + str(i + 1)) if n == "" else "%s_%s" % (pfx, n) + (str(i + 1) if i else "")
        bone(name, parent_name, (px, py, pz), rot=(rot if i else tuple(a + b for a, b in zip(rot, rot0))), side=sd)
        m = mats[i] if mats else mat
        pnt = paint_last if (paint_last and i == len(segs) - 1) else None
        if axis == "y":
            cube(name, name + "_c", (px - w / 2, py, pz - d / 2), (w, ln, d), m, side=sd, paint=pnt)
            py += ln
        elif axis == "ny":
            cube(name, name + "_c", (px - w / 2, py - ln, pz - d / 2), (w, ln, d), m, side=sd, paint=pnt)
            py -= ln
        elif axis == "z":
            cube(name, name + "_c", (px - w / 2, py - d / 2, pz), (w, d, ln), m, side=sd, paint=pnt)
            pz += ln
        else:
            cube(name, name + "_c", (px - w / 2, py - d / 2, pz - ln), (w, d, ln), m, side=sd, paint=pnt)
            pz -= ln
        parent_name = name
    return parent_name


def dims(spec):
    D = dict(hip=13.0, lt=4.4, lx=2.6, pw=9.4, ah=4.2, aw=8.4, cw=12.0, chh=6.2, cd=6.6, hs=8.0, at=4.2, ua=7.0, fa=6.8, hand=3.0)
    D.update(spec.get("dims", {}))
    D["pel_y"] = D["hip"] - 1.6
    D["ab_y"] = D["hip"] + 1.4
    D["ch_y"] = D["ab_y"] + D["ah"] - 0.2
    D["sh_y"] = D["ch_y"] + D["chh"]
    D["head_y"] = D["sh_y"] + 0.4
    D["hk"] = D["hs"] / 8.0
    D["top"] = D["head_y"] + D["hs"]
    return D


def hcube(D, name, o, s, mat, bone_name="head", rot=None, pv=None, side=-1, paint=None):
    """A cube in head-local coordinates of an 8 px head (x -4..4, y 0..8 up from the bottom, z -4..4), scaled by the head size."""
    k, hy = D["hk"], D["head_y"]
    oo = (o[0] * k, hy + o[1] * k, o[2] * k)
    ss = tuple(v * k for v in s)
    pp = None if pv is None else (pv[0] * k, hy + pv[1] * k, pv[2] * k)
    cube(bone_name, name, oo, ss, mat, rot=rot, pv=pp, side=side, paint=paint)


def hpt(D, x, y, z):
    k = D["hk"]
    return (x * k, D["head_y"] + y * k, z * k)


# ---------------------------------------------------------------- the humanoid body
def humanoid(spec):
    D = dims(spec)
    hip, lt, lx = D["hip"], D["lt"], D["lx"]
    st = spec.get("legs", "hoof")
    bone("root", None, (0, 0, 0))
    bone("body", "root", (0, hip, 0))
    bone("chest", "body", (0, D["ch_y"] - 0.4, 0))
    bone("head", "chest", (0, D["sh_y"] - 0.1, 0))
    bone("jaw", "head", (0, D["head_y"] + 1.2 * D["hk"], -1.6 * D["hk"]))
    bone("mane", "head", (0, D["top"] - 1.5 * D["hk"], 3.2 * D["hk"]))
    knee, ank = hip * 0.52, hip * 0.14
    LEG_MAT = spec.get("leg_mat", "skin")

    def legs(sd, n):
        bone("leg_" + n, "root", (-lx, hip, 0), side=sd)
        cube("leg_" + n, "thigh_" + n, (-lx - lt / 2 - 0.2, knee - 0.2, -lt / 2 - 0.1), (lt, hip - knee + 0.4, lt + 0.2), LEG_MAT, side=sd)
        bone("leg_%s_low" % n, "leg_" + n, (-lx, knee, 0.6), side=sd)
        cube("leg_%s_low" % n, "shin_" + n, (-lx - lt / 2 + 0.1, ank, -lt / 2 + 0.3), (lt - 0.4, knee - ank + 0.4, lt - 0.6), LEG_MAT, rot=(14, 0, 0), pv=(-lx, knee, 0.6), side=sd)
        bone("leg_%s_foot" % n, "leg_%s_low" % n, (-lx, ank, -0.4), side=sd)
        fw = lt - 0.2
        if st in ("hoof", "claw"):
            cube("leg_%s_foot" % n, "hoof_" + n, (-lx - fw / 2, 0.0, -3.4), (fw, ank + 0.2, 5.4), "hoof", side=sd)
            cube("leg_%s_foot" % n, "toe_a_" + n, (-lx - fw / 2 + 0.2, 0.0, -4.4), (fw * 0.38, 1.5, 1.2), "claw", side=sd)
            cube("leg_%s_foot" % n, "toe_b_" + n, (-lx + 0.4, 0.0, -4.4), (fw * 0.38, 1.5, 1.2), "claw", side=sd)
        elif st == "boot":
            cube("leg_%s_foot" % n, "boot_" + n, (-lx - fw / 2 - 0.3, 0.0, -4.0), (fw + 0.6, ank + 0.4, 6.4), "boot", side=sd)
        elif st == "heel":                                                  # a stiletto: the sole on the toes and a thin heel under the ankle
            cube("leg_%s_foot" % n, "boot_" + n, (-lx - fw / 2, ank * 0.55, -3.8), (fw, ank * 0.5, 6.0), "boot", side=sd)
            cube("leg_%s_foot" % n, "heel_" + n, (-lx - 0.5, 0.0, 1.0), (1.0, ank * 0.6, 1.0), "boot", side=sd)
            cube("leg_%s_foot" % n, "sole_" + n, (-lx - fw / 2, 0.0, -3.8), (fw, ank * 0.45, 2.2), "boot", side=sd)
        if spec.get("pants"):                                               # trousers over the thigh and shin
            cube("leg_" + n, "pant_up_" + n, (-lx - lt / 2 - 0.7, knee - 0.4, -lt / 2 - 0.6), (lt + 1.4, hip - knee + 0.9, lt + 1.4), spec["pants"], side=sd)
            cube("leg_%s_low" % n, "pant_lo_" + n, (-lx - lt / 2 - 0.4, ank + 0.8, -lt / 2 - 0.1), (lt + 0.8, knee - ank - 0.6, lt + 0.4), spec["pants"], side=sd)
    if st not in ("serpent", "tentacles", "none"):
        pair(legs)
    # torso
    cube("body", "pelvis", (-D["pw"] / 2, D["pel_y"], -2.7), (D["pw"], 3.2, 5.4), spec.get("pelvis_mat", "skin"))
    cube("body", "belt", (-D["pw"] / 2 - 0.3, D["pel_y"] + 1.2, -3.0), (D["pw"] + 0.6, 1.8, 6.0), spec.get("belt_mat", "accent"))
    cube("body", "belt_gem", (-1.1, D["pel_y"] + 1.1, -3.4), (2.2, 2.0, 0.6), "gem" if spec.get("belt_gem", True) else spec.get("belt_mat", "accent"))
    cube("body", "abdomen", (-D["aw"] / 2, D["ab_y"], -2.4), (D["aw"], D["ah"], 4.8), spec.get("torso_mat", "skin"), paint="abs")
    cube("chest", "chest", (-D["cw"] / 2, D["ch_y"], -D["cd"] / 2), (D["cw"], D["chh"], D["cd"]), spec.get("torso_mat", "skin"), paint="chest")
    cube("chest", "neck", (-2.0, D["sh_y"] - 0.2, -1.8), (4.0, 1.4, 3.6), spec.get("neck_mat", "skin"))
    # the head
    hs = D["hs"]
    hcube(D, "head", (-4, 0, -4), (8, 8, 8), spec.get("head_mat", "skin"), paint="face")
    if spec.get("brow", True):
        hcube(D, "brow", (-4.4, 5.0, -4.5), (8.8, 1.2, 1.5), spec.get("head_mat", "skin"))
    if spec.get("nose", True):
        hcube(D, "nose", (-0.8, 1.8, -4.8), (1.6, 2.4, 1.0), spec.get("head_mat", "skin"))
    hcube(D, "jaw", (-3.4, 0.0, -4.3), (6.8, 1.9, 4.7), spec.get("head_mat", "skin"), bone_name="jaw", paint="jaw")
    if spec.get("fangs", True):
        hcube(D, "fang_r", (-3.0, 1.8, -4.2), (0.8, 1.0, 0.8), "tooth", bone_name="jaw")
        hcube(D, "fang_l", (2.2, 1.8, -4.2), (0.8, 1.0, 0.8), "tooth", bone_name="jaw")
    if spec.get("chin_spike"):
        hcube(D, "chin_spike", (-0.6, -1.1, -4.2), (1.2, 1.3, 1.0), "horn", bone_name="jaw")
    ears = spec.get("ears", "pointed")
    if ears:
        L = {"pointed": (2.4, 24, 1.0), "long": (4.6, 20, 1.0), "short": (1.6, 10, 1.0), "round": (1.4, 0, 1.4), "fox": (3.2, 14, 2.4)}[ears]

        def ear(sd, n):
            hcube(D, "ear_" + n, (-4.3 - L[0], 3.4, -0.8), (L[0], L[2], 2.0), spec.get("head_mat", "skin"), rot=(0, 0, L[1]), side=sd)
            if ears in ("pointed", "long"):
                hcube(D, "ear_tip_" + n, (-4.5 - L[0] - 1.4, 4.0, -0.5), (1.8, 0.8, 1.4), spec.get("head_mat", "skin"), rot=(0, 0, L[1] + 14), side=sd)
        pair(ear)
    if spec.get("cheeks", False):
        def cheek(sd, n):
            hcube(D, "cheek_" + n, (-5.1, 1.2, -2.4), (1.2, 1.6, 2.6), spec.get("head_mat", "skin"), rot=(0, 0, 18), side=sd)
        pair(cheek)
    hair(spec.get("hair"), D, spec)
    horns(spec.get("horns", []), D)
    # arms
    at, ua, fa, ax = D["at"], D["ua"], D["fa"], D["cw"] / 2 + 1.0
    sh = D["sh_y"] - 1.0
    ARM = spec.get("arm_mat", "skin")
    claws = spec.get("claws", 3)

    def arms(sd, n):
        bone("arm_" + n, "chest", (-ax, sh, 0), side=sd)
        cube("arm_" + n, "upper_" + n, (-ax - at / 2 - 0.4, sh - ua, -at / 2 - 0.1), (at, ua + 0.2, at + 0.2), ARM, side=sd)
        bone("arm_%s_low" % n, "arm_" + n, (-ax + 0.2, sh - ua + 0.4, 0), side=sd)
        cube("arm_%s_low" % n, "fore_" + n, (-ax - at / 2 - 0.1, sh - ua - fa + 0.6, -at / 2 - 0.2), (at + 0.2, fa, at + 0.4), ARM, side=sd)
        bone("arm_%s_hand" % n, "arm_%s_low" % n, (-ax + 0.2, sh - ua - fa + 0.8, 0), side=sd)
        hy = sh - ua - fa + 0.8
        hm = spec.get("hand_mat", ARM)
        cube("arm_%s_hand" % n, "palm_" + n, (-ax - 1.7, hy - 3.0, -1.9), (3.6, 3.0, 3.8), hm, side=sd)
        cl = spec.get("claw_len", 2.8)
        for i in range(claws):
            z = -1.7 + i * (3.4 / max(1, claws - 1)) if claws > 1 else -0.5
            cube("arm_%s_hand" % n, "claw_%d_%s" % (i, n), (-ax - 1.3 + 0.9 * (i == 1), hy - 3.0 - cl, z), (0.9, cl, 1.0), spec.get("claw_mat", "claw"), side=sd)
        if claws > 1:
            cube("arm_%s_hand" % n, "thumb_" + n, (-ax - 2.4, hy - 2.6, -0.6), (1.0, 2.6, 1.0), spec.get("claw_mat", "claw"), rot=(0, 0, -14), side=sd)
    pair(arms)
    outfit(spec.get("outfit"), D, spec)
    for w in spec.get("wings", []):
        wings(w, D)
    if spec.get("tail"):
        tail(spec["tail"], D, spec)
    lower(spec, D)
    for ex in spec.get("extras", []):
        EXTRAS[ex[0]](D, spec, *ex[1:])
    return D


# ---------------------------------------------------------------- lower bodies that replace the legs
def lower(spec, D):
    st = spec.get("legs", "hoof")
    hip = D["hip"]
    if st == "serpent":                                                   # a serpentine tail of rock, thick at the pelvis, along the ground behind
        down = hip - 3.6                                                  # down to the bend, then three segments along the ground behind
        segs = [(down * 0.6, 8.6, 7.6, (0, 0, 0)), (down * 0.4, 7.4, 6.6, (-6, 0, 0)), (7.0, 6.2, 5.4, (-78, 0, 0)), (7.0, 4.8, 4.0, (-6, 0, 0)), (7.0, 3.2, 2.8, (-6, 0, 0))]
        chain("tail", "body", -1, "", (0, D["pel_y"] + 1.0, 0), segs, spec.get("serpent_mat", "skin"), axis="ny", mats=spec.get("serpent_mats"))
    elif st == "tentacles":                                               # eight limp arms of water
        for i, (x, z, a) in enumerate(((-2.6, -1.8, 0), (2.6, -1.8, 0), (-4.4, 0.6, 10), (4.4, 0.6, -10), (-2.0, 2.8, -6), (2.0, 2.8, 6))):
            segs = [(hip * 0.45, 3.2, 3.2, (6, 0, a)), (hip * 0.4, 2.6, 2.6, (-10, 0, 0)), (hip * 0.35, 2.0, 2.0, (-16, 0, 0)), (hip * 0.3, 1.4, 1.4, (-10, 0, 0))]
            if i < 2:
                chain("leg", "root", 1 if x > 0 else -1, "l" if x > 0 else "r", (-abs(x), hip, z), segs, "water", axis="ny")
            else:
                chain("tent%d" % i, "root", 1 if x > 0 else -1, "", (-abs(x), hip, z), segs, "water", axis="ny")


# ---------------------------------------------------------------- hair
def hair(h, D, spec):
    if not h:
        return
    m = h.get("mat", "hair")
    hk = D["hk"]
    if h.get("cap", True):
        hcube(D, "scalp", (-4.4, 6.0, -4.4), (8.8, 2.4, 8.8), m)
        hcube(D, "scalp_back", (-4.4, 0.6, 3.7), (8.8, 6.0, 1.4), m)
        hcube(D, "scalp_side_r", (-4.5, 3.4, -3.0), (0.6, 3.2, 6.2), m)
        hcube(D, "scalp_side_l", (3.9, 3.4, -3.0), (0.6, 3.2, 6.2), m)
    if h.get("fringe", 3):
        hcube(D, "fringe", (-4.4, 6.0, -4.8), (8.8, 1.2, 0.9), m)
        n = h.get("fringe", 3)
        for i in range(n):
            x = -4.2 + i * (7.2 / max(1, n - 1)) if n > 1 else -0.6
            ln = h.get("fringe_len", 2.4) * (1.0 if i % 2 == 0 else 0.7)
            hcube(D, "fringe_tuft_%d" % i, (x, 6.4 - ln, -4.9), (1.2, ln, 0.8), m)
    for i in range(h.get("spikes", 0)):                                   # spikes standing up from the crown, fanned
        a = (i - (h["spikes"] - 1) / 2.0) * (h.get("spike_fan", 24))
        ln = h.get("spike_len", 4.0) * (1.0 - 0.15 * (i % 2))
        hcube(D, "spike_%d" % i, (-0.7 + (i - (h["spikes"] - 1) / 2.0) * 1.5, 8.2, -1.6 + (i % 3) * 1.4), (1.5, ln, 1.5), m, rot=(h.get("spike_back", -14) * ((i % 2) + 0.4), 0, a))
    if h.get("back", 0):                                                  # a hanging back panel
        ln = h["back"]
        hcube(D, "mane_a", (-3.4 * h.get("back_w", 1.0), 8.0 - ln, 3.9), (6.8 * h.get("back_w", 1.0), ln, 1.6), m, bone_name="mane", rot=(-6, 0, 0))
        if ln > 9:
            hcube(D, "mane_b", (-2.6 * h.get("back_w", 1.0), 8.0 - ln - 4, 4.3), (5.2 * h.get("back_w", 1.0), 4.6, 1.4), m, bone_name="mane", rot=(-12, 0, 0))
    if h.get("side", 0):
        def lock(sd, n):
            bone("lock_" + n, "head", hpt(D, -3.6, 8.0, 4.0)[:1] + hpt(D, -3.6, 8.0, 4.0)[1:], side=sd)
            ln = h["side"]
            hcube(D, "lock_a_" + n, (-4.6, 7.0 - ln, -1.2), (1.8, ln, 2.6), m, bone_name="lock_" + n, side=sd)
            if h.get("side_tip", True):
                hcube(D, "lock_b_" + n, (-4.4, 5.6 - ln, -0.9), (1.4, 2.4, 2.0), m, bone_name="lock_" + n, rot=(-6, 0, 0), side=sd)
        pair(lock)
    if h.get("tail"):                                                     # a ponytail: a chain from the back of the head
        t = h["tail"]
        px, py, pz = hpt(D, t.get("x", 0.0), t.get("y", 6.5), 4.4)
        segs = [(t.get("len", 5.0) * f, t.get("w", 3.0) * g, t.get("w", 3.0) * g, (t.get("rx", 10) if i else t.get("rx0", 40), 0, t.get("rz", 0))) for i, (f, g) in enumerate(((1.0, 1.0), (1.0, 0.9), (0.9, 0.8), (0.8, 0.6)))][:t.get("n", 3)]
        chain("ptail", "head", -1 if px <= 0 else 1, "", (px if px <= 0 else -px, py, pz), segs, t.get("mat", m), axis="ny")
    for i, (x, y, ln, a) in enumerate(h.get("tufts", [])):               # free tufts (flame bulbs, antennae)
        hcube(D, "tuft_%d" % i, (x, y, 0.2), (1.8, ln, 1.8), h.get("tuft_mat", m), rot=(0, 0, a))


# ---------------------------------------------------------------- horns
def horns(hs_list, D):
    """Each entry: dict(kind, x, y, z, ...). kinds: bull, curve, ram, up, spike, tear, flat. Names horn_r, horn_r2, ..., then horn2_r ... for the second pair."""
    hk = D["hk"]
    for idx, h in enumerate(hs_list):
        pfx = "horn" if idx == 0 else "horn%d" % (idx + 1)
        x, y, z = h.get("x", 2.9), h.get("y", 8.4), h.get("z", -0.6)
        s = h.get("s", 1.0) * hk
        kind = h["kind"]
        mat = h.get("mat", "horn")
        tipmat = h.get("tip", "horn_tip")
        if kind == "bull":     # up and out, then up
            segs = [(3.2 * s, 2.8 * s, 2.8 * s, (8, 0, 24)), (3.4 * s, 2.2 * s, 2.2 * s, (-18, 0, 20)), (3.6 * s, 1.4 * s, 1.4 * s, (-26, 0, 22))]
        elif kind == "ram":    # out, round and forward
            segs = [(3.0 * s, 3.4 * s, 3.4 * s, (0, 0, 52)), (3.6 * s, 3.0 * s, 3.0 * s, (0, 0, 54)), (3.4 * s, 2.4 * s, 2.4 * s, (0, 0, 62)), (3.0 * s, 1.5 * s, 1.5 * s, (-26, 0, 50))]
        elif kind == "curve":  # long, curving up and back (Lucifero)
            segs = [(4.0 * s, 2.6 * s, 2.6 * s, (-4, 0, 26)), (4.4 * s, 2.0 * s, 2.0 * s, (-12, 0, 22)), (4.4 * s, 1.4 * s, 1.4 * s, (-20, 0, 18)), (3.2 * s, 0.9 * s, 0.9 * s, (-24, 0, 16))]
        elif kind == "up":     # small and upright
            segs = [(3.0 * s, 1.8 * s, 1.8 * s, (0, 0, 8)), (2.6 * s, 1.0 * s, 1.0 * s, (-4, 0, 6))]
        elif kind == "spike":  # a single short cone
            segs = [(2.4 * s, 1.8 * s, 1.8 * s, (0, 0, 0)), (1.8 * s, 0.9 * s, 0.9 * s, (0, 0, 0))]
        elif kind == "tear":   # a teardrop: thick near the middle, thin tip, bent back
            segs = [(2.2 * s, 2.4 * s, 2.4 * s, (-10, 0, 0)), (3.2 * s, 2.0 * s, 2.0 * s, (-20, 0, 0)), (2.6 * s, 1.0 * s, 1.0 * s, (-30, 0, 0))]
        elif kind == "sweep":  # back along the skull
            segs = [(3.2 * s, 2.6 * s, 2.6 * s, (-50, 0, 14)), (4.0 * s, 2.0 * s, 2.0 * s, (-24, 0, 6)), (3.4 * s, 1.2 * s, 1.2 * s, (-20, 0, 4))]
        else:
            raise SystemExit("horn kind " + kind)
        if h.get("single"):    # one horn on the middle line (Amaimon's spike, a forehead horn)
            chain(pfx, "head", -1, "r", hpt(D, h.get("x", 0.0) - 0.0, y, z)[0:1] + hpt(D, 0, y, z)[1:], [(sg[0], sg[1], sg[2], (sg[3][0], 0, 0)) for sg in segs], mat, axis="y", mats=[mat] * (len(segs) - 1) + [tipmat])
            continue
        px, py, pz = hpt(D, -x, y, z)

        def one(sd, n):
            chain(pfx, "head", sd, n, (px, py, pz), segs, mat, axis="y", mats=[mat] * (len(segs) - 1) + [tipmat])
        pair(one)


# ---------------------------------------------------------------- wings
def wings(w, D):
    kind = w.get("kind", "bat")
    pfx = w.get("pfx", "wing")
    sh = D["sh_y"]
    sp = w.get("span", 1.0)
    dr = w.get("drop", 10.5) / 10.5
    ry = w.get("y", 0.0)
    mem = w.get("mem", "membrane")
    arm = w.get("arm", "horn")
    base_rot = w.get("rot", (0, 14, 10))
    if kind == "bat":
        def one(sd, n):
            r0 = (base_rot[0], base_rot[1], base_rot[2])
            bone("%s_%s" % (pfx, n), "chest", (-2.4, sh - 1.4 + ry, 3.6), rot=r0, side=sd)
            xa = 2.2 + 9.4 * sp
            cube("%s_%s" % (pfx, n), "%s_arm_a_%s" % (pfx, n), (-xa, sh - 2.2 + ry, 3.0), (9.4 * sp, 1.8, 1.8), arm, side=sd)
            cube("%s_%s" % (pfx, n), "%s_root_%s" % (pfx, n), (-3.6, sh - 3.0 + ry, 2.6), (2.4, 3.2, 2.6), w.get("root", "skin"), side=sd)
            bone("%s_%s_tip" % (pfx, n), "%s_%s" % (pfx, n), (-xa, sh - 1.4 + ry, 3.9), rot=(0, 10, 8), side=sd)
            xb = xa + 7.6 * sp
            cube("%s_%s_tip" % (pfx, n), "%s_arm_b_%s" % (pfx, n), (-xb, sh - 2.0 + ry, 3.4), (7.6 * sp, 1.4, 1.4), arm, side=sd)
            cube("%s_%s_tip" % (pfx, n), "%s_hook_%s" % (pfx, n), (-xb - 1.6, sh - 2.4 + ry, 3.0), (1.8, 1.0, 2.2), "claw", side=sd)
            lens = (8.5, 10.5, 9.0, 6.5, 4.0)
            npan = w.get("panels", 5)
            for i in range(npan):
                x1 = 3.4 * sp + i * (3.1 * sp)
                cube("%s_%s" % (pfx, n) if i < 3 else "%s_%s_tip" % (pfx, n), "%s_mem_%d_%s" % (pfx, i, n), (-(x1 + 3.1 * sp), sh - 1.6 + ry - lens[i] * dr, 4.1), (3.1 * sp, lens[i] * dr, 0.4), mem, side=sd, paint="membrane")
            for i, (x, ln) in enumerate(((7.0, 10.5), (12.0, 9.0), (17.0, 5.0))):
                cube("%s_%s" % (pfx, n) if i < 2 else "%s_%s_tip" % (pfx, n), "%s_rib_%d_%s" % (pfx, i, n), (-x * sp - 0.4, sh - 1.2 + ry - ln * dr, 3.9), (0.8, ln * dr, 0.9), arm, side=sd)
        pair(one)
    elif kind == "feather":                                               # a bone arm and three rows of long feathers
        rows = w.get("rows", 3)
        nf = w.get("n", 6)

        def one(sd, n):
            bone("%s_%s" % (pfx, n), "chest", (-2.4, sh - 1.4 + ry, 3.6), rot=base_rot, side=sd)
            xa = 2.4 + 12.0 * sp
            cube("%s_%s" % (pfx, n), "%s_arm_%s" % (pfx, n), (-xa, sh - 2.0 + ry, 3.0), (12.0 * sp, 1.8, 1.8), arm, side=sd)
            bone("%s_%s_tip" % (pfx, n), "%s_%s" % (pfx, n), (-xa, sh - 1.2 + ry, 3.6), rot=(0, 8, 6), side=sd)
            for r in range(rows):
                for i in range(nf):
                    fx = 3.2 * sp + i * (xa - 3.0 * sp) / max(1, nf - 1)
                    ln = (11.0 - r * 2.6) * dr * (0.6 + 0.4 * math.sin(math.pi * (i + 0.5) / nf)) + 3
                    ang = 8 + i * 4 + r * 3
                    cube("%s_%s" % (pfx, n) if i < nf // 2 + 1 else "%s_%s_tip" % (pfx, n), "%s_f_%d_%d_%s" % (pfx, r, i, n), (-fx - 0.9, sh - 1.4 + ry - ln, 3.4 + r * 0.55), (1.8, ln, 0.5), mem if r % 2 == 0 else w.get("mem2", mem),
                         rot=(0, 0, ang), pv=(-fx, sh - 1.4 + ry, 3.4 + r * 0.55), side=sd)
        pair(one)
    elif kind == "blades":                                                # a fan of bony blades (Ultima's six wings a side)
        nb = w.get("n", 6)

        def one(sd, n):
            bone("%s_%s" % (pfx, n), "chest", (-2.2, sh - 2.0 + ry, 3.4), rot=(0, 10, 0), side=sd)
            for i in range(nb):
                a = -18 + i * (86.0 / max(1, nb - 1))
                ln = (13.0 - 4.0 * abs(i - nb / 2.0) / nb * 2) * sp
                bn = "%s_%s_b%d" % (pfx, n, i)
                bone(bn, "%s_%s" % (pfx, n), (-2.2, sh - 2.0 + ry, 3.4 + 0.5 * (i % 2)), rot=(0, 20 + (i % 3) * 10, a), side=sd)
                cube(bn, bn + "_c", (-2.2 - ln, sh - 2.0 + ry - 0.6, 3.4 + 0.5 * (i % 2)), (ln, 1.2, 0.6), mem if i % 2 == 0 else arm, side=sd)
                cube(bn, bn + "_t", (-2.2 - ln - 2.2, sh - 2.0 + ry - 0.3, 3.4 + 0.5 * (i % 2)), (2.4, 0.6, 0.5), "claw", side=sd)
        pair(one)
    else:
        raise SystemExit("wing kind " + kind)


# ---------------------------------------------------------------- tails
def tail(t, D, spec):
    """t: dict(len, w, tip, mat, n). tip: spade, heart, trident, flame, fluff, none, blade."""
    hip = D["hip"]
    ln, w = t.get("len", 5.6), t.get("w", 3.4)
    mat = t.get("mat", spec.get("tail_mat", "skin"))
    n = t.get("n", 3)
    z0 = 2.8
    segs = []
    for i in range(n):
        f = 1.0 - 0.28 * i
        segs.append((ln * (1.0 if i == 0 else 0.95), w * f, w * f, (-8 if i == 0 else (10 if i == 1 else -16), 0, 0)))
    chain("tail", "body", -1, "", (0, hip - 0.2, z0), segs, mat, axis="z", mats=None)
    tip = t.get("tip", "spade")
    last = "tail%d" % n
    zt = z0 + sum(s[0] for s in segs)
    ymid = hip - 0.2
    if tip == "spade":
        cube(last, "tail_spade", (-2.0, ymid - 0.6, zt), (4.0, 0.7, 4.0), "horn", rot=(0, 45, 0))
        cube(last, "tail_spade_fin", (-0.35, ymid - 1.8, zt - 0.5), (0.7, 3.0, 4.8), "horn")
    elif tip == "heart":
        cube(last, "tail_heart_a", (-2.6, ymid - 1.0, zt), (2.6, 2.0, 0.8), "accent", rot=(0, 0, 40), pv=(-1.3, ymid, zt))
        cube(last, "tail_heart_b", (0.0, ymid - 1.0, zt), (2.6, 2.0, 0.8), "accent", rot=(0, 0, -40), pv=(1.3, ymid, zt))
        cube(last, "tail_heart_c", (-1.0, ymid - 2.6, zt), (2.0, 2.0, 0.8), "accent", rot=(0, 0, 45))
    elif tip == "trident":
        for i, (x, a) in enumerate(((-1.4, 18), (0.0, 0), (1.4, -18))):
            cube(last, "tail_prong_%d" % i, (x - 0.4, ymid - 0.5, zt), (0.8, 0.8, 4.2 if i == 1 else 3.2), "horn", rot=(0, a, 0), pv=(x, ymid, zt))
        cube(last, "tail_prong_base", (-1.8, ymid - 0.6, zt - 0.4), (3.6, 1.0, 0.9), "horn")
    elif tip == "flame":
        cube(last, "tail_flame_a", (-1.8, ymid - 1.4, zt), (3.6, 3.6, 4.0), "flame")
        cube(last, "tail_flame_b", (-1.0, ymid - 0.4, zt + 3.6), (2.0, 2.0, 3.2), "flame")
    elif tip == "fluff":
        cube(last, "tail_fluff_a", (-3.2, ymid - 3.2, zt - 1.0), (6.4, 6.4, 7.0), t.get("fluff", "fur"))
        cube(last, "tail_fluff_b", (-2.2, ymid - 2.2, zt + 6.0), (4.4, 4.4, 4.0), t.get("fluff2", "white"))
    elif tip == "blade":
        cube(last, "tail_blade", (-0.6, ymid - 2.4, zt), (1.2, 4.8, 5.4), "metal")


EXTRAS = {}


def extra(fn):
    EXTRAS[fn.__name__] = fn
    return fn


# ---------------------------------------------------------------- outfits
def outfit(o, D, spec):
    if not o:
        return
    cw, cd, ch_y, chh, ab_y, ah, hip, pw = D["cw"], D["cd"], D["ch_y"], D["chh"], D["ab_y"], D["ah"], D["hip"], D["pw"]
    cl, cl2 = "cloth", "cloth2"
    if o in ("butler", "prince", "military", "coat", "maid", "dress"):
        # a jacket shell over the chest and the abdomen
        cube("chest", "jacket", (-cw / 2 - 0.5, ch_y - 0.2, -cd / 2 - 0.4), (cw + 1.0, chh - 0.6, cd + 0.8), cl)
        cube("body", "jacket_low", (-D["aw"] / 2 - 0.4, ab_y - 0.2, -2.8), (D["aw"] + 0.8, ah + 0.2, 5.6), cl)
        cube("chest", "collar", (-3.6, D["sh_y"] - 0.9, -2.6), (7.2, 1.4, 5.2), cl2)
        if o in ("butler", "prince", "coat"):
            cube("chest", "vest", (-2.4, ch_y + 0.4, -cd / 2 - 0.9), (4.8, chh - 1.4, 0.6), "white" if o == "butler" else cl2)
            cube("body", "coat_tail_a", (-3.8, hip - 6.6, 2.6), (3.4, 8.8, 1.0), cl, rot=(-4, 0, 0))
            cube("body", "coat_tail_b", (0.4, hip - 6.6, 2.6), (3.4, 8.8, 1.0), cl, rot=(-4, 0, 0))
            if o == "butler":
                cube("chest", "tie", (-0.6, ch_y + 1.2, -cd / 2 - 1.1), (1.2, 3.4, 0.5), "accent")
                cube("chest", "watch_chain", (1.6, ch_y + 0.6, -cd / 2 - 1.1), (1.8, 0.3, 0.3), "metal")
        if o in ("prince", "military"):
            def epaulet(sd, n):
                cube("arm_" + n, "epaulet_" + n, (-cw / 2 - 3.2, D["sh_y"] - 1.6, -3.4), (5.0, 1.4, 6.8), "accent", side=sd)
                cube("arm_%s_low" % n, "cuff_" + n, (-cw / 2 - 2.6, D["sh_y"] - 1.0 - D["ua"] - D["fa"] + 0.6, -2.8), (D["at"] + 0.8, 2.0, D["at"] + 1.2), cl2, side=sd)
            pair(epaulet)
        # sleeves
        def sleeves(sd, n):
            cube("arm_" + n, "sleeve_up_" + n, (-cw / 2 - 1.0 - D["at"] / 2 - 0.4, D["sh_y"] - 1.0 - D["ua"], -D["at"] / 2 - 0.5), (D["at"] + 0.8, D["ua"] + 0.4, D["at"] + 1.0), cl, side=sd)
            cube("arm_%s_low" % n, "sleeve_lo_" + n, (-cw / 2 - 1.0 - D["at"] / 2 - 0.3, D["sh_y"] - 1.0 - D["ua"] - D["fa"] * (0.55 if o in ("maid", "dress") else 0.86) + 0.6, -D["at"] / 2 - 0.6), (D["at"] + 0.6, D["fa"] * (0.55 if o in ("maid", "dress") else 0.86), D["at"] + 1.2), cl if o not in ("maid", "dress") else "glove", side=sd)
            if o == "butler":
                cube("arm_%s_hand" % n, "glove_" + n, (-cw / 2 - 1.0 - 1.9, D["sh_y"] - 1.0 - D["ua"] - D["fa"] + 0.5, -2.1), (3.8, 3.2, 4.2), "white", side=sd)
        pair(sleeves)
        if o in ("military",):
            cube("body", "skirt", (-pw / 2 - 1.2, hip - 6.0, -4.2), (pw + 2.4, 7.2, 8.4), cl2)
        if o in ("maid", "dress"):
            cube("body", "skirt_a", (-pw / 2 - 1.4, hip - 3.0, -4.6), (pw + 2.8, 4.8, 9.2), cl)
            cube("body", "skirt_b", (-pw / 2 - 2.6, hip - 7.4, -5.6), (pw + 5.2, 4.8, 11.2), cl)
            cube("body", "apron", (-3.0, hip - 6.8, -6.0), (6.0, 9.0, 0.6), "white")
            cube("chest", "apron_top", (-2.8, ch_y + 0.2, -cd / 2 - 1.0), (5.6, chh - 0.8, 0.5), "white")
        if o == "coat" and spec.get("coat_len"):
            cube("body", "coat_skirt", (-pw / 2 - 1.0, hip - spec["coat_len"], -3.6), (pw + 2.0, spec["coat_len"] + 1.0, 7.2), cl)
    elif o == "open_jacket":                                              # bare chest, jacket sleeves and shoulders, a sash
        def sleeves(sd, n):
            cube("arm_" + n, "sleeve_up_" + n, (-cw / 2 - 1.0 - D["at"] / 2 - 0.4, D["sh_y"] - 1.0 - D["ua"], -D["at"] / 2 - 0.5), (D["at"] + 0.8, D["ua"] + 0.4, D["at"] + 1.0), cl, side=sd)
            cube("chest", "lapel_" + n, (-cw / 2 - 0.5, ch_y - 0.2, -cd / 2 - 0.5), (2.6, chh - 0.4, cd + 1.0), cl, side=sd)
        pair(sleeves)
        cube("chest", "jacket_back", (-cw / 2 - 0.5, ch_y - 0.2, cd / 2 - 0.2), (cw + 1.0, chh - 0.4, 0.9), cl)
        cube("body", "sash", (-pw / 2 - 0.6, hip - 0.4, -3.2), (pw + 1.2, 2.6, 6.4), cl2)
        cube("body", "sash_tail", (1.6, hip - 6.6, -3.5), (1.8, 7.0, 0.7), cl2)
        for i in range(3):
            cube("chest", "necklace_%d" % i, (-2.2 + i * 0.3, ch_y + 3.4 - i * 0.9, -cd / 2 - 0.5 - 0.1 * i), (4.4 - i * 0.6, 0.6, 0.5), "tooth" if i != 1 else "metal")
        cube("body", "coat_tail", (-3.6, hip - 8.0, 2.6), (7.2, 8.8, 1.0), cl, rot=(-4, 0, 0))
    elif o == "robe":                                                     # a long dark coat with a high collar
        cube("chest", "jacket", (-cw / 2 - 0.6, ch_y - 0.2, -cd / 2 - 0.5), (cw + 1.2, chh - 0.6, cd + 1.0), cl)
        cube("body", "jacket_low", (-D["aw"] / 2 - 0.5, ab_y - 0.2, -2.9), (D["aw"] + 1.0, ah + 0.2, 5.8), cl)
        cube("chest", "collar_hi", (-4.4, D["sh_y"] - 0.5, -3.0), (8.8, 3.4, 6.0), cl2)
        cube("body", "coat_skirt_f", (-pw / 2 - 1.2, 1.4, -4.0), (pw + 2.4, hip - 0.4, 2.6), cl)
        cube("body", "coat_skirt_b", (-pw / 2 - 1.2, 1.0, 2.0), (pw + 2.4, hip, 3.0), cl)
        def sleeves(sd, n):
            cube("arm_" + n, "sleeve_up_" + n, (-cw / 2 - 1.0 - D["at"] / 2 - 0.5, D["sh_y"] - 1.0 - D["ua"], -D["at"] / 2 - 0.6), (D["at"] + 1.0, D["ua"] + 0.4, D["at"] + 1.2), cl, side=sd)
            cube("arm_%s_low" % n, "sleeve_lo_" + n, (-cw / 2 - 1.0 - D["at"] / 2 - 0.4, D["sh_y"] - 1.0 - D["ua"] - D["fa"] + 0.6, -D["at"] / 2 - 0.7), (D["at"] + 0.8, D["fa"] * 0.9, D["at"] + 1.4), cl, side=sd)
        pair(sleeves)
    elif o == "long_skirt":                                               # Lucifero: a long dark skirt to the feet
        cube("body", "skirt_a", (-pw / 2 - 1.2, hip - 5.0, -4.2), (pw + 2.4, 7.0, 8.4), cl)
        cube("body", "skirt_b", (-pw / 2 - 2.4, hip - 10.4, -5.2), (pw + 4.8, 5.8, 10.4), cl)
        cube("body", "skirt_c", (-pw / 2 - 3.2, 0.4, -5.8), (pw + 6.4, hip - 10.0, 11.6), cl2)
    elif o == "fur":                                                      # Beelzebub: fur on the chest and shoulders
        cube("chest", "fur_chest", (-cw / 2 - 0.8, ch_y + 0.4, -cd / 2 - 0.9), (cw + 1.6, chh - 0.4, cd * 0.55), "fur")
        cube("chest", "fur_back", (-cw / 2 - 0.8, ch_y + 0.6, cd / 2 - 1.2), (cw + 1.6, chh - 0.6, 1.8), "fur")
        for i in range(5):
            cube("chest", "fur_tuft_%d" % i, (-cw / 2 + i * (cw - 2.2) / 4, ch_y - 1.0, -cd / 2 - 1.1), (2.2, 2.0, 1.2), "fur")

        def mantle(sd, n):
            cube("arm_" + n, "fur_sh_" + n, (-cw / 2 - 4.2, D["sh_y"] - 2.6, -4.0), (5.6, 3.6, 8.0), "fur", side=sd)
            cube("arm_" + n, "fur_sh2_" + n, (-cw / 2 - 3.2, D["sh_y"] - 0.4, -3.0), (4.2, 1.8, 6.0), "fur", side=sd)
        pair(mantle)
    elif o == "bandage":                                                  # bare with wrapped forearms
        def wrap(sd, n):
            cube("arm_%s_low" % n, "wrap_" + n, (-D["cw"] / 2 - 1.0 - D["at"] / 2 - 0.2, D["sh_y"] - 1.0 - D["ua"] - D["fa"] + 0.5, -D["at"] / 2 - 0.5), (D["at"] + 0.4, D["fa"] * 0.6, D["at"] + 1.0), "cloth", side=sd)
        pair(wrap)
    elif o == "cape":                                                     # a cloak from the shoulders
        cube("chest", "cape_top", (-cw / 2 - 0.8, ch_y + 0.6, cd / 2), (cw + 1.6, chh - 0.4, 1.2), cl)
        cube("chest", "cape_mid", (-cw / 2 - 1.2, ch_y - 6.0, cd / 2 + 0.4), (cw + 2.4, 6.4, 1.0), cl)
        cube("chest", "cape_low", (-cw / 2 - 1.8, ch_y - 14.0, cd / 2 + 0.8), (cw + 3.6, 8.2, 1.0), cl2)
    elif o == "plate":                                                    # sleek armour: breastplate, pauldrons, greaves
        cube("chest", "breastplate", (-cw / 2 - 0.6, ch_y - 0.2, -cd / 2 - 0.7), (cw + 1.2, chh - 0.5, cd * 0.5), "white")
        cube("chest", "plate_trim", (-cw / 2 - 0.7, ch_y + chh - 1.2, -cd / 2 - 0.8), (cw + 1.4, 0.9, cd * 0.5 + 0.2), "metal")
        cube("body", "tasset", (-pw / 2 - 0.6, hip - 4.8, -3.6), (pw + 1.2, 5.2, 1.2), "white")

        def pauld(sd, n):
            cube("arm_" + n, "pauldron_" + n, (-cw / 2 - 3.6, D["sh_y"] - 2.2, -3.4), (5.2, 2.6, 6.8), "white", side=sd)
            cube("arm_" + n, "pauldron_trim_" + n, (-cw / 2 - 3.8, D["sh_y"] - 2.4, -3.6), (5.6, 0.7, 7.2), "metal", side=sd)
            cube("arm_%s_low" % n, "vambrace_" + n, (-cw / 2 - 1.0 - D["at"] / 2 - 0.4, D["sh_y"] - 1.0 - D["ua"] - D["fa"] + 0.6, -D["at"] / 2 - 0.6), (D["at"] + 0.8, D["fa"] * 0.7, D["at"] + 1.2), "white", side=sd)
        pair(pauld)
    elif o == "belly":                                                    # a top and hip scarves
        cube("chest", "top", (-cw / 2 - 0.3, ch_y + 1.6, -cd / 2 - 0.5), (cw + 0.6, 2.4, cd * 0.55), cl)
        cube("body", "hip_scarf", (-pw / 2 - 1.0, hip - 3.4, -3.6), (pw + 2.0, 4.8, 7.2), cl)
        cube("body", "hip_scarf2", (-pw / 2 - 1.6, hip - 7.4, -4.0), (pw + 3.2, 4.4, 8.0), cl2)
    else:
        raise SystemExit("outfit " + o)


# ---------------------------------------------------------------- extras that can be attached to the humanoid by name
@extra
def third_eye(D, spec):
    hcube(D, "third_eye", (-0.9, 5.4, -4.7), (1.8, 1.2, 0.6), "glow")


@extra
def crown(D, spec, mat="metal", spikes=5):
    hcube(D, "crown_band", (-4.5, 6.8, -4.5), (9.0, 1.2, 9.0), mat)
    for i in range(spikes):
        x = -3.6 + i * (7.2 / max(1, spikes - 1)) - 0.5
        hcube(D, "crown_sp_%d" % i, (x, 8.0, -4.5), (1.0, 2.6 - 0.5 * abs(i - spikes // 2), 1.0), mat)


@extra
def halo(D, spec, mat="gem", r=7.0):
    """A ring behind the head (a clock or a halo): four bars around a hollow centre."""
    t = 1.2
    y0 = D["top"] + 0.6 * D["hk"]
    cube("head", "halo_top", (-r / 2, y0 + r - t + 0.6, 6.4), (r, t, 0.8), mat)
    cube("head", "halo_bot", (-r / 2, y0 + 0.6, 6.4), (r, t, 0.8), mat)
    cube("head", "halo_l", (-r / 2, y0 + 0.6, 6.4), (t, r, 0.8), mat)
    cube("head", "halo_r", (r / 2 - t, y0 + 0.6, 6.4), (t, r, 0.8), mat)
    cube("head", "halo_hand_a", (-0.4, y0 + r / 2 + 0.6, 6.5), (0.8, r / 2 - 0.8, 0.8), mat)
    cube("head", "halo_hand_b", (-0.4, y0 + r / 2 + 0.6, 6.5), (r / 2 - 0.8, 0.8, 0.8), mat)


@extra
def tentacles(D, spec, pairs=3, mat="skin", tipmat="white", y=0.55):
    """Limp tentacles from the back and the hips (Megicula), each three segments."""
    for i in range(pairs):
        yy = D["ch_y"] + D["chh"] * (0.9 - 0.6 * i / max(1, pairs - 1)) if i < pairs - 1 else D["hip"] + 1.0
        zz = 3.4

        def one(sd, n, i=i, yy=yy):
            segs = [(4.0, 1.6, 1.6, (30 + 8 * i, 0, 24 + 10 * i)), (4.0, 1.3, 1.3, (-18, 0, 8)), (3.6, 1.0, 1.0, (-14, 0, 6))]
            chain("tentacle%d" % i, "chest" if i < pairs - 1 else "body", sd, n, (-2.8, yy, zz), segs, mat, axis="ny", mats=[mat, mat, tipmat])
        pair(one)


@extra
def mushrooms(D, spec):
    """Mushroom caps growing on the shoulders and the head (Astaroth's kin): stalk and cap."""
    for i, (b, x, y, z, s) in enumerate((("chest", -7.4, D["sh_y"] - 0.2, 1.0, 1.0), ("chest", 7.0, D["sh_y"] - 0.4, -0.5, 0.8), ("chest", -3.2, D["sh_y"] - 0.2, 3.4, 0.7), ("head", 2.6, D["top"] - 0.4, 2.4, 0.9))):
        cube(b, "mush_stalk_%d" % i, (x - 0.6 * s, y, z - 0.6 * s), (1.2 * s, 2.0 * s, 1.2 * s), "tooth")
        cube(b, "mush_cap_%d" % i, (x - 2.0 * s, y + 2.0 * s, z - 2.0 * s), (4.0 * s, 1.6 * s, 4.0 * s), "rot")
        cube(b, "mush_top_%d" % i, (x - 1.2 * s, y + 3.6 * s, z - 1.2 * s), (2.4 * s, 0.8 * s, 2.4 * s), "rot")


@extra
def pauldron_spikes(D, spec, mat="horn"):
    def one(sd, n):
        cube("arm_" + n, "ps_a_" + n, (-D["cw"] / 2 - 2.8, D["sh_y"], -1.8), (1.4, 2.8, 1.4), mat, rot=(0, 0, 14), side=sd)
        cube("arm_" + n, "ps_b_" + n, (-D["cw"] / 2 - 1.8, D["sh_y"] - 0.4, 0.6), (1.2, 2.2, 1.2), mat, rot=(0, 0, 10), side=sd)
        cube("arm_%s_low" % n, "elbow_spike_" + n, (-D["cw"] / 2 - 3.6, D["sh_y"] - 1.0 - D["ua"] + 0.6, -0.6), (1.6, 1.2, 1.2), mat, rot=(0, 0, 18), side=sd)
    pair(one)


@extra
def chest_gem(D, spec):
    cube("chest", "chest_gem", (-1.0, D["ch_y"] + 2.2, -D["cd"] / 2 - 0.5), (2.0, 2.4, 0.7), "gem")


@extra
def collar_ruff(D, spec, mat="fur"):
    cube("chest", "ruff", (-5.0, D["sh_y"] - 0.8, -3.6), (10.0, 2.0, 7.2), mat)


@extra
def hole_eye(D, spec):
    pass


# ---------------------------------------------------------------- the custom bodies
def custom_fox(spec):
    """The Fox Devil: a giant fox head with many eyes on a hunched body, one monstrous arm with eyes and claws, a bushy tail."""
    bone("root", None, (0, 0, 0))
    bone("body", "root", (0, 12, 0))
    bone("chest", "body", (0, 17, 0))
    bone("head", "chest", (0, 22, -1))
    bone("jaw", "head", (0, 24, -8))
    bone("mane", "head", (0, 34, 4))
    def legs(sd, n):
        bone("leg_" + n, "root", (-3.4, 12, 0), side=sd)
        cube("leg_" + n, "thigh_" + n, (-6.0, 5.6, -3.0), (5.2, 6.8, 6.0), "fur", side=sd)
        bone("leg_%s_low" % n, "leg_" + n, (-3.4, 6.0, 1.0), side=sd)
        cube("leg_%s_low" % n, "shin_" + n, (-5.4, 1.4, -1.0), (4.0, 5.2, 4.0), "fur", rot=(18, 0, 0), pv=(-3.4, 6.0, 1.0), side=sd)
        bone("leg_%s_foot" % n, "leg_%s_low" % n, (-3.4, 1.6, 0), side=sd)
        cube("leg_%s_foot" % n, "paw_" + n, (-5.8, 0, -5.0), (4.8, 1.8, 7.0), "white", side=sd)
        for i in range(3):
            cube("leg_%s_foot" % n, "toe_%d_%s" % (i, n), (-5.6 + i * 1.5, 0, -6.2), (1.0, 1.0, 1.4), "claw", side=sd)
    pair(legs)
    cube("body", "pelvis", (-5.0, 9.8, -3.2), (10.0, 4.6, 6.4), "fur")
    cube("body", "belly", (-4.4, 13.6, -3.0), (8.8, 4.4, 5.6), "white")
    cube("chest", "chest", (-7.0, 17.0, -4.4), (14.0, 7.0, 8.4), "fur", paint="chest")
    cube("chest", "chest_ruff", (-6.0, 18.0, -5.4), (12.0, 5.2, 2.0), "white")
    cube("chest", "back_ridge", (-3.0, 22.4, 3.6), (6.0, 2.4, 1.6), "fur2")
    # the head: huge, with a long muzzle, many eyes
    cube("head", "head", (-6.0, 22.0, -6.0), (12.0, 11.0, 10.0), "fur", paint="face")
    cube("head", "brow", (-6.4, 30.4, -6.4), (12.8, 1.6, 2.0), "fur2")
    cube("head", "muzzle", (-3.0, 22.4, -12.0), (6.0, 5.0, 6.4), "white")
    cube("head", "snout_top", (-2.6, 27.0, -11.4), (5.2, 1.8, 5.6), "fur")
    cube("head", "nose", (-1.3, 26.0, -12.8), (2.6, 2.0, 1.2), "ink")
    cube("head", "cheek_fur_r", (-8.0, 22.4, -4.0), (2.4, 5.0, 5.0), "white", rot=(0, 0, 14), pv=(-6, 24, -2))
    cube("head", "cheek_fur_l", (5.6, 22.4, -4.0), (2.4, 5.0, 5.0), "white", rot=(0, 0, -14), pv=(6, 24, -2))
    cube("jaw", "jaw", (-2.8, 20.6, -11.0), (5.6, 2.0, 9.0), "white", paint="jaw")
    cube("jaw", "fang_r", (-2.6, 22.5, -11.0), (0.9, 1.6, 0.9), "tooth")
    cube("jaw", "fang_l", (1.7, 22.5, -11.0), (0.9, 1.6, 0.9), "tooth")
    for sd, n in ((-1, "r"), (1, "l")):
        cube("head", "ear_a_" + n, (-6.0, 32.4, -2.0), (4.0, 4.0, 2.0), "fur", rot=(0, 0, 14), pv=(-4, 32.4, -1), side=sd)
        cube("head", "ear_b_" + n, (-5.4, 36.0, -1.6), (2.8, 3.4, 1.6), "fur", rot=(0, 0, 18), pv=(-4, 36, -1), side=sd)
        cube("head", "ear_in_" + n, (-5.0, 33.0, -2.2), (2.0, 3.8, 0.6), "ink", rot=(0, 0, 14), pv=(-4, 33, -1.9), side=sd)
    hair_cube = [("tuft_%d" % i, (-2.6 + i * 2.6, 32.8, -5.6), (2.0, 2.0 + (i % 2), 1.6)) for i in range(3)]
    for nm, o, s in hair_cube:
        cube("head", nm, o, s, "fur2")
    # the arms: the right one is the monstrous arm covered in eyes
    def arm_small(sd, n):
        bone("arm_" + n, "chest", (-8.4, 23, 0), side=sd)
        cube("arm_" + n, "upper_" + n, (-10.8, 17, -2.2), (3.8, 6.4, 4.4), "fur", side=sd)
        bone("arm_%s_low" % n, "arm_" + n, (-8.8, 17.4, 0), side=sd)
        cube("arm_%s_low" % n, "fore_" + n, (-10.8, 11, -2.2), (3.8, 6.4, 4.4), "fur", side=sd)
        bone("arm_%s_hand" % n, "arm_%s_low" % n, (-8.8, 11.4, 0), side=sd)
        cube("arm_%s_hand" % n, "palm_" + n, (-10.6, 8.2, -2.0), (3.6, 3.2, 4.0), "white", side=sd)
        for i in range(3):
            cube("arm_%s_hand" % n, "claw_%d_%s" % (i, n), (-10.4 + i * 1.1, 5.4, -1.8 + i * 1.3), (0.8, 2.8, 0.9), "claw", side=sd)
    arm_small(1, "l")
    bone("arm_r", "chest", (-9.4, 24, 0))                                     # the huge right arm
    cube("arm_r", "upper_r", (-15.6, 16.0, -4.4), (8.0, 9.0, 8.8), "fur", paint="eyes_arm")
    bone("arm_r_low", "arm_r", (-12.0, 16.6, 0))
    cube("arm_r_low", "fore_r", (-16.4, 7.0, -5.2), (9.6, 10.4, 10.4), "fur", paint="eyes_arm")
    bone("arm_r_hand", "arm_r_low", (-12.0, 7.6, 0))
    cube("arm_r_hand", "palm_r", (-16.0, 4.0, -5.0), (8.8, 4.0, 10.0), "white", paint="eyes_arm")
    for i in range(4):
        cube("arm_r_hand", "claw_%d_r" % i, (-15.6 + i * 2.2, 0.0, -4.4 + i * 2.5), (1.8, 4.4, 1.8), "claw")
    # the tail: three bushy segments, white tip
    segs = [(6.0, 5.0, 5.0, (-14, 0, 0)), (6.4, 6.4, 6.4, (10, 0, 0)), (6.0, 6.4, 6.4, (-12, 0, 0))]
    chain("tail", "body", -1, "", (0, 12.0, 3.0), segs, "fur", axis="z", mats=["fur", "fur", "white"])
    return dict(hip=12, top=37)


def custom_curse(spec):
    """The Curse Devil: a large two-headed skeleton with horns, a distended ribcage, clawed bony hands."""
    bone("root", None, (0, 0, 0))
    bone("body", "root", (0, 14, 0))
    bone("chest", "body", (0, 19, 0))
    bone("head", "chest", (3.4, 27, 0))
    bone("jaw", "head", (3.4, 28, -3))
    bone("head2", "chest", (-3.6, 27, 0))
    bone("jaw2", "head2", (-3.6, 28, -3))
    def legs(sd, n):
        bone("leg_" + n, "root", (-3.2, 14, 0), side=sd)
        cube("leg_" + n, "femur_" + n, (-4.4, 7.4, -1.2), (2.4, 6.8, 2.4), "bone", side=sd)
        cube("leg_" + n, "thigh_flesh_" + n, (-5.0, 9.0, -1.8), (3.6, 4.0, 3.6), "cloth", side=sd)
        bone("leg_%s_low" % n, "leg_" + n, (-3.2, 7.4, 0), side=sd)
        cube("leg_%s_low" % n, "tibia_" + n, (-4.2, 1.4, -1.0), (2.0, 6.4, 2.0), "bone", rot=(8, 0, 0), pv=(-3.2, 7.4, 0), side=sd)
        bone("leg_%s_foot" % n, "leg_%s_low" % n, (-3.2, 1.4, 0), side=sd)
        cube("leg_%s_foot" % n, "foot_" + n, (-4.8, 0, -4.4), (3.2, 1.6, 6.4), "bone", side=sd)
        for i in range(3):
            cube("leg_%s_foot" % n, "toe_%d_%s" % (i, n), (-4.8 + i * 1.1, 0, -5.8), (0.9, 1.0, 1.6), "claw", side=sd)
    pair(legs)
    cube("body", "pelvis_bone", (-5.2, 12.0, -2.4), (10.4, 3.4, 5.0), "bone")
    cube("body", "spine_l", (-1.0, 15.4, 1.0), (2.0, 4.8, 2.0), "bone")
    cube("chest", "spine_u", (-1.0, 19.0, 1.2), (2.0, 7.0, 2.0), "bone")
    cube("chest", "sternum", (-1.2, 20.4, -5.6), (2.4, 6.4, 1.2), "bone")
    for i in range(5):                                                        # ribs: wide and distended, pairs that bow out and round
        y = 20.0 + i * 1.5
        wd = 7.0 + (2.0 if 1 <= i <= 3 else 0.0) - (1.0 if i == 4 else 0.0)
        def rib(sd, n, i=i, y=y, wd=wd):
            cube("chest", "rib_%d_%s" % (i, n), (-wd - 0.6, y, -4.8), (wd - 0.6, 0.9, 1.2), "bone", side=sd)
            cube("chest", "rib_side_%d_%s" % (i, n), (-wd - 0.6, y, -4.8), (1.2, 0.9, 8.6), "bone", side=sd)
            cube("chest", "rib_back_%d_%s" % (i, n), (-wd - 0.6, y, 3.0), (wd - 0.6, 0.9, 1.0), "bone", side=sd)
        pair(rib)
    cube("chest", "heart_glow", (-1.5, 21.0, -1.0), (3.0, 3.0, 3.0), "glow")
    cube("chest", "clavicle", (-7.4, 26.0, -1.6), (14.8, 1.2, 2.4), "bone")
    # the right head: nearly human skull; the left: three eye slits, long mandible teeth
    for hd, jw, cx, nm in (("head", "jaw", 3.4, "a"), ("head2", "jaw2", -3.6, "b")):
        big = 6.6 if nm == "a" else 7.4
        cube(hd, "skull_" + nm, (cx - big / 2, 27.0, -big / 2), (big, big, big), "bone", paint="face_" + nm)
        cube(jw, "jaw_" + nm, (cx - (big - 1.6) / 2, 25.6, -big / 2 - 0.2), (big - 1.6, 1.8 + (1.4 if nm == "b" else 0.0), big - 2.0 + (1.4 if nm == "b" else 0.0)), "bone")
        for i in range(4 if nm == "b" else 3):
            cube(jw, "tooth_%s_%d" % (nm, i), (cx - 2.4 + i * (4.8 / (3 if nm == "b" else 2)), 27.2 - (1.0 if nm == "b" else 0.0), -big / 2 - 0.2), (0.7, 1.6 + (0.8 if nm == "b" else 0), 0.7), "tooth")
        if nm == "a":
            for sd2, n2 in ((-1, "r"), (1, "l")):
                cube(hd, "horn_a_%s" % n2, (cx - 2.2 if sd2 < 0 else cx + 1.2, 33.4, -0.8), (1.2, 2.8, 1.2), "horn", rot=(0, 0, 10 if sd2 < 0 else -10))
        else:
            for sd2, n2 in ((-1, "r"), (1, "l")):
                cube(hd, "horn_b_%s" % n2, (cx - 3.2 if sd2 < 0 else cx + 2.0, 33.8, -0.8), (1.2, 4.2, 1.2), "horn", rot=(0, 0, 18 if sd2 < 0 else -18))
    cube("chest", "neck_a", (2.4, 25.0, -0.6), (2.0, 3.0, 2.0), "bone")
    cube("chest", "neck_b", (-4.4, 25.0, -0.6), (2.0, 3.0, 2.0), "bone")
    def arms(sd, n):
        bone("arm_" + n, "chest", (-8.2, 25.4, 0), side=sd)
        cube("arm_" + n, "humerus_" + n, (-9.4, 18.6, -1.0), (2.2, 7.4, 2.2), "bone", side=sd)
        cube("arm_" + n, "shoulder_" + n, (-10.6, 24.0, -2.0), (4.0, 2.4, 4.0), "bone", side=sd)
        bone("arm_%s_low" % n, "arm_" + n, (-8.4, 19.0, 0), side=sd)
        cube("arm_%s_low" % n, "radius_" + n, (-9.4, 11.6, -1.0), (2.0, 7.6, 2.0), "bone", side=sd)
        bone("arm_%s_hand" % n, "arm_%s_low" % n, (-8.4, 11.8, 0), side=sd)
        cube("arm_%s_hand" % n, "palm_" + n, (-9.8, 9.0, -1.6), (3.0, 2.8, 3.2), "bone", side=sd)
        for i in range(4):
            cube("arm_%s_hand" % n, "finger_%d_%s" % (i, n), (-9.8 + i * 0.8, 3.8, -1.6 + i * 1.0), (0.7, 5.2, 0.8), "claw", side=sd)
    pair(arms)
    # a tattered shroud and a long tail of vertebrae
    cube("body", "rag_f", (-3.0, 5.0, -3.2), (6.0, 8.6, 0.8), "cloth")
    cube("body", "rag_b", (-3.4, 4.0, 2.4), (6.8, 9.6, 0.8), "cloth")
    segs = [(5.0, 2.2, 2.2, (-8, 0, 0)), (5.0, 1.8, 1.8, (8, 0, 0)), (5.0, 1.4, 1.4, (-14, 0, 0))]
    chain("tail", "body", -1, "", (0, 13.4, 2.4), segs, "bone", axis="z")
    return dict(hip=14, top=38)


def custom_tree(spec):
    """The Future Devil: a trunk with roots for legs, a humanoid upper body with outstretched arms and a horned head with six eyes, a hole in the chest with a huge eye, vine streamers."""
    bone("root", None, (0, 0, 0))
    bone("body", "root", (0, 12, 0))
    bone("chest", "body", (0, 17, 0))
    bone("head", "chest", (0, 25, 0))
    bone("jaw", "head", (0, 26.8, -2.4))
    bone("mane", "head", (0, 33, 3))
    # the trunk: flared roots, a wood trunk with bark rings and branches
    for i, (x, z, a) in enumerate(((-6.4, -2.0, 24), (6.4, -2.0, -24), (-5.0, 5.0, 14), (5.0, 5.0, -14), (0.0, -6.0, 0))):
        nm = ("leg_r" if x < 0 else "leg_l") if i < 2 else "root_%d" % i
        sd = -1 if x <= 0 else 1
        bone(nm, "root", (-abs(x) * 0.5, 7, z * 0.5), side=sd)
        cube(nm, nm + "_c", (-abs(x) - 1.6, 0.0, z - 1.8), (3.6, 7.6, 3.6), "wood", rot=(0, 0, a if sd < 0 else -a), pv=(-abs(x) * 0.6, 7, z), side=sd) if i < 4 else cube(nm, nm + "_c", (-1.8, 0.0, z - 1.8), (3.6, 7.6, 3.6), "wood", rot=(-24, 0, 0), pv=(0, 7, z))
    cube("body", "trunk_low", (-5.4, 7.0, -5.0), (10.8, 6.0, 10.0), "wood")
    cube("body", "trunk_mid", (-4.6, 12.0, -4.4), (9.2, 4.6, 8.8), "wood")
    cube("body", "bark_ring", (-5.0, 11.4, -4.8), (10.0, 0.9, 9.6), "wood2")
    cube("body", "knot", (-1.6, 9.6, -5.5), (3.2, 3.0, 0.8), "wood2")
    cube("chest", "chest", (-6.0, 16.6, -3.8), (12.0, 7.8, 7.6), "wood", paint="chest")
    cube("chest", "hole_rim", (-3.4, 18.0, -4.4), (6.8, 5.0, 0.8), "wood2")
    cube("chest", "hole_eye", (-2.4, 18.8, -4.6), (4.8, 3.6, 0.5), "glow", paint="big_eye")
    cube("chest", "neck", (-1.8, 24.0, -1.6), (3.6, 1.8, 3.2), "wood")
    cube("head", "head", (-4.0, 25.0, -4.0), (8.0, 9.0, 8.0), "wood", paint="face")
    cube("jaw", "jaw", (-3.4, 24.6, -4.3), (6.8, 2.0, 4.7), "wood", paint="jaw")
    cube("head", "brow", (-4.4, 31.4, -4.5), (8.8, 1.2, 1.5), "wood2")
    for sd, n in ((-1, "r"), (1, "l")):
        chain("horn", "head", sd, n, (-2.4, 34.0, -0.6), [(4.0, 2.4, 2.4, (0, 0, 20)), (4.2, 1.8, 1.8, (-10, 0, 20)), (3.6, 1.2, 1.2, (-14, 0, 16)), (2.4, 0.8, 0.8, (-16, 0, 14))], "wood2", axis="y")
        chain("branch", "head", sd, n, (-3.8, 31.0, 2.0), [(4.0, 1.4, 1.4, (10, 0, 70)), (3.6, 1.0, 1.0, (0, 0, 24))], "wood", axis="y")
    def arms(sd, n):
        bone("arm_" + n, "chest", (-6.4, 23.0, 0), rot=(0, 0, 0), side=sd)
        cube("arm_" + n, "upper_" + n, (-12.8, 21.4, -1.8), (6.6, 3.2, 3.6), "wood", side=sd)
        bone("arm_%s_low" % n, "arm_" + n, (-12.8, 22.4, 0), side=sd)
        cube("arm_%s_low" % n, "fore_" + n, (-19.4, 21.0, -1.6), (6.8, 2.8, 3.2), "wood", side=sd)
        bone("arm_%s_hand" % n, "arm_%s_low" % n, (-19.4, 22.2, 0), side=sd)
        cube("arm_%s_hand" % n, "palm_" + n, (-22.6, 20.8, -1.6), (3.4, 2.6, 3.2), "wood2", side=sd)
        for i in range(4):
            cube("arm_%s_hand" % n, "twig_%d_%s" % (i, n), (-26.0 + i * 0.2, 21.2 + i * 0.5 - 0.6, -1.8 + i * 1.1), (3.8, 0.6, 0.7), "wood2", side=sd)
        chain("vine_a", "arm_%s_low" % n, sd, n, (-14.0, 21.0, 0.6), [(5.0, 1.0, 0.4, (0, 0, 0)), (5.0, 0.9, 0.4, (6, 0, 0)), (5.0, 0.8, 0.4, (8, 0, 0))], "leaf", axis="ny")
        chain("vine_b", "arm_%s_low" % n, sd, n, (-17.0, 21.0, -0.6), [(4.0, 1.0, 0.4, (0, 0, 0)), (4.0, 0.9, 0.4, (-6, 0, 0)), (4.0, 0.8, 0.4, (-8, 0, 0))], "leaf", axis="ny")
    pair(arms)
    for i, x in enumerate((-4.0, 0.0, 4.0)):                                     # streamers hanging from the chest
        chain("stream", "chest", -1, str(i), (x, 17.0, -4.0), [(4.0, 1.4, 0.4, (0, 0, 0)), (4.0, 1.2, 0.4, (-6, 0, 0)), (4.0, 1.0, 0.4, (-10, 0, 0))], "leaf", axis="ny")
    return dict(hip=12, top=39)


def custom_flower(spec):
    """The Ghost Devil: a large cylindrical body covered in white flowers, many arms instead of legs, an old, desiccated head with long dark hair and sewn-shut eyes and mouth."""
    bone("root", None, (0, 0, 0))
    bone("body", "root", (0, 12, 0))
    bone("chest", "body", (0, 18, 0))
    bone("head", "chest", (0, 28, 0))
    bone("jaw", "head", (0, 29, -2))
    bone("mane", "head", (0, 36, 4))
    cube("body", "belly", (-6.8, 8.0, -6.8), (13.6, 10.4, 13.6), "skin", paint="stitch")
    cube("chest", "torso", (-6.4, 17.6, -6.4), (12.8, 9.6, 12.8), "skin")
    cube("chest", "neck", (-2.0, 26.6, -2.0), (4.0, 2.4, 4.0), "skin")
    k = 0
    for (b, y, ring, h) in (("body", 8.4, 7.6, 9.6), ("chest", 18.0, 7.2, 8.4)):          # flowers: a cross of petals and a centre, on the four sides, in rows
        for row in range(2 if b == "body" else 2):
            yy = y + row * h * 0.5
            for si, (dx, dz, rx) in enumerate(((0, -1, 0), (0, 1, 0), (-1, 0, 1), (1, 0, 1))):
                for col in (-2.6, 2.6):
                    cx = (ring + 0.4) * dx + (col if dx == 0 else 0)
                    cz = (ring + 0.4) * dz + (col if dz == 0 else 0)
                    sx, sz = (4.2, 1.0) if dz else (1.0, 4.2)
                    cube(b, "petal_%d" % k, (cx - sx / 2, yy - 2.1 + 2.0, cz - sz / 2), (sx, 4.2, sz), "flower")
                    cube(b, "petal2_%d" % k, (cx - (sz if dz else 4.2) / 2 + (0.0), yy - 0.6, cz - (4.2 if dz else sz) / 2 + 0.0), (4.2 if dz else 1.0, 1.0, 1.0 if dz else 4.2), "flower")
                    cube(b, "heart_%d" % k, (cx - 0.8 + (0.4 * dx), yy + 1.0, cz - 0.8 + (0.4 * dz)), (1.6, 1.6, 1.6), "flower2")
                    k += 1
    hair_len = 18
    cube("head", "head", (-4.4, 27.0, -4.4), (8.8, 9.0, 8.8), "skin", paint="stitched_face")
    cube("head", "sunken_r", (-4.6, 28.4, -2.6), (0.6, 3.0, 4.0), "skin2")
    cube("head", "sunken_l", (4.0, 28.4, -2.6), (0.6, 3.0, 4.0), "skin2")
    cube("jaw", "jaw", (-3.6, 26.4, -4.6), (7.2, 2.0, 5.2), "skin", paint="jaw")
    cube("head", "hair_top", (-4.9, 35.0, -4.9), (9.8, 2.4, 9.8), "hair")
    cube("mane", "hair_back", (-5.0, 36.0 - hair_len, 4.2), (10.0, hair_len, 1.8), "hair")
    def locks(sd, n):
        bone("lock_" + n, "head", (-4.6, 34.0, 0), side=sd)
        cube("lock_" + n, "hair_side_a_" + n, (-6.2, 22.0, -2.8), (1.8, 13.0, 5.6), "hair", side=sd)
        cube("lock_" + n, "hair_side_b_" + n, (-6.6, 14.0, -2.4), (1.4, 8.4, 4.8), "hair", side=sd)
    pair(locks)
    def arms(sd, n):                                                           # two upper arms, long and thin
        bone("arm_" + n, "chest", (-8.4, 25.0, 0), side=sd)
        cube("arm_" + n, "upper_" + n, (-10.4, 17.4, -1.8), (3.2, 8.4, 3.6), "skin", side=sd)
        bone("arm_%s_low" % n, "arm_" + n, (-8.8, 18.0, 0), side=sd)
        cube("arm_%s_low" % n, "fore_" + n, (-10.4, 9.6, -1.6), (3.0, 8.8, 3.2), "skin", side=sd)
        bone("arm_%s_hand" % n, "arm_%s_low" % n, (-8.8, 10.0, 0), side=sd)
        cube("arm_%s_hand" % n, "palm_" + n, (-10.4, 6.6, -1.6), (3.0, 3.4, 3.4), "skin", side=sd)
        for i in range(4):
            cube("arm_%s_hand" % n, "finger_%d_%s" % (i, n), (-10.4 + i * 0.7, 2.0, -1.6 + i * 1.0), (0.6, 4.8, 0.8), "skin2", side=sd)
    pair(arms)
    # the "legs": arms. Two named leg_r / leg_l walk the body, four more hang around the base and sway.
    def legs(sd, n):
        bone("leg_" + n, "root", (-3.6, 12, 0), side=sd)
        cube("leg_" + n, "larm_up_" + n, (-5.0, 5.0, -1.6), (3.0, 7.4, 3.2), "skin", side=sd)
        bone("leg_%s_low" % n, "leg_" + n, (-3.6, 5.4, 0.4), side=sd)
        cube("leg_%s_low" % n, "larm_lo_" + n, (-4.9, 0.8, -1.4), (2.8, 5.2, 2.8), "skin", rot=(10, 0, 0), pv=(-3.6, 5.4, 0.4), side=sd)
        bone("leg_%s_foot" % n, "leg_%s_low" % n, (-3.6, 1.2, 0), side=sd)
        cube("leg_%s_foot" % n, "lhand_" + n, (-5.2, 0, -2.6), (3.6, 1.4, 4.4), "skin2", side=sd)
        for i in range(3):
            cube("leg_%s_foot" % n, "lfinger_%d_%s" % (i, n), (-5.2 + i * 1.2, 0, -4.4), (0.7, 0.8, 2.0), "skin2", side=sd)
    pair(legs)
    for i, (x, z, a) in enumerate(((-6.0, 4.0, 20), (6.0, 4.0, -20), (-3.0, 6.0, 8), (3.0, 6.0, -8))):
        sd = -1 if x < 0 else 1
        chain("limb%d" % i, "body", sd, "", (-abs(x), 10.0, z), [(5.0, 3.0, 3.0, (14, 0, a * (1 if sd < 0 else 1))), (5.0, 2.6, 2.6, (-20, 0, 0)), (3.6, 2.2, 2.4, (-12, 0, 0))], "skin", axis="ny", mats=["skin", "skin", "skin2"])
    return dict(hip=12, top=39)


def custom_statue(spec):
    """The Hall of Hell demon (the Twelfth Demon, Asura's statue): three heads, six arms, a thick stone body on a lotus base."""
    bone("root", None, (0, 0, 0))
    bone("body", "root", (0, 12, 0))
    bone("chest", "body", (0, 17, 0))
    bone("head", "chest", (0, 25, 0))
    bone("jaw", "head", (0, 26.6, -2.6))
    bone("mane", "head", (0, 33, 3))
    bone("head_r", "chest", (-5.2, 25, 0), rot=(0, 28, 0))
    bone("head_l", "chest", (5.2, 25, 0), rot=(0, -28, 0))
    def legs(sd, n):
        bone("leg_" + n, "root", (-3.4, 12, 0), side=sd)
        cube("leg_" + n, "thigh_" + n, (-6.0, 5.0, -3.2), (5.4, 8.0, 6.4), "stone", side=sd)
        bone("leg_%s_low" % n, "leg_" + n, (-3.4, 5.4, 0), side=sd)
        cube("leg_%s_low" % n, "shin_" + n, (-5.8, 1.4, -2.6), (4.8, 4.4, 5.2), "stone", side=sd)
        bone("leg_%s_foot" % n, "leg_%s_low" % n, (-3.4, 1.4, 0), side=sd)
        cube("leg_%s_foot" % n, "foot_" + n, (-6.0, 0.0, -4.4), (5.2, 1.8, 7.4), "stone2", side=sd)
    pair(legs)
    cube("body", "dhoti", (-6.2, 6.6, -4.4), (12.4, 7.8, 8.8), "cloth")
    cube("body", "dhoti_fold", (-2.0, 3.0, -5.0), (4.0, 7.0, 1.0), "cloth2")
    cube("body", "belt", (-6.4, 13.2, -4.6), (12.8, 1.8, 9.2), "accent")
    cube("body", "belt_gem", (-1.2, 13.0, -5.2), (2.4, 2.2, 0.8), "gem")
    cube("body", "abdomen", (-5.4, 14.6, -3.6), (10.8, 4.2, 7.2), "stone", paint="abs")
    cube("chest", "chest", (-8.0, 18.2, -4.4), (16.0, 8.0, 8.8), "stone", paint="chest")
    cube("chest", "collar_beads", (-5.0, 25.0, -4.0), (10.0, 1.6, 8.0), "accent")
    cube("chest", "neck", (-2.4, 25.4, -2.0), (4.8, 1.6, 4.0), "stone")
    cube("head", "halo_disc", (-9.0, 24.0, 5.2), (18.0, 18.0, 1.2), "flame")
    cube("head", "halo_core", (-6.4, 26.6, 5.6), (12.8, 12.8, 1.0), "stone2")
    for hd, ang, pre in (("head", 0, "c"), ("head_r", 0, "r"), ("head_l", 0, "l")):
        cx = 0.0 if hd == "head" else (-5.2 if hd == "head_r" else 5.2)
        sc = 1.0 if hd == "head" else 0.86
        cube(hd, "face_" + pre, (cx - 4.0 * sc, 25.6, -4.0 * sc), (8.0 * sc, 8.0 * sc, 8.0 * sc), "stone", paint="face")
        cube(hd, "crown_" + pre, (cx - 4.6 * sc, 33.4 * sc + (25.6 * (1 - sc)), -4.6 * sc), (9.2 * sc, 1.4, 9.2 * sc), "accent")
        for i in range(3):
            cube(hd, "crown_sp_%s_%d" % (pre, i), (cx - 3.0 * sc + i * 3.0 * sc, 34.8 * sc + (25.6 * (1 - sc)), -0.5), (1.2, 2.8, 1.2), "accent")
        cube(hd, "brow_" + pre, (cx - 4.2 * sc, 31.0 * sc + 25.6 * (1 - sc) - 0.2, -4.4 * sc), (8.4 * sc, 1.2, 1.4), "stone2")
        for sd2, nn in ((-1, "r"), (1, "l")):
            cube(hd, "horn_%s_%s" % (pre, nn), (cx - 4.9 * sc if sd2 < 0 else cx + 3.7 * sc, 31.6, -0.8), (1.2, 3.6, 1.4), "horn", rot=(0, 0, 18 if sd2 < 0 else -18))
        cube(hd, "third_eye_" + pre, (cx - 0.7, 30.2 * sc + 25.6 * (1 - sc) - 0.6, -4.5 * sc), (1.4, 1.0, 0.6), "glow")
    cube("jaw", "jaw", (-3.0, 25.2, -4.2), (6.0, 1.8, 4.4), "stone", paint="jaw")
    cube("jaw", "fang_r", (-2.6, 26.8, -4.0), (0.8, 1.2, 0.8), "tooth")
    cube("jaw", "fang_l", (1.8, 26.8, -4.0), (0.8, 1.2, 0.8), "tooth")
    arm_defs = (("", 24.2, 0, 18, -0), ("2", 22.0, 12, 0, 0), ("3", 19.6, 24, -16, 0))
    for suf, y, zr, xr, _ in arm_defs:
        def arms(sd, n, suf=suf, y=y, zr=zr, xr=xr):
            nm = "arm%s_%s" % (suf, n)
            ax = 9.4 + 0.0
            bone(nm, "chest", (-ax, y, 0), rot=(xr, 0, zr), side=sd)
            cube(nm, "upper%s_%s" % (suf, n), (-ax - 2.4, y - 6.4, -2.4), (4.8, 6.8, 4.8), "stone", side=sd)
            cube(nm, "bangle%s_%s" % (suf, n), (-ax - 2.7, y - 4.4, -2.7), (5.4, 1.2, 5.4), "accent", side=sd)
            bone(nm + "_low", nm, (-ax, y - 6.2, 0), side=sd)
            cube(nm + "_low", "fore%s_%s" % (suf, n), (-ax - 2.1, y - 12.2, -2.1), (4.2, 6.4, 4.2), "stone", side=sd)
            bone(nm + "_hand", nm + "_low", (-ax, y - 12.0, 0), side=sd)
            cube(nm + "_hand", "palm%s_%s" % (suf, n), (-ax - 2.1, y - 15.0, -2.0), (4.2, 3.0, 4.0), "stone2", side=sd)
            for i in range(3):
                cube(nm + "_hand", "finger%s_%d_%s" % (suf, i, n), (-ax - 2.1 + i * 1.5, y - 18.0, -1.8), (1.0, 3.2, 1.2), "stone2", side=sd)
        pair(arms)
    cube("body", "lotus_a", (-9.0, 0.0, -9.0), (18.0, 1.6, 18.0), "stone2")
    cube("body", "lotus_b", (-7.4, 1.6, -7.4), (14.8, 1.4, 14.8), "accent")
    return dict(hip=12, top=39)


CUSTOM = {"fox": custom_fox, "curse": custom_curse, "tree": custom_tree, "flower": custom_flower, "statue": custom_statue}
