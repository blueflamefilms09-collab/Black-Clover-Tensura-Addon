"""The poses of the Spirit of Darkness (docs/magic/spirit_dark.md, section poses): every clip of both forms, written as small functions of the normalised time u in 0..1 and sampled into keyframes,
so a pose reads as what it is (a bow is a smooth lean, a cackle is a head thrown back with shaking shoulders) and the numbers stay tunable. Bones move by rotation, position and scale only; no
geometry changes per frame. The tail, the coat, the wisps and the tendrils always sway on top of the pose, so even a held pose is alive.

    powered:   idle (the lounging aristocrat), flap (travelling), bow, smug (arms crossed), cast (claws), feast (life steal), recoil (hurt), cackle, battle (stance), vanish, appear, farewell
    companion: idle (lounging), bow, smug, cast, feast, recoil, cackle, battle, vanish, appear, farewell

A clip is {"loop": bool, "length": seconds, "bones": {bone: {"rotation"|"position"|"scale": {t: (x, y, z)}}}}; the pose table with the ticks each plays for is spiritdark/SpiritDarkRules.Pose.
"""
import math

TAU = math.tau


def sm(u):
    u = max(0.0, min(1.0, u))
    return u * u * (3 - 2 * u)


def trap(u, a, b, c, d):
    """0 before a, rising to 1 at b, 1 until c, falling to 0 at d (smooth)."""
    if u <= a or u >= d:
        return 0.0
    if u < b:
        return sm((u - a) / max(1e-6, b - a))
    if u <= c:
        return 1.0
    return 1.0 - sm((u - c) / max(1e-6, d - c))


def s(u, cycles=1.0, phase=0.0):
    return math.sin((u * cycles + phase) * TAU)


def v3(x=0.0, y=0.0, z=0.0):
    return (x, y, z)


def add(*vs):
    return (sum(v[0] for v in vs), sum(v[1] for v in vs), sum(v[2] for v in vs))


def mul(v, k):
    return (v[0] * k, v[1] * k, v[2] * k)


ZERO = (0.0, 0.0, 0.0)


class Pose:
    """tracks[bone][kind] = function(u) -> (x, y, z); kind is rotation, position or scale (scale defaults to 1)."""

    def __init__(self, length, loop=True):
        self.length, self.loop, self.tracks = length, loop, {}

    def rot(self, bone, fn):
        self.tracks.setdefault(bone, {})["rotation"] = fn
        return self

    def pos(self, bone, fn):
        self.tracks.setdefault(bone, {})["position"] = fn
        return self

    def scl(self, bone, fn):
        self.tracks.setdefault(bone, {})["scale"] = fn
        return self


def bake(pose, sway=None, steps=16):
    """Samples a pose (plus the always-on sway functions {bone: fn(u) -> rotation}) into the keyframe dict write_animations takes."""
    bones = {}
    names = set(pose.tracks) | set(sway or {})
    times = [pose.length * i / steps for i in range(steps + 1)]
    for b in sorted(names):
        tr = pose.tracks.get(b, {})
        out = {}
        for kind in ("rotation", "position", "scale"):
            fn = tr.get(kind)
            sw = (sway or {}).get(b) if kind == "rotation" else None
            if fn is None and sw is None:
                continue
            frames = {}
            for i, t in enumerate(times):
                u = i / steps
                if kind == "scale":
                    v = fn(u)
                else:
                    v = add(fn(u) if fn else ZERO, sw(u) if sw else ZERO)
                frames[t] = v
            out[kind] = frames
        bones[b] = out
    return {"loop": pose.loop, "length": pose.length, "bones": bones}


# ====================================================================================================================== sway
def powered_sway(loop_cycles=1.0, amp=1.0):
    sw = {}
    for i in range(7):
        sw["tail%d" % i] = (lambda i: lambda u: (amp * 2.0 * s(u, loop_cycles, -0.09 * i), amp * (4.0 + 0.8 * i) * s(u, loop_cycles, -0.07 * i + 0.2), 0.0))(i)
    sw["tailtip"] = lambda u: (0.0, amp * 9.0 * s(u, loop_cycles, -0.6), 0.0)
    for side, k in (("L", 1), ("R", -1)):
        sw["coatB_" + side] = (lambda u, k=k: (amp * 5.0 * s(u, loop_cycles, 0.3), 0.0, 0.0))
        sw["coatF_" + side] = (lambda u, k=k: (amp * -4.0 * s(u, loop_cycles, 0.0), 0.0, 0.0))
        sw["coatS_" + side] = (lambda u, k=k: (0.0, 0.0, amp * 3.0 * k * s(u, loop_cycles, 0.2)))
        sw["wisp_" + side] = (lambda u, k=k: (0.0, 0.0, amp * 7.0 * s(u, loop_cycles, 0.4 * k)))
    return sw


