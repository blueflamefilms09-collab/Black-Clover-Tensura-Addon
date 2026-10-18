"""tools/elite/gen_world_models.py: builds the five GeoLite models of the elite world engine and their preview scenes.

    python3 -B tools/elite/gen_world_models.py             # writes assets/nusmp/geo|animations|textures/entity/elite_* and tools/render_preview/scenes.d/elite_*.json
    python3 -B tools/render_preview/preview.py elite_gravebound_colossus_front     # looks at one (needs JDK 21, numpy, Pillow)

Why these are GeoLite models and not arc-kit characters (docs/arc/art_kit.md): the arc kit paints named humanoid characters (a face, hair, an outfit, a weapon) and solves their arms with
inverse kinematics. A golem of grave soil, a robed skeleton with floating orbs, a vessel of bone and crystal, a spike of earth and a bolt of void are not characters, so they use the
same GeoLite builder the Zogratis, Foundry and Bronze models use (tools/geo_builder.py: bones, cubes, keyframes) with the small painting kit of world_art.py. Every model has the clips the
renderer asks for (bosses: idle walk windup cast strike transform death), a glow layer for what burns (eyes, runes, cores), and is deterministic. The numbers (sizes, angles, timings) are
the mod's own; the original is not available.
"""
import json
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

from world_art import Atlas, Model, Anim, box, draw_on_face  # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
SCENES = os.path.join(ROOT, "tools", "render_preview", "scenes.d")
KEY = "elite"


def mirror(origin, size):
    """The origin of the same cube on the other side of the x axis."""
    return (-(origin[0] + size[0]), origin[1], origin[2])


