"""tdparts: the part recipes of the Tiny Djinn generator. Every recipe is a function f(c) that adds cubes to a tdlib.Ctx; the spec table (specs.json) names which
recipe a Djinn uses for its hair, headgear, ears, cape, weapon, tail and accessory. Coordinates are the file frame of tdlib (y up, front -z, +x the model's left,
origin at the waist): the head box is x -5..5, y 6..15, z -4.5..4.5; the right hand (the weapon hand) is at (-4, 0.5, 0).
"""
import math

from tdlib import SW
import tdpaint as tp

G = (-4.0, 0.5, 0.0)                                    # the grip
SIDES = (1, -1)


def sb(prefix, s):
    """The bone of a side: ear_l / ear_r, horn_l / horn_r (the model's left is +x)."""
    return prefix + ("_l" if s > 0 else "_r")


def radial_spike(c, bone, centre, r, a, h, w, tilt, sw, wz=None):
    """A spike standing on a circle of radius r (angle a, in the xz plane) leaning outwards by `tilt` degrees: a crown tooth, a ruff feather."""
    cx, cy, cz = centre
    x, z = cx + math.cos(a) * r, cz + math.sin(a) * r
    # lean outward: towards +x by cos(a), towards +z (back) by sin(a) -> rz = -tilt*cos(a), rx = +tilt*sin(a)
    c.spike(bone, (x, cy, z), h, w, rot=(tilt * math.sin(a), 0, -tilt * math.cos(a)), sw=sw, wz=wz)


# ================================================================ the body every Djinn has
def body(c):
    s = c.spec
    c.pbox("head", (-5, 6, -4.5), (10, 9, 9), tp.head_painter(s))
    c.pbox("body", (-3, 0, -2), (6, 6, 4), tp.torso_painter(s))
    sizes = ((5, 4, 4), (4, 4, 3), (3, 4, 3), (2, 3, 2))
    origins = ((-2.5, -4, -2), (-2, -8, -1.5), (-1.5, -12, -1.5), (-1, -15, -1))
    for k in range(4):
        c.pbox("tail%d" % (k + 1), origins[k], sizes[k], tp.tail_painter(s, k))
    uv = c.pbox("arm_r", (-5, 1.5, -1), (2, 4, 2), tp.arm_painter(s))
    c.pbox("arm_l", (3, 1.5, -1), (2, 4, 2), None, mirror=True, uv_from=uv)
    uv = c.pbox("hand_r", (-5, -0.5, -1), (2, 2, 2), tp.hand_painter(s))
    c.pbox("hand_l", (3, -0.5, -1), (2, 2, 2), None, mirror=True, uv_from=uv)
    if s.get("eyes", "round") not in ("hooded", "visor"):
        for sx in SIDES:
            x0 = -4 if sx > 0 else 1                                    # the lids over the eyes: shut for a blink (scale y 0.02 at rest)
            c.cube("lids", (x0, 9, -4.64), (3, 3, 0.14), "skin")
    # the kit every Djinn wears: shoulder pieces, a pendant, ear studs, rune marks
    sh = s.get("pauldron", "round")
    if sh != "none":
        for sx in SIDES:
            b = sb("arm", sx)
            big = 1.35 if sh == "big" else 1.0
            c.cube(b, (sx * 4 - 1.6 * big, 4.9, -1.6 * big), (3.2 * big, 1.6, 3.2 * big), "trim" if s.get("pauldron_sw", "trim") == "trim" else s["pauldron_sw"])
            if sh == "spiked":
                c.spike(b, (sx * 4, 6.4, 0), 2.6, 1.2, rot=(0, 0, -sx * 20), sw="metal")
    c.cube("body", (-0.8, 2.6, -2.35), (1.6, 1.6, 0.5), "gem")           # the pendant / chest gem
    c.cube("head", (-3.7, 5.6, -2.8), (7.4, 1.0, 5.6), "cloth2")        # the collar between head and body
    for sx in SIDES:
        c.cube(sb("ear", sx), (sx * 5.0 - 0.4 + (0.3 if sx > 0 else -0.3), 9.6, -0.4), (0.8, 0.8, 0.8), "trim")      # an ear stud
    for sx in SIDES:
        c.cube(sb("arm", sx), (sx * 4 - 0.3 + (0.4 if sx > 0 else -0.4) * 0, 2.4, -1.18), (0.6, 0.6, 0.2), "glow")      # a rune on each forearm
    c.cube("body", (-3.2, -0.3, -2.2), (6.4, 1.0, 4.4), "trim", infl=0.0)  # the belt ring
    c.cube("body", (-0.9, -0.1, -2.5), (1.8, 1.2, 0.5), "gem")             # its buckle
    # dark-only: the thorn crown and the eye crest (the strips are transparent in the light texture)
    for k in range(9):
        a = 2 * math.pi * k / 9
        radial_spike(c, "dark", (0, 15.4, 0), 4.6, a, 3.6 + 1.2 * (k % 2), 1.0, 22, "dkthorn")
    c.cube("dark", (-1.8, 13.1, -4.95), (3.6, 1.3, 0.45), "dkthorn")
    c.cube("dark", (-0.5, 13.3, -5.1), (1.0, 0.9, 0.3), "dkeye")


# ================================================================ hair
def hair_none(c):
    pass


def hair_spiky(c, sw="hair", tip="hair2"):
    for k, (x, z, h, rx) in enumerate(((-3, -1, 4.5, 28), (-1.5, 0.5, 5.5, 34), (0, 1.5, 6.5, 40), (1.5, 0.5, 5.5, 34), (3, -1, 4.5, 28))):
        c.spike("hair", (x, 15, z), h, 2.4, rot=(rx, 0, -x * 4), sw=sw if k % 2 == 0 else tip)
    c.spike("hair", (-4.2, 12.5, 2.5), 4.5, 2.0, rot=(32, 0, 14), sw=sw)
    c.spike("hair", (4.2, 12.5, 2.5), 4.5, 2.0, rot=(32, 0, -14), sw=sw)


def hair_flame(c):
    for k, (x, z, h, rx) in enumerate(((-3.4, -1, 5, 22), (-1.7, 0, 6.5, 30), (0, 1, 8, 38), (1.7, 0, 6.5, 30), (3.4, -1, 5, 22), (-2.5, 2.5, 6, 46), (2.5, 2.5, 6, 46))):
        c.spike("hair", (x, 15, z), h, 2.3, rot=(rx, 0, -x * 5), sw="fire")


def hair_mane(c, sw="hair", tip="hair2", big=1.0):
    for sx in SIDES:                                       # locks down the sides of the face, a wild crest and a long back
        for k, y in enumerate((13.2, 10.8, 8.4, 6.6)):
            c.cube("hair", (sx * 5.0 - 0.9 + (0.5 if sx > 0 else -0.1), y - 1.2, -1.2 - k * 0.1), (1.6 * big, 2.8, 3.2 + k * 0.4), sw if k % 2 == 0 else tip,
                   rot=(0, 0, sx * (8 + 4 * k)), pivot=(sx * 5, y + 1, 0))
    for k, x in enumerate((-4, -2, 0, 2, 4)):
        c.spike("hair", (x, 15, -1.2 + 0.6 * abs(x) / 4), 3.2 + (2.2 if k % 2 == 0 else 0) * big, 2.4, rot=(10, 0, -x * 6), sw=tip if k % 2 else sw)
    c.cube("hair", (-4.3, 1.5, 4.5), (8.6, 10.5, 2.2 * big), sw)
    c.cube("hair", (-3.4, 0.5, 6.7), (6.8, 6, 1.2), tip)


def hair_long(c, sw="hair", tip="hair2"):
    c.cube("hair", (-4.6, 2.0, 4.6), (9.2, 11, 2.0), sw)
    c.cube("hair", (-3.6, 0.0, 6.5), (7.2, 6, 1.4), tip)
    for sx in SIDES:
        c.cube("hair", (sx * 5.3 - 0.8, 5.5, -2.5), (1.6, 8, 1.8), sw if sx > 0 else tip, rot=(0, 0, sx * 4), pivot=(sx * 5.3, 13, -2))


def hair_bun(c):
    hair_long(c)
    for sx in SIDES:
        c.cube("hair", (sx * 4.2 - 1.7, 14.4, -1.7), (3.4, 3.2, 3.4), "hair2")
        c.cube("hair", (sx * 4.2 - 0.5, 17.2, -0.5), (1.0, 1.4, 1.0), "gold")


def hair_dreads(c):
    for k in range(8):
        a = math.pi * 0.35 + math.pi * 1.3 * k / 7
        x, z = math.cos(a) * 4.7, math.sin(a) * 4.0
        c.cube("hair", (x - 0.55, 2.0 + (k % 3), z - 0.55), (1.1, 8.5 - (k % 3) * 0.6, 1.1), "hair" if k % 2 else "hair2")
        c.cube("hair", (x - 0.7, 2.0 + (k % 3) - 0.8, z - 0.7), (1.4, 1.1, 1.4), "bone")
    c.cube("hair", (-4.2, 13.6, -3.6), (8.4, 2.0, 7.6), "hair")


def hair_braids(c):
    hair_spiky(c)
    for sx in SIDES:
        for k in range(4):
            c.cube("hair", (sx * 5.4 - 0.7, 8.5 - k * 2.0, -2.4), (1.4, 1.8, 1.4), "hair" if k % 2 else "hair2")
        c.cube("hair", (sx * 5.4 - 0.6, 0.2, -2.3), (1.2, 1.2, 1.2), "gold")


def hair_topknot(c):
    c.cube("hair", (-1.6, 15.0, -0.2), (3.2, 3.2, 3.2), "hair")
    c.cube("hair", (-0.5, 18.0, 0.9), (1.0, 1.2, 1.0), "trim")
    c.cube("hair", (-0.9, 3.0, 4.6), (1.8, 10, 1.6), "hair2", rot=(8, 0, 0), pivot=(0, 13, 4.6))


def hair_moss(c):
    for k, (x, z, w) in enumerate(((-3, -1.5, 3.2), (0.4, -2.4, 3.6), (3, -1, 3.0), (-2, 1.8, 3.4), (2.2, 2.4, 3.2), (0, 0.2, 3.0))):
        c.cube("hair", (x - w / 2, 15.0, z - w / 2), (w, 1.6 + (k % 2) * 0.8, w), "hair" if k % 2 else "leaf")
    c.cube("hair", (-4.4, 5.0, 4.4), (8.8, 8, 1.6), "hair")
    for sx in SIDES:
        c.cube("hair", (sx * 5.2 - 0.7, 8.0, 0.0), (1.4, 5, 3), "leaf")


def hair_white_mane(c):
    hair_mane(c, "white", "silver", 1.25)


def hair_short(c):
    c.cube("hair", (-4.8, 14.6, -4.0), (9.6, 1.6, 8.6), "hair")
    c.cube("hair", (-4.8, 10.0, 4.0), (9.6, 5.0, 1.0), "hair2")


HAIR = {"none": hair_none, "spiky": hair_spiky, "flame": hair_flame, "mane": hair_mane, "long": hair_long, "bun": hair_bun, "dreads": hair_dreads, "braids": hair_braids,
        "topknot": hair_topknot, "moss": hair_moss, "white_mane": hair_white_mane, "short": hair_short}


# ================================================================ ears
def ears_none(c):
    pass


def ears_cat(c):
    for sx in SIDES:
        b = sb("ear", sx)
        c.spike(b, (sx * 3.4, 15, -0.3), 3.4, 2.8, rot=(0, 0, -sx * 12), sw="hair", wz=1.4)
        c.spike(b, (sx * 3.4, 15, -0.7), 2.2, 1.5, rot=(0, 0, -sx * 12), sw="coral", wz=0.5)


def ears_long(c):
    for sx in SIDES:
        c.spike(sb("ear", sx), (sx * 5, 11.6, 0), 4.2, 1.6, rot=(0, 0, -sx * 78), sw="skin", wz=1.0)


