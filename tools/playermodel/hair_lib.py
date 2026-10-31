"""Why: the hair system of the player-model kit (client/playermodel, docs/playermodel.md) draws every haircut as layered boxes slightly larger than the head's hat layer, each with a
box-UV region of a 64x64 hair sheet, and moves the hanging locks on springs. A haircut is therefore two things that must agree: the boxes (geometry, written to
assets/nusmp/playermodel/hair_styles.json, which the Java side reads) and the painted sheet of that style (assets/nusmp/textures/entity/playermodel/hair/<style>.png). This module is the
shared part of the generator: a packer that gives every box its UV region (the scalp cap always sits where the hat layer of a skin is, u 32 v 0), the strand painters that carve a
haircut out of a grey sheet with an alpha mask (the game tints it with the character's hair colour, so the sheet is grey and bright), and the writer.

Frame (model pixels, y DOWN, front = -z, +x = the entity's left, the head box is x -4..4, y -8..0, z -4..4, the neck line is y 0), exactly the player model's own.
A scalp cell is (lx, ly, lz): lx 0..7 from the entity's right, ly 0..7 from the top of the head, lz 0..7 from the front.
"""
import json
import math
import os
import sys

sys.dont_write_bytecode = True

import numpy as np
from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
SHEET = 64


def hash01(*vals):
    h = 2166136261
    for v in vals:
        h = ((h ^ (int(v) & 0xFFFFFFFF)) * 16777619) & 0xFFFFFFFF
        h ^= h >> 13
    return (h & 0xFFFF) / 65535.0


# ---------------------------------------------------------------- the faces of a box, as vanilla lays them out
def regions(u, v, w, h, d):
    """Texel rectangles (x, y, w, h) of the six faces of a box with its texture origin at (u, v): the layout of vanilla's ModelPart.Cube."""
    return {
        "west": (u, v + d, d, h), "front": (u + d, v + d, w, h), "east": (u + d + w, v + d, d, h), "back": (u + d + w + d, v + d, w, h),
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
    }


def footprint(w, h, d):
    return int(2 * (w + d)), int(d + h)


class Sheet:
    """The painted 64x64 sheet of one style and its shelf packer. The cap region (u 32, v 0, 32 x 16) is reserved first: it is the hat layer's place."""

    def __init__(self, name):
        self.name = name
        self.px = np.zeros((SHEET, SHEET, 4), dtype=np.float32)       # grey value 0..1 in r=g=b, alpha 0..1
        self.used = np.zeros((SHEET, SHEET), dtype=bool)
        self.shared = {}

    def free(self, x, y, w, h):
        return x + w <= SHEET and y + h <= SHEET and not self.used[y:y + h, x:x + w].any()

    def take(self, x, y, w, h):
        self.used[y:y + h, x:x + w] = True

    def alloc(self, w, h, d, key=None, at=None):
        """Finds a free region for a box of w x h x d texels; the same key gives the same region (shared strands). Returns (u, v)."""
        fw, fh = footprint(w, h, d)
        if key is not None and key in self.shared:
            return self.shared[key], False
        if at is not None:
            u, v = at
            if not self.free(u, v, fw, fh):
                raise ValueError("%s: the reserved region %s is taken" % (self.name, at))
            self.take(u, v, fw, fh)
            if key is not None:
                self.shared[key] = (u, v)
            return (u, v), True
        for y in range(0, SHEET - fh + 1):
            for x in range(0, SHEET - fw + 1):
                if self.free(x, y, fw, fh):
                    self.take(x, y, fw, fh)
                    if key is not None:
                        self.shared[key] = (x, y)
                    return (x, y), True
        raise ValueError("%s: no room for a %dx%dx%d box (sheet is full)" % (self.name, w, h, d))

    def image(self):
        a = np.clip(self.px * 255.0 + 0.5, 0, 255).astype(np.uint8)
        return Image.fromarray(a, "RGBA")


