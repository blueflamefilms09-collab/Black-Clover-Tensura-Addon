"""Why: ASMODEUS, Lord of the Nine Hells, the archdevil: crimson skin, a bald head under a crown of seven curved horns, gold eyes, a black and gold robe with a mantle over the shoulders, great
leathery wings, a spaded tail, and the Ruby Rod in his right hand. Phase 2 the wings spread to their full span and hellfire climbs his shoulders, phase 3 the horns grow tall, the wings
catch fire along the veins and a ring of hellfire stands behind his head. Player skin plus extras.
"""
import math
import lib
from lib import (Humanoid, AnimRig, over, on, shade, bone, cloth, metal, plate, emissive, glowfill, wisp, halo, humanoid_motion, beam, spike, ring, TAU, keys, smooth, flat, membrane, bat_wing, horn, tail, scales)
import humans
import look_lich as L

LOOK = dict(id="asmodeus", name="Asmodeus", kind="humanoid", slim=False, height=2.1, glow="#ffb030", boost=1.05, shadow=0.6, dust=False, hold_r=-0.42,
            captions=["", "Asmodeus spreads his wings and the hall fills with the heat of the Nine Hells.", "The wings burn along every vein, the horns grow tall, and a ring of hellfire opens behind the Archdevil."])

P = dict(id="asmodeus", skin="#b02a24", veins="#5a1010", eye="#ffc830", eye_glow="#ffc830", eye_solid=True, hair=None, mouth="grin", mouth_color="#2a0808", brow="#2a0a0a",
         coat="#1c1014", trim="#d8a830", shirt="#7a141c", belt="#2a1a14", buckle="#d8a830", boots="#1a1014", legs="#1c1014", cuff="#d8a830", buttons="#d8a830", emblem_glow="#ffb030",
         mark="#d8a830", mark_glow="#ffb030", eye_big=0, scale=None)


def horns(c, P, phase):
    mt = bone("#2a1c1a", 0.12)
    tip = bone("#6a4a30", 0.1)
    c.bone("x_horns", "head", pivot=(0, 32, 0))
    mult = (1.0, 1.15, 1.5)[phase - 1]
    spec = [(0.0, -1.0, 1.0), (-0.9, -0.8, 0.9), (0.9, -0.8, 0.9), (-1.0, 0.1, 0.85), (1.0, 0.1, 0.85), (-0.7, 0.8, 0.75), (0.7, 0.8, 0.75)]
    for dx, dz, sc in spec:
        n = math.hypot(dx, dz)
        base = (dx / n * 3.6, 32.4, dz / n * 3.6)
        horn(c, "x_horns", base, dx, dz, [3.4 * sc * mult, 3.0 * sc * mult, 2.6 * sc * mult, 2.0 * sc * mult], [2.0, 1.6, 1.2, 0.8], [30, 38, 28, 8], mt, k=1)
    return ["x_horns"]


def mantle(c, P, phase):
    blk = cloth("#1c1014", 3, trim="#d8a830", trim_rows=1)
    c.bone("x_mantle", "body", pivot=(0, 23.5, 0))
    c.box("x_mantle", (-8.2, 21.2, -3.6), (16.4, 3.4, 7.2), blk, k=2)
    c.box("x_mantle", (-6.0, 24.4, -2.9), (12.0, 1.0, 5.8), metal("#d8a830", "#fff0a0"), k=1)
    for s in (-1, 1):
        for i in range(3):
            spike(c, "x_mantle", (s * (4.5 + i * 1.8), 24.6, -1.5 + i * 1.4), 4.5 - i * 0.6, 1.6, metal("#8a6a20", "#e8c860"), rot=(0, 0, -s * 14), n=3)
    return ["x_mantle"]


def rod(c, P, phase):
    c.bone("x_rod", "arm_r", pivot=(-6.0, 10.6, -1.2))
    c.box("x_rod", (-6.7, 0.5, -1.9), (1.4, 28.5, 1.4), metal("#d8a830", "#fff0a0"), k=2)
    for yy in (8.0, 17.0):
        c.box("x_rod", (-7.2, yy, -2.4), (2.4, 1.2, 2.4), metal("#8a6a20", "#e8c860"), k=1)
    top = 29.0
    for s in (-1, 1):
        beam(c, "x_rod", (-6.0 + s * 0.6, top - 0.2, -1.2), 3.4, 0.9, 0.9, (0, 0, -s * 24), metal("#d8a830", "#fff0a0"), k=1)
    c.box("x_rod", (-7.6, top + 0.4, -2.8), (3.2, 3.6, 3.2), emissive("#ff3040", "#ff5060", soft=0.15), k=1)
    c.box("x_rod", (-9.6, top - 1.4, -4.8), (7.2, 7.2, 7.2), halo("#ff2030", 0.9), k=1)
    return ["x_rod"]