def ears_round(c):
    for sx in SIDES:
        c.cube(sb("ear", sx), (sx * 5.2 - 0.6, 12.8, -1.2), (1.4, 2.6, 2.4), "hair")
        c.cube(sb("ear", sx), (sx * 5.7 - 0.2, 13.3, -0.7), (0.5, 1.5, 1.4), "skin2")


def ears_wolf(c):
    for sx in SIDES:
        c.spike(sb("ear", sx), (sx * 3.6, 14.8, 0.8), 4.4, 2.8, rot=(0, 0, -sx * 14), sw="hair", wz=1.6)
        c.spike(sb("ear", sx), (sx * 3.6, 14.8, 0.2), 3.0, 1.4, rot=(0, 0, -sx * 14), sw="skin2", wz=0.5)


def ears_fin(c):
    for sx in SIDES:
        c.spike(sb("ear", sx), (sx * 5.0, 10.6, 0), 4.6, 3.4, rot=(0, 0, -sx * 70), sw="water", wz=0.7)
        c.spike(sb("ear", sx), (sx * 5.0, 12.4, 0), 3.4, 2.4, rot=(0, 0, -sx * 40), sw="water", wz=0.6)


def ears_leaf(c):
    for sx in SIDES:
        c.spike(sb("ear", sx), (sx * 5.0, 11.4, 0), 4.2, 2.2, rot=(0, 0, -sx * 60), sw="leaf", wz=0.6)


def ears_donkey(c):
    for sx in SIDES:
        c.spike(sb("ear", sx), (sx * 3.2, 15, 0), 6.4, 2.0, rot=(0, 0, -sx * 24), sw="hair", wz=1.0)
        c.spike(sb("ear", sx), (sx * 3.2, 15, -0.3), 4.8, 1.0, rot=(0, 0, -sx * 24), sw="skin2", wz=0.4)


def ears_horse(c):
    for sx in SIDES:
        c.spike(sb("ear", sx), (sx * 3.0, 15, 0.6), 5.4, 2.0, rot=(-8, 0, -sx * 10), sw="hair", wz=1.4)


def ears_deer(c):
    for sx in SIDES:
        c.spike(sb("ear", sx), (sx * 5, 12.4, 0), 4.0, 2.2, rot=(0, 0, -sx * 58), sw="hair", wz=0.9)
        c.spike(sb("ear", sx), (sx * 5, 12.4, -0.2), 3.0, 1.0, rot=(0, 0, -sx * 58), sw="skin2", wz=0.4)


def ears_cup(c):                                                     # Furfur's speaker cups
    for sx in SIDES:
        c.cube(sb("ear", sx), (sx * 5.0 - (0.0 if sx > 0 else 2.0), 9.5, -2.2), (2.0, 4.4, 4.4), "metal")
        c.cube(sb("ear", sx), (sx * 5.0 + (1.6 if sx > 0 else -2.4), 10.6, -1.1), (0.8, 2.2, 2.2), "glow")


EARS = {"none": ears_none, "cat": ears_cat, "long": ears_long, "round": ears_round, "wolf": ears_wolf, "fin": ears_fin, "leaf": ears_leaf, "donkey": ears_donkey,
        "horse": ears_horse, "deer": ears_deer, "cup": ears_cup}


# ================================================================ horns
def horns_crescent(c, sw="bone", n=5, seg=2.4, p0=24, turn=-78):
    for sx in SIDES:
        c.arc(sb("horn", sx), (sx * 3.4, 14.2, 0), sx * p0, sx * turn, n, seg, 1.9, 0.7, sw)


def horns_ram(c, sw="bone"):
    for sx in SIDES:
        c.arc(sb("horn", sx), (sx * 4.2, 12.8, 1.2), sx * 72, sx * 205, 6, 2.2, 2.4, 0.9, sw)


def horns_bull(c):
    for sx in SIDES:
        c.arc(sb("horn", sx), (sx * 4.6, 13.0, 0), sx * 84, sx * -88, 4, 2.6, 2.4, 1.1, "bone")
        c.spike(sb("horn", sx), (sx * 8.6, 15.4, 0), 1.6, 1.4, rot=(0, 0, 0), sw="gold")      # the bronze cap


def horns_stag(c, sw="bone"):
    for sx in SIDES:
        b = sb("horn", sx)
        end = c.arc(b, (sx * 2.6, 15, 0), sx * 20, sx * 24, 4, 3.0, 1.5, 0.8, sw)
        for k, (dx, dy, tilt) in enumerate(((0.6, 3.0, 42), (1.6, 6.2, 38), (2.4, 9.0, 34))):
            c.spike(b, (sx * (2.6 + dx * 1.2), 15 + dy, 0), 3.2, 1.0, rot=(0, 0, -sx * (tilt + 20)), sw=sw)


def horns_spike(c, sw="metal"):
    for sx in SIDES:
        c.spike(sb("horn", sx), (sx * 3.4, 14.8, 0), 5.4, 2.0, rot=(0, 0, -sx * 28), sw=sw)


def horns_swept(c, sw="bone"):                                       # swept back along the head
    for sx in SIDES:
        end = c.arc(sb("horn", sx), (sx * 3.6, 14.6, 1.0), 20, 44, 4, 2.6, 2.0, 0.8, sw, plane="yz")


def horns_lyre(c, sw="gold"):
    for sx in SIDES:
        c.arc(sb("horn", sx), (sx * 3.6, 14.4, 0), sx * 40, sx * -100, 6, 2.5, 1.7, 0.7, sw)


def horns_spiral(c):
    # the unicorn horn: a twisted taper on the brow, leaning slightly forward
    for k in range(6):
        w = 2.6 - k * 0.38
        c.cube("brow", (-w / 2, 13.2 + k * 1.9, -4.9 - k * 0.45), (w, 2.0, w), "white" if k % 2 == 0 else "gem", rot=(0, 28 * k, 0),
               pivot=(0, 13.2 + k * 1.9 + 1, -4.9 - k * 0.45))


# ================================================================ headgear
def head_bolt_crown(c):
    for k, (x, h) in enumerate(((-3, 5.0), (0, 7.2), (3, 5.0))):
        y = 15.0
        for q in range(3):
            c.cube("crown", (x - 0.7 + (0.9 if q % 2 else -0.5), y, -0.6), (1.4, h / 3.0 + 0.4, 1.2), "bolt", rot=(0, 0, 18 if q % 2 else -18), pivot=(x, y, 0))
            y += h / 3.0 - 0.2
    c.cube("crown", (-4.6, 14.4, -4.2), (9.2, 1.0, 8.4), "metal")


def head_flame_crown(c):
    c.cube("crown", (-5.2, 14.5, -4.7), (10.4, 1.2, 9.4), "gold")
    for k in range(6):
        a = 2 * math.pi * k / 6 + 0.3
        radial_spike(c, "crown", (0, 15.5, 0), 3.8, a, 4.2 + (k % 2) * 2.4, 2.0, 10, "fire")


def head_fin_tiara(c):
    c.cube("crown", (-5.1, 14.2, -4.6), (10.2, 1.0, 9.2), "silver")
    for x, h in ((-3.2, 3.6), (0, 6.2), (3.2, 3.6)):
        c.spike("crown", (x, 15.0, -2.6), h, 3.0, rot=(-8, 0, 0), sw="water", wz=0.6)
    for x in (-1.6, 1.6):
        c.cube("crown", (x - 0.6, 14.8, -4.9), (1.2, 1.2, 1.0), "coral")


def head_wind_plume(c):
    c.arc("crown", (0, 15.0, 0.6), 18, 80, 5, 3.0, 2.8, 0.9, "wind", plane="yz", wz=0.9)
    for sx in SIDES:
        c.spike(sb("ear", sx), (sx * 5.2, 12.2, 0.5), 4.2, 2.6, rot=(0, 0, -sx * 52), sw="white", wz=0.6)
        c.spike(sb("ear", sx), (sx * 5.2, 11.0, 0.5), 3.2, 2.2, rot=(0, 0, -sx * 82), sw="wind", wz=0.6)


def head_antlers_leaves(c):
    horns_stag(c, "wood")
    for sx in SIDES:
        for k, (x, y) in enumerate(((3.6, 17.5), (5.0, 20.6), (6.6, 22.8))):
            c.cube(sb("horn", sx), (sx * x - 0.9, y - 0.5, -0.9), (1.8, 1.0, 1.8), "leaf", rot=(0, 25 * k, 20), pivot=(sx * x, y, 0))
    for k in range(3):
        c.cube("crown", (-3.2 + k * 3.2 - 0.9, 14.8, -2.0), (1.8, 1.6, 1.8), "coral" if k == 1 else "leaf")
    c.cube("crown", (-0.5, 16.2, -1.6), (1.0, 1.0, 1.0), "gold")


def head_great_helm(c):
    c.cube("crown", (-5.4, 9.6, -4.9), (10.8, 6.0, 9.8), "iron")
    c.cube("crown", (-4.6, 10.8, -5.15), (9.2, 1.5, 0.4), "black")
    c.cube("crown", (-3.6, 10.9, -5.25), (7.2, 0.8, 0.3), "glow")
    c.cube("crown", (-0.7, 15.4, -4.2), (1.4, 2.4, 8.4), "metal")
    c.cube("crown", (-5.5, 9.4, -5.0), (11.0, 0.8, 10.0), "trim")


def head_bell_crown(c):
    c.cube("crown", (-5.0, 14.4, -4.5), (10.0, 1.0, 9.0), "gold")
    for k in range(6):
        a = 2 * math.pi * k / 6
        x, z = math.cos(a) * 4.0, math.sin(a) * 3.6
        c.cube("crown", (x - 0.9, 15.4, z - 0.9), (1.8, 2.0, 1.8), "gold")
        c.cube("crown", (x - 0.4, 15.0, z - 0.4), (0.8, 0.6, 0.8), "metal")


def head_wolf_cowl(c):
    c.cube("hair", (-5.5, 14.2, -5.0), (11.0, 2.2, 10.0), "hair")
    c.cube("hair", (-5.4, 5.6, -5.0), (10.8, 1.8, 10.0), "hair2")
    for sx in SIDES:
        c.cube("hair", (sx * 5.5 - 0.6 + (0.0 if sx > 0 else -0.5), 7.0, -4.4), (1.6, 7.4, 8.8), "hair")
    c.cube("hair", (-3.2, 15.6, -3.8), (6.4, 1.2, 3.0), "hair2")


def head_hood(c):
    c.spike("crown", (0, 14.6, 3.6), 4.6, 4.2, rot=(62, 0, 0), sw="cloth", wz=3.0)
    c.cube("hair", (-5.5, 6.2, -4.9), (11, 1.4, 1.2), "cloth2")


def head_visor(c, sw="metal"):
    c.cube("brow", (-5.4, 8.8, -5.0), (10.8, 3.4, 1.0), sw)
    c.cube("brow", (-4.6, 9.8, -5.15), (9.2, 1.0, 0.3), "glow")
    for sx in SIDES:
        c.spike("brow", (sx * 5.4, 9.6, -1.2), 4.8, 1.6, rot=(0, 0, -sx * 26), sw=sw, wz=3.0)
    c.cube("brow", (-5.0, 14.6, -4.6), (10.0, 1.0, 9.2), sw)


def head_storm_visor(c):
    head_visor(c, "iron")
    horns_stag(c, "iron")
    for sx in SIDES:
        c.cube(sb("horn", sx), (sx * 4.2 - 0.3, 17.0, -0.3), (0.6, 2.4, 0.6), "bolt")


def head_crescent_circlet(c):
    c.cube("crown", (-5.1, 13.7, -4.6), (10.2, 1.0, 9.2), "silver")
    r, cx, cy = 2.4, 0.0, 15.6
    for k in range(7):
        a = math.radians(-100 + k * 33.0)
        x, y = cx + math.sin(a) * r, cy + math.cos(a) * r * 1.0
        c.cube("brow", (x - 0.6, y - 0.6, -5.2), (1.2, 1.2, 0.8), "glow" if k in (0, 6) else "silver")


