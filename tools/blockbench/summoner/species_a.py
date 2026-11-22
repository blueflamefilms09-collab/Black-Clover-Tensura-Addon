"""Summon models, part 1: the beasts (Hayate, Cerorin), the stone summons (Rocanel, Mira). Each build_* resets the shared lists, writes bones and cubes, returns (palette, clips, top, width)."""
import math

from summon_lib import C, Cx, Clips, S, bone, pair, reset, has, tl


# ---------------------------------------------------------------- shared dog parts
def dog_head(b, nm, cx, cy, cz, hs, mat, snout_mat, eye="round", parent="body", jaw=True, ears=True, ear_mat=None, tooth="tooth"):
    """A wolf head whose back is at z = cz, looking toward -z. cy is the bottom of the skull."""
    bone(b, parent, (cx, cy + hs * 0.4, cz))
    C(b, nm + "_skull", cx, cy, cz - hs, hs, hs, hs, mat, paint=eye)
    sn = hs * 0.62
    C(b, nm + "_snout", cx, cy + 0.5, cz - hs - sn, hs * 0.52, hs * 0.42, sn, snout_mat)
    C(b, nm + "_nose", cx, cy + hs * 0.3, cz - hs - sn - 0.8, hs * 0.2, hs * 0.18, 0.9, "dark")
    if jaw:
        bone(b + "_jaw", b, (cx, cy + 1.2, cz - hs * 0.8))
        C(b + "_jaw", nm + "_jaw", cx, cy - 0.8, cz - hs - sn + 0.6, hs * 0.44, 1.8, sn + hs * 0.3, snout_mat)
        C(b + "_jaw", nm + "_tooth_a", cx - hs * 0.16, cy + 0.9, cz - hs - sn + 0.8, 0.9, 1.1, 0.9, tooth)
        C(b + "_jaw", nm + "_tooth_b", cx + hs * 0.16, cy + 0.9, cz - hs - sn + 0.8, 0.9, 1.1, 0.9, tooth)
    if ears:
        for sx in (-1, 1):
            C(b, nm + "_ear" + ("r" if sx < 0 else "l"), cx + sx * hs * 0.32, cy + hs - 0.3, cz - hs * 0.55, hs * 0.26, hs * 0.42, hs * 0.2, ear_mat or mat, rot=(0, 0, sx * 8))


def leg_pair(L, lt, lx, z, mat, paw_mat, prefix, swing=True, parent="body", hip_y=None):
    hy = hip_y if hip_y is not None else L

    def f(sd, n):
        bone("%s_%s" % (prefix, n), parent, (-lx, hy, z), side=sd)
        T_cube = Cx
        T_cube("%s_%s" % (prefix, n), "%s_up_%s" % (prefix, n), -lx - lt / 2, L * 0.45, z - lt / 2, lt, hy - L * 0.45, lt, mat, side=sd)
        T_cube("%s_%s" % (prefix, n), "%s_low_%s" % (prefix, n), -lx - lt / 2 + 0.3, 1.5, z - lt / 2 + 0.2, lt - 0.6, L * 0.45 - 1.5, lt - 0.4, mat, side=sd)
        T_cube("%s_%s" % (prefix, n), "%s_paw_%s" % (prefix, n), -lx - lt / 2 - 0.3, 0, z - lt / 2 - 1.4, lt + 0.6, 2.0, lt + 1.8, paw_mat, side=sd)
        for i in range(3):
            T_cube("%s_%s" % (prefix, n), "%s_claw%d_%s" % (prefix, i, n), -lx - lt / 2 - 0.1 + i * (lt / 3.0), 0, z - lt / 2 - 2.3, lt / 3.0 - 0.2, 1.0, 1.0, "claw", side=sd)
    pair(f)


