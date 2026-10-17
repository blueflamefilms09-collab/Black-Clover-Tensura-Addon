"""Why: the clip recipes themselves (see clips.py for the machinery): one function per clip name, written against the character's measured body, never against fixed
numbers of one model. The poses use the sign rules of rig.py (positive x swings a hanging limb forward, negative x folds a knee and leans forward ...). A weapon swing is a
table of where the wrist goes and where the weapon points at each moment (elevation, azimuth, reach, phi, tilt: see ArmIK.target and anim.dir_from); the body, legs
and off hand support it.
"""
import math

import numpy as np

from .anim import Clip, GRID, SIN, COS, TAU
from .clips import Ctx, follow, HOLD, ONE_SHOT

S_, C_ = np.sin, np.cos
# Every key (0.0, -77, 41, 0.99, ...) below is the wrist of a relaxed hanging arm (elevation, azimuth, reach; clips.HANG): the arm the idle clip hangs by its forward
# kinematics, so a clip that starts or ends on a hanging arm starts and ends on the idle's pose.


def zeros(c):
    return np.zeros(c.n + 1)


def stack(*cols):
    return np.stack(cols, 1)


def mirror_keys(keys, flip):
    return keys


def pose_keys(c, bone, kind, rows, T):
    """rows: one (x, y, z) per time in T."""
    c.keys(bone, kind, list(zip(T, rows)))


# ------------------------------------------------------------------------------------------------------------------------ idle
def c_idle(ctx):
    c = Clip("idle", 3.2, True)
    th = c.theta()
    z = zeros(c)
    st = ctx.idle_style
    d = ctx.d
    c.add("body", "pos", stack(z, 0.22 * S_(th), z))
    c.add("body", "rot", stack(0.5 * S_(th + 0.6), 1.2 * S_(th + 0.3), 0.4 * S_(th)))
    c.add("body", "scale", stack(1 + z, 1 + 0.006 * S_(th), 1 + 0.010 * S_(th)))
    c.add("head", "rot", stack(1.0 + 1.5 * S_(th + 0.7), 3.0 * S_(th + 1.0), 0.8 * S_(th + 0.2)))
    legs = {"relaxed": (0, 0, -2, -2), "guard": (10, -8, -12, -10), "proud": (2, -2, -2, -2), "slouch": (3, -3, -4, -4), "hover": (6, 2, -16, -12)}[st]
    for side, i0, i1 in (("l", 0, 2), ("r", 1, 3)):
        c.add("leg_" + side, "rot", stack(z + legs[i0], z, z))
        c.add("shin_" + side, "rot", stack(z + legs[i1], z, z))
    if st == "guard":
        c.add("body", "rot", stack(z - 6, z, z))
        c.add("head", "rot", stack(z + 4, z, z))
    elif st == "proud":
        c.add("body", "rot", stack(z + 2.5, z, z))
        c.add("head", "rot", stack(z - 1.5, z, z))
    elif st == "slouch":
        c.add("body", "rot", stack(z - 7, z, z))
        c.add("head", "rot", stack(z - 9, z, z))
    elif st == "hover":
        c.add("root", "pos", stack(z, 1.2 * S_(th), z))
    holder_done = False
    if ctx.holder:
        ctx.rest_hold(c)
        holder_done = True
    for side in ("l", "r"):
        if holder_done and side == ctx.holder:
            continue
        if ctx.coupled and side != ctx.holder:
            continue
        if st == "guard":
            ctx.free_arm(c, side, [(0.0, 5, 24, 0.62, None, 0), (1.6, 8, 26, 0.64, None, 0), (3.2, 5, 24, 0.62, None, 0)])
        elif st == "proud":
            ctx.hanging(c, side, 0.6, bend=6.0, out=14.0, ph=0.4 if side == "r" else 0.0)
        elif ctx.shield_side == side:
            ctx.free_arm(c, side, [(0.0, -36, 30, 0.74, None, 0), (3.2, -36, 30, 0.74, None, 0)])
        else:
            ctx.hanging(c, side, 1.0, ph=0.7 if side == "r" else 0.0)
    follow(ctx, c, wind=8.0 if st == "hover" else 0.0, osc=1.0)
    ctx.finish(c)
    return c


# ------------------------------------------------------------------------------------------------------------------------ walk and run
def locomotion(ctx, run):
    c = Clip("run" if run else "walk", 0.62 if run else 1.1, True)
    th = c.theta()
    z = zeros(c)
    d = ctx.d
    build = d.build
    scale = {"huge": 0.82, "small": 1.1, "child": 1.15, "lanky": 0.95}.get(build, 1.0)
    A = (46.0 if run else 24.0) * scale * (0.72 if ctx.long else 1.0)
    K = 80.0 if run else 34.0
    B = 52.0 if run else 20.0
    for side, sg, ph in (("l", 1, 0.0), ("r", -1, math.pi)):
        s = S_(th + ph)
        thigh = A * s
        flex = np.maximum(0.0, C_(th + ph)) * K
        c.add("leg_" + side, "rot", stack(thigh, z, sg * (2.0 if run else 1.2) + z))
        c.add("shin_" + side, "rot", stack(-(flex + 3.0), z, z))
        k_s = 0.35 if ctx.long else 0.55
        c.add("skirt_" + side, "rot", stack(thigh * k_s, z, sg * 0.8 + z))
        if ctx.holder == side:
            continue
        if ctx.coupled and side != ctx.holder:
            continue
        if ctx.shield_side == side:
            ctx.free_arm(c, side, [(0.0, -36, 30, 0.74, None, 0), (c.length, -36, 30, 0.74, None, 0)])
            continue
        swing = -B * s
        c.add("arm_" + side, "rot", stack(swing + (6 if run else 2), z, sg * ((12.0 if run else 7.0) + 0 * z)))
        bend = (62.0 + 24.0 * np.maximum(0.0, s)) if run else (10.0 + 12.0 * np.maximum(0.0, s))
        c.add("forearm_" + side, "rot", stack(bend, z, z))
    lean = -13.0 if run else -2.0
    c.add("body", "pos", stack(z, (0.9 if run else 0.35) * C_(2 * th), z))
    c.add("body", "rot", stack(lean + (1.6 if run else 0.5) * S_(2 * th), (6.0 if run else 3.0) * S_(th), (1.6 if run else 0.8) * S_(th)))
    c.add("head", "rot", stack((9.0 if run else 2.0) + 1.2 * S_(2 * th + 1.6), (-4.5 if run else -2.4) * S_(th), z))
    if ctx.holder:
        bob = (4.0 if run else 1.5) * C_(2 * th)
        if run:
            ctx.rest_hold(c, bob=bob, sway=1.6)
        else:
            ctx.rest_hold(c, bob=bob, sway=1.2)
    follow(ctx, c, wind=26.0 if run else 6.0, osc=1.8 if run else 1.0, flutter=1.8 if run else 1.0, wing_flap=14.0 if run else 6.0, wing_fold=12.0 if run else 0.0)
    ctx.finish(c)
    return c