def companion_sway(loop_cycles=1.0, amp=1.0):
    sw = {}
    for i in range(8):
        sw["tail%d" % i] = (lambda i: lambda u: (amp * 6.0 * s(u, loop_cycles, -0.08 * i), amp * (9.0 + 1.5 * i) * s(u, loop_cycles, -0.1 * i), 0.0))(i)
    for i in range(7):
        sw["ten%d" % i] = (lambda i: lambda u: (amp * 10.0 * s(u, loop_cycles, 0.13 * i), 0.0, amp * 8.0 * s(u, loop_cycles, 0.09 * i)))(i)
        sw["ten%db" % i] = (lambda i: lambda u: (amp * 14.0 * s(u, loop_cycles, 0.13 * i + 0.2), 0.0, amp * 10.0 * s(u, loop_cycles, 0.09 * i + 0.1)))(i)
    for i in range(3):
        sw["tuft%d" % i] = (lambda i: lambda u: (amp * 12.0 * s(u, loop_cycles, 0.11 * i), 0.0, amp * 9.0 * s(u, loop_cycles, 0.17 * i)))(i)
        sw["tuft%db" % i] = (lambda i: lambda u: (amp * 18.0 * s(u, loop_cycles, 0.11 * i + 0.2), 0.0, amp * 12.0 * s(u, loop_cycles, 0.17 * i + 0.1)))(i)
    for side in ("L", "R"):
        sw["cheek_" + side] = lambda u: (0.0, 0.0, amp * 9.0 * s(u, loop_cycles, 0.3))
    sw["cloak"] = lambda u: (amp * 2.5 * s(u, loop_cycles, 0.3), amp * 4.0 * s(u, loop_cycles, 0.1), 0.0)
    sw["cloak2"] = lambda u: (amp * 3.0 * s(u, loop_cycles, 0.1), amp * 6.0 * s(u, loop_cycles, -0.1), 0.0)
    return sw


