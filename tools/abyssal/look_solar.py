"""Why: the SOLAR, the greatest of the angels: gold skin and blind white-gold eyes, long pale hair, white and gold plate, a great halo behind the head, white-gold feathered wings (one pair, then two,
then three), a sunburst of rays behind the back and a longbow of light in the left hand. It is the one holy look of the set: bright where the lich is dark. Player skin plus extras.
"""
import math
import lib
from lib import (Humanoid, AnimRig, over, on, shade, bone, cloth, metal, plate, emissive, glowfill, wisp, halo, humanoid_motion, beam, spike, curve, TAU, keys, smooth, flat, feathers, feather_wing)
import humans
import look_lich as L

LOOK = dict(id="solar", name="Solar", kind="humanoid", slim=False, height=2.1, glow="#ffe890", boost=1.05, shadow=0.55, dust=False,
            captions=["", "A second pair of wings unfolds behind the Solar and the sunburst turns.", "Six wings, a full halo and a bow drawn to the ear: the Solar judges the hall."])

P = dict(id="solar", skin="#e8c878", eye="#fff4c0", eye_glow="#fff4c0", eye_solid=True, hair="#f4ecc8", hair_style="long", mouth="line", brow="#c8a860", coat="#f0ead8", trim="#d8a830",
         shirt="#f0ead8", belt="#d8a830", buckle="#fff4c0", boots="#e8dcb8", legs="#e8dcb8", cuff="#d8a830", armor="#eadfc0", armor_trim="#d8a830", glow="#ffe890", emblem_glow="#ffe890",
         angry=False, eye_big=0)


def sunburst(c, P, phase):
    c.bone("x_sun", "body", pivot=(0, 24, 5))
    n = (12, 16, 20)[phase - 1]
    for i in range(n):
        a = TAU * i / n
        ln = 10 + (i % 2) * 6 + 3 * phase
        x, y = math.cos(a) * 5.5, 25.5 + math.sin(a) * 5.5
        c.box("x_sun", (x - 0.7, y, 4.6), (1.4, ln, 0.5), wisp("#ffe890", 1.0), k=1, rot=(0, 0, math.degrees(a) - 90), pivot=(x, y, 4.8))
    c.box("x_sun", (-9, 15.5, 4.4), (18, 18, 0.5), halo("#ffe890", 0.8), k=1)
    return ["x_sun"]


def halo_ring(c, P, phase):
    c.bone("x_halo", "head", pivot=(0, 36, 3.0))
    n = 20
    r = 7.0 + phase
    for i in range(n):
        a = TAU * i / n
        x, y = math.cos(a) * r, 37.0 + math.sin(a) * r * 0.9
        seg = 2 * math.pi * r / n
        c.box("x_halo", (x - seg / 2, y - 0.8, 2.4), (seg, 1.6, 1.4), emissive("#fff4c0", "#ffe890", soft=0.15), k=1, rot=(0, 0, math.degrees(a) + 90), pivot=(x, y, 3.0))
    return ["x_halo"]


def bow(c, P, phase):
    gold = metal("#d8a830", "#fff4c0")
    c.bone("x_bow", "arm_l", pivot=(6.0, 10.6, -1.5))
    c.box("x_bow", (5.3, 9.0, -2.4), (1.4, 3.2, 1.6), metal("#8a6a20", "#e8c860"), k=1)
    g = 1.0 + 0.15 * (phase - 1)
    ls, ws = [5.0 * g, 5.0 * g, 4.5 * g, 3.5 * g], [1.4, 1.2, 1.0, 0.8]
    up = curve(c, "x_bow", (6.0, 12.2, -1.6), ls, ws, [(10, 0, 0), (25, 0, 0), (40, 0, 0), (55, 0, 0)], gold, k=1)
    lo = curve(c, "x_bow", (6.0, 9.0, -1.6), ls, ws, [(-10, 0, 0), (-25, 0, 0), (-40, 0, 0), (-55, 0, 0)], gold, k=1, along="-y")
    c.box("x_bow", (5.85, lo[1], up[2] - 0.15), (0.3, up[1] - lo[1], 0.3), emissive("#fff4c0", "#ffe890"), k=1)
    c.box("x_bow", (4.0, lo[1], up[2] - 1.5), (4.0, up[1] - lo[1], 3.0), halo("#ffe890", 0.35), k=1)
    if phase >= 2:
        c.box("x_bow", (5.5, 10.4, -12.0), (1.0, 1.0, 12.0 + up[2]), wisp("#fff4c0", 1.0), k=1)         # the arrow of light on the string
    return ["x_bow"]