def head_bone_mask(c):
    c.cube("brow", (-4.8, 8.2, -5.1), (9.6, 6.4, 0.9), "bone")
    for sx in SIDES:
        c.cube("brow", (sx * 2.4 - 1.4, 9.4, -5.35), (2.8, 2.8, 0.5), "black")
        c.cube("brow", (sx * 2.4 - 0.6, 10.0, -5.45), (1.2, 1.2, 0.4), "eye")
        c.cube("brow", (sx * 3.9 - 0.4, 8.0, -5.3), (0.8, 2.0, 0.6), "bone")
    c.cube("brow", (-0.7, 8.6, -5.4), (1.4, 1.8, 0.5), "black")
    for k in range(5):
        c.cube("brow", (-2.5 + k * 1.0 - 0.0, 7.2, -5.3), (0.8, 1.2, 0.5), "white")


def head_ice_crown(c):
    c.cube("crown", (-4.8, 14.6, -4.3), (9.6, 0.9, 8.6), "silver")
    for k in range(7):
        a = 2 * math.pi * k / 7
        radial_spike(c, "crown", (0, 15.0, 0), 3.9, a, 3.4 + 2.8 * ((k * 3) % 4) / 3.0, 1.6, 12, "ice")


def head_scholar_cap(c):
    c.cube("crown", (-5.6, 15.0, -5.4), (11.2, 1.0, 10.8), "cloth2")
    c.cube("crown", (-4.8, 14.2, -4.6), (9.6, 0.9, 9.2), "cloth")
    c.cube("crown", (4.6, 12.4, -4.9), (1.0, 3.2, 1.0), "trim")
    c.cube("crown", (4.4, 11.4, -5.1), (1.4, 1.2, 1.4), "gold")
    for sx in SIDES:
        c.cube(sb("ear", sx), (sx * 5.25 - 0.4, 9.8, -1.6), (0.8, 3.0, 3.2), "bone")
        c.cube(sb("ear", sx), (sx * 5.7 - 0.2, 11.2, -0.8), (0.4, 0.8, 0.8), "glow")
        c.cube(sb("ear", sx), (sx * 5.7 - 0.2, 11.2, 0.4), (0.4, 0.8, 0.8), "glow")


def head_skull_mask(c):
    head_bone_mask(c)
    for sx in SIDES:
        c.spike(sb("horn", sx), (sx * 5.5, 12.4, 0), 8.4, 0.9, rot=(0, 0, -sx * 14), sw="wood")
        c.cube(sb("horn", sx), (sx * 6.9 - 0.9, 20.4, -0.9), (1.8, 1.8, 1.8), "bone")


def head_beak_hood(c):
    c.spike("crown", (0, 14.6, 3.6), 4.0, 4.0, rot=(60, 0, 0), sw="cloth", wz=2.8)
    c.cube("brow", (-1.2, 9.6, -6.6), (2.4, 1.5, 2.2), "trim", rot=(-14, 0, 0), pivot=(0, 11.1, -4.8))
    c.cube("brow", (-1.0, 8.5, -6.0), (2.0, 1.1, 1.8), "gold", rot=(10, 0, 0), pivot=(0, 9.6, -4.8))
    for k in range(9):
        a = math.pi * (0.1 + 1.8 * k / 8)
        radial_spike(c, "hair", (0, 6.4, 0), 4.2, a, 2.8, 1.4, -22, "black", wz=1.0)


def head_carapace_cap(c):
    c.cube("crown", (-5.4, 14.2, -4.8), (10.8, 2.4, 9.6), "trim")
    c.cube("crown", (-3.6, 16.2, -3.2), (7.2, 0.8, 6.4), "gold")
    for sx in SIDES:
        c.spike("crown", (sx * 2.4, 16.6, -1.0), 3.0, 0.7, rot=(0, 0, 0), sw="trim")
        c.cube("crown", (sx * 2.4 - 0.9, 19.4, -1.9), (1.8, 1.8, 1.8), "white")
        c.cube("crown", (sx * 2.4 - 0.4, 19.7, -2.0), (0.8, 0.9, 0.3), "glow")
    for dx, dy in ((0, 0), (-1.2, 0), (1.2, 0), (0, 1.2), (0, -1.2)):                    # the star on the brow
        c.cube("brow", (dx - 0.5, 13.6 + dy - 0.5, -4.95), (1.0, 1.0, 0.6), "glow")
    for sx in SIDES:                                                                       # a pincer at each temple
        c.arc(sb("horn", sx), (sx * 5.3, 11.0, -1.0), sx * 70, sx * -140, 3, 1.8, 1.4, 0.7, "trim")


def head_lion_brow(c):
    c.cube("brow", (-4.8, 12.6, -5.1), (9.6, 1.6, 0.9), "gold")
    for sx in SIDES:
        c.cube("brow", (sx * 2.6 - 1.0, 12.5, -5.45), (2.0, 1.8, 0.5), "black")
        c.cube("brow", (sx * 2.6 - 0.6, 12.8, -5.6), (1.2, 1.2, 0.4), "eye")
    c.spike("brow", (0, 14.2, -5.1), 1.6, 1.0, sw="gem")


def head_third_eye_hood(c):
    head_hood(c)
    c.cube("brow", (-1.1, 13.0, -4.95), (2.2, 1.5, 0.6), "white")
    c.cube("brow", (-0.5, 13.3, -5.15), (1.0, 1.0, 0.4), "eye")
    c.cube("brow", (-1.5, 14.5, -5.0), (3.0, 0.5, 0.5), "black")
    for k in range(6):                                                                      # the serpent coiled over the hood
        a = 2 * math.pi * k / 6
        c.cube("crown", (math.cos(a) * 3.2 - 0.7, 15.4 + 0.5 * k, math.sin(a) * 3.0 - 0.7), (1.4, 1.4, 1.4), "leaf" if k % 2 else "accent")
    c.cube("crown", (-0.8, 18.6, -1.6), (1.6, 1.2, 1.6), "accent")


def head_coral_crown(c):
    c.cube("crown", (-4.8, 14.6, -4.3), (9.6, 0.9, 8.6), "gold")
    for k in range(5):
        x = -3.6 + k * 1.8
        c.spike("crown", (x, 15.4, -0.5 + (k % 2) * 1.4), 3.6 + (k % 3) * 1.4, 1.5, rot=(0, 0, -x * 6), sw="coral")
        c.spike("crown", (x + 0.3, 17.4 + (k % 3) * 0.6, -0.5 + (k % 2) * 1.4), 2.0, 1.0, rot=(0, 0, 36), sw="coral")
    for sx in SIDES:
        c.cube(sb("horn", sx), (sx * 5.4 - 0.9, 10.8, -1.6), (1.8, 2.8, 3.2), "white")
        c.cube(sb("horn", sx), (sx * 5.9 - 0.4, 11.5, -0.9), (0.8, 1.4, 1.8), "coral")


def head_dove_wings(c):
    for sx in SIDES:
        for k, (r, h) in enumerate(((78, 4.6), (52, 5.0), (26, 4.8))):
            c.spike("crown", (sx * 1.2, 14.8 + k * 0.5, -1.2), h, 1.8, rot=(0, 0, -sx * r), sw="white" if k != 1 else "feather", wz=0.7)
    c.cube("crown", (-1.0, 15.2, -1.5), (2.0, 1.2, 1.6), "white")


def head_hound_skull(c):
    c.cube("brow", (-3.5, 13.4, -5.4), (7.0, 3.0, 2.6), "bone")
    for sx in SIDES:
        c.cube("brow", (sx * 1.9 - 0.8, 13.9, -5.65), (1.6, 1.6, 0.4), "black")
        c.cube("brow", (sx * 1.9 - 0.3, 14.3, -5.75), (0.6, 0.6, 0.3), "eye")
        c.spike("brow", (sx * 1.5, 13.4, -5.2), 1.6, 0.8, rot=(180, 0, 0), sw="white")
        c.spike(sb("ear", sx), (sx * 4.6, 14.2, -0.4), 3.6, 2.2, rot=(0, 0, -sx * 62), sw="hair", wz=1.0)
    for k in range(10):
        a = math.pi * (0.0 + 2.0 * k / 10)
        radial_spike(c, "hair", (0, 6.5, 0), 4.4, a, 2.4, 1.1, -18, "iron", wz=1.0)
    horns_swept(c, "bone")


def head_goggles(c):
    c.cube("brow", (-5.2, 12.2, -5.0), (10.4, 1.0, 0.9), "wood")
    for sx in SIDES:
        c.cube("brow", (sx * 2.5 - 1.6, 10.6, -5.4), (3.2, 3.2, 0.9), "gold")
        c.cube("brow", (sx * 2.5 - 1.1, 11.1, -5.6), (2.2, 2.2, 0.5), "glow")
    c.cube("brow", (-1.0, 7.6, -5.0), (2.0, 0.4, 0.4), "gold")
    c.cube("brow", (-1.0, 6.9, -5.0), (0.4, 0.8, 0.4), "gold")
    c.cube("brow", (0.6, 6.9, -5.0), (0.4, 0.8, 0.4), "gold")
    c.cube("brow", (-1.0, 6.7, -5.0), (2.0, 0.4, 0.4), "gold")


def head_sallet(c):
    c.cube("crown", (-5.3, 12.4, -4.8), (10.6, 3.8, 9.6), "metal")
    c.cube("crown", (-0.7, 6.9, -5.2), (1.4, 7.0, 0.8), "silver")
    c.cube("crown", (-5.4, 12.2, 4.6), (10.8, 1.2, 1.6), "metal")
    c.arc("crown", (0, 16.0, 0.6), 18, 80, 5, 2.8, 1.8, 0.8, "ice", plane="yz", wz=0.9)
    c.cube("hair", (-3.0, 1.0, -4.0), (6.0, 6.2, 1.8), "white")
    c.cube("hair", (-2.2, -2.4, -3.6), (4.4, 3.6, 1.4), "white")


def head_three_faces(c):
    for k, x in enumerate((-3.4, 0.0, 3.4)):
        c.cube("brow", (x - 1.3, 13.2 + (1.0 if k == 1 else 0), -5.0), (2.6, 2.8, 0.8), ("bone", "skin", "wood")[k])
        c.cube("brow", (x - 0.9, 14.4 + (1.0 if k == 1 else 0), -5.2), (0.6, 0.6, 0.3), "eye")
        c.cube("brow", (x + 0.3, 14.4 + (1.0 if k == 1 else 0), -5.2), (0.6, 0.6, 0.3), "eye")
    for sx in SIDES:
        c.spike("brow", (sx * 3.4, 16.0, -4.8), 1.8, 0.8, rot=(0, 0, -sx * 20), sw="gold")
    horns_lyre(c)
    for sx in SIDES:
        c.arc(sb("ear", sx), (sx * 5.1, 11.0, 0), sx * 60, sx * 220, 4, 1.4, 1.5, 0.7, "bone")
    for k in range(5):
        x = -4 + k * 2.0
        c.spike("crown", (x, 15.0, 1.5), 3.2 + (k % 2) * 1.6, 1.6, rot=(8, 0, 0), sw="fire")


def head_sun_crown(c):
    c.cube("crown", (-4.9, 14.5, -4.4), (9.8, 1.1, 8.8), "gold")
    for k in range(9):
        a = 2 * math.pi * k / 9
        radial_spike(c, "crown", (0, 15.4, 0), 4.2, a, 3.0 + (k % 2) * 1.4, 1.1, 24, "glow" if k % 2 else "gold")


def head_horse_plate(c):
    c.cube("brow", (-3.4, 8.8, -5.3), (6.8, 5.6, 1.0), "iron")
    for sx in SIDES:
        c.cube("brow", (sx * 2.0 - 0.9, 10.4, -5.5), (1.8, 1.2, 0.4), "glow")
    c.cube("brow", (-0.6, 8.4, -5.5), (1.2, 6.0, 0.4), "metal")
    ears_horse(c)
    for k, (y, h) in enumerate(((13.5, 3.4), (10.8, 3.0), (8.2, 2.6), (5.8, 2.2))):
        c.cube("hair", (-1.0, y, 4.6), (2.0, h, 2.0), "fire", rot=(-6 * k, 0, 0), pivot=(0, y + h, 4.6))


