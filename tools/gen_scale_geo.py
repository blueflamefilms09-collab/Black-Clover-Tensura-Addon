"""Scale Magic (1.93.0 rebuild): the geo models of the temporary armour and of the two constructs.

    python3 -B tools/gen_scale_geo.py
    python3 -B tools/geo_check.py scale_armor scale_serpent scale_shield

scale_armor    pieces on the humanoid frame, one root bone per piece: helm (a scaled hood with swept ear fins and a crest fin), chest
               (overlapping scale rows over a pale belly plate, a frill collar), waist (scale lames), pauldron_r/_l (three scale
               cowls and a fin), vambrace_r/_l (a scale sleeve with a blade fin), gauntlet_r/_l (clawed), greave_r/_l (scale cuisse, shin
               and boot with a calf fin), back (a dorsal ridge of fin plates, sways) and tail (seven segments, waves). Material:
               teal scales with a pale shimmering rim; the shimmer is the glow map. Clips: idle, move.
scale_serpent  the wyrm the book calls: ten scaled segments with a fanged head and a dorsal fin. Clips: idle (slow slither), strike (lunge
               and bite), coil.
scale_shield   a kite shield of overlapping scales with a gilt-pale rim and a glowing boss. Clips: idle (a slow tilt and bob), block.
"""
import math
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from shakudo_kit import Kit, Anim, scales, emissive, keratin, solid, sway, bob, spike, chain_bones, wave        # noqa: E402

SC = scales(deep=0x14707A, mid=0x3CC2BC, rim=0xC0FFF2)
SC2 = scales(deep=0x0E5560, mid=0x2A9EA0, rim=0x9AF0E0, size=3, glow_rate=0.08)
PALE = scales(deep=0x5FA8A0, mid=0xB5E4D6, rim=0xF2FFF6, size=4, glow_rate=0.2)
FIN = scales(deep=0x0B5E70, mid=0x35C0C4, rim=0xC8FFF4, size=3, glow_rate=0.3)
CLAW = keratin(base=0xE4F2E8, edge=0x7FA39A, glow=0xA8FFE8)
EYE = emissive(0xC8FFF0, base=0x08262C)


