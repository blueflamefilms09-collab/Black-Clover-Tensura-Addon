"""Why: the BALOR, the fiend of the Abyss that every balrog descends from: a giant, broad, black-red body cracked with lava, a horned head in a mane of flame with orange eyes and tusks, enormous bat wings, hooved
legs, a jagged lightning sword in the right hand and a whip of fire in the left. Phase 2 the cracks open wider and the wings burn along their veins, phase 3 the whole body is ablaze and the horns
grow into a crown. A full GeoLite body.
"""
import math
import lib
from lib import Creature, AnimRig, over, on, shade, flat, scales, emissive, halo, wisp, bone, flesh, membrane, bat_wing, TAU, keys, smooth, humanoid_motion, spike, beam, horn, plate
import beasts as B

LOOK = dict(id="balor", name="Balor", kind="geo", height=3.6, glow="#ff7a20", boost=0.9, shadow=1.3, walk_cycle=2.2, run_cycle=3.8, dust=False,
            captions=["", "The cracks in the Balor's skin open and its wings catch fire along the veins.", "The Balor is ablaze from horn to hoof."])


def lava(base, shadow_c, n, glow="#ff7a20"):
    """Black-red hide with a net of glowing cracks (n cracks per face)."""
    def f(cv):
        flesh(base, shadow_c, 0.02)(cv)
        for i in range(n):
            x, y = cv.rng.randint(0, max(1, cv.w)), cv.rng.randint(0, max(1, cv.h))
            for j in range(cv.rng.randint(4, 10)):
                cv.px(x, y, "#ffb040", glow)
                x += cv.rng.randint(-1, 2)
                y += cv.rng.randint(0, 2)
    return f