# ====================================================================================================== Gravebound Colossus
def colossus():
    m = Model(KEY, "gravebound_colossus", 384, 384)
    at = Atlas(384, 384)
    m.bone("root", pivot=(0, 0, 0))
    m.bone("body", "root", (0, 28, 0))
    m.bone("torso", "body", (0, 36, 0))
    m.bone("head", "torso", (0, 62, 0))
    m.bone("jaw", "head", (0, 64, -7))
    m.bone("back", "torso", (0, 50, 10))
    for side, sx in (("l", 1), ("r", -1)):
        m.bone("arm_" + side, "torso", (26 * sx, 60, 0))
        m.bone("fist_" + side, "arm_" + side, (26 * sx, 36, 0))
        m.bone("leg_" + side, "root", (9 * sx, 28, 0))

    box(m, at, "body", (-17, 26, -9), (34, 10, 18), "stone", seed=1)
    box(m, at, "body", (-18, 31, -10), (36, 3, 20), "bone", seed=2)
    box(m, at, "torso", (-20, 36, -11), (40, 26, 22), "soil", seed=3)
    for i, y in enumerate((40, 45, 50)):
        box(m, at, "torso", (-15, y, -14), (30, 2, 3), "bone", seed=10 + i)
    box(m, at, "torso", (-2, 38, -14), (4, 22, 3), "bone", seed=14)
    core = len(at.items)
    box(m, at, "torso", (-6, 42, -15), (12, 6, 1), "rune", glow=(255, 150, 60), seed=15)
    for i, (x, w, h, rz) in enumerate(((-13, 9, 18, 8), (3, 9, 20, -6), (-4, 8, 13, 2))):
        box(m, at, "back", (x, 44 if i < 2 else 36, 10), (w, h, 3), "stone", rotation=(0, 0, rz), pivot=(x + w / 2.0, 44, 11), seed=20 + i)
    box(m, at, "back", (-10, 62, 9), (3, 8, 3), "bone", rotation=(0, 0, -20), pivot=(-9, 62, 10), seed=24)

    box(m, at, "head", (-8, 62, -9), (16, 13, 16), "bone", seed=30)
    box(m, at, "head", (-9, 69, -10), (18, 3, 4), "stone", seed=31)
    box(m, at, "head", (-6, 65, -10), (4, 3, 1), "rune", glow=(255, 190, 70), seed=32)
    box(m, at, "head", (2, 65, -10), (4, 3, 1), "rune", glow=(255, 190, 70), seed=33)
    for i, (x, h, rz) in enumerate(((-9, 10, -14), (5, 10, 14), (-2, 12, 0))):
        box(m, at, "head", (x, 75, -3), (4, h, 4), "stone", rotation=(0, 0, rz), pivot=(x + 2, 75, -1), seed=34 + i)
    box(m, at, "jaw", (-6, 58, -10), (12, 4, 12), "bone", seed=40)
    for i, x in enumerate((-5, -1, 3)):
        box(m, at, "jaw", (x, 62, -10), (2, 2, 1), "bone", seed=41 + i)

    for side, sx in (("l", 1), ("r", -1)):
        def X(o, s):
            return o if sx == 1 else mirror(o, s)
        s1, s2, s3, s4 = (12, 26, 12), (16, 12, 16), (3, 10, 3), (18, 24, 18)
        box(m, at, "arm_" + side, X((20, 34, -6), s1), s1, "soil", seed=50)
        box(m, at, "arm_" + side, X((19, 56, -8), s2), s2, "stone", seed=51)
        o = X((30, 60, -2), s3)
        box(m, at, "arm_" + side, o, s3, "bone", rotation=(0, 0, -25 * sx), pivot=(o[0] + 1.5, 60, 0), seed=52)
        box(m, at, "fist_" + side, X((17, 12, -9), s4), s4, "stone", seed=53)
        s5 = (18, 4, 2)
        box(m, at, "fist_" + side, X((17, 12, -11), s5), s5, "bone", seed=54)
        s6, s7 = (12, 28, 12), (14, 5, 17)
        box(m, at, "leg_" + side, X((3, 0, -6), s6), s6, "soil", seed=60)
        box(m, at, "leg_" + side, X((2, 0, -9), s7), s7, "stone", seed=61)
        s8 = (10, 6, 2)
        box(m, at, "leg_" + side, X((4, 18, -8), s8), s8, "bone", seed=62)

    a = Anim()
    a.clip("idle", 3.0, True)
    a.pos("idle", "body", {0.0: (0, 0, 0), 1.5: (0, 0.8, 0), 3.0: (0, 0, 0)})
    a.rot("idle", "torso", {0.0: (0, 0, 0), 1.5: (2, 0, 0), 3.0: (0, 0, 0)})
    a.rot("idle", "head", {0.0: (0, 0, 0), 1.5: (-2, 3, 0), 3.0: (0, 0, 0)})
    a.rot("idle", "arm_l", {0.0: (0, 0, 0), 1.5: (4, 0, -2), 3.0: (0, 0, 0)})
    a.rot("idle", "arm_r", {0.0: (0, 0, 0), 1.5: (4, 0, 2), 3.0: (0, 0, 0)})
    a.rot("idle", "jaw", {0.0: (0, 0, 0), 1.5: (6, 0, 0), 3.0: (0, 0, 0)})

    a.clip("walk", 1.4, True)
    a.rot("walk", "leg_l", {0.0: (28, 0, 0), 0.7: (-28, 0, 0), 1.4: (28, 0, 0)})
    a.rot("walk", "leg_r", {0.0: (-28, 0, 0), 0.7: (28, 0, 0), 1.4: (-28, 0, 0)})
    a.rot("walk", "arm_l", {0.0: (-20, 0, 0), 0.7: (20, 0, 0), 1.4: (-20, 0, 0)})
    a.rot("walk", "arm_r", {0.0: (20, 0, 0), 0.7: (-20, 0, 0), 1.4: (20, 0, 0)})
    a.pos("walk", "body", {0.0: (0, 0, 0), 0.35: (0, 1.8, 0), 0.7: (0, 0, 0), 1.05: (0, 1.8, 0), 1.4: (0, 0, 0)})
    a.rot("walk", "torso", {0.0: (4, 3, 0), 0.7: (4, -3, 0), 1.4: (4, 3, 0)})

    # windup: the right fist goes up over the head, the body leans back; held on the last frame
    a.clip("windup", 1.0, False)
    a.rot("windup", "arm_r", {0.0: (0, 0, 0), 0.9: (-165, 0, -10), 1.0: (-165, 0, -10)})
    a.rot("windup", "arm_l", {0.0: (0, 0, 0), 0.9: (-40, 0, 0), 1.0: (-40, 0, 0)})
    a.rot("windup", "torso", {0.0: (0, 0, 0), 0.9: (-14, -10, 0), 1.0: (-14, -10, 0)})
    a.rot("windup", "head", {0.0: (0, 0, 0), 0.9: (-8, 0, 0), 1.0: (-8, 0, 0)})
    a.pos("windup", "body", {0.0: (0, 0, 0), 0.9: (0, -2, 0), 1.0: (0, -2, 0)})

    # cast: both arms up and out, head back, the core flares (the renderer pulses the glow)
    a.clip("cast", 1.4, False)
    a.rot("cast", "arm_l", {0.0: (0, 0, 0), 1.2: (-140, 0, -25), 1.4: (-140, 0, -25)})
    a.rot("cast", "arm_r", {0.0: (0, 0, 0), 1.2: (-140, 0, 25), 1.4: (-140, 0, 25)})
    a.rot("cast", "head", {0.0: (0, 0, 0), 1.2: (-22, 0, 0), 1.4: (-22, 0, 0)})
    a.rot("cast", "torso", {0.0: (0, 0, 0), 1.2: (-10, 0, 0), 1.4: (-10, 0, 0)})
    a.rot("cast", "jaw", {0.0: (0, 0, 0), 1.2: (25, 0, 0), 1.4: (25, 0, 0)})

    # strike: the fist comes down hard and the whole body follows, then recovers slowly
    a.clip("strike", 0.9, False)
    a.rot("strike", "arm_r", {0.0: (-165, 0, -10), 0.12: (25, 0, 0), 0.9: (0, 0, 0)})
    a.rot("strike", "arm_l", {0.0: (-40, 0, 0), 0.12: (10, 0, 0), 0.9: (0, 0, 0)})
    a.rot("strike", "torso", {0.0: (-14, -10, 0), 0.12: (24, 6, 0), 0.9: (0, 0, 0)})
    a.pos("strike", "body", {0.0: (0, -2, 0), 0.12: (0, -4, 0), 0.9: (0, 0, 0)})
    a.rot("strike", "head", {0.0: (-8, 0, 0), 0.12: (14, 0, 0), 0.9: (0, 0, 0)})

    a.clip("transform", 2.5, False)
    a.pos("transform", "body", {0.0: (0, 0, 0), 0.4: (0, -6, 0), 1.2: (0, -6, 0), 1.6: (0, 2, 0), 2.5: (0, 0, 0)})
    a.rot("transform", "torso", {0.0: (0, 0, 0), 0.4: (26, 0, 0), 1.2: (26, 0, 0), 1.6: (-16, 0, 0), 2.5: (0, 0, 0)})
    a.rot("transform", "arm_l", {0.0: (0, 0, 0), 1.2: (-20, 0, -10), 1.6: (-150, 0, -30), 2.5: (0, 0, 0)})
    a.rot("transform", "arm_r", {0.0: (0, 0, 0), 1.2: (-20, 0, 10), 1.6: (-150, 0, 30), 2.5: (0, 0, 0)})
    a.rot("transform", "head", {0.0: (0, 0, 0), 0.4: (20, 0, 0), 1.2: (20, 0, 0), 1.6: (-30, 0, 0), 2.5: (0, 0, 0)})
    a.rot("transform", "jaw", {0.0: (0, 0, 0), 1.6: (30, 0, 0), 2.5: (0, 0, 0)})

    a.clip("death", 1.0, False)
    a.rot("death", "root", {0.0: (0, 0, 0), 1.0: (82, 0, 0)})
    a.pos("death", "root", {0.0: (0, 0, 0), 1.0: (0, -24, 14)})
    a.rot("death", "arm_l", {0.0: (0, 0, 0), 1.0: (-40, 0, -30)})
    a.rot("death", "arm_r", {0.0: (0, 0, 0), 1.0: (-40, 0, 30)})
    a.rot("death", "jaw", {0.0: (0, 0, 0), 1.0: (35, 0, 0)})
    return m, a, at, {"eyes": core}


