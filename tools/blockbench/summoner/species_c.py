"""Summon models, part 3: Ellie and Okiyosan (Spirit dolls), Dradra (Dragon B), Merus (the Angel)."""
import math

from summon_lib import C, Cx, Clips, bone, pair, reset


def humanoid_clips(skirt=False, wings=False, hover=True):
    c = Clips()
    P = 3.2
    s = lambda t, ph=0.0: math.sin(2 * math.pi * t / P + ph)
    c.pos("idle", "body", lambda t: (0, (1.4 if hover else 0.3) * s(t), 0))
    c.rot("idle", "head", lambda t: (2 * s(t, 1), 5 * s(t), 1.5 * s(t, 2)))
    c.rot("idle", "arm_r", lambda t: (3 * s(t, 0.6), 0, -5 + 2 * s(t)))
    c.rot("idle", "arm_l", lambda t: (3 * s(t, 2.2), 0, 5 - 2 * s(t)))
    c.rot("idle", "skirt", lambda t: (3 * s(t, 0.3), 0, 2 * s(t, 1)))
    c.rot("idle", "hair", lambda t: (4 + 3 * s(t, 0.8), 0, 2 * s(t)))
    for n in ("a", "b"):
        c.rot("idle", "wing_r" if n == "a" else "wing_l", lambda t, n=n: (0, 6 * s(t, 0.4) * (1 if n == "a" else -1), (-10 if n == "a" else 10) * s(t, 0.3)))
    w = lambda t, ph=0.0: math.sin(2 * math.pi * t / 1.0 + ph)
    c.pos("walk", "body", lambda t: (0, (1.2 if hover else 0.4) * abs(w(t)), 0))
    c.rot("walk", "body", lambda t: (8, 0, 0))
    c.rot("walk", "leg_r", lambda t: (28 * w(t), 0, 0))
    c.rot("walk", "leg_l", lambda t: (28 * w(t, math.pi), 0, 0))
    c.rot("walk", "arm_r", lambda t: (-22 * w(t), 0, -4))
    c.rot("walk", "arm_l", lambda t: (-22 * w(t, math.pi), 0, 4))
    c.rot("walk", "skirt", lambda t: (8 * w(t, 1), 0, 0))
    c.rot("walk", "hair", lambda t: (16 + 6 * w(t, 0.6), 0, 0))
    c.rot("walk", "wing_r", lambda t: (0, 8 * w(t, 0.5), -16 * w(t)))
    c.rot("walk", "wing_l", lambda t: (0, -8 * w(t, 0.5), 16 * w(t)))
    c.rot("attack", "body", rows=[(0, (0, 0, 0)), (0.15, (-8, 0, 0)), (0.3, (10, 0, 0)), (0.6, (0, 0, 0))])
    c.rot("attack", "arm_r", rows=[(0, (0, 0, -5)), (0.15, (-130, 0, -10)), (0.3, (-30, 0, 4)), (0.6, (0, 0, -5))])
    c.rot("attack", "arm_l", rows=[(0, (0, 0, 5)), (0.15, (-130, 0, 10)), (0.3, (-30, 0, -4)), (0.6, (0, 0, 5))])
    c.rot("attack", "head", rows=[(0, (0, 0, 0)), (0.15, (-10, 0, 0)), (0.3, (8, 0, 0)), (0.6, (0, 0, 0))])
    c.rot("ability", "arm_r", rows=[(0, (0, 0, -5)), (0.5, (-150, 0, -30)), (1.1, (-150, 0, -30)), (1.6, (0, 0, -5))])
    c.rot("ability", "arm_l", rows=[(0, (0, 0, 5)), (0.5, (-150, 0, 30)), (1.1, (-150, 0, 30)), (1.6, (0, 0, 5))])
    c.rot("ability", "head", rows=[(0, (0, 0, 0)), (0.5, (-20, 0, 0)), (1.1, (-20, 0, 0)), (1.6, (0, 0, 0))])
    c.pos("ability", "body", rows=[(0, (0, 0, 0)), (0.6, (0, 4, 0)), (1.1, (0, 4, 0)), (1.6, (0, 0, 0))])
    c.rot("ability", "wing_r", rows=[(0, (0, 0, 0)), (0.5, (0, 18, -40)), (1.1, (0, 18, -40)), (1.6, (0, 0, 0))])
    c.rot("ability", "wing_l", rows=[(0, (0, 0, 0)), (0.5, (0, -18, 40)), (1.1, (0, -18, 40)), (1.6, (0, 0, 0))])
    return c


