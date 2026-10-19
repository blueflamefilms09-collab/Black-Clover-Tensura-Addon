"""Soul Magic (1.93.0 rebuild) geo models: the Spirit Raiment armour, the soul wisp and the soul-flame orb.

    python3 -B tools/gen_soul_rebuild_geo.py        # writes every model, animation and texture, runs tools/geo_check.py

Models (assets/nusmp/geo/entity/soul_<name>.geo.json, animations/entity/..., textures/entity/soul_<name>.png + _glow.png):
  armour    (Space.PLAYER, one root bone per piece, drawn piece by piece by client/aura/SongArmourKit with the Djinn equip animation)
            hood (a spectral hood with a bone mask, hollow glowing eyes and two flames for horns), ribs (a ribcage of bone bars round a soul core),
            wing_r / wing_l (wings of soul fire that beat), wisps (three small souls orbiting the torso), tail (a tail of soul fire), pauldron_r/l
            (flame-headed shoulders over a bone cap), vambrace_r/l (chain rings), gauntlet_r/l (spectral claws), greave_r/l (shins that burn away
            into a flame under the feet). The ghostly pieces are drawn translucent; the bone pieces solid. clip: idle.
  wisp      the soul spirit (SOUL_1): a skull face with hollow eyes, a tapering body of stepped soul-cloth, trailing arms, a flame core, horns of fire and a tail.
            clips: idle (floats, sways), attack (lunges forward with the mouth wide)
  orb       the soul-flame orb (SOUL_2): a bright core in a shell of flame cubes with a tail and three orbiting small souls. clips: idle, strike
The look: pale teal soul fire (0xFF72D9CC, the Soul colour) over bone white and hollow dark: flames glow, veins run through the cloth, eyes are empty and bright.
File frame: y up, feet at the origin, the front on -z (see tools/geo_builder.py).
"""
import math
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from song_rebuild_kit import Atlas, material, loop, sin_loop, bob_loop, spin_loop, check   # noqa: E402

KEY = "soul"
TEAL = 0x72D9CC

material("spirit", 0xB8EFE6, "wisp", glow=TEAL, glow_strength=170, alpha=205, dark=0.78, light=1.1)
material("robe", 0x86D2C8, "veins", glow=TEAL, glow_strength=140, alpha=215, dark=0.7, light=1.12)
material("bone", 0xE6E2D2, "bone", glow=TEAL, glow_strength=0, dark=0.6)
material("flame", 0x66E0D0, "flame", glow=TEAL, glow_strength=255, alpha=235, light=1.2, spec=0xF0FFFA)
material("core", 0xBFFFF4, "core", glow=0xA8FFF0, glow_strength=255)
material("hollow", 0x0E2428, "emit", glow=0x8CFFF0, glow_strength=255, dark=0.9, light=1.0)
material("chain", 0x93AEA9, "plate", glow=TEAL, glow_strength=30)
material("hollowdark", 0x10282C, "plain", glow=TEAL, glow_strength=0)


def flame(A, bone, base, h, w, mat="flame", lean=(0, 0, 0), layers=3, share=None):
    """A stepped flame: `layers` cubes rising from `base`, narrowing, optionally leaning (rotation about the foot)."""
    n = 0
    seg = max(1, int(round(h / float(layers))))
    for k in range(layers):
        ww = max(1, int(round(w * (1 - k / float(layers)))))
        A.cube(bone, (base[0] - ww / 2.0, base[1] + k * seg, base[2] - ww / 2.0), (ww, seg, ww), mat, rot=lean if any(lean) else None,
               pivot=(base[0], base[1], base[2]) if any(lean) else None, share=(share + str(k)) if share else None)
        n += 1
    return n