# ====================================================================================================== Lich
def lich():
    m = Model(KEY, "lich", 192, 192)
    at = Atlas(192, 192)
    m.bone("root", pivot=(0, 0, 0))
    m.bone("torso", "root", (0, 16, 0))
    m.bone("head", "torso", (0, 28, 0))
    m.bone("jaw", "head", (0, 28, -3))
    m.bone("cape", "torso", (0, 30, 4))
    m.bone("arm_l", "torso", (5, 27, 0))
    m.bone("arm_r", "torso", (-5, 27, 0))
    m.bone("hand_r", "arm_r", (-6.5, 18, 0))
    m.bone("orbit", "root", (0, 24, 0))

    box(m, at, "root", (-8, 0, -6), (16, 6, 12), "cloth", seed=1)
    box(m, at, "root", (-7, 6, -5), (14, 6, 10), "cloth", seed=2)
    box(m, at, "root", (-8, 0, -6), (16, 1, 12), "trim", inflate=0.15, seed=3)
    box(m, at, "torso", (-6, 12, -4), (12, 4, 8), "cloth", seed=4)
    box(m, at, "torso", (-5, 16, -3), (10, 12, 6), "cloth", seed=5)
    for i, y in enumerate((18, 21, 24)):
        box(m, at, "torso", (-3, y, -3.8), (6, 1, 1), "bone", seed=6 + i)
    box(m, at, "torso", (-1, 17, -3.9), (2, 9, 1), "bone", seed=9)
    box(m, at, "torso", (-5.5, 26, -3.5), (11, 2, 7), "trim", seed=10)
    eyes = len(at.items)
    box(m, at, "head", (-4, 28, -4), (8, 8, 8), "bone", seed=11)
    box(m, at, "head", (-3, 31, -4.6), (2, 2, 1), "rune", glow=(190, 130, 255), seed=12)
    box(m, at, "head", (1, 31, -4.6), (2, 2, 1), "rune", glow=(190, 130, 255), seed=13)
    box(m, at, "head", (-5, 27, -1), (10, 11, 6), "cloth", seed=14)
    box(m, at, "head", (-5, 27, -5), (10, 2, 4), "cloth", seed=15)
    for i, (x, h, rz) in enumerate(((-4, 5, -10), (-1, 7, 0), (2, 5, 10))):
        box(m, at, "head", (x, 36, -1), (2, h, 2), "trim", rotation=(0, 0, rz), pivot=(x + 1, 36, 0), seed=16 + i)
    box(m, at, "jaw", (-3, 26, -4), (6, 2, 6), "bone", seed=20)
    box(m, at, "cape", (-6, 8, 4), (12, 22, 1), "cloth", rotation=(6, 0, 0), pivot=(0, 30, 4), seed=21)
    box(m, at, "arm_l", (5, 18, -2), (3, 10, 4), "cloth", seed=22)
    box(m, at, "arm_l", (5.5, 14, -1.5), (2, 4, 3), "bone", seed=23)
    box(m, at, "arm_r", (-8, 18, -2), (3, 10, 4), "cloth", seed=24)
    box(m, at, "hand_r", (-7.5, 14, -1.5), (2, 4, 3), "bone", seed=25)
    box(m, at, "hand_r", (-7, 0, -0.5), (1, 44, 1), "dark", seed=26)
    box(m, at, "hand_r", (-8.5, 40, -2), (4, 4, 4), "crystal", glow=(170, 110, 255), seed=27)
    for i in range(3):
        import math
        ang = math.radians(120 * i)
        x, z = 11 * math.cos(ang), 11 * math.sin(ang)
        box(m, at, "orbit", (x - 1.5, 22, z - 1.5), (3, 3, 3), "crystal", glow=(170, 110, 255), seed=30 + i)

    a = Anim()
    a.clip("idle", 3.0, True)
    a.pos("idle", "root", {0.0: (0, 0, 0), 1.5: (0, 0.6, 0), 3.0: (0, 0, 0)})
    a.rot("idle", "torso", {0.0: (0, 0, 0), 1.5: (2, 0, 0), 3.0: (0, 0, 0)})
    a.rot("idle", "head", {0.0: (0, 0, 0), 1.5: (-3, 4, 0), 3.0: (0, 0, 0)})
    a.rot("idle", "cape", {0.0: (0, 0, 0), 1.5: (5, 0, 0), 3.0: (0, 0, 0)})
    a.rot("idle", "arm_l", {0.0: (0, 0, 0), 1.5: (6, 0, -4), 3.0: (0, 0, 0)})
    a.rot("idle", "orbit", {0.0: (0, 0, 0), 3.0: (0, 360, 0)})

    a.clip("walk", 1.6, True)
    a.rot("walk", "torso", {0.0: (4, 3, 0), 0.8: (4, -3, 0), 1.6: (4, 3, 0)})
    a.rot("walk", "cape", {0.0: (14, 0, 0), 0.8: (22, 0, 0), 1.6: (14, 0, 0)})
    a.rot("walk", "arm_l", {0.0: (-10, 0, 0), 0.8: (10, 0, 0), 1.6: (-10, 0, 0)})
    a.rot("walk", "orbit", {0.0: (0, 0, 0), 1.6: (0, 360, 0)})
    a.pos("walk", "root", {0.0: (0, 0, 0), 0.4: (0, 1, 0), 0.8: (0, 0, 0), 1.2: (0, 1, 0), 1.6: (0, 0, 0)})

    a.clip("windup", 0.8, False)
    a.rot("windup", "arm_r", {0.0: (0, 0, 0), 0.7: (-110, 0, -10), 0.8: (-110, 0, -10)})
    a.rot("windup", "torso", {0.0: (0, 0, 0), 0.7: (-10, 12, 0), 0.8: (-10, 12, 0)})
    a.rot("windup", "head", {0.0: (0, 0, 0), 0.7: (-8, 0, 0), 0.8: (-8, 0, 0)})

    a.clip("cast", 1.2, False)
    a.rot("cast", "arm_l", {0.0: (0, 0, 0), 1.0: (-135, 0, -20), 1.2: (-135, 0, -20)})
    a.rot("cast", "arm_r", {0.0: (0, 0, 0), 1.0: (-120, 0, 12), 1.2: (-120, 0, 12)})
    a.rot("cast", "head", {0.0: (0, 0, 0), 1.0: (-18, 0, 0), 1.2: (-18, 0, 0)})
    a.pos("cast", "root", {0.0: (0, 0, 0), 1.0: (0, 3, 0), 1.2: (0, 3, 0)})
    a.rot("cast", "orbit", {0.0: (0, 0, 0), 1.2: (0, 720, 0)})

    a.clip("strike", 0.6, False)
    a.rot("strike", "arm_r", {0.0: (-110, 0, -10), 0.1: (30, 0, 0), 0.6: (0, 0, 0)})
    a.rot("strike", "torso", {0.0: (-10, 12, 0), 0.1: (14, -8, 0), 0.6: (0, 0, 0)})
    a.rot("strike", "head", {0.0: (-8, 0, 0), 0.1: (8, 0, 0), 0.6: (0, 0, 0)})
    a.rot("strike", "orbit", {0.0: (0, 0, 0), 0.6: (0, 360, 0)})

    a.clip("transform", 2.5, False)
    a.pos("transform", "root", {0.0: (0, 0, 0), 0.6: (0, -3, 0), 1.2: (0, 6, 0), 2.5: (0, 0, 0)})
    a.rot("transform", "arm_l", {0.0: (0, 0, 0), 1.2: (-150, 0, -40), 2.5: (0, 0, 0)})
    a.rot("transform", "arm_r", {0.0: (0, 0, 0), 1.2: (-150, 0, 40), 2.5: (0, 0, 0)})
    a.rot("transform", "head", {0.0: (0, 0, 0), 1.2: (-35, 0, 0), 2.5: (0, 0, 0)})
    a.rot("transform", "jaw", {0.0: (0, 0, 0), 1.2: (35, 0, 0), 2.5: (0, 0, 0)})
    a.rot("transform", "orbit", {0.0: (0, 0, 0), 2.5: (0, 1080, 0)})

    a.clip("death", 1.0, False)
    a.rot("death", "root", {0.0: (0, 0, 0), 1.0: (-80, 0, 0)})
    a.pos("death", "root", {0.0: (0, 0, 0), 1.0: (0, -4, 8)})
    a.rot("death", "arm_l", {0.0: (0, 0, 0), 1.0: (-30, 0, -50)})
    a.rot("death", "arm_r", {0.0: (0, 0, 0), 1.0: (-30, 0, 50)})
    return m, a, at, {"eyes": eyes}


