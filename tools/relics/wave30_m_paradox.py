"""Paradox (the staff of the God of Time, "Wondrous Item, artifact, requires attunement"): a tall twisting staff of dark thorned purple crystal with a
green-wrapped grip, crowned with a cage of downward spikes under a cluster of glowing violet crystals and a halo of lilac energy, three claws at the foot.

Reference: the card page, 624 x 803, the staff down the right (crop box 430, 56 .. 605, 762). The measures below are in "crop px" of the 2x enlarged crop
(the reference the preview shows): the tip of the top crystal at y 45, the point of the foot claw at y 1370: 1325 px for a staff of 1.9 m.
s = crop px from the foot's point up the staff. Axis: y up from the foot; the crown faces +z. The card staff leans about eight degrees: the model stands upright.
Parts (14): foot_claws, lower_shaft, lower_thorns, thorn_spray, green_binding, binding_rings, collar_thorns, upper_shaft, cage_ring, cage_spikes,
crystal_core, crystal_shards, crystal_orb, halo. No part is named like a grip/handle (a mid-shaft grip must not make the runtime flip the staff).
"""
import math

import numpy as np

import mesh_lib as ml
import wave1_models as w

# ---------------------------------------------------------------- 1 measure: crop px -> cm
TOTAL_M = 1.90
S_TIP = 1325.0
K = w.px_to_cm(TOTAL_M * 100.0, S_TIP)                  # cm per crop px (0.1434)


def Y(s):
    return s * K


# foot: three claws (a long centre blade and two outer prongs that curve out and back in)
S_FOOT_TOP = 205.0                                      # where the claws join the shaft (crop y 1165)
CLAW_W = 20.0 * K                                       # centre blade width at the top (crop 20 px)
CLAW_SIDE_SPREAD = 46.0 * K                             # the outer prongs bow out to 46 px each side
# lower shaft: a bramble of two twisted strands round a core
S_LOW0, S_LOW1 = 205.0, 635.0
R_CORE = 5.4 * K
R_STRAND_AT, R_STRAND = 6.2 * K, 3.8 * K                # strand centre radius, strand tube radius
TURNS_LOW = 9.0
# the thorn spray at the foot of the grip (crop y 735: four big thorns reach out and up)
S_SPRAY = 640.0
SPRAY_LEN = 42.0 * K
# the green grip
S_GRIP0, S_GRIP1 = 635.0, 860.0
R_GRIP = 11.0 * K                                       # crop width about 22 px
GRIP_RINGS = 8
# the collar of thorns above the grip (crop y 490)
S_COLLAR = 872.0
COLLAR_LEN = 30.0 * K
# upper shaft
S_UP0, S_UP1 = 890.0, 1010.0
TURNS_UP = 3.5
# the cage: a ring with spikes hanging down (crop y 340, spikes to y 380)
S_CAGE = 1030.0
R_CAGE = 52.0 * K
CAGE_SPIKES = 10
CAGE_SPIKE_LEN = 38.0 * K
# the crystals
CRYSTALS = [  # (direction x, y, z, start s, length px, width px)
    (0.00, 1.00, 0.00, 1030.0, 235.0, 44.0),           # the core crystal
    (-0.20, 1.00, 0.06, 1040.0, 285.0, 34.0),          # the tall pale spike leaning left (the card's tip at y 45)
    (0.26, 1.00, -0.02, 1040.0, 175.0, 32.0),          # the right one
]
SHARDS = [(-0.62, 1.0, 0.25, 1050.0, 150.0, 18.0), (0.60, 1.0, 0.30, 1052.0, 140.0, 17.0), (-0.28, 1.0, -0.55, 1048.0, 120.0, 16.0), (0.18, 1.0, 0.62, 1050.0, 155.0, 18.0),
          (-0.95, 1.0, -0.2, 1046.0, 105.0, 14.0), (0.92, 1.0, -0.25, 1046.0, 100.0, 14.0)]
ORB_S, ORB_R = 1085.0, 26.0 * K
HALO_S, HALO_R, HALO_TUBE = 1150.0, 75.0 * K, 0.34


def thorn(part, base, direction, length, radius, sides=4):
    d = np.array(direction, float)
    d = d / np.linalg.norm(d)
    ml.loft(part, tuple(d), tuple(base), [(0.0, 1.0, 1.0), (length * 0.45, 0.55, 0.55), (length, 0.0, 0.0)], ml.circle(radius, sides))


def strand(part, y0, y1, turns, n, phase, r_at, tube, sides=6):
    pts = w.helix(lambda t: r_at, y0, y1, turns, n, phase=phase)
    ml.sweep(part, pts, ml.circle(tube, sides), scales=lambda t: (1.0, 1.0) if 0.03 < t < 0.97 else (0.6, 0.6))


