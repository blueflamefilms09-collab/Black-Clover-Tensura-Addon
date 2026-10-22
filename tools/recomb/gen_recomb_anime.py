"""Recombination Magic, the "anime look": a colossal house-golem (the Raging Black Bull as Henry Legolant's town rebuilt into a giant), the recombined house, the ring of
building pieces and the brick drill. Writes the geo models, animations, textures and glow maps (assets/nusmp/geo|animations|textures/entity/recombanime_*) and prints the
numbers the Java rules class copies (tools/recomb/golem_numbers.json).

    python3 -B tools/recomb/gen_recomb_anime.py                 # everything
    python3 -B tools/recomb/gen_recomb_anime.py golem house     # some
    python3 -B tools/geo_check.py recombanime_golem recombanime_house recombanime_ring recombanime_drill

What the golem is (the anime frames of Henry's Raging Black Bull): a humanoid built of houses. The torso is stacked houses (a plastered timber-frame belly under a slate cornice, a
chest of stone and timber with arched windows, gabled roofs on the shoulders with a chimney on one and a small tree on the other), the head a stone block with two glowing
yellow-green slit eyes and two TOWERS for horns (crenellated crowns under cone roofs); the arms are stacked house segments ending in blocky brick fists; the legs are thick pillars of
stone and timber standing on stone slabs. Walk, punch, stomp (settle), roar (fire), assemble from the ground and collapse are keyframed on the bones.
Every number below is in model pixels (16 per block); the prop painter scales the model (client/prop/RecombAnimePainter). Deterministic: the same files every run.
"""
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from recomb_kit import (Sheet, Anim, wave, keys, S, PLASTER, PLASTER_D, STONE, STONE_D, BRICK, BRICK_D, ROOT)  # noqa: E402

KEY = "recombanime"
HERE = os.path.dirname(os.path.abspath(__file__))


# ------------------------------------------------------------------------------------------------ face recipes
def timber(windows=None, door=None, moss=None, bay=13.0, shutters=False, plaster=None, braces=True, rails=None):
    o = {"windows": windows or [], "bay": bay, "shutters": shutters, "braces": braces}
    if door:
        o["door"] = door
    if moss:
        o["moss"] = moss
    if plaster:
        o["plaster"] = plaster
    if rails is not None:
        o["rails"] = rails
    return ("timber", o)


def stone(windows=None, door=None, moss=None, base=None, blk=7.0, row=4.0, moss_amount=0.5):
    o = {"windows": windows or [], "blk": blk, "row": row, "moss_amount": moss_amount}
    if door:
        o["door"] = door
    if moss:
        o["moss"] = moss
    if base:
        o["base"] = base
    return ("stone", o)


def brick(**kw):
    return ("brick", kw)


def roof(tone="slate", moss=None, across=False, ridge=False, row=3.0, sw=3.0, moss_amount=0.55):
    o = {"tone": tone, "across": across, "ridge": ridge, "row": row, "sw": sw, "moss_amount": moss_amount}
    if moss:
        o["moss"] = moss
    return ("slate", o)


GRASS = ("grass", {})
LEAVES = ("leaves", {})
WOOD = ("wood", {})
IRONBAND = ("iron", {})


def gable_roof(sh, bone, cx, y0, half, depth, rise, tone="slate", moss="top", over=2.2, thick=1.6, steps=3, wall=None, z0=None, sign_flip=False):
    """A gabled roof with its ridge along z over a body of width 2*half centred at cx: a stepped gable wall (front and back show it) and two sloped slabs with an overhang.
    y0 = the top of the body; rise = the height of the ridge above y0. Returns the y of the ridge."""
    z0 = -depth / 2 if z0 is None else z0
    wall = wall or timber(bay=20.0, rails=[])
    sh_h = rise / steps
    for i in range(steps):
        w = 2 * half * (1.0 - (i + 0.5) / (steps + 0.6))
        sh.cube(bone, (cx - w / 2, y0 + i * sh_h, z0), (w, sh_h + 0.02, depth), base=wall, faces={"top": roof(tone), "bottom": roof(tone)}, hide=("top", "bottom") if i < steps - 1 else ("bottom",))
    ang = math.degrees(math.atan2(rise, half + over))
    length = math.hypot(half + over, rise)
    yr = y0 + rise
    for sgn in (1, -1):
        if sgn == 1:                                                  # the +x slope: starts at the ridge, goes to +x, its outer end down (a turn of -ang about z)
            o = (cx, yr - thick, z0 - over)
            rot = (0, 0, -ang)
        else:
            o = (cx - length, yr - thick, z0 - over)
            rot = (0, 0, ang)
        sh.cube(bone, o, (length, thick, depth + 2 * over), base=roof(tone, moss=None, across=True, row=3.0), faces={"top": roof(tone, moss=moss, across=True), "bottom": roof(tone, across=True)},
                hide=("bottom",), rot=rot, pv=(cx, yr, 0))
    sh.cube(bone, (cx - 1.2, yr - 0.6, z0 - over - 0.4), (2.4, 1.5, depth + 2 * over + 0.8), base=roof(tone, ridge=True, across=True), hide=("bottom",))
    return yr


# ------------------------------------------------------------------------------------------------ numbers shared with the Java side
NUM = {}


def fk_arm(shoulder_pivot, forearm_pivot, fist_c, torso_pivot, hips_pivot, arm_a, fore_a, torso_a, hips_a=0.0):
    """Forward kinematics (model px) of the fist centre: rotations about x, a positive angle swings a hanging limb FORWARD (toward -z). Returns (y, z)."""
    def rot(p, c, a):
        a = math.radians(a)
        dy, dz = p[0] - c[0], p[1] - c[1]
        return (c[0] + dy * math.cos(a) - dz * math.sin(a), c[1] + dy * math.sin(a) + dz * math.cos(a))
    p = (fist_c[0], fist_c[1])
    p = rot(p, (forearm_pivot[0], forearm_pivot[1]), fore_a)
    p = rot(p, (shoulder_pivot[0], shoulder_pivot[1]), arm_a)
    p = rot(p, (torso_pivot[0], torso_pivot[1]), torso_a)
    p = rot(p, (hips_pivot[0], hips_pivot[1]), hips_a)
    return p


