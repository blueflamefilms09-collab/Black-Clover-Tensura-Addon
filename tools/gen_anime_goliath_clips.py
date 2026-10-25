"""Animation clips of the anime Goliath: idle walk slam stomp roar rise hurt death transform attack.

Pure bone rotation / position / scale clips written as 20 Hz keyframes (Catmull-Rom smoothed from a few poses, the loader interpolates linearly). Signs (tools/geo_builder.py): rotation x > 0
swings a hanging limb FORWARD (the part above the pivot goes back: a chest or head turns up / leans back), z > 0 swings it to +x (the model's left), y > 0 turns it to the model's right;
position y is up and z > 0 is back; the clip values are ADDED to the bones' own rest rotation (a slight hunch, arms a little out and forward). A leg is two links of 15 px, so a squat of
angle a (thigh +a, shin -2a, foot +a) lowers the hips by 30 (1 - cos a) and keeps the feet where they are: `squat(a)` below is that rule.
"""
import math
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from zogratis_lucius_kit import keys, cyc, SIN, COS, TAU  # noqa: E402

BONES_LR = ("pauld", "moss", "arm", "forearm", "hand", "vine", "leg", "shin", "foot")


def drop_of(a):
    return 30.0 * (1.0 - math.cos(math.radians(a)))


def R(x=0.0, y=0.0, z=0.0):
    return (x, y, z)


class C:
    """One clip under construction: tracks are lists of (time, vector) per bone and kind; emit() writes them."""

    def __init__(self, a, name, L, loop=False):
        self.a, self.name, self.L, self.loop = a, name, L, loop
        a.clip(name, L, loop)
        self.t = {}

    def add(self, bone, kind, pts):
        self.t.setdefault((bone, kind), []).extend(pts)

    def rot(self, bone, pts):
        self.add(bone, "rot", pts)

    def pos(self, bone, pts):
        self.add(bone, "pos", pts)

    def scl(self, bone, pts):
        self.add(bone, "scale", pts)

    def lr(self, base, pts, kind="rot", mirror=True):
        """The same keys on base_l and (x, -y, -z)-mirrored on base_r for a rotation, (-x, y, z) for a position."""
        for suf, s in (("_l", 1), ("_r", -1)):
            if kind == "rot":
                self.add(base + suf, kind, [(t, (v[0], v[1] * s, v[2] * s)) for t, v in pts])
            elif kind == "pos":
                self.add(base + suf, kind, [(t, (v[0] * s, v[1], v[2])) for t, v in pts])
            else:
                self.add(base + suf, kind, list(pts))

    def squat(self, ang):
        """ang: [(t, degrees)] of a symmetric squat: hips down, thighs forward, shins back, feet flat."""
        self.rot("leg_l", [(t, R(a)) for t, a in ang])
        self.rot("leg_r", [(t, R(a)) for t, a in ang])
        self.rot("shin_l", [(t, R(-2 * a)) for t, a in ang])
        self.rot("shin_r", [(t, R(-2 * a)) for t, a in ang])
        self.rot("foot_l", [(t, R(a)) for t, a in ang])
        self.rot("foot_r", [(t, R(a)) for t, a in ang])
        d = [(t, drop_of(a)) for t, a in ang]
        self.pos("body", [(t, (0, -v, 0)) for t, v in d])
        self.pos("leg_l", [(t, (0, -v, 0)) for t, v in d])
        self.pos("leg_r", [(t, (0, -v, 0)) for t, v in d])
        self.pos("tasset_c", [(t, (0, 0, 0)) for t, v in d])

    def emit(self):
        for (bone, kind), pts in self.t.items():
            pts = sorted(((float(t), tuple(float(x) for x in v)) for t, v in pts), key=lambda p: p[0])
            merged = {}
            for t, v in pts:
                merged[round(t, 4)] = v
            pts = sorted(merged.items())
            if len(pts) == 1:
                pts = [(0.0, pts[0][1]), (self.L, pts[0][1])]
            if pts[0][0] > 0.0:
                pts = [(0.0, pts[0][1])] + pts
            if pts[-1][0] < self.L - 1e-6:
                pts = pts + [(self.L, pts[-1][1])]
            keys(self.a, self.name, bone, kind, pts, smooth_=len(pts) > 2)


