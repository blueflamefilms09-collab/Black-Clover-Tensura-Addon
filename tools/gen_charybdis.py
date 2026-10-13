"""The Charybdis Spawn, the Deep Waterways' floor boss, and the magicule scale it fires, in the repo's GeoLite pipeline (tools/geo_builder.py ->
client.geo.GeoDraw): an ancient squid-whale leviathan, about 5 blocks of body and 7 with its arms, teal and deep-blue skin in rows of cyan scales, a
scarred blind eye and a living one, a dark beak ringed with eight arms and two long feeding tentacles, great ragged side fins, a whale's fluke, barnacles
and two broken harpoons in its back. One model, two skins: the leviathan of the opening, and the flushed one of the Gates (white-hot scales, violet
back, rose veins). The scale is a small flat shard of light in a warm skin for when it is sent back.

    python3 -B tools/gen_charybdis.py                  # models, animations, skins, preview scenes (idempotent)
    python3 -B tools/gen_charybdis.py --check          # read every file back and check the models against their clips
    python3 -B tools/gen_charybdis.py --preview [scene ...]    # the headless render of tools/render_preview (needs JOML_JAR / GSON_JAR or the usual lib folders)

Writes (GeoSpec.of("charybdis", "spawn"), skin "charybdis_spawn_gates"; GeoSpec.of("magicule", "scale"), skin "magicule_scale_returned"):
    assets/nusmp/geo/entity/charybdis_spawn.geo.json
    assets/nusmp/animations/entity/charybdis_spawn.animation.json
    assets/nusmp/textures/entity/charybdis_spawn.png                the opening skin, 1024 wide at 2 texels per model pixel (eyes at 4)
    assets/nusmp/textures/entity/charybdis_spawn_glow.png           the rows of scales, the living eye, the suckers, the harpoon wounds (additive, full bright)
    assets/nusmp/textures/entity/charybdis_spawn_gates.png          the flushed skin of the Gates (the same boxes)
    assets/nusmp/textures/entity/charybdis_spawn_gates_glow.png     every scale white-hot, rose veins in the fins
    assets/nusmp/geo/entity/magicule_scale.geo.json, textures/entity/magicule_scale[_glow].png, magicule_scale_returned[_glow].png
and tools/render_preview/scenes.d/charybdis.json.

Clips (the names ZogratisPose chooses): idle walk run attack cast cast_big hurt transform death. walk is the swim (a travelling wave down the body, the fins
beating, the arms streaming), run the dart of a lunge. Rotation signs of this frame (checked in the preview): positive x swings a hanging limb FORWARD
and lifts the nose of a body whose front is -z; positive z swings towards +x (the model's left); positive y turns the front towards the model's right
(-x). Positions are pixels (y up), scales multiply.
"""
import json
import math
import os
import subprocess
import sys

import numpy as np

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from geo_builder import Model, Anim, ROOT  # noqa: E402
from zogratis_lucius_kit import Pack, Canvas, SIN, COS, TAU, keys, cyc, swing  # noqa: E402
from spirit_geo import NOISE  # noqa: E402

KEY, NAME = "charybdis", "spawn"
REPO = os.path.abspath(os.path.join(HERE, ".."))
SCENES_OUT = os.path.join(HERE, "render_preview", "scenes.d", "charybdis.json")
SCRATCH = os.environ.get("CHARYBDIS_SCRATCH", "/tmp/claude-0/-home-user-Black-Clover-Tensura-Addon/2c8ee7c0-aec7-5d43-b651-54cec528bb71/scratchpad/work_cha")
PREVIEW_OUT = os.path.join(SCRATCH, "previews")
TEX_DIR = os.path.join(ROOT, "textures", "entity")

# ------------------------------------------------------------------------------------------------------------------ palettes (dark -> light)
# the opening skin
DEEP = ((5, 20, 40), (9, 34, 64), (14, 52, 90), (22, 76, 118), (34, 102, 146))
TEAL = ((12, 62, 78), (20, 92, 106), (32, 126, 132), (54, 160, 158), (100, 198, 188))
BELLY = ((78, 134, 140), (120, 172, 174), (160, 204, 200), (200, 230, 224), (232, 246, 240))
FIN = ((16, 60, 86), (26, 90, 116), (44, 124, 146), (78, 162, 174), (130, 204, 208))
# the Gates: violet back, bright turquoise flank, rose-white belly
DEEP_G = ((14, 14, 50), (24, 24, 82), (40, 38, 118), (62, 56, 156), (92, 80, 190))
TEAL_G = ((20, 70, 120), (32, 104, 156), (48, 144, 188), (84, 186, 214), (150, 224, 236))
BELLY_G = ((124, 106, 168), (166, 140, 196), (206, 174, 210), (236, 204, 224), (252, 232, 240))
FIN_G = ((40, 36, 100), (66, 58, 140), (100, 84, 176), (146, 116, 202), (196, 150, 220))
HORN = ((14, 12, 16), (30, 26, 32), (54, 46, 48), (86, 72, 64), (122, 104, 88))
BONE = ((120, 112, 96), (162, 154, 134), (200, 192, 170), (232, 226, 206))
IRON = ((30, 22, 18), (62, 40, 28), (96, 62, 40), (132, 90, 58), (170, 124, 82))
BARN = ((88, 92, 84), (128, 134, 124), (168, 174, 162), (206, 212, 200))
SCALE_GLOW, SCALE_GLOW_G = (60, 232, 255), (210, 255, 255)
VEIN_GLOW_G = (255, 120, 190)
EYE_GLOW = (120, 255, 235)

BAY = np.array(((0, 8, 2, 10), (12, 4, 14, 6), (3, 11, 1, 9), (15, 7, 13, 5)), np.float32)
FACE_T = {"top": 0.12, "bottom": -0.26, "front": 0.0, "back": -0.08, "left": -0.03, "right": -0.03}
STYLE = {"gates": False}

# the body axis: every segment is centred on it
YC = 22.0


def G(base, gates):
    return gates if STYLE["gates"] else base


# ------------------------------------------------------------------------------------------------------------------ vectorised helpers
def vz(x, y, salt=0):
    """The repo's value noise (spirit_geo.nz) over whole arrays, -0.5 .. 0.5."""
    xi = (np.asarray(x) * 3 + 128 + salt * 11).astype(np.int64) % 256
    yi = (np.asarray(y) * 3 + salt * 37).astype(np.int64) % 256
    return NOISE[yi, xi] - 0.5


def hv(a, b, salt=0):
    """A hash of two integer arrays to 0..1."""
    a = np.asarray(a).astype(np.int64) & 0xFFFFFFFF
    b = np.asarray(b).astype(np.int64) & 0xFFFFFFFF
    h = (a * 374761393 + b * 668265263 + ((salt * 2246822519) & 0xFFFFFFFF)) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    h = h ^ (h >> 16)
    return (h & 0xFFFF) / 65535.0


def palarr(cols, t, ii, jj, dither=0.05):
    """The palette entry each tone falls on, ordered-dithered across band edges (hard-edged pixel art, not a gradient)."""
    cols = np.asarray(cols, np.float32)
    n = len(cols)
    t = np.clip(t + ((BAY[jj % 4, ii % 4] + 0.5) / 16.0 - 0.5) * dither, 0.0, 0.9999)
    return cols[(t * n).astype(int)]


def surface(face, W, H, o, s):
    """Model-space x, y, z (pixels) of every texel centre of a box face, and the texel indices. i = 0 is the x-min edge of front / top / bottom, the x-max
    edge of back, the BACK edge of right (-x) and the FRONT edge of left (+x); j = 0 is the top edge of the sides and the BACK edge of top / bottom."""
    ox, oy, oz = o
    sx, sy, sz = s
    jj, ii = np.mgrid[0:H, 0:W]
    fi, fj = (ii + 0.5) / W, (jj + 0.5) / H
    if face == "front":
        return ox + fi * sx, oy + sy - fj * sy, np.full((H, W), float(oz)), ii, jj
    if face == "back":
        return ox + sx - fi * sx, oy + sy - fj * sy, np.full((H, W), float(oz + sz)), ii, jj
    if face == "right":
        return np.full((H, W), float(ox)), oy + sy - fj * sy, oz + sz - fi * sz, ii, jj
    if face == "left":
        return np.full((H, W), float(ox + sx)), oy + sy - fj * sy, oz + fi * sz, ii, jj
    if face == "top":
        return ox + fi * sx, np.full((H, W), float(oy + sy)), oz + sz - fj * sz, ii, jj
    return ox + fi * sx, np.full((H, W), float(oy)), oz + sz - fj * sz, ii, jj