# ================================================================================================================================ the golem
def golem():
    sh = Sheet(KEY, "golem", tex_w=2048, K=3.4, seed=7)
    sh.bone("root", pivot=(0, 0, 0))
    sh.bone("hips", "root", (0, 46, 0))
    sh.bone("leg_l", "hips", (14, 46, 0))
    sh.bone("shin_l", "leg_l", (14, 24, 0))
    sh.bone("leg_r", "hips", (-14, 46, 0))
    sh.bone("shin_r", "leg_r", (-14, 24, 0))
    sh.bone("torso", "hips", (0, 52, 0))
    sh.bone("head", "torso", (0, 100, 0))
    sh.bone("horn_l", "head", (9.5, 114, 0))
    sh.bone("horn_r", "head", (-9.5, 114, 0))
    sh.bone("arm_l", "torso", (42, 92, 0))
    sh.bone("fore_l", "arm_l", (42, 62, 0))
    sh.bone("arm_r", "torso", (-42, 92, 0))
    sh.bone("fore_r", "arm_r", (-42, 62, 0))
    sh.bone("chimney", "torso", (27, 108, -5))
    sh.bone("treetop", "torso", (-22, 106, 0))

    # ---- pelvis: a stone platform on arches
    sh.cube("hips", (-28, 46, -16), (56, 6, 32), base=stone(base=(150, 144, 134), blk=9.0, row=3.0, moss="bottom"), faces={
        "north": stone(windows=[(14, 3.2, 4.4, 4.2, "dark"), (28, 3.2, 4.4, 4.2, "dark"), (42, 3.2, 4.4, 4.2, "dark")], base=(150, 144, 134), blk=9.0, row=3.0)},
        hide=("bottom", "top"))

    # ---- legs: thick pillars of stone and timber on stone slabs
    for sx, n in ((1, "l"), (-1, "r")):
        cx = 14 * sx
        leg, shin = "leg_" + n, "shin_" + n
        sh.cube(shin, (cx - 13, 0, -17), (26, 5, 30), base=stone(base=(168, 162, 150), blk=9.0, row=2.6, moss="bottom"), faces={"top": ("grass", {"cover": 0.5})}, hide=("bottom",))
        sh.cube(shin, (cx - 9.5, 5, -9.5), (19, 17, 19), base=brick(bw=6.0, bh=3.0, base=(168, 100, 72), moss="bottom"), faces={"top": stone()}, hide=("top", "bottom"))
        sh.cube(shin, (cx - 10.4, 9.5, -10.4), (20.8, 2.2, 20.8), base=("iron", {}), hide=("top", "bottom"))
        sh.cube(leg, (cx - 11.5, 22, -11.5), (23, 4, 23), base=stone(base=(176, 170, 158), blk=8.0, row=2.2), hide=("bottom",))
        sh.cube(leg, (cx - 10, 26, -10), (20, 20, 20), base=timber(bay=20.0, rails=[], windows=[(10, 10, 5.5, 9, "lit" if sx > 0 else "dark")]), faces={
            "north": timber(windows=[(10, 9.5, 6, 9.5, "lit")], bay=20.0, rails=[], shutters=True), "south": stone(base=(160, 154, 144), moss="top"),
            "west": stone(windows=[(10, 10, 5, 8, "dark")], base=(160, 154, 144), moss="top"), "east": stone(windows=[(10, 10, 5, 8, "dark")], base=(160, 154, 144), moss="top")},
            hide=("bottom", "top"))

    # ---- belly: a plastered house with a door, under a slate cornice
    sh.cube("torso", (-22, 52, -14), (44, 16, 28), base=timber(bay=14.0, rails=[], windows=[(7, 7.5, 5, 7.5, "lit"), (37, 7.5, 5, 7.5, "lit")], door=(22, 7.2, 11.5)), faces={
        "north": timber(bay=14.0, rails=[], windows=[(7, 7.5, 5, 7.5, "lit"), (37, 7.5, 5, 7.5, "lit")], door=(22, 7.2, 11.5)),
        "south": timber(bay=14.0, rails=[], windows=[(11, 7.5, 5, 7.5, "lit"), (33, 7.5, 5, 7.5, "dark")]),
        "west": timber(bay=14.0, rails=[], windows=[(14, 7.5, 5, 7.5, "lit")]), "east": timber(bay=14.0, rails=[], windows=[(14, 7.5, 5, 7.5, "dark")])},
        hide=("bottom", "top"))
    sh.cube("torso", (-26, 68, -18), (52, 3, 36), base=roof("tile", across=True), faces={"top": ("slate", {"tone": "tile", "across": True})}, hide=("bottom",))

    # ---- chest: stone base and a timber-framed storey of arched windows
    sh.cube("torso", (-32, 71, -17), (64, 7, 34), base=stone(base=(150, 144, 134), blk=9.0, row=3.5, moss="bottom"), faces={
        "north": stone(base=(150, 144, 134), blk=9.0, row=3.5, moss="bottom", windows=[(10, 3.5, 4.4, 4.6, "dark"), (54, 3.5, 4.4, 4.6, "dark")]),
        "west": stone(base=(150, 144, 134), blk=9.0, row=3.5, moss="bottom"), "east": stone(base=(150, 144, 134), blk=9.0, row=3.5, moss="bottom")}, hide=("bottom", "top"))
    chest_front = timber(bay=12.8, windows=[(6.4, 10.5, 6, 10.5, "lit"), (19.2, 10.5, 6, 10.5, "dark"), (32, 10.5, 7, 11.5, "lit"), (44.8, 10.5, 6, 10.5, "dark"), (57.6, 10.5, 6, 10.5, "lit")], shutters=True, moss="top")
    chest_back = timber(bay=12.8, windows=[(6.4, 10.5, 6, 10.5, "dark"), (19.2, 10.5, 6, 10.5, "lit"), (32, 10.5, 7, 11.5, "dark"), (44.8, 10.5, 6, 10.5, "lit"), (57.6, 10.5, 6, 10.5, "dark")], moss="top")
    chest_side = timber(bay=17.0, windows=[(8.5, 10.5, 6, 10.5, "lit"), (25.5, 10.5, 6, 10.5, "lit")], moss="top")
    sh.cube("torso", (-32, 78, -17), (64, 20, 34), base=chest_front, faces={"north": chest_front, "south": chest_back, "west": chest_side, "east": chest_side}, hide=("bottom", "top"))
    sh.cube("torso", (-33.5, 77.3, -18.5), (67, 1.6, 37), base=("iron", {}), hide=("bottom",), faces={"top": ("iron", {})})

    # ---- shoulder houses: gabled roofs, a chimney on the left, a small tree on the right
    gable_roof(sh, "torso", 22, 98, 10, 34, 9.5, tone="slate", moss="top", wall=timber(bay=20.0, rails=[], windows=[(10, 1.4, 3, 2.2, "lit")]))
    gable_roof(sh, "torso", -22, 98, 10, 34, 9.5, tone="tile", moss="top", wall=timber(bay=20.0, rails=[], windows=[(10, 1.4, 3, 2.2, "lit")]))
    sh.cube("chimney", (24.5, 102, -8), (6, 15, 6), base=("chimney", {}), faces={"top": stone(base=(80, 72, 70))}, hide=("bottom",))
    sh.cube("chimney", (23.7, 116.4, -8.8), (7.6, 2.0, 7.6), base=stone(base=(176, 170, 158), blk=5.0, row=2.0), hide=("bottom",))
    sh.cube("treetop", (-23.6, 104, -1.6), (3.2, 9, 3.2), base=WOOD, hide=("bottom",), faces={"top": WOOD})
    sh.cube("treetop", (-29, 111, -6), (14, 6, 12), base=LEAVES, hide=("bottom",))
    sh.cube("treetop", (-27, 116.5, -4.5), (10, 5, 9), base=LEAVES, hide=("bottom",))
    sh.cube("treetop", (-25, 120.8, -3), (6, 4, 6), base=LEAVES, hide=("bottom",))

    # ---- the terrace between the shoulders carries the head
    sh.cube("torso", (-12, 97.6, -17), (24, 2.4, 34), base=stone(base=(168, 162, 150), blk=8.0, row=2.2), faces={"top": GRASS}, hide=("bottom",))

    # ---- head: a stone block with a slit-eyed visor, a brow roof, and two towers for horns
    sh.cube("head", (-10, 100, -10), (20, 16, 20), base=stone(base=(132, 126, 118), blk=6.0, row=3.0, moss="bottom"), faces={"north": ("visor", {}), "top": stone()}, hide=("bottom",))
    sh.cube("head", (-11.5, 99, -11.5), (23, 3, 23), base=("iron", {}), hide=("bottom", "top"))
    sh.cube("head", (-12, 108, -9), (2.6, 9, 18), base=stone(base=(150, 144, 134), blk=6.0, row=3.0), hide=("bottom",))
    sh.cube("head", (9.4, 108, -9), (2.6, 9, 18), base=stone(base=(150, 144, 134), blk=6.0, row=3.0), hide=("bottom",))
    sh.cube("head", (-11.5, 115.4, -11.5), (23, 2.6, 23), base=roof("slate", moss="top", across=True), hide=("bottom",))
    sh.cube("head", (-8, 118, -8), (16, 2.2, 16), base=roof("slate", across=True), hide=("bottom",))
    for sx, n in ((1, "l"), (-1, "r")):
        bone = "horn_" + n
        px = 9.5 * sx
        rot = (0, 0, -20 * sx)
        pv = (px, 114, 0)
        sh.cube(bone, (px - 4.2, 114, -4.2), (8.4, 20, 8.4), base=("tower", {"slits": [(4.2, 9, 1.1, 5), (4.2, 15.5, 1.1, 3)]}), hide=("bottom",), rot=rot, pv=pv)
        sh.cube(bone, (px - 5.6, 134, -5.6), (11.2, 3.2, 11.2), base=stone(base=(150, 144, 134), blk=4.0, row=1.6), hide=("bottom",), rot=rot, pv=pv)
        for mx in (-1, 1):
            for mz in (-1, 1):
                sh.cube(bone, (px + mx * 3.8 - 1.5, 137.2, mz * 3.8 - 1.5), (3, 2.6, 3), base=stone(base=(176, 170, 158), blk=3.0, row=1.5), hide=("bottom",), rot=rot, pv=pv)
        for i, (w, h) in enumerate(((9.0, 3.2), (6.8, 3.2), (4.6, 3.2), (2.4, 4.0))):
            y = 137.2 + sum(hh for _, hh in ((9.0, 3.2), (6.8, 3.2), (4.6, 3.2), (2.4, 4.0))[:i])
            sh.cube(bone, (px - w / 2, y, -w / 2), (w, h, w), base=roof("slate", across=True, row=2.4, sw=2.4), hide=("bottom",), rot=rot, pv=pv)
        sh.cube(bone, (px - 0.25, 150.2, -0.25), (0.5, 5, 0.5), base=IRONBAND, hide=("bottom",), rot=rot, pv=pv)

    # ---- arms: stacked house segments ending in brick fists
    for sx, n in ((1, "l"), (-1, "r")):
        cx = 42 * sx
        arm, fore = "arm_" + n, "fore_" + n
        sh.cube(arm, (cx - 12, 91.2, -13), (24, 2.8, 26), base=roof("tile", across=True), hide=("bottom",), faces={"top": roof("tile", moss="top", across=True)})
        sh.cube(arm, (cx - 8, 94, -9), (16, 3, 18), base=roof("tile", across=True), hide=("bottom",), faces={"top": roof("tile", moss="top", across=True)})
        sh.cube(arm, (cx - 4, 97, -5), (8, 3, 10), base=roof("tile", across=True), hide=("bottom",), faces={"top": roof("tile", across=True, ridge=True)})
        sh.cube(arm, (cx - 10, 63, -11), (20, 28.2, 22), base=timber(bay=20.0, rails=[], windows=[(10, 9, 5.5, 9.5, "lit")], shutters=True), faces={
            "north": timber(bay=20.0, rails=[14.0], windows=[(10, 8, 5.5, 9.5, "lit")], shutters=True), "south": timber(bay=20.0, rails=[], windows=[(10, 9, 5.5, 9.5, "dark")]),
            "west": stone(windows=[(11, 9, 5, 9, "lit")], moss="top"), "east": stone(windows=[(11, 9, 5, 9, "lit")], moss="top")}, hide=("bottom", "top"))
        sh.cube(arm, (cx - 11, 61.4, -12), (22, 2.4, 24), base=("iron", {}), hide=("top",))
        sh.cube(fore, (cx - 9, 53, -10), (18, 9.4, 20), base=stone(base=(150, 144, 134), blk=7.0, row=3.0, moss="top"), hide=("bottom", "top"))
        sh.cube(fore, (cx - 3 * sx - 2.5, 62.2, 3.6), (5, 8, 5), base=("chimney", {}), hide=("bottom",))
        sh.cube(fore, (cx - 9.5, 32, -10.5), (19, 21.2, 21), base=timber(bay=19.0, rails=[], windows=[(9.5, 10, 5, 8.5, "lit")]), faces={
            "north": timber(bay=19.0, rails=[], windows=[(9.5, 10, 5, 8.5, "dark")]), "west": timber(bay=21.0, rails=[], windows=[(10.5, 10, 5, 8.5, "lit")]),
            "east": timber(bay=21.0, rails=[], windows=[(10.5, 10, 5, 8.5, "lit")]), "south": timber(bay=19.0, rails=[], windows=[(9.5, 10, 5, 8.5, "lit")])}, hide=("bottom", "top"))
        sh.cube(fore, (cx - 11, 29.4, -12), (22, 3.4, 24), base=("iron", {}), hide=("top",))
        # the fist: one big block of brick, with knuckle blocks on its front
        sh.cube(fore, (cx - 15, 3.4, -16), (30, 27.6, 32), base=brick(bw=8.0, bh=4.0, soft=True, ao=0.26), faces={"top": brick(bw=8.0, bh=4.0, soft=True, ao=0.26, moss="top")}, hide=())
        for i in range(4):
            sh.cube(fore, (cx - 13.4 + i * 6.8, 17, -19), (6.2, 9.4, 3.2), base=brick(bw=5.0, bh=3.0, soft=True, ao=0.3), hide=("south",))
        sh.cube(fore, (cx - 13.5 - 2.6 * (1 if sx < 0 else 0) + (0 if sx > 0 else 0), 18, -4), (2.6, 9, 9), base=brick(bw=5.0, bh=3.0, soft=True), hide=())

    # ---------------------------------------------------------------------------------------------- animations
    a = Anim()
    # idle: a slow breath, the head turns a little, the arms hang heavy
    L = 4.0
    wave(a, "idle", "torso", L, 16, x=S(0.9, 0.0), z=S(0.5, 0.25))
    wave(a, "idle", "hips", L, 16, kind="pos", y=S(0.5, 0.0))
    wave(a, "idle", "head", L, 16, y=S(4.0, 0.1), x=S(0.8, 0.3))
    wave(a, "idle", "arm_l", L, 16, x=S(1.6, 0.35), z=S(-0.8, 0.0))
    wave(a, "idle", "arm_r", L, 16, x=S(1.6, 0.35 + 0.5), z=S(0.8, 0.0))
    wave(a, "idle", "horn_l", L, 16, z=S(0.8, 0.2))
    wave(a, "idle", "horn_r", L, 16, z=S(-0.8, 0.2))
    wave(a, "idle", "chimney", L, 16, kind="pos", y=S(0.15))
    wave(a, "idle", "treetop", L, 16, z=S(1.4, 0.0), x=S(0.9, 0.3))

    # walk: a heavy stride, the body drops on every footfall, the arms swing against the legs; a cycle of 1.0 s = two steps
    L = 1.0
    n = 20
    ph = lambda p: math.sin(2 * math.pi * p)
    lift = lambda p, s: max(0.0, s * math.cos(2 * math.pi * p))                        # the knee bends while the leg passes
    wave(a, "walk", "leg_l", L, n, x=lambda p: 30 * ph(p))
    wave(a, "walk", "leg_r", L, n, x=lambda p: -30 * ph(p))
    wave(a, "walk", "shin_l", L, n, x=lambda p: -34 * lift(p, 1.0) if True else 0)
    wave(a, "walk", "shin_r", L, n, x=lambda p: -34 * lift(p + 0.5, 1.0))
    wave(a, "walk", "hips", L, n, kind="pos", y=lambda p: -2.6 * abs(math.cos(2 * math.pi * p)) + 1.6)
    wave(a, "walk", "hips", L, n, z=lambda p: 2.0 * ph(p))
    wave(a, "walk", "torso", L, n, x=lambda p: -5 + 1.6 * math.cos(4 * math.pi * p), y=lambda p: -6 * ph(p), z=lambda p: 2.2 * math.cos(2 * math.pi * p))
    wave(a, "walk", "head", L, n, x=lambda p: 3 + 1.5 * math.cos(4 * math.pi * p), y=lambda p: 4 * ph(p))
    wave(a, "walk", "arm_l", L, n, x=lambda p: -26 * ph(p), z=lambda p: -4)
    wave(a, "walk", "arm_r", L, n, x=lambda p: 26 * ph(p), z=lambda p: 4)
    wave(a, "walk", "fore_l", L, n, x=lambda p: 10 + 14 * max(0, -ph(p)))
    wave(a, "walk", "fore_r", L, n, x=lambda p: 10 + 14 * max(0, ph(p)))
    wave(a, "walk", "horn_l", L, n, z=lambda p: 3 * math.cos(4 * math.pi * p))
    wave(a, "walk", "horn_r", L, n, z=lambda p: -3 * math.cos(4 * math.pi * p))
    wave(a, "walk", "treetop", L, n, z=lambda p: 5 * math.cos(4 * math.pi * p))

    # punch (the ram): wind up the right fist, lean back, then drive it down and forward; the blow lands at 0.60 s (tick 12 of the ram state), then recoil
    T = 1.4
    keys(a, "punch", "arm_r", "rot", {0.0: (0, 0, 0), 0.30: (-48, 0, -8), 0.52: (-70, 0, -10), 0.60: (55, 0, 0), 0.72: (52, 0, 0), 1.0: (24, 0, 0), 1.4: (0, 0, 0)}, loop=False)
    keys(a, "punch", "fore_r", "rot", {0.0: (0, 0, 0), 0.30: (-30, 0, 0), 0.52: (-36, 0, 0), 0.60: (14, 0, 0), 0.72: (16, 0, 0), 1.0: (10, 0, 0), 1.4: (0, 0, 0)}, loop=False)
    keys(a, "punch", "arm_l", "rot", {0.0: (0, 0, 0), 0.30: (14, 0, 6), 0.60: (-26, 0, 6), 1.0: (-6, 0, 0), 1.4: (0, 0, 0)}, loop=False)
    keys(a, "punch", "torso", "rot", {0.0: (0, 0, 0), 0.30: (8, 14, 0), 0.52: (10, 20, 0), 0.60: (-24, -12, 0), 0.72: (-22, -10, 0), 1.0: (-6, -3, 0), 1.4: (0, 0, 0)}, loop=False)
    keys(a, "punch", "hips", "rot", {0.0: (0, 0, 0), 0.52: (2, 6, 0), 0.60: (-4, -4, 0), 1.0: (-1, 0, 0), 1.4: (0, 0, 0)}, loop=False)
    keys(a, "punch", "hips", "pos", {0.0: (0, 0, 0), 0.52: (0, 1, 3), 0.60: (0, -6, -5), 0.72: (0, -5, -4), 1.0: (0, -1, -1), 1.4: (0, 0, 0)}, loop=False)
    keys(a, "punch", "leg_l", "rot", {0.0: (0, 0, 0), 0.52: (12, 0, 0), 0.60: (-22, 0, 0), 1.0: (-6, 0, 0), 1.4: (0, 0, 0)}, loop=False)
    keys(a, "punch", "leg_r", "rot", {0.0: (0, 0, 0), 0.52: (-16, 0, 0), 0.60: (18, 0, 0), 1.0: (6, 0, 0), 1.4: (0, 0, 0)}, loop=False)
    keys(a, "punch", "shin_l", "rot", {0.0: (0, 0, 0), 0.52: (-10, 0, 0), 0.60: (0, 0, 0), 1.4: (0, 0, 0)}, loop=False)
    keys(a, "punch", "head", "rot", {0.0: (0, 0, 0), 0.52: (6, -8, 0), 0.60: (-14, 6, 0), 1.0: (-4, 0, 0), 1.4: (0, 0, 0)}, loop=False)
    keys(a, "punch", "horn_l", "rot", {0.0: (0, 0, 0), 0.60: (0, 0, 4), 0.8: (0, 0, -3), 1.4: (0, 0, 0)}, loop=False)
    keys(a, "punch", "horn_r", "rot", {0.0: (0, 0, 0), 0.60: (0, 0, -4), 0.8: (0, 0, 3), 1.4: (0, 0, 0)}, loop=False)
    a.clips["punch"]["animation_length"] = T

    # stomp: the settle after the assembly and the cannons' kick: the left leg rises and slams, the whole body jolts; lands at 0.5 s
    keys(a, "stomp", "leg_l", "rot", {0.0: (0, 0, 0), 0.30: (-34, 0, 0), 0.46: (-30, 0, 0), 0.52: (4, 0, 0), 0.9: (0, 0, 0)}, loop=False)
    keys(a, "stomp", "shin_l", "rot", {0.0: (0, 0, 0), 0.30: (-46, 0, 0), 0.46: (-30, 0, 0), 0.52: (0, 0, 0), 0.9: (0, 0, 0)}, loop=False)
    keys(a, "stomp", "hips", "pos", {0.0: (0, 0, 0), 0.30: (0, 1, 0), 0.52: (0, -5, 0), 0.62: (0, 0.5, 0), 0.9: (0, 0, 0)}, loop=False)
    keys(a, "stomp", "torso", "rot", {0.0: (0, 0, 0), 0.30: (4, 0, 3), 0.52: (-7, 0, 0), 0.7: (0, 0, 0), 0.9: (0, 0, 0)}, loop=False)
    keys(a, "stomp", "arm_l", "rot", {0.0: (0, 0, 0), 0.30: (-10, 0, 12), 0.52: (8, 0, 3), 0.9: (0, 0, 0)}, loop=False)
    keys(a, "stomp", "arm_r", "rot", {0.0: (0, 0, 0), 0.30: (-10, 0, -12), 0.52: (8, 0, -3), 0.9: (0, 0, 0)}, loop=False)
    keys(a, "stomp", "head", "rot", {0.0: (0, 0, 0), 0.30: (6, 0, 0), 0.52: (-10, 0, 0), 0.9: (0, 0, 0)}, loop=False)
    keys(a, "stomp", "horn_l", "rot", {0.0: (0, 0, 0), 0.52: (0, 0, 5), 0.7: (0, 0, -3), 0.9: (0, 0, 0)}, loop=False)
    keys(a, "stomp", "horn_r", "rot", {0.0: (0, 0, 0), 0.52: (0, 0, -5), 0.7: (0, 0, 3), 0.9: (0, 0, 0)}, loop=False)
    a.clips["stomp"]["animation_length"] = 0.9

    # fire (the volley): the torso leans back, both arms spread and rise, the head roars up, then a recoil
    keys(a, "fire", "torso", "rot", {0.0: (0, 0, 0), 0.20: (12, 0, 0), 0.40: (8, 0, 0), 0.6: (0, 0, 0)}, loop=False)
    keys(a, "fire", "arm_l", "rot", {0.0: (0, 0, 0), 0.20: (-20, 0, 62), 0.40: (-10, 0, 50), 0.6: (0, 0, 0)}, loop=False)
    keys(a, "fire", "arm_r", "rot", {0.0: (0, 0, 0), 0.20: (-20, 0, -62), 0.40: (-10, 0, -50), 0.6: (0, 0, 0)}, loop=False)
    keys(a, "fire", "fore_l", "rot", {0.0: (0, 0, 0), 0.20: (-28, 0, 0), 0.6: (0, 0, 0)}, loop=False)
    keys(a, "fire", "fore_r", "rot", {0.0: (0, 0, 0), 0.20: (-28, 0, 0), 0.6: (0, 0, 0)}, loop=False)
    keys(a, "fire", "head", "rot", {0.0: (0, 0, 0), 0.20: (22, 0, 0), 0.40: (14, 0, 0), 0.6: (0, 0, 0)}, loop=False)
    keys(a, "fire", "chimney", "pos", {0.0: (0, 0, 0), 0.10: (0, 3, 0), 0.4: (0, 0, 0)}, loop=False)
    a.clips["fire"]["animation_length"] = 0.6

    # assemble (26 ticks = 1.3 s): the parts rise out of the ground one after another, legs, body, arms, head, with a small overshoot, and the whole thing settles
    D = 30
    def rise(bone, t0, t1, depth=-110):
        keys(a, "assemble", bone, "pos", {0.0: (0, depth, 0), t0: (0, depth, 0), t1: (0, 3, 0), t1 + 0.12: (0, 0, 0), 1.3: (0, 0, 0)}, loop=False)
    keys(a, "assemble", "leg_l", "pos", {0.0: (0, -60, 0), 0.30: (0, 2, 0), 0.42: (0, 0, 0), 1.3: (0, 0, 0)}, loop=False)
    keys(a, "assemble", "leg_r", "pos", {0.0: (0, -60, 0), 0.10: (0, -60, 0), 0.40: (0, 2, 0), 0.52: (0, 0, 0), 1.3: (0, 0, 0)}, loop=False)
    keys(a, "assemble", "torso", "pos", {0.0: (0, -95, 0), 0.30: (0, -95, 0), 0.75: (0, 4, 0), 0.9: (0, 0, 0), 1.3: (0, 0, 0)}, loop=False)
    keys(a, "assemble", "arm_l", "pos", {0.0: (0, -110, 0), 0.50: (0, -110, 0), 0.92: (0, 3, 0), 1.04: (0, 0, 0), 1.3: (0, 0, 0)}, loop=False)
    keys(a, "assemble", "arm_r", "pos", {0.0: (0, -110, 0), 0.58: (0, -110, 0), 1.0: (0, 3, 0), 1.12: (0, 0, 0), 1.3: (0, 0, 0)}, loop=False)
    keys(a, "assemble", "head", "pos", {0.0: (0, -130, 0), 0.7: (0, -130, 0), 1.12: (0, 4, 0), 1.24: (0, 0, 0), 1.3: (0, 0, 0)}, loop=False)
    keys(a, "assemble", "horn_l", "pos", {0.0: (0, -30, 0), 0.9: (0, -30, 0), 1.24: (0, 2, 0), 1.3: (0, 0, 0)}, loop=False)
    keys(a, "assemble", "horn_r", "pos", {0.0: (0, -30, 0), 0.94: (0, -30, 0), 1.26: (0, 2, 0), 1.3: (0, 0, 0)}, loop=False)
    keys(a, "assemble", "root", "rot", {0.0: (0, 0, 0), 1.3: (0, 0, 0)}, loop=False)
    keys(a, "assemble", "hips", "rot", {0.0: (0, 0, 0), 1.3: (0, 0, 0)}, loop=False)
    a.clips["assemble"]["animation_length"] = 1.3

    # collapse (24 ticks = 1.2 s): the head tips and drops, the arms fall away, the body sinks back into rubble
    keys(a, "collapse", "head", "rot", {0.0: (0, 0, 0), 0.5: (-16, 0, 10), 1.2: (-28, 0, 20)}, loop=False)
    keys(a, "collapse", "head", "pos", {0.0: (0, 0, 0), 0.5: (0, -2, 0), 1.2: (0, -70, 6)}, loop=False)
    keys(a, "collapse", "horn_l", "pos", {0.0: (0, 0, 0), 0.6: (0, -2, 0), 1.2: (0, -20, 0)}, loop=False)
    keys(a, "collapse", "horn_r", "pos", {0.0: (0, 0, 0), 0.6: (0, -2, 0), 1.2: (0, -20, 0)}, loop=False)
    keys(a, "collapse", "arm_l", "rot", {0.0: (0, 0, 0), 0.4: (6, 0, 18), 1.2: (18, 0, 40)}, loop=False)
    keys(a, "collapse", "arm_r", "rot", {0.0: (0, 0, 0), 0.4: (6, 0, -18), 1.2: (18, 0, -40)}, loop=False)
    keys(a, "collapse", "arm_l", "pos", {0.0: (0, 0, 0), 0.5: (0, -4, 0), 1.2: (0, -62, 0)}, loop=False)
    keys(a, "collapse", "arm_r", "pos", {0.0: (0, 0, 0), 0.5: (0, -4, 0), 1.2: (0, -62, 0)}, loop=False)
    keys(a, "collapse", "torso", "pos", {0.0: (0, 0, 0), 0.5: (0, -3, 0), 1.2: (0, -66, 0)}, loop=False)
    keys(a, "collapse", "torso", "rot", {0.0: (0, 0, 0), 0.6: (-6, 0, 3), 1.2: (-12, 0, 6)}, loop=False)
    keys(a, "collapse", "leg_l", "pos", {0.0: (0, 0, 0), 0.7: (0, -2, 0), 1.2: (0, -46, 0)}, loop=False)
    keys(a, "collapse", "leg_r", "pos", {0.0: (0, 0, 0), 0.7: (0, -2, 0), 1.2: (0, -46, 0)}, loop=False)
    keys(a, "collapse", "leg_l", "rot", {0.0: (0, 0, 0), 1.2: (-10, 0, 3)}, loop=False)
    keys(a, "collapse", "leg_r", "rot", {0.0: (0, 0, 0), 1.2: (10, 0, -3)}, loop=False)
    a.clips["collapse"]["animation_length"] = 1.2

    # ---- numbers the Java side copies: the fist at the end of the blow, the feet, the eyes, the chimney
    arm_a, fore_a, torso_a, hips_a = 55, 14, -24, -4
    fist_c = (4 + 25.6 / 2 + 0.0, -0.0)
    fy, fz = fk_arm((92, 0), (62, 0), (16.8, 0.0), (52, 0), (46, 0), arm_a, fore_a, torso_a, hips_a)
    fy -= 6.0                                                                          # the hips crouch by 6 px at the blow
    front_reach = fz - 15
    NUM.update({
        "height_px": 156.0,
        "punch_impact_s": 0.60, "punch_length_s": 1.4, "punch_fist_forward_px": round(-fz, 1), "punch_fist_height_px": round(fy, 1), "punch_fist_front_px": round(-front_reach, 1),
        "walk_cycle_s": 1.0, "foot_x_px": 14.0, "foot_front_px": -17.0, "foot_back_px": 13.0, "stomp_land_s": 0.52, "stomp_length_s": 0.9,
        "eye_y_px": 108.0, "eye_x_px": 5.4, "eye_front_px": -10.0, "chimney_px": [27.5, 119.0, -5.0], "chimney2_px": [-27, 119, 0], "assemble_s": 1.3, "collapse_s": 1.2, "fire_s": 0.6,
        "tower_tip_px": [11.2 + 36 * math.sin(math.radians(20)) * 0.0, 150.0],
    })
    sh.save(a)
    return sh


