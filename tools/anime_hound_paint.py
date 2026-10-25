"""Painter of the anime hell hound (tools/gen_anime_hound.py): a limited palette, cel-shaded surface painters evaluated at 4x4 samples per texel and reduced by
MODE (so every texel is one palette entry, no blur, no dither, no noise), 3D crack patterns that run on from one cube face to the next, 1-texel painted outlines
(drawn at final resolution, in the darker tone of the material's own family), alpha-cut card shapes, the five eye plates, flame cards, bone plates.

Nothing here writes into the game by itself; gen_anime_hound.py packs the pictures into the sheet and writes the files.
"""
import math
import sys

import numpy as np

sys.dont_write_bytecode = True
SS = 4                                                          # samples per texel side

# ------------------------------------------------------------------------------------------------------------------ palette (index 0 = transparent)
# five hue families, each a base / shadow (shifted to violet) / light (shifted to warm yellow) + an outline tone: charcoal fur, ember, bone, nose / mouth, eye.
_COLS = [
    ("none", (0, 0, 0), "none"),
    ("F_O", (14, 10, 24), "F_O"), ("F_S", (30, 25, 46), "F_O"), ("F_B", (55, 47, 72), "F_O"), ("F_L", (96, 80, 98), "F_O"),          # charcoal fur
    ("E_O", (58, 12, 22), "E_O"), ("E_0", (116, 28, 30), "E_O"), ("E_1", (204, 64, 24), "E_O"), ("E_2", (255, 136, 34), "E_O"),     # ember
    ("E_3", (255, 214, 94), "E_O"), ("E_W", (255, 246, 206), "E_O"),
    ("B_O", (62, 40, 50), "B_O"), ("B_S", (152, 124, 132), "B_O"), ("B_B", (218, 200, 176), "B_O"), ("B_L", (250, 238, 208), "B_O"),  # bone
    ("N_B", (28, 22, 38), "F_O"), ("N_L", (112, 98, 134), "F_O"),                                                                     # nose
    ("M_I", (112, 22, 40), "E_O"), ("M_T", (226, 84, 70), "E_O"),                                                                     # mouth, tongue
    ("X_P", (34, 8, 14), "F_O"),                                                                                                      # pupil
]
NAMES = [c[0] for c in _COLS]
IDX = {n: i for i, n in enumerate(NAMES)}
RGB = np.array([c[1] for c in _COLS], np.uint8)
OUTL = np.array([IDX[c[2]] for c in _COLS], np.uint8)           # the outline tone of each entry's family
N = IDX
LIGHT_DIR = np.array([0.0, 0.88, -0.47]) / math.hypot(0.88, 0.47)


# ------------------------------------------------------------------------------------------------------------------ small maths
def tri(x):
    f = x - np.floor(x)
    return 1.0 - np.abs(2.0 * f - 1.0)                          # 0..1 triangle wave, period 1


def smoothstep(a, b, x):
    t = np.clip((x - a) / (b - a), 0.0, 1.0)
    return t * t * (3 - 2 * t)


def _h(ix, iy, iz, seed):
    h = (ix * 73856093) ^ (iy * 19349663) ^ (iz * 83492791) ^ (seed * 2654435761)
    h &= 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    h ^= h >> 16
    return (h & 0xFFFF) / 65535.0


def worley(P, cell, seed):
    """F1, F2 of 3D cellular noise at the points P (n, 3); distances in the units of P."""
    q = P / cell
    base = np.floor(q).astype(np.int64)
    f1 = np.full(len(P), 9.0)
    f2 = np.full(len(P), 9.0)
    for dx in (-1, 0, 1):
        for dy in (-1, 0, 1):
            for dz in (-1, 0, 1):
                ix, iy, iz = base[:, 0] + dx, base[:, 1] + dy, base[:, 2] + dz
                jx = ix + _h(ix, iy, iz, seed)
                jy = iy + _h(ix, iy, iz, seed + 1)
                jz = iz + _h(ix, iy, iz, seed + 2)
                d = np.sqrt((jx - q[:, 0]) ** 2 + (jy - q[:, 1]) ** 2 + (jz - q[:, 2]) ** 2)
                lo = d < f1
                f2 = np.where(lo, f1, np.minimum(f2, d))
                f1 = np.where(lo, d, f1)
    return f1 * cell, f2 * cell


