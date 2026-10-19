"""Sound Magic (1.93.0 rebuild) geo models: the Resonance Plate armour, the speaker stack and the tuning-fork bolt.

    python3 -B tools/gen_sound_rebuild_geo.py        # writes every model, animation and texture, runs tools/geo_check.py

Models (assets/nusmp/geo/entity/sound_<name>.geo.json, animations/entity/..., textures/entity/sound_<name>.png + _glow.png):
  armour    (Space.PLAYER, one root bone per piece, drawn piece by piece by client/aura/SongArmourKit with the Djinn equip animation)
            helm (headphones with speaker-cone ear cups, a mic boom, two antenna fins), chest (amp plate: speaker grille, VU meter bars, knobs),
            wave_a / wave_b / wave_c (three arcs of sound behind the back, each pulsing outward), cable (a coiled cable tail with a plug),
            pauldron_r/l (speaker stacks with cones), vambrace_r/l (tuning forks along the forearm), gauntlet_r/l (resonator rings on the knuckles),
            greave_r/l (bass-drum shin plates with an emissive ankle ring). clip: idle.
  speaker   the speaker stack construct: a cabinet with a woofer, a mid driver, a tweeter, a horn, a VU strip and an antenna. clips: idle, pulse (a bass beat)
  fork      the tuning-fork bolt: two steel prongs on a stem with three sound rings; clips: idle, strike (the prongs shake fast)
The look: brushed steel and black rubber with cyan glow (0xFF70C8F0, the Sound colour): speaker cones with a glowing dust cap, grilles that glow behind their holes,
VU bars, a ring of light on every ankle. File frame: y up, feet at the origin, the front on -z (see tools/geo_builder.py).
"""
import math
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from song_rebuild_kit import Atlas, material, loop, sin_loop, bob_loop, spin_loop, check   # noqa: E402

KEY = "sound"
CY = 0x70C8F0

material("steel", 0x8097AE, "brushed", glow=CY, glow_strength=0, spec=0xE6F4FF)
material("steel_dark", 0x39475A, "plate", glow=CY, glow_strength=0)
material("rubber", 0x20242C, "brushed", glow=CY, glow_strength=0, dark=0.6)
material("cone", 0x2C323E, "cone", glow=CY, glow_strength=210)
material("grille", 0x333B49, "grille", glow=CY, glow_strength=200)
material("vu", 0x151A24, "bars", glow=CY, glow_strength=255)
material("fork", 0xC8D8E4, "brushed", glow=CY, glow_strength=40, spec=0xFFFFFF)
material("emit", 0x3AA0D0, "emit", glow=CY, glow_strength=230, dark=0.8, light=1.0)
material("cable", 0x15171D, "ribs", glow=CY, glow_strength=60, dark=0.7)


def arc_cubes(A, bone, centre, radius, a0, a1, step, mat, thick=1, z=0, width=3, share=None):
    """Cubes along an arc in the xy plane (upright, behind the back): angles in degrees, 90 = straight up."""
    n = 0
    a = a0
    while a <= a1 + 1e-6:
        ang = math.radians(a)
        px, py = centre[0] + radius * math.cos(ang), centre[1] + radius * math.sin(ang)
        A.cube(bone, (px - width / 2.0, py - 0.5, z), (width, 1, thick), mat, rot=(0, 0, a - 90), pivot=(px, py, z + thick / 2.0), share=share)
        n += 1
        a += step
    return n


