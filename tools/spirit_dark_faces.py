"""The faces of the Spirit of Darkness (docs/magic/spirit_dark.md, section faces): seven moods drawn on the same face plates, so a mood costs no geometry and no quads, only a different small sheet.

    smug      half-lidded eyes, one brow cocked, a lopsided smirk with one fang
    grin      wide bright eyes, a big toothy grin
    angry     narrowed eyes under a hard V of brows, a snarl with bared fangs
    bored     heavy flat lids, low flat brows, a flat mouth
    surprised huge round pale eyes, high arched brows, a small round O
    sinister  thin burning slits, steep brows, a very wide thin smile full of fangs
    amused    eyes closed in laughing arcs, a wide open laughing mouth

The game draws the face as its own small model (spirit_dark_{powered,companion}_face: the two plates of the head, the same bones as the body so every clip moves it) with the colour sheet of the
mood and then the glow sheet of the mood additive. Colour sheets: textures/entity/spirit_dark_<form>_face_<mood>.png, glow sheets: ..._faceglow_<mood>.png. Deterministic.
"""
import math

import numpy as np

import spirit_dark_art as A

MOODS = ["smug", "grin", "angry", "bored", "surprised", "sinister", "amused"]
WHITE = np.array([232, 220, 244], np.float32)

# eye: lid 0 (open) .. 1 (shut), ry the half height of an open eye, slant the vertical tilt of the outer end (positive = outer end lower), kind: "eye" or "arc" (laughing, closed)
# brow: (left raise, right raise, tilt); tilt > 0 lowers the inner ends (angry); mouth: the kind drawn below
SPEC = {
    "smug":      dict(lid=0.50, ry=0.09, rx=0.17, slant=0.00, brows=(0.00, 0.06, 0.02), mouth="smirk", glow=(255, 70, 110), halo=1.0, kind="eye"),
    "grin":      dict(lid=0.00, ry=0.105, rx=0.17, slant=-0.10, brows=(0.04, 0.04, -0.06), mouth="grin", glow=(255, 120, 60), halo=1.2, kind="eye"),
    "angry":     dict(lid=0.30, ry=0.085, rx=0.18, slant=0.26, brows=(-0.03, -0.03, 0.30), mouth="snarl", glow=(255, 30, 60), halo=1.4, kind="eye"),
    "bored":     dict(lid=0.72, ry=0.09, rx=0.16, slant=0.00, brows=(-0.04, -0.04, 0.0), mouth="flat", glow=(200, 60, 100), halo=0.5, kind="eye"),
    "surprised": dict(lid=0.00, ry=0.17, rx=0.14, slant=0.0, brows=(0.09, 0.09, -0.08), mouth="oh", glow=(255, 170, 200), halo=1.3, kind="eye"),
    "sinister":  dict(lid=0.52, ry=0.07, rx=0.20, slant=0.20, brows=(0.00, 0.00, 0.36), mouth="wide", glow=(214, 150, 255), halo=1.5, kind="eye"),
    "amused":    dict(lid=0.00, ry=0.09, rx=0.16, slant=0.0, brows=(0.05, 0.05, -0.04), mouth="laugh", glow=(255, 190, 90), halo=1.0, kind="arc"),
}


def _grids(w, h):
    x, y = A.grid(w, h)
    return x / (w - 1), y / (h - 1)


def _set(c, g, mask, rgb=None, grgb=None, ga=255.0):
    if rgb is not None:
        c[mask, :3] = rgb
        c[mask, 3] = 255.0
    if grgb is not None:
        g[mask, :3] = grgb
        g[mask, 3] = ga