# ====================================================================================================== Phylactery
def phylactery():
    m = Model(KEY, "phylactery", 128, 128)
    at = Atlas(128, 128)
    m.bone("root", pivot=(0, 0, 0))
    m.bone("core", "root", (0, 10, 0))
    m.bone("ring_a", "core", (0, 10, 0))
    m.bone("ring_b", "core", (0, 10, 0))
    for i in range(3):
        import math
        ang = math.radians(120 * i + 30)
        x, z = 4 * math.cos(ang), 4 * math.sin(ang)
        box(m, at, "root", (x - 1, 0, z - 1), (2, 5, 2), "bone", rotation=(0, 0, 0), seed=1 + i)
    box(m, at, "root", (-4, 0, -4), (8, 2, 8), "stone", seed=5)
    box(m, at, "core", (-4, 6, -4), (8, 8, 8), "crystal", glow=(160, 90, 255), rotation=(45, 0, 45), pivot=(0, 10, 0), seed=6)
    box(m, at, "core", (-2, 8, -2), (4, 4, 4), "dark", glow=(255, 235, 255), seed=7)
    box(m, at, "ring_a", (-8, 9, -1), (16, 2, 2), "bone", seed=8)
    box(m, at, "ring_a", (-1, 9, -8), (2, 2, 16), "bone", seed=9)
    box(m, at, "ring_b", (-1, 2, -1), (2, 16, 2), "bone", seed=10)
    box(m, at, "ring_b", (-7, 17, -1), (14, 2, 2), "bone", seed=11)
    a = Anim()
    a.clip("idle", 4.0, True)
    a.rot("idle", "ring_a", {0.0: (0, 0, 0), 4.0: (0, 360, 0)})
    a.rot("idle", "ring_b", {0.0: (0, 0, 0), 4.0: (360, 0, 0)})
    a.rot("idle", "core", {0.0: (0, 0, 0), 4.0: (0, -360, 0)})
    a.scale("idle", "core", {0.0: (1, 1, 1), 1.0: (1.12, 1.12, 1.12), 2.0: (1, 1, 1), 3.0: (1.12, 1.12, 1.12), 4.0: (1, 1, 1)})
    return m, a, at, {}


