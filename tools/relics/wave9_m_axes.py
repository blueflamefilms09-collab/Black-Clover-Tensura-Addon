"""Wave 9 axes as 3D models: the Axe of the Exile, Astralaria, Hunger and Thornblight (the owner's model method).

Axis of every model: y runs from the butt (0) up the haft to the head, the head's blade extends toward +x, z is through the cheek (the flat toward the viewer in hand).
"""
import math

import numpy as np

import mesh_lib as ml
import wave9_models as w9
from wave9_models import w

# ================================================================ Axe of the Exile
# 1 measure. Screenshot 1080 x 2400 (shown 900 wide, x1.2 for the file): the haft runs from the head's eye (240, 745) to the pommel's end (665, 1045) in the shown
# frame: 532 px for a handaxe of about 52 cm of haft, so 0.0977 cm per shown px.
EX_K = 52.0 / 532.0
EX_HAFT = 46.0                                    # butt cap to the eye of the head (the card's haft is 56 cm in this scale; shortened so the big head still balances)
EX_HAFT_R = (1.7, 1.45)                           # haft radius at the head and at the butt end (35 px wide in the frame, 3.4 cm)
EX_BIND = (11.0, 29.0)                            # the criss-cross leather binding: shown (470, 885) to (600, 965), 0.22 to 0.55 of the way up from the butt
EX_CAP = (-5.0, 2.0)                              # the carved, diamond-shaped butt cap, shown (640, 1015) to (690, 1060)
EX_HEAD_DROP, EX_HEAD_REACH = 19.0, 14.0         # the head hangs 19 cm below the eye and reaches 14 cm out (shown 190 px and 140 px, scaled to a handaxe)
EX_EYE_T = 3.4                                    # the head's thickness at the eye