# ================================================================================================================================ the house (a recombined room)
def house():
    sh = Sheet(KEY, "house", tex_w=1536, K=3.2, seed=11)
    sh.bone("root", pivot=(0, 0, 0))
    sh.bone("house", "root", (0, 0, 0))
    sh.bone("smoke", "house", (6, 36, 0))
    sh.bone("tree", "house", (-7, 30, 0))
    sh.cube("house", (-15, 0, -13), (30, 4, 26), base=stone(base=(150, 144, 134), blk=8.0, row=2.4, moss="bottom"), faces={"top": stone()}, hide=("bottom",))
    front = timber(bay=26.0, rails=[], windows=[(7, 9, 5.5, 8.5, "lit"), (19, 9, 5.5, 8.5, "lit")], door=(13, 7.0, 11.0), shutters=True, moss="top")
    side = timber(bay=22.0, rails=[], windows=[(11, 9, 5.5, 8.5, "lit")], moss="top")
    sh.cube("house", (-13, 4, -11), (26, 17, 22), base=front, faces={"north": front, "south": timber(bay=26.0, rails=[], windows=[(7, 9, 5.5, 8.5, "dark"), (19, 9, 5.5, 8.5, "lit")], moss="top"),
                                                                    "west": side, "east": side}, hide=("bottom", "top"))
    sh.cube("house", (-14.4, 20.6, -12.4), (28.8, 1.6, 24.8), base=("iron", {}), hide=("bottom",))
    gable_roof(sh, "house", 0, 22, 13, 22, 11, tone="tile", moss="top", wall=timber(bay=26.0, rails=[], windows=[(13, 2.0, 4.2, 3.2, "lit")]))
    sh.cube("smoke", (4.5, 26, -6), (5.5, 13, 5.5), base=("chimney", {}), faces={"top": stone(base=(80, 72, 70))}, hide=("bottom",))
    sh.cube("smoke", (3.8, 38.4, -6.7), (6.9, 1.8, 6.9), base=stone(base=(176, 170, 158), blk=5.0, row=2.0), hide=("bottom",))
    sh.cube("tree", (-8.6, 28, -1.4), (2.8, 7, 2.8), base=WOOD, hide=("bottom",), faces={"top": WOOD})
    sh.cube("tree", (-13, 33, -5), (11, 5, 10), base=LEAVES, hide=("bottom",))
    sh.cube("tree", (-11.4, 37.5, -3.6), (8, 4.5, 7.2), base=LEAVES, hide=("bottom",))
    a = Anim()
    keys(a, "rise", "house", "scale", {0.0: (1, 0.05, 1), 0.3: (1.06, 1.12, 1.06), 0.5: (1, 1, 1)}, loop=False)
    keys(a, "rise", "house", "pos", {0.0: (0, -8, 0), 0.3: (0, 0, 0), 0.5: (0, 0, 0)}, loop=False)
    a.clips["rise"]["animation_length"] = 0.5
    wave(a, "idle", "house", 3.0, 12, kind="pos", y=S(0.3))
    wave(a, "idle", "tree", 3.0, 12, z=S(2.0, 0.1))
    keys(a, "swap", "house", "scale", {0.0: (1, 1, 1), 0.2: (0.6, 1.3, 0.6), 0.4: (1.2, 0.8, 1.2), 0.6: (1, 1, 1)}, loop=False)
    a.clips["swap"]["animation_length"] = 0.6
    NUM["house_chimney_px"] = [7.2, 40.0, -3.5]
    sh.save(a)
    return sh


