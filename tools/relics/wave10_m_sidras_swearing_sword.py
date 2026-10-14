"""Sidras' swearing sword (card: Magwa's Magic Item Compendium, "Sidras' swearing sword", rare, shortsword +1, requires attunement): a short, broad sword with
a pale white-lavender crystalline blade and crystal shards growing at its base, a grey metal flared cross-guard with angular ends and a chunky
faceted pommel (a pen-and-ink sketch). The 3D model, built with the owner's method.

Reference: the card, 661 x 1920; the sword at the top. The pommel is at (95, 590), the point at (585, 60): 722 px; the sword is 70 cm long, so
K = 0.097 cm per px. s = px from the pommel along the axis. Axis: y up from the pommel to the point; the broad face looks toward +z.
"""
import math

import numpy as np

import mesh_lib as ml
import wave1_models as w
from wave10_models import card, tube, glow_all, wrapped_grip

# ---------------------------------------------------------------- 1 measure
TOTAL = 70.0
S_TIP = 722.0
K = TOTAL / S_TIP
Y_POMMEL = 4.0                                        # the chunky pommel, card s 0..42
Y_GRIP0, Y_GRIP1 = 4.0, 14.5                          # the grip between pommel and guard (card s 42..150)
Y_GUARD = 15.8                                        # the cross-guard's centre, card s 163
GUARD_HALF = 6.8                                      # the guard spans 140 px on the card: 13.6 cm
BLADE_W, BLADE_T = 8.0, 1.0                           # the blade is 80 px wide on the card (broad for a shortsword), thick as a lozenge
Y_BLADE0 = Y_GUARD + 0.6
R_GRIP = 1.4
N_SHARDS = 9                                          # the crystals growing out of the blade's root
RNG = np.random.default_rng(6)


