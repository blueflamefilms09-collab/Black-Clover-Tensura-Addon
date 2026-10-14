"""Unicorn Dagger (card: @WondrousHomebrew, "Unicorn Dagger"): a dagger whose blade is a spiralled unicorn horn in pastel rainbow stripes with a
pearly tip, a golden-and-crystal hilt with ornate golden filigree curls as the guard, a red string tied round the grip with a small pink charm.
The 3D model, built with the owner's method.

Reference: the card, 740 x 740: the dagger runs from the horn's tip (100, 28) to the hilt's point (520, 430), 587 px; a dagger is 42 cm long, so
K = 0.0716 cm per card px. s = distance from the hilt's point along the axis. Axis: y up from the hilt's point to the horn's tip; the guard
curls spread in x.
"""
import math

import numpy as np

import mesh_lib as ml
import wave1_models as w
from wave10_models import card, tube, glow_all

# ---------------------------------------------------------------- 1 measure
TOTAL = 42.0                                          # a dagger (card px 587 x K)
K = TOTAL / 587.0
Y_POMMEL0, Y_POMMEL1 = 0.0, 3.4                       # the point-shaped crystal pommel
Y_GRIP0, Y_GRIP1 = 3.4, 13.2                          # the crystal grip (card (470,370) to (350,260): 160 px)
Y_GUARD = 13.4                                        # the guard's centre: where the filigree curls spring from (card (470, 345))
Y_HORN0 = 15.0                                        # the horn begins above the guard (card (350, 260))
R_HORN = 1.55                                          # horn radius at its base (card 55 px wide)
R_GRIP = 1.35
HORN_TURNS = 1.8                                      # the spiral flutes make 1.8 turns along the horn
FLUTES, FLUTE_D = 3, 0.12                             # three spiral flutes, 12 percent deep
CURL_R, CURL_TURNS = 2.6, 1.15                        # each guard curl: its first radius and the turns it makes


def horn_section(s):
    th = [2 * math.pi * i / 24 for i in range(24)]
    k = 2 * math.pi * HORN_TURNS / (TOTAL - Y_HORN0)
    return [((1 + FLUTE_D * math.cos(FLUTES * a - k * (s - Y_HORN0))) * math.cos(a), (1 + FLUTE_D * math.cos(FLUTES * a - k * (s - Y_HORN0))) * math.sin(a)) for a in th]