# ------------------------------------------------------------------------------------------------------------------------ attacks
def _legs(c, T, lead, rows, flip):
    """rows = {'f': [(thigh, knee)...], 'b': [...]} for the front and back leg; lead = the side of the front leg."""
    front, back = ("l", "r") if lead == "l" else ("r", "l")
    for key, side in (("f", front), ("b", back)):
        c.keys("leg_" + side, "rot", [(t, (v[0], 0.0, 0.0)) for t, v in zip(T, rows[key])])
        c.keys("shin_" + side, "rot", [(t, (v[1], 0.0, 0.0)) for t, v in zip(T, rows[key])])


def c_attack(ctx):
    style = ctx.attack_style
    c = Clip("attack", 0.9, False)
    z = zeros(c)
    hold = ctx.hold()
    rest = ctx.hold()
    flip = -1.0 if ctx.holder == "l" else 1.0                       # a left-hand weapon turns the body the other way
    lead = "r" if ctx.holder == "l" else "l"                         # the foot opposite the weapon hand leads
    free = "l" if ctx.holder in (None, "r") else "r"
    if style == "none":
        c.keys("body", "rot", [(0, (0, 0, 0)), (0.2, (-4, 6 * flip, 0)), (0.9, (0, 0, 0))])
        if ctx.holder:
            ctx.rest_hold(c)
        else:
            for sd in ("l", "r"):
                ctx.hanging(c, sd)
        follow(ctx, c)
        ctx.finish(c)
        return c
    if style == "slash":
        T = (0.0, 0.22, 0.38, 0.54, 0.9)
        hk = [(0.0,) + rest, (0.22, 56, 98, 0.80, 150, -8 * flip), (0.38, -6, 18, 0.97, 8, 0), (0.54, -34, -14, 0.93, -40, 16 * flip), (0.9,) + rest]
        body = [(0, 0, 0), (5, 16 * flip, 0), (-13, -16 * flip, 0), (-8, -10 * flip, 0), (0, 0, 0)]
        bpos = [(0, 0, 0), (0, 0.3, 0.5), (0, -0.9, -2.4), (0, -0.4, -1.2), (0, 0, 0)]
        legs = {"f": [(0, -2), (-6, -4), (24, -20), (14, -10), (0, -2)], "b": [(0, -2), (8, -10), (-18, -10), (-8, -6), (0, -2)]}
        off = [(0.0, -77, 41, 0.99, None, 0), (0.22, -20, 60, 0.6, None, 0), (0.38, 6, 84, 0.7, None, 0), (0.54, -10, 60, 0.7, None, 0), (0.9, -77, 41, 0.99, None, 0)]
        head = [(0, 0, 0), (-3, 8 * flip, 0), (3, -10 * flip, 0), (0, -5 * flip, 0), (0, 0, 0)]
    elif style == "overhead":
        T = (0.0, 0.26, 0.40, 0.56, 0.9)
        hk = [(0.0,) + rest, (0.26, 76, 62, 0.86, 168, 0), (0.40, -24, 6, 0.97, -34, 0), (0.56, -46, 6, 0.90, -58, 0), (0.9,) + rest]
        body = [(0, 0, 0), (12, 6 * flip, 0), (-22, -4 * flip, 0), (-14, -3 * flip, 0), (0, 0, 0)]
        bpos = [(0, 0, 0), (0, 0.8, 0.8), (0, -1.6, -2.6), (0, -1.0, -1.6), (0, 0, 0)]
        legs = {"f": [(0, -2), (-6, -4), (26, -22), (16, -12), (0, -2)], "b": [(0, -2), (10, -10), (-20, -12), (-10, -6), (0, -2)]}
        off = [(0.0, -77, 41, 0.99, None, 0), (0.26, 60, 40, 0.8, None, 0), (0.40, -20, 12, 0.9, None, 0), (0.56, -40, 14, 0.85, None, 0), (0.9, -77, 41, 0.99, None, 0)]
        head = [(0, 0, 0), (-8, 0, 0), (6, 0, 0), (2, 0, 0), (0, 0, 0)]
    elif style == "thrust":
        T = (0.0, 0.20, 0.36, 0.50, 0.9)
        hk = [(0.0,) + rest, (0.20, -24, 92, 0.50, 4, 0), (0.36, 4, 6, 0.98, 2, 0), (0.50, -4, 12, 0.90, 10, 0), (0.9,) + rest]
        body = [(0, 0, 0), (3, 16 * flip, 0), (-14, -10 * flip, 0), (-8, -5 * flip, 0), (0, 0, 0)]
        bpos = [(0, 0, 0), (0, 0.2, 1.0), (0, -1.0, -3.2), (0, -0.5, -1.6), (0, 0, 0)]
        legs = {"f": [(0, -2), (-4, -4), (30, -26), (16, -12), (0, -2)], "b": [(0, -2), (8, -10), (-22, -8), (-10, -6), (0, -2)]}
        off = [(0.0, -77, 41, 0.99, None, 0), (0.20, -10, 40, 0.7, None, 0), (0.36, 4, 20, 0.9, None, 0), (0.5, -10, 22, 0.8, None, 0), (0.9, -77, 41, 0.99, None, 0)]
        head = [(0, 0, 0), (-1, 6 * flip, 0), (2, -4 * flip, 0), (0, 0, 0), (0, 0, 0)]
    elif style == "smash":
        T = (0.0, 0.28, 0.42, 0.58, 0.9)
        hk = [(0.0,) + rest, (0.28, 84, 40, 0.90, 102, 0), (0.42, -52, 8, 0.96, -70, 0), (0.58, -60, 10, 0.92, -72, 0), (0.9,) + rest]
        body = [(0, 0, 0), (14, 8 * flip, 0), (-26, -6 * flip, 0), (-18, -3 * flip, 0), (0, 0, 0)]
        bpos = [(0, 0, 0), (0, 0.8, 0.9), (0, -2.0, -3.2), (0, -1.4, -2.0), (0, 0, 0)]
        legs = {"f": [(0, -2), (-6, -4), (28, -24), (20, -16), (0, -2)], "b": [(0, -2), (12, -12), (-24, -14), (-12, -8), (0, -2)]}
        off = [(0.0, -77, 41, 0.99, None, 0), (0.28, 30, 70, 0.8, None, 0), (0.42, -30, 70, 0.85, None, 0), (0.58, -40, 60, 0.85, None, 0), (0.9, -77, 41, 0.99, None, 0)]
        head = [(0, 0, 0), (-8, 0, 0), (8, 0, 0), (3, 0, 0), (0, 0, 0)]
    elif style == "stab":
        T = (0.0, 0.18, 0.34, 0.50, 0.9)
        hk = [(0.0,) + rest, (0.18, -24, 62, 0.50, 24, 0), (0.34, -10, 10, 0.95, 4, 0), (0.50, -22, 34, 0.80, 22, 0), (0.9,) + rest]
        body = [(0, 0, 0), (2, 12 * flip, 0), (-12, -8 * flip, 0), (-5, -3 * flip, 0), (0, 0, 0)]
        bpos = [(0, 0, 0), (0, 0, 0.6), (0, -0.8, -2.4), (0, -0.3, -1.0), (0, 0, 0)]
        legs = {"f": [(0, -2), (-4, -4), (26, -20), (12, -8), (0, -2)], "b": [(0, -2), (8, -8), (-18, -8), (-8, -4), (0, -2)]}
        off = [(0.0, -77, 41, 0.99, None, 0), (0.18, 0, 30, 0.7, None, 0), (0.34, 10, 40, 0.7, None, 0), (0.5, -10, 30, 0.7, None, 0), (0.9, -77, 41, 0.99, None, 0)]
        head = [(0, 0, 0), (0, 4 * flip, 0), (2, -5 * flip, 0), (0, 0, 0), (0, 0, 0)]
    elif style == "sweep":
        T = (0.0, 0.28, 0.42, 0.58, 0.9)
        hk = [(0.0,) + rest, (0.28, 14, 118, 0.82, 52, -50 * flip), (0.42, -4, 24, 0.97, 38, 8 * flip), (0.58, -12, -36, 0.92, 34, 54 * flip), (0.9,) + rest]
        body = [(0, 0, 0), (2, 26 * flip, 0), (-8, -4 * flip, 0), (-6, -30 * flip, 0), (0, 0, 0)]
        bpos = [(0, 0, 0), (0, 0.2, 0.6), (0, -0.6, -1.4), (0, -0.4, -0.8), (0, 0, 0)]
        legs = {"f": [(0, -2), (-6, -4), (20, -16), (14, -10), (0, -2)], "b": [(0, -2), (8, -8), (-14, -8), (-8, -4), (0, -2)]}
        off = [(0.0, -77, 41, 0.99, None, 0), (0.28, 0, 70, 0.8, None, 0), (0.42, 0, 40, 0.9, None, 0), (0.58, -10, 20, 0.85, None, 0), (0.9, -77, 41, 0.99, None, 0)]
        head = [(0, 0, 0), (0, 12 * flip, 0), (0, -4 * flip, 0), (0, -16 * flip, 0), (0, 0, 0)]
    elif style == "cast_jab":
        T = (0.0, 0.22, 0.40, 0.58, 0.9)
        hk = [(0.0,) + rest, (0.22, 32, 30, 0.82, 60, 0), (0.40, 10, 4, 0.96, 18, 0), (0.58, 12, 8, 0.92, 26, 0), (0.9,) + rest]
        body = [(0, 0, 0), (4, 4 * flip, 0), (-8, -3 * flip, 0), (-4, -1 * flip, 0), (0, 0, 0)]
        bpos = [(0, 0, 0), (0, 0.3, 0.3), (0, -0.4, -1.0), (0, -0.2, -0.5), (0, 0, 0)]
        legs = {"f": [(0, -2), (-3, -3), (12, -10), (6, -6), (0, -2)], "b": [(0, -2), (4, -4), (-8, -6), (-4, -3), (0, -2)]}
        off = [(0.0, -77, 41, 0.99, None, 0), (0.22, 30, 36, 0.8, None, 0), (0.40, 14, 22, 0.9, None, 0), (0.58, 10, 26, 0.85, None, 0), (0.9, -77, 41, 0.99, None, 0)]
        head = [(0, 0, 0), (-3, 0, 0), (3, 0, 0), (1, 0, 0), (0, 0, 0)]
    elif style == "shoot":
        T = (0.0, 0.30, 0.46, 0.58, 0.9)
        hk = [(0.0,) + rest, (0.30,) + rest, (0.46,) + rest, (0.58,) + rest, (0.9,) + rest]
        body = [(0, 0, 0), (1, 12 * flip, 0), (1, 12 * flip, 0), (-2, 6 * flip, 0), (0, 0, 0)]
        bpos = [(0, 0, 0)] * 5
        legs = {"f": [(0, -2), (2, -4), (4, -6), (4, -6), (0, -2)], "b": [(0, -2), (-2, -4), (-4, -6), (-4, -6), (0, -2)]}
        off = None
        head = [(0, 0, 0), (0, 10 * flip, 0), (0, 10 * flip, 0), (0, 4 * flip, 0), (0, 0, 0)]
    else:                                                              # punch
        T = (0.0, 0.20, 0.36, 0.52, 0.9)
        hk = None
        body = [(0, 0, 0), (2, 20 * flip, 0), (-12, -16 * flip, 0), (-6, -8 * flip, 0), (0, 0, 0)]
        bpos = [(0, 0, 0), (0, 0.2, 0.6), (0, -0.7, -2.2), (0, -0.3, -1.0), (0, 0, 0)]
        legs = {"f": [(0, -2), (-4, -4), (22, -18), (12, -8), (0, -2)], "b": [(0, -2), (6, -8), (-16, -8), (-8, -4), (0, -2)]}
        off = [(0.0, -77, 41, 0.99, None, 0), (0.2, 0, 26, 0.6, None, 0), (0.36, 4, 20, 0.62, None, 0), (0.52, 2, 24, 0.6, None, 0), (0.9, -77, 41, 0.99, None, 0)]
        head = [(0, 0, 0), (0, 8 * flip, 0), (1, -6 * flip, 0), (0, 0, 0), (0, 0, 0)]
    c.keys("body", "rot", list(zip(T, body)))
    c.keys("body", "pos", list(zip(T, bpos)))
    c.keys("head", "rot", list(zip(T, head)))
    _legs(c, T, lead, legs, flip)
    if ctx.holder:
        if style == "shoot":
            ctx.rest_hold(c)
        elif hk:
            ctx.arm_track(c, ctx.holder, hk, holder=True)
        else:
            ctx.rest_hold(c)
    other = "l" if ctx.holder == "r" else "r"
    if ctx.coupled and off is not None and style != "shoot":              # the off hand stays on the shaft while it can reach it and swings free when it cannot
        ctx.release_off_hand(c, off)
    if ctx.holder is None:                                               # bare hands: the right hand punches, the left guards
        punch = [(0.0, -77, 41, 0.99, None, 0), (0.20, -30, 76, 0.5, None, 0), (0.36, 4, 4, 0.98, None, 0), (0.52, -6, 12, 0.8, None, 0), (0.9, -77, 41, 0.99, None, 0)]
        ctx.free_arm(c, "r" if flip > 0 else "l", punch)
        ctx.free_arm(c, "l" if flip > 0 else "r", off)
    elif not ctx.coupled and off is not None and ctx.shield_side != other:
        ctx.free_arm(c, other, off)
    elif ctx.shield_side == other:
        ctx.free_arm(c, other, [(0.0, -36, 30, 0.74, None, 0), (0.36, -10, 24, 0.8, None, 0), (0.9, -36, 30, 0.74, None, 0)])
    if style == "shoot":
        _shoot_hand(ctx, c, T)
    follow(ctx, c, wind=4.0, osc=1.4, flutter=1.6, wing_flap=10.0)
    ctx.finish(c)
    return c


