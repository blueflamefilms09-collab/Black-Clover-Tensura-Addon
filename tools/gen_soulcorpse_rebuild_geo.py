"""Soul Corpse Magic (1.93.0 rebuild) geo models: the Marionette Shell armour, the marionette puppet and the grasping hand.

    python3 -B tools/gen_soulcorpse_rebuild_geo.py        # writes every model, animation and texture, runs tools/geo_check.py

Models (assets/nusmp/geo/entity/soulcorpse_<name>.geo.json, animations/entity/..., textures/entity/soulcorpse_<name>.png + _glow.png):
  armour    (Space.PLAYER, one root bone per piece, drawn piece by piece by client/aura/SongArmourKit with the Djinn equip animation)
            cowl (a stitched leather cowl over a bone skull mask with hollow glowing eyes and a nail through the crown), corset (a leather corset with bone ribs and
            coffin nails), lid (a coffin lid on the back), bar (the puppeteer's control bar floating above the head with the strings that run down to the head and
            shoulders), chain (a hanging chain with a hook), pauldron_r/l (slate shoulder pads with spikes), vambrace_r/l (bandaged forearms with coffin nails),
            gauntlet_r/l (skeletal hands), greave_r/l (bandaged greaves with chain loops). clip: idle.
  puppet    the marionette (SOUL_CORPSE_1): a stitched corpse puppet with button eyes, jointed limbs, strings to a small control bar above it. clips: idle, walk, attack, collapse
  hand      the grasping hand (SOUL_CORPSE_2): a decayed hand bursting out of the ground with a torn forearm, four fingers and a thumb. clips: idle, grasp
The look: slate blue-grey (0xFF6C7899, the Soul Corpse colour), stitched leather, burlap, bone and rusted iron; the strings and the eyes glow pale violet-blue.
File frame: y up, feet at the origin, the front on -z (see tools/geo_builder.py).
"""
import math
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from song_rebuild_kit import Atlas, material, loop, sin_loop, bob_loop, spin_loop, check   # noqa: E402

KEY = "soulcorpse"
THREAD = 0xA8B4F0

material("leather", 0x3F3B4C, "stitch", glow=THREAD, glow_strength=170, dark=0.55)
material("burlap", 0x8D7C5E, "cloth", glow=THREAD, glow_strength=0, dark=0.6)
material("bone", 0xD9D3BD, "bone", glow=THREAD, glow_strength=0, dark=0.6)
material("slate", 0x586382, "plate", glow=THREAD, glow_strength=0)
material("iron", 0x5E585C, "plate", glow=THREAD, glow_strength=0, dark=0.5)
material("wood", 0x4C3527, "plate", glow=THREAD, glow_strength=0, dark=0.55)
material("thread", 0xB6C0FF, "emit", glow=THREAD, glow_strength=230, dark=0.85, light=1.0)
material("socket", 0x120E1E, "emit", glow=0xB48CFF, glow_strength=255, dark=0.9, light=1.0)
material("flesh", 0x7C8470, "bone", glow=THREAD, glow_strength=0, dark=0.55)
material("wound", 0x3A2A3C, "veins", glow=THREAD, glow_strength=120)