def secondary(c, amp=1.0, cycles=2, decay=False):
    """Soft parts that lag the body: moss, vines, tassets, the crest, the floating stones; whole cycles over the clip so loops close."""
    L = c.L
    n = 24
    for bone, ax, a_, ph in (("moss_l", 2, 3.0, 0.0), ("moss_r", 2, 3.0, 1.1), ("moss_back", 0, 3.0, 0.5), ("vine_l", 0, 7.0, 0.3), ("vine_r", 0, 7.0, 1.7),
                             ("tasset_c", 0, 2.2, 0.0), ("tasset_l", 2, 2.0, 0.6), ("tasset_r", 2, 2.0, 1.9), ("tasset_b", 0, 2.0, 0.9), ("crest", 0, 1.6, 0.4), ("ridge", 0, 1.2, 0.2)):
        pts = []
        for k in range(n + 1):
            t = L * k / n
            th = TAU * cycles * k / n
            env = 1.0 if not decay else max(0.0, 1.0 - k / n)
            v = [0.0, 0.0, 0.0]
            v[ax] = a_ * amp * env * math.sin(th + ph)
            pts.append((t, tuple(v)))
        c.rot(bone, pts)


def orbit(c, turns, bob=2.0, lift=None):
    L = c.L
    n = 24
    c.rot("orbit", [(L * k / n, (0, 360.0 * turns * k / n, 0)) for k in range(n + 1)])
    c.pos("orbit", [(L * k / n, (0, bob * math.sin(TAU * max(1, round(turns)) * k / n) + (lift(L * k / n) if lift else 0.0), 0)) for k in range(n + 1)])


# ====================================================================================================================== idle
def clip_idle(a):
    c = C(a, "idle", 8.0, True)
    L, n = c.L, 32
    for bone, kind, fn in (
            ("body", "pos", lambda t: (0, 0.5 * math.sin(t), 0)),
            ("chest", "rot", lambda t: (1.4 * math.sin(t - 0.6), 1.6 * math.sin(t / 2), 0.5 * math.sin(t / 2 + 1))),
            ("head", "rot", lambda t: (1.5 * math.sin(t - 1.0) - 1.0 * (math.sin(t / 2) > 0.2) * 0.0, 5.0 * math.sin(t / 2 + 0.4), 1.2 * math.sin(t / 2))),
            ("pauld_l", "rot", lambda t: (0, 0, 1.2 * math.sin(t - 0.4))),
            ("pauld_r", "rot", lambda t: (0, 0, -1.2 * math.sin(t - 0.4))),
            ("arm_l", "rot", lambda t: (2.0 * math.sin(t / 2), 0, 1.5 * math.sin(t))),
            ("arm_r", "rot", lambda t: (-2.0 * math.sin(t / 2), 0, -1.5 * math.sin(t))),
            ("forearm_l", "rot", lambda t: (2.0 * math.sin(t - 0.8), 0, 0)),
            ("forearm_r", "rot", lambda t: (2.0 * math.sin(t - 0.8 + 0.5), 0, 0)),
            ("hand_l", "rot", lambda t: (2.5 * math.sin(t - 1.4), 0, 0)),
            ("hand_r", "rot", lambda t: (2.5 * math.sin(t - 1.4 + 0.5), 0, 0)),
            ("leg_l", "rot", lambda t: (0.4 * math.sin(t), 0, 0)),
            ("leg_r", "rot", lambda t: (-0.4 * math.sin(t), 0, 0)),
            ("trail", "pos", lambda t: (0, 0.5 * (1 + math.sin(t * 2)) * 0.5, 0))):
        pts = [(L * k / n, fn(TAU * k / n)) for k in range(n + 1)]
        (c.rot if kind == "rot" else c.pos)(bone, pts)
    # the cycle runs 2 breaths in 8 s: theta covers 0..TAU once for the slow terms (t/2) and the fast ones use the same theta twice through the double angle
    orbit(c, 1, 1.6)
    secondary(c, 1.0, 2)
    c.emit()