def build():
    m = ml.Mesh("sidras_swearing_sword")
    # ---- 1 the crystal blade: a broad lozenge with a slight ridge, a tapered point; 2 the midrib of the crystal
    bl = w.add(m, "blade", "crystal", base=(228, 216, 244), smooth=25, uv_mode="loft", glow=True)
    ml.loft(bl, "y", (0, 0, 0), [(Y_BLADE0 - 0.6, 0.8, 0.9), (Y_BLADE0 + 4, 1.0, 1.0), (Y_BLADE0 + 20, 1.0, 1.0), (Y_BLADE0 + 38, 1.0, 1.0), (TOTAL - 13.0, 0.98, 0.95), (TOTAL - 5.0, 0.6, 0.7), (TOTAL - 0.8, 0.2, 0.3), (TOTAL, 0.0, 0.0)],
            ml.lozenge(BLADE_W, BLADE_T, 0.28))
    mr = w.add(m, "midrib", "crystal", base=(246, 238, 255), smooth=30, glow=True)
    for face in (1, -1):
        ml.fuller(mr, (0.0, Y_BLADE0 + 3, face * BLADE_T * 0.5), (0.0, TOTAL - 8.0, face * BLADE_T * 0.36), (0, 0, face), 1.7, 0.4, 0.14, taper=6.0)
    # ---- 3 the shards growing from the root: hexagonal crystal prisms leaning out, long ones at the sides, short ones in the middle
    sh = w.add(m, "crystal_shards", "crystal", base=(214, 196, 238), smooth=0, glow=True)
    for i in range(N_SHARDS):
        a = (i / N_SHARDS - 0.5) * 2.4
        side = -1 if i % 2 else 1
        x = side * (0.9 + 0.55 * (i // 2))
        ln = 3.6 + 2.6 * RNG.random() + (1.6 if i % 3 == 0 else 0.0)
        tilt = side * (0.3 + 0.12 * (i // 2))
        axis = (tilt, 1.0, (RNG.random() - 0.5) * 0.9)
        r = 0.55 + 0.25 * RNG.random()
        ml.lathe(sh, axis, (x, Y_BLADE0 + 0.4 + 0.5 * RNG.random(), (RNG.random() - 0.5) * 0.6), [(-0.6, r * 0.9), (0.0, r), (ln * 0.8, r * 0.85), (ln, 0.0)], sides=6)
    # ---- 4 the cross-guard: a flared bar with chamfered corners, 5 its angular wing tips
    gd = w.add(m, "guard", "iron", base=(122, 124, 136), smooth=25)
    ml.loft(gd, "x", (0, Y_GUARD, 0), [(-GUARD_HALF + 0.6, 2.2, 1.2), (-GUARD_HALF + 1.6, 3.6, 1.5), (-3.8, 1.5, 1.1), (0.0, 1.3, 1.2), (3.8, 1.5, 1.1), (GUARD_HALF - 1.6, 3.6, 1.5), (GUARD_HALF - 0.6, 2.2, 1.2)],
            ml.octagon(1.0, 1.0, 0.28))
    gt = w.add(m, "guard_tips", "iron", base=(158, 160, 172), smooth=0)
    for sgn in (-1, 1):
        ml.lathe(gt, "x", (sgn * (GUARD_HALF - 0.6), Y_GUARD, 0), [(0.0, 2.2), (sgn * 0.9, 2.7), (sgn * 1.8, 0.0)], sides=4, squash=0.55)
    # ---- 6 the collar (a faceted ring where the blade meets the guard), 7 the grip with its wraps (8)
    cl = w.add(m, "collar", "iron", base=(138, 140, 152), smooth=20)
    ml.lathe(cl, "y", (0, 0, 0), [(Y_GUARD + 0.3, R_GRIP * 1.15), (Y_GUARD + 0.8, R_GRIP * 1.7), (Y_GUARD + 1.5, R_GRIP * 1.35), (Y_GUARD + 1.9, R_GRIP * 0.8)], sides=6, squash=1.0)
    wrapped_grip(m, Y_GRIP0, Y_GRIP1, R_GRIP, (56, 50, 64), (96, 88, 110), 6, name="grip", swell=1.08)
    # ---- 9 the chunky faceted pommel, 10 the ridge band round it
    pm = w.add(m, "pommel", "iron", base=(118, 120, 132), smooth=0)
    ml.lathe(pm, "y", (0, 0, 0), [(0.0, 0.9), (0.4, 2.0), (1.8, 2.7), (3.2, 2.3), (Y_POMMEL, R_GRIP * 1.0)], sides=6, squash=0.85)
    pb = w.add(m, "pommel_band", "silver", base=(176, 178, 192), wear=0.5, smooth=0)
    ml.collar(pb, 1.5, 2.8, 0.7, lip=0.2, sides=6, oval=0.85, caps=(False, False))

    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    _paint(m, atlas)
    ref = card("p3/Relics of the world part 3/e87cdeb913942bd1bd67b7921f4a71f3.jpg", (40, 20, 661, 640), "sidras_swearing_sword")
    w.finish("sidras_swearing_sword", m, atlas, real_m=TOTAL / 100.0, glow=("FFFFFF", 0.16, 0.3, 0.9), ref=ref)


def _paint(m, atlas):
    def crystal(c):
        sh = c.img.shape[:2]
        n = w.noise2(sh, 11, 8)
        fac = np.floor(c.S * 5 + n * 0.6) % 3
        tintc = np.array([[236, 226, 250], [214, 200, 238], [244, 238, 255]], np.float32)[fac.astype(int)]
        c.img[..., :3] = np.clip(tintc * (0.9 + 0.2 * n[..., None]), 0, 255)
        w.set_glow(c, (200, 170, 255), 0.22, mask=(n > 0.55).astype(np.float32) * 0.6, core=0.2)
    w.paint_part(atlas, m.part("blade"), crystal)
    for nm in ("midrib", "crystal_shards"):
        glow_all(atlas, m.part(nm), (210, 180, 255), 0.45, 0.3)
