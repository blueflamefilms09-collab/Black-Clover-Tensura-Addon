"""Why: the QUASIT, the little imp-demon of the Abyss: a pot-bellied green-black body only as tall as a boy, a big head with swept-back ears and two short horns, a wide fanged grin and orange eyes, thin
clawed arms, small bat wings and a long tail with a barbed tip. Phase 2 the wings are bigger and the tail tip burns, phase 3 the horns and claws grow and a small corona of fire circles it.
A full GeoLite body (the models are drawn larger in the game: the profile's size boost scales a small creature to a boss).
"""
import math
import lib
from lib import Creature, AnimRig, over, on, shade, flat, scales, emissive, halo, wisp, bone, flesh, membrane, bat_wing, TAU, keys, smooth, humanoid_motion, spike, beam, horn

LOOK = dict(id="quasit", name="Quasit", kind="geo", height=1.1, glow="#ff8a20", boost=0.8, shadow=0.45, walk_cycle=0.8, run_cycle=1.4, dust=False,
            captions=["", "The Quasit's wings unfurl and its tail ignites.", "A ring of fire circles the Quasit as its horns and claws lengthen."])


def build(phase):
    c = Creature("quasit", phase, tex_w=256)
    c.bone("root", pivot=(0, 0, 0))
    c.bone("body", "root", pivot=(0, 6, 0))
    skin = flesh("#2c5a46", "#14301f", 0.02)
    belly = flesh("#c08a48", "#7a5020", 0.02)
    c.box("body", (-3.2, 5.0, -2.4), (6.4, 6.4, 4.8), skin, k=2)
    c.box("body", (-2.4, 5.6, -2.9), (4.8, 4.6, 0.8), belly, k=1)
    c.bone("head", "body", pivot=(0, 11.4, 0))
    c.box("head", (-3.6, 11.0, -3.4), (7.2, 6.0, 6.4), skin, k=2)
    c.box("head", (-2.2, 11.4, -4.2), (4.4, 2.6, 1.2), skin, k=1)
    c.box("head", (-2.6, 11.2, -4.0), (5.2, 1.2, 0.8), flat("#1a0a0a", 0.05), k=1)
    for i in range(5):
        c.box("head", (-2.2 + i * 1.1 - 0.3, 11.5 if i % 2 else 11.2, -4.2), (0.6, 1.0, 0.6), bone("#f0e8d0", 0.04), k=1)
    for sx in (-1, 1):
        c.box("head", (sx * 1.8 - 0.9, 14.0, -3.7), (1.8, 1.8, 0.8), emissive("#ff8a20", "#ff8a20", soft=0.1), k=1)
        c.box("head", (sx * 1.8 - 2.0, 13.0, -4.6), (4.0, 4.0, 2.0), halo("#ff8a20", 0.5), k=1)
        # big swept ears
        beam(c, "head", (sx * 3.4, 15.2, 0.4), 4.6, 2.0, 0.6, (0, 0, -sx * 62), skin, k=1)
        beam(c, "head", (sx * 3.4 + sx * 4.0, 18.2, 0.4), 2.0, 1.2, 0.5, (0, 0, -sx * 80), skin, k=1)
        g = 1.0 + 0.5 * (phase >= 3)
        horn(c, "head", (sx * 1.6, 17.0, -1.0), sx * 0.3, 0.6, [2.0 * g, 1.8 * g], [1.2, 0.8], [20, 40], bone("#e8d8b0", 0.08), k=1)
    for nm, sx in (("arm_l", 1), ("arm_r", -1)):
        c.bone(nm, "body", pivot=(sx * 3.8, 10.2, 0))
        c.box(nm, (sx * 3.8 - 0.9, 3.6, -0.9), (1.8, 7.0, 1.8), skin, k=1)
        for f in (-1, 0, 1):
            beam(c, nm, (sx * 3.8 + f * 0.6, 3.8, -0.8), (2.0 if phase < 3 else 2.8), 0.5, 0.5, (-14, 0, -f * 8), bone("#f0e8d0", 0.04), k=1, along="-y")
    for nm, sx in (("leg_l", 1), ("leg_r", -1)):
        c.bone(nm, "root", pivot=(sx * 1.8, 5.2, 0))
        c.box(nm, (sx * 1.8 - 1.1, 0.8, -1.1), (2.2, 5.0, 2.2), skin, k=1)
        c.box(nm, (sx * 1.8 - 1.4, 0.0, -3.0), (2.8, 1.0, 4.4), skin, k=1)
        for f in (-1, 0, 1):
            c.box(nm, (sx * 1.8 + f * 0.9 - 0.25, 0.0, -3.6), (0.5, 0.8, 1.0), bone("#f0e8d0", 0.04), k=1)
    span = (16, 20, 22)[phase - 1]
    bat_wing(c, "body", "wing_r", (-2.4, 9.6, 2.2), -1, span, bone("#143020", 0.08), membrane("#5a2a2a", "#2a1010"), lift=26, droop=span * 0.85, sweep=32)
    bat_wing(c, "body", "wing_l", (2.4, 9.6, 2.2), 1, span, bone("#143020", 0.08), membrane("#5a2a2a", "#2a1010"), lift=26, droop=span * 0.85, sweep=32)
    tail = c.chain("tail", "body", (0, 6.4, 2.2), "+z", [3.2, 3.2, 3.0, 3.0, 2.8, 2.6], [1.6, 1.4, 1.3, 1.2, 1.0, 0.9], skin, curl=(-12, 0, 0), k=1)
    spike(c, tail[-1], (0, 6.4, 2.2 + 17.8), 2.6, 1.8, bone("#e8d8b0", 0.08), rot=(90, 0, 0), n=2)
    if phase >= 2:
        c.bone("x_tailfire", tail[-1], pivot=(0, 6.4, 20.0))
        c.box("x_tailfire", (-1.0, 6.0, 20.4), (2.0, 5.0 + phase, 2.0), wisp("#ff8a20", 1.0), k=1)
        c.box("x_tailfire", (-2.5, 4.0, 18.5), (5.0, 7.0, 5.0), halo("#ff8a20", 0.6), k=1)
    if phase >= 3:
        for i in range(8):
            a = TAU * i / 8
            nm = "x_corona%d" % i
            c.bone(nm, "root", pivot=(math.cos(a) * 8, 3, math.sin(a) * 8))
            c.box(nm, (math.cos(a) * 8 - 0.9, 3, math.sin(a) * 8 - 0.9), (1.8, 5.0 + (i % 2) * 2, 1.8), wisp("#ff8a20", 1.0), k=1)
    R = RIG
    import beasts as B
    B.sway_names(R, tail, 1, 9, 1, 0.16, 0, 5)
    for s, sg in (("r", -1), ("l", 1)):
        R.sway("wing_" + s, 2, 12 * sg, 3, 0)
        R.sway("wing_%s2" % s, 2, 16 * sg, 3, 0.1)
    R.sway("x_tailfire", 3, 0.25, 4, 0, kind="scale")
    for i in range(8):
        R.sway("x_corona%d" % i, 3, 0.25, 3, i * 0.12, kind="scale")
    R.sway("head", 1, 5, 2, 0.1)
    return c


def _extra(clip, t, L):
    d = {}
    if clip in ("cast", "cast_big"):
        for s, sg in (("r", -1), ("l", 1)):
            d["wing_" + s] = {"rot": (0, 0, 24 * sg)}
    return d


RIG = AnimRig(humanoid_motion(_extra, stride=28, arm_swing=26, bob=0.6, cast_arms=(-150, -150)))