def build_axe_of_the_exile():
    m = ml.Mesh("axe_of_the_exile")
    # ---- 1 the haft: a pale carved bone, curving a little toward the butt (shown (620, 980))
    path = w9.bez3((0.0, 0.0, 0.0), (-1.4, 14.0, 0.0), (0.9, 30.0, 0.0), (0.0, EX_HAFT, 0.0), 14)
    hf = w.add(m, "haft", "bone", base=(226, 218, 200), smooth=45, uv_mode="loft")
    ml.sweep(hf, path, ml.ellipse(1.0, 0.85, 10), scales=lambda t: (EX_HAFT_R[1] + (EX_HAFT_R[0] - EX_HAFT_R[1]) * t, (EX_HAFT_R[1] + (EX_HAFT_R[0] - EX_HAFT_R[1]) * t) * 0.9))
    # ---- 2 the carved lines down the bone: two engraved ridge pairs, the groove painted dark
    cv = w.add(m, "haft_carving", "bone", base=(170, 160, 140), wear=0.9)
    for sgn in (-1, 1):
        ml.ridge_pair(cv, (sgn * 0.55 + 0.1, 31.0, 1.35), (sgn * 0.45 - 0.2, EX_HAFT - 3.0, 1.55), (0, 0, 1), 0.4, 0.13, 0.07, 2.0)
    # ---- 3 the leather cross-binding: diagonal strips both ways over the grip, and a ring at each end
    bd = w.add(m, "binding", "leather", base=(106, 66, 42), wear=0.5)
    for i in range(8):
        y = EX_BIND[0] + 1.0 + i * (EX_BIND[1] - EX_BIND[0] - 2.0) / 8.0
        px = -1.4 * (1 - y / 14.0) if y < 14.0 else 0.9 * math.sin((y - 14.0) / 16.0 * math.pi * 0.5)
        for sgn in (-1, 1):
            ml.bar(bd, (px - 1.45, y, 1.0 * sgn * 0.9), (px + 1.45, y + 2.4, 1.0 * sgn * 0.9), 0.7, 0.25, up=(0, 0, sgn), taper=0.4)
    for y in (EX_BIND[0], EX_BIND[1]):
        ml.collar(bd, y, 1.85, 0.8, lip=0.2, sides=12)
    # ---- 4 the head: a dark leather-wrapped wedge, thick at the eye, hanging toward the butt and flaring to a long edge
    hd = w.add(m, "head", "leather", base=(96, 52, 38), smooth=40)
    top = EX_HAFT
    w9.wedge_x(hd, [(-2.0, top + 1.6, top - 3.0, EX_EYE_T), (2.0, top + 1.7, top - 6.0, EX_EYE_T), (5.0, top + 1.0, top - 10.0, 2.2),
                    (8.5, top - 0.4, top - EX_HEAD_DROP + 0.8, 1.2), (EX_HEAD_REACH, top - 2.0, top - EX_HEAD_DROP, 0.25)])
    # ---- 5 the binding straps round the eye (the card's leather), 6 the runes cut in the cheek
    ew = w.add(m, "eye_wrap", "leather", base=(76, 42, 30), wear=0.4)
    ml.loft(ew, "y", (0, 0, 0), [top - 5.4, top - 4.2], ml.octagon(4.8, EX_EYE_T + 0.7, 1.0))
    ml.loft(ew, "y", (0, 0, 0), [top - 0.3, top + 0.9], ml.octagon(4.8, EX_EYE_T + 0.7, 1.0))
    rn = w.add(m, "runes", "bronze", base=(176, 112, 64), wear=0.8)
    for i in range(5):
        a = 4.8 + i * 1.15
        ml.bar(rn, (a, top - 7.6 - i * 0.55, 1.2 - 0.14 * i), (a + 0.5, top - 7.0 - i * 0.55, 1.2 - 0.14 * i), 0.28, 0.1, up=(0, 0, 1), taper=0.15)
    # ---- 7 the butt cap: a diamond of carved bone with a notch, 8 the rings above it
    bc = w.add(m, "butt_cap", "bone", base=(210, 200, 180), smooth=55)
    ml.loft(bc, "y", (0, 0, 0), [(EX_CAP[0], 0.0, 0.0), (EX_CAP[0] + 1.6, 1.45, 1.1), (EX_CAP[0] + 3.8, 2.0, 1.5), (EX_CAP[1] - 1.0, 1.55, 1.2), (EX_CAP[1], 1.35, 1.1)], ml.octagon(1.0, 1.0, 0.25))
    # ---- 9 the feathers hanging from the pommel, 10 the claws and teeth, 11 the cords
    fe = w.add(m, "feathers", "leather", base=(112, 70, 44), wear=0.3, smooth=50)
    for ang, ln in ((-35, 10.5), (-15, 12.5), (5, 9.5)):
        a = math.radians(ang)
        w9.leaf(fe, (0.4, EX_CAP[0] + 1.5, 0.3), (math.sin(a) + 0.35, -math.cos(a) * 0.8, 0.08), ln, 2.0, 0.22, up=(0, 0, 1))
    cl = w.add(m, "claws", "bone", base=(236, 230, 216), smooth=60)
    w9.spike(cl, (1.2, EX_CAP[0] + 2.4, 0.9), (0.55, -0.7, 0.3), 3.6, 0.5, sides=6)
    w9.spike(cl, (-0.9, EX_CAP[0] + 2.8, -0.9), (-0.4, -0.8, -0.3), 3.0, 0.45, sides=6)
    w9.spike(cl, (0.6, EX_CAP[0] + 3.4, -1.0), (0.7, -0.5, -0.3), 2.6, 0.4, sides=6)
    cd = w.add(m, "cords", "leather", base=(120, 78, 50), wear=0.2)
    for p1, p2 in (((2.5, -3.0, 0.8), (4.5, -8.0, 1.6)), ((-1.5, -3.5, -0.6), (-3.5, -8.0, -1.2)), ((0.5, -3.0, 0.2), (1.0, -9.5, 0.0))):
        ml.sweep(cd, w9.bez3((0.2, EX_CAP[0] + 2.0, 0.0), (p1[0], p1[1], p1[2]), (p2[0] * 0.9, p2[1] + 1.0, p2[2]), p2, 8), ml.circle(0.16, 5), scales=lambda t: (1.0, 1.0))

    atlas = ml.Atlas(w9.ATLAS_PX)
    atlas.layout(m)
    atlas.paint(m)

    def leather_head(c):                                            # dark red-brown leather, a lighter wear on the edge, the runes' glow
        edge = c.A > 0.0 if c.kind != "loft" else (np.abs(c.S - 0.5) > 0.4)
        n = w.noise2(c.img.shape[:2], 6, 5 + c.g.rect[0])
        c.img[..., :3] = c.img[..., :3] * (0.85 + 0.3 * n[..., None])
    w.paint_part(atlas, m.part("head"), leather_head)
    w.paint_part(atlas, m.part("runes"), lambda c: w.set_glow(c, (255, 150, 70), 0.8, core=0.4))
    w.paint_part(atlas, m.part("haft_carving"), lambda c: w.set_glow(c, (255, 190, 120), 0.2, core=0.1))
    r = w9.ref("p2/Relics of the world part 2/Screenshot_20261013_222936_Opera.jpg", (160, 650, 960, 1380), "axe_of_the_exile")
    w9.finish("axe_of_the_exile", m, atlas, real_m=0.62, glow=("FFFFFF", 0.10, 0.5, 1.0), ref_png=r)


# ================================================================ Hunger
# 1 measure. Screenshot 1080 x 2400, the crop (60, 500) to (600, 1060) of the shown frame x1.2 x0.9: the greataxe runs from its butt spike (60, 575) to the crown
# horn (220, 28); the haft from the head's collar (290, 235) to the ferrule (150, 410). 498 px from the head's middle (350, 170) to the butt spike: a greataxe of 145 cm.
HU_K = 145.0 / 498.0                              # cm per crop px along the haft (0.29)
HU_HEAD_K = 0.17                                  # the head is drawn very large; its widths are scaled by this (cm per crop px) so that it stays a weapon one can swing
HU_BUTT = (0.0, 12.0)                             # the pointed butt spike, crop (60, 575) to (100, 520)
HU_FERRULE = (8.0, 38.0)                          # the grey bound sleeve with three rings, crop (120, 400) to (185, 520): the lower third of the haft
HU_HAFT = (36.0, 106.0)                           # the brown wooden haft, a little crooked, crop (150, 410) to (285, 235)
HU_SOCKET = (100.0, 128.0)                        # the head's rune-carved socket block, crop (300, 90) to (440, 235)
HU_SOCKET_W, HU_SOCKET_T = 11.0, 6.0
HU_BLADE_X = (4.0, 34.0)                          # the big upper blade reaches 30 cm out (crop 440 to 525: 85 px at the head scale: shortened)
HU_HOOK = 26.0                                    # the lower hook below the mouth reaches this far


