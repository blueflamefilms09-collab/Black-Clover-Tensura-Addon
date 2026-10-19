"""Why: VECNA, the Whispered One: a lich who lost his left eye and his left hand and wears the relics that replaced them. So the skin has one red-lit eye and a sewn, scarred socket, the left arm is
shortened to a bound stump (`part_scale`, a table field the converter can apply), and the extras are what makes him Vecna and not another lich: the HAND OF VECNA, a mummified hand with a glowing
palm hanging in the air beyond the stump, and the EYE OF VECNA, a bloodshot eye in a black iron ring that floats at his shoulder and, from phase two, looks out in rays. A ragged black web-trimmed
cape and a dark smoke aura finish him; there is no staff and no crown, he is the pale, quiet one.
"""
import math
import lib
from lib import (Humanoid, AnimRig, over, on, shade, bone, cloth, metal, emissive, glowfill, wisp, halo, humanoid_motion, beam, spike, TAU, keys, smooth, flat, eyeball, banded)
import undead
import look_lich as L

LOOK = dict(id="vecna", name="Vecna", kind="humanoid", slim=False, height=2.0, glow="#ff4040", boost=1.0, dust=True, shadow=0.55, undead=True, part_scale={"arm_l": [1.0, 0.62, 1.0]},
            captions=["", "The Hand of Vecna ignites and the Eye opens: he sees everyone in the hall.", "Vecna is whole again. The Hand burns, the Eye is wide open, and the web closes."])

P = dict(id="vecna", bone="#b9b2a8", dark="#100a12", robe="#18101e", trim="#a8802c", eye="#ff3030", eye2="#ffb040", soul="#ff4040", rune="#ff4040", gem="#ff4040",
         belt="#120c14", buckle="#a8802c", hood=True, one_eye="r", eye_n=2, no_hood_trim=False)


def stump(sk, phase):
    """The left arm ends in black bandages stitched with red."""
    for f in ("front", "back", "right", "left", "top", "bottom"):
        cv = sk.face("lsleeve", f)
        if f not in ("top",):
            cloth("#120c14", 2)(cv)
            cv.rect(0, 0, cv.w, 3, "#18101e")
            if f in ("front", "back", "right", "left"):
                for y in range(4, 12, 2):
                    cv.rect(0, y, cv.w, 1, "#1d1220", "#8a1020" if phase >= 2 else None)
    for f in ("front", "back", "right", "left"):
        cv = sk.face("larm", f)
        banded("#2a1c2c", "#181018", 2)(cv)
        cv.rect(0, 9, cv.w, 3, "#241624", "#601018" if phase >= 2 else None)


def hand(c, P, phase):
    """The Hand of Vecna: a mummified left hand floating where the wrist should be, palm out, glowing."""
    skin = over(banded("#8a6a44", "#6a4c30", 2), on(("front", "back"), lambda cv: cv.line(0, 0, cv.w - 1, cv.h - 1, "#3a2410", "#d02020" if phase >= 2 else None)))
    x, y, z = 7.0, 12.6, -3.2
    c.bone("x_hand", "root", pivot=(x, y, z))
    c.box("x_hand", (x - 1.9, y - 1.0, z - 0.8), (3.8, 3.8, 1.8), skin, k=2)
    for i, (dx, h) in enumerate(((-1.5, 3.4), (-0.5, 4.0), (0.5, 3.8), (1.5, 3.0))):
        beam(c, "x_hand", (x + dx, y + 2.8, z), h * 0.55, 0.85, 1.0, (-8, 0, -dx * 6), skin, k=1)
        beam(c, "x_hand", (x + dx + dx * 0.15, y + 2.8 + h * 0.52, z - 0.45), h * 0.45, 0.8, 0.9, (-28, 0, -dx * 9), skin, k=1)
    beam(c, "x_hand", (x + 2.0, y + 0.2, z), 2.2, 0.9, 1.0, (-5, 0, -48), skin, k=1)
    c.box("x_hand", (x - 1.2, y - 0.2, z - 1.15), (2.4, 2.4, 0.4), emissive("#ff3030", soft=0.3), k=1)         # the palm's glow
    c.box("x_hand", (x - 3.0, y - 2.0, z - 2.2), (6.0, 7.5, 3.2), halo("#ff3030", 0.8) if phase >= 2 else halo("#a02020", 0.6), k=1)
    if phase >= 2:
        for i, dx in enumerate((-1.5, -0.5, 0.5, 1.5)):
            nm = "x_handflame%d" % i
            c.bone(nm, "x_hand", pivot=(x + dx, y + 6.2, z))
            c.box(nm, (x + dx - 0.5, y + 6.0, z - 0.5), (1.0, 2.0 + phase, 1.0), glowfill("#ff7030"), k=1)
    return ["x_hand"] + (["x_handflame%d" % i for i in range(4)] if phase >= 2 else [])


