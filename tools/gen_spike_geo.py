"""Spike Magic (1.93.0 rebuild): the geo models of the temporary armour and of the two constructs.

    python3 -B tools/gen_spike_geo.py
    python3 -B tools/geo_check.py spike_armor spike_pillar spike_urchin

spike_armor   pieces on the humanoid frame, one root bone per piece: helm (a crown of five long spikes, a brow spike, cheek thorns),
              chest (a ridged cuirass with two rows of forward spikes), waist (a belt with hip spikes), pauldron_r/_l (a plate and three
              long swept spikes: the silhouette of the set), vambrace_r/_l (a bracer with a spike blade), gauntlet_r/_l (knuckle spikes),
              greave_r/_l (knee spike, shin plates, a heel spike), back (a fan of seven long spines that flex). Material: brushed gun-metal
              with the spike tips glowing ice-white (the glow map). Clips: idle (the spines breathe), move.
spike_pillar  the spike cluster the book raises from the ground: one long central spike ringed by six, on a rocky base. Clips: idle
              (shimmer), rise (the spikes thrust up from below ground), retract.
spike_urchin  a spiked sphere: a dark core with 18 spikes pointing every way and a glowing seam. Clips: idle (slow roll), roll, burst
              (the spikes shoot out).
"""
import math
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from shakudo_kit import Kit, Anim, steel_spike, emissive, stone, sway, bob, spike         # noqa: E402

ST = steel_spike()
ST2 = steel_spike(dark=0x2A2F3A, light=0x8D9BB0)
TIP = steel_spike(glow_tip=0.95)
ROCK = stone(dark=0x3C3A42, light=0x77757F)
CORE = emissive(0xBFE8FF, base=0x1A2230)