def blobs(X, Y, Z, seed=0.0):
    """A smooth low-frequency field (sum of sines), -1.5..1.5: where the cracks gather."""
    return (np.sin(X * 0.33 + 1.7 + seed) + np.sin(Y * 0.41 + 0.4 + seed * 1.3) * np.sin(Z * 0.27 + 2.2 + seed * 0.7) + np.sin((X + Z) * 0.19 + Y * 0.23 + seed * 2.1) * 0.7)


# ------------------------------------------------------------------------------------------------------------------ the context a painter gets
class Ctx:
    """One face of a part: sample grids at SS per texel (X Y Z = the rest-pose model position of every sample, u v = 0..1 over the face), the face's outward normal, the part's parameters."""

    def __init__(self, X, Y, Z, U, V, w, h, normal, face, pa, shape_hw):
        self.X, self.Y, self.Z, self.U, self.V = X, Y, Z, U, V
        self.w, self.h, self.n, self.face, self.pa = w, h, normal, face, pa
        self.light = float(np.dot(normal, LIGHT_DIR))
        self.shape = X.shape
        self.hw = shape_hw                                       # (W, H) in texels


def full(c, idx):
    return np.full(c.shape, idx, np.uint8)


def tone3(c, shadow, base, light, lo=-0.2, hi=0.55):
    """The face decides the tone: lit from above and a little from the front, the back and the belly side in shade."""
    return shadow if c.light < lo else (light if c.light > hi else base)


# ------------------------------------------------------------------------------------------------------------------ materials. Each returns (colour index grid, glow index grid, alpha grid or None)
def _zig(c, k=0.34, amp=0.9, salt=0.0):
    return (tri((c.Z + 0.6 * c.X) * k + salt) - 0.5) * 2.0 * amp


def m_fur(c):
    pa = c.pa
    col = full(c, tone3(c, N["F_S"], N["F_B"], N["F_L"], lo=pa.get("lo", -0.2), hi=pa.get("hi", 0.55)))
    glow = np.zeros(c.shape, np.uint8)
    zz = _zig(c, 0.30, pa.get("amp", 0.9), pa.get("salt", 0.0))
    if pa.get("li_y") is not None:                               # upper light band, a zigzag of fur tufts
        col = np.where(c.Y > pa["li_y"] + zz, N["F_L"], col)
    if pa.get("sh_y") is not None:                               # lower shadow band
        col = np.where(c.Y < pa["sh_y"] + zz, N["F_S"], col)
    if pa.get("dark_y") is not None:                             # leg stocking: the darkest tone below a line
        col = np.where(c.Y < pa["dark_y"] + zz * 0.6, N["F_S"], col)
    if pa.get("em_y") is not None:                               # ember underglow: the belly is lit from inside
        em = c.Y < pa["em_y"] + zz * 0.9
        col = np.where(em, N["E_0"], col)
        glow = np.where(em, N["E_0"], glow)
        em2 = c.Y < pa["em_y"] - 1.5 + zz * 0.5
        col = np.where(em2, N["E_1"], col)
        glow = np.where(em2, N["E_1"], glow)
    if pa.get("crack"):
        n = c.shape[0] * c.shape[1]
        P = np.stack([c.X.ravel(), c.Y.ravel(), c.Z.ravel()], 1)
        f1, f2 = worley(P, pa.get("cell", 6.5), pa.get("seed", 3))
        edge = (f2 - f1).reshape(c.shape)
        patch = blobs(c.X, c.Y, c.Z, pa.get("pseed", 0.0))
        on = patch > pa.get("thr", 0.55)
        wd = pa.get("cw", 0.46)
        scorch = on & (edge < wd * 2.6)
        col = np.where(scorch & (col != N["F_L"]), np.where(col == N["F_B"], N["F_S"], col), col)
        core = on & (edge < wd)
        col = np.where(core, N["E_2"], col)
        glow = np.where(core, N["E_2"], glow)
        hot = on & (edge < wd * 0.42)
        col = np.where(hot, N["E_3"], col)
        glow = np.where(hot, N["E_3"], glow)
    return col, glow, None