def _eye(c, g, u, v, cx, cy, sp, side, powered):
    """One eye at (cx, cy) in normalised tile coordinates: the dark socket, the burning iris, the lid, the arcs of a laugh."""
    rx, ry = sp["rx"], sp["ry"]
    # the slant tilts the whole eye: the outer end drops (angry/sinister point down toward the nose, smug flat)
    ex = (u - cx) / rx
    ey = (v - cy - side * (u - cx) * sp["slant"] * 1.5) / ry
    glow = np.array(sp["glow"], np.float32)
    if sp["kind"] == "arc":
        arc = np.abs(v - (cy + 0.05 - 0.10 * (1 - np.clip(ex * ex, 0, 1)))) < 0.028
        arc &= np.abs(ex) < 1.05
        _set(c, g, arc, A.INK, glow, 255.0)
        halo = (np.abs(v - (cy + 0.05 - 0.10 * (1 - np.clip(ex * ex, 0, 1)))) < 0.07) & (np.abs(ex) < 1.1) & ~arc
        a = A.smooth(1 - np.abs(v - (cy + 0.05 - 0.10 * (1 - np.clip(ex * ex, 0, 1)))) / 0.07) * 90 * sp["halo"]
        g[halo, :3] = A.GLOW_V
        g[halo, 3] = a[halo]
        return
    sock = ex * ex + ey * ey < 1.0
    if sp["lid"] > 0:
        lid_line = -1.0 + 2.0 * sp["lid"]                                  # the top of the visible eye in ey units
        sock &= ey > lid_line
    _set(c, g, sock, A.INK * 1.1)
    iris = (ex * ex * 0.9 + ey * ey * 1.5) < 0.66
    if sp["lid"] > 0:
        iris &= ey > -1.0 + 2.0 * sp["lid"] + 0.1
    _set(c, g, iris, None, glow, 255.0)
    hot = (ex * ex + ey * ey * 3.0) < 0.2
    if sp["lid"] > 0:
        hot &= ey > -1.0 + 2.0 * sp["lid"] + 0.2
    g[hot, :3] = np.array([255, 215, 230], np.float32)
    g[hot, 3] = 255.0
    halo = (ex * ex + ey * ey < 2.8) & ~sock
    a = A.smooth(1 - (ex * ex + ey * ey) / 2.8) * 80 * sp["halo"]
    g[halo, :3] = A.GLOW_V
    g[halo, 3] = np.maximum(g[halo, 3], a[halo])
    if sp["lid"] > 0:                                                      # the lid line: a thick ink stroke across the eye
        lid = (np.abs(ey - (-1.0 + 2.0 * sp["lid"])) < 0.22) & (np.abs(ex) < 1.12)
        c[lid, :3] = A.INK
        c[lid, 3] = 255.0
        g[lid, 3] = 0


def upper(w, h, mood, seed=21):
    """The powered form's upper face plate (eyes and brows) for a mood: colour and glow arrays."""
    sp = SPEC[mood]
    c, g = A.cel(w, h, A.SKIN, "front", seed, bands=(0.14, 0.86), hatch=0.0)
    u, v = _grids(w, h)
    for side in (-1, 1):
        cx = 0.5 + side * 0.24
        raise_ = sp["brows"][0 if side < 0 else 1]
        tilt = sp["brows"][2]
        # brow: a bar whose inner end is lowered by tilt
        yb = 0.27 - raise_ + tilt * 0.14 * (1.0 - np.clip(np.abs(u - 0.5) / 0.42, 0, 1))
        brow = (np.abs(v - yb) < 0.05) & (np.abs(u - cx) < 0.19)
        c[brow, :3] = A.INK
        shade = (v > yb + 0.04) & (v < yb + 0.12) & (np.abs(u - cx) < 0.19)
        c[shade, :3] = c[shade, :3] * 0.75
        _eye(c, g, u, v, cx, 0.58, sp, side, True)
    nose = (np.abs(u - 0.5) < 0.014) & (v > 0.5)
    c[nose, :3] = c[nose, :3] * 0.6
    A.outline(c, w, h, 2)
    return c, g