# ---------------------------------------------------------------- strand painting
def strand_grey(seed, col, row, rows, flow="down", tip=0.0):
    """The grey of one texel of a strand sheet: strands run along the flow, each column has its own brightness, a sheen band sits near the top, the tips darken."""
    n1 = hash01(seed, col, 1)
    n2 = hash01(seed, col // 2, 7)
    base = 0.76 + 0.24 * (n1 - 0.5) + 0.12 * (n2 - 0.5)
    t = row / max(1.0, rows - 1.0)
    sheen = 0.10 * math.exp(-((t - 0.22) ** 2) / 0.02)
    root = -0.08 * max(0.0, 1.0 - t * 4.0)
    ends = -0.10 * max(0.0, t - 0.7) * (1.0 + tip)
    streak = 0.08 * (1.0 if hash01(seed, col, row // 3) > 0.8 else 0.0) - 0.07 * (1.0 if hash01(seed, col + 5, row // 2) > 0.9 else 0.0)
    return float(min(1.0, max(0.25, base + sheen + root + ends + streak)))


# ---------------------------------------------------------------- anime strands (3.37, "anime skin style v2"): clumps with cel-shaded light-to-shade steps
ANIME_LEVELS = (0.58, 0.76, 0.91, 1.0)           # the grey steps of an anime sheet: deep shade, shade, base, highlight (the game's tint multiplies them)


def _q(g):
    """Snaps a grey to the nearest anime step (cel shading: clusters, not a smooth gradient)."""
    return min(ANIME_LEVELS, key=lambda lv: abs(lv - g))


def clump_of(seed, col):
    """The clump a strand column belongs to and the column's place in it: clumps are 2 to 4 strands wide, mostly 3, the boundaries are hashed (so two sheets never repeat each other)."""
    start = 0
    c = 0
    for i in range(1, int(col) + 1):
        if i - start >= 4 or (i - start >= 3 and hash01(seed, i, 11) > 0.35) or (i - start == 2 and hash01(seed, i, 11) > 0.8):
            start = i
            c += 1
    return c, int(col) - start


def clump_grey(seed, col, row, rows, tip=0.0):
    """The grey of one texel of an anime strand sheet: strands run down in clumps; every clump has its own base step, a bright root band, a shaded lower half, a darker left edge (the gap to the
    next clump) and a lit right edge, a few bright highlight pixels and a few shade pixels; the tips darken. Four steps only."""
    c, pos = clump_of(seed, col)
    t = row / max(1.0, rows - 1.0)
    base = 0.86 + 0.20 * (hash01(seed, c, 5) - 0.5)
    g = base
    g += 0.16 * math.exp(-((t - 0.16) ** 2) / 0.012)             # the lit band near the root
    g -= 0.26 * max(0.0, t - 0.55) / 0.45                           # the shaded lower half
    g -= 0.10 * max(0.0, t - 0.82) * (1.0 + tip) / 0.18             # the tips
    if pos == 0:
        g -= 0.17                                                   # the gap between two clumps
    elif pos >= 1 and hash01(seed, c, 9) > 0.5:
        g += 0.05
    h = hash01(seed, col, row + 31)
    if h > 0.965 and t < 0.65:
        g = 1.04                                                    # a bright highlight pixel
    elif h < 0.025 and t > 0.25:
        g -= 0.2                                                    # a shade pixel
    return float(_q(min(1.04, max(0.35, g))))


def fill_face(sheet, rect, grey_fn, alpha_fn=None):
    x0, y0, w, h = rect
    for y in range(h):
        for x in range(w):
            a = 1.0 if alpha_fn is None else alpha_fn(x, y, w, h)
            if a <= 0.0:
                continue
            g = grey_fn(x, y, w, h)
            sheet.px[y0 + y, x0 + x] = (g, g, g, a)


def paint_block(sheet, u, v, w, h, d, seed, taper="none", pattern="strand", rows=None, tip=0.0, anime=False):
    """Paints a plain block of hair: every face a strand sheet; `taper` carves the alpha ('point_down': the lower rows narrow to a point; 'point_up'; 'round' for curls; 'ends': the bottom
    corners are cut), `pattern` 'strand' (vertical strands), 'plait' (diagonal braid pattern), 'curl' (rings), 'solid' (a tie / band)."""
    rg = regions(u, v, w, h, d)
    for face, rect in rg.items():
        rows_n = rect[3] if face not in ("top", "bottom") else rect[3]

        def grey(x, y, ww, hh, face=face):
            if pattern == "plait":
                k = ((x + y) // 2) % 2
                return (0.92 if k == 0 else 0.62) * (0.9 + 0.1 * hash01(seed, x, y))
            if pattern == "curl":
                k = (y % 3)
                return (0.92, 0.72, 0.84)[k] * (0.9 + 0.1 * hash01(seed, x, y))
            if pattern == "solid":
                return 0.82 + 0.1 * hash01(seed, x, y)
            if anime:
                if face in ("top", "bottom"):
                    return clump_grey(seed, x, y, hh, tip=tip) * 0.96
                return clump_grey(seed, x + (0 if face != "back" else 3), y, hh, tip=tip)
            if face in ("top", "bottom"):
                return strand_grey(seed, x, y, hh, tip=tip)
            return strand_grey(seed, x + (0 if face != "back" else 3), y, hh, tip=tip)

        def alpha(x, y, ww, hh, face=face):
            if taper == "zigzag" and face == "bottom":
                return 0.0                              # (anime) the underside of a jagged slab is never drawn
            if face in ("top", "bottom"):
                return 1.0
            if taper == "point_down":
                half = (ww - 1) / 2.0
                lim = half * (1.0 - max(0.0, (y - hh * 0.35) / (hh * 0.65)))
                return 1.0 if abs(x - half) <= lim + 0.5 else 0.0
            if taper == "point_up":
                half = (ww - 1) / 2.0
                lim = half * (1.0 - max(0.0, ((hh - 1 - y) - hh * 0.35) / (hh * 0.65)))
                return 1.0 if abs(x - half) <= lim + 0.5 else 0.0
            if taper == "ends":
                if y >= hh - 1 and (x == 0 or x == ww - 1) and ww > 2:
                    return 0.0
                return 1.0
            if taper == "zigzag":                       # (anime) a jagged lower edge: every other bottom texel is cut, the next to last one every third
                if y == hh - 1 and ww > 1 and x % 2 == 1:
                    return 0.0
                if y == hh - 2 and hh > 3 and x % 3 == 1:
                    return 0.0
                return 1.0
            if taper == "round":
                cx, cy = (ww - 1) / 2.0, (hh - 1) / 2.0
                return 1.0 if ((x - cx) / max(0.5, ww / 2.0)) ** 2 + ((y - cy) / max(0.5, hh / 2.0)) ** 2 <= 1.15 else 0.0
            return 1.0

        fill_face(sheet, rect, grey, alpha)


# ---------------------------------------------------------------- spikes and rolls (3.37 hair rework)
def spike_grey(seed, face, x, y, fw, fh):
    """The grey of one texel of a spike section's side face (y = 0 is the tip end): strands run along the spike, in clumps; the left of a face is lit, the right shaded (a rounded clump),
    the tip end is lit and the root end dark, so the stacked sections read as layered clumps with a dark gap between them. Four grey steps (ANIME_LEVELS)."""
    xn = (x + 0.5) / max(1.0, fw)
    t = y / max(1.0, fh - 1.0)
    c, pos = clump_of(seed, x)
    g = 0.90 + 0.12 * (hash01(seed, c, 5) - 0.5)
    g += 0.20 * (0.5 - xn)                                          # the clump's rounding: lit left, shaded right
    g += 0.07 * (1.0 - t) ** 2                                      # the lit tip end
    g -= 0.30 * max(0.0, t - 0.5) / 0.5                             # the shaded root end
    if pos == 0 and c > 0:
        g -= 0.13                                                   # the gap between two strands clumps
    if pos == 1 and hash01(seed, c, 3) > 0.5 and 0.08 < t < 0.7:
        g += 0.09                                                   # a lit strand
    if y == fh - 1:
        g -= 0.2                                                    # the dark line where the section meets the one below
    h = hash01(seed, x * 7 + len(face), y + 5)
    if h > 0.95 and t < 0.6:
        g = 1.04
    return float(_q(min(1.04, max(0.35, g))))


def paint_spike(sheet, u, v, w, h, d, seed, ratio):
    """A section of a spike: its sides are trapezoids that narrow from the full width at the root (the bottom row) to `ratio` of it at the tip end (the top row; 0 is a point), the top face
    keeps only the middle `ratio` of its footprint (a point's top is one texel), the bottom is never drawn (it is inside the hair below)."""
    rg = regions(u, v, w, h, d)
    for face, rect in rg.items():
        x0, y0, fw, fh = rect
        if face == "bottom":
            continue
        for yy in range(fh):
            for xx in range(fw):
                if face == "top":
                    lim_x = max(0.5, ratio * (fw - 1) / 2.0 + 0.5)
                    lim_y = max(0.5, ratio * (fh - 1) / 2.0 + 0.5)
                    if abs(xx - (fw - 1) / 2.0) > lim_x or abs(yy - (fh - 1) / 2.0) > lim_y:
                        continue
                    g = min(1.0, 0.95 + 0.08 * hash01(seed, xx, yy))
                    g = _q(g)
                else:
                    half = (fw - 1) / 2.0
                    t = yy / max(1.0, fh - 1.0)
                    lim = half * (ratio + (1.0 - ratio) * t)
                    if abs(xx - half) > lim + 0.5:
                        continue
                    g = spike_grey(seed, face, xx, yy, fw, fh)
                sheet.px[y0 + yy, x0 + xx] = (g, g, g, 1.0)


def ring_grey(seed, x, y, fw, fh, light=1.0):
    """The grey of one texel of a rolled curl's face: concentric bands round the face's centre (the spiral of a curl, four steps), lit towards the top left, a bright highlight pixel."""
    cx, cy = (fw - 1) / 2.0, (fh - 1) / 2.0
    r = math.hypot((x - cx) / max(0.5, fw / 2.0), (y - cy) / max(0.5, fh / 2.0))
    band = int(r * 3.2 + 0.35 * math.sin(math.atan2(y - cy, x - cx) * 1.0 + hash01(seed, 1, 1) * 6.0)) % 3
    g = (0.78, 0.97, 0.66)[band]
    g += 0.08 * (-(x - cx) / max(1.0, fw) - (y - cy) / max(1.0, fh))
    g *= light
    if hash01(seed, x, y + 9) > 0.96 and r < 0.85:
        g = 1.04
    return float(_q(min(1.04, max(0.35, g))))


def paint_curl(sheet, u, v, w, h, d, seed, round_cut=True):
    """A rolled curl: every face is a disc of concentric bands (a cube with six round faces reads as a ball), the bottom face a little darker."""
    rg = regions(u, v, w, h, d)
    for face, rect in rg.items():
        x0, y0, fw, fh = rect
        for yy in range(fh):
            for xx in range(fw):
                cx, cy = (fw - 1) / 2.0, (fh - 1) / 2.0
                if round_cut and ((xx - cx) / max(0.5, fw / 2.0)) ** 2 + ((yy - cy) / max(0.5, fh / 2.0)) ** 2 > 1.22:
                    continue
                g = ring_grey(seed + len(face), xx, yy, fw, fh, 0.9 if face == "bottom" else 1.0)
                sheet.px[y0 + yy, x0 + xx] = (g, g, g, 1.0)


def paint_mass(sheet, u, v, w, h, d, seed, round_cut=0.0, flat_top=False):
    """A chunk of a head of hair (an afro tier, a flat top): clump strands on the sides, a lit top with wedge clumps; `round_cut` > 0 cuts the corners of the side faces (an ellipse), so a
    stack of tiers reads as a dome. The bottom is never drawn."""
    rg = regions(u, v, w, h, d)
    for face, rect in rg.items():
        x0, y0, fw, fh = rect
        if face == "bottom":
            continue
        for yy in range(fh):
            for xx in range(fw):
                if round_cut > 0.0 and face != "top":
                    cx, cy = (fw - 1) / 2.0, (fh - 1) / 2.0
                    ex = abs(xx - cx) / max(0.5, fw / 2.0)
                    ey = abs(yy - cy) / max(0.5, fh / 2.0)
                    if ex ** round_cut + ey ** round_cut > 1.0 + 0.5 / max(1.0, min(fw, fh)):
                        continue
                if face == "top":
                    ang = math.atan2(xx - (fw - 1) / 2.0, yy - (fh - 1) / 2.0)
                    wedge = int((ang + math.pi) / (2 * math.pi) * 12)
                    g = 0.92 + 0.14 * (hash01(seed, wedge, 3) - 0.5) - (0.12 if wedge % 3 == 0 else 0.0)
                    if hash01(seed, xx, yy + 40) > 0.93:
                        g = 1.04
                    g = _q(min(1.04, max(0.4, g)))
                else:
                    g = clump_grey(seed, xx + (3 if face == "back" else 0), yy, fh, tip=0.0)
                sheet.px[y0 + yy, x0 + xx] = (g, g, g, 1.0)


# ---------------------------------------------------------------- the scalp cap (hat-layer UV)
def cell_of(face, x, y):
    """The scalp cell (lx, ly, lz) a texel of the 8 x 8 x 8 cap's face covers."""
    if face == "front":
        return x, y, 0
    if face == "back":
        return 7 - x, y, 7
    if face == "west":
        return 0, y, 7 - x
    if face == "east":
        return 7, y, x
    if face == "top":
        return x, 0, 7 - y
    return x, 7, y                      # bottom


def anime_cap_grey(seed, face, lx, ly, lz):
    """(anime) the grey of one scalp texel: the top is wedges of clumps round the crown with a bright arc across the front (the halo of an anime head), the fringe row is lit and the rows
    below it shaded, the sides and the back are clumps running down with the lit band at the top."""
    if face == "top":
        ang = math.atan2(lx - 3.5, lz - 3.5)
        wedge = int((ang + math.pi) / (2 * math.pi) * 14)
        dist = math.hypot(lx - 3.5, lz - 3.5)
        g = 0.78 + 0.2 * (hash01(seed, wedge, 3) - 0.5)
        if lz <= 3 and 1.6 <= dist <= 3.4:
            g += 0.2                                       # the lit arc over the forehead
        if wedge % 3 == 0:
            g -= 0.14                                      # a gap between two wedges
        if hash01(seed, lx, lz + 40) > 0.94:
            g = 1.04
        return float(_q(min(1.04, max(0.4, g))))
    col = lx + lz * 2 if face in ("west", "east") else lx + 3 * (face == "back")
    return clump_grey(seed, col, ly, 8, tip=0.3)


def paint_cap(sheet, mask, seed, u=32, v=0, anime=False):
    """Paints the 8x8x8 scalp cap at the hat layer's place. `mask(face, lx, ly, lz)` says whether the hair covers that cell of the head's surface (the bottom is never covered)."""
    rg = regions(u, v, 8, 8, 8)
    for face, rect in rg.items():
        if face == "bottom":
            continue

        def alpha(x, y, w, h, face=face):
            lx, ly, lz = cell_of(face, x, y)
            return 1.0 if mask(face, lx, ly, lz) else 0.0

        def grey(x, y, w, h, face=face):
            lx, ly, lz = cell_of(face, x, y)
            if anime:
                return anime_cap_grey(seed, face, lx, ly, lz)
            if face == "top":                    # radial strands from the crown
                ang = math.atan2(lx - 3.5, lz - 3.5)
                col = int((ang + math.pi) / (2 * math.pi) * 22)
                dist = math.hypot(lx - 3.5, lz - 3.5)
                g = 0.80 + 0.12 * (hash01(seed, col, 3) - 0.5) + 0.05 * math.exp(-((dist - 2.4) ** 2) / 1.6)
                return min(1.0, max(0.3, g))
            col = lx + lz * 2 if face in ("west", "east") else lx + 3 * (face == "back")
            return strand_grey(seed, col, ly, 8, tip=0.3)

        fill_face(sheet, rect, grey, alpha)


# ---------------------------------------------------------------- the style being built
class Style:
    def __init__(self, sid, anime=False):
        self.id = sid
        self.anime = anime                       # 3.37: an anime-style sheet (clumps, four grey steps); the geometry JSON is the same format
        self.sheet = Sheet(sid)
        self.boxes = []
        self.chains = []
        self.seed = int(hash01(len(sid), sum(map(ord, sid))) * 100000)
        self.cap_reserved = False

    def cap(self, mask, inflate=0.7, tone=0, helm="hide", name="cap"):
        """The scalp cap: an 8x8x8 box at the hat layer's UV with the haircut's alpha mask."""
        (u, v), fresh = self.sheet.alloc(8, 8, 8, key="cap", at=(32, 0))
        self.mask = mask
        paint_cap(self.sheet, mask, self.seed + 1, anime=self.anime)
        self.boxes.append({"name": name, "part": "head", "from": [-4, -8, -4], "size": [8, 8, 8], "inflate": inflate, "uv": [u, v], "tone": tone, "helm": helm})

    def _paint(self, u, v, uw, uh, ud, sd, taper, pattern, tip):
        if pattern == "spike":                              # 3.37 hair rework: a spike section; `taper` is "pt<ratio>" (the tip end's share of the root's width)
            paint_spike(self.sheet, u, v, uw, uh, ud, sd, float(taper[2:]))
        elif pattern == "curl3d":                           # a rolled curl: round discs of concentric bands on all six faces
            paint_curl(self.sheet, u, v, uw, uh, ud, sd)
        elif pattern == "mass":                             # a tier of an afro / a flat top: `taper` is "m<round exponent>" ("m0" no cut, "m2.4" a rounded dome)
            paint_mass(self.sheet, u, v, uw, uh, ud, sd, round_cut=float(taper[1:]) if taper[:1] == "m" else 0.0)
        else:
            paint_block(self.sheet, u, v, uw, uh, ud, sd, taper=taper, pattern=pattern, tip=tip, anime=self.anime)

    def box(self, name, frm, size, inflate=0.2, tone=0, part="head", rot=None, pivot=None, helm="hide", taper="none", pattern="strand", key=None, tip=0.0, uvsize=None):
        w, h, d = size
        uw, uh, ud = uvsize or (max(1, int(round(w))), max(1, int(round(h))), max(1, int(round(d))))
        k = key if key is not None else "%s:%s:%s:%s:%s" % (uw, uh, ud, taper, pattern)
        (u, v), fresh = self.sheet.alloc(uw, uh, ud, key=k)
        if fresh:
            self._paint(u, v, uw, uh, ud, self.seed + len(self.boxes) * 7, taper, pattern, tip)
        b = {"name": name, "part": part, "from": [round(c, 3) for c in frm], "size": [round(c, 3) for c in size], "inflate": inflate, "uv": [u, v], "tone": tone, "helm": helm}
        if (uw, uh, ud) != (w, h, d):
            b["uvsize"] = [uw, uh, ud]
        if rot:
            b["rot"] = [round(c, 3) for c in rot]
            b["pivot"] = [round(c, 3) for c in (pivot if pivot else (frm[0] + w / 2, frm[1] + h, frm[2] + d / 2))]
        self.boxes.append(b)

    def chain(self, name, root, segments, tuning="head", params="default", phase=0.0, helm="tail", pattern="strand", taper_last="point_down", tones=None, shared_key=None, mirror_of=None):
        """A hanging lock. `segments` is a list of (width, length, depth, part, inflate) from the root down; `root` is where the first segment hangs from (in `part` space)."""
        segs = []
        n = len(segments)
        for i, (w, ln, d, part, infl) in enumerate(segments):
            last = i == n - 1
            taper = taper_last if last else "none"
            uw, uh, ud = max(1, int(round(w))), max(1, int(round(ln))), max(1, int(round(d)))
            key = "%s:%s:%s:%s:%s:%d" % (shared_key if shared_key else name, uw, uh, ud, taper, i) if not mirror_of else "%s:%s:%s:%s:%s:%d" % (mirror_of, uw, uh, ud, taper, i)
            (u, v), fresh = self.sheet.alloc(uw, uh, ud, key=key)
            if fresh:
                self._paint(u, v, uw, uh, ud, self.seed + 31 * (i + 1) + len(self.chains) * 13, taper, pattern, 0.6 if last else 0.0)
            tone = (tones[i] if tones else (0 if i % 2 == 0 else 1)) if pattern == "plait" else (tones[i] if tones else 0)
            seg = {"part": part, "size": [round(w, 3), round(ln, 3), round(d, 3)], "inflate": infl, "uv": [u, v], "tone": tone}
            if (uw, uh, ud) != (round(w), round(ln), round(d)):
                seg["uvsize"] = [uw, uh, ud]
            segs.append(seg)
        self.chains.append({"name": name, "tuning": tuning, "params": params, "phase": phase, "root": [round(c, 3) for c in root], "helm": helm, "segments": segs})

    def spec(self):
        return {"texture": "hair/" + self.id, "boxes": self.boxes, "chains": self.chains}

    def save_sheet(self, folder):
        os.makedirs(folder, exist_ok=True)
        path = os.path.join(folder, self.id + ".png")
        img = self.sheet.image()
        if os.path.isfile(path):                 # 3.37: an unchanged sheet is left alone (the committed PNGs are optimised; rewriting them would only change their bytes)
            try:
                if np.array_equal(np.array(Image.open(path).convert("RGBA")), np.array(img)):
                    return
            except OSError:
                pass
        img.save(path)


def write_styles(styles, order, out_json, meta=None):
    doc = {"version": 1, "sheet": SHEET, "styles": {}}
    if meta:
        doc.update(meta)
    for sid in order:
        doc["styles"][sid] = styles[sid].spec()
    os.makedirs(os.path.dirname(out_json), exist_ok=True)
    with open(out_json, "w") as f:
        json.dump(doc, f, indent=1)
        f.write("\n")


# ---------------------------------------------------------------- the attachment check (3.37 hair rework: nothing floats)
# Every box and every chain segment of a style is an oriented box in the head's frame (a rotated box turns about its pivot, vanilla order Rz Ry Rx; a chain segment hangs from its pivot at
# rest). A piece is ATTACHED when the exact distance between it and the scalp cap, or a piece that is already attached, is at most ATTACH_GAP model pixels (0 when they overlap). A static box
# must also be EMBEDDED: it overlaps its parent by at least EMBED_DEPTH pixels (the smallest overlap over the 15 separating axes of two boxes), so a spike stands IN the hair, not on one
# corner of it. The same maths is in the JUnit test client/playermodel/HairAttachedTest.java (read it together with this).
ATTACH_GAP = 0.1
EMBED_DEPTH = 0.4
SIDE_Y = -8.0           # a static box with any part lower than this (beside the head, not above it) must touch the scalp cap itself: a piece that hangs off another piece beside the head floats


def rot_zyx(rx, ry, rz):
    """The 3x3 rotation of a vanilla model part: Rz * Ry * Rx, angles in degrees."""
    rx, ry, rz = math.radians(rx), math.radians(ry), math.radians(rz)
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    Rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]], float)
    Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]], float)
    Rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]], float)
    return Rz @ Ry @ Rx


