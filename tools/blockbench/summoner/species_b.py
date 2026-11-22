"""Summon models, part 2: Hatch (Insect A, the queen hornet), Tsubame (Bird A, the swallow), Solarin (Grass A, the bean), Genbu (Fish B, the archelon)."""
import math

from summon_lib import C, Cx, Clips, bone, pair, reset, decal, box, line, np, rgb


def S(period, ph=0.0):
    return lambda t: math.sin(2 * math.pi * t / period + ph)


# ---------------------------------------------------------------- decals (second pass)
@decal("hatch_eye")
def _hatch_eye(img, g, face, fw, fh, pal):
    if face in ("north", "east", "west"):
        box(img, fw, fh, 0.0, 0.0, 1.0, 1.0, "#101014")
        if face == "north":
            box(img, fw, fh, 0.50, 0.10, 0.88, 0.42, "#ffffff")   # the white highlight of the big manga eye
            box(img, fw, fh, 0.14, 0.62, 0.30, 0.78, "#9098ac")


@decal("hatch_head")
def _hatch_head(img, g, face, fw, fh, pal):
    if face == "north":
        line(img, fw, fh, 0.50, 0.0, 0.50, 0.5, "#c8c2ae", 1)
        line(img, fw, fh, 0.08, 0.55, 0.30, 0.78, "#c8c2ae", 1)
        line(img, fw, fh, 0.92, 0.55, 0.70, 0.78, "#c8c2ae", 1)


@decal("hatch_jaw")
def _hatch_jaw(img, g, face, fw, fh, pal):
    if face in ("north", "up"):
        box(img, fw, fh, 0.30, 0.0, 0.70, 0.6, "#2a1c20")
        box(img, fw, fh, 0.40, 0.0, 0.60, 0.3, "#e8e0d0")


@decal("hatch_tip")
def _hatch_tip(img, g, face, fw, fh, pal):
    box(img, fw, fh, 0.2, 0.2, 0.8, 0.8, "#fff1c8", blend=0.4)


