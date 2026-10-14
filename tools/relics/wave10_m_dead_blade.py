"""Dead Blade (card: @jaaamesperrett, "Dead Blade", 1d12 slashing, Finger of Death once per long rest): a glaive: a long thin dark purple-grey pole with a huge
crescent, axe-like blade glowing neon pink, the blade textured with piles of skulls, a pink glow aura round it. The 3D model, built with the
owner's method.

Reference: the card screenshot, 1080 x 2400 (it shows 900 x 2000; figures in the 900-wide view, x 1.2 for the file): the pole runs from the butt (690, 1240) to
the top (298, 600): 750 px; the blade covers the top 58 percent of it and reaches 190 px to the left of the pole. A glaive of 1.9 m with a blade
that large would not be carried: the blade is drawn at 60 percent of the card's size (70 cm long, 32 cm broad), the pole is 1.85 m, so
K = 0.247 cm per px for the pole and the blade's px are scaled by 0.6. Axis: y up from the butt; the blade lies to -x; its face looks at +z.
"""
import math

import numpy as np

import mesh_lib as ml
import wave1_models as w
from wave10_models import card, tube, glow_all

# ---------------------------------------------------------------- 1 measure
TOTAL = 185.0
R_POLE = 1.5                                           # the pole is 14 px thick on the card; drawn heavy, 3 cm across
Y_BLADE0, Y_BLADE1 = TOTAL - 78.0, TOTAL - 4.0         # the blade's lower tip and its top, along the pole (78 cm: the card's 435 px x 0.6 x 0.247 ... scaled for the hand)
Y_RUNES = Y_BLADE0 - 8.0                               # the carved runes on the pole below the blade, card (385, 790)
BLADE_T = 0.9                                          # half thickness of the blade plate
# the blade's width (cm, to -x of the pole) along its length, read off the card: a long tail to the point, the belly a third of the way down, a squarer top
WIDTH = w.hermite([(Y_BLADE0, 0.0), (Y_BLADE0 + 8, 5.0), (Y_BLADE0 + 22, 14.0), (Y_BLADE0 + 38, 24.0), (Y_BLADE0 + 52, 31.0), (Y_BLADE0 + 62, 28.0), (Y_BLADE0 + 70, 17.0), (Y_BLADE1, 3.0)])
N_SKULLS = 16
RNG = np.random.default_rng(13)