# ====================================================================================================== Gaia spike
def spike():
    import math
    m = Model(KEY, "gaia_spike", 128, 128)
    at = Atlas(128, 128)
    m.bone("root", pivot=(0, 0, 0))
    m.bone("spike", "root", (0, 0, 0))
    m.bone("rubble", "root", (0, 0, 0))
    box(m, at, "spike", (-8, 0, -8), (16, 6, 16), "soil", seed=1)
    box(m, at, "spike", (-6, 6, -6), (12, 8, 12), "soil", seed=2)
    box(m, at, "spike", (-4, 14, -4), (8, 8, 8), "stone", seed=3)
    box(m, at, "spike", (-2, 22, -2), (4, 8, 4), "stone", seed=4)
    box(m, at, "spike", (-1, 30, -1), (2, 4, 2), "bone", seed=5)
    box(m, at, "spike", (-1, 8, -6.6), (2, 14, 1), "rune", glow=(255, 205, 120), seed=6)
    for i in range(5):
        ang = math.radians(72 * i + 10)
        x, z = 11 * math.cos(ang), 11 * math.sin(ang)
        box(m, at, "rubble", (x - 1.5, 0, z - 1.5), (3, 3 + i % 2, 3), "stone", rotation=(0, 20 * i, 0), pivot=(x, 1, z), seed=10 + i)
    a = Anim()
    a.clip("rise", 0.4, False)
    a.scale("rise", "spike", {0.0: (1, 0.02, 1), 0.12: (1, 1.18, 1), 0.2: (1, 1.0, 1), 0.4: (1, 1.0, 1)})
    a.pos("rise", "rubble", {0.0: (0, 0, 0), 0.12: (0, 3, 0), 0.4: (0, 0, 0)})
    a.clip("sink", 0.3, False)
    a.scale("sink", "spike", {0.0: (1, 1.0, 1), 0.3: (1, 0.02, 1)})
    a.pos("sink", "rubble", {0.0: (0, 0, 0), 0.3: (0, -2, 0)})
    return m, a, at, {}