def build_hunger():
    m = ml.Mesh("hunger")
    # ---- 1 the haft: brown wood with a slight crook
    hf = w.add(m, "haft", "wood", base=(112, 74, 48), smooth=45, uv_mode="loft")
    ml.sweep(hf, w9.bez3((0.0, HU_HAFT[0] - 2, 0.0), (-1.6, 62.0, 0.0), (1.2, 86.0, 0.0), (0.0, HU_HAFT[1], 0.0)), ml.circle(1.0, 10), scales=lambda t: (2.0 - 0.3 * t, 2.0 - 0.3 * t))
    # ---- 2 the grey bound sleeve at the foot of the haft
    fe = w.add(m, "ferrule", "iron", base=(124, 128, 140), smooth=45, uv_mode="loft")
    ml.loft(fe, "y", (0, 0, 0), [(HU_FERRULE[0], 1.4, 1.4), (HU_FERRULE[0] + 6, 5.2, 5.2), (HU_FERRULE[1] - 8, 4.9, 4.9), (HU_FERRULE[1], 4.2, 4.2)], ml.octagon(1.0, 1.0, 0.18))
    # ---- 3 the three bands round it
    rg = w.add(m, "ferrule_rings", "silver", base=(196, 200, 212), wear=0.8)
    for y in (HU_FERRULE[0] + 9.0, HU_FERRULE[0] + 17.0, HU_FERRULE[1] - 4.0):
        ml.collar(rg, y, 2.75, 1.4, lip=0.35, sides=12)
    # ---- 4 the butt spike: a pale blade-like point
    bs = w.add(m, "butt_spike", "bone", base=(204, 206, 214), smooth=40)
    ml.loft(bs, "y", (0, 0, 0), [(HU_BUTT[0], 0.0, 0.0), (HU_BUTT[0] + 3.0, 0.9, 0.5), (HU_BUTT[1], 2.1, 1.1), (HU_FERRULE[0] + 1.0, 1.4, 1.0)], ml.octagon(1.0, 1.0, 0.3))
    # ---- 5 the socket: a rune-carved block, wider at the middle
    so = w.add(m, "socket", "stone", base=(196, 198, 206), smooth=40, uv_mode="loft")
    ml.loft(so, "y", (0, 0, 0), [(HU_SOCKET[0], 0.75, 0.8), (HU_SOCKET[0] + 7, 1.0, 1.0), (HU_SOCKET[1] - 7, 1.05, 1.05), (HU_SOCKET[1], 0.8, 0.85)], ml.octagon(HU_SOCKET_W, HU_SOCKET_T, 1.6))
    # ---- 6 the carved runes: knotted bars raised on both cheeks, painted dark in the grooves and glowing faintly red
    rn = w.add(m, "runes", "stone", base=(120, 120, 132), wear=0.9)
    for sgn in (1, -1):
        z = sgn * HU_SOCKET_T * 0.5
        for i in range(4):
            y = HU_SOCKET[0] + 4.0 + i * 5.2
            ml.bar(rn, (-3.6, y, z), (3.6, y + 1.2, z), 0.5, 0.2, up=(0, 0, sgn), taper=0.5)
            ml.bar(rn, (-2.8, y, z), (-1.0, y + 4.4, z), 0.45, 0.2, up=(0, 0, sgn), taper=0.5)
            ml.bar(rn, (2.8, y, z), (1.0, y + 4.4, z), 0.45, 0.2, up=(0, 0, sgn), taper=0.5)
    # ---- 7 the big upper blade: a broad pale plate, thick at the socket and ground thin along its curved edge
    ub = w.add(m, "blade_upper", "bone", base=(212, 214, 222), smooth=40)
    y0 = HU_SOCKET[0] + 8.0
    prof = []
    for i in range(10):                                              # an elliptical outline: the edge bows out and the whole plate climbs a little toward the tip (the art's crescent)
        t = i / 9.0
        x = HU_BLADE_X[0] + (HU_BLADE_X[1] - HU_BLADE_X[0]) * t
        half = 2.0 + 11.5 * math.sqrt(max(0.0, 1.0 - t * t))
        centre = y0 + 9.0 + 7.0 * t
        prof.append((x, centre + half, centre - half, 3.0 - 2.78 * t ** 1.5))
    w9.wedge_x(ub, prof)
    # ---- 8 the lower hook: a sweeping crescent below the mouth, a long point curling back toward the haft
    lh = w.add(m, "blade_hook", "bone", base=(200, 202, 212), smooth=40)
    hp = w9.bez3((4.0, HU_SOCKET[0] + 6.0, 0.0), (14.0, HU_SOCKET[0] - 8.0, 0.0), (24.0, HU_SOCKET[0] - 12.0, 0.0), (HU_BLADE_X[1] - 2.0, HU_SOCKET[0] - 28.0, 0.0), 12)
    ml.sweep(lh, hp, ml.octagon(1.0, 1.0, 0.25), scales=lambda t: (13.0 * (1 - t) ** 0.8 + 0.3, 2.2 * (1 - 0.88 * t)))
    # ---- 9 the teeth round the mouth: short fangs on the upper blade's lower rim and on the hook's inner rim
    te = w.add(m, "teeth", "bone", base=(236, 236, 240), smooth=60)
    for x in (10.0, 16.0, 22.0):
        w9.spike(te, (x, y0 - 4.0 - 0.18 * x, 0.0), (0.2, -1.0, 0.0), 3.4, 0.7, sides=5, belly=0.7)
    for k, t in enumerate((0.25, 0.42, 0.58)):
        px, py, _pz = hp[int(len(hp) * t)]
        w9.spike(te, (px, py + 4.0, 0.0), (0.0, 1.0, 0.0), 3.0, 0.65, sides=5, belly=0.7)
    # ---- 10 the crown: a long horn sweeping up and back on the left, 11 a short horn on the right
    ch = w.add(m, "crown_horn_l", "bone", base=(206, 208, 218), smooth=45)
    cp = w9.bez3((-2.0, HU_SOCKET[1] - 3.0, 0.0), (-8.0, HU_SOCKET[1] + 8.0, 0.0), (-20.0, HU_SOCKET[1] + 14.0, 0.0), (-34.0, HU_SOCKET[1] + 26.0, 0.0), 12)
    ml.sweep(ch, cp, ml.octagon(1.0, 1.0, 0.25), scales=lambda t: (9.0 * (1 - t) ** 0.9 + 0.3, 2.4 * (1 - 0.85 * t)))
    cr = w.add(m, "crown_horn_r", "bone", base=(218, 220, 228), smooth=45)
    cp2 = w9.bez3((3.0, HU_SOCKET[1] - 3.0, 0.0), (6.0, HU_SOCKET[1] + 4.0, 0.0), (9.0, HU_SOCKET[1] + 12.0, 0.0), (6.0, HU_SOCKET[1] + 22.0, 0.0), 10)
    ml.sweep(cr, cp2, ml.octagon(1.0, 1.0, 0.25), scales=lambda t: (4.6 * (1 - t) ** 0.8 + 0.25, 2.0 * (1 - 0.8 * t)))
    # ---- 12 the left cheek spike: a smaller fin on the back of the head
    bk = w.add(m, "beard", "bone", base=(196, 198, 208), smooth=45)
    w9.wedge_x(bk, [(-2.0, HU_SOCKET[0] + 18, HU_SOCKET[0] + 6, 2.4), (-9.0, HU_SOCKET[0] + 14, HU_SOCKET[0] - 2, 1.4), (-16.0, HU_SOCKET[0] + 6, HU_SOCKET[0] - 10, 0.25)])

    atlas = ml.Atlas(w9.ATLAS_PX)
    atlas.layout(m)
    atlas.paint(m)

    def bone_cracks(c):                                             # cold grey bone with dark cracks and blotches, as the art's ink wash
        n = w.noise2(c.img.shape[:2], 14, 3 + c.g.rect[0])
        crack = np.clip(1.0 - np.abs(n - 0.5) / 0.03, 0, 1)[..., None]
        c.img[..., :3] = c.img[..., :3] * (1 - 0.5 * crack) + np.array([60, 60, 74], np.float32) * 0.5 * crack
        edge = (np.abs(c.S - 0.5) > 0.42) if c.kind == "loft" else np.zeros(c.img.shape[:2], bool)
        w.set_glow(c, (210, 60, 70), 0.18, mask=np.maximum(edge, crack[..., 0] > 0.6), core=0.1)
    for nm in ("blade_upper", "blade_hook", "socket", "crown_horn_l", "crown_horn_r", "beard"):
        w.paint_part(atlas, m.part(nm), bone_cracks)
    w.paint_part(atlas, m.part("runes"), lambda c: w.set_glow(c, (220, 50, 60), 0.8, core=0.3))
    w.paint_part(atlas, m.part("teeth"), lambda c: w.set_glow(c, (230, 90, 90), 0.25, core=0.1))
    r = w9.ref("p2/Relics of the world part 2/Screenshot_20261013_222954_Opera.jpg", (72, 600, 720, 1272), "hunger")
    w9.finish("hunger", m, atlas, real_m=1.45, glow=("FFFFFF", 0.09, 0.45, 1.0), ref_png=r)


