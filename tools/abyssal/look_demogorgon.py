"""Why: DEMOGORGON, Prince of Demons: two mandrill heads on two long necks (red-striped nose, blue ridged cheeks, orange eyes, a purple mane), a scaled purple-blue reptilian body on clawed legs, a serpent
tail, and in place of arms two long barbed tentacles (the prince's tentacle arms). Phase 2 the eyes swell and the tentacles light along their barbs, phase 3 both heads grow a crown of horns and a
whirl of dark purple light surrounds the prince. A full GeoLite body (it is not human-shaped).
"""
import math
import lib
from lib import Creature, AnimRig, over, on, shade, flat, scales, emissive, halo, wisp, bone, fur, flesh, TAU, keys, smooth, humanoid_motion, spike, beam, horn
import beasts as B

LOOK = dict(id="demogorgon", name="Demogorgon", kind="geo", height=3.6, glow="#ffa030", boost=0.9, shadow=1.3, walk_cycle=2.0, run_cycle=3.4, dust=False,
            captions=["", "Both of Demogorgon's heads open their eyes and the barbs on his tentacles light up.", "The Prince of Demons crowns both heads in horns and the air round him whirls dark purple."])

SCALE = "#4a3a8a"
BELLY = "#8a7ac0"
TENT = "#3a4aa0"


def mandrill(c, parent, pre, base, phase, side):
    """A mandrill head with its back of skull at `base`, looking -z: skull, a long muzzle with a red stripe, blue ridged cheeks, orange eyes, fangs, a jaw that opens."""
    x, y, z = base
    fu = fur("#5a3a9a", "#8a6ac8")
    c.box(parent, (x - 4.2, y - 4.0, z - 8.0), (8.4, 8.0, 9.0), fu, k=2)
    c.box(parent, (x - 2.8, y - 3.2, z - 16.0), (5.6, 5.4, 8.5), flat("#6a4aa8", 0.06), k=2)
    c.box(parent, (x - 0.9, y - 3.0 + 1.2, z - 16.4), (1.8, 4.2, 8.9), flat("#e03038", 0.05), k=1)                                  # the red nose stripe
    for sx in (-1, 1):
        for j in range(3):                                                                                                   # blue ridged cheeks
            c.box(parent, (x + sx * 3.1 - (0.8 if sx > 0 else 0), y - 2.8 + j * 0.0, z - 14.8 + j * 2.2), (0.8, 4.0, 1.5), flat("#3a62e0", 0.05), k=1)
        ey = 0.8 + (0.4 if phase >= 2 else 0.0)
        c.box(parent, (x + sx * 3.2 - 1.0, y + 0.6, z - 7.4), (2.0, 1.8, 1.4), emissive("#ffa030", "#ffa030", soft=0.1), k=1)
        c.box(parent, (x + sx * 3.2 - 2.2, y - 0.6, z - 8.6), (4.4, 4.4, 3.8), halo("#ffa030", 0.5 + 0.12 * phase), k=1)
        c.box(parent, (x + sx * 3.0 - 1.4, y + 2.0, z - 8.0), (2.8, 1.2, 3.4), flat("#2a1a50", 0.05), k=1)                           # the brow
        horn(c, parent, (x + sx * 3.0, y + 3.6, z - 2.0), sx * 0.5, 1.0, [3.0, 2.6, 2.2] if phase < 3 else [4.5, 4.0, 3.4], [1.6, 1.2, 0.8], [30, 45, 60], bone("#c8b890", 0.1), k=1)
    c.bone(pre + "_jaw", parent, pivot=(x, y - 2.6, z - 5.0))
    c.box(pre + "_jaw", (x - 2.4, y - 5.2, z - 14.4), (4.8, 2.4, 10.0), flat("#6a4aa8", 0.06), k=2)
    c.box(pre + "_jaw", (x - 1.8, y - 3.0, z - 13.4), (3.6, 0.8, 8.4), flat("#a02a40", 0.08), k=1)
    for i in range(3):
        for sx in (-1, 1):
            c.box(parent, (x + sx * 2.1 - 0.5, y - 3.2 - 1.8, z - 14.6 + i * 3.0), (1.0, 2.2 if i == 0 else 1.5, 1.0), bone("#ece4d0", 0.05), k=1)
            c.box(pre + "_jaw", (x + sx * 1.9 - 0.5, y - 3.0, z - 13.4 + i * 3.0), (1.0, 1.7, 1.0), bone("#ece4d0", 0.05), k=1)
    # the mane round the neck
    c.box(parent, (x - 6.0, y - 5.5, z - 2.0), (12.0, 11.0, 5.0), fu, k=2)
    return pre + "_jaw"


