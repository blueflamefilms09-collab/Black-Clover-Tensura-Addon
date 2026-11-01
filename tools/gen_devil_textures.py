#!/usr/bin/env python3
"""Generates the Devil Contract textures (deterministic, idempotent, PIL only).

    python3 tools/gen_devil_textures.py

Writes to src/main/resources/assets/nusmp/textures/entity/devil/:
  weg_<tier>[_slim].png, weg_<tier>[_slim]_glow.png   tier = low | mid | high | supreme   (64x64 RGBA, standard player skin layout)
  horns.png (32x32), wings.png + wings_glow.png (64x64)
and the preview sheets docs/previews/devil/weg_sheet.png + parts_sheet.png (not part of the mod).

The Weg marks are black tattoo lines painted ONLY on the base-layer boxes of the skin (never on hat/jacket/sleeves/pants), so they can be
drawn translucently over the real skin on a copy of the player model. The glow files hold a thin bright core of ~40% of the mark pixels.
Every part is painted on an "unrolled strip" (right side, front, left side, back side by side, as in the skin file), so a band or a
diagonal wraps around the limb and lines continue from one face onto the next. Self-checks at the end assert the layout rules.
"""
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/assets/nusmp/textures/entity/devil"
PREV = ROOT / "docs/previews/devil"

TIERS = ["low", "mid", "high", "supreme"]
GLOW_COL = {"low": (0x9A, 0x5C, 0xFF), "mid": (0xFF, 0x2A, 0x3A), "high": (0xFF, 0xB0, 0x20), "supreme": (0xFF, 0xF2, 0xB8)}
MARK = (0x08, 0x06, 0x0C, 235)
VIOLET = (0x1A, 0x10, 0x30, 235)
# outer-layer areas of the 64x64 skin (x0, y0, x1, y1 exclusive): hat, jacket, sleeves, pants
OUTER = [(32, 0, 64, 16), (16, 32, 40, 48), (40, 32, 56, 48), (48, 48, 64, 64), (0, 32, 16, 48), (0, 48, 16, 64)]


def hexc(s):
    return tuple(int(s[i:i + 2], 16) for i in (1, 3, 5))


def bres(a, b):
    (x0, y0), (x1, y1) = a, b
    dx, dy = abs(x1 - x0), -abs(y1 - y0)
    sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
    err, out = dx + dy, []
    while True:
        out.append((x0, y0))
        if (x0, y0) == (x1, y1):
            return out
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x0 += sx
        if e2 <= dx:
            err += dx
            y0 += sy


def spiral(cx, cy, r0, r1, turns, a0=0.0, cw=1, n=48):
    """Rounded polyline of a spiral (r0 == r1 gives a ring)."""
    pts = []
    for i in range(n + 1):
        t = i / n
        a = a0 + cw * turns * 2 * math.pi * t
        r = r0 + (r1 - r0) * t
        pts.append((round(cx + r * math.cos(a)), round(cy + r * math.sin(a))))
    return pts


def boxes(slim):
    aw = 3 if slim else 4
    # name: (u, v, w, h, d)
    return {"head": (0, 0, 8, 8, 8), "body": (16, 16, 8, 12, 4), "rarm": (40, 16, aw, 12, 4), "rleg": (0, 16, 4, 12, 4),
            "larm": (32, 48, aw, 12, 4), "lleg": (16, 48, 4, 12, 4)}


def face_rects(box):
    u, v, w, h, d = box
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
            "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


