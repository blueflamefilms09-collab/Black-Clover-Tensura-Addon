"""Sixteen-by-sixteen sprites for the generated relic waves (1.70.0): one drawing function per motif, painted from a relic's palette (main, trim, glow).

Why drawn in code: the generated relics (gen_relic_waves.py) are hundreds, each needs its own flat sprite for the inventory (the held and worn forms come from the shared 3D libraries), and a
motif plus the relic's own three colours gives every relic a distinct, readable icon without hundreds of hand-painted files. Every sprite gets a dark outline, a light edge and (for glowing
things) a glow pixel or two, in the style of the other relic icons. Deterministic: the same inputs give the same bytes. Hand-painted icons can replace any file later: the generator never
overwrites a sprite that is not marked as its own (see gen_relic_waves.py)."""
import colorsys
import math
import struct
import zlib


def _rgb(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16))


def _shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


def _mix(a, b, t):
    return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3))


class Canvas:
    def __init__(self, main, trim, glow):
        self.px = {}
        self.main, self.trim, self.glow = _rgb(main), _rgb(trim), _rgb(glow)
        self.dark = _shade(self.main, 0.45)
        self.light = _mix(self.main, (255, 255, 255), 0.45)
        self.tdark = _shade(self.trim, 0.6)

    def p(self, x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            self.px[(x, y)] = c

    def rect(self, x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.p(x, y, c)

    def disc(self, cx, cy, r, c):
        for y in range(16):
            for x in range(16):
                if (x - cx) ** 2 + (y - cy) ** 2 <= r * r + 0.3:
                    self.p(x, y, c)

    def ring(self, cx, cy, r0, r1, c):
        for y in range(16):
            for x in range(16):
                d = (x - cx) ** 2 + (y - cy) ** 2
                if r0 * r0 - 0.2 <= d <= r1 * r1 + 0.3:
                    self.p(x, y, c)

    def line(self, x0, y0, x1, y1, c):
        n = max(abs(x1 - x0), abs(y1 - y0), 1)
        for i in range(n + 1):
            self.p(round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n), c)

    def poly(self, pts, c):
        ys = [p[1] for p in pts]
        for y in range(min(ys), max(ys) + 1):
            xs = []
            for i in range(len(pts)):
                (xa, ya), (xb, yb) = pts[i], pts[(i + 1) % len(pts)]
                if (ya <= y < yb) or (yb <= y < ya):
                    xs.append(xa + (y - ya) * (xb - xa) / (yb - ya))
            xs.sort()
            for i in range(0, len(xs) - 1, 2):
                for x in range(math.ceil(xs[i] - 0.5), math.floor(xs[i + 1] + 0.5) + 1):
                    self.p(x, y, c)

    def finish(self):
        """Outline, a light top-left edge, then the png bytes."""
        out = dict(self.px)
        for (x, y) in list(self.px):
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                q = (x + dx, y + dy)
                if q not in self.px and 0 <= q[0] < 16 and 0 <= q[1] < 16:
                    out[q] = (20, 16, 24)
        return out


def png(pixels):
    raw = b""
    for y in range(16):
        raw += b"\x00"
        for x in range(16):
            r = pixels.get((x, y))
            raw += bytes(r + (255,)) if r else b"\x00\x00\x00\x00"

    def chunk(t, d):
        c = struct.pack(">I", len(d)) + t + d
        return c + struct.pack(">I", zlib.crc32(t + d) & 0xFFFFFFFF)

    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")


# ------------------------------------------------------------------ the motifs: each draws on a Canvas
def ring_(c):
    c.ring(8, 9, 3, 5, c.trim); c.ring(8, 9, 4, 4, c.tdark); c.rect(6, 3, 10, 5, c.main); c.rect(7, 2, 9, 2, c.glow); c.p(8, 4, c.light)


def pendant_(c):
    c.line(3, 1, 8, 6, c.tdark); c.line(13, 1, 8, 6, c.tdark); c.line(4, 1, 8, 5, c.trim); c.line(12, 1, 8, 5, c.trim)
    c.poly([(8, 6), (12, 9), (8, 14), (4, 9)], c.main); c.poly([(8, 7), (10, 9), (8, 12), (6, 9)], c.glow); c.p(7, 8, c.light)


def amulet_(c):
    c.line(3, 1, 7, 4, c.tdark); c.line(13, 1, 9, 4, c.tdark)
    c.disc(8, 9, 5, c.trim); c.disc(8, 9, 4, c.main); c.disc(8, 9, 2, c.glow); c.p(6, 7, c.light)


def orb_(c):
    c.disc(8, 8, 6, c.main); c.disc(8, 8, 4, _mix(c.main, c.glow, 0.5)); c.disc(7, 7, 2, c.glow); c.p(5, 5, (255, 255, 255)); c.rect(5, 13, 10, 14, c.trim)


def key_(c):
    c.ring(5, 5, 1, 3, c.trim); c.p(5, 5, c.glow); c.line(7, 7, 13, 13, c.trim); c.line(8, 7, 14, 13, c.tdark); c.rect(11, 13, 13, 14, c.main); c.rect(9, 11, 10, 12, c.main)


def coin_(c):
    c.disc(8, 8, 6, c.trim); c.disc(8, 8, 5, c.main); c.ring(8, 8, 3, 3, c.trim); c.p(8, 8, c.glow); c.p(6, 5, c.light)


def scroll_(c):
    c.rect(3, 4, 12, 12, c.light); c.rect(3, 4, 12, 5, c.main); c.rect(3, 11, 12, 12, c.main); c.rect(1, 3, 3, 13, c.trim); c.rect(12, 3, 14, 13, c.trim)
    for y in (7, 9):
        c.line(5, y, 11, y, c.dark)
    c.p(8, 8, c.glow)


def book_(c):
    c.rect(3, 2, 12, 14, c.main); c.rect(3, 2, 4, 14, c.dark); c.rect(11, 3, 12, 13, c.light); c.rect(6, 5, 9, 8, c.trim); c.p(7, 6, c.glow); c.rect(5, 11, 10, 11, c.tdark)


def vial_(c):
    c.rect(6, 2, 9, 3, c.trim); c.rect(7, 4, 8, 5, c.light); c.poly([(7, 5), (8, 5), (12, 11), (11, 14), (4, 14), (3, 11)], c.light)
    c.poly([(4, 10), (11, 10), (12, 11), (11, 13), (4, 13), (3, 11)], c.glow); c.p(5, 8, (255, 255, 255))


def crown_(c):
    c.rect(2, 9, 13, 13, c.trim); c.poly([(2, 9), (2, 3), (5, 7), (8, 2), (11, 7), (14, 3), (14, 9)], c.trim); c.rect(3, 10, 12, 12, c.main)
    for x in (5, 8, 11):
        c.p(x, 11, c.glow)
    c.p(8, 4, c.glow)


def circlet_(c):
    c.rect(3, 8, 12, 10, c.trim); c.rect(4, 7, 11, 7, c.tdark); c.poly([(8, 3), (10, 6), (8, 9), (6, 6)], c.main); c.p(8, 6, c.glow); c.p(7, 5, c.light)


def helm_(c):
    c.poly([(3, 14), (3, 7), (5, 3), (11, 3), (13, 7), (13, 14)], c.main); c.rect(5, 8, 11, 9, c.dark); c.rect(7, 8, 8, 12, c.dark)
    c.rect(7, 2, 8, 4, c.trim); c.p(6, 5, c.light); c.rect(3, 12, 5, 14, c.trim); c.rect(11, 12, 13, 14, c.trim)


def hood_(c):
    c.poly([(8, 1), (13, 6), (13, 14), (3, 14), (3, 6)], c.main); c.disc(8, 9, 3, c.dark); c.p(7, 9, c.glow); c.p(9, 9, c.glow); c.line(4, 6, 8, 2, c.light)


def cloak_(c):
    c.rect(4, 2, 11, 3, c.trim); c.poly([(4, 3), (11, 3), (14, 14), (2, 14)], c.main); c.line(8, 4, 8, 14, c.dark); c.line(4, 5, 3, 13, c.light); c.p(8, 3, c.glow)


def cape_(c):
    c.rect(3, 2, 12, 4, c.trim); c.poly([(3, 4), (12, 4), (13, 12), (8, 14), (2, 12)], c.main); c.rect(7, 2, 8, 3, c.glow); c.line(4, 5, 3, 11, c.light); c.line(11, 5, 12, 11, c.dark)


def belt_(c):
    c.rect(1, 6, 14, 10, c.main); c.rect(1, 6, 14, 6, c.light); c.rect(1, 10, 14, 10, c.dark); c.rect(6, 5, 9, 11, c.trim); c.rect(7, 7, 8, 9, c.glow)


def glove_(c):
    c.rect(5, 8, 11, 14, c.main); c.rect(4, 3, 5, 8, c.main); c.rect(6, 2, 7, 8, c.main); c.rect(8, 3, 9, 8, c.main); c.rect(10, 4, 11, 8, c.main)
    c.rect(2, 8, 4, 11, c.main); c.rect(5, 13, 11, 14, c.trim); c.p(8, 10, c.glow); c.line(6, 3, 6, 7, c.light)


def boots_(c):
    c.rect(4, 2, 8, 11, c.main); c.rect(4, 10, 13, 14, c.main); c.rect(4, 2, 8, 3, c.trim); c.rect(4, 14, 13, 14, c.dark); c.p(5, 6, c.light); c.p(12, 11, c.glow)


def plate_(c):
    c.poly([(2, 3), (5, 2), (11, 2), (14, 3), (13, 8), (11, 14), (5, 14), (3, 8)], c.main); c.rect(7, 2, 8, 13, c.dark); c.rect(5, 6, 10, 7, c.trim); c.p(8, 9, c.glow); c.line(4, 4, 4, 8, c.light)


def robe_(c):
    c.poly([(5, 2), (11, 2), (14, 8), (13, 14), (3, 14), (2, 8)], c.main); c.rect(7, 2, 8, 14, c.trim); c.rect(5, 2, 10, 3, c.trim); c.p(8, 8, c.glow); c.line(4, 5, 3, 12, c.light)


def feather_(c):
    c.poly([(13, 2), (14, 3), (10, 11), (6, 13), (4, 14), (6, 10), (8, 6)], c.main); c.line(13, 2, 4, 14, c.trim); c.line(11, 5, 7, 6, c.light); c.line(10, 8, 6, 9, c.light); c.p(12, 3, c.glow)


def eye_(c):
    c.poly([(1, 8), (5, 4), (11, 4), (15, 8), (11, 12), (5, 12)], c.light); c.disc(8, 8, 3, c.main); c.disc(8, 8, 2, c.glow); c.p(8, 8, (10, 8, 12)); c.line(2, 8, 5, 5, c.dark); c.line(14, 8, 11, 5, c.dark)


def claw_(c):
    c.poly([(3, 3), (6, 3), (10, 9), (8, 14), (6, 9)], c.light); c.poly([(7, 3), (10, 3), (13, 9), (12, 13), (10, 9)], c.main); c.rect(3, 2, 10, 2, c.trim); c.p(8, 7, c.glow)


def finger_(c):
    c.poly([(6, 14), (5, 8), (6, 2), (9, 2), (10, 8), (10, 14)], c.light); c.rect(6, 4, 9, 4, c.dark); c.rect(6, 8, 9, 8, c.dark); c.rect(6, 12, 9, 12, c.dark); c.rect(6, 1, 9, 2, c.main); c.p(8, 3, c.glow); c.rect(5, 14, 10, 14, c.tdark)


def cup_(c):
    c.poly([(3, 3), (13, 3), (12, 8), (9, 10), (7, 10), (4, 8)], c.trim); c.rect(7, 10, 8, 12, c.trim); c.rect(4, 13, 11, 14, c.tdark); c.rect(4, 4, 11, 5, c.glow); c.p(5, 5, c.light)


def tablet_(c):
    c.poly([(3, 14), (3, 4), (5, 2), (11, 2), (13, 4), (13, 14)], c.main); c.rect(5, 5, 10, 5, c.dark); c.rect(5, 8, 10, 8, c.dark); c.rect(5, 11, 8, 11, c.dark); c.p(10, 11, c.glow); c.line(4, 4, 4, 13, c.light)


def mask_(c):
    c.poly([(3, 3), (13, 3), (13, 9), (10, 14), (6, 14), (3, 9)], c.light); c.rect(4, 6, 6, 7, c.dark); c.rect(10, 6, 12, 7, c.dark); c.p(5, 6, c.glow); c.p(11, 6, c.glow); c.rect(7, 11, 9, 12, c.dark); c.rect(3, 3, 13, 4, c.trim)


def whistle_(c):
    c.line(2, 3, 5, 6, c.tdark); c.rect(5, 6, 12, 10, c.main); c.rect(5, 6, 12, 7, c.light); c.rect(10, 5, 13, 6, c.trim); c.disc(7, 8, 1, c.dark); c.p(7, 8, c.glow); c.rect(12, 7, 14, 9, c.trim)


def lantern_(c):
    c.rect(7, 1, 8, 2, c.trim); c.rect(5, 3, 10, 4, c.trim); c.rect(5, 5, 10, 11, c.glow); c.rect(5, 5, 5, 11, c.trim); c.rect(10, 5, 10, 11, c.trim); c.rect(5, 12, 10, 13, c.trim); c.rect(7, 7, 8, 9, (255, 255, 255))


def compass_(c):
    c.disc(8, 8, 6, c.trim); c.disc(8, 8, 5, c.light); c.poly([(8, 3), (10, 8), (8, 13), (6, 8)], c.dark); c.poly([(8, 3), (9, 8), (7, 8)], c.glow); c.p(8, 8, c.trim)


def hourglass_(c):
    c.rect(3, 2, 12, 3, c.trim); c.rect(3, 13, 12, 14, c.trim); c.poly([(4, 4), (11, 4), (8, 8)], c.glow); c.poly([(8, 8), (5, 12), (10, 12)], c.glow); c.line(4, 4, 7, 8, c.light); c.rect(3, 4, 3, 12, c.tdark); c.rect(12, 4, 12, 12, c.tdark)


def mirror_(c):
    c.disc(8, 7, 5, c.trim); c.disc(8, 7, 4, c.light); c.line(5, 5, 8, 3, (255, 255, 255)); c.rect(7, 12, 8, 14, c.trim); c.p(9, 8, c.glow)


def flute_(c):
    c.line(2, 13, 13, 2, c.trim); c.line(3, 13, 14, 2, c.main); c.line(2, 12, 13, 1, c.light)
    for i in (5, 7, 9):
        c.p(i, 15 - i + 1, c.dark)
    c.p(12, 3, c.glow)


def gem_(c):
    c.poly([(3, 6), (6, 3), (10, 3), (13, 6), (8, 14)], c.main); c.poly([(5, 6), (8, 3), (11, 6), (8, 12)], c.glow); c.line(3, 6, 13, 6, c.light); c.p(6, 4, (255, 255, 255))


def horn_(c):
    c.poly([(3, 12), (4, 8), (7, 5), (12, 2), (13, 3), (11, 8), (8, 12), (5, 14)], c.light); c.line(4, 11, 12, 3, c.main); c.rect(3, 12, 5, 14, c.trim); c.p(11, 4, c.glow)


def skull_(c):
    c.disc(8, 7, 5, c.light); c.rect(5, 11, 10, 13, c.light); c.rect(4, 6, 6, 8, c.dark); c.rect(9, 6, 11, 8, c.dark); c.p(5, 7, c.glow); c.p(10, 7, c.glow); c.rect(7, 12, 7, 13, c.dark); c.rect(9, 12, 9, 13, c.dark); c.p(8, 9, c.dark)


def clover_(c):
    for cx, cy in ((6, 6), (10, 6), (6, 10), (10, 10)):
        c.disc(cx, cy, 2, c.main)
    c.disc(8, 8, 1, c.glow); c.line(8, 10, 10, 15, c.trim); c.p(5, 5, c.light)


def star_(c):
    c.poly([(8, 1), (10, 6), (15, 6), (11, 9), (13, 14), (8, 11), (3, 14), (5, 9), (1, 6), (6, 6)], c.main); c.disc(8, 8, 2, c.glow); c.p(6, 4, c.light)


def flame_(c):
    c.poly([(8, 1), (11, 6), (12, 10), (10, 14), (6, 14), (4, 10), (5, 6)], c.main); c.poly([(8, 5), (10, 9), (9, 13), (7, 13), (6, 9)], c.glow); c.p(8, 10, (255, 255, 255))


def drop_(c):
    c.poly([(8, 1), (12, 8), (12, 11), (10, 14), (6, 14), (4, 11), (4, 8)], c.main); c.poly([(8, 5), (10, 9), (9, 12), (7, 12), (6, 9)], c.glow); c.p(6, 8, c.light)


def tooth_(c):
    c.poly([(3, 3), (13, 3), (12, 9), (10, 14), (8, 10), (6, 14), (4, 9)], c.light); c.rect(3, 3, 13, 4, c.trim); c.line(5, 5, 5, 8, (255, 255, 255)); c.p(8, 6, c.glow)


MOTIFS = {n[:-1]: f for n, f in globals().items() if n.endswith("_") and callable(f) and n != "_"}


import hashlib
import random


def _seed(rid, bump=0):
    return int(hashlib.sha256(("%s:%d" % (rid, bump)).encode()).hexdigest()[:12], 16)


def _vary(c, rng):
    """The per-relic variation: the owner's rule is that no two relics look alike, so on top of the motif and the three palette colours every relic gets its own overlay, drawn from the
    seed of its id: one of eight overlays (a stripe, studs, a band, a sparkle, rune marks, a notch, a gem row, a halo), a possible mirror, and a small shift of every colour."""
    keys = list(c.px)
    body = [k for k in keys if c.px[k] in (c.main, c.light, c.dark)]
    pick = rng.randrange(8)
    if body:
        if pick == 0:                               # a diagonal stripe of trim across the body
            x0, y0 = rng.choice(body)
            for i in range(-3, 4):
                if (x0 + i, y0 + i) in c.px and c.px[(x0 + i, y0 + i)] in (c.main, c.light, c.dark):
                    c.p(x0 + i, y0 + i, c.trim)
        elif pick == 1:                             # three to five studs
            for _ in range(rng.randrange(3, 6)):
                c.p(*rng.choice(body), c.glow if rng.random() < 0.5 else c.trim)
        elif pick == 2:                             # a horizontal band
            y = rng.choice(body)[1]
            for k in body:
                if k[1] == y:
                    c.p(k[0], y, c.tdark)
        elif pick == 3:                             # a sparkle off the shape
            x, y = rng.randrange(1, 15), rng.randrange(1, 15)
            if (x, y) not in c.px:
                for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
                    if (x + dx, y + dy) not in c.px:
                        c.p(x + dx, y + dy, (255, 255, 255) if (dx, dy) == (0, 0) else c.glow)
        elif pick == 4:                             # rune marks: two short vertical ticks
            for _ in range(2):
                x, y = rng.choice(body)
                c.p(x, y, c.dark)
                c.p(x, y + 1, c.dark)
        elif pick == 5:                             # a notch cut from the edge
            x, y = rng.choice(keys)
            if c.px.get((x, y)) in (c.main, c.light, c.dark) and (x + 1, y) not in c.px:
                del c.px[(x, y)]
        elif pick == 6:                             # a row of tiny gems along the lower body
            lowest = max(k[1] for k in body)
            xs = sorted(k[0] for k in body if k[1] == lowest)
            for x in xs[::2][:4]:
                c.p(x, lowest, c.glow)
        else:                                       # a halo dot ring around the top
            top = min(k[1] for k in keys)
            xs = sorted(k[0] for k in keys if k[1] == top)
            if xs:
                cx = xs[len(xs) // 2]
                for dx in (-2, 0, 2):
                    if (cx + dx, top - 1) not in c.px and top - 1 >= 0:
                        c.p(cx + dx, top - 1, c.glow)
    if rng.random() < 0.5:                          # a mirror
        c.px = {(15 - x, y): v for (x, y), v in c.px.items()}


def render(motif, main, trim, glow, rid=None, bump=0):
    c = Canvas(main, trim, glow)
    MOTIFS.get(motif, orb_)(c)
    if rid is not None:
        rng = random.Random(_seed(rid, bump))
        _vary(c, rng)
        # a small, id-seeded shift of the three colours (at most 6 per cent): two relics with the same palette still differ by a few shades
        f = 1.0 + (rng.random() - 0.5) * 0.12
        c.px = {k: tuple(max(0, min(255, int(v * f))) if v not in (255,) else v for v in col) for k, col in c.px.items()}
    return png(c.finish())