def _shoot_hand(ctx, c, T):
    """The string hand of a bow: it hangs at its side, is drawn up to the cheek (0.3 s), holds and lets go (0.46 s)."""
    side = "r" if ctx.holder == "l" else "l"
    n = c.n + 1
    draw = np.array([float(np.interp(float(t), [0.0, 0.30, 0.46, 0.58, 0.9], [0.0, 1.0, 1.0, 0.0, 0.0])) for t in c.ts])
    q, _ = ctx.arm[side].solve_track(ctx.string_targets(c, draw))
    c.put("arm_" + side, "rot", q[:, :3])
    c.put("forearm_" + side, "rot", np.stack([q[:, 3], np.zeros(n), np.zeros(n)], 1))
    ctx.ik_arms.add((c.name, side))


# ------------------------------------------------------------------------------------------------------------------------ cast
def c_cast(ctx):
    c = Clip("cast", 1.2, True)
    th = c.theta()
    z = zeros(c)
    style = ctx.cast_style
    c.add("body", "pos", stack(z, 0.35 + 0.35 * S_(th), z))
    c.add("body", "rot", stack(2.0 + 1.0 * S_(th + 0.5), 1.5 * S_(th), z))
    c.add("head", "rot", stack(-2.0 + 1.5 * S_(th + 0.4), 2.0 * S_(th), z))
    for side, sg in (("l", 1), ("r", -1)):
        c.add("leg_" + side, "rot", stack(z + (3 if side == "l" else -2), z, z))
        c.add("shin_" + side, "rot", stack(z - 3, z, z))
    free = "l" if ctx.holder in (None, "r") else "r"
    ts = [0.0, 0.3, 0.6, 0.9, 1.2]
    if style == "gesture":
        # talking: one hand gestures, the head nods, the body sways
        c.add("head", "rot", stack(2.5 * S_(2 * th), 2.5 * S_(th + 0.4), z))
        g = [(0.0, -34, 30, 0.78, None, 0), (0.3, -8, 34, 0.80, None, 0), (0.6, -22, 26, 0.74, None, 0), (0.9, 6, 36, 0.76, None, 0), (1.2, -34, 30, 0.78, None, 0)]
        gs = "l" if (ctx.holder == "r" or ctx.holder is None) else "r"
        gs = "r" if (ctx.holder is None) else gs
        ctx.free_arm(c, gs, g)
        if ctx.holder:
            ctx.rest_hold(c)
        other = "l" if gs == "r" else "r"
        if ctx.holder != other and not (ctx.coupled and other != ctx.holder):
            if ctx.shield_side == other:
                ctx.free_arm(c, other, [(0.0, -36, 30, 0.74, None, 0), (1.2, -36, 30, 0.74, None, 0)])
            else:
                ctx.hanging(c, other, 0.8, ph=1.0)
    elif style == "pray":
        c.add("head", "rot", stack(z - 10, z, z))
        for sd in ("l", "r"):
            ctx.free_arm(c, sd, [(0.0, -2, 14, 0.52, None, 0), (0.6, 2, 14, 0.54, None, 0), (1.2, -2, 14, 0.52, None, 0)])
    elif style == "book" and ctx.kind == "grimoire":
        ctx.arm_track(c, ctx.holder, [(0.0, -2, 14, 0.82, 78, 0), (0.6, 4, 14, 0.84, 82, 0), (1.2, -2, 14, 0.82, 78, 0)], holder=True)
        sw = [(t, 14 + 12 * math.sin(TAU * t / 1.2), 18 + 16 * math.cos(TAU * t / 1.2), 0.78, None, 0) for t in (0.0, 0.2, 0.4, 0.6, 0.8, 1.0, 1.2)]
        ctx.free_arm(c, "r" if ctx.holder == "l" else "l", sw)
    else:                                                               # magic: the weapon and the free hand raised, the power gathering
        if ctx.holder:
            keys = [(t, 28 + 3 * math.sin(TAU * t / 1.2), 22, 0.88, 62 + 4 * math.sin(TAU * t / 1.2 + 1), 0) for t in ts]
            ctx.arm_track(c, ctx.holder, keys, holder=True)
        free_keys = [(t, 30 + 3 * math.sin(TAU * t / 1.2 + 1.4), 34, 0.86, None, 0) for t in ts]
        if not ctx.coupled:
            ctx.free_arm(c, free if ctx.holder else "r", free_keys)
            if not ctx.holder:
                ctx.free_arm(c, "l", free_keys)
    follow(ctx, c, wind=4.0, osc=1.4, flutter=1.4, wing_flap=8.0, wing_fold=-6.0)
    ctx.finish(c)
    return c


