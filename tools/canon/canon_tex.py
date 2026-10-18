"""Paints the 192 x 240 atlas of a Canon Grimoire (layout: canon_book.REG) from five inputs: the cover colour, the trim colour, the metal colour, the old 7x7 sigil of the book and the
emblem mask. Pure Pillow, deterministic (a hash, no random), a small palette: every ramp has a handful of steps, so the PNG is saved as a palette image of at most a few dozen colours.
"""
from PIL import Image

import canon_book as B

REG = B.REG


# ---------------------------------------------------------------------------------------------------------------------------------------------- colour
def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def shade(c, f):
    """f < 0 darkens towards black, f > 0 lightens towards white."""
    return mix(c, (0, 0, 0), -f) if f < 0 else mix(c, (255, 255, 255), f)


def hsh(x, y, s=0):
    n = (x * 73856093) ^ (y * 19349663) ^ (s * 83492791)
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


STEPS = 0.07           # the ramp step: a shade is snapped to a multiple of it, so a cover uses a dozen leather colours and not a thousand


def snap(g):
    return round(g / STEPS) * STEPS


class Atlas:
    def __init__(self):
        self.im = Image.new("RGBA", (B.W, B.H), (0, 0, 0, 0))
        self.px = self.im.load()

    def put(self, x, y, c):
        if 0 <= x < B.W and 0 <= y < B.H:
            self.px[x, y] = tuple(c) + (255,)

    def rect(self, x0, y0, x1, y1, c):
        for y in range(y0, y1):
            for x in range(x0, x1):
                self.put(x, y, c)


# ---------------------------------------------------------------------------------------------------------------------------------------------- pieces
def leather_px(cover, g):
    return shade(cover, snap(g))