def head_hollow_crown(c):
    for k, (y, r) in enumerate(((16.8, 3.8), (19.0, 3.0), (21.0, 2.2))):
        c.ring("crown", (0, y, 0), r, 8, 1.2, 0.7, "crimson" if k % 2 == 0 else "glow", thick=1.0)
    for sx in SIDES:
        end = c.arc(sb("horn", sx), (sx * 3.6, 14.4, 0), sx * 36, sx * 140, 5, 2.4, 2.0, 1.0, "shadow")
        c.disc_v(sb("horn", sx), (end[0], end[1], 0), 1.7, 6, 0.9, 0.9, "glow")


def head_pane_crown(c):
    for k in range(5):
        a = math.radians(-60 + k * 30)
        x, y = math.sin(a) * 4.6, 15.0
        c.spike("crown", (x, y, -0.8 + 0.6 * abs(math.sin(a))), 4.0 + 1.8 * math.cos(a), 1.8, rot=(0, 0, -math.degrees(a) * 0.35), sw="ice", wz=0.5)
    c.cube("brow", (-1.8, 12.4, -5.2), (3.6, 3.4, 0.7), "silver")
    c.cube("brow", (-1.3, 12.9, -5.45), (2.6, 2.4, 0.4), "glow")
    for sx in SIDES:
        c.arc(sb("horn", sx), (sx * 5.2, 10.6, 0), sx * 80, sx * -150, 3, 1.6, 1.8, 0.8, "water")


def head_aigrette(c):
    for k, (x, h) in enumerate(((-2.4, 7.0), (0.0, 8.6), (2.4, 7.0))):
        c.spike("crown", (x, 15.2, -0.4), h, 0.55, rot=(0, 0, -x * 4), sw="trim")
        c.cube("crown", (x - 1.1, 15.2 + h - 0.2, -1.1), (2.2, 2.6, 1.6), "feather")
        c.cube("crown", (x - 0.4, 15.2 + h + 0.7, -1.2), (0.8, 0.8, 0.4), "glow")
    for k in range(5):
        c.spike("crown", (-3.6 + k * 1.8, 15.0, 2.2), 4.4, 1.5, rot=(28, 0, (k - 2) * -8), sw="feather", wz=0.7)


def head_bolt_temples(c):
    for sx in SIDES:
        for q in range(3):
            c.cube(sb("horn", sx), (sx * 5.5 - 0.5 + (0.5 if q % 2 else -0.5), 13.4 - q * 1.6, -0.5), (1.0, 1.9, 1.0), "bolt", rot=(0, 0, 24 if q % 2 else -24),
                   pivot=(sx * 5.5, 13.4 - q * 1.6, 0))


def head_croc_snout(c):
    c.cube("brow", (-3.6, 12.8, -9.0), (7.2, 2.2, 4.7), "leaf")
    c.cube("brow", (-3.0, 12.4, -9.2), (6.0, 0.8, 4.6), "bone")
    for k in range(4):
        c.spike("brow", (-2.4 + k * 1.6, 12.8, -8.4), 1.0, 0.7, rot=(180, 0, 0), sw="white")
    for sx in SIDES:
        c.cube("brow", (sx * 1.2 - 0.4, 14.4, -9.2), (0.8, 0.6, 0.5), "black")
        c.cube("crown", (sx * 2.8 - 1.0, 15.0, -2.6), (2.0, 1.8, 2.0), "leaf")
        c.cube("crown", (sx * 2.8 - 0.5, 15.5, -2.75), (1.0, 1.0, 0.4), "eye")
    for k in range(5):
        c.spike("hair", (0, 14.4 - k * 1.8, 4.6), 2.4, 1.4, rot=(40, 0, 0), sw="leaf" if k % 2 else "trim")


def head_wheel_crown(c):
    c.cube("crown", (-4.8, 14.6, -4.3), (9.6, 0.9, 8.6), "trim")
    c.disc_v("crown", (0, 18.2, 0), 3.0, 8, 1.3, 1.3, "wood")
    c.cube("crown", (-3.0, 17.8, -0.4), (6.0, 0.8, 0.8), "wood")
    c.cube("crown", (-0.4, 15.2, -0.4), (0.8, 6.0, 0.8), "wood")
    c.cube("crown", (-0.8, 17.6, -0.8), (1.6, 1.6, 1.6), "gem")
    for sx in SIDES:
        for k in range(3):
            c.spike("crown", (sx * (3.0 + k * 0.7), 15.0, -2.0 + k * 1.2), 3.0 + k * 0.4, 1.3, rot=(0, 0, -sx * (24 + 10 * k)), sw="leaf", wz=0.6)


def head_gale_helm(c):                                           # a plain brow band and sweep: the default for the Djinn without a headgear
    c.cube("crown", (-5.1, 14.0, -4.6), (10.2, 0.9, 9.2), "trim")


HEAD = {"bolt_crown": head_bolt_crown, "flame_crown": head_flame_crown, "fin_tiara": head_fin_tiara, "wind_plume": head_wind_plume,
        "antlers_leaves": head_antlers_leaves, "great_helm": head_great_helm, "bell_crown": head_bell_crown, "wolf_cowl": head_wolf_cowl, "hood": head_hood,
        "storm_visor": head_storm_visor, "crescent_circlet": head_crescent_circlet, "bone_mask": head_bone_mask, "ice_crown": head_ice_crown,
        "scholar_cap": head_scholar_cap, "skull_mask": head_skull_mask, "beak_hood": head_beak_hood, "carapace_cap": head_carapace_cap, "lion_brow": head_lion_brow,
        "third_eye_hood": head_third_eye_hood, "coral_crown": head_coral_crown, "dove_wings": head_dove_wings, "hound_skull": head_hound_skull,
        "goggles": head_goggles, "sallet": head_sallet, "three_faces": head_three_faces, "sun_crown": head_sun_crown, "horse_plate": head_horse_plate,
        "hollow_crown": head_hollow_crown, "pane_crown": head_pane_crown, "aigrette": head_aigrette, "band": head_gale_helm,
        "horns_crescent": horns_crescent, "horns_ram": horns_ram, "horns_bull": horns_bull, "horns_stag": horns_stag, "horns_spike": horns_spike,
        "horns_swept": horns_swept, "horns_lyre": horns_lyre, "horns_spiral": horns_spiral,
        "crescent_horns_gold": lambda c: horns_crescent(c, "gold"), "horns_ice": lambda c: horns_crescent(c, "ice"), "bolt_temples": head_bolt_temples,
        "croc_snout": head_croc_snout, "wheel_crown": head_wheel_crown, "ram_horns_dark": lambda c: horns_ram(c, "shadow"), "spire_crown": None}


def head_spire_crown(c):
    c.cube("crown", (-5.0, 14.4, -4.5), (10.0, 1.2, 9.0), "gold")
    for k in range(9):
        a = 2 * math.pi * k / 9
        r = 3.6 if k % 3 else 0.0
        radial_spike(c, "crown", (0, 15.4, 0), r if k else 0, a, 3.4 + (k % 3) * 0.9, 1.4, 6 if k else 0, "gold" if k % 2 else "trim")
    for sx in SIDES:
        c.arc("crown", (sx * 3.8, 15.4, 0), sx * -4, sx * -30, 3, 2.4, 0.9, 0.9, "gold")
    c.cube("crown", (-1.0, 21.2, -1.0), (2.0, 2.0, 2.0), "gem")


HEAD["spire_crown"] = head_spire_crown


# ================================================================ capes
def cape_cape(c):
    c.pbox("cape", (-3.5, -3.5, 2.1), (7, 9, 1), tp.cape_painter(c.spec))


def cape_none(c):
    pass


def cape_scarf(c):
    c.cube("scarf", (-3.4, 4.6, -2.6), (6.8, 1.6, 5.2), "cloth2")
    c.cube("scarf", (-1.0, -2.0, 2.6), (2.0, 8.6, 1.0), "cloth2", rot=(-6, 0, 0), pivot=(0, 5, 2.6))
    c.cube("scarf", (-1.0, -3.2, 2.6), (2.0, 1.2, 1.0), "trim", rot=(-6, 0, 0), pivot=(0, 5, 2.6))
    c.cube("scarf", (3.6, -0.6, 1.2), (1.8, 6.2, 1.0), "cloth", rot=(-8, 0, -8), pivot=(4.4, 5, 1.2))


def cape_mantle(c):
    c.cube("scarf", (-5.4, 4.0, -2.8), (10.8, 2.0, 5.6), "cloth2")
    c.cube("scarf", (-5.6, 3.4, -3.0), (11.2, 0.8, 6.0), "trim")
    c.cube("scarf", (-3.4, -2.4, 2.4), (6.8, 6.2, 0.9), "cloth2", rot=(-5, 0, 0), pivot=(0, 4, 2.4))


def cape_feather(c):
    c.cube("scarf", (-5.0, 4.2, -2.6), (10.0, 1.8, 5.2), "feather")
    for k in range(8):
        x = -4.2 + k * 1.2
        c.spike("cape", (x, 4.0, 2.6), 6.4 - abs(k - 3.5) * 0.7, 1.5, rot=(-10, 0, (k - 3.5) * 4), sw="feather" if k % 2 else "cloth2", wz=0.7)


def cape_veil(c):
    c.cube("cape", (-3.6, -3.0, 2.2), (7.2, 8.6, 0.6), "smoke")
    c.cube("cape", (-3.8, -3.4, 2.3), (7.6, 1.0, 0.8), "silver")


def cape_tattered(c):
    cape_cape(c)
    for k, x in enumerate((-3.0, -0.6, 1.8)):
        c.spike("cape", (x, -4.2, 2.5), 2.8 + (k % 2), 1.4, rot=(0, 0, 180), sw="cloth", wz=0.9)


def cape_wings_short(c):
    for sx in SIDES:
        for k in range(4):
            c.spike("cape", (sx * (1.6 + k * 0.4), 5.0 - k * 0.3, 2.6), 5.2 - k * 0.3, 1.8, rot=(-6, 0, -sx * (42 + 12 * k)), sw="white" if k % 2 else "feather", wz=0.7)


def cape_plates(c):
    for k in range(4):
        c.cube("cape", (-3.6 + k * 0.3, 4.4 - k * 2.2, 2.2 + k * 0.1), (7.2 - k * 0.6, 2.4, 1.0), "trim" if k % 2 == 0 else "gold")


def cape_ragged_cloak(c):
    cape_tattered(c)
    for k in range(5):
        c.spike("cape", (-3.2 + k * 1.6, -3.6, 3.0), 2.4, 0.9, rot=(0, 0, 180), sw="ice", wz=0.6)


CAPE = {"cape": cape_cape, "none": cape_none, "scarf": cape_scarf, "mantle": cape_mantle, "feather": cape_feather, "veil": cape_veil, "tattered": cape_tattered,
        "wings": cape_wings_short, "plates": cape_plates, "icecloak": cape_ragged_cloak}


# ================================================================ weapons (the weapon bone; the grip is the right hand)
def shaft(c, length, w=1.0, sw="wood", y0=-4.0):
    c.cube("weapon", (G[0] - w / 2, y0, G[2] - w / 2), (w, length, w), sw)
    return y0 + length


def w_lance(c, head="silver", flair="bolt"):
    top = shaft(c, 20, 1.0, "wood")
    c.spike("weapon", (G[0], top - 0.2, G[2]), 6.0, 2.4, sw=head, wz=0.9)
    c.spike("weapon", (G[0], top + 5.6, G[2]), 2.0, 1.4, sw=head, wz=0.7)
    c.cube("weapon", (G[0] - 1.6, top - 1.2, G[2] - 0.5), (3.2, 0.9, 1.0), "trim")
    c.cube("weapon", (G[0] - 0.6, top + 1.0, G[2] - 0.7), (1.2, 2.4, 0.4), flair if flair else "glow")
    c.cube("weapon", (G[0] - 0.4, -4.6, G[2] - 0.4), (0.8, 1.0, 0.8), "gold")