def c_cast_big(ctx):
    c = Clip("cast_big", 2.4, False)
    z = zeros(c)
    T = (0.0, 0.6, 1.4, 1.72, 1.92, 2.1, 2.4)
    c.keys("body", "rot", list(zip(T, [(0, 0, 0), (6, 0, 0), (10, 0, 0), (14, 0, 0), (-16, 0, 0), (-10, 0, 0), (0, 0, 0)])))
    c.keys("body", "pos", list(zip(T, [(0, 0, 0), (0, -1.6, 0.4), (0, 1.0, 0.6), (0, 2.2, 0.8), (0, -2.4, -2.0), (0, -1.2, -1.0), (0, 0, 0)])))
    c.keys("head", "rot", list(zip(T, [(0, 0, 0), (6, 0, 0), (12, 0, 0), (14, 0, 0), (-14, 0, 0), (-8, 0, 0), (0, 0, 0)])))
    c.keys("leg_l", "rot", list(zip(T, [(0, 0, 0), (8, 0, 4), (10, 0, 7), (10, 0, 7), (18, 0, 5), (10, 0, 3), (0, 0, 0)])))
    c.keys("leg_r", "rot", list(zip(T, [(0, 0, 0), (-4, 0, -4), (-8, 0, -7), (-8, 0, -7), (-14, 0, -5), (-8, 0, -3), (0, 0, 0)])))
    c.keys("shin_l", "rot", list(zip(T, [(-2, 0, 0), (-14, 0, 0), (-10, 0, 0), (-10, 0, 0), (-24, 0, 0), (-14, 0, 0), (-2, 0, 0)])))
    c.keys("shin_r", "rot", list(zip(T, [(-2, 0, 0), (-10, 0, 0), (-8, 0, 0), (-8, 0, 0), (-12, 0, 0), (-8, 0, 0), (-2, 0, 0)])))
    hold = ctx.hold()
    if ctx.holder:
        rest = hold
        keys = [(0.0,) + rest, (0.6, 14, 58, 0.86, 84, 0), (1.4, 60, 40, 0.92, 112, 0), (1.72, 84, 22, 0.95, 124, 0), (1.92, -10, 4, 0.98, 14, 0), (2.1, -26, 12, 0.92, 4, 0), (2.4,) + rest]
        ctx.arm_track(c, ctx.holder, keys, holder=True)
    big = [(0.0, -77, 41, 0.99, None, 0), (0.6, 24, 66, 0.84, None, 0), (1.4, 64, 52, 0.92, None, 0), (1.72, 86, 30, 0.95, None, 0), (1.92, -6, 10, 0.98, None, 0),
           (2.1, -20, 14, 0.9, None, 0), (2.4, -77, 41, 0.99, None, 0)]
    for side in ("l", "r"):
        if side == ctx.holder:
            continue
        ctx.free_arm(c, side, big)
    follow(ctx, c, wind=10.0, osc=1.6, flutter=2.0, wing_flap=12.0, wing_fold=-14.0)
    ctx.finish(c)
    return c