def armor():
    k = Kit("scale", "armor", 256, 256, seed=13)
    # ------------------------------------------------------------------ helm: scaled hood
    k.bone("helm", pivot=(0, 24, 0))
    k.box("helm", (-4, 26.5, -4), (8, 6, 8), SC, inflate=0.7)
    k.box("helm", (-4, 24, -4), (8, 2, 8), SC2, inflate=0.6)                          # the hood's lower rim round the jaw line
    k.box("helm", (-4.5, 27.8, -5.1), (9, 1, 1), FIN)                                  # the brow ridge
    k.pair("helm", "helm", (-3.2, 27.2, -5.2), (2, 1, 1), EYE)                         # eye studs above the eyes
    k.pair("helm", "helm", (-8.0, 26.0, -1.5), (3, 1, 5), FIN, rotation=(0, 0, 14), pivot=(-5, 28, 0))      # the swept ear fins
    k.pair("helm", "helm", (-8.5, 28.5, -1.5), (3, 1, 4), FIN, rotation=(0, 0, 24), pivot=(-5, 28, 0))
    for i, h in enumerate((7, 6, 5, 4, 3)):                                            # the crest fin along the head
        k.box("helm", (-0.5, 32 + 0.3, -3.5 + i * 1.8), (1, h - 3, 2), FIN if i % 2 == 0 else PALE, inflate=0.2)
    for i, (y, w) in enumerate([(25.6, 9), (23.8, 10)]):                               # the nape frill
        k.box("helm", (-5, y, 3.0 + 0.4 * i), (10, 2, 2), SC if i == 0 else SC2, inflate=0.1)
    # ------------------------------------------------------------------ chest: rows of scales over a pale belly plate
    k.bone("chest", pivot=(0, 24, 0))
    k.box("chest", (-4, 12.5, -2), (8, 11, 4), SC, inflate=0.8)
    k.box("chest", (-2, 13, -2), (4, 10, 1), PALE, inflate=0.95)                       # the belly plates (a vertical strip)
    for i, y in enumerate((14, 17, 20)):
        k.box("chest", (-4, y, -2), (8, 1, 1), FIN, inflate=0.95)                       # rim of each scale row
    k.box("chest", (-5, 22.4, -2.5), (10, 2, 5), SC2, inflate=0.2)                      # the frill collar
    k.box("chest", (-1, 18, -3.1), (2, 2, 1), EYE)                                      # a pearl on the breast
    k.box("chest", (-3.5, 13.5, 2.0), (7, 8, 1), SC2, inflate=0.9)                      # the back plate
    # ------------------------------------------------------------------ waist: scale lames
    k.bone("waist", pivot=(0, 13, 0))
    k.box("waist", (-4, 11.5, -2), (8, 2, 4), FIN, inflate=0.9)
    for i in range(-2, 2):
        k.box("waist", (i * 2.0, 8.0, -3.4), (2, 4, 1), SC, rotation=(-8, 0, 0), pivot=(0, 12, -3))
        k.box("waist", (i * 2.0, 8.0, 2.4), (2, 4, 1), SC, rotation=(8, 0, 0), pivot=(0, 12, 3))
    k.pair("waist", "waist", (-5.7, 8.0, -1.5), (1, 5, 3), SC2, rotation=(0, 0, 10), pivot=(-5, 12, 0))
    # ------------------------------------------------------------------ pauldrons: three cowls and a fin
    r, l = k.pair_bones("pauldron", pivot=(-5, 22, 0))
    k.pair(r, l, (-9.4, 21.6, -3.6), (6, 2, 7), SC, rotation=(0, 0, -8), pivot=(-5, 22, 0))
    k.pair(r, l, (-10.2, 19.4, -3.4), (6, 2, 7), SC2, rotation=(0, 0, -14), pivot=(-5, 22, 0))
    k.pair(r, l, (-10.9, 17.2, -3.2), (6, 2, 6), SC, rotation=(0, 0, -20), pivot=(-5, 22, 0))
    k.pair(r, l, (-8.5, 23.0, -1.0), (1, 5, 4), FIN, rotation=(0, 0, -22), pivot=(-6, 22, 0))
    k.pair(r, l, (-9.9, 23.0, 1.0), (1, 4, 3), PALE, rotation=(0, 0, -34), pivot=(-6, 22, 0))
    # ------------------------------------------------------------------ vambraces: sleeve and blade fin
    r, l = k.pair_bones("vambrace", pivot=(-5, 22, 0))
    k.pair(r, l, (-8, 13.2, -2), (4, 6, 4), SC, inflate=0.75)
    k.pair(r, l, (-8, 17.6, -2), (4, 1, 4), FIN, inflate=0.95)
    k.pair(r, l, (-9.9, 13.0, -0.5), (1, 6, 5), FIN, rotation=(0, 0, 0), pivot=(-9, 16, 0))
    k.pair(r, l, (-10.6, 14.0, 0.5), (1, 4, 3), PALE)
    # ------------------------------------------------------------------ gauntlets: webbed claws
    r, l = k.pair_bones("gauntlet", pivot=(-5, 22, 0))
    k.pair(r, l, (-8, 11.6, -2), (4, 3, 4), SC2, inflate=0.8)
    k.pair(r, l, (-8, 11.0, -3.3), (4, 2, 1), FIN)
    for i in range(3):
        k.pair(r, l, (-7.6 + i * 1.3, 9.8, -3.6), (1, 3, 1), CLAW)                       # three claws hanging past the hand
    k.pair(r, l, (-9.0, 11.0, -2.0), (1, 2, 3), PALE)
    # ------------------------------------------------------------------ greaves
    r, l = k.pair_bones("greave", pivot=(-1.9, 12, 0))
    k.pair(r, l, (-4, 6.2, -2), (4, 5, 4), SC, inflate=0.7)
    k.pair(r, l, (-4, 0.9, -2), (4, 5, 4), SC2, inflate=0.6)
    k.pair(r, l, (-4, 3.6, -2), (4, 1, 4), FIN, inflate=0.78)
    k.pair(r, l, (-4, 0, -2), (4, 2, 4), SC, inflate=0.55)
    k.pair(r, l, (-4.9, 1.5, 0.0), (1, 5, 3), FIN, rotation=(0, 0, 10), pivot=(-4.5, 2, 0))     # the calf fin
    k.pair(r, l, (-4, 0, -4), (4, 1, 2), PALE)
    # ------------------------------------------------------------------ back: a dorsal ridge of fin plates
    k.bone("back", pivot=(0, 22, 2))
    names = []
    for i, (y, h) in enumerate([(21, 5), (18.5, 6), (16, 6), (13.5, 5), (11, 4)]):
        n = "ridge%d" % i
        k.bone(n, parent="back", pivot=(0, y, 3))
        k.box(n, (-0.5, y, 3.0), (1, h, 3), FIN if i % 2 == 0 else PALE, inflate=0.1)
        names.append(n)
    k.box("back", (-2, 14, 2.2), (4, 8, 1), SC2)
    # ------------------------------------------------------------------ the tail: seven segments
    k.bone("tail", pivot=(0, 11, 2.5))
    segs = chain_bones(k, "tail", "seg", 7, (0, 11, 2.5), (0, -0.6, 2.4))
    for i, n in enumerate(segs):
        w = max(1, 5 - (i * 5) // 7)
        h = max(1, 4 - (i * 3) // 7)
        z = 2.5 + 2.4 * i
        k.box(n, (-w / 2.0, 11 - 0.6 * i - h / 2.0, z), (w, h, 3), SC if i % 2 == 0 else SC2, inflate=0.1 if i < 5 else 0.0)
        if i in (2, 4, 6):
            k.box(n, (-0.5, 11 - 0.6 * i + h / 2.0, z), (1, 2 if i < 6 else 3, 2), FIN)
    a = Anim()
    a.clip("idle", length=4.0, loop=True)
    wave(a, "idle", segs, 1, 9, 4.0, 0.75, 1.1)
    wave(a, "idle", names, 0, 3, 4.0, 0.5)
    a.clip("move", length=2.0, loop=True)
    wave(a, "move", segs, 1, 18, 1.2, 0.8, 1.1)
    wave(a, "move", names, 0, 7, 1.2, 0.5)
    k.save(a)


def serpent():
    k = Kit("scale", "serpent", 256, 128, seed=24)
    k.bone("root", pivot=(0, 0, 0))
    n = 10
    segs = chain_bones(k, "root", "s", n, (0, 6, 0), (0, 0, 7))
    for i, nm in enumerate(segs):
        w = max(4, 14 - (i * 9) // n)
        h = max(4, 12 - (i * 7) // n)
        z = 7 * i
        k.box(nm, (-w / 2.0, 6 - h / 2.0, z), (w, h, 7), SC if i % 2 == 0 else SC2, inflate=0.2)
        k.box(nm, (-w / 2.0 + 1, 6 - h / 2.0 - 0.6, z + 1), (max(1, w - 2), 1, 5), PALE)          # the belly plates
        if i < n - 1:
            k.box(nm, (-0.5, 6 + h / 2.0, z + 1), (1, 3 if i < n - 3 else 2, 5), FIN)             # dorsal fin
    # the head: on the first segment's front (-z side)
    k.bone("head", parent="s0", pivot=(0, 6, 0))
    k.box("head", (-6, 1, -11), (12, 9, 11), SC, inflate=0.1)
    k.box("head", (-5, 0, -18), (10, 4, 7), SC2)                                                 # the snout and the lower jaw
    k.box("head", (-5, 4, -18), (10, 3, 7), SC)
    k.box("head", (-4.5, -1.0, -17.5), (9, 1, 6), PALE)
    for sgn in (-1, 1):
        k.box("head", (sgn * 4.5 - 0.5, 7.5, -9), (1, 2, 3), EYE)
        k.box("head", (sgn * 3.5 - 0.5, -1.2, -17.5), (1, 3, 1), CLAW)                           # fangs
    k.box("head", (-0.5, 10, -9), (1, 4, 8), FIN)
    k.box("head", (-8, 5, -4), (2, 3, 5), FIN)
    k.box("head", (6, 5, -4), (2, 3, 5), FIN)
    a = Anim()
    a.clip("idle", length=4.0, loop=True)
    wave(a, "idle", segs, 1, 11, 4.0, 0.7, 1.0)
    sway(a, "idle", "head", 0, 4, 2.0)
    a.clip("strike", length=0.8, loop=False)
    a.rot("strike", "s0", {0.0: (0, 0, 0), 0.25: (28, 0, 0), 0.4: (-18, 0, 0), 0.8: (0, 0, 0)})
    a.rot("strike", "head", {0.0: (0, 0, 0), 0.25: (30, 0, 0), 0.4: (-22, 0, 0), 0.5: (-30, 0, 0), 0.8: (0, 0, 0)})
    a.pos("strike", "s0", {0.0: (0, 0, 0), 0.25: (0, 2, 5), 0.4: (0, 0, -9), 0.8: (0, 0, 0)})
    a.clip("coil", length=4.0, loop=True)
    wave(a, "coil", segs, 1, 30, 4.0, 0.5, 1.02)
    k.save(a)


def shield():
    k = Kit("scale", "shield", 128, 128, seed=35)
    k.bone("root", pivot=(0, 0, 0))
    k.bone("plate", parent="root", pivot=(0, 18, 0))
    k.box("plate", (-8, 12, -1), (16, 20, 2), SC)                                              # the broad upper plate
    k.box("plate", (-6, 6, -1), (12, 6, 2), SC2)
    k.box("plate", (-4, 1, -1), (8, 5, 2), SC)                                                 # the tapering point
    k.box("plate", (-2, -3, -1), (4, 4, 2), SC2)
    k.box("plate", (-9, 31, -1.6), (18, 2, 3), PALE)                                           # the pale rim
    k.box("plate", (-9, 12, -1.6), (2, 20, 3), PALE)
    k.box("plate", (7, 12, -1.6), (2, 20, 3), PALE)
    k.box("plate", (-3, 19, -2.6), (6, 6, 2), FIN)                                            # the boss
    k.box("plate", (-1, 20.5, -3.6), (2, 3, 2), EYE)
    a = Anim()
    a.clip("idle", length=3.0, loop=True)
    bob(a, "idle", "plate", 0.8, 3.0)
    sway(a, "idle", "plate", 2, 3, 3.0)
    a.clip("block", length=0.5, loop=False)
    a.scale("block", "plate", {0.0: (1, 1, 1), 0.15: (1.12, 1.12, 1.12), 0.5: (1, 1, 1)})
    k.save(a)


if __name__ == "__main__":
    armor()
    serpent()
    shield()