def build_armour():
    A = Atlas(KEY, "armour", 256, 256, seed=111)
    c = A.cube

    # ---- COWL: stitched leather over a bone mask
    A.bone("cowl", pivot=(0, 28, 0))
    c("cowl", (-4.6, 27.6, -4.6), (9, 8, 9), "leather", inflate=0.4)
    c("cowl", (-4.4, 24.6, -4.4), (9, 3, 9), "leather", inflate=0.3)
    c("cowl", (-3, 26.5, -5.4), (6, 7, 1), "bone")
    c("cowl", (-2.5, 29.5, -5.8), (2, 2, 1), "socket")
    c("cowl", (0.5, 29.5, -5.8), (2, 2, 1), "socket")
    c("cowl", (-0.5, 28, -5.8), (1, 1, 1), "wound")
    for k in range(4):
        c("cowl", (-2 + k * 1.3, 26.6, -5.8), (1, 1, 1), "wound", share="tooth")
    c("cowl", (-0.5, 35.5, -0.5), (1, 4, 1), "iron")                                     # a nail through the crown
    c("cowl", (-1.5, 39.2, -1.5), (3, 1, 3), "iron")

    # ---- CORSET: leather corset with bone ribs and coffin nails
    A.bone("corset", pivot=(0, 18, 0))
    c("corset", (-4.6, 14, -3.3), (9, 9, 2), "leather", inflate=0.2)
    for k in range(3):
        c("corset", (-4.4, 15.5 + k * 2.6, -4.1), (8.8, 1, 1), "bone", share="crib")
        c("corset", (-0.5, 14.5, -4.1), (1, 8, 1), "bone", share="cster")
    for k in range(4):                                                                   # lacing and nails
        c("corset", (-3.6 + k * 2.4, 14.5, -4.3), (1, 1, 1), "iron", share="nail")
    c("corset", (-4.7, 12.4, -3.0), (9, 2, 6), "leather", inflate=0.3)
    c("corset", (-1.5, 12.2, -3.7), (3, 2, 1), "iron")
    c("corset", (-3.5, 24, -2.5), (7, 2, 5), "leather", inflate=0.1)
    c("corset", (-4.6, 14, 1.4), (9, 9, 2), "leather", inflate=0.2)
    c("corset", (-6.2, 22.2, -2.4), (2, 2, 5), "leather", inflate=0.1)
    c("corset", (4.2, 22.2, -2.4), (2, 2, 5), "leather", inflate=0.1)

    # ---- the coffin lid on the back
    A.bone("lid", pivot=(0, 18, 3))
    c("lid", (-4.5, 10, 2.6), (9, 15, 1), "wood", inflate=0.2)
    c("lid", (-4.7, 12, 3.7), (9.4, 1, 1), "iron", share="lidband")
    c("lid", (-4.7, 20, 3.7), (9.4, 1, 1), "iron", share="lidband")
    c("lid", (-0.5, 12, 3.8), (1, 9, 1), "bone")
    c("lid", (-2.5, 16, 3.8), (5, 1, 1), "bone")

    # ---- the puppeteer's control bar above the head, with the strings
    A.bone("bar", pivot=(0, 44, 0))
    c("bar", (-8, 43, -1), (16, 1, 2), "wood")
    c("bar", (-0.5, 43, -4), (1, 1, 8), "wood")
    c("bar", (-8.4, 42.6, -1.4), (1, 2, 3), "iron", share="bend")
    c("bar", (7.4, 42.6, -1.4), (1, 2, 3), "iron", share="bend")
    c("bar", (-0.5, 33, -0.5), (1, 10, 1), "thread")                                      # to the head
    c("bar", (-7.5, 26, -0.5), (1, 17, 1), "thread")                                      # to the shoulders
    c("bar", (6.5, 26, -0.5), (1, 17, 1), "thread")
    c("bar", (-0.5, 33, -3.5), (1, 10, 1), "thread")
    c("bar", (-0.5, 33, 2.5), (1, 10, 1), "thread")

    # ---- the hanging chain with a hook
    A.bone("chain", pivot=(0, 12, 3))
    for k in range(7):
        c("chain", (-0.5, 10 - k * 2, 3 + (k % 2) * 0.6), (1, 2, 1), "iron", share="link%d" % (k % 2))
    c("chain", (-0.5, -3.4, 3), (1, 2, 2), "iron")
    c("chain", (-0.5, -4.4, 4), (1, 1, 2), "iron")

    # ---- PAULDRONS (slate pads with spikes), VAMBRACES (bandages with coffin nails), GAUNTLETS (skeletal hands)
    for sg, tag in ((-1, "r"), (1, "l")):
        sx = 6.0 * sg
        A.bone("pauldron_" + tag, pivot=(sx, 22, 0))
        c("pauldron_" + tag, (sx - 3, 22.2, -3), (6, 3, 6), "slate", inflate=0.3)
        for k in range(2):
            c("pauldron_" + tag, (sx - 0.5 + (k * 2 - 1) * 1.5 * sg, 25, -0.5), (1, 4, 1), "iron", rot=(0, 0, -sg * 10), share="spk")
        c("pauldron_" + tag, (sx - 1.5, 19.4, -3.6), (3, 3, 1), "leather")
        A.bone("vambrace_" + tag, pivot=(sx, 18, 0))
        ox = sx - 2.5
        c("vambrace_" + tag, (ox, 14.2, -2.7), (5, 5, 5), "burlap", inflate=0.2)
        c("vambrace_" + tag, (ox - 0.2, 16.2, -3.2), (5.4, 1, 6), "leather", inflate=0.05)
        c("vambrace_" + tag, (ox + 1, 17.2, -3.4), (1, 1, 2), "iron")
        c("vambrace_" + tag, (ox + 3, 15.2, -3.4), (1, 1, 2), "iron")
        A.bone("gauntlet_" + tag, pivot=(sx, 13, 0))
        c("gauntlet_" + tag, (ox - 0.2, 11.8, -2.7), (5, 2, 5), "bone", inflate=0.2)
        for k in range(4):
            c("gauntlet_" + tag, (ox + 0.2 + k * 1.2, 8.8, -3.2), (1, 3, 1), "bone", rot=(-12, 0, 0), pivot=(ox + 0.7 + k * 1.2, 11.5, -3.3), share="fing")
            c("gauntlet_" + tag, (ox + 0.2 + k * 1.2, 8.2, -3.4), (1, 1, 1), "thread", share="tip")
    # ---- GREAVES
    for sg, tag in ((-1, "r"), (1, "l")):
        lx = -2.0 if sg < 0 else 2.0
        A.bone("greave_" + tag, pivot=(lx, 6, 0))
        c("greave_" + tag, (lx - 2.3, 2.5, -2.8), (4, 8, 1), "burlap", inflate=0.3)
        c("greave_" + tag, (lx - 2.6, 9.4, -2.6), (5, 3, 5), "bone", inflate=0.3)
        c("greave_" + tag, (lx - 2.8, 1.6, -2.8), (5, 1, 5), "iron", inflate=0.4)
        c("greave_" + tag, (lx - 2.6, 0.2, -3.6), (5, 1, 6), "leather", inflate=0.1)
        c("greave_" + tag, (lx - 2.8, 5, 1.6), (5, 5, 1), "leather", inflate=0.2)
        c("greave_" + tag, (lx - 0.5, 5.5, -3.7), (1, 1, 1), "iron")

    A.anim.clip("idle", length=3.0, loop=True)
    A.anim.rot("idle", "bar", sin_loop(3.0, 3.0, ax=2))
    A.anim.rot("idle", "chain", sin_loop(3.0, 6.0, ax=0))
    A.anim.rot("idle", "lid", sin_loop(3.0, 1.0, ax=0))
    return A.save()