def build():
    m = ml.Mesh("paradox")
    # ---- 1 the foot: a long centre blade and two prongs that bow out and curve back
    foot = w.add(m, "foot_claws", "obsidian", base=(56, 40, 104), smooth=40)
    ml.loft(foot, "-y", (0, Y(S_FOOT_TOP), 0), [(0, 1.0, 1.0), (Y(S_FOOT_TOP) * 0.35, 0.8, 0.8), (Y(S_FOOT_TOP) * 0.8, 0.45, 0.6), (Y(S_FOOT_TOP), 0.0, 0.0)], ml.lozenge(CLAW_W, CLAW_W * 0.55))
    for sgn in (-1, 1):
        path = [(sgn * 5 * K, Y(S_FOOT_TOP + 5), 0), (sgn * 22 * K, Y(S_FOOT_TOP - 35), 0), (sgn * CLAW_SIDE_SPREAD, Y(S_FOOT_TOP - 100), 0),
                (sgn * (CLAW_SIDE_SPREAD - 6 * K), Y(S_FOOT_TOP - 150), 0), (sgn * (CLAW_SIDE_SPREAD - 18 * K), Y(24), 0)]
        ml.sweep(foot, path, ml.lozenge(1.0, 0.6), scales=lambda t: (CLAW_W * 0.55 * (1.0 - 0.9 * t ** 1.5), CLAW_W * 0.32 * (1.0 - 0.9 * t ** 1.5)))
    # ---- 2 the lower shaft: a core and two twisted strands
    low = w.add(m, "lower_shaft", "obsidian", base=(52, 38, 98), smooth=45)
    ml.loft(low, "y", (0, 0, 0), [(Y(S_LOW0) - 0.3, 1.0, 1.0), (Y(S_LOW1) + 0.3, 1.0, 1.0)], ml.circle(R_CORE, 8))
    for ph in (0.0, math.pi):
        strand(low, Y(S_LOW0), Y(S_LOW1), TURNS_LOW, 34, ph, R_STRAND_AT, R_STRAND)
    # ---- 3 the thorns along the lower shaft: one every ~30 px, turning round the shaft
    lt = w.add(m, "lower_thorns", "obsidian", base=(96, 70, 168), smooth=0)
    for i in range(14):
        s = S_LOW0 + 25 + i * 30.0
        a = i * 2.4
        d = (math.cos(a) * 1.0, 0.55, math.sin(a) * 1.0)
        thorn(lt, (math.cos(a) * 7.0 * K, Y(s), math.sin(a) * 7.0 * K), d, (18 + 9 * (i % 3)) * K, 3.0 * K)
    # ---- 4 the spray of four big thorns at the foot of the grip
    sp = w.add(m, "thorn_spray", "obsidian", base=(100, 74, 176), smooth=0)
    for i in range(4):
        a = i * math.pi / 2 + 0.4
        thorn(sp, (math.cos(a) * 7 * K, Y(S_SPRAY), math.sin(a) * 7 * K), (math.cos(a) * 1.0, 0.65, math.sin(a) * 1.0), SPRAY_LEN, 3.4 * K)
    # ---- 5 the green grip: a slightly swelling round loft
    gb = w.add(m, "green_binding", "cloth", base=(54, 96, 60), uv_mode="loft")
    mid = (Y(S_GRIP0) + Y(S_GRIP1)) / 2
    ml.loft(gb, "y", (0, 0, 0), [(Y(S_GRIP0), 0.92, 0.92), (Y(S_GRIP0) + 6, 1.0, 1.0), (mid, 1.05, 1.05), (Y(S_GRIP1) - 6, 1.0, 1.0), (Y(S_GRIP1), 0.92, 0.92)], ml.circle(R_GRIP, 10))
    # ---- 6 the binding rings: dark cord every ~28 px
    br = w.add(m, "binding_rings", "cloth", base=(32, 60, 40), wear=0.3)
    for i in range(GRIP_RINGS):
        s = S_GRIP0 + 14 + (S_GRIP1 - S_GRIP0 - 28) * i / (GRIP_RINGS - 1)
        ml.collar(br, Y(s), R_GRIP * 1.07, 0.7, lip=0.2, sides=10)
    # ---- 7 the collar of five thorns above the grip
    ct = w.add(m, "collar_thorns", "obsidian", base=(100, 74, 176), smooth=0)
    for i in range(5):
        a = i * 2 * math.pi / 5 + 0.2
        thorn(ct, (math.cos(a) * 6 * K, Y(S_COLLAR), math.sin(a) * 6 * K), (math.cos(a) * 1.0, 0.75, math.sin(a) * 1.0), COLLAR_LEN, 3.0 * K)
    # ---- 8 the upper shaft: two strands round a core, thinner
    up = w.add(m, "upper_shaft", "obsidian", base=(58, 42, 108), smooth=45)
    ml.loft(up, "y", (0, 0, 0), [(Y(S_UP0) - 0.3, 1.0, 1.0), (Y(S_UP1) + 0.3, 1.0, 1.0)], ml.circle(R_CORE * 0.95, 8))
    for ph in (0.0, math.pi):
        strand(up, Y(S_UP0), Y(S_UP1), TURNS_UP, 18, ph, R_STRAND_AT * 0.95, R_STRAND * 0.9)
    # ---- 9/10 the cage: a thin ring and the ten spikes that hang from it
    cr = w.add(m, "cage_ring", "obsidian", base=(120, 90, 200), smooth=45)
    ring_pts = [(R_CAGE * math.cos(2 * math.pi * i / 24), Y(S_CAGE), R_CAGE * math.sin(2 * math.pi * i / 24)) for i in range(25)]
    ml.sweep(cr, ring_pts, ml.circle(2.4 * K, 6), caps=(False, False))
    cs = w.add(m, "cage_spikes", "obsidian", base=(110, 82, 190), smooth=0)
    for i in range(CAGE_SPIKES):
        a = 2 * math.pi * i / CAGE_SPIKES
        thorn(cs, (R_CAGE * math.cos(a), Y(S_CAGE), R_CAGE * math.sin(a)), (math.cos(a) * 0.18, -1.0, math.sin(a) * 0.18), CAGE_SPIKE_LEN * (0.8 + 0.4 * ((i * 7) % 3) / 2.0), 3.2 * K)
    # ---- 11 the core crystals (the big three) and 12 the shards
    cc = w.add(m, "crystal_core", "crystal", base=(150, 112, 236), smooth=0)
    for (dx, dy, dz, s0, ln, wd) in CRYSTALS:
        d = np.array([dx, dy, dz], float)
        d /= np.linalg.norm(d)
        ml.loft(cc, tuple(d), (dx * 2 * K, Y(s0), dz * 2 * K), [(0, 0.55, 0.55), (ln * K * 0.12, 1.0, 1.0), (ln * K * 0.62, 0.85, 0.85), (ln * K, 0.0, 0.0)], ml.lozenge(wd * K, wd * K * 0.62, 0.2))
    ch = w.add(m, "crystal_shards", "crystal", base=(176, 132, 244), smooth=0)
    for (dx, dy, dz, s0, ln, wd) in SHARDS:
        d = np.array([dx, dy, dz], float)
        d /= np.linalg.norm(d)
        ml.loft(ch, tuple(d), (dx * 3 * K, Y(s0), dz * 3 * K), [(0, 0.6, 0.6), (ln * K * 0.15, 1.0, 1.0), (ln * K, 0.0, 0.0)], ml.lozenge(wd * K, wd * K * 0.6))
    # ---- 13 the glowing orb in the heart of the crown
    ob = w.add(m, "crystal_orb", "crystal", base=(210, 170, 255), smooth=60, uv_mode="loft")
    ml.loft(ob, "y", (0, Y(ORB_S) - ORB_R, 0), [(2 * ORB_R * (1 - math.cos(math.pi * i / 10)) / 2, math.sin(math.pi * i / 10) * 1.0, math.sin(math.pi * i / 10) * 1.0) for i in range(11)], ml.circle(ORB_R, 10))
    # ---- 14 the halo: a thin ring of lilac light behind the crystals (glow only)
    ha = ml.Part("halo", layer=2, material="crystal", base=(210, 170, 255), smooth=40)
    m.add(ha)
    halo_pts = [(HALO_R * math.cos(2 * math.pi * i / 28), Y(HALO_S) + HALO_R * 0.55 * math.sin(2 * math.pi * i / 28), -1.0 + 0.5 * math.sin(2 * math.pi * i / 28)) for i in range(29)]
    ml.sweep(ha, halo_pts, ml.circle(HALO_TUBE, 6), caps=(False, False))

    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    _paint(m, atlas)
    ref = w.reference("img/784541197571317248.jpg", (430, 56, 605, 762), "paradox")
    w.finish("paradox", m, atlas, real_m=TOTAL_M, glow=("FFFFFF", 0.10, 0.45, 1.0), ref=ref)