def _dot(c, col, glow, U, V, r, color, glow_color=None):
    m = ((c.U - U) * c.w) ** 2 + ((c.V - V) * c.h) ** 2 < r * r
    col = np.where(m, color, col)
    if glow_color is not None:
        glow = np.where(m, glow_color, glow)
    return col, glow


def m_skull(c):
    """The cranium: fur, the two ember brow dots, a brow line; faces only differ by their normal."""
    col, glow, _ = m_fur(c)
    if c.face == "north":
        for sx in (-1, 1):
            col, glow = _dot(c, col, glow, 0.5 + sx * 0.30, 0.20, 0.52, N["E_2"], N["E_2"])
            col, glow = _dot(c, col, glow, 0.5 + sx * 0.30, 0.20, 0.26, N["E_3"], N["E_3"])
        brow = (c.V > 0.30) & (c.V < 0.42) & (np.abs(c.U - 0.5) > 0.12)
        col = np.where(brow, N["F_S"], col)
    return col, glow, None


def m_muzzle(c):
    col, glow, _ = m_fur(c)
    pa = c.pa
    if c.face == "down":                                         # the roof of the mouth with the upper teeth along both edges
        col = full(c, N["M_I"])
        for t0 in (0.0, 1.0):
            rowm = (np.abs(c.U - t0) < 0.0)
        teeth = (tri(c.Z * 0.9) > 0.5)
        edge = (np.abs(c.U - 0.5) > 0.30)
        col = np.where(teeth & edge, N["B_L"], col)
        return col, glow, None
    if c.face in ("west", "east"):                               # the lip line along the lower edge of the muzzle, whisker dots
        lip = (c.V > 0.80)
        col = np.where(lip, N["F_S"], col)
        for k in range(3):
            col, glow = _dot(c, col, glow, 0.30 + 0.17 * k, 0.52, 0.28, N["F_L"])
    return col, glow, None


def m_jaw(c):
    col, glow, _ = m_fur(c)
    if c.face == "up":                                           # inside of the lower jaw: lower teeth, a tongue
        col = full(c, N["M_I"])
        tongue = (np.abs(c.U - 0.5) < 0.20) & (c.V < 0.80)
        col = np.where(tongue, N["M_T"], col)
        teeth = (tri(c.Z * 0.9 + 0.5) > 0.5) & (np.abs(c.U - 0.5) > 0.30)
        col = np.where(teeth, N["B_L"], col)
        glow = np.where(tongue, N["E_0"], glow)
        return col, glow, None
    if c.face == "down":
        return full(c, N["F_S"]), glow, None
    return col, glow, None


def m_nose(c):
    col = full(c, N["N_B"])
    if c.face in ("up", "north"):
        sh = ((c.U - 0.30) ** 2 * 5 + (c.V - 0.42) ** 2 * 7) < 0.14
        col = np.where(sh, N["N_L"], col)
    if c.face == "north":                                        # two nostrils
        for sx in (-1, 1):
            m = ((c.U - 0.5 - sx * 0.2) ** 2 * 6 + (c.V - 0.70) ** 2 * 14) < 0.10
            col = np.where(m, N["F_O"], col)
    return col, np.zeros(c.shape, np.uint8), None