# ====================================================================================================== Void bolt
def bolt():
    m = Model(KEY, "void_bolt", 64, 64)
    at = Atlas(64, 64)
    m.bone("root", pivot=(0, 3, 0))
    m.bone("spin", "root", (0, 3, 0))
    box(m, at, "root", (-2, 1, -2), (4, 4, 4), "dark", glow=(190, 120, 255), seed=1)
    box(m, at, "spin", (-5, 2, -1), (10, 2, 2), "crystal", glow=(150, 80, 255), seed=2)
    box(m, at, "spin", (-1, -2, -1), (2, 10, 2), "crystal", glow=(150, 80, 255), seed=3)
    box(m, at, "spin", (-1, 2, -5), (2, 2, 10), "crystal", glow=(150, 80, 255), seed=4)
    a = Anim()
    a.clip("spin", 0.6, True)
    a.rot("spin", "spin", {0.0: (0, 0, 0), 0.6: (360, 360, 0)})
    a.rot("spin", "root", {0.0: (0, 0, 0), 0.6: (0, 0, 360)})
    return m, a, at, {}


def build(name, fn, extras=None):
    m, a, at, info = fn()
    img, glow = at.paint()
    if extras:
        extras(at, img, glow, info)
    m.save(a)
    m.save_texture(img, glow)
    return m, at