def _paint(m, atlas):
    # the crystals and shards: violet glow through the whole body, hotter toward the tips
    def crystal(c):
        w.set_glow(c, (176, 120, 255), 0.55, core=0.35)
        k = np.clip(c.T, 0, 1)[..., None] * 0.30
        c.img[..., :3] = c.img[..., :3] * (1 - k) + np.array([236, 220, 255], np.float32) * k
    for nm in ("crystal_core", "crystal_shards"):
        w.paint_part(atlas, m.part(nm), crystal)
    w.paint_part(atlas, m.part("crystal_orb"), lambda c: w.set_glow(c, (226, 190, 255), 0.95, core=0.6))
    w.paint_part(atlas, m.part("halo"), lambda c: w.set_glow(c, (190, 140, 255), 0.6, core=0.25))
    # the cage glows; the thorns stay unlit (a glow copy of every thorn quad would cost the mesh its budget)
    for nm, s in (("cage_spikes", 0.4), ("cage_ring", 0.3)):
        w.paint_part(atlas, m.part(nm), lambda c, s=s: w.set_glow(c, (150, 100, 235), s, core=0.2))
    # the grip's bindings: darker rings, a few flecks of green light
    def binding(c):
        n = w.noise2(c.img.shape[:2], 3.0, 21)
        c.img[..., :3] *= (0.85 + 0.3 * n[..., None])
    w.paint_part(atlas, m.part("green_binding"), binding)