def lower(w, h, mood, seed=29):
    """The lower face plate (nose, mouth, chin) for a mood."""
    sp = SPEC[mood]
    c, g = A.cel(w, h, A.SKIN, "front", seed, bands=(0.1, 0.9), hatch=0.0)
    u, v = _grids(w, h)
    nose = ((u - 0.5) / 0.1) ** 2 + ((v - 0.12) / 0.16) ** 2 < 1.0
    c[nose, :3] = c[nose, :3] * 0.75
    x = (u - 0.5) * 2.0                                                     # -1 .. 1
    m = sp["mouth"]
    glowc = A.GLOW_V
    if m == "smirk":
        gy = 0.58 - 0.12 * x + 0.05 * (1 - x * x) * -1.0 * 0.0
        line = (np.abs(v - gy) < 0.05) & (np.abs(x) < 0.8)
        _set(c, g, line, A.INK)
        seam = (np.abs(v - gy) < 0.018) & (np.abs(x) < 0.74)
        _set(c, g, seam, None, glowc, 200)
        fang = (np.abs(x - 0.4) < 0.07) & (v > gy) & (v < gy + 0.2 - np.abs(x - 0.4) * 1.5)
        _set(c, g, fang, WHITE)
    elif m == "grin":
        top = 0.46 + 0.14 * (1 - x * x) * 0.0 - 0.05 * x * x
        lo = 0.62 + 0.20 * (1 - x * x)
        mouth = (v > top) & (v < lo) & (np.abs(x) < 0.82)
        _set(c, g, mouth, A.INK)
        g[mouth, :3] = np.array([120, 40, 160], np.float32)
        g[mouth, 3] = 90.0
        for tx in np.linspace(-0.66, 0.66, 8):
            tooth = (np.abs(x - tx) < 0.045) & (v > top) & (v < top + 0.1)
            _set(c, g, tooth, WHITE)
        for tx in (-0.5, 0.5):
            fang = (np.abs(x - tx) < 0.06) & (v > top) & (v < top + 0.2 - np.abs(x - tx) * 1.2)
            _set(c, g, fang, WHITE)
        rim = mouth & ((v < top + 0.025) | (v > lo - 0.03) | (np.abs(x) > 0.78))
        c[rim, :3] = A.INK
    elif m == "snarl":
        top = 0.50 + 0.10 * x * x
        lo = 0.70 + 0.04 * x * x
        mouth = (v > top) & (v < lo) & (np.abs(x) < 0.74)
        _set(c, g, mouth, A.INK)
        g[mouth, :3] = np.array([150, 30, 70], np.float32)
        g[mouth, 3] = 110.0
        for tx in np.linspace(-0.6, 0.6, 7):
            tooth = (np.abs(x - tx) < 0.05) & (v > top) & (v < top + 0.13)
            _set(c, g, tooth, WHITE)
        for tx in (-0.35, 0.35):
            fang = (np.abs(x - tx) < 0.06) & (v < lo) & (v > lo - 0.16 + np.abs(x - tx) * 1.2)
            _set(c, g, fang, WHITE)
    elif m == "flat":
        line = (np.abs(v - 0.62) < 0.04) & (np.abs(x) < 0.38)
        _set(c, g, line, A.INK)
        seam = (np.abs(v - 0.62) < 0.012) & (np.abs(x) < 0.34)
        _set(c, g, seam, None, glowc, 120)
    elif m == "oh":
        ell = (x / 0.22) ** 2 + ((v - 0.62) / 0.16) ** 2 < 1.0
        _set(c, g, ell, A.INK)
        inner = (x / 0.16) ** 2 + ((v - 0.64) / 0.11) ** 2 < 1.0
        g[inner, :3] = glowc
        g[inner, 3] = 120.0
    elif m == "wide":
        gy = 0.50 + 0.14 * (1 - x * x) * 0.0 + 0.16 * x * x * -1.0
        gy = 0.74 - 0.26 * (1 - x * x) * 0.0 - 0.0
        curve = 0.60 + 0.16 * (x * x) * -1.0 + 0.06
        line = (np.abs(v - (0.74 - 0.20 * (1 - x * x))) < 0.045) & (np.abs(x) < 0.97)
        _set(c, g, line, A.INK)
        seam = (np.abs(v - (0.74 - 0.20 * (1 - x * x))) < 0.015) & (np.abs(x) < 0.94)
        _set(c, g, seam, None, glowc, 230)
        for tx in np.linspace(-0.8, 0.8, 12):
            yy = 0.74 - 0.20 * (1 - tx * tx)
            fang = (np.abs(x - tx) < 0.028) & (v > yy) & (v < yy + 0.12)
            _set(c, g, fang, WHITE)
    elif m == "laugh":
        top = 0.48 - 0.0 * x
        lo = 0.58 + 0.30 * (1 - x * x)
        mouth = (v > top) & (v < lo) & (np.abs(x) < 0.84)
        _set(c, g, mouth, A.INK)
        tongue = mouth & (v > 0.7) & (np.abs(x) < 0.5)
        g[tongue, :3] = np.array([255, 100, 160], np.float32)
        g[tongue, 3] = 140.0
        for tx in (-0.55, -0.3, 0.3, 0.55):
            fang = (np.abs(x - tx) < 0.05) & (v > top) & (v < top + 0.18 - np.abs(x - tx) * 0.8)
            _set(c, g, fang, WHITE)
    chin = (np.abs(u - 0.5) < 0.012) & (v > 0.84)
    c[chin, :3] = c[chin, :3] * 0.7
    A.outline(c, w, h, 2)
    return c, g


