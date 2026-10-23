#!/usr/bin/env python3
"""Textures of the Wind Spirit Dive v2 overlay (Yuno's coat, crown and wings), written to assets/nusmp/textures/entity/mode/mwv2_*.png.

    python tools/gen_mwv2_textures.py

The layout (which rectangle of which texture belongs to which mesh) is tools/mwv2_layout.json; ModeArmorWindV2Rules says the same numbers and
ModeArmorWindV2AssetTest checks both. Everything is painted from distance fields with numpy and written with Pillow, so it is reproducible:
  mwv2_coat.png  cel banded cloth (green bands with a dark outline and light edge highlights), gold trim, button line, back crest, wind stitching
  mwv2_fur.png   white-green fur with tuft silhouettes (alpha cut), banded shading and strands
  mwv2_glow.png  additive map over the coat: seams, trim, buttons, crest and stitching light up; a very faint green wash everywhere (the aura)
  mwv2_wing.png  the leaf wing: translucent mint gradient, veins, a bright leading edge (U across the leaf, V from the root to the tip)
  mwv2_fx.png    crown blades / ring / halo, cross ornament, ribbon, leaf, eye, hair tint
"""
import json
import math
import os

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "entity", "mode")
LAYOUT = json.load(open(os.path.join(HERE, "mwv2_layout.json")))

GOLD = np.array([236, 204, 100]) / 255.0
GOLD_D = np.array([150, 108, 40]) / 255.0
GOLD_L = np.array([255, 240, 170]) / 255.0
MINT_L = np.array([196, 255, 218]) / 255.0
WHITE = np.array([250, 255, 246]) / 255.0
GLOWC = np.array([150, 255, 205]) / 255.0
OUTLINE = np.array([8, 44, 30]) / 255.0


def rgb(*c):
    return np.array(c, float) / 255.0


# ------------------------------------------------------------------------------------------------ canvas and fields
class Cv:
    """A straight-alpha canvas (float 0..1)."""

    def __init__(self, w, h):
        self.w, self.h = w, h
        self.c = np.zeros((h, w, 3), np.float32)
        self.a = np.zeros((h, w), np.float32)

    def fill(self, col, alpha=1.0):
        self.c[:] = col
        self.a[:] = alpha

    def over(self, mask, col, alpha=1.0):
        m = np.clip(mask * alpha, 0, 1).astype(np.float32)
        col = np.asarray(col, np.float32)
        if col.ndim == 1:
            col = np.broadcast_to(col, self.c.shape)
        oa = m + self.a * (1 - m)
        safe = np.maximum(oa, 1e-6)[..., None]
        self.c = (col * m[..., None] + self.c * (self.a * (1 - m))[..., None]) / safe
        self.a = oa

    def paste_into(self, big, x0, y0):
        big.c[y0:y0 + self.h, x0:x0 + self.w] = self.c
        big.a[y0:y0 + self.h, x0:x0 + self.w] = self.a

    def clean(self):
        self.c[self.a <= 0.002] = 0
        return self

    def image(self):
        self.clean()
        out = np.concatenate([np.clip(self.c, 0, 1), self.a[..., None]], axis=2)
        return Image.fromarray((out * 255 + 0.5).astype(np.uint8), "RGBA")


def grid(w, h):
    y, x = np.mgrid[0:h, 0:w]
    return x.astype(np.float32) + 0.5, y.astype(np.float32) + 0.5


def wrapdx(X, cx, w):
    d = X - cx
    return d - w * np.round(d / w)


def aa(d, width=1.0):
    return np.clip(width * 0.5 + 0.5 - d, 0, 1)


def seg_d(X, Y, x0, y0, x1, y1, w=None):
    """Distance to a segment; with w the x distance wraps (a ring texture)."""
    vx, vy = x1 - x0, y1 - y0
    L2 = max(vx * vx + vy * vy, 1e-9)
    dx = X - x0 if w is None else wrapdx(X, x0, w)
    t = np.clip((dx * vx + (Y - y0) * vy) / L2, 0, 1)
    px, py = vx * t, vy * t
    return np.hypot(dx - px, (Y - y0) - py)


def poly_d(X, Y, pts, w=None):
    d = np.full(X.shape, 1e9, np.float32)
    for (a, b), (c, e) in zip(pts[:-1], pts[1:]):
        d = np.minimum(d, seg_d(X, Y, a, b, c, e, w))
    return d


def noise(w, h, cell, seed, wrapx=False):
    rng = np.random.RandomState(seed)
    gw, gh = max(2, int(math.ceil(w / cell)) + 1), max(2, int(math.ceil(h / cell)) + 1)
    g = rng.rand(gh, gw).astype(np.float32)
    if wrapx:
        g[:, -1] = g[:, 0]
    im = Image.fromarray(g, "F").resize((w, h), Image.BICUBIC)
    return np.clip(np.asarray(im, np.float32), 0, 1)


def spiral(cx, cy, r0, r1, turns, a0=0.0, n=40, flip=1):
    pts = []
    for i in range(n + 1):
        t = i / n
        r = r0 + (r1 - r0) * t
        a = a0 + flip * turns * 2 * math.pi * t
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return pts


def lerp_pal(pal, s):
    """Quantise s (0..1) into the bands of a palette: returns (rgb, band index)."""
    n = len(pal)
    idx = np.clip((s * n).astype(int), 0, n - 1)
    return np.asarray(pal, np.float32)[idx], idx


# ------------------------------------------------------------------------------------------------ cloth
PAL_COAT = [rgb(16, 74, 50), rgb(28, 118, 76), rgb(44, 158, 98), rgb(92, 208, 142)]
PAL_COAT_LIGHT = [rgb(30, 110, 74), rgb(52, 152, 100), rgb(88, 196, 134), rgb(150, 236, 180)]
PAL_TROUSER = [rgb(12, 52, 46), rgb(20, 84, 70), rgb(32, 116, 92), rgb(58, 160, 124)]
PAL_BOOT = [rgb(22, 62, 48), rgb(36, 96, 70), rgb(54, 132, 92), rgb(88, 176, 124)]