def build(phase):
    sk = humans.make_skin(P, phase)
    c = Humanoid("solar", phase, sk)
    f1, f2 = feathers("#f0ead8", "#d8a830", "#ffe890"), feathers("#e0d4b0", "#e8c860", "#fff4c0")
    span = (30, 34, 36)[phase - 1]
    wings = feather_wing(c, "body", "x_wing_r", (-3.5, 22.0, 3.0), -1, span, f1, f2, rows=5, height=span * 0.9)
    wings += feather_wing(c, "body", "x_wing_l", (3.5, 22.0, 3.0), 1, span, f1, f2, rows=5, height=span * 0.9)
    if phase >= 2:
        wings += feather_wing(c, "body", "x_wing2_r", (-3.2, 18.0, 3.4), -1, span * 0.78, f1, f2, rows=4, height=span * 0.7, sweep=52, raise_=8)
        wings += feather_wing(c, "body", "x_wing2_l", (3.2, 18.0, 3.4), 1, span * 0.78, f1, f2, rows=4, height=span * 0.7, sweep=52, raise_=8)
    if phase >= 3:
        wings += feather_wing(c, "body", "x_wing3_r", (-2.8, 25.0, 3.6), -1, span * 0.6, f1, f2, rows=4, height=span * 0.55, sweep=18, raise_=48)
        wings += feather_wing(c, "body", "x_wing3_l", (2.8, 25.0, 3.6), 1, span * 0.6, f1, f2, rows=4, height=span * 0.55, sweep=18, raise_=48)
    c.note_extra("wings", "body", wings)
    c.note_extra("halo", "head", halo_ring(c, P, phase))
    c.note_extra("sunburst", "body", sunburst(c, P, phase))
    c.note_extra("bow_of_light", "arm_l", bow(c, P, phase))
    pm = plate("#eadfc0", rim="#d8a830")
    for s, arm in ((-1, "arm_r"), (1, "arm_l")):
        c.box(arm, (s * 6.5 - 3.0, 21.7, -3.2), (6.0, 2.8, 6.4), pm, k=2)
        spike(c, arm, (s * 7.2, 24.2, 0.0), 4.5, 1.8, metal("#d8a830", "#fff4c0"), rot=(0, 0, -s * 20), n=3)
    c.note_extra("pauldrons", "arm_r", [])
    c.note_extra("radiance", "root", L.wisps(c, P, phase, colors=("#ffe890", "#fff8d8"), count=(3, 6, 9)[phase - 1], height=(10, 18), radius=(9.5, 13.0)))
    R = RIG
    for s, sg in (("r", -1), ("l", 1)):
        for nm, a1, a2 in (("x_wing_", 4, 8), ("x_wing2_", 5, 8), ("x_wing3_", 6, 9)):
            R.sway(nm + s, 2, a1 * sg, 1, 0)
            R.sway(nm + s + "2", 2, a2 * sg, 1, 0.13)
    R.sway("x_sun", 2, 1.0, 1, 0, kind="spin")
    R.sway("x_halo", 1, 1.0, 1, 0, kind="spin")
    R.sway("x_bow", 2, 1.0, 1)
    for i in range(9):
        R.sway("x_wisp%d" % i, 1, 3.0 + (i % 3), 1, i * 0.083, kind="pos")
        R.sway("x_wisp%d" % i, 3, 0.25, 2, i * 0.11, kind="scale")
    return c


def _extra(clip, t, L_):
    d = {}
    if clip in ("cast", "cast_big"):
        up = 1.0 if clip == "cast" else keys(t / L_, [(0, 0), (0.35, 1.0), (0.8, 1.2), (0.9, 0), (1, 0)])
        for s, sg in (("r", -1), ("l", 1)):
            for nm in ("x_wing_", "x_wing2_", "x_wing3_"):
                d[nm + s] = {"rot": (0, 0, 20 * sg * up)}
    return d


RIG = AnimRig(humanoid_motion(_extra, stride=22, arm_swing=16, bob=0.6, hover=1.5, attack_hand="r"))
