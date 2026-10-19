"""Why: the KRAKEN of the black tide: a colossal deep-sea squid hauling itself over the floor on eight thick arms, a tall slanted mantle with two fins, a head with two huge slit-pupil eyes and a beak,
and two long feeding tentacles ending in clubs. Dark blue-violet hide mottled darker, bioluminescent spots in cyan. Phase 2 the spots blaze and four more arms come up from under the body, phase 3
the mantle crackles with lightning (the breath of the profile). A full GeoLite body.
"""
import math
import lib
from lib import Creature, AnimRig, over, on, shade, flat, scales, emissive, halo, wisp, TAU, keys, smooth, bone, metal, glowfill, flesh
import beasts as B

LOOK = dict(id="kraken", name="Kraken", kind="geo", height=3.2, glow="#40ffd0", boost=0.95, shadow=1.4, walk_cycle=2.6, run_cycle=4.2, dust=False,
            captions=["", "The Kraken's lights blaze and four more arms climb out from under it.", "Lightning crawls over the Kraken's mantle: the black tide is about to break."])


def build(phase):
    c = Creature("kraken", phase, tex_w=512)
    c.bone("root", pivot=(0, 0, 0))
    c.bone("body", "root", pivot=(0, 22, 0))
    hide = over(flesh("#2c3c70", "#161e48", 0.012), on(("front", "back", "left", "right"), lambda cv: [cv.rect(x, y, 2, 2, "#4a3a8a") for x in range(2, cv.w - 2, 7) for y in range(1 + (x * 3) % 5, cv.h - 2, 8)]))
    belly = flesh("#6a5a98", "#3a2e68", 0.015)
    # head, eyes, beak
    B.blob(c, "body", (0, 27, -1), (20, 14, 20), hide, bands=6, k=2)
    c.bone("mantle", "body", pivot=(0, 30, 6), rotation=(18, 0, 0))
    B.blob(c, "mantle", (0, 44, 6), (18, 34, 18), hide, bands=12, k=2)
    for sx in (-1, 1):                                              # the fins
        c.box("mantle", (sx * 9 - (0 if sx > 0 else 6.5), 46, 4.0), (6.5, 16, 1.6), belly, k=2, rot=(0, 0, sx * -18), pivot=(sx * 9, 52, 6))
    for sx in (-1, 1):
        iris = "#ffd030"
        def f(cv, sx=sx):
            if cv.face == ("left" if sx > 0 else "right"):
                lib.eyeball(iris, "#080808", "#e8e0b0", None)(cv)
            else:
                flat("#a09060", 0.05)(cv)
        c.box("body", (sx * 10.4 - (0 if sx > 0 else 2.6), 25.5, -6.5), (2.6, 7.0, 7.0), f, k=2)
        c.box("body", (sx * 10.0 - (0 if sx > 0 else 3.4), 24.5, -7.5), (3.4, 9.0, 9.0), halo("#ffd030", 0.5), k=1)
    c.box("body", (-2.5, 18.5, -9.5), (5.0, 5.0, 5.0), bone("#2a1c1c", 0.1), k=2)
    c.box("body", (-1.2, 17.0, -10.5), (2.4, 3.0, 2.6), bone("#1a1010", 0.1), k=1)
    for i in range(6):                                              # spots
        a = (i - 2.5) * 0.45
        y = 40 + (i % 3) * 7
        c.box("mantle", (math.sin(a) * 9.1 - 0.9, y, -3.0 - math.cos(a) * 0.1), (1.8, 1.8, 0.6), emissive("#40ffd0", "#40ffd0"), k=1)
    for i in range(4 + 4 * (phase - 1)):
        a = TAU * i / (4 + 4 * (phase - 1))
        c.box("body", (math.cos(a) * 10.3 - 0.8, 28 + (i % 3) * 2.0, math.sin(a) * 10.3 - 0.8), (1.6, 1.6, 1.6), emissive("#40ffd0", "#40ffd0"), k=1)
    # eight arms (twelve from phase two)
    arm_skin = flesh("#2c3c70", "#161e48", 0.012)
    arm_tip = flesh("#4a5a98", "#2a3668", 0.012)
    n_arms = 8 if phase == 1 else 12
    names = []
    for i in range(n_arms):
        a = TAU * (i + (0.5 if i >= 8 else 0)) / (8 if i < 8 else 4) if i >= 8 else TAU * i / 8
        dx, dz = math.cos(a), math.sin(a)
        r = 7.0 if i < 8 else 4.0
        names.append(B.tentacle(c, "body", "x_arm%d" % i, (dx * r, 22.0, dz * r), (dx, dz), [8, 8, 7, 6.5, 6, 5, 4], [5.0, 4.6, 4.0, 3.5, 3.0, 2.4, 1.8], arm_skin, tilt=56 + (i % 2) * 10 - (14 if i >= 8 else 0),
                                curl=-9 if i % 2 else 12, tip=arm_tip, sucker=flesh("#c8b8e8", None), k=2))
    for i, a in enumerate((-0.28, 0.28)):
        dx, dz = math.sin(a), -math.cos(a)
        B.tentacle(c, "body", "x_feed%d" % i, (dx * 5, 24.0, dz * 8), (dx, dz), [9, 9, 8, 8, 7, 7], [3.0, 2.6, 2.3, 2.0, 1.8, 1.6], arm_skin, tilt=84, curl=7, tip=arm_tip, k=2)
        c.box("x_feed%d_5" % i, (dx * 5 - 3.0, 24.0 - 48.0 - 0.5, dz * 8 - 3.0), (6.0, 7.0, 6.0), over(flesh("#4a5a98", "#2a3668", 0.012), on(("front", "back", "left", "right"), lambda cv: [cv.rect(x, y, 2, 2, "#80fff0", "#40ffd0") for x in range(1, cv.w - 1, 4) for y in range(1, cv.h - 1, 4)])), k=1)
    # lightning
    if phase >= 3:
        c.bone("x_bolts", "mantle", pivot=(0, 44, 6))
        pts = [(9.8, 36, 0), (9.2, 42, 3), (10.0, 48, -2), (9.0, 54, 3), (-9.8, 38, 1), (-9.2, 45, -3), (-10.0, 51, 2), (0, 40, -4), (0, 54, 11)]
        for i, (x, y, z) in enumerate(pts):
            c.box("x_bolts", (x - 0.5, y, z + 6 - 0.5), (1.0, 5.5, 1.0), wisp("#a8f0ff", 1.0), k=1, rot=(0, 0, 25 if i % 2 else -25), pivot=(x, y, z + 6))
            c.box("x_bolts", (x - 0.5 + (2 if i % 2 else -2), y + 4.0, z + 6 - 0.5), (1.0, 5.0, 1.0), wisp("#ffffff", 1.0), k=1, rot=(0, 0, -30 if i % 2 else 30), pivot=(x, y + 4, z + 6))
    R = RIG
    for i in range(12):
        B.sway_names(R, ["x_arm%d_%d" % (i, s_) for s_ in range(7)], 0, 5, 1, 0.14, 2, 5, phase=i * 0.083)
    for i in range(2):
        B.sway_names(R, ["x_feed%d_%d" % (i, s_) for s_ in range(6)], 0, 7, 1, 0.12, 2, 6, phase=i * 0.5)
    R.sway("mantle", 0, 2.0, 1, 0.1)
    R.sway("x_bolts", 3, 0.06, 6, 0, kind="scale")
    return c