def build_armour():
    A = Atlas(KEY, "armour", 256, 256, seed=51)
    c = A.cube

    # ---- HELM: headphones, mic boom, antenna fins
    A.bone("helm", pivot=(0, 28, 0))
    for k in range(5):                                                                  # the headband: an arch over the crown
        w = 9 - abs(k - 2) * 0
        c("helm", (-4.6 + k * 1.84, 32.6 + (1 - abs(k - 2) / 2.0) * 0.6, -1), (2, 1, 2), "steel", inflate=0.1, share="hb")
    c("helm", (-5.5, 30.5, -1), (1, 2, 2), "steel_dark")
    c("helm", (4.5, 30.5, -1), (1, 2, 2), "steel_dark")
    for sg in (-1, 1):                                                                  # ear cups with a speaker cone facing out
        x0 = 4.4 if sg > 0 else -7.4
        c("helm", (x0, 25.5, -3), (3, 6, 6), "steel_dark", inflate=0.2)
        c("helm", (x0 + (3.2 if sg > 0 else -0.2), 26.2, -2.3), (1, 4, 4), "cone")
        c("helm", (x0 + (3.4 if sg > 0 else -0.4), 27.6, -0.9), (1, 1, 1), "emit")
    c("helm", (-8.6, 24.5, -4.5), (1, 1, 5), "steel")                                   # the mic boom
    c("helm", (-8.6, 23.7, -7.5), (1, 1, 3), "steel")
    c("helm", (-7.9, 22.8, -8.6), (2, 2, 2), "rubber")
    c("helm", (-7.5, 23.2, -9.0), (1, 1, 1), "emit")
    for sg in (-1, 1):                                                                  # antenna fins
        c("helm", (2.5 * sg - 0.5, 32.5, 1.5), (1, 6, 1), "steel", rot=(-12, 0, -8 * sg), share="ant")
        c("helm", (2.5 * sg - 0.5, 38.2, 1.9), (1, 1, 1), "emit", rot=(-12, 0, -8 * sg), share="ant_tip")

    # ---- CHEST: the amp plate
    A.bone("chest", pivot=(0, 18, 0))
    c("chest", (-4.6, 14, -3.3), (9, 9, 2), "steel_dark", inflate=0.2)
    c("chest", (-3.6, 17, -4.0), (7, 5, 1), "grille")                                   # speaker grille
    c("chest", (-3.6, 22, -4.0), (7, 2, 1), "vu")                                       # VU meter strip
    for k in range(3):
        c("chest", (-3 + k * 2.6, 14.3, -4.0), (1, 1, 1), "steel", share="knob")        # knobs
    c("chest", (-4.7, 12.4, -3.0), (9, 2, 6), "rubber", inflate=0.3)                    # belt
    c("chest", (-1.5, 12.2, -3.7), (3, 2, 1), "steel")
    c("chest", (-3.5, 24, -2.5), (7, 2, 5), "steel", inflate=0.1)                       # collar
    c("chest", (-4.6, 14, 1.4), (9, 9, 2), "steel_dark", inflate=0.2)                   # back plate with vents
    c("chest", (-3.6, 16, 3.3), (7, 5, 1), "grille")
    c("chest", (-6.2, 22.2, -2.4), (2, 2, 5), "steel_dark", inflate=0.1)                # shoulder straps
    c("chest", (4.2, 22.2, -2.4), (2, 2, 5), "steel_dark", inflate=0.1)

    # ---- three arcs of sound behind the back
    for tag, r, ph in (("a", 9, 0), ("b", 13, 1), ("c", 17, 2)):
        name = "wave_" + tag
        A.bone(name, pivot=(0, 20, 6))
        arc_cubes(A, name, (0, 20), r, 35, 145, 14 if r > 9 else 18, "emit", thick=1, z=6 + ph * 0.5, width=3 + (1 if r > 9 else 0), share="w" + tag)
        arc_cubes(A, name, (0, 20), r - 1.4, 45, 135, 18, "steel", thick=1, z=6 + ph * 0.5, width=2, share="ws" + tag)

    # ---- the cable tail
    A.bone("cable", pivot=(0, 12, 3))
    for k in range(7):
        sway = 2.2 * math.sin(k * 0.9)
        c("cable", (-0.5 + sway, 10.5 - k * 1.6, 2.4 + k * 0.4), (1, 2, 1), "cable", rot=(12, 0, 0), pivot=(0, 12, 3), share="cb")
    c("cable", (-1 + 2.2 * math.sin(6 * 0.9), -0.4, 5.0), (2, 2, 2), "steel", share="plug")
    c("cable", (-0.5 + 2.2 * math.sin(6 * 0.9), -0.9, 5.4), (1, 1, 1), "emit", share="plugg")

    # ---- PAULDRONS (speaker stacks), VAMBRACES (tuning forks), GAUNTLETS (resonator rings)
    for sg, tag in ((-1, "r"), (1, "l")):
        sx = 6.0 * sg
        A.bone("pauldron_" + tag, pivot=(sx, 22, 0))
        c("pauldron_" + tag, (sx - 3, 22.2, -3), (6, 5, 6), "steel_dark", inflate=0.3)
        c("pauldron_" + tag, (sx + sg * 2.8 - (0.5 if sg < 0 else 0.0), 22.8, -2), (1, 4, 4), "cone")
        c("pauldron_" + tag, (sx - 2, 27.4, -2), (4, 1, 4), "cone")
        c("pauldron_" + tag, (sx - 0.5, 23.2, -3.6), (1, 1, 1), "emit")
        A.bone("vambrace_" + tag, pivot=(sx, 18, 0))
        ox = sx - 2.5
        c("vambrace_" + tag, (ox, 14.2, -2.7), (5, 4, 5), "steel", inflate=0.2)
        c("vambrace_" + tag, (ox + 1.0, 14.0, -3.4), (1, 7, 1), "fork")                  # the two prongs
        c("vambrace_" + tag, (ox + 3.0, 14.0, -3.4), (1, 7, 1), "fork")
        c("vambrace_" + tag, (ox + 1.0, 13.4, -3.4), (3, 1, 1), "fork")
        c("vambrace_" + tag, (ox + 2.0, 15.0, -3.5), (1, 5, 1), "emit")                  # the vibration between them
        A.bone("gauntlet_" + tag, pivot=(sx, 13, 0))
        c("gauntlet_" + tag, (ox - 0.2, 11.5, -2.7), (5, 3, 5), "steel_dark", inflate=0.25)
        for k in range(4):
            c("gauntlet_" + tag, (ox + 0.2 + k * 1.2, 12.0, -3.5), (1, 1, 1), "emit", share="rg")
        c("gauntlet_" + tag, (ox - 0.3, 14.1, -2.8), (5, 1, 5), "emit", inflate=0.1)
    # ---- GREAVES
    for sg, tag in ((-1, "r"), (1, "l")):
        lx = -2.0 if sg < 0 else 2.0
        A.bone("greave_" + tag, pivot=(lx, 6, 0))
        c("greave_" + tag, (lx - 2.3, 3, -2.9), (4, 7, 1), "steel_dark", inflate=0.3)
        c("greave_" + tag, (lx - 1.3, 4.2, -3.6), (2, 3, 1), "cone")
        c("greave_" + tag, (lx - 2.6, 9.4, -2.6), (5, 3, 5), "steel", inflate=0.3)
        c("greave_" + tag, (lx - 1, 9.8, -3.6), (2, 2, 1), "grille")
        c("greave_" + tag, (lx - 2.8, 1.8, -2.8), (5, 1, 5), "emit", inflate=0.4)
        c("greave_" + tag, (lx - 2.6, 0.2, -3.6), (5, 1, 6), "rubber", inflate=0.1)
        c("greave_" + tag, (lx - 2.8, 5, 1.6), (5, 5, 1), "steel_dark", inflate=0.2)

    A.anim.clip("idle", length=2.0, loop=True)
    for tag, ph in (("a", 0.0), ("b", 0.33), ("c", 0.66)):
        A.anim.scale("idle", "wave_" + tag, loop(2.0, lambda p, ph=ph: (1 + 0.1 * (0.5 - 0.5 * math.cos((p + ph) * 2 * math.pi)),) * 2 + (1,)))
    A.anim.rot("idle", "cable", sin_loop(2.0, 7.0, ax=0))
    A.anim.rot("idle", "helm", sin_loop(2.0, 0.8, ax=2))
    return A.save()


