"""Why: LOLTH, Queen of the Demonweb Pits, in her drider form: the upper body of a dark-skinned, white-haired elf queen with burning red eyes and a crown of spider-leg spikes rises from the front of a
giant black spider: a thorax, a huge spotted abdomen with a red hourglass and spinnerets, and eight jointed legs that climb up and out and come down to the floor. In her right hand a scourge of
fire-coloured links. Phase 2 four more legs rise behind her like a crown of their own, phase 3 a second pair of arms bears blades, a cluster of eight eyes opens in her brow and the hourglass blazes.
A full GeoLite body (the spider is not human-shaped, so no player skin).
"""
import math
import lib
from lib import Creature, AnimRig, over, on, shade, flat, chitin, emissive, halo, wisp, bone, flesh, fur, metal, TAU, keys, smooth, humanoid_motion, spike, beam, tilt_rot, horn
import beasts as B

LOOK = dict(id="lolth", name="Lolth", kind="geo", height=3.0, glow="#ff2030", boost=0.95, shadow=1.4, walk_cycle=2.0, run_cycle=3.4, dust=False,
            captions=["", "Four more legs rise behind Lolth like a crown of black blades.", "A second pair of arms, eight burning eyes and a blazing hourglass: the Spider Queen is whole."])

LEG_N = 4


def spider_leg(c, bone_, name, start, side, fan, mat, tip, lens=(10, 17, 18, 6), wids=(3.4, 2.8, 2.2, 1.4)):
    """One leg: up and out from the thorax, then a hard bend and down to the floor. Bones name_0 .. name_3; rotation about the axis that points out sideways (+x for the left)."""
    px, py, pz = start
    d = (side, fan)
    ang = [66, 84, 14, 4]
    names = []
    par = bone_
    for i, (ln, wd, a) in enumerate(zip(lens, wids, ang)):
        bn = "%s_%d" % (name, i)
        c.bone(bn, par, pivot=(px, py, pz), rotation=tilt_rot(d[0], d[1], a))
        c.box(bn, (px - wd / 2, py, pz - wd / 2), (wd, ln, wd), tip if i == 3 else mat, k=2 if wd > 2 else 1)
        py += ln
        names.append(bn)
        par = bn
    return names


