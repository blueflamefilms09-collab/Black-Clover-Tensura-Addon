"""Stegosaurus Greatsword (card: "Stegosaurus Greatsword", +2 greatsword that clatters apart into a +2 whip): the greatsword's blade is a stegosaurus spine: a
column of pale vertebrae with spiked plates and a pair of curved side spikes near the tip; the hilt is wrapped in teal cord with a green band,
red-and-pink leaf-like blades as the guard, and a bone pommel. The 3D model, built with the owner's method (the sword form; the whip form is the
card's second picture and is played by the item's toggle).

Reference: the card, 736 x 736; the left figure, upright: pommel foot (125, 645), top spike (130, 172): 473 px; a greatsword is 1.4 m, so
K = 0.296 cm per px. s = px from the pommel's foot. Axis: y up; the guard's leaves spread in x.
"""
import math

import numpy as np

import mesh_lib as ml
import wave1_models as w
from wave10_models import card, tube, glow_all, wrapped_grip

# ---------------------------------------------------------------- 1 measure
TOTAL = 140.0
S_TOT = 473.0
K = TOTAL / S_TOT


def Y(s):
    return s * K


Y_POMMEL = (0.0, Y(30))                                # the bone pommel: two knobs, card s 0..30
Y_GRIP = (Y(30), Y(100))                               # the teal-wrapped grip, card s 35..105 (about 35 px wide: 10 cm; drawn heavy, 3 cm)
Y_BAND = (Y(100), Y(125))                              # the green binding band under the guard, card s 100..125
Y_GUARD = Y(120)                                       # the leaf guard centre, card s 120 (card y 525)
Y_COL0, Y_COL1 = Y(128), Y(445)                        # the vertebral column from the guard to the top spike
N_VERT = 11                                            # eleven vertebrae are drawn on the card
R_CENTRUM = 4.2                                        # a vertebral body is about 36 px wide on the card
PITCH = (Y_COL1 - Y_COL0) / N_VERT
Y_HORNS = Y(405)                                       # the pair of curved side spikes near the top (card (90..185, 215))
R_GRIP = 1.6
RNG = np.random.default_rng(9)