# ---------------------------------------------------------------------------------------------------- one body part
class Part:
    """Paint on the unrolled side strip: columns R(i) right side, F(i) front, L(i) left side, B(i) back; y 0..h-1 from the top."""

    def __init__(self, name, box, mirror=False):
        self.name, (self.u, self.v, self.w, self.h, self.d) = name, box
        self.S = 2 * (self.w + self.d)
        self.mirror = mirror
        self.rng = random.Random(f"devil-{name}")
        self.strokes = []

    def R(self, i): return i
    def F(self, i): return self.d + i
    def L(self, i): return self.d + self.w + i
    def B(self, i): return 2 * self.d + self.w + i
    def mx(self, sx): return 2 * self.d + self.w - 1 - sx   # mirror about the vertical axis of front and back (no wrap!)

    def _side(self, sx, y):
        if not 0 <= y < self.h:
            return None
        if self.mirror:
            sx = self.mx(sx)
        return (self.u + sx % self.S, self.v + self.d + y)

    def _top(self, x, y):
        if not (0 <= x < self.w and 0 <= y < self.d):
            return None
        return (self.u + self.d + (self.w - 1 - x if self.mirror else x), self.v + y)

    def line(self, pts, width=1, sym=False, face="side", scatter=False):
        """Polyline through design points (strip coords, may run past the seam). width 2 adds a second pixel beside the core."""
        if sym:
            self.line(pts, width, False, face, scatter)
            pts = [((self.mx(x) if face == "side" else self.w - 1 - x), y) for x, y in pts]
        conv = self._side if face == "side" else self._top
        raw = []
        for a, b in (zip(pts, pts[1:]) if len(pts) > 1 else [(pts[0], pts[0])]):
            major_x = abs(b[0] - a[0]) >= abs(b[1] - a[1])
            for c in bres(a, b):
                if not raw or raw[-1][0] != c:
                    raw.append((c, (c[0], c[1] + 1) if major_x else (c[0] + 1, c[1])))
        cells = []
        for c, e in raw:
            p = conv(*c)
            if p is not None:
                cells.append((p, conv(*e) if width == 2 else None))
        if cells:
            self.strokes.append({"cells": cells, "scatter": scatter, "violet": len(cells) >= 4 and self.rng.random() < 0.2})

    def dot(self, sx, y, face="side"): self.line([(sx, y)], face=face)

    def stamp(self, art, sx0, y0, sym=False):
        pts = [(sx0 + x, y0 + y) for y, row in enumerate(art) for x, ch in enumerate(row) if ch == "X"]
        for p in pts:
            self.line([p], sym=sym)


DIAMOND = ["..XX..", ".X..X.", "..XX.."]
SIGIL = ["..XX..", ".X..X.", "X.XX.X", ".X..X.", "..XX.."]
RUNE_A = ["X.X", ".X.", "X.X"]


# ---------------------------------------------------------------------------------------------------- designs (cumulative)
# lvl 0 = low, 1 = mid, 2 = high, 3 = supreme; every part also adds all lower levels.
def d_head(p, lvl):
    F, R, L, B = p.F, p.R, p.L, p.B
    if lvl >= 0:
        p.line([(F(2), 7), (F(5), 7)])                                              # front-lower-head edge
    if lvl >= 2:
        p.line([(R(3), 5), (R(7), 5), (F(0), 5), (F(1), 5)], sym=True)               # cheek stripe each side (ear to under the eye)
        p.line([(R(2), 6), (R(4), 7), (F(0), 7), (F(1), 7)], sym=True)               # jaw line, joins the chin tick
        p.stamp(DIAMOND, F(1), 1)                                                   # forehead sigil: small ringed eye
        p.dot(F(3), 2); p.dot(F(4), 2)
    if lvl >= 3:
        p.line([(F(5), 3), (F(6), 4), (F(7), 5), (L(0), 6), (L(1), 7)], width=2)    # bold scar, one side only (asymmetric)
        p.line([(F(1), 0), (F(2), 0)]); p.line([(F(5), 0), (F(6), 0)])              # crown ticks above the sigil
        p.line([(R(0), 2), (R(3), 2), (R(5), 3)], sym=True)                         # temple branches
        p.line(spiral(L(5), 4.5, 0.3, 1.9, 1.4, 0.5))                               # spiral behind the ear (left side only)
        p.line([(B(0), 1), (B(3), 4), (B(7), 1)], width=2)                          # back of the head: bold V, ring, drops
        p.line(spiral(B(3.5), 5, 1.8, 1.8, 1.05))
        p.line([(0, 0), (7, 7)], face="top", width=2); p.line([(7, 0), (0, 7)], face="top")   # top of the head: runic X


