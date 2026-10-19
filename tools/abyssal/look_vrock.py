"""Why: the VROCK, the vulture-demon: a hunched grey-blue body, a long bare neck ringed with a ruff of dirty feathers, a vulture's bald red head with a hooked yellow beak and red eyes, long arms ending
in talons, bird legs with three toes and a spur, and two huge ragged wings. Phase 2 the spore cloud of the demon rises round it, phase 3 the wings are lit along their tips by violet fire and the
eyes swell. A full GeoLite body.
"""
import math
import lib
from lib import Creature, AnimRig, over, on, shade, flat, scales, emissive, halo, wisp, bone, flesh, fur, feathers, feather_wing, TAU, keys, smooth, humanoid_motion, spike, beam

LOOK = dict(id="vrock", name="Vrock", kind="geo", height=2.3, glow="#ff4040", boost=1.0, shadow=0.9, walk_cycle=1.6, run_cycle=2.8, dust=False,
            captions=["", "Spores rise from the Vrock's wings.", "Violet fire runs along the Vrock's wings and its eyes swell."])


def build(phase):
    c = Creature("vrock", phase, tex_w=512)
    c.bone("root", pivot=(0, 0, 0))
    c.bone("body", "root", pivot=(0, 17, 0), rotation=(-10, 0, 0))
    hide = flesh("#5a5a6a", "#2a2a38", 0.02)
    pink = flesh("#c06a68", "#7a2a30", 0.025)
    ruff = feathers("#d8d4cc", "#9a948a", None, size=2)
    c.box("body", (-5.0, 15.0, -3.0), (10.0, 14.0, 6.0), hide, k=2)
    c.box("body", (-4.0, 12.0, -2.4), (8.0, 4.0, 4.8), hide, k=2)
    c.box("body", (-3.4, 17.0, -3.6), (6.8, 8.0, 1.2), flesh("#8a8a9a", "#3a3a48", 0.02), k=1)
    # neck, ruff, head
    c.bone("head", "body", pivot=(0, 29, -1))
    c.box("head", (-1.9, 28.0, -2.6), (3.8, 7.0, 3.8), pink, k=2, rot=(-14, 0, 0), pivot=(0, 28, -1))
    c.box("head", (-6.0, 27.0, -5.5), (12.0, 4.0, 10.0), ruff, k=2)
    c.box("head", (-4.4, 30.5, -6.0), (8.8, 2.4, 8.0), ruff, k=2)
    c.box("head", (-2.8, 34.0, -7.0), (5.6, 5.4, 7.0), pink, k=2)
    c.box("head", (-1.5, 32.4, -10.4), (3.0, 3.6, 4.4), over(flat("#d8b030", 0.06), on("front", lambda cv: cv.rect(0, cv.h - 2, cv.w, 2, "#8a6a10"))), k=2)       # beak
    c.box("head", (-1.0, 31.0, -11.4), (2.0, 2.4, 1.6), flat("#c89820", 0.06), k=1)                                                                                    # the hook
    c.box("head", (-0.9, 30.2, -11.8), (1.8, 1.4, 1.0), flat("#8a6a10", 0.06), k=1)
    for sx in (-1, 1):
        e = 1.7 + 0.5 * (phase >= 3)
        c.box("head", (sx * 2.5 - e / 2 - (0.0 if sx > 0 else 0.0), 36.2, -8.6), (e, e, 1.2), emissive("#ff4040", "#ff4040", soft=0.1), k=1)
        c.box("head", (sx * 2.5 - 2.5, 35.0, -9.4), (5.0, 4.0, 2.4), halo("#ff4040", 0.5), k=1)
    c.box("head", (-0.9, 29.6, -9.0), (1.8, 3.6, 1.2), pink, k=1)                                                                                                       # the wattle
    # arms: long and thin with talons
    for nm, sx in (("arm_l", 1), ("arm_r", -1)):
        c.bone(nm, "body", pivot=(sx * 6.5, 28.0, 0))
        c.box(nm, (sx * 6.5 - 1.7, 8.0, -1.7), (3.4, 21.0, 3.4), hide, k=2)
        c.box(nm, (sx * 6.5 - 2.4, 4.8, -2.4), (4.8, 4.0, 4.8), hide, k=1)
        for f in (-1, 0, 1):
            beam(c, nm, (sx * 6.5 + f * 1.5, 5.0, -2.0), 4.4, 0.9, 0.9, (-12, 0, -f * 6), bone("#2a2a30", 0.08), k=1, along="-y")
    # legs: bird legs
    for nm, sx in (("leg_l", 1), ("leg_r", -1)):
        c.bone(nm, "root", pivot=(sx * 3.2, 15.0, 0))
        c.box(nm, (sx * 3.2 - 2.0, 7.0, -2.2), (4.0, 8.4, 4.4), hide, k=2)
        c.bone("shin_" + nm[4:], nm, pivot=(sx * 3.2, 7.0, 0), rotation=(-16, 0, 0))
        c.box("shin_" + nm[4:], (sx * 3.2 - 1.2, 0.8, -1.2), (2.4, 7.0, 2.4), flesh("#8a8a9a", "#3a3a48", 0.02), k=1)
        for f in (-1, 0, 1):
            c.box("shin_" + nm[4:], (sx * 3.2 + f * 1.6 - 0.6, 0.0, -6.0), (1.2, 1.4, 6.0), bone("#2a2a30", 0.08), k=1, rot=(0, f * 14, 0), pivot=(sx * 3.2, 0.0, -1.0))
        c.box("shin_" + nm[4:], (sx * 3.2 - 0.6, 0.0, 1.0), (1.2, 1.4, 3.4), bone("#2a2a30", 0.08), k=1)
    # the wings
    f1, f2 = feathers("#3a3a46", "#5a4a6a" if phase < 3 else "#8a5ac8", "#a060ff" if phase >= 3 else None), feathers("#4a4a58", "#6a5a7a" if phase < 3 else "#a070d8", "#c090ff" if phase >= 3 else None)
    span = (36, 38, 42)[phase - 1]
    feather_wing(c, "body", "wing_r", (-4.0, 26.0, 3.0), -1, span, f1, f2, rows=5, height=span * 0.85, sweep=30, raise_=26)
    feather_wing(c, "body", "wing_l", (4.0, 26.0, 3.0), 1, span, f1, f2, rows=5, height=span * 0.85, sweep=30, raise_=26)
    # the spores
    n = (0, 5, 9)[phase - 1]
    for i in range(n):
        a = TAU * (i + 0.2) / max(1, n)
        nm = "x_spore%d" % i
        r = 11.0 + (i % 3) * 2.0
        c.bone(nm, "root", pivot=(math.cos(a) * r, 6, math.sin(a) * r))
        c.box(nm, (math.cos(a) * r - 2.0, 6 + (i % 4) * 4, math.sin(a) * r - 2.0), (4.0, 7.0, 4.0), halo("#c8ff50" if phase == 2 else "#c080ff", 0.6), k=1)
    R = RIG
    for s, sg in (("r", -1), ("l", 1)):
        R.sway("wing_" + s, 2, 6 * sg, 1, 0)
        R.sway("wing_%s2" % s, 2, 10 * sg, 1, 0.14)
    for i in range(9):
        R.sway("x_spore%d" % i, 1, 2.5, 1, i * 0.11, kind="pos")
        R.sway("x_spore%d" % i, 3, 0.22, 2, i * 0.13, kind="scale")
    R.sway("head", 0, 3, 2, 0.2)
    return c


def _extra(clip, t, L):
    d = {}
    if clip in ("cast", "cast_big", "attack"):
        up = 0.6 if clip == "cast" else (math.sin(math.pi * t / L) if clip == "attack" else keys(t / L, [(0, 0), (0.35, 1.0), (0.8, 1.2), (0.9, 0), (1, 0)]))
        for s, sg in (("r", -1), ("l", 1)):
            d["wing_" + s] = {"rot": (0, 0, 26 * sg * up)}
    return d


RIG = AnimRig(humanoid_motion(_extra, stride=24, arm_swing=22, bob=0.5, slouch=3, cast_arms=(-140, -140)))