def w_pennon_lance(c, flair="fire"):
    w_lance(c, "iron", None)
    c.cube("weapon", (G[0] + 0.5, 12.0, G[2] - 0.2), (0.4, 3.4, 4.6), flair, rot=(0, 0, -6), pivot=(G[0], 15.4, G[2]))


def w_star_lance(c):
    top = shaft(c, 20, 1.0, "gold")
    c.disc_v("weapon", (G[0], top + 2.6, G[2]), 2.4, 5, 1.1, 0.7, "gold")
    c.cube("weapon", (G[0] - 0.7, top + 1.9, G[2] - 0.5), (1.4, 1.4, 1.0), "glow")
    for k, dx in enumerate((-2.2, 0.0, 2.2)):
        c.spike("weapon", (G[0] + dx * 0.5, top - 0.5, G[2]), 5.0, 1.2, rot=(0, 0, -dx * 10), sw="feather", wz=0.5)
        c.cube("weapon", (G[0] + dx * 0.9 - 0.5, top + 4.0, G[2] - 0.4), (1.0, 1.0, 0.5), "glow")


def w_blade(c, length=15, width=2.4, guard=5.0, sw="silver", edge="white", gsw="gold", hollow=False, curve=0.0, tipglow=False, lion=False):
    c.cube("weapon", (G[0] - 0.6, -3.4, G[2] - 0.6), (1.2, 3.8, 1.2), "wood")
    c.cube("weapon", (G[0] - 0.8, -3.9, G[2] - 0.8), (1.6, 1.0, 1.6), gsw)
    c.cube("weapon", (G[0] - guard / 2, 0.4, G[2] - 0.7), (guard, 1.0, 1.4), gsw)
    if lion:
        c.cube("weapon", (G[0] - 1.3, 1.3, G[2] - 1.0), (2.6, 2.2, 0.8), "gold")
        c.cube("weapon", (G[0] - 0.9, 1.7, G[2] - 1.4), (0.6, 0.6, 0.4), "eye")
        c.cube("weapon", (G[0] + 0.3, 1.7, G[2] - 1.4), (0.6, 0.6, 0.4), "eye")
    if hollow:
        c.cube("weapon", (G[0] - width / 2, 1.4, G[2] - 0.4), (0.7, length, 0.8), sw)
        c.cube("weapon", (G[0] + width / 2 - 0.7, 1.4, G[2] - 0.4), (0.7, length, 0.8), sw)
        c.cube("weapon", (G[0] - width / 2 + 0.7, 2.0, G[2] - 0.2), (width - 1.4, length - 1.2, 0.4), "glow")
        c.spike("weapon", (G[0], 1.4 + length, G[2]), 3.0, width, sw=sw, wz=0.8)
        return
    segs = 3
    for k in range(segs):
        seg = length / segs
        y = 1.4 + seg * k
        bend = curve * k
        c.cube("weapon", (G[0] - width / 2 + bend * 0.08 * seg, y, G[2] - 0.45), (width - 0.3 * k, seg + 0.3, 0.9), sw if k != 1 else edge,
               rot=(0, 0, -bend) if bend else None, pivot=(G[0], y, G[2]) if bend else None)
    c.spike("weapon", (G[0] + curve * 0.4, 1.4 + length - 0.2, G[2]), 3.2, width - 0.9, rot=(0, 0, -curve * 3), sw="glow" if tipglow else edge, wz=0.7)


def w_greatsword(c, sw="silver", edge="white", fire=False):
    w_blade(c, 17, 3.2, 6.4, "iron" if fire else sw, "fire" if fire else edge, "gold")


def w_claymore(c):
    w_blade(c, 17, 3.0, 7.0, "silver", "white", "gold")
    for x in (-2.2, 0, 2.2):
        c.spike("weapon", (G[0] + x * 1.6, 1.2, G[2]), 2.2, 1.0, rot=(0, 0, -x * 6), sw="trim")


def w_rapier(c, gsw="silver", spiral=False):
    c.cube("weapon", (G[0] - 0.5, -3.0, G[2] - 0.5), (1.0, 3.4, 1.0), "wood")
    c.cube("weapon", (G[0] - 2.4, 0.4, G[2] - 0.5), (4.8, 0.7, 1.0), gsw)
    c.disc_v("weapon", (G[0], 0.9, G[2] - 0.8), 1.8, 6, 0.9, 0.6, gsw)
    for k in range(5):
        c.cube("weapon", (G[0] - 0.4 + (0.1 if k % 2 else -0.1), 1.0 + k * 3.0, G[2] - 0.35), (0.8 - k * 0.07, 3.1, 0.7), ("white" if spiral and k % 2 else "silver"),
               rot=(0, 30 * k, 0) if spiral else None, pivot=(G[0], 2.5 + k * 3, G[2]) if spiral else None)
    c.cube("weapon", (G[0] - 0.3, 16.0, G[2] - 0.3), (0.6, 1.4, 0.6), "glow")


def w_sabre(c):
    w_blade(c, 14, 2.0, 3.6, "silver", "white", "gold", curve=6.0)
    c.spike("weapon", (G[0], -4.4, G[2]), 1.8, 1.4, rot=(0, 0, 180), sw="feather", wz=0.6)


def w_trident(c, flames=False, faces=False):
    top = shaft(c, 19, 1.0, "gold" if flames else "silver")
    c.cube("weapon", (G[0] - 2.6, top - 0.2, G[2] - 0.5), (5.2, 1.0, 1.0), "gold")
    for k, dx in enumerate((-2.2, 0.0, 2.2)):
        h = 5.4 if k == 1 else 4.4
        c.spike("weapon", (G[0] + dx, top + 0.6, G[2]), h, 1.1 if k != 1 else 1.4, sw="silver" if not flames else "gold", wz=0.8)
        if flames:
            c.spike("weapon", (G[0] + dx, top + 0.6 + h, G[2]), 2.6, 1.6, sw="fire", wz=1.0)
        if faces:
            c.cube("weapon", (G[0] + dx - 0.7, top - 1.8, G[2] - 0.8), (1.4, 1.5, 0.5), "bone")


def w_glaive(c, sw="silver", glow="glow", moon=False):
    top = shaft(c, 21, 1.0, "wood")
    c.cube("weapon", (G[0] - 0.8, top - 0.6, G[2] - 0.8), (1.6, 1.4, 1.6), "gold")
    c.arc("weapon", (G[0], top + 0.6, G[2]), 6, 40, 5, 2.6, 2.2, 1.0, sw, wz=0.6)
    c.spike("weapon", (G[0], top + 0.4, G[2]), 3.6, 1.6, rot=(0, 0, 160), sw=sw, wz=0.6)
    c.cube("weapon", (G[0] - 0.4, top + 1.2, G[2] - 0.5), (0.8, 2.6, 0.3), glow)


def w_staff(c, head="orb"):
    top = shaft(c, 21, 1.0, "wood")
    if head == "orb":
        c.cube("weapon", (G[0] - 1.4, top, G[2] - 1.4), (2.8, 2.8, 2.8), "gem")
        for sx in SIDES:
            c.spike("weapon", (G[0] + sx * 1.0, top - 0.6, G[2]), 3.6, 0.8, rot=(0, 0, -sx * 24), sw="gold")
    elif head == "bloom":
        c.cube("weapon", (G[0] - 1.0, top, G[2] - 1.0), (2.0, 1.6, 2.0), "leaf")
        for k in range(5):
            a = 2 * math.pi * k / 5
            c.cube("weapon", (G[0] + math.cos(a) * 1.6 - 0.8, top + 1.4, G[2] + math.sin(a) * 1.6 - 0.8), (1.6, 1.2, 1.6), "coral")
        c.cube("weapon", (G[0] - 0.6, top + 1.4, G[2] - 0.6), (1.2, 1.2, 1.2), "gold")
    elif head == "wheel":
        c.disc_v("weapon", (G[0], top + 3.0, G[2]), 2.8, 8, 1.2, 0.8, "wood")
        c.cube("weapon", (G[0] - 0.4, top + 2.6, G[2] - 0.4), (0.8, 0.8, 0.8), "leaf")
        for k in range(5):
            a = math.radians(90 + k * 72)
            c.spike("weapon", (G[0], top + 3.0, G[2]), 2.6, 0.5, rot=(0, 0, math.degrees(a) - 90), sw="wood")
        for k in range(5):                                             # the serpent
            c.cube("weapon", (G[0] + (0.9 if k % 2 else -0.9) - 0.5, top - 4.0 + k * 2.2, G[2] - 0.5 + 0.5), (1.0, 1.4, 1.0), "leaf")
    elif head == "tome":
        c.cube("weapon", (G[0] - 2.2, top + 0.2, G[2] - 1.6), (4.4, 3.6, 3.2), "cloth")
        c.cube("weapon", (G[0] - 1.8, top + 0.6, G[2] - 1.7), (3.6, 2.8, 0.4), "white")
        c.cube("weapon", (G[0] - 0.5, top + 1.2, G[2] - 1.95), (1.0, 1.0, 0.3), "glow")
        for k in range(3):
            c.cube("weapon", (G[0] - 2.4 + k * 2.4, top - 1.4 - k * 0.5, G[2] - 0.35), (0.8, 1.6, 0.7), "iron", rot=(0, 0, 14 * (k - 1)), pivot=(G[0], top + 0.2, G[2]))
    elif head == "crystal":
        for k, (dx, h, tilt) in enumerate(((0, 6.0, 0), (-1.6, 4.0, 32), (1.6, 4.4, -32), (-2.8, 2.6, 62), (2.8, 2.8, -62))):
            c.spike("weapon", (G[0] + dx * 0.4, top, G[2]), h, 1.6 if k == 0 else 1.2, rot=(0, 0, tilt), sw="ice", wz=1.0)
        c.cube("weapon", (G[0] - 0.7, top + 1.6, G[2] - 0.7), (1.4, 1.4, 1.4), "glow")
    elif head == "quill":
        c.arc("weapon", (G[0], top - 0.4, G[2]), 0, 26, 4, 2.4, 1.6, 0.6, "black", wz=0.5)
        c.cube("weapon", (G[0] - 1.1, top + 8.6, G[2] - 0.4), (2.2, 1.2, 0.5), "trim")
        c.cube("weapon", (G[0] - 0.9, top + 0.2, G[2] - 0.5), (1.8, 1.0, 1.0), "gold")
        c.cube("weapon", (G[0] - 2.4, top + 1.6, G[2] - 1.0), (2.4, 1.6, 2.0), "black")      # the crow perched on the staff
        c.cube("weapon", (G[0] - 3.0, top + 2.8, G[2] - 1.2), (1.4, 1.4, 1.4), "black")
        c.cube("weapon", (G[0] - 3.9, top + 3.1, G[2] - 0.7), (1.0, 0.6, 0.6), "trim")


def w_axe(c):
    top = shaft(c, 19, 1.2, "wood")
    c.cube("weapon", (G[0] - 0.8, top - 5.2, G[2] - 1.0), (1.6, 5.2, 2.0), "iron")
    c.cube("weapon", (G[0] + 0.6, top - 5.0, G[2] - 0.5), (4.0, 4.6, 1.0), "metal")
    c.cube("weapon", (G[0] + 3.6, top - 5.6, G[2] - 0.4), (1.2, 5.8, 0.8), "silver")
    c.cube("weapon", (G[0] - 4.2, top - 4.4, G[2] - 0.5), (3.4, 3.2, 1.0), "metal")
    c.spike("weapon", (G[0], top, G[2]), 3.4, 1.4, sw="silver", wz=0.8)


def w_horn(c):
    c.arc("weapon", (G[0], -2.4, G[2]), 0, 64, 7, 3.2, 3.8, 1.2, "bone", wz=3.0)
    c.cube("weapon", (G[0] - 1.9, 16.4, G[2] - 1.6), (3.8, 1.0, 3.2), "gold")
    c.cube("weapon", (G[0] - 0.9, -3.0, G[2] - 0.9), (1.8, 1.2, 1.8), "gold")
    c.cube("weapon", (G[0] - 0.6, 4.0, G[2] - 0.6), (1.2, 1.2, 1.2), "glow")


