"""Memory Magic (1.93.0 rebuild) geo models: the Reel Vestments armour, the memory echo and the recollection orb.

    python3 -B tools/gen_memory_rebuild_geo.py        # writes every model, animation and texture, runs tools/geo_check.py

Models (assets/nusmp/geo/entity/memory_<name>.geo.json, animations/entity/..., textures/entity/memory_<name>.png + _glow.png):
  armour    (Space.PLAYER, one root bone per piece, drawn piece by piece by client/aura/SongArmourKit with the Djinn equip animation)
            crown (a film-strip band round the head with a camera lens for a third eye and two photo-frame fins), shards (three glass shards turning over the head),
            book (a cuirass of pages bound in gold with a lens-locket), reel (a film reel standing behind the back, turning), film (a strip of film hanging from the reel
            down the back), mirrors (three hand-mirror shards orbiting the waist), pauldron_r/l (tilted photo frames with glass spikes), vambrace_r/l (film-reel bands),
            gauntlet_r/l (frames with glass fingertips), greave_r/l (stacked pages with a film ankle strip). clip: idle.
  echo      the memory echo (MEMORY_1): a translucent afterimage of a person in a film frame, flickering. clips: idle, strike
  orb       the recollection orb (MEMORY_2): a photograph in a frame with a glowing picture, three small frames orbiting it. clips: idle, strike
The look: lavender white (0xFFBCA8FF, the Memory colour), film black with sprocket holes, paper, gold trim and mirror glass; the lit frames and the lens glow.
File frame: y up, feet at the origin, the front on -z (see tools/geo_builder.py).
"""
import math
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from song_rebuild_kit import Atlas, material, loop, sin_loop, bob_loop, spin_loop, check   # noqa: E402

KEY = "memory"
LAV = 0xBCA8FF

material("film", 0x1D1A2C, "film", glow=LAV, glow_strength=170, dark=0.6)
material("frame", 0xD7CFFF, "plate", glow=LAV, glow_strength=0, spec=0xFFFFFF)
material("page", 0xF0EAD4, "brushed", glow=LAV, glow_strength=0, dark=0.75)
material("trim", 0xE2C27A, "plate", glow=LAV, glow_strength=0, spec=0xFFF3C8)
material("glass", 0xCDC2FF, "glass", glow=LAV, glow_strength=200, alpha=190, dark=0.8, light=1.15)
material("lens", 0x140F28, "core", glow=0xC8B8FF, glow_strength=255, dark=0.7)
material("pic", 0xBFB0FF, "emit", glow=LAV, glow_strength=235, dark=0.8, light=1.0)
material("ghost", 0xCFC6FF, "wisp", glow=LAV, glow_strength=130, alpha=185, dark=0.75, light=1.1)
material("ghostfilm", 0x7E70C0, "film", glow=LAV, glow_strength=150, alpha=200, dark=0.7)


