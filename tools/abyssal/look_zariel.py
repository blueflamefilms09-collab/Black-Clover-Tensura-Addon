"""Why: ZARIEL, Archduchess of Avernus, a fallen angel: burnt black-red plate with glowing seams over golden skin, a long dark mane, gold eyes, a broken halo of fire hanging behind her head,
charred angelic wings with burning tips, a sword that is a blade of flame held in her right hand, and flames running off her pauldrons. Phase 2 the wings are larger and burn harder, phase 3
a second pair opens, the plate turns molten and the halo is whole again, a ring of fire. Player skin plus extras.
"""
import math
import lib
from lib import (Humanoid, AnimRig, over, on, shade, bone, cloth, metal, plate, emissive, glowfill, wisp, halo, humanoid_motion, beam, spike, ring, TAU, keys, smooth, flat, feathers, feather_wing)
import humans
import look_lich as L

LOOK = dict(id="zariel", name="Zariel", kind="humanoid", slim=True, height=2.0, glow="#ff7a20", boost=1.0, shadow=0.5, dust=False, hair="long", hair_color="#241014", hold_r=-0.42,
            captions=["", "Zariel's wings catch fire and the plate begins to glow.", "The Archduchess of Avernus unfolds a second pair of wings; her halo closes into a ring of fire."])

P = dict(id="zariel", skin="#d9a884", eye="#ffc040", eye_glow="#ffc040", hair="#241014", hair_style="long", mouth="line", brow="#2a1414", coat="#3a1a1c", trim="#c8602a", shirt="#2a1416",
         belt="#2a1014", buckle="#e8a040", boots="#2a1414", legs="#2a1416", armor="#4a2420", armor_trim="#e0702a", glow="#ff6a1a", eye_solid=True, cuff="#4a2420", scar=None, eye_big=0)


def halo_ring(c, P, phase):
    c.bone("x_halo", "head", pivot=(0, 36, 3.5))
    n = 6 if phase < 3 else 12
    r = 8.0
    for i in range(n):
        if phase < 3 and i in (2, 3):
            continue                                  # a broken halo: the gap at the top left
        a = TAU * i / n + 0.2
        x, y = math.cos(a) * r, 36 + math.sin(a) * r * 0.9 + 4
        seg = 2 * math.pi * r / n * 0.95
        c.box("x_halo", (x - seg / 2, y - 0.7, 2.9), (seg, 1.4, 1.2), emissive("#ffb040", "#ffb040", soft=0.2), k=1, rot=(0, 0, math.degrees(a) + 90), pivot=(x, y, 3.5))
    c.box("x_halo", (-10.5, 31.5, 2.6), (21, 17, 0.6), halo("#ff8030", 0.55), k=1)
    return ["x_halo"]


def sword(c, P, phase):
    steel = metal("#c8c0b8", "#ffffff")
    c.bone("x_sword", "arm_r", pivot=(-6.0, 10.6, -1.0))
    c.box("x_sword", (-6.8, 9.8, -1.2), (1.6, 3.2, 1.6), bone("#3a2018"), k=1)                       # grip
    c.box("x_sword", (-8.8, 12.8, -1.5), (5.6, 1.0, 2.2), metal("#8a6a30", "#e8c060"), k=1)       # guard
    ln = 22 + 4 * (phase - 1)
    c.box("x_sword", (-6.7, 13.8, -0.9), (1.4, ln, 0.8), emissive("#ffd070", "#ffb040", soft=0.2), k=2, rot=(-62, 0, 0), pivot=(-6.0, 13.8, -1.0))
    c.box("x_sword", (-7.6, 13.4, -1.8), (3.2, ln + 2, 2.6), halo("#ff6a1a", 0.8), k=1, rot=(-62, 0, 0), pivot=(-6.0, 13.8, -1.0))
    names = ["x_sword"]
    for i in range(3):
        nm = "x_swordflame%d" % i
        c.bone(nm, "x_sword", pivot=(-6.0, 13.8, -1.0))
        t = (i + 1) / 4
        tip = L.rotpt((0, ln * t, 0), (-62, 0, 0))
        c.box(nm, (-6.0 + tip[0] - 0.9, 13.8 + tip[1] - 0.5, -1.0 + tip[2] - 0.9), (1.8, 3.0 + phase, 1.8), wisp("#ff8a2a", 1.0), k=1)
        names.append(nm)
    return names


