"""Catstaff (card: Catilus, "Catstaff", very rare, requires attunement, cat lover): a tall slender pale-wood staff topped by a carved sitting cat
with yellow eyes and red-brown face marks, a red cloth-wrapped grip in the middle, a cat's tail spiralling up the shaft, a second sitting cat
carved at the base with red-brown feet. The 3D model, built with the owner's method.

Reference: the card, 739 x 1024, the staff down the right at card x about 626. The top cat's ear tips are at card y 146, the base cat's feet end at 925.
Axis: y up from the feet (0) to the ear tips; the cats face +z.
"""
import math

import numpy as np

import mesh_lib as ml
import wave1_models as w

# ---------------------------------------------------------------- 1 measure
TOTAL_M = 1.70                                       # a quarterstaff, taller than a man's shoulder
P_TOP, P_FOOT = 146.0, 925.0                         # card y of the ear tips and of the feet
K = w.px_to_cm(TOTAL_M * 100.0, P_FOOT - P_TOP)      # cm per card px
AXIS = 626.0


def Y(py):
    return (P_FOOT - py) * K


SHAFT_R = 8.0 * K                                    # the bare shaft, card width 16 px
WRAP = (372.0, 800.0)                                # card y of the red wrap
R_WRAP = 9.5 * K                                     # card width 19 px
# the top cat (card y): ear tips 146, head 175..235 (58 px wide), chest 300 (42 px wide), paws 292..312, narrowing into the shaft at 366
HEAD_C, HEAD_RX, HEAD_RY, HEAD_RZ = 207.0, 34.0, 30.0, 26.0
EAR_BASE, EAR_TIP, EAR_X, EAR_W = 190.0, 140.0, 20.0, 25.0
EYE_Y, EYE_X = 200.0, 12.5
TORSO = [(366, 8.5), (340, 14), (312, 25), (280, 26), (250, 21), (232, 17)]      # (card y, half width px) of the cat's body down to the shaft
PAW_Y, PAW_X = 302.0, 7.0
# the base cat: haunches 775..900 (96 px across), feet 888..925
HAUNCH_C, HAUNCH_RX, HAUNCH_RY, HAUNCH_RZ, HAUNCH_X = 850.0, 25.0, 52.0, 23.0, 27.0
BASE_BODY = [(780, 9), (810, 15), (840, 22), (880, 24), (905, 22)]
FOOT_Y, FOOT_RX = 910.0, 13.0
TAIL_Y = (880.0, 548.0)                              # the tail winds up the shaft from the base cat to where it curls round the grip's top