# ====================================================================================================================== the powered form
def powered_poses():
    P = {}
    # ---- idle: the lounging aristocrat: reclined a little, one hand on the hip, the other loosely forward, a slow bob
    p = Pose(3.2)
    p.pos("body", lambda u: (0, 0.8 * s(u), 0))
    p.rot("chest", lambda u: (-5 + 1.5 * s(u, 1, 0.1), 3.0 * s(u), 0))
    p.rot("head", lambda u: (-3 + 2.0 * s(u, 1, 0.3), -8 + 4.0 * s(u), 4.0 * s(u, 1, 0.2)))
    p.rot("arm_R", lambda u: (0, 0, -20))
    p.rot("fore_R", lambda u: (-70, 0, 0))
    p.rot("arm_L", lambda u: (-22 + 4.0 * s(u, 1, 0.2), 0, 8))
    p.rot("fore_L", lambda u: (-40 + 5.0 * s(u, 1, 0.4), 0, 0))
    p.rot("hand_L", lambda u: (-12 + 6.0 * s(u, 2, 0.1), 0, 0))
    p.rot("wing_L", lambda u: (0, 0, 5.0 * s(u, 1, 0.05)))
    p.rot("wing_R", lambda u: (0, 0, -5.0 * s(u, 1, 0.05)))
    p.rot("wing2_L", lambda u: (0, 0, 6.0 * s(u, 1, -0.12)))
    p.rot("wing2_R", lambda u: (0, 0, -6.0 * s(u, 1, -0.12)))
    p.rot("leg_L", lambda u: (6, 0, 0))
    p.rot("leg_R", lambda u: (-8, 0, 0))
    p.rot("orbit", lambda u: (0, 120.0 * u, 0))
    p.rot("ring", lambda u: (0, 120.0 * u, 0))
    P["idle"] = (p, 1.0)

    # ---- flap: travelling: wings beat, body bobs, the coat streams
    p = Pose(0.9)
    p.pos("body", lambda u: (0, 1.6 * s(u, 1, 0.1), 0))
    p.rot("chest", lambda u: (-8, 0, 0))
    p.rot("wing_L", lambda u: (0, 0, 30 * s(u)))
    p.rot("wing_R", lambda u: (0, 0, -30 * s(u)))
    p.rot("wing2_L", lambda u: (0, 0, 24 * s(u, 1, -0.14)))
    p.rot("wing2_R", lambda u: (0, 0, -24 * s(u, 1, -0.14)))
    p.rot("arm_L", lambda u: (-12, 0, 0))
    p.rot("arm_R", lambda u: (-12, 0, 0))
    p.rot("orbit", lambda u: (0, 240.0 * u, 0))
    p.rot("ring", lambda u: (0, 240.0 * u, 0))
    P["flap"] = (p, 2.0)

    # ---- bow: a smooth lean from the waist, the right hand across the chest, the left behind the back, the head last to rise
    p = Pose(1.8, loop=False)
    e = lambda u: trap(u, 0.0, 0.32, 0.62, 1.0)
    p.rot("chest", lambda u: (-34 * e(u), 0, 0))
    p.rot("neck", lambda u: (-10 * e(u), 0, 0))
    p.rot("head", lambda u: (-14 * e(u), 0, 0))
    p.rot("arm_R", lambda u: (-30 * e(u), 0, -38 * e(u)))
    p.rot("fore_R", lambda u: (-100 * e(u), 0, 0))
    p.rot("arm_L", lambda u: (24 * e(u), 0, 14 * e(u)))
    p.rot("fore_L", lambda u: (-20 * e(u), 0, 0))
    p.rot("hips", lambda u: (6 * e(u), 0, 0))
    p.rot("wing_L", lambda u: (0, 0, -10 * e(u)))
    p.rot("wing_R", lambda u: (0, 0, 10 * e(u)))
    p.pos("body", lambda u: (0, -0.8 * e(u), 0))
    p.rot("orbit", lambda u: (0, 60.0 * u, 0))
    P["bow"] = (p, 0.6)

    # ---- smug: arms crossed over the chest, chin up, a slow sway, one wing flicked
    p = Pose(2.4)
    p.rot("chest", lambda u: (-3, 4.0 * s(u), 0))
    p.rot("head", lambda u: (6, -10 + 5.0 * s(u, 1, 0.2), 6.0 * s(u, 1, 0.1)))
    p.rot("arm_L", lambda u: (-48, -50, 10))
    p.rot("fore_L", lambda u: (-96, 0, 0))
    p.rot("arm_R", lambda u: (-48, 50, -10))
    p.rot("fore_R", lambda u: (-96, 0, 0))
    p.rot("wing_L", lambda u: (0, 0, 4 + 14 * trap(u, 0.5, 0.6, 0.65, 0.8)))
    p.rot("wing_R", lambda u: (0, 0, -4))
    p.pos("body", lambda u: (0, 0.6 * s(u), 0))
    p.rot("orbit", lambda u: (0, 120.0 * u, 0))
    p.rot("ring", lambda u: (0, 120.0 * u, 0))
    P["smug"] = (p, 1.0)

    # ---- cast: the claws: the right hand sweeps forward and up, the left behind, the head leans in, the wings flare, the coat billows
    p = Pose(0.9, loop=False)
    p.rot("chest", lambda u: (-8 * trap(u, 0, 0.2, 0.3, 0.45) + 10 * trap(u, 0.3, 0.45, 0.65, 1.0), 6 * trap(u, 0, 0.2, 0.3, 0.45) - 8 * trap(u, 0.3, 0.45, 0.65, 1.0), 0))
    p.rot("head", lambda u: (-6 * trap(u, 0, 0.2, 0.3, 0.45) + 12 * trap(u, 0.3, 0.45, 0.65, 1.0), 0, 0))
    p.rot("arm_R", lambda u: (-100 * trap(u, 0.1, 0.45, 0.65, 1.0), 0, -8 * trap(u, 0.1, 0.45, 0.65, 1.0)))
    p.rot("fore_R", lambda u: (-40 * trap(u, 0.1, 0.45, 0.65, 1.0), 0, 0))
    p.rot("hand_R", lambda u: (-20 * trap(u, 0.3, 0.45, 0.65, 1.0), 0, 0))
    p.rot("arm_L", lambda u: (30 * trap(u, 0.1, 0.45, 0.65, 1.0), 0, 22 * trap(u, 0.1, 0.45, 0.65, 1.0)))
    p.rot("wing_L", lambda u: (0, 0, 26 * trap(u, 0.1, 0.45, 0.65, 1.0)))
    p.rot("wing_R", lambda u: (0, 0, -26 * trap(u, 0.1, 0.45, 0.65, 1.0)))
    p.rot("coatB_L", lambda u: (24 * trap(u, 0.1, 0.45, 0.7, 1.0), 0, 0))
    p.rot("coatB_R", lambda u: (24 * trap(u, 0.1, 0.45, 0.7, 1.0), 0, 0))
    P["cast"] = (p, 1.0)

    # ---- feast: the life steal: the head thrown back, the arms wide, the wings spread, the body swelling and shrinking with the drink
    p = Pose(1.5, loop=False)
    e = lambda u: trap(u, 0.0, 0.25, 0.7, 1.0)
    p.rot("chest", lambda u: (8 * e(u), 0, 0))
    p.rot("head", lambda u: (24 * e(u), 0, 0))
    p.rot("arm_L", lambda u: (-10 * e(u), 0, 54 * e(u)))
    p.rot("arm_R", lambda u: (-10 * e(u), 0, -54 * e(u)))
    p.rot("fore_L", lambda u: (-30 * e(u), 0, 0))
    p.rot("fore_R", lambda u: (-30 * e(u), 0, 0))
    p.rot("wing_L", lambda u: (0, 0, 24 * e(u)))
    p.rot("wing_R", lambda u: (0, 0, -24 * e(u)))
    p.scl("body", lambda u: (1.0 + 0.05 * e(u) * s(u, 3), 1.0 + 0.06 * e(u) * s(u, 3), 1.0 + 0.05 * e(u) * s(u, 3)))
    p.pos("body", lambda u: (0, 1.2 * e(u), 0))
    p.rot("orbit", lambda u: (0, 360.0 * u, 0))
    P["feast"] = (p, 1.6)

    # ---- recoil: struck: thrown back, the wings snapping open, the arms up, then settling
    p = Pose(0.6, loop=False)
    e = lambda u: trap(u, 0.0, 0.18, 0.4, 1.0)
    p.rot("chest", lambda u: (16 * e(u), 0, 6 * e(u)))
    p.rot("head", lambda u: (14 * e(u), -10 * e(u), 0))
    p.rot("arm_L", lambda u: (-60 * e(u), 0, 30 * e(u)))
    p.rot("arm_R", lambda u: (-60 * e(u), 0, -30 * e(u)))
    p.rot("wing_L", lambda u: (0, 0, 32 * e(u)))
    p.rot("wing_R", lambda u: (0, 0, -32 * e(u)))
    p.pos("body", lambda u: (0, 0.4 * e(u), 2.6 * e(u)))
    p.rot("hips", lambda u: (-8 * e(u), 0, 0))
    P["recoil"] = (p, 1.0)

    # ---- cackle: the head thrown back, the shoulders shaking, one hand on the belly, the wings quivering
    p = Pose(2.4)
    e = lambda u: trap(u, 0.0, 0.1, 0.9, 1.0)
    p.rot("chest", lambda u: (6 * e(u) + 5.0 * e(u) * s(u, 9), 0, 2.0 * e(u) * s(u, 9, 0.25)))
    p.rot("head", lambda u: (28 * e(u) + 4.0 * e(u) * s(u, 9, 0.5), 0, 0))
    p.rot("arm_R", lambda u: (-40 * e(u), 0, -6 * e(u)))
    p.rot("fore_R", lambda u: (-100 * e(u), 0, 0))
    p.rot("arm_L", lambda u: (-20 * e(u) + 6.0 * e(u) * s(u, 9), 0, 18 * e(u)))
    p.rot("wing_L", lambda u: (0, 0, 10 * e(u) + 6.0 * e(u) * s(u, 9, 0.1)))
    p.rot("wing_R", lambda u: (0, 0, -10 * e(u) - 6.0 * e(u) * s(u, 9, 0.1)))
    p.pos("body", lambda u: (0, 1.0 * e(u) * abs(s(u, 4.5)), 0))
    p.rot("orbit", lambda u: (0, 240.0 * u, 0))
    P["cackle"] = (p, 3.0)

    # ---- battle: the stance: leaning in, the wings flared wide, claws forward, the tail raised, hovering with a hard bob
    p = Pose(1.6)
    p.pos("body", lambda u: (0, 1.0 * s(u, 1, 0.1), 0))
    p.rot("chest", lambda u: (-12, 0, 0))
    p.rot("head", lambda u: (-6, 0, 0))
    p.rot("arm_L", lambda u: (-52 + 4 * s(u), 0, 14))
    p.rot("arm_R", lambda u: (-52 + 4 * s(u, 1, 0.3), 0, -14))
    p.rot("fore_L", lambda u: (-34, 0, 0))
    p.rot("fore_R", lambda u: (-34, 0, 0))
    p.rot("hand_L", lambda u: (-18, 0, 0))
    p.rot("hand_R", lambda u: (-18, 0, 0))
    p.rot("wing_L", lambda u: (0, 0, 24 + 6 * s(u, 2)))
    p.rot("wing_R", lambda u: (0, 0, -24 - 6 * s(u, 2)))
    p.rot("wing2_L", lambda u: (0, 0, 12 + 5 * s(u, 2, -0.15)))
    p.rot("wing2_R", lambda u: (0, 0, -12 - 5 * s(u, 2, -0.15)))
    p.rot("hips", lambda u: (-6, 0, 0))
    p.rot("leg_L", lambda u: (16, 0, 5))
    p.rot("leg_R", lambda u: (-6, 0, -5))
    p.rot("orbit", lambda u: (0, 240.0 * u, 0))
    p.rot("ring", lambda u: (0, 240.0 * u, 0))
    for i in range(7):
        p.rot("tail%d" % i, (lambda i: lambda u: (-5 - 1.5 * i, 0, 0))(i))
    P["battle"] = (p, 2.0)

    # ---- vanish / appear: a spin and a squash into nothing, and back
    for name, rev in (("appear", True), ("vanish", False)):
        p = Pose(0.45, loop=False)
        k = (lambda u: 1 - sm(u)) if rev else (lambda u: sm(u))                  # 1 = gone
        p.scl("body", lambda u, k=k: (1.0 - 0.95 * k(u) + 0.12 * math.sin(math.pi * u) * (1 - k(u)), 1.0 - 0.95 * k(u) - 0.1 * math.sin(math.pi * u), 1.0 - 0.95 * k(u) + 0.12 * math.sin(math.pi * u) * (1 - k(u))))
        p.rot("body", lambda u, k=k, rev=rev: (0, (-90 if rev else 110) * k(u), 0))
        p.pos("body", lambda u, k=k: (0, -4 * k(u), 0))
        P[name] = (p, 1.0)

    # ---- farewell: a deep bow and a dissolve: the body shrinks, spins and rises into smoke
    p = Pose(1.8, loop=False)
    bowe = lambda u: trap(u, 0.0, 0.25, 0.5, 0.7)
    gone = lambda u: sm((u - 0.45) / 0.55)
    p.rot("chest", lambda u: (-30 * bowe(u), 0, 0))
    p.rot("head", lambda u: (-14 * bowe(u), 0, 0))
    p.rot("arm_R", lambda u: (-30 * bowe(u), 0, -38 * bowe(u)))
    p.rot("fore_R", lambda u: (-100 * bowe(u), 0, 0))
    p.rot("wing_L", lambda u: (0, 0, 18 * gone(u)))
    p.rot("wing_R", lambda u: (0, 0, -18 * gone(u)))
    p.scl("body", lambda u: (1.0 - 0.95 * gone(u), 1.0 - 0.7 * gone(u), 1.0 - 0.95 * gone(u)))
    p.rot("body", lambda u: (0, 200 * gone(u), 0))
    p.pos("body", lambda u: (0, 5 * gone(u), 0))
    P["farewell"] = (p, 0.5)
    return P


