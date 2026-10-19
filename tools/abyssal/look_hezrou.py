"""Why: the HEZROU, the toad-demon of the Abyss: a hulking, squat, green-brown body with a yellow belly, a huge flat head with a mouth from ear to ear and bulging yellow eyes on top, thick webbed arms
that nearly touch the floor, a ridge of spines and warts, and a cloud of stench round it. Phase 2 the spines glow and acid drips from the mouth, phase 3 the crest grows, the eyes swell and the
stench thickens. A full GeoLite body (a demon of the abyss, not human-shaped).
"""
import math
import lib
from lib import Creature, AnimRig, over, on, shade, flat, scales, emissive, halo, wisp, bone, flesh, TAU, keys, smooth, humanoid_motion, spike, beam

LOOK = dict(id="hezrou", name="Hezrou", kind="geo", height=2.3, glow="#b8e030", boost=1.0, shadow=1.0, walk_cycle=1.5, run_cycle=2.6, dust=False,
            captions=["", "The spines of the Hezrou glow and acid drips from its jaw.", "The Hezrou swells: eyes huge, crest raised, the stench so thick the air turns green."])


def build(phase):
    c = Creature("hezrou", phase, tex_w=512)
    c.bone("root", pivot=(0, 0, 0))
    c.bone("body", "root", pivot=(0, 14, 0))
    skin = over(flesh("#5a6a2a", "#2a3a12", 0.02), on(("front", "back", "left", "right", "top"), lambda cv: [cv.rect(x, y, 2, 2, "#3a4a18") for x in range(1, cv.w - 2, 6) for y in range(1 + (x * 5) % 4, cv.h - 2, 7)]))
    belly = flesh("#b0a050", "#6a6a28", 0.02)
    c.box("body", (-9.5, 9.0, -7.0), (19.0, 17.0, 14.0), skin, k=2)
    c.box("body", (-7.5, 9.5, -7.9), (15.0, 14.0, 1.4), belly, k=2)
    c.box("body", (-8.5, 22.0, -6.0), (17.0, 4.0, 12.0), skin, k=2)
    # head: broad and flat, the mouth across the whole front
    c.bone("head", "body", pivot=(0, 25, 0))
    c.box("head", (-9.0, 24.0, -8.5), (18.0, 6.5, 13.0), skin, k=2)
    c.box("head", (-8.0, 30.0, -7.0), (16.0, 2.5, 10.0), skin, k=2)
    c.box("head", (-8.6, 25.4, -9.4), (17.2, 1.4, 1.2), flat("#2a0e0e", 0.05), k=1)
    for i in range(9):
        c.box("head", (-7.4 + i * 1.85, 24.6 - (1.0 if i % 2 else 0.0), -9.8), (0.9, 1.6 if i % 2 else 2.4, 0.9), bone("#e8e0b8", 0.06), k=1)
    c.bone("jaw", "head", pivot=(0, 24.5, -6))
    c.box("jaw", (-8.5, 20.0, -9.0), (17.0, 4.2, 11.0), skin, k=2)
    c.box("jaw", (-7.5, 24.0, -8.6), (15.0, 0.8, 9.0), flat("#7a1a22", 0.08), k=1)
    for i in range(8):
        c.box("jaw", (-6.6 + i * 1.9, 24.0, -9.6), (0.9, 2.0 if i % 2 else 1.4, 0.9), bone("#e8e0b8", 0.06), k=1)
    for sx in (-1, 1):
        sz = 5.0 + 0.8 * (phase - 1)
        c.box("head", (sx * 5.2 - sz / 2, 31.0, -4.5), (sz, sz, sz), over(flat("#e8d030", 0.05), on(("front",), lambda cv: cv.rect(cv.w // 2 - 1, 0, 2, cv.h, "#101008", (0, 0, 0)))), k=2)
        c.box("head", (sx * 5.2 - sz / 2 - 1.5, 30.0, -6.0), (sz + 3, sz + 3, sz + 3), halo("#e8d030", 0.45), k=1)
        c.box("head", (sx * 7.4 - 0.9, 29.5, -3.5), (1.8, 1.8, 1.8), flat("#3a4a18", 0.05), k=1)
    for i in range(7):                                                  # warts
        c.box("head", (-7 + (i * 5) % 14, 30.5 + (i % 2) * 0.3, 1.0 - (i * 3) % 7), (1.4, 1.2, 1.4), flat("#7a8a38", 0.05), k=1)
    # the ridge of spines
    n = 5 + (2 if phase >= 3 else 0)
    for i in range(n):
        h = (4.4 if phase < 3 else 6.0) - 0.3 * i
        spike(c, "body", (-8.0 + i * (16.0 / (n - 1)), 26.0, 5.0), h, 2.2, over(bone("#c8b878", 0.08), on(("front", "back"), lambda cv: cv.rect(cv.w // 2 - 1, 1, 2, cv.h - 2, "#c8e030", "#c8e030") if phase >= 2 else None)), rot=(30, 0, 0), n=2)
    # arms: long, thick, webbed
    for nm, sx in (("arm_l", 1), ("arm_r", -1)):
        c.bone(nm, "body", pivot=(sx * 11.5, 24.0, 0))
        c.box(nm, (sx * 11.5 - 3.5, 8.0, -3.5), (7.0, 17.0, 7.0), skin, k=2)
        c.box(nm, (sx * 11.5 - 4.0, 3.0, -4.5), (8.0, 5.5, 9.0), skin, k=2)
        for f in (-1, 0, 1):
            c.box(nm, (sx * 11.5 + f * 2.6 - 0.9, 0.4, -7.8), (1.8, 2.6, 3.4), bone("#c8b878", 0.08), k=1)
    for nm, sx in (("leg_l", 1), ("leg_r", -1)):
        c.bone(nm, "root", pivot=(sx * 5.5, 12.0, 0))
        c.box(nm, (sx * 5.5 - 3.8, 2.4, -4.0), (7.6, 10.0, 8.0), skin, k=2)
        c.box(nm, (sx * 5.5 - 4.2, 0.0, -9.0), (8.4, 2.6, 13.0), skin, k=2)
        for f in (-1, 0, 1):
            c.box(nm, (sx * 5.5 + f * 2.8 - 0.9, 0.0, -11.0), (1.8, 1.8, 2.8), bone("#c8b878", 0.08), k=1)
    # the stench
    n = (3, 6, 9)[phase - 1]
    for i in range(n):
        a = TAU * (i + 0.3) / n
        nm = "x_stench%d" % i
        r = 15.0 + (i % 3) * 2
        c.bone(nm, "root", pivot=(math.cos(a) * r, 4, math.sin(a) * r))
        c.box(nm, (math.cos(a) * r - 3.0, 4 + (i % 3) * 3, math.sin(a) * r - 3.0), (6.0, 12.0, 6.0), halo("#9ac830", 0.65), k=1)
    if phase >= 2:
        c.bone("x_drip", "jaw", pivot=(0, 20, -9))
        for i in range(3):
            c.box("x_drip", (-5 + i * 5 - 0.5, 14.0, -10.0), (1.0, 6.0, 1.0), wisp("#b8e030", 1.0), k=1)
    R = RIG
    for i in range(9):
        R.sway("x_stench%d" % i, 1, 2.0, 1, i * 0.11, kind="pos")
        R.sway("x_stench%d" % i, 3, 0.22, 2, i * 0.13, kind="scale")
    R.sway("jaw", 0, -3, 2, 0)
    R.sway("x_drip", 1, 1.5, 3, 0, kind="pos")
    return c


def _extra(clip, t, L):
    d = {}
    k = t / L
    if clip == "attack":
        d["jaw"] = {"rot": (-30 * keys(k, [(0, 0), (0.25, 0.1), (0.42, 1.0), (0.75, 0.2), (1, 0)]), 0, 0)}
    if clip in ("cast", "cast_big"):
        d["jaw"] = {"rot": (-24 * (0.7 if clip == "cast" else keys(k, [(0, 0), (0.35, 1.0), (0.8, 1.2), (0.9, 0), (1, 0)])), 0, 0)}
    return d


RIG = AnimRig(humanoid_motion(_extra, stride=18, arm_swing=14, bob=0.8, slouch=2, cast_arms=(-110, -110)))
