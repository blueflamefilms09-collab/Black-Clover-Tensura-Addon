"""Summon models, part 2: Hatch (Insect A, the queen hornet), Tsubame (Bird A, the swallow), Solarin (Grass A, the bean), Genbu (Fish B, the archelon)."""
import math

from summon_lib import C, Cx, Clips, bone, pair, reset


def S(period, ph=0.0):
    return lambda t: math.sin(2 * math.pi * t / period + ph)


# ---------------------------------------------------------------- Hatch: an upright queen hornet, white head, big black eyes, striped abdomen
def build_hatch():
    reset()
    bone("root", None, (0, 0, 0))
    bone("body", "root", (0, 20, 0))
    # abdomen hangs behind and below the thorax and ends in a sting
    bone("abdomen", "body", (0, 24, 4))
    C("abdomen", "abd_a", 0, 14, 2, 9, 10, 9, "stripe_a")
    C("abdomen", "abd_b", 0, 7, 5, 12, 9, 12, "stripe_b")
    C("abdomen", "abd_c", 0, 0, 7, 10, 8, 10, "stripe_a")
    C("abdomen", "sting", 0, -3, 10, 2.0, 4, 2.0, "sting", rot=(18, 0, 0))
    C("body", "thorax", 0, 18, -4, 9, 10, 8, "pale")
    C("body", "thorax_back", 0, 20, 3, 8, 8, 4, "pale2")
    bone("head", "body", (0, 29, -3))
    C("head", "head", 0, 28, -9, 9, 8, 8, "pale", paint="round")
    C("head", "jaw_r", -2.4, 27, -11, 1.6, 2.6, 2.6, "dark")
    C("head", "jaw_l", 2.4, 27, -11, 1.6, 2.6, 2.6, "dark")
    for sx, n in ((-1, "r"), (1, "l")):
        bone("antenna_" + n, "head", (sx * 2.2, 36, -7))
        C("antenna_" + n, "ant_a_" + n, sx * 2.4, 35.5, -8, 1.0, 5.5, 1.0, "dark", rot=(-18, 0, sx * -8))
        C("antenna_" + n, "ant_b_" + n, sx * 3.6, 40.5, -11, 1.0, 4.5, 1.0, "dark", rot=(-38, 0, sx * -12))
    # wings
    def wings(sd, n):
        bone("wing_" + n, "body", (-3.5, 29, 3), side=sd)
        Cx("wing_" + n, "wing_a_" + n, -3.5 - 16, 28, 3, 16, 0.8, 7, "wing", rot=(0, 0, 8), pv=(-3.5, 29, 5), side=sd)
        Cx("wing_" + n, "wing_b_" + n, -3.5 - 22, 27, 8.5, 20, 0.8, 5, "wing", rot=(0, 0, 8), pv=(-3.5, 29, 8), side=sd)
    pair(wings)
    def arms(sd, n):
        bone("arm_" + n, "body", (-4.5, 27, -2), side=sd)
        Cx("arm_" + n, "arm_up_" + n, -4.5 - 1.6, 21, -3.2, 2.2, 7, 2.2, "pale", side=sd)
        bone("arm_%s_low" % n, "arm_" + n, (-4.5, 21, -2), side=sd)
        Cx("arm_%s_low" % n, "arm_lo_" + n, -4.5 - 1.6, 14, -5.6, 2.0, 2.0, 6, "dark", side=sd)
    pair(arms)
    def legs(sd, n):
        for k, (z, ln) in enumerate(((0, 14), (4, 13))):
            nm = "leg%s_%s" % ("" if k == 0 else "b", n)
            bone(nm, "body", (-3, 20, z), side=sd)
            Cx(nm, nm + "_up", -3 - 1.6, ln * 0.45, z - 1, 2.0, ln * 0.55 + 6, 2.0, "dark", side=sd)
            Cx(nm, nm + "_lo", -3 - 2.4, 0, z - 1.4, 2.0, ln * 0.45, 2.0, "dark", side=sd)
    pair(legs)
    top = 44.0
    pal = dict(m=dict(pale=("#f0ecd8", "plate"), pale2=("#d8d0b4", "plate"), dark=("#241c1c", "flat"), stripe_a=("#f2c230", "stripe", "#1c1618"), stripe_b=("#f2c230", "stripe", "#1c1618"),
                      sting=("#2a2020", "flat"), wing=("#cfe4ee", "scale")), eye="#c0f0ff", glow="#ffd060")
    c = Clips()
    P = 3.2
    s = lambda t, ph=0.0: math.sin(2 * math.pi * t / P + ph)
    f = lambda t, ph=0.0: math.sin(2 * math.pi * t / 0.25 + ph)
    c.pos("idle", "body", lambda t: (0, 1.2 * s(t), 0))
    c.rot("idle", "abdomen", lambda t: (6 * s(t, 0.5), 0, 0))
    c.rot("idle", "head", lambda t: (2 * s(t, 1), 5 * s(t), 0))
    c.rot("idle", "wing_r", lambda t: (0, 0, 22 * f(t)))
    c.rot("idle", "wing_l", lambda t: (0, 0, -22 * f(t)))
    for n in ("r", "l"):
        c.rot("idle", "antenna_" + n, lambda t: (4 * s(t, 0.3), 0, 0))
    W = 1.0
    w = lambda t, ph=0.0: math.sin(2 * math.pi * t / W + ph)
    c.pos("walk", "body", lambda t: (0, 0.6 * abs(w(t)), 0))
    c.rot("walk", "abdomen", lambda t: (8 + 4 * w(t), 0, 0))
    c.rot("walk", "wing_r", lambda t: (0, 0, 26 * f(t)))
    c.rot("walk", "wing_l", lambda t: (0, 0, -26 * f(t)))
    c.rot("walk", "leg_r", lambda t: (25 * w(t), 0, 0))
    c.rot("walk", "leg_l", lambda t: (25 * w(t, math.pi), 0, 0))
    c.rot("walk", "legb_r", lambda t: (25 * w(t, math.pi), 0, 0))
    c.rot("walk", "legb_l", lambda t: (25 * w(t), 0, 0))
    c.rot("attack", "abdomen", rows=[(0, (0, 0, 0)), (0.15, (-40, 0, 0)), (0.3, (50, 0, 0)), (0.6, (0, 0, 0))])
    c.rot("attack", "body", rows=[(0, (0, 0, 0)), (0.15, (-12, 0, 0)), (0.3, (16, 0, 0)), (0.6, (0, 0, 0))])
    c.rot("attack", "arm_r", rows=[(0, (0, 0, 0)), (0.15, (-60, 0, 0)), (0.3, (10, 0, 0)), (0.6, (0, 0, 0))])
    c.rot("attack", "arm_l", rows=[(0, (0, 0, 0)), (0.15, (-60, 0, 0)), (0.3, (10, 0, 0)), (0.6, (0, 0, 0))])
    c.rot("ability", "abdomen", rows=[(0, (0, 0, 0)), (0.5, (40, 0, 0)), (1.1, (40, 0, 0)), (1.6, (0, 0, 0))])
    c.rot("ability", "body", rows=[(0, (0, 0, 0)), (0.5, (-14, 0, 0)), (1.1, (-14, 0, 0)), (1.6, (0, 0, 0))])
    c.rot("ability", "wing_r", lambda t: (0, 0, 40 * f(t)))
    c.rot("ability", "wing_l", lambda t: (0, 0, -40 * f(t)))
    return pal, c, top, 3.5