# ====================================================================================================================== walk
def clip_walk(a):
    c = C(a, "walk", 2.0, True)
    L, n = c.L, 32
    legs = {"leg_l": [], "leg_r": [], "shin_l": [], "shin_r": [], "foot_l": [], "foot_r": [], "body": [], "chest": [], "arm_l": [], "arm_r": [], "forearm_l": [], "forearm_r": [],
            "head": [], "pauld_l": [], "pauld_r": [], "trail": [], "root": [], "hand_l": [], "hand_r": []}
    for k in range(n + 1):
        t = L * k / n
        th = TAU * k / n
        s, co = math.sin(th), math.cos(th)
        for side, sg in (("l", 1), ("r", -1)):
            ph = th if sg > 0 else th + math.pi
            sw = math.sin(ph)                                                  # + = leg forward
            lift = max(0.0, math.cos(ph))                                      # the swing phase (the leg comes forward)
            legs["leg_" + side].append((t, R(24 * sw + 2, 0, 0)))
            legs["shin_" + side].append((t, R(-46 * lift ** 1.2 + 8 * max(0.0, -math.cos(ph)) * 0.0, 0, 0)))
            legs["foot_" + side].append((t, R(-10 * sw + 22 * lift ** 1.2 - 4, 0, 0)))
            legs["arm_" + side].append((t, R(-17 * sw, 0, 2 * sg)))
            legs["forearm_" + side].append((t, R(10 + 14 * max(0.0, -sw), 0, 0)))
            legs["hand_" + side].append((t, R(-4 * sw, 0, 0)))
            legs["pauld_" + side].append((t, R(0, 0, 1.6 * sg * abs(sw))))
        legs["body"].append((t, (1.2 * math.sin(th * 2 + 1.3) * 0.0 + 0.0, -2.6 * abs(math.cos(th)) + 1.0, 0)))
        legs["chest"].append((t, R(1.0 * math.cos(th * 2), -6 * s, 2.0 * s)))
        legs["head"].append((t, R(-1.0 * math.cos(th * 2), 4 * s, -1.2 * s)))
        legs["trail"].append((t, (0, 1.2 * abs(math.sin(th * 2)), 0)))
        legs["root"].append((t, (0, 0, 0)))
    for bone, pts in legs.items():
        if bone in ("body", "trail"):
            c.pos(bone, [(t, v) for t, v in pts])
        elif bone == "root":
            continue
        else:
            c.rot(bone, pts)
    c.rot("body", [(L * k / n, R(0, 4 * math.sin(TAU * k / n), -2.5 * math.sin(TAU * k / n))) for k in range(n + 1)])
    orbit(c, 1, 2.2)
    secondary(c, 1.5, 2)
    c.emit()


# ====================================================================================================================== slam
def clip_slam(a):
    c = C(a, "slam", 2.2, False)
    T = (0.0, 0.35, 0.7, 0.95, 1.05, 1.15, 1.35, 1.6, 1.9, 2.2)
    # arms: down -> overhead -> trembling at the top -> down hard -> hold -> recover
    arm = (R(0, 0, 0), R(70, 0, 8), R(172, 0, 12), R(178, 0, 10), R(160, 0, 8), R(20, 0, 6), R(30, 0, 8), R(26, 0, 7), R(10, 0, 3), R(0, 0, 0))
    fore = (R(0, 0, 0), R(20, 0, 0), R(-14, 0, 0), R(-18, 0, 0), R(-4, 0, 0), R(16, 0, 0), R(8, 0, 0), R(10, 0, 0), R(4, 0, 0), R(0, 0, 0))
    hand = (R(0, 0, 0), R(10, 0, 0), R(20, 0, 0), R(24, 0, 0), R(10, 0, 0), R(-6, 0, 0), R(-2, 0, 0), R(0, 0, 0), R(0, 0, 0), R(0, 0, 0))
    chest = (R(0, 0, 0), R(10, 0, 0), R(18, 0, 0), R(22, 0, 0), R(-4, 0, 0), R(-30, 0, 0), R(-26, 0, 0), R(-20, 0, 0), R(-8, 0, 0), R(0, 0, 0))
    head = (R(0, 0, 0), R(-6, 0, 0), R(-14, 0, 0), R(-14, 0, 0), R(6, 0, 0), R(22, 0, 0), R(18, 0, 0), R(12, 0, 0), R(4, 0, 0), R(0, 0, 0))
    c.lr("arm", list(zip(T, arm)))
    c.lr("forearm", list(zip(T, fore)))
    c.lr("hand", list(zip(T, hand)))
    c.rot("chest", list(zip(T, chest)))
    c.rot("head", list(zip(T, head)))
    c.lr("pauld", list(zip(T, (R(), R(0, 0, 4), R(0, 0, 10), R(0, 0, 10), R(0, 0, 6), R(0, 0, -4), R(0, 0, -2), R(0, 0, -2), R(), R()))))
    c.squat(list(zip(T, (0, 6, 14, 14, 26, 36, 32, 24, 10, 0))))
    # the impact: a fast shudder of the whole giant
    c.pos("root", [(0.0, (0, 0, 0)), (1.05, (0, 0, 0)), (1.1, (0, 2.5, 0)), (1.17, (0, -1.5, 0)), (1.25, (0, 1.2, 0)), (1.33, (0, -0.8, 0)), (1.45, (0, 0.4, 0)), (1.6, (0, 0, 0)), (2.2, (0, 0, 0))])
    c.pos("rubble", [(0.0, (0, 0, 0)), (1.05, (0, 0, 0)), (1.14, (0, 3.5, 0)), (1.3, (0, 0.6, 0)), (1.5, (0, 0, 0)), (2.2, (0, 0, 0))])
    c.pos("trail", [(0.0, (0, 0, 0)), (1.05, (0, 0, 0)), (1.18, (0, 4.0, 0)), (1.4, (0, 0.6, 0)), (1.7, (0, 0, 0)), (2.2, (0, 0, 0))])
    orbit(c, 2, 3.0, lambda t: 6.0 * math.sin(math.pi * min(1.0, t / 1.1)) if t < 1.1 else 0.0)
    secondary(c, 2.0, 3, decay=False)
    c.emit()