def quad_clips(prefix_f="lf", prefix_b="lb", head="head", tail=("tail1", "tail2", "tail3"), amp=30):
    c = Clips()
    P = 3.2
    for b in ("body",):
        c.pos("idle", b, lambda t: (0, 0.35 * math.sin(2 * math.pi * t / P), 0))
    c.rot("idle", head, lambda t: (2 * math.sin(2 * math.pi * t / P + 1), 5 * math.sin(2 * math.pi * t / P), 0))
    for i, tn in enumerate(tail):
        c.rot("idle", tn, lambda t, i=i: (0, 6 * (i + 1) * math.sin(2 * math.pi * t / P - i * 0.7), 0))
    W = 1.0
    w = lambda t, ph=0.0: math.sin(2 * math.pi * t / W + ph)
    for pre, ph in ((prefix_f, 0.0), (prefix_b, math.pi)):
        c.rot("walk", pre + "_r", lambda t, ph=ph: (amp * w(t, ph), 0, 0))
        c.rot("walk", pre + "_l", lambda t, ph=ph: (amp * w(t, ph + math.pi), 0, 0))
    c.pos("walk", "body", lambda t: (0, 0.5 * abs(w(t)) - 0.2, 0))
    c.rot("walk", head, lambda t: (3, 3 * w(t, 1.2), 0))
    for i, tn in enumerate(tail):
        c.rot("walk", tn, lambda t, i=i: (-6, 10 * (i + 1) * w(t, 0.8 - i * 0.6), 0))
    c.rot("attack", "body", rows=[(0, (0, 0, 0)), (0.15, (-8, 0, 0)), (0.32, (10, 0, 0)), (0.6, (0, 0, 0))])
    c.pos("attack", "body", rows=[(0, (0, 0, 0)), (0.15, (0, -0.5, 1.2)), (0.32, (0, -0.3, -3.0)), (0.6, (0, 0, 0))])
    c.rot("attack", head, rows=[(0, (0, 0, 0)), (0.15, (-14, 0, 0)), (0.32, (18, 0, 0)), (0.6, (0, 0, 0))])
    c.rot("attack", head + "_jaw", rows=[(0, (0, 0, 0)), (0.15, (30, 0, 0)), (0.32, (4, 0, 0)), (0.6, (0, 0, 0))])
    for pre in (prefix_f,):
        c.rot("attack", pre + "_r", rows=[(0, (0, 0, 0)), (0.15, (30, 0, 0)), (0.32, (-45, 0, 0)), (0.6, (0, 0, 0))])
        c.rot("attack", pre + "_l", rows=[(0, (0, 0, 0)), (0.15, (24, 0, 0)), (0.32, (-38, 0, 0)), (0.6, (0, 0, 0))])
    c.rot("ability", "body", rows=[(0, (0, 0, 0)), (0.5, (-22, 0, 0)), (1.1, (-22, 0, 0)), (1.6, (0, 0, 0))])
    c.pos("ability", "body", rows=[(0, (0, 0, 0)), (0.5, (0, 3, 3)), (1.1, (0, 3, 3)), (1.6, (0, 0, 0))])
    c.rot("ability", head, rows=[(0, (0, 0, 0)), (0.5, (-30, 0, 0)), (1.1, (-34, 0, 0)), (1.6, (0, 0, 0))])
    c.rot("ability", head + "_jaw", rows=[(0, (0, 0, 0)), (0.5, (34, 0, 0)), (1.1, (34, 0, 0)), (1.6, (0, 0, 0))])
    for pre in (prefix_f,):
        c.rot("ability", pre + "_r", rows=[(0, (0, 0, 0)), (0.5, (-35, 0, 0)), (1.1, (-35, 0, 0)), (1.6, (0, 0, 0))])
        c.rot("ability", pre + "_l", rows=[(0, (0, 0, 0)), (0.5, (-35, 0, 0)), (1.1, (-35, 0, 0)), (1.6, (0, 0, 0))])
    return c