def d_body(p, lvl):
    F, R, L, B = p.F, p.R, p.L, p.B
    if lvl in (1, 2):
        for yb in (3, 5):                                                           # chest chevrons
            p.line([(F(3), yb), (F(0), yb - 2)], sym=True)
    if lvl >= 2:
        p.line([(R(2), 2), (R(2), 10)], sym=True)                                   # side lines
        for y in (4, 7, 10):
            p.dot(R(1), y); p.dot(p.mx(R(1)), y)
    if lvl == 2:
        p.line([(F(3), 1), (F(3), 11)])                                             # spine ladder, front
        for y in (7, 9, 11):
            p.dot(F(4), y)
        p.line([(B(3), 0), (B(3), 11)])                                             # spine ladder, back
        for y in (1, 3, 5, 7, 9, 11):
            p.dot(B(4), y)
        for y in (3, 6, 9):
            p.line([(B(2), y), (B(1), y), (B(0), y + 1)], sym=True)                 # ribs
    if lvl >= 3:
        p.line([(F(1), 0), (F(0), 1)]); p.line([(F(6), 0), (F(7), 1)])              # collar droops into the clavicles
        p.line(spiral(F(3.5), 4.5, 2.5, 2.5, 1.05))                                 # ring over the chest
        p.line([(F(3), 3), (F(4), 5)]); p.line([(F(4), 3), (F(3), 5)])              # crossed core inside the ring
        p.line([(F(3), 7), (F(3), 11)])                                             # trunk below the ring, with branches
        for y in (8, 10):
            p.line([(F(3), y), (F(2), y - 1), (F(1), y - 1), (F(0), y - 2)], sym=True)
        p.line([(B(3), 0), (B(3), 11)], width=2)                                    # back: bold trunk with wing branches
        p.line([(B(2), 3), (B(1), 2), (B(0), 0)], sym=True)
        p.line([(B(2), 7), (B(1), 6), (B(0), 4), (B(0), 3)], sym=True)
        p.line(spiral(B(1), 9.5, 0.3, 1.3, 1.2, 1.0))                               # asymmetric: spiral lower left only
        p.line([(R(0), 1), (R(1), 3), (R(2), 5), (R(1), 8), (R(0), 10)])            # side vines, one per side, different
        p.line([(L(3), 1), (L(2), 4), (L(1), 7), (L(2), 10)])


def d_arm(p, lvl):
    F, R, L, B, w = p.F, p.R, p.L, p.B, p.w
    left = p.name == "larm"
    if lvl >= 0:
        p.line([(F(1), 5), (F(1), 11)])                                             # forearm line
        p.line([(R(0), 5), (R(1), 7), (R(2), 9), (R(3), 11)])                        # outer diagonal, wraps toward the front
        p.dot(F(0), 8); p.dot(F(w - 1), 8)                                          # runic ticks
    if lvl >= 1:
        p.line([(0, 2), (p.S - 1, 2)])                                              # upper-arm band
        for sx in (R(1), F(1), L(1), B(1)):
            p.dot(sx, 3)
        p.line([(0, 0), (w - 1, 3)], face="top")                                    # shoulder mark
        p.line([(R(0), 0), (R(1), 1), (R(2), 0)])
        p.line([(F(i), 9 + min(i, w - 1 - i)) for i in range(w)])                    # back of the hand
    if lvl >= 2:
        p.line([(F(1), 7), (F(2), 6), (F(w - 1), 5)])                                   # branch off the forearm line
        p.line([(B(0), 6), (B(1), 7), (B(w - 1), 6)])                               # elbow chevron
        p.line([(B(1), 8), (B(1), 11)])
    if lvl >= 3:
        # one vine wraps once round the arm (the left arm winds the other way), a bold forearm stripe, a spiral on one face
        S = p.S
        p.line([(0, 3), (S, 11)] if not left else [(S, 3), (0, 11)])
        p.line([(B(1), 5), (B(1), 11)] if not left else [(R(2), 4), (R(2), 11)], width=2)   # bold stripe: back (right arm) / outer side (left arm)
        p.line([(F(1), 3), (F(1), 4)])
        p.line([(F(1), 5), (F(w - 1), 4)] if not left else [(F(1), 4), (F(0), 3)])
        p.line(spiral(F(w / 2 - 0.5), 7.5, 0.3, 1.5, 1.2, 2.0 if left else 0.0))