def _extra(clip, t, L):
    d = {}
    k = t / L
    if clip == "attack":
        lg = keys(k, [(0, 0.0), (0.22, -0.5), (0.42, 1.0), (0.7, 0.15), (1, 0.0)])
        for i in range(2):
            for s_ in range(6):
                d["x_feed%d_%d" % (i, s_)] = {"rot": (-24 * lg, 0, 0)}
        d["mantle"] = {"rot": (-10 * lg, 0, 0)}
    if clip in ("walk", "run"):
        sg = 1.0 if clip == "walk" else 1.5
        for i in range(8):
            d["x_arm%d_0" % i] = {"rot": (16 * sg * math.sin(TAU * (t / L - i / 8.0)), 0, 0)}
    if clip in ("cast", "cast_big"):
        up = 1.0 if clip == "cast" else keys(k, [(0, 0), (0.35, 1.0), (0.8, 1.2), (0.9, 0), (1, 0)])
        d["mantle"] = {"rot": (-14 * up, 0, 0), "scale": (1 + 0.06 * up, 1 + 0.06 * up, 1 + 0.06 * up)}
    return d


RIG = AnimRig(B.float_motion("body", hover=3.0, bob=1.2, tilt=2.5, extra=_extra, attack_dist=-6.0, rise=2.0))
