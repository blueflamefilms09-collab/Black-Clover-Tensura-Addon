"""Black Clover boss player models (4.13): the shared library of the generator.  See docs/bc_playermodels.md.

What it builds, for one look, is the thing the owner's reference (the No Index mod's anime player model) has: the vanilla player model (a 256x256 painted skin, every model pixel a 4x4 block of
texels) plus LAYERED CUBE PARTS drawn on top of it through the kit's extras (a GeoLite geo.json whose root bones are x_head / x_body / x_arm_r / x_arm_l / x_leg_r / x_leg_l, one atlas texture,
one animation file). This module is the pure part of that: the texture atlas with its procedural material tiles (hair strands with ragged tips, gold, fur, mesh, gems, halo rings, horns, wing
membranes ...), the geo builder (cubes with per-face UV into the atlas, single-face cards, bones, per-cube rotation) and the animation builder (idle sway, walk, run, cast). Numpy + Pillow only,
deterministic (no random numbers: every variation comes from a hash), so the same bytes come out on every run.

Coordinates are the geo file's: pixels (1/16 block), y UP, the model's front on -z, +x the model's left, 24 = the neck line (the head is x -4..4, y 24..32, z -4..4; the body y 12..24; an arm's
pivot is (+-5, 22, 0); a leg's (+-1.9, 12, 0)).
"""
import json
import math
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
TOOLS = os.path.join(ROOT, "tools")
for _p in (os.path.join(TOOLS, "tier_bosses", "hd"), os.path.join(TOOLS, "playermodel"), os.path.join(TOOLS, "tier_bosses"), TOOLS):
    if _p not in sys.path:
        sys.path.insert(0, _p)

import numpy as np  # noqa: E402
from PIL import Image  # noqa: E402

ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse")


# ================================================================================================ colour
def rgb(c):
    """'#rrggbb' or (r, g, b) 0..255 -> float32 (3,) in 0..1."""
    if isinstance(c, np.ndarray):
        return np.asarray(c[:3], np.float32)
    if isinstance(c, str):
        s = c.lstrip("#")
        return np.array([int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16)], np.float32) / 255.0
    return np.array([c[0], c[1], c[2]], np.float32) / 255.0


def hexs(a):
    a = np.clip(np.asarray(a, np.float32) * 255 + 0.5, 0, 255).astype(int)
    return "#%02x%02x%02x" % (a[0], a[1], a[2])


def mix(a, b, t):
    a, b = rgb(a), rgb(b)
    return a + (b - a) * float(t)


_COOL = np.array([70, 60, 130], np.float32) / 255.0
_WARM = np.array([255, 244, 226], np.float32) / 255.0


def tone(c, k):
    """Colour at light k: 1 as is, < 1 a cooler, richer shadow, > 1 a warmer light (the anime hue shift of the HD skins)."""
    c = rgb(c)
    if k < 1.0:
        t = min(0.5, (1.0 - k) * 0.9)
        return np.clip((c + (_COOL - c) * t) * (0.55 + 0.45 * k), 0, 1).astype(np.float32)
    t = min(0.55, (k - 1.0) * 1.6)
    return np.clip(c + (_WARM - c) * t, 0, 1).astype(np.float32)


def hsh(*vals):
    """A deterministic hash in 0..1."""
    h = 2166136261
    for v in vals:
        h ^= int(v * 1000003) & 0xFFFFFFFF
        h = (h * 16777619) & 0xFFFFFFFF
        h ^= h >> 13
    return (h % 100003) / 100003.0


# ================================================================================================ the atlas and its material tiles
class Tile:
    """A rectangle of the atlas (texels) and the glow companion's presence."""
    __slots__ = ("u", "v", "w", "h", "glow")

    def __init__(self, u, v, w, h, glow):
        self.u, self.v, self.w, self.h, self.glow = u, v, w, h, glow