def d_leg(p, lvl):
    F, R, L, B, w = p.F, p.R, p.L, p.B, p.w
    left = p.name == "lleg"
    if lvl >= 1:
        for y in (7, 9, 11):
            p.dot(F(1), y)                                                          # faint calf line (dashed)
        p.dot(R(2), 8); p.dot(R(2), 10)
    if lvl >= 2:
        p.line([(F(2), 0), (F(2), 5), (F(1), 6)])                                   # thigh line, forks at the knee
        p.line([(F(2), 5), (F(3), 6)])
        p.line([(R(1), 1), (R(2), 3), (R(1), 5)])                                   # outer thigh zig
        p.line([(B(1), 2), (B(1), 5)])
    if lvl >= 3:
        S = p.S
        p.line([(0, 0), (S, 11)] if not left else [(S, 0), (0, 11)])                # one vine wraps once round the leg
        p.line([(0, 6), (S - 1, 6)])                                                # knee ring
        p.line([(F(2), 0), (F(2), 5)] if not left else [(B(1), 0), (B(1), 5)], width=2)   # bold thigh stripe (front / back)
        p.line([(B(1), 7), (B(2), 9)])
        p.line([(L(0), 1), (L(2), 4), (L(1), 8)])
        p.line(spiral(R(1.5), 8.5, 0.3, 1.5, 1.2, 1.0 if left else 0.0))             # spiral on the outer calf


DESIGN = {"head": d_head, "body": d_body, "rarm": d_arm, "larm": d_arm, "rleg": d_leg, "lleg": d_leg}


def build_marks(lvl, slim):
    """-> (marks {(x,y): rgba}, glow {(x,y)}, per-part Part objects)."""
    marks, glow, parts = {}, set(), {}
    for name, box in boxes(slim).items():
        p = Part(name, box, mirror=name in ("larm", "lleg"))
        DESIGN[name](p, lvl)
        parts[name] = p
        grng = random.Random(f"glow-{name}-{lvl}")
        for s in p.strokes:
            cells = s["cells"]
            for i, (c, e) in enumerate(cells):
                marks.setdefault(c, MARK)
                if e is not None:
                    marks.setdefault(e, MARK)
            n = len(cells)
            if n <= 2 or s["scatter"]:
                chosen = [i for i in range(n) if grng.random() < 0.4]
            else:
                run = max(1, round(n * 0.37))
                start = grng.randint(0, n - run)
                chosen = range(start, start + run)
            for i in chosen:
                glow.add(cells[i][0])
                if lvl == 3 and cells[i][1] is not None and grng.random() < 0.8:
                    glow.add(cells[i][1])                                            # supreme: 2px core in places
        for s in p.strokes:                                                           # a few line tips in very dark violet
            if s["violet"] and marks[s["cells"][-1][0]] == MARK:
                marks[s["cells"][-1][0]] = VIOLET
    return marks, glow, parts


def marks_image(marks):
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for xy, c in marks.items():
        im.putpixel(xy, c)
    return im


def glow_image(glow, tier):
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for xy in glow:
        im.putpixel(xy, GLOW_COL[tier] + (255,))
    return im


# ---------------------------------------------------------------------------------------------------- horns / wings (tileable)
def to_img(grid, w, h):
    im = Image.new("RGBA", (w, h))
    for y in range(h):
        for x in range(w):
            im.putpixel((x, y), tuple(grid[y][x]) + (255,))
    return im


