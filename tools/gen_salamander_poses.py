"""The new pose clips of Fuegoleon's Salamander (docs/magic/fire_casting.md, section 4.2): the small shoulder form and the big golden dragon each get a set of one-shot and looping clips that the
server picks from what happens (tools: SalamanderMoodRules in firecast/). They are ADDED to the four clips the forms already had (idle, perch, fly, breathe), which stay as they were.

    small form   grip_tighten  head_tilt  tail_flick  wing_stretch  yawn  flinch  puff_up  sleepy_curl (loop)
    big form     hover (loop)  roar  wing_display  claw_swipe  hurt  descend  land

The lengths of the one-shot clips are the durations in firecast/SalamanderMoodRules (a unit test reads both). The clips move bones by rotation, position and scale only (rotations are ADDED to the bones'
rest rotations; +x on the head and neck lifts the snout, -x on the jaw opens it, z on a wing raises it), like the clips of the first four, so every level of detail and the face plates are posed by the same
file. Keys are written densely with an ease between them so the motion starts and stops softly.

Used by tools/gen_salamander_geo.py (animations()); run `python3 -B tools/gen_salamander_geo.py --poses` to write only the animation files.
"""
import math
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import firecast_geo_lib as L  # noqa: E402

STEP = 0.1


def ease(keys, length, step=STEP, loop=False):
    """keys: [(t, (x, y, z))] sorted; a dense {time: vector} with a smoothstep ease between the keys, a key every `step` seconds and at both ends. A looping track repeats its first key at the end."""
    keys = sorted(keys, key=lambda k: k[0])
    if keys[0][0] > 0:
        keys = [(0.0, keys[0][1])] + keys
    if keys[-1][0] < length:
        keys = keys + [(length, keys[0][1] if loop else keys[-1][1])]
    out = {}
    n = int(round(length / step))
    times = sorted(set([round(i * step, 4) for i in range(n + 1)] + [round(k[0], 4) for k in keys]))
    for t in times:
        for (t0, v0), (t1, v1) in zip(keys[:-1], keys[1:]):
            if t0 <= t <= t1:
                u = 0.0 if t1 - t0 < 1e-9 else (t - t0) / (t1 - t0)
                u = u * u * (3 - 2 * u)
                out[t] = tuple(a + (b - a) * u for a, b in zip(v0, v1))
                break
    return out


def rot(bone, keys, length, loop=False):
    return {bone: {"rotation": ease(keys, length, loop=loop)}}


def merge_bones(*dicts):
    """Several {bone: {track: {...}}} dicts into one; two dicts that move the same bone track add their vectors (they must share their times)."""
    out = {}
    for d in dicts:
        for bone, tr in d.items():
            o = out.setdefault(bone, {})
            for k, v in tr.items():
                o[k] = L.merge(o[k], v) if k in o else v
    return out


def wave(length, amp, axis, phase=0.0, steps=8):
    return L.sine(length, amp, axis, phase, steps)


def clip(length, bones, loop=False):
    return {"loop": loop, "length": length, "bones": bones}


class Rig:
    """The bone names of a form (so a clip can say 'the whole tail' or 'both wings')."""

    def __init__(self, form):
        self.big = form == "dragon"
        self.necks = ["neck1", "neck2", "neck3", "neck4"] if self.big else ["neck1", "neck2"]
        self.ntail = 14 if self.big else 12
        self.tails = ["t%d" % i for i in range(self.ntail)] + ["tail_tip"]
        self.K = 2.1 if self.big else 1.0           # the big form moves by bigger numbers (its units are bigger)
        self.sides = ((-1, "R"), (1, "L"))

    def wings(self, fn):
        """fn(sx, tag) -> {bone: tracks}; merged for both sides."""
        out = {}
        for sx, tag in self.sides:
            out.update(fn(sx, tag))
        return out


def zeros(length, step=STEP):
    return {t: (0, 0, 0) for t in ease([(0, (0, 0, 0))], length, step)}