def build():
    m = ml.Mesh("stegosaurus_greatsword")
    # ---- 1 the vertebrae: eleven hourglass bodies with flared ends, in a column
    vt = w.add(m, "vertebrae", "bone", base=(236, 230, 208), smooth=45, wear=0.7)
    for i in range(N_VERT):
        y0 = Y_COL0 + i * PITCH
        r = R_CENTRUM * (1.0 - 0.18 * i / N_VERT)
        ml.lathe(vt, "y", (0, 0, 0), [(y0 + 0.2, r * 0.7), (y0 + 0.9, r * 1.05), (y0 + PITCH * 0.5, r * 0.72), (y0 + PITCH - 0.9, r * 1.05), (y0 + PITCH - 0.2, r * 0.7)], sides=8, squash=0.9)
    # ---- 2 the spiked plates: a spike on each side of every vertebra, alternating high and low, and a plate edge on the front
    sp = w.add(m, "spiked_plates", "bone", base=(214, 206, 178), smooth=0, wear=0.8)
    for i in range(N_VERT):
        y0 = Y_COL0 + i * PITCH + PITCH * 0.5
        r = R_CENTRUM * (1.0 - 0.18 * i / N_VERT)
        for sgn in (-1, 1):
            hi = 0.5 if (i + (sgn > 0)) % 2 else -0.5
            ml.lathe(sp, (sgn * 0.9, 0.35 * (1 if hi > 0 else -1), 0.0), (sgn * r * 0.75, y0 + hi, 0.0), [(-0.4, 0.75), (0.0, 0.85), (r * 0.75, 0.0)], sides=5)
        ml.lathe(sp, (0.0, 0.3, 1.0), (0.0, y0, r * 0.7), [(-0.3, 0.6), (0.0, 0.7), (1.6, 0.0)], sides=5)            # the plate on the front
    # ---- 3 the pair of curved side horns near the tip and the top spike
    hr = w.add(m, "horns", "bone", base=(228, 222, 200), smooth=55, uv_mode="loft")
    for sgn in (-1, 1):
        pts = [(sgn * 2.4, Y_HORNS, 0.0), (sgn * 6.4, Y_HORNS - 0.2, 0.0), (sgn * 10.6, Y_HORNS - 2.4, 0.0), (sgn * 13.6, Y_HORNS - 6.0, 0.0)]
        ml.sweep(hr, pts, ml.circle(1.0, 6), scales=lambda t: (1.15 * (1 - 0.88 * t) if t < 0.97 else 0.0,) * 2)
    ml.lathe(hr, "y", (0, 0, 0), [(Y_COL1 - 0.5, R_CENTRUM * 0.5), (Y_COL1 + 3.0, R_CENTRUM * 0.45), (Y_COL1 + 7.0, 0.0)], sides=6)
    # ---- 4 the green binding band, 5 the leaf guard (two pairs of pink-red diamonds each side, mirrored)
    bd = w.add(m, "guard_band", "cloth", base=(96, 112, 56), wear=0.4, smooth=50)
    ml.lathe(bd, "y", (0, 0, 0), [(Y_BAND[0], R_GRIP * 1.15), (Y_BAND[0] + 0.7, R_GRIP * 1.9), (Y_BAND[1] - 0.7, R_GRIP * 1.9), (Y_BAND[1], R_GRIP * 1.2)], sides=10, squash=0.8)
    lf = w.add(m, "guard_leaves", "cloth", base=(228, 94, 110), wear=0.2, smooth=0)
    for sgn in (-1, 1):
        for k, (dy, ln, wd) in enumerate(((0.0, 10.4, 3.0), (4.2, 8.0, 2.4), (-4.2, 8.0, 2.4))):
            ml.lathe(lf, "x", (sgn * R_GRIP * 1.5, Y_GUARD + dy, 0.0), [(0.0, wd * 0.45), (sgn * ln * 0.38, wd), (sgn * ln, 0.0)], sides=4, squash=0.22, start=math.pi / 4 * 0)
    # ---- 6 the teal-corded grip and its wraps, 7 the pommel: a bone with two knobs
    wrapped_grip(m, Y_GRIP[0], Y_GRIP[1], R_GRIP, (46, 96, 98), (28, 62, 66), 9, name="grip", wrap_material="cloth", material="cloth", swell=1.06)
    pm = w.add(m, "pommel", "bone", base=(226, 220, 196), smooth=55, wear=0.6)
    ml.lathe(pm, "y", (0, 0, 0), [(Y_POMMEL[0], 1.0), (Y_POMMEL[0] + 0.5, 2.2), (Y_POMMEL[0] + 1.6, 2.6), (Y_POMMEL[0] + 2.8, 1.5), (Y_POMMEL[0] + 3.6, 1.2), (Y_POMMEL[1] - 0.2, R_GRIP * 1.1)], sides=10)
    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    _paint(m, atlas)
    ref = card("p3/Relics of the world part 3/f700d88b2ba52475a7abdddda240574f.jpg", (60, 150, 400, 670), "stegosaurus_greatsword")
    w.finish("stegosaurus_greatsword", m, atlas, real_m=TOTAL / 100.0, glow=("FFFFFF", 0.1, 0.3, 0.8), ref=ref)


def _paint(m, atlas):
    def bone(c):
        sh = c.img.shape[:2]
        n = w.noise2(sh, 4, 2)
        crack = (np.abs(w.noise2(sh, 30, 9) - 0.5) < 0.015)
        c.img[..., :3] = np.clip(c.img[..., :3] * (0.88 + 0.22 * n[..., None]) * np.where(crack, 0.55, 1.0)[..., None], 0, 255)
        w.set_glow(c, (255, 120, 140), 0.08, mask=crack.astype(np.float32), core=0.0)
    for nm in ("vertebrae", "spiked_plates", "horns", "pommel"):
        w.paint_part(atlas, m.part(nm), bone)
    glow_all(atlas, m.part("guard_leaves"), (255, 110, 130), 0.22, 0.0)