# ---------------------------------------------------------------- Hayate (Beast A): the gale wolf, white mane, black paws
def build_hayate():
    reset()
    L, bh, bw, bl = 17.0, 13.0, 11.0, 22.0
    bone("root", None, (0, 0, 0))
    bone("body", "root", (0, L + bh / 2, 0))
    C("body", "torso", 0, L, -bl / 2, bw, bh, bl, "fur")
    C("body", "belly", 0, L - 0.4, -bl / 2 + 3, bw - 3, 1.2, bl - 6, "fur2")
    C("body", "chest_fluff", 0, L + 1, -bl / 2 - 2.0, bw + 3, bh - 1, 6, "mane")
    C("body", "back_fluff", 0, L + bh - 0.5, -bl / 2 + 2, bw - 2, 2.2, bl - 4, "mane")
    # the lion-like mane around the neck
    for i, (dx, dy, dz, w, h, d, rot) in enumerate([(0, 3, -3, 15, 11, 7, None), (-6.2, 1, -2, 5, 9, 7, (0, 0, -10)), (6.2, 1, -2, 5, 9, 7, (0, 0, 10)), (0, -1, -4, 9, 5, 6, None), (0, 10, -2, 11, 3, 6, None)]):
        C("body", "mane_%d" % i, dx, L + bh - 4 + dy, -bl / 2 - 2 + dz + 4, w, h, d, "mane", rot=rot)
    hy = L + bh - 3
    dog_head("head", "h", 0, hy, -bl / 2 - 3.5, 9, "fur", "fur2", eye="round", ear_mat="fur")
    C("head", "mark", 0, hy + 6.4, -bl / 2 - 3.5 - 9.6, 1.6, 1.6, 0.6, "gem", rot=(0, 0, 45))
    leg_pair(L, 4.2, 3.6, -bl / 2 + 2.5, "fur", "paw", "lf")
    leg_pair(L, 4.2, 3.6, bl / 2 - 3.5, "fur", "paw", "lb")
    bone("tail1", "body", (0, L + bh - 3, bl / 2 - 1), rot=(-30, 0, 0))
    C("tail1", "tail_a", 0, L + bh - 6, bl / 2 - 1, 6, 6, 6, "mane", rot=(-30, 0, 0), pv=(0, L + bh - 3, bl / 2 - 1))
    bone("tail2", "tail1", (0, L + bh - 6, bl / 2 + 5), rot=(-20, 0, 0))
    C("tail2", "tail_b", 0, L + bh - 9, bl / 2 + 4, 7, 7, 7, "mane", rot=(-20, 0, 0), pv=(0, L + bh - 6, bl / 2 + 5))
    bone("tail3", "tail2", (0, L + bh - 9, bl / 2 + 10), rot=(-10, 0, 0))
    C("tail3", "tail_c", 0, L + bh - 12, bl / 2 + 9, 6, 6, 8, "fur2")
    top = hy + 9 + 4
    pal = dict(m=dict(fur=("#e8e4dc", "fur"), fur2=("#cfd6dc", "fur"), mane=("#f4f2ee", "fur"), paw=("#2a2a32", "flat"), claw="#1e1e24", tooth="#f4f0e4", dark="#1c1c22", gem=("#7fe8ff", "glow")),
               eye="#58b8e8", glow="#7fe8ff")
    c = quad_clips()
    c.rot("idle", "tail1", lambda t: (-30, 4 * math.sin(2 * math.pi * t / 3.2), 0))
    return pal, c, top, 3.0


