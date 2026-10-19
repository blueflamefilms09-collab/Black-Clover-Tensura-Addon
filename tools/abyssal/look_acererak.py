"""Why: ACERERAK, the demilich of the D&D tomb: where the generic lich is a hooded skeleton with a bone staff, he is a bare gem-studded skull under a tall gold crown, wrapped in indigo and gold
robes with a fan collar standing up behind his head, cyan soul fire instead of green, a floating open tome instead of a staff, and the skulls of the Devoured circling him (more of them each phase).
Skin and extras follow the same split as the lich (look_lich.py).
"""
import math
import lib
from lib import (Humanoid, AnimRig, over, on, shade, bone, cloth, metal, emissive, glowfill, wisp, humanoid_motion, beam, ring, spike, TAU, keys, smooth, flat)
import undead
import look_lich as L

LOOK = dict(id="acererak", name="Acererak", kind="humanoid", slim=False, height=2.0, glow="#5ff0ff", boost=1.05, dust=True, shadow=0.6, undead=True,
            captions=["", "Acererak's gems blaze, and the skulls of the Devoured begin to circle him faster.", "The Eternal Lich rises: every gem in his crown is a soul, and all of them scream."])

P = dict(id="acererak", bone="#d2d8cc", dark="#0c1018", robe="#1e2a58", trim="#d8b848", eye="#5ff0ff", eye2="#9a5cff", soul="#5ff0ff", rune="#5ff0ff", gem="#5ff0ff",
         belt="#141828", buckle="#d8b848", hood=False, brow_gems=True, eye_n=2)


def fan_collar(c, P, phase):
    """The robe's tall stand collar fanning out behind his head, edged in gold."""
    rob = cloth(P["robe"], 3, trim=P["trim"], trim_rows=1)
    c.bone("x_collar", "body", pivot=(0, 24, 3.0))
    for i, (x, h, r) in enumerate(((-6.0, 9.0, 28), (-3.4, 11.5, 12), (0, 12.5, 0), (3.4, 11.5, -12), (6.0, 9.0, -28))):
        c.box("x_collar", (x - 1.5, 23.0, 2.6), (3.0, h, 0.9), rob, k=2, rot=(0, 0, r), pivot=(x * 0.6, 24, 3.0))
        c.box("x_collar", (x - 0.5, 23.0 + h - 0.6, 2.4), (1.0, 1.0, 1.1), metal(P["trim"]), k=1, rot=(0, 0, r), pivot=(x * 0.6, 24, 3.0))
    return ["x_collar"]


def orbit_skulls(c, P, phase):
    n = (3, 5, 7)[phase - 1]
    c.bone("x_orbit", "root", pivot=(0, 19, 0))
    names = ["x_orbit"]
    bn = bone("#d8ddd0", 0.05)
    for i in range(n):
        a = TAU * i / n
        r = 12.5
        x, z = math.cos(a) * r, math.sin(a) * r
        y = 15.0 + 4.0 * math.sin(a * 2 + 1)
        nm = "x_skull%d" % i
        c.bone(nm, "x_orbit", pivot=(x, y, z))
        c.box(nm, (x - 1.7, y - 1.7, z - 1.7), (3.4, 3.2, 3.4), bn, k=2)
        c.box(nm, (x - 1.2, y - 0.5, z - 1.9), (0.9, 0.9, 0.4), emissive(P["eye"]), k=1)
        c.box(nm, (x + 0.3, y - 0.5, z - 1.9), (0.9, 0.9, 0.4), emissive(P["eye"]), k=1)
        names.append(nm)
    return names


def tome(c, P, phase):
    leather = cloth("#2a1a3a", 2, trim="#d8b848")
    page = over(flat("#d8dcc8", 0.05), on(("top", "bottom"), lambda cv: [cv.px(x, y, "#103848", P["eye"]) for x in range(1, cv.w - 1, 2) for y in range(1, cv.h - 1, 2)]))
    x, y, z = -11.5, 19.0, -4.0
    c.bone("x_tome", "root", pivot=(x, y, z))
    c.box("x_tome", (x - 4.5, y - 0.9, z - 3.2), (9.0, 0.5, 6.4), leather, k=2)
    c.box("x_tome", (x - 4.2, y - 0.4, z - 3.0), (4.2, 0.6, 6.0), page, k=2, rot=(0, 0, 8), pivot=(x - 0.1, y - 0.4, z))
    c.box("x_tome", (x + 0.0, y - 0.4, z - 3.0), (4.2, 0.6, 6.0), page, k=2, rot=(0, 0, -8), pivot=(x + 0.1, y - 0.4, z))
    c.box("x_tome", (x - 3.8, y + 0.4, z - 2.6), (7.6, 0.2, 5.2), glowfill(P["eye"], 0.5), k=1)
    return ["x_tome"]


def build(phase):
    sk = undead.make_skin(P, phase)
    c = Humanoid("acererak", phase, sk)
    c.note_extra("skirt", "body", L.skirt(c, P, phase, wide=1.05))
    c.note_extra("crown", "head", L.crown(c, P, phase, tall=1.5, metal_c="#c9a63c", gem=P["gem"]))
    c.note_extra("fan_collar", "body", fan_collar(c, P, phase))
    c.note_extra("eye_fire", "head", L.eye_fire(c, P, phase, colors=(P["eye"], P["eye2"])))
    c.note_extra("phylactery", "body", L.phylactery(c, dict(P, rune=P["eye"]), phase))
    c.note_extra("tome", "root", tome(c, P, phase))
    c.note_extra("orbit_skulls", "root", orbit_skulls(c, P, phase))
    c.note_extra("wisps", "root", L.wisps(c, P, phase, colors=(P["eye"], P["eye2"])))
    L.shoulders(c, P, phase, spiked=True, color="#c9a63c")
    c.note_extra("shoulder_spikes", "arm_r", [])
    L.base_rig(c, phase, RIG)
    R = RIG
    R.sway("x_collar", 0, 1.5, 1)
    R.sway("x_tome", 2, 6, 1, 0.1)
    R.sway("x_tome", 1, 1.6, 1, 0.3, kind="pos")
    R.sway("x_orbit", 1, 1.0, 1, 0, kind="spin")
    for i in range(7):
        R.sway("x_skull%d" % i, 0, 6, 2, i * 0.13)
    return c


def _extra(clip, t, L_):
    d = {}
    if clip in ("cast", "cast_big"):
        d["x_tome"] = {"pos": (4, 3, -5), "rot": (-30, 0, 0)}
    if clip == "death":
        f = smooth(t / L_ / 0.8)
        d["x_tome"] = {"pos": (0, -12 * f, 0), "rot": (0, 0, 40 * f)}
        d["x_orbit"] = {"pos": (0, -8 * f, 0)}
    return d


RIG = AnimRig(humanoid_motion(_extra, stride=24, arm_swing=14, bob=0.4))