def armor():
    k = Kit("spike", "armor", 256, 256, seed=22)
    # ------------------------------------------------------------------ helm
    k.bone("helm", pivot=(0, 24, 0))
    k.box("helm", (-4, 27, -4), (8, 5, 8), ST, inflate=0.7)
    k.box("helm", (-5, 26.2, -5.5), (10, 1, 2), ST2)
    k.pair("helm", "helm", (-6.0, 24.0, -3.0), (1, 5, 6), ST2, inflate=0.1)             # cheek plates
    crown = [(0, 32.4, 0, 0, 0, 0, 9), (-2.4, 32.2, 0, 0, 0, 18, 7), (2.4, 32.2, 0, 0, 0, -18, 7), (-3.6, 32.0, 0, 0, 0, 36, 5), (3.6, 32.0, 0, 0, 0, -36, 5)]
    for i, (x, y, z, rx, ry, rz, ln) in enumerate(crown):
        spike(k, "helm", "crown%d" % i, (x, y, z), (rx, ry, rz), ln, 3, ST, TIP, segs=2)
    spike(k, "helm", "brow", (0, 28.6, -5.6), (-80, 0, 0), 5, 2, ST, TIP, segs=2)       # a brow spike pointing forward
    for sgn, nm in ((-1, "thorn_r"), (1, "thorn_l")):
        spike(k, "helm", nm, (sgn * 6.4, 25.5, -1.0), (0, 0, -sgn * 80), 4, 2, ST, TIP, segs=2)
    k.box("helm", (-5, 24.4, 3.0), (10, 2, 2), ST2, inflate=0.1)                         # the nape lame
    # ------------------------------------------------------------------ chest
    k.bone("chest", pivot=(0, 24, 0))
    k.box("chest", (-4, 12.5, -2), (8, 11, 4), ST, inflate=0.8)
    k.box("chest", (-1, 13, -3.2), (2, 10, 2), ST2, inflate=0.0)                          # the central ridge
    for i, y in enumerate((14.5, 17.5, 20.5)):
        k.box("chest", (-4.4, y, -3.0), (9, 1, 1), ST2)
    for i, (x, y) in enumerate(((-2.4, 21.5), (2.4, 21.5), (-2.4, 18.0), (2.4, 18.0))):
        spike(k, "chest", "cs%d" % i, (x, y, -3.2), (-90, 0, 0), 4, 2, ST, TIP, segs=2)  # forward spikes
    k.box("chest", (-5, 22.6, -2.5), (10, 2, 5), ST2, inflate=0.1)
    k.box("chest", (-3.5, 13.5, 2.0), (7, 8, 1), ST2, inflate=0.9)
    # ------------------------------------------------------------------ waist
    k.bone("waist", pivot=(0, 13, 0))
    k.box("waist", (-4, 11.5, -2), (8, 2, 4), ST2, inflate=0.9)
    for i in range(-2, 2):
        k.box("waist", (i * 2.0, 8.5, -3.4), (2, 4, 1), ST, rotation=(-8, 0, 0), pivot=(0, 12, -3))
        k.box("waist", (i * 2.0, 8.5, 2.4), (2, 4, 1), ST, rotation=(8, 0, 0), pivot=(0, 12, 3))
    spike(k, "waist", "hip_r", (-5.4, 12.0, 0), (0, 0, 70), 5, 3, ST, TIP)
    spike(k, "waist", "hip_l", (5.4, 12.0, 0), (0, 0, -70), 5, 3, ST, TIP)
    # ------------------------------------------------------------------ pauldrons: the long swept spikes
    r, l = k.pair_bones("pauldron", pivot=(-5, 22, 0))
    k.pair(r, l, (-9.4, 21.6, -3.6), (6, 2, 7), ST, rotation=(0, 0, -8), pivot=(-5, 22, 0))
    k.pair(r, l, (-10.2, 19.4, -3.4), (6, 2, 7), ST2, rotation=(0, 0, -14), pivot=(-5, 22, 0))
    k.pair(r, l, (-11.0, 17.2, -3.2), (6, 2, 6), ST, rotation=(0, 0, -20), pivot=(-5, 22, 0))
    # the spikes: built on the right arm bone and mirrored by hand onto the left one
    for nm, sgn in (("_r", -1), ("_l", 1)):
        par = "pauldron" + nm
        spike(k, par, "ps0" + nm, (sgn * 8.0, 23.0, 0), (0, 0, -sgn * 55), 11, 4, ST, TIP, segs=2)
        spike(k, par, "ps1" + nm, (sgn * 8.2, 22.4, -2.4), (-25, 0, -sgn * 70), 8, 3, ST, TIP)
        spike(k, par, "ps2" + nm, (sgn * 8.2, 22.4, 2.4), (25, 0, -sgn * 70), 8, 3, ST, TIP)
    # ------------------------------------------------------------------ vambraces
    r, l = k.pair_bones("vambrace", pivot=(-5, 22, 0))
    k.pair(r, l, (-8, 13.2, -2), (4, 6, 4), ST, inflate=0.75)
    k.pair(r, l, (-8, 17.6, -2), (4, 1, 4), ST2, inflate=0.95)
    for nm, sgn in (("_r", -1), ("_l", 1)):
        spike(k, "vambrace" + nm, "vb" + nm, (sgn * 8.8, 17.0, 0), (0, 0, -sgn * 20), 8, 3, ST, TIP)
        spike(k, "vambrace" + nm, "vc" + nm, (sgn * 8.8, 14.5, 0), (0, 0, -sgn * 16), 5, 2, ST, TIP, segs=2)
    # ------------------------------------------------------------------ gauntlets
    r, l = k.pair_bones("gauntlet", pivot=(-5, 22, 0))
    k.pair(r, l, (-8, 11.6, -2), (4, 3, 4), ST2, inflate=0.8)
    for nm, sgn in (("_r", -1), ("_l", 1)):
        for i in range(4):
            spike(k, "gauntlet" + nm, "kn%d%s" % (i, nm), (sgn * (5.9 + i * 0.95) - sgn * 0.0, 13.2, -3.0), (-90, 0, 0), 3, 1, ST, TIP, segs=1)
    # ------------------------------------------------------------------ greaves
    r, l = k.pair_bones("greave", pivot=(-1.9, 12, 0))
    k.pair(r, l, (-4, 6.2, -2), (4, 5, 4), ST, inflate=0.7)
    k.pair(r, l, (-4, 0.9, -2), (4, 5, 4), ST2, inflate=0.6)
    k.pair(r, l, (-4, 3.6, -2), (4, 1, 4), ST, inflate=0.78)
    k.pair(r, l, (-4, 0, -2), (4, 2, 4), ST, inflate=0.55)
    for nm, sgn in (("_r", -1), ("_l", 1)):
        spike(k, "greave" + nm, "knee" + nm, (sgn * 2.0, 6.0, -3.2), (-90, 0, 0), 6, 3, ST, TIP)
        spike(k, "greave" + nm, "heel" + nm, (sgn * 2.0, 1.5, 3.2), (90, 0, 0), 5, 2, ST, TIP, segs=2)
    # ------------------------------------------------------------------ back: seven flexing spines
    k.bone("back", pivot=(0, 20, 3))
    k.box("back", (-3, 14, 2.4), (6, 8, 1), ST2, inflate=0.8)
    names = []
    fan = [(-54, 14), (-36, 20), (-18, 26), (0, 30), (18, 26), (36, 20), (54, 14)]
    for i, (ang, ln) in enumerate(fan):
        nm = "spine%d" % i
        spike(k, "back", nm, (0, 19.0, 3.6), (25 + abs(ang) * 0.1, 0, ang), ln, 3 if i in (0, 6) else 4, ST, TIP, segs=2)
        names.append(nm)
    a = Anim()
    a.clip("idle", length=4.0, loop=True)
    for i, nm in enumerate(names):
        sway(a, "idle", nm, 2, 3.5, 4.0, i * 0.5)
    a.clip("move", length=1.5, loop=True)
    for i, nm in enumerate(names):
        sway(a, "move", nm, 2, 8, 1.5, i * 0.5)
    k.save(a)