class Obb:
    """An oriented box: centre c, half extents h, axes = the columns of R."""
    __slots__ = ("name", "c", "h", "R", "rotated", "chain", "index", "verts")

    def __init__(self, name, c, h, R, rotated=False, chain=None, index=0):
        self.name, self.c, self.h, self.R, self.rotated, self.chain, self.index = name, np.array(c, float), np.array(h, float), np.array(R, float), rotated, chain, index
        self.verts = np.array([self.c + self.R @ (self.h * np.array([sx, sy, sz])) for sx in (-1, 1) for sy in (-1, 1) for sz in (-1, 1)])

    def aabb(self):
        return self.verts.min(axis=0), self.verts.max(axis=0)


def pieces_of(spec):
    """The oriented boxes of a style spec (the dict a Style.spec() / hair_styles.json entry holds), boxes first, then every chain segment at rest."""
    out = []
    for b in spec["boxes"]:
        f, s, infl = np.array(b["from"], float), np.array(b["size"], float), float(b.get("inflate", 0.0))
        c, R, rotated = f + s / 2.0, np.eye(3), False
        if b.get("rot"):
            pv = np.array(b.get("pivot") or (f[0] + s[0] / 2, f[1] + s[1], f[2] + s[2] / 2), float)
            R = rot_zyx(*b["rot"])
            c = pv + R @ (c - pv)
            rotated = True
        out.append(Obb(b["name"], c, s / 2.0 + infl, R, rotated))
    for ch in spec["chains"]:
        y = float(ch["root"][1])
        for i, sg in enumerate(ch["segments"]):
            w, ln, d = sg["size"]
            infl = float(sg.get("inflate", 0.0))
            out.append(Obb("%s#%d" % (ch["name"], i), (ch["root"][0], y + ln / 2.0, ch["root"][2]), (w / 2.0 + infl, ln / 2.0 + infl, d / 2.0 + infl), np.eye(3), False, ch["name"], i))
            y += ln
    return out