def companion(w, h, mood, seed=71):
    """The wisp's face plate: a dark mask, burning violet eyes and a jagged mouth, for a mood (the sheet is the whole plate)."""
    sp = dict(SPEC[mood])
    sp["glow"] = (176, 92, 255) if mood not in ("angry", "surprised", "amused") else {"angry": (255, 60, 150), "surprised": (240, 220, 255), "amused": (255, 170, 230)}[mood]
    c, g = A.cel(w, h, np.array([20, 13, 34], np.float32), "front", seed, bands=(0.2, 0.85))
    u, v = _grids(w, h)
    for side in (-1, 1):
        cx = 0.5 + side * 0.22
        sp2 = dict(sp)
        sp2["rx"] = sp["rx"] * 0.85
        sp2["ry"] = sp["ry"] * 1.5
        _eye(c, g, u, v, cx, 0.40, sp2, side, False)
        yb = 0.14 - sp["brows"][0 if side < 0 else 1] * 0.5 + sp["brows"][2] * 0.10 * (1.0 - np.clip(np.abs(u - 0.5) / 0.4, 0, 1))
        brow = (np.abs(v - yb) < 0.035) & (np.abs(u - cx) < 0.15)
        g[brow, :3] = A.GLOW_V
        g[brow, 3] = 120.0
    x = (u - 0.5) * 2.0
    m = sp["mouth"]
    zig = 0.80 + 0.06 * np.abs(((u * 9) % 2) - 1)
    if m in ("smirk", "flat"):
        k = 0.12 if m == "smirk" else 0.0
        gy = 0.80 - k * x
        line = (np.abs(v - gy) < 0.05) & (np.abs(x) < (0.6 if m == "smirk" else 0.32))
        c[line, :3] = A.INK
        seam = (np.abs(v - gy) < 0.02) & (np.abs(x) < (0.56 if m == "smirk" else 0.28))
        g[seam, :3] = A.GLOW_V
        g[seam, 3] = 220.0
    elif m == "oh":
        ell = (x / 0.16) ** 2 + ((v - 0.80) / 0.13) ** 2 < 1.0
        c[ell, :3] = A.INK
        inner = (x / 0.1) ** 2 + ((v - 0.80) / 0.08) ** 2 < 1.0
        g[inner, :3] = A.GLOW_V
        g[inner, 3] = 200.0
    else:                                                                   # grin / snarl / wide / laugh: the jagged mouth, wider and deeper
        depth = {"grin": 0.07, "snarl": 0.05, "wide": 0.04, "laugh": 0.12}[m]
        wid = {"grin": 0.7, "snarl": 0.55, "wide": 0.86, "laugh": 0.6}[m]
        up = 0.76 - (depth if m != "snarl" else -0.02)
        mouth = (np.abs(x) < wid) & (v > up) & (v < up + depth * 2 + 0.06) if m != "wide" else (np.abs(x) < wid) & (np.abs(v - zig) < 0.05)
        c[mouth, :3] = A.INK
        seam = (np.abs(x) < wid - 0.04) & (np.abs(v - zig) < 0.02) if m == "wide" else mouth & ((np.abs(((u * 11) % 2) - 1) < 0.5))
        g[seam, :3] = A.GLOW_V
        g[seam, 3] = 220.0
    A.outline(c, w, h, 2)
    return c, g
