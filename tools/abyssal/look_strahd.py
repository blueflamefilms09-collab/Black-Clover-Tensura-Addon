"""Why: STRAHD VON ZAROVICH, the vampire lord of Barovia: a pale noble in a black and crimson coat with a gold-edged jabot, slicked black hair with a widow's peak, red eyes and fangs, a long
cape with a stiff stand collar and a blood-red lining, and a cloud of bats circling him (four, then seven, then ten). Phase 2 tears the cape open into bat wings, phase 3 is the swarm: the
eyes blaze, the wings are full and a red mist runs along the floor. Player skin plus extras, as the other humanoids.
"""
import math
import lib
from lib import (Humanoid, AnimRig, over, on, shade, bone, cloth, metal, emissive, glowfill, wisp, halo, humanoid_motion, beam, spike, TAU, keys, smooth, flat, membrane, bat_wing)
import humans
import look_lich as L

LOOK = dict(id="strahd", name="Strahd", kind="humanoid", slim=False, height=2.0, glow="#ff3030", boost=1.0, shadow=0.5, dust=False,
            captions=["", "Strahd tears his cape open: leathery wings, and the bats come down from the rafters.", "The Devil of Barovia is the swarm. His eyes burn and the floor runs with red mist."])

P = dict(id="strahd", skin="#d9ccd0", eye="#ff3030", eye_glow="#ff3030", hair="#0e0a10", hair_style="slick", mouth="fangs", brow="#201418", coat="#2a0f18", trim="#c8a040", shirt="#e6dcd4",
         belt="#140a10", buckle="#c8a040", boots="#120c10", legs="#16101a", cuff="#c8a040", buttons="#c8a040", emblem_glow="#ff3030", eye_solid=False, eye_big=0, mark="#0e0a10")


def stand_collar(c, P, phase):
    red = cloth("#7a1020", 2, trim="#c8a040")
    blk = cloth("#1a0c14", 2, trim="#c8a040")
    c.bone("x_collar", "body", pivot=(0, 24, 0))
    for s in (-1, 1):
        c.box("x_collar", (s * 4.3 - (0.8 if s < 0 else 0), 23.6, -2.6), (0.8, 6.5, 5.6), red, k=2, rot=(0, 0, s * 8), pivot=(s * 4.3, 24, 0))
    c.box("x_collar", (-4.4, 23.6, 2.4), (8.8, 7.0, 0.8), blk, k=2)
    return ["x_collar"]


def cape(c, P, phase):
    names = []
    for i, x in enumerate((-3.2, 0.0, 3.2)):
        n = "x_cape%d" % i
        lining = over(cloth("#14080e", 4, tatter=3 + (phase - 1), trim="#c8a040"), on("front", lambda cv: cv.rect(0, 0, cv.w, cv.h - 1, "#8a1424")))
        c.bone(n, "body", pivot=(x, 23.6, 2.5))
        c.box(n, (x - 1.7, 1.0 - (i % 2) * 2.0, 2.2), (3.4, 22.6 + (i % 2) * 2.0, 0.9), lining, k=2)
        names.append(n)
    return names


def bats(c, P, phase):
    n = (4, 7, 10)[phase - 1]
    c.bone("x_bats", "root", pivot=(0, 20, 0))
    names = ["x_bats"]
    body = bone("#241822", 0.12)
    memb = membrane("#321c2a", "#5a2a3c")
    for i in range(n):
        a = TAU * i / n
        r = 11.0 + (i % 3) * 2.2
        x, z = math.cos(a) * r, math.sin(a) * r
        y = 14.0 + (i % 4) * 4.2
        nm = "x_bat%d" % i
        c.bone(nm, "x_bats", pivot=(x, y, z))
        c.box(nm, (x - 0.8, y - 0.8, z - 0.9), (1.6, 1.5, 1.8), body, k=1)
        c.box(nm, (x - 0.6, y + 0.4, z - 1.0), (0.4, 0.8, 0.4), emissive("#ff3030"), k=1)
        c.box(nm, (x + 0.2, y + 0.4, z - 1.0), (0.4, 0.8, 0.4), emissive("#ff3030"), k=1)
        for s in (-1, 1):
            wn = "%s_w%d" % (nm, 0 if s < 0 else 1)
            c.bone(wn, nm, pivot=(x + s * 0.6, y + 0.3, z))
            c.box(wn, (x + s * 0.6 - (3.4 if s < 0 else 0), y + 0.1, z - 0.9), (3.4, 0.35, 1.9), memb, k=1)
            names.append(wn)
        names.append(nm)
    return names