def _aabb_gap(a, b):
    lo_a, hi_a = a.aabb()
    lo_b, hi_b = b.aabb()
    g = np.maximum(0.0, np.maximum(lo_b - hi_a, lo_a - hi_b))
    return float(np.linalg.norm(g))


def _point_obb(p, o):
    q = o.R.T @ (np.asarray(p, float) - o.c)
    return float(np.linalg.norm(q - np.clip(q, -o.h, o.h)))


def _seg_seg(p1, q1, p2, q2):
    d1, d2, r = q1 - p1, q2 - p2, p1 - p2
    a, e, f = float(d1 @ d1), float(d2 @ d2), float(d2 @ r)
    s = t = 0.0
    if a <= 1e-12 and e <= 1e-12:
        return float(np.linalg.norm(r))
    if a <= 1e-12:
        t = min(1.0, max(0.0, f / e))
    else:
        c = float(d1 @ r)
        if e <= 1e-12:
            s = min(1.0, max(0.0, -c / a))
        else:
            b = float(d1 @ d2)
            den = a * e - b * b
            s = min(1.0, max(0.0, (b * f - c * e) / den)) if den > 1e-12 else 0.0
            t = (b * s + f) / e
            if t < 0.0:
                t, s = 0.0, min(1.0, max(0.0, -c / a))
            elif t > 1.0:
                t, s = 1.0, min(1.0, max(0.0, (b - c) / a))
    return float(np.linalg.norm((p1 + d1 * s) - (p2 + d2 * t)))