def build_armour():
    A = Atlas(KEY, "armour", 256, 256, seed=151)
    c = A.cube

    # ---- CROWN: film strip band, camera-lens third eye, photo-frame fins
    A.bone("crown", pivot=(0, 28, 0))
    for (o, s) in (((-4.6, 28.2, -4.6), (9, 3, 1)), ((-4.6, 28.2, 3.6), (9, 3, 1)), ((-4.6, 28.2, -3.6), (1, 3, 7)), ((3.6, 28.2, -3.6), (1, 3, 7))):
        c("crown", o, s, "film", inflate=0.25)
    c("crown", (-1.8, 28.2, -5.4), (3.6, 3, 1), "frame", inflate=0.1)                    # the lens housing
    c("crown", (-1.2, 28.8, -5.9), (2.4, 1.8, 1), "lens")
    for sg in (-1, 1):
        c("crown", (5.4 * sg - 0.5, 28, -2.5), (1, 6, 5), "frame", rot=(0, 0, -14 * sg), share="fin")
        c("crown", (5.4 * sg - 0.5 + 0.1, 29, -1.5), (1, 4, 3), "pic", rot=(0, 0, -14 * sg), share="finp")

    # ---- SHARDS over the head
    A.bone("shards", pivot=(0, 36, 0))
    for k, ang in enumerate((0, 120, 240)):
        r = 5.0
        cx, cz = r * math.sin(math.radians(ang)), r * math.cos(math.radians(ang))
        c("shards", (cx - 1, 36 + k * 1.5, cz - 0.5), (2, 5, 1), "glass", rot=(0, ang, 10), share="sh%d" % k)
        c("shards", (cx - 0.5, 41 + k * 1.5, cz - 0.25), (1, 2, 1), "glass", rot=(0, ang, 10), share="shp%d" % k)

    # ---- BOOK: a cuirass of pages bound in gold, with a lens-locket
    A.bone("book", pivot=(0, 18, 0))
    c("book", (-4.6, 14, -3.3), (9, 9, 2), "page", inflate=0.2)
    c("book", (-4.8, 13.8, -3.5), (9.6, 9.4, 1), "trim", inflate=0.1)
    c("book", (-4.2, 14.4, -3.9), (8.4, 8.4, 1), "page")
    for k in range(4):
        c("book", (-3.6, 15.2 + k * 2.0, -4.1), (7.2, 1, 1), "film", share="line%d" % (k % 2))
    c("book", (-1.5, 17.5, -4.6), (3, 3, 1), "lens")                                      # the lens-locket
    c("book", (-2, 17, -4.5), (4, 4, 1), "trim", inflate=0.05)
    c("book", (-4.7, 12.4, -3.0), (9, 2, 6), "film", inflate=0.3)
    c("book", (-3.5, 24, -2.5), (7, 2, 5), "trim", inflate=0.1)
    c("book", (-4.6, 14, 1.4), (9, 9, 2), "page", inflate=0.2)
    c("book", (-6.2, 22.2, -2.4), (2, 2, 5), "trim", inflate=0.1)
    c("book", (4.2, 22.2, -2.4), (2, 2, 5), "trim", inflate=0.1)

    # ---- the REEL behind the back (turns) and the FILM strip hanging from it
    A.bone("reel", pivot=(0, 21, 6))
    A.ring("reel", (0, 21, 6.5), 8, 1, 2, 12, "frame", plane="xy", share="reelrim")
    for k in range(6):
        a = k * math.pi / 3
        c("reel", (6 * math.cos(a) * 0.5 - 0.5 + 0.0, 21 + 6 * math.sin(a) * 0.5 - 0.5, 6), (1, 1, 1), "trim", share="sp")
        c("reel", (3 * math.cos(a) - 1.5, 21 + 3 * math.sin(a) - 1.5, 6.2), (3, 3, 1), "film", share="hole")
    c("reel", (-1.5, 19.5, 5.7), (3, 3, 2), "trim")
    A.bone("film", pivot=(0, 14, 3))
    c("film", (-2, 0, 3.4), (4, 15, 1), "film", rot=(6, 0, 0), pivot=(0, 14, 3.4), share="strip")
    c("film", (-2, -3, 4.2), (4, 4, 1), "pic", rot=(6, 0, 0), pivot=(0, 14, 3.4))

    # ---- three hand-mirror shards orbiting the waist
    A.bone("mirrors", pivot=(0, 14, 0))
    for ang, y in ((0, 14), (120, 11), (240, 17)):
        r = 9.0
        cx, cz = r * math.sin(math.radians(ang)), r * math.cos(math.radians(ang))
        c("mirrors", (cx - 1.5, y - 2, cz - 0.5), (3, 4, 1), "glass", rot=(0, ang, 0), share="mi")
        c("mirrors", (cx - 0.5, y - 3.5, cz - 0.25), (1, 2, 1), "trim", rot=(0, ang, 0), share="mih")

    # ---- PAULDRONS (tilted photo frames with glass spikes), VAMBRACES (reel bands), GAUNTLETS (frames with glass fingertips)
    for sg, tag in ((-1, "r"), (1, "l")):
        sx = 6.0 * sg
        A.bone("pauldron_" + tag, pivot=(sx, 22, 0))
        c("pauldron_" + tag, (sx - 3, 22.2, -3), (6, 3, 6), "trim", inflate=0.3)
        c("pauldron_" + tag, (sx - 2.5, 25, -3.5), (5, 5, 1), "frame", rot=(0, 0, -sg * 22), pivot=(sx, 24, 0))
        c("pauldron_" + tag, (sx - 1.8, 25.7, -3.9), (3.6, 3.6, 1), "pic", rot=(0, 0, -sg * 22), pivot=(sx, 24, 0))
        for k in range(2):
            c("pauldron_" + tag, (sx - 0.5 + (k * 2 - 1) * sg * 1.5, 25, 0.5), (1, 5, 1), "glass", rot=(0, 0, sg * (12 + k * 8)), pivot=(sx, 24, 0), share="gsp")
        A.bone("vambrace_" + tag, pivot=(sx, 18, 0))
        ox = sx - 2.5
        c("vambrace_" + tag, (ox, 14.2, -2.7), (5, 5, 5), "film", inflate=0.2)
        c("vambrace_" + tag, (ox - 0.3, 13.6, -3.0), (5, 1, 6), "frame", inflate=0.1)
        c("vambrace_" + tag, (ox - 0.3, 19, -3.0), (5, 1, 6), "frame", inflate=0.1)
        A.bone("gauntlet_" + tag, pivot=(sx, 13, 0))
        c("gauntlet_" + tag, (ox - 0.2, 11.6, -2.7), (5, 3, 5), "frame", inflate=0.2)
        for k in range(4):
            c("gauntlet_" + tag, (ox + 0.2 + k * 1.2, 9.6, -3.3), (1, 2, 1), "glass", share="gf")
    # ---- GREAVES: stacked pages with a film strip at the ankle
    for sg, tag in ((-1, "r"), (1, "l")):
        lx = -2.0 if sg < 0 else 2.0
        A.bone("greave_" + tag, pivot=(lx, 6, 0))
        for k in range(3):
            c("greave_" + tag, (lx - 2.3 + 0.2 * k, 3 + k * 2.2, -2.9 - 0.2 * k), (4, 3, 1), "page", inflate=0.2, rot=(0, 0, 3 * (k - 1)), share="gp%d" % k)
        c("greave_" + tag, (lx - 2.6, 9.4, -2.6), (5, 3, 5), "trim", inflate=0.3)
        c("greave_" + tag, (lx - 2.8, 1.6, -2.8), (5, 2, 5), "film", inflate=0.4)
        c("greave_" + tag, (lx - 2.6, 0.2, -3.6), (5, 1, 6), "frame", inflate=0.1)
        c("greave_" + tag, (lx - 2.8, 5, 1.6), (5, 5, 1), "page", inflate=0.2)

    A.anim.clip("idle", length=4.0, loop=True)
    A.anim.rot("idle", "reel", loop(4.0, lambda p: (0, 0, 360 * p)))
    A.anim.rot("idle", "shards", loop(4.0, lambda p: (0, 360 * p, 0)))
    A.anim.rot("idle", "mirrors", loop(4.0, lambda p: (0, -360 * p, 0)))
    A.anim.pos("idle", "mirrors", bob_loop(4.0, 1.0))
    A.anim.rot("idle", "film", sin_loop(4.0, 5.0, ax=0))
    return A.save()


