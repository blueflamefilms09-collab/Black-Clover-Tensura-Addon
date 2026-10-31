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

    def box(self, name, frm, size, inflate=0.2, tone=0, part="head", rot=None, pivot=None, helm="hide", taper="none", pattern="strand", key=None, tip=0.0, uvsize=None):
        w, h, d = size
        uw, uh, ud = uvsize or (max(1, int(round(w))), max(1, int(round(h))), max(1, int(round(d))))
        k = key if key is not None else "%s:%s:%s:%s:%s" % (uw, uh, ud, taper, pattern)
        (u, v), fresh = self.sheet.alloc(uw, uh, ud, key=k)
        if fresh:
            paint_block(self.sheet, u, v, uw, uh, ud, self.seed + len(self.boxes) * 7, taper=taper, pattern=pattern, tip=tip, anime=self.anime)
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
                paint_block(self.sheet, u, v, uw, uh, ud, self.seed + 31 * (i + 1) + len(self.chains) * 13, taper=taper, pattern=pattern, tip=0.6 if last else 0.0, anime=self.anime)
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