def build():
    m = ml.Mesh("catstaff")
    # ---- 1 the shaft core, 2 the red wrap, 3 the wrap's cord rings
    sh = w.add(m, "shaft", "wood", base=(206, 184, 140), uv_mode="loft")
    ml.loft(sh, "y", (0, 0, 0), [Y(900), Y(330)], ml.circle(SHAFT_R, 12))
    wrap = w.add(m, "grip_wrap", "cloth", base=(150, 52, 52), uv_mode="loft")
    y0, y1 = Y(WRAP[1]), Y(WRAP[0])
    ml.loft(wrap, "y", (0, 0, 0), [(y0, 0.97, 0.97), ((y0 + y1) / 2, 1.0, 1.0), (y1, 0.97, 0.97)], ml.circle(R_WRAP, 14))
    cords = w.add(m, "wrap_cords", "cloth", base=(196, 82, 76), wear=0.3)
    n = 34
    for i in range(n):
        ml.collar(cords, y0 + 0.5 + (y1 - y0 - 1.6) * i / (n - 1), R_WRAP * 1.05, 0.55, lip=0.14, sides=14)
    # ---- 4 the top cat's body: a tapering torso
    body = w.add(m, "top_cat_body", "wood", base=(186, 162, 126), smooth=45, uv_mode="loft")
    st = [(Y(py), hw * K / (TORSO[0][1] * K), hw * K / (TORSO[0][1] * K) * 0.8) for py, hw in TORSO]
    ml.loft(body, "y", (0, 0, 0), st, ml.circle(TORSO[0][1] * K, 16))
    # ---- 5 the head, 6/7 ears with their red-brown inner, 8 eyes, 9 face mark, 10 nose and mouth, 11 whiskers
    head = w.add(m, "top_cat_head", "wood", base=(184, 160, 126), smooth=60)
    w.ellipsoid(head, (0, Y(HEAD_C), 0), HEAD_RX * K, HEAD_RY * K, HEAD_RZ * K, sides=18, stacks=12)
    for sgn, nm in ((-1, "l"), (1, "r")):
        ear = w.add(m, f"ear_{nm}", "wood", base=(176, 150, 116), smooth=50)
        sec = [(EAR_W * K / 2, -0.55), (-EAR_W * K / 2, -0.55), (0.0, 0.7)]
        ml.loft(ear, "y", (sgn * EAR_X * K, Y(EAR_BASE), 0), [(0, 1, 1), ((EAR_BASE - EAR_TIP) * K * 0.55, 0.75, 0.75), ((EAR_BASE - EAR_TIP) * K, 0.0, 0.0)], sec)
        inner = w.add(m, f"ear_inner_{nm}", "cloth", base=(150, 70, 52), smooth=0)
        ml.loft(inner, "y", (sgn * EAR_X * K, Y(EAR_BASE), 0.55), [(0.6, 0.6, 0.35), ((EAR_BASE - EAR_TIP) * K * 0.6, 0.3, 0.2), ((EAR_BASE - EAR_TIP) * K * 0.85, 0.0, 0.0)], sec)
    for sgn, nm in ((-1, "l"), (1, "r")):
        eye = w.add(m, f"eye_{nm}", "crystal", base=(255, 214, 70), glow=True, smooth=40)
        w.ellipsoid(eye, (sgn * EYE_X * K, Y(EYE_Y), HEAD_RZ * K * 0.82), 3.6 * K, 2.8 * K, 1.4 * K, sides=10, stacks=6)
    mark = w.add(m, "face_mark", "cloth", base=(156, 70, 50), wear=0.2)
    ml.bar(mark, (0, Y(HEAD_C - 24), HEAD_RZ * K * 0.96), (0, Y(HEAD_C - 2), HEAD_RZ * K * 0.99), 3.2 * K, 0.1, up=(0, 0, 1), taper=1.0)
    for sgn in (-1, 1):
        ml.bar(mark, (sgn * 3 * K, Y(HEAD_C - 20), HEAD_RZ * K * 0.95), (sgn * 11 * K, Y(HEAD_C - 8), HEAD_RZ * K * 0.97), 2.0 * K, 0.08, up=(0, 0, 1), taper=0.8)
    nose = w.add(m, "nose_mouth", "cloth", base=(200, 120, 110), wear=0.2)
    ml.lathe(nose, "y", (0, Y(HEAD_C + 9), HEAD_RZ * K * 0.97), [(-0.4, 0.0), (-0.15, 0.5), (0.25, 0.45), (0.5, 0.0)], sides=6)
    whisk = w.add(m, "whiskers", "bone", base=(236, 232, 220), wear=0.1)
    for sgn in (-1, 1):
        for k in range(3):
            a = math.radians(12 * (k - 1))
            p0 = (sgn * 5 * K, Y(HEAD_C + 8 + k * 1.5), HEAD_RZ * K * 0.9)
            p1 = (sgn * (5 * K + 5.5 * math.cos(a)), Y(HEAD_C + 8 + k * 1.5) + 5.5 * math.sin(a) * 0.6, HEAD_RZ * K * 0.9 + 1.5)
            ml.bar(whisk, p0, p1, 0.08, 0.04, up=(0, 0, 1), section="rect")
    # ---- 12/13 the clasped paws
    for sgn, nm in ((-1, "l"), (1, "r")):
        paw = w.add(m, f"paw_{nm}", "wood", base=(196, 172, 138), smooth=60)
        w.ellipsoid(paw, (sgn * PAW_X * K, Y(PAW_Y), TORSO[3][1] * K * 0.7), 6.5 * K, 9.0 * K, 5.0 * K, sides=10, stacks=8)
    # ---- 14 the base cat: its body, 15/16 haunches, 17 chest ruff, 18/19 feet
    bb = w.add(m, "base_cat_body", "wood", base=(186, 162, 126), smooth=45, uv_mode="loft")
    ml.loft(bb, "y", (0, 0, 0), [(Y(py), hw / BASE_BODY[0][1], hw / BASE_BODY[0][1] * 0.85) for py, hw in reversed(BASE_BODY)], ml.circle(BASE_BODY[0][1] * K, 16))
    for sgn, nm in ((-1, "l"), (1, "r")):
        hn = w.add(m, f"haunch_{nm}", "wood", base=(182, 158, 124), smooth=60)
        w.ellipsoid(hn, (sgn * HAUNCH_X * K, Y(HAUNCH_C), -2.0 * K), HAUNCH_RX * K, HAUNCH_RY * K, HAUNCH_RZ * K, sides=16, stacks=12)
        ft = w.add(m, f"foot_{nm}", "cloth", base=(124, 62, 46), smooth=60)
        w.ellipsoid(ft, (sgn * 18 * K, Y(FOOT_Y), 8 * K), FOOT_RX * K, 11 * K, 12 * K, sides=12, stacks=8)
    ruff = w.add(m, "chest_ruff", "wood", base=(206, 186, 150), smooth=60)
    w.ellipsoid(ruff, (0, Y(835), 6 * K), 14 * K, 34 * K, 10 * K, sides=14, stacks=10)
    # ---- 20 the tail: a tapering tube winding up the shaft
    tail = w.add(m, "tail", "wood", base=(176, 150, 116), smooth=50, uv_mode="loft")
    pts = w.helix(lambda t: (SHAFT_R + 4.5 * K) * (1.0 - 0.15 * t) + 3 * K * (1 - t), Y(TAIL_Y[0]), Y(TAIL_Y[1]), 2.1, 56, phase=-0.6)
    ml.sweep(tail, pts, ml.circle(1.0, 8), scales=lambda t: (6.5 * K * (1.0 - 0.6 * t) if t < 0.97 else 0.0, 6.5 * K * (1.0 - 0.6 * t) if t < 0.97 else 0.0))

    atlas = ml.Atlas(1024)
    atlas.layout(m)
    atlas.paint(m)
    _paint(m, atlas)
    ref = w.reference("8f7463fbb8fed4eff33ba5882241ed13.jpg", (560, 130, 700, 960), "catstaff")
    w.finish("catstaff", m, atlas, real_m=TOTAL_M, glow=("FFFFFF", 0.12, 0.6, 1.0), ref=ref)


def _paint(m, atlas):
    def fur(c):                                                       # tabby streaks across the grain, a darker back
        n = w.noise2(c.img.shape[:2], 5, 91)
        streak = np.clip((n - 0.55) * 4.0, 0, 1)[..., None]
        c.img[..., :3] = c.img[..., :3] * (1 - 0.35 * streak) + np.array([96, 76, 58], np.float32) * (0.35 * streak)
    for nm in ("top_cat_body", "top_cat_head", "base_cat_body", "haunch_l", "haunch_r", "tail", "paw_l", "paw_r"):
        w.paint_part(atlas, m.part(nm), fur)
    for nm in ("eye_l", "eye_r"):
        w.paint_part(atlas, m.part(nm), lambda c: w.set_glow(c, (255, 220, 80), 1.0, core=0.4))
    for nm in ("grip_wrap", "wrap_cords"):
        w.paint_part(atlas, m.part(nm), lambda c: w.set_glow(c, (255, 120, 90), 0.06, core=0.0))