# ====================================================================================================================== stomp
def clip_stomp(a):
    c = C(a, "stomp", 3.0, False)
    T = (0.0, 0.45, 0.85, 1.0, 1.1, 1.3, 1.6, 2.0, 2.15, 2.25, 2.5, 3.0)
    # right foot lifts and stamps at 1.0 s, left foot lifts and stamps at 2.15 s; the weight shifts onto the standing leg
    lr = (R(), R(58, 0, 0), R(74, 0, 0), R(6, 0, 0), R(2, 0, 0), R(0, 0, 0), R(0, 0, 0), R(0, 0, 0), R(0, 0, 0), R(0, 0, 0), R(0, 0, 0), R(0, 0, 0))
    leg_r = (R(), R(58, 0, 0), R(78, 0, 0), R(4, 0, 0), R(0, 0, 0), R(0, 0, 0), R(0, 0, 0), R(0, 0, 0), R(0, 0, 0), R(0, 0, 0), R(0, 0, 0), R(0, 0, 0))
    leg_l = (R(), R(0, 0, 0), R(0, 0, 0), R(0, 0, 0), R(0, 0, 0), R(0, 0, 0), R(10, 0, 0), R(60, 0, 0), R(80, 0, 0), R(4, 0, 0), R(0, 0, 0), R(0, 0, 0))
    c.rot("leg_r", list(zip(T, leg_r)))
    c.rot("leg_l", list(zip(T, leg_l)))
    sh_r = (R(), R(-80, 0, 0), R(-96, 0, 0), R(-6, 0, 0), R(), R(), R(), R(), R(), R(), R(), R())
    sh_l = (R(), R(), R(), R(), R(), R(), R(-20, 0, 0), R(-80, 0, 0), R(-100, 0, 0), R(-6, 0, 0), R(), R())
    c.rot("shin_r", list(zip(T, sh_r)))
    c.rot("shin_l", list(zip(T, sh_l)))
    c.rot("foot_r", list(zip(T, (R(), R(30, 0, 0), R(40, 0, 0), R(-8, 0, 0), R(), R(), R(), R(), R(), R(), R(), R()))))
    c.rot("foot_l", list(zip(T, (R(), R(), R(), R(), R(), R(), R(), R(30, 0, 0), R(40, 0, 0), R(-8, 0, 0), R(), R()))))
    # body: sink onto the standing leg, rise with the lifted knee, slam down on the stamp (hips drop with the foot)
    c.pos("body", list(zip(T, ((0, 0, 0), (0, 3, 0), (0, 5, 0), (0, -6, 0), (0, -3, 0), (0, 0, 0), (0, 0, 0), (0, 5, 0), (0, 6, 0), (0, -6, 0), (0, -2, 0), (0, 0, 0)))))
    c.rot("body", list(zip(T, (R(), R(0, 0, -3), R(0, 0, -5), R(4, 0, 0), R(), R(), R(0, 0, 3), R(0, 0, 5), R(0, 0, 4), R(4, 0, 0), R(), R()))))
    c.pos("leg_l", list(zip(T, ((0, 0, 0), (0, 0, 0), (0, 0, 0), (0, -6, 0), (0, -3, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, -6, 0), (0, -2, 0), (0, 0, 0)))))
    c.pos("leg_r", list(zip(T, ((0, 0, 0), (0, 0, 0), (0, 0, 0), (0, -6, 0), (0, -3, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, -6, 0), (0, -2, 0), (0, 0, 0)))))
    c.rot("chest", list(zip(T, (R(), R(8, 0, 0), R(12, 0, 0), R(-18, 0, 0), R(-8, 0, 0), R(), R(8, 0, 0), R(12, 0, 0), R(10, 0, 0), R(-18, 0, 0), R(-8, 0, 0), R()))))
    c.rot("head", list(zip(T, (R(), R(-4, 0, 0), R(-8, 0, 0), R(10, 0, 0), R(4, 0, 0), R(), R(-4, 0, 0), R(-8, 0, 0), R(-8, 0, 0), R(10, 0, 0), R(4, 0, 0), R()))))
    # arms spread for balance, then slam down with each stamp
    c.lr("arm", list(zip(T, (R(), R(10, 0, 38), R(16, 0, 56), R(-6, 0, 10), R(0, 0, 6), R(0, 0, 4), R(10, 0, 30), R(16, 0, 56), R(16, 0, 60), R(-6, 0, 10), R(0, 0, 6), R()))))
    c.lr("forearm", list(zip(T, (R(), R(24, 0, 0), R(40, 0, 0), R(0, 0, 0), R(6, 0, 0), R(4, 0, 0), R(24, 0, 0), R(40, 0, 0), R(40, 0, 0), R(0, 0, 0), R(6, 0, 0), R()))))
    c.lr("pauld", list(zip(T, (R(), R(0, 0, 4), R(0, 0, 8), R(0, 0, -3), R(), R(), R(0, 0, 4), R(0, 0, 8), R(0, 0, 8), R(0, 0, -3), R(), R()))))
    c.pos("root", [(0.0, (0, 0, 0)), (1.0, (0, 0, 0)), (1.05, (0, 2.2, 0)), (1.13, (0, -1.2, 0)), (1.22, (0, 0.8, 0)), (1.4, (0, 0, 0)), (2.15, (0, 0, 0)), (2.2, (0, 2.2, 0)), (2.28, (0, -1.2, 0)), (2.38, (0, 0.8, 0)),
                    (2.6, (0, 0, 0)), (3.0, (0, 0, 0))])
    c.pos("rubble", [(0.0, (0, 0, 0)), (1.0, (0, 0, 0)), (1.1, (0, 3.5, 0)), (1.3, (0, 0, 0)), (2.15, (0, 0, 0)), (2.25, (0, 3.5, 0)), (2.45, (0, 0, 0)), (3.0, (0, 0, 0))])
    orbit(c, 2, 2.5)
    secondary(c, 2.0, 3)
    c.emit()