def make_horns():
    N = 32
    rng = random.Random("devil-horns")
    base, body, ridge, light, crack = hexc("#17121c"), hexc("#2a2230"), hexc("#3d3345"), hexc("#5a4c63"), hexc("#5a1020")
    g = [[body if rng.random() > 0.14 else base for _ in range(N)] for _ in range(N)]
    cols, x = [], rng.randint(0, 2)
    while x < N:
        cols.append(x)
        x += rng.randint(3, 5)
    for cx in cols:                                                     # vertical ridge segments with a shadow on one side
        y, x = rng.randint(0, 6), cx
        while y < N + 4:
            for k in range(rng.randint(3, 9)):
                if rng.random() < 0.12:
                    x += rng.choice((-1, 1))
                g[(y + k) % N][x % N] = ridge
                if rng.random() < 0.75:
                    g[(y + k) % N][(x + 1) % N] = base
                if k == 0 and rng.random() < 0.4:
                    g[(y + k) % N][x % N] = light
            y += k + 1 + rng.randint(1, 3)
    for _ in range(5):                                                  # faint crimson cracks, short and jagged
        x, y = rng.randrange(N), rng.randrange(N)
        for _ in range(rng.randint(3, 5)):
            if g[y % N][x % N] not in (ridge, light):
                g[y % N][x % N] = crack
            y += 1
            x += rng.choice((-1, 0, 0, 1))
    for _ in range(6):                                                  # a few extra highlight specks on the body
        x, y = rng.randrange(N), rng.randrange(N)
        if g[y][x] == body:
            g[y][x] = light
    return to_img(g, N, N)