def to_px(rgb, a=None):
    """float colour array (H, W, 3) and optional alpha (H, W) -> the kit's (rgba, glow) tuples are made per texel by the cache below."""
    out = np.zeros(rgb.shape[:2] + (4,), np.float32)
    out[..., :3] = rgb
    out[..., 3] = 255 if a is None else a
    return out


class Cached:
    """A painter for Pack.box that builds a whole face as arrays on its first texel and hands the texels out. `make(face, W, H, o, s)` returns
    (rgba array (H, W, 4), glow array (H, W, 4)); it is built again when the style (the skin) changes."""

    def __init__(self, make, o, s):
        self.make, self.o, self.s = make, tuple(o), tuple(s)
        self.cache = {}

    def __call__(self, face, i, j, fw, fh):
        key = (face, fw, fh, STYLE["gates"])
        if key not in self.cache:
            c, g = self.make(face, fw, fh, self.o, self.s)
            g = np.array(g, np.float32)
            k = np.clip(g[..., 3:4] / 255.0, 0.0, 1.0)             # the glow pass adds the texel's colour as it is: the brightness goes into the colour
            g[..., :3] = g[..., :3] * k
            g[..., 3] = np.where(g[..., 3] > 6, 255.0, 0.0)
            self.cache[key] = (c, g)
        c, g = self.cache[key]
        cc, gg = c[j, i], g[j, i]
        return ((int(cc[0]), int(cc[1]), int(cc[2]), int(cc[3])) if cc[3] > 0 else None,
                (int(gg[0]), int(gg[1]), int(gg[2]), int(gg[3])) if gg[3] > 0 else None)


def part(pk, m, bone, o, s, make, **kw):
    """A box on a bone, painted by `make` (see Cached). Pack rounds the size to its density grid; the painter sees the rounded box."""
    g = 1.0 / pk.dens
    s = tuple(max(g, round(v / g) * g) for v in s)
    return pk.box(m, bone, o, s, Cached(make, o, s), **kw)


# ------------------------------------------------------------------------------------------------------------------ materials
SW, SH = 7.0, 5.5                       # one scale: 7 px round the body, 5.5 px along it


def square_param(nx, ny):
    """Where a point (nx, ny) (each -1..1 across a box) falls on the perimeter of the square, 0..8, going round from the +x flank over the back."""
    ax, ay = np.abs(nx), np.abs(ny)
    right = (ax >= ay) & (nx >= 0)
    left = (ax >= ay) & (nx < 0)
    top = (ay > ax) & (ny >= 0)
    # right edge: s = ny (-1..1); top: 1 + (1 - nx); left: 3 + (1 - ny); bottom: 5 + (nx + 1)
    sr = ny / np.maximum(ax, 1e-6)
    st = 1 + (1 - nx / np.maximum(ay, 1e-6))
    sl = 3 + (1 - ny / np.maximum(ax, 1e-6))
    sb = 5 + (nx / np.maximum(ay, 1e-6) + 1)
    s = np.where(right, sr, np.where(top, st, np.where(left, sl, sb)))
    return np.mod(s + 1, 8)


def seg_dist(P, A, B):
    """Distance from every point of P (H, W, 3) to the segment A-B."""
    A, B = np.asarray(A, float), np.asarray(B, float)
    AB = B - A
    t = np.clip(((P - A) @ AB) / max(float(AB @ AB), 1e-9), 0.0, 1.0)
    Q = A + t[..., None] * AB
    return np.sqrt(((P - Q) ** 2).sum(-1))


# scars, in the model's pixels: long pale cuts across the flanks and the head, a few short ones, and the rings of two old wounds
SCARS = (((-19, 30, -12), (-19, 12, 2)), ((-19, 33, 0), (-19, 22, 8)), ((19, 28, -10), (19, 8, 6)), ((19, 34, -4), (19, 26, 6)), ((-15, 30, -28), (-15, 14, -20)),
         ((15, 32, -26), (15, 16, -30)), ((-8, 40, -4), (6, 40, 3)), ((4, 40, -10), (11, 40, -6)), ((-14, 12, 12), (-14, 26, 17)), ((13, 14, 10), (13, 28, 14)),
         ((-4, 36, -34), (6, 28, -34)), ((-12, 6, -4), (0, 4, 6)))
WOUNDS = ((-19.0, 26.0, 11.0), (19.0, 22.0, -2.0), (0.0, 40.0, 11.0))


def skin_make(rad_w, rad_h, salt, nogrow=False):
    """The leviathan's hide as a function of where a texel is on the body: back, flank and belly in three palettes, rows of fish scales that go round the body
    (the same rows on every segment), the glowing rows, the scars and the old wounds. rad_w / rad_h are the half width / half height of the segment."""
    ncells = max(6, int(round(4 * (rad_w + rad_h) / SW)))

    def make(face, W, H, o, s):
        X, Y, Z, ii, jj = surface(face, W, H, o, s)
        nx, ny = X / max(rad_w, 1e-6), (Y - YC) / max(rad_h, 1e-6)
        cap = face in ("front", "back")
        if cap:                                                       # the end faces of a segment: a flat lattice
            cu, rv = X / SW + 40.0, (YC - Y) / SH + 40.0
        else:
            cu, rv = square_param(nx, ny) / 8.0 * ncells, Z / SH + 40.0
        r = np.floor(rv)
        b = rv - r
        ri = r.astype(np.int64)
        off = np.where((ri & 1) == 1, 0.5, 0.0)
        cc = np.floor(cu + off)
        a = cu + off - cc
        cid = cc.astype(np.int64)
        yb = 0.94 - 0.52 * (2 * a - 1) ** 2
        inside = b <= yb
        arc = inside & (b > yb - 0.2)
        sid = hv(cid, ri, salt)
        mott = vz(Z * 1.1 + X * 0.5, Y * 1.1 + Z * 0.3, salt)
        zz = (ny if not cap else np.clip(ny * 0.6 + 0.2, -1, 1)) + mott * 0.4
        back = zz > 0.42
        belly = zz < -0.40
        ft = FACE_T[face]
        t = 0.50 + 0.20 * (sid - 0.5) + 0.13 * mott + 0.20 * (0.5 - b) * 0.8 - 0.10 * np.abs(a - 0.5) + ft * 0.55
        t = np.where(arc, t - 0.26, t)
        t = np.where(inside, t, 0.02)
        col = np.where(back[..., None], palarr(G(DEEP, DEEP_G), t, ii, jj), np.where(belly[..., None], palarr(G(BELLY, BELLY_G), t * 0.9 + 0.1, ii, jj),
                                                                                 palarr(G(TEAL, TEAL_G), t, ii, jj)))
        # the pale scars and the dark ring of an old wound
        P = np.stack([X, Y, Z], -1)
        scar = np.zeros(X.shape, bool)
        rim = np.zeros(X.shape, bool)
        for A, B in SCARS:
            d = seg_dist(P, A, B)
            scar |= d < 0.62 + 0.25 * mott
            rim |= d < 1.25
        for (wx, wy, wz) in WOUNDS:
            d = np.sqrt((X - wx) ** 2 + (Y - wy) ** 2 + (Z - wz) ** 2)
            rim |= d < 3.2
            scar |= (d > 1.9) & (d < 2.7)
        col = np.where(rim[..., None] & ~scar[..., None], col * 0.72, col)
        col = np.where(scar[..., None], np.asarray(G(BELLY[3], BELLY_G[3]), np.float32) * (0.85 + 0.3 * mott[..., None]), col)
        # the glow: rows of scales alight
        ci = r.astype(np.int64)
        row_on = (ci % 3 == 1)
        lit = np.where(row_on, sid > 0.08, sid > 0.9)
        d = ((a - 0.5) / 0.34) ** 2 + ((b - 0.42) / 0.30) ** 2
        k = np.clip(1.0 - d, 0.0, 1.0) * inside
        zone = np.where(back, 1.0, np.where(belly, 0.0, 0.75))
        g_alpha = (k * lit * zone * np.where(row_on, 1.0, 0.45) * (1 - scar) * 255.0)
        gl = np.asarray(G(SCALE_GLOW, SCALE_GLOW_G), np.float32)
        glow = np.zeros(X.shape + (4,), np.float32)
        glow[..., :3] = gl
        glow[..., 3] = g_alpha
        # a lit scale's face takes some of the light
        col = np.where((g_alpha > 60)[..., None], col * 0.55 + gl * 0.45, col)
        return to_px(col), glow
    return make