# ====================================================================================================================== roar
def clip_roar(a):
    c = C(a, "roar", 2.4, False)
    T = (0.0, 0.4, 0.75, 1.0, 1.4, 1.7, 2.0, 2.4)
    c.rot("chest", list(zip(T, (R(), R(-10, 0, 0), R(8, 0, 0), R(16, 0, 0), R(16, 0, 0), R(14, 0, 0), R(4, 0, 0), R()))))
    c.rot("head", list(zip(T, (R(), R(-12, 0, 0), R(18, 0, 0), R(28, 0, 0), R(28, 0, 0), R(26, 0, 0), R(6, 0, 0), R()))))
    c.lr("arm", list(zip(T, (R(), R(14, 0, 4), R(28, 0, 42), R(32, 0, 62), R(32, 0, 62), R(30, 0, 56), R(8, 0, 12), R()))))
    c.lr("forearm", list(zip(T, (R(), R(24, 0, 0), R(46, 0, 0), R(54, 0, 0), R(54, 0, 0), R(48, 0, 0), R(12, 0, 0), R()))))
    c.lr("hand", list(zip(T, (R(), R(10, 0, 0), R(-14, 0, 0), R(-20, 0, 0), R(-20, 0, 0), R(-16, 0, 0), R(), R()))))
    c.lr("pauld", list(zip(T, (R(), R(0, 0, -2), R(0, 0, 10), R(0, 0, 14), R(0, 0, 14), R(0, 0, 12), R(0, 0, 3), R()))))
    c.pos("pauld_l", list(zip(T, ((0, 0, 0), (0, -1, 0), (0, 2.5, 0), (0, 3.5, 0), (0, 3.5, 0), (0, 3, 0), (0, 0.5, 0), (0, 0, 0)))))
    c.pos("pauld_r", list(zip(T, ((0, 0, 0), (0, -1, 0), (0, 2.5, 0), (0, 3.5, 0), (0, 3.5, 0), (0, 3, 0), (0, 0.5, 0), (0, 0, 0)))))
    c.squat(list(zip(T, (0, 10, 8, 12, 12, 10, 3, 0))))
    # the shout shakes the whole body: a fast tremor while the roar holds (1.0 .. 1.7 s)
    pts = [(0.0, (0, 0, 0))]
    for k in range(0, 15):
        t = 1.0 + 0.05 * k
        pts.append((t, (0.9 * math.sin(k * 2.3), 0.7 * math.sin(k * 3.1 + 1), 0)))
    pts += [(1.8, (0, 0, 0)), (2.4, (0, 0, 0))]
    c.pos("root", pts)
    c.scl("chest", list(zip(T, ((1, 1, 1), (0.98, 0.98, 0.98), (1.03, 1.03, 1.03), (1.06, 1.06, 1.06), (1.06, 1.06, 1.06), (1.05, 1.05, 1.05), (1.01, 1.01, 1.01), (1, 1, 1)))))
    c.scl("crest", list(zip(T, ((1, 1, 1), (1, 1, 1), (1.15, 1.15, 1.15), (1.3, 1.3, 1.3), (1.3, 1.3, 1.3), (1.25, 1.25, 1.25), (1.05, 1.05, 1.05), (1, 1, 1)))))
    orbit(c, 3, 3.0)
    secondary(c, 2.5, 4)
    c.emit()