# ================================================================================================================================ the ring of building pieces
def ring():
    sh = Sheet(KEY, "ring", tex_w=1024, K=3.0, seed=13)
    sh.bone("root", pivot=(0, 36, 0))
    sh.bone("spin", "root", (0, 36, 0))
    R = 17.0
    pieces = [
        ("brick", (7, 5, 4), brick(bw=4.0, bh=2.5, soft=True, ao=0.3)),
        ("beam", (14, 2.4, 2.4), ("wood", {"bw": 2.4})),
        ("window", (7, 9, 2), timber(bay=7.0, rails=[], windows=[(3.5, 4.5, 4, 6.5, "lit")], braces=False)),
        ("shingles", (10, 1.4, 6), roof("tile", across=True)),
        ("chimney", (3.4, 8, 3.4), ("chimney", {})),
        ("door", (5, 10, 1.6), ("wood", {"bw": 2.5})),
        ("stone", (6, 6, 6), stone(base=(158, 152, 142), blk=5.0, row=3.0, moss="top")),
        ("tower", (4.4, 11, 4.4), ("tower", {"slits": [(2.2, 5, 0.9, 3)]})),
    ]
    for k, (name, size, face) in enumerate(pieces):
        a = 2 * math.pi * k / len(pieces)
        x, z = R * math.cos(a), R * math.sin(a)
        y = 34 + (k % 3) * 2.0
        size = tuple(v * 1.35 for v in size)
        sh.cube("spin", (x - size[0] / 2, y, z - size[2] / 2), size, base=face, rot=(0, -math.degrees(a) + 90, 0), pv=(x, y + size[1] / 2, z), hide=("bottom",))
    sh.cube("spin", (-1.4, 33, -1.4), (2.8, 2.8, 2.8), base=("lamp", {}), hide=())
    a = Anim()
    wave(a, "idle", "spin", 8.0, 24, y=lambda p: 360 * p)
    wave(a, "idle", "root", 3.0, 12, kind="pos", y=S(0.6))
    sh.save(a)
    return sh