def cloth(w, h, folds, seed, pal=PAL_COAT, wrapx=True, fade=0.18, phase=0.0, amp=0.23):
    """Cel banded cloth: soft folds quantised to four bands, a light edge on the lit side of every band step, a dark one on the shadow side."""
    X, Y = grid(w, h)
    n = noise(w, h, 9, seed, wrapx)
    warp = np.sin(2 * np.pi * (Y / h * 0.9) + phase)
    s = 0.56 + amp * np.sin(2 * np.pi * (folds * X / w + 0.035 * warp + 0.03 * (n - 0.5))) - fade * (Y / h) + 0.05 * (n - 0.5)
    s = np.clip(s, 0, 0.999)
    col, band = lerp_pal(pal, s)
    prev = np.roll(band, 1, axis=1)
    edge = band != prev
    if not wrapx:
        edge[:, 0] = False
    lighter = edge & (band > prev)
    darker = edge & (band < prev)
    col = col.copy()
    col[lighter] = np.clip(col[lighter] * 1.18 + 0.05, 0, 1)
    col[darker] = col[darker] * 0.78
    # a fine cloth weave, barely there
    weave = ((np.floor(X) + np.floor(Y)) % 2) * 0.025
    col = np.clip(col + weave[..., None] - 0.012, 0, 1)
    return col, band


def outline_edges(cv, top=True, bottom=True, left=False, right=False, col=OUTLINE):
    m = np.zeros((cv.h, cv.w), np.float32)
    if top: m[0, :] = 1
    if bottom: m[-1, :] = 1
    if left: m[:, 0] = 1
    if right: m[:, -1] = 1
    cv.over(m, col)


def new_cloth(w, h, folds, seed, pal=PAL_COAT, wrapx=True, **kw):
    cv = Cv(w, h)
    c, _ = cloth(w, h, folds, seed, pal, wrapx, **kw)
    cv.c[:] = c
    cv.a[:] = 1
    return cv


class Pair:
    """A region painted on the coat canvas and on the glow canvas at once."""

    def __init__(self, w, h, folds, seed, pal=PAL_COAT, wrapx=True, **kw):
        self.w, self.h = w, h
        self.cv = new_cloth(w, h, folds, seed, pal, wrapx, **kw)
        self.gl = Cv(w, h)
        self.X, self.Y = grid(w, h)
        self.wash(0.10)

    def wash(self, a):
        self.gl.over(np.ones((self.h, self.w), np.float32), rgb(110, 255, 190), a)

    def both(self, mask, col, gcol=GLOWC, a=1.0, ga=0.8):
        self.cv.over(mask, col, a)
        if ga > 0:
            self.gl.over(mask, gcol, ga)

    def only_glow(self, mask, gcol=GLOWC, ga=0.8):
        self.gl.over(mask, gcol, ga)

    def only_coat(self, mask, col, a=1.0):
        self.cv.over(mask, col, a)