# ====================================================================================================================== rise
def clip_rise(a):
    c = C(a, "rise", 2.4, False)
    T = (0.0, 0.3, 0.7, 1.1, 1.5, 1.9, 2.4)
    # dormant: half sunk in the rubble, hunched over its knees, head down; it pushes up on its fists, stands, and ends in the idle stance
    c.pos("root", list(zip(T, ((0, -26, 0), (0, -26, 0), (0, -24, 0), (0, -12, 0), (0, -3, 0), (0, 0, 0), (0, 0, 0)))))
    c.squat(list(zip(T, (58, 58, 56, 46, 22, 6, 0))))
    c.rot("chest", list(zip(T, (R(-42, 0, 0), R(-42, 0, 0), R(-40, 0, 0), R(-26, 0, 0), R(-4, 0, 0), R(10, 0, 0), R()))))
    c.rot("head", list(zip(T, (R(30, 0, 0), R(30, 0, 0), R(26, 0, 0), R(14, 0, 0), R(0, 0, 0), R(-8, 0, 0), R()))))
    c.lr("arm", list(zip(T, (R(48, 0, -8), R(48, 0, -8), R(54, 0, -4), R(60, 0, 8), R(20, 0, 28), R(26, 0, 22), R()))))
    c.lr("forearm", list(zip(T, (R(14, 0, 0), R(14, 0, 0), R(10, 0, 0), R(0, 0, 0), R(18, 0, 0), R(26, 0, 0), R()))))
    c.lr("pauld", list(zip(T, (R(0, 0, -6), R(0, 0, -6), R(0, 0, -4), R(0, 0, 2), R(0, 0, 8), R(0, 0, 5), R()))))
    # a shudder when it wakes (0.3 .. 0.9 s)
    pts = [(0.0, (0, 0, 0)), (0.3, (0, 0, 0))]
    for k in range(0, 13):
        t = 0.3 + 0.05 * k
        pts.append((t, (0.8 * math.sin(k * 2.9) * (1 - k / 14.0), 0.6 * math.sin(k * 2.1 + 1) * (1 - k / 14.0), 0)))
    pts += [(1.2, (0, 0, 0)), (2.4, (0, 0, 0))]
    c.pos("body", pts)
    c.pos("orbit", list(zip(T, ((0, -70, 0), (0, -70, 0), (0, -50, 0), (0, -26, 0), (0, -6, 0), (0, 0, 0), (0, 0, 0)))))
    c.rot("orbit", [(2.4 * k / 24, (0, 360.0 * 2 * k / 24, 0)) for k in range(25)])
    secondary(c, 2.0, 3)
    c.emit()


# ====================================================================================================================== hurt
def clip_hurt(a):
    c = C(a, "hurt", 0.6, False)
    T = (0.0, 0.08, 0.2, 0.38, 0.6)
    c.rot("chest", list(zip(T, (R(), R(14, 0, 0), R(8, 0, 2), R(2, 0, 0), R()))))
    c.rot("head", list(zip(T, (R(), R(20, 6, 0), R(10, -4, 0), R(2, 0, 0), R()))))
    c.lr("arm", list(zip(T, (R(), R(-14, 0, 12), R(-6, 0, 8), R(-2, 0, 2), R()))))
    c.lr("forearm", list(zip(T, (R(), R(10, 0, 0), R(6, 0, 0), R(2, 0, 0), R()))))
    c.lr("pauld", list(zip(T, (R(), R(0, 0, 6), R(0, 0, 3), R(), R()))))
    c.pos("body", list(zip(T, ((0, 0, 0), (0, -1.5, 3.0), (0, -0.6, 1.4), (0, 0, 0.4), (0, 0, 0)))))
    c.squat(list(zip(T, (0, 8, 5, 2, 0))))
    secondary(c, 2.0, 2)
    c.emit()