def veins(rng, W, H, n_trunks):
    """Branching vein lines on a W x H torus -> list of strokes (list of (x, y))."""
    strokes = []

    def walk(x, y, ang, length, depth):
        pts, drift = [], 0.0
        for i in range(length):
            pts.append((round(x) % W, round(y) % H))
            drift += rng.uniform(-0.12, 0.12)
            ang += drift * 0.5
            x += math.cos(ang)
            y += math.sin(ang)
            if depth < 2 and i > 3 and rng.random() < 0.13:
                walk(x, y, ang + rng.choice((-1, 1)) * rng.uniform(0.5, 0.95), rng.randint(5, 9), depth + 1)
        strokes.append(list(dict.fromkeys(pts)))

    cw, ch = W / 4, H / (n_trunks // 4)                                 # jittered grid so no 12x12 window is empty
    for gy in range(n_trunks // 4):
        for gx in range(4):
            walk((gx + rng.random()) * cw, (gy + rng.random()) * ch, math.pi / 2 + rng.uniform(-0.9, 0.9), rng.randint(14, 22), 0)
    return strokes


def make_wings():
    W, H = 32, 64
    rng = random.Random("devil-wings")
    col = {k: hexc(v) for k, v in dict(bb="#3a3138", br="#54474f", bj="#2b2329", bs="#241d22", mb="#2a0a14", mm="#3a0f1c", mv="#160509",
                                       ms="#55202e").items()}
    img, glow = [[(0, 0, 0)] * 64 for _ in range(64)], [[(0, 0, 0)] * 64 for _ in range(64)]

    # --- bone: vertical streaks, lighter ridges, shadows, darker joints
    bone = [[col["bb"]] * W for _ in range(H)]
    for x in range(W):
        for y in range(H):
            r = rng.random()
            bone[y][x] = hexc("#403740") if r < 0.07 else hexc("#332b32") if r < 0.14 else col["bb"]
    cx = rng.randint(0, 2)
    while cx < W:
        for rx, ry_c, ry_p in ((cx, "br", 0.0), (cx + 1, "bs", 0.55)):
            y = rng.randint(0, 8)
            while y < H + 10:
                seg = rng.randint(6, 18)
                for k in range(seg):
                    if rx == cx or rng.random() < ry_p:
                        bone[(y + k) % H][rx % W] = col[ry_c]
                y += seg + rng.randint(1, 4)
        cx += rng.choice((2, 3, 3, 4))
    jy = rng.randint(2, 6)
    while jy < H:
        for x in range(W):
            if rng.random() < 0.9:
                bone[jy % H][x] = col["bs"] if rng.random() < 0.6 else col["bj"]
            if rng.random() < 0.55:
                bone[(jy + 1) % H][x] = col["bj"]
            if rng.random() < 0.35:
                bone[(jy - 1) % H][x] = col["br"]
        jy += rng.randint(9, 13)
    for y in range(H):
        for x in range(W):
            img[y][x] = bone[y][x]

    # --- membrane: base, smooth mottling, specks, dark vein network (the glow lines follow a subset of it)
    gx, gy, cell = 8, 16, 4
    knots = [[rng.random() for _ in range(gx)] for _ in range(gy)]
    for y in range(H):
        for x in range(W):
            fx, fy = x / cell, y / cell
            x0, y0 = int(fx), int(fy)
            tx, ty = fx - x0, fy - y0
            tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
            v = ((knots[y0 % gy][x0 % gx] * (1 - tx) + knots[y0 % gy][(x0 + 1) % gx] * tx) * (1 - ty) +
                 (knots[(y0 + 1) % gy][x0 % gx] * (1 - tx) + knots[(y0 + 1) % gy][(x0 + 1) % gx] * tx) * ty)
            img[y][32 + x] = col["mm"] if v > 0.55 else col["mb"]
    vs = veins(random.Random("devil-veins-17"), W, H, 32)   # seed picked so every 12x12 window gets 17..63 glow px
    vein_px = set()
    for s in vs:
        vein_px.update(s)
    for (x, y) in vein_px:
        img[y][32 + x] = col["mv"]
    for _ in range(40):
        x, y = rng.randrange(W), rng.randrange(H)
        if (x, y) not in vein_px:
            img[y][32 + x] = col["ms"]
    # --- glow: ember veins on the membrane, ember dots on the bone
    core, hot = hexc("#ff3040"), hexc("#ff7a5a")
    glow_px = set()
    for i, s in enumerate(vs):
        if i % 5 != 4 and len(s) >= 5:
            for (x, y) in s[1:-1]:
                glow[y][32 + x] = core
                glow_px.add((x, y))
            glow[s[len(s) // 2][1]][32 + s[len(s) // 2][0]] = hot
    for _ in range(14):
        glow[rng.randrange(H)][rng.randrange(W)] = core if rng.random() < 0.8 else hot
    return to_img(img, 64, 64), to_img(glow, 64, 64), glow_px


# ---------------------------------------------------------------------------------------------------- preview sheets
SKIN, SHIRT, PANTS = hexc("#c89a7a"), hexc("#5a5f6a"), hexc("#2c3040")
SCALE = 8


def flat_skin(slim):
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    for name, box in boxes(slim).items():
        colr = SKIN if name == "head" else SHIRT if name in ("body", "rarm", "larm") else PANTS
        for fx, fy, fw, fh in face_rects(box).values():
            d.rectangle([fx, fy, fx + fw - 1, fy + fh - 1], fill=colr + (255,))
    if True:                                                           # eyes + mouth so the cheek / brow marks can be judged
        for x, y in ((1, 12), (2, 12), (5, 12), (6, 12)):
            im.putpixel((x + 8, y), (0x2A, 0x2A, 0x3A, 255))
        for x in (3, 4):
            im.putpixel((x + 8, 14), (0x8A, 0x5A, 0x4A, 255))
    return im


def doll(tex, slim, back):
    """Front or back view assembled from the faces of a 64x64 skin-layout texture -> RGBA image."""
    b = boxes(slim)
    aw = 3 if slim else 4
    out = Image.new("RGBA", (16, 32), (0, 0, 0, 0))
    key = "back" if back else "front"

    def put(name, x, y):
        fx, fy, fw, fh = face_rects(b[name])[key]
        out.alpha_composite(tex.crop((fx, fy, fx + fw, fy + fh)), (x, y))

    put("head", 4, 0)
    put("body", 4, 8)
    put("larm" if back else "rarm", 4 - aw, 8)           # seen from behind the player's left side is on the viewer's left
    put("rarm" if back else "larm", 12, 8)
    put("lleg" if back else "rleg", 4, 20)
    put("rleg" if back else "lleg", 8, 20)
    return out


def with_glow(base, glow, k=0.7):
    g = glow.copy()
    g.putalpha(g.getchannel("A").point(lambda a: int(a * k)))
    out = base.copy()
    out.alpha_composite(g)
    return out


def side_dolls(tex, slim):
    """Right side and left side view (8 px each, 2 px gap) from the faces of a skin-layout texture."""
    b = boxes(slim)
    out = Image.new("RGBA", (18, 32), (0, 0, 0, 0))
    for key, ox, arm, leg in (("right", 0, "rarm", "rleg"), ("left", 10, "larm", "lleg")):
        def put(name, x, y):
            fx, fy, fw, fh = face_rects(b[name])[key]
            out.alpha_composite(tex.crop((fx, fy, fx + fw, fy + fh)), (ox + x, y))
        put("head", 0, 0)
        put("body", 2, 8)
        put(arm, 2, 8)
        put(leg, 2, 20)
    return out


def weg_sheet(sets):
    font = ImageFont.load_default()
    widths = [16, 16, 18, 16, 16]                       # skin front, skin back, skin sides, plain front, plain back
    pad, lab, ch = 16, 26, 32 * SCALE
    sheet = Image.new("RGBA", (pad + sum(w * SCALE + pad for w in widths), pad + len(TIERS) * (ch + lab + pad)), (70, 74, 82, 255))
    d = ImageDraw.Draw(sheet)
    for r, t in enumerate(TIERS):
        marks, glow = sets[t]
        skin_c = with_glow(Image.alpha_composite(flat_skin(False), marks), glow)
        plain_c = with_glow(Image.alpha_composite(Image.new("RGBA", (64, 64), (0xD6, 0xD6, 0xD6, 255)), marks), glow)
        y0 = pad + r * (ch + lab + pad)
        d.text((pad, y0), f"{t}   (left 3: over flat skin, glow at 70%: front, back, right+left side; right 2: marks + glow alone on light grey)",
               fill=(255, 255, 255, 255), font=font)
        x = pad
        for i, (img, wpx) in enumerate(((doll(skin_c, False, False), 16), (doll(skin_c, False, True), 16), (side_dolls(skin_c, False), 18),
                                        (doll(plain_c, False, False), 16), (doll(plain_c, False, True), 16))):
            bgc = Image.new("RGBA", (wpx * SCALE, ch), (52, 55, 62, 255) if i < 3 else (0xD6, 0xD6, 0xD6, 255))
            bgc.alpha_composite(img.resize((wpx * SCALE, ch), Image.NEAREST))
            sheet.paste(bgc, (x, y0 + lab))
            x += wpx * SCALE + pad
    PREV.mkdir(parents=True, exist_ok=True)
    sheet.convert("RGB").save(PREV / "weg_sheet.png")


def parts_sheet(horns, wings, wglow):
    font = ImageFont.load_default()
    s, pad, lab = 6, 20, 22
    sheet = Image.new("RGB", (pad * 4 + 32 * s + 2 * 64 * s, pad * 2 + lab + 64 * s), (128, 128, 128))
    d = ImageDraw.Draw(sheet)
    x = pad
    for name, im in (("horns.png 32x32", horns), ("wings.png 64x64 (left bone, right membrane)", wings), ("wings_glow.png 64x64", wglow)):
        d.text((x, pad), name, fill=(255, 255, 255), font=font)
        big = im.convert("RGB").resize((im.width * s, im.height * s), Image.NEAREST)
        sheet.paste(big, (x, pad + lab))
        x += big.width + pad
    sheet.save(PREV / "parts_sheet.png")


# ---------------------------------------------------------------------------------------------------- self-check + main
def in_rect(xy, r): return r[0] <= xy[0] < r[2] and r[1] <= xy[1] < r[3]


def check_marks(tier_i, slim, marks, glow, parts, im_m, im_g):
    assert im_m.size == (64, 64) and im_m.mode == "RGBA" and im_g.size == (64, 64) and im_g.mode == "RGBA"
    faces = [r for b in boxes(slim).values() for fx, fy, fw, fh in face_rects(b).values() for r in [(fx, fy, fx + fw, fy + fh)]]
    for xy in marks:
        assert not any(in_rect(xy, o) for o in OUTER), f"mark in an outer-layer area: {xy}"
        assert any(in_rect(xy, f) for f in faces), f"mark outside every base face: {xy}"
    assert set(glow) <= set(marks), "glow pixel without a mark under it"
    assert {xy for xy in glow} == {(x, y) for x in range(64) for y in range(64) if im_g.getpixel((x, y))[3] == 255}
    assert all(im_g.getpixel(xy)[:3] == GLOW_COL[TIERS[tier_i]] for xy in glow)
    assert 0.3 <= len(glow) / len(marks) <= 0.55, f"glow ratio {len(glow) / len(marks):.2f}"
    for name, b in boxes(slim).items():
        fr = list(face_rects(b).values())
        area = sum(fw * fh for _, _, fw, fh in fr)
        used = sum(1 for xy in marks if any(in_rect(xy, (fx, fy, fx + fw, fy + fh)) for fx, fy, fw, fh in fr))
        assert used / area < 0.5, f"{TIERS[tier_i]} {name} is {used / area:.0%} painted"
        if tier_i == 0:
            assert used <= 25, f"low {name} has {used} px"
    if tier_i == 0:
        for n in ("rleg", "lleg"):
            assert not any(in_rect(xy, (b[0], b[1], b[0] + 2 * (b[2] + b[4]), b[1] + b[4] + b[3])) for b in [boxes(slim)[n]] for xy in marks), "low: legs must be empty"


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    PREV.mkdir(parents=True, exist_ok=True)
    sets, prev_marks = {}, {}
    for slim in (False, True):
        prev = None
        for i, t in enumerate(TIERS):
            marks, glow, parts = build_marks(i, slim)
            im_m, im_g = marks_image(marks), glow_image(glow, t)
            check_marks(i, slim, marks, glow, parts, im_m, im_g)
            if prev is not None and i < 3:
                assert set(prev) <= set(marks), "low < mid < high must be cumulative"
            prev = marks
            sfx = "_slim" if slim else ""
            im_m.save(OUT / f"weg_{t}{sfx}.png")
            im_g.save(OUT / f"weg_{t}{sfx}_glow.png")
            if not slim:
                sets[t] = (im_m, im_g)
            print(f"weg_{t}{sfx}: {len(marks)} mark px, {len(glow)} glow px ({len(glow) / len(marks):.0%})")
    horns = make_horns()
    wings, wglow, gpx = make_wings()
    assert horns.size == (32, 32) and wings.size == (64, 64) and wglow.size == (64, 64)
    for im in (horns, wings, wglow):
        assert im.getchannel("A").getextrema() == (255, 255), "must be fully opaque"
    for y in range(0, 64 - 12 + 1, 4):                                  # every 12x12 window of the membrane (wrapping) carries ember veins
        for x in range(0, 32, 4):
            n = sum(1 for dx in range(12) for dy in range(12) if ((x + dx) % 32, (y + dy) % 64) in gpx)
            assert 14 <= n <= 70, f"membrane glow window ({x},{y}) has {n} px"
    horns.save(OUT / "horns.png")
    wings.save(OUT / "wings.png")
    wglow.save(OUT / "wings_glow.png")
    weg_sheet(sets)
    parts_sheet(horns, wings, wglow)
    print("ok ->", OUT)


if __name__ == "__main__":
    main()
