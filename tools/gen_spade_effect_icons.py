"""The HUD icons of the three Spade Raid effects (Lucifero's Flattened Air and Kneeling, Morris's Specimen).

    python3 -B tools/gen_spade_effect_icons.py

Writes assets/nusmp/textures/mob_effect/{flattened_air,kneeling,specimen}.png (18 x 18, the size of the other effect icons). Original pixel art, drawn from
simple shapes (a mask per part, an outline round the union, a vertical gradient inside), so every icon has the same light from the top left.
Why: a player must SEE what a boss did to them, and the icons are the first thing the HUD shows; a missing texture would be the purple-black square.
"""
import math
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "nusmp", "textures", "mob_effect")
SIZE = 18


class Layer:
    """One filled shape: a set of pixels, a top colour and a bottom colour (the fill is a vertical gradient between them)."""

    def __init__(self, top, bottom, light=None):
        self.px = set()
        self.top, self.bottom, self.light = top, bottom, light

    def rect(self, x0, y0, x1, y1):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.px.add((x, y))
        return self

    def poly(self, pts):
        """Fills a convex or concave polygon by testing each pixel centre with the even-odd rule."""
        for y in range(SIZE):
            for x in range(SIZE):
                cx, cy = x + 0.5, y + 0.5
                inside = False
                for i in range(len(pts)):
                    ax, ay = pts[i]
                    bx, by = pts[(i + 1) % len(pts)]
                    if (ay > cy) != (by > cy) and cx < (bx - ax) * (cy - ay) / (by - ay) + ax:
                        inside = not inside
                if inside:
                    self.px.add((x, y))
        return self

    def disc(self, cx, cy, r):
        for y in range(SIZE):
            for x in range(SIZE):
                if math.hypot(x + 0.5 - cx, y + 0.5 - cy) <= r:
                    self.px.add((x, y))
        return self

    def ring(self, cx, cy, r0, r1):
        for y in range(SIZE):
            for x in range(SIZE):
                if r0 <= math.hypot(x + 0.5 - cx, y + 0.5 - cy) <= r1:
                    self.px.add((x, y))
        return self

    def line(self, x0, y0, x1, y1, width=1.0):
        n = int(max(abs(x1 - x0), abs(y1 - y0)) * 3) + 1
        for i in range(n + 1):
            t = i / n
            cx, cy = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
            for y in range(SIZE):
                for x in range(SIZE):
                    if math.hypot(x + 0.5 - cx, y + 0.5 - cy) <= width / 2:
                        self.px.add((x, y))
        return self


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def render(layers, edge):
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()
    union = set()
    for layer in layers:
        union |= layer.px
    for (x, y) in union:                      # outline: every pixel next to the shape (4-neighbourhood) that is not part of it
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, ny = x + dx, y + dy
            if 0 <= nx < SIZE and 0 <= ny < SIZE and (nx, ny) not in union:
                px[nx, ny] = edge + (255,)
    for layer in layers:                      # fills, later layers over earlier ones
        ys = [y for (_, y) in layer.px] or [0]
        y0, y1 = min(ys), max(ys)
        for (x, y) in layer.px:
            t = 0 if y1 == y0 else (y - y0) / (y1 - y0)
            col = lerp(layer.top, layer.bottom, t)
            if layer.light and ((x - 1, y) not in layer.px or (x, y - 1) not in layer.px):
                col = layer.light                # a lit edge on the top and the left
            px[x, y] = col + (255,)
    return img


def flattened_air():
    """A broad arrow driving down onto a thin slab of squeezed air: the air is pressed flat."""
    edge = (26, 14, 58)
    arrow = Layer((206, 190, 255), (122, 92, 200), (236, 228, 255))
    arrow.rect(7, 1, 10, 6).poly([(2.6, 6.2), (15.4, 6.2), (9, 12.4)])
    slab = Layer((150, 118, 230), (70, 46, 140), (200, 180, 255))
    slab.rect(1, 14, 16, 15)
    squeeze = Layer((236, 228, 255), (236, 228, 255))
    squeeze.rect(0, 3, 2, 3).rect(15, 3, 17, 3)           # the air squeezed out to the sides
    return render([squeeze, arrow, slab], edge)


def kneeling():
    """A heavy block coming down on a figure on its knees, seen from the front: head bowed, shoulders pressed, knees on the floor."""
    edge = (22, 12, 44)
    weight = Layer((150, 126, 214), (64, 44, 120), (206, 190, 255))
    weight.poly([(1.5, 1), (16.5, 1), (14.5, 5.4), (3.5, 5.4)])
    figure = Layer((244, 236, 255), (170, 150, 222), None)
    figure.disc(9, 8.2, 1.7)                                            # the bowed head
    figure.poly([(4.2, 14.4), (5.0, 10.6), (7.0, 9.6), (11.0, 9.6), (13.0, 10.6), (13.8, 14.4)])   # shoulders and back, hunched
    figure.rect(2, 12, 4, 13)                                           # a hand pressed to the floor on each side
    figure.rect(14, 12, 16, 13)
    figure.rect(4, 15, 8, 16)                                           # the two knees and shins on the floor
    figure.rect(10, 15, 14, 16)
    return render([figure, weight], edge)


def specimen():
    """A rune ring (Modification Magic's orange) pinned through its middle by a needle: the specimen."""
    edge = (60, 26, 8)
    ring = Layer((255, 190, 120), (214, 108, 42), (255, 226, 176))
    ring.ring(8.6, 9.4, 3.4, 6.2)
    core = Layer((255, 238, 200), (255, 190, 120))
    core.disc(8.6, 9.4, 1.5)
    needle = Layer((244, 244, 252), (150, 156, 176), (255, 255, 255))
    needle.line(3.2, 2.6, 14.4, 15.4, 1.5)
    head = Layer((255, 214, 150), (230, 140, 60), (255, 240, 210))
    head.disc(3.0, 2.6, 1.6)
    return render([ring, core, needle, head], edge)


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, make in (("flattened_air", flattened_air), ("kneeling", kneeling), ("specimen", specimen)):
        path = os.path.join(OUT, name + ".png")
        make().save(path)
        print("wrote", os.path.normpath(path))


if __name__ == "__main__":
    main()