# ---------------------------------------------------------------- Hatch: the queen hornet, upright: huge round head with black manga eyes, white thorax, striped abdomen curled forward under her, a hooked sting
def build_hatch():
    reset()
    bone("root", None, (0, 0, 0))
    bone("body", "root", (0, 18, 0))
    C("body", "thorax", 0, 20, -5, 10, 10, 9, "pale", paint=None)
    C("body", "thorax_back", 0, 22, 2.6, 8, 7, 3, "dark")
    C("body", "shoulder_r", -5.8, 25, -4.5, 3.4, 4.6, 5.4, "pale")
    C("body", "shoulder_l", 5.8, 25, -4.5, 3.4, 4.6, 5.4, "pale")
    C("body", "waist", 0, 17, -2, 5, 4, 5, "dark")
    # the abdomen curls down, back and forward: four stripe rings and a hooked sting
    bone("abdomen", "body", (0, 20, 2))
    C("abdomen", "abd_a", 0, 11, 1, 9.5, 9, 10, "stripe_a", rot=(-14, 0, 0), pv=(0, 18, 3))
    C("abdomen", "abd_b", 0, 3.6, 5, 11, 9, 10.5, "stripe_b", rot=(-6, 0, 0), pv=(0, 10, 8))
    C("abdomen", "abd_c", 0, -1, 4.2, 9.6, 7.6, 9.6, "stripe_a", rot=(14, 0, 0), pv=(0, 4, 9))
    C("abdomen", "abd_tip", 0, -2.6, 0.6, 6.6, 6.0, 7.2, "tip", paint="hatch_tip")
    C("abdomen", "sting_a", 0, -1.0, -3.8, 2.6, 2.6, 4.6, "dark", rot=(-20, 0, 0))
    C("abdomen", "sting_b", 0, 1.2, -7.4, 1.8, 1.8, 4.2, "dark", rot=(-56, 0, 0))
    C("abdomen", "sting_c", 0, 4.2, -9.6, 1.1, 1.1, 3.4, "dark", rot=(-86, 0, 0))
    bone("head", "body", (0, 30, -4))
    C("head", "head", 0, 29.5, -10, 13, 12, 11, "pale", paint="hatch_head")
    C("head", "cheek_r", -5.2, 28, -11, 2.6, 4, 5, "pale2")
    C("head", "cheek_l", 5.2, 28, -11, 2.6, 4, 5, "pale2")
    C("head", "brow", 0, 40.6, -9.8, 11, 1.8, 10, "pale2")
    C("head", "eye_r", -4.2, 30.6, -11.4, 6.0, 9.2, 4.2, "dark", paint="hatch_eye")
    C("head", "eye_l", 4.2, 30.6, -11.4, 6.0, 9.2, 4.2, "dark", paint="hatch_eye")
    C("head", "muzzle", 0, 27.6, -13.4, 5.0, 6.4, 4.4, "pale", paint="hatch_head")
    bone("jaw", "head", (0, 28, -12))
    C("jaw", "mand_r", -2.0, 24.0, -15.6, 2.2, 4.4, 4.6, "mand", paint="hatch_jaw", rot=(0, 14, 0))
    C("jaw", "mand_l", 2.0, 24.0, -15.6, 2.2, 4.4, 4.6, "mand", paint="hatch_jaw", rot=(0, -14, 0))
    for sx, n in ((-1, "r"), (1, "l")):
        bone("antenna_" + n, "head", (sx * 2.4, 40, -6))
        C("antenna_" + n, "ant_a_" + n, sx * 2.6, 39.5, -8.0, 1.0, 6.0, 1.0, "dark", rot=(-16, 0, sx * -7))
        C("antenna_" + n, "ant_b_" + n, sx * 4.0, 44.8, -11.6, 1.0, 5.0, 1.0, "dark", rot=(-44, 0, sx * -12))
        C("antenna_" + n, "ant_c_" + n, sx * 4.6, 47.6, -15.6, 1.0, 3.0, 1.0, "dark", rot=(-74, 0, sx * -12))

    def wings(sd, n):
        bone("wing_" + n, "body", (-3.5, 29, 3), side=sd)
        Cx("wing_" + n, "wing_a_" + n, -3.5 - 20, 28.6, 2.8, 20, 0.8, 9, "wing", rot=(0, 0, -38), pv=(-3.5, 29, 4), side=sd)
        Cx("wing_" + n, "wing_b_" + n, -3.5 - 16, 28.0, 10.6, 16, 0.8, 6.4, "wing", rot=(0, 0, -30), pv=(-3.5, 29, 8), side=sd)
        Cx("wing_" + n, "wing_v_" + n, -3.5 - 20, 28.9, 2.6, 20, 0.5, 1.0, "dark", rot=(0, 0, -38), pv=(-3.5, 29, 4), side=sd)
    pair(wings)

    def arms(sd, n):
        bone("arm_" + n, "body", (-6.4, 27, -3), side=sd)
        Cx("arm_" + n, "arm_up_" + n, -6.4 - 1.2, 20, -4.2, 2.6, 7.6, 2.6, "pale2", side=sd)
        bone("arm_%s_low" % n, "arm_" + n, (-6.4, 20, -3), side=sd)
        Cx("arm_%s_low" % n, "arm_lo_" + n, -6.4 - 1.0, 14, -9.0, 2.0, 2.2, 7.6, "dark", side=sd)
        Cx("arm_%s_low" % n, "claw_" + n, -6.4 - 0.8, 13.4, -11.4, 1.6, 1.6, 2.6, "dark", rot=(20, 0, 0), side=sd)
    pair(arms)

    def legs(sd, n):
        for k, (z, ln) in enumerate(((-2.0, 17), (2.0, 16))):
            nm = "leg%s_%s" % ("" if k == 0 else "b", n)
            bone(nm, "body", (-3.4, 18, z), side=sd)
            Cx(nm, nm + "_up", -3.4 - 4.2, ln * 0.5, z - 1.1, 4.6, 2.2, 2.2, "pale2", rot=(0, 0, 0), side=sd)
            Cx(nm, nm + "_lo", -3.4 - 5.4, 0, z - 1.0, 2.0, ln * 0.55, 2.0, "dark", side=sd)
    pair(legs)
    top = 50.0
    pal = dict(m=dict(pale=("#f4f0e2", "chitin"), pale2=("#d9d3bd", "chitin"), dark=("#1a1618", "chitin"), mand=("#efe6d0", "chitin"),
                      stripe_a=("#f0ecdc", "stripe", "#16131a"), stripe_b=("#f0ecdc", "stripe", "#16131a"), tip=("#f6e6b0", "chitin"),
                      wing=("#cfdfe6", "membrane")), eye="#9ab0ff", glow="#fff2c0", glow_eye=False)
    c = Clips()
    P = 3.2
    s = lambda t, ph=0.0: math.sin(2 * math.pi * t / P + ph)
    f = lambda t, ph=0.0: math.sin(2 * math.pi * t / 0.25 + ph)
    c.pos("idle", "body", lambda t: (0, 1.4 * s(t), 0))
    c.rot("idle", "abdomen", lambda t: (7 * s(t, 0.5), 0, 0))
    c.rot("idle", "head", lambda t: (3 * s(t, 1), 6 * s(t), 0))
    c.rot("idle", "jaw", lambda t: (4 + 4 * max(0, s(t, 1.2)), 0, 0))
    c.rot("idle", "wing_r", lambda t: (0, 0, 20 * f(t)))
    c.rot("idle", "wing_l", lambda t: (0, 0, -20 * f(t)))
    for n in ("r", "l"):
        c.rot("idle", "antenna_" + n, lambda t: (5 * s(t, 0.3), 0, 0))
        c.rot("idle", "arm_" + n, lambda t: (-8 + 4 * s(t, 0.7), 0, 0))
    W = 1.0
    w = lambda t, ph=0.0: math.sin(2 * math.pi * t / W + ph)
    c.pos("walk", "body", lambda t: (0, 0.6 * abs(w(t)), 0))
    c.rot("walk", "abdomen", lambda t: (6 + 4 * w(t), 0, 0))
    c.rot("walk", "wing_r", lambda t: (0, 0, 24 * f(t)))
    c.rot("walk", "wing_l", lambda t: (0, 0, -24 * f(t)))
    c.rot("walk", "leg_r", lambda t: (25 * w(t), 0, 0))
    c.rot("walk", "leg_l", lambda t: (25 * w(t, math.pi), 0, 0))
    c.rot("walk", "legb_r", lambda t: (25 * w(t, math.pi), 0, 0))
    c.rot("walk", "legb_l", lambda t: (25 * w(t), 0, 0))
    c.rot("attack", "abdomen", rows=[(0, (0, 0, 0)), (0.15, (-50, 0, 0)), (0.3, (60, 0, 0)), (0.6, (0, 0, 0))])
    c.rot("attack", "body", rows=[(0, (0, 0, 0)), (0.15, (-12, 0, 0)), (0.3, (16, 0, 0)), (0.6, (0, 0, 0))])
    c.rot("attack", "jaw", rows=[(0, (0, 0, 0)), (0.15, (30, 0, 0)), (0.3, (0, 0, 0)), (0.6, (0, 0, 0))])
    c.rot("attack", "arm_r", rows=[(0, (0, 0, 0)), (0.15, (-60, 0, 0)), (0.3, (10, 0, 0)), (0.6, (0, 0, 0))])
    c.rot("attack", "arm_l", rows=[(0, (0, 0, 0)), (0.15, (-60, 0, 0)), (0.3, (10, 0, 0)), (0.6, (0, 0, 0))])
    c.rot("ability", "abdomen", rows=[(0, (0, 0, 0)), (0.5, (50, 0, 0)), (1.1, (50, 0, 0)), (1.6, (0, 0, 0))])
    c.rot("ability", "body", rows=[(0, (0, 0, 0)), (0.5, (-14, 0, 0)), (1.1, (-14, 0, 0)), (1.6, (0, 0, 0))])
    c.rot("ability", "wing_r", lambda t: (0, 0, 40 * f(t)))
    c.rot("ability", "wing_l", lambda t: (0, 0, -40 * f(t)))
    return pal, c, top, 3.8