# ====================================================================================================================== the companion
def companion_poses():
    P = {}
    p = Pose(2.6)                                                         # idle: lounging: leaned back, one arm tucked, drifting
    p.pos("body", lambda u: (0, 1.0 * s(u), 0))
    p.rot("core", lambda u: (10 + 3 * s(u, 1, 0.1), 5 * s(u), 8 + 3 * s(u, 1, 0.2)))
    p.rot("head", lambda u: (2 * s(u, 1, 0.15), -6 + 3 * s(u), 0))
    p.rot("wing_L", lambda u: (0, 0, 12 * s(u)))
    p.rot("wing_R", lambda u: (0, 0, -12 * s(u)))
    p.rot("wing2_L", lambda u: (0, 0, 10 * s(u, 1, -0.15)))
    p.rot("wing2_R", lambda u: (0, 0, -10 * s(u, 1, -0.15)))
    p.rot("arm_L", lambda u: (-30, 0, 10 + 3 * s(u, 1, 0.2)))
    p.rot("arm_R", lambda u: (-18, 0, -8))
    P["idle"] = (p, 1.0)

    p = Pose(1.6, loop=False)                                             # bow: a nod and a sweep
    e = lambda u: trap(u, 0.0, 0.3, 0.6, 1.0)
    p.rot("core", lambda u: (-38 * e(u), 0, 0))
    p.rot("head", lambda u: (-14 * e(u), 0, 0))
    p.rot("arm_R", lambda u: (-60 * e(u), 0, -40 * e(u)))
    p.rot("arm_L", lambda u: (20 * e(u), 0, 25 * e(u)))
    p.pos("body", lambda u: (0, -0.8 * e(u), 0))
    P["bow"] = (p, 0.6)

    p = Pose(2.4)                                                         # smug: arms crossed, chin up
    p.pos("body", lambda u: (0, 0.8 * s(u), 0))
    p.rot("core", lambda u: (-6, 4 * s(u), 3 * s(u, 1, 0.2)))
    p.rot("head", lambda u: (8, -8 + 4 * s(u, 1, 0.2), 4))
    p.rot("arm_L", lambda u: (-70, -55, 10))
    p.rot("arm_R", lambda u: (-70, 55, -10))
    p.rot("wing_L", lambda u: (0, 0, 6 + 12 * trap(u, 0.5, 0.6, 0.65, 0.8)))
    p.rot("wing_R", lambda u: (0, 0, -6))
    P["smug"] = (p, 1.0)

    p = Pose(0.8, loop=False)                                             # cast: lean back, then lunge with both claws
    tt = lambda u: trap(u, 0.0, 0.25, 0.5, 1.0)
    p.rot("core", lambda u: (-14 * trap(u, 0, 0.2, 0.3, 0.5) + 12 * trap(u, 0.3, 0.5, 0.7, 1.0), 0, 0))
    p.scl("core", lambda u: (1 - 0.1 * trap(u, 0, 0.2, 0.3, 0.5) + 0.25 * trap(u, 0.3, 0.5, 0.7, 1.0),) * 3)
    p.rot("wing_L", lambda u: (0, 0, 34 * tt(u)))
    p.rot("wing_R", lambda u: (0, 0, -34 * tt(u)))
    p.rot("arm_L", lambda u: (-80 * tt(u), 0, 20 * tt(u)))
    p.rot("arm_R", lambda u: (-80 * tt(u), 0, -20 * tt(u)))
    for i in range(7):
        p.rot("ten%d" % i, (lambda i: lambda u: (-24 * tt(u), 0, 0))(i))
    P["cast"] = (p, 1.0)

    p = Pose(1.4, loop=False)                                             # feast: swell, head back, drink
    e = lambda u: trap(u, 0.0, 0.25, 0.7, 1.0)
    p.rot("core", lambda u: (10 * e(u), 0, 0))
    p.rot("head", lambda u: (22 * e(u), 0, 0))
    p.scl("core", lambda u: (1 + 0.14 * e(u) * s(u, 3),) * 3)
    p.rot("wing_L", lambda u: (0, 0, 26 * e(u)))
    p.rot("wing_R", lambda u: (0, 0, -26 * e(u)))
    p.rot("arm_L", lambda u: (-20 * e(u), 0, 50 * e(u)))
    p.rot("arm_R", lambda u: (-20 * e(u), 0, -50 * e(u)))
    p.pos("body", lambda u: (0, 1.4 * e(u), 0))
    P["feast"] = (p, 1.6)

    p = Pose(0.55, loop=False)                                            # recoil: jerked back
    e = lambda u: trap(u, 0.0, 0.18, 0.4, 1.0)
    p.rot("core", lambda u: (18 * e(u), 0, 8 * e(u)))
    p.rot("wing_L", lambda u: (0, 0, 34 * e(u)))
    p.rot("wing_R", lambda u: (0, 0, -34 * e(u)))
    p.pos("body", lambda u: (0, 0.5 * e(u), 3.0 * e(u)))
    p.rot("arm_L", lambda u: (-70 * e(u), 0, 20 * e(u)))
    p.rot("arm_R", lambda u: (-70 * e(u), 0, -20 * e(u)))
    P["recoil"] = (p, 1.0)

    p = Pose(2.2)                                                         # cackle: head back, shaking
    e = lambda u: trap(u, 0.0, 0.1, 0.9, 1.0)
    p.rot("core", lambda u: (16 * e(u) + 6 * e(u) * s(u, 9), 0, 3 * e(u) * s(u, 9, 0.25)))
    p.rot("head", lambda u: (20 * e(u), 0, 0))
    p.rot("wing_L", lambda u: (0, 0, 14 * e(u) + 8 * e(u) * s(u, 9, 0.1)))
    p.rot("wing_R", lambda u: (0, 0, -14 * e(u) - 8 * e(u) * s(u, 9, 0.1)))
    p.pos("body", lambda u: (0, 1.0 * e(u) * abs(s(u, 4.5)), 0))
    p.rot("arm_L", lambda u: (-50 * e(u), 0, 12 * e(u)))
    p.rot("arm_R", lambda u: (-50 * e(u), 0, -12 * e(u)))
    P["cackle"] = (p, 3.0)

    p = Pose(1.4)                                                         # battle: puffed up, wings spread, tendrils flared
    p.pos("body", lambda u: (0, 1.0 * s(u), 0))
    p.rot("core", lambda u: (-14, 0, 0))
    p.scl("core", lambda u: (1.08, 1.08, 1.08))
    p.rot("wing_L", lambda u: (0, 0, 28 + 8 * s(u, 2)))
    p.rot("wing_R", lambda u: (0, 0, -28 - 8 * s(u, 2)))
    p.rot("arm_L", lambda u: (-64, 0, 14))
    p.rot("arm_R", lambda u: (-64, 0, -14))
    for i in range(7):
        p.rot("ten%d" % i, (lambda i: lambda u: (-30 + 6 * s(u, 2, 0.1 * i), 0, 0))(i))
    P["battle"] = (p, 2.0)

    for name, rev in (("appear", True), ("vanish", False)):
        p = Pose(0.4, loop=False)
        k = (lambda u: 1 - sm(u)) if rev else (lambda u: sm(u))
        p.scl("body", lambda u, k=k: (1.0 - 0.95 * k(u) + 0.2 * math.sin(math.pi * u) * (1 - k(u)), 1.0 - 0.95 * k(u) - 0.15 * math.sin(math.pi * u), 1.0 - 0.95 * k(u) + 0.2 * math.sin(math.pi * u) * (1 - k(u))))
        p.rot("body", lambda u, k=k, rev=rev: (0, (-120 if rev else 140) * k(u), 0))
        P[name] = (p, 1.0)

    p = Pose(1.8, loop=False)                                             # farewell: a bow, then a dissolve upward in a spin
    bowe = lambda u: trap(u, 0.0, 0.25, 0.5, 0.7)
    gone = lambda u: sm((u - 0.45) / 0.55)
    p.rot("core", lambda u: (-30 * bowe(u), 0, 0))
    p.rot("wing_L", lambda u: (0, 0, 20 * gone(u)))
    p.rot("wing_R", lambda u: (0, 0, -20 * gone(u)))
    p.scl("body", lambda u: (1.0 - 0.95 * gone(u), 1.0 - 0.6 * gone(u), 1.0 - 0.95 * gone(u)))
    p.rot("body", lambda u: (0, 260 * gone(u), 0))
    p.pos("body", lambda u: (0, 5 * gone(u), 0))
    P["farewell"] = (p, 0.5)
    return P


def clips(poses, sway_fn):
    """Bakes every pose into a clip; the sway cycles per clip follow the pose's own second number (cycles over its length)."""
    out = {}
    for name, (pose, cycles) in poses.items():
        sw = sway_fn(cycles if pose.loop else 1.0, 0.5 if not pose.loop else 1.0)
        out[name] = bake(pose, sw)
    return out