def build_armour():
    A = Atlas(KEY, "armour", 256, 256, seed=81)
    c = A.cube

    # ---- HOOD: spectral hood, bone mask, hollow eyes, flame horns
    A.bone("hood", pivot=(0, 28, 0))
    c("hood", (-4.6, 27.6, -4.6), (9, 8, 9), "spirit", inflate=0.4)
    c("hood", (-3.5, 25.6, -5.2), (7, 2, 1), "spirit")
    c("hood", (-3, 26.5, -5.6), (6, 7, 1), "bone")                                      # the mask
    c("hood", (-2.5, 29.5, -5.9), (2, 2, 1), "hollow")
    c("hood", (0.5, 29.5, -5.9), (2, 2, 1), "hollow")
    c("hood", (-0.5, 28.2, -5.9), (1, 1, 1), "hollowdark")
    c("hood", (-2, 26.6, -5.9), (4, 1, 1), "hollowdark")
    for sg in (-1, 1):
        flame(A, "hood", (3.2 * sg, 35.3, -0.5), 8, 3, "flame", lean=(0, 0, -16 * sg), share="horn")
    flame(A, "hood", (0, 36, 2.5), 6, 3, "flame", lean=(-14, 0, 0), share="crest")

    # ---- RIBS: a ribcage of bone bars round a soul core
    A.bone("ribs", pivot=(0, 18, 0))
    c("ribs", (-4.5, 14, -3.2), (9, 9, 1), "robe", inflate=0.2)
    for k in range(4):
        y = 15 + k * 2.2
        c("ribs", (-4.6, y, -3.9), (9, 1, 1), "bone", share="rib")
        c("ribs", (-5.0, y, -3.0), (1, 1, 5), "bone", share="ribs")
        c("ribs", (4.0, y, -3.0), (1, 1, 5), "bone", share="ribs")
    c("ribs", (-0.5, 14.5, -4.4), (1, 8, 1), "bone")                                    # sternum
    c("ribs", (-1.5, 17.5, -4.4), (3, 3, 1), "core")                                    # the soul core
    c("ribs", (-4.6, 14, 1.5), (9, 9, 1), "robe", inflate=0.2)
    c("ribs", (-4.6, 22.5, -2.5), (9, 2, 5), "spirit", inflate=0.2)                      # a mantle at the neck
    for k in range(3):                                                                  # the spine on the back
        c("ribs", (-0.5, 15 + k * 2.6, 2.6), (1, 2, 1), "bone", share="spine")

    # ---- WINGS of soul fire
    for sg, tag in ((-1, "r"), (1, "l")):
        name = "wing_" + tag
        A.bone(name, pivot=(2.5 * sg, 22, 3))
        for k, (ang, ln) in enumerate(((52, 15), (32, 13), (12, 10), (-8, 8))):
            flame(A, name, (2.5 * sg, 22 - k * 1.6, 3.2), ln, 3 if k < 2 else 2, "flame", lean=(0, -sg * ang, -sg * (34 - k * 6)), share="wf%d" % k)
        c(name, (2 * sg - 1, 21, 2.6), (2, 4, 1), "bone")

    # ---- three small souls orbiting
    A.bone("wisps", pivot=(0, 18, 0))
    for ang, y in ((0, 22), (120, 15), (240, 27)):
        r = 11.0
        cx, cz = r * math.sin(math.radians(ang)), r * math.cos(math.radians(ang))
        c("wisps", (cx - 1.5, y - 1.5, cz - 1.5), (3, 3, 3), "core", share="ws")
        flame(A, "wisps", (cx, y + 1.5, cz), 5, 2, "flame", lean=(0, 0, 10), share="wsf")

    # ---- tail of soul fire
    A.bone("tail", pivot=(0, 12, 3))
    for k in range(5):
        w = 5 - k
        c("tail", (-w / 2.0 + 1.5 * math.sin(k * 1.1), 10 - k * 2.6, 3 + k * 1.3), (w, 3, w), "flame", rot=(14, 0, 0), pivot=(0, 12, 3), share="tl%d" % k)

    # ---- PAULDRONS (flame heads on a bone cap), VAMBRACES (chain rings), GAUNTLETS (spectral claws)
    for sg, tag in ((-1, "r"), (1, "l")):
        sx = 6.0 * sg
        A.bone("pauldron_" + tag, pivot=(sx, 22, 0))
        c("pauldron_" + tag, (sx - 3, 22.2, -3), (6, 3, 6), "bone", inflate=0.3)
        flame(A, "pauldron_" + tag, (sx, 25.4, 0), 7, 4, "flame", lean=(0, 0, -sg * 14), share="pf")
        c("pauldron_" + tag, (sx - 1.5, 19.2, -3.6), (3, 3, 1), "robe")
        A.bone("vambrace_" + tag, pivot=(sx, 18, 0))
        ox = sx - 2.5
        c("vambrace_" + tag, (ox, 14.2, -2.7), (5, 5, 5), "robe", inflate=0.2)
        for k in range(3):
            c("vambrace_" + tag, (ox - 0.4, 14.6 + k * 1.6, -3.1), (6, 1, 6), "chain", inflate=0.05, share="cr")
        A.bone("gauntlet_" + tag, pivot=(sx, 13, 0))
        c("gauntlet_" + tag, (ox - 0.2, 11.8, -2.7), (5, 2, 5), "bone", inflate=0.25)
        for k in range(4):
            c("gauntlet_" + tag, (ox + 0.2 + k * 1.2, 9.0, -3.3), (1, 3, 1), "bone", rot=(-14, 0, 0), pivot=(ox + 0.7 + k * 1.2, 11.5, -3.3), share="claw")
    # ---- GREAVES: shins that burn away into a flame under the feet
    for sg, tag in ((-1, "r"), (1, "l")):
        lx = -2.0 if sg < 0 else 2.0
        A.bone("greave_" + tag, pivot=(lx, 6, 0))
        c("greave_" + tag, (lx - 2.3, 3, -2.8), (4, 8, 1), "robe", inflate=0.3)
        c("greave_" + tag, (lx - 2.6, 9.4, -2.6), (5, 3, 5), "bone", inflate=0.3)
        c("greave_" + tag, (lx - 2.8, 1.5, -2.8), (5, 2, 5), "spirit", inflate=0.4)
        flame(A, "greave_" + tag, (lx, -3.5, 0), 5, 4, "flame", share="gf")
        c("greave_" + tag, (lx - 2.8, 5, 1.6), (5, 5, 1), "robe", inflate=0.2)

    A.anim.clip("idle", length=3.0, loop=True)
    A.anim.rot("idle", "wing_r", loop(3.0, lambda p: (0, 0, 8 * math.sin(p * 2 * math.pi))))
    A.anim.rot("idle", "wing_l", loop(3.0, lambda p: (0, 0, -8 * math.sin(p * 2 * math.pi))))
    A.anim.rot("idle", "wisps", loop(3.0, lambda p: (0, 360 * p, 0)))
    A.anim.pos("idle", "wisps", bob_loop(3.0, 1.0))
    A.anim.rot("idle", "tail", sin_loop(3.0, 7.0, ax=0))
    A.anim.rot("idle", "hood", sin_loop(3.0, 1.2, ax=2))
    return A.save()


