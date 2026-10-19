"""Why: ORCUS, Demon Prince of Undeath: a bloated, grey-green rotting demon with a great belly hung with a chain of skulls, ram horns curling down either side of his head, tattered black wings,
skull pauldrons and the WAND OF ORCUS, a black rod topped by a skull whose eyes burn green. Phase 2 the belly splits and soul fire shows in it, phase 3 ribs break out of the gut, the horns
grow and a crowd of green souls rises round him. Player skin plus extras (the belly and pauldrons are what make the player body read as bloated).
"""
import math
import lib
from lib import (Humanoid, AnimRig, over, on, shade, bone, cloth, metal, plate, emissive, glowfill, wisp, halo, humanoid_motion, beam, spike, curve, TAU, keys, smooth, flat, membrane, bat_wing, flesh, scales)
import humans
import look_lich as L

LOOK = dict(id="orcus", name="Orcus", kind="humanoid", slim=False, height=2.1, glow="#78ff60", boost=1.1, shadow=0.8, dust=False,
            captions=["", "The gut of Orcus splits and soul fire shows in the wound.", "Ribs break out of the demon prince; a crowd of souls rises with him."])

P = dict(id="orcus", skin="#7c8268", veins="#3a4a2a", eye="#78ff60", eye_glow="#78ff60", eye_solid=True, hair=None, mouth="grin", mouth_color="#1a1a10", brow="#2a2a1a",
         coat="#242018", trim="#c8c0a0", shirt="#5a604a", belt="#2a2418", buckle="#c8c0a0", boots="#1a1810", legs="#2a2820", cuff="#c8c0a0", emblem_glow="#78ff60", eye_big=0)

FLESH = flesh("#868c70", "#4a5a38", 0.02)


def belly(c, P, phase):
    c.bone("x_belly", "body", pivot=(0, 16, -2))
    fl = over(flesh("#8a9074", "#4a5a38", 0.025), on("front", lambda cv: [cv.line(0, y, cv.w - 1, y + 1, "#5a6048") for y in range(2, cv.h, 6)]))
    layers = [(-3.0, 8.0, 12.0, 6.0), (-4.5, 11.0, 14.0, 4.0)]
    c.box("x_belly", (-7.0, 9.0, -8.0), (14.0, 11.5, 6.2), fl, k=2)
    c.box("x_belly", (-6.0, 7.5, -8.8), (12.0, 14.5, 3.0), fl, k=2)
    c.box("x_belly", (-8.2, 11.0, -6.0), (16.4, 7.0, 4.5), fl, k=2)
    c.box("x_belly", (-5.0, 19.0, -6.0), (10.0, 3.0, 4.0), fl, k=2)
    if phase >= 2:                                           # the split
        c.box("x_belly", (-1.2, 8.8, -9.1), (2.4, 12.0, 0.5), emissive("#78ff60", "#78ff60", soft=0.3), k=1)
        c.box("x_belly", (-3.0, 10.0, -9.0), (6.0, 9.0, 0.3), halo("#78ff60", 0.8), k=1)
    if phase >= 3:
        for i in range(4):
            x = -4.5 + i * 3.0
            beam(c, "x_belly", (x, 18.5 - (i % 2) * 2, -8.6), 7.0, 1.2, 1.2, (-36 + (i % 2) * 14, 0, (i - 1.5) * 6), bone("#d6d0b0", 0.06), k=1)
    return ["x_belly"]


def skull_chain(c, P, phase):
    bn = bone("#d8d4bc", 0.06)
    c.bone("x_chain", "body", pivot=(0, 22, -3))
    for i in range(5):
        x = -6.4 + i * 3.2
        y = 21.0 - 2.6 * math.sin(math.pi * i / 4)
        c.box("x_chain", (x - 1.3, y - 1.5, -9.6), (2.6, 2.6, 2.4), bn, k=1)
        c.box("x_chain", (x - 0.9, y - 0.4, -10.05), (0.7, 0.7, 0.4), emissive("#78ff60"), k=1)
        c.box("x_chain", (x + 0.2, y - 0.4, -10.05), (0.7, 0.7, 0.4), emissive("#78ff60"), k=1)
    return ["x_chain"]


def horns(c, P, phase):
    mt = bone("#c8c0a0", 0.12)
    c.bone("x_horns", "head", pivot=(0, 30, 0))
    g = (1.0, 1.1, 1.3)[phase - 1]
    for s in (-1, 1):
        curve(c, "x_horns", (s * 3.8, 30.5, 0.0), [4.2 * g, 4.0 * g, 3.6 * g, 3.0 * g, 2.4 * g], [2.4, 2.1, 1.8, 1.4, 1.0],
              [(0, 0, -s * 70), (0, 0, -s * 120), (0, 0, -s * 170), (0, 0, -s * 215), (0, 0, -s * 250)], mt, k=1)
    return ["x_horns"]