class Atlas:
    """One texture (and its glow map) shared by every cube of a look's extras file. Tiles are cached by key and shelf-packed with a 1 texel padding (the edge texel repeated, so nothing bleeds)."""

    def __init__(self, w=256, h=256):
        self.w, self.h = w, h
        self.img = np.zeros((h, w, 4), np.float32)
        self.glow = np.zeros((h, w, 4), np.float32)
        self.tiles = {}
        self.x = 1
        self.y = 1
        self.row_h = 0

    def _alloc(self, tw, th):
        if self.x + tw + 1 > self.w:
            self.x = 1
            self.y += self.row_h + 1
            self.row_h = 0
        if self.y + th + 1 > self.h:
            raise RuntimeError("atlas full (%dx%d): %d tiles" % (self.w, self.h, len(self.tiles)))
        u, v = self.x, self.y
        self.x += tw + 1
        self.row_h = max(self.row_h, th)
        return u, v

    def tile(self, key, tw, th, painter):
        """The tile of `key` (painted once): painter() -> RGBA float (th, tw, 4), or (rgba, glow rgba)."""
        t = self.tiles.get(key)
        if t is not None:
            return t
        res = painter()
        glow = None
        if isinstance(res, tuple):
            res, glow = res
        assert res.shape == (th, tw, 4), (key, res.shape, (th, tw))
        u, v = self._alloc(tw, th)
        self.img[v:v + th, u:u + tw] = res
        if glow is not None:
            self.glow[v:v + th, u:u + tw] = glow
        t = Tile(u, v, tw, th, glow is not None)
        self.tiles[key] = t
        return t

    def save(self, path, glow_path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        Image.fromarray((np.clip(self.img, 0, 1) * 255 + 0.5).astype(np.uint8), "RGBA").save(path, optimize=True)
        if glow_path is not None and self.glow[..., 3].max() > 0.0:
            a = self.glow
            pm = np.concatenate([a[..., :3] * a[..., 3:4], a[..., 3:4]], axis=-1)
            Image.fromarray((np.clip(pm, 0, 1) * 255 + 0.5).astype(np.uint8), "RGBA").save(glow_path, optimize=True)
            return True
        return False


def _grad(n, stops):
    """A (n,) float ramp through [(t, colour)...] -> (n, 3)."""
    ts = np.array([s[0] for s in stops], np.float32)
    cs = np.stack([rgb(s[1]) for s in stops])
    t = (np.arange(n, dtype=np.float32) + 0.5) / n
    return np.stack([np.interp(t, ts, cs[:, k]) for k in range(3)], axis=-1)


def _edge_outline(a, color, strength=0.7):
    """Darken the opaque texels that touch a transparent one (a thin outline that gives a card its shape)."""
    al = a[..., 3] > 0.5
    pad = np.pad(al, 1, constant_values=False)
    near = ~(pad[:-2, 1:-1] & pad[2:, 1:-1] & pad[1:-1, :-2] & pad[1:-1, 2:])
    edge = al & near
    a = a.copy()
    a[edge, :3] = a[edge, :3] * (1 - strength) + rgb(color) * strength
    return a


# ---------------------------------------------------------------- materials (each returns the RGBA (th, tw, 4) [, glow])
def m_strand(color, tw=12, th=48, seed=0, prongs=3, tip=0.22, light=1.28, dark=0.6, streaks=0.16, outline=0.45):
    """A hair strand card: a dark root fading to the colour and a lit lower half, thin lighter streaks, a ragged lower edge of `prongs` points of different lengths, a darker outline."""
    ramp = _grad(th, [(0.0, tone(color, dark)), (0.18, tone(color, 0.82)), (0.46, rgb(color)), (0.74, tone(color, light)), (1.0, tone(color, 0.95))])
    a = np.zeros((th, tw, 4), np.float32)
    a[..., :3] = ramp[:, None, :]
    a[..., 3] = 1.0
    for x in range(tw):                                           # vertical streaks: some columns lighter, some darker
        k = hsh(x // 2, seed, 7) - 0.5
        a[:, x, :3] = np.clip(a[:, x, :3] * (1.0 + k * streaks * 2.0), 0, 1)
    hx = max(1, int(tw * 0.32))                                    # a sheen line
    for y in range(int(th * 0.12), int(th * 0.8)):
        f = math.sin((y - th * 0.12) / (th * 0.68) * math.pi)
        a[y, hx, :3] = np.clip(a[y, hx, :3] + (tone(color, 1.6) - a[y, hx, :3]) * 0.55 * f, 0, 1)
    pw = max(1, tw // prongs)
    for p in range(prongs):
        x0, x1 = p * pw, tw if p == prongs - 1 else (p + 1) * pw
        end = th * (1.0 - tip * hsh(p, seed, 3))
        start = th * (1.0 - tip * 1.6)
        for x in range(x0, x1):
            u = (x + 0.5 - x0) / max(1.0, x1 - x0)                 # 0..1 across the prong
            cut = end + (th - end) * 0.0
            taper = start + (end - start) * (1.0 - abs(u - 0.5) * 2.0) ** 0.8
            for y in range(th):
                if y > taper:
                    a[y, x, 3] = 0.0
    a[:, 0, 3] = np.where(hsh(seed, 11) > 0.55, a[:, 0, 3], a[:, 0, 3])
    return _edge_outline(a, tone(color, 0.35), outline)


def m_cloth(color, tw=32, th=32, seed=0, trim=None, trim_h=3, hem="straight", folds=4, light=1.18, dark=0.62, emblem=None, emblem_color="#e8b830", fringe=0, fringe_color=None, weave=0.03):
    """A hanging cloth panel: soft vertical folds, a lighter top and darker hem, an optional gold trim band, a hem (straight, scallop, zigzag, ragged), an optional emblem, optional fringe threads."""
    top = rgb(color)
    ramp = _grad(th, [(0.0, tone(color, light)), (0.5, rgb(color)), (1.0, tone(color, dark))])
    a = np.zeros((th, tw, 4), np.float32)
    a[..., :3] = ramp[:, None, :]
    a[..., 3] = 1.0
    for x in range(tw):
        f = math.sin((x + 0.5) / tw * math.pi * folds + seed)
        a[:, x, :3] = np.clip(a[:, x, :3] * (1.0 + 0.10 * f), 0, 1)
    for y in range(th):
        for x in range(tw):
            if weave:
                a[y, x, :3] = np.clip(a[y, x, :3] * (1.0 + weave * (1 if (x + y) % 3 == 0 else -1)), 0, 1)
    if emblem:
        _emblem(a, emblem, rgb(emblem_color), tw / 2.0, th * 0.42, min(tw, th) * 0.26)
    if trim is not None:
        tc = rgb(trim)
        y0 = th - trim_h - (fringe if fringe else 0) - 1
        for y in range(y0, y0 + trim_h):
            k = (y - y0) / max(1.0, trim_h - 1.0)
            c = np.clip(tc * (1.25 - 0.55 * k), 0, 1)
            a[y, :, :3] = c
        a[y0, :, :3] = np.clip(tc * 1.45, 0, 1)
    if hem == "scallop":
        for x in range(tw):
            d = (abs(((x + 0.5) % 8.0) - 4.0) / 4.0) ** 2
            cut = int(th - 3 + 3 * d)
            a[cut:, x, 3] = 0.0
    elif hem == "zigzag":
        for x in range(tw):
            d = abs(((x + 0.5) % 6.0) - 3.0) / 3.0
            a[int(th - 1 - 4 * d):, x, 3] = 0.0
    elif hem == "ragged":
        for x in range(tw):
            a[int(th - 1 - 5 * hsh(x, seed, 5)):, x, 3] = 0.0
    if fringe:
        fc = rgb(fringe_color or trim or color)
        y1 = th - fringe
        a[y1:, :, 3] = 0.0
        for x in range(0, tw, 2):
            ln = fringe * (0.6 + 0.4 * hsh(x, seed, 9))
            for y in range(int(ln)):
                a[y1 + y, x, :3] = np.clip(fc * (1.1 - 0.3 * y / max(1, fringe)), 0, 1)
                a[y1 + y, x, 3] = 1.0
    return _edge_outline(a, tone(color, 0.3), 0.35)


def _emblem(a, kind, c, cx, cy, r):
    """Draws a small emblem into a tile (spade, clover, sun, diamond, eye, cross) in colour c."""
    th, tw = a.shape[:2]
    ys, xs = np.mgrid[0:th, 0:tw].astype(np.float32)
    x, y = (xs + 0.5 - cx) / r, (ys + 0.5 - cy) / r
    if kind == "spade":
        heart = (x * x + (y * 0.9 + 0.35 - np.sqrt(np.abs(x)) * 0.9) ** 2) < 0.55
        top = (np.abs(x) * 1.1 + (y + 0.7)) < 0.0
        m = ((x * x) / 0.55 + ((-y + 0.15)) ** 2 / 0.55 < 1.0) & (y < 0.45)
        stem = (np.abs(x) < 0.16) & (y > 0.2) & (y < 0.95)
        base = (np.abs(x) < 0.5 - (0.95 - y) * 0.0) & (y > 0.85) & (y < 1.0)
        tip = ((np.abs(x) + (0.1 - y) * 0.55) < 0.6) & (y < 0.1)
        m = m | stem | tip
    elif kind == "sun":
        ang = np.arctan2(y, x)
        rr = np.sqrt(x * x + y * y)
        m = (rr < 0.5) | ((rr < 1.0) & (np.cos(ang * 8.0) > 0.55))
    elif kind == "diamond":
        m = (np.abs(x) + np.abs(y)) < 0.9
    elif kind == "eye":
        m = ((np.abs(y) < 0.45 * (1 - x * x)) & (np.abs(x) < 1.0)) & ~((x * x + y * y) < 0.06)
    elif kind == "cross":
        m = ((np.abs(x) < 0.2) & (np.abs(y) < 0.95)) | ((np.abs(y) < 0.2) & (np.abs(x) < 0.7))
    elif kind == "clover":
        m = (((x - 0.35) ** 2 + (y - 0.0) ** 2) < 0.2) | (((x + 0.35) ** 2 + y * y) < 0.2) | ((x * x + (y - 0.35) ** 2) < 0.2) | ((x * x + (y + 0.35) ** 2) < 0.2)
    else:
        m = (x * x + y * y) < 0.6
    a[m, :3] = c
    return a


def m_metal(color="#e8b830", tw=16, th=16, band=True, rivets=True, seed=0, glow=False):
    """Polished gold / steel: a bright top line, a body gradient, a dark horizon line, a warm bounce at the bottom, dark edge lines at the sides, optional rivet dots."""
    ramp = _grad(th, [(0.0, tone(color, 1.55)), (0.18, tone(color, 1.2)), (0.5, rgb(color)), (0.62, tone(color, 0.55)), (0.8, tone(color, 0.8)), (1.0, tone(color, 0.5))])
    a = np.zeros((th, tw, 4), np.float32)
    a[..., :3] = ramp[:, None, :]
    a[..., 3] = 1.0
    a[:, 0, :3] *= 0.7
    a[:, -1, :3] *= 0.7
    if rivets and tw >= 8:
        for x in range(2, tw - 1, 4):
            a[th // 2, x, :3] = tone(color, 1.65)
            a[min(th - 1, th // 2 + 1), x, :3] = tone(color, 0.45)
    if band:
        a[0, :, :3] = tone(color, 1.7)
    return a


def m_fur(color, tw=32, th=16, seed=0, tuft=4, light=1.3, dark=0.62):
    """Fluffy fur: a light top, dark underfur, light and dark strokes, a hem of rounded tufts (alpha cut)."""
    ramp = _grad(th, [(0.0, tone(color, light)), (0.45, rgb(color)), (1.0, tone(color, dark))])
    a = np.zeros((th, tw, 4), np.float32)
    a[..., :3] = ramp[:, None, :]
    a[..., 3] = 1.0
    for y in range(th):
        for x in range(tw):
            n = hsh(x, y // 2, seed, 4)
            if n > 0.72:
                a[y, x, :3] = np.clip(a[y, x, :3] + (tone(color, 1.5) - a[y, x, :3]) * 0.5, 0, 1)
            elif n < 0.22:
                a[y, x, :3] = np.clip(a[y, x, :3] * 0.8, 0, 1)
    for x in range(tw):
        cell = (x + 0.5) % tuft
        d = abs(cell - tuft / 2.0) / (tuft / 2.0)
        cut = int(th - 1 - (tuft * 0.8) * (d ** 2) * (0.7 + 0.5 * hsh(x // tuft, seed, 6)))
        a[cut + 1:, x, 3] = 0.0
    return _edge_outline(a, tone(color, 0.35), 0.4)


def m_mesh(color, tw=16, th=16, seed=0, hole=2, light=1.2, dark=0.7, edge="#e8b830", hem=True):
    """A translucent-looking mesh (a fine net: every other texel is a hole, cutout) with a gold-edged hem; the reference's skirt and veil."""
    ramp = _grad(th, [(0.0, tone(color, light)), (1.0, tone(color, dark))])
    a = np.zeros((th, tw, 4), np.float32)
    a[..., :3] = ramp[:, None, :]
    a[..., 3] = 1.0
    for y in range(th):
        for x in range(tw):
            if (x // 1 + y // 1) % hole == 1:
                a[y, x, 3] = 0.0
    if hem:
        a[th - 2:, :, :3] = rgb(edge)
        a[th - 2:, :, 3] = 1.0
        a[th - 1, :, :3] = tone(edge, 0.6)
    return a


def m_gem(color="#d81c40", tw=8, th=8, glow=True):
    """A faceted stone: a dark rim, a light core, facet lines and a glint; the glow companion lights the core."""
    a = np.zeros((th, tw, 4), np.float32)
    a[..., 3] = 1.0
    ys, xs = np.mgrid[0:th, 0:tw].astype(np.float32)
    x, y = (xs + 0.5 - tw / 2) / (tw / 2), (ys + 0.5 - th / 2) / (th / 2)
    r = np.sqrt(x * x + y * y)
    for k in range(3):
        a[..., k] = np.interp(np.clip(r, 0, 1), [0.0, 0.5, 1.0], [tone(color, 1.5)[k], rgb(color)[k], tone(color, 0.5)[k]])
    f = (np.abs(x) < 0.12) | (np.abs(y) < 0.12) | (np.abs(np.abs(x) - np.abs(y)) < 0.1)
    a[f, :3] *= 0.82
    a[1, 1:3, :3] = 1.0
    a[2, 1, :3] = 1.0
    g = np.zeros((th, tw, 4), np.float32)
    g[..., :3] = rgb(color)[None, None, :]
    g[..., 3] = np.clip(1.0 - r, 0, 1) * 0.9
    return (a, g) if glow else a


def m_ring(color="#fff2a8", tw=64, th=64, rings=2, seed=0, glow=True, spark=True):
    """The halo: concentric thin rings seen from above (a flat quad), bright cores and a sparkle here and there; cutout, with a glow companion that is the same picture."""
    a = np.zeros((th, tw, 4), np.float32)
    ys, xs = np.mgrid[0:th, 0:tw].astype(np.float32)
    x, y = (xs + 0.5 - tw / 2) / (tw / 2), (ys + 0.5 - th / 2) / (th / 2)
    r = np.sqrt(x * x + y * y)
    ang = np.arctan2(y, x)
    for i in range(rings):
        r0 = 0.93 - i * 0.16
        wid = 0.055 if i == 0 else 0.035
        wob = 0.012 * np.sin(ang * (5 + 2 * i) + seed + i)
        d = np.abs(r - r0 - wob)
        core = np.clip(1.0 - d / wid, 0, 1)
        cc = tone(color, 1.0)
        m = d < wid
        shade = np.clip(0.8 + 0.5 * core, 0, 1.4)
        for k in range(3):
            a[..., k] = np.where(m, np.clip(cc[k] * shade, 0, 1), a[..., k])
        a[..., 3] = np.where(m, 1.0, a[..., 3])
    if spark:
        for i in range(10):
            ang0 = hsh(i, seed, 2) * math.tau
            rr = 0.93 - (i % 2) * 0.16
            sx, sy = int(tw / 2 + math.cos(ang0) * rr * tw / 2), int(th / 2 + math.sin(ang0) * rr * th / 2)
            if 0 <= sx < tw and 0 <= sy < th:
                a[sy, sx] = (1, 1, 1, 1)
    g = a.copy()
    return (a, g) if glow else a


def m_horn(color="#2a2230", tw=8, th=24, seed=0, ridges=5, tip="#d8d0e0"):
    """A horn: ridged bands getting lighter toward the tip, a dark root, a pale polished tip."""
    ramp = _grad(th, [(0.0, tone(color, 0.55)), (0.4, rgb(color)), (0.85, tone(color, 1.35)), (1.0, rgb(tip))])
    a = np.zeros((th, tw, 4), np.float32)
    a[..., :3] = ramp[:, None, :]
    a[..., 3] = 1.0
    for y in range(th):
        if y % max(2, th // ridges) == 0:
            a[y, :, :3] *= 0.7
    for x in range(tw):
        f = (x + 0.5) / tw
        a[:, x, :3] = np.clip(a[:, x, :3] * (1.18 - 0.5 * abs(f - 0.3)), 0, 1)
    return _edge_outline(a, tone(color, 0.3), 0.45)


def m_membrane(color="#241a34", vein="#6a4a8a", tw=32, th=32, seed=0, scallops=4, edge="#8a6ab0"):
    """A bat-wing membrane panel: a dark skin, lighter veins fanning out from the top-left, a scalloped lower edge (alpha cut) and a lit rim."""
    ramp = _grad(th, [(0.0, tone(color, 1.1)), (1.0, tone(color, 0.7))])
    a = np.zeros((th, tw, 4), np.float32)
    a[..., :3] = ramp[:, None, :]
    a[..., 3] = 1.0
    ys, xs = np.mgrid[0:th, 0:tw].astype(np.float32)
    ang = np.arctan2(ys + 0.5, tw - xs - 0.5)
    for i in range(scallops + 1):
        t = i / float(scallops)
        d = np.abs(ang - (0.15 + t * 1.25))
        m = d < 0.045
        a[m, :3] = rgb(vein)
    for x in range(tw):                                         # scalloped lower edge
        cell = (x + 0.5) / tw * scallops
        d = abs((cell % 1.0) - 0.5) * 2.0
        cut = int(th - 1 - (th * 0.22) * (1.0 - d ** 2))
        a[cut + 1:, x, 3] = 0.0
        if cut >= 0:
            a[max(0, cut - 1):cut + 1, x, :3] = rgb(edge)
    a[0, :, :3] = rgb(edge)
    return _edge_outline(a, tone(color, 0.3), 0.4)


def m_flat(color, tw=8, th=8, shade=0.18, glow=False):
    """A flat colour with a light top and dark bottom (belts, straps, bandages)."""
    ramp = _grad(th, [(0.0, tone(color, 1.0 + shade)), (1.0, tone(color, 1.0 - shade * 1.4))])
    a = np.zeros((th, tw, 4), np.float32)
    a[..., :3] = ramp[:, None, :]
    a[..., 3] = 1.0
    return a


def m_rune(color="#ffe9a0", tw=16, th=16, seed=0):
    """A small field of glowing marks (a rune band): the glow map is the picture, the base is a dark plate."""
    a = np.zeros((th, tw, 4), np.float32)
    a[..., :3] = np.array([0.08, 0.06, 0.1], np.float32)
    a[..., 3] = 1.0
    g = np.zeros((th, tw, 4), np.float32)
    for x in range(1, tw - 1, 3):
        h = 2 + int(hsh(x, seed, 1) * (th - 6))
        for y in range(th // 2 - h // 2, th // 2 + h // 2):
            g[y, x, :3] = rgb(color)
            g[y, x, 3] = 1.0
        if hsh(x, seed, 2) > 0.5:
            g[th // 2, x + 1, :3] = rgb(color)
            g[th // 2, x + 1, 3] = 1.0
    a[g[..., 3] > 0, :3] = rgb(color) * 0.8
    return a, g


# ================================================================================================ the geo builder
FACES = ("north", "south", "east", "west", "up", "down")


class Geo:
    """One extras file: roots x_head / x_body / x_arm_r / x_arm_l / x_leg_r / x_leg_l, child bones with pivots, cubes with per-face UV into the atlas."""
    PIVOT = {"head": (0.0, 24.0, 0.0), "body": (0.0, 24.0, 0.0), "arm_r": (-5.0, 22.0, 0.0), "arm_l": (5.0, 22.0, 0.0), "leg_r": (-1.9, 12.0, 0.0), "leg_l": (1.9, 12.0, 0.0)}

    def __init__(self, name, atlas):
        self.name = name
        self.atlas = atlas
        self.bones = {}
        self.order = []
        self.cubes = 0
        self.quads = 0
        self.glow_cubes = 0
        self.glow_quads = 0
        self.has_glow = False

    def root(self, anchor):
        n = "x_" + anchor
        if n not in self.bones:
            self.bones[n] = {"name": n, "pivot": list(self.PIVOT[anchor]), "cubes": []}
            self.order.append(n)
        return n

    def bone(self, name, parent, pivot, rotation=None):
        """A child bone (an animation target): absolute pivot; `parent` is a root name ('head' etc. for a root) or another bone."""
        if name in self.bones:
            return name
        if parent in self.PIVOT:
            parent = self.root(parent)
        b = {"name": name, "parent": parent, "pivot": [round(float(v), 4) for v in pivot], "cubes": []}
        if rotation:
            b["rotation"] = [round(float(v), 4) for v in rotation]
        self.bones[name] = b
        self.order.append(name)
        return name

    def cube(self, bone, origin, size, tile, faces=FACES, rot=None, pivot=None, mirror_uv=False, uv_scale=None):
        """A cube with its min corner `origin` and `size` (pixels). `tile` is a Tile (all chosen faces show the whole tile; the top and bottom faces take its first / last two rows) or a dict face -> (u, v, w, h)."""
        if bone in self.PIVOT:
            bone = self.root(bone)
        uv = {}
        for f in faces:
            if isinstance(tile, dict):
                r = tile.get(f)
                if r is None:
                    continue
                u, v, w, h = r
            elif f == "up":
                u, v, w, h = tile.u, tile.v, tile.w, min(2, tile.h)
            elif f == "down":
                u, v, w, h = tile.u, tile.v + tile.h - min(2, tile.h), tile.w, min(2, tile.h)
            else:
                u, v, w, h = tile.u, tile.v, tile.w, tile.h
            uv[f] = {"uv": [u, v], "uv_size": [w, h]}
        c = {"origin": [round(float(o), 4) for o in origin], "size": [round(float(s), 4) for s in size], "uv": uv}
        if rot is not None and any(abs(r) > 1e-6 for r in rot):
            c["rotation"] = [round(float(r), 4) for r in rot]
            p = pivot if pivot is not None else [origin[i] + size[i] / 2.0 for i in range(3)]
            c["pivot"] = [round(float(v), 4) for v in p]
        if mirror_uv:
            c["mirror"] = True
        self.bones[bone]["cubes"].append(c)
        self.cubes += 1
        self.quads += len(uv)
        if not isinstance(tile, dict) and getattr(tile, "glow", False):
            self.has_glow = True
            self.glow_cubes += 1
            self.glow_quads += len(uv)
        return c

    def card(self, bone, x, y_top, z, w, length, tile, rot=None, pivot=None, thick=0.0):
        """A single-face hanging card (front and back both show: the cutout type draws both sides): `w` wide, `length` long, hanging from y_top at depth z, centred on x."""
        return self.cube(bone, (x - w / 2.0, y_top - length, z - thick / 2.0), (w, length, thick), tile, faces=("north",), rot=rot,
                         pivot=pivot if pivot is not None else (x, y_top, z))

    def to_json(self):
        bones = []
        for n in self.order:
            b = dict(self.bones[n])
            bones.append(b)
        # roots first, then children in creation order (a child never precedes its parent: bones are created parent first)
        return {"format_version": "1.12.0", "minecraft:geometry": [{"description": {"identifier": "geometry." + self.name, "texture_width": self.atlas.w, "texture_height": self.atlas.h,
                                                                                    "visible_bounds_width": 6, "visible_bounds_height": 6, "visible_bounds_offset": [0, 1.5, 0]}, "bones": bones}]}


# ================================================================================================ animation
class Anim:
    """The animation file: idle (loops), walk, run, cast, cast_big. A track is a swing of a bone about its pivot (degrees), phase shifted per bone so strands do not move as one."""

    def __init__(self):
        self.tracks = {}      # bone -> dict(kind params)

    def sway(self, bone, ax=2.0, az=1.5, phase=0.0, walk=1.0, run=2.0, cast=1.0, spin=0.0, bob=0.0):
        self.tracks[bone] = dict(ax=ax, az=az, phase=phase, walk=walk, run=run, cast=cast, spin=spin, bob=bob)

    def _clip(self, length, loop, scale_fn):
        bones = {}
        for b, p in self.tracks.items():
            kf, pos = {}, {}
            steps = 8
            for i in range(steps + 1):
                t = length * i / steps
                th = (i / steps) * math.tau + p["phase"]
                rx, rz = scale_fn(p, th, i / steps)
                kf["%.3f" % t] = [round(rx, 3), round(p["spin"] * (i / steps) * 360.0 if p["spin"] else 0.0, 3), round(rz, 3)]
                if p["bob"]:
                    pos["%.3f" % t] = [0.0, round(p["bob"] * math.sin(th), 3), 0.0]
            e = {"rotation": kf}
            if pos:
                e["position"] = pos
            bones[b] = e
        return {"loop": loop, "animation_length": length, "bones": bones}

    def to_json(self):
        idle = self._clip(3.2, True, lambda p, th, f: (p["ax"] * math.sin(th), p["az"] * math.sin(th * 1.0 + 1.3)))
        walk = self._clip(1.1, True, lambda p, th, f: (-p["walk"] * 5.0 + p["ax"] * 0.8 * math.sin(th), p["az"] * 0.8 * math.sin(th + 0.6)))
        run = self._clip(0.62, True, lambda p, th, f: (-p["run"] * 7.0 + p["ax"] * 1.2 * math.sin(th), p["az"] * 1.0 * math.sin(th + 0.6)))
        cast = self._clip(1.2, True, lambda p, th, f: (p["cast"] * 6.0 * math.sin(th) + p["ax"] * 0.5, p["az"] * 1.6 * math.sin(th + 0.9)))
        big = self._clip(2.4, False, lambda p, th, f: (p["cast"] * 12.0 * math.sin(f * math.pi) , p["az"] * 2.4 * math.sin(th * 2.0)))
        return {"format_version": "1.8.0", "animations": {"idle": idle, "walk": walk, "run": run, "cast": cast, "cast_big": big}}


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, separators=(",", ":"))