_EDGES = [(i, i ^ m) for i in range(8) for m in (1, 2, 4) if i < (i ^ m)]


def obb_overlap(a, b):
    """The penetration depth of two oriented boxes: the smallest overlap over their 15 separating axes, or None when some axis separates them."""
    axes = [a.R[:, i] for i in range(3)] + [b.R[:, i] for i in range(3)]
    for i in range(3):
        for j in range(3):
            x = np.cross(a.R[:, i], b.R[:, j])
            n = np.linalg.norm(x)
            if n > 1e-6:
                axes.append(x / n)
    dc = b.c - a.c
    depth = 1e9
    for ax in axes:
        ra = float(np.sum(a.h * np.abs(a.R.T @ ax)))
        rb = float(np.sum(b.h * np.abs(b.R.T @ ax)))
        ov = ra + rb - abs(float(dc @ ax))
        if ov < 0.0:
            return None
        depth = min(depth, ov)
    return depth


def obb_distance(a, b, limit=None):
    """The exact distance between two oriented boxes (0 when they overlap). `limit`: gives up (returns a value above it) when the boxes' bounds are farther apart than that."""
    if limit is not None and _aabb_gap(a, b) > limit:
        return limit + 1.0
    if obb_overlap(a, b) is not None:
        return 0.0
    best = 1e9
    for v in a.verts:
        best = min(best, _point_obb(v, b))
    for v in b.verts:
        best = min(best, _point_obb(v, a))
    for (i, j) in _EDGES:
        for (k, l) in _EDGES:
            best = min(best, _seg_seg(a.verts[i], a.verts[j], b.verts[k], b.verts[l]))
    return best