def fin_make(salt, tears):
    """A fin membrane on a flat plate (x outward from the root, z from the leading edge back): a paddle that narrows to a rounded tip and sweeps back,
    veins fanning from the root, a ragged trailing edge with the tears the old fights left, glowing rose veins on the flushed skin."""
    def make(face, W, H, o, s):
        X, Y, Z, ii, jj = surface(face, W, H, o, s)
        sx, sz = s[0], s[2]
        u = np.abs(X - (o[0] if o[0] >= 0 else o[0] + sx)) / sx          # 0 at the root, 1 at the tip (a mirrored twin has its root at the other end)
        w = (Z - o[2]) / sz
        half = 0.5 * (1 - 0.80 * np.power(u, 1.5))
        cen = 0.5 + 0.2 * u
        edge = (w - cen) / np.maximum(half, 1e-3)
        ragged = 0.07 * vz(u * 14, w * 14, salt) + 0.05 * np.sin(u * 22 + salt)
        ok = (np.abs(edge) <= 1.0 + ragged * 3) & (u < 1.0 - 0.06 * (1 - np.abs(edge)))
        for (tu, tw, tr) in tears:
            ok &= np.sqrt(((u - tu) * 1.6) ** 2 + (w - tw) ** 2) > tr * (1.0 - 0.35 * np.sin(u * 40))      # a bite out of the edge
        ve = (w - cen * 0.0 - 0.5) / (u + 0.18)
        vein = np.mod(ve * 3.2 + vz(u * 8, w * 8, salt + 1) * 0.5, 1.0)
        on_vein = vein < 0.16
        top = face == "top"
        t = 0.36 + 0.12 * vz(u * 10, w * 10, salt + 2) + (0.10 if top else -0.04) - 0.20 * on_vein + 0.16 * np.abs(edge) ** 3 + FACE_T[face] * 0.4
        col = palarr(G(FIN, FIN_G), t, ii, jj, 0.05)
        glow = np.zeros(X.shape + (4,), np.float32)
        gl = np.asarray(G(SCALE_GLOW, VEIN_GLOW_G), np.float32)
        glow[..., :3] = gl
        glow[..., 3] = np.where(on_vein & ok, (70 if not STYLE["gates"] else 190) * (0.4 + 0.6 * (1 - u)), 0)
        rgba = to_px(col, np.where(ok, 255, 0))
        glow[..., 3] = np.where(ok, glow[..., 3], 0)
        return rgba, glow
    return make


def fluke_make(salt):
    """The whale's fluke: a crescent, notched at the middle, with the ribs of the fin fanning from the tail and a ragged, scarred rim."""
    def make(face, W, H, o, s):
        X, Y, Z, ii, jj = surface(face, W, H, o, s)
        hw, sz = s[0] / 2.0, s[2]
        u = (X - (o[0] + hw)) / hw
        w = (Z - o[2]) / sz
        lobe = 0.40 + 0.62 * np.power(np.abs(u), 0.85)
        ragged = 0.06 * vz(u * 9, w * 9, salt)
        ok = (w <= lobe + ragged) & (np.abs(u) <= 1.0 - 0.34 * np.power(1 - w, 2) + 0.02 * vz(u * 12, w * 12, salt + 1))
        ok &= ~((np.abs(u - 0.62) < 0.07) & (w > 0.62))                      # an old bite out of the right lobe
        ribs = np.mod(u / np.maximum(w + 0.25, 0.25) * 4.0 + vz(u * 6, w * 6, salt + 2) * 0.4, 1.0)
        t = 0.36 + 0.14 * vz(u * 8, w * 8, salt + 3) - 0.18 * (ribs < 0.16) + 0.14 * w + (0.10 if face == "top" else -0.05) + FACE_T[face] * 0.4
        col = palarr(G(FIN, FIN_G), t, ii, jj, 0.05)
        glow = np.zeros(X.shape + (4,), np.float32)
        glow[..., :3] = np.asarray(G(SCALE_GLOW, VEIN_GLOW_G), np.float32)
        glow[..., 3] = np.where(ok & (ribs < 0.16), (60 if not STYLE["gates"] else 170) * (0.3 + 0.7 * w), 0)
        return to_px(col, np.where(ok, 255, 0)), glow
    return make


