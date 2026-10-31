"""Why: the "Multiverse of Anime" advancement tab (data/nusmp/advancement/root.json) used the altar block texture (a flat green 16x16 tile) as its
background. 3.44.0 gives the tab its own seamless background, three candidates, all dark and low-contrast so the advancement frames and lines stay readable.

How the game draws it: Minecraft 1.21.1's advancement tab fills the window black and then blits ONE texture over and over in 16x16 GUI-pixel cells, with the
texture size declared as 16x16. A larger square texture therefore shows complete inside each cell (it is squeezed into the cell), so a 32x32 tile is pixel-exact
at GUI scale 2 and 4 (and slightly uneven at 3). Every tile here is 32x32 and wraps at the edges, so the cells join without a seam.

    python tools/quests/gen_tab_background.py            # writes the three textures + docs/previews/advancement_tab/*.png
    python tools/quests/gen_tab_background.py --check    # exits 1 if a texture on disk differs from what the script makes

Output (textures): src/main/resources/assets/nusmp/textures/gui/advancements/{multiverse_weave,sigil_tiles,grimoire_pages}.png
The shipped one is multiverse_weave (root.json). To use another, change the "background" string in data/nusmp/advancement/root.json AND ROOT_BG in
tools/quests/spec_adv.py. The old texture textures/block/grimoire_altar_top.png stays in the jar (the altar block still uses it).
"""
import math
import os
import random
import sys

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "gui", "advancements")
PREV = os.path.join(ROOT, "docs", "previews", "advancement_tab")
N = 32


def rgb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255, 255)


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(4))


def put(img, x, y, c):
    img.putpixel((x % N, y % N), c)      # wraps: nothing is ever cut at an edge


