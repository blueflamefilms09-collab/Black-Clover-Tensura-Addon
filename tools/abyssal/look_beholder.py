"""Why: the BEHOLDER, the sphere of many eyes: a round mauve body of leathery scales that hovers, one huge central eye with heavy lids, a wide maw of teeth under it, and ten eyestalks crowning it,
each ending in an eye of its own colour (the rays of the D&D monster, one per stalk). Phase 2 the stalk eyes swell and burn, phase 3 four more writhing stalks break out, the hide cracks with
light and the central eye throws the cone of the anti-magic gaze. A full GeoLite body (not a player skin): the owner's rule keeps real geo bodies for the non-humanoids.
"""
import math
import lib
from lib import Creature, AnimRig, over, on, shade, flat, scales, emissive, halo, wisp, eyeball, TAU, keys, smooth, bone, metal, glowfill
import beasts as B

LOOK = dict(id="beholder", name="Beholder", kind="geo", height=3.3, glow="#ffb020", boost=0.9, shadow=1.1, walk_cycle=2.2, run_cycle=3.6, lift=0.0, dust=False,
            captions=["", "The Beholder's eyestalks flare: every ray is awake.", "Four more stalks tear free and the central eye opens its cone of nothing."])

EYE_COLORS = ["#ff3030", "#ff8a20", "#ffe030", "#60ff40", "#30ffd0", "#3090ff", "#8a40ff", "#ff40d0", "#f0f0ff", "#b030ff"]