# ---------------------------------------------------------------- Cerorin (Beast B): three-headed black hound
def build_cerorin():
    reset()
    L, bh, bw, bl = 15.0, 13.0, 12.0, 20.0
    bone("root", None, (0, 0, 0))
    bone("body", "root", (0, L + bh / 2, 0))
    C("body", "torso", 0, L, -bl / 2, bw, bh, bl, "fur")
    C("body", "belly", 0, L - 0.4, -bl / 2 + 3, bw - 3, 1.2, bl - 6, "fur2")
    C("body", "chest", 0, L + 1, -bl / 2 - 3, bw + 2, bh, 5, "fur")
    # a spiky mane down the back
    for i in range(6):
        z = -bl / 2 + 1 + i * 3.4
        C("body", "spike_%d" % i, 0, L + bh - 0.5, z, 2.6, 5.5 - 0.4 * abs(i - 1.5), 2.2, "spike", rot=(-12, 0, 0))
    hy = L + bh - 3
    hz = -bl / 2 - 2
    dog_head("head", "hc", 0, hy + 1, hz - 1, 9, "fur", "fur2", eye="slit", ear_mat="spike")
    dog_head("head_r", "hr", -8.5, hy - 2, hz + 1, 8, "fur", "fur2", eye="slit", ear_mat="spike")
    dog_head("head_l", "hl", 8.5, hy - 2, hz + 1, 8, "fur", "fur2", eye="slit", ear_mat="spike")
    for nm, x in (("neck_r", -5.5), ("neck_l", 5.5)):
        C("body", nm, x, L + bh - 5, hz + 1, 5, 6, 8, "fur", rot=(0, 0, 0))
    for sx, nm in ((-1, "r"), (1, "l")):
        for i in range(3):
            C("head" if nm == "x" else "head_" + nm, "hspike_%s%d" % (nm, i), sx * 8.5, hy + 6 - 2, hz + 1 - 3 - i * 2.4, 1.8, 3.5, 1.8, "spike", rot=(-20, 0, 0))
    leg_pair(L, 4.6, 4.0, -bl / 2 + 2.5, "fur", "paw", "lf")
    leg_pair(L, 4.6, 4.0, bl / 2 - 3.5, "fur", "paw", "lb")
    bone("tail1", "body", (0, L + bh - 3, bl / 2 - 1), rot=(-25, 0, 0))
    C("tail1", "tail_a", 0, L + bh - 5, bl / 2 - 1, 3.6, 3.6, 7, "fur", rot=(-25, 0, 0), pv=(0, L + bh - 3, bl / 2 - 1))
    bone("tail2", "tail1", (0, L + bh - 5, bl / 2 + 6), rot=(-15, 0, 0))
    C("tail2", "tail_b", 0, L + bh - 7.5, bl / 2 + 5.5, 3, 3, 7, "fur")
    bone("tail3", "tail2", (0, L + bh - 7.5, bl / 2 + 12), rot=(-10, 0, 0))
    C("tail3", "tail_tip", 0, L + bh - 9.5, bl / 2 + 11, 2.2, 2.2, 6, "spike")
    top = hy + 12
    pal = dict(m=dict(fur=("#2b2a32", "fur"), fur2=("#4a4650", "fur"), spike=("#14131a", "plate"), paw=("#18171d", "flat"), claw="#d8d0c0", tooth="#ece6d6", dark="#0e0d12"),
               eye="#ff3a40", glow="#ff3a40", mouth="#8a1e22")
    c = quad_clips(head="head")
    for hn in ("head_r", "head_l"):
        c.rot("idle", hn, lambda t, hn=hn: (3 * math.sin(2 * math.pi * t / 3.2 + (0 if hn.endswith("r") else 1.6)), 8 * math.sin(2 * math.pi * t / 3.2 + 2), 0))
        c.rot("walk", hn, lambda t, hn=hn: (4 * math.sin(2 * math.pi * t + (0 if hn.endswith("r") else 2)), 5, 0))
        c.rot("attack", hn, rows=[(0, (0, 0, 0)), (0.15, (-12, 0, 0)), (0.32, (20, 0, 0)), (0.6, (0, 0, 0))])
        c.rot("attack", hn + "_jaw", rows=[(0, (0, 0, 0)), (0.15, (30, 0, 0)), (0.32, (4, 0, 0)), (0.6, (0, 0, 0))])
        c.rot("ability", hn, rows=[(0, (0, 0, 0)), (0.5, (-28, 0, 0)), (1.1, (-30, 0, 0)), (1.6, (0, 0, 0))])
        c.rot("ability", hn + "_jaw", rows=[(0, (0, 0, 0)), (0.5, (34, 0, 0)), (1.1, (34, 0, 0)), (1.6, (0, 0, 0))])
    return pal, c, top, 3.0