# ------------------------------------------------------------------------------------------------------------------------ hurt
def c_hurt(ctx):
    c = Clip("hurt", 0.45, False)
    T = (0.0, 0.08, 0.45)
    c.keys("body", "rot", list(zip(T, [(0, 0, 0), (12, 4, 3), (0, 0, 0)])))
    c.keys("body", "pos", list(zip(T, [(0, 0, 0), (0, -0.4, 1.8), (0, 0, 0)])))
    c.keys("head", "rot", list(zip(T, [(0, 0, 0), (16, 8, 0), (0, 0, 0)])))
    c.keys("leg_l", "rot", list(zip(T, [(0, 0, 0), (-8, 0, 0), (0, 0, 0)])))
    c.keys("leg_r", "rot", list(zip(T, [(0, 0, 0), (10, 0, 0), (0, 0, 0)])))
    c.keys("shin_l", "rot", list(zip(T, [(-2, 0, 0), (-8, 0, 0), (-2, 0, 0)])))
    c.keys("shin_r", "rot", list(zip(T, [(-2, 0, 0), (-12, 0, 0), (-2, 0, 0)])))
    if ctx.holder:
        h = ctx.hold()
        ctx.arm_track(c, ctx.holder, [(0.0,) + h, (0.08, h[0] + 22, h[1] + 24, h[2], h[3] + 24, 0), (0.45,) + h], holder=True)
    for side in ("l", "r"):
        if side == ctx.holder or (ctx.coupled and side != ctx.holder):
            continue
        ctx.free_arm(c, side, [(0.0, -77, 41, 0.99, None, 0), (0.08, -20, 56, 0.8, None, 0), (0.45, -77, 41, 0.99, None, 0)])
    follow(ctx, c, wind=0.0)
    ctx.finish(c)
    return c