def mist(c, P, phase):
    if phase < 3:
        return []
    names = []
    for i in range(10):
        a = TAU * i / 10
        r = 6 + (i % 3) * 3
        nm = "x_mist%d" % i
        c.bone(nm, "root", pivot=(math.cos(a) * r, 0, math.sin(a) * r))
        c.box(nm, (math.cos(a) * r - 2.5, 0.2, math.sin(a) * r - 2.5), (5, 2.4, 5), halo("#c01830", 0.9), k=1)
        names.append(nm)
    return names


def build(phase):
    pp = dict(P, eye_big=1 if phase >= 2 else 0)
    sk = humans.make_skin(pp, phase)
    c = Humanoid("strahd", phase, sk)
    c.note_extra("collar", "body", stand_collar(c, P, phase))
    c.note_extra("cape", "body", cape(c, P, phase))
    wings = []
    if phase >= 2:
        span = (26, 34)[phase - 2]
        bp, mp = bone("#2a1a24", 0.1), membrane("#3a1626", "#8a2a40", ragged=True)
        wings += bat_wing(c, "body", "x_wing_r", (-3.0, 21.5, 3.2), -1, span, bp, mp, lift=22, droop=span * 0.9)
        wings += bat_wing(c, "body", "x_wing_l", (3.0, 21.5, 3.2), 1, span, bp, mp, lift=22, droop=span * 0.9)
    c.note_extra("bat_wings", "body", wings)
    c.note_extra("bats", "root", bats(c, P, phase))
    c.note_extra("blood_mist", "root", mist(c, P, phase))
    c.note_extra("eye_fire", "head", L.eye_fire(c, P, phase, scale=0.6, colors=("#ff3030", "#ff9060")) if phase >= 2 else [])
    R = RIG
    for i in range(3):
        R.sway("x_cape%d" % i, 0, 7, 1, 0.12 * i)
    R.sway("x_bats", 1, 1.0, 1, 0, kind="spin")
    for i in range(10):
        R.sway("x_bat%d" % i, 1, 6, 2, i * 0.1)
        R.sway("x_bat%d" % i, 1, 1.4, 2, i * 0.13, kind="pos")
        for w, sgn in ((0, -1), (1, 1)):
            R.sway("x_bat%d_w%d" % (i, w), 2, 38 * sgn * -1, 6, i * 0.07)
        R.sway("x_mist%d" % i, 3, 0.2, 1, i * 0.1, kind="scale")
    for s in ("r", "l"):
        sg = -1 if s == "r" else 1
        R.sway("x_wing_" + s, 2, 6 * sg, 1, 0.0)
        R.sway("x_wing_%s2" % s, 2, 12 * sg, 1, 0.15)
    for i in range(2):
        R.sway("x_eyefire%d" % i, 3, 0.22, 3, i * 0.3, kind="scale")
    return c


def _extra(clip, t, L_):
    d = {}
    if clip in ("cast", "cast_big"):
        for s, sg in (("r", -1), ("l", 1)):
            d["x_wing_" + s] = {"rot": (0, 0, 22 * sg)}
    if clip == "attack":
        for s, sg in (("r", -1), ("l", 1)):
            d["x_wing_" + s] = {"rot": (0, 0, 14 * sg * math.sin(math.pi * t / L_))}
    if clip == "death":
        f = smooth(t / L_ / 0.8)
        d["x_bats"] = {"pos": (0, 8 * f, 0)}
    return d


RIG = AnimRig(humanoid_motion(_extra, stride=24, arm_swing=18, bob=0.5))