def doll(spec):
    """A porcelain doll, 22 px tall: big head, a flared dress, joints. spec: hair mat, dress mat, trim mat, hair length, sleeves wide."""
    bone("root", None, (0, 0, 0))
    bone("body", "root", (0, 9, 0))
    bone("skirt", "body", (0, 12, 0))
    C("skirt", "skirt_a", 0, 6, -4.2, 8.4, 3, 8.4, spec["dress"])
    C("skirt", "skirt_b", 0, 3.4, -5.2, 10.4, 2.8, 10.4, spec["dress"])
    C("skirt", "skirt_c", 0, 1.0, -5.8, 11.6, 2.6, 11.6, spec["trim"])
    C("body", "torso", 0, 9, -2.6, 6.4, 5.6, 5.2, spec["dress"])
    C("body", "collar", 0, 13.4, -3.1, 7.2, 1.8, 6.2, spec["trim"])
    if spec.get("bow"):
        C("body", "bow", 0, 11.8, -3.6, 3.2, 1.8, 1.0, "bow")
    bone("head", "body", (0, 15, 0))
    C("head", "head", 0, 15, -3.8, 7.6, 7.6, 7.6, "porcelain", paint="doll")
    C("head", "hair_top", 0, 21.6, -4.3, 8.4, 2.0, 8.6, spec["hair"])
    C("head", "bang", 0, 19.6, -4.55, 8.4, 2.6, 1.2, spec["hair"])
    C("head", "hair_side_r", -4.2, 14.4, -3.8, 1.2, 7.6, 7.6, spec["hair"])
    C("head", "hair_side_l", 4.2, 14.4, -3.8, 1.2, 7.6, 7.6, spec["hair"])
    bone("hair", "head", (0, 21, 3.8))
    C("hair", "hair_back", 0, 14.4 - spec["hair_len"], 3.6, 8.6, 6.6 + spec["hair_len"], 1.8, spec["hair"])
    C("hair", "hair_tail_r", -3.2, 7.4 - spec["hair_len"] * 0.4, 3.9, 2.4, 6, 1.2, spec["hair"])
    C("hair", "hair_tail_l", 3.2, 7.4 - spec["hair_len"] * 0.4, 3.9, 2.4, 6, 1.2, spec["hair"])

    def arms(sd, n):
        bone("arm_" + n, "body", (-4.4, 13.4, 0), side=sd)
        Cx("arm_" + n, "sleeve_" + n, -4.4 - spec["sleeve"] / 2 - 0.6, 9.4, -1.6 - (spec["sleeve"] - 3.2) / 2, spec["sleeve"], 4.8, spec["sleeve"] + (0), spec["dress"], side=sd)
        Cx("arm_" + n, "joint_" + n, -4.4 - 1.5, 8.2, -1.5, 3.0, 1.4, 3.0, "joint", side=sd)
        Cx("arm_" + n, "hand_" + n, -4.4 - 1.4, 5.0, -1.4, 2.8, 3.2, 2.8, "porcelain", side=sd)
    pair(arms)

    def legs(sd, n):
        bone("leg_" + n, "body", (-1.8, 8, 0), side=sd)
        Cx("leg_" + n, "leg_" + n, -1.8 - 1.2, 2.0, -1.2, 2.4, 6.4, 2.4, "porcelain", side=sd)
        Cx("leg_" + n, "shoe_" + n, -1.8 - 1.6, 0.6, -2.4, 3.2, 1.8, 4.4, "shoe", side=sd)
    pair(legs)
    return 22.0 * 1.0 + 2.5


def build_ellie():
    reset()
    spec = dict(hair="hair", dress="dress", trim="trim", hair_len=9, sleeve=4.0, bow=True)
    top = doll(spec)
    pal = dict(m=dict(porcelain=("#f1e2d8", "porcelain"), joint=("#cdbab0", "plate"), hair=("#e0c470", "fur"), dress=("#242c4e", "cloth"), trim=("#f2f0f4", "cloth"), bow=("#14141c", "flat"),
                      shoe=("#14141c", "flat")), eye="#7fc8e8", glow="#8fb8ff", mouth="#c88a8a")
    c = humanoid_clips()
    return pal, c, top, 2.0