def horn_make(salt, tip_axis="z"):
    """The beak: dark horn in growth rings, a worn pale cutting edge, a few chips."""
    def make(face, W, H, o, s):
        X, Y, Z, ii, jj = surface(face, W, H, o, s)
        ring = np.sin((Z - o[2]) * 1.9 + vz(X, Y, salt) * 3.0)
        edge = 1.0 - np.clip((Z - o[2]) / max(s[2], 1e-6), 0, 1)           # 1 at the front (-z) tip
        t = 0.34 + 0.14 * ring + 0.30 * edge ** 2.5 + FACE_T[face] * 0.5 + 0.1 * vz(X * 2, Y * 2 + Z, salt + 3)
        col = palarr(HORN, t, ii, jj, 0.06)
        chip = hv(ii // 2, jj // 2, salt) > 0.93
        col = np.where(chip[..., None], col * 0.6, col)
        return to_px(col), np.zeros(X.shape + (4,), np.float32)
    return make


def arm_make(salt, inner, thin=False):
    """An arm segment: teal on the outside, a pale underside with two rows of suckers (each lit with a faint cyan) on the face that looks at the beak."""
    def make(face, W, H, o, s):
        X, Y, Z, ii, jj = surface(face, W, H, o, s)
        t = 0.52 + 0.13 * vz(X * 1.5 + Z, Y * 1.5, salt) + FACE_T[face] * 0.6 + 0.10 * np.sin(Z * 0.9 + salt)
        col = palarr(G(TEAL, TEAL_G), t, ii, jj, 0.05)
        glow = np.zeros(X.shape + (4,), np.float32)
        if face == inner:
            col = palarr(G(TEAL, TEAL_G), 0.62 + 0.2 * vz(X, Z, salt), ii, jj, 0.05)
            across = (X - (o[0] + s[0] / 2)) if face in ("top", "bottom") else (Y - (o[1] + s[1] / 2))
            along = Z
            if face in ("left", "right"):
                across = Y - (o[1] + s[1] / 2)
            lane = np.where(across < 0, -1, 1) * 0.0 + np.round(across / max(0.7, (max(s[0], s[1]) / 2.2)))
            cx = across - np.sign(across) * max(s[0], s[1]) / 4.0
            cz = (np.mod(along + (across > 0) * 2.0, 4.0)) - 2.0
            d = np.sqrt(cx ** 2 + cz ** 2)
            sucker = d < 1.15
            col = np.where(sucker[..., None], np.asarray(G(BELLY[2], BELLY_G[2]), np.float32), col)
            col = np.where((d < 0.55)[..., None], np.asarray(G(TEAL[0], TEAL_G[0]), np.float32), col)
            glow[..., :3] = np.asarray(EYE_GLOW, np.float32)
            glow[..., 3] = np.where(sucker & (d >= 0.55), 90, 0)
        return to_px(col), glow
    return make


def plain_make(cols, salt, noise=0.14, glow_rgba=None):
    def make(face, W, H, o, s):
        X, Y, Z, ii, jj = surface(face, W, H, o, s)
        t = 0.52 + noise * vz(X * 2 + Z, Y * 2, salt) + FACE_T[face] * 0.6
        col = palarr(cols, t, ii, jj, 0.06)
        glow = np.zeros(X.shape + (4,), np.float32)
        if glow_rgba:
            glow[..., :3] = np.asarray(glow_rgba[:3], np.float32)
            glow[..., 3] = glow_rgba[3]
        return to_px(col), glow
    return make


def barnacle_make(salt):
    def make(face, W, H, o, s):
        X, Y, Z, ii, jj = surface(face, W, H, o, s)
        ring = np.sin((np.abs(X - (o[0] + s[0] / 2)) + np.abs(Z - (o[2] + s[2] / 2))) * 2.4)
        t = 0.5 + 0.2 * ring + 0.12 * vz(X * 3, Y * 3 + Z, salt) + FACE_T[face] * 0.6
        return to_px(palarr(BARN, t, ii, jj, 0.06)), np.zeros(X.shape + (4,), np.float32)
    return make


def dorsal_make(salt):
    """A glowing dorsal plate: a dark rim round a bright core."""
    def make(face, W, H, o, s):
        X, Y, Z, ii, jj = surface(face, W, H, o, s)
        big = face in ("left", "right")
        if big:
            ax, ay = (Z - o[2]) / s[2], (Y - o[1]) / s[1]
            d = np.maximum(np.abs(ax - 0.5) * 2, np.abs(ay - 0.4) * 1.6)
        else:
            d = np.full(X.shape, 0.9)
        t = 0.55 - 0.35 * d + FACE_T[face] * 0.4
        col = palarr(G(TEAL, TEAL_G), t, ii, jj, 0.05)
        glow = np.zeros(X.shape + (4,), np.float32)
        glow[..., :3] = np.asarray(G(SCALE_GLOW, SCALE_GLOW_G), np.float32)
        glow[..., 3] = np.clip(1.0 - d, 0, 1) * 255 if big else 40
        return to_px(col), glow
    return make


def eye_make(alive):
    """An eye: the outward face is drawn by hand on a canvas (the living eye a glowing slit in a cold iris, the blind one clouded white with a pale scar
    cut across it); the other faces are skin."""
    def make(face, W, H, o, s):
        X, Y, Z, ii, jj = surface(face, W, H, o, s)
        t = 0.45 + 0.1 * vz(X * 2, Y * 2 + Z, 5) + FACE_T[face] * 0.6
        col = palarr(G(TEAL, TEAL_G), t, ii, jj, 0.05)
        glow = np.zeros(X.shape + (4,), np.float32)
        if (face == "left" and o[0] > 0) or (face == "right" and o[0] < 0):
            cv = Canvas(W, H)
            c = (W - 1) / 2.0
            r = W * 0.46
            for y in range(H):
                for x in range(W):
                    dx, dy = (x - c) / r, (y - (H - 1) / 2.0) / (H * 0.46)
                    d2 = dx * dx + dy * dy
                    if d2 > 1.0:
                        continue
                    if alive:
                        iris = d2 < 0.64
                        pupil = abs(dx) < 0.13 and abs(dy) < 0.78
                        if pupil:
                            cv.put(x, y, (4, 10, 14), 255, None)
                        elif iris:
                            k = 0.4 + 0.6 * (1 - math.sqrt(d2))
                            cv.put(x, y, (int(40 + 60 * k), int(140 + 90 * k), int(150 + 80 * k)), 255, EYE_GLOW, int(120 + 135 * k))
                        else:
                            cv.put(x, y, (170, 196, 190) if d2 < 0.9 else (60, 100, 104), 255, None)
                    else:
                        base = (196, 214, 222) if d2 < 0.62 else ((150, 178, 190) if d2 < 0.9 else (62, 92, 100))
                        cv.put(x, y, base, 255, None)
            if not alive:
                cv.line(int(W * 0.18), int(H * 0.12), int(W * 0.84), int(H * 0.92), (230, 238, 244), 255, None, 255, width=max(1, W // 12))
                cv.line(int(W * 0.14), int(H * 0.16), int(W * 0.80), int(H * 0.96), (60, 70, 90), 255, None, 255, width=max(1, W // 24))
            img, gl = cv.c, cv.g
            m_ = img[..., 3] > 0
            col = np.where(m_[..., None], img[..., :3], col)
            glow = gl
        return to_px(col), glow
    return make


# ------------------------------------------------------------------------------------------------------------------ the leviathan
ARMS = 8
RING_C = (0.0, 17.0)                    # the centre of the ring of arms (x, y), on the front of the snout
RING_R = 10.5
ARM_Z = -40.0
ARM_LEN = (14.0, 12.0, 10.0)
ARM_W = (5.4, 4.2, 3.2)
LONG_LEN = (14.0, 14.0, 13.0, 12.0)
LONG_W = (3.8, 3.3, 2.9, 2.6)
DORSAL = (("body", -11.0, 41.0, 8.0), ("body", -5.0, 41.0, 9.5), ("body", 1.0, 41.0, 8.5), ("rear1", 12.0, 36.0, 7.0), ("rear2", 24.0, 33.0, 6.0), ("tail", 33.0, 29.0, 5.0))


def arm_angle(k):
    return TAU * (k + 0.5) / ARMS


def octo(pk, m, bone, o, s, make, c=5.0, **kw):
    """A segment with a chamfered cross-section: the main box and a slimmer slab above and below it, painted as one surface (the skin function reads the
    position on the whole segment). o, s: the box of the whole segment."""
    ox, oy, oz = o
    sx, sy, sz = s
    part(pk, m, bone, (ox, oy + c, oz), (sx, sy - 2 * c, sz), make, **kw)
    part(pk, m, bone, (ox + c, oy + sy - c, oz), (sx - 2 * c, c, sz), make, faces=("top", "front", "back", "left", "right"), **kw)
    part(pk, m, bone, (ox + c, oy, oz), (sx - 2 * c, c, sz), make, faces=("bottom", "front", "back", "left", "right"), **kw)


def build_spawn():
    m = Model(KEY, NAME, tex_w=1024, tex_h=512)
    pk = Pack(1024, 512, dens=2)
    m.bone("root", pivot=(0, 0, 0))
    m.bone("body", "root", pivot=(0, YC, 0))
    m.bone("head", "body", pivot=(0, YC, -14))
    m.bone("jaw_up", "head", pivot=(0, 19, -40))
    m.bone("jaw_low", "head", pivot=(0, 14, -40))
    m.bone("fin_l", "body", pivot=(18, 24, 0), rotation=(0, 0, -9))
    m.bone("fin_r", "body", pivot=(-18, 24, 0), rotation=(0, 0, 9))
    for k, (parent, z, y, h) in enumerate(DORSAL):
        m.bone("dorsal%d" % k, parent, pivot=(0, y, z))
    m.bone("rear1", "body", pivot=(0, YC, 8))
    m.bone("rear2", "rear1", pivot=(0, YC, 20))
    m.bone("tail", "rear2", pivot=(0, YC, 30))
    m.bone("fluke", "tail", pivot=(0, YC, 38))
    m.bone("spear1", "body", pivot=(-2, 40, 11), rotation=(24, 0, -16))
    m.bone("spear2", "body", pivot=(20, 22, -2), rotation=(0, 0, -62))

    # ---- the body, nose to tail; the cross-section is chamfered so it reads round
    octo(pk, m, "head", (-15, 7, -34), (30, 31, 20), skin_make(15, 15.5, 1), c=5)
    part(pk, m, "head", (-12, 31, -36), (24, 5, 8), skin_make(15, 15.5, 2))                      # the forehead, a heavy brow over the snout
    part(pk, m, "head", (-11, 10, -40), (22, 17, 6), skin_make(11, 8.5, 3))                       # the snout the beak grows from
    part(pk, m, "head", (-8, 5, -38), (16, 5, 9), skin_make(9, 4, 4))                             # the chin
    part(pk, m, "head", (-3, 4, -24), (6, 3, 9), plain_make(G(TEAL, TEAL_G), 7))                 # the siphon
    octo(pk, m, "body", (-19, 3, -14), (38, 38, 22), skin_make(19, 19, 5), c=6)
    octo(pk, m, "rear1", (-15, 8, 8), (30, 28, 12), skin_make(15, 14, 6), c=5)
    octo(pk, m, "rear2", (-11, 11, 20), (22, 22, 10), skin_make(11, 11, 7), c=4)
    octo(pk, m, "tail", (-7, 15, 30), (14, 14, 8), skin_make(7, 7, 8), c=3)
    part(pk, m, "fluke", (-22, 21, 38), (44, 2, 13), fluke_make(9), faces=("top", "bottom"), dens=2)

    # ---- the eyes: the blind one on the model's left (+x), the living one on its right, under a heavy brow
    part(pk, m, "head", (14, 21, -32), (6, 10, 11), eye_make(False), dens={"left": 4, "right": 4, "top": 2, "bottom": 2, "front": 2, "back": 2})
    part(pk, m, "head", (-20, 21, -32), (6, 10, 11), eye_make(True), dens={"left": 4, "right": 4, "top": 2, "bottom": 2, "front": 2, "back": 2})
    part(pk, m, "head", (13, 31, -33), (8, 3, 13), skin_make(15, 15.5, 11))                       # the brow ridges over the eyes
    part(pk, m, "head", (-21, 31, -33), (8, 3, 13), skin_make(15, 15.5, 12))

    # ---- the beak: a hooked upper half over a lower one, in a dark mouth
    part(pk, m, "head", (-6, 12, -42), (12, 12, 3), plain_make(((2, 4, 8), (6, 10, 16), (12, 18, 28), (20, 30, 42)), 10))
    part(pk, m, "jaw_up", (-4, 17, -50), (8, 7, 10), horn_make(11))
    part(pk, m, "jaw_up", (-3, 14.5, -48), (6, 3, 7), horn_make(12))                              # the hook under the upper beak
    part(pk, m, "jaw_low", (-3, 11, -47), (6, 4, 8), horn_make(13))

    # ---- the fins
    f_l = part(pk, m, "fin_l", (17, 23, -13), (28, 1.5, 30), fin_make(14, ((0.55, 0.82, 0.10), (0.85, 0.35, 0.07))), faces=("top", "bottom"), dens=2)
    pk.twin(m, "fin_r", f_l)

    # ---- the dorsal plates, a row of glowing shards down the spine
    for k, (parent, z, y, h) in enumerate(DORSAL):
        part(pk, m, "dorsal%d" % k, (-0.9, y - 1.5, z - 2.5), (1.8, h, 5), dorsal_make(20 + k), rotation=(-18, 0, 0), pivot=(0, y, z))

    # ---- the ring of arms, and the two long feeding tentacles
    for k in range(ARMS):
        th = arm_angle(k)
        dx, dy = math.sin(th), math.cos(th)
        px_, py_, pz_ = RING_C[0] + RING_R * dx, RING_C[1] + RING_R * dy, ARM_Z
        inner = ("bottom" if dy > 0.5 else "top" if dy < -0.5 else ("right" if dx > 0 else "left"))
        z0 = pz_
        parent = "head"
        for sidx in range(3):
            nm = "arm%d%s" % (k, "abc"[sidx])
            bend = (7.0, 11.0, 16.0)[sidx] * (0.8 + 0.4 * ((k * 5) % 3) / 2.0)
            m.bone(nm, parent, pivot=(px_, py_, z0), rotation=(bend * dy - (5 if sidx else 1), -bend * dx, 0))
            w = ARM_W[sidx] * (0.92 + 0.16 * ((k * 3) % 4) / 3.0)
            part(pk, m, nm, (px_ - w / 2, py_ - w / 2, z0 - ARM_LEN[sidx]), (w, w, ARM_LEN[sidx]), arm_make(30 + k * 3 + sidx, inner))
            parent = nm
            z0 -= ARM_LEN[sidx]
    for sgn, tag in ((1, "l"), (-1, "r")):
        z0, parent = -40.0, "head"
        bx, by = 5.0 * sgn, 10.0
        for sidx in range(4):
            nm = "long_%s%d" % (tag, sidx)
            m.bone(nm, parent, pivot=(bx, by, z0), rotation=(-3 - 4 * sidx, -sgn * (7 + 4 * sidx), 0))
            w = LONG_W[sidx]
            part(pk, m, nm, (bx - w / 2, by - w / 2, z0 - LONG_LEN[sidx]), (w, w, LONG_LEN[sidx]), arm_make(60 + sidx + (0 if sgn > 0 else 7), "bottom"))
            parent = nm
            z0 -= LONG_LEN[sidx]
        m.bone("club_%s" % tag, parent, pivot=(bx, by, z0), rotation=(-6, -sgn * 6, 0))
        part(pk, m, "club_%s" % tag, (bx - 3.5, by - 3.5, z0 - 9), (7, 7, 9), arm_make(70 + (0 if sgn > 0 else 3), "bottom"))
        part(pk, m, "club_%s" % tag, (bx - 2.5, by - 2.5, z0 - 11), (5, 5, 2), plain_make(G(BELLY, BELLY_G), 77))

    # ---- barnacles on the head and the fin roots, and the two broken harpoons
    for n, (bone, o, s) in enumerate((("head", (-17, 13, -27), (3, 3, 3)), ("head", (-16.5, 16.5, -23), (2.5, 2.5, 2.5)), ("head", (14.5, 10, -25), (3, 3, 3)),
                                     ("head", (-6, 38, -27), (3, 2, 3)), ("body", (-21, 12, -6), (2.5, 3, 3)), ("body", (18.5, 8, 2), (3, 3, 3)),
                                     ("rear1", (14, 20, 12), (2.5, 2.5, 3)))):
        part(pk, m, bone, o, s, barnacle_make(80 + n))
    part(pk, m, "spear1", (-1, 40, 10), (2, 11, 2), plain_make(BONE, 90))
    part(pk, m, "spear1", (-1.5, 50, 9.5), (3, 2, 3), plain_make(IRON, 91))
    part(pk, m, "spear2", (20, 21, -3), (11, 2, 2), plain_make(BONE, 92))
    part(pk, m, "spear2", (31, 20.5, -3.5), (2, 3, 3), plain_make(IRON, 93))
    return m, pk


# ------------------------------------------------------------------------------------------------------------------ animations
DX = [math.sin(arm_angle(k)) for k in range(ARMS)]
DY = [math.cos(arm_angle(k)) for k in range(ARMS)]
ARM_BONES = [("arm%d%s" % (k, c), k, i) for k in range(ARMS) for i, c in enumerate("abc")]
LONG_BONES = [("long_%s%d" % (t, i), (1 if t == "l" else -1), i) for t in "lr" for i in range(4)] + [("club_l", 1, 4), ("club_r", -1, 4)]
TAIL = (("rear1", 5.0, 0.0), ("rear2", 9.0, 0.8), ("tail", 14.0, 1.6), ("fluke", 8.0, 2.4))
DORS = ["dorsal%d" % k for k in range(len(DORSAL))]


def bend(k, deg):
    """The rotation (degrees) that bends arm k outward from the beak by `deg`."""
    return (deg * DY[k], -deg * DX[k], 0.0)


def plus(*vs):
    return tuple(sum(v[i] for v in vs) for i in range(3))


def arm_loop(a, clip, L, base=0.0, sway=(5.0, 8.0, 12.0), lag=0.9, n=1, curl=0.0):
    """Every arm and the two long tentacles drifting in their own phase: `base` degrees of outward bend, `sway` the amplitude of the ripple down the arm."""
    for nm, k, i in ARM_BONES:
        w = (0.6, 1.0, 1.4)[i % 3]
        ph = k * 0.83 + i * lag
        cyc(a, clip, nm, "rot", L, lambda th, k=k, i=i, w=w, ph=ph: plus(tuple(base * w * v for v in (DY[k], -DX[k], 0)), (sway[i] * SIN(n * th + ph) - sway[i] * SIN(ph), sway[i] * 0.8 * COS(n * th + ph + 1.1) - sway[i] * 0.8 * COS(ph + 1.1), 0), (curl * w, 0, 0)), n=1)
    for nm, sgn, i in LONG_BONES:
        ph = (0.0 if sgn > 0 else 1.7) + i * 0.8
        amp = (3.0, 5.0, 8.0, 10.0, 8.0)[i]
        cyc(a, clip, nm, "rot", L, lambda th, sgn=sgn, i=i, ph=ph, amp=amp: (amp * SIN(n * th + ph) - amp * SIN(ph), sgn * amp * 0.9 * COS(n * th + ph + 0.8) - sgn * amp * 0.9 * COS(ph + 0.8), 0), n=1)


def tail_wave(a, clip, L, scale=1.0, n=1, fluke_pitch=12.0):
    for bone, amp, ph in TAIL:
        cyc(a, clip, bone, "rot", L, swing((0, 0, 0), (0, amp * scale, 0), ph * 0.9), n=n)
    cyc(a, clip, "fluke", "rot", L, swing((0, 0, 0), (fluke_pitch * scale, 0, 0), 2.0), n=n)


def fins_loop(a, clip, L, flap=14.0, base=0.0, n=1, ph=0.0):
    cyc(a, clip, "fin_l", "rot", L, swing((0, 0, base), (0, 0, flap), ph), n=n)
    cyc(a, clip, "fin_r", "rot", L, swing((0, 0, -base), (0, 0, -flap), ph), n=n)


def body_loop(a, clip, L, bob=0.8, pitch=1.2, roll=0.0, n=1):
    cyc(a, clip, "body", "pos", L, swing((0, 0, 0), (0, bob, 0), 0.0), n=n)
    cyc(a, clip, "body", "rot", L, swing((0, 0, 0), (pitch, 0, roll), 0.5), n=n)


def dorsal_loop(a, clip, L, amp=4.0, base=0.0, n=1):
    for k, nm in enumerate(DORS):
        cyc(a, clip, nm, "rot", L, swing((base, 0, 0), (amp, 0, 0), k * 0.6), n=n)


def jaw_loop(a, clip, L, open_=3.0, base=0.0, n=1):
    cyc(a, clip, "jaw_low", "rot", L, swing((base, 0, 0), (-open_, 0, 0), 0.4), n=n)
    cyc(a, clip, "jaw_up", "rot", L, swing((base * 0.4, 0, 0), (open_ * 0.5, 0, 0), 0.4), n=n)


def build_anims(m):
    a = Anim()
    # ---- idle: it hangs in the water, breathing, the tail and the arms drifting
    a.clip("idle", 4.0, True)
    body_loop(a, "idle", 4.0, 0.9, 1.4)
    tail_wave(a, "idle", 4.0, 0.45, fluke_pitch=7.0)
    fins_loop(a, "idle", 4.0, 7.0, 0.0)
    arm_loop(a, "idle", 4.0, base=2.0, sway=(4.0, 7.0, 10.0))
    dorsal_loop(a, "idle", 4.0, 3.0)
    jaw_loop(a, "idle", 4.0, 3.0)
    # ---- walk: the swim, a wave down the body, the fins beating, the arms trailing
    a.clip("walk", 1.6, True)
    body_loop(a, "walk", 1.6, 0.8, 1.8)
    tail_wave(a, "walk", 1.6, 1.0, fluke_pitch=14.0)
    fins_loop(a, "walk", 1.6, 16.0, 4.0)
    arm_loop(a, "walk", 1.6, base=9.0, sway=(5.0, 9.0, 13.0), curl=-4.0)
    dorsal_loop(a, "walk", 1.6, 4.0, base=-6.0)
    jaw_loop(a, "walk", 1.6, 2.0)
    # ---- run: the dart of a lunge, the arms swept back, the fins folded, the body stretched
    a.clip("run", 0.8, True)
    body_loop(a, "run", 0.8, 0.4, 1.2)
    tail_wave(a, "run", 0.8, 1.35, fluke_pitch=20.0)
    fins_loop(a, "run", 0.8, 9.0, -22.0)
    arm_loop(a, "run", 0.8, base=22.0, sway=(3.0, 5.0, 7.0), curl=-10.0)
    dorsal_loop(a, "run", 0.8, 3.0, base=-16.0)
    cyc(a, "run", "jaw_low", "rot", 0.8, swing((-14.0, 0, 0), (0, 0, 0), 0.0))
    # ---- attack: the arms wound back and spread, then lashed round with a snap of the beak
    a.clip("attack", 0.9, False)
    for nm, k, i in ARM_BONES:
        w = (0.6, 1.0, 1.4)[i]
        keys(a, "attack", nm, "rot", [(0.0, plus(bend(k, 26 * w), (-8 * w, 0, 0))), (0.22, plus(bend(k, 30 * w), (-10 * w, 0, 0))), (0.38, plus(bend(k, -10 * w), (14 * w, 0, 0))),
                                      (0.55, plus(bend(k, -4 * w), (8 * w, 0, 0))), (0.9, (0, 0, 0))])
    for nm, sgn, i in LONG_BONES:
        keys(a, "attack", nm, "rot", [(0.0, (-6 - 2 * i, sgn * 14, 0)), (0.22, (-10 - 3 * i, sgn * 18, 0)), (0.38, (10 + 3 * i, -sgn * 12, 0)), (0.9, (0, 0, 0))])
    keys(a, "attack", "head", "rot", [(0.0, (-8, 0, 0)), (0.22, (-10, 0, 0)), (0.38, (12, 0, 0)), (0.9, (0, 0, 0))])
    keys(a, "attack", "body", "pos", [(0.0, (0, 0, 3)), (0.22, (0, 0, 4)), (0.38, (0, 0, -6)), (0.9, (0, 0, 0))])
    keys(a, "attack", "jaw_low", "rot", [(0.0, (-20, 0, 0)), (0.22, (-24, 0, 0)), (0.36, (-24, 0, 0)), (0.42, (2, 0, 0)), (0.9, (0, 0, 0))])
    keys(a, "attack", "jaw_up", "rot", [(0.0, (10, 0, 0)), (0.22, (12, 0, 0)), (0.42, (-2, 0, 0)), (0.9, (0, 0, 0))])
    for bone, amp, ph in TAIL:
        keys(a, "attack", bone, "rot", [(0.0, (0, -amp, 0)), (0.38, (0, amp * 0.8, 0)), (0.9, (0, 0, 0))])
    for k in range(2):
        keys(a, "attack", "fin_l" if k == 0 else "fin_r", "rot", [(0.0, (0, 0, (1 if k == 0 else -1) * 18)), (0.38, (0, 0, (1 if k == 0 else -1) * -10)), (0.9, (0, 0, 0))])
    # ---- cast: arms raised and spread, the plates on its back flared, the beak open, a slow pulse
    a.clip("cast", 1.2, True)
    body_loop(a, "cast", 1.2, 0.6, 1.0)
    tail_wave(a, "cast", 1.2, 0.35, fluke_pitch=5.0)
    fins_loop(a, "cast", 1.2, 5.0, 12.0)
    arm_loop(a, "cast", 1.2, base=22.0, sway=(3.0, 5.0, 8.0), curl=6.0)
    dorsal_loop(a, "cast", 1.2, 5.0, base=18.0)
    jaw_loop(a, "cast", 1.2, 4.0, base=-12.0)
    cyc(a, "cast", "head", "rot", 1.2, swing((-6, 0, 0), (2, 0, 0), 0.0))
    # ---- cast_big: it rears, the arms flung wide, the fins and plates flared; the slam is at 80% of the clip, then it settles
    L = 2.0
    a.clip("cast_big", L, False)
    for nm, k, i in ARM_BONES:
        w = (0.6, 1.0, 1.4)[i]
        keys(a, "cast_big", nm, "rot", [(0.0, (0, 0, 0)), (0.5, plus(bend(k, 20 * w), (-4 * w, 0, 0))), (1.2, plus(bend(k, 46 * w), (-10 * w, 0, 0))), (1.5, plus(bend(k, 50 * w), (-12 * w, 0, 0))),
                                        (1.62, plus(bend(k, -14 * w), (16 * w, 0, 0))), (1.8, plus(bend(k, -2 * w), (6 * w, 0, 0))), (2.0, (0, 0, 0))])
    for nm, sgn, i in LONG_BONES:
        keys(a, "cast_big", nm, "rot", [(0.0, (0, 0, 0)), (1.2, (-12 - 3 * i, sgn * 24, 0)), (1.5, (-14 - 3 * i, sgn * 26, 0)), (1.62, (14 + 3 * i, -sgn * 10, 0)), (2.0, (0, 0, 0))])
    keys(a, "cast_big", "body", "rot", [(0.0, (0, 0, 0)), (1.2, (-14, 0, 0)), (1.5, (-18, 0, 0)), (1.62, (16, 0, 0)), (1.8, (4, 0, 0)), (2.0, (0, 0, 0))])
    keys(a, "cast_big", "body", "pos", [(0.0, (0, 0, 0)), (1.2, (0, 5, 3)), (1.5, (0, 7, 4)), (1.62, (0, -2, -7)), (2.0, (0, 0, 0))])
    keys(a, "cast_big", "jaw_low", "rot", [(0.0, (0, 0, 0)), (1.0, (-26, 0, 0)), (1.5, (-30, 0, 0)), (1.6, (-30, 0, 0)), (1.66, (2, 0, 0)), (2.0, (0, 0, 0))])
    keys(a, "cast_big", "jaw_up", "rot", [(0.0, (0, 0, 0)), (1.0, (14, 0, 0)), (1.5, (16, 0, 0)), (1.66, (-2, 0, 0)), (2.0, (0, 0, 0))])
    for k, fin in enumerate(("fin_l", "fin_r")):
        sg = 1 if k == 0 else -1
        keys(a, "cast_big", fin, "rot", [(0.0, (0, 0, 0)), (1.2, (0, 0, sg * 30)), (1.5, (0, 0, sg * 34)), (1.62, (0, 0, sg * -14)), (2.0, (0, 0, 0))])
    for k, nm in enumerate(DORS):
        keys(a, "cast_big", nm, "rot", [(0.0, (0, 0, 0)), (1.2, (28, 0, 0)), (1.5, (30 + k, 0, 0)), (1.64, (-10, 0, 0)), (2.0, (0, 0, 0))])
    for bone, amp, ph in TAIL:
        keys(a, "cast_big", bone, "rot", [(0.0, (0, 0, 0)), (1.2, (-6 - amp * 0.3, 0, 0)), (1.62, (8, 0, 0)), (2.0, (0, 0, 0))])
    # ---- hurt: a recoil, the arms flung out, the beak open
    a.clip("hurt", 0.4, False)
    keys(a, "hurt", "body", "rot", [(0.0, (0, 0, 0)), (0.1, (-7, 0, 3)), (0.4, (0, 0, 0))])
    keys(a, "hurt", "body", "pos", [(0.0, (0, 0, 0)), (0.1, (0, 1, 4)), (0.4, (0, 0, 0))])
    for nm, k, i in ARM_BONES:
        keys(a, "hurt", nm, "rot", [(0.0, (0, 0, 0)), (0.1, plus(bend(k, 16 * (0.6, 1.0, 1.4)[i]), (-6, 0, 0))), (0.4, (0, 0, 0))])
    keys(a, "hurt", "jaw_low", "rot", [(0.0, (0, 0, 0)), (0.1, (-16, 0, 0)), (0.4, (0, 0, 0))])
    for k, fin in enumerate(("fin_l", "fin_r")):
        keys(a, "hurt", fin, "rot", [(0.0, (0, 0, 0)), (0.1, (0, 0, (1 if k == 0 else -1) * 20)), (0.4, (0, 0, 0))])
    # ---- transform: the Gates. It coils and shudders, bursts open at the middle (where the skin changes) and rises flared, then settles
    a.clip("transform", 3.0, False)
    for nm, k, i in ARM_BONES:
        w = (0.6, 1.0, 1.4)[i]
        keys(a, "transform", nm, "rot", [(0.0, (0, 0, 0)), (0.6, plus(bend(k, -16 * w), (10 * w, 0, 0))), (1.2, plus(bend(k, -22 * w), (14 * w, 0, 0))), (1.5, plus(bend(k, 40 * w), (-8 * w, 0, 0))),
                                         (2.2, plus(bend(k, 30 * w), (-6 * w, 0, 0))), (3.0, (0, 0, 0))])
    for nm, sgn, i in LONG_BONES:
        keys(a, "transform", nm, "rot", [(0.0, (0, 0, 0)), (1.2, (8 + 2 * i, -sgn * 18, 0)), (1.5, (-14 - 3 * i, sgn * 28, 0)), (2.2, (-10 - 2 * i, sgn * 18, 0)), (3.0, (0, 0, 0))])
    keys(a, "transform", "body", "rot", [(0.0, (0, 0, 0)), (0.6, (10, 0, 4)), (1.2, (14, 0, -4)), (1.5, (-20, 0, 0)), (2.2, (-8, 0, 0)), (3.0, (0, 0, 0))])
    keys(a, "transform", "body", "pos", [(0.0, (0, 0, 0)), (1.2, (0, -3, 3)), (1.5, (0, 6, -3)), (2.2, (0, 3, 0)), (3.0, (0, 0, 0))])
    for bone, amp, ph in TAIL:
        keys(a, "transform", bone, "rot", [(0.0, (0, 0, 0)), (0.6, (0, amp * 1.4, 0)), (1.2, (0, -amp * 1.6, 0)), (1.5, (0, 0, 0)), (2.2, (0, amp * 0.6, 0)), (3.0, (0, 0, 0))])
    keys(a, "transform", "jaw_low", "rot", [(0.0, (0, 0, 0)), (1.2, (-6, 0, 0)), (1.5, (-32, 0, 0)), (2.4, (-14, 0, 0)), (3.0, (0, 0, 0))])
    keys(a, "transform", "jaw_up", "rot", [(0.0, (0, 0, 0)), (1.5, (16, 0, 0)), (3.0, (0, 0, 0))])
    for k, fin in enumerate(("fin_l", "fin_r")):
        sg = 1 if k == 0 else -1
        keys(a, "transform", fin, "rot", [(0.0, (0, 0, 0)), (1.2, (0, 0, sg * -22)), (1.5, (0, 0, sg * 36)), (2.2, (0, 0, sg * 22)), (3.0, (0, 0, 0))])
    for k, nm in enumerate(DORS):
        keys(a, "transform", nm, "rot", [(0.0, (0, 0, 0)), (1.2, (-14, 0, 0)), (1.5, (34, 0, 0)), (2.2, (20 + k, 0, 0)), (3.0, (0, 0, 0))])
    # ---- death: it rolls onto its side and sinks, the arms and the tail going slack
    a.clip("death", 1.2, False)
    keys(a, "death", "root", "rot", [(0.0, (0, 0, 0)), (0.5, (-10, 0, 40)), (1.2, (-16, 0, 96))])
    keys(a, "death", "root", "pos", [(0.0, (0, 0, 0)), (1.2, (0, -4, 0))])
    for nm, k, i in ARM_BONES:
        w = (0.6, 1.0, 1.4)[i]
        keys(a, "death", nm, "rot", [(0.0, (0, 0, 0)), (0.5, plus(bend(k, 30 * w), (10 * w, 0, 0))), (1.2, plus(bend(k, 50 * w), (26 * w, 0, 0)))])
    for nm, sgn, i in LONG_BONES:
        keys(a, "death", nm, "rot", [(0.0, (0, 0, 0)), (1.2, (18 + 4 * i, sgn * 24, 0))])
    keys(a, "death", "jaw_low", "rot", [(0.0, (0, 0, 0)), (0.4, (-34, 0, 0)), (1.2, (-38, 0, 0))])
    keys(a, "death", "jaw_up", "rot", [(0.0, (0, 0, 0)), (0.4, (16, 0, 0)), (1.2, (18, 0, 0))])
    for bone, amp, ph in TAIL:
        keys(a, "death", bone, "rot", [(0.0, (0, 0, 0)), (0.6, (0, amp * 1.2, 0)), (1.2, (0, amp * 1.8, 0))])
    for k, fin in enumerate(("fin_l", "fin_r")):
        keys(a, "death", fin, "rot", [(0.0, (0, 0, 0)), (1.2, (0, 0, (1 if k == 0 else -1) * -34))])
    for nm in DORS:
        keys(a, "death", nm, "rot", [(0.0, (0, 0, 0)), (1.2, (-24, 0, 0))])
    return a


# ------------------------------------------------------------------------------------------------------------------ the magicule scale
def scale_make(warm):
    """A shard of light: a pointed lens, bright core, darker rim. The tip is the front (-z). warm = the skin of a scale that was sent back."""
    rim = ((200, 120, 40), (255, 190, 90)) if warm else ((10, 90, 120), (60, 200, 230))
    core = (255, 250, 220) if warm else (220, 255, 255)
    gl = (255, 200, 90) if warm else (60, 232, 255)

    def make(face, W, H, o, s):
        X, Y, Z, ii, jj = surface(face, W, H, o, s)
        u = (X - (o[0] + s[0] / 2)) / (s[0] / 2)
        w = (Z - o[2]) / s[2]
        half = np.sin(np.clip(w, 0, 1) * math.pi) ** 0.8
        ok = np.abs(u) <= half
        d = np.clip(np.abs(u) / np.maximum(half, 1e-3), 0, 1)
        col = np.zeros(X.shape + (3,), np.float32)
        for c in range(3):
            col[..., c] = rim[0][c] + (rim[1][c] - rim[0][c]) * (1 - d) ** 0.7
        col = np.where((d < 0.35)[..., None], np.asarray(core, np.float32), col)
        glow = np.zeros(X.shape + (4,), np.float32)
        glow[..., :3] = np.asarray(gl, np.float32) * (1 - 0.5 * d[..., None])
        glow[..., 3] = np.where(ok, 255, 0)
        return to_px(col, np.where(ok, 255, 0)), glow
    return make


def build_scale(warm):
    m = Model("magicule", "scale", tex_w=64, tex_h=64)
    pk = Pack(64, 64, dens=4)
    m.bone("scale", pivot=(0, 0, 0))
    mk = scale_make(warm)
    part(pk, m, "scale", (-3.5, -0.5, -6), (7, 1, 12), mk, faces=("top", "bottom"))
    return m, pk


def save_scale():
    m, pk = build_scale(False)
    pk.finish()
    m.save(None)
    for warm, name in ((False, "magicule_scale"), (True, "magicule_scale_returned")):
        _, pk2 = build_scale(warm)
        pk2.finish()
        img, glow = pk2.images()
        img.save(os.path.join(TEX_DIR, name + ".png"))
        glow.save(os.path.join(TEX_DIR, name + "_glow.png"))


# ------------------------------------------------------------------------------------------------------------------ writing
def save_skins(m, pk, name_open, name_gates, anim):
    """Packs and paints the sheet in the opening skin, writes the model (its cubes know their uv only now), then paints the skin of the Gates and writes
    both textures; returns the used height."""
    os.makedirs(TEX_DIR, exist_ok=True)
    STYLE["gates"] = False
    pk.finish()
    m.save(anim)
    img, glow = pk.images()
    img.save(os.path.join(TEX_DIR, name_open + ".png"))
    glow.save(os.path.join(TEX_DIR, name_open + "_glow.png"))
    STYLE["gates"] = True
    pk.paint_all()
    img, glow = pk.images()
    img.save(os.path.join(TEX_DIR, name_gates + ".png"))
    glow.save(os.path.join(TEX_DIR, name_gates + "_glow.png"))
    STYLE["gates"] = False
    return pk.height_used()


def scene(model, anim, ages, views, texture=None, tile=(520, 400), glow="FFFFFFFF", layer="CUTOUT"):
    sc = {"kind": "geo", "model": model, "space": "PROP", "layer": layer, "argb": "FFFFFFFF", "glow": glow, "ages": ages, "tile": list(tile), "floor": False,
          "figure": False, "expect": [], "views": views}
    if anim:
        sc["anim"] = anim
    if texture:
        sc["texture"] = texture
    return sc


def view(name, yaw, pitch, target=(0, 1.5, 0), dist=9.0):
    return {"name": name, "yaw": yaw, "pitch": pitch, "target": list(target), "dist": dist}


def write_scenes():
    four = [view("front", 0, 6), view("side", 90, 4), view("3/4 above", 38, 24), view("back", 180, 6)]
    two = [view("side", 90, 4), view("above 3/4", 55, 28)]
    sc = {
        "charybdis_look": scene("charybdis_spawn", "idle", [0], four, tile=(560, 440)),
        "charybdis_look_gates": scene("charybdis_spawn", "idle", [0], four, texture="charybdis_spawn_gates", tile=(560, 440)),
        "charybdis_head": scene("charybdis_spawn", "idle", [0], [view("head front", 0, 4, (0, 1.4, -1.8), 4.6), view("head 3/4", 40, 12, (0, 1.4, -1.8), 4.6),
                                                                 view("left eye", 90, 4, (0.7, 1.6, -1.5), 3.2), view("right eye", 270, 4, (-0.7, 1.6, -1.5), 3.2)], tile=(480, 420)),
        "charybdis_idle": scene("charybdis_spawn", "idle", [0, 20, 40, 60, 80], two, tile=(420, 330)),
        "charybdis_swim": scene("charybdis_spawn", "walk", [0, 8, 16, 24, 32], two, tile=(420, 330)),
        "charybdis_dart": scene("charybdis_spawn", "run", [0, 5, 10, 15, 20], two, tile=(420, 330)),
        "charybdis_attack": scene("charybdis_spawn", "attack", [4, 8, 12, 16, 22], two, tile=(420, 330)),
        "charybdis_cast": scene("charybdis_spawn", "cast", [0, 10, 20, 30, 40], two, tile=(420, 330)),
        "charybdis_cast_big": scene("charybdis_spawn", "cast_big", [0, 12, 24, 36, 48], two, tile=(420, 330)),
        "charybdis_hurt": scene("charybdis_spawn", "hurt", [0, 3, 6, 9, 12], two, tile=(420, 330)),
        "charybdis_transform": scene("charybdis_spawn", "transform", [0, 15, 30, 45, 58], two, tile=(420, 330)),
        "charybdis_death": scene("charybdis_spawn", "death", [0, 5, 10, 15, 19], two, tile=(420, 330)),
        "magicule_scale": scene("magicule_scale", None, [0], [view("front", 0, 20, (0, 0, 0), 1.6), view("side", 90, 12, (0, 0, 0), 1.6), view("above", 30, 60, (0, 0, 0), 1.6)],
                                tile=(420, 330)),
        "magicule_scale_returned": scene("magicule_scale", None, [0], [view("front", 0, 20, (0, 0, 0), 1.6), view("above", 30, 60, (0, 0, 0), 1.6)],
                                         texture="magicule_scale_returned", tile=(420, 330)),
    }
    with open(SCENES_OUT, "w") as f:
        json.dump(sc, f, indent=1)
    return sc


def main(argv):
    m, pk = build_spawn()
    a = build_anims(m) if "build_anims" in globals() else None
    used = save_skins(m, pk, m.id, m.id + "_gates", a)
    print("charybdis_spawn: %d cubes, %d bones, texture rows used %d of %d" % (m.cube_count(), len(m.bones), used, m.tex_h))
    save_scale()
    write_scenes()


if __name__ == "__main__":
    main(sys.argv[1:])
