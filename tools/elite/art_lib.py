"""Shared helpers of the elite/items art scripts (tools/elite/gen_*.py): paths, 16x16 icon canvases, a diagonal sword painter, a contact sheet.

Everything is deterministic (no random without a seed) so a script can be re-run and the files do not change.
"""
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
DATA = os.path.join(ROOT, "src", "main", "resources", "data", "nusmp")
ITEM_TEX = os.path.join(ASSETS, "textures", "item")
BUILD = os.path.join(ROOT, "build", "elite_preview")


def ensure(path):
    os.makedirs(path, exist_ok=True)
    return path


def write_text(path, text):
    ensure(os.path.dirname(path))
    with open(path, "w", newline="\n", encoding="utf-8") as f:
        f.write(text)


def rgba(c, a=255):
    return (int(c[0]), int(c[1]), int(c[2]), a)


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3])


def mix(a, b, t):
    return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3))


class Canvas:
    """A 16x16 (or any size) RGBA canvas with put() that ignores out-of-range writes."""

    def __init__(self, w=16, h=16):
        self.w, self.h = w, h
        self.im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        self.px = self.im.load()

    def put(self, x, y, c, a=255):
        if 0 <= x < self.w and 0 <= y < self.h and c is not None:
            self.px[x, y] = rgba(c, a)

    def get(self, x, y):
        return self.px[x, y] if 0 <= x < self.w and 0 <= y < self.h else (0, 0, 0, 0)

    def filled(self, x, y):
        return self.get(x, y)[3] > 0

    def outline(self, c, diagonal=False):
        """Paints c on every empty pixel next to a filled one (4-neighbourhood, or 8 when diagonal)."""
        todo = []
        for y in range(self.h):
            for x in range(self.w):
                if self.filled(x, y):
                    continue
                nb = [(1, 0), (-1, 0), (0, 1), (0, -1)] + ([(1, 1), (-1, 1), (1, -1), (-1, -1)] if diagonal else [])
                if any(self.filled(x + dx, y + dy) for dx, dy in nb):
                    todo.append((x, y))
        for x, y in todo:
            self.put(x, y, c)

    def rows(self, art, pal):
        """Paints an ASCII picture: one string per row, '.' or ' ' transparent, every other character looked up in pal."""
        for y, row in enumerate(art):
            for x, ch in enumerate(row):
                if ch in ". ":
                    continue
                self.put(x, y, pal[ch])
        return self

    def save(self, path):
        ensure(os.path.dirname(path))
        self.im.save(path)


def diag_sword(blade, hw, tip_d, guard, grip, pommel, extra=(), outline=(12, 12, 20), center=15, d0=-7):
    """A 16x16 diagonal pixel sword in the pose of the mod's older icons (tip top-right, hilt bottom-left).

    Cells are addressed by s = x + y (across the blade) and d = x - y (along it); the blade runs from d0 to tip_d around the anti-diagonal s = center.
    blade   (light, mid, dark, edge): the palette; light is the upper-left face, dark the lower-right one, edge the sharpened outermost light line
    hw      half width in bands for each d step (a function d -> int >= 0, or a list from d0)
    guard   dict(d=int, half=int, color=(c1, c2, c3), thick=bool)  the cross piece
    grip    (color_a, color_b)  two or three cells of grip below the guard
    pommel  colour of the end cap
    extra   (x, y, colour) pixels painted last (runes, gems, sparks)
    """
    cv = Canvas()
    light, mid, dark, edge = blade
    steps = list(range(d0, tip_d + 1, 2))

    def half(d):
        if callable(hw):
            return hw(d)
        i = (d - d0) // 2
        return hw[min(i, len(hw) - 1)]

    for d in range(d0, tip_d + 1):
        h = half(d)
        for s in range(center - h, center + h + 1):
            if (s + d) % 2:
                continue
            x, y = (s + d) // 2, (s - d) // 2
            if s < center:
                c = edge if s == center - h and h >= 1 else light
            elif s == center:
                c = mid
            else:
                c = dark
            cv.put(x, y, c)
    gd, gh = guard["d"], guard["half"]
    c1, c2, c3 = guard["color"]
    for t in range(-gh, gh + 1):
        s = center + t * 2
        x, y = (s + gd) // 2, (s - gd) // 2
        cv.put(x, y, c2 if t == 0 else c1 if t < 0 else c3)
        if guard.get("thick"):
            cv.put(x + 1, y, c3 if t >= 0 else c2)
    ga, gb = grip
    for k, dd in enumerate((gd - 2, gd - 4, gd - 6)):
        x, y = (center + dd) // 2, (center - dd) // 2
        cv.put(x, y, ga if k % 2 == 0 else gb)
        if k < 2:
            cv.put(x + 1, y, gb)
    xq, yq = (center + gd - 8) // 2, (center - gd + 8) // 2
    cv.put(xq, yq, pommel)
    cv.put(xq - 1, yq + 1 if yq < 15 else yq, shade(pommel, 0.7))
    cv.outline(outline)
    for (x, y, c) in extra:
        cv.put(x, y, c)
    return cv


def sheet(images, labels=None, scale=8, cols=8, bg=(40, 44, 56)):
    """A contact sheet of equally sized RGBA images (PIL), nearest neighbour upscaled, on a dark background."""
    if not images:
        return None
    w, h = images[0].size
    rows = (len(images) + cols - 1) // cols
    out = Image.new("RGBA", (cols * (w * scale + 8) + 8, rows * (h * scale + 8) + 8), bg + (255,))
    for i, im in enumerate(images):
        big = im.resize((w * scale, h * scale), Image.NEAREST)
        out.alpha_composite(big, (8 + (i % cols) * (w * scale + 8), 8 + (i // cols) * (h * scale + 8)))
    return out