# ---------------------------------------------------------------- armoured bipeds
def armour_biped(spec):
    """spec: lx leg offset, leg length, torso width/height/depth, head size, arm thickness, pauldrons, horns, shield, mat names."""
    hip = spec["leg"]
    lt, lx = spec["lt"], spec["lx"]
    bone("root", None, (0, 0, 0))
    bone("body", "root", (0, hip, 0))
    bone("chest", "body", (0, hip + 4, 0))
    bone("head", "chest", (0, hip + 4 + spec["ch"], 0))

    def legs(sd, n):
        bone("leg_" + n, "root", (-lx, hip, 0), side=sd)
        Cx("leg_" + n, "thigh_" + n, -lx - lt / 2, hip * 0.5, -lt / 2, lt, hip * 0.5, lt, "armor", side=sd)
        Cx("leg_" + n, "knee_" + n, -lx - lt / 2 - 0.5, hip * 0.5 - 1.5, -lt / 2 - 1.2, lt + 1, 3, lt + 1.2, "plate", side=sd)
        Cx("leg_" + n, "shin_" + n, -lx - lt / 2 + 0.2, 2.5, -lt / 2 + 0.2, lt - 0.4, hip * 0.5 - 3.5, lt - 0.4, "armor", side=sd)
        Cx("leg_" + n, "boot_" + n, -lx - lt / 2 - 0.6, 0, -lt / 2 - 2.4, lt + 1.2, 3, lt + 3.6, "plate", side=sd)
    pair(legs)
    C("body", "pelvis", 0, hip - 1.5, -spec["td"] / 2 + 0.5, spec["tw"] - 1, 5.5, spec["td"] - 1, "armor")
    C("body", "skirt", 0, hip - 4, -spec["td"] / 2 - 0.5, spec["tw"] + 1, 4.0, spec["td"] + 1, "plate")
    C("chest", "torso", 0, hip + 4, -spec["td"] / 2, spec["tw"], spec["ch"], spec["td"], "armor")
    C("chest", "breast", 0, hip + 4 + spec["ch"] * 0.25, -spec["td"] / 2 - 1.2, spec["tw"] - 2, spec["ch"] * 0.6, 1.6, "plate")
    if spec.get("core"):
        C("chest", "core", 0, hip + 4 + spec["ch"] * 0.5, -spec["td"] / 2 - 1.8, 2.4, 2.4, 0.8, spec["core"])
    C("chest", "back", 0, hip + 4, spec["td"] / 2 - 0.2, spec["tw"] - 1, spec["ch"] - 1, 1.6, "plate")
    hs = spec["hs"]
    hy = hip + 4 + spec["ch"] + 0.5
    C("head", "head", 0, hy, -hs / 2, hs, hs, hs, "armor", paint="slit")
    C("head", "helm", 0, hy + hs - 1.5, -hs / 2 - 0.4, hs + 1.0, 3.5, hs + 0.8, "plate")
    C("head", "visor", 0, hy + 0.5, -hs / 2 - 0.9, hs + 0.2, hs * 0.55, 1.0, "plate", paint="slit")
    if spec.get("horns"):
        for sx in (-1, 1):
            C("head", "horn%d" % sx, sx * (hs / 2 - 0.5), hy + hs + 1, -1.5, 2.0, 6.5, 2.4, "plate", rot=(0, 0, sx * -10))
    if spec.get("crest"):
        C("head", "crest", 0, hy + hs, -hs / 2, 2.0, 4.5, hs + 1, "plate")

    def arms(sd, n):
        ax = spec["tw"] / 2 + spec["at"] / 2 + 0.6
        sy = hip + 4 + spec["ch"] - 2.2
        bone("arm_" + n, "chest", (-ax, sy, 0), side=sd)
        Cx("arm_" + n, "shoulder_" + n, -ax - spec["at"] / 2 - 1.4, sy - 3.2, -spec["at"] / 2 - 1.4, spec["at"] + 2.8, 5, spec["at"] + 2.8, "plate", side=sd)
        Cx("arm_" + n, "upper_" + n, -ax - spec["at"] / 2, sy - 8.5, -spec["at"] / 2, spec["at"], 6, spec["at"], "armor", side=sd)
        bone("arm_%s_low" % n, "arm_" + n, (-ax, sy - 8.5, 0), side=sd)
        Cx("arm_%s_low" % n, "fore_" + n, -ax - spec["at"] / 2 - 0.3, sy - 15.5, -spec["at"] / 2 - 0.3, spec["at"] + 0.6, 7, spec["at"] + 0.6, "plate", side=sd)
        Cx("arm_%s_low" % n, "fist_" + n, -ax - spec["at"] / 2 - 0.8, sy - 19.5, -spec["at"] / 2 - 0.8, spec["at"] + 1.6, 4, spec["at"] + 1.6, "armor", side=sd)
    pair(arms)
    top = hy + hs + (6 if spec.get("horns") else 3)
    return top