def build_okiyosan():
    reset()
    spec = dict(hair="hair", dress="dress", trim="trim", hair_len=18, sleeve=7.0, bow=False)
    top = doll(spec)
    # the headband with two candles that burn, and the long curse nail she holds
    C("head", "band", 0, 20.6, -4.1, 8.4, 1.6, 8.2, "band")
    for sx in (-1, 1):
        C("head", "candle%d" % sx, sx * 2.6, 22.0, -0.6, 1.2, 3.2, 1.2, "candle")
        C("head", "flame%d" % sx, sx * 2.6, 25.2, -0.8, 1.0, 1.8, 1.0, "flame")
    C("body", "obi", 0, 9.4, -3.0, 7.0, 1.8, 5.8, "obi")
    C("arm_r", "nail", -5.8, 2.0, -1.0, 0.9, 5.0, 0.9, "nail", rot=(0, 0, 0))
    pal = dict(m=dict(porcelain=("#ece4de", "porcelain"), joint=("#c8bcb4", "plate"), hair=("#14121a", "fur"), dress=("#f0ece4", "cloth"), trim=("#d8d0c4", "cloth"), band=("#f2eee6", "cloth"),
                      candle=("#e8e0c8", "flat"), flame=("#ffb040", "glow"), obi=("#58535e", "cloth"), nail=("#d84a3a", "glow"), shoe=("#2a2428", "flat")), eye="#101010", glow="#ffa030", mouth="#8a4a4a")
    c = humanoid_clips()
    return pal, c, top, 2.0