def w_claws(c):
    for k, dx in enumerate((-0.9, 0.0, 0.9)):
        c.spike("weapon", (G[0] + dx, -0.4, G[2] - 0.6 + k * 0.1), 4.8, 0.9, rot=(180 - 6, 0, 0), sw="white" if k != 1 else "silver", wz=0.6)
    c.cube("weapon", (G[0] - 1.4, -0.8, G[2] - 1.2), (2.8, 0.8, 2.4), "metal")
    c.cube("weapon", (G[0] - 0.4, -1.4, G[2] - 1.5), (0.8, 0.6, 0.4), "glow")


def w_bow(c, antler=False):
    c.arc("weapon", (G[0], 0.4, G[2]), 8, -42, 5, 2.4, 1.2, 0.6, "wood", wz=1.0)
    c.arc("weapon", (G[0], 0.4, G[2]), 172, 42, 5, 2.4, 1.2, 0.6, "wood", wz=1.0)
    c.cube("weapon", (G[0] + 0.7, -9.6, G[2] - 0.15), (0.3, 20.6, 0.3), "white")
    c.cube("weapon", (G[0] - 0.7, -1.0, G[2] - 0.8), (1.4, 3.0, 1.6), "gold")
    if antler:
        for sy in (1, -1):
            c.spike("weapon", (G[0] - 0.4, 10.4 * sy + 0.4, G[2]), 2.6, 0.8, rot=(0, 0, 40 * sy), sw="bone")
    c.cube("weapon", (G[0] - 0.2, 0.2, G[2] - 4.2), (0.4, 0.4, 4.0), "wood")
    c.spike("weapon", (G[0], 0.3, G[2] - 4.2), 1.2, 0.9, rot=(-90, 0, 0), sw="silver", wz=0.4)


def w_scythe(c):
    top = shaft(c, 24, 1.0, "bone")
    c.arc("weapon", (G[0], top - 1.0, G[2]), 80, -75, 6, 3.0, 2.2, 0.7, "silver", wz=0.8)
    c.cube("weapon", (G[0] - 0.5, top - 2.2, G[2] - 0.5), (1.0, 3.2, 1.0), "iron")
    c.cube("weapon", (G[0] - 0.6, top - 3.6, G[2] - 0.6), (1.2, 1.0, 1.2), "glow")


def w_harpoon(c):
    top = shaft(c, 20, 1.0, "wood")
    c.spike("weapon", (G[0], top - 0.2, G[2]), 5.0, 1.8, sw="silver", wz=0.8)
    for sx in SIDES:
        c.spike("weapon", (G[0] + sx * 0.5, top + 1.4, G[2]), 2.6, 1.0, rot=(0, 0, -sx * 140), sw="bone")
    for k in range(4):
        c.cube("weapon", (G[0] + (k % 2) * 0.9 - 1.0, 1.0 + k * 1.2, G[2] - 0.8 - 0.1 * k), (1.4, 0.9, 0.8), "bone")


def w_daggers(c):
    for sx in SIDES:
        c.cube("weapon", (G[0] - 0.5 + sx * 0.5, -1.0, G[2] - 0.5), (1.0, 3.2, 1.0), "wood")
        c.cube("weapon", (G[0] - 1.4 + sx * 0.5, 1.8, G[2] - 0.6), (2.8, 0.8, 1.2), "gold")
        for k in range(3):
            c.cube("weapon", (G[0] + sx * 0.5 - 0.7 + 0.5 * math.sin(k * 1.2) * sx, 2.6 + k * 2.2, G[2] - 0.35), (1.3 - 0.2 * k, 2.4, 0.7), "silver" if k else "white",
                   rot=(0, 0, -sx * (16 + 4 * k)), pivot=(G[0] + sx * 0.5, 2.6 + k * 2.2, G[2]))
        c.cube("weapon", (G[0] - 0.4 + sx * 1.8, 4.6, G[2] - 0.8), (0.8, 0.8, 0.4), "gem")


def w_anchor(c):
    top = shaft(c, 17, 1.2, "iron")
    c.disc_v("weapon", (G[0], top + 2.0, G[2]), 1.8, 6, 0.9, 0.9, "iron")
    c.cube("weapon", (G[0] - 3.0, top - 3.0, G[2] - 0.6), (6.0, 1.2, 1.2), "iron")
    for sx in SIDES:
        c.arc("weapon", (G[0] + sx * 0.5, -3.6, G[2]), sx * 98, sx * -128, 4, 2.0, 1.6, 0.9, "iron")
    for k in range(5):
        c.cube("weapon", (G[0] + (1.8 if k % 2 else 0.8) - 0.5, top - 1.0 - k * 1.5, G[2] - 1.5 - 0.1 * k), (1.0, 1.2, 0.5), "silver")


def w_fans(c):
    for sx in SIDES:
        for k in range(5):
            a = (k - 2) * 18
            c.spike("weapon", (G[0] + sx * 0.9, 1.0, G[2] - (0.4 if sx > 0 else -0.4)), 9.0, 1.5, rot=(0, 0, a), sw="white" if k % 2 else "feather", wz=0.4)
        c.cube("weapon", (G[0] + sx * 0.9 - 0.4, 0.2, G[2] - 0.5), (0.8, 1.6, 1.0), "gold")


def w_halberd(c):
    top = shaft(c, 22, 1.2, "wood")
    c.spike("weapon", (G[0], top - 0.2, G[2]), 4.6, 1.8, sw="silver", wz=0.8)
    c.cube("weapon", (G[0] + 0.5, top - 4.2, G[2] - 0.5), (3.6, 4.0, 1.0), "metal")
    c.cube("weapon", (G[0] + 3.0, top - 4.6, G[2] - 0.4), (1.0, 4.8, 0.8), "ice")
    c.spike("weapon", (G[0] - 0.6, top - 3.0, G[2]), 3.0, 1.2, rot=(0, 0, 70), sw="silver", wz=0.8)
    c.cube("weapon", (G[0] - 4.4, top - 2.6, G[2] - 0.2), (3.8, 2.6, 0.4), "ice", rot=(0, 0, 4), pivot=(G[0] - 0.6, top - 2.0, G[2]))


def w_spear_eye(c):
    top = shaft(c, 20, 1.0, "iron")
    c.spike("weapon", (G[0], top - 0.2, G[2]), 6.6, 2.6, sw="silver", wz=0.8)
    c.cube("weapon", (G[0] - 0.7, top + 1.0, G[2] - 0.55), (1.4, 1.4, 0.3), "white")
    c.cube("weapon", (G[0] - 0.3, top + 1.4, G[2] - 0.7), (0.6, 0.6, 0.3), "eye")
    for k in range(6):
        c.cube("weapon", (G[0] + (0.9 if k % 2 else -0.9) - 0.6, top - 9.0 + k * 1.9, G[2] - 0.6), (1.2, 1.6, 1.2), "leaf" if k % 2 else "accent")


def w_cleaver(c):
    c.cube("weapon", (G[0] - 0.6, -3.4, G[2] - 0.6), (1.2, 3.8, 1.2), "wood")
    for sx in SIDES:
        c.arc("weapon", (G[0] + sx * 0.8, 0.5, G[2]), sx * 80, sx * -70, 2, 1.8, 1.6, 1.2, "bone")
    c.cube("weapon", (G[0] - 2.4, 1.0, G[2] - 0.5), (4.8, 11.0, 1.0), "gold")
    c.cube("weapon", (G[0] - 2.0, 1.4, G[2] - 0.65), (3.6, 10.0, 0.3), "silver")
    for k in range(5):
        c.cube("weapon", (G[0] + 2.2, 2.0 + k * 2.0, G[2] - 0.45), (1.0, 1.2, 0.9), "iron")
    c.cube("weapon", (G[0] - 1.2, -4.4, G[2] - 1.2), (2.4, 2.4, 2.4), "ice")
    c.cube("weapon", (G[0] - 0.8, -4.0, G[2] - 0.8), (1.6, 1.6, 1.6), "glow")


def w_pincers(c):
    for sx in SIDES:
        c.arc("weapon", (G[0] + sx * 0.6, 1.0, G[2]), sx * -16, sx * 112, 5, 2.4, 2.4, 0.9, "trim", wz=1.6)
    c.cube("weapon", (G[0] - 0.7, 8.4, G[2] - 0.7), (1.4, 1.4, 1.4), "glow")
    c.cube("weapon", (G[0] - 0.8, -1.0, G[2] - 0.8), (1.6, 3.0, 1.6), "wood")


def w_mace_drum(c):
    top = shaft(c, 18, 1.2, "wood")
    c.disc_v("weapon", (G[0], top + 3.0, G[2]), 3.0, 8, 1.6, 3.0, "bone")
    c.cube("weapon", (G[0] - 2.2, top + 0.8, G[2] - 1.4), (4.4, 4.4, 2.8), "wood")
    c.cube("weapon", (G[0] - 1.6, top + 1.4, G[2] - 1.6), (3.2, 3.2, 0.4), "bone")
    c.cube("weapon", (G[0] - 0.6, top + 2.4, G[2] - 1.9), (1.2, 1.2, 0.4), "black")
    for k in range(6):
        a = 2 * math.pi * k / 6
        c.spike("weapon", (G[0] + math.cos(a) * 2.8, top + 3.0 + math.sin(a) * 2.8, G[2]), 1.8, 1.0, rot=(0, 0, math.degrees(a) - 90), sw="iron")


WEAPON = {"lance": lambda c: w_lance(c), "pennon_lance": w_pennon_lance, "star_lance": w_star_lance, "greatsword": lambda c: w_greatsword(c),
          "firesword": lambda c: w_greatsword(c, fire=True), "claymore": w_claymore, "rapier": lambda c: w_rapier(c), "mirror_rapier": lambda c: w_rapier(c, "silver"),
          "horn_rapier": lambda c: w_rapier(c, "gold", True), "sabre": w_sabre, "trident": lambda c: w_trident(c), "flame_trident": lambda c: w_trident(c, True, True),
          "glaive": lambda c: w_glaive(c), "storm_glaive": lambda c: w_glaive(c, "iron", "bolt"), "moon_glaive": lambda c: w_glaive(c, "white", "glow"),
          "bloom_staff": lambda c: w_staff(c, "bloom"), "wheel_staff": lambda c: w_staff(c, "wheel"), "tome_staff": lambda c: w_staff(c, "tome"),
          "crystal_staff": lambda c: w_staff(c, "crystal"), "quill_staff": lambda c: w_staff(c, "quill"), "orb_staff": lambda c: w_staff(c, "orb"),
          "axe": w_axe, "horn": w_horn, "claws": w_claws, "bow": lambda c: w_bow(c), "antler_bow": lambda c: w_bow(c, True), "scythe": w_scythe, "harpoon": w_harpoon,
          "daggers": w_daggers, "anchor": w_anchor, "fans": w_fans, "halberd": w_halberd, "spear_eye": w_spear_eye, "cleaver": w_cleaver, "pincers": w_pincers,
          "drum_mace": w_mace_drum, "hollow_sword": lambda c: w_blade(c, 17, 3.2, 6.0, "shadow", "crimson", "gold", hollow=True),
          "lion_sword": lambda c: w_blade(c, 16, 2.8, 5.6, "gold", "white", "gold", lion=True), "crown_claymore": w_claymore}