def build_echo():
    A = Atlas(KEY, "echo", 128, 128, seed=161)
    c = A.cube
    A.bone("root", pivot=(0, 0, 0))
    A.bone("body", "root", (0, 12, 0))
    A.bone("head", "body", (0, 24, 0))
    A.bone("arm_r", "body", (-5, 22, 0))
    A.bone("arm_l", "body", (5, 22, 0))
    A.bone("leg_r", "root", (-2, 12, 0))
    A.bone("leg_l", "root", (2, 12, 0))
    A.bone("frame", "root", (0, 0, 0))
    c("body", (-4, 12, -2), (8, 12, 4), "ghost")
    c("head", (-4, 24, -4), (8, 8, 8), "ghost")
    c("head", (-4.3, 23.7, -4.3), (9, 3, 9), "ghostfilm")
    c("head", (-2.5, 27.5, -4.2), (2, 1, 1), "pic")
    c("head", (0.5, 27.5, -4.2), (2, 1, 1), "pic")
    c("arm_r", (-8, 12, -2), (4, 12, 4), "ghost")
    c("arm_l", (4, 12, -2), (4, 12, 4), "ghost")
    c("leg_r", (-4, 0, -2), (4, 12, 4), "ghost")
    c("leg_l", (0, 0, -2), (4, 12, 4), "ghost")
    c("body", (-4.2, 17, -2.3), (8.4, 1, 4.6), "ghostfilm")
    # the film frame round the figure
    c("frame", (-9, -1, -1), (18, 2, 1), "film")
    c("frame", (-9, 38, -1), (18, 2, 1), "film")
    c("frame", (-9, 1, -1), (2, 37, 1), "film")
    c("frame", (7, 1, -1), (2, 37, 1), "film")
    for k in range(7):
        c("frame", (-8.6, 3 + k * 5, -1.4), (1, 2, 1), "pic", share="sp")
        c("frame", (7.6, 3 + k * 5, -1.4), (1, 2, 1), "pic", share="sp")
    A.anim.clip("idle", length=2.0, loop=True)
    A.anim.pos("idle", "root", bob_loop(2.0, 0.8))
    A.anim.scale("idle", "frame", loop(2.0, lambda p: (1.0, 1.0 + 0.01 * math.sin(p * 12 * math.pi), 1.0)))
    A.anim.rot("idle", "arm_r", sin_loop(2.0, 5.0, ax=0))
    A.anim.rot("idle", "arm_l", sin_loop(2.0, -5.0, ax=0))
    A.anim.clip("strike", length=0.6, loop=True)
    A.anim.rot("strike", "arm_r", loop(0.6, lambda p: (-100 * math.sin(p * math.pi), 0, 0)))
    A.anim.rot("strike", "arm_l", loop(0.6, lambda p: (-100 * math.sin(p * math.pi), 0, 0)))
    A.anim.rot("strike", "body", loop(0.6, lambda p: (-12 * math.sin(p * math.pi), 0, 0)))
    A.anim.pos("strike", "root", loop(0.6, lambda p: (0, 0, -3 * math.sin(p * math.pi))))
    A.anim.scale("strike", "frame", loop(0.6, lambda p: (1.0, 1.0 + 0.03 * math.sin(p * 2 * math.pi), 1.0)))
    return A.save()


