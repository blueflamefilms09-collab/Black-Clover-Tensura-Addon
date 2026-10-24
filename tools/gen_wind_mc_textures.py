#!/usr/bin/env python3
"""
Wind, "Minecraft style" pixel textures.  PHASE A (preview): all three looks to docs/previews.  PHASE B (in the game; the owner picked C "pixel-anime"): the C subset that the
four shipped effects use (Slash, Emperor, Gale / Gust Lane, Ring) goes to src/main/resources/assets/nusmp/textures/particle/wmc_*.png, in two sets:
  wmc_<name>_<frame>.png     the pack palette (white / mint / lime / deep green + a dark green outline), drawn with a white vertex colour
  wmc_<name>_<frame>_t.png   the same shapes in five greys, drawn with the effect's tint (the six non-wind melee pages that reuse Crescent Slash, and any other book colour)

Three looks, each with its own tiny palette, hard pixel edges (no anti-aliasing, no gradients), nearest filtering:

  A  vanilla-faithful  16x16 / 32x32 flat sprites, grey-white "poof / sweep attack / gust" palette + two leaf greens   (6 colours)
  B  voxel             16x16 block-like cube faces (white / mint / lime / green, 1 px bevel, speckles)                (5 colours)
  C  pixel-anime       32x32 / 32x16 sprites with a dark outline, 2-frame flip-book (_0 / _1) from the Black Clover pack  (5 colours)

Output (preview folder only): docs/previews/vfx_remake/mc/wind_tex/<style>_<name>[_<frame>].png  +  wind_mc_textures.png (contact sheet).
Run:  python tools/gen_wind_mc_textures.py            (Pillow + numpy only, deterministic)
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
OUT = os.path.join(ROOT, "docs", "previews", "vfx_remake", "mc", "wind_tex")
GAME = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "particle")

# ---------------------------------------------------------------------------------------------------------------- palettes
PAL_A = {"W": (247, 247, 247), "L": (213, 221, 211), "M": (163, 175, 161), "D": (107, 120, 106), "G": (142, 212, 90), "H": (79, 154, 53)}
PAL_B = {"W": (248, 251, 240), "N": (204, 238, 190), "G": (137, 248, 66), "M": (84, 210, 52), "D": (70, 169, 44)}
PAL_C = {"O": (55, 100, 30), "D": (70, 169, 44), "G": (137, 248, 66), "N": (204, 238, 190), "W": (248, 251, 240)}      # pack: shadow green, deep, lime, mint rim, core white
PAL_T = {"O": (82, 82, 82), "D": (140, 140, 140), "G": (199, 199, 199), "N": (235, 235, 235), "W": (255, 255, 255)}      # the same five steps as greys, multiplied by the effect's tint


class Cv:
    def __init__(self, w, h, pal):
        self.w, self.h, self.pal = w, h, pal
        self.p = np.zeros((h, w, 4), np.uint8)

    def fill(self, fn):
        for y in range(self.h):
            for x in range(self.w):
                c = fn(x + 0.5, y + 0.5)
                if c is not None:
                    self.p[y, x] = self.pal[c] + (255,)
        return self

    def put(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.p[y, x] = self.pal[c] + (255,)

    def outline(self, c):
        m = self.p[..., 3] > 0
        nb = np.zeros_like(m)
        nb[1:, :] |= m[:-1, :]
        nb[:-1, :] |= m[1:, :]
        nb[:, 1:] |= m[:, :-1]
        nb[:, :-1] |= m[:, 1:]
        o = nb & ~m
        self.p[o] = self.pal[c] + (255,)
        return self

    def save(self, name):
        os.makedirs(OUT, exist_ok=True)
        Image.fromarray(self.p, "RGBA").save(os.path.join(OUT, name + ".png"))
        return self.p


def colours_used(arr):
    a = arr.reshape(-1, 4)
    return {tuple(px[:3]) for px in a if px[3] > 0}


# ---------------------------------------------------------------------------------------------------------------- shapes
def crescent_fn(c1x, r1, c2x, r2, cy, rim, body, inner):
    """outer disc minus an offset inner disc: crescent whose two tips point to +x"""
    def f(x, y):
        d1 = math.hypot(x - c1x, y - cy)
        d2 = math.hypot(x - c2x, y - cy)
        if d1 > r1 or d2 < r2:
            return None
        if r1 - d1 < 1.3:
            return rim
        if d2 - r2 < 1.3:
            return inner
        return body
    return f


def sribbon_fn(w, h, amp, half, phase, core, edge):
    """S-curve ribbon with pointed tails, along +x"""
    def f(x, y):
        u = x / w
        cy = h / 2 + amp * math.sin(2 * math.pi * u + phase)
        hw = half * max(0.0, math.sin(math.pi * u)) ** 0.7
        d = abs(y - cy)
        if d > hw:
            return None
        return core if d < hw * 0.45 else edge
    return f


def swirl_fn(size, arms, r0, r1, thick, rot, core, edge):
    """top-down pinwheel: `arms` separate spiral blades, each covers ~65 percent of its sector and tapers to a point at both ends"""
    c = size / 2

    def f(x, y):
        dx, dy = x - c, y - c
        r = math.hypot(dx, dy)
        if r < r0 - 1 or r > r1 + 1:
            return None
        th = math.atan2(dy, dx) - rot
        sector = 2 * math.pi / arms
        for k in range(arms):
            ang = ((th - k * sector) % (2 * math.pi)) / sector          # 0..1 inside the arm's own sector (arms are 1 sector apart)
            if ang > 0.68:
                continue
            t = ang / 0.68
            rk = r0 + (r1 - r0) * (t ** 0.8)
            tk = thick * math.sin(math.pi * t) ** 0.6 + 0.35
            d = abs(r - rk)
            if d < tk:
                return core if d < tk * 0.5 else edge
        return None
    return f


def ring_fn(size, r0, r1, gaps, rot, core, edge):
    c = size / 2

    def f(x, y):
        r = math.hypot(x - c, y - c)
        if r < r0 or r > r1:
            return None
        if gaps:
            th = (math.atan2(y - c, x - c) - rot) % (2 * math.pi)
            if (th * gaps / (2 * math.pi)) % 1.0 > 0.78:
                return None
        return core if (r1 - r0) <= 2 or r1 - r > 1.0 and r - r0 > 1.0 else edge
    return f


def spark_fn(size, arm, diag, core, edge):
    c = size / 2

    def f(x, y):
        dx, dy = abs(x - c), abs(y - c)
        if dx < 1 and dy < 1:
            return core
        if (dx < 1 and dy <= arm) or (dy < 1 and dx <= arm):
            return core if max(dx, dy) < arm * 0.5 else edge
        if diag and abs(dx - dy) < 0.6 and dx <= diag and dx >= 1.4:
            return edge
        return None
    return f


def disc_union(discs, size, shade_cols):
    def f(x, y):
        inside = any(math.hypot(x - cx, y - cy) <= r for cx, cy, r in discs)
        if not inside:
            return None
        t = ((x - size * 0.3) + (y - size * 0.3)) / (size * 0.9)
        i = 0 if t < 0.28 else 1 if t < 0.55 else 2 if t < 0.8 else 3
        return shade_cols[min(i, len(shade_cols) - 1)]
    return f


def leaf_fn(size, rot, core, edge, rib):
    c = size / 2
    ca, sa = math.cos(rot), math.sin(rot)

    def f(x, y):
        dx, dy = x - c, y - c
        u = dx * ca + dy * sa
        v = -dx * sa + dy * ca
        a, b = size * 0.46, size * 0.2
        if (u / a) ** 2 + (v / (b * (1 - 0.35 * (u / a)))) ** 2 > 1:
            return None
        if abs(v) < 0.6:
            return rib
        return core if v < 0 else edge
    return f


def beam_fn(w, h, shift, core, mid, edge, chevron):
    """horizontal beam strip, tiles along x; chevron ticks every 16 px flow with `shift`"""
    c = h / 2

    def f(x, y):
        d = abs(y - c)
        if d > h / 2 - 1:
            return None
        pos = (x + shift) % 16
        if d < 1.6:
            return core
        if abs((pos - 8) - (d - 3)) < 1.0 and d >= 2.4 and d < h / 2 - 2:
            return chevron
        if d < 3.6:
            return mid
        return edge
    return f


def streak_fn(w, h, core, edge):
    c = h / 2

    def f(x, y):
        u = x / w
        hw = (h / 2) * min(1.0, 4 * u, 4 * (1 - u)) ** 0.8
        d = abs(y - c)
        if d > hw:
            return None
        return core if d < 1.0 else edge
    return f


def hawk_fn(size, flap, core, edge, dark):
    """white hawk, side view, flying to +x (right): raised wing with finger feathers, fan tail, hooked beak.  flap 0 = wings up, 1 = wings level"""
    def poly_in(pts, x, y):
        ins = False
        j = len(pts) - 1
        for i in range(len(pts)):
            xi, yi = pts[i]
            xj, yj = pts[j]
            if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi + 1e-9) + xi:
                ins = not ins
            j = i
        return ins

    def rot(pts, ang, ox=19.0, oy=18.0):
        ca, sa = math.cos(ang), math.sin(ang)
        return [(ox + (px - ox) * ca - (py - oy) * sa, oy + (px - ox) * sa + (py - oy) * ca) for px, py in pts]
    up = [(21, 17), (18, 8), (13, 2), (9, 1), (10, 4), (7, 4), (9, 7), (6, 8), (9, 10), (7, 12), (12, 13), (14, 17)]
    down = [(19, 20), (16, 26), (11, 30), (12, 26), (8, 27), (12, 23)]
    ang = flap * 0.75
    near = rot(up, -ang)
    far = rot(down, ang * 0.6)
    tail = [(11, 18), (3, 14), (1, 17), (2, 20), (4, 23), (11, 21)]

    def f(x, y):
        if poly_in(near, x, y):
            return core if ((x * 0.6 + y) % 5) > 1.2 else edge
        if ((x - 18) / 8.5) ** 2 + ((y - 19.5) / 3.0) ** 2 <= 1:
            return core if y < 19.5 else edge
        if math.hypot(x - 26.5, y - 17.5) <= 2.7:
            return core
        if 28 < x < 31.5 and 16.5 < y < 16.5 + (31.5 - x) * 0.9:
            return dark
        if poly_in(far, x, y):
            return edge
        if poly_in(tail, x, y):
            return edge if y > 18 else core
        if abs(x - 27.2) < 0.6 and abs(y - 17) < 0.6:
            return dark
        return None
    return f


def cube_tex(base, spk1, spk2, hi, lo, pal, seed):
    cv = Cv(16, 16, pal)
    rnd = np.random.RandomState(seed)

    def f(x, y):
        xi, yi = int(x), int(y)
        if yi == 0 or xi == 0:
            return hi
        if yi == 15 or xi == 15:
            return lo
        r = rnd.rand()
        return spk1 if r < 0.2 else spk2 if r < 0.3 else base
    cv.fill(f)
    return cv


# ---------------------------------------------------------------------------------------------------------------- the set
def build():
    made = {}

    def keep(style, name, cv, outline=None):
        if outline:
            cv.outline(outline)
        made[f"{style}_{name}"] = cv.save(f"{style}_{name}")

    # ======================================================= A : vanilla-faithful (grey/white, flat, like poof / sweep_attack / gust)
    cv = Cv(32, 32, PAL_A).fill(crescent_fn(14, 14.4, 21, 12.6, 16, "W", "L", "M"))
    for y in range(8, 24):
        cv.put(2, y, "W") if cv.p[y, 2, 3] else None
    keep("A", "crescent", cv)
    keep("A", "puff", Cv(16, 16, PAL_A).fill(disc_union([(5, 9, 4.5), (9.5, 6.5, 4.3), (10.5, 10, 4.2)], 16, ["W", "L", "M", "D"])))
    keep("A", "streak", Cv(32, 8, PAL_A).fill(streak_fn(32, 8, "W", "L")))
    keep("A", "leaf", Cv(16, 16, PAL_A).fill(leaf_fn(16, -0.7, "G", "H", "H")))
    keep("A", "spark", Cv(16, 16, PAL_A).fill(spark_fn(16, 7, 3, "W", "L")))
    keep("A", "ring", Cv(32, 32, PAL_A).fill(ring_fn(32, 12.5, 15.5, 0, 0, "W", "L")))
    keep("A", "swirl", Cv(32, 32, PAL_A).fill(swirl_fn(32, 3, 5, 14, 2.6, 0.0, "W", "L")))
    keep("A", "ribbon", Cv(32, 16, PAL_A).fill(sribbon_fn(32, 16, 3.2, 4.2, 0.0, "W", "L")))
    keep("A", "beam", Cv(32, 16, PAL_A).fill(beam_fn(32, 16, 0, "W", "L", "M", "L")))
    keep("A", "cloud", Cv(32, 16, PAL_A).fill(disc_union([(7, 10, 5), (14, 7, 6), (21, 9, 6), (26, 11, 4)], 32, ["W", "L", "M", "D"])))

    # ======================================================= B : voxel cube faces (16x16, bevel + speckle)
    keep("B", "cube_white", cube_tex("W", "N", "N", "W", "N", PAL_B, 1))
    keep("B", "cube_mint", cube_tex("N", "W", "G", "W", "G", PAL_B, 2))
    keep("B", "cube_lime", cube_tex("G", "N", "M", "N", "M", PAL_B, 3))
    keep("B", "cube_green", cube_tex("M", "G", "D", "G", "D", PAL_B, 4))

    # ======================================================= C : pixel-anime (dark outline, 2-frame flip-book)
    for name, cv in c_set(PAL_C).items():
        made["C_" + name] = cv.save("C_" + name)
    return made


# The C set, built in any five-step palette (the pack palette, or the greys of the tintable twins). Name -> canvas, outline already applied.
GAME_NAMES = ("crescent", "ribbon", "swirl", "spark", "streak", "leaf", "ring", "flash")        # what the shipped effects use (the hawk, puff and beam of the preview are not shipped)


def c_set(pal):
    out = {}
    for fr in (0, 1):
        sh = fr * 1.2
        # crescent: frame 1 = inner disc 1.2 px further out, so the blade "breathes" in two steps
        out[f"crescent_{fr}"] = Cv(32, 32, pal).fill(crescent_fn(15, 13.2, 20.5 + sh, 11.6 + sh * 0.2, 16, "W", "N", "G")).outline("O")
        out[f"ribbon_{fr}"] = Cv(32, 16, pal).fill(sribbon_fn(32, 16, 3.0, 3.6, fr * 0.9, "W", "N")).outline("O")
        out[f"swirl_{fr}"] = Cv(32, 32, pal).fill(swirl_fn(32, 3, 5, 13, 2.4, fr * 2 * math.pi / 6, "W", "N")).outline("O")
        out[f"spark_{fr}"] = Cv(16, 16, pal).fill(spark_fn(16, 5 + fr * 2, 2 + fr, "W", "N")).outline("O")
        cv = Cv(32, 16, pal).fill(beam_fn(32, 16, fr * 8, "W", "N", "G", "W"))
        for x in range(32):                                    # the beam tiles along x, so only its two long edges get the dark outline row
            cv.put(x, 0, "O")
            cv.put(x, 15, "O")
        out[f"beam_{fr}"] = cv
        out[f"streak_{fr}"] = Cv(32, 8, pal).fill(streak_fn(32, 8, "W", "N"))
        out[f"hawk_{fr}"] = Cv(32, 32, pal).fill(hawk_fn(32, fr, "W", "N", "G")).outline("O")
        out[f"leaf_{fr}"] = Cv(16, 16, pal).fill(leaf_fn(16, -0.7 + fr * 1.4, "G", "D", "O"))
        out[f"puff_{fr}"] = Cv(16, 16, pal).fill(disc_union([(5, 9, 4.2 + fr * 0.5), (9.5, 6.5, 4), (10.5, 10, 3.8)], 16, ["W", "N", "N", "G"])).outline("O")
        out[f"ring_{fr}"] = Cv(32, 32, pal).fill(ring_fn(32, 12.0, 14.5, 6, fr * 0.5, "W", "N")).outline("O")
        out[f"flash_{fr}"] = Cv(32, 32, pal).fill(spark_fn(32, 13 + fr * 2, 7 + fr * 2, "W", "N")).outline("O")         # big flash star for the palm / impact
    return out


def game_files():
    """file name (no folder) -> RGBA array of every texture the game gets: wmc_<name>_<frame>.png (pack palette) and wmc_<name>_<frame>_t.png (greys, tinted at draw time)"""
    files = {}
    for suffix, pal in (("", PAL_C), ("_t", PAL_T)):
        for name, cv in c_set(pal).items():
            if name.rsplit("_", 1)[0] in GAME_NAMES:
                files[f"wmc_{name}{suffix}.png"] = cv.p
    return files


def write_game():
    os.makedirs(GAME, exist_ok=True)
    files = game_files()
    for fn, arr in files.items():
        Image.fromarray(arr, "RGBA").save(os.path.join(GAME, fn))
    return files


def sheet(made):
    names = sorted(made)
    cell, per = 8, 8                              # every texture shown 8x (nearest), checker behind
    cols = 8
    items = []
    for n in names:
        a = made[n]
        im = Image.fromarray(a, "RGBA").resize((a.shape[1] * cell, a.shape[0] * cell), Image.NEAREST)
        items.append((n, im))
    cw = max(i.width for _, i in items) + 12
    ch = max(i.height for _, i in items) + 22
    rows = (len(items) + cols - 1) // cols
    sh = Image.new("RGB", (cols * cw, rows * ch), (28, 30, 34))
    d = ImageDraw.Draw(sh)
    for k, (n, im) in enumerate(items):
        x, y = (k % cols) * cw + 6, (k // cols) * ch + 4
        bg = Image.new("RGB", im.size, (62, 66, 74))
        sh.paste(bg, (x, y + 14))
        sh.paste(im, (x, y + 14), im)
        d.text((x, y), n, fill=(255, 230, 140))
    sh.save(os.path.join(os.path.dirname(OUT), "wind_mc_textures.png"))


def main():
    made = build()
    sheet(made)
    for style, lim in (("A", 6), ("B", 5), ("C", 5)):
        used = set()
        for n, a in made.items():
            if n.startswith(style + "_"):
                used |= colours_used(a)
        print(f"style {style}: {len(used)} colours (limit {lim})")
        if len(used) > lim:
            print("  TOO MANY COLOURS", sorted(used))
            sys.exit(1)
    print(len(made), "textures ->", OUT)
    # phase B: the shipped set. Hard alpha only, at most five colours per set, exact palette, sizes 8..32.
    files = write_game()
    native = {n: a for n, a in files.items() if not n.endswith("_t.png")}
    grey = {n: a for n, a in files.items() if n.endswith("_t.png")}
    for label, group, pal in (("native", native, PAL_C), ("tinted greys", grey, PAL_T)):
        used = set()
        for n, a in group.items():
            used |= colours_used(a)
            alpha = set(np.unique(a[..., 3]).tolist())
            if not alpha <= {0, 255}:
                print("  SOFT ALPHA in", n, sorted(alpha))
                sys.exit(1)
        allowed = set(pal.values())
        if not used <= allowed:
            print("  COLOURS OUTSIDE THE PALETTE", label, sorted(used - allowed))
            sys.exit(1)
        print(f"game set {label}: {len(group)} files, {len(used)} colours (limit 5)")
    print("game textures ->", GAME)


if __name__ == "__main__":
    main()