# ---------------------------------------------------------------- Dradra: the winged fire dragon
def build_dradra():
    reset()
    L, bh, bw, bl = 16.0, 14.0, 14.0, 26.0
    bone("root", None, (0, 0, 0))
    bone("body", "root", (0, L + bh / 2, 0))
    C("body", "torso", 0, L, -bl / 2, bw, bh, bl, "scale")
    C("body", "belly", 0, L - 0.5, -bl / 2 + 2, bw - 3, 2, bl - 4, "belly")
    for i in range(6):
        C("body", "spine_%d" % i, 0, L + bh, -bl / 2 + 2 + i * 4.2, 2.4, 4.6 - 0.35 * i, 2.4, "spike", rot=(-10, 0, 0))
    bone("neck", "body", (0, L + bh - 3, -bl / 2 + 1))
    C("neck", "neck_a", 0, L + bh - 4, -bl / 2 - 5, 9, 9, 7, "scale", rot=(18, 0, 0), pv=(0, L + bh - 3, -bl / 2 + 1))
    bone("neck2", "neck", (0, L + bh + 3, -bl / 2 - 5))
    C("neck2", "neck_b", 0, L + bh + 1, -bl / 2 - 11, 8, 8, 7, "scale", rot=(12, 0, 0), pv=(0, L + bh + 3, -bl / 2 - 5))
    bone("head", "neck2", (0, L + bh + 6, -bl / 2 - 11))
    hz = -bl / 2 - 11
    hy = L + bh + 4
    C("head", "skull", 0, hy, hz - 9, 9, 8, 9, "scale", paint="slit")
    C("head", "snout", 0, hy + 0.5, hz - 17, 6.4, 5, 8, "scale")
    C("head", "nostril_r", -1.6, hy + 4, hz - 17.4, 1.2, 1.2, 1.0, "gem")
    C("head", "nostril_l", 1.6, hy + 4, hz - 17.4, 1.2, 1.2, 1.0, "gem")
    bone("jaw", "head", (0, hy + 1, hz - 6))
    C("jaw", "jaw", 0, hy - 1.6, hz - 16.6, 5.8, 2.2, 10, "belly")
    C("jaw", "tongue", 0, hy + 0.4, hz - 15, 3, 0.8, 7, "gem")
    for sx in (-1, 1):
        C("head", "horn%d" % sx, sx * 3.4, hy + 7, hz - 4, 2.2, 6, 2.2, "bone", rot=(-35, 0, sx * -12))
        C("head", "cheek%d" % sx, sx * 5.4, hy + 2, hz - 7, 2.4, 3, 4, "spike")
    def wings(sd, n):
        bone("wing_" + n, "body", (-6, L + bh - 1, -3), side=sd)
        Cx("wing_" + n, "wing_arm_" + n, -6 - 14, L + bh - 2, -4, 14, 2.4, 2.4, "scale", side=sd)
        Cx("wing_" + n, "wing_mem_a_" + n, -6 - 14, L + bh - 2.8, -1.6, 14, 0.8, 14, "membrane", side=sd)
        bone("wing_%s_tip" % n, "wing_" + n, (-20, L + bh - 1, -3), side=sd)
        Cx("wing_%s_tip" % n, "wing_fin_" + n, -20 - 16, L + bh - 2, -4, 16, 2, 2, "bone", side=sd)
        Cx("wing_%s_tip" % n, "wing_mem_b_" + n, -20 - 18, L + bh - 2.6, -2, 18, 0.8, 18, "membrane", side=sd)
    pair(wings)
    def legs(sd, n, prefix, z, thick):
        bone("%s_%s" % (prefix, n), "body", (-bw / 2 - 1, L, z), side=sd)
        Cx("%s_%s" % (prefix, n), "%s_up_%s" % (prefix, n), -bw / 2 - 1 - thick / 2 - 1, L * 0.4, z - thick / 2, thick + 1, L * 0.6, thick, "scale", side=sd)
        Cx("%s_%s" % (prefix, n), "%s_lo_%s" % (prefix, n), -bw / 2 - 1 - thick / 2 - 0.4, 2, z - thick / 2 + 0.2, thick, L * 0.4 - 2, thick - 0.4, "scale", side=sd)
        Cx("%s_%s" % (prefix, n), "%s_foot_%s" % (prefix, n), -bw / 2 - 1 - thick / 2 - 0.8, 0, z - thick / 2 - 3.0, thick + 1.6, 2.4, thick + 3.4, "bone", side=sd)
    pair(lambda sd, n: legs(sd, n, "lf", -bl / 2 + 4, 4.6))
    pair(lambda sd, n: legs(sd, n, "lb", bl / 2 - 5, 5.6))
    cur = "body"
    ty = L + bh * 0.4
    bone("tail1", "body", (0, ty, bl / 2 - 1))
    C("tail1", "tail_a", 0, ty - 5, bl / 2 - 1, 9, 9, 8, "scale")
    bone("tail2", "tail1", (0, ty - 1, bl / 2 + 7))
    C("tail2", "tail_b", 0, ty - 5, bl / 2 + 7, 7, 7, 8, "scale")
    bone("tail3", "tail2", (0, ty - 1.5, bl / 2 + 15))
    C("tail3", "tail_c", 0, ty - 4.6, bl / 2 + 15, 5, 5, 8, "scale")
    C("tail3", "tail_spike", 0, ty - 6.8, bl / 2 + 22, 3.2, 8.4, 3.2, "spike", rot=(80, 0, 0))
    top = L + bh + 22
    pal = dict(m=dict(scale=("#3a3d46", "scale"), belly=("#c8c4b4", "plate"), spike=("#22242c", "plate"), bone=("#d6d0bc", "plate"), membrane=("#5a4a52", "scale"), gem=("#ff7a2a", "glow")),
               eye="#ffc040", glow="#ff7a2a")
    c = Clips()
    P = 3.2
    s = lambda t, ph=0.0: math.sin(2 * math.pi * t / P + ph)
    c.pos("idle", "body", lambda t: (0, 0.5 * s(t), 0))
    c.rot("idle", "neck", lambda t: (3 * s(t, 0.6), 4 * s(t), 0))
    c.rot("idle", "head", lambda t: (2 * s(t, 1), 3 * s(t, 0.4), 0))
    c.rot("idle", "jaw", lambda t: (3 + 3 * max(0, s(t, 0.5)), 0, 0))
    c.rot("idle", "wing_r", lambda t: (0, 0, -9 * s(t, 0.3)))
    c.rot("idle", "wing_l", lambda t: (0, 0, 9 * s(t, 0.3)))
    c.rot("idle", "wing_r_tip", lambda t: (0, 0, -7 * s(t, -0.4)))
    c.rot("idle", "wing_l_tip", lambda t: (0, 0, 7 * s(t, -0.4)))
    for i, tn in enumerate(("tail1", "tail2", "tail3")):
        c.rot("idle", tn, lambda t, i=i: (0, 7 * (i + 1) * s(t, -i * 0.8), 0))
    w = lambda t, ph=0.0: math.sin(2 * math.pi * t / 1.0 + ph)
    c.rot("walk", "wing_r", lambda t: (0, 0, -34 * w(t)))
    c.rot("walk", "wing_l", lambda t: (0, 0, 34 * w(t)))
    c.rot("walk", "wing_r_tip", lambda t: (0, 0, -24 * w(t, -0.9)))
    c.rot("walk", "wing_l_tip", lambda t: (0, 0, 24 * w(t, -0.9)))
    c.pos("walk", "body", lambda t: (0, 0.9 * w(t, 1.2), 0))
    for pre, ph in (("lf", 0.0), ("lb", math.pi)):
        c.rot("walk", pre + "_r", lambda t, ph=ph: (22 * w(t, ph), 0, 0))
        c.rot("walk", pre + "_l", lambda t, ph=ph: (22 * w(t, ph + math.pi), 0, 0))
    for i, tn in enumerate(("tail1", "tail2", "tail3")):
        c.rot("walk", tn, lambda t, i=i: (-4, 9 * (i + 1) * w(t, 0.8 - i * 0.6), 0))
    c.rot("attack", "neck", rows=[(0, (0, 0, 0)), (0.15, (-16, 0, 0)), (0.32, (24, 0, 0)), (0.6, (0, 0, 0))])
    c.rot("attack", "head", rows=[(0, (0, 0, 0)), (0.15, (-14, 0, 0)), (0.32, (18, 0, 0)), (0.6, (0, 0, 0))])
    c.rot("attack", "jaw", rows=[(0, (0, 0, 0)), (0.15, (36, 0, 0)), (0.32, (4, 0, 0)), (0.6, (0, 0, 0))])
    c.rot("ability", "neck", rows=[(0, (0, 0, 0)), (0.5, (-24, 0, 0)), (1.2, (6, 0, 0)), (1.6, (0, 0, 0))])
    c.rot("ability", "head", rows=[(0, (0, 0, 0)), (0.5, (-30, 0, 0)), (1.2, (10, 0, 0)), (1.6, (0, 0, 0))])
    c.rot("ability", "jaw", rows=[(0, (0, 0, 0)), (0.5, (38, 0, 0)), (1.2, (34, 0, 0)), (1.6, (0, 0, 0))])
    c.rot("ability", "body", rows=[(0, (0, 0, 0)), (0.5, (-14, 0, 0)), (1.2, (-4, 0, 0)), (1.6, (0, 0, 0))])
    c.rot("ability", "wing_r", rows=[(0, (0, 0, 0)), (0.5, (0, 0, -40)), (1.1, (0, 0, -40)), (1.6, (0, 0, 0))])
    c.rot("ability", "wing_l", rows=[(0, (0, 0, 0)), (0.5, (0, 0, 40)), (1.1, (0, 0, 40)), (1.6, (0, 0, 0))])
    return pal, c, top, 5.0