def m_bone(c):
    pa = c.pa
    t = tone3(c, N["B_S"], N["B_B"], N["B_L"], lo=-0.1, hi=0.5)
    col = full(c, t)
    glow = np.zeros(c.shape, np.uint8)
    # ridge grooves across the plate, a hard shade band on the lower third
    groove = (tri(c.V * c.h * 0.42 + pa.get("salt", 0.0)) < 0.10)
    col = np.where(groove & (col == N["B_B"]), N["B_S"], col)
    col = np.where((c.V > 0.72), N["B_S"] if t != N["B_S"] else N["B_S"], col)
    if pa.get("seam"):                                           # an ember crack split through the plate
        s = np.abs(c.U - (0.5 + 0.18 * np.sin(c.V * 7.0 + pa.get("salt", 0.0)))) < 0.075
        col = np.where(s, N["E_2"], col)
        glow = np.where(s, N["E_2"], glow)
    return col, glow, None


# ----- card shapes: (mask grid) from the face's u, v
def shape_mask(kind, c):
    U, V = c.U, c.V
    if kind == "rect":
        return np.ones(c.shape, bool)
    if kind in ("tri", "spike"):                                 # apex at the top, base at the bottom
        hw = 0.5 * V ** (1.1 if kind == "tri" else 1.0)
        return np.abs(U - 0.5 - c.pa.get("lean", 0.0) * (1 - V)) <= hw
    if kind == "tri_dn":                                         # apex at the bottom (fangs, hanging tufts)
        hw = 0.5 * (1 - V) ** 1.05
        return np.abs(U - 0.5 - c.pa.get("lean", 0.0) * V) <= hw
    if kind == "claw":                                           # a hooked triangle, the apex curls forward
        hw = 0.5 * (1 - V) ** 1.1
        return np.abs(U - 0.5 - 0.28 * V ** 2) <= hw
    if kind == "tuft":                                           # a pointed fur lock: wide at the root (top) and curling to the point (bottom)
        hw = 0.5 * (1 - V ** 1.6) ** 0.9 * (0.82 + 0.18 * np.cos(V * 6.0))
        return np.abs(U - 0.5 - c.pa.get("lean", 0.12) * V ** 1.5) <= hw
    if kind == "flame":
        ph = c.pa.get("phase", 0.0)
        xc = 0.5 + c.pa.get("lean", 0.16) * np.sin(3.1 * (1 - V) + ph) * (1 - V) ** 0.7
        hw = 0.5 * (V ** 0.62) * (0.78 + 0.22 * np.cos(9.0 * V + ph)) * (1.0 - 0.08 * (1 - V))
        return np.abs(U - xc) <= hw
    raise ValueError(kind)


def m_card_fur(c):
    """A fur card: the fur tones in bands from the root (top / bottom) to the tip, with one ember streak optionally."""
    pa = c.pa
    root = c.V if pa.get("root_top", True) else 1 - c.V
    col = full(c, N["F_B"])
    col = np.where(root > 0.55, N["F_S"], col)
    col = np.where(root < 0.22, N["F_L"], col)
    glow = np.zeros(c.shape, np.uint8)
    if pa.get("ember"):
        m = (np.abs(c.U - 0.5) < 0.09) & (root > 0.1) & (root < 0.75)
        col = np.where(m, N["E_2"], col)
        glow = np.where(m, N["E_2"], glow)
    mask = shape_mask(c.pa["shape"], c)
    return col, glow, mask


def m_ear_in(c):
    m = shape_mask("tri", c)
    col = full(c, N["E_0"])
    col = np.where(c.V > 0.45, N["E_1"], col)
    col = np.where((c.V > 0.78) & (np.abs(c.U - 0.5) < 0.2), N["E_2"], col)
    glow = np.where(c.V > 0.45, N["E_0"], 0).astype(np.uint8)
    return col, glow, m