def build_wisp():
    A = Atlas(KEY, "wisp", 128, 128, seed=91)
    c = A.cube
    A.bone("root", pivot=(0, 0, 0))
    A.bone("body", "root", (0, 14, 0))
    A.bone("head", "body", (0, 28, 0))
    A.bone("jaw", "head", (0, 27, -2))
    A.bone("arm_r", "body", (-6, 26, 0))
    A.bone("arm_l", "body", (6, 26, 0))
    A.bone("tail", "root", (0, 12, 0))
    A.bone("halo", "head", (0, 38, 0))
    for k, (w, d) in enumerate(((11, 8), (10, 7), (9, 7), (8, 6), (7, 6))):             # a body of stepped soul cloth tapering down
        c("body", (-w / 2.0, 12 + k * 3, -d / 2.0), (w, 3, d), "robe", inflate=0.05)
    c("body", (-1.5, 18, -4.4), (3, 3, 1), "core")                                       # the flame core
    c("body", (-0.5, 14, -4.2), (1, 6, 1), "bone")
    for k in range(4):                                                                   # a few ribs
        c("body", (-3.5, 20 + k * 1.6, -3.9), (7, 1, 1), "bone", share="wr")
    c("head", (-4, 27, -4), (8, 8, 8), "bone")
    c("head", (-4.5, 26.6, -4.5), (9, 4, 9), "spirit", inflate=0.1)
    c("head", (-3, 30, -4.4), (2, 2, 1), "hollow")
    c("head", (1, 30, -4.4), (2, 2, 1), "hollow")
    c("jaw", (-2.5, 25.4, -4.4), (5, 2, 4), "bone")
    c("jaw", (-1.5, 26.4, -4.6), (3, 1, 1), "hollowdark")
    flame(A, "head", (-3, 35, 0), 7, 3, "flame", lean=(0, 0, 14), share="hl")
    flame(A, "head", (3, 35, 0), 7, 3, "flame", lean=(0, 0, -14), share="hl")
    for sg, nm in ((-1, "arm_r"), (1, "arm_l")):                                         # long trailing arms ending in claws
        x = 6.0 * sg
        c(nm, (x - 1.5, 17, -2), (3, 10, 3), "robe", rot=(-22, 0, 0), pivot=(x, 26, 0), share="wa")
        for k in range(3):
            c(nm, (x - 1.5 + k, 12.5, -6.2), (1, 4, 1), "bone", rot=(-22, 0, 0), pivot=(x, 26, 0), share="wc")
    for k, (w, h) in enumerate(((6, 5), (4, 5), (3, 4))):                                # the tail
        c("tail", (-w / 2.0, 8 - k * 4.5, -w / 2.0), (w, h, w), "spirit", rot=(0, 0, 7 * (k % 2 * 2 - 1)))
    flame(A, "tail", (0, -6, 0), 4, 3, "flame", share="tf")
    for k in range(6):                                                                   # a halo of six small embers
        a = k * math.pi / 3
        c("halo", (6 * math.cos(a) - 0.5, 38, 6 * math.sin(a) - 0.5), (1, 1, 1), "flame", share="he")
    A.anim.clip("idle", length=3.0, loop=True)
    A.anim.pos("idle", "root", bob_loop(3.0, 1.6))
    A.anim.rot("idle", "body", sin_loop(3.0, 3.0, ax=2))
    A.anim.rot("idle", "arm_r", sin_loop(3.0, 8.0, ax=0))
    A.anim.rot("idle", "arm_l", sin_loop(3.0, -8.0, ax=0))
    A.anim.rot("idle", "tail", sin_loop(3.0, 9.0, ax=2))
    A.anim.rot("idle", "halo", loop(3.0, lambda p: (0, 360 * p, 0)))
    A.anim.clip("attack", length=0.8, loop=True)
    A.anim.rot("attack", "body", loop(0.8, lambda p: (-24 * math.sin(p * math.pi), 0, 0)))
    A.anim.rot("attack", "head", loop(0.8, lambda p: (10 * math.sin(p * math.pi), 0, 0)))
    A.anim.rot("attack", "jaw", loop(0.8, lambda p: (28 * math.sin(p * math.pi), 0, 0)))
    A.anim.rot("attack", "arm_r", loop(0.8, lambda p: (-70 * math.sin(p * math.pi), 0, 0)))
    A.anim.rot("attack", "arm_l", loop(0.8, lambda p: (-70 * math.sin(p * math.pi), 0, 0)))
    A.anim.pos("attack", "root", loop(0.8, lambda p: (0, 0, -3 * math.sin(p * math.pi))))
    A.anim.rot("attack", "halo", loop(0.8, lambda p: (0, 720 * p, 0)))
    return A.save()