def paint_leather(a, rect, cover, seed, base=0.0, vig=0.0, grain=0.07):
    x0, y0, x1, y1 = rect
    w, h = x1 - x0, y1 - y0
    for y in range(h):
        for x in range(w):
            d = min(x, y, w - 1 - x, h - 1 - y)
            g = base + (hsh(x, y, seed) - 0.5) * 2 * grain + (hsh(x // 3, y, seed + 1) - 0.5) * 0.05
            if vig:
                g -= vig * max(0.0, 1.0 - d / 10.0)
            a.put(x0 + x, y0 + y, leather_px(cover, g))


def bevel_plate(a, x0, y0, x1, y1, metal, depth=1):
    """A metal plate with a light top-left edge and a dark bottom-right edge."""
    hi, mid, lo, dk = shade(metal, 0.42), shade(metal, 0.12), shade(metal, -0.18), shade(metal, -0.5)
    for y in range(y0, y1):
        for x in range(x0, x1):
            c = mid if (x + y) % 5 else shade(metal, 0.07)
            if x < x0 + depth or y < y0 + depth:
                c = hi
            if x >= x1 - depth or y >= y1 - depth:
                c = lo
            if (x in (x0, x1 - 1)) and (y in (y0, y1 - 1)):
                c = dk
            a.put(x, y, c)


def diamond(a, cx, cy, r, c):
    for dy in range(-r, r + 1):
        for dx in range(-r, r + 1):
            if abs(dx) + abs(dy) <= r:
                a.put(cx + dx, cy + dy, c)


def glyph(a, ox, oy, rows, scale, c):
    for dy, row in enumerate(rows):
        for dx, ch in enumerate(row):
            if ch == "#":
                a.rect(ox + dx * scale, oy + dy * scale, ox + (dx + 1) * scale, oy + (dy + 1) * scale, c)


def frame_lines(a, rect, cover, inset=9):
    """An embossed double line (light, then dark) inset from the edge; the corners are cut."""
    x0, y0, x1, y1 = rect
    light, dark = shade(cover, 0.22), shade(cover, -0.42)
    for k, c in ((inset, light), (inset + 1, dark)):
        for x in range(x0 + k + 3, x1 - k - 3):
            a.put(x, y0 + k, c)
            a.put(x, y1 - 1 - k, c)
        for y in range(y0 + k + 3, y1 - k - 3):
            a.put(x0 + k, y, c)
            a.put(x1 - 1 - k, y, c)


def paint_front(a, cover, trim, metal, rows):
    x0, y0, x1, y1 = REG["front"]
    paint_leather(a, REG["front"], cover, 1, base=0.02, vig=0.30)
    w, h = x1 - x0, y1 - y0
    for y in range(h):                                  # the outer 2 px of the board show 0.25 units round the bevel: dark
        for x in range(w):
            if min(x, y, w - 1 - x, h - 1 - y) < 2:
                a.put(x0 + x, y0 + y, shade(cover, -0.5))
    frame_lines(a, REG["front"], cover)
    # the title plate (top): a metal bar with three diamonds
    bevel_plate(a, x0 + 22, y0 + 13, x0 + 58, y0 + 25, metal)
    for k, dx in enumerate((-9, 0, 9)):
        diamond(a, x0 + 40 + dx, y0 + 19, 3 if k == 1 else 2, shade(cover, -0.62))
        diamond(a, x0 + 40 + dx, y0 + 19, 1, trim)
    # the shadow the medallion throws (it is a raised plate: the leather under it is darker round it)
    cx, cy = B.fx(8.2) - x0, B.fy(8.4) - y0
    for y in range(h):
        for x in range(w):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if 21 < d < 27:
                px = a.px[x0 + x, y0 + y]
                a.px[x0 + x, y0 + y] = shade(px[:3], -snap(0.2 * (1 - (d - 21) / 6.0))) + (255,)
    # the nameplate (bottom) with the old sigil of the book
    bevel_plate(a, x0 + 18, y0 + 75, x0 + 62, y0 + 93, metal)
    a.rect(x0 + 31, y0 + 77, x0 + 49, y0 + 91, shade(cover, -0.7))
    glyph(a, x0 + 33, y0 + 78, rows, 2, trim)
    for dx in (-17, 17):
        diamond(a, x0 + 40 + dx, y0 + 84, 2, shade(cover, -0.62))
        diamond(a, x0 + 40 + dx, y0 + 84, 1, trim)


def paint_back(a, cover, trim, metal, rows):
    x0, y0, x1, y1 = REG["back"]
    paint_leather(a, REG["back"], cover, 2, base=0.0, vig=0.30)
    w, h = x1 - x0, y1 - y0
    for y in range(h):
        for x in range(w):
            if min(x, y, w - 1 - x, h - 1 - y) < 2:
                a.put(x0 + x, y0 + y, shade(cover, -0.5))
    frame_lines(a, REG["back"], cover)
    cx, cy = x0 + w // 2, y0 + h // 2
    for y in range(h):                                  # a round seal: metal ring, dark disc, the sigil large
        for x in range(w):
            d = ((x - w // 2) ** 2 + (y - h // 2) ** 2) ** 0.5
            if d < 26:
                if d < 18.5:
                    a.put(x0 + x, y0 + y, shade(cover, -0.66 - (0.07 if d > 15 else 0)))
                elif d < 23.5:
                    t = (x + y) / float(w + h)
                    a.put(x0 + x, y0 + y, shade(metal, 0.4) if (x - w // 2 + y - h // 2) < -3 and d > 20.5 else shade(metal, -0.2) if (x - w // 2 + y - h // 2) > 3 and d > 20.5 else shade(metal, 0.12))
                else:
                    a.put(x0 + x, y0 + y, shade(cover, -0.4))
    glyph(a, cx - 17, cy - 17, rows, 5, trim)
    for dx, dy in ((-30, -40), (30, -40), (-30, 40), (30, 40)):
        diamond(a, cx + dx, cy + dy, 3, shade(metal, 0.1))
        diamond(a, cx + dx, cy + dy, 1, shade(cover, -0.6))


def paint_spine(a, cover, trim, metal, rows):
    x0, y0, x1, y1 = REG["spine"]
    w, h = x1 - x0, y1 - y0
    for y in range(h):
        for x in range(w):
            g = -0.22 + 0.2 * (1.0 - abs(x - (w - 1) / 2.0) / (w / 2.0)) + (hsh(x, y, 5) - 0.5) * 0.12
            a.put(x0 + x, y0 + y, leather_px(cover, g))
    bands = [(int(round(B.fy(b1) - y0)), int(round(B.fy(b0) - y0))) for b0, b1 in ((3.7, 4.3), (6.4, 7.0), (9.0, 9.6), (11.7, 12.3))]    # pixel rows (top, bottom) of the bands
    # tooling between the bands: a gold double line next to each band and a diamond in the middle of each free segment
    edges = [0] + [v for b in bands for v in b] + [h]
    segs = [(edges[i], edges[i + 1]) for i in range(0, len(edges), 2)]
    for k, (ya, yb) in enumerate(segs):
        for u in range(4, w - 4):
            if yb - ya > 8:
                a.put(x0 + u, y0 + ya + 2, shade(metal, -0.1))
                a.put(x0 + u, y0 + yb - 3, shade(metal, -0.1))
        if k == 2:                                      # the label between band 2 and band 3: a dark plate with the sigil
            ym = (ya + yb) // 2
            a.rect(x0 + 6, y0 + ym - 8, x0 + w - 6, y0 + ym + 8, shade(metal, -0.35))
            a.rect(x0 + 7, y0 + ym - 7, x0 + w - 7, y0 + ym + 7, shade(cover, -0.7))
            glyph(a, x0 + 8, y0 + ym - 7, rows, 2, trim)
        elif yb - ya > 10:
            ym = (ya + yb) // 2
            diamond(a, x0 + w // 2, y0 + ym, 3, shade(metal, 0.05))
            diamond(a, x0 + w // 2, y0 + ym, 1, shade(cover, -0.5))


def paint_edges(a, cover):
    for name in ("edgeV", "edgeH"):
        r = REG[name]
        for y in range(r[1], r[3]):
            for x in range(r[0], r[2]):
                g = -0.4 + (hsh(x, y, 9) - 0.5) * 0.14
                a.put(x, y, leather_px(cover, g))
    r = REG["edgeV"]
    for y in range(r[1] + 3, r[3] - 3, 6):             # stitches along the fore-edge strip
        a.put(r[0] + 3, y, shade(cover, -0.12))
        a.put(r[0] + 3, y + 1, shade(cover, -0.12))


def paint_small(a, cover, trim, metal, glow):
    # leather and strap
    paint_leather(a, REG["leather"], cover, 11, base=-0.06, grain=0.05)
    r = REG["strap"]
    for y in range(r[1], r[3]):
        for x in range(r[0], r[2]):
            a.put(x, y, leather_px(cover, -0.5 + (hsh(x, y, 12) - 0.5) * 0.1))
    for x in range(r[0] + 1, r[2] - 1):
        if (x - r[0]) % 4 < 2:
            a.put(x, r[1] + 2, shade(cover, 0.2))
            a.put(x, r[3] - 3, shade(cover, 0.2))
    # metal plates
    for name, base in (("metal", metal), ("metalDark", shade(metal, -0.38))):
        x0, y0, x1, y1 = REG[name]
        bevel_plate(a, x0, y0, x1, y1, base)
        a.put(x0 + 7, y0 + 7, shade(base, 0.55))
        a.put(x0 + 8, y0 + 8, shade(base, -0.45))
        a.put(x0 + 7, y0 + 8, shade(base, 0.1))
        a.put(x0 + 8, y0 + 7, shade(base, 0.1))
    # ribbon: the trim colour, a light stripe in the middle
    x0, y0, x1, y1 = REG["ribbon"]
    for y in range(y0, y1):
        for x in range(x0, x1):
            c = trim
            if x in (x0, x1 - 1):
                c = shade(trim, -0.3)
            elif x in (x0 + 3, x0 + 4):
                c = shade(trim, 0.28)
            a.put(x, y, shade(c, -0.08 * (y - y0) / 16.0))
    # the clasp's stone: four facets
    x0, y0, x1, y1 = REG["gem"]
    for y in range(y0, y1):
        for x in range(x0, x1):
            u, v = x - x0, y - y0
            d = max(abs(u - 3.5), abs(v - 3.5))
            c = shade(glow, 0.5) if d < 1.5 else glow if d < 2.5 else shade(glow, -0.25)
            if u < 3 and v < 3:
                c = shade(c, 0.15)
            a.put(x, y, c)
    # pages: fore-edge (lines run down) and head/tail (lines run across)
    cream, c2, line = (238, 226, 196), (226, 212, 178), (194, 178, 140)
    for name, vertical in (("pageV", True), ("pageH", False)):
        x0, y0, x1, y1 = REG[name]
        for y in range(y0, y1):
            for x in range(x0, x1):
                i = (x - x0) if vertical else (y - y0)
                j = (y - y0) if vertical else (x - x0)
                c = (cream, c2, line)[i % 3]
                if i % 3 != 2 and hsh(i, j // 5, 21) > 0.7:
                    c = c2 if c is cream else cream
                a.put(x, y, c)


def dilate(mask, n):
    w, h = mask.size
    src = mask.load()
    pts = {(x, y) for y in range(h) for x in range(w) if src[x, y]}
    out = set(pts)
    for _ in range(n):
        out |= {(x + dx, y + dy) for (x, y) in out for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))}
    return out


def paint_emblem(a, cover, trim, metal, glow, mask):
    """The medal (bezel, disc, a faint halo of the emblem) and the full-bright emblem on its own quad."""
    x0, y0, x1, y1 = REG["medal"]
    n = x1 - x0
    c = (n - 1) / 2.0
    dark = shade(cover, -0.7)
    halo = dilate(mask, 1)
    for y in range(n):
        for x in range(n):
            d = ((x - c) ** 2 + (y - c) ** 2) ** 0.5
            light = ((x - c) + (y - c)) / (2 * n)
            col = shade(metal, 0.4) if light < -0.12 else shade(metal, -0.22) if light > 0.12 else shade(metal, 0.1)
            if d <= 19.5 and d > 17.5:
                col = shade(metal, -0.5)
            if d <= 17.5:
                col = shade(dark, -0.08 * (d / 17.5))
                if 15.6 < d <= 16.4:
                    col = mix(dark, glow, 0.35)
                if (x - 2, y - 2) in halo:
                    col = mix(dark, glow, 0.2)
            a.put(x0 + x, y0 + y, col)
    gx0, gy0, gx1, gy1 = REG["glow"]
    m = mask.load()
    w, h = mask.size

    def on(x, y):
        return 0 <= x < w and 0 <= y < h and m[x, y] > 0
    for y in range(h):
        for x in range(w):
            if not on(x, y):
                continue
            col = glow
            if not on(x - 1, y) or not on(x, y - 1):
                col = shade(glow, 0.45)
            elif not on(x + 1, y) or not on(x, y + 1):
                col = shade(glow, -0.28)
            elif all(on(x + dx, y + dy) for dx in (-2, 0, 2) for dy in (-2, 0, 2)):
                col = shade(glow, 0.18)
            a.put(gx0 + x, gy0 + y, col)


def paint_all(cover, trim, metal, glow, rows, mask):
    a = Atlas()
    paint_front(a, cover, trim, metal, rows)
    paint_back(a, cover, trim, metal, rows)
    paint_spine(a, cover, trim, metal, rows)
    paint_edges(a, cover)
    paint_small(a, cover, trim, metal, glow)
    paint_emblem(a, cover, trim, metal, glow, mask)
    return a.im


def to_palette(im):
    """RGBA -> an indexed PNG image: colour 0 is the fully transparent one, every other pixel is opaque (the game cuts out, it never blends, these textures)."""
    px = im.load()
    pal = {}
    out = Image.new("P", im.size, 0)
    op = out.load()
    for y in range(im.size[1]):
        for x in range(im.size[0]):
            r, g, b, al = px[x, y]
            if al < 128:
                continue
            k = (r, g, b)
            if k not in pal:
                pal[k] = len(pal) + 1
            op[x, y] = pal[k]
    assert len(pal) <= 255, "too many colours: %d" % len(pal)
    flat = [0, 0, 0]
    for k in pal:
        flat += list(k)
    out.putpalette(flat)
    out.info["transparency"] = 0
    return out, len(pal)
