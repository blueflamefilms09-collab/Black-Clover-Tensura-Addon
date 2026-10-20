"""tdanim: the one animation file every Tiny Djinn model shares (assets/nusmp/animations/entity/tinydjinn_anim.animation.json).

All 36 models have the same 25 bones (tdlib.BONES), so one file animates them all. Clips (seconds):
  idle    6.0  loop   float and breathe: the wisp tail sways, the arms and cape swing, the ears twitch, the lids blink, the accessory turns
  cast    0.6  once   the Lesser Summon's spell: the weapon arm points forward (at 0.24 s), the body leans back then forward, the free hand opens, the tail flicks
  attack  0.6  once   the dash: a wind-up, then a thrust with the weapon forward, the body leaning into it, the tail streaming behind
  hop     0.8  once   the happy hop after a kill: a crouch, a leap with both arms up and a spin of the head, a landing squash
  poof    0.5  once   the entrance: the figure pops out of nothing (scale 0.05 -> 1.12 -> 1) with a half turn and open arms
  exit    0.6  once   the exit: it turns two full circles while it shrinks and the tail stretches (the spiral into the vessel)
Rotations are degrees added to the bone's own, positions are pixels, scales multiply (a scale of 0.02 stands for "hidden": the lids at rest).
Every clip starts and ends in the rest pose (idle included), so switching clips does not jump.
Sign conventions (GeoDraw): rx > 0 swings a hanging limb FORWARD (towards -z) and leans a standing part BACK; rz > 0 swings a hanging limb towards -x; ry > 0 turns the front towards -x.
"""
import math
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
from geo_builder import Anim  # noqa: E402

ARM_R, ARM_L = 22.0, -12.0          # the idle pose holds the arms a little out (the weapon leans clear of the head); every clip keeps it so clips do not jump
CLIPS = {"idle": (6.0, True), "cast": (0.6, False), "attack": (0.6, False), "hop": (0.8, False), "poof": (0.5, False), "exit": (0.6, False)}


def _sine(a, clip, bone, kind, length, amp, cycles=1, phase=0.0, axes=(0,), steps=12, base=0.0):
    """A looped sine on the given axes: values at steps+1 keys over the clip, equal at both ends."""
    frames = {}
    for i in range(steps + 1):
        t = length * i / steps
        v = amp * math.sin(2 * math.pi * (cycles * i / steps + phase))
        # a closed loop: the last key equals the first, so the value at i == steps is the one at i == 0
        if i == steps:
            v = amp * math.sin(2 * math.pi * phase)
        vec = [0.0, 0.0, 0.0]
        for ax in axes:
            vec[ax] = round(base + v, 3)
        frames[t] = tuple(vec)
    getattr(a, kind)(clip, bone, frames)