def build_puppet():
    A = Atlas(KEY, "puppet", 128, 128, seed=121)
    c = A.cube
    A.bone("root", pivot=(0, 0, 0))
    A.bone("body", "root", (0, 12, 0))
    A.bone("head", "body", (0, 24, 0))
    A.bone("arm_r", "body", (-5, 22, 0))
    A.bone("arm_l", "body", (5, 22, 0))
    A.bone("leg_r", "root", (-2, 12, 0))
    A.bone("leg_l", "root", (2, 12, 0))
    A.bone("bar", "root", (0, 46, 0))
    c("body", (-4, 12, -2), (8, 12, 4), "leather", inflate=0.1)
    c("body", (-4.2, 13, -2.4), (8.4, 2, 4.8), "burlap")
    for k in range(3):
        c("body", (-3, 15 + k * 2.4, -2.5), (6, 1, 1), "bone", share="pr")
    c("body", (-0.5, 12.5, -2.6), (1, 10, 1), "wound")                                    # the seam
    c("head", (-3.5, 24, -3.5), (7, 7, 7), "burlap")
    c("head", (-3.9, 23.6, -3.9), (7.8, 3, 7.8), "leather")
    c("head", (-2.5, 27, -3.9), (2, 2, 1), "socket")
    c("head", (0.5, 27, -3.9), (2, 2, 1), "socket")
    c("head", (-1.5, 25, -3.9), (3, 1, 1), "wound")
    c("head", (-0.5, 31, -0.5), (1, 3, 1), "iron")                                         # a nail in the crown for the string
    for sg, nm in ((-1, "arm_r"), (1, "arm_l")):
        x = 5.0 * sg
        c(nm, (x - 1.5 if sg > 0 else x - 1.5, 12, -2), (3, 10, 3), "burlap", inflate=0.05)
        c(nm, (x - 1.5, 10.5, -2), (3, 2, 3), "bone")
        c(nm, (x - 0.5, 20.5, -0.5), (1, 6, 1), "thread")
    for sg, nm in ((-1, "leg_r"), (1, "leg_l")):
        x = 2.0 * sg
        c(nm, (x - 2 if sg > 0 else x - 2, 0, -2), (4, 12, 4), "burlap", inflate=0.05)
        c(nm, (x - 2, 5.5, -2.2), (4, 2, 4.4), "leather")
        c(nm, (x - 2.1, 0, -2.6), (4.2, 1, 5), "iron")
    c("bar", (-6, 45, -0.5), (12, 1, 1), "wood")
    c("bar", (-0.5, 45, -3), (1, 1, 6), "wood")
    for x in (-5.5, 5.5):
        c("bar", (x - 0.5, 21, -0.5), (1, 24, 1), "thread", share="ps")
    c("bar", (-0.5, 31, -0.5), (1, 14, 1), "thread")
    A.anim.clip("idle", length=3.0, loop=True)
    A.anim.rot("idle", "bar", sin_loop(3.0, 3.0, ax=2))
    A.anim.rot("idle", "arm_r", sin_loop(3.0, 6.0, ax=0))
    A.anim.rot("idle", "arm_l", sin_loop(3.0, -6.0, ax=0))
    A.anim.rot("idle", "head", sin_loop(3.0, 5.0, ax=2))
    A.anim.pos("idle", "root", bob_loop(3.0, 0.8))
    A.anim.clip("walk", length=0.8, loop=True)
    A.anim.rot("walk", "leg_r", loop(0.8, lambda p: (28 * math.sin(p * 2 * math.pi), 0, 0)))
    A.anim.rot("walk", "leg_l", loop(0.8, lambda p: (-28 * math.sin(p * 2 * math.pi), 0, 0)))
    A.anim.rot("walk", "arm_r", loop(0.8, lambda p: (-22 * math.sin(p * 2 * math.pi), 0, 0)))
    A.anim.rot("walk", "arm_l", loop(0.8, lambda p: (22 * math.sin(p * 2 * math.pi), 0, 0)))
    A.anim.rot("walk", "bar", sin_loop(0.8, 5.0, ax=2))
    A.anim.pos("walk", "root", bob_loop(0.8, 1.2))
    A.anim.clip("attack", length=0.6, loop=True)
    A.anim.rot("attack", "arm_r", loop(0.6, lambda p: (-130 * math.sin(p * math.pi), 0, 0)))
    A.anim.rot("attack", "arm_l", loop(0.6, lambda p: (-130 * math.sin(p * math.pi), 0, 0)))
    A.anim.rot("attack", "body", loop(0.6, lambda p: (-14 * math.sin(p * math.pi), 0, 0)))
    A.anim.rot("attack", "head", loop(0.6, lambda p: (10 * math.sin(p * math.pi), 0, 0)))
    A.anim.pos("attack", "root", loop(0.6, lambda p: (0, 0, -3 * math.sin(p * math.pi))))
    A.anim.clip("collapse", length=1.0, loop=False)
    A.anim.rot("collapse", "body", {0.0: (0, 0, 0), 0.5: (-60, 0, 0), 1.0: (-85, 0, 0)})
    A.anim.rot("collapse", "leg_r", {0.0: (0, 0, 0), 1.0: (20, 0, 8)})
    A.anim.rot("collapse", "leg_l", {0.0: (0, 0, 0), 1.0: (-12, 0, -8)})
    A.anim.rot("collapse", "arm_r", {0.0: (0, 0, 0), 1.0: (-30, 0, 40)})
    A.anim.rot("collapse", "arm_l", {0.0: (0, 0, 0), 1.0: (-20, 0, -40)})
    A.anim.pos("collapse", "root", {0.0: (0, 0, 0), 1.0: (0, -9, 0)})
    A.anim.scale("collapse", "bar", {0.0: (1, 1, 1), 0.6: (1, 1, 1), 1.0: (0, 0, 0)})
    return A.save()