# ---------------------------------------------------------------- Tsubame: a swallow, forked tail, long dark wings
@decal("tsu_face")
def _tsu_face(img, g, face, fw, fh, pal):
    if face == "north":
        for lo in (0.16, 0.66):
            box(img, fw, fh, lo, 0.34, lo + 0.18, 0.54, "#0c0c10")
            box(img, fw, fh, lo + 0.04, 0.36, lo + 0.08, 0.42, "#ffffff")
        line(img, fw, fh, 0.10, 0.28, 0.40, 0.30, "#0c0c10", 1)
        line(img, fw, fh, 0.90, 0.28, 0.60, 0.30, "#0c0c10", 1)
        box(img, fw, fh, 0.30, 0.70, 0.70, 1.0, "#b8483a")


def build_tsubame():
    reset()
    bone("root", None, (0, 0, 0))
    bone("body", "root", (0, 6, 0))
    C("body", "body", 0, 3, -6, 6, 5, 12, "navy")
    C("body", "chest", 0, 2.4, -7.2, 5, 4.2, 7, "white")
    C("body", "belly", 0, 2.0, -1, 4.4, 3.2, 7, "white")
    C("body", "back", 0, 7.8, -4, 5, 1.2, 8, "navy2")
    C("body", "rump", 0, 5.6, 5.0, 4.0, 2.6, 2.4, "navy2")
    bone("head", "body", (0, 8, -6))
    C("head", "head", 0, 5.4, -10.8, 5.2, 5.2, 5.2, "navy", paint="tsu_face")
    C("head", "cap", 0, 9.8, -10.4, 4.8, 0.9, 4.6, "navy2")
    C("head", "throat", 0, 4.4, -11.0, 4.6, 2.2, 4.4, "red")
    C("head", "beak", 0, 6.0, -13.4, 1.8, 1.4, 2.6, "dark")
    C("head", "beak_lo", 0, 5.3, -13.0, 1.4, 0.7, 2.0, "dark")

    def wings(sd, n):
        bone("wing_" + n, "body", (-3, 7.5, -3), side=sd)
        Cx("wing_" + n, "wing_a_" + n, -3 - 12, 7.2, -5.2, 12, 0.9, 7.6, "navy2", side=sd)
        Cx("wing_" + n, "cov_a_" + n, -3 - 10, 8.0, -4.4, 9, 0.5, 2.2, "white", side=sd)
        Cx("wing_" + n, "cov_b_" + n, -3 - 8, 8.0, -1.8, 7, 0.5, 2.0, "white", side=sd)
        Cx("wing_" + n, "under_" + n, -3 - 10, 6.9, -4.6, 9, 0.4, 6.4, "white", side=sd)
        bone("wing_%s_tip" % n, "wing_" + n, (-15, 7.5, -3), side=sd)
        for k, (z, ln, ry) in enumerate(((-4.6, 15, -9), (-2.8, 17, -3), (-1.0, 16, 3), (0.8, 13, 9))):
            Cx("wing_%s_tip" % n, "prim_%d_%s" % (k, n), -15 - ln, 7.2, z, ln, 0.9, 2.0, "navy" if k % 2 == 0 else "navy2", rot=(0, ry, 0), pv=(-15, 7.5, -3), side=sd)
    pair(wings)
    bone("tail", "body", (0, 6.5, 5))
    C("tail", "tail_c", 0, 5.4, 5, 3.2, 0.9, 5, "navy")

    def fork(sd, n):
        Cx("tail", "tail_a_" + n, -2.0, 5.5, 5, 2.4, 0.9, 7, "navy", side=sd)
        Cx("tail", "tail_b_" + n, -3.0, 5.5, 11, 1.5, 0.9, 11, "navy", rot=(0, 5, 0), pv=(-2.4, 5.5, 11), side=sd)
        Cx("tail", "tail_bar_" + n, -3.2, 5.7, 12.5, 1.9, 0.5, 1.4, "white", rot=(0, 5, 0), pv=(-2.4, 5.5, 11), side=sd)
        Cx("tail", "tail_bar2_" + n, -3.4, 5.7, 15.5, 1.9, 0.5, 1.4, "white", rot=(0, 5, 0), pv=(-2.4, 5.5, 11), side=sd)
        Cx("tail", "tail_bar3_" + n, -3.6, 5.7, 18.5, 1.9, 0.5, 1.4, "white", rot=(0, 5, 0), pv=(-2.4, 5.5, 11), side=sd)
    pair(fork)

    def feet(sd, n):
        bone("leg_" + n, "body", (-1.6, 3, 0), side=sd)
        Cx("leg_" + n, "foot_" + n, -1.6 - 0.5, 0.6, -0.5, 1.0, 2.4, 1.0, "dark", side=sd)
    pair(feet)
    top = 20.0
    pal = dict(m=dict(navy=("#26335a", "feather"), navy2=("#161e38", "feather"), white=("#f6f4ee", "feather"), red=("#b84a38", "feather"), dark=("#1a1618", "flat")), eye="#101010", glow="#ffffff")
    c = Clips()
    f = lambda t, ph=0.0: math.sin(2 * math.pi * t / 0.5 + ph)
    P = 3.2
    s = lambda t, ph=0.0: math.sin(2 * math.pi * t / P + ph)
    for clip, amp, per in (("idle", 28, 0.9), ("walk", 38, 0.5), ("ability", 50, 0.4)):
        g = lambda t, per=per: math.sin(2 * math.pi * t / per)
        c.rot(clip, "wing_r", lambda t, a=amp, g=g: (0, 0, -a * g(t)))
        c.rot(clip, "wing_l", lambda t, a=amp, g=g: (0, 0, a * g(t)))
        c.rot(clip, "wing_r_tip", lambda t, a=amp, g=g: (0, 0, -a * 0.6 * math.sin(2 * math.pi * t / per - 0.8)))
        c.rot(clip, "wing_l_tip", lambda t, a=amp, g=g: (0, 0, a * 0.6 * math.sin(2 * math.pi * t / per - 0.8)))
    c.pos("idle", "body", lambda t: (0, 0.5 * s(t), 0))
    c.rot("idle", "tail", lambda t: (4 * s(t), 0, 0))
    c.rot("walk", "tail", lambda t: (6, 0, 0))
    c.rot("walk", "body", lambda t: (-8, 0, 0))
    c.rot("attack", "body", rows=[(0, (0, 0, 0)), (0.2, (-30, 0, 0)), (0.4, (20, 0, 0)), (0.6, (0, 0, 0))])
    c.rot("attack", "head", rows=[(0, (0, 0, 0)), (0.2, (-12, 0, 0)), (0.4, (14, 0, 0)), (0.6, (0, 0, 0))])
    c.rot("ability", "body", rows=[(0, (0, 0, 0)), (0.8, (-24, 0, 0)), (1.6, (0, 0, 0))])
    return pal, c, top, 3.0