def build():
    m = ml.Mesh("unicorn_dagger")
    # ---- 1 the horn: a fluted twisted cone to a pearly point
    hn = w.add(m, "horn", "bone", base=(248, 226, 240), smooth=60, uv_mode="loft")
    stations = [(Y_HORN0 - 0.4, R_HORN * 0.95, R_HORN * 0.95)]
    n = 14
    for i in range(1, n):
        t = i / n
        r = R_HORN * (1.0 - t) ** 0.85
        stations.append((Y_HORN0 + (TOTAL - Y_HORN0) * t * 0.985, r, r))
    stations.append((TOTAL, 0.0, 0.0))
    ml.loft(hn, "y", (0, 0, 0), stations, horn_section)
    # ---- 2 a golden thread following one flute up the horn (a raised helical wire)
    wr = w.add(m, "horn_wire", "gold", base=(246, 214, 120), wear=0.2, smooth=70)
    pts = []
    for i in range(0, 25):
        t = i / 24
        yy = Y_HORN0 + 0.4 + (TOTAL - Y_HORN0 - 6.0) * t
        r = R_HORN * (1.0 - (yy - Y_HORN0) / (TOTAL - Y_HORN0)) ** 0.85 * 1.0
        a = 2 * math.pi * HORN_TURNS * (yy - Y_HORN0) / (TOTAL - Y_HORN0) / 1.0
        pts.append((r * 1.04 * math.cos(a), yy, r * 1.04 * math.sin(a)))
    ml.sweep(wr, pts, ml.circle(1.0, 5), scales=lambda t: (0.12 * (1 - 0.5 * t),) * 2)
    # ---- 3 the pearl at the horn's foot (the cap where it meets the guard)
    cap = w.add(m, "horn_cap", "silver", base=(250, 240, 250), smooth=55)
    ml.lathe(cap, "y", (0, 0, 0), [(Y_HORN0 - 1.6, R_GRIP * 1.1), (Y_HORN0 - 0.9, R_HORN * 1.12), (Y_HORN0 - 0.1, R_HORN * 1.05), (Y_HORN0 + 0.5, R_HORN * 0.92)], sides=12)
    # ---- 4 the guard's core: a small golden barrel the curls spring from
    gc = w.add(m, "guard_core", "gold", base=(230, 190, 90), smooth=45)
    ml.lathe(gc, "x", (0, Y_GUARD, 0), [(-2.2, 0.55), (-1.4, 1.05), (0.0, 1.25), (1.4, 1.05), (2.2, 0.55)], sides=10, squash=0.7)
    # ---- 5/6 the two ornate filigree curls (mirrored): a flat ribbon that sweeps out and winds into a scroll
    for sgn, nm in ((-1, "curl_l"), (1, "curl_r")):
        cp = w.add(m, nm, "gold", base=(236, 196, 96), smooth=50, uv_mode="loft")
        path = []
        for i in range(0, 31):
            t = i / 30
            a = -0.6 + sgn * 0 + 2 * math.pi * CURL_TURNS * t
            r = CURL_R * (1.0 - 0.72 * t)
            cx, cy = sgn * (2.2 + CURL_R * 0.9), Y_GUARD - 0.6 - CURL_R * 0.3
            path.append((cx + sgn * r * math.cos(a) * -1 if False else cx + sgn * r * math.sin(a) * 1.0, cy - r * math.cos(a) * 1.15, 0.12 * math.sin(6 * t)))
        ml.sweep(cp, path, ml.rect_ring(1.0, 1.0), scales=lambda t: (0.55 * (1 - 0.6 * t) if t < 0.97 else 0.0, 0.3 if t < 0.97 else 0.0), up=(0, 0, 1))
    # ---- 7 the crystal grip, 8 gold bands round it
    cg = w.add(m, "crystal_grip", "crystal", base=(206, 236, 255), smooth=45, glow=True, uv_mode="loft")
    ml.loft(cg, "y", (0, 0, 0), [(Y_GRIP0 + 0.6, 0.78, 0.78), (Y_GRIP0 + 2.6, 1.0, 1.0), ((Y_GRIP0 + Y_GRIP1) / 2, 1.1, 1.1), (Y_GRIP1 - 1.5, 1.0, 1.0), (Y_GRIP1, 1.2, 1.2)], ml.circle(R_GRIP, 8))
    gb = w.add(m, "grip_bands", "gold", base=(238, 200, 100), wear=0.3, smooth=60)
    for yy in (Y_GRIP0 + 1.4, Y_GRIP1 - 0.6, Y_GRIP1 - 4.2):
        ml.collar(gb, yy, R_GRIP * 1.12, 0.7, lip=0.2, sides=10, caps=(False, False))
    # ---- 9 the pommel: a faceted point (a gold-capped crystal drop)
    pm = w.add(m, "pommel", "crystal", base=(190, 232, 255), smooth=0, glow=True)
    ml.lathe(pm, "y", (0, 0, 0), [(Y_POMMEL0, 0.0), (Y_POMMEL0 + 0.9, 0.9), (Y_POMMEL0 + 2.2, 1.35), (Y_POMMEL1 + 0.2, 1.0)], sides=6)
    # ---- 10 the red string wound round the grip and 11 its charm
    st = w.add(m, "string", "cloth", base=(206, 44, 70), wear=0.1, smooth=60)
    pts = w.helix(lambda t: R_GRIP * 1.35, Y_GRIP0 + 4.0, Y_GRIP1 - 2.0, 3.0, 36)
    ml.sweep(st, pts, ml.circle(1.0, 4), scales=lambda t: (0.1, 0.1))
    tail = [(R_GRIP * 1.3, Y_GRIP0 + 4.0, 0.0), (R_GRIP * 1.9, Y_GRIP0 + 2.2, 0.6), (2.6, Y_GRIP0 + 0.8, 1.6), (3.4, Y_GRIP0 - 0.8, 1.4)]
    ml.sweep(st, tail, ml.circle(1.0, 4), scales=lambda t: (0.1, 0.1))
    ch = w.add(m, "charm", "cloth", base=(250, 150, 176), smooth=40)
    ml.lathe(ch, "z", (3.4, Y_GRIP0 - 1.6, 1.4), [(-0.15, 0.9), (0.0, 1.0), (0.15, 0.9)], sides=10)
    cr = w.add(m, "charm_stud", "gold", base=(255, 220, 140))
    ml.stud(cr, (3.4, Y_GRIP0 - 1.6, 1.58), 0.38, 0.2, (0, 0, 1), 6, 0.5)

    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    _paint(m, atlas)
    ref = card("p3/Relics of the world part 3/b114f121c73f90b92d362acc321b700c.jpg", (80, 0, 700, 540), "unicorn_dagger")
    w.finish("unicorn_dagger", m, atlas, real_m=TOTAL / 100.0, glow=("FFFFFF", 0.16, 0.4, 1.0), ref=ref)


def _paint(m, atlas):
    stripes = [np.array(c, np.float32) for c in ((255, 168, 204), (255, 230, 150), (170, 238, 204), (200, 178, 255))]

    def rainbow(c):
        sh = c.img.shape[:2]
        n = w.noise2(sh, 9, 4)
        idx = np.floor(((c.S - HORN_TURNS * (1 - c.T) * 0.0) * 4.0 + c.T * 4.0 * HORN_TURNS) % 4.0).astype(int)
        col = np.zeros(sh + (3,), np.float32)
        for k in range(4):
            col[idx == k] = stripes[k]
        col = col * (0.86 + 0.26 * n[..., None])
        tipw = np.clip((c.T - 0.82) / 0.18, 0, 1)[..., None]                      # the tip fades to pearl
        col = col * (1 - tipw) + np.array([250, 244, 252], np.float32) * tipw
        c.img[..., :3] = np.clip(col, 0, 255)
        w.set_glow(c, (255, 220, 250), 0.24, mask=(0.35 + 0.65 * tipw[..., 0]), core=0.3)
    w.paint_part(atlas, m.part("horn"), rainbow)
    for nm in ("crystal_grip", "pommel"):
        w.paint_part(atlas, m.part(nm), lambda c: (w.tint(c.img, (190, 230, 255), 0.35), w.set_glow(c, (170, 230, 255), 0.4, core=0.3)))
    w.paint_part(atlas, m.part("horn_wire"), lambda c: w.set_glow(c, (255, 226, 140), 0.5, core=0.2))