def build():
    a = Anim()
    for name, (length, loop) in CLIPS.items():
        a.clip(name, length=length, loop=loop)

    # ---------------------------------------------------------------- idle (6 s loop)
    L = 6.0
    _sine(a, "idle", "body", "rot", L, 2.2, 2, 0.0, axes=(2,))
    _sine(a, "idle", "body", "rot", L, 1.6, 1, 0.25, axes=(0,))
    _sine(a, "idle", "head", "rot", L, 2.0, 1, 0.1, axes=(0,))
    _sine(a, "idle", "head", "rot", L, 3.0, 1, 0.0, axes=(1,))
    for i, b in enumerate(("tail1", "tail2", "tail3", "tail4")):
        _sine(a, "idle", b, "rot", L, 7.0 + 3 * i, 2, 0.12 * i, axes=(2,))
        _sine(a, "idle", b, "rot", L, 4.0 + 2 * i, 2, 0.12 * i + 0.25, axes=(0,))
    _sine(a, "idle", "arm_l", "rot", L, 5.0, 2, 0.1, axes=(2,), base=ARM_L)
    _sine(a, "idle", "arm_l", "rot", L, 4.0, 2, 0.35, axes=(0,))
    _sine(a, "idle", "arm_r", "rot", L, 3.0, 2, 0.6, axes=(2,), base=ARM_R)
    _sine(a, "idle", "arm_r", "rot", L, 3.0, 2, 0.35, axes=(0,))
    _sine(a, "idle", "cape", "rot", L, 4.0, 2, 0.15, axes=(0,))
    _sine(a, "idle", "scarf", "rot", L, 5.0, 2, 0.2, axes=(0,))
    _sine(a, "idle", "hair", "rot", L, 2.5, 2, 0.3, axes=(0,))
    _sine(a, "idle", "crown", "rot", L, 1.0, 2, 0.4, axes=(0,))
    _sine(a, "idle", "ear_l", "rot", L, 2.0, 3, 0.0, axes=(2,))
    _sine(a, "idle", "ear_r", "rot", L, 2.0, 3, 0.5, axes=(2,))
    _sine(a, "idle", "back", "rot", L, 1.5, 2, 0.5, axes=(1,))
    a.rot("idle", "acc", {0.0: (0, 0, 0), 3.0: (0, 180, 0), 6.0: (0, 360, 0)})
    a.rot("idle", "weapon", {0.0: (-6, 0, 0), 3.0: (-3, 0, 2), 6.0: (-6, 0, 0)})
    a.scale("idle", "lids", {0.0: (1, 0.02, 1), 4.7: (1, 0.02, 1), 4.78: (1, 1.0, 1), 4.9: (1, 1.0, 1), 5.0: (1, 0.02, 1), 6.0: (1, 0.02, 1)})

    # ---------------------------------------------------------------- cast (0.6 s): wind up, point at the target, hold, recover
    a.rot("cast", "arm_r", {0.0: (0, 0, 0), 0.1: (-35, 0, 8), 0.24: (92, 0, 4), 0.46: (96, 0, 2), 0.6: (0, 0, 0)})
    a.rot("cast", "weapon", {0.0: (-6, 0, 0), 0.1: (55, 0, 0), 0.24: (-182, 0, 0), 0.46: (-182, 0, 0), 0.6: (-6, 0, 0)})
    a.rot("cast", "arm_l", {0.0: (0, 0, 0), 0.16: (50, 0, -22), 0.44: (62, 0, -26), 0.6: (0, 0, 0)})
    a.rot("cast", "body", {0.0: (0, 0, 0), 0.1: (10, 0, 0), 0.26: (-12, 0, 0), 0.44: (-6, 0, 0), 0.6: (0, 0, 0)})
    a.rot("cast", "head", {0.0: (0, 0, 0), 0.24: (-8, 0, 0), 0.6: (0, 0, 0)})
    a.rot("cast", "tail1", {0.0: (0, 0, 0), 0.1: (-14, 0, 0), 0.3: (22, 0, 0), 0.6: (0, 0, 0)})
    a.rot("cast", "tail2", {0.0: (0, 0, 0), 0.14: (-18, 0, 0), 0.34: (26, 0, 0), 0.6: (0, 0, 0)})
    a.rot("cast", "tail3", {0.0: (0, 0, 0), 0.18: (-22, 0, 0), 0.38: (30, 0, 0), 0.6: (0, 0, 0)})
    a.rot("cast", "tail4", {0.0: (0, 0, 0), 0.22: (-26, 0, 0), 0.42: (34, 0, 0), 0.6: (0, 0, 0)})
    a.rot("cast", "cape", {0.0: (0, 0, 0), 0.26: (22, 0, 0), 0.6: (0, 0, 0)})
    a.rot("cast", "hair", {0.0: (0, 0, 0), 0.26: (10, 0, 0), 0.6: (0, 0, 0)})
    a.rot("cast", "acc", {0.0: (0, 0, 0), 0.6: (0, 90, 0)})

    # ---------------------------------------------------------------- attack (0.6 s): the dash (out 0.2 s, a beat at the foe, back 0.3 s)
    a.rot("attack", "arm_r", {0.0: (0, 0, 0), 0.08: (-62, 0, 14), 0.2: (88, 0, 0), 0.3: (88, 0, 0), 0.6: (0, 0, 0)})
    a.rot("attack", "weapon", {0.0: (-6, 0, 0), 0.08: (87, 0, 0), 0.2: (-178, 0, 0), 0.3: (-178, 0, 0), 0.6: (-6, 0, 0)})
    a.rot("attack", "arm_l", {0.0: (0, 0, 0), 0.08: (-30, 0, -20), 0.2: (40, 0, -30), 0.6: (0, 0, 0)})
    a.rot("attack", "body", {0.0: (0, 0, 0), 0.08: (14, 0, 0), 0.2: (-26, 0, 0), 0.3: (-22, 0, 0), 0.6: (0, 0, 0)})
    a.rot("attack", "head", {0.0: (0, 0, 0), 0.2: (-14, 0, 0), 0.6: (0, 0, 0)})
    for i, b in enumerate(("tail1", "tail2", "tail3", "tail4")):
        a.rot("attack", b, {0.0: (0, 0, 0), 0.08: (-8 - 4 * i, 0, 0), 0.2: (-28 - 8 * i, 0, 0), 0.3: (-22 - 6 * i, 0, 0), 0.6: (0, 0, 0)})
    a.rot("attack", "cape", {0.0: (0, 0, 0), 0.2: (-24, 0, 0), 0.6: (0, 0, 0)})
    a.rot("attack", "hair", {0.0: (0, 0, 0), 0.2: (18, 0, 0), 0.6: (0, 0, 0)})
    a.rot("attack", "scarf", {0.0: (0, 0, 0), 0.2: (24, 0, 0), 0.6: (0, 0, 0)})

    # ---------------------------------------------------------------- hop (0.8 s): the happy jump
    a.pos("hop", "root", {0.0: (0, 0, 0), 0.12: (0, -1.4, 0), 0.4: (0, 9.0, 0), 0.62: (0, 0.5, 0), 0.7: (0, -1.0, 0), 0.8: (0, 0, 0)})
    a.scale("hop", "root", {0.0: (1, 1, 1), 0.12: (1.08, 0.88, 1.08), 0.28: (0.94, 1.12, 0.94), 0.62: (1, 1, 1), 0.7: (1.08, 0.9, 1.08), 0.8: (1, 1, 1)})
    a.rot("hop", "arm_l", {0.0: (0, 0, 0), 0.12: (20, 0, -10), 0.34: (150, 0, -18), 0.55: (150, 0, -22), 0.8: (0, 0, 0)})
    a.rot("hop", "arm_r", {0.0: (0, 0, 0), 0.12: (20, 0, 10), 0.34: (150, 0, 18), 0.55: (150, 0, 22), 0.8: (0, 0, 0)})
    a.rot("hop", "weapon", {0.0: (-6, 0, 0), 0.34: (-156, 0, 0), 0.55: (-156, 0, 0), 0.8: (-6, 0, 0)})
    a.rot("hop", "head", {0.0: (0, 0, 0), 0.3: (10, 0, 0), 0.5: (6, 360, 0), 0.8: (0, 360, 0)})
    a.rot("hop", "body", {0.0: (0, 0, 0), 0.12: (-10, 0, 0), 0.34: (8, 0, 0), 0.8: (0, 0, 0)})
    for i, b in enumerate(("tail1", "tail2", "tail3", "tail4")):
        a.rot("hop", b, {0.0: (0, 0, 0), 0.12: (12 + 3 * i, 0, 0), 0.4: (-16 - 4 * i, 0, 8 * i), 0.62: (6, 0, 0), 0.8: (0, 0, 0)})
    a.rot("hop", "cape", {0.0: (0, 0, 0), 0.4: (-18, 0, 0), 0.8: (0, 0, 0)})
    a.rot("hop", "ear_l", {0.0: (0, 0, 0), 0.34: (0, 0, -16), 0.8: (0, 0, 0)})
    a.rot("hop", "ear_r", {0.0: (0, 0, 0), 0.34: (0, 0, 16), 0.8: (0, 0, 0)})

    # ---------------------------------------------------------------- poof (0.5 s): out of nothing
    a.scale("poof", "root", {0.0: (0.05, 0.05, 0.05), 0.22: (1.16, 1.16, 1.16), 0.34: (0.96, 0.96, 0.96), 0.5: (1, 1, 1)})
    a.rot("poof", "root", {0.0: (0, 180, 0), 0.5: (0, 0, 0)})
    a.rot("poof", "arm_l", {0.0: (0, 0, -60), 0.25: (0, 0, -40), 0.5: (0, 0, 0)})
    a.rot("poof", "arm_r", {0.0: (0, 0, 60), 0.25: (0, 0, 40), 0.5: (0, 0, 0)})
    for i, b in enumerate(("tail1", "tail2", "tail3", "tail4")):
        a.rot("poof", b, {0.0: (-18 - 6 * i, 0, 0), 0.3: (10, 0, 0), 0.5: (0, 0, 0)})
    a.rot("poof", "head", {0.0: (-12, 0, 0), 0.3: (6, 0, 0), 0.5: (0, 0, 0)})

    # ---------------------------------------------------------------- exit (0.6 s): the spiral into the vessel
    a.rot("exit", "root", {0.0: (0, 0, 0), 0.6: (0, 720, 0)})
    a.scale("exit", "root", {0.0: (1, 1, 1), 0.2: (1.1, 1.1, 1.1), 0.6: (0.05, 0.05, 0.05)})
    a.scale("exit", "tail1", {0.0: (1, 1, 1), 0.6: (0.7, 1.9, 0.7)})
    a.rot("exit", "arm_l", {0.0: (0, 0, 0), 0.3: (90, 0, -20), 0.6: (150, 0, -10)})
    a.rot("exit", "arm_r", {0.0: (0, 0, 0), 0.3: (90, 0, 20), 0.6: (150, 0, 10)})
    a.rot("exit", "weapon", {0.0: (-6, 0, 0), 0.6: (-156, 0, 0)})
    a.rot("exit", "head", {0.0: (0, 0, 0), 0.6: (-14, 0, 0)})
    # every clip keeps the idle arm stance (rz), on top of its own swing
    for name, cl in a.clips.items():
        if name == "idle":
            continue
        for arm, base in (("arm_r", ARM_R), ("arm_l", ARM_L)):
            tr = cl["bones"].setdefault(arm, {})
            rot = tr.get("rotation")
            if rot is None:
                tr["rotation"] = {"0.00": [0, 0, base], "%.2f" % cl["animation_length"]: [0, 0, base]}
            else:
                for k, v in rot.items():
                    v[2] = round(v[2] + base, 3)
    return a


def bones_used(a):
    out = set()
    for cl in a.clips.values():
        out.update(cl["bones"].keys())
    return out
