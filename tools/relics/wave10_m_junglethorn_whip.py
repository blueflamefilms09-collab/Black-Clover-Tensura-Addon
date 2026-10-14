"""Junglethorn Whip (card: Catilus, "Junglethorn Whip", weapon (whip), very rare, requires attunement): a long thorny vine, green-brown with an orange
tint and many small thorns, coiled in a spiral and ending in a claw-like thorn; the handle wrapped in green vine, a clawed pommel and bright red,
orange and purple feathers. The 3D model, built with the owner's method.

Reference: the card, 736 x 1019. The handle runs from the clawed pommel (690, 428) to where the vine leaves it (590, 262): 195 px; a whip's handle is
30 cm, so K = 0.154 cm per px. The vine winds out of the handle in a spiral of 2.4 turns (outer radius about 150 px on the card) and ends in a thorn
tip at (330, 462). In hand the vine hangs from the handle in a loose helix (a whip is carried coiled) of 2.4 turns and a tail.
Axis: y up from the pommel; the vine spirals round the y axis above the handle.
"""
import math

import numpy as np

import mesh_lib as ml
import wave1_models as w
from wave10_models import card, tube, glow_all, wrapped_grip

# ---------------------------------------------------------------- 1 measure
K = 30.0 / 195.0                                      # cm per card px (handle 195 px = 30 cm)
HANDLE_LEN = 30.0
R_HANDLE = 1.6                                        # the handle is 20 px wide on the card
Y_HANDLE0, Y_HANDLE1 = 3.0, 3.0 + HANDLE_LEN          # above the clawed pommel
COIL_TURNS = 2.4                                      # the card's spiral
COIL_R0, COIL_R1 = 11.0, 5.0                          # radius of the first and the last turn (card 150 px of outer radius = 23 cm, scaled to a hand-carried coil)
COIL_PITCH = 4.2                                      # rise per turn: the coil hangs open like a spring
VINE_R0, VINE_R1 = 0.75, 0.22                         # vine radius at the handle and at the tip (the card's vine is about 5 px: 0.8 cm)
N_THORNS = 34                                         # thorns along the vine
RNG = np.random.default_rng(4)


def vine_point(t):
    """Point of the vine at t in 0..1: the straight start up the handle's axis, then the helix, then a slack tail that swings out."""
    y0 = Y_HANDLE1 - 1.0
    turns = COIL_TURNS
    if t < 0.08:                                       # leaves the handle and swings out
        u = t / 0.08
        r = COIL_R0 * u * u
        a = 0.0
        return (r * math.cos(a), y0 + 3.0 * u, r * math.sin(a))
    u = (t - 0.08) / 0.92
    r = COIL_R0 + (COIL_R1 - COIL_R0) * u
    a = 2 * math.pi * turns * u
    return (r * math.cos(a), y0 + 3.0 + COIL_PITCH * turns * u, r * math.sin(a))