# ================================================================================================================================ the drill
def drill():
    sh = Sheet(KEY, "drill", tex_w=1024, K=3.0, seed=17)
    sh.bone("root", pivot=(0, 0, 0))
    sh.bone("spin", "root", (0, 12, 0))
    sh.cube("root", (-3.4, 0, -3.4), (6.8, 4, 6.8), base=stone(base=(150, 144, 134), blk=4.0, row=2.0), hide=())
    sh.cube("root", (-4.2, 4, -4.2), (8.4, 1.6, 8.4), base=("iron", {}), hide=())
    widths = (7.0, 6.2, 5.4, 4.6, 3.8, 3.0, 2.2, 1.4)
    y = 5.6
    for i, w in enumerate(widths):
        h = 2.9
        face = ("brick", {"bw": 3.0, "bh": 1.8, "soft": True}) if i % 2 == 0 else ("slate", {"tone": "tile", "row": 2.0, "sw": 2.0, "across": False})
        sh.cube("spin", (-w / 2, y, -w / 2), (w, h, w), base=face, rot=(0, 26 * i, 0), pv=(0, y + h / 2, 0), hide=("bottom",) if i else ())
        if i < len(widths) - 1:
            sh.cube("spin", (-w / 2 - 0.4, y + h - 0.1, -w / 2 - 0.4), (w + 0.8, 0.5, w + 0.8), base=("lamp", {}), rot=(0, 26 * i, 0), pv=(0, y + h, 0), hide=())
        y += h + 0.3
    sh.cube("spin", (-0.4, y, -0.4), (0.8, 3.2, 0.8), base=("lamp", {}), hide=("bottom",))
    a = Anim()
    wave(a, "idle", "spin", 0.5, 10, y=lambda p: 360 * p)
    sh.save(a)
    return sh


ALL = {"golem": golem, "house": house, "ring": ring, "drill": drill}

if __name__ == "__main__":
    which = sys.argv[1:] or list(ALL)
    for k in which:
        ALL[k]()
    if "golem" in which or not sys.argv[1:]:
        with open(os.path.join(HERE, "golem_numbers.json"), "w") as f:
            json.dump(NUM, f, indent=1, sort_keys=True)
        print(json.dumps(NUM, sort_keys=True))