def m_ear_out(c):
    m = shape_mask("tri", c)
    col = full(c, N["F_B"])
    col = np.where(c.V > 0.62, N["F_S"], col)
    col = np.where(np.abs(c.U - 0.5 - 0.0) < 0.5 * c.V ** 1.1 - 0.30, N["F_B"], col)
    col = np.where((c.V < 0.25), N["F_L"], col)
    return col, np.zeros(c.shape, np.uint8), m


def m_flame(c):
    """A flame card: bands from the root (bottom) to the tip (top): deep ember, red-orange, orange, yellow, white-hot core."""
    m = shape_mask("flame", c)
    t = 1.0 - c.V                                                # 0 at the root, 1 at the tip... (the root is at the bottom of the card)
    tipf = c.pa.get("tip", 0.0)
    col = full(c, N["E_0"])
    col = np.where(t > 0.26, N["E_1"], col)
    col = np.where(t > 0.50, N["E_2"], col)
    col = np.where(t > 0.74, N["E_3"], col)
    xc = 0.5 + c.pa.get("lean", 0.16) * np.sin(3.1 * (1 - c.V) + c.pa.get("phase", 0.0)) * (1 - c.V) ** 0.7
    hw = 0.5 * (c.V ** 0.62) * (0.78 + 0.22 * np.cos(9.0 * c.V + c.pa.get("phase", 0.0)))
    core = (np.abs(c.U - xc) < hw * 0.38) & (t > 0.10) & (t < 0.82)
    col = np.where(core, np.where(t > 0.50, N["E_W"], N["E_3"]), col)
    glow = col.copy()
    return col, glow, m


def m_claw(c):
    m = shape_mask("claw", c)
    col = full(c, N["B_B"])
    col = np.where(c.V > 0.55, N["B_S"], col)
    col = np.where(c.V < 0.25, N["B_L"], col)
    return col, np.zeros(c.shape, np.uint8), m


def m_fang(c):
    m = shape_mask("tri_dn", c)
    col = full(c, N["B_L"])
    col = np.where(c.U > 0.62, N["B_B"], col)
    col = np.where(c.V < 0.14, N["B_S"], col)
    return col, np.zeros(c.shape, np.uint8), m


def m_spike(c):
    m = shape_mask("spike", c)
    col = full(c, N["B_B"])
    col = np.where(c.U > 0.55, N["B_S"], col)
    col = np.where(c.V < 0.26, N["B_L"], col)
    col = np.where(c.V > 0.86, N["E_1"], col)
    glow = np.where(c.V > 0.86, N["E_1"], 0).astype(np.uint8)
    return col, glow, m


def m_ember_spike(c):
    """A small ridge spike: charcoal with an ember root."""
    m = shape_mask("spike", c)
    col = full(c, N["F_L"])
    col = np.where(c.U > 0.55, N["F_B"], col)
    col = np.where(c.V > 0.70, N["E_1"], col)
    col = np.where(c.V > 0.88, N["E_2"], col)
    glow = np.where(c.V > 0.70, N["E_1"], 0).astype(np.uint8)
    return col, glow, m


# ------------------------------------------------------------------------------------------------------------------ the five eye plates
def _eye_geometry(c, plate, mirror):
    U = (1.0 - c.U) if mirror else c.U                           # U = 0 at the nose side (inner corner), 1 at the outer corner
    V = c.V
    s = np.sin(np.pi * np.clip(U, 0, 1))
    slant = {"open": 0.30, "narrow": 0.20, "snarl": 0.46, "glow": 0.22, "closed": 0.10}[plate]
    top0, top1 = 0.60, 0.60 - slant                             # y of the upper lid at the inner / outer corner
    bot0, bot1 = 0.78, 0.78 - slant * 0.9
    ytop = top0 + (top1 - top0) * U - 0.20 * s ** 0.9
    ybot = bot0 + (bot1 - bot0) * U + 0.12 * s ** 1.1
    if plate == "narrow":
        ybot = ytop + (ybot - ytop) * 0.42
    elif plate == "snarl":
        ybot = ytop + (ybot - ytop) * 0.68
    elif plate == "glow":
        ytop -= 0.06
        ybot += 0.07
    return U, V, s, ytop, ybot