def build(phase):
    c = Creature("beholder", phase, tex_w=512)
    c.bone("root", pivot=(0, 0, 0))
    c.bone("body", "root", pivot=(0, 20, 0))
    hide = scales("#7a4a68", 3)
    hide2 = scales("#94607a", 3)
    B.blob(c, "body", (0, 20, 0), (28, 28, 28), hide, bands=12, k=1)
    # the central eye, heavy lids over and under it
    ir = "#ffb020"
    sz = 11 + (2 if phase >= 2 else 0)
    B.eye(c, "body", (-sz / 2, 22.5 - sz / 2 + 2, -15.6), (sz, sz, 2.4), ir, glow=True)
    c.box("body", (-sz / 2 - 1.5, 22.5 + sz / 2 + 1.4, -15.9), (sz + 3, 2.6, 3.0), hide2, k=1)
    c.box("body", (-sz / 2 - 1.5, 22.5 - sz / 2 + 0.2, -15.9), (sz + 3, 2.0, 3.0), hide2, k=1)
    c.box("body", (-sz / 2 - 3.0, 22.5 - 2.0, -15.0), (3.0, 4.0, 2.4), hide2, k=1)
    c.box("body", (sz / 2, 22.5 - 2.0, -15.0), (3.0, 4.0, 2.4), hide2, k=1)
    if phase >= 2:
        c.box("body", (-sz / 2 - 3, 22.5 - sz / 2 - 1.0, -17.4), (sz + 6, sz + 6, 2.0), halo(ir, 0.7), k=1)
    # the maw
    c.bone("jaw", "body", pivot=(0, 11.0, -11.5))
    dark = flat("#2a0a14", 0.05)
    c.box("body", (-7.5, 9.0, -14.6), (15.0, 6.0, 2.2), dark, k=2)
    tooth = bone("#e8e0c8", 0.06)
    for i in range(7):
        x = -6.6 + i * 2.2
        c.box("body", (x - 0.55, 13.4 - (3.2 if i % 2 == 0 else 2.2), -15.4), (1.1, 3.2 if i % 2 == 0 else 2.2, 1.1), tooth, k=1)
    c.box("jaw", (-8.0, 5.5, -15.2), (16.0, 4.0, 3.8), hide2, k=2)
    c.box("jaw", (-6.5, 9.0, -15.0), (13.0, 1.2, 2.6), flat("#c0405a", 0.08), k=1)
    for i in range(6):
        x = -5.6 + i * 2.25
        c.box("jaw", (x - 0.55, 9.4, -15.4), (1.1, 2.6 if i % 2 else 3.4, 1.1), tooth, k=1)
    # the ten eyestalks (and, in phase three, four writhing tendrils)
    names = []
    n_st = 10
    for i in range(n_st):
        a = TAU * i / n_st
        dx, dz = math.cos(a), math.sin(a)
        start = (dx * 6.5, 32.5, dz * 6.5)
        col = EYE_COLORS[i]
        bs = 3.0 + (1.3 if phase >= 2 else 0.0)
        ball = over(eyeball(col, "#101010", "#e8e0d0", "#a03030"), on(("front", "back", "left", "right", "top", "bottom"), lambda cv: None))
        nm = "x_stalk%d" % i
        names += B.stalk(c, "body", nm, start, (dx, dz), [5.0, 5.0, 4.5], [3.0, 2.5, 2.1], hide2, tilt=24 + (i % 2) * 18, curl=10, ball=emissive(col, col, soft=0.2) if False else ball, ball_size=bs)
        if phase >= 2:
            last = nm + "_2"
            c.box(last, (start[0] - bs, start[1] + 14.2 - bs, start[2] - bs), (2 * bs, 2 * bs, 2 * bs), halo(col, 0.6), k=1)
    c.note = None
    if phase >= 3:
        for i in range(4):
            a = TAU * (i + 0.5) / 4
            dx, dz = math.cos(a), math.sin(a)
            names += B.tentacle(c, "body", "x_tend%d" % i, (dx * 9.5, 29.0, dz * 9.5), (dx, dz), [4.5, 4.5, 4.0, 3.5], [2.6, 2.2, 1.8, 1.4], hide2, tilt=55, curl=16, axis="+y", k=1)
        for i in range(8):
            a = TAU * i / 8
            c.box("body", (math.cos(a) * 14.2 - 0.6, 12 + (i % 4) * 4.5, math.sin(a) * 14.2 - 0.6), (1.2, 3.0, 1.2), emissive("#ffb020", "#ffb020"), k=1)
        c.bone("x_cone", "body", pivot=(0, 22.5, -16))
        c.box("x_cone", (-9.0, 13.5, -40.0), (18.0, 18.0, 24.0), wisp("#ffb020", 0.55), k=1)
    # the aura of motes
    R = RIG
    for i in range(14):
        nm = "x_stalk%d" % i
        for s in range(3):
            R.sway(nm + "_%d" % s, 0, 5, 1, i * 0.1 - s * 0.14)
            R.sway(nm + "_%d" % s, 2, 5, 1, i * 0.07 - s * 0.14 + 0.25)
    for i in range(4):
        B.sway_names(R, ["x_tend%d_%d" % (i, s) for s in range(4)], 0, 8, 1, 0.15, 2, 8, phase=i * 0.2)
    R.sway("x_cone", 3, 0.04, 2, 0, kind="scale")
    return c


def _extra(clip, t, L):
    d = {}
    k = t / L
    if clip == "attack":
        d["jaw"] = {"rot": (-34 * keys(k, [(0, 0), (0.25, 0.1), (0.4, 1.0), (0.75, 0.2), (1, 0)]), 0, 0)}
    if clip in ("cast", "cast_big"):
        up = 1.0 if clip == "cast" else keys(k, [(0, 0), (0.35, 1.0), (0.8, 1.2), (0.9, 0), (1, 0)])
        d["jaw"] = {"rot": (-16 * up, 0, 0)}
        for i in range(14):
            d["x_stalk%d_0" % i] = {"rot": (-18 * up, 0, 0)}
    if clip == "idle":
        d["jaw"] = {"rot": (-3 - 3 * math.sin(TAU * t / L), 0, 0)}
    return d


RIG = AnimRig(B.float_motion("body", hover=6.0, bob=1.4, tilt=3.0, extra=_extra, attack_dist=-5.0, rise=3.0))