# ================================================================ Astralaria
# 1 measure. Screenshot 1080 x 2400, crop (20, 560) to (420, 1100) of the shown frame x1.2: the night-sky crescent fills the top right (its top at (350, 45)), the brass lens ring
# sits at (245, 205), the long brass beak runs down the shaft from it, the shaft goes down to the brass gear-fan at (225, 570). 540 px from the crescent's top to the gear.
AS_TOTAL_CM = 130.0                               # a battleaxe
AS_K = AS_TOTAL_CM / 540.0                        # cm per crop px (0.24)
AS_GEAR_Y = 8.0                                   # the shaft's lower end; the gear fan hangs below it
AS_SHAFT = (8.0, 100.0)                           # the dark shaft, filled with the night sky
AS_SHAFT_R = 1.55                                 # 14 px wide in the crop
AS_LENS = (-2.6, 108.0)                           # the lens ring's centre (x, y); card (245, 205): 90 px across, ring 4.6 cm radius at this scale
AS_LENS_R = 4.6
AS_D_X0, AS_D_R, AS_D_YC, AS_D_H = 2.4, 17.0, 112.0, 17.0   # the crescent: a D with its straight side at the shaft, 17 cm out, 34 cm tall (card 150 x 300 px, scaled down)
AS_GEAR_R = 8.5                                   # the gear fan's radius, 40 px in the crop at this scale