def armour_clips(shield=False):
    c = Clips()
    P = 3.2
    s = lambda t, ph=0.0: math.sin(2 * math.pi * t / P + ph)
    c.pos("idle", "body", lambda t: (0, 0.3 * s(t), 0))
    c.rot("idle", "chest", lambda t: (1.2 * s(t, 0.4), 1.2 * s(t, 1.1), 0))
    c.rot("idle", "head", lambda t: (1.5 * s(t, 1.0), 4 * s(t, 0.2), 0))
    c.rot("idle", "arm_r", lambda t: (2 * s(t, 0.6), 0, -3))
    c.rot("idle", "arm_l", lambda t: (2 * s(t, 2.2), 0, 3))
    c.rot("idle", "arm_r_low", lambda t: (-12 + 3 * s(t, 0.2), 0, 0))
    c.rot("idle", "arm_l_low", lambda t: (-12 + 3 * s(t, 1.6), 0, 0))
    W = 1.0
    w = lambda t, ph=0.0: math.sin(2 * math.pi * t / W + ph)
    c.pos("walk", "body", lambda t: (0, 0.5 * abs(w(t)) - 0.2, 0))
    c.rot("walk", "chest", lambda t: (0, -4 * w(t, 1.57), 0))
    c.rot("walk", "leg_r", lambda t: (30 * w(t), 0, 0))
    c.rot("walk", "leg_l", lambda t: (30 * w(t, math.pi), 0, 0))
    c.rot("walk", "arm_r", lambda t: (-24 * w(t), 0, -3))
    c.rot("walk", "arm_l", lambda t: (-24 * w(t, math.pi), 0, 3))
    c.rot("walk", "arm_r_low", lambda t: (-18, 0, 0))
    c.rot("walk", "arm_l_low", lambda t: (-18, 0, 0))
    c.rot("attack", "chest", rows=[(0, (0, 0, 0)), (0.15, (-8, -26, 0)), (0.3, (12, 30, 0)), (0.6, (0, 0, 0))])
    c.rot("attack", "head", rows=[(0, (0, 0, 0)), (0.15, (-6, -10, 0)), (0.3, (10, 12, 0)), (0.6, (0, 0, 0))])
    c.rot("attack", "arm_r", rows=[(0, (0, 0, -3)), (0.15, (-150, 0, -10)), (0.3, (-40, 0, 4)), (0.6, (0, 0, -3))])
    c.rot("attack", "arm_r_low", rows=[(0, (-12, 0, 0)), (0.15, (-30, 0, 0)), (0.3, (-4, 0, 0)), (0.6, (-12, 0, 0))])
    c.rot("attack", "arm_l", rows=[(0, (0, 0, 3)), (0.15, (-40, 0, 12)), (0.3, (12, 0, 6)), (0.6, (0, 0, 3))])
    c.rot("ability", "chest", rows=[(0, (0, 0, 0)), (0.5, (-10, 0, 0)), (1.1, (-10, 0, 0)), (1.6, (0, 0, 0))])
    c.rot("ability", "head", rows=[(0, (0, 0, 0)), (0.5, (-18, 0, 0)), (1.1, (-18, 0, 0)), (1.6, (0, 0, 0))])
    c.rot("ability", "arm_r", rows=[(0, (0, 0, -3)), (0.5, (-80, 0, -50)), (1.1, (-80, 0, -50)), (1.6, (0, 0, -3))])
    c.rot("ability", "arm_l", rows=[(0, (0, 0, 3)), (0.5, (-80, 0, 50)), (1.1, (-80, 0, 50)), (1.6, (0, 0, 3))])
    return c


def build_rocanel():
    reset()
    spec = dict(leg=16, lt=4.6, lx=3.4, tw=11, td=6.5, ch=11, hs=8, at=4.2, horns=True, core="gem")
    top = armour_biped(spec)
    # the big round shield on the left forearm, and a spiked fist on the right
    bone("shield", "arm_l_low", (7.6, 24, -3))
    C("shield", "shield", 10.5, 15.5, -9.5, 16, 16, 2.2, "shield", side=-1, rot=None)
    C("shield", "shield_rim", 10.5, 14.5, -9.8, 18, 18, 1.0, "plate")
    pal = dict(m=dict(armor=("#6d7480", "plate"), plate=("#9aa2ae", "plate"), gem=("#69e6ff", "glow"), shield=("#8e96a2", "ring", "#69e6ff")), eye="#69e6ff", glow="#69e6ff")
    c = armour_clips()
    return pal, c, top, 3.0


def build_mira():
    reset()
    spec = dict(leg=13, lt=5.6, lx=4.2, tw=15, td=8.5, ch=12, hs=9, at=5.4, crest=True, core=None)
    top = armour_biped(spec)
    # a large polished mirror stone on the chest and a smaller one on each shoulder
    C("chest", "mirror", 0, 13 + 4 + 2, -8.5 / 2 - 2.6, 9, 9, 1.4, "mirror", rot=None)
    C("chest", "mirror_frame", 0, 13 + 4 + 1, -8.5 / 2 - 2.2, 11, 11, 1.0, "plate")
    pal = dict(m=dict(armor=("#5d5a66", "plate"), plate=("#7b7886", "plate"), mirror=("#bfe8f6", "mirror", "#e8ffff"), gem=("#9be8ff", "glow")), eye="#9be8ff", glow="#9be8ff")
    c = armour_clips()
    return pal, c, top, 3.0