# ====================================================================================================================== the small form
def small_clips():
    R = Rig("salamander")
    clips = {}

    # ---- grip_tighten (1.4 s): the hands clench the shoulder's edge, the body sinks and presses down, the wings fold in, the tail curls tight; then it eases out
    T = 1.4
    b = {}
    b.update(rot("hand_R", [(0, (0, 0, 0)), (0.3, (-18, 0, 0)), (0.9, (-18, 0, 0)), (T, (0, 0, 0))], T))
    b.update(rot("hand_L", [(0, (0, 0, 0)), (0.3, (-18, 0, 0)), (0.9, (-18, 0, 0)), (T, (0, 0, 0))], T))
    for tag in ("R", "L"):
        b.update(rot("fore_" + tag, [(0, (0, 0, 0)), (0.3, (-10, 0, 0)), (0.9, (-10, 0, 0)), (T, (0, 0, 0))], T))
        b.update(rot("leg_" + tag, [(0, (0, 0, 0)), (0.3, (6, 0, 0)), (0.9, (6, 0, 0)), (T, (0, 0, 0))], T))
    b["body"] = {"position": ease([(0, (0, 0, 0)), (0.3, (0, -0.9, 0)), (0.9, (0, -0.9, 0)), (T, (0, 0, 0))], T),
                 "scale": ease([(0, (1, 1, 1)), (0.3, (1.02, 0.96, 1.0)), (0.9, (1.02, 0.96, 1.0)), (T, (1, 1, 1))], T)}
    b.update(rot("head", [(0, (0, 0, 0)), (0.3, (-8, 0, 0)), (0.9, (-6, 8, 0)), (T, (0, 0, 0))], T))
    for nb in R.necks:
        b.update(rot(nb, [(0, (0, 0, 0)), (0.3, (-4, 0, 0)), (0.9, (-3, 0, 0)), (T, (0, 0, 0))], T))
    for sx, tag in R.sides:
        b.update(rot("wing_" + tag, [(0, (0, 0, 0)), (0.3, (0, 0, sx * 22)), (0.9, (0, 0, sx * 22)), (T, (0, 0, 0))], T))
        b.update(rot("wingout_" + tag, [(0, (0, 0, 0)), (0.3, (0, 0, sx * 10)), (0.9, (0, 0, sx * 10)), (T, (0, 0, 0))], T))
    for i, tb in enumerate(R.tails):
        b.update(rot(tb, [(0, (0, 0, 0)), (0.45, (0, 4 + 0.8 * i, 0)), (0.9, (0, 4 + 0.8 * i, 0)), (T, (0, 0, 0))], T))
    clips["grip_tighten"] = clip(T, b)

    # ---- head_tilt (1.8 s): a curious look: the head turns and rolls to one side, the neck leans in, the tail tip twitches
    T = 1.8
    b = {}
    b.update(rot("head", [(0, (0, 0, 0)), (0.35, (-3, 14, 22)), (1.3, (-3, 14, 22)), (T, (0, 0, 0))], T))
    b.update(rot("neck1", [(0, (0, 0, 0)), (0.35, (-4, 6, 4)), (1.3, (-4, 6, 4)), (T, (0, 0, 0))], T))
    b.update(rot("neck2", [(0, (0, 0, 0)), (0.35, (-3, 6, 6)), (1.3, (-3, 6, 6)), (T, (0, 0, 0))], T))
    b.update(rot("jaw", [(0, (0, 0, 0)), (0.5, (-4, 0, 0)), (1.2, (-4, 0, 0)), (T, (0, 0, 0))], T))
    b["body"] = {"rotation": ease([(0, (0, 0, 0)), (0.4, (0, 3, 0)), (1.3, (0, 3, 0)), (T, (0, 0, 0))], T)}
    for sx, tag in R.sides:
        b.update(rot("wing_" + tag, [(0, (0, 0, 0)), (0.5, (0, 0, -sx * 5)), (0.9, (0, 0, -sx * 2)), (1.3, (0, 0, -sx * 5)), (T, (0, 0, 0))], T))
    for i, tb in enumerate(R.tails):
        b.update(rot(tb, [(0, (0, 0, 0)), (0.6, (0, 0.2 * i, 0)), (0.9, (0, -0.35 * i, 0)), (1.2, (0, 0.3 * i, 0)), (T, (0, 0, 0))], T))
    clips["head_tilt"] = clip(T, b)

    # ---- tail_flick (1.2 s): the tail curls to one side and snaps back with a flick at the barb
    T = 1.2
    b = {}
    for i, tb in enumerate(R.tails):
        k = 1.0 + i / R.ntail
        b.update(rot(tb, [(0, (0, 0, 0)), (0.3, (0, -9 * k, 0)), (0.5, (0, 12 * k, 0)), (0.75, (0, -4 * k, 0)), (T, (0, 0, 0))], T))
    b.update(rot("tail_tip", [(0, (0, 0, 0)), (0.3, (0, -26, 0)), (0.5, (0, 34, 0)), (0.75, (0, -10, 0)), (T, (0, 0, 0))], T))
    b.update(rot("head", [(0, (0, 0, 0)), (0.3, (0, 9, 0)), (0.7, (0, -4, 0)), (T, (0, 0, 0))], T))
    b["body"] = {"rotation": ease([(0, (0, 0, 0)), (0.3, (0, 0, 2)), (0.5, (0, 0, -2)), (T, (0, 0, 0))], T)}
    clips["tail_flick"] = clip(T, b)

    # ---- wing_stretch (2.2 s): the wings open wide and the fingers fan, the chest arches, a held stretch, then they fold back
    T = 2.2
    b = {}
    for sx, tag in R.sides:
        b.update(rot("wing_" + tag, [(0, (0, 0, 0)), (0.6, (-10, 0, -sx * 30)), (1.5, (-10, 0, -sx * 30)), (T, (0, 0, 0))], T))
        b.update(rot("wingout_" + tag, [(0, (0, 0, 0)), (0.6, (0, 0, -sx * 22)), (1.5, (0, 0, -sx * 22)), (T, (0, 0, 0))], T))
        for k in range(4):
            b.update(rot("wf%d_%s" % (k + 1, tag), [(0, (0, 0, 0)), (0.6, (0, sx * (-10 - 6 * k), 0)), (0.85, (0, sx * (-12 - 6 * k), 0)), (1.5, (0, sx * (-10 - 6 * k), 0)), (T, (0, 0, 0))], T))
        b.update(rot("arm_" + tag, [(0, (0, 0, 0)), (0.6, (-6, 0, 0)), (1.5, (-6, 0, 0)), (T, (0, 0, 0))], T))
    b.update(rot("head", [(0, (0, 0, 0)), (0.6, (14, 0, 0)), (1.5, (12, 0, 0)), (T, (0, 0, 0))], T))
    for nb in R.necks:
        b.update(rot(nb, [(0, (0, 0, 0)), (0.6, (6, 0, 0)), (1.5, (5, 0, 0)), (T, (0, 0, 0))], T))
    b["body"] = {"rotation": ease([(0, (0, 0, 0)), (0.6, (-4, 0, 0)), (1.5, (-3, 0, 0)), (T, (0, 0, 0))], T),
                 "scale": ease([(0, (1, 1, 1)), (0.6, (1.0, 1.04, 1.0)), (1.5, (1.0, 1.03, 1.0)), (T, (1, 1, 1))], T)}
    b.update(rot("jaw", [(0, (0, 0, 0)), (0.6, (-8, 0, 0)), (1.5, (-6, 0, 0)), (T, (0, 0, 0))], T))
    for i, tb in enumerate(R.tails):
        b.update(rot(tb, [(0, (0, 0, 0)), (0.6, (-1.0, 0, 0)), (1.5, (-1.0, 0, 0)), (T, (0, 0, 0))], T))
    clips["wing_stretch"] = clip(T, b)

    # ---- yawn (2.4 s): the head tips back, the jaw opens wide and holds, the chest fills, the eyes shut (the sleepy face), the jaw snaps closed
    T = 2.4
    b = {}
    b.update(rot("head", [(0, (0, 0, 0)), (0.7, (18, 0, 0)), (1.6, (18, 0, 0)), (T, (0, 0, 0))], T))
    b.update(rot("jaw", [(0, (-2, 0, 0)), (0.7, (-46, 0, 0)), (1.6, (-44, 0, 0)), (1.85, (-2, 0, 0)), (T, (-2, 0, 0))], T))
    for nb in R.necks:
        b.update(rot(nb, [(0, (0, 0, 0)), (0.7, (7, 0, 0)), (1.6, (7, 0, 0)), (T, (0, 0, 0))], T))
    b["body"] = {"scale": ease([(0, (1, 1, 1)), (0.7, (1.03, 1.06, 1.03)), (1.6, (1.03, 1.05, 1.03)), (T, (1, 1, 1))], T),
                 "rotation": ease([(0, (0, 0, 0)), (0.7, (-3, 0, 0)), (1.6, (-3, 0, 0)), (T, (0, 0, 0))], T)}
    for sx, tag in R.sides:
        b.update(rot("wing_" + tag, [(0, (0, 0, 0)), (0.7, (0, 0, -sx * 9)), (1.6, (0, 0, -sx * 9)), (T, (0, 0, 0))], T))
        b.update(rot("fore_" + tag, [(0, (0, 0, 0)), (0.7, (-5, 0, 0)), (1.6, (-5, 0, 0)), (T, (0, 0, 0))], T))
    for i, tb in enumerate(R.tails):
        b.update(rot(tb, [(0, (0, 0, 0)), (0.9, (0, 0.25 * i, 0)), (1.8, (0, -0.2 * i, 0)), (T, (0, 0, 0))], T))
    clips["yawn"] = clip(T, b)

    # ---- flinch (0.7 s): a hurt jerk: the body snaps back, the head throws up, the jaw gapes, the wings flare, the tail whips
    T = 0.7
    b = {}
    b["body"] = {"rotation": ease([(0, (0, 0, 0)), (0.08, (9, 0, 3)), (0.3, (3, 0, -2)), (T, (0, 0, 0))], T), "position": ease([(0, (0, 0, 0)), (0.08, (0, 0.8, -0.8)), (T, (0, 0, 0))], T),
                 "scale": ease([(0, (1, 1, 1)), (0.08, (1.03, 0.97, 1.0)), (T, (1, 1, 1))], T)}
    b.update(rot("head", [(0, (0, 0, 0)), (0.08, (16, -8, 0)), (0.3, (5, 4, 0)), (T, (0, 0, 0))], T))
    b.update(rot("jaw", [(0, (0, 0, 0)), (0.08, (-22, 0, 0)), (0.3, (-6, 0, 0)), (T, (0, 0, 0))], T))
    for nb in R.necks:
        b.update(rot(nb, [(0, (0, 0, 0)), (0.08, (8, 0, 0)), (0.3, (2, 0, 0)), (T, (0, 0, 0))], T))
    for sx, tag in R.sides:
        b.update(rot("wing_" + tag, [(0, (0, 0, 0)), (0.08, (0, 0, -sx * 20)), (0.3, (0, 0, -sx * 7)), (T, (0, 0, 0))], T))
        b.update(rot("wingout_" + tag, [(0, (0, 0, 0)), (0.08, (0, 0, -sx * 12)), (T, (0, 0, 0))], T))
        b.update(rot("hand_" + tag, [(0, (0, 0, 0)), (0.08, (-22, 0, 0)), (0.4, (-14, 0, 0)), (T, (0, 0, 0))], T))
    for i, tb in enumerate(R.tails):
        b.update(rot(tb, [(0, (0, 0, 0)), (0.1, (0, 7 + 0.8 * i, 0)), (0.3, (0, -5 - 0.6 * i, 0)), (T, (0, 0, 0))], T))
    clips["flinch"] = clip(T, b)

    # ---- puff_up (1.8 s): proud: the chest swells, the head lifts, the wings open a little, the tail rises
    T = 1.8
    b = {}
    b["body"] = {"scale": ease([(0, (1, 1, 1)), (0.45, (1.1, 1.12, 1.06)), (1.3, (1.09, 1.11, 1.05)), (T, (1, 1, 1))], T), "rotation": ease([(0, (0, 0, 0)), (0.45, (-4, 0, 0)), (1.3, (-4, 0, 0)), (T, (0, 0, 0))], T)}
    b.update(rot("head", [(0, (0, 0, 0)), (0.45, (12, 0, 0)), (1.3, (10, 6, 0)), (T, (0, 0, 0))], T))
    for nb in R.necks:
        b.update(rot(nb, [(0, (0, 0, 0)), (0.45, (6, 0, 0)), (1.3, (6, 0, 0)), (T, (0, 0, 0))], T))
    for sx, tag in R.sides:
        b.update(rot("wing_" + tag, [(0, (0, 0, 0)), (0.45, (-6, 0, -sx * 16)), (1.3, (-6, 0, -sx * 14)), (T, (0, 0, 0))], T))
        b.update(rot("wingout_" + tag, [(0, (0, 0, 0)), (0.45, (0, 0, -sx * 10)), (1.3, (0, 0, -sx * 10)), (T, (0, 0, 0))], T))
        for k in range(4):
            b.update(rot("wf%d_%s" % (k + 1, tag), [(0, (0, 0, 0)), (0.45, (0, sx * (-5 - 3 * k), 0)), (1.3, (0, sx * (-5 - 3 * k), 0)), (T, (0, 0, 0))], T))
        b.update(rot("arm_" + tag, [(0, (0, 0, 0)), (0.45, (-4, 0, 0)), (1.3, (-4, 0, 0)), (T, (0, 0, 0))], T))
    b.update(rot("jaw", [(0, (0, 0, 0)), (0.45, (-6, 0, 0)), (1.3, (-8, 0, 0)), (T, (0, 0, 0))], T))
    for i, tb in enumerate(R.tails):
        b.update(rot(tb, [(0, (0, 0, 0)), (0.5, (-1.4, 0, 0)), (1.3, (-1.4, 0, 0)), (T, (0, 0, 0))], T))
    clips["puff_up"] = clip(T, b)

    # ---- sleepy_curl (4.0 s, loops): it settles low on the shoulder, the head tucked down and to the side, the wings folded, the tail curled round it, slow breathing
    T = 4.0
    b = {}
    b["body"] = {"position": {t: (0, -1.6, 0) for t in zeros(T)}, "scale": {t: (1.0, 1.0 + 0.025 * math.sin(t / T * math.tau), 1.0 + 0.012 * math.sin(t / T * math.tau)) for t in zeros(T)},
                 "rotation": {t: (3, 0, 2) for t in zeros(T)}}
    b["head"] = {"rotation": {t: (-22 + 1.2 * math.sin(t / T * math.tau), 26, 8) for t in zeros(T)}}
    b["jaw"] = {"rotation": {t: (-1, 0, 0) for t in zeros(T)}}
    for nb in R.necks:
        b[nb] = {"rotation": {t: (-12, 10, 3) for t in zeros(T)}}
    for sx, tag in R.sides:
        b["wing_" + tag] = {"rotation": {t: (0, 0, sx * 30 + -sx * 0.8 * math.sin(t / T * math.tau)) for t in zeros(T)}}
        b["wingout_" + tag] = {"rotation": {t: (0, 0, sx * 14) for t in zeros(T)}}
        b["fore_" + tag] = {"rotation": {t: (-6, 0, 0) for t in zeros(T)}}
        b["hand_" + tag] = {"rotation": {t: (-12, 0, 0) for t in zeros(T)}}
    for i, tb in enumerate(R.tails):
        b[tb] = {"rotation": {t: (0, 10 + 0.8 * i, 0) for t in zeros(T)}}
    clips["sleepy_curl"] = clip(T, b, loop=True)
    return clips