def build(phase):
    sk = humans.make_skin(P, phase)
    c = Humanoid("asmodeus", phase, sk)
    c.note_extra("horn_crown", "head", horns(c, P, phase))
    c.note_extra("mantle", "body", mantle(c, P, phase))
    span = (36, 44, 52)[phase - 1]
    bp = bone("#2a1a1c", 0.1)
    mp = membrane("#5a1418", "#d8a830")
    wings = bat_wing(c, "body", "x_wing_r", (-3.2, 21.5, 3.2), -1, span, bp, mp, lift=(10, 20, 26)[phase - 1], droop=span * 0.95, sweep=(46, 34, 28)[phase - 1])
    wings += bat_wing(c, "body", "x_wing_l", (3.2, 21.5, 3.2), 1, span, bp, mp, lift=(10, 20, 26)[phase - 1], droop=span * 0.95, sweep=(46, 34, 28)[phase - 1])
    c.note_extra("wings", "body", wings)
    c.note_extra("ruby_rod", "arm_r", rod(c, P, phase))
    c.note_extra("skirt", "body", L.skirt(c, dict(P, robe="#1c1014", trim="#d8a830"), phase, length=11.5, wide=1.05))
    c.note_extra("tail", "body", c.chain("x_tail", "body", (0, 13.0, 2.2), "+z", [4, 4, 4, 3.5, 3], [1.8, 1.6, 1.4, 1.2, 1.0], scales("#7a1a1c", 3), curl=(-14, 0, 0)))
    c.box("x_tail_4", (-1.5, 12.0, 18.0 + 4.0), (3.0, 2.4, 3.0), emissive("#ffb030", "#ffb030"), k=1) if False else None
    if phase >= 2:
        n = 4 if phase == 2 else 6
        c.note_extra("hellfire", "body", L.wisps(c, P, phase, colors=("#ff7a20", "#ffc040"), count=n, height=(8, 14), radius=(8.5, 11.5)))
    if phase >= 3:
        c.bone("x_ring", "head", pivot=(0, 33, 3.5))
        for i in range(16):
            a = TAU * i / 16
            x, y = math.cos(a) * 9.5, 34 + math.sin(a) * 9.5
            c.box("x_ring", (x - 1.2, y - 1.2, 3.0), (2.4, 2.4, 1.2), emissive("#ff8a20", "#ffb040", soft=0.2), k=1, rot=(0, 0, math.degrees(a)), pivot=(x, y, 3.5))
        c.note_extra("fire_ring", "head", ["x_ring"])
    R = RIG
    for s, sg in (("r", -1), ("l", 1)):
        R.sway("x_wing_" + s, 2, 5 * sg, 1, 0)
        R.sway("x_wing_%s2" % s, 2, 9 * sg, 1, 0.14)
    R.sway_chain(["x_tail_%d" % i for i in range(5)], 0, 6, 1, lag=0.12, axis2=1, amp2=8)
    R.sway("x_rod", 2, 1.5, 1)
    R.sway("x_ring", 2, 1.0, 1, 0, kind="spin") if False else None
    for i in range(6):
        R.sway("x_wisp%d" % i, 1, 3.0 + (i % 3), 1, i * 0.083, kind="pos")
        R.sway("x_wisp%d" % i, 3, 0.25, 2, i * 0.11, kind="scale")
    return c


def _extra(clip, t, L_):
    d = {}
    if clip in ("cast", "cast_big"):
        up = 1.0 if clip == "cast" else keys(t / L_, [(0, 0), (0.35, 1.0), (0.8, 1.2), (0.9, 0), (1, 0)])
        for s, sg in (("r", -1), ("l", 1)):
            d["x_wing_" + s] = {"rot": (0, 0, 26 * sg * up)}
            d["x_wing_%s2" % s] = {"rot": (0, 0, 14 * sg * up)}
    if clip == "attack":
        for s, sg in (("r", -1), ("l", 1)):
            d["x_wing_" + s] = {"rot": (0, 0, 14 * sg * math.sin(math.pi * t / L_))}
    return d


RIG = AnimRig(humanoid_motion(_extra, stride=24, arm_swing=16, bob=0.5))