def attach_report(spec, gap=ATTACH_GAP, embed=EMBED_DEPTH):
    """{'floating': [names not within `gap` of the cap or an attached piece], 'shallow': [static boxes attached but overlapping their parent by less than `embed`], 'pieces': n}.
    Chain segments are checked at rest; a segment hangs from the end of the one above, so only the first one has to find the hair."""
    ps = pieces_of(spec)
    root = [p for p in ps if p.name == "cap"]
    if not root:
        return {"floating": [], "shallow": [], "pieces": len(ps), "note": "no cap"}
    attached = list(root)
    todo = [p for p in ps if p.name != "cap"]
    depth_of = {}
    progress = True
    while todo and progress:
        progress = False
        for p in list(todo):
            best = None
            for q in attached:
                if p.chain is not None and p.index > 0 and not (q.chain == p.chain and q.index == p.index - 1):
                    continue                            # a chain segment hangs from the segment above it
                d = obb_distance(p, q, gap)
                if d <= gap:
                    ov = obb_overlap(p, q)
                    best = max(best or 0.0, ov if ov is not None else 0.0)
            if best is not None:
                attached.append(p)
                todo.remove(p)
                depth_of[p.name] = best
                progress = True
    shallow = [p.name for p in attached if p.chain is None and p.name != "cap" and depth_of.get(p.name, 0.0) < embed]
    cap = root[0]
    beside = [p.name for p in ps if p.chain is None and p.name != "cap" and p.aabb()[1][1] > SIDE_Y and obb_distance(p, cap, gap) > gap]
    return {"floating": [p.name for p in todo], "shallow": shallow, "beside": beside, "coplanar": coplanar_pairs(ps), "pieces": len(ps)}