def eye_plate(c):
    """The eye of a plate (pa['plate'], pa['mirror']): returns colour, glow, alpha (the glow plate also has a halo that exists on the glow sheet only)."""
    plate, mirror = c.pa["plate"], c.pa.get("mirror", False)
    U, V, s, ytop, ybot = _eye_geometry(c, plate, mirror)
    col = np.zeros(c.shape, np.uint8)
    glow = np.zeros(c.shape, np.uint8)
    alpha = np.zeros(c.shape, bool)
    # brow: a dark fur stroke above the eye on every plate
    brow_c = ytop - 0.17 - 0.0 * U
    brow = (np.abs(V - brow_c) < 0.075 + 0.05 * s) & (U > 0.0) & (U < 1.02)
    if plate == "snarl":
        brow = (np.abs(V - (ytop - 0.15 - 0.10 * U)) < 0.11) & (U < 1.05)
    col = np.where(brow, N["F_O"], col)
    alpha |= brow
    if plate == "closed":
        arc = 0.50 + 0.04 * U + 0.15 * s
        line = (np.abs(V - arc) < 0.085 + 0.02 * s) & (U > 0.02) & (U < 1.0)
        col = np.where(line, N["F_O"], col)
        alpha |= line
        ember = (np.abs(V - (arc + 0.15)) < 0.045) & (U > 0.12) & (U < 0.88)
        col = np.where(ember, N["E_0"], col)
        glow = np.where(ember, N["E_1"], glow)
        alpha |= ember
        for ux in (0.35, 0.62, 0.88):                            # lash ticks
            tick = (np.abs(U - ux) < 0.035) & (V > arc - 0.02) & (V < arc + 0.15)
            col = np.where(tick, N["F_O"], col)
            alpha |= tick
        return col, glow, alpha
    inside = (U > 0.02) & (U < 1.0) & (V > ytop) & (V < ybot)
    lid = (U > -0.02) & (U < 1.04) & (V > ytop - 0.13) & (V <= ytop) | ((V > ybot) & (V < ybot + 0.07) & (U > 0.05) & (U < 0.96))
    if plate == "glow":
        halo = (V > ytop - 0.30) & (V < ybot + 0.26) & (U > -0.08) & (U < 1.10)
        alpha_g = halo
        glow = np.where(halo & ~inside, N["E_1"], glow)
        glow = np.where(halo & ~inside & (V > ytop - 0.18) & (V < ybot + 0.14) & (U > -0.02) & (U < 1.05), N["E_2"], glow)
    col = np.where(lid, N["F_O"], col)
    alpha |= lid
    body = N["E_2"] if plate != "snarl" else N["E_1"]
    col = np.where(inside, body, col)
    alpha |= inside
    glow = np.where(inside, body, glow)
    # iris and pupil
    ic_x, ic_y = 0.55, (ytop + ybot) * 0.5 + 0.0
    ry = np.maximum((ybot - ytop) * 0.5, 0.01)
    iris = inside & (((U - ic_x) / 0.26) ** 2 + ((V - ic_y) / (ry * 1.0)) ** 2 < 1.0)
    col = np.where(iris, N["E_3"], col)
    glow = np.where(iris, N["E_3"], glow)
    if plate in ("open", "snarl", "narrow"):
        pup = inside & (np.abs(U - ic_x) < (0.05 if plate == "open" else 0.04)) & (np.abs(V - ic_y) < ry * 0.95)
        col = np.where(pup, N["X_P"], col)
        glow = np.where(pup, 0, glow)
    if plate == "open":
        hl = inside & (np.abs(U - (ic_x - 0.10)) < 0.045) & (np.abs(V - (ic_y - ry * 0.5)) < 0.06)
        col = np.where(hl, N["E_W"], col)
        glow = np.where(hl, N["E_W"], glow)
    if plate == "glow":
        core = inside & (((U - ic_x) / 0.40) ** 2 + ((V - ic_y) / (ry * 0.78)) ** 2 < 1.0)
        col = np.where(core, N["E_W"], col)
        glow = np.where(core, N["E_W"], glow)
        # the halo exists on the glow sheet only: opaque nothing on the base sheet
        return col, glow, alpha | (glow > 0)
    return col, glow, alpha