def build(phase):
    c = Creature("lolth", phase, tex_w=512)
    c.bone("root", pivot=(0, 0, 0))
    c.bone("spider", "root", pivot=(0, 20, 4))
    c.bone("body", "root", pivot=(0, 28, -4))            # the elf's torso: leans and breathes on its own
    shell = chitin("#1c1424", "#3a1a4a")
    shell2 = chitin("#2a1c38", "#5a2a6a")
    # thorax and abdomen
    c.box("spider", (-6.5, 17.0, -7.0), (13.0, 9.0, 14.0), shell, k=2)
    B.blob(c, "spider", (0, 22.0, 17.0), (17, 15, 22), over(shell, on(("top", "front", "back", "left", "right"), lambda cv: [cv.rect(x, y, 2, 2, "#4a2a5a") for x in range(2, cv.w - 2, 7) for y in range(1 + (x * 3) % 5, cv.h - 2, 8)])), bands=8, k=2)
    c.box("spider", (-1.6, 28.0, 14.0), (3.2, 0.7, 8.0), emissive("#ff2030", "#ff2030", soft=0.2), k=1)             # the hourglass
    c.box("spider", (-2.8, 28.0, 10.6), (5.6, 0.7, 3.6), emissive("#ff2030", "#ff2030", soft=0.2), k=1)
    c.box("spider", (-2.8, 28.0, 21.0), (5.6, 0.7, 3.6), emissive("#ff2030", "#ff2030", soft=0.2), k=1)
    if phase >= 3:
        c.box("spider", (-6.0, 27.0, 9.0), (12.0, 3.0, 18.0), halo("#ff2030", 0.9), k=1)
    for i in range(3):
        c.box("spider", (-1.8 + (i - 1) * 2.6, 19.0, 27.6), (1.6, 1.6, 3.0), shell2, k=1)
    # eight legs (twelve from phase two: the four rising ones are in the extras below)
    legs = []
    for side, sn in ((1, "l"), (-1, "r")):
        for i in range(LEG_N):
            fan = (i - 1.5) * 0.42
            legs.append(spider_leg(c, "spider", "spl_%s%d" % (sn, i), (side * 5.0, 21.0, -3.0 + i * 3.6), side, fan, shell2, chitin("#3a2a48", "#7a4a8a")))
    if phase >= 2:
        for side, sn in ((1, "l"), (-1, "r")):
            for i in range(2):
                # the rising legs: up, then a long curve over the shoulders
                px, py, pz = side * 3.2, 30.0, 4.0 + i * 3.0
                par = "spider"
                for j, (ln, wd, a) in enumerate(((12, 3.0, 28 + i * 10), (12, 2.6, -40), (10, 2.1, -42), (7, 1.4, -30))):
                    bn = "spb_%s%d_%d" % (sn, i, j)
                    c.bone(bn, par, pivot=(px, py, pz), rotation=tilt_rot(side * 0.8, 0.6 * (1 if i else -1) + 0.2, a))
                    c.box(bn, (px - wd / 2, py, pz - wd / 2), (wd, ln, wd), shell if j < 3 else metal("#a02a50", "#ff4060"), k=1)
                    py += ln
                    par = bn
    # the queen: torso, head, hair, crown, arms
    skin = flesh("#34304a", "#18162a", 0.015)
    robe = chitin("#1c1424", "#6a2a7a")
    c.box("body", (-4.8, 28.0, -6.5), (9.6, 11.0, 5.4), robe, k=2)
    c.box("body", (-3.8, 30.0, -7.1), (7.6, 6.0, 0.8), skin, k=1)
    c.box("body", (-5.2, 26.0, -6.8), (10.4, 2.0, 6.0), shell2, k=1)
    c.bone("head", "body", pivot=(0, 39.0, -4.0))
    c.box("head", (-3.4, 39.0, -7.2), (6.8, 7.6, 6.8), skin, k=2)
    c.box("head", (-3.0, 38.4, -7.9), (6.0, 1.4, 1.2), skin, k=1)
    hair = fur("#e8e8f0", "#ffffff")
    c.box("head", (-3.9, 39.6, -4.2), (7.8, 8.6, 1.6), hair, k=2)
    c.box("head", (-3.9, 28.0, -4.0), (7.8, 18.0, 1.4), hair, k=2)
    for sx in (-1, 1):
        c.box("head", (sx * 3.6 - (0.0 if sx > 0 else 1.0), 38.4, -7.2), (1.0, 9.0, 4.4), hair, k=1)
        beam(c, "head", (sx * 3.4, 43.4, -4.4), 4.2, 1.4, 0.5, (0, 0, -sx * 72), skin, k=1)
        c.box("head", (sx * 1.7 - 0.7, 43.4, -7.4), (1.4, 1.2, 0.8), emissive("#ff2030", "#ff2030", soft=0.1), k=1)
        c.box("head", (sx * 1.7 - 1.8, 42.2, -8.2), (3.6, 3.6, 2.0), halo("#ff2030", 0.55), k=1)
    c.box("head", (-0.9, 41.4, -7.2), (1.8, 1.2, 0.5), flat("#201830", 0.05), k=1)
    c.box("head", (-1.5, 39.8, -7.3), (3.0, 0.7, 0.5), flat("#7a1a3a", 0.05), k=1)
    for i in range(7):                                                                                        # the crown of spider-leg spikes
        a = math.radians(-70 + i * 23.3)
        spike(c, "head", (math.sin(a) * 3.4, 46.4, -4.0 - math.cos(a) * 2.8), 6.5 if i == 3 else 4.8, 1.6, metal("#14101c", "#6a4a8a"), rot=(0, 0, -math.degrees(a) * 0.45), n=3)
    c.box("head", (-0.7, 46.4, -7.4), (1.4, 1.4, 0.8), emissive("#ff2030", "#ff2030"), k=1)
    if phase >= 3:
        for i in range(8):
            c.box("head", (-2.4 + (i % 4) * 1.5 - 0.3, 44.8 + (i // 4) * 1.4, -7.6), (0.7, 0.7, 0.5), emissive("#ff4050", "#ff4050", soft=0.1), k=1)
    arms = [("arm_l", 1), ("arm_r", -1)]
    for nm, sx in arms:
        c.bone(nm, "body", pivot=(sx * 6.4, 38.0, -4.8))
        c.box(nm, (sx * 6.4 - 1.7, 25.0, -6.5), (3.4, 14.0, 3.4), skin, k=2)
        c.box(nm, (sx * 6.4 - 1.9, 24.0, -6.7), (3.8, 2.4, 3.8), skin, k=1)
        c.box(nm, (sx * 6.4 - 2.2, 36.6, -7.0), (4.4, 2.6, 4.4), shell2, k=1)
    # the scourge
    sc = c.chain("x_scourge", "arm_r", (-6.4, 25.0, -6.4), "-y", [3.6, 3.6, 3.4, 3.4, 3.2, 3.2, 3.0, 3.0], [1.4, 1.4, 1.3, 1.3, 1.2, 1.2, 1.1, 1.0], emissive("#b04aff", "#b04aff", soft=0.3), curl=(-8, 0, 0), k=1)
    c.box("x_scourge_7", (-7.4, 25.0 - 26.8 - 1.0, -7.4), (2.0, 2.4, 2.0), emissive("#ff2030", "#ff2030"), k=1)
    if phase >= 3:
        for nm, sx in (("arm2_l", 1), ("arm2_r", -1)):
            c.bone(nm, "body", pivot=(sx * 5.4, 34.0, -4.8))
            c.box(nm, (sx * 5.4 - 1.5, 21.0, -6.3), (3.0, 13.0, 3.0), skin, k=2)
            p = (sx * 5.4 + sx * 0.5, 21.0, -6.3)
            for ln, wd, a in ((8, 1.8, 20), (8, 1.5, 38), (6, 1.0, 54)):
                p = beam(c, nm, p, ln, wd, 0.6, (-60, 0, -sx * a), emissive("#ffd0d8", "#ff2030", soft=0.2), k=1, along="-y")
    # a haze of red
    n = (0, 4, 8)[phase - 1]
    for i in range(n):
        a = TAU * i / n
        nm = "x_haze%d" % i
        c.bone(nm, "root", pivot=(math.cos(a) * 17, 3, math.sin(a) * 17))
        c.box(nm, (math.cos(a) * 17 - 2.5, 3 + (i % 3) * 3, math.sin(a) * 17 - 2.5), (5.0, 9.0, 5.0), halo("#c01830", 0.65), k=1)
    R = RIG
    B.sway_names(R, sc, 0, 10, 2, 0.1, 2, 8)
    for side in "lr":
        for i in range(LEG_N):
            R.sway("spl_%s%d_1" % (side, i), 0, 3.0, 1, i * 0.12 + (0.0 if side == "l" else 0.5))
        for i in range(2):
            B.sway_names(R, ["spb_%s%d_%d" % (side, i, j) for j in range(4)], 0, 4.0, 1, 0.14, 2, 3, phase=i * 0.3)
    for nm in ("arm2_l", "arm2_r"):
        R.sway(nm, 0, 4, 1, 0.2)
    R.sway("spider", 1, 1.5, 1, 0)
    for i in range(8):
        R.sway("x_haze%d" % i, 1, 2.5, 1, i * 0.11, kind="pos")
        R.sway("x_haze%d" % i, 3, 0.22, 2, i * 0.13, kind="scale")
    return c


def _extra(clip, t, L):
    d = {}
    k = t / L
    if clip in ("walk", "run"):
        a = 20.0 if clip == "walk" else 30.0
        for side, sg in (("l", 1), ("r", -1)):
            for i in range(LEG_N):
                off = (i % 2) * 0.5 + (0.0 if side == "l" else 0.5)
                d["spl_%s%d_0" % (side, i)] = {"rot": (0, a * math.sin(TAU * (t / L - off)) * sg, 0)}
                d["spl_%s%d_1" % (side, i)] = {"rot": (6 * math.sin(TAU * (t / L - off)) * sg, 0, 0)}
        d["spider"] = {"pos": (0, 0.8 * abs(math.sin(TAU * t / L)), 0)}
    if clip == "attack":
        lg = keys(k, [(0, 0.0), (0.24, -0.4), (0.42, 1.0), (0.7, 0.1), (1, 0.0)])
        for i in range(8):
            d["x_scourge_%d" % i] = {"rot": (-16 * lg, 0, 0)}
        d["spider"] = {"rot": (-6 * lg, 0, 0), "pos": (0, 0, -3.0 * max(0.0, lg))}
    if clip in ("cast", "cast_big"):
        up = 0.6 if clip == "cast" else keys(k, [(0, 0), (0.35, 1.0), (0.8, 1.2), (0.9, 0), (1, 0)])
        d["spider"] = {"rot": (12 * up, 0, 0), "pos": (0, 2 * up, 0)}
        for side in "lr":
            for i in range(LEG_N):
                d["spl_%s%d_0" % (side, i)] = {"rot": (0, 0, 0)}
    if clip == "death":
        f = smooth(k / 0.7)
        d["spider"] = {"pos": (0, -8 * f, 0)}
        for side, sg in (("l", 1), ("r", -1)):
            for i in range(LEG_N):
                d["spl_%s%d_1" % (side, i)] = {"rot": (0, 0, 40 * sg * f)}
    return d


RIG = AnimRig(humanoid_motion(_extra, stride=0, arm_swing=14, bob=0.4, legs_still=True, cast_arms=(-140, -140)))