def build_orb():
    A = Atlas(KEY, "orb", 128, 128, seed=101)
    c = A.cube
    A.bone("root", pivot=(0, 8, 0))
    A.bone("core", "root", (0, 8, 0))
    A.bone("shell", "root", (0, 8, 0))
    A.bone("tail", "root", (0, 8, 0))
    A.bone("m1", "root", (0, 8, 0))
    A.bone("m2", "root", (0, 8, 0))
    A.bone("m3", "root", (0, 8, 0))
    c("core", (-3, 5, -3), (6, 6, 6), "core")
    c("core", (-2, 4, -2), (4, 8, 4), "core", rot=(0, 45, 0))
    c("shell", (-4, 4, -4), (8, 8, 8), "flame", inflate=0.4)
    c("shell", (-3, 3, -3), (6, 10, 6), "flame", rot=(0, 45, 0), inflate=0.3)
    for k, (w, d) in enumerate(((6, 4), (5, 4), (4, 4), (3, 3), (2, 3))):               # the tail streaming back (+z)
        c("tail", (-w / 2.0, 5 + 0.3 * k, 4 + k * 4), (w, w, d), "flame", share="ot%d" % k)
    for tag, ang in (("m1", 0), ("m2", 120), ("m3", 240)):
        cx, cz = 7 * math.sin(math.radians(ang)), 7 * math.cos(math.radians(ang))
        c(tag, (cx - 1.5, 6.5, cz - 1.5), (3, 3, 3), "core", share="om")
        c(tag, (cx - 1, 9.5, cz - 1), (2, 3, 2), "flame", share="omf")
    A.anim.clip("idle", length=2.0, loop=True)
    A.anim.rot("idle", "shell", spin_loop(2.0, 1, ax=1))
    A.anim.scale("idle", "core", loop(2.0, lambda p: (1 + 0.12 * math.sin(p * 4 * math.pi),) * 3))
    for tag, ph in (("m1", 0), ("m2", 1 / 3.0), ("m3", 2 / 3.0)):
        A.anim.rot("idle", tag, loop(2.0, lambda p: (0, 360 * p, 0)))
    A.anim.clip("strike", length=0.4, loop=True)
    A.anim.rot("strike", "shell", spin_loop(0.4, 2, ax=1))
    for tag in ("m1", "m2", "m3"):
        A.anim.rot("strike", tag, loop(0.4, lambda p: (0, 720 * p, 0)))
    A.anim.scale("strike", "core", loop(0.4, lambda p: (1 + 0.2 * math.sin(p * 4 * math.pi),) * 3))
    return A.save()


if __name__ == "__main__":
    ids = [build_armour(), build_wisp(), build_orb()]
    check(*ids)