def build_astralaria():
    m = ml.Mesh("astralaria")
    # ---- 1 the shaft: long and dark, a very slight swell where it is held
    sh = w.add(m, "shaft", "iron", base=(30, 34, 78), smooth=45, uv_mode="loft")
    ml.loft(sh, "y", (0, 0, 0), [(AS_SHAFT[0], AS_SHAFT_R * 0.95, AS_SHAFT_R * 0.95), (40.0, AS_SHAFT_R * 1.08, AS_SHAFT_R * 1.08), (70.0, AS_SHAFT_R, AS_SHAFT_R), (AS_SHAFT[1], AS_SHAFT_R * 0.9, AS_SHAFT_R * 0.9)],
            ml.circle(1.0, 12))
    # ---- 2 the brass rings: one at the foot, two for the grip, one under the head
    rg = w.add(m, "shaft_rings", "bronze", base=(198, 148, 66), wear=0.7)
    for y, h in ((AS_SHAFT[0] + 1.0, 2.0), (26.0, 1.2), (44.0, 1.2), (AS_SHAFT[1] - 5.0, 2.4)):
        ml.collar(rg, y, AS_SHAFT_R + 0.55, h, lip=0.3, sides=12)
    # ---- 3 the head housing: a brass block that the shaft enters, where the lens and the beak join
    hh = w.add(m, "head_housing", "bronze", base=(190, 140, 62), smooth=45)
    ml.loft(hh, "y", (0, 0, 0), [(AS_SHAFT[1] - 4.0, 0.9, 0.9), (AS_SHAFT[1] + 1.0, 1.25, 1.25), (AS_SHAFT[1] + 6.0, 1.15, 1.15), (AS_SHAFT[1] + 9.0, 0.8, 0.8)], ml.octagon(4.0, 3.4, 0.9))
    # ---- 4 the lens ring: a thick brass ring round the lens, 5 the lens itself (a glass disc of the night sky)
    lr = w.add(m, "lens_ring", "bronze", base=(210, 160, 76), smooth=55)
    ml.sweep(lr, w9.arc_points(AS_LENS[0], AS_LENS[1], AS_LENS_R, 0, 352, 28), ml.circle(0.95, 8), scales=lambda t: (1.0, 1.0))
    lg = w.add(m, "lens_glass", "crystal", base=(40, 36, 120), smooth=60)
    ml.lathe(lg, "z", (AS_LENS[0], AS_LENS[1], 0.0), [(-0.35, 0.0), (-0.35, AS_LENS_R - 0.7), (0.35, AS_LENS_R - 0.7), (0.35, 0.0)], sides=20)
    # ---- 6 the beak: a long brass sliver running down the shaft from the lens
    bk = w.add(m, "beak", "bronze", base=(214, 164, 80), smooth=50, uv_mode="loft")
    ml.sweep(bk, w9.bez3((-1.6, AS_LENS[1] - AS_LENS_R + 0.5, 0.0), (2.6, 98.0, 0.0), (3.4, 90.0, 0.0), (2.2, 80.0, 0.0), 12), ml.octagon(1.0, 1.0, 0.25), scales=lambda t: (2.8 * (1 - t) + 0.2, 1.0 * (1 - 0.7 * t)))
    # ---- 7 the crescent: a D of night sky, its straight side on the shaft, thick near the shaft and thin to the curved edge
    cr = w.add(m, "crescent", "obsidian", base=(40, 36, 110), smooth=40)
    prof = []
    for i in range(12):
        phi = (math.pi / 2) * i / 11.0 * 0.99
        half = AS_D_H * math.cos(phi)
        prof.append((AS_D_X0 + AS_D_R * math.sin(phi), AS_D_YC + half, AS_D_YC - half, 1.8 * (1 - 0.85 * math.sin(phi))))
    w9.wedge_x(cr, prof)
    # ---- 8 the brass rim along the crescent's curved edge
    cm_ = w.add(m, "crescent_rim", "bronze", base=(200, 150, 70), smooth=60)
    rim_pts = [(AS_D_X0 + AS_D_R * math.cos(math.radians(a)) * 0.985, AS_D_YC + AS_D_H * math.sin(math.radians(a)) * 0.985, 0.0) for a in np.linspace(-88, 88, 22)]
    ml.sweep(cm_, rim_pts, ml.circle(0.42, 6), scales=lambda t: (1.0, 1.0))
    # ---- 9 the crest fin: a scalloped brass fin curving up and back from the housing (the art's top-left), with its teeth
    cf = w.add(m, "crest_fin", "bronze", base=(196, 146, 64), smooth=45, uv_mode="loft")
    ml.sweep(cf, w9.bez3((-1.0, AS_SHAFT[1] + 6.0, 0.0), (-6.0, 112.0, 0.0), (-13.0, 118.0, 0.0), (-19.0, 112.0, 0.0), 12), ml.octagon(1.0, 1.0, 0.25), scales=lambda t: (4.2 * (1 - 0.8 * t) + 0.3, 0.9 * (1 - 0.6 * t)))
    for i, t in enumerate((0.3, 0.5, 0.7, 0.88)):
        px, py, _pz = w9.bez3((-1.0, AS_SHAFT[1] + 6.0, 0.0), (-6.0, 112.0, 0.0), (-13.0, 118.0, 0.0), (-19.0, 112.0, 0.0), 12)[int(12 * t)]
        w9.spike(cf, (px, py + 1.2, 0.0), (-0.2, 1.0, 0.0), 2.2, 0.5, sides=5, belly=0.7)
    # ---- 10 the gear fan: a flat sector with three round windows, 11 its cogs along the arc
    gf = w.add(m, "gear_fan", "bronze", base=(204, 154, 70), smooth=50)
    sector = [(0.0, 0.0)] + [(AS_GEAR_R * 0.93 * math.cos(math.radians(a)), AS_GEAR_R * 0.93 * math.sin(math.radians(a))) for a in np.linspace(210, 330, 13)]
    ml.loft(gf, "z", (0, AS_GEAR_Y, 0), [-0.65, 0.65], sector)
    gc = w.add(m, "gear_cogs", "bronze", base=(220, 172, 88), smooth=60)
    for a in np.linspace(214, 326, 9):
        r = math.radians(a)
        ml.loft(gc, (math.cos(r), math.sin(r), 0.0), (AS_GEAR_R * 0.88 * math.cos(r), AS_GEAR_Y + AS_GEAR_R * 0.88 * math.sin(r), 0.0), [0.0, 0.8, 1.5], ml.octagon(1.1, 1.2, 0.3), up=(0, 0, 1))
    # ---- 12 the finial above the lens: a brass spike
    fi = w.add(m, "finial", "bronze", base=(216, 166, 82), smooth=60)
    w9.spike(fi, (0.0, AS_SHAFT[1] + 8.0, 0.0), (0.0, 1.0, 0.0), 12.0, 0.9, sides=8, belly=0.5)

    atlas = ml.Atlas(w9.ATLAS_PX)
    atlas.layout(m)
    atlas.paint(m)

    def night(c):                                                   # deep blue-violet nebula with stars; the stars and the cloud glow
        n = w.noise2(c.img.shape[:2], 12, 4 + c.g.rect[0])
        n2 = w.noise2(c.img.shape[:2], 5, 9 + c.g.rect[0])
        deep, mid, hot = np.array([16, 16, 64], np.float32), np.array([70, 52, 170], np.float32), np.array([150, 100, 230], np.float32)
        t = np.clip(n * 1.3 - 0.15, 0, 1)[..., None]
        col = deep * (1 - t) + mid * t
        col = col + (hot - col) * np.clip((n2 - 0.62) * 3.0, 0, 1)[..., None] * 0.7
        c.img[..., :3] = col
        c.img[..., 3] = 255
        w.set_glow(c, (120, 90, 230), 0.35, mask=np.clip((n - 0.45) * 2.0, 0, 1), core=0.1)
        w9.stars(c.img, c.glow, 31 + c.g.rect[0], max(8, c.img.shape[0] * c.img.shape[1] // 160), glow_rgb=(210, 210, 255), p_glow=0.6)
    for nm in ("crescent", "shaft"):
        w.paint_part(atlas, m.part(nm), night)
    w.paint_part(atlas, m.part("lens_glass"), lambda c: w.set_glow(c, (140, 110, 255), 0.9, core=0.5))
    w.paint_part(atlas, m.part("crescent_rim"), lambda c: w.set_glow(c, (170, 130, 255), 0.35, core=0.2))
    r = w9.ref("p2/Relics of the world part 2/Screenshot_20261013_223308_Opera.jpg", (24, 672, 504, 1320), "astralaria")
    w9.finish("astralaria", m, atlas, real_m=1.3, glow=("FFFFFF", 0.12, 0.5, 1.0), ref_png=r)


# ================================================================ Thornblight
# 1 measure. Screenshot 1080 x 2400, crop (150, 700) to (800, 1180) of the shown frame x1.2 x0.9: the butt's point (640, 470) to the middle of the head (130, 150): about 600 px
# for a handaxe of 55 cm over all. The head (35 to 245 px across, 60 to 265 tall) is a ragged black leaf-plate that hangs to the left of the haft's top; the haft is a twisted
# vine, thick and glowing green with thorns from (430, 270) to (600, 380), ending in a bark shard.
TH_K = 55.0 / 600.0
TH_BUTT = (0.0, 11.0)                             # the bark shard, crop (560, 390) to (640, 470): 80 px
TH_GLOW = (11.0, 30.0)                            # the swollen green section with thorns, crop (440, 270) to (600, 380)
TH_VINE = (30.0, 52.0)                            # the twisted two-strand vine up to the head, crop (230, 130) to (430, 270)
TH_HEAD_Y = 52.0                                  # the head's eye on the haft
TH_HEAD = [(0.0, 9.0, -9.0, 3.0), (-3.0, 10.0, -11.0, 2.6), (-8.0, 11.0, -13.0, 1.8), (-13.0, 9.0, -15.0, 1.0), (-16.0, 5.0, -16.0, 0.3)]   # x, top, bottom (relative to the eye), thickness: the card's plate, 210 px across (19 cm), scaled to 16
TH_THORNS = 13


def _vine_path(phase, r0, r1, y0, y1, turns, n=22, x_of=None):
    pts = []
    for i in range(n + 1):
        t = i / n
        a = phase + 2 * math.pi * turns * t
        r = r0 + (r1 - r0) * t
        y = y0 + (y1 - y0) * t
        cx = x_of(y) if x_of else 0.0
        pts.append((cx + r * math.cos(a), y, r * math.sin(a)))
    return pts


def build_thornblight():
    m = ml.Mesh("thornblight")
    bend = lambda y: 2.2 * math.sin((y - 11.0) / 41.0 * math.pi) * (1.0 if y > 11.0 else 0.0)      # the haft's S-curve toward the head
    # ---- 1 the core: a dark thin haft through the middle of the vine
    co = w.add(m, "core", "wood", base=(66, 48, 36), smooth=45, uv_mode="loft")
    ml.sweep(co, [(bend(y), y, 0.0) for y in np.linspace(TH_BUTT[1] - 2.0, TH_HEAD_Y, 14)], ml.circle(0.85, 8), scales=lambda t: (1.0, 1.0))
    # ---- 2/3 the two vine strands twisting round it
    for nm, ph, base in (("vine_a", 0.0, (112, 80, 52)), ("vine_b", math.pi, (92, 64, 44))):
        v = w.add(m, nm, "wood", base=base, smooth=50, uv_mode="loft")
        ml.sweep(v, _vine_path(ph, 1.15, 1.0, TH_VINE[0] - 2.0, TH_HEAD_Y + 0.5, 3.2, 26, bend), ml.circle(0.78, 7), scales=lambda t: (1.0, 1.0 - 0.15 * t))
    # ---- 4 the swollen green section: a knotted green-glowing bulb of the vine
    gs = w.add(m, "glow_section", "crystal", base=(48, 190, 124), smooth=50)
    ml.lathe(gs, "y", (0, 0, 0), [(TH_GLOW[0] - 1.0, 1.2), (TH_GLOW[0] + 2.0, 2.0), (TH_GLOW[0] + 7.0, 2.5), (TH_GLOW[1] - 4.0, 2.3), (TH_GLOW[1], 1.5), (TH_GLOW[1] + 2.0, 1.2)], sides=12)
    # ---- 5 the thorns on it, spiralling, leaning toward the head
    th = w.add(m, "thorns", "crystal", base=(90, 235, 160), smooth=60)
    for i in range(TH_THORNS):
        y = TH_GLOW[0] + 1.5 + (TH_GLOW[1] - TH_GLOW[0] - 3.0) * i / (TH_THORNS - 1)
        a = i * 2.4
        r = 2.1
        w9.spike(th, (r * math.cos(a) * 0.8, y, r * math.sin(a) * 0.8), (math.cos(a), 0.55, math.sin(a)), 3.0 - 0.8 * (i % 3) * 0.4, 0.46, sides=5, belly=0.7)
    # ---- 6 the long thorns on the vine below the head (the card's three, one pointing out to the right)
    bt = w.add(m, "thorns_big", "crystal", base=(60, 215, 140), smooth=60)
    for y, d in ((36.0, (1.0, 0.7, 0.2)), (42.0, (-0.5, 0.8, -0.8)), (47.0, (0.9, 0.8, -0.3))):
        w9.spike(bt, (bend(y) + 1.0 * d[0] * 0.6, y, 0.6 * d[2]), d, 5.2, 0.55, sides=5, belly=0.6)
    # ---- 7 the bark shard at the butt, with its grooves
    bb = w.add(m, "butt_bark", "wood", base=(82, 62, 48), smooth=40, uv_mode="loft")
    ml.sweep(bb, [(0.0, 0.0, 0.0), (0.4, 4.0, 0.0), (0.9, 8.0, 0.0), (0.4, TH_BUTT[1] + 0.5, 0.0)], ml.octagon(1.0, 1.0, 0.25), scales=lambda t: (0.5 + 4.0 * min(1.0, t * 1.6) * (1 - 0.1 * t), 0.5 + 2.8 * min(1.0, t * 1.6)))
    gr = w.add(m, "butt_grooves", "wood", base=(46, 34, 28), wear=0.9)
    for z in (-0.9, 0.0, 0.9):
        ml.bar(gr, (0.3, 2.0, z * 1.1 + (0.0 if z else 1.3)), (0.5, 9.5, z * 1.1 + (0.0 if z else 1.3)), 0.3, 0.18, up=(0, 0, 1), taper=1.4)
    # ---- 8 the head: a dark ragged plate, thick at the eye, hanging to the left of the vine
    hd = w.add(m, "head", "iron", base=(30, 32, 40), smooth=40)
    w9.wedge_x(hd, [(x, TH_HEAD_Y + yt, TH_HEAD_Y + yb, t) for (x, yt, yb, t) in TH_HEAD])
    # ---- 9 the leaf ribs raised on both faces, radiating from the eye
    rb = w.add(m, "head_ribs", "iron", base=(58, 62, 74), wear=0.8)
    for sgn in (1, -1):
        for ang in (-38, -14, 10):
            a = math.radians(ang)
            x1, y1 = -13.0 * math.cos(a), TH_HEAD_Y - 2.0 + 13.0 * math.sin(a) * -1.0 * 1.0
            ml.bar(rb, (-1.2, TH_HEAD_Y - 1.0, sgn * 1.4), (x1, y1 - 1.0, sgn * 0.62), 0.5, 0.2, up=(0, 0, sgn), taper=2.0)
    # ---- 10 the jagged teeth along the far edge, pointing out and down
    et = w.add(m, "edge_teeth", "iron", base=(44, 48, 58), smooth=60)
    for i, yy in enumerate(np.linspace(TH_HEAD[-1][1] - 0.5, TH_HEAD[-1][2] + 1.0, 6)):
        w9.spike(et, (-16.0, TH_HEAD_Y + yy, 0.0), (-0.9, -0.45, 0.0), 3.6 - 0.25 * i, 1.1, sides=4, belly=0.8)
    # ---- 11 the vines that wrap the head: two thorned strands crossing the plate
    hv = w.add(m, "head_vines", "wood", base=(104, 74, 50), smooth=50, uv_mode="loft")
    for z, pts in ((1.9, [(bend(TH_HEAD_Y) + 0.5, TH_HEAD_Y + 9.0, 1.9), (-5.0, TH_HEAD_Y + 4.0, 1.9), (-9.0, TH_HEAD_Y - 3.0, 1.1), (-13.0, TH_HEAD_Y - 8.0, 0.8)]),
                   (-1.9, [(bend(TH_HEAD_Y) + 0.5, TH_HEAD_Y + 5.0, -1.9), (-6.0, TH_HEAD_Y - 3.0, -1.9), (-10.0, TH_HEAD_Y - 9.0, -1.1), (-14.0, TH_HEAD_Y - 13.0, -0.8)])):
        ml.sweep(hv, w9.bez3(pts[0], pts[1], pts[2], pts[3], 10), ml.circle(0.5, 6), scales=lambda t: (1.0 - 0.4 * t, 1.0 - 0.4 * t))
    sp = w.add(m, "head_thorns", "crystal", base=(80, 230, 150), smooth=60)
    w9.spike(sp, (-4.6, TH_HEAD_Y + 4.0, 2.1), (-0.3, 1.0, 0.5), 3.4, 0.5, sides=5)
    w9.spike(sp, (-8.4, TH_HEAD_Y - 2.5, 1.3), (-0.8, 0.5, 0.6), 3.0, 0.45, sides=5)
    w9.spike(sp, (-5.0, TH_HEAD_Y - 3.0, -2.1), (0.0, -0.4, -1.0), 3.0, 0.45, sides=5)

    atlas = ml.Atlas(w9.ATLAS_PX)
    atlas.layout(m)
    atlas.paint(m)
    for nm, k in (("glow_section", 0.7), ("thorns", 1.0), ("thorns_big", 1.0), ("head_thorns", 1.0)):
        w.paint_part(atlas, m.part(nm), lambda c, k=k: w.set_glow(c, (60, 235, 150), k, core=0.45))
    w.paint_part(atlas, m.part("edge_teeth"), lambda c: w.set_glow(c, (60, 230, 150), 0.9, core=0.3))

    def plate(c):                                                   # dark slate with leaf veins, a green sheen on the rim (the card's glow round the blade)
        n = w.noise2(c.img.shape[:2], 9, 3 + c.g.rect[0])
        c.img[..., :3] = c.img[..., :3] * (0.9 + 0.25 * n[..., None])
        w.set_glow(c, (50, 220, 140), 0.12, core=0.1)
    w.paint_part(atlas, m.part("head"), plate)
    w.paint_part(atlas, m.part("head_ribs"), lambda c: w.set_glow(c, (60, 230, 150), 0.55, core=0.3))
    w.paint_part(atlas, m.part("vine_a"), lambda c: w.set_glow(c, (50, 200, 120), 0.12, core=0.1))
    r = w9.ref("p2/Relics of the world part 2/Screenshot_20261013_223323_Opera.jpg", (160, 840, 940, 1440), "thornblight")
    w9.finish("thornblight", m, atlas, real_m=0.58, glow=("FFFFFF", 0.12, 0.5, 1.0), ref_png=r)