# ---------------------------------------------------------------- Tsubame: a swallow, forked tail, long dark wings
def build_tsubame():
    reset()
    bone("root", None, (0, 0, 0))
    bone("body", "root", (0, 6, 0))
    C("body", "body", 0, 3, -6, 6, 5, 12, "navy")
    C("body", "chest", 0, 2.4, -7, 5, 4, 6, "white")
    C("body", "back", 0, 7.8, -4, 5, 1.2, 8, "navy2")
    bone("head", "body", (0, 8, -6))
    C("head", "head", 0, 5.5, -10.5, 5, 5, 5, "navy", paint="dot")
    C("head", "throat", 0, 4.6, -10.8, 4.4, 2, 4, "red")
    C("head", "beak", 0, 6.2, -13, 1.8, 1.4, 2.4, "dark")
    def wings(sd, n):
        bone("wing_" + n, "body", (-3, 7.5, -3), side=sd)
        Cx("wing_" + n, "wing_a_" + n, -3 - 12, 7.2, -5, 12, 0.9, 7, "navy2", side=sd)
        bone("wing_%s_tip" % n, "wing_" + n, (-15, 7.5, -3), side=sd)
        Cx("wing_%s_tip" % n, "wing_b_" + n, -15 - 14, 7.2, -4, 14, 0.9, 4.5, "navy", rot=(0, 0, 0), side=sd)
        Cx("wing_%s_tip" % n, "wing_t_" + n, -15 - 16, 7.2, -2.5, 6, 0.9, 2, "navy2", side=sd)
    pair(wings)
    bone("tail", "body", (0, 6.5, 5))
    def fork(sd, n):
        Cx("tail", "tail_a_" + n, -1.6, 5.5, 5, 2.2, 0.9, 8, "navy", side=sd)
        Cx("tail", "tail_b_" + n, -2.6, 5.5, 12, 1.4, 0.9, 9, "navy", rot=(0, 6, 0), pv=(-2.2, 5.5, 12), side=sd)
        Cx("tail", "tail_w_" + n, -2.6, 5.7, 14, 1.4, 0.6, 3, "white", side=sd)
    pair(fork)
    def feet(sd, n):
        bone("leg_" + n, "body", (-1.6, 3, 0), side=sd)
        Cx("leg_" + n, "foot_" + n, -1.6 - 0.5, 0.6, -0.5, 1.0, 2.4, 1.0, "dark", side=sd)
    pair(feet)
    top = 20.0
    pal = dict(m=dict(navy=("#1f2a45", "fur"), navy2=("#151d33", "fur"), white=("#f2f0ea", "fur"), red=("#b84a38", "flat"), dark=("#1a1618", "flat")), eye="#101010", glow="#ffffff")
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
def build_solarin():
    reset()
    bone("root", None, (0, 0, 0))
    bone("body", "root", (0, 9, 0))
    C("body", "pod_low", 0, 6, -4.5, 9, 5, 9, "bean")
    C("body", "pod_mid", 0, 10.5, -5.5, 11, 8, 11, "bean2")
    C("body", "pod_top", 0, 18, -4.5, 9, 4, 9, "bean")
    C("body", "seam", 0, 7, -5.8, 1.4, 14, 1.0, "seam")
    C("body", "face", 0, 12, -6.0, 8, 4, 0.6, "bean2", paint="slit")
    C("body", "brow", 0, 15.2, -6.2, 8.5, 1.0, 0.8, "seam")
    bone("sprout", "body", (0, 22, 0))
    C("sprout", "sprout_stem", 0, 22, -0.8, 1.2, 4, 1.2, "leaf")
    C("sprout", "leaf_r", -2.6, 25, -1, 3.2, 0.8, 2, "leaf", rot=(0, 0, 25))
    C("sprout", "leaf_l", 2.6, 25, -1, 3.2, 0.8, 2, "leaf", rot=(0, 0, -25))
    def arms(sd, n):
        bone("arm_" + n, "body", (-5.2, 13, 0), side=sd)
        Cx("arm_" + n, "arm_" + n, -5.2 - 2.4, 9, -0.6, 1.2, 5, 1.2, "seam", side=sd)
    pair(arms)
    def legs(sd, n):
        bone("leg_" + n, "root", (-2.4, 8, 0), side=sd)
        Cx("leg_" + n, "leg_" + n, -2.4 - 0.6, 1.4, -0.6, 1.2, 6.6, 1.2, "seam", side=sd)
        Cx("leg_" + n, "foot_" + n, -2.4 - 0.9, 0, -1.8, 1.8, 1.4, 3.0, "seam", side=sd)
    pair(legs)
    top = 30.0
    pal = dict(m=dict(bean=("#6c8a52", "plate"), bean2=("#7fa05e", "plate"), seam=("#3e5230", "flat"), leaf=("#58b040", "flat")), eye="#ffd860", glow="#ffe080")
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
def build_genbu():
    reset()
    bone("root", None, (0, 0, 0))
    bone("body", "root", (0, 12, 0))
    C("body", "plastron", 0, 8, -13, 18, 3, 26, "belly")
    C("body", "shell_low", 0, 10, -14, 24, 5, 28, "shell_rim")
    C("body", "shell_mid", 0, 14.5, -12, 20, 5, 24, "shell")
    C("body", "shell_top", 0, 19, -9, 14, 3, 18, "shell")
    for i, (x, z) in enumerate(((-4.5, -7), (4.5, -7), (-4.5, 0), (4.5, 0), (0, 7))):
        C("body", "scute_%d" % i, x, 21.5, z - 2.5, 5, 1.6, 5, "shell_glow")
    bone("neck", "body", (0, 14, -13))
    C("neck", "neck", 0, 11, -19, 7, 6, 8, "skin", rot=(0, 0, 0))
    bone("head", "neck", (0, 13, -19))
    C("head", "head", 0, 10, -26, 8, 7, 8, "skin", paint="round")
    C("head", "beak", 0, 10.2, -29, 5, 3.4, 3.2, "beak")
    C("head", "crest", 0, 17, -25, 5, 1.6, 6, "shell_glow")
    def flippers(sd, n):
        bone("flip_f_" + n, "body", (-11, 14, -8), side=sd)
        Cx("flip_f_" + n, "ff_a_" + n, -11 - 14, 13.6, -12, 14, 1.8, 7, "skin", side=sd)
        bone("flip_f_%s_tip" % n, "flip_f_" + n, (-25, 14, -9), side=sd)
        Cx("flip_f_%s_tip" % n, "ff_b_" + n, -25 - 13, 13.8, -11, 13, 1.4, 5, "fin", side=sd)
        bone("flip_b_" + n, "body", (-9, 13, 11), side=sd)
        Cx("flip_b_" + n, "fb_" + n, -9 - 9, 12.6, 9, 9, 1.6, 6, "fin", side=sd)
    pair(flippers)
    bone("tail", "body", (0, 12, 14))
    C("tail", "tail", 0, 11.4, 14, 3, 2, 7, "skin")
    top = 26.0
    pal = dict(m=dict(belly=("#c8c070", "plate"), shell_rim=("#d8b848", "plate"), shell=("#2c5a44", "shell", "#98f0a0"), shell_glow=("#a8f070", "glow"),
                      skin=("#2f6a50", "scale"), fin=("#b8d060", "scale"), beak=("#6a7a40", "flat")), eye="#e8f060", glow="#a8f070")
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