# ------------------------------------------------------------------------------------------------------------------ dispatch
MATS = {"fur": m_fur, "skull": m_skull, "muzzle": m_muzzle, "jaw": m_jaw, "nose": m_nose, "bone": m_bone, "card_fur": m_card_fur, "ear_in": m_ear_in, "ear_out": m_ear_out,
        "flame": m_flame, "claw": m_claw, "fang": m_fang, "spike": m_spike, "ember_spike": m_ember_spike, "eye": eye_plate}


# ------------------------------------------------------------------------------------------------------------------ reduction and outlines
def _mode_reduce(a, S, n_idx):
    H, W = a.shape[0] // S, a.shape[1] // S
    blk = a.reshape(H, S, W, S).transpose(0, 2, 1, 3).reshape(H, W, S * S)
    best = np.zeros((H, W), np.uint8)
    bestc = np.full((H, W), -1, np.int32)
    for k in np.unique(blk):
        cnt = (blk == k).sum(-1)
        better = cnt > bestc
        best = np.where(better, k, best)
        bestc = np.where(better, cnt, bestc)
    return best


def _erode(m):
    p = np.pad(m, 1, constant_values=False)
    return m & p[:-2, 1:-1] & p[2:, 1:-1] & p[1:-1, :-2] & p[1:-1, 2:]


def reduce_face(col4, glow4, alpha4, W, H, outline, edges, is_card, thick=1):
    """4x4 samples -> one texel each (mode); the alpha by majority; the outline at final resolution: the card's rim (inside the cut) or the face border."""
    S = SS
    if alpha4 is None:
        a = np.ones((H, W), bool)
    else:
        a = alpha4.reshape(H, S, W, S).mean(axis=(1, 3)) >= 0.5
    col4 = np.where(alpha4 if alpha4 is not None else True, col4, 0).astype(np.uint8)
    col = _mode_reduce(col4, S, len(NAMES))
    glow = _mode_reduce(glow4, S, len(NAMES))
    col = np.where(a, np.where(col == 0, N["F_B"], col), 0).astype(np.uint8)
    glow = np.where(a | (glow > 0), glow, 0).astype(np.uint8)
    if outline:
        ring = np.zeros((H, W), bool)
        if is_card:
            for _ in range(thick):
                ring |= a & ~_erode(a)
                a2 = a & ~ring
                a = a
        else:
            if "t" in edges: ring[:thick, :] = True
            if "b" in edges: ring[H - thick:, :] = True
            if "l" in edges: ring[:, :thick] = True
            if "r" in edges: ring[:, W - thick:] = True
        ring &= a
        col = np.where(ring, OUTL[col], col).astype(np.uint8)
        glow = np.where(ring & (glow > 0) & (glow != N["E_W"]), 0, glow).astype(np.uint8)
    return col, glow, a


def to_rgba(col, glow, a, glow_only_alpha=True):
    H, W = col.shape
    img = np.zeros((H, W, 4), np.uint8)
    img[..., :3] = RGB[col]
    img[..., 3] = np.where((col > 0) & a, 255, 0)
    g = np.zeros((H, W, 4), np.uint8)
    g[..., :3] = RGB[glow]
    g[..., 3] = np.where(glow > 0, 255, 0)
    return img, g