def pauldrons(c, P, phase):
    bn = bone("#d8d4bc", 0.06)
    for s, arm in ((-1, "arm_r"), (1, "arm_l")):
        c.box(arm, (s * 6.5 - 3.2, 21.5, -3.4), (6.4, 3.0, 6.8), plate("#3a3830", "#c8c0a0"), k=2)
        c.box(arm, (s * 7.4 - 2.0, 24.3, -2.2), (4.0, 3.6, 4.2), bn, k=1)
        c.box(arm, (s * 7.4 - 1.4, 25.2, -2.45), (0.9, 0.9, 0.4), emissive("#78ff60"), k=1)
        c.box(arm, (s * 7.4 + 0.5, 25.2, -2.45), (0.9, 0.9, 0.4), emissive("#78ff60"), k=1)
    return []


def wand(c, P, phase):
    c.bone("x_wand", "arm_r", pivot=(-6.0, 10.6, -1.2))
    c.box("x_wand", (-6.7, 1.0, -1.9), (1.4, 29.0, 1.4), bone("#1c1a18", 0.1), k=2)
    for yy in (6.0, 15.0, 24.0):
        c.box("x_wand", (-7.1, yy, -2.3), (2.2, 1.0, 2.2), metal("#8a8468", "#d8d0a8"), k=1)
    top = 30.0
    c.box("x_wand", (-8.0, top, -3.2), (4.0, 3.6, 4.0), bone("#d8d4bc", 0.05), k=2)
    c.box("x_wand", (-7.4, top + 1.2, -3.35), (1.1, 1.1, 0.4), emissive("#78ff60"), k=1)
    c.box("x_wand", (-5.8, top + 1.2, -3.35), (1.1, 1.1, 0.4), emissive("#78ff60"), k=1)
    c.box("x_wand", (-9.8, top - 1.6, -5.0), (7.6, 7.6, 7.6), halo("#78ff60", 0.9), k=1)
    return ["x_wand"]


def build(phase):
    sk = humans.make_skin(P, phase)
    c = Humanoid("orcus", phase, sk)
    c.note_extra("belly", "body", belly(c, P, phase))
    c.note_extra("skull_chain", "body", skull_chain(c, P, phase))
    c.note_extra("ram_horns", "head", horns(c, P, phase))
    c.note_extra("skull_pauldrons", "arm_r", pauldrons(c, P, phase))
    span = (34, 40, 44)[phase - 1]
    bp = bone("#2a2820", 0.1)
    mp = membrane("#3a3a34", "#78ff60", ragged=True)
    wings = bat_wing(c, "body", "x_wing_r", (-3.4, 21.0, 3.4), -1, span, bp, mp, lift=16, droop=span * 0.9, sweep=36)
    wings += bat_wing(c, "body", "x_wing_l", (3.4, 21.0, 3.4), 1, span, bp, mp, lift=16, droop=span * 0.9, sweep=36)
    c.note_extra("wings", "body", wings)
    c.note_extra("wand_of_orcus", "arm_r", wand(c, P, phase))
    c.note_extra("skirt", "body", L.skirt(c, dict(P, robe="#242018", trim="#c8c0a0"), phase, wide=1.2))
    c.note_extra("souls", "root", L.wisps(c, P, phase, colors=("#78ff60", "#c0ffa0"), count=(3, 8, 12)[phase - 1], height=(9, 15), radius=(10.5, 14.0)))
    R = RIG
    for s, sg in (("r", -1), ("l", 1)):
        R.sway("x_wing_" + s, 2, 4 * sg, 1, 0)
        R.sway("x_wing_%s2" % s, 2, 8 * sg, 1, 0.14)
    R.sway("x_belly", 0, 1.8, 1, 0.1)
    R.sway("x_belly", 3, 0.03, 1, 0.1, kind="scale")
    R.sway("x_chain", 0, 3, 2, 0.0)
    R.sway("x_wand", 2, 1.4, 1)
    for i in range(12):
        R.sway("x_wisp%d" % i, 1, 3.0 + (i % 3), 1, i * 0.083, kind="pos")
        R.sway("x_wisp%d" % i, 3, 0.25, 2, i * 0.11, kind="scale")
    for i in range(3):
        R.sway("x_skirt_f%d" % i, 0, 4, 1, 0.1 * i)
        R.sway("x_skirt_b%d" % i, 0, -4, 1, 0.1 * i + 0.3)
    for i in range(2):
        R.sway("x_skirt_s%d" % i, 2, 4 if i else -4, 1, 0.2 * i)
    return c


def _extra(clip, t, L_):
    d = {}
    if clip in ("cast", "cast_big"):
        up = 1.0 if clip == "cast" else keys(t / L_, [(0, 0), (0.35, 1.0), (0.8, 1.2), (0.9, 0), (1, 0)])
        for s, sg in (("r", -1), ("l", 1)):
            d["x_wing_" + s] = {"rot": (0, 0, 22 * sg * up)}
    return d


RIG = AnimRig(humanoid_motion(_extra, stride=20, arm_swing=14, bob=0.6, slouch=4))