def build_speaker():
    A = Atlas(KEY, "speaker", 128, 128, seed=61)
    c = A.cube
    A.bone("root", pivot=(0, 0, 0))
    A.bone("cabinet", "root", (0, 8, 0))
    A.bone("woofer", "cabinet", (0, 12, -5))
    A.bone("mid", "cabinet", (0, 22, -5))
    A.bone("tweeter", "cabinet", (0, 29, -5))
    A.bone("horn", "cabinet", (0, 33, 0))
    A.bone("antenna", "cabinet", (4, 34, 2))
    c("cabinet", (-6, 2, -5), (12, 30, 10), "rubber", inflate=0.1)
    c("cabinet", (-6.5, 2, -5.5), (13, 2, 11), "steel_dark")                            # base trim
    c("cabinet", (-6.5, 31, -5.5), (13, 2, 11), "steel_dark")
    for sx in (-6.7, 6.2):
        c("cabinet", (sx, 8, -3), (1, 3, 6), "steel")                                    # side handles
        c("cabinet", (sx, 20, -3), (1, 3, 6), "steel")
    for sx in (-5, 4):
        c("cabinet", (sx, 0, -4), (2, 2, 8), "steel_dark")                               # feet
    c("cabinet", (-4.5, 4, -5.6), (9, 2, 1), "vu")                                       # VU strip
    c("woofer", (-4.5, 7.5, -6.5), (9, 9, 1), "cone")
    c("woofer", (-5, 7, -6.2), (10, 10, 1), "steel_dark")
    c("mid", (-3, 19.5, -6.5), (6, 6, 1), "cone")
    c("mid", (-3.5, 19, -6.2), (7, 7, 1), "steel_dark")
    c("tweeter", (-1.5, 27.5, -6.5), (3, 3, 1), "emit")
    c("tweeter", (-2, 27, -6.2), (4, 4, 1), "steel")
    for k, (w, d) in enumerate(((4, 4), (6, 6), (8, 8))):                                # the horn on top
        c("horn", (-w / 2.0, 33 + k * 2, -d / 2.0), (w, 2, d), "steel", share="hn%d" % k)
    c("horn", (-3, 38, -3), (6, 1, 6), "emit")
    c("antenna", (3.5, 34, 1.5), (1, 8, 1), "steel")
    c("antenna", (3.2, 41.6, 1.2), (2, 2, 2), "emit")
    A.anim.clip("idle", length=2.0, loop=True)
    A.anim.scale("idle", "woofer", loop(2.0, lambda p: (1, 1, 1.0 + 0.2 * (0.5 - 0.5 * math.cos(p * 4 * math.pi)))))
    A.anim.rot("idle", "antenna", sin_loop(2.0, 3.0, ax=2))
    A.anim.clip("pulse", length=0.5, loop=True)
    A.anim.scale("pulse", "woofer", loop(0.5, lambda p: (1.0 + 0.1 * math.sin(p * 2 * math.pi), 1.0 + 0.1 * math.sin(p * 2 * math.pi), 1.0 + 0.6 * math.sin(p * math.pi) ** 2)))
    A.anim.scale("pulse", "mid", loop(0.5, lambda p: (1, 1, 1.0 + 0.4 * math.sin(p * math.pi * 2) ** 2)))
    A.anim.scale("pulse", "tweeter", loop(0.5, lambda p: (1, 1, 1.0 + 0.5 * math.sin(p * math.pi * 4) ** 2)))
    A.anim.pos("pulse", "cabinet", loop(0.5, lambda p: (0.4 * math.sin(p * 4 * math.pi), 0, 0)))
    A.anim.rot("pulse", "antenna", sin_loop(0.5, 6.0, ax=2))
    return A.save()