# ================================================================ tails: the decoration along the four segments
def tail_decor(c, style):
    if style == "flame":
        for k, (b, y, dx) in enumerate((("tail1", -2.0, 2.8), ("tail1", -3.0, -2.8), ("tail2", -6.0, 2.4), ("tail2", -7.0, -2.4), ("tail3", -10.0, 2.0))):
            c.spike(b, (dx, y, 0), 3.4, 1.8, rot=(0, 0, -dx * 12), sw="fire")
        c.spike("tail4", (0, -15.0, 0), 3.0, 1.8, rot=(180, 0, 0), sw="fire")
    elif style == "water":
        for b, x, y, z in (("tail1", 3.4, -1.0, 1.0), ("tail2", -3.2, -6.0, -1.4), ("tail3", 2.6, -10.0, 1.4), ("tail4", -2.0, -14.0, -1.0)):
            c.cube(b, (x - 0.6, y - 0.6, z - 0.6), (1.2, 1.2, 1.2), "water")
        for sx in SIDES:
            c.spike("tail4", (sx * 0.4, -15.6, 0), 4.2, 2.2, rot=(180, 0, sx * 36), sw="water", wz=0.5)
    elif style == "wind":
        for b, y, r in (("tail2", -5.4, 4.6), ("tail3", -9.4, 3.8), ("tail4", -13.0, 3.0)):
            c.ring(b, (0, y, 0), r, 6, 1.8, 0.5, "wind", thick=1.0)
    elif style == "bolt":
        for k, (b, x, y, rz) in enumerate((("tail1", 3.0, -1.0, -34), ("tail2", -3.0, -6.0, 34), ("tail3", 2.6, -9.0, -34), ("tail4", -2.0, -13.0, 30))):
            c.cube(b, (x - 0.4, y - 1.6, -0.4), (0.8, 3.2, 0.8), "bolt", rot=(0, 0, rz), pivot=(x, y, 0))
    elif style == "ice":
        for k, (b, x, y) in enumerate((("tail1", 2.8, -1.5), ("tail1", -2.8, -2.5), ("tail2", 2.4, -6.0), ("tail3", -2.0, -10.0))):
            c.spike(b, (x, y, 0), 3.0, 1.4, rot=(0, 0, -x * 16), sw="ice")
    elif style == "blade":
        c.spike("tail4", (0, -15.0, 0), 4.0, 2.4, rot=(180, 0, 0), sw="silver", wz=0.8)
        c.cube("tail4", (-0.4, -17.0, -0.3), (0.8, 2.0, 0.6), "glow")
    elif style == "vine":
        for k, (b, x, y, rz) in enumerate((("tail1", 3.4, -2.0, -50), ("tail2", -3.0, -6.0, 50), ("tail3", 2.6, -10.0, -50), ("tail4", -1.6, -13.4, 50))):
            c.cube(b, (x - 1.1, y - 0.4, -0.6), (2.2, 1.0, 1.2), "leaf", rot=(0, 0, rz), pivot=(x, y, 0))
    elif style == "tendril":
        c.arc("tail4", (0, -15.0, 0), 180, -130, 3, 1.6, 1.2, 0.6, "accent")
    elif style == "feather":
        for k, (b, x, y) in enumerate((("tail1", 3.0, -1.5), ("tail1", -3.0, -2.0), ("tail2", 2.4, -6.0), ("tail3", -2.0, -9.5))):
            c.spike(b, (x, y, 0), 3.4, 1.4, rot=(0, 0, 180 - x * 12), sw="feather", wz=0.5)
    else:  # smoke
        for b, x, y, z in (("tail1", 3.2, -2.0, 0.6), ("tail2", -2.8, -6.0, -0.6), ("tail3", 2.4, -10.0, 0.4)):
            c.cube(b, (x - 0.9, y - 0.9, z - 0.9), (1.8, 1.8, 1.8), "smoke")


# ================================================================ accessories
def orbit(c, kind, n=4, r=9.0, y=8.0):
    for k in range(n):
        a = 2 * math.pi * k / n
        x, z = math.cos(a) * r, math.sin(a) * r
        yy = y + 2.0 * math.sin(a * 2)
        if kind == "orbs":
            c.cube("acc", (x - 0.9, yy - 0.9, z - 0.9), (1.8, 1.8, 1.8), "glow")
        elif kind == "petals":
            c.cube("acc", (x - 1.1, yy, z - 1.1), (2.2, 0.5, 2.2), "coral" if k % 2 else "leaf", rot=(20 * k, 40 * k, 10), pivot=(x, yy, z))
        elif kind == "rings":
            c.ring("acc", (0, yy, 0), r - 3 + k, 7, 1.2, 0.4, "glow", thick=0.8, phase=k)
            return
        elif kind == "bubbles":
            c.cube("acc", (x - 0.8, yy - 0.8, z - 0.8), (1.6, 1.6, 1.6), "water")
        elif kind == "embers":
            c.cube("acc", (x - 0.6, yy - 0.6, z - 0.6), (1.2, 1.2, 1.2), "fire")
        elif kind == "shards":
            c.spike("acc", (x, yy - 1.6, z), 3.2, 1.2, rot=(20, 0, 20 * (k % 3 - 1)), sw="ice")
        elif kind == "notes":
            c.cube("acc", (x - 0.8, yy - 0.8, z - 0.8), (1.6, 1.4, 1.6), "glow")
            c.cube("acc", (x + 0.4, yy, z - 0.2), (0.4, 3.0, 0.4), "glow")
            c.cube("acc", (x + 0.4, yy + 2.4, z - 0.2), (1.4, 0.6, 0.4), "glow")
        elif kind == "boulders":
            c.cube("acc", (x - 1.3, yy - 1.3, z - 1.3), (2.6, 2.4, 2.6), "iron", rot=(20, 30, 10), pivot=(x, yy, z))
        elif kind == "sparks":
            c.cube("acc", (x - 0.3, yy - 1.4, z - 0.3), (0.6, 2.8, 0.6), "bolt", rot=(0, 0, 40 * k), pivot=(x, yy, z))
        elif kind == "leaves":
            c.cube("acc", (x - 1.1, yy, z - 0.6), (2.2, 0.5, 1.2), "leaf", rot=(10 * k, 30 * k, 20), pivot=(x, yy, z))
        elif kind == "skulls":
            c.cube("acc", (x - 1.0, yy - 0.8, z - 1.0), (2.0, 1.8, 2.0), "bone")
            c.cube("acc", (x - 0.7, yy - 0.2, z - 1.05), (0.5, 0.5, 0.2), "black")
            c.cube("acc", (x + 0.2, yy - 0.2, z - 1.05), (0.5, 0.5, 0.2), "black")
        elif kind == "feathers":
            c.spike("acc", (x, yy - 1.4, z), 3.0, 1.2, rot=(24, 0, 24 * (k % 3 - 1)), sw="white", wz=0.4)
        elif kind == "suns":
            c.cube("acc", (x - 1.0, yy - 1.0, z - 1.0), (2.0, 2.0, 2.0), "gold")
            c.cube("acc", (x - 0.5, yy - 0.5, z - 1.05), (1.0, 1.0, 0.3), "glow")
        elif kind == "runes":
            c.cube("acc", (x - 0.9, yy - 0.9, z - 0.2), (1.8, 1.8, 0.4), "glow")


def acc_orbit(kind, n=4, r=9.0, y=8.0):
    return lambda c: orbit(c, kind, n, r, y)


def acc_sun_disc(c):
    c.disc_v("back", (0, 9.0, 4.4), 10.5, 14, 2.0, 1.2, "gold")
    c.rays("back", (0, 9.0, 4.4), 10.8, 3.6, 14, 1.5, "fire", 1.0, phase=0.2)
    c.cube("back", (-3.0, 6.0, 4.0), (6.0, 6.0, 0.8), "trim")
    c.cube("back", (-1.5, 7.5, 3.6), (3.0, 3.0, 0.6), "glow")


def acc_throne(c):
    for sx in SIDES:
        c.cube("back", (sx * 5.6 - 0.9, -2.0, 3.6), (1.8, 17.0, 1.8), "gold")
        c.cube("back", (sx * 5.6 - 1.2, 14.4, 3.4), (2.4, 1.0, 2.2), "trim")
    c.arc("back", (-5.6, 15.0, 4.4), 62, -124, 7, 2.0, 1.4, 1.4, "gold", wz=1.4)
    c.cube("back", (-1.0, 18.6, 3.8), (2.0, 2.0, 2.0), "gem")
    c.cube("back", (-4.0, 1.0, 3.8), (8.0, 9.0, 0.8), "cloth")


def acc_peacock(c):
    for k in range(9):
        a = -76 + k * 19
        c.spike("back", (0, 7.0, 3.6), 11.0 - abs(k - 4) * 0.5, 1.2, rot=(0, 0, -a), sw="feather" if k % 2 else "cloth2", wz=0.5)
        r = math.radians(a)
        L = 11.0 - abs(k - 4) * 0.5
        ex, ey = math.sin(r) * (L + 0.8), 7.0 + math.cos(r) * (L + 0.8)
        c.cube("back", (ex - 1.1, ey - 1.1, 3.35), (2.2, 2.2, 0.6), "gem")
        c.cube("back", (ex - 0.5, ey - 0.5, 3.0), (1.0, 1.0, 0.5), "glow")


def acc_wheel(c):
    c.disc_v("back", (0, 7.0, 4.2), 8.5, 12, 1.8, 1.2, "wood")
    for k in range(5):
        a = math.radians(90 + k * 72)
        c.spike("back", (0, 7.0, 4.2), 8.2, 0.9, rot=(0, 0, math.degrees(a) - 90), sw="wood")
        c.cube("back", (math.cos(a) * 9.4 - 1.0, 7.0 + math.sin(a) * 9.4 - 0.6, 3.8), (2.0, 1.2, 0.8), "leaf")
    c.cube("back", (-1.0, 6.0, 3.8), (2.0, 2.0, 1.4), "gem")


def acc_moon(c):
    c.arc("back", (-4.5, 3.0, 4.4), -100, 200, 9, 3.4, 3.2, 1.2, "silver", wz=1.2)
    c.cube("back", (-1.0, 8.0, 3.8), (2.0, 2.0, 0.6), "glow")


def acc_horseshoe(c):
    c.arc("back", (-5.0, 1.0, 4.4), 0, 180, 9, 3.0, 2.4, 2.4, "iron", wz=1.4)
    for k in range(5):
        a = math.radians(-70 + k * 35)
        c.cube("back", (math.sin(a) * 5.4 - 0.4, 11.4 + math.cos(a) * 5.0 - 0.4, 3.6), (0.8, 0.8, 0.5), "glow")
    for sx in SIDES:
        c.cube("back", (sx * 6.0 - 0.3, 8.0, 4.6), (0.6, 6.0, 0.3), "fire", rot=(0, 0, sx * 6), pivot=(sx * 6.0, 8.0, 4.6))


def acc_maw(c):
    c.disc_v("back", (0, 8.0, 4.2), 8.5, 12, 1.6, 1.0, "bone")
    for k in range(12):
        a = 2 * math.pi * k / 12
        c.spike("back", (math.cos(a) * 8.0, 8.0 + math.sin(a) * 8.0, 4.2), 3.0, 1.2, rot=(0, 0, math.degrees(a) + 90), sw="white", wz=0.8)
    for k in range(3):
        c.cube("back", (-2.0 + k * 1.2 - 3, 4.0 + k * 0.8, 3.8), (6.0, 0.6, 0.4), "crimson", rot=(0, 0, -30), pivot=(0, 8, 3.8))


def acc_jaws(c):
    c.arc("back", (-6.0, 2.0, 3.6), -20, 36, 4, 3.0, 2.4, 1.6, "leaf", wz=2.0)
    c.arc("back", (6.0, 2.0, 3.6), 20, -36, 4, 3.0, 2.4, 1.6, "leaf", wz=2.0)
    for k in range(5):
        c.spike("back", (-5.0 + k * 2.5, 12.0, 3.8), 1.8, 0.8, rot=(0, 0, 180), sw="white")
    c.cube("back", (-3.0, 0.0, 3.6), (6.0, 2.0, 1.0), "leaf")


def acc_tome(c):
    for k in range(2):
        a = math.pi * k
        x, z = math.cos(a) * 9.0, math.sin(a) * 9.0
        c.cube("acc", (x - 2.2, 9.0, z - 1.6), (4.4, 0.8, 3.2), "cloth")
        c.cube("acc", (x - 1.9, 9.8, z - 1.4), (3.8, 0.4, 2.8), "white")
        c.cube("acc", (x - 0.5, 10.3, z - 0.5), (1.0, 0.3, 1.0), "glow")
    for k in range(5):
        a = k * 1.3
        c.cube("acc", (math.cos(a) * 5 - 0.8, 3.0 + k, math.sin(a) * 5 - 0.5), (1.6, 0.3, 1.0), "white", rot=(0, 20 * k, 30), pivot=(math.cos(a) * 5, 3 + k, math.sin(a) * 5))