# ---------------------------------------------------------------- Merus: a male angel, white wrap, gold bands, wings and a halo
def build_merus():
    reset()
    lt, lx, hip = 4.4, 2.6, 15.0
    bone("root", None, (0, 0, 0))
    bone("body", "root", (0, hip, 0))
    bone("chest", "body", (0, hip + 5, 0))
    bone("head", "chest", (0, hip + 5 + 12, 0))
    def legs(sd, n):
        bone("leg_" + n, "root", (-lx, hip, 0), side=sd)
        Cx("leg_" + n, "thigh_" + n, -lx - lt / 2, hip * 0.5, -lt / 2, lt, hip * 0.5, lt, "robe", side=sd)
        Cx("leg_" + n, "shin_" + n, -lx - lt / 2 + 0.2, 1.5, -lt / 2 + 0.2, lt - 0.4, hip * 0.5 - 1.5, lt - 0.4, "robe", side=sd)
        Cx("leg_" + n, "sandal_" + n, -lx - lt / 2 - 0.4, 0, -lt / 2 - 1.6, lt + 0.8, 1.6, lt + 2.6, "gold", side=sd)
        Cx("leg_" + n, "band_" + n, -lx - lt / 2 - 0.3, hip * 0.5 - 2, -lt / 2 - 0.3, lt + 0.6, 1.4, lt + 0.6, "gold", side=sd)
    pair(legs)
    C("body", "wrap", 0, hip - 2, -3.4, 10.2, 7.5, 6.8, "robe")
    C("body", "sash", 0, hip + 3, -3.6, 10.6, 2, 7.2, "gold")
    C("body", "cloth_f", 0, 4, -3.8, 5.4, 12, 1.2, "robe")
    C("chest", "torso", 0, hip + 5, -2.8, 10.6, 10, 5.6, "skin")
    C("chest", "pec", 0, hip + 9, -3.2, 9.6, 3.2, 1.0, "skin2")
    C("chest", "collar", 0, hip + 14.2, -3.2, 8, 1.6, 6.6, "gold")
    C("head", "head", 0, hip + 17.4, -4, 8, 8, 8, "skin", paint="round")
    C("head", "hair_a", 0, hip + 24.4, -4.6, 9.4, 3.2, 9.2, "hair")
    C("head", "hair_b", 0, hip + 22, 3.2, 9, 4.6, 2.4, "hair")
    for sx in (-1, 1):
        C("head", "curl%d" % sx, sx * 4.6, hip + 20, -2.2, 1.8, 4.6, 5.4, "hair")
    C("head", "brow_gem", 0, hip + 21.2, -4.7, 1.4, 1.4, 0.6, "gem")
    bone("halo", "head", (0, hip + 29.5, 0))
    for i in range(8):
        a = i * math.pi / 4
        C("halo", "halo_%d" % i, 7.2 * math.cos(a), hip + 28.8, 7.2 * math.sin(a) - 0.0, 3.4, 1.0, 1.2, "gem", rot=(0, -math.degrees(a), 0))
    def arms(sd, n):
        bone("arm_" + n, "chest", (-6.4, hip + 14, 0), side=sd)
        Cx("arm_" + n, "upper_" + n, -6.4 - 2.0, hip + 7, -2.0, 4, 7, 4, "skin", side=sd)
        Cx("arm_" + n, "bracer_a_" + n, -6.4 - 2.4, hip + 11, -2.4, 4.8, 1.6, 4.8, "gold", side=sd)
        bone("arm_%s_low" % n, "arm_" + n, (-6.4, hip + 7, 0), side=sd)
        Cx("arm_%s_low" % n, "fore_" + n, -6.4 - 1.8, hip - 0.4, -1.8, 3.6, 7.4, 3.6, "skin", side=sd)
        Cx("arm_%s_low" % n, "bracer_b_" + n, -6.4 - 2.3, hip + 2, -2.3, 4.6, 2.2, 4.6, "gold", side=sd)
    pair(arms)
    def wings(sd, n):
        bone("wing_" + n, "chest", (-3, hip + 12, 3.2), side=sd)
        for i, (ln, yy, rz) in enumerate(((20, hip + 15, 12), (22, hip + 12, 2), (18, hip + 9, -10), (14, hip + 6, -24))):
            Cx("wing_" + n, "feather_%d_%s" % (i, n), -3 - ln, yy, 3.2 + i * 0.4, ln, 3.2, 0.9, "feather", rot=(0, 0, rz), pv=(-3, yy + 1.6, 3.4), side=sd)
        Cx("wing_" + n, "wing_bone_" + n, -3 - 8, hip + 14.4, 2.8, 8, 2.2, 1.2, "robe", side=sd)
    pair(wings)
    top = hip + 31
    pal = dict(m=dict(robe=("#f0ece6", "cloth"), gold=("#e8c050", "plate"), skin=("#f0d4b8", "porcelain"), skin2=("#e2c0a4", "porcelain"), hair=("#d8b070", "fur"),
                      feather=("#f8f4ec", "fur"), gem=("#fff0a0", "glow")), eye="#6ab8e8", glow="#fff0a0")
    c = humanoid_clips(wings=True, hover=True)
    for i in range(2):
        pass
    c.rot("idle", "halo", lambda t: (0, 20 * t, 0))
    c.rot("walk", "halo", lambda t: (0, 90 * t, 0))
    c.rot("ability", "halo", lambda t: (0, 180 * t, 0))
    return pal, c, top, 4.5