def build_hand():
    A = Atlas(KEY, "hand", 128, 128, seed=131)
    c = A.cube
    A.bone("root", pivot=(0, 0, 0))
    A.bone("palm", "root", (0, 22, 0))
    names = []
    for k in range(4):
        A.bone("f%d" % k, "palm", (-4.5 + k * 3, 28, 0))
        A.bone("f%db" % k, "f%d" % k, (-4.5 + k * 3, 33, 0))
        names.append(k)
    A.bone("thumb", "palm", (5, 26, 0))
    for k, (w, d) in enumerate(((9, 9), (8, 8), (7, 7), (7, 7))):                          # the forearm bursting out of the ground
        c("root", (-w / 2.0, k * 5 - 2, -d / 2.0), (w, 6, d), "flesh", rot=(0, 0, 4 * (k % 2 * 2 - 1)), share="fa%d" % k)
    c("root", (-4, 9, -4), (8, 4, 8), "wound")
    c("root", (-3, 17, -3), (6, 5, 6), "flesh")                                             # the wrist
    c("root", (-3.2, 18.5, -3.2), (6.4, 1, 6.4), "wound")
    c("root", (-1, 14, -5), (2, 6, 1), "bone")                                              # a bone sticking out
    c("palm", (-6, 22, -3), (12, 6, 6), "flesh")
    c("palm", (-5.5, 27.5, -2.5), (11, 1, 5), "wound")
    for k in range(4):
        h = 6 - (1 if k in (0, 3) else 0)
        c("f%d" % k, (-5.5 + k * 3, 28, -1.5), (2.4, h, 3), "flesh", share="fg%d" % k)
        c("f%db" % k, (-5.4 + k * 3, 28 + h, -1.4), (2.2, 5, 2.8), "flesh", share="fgb%d" % k)
        c("f%db" % k, (-5.2 + k * 3, 32 + h, -1.0), (1.8, 2, 2), "bone", share="claw")
    c("thumb", (4.5, 24, -2), (3, 5, 3), "flesh")
    c("thumb", (6.5, 28, -1.6), (2.4, 4, 2.8), "flesh", rot=(0, 0, -25), pivot=(6.5, 28, 0))
    A.anim.clip("idle", length=2.0, loop=True)
    for k in range(4):
        A.anim.rot("idle", "f%d" % k, sin_loop(2.0, 6.0, phase=k * 0.15, ax=2))
        A.anim.rot("idle", "f%db" % k, sin_loop(2.0, 10.0, phase=0.2 + k * 0.15, ax=2))
    A.anim.rot("idle", "palm", sin_loop(2.0, 3.0, ax=2))
    A.anim.clip("grasp", length=0.8, loop=True)
    for k in range(4):
        A.anim.rot("grasp", "f%d" % k, loop(0.8, lambda p: (-70 * math.sin(p * math.pi), 0, 0)))
        A.anim.rot("grasp", "f%db" % k, loop(0.8, lambda p: (-80 * math.sin(p * math.pi), 0, 0)))
    A.anim.rot("grasp", "thumb", loop(0.8, lambda p: (-50 * math.sin(p * math.pi), 0, 30 * math.sin(p * math.pi))))
    A.anim.pos("grasp", "palm", loop(0.8, lambda p: (0, 3 * math.sin(p * math.pi), 0)))
    return A.save()