# ====================================================================================================== painted details
def paint_colossus(at, img, glow, info):
    """The chest core: a pattern of bright and dim cells on the glow layer, like carved runes."""
    def runes(px, x0, y0, w, h):
        for x in range(w):
            for y in range(h):
                px[x0 + x, y0 + y] = (255, 190, 80, 255) if (x + y) % 3 else (160, 70, 20, 255)
    draw_on_face(at, glow, info["eyes"], "front", runes)


def paint_none(at, img, glow, info):
    pass


def scenes(name, clips, height, dist=None):
    """Preview scenes in the format of tools/render_preview (one per clip, two views each)."""
    out = {}
    d = dist or max(4.0, height * 2.2)
    for clip, ages in clips.items():
        key = "%s_%s_%s" % (KEY, name, clip)
        out[key] = {"kind": "geo", "model": "%s_%s" % (KEY, name), "space": "PROP", "layer": "CUTOUT", "argb": "FFFFFFFF", "glow": "FFFFFFFF", "ages": ages, "tile": [400, 560], "floor": True,
                    "figure": False, "expect": [], "anim": clip,
                    "views": [{"name": "front", "yaw": 0, "pitch": 5, "target": [0, height * 0.5, 0], "dist": d}, {"name": "3/4", "yaw": 38, "pitch": 8, "target": [0, height * 0.5, 0], "dist": d}]}
    return out


def main():
    os.makedirs(SCENES, exist_ok=True)
    allscenes = {}
    build("gravebound_colossus", colossus, paint_colossus)
    allscenes.update(scenes("gravebound_colossus", {"idle": [0, 30], "walk": [0, 10, 20], "windup": [0, 10, 18], "strike": [0, 3, 12], "cast": [0, 14, 24], "transform": [0, 20, 36, 50], "death": [0, 10, 20]}, 5.4))
    build("lich", lich, paint_none)
    allscenes.update(scenes("lich", {"idle": [0, 30], "walk": [0, 10, 20], "windup": [0, 8, 14], "strike": [0, 2, 8], "cast": [0, 12, 20], "transform": [0, 20, 36, 50], "death": [0, 10, 20]}, 2.6))
    build("phylactery", phylactery, paint_none)
    allscenes.update(scenes("phylactery", {"idle": [0, 20, 40]}, 1.2, 3.0))
    build("gaia_spike", spike, paint_none)
    allscenes.update(scenes("gaia_spike", {"rise": [0, 3, 6, 9], "sink": [0, 3, 6]}, 2.0, 5.0))
    build("void_bolt", bolt, paint_none)
    allscenes.update(scenes("void_bolt", {"spin": [0, 4, 8]}, 0.2, 1.5))
    names = {}
    for k, v in allscenes.items():
        names.setdefault(v["model"], {})[k] = v
    for model, sc in names.items():
        with open(os.path.join(SCENES, model + ".json"), "w") as f:
            json.dump(sc, f, indent=1)
    print("wrote", len(names), "models and", len(allscenes), "preview scenes")


if __name__ == "__main__":
    main()