# ====================================================================================================================== death
def clip_death(a):
    c = C(a, "death", 3.0, False)
    T = (0.0, 0.3, 0.8, 1.3, 1.8, 2.2, 2.6, 3.0)
    c.squat(list(zip(T, (0, 4, 30, 62, 66, 66, 66, 66))))
    c.rot("chest", list(zip(T, (R(), R(14, 0, 0), R(6, 0, 0), R(-30, 0, 0), R(-48, 0, 0), R(-56, 0, 0), R(-58, 0, 0), R(-58, 0, 0)))))
    c.rot("head", list(zip(T, (R(), R(24, 6, 0), R(12, 0, 0), R(-10, 0, 0), R(-22, 0, 0), R(-28, 0, 0), R(-30, 0, 0), R(-30, 0, 0)))))
    c.lr("arm", list(zip(T, (R(), R(-12, 0, 16), R(0, 0, 26), R(20, 0, 24), R(46, 0, 18), R(58, 0, 14), R(60, 0, 12), R(60, 0, 12)))))
    c.lr("forearm", list(zip(T, (R(), R(6, 0, 0), R(14, 0, 0), R(26, 0, 0), R(20, 0, 0), R(16, 0, 0), R(14, 0, 0), R(14, 0, 0)))))
    c.lr("pauld", list(zip(T, (R(), R(0, 0, 8), R(0, 0, 6), R(0, 0, -4), R(0, 0, -10), R(0, 0, -12), R(0, 0, -12), R(0, 0, -12)))))
    # the whole giant pitches forward over its knees: pivot at the feet, so lift it as it falls to keep the front of the chest on the ground
    c.rot("root", list(zip(T, (R(), R(), R(), R(-4, 0, 0), R(-24, 0, 0), R(-46, 0, 0), R(-52, 0, 0), R(-52, 0, 0)))))
    c.pos("root", list(zip(T, ((0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, -2), (0, 2, -6), (0, 4, -9), (0, 4, -9)))))
    c.pos("body", list(zip(T, ((0, 0, 0), (0, 0, 3), (0, 0, 2), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0)))))
    c.pos("orbit", list(zip(T, ((0, 0, 0), (0, 2, 0), (0, 5, 0), (0, 2, 0), (0, -20, 0), (0, -70, 0), (0, -88, 0), (0, -88, 0)))))
    c.rot("orbit", [(3.0 * k / 24, (0, 360.0 * 2 * k / 24 * (1 - k / 28.0), 0)) for k in range(25)])
    c.scl("crest", list(zip(T, ((1, 1, 1),) * 3 + ((0.9, 0.9, 0.9), (0.7, 0.7, 0.7), (0.6, 0.6, 0.6), (0.6, 0.6, 0.6), (0.6, 0.6, 0.6)))))
    secondary(c, 2.0, 3, decay=True)
    c.emit()