def build(phase):
    c = Creature("demogorgon", phase, tex_w=512)
    c.bone("root", pivot=(0, 0, 0))
    c.bone("body", "root", pivot=(0, 22, 0))
    sc, bl = scales(SCALE, 3), scales(BELLY, 3)
    c.box("body", (-8.5, 20, -5.5), (17, 18, 11), sc, k=2)
    c.box("body", (-6.5, 20.5, -6.4), (13, 15, 1.4), bl, k=2)
    c.box("body", (-7.0, 15, -5.0), (14, 6, 10), sc, k=2)
    for i in range(5):                                                       # the dorsal spines
        spike(c, "body", (0, 37.5 - i * 3.2, 5.4), 4.0 - i * 0.2, 1.8, bone("#c8b890", 0.1), rot=(40, 0, 0), n=2)
    c.box("body", (-3.5, 27.5, -6.9), (7.0, 7.0, 0.7), emissive("#ffa030", "#ffa030", soft=0.3), k=1)    # the sigil on the chest
    c.box("body", (-1.7, 29.5, -7.1), (3.4, 3.0, 0.4), flat("#2a1a50", 0.05), k=1)
    # legs
    for nm, sx in (("leg_l", 1), ("leg_r", -1)):
        c.bone(nm, "root", pivot=(sx * 4.8, 17.0, 0))
        c.box(nm, (sx * 4.8 - 3.6, 8.0, -4.0), (7.2, 10.0, 8.0), sc, k=2)
        c.bone("shin_" + nm[4:], nm, pivot=(sx * 4.8, 8.0, 0), rotation=(-14, 0, 0))
        c.box("shin_" + nm[4:], (sx * 4.8 - 2.8, 0.6, -3.0), (5.6, 8.2, 6.0), sc, k=2)
        c.box("shin_" + nm[4:], (sx * 4.8 - 3.4, 0.0, -8.0), (6.8, 2.6, 8.4), sc, k=2)
        for cl in (-1, 0, 1):
            c.box("shin_" + nm[4:], (sx * 4.8 + cl * 2.1 - 0.6, 0.0, -10.2), (1.2, 1.6, 2.6), bone("#c8b890", 0.1), k=1)
    # the two necks and heads
    c.bone("head", "body", pivot=(0, 36, -1))                                  # the centre of the necks (the humanoid motion nods it)
    jaws = []
    for i, sx in enumerate((-1, 1)):
        names = []
        px, py, pz = sx * 4.6, 37.0, -1.0
        par = "head"
        for j, (ln, w_) in enumerate(((7, 6.4), (7, 5.8), (6, 5.2))):
            bn = "neck_%d_%d" % (i, j)
            c.bone(bn, par, pivot=(px, py, pz), rotation=(0 if j else -8, 0, -sx * (30 if j == 0 else -7)))
            c.box(bn, (px - w_ / 2, py, pz - w_ / 2), (w_, ln, w_), scales("#5a4aa0", 3), k=2)
            py += ln
            names.append(bn)
            par = bn
        jaws.append(mandrill(c, par, "head_%d" % i, (px, py + 3.0, pz), phase, sx))
    # the arms: two long barbed tentacles
    ten = flesh(TENT, "#20287a", 0.01)
    tip = flesh("#4a5ab8", "#2a3a8a", 0.01)
    arms = []
    for nm, sx in (("arm_l", 1), ("arm_r", -1)):
        c.bone(nm, "body", pivot=(sx * 9.0, 35.0, 0))
        names = []
        par = nm
        px, py, pz = sx * 9.0, 35.0, 0.0
        lens, wids = [7, 7, 6.5, 6, 5.5, 5, 4.5], [6.2, 5.6, 5.0, 4.5, 4.0, 3.4, 2.8]
        for j, (ln, w_) in enumerate(zip(lens, wids)):
            bn = "%s_t%d" % (nm, j)
            c.bone(bn, par, pivot=(px, py, pz), rotation=(sx * 0 + (8 if j else 0), 0, (sx * 10 if j == 0 else sx * -6)))
            c.box(bn, (px - w_ / 2, py - ln, pz - w_ / 2), (w_, ln, w_), tip if j == len(lens) - 1 else ten, k=2)
            if j % 2 == 0:
                for side in (-1, 1):
                    beam(c, bn, (px + side * w_ / 2, py - ln / 2, pz), 2.6 + phase * 0.4, 1.0, 1.0, (0, 0, -side * 70), emissive("#ffa030", "#ffa030", soft=0.1) if phase >= 2 else bone("#c8b890", 0.1), k=1)
            names.append(bn)
            py -= ln
            par = bn
        arms.append(names)
    # the tail
    tail = c.chain("tail", "body", (0, 19.0, 5.0), "+z", [8, 8, 8, 7, 7, 6, 6], [8, 7.2, 6.4, 5.6, 4.8, 4.0, 3.2], sc, curl=(-3, 0, 0), k=2)
    c.note = None
    if phase >= 3:
        for i in range(10):
            a = TAU * i / 10
            nm = "x_swirl%d" % i
            c.bone(nm, "root", pivot=(math.cos(a) * 14, 4, math.sin(a) * 14))
            c.box(nm, (math.cos(a) * 14 - 1.2, 4 + (i % 4) * 5, math.sin(a) * 14 - 1.2), (2.4, 12, 2.4), wisp("#a060ff", 1.0), k=1)
    R = RIG
    for i in range(2):
        B.sway_names(R, ["neck_%d_%d" % (i, j) for j in range(3)], 1, 6, 1, 0.12, 0, 4, phase=i * 0.5)
        B.sway_names(R, arms[i], 2 if False else 0, 5, 1, 0.13, 2, 5, phase=i * 0.5)
        R.sway("head_%d_jaw" % i, 0, -4, 2, i * 0.5)
    B.sway_names(R, tail, 1, 7, 1, 0.16, 0, 3)
    for i in range(10):
        R.sway("x_swirl%d" % i, 1, 4.0, 1, i * 0.1, kind="pos")
        R.sway("x_swirl%d" % i, 3, 0.25, 2, i * 0.11, kind="scale")
    return c


def _extra(clip, t, L):
    d = {}
    k = t / L
    if clip == "attack":
        lg = keys(k, [(0, 0.0), (0.25, 0.1), (0.42, 1.0), (0.75, 0.2), (1, 0.0)])
        for i in range(2):
            d["head_%d_jaw" % i] = {"rot": (-38 * lg, 0, 0)}
            d["neck_%d_0" % i] = {"rot": (-18 * lg, 0, 0)}
        for nm in ("arm_l", "arm_r"):
            for j in range(7):
                d["%s_t%d" % (nm, j)] = {"rot": (-22 * lg, 0, 0)}
    if clip in ("cast", "cast_big"):
        up = 0.7 if clip == "cast" else keys(k, [(0, 0), (0.35, 1.0), (0.8, 1.2), (0.9, 0), (1, 0)])
        for i in range(2):
            d["head_%d_jaw" % i] = {"rot": (-30 * up, 0, 0)}
            d["neck_%d_0" % i] = {"rot": (14 * up, 0, 0)}
    return d


RIG = AnimRig(humanoid_motion(_extra, stride=20, arm_swing=6, bob=0.8, cast_arms=(-60, -60)))