def pauldrons(c, P, phase):
    pm = plate(P["armor"], rim=P["armor_trim"])
    names = []
    for s, arm in ((-1, "arm_r"), (1, "arm_l")):
        c.box(arm, (s * 6.5 - 2.9, 21.7, -3.1), (5.8, 2.6, 6.2), pm, k=2)
        spike(c, arm, (s * 7.3, 24.1, -1.6), 6.0, 1.8, pm, rot=(0, 0, -s * 14), n=3)
        spike(c, arm, (s * 7.3, 24.1, 1.6), 5.0, 1.6, pm, rot=(0, 0, -s * 14), n=3)
        c.box(arm, (s * 6.5 - 0.9, 24.0, -0.9), (1.8, 1.8, 1.8), emissive("#ff8a2a"), k=1)
    return names


def flames(c, P, phase):
    names = []
    n = (4, 6, 8)[phase - 1]
    pts = [(-7.4, 27, 0), (7.4, 27, 0), (0, 25, 3.4), (-3.2, 19, 3.2), (3.2, 19, 3.2), (-5.0, 14, -2.2), (5.0, 14, -2.2), (0, 31, 3.0)]
    for i in range(n):
        x, y, z = pts[i]
        nm = "x_flame%d" % i
        c.bone(nm, "body", pivot=(x, y, z))
        c.box(nm, (x - 1.3, y, z - 1.3), (2.6, 7 + (i % 3) * 2 + phase * 2, 2.6), wisp("#ff7a20" if i % 2 else "#ffb040", 1.0), k=1)
        names.append(nm)
    return names


def build(phase):
    sk = humans.make_skin(dict(P, eye_big=0), phase, slim=True)
    # the plate turns molten in phase 3: the seams are brighter and wider
    c = Humanoid("zariel", phase, sk)
    c.kit_skin = humans.make_skin(dict(P, eye_big=0, kit_hair=True), phase, slim=True)
    feather = feathers("#2a1214", "#7a2010", "#ff5a10")
    span = (30, 38, 40)[phase - 1]
    wings = feather_wing(c, "body", "x_wing_r", (-3.5, 21.0, 3.0), -1, span, feather, feathers("#3a1618", "#a03010", "#ff7a20"), rows=5, height=span * 0.95)
    wings += feather_wing(c, "body", "x_wing_l", (3.5, 21.0, 3.0), 1, span, feather, feathers("#3a1618", "#a03010", "#ff7a20"), rows=5, height=span * 0.95)
    if phase >= 3:
        wings += feather_wing(c, "body", "x_wing2_r", (-3.0, 17.0, 3.6), -1, span * 0.7, feather, rows=4, height=span * 0.7, tilt=-6)
        wings += feather_wing(c, "body", "x_wing2_l", (3.0, 17.0, 3.6), 1, span * 0.7, feather, rows=4, height=span * 0.7, tilt=-6)
    c.note_extra("wings", "body", wings)
    c.note_extra("halo", "head", halo_ring(c, P, phase))
    c.note_extra("pauldrons", "arm_r", pauldrons(c, P, phase))
    c.note_extra("flame_sword", "arm_r", sword(c, P, phase))
    c.note_extra("flames", "body", flames(c, P, phase))
    R = RIG
    for s, sg in (("r", -1), ("l", 1)):
        R.sway("x_wing_" + s, 2, 5 * sg, 1, 0)
        R.sway("x_wing_%s2" % s, 2, 9 * sg, 1, 0.12)
        R.sway("x_wing2_" + s, 2, 6 * sg, 1, 0.3)
        R.sway("x_wing2_%s2" % s, 2, 8 * sg, 1, 0.4)
    R.sway("x_halo", 1, 1.0, 1, 0, kind="spin")
    for i in range(8):
        R.sway("x_flame%d" % i, 3, 0.3, 3, i * 0.17, kind="scale")
        R.sway("x_flame%d" % i, 0, 5, 3, i * 0.2)
    for i in range(3):
        R.sway("x_swordflame%d" % i, 3, 0.3, 4, i * 0.3, kind="scale")
    return c


def _extra(clip, t, L_):
    d = {}
    if clip in ("cast", "cast_big"):
        up = 1.0 if clip == "cast" else keys(t / L_, [(0, 0), (0.35, 1.0), (0.8, 1.2), (0.9, 0), (1, 0)])
        for s, sg in (("r", -1), ("l", 1)):
            d["x_wing_" + s] = {"rot": (0, 0, 24 * sg * up)}
            d["x_wing_%s2" % s] = {"rot": (0, 0, 16 * sg * up)}
    if clip == "attack":
        for s, sg in (("r", -1), ("l", 1)):
            d["x_wing_" + s] = {"rot": (0, 0, 16 * sg * math.sin(math.pi * t / L_))}
    return d


RIG = AnimRig(humanoid_motion(_extra, stride=26, arm_swing=18, bob=0.5))