# ------------------------------------------------------------------------------------------------------------------------ transform
TRANSFORM_PEAK = 1.75           # the moment of the power-up peak: where a transformation that swaps models hands over to the second model (on the sample grid)


def c_transform(ctx):
    """3 s: crouch, burst, rise, hold at the peak, settle. With a second form (`form2`) the clip is cut at the peak: the first model plays 0 .. TRANSFORM_PEAK and the
    second model plays TRANSFORM_PEAK .. 3 s, so the swap happens at the peak pose, hidden by the flash."""
    c = Clip("transform", 3.0, False)
    T = (0.0, 0.5, 1.1, TRANSFORM_PEAK, 2.3, 3.0)
    c.keys("body", "rot", list(zip(T, [(0, 0, 0), (-10, 0, 0), (6, 0, 0), (12, 0, 0), (8, 0, 0), (0, 0, 0)])))
    c.keys("body", "pos", list(zip(T, [(0, 0, 0), (0, -2.4, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0)])))
    c.keys("root", "pos", list(zip(T, [(0, 0, 0), (0, 0, 0), (0, 3, 0), (0, 7, 0), (0, 6, 0), (0, 0, 0)])))
    c.keys("head", "rot", list(zip(T, [(0, 0, 0), (14, 0, 0), (-8, 0, 0), (-20, 0, 0), (-12, 0, 0), (0, 0, 0)])))
    c.keys("leg_l", "rot", list(zip(T, [(0, 0, 0), (26, 0, 4), (-2, 0, 6), (-6, 0, 8), (-4, 0, 4), (0, 0, 0)])))
    c.keys("leg_r", "rot", list(zip(T, [(0, 0, 0), (26, 0, -4), (-2, 0, -6), (-6, 0, -8), (-4, 0, -4), (0, 0, 0)])))
    c.keys("shin_l", "rot", list(zip(T, [(-2, 0, 0), (-50, 0, 0), (-4, 0, 0), (-4, 0, 0), (-4, 0, 0), (-2, 0, 0)])))
    c.keys("shin_r", "rot", list(zip(T, [(-2, 0, 0), (-50, 0, 0), (-4, 0, 0), (-4, 0, 0), (-4, 0, 0), (-2, 0, 0)])))
    wide = [(0.0, -77, 41, 0.99, None, 0), (0.5, -60, 150, 0.7, None, 0), (1.1, 40, 84, 0.9, None, 0), (TRANSFORM_PEAK, 58, 80, 0.96, None, 0), (2.3, 50, 76, 0.94, None, 0),
            (3.0, -77, 41, 0.99, None, 0)]
    if ctx.holder:
        h = ctx.hold()
        ctx.arm_track(c, ctx.holder, [(0.0,) + h, (0.5, -50, 120, 0.6, h[3] - 30, 0), (1.1, 38, 78, 0.9, 100, 0), (TRANSFORM_PEAK, 60, 70, 0.96, 120, 0), (2.3, 52, 70, 0.94, 110, 0),
                                      (3.0,) + h], holder=True)
    for side in ("l", "r"):
        if side == ctx.holder:
            continue
        ctx.free_arm(c, side, wide)
    follow(ctx, c, wind=0.0, osc=2.0, flutter=2.0, wing_flap=16.0, wing_fold=-18.0)
    ctx.finish(c, ground=False)
    part = getattr(ctx.R, "form_part", "whole")
    if part == "first":
        return c.slice(0.0, TRANSFORM_PEAK)
    if part == "second":
        return c.slice(TRANSFORM_PEAK, 3.0)
    return c