# ====================================================================================================================== transform
def clip_transform(a):
    c = C(a, "transform", 3.4, False)
    T = (0.0, 0.5, 0.9, 1.3, 1.7, 2.1, 2.6, 3.0, 3.4)
    c.squat(list(zip(T, (0, 22, 30, 18, 4, 8, 6, 2, 0))))
    c.rot("chest", list(zip(T, (R(), R(-16, 0, 0), R(-22, 0, 0), R(6, 0, 0), R(18, 0, 0), R(16, 0, 0), R(12, 0, 0), R(4, 0, 0), R()))))
    c.rot("head", list(zip(T, (R(), R(-10, 0, 0), R(-20, 0, 0), R(10, 0, 0), R(30, 0, 0), R(28, 0, 0), R(22, 0, 0), R(6, 0, 0), R()))))
    c.lr("arm", list(zip(T, (R(), R(20, 0, 6), R(30, 0, 4), R(26, 0, 40), R(24, 0, 72), R(24, 0, 70), R(20, 0, 58), R(6, 0, 12), R()))))
    c.lr("forearm", list(zip(T, (R(), R(30, 0, 0), R(40, 0, 0), R(50, 0, 0), R(62, 0, 0), R(60, 0, 0), R(50, 0, 0), R(14, 0, 0), R()))))
    c.lr("hand", list(zip(T, (R(), R(), R(), R(-12, 0, 0), R(-24, 0, 0), R(-24, 0, 0), R(-16, 0, 0), R(), R()))))
    c.lr("pauld", list(zip(T, (R(), R(0, 0, -4), R(0, 0, -6), R(0, 0, 10), R(0, 0, 18), R(0, 0, 18), R(0, 0, 14), R(0, 0, 4), R()))))
    c.pos("pauld_l", list(zip(T, ((0, 0, 0), (0, -1, 0), (0, -2, 0), (0, 3, 0), (0, 5, 0), (0, 5, 0), (0, 4, 0), (0, 1, 0), (0, 0, 0)))))
    c.pos("pauld_r", list(zip(T, ((0, 0, 0), (0, -1, 0), (0, -2, 0), (0, 3, 0), (0, 5, 0), (0, 5, 0), (0, 4, 0), (0, 1, 0), (0, 0, 0)))))
    # the stone swells (growth) while the runes flare: the root scale rises to 1.07 and settles
    s = (1.0, 1.0, 1.0, 1.03, 1.07, 1.07, 1.05, 1.02, 1.0)
    c.scl("root", list(zip(T, ((v, v, v) for v in s))))
    pts = [(0.0, (0, 0, 0))]
    for k in range(0, 24):
        t = 0.3 + 0.05 * k
        amp = 1.4 * (1 - abs(t - 1.2) / 1.0) if abs(t - 1.2) < 1.0 else 0.0
        pts.append((t, (amp * math.sin(k * 3.7), amp * 0.7 * math.sin(k * 2.7 + 1), 0)))
    pts += [(1.6, (0, 0, 0)), (3.4, (0, 0, 0))]
    c.pos("root", sorted(set(pts), key=lambda p: p[0]))
    c.scl("crest", list(zip(T, ((1, 1, 1), (1, 1, 1), (1, 1, 1), (1.2, 1.2, 1.2), (1.5, 1.5, 1.5), (1.5, 1.5, 1.5), (1.4, 1.4, 1.4), (1.2, 1.2, 1.2), (1.1, 1.1, 1.1)))))
    c.rot("orbit", [(3.4 * k / 24, (0, 360.0 * 4 * (k / 24.0) ** 1.4, 0)) for k in range(25)])
    c.pos("orbit", list(zip(T, ((0, 0, 0), (0, 1, 0), (0, 2, 0), (0, 6, 0), (0, 10, 0), (0, 10, 0), (0, 8, 0), (0, 4, 0), (0, 0, 0)))))
    secondary(c, 3.0, 5)
    c.emit()


# ====================================================================================================================== attack (a right-fist haymaker)
def clip_attack(a):
    c = C(a, "attack", 1.2, False)
    T = (0.0, 0.3, 0.55, 0.7, 0.8, 1.0, 1.2)
    c.rot("arm_r", list(zip(T, (R(), R(-46, -10, -34), R(-70, 0, -48), R(70, 0, 8), R(48, 0, -14), R(16, 0, -6), R()))))
    c.rot("forearm_r", list(zip(T, (R(), R(40, 0, 0), R(70, 0, 0), R(-4, 0, 0), R(0, 0, 0), R(6, 0, 0), R()))))
    c.rot("chest", list(zip(T, (R(), R(8, 0, 0), R(14, 22, 0), R(-20, -26, 0), R(-14, -22, 0), R(-4, -8, 0), R()))))
    c.rot("head", list(zip(T, (R(), R(-4, 0, 0), R(-8, -10, 0), R(10, 14, 0), R(6, 10, 0), R(2, 3, 0), R()))))
    c.lr("arm", list(zip(T, (R(), R(10, 0, 8), R(12, 0, 10), R(-4, 0, 6), R(), R(), R())))) if False else None
    c.rot("arm_l", list(zip(T, (R(), R(10, 0, 8), R(14, 0, 10), R(4, 0, 6), R(2, 0, 4), R(), R()))))
    c.rot("pauld_r", list(zip(T, (R(), R(0, 0, -6), R(0, 0, -10), R(0, 0, 6), R(0, 0, 3), R(), R()))))
    c.squat(list(zip(T, (0, 10, 14, 20, 14, 6, 0))))
    secondary(c, 1.5, 2)
    c.emit()


def all_clips(a):
    for fn in (clip_idle, clip_walk, clip_slam, clip_stomp, clip_roar, clip_rise, clip_hurt, clip_death, clip_transform, clip_attack):
        fn(a)