def build_fork():
    A = Atlas(KEY, "fork", 128, 128, seed=71)
    c = A.cube
    A.bone("root", pivot=(0, 8, 0))
    A.bone("stem", "root", (0, 8, 0))
    A.bone("prong_l", "stem", (2, 12, 0))
    A.bone("prong_r", "stem", (-2, 12, 0))
    A.bone("rings", "root", (0, 16, 0))
    c("stem", (-1, 2, -1), (2, 10, 2), "fork")
    c("stem", (-1.5, 0, -1.5), (3, 2, 3), "steel_dark")
    c("stem", (-3, 11, -1), (6, 2, 2), "fork")
    c("prong_l", (1, 12, -1), (2, 14, 2), "fork")
    c("prong_r", (-3, 12, -1), (2, 14, 2), "fork")
    c("prong_l", (1.2, 24, -0.8), (1.6, 2, 1.6), "emit")
    c("prong_r", (-2.8, 24, -0.8), (1.6, 2, 1.6), "emit")
    c("stem", (-0.5, 14, -0.5), (1, 9, 1), "emit")
    for k, r in enumerate((4, 7, 10)):                                                  # the sound rings round the prongs
        A.bone("ring%d" % k, "rings", (0, 20 + k * 2, 0))
        A.ring("ring%d" % k, (0, 19 + k * 2.5, 0), r, 1, 1, 8, "emit", plane="xz", share="rg%d" % k)
    A.anim.clip("idle", length=1.0, loop=True)
    A.anim.rot("idle", "prong_l", sin_loop(1.0, 2.0, ax=2))
    A.anim.rot("idle", "prong_r", sin_loop(1.0, -2.0, ax=2))
    A.anim.clip("strike", length=0.25, loop=True)
    A.anim.rot("strike", "prong_l", loop(0.25, lambda p: (0, 0, 6 * math.sin(p * 2 * math.pi * 2))))
    A.anim.rot("strike", "prong_r", loop(0.25, lambda p: (0, 0, -6 * math.sin(p * 2 * math.pi * 2))))
    for k in range(3):
        A.anim.scale("strike", "ring%d" % k, loop(0.25, lambda p, k=k: (1 + 0.3 * ((p + k / 3.0) % 1.0),) * 2 + (1 + 0.3 * ((p + k / 3.0) % 1.0),)))
        A.anim.scale("idle", "ring%d" % k, loop(1.0, lambda p, k=k: (1 + 0.15 * ((p + k / 3.0) % 1.0),) * 2 + (1 + 0.15 * ((p + k / 3.0) % 1.0),)))
    return A.save()


if __name__ == "__main__":
    ids = [build_armour(), build_speaker(), build_fork()]
    check(*ids)