def build(phase):
    c = Creature("balor", phase, tex_w=512)
    c.bone("root", pivot=(0, 0, 0))
    c.bone("body", "root", pivot=(0, 26, 0))
    n = (3, 6, 10)[phase - 1]
    hide = lava("#2c1214", "#120608", n)
    hide2 = lava("#3a1a1a", "#1a0a0a", n)
    c.box("body", (-10.0, 24.0, -5.5), (20.0, 18.0, 11.0), hide, k=2)
    c.box("body", (-8.0, 22.0, -5.0), (16.0, 3.0, 10.0), hide2, k=2)
    for i in range(4):
        c.box("body", (-6.0, 26.0 + i * 3.6, -6.4), (12.0, 2.6, 1.2), hide2, k=1)
    # head and horns, tusks, mane of flame
    c.bone("head", "body", pivot=(0, 42, 0))
    c.box("head", (-5.0, 42.0, -5.5), (10.0, 10.0, 10.0), hide, k=2)
    c.box("head", (-4.0, 43.0, -9.0), (8.0, 4.0, 4.0), hide2, k=2)
    c.box("head", (-3.6, 42.4, -9.4), (7.2, 1.4, 1.4), flat("#2a0a08", 0.05), k=1)
    for sx in (-1, 1):
        c.box("head", (sx * 2.6 - 0.9, 47.6, -5.9), (1.8, 1.6, 0.8), emissive("#ffa020", "#ffa020", soft=0.1), k=1)
        c.box("head", (sx * 2.6 - 2.2, 46.4, -6.8), (4.4, 4.4, 2.0), halo("#ff8a20", 0.6), k=1)
        spike(c, "head", (sx * 2.2, 43.3, -9.2), 3.0, 1.2, bone("#e8d8b0", 0.06), rot=(0, 0, sx * 6), n=2)
        g = (1.0, 1.15, 1.4)[phase - 1]
        horn(c, "head", (sx * 4.2, 50.5, -1.0), sx * 0.9, 0.5, [5.0 * g, 5.0 * g, 4.5 * g, 4.0 * g], [2.8, 2.2, 1.7, 1.2], [18, 36, 54, 70], bone("#2a1a1a", 0.12), k=1)
    for i in range(6):
        c.box("head", (-4.0 + i * 1.5, 52.4, -4.0 - (i % 2) * 2.0), (1.5, 2.4 + phase, 1.5), wisp("#ff8a20", 1.0), k=1)
    # arms
    for nm, sx in (("arm_l", 1), ("arm_r", -1)):
        c.bone(nm, "body", pivot=(sx * 12.5, 40.0, 0))
        c.box(nm, (sx * 12.5 - 4.0, 21.0, -4.0), (8.0, 20.0, 8.0), hide, k=2)
        c.box(nm, (sx * 12.5 - 5.0, 12.0, -5.0), (10.0, 9.5, 10.0), hide2, k=2)
        c.box(nm, (sx * 12.5 - 5.5, 39.0, -5.2), (11.0, 3.4, 10.4), plate("#2a1a1a", "#ff7a20"), k=2)
        for f in (-1, 0, 1):
            beam(c, nm, (sx * 12.5 + f * 2.8, 12.5, -4.4), 4.0, 1.2, 1.2, (-10, 0, 0), bone("#e8d8b0", 0.06), k=1, along="-y")
    # legs
    for nm, sx in (("leg_l", 1), ("leg_r", -1)):
        c.bone(nm, "root", pivot=(sx * 5.8, 25.0, 0))
        c.box(nm, (sx * 5.8 - 4.2, 12.0, -4.6), (8.4, 13.5, 9.2), hide, k=2)
        c.bone("shin_" + nm[4:], nm, pivot=(sx * 5.8, 12.0, 0), rotation=(-14, 0, 0))
        c.box("shin_" + nm[4:], (sx * 5.8 - 3.2, 3.0, -3.4), (6.4, 9.5, 6.8), hide2, k=2)
        c.box("shin_" + nm[4:], (sx * 5.8 - 3.8, 0.0, -6.0), (7.6, 3.4, 9.0), bone("#1a1010", 0.1), k=2)
    # wings
    span = (58, 62, 68)[phase - 1]
    mem = membrane("#2a0c0e", "#ff7a20" if phase >= 2 else "#5a1a14", ragged=True)
    bat_wing(c, "body", "wing_r", (-5.0, 38.0, 4.0), -1, span, bone("#2a1414", 0.1), mem, lift=24, droop=span * 0.85, sweep=30)
    bat_wing(c, "body", "wing_l", (5.0, 38.0, 4.0), 1, span, bone("#2a1414", 0.1), mem, lift=24, droop=span * 0.85, sweep=30)
    # the lightning sword in the right hand
    c.bone("x_sword", "arm_r", pivot=(-12.5, 16.0, -3.0))
    c.box("x_sword", (-13.2, 14.0, -3.8), (1.6, 6.0, 1.6), bone("#2a1a1a", 0.1), k=1)
    c.box("x_sword", (-16.0, 19.4, -4.2), (7.0, 1.6, 2.4), metal("#6a5a5a", "#c8c0c0"), k=1) if False else c.box("x_sword", (-16.0, 19.4, -4.2), (7.0, 1.6, 2.4), flat("#6a5a5a", 0.05), k=1)
    p = (-12.5, 21.0, -3.2)
    for i, (ln, w_, rz) in enumerate(((9, 2.6, 8), (8, 2.4, -14), (8, 2.0, 12), (7, 1.5, -10), (5, 1.0, 6))):
        p = beam(c, "x_sword", p, ln, w_, 0.9, (-52, 0, rz), emissive("#d8f0ff", "#8ad0ff", soft=0.2), k=1)
    c.box("x_sword", (-17.0, 18.0, -22.0), (9.0, 12.0, 20.0), halo("#8ad0ff", 0.4), k=1)
    # the whip of fire in the left hand
    whip = c.chain("x_whip", "arm_l", (12.5, 14.0, -4.0), "-y", [4, 4, 4, 4, 3.6, 3.6, 3.2, 3.2, 3.0, 3.0], [1.8, 1.7, 1.6, 1.5, 1.4, 1.3, 1.2, 1.1, 1.0, 0.9], emissive("#ff8a20", "#ff8a20", soft=0.3), curl=(-8, 0, 3), k=1)
    # the aura
    for i in range((4, 8, 12)[phase - 1]):
        a = TAU * i / ((4, 8, 12)[phase - 1])
        nm = "x_fire%d" % i
        r = 12.0 + (i % 3) * 2.5
        c.bone(nm, "root", pivot=(math.cos(a) * r, 4, math.sin(a) * r))
        c.box(nm, (math.cos(a) * r - 1.4, 4 + (i % 3) * 5, math.sin(a) * r - 1.4), (2.8, 12, 2.8), wisp("#ff7a20" if i % 2 else "#ffc040", 1.0), k=1)
    R = RIG
    for s, sg in (("r", -1), ("l", 1)):
        R.sway("wing_" + s, 2, 5 * sg, 1, 0)
        R.sway("wing_%s2" % s, 2, 9 * sg, 1, 0.14)
    B.sway_names(R, whip, 0, 10, 2, 0.1, 2, 8)
    R.sway("x_sword", 2, 1.5, 1)
    for i in range(12):
        R.sway("x_fire%d" % i, 1, 3.0 + (i % 3), 1, i * 0.083, kind="pos")
        R.sway("x_fire%d" % i, 3, 0.25, 2, i * 0.11, kind="scale")
    return c


from lib import metal  # noqa: E402


def _extra(clip, t, L):
    d = {}
    k = t / L
    if clip in ("cast", "cast_big", "attack"):
        up = 0.6 if clip == "cast" else (math.sin(math.pi * k) if clip == "attack" else keys(k, [(0, 0), (0.35, 1.0), (0.8, 1.2), (0.9, 0), (1, 0)]))
        for s, sg in (("r", -1), ("l", 1)):
            d["wing_" + s] = {"rot": (0, 0, 24 * sg * up)}
    if clip == "attack":
        lg = keys(k, [(0, 0.0), (0.24, -0.4), (0.42, 1.0), (0.7, 0.1), (1, 0.0)])
        for i in range(10):
            d["x_whip_%d" % i] = {"rot": (-14 * lg, 0, 0)}
    return d


RIG = AnimRig(humanoid_motion(_extra, stride=22, arm_swing=16, bob=0.8, slouch=0, cast_arms=(-130, -130)))