COPLANAR = 0.02


def coplanar_pairs(ps, tol=COPLANAR, min_area=0.5):
    """Pairs of pieces with the same orientation that have a face on one plane (within `tol`), both facing the same way, and overlap there by at least `min_area` square pixels: two such faces
    z-fight. Back-to-back faces (one piece's high face on the other's low face) are inside the hair, slivers are not seen, pieces of different orientation cannot share a plane except by
    accident, and two segments of one chain move against each other, so none of those are reported."""
    out = []
    for i in range(len(ps)):
        for j in range(i + 1, len(ps)):
            a, b = ps[i], ps[j]
            if not np.allclose(a.R, b.R, atol=1e-6) or (a.chain is not None and a.chain == b.chain):
                continue
            ca, cb = a.R.T @ a.c, b.R.T @ b.c
            lo_a, hi_a, lo_b, hi_b = ca - a.h, ca + a.h, cb - b.h, cb + b.h
            for ax in range(3):
                others = [k for k in range(3) if k != ax]
                ov = 1.0
                for k in others:
                    ov *= max(0.0, min(hi_a[k], hi_b[k]) - max(lo_a[k], lo_b[k]))
                if ov < min_area:
                    continue
                if abs(lo_a[ax] - lo_b[ax]) < tol or abs(hi_a[ax] - hi_b[ax]) < tol:
                    out.append("%s|%s" % (a.name, b.name))
    return sorted(set(out))