def pillar():
    k = Kit("spike", "pillar", 128, 128, seed=33)
    k.bone("root", pivot=(0, 0, 0))
    k.bone("base", parent="root", pivot=(0, 0, 0))
    k.box("base", (-12, 0, -12), (24, 2, 24), ROCK)
    k.box("base", (-8, 2, -8), (16, 2, 16), ROCK)
    k.bone("core", parent="root", pivot=(0, 0, 0))
    spike(k, "core", "main", (0, 2, 0), (0, 0, 0), 34, 8, ST, TIP, segs=4)
    names = ["main"]
    for i in range(6):
        a = 2 * math.pi * i / 6
        nm = "ring%d" % i
        spike(k, "core", nm, (7.0 * math.cos(a), 2, 7.0 * math.sin(a)), (22 * math.sin(a), 0, -22 * math.cos(a)), 18 + (i % 2) * 4, 5, ST2, TIP, segs=3)
        names.append(nm)
    an = Anim()
    an.clip("idle", length=3.0, loop=True)
    for i, nm in enumerate(names[1:]):
        sway(an, "idle", nm, 0, 2, 3.0, i)
    an.clip("rise", length=0.5, loop=False)
    an.pos("rise", "core", {0.0: (0, -36, 0), 0.25: (0, 3, 0), 0.5: (0, 0, 0)})
    an.clip("retract", length=0.6, loop=False)
    an.pos("retract", "core", {0.0: (0, 0, 0), 0.6: (0, -38, 0)})
    k.save(an)


def urchin():
    k = Kit("spike", "urchin", 128, 128, seed=44)
    k.bone("root", pivot=(0, 0, 0))
    k.bone("ball", parent="root", pivot=(0, 12, 0))
    k.box("ball", (-6, 6, -6), (12, 12, 12), ST2, inflate=0.0)
    k.box("ball", (-7, 11, -7), (14, 2, 14), CORE)                                    # the glowing seam
    k.box("ball", (-5, 5, -5), (10, 14, 10), ST, inflate=0.4)
    golden = math.pi * (3 - math.sqrt(5))
    n = 18
    names = []
    for i in range(n):
        yv = 1 - (i + 0.5) * 2.0 / n
        rad = math.sqrt(max(0.0, 1 - yv * yv))
        th = golden * i
        dx, dy, dz = rad * math.cos(th), yv, rad * math.sin(th)
        # the spike bone points along +y; turn it to (dx, dy, dz): about z by the angle off the vertical, about y by the azimuth
        rx = math.degrees(math.asin(max(-1.0, min(1.0, dz))))            # the spike's +y is turned by x first, then by z
        rz = math.degrees(math.atan2(-dx, dy))
        nm = "u%d" % i
        base = (dx * 6.0, 12 + dy * 6.0, dz * 6.0)
        spike(k, "ball", nm, base, (rx, 0, rz), 10, 3, ST, TIP, segs=2)
        names.append(nm)
    an = Anim()
    an.clip("idle", length=6.0, loop=True)
    an.rot("idle", "ball", {0.0: (0, 0, 0), 3.0: (0, 180, 0), 6.0: (0, 360, 0)})
    an.clip("roll", length=1.0, loop=True)
    an.rot("roll", "ball", {0.0: (0, 0, 0), 0.5: (180, 0, 0), 1.0: (360, 0, 0)})
    an.clip("burst", length=0.4, loop=False)
    an.scale("burst", "ball", {0.0: (1, 1, 1), 0.1: (0.8, 0.8, 0.8), 0.4: (1.5, 1.5, 1.5)})
    k.save(an)


if __name__ == "__main__":
    armor()
    pillar()
    urchin()
