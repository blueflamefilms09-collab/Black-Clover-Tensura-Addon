"""The Tower Wizard, built from the owner's design (docs/attributes/art_reference/tower_wizard_design.png):
an old wizard in a very wide-brimmed, tall, floppy-tipped brown hat, a long grey-white beard and bushy brows over tired kind eyes, a brown-mauve
robe with wide sleeves and a belt, holding a glowing green orb at chest / mouth height.

    python3 -B tools/gen_tower_wizard.py

Writes (through tools/geo_builder.py, drawn by client/wizard/TowerWizardRenderer.java with client.geo.GeoDraw):
    assets/nusmp/geo/entity/tower_wizard.geo.json
    assets/nusmp/animations/entity/tower_wizard.animation.json     clips: idle (breathing, beard + hat sway, orb pulse and bob), talk (nods, a hand gesture)
    assets/nusmp/textures/entity/tower_wizard.png                  256x128 painted procedurally (cloth weave, stitching, beard strands, hat band)
    assets/nusmp/textures/entity/tower_wizard_glow.png             the green orb, the motes and the faint glint in the eyes (additive, full bright)

Every cube has whole-number sizes so the painted texels stay square. Preview: python3 -B tools/render_preview/preview.py tower_wizard_idle
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_builder import Model, Anim  # noqa: E402
from spirit_geo import Sheet, nz, fz, hsh, rgba, mix, shade  # noqa: E402

S = math.sin
TAU = 2 * math.pi
TEX_W, TEX_H = 256, 128

# ------------------------------------------------------------------------------------------------------------- palette
ROBE, ROBE_D, ROBE_L = (121, 86, 94), (84, 58, 68), (156, 118, 122)
INNER, INNER_D = (98, 70, 84), (66, 46, 60)
TRIM, TRIM_D = (178, 146, 112), (128, 100, 76)
HAT, HAT_D, HAT_L = (122, 90, 68), (78, 56, 46), (156, 120, 90)
LEATHER, LEATHER_D = (74, 52, 42), (46, 32, 28)
GOLD, GOLD_D = (206, 172, 84), (136, 108, 48)
SKIN, SKIN_D, SKIN_L = (222, 184, 158), (186, 142, 122), (238, 204, 178)
WHITE, LGREY, GREY, DGREY = (236, 236, 238), (206, 206, 214), (166, 166, 178), (124, 124, 138)
ORB, ORB_L = (96, 236, 124), (214, 255, 214)


def mx(a, b, t):
    return np.array(mix(a, b, t), np.float32)


# ------------------------------------------------------------------------------------------------------------- painters
def cloth(base, dark, light, salt=0, folds=0, stitch=None, seam_top=True, hem=None, hem_rows=2):
    """Woven cloth: a basket weave, thread noise, vertical folds, a dark seam on the first row and a dashed stitch line under it,
    optionally a trim band along the lower rows."""
    base, dark, light = np.array(base, np.float32), np.array(dark, np.float32), np.array(light, np.float32)

    def p(face, i, j, fw, fh):
        weave = (3.0 if ((i // 2 + j // 2) % 2) else -3.0) + (2.0 if (i + j) % 2 == 0 else -2.0)
        c = base + weave + fz(i, j, salt) * 14 + nz(i * 0.5, j * 0.5, salt) * 10
        if face == "top":
            return rgba(c * 1.08), None
        if face == "bottom":
            return rgba(c * 0.62), None
        if folds:
            f = 0.5 + 0.5 * S(i * TAU * folds / max(4, fw) + salt * 1.7 + 0.35 * S(j * 0.5))
            c = c * (0.82 + 0.26 * f) + (light - base) * 0.35 * max(0.0, f - 0.8) * 4
        c = c * (1.0 - 0.1 * (j / max(1, fh - 1)))                        # a little darker towards the bottom
        if seam_top and j == 0:
            c = dark
        elif stitch is not None and j == 1 and i % 2 == 0:
            c = np.array(stitch, np.float32)
        if hem is not None and j >= fh - hem_rows:
            k = j - (fh - hem_rows)
            hc = np.array(hem, np.float32) + fz(i, j, salt + 4) * 10 + (3 if (i + k) % 2 == 0 else -3)
            if k == 0 and i % 3 == 0:
                hc = hc * 0.78                                            # a dashed line of embroidery
            c = hc * (0.76 if k == hem_rows - 1 else 1.0)
        return rgba(c), None
    return p


def robe_front(base, dark, light, salt, hem=None, hem_rows=2, open_col=True):
    """A skirt layer: the front has a dark inner opening down the middle with piped edges."""
    inner = cloth(base, dark, light, salt, folds=3, stitch=(170, 138, 120), hem=hem, hem_rows=hem_rows)

    def p(face, i, j, fw, fh):
        c, g = inner(face, i, j, fw, fh)
        if face == "front" and open_col:
            mid = fw // 2
            if i in (mid - 1, mid) and j > 0:
                inn = np.array(INNER_D, np.float32) + fz(i, j, salt + 8) * 10
                return rgba(inn), None
            if i in (mid - 2, mid + 1) and j > 0:
                return rgba(np.array(TRIM_D, np.float32) * (0.9 + 0.1 * ((j % 2)))), None
        return c, g
    return p


def beard(salt=0, tint=0.0, length_fade=True):
    """Long grey-white strands: columns that wander a little as they hang, each with its own tone, shadows between them."""
    def p(face, i, j, fw, fh):
        if face in ("top", "bottom"):
            return rgba(mx(LGREY, GREY, 0.5) + fz(i, j, salt) * 12), None
        x = i + 1.3 * S(j * 0.37 + i * 0.9 + salt) + 0.6 * S(j * 0.9 + salt * 3)
        sid = int(math.floor(x))
        h = hsh(sid, salt, 3)
        t = 0.2 + 0.8 * h
        c = np.array(mix(GREY, WHITE, t), np.float32)
        frac = x - math.floor(x)
        if frac < 0.22:
            c = c * 0.8 + np.array(DGREY, np.float32) * 0.2               # shadow between two strands
        elif frac > 0.78:
            c = c * 1.05 + 6
        if hsh(sid, salt, 11) > 0.86:
            c = np.array(mix(c, DGREY, 0.45), np.float32)                  # a few darker old hairs
        c = c + fz(i, j, salt + 2) * 10
        if tint > 0:
            c = c * (1 - tint) + np.array((238, 232, 208), np.float32) * tint     # a slight yellow near the mouth
        c = c * (0.9 + 0.1 * (1 - j / max(1, fh - 1)))
        if length_fade and face in ("front", "back") and fw >= 3:
            cx = (fw - 1) / 2.0
            k = fh - 1 - j                                                 # rows from the bottom: the block ends in a point, like a lock of hair
            if k < 2 and abs(i - cx) > (fw * (0.22 if k == 0 else 0.38)) + 0.3:
                return None, None
            if j == 0:
                c = c * 0.78                                               # shadow under the block above
        return rgba(c), None
    return p


def hair(salt=0):
    """Grey-white hair at the sides and back of the head."""
    return beard(salt + 40, 0.0, length_fade=False)


def head_paint(face, i, j, fw, fh):
    """A 9 x 8 face: the brim shades row 0, then brow, heavy lid, two rows of wide eyes, eye bags, and the moustache and beard below."""
    skin = mx(SKIN, SKIN_D, 0.15 + 0.2 * (0.5 + fz(i, j, 2))) + fz(i, j, 6) * 8
    if face == "front":
        eyes = i in (1, 2, 3, 5, 6, 7)
        iris = i in (2, 6)
        c = skin * (0.66 if j == 0 else 1.0)                               # the hat's brim shades the forehead
        if j == 1:
            return rgba(mix(WHITE, GREY, 0.3 + 0.3 * hsh(i, 1, 2))), None  # the brow line (the brow cubes sit over it)
        if j == 2 and eyes:
            return rgba(np.array(SKIN_D) * 0.62), None                     # a heavy, tired upper lid
        if j in (3, 4) and eyes:
            if iris:
                return rgba((52, 42, 38)), rgba((120, 255, 150), 70 if j == 3 else 110)    # the iris, a faint green glint from the orb
            return rgba((226, 218, 204) if j == 3 else (200, 192, 180)), None             # the white
        if j == 5 and eyes:
            return rgba(np.array(SKIN_D) * 0.82), None                     # eye bags
        if j >= 5 and i in (0, 8):
            return rgba(mix(LGREY, WHITE, hsh(i, j, 3))), None             # the beard rising up the cheeks
        if j >= 6:
            return rgba(mix(LGREY, GREY, hsh(i, j, 5))), None
        if j == 1 and i % 2 == 0:
            c = c * 0.9
        return rgba(c), None
    if face == "top":
        return rgba(np.array(HAT_D) * 0.9), None
    if face == "bottom":
        return rgba(np.array(SKIN_D) * 0.8), None
    # sides and back: grey hair, an ear on the side
    if face in ("left", "right") and 3 <= j <= 5 and 3 <= i <= 5:
        return rgba(np.array(SKIN) * (0.88 if j == 3 else 0.8)), None
    return hair(7)(face, i, j, fw, fh)


def skin_paint(salt=0, knuckles=False):
    def p(face, i, j, fw, fh):
        c = mx(SKIN, SKIN_D, 0.25 + 0.25 * (0.5 + fz(i, j, salt))) + fz(i, j, salt + 3) * 8
        if knuckles and face in ("front", "back", "top") and (i + j) % 3 == 0:
            c = c * 0.9
        return rgba(c), None
    return p


def leather(salt=0, buckle=False, stitch=True):
    base = np.array(LEATHER, np.float32)

    def p(face, i, j, fw, fh):
        c = base + fz(i, j, salt) * 16 + (6 if (i * 7 + j * 3) % 11 == 0 else 0)
        if face in ("top", "bottom"):
            return rgba(c * (1.1 if face == "top" else 0.6)), None
        if stitch and (j == 1 or j == fh - 2) and i % 2 == 0:
            c = np.array(TRIM_D, np.float32)
        elif j == 0 or j == fh - 1:
            c = c * 0.7
        if buckle and face == "front" and abs(i - fw // 2) <= 1 and 0 <= j < fh:
            edge = abs(i - fw // 2) == 1 or j in (0, fh - 1)
            return rgba(GOLD if edge else GOLD_D), None
        return rgba(c), None
    return p


def hat_cloth(salt=0, patch=False):
    base, dark, light = np.array(HAT, np.float32), np.array(HAT_D, np.float32), np.array(HAT_L, np.float32)

    def p(face, i, j, fw, fh):
        weave = (3.0 if ((i // 2 + j // 2) % 2) else -3.0) + (2.0 if (i + j) % 2 == 0 else -2.0)
        c = base + weave + fz(i, j, salt) * 16 + nz(i * 0.6, j * 0.6, salt) * 14
        if face == "top":
            return rgba(c * 1.06), None
        if face == "bottom":
            return rgba(c * 0.7), None
        f = 0.5 + 0.5 * S(i * TAU * 2 / max(4, fw) + salt + 0.4 * S(j * 0.4))
        c = c * (0.84 + 0.22 * f)
        if j == fh - 1:
            c = c * 0.78
        if patch and 2 <= i <= 4 and 2 <= j <= 4:                          # a sewn patch
            c = np.array(HAT_L, np.float32) * 0.9 + weave
            if i in (2, 4) or j in (2, 4):
                c = np.array(HAT_D, np.float32) * 1.05
        return rgba(c), None
    return p


def hat_band(face, i, j, fw, fh):
    c = np.array(LEATHER, np.float32) + fz(i, j, 31) * 14
    if face in ("top", "bottom"):
        return rgba(c * 0.9), None
    if j == 0 or j == fh - 1:
        c = c * 0.7
    elif i % 2 == 0 and fh >= 2 and j == 1:
        c = np.array(TRIM_D, np.float32)
    if face == "front" and abs(i - fw // 2) <= 1:                          # a worn brass buckle
        edge = abs(i - fw // 2) == 1 or j in (0, fh - 1)
        return rgba(GOLD if edge else GOLD_D), None
    return rgba(c), None


HB = 8.5                                                                  # half width of the brim


def brim(x0, z0, w, d, up=True):
    """The brim is made of five plates; this paints one of them using the brim's own coordinates (the middle plate or a drooping edge)."""
    base, dark = np.array(HAT, np.float32), np.array(HAT_D, np.float32)

    def p(face, i, j, fw, fh):
        if face in ("top", "bottom"):
            x = x0 + i + 0.5
            z = z0 + d - j - 0.5                                           # j = 0 is the +z edge
            de = min(HB - abs(x), HB - abs(z))
            weave = (3.0 if ((i // 2 + j // 2) % 2) else -3.0) + (2.0 if (i + j) % 2 == 0 else -2.0)
            c = base + weave + fz(x, z, 21) * 18 + nz(x * 0.4, z * 0.4, 9) * 18
            if face == "bottom":
                c = c * 0.8 + np.array((10, 6, 4), np.float32)
                if int(de) in (3, 7) and int(x + z) % 2 == 0:
                    c = c * 0.9
                return rgba(c), None
            c = c * (1.04 + 0.06 * (de / HB))                            # a little lighter out at the rim where the sun wore it
            if de < 0.9:
                c = dark * 1.05                                            # a wired, darker rim
            elif 1.5 <= de < 2.5 and int(x + z + 100) % 2 == 0:
                c = np.array(HAT_L, np.float32) * 0.95                     # the dashed stitch ring near the rim
            elif 4.5 <= de < 5.5 and int(x - z + 100) % 3 != 0:
                c = c * 0.86                                               # a second, shadowy ring
            return rgba(c), None
        return rgba(dark * (1.0 + 0.1 * fz(i, j, 12))), None
    return p


def orb_paint(core=True):
    def p(face, i, j, fw, fh):
        cx, cy = (fw - 1) / 2.0, (fh - 1) / 2.0
        r = math.hypot(i - cx, j - cy) / max(0.7, math.hypot(cx, cy))
        c = np.array(mix(ORB_L, ORB, min(1.0, r * 1.2)), np.float32) if core else np.array(mix(ORB, (56, 190, 96), r), np.float32)
        return rgba(c), rgba(c, 255 if core else 230)
    return p


def halo_paint(face, i, j, fw, fh):
    return None, rgba(ORB, 58)


def mote_paint(face, i, j, fw, fh):
    return rgba(ORB_L), rgba(ORB, 255)


# ------------------------------------------------------------------------------------------------------------- animation helpers
def wave(a, clip, bone, base, amp, period, phase=0.0, axis=0, length=4.0, kind="rot"):
    steps = int(round(length / min(period, length) * 8))
    frames = {}
    for k in range(steps + 1):
        t = length * k / steps
        v = list(base)
        v[axis] += amp * S(TAU * t / period + phase)
        frames[t] = tuple(v)
    (a.rot if kind == "rot" else a.pos if kind == "pos" else a.scale)(clip, bone, frames)


def wave3(a, clip, bone, base, amps, period, phases=(0, 0, 0), length=4.0, kind="rot"):
    steps = int(round(length / min(period, length) * 8))
    frames = {}
    for k in range(steps + 1):
        t = length * k / steps
        frames[t] = tuple(base[n] + amps[n] * S(TAU * t / period + phases[n]) for n in range(3))
    (a.rot if kind == "rot" else a.pos if kind == "pos" else a.scale)(clip, bone, frames)


# ------------------------------------------------------------------------------------------------------------- the model
def build():
    m = Model("tower", "wizard", TEX_W, TEX_H)
    sh = Sheet(TEX_W, TEX_H)
    A = Anim()

    m.bone("root", pivot=(0, 0, 0))

    # ---------------------------------------------------------------- the robe skirt: four layers that flare towards the hem
    m.bone("skirt", "root", pivot=(0, 12, 0))
    layers = ((0, 3, 14, 11), (2, 3, 13, 10), (4, 3, 12, 10), (6, 3, 11, 9))
    for n, (y, h, w, d) in enumerate(layers):
        hem = TRIM if n == 0 else None
        sh.box(m, "skirt", (-w / 2, y, -d / 2), (w, h, d), robe_front(ROBE if n % 2 == 0 else (126, 90, 98), ROBE_D, ROBE_L, n + 1, hem=hem))
    sh.box(m, "skirt", (-2.8, 0, -5.8), (3, 1, 3), leather(3, stitch=False))                   # the toes of two boots under the hem
    sh.box(m, "skirt", (-0.2, 0, -5.8), (3, 1, 3), leather(4, stitch=False))

    # ---------------------------------------------------------------- chest (breathes) and the upper body
    m.bone("chest", "root", pivot=(0, 13, 0))
    sh.box(m, "chest", (-4.5, 10, -3), (9, 7, 6), cloth(ROBE, ROBE_D, ROBE_L, 11, folds=2, stitch=(170, 138, 120), seam_top=False))
    sh.box(m, "chest", (-5, 12.2, -3.5), (10, 2, 7), leather(2, buckle=True))                  # the belt and its buckle
    sh.box(m, "chest", (-5.5, 14.6, -3.5), (11, 3, 7), cloth(ROBE_D, (60, 40, 50), ROBE, 12, folds=3, stitch=(150, 118, 112), seam_top=False))   # shoulder mantle
    sh.box(m, "chest", (-4.5, 16, -3.5), (9, 2, 7), cloth(INNER, INNER_D, ROBE, 13, folds=2, seam_top=False))                                  # collar

    m.bone("upper", "root", pivot=(0, 13, 0))

    # ---------------------------------------------------------------- head
    m.bone("head", "upper", pivot=(0, 17, 0), rotation=(-3, 0, 0))
    sh.box(m, "head", (-4.5, 16.3, -4), (9, 8, 8), head_paint)
    sh.box(m, "head", (-5.4, 14.4, -3), (1, 9, 6), hair(3))                                    # long grey hair at the sides ...
    sh.box(m, "head", (4.4, 14.4, -3), (1, 9, 6), hair(4))
    sh.box(m, "head", (-4.5, 13.6, 3.1), (9, 10, 2), hair(5))                                  # ... and down the back
    sh.box(m, "head", (-1, 18.8, -5.0), (2, 3, 1), skin_paint(9), rotation=(-12, 0, 0), pivot=(0, 21, -4.2))     # nose
    sh.box(m, "head", (-1, 18.4, -5.6), (2, 1, 1), skin_paint(10))                                                # the nose tip
    # bushy brows: a heavy ridge and bushes whose outer ends droop (tired, kind)
    sh.box(m, "head", (-4.0, 22.2, -4.5), (8, 1, 1), beard(21, 0.0, False))
    sh.box(m, "head", (0.6, 22.2, -5.1), (4, 1, 1), beard(22, 0.0, False), rotation=(0, 0, -10), pivot=(0.6, 22.7, -4.6))
    sh.box(m, "head", (-4.6, 22.2, -5.1), (4, 1, 1), beard(23, 0.0, False), rotation=(0, 0, 10), pivot=(-0.6, 22.7, -4.6))
    sh.box(m, "head", (1.0, 23.0, -4.8), (3, 1, 1), beard(24, 0.0, False), rotation=(0, 0, -6), pivot=(1.0, 23.4, -4.4))
    sh.box(m, "head", (-4.0, 23.0, -4.8), (3, 1, 1), beard(25, 0.0, False), rotation=(0, 0, 6), pivot=(-1.0, 23.4, -4.4))
    # moustache
    sh.box(m, "head", (0, 17.7, -5.8), (5, 1, 2), beard(26, 0.12, False), rotation=(0, 0, -9), pivot=(0, 18.2, -5.0))
    sh.box(m, "head", (-5, 17.7, -5.8), (5, 1, 2), beard(27, 0.12, False), rotation=(0, 0, 9), pivot=(0, 18.2, -5.0))

    # the beard: a chain of stacked, tapering blocks hung from the cheeks that sway in the idle clip
    m.bone("beard1", "head", pivot=(0, 17, -4.5))
    sh.box(m, "beard1", (-4, 13.6, -5.9), (8, 4, 2), beard(30, 0.1))
    sh.box(m, "beard1", (-5, 15.4, -4.9), (10, 3, 1), beard(31, 0.05))                           # the cheeks, wider than the chin
    sh.box(m, "beard1", (-5.0, 12.6, -3.7), (1, 5, 3), beard(36))                                # side locks falling beside the chest
    sh.box(m, "beard1", (4.0, 12.6, -3.7), (1, 5, 3), beard(37))
    m.bone("beard2", "beard1", pivot=(0, 14.0, -4.9))
    sh.box(m, "beard2", (-3.5, 11.2, -5.9), (7, 3, 2), beard(32))
    sh.box(m, "beard2", (-2.2, 11.8, -6.5), (2, 2, 1), beard(38), rotation=(0, 0, 8), pivot=(-1.2, 13.6, -6.0))   # two loose locks
    sh.box(m, "beard2", (0.2, 11.4, -6.5), (2, 2, 1), beard(39), rotation=(0, 0, -8), pivot=(1.2, 13.6, -6.0))
    m.bone("beard3", "beard2", pivot=(0, 11.6, -4.9))
    sh.box(m, "beard3", (-3, 8.8, -5.7), (6, 3, 2), beard(33))
    m.bone("beard4", "beard3", pivot=(0, 9.2, -4.7))
    sh.box(m, "beard4", (-2, 7.6, -5.5), (4, 2, 2), beard(34))
    m.bone("beard5", "beard4", pivot=(0, 8.0, -4.5))
    sh.box(m, "beard5", (-1, 6.0, -5.3), (2, 2, 1), beard(35))

    # ---------------------------------------------------------------- the hat: a wide thin drooping brim and a cone of five bent blocks
    m.bone("hat", "head", pivot=(0, 22.6, 0), rotation=(13, 0, 0))                       # tipped well back so the face shows under the brim
    sh.box(m, "hat", (-5, 22, -5), (10, 1, 10), brim(-5, -5, 10, 10))
    sh.box(m, "hat", (-8.5, 22, -8.5), (17, 1, 4), brim(-8.5, -8.5, 17, 4), rotation=(-3, 0, 0), pivot=(0, 22.5, -4.8))    # front edge, drooping
    sh.box(m, "hat", (-8.5, 22, 4.5), (17, 1, 4), brim(-8.5, 4.5, 17, 4), rotation=(6, 0, 0), pivot=(0, 22.5, 4.8))        # back edge
    sh.box(m, "hat", (4.5, 22, -4.5), (4, 1, 9), brim(4.5, -4.5, 4, 9), rotation=(0, 0, -5), pivot=(4.8, 22.5, 0))         # the +x edge
    sh.box(m, "hat", (-8.5, 22, -4.5), (4, 1, 9), brim(-8.5, -4.5, 4, 9), rotation=(0, 0, 5), pivot=(-4.8, 22.5, 0))       # the -x edge
    sh.box(m, "hat", (-5.5, 22.6, -5.5), (11, 3, 11), hat_cloth(41, patch=True))
    sh.box(m, "hat", (-5.5, 22.6, -5.5), (11, 1, 11), hat_band, inflate=0.3)
    cone = ((25.2, 9, 3, 2, 0.0, 2), (27.4, 7, 2, 3, 0.0, 3), (28.8, 5, 2, 5, 0.0, 4), (30.2, 3, 2, 8, 0.0, 5))
    prev = "hat"
    for n, (y, w, h, rx, ry, rz) in enumerate(cone):
        name = "hat%d" % (n + 2)
        m.bone(name, prev, pivot=(0, y, 0), rotation=(rx, ry, rz))
        sh.box(m, name, (-w / 2, y - 0.4, -w / 2), (w, h + 1, w), hat_cloth(42 + n))
        prev = name

    # ---------------------------------------------------------------- arms: wide sleeves, the hands cupped round the orb
    for sx, nm in ((1, "arm_l"), (-1, "arm_r")):
        px = sx * 5.0
        m.bone(nm, "upper", pivot=(px, 15.8, 0), rotation=(32, sx * 2, 0))
        sh.box(m, nm, (px - 2, 11, -2), (4, 5, 4), cloth(ROBE, ROBE_D, ROBE_L, 50 + sx, folds=2, stitch=(170, 138, 120), seam_top=False))
        m.bone(nm + "2", nm, pivot=(px, 11.2, 0), rotation=(72, 0, 0))
        sh.box(m, nm + "2", (px - 2, 8, -2), (4, 3, 4), cloth(ROBE, ROBE_D, ROBE_L, 52 + sx, folds=2, stitch=(170, 138, 120), seam_top=False))
        sh.box(m, nm + "2", (px - 2.5, 5.2, -2.5), (5, 3, 5), cloth((132, 98, 104), ROBE_D, ROBE_L, 54 + sx, folds=3, hem=TRIM, hem_rows=2, seam_top=False))   # the wide cuff
        m.bone(nm + "_hand", nm + "2", pivot=(px, 5.6, 0))
        sh.box(m, nm + "_hand", (px - 1, 3.2, -1), (2, 3, 2), skin_paint(60 + sx, knuckles=True))
        sh.box(m, nm + "_hand", (px - sx * 1.0 - 0.5, 3.6, -1.6), (1, 2, 1), skin_paint(62 + sx))     # the thumb

    # ---------------------------------------------------------------- the orb, floating at chest / mouth height between the hands
    OY, OZ = 15.4, -9.0                                                                          # (before the figure is lowered by 3)
    m.bone("orb", "root", pivot=(0, OY, OZ))
    sh.box(m, "orb", (-1.5, OY - 1.5, OZ - 1.5), (3, 3, 3), orb_paint(True))
    sh.box(m, "orb", (-1, OY - 1, OZ - 1), (2, 2, 2), orb_paint(False), rotation=(45, 45, 0), pivot=(0, OY, OZ), inflate=0.5)
    sh.box(m, "orb", (-1.5, OY - 1.5, OZ - 1.5), (3, 3, 3), halo_paint, inflate=0.9)                     # the soft halo (glow layer only)
    for k, (dx, dy, dz) in enumerate(((2.9, 0.4, 0), (-2.0, -0.6, 2.4), (-1.2, 1.2, -2.6))):
        m.bone("mote%d" % (k + 1), "orb", pivot=(0, OY, OZ))
        sh.box(m, "mote%d" % (k + 1), (dx - 0.5, OY + dy - 0.5, OZ + dz - 0.5), (1, 1, 1), mote_paint)

    # ================================================================ animations (both 4 s loops so the idle <-> talk switch is smooth)
    L = 4.0
    A.clip("idle", L, True)
    wave3(A, "idle", "chest", (1, 1, 1), (0, 0.018, 0.035), 4.0, (0, 0, 0), L, "scale")
    wave(A, "idle", "upper", (0, 0, 0), 0.18, 4.0, 0, 1, L, "pos")
    wave3(A, "idle", "head", (-3, 0, 0), (1.6, 2.2, 0.8), 4.0, (0.5, 0, 1.4), L)
    for n in range(1, 6):
        wave3(A, "idle", "beard%d" % n, (0, 0, 0), (1.6 + 0.5 * n, 0, 0.8 + 0.5 * n), 4.0, (-0.5 * n, 0, -0.45 * n + 1.2), L)
    hat_base = {2: (2, 0, 2), 3: (3, 0, 3), 4: (5, 0, 4), 5: (8, 0, 5)}
    for n, b in hat_base.items():
        wave3(A, "idle", "hat%d" % n, b, (1.0 + 0.4 * n, 0.5 * n, 1.2 + 0.4 * n), 4.0, (-0.6 * n, 0.3 * n, -0.5 * n), L)
    for nm, sx in (("arm_l", 1), ("arm_r", -1)):
        wave3(A, "idle", nm, (32, sx * 2, 0), (1.2, 0, 0), 4.0, (1.0, 0, 0), L)
    wave(A, "idle", "orb", (0, 0, 0), 0.45, 2.0, 0, 1, L, "pos")
    wave3(A, "idle", "orb", (1, 1, 1), (0.1, 0.1, 0.1), 2.0, (0, 0, 0), L, "scale")
    for k in (1, 2, 3):
        A.rot("idle", "mote%d" % k, {0.0: (0, 0, 0), 2.0: (0, 180 * (1 if k != 2 else -1), 0), 4.0: (0, 360 * (1 if k != 2 else -1), 0)})
    wave3(A, "idle", "mote2", (0, 0, 0), (0, 0.6, 0), 2.0, (0, 0, 0), L, "pos")
    wave3(A, "idle", "mote3", (0, 0, 0), (0, 0.5, 0), 1.0, (0, 1.0, 0), L, "pos")

    A.clip("talk", L, True)
    for bone, tracks in A.clips["idle"]["bones"].items():
        A.clips["talk"]["bones"][bone] = {k: dict(v) for k, v in tracks.items()}
    # a small nod every second, the beard bouncing a touch behind it
    wave3(A, "talk", "head", (-3, 0, 0), (-4.0, 3.0, 1.0), 1.0, (-math.pi / 2, 0, 0), L)
    for n in range(1, 6):
        wave3(A, "talk", "beard%d" % n, (0, 0, 0), (2.2 + 0.7 * n, 0, 0.8 + 0.5 * n), 1.0, (-math.pi / 2 - 0.6 * n, 0, 0), L)
    # the -x hand lets go of the orb and makes a small gesture, then comes back
    A.rot("talk", "arm_r", {0.0: (32, -2, 0), 0.5: (46, -4, 0), 1.0: (54, 12, -8), 1.5: (46, 4, -2), 2.0: (54, 14, -8), 2.5: (46, 4, -2), 3.0: (40, -6, 0), 4.0: (32, -2, 0)})
    A.rot("talk", "arm_r2", {0.0: (72, 0, 0), 0.5: (66, 0, 0), 1.0: (50, 0, 0), 1.5: (62, 0, 0), 2.0: (48, 0, 0), 2.5: (62, 0, 0), 3.0: (68, 0, 0), 4.0: (72, 0, 0)})
    return m, sh, A


def lower(m, dy):
    """Everything above the skirt sits dy lower (the figure is stout, about 1.9 blocks with the hat)."""
    for n, b in m.bones.items():
        if n == "skirt":
            continue
        d = dy + (1.5 if n.startswith("hat") else 0.0)                 # the hat sits a pixel above the head, so the face shows under the brim
        b["pivot"][1] += d
        for c in b["cubes"]:
            c["origin"][1] += d
            if "pivot" in c:
                c["pivot"][1] += d


def main():
    m, sh, A = build()
    lower(m, -3.0)
    img, glow = sh.images()
    m.save(A)
    m.save_texture(img, glow)
    print(m.id, "cubes", m.cube_count(), "texture rows used", sh.used(), "of", sh.H)


if __name__ == "__main__":
    main()
