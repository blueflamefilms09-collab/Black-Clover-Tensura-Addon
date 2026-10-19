"""Celestial Spirit Magic: the GeoLite models of the spirits, the Gate and the Spirit King (see tools/geo_builder.py, docs/geo_models.md).

    python3 -B tools/celestial/gen_celestial_geo.py [id ...]          (no ids = all)
    python3 -B tools/geo_check.py $(python3 -B tools/celestial/gen_celestial_geo.py --list)

Writes assets/nusmp/geo/entity/celestial_<id>.geo.json, animations/entity/celestial_<id>.animation.json, textures/entity/celestial_<id>.png and _glow.png for
  the twelve Gold Zodiac spirits   aquarius taurus leo virgo sagittarius gemini cancer scorpio capricorn pisces aries libra
  the four silver spirits          horologium crux lyra nikora
  the Gate                         gate            clips idle, open, open_hold, close
  the Spirit King, three phases    king_p1 king_p2 king_p3   clips idle, raise, slam, point, judge, death (the same bones in every phase)
Every spirit has the clips idle (loop), summon, cast, leave on the standard skeleton (root, body, head, arm_r, arm_l, leg_r, leg_l where it has them) plus
the bones of its own props, which have their own tracks (an axe swinging, chains swaying, a tail lashing, a pendulum ...). Spirits are built at 32 px (two
blocks) tall; the prop scales them. Box UV, pivots on the joints, an emissive glow map for everything that shines. Deterministic.
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, ".."))
from celestial_geo_lib import B, clips, face, eyes, disc_decal, keyhole  # noqa: E402
from geo_builder import Anim  # noqa: E402

GOLD_GLOW = (255, 214, 110)


# ================================================================== the shared humanoid
def skeleton(b, legs=True):
    b.bone("root")
    b.bone("body", "root", (0, 12, 0))
    b.bone("head", "body", (0, 24, 0))
    b.bone("arm_r", "body", (-5, 22, 0))
    b.bone("arm_l", "body", (5, 22, 0))
    if legs:
        b.bone("leg_r", "root", (-2, 12, 0))
        b.bone("leg_l", "root", (2, 12, 0))


def humanoid(b, skin="skin", top="cloth_white", bottom="cloth_black", shoes="cloth_black", sleeves=None, hand=None, face_fn=None, legs=True, wide=0, long_legs=False):
    """The body: torso, arms (sleeve + hand), legs (trouser + shoe), head with a face. `wide` adds pixels to the torso and arm width."""
    skeleton(b, legs)
    sleeves = sleeves or top
    hand = hand or skin
    tw = 8 + wide
    b.cube("body", (-tw / 2, 12, -2), (tw, 12, 4), top)
    aw = 3 + (wide // 2)
    b.cube("arm_r", (-4 - aw - wide / 2, 15, -2), (aw, 9, 4), sleeves)
    b.cube("arm_r", (-4 - aw - wide / 2, 12, -2), (aw, 3, 4), hand)
    b.cube("arm_l", (4 + wide / 2, 15, -2), (aw, 9, 4), sleeves)
    b.cube("arm_l", (4 + wide / 2, 12, -2), (aw, 3, 4), hand)
    if legs:
        for side, x in (("leg_r", -4), ("leg_l", 0)):
            b.cube(side, (x, 3, -2), (4, 9, 4), bottom)
            b.cube(side, (x, 0, -2), (4, 3, 4), shoes)
    b.cube("head", (-4, 24, -4), (8, 8, 8), skin, decal=("front", face_fn or face()))


def hair(b, mat, back=8, sides=True, fringe=True, bun=False, twin=False):
    b.cube("head", (-4.5, 31, -4.5), (9, 2, 9), mat)                       # the crown
    if fringe:
        b.cube("head", (-4.5, 29, -4.5), (9, 2, 1), mat)
    if sides:
        b.cube("head", (-4.75, 24 + 2, -4.5), (1, 7, 9), mat)
        b.cube("head", (3.75, 24 + 2, -4.5), (1, 7, 9), mat)
    b.cube("head", (-4.5, 32 - 1 - back, 3.5), (9, back, 2), mat)           # the back
    if bun:
        b.cube("head", (-2, 33, 0), (4, 3, 4), mat)
    if twin:
        b.cube("head", (-7, 26, 1), (3, 8, 3), mat)
        b.cube("head", (4, 26, 1), (3, 8, 3), mat)


def stand_extras(a, bones, tracks):
    for clip, bone, kind, frames in tracks:
        if bone in bones:
            getattr(a, kind)(clip, bone, frames)


def new(name):
    b = B("celestial", name)
    return b


def save(b, extras=None, bones=None):
    a = Anim()
    names = set(b.m.bones.keys())
    clips(a, names, extras)
    return b.save(a)


# ================================================================== the twelve
def aquarius():
    b = new("aquarius")
    skeleton(b, legs=False)
    b.cube("body", (-4, 12, -2), (8, 12, 4), "skin_pale")
    b.cube("body", (-4.5, 17, -2.5), (9, 4, 5), "cloth_blue", glow=((90, 170, 255), 0.35))           # the bikini top
    b.cube("arm_r", (-8, 15, -2), (3, 9, 4), "skin_pale"); b.cube("arm_r", (-8, 12, -2), (3, 3, 4), "skin_pale")
    b.cube("arm_l", (5, 15, -2), (3, 9, 4), "skin_pale"); b.cube("arm_l", (5, 12, -2), (3, 3, 4), "skin_pale")
    b.cube("head", (-4, 24, -4), (8, 8, 8), "skin_pale", decal=("front", face(eye_color=(40, 90, 170), mouth=(210, 110, 130), blush=True)))
    hair(b, "hair_blue", back=14, twin=True)
    b.bone("tail1", "root", (0, 12, 0)); b.bone("tail2", "tail1", (0, 5, 0)); b.bone("tail3", "tail2", (0, -2, 0)); b.bone("fin", "tail3", (0, -8, 0))
    b.cube("tail1", (-4, 5, -2.5), (8, 7, 5), "scale_teal", glow=((60, 220, 200), 0.18))
    b.cube("tail2", (-3.5, -2, -2), (7, 7, 4), "scale_teal")
    b.cube("tail3", (-3, -8, -1.5), (6, 6, 3), "scale_blue")
    b.cube("fin", (-7, -13, -0.5), (14, 6, 1), "scale_blue", glow=((110, 200, 255), 0.5))
    b.bone("urn", "arm_r", (-6.5, 12, -3))
    b.cube("urn", (-11, 8, -6), (8, 10, 8), "ceramic", glow=((90, 180, 255), 0.25))
    b.cube("urn", (-10, 18, -5), (6, 2, 6), "ceramic")
    b.cube("urn", (-9, 4, -5), (6, 4, 6), "glass", glow=((140, 220, 255), 0.85))                     # water pouring
    b.cube("urn", (-8.5, -6, -4.5), (5, 10, 5), "glass", glow=((140, 220, 255), 0.6))

    def ex(a):
        a.rot("idle", "tail1", {0.0: (0, 0, 4), 1.0: (0, 0, -4), 2.0: (0, 0, 4)})
        a.rot("idle", "tail2", {0.0: (0, 0, -6), 1.0: (0, 0, 6), 2.0: (0, 0, -6)})
        a.rot("idle", "tail3", {0.0: (0, 0, 8), 1.0: (0, 0, -8), 2.0: (0, 0, 8)})
        a.rot("idle", "fin", {0.0: (0, 0, -12), 1.0: (0, 0, 12), 2.0: (0, 0, -12)})
        a.rot("cast", "urn", {0.0: (0, 0, 0), 0.3: (-20, 0, 0), 0.5: (95, 0, -10), 0.9: (100, 0, -10), 1.2: (0, 0, 0)})
        a.rot("cast", "tail2", {0.0: (0, 0, 0), 0.5: (-20, 0, 0), 1.2: (0, 0, 0)})
        a.rot("summon", "fin", {0.0: (-60, 0, 0), 0.7: (0, 0, 0)})
        a.rot("leave", "fin", {0.0: (0, 0, 0), 0.7: (50, 0, 0)})
    b.lift(14)                                                                                             # the tail hangs below where the feet would be: she floats
    return save(b, ex)


def taurus():
    b = new("taurus")
    humanoid(b, skin="skin_bull", top="skin_bull", bottom="cloth_brown", shoes="black", sleeves="skin_bull", wide=4, face_fn=face(eye_color=(30, 20, 20), mouth=(120, 50, 50)))
    b.cube("body", (-6, 12, -2.6), (12, 5, 5), "cloth_brown")                                          # a belt of hide
    b.cube("head", (-5, 22, -5), (10, 4, 10), "skin_bull")                                             # the heavy jaw
    b.cube("head", (-2, 24, -5.4), (4, 3, 2), "skin_pale", glow=((255, 220, 120), 0.4))                # the muzzle with its golden ring
    b.cube("head", (-1, 24.5, -6.2), (2, 2, 1), "gold", glow=((255, 220, 120), 0.9))
    for sgn in (-1, 1):                                                                                # the horns
        x = 4.5 * sgn
        b.cube("head", (x - (1.5 if sgn > 0 else 3.5) , 30, -1), (2, 3, 2), "horn", rotation=(0, 0, -25 * sgn), pivot=(x, 31, 0))
        b.cube("head", (x * 1.5 - (1 if sgn > 0 else 1), 33, -1), (2, 4, 2), "horn", rotation=(0, 0, -55 * sgn), pivot=(x * 1.5, 33, 0))
    b.cube("head", (-4.5, 31, -4.5), (9, 2, 9), "hair_black")
    b.bone("axe", "arm_r", (-8, 12, -3))
    b.cube("axe", (-9, -2, -3), (2, 30, 2), "wood_dark")                                                # the haft
    b.cube("axe", (-9, 26, -8), (2, 10, 12), "iron", glow=((255, 210, 110), 0.25))                      # the double head
    b.cube("axe", (-9.5, 24, -9), (3, 14, 2), "gold", glow=((255, 210, 110), 0.7))
    b.cube("axe", (-9.5, 24, 3), (3, 14, 2), "gold", glow=((255, 210, 110), 0.7))

    def ex(a):
        a.rot("idle", "axe", {0.0: (4, 0, 0), 1.0: (8, 0, 0), 2.0: (4, 0, 0)})
        a.rot("cast", "axe", {0.0: (4, 0, 0), 0.3: (-110, 0, 0), 0.5: (30, 0, 0), 0.9: (35, 0, 0), 1.2: (4, 0, 0)})
        a.rot("cast", "arm_r", {0.0: (0, 0, 2), 0.3: (-60, 0, 0), 0.5: (95, 0, 0), 0.9: (95, 0, 0), 1.2: (0, 0, 2)})
        a.pos("cast", "root", {0.0: (0, 0, 0), 0.5: (0, -2, -3), 0.9: (0, 0, -1), 1.2: (0, 0, 0)})
    return save(b, ex)


def leo():
    b = new("leo")
    humanoid(b, skin="skin", top="cloth_black", bottom="cloth_black", shoes="black", sleeves="cloth_black", face_fn=face(eye_color=(200, 130, 40), glow=(255, 200, 90), glasses=True))
    b.cube("body", (-1, 12.5, -2.3), (2, 11, 1), "cloth_white")                                          # the shirt front
    b.cube("body", (-0.5, 21, -2.4), (1, 3, 1), "cloth_red", glow=((255, 120, 90), 0.3))               # the tie
    hair(b, "hair_orange", back=4, sides=True)
    for i, (x, z, h) in enumerate(((-3, -2, 3), (-1, -3, 4), (1, -2, 4), (3, -1, 3), (0, 0, 4))):     # a lion's spiky mane
        b.cube("head", (x - 1, 33, z - 1), (2, h, 2), "hair_orange")
    b.bone("regulus", "arm_l", (6.5, 12, 0))
    b.cube("regulus", (5, 11, -2.5), (3, 1, 5), "gold", glow=((255, 230, 130), 0.9))                 # the ring Regulus on the hand
    b.cube("regulus", (5.5, 10, -1), (2, 1, 2), "star", glow=((255, 250, 210), 1.0))
    b.bone("lion", "root", (0, 20, 6))                                                                  # a halo of light behind him, a lion's mane in light
    b.cube("lion", (-9, 14, 5), (18, 18, 1), "star", glow=((255, 226, 120), 0.55))
    b.cube("lion", (-6, 17, 4.5), (12, 12, 1), "gold", glow=((255, 240, 170), 0.8))

    def ex(a):
        a.rot("idle", "lion", {0.0: (0, 0, 0), 1.0: (0, 0, 20), 2.0: (0, 0, 40)})
        a.rot("cast", "lion", {0.0: (0, 0, 0), 0.6: (0, 0, 180), 1.2: (0, 0, 360)})
        a.rot("cast", "regulus", {0.0: (0, 0, 0), 0.5: (0, 90, 0), 1.2: (0, 360, 0)})
    return save(b, ex)


def virgo():
    b = new("virgo")
    humanoid(b, skin="skin_pale", top="cloth_black", bottom="cloth_black", shoes="black", sleeves="cloth_black", hand="cloth_white", legs=True,
             face_fn=face(eye_color=(60, 40, 70), mouth=(210, 120, 140), blush=True))
    b.cube("body", (-3.5, 13, -2.6), (7, 10, 1), "cloth_white")                                          # the apron
    b.cube("body", (-4.5, 8, -2.5), (9, 5, 5), "cloth_black")                                            # the skirt
    b.cube("body", (-4.5, 6, -2.5), (9, 2, 5), "cloth_white")                                            # its white hem
    hair(b, "hair_pink", back=6, bun=True)
    b.cube("head", (-4.5, 32, -4), (9, 1, 3), "cloth_white")                                              # the maid's headband
    for side, x, bn in (("r", -1, "chain_r"), ("l", 1, "chain_l")):
        b.bone(bn, "arm_" + side, (-6.5 if side == "r" else 6.5, 12, 0))
        sx = -6.5 if side == "r" else 6.5
        for i in range(4):
            b.cube(bn, (sx - 0.5 + (i % 2) * 0.5, 11 - i * 3, -0.5), (2, 3, 2), "silver", glow=((255, 150, 200), 0.25 + 0.15 * i))
        b.cube(bn, (sx - 1.5, -2, -1.5), (4, 3, 4), "gold", glow=((255, 150, 200), 0.9))                  # the cuff and the hook

    def ex(a):
        for bn, s in (("chain_r", 1), ("chain_l", -1)):
            a.rot("idle", bn, {0.0: (4 * s, 0, 3), 1.0: (-4 * s, 0, -3), 2.0: (4 * s, 0, 3)})
            a.rot("cast", bn, {0.0: (0, 0, 0), 0.3: (-60, 0, 0), 0.6: (100, 0, 0), 1.0: (60, 0, 0), 1.2: (0, 0, 0)})
    return save(b, ex)


def sagittarius():
    b = new("sagittarius")
    b.bone("root"); b.bone("body", "root", (0, 20, -6)); b.bone("head", "body", (0, 32, -6)); b.bone("arm_r", "body", (-5, 30, -6)); b.bone("arm_l", "body", (5, 30, -6))
    b.bone("horse", "root", (0, 12, 0))
    for nm, x, z in (("leg_fr", -3, -9), ("leg_fl", 1, -9), ("leg_br", -3, 7), ("leg_bl", 1, 7)):
        b.bone(nm, "root", (x + 1, 12, z + 1))
        b.cube(nm, (x, 3, z), (2, 9, 2), "cloth_brown"); b.cube(nm, (x, 0, z), (2, 3, 2), "black")
    b.cube("horse", (-4, 11, -9), (8, 9, 22), "cloth_brown")                                             # the horse's barrel
    b.cube("horse", (-3, 12, 12), (6, 5, 3), "hair_brown")                                               # the tail
    b.bone("tail", "horse", (0, 18, 13)); b.cube("tail", (-1.5, 6, 13), (3, 12, 2), "hair_brown")
    b.cube("body", (-4, 20, -8), (8, 12, 4), "cloth_tan")                                                # the human torso, forward and up
    b.cube("arm_r", (-8, 23, -8), (3, 9, 4), "cloth_tan"); b.cube("arm_r", (-8, 20, -8), (3, 3, 4), "skin_tan")
    b.cube("arm_l", (5, 23, -8), (3, 9, 4), "cloth_tan"); b.cube("arm_l", (5, 20, -8), (3, 3, 4), "skin_tan")
    b.cube("head", (-4, 32, -10), (8, 8, 8), "skin_tan", decal=("front", face(eye_color=(60, 30, 20), mouth=(150, 70, 60))))
    b.cube("head", (-4.5, 39, -10.5), (9, 3, 9), "cloth_brown")                                          # a horse-hair cap and mane
    b.cube("head", (-1, 33, -2.5), (2, 9, 2), "hair_brown")
    b.cube("head", (-1.5, 31, -13), (3, 2, 4), "cloth_brown")                                            # a horse's nose piece strapped on
    b.bone("bow", "arm_l", (6.5, 20, -8))
    b.cube("bow", (6, 12, -9), (1, 18, 1), "wood")
    b.cube("bow", (6, 28, -11), (1, 4, 2), "wood"); b.cube("bow", (6, 10, -11), (1, 4, 2), "wood")
    b.cube("bow", (6.2, 10, -12), (1, 22, 1), "star", glow=((255, 230, 150), 0.9))            # the string
    b.bone("arrow", "arm_r", (-6.5, 21, -8)); b.cube("arrow", (-7.5, 19, -18), (1, 1, 16), "wood"); b.cube("arrow", (-7.6, 18.8, -20), (1, 1, 3), "gold", glow=((255, 230, 150), 0.9))

    def ex(a):
        a.rot("idle", "tail", {0.0: (6, 0, 0), 1.0: (-6, 0, 0), 2.0: (6, 0, 0)})
        a.rot("idle", "leg_fr", {0.0: (0, 0, 0), 1.0: (8, 0, 0), 2.0: (0, 0, 0)})
        a.rot("cast", "bow", {0.0: (0, 0, 0), 0.4: (-70, 0, 0), 0.9: (-80, 0, 0), 1.2: (0, 0, 0)})
        a.rot("cast", "arrow", {0.0: (0, 0, 0), 0.4: (-60, 0, 0), 0.9: (-80, 0, 0), 1.2: (0, 0, 0)})
        a.pos("cast", "arrow", {0.0: (0, 0, 0), 0.6: (0, 0, 2), 0.7: (0, 0, -14), 1.2: (0, 0, -14)})
        a.rot("summon", "leg_fl", {0.0: (-30, 0, 0), 0.7: (0, 0, 0)})
    return save(b, ex)


def gemini():
    b = new("gemini")
    b.bone("root")
    for tw, x, skin, hairm, top, accent, eye in (("a", -8, "skin", "hair_blue", "cloth_blue", (120, 180, 255), (60, 100, 200)), ("b", 8, "skin", "hair_red", "cloth_red", (255, 130, 120), (200, 70, 60))):
        p = "twin_" + tw
        b.bone(p, "root", (x, 2, 0))
        b.bone("body_" + tw, p, (x, 11, 0)); b.bone("head_" + tw, "body_" + tw, (x, 18, 0)); b.bone("arm_" + tw + "r", "body_" + tw, (x - 4, 17, 0)); b.bone("arm_" + tw + "l", "body_" + tw, (x + 4, 17, 0))
        b.cube("body_" + tw, (x - 3, 8, -1.5), (6, 9, 3), top, glow=(accent, 0.18))
        b.cube("body_" + tw, (x - 3, 4, -1.5), (6, 4, 3), "cloth_white")                                      # short trousers
        b.cube("body_" + tw, (x - 2.5, 2, -1.5), (2, 2, 3), skin); b.cube("body_" + tw, (x + 0.5, 2, -1.5), (2, 2, 3), skin)
        b.cube("arm_" + tw + "r", (x - 6, 10, -1.5), (2, 6, 3), top); b.cube("arm_" + tw + "l", (x + 4, 10, -1.5), (2, 6, 3), top)
        b.cube("head_" + tw, (x - 3.5, 18, -3.5), (7, 7, 7), skin, decal=("front", face(eye_color=eye, mouth=(210, 100, 110), blush=True, y=2)))
        b.cube("head_" + tw, (x - 4, 24, -4), (8, 2, 8), hairm); b.cube("head_" + tw, (x - 4, 19, 3), (8, 6, 2), hairm)
        b.cube("head_" + tw, (x - 1, 26, -1), (2, 2, 2), "star", glow=(accent, 1.0))                          # a star on the cap
    b.bone("link", "root", (0, 14, 0)); b.cube("link", (-5, 13.5, -0.5), (10, 1, 1), "star", glow=((190, 210, 255), 0.9))   # the bond between the twins

    def ex(a):
        a.pos("idle", "twin_a", {0.0: (0, 0, 0), 1.0: (0, 1.5, 0), 2.0: (0, 0, 0)})
        a.pos("idle", "twin_b", {0.0: (0, 1.5, 0), 1.0: (0, 0, 0), 2.0: (0, 1.5, 0)})
        a.rot("cast", "twin_a", {0.0: (0, 0, 0), 0.5: (0, 90, 0), 1.2: (0, 360, 0)})
        a.rot("cast", "twin_b", {0.0: (0, 0, 0), 0.5: (0, -90, 0), 1.2: (0, -360, 0)})
        a.scale("cast", "link", {0.0: (1, 1, 1), 0.5: (1.6, 1, 1), 1.2: (1, 1, 1)})
    return save(b, ex)


def cancer():
    b = new("cancer")
    humanoid(b, skin="skin_tan", top="cloth_white", bottom="cloth_black", shoes="black", sleeves="cloth_white", face_fn=face(eye_color=(40, 30, 40), mouth=(160, 80, 80), glasses=True))
    hair(b, "hair_white", back=3, fringe=False)
    b.cube("head", (-5, 33, -3), (10, 3, 6), "hair_white")                                                # the big pompadour
    b.cube("body", (-6, 13, 2), (12, 12, 4), "shell", glow=((255, 120, 90), 0.12))                        # the crab shell on his back
    b.cube("body", (-5, 24, 2.5), (10, 2, 3), "shell")
    b.bone("claw_r", "body", (-6, 22, 4)); b.bone("claw_l", "body", (6, 22, 4))
    for bn, s in (("claw_r", -1), ("claw_l", 1)):
        x = 6 * s
        b.cube(bn, (x - 1.5, 20, 3), (3, 8, 3), "shell")
        b.cube(bn, (x - 3 + (0 if s < 0 else 0), 27, 2.5), (6, 6, 4), "shell", glow=((255, 140, 110), 0.2))
        b.cube(bn, (x - (3 if s > 0 else 1), 33, 3), (2, 5, 2), "silver", glow=((255, 220, 200), 0.5))
    b.bone("shears", "arm_r", (-6.5, 12, 0))                                                              # the hairdresser's scissors
    b.cube("shears", (-8, 4, -3), (1, 11, 1), "silver", rotation=(0, 0, 8), pivot=(-7.5, 12, 0))
    b.cube("shears", (-7, 4, -3), (1, 11, 1), "silver", rotation=(0, 0, -8), pivot=(-7.5, 12, 0), glow=((255, 230, 230), 0.4))
    b.cube("shears", (-9.5, 10, -3.5), (4, 3, 2), "gold")
    b.bone("leg_crab", "root", (0, 8, 4))
    for i in range(3):
        for s in (-1, 1):
            b.cube("leg_crab", (s * (5 + i) - 0.5, 8 - i * 2, 3 + i * 1.5), (1, 5, 1), "shell")

    def ex(a):
        a.rot("idle", "claw_r", {0.0: (0, 0, 0), 1.0: (0, 0, 10), 2.0: (0, 0, 0)})
        a.rot("idle", "claw_l", {0.0: (0, 0, 0), 1.0: (0, 0, -10), 2.0: (0, 0, 0)})
        a.rot("idle", "shears", {0.0: (0, 0, 0), 0.5: (0, 0, 14), 1.0: (0, 0, 0), 1.5: (0, 0, 14), 2.0: (0, 0, 0)})
        if True:
            a.rot("cast", "shears", {0.0: (0, 0, 0), 0.2: (50, 0, 20), 0.4: (60, 0, -20), 0.6: (50, 0, 20), 0.8: (60, 0, -20), 1.0: (30, 0, 0), 1.2: (0, 0, 0)})
        a.rot("cast", "claw_r", {0.0: (0, 0, 0), 0.4: (-40, 0, 40), 1.2: (0, 0, 0)})
        a.rot("cast", "claw_l", {0.0: (0, 0, 0), 0.4: (-40, 0, -40), 1.2: (0, 0, 0)})
    return save(b, ex)


def scorpio():
    b = new("scorpio")
    humanoid(b, skin="skin_tan", top="cloth_tan", bottom="cloth_brown", shoes="cloth_brown", sleeves="cloth_tan", wide=2, face_fn=face(eye_color=(40, 30, 20), mouth=(140, 70, 60)))
    hair(b, "hair_red", back=3)
    for x, h in ((-3, 5), (0, 6), (3, 5), (-1.5, 4), (1.5, 4)):
        b.cube("head", (x - 1, 33, -2), (2, h, 2), "hair_red")
    b.cube("body", (-6, 20, -2.5), (12, 5, 5), "cloth_brown")                                             # a sand-coloured mantle on the shoulders
    b.cube("body", (-4.5, 17, 2.2), (9, 8, 1), "cloth_brown")
    segs = [("tail1", (0, 14, 4), (-2, 14, 3), (4, 6, 4)), ("tail2", (0, 21, 8), (-1.5, 20, 7), (3, 6, 3)), ("tail3", (0, 26, 8), (-1.5, 26, 5), (3, 3, 6)), ("tail4", (0, 28, 2), (-1.5, 28, -1), (3, 3, 5))]
    parent = "body"
    pivots = [(0, 14, 4), (0, 19, 6), (0, 26, 8), (0, 29, 4)]
    for (nm, _pv, org, size), pv in zip(segs, pivots):
        b.bone(nm, parent, pv)
        b.cube(nm, org, size, "scale_orange", glow=((255, 180, 80), 0.2))
        parent = nm
    b.cube("tail4", (-1, 29, -5), (2, 3, 3), "gold", glow=((255, 240, 160), 1.0))                          # the stinger
    b.bone("sand", "root", (0, 2, 0))
    for i in range(6):
        b.cube("sand", (-7 + i * 2.6, 1 + (i % 2) * 6, -6 + (i % 3) * 4), (2, 2, 2), "sand", glow=((255, 220, 140), 0.45))

    def ex(a):
        a.rot("idle", "tail1", {0.0: (0, 0, 0), 1.0: (8, 0, 0), 2.0: (0, 0, 0)})
        a.rot("idle", "tail2", {0.0: (0, 0, 0), 1.0: (-10, 0, 0), 2.0: (0, 0, 0)})
        a.rot("idle", "tail4", {0.0: (0, 0, 0), 1.0: (14, 0, 0), 2.0: (0, 0, 0)})
        a.rot("cast", "tail1", {0.0: (0, 0, 0), 0.4: (-40, 0, 0), 0.7: (30, 0, 0), 1.2: (0, 0, 0)})
        a.rot("cast", "tail3", {0.0: (0, 0, 0), 0.4: (-50, 0, 0), 0.7: (40, 0, 0), 1.2: (0, 0, 0)})
        a.rot("idle", "sand", {0.0: (0, 0, 0), 2.0: (0, 360, 0)})
        a.rot("cast", "sand", {0.0: (0, 0, 0), 1.2: (0, 720, 0)})
        a.scale("cast", "sand", {0.0: (0.6, 0.6, 0.6), 0.5: (2.4, 1.6, 2.4), 1.2: (1, 1, 1)})
    return save(b, ex)


def capricorn():
    b = new("capricorn")
    humanoid(b, skin="skin_goat", top="cloth_black", bottom="cloth_black", shoes="black", sleeves="cloth_black", hand="cloth_white", wide=2, face_fn=face(eye_color=(200, 160, 40), mouth=(120, 90, 80)))
    b.cube("body", (-1.5, 12.5, -2.3), (3, 11, 1), "cloth_white")                                          # shirt front with a waistcoat edge
    b.cube("body", (-0.5, 21, -2.5), (1, 3, 1), "gold", glow=((255, 220, 120), 0.4))
    b.cube("body", (-5, 8, 2), (10, 8, 2), "cloth_black")                                                  # the tailcoat tails
    b.cube("head", (-3, 26, -6), (6, 4, 3), "skin_goat")                                                   # the long goat muzzle
    b.cube("head", (-2, 21, -6), (4, 6, 2), "hair_white")                                                  # the beard
    for sgn in (-1, 1):
        b.cube("head", (sgn * 3.5 - 1, 32, -1), (2, 3, 2), "horn")
        b.cube("head", (sgn * 5 - 1, 34, -1.5), (2, 3, 3), "horn", rotation=(0, 0, -30 * sgn), pivot=(sgn * 4.5, 34, 0))
        b.cube("head", (sgn * 7 - 1, 33, -3), (2, 2, 4), "horn", rotation=(25, 0, -50 * sgn), pivot=(sgn * 6, 34, -1))
        b.cube("head", (sgn * 5 - 1, 28, 0), (2, 3, 1), "skin_goat")                                       # the ears
    b.bone("horns", "head", (0, 32, 0))
    b.cube("horns", (-1, 36, -1), (2, 2, 2), "gold", glow=((255, 220, 120), 0.9))

    def ex(a):
        a.rot("idle", "horns", {0.0: (0, 0, 0), 1.0: (0, 12, 0), 2.0: (0, 0, 0)})
        a.rot("cast", "head", {0.0: (0, 0, 0), 0.3: (30, 0, 0), 0.6: (-5, 0, 0), 1.2: (0, 0, 0)})
        a.rot("cast", "body", {0.0: (0, 0, 0), 0.3: (-12, 0, 0), 0.6: (35, 0, 0), 1.2: (0, 0, 0)})
        a.pos("cast", "root", {0.0: (0, 0, 0), 0.4: (0, 0, 2), 0.6: (0, 0, -6), 1.2: (0, 0, 0)})
    return save(b, ex)


def pisces():
    b = new("pisces")
    b.bone("root"); b.bone("body", "root", (0, 12, 0)); b.bone("head", "body", (0, 20, 0))
    b.cube("body", (-3, 8, -3), (6, 8, 6), "glass", glow=((120, 230, 220), 0.7))                          # a bubble of water at the heart
    b.cube("body", (-2, 9, -2), (4, 6, 4), "star", glow=((220, 255, 250), 1.0))
    b.cube("head", (-3.5, 18, -3.5), (7, 7, 7), "skin_pale", decal=("front", face(eye_color=(40, 120, 150), mouth=(230, 150, 160), blush=True, y=2)))
    b.cube("head", (-4, 24, -4), (8, 2, 8), "hair_blue"); b.cube("head", (-4, 18, 3), (8, 7, 2), "hair_blue")
    b.cube("body", (-4, 12, -2.5), (8, 6, 5), "cloth_white", glow=((150, 240, 230), 0.15))               # a small robe
    for tw, col, glow in (("a", "scale_orange", (255, 180, 100)), ("b", "scale_teal", (110, 240, 220))):
        b.bone("orbit_" + tw, "root", (0, 16, 0)); b.bone("fish_" + tw, "orbit_" + tw, (0, 16, -14 if tw == "a" else 14)); b.bone("ftail_" + tw, "fish_" + tw, (0, 16, -8 if tw == "a" else 8))
        z = -14 if tw == "a" else 14
        sgn = 1 if tw == "a" else -1
        b.cube("fish_" + tw, (-2.5, 13, z - 6), (5, 7, 12), col, glow=(glow, 0.2))
        b.cube("fish_" + tw, (-2, 14, z - 6 - 1 * sgn), (4, 5, 2), col)
        b.cube("fish_" + tw, (-0.5, 20, z - 4), (1, 3, 7), col, glow=(glow, 0.5))                         # the dorsal fin
        b.cube("fish_" + tw, (-3.5, 14, z - 7 if tw == "a" else z + 3), (1, 1, 1), "black")
        b.cube("ftail_" + tw, (-0.5, 12, (z + 6 if tw == "a" else z - 8) - 0), (1, 11, 5), col, glow=(glow, 0.6))
        b.cube("fish_" + tw, (2.5, 12, z - 2), (3, 1, 4), col); b.cube("fish_" + tw, (-5.5, 12, z - 2), (3, 1, 4), col)

    def ex(a):
        a.rot("idle", "orbit_a", {0.0: (0, 0, 0), 2.0: (0, 360, 0)})
        a.rot("idle", "orbit_b", {0.0: (0, 0, 0), 2.0: (0, 360, 0)})
        a.rot("idle", "ftail_a", {0.0: (0, 18, 0), 0.5: (0, -18, 0), 1.0: (0, 18, 0), 1.5: (0, -18, 0), 2.0: (0, 18, 0)})
        a.rot("idle", "ftail_b", {0.0: (0, -18, 0), 0.5: (0, 18, 0), 1.0: (0, -18, 0), 1.5: (0, 18, 0), 2.0: (0, -18, 0)})
        a.rot("cast", "orbit_a", {0.0: (0, 0, 0), 1.2: (0, 720, 0)})
        a.rot("cast", "orbit_b", {0.0: (0, 0, 0), 1.2: (0, 720, 0)})
        a.scale("cast", "orbit_a", {0.0: (1, 1, 1), 0.6: (1.5, 1, 1.5), 1.2: (1, 1, 1)})
        a.scale("cast", "orbit_b", {0.0: (1, 1, 1), 0.6: (1.5, 1, 1.5), 1.2: (1, 1, 1)})
    return save(b, ex)


def aries():
    b = new("aries")
    humanoid(b, skin="skin_pale", top="cloth_pink", bottom="cloth_pink", shoes="cloth_white", sleeves="cloth_pink", face_fn=face(eye_color=(120, 70, 110), mouth=(230, 130, 150), blush=True))
    hair(b, "hair_pink", back=6, twin=False)
    b.bone("wool", "body", (0, 18, 0))
    for (x, y, z, s) in ((-6, 22, -3, 4), (4, 22, -3, 4), (-6, 14, -3, 5), (3, 14, -3, 5), (-3, 17, -4, 6), (-5, 12, 2, 5), (1, 12, 2, 5), (-3, 20, 2, 6), (-2, 6, -3, 5), (-6, 8, 0, 4), (3, 8, 0, 4)):
        b.cube("wool", (x, y, z), (s, s, s), "wool", glow=((255, 220, 240), 0.12))
    b.cube("head", (-5, 30, -4), (10, 3, 8), "wool")                                                       # a woolly fringe
    for sgn in (-1, 1):                                                                                    # curled ram's horns
        b.cube("head", (sgn * 5 - 1, 29, -1), (2, 4, 3), "horn")
        b.cube("head", (sgn * 6.5 - 1, 31, -2), (3, 2, 4), "horn", rotation=(0, 0, -20 * sgn), pivot=(sgn * 6, 32, 0))
        b.cube("head", (sgn * 8 - 1, 28, -2), (2, 4, 4), "horn")
    b.bone("tailw", "root", (0, 8, 4)); b.cube("tailw", (-2, 6, 3), (4, 4, 4), "wool")

    def ex(a):
        a.scale("idle", "wool", {0.0: (1, 1, 1), 1.0: (1.06, 1.04, 1.06), 2.0: (1, 1, 1)})
        a.scale("cast", "wool", {0.0: (1, 1, 1), 0.5: (1.5, 1.4, 1.5), 0.9: (1.5, 1.4, 1.5), 1.2: (1, 1, 1)})
        a.rot("idle", "tailw", {0.0: (0, 0, 6), 1.0: (0, 0, -6), 2.0: (0, 0, 6)})
    return save(b, ex)


def libra():
    b = new("libra")
    humanoid(b, skin="skin", top="cloth_lilac", bottom="cloth_lilac", shoes="cloth_gold", sleeves="cloth_lilac", face_fn=face(eye_color=(90, 80, 180), mouth=(200, 110, 130), glow=(200, 190, 255)))
    b.cube("body", (-4.5, 6, -2.5), (9, 6, 5), "cloth_lilac")                                              # a toga skirt
    b.cube("body", (-4.5, 21, -2.6), (9, 2, 5.2), "gold", glow=((255, 230, 140), 0.5))                    # the golden sash
    hair(b, "hair_purple", back=9, bun=True)
    b.cube("head", (-4.5, 32, -4.5), (9, 1, 2), "gold", glow=((255, 230, 140), 0.7))                      # the tiara
    b.cube("head", (-1, 33, -4.8), (2, 2, 1), "star", glow=((200, 190, 255), 1.0))
    b.bone("scales", "root", (0, 38, 0))                                                                   # the scales float above her hands
    b.cube("scales", (-12, 37, -0.5), (24, 1, 1), "gold", glow=((255, 230, 140), 0.4))
    b.cube("scales", (-1, 30, -0.5), (2, 8, 1), "gold")
    for s, nm in ((-1, "pan_l"), (1, "pan_r")):
        b.bone(nm, "scales", (s * 11, 37, 0))
        b.cube(nm, (s * 11 - 0.5, 29, -0.5), (1, 8, 1), "silver")
        b.cube(nm, (s * 11 - 4, 27, -4), (8, 1, 8), "gold", glow=((255, 230, 140), 0.6))
        b.cube(nm, (s * 11 - 2, 28, -2), (4, 3, 4), "glass", glow=((190, 180, 255), 0.9))

    def ex(a):
        a.rot("idle", "scales", {0.0: (0, 0, -8), 1.0: (0, 0, 8), 2.0: (0, 0, -8)})
        a.rot("idle", "pan_l", {0.0: (0, 0, 8), 1.0: (0, 0, -8), 2.0: (0, 0, 8)})
        a.rot("idle", "pan_r", {0.0: (0, 0, 8), 1.0: (0, 0, -8), 2.0: (0, 0, 8)})
        a.rot("cast", "scales", {0.0: (0, 0, 0), 0.5: (0, 360, -25), 1.2: (0, 720, 0)})
        a.pos("cast", "scales", {0.0: (0, 0, 0), 0.5: (0, 4, -3), 1.2: (0, 0, 0)})
    return save(b, ex)


# ================================================================== the silver four
def horologium():
    b = new("horologium")
    b.bone("root"); b.bone("body", "root", (0, 0, 0)); b.bone("door_l", "body", (-5, 14, -4)); b.bone("door_r", "body", (5, 14, -4)); b.bone("pendulum", "body", (0, 22, -1)); b.bone("hand_big", "body", (0, 40, -4.5)); b.bone("hand_small", "body", (0, 40, -4.6))
    b.cube("body", (-8, 0, -5), (16, 3, 10), "wood_dark")                                                   # a grandfather clock
    b.cube("body", (-7, 3, -4), (14, 24, 8), "wood")
    b.cube("body", (-9, 27, -5), (18, 3, 10), "wood_dark")
    b.cube("body", (-8, 30, -4.5), (16, 14, 9), "wood")
    b.cube("body", (-6.5, 31.5, -5.2), (13, 11, 1), "gold", decal=("front", disc_decal((246, 240, 224), GOLD_GLOW)))
    b.cube("body", (-9, 44, -5), (18, 2, 10), "wood_dark")
    b.cube("body", (-3, 46, -3), (6, 4, 6), "gold", glow=(GOLD_GLOW, 0.5))
    b.cube("body", (-1, 50, -1), (2, 3, 2), "star", glow=((255, 240, 190), 1.0))
    b.cube("door_l", (-6.5, 4, -5), (6, 22, 1), "wood_dark", glow=((255, 220, 140), 0.12))
    b.cube("door_r", (0.5, 4, -5), (6, 22, 1), "wood_dark", glow=((255, 220, 140), 0.12))
    b.cube("body", (-5, 6, -3.5), (10, 18, 1), "void", glow=((255, 200, 110), 0.2))                         # the inside of the case
    b.cube("pendulum", (-0.5, 6, -2), (1, 16, 1), "gold")
    b.cube("pendulum", (-2.5, 4, -2.5), (5, 5, 2), "gold", glow=(GOLD_GLOW, 0.75))
    b.cube("hand_big", (-0.5, 40, -5), (1, 5, 1), "black", glow=(GOLD_GLOW, 0.6))
    b.cube("hand_small", (-0.5, 40, -5.2), (3, 1, 1), "black", glow=(GOLD_GLOW, 0.6))

    def ex(a):
        a.clips["idle"]["bones"].pop("root", None)
        a.rot("idle", "pendulum", {0.0: (0, 0, 14), 1.0: (0, 0, -14), 2.0: (0, 0, 14)})
        a.rot("idle", "hand_big", {0.0: (0, 0, 0), 2.0: (0, 0, -90)})
        a.rot("idle", "hand_small", {0.0: (0, 0, 0), 2.0: (0, 0, -8)})
        a.rot("cast", "door_l", {0.0: (0, 0, 0), 0.5: (0, 110, 0), 1.2: (0, 100, 0)})
        a.rot("cast", "door_r", {0.0: (0, 0, 0), 0.5: (0, -110, 0), 1.2: (0, -100, 0)})
        a.rot("cast", "hand_big", {0.0: (0, 0, 0), 1.2: (0, 0, -720)})
        a.rot("leave", "door_l", {0.0: (0, 100, 0), 0.5: (0, 0, 0)})
        a.rot("leave", "door_r", {0.0: (0, -100, 0), 0.5: (0, 0, 0)})
    a = Anim()
    clips(a, set(b.m.bones.keys()), ex)
    return b.save(a)


def crux():
    b = new("crux")
    b.bone("root"); b.bone("cross", "root", (0, 20, 0)); b.bone("halo", "cross", (0, 20, 0))
    b.cube("cross", (-1.5, 4, -1.5), (3, 32, 3), "star", glow=((190, 240, 255), 0.9))
    b.cube("cross", (-12, 21, -1.5), (24, 3, 3), "star", glow=((190, 240, 255), 0.9))
    b.cube("cross", (-3, 18, -3), (6, 8, 6), "glass", glow=((230, 252, 255), 1.0))
    for (x, y) in ((-1.5, 36), (-1.5, 1), (-14, 21), (11, 21)):
        b.cube("cross", (x - 0.5, y, -2), (4, 4, 4), "star", glow=((255, 255, 255), 1.0), rotation=(0, 0, 45), pivot=(x + 1.5, y + 2, 0))
    b.cube("halo", (-10, 19.5, -10), (20, 1, 20), "glass", glow=((170, 230, 255), 0.35))
    for i in range(8):
        a = i * math.pi / 4
        b.cube("halo", (math.cos(a) * 11 - 1, 19, math.sin(a) * 11 - 1), (2, 2, 2), "star", glow=((220, 250, 255), 1.0))

    def ex(a):
        a.pos("idle", "cross", {0.0: (0, 0, 0), 1.0: (0, 2, 0), 2.0: (0, 0, 0)})
        a.rot("idle", "cross", {0.0: (0, 0, 0), 2.0: (0, 360, 0)})
        a.rot("idle", "halo", {0.0: (0, 0, 0), 2.0: (0, -360, 0)})
        a.rot("cast", "cross", {0.0: (0, 0, 0), 1.2: (0, 720, 0)})
        a.scale("cast", "halo", {0.0: (1, 1, 1), 0.6: (2.4, 1, 2.4), 1.2: (1, 1, 1)})
        a.scale("summon", "cross", {0.0: (0.1, 0.1, 0.1), 0.7: (1, 1, 1)})
        a.scale("leave", "cross", {0.0: (1, 1, 1), 0.7: (0.1, 0.1, 0.1)})
    a = Anim()
    clips(a, set(b.m.bones.keys()), ex)
    return b.save(a)


def lyra():
    b = new("lyra")
    humanoid(b, skin="skin_pale", top="cloth_white", bottom="cloth_white", shoes="cloth_pink", sleeves="cloth_white", face_fn=face(eye_color=(200, 110, 170), mouth=(240, 140, 170), blush=True, glow=(255, 200, 240)))
    b.cube("body", (-5, 4, -2.5), (10, 9, 5), "cloth_pink")                                                # a long flowing dress
    hair(b, "hair_pink", back=16, twin=True)
    b.cube("head", (-4.5, 32, -3), (9, 1, 5), "gold", glow=((255, 220, 150), 0.6))
    b.bone("harp", "arm_l", (7, 14, -6))
    b.cube("harp", (6, 10, -10), (2, 22, 3), "gold", glow=((255, 220, 150), 0.35))                          # the pillar of the harp
    b.cube("harp", (6, 29, -14), (2, 3, 10), "gold")                                                       # the neck
    b.cube("harp", (6, 8, -14), (2, 3, 7), "gold")
    for i in range(7):
        b.cube("harp", (6.4, 11 + i * 2.6, -13 + i * 0.3), (1, 1, 7 - i * 0.4 if False else 6), "star", glow=((255, 230, 250), 0.95))
    b.bone("notes", "root", (0, 30, 0))
    for i in range(5):
        b.cube("notes", (-12 + i * 5, 34 + (i % 2) * 5, -6 + (i % 3) * 4), (2, 3, 1), "star", glow=((255, 220, 250), 0.9))

    def ex(a):
        a.rot("idle", "notes", {0.0: (0, 0, 0), 2.0: (0, 360, 0)})
        a.pos("idle", "notes", {0.0: (0, 0, 0), 1.0: (0, 2, 0), 2.0: (0, 0, 0)})
        a.rot("cast", "harp", {0.0: (0, 0, 0), 0.4: (-20, 0, 0), 0.8: (-20, 0, 8), 1.2: (0, 0, 0)})
        a.rot("cast", "arm_r", {0.0: (0, 0, 2), 0.3: (60, 0, 0), 0.5: (80, 0, 6), 0.7: (60, 0, -6), 0.9: (80, 0, 6), 1.2: (0, 0, 2)})
        a.scale("cast", "notes", {0.0: (0.5, 0.5, 0.5), 0.6: (2.0, 1.4, 2.0), 1.2: (1, 1, 1)})
    a = Anim()
    clips(a, set(b.m.bones.keys()), ex)
    return b.save(a)


def nikora():
    b = new("nikora")
    b.bone("root"); b.bone("body", "root", (0, 5, 0)); b.bone("head", "body", (0, 10, 0))
    b.bone("leg_r", "root", (-2, 4, -1)); b.bone("leg_l", "root", (2, 4, -1)); b.bone("arm_r", "body", (-4, 9, 0)); b.bone("arm_l", "body", (4, 9, 0)); b.bone("tailn", "body", (0, 7, 3))
    b.cube("body", (-4, 3, -3), (8, 7, 6), "dog_white")
    b.cube("head", (-5, 9, -5), (10, 9, 9), "dog_white", decal=("front", face(eye_color=(20, 16, 24), mouth=(230, 150, 150), y=3)))
    b.cube("head", (-2, 10, -7), (4, 3, 3), "dog_orange")                                                  # the carrot nose
    b.cube("head", (-0.5, 12, -7.5), (1, 1, 1), "black")
    b.cube("head", (-4.5, 17, -2), (3, 3, 2), "dog_white"); b.cube("head", (1.5, 17, -2), (3, 3, 2), "dog_white")
    b.cube("leg_r", (-4, 0, -2), (3, 4, 3), "dog_white"); b.cube("leg_l", (1, 0, -2), (3, 4, 3), "dog_white")
    b.cube("arm_r", (-6, 4, -1.5), (2, 5, 3), "dog_white"); b.cube("arm_l", (4, 4, -1.5), (2, 5, 3), "dog_white")
    b.cube("tailn", (-1, 5, 3), (2, 4, 2), "dog_white")
    b.cube("body", (-4.2, 8, -3.2), (8.4, 1, 6.4), "cloth_red", glow=((255, 130, 130), 0.3))               # a red scarf

    def ex(a):
        a.rot("idle", "tailn", {0.0: (0, 0, 20), 0.25: (0, 0, -20), 0.5: (0, 0, 20), 0.75: (0, 0, -20), 1.0: (0, 0, 20), 1.25: (0, 0, -20), 1.5: (0, 0, 20), 1.75: (0, 0, -20), 2.0: (0, 0, 20)})
        a.pos("idle", "root", {0.0: (0, 0, 0), 0.5: (0, 1.2, 0), 1.0: (0, 0, 0), 1.5: (0, 1.2, 0), 2.0: (0, 0, 0)})
        a.pos("cast", "root", {0.0: (0, 0, 0), 0.3: (0, 6, -4), 0.6: (0, 0, -10), 0.9: (0, 0, -10), 1.2: (0, 0, 0)})
        a.rot("cast", "tailn", {0.0: (0, 0, 0), 0.6: (0, 0, 40), 1.2: (0, 0, 0)})
    a = Anim()
    clips(a, set(b.m.bones.keys()), ex)
    return b.save(a)


# ================================================================== the Gate
def gate():
    b = new("gate")
    b.bone("root"); b.bone("frame", "root", (0, 0, 0)); b.bone("door_l", "root", (-8, 0, -1)); b.bone("door_r", "root", (8, 0, -1)); b.bone("keyhole", "root", (0, 56, -3)); b.bone("stars", "root", (0, 52, 0))
    for sgn in (-1, 1):
        x = sgn * 12
        b.cube("frame", (x - 3 - (0 if sgn > 0 else 0), 0, -4), (6, 4, 8), "marble_dark")
        b.cube("frame", (x - 2.5, 4, -3.5), (5, 44, 7), "marble")
        b.cube("frame", (x - 3.5, 20, -4.5), (7, 3, 9), "gold", glow=((255, 220, 130), 0.4))
        b.cube("frame", (x - 3.5, 36, -4.5), (7, 3, 9), "gold", glow=((255, 220, 130), 0.4))
        b.cube("frame", (x - 3, 48, -4), (6, 4, 8), "marble_dark")
    for i, (w, h, y) in enumerate(((30, 4, 52), (24, 4, 56), (16, 3, 60), (8, 3, 63))):                     # the stepped arch
        b.cube("frame", (-w / 2, y, -4 + i * 0.4), (w, h, 8 - i), "marble" if i % 2 == 0 else "gold", glow=((255, 220, 130), 0.3) if i % 2 else None)
    b.cube("frame", (-3, 58, -5), (6, 6, 2), "gold", glow=((255, 230, 150), 0.6))
    b.cube("keyhole", (-2, 56, -5.5), (4, 6, 1), "gold", decal=("front", keyhole((255, 240, 190))), glow=((255, 230, 150), 0.5))
    b.cube("frame", (-9, 0, 0), (18, 52, 1), "void", glow=((255, 226, 150), 0.1))                           # the space behind the doors
    b.cube("door_l", (-8, 4, -1.5), (8, 48, 2), "marble")
    b.cube("door_r", (0, 4, -1.5), (8, 48, 2), "marble")
    for s, nm in ((-1, "door_l"), (1, "door_r")):
        b.cube(nm, (s * 4 - 3 if s < 0 else 1, 14, -2.2), (6, 1, 1), "gold", glow=((255, 220, 130), 0.5))
        b.cube(nm, (s * 4 - 3 if s < 0 else 1, 30, -2.2), (6, 1, 1), "gold", glow=((255, 220, 130), 0.5))
        b.cube(nm, (s * 4 - 1 if s < 0 else 3, 6, -2.2), (1, 44, 1), "gold", glow=((255, 220, 130), 0.3))
    for i in range(12):                                                                                    # twelve stars round the arch
        a = math.pi * i / 11
        b.cube("stars", (math.cos(a) * 17 - 1, 50 + math.sin(a) * 14 - 1, -4.5), (2, 2, 1), "star", glow=((255, 244, 200), 1.0))

    def anim():
        a = Anim()
        a.clip("idle", 3.0, True); a.clip("open", 1.2, False); a.clip("open_hold", 3.0, True); a.clip("close", 1.0, False)
        a.rot("idle", "stars", {0.0: (0, 0, 0), 3.0: (0, 0, 0)})
        a.scale("idle", "stars", {0.0: (1, 1, 1), 1.5: (1.04, 1.04, 1), 3.0: (1, 1, 1)})
        a.rot("open", "door_l", {0.0: (0, 0, 0), 1.2: (0, 105, 0)}); a.rot("open", "door_r", {0.0: (0, 0, 0), 1.2: (0, -105, 0)})
        a.rot("open_hold", "door_l", {0.0: (0, 105, 0), 3.0: (0, 105, 0)}); a.rot("open_hold", "door_r", {0.0: (0, -105, 0), 3.0: (0, -105, 0)})
        a.scale("open_hold", "stars", {0.0: (1, 1, 1), 1.5: (1.06, 1.06, 1), 3.0: (1, 1, 1)})
        a.rot("close", "door_l", {0.0: (0, 105, 0), 1.0: (0, 0, 0)}); a.rot("close", "door_r", {0.0: (0, -105, 0), 1.0: (0, 0, 0)})
        return a
    return b.save(anim())


# ================================================================== the Spirit King
def king(phase):
    b = new("king_p%d" % phase)
    robe = "cloth_white" if phase == 1 else "cloth_night" if phase == 2 else "gold"
    trim = "gold"
    b.bone("root"); b.bone("body", "root", (0, 18, 0)); b.bone("head", "body", (0, 36, 0)); b.bone("arm_r", "body", (-9, 34, 0)); b.bone("arm_l", "body", (9, 34, 0))
    b.bone("cape", "body", (0, 36, 5)); b.bone("staff", "arm_r", (-12, 22, -2)); b.bone("orbit", "root", (0, 30, 0)); b.bone("halo", "head", (0, 42, 6))
    # the robe: three stacked widths down to the floor, the chest, the shoulders
    b.cube("body", (-9, 0, -6), (18, 8, 12), robe if phase != 3 else "cloth_gold")
    b.cube("body", (-8, 8, -5.5), (16, 10, 11), robe if phase != 3 else "cloth_gold", glow=((255, 230, 150), 0.12) if phase > 1 else None)
    b.cube("body", (-7, 18, -5), (14, 18, 10), robe if phase != 3 else "gold", glow=((255, 240, 180), 0.2) if phase == 3 else None)
    b.cube("body", (-9.5, 0, -6.5), (19, 2, 13), trim, glow=((255, 220, 130), 0.45))                         # the gold hem
    b.cube("body", (-1, 2, -6.8), (2, 34, 1), trim, glow=((255, 220, 130), 0.35))                             # the gold stole down the front
    b.cube("body", (-3, 28, -6), (6, 6, 1), "star" if phase < 3 else "gold", glow=((255, 244, 200), 1.0))    # the gem on the chest
    b.cube("body", (-10, 33, -6), (20, 4, 12), trim, glow=((255, 220, 130), 0.3))                             # the collar and shoulders
    for sgn in (-1, 1):
        b.cube("body", (sgn * 11 - 3, 34, -5), (6, 5, 10), "gold", glow=((255, 220, 130), 0.5))               # pauldrons
        if phase == 3:
            b.cube("body", (sgn * 12 - 2, 38, -4), (4, 6, 8), "gold", glow=((255, 240, 180), 0.7))
    # arms with wide sleeves
    b.cube("arm_r", (-15, 20, -4), (6, 14, 8), robe if phase != 3 else "cloth_gold"); b.cube("arm_r", (-14.5, 15, -3.5), (5, 5, 7), trim, glow=((255, 220, 130), 0.4)); b.cube("arm_r", (-14, 11, -2.5), (4, 4, 5), "skin_pale")
    b.cube("arm_l", (9, 20, -4), (6, 14, 8), robe if phase != 3 else "cloth_gold"); b.cube("arm_l", (9.5, 15, -3.5), (5, 5, 7), trim, glow=((255, 220, 130), 0.4)); b.cube("arm_l", (10, 11, -2.5), (4, 4, 5), "skin_pale")
    # the head: face, long white beard, crown
    b.cube("head", (-5, 36, -5), (10, 10, 10), "skin_pale", decal=("front", face(eye_color=(120, 160, 230) if phase < 3 else (255, 255, 255), glow=(255, 240, 190) if phase == 3 else None, mouth=(190, 130, 130))))
    b.cube("head", (-5.5, 33, -6), (11, 6, 3), "hair_white"); b.cube("head", (-4, 28, -6.5), (8, 6, 2), "hair_white"); b.cube("head", (-3, 24, -6.5), (6, 5, 2), "hair_white")   # the beard
    b.cube("head", (-5.5, 41, -5.5), (11, 3, 11), "hair_white"); b.cube("head", (-5.5, 37, 4.5), (11, 8, 2), "hair_white")
    b.cube("head", (-5.5, 44, -5.5), (11, 3, 11), "gold", glow=((255, 220, 130), 0.5))                      # the crown's band
    nspike = 5 if phase < 3 else 9
    for i in range(nspike):
        x = -4.5 + i * 9.0 / (nspike - 1)
        h = 4 + (3 if i % 2 == 0 else 0) + (3 if phase == 3 else 0) + (2 if i == nspike // 2 else 0)
        b.cube("head", (x - 0.75, 47, -5.5), (2, h, 2), "gold", glow=((255, 230, 150), 0.7))
        b.cube("head", (x - 0.75, 47, 3.5), (2, h - 1, 2), "gold", glow=((255, 230, 150), 0.5))
    b.cube("head", (-1.5, 46, -6), (3, 3, 1), "star", glow=((255, 250, 220), 1.0))                         # the crown's jewel
    # the sceptre in his right hand: a long staff, a star in a cage of gold
    b.cube("staff", (-13, -4, -1), (2, 34, 2), "gold", glow=((255, 220, 130), 0.3))
    b.cube("staff", (-14.5, 30, -2.5), (5, 5, 5), "star", glow=((255, 248, 210), 1.0))
    for (x, y, z) in ((-15, 29, -3), (-10.5, 29, -3), (-15, 29, 1.5), (-10.5, 29, 1.5)):
        b.cube("staff", (x, y, z), (1, 7, 1), "gold", glow=((255, 220, 130), 0.5))
    b.cube("staff", (-14, 35, -2), (4, 1, 4), "gold")
    # the cape
    b.cube("cape", (-8, -2, 4), (16, 36, 1), "cloth_night" if phase > 1 else "cloth_blue", glow=((120, 150, 255), 0.22) if phase > 1 else None)
    if phase >= 2:                                                                                         # a mantle of night with stars in it
        for (x, y) in ((-6, 30), (-2, 22), (3, 28), (5, 14), (-5, 10), (0, 4), (-3, 16)):
            b.cube("cape", (x, y, 5), (1, 1, 1), "star", glow=((255, 250, 230), 1.0))
        # the halo of twelve star shards behind the head
        for i in range(12):
            a = i * math.pi / 6
            b.cube("halo", (math.cos(a) * 12 - 1, 42 + math.sin(a) * 12 - 1, 6), (2, 2, 1), "star" if phase == 2 else "gold", glow=((255, 244, 200), 1.0),
                   rotation=(0, 0, math.degrees(a)), pivot=(math.cos(a) * 12, 42 + math.sin(a) * 12, 6))
            b.cube("halo", (math.cos(a) * 9 - 0.5, 42 + math.sin(a) * 9 - 0.5, 6), (1, 1, 1), "star", glow=((255, 244, 200), 0.8))
    if phase == 3:                                                                                         # twelve keys floating round him
        for i in range(12):
            a = i * math.pi / 6
            x, z = math.cos(a) * 18, math.sin(a) * 18
            b.cube("orbit", (x - 1, 22 + (i % 3) * 3, z - 3), (2, 2, 6), "gold", glow=((255, 230, 150), 0.7))     # the shaft
            b.cube("orbit", (x - 1.5, 22 + (i % 3) * 3 - 1, z + 3), (3, 4, 2), "gold", glow=((255, 244, 200), 1.0))   # the bow
            b.cube("orbit", (x - 0.5, 22 + (i % 3) * 3 - 1, z - 3), (1, 2, 2), "gold", glow=((255, 230, 150), 0.7))    # the bit
    # a ring of light under him
    b.cube("root", (-14, -0.5, -14), (28, 1, 28), "star", glow=((255, 226, 130), 0.22 + 0.12 * phase))

    def anim():
        a = Anim()
        a.clip("idle", 3.0, True); a.clip("raise", 2.0, False); a.clip("slam", 2.0, False); a.clip("point", 2.0, False); a.clip("judge", 4.0, False); a.clip("death", 2.0, False)
        a.pos("idle", "root", {0.0: (0, 0, 0), 1.5: (0, 2.0, 0), 3.0: (0, 0, 0)})
        a.rot("idle", "head", {0.0: (0, 0, 0), 1.5: (2, 4, 0), 3.0: (0, 0, 0)})
        a.rot("idle", "arm_l", {0.0: (0, 0, 4), 1.5: (-8, 0, 6), 3.0: (0, 0, 4)})
        a.rot("idle", "arm_r", {0.0: (4, 0, -3), 1.5: (8, 0, -5), 3.0: (4, 0, -3)})
        a.rot("idle", "cape", {0.0: (4, 0, 0), 1.5: (10, 0, 2), 3.0: (4, 0, 0)})
        a.rot("idle", "halo", {0.0: (0, 0, 0), 3.0: (0, 0, 360)})
        a.rot("idle", "orbit", {0.0: (0, 0, 0), 3.0: (0, 360, 0)})
        a.rot("idle", "staff", {0.0: (0, 0, 0), 1.5: (3, 0, 0), 3.0: (0, 0, 0)})
        # Star Fall: the sceptre rises, both arms up, the halo spins
        a.rot("raise", "arm_r", {0.0: (4, 0, -3), 0.6: (130, 0, -10), 1.4: (130, 0, -10), 2.0: (4, 0, -3)})
        a.rot("raise", "arm_l", {0.0: (0, 0, 4), 0.6: (110, 0, 20), 1.4: (110, 0, 20), 2.0: (0, 0, 4)})
        a.rot("raise", "halo", {0.0: (0, 0, 0), 2.0: (0, 0, 720)})
        a.pos("raise", "root", {0.0: (0, 0, 0), 0.8: (0, 6, 0), 1.6: (0, 6, 0), 2.0: (0, 0, 0)})
        # Gate Slam: the sceptre down on the floor, a bow
        a.rot("slam", "arm_r", {0.0: (4, 0, -3), 0.7: (150, 0, -6), 1.0: (-10, 0, 0), 1.3: (-5, 0, 0), 2.0: (4, 0, -3)})
        a.rot("slam", "body", {0.0: (0, 0, 0), 0.7: (-10, 0, 0), 1.0: (24, 0, 0), 1.5: (10, 0, 0), 2.0: (0, 0, 0)})
        a.rot("slam", "head", {0.0: (0, 0, 0), 1.0: (14, 0, 0), 2.0: (0, 0, 0)})
        a.pos("slam", "root", {0.0: (0, 0, 0), 0.7: (0, 4, 0), 1.0: (0, -1, -3), 2.0: (0, 0, 0)})
        # Bind and Beams: the left hand points
        a.rot("point", "arm_l", {0.0: (0, 0, 4), 0.5: (95, 0, 12), 1.5: (95, 0, 12), 2.0: (0, 0, 4)})
        a.rot("point", "body", {0.0: (0, 0, 0), 0.5: (0, 22, 0), 1.5: (0, -22, 0), 2.0: (0, 0, 0)})
        a.rot("point", "head", {0.0: (0, 0, 0), 0.5: (0, 20, 0), 1.5: (0, -20, 0), 2.0: (0, 0, 0)})
        # Judgement: he rises, arms wide, the halo and the keys spin fast
        a.pos("judge", "root", {0.0: (0, 0, 0), 1.0: (0, 14, 0), 3.2: (0, 14, 0), 4.0: (0, 0, 0)})
        a.rot("judge", "arm_r", {0.0: (4, 0, -3), 1.0: (30, 0, -80), 3.2: (30, 0, -80), 4.0: (4, 0, -3)})
        a.rot("judge", "arm_l", {0.0: (0, 0, 4), 1.0: (30, 0, 80), 3.2: (30, 0, 80), 4.0: (0, 0, 4)})
        a.rot("judge", "halo", {0.0: (0, 0, 0), 4.0: (0, 0, 1440)})
        a.rot("judge", "orbit", {0.0: (0, 0, 0), 4.0: (0, 1080, 0)})
        a.rot("judge", "head", {0.0: (0, 0, 0), 1.0: (-20, 0, 0), 3.2: (-20, 0, 0), 4.0: (0, 0, 0)})
        # death: he kneels, the glow of the halo goes out
        a.rot("death", "body", {0.0: (0, 0, 0), 1.2: (30, 0, 0), 2.0: (45, 0, 0)})
        a.rot("death", "head", {0.0: (0, 0, 0), 1.2: (30, 0, 0), 2.0: (40, 0, 0)})
        a.pos("death", "root", {0.0: (0, 0, 0), 1.2: (0, -6, 0), 2.0: (0, -12, 0)})
        a.scale("death", "halo", {0.0: (1, 1, 1), 2.0: (0.2, 0.2, 0.2)})
        a.rot("death", "arm_r", {0.0: (4, 0, -3), 1.5: (40, 0, 0)})
        a.rot("death", "arm_l", {0.0: (0, 0, 4), 1.5: (40, 0, 0)})
        return a
    return b.save(anim())


BUILDERS = {"aquarius": aquarius, "taurus": taurus, "leo": leo, "virgo": virgo, "sagittarius": sagittarius, "gemini": gemini, "cancer": cancer, "scorpio": scorpio,
            "capricorn": capricorn, "pisces": pisces, "aries": aries, "libra": libra, "horologium": horologium, "crux": crux, "lyra": lyra, "nikora": nikora, "gate": gate,
            "king_p1": lambda: king(1), "king_p2": lambda: king(2), "king_p3": lambda: king(3)}


def main(argv):
    if argv and argv[0] == "--list":
        print(" ".join("celestial_" + k for k in BUILDERS))
        return
    ids = [i for i in BUILDERS if not argv or i in argv]
    for i in ids:
        print("built", BUILDERS[i]())


if __name__ == "__main__":
    main(sys.argv[1:])