def eye(c, P, phase):
    """The Eye of Vecna: an eyeball in a ring of black iron with spikes, hanging at his right shoulder; the iris looks forward."""
    x, y, z = -9.0, 31.0, -1.0
    c.bone("x_eye", "root", pivot=(x, y, z))
    ball = eyeball("#ff3020", "#100808", "#e8dccc", "#a02020")
    s = 2.6 + 0.5 * (phase - 1)
    c.box("x_eye", (x - s, y - s, z - s), (2 * s, 2 * s, 2 * s), ball, k=2)
    c.box("x_eye", (x - s - 0.7, y - 0.5, z - 1.2), (2 * s + 1.4, 1.0, 2.4), metal("#2a2430", "#6a6070"), k=1)
    c.box("x_eye", (x - 0.5, y - s - 0.7, z - 1.2), (1.0, 2 * s + 1.4, 2.4), metal("#2a2430", "#6a6070"), k=1)
    for a in range(4):
        spike(c, "x_eye", (x + math.cos(a * math.pi / 2 + 0.78) * (s + 0.5), y + math.sin(a * math.pi / 2 + 0.78) * (s + 0.5), z), 2.6, 1.0, metal("#2a2430", "#6a6070"),
              rot=(0, 0, math.degrees(a * math.pi / 2 + 0.78) - 90), n=2)
    c.box("x_eye", (x - s - 1.6, y - s - 1.6, z - s - 1.6), (2 * s + 3.2, 2 * s + 3.2, 2 * s + 3.2), halo("#ff3020", 0.9), k=1)
    names = ["x_eye"]
    rays = 0 if phase == 1 else (1 if phase == 2 else 3)
    for i in range(rays):
        nm = "x_ray%d" % i
        c.bone(nm, "x_eye", pivot=(x, y, z - s))
        yaw = (i - (rays - 1) / 2) * 16
        c.box(nm, (x - 0.35, y - 0.35, z - s - 18), (0.7, 0.7, 18), wisp("#ff3020", 1.0), k=1, rot=(0, yaw, 0), pivot=(x, y, z - s))
        names.append(nm)
    return names


def cape(c, P, phase):
    rob = cloth("#120c16", 4, tatter=5, trim="#a8802c", trim_rows=1)
    names = []
    for i, x in enumerate((-3.2, 0.0, 3.2)):
        n = "x_cape%d" % i
        c.bone(n, "body", pivot=(x, 23.6, 2.6))
        c.box(n, (x - 1.7, 1.2 - i % 2 * 2, 2.3), (3.4, 22.4 + i % 2 * 2, 0.9), rob, k=2)
        names.append(n)
    return names


def build(phase):
    sk = undead.make_skin(P, phase)
    stump(sk, phase)
    sk.opaque_base()
    c = Humanoid("vecna", phase, sk)
    c.note_extra("skirt", "body", L.skirt(c, P, phase))
    c.note_extra("cape", "body", cape(c, P, phase))
    c.note_extra("eye_fire", "head", L.eye_fire(c, P, phase, colors=("#ff3030", "#ffb040"), which=(0,)))
    c.note_extra("hand_of_vecna", "root", hand(c, P, phase))
    c.note_extra("eye_of_vecna", "root", eye(c, P, phase))
    c.note_extra("wisps", "root", L.wisps(c, P, phase, colors=("#c02030", "#601020"), count=(4, 7, 10)[phase - 1]))
    # the player-model kit has no per-part scale, so the missing hand is also drawn as a black bound cap over the end of the left arm (the composite shortens the arm as well)
    c.box("arm_l", (3.65, 9.65, -2.65), (4.7, 4.7, 5.3), cloth("#120c14", 2, trim="#8a1020", trim_rows=1), k=1)
    c.note_extra("stump_cap", "arm_l", [])
    bn = bone("#b9b2a8", 0.06)
    c.box("arm_r", (-8.9, 21.6, -2.9), (4.8, 1.6, 5.8), bn, k=2)
    spike(c, "arm_r", (-7.4, 23.0, -1.4), 5.0, 1.6, bn, rot=(0, 0, 16), n=3)
    spike(c, "body", (4.8, 23.2, 0), 4.5, 1.6, bn, rot=(0, 0, -16), n=3)
    c.note_extra("shoulder_spikes", "body", [])
    L.base_rig(c, phase, RIG)
    R = RIG
    for i in range(3):
        R.sway("x_cape%d" % i, 0, 6, 1, 0.12 * i)
    R.sway("x_hand", 1, 1.5, 1, 0.0, kind="pos")
    R.sway("x_hand", 2, 6, 1, 0.2)
    R.sway("x_eye", 1, 1.2, 1, 0.5, kind="pos")
    R.sway("x_eye", 1, 10, 1, 0.0)
    for i in range(4):
        R.sway("x_handflame%d" % i, 3, 0.3, 3, i * 0.2, kind="scale")
    return c


def _extra(clip, t, L_):
    d = {"arm_l": {"scale": (1.0, 0.62, 1.0)}}                       # the missing hand: the arm is always short
    if clip in ("cast", "cast_big"):
        d["x_hand"] = {"pos": (-3, 5, -5)}
        d["x_eye"] = {"pos": (4, 0, -3)}
    if clip == "death":
        f = smooth(t / L_ / 0.8)
        d["x_hand"] = {"pos": (0, -10 * f, 0)}
        d["x_eye"] = {"pos": (0, -26 * f, 0)}
    return d


RIG = AnimRig(humanoid_motion(_extra, stride=24, arm_swing=14, bob=0.4))