# ---------------------------------------------------------------- Solarin: the bean
@decal("solarin_face")
def _solarin_face(img, g, face, fw, fh, pal):
    if face != "north":
        return
    for lo, hi, mir in ((0.12, 0.42, False), (0.58, 0.88, True)):
        box(img, fw, fh, lo, 0.28, hi, 0.58, "#f4f0e0")
        box(img, fw, fh, lo + 0.08, 0.34, hi - 0.06, 0.58, "#14100c")
        box(img, fw, fh, lo + 0.12, 0.36, lo + 0.2, 0.44, "#ffffff")
        line(img, fw, fh, lo - 0.03, 0.30 + (0.0 if not mir else 0.04), hi + 0.03, 0.30 + (0.04 if not mir else 0.0), "#14100c", 2)
        o = lo - 0.05 if not mir else hi + 0.05
        line(img, fw, fh, o, 0.30, o + (-0.05 if not mir else 0.05), 0.42, "#14100c", 1)
    line(img, fw, fh, 0.38, 0.80, 0.62, 0.80, "#14100c", 1)
    line(img, fw, fh, 0.62, 0.80, 0.72, 0.72, "#14100c", 1)
    line(img, fw, fh, 0.38, 0.80, 0.28, 0.72, "#14100c", 1)


@decal("spots")
def _spots(img, g, face, fw, fh, pal):
    import random
    r = random.Random(fw * 131 + fh * 17 + len(face))
    for _ in range(max(2, fw * fh // 40)):
        x, y = r.randrange(fw), r.randrange(fh)
        img[y:y + 2, x:x + 2] *= 0.72


def build_solarin():
    reset()
    bone("root", None, (0, 0, 0))
    bone("body", "root", (0, 9, 0))
    for i, (y, h, w, d) in enumerate(((4.6, 2.6, 7.6, 7.6), (7.2, 2.6, 10.4, 10.0), (9.8, 3.0, 12.4, 11.6), (12.8, 3.2, 12.8, 12.0), (16.0, 2.8, 11.8, 11.0), (18.8, 2.6, 9.6, 9.0), (21.4, 1.8, 6.0, 6.0))):
        C("body", "pod_%d" % i, 0, y, -d / 2.0 + 0.4, w, h, d, "bean" if i % 2 == 0 else "bean2", paint="spots")
    C("body", "face", 0, 10.2, -5.9, 8.6, 6.8, 0.5, "bean2", paint="solarin_face")
    C("body", "band", 0, 16.4, -6.0, 11.0, 1.5, 1.4, "seam")
    C("body", "band_a", -4.6, 16.4, -4.2, 2.0, 1.5, 4.6, "seam", rot=(0, 24, 0))
    C("body", "band_b", 4.6, 16.4, -4.2, 2.0, 1.5, 4.6, "seam", rot=(0, -24, 0))
    C("body", "band_up", 0, 18.0, -5.4, 9.4, 1.1, 1.2, "seam")
    bone("sprout", "body", (0, 23, -0.5))
    C("sprout", "sprout_stem", 0, 23, -1.3, 1.2, 4.4, 1.2, "leaf")
    C("sprout", "leaf_r", -2.8, 26.4, -1.4, 3.6, 0.8, 2.2, "leaf", rot=(0, 0, 28))
    C("sprout", "leaf_l", 2.8, 26.4, -1.4, 3.6, 0.8, 2.2, "leaf", rot=(0, 0, -28))
    C("sprout", "leaf_t", 0, 27.2, -1.4, 1.4, 2.6, 2.2, "leaf")

    def arms(sd, n):
        bone("arm_" + n, "body", (-6.4, 13, 0), side=sd)
        Cx("arm_" + n, "arm_" + n, -6.4 - 0.9, 8.6, -0.7, 1.2, 5.4, 1.4, "pale", side=sd)
        Cx("arm_" + n, "hand_" + n, -6.4 - 1.1, 7.8, -0.9, 1.6, 1.2, 1.8, "pale", side=sd)
    pair(arms)

    def legs(sd, n):
        bone("leg_" + n, "root", (-2.6, 8, 0), side=sd)
        Cx("leg_" + n, "leg_" + n, -2.6 - 0.6, 1.2, -0.6, 1.2, 7.0, 1.2, "pale", side=sd)
        Cx("leg_" + n, "foot_" + n, -2.6 - 0.9, 0, -2.2, 1.8, 1.2, 3.4, "pale", side=sd)
    pair(legs)
    top = 30.0
    pal = dict(m=dict(bean=("#8a8a50", "plate"), bean2=("#9a9a5c", "plate"), seam=("#3a4228", "plate"), leaf=("#58b040", "plate"), pale=("#e8e2c8", "plate")), eye="#ffd860", glow="#ffe080")
    c = Clips()
    P = 2.4
    s = lambda t, ph=0.0: math.sin(2 * math.pi * t / P + ph)
    c.pos("idle", "body", lambda t: (0, 0.5 * abs(s(t)), 0))
    c.rot("idle", "sprout", lambda t: (0, 0, 8 * s(t)))
    c.rot("idle", "arm_r", lambda t: (0, 0, -4 + 4 * s(t)))
    c.rot("idle", "arm_l", lambda t: (0, 0, 4 - 4 * s(t)))
    W = 0.6
    w = lambda t, ph=0.0: math.sin(2 * math.pi * t / 1.0 + ph)
    c.pos("walk", "body", lambda t: (0, 0.9 * abs(w(t)), 0))
    c.rot("walk", "body", lambda t: (0, 0, 7 * w(t)))
    c.rot("walk", "leg_r", lambda t: (38 * w(t), 0, 0))
    c.rot("walk", "leg_l", lambda t: (38 * w(t, math.pi), 0, 0))
    c.rot("walk", "sprout", lambda t: (0, 0, -10 * w(t)))
    c.rot("attack", "body", rows=[(0, (0, 0, 0)), (0.2, (-16, 0, 0)), (0.4, (14, 0, 0)), (0.6, (0, 0, 0))])
    c.rot("attack", "arm_r", rows=[(0, (0, 0, 0)), (0.2, (-110, 0, 0)), (0.4, (20, 0, 0)), (0.6, (0, 0, 0))])
    c.rot("attack", "arm_l", rows=[(0, (0, 0, 0)), (0.2, (-110, 0, 0)), (0.4, (20, 0, 0)), (0.6, (0, 0, 0))])
    c.pos("ability", "body", rows=[(0, (0, 0, 0)), (0.5, (0, 2.5, 0)), (1.1, (0, 2.5, 0)), (1.6, (0, 0, 0))])
    c.rot("ability", "arm_r", rows=[(0, (0, 0, 0)), (0.5, (-20, 0, -150)), (1.1, (-20, 0, -150)), (1.6, (0, 0, 0))])
    c.rot("ability", "arm_l", rows=[(0, (0, 0, 0)), (0.5, (-20, 0, 150)), (1.1, (-20, 0, 150)), (1.6, (0, 0, 0))])
    c.rot("ability", "sprout", rows=[(0, (0, 0, 0)), (0.5, (0, 0, 14)), (1.1, (0, 0, -14)), (1.6, (0, 0, 0))])
    return pal, c, top, 2.0


# ---------------------------------------------------------------- Genbu: the archelon, a huge sea turtle that swims through the air
@decal("genbu_face")
def _genbu_face(img, g, face, fw, fh, pal):
    if face != "north":
        return
    for lo, hi, mir in ((0.10, 0.40, False), (0.60, 0.90, True)):
        box(img, fw, fh, lo, 0.36, hi, 0.60, "#e6ecd0")
        box(img, fw, fh, lo + 0.07, 0.40, hi - 0.07, 0.60, "#1a1c20")
        box(img, fw, fh, lo + 0.10, 0.42, lo + 0.17, 0.48, "#ffffff")
        # a heavy, drooping lid that slopes down to the outside: the sad look of the picture
        line(img, fw, fh, lo - 0.02, 0.36 + (0.0 if not mir else 0.07), hi + 0.02, 0.36 + (0.07 if not mir else 0.0), "#101214", 2)
        line(img, fw, fh, lo, 0.64, hi, 0.64, "#3a4a46", 1)
    line(img, fw, fh, 0.30, 0.20, 0.44, 0.28, "#101214", 1)
    line(img, fw, fh, 0.70, 0.20, 0.56, 0.28, "#101214", 1)
    box(img, fw, fh, 0.44, 0.74, 0.50, 0.80, "#101214")
    box(img, fw, fh, 0.52, 0.74, 0.58, 0.80, "#101214")


@decal("shell_pat")
def _shell_pat(img, g, face, fw, fh, pal):
    # dark hexagon plates inside lime borders, as on the shell in the picture
    if face not in ("up", "north", "south", "east", "west"):
        return
    lime = "#c4dc4c"
    line(img, fw, fh, 0.0, 0.05, 1.0, 0.05, lime, 1, g, 160)
    line(img, fw, fh, 0.0, 0.95, 1.0, 0.95, lime, 1, g, 160)
    if face == "up":
        for (cu, cv) in ((0.25, 0.28), (0.75, 0.28), (0.25, 0.72), (0.75, 0.72), (0.5, 0.5)):
            box(img, fw, fh, cu - 0.14, cv - 0.07, cu + 0.14, cv + 0.07, lime, g, 200, 0.9)
            box(img, fw, fh, cu - 0.09, cv - 0.04, cu + 0.09, cv + 0.04, "#142c2e")


@decal("scute")
def _scute(img, g, face, fw, fh, pal):
    if face == "up":
        box(img, fw, fh, 0.0, 0.0, 1.0, 1.0, "#c4dc4c", g, 190)
        box(img, fw, fh, 0.22, 0.22, 0.78, 0.78, "#1c3a36")


def build_genbu():
    reset()
    bone("root", None, (0, 0, 0))
    bone("body", "root", (0, 12, 0))
    C("body", "plastron", 0, 8, -13, 18, 3, 26, "belly")
    C("body", "rim", 0, 10.0, -15.4, 26.4, 1.3, 30.8, "shell_rim")
    C("body", "shell_low", 0, 11.2, -14.5, 25, 3.6, 29, "shell", paint="shell_pat")
    C("body", "shell_mid", 0, 14.6, -13.4, 22.6, 3.4, 26.8, "shell", paint="shell_pat")
    C("body", "shell_mid2", 0, 17.8, -11.8, 19, 3.0, 23.6, "shell", paint="shell_pat")
    C("body", "shell_top", 0, 20.6, -9.6, 14.4, 2.6, 19.2, "shell", paint="shell_pat")
    C("body", "shell_crown", 0, 23.0, -6.6, 8.6, 1.6, 13.0, "shell", paint="shell_pat")
    for i, (x, z) in enumerate(((-3.6, -1.5), (3.6, -1.5), (-3.6, 5.6), (3.6, 5.6))):
        C("body", "scute_%d" % i, x, 24.0, z - 2.0, 3.6, 0.7, 4.0, "shell_glow", paint="scute")
    C("body", "scute_c", 0, 24.0, -5.6, 5.0, 0.8, 5.0, "shell_glow", paint="scute")
    bone("neck", "body", (0, 14, -14))
    C("neck", "neck", 0, 10.4, -21, 7.4, 7.0, 9, "skin")
    C("neck", "neck_fold", 0, 10.0, -20, 8.0, 1.0, 7, "skin2")
    bone("head", "neck", (0, 13.5, -20))
    C("head", "head", 0, 9.6, -29.2, 9.6, 8.4, 9.2, "skin", paint="genbu_face")
    C("head", "brow", 0, 16.4, -28.8, 10.0, 1.4, 7.0, "skin2")
    C("head", "beak", 0, 9.2, -32.6, 5.0, 3.2, 3.8, "beak")
    C("head", "beak_tip", 0, 8.6, -33.6, 3.0, 2.0, 1.4, "beak")
    C("head", "beard", 0, 6.0, -31.6, 1.8, 3.2, 1.2, "beard", rot=(8, 0, 0))
    C("head", "beard_b", 0, 4.2, -31.8, 1.2, 2.2, 1.0, "beard", rot=(12, 0, 0))
    C("head", "crest", 0, 17.6, -27.6, 5, 1.2, 6, "shell_glow")

    def flippers(sd, n):
        bone("flip_f_" + n, "body", (-11, 14, -8), side=sd)
        Cx("flip_f_" + n, "ff_a_" + n, -11 - 14, 13.6, -12, 14, 1.8, 7.6, "skin", side=sd)
        Cx("flip_f_" + n, "ff_edge_" + n, -11 - 14, 14.2, -12.3, 14, 0.7, 0.9, "fin", side=sd)
        bone("flip_f_%s_tip" % n, "flip_f_" + n, (-25, 14, -9), side=sd)
        Cx("flip_f_%s_tip" % n, "ff_b_" + n, -25 - 14, 13.8, -11.4, 14, 1.4, 5.6, "skin", side=sd)
        Cx("flip_f_%s_tip" % n, "ff_b_edge_" + n, -25 - 14, 14.2, -11.8, 14, 0.7, 0.9, "fin", side=sd)
        Cx("flip_f_%s_tip" % n, "ff_b_point_" + n, -25 - 17, 13.9, -10.4, 3, 1.2, 2.6, "fin", side=sd)
        bone("flip_b_" + n, "body", (-9, 13, 11), side=sd)
        Cx("flip_b_" + n, "fb_" + n, -9 - 9, 12.6, 9, 9, 1.6, 6, "skin", side=sd)
        Cx("flip_b_" + n, "fb_edge_" + n, -9 - 9, 13.0, 8.7, 9, 0.7, 0.9, "fin", side=sd)
    pair(flippers)
    bone("tail", "body", (0, 12, 14))
    C("tail", "tail", 0, 11.4, 14, 3, 2, 7, "skin")
    top = 26.0
    pal = dict(m=dict(belly=("#c8c070", "plate"), shell_rim=("#b8d048", "plate"), shell=("#1c4244", "plate"), shell_glow=("#c4dc4c", "glow"),
                      skin=("#27474c", "scale"), skin2=("#1d383c", "scale"), fin=("#c4dc4c", "plate"), beak=("#d4dc50", "plate"), beard=("#7a8088", "hair")), eye="#e8f060", glow="#c4dc4c")
    c = Clips()
    P = 3.2
    s = lambda t, ph=0.0: math.sin(2 * math.pi * t / P + ph)
    c.pos("idle", "body", lambda t: (0, 1.0 * s(t), 0))
    c.rot("idle", "head", lambda t: (3 * s(t, 1), 6 * s(t), 0))
    c.rot("idle", "neck", lambda t: (2 * s(t, 0.6), 0, 0))
    c.rot("idle", "flip_f_r", lambda t: (0, 0, -14 * s(t)))
    c.rot("idle", "flip_f_l", lambda t: (0, 0, 14 * s(t)))
    c.rot("idle", "flip_f_r_tip", lambda t: (0, 0, -10 * s(t, -0.8)))
    c.rot("idle", "flip_f_l_tip", lambda t: (0, 0, 10 * s(t, -0.8)))
    c.rot("idle", "flip_b_r", lambda t: (0, 0, -8 * s(t, 0.5)))
    c.rot("idle", "flip_b_l", lambda t: (0, 0, 8 * s(t, 0.5)))
    c.rot("idle", "tail", lambda t: (0, 8 * s(t), 0))
    w = lambda t, ph=0.0: math.sin(2 * math.pi * t / 1.0 + ph)
    c.pos("walk", "body", lambda t: (0, 0.8 * w(t), 0))
    c.rot("walk", "flip_f_r", lambda t: (0, 0, -34 * w(t)))
    c.rot("walk", "flip_f_l", lambda t: (0, 0, 34 * w(t)))
    c.rot("walk", "flip_f_r_tip", lambda t: (0, 0, -22 * w(t, -0.9)))
    c.rot("walk", "flip_f_l_tip", lambda t: (0, 0, 22 * w(t, -0.9)))
    c.rot("walk", "flip_b_r", lambda t: (0, 0, -18 * w(t, 0.6)))
    c.rot("walk", "flip_b_l", lambda t: (0, 0, 18 * w(t, 0.6)))
    c.rot("attack", "neck", rows=[(0, (0, 0, 0)), (0.2, (-14, 0, 0)), (0.4, (18, 0, 0)), (0.6, (0, 0, 0))])
    c.rot("attack", "head", rows=[(0, (0, 0, 0)), (0.2, (-10, 0, 0)), (0.4, (14, 0, 0)), (0.6, (0, 0, 0))])
    c.rot("ability", "head", rows=[(0, (0, 0, 0)), (0.5, (-20, 0, 0)), (1.1, (-20, 0, 0)), (1.6, (0, 0, 0))])
    c.pos("ability", "body", rows=[(0, (0, 0, 0)), (0.5, (0, 1.5, 0)), (1.1, (0, 1.5, 0)), (1.6, (0, 0, 0))])
    c.rot("ability", "flip_f_r", rows=[(0, (0, 0, 0)), (0.5, (0, 0, -48)), (1.1, (0, 0, -48)), (1.6, (0, 0, 0))])
    c.rot("ability", "flip_f_l", rows=[(0, (0, 0, 0)), (0.5, (0, 0, 48)), (1.1, (0, 0, 48)), (1.6, (0, 0, 0))])
    return pal, c, top, 4.5