# ------------------------------------------------------------------------------------------------ the coat atlas
def paint_torso(R):
    w, h = R[2] - R[0], R[3] - R[1]
    P = Pair(w, h, 5, 11, PAL_COAT, True, fade=0.16)
    X, Y = P.X, P.Y
    fx = wrapdx(X, w / 2, w)                                    # distance from the front centre
    bx = wrapdx(X, 0.0, w)                                      # distance from the back centre
    # the side seams
    for sx in (w * 0.25, w * 0.75):
        d = np.abs(wrapdx(X, sx, w))
        P.only_coat(aa(d, 1.3), OUTLINE)
        P.only_coat(aa(np.abs(d - 1.6), 1.0), MINT_L, 0.45)
        P.only_glow(aa(d, 1.0), GLOWC, 0.55)
    # the placket and its gold lines
    P.only_coat(np.where(np.abs(fx) < 5.6, 1.0, 0.0), rgb(18, 82, 56))
    P.both(aa(np.abs(np.abs(fx) - 6.4), 1.2), GOLD, GLOWC, 1.0, 0.8)
    P.both(aa(np.abs(np.abs(fx) - 9.0), 0.8), GOLD_D, GLOWC, 0.7, 0.35)
    P.only_coat(aa(np.abs(fx), 0.8), OUTLINE, 0.8)
    # the lapels: from the collar down to the placket
    for s in (-1, 1):
        d = seg_d(X, Y, w / 2 + s * 27, 0, w / 2 + s * 8.5, 24)
        P.both(aa(d, 1.5), GOLD_L, GLOWC, 0.95, 0.85)
        P.only_coat(aa(seg_d(X, Y, w / 2 + s * 25.2, 1.8, w / 2 + s * 7.8, 25.6), 1.2), OUTLINE, 0.55)
    # buttons
    for row in (17, 26, 35, 44):
        d = np.hypot(fx, Y - row)
        P.only_coat(aa(d - 2.3, 1.0), OUTLINE)
        P.both(aa(d - 1.8, 1.0), GOLD, GLOWC, 1.0, 0.0)
        P.only_coat(aa(np.hypot(fx + 0.6, Y - row + 0.6) - 0.8, 1.0), GOLD_L)
        P.only_glow(aa(d - 3.1, 1.0), GLOWC, 0.55)
    # the hem band
    hem = Y > h - 8
    P.only_coat(np.where(hem, 1.0, 0.0), rgb(20, 90, 60))
    P.both(aa(np.abs(Y - (h - 5.5)), 1.4), GOLD, GLOWC, 1.0, 0.8)
    P.only_coat(aa(np.abs(Y - (h - 7.8)), 1.0), OUTLINE, 0.8)
    # the belt line (covered by the sash, painted so the seam below it reads)
    # the crest on the back: a ring, a cross with arrow ends and four leaf swirls
    cy = 28.0
    dr = np.hypot(bx, Y - cy)
    P.both(aa(np.abs(dr - 11.0), 1.5), GOLD_L, GLOWC, 0.95, 0.9)
    P.both(aa(np.abs(dr - 8.4), 0.8), GOLD_D, GLOWC, 0.7, 0.35)
    P.both(aa(seg_d(X, Y, 0, cy - 18, 0, cy + 18, w), 1.6), MINT_L, WHITE, 0.95, 0.9)
    P.both(aa(seg_d(X, Y, -9, cy - 3, 9, cy - 3, w), 1.4), MINT_L, WHITE, 0.95, 0.9)
    for dy in (-18, 18):
        for k in range(2):
            ax = (-1) ** k
            P.both(aa(seg_d(X, Y, 0, cy + dy, ax * 3.0, cy + dy - np.sign(dy) * 4.5, w), 1.3), MINT_L, WHITE, 0.9, 0.85)
    for k in range(4):
        a0 = math.pi / 4 + k * math.pi / 2
        pts = spiral(11.5 * math.cos(a0), cy + 11.5 * math.sin(a0), 0.4, 3.4, 1.1, a0, 20, 1)
        P.both(aa(poly_d(X, Y, pts, w), 1.0), MINT_L, GLOWC, 0.85, 0.75)
    P.both(aa(np.abs(bx), 0.9) * np.where((Y > 3) & (Y < h - 9) & (((Y // 3) % 2) == 0), 1.0, 0.0), GOLD_D, GLOWC, 0.75, 0.5)   # stitched back seam
    # wind stitching: loose curls on the skirt and the chest panels
    for cx, cy2, r, fl in ((w / 2 - 31, 44, 7.5, 1), (w / 2 + 31, 44, 7.5, -1), (w / 2 - 52, 30, 6.5, -1), (w / 2 + 52, 30, 6.5, 1), (22, 46, 6.5, 1), (w - 22, 46, 6.5, -1)):
        pts = spiral(cx, cy2, 0.6, r, 1.6, 0.0, 44, fl)
        P.both(aa(poly_d(X, Y, pts, w), 1.0), rgb(166, 240, 196), GLOWC, 0.55, 0.5)
        pts2 = [(cx + r * 1.15 * fl, cy2 + 1.5), (cx + r * 2.6 * fl, cy2 - 3.0), (cx + r * 4.0 * fl, cy2 + 0.8)]
        P.both(aa(poly_d(X, Y, pts2, w), 0.9), rgb(166, 240, 196), GLOWC, 0.45, 0.4)
    # a faint stronger wash toward the hem
    P.gl.over((Y / h) ** 2 * np.ones_like(X), rgb(110, 255, 190), 0.10)
    outline_edges(P.cv, True, True)
    return P


def paint_tail(R, back):
    w, h = R[2] - R[0], R[3] - R[1]
    P = Pair(w, h, 2.4 if back else 1.8, 21 if back else 22, PAL_COAT, False, fade=0.12, phase=1.0 if back else 2.0)
    X, Y = P.X, P.Y
    # inner edge (the slit): gold binding; outer edge: dark outline
    P.both(aa(np.abs(X - 1.5), 1.4), GOLD, GLOWC, 1.0, 0.8)
    P.only_coat(aa(np.abs(X - 3.2), 1.0), OUTLINE, 0.6)
    outline_edges(P.cv, False, False, False, True)
    # the hem: a darker band with a gold line and a fringe of light dashes
    hem = Y > h - 9
    P.only_coat(np.where(hem, 1.0, 0.0), rgb(20, 90, 60))
    P.both(aa(np.abs(Y - (h - 6.0)), 1.4), GOLD, GLOWC, 1.0, 0.85)
    P.only_coat(aa(np.abs(Y - (h - 8.6)), 1.0), OUTLINE, 0.7)
    P.only_coat(aa(np.abs(Y - (h - 1.0)), 1.2), OUTLINE)
    # a fold line down the middle and the stitched curl
    P.only_coat(aa(np.abs(X - w * 0.55 - 1.5 * np.sin(Y / h * 5)), 0.9), OUTLINE, 0.45)
    P.only_glow(aa(np.abs(X - w * 0.55 - 1.5 * np.sin(Y / h * 5)), 0.9), GLOWC, 0.3)
    pts = spiral(w * 0.52, h * 0.44, 0.5, 6.4, 1.6, 0.4, 40, 1 if back else -1)
    P.both(aa(poly_d(X, Y, pts), 1.0), rgb(166, 240, 196), GLOWC, 0.6, 0.55)
    if not back:
        for row in (7, 16):
            d = np.hypot(X - 5.0, Y - row)
            P.only_coat(aa(d - 2.0, 1.0), OUTLINE)
            P.both(aa(d - 1.5, 1.0), GOLD, GLOWC, 1.0, 0.0)
            P.only_glow(aa(d - 2.8, 1.0), GLOWC, 0.5)
    P.gl.over((Y / h) ** 2 * np.ones_like(X), rgb(110, 255, 190), 0.05)
    return P


def paint_sleeve(R):
    w, h = R[2] - R[0], R[3] - R[1]
    P = Pair(w, h, 4, 31, PAL_COAT, True, fade=0.14)
    X, Y = P.X, P.Y
    P.only_coat(aa(np.abs(wrapdx(X, 0.0, w)), 1.2), OUTLINE, 0.9)                 # the seam on the back of the arm
    P.only_glow(aa(np.abs(wrapdx(X, 0.0, w)), 1.0), GLOWC, 0.5)
    P.both(aa(np.abs(Y - 26.0), 1.6), GOLD, GLOWC, 1.0, 0.85)                       # the band above the cuff
    P.only_coat(aa(np.abs(Y - 28.4), 1.0), OUTLINE, 0.8)
    P.only_coat(aa(np.abs(Y - 23.6), 1.0), OUTLINE, 0.8)
    P.both(aa(np.abs(Y - 3.5), 1.0), GOLD_D, GLOWC, 0.8, 0.4)                       # the shoulder stitch
    for cx, cy, fl in ((16, 13, 1), (48, 16, -1)):
        pts = spiral(cx, cy, 0.4, 5.2, 1.5, 0.3, 36, fl)
        P.both(aa(poly_d(X, Y, pts, w), 1.0), rgb(166, 240, 196), GLOWC, 0.55, 0.5)
    outline_edges(P.cv, True, True)
    return P


def paint_mantle(R):
    w, h = R[2] - R[0], R[3] - R[1]
    P = Pair(w, h, 6, 41, PAL_COAT_LIGHT, True, fade=0.1)
    X, Y = P.X, P.Y
    for y in (7.5, 14.5):
        P.both(aa(np.abs(Y - y), 1.1), GOLD_D, GLOWC, 0.9, 0.45)
    for k in range(8):
        P.only_coat(aa(np.abs(wrapdx(X, k * w / 8.0, w)), 1.0) * np.where(Y > 0, 1.0, 0.0), OUTLINE, 0.55)
    P.both(aa(np.abs(Y - 20.0), 1.8), GOLD, GLOWC, 1.0, 0.85)
    P.only_coat(aa(np.abs(Y - 22.4), 1.0), OUTLINE, 0.9)
    # a small cross on the outer face
    cx = w * 0.25
    P.both(aa(seg_d(X, Y, cx, 2.5, cx, 11.5, w), 1.3), GOLD_L, WHITE, 1.0, 0.9)
    P.both(aa(seg_d(X, Y, cx - 3.2, 5.4, cx + 3.2, 5.4, w), 1.3), GOLD_L, WHITE, 1.0, 0.9)
    outline_edges(P.cv, True, False)
    return P


def box_edges(P, w, face_w, a=0.9):
    """The chunky corner lines of a box atlas: a dark line on every face border and a light line just inside it (the cel look of the classic box, in the v2 palette)."""
    X = P.X
    for k in range(w // face_w):
        P.only_coat(aa(np.abs(wrapdx(X, k * face_w, w)), 1.6), OUTLINE, a)
        P.only_coat(aa(np.abs(wrapdx(X, k * face_w + 2.0, w)), 1.0), MINT_L, 0.30)
        P.only_glow(aa(np.abs(wrapdx(X, k * face_w, w)), 1.0), GLOWC, 0.35)


def paint_sleeve_box(R):
    """The BLOCKY sleeve (the classic's box arm, 4 faces of 20 x 36: front, outer, back, inner), cel banded cloth with the gold band above the fur cuff, shoulder stitch, seam, buttons and a wind curl."""
    w, h = R[2] - R[0], R[3] - R[1]
    fw = w // 4
    P = Pair(w, h, 6, 57, PAL_COAT, True, fade=0.14)
    X, Y = P.X, P.Y
    P.both(aa(np.abs(Y - 30.0), 2.0), GOLD, GLOWC, 1.0, 0.85)                      # the band above the cuff
    P.only_coat(aa(np.abs(Y - 32.4), 1.0), OUTLINE, 0.8)
    P.only_coat(aa(np.abs(Y - 27.6), 1.0), OUTLINE, 0.8)
    P.both(aa(np.abs(Y - 7.0), 1.0), GOLD_D, GLOWC, 0.8, 0.4)                      # the shoulder stitch
    P.only_coat(aa(np.abs(wrapdx(X, 2.5 * fw, w)), 1.2), OUTLINE, 0.9)             # the seam up the back of the arm
    P.only_glow(aa(np.abs(wrapdx(X, 2.5 * fw, w)), 1.0), GLOWC, 0.5)
    for cy in (13.0, 19.0):                                                        # two gold buttons on the front face
        d = np.hypot(wrapdx(X, 0.5 * fw, w), Y - cy)
        P.only_coat(aa(d - 2.0, 1.0), OUTLINE)
        P.both(aa(d - 1.5, 1.0), GOLD, GLOWC, 1.0, 0.0)
        P.only_glow(aa(d - 2.8, 1.0), GLOWC, 0.5)
    for cx, cy, fl in ((1.5 * fw, 15.0, 1), (3.5 * fw, 15.0, -1)):                # a wind curl on the outer and the inner face
        pts = spiral(cx, cy, 0.4, 5.6, 1.5, 0.3, 36, fl)
        P.both(aa(poly_d(X, Y, pts, w), 1.0), rgb(166, 240, 196), GLOWC, 0.6, 0.55)
    box_edges(P, w, fw)
    outline_edges(P.cv, True, False)
    return P


def paint_sleeve_cap(R):
    """The top of the blocky sleeve (20 x 20): a stitched seam, a gold ring and a small wind curl."""
    w, h = R[2] - R[0], R[3] - R[1]
    P = Pair(w, h, 3, 63, PAL_COAT_LIGHT, True, fade=0.05)
    X, Y = P.X, P.Y
    d = np.hypot(X - w / 2, Y - h / 2)
    P.both(aa(np.abs(d - 6.4), 1.2), GOLD_D, GLOWC, 0.85, 0.45)
    pts = spiral(w / 2, h / 2, 0.4, 4.6, 1.4, 0.2, 30, 1)
    P.both(aa(poly_d(X, Y, pts), 1.0), rgb(166, 240, 196), GLOWC, 0.6, 0.55)
    for m in (np.abs(X - 0.5), np.abs(X - w + 0.5), np.abs(Y - 0.5), np.abs(Y - h + 0.5)):
        P.only_coat(aa(m, 1.6), OUTLINE, 0.95)
    return P


def paint_mantle_box(R):
    """The BLOCKY shoulder mantle (the pauldron over the sleeve: 4 faces of 20 x 12): lighter cloth, a gold line and a small gold cross on the outer face."""
    w, h = R[2] - R[0], R[3] - R[1]
    fw = w // 4
    P = Pair(w, h, 5, 43, PAL_COAT_LIGHT, True, fade=0.08)
    X, Y = P.X, P.Y
    P.both(aa(np.abs(Y - 8.4), 1.4), GOLD, GLOWC, 1.0, 0.85)
    P.only_coat(aa(np.abs(Y - 10.2), 1.0), OUTLINE, 0.9)
    cx = 1.5 * fw                                                                  # the outer face
    P.both(aa(seg_d(X, Y, cx, 1.6, cx, 7.0, w), 1.3), GOLD_L, WHITE, 1.0, 0.9)
    P.both(aa(seg_d(X, Y, cx - 2.8, 3.4, cx + 2.8, 3.4, w), 1.3), GOLD_L, WHITE, 1.0, 0.9)
    box_edges(P, w, fw, 0.8)
    outline_edges(P.cv, True, False)
    return P


def paint_mantle_cap(R):
    """The top of the blocky mantle (20 x 20): a gold ring round a mint star, the crest of the shoulder."""
    w, h = R[2] - R[0], R[3] - R[1]
    P = Pair(w, h, 3, 71, PAL_COAT_LIGHT, True, fade=0.04)
    X, Y = P.X, P.Y
    d = np.hypot(X - w / 2, Y - h / 2)
    P.both(aa(np.abs(d - 7.4), 1.4), GOLD, GLOWC, 1.0, 0.85)
    P.both(aa(seg_d(X, Y, w / 2, h / 2 - 4.6, w / 2, h / 2 + 4.6), 1.3), MINT_L, WHITE, 0.95, 0.9)
    P.both(aa(seg_d(X, Y, w / 2 - 4.6, h / 2, w / 2 + 4.6, h / 2), 1.3), MINT_L, WHITE, 0.95, 0.9)
    for m in (np.abs(X - 0.5), np.abs(X - w + 0.5), np.abs(Y - 0.5), np.abs(Y - h + 0.5)):
        P.only_coat(aa(m, 1.6), OUTLINE, 0.95)
    return P


def paint_trouser(R):
    w, h = R[2] - R[0], R[3] - R[1]
    P = Pair(w, h, 3, 51, PAL_TROUSER, True, fade=0.1)
    X, Y = P.X, P.Y
    for sx in (w * 0.25, w * 0.75):
        P.only_coat(aa(np.abs(wrapdx(X, sx, w)), 1.2), OUTLINE, 0.8)
        P.only_glow(aa(np.abs(wrapdx(X, sx, w)), 1.0), GLOWC, 0.35)
    P.both(aa(np.abs(Y - 2.5), 1.0), GOLD_D, GLOWC, 0.8, 0.4)
    outline_edges(P.cv, True, True)
    P.wash(0.04)
    return P


def paint_boot(R):
    w, h = R[2] - R[0], R[3] - R[1]
    P = Pair(w, h, 3, 61, PAL_BOOT, True, fade=0.12)
    X, Y = P.X, P.Y
    fx = wrapdx(X, w / 2, w)
    for y in (6.5,):
        P.both(aa(np.abs(Y - y), 2.0), GOLD_D, GLOWC, 1.0, 0.5)
    P.both(np.where((np.abs(fx) < 3.0) & (np.abs(Y - 6.5) < 2.4), 1.0, 0.0), GOLD, GLOWC, 1.0, 0.6)           # the buckle
    P.only_coat(aa(np.abs(Y - 16.0), 1.0), OUTLINE, 0.8)                                                      # the toe cap
    P.only_coat(aa(np.abs(Y - 17.4), 1.0), rgb(90, 170, 120), 0.5)
    outline_edges(P.cv, True, True)
    P.wash(0.04)
    return P


def paint_sash(R):
    w, h = R[2] - R[0], R[3] - R[1]
    cv = Cv(w, h)
    X, Y = grid(w, h)
    per = 10.0
    t = ((X + Y * 1.2) % per) / per
    band = np.where(t < 0.5, 0, 1)
    pal = [rgb(86, 160, 96), rgb(150, 206, 112)]
    cv.fill(0)
    cv.c[:] = np.asarray(pal, np.float32)[band]
    cv.a[:] = 1
    cv.over(aa(np.abs(((X + Y * 1.2) % per) - 0.0) , 1.0) + aa(np.abs(((X + Y * 1.2) % per) - per), 1.0), OUTLINE, 0.5)
    cv.over(aa(np.abs(((X + Y * 1.2) % per) - per * 0.5), 1.0), GOLD_L, 0.55)
    cv.over(np.where((Y < 1.0) | (Y > h - 1.0), 1.0, 0.0), OUTLINE)
    cv.over(np.where((Y > 1.0) & (Y < 2.2), 1.0, 0.0), GOLD, 0.8)
    cv.over(np.where((Y > h - 2.2) & (Y < h - 1.0), 1.0, 0.0), GOLD, 0.8)
    gl = Cv(w, h)
    gl.over(np.where((Y > 1.0) & (Y < 2.2), 1.0, 0.0) + np.where((Y > h - 2.2) & (Y < h - 1.0), 1.0, 0.0), GLOWC, 0.7)
    gl.over(np.ones_like(X), rgb(110, 255, 190), 0.08)
    return cv, gl


def paint_buckle(R):
    w, h = R[2] - R[0], R[3] - R[1]
    cv, gl = Cv(w, h), Cv(w, h)
    X, Y = grid(w, h)
    cv.over(np.ones_like(X), GOLD, 1.0)
    cv.over((Y / h) * np.ones_like(X), GOLD_D, 0.6)
    cv.over(np.where((X < 1.2) | (X > w - 1.2) | (Y < 1.2) | (Y > h - 1.2), 1.0, 0.0), OUTLINE)
    cv.over(aa(np.hypot(X - w / 2, Y - h / 2) - 2.0, 1.0), rgb(110, 240, 180))
    cv.over(aa(np.hypot(X - w / 2 + 0.7, Y - h / 2 + 0.7) - 0.8, 1.0), WHITE)
    gl.over(aa(np.hypot(X - w / 2, Y - h / 2) - 2.6, 1.0), GLOWC, 0.9)
    return cv, gl


def paint_cord(R):
    w, h = R[2] - R[0], R[3] - R[1]
    cv, gl = Cv(w, h), Cv(w, h)
    X, Y = grid(w, h)
    t = ((Y * 1.0 + X * 0.9) % 6.0) / 6.0
    cv.c[:] = np.asarray([rgb(78, 150, 92), rgb(146, 204, 110)], np.float32)[np.where(t < 0.5, 0, 1)]
    cv.a[:] = 1
    cv.over(np.where((X < 1.0) | (X > w - 1.0), 1.0, 0.0), OUTLINE)
    gl.over(np.where((X < 3) & (X > 1), 1.0, 0.0) * 0.0 + 0.0, GLOWC, 0.0)
    gl.over(np.ones_like(X), rgb(110, 255, 190), 0.07)
    return cv, gl


def paint_tassel(R):
    w, h = R[2] - R[0], R[3] - R[1]
    cv, gl = Cv(w, h), Cv(w, h)
    X, Y = grid(w, h)
    s = 0.5 + 0.5 * np.sin(X * 1.9)
    cv.c[:] = np.asarray([GOLD_D, GOLD, GOLD_L], np.float32)[np.clip((s * 3).astype(int), 0, 2)]
    cv.a[:] = 1
    cv.over(np.where((Y > h - 1.5), 1.0, 0.0), OUTLINE, 0.6)
    gl.over(np.ones_like(X), GOLD_L, 0.25)
    return cv, gl


def build_coat():
    reg = LAYOUT["coat"]["regions"]
    W, H = LAYOUT["coat"]["size"]
    coat, glow = Cv(W, H), Cv(W, H)
    for name, fn in (("TORSO", paint_torso), ("SLEEVE", paint_sleeve), ("MANTLE", paint_mantle), ("TROUSER", paint_trouser), ("BOOT", paint_boot),
                     ("SLEEVE_BOX", paint_sleeve_box), ("SLEEVE_CAP", paint_sleeve_cap), ("MANTLE_BOX", paint_mantle_box), ("MANTLE_CAP", paint_mantle_cap)):
        R = reg[name]
        P = fn(R)
        P.cv.paste_into(coat, R[0], R[1]); P.gl.paste_into(glow, R[0], R[1])
    for name, back in (("TAIL_BACK", True), ("TAIL_FRONT", False)):
        R = reg[name]
        P = paint_tail(R, back)
        P.cv.paste_into(coat, R[0], R[1]); P.gl.paste_into(glow, R[0], R[1])
    for name, fn in (("SASH", paint_sash), ("BUCKLE", paint_buckle), ("CORD", paint_cord), ("TASSEL", paint_tassel)):
        R = reg[name]
        cv, gl = fn(R)
        cv.paste_into(coat, R[0], R[1]); gl.paste_into(glow, R[0], R[1])
    return coat, glow


# ------------------------------------------------------------------------------------------------ fur
FUR_PAL = [rgb(150, 196, 168), rgb(196, 230, 208), rgb(232, 250, 238), rgb(252, 255, 250)]
FUR_PAL_IN = [rgb(100, 150, 124), rgb(140, 188, 160), rgb(180, 224, 200), rgb(216, 244, 228)]


def fur_fill(w, h, seed, pal, tufts=None, tuft_h=12):
    """Fur: banded shading by depth, short strands, and (for the outer wall) tufts along the top edge as the silhouette (alpha cut)."""
    cv = Cv(w, h)
    X, Y = grid(w, h)
    n1 = noise(w, h, 6, seed, True)
    n2 = noise(w, h, 2.5, seed + 7, True)
    depth = Y / h
    s = 0.86 - 0.34 * depth + 0.30 * (n1 - 0.5) + 0.08 * (n2 - 0.5)
    # strands: thin dark strokes running down the fur, slightly slanted
    st = np.sin((X * 1.0 + Y * 0.30) * 2 * np.pi / 5.0 + n1 * 5.0)
    s = s - 0.20 * (st > 0.93)
    col, band = lerp_pal(pal, np.clip(s, 0, 0.999))
    cv.c[:] = col
    cv.a[:] = 1
    if tufts:
        rng = np.random.RandomState(seed)
        n = tufts
        top = np.full((w,), float(h), np.float32)
        for i in range(n):
            cx = (i + 0.5) * w / n + rng.uniform(-1.2, 1.2)
            hh = rng.uniform(0.55, 1.0) * tuft_h
            hw = w / n * rng.uniform(0.62, 0.9)
            xs = np.arange(w) + 0.5
            dx = np.abs(wrapdx(xs, cx, w))
            prof = np.where(dx < hw, tuft_h - hh * np.clip(1 - dx / hw, 0, 1) ** 0.85, h + 99)
            top = np.minimum(top, prof)
        top = np.minimum(top, tuft_h)               # the solid fur starts at the tuft line
        edge = Y - top[None, :]
        cv.a[:] = np.where(edge >= 0, 1.0, 0.0)
        # lighter tips, darker valleys
        tip = np.clip(1 - edge / 3.0, 0, 1) * (edge >= 0)
        cv.c = np.clip(cv.c + tip[..., None] * 0.20, 0, 1)
        # a fuzzy bite out of the silhouette
        rr = np.random.RandomState(seed + 3).rand(h, w)
        cv.a[(edge >= 0) & (edge < 1.2) & (rr < 0.12)] = 0
        cv.c[cv.a <= 0.01] = 0
    return cv


def build_fur():
    W, H = LAYOUT["fur"]["size"]
    reg = LAYOUT["fur"]["regions"]
    big = Cv(W, H)
    R = reg["OUTER"]
    fur_fill(R[2] - R[0], R[3] - R[1], 5, FUR_PAL, tufts=16, tuft_h=12).paste_into(big, R[0], R[1])
    R = reg["LIP"]
    fur_fill(R[2] - R[0], R[3] - R[1], 9, FUR_PAL).paste_into(big, R[0], R[1])
    R = reg["INNER"]
    fur_fill(R[2] - R[0], R[3] - R[1], 13, FUR_PAL_IN).paste_into(big, R[0], R[1])
    return big


# ------------------------------------------------------------------------------------------------ the wing
def build_wing():
    W, H = LAYOUT["wing"]["size"]
    cv = Cv(W, H)
    X, Y = grid(W, H)
    a = X / W                      # across: 0 leading edge .. 1 trailing edge
    v = Y / H                      # along: 0 root .. 1 tip
    n = noise(W, H, 16, 71)
    n2 = noise(W, H, 5, 72)
    # the body of the leaf: pale mint-white at the leading edge, mint in the middle, teal-green toward the trailing edge
    t = np.clip(a * 1.15, 0, 1)
    c_lead, c_mid, c_trail = rgb(246, 255, 250), rgb(196, 252, 220), rgb(136, 238, 190)
    col = np.where(t[..., None] < 0.5, c_lead + (c_mid - c_lead) * (t[..., None] / 0.5), c_mid + (c_trail - c_mid) * ((t[..., None] - 0.5) / 0.5))
    # long soft streaks of wind along the leaf
    streak = 0.5 + 0.5 * np.sin((a * 26 + n * 4.0 + v * 3.0) * 1.0)
    col = np.clip(col + (streak[..., None] - 0.5) * 0.08 + (n2[..., None] - 0.5) * 0.06, 0, 1)
    alpha = (0.97 - 0.27 * np.clip((a - 0.1) / 0.9, 0, 1)) * (0.9 + 0.1 * (1 - v)) + 0.04 * (n - 0.5)
    cv.c = col.astype(np.float32)
    cv.a = np.clip(alpha, 0, 1).astype(np.float32)
    # the midrib and the side veins (they curve toward the tip)
    mid = 0.40 + 0.03 * np.sin(v * 5.0)
    veins = np.zeros_like(a)
    vein_core = np.zeros_like(a)
    d = np.abs(a - mid) * W
    rib = aa(d, 2.4 - 1.4 * v)
    vein_core = np.maximum(vein_core, aa(d, 0.9 * (1 - v) + 0.4))
    for i in range(16):
        v0 = 0.04 + i * 0.058
        for side, a_end in ((-1, 0.035), (1, 0.97)):
            pts = []
            for k in range(26):
                tt = k / 25.0
                aa_ = (0.40 + 0.03 * math.sin(v0 * 5.0)) + (a_end - 0.40) * (tt ** 0.9)
                vv = v0 + (0.19 - 0.05 * (1 - v0)) * (tt ** 1.15)
                pts.append((aa_ * W, vv * H))
            dd = poly_d(X, Y, pts)
            fadeout = 1.0 - 0.35 * v0
            veins = np.maximum(veins, aa(dd, 1.3) * fadeout)
            vein_core = np.maximum(vein_core, aa(dd, 0.6) * fadeout)
    vein_col = rgb(246, 255, 244)
    cv.over(rib, rgb(120, 220, 170), 0.5)
    cv.over(veins, rgb(226, 255, 238), 0.78)
    cv.over(vein_core, vein_col, 0.9)
    cv.over(rib, vein_col, 0.55)
    # a dark teal line beside the leading edge gives the cel outline, then the bright edge itself
    cv.over(aa(np.abs(a * W - 4.4), 1.4), rgb(60, 176, 132), 0.85)
    cv.over(np.where(a * W < 3.6, 1.0, 0.0), rgb(255, 255, 250), 1.0)
    cv.over(aa(np.abs(a * W - 0.5), 1.2), rgb(255, 255, 255), 1.0)
    # the trailing edge: a thin bright line and a darker teal one inside
    cv.over(aa(np.abs((1 - a) * W - 4.0), 1.2), rgb(70, 184, 140), 0.7)
    cv.over(np.where((1 - a) * W < 2.4, 1.0, 0.0), rgb(224, 255, 238), 0.95)
    # the root: brighter and opaque so the wing grows out of the shoulder
    cv.over(np.clip(1 - v / 0.12, 0, 1), rgb(244, 255, 246), 0.5)
    # a few wind swirls, painted as pale thin curls
    for cx, cy, r, fl in ((0.62, 0.30, 11, 1), (0.55, 0.58, 9, -1), (0.66, 0.78, 7, 1)):
        pts = spiral(cx * W, cy * H, 0.5, r, 1.4, 0.0, 36, fl)
        cv.over(aa(poly_d(X, Y, pts), 1.0), rgb(240, 255, 245), 0.5)
    # the very tip: a little brighter
    cv.over(np.clip((v - 0.9) / 0.1, 0, 1), rgb(250, 255, 250), 0.35)
    return cv


# ------------------------------------------------------------------------------------------------ the light effects atlas
def build_fx():
    W, H = LAYOUT["fx"]["size"]
    reg = LAYOUT["fx"]["regions"]
    big = Cv(W, H)

    def region(name):
        R = reg[name]
        return R, R[2] - R[0], R[3] - R[1]

    # crown blade (the shaft of an arrow): bright core, teal edge, brighter toward the tip (v=0 is the tip)
    R, w, h = region("BLADE")
    cv = Cv(w, h); X, Y = grid(w, h)
    cv.fill(MINT_L)
    cv.over(np.clip(1 - Y / h, 0, 1) * np.ones_like(X), WHITE, 0.55)
    cv.over(np.where((X < 2.2) | (X > w - 2.2), 1.0, 0.0), rgb(56, 190, 140))
    cv.over(np.where((X < 1.0) | (X > w - 1.0), 1.0, 0.0), rgb(20, 120, 90))
    cv.over(aa(np.abs(X - w / 2), 2.0), WHITE, 1.0)
    cv.paste_into(big, R[0], R[1])
    # the arrow head (a pyramid face: wide base at the bottom, the apex at the top)
    R, w, h = region("ARROW")
    cv = Cv(w, h); X, Y = grid(w, h)
    cv.fill(MINT_L)
    cv.over(np.clip(1 - Y / h, 0, 1) * np.ones_like(X), WHITE, 0.7)
    cv.over(aa(np.abs(X - w / 2), 2.2), WHITE, 1.0)
    cv.over(np.where(Y > h - 2.0, 1.0, 0.0), rgb(56, 190, 140))
    cv.paste_into(big, R[0], R[1])
    # the flat arrow (mid distance): a shaft with a diamond head
    R, w, h = region("BLADE_FLAT")
    cv = Cv(w, h); X, Y = grid(w, h)
    shaft = np.where((np.abs(X - w / 2) < 1.8) & (Y > 11), 1.0, 0.0)
    head_t = np.clip((Y) / 12.0, 0, 1)
    head = np.where((Y < 14) & (np.abs(X - w / 2) < (np.minimum(Y, 14 - (Y - 7) * 0.0) * 0.0 + np.where(Y < 8, Y * 1.0, (14 - Y) * 1.3 + 0.0))), 1.0, 0.0)
    cv.over(np.maximum(shaft, head), rgb(56, 190, 140))
    cv.over(np.maximum(np.where((np.abs(X - w / 2) < 0.9) & (Y > 4), 1.0, 0.0), np.where((np.abs(X - w / 2) < 3.2) & (Y < 8) & (np.abs(X - w / 2) < Y - 1.0), 1.0, 0.0)), WHITE)
    cv.paste_into(big, R[0], R[1])
    # the ring wall: bright rims, a mint band, notches
    R, w, h = region("RING")
    cv = Cv(w, h); X, Y = grid(w, h)
    cv.fill(MINT_L)
    cv.over(np.where((Y < 1.6) | (Y > h - 1.6), 1.0, 0.0), WHITE)
    cv.over(np.where(((X // 4) % 2 == 0) & (Y > 2.4) & (Y < h - 2.4), 1.0, 0.0), rgb(56, 190, 140), 0.85)
    cv.paste_into(big, R[0], R[1])
    R, w, h = region("RING_TOP")
    cv = Cv(w, h); X, Y = grid(w, h)
    cv.fill(WHITE)
    cv.over(aa(np.abs(Y - h / 2), 1.0), rgb(120, 230, 180), 0.7)
    cv.paste_into(big, R[0], R[1])
    # the halo: a soft ring of light seen from above (additive)
    R, w, h = region("HALO")
    cv = Cv(w, h); X, Y = grid(w, h)
    r = np.hypot(X - w / 2, Y - h / 2)
    ring = np.exp(-((r - 15.5) / 2.6) ** 2)
    soft = np.exp(-((r - 13.0) / 7.0) ** 2) * 0.35
    cv.over(np.clip(ring + soft, 0, 1) * (r < w / 2), rgb(170, 255, 215), 0.75)
    cv.over(aa(np.abs(r - 15.5), 1.2) * (r < w / 2), WHITE, 0.85)
    cv.paste_into(big, R[0], R[1])
    # the cross ornament: a long cross whose ends are arrow heads, with a small diamond at the crossing
    R, w, h = region("CROSS")
    cv = Cv(w, h); X, Y = grid(w, h)
    cx, cy = w / 2, h * 0.42
    shape = np.zeros_like(X)
    shape = np.maximum(shape, aa(seg_d(X, Y, cx, 2.5, cx, h - 3.0), 2.8))
    shape = np.maximum(shape, aa(seg_d(X, Y, cx - 12.5, cy, cx + 12.5, cy), 2.8))
    for (px, py, ang) in ((cx, 2.0, -90), (cx, h - 2.0, 90), (cx - 13, cy, 180), (cx + 13, cy, 0)):
        ca, sa = math.cos(math.radians(ang)), math.sin(math.radians(ang))
        tri = [(px + ca * 4.0, py + sa * 4.0), (px - sa * 3.0 - ca * 2.0, py + ca * 3.0 - sa * 2.0), (px + sa * 3.0 - ca * 2.0, py - ca * 3.0 - sa * 2.0)]
        tri.append(tri[0])
        shape = np.maximum(shape, aa(poly_d(X, Y, tri), 3.4))
        shape = np.maximum(shape, np.where(np.hypot(X - px, Y - py) < 3.0, 1.0, 0.0))
    dia = np.abs(X - cx) + np.abs(Y - cy)
    shape = np.maximum(shape, np.where(dia < 4.6, 1.0, 0.0))
    inner = np.zeros_like(X)
    inner = np.maximum(inner, aa(seg_d(X, Y, cx, 3.5, cx, h - 4.0), 1.0))
    inner = np.maximum(inner, aa(seg_d(X, Y, cx - 12.0, cy, cx + 12.0, cy), 1.0))
    cv.over(shape, rgb(84, 214, 160))
    cv.over(np.clip(shape * 2 - 1, 0, 1), MINT_L)
    cv.over(inner, WHITE)
    cv.over(np.where(dia < 3.2, 1.0, 0.0), WHITE)
    cv.paste_into(big, R[0], R[1])
    # the ribbon of wind: V runs from the base (0) to the free end (1); the end fades out (additive)
    R, w, h = region("RIBBON")
    cv = Cv(w, h); X, Y = grid(w, h)
    fade = np.clip(1 - (Y / h) ** 1.4, 0, 1) * np.clip(Y / 3.0, 0.3, 1)
    core = np.exp(-((X - w / 2) / (w * 0.18)) ** 2)
    soft = np.exp(-((X - w / 2) / (w * 0.42)) ** 2)
    cv.over(np.clip(soft * 0.6 + core, 0, 1) * fade, rgb(190, 255, 224), 0.9)
    cv.over(core * fade * (0.5 + 0.5 * np.sin(Y * 0.9)), WHITE, 0.7)
    cv.paste_into(big, R[0], R[1])
    # a drifting leaf
    R, w, h = region("LEAF")
    cv = Cv(w, h); X, Y = grid(w, h)
    t = (Y - 1.5) / (h - 3.0)
    half = np.abs(np.sin(np.clip(t, 0, 1) * math.pi)) ** 0.8 * (w / 2 - 1.5)
    leaf = np.where((t > 0) & (t < 1) & (np.abs(X - w / 2) < half), 1.0, 0.0)
    cv.over(leaf, rgb(120, 226, 176))
    cv.over(leaf * (np.abs(X - w / 2) < half * 0.5), rgb(190, 255, 220))
    cv.over(aa(np.abs(X - w / 2), 1.0) * (t > 0) * (t < 1), WHITE, 0.9)
    cv.paste_into(big, R[0], R[1])
    # the eye light
    R, w, h = region("EYE")
    cv = Cv(w, h); X, Y = grid(w, h)
    e = np.exp(-(((X - w / 2) / (w * 0.40)) ** 2 + ((Y - h / 2) / (h * 0.36)) ** 2))
    cv.over(e, rgb(190, 255, 222), 0.95)
    cv.over(np.exp(-(((X - w / 2) / (w * 0.22)) ** 2 + ((Y - h / 2) / (h * 0.2)) ** 2)), WHITE, 1.0)
    cv.paste_into(big, R[0], R[1])
    # the hair tint: mottled mint, soft
    R, w, h = region("HAIR")
    cv = Cv(w, h); X, Y = grid(w, h)
    n = noise(w, h, 3, 91)
    cv.over(np.ones_like(X), rgb(120, 240, 180), 0.13)
    cv.paste_into(big, R[0], R[1])
    return big


def save(cv, name):
    os.makedirs(OUT, exist_ok=True)
    path = os.path.join(OUT, name)
    cv.image().save(path)
    print("wrote", path)


def main():
    coat, glow = build_coat()
    save(coat, "mwv2_coat.png")
    save(glow, "mwv2_glow.png")
    save(build_fur(), "mwv2_fur.png")
    save(build_wing(), "mwv2_wing.png")
    save(build_fx(), "mwv2_fx.png")


if __name__ == "__main__":
    main()