def hashnoise(seed):
    """A 32x32 field in 0..1 that wraps (value noise with period 8, so it tiles)."""
    rnd = random.Random(seed)
    g = [[rnd.random() for _ in range(N // 8)] for _ in range(N // 8)]
    out = [[0.0] * N for _ in range(N)]
    for y in range(N):
        for x in range(N):
            gx, gy = x / 8.0, y / 8.0
            x0, y0 = int(gx) % (N // 8), int(gy) % (N // 8)
            x1, y1 = (x0 + 1) % (N // 8), (y0 + 1) % (N // 8)
            fx, fy = gx - int(gx), gy - int(gy)
            fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
            top = g[y0][x0] * (1 - fx) + g[y0][x1] * fx
            bot = g[y1][x0] * (1 - fx) + g[y1][x1] * fx
            out[y][x] = top * (1 - fy) + bot * fy
    return out


# ---------------------------------------------------------------- candidate A: the weave
def weave():
    """Midnight blue with a faint woven lattice (the threads between worlds) and a few stars, one in each franchise colour."""
    lo, hi = rgb(0x070A1C), rgb(0x0F1736)
    line, node = rgb(0x121B48), rgb(0x24347A)
    img = Image.new("RGBA", (N, N))
    f = hashnoise(11)
    for y in range(N):
        for x in range(N):
            img.putpixel((x, y), mix(lo, hi, f[y][x]))
    for y in range(N):
        for x in range(N):
            if (x + y) % 16 == 0 or (x - y) % 16 == 0:
                put(img, x, y, mix(img.getpixel((x, y)), line, 0.85))
    for nx in range(0, N, 8):
        for ny in range(0, N, 8):
            if (nx + ny) % 16 == 0:                      # lattice crossings
                put(img, nx, ny, node)
    stars = [(5, 3, 0xA83C34), (21, 5, 0xA88A28), (13, 12, 0x3C86A8), (28, 15, 0x46965C), (9, 22, 0x7452A8), (24, 27, 0xA85C8C), (17, 20, 0xBFD0FF)]
    for sx, sy, col in stars:
        c = rgb(col)
        dim = mix(rgb(0x0B1030), c, 0.35)
        put(img, sx, sy, c)
        if col == 0xBFD0FF:                              # the two bigger stars get a plus shape
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                put(img, sx + dx, sy + dy, dim)
    return img


# ---------------------------------------------------------------- candidate B: sigil tiles
def sigil():
    """Deep violet with one grimoire sigil per cell: a ring, an eight-point star and four corner diamonds, all dim."""
    lo, hi = rgb(0x0A0618), rgb(0x150C2C)
    ring, star, spark = rgb(0x2A1B58), rgb(0x3A2678), rgb(0x6A4CC0)
    img = Image.new("RGBA", (N, N))
    f = hashnoise(23)
    for y in range(N):
        for x in range(N):
            img.putpixel((x, y), mix(lo, hi, f[y][x]))
    d = ImageDraw.Draw(img)
    c = 15.5
    for a in range(0, 360 * 4):
        t = math.radians(a / 4.0)
        put(img, int(round(c + 13 * math.cos(t))), int(round(c + 13 * math.sin(t))), ring)
        put(img, int(round(c + 9 * math.cos(t))), int(round(c + 9 * math.sin(t))), mix(ring, lo, 0.35))
    pts = []
    for k in range(8):
        t = math.radians(k * 45 + 22.5)
        pts.append((c + 12 * math.cos(t), c + 12 * math.sin(t)))
    for k in range(8):                                    # {8/3} star: join every third point
        x0, y0 = pts[k]
        x1, y1 = pts[(k + 3) % 8]
        for s in range(41):
            u = s / 40.0
            put(img, int(round(x0 + (x1 - x0) * u)), int(round(y0 + (y1 - y0) * u)), star)
    for sx, sy in ((15, 15), (16, 15), (15, 16), (16, 16)):
        put(img, sx, sy, spark)
    for sx, sy in ((0, 0), (0, N - 1), (N - 1, 0), (N - 1, N - 1)):   # corner diamonds join into one in the middle of four cells
        put(img, sx, sy, spark)
    return img


# ---------------------------------------------------------------- candidate C: grimoire pages
def pages():
    """Dark ink-brown pages: ruled lines, a margin and a few runic ticks. Reads like the inside of a grimoire."""
    lo, hi = rgb(0x120D0A), rgb(0x1E1610)
    rule, margin, ink = rgb(0x2A1F16), rgb(0x4A2A22), rgb(0x6B5336)
    img = Image.new("RGBA", (N, N))
    f = hashnoise(37)
    for y in range(N):
        for x in range(N):
            img.putpixel((x, y), mix(lo, hi, f[y][x]))
    for y in range(3, N, 8):
        for x in range(N):
            put(img, x, y, mix(img.getpixel((x, y)), rule, 0.9))
    for y in range(N):
        put(img, 3, y, mix(img.getpixel((3, y)), margin, 0.8))
    rnd = random.Random(5)
    for row in range(4):
        x = 6
        while x < N - 3:
            w = rnd.choice((1, 2, 2, 3))
            for k in range(w):
                put(img, x + k, row * 8 + 5, ink)
            x += w + rnd.choice((2, 3))
    return img


CANDIDATES = {"multiverse_weave": weave, "sigil_tiles": sigil, "grimoire_pages": pages}


def mock(tile, scale=3):
    """What the tab roughly looks like: the tile repeated over a window, with a few advancement frames and lines on top (so contrast can be judged)."""
    cols, rows = 12, 7
    img = Image.new("RGBA", (cols * 16 * scale, rows * 16 * scale), (0, 0, 0, 255))
    t = tile.resize((16 * scale, 16 * scale), Image.NEAREST)
    for cy in range(rows):
        for cx in range(cols):
            img.paste(t, (cx * 16 * scale, cy * 16 * scale))
    d = ImageDraw.Draw(img)
    nodes = [(2, 3), (5, 2), (5, 5), (8, 3), (10, 5)]
    px = lambda cell: (int(cell[0] * 16 * scale + 8 * scale), int(cell[1] * 16 * scale + 8 * scale))
    for a, b in ((0, 1), (0, 2), (1, 3), (2, 4), (3, 4)):
        d.line([px(nodes[a]), px(nodes[b])], fill=(235, 235, 235, 255), width=max(2, scale))
    for cell in nodes:
        x, y = px(cell)
        r = 13 * scale // 2 * 2 // 2
        d.rectangle([x - r, y - r, x + r, y + r], fill=(60, 60, 60, 255), outline=(255, 215, 90, 255), width=max(1, scale // 2 + 1))
        d.rectangle([x - r // 2, y - r // 2, x + r // 2, y + r // 2], fill=(190, 150, 255, 255))
    return img


def main(check):
    os.makedirs(OUT, exist_ok=True)
    os.makedirs(PREV, exist_ok=True)
    bad = 0
    for name, fn in CANDIDATES.items():
        img = fn()
        path = os.path.join(OUT, name + ".png")
        if check:
            if not os.path.isfile(path) or Image.open(path).convert("RGBA").tobytes() != img.tobytes():
                print("STALE:", path)
                bad += 1
            continue
        img.save(path, optimize=True)
        mock(img).save(os.path.join(PREV, name + "_tab.png"))
        print("wrote", os.path.relpath(path, ROOT))
    if check:
        sys.exit(1 if bad else 0)
    sheet = Image.new("RGBA", (12 * 48 * 1, 3 * 7 * 48 + 20), (0, 0, 0, 255))
    y = 0
    for name in CANDIDATES:
        m = Image.open(os.path.join(PREV, name + "_tab.png")).convert("RGBA")
        sheet.paste(m, (0, y))
        y += m.height + 10
    sheet.save(os.path.join(PREV, "all_three.png"))


if __name__ == "__main__":
    main("--check" in sys.argv)