def acc_drum(c):
    c.disc_v("back", (0, 6.0, 5.0), 6.0, 12, 2.4, 3.6, "bone")
    c.cube("back", (-4.8, 2.4, 3.4), (9.6, 7.6, 3.2), "wood")
    c.cube("back", (-4.0, 3.0, 3.1), (8.0, 6.4, 0.4), "bone")
    c.cube("back", (-1.0, 5.4, 2.8), (2.0, 2.0, 0.4), "black")
    for sx in SIDES:
        c.spike("back", (sx * 5.0, 1.0, 3.4), 10.0, 0.9, rot=(0, 0, sx * -40), sw="wood")
    for k in range(2):
        a = math.pi * (0.5 + k)
        x, z = math.cos(a) * 8.5, math.sin(a) * 8.5
        c.cube("acc", (x - 1.5, 7.0, z - 1.5), (3.0, 2.0, 3.0), "wood")
        c.cube("acc", (x - 1.3, 9.0, z - 1.3), (2.6, 0.4, 2.6), "bone")


def acc_crows(c):
    for k in range(3):
        a = 2 * math.pi * k / 3
        x, z = math.cos(a) * 9.0, math.sin(a) * 9.0
        y = 7.0 + 2.5 * k
        c.cube("acc", (x - 1.0, y - 0.8, z - 1.6), (2.0, 1.6, 3.2), "black")
        c.cube("acc", (x - 0.8, y + 0.4, z - 2.8), (1.6, 1.6, 1.6), "black")
        c.cube("acc", (x - 0.3, y + 0.5, z - 3.6), (0.6, 0.5, 1.0), "trim")
        c.cube("acc", (x - 3.2, y + 0.3, z - 0.6), (2.4, 0.4, 2.0), "shadow", rot=(0, 0, 24), pivot=(x - 0.8, y + 0.3, z))
        c.cube("acc", (x + 0.8, y + 0.3, z - 0.6), (2.4, 0.4, 2.0), "shadow", rot=(0, 0, -24), pivot=(x + 0.8, y + 0.3, z))
        c.cube("acc", (x - 0.2, y + 1.0, z - 2.7), (0.4, 0.4, 0.3), "eye")


def acc_flask(c):
    c.cube("hand_l", (3.0, -2.6, -1.8), (3.6, 3.6, 3.6), "ice")
    c.cube("hand_l", (3.4, -2.2, -1.4), (2.8, 2.0, 2.8), "fire")
    c.cube("hand_l", (4.0, 1.0, -0.8), (1.6, 2.4, 1.6), "ice")
    for k in range(3):
        c.cube("hand_l", (4.0 + (k % 2) * 1.0, 3.4 + k * 1.2, -0.6), (1.4, 1.0, 1.2), "gold")
    c.cube("back", (-3.0, 2.0, 3.6), (6.0, 6.0, 1.0), "iron")
    for k, (x, y, r) in enumerate(((-3.0, 4.0, 1.5), (3.0, 4.0, 1.5), (0.0, 8.5, 2.0))):
        c.disc_v("back", (x, y, 4.6), r, 6, 0.9, 0.9, "gold")


def acc_mirror(c):
    c.disc_v("acc", (0, 9.0, -8.0), 3.4, 10, 1.0, 1.0, "silver")
    c.cube("acc", (-2.5, 6.5, -8.2), (5.0, 5.0, 0.5), "glow")
    c.cube("acc", (-0.4, 5.0, -8.2), (0.8, 2.0, 0.6), "silver")


def acc_three_masks(c):
    for k in range(3):
        a = 2 * math.pi * k / 3
        x, z = math.cos(a) * 9.0, math.sin(a) * 9.0
        c.cube("acc", (x - 1.3, 8.0, z - 0.4), (2.6, 3.0, 0.8), ("bone", "skin", "wood")[k], rot=(0, -math.degrees(a) + 90, 0), pivot=(x, 9.5, z))
        c.cube("acc", (x - 0.6, 9.2, z - 0.9), (1.2, 0.6, 0.3), "eye", rot=(0, -math.degrees(a) + 90, 0), pivot=(x, 9.5, z))


def acc_shield(c):
    c.cube("hand_l", (3.0, -1.0, -3.6), (0.9, 8.6, 6.4), "iron")
    c.cube("hand_l", (3.5, 3.2, -1.4), (0.5, 4.8, 0.8), "glow")
    c.cube("hand_l", (3.5, 5.2, -3.4), (0.5, 0.8, 4.4), "glow")
    c.cube("hand_l", (2.9, -1.4, -3.9), (1.1, 0.8, 7.0), "metal")
    for sx in SIDES:
        c.spike("back", (sx * 2.0, 0.0, 3.6), 17.0, 1.0, rot=(0, 0, -sx * 22), sw="iron")
        c.spike("back", (sx * 2.0, 16.6, 3.6), 2.6, 1.6, rot=(0, 0, -sx * 22), sw="silver")


def acc_crab_legs(c):
    for sx in SIDES:
        for k in range(2):
            c.arc("back", (sx * 3.0, 6.0 - k * 2.0, 3.8), sx * (60 + 24 * k), sx * -70, 3, 3.6, 1.5, 1.0, "trim")
    c.disc_v("back", (0, 9.0, 4.6), 6.0, 10, 1.0, 0.8, "gold")
    for k in range(5):
        a = math.radians(90 + k * 144)
        c.cube("back", (math.cos(a) * 6 - 0.4, 9.0 + math.sin(a) * 6 - 0.4, 4.2), (0.8, 0.8, 0.8), "glow")


def acc_keys(c):
    for k in range(3):
        a = 2 * math.pi * k / 3
        x, z = math.cos(a) * 9.0, math.sin(a) * 9.0
        c.cube("acc", (x - 0.3, 6.0, z - 0.3), (0.6, 5.0, 0.6), "gold", rot=(0, 0, 30 * (k - 1)), pivot=(x, 8.5, z))
        c.disc_v("acc", (x, 11.4, z), 1.1, 6, 0.7, 0.7, "gold")
        c.cube("acc", (x + 0.2, 6.2, z - 0.3), (1.0, 0.6, 0.6), "gold")
    c.cube("acc", (-1.0, 7.0, 7.6), (2.0, 2.0, 1.0), "crimson")


def acc_quiver(c):
    c.cube("back", (-1.6, 1.0, 3.2), (3.2, 9.6, 3.2), "wood", rot=(0, 0, 14), pivot=(0, 6, 4.8))
    c.cube("back", (-1.8, 10.2, 3.0), (3.6, 0.8, 3.6), "trim", rot=(0, 0, 14), pivot=(0, 6, 4.8))
    for k in range(3):
        c.spike("back", (-1.2 + k * 1.2, 10.6, 4.8), 4.0, 0.5, rot=(0, 0, 14 + k * 4), sw="feather")
        c.spike("back", (-1.2 + k * 1.2 + 0.1, 13.6, 4.8), 1.2, 0.8, rot=(0, 0, 14 + k * 4), sw="silver")


def acc_snowflake(c):
    for k in range(3):
        c.cube("back", (-6.0, 8.6, 4.0), (12.0, 0.8, 0.8), "ice", rot=(0, 0, 60 * k), pivot=(0, 9.0, 4.4))
    for k in range(6):
        a = math.radians(30 + 60 * k)
        c.cube("back", (math.cos(a) * 5.0 - 0.4, 9.0 + math.sin(a) * 5.0 - 0.4, 4.0), (0.8, 0.8, 0.8), "glow")
    c.cube("back", (-1.0, 8.0, 4.0), (2.0, 2.0, 0.8), "glow")


def acc_pinwheel(c):
    for k in range(6):
        a = 2 * math.pi * k / 6
        c.cube("back", (-0.8, 9.0, 4.0), (1.6, 7.0, 0.6), "wind", rot=(0, 0, math.degrees(a) + 24), pivot=(0, 9.0, 4.3))
    c.cube("back", (-1.2, 7.8, 3.8), (2.4, 2.4, 1.0), "glow")


def acc_eye_ring(c):
    c.cube("back", (-2.6, 6.2, 3.8), (5.2, 5.2, 0.9), "white")
    c.cube("back", (-1.2, 7.6, 3.4), (2.4, 2.4, 0.5), "eye")
    c.cube("back", (-0.5, 8.3, 3.2), (1.0, 1.0, 0.4), "black")
    for k in range(8):
        a = 2 * math.pi * k / 8
        x, y = math.cos(a) * 7.5, 8.8 + math.sin(a) * 7.5
        c.cube("back", (x - 0.9, y - 0.7, 4.0), (1.8, 1.4, 0.6), "white")
        c.cube("back", (x - 0.35, y - 0.35, 3.7), (0.7, 0.7, 0.4), "eye")


def acc_doves(c):
    for k in range(3):
        a = 2 * math.pi * k / 3
        x, z = math.cos(a) * 9.0, math.sin(a) * 9.0
        y = 8.0 + 2.0 * k
        c.cube("acc", (x - 0.8, y - 0.6, z - 1.2), (1.6, 1.2, 2.4), "white")
        c.cube("acc", (x - 0.5, y + 0.4, z - 1.9), (1.0, 1.0, 1.0), "white")
        c.cube("acc", (x - 0.2, y + 0.6, z - 2.4), (0.4, 0.3, 0.6), "gold")
        for sx in SIDES:
            c.cube("acc", (x + sx * 0.8 - (0.0 if sx > 0 else 2.4), y + 0.2, z - 0.7), (2.4, 0.3, 1.6), "feather", rot=(0, 0, sx * 18), pivot=(x + sx * 0.8, y + 0.2, z))


def acc_star(c):
    for k in range(5):
        a = math.radians(90 + k * 72)
        c.cube("acc", (math.cos(a) * 9.0 - 0.5, 9.0 + math.sin(a) * 4.0 - 0.5, math.sin(a) * 9.0 - 0.5), (1.0, 1.0, 1.0), "glow")
    c.ring("acc", (0, 6.0, 0), 9.0, 10, 1.0, 0.5, "gold", thick=0.8)


def acc_harp(c):                                      # tuning forks (Amdusias): two small forks hanging beside the head
    for sx in SIDES:
        c.cube("acc", (sx * 9.0 - 0.3, 9.0, -0.3), (0.6, 4.4, 0.6), "silver")
        c.cube("acc", (sx * 9.0 - 1.0, 13.2, -0.3), (0.5, 3.0, 0.6), "silver")
        c.cube("acc", (sx * 9.0 + 0.5, 13.2, -0.3), (0.5, 3.0, 0.6), "silver")
        c.cube("acc", (sx * 9.0 - 0.5, 16.4, -0.2), (1.0, 1.0, 0.4), "glow")


def acc_none(c):
    pass


ACC = {
    "none": acc_none, "sun_disc": acc_sun_disc, "throne": acc_throne, "peacock": acc_peacock, "wheel": acc_wheel, "moon": acc_moon, "horseshoe": acc_horseshoe,
    "maw": acc_maw, "jaws": acc_jaws, "tome": acc_tome, "drum": acc_drum, "crows": acc_crows, "flask": acc_flask, "mirror": acc_mirror, "masks": acc_three_masks,
    "shield": acc_shield, "crab_legs": acc_crab_legs, "keys": acc_keys, "quiver": acc_quiver, "snowflake": acc_snowflake, "pinwheel": acc_pinwheel,
    "eye_ring": acc_eye_ring, "doves": acc_doves, "star": acc_star, "forks": acc_harp,
}
for _k in ("orbs", "petals", "rings", "bubbles", "embers", "shards", "notes", "boulders", "sparks", "leaves", "skulls", "feathers", "suns", "runes"):
    ACC["orbit_" + _k] = acc_orbit(_k)