def build():
    m = ml.Mesh("junglethorn_whip")
    # ---- 1 the handle wrapped in green vine, 2 its binding rings
    wrapped_grip(m, Y_HANDLE0, Y_HANDLE1, R_HANDLE, (86, 124, 58), (52, 90, 44), 8, name="handle", wrap_material="leather", swell=1.06)
    # ---- 3 a vine wound round the handle (a helix of thin vine)
    hv = w.add(m, "handle_vine", "leather", base=(70, 122, 56), wear=0.3, smooth=70)
    pts = w.helix(lambda t: R_HANDLE * 1.16, Y_HANDLE0 + 1.5, Y_HANDLE1 - 1.0, 4.0, 40)
    ml.sweep(hv, pts, ml.circle(1.0, 5), scales=lambda t: (0.34, 0.34))
    # ---- 4 the long thorny vine coiled out of the handle
    vn = w.add(m, "whip_vine", "wood", base=(98, 96, 44), smooth=70, uv_mode="loft")
    n = 90
    vpts = [vine_point(i / n) for i in range(n + 1)]
    ml.sweep(vn, vpts, ml.circle(1.0, 5), scales=lambda t: (VINE_R0 + (VINE_R1 - VINE_R0) * t,) * 2 if t < 0.985 else (0.0, 0.0))
    # ---- 5 the thorns: small cones standing out of the vine all along it
    th = w.add(m, "thorns", "bone", base=(214, 96, 52), wear=0.5, smooth=0)
    for i in range(N_THORNS):
        t = 0.1 + 0.88 * i / (N_THORNS - 1)
        p = np.array(vine_point(t))
        q = np.array(vine_point(min(1.0, t + 0.01)))
        tan = (q - p) / max(1e-6, np.linalg.norm(q - p))
        side = np.cross(tan, (0, 1, 0))
        side = side / max(1e-6, np.linalg.norm(side))
        ang = (i % 3) * 2.1
        out = side * math.cos(ang) + np.cross(tan, side) * math.sin(ang)
        r = VINE_R0 + (VINE_R1 - VINE_R0) * t
        axis = out * 0.8 + tan * 0.5
        ml.lathe(th, tuple(axis), tuple(p + out * r * 0.7), [(-0.2, 0.28 * (1 - 0.5 * t)), (0.0, 0.3 * (1 - 0.5 * t)), (0.95 * (1 - 0.4 * t), 0.0)], sides=4, caps=(False, False))
    # ---- 6 the claw-like thorn at the tip of the whip
    tc = w.add(m, "tip_claw", "bone", base=(226, 190, 150), smooth=60)
    e = np.array(vine_point(1.0))
    d = np.array(vine_point(0.985))
    tan = (e - d) / max(1e-6, np.linalg.norm(e - d))
    ml.sweep(tc, [tuple(e - tan * 0.2), tuple(e + tan * 1.2 + (0, 0.4, 0)), tuple(e + tan * 2.4 + (0, 1.3, 0)), tuple(e + tan * 3.0 + (0, 2.6, 0))], ml.circle(1.0, 5),
             scales=lambda t: (0.34 * (1 - t) + 0.02 if t < 0.97 else 0.0,) * 2)
    # ---- 7 the clawed pommel: three curved claws under the handle, 8 their ring
    pm = w.add(m, "pommel_claws", "bone", base=(212, 176, 130), smooth=60)
    for k in range(3):
        a = 2 * math.pi * k / 3 + 0.4
        c, s = math.cos(a), math.sin(a)
        ml.sweep(pm, [(c * 0.4, Y_HANDLE0 + 0.4, s * 0.4), (c * 1.6, Y_HANDLE0 - 1.2, s * 1.6), (c * 2.2, Y_HANDLE0 - 3.0, s * 2.2), (c * 1.4, Y_HANDLE0 - 4.4, s * 1.4)], ml.circle(1.0, 5),
                 scales=lambda t: (0.55 * (1 - 0.8 * t) if t < 0.97 else 0.0,) * 2)
    pr = w.add(m, "pommel_ring", "bronze", base=(120, 150, 70), smooth=50)
    ml.collar(pr, Y_HANDLE0 - 0.5, R_HANDLE * 1.3, 1.1, lip=0.3, sides=10)
    # ---- 9 the feathers tied below the pommel: red, orange, purple
    for (nm, col, dx, dz, ln) in (("feather_red", (226, 56, 48), 1.0, 0.4, 13.0), ("feather_orange", (238, 140, 40), -0.8, 0.9, 11.5), ("feather_purple", (150, 70, 190), 0.2, -1.1, 12.0)):
        ft = w.add(m, nm, "cloth", base=col, wear=0.1, smooth=40, uv_mode="loft")
        path = [(dx * 0.3, Y_HANDLE0 - 0.6, dz * 0.3), (dx * 1.4, Y_HANDLE0 - ln * 0.35, dz * 1.4), (dx * 3.0, Y_HANDLE0 - ln * 0.7, dz * 3.0), (dx * 4.4, Y_HANDLE0 - ln, dz * 4.2)]
        f = ml.sweep(ft, path, ml.rect_ring(1.0, 1.0), scales=lambda t: (1.5 * math.sin(math.pi * min(1.0, t * 0.9 + 0.1)) ** 0.7 + 0.05 if t < 0.97 else 0.0, 0.07 if t < 0.97 else 0.0), up=(0, 0, 1))
    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    _paint(m, atlas)
    ref = card("p3/Relics of the world part 3/3716a63dac5b609a36f2a9822ba2c069.jpg", (340, 160, 736, 680), "junglethorn_whip")
    w.finish("junglethorn_whip", m, atlas, real_m=1.15, glow=("FFFFFF", 0.14, 0.3, 0.9), ref=ref)


def _paint(m, atlas):
    def vine(c):
        sh = c.img.shape[:2]
        n = w.noise2(sh, 6, 3)
        orange = np.clip((c.T - 0.35) * 1.6, 0, 1)[..., None] * 0.55                 # the vine warms toward the tip
        col = np.array([92, 112, 46], np.float32) * (1 - orange) + np.array([176, 98, 46], np.float32) * orange
        c.img[..., :3] = np.clip(col * (0.8 + 0.45 * n[..., None]), 0, 255)
        w.set_glow(c, (140, 255, 100), 0.12, mask=(n > 0.72).astype(np.float32), core=0.0)
    w.paint_part(atlas, m.part("whip_vine"), vine)
    glow_all(atlas, m.part("thorns"), (255, 150, 90), 0.25, 0.0)
    for nm in ("feather_red", "feather_orange", "feather_purple"):
        w.paint_part(atlas, m.part(nm), lambda c: w.set_glow(c, (255, 190, 120), 0.12, core=0.0))