def build():
    m = ml.Mesh("dead_blade")
    # ---- 1 the pole: a long thin loft of dark purple-grey wood, a little thicker under the blade
    pl = w.add(m, "pole", "wood", base=(70, 58, 84), smooth=45, uv_mode="loft")
    ml.loft(pl, "y", (0, 0, 0), [(0.0, 0.85, 0.85), (20, 0.95, 0.95), (Y_BLADE0, 1.0, 1.0), (Y_BLADE1 - 4, 1.08, 1.08), (TOTAL - 8.0, 1.0, 1.0)], ml.circle(R_POLE, 10))
    # ---- 2 the blade plate: a lozenge whose width follows the card's silhouette; its right edge sits on the pole (du = -width)
    bl = w.add(m, "blade", "obsidian", base=(214, 40, 168), smooth=40, uv_mode="loft", glow=True)
    st = []
    ys = [Y_BLADE0, Y_BLADE0 + 3, Y_BLADE0 + 8, Y_BLADE0 + 16, Y_BLADE0 + 28, Y_BLADE0 + 40, Y_BLADE0 + 52, Y_BLADE0 + 62, Y_BLADE0 + 70, Y_BLADE1 - 1.5, Y_BLADE1]
    for yy in ys:
        hw = max(0.0, WIDTH(yy)) / 2.0
        if yy <= Y_BLADE0 + 0.01 or yy >= Y_BLADE1 - 0.01:
            st.append((yy, 0.0, 0.0, -hw - 0.6, 0.0))
        else:
            st.append((yy, hw, BLADE_T * (0.55 + 0.45 * min(1.0, hw / 8.0)), -hw - 0.6, 0.0))
    ml.loft(bl, "y", (0, 0, 0), st, ml.lozenge(2.0, 2.0, 0.3))
    # ---- 3 the raised rim along the curved cutting edge (a bead following the outline), 4 the spine bar where the blade meets the pole
    rm = w.add(m, "blade_rim", "obsidian", base=(255, 110, 220), smooth=60, glow=True)
    for face in (1, -1):
        pts = [(-WIDTH(yy) - 0.6, yy, face * BLADE_T * 0.55) for yy in np.linspace(Y_BLADE0 + 4, Y_BLADE1 - 3, 16)]
        ml.sweep(rm, pts, ml.octagon(1.0, 1.0, 0.3), scales=lambda t: (0.5 * math.sin(math.pi * t) ** 0.5 + 0.1, 0.32), up=(0, 0, 1))
    sp = w.add(m, "spine_bar", "iron", base=(56, 44, 70), smooth=45)
    ml.loft(sp, "y", (0, 0, 0), [(Y_BLADE0 - 3.0, 0.9, 0.9), (Y_BLADE0 + 2.0, 1.0, 1.0), (Y_BLADE1 + 2.0, 1.0, 1.0), (Y_BLADE1 + 4.5, 0.6, 0.6)], ml.octagon(R_POLE * 2.1, R_POLE * 2.1, 0.5))
    # ---- 5 the piles of skulls: domed skull studs on both faces (eye holes are painted)
    sk = w.add(m, "skulls", "bone", base=(255, 176, 238), wear=0.5, smooth=50, glow=True)
    placed = 0
    tries = 0
    while placed < N_SKULLS and tries < 300:
        tries += 1
        yy = Y_BLADE0 + 10 + RNG.random() * 62
        wd = WIDTH(yy)
        if wd < 8:
            continue
        xx = -0.6 - wd * (0.1 + 0.8 * RNG.random())
        r = 1.3 + 0.6 * RNG.random()
        face = 1 if placed % 2 == 0 else -1
        ml.stud(sk, (xx, yy, face * BLADE_T * 0.5), r, r * 0.8, (0, 0, face), 6, 0.45)
        placed += 1
    # ---- 6 the ferrules: collars on the pole at the blade's two ends, 7 the glowing runes carved on the pole, 8 the butt spike
    fr = w.add(m, "ferrules", "iron", base=(52, 42, 64), wear=0.4, smooth=55)
    for yy in (Y_BLADE0 - 4.5, Y_BLADE1 + 0.6, 12.0, 60.0):
        ml.collar(fr, yy, R_POLE * 1.2, 1.4, lip=0.4, sides=10)
    rn = w.add(m, "runes", "obsidian", base=(255, 100, 220), glow=True, smooth=40)
    for k in range(5):
        yy = Y_RUNES - 3.0 * k
        ml.bar(rn, (-R_POLE * 0.2, yy, R_POLE * 1.0), (R_POLE * 0.5, yy + 1.0, R_POLE * 1.0), 0.32, 0.14, up=(0, 0, 1), taper=0.2)
        ml.bar(rn, (R_POLE * 0.2, yy + 0.2, R_POLE * 1.0), (-R_POLE * 0.3, yy - 1.0, R_POLE * 1.0), 0.28, 0.14, up=(0, 0, 1), taper=0.2)
    bs = w.add(m, "butt_spike", "iron", base=(60, 48, 76), smooth=45)
    ml.lathe(bs, "y", (0, 0, 0), [(-6.0, 0.0), (-1.5, 0.6), (0.0, R_POLE * 0.9), (1.8, R_POLE * 1.2)], sides=8)
    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    _paint(m, atlas)
    ref = card("p2/Relics of the world part 2/Screenshot_20261013_223047_Opera.jpg", (60, 720, 1080, 1560), "dead_blade")
    w.finish("dead_blade", m, atlas, real_m=TOTAL / 100.0, glow=("FFFFFF", 0.2, 0.45, 1.0), ref=ref)


def _paint(m, atlas):
    def blade(c):
        sh = c.img.shape[:2]
        gx, gy = c.A / 5.5, c.B / 5.5
        cx, cy = np.floor(gx) + 0.5, np.floor(gy) + 0.5
        h = np.sin(cx * 12.9898 + cy * 78.233) * 43758.5453
        jx, jy = (h % 1.0) - 0.5, ((h * 7.0) % 1.0) - 0.5
        dx, dy = gx - cx - jx * 0.3, gy - cy - jy * 0.3
        d = np.hypot(dx, dy)
        skull = d < 0.46
        eye = (np.hypot(np.abs(dx) - 0.17, dy + 0.06) < 0.1) | (np.hypot(dx, dy - 0.16) < 0.06)
        base = np.array([150, 14, 112], np.float32)
        lit = np.array([255, 120, 226], np.float32)
        col = np.where(skull[..., None], lit, base)
        col = np.where((skull & eye)[..., None], np.array([60, 0, 48], np.float32), col)
        n = w.noise2(sh, 5, 7)
        c.img[..., :3] = np.clip(col * (0.85 + 0.3 * n[..., None]), 0, 255)
        gl = (skull & ~eye).astype(np.float32) * 0.85 + 0.2
        w.set_glow(c, (255, 70, 214), 0.6, mask=np.clip(gl, 0, 1), core=0.3)
    w.paint_part(atlas, m.part("blade"), blade)
    glow_all(atlas, m.part("blade_rim"), (255, 90, 224), 0.8, 0.4)
    glow_all(atlas, m.part("skulls"), (255, 120, 230), 0.6, 0.3)
    glow_all(atlas, m.part("runes"), (255, 90, 224), 0.9, 0.4)