def build_orb():
    A = Atlas(KEY, "orb", 128, 128, seed=171)
    c = A.cube
    A.bone("root", pivot=(0, 8, 0))
    A.bone("photo", "root", (0, 8, 0))
    A.bone("m1", "root", (0, 8, 0))
    A.bone("m2", "root", (0, 8, 0))
    A.bone("m3", "root", (0, 8, 0))
    c("photo", (-5, 3, -1), (10, 1, 1), "frame")
    c("photo", (-5, 13, -1), (10, 1, 1), "frame")
    c("photo", (-5, 4, -1), (1, 9, 1), "frame")
    c("photo", (4, 4, -1), (1, 9, 1), "frame")
    c("photo", (-4, 4, -0.5), (8, 9, 1), "pic")
    c("photo", (-4, 4, 0.5), (8, 9, 1), "film")
    c("photo", (-3, 5, -1.2), (6, 1, 1), "lens")
    for tag, ang in (("m1", 0), ("m2", 120), ("m3", 240)):
        cx, cz = 8 * math.sin(math.radians(ang)), 8 * math.cos(math.radians(ang))
        c(tag, (cx - 2, 6, cz - 0.5), (4, 4, 1), "frame", rot=(0, ang, 0), share="mf")
        c(tag, (cx - 1.5, 6.5, cz - 0.3), (3, 3, 1), "pic", rot=(0, ang, 0), share="mp")
    A.anim.clip("idle", length=3.0, loop=True)
    A.anim.rot("idle", "photo", sin_loop(3.0, 12.0, ax=1))
    A.anim.pos("idle", "root", bob_loop(3.0, 1.0))
    for tag in ("m1", "m2", "m3"):
        A.anim.rot("idle", tag, loop(3.0, lambda p: (0, 360 * p, 0)))
    A.anim.clip("strike", length=0.4, loop=True)
    A.anim.rot("strike", "photo", loop(0.4, lambda p: (0, 360 * p, 0)))
    for tag in ("m1", "m2", "m3"):
        A.anim.rot("strike", tag, loop(0.4, lambda p: (0, 720 * p, 0)))
    return A.save()


if __name__ == "__main__":
    ids = [build_armour(), build_echo(), build_orb()]
    check(*ids)