def build_nail():
    A = Atlas(KEY, "nail", 64, 64, seed=141)
    c = A.cube
    A.bone("root", pivot=(0, 8, 0))
    A.bone("nail", "root", (0, 8, 0))
    A.bone("ring", "root", (0, 8, 0))
    c("nail", (-0.5, 7.5, -7), (1, 1, 10), "iron")                                          # the shaft, point towards -z
    c("nail", (-0.5, 7.5, -9), (1, 1, 2), "bone")
    c("nail", (-1.5, 6.5, 3), (3, 3, 1), "iron")                                            # the head
    c("nail", (-0.5, 7.5, 4), (1, 1, 12), "thread")                                         # the thread trailing behind
    c("ring", (-2.5, 5.5, -1), (5, 5, 1), "thread", share="stitch")
    c("ring", (-2.5, 5.5, -4), (5, 5, 1), "thread", share="stitch")
    A.anim.clip("idle", length=1.0, loop=True)
    A.anim.rot("idle", "ring", loop(1.0, lambda p: (0, 0, 360 * p)))
    A.anim.clip("strike", length=0.3, loop=True)
    A.anim.rot("strike", "nail", loop(0.3, lambda p: (0, 0, 360 * p)))
    A.anim.rot("strike", "ring", loop(0.3, lambda p: (0, 0, -720 * p)))
    return A.save()


if __name__ == "__main__":
    ids = [build_armour(), build_puppet(), build_hand(), build_nail()]
    check(*ids)