# ====================================================================================================================== the big form
def big_clips():
    R = Rig("dragon")
    K = R.K
    clips = {}

    # ---- hover (2.8 s, loops): the base of the big form: slow strong wing beats, a calm bob, the legs tucked, the tail trailing
    T = 2.8
    b = {"body": {"position": {t: (0, 0.9 * K * math.sin(t / T * math.tau + 0.6), 0) for t in zeros(T)}},
         "head": {"rotation": wave(T, 2.5, 0, 0.2)}, "jaw": {"rotation": {t: (-2.0, 0, 0) for t in zeros(T)}}}
    for i, nb in enumerate(R.necks):
        b[nb] = {"rotation": wave(T, 1.6, 1, -0.06 * i)}
    for sx, tag in R.sides:
        b["wing_" + tag] = {"rotation": L.merge(wave(T, sx * 17, 2, 0.0), {t: (0, 0, -sx * 6) for t in zeros(T)})}
        b["wingout_" + tag] = {"rotation": wave(T, sx * 10, 2, -0.1)}
        for k in range(4):
            b["wf%d_%s" % (k + 1, tag)] = {"rotation": wave(T, sx * 4 * (0.5 + 0.35 * k), 1, -0.1 * (k + 1))}
        b["leg_" + tag] = {"rotation": wave(T, 3.0, 0, 0.1 * sx)}
        b["arm_" + tag] = {"rotation": wave(T, 2.0, 0, 0.3)}
    for i, tb in enumerate(R.tails[:-1]):
        b[tb] = {"rotation": L.merge(wave(T, 3.0 * (0.5 + 0.7 * i / R.ntail), 1, -0.05 * i), wave(T, 1.0, 0, -0.05 * i - 0.12))}
    b["tail_tip"] = {"rotation": wave(T, 3.0, 1, -0.05 * R.ntail)}
    clips["hover"] = clip(T, b, loop=True)

    # ---- roar (1.6 s): the head throws back, the jaw gapes, the chest swells, the arms open, the wings spread, the tail lashes; then the jaw closes
    T = 1.6
    b = {}
    b.update(rot("head", [(0, (0, 0, 0)), (0.3, (20, 0, 0)), (0.5, (14, 0, 0)), (1.1, (14, 0, 0)), (T, (0, 0, 0))], T))
    b.update(rot("jaw", [(0, (-2, 0, 0)), (0.3, (-18, 0, 0)), (0.5, (-48, 0, 0)), (1.1, (-46, 0, 0)), (1.4, (-6, 0, 0)), (T, (-2, 0, 0))], T))
    for i, nb in enumerate(R.necks):
        b.update(rot(nb, [(0, (0, 0, 0)), (0.3, (10 / (1 + 0.3 * i), 0, 0)), (0.5, (6, 0, 0)), (1.1, (6, 0, 0)), (T, (0, 0, 0))], T))
    b["chest"] = {"scale": ease([(0, (1, 1, 1)), (0.3, (1.1, 1.1, 1.06)), (0.5, (1.12, 1.12, 1.08)), (1.1, (1.1, 1.1, 1.06)), (T, (1, 1, 1))], T)}
    b["body"] = {"rotation": ease([(0, (0, 0, 0)), (0.3, (-5, 0, 0)), (1.1, (-4, 0, 0)), (T, (0, 0, 0))], T), "position": ease([(0, (0, 0, 0)), (0.3, (0, 1.6, 0)), (1.1, (0, 1.2, 0)), (T, (0, 0, 0))], T)}
    for sx, tag in R.sides:
        b.update(rot("arm_" + tag, [(0, (0, 0, 0)), (0.4, (-14, 0, -sx * 22)), (1.1, (-14, 0, -sx * 22)), (T, (0, 0, 0))], T))
        b.update(rot("wing_" + tag, [(0, (0, 0, 0)), (0.4, (0, 0, -sx * 26)), (1.1, (0, 0, -sx * 26)), (T, (0, 0, 0))], T))
        b.update(rot("wingout_" + tag, [(0, (0, 0, 0)), (0.4, (0, 0, -sx * 16)), (1.1, (0, 0, -sx * 16)), (T, (0, 0, 0))], T))
        for k in range(4):
            b.update(rot("wf%d_%s" % (k + 1, tag), [(0, (0, 0, 0)), (0.4, (0, sx * (-7 - 4 * k), 0)), (1.1, (0, sx * (-7 - 4 * k), 0)), (T, (0, 0, 0))], T))
    for i, tb in enumerate(R.tails[:-1]):
        b.update(rot(tb, [(0, (0, 0, 0)), (0.4, (0, 5 + 0.5 * i, 0)), (0.8, (0, -6 - 0.5 * i, 0)), (1.2, (0, 5 + 0.4 * i, 0)), (T, (0, 0, 0))], T))
    clips["roar"] = clip(T, b)

    # ---- wing_display (1.6 s): the wings open to their full span and the fingers fan, the chest out, the head up: a show of size
    T = 1.6
    b = {}
    for sx, tag in R.sides:
        b.update(rot("wing_" + tag, [(0, (0, 0, 0)), (0.5, (-8, 0, -sx * 40)), (1.2, (-8, 0, -sx * 40)), (T, (0, 0, 0))], T))
        b.update(rot("wingout_" + tag, [(0, (0, 0, 0)), (0.5, (0, 0, -sx * 24)), (1.2, (0, 0, -sx * 24)), (T, (0, 0, 0))], T))
        for k in range(4):
            b.update(rot("wf%d_%s" % (k + 1, tag), [(0, (0, 0, 0)), (0.5, (0, sx * (-12 - 6 * k), 0)), (0.8, (0, sx * (-14 - 6 * k), 0)), (1.2, (0, sx * (-12 - 6 * k), 0)), (T, (0, 0, 0))], T))
        b.update(rot("arm_" + tag, [(0, (0, 0, 0)), (0.5, (-8, 0, -sx * 18)), (1.2, (-8, 0, -sx * 18)), (T, (0, 0, 0))], T))
        b.update(rot("hand_" + tag, [(0, (0, 0, 0)), (0.5, (-10, 0, 0)), (1.2, (-10, 0, 0)), (T, (0, 0, 0))], T))
    b.update(rot("head", [(0, (0, 0, 0)), (0.5, (10, 0, 0)), (1.2, (8, 8, 0)), (T, (0, 0, 0))], T))
    for nb in R.necks:
        b.update(rot(nb, [(0, (0, 0, 0)), (0.5, (4, 0, 0)), (1.2, (4, 0, 0)), (T, (0, 0, 0))], T))
    b["chest"] = {"scale": ease([(0, (1, 1, 1)), (0.5, (1.08, 1.08, 1.05)), (1.2, (1.07, 1.07, 1.04)), (T, (1, 1, 1))], T)}
    b["body"] = {"rotation": ease([(0, (0, 0, 0)), (0.5, (-6, 0, 0)), (1.2, (-5, 0, 0)), (T, (0, 0, 0))], T), "position": ease([(0, (0, 0, 0)), (0.5, (0, 1.4, 0)), (1.2, (0, 1.4, 0)), (T, (0, 0, 0))], T)}
    for i, tb in enumerate(R.tails[:-1]):
        b.update(rot(tb, [(0, (0, 0, 0)), (0.5, (-0.8, 0, 0)), (1.2, (-0.8, 0, 0)), (T, (0, 0, 0))], T))
    clips["wing_display"] = clip(T, b)

    # ---- claw_swipe (0.9 s): the right arm winds back and rakes across, the body twists with it, the head follows, the jaw snaps
    T = 0.9
    b = {}
    b.update(rot("arm_R", [(0, (0, 0, 0)), (0.2, (-62, 52, 20)), (0.4, (-84, -64, -12)), (0.6, (-40, -40, -6)), (T, (0, 0, 0))], T))
    b.update(rot("fore_R", [(0, (0, 0, 0)), (0.2, (-50, 0, 0)), (0.4, (-12, 0, 0)), (T, (0, 0, 0))], T))
    b.update(rot("hand_R", [(0, (0, 0, 0)), (0.2, (-14, 0, 0)), (0.4, (6, 0, 0)), (T, (0, 0, 0))], T))
    b.update(rot("arm_L", [(0, (0, 0, 0)), (0.2, (-6, 0, 6)), (0.4, (-12, 0, -4)), (T, (0, 0, 0))], T))
    b["body"] = {"rotation": ease([(0, (0, 0, 0)), (0.2, (0, 14, 0)), (0.4, (-2, -12, 0)), (0.65, (0, -4, 0)), (T, (0, 0, 0))], T)}
    b.update(rot("head", [(0, (0, 0, 0)), (0.2, (0, 10, 0)), (0.4, (-6, -14, 0)), (T, (0, 0, 0))], T))
    b.update(rot("jaw", [(0, (-2, 0, 0)), (0.2, (-8, 0, 0)), (0.42, (-26, 0, 0)), (0.7, (-4, 0, 0)), (T, (-2, 0, 0))], T))
    b["chest"] = {"scale": ease([(0, (1, 1, 1)), (0.2, (1.03, 1.03, 1.02)), (0.5, (1.0, 1.0, 1.0)), (T, (1, 1, 1))], T)}
    for sx, tag in R.sides:
        b.update(rot("wing_" + tag, [(0, (0, 0, 0)), (0.2, (0, 0, -sx * 12)), (0.45, (0, 0, -sx * 4)), (T, (0, 0, 0))], T))
    for i, tb in enumerate(R.tails[:-1]):
        b.update(rot(tb, [(0, (0, 0, 0)), (0.2, (0, -4 - 0.4 * i, 0)), (0.45, (0, 5 + 0.5 * i, 0)), (T, (0, 0, 0))], T))
    clips["claw_swipe"] = clip(T, b)

    # ---- hurt (0.8 s): the body jerks back, the head flings up, the jaw gapes, the arms fly up, the wings flare, the tail whips
    T = 0.8
    b = {}
    b["body"] = {"rotation": ease([(0, (0, 0, 0)), (0.1, (10, 0, 3)), (0.35, (3, 0, -2)), (T, (0, 0, 0))], T), "position": ease([(0, (0, 0, 0)), (0.1, (0, 2.4, -1.6)), (T, (0, 0, 0))], T)}
    b.update(rot("head", [(0, (0, 0, 0)), (0.1, (22, -8, 0)), (0.35, (6, 4, 0)), (T, (0, 0, 0))], T))
    b.update(rot("jaw", [(0, (-2, 0, 0)), (0.1, (-30, 0, 0)), (0.4, (-8, 0, 0)), (T, (-2, 0, 0))], T))
    for i, nb in enumerate(R.necks):
        b.update(rot(nb, [(0, (0, 0, 0)), (0.1, (9, 0, 0)), (0.35, (2, 0, 0)), (T, (0, 0, 0))], T))
    b["chest"] = {"scale": ease([(0, (1, 1, 1)), (0.1, (0.96, 0.96, 1.0)), (T, (1, 1, 1))], T)}
    for sx, tag in R.sides:
        b.update(rot("arm_" + tag, [(0, (0, 0, 0)), (0.1, (-22, 0, -sx * 20)), (0.4, (-8, 0, -sx * 6)), (T, (0, 0, 0))], T))
        b.update(rot("wing_" + tag, [(0, (0, 0, 0)), (0.1, (0, 0, -sx * 22)), (0.4, (0, 0, -sx * 8)), (T, (0, 0, 0))], T))
        b.update(rot("wingout_" + tag, [(0, (0, 0, 0)), (0.1, (0, 0, -sx * 12)), (T, (0, 0, 0))], T))
    for i, tb in enumerate(R.tails[:-1]):
        b.update(rot(tb, [(0, (0, 0, 0)), (0.12, (0, 7 + 0.6 * i, 0)), (0.4, (0, -5 - 0.5 * i, 0)), (T, (0, 0, 0))], T))
    clips["hurt"] = clip(T, b)

    # ---- descend (0.4 s, once): it drops in from above with the wings flared wide, beating down once to brake, and ends in the hover's pose
    T = 0.4
    b = {}
    b["body"] = {"position": ease([(0, (0, 30 * K / 2.1, 0)), (0.28, (0, 3, 0)), (T, (0, 0, 0))], T), "rotation": ease([(0, (-8, 0, 0)), (0.28, (3, 0, 0)), (T, (0, 0, 0))], T)}
    for sx, tag in R.sides:
        b.update(rot("wing_" + tag, [(0, (0, 0, -sx * 34)), (0.2, (0, 0, sx * 6)), (T, (0, 0, -sx * 6))], T))
        b.update(rot("wingout_" + tag, [(0, (0, 0, -sx * 18)), (0.2, (0, 0, sx * 8)), (T, (0, 0, 0))], T))
        for k in range(4):
            b.update(rot("wf%d_%s" % (k + 1, tag), [(0, (0, sx * (-10 - 4 * k), 0)), (0.2, (0, sx * 4, 0)), (T, (0, 0, 0))], T))
        b.update(rot("arm_" + tag, [(0, (-18, 0, -sx * 14)), (T, (0, 0, 0))], T))
        b.update(rot("leg_" + tag, [(0, (-16, 0, 0)), (T, (0, 0, 0))], T))
    b.update(rot("head", [(0, (10, 0, 0)), (T, (0, 0, 0))], T))
    b.update(rot("jaw", [(0, (-14, 0, 0)), (T, (-2, 0, 0))], T))
    for i, tb in enumerate(R.tails[:-1]):
        b.update(rot(tb, [(0, (-1.6, 0, 0)), (T, (0, 0, 0))], T))
    clips["descend"] = clip(T, b)

    # ---- land (0.6 s, once): the dragon settles: the wings flare up to brake, the legs reach down, the body sinks a little, the wings fold as it ends
    T = 0.6
    b = {}
    b["body"] = {"position": ease([(0, (0, 0, 0)), (0.3, (0, -2.5, 0)), (T, (0, -6 * K / 2.1, 0))], T), "rotation": ease([(0, (0, 0, 0)), (0.3, (6, 0, 0)), (T, (4, 0, 0))], T)}
    for sx, tag in R.sides:
        b.update(rot("wing_" + tag, [(0, (0, 0, -sx * 6)), (0.22, (0, 0, -sx * 30)), (T, (0, 0, sx * 10))], T))
        b.update(rot("wingout_" + tag, [(0, (0, 0, 0)), (0.22, (0, 0, -sx * 14)), (T, (0, 0, sx * 12))], T))
        for k in range(4):
            b.update(rot("wf%d_%s" % (k + 1, tag), [(0, (0, 0, 0)), (0.22, (0, sx * (-8 - 4 * k), 0)), (T, (0, sx * 3, 0))], T))
        b.update(rot("leg_" + tag, [(0, (0, 0, 0)), (0.3, (-14, 0, 0)), (T, (-18, 0, 0))], T))
        b.update(rot("arm_" + tag, [(0, (0, 0, 0)), (0.3, (-8, 0, -sx * 8)), (T, (-4, 0, 0))], T))
    b.update(rot("head", [(0, (0, 0, 0)), (0.3, (8, 0, 0)), (T, (-4, 0, 0))], T))
    for i, tb in enumerate(R.tails[:-1]):
        b.update(rot(tb, [(0, (0, 0, 0)), (0.3, (-1.2, 0, 0)), (T, (-0.4, 0, 0))], T))
    clips["land"] = clip(T, b)
    return clips


def pose_clips(form):
    """The new clips of a form: {name: {"loop": bool, "length": s, "bones": {...}}}."""
    return big_clips() if form == "dragon" else small_clips()


if __name__ == "__main__":
    for f in ("salamander", "dragon"):
        for name, c in pose_clips(f).items():
            print("%-10s %-14s %.2f s  %s  %d bones" % (f, name, c["length"], "loop" if c["loop"] else "once", len(c["bones"])))