# ------------------------------------------------------------------------------------------------------------------------ death
def c_death(ctx):
    d = ctx.d
    c = Clip("death", 2.8, False)
    T = (0.0, 0.35, 0.95, 1.5, 2.2, 2.8)
    drop = d.leg_len * 0.45
    c.keys("body", "rot", list(zip(T, [(0, 0, 0), (14, 0, 4), (-8, 0, 0), (-22, 0, 0), (-4, 0, 0), (-4, 0, 0)])))
    c.keys("body", "pos", list(zip(T, [(0, 0, 0), (0, 0, 2), (0, -drop, 0), (0, -drop * 1.1, 0), (0, -drop * 1.4, 0), (0, -drop * 1.4, 0)])))
    c.keys("head", "rot", list(zip(T, [(0, 0, 0), (20, 0, 0), (-8, 0, 0), (-34, 0, 4), (-6, 0, 10), (-6, 0, 10)])))
    c.keys("leg_l", "rot", list(zip(T, [(0, 0, 0), (-10, 0, 0), (66, 0, 4), (82, 0, 5), (6, 0, 0), (6, 0, 0)])))
    c.keys("leg_r", "rot", list(zip(T, [(0, 0, 0), (10, 0, 0), (-8, 0, -4), (-12, 0, -6), (6, 0, 0), (6, 0, 0)])))
    c.keys("shin_l", "rot", list(zip(T, [(-2, 0, 0), (-8, 0, 0), (-70, 0, 0), (-82, 0, 0), (-6, 0, 0), (-6, 0, 0)])))
    c.keys("shin_r", "rot", list(zip(T, [(-2, 0, 0), (-8, 0, 0), (-104, 0, 0), (-110, 0, 0), (-6, 0, 0), (-6, 0, 0)])))
    c.keys("root", "rot", list(zip(T, [(0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 84), (0, 0, 84)])))
    H = d.top
    c.keys("root", "pos", list(zip(T, [(0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (-H * 0.30 + 3, 2, 0), (-H * 0.30 + 3, 3, 0)])))
    for side, sg in (("l", 1), ("r", -1)):
        c.keys("arm_" + side, "rot", list(zip(T, [(8, 0, sg * 9), (-30, 0, sg * 16), (30, 0, sg * 12), (38, 0, sg * 10), (-8, 0, sg * 30), (-8, 0, sg * 30)])))
        c.keys("forearm_" + side, "rot", list(zip(T, [(10, 0, 0), (50, 0, 0), (20, 0, 0), (10, 0, 0), (6, 0, 0), (6, 0, 0)])))
    follow(ctx, c, wind=0.0, osc=0.6, wing_fold=18.0)
    ctx.finish(c, ground=False, clear=False)
    return c


# ------------------------------------------------------------------------------------------------------------------------ the optional clips
def c_spawn(ctx):
    c = Clip("spawn", 2.0, False)
    T = (0.0, 0.5, 1.1, 1.6, 2.0)
    c.keys("body", "rot", list(zip(T, [(-26, 0, 0), (-18, 0, 0), (6, 0, 0), (-2, 0, 0), (0, 0, 0)])))
    c.keys("body", "pos", list(zip(T, [(0, -3.4, 0), (0, -3.0, 0), (0, 0.4, 0), (0, 0, 0), (0, 0, 0)])))
    c.keys("head", "rot", list(zip(T, [(24, 0, 0), (20, 0, 0), (-8, 0, 0), (-2, 0, 0), (0, 0, 0)])))
    c.keys("leg_l", "rot", list(zip(T, [(40, 0, 3), (36, 0, 3), (-2, 0, 2), (0, 0, 0), (0, 0, 0)])))
    c.keys("leg_r", "rot", list(zip(T, [(40, 0, -3), (36, 0, -3), (-2, 0, -2), (0, 0, 0), (0, 0, 0)])))
    c.keys("shin_l", "rot", list(zip(T, [(-72, 0, 0), (-66, 0, 0), (-4, 0, 0), (-2, 0, 0), (-2, 0, 0)])))
    c.keys("shin_r", "rot", list(zip(T, [(-72, 0, 0), (-66, 0, 0), (-4, 0, 0), (-2, 0, 0), (-2, 0, 0)])))
    if ctx.holder:
        h = ctx.hold()
        ctx.arm_track(c, ctx.holder, [(0.0, -40, 30, 0.7, h[3] - 40, 0), (0.5, -40, 30, 0.7, h[3] - 40, 0), (1.1, 30, 70, 0.9, h[3] + 30, 0), (1.6, h[0] + 6, h[1], h[2], h[3] + 4, 0),
                                      (2.0,) + h], holder=True)
    for side in ("l", "r"):
        if side == ctx.holder:
            continue
        ctx.free_arm(c, side, [(0.0, -40, 30, 0.7, None, 0), (0.5, -40, 30, 0.7, None, 0), (1.1, 20, 90, 0.9, None, 0), (1.6, -50, 40, 0.9, None, 0), (2.0, -77, 41, 0.99, None, 0)])
    follow(ctx, c, wind=2.0, osc=1.4, wing_fold=-10.0)
    ctx.finish(c, ground=False)
    return c


def c_reveal(ctx):
    c = Clip("chest_gem_reveal", 1.8, False)
    T = (0.0, 0.35, 0.9, 1.4, 1.8)
    c.keys("body", "rot", list(zip(T, [(0, 0, 0), (-6, 0, 0), (9, 0, 0), (9, 0, 0), (0, 0, 0)])))
    c.keys("body", "pos", list(zip(T, [(0, 0, 0), (0, -1.6, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0)])))
    c.keys("head", "rot", list(zip(T, [(0, 0, 0), (8, 0, 0), (-14, 0, 0), (-14, 0, 0), (0, 0, 0)])))
    wide = [(0.0, -77, 41, 0.99, None, 0), (0.35, -50, 40, 0.8, None, 0), (0.9, 16, 100, 0.92, None, 0), (1.4, 20, 96, 0.92, None, 0), (1.8, -77, 41, 0.99, None, 0)]
    if ctx.holder:
        h = ctx.hold()
        ctx.arm_track(c, ctx.holder, [(0.0,) + h, (0.35, -40, 40, 0.7, h[3], 0), (0.9, 20, 94, 0.9, 100, 0), (1.4, 22, 92, 0.9, 100, 0), (1.8,) + h], holder=True)
    for side in ("l", "r"):
        if side == ctx.holder:
            continue
        ctx.free_arm(c, side, wide)
    follow(ctx, c, wind=0.0, osc=1.2, wing_fold=-12.0)
    ctx.finish(c)
    return c


CLIP_FUNCS = (("idle", c_idle), ("walk", lambda ctx: locomotion(ctx, False)), ("run", lambda ctx: locomotion(ctx, True)), ("attack", c_attack), ("cast", c_cast),
              ("cast_big", c_cast_big), ("hurt", c_hurt), ("transform", c_transform), ("death", c_death))
EXTRA_FUNCS = {"spawn": c_spawn, "chest_gem_reveal": c_reveal}


def emit(R, c, tol_rot=0.12, tol_pos=0.04, tol_scale=0.003):
    """Writes a finished clip into the Anim of the rig: every moving track, thinned so a straight run of samples is one segment."""
    A = R.A
    A.clip(c.name, c.length, c.loop)
    for kind, store, setter, tol, neutral in (("rot", c.rot, A.rot, tol_rot, 0.0), ("pos", c.pos, A.pos, tol_pos, 0.0), ("scale", c.scale, A.scale, tol_scale, 1.0)):
        for bone in sorted(store):
            if not R.has(bone):
                continue
            arr = store[bone].copy()
            if c.loop:
                arr[-1] = arr[0]
            if np.max(np.abs(arr - neutral)) < tol * 0.5:
                continue
            idx = _simplify(arr, tol)
            frames = {float(round(c.ts[i], 4)): tuple(round(float(v), 3) for v in arr[i]) for i in idx}
            setter(c.name, bone, frames)


def _simplify(arr, tol):
    """Indices of the samples to keep: the ends plus every sample a straight line between its kept neighbours would miss by more than tol."""
    n = arr.shape[0]
    keep = [0, n - 1]
    stack_ = [(0, n - 1)]
    while stack_:
        a, b = stack_.pop()
        if b - a < 2:
            continue
        t = (np.arange(a, b + 1) - a) / float(b - a)
        line = arr[a][None, :] * (1 - t)[:, None] + arr[b][None, :] * t[:, None]
        err = np.max(np.abs(arr[a:b + 1] - line), axis=1)
        k = int(np.argmax(err))
        if err[k] > tol:
            keep.append(a + k)
            stack_.append((a, a + k))
            stack_.append((a + k, b))
    return sorted(set(keep))


def _build_all(R, S, only):
    """One pass over every clip of the character: builds, writes into R.A and returns the Ctx that did it."""
    ctx = Ctx(R, S)
    R.form_part = "second" if getattr(R, "form", 1) == 2 else ("first" if S["form2"] is not None else "whole")
    out = {}
    for name, fn in CLIP_FUNCS:
        if only is None or name in only:
            out[name] = fn(ctx)
    for name in S["clips"]["extra"]:
        if only is None or name in only:
            out[name] = EXTRA_FUNCS[name](ctx)
    for name, c in out.items():
        emit(R, c)
    R.facts["clips"] = {n: (c.length, c.loop) for n, c in out.items()}
    return ctx, out


def build_clips(R, S):
    """Builds and writes every clip. The environment variable ARC_FAST (a comma list of clip names, or 1 for idle only) restricts the build for quick looks at a model;
    a restricted build is never committed (--check would differ). A two-handed prop whose clips break the arm rules (an arm in the chest, an elbow that flips, a one-shot that
    ends on another pose than it started on) is carried in one hand instead and the clips are built again: a character that is built is a character that passes."""
    import copy
    import os
    from . import clipcheck
    from .rig import Anim
    fast = os.environ.get("ARC_FAST")
    only = None if not fast else (set(fast.split(",")) if fast != "1" else {"idle"})
    ctx, out = _build_all(R, S, only)
    R.facts["grip"] = "two-handed" if ctx.coupled and ctx.kind != "bow" else ("one-handed" if ctx.main and ctx.main.two and ctx.kind != "bow" else "")
    if ctx.main and ctx.main.two and ctx.kind != "bow" and not ctx.coupled:
        R.facts["grip_note"] = "carried in one hand: " + ("`two_hand` is false" if S["props"][ctx.main_slot].get("two_hand") is False else "the arms of this build cannot reach the shaft")
    if ctx.coupled and ctx.kind != "bow" and only is None:
        bad = clipcheck.arm_problems(R)
        if bad:
            S2 = copy.deepcopy(S)
            S2["props"][ctx.main_slot]["two_hand"] = False
            R.A = Anim()
            ctx, out = _build_all(R, S2, only)
            R.facts["grip"] = "one-handed"
            R.facts["grip_note"] = "carried in one hand: both hands broke the arm rules (%s)" % "; ".join(bad[:2])
    return out
