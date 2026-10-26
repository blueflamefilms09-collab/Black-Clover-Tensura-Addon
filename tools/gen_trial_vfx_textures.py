"""Generates the Veldanava "Israfil, Lord of Trials" VFX textures (vfx.client.layer.TrialLayer).

Deterministic and font-free (every machine writes the same pixels). Written from scratch for this mod; shares no code or asset with any other mod.
Output: src/main/resources/assets/nusmp/textures/particle/trial_*.png  (new files only; nothing existing is touched)

    python tools/gen_trial_vfx_textures.py

Theme: the angel of trials, space and labyrinths. Deep indigo and midnight teal, cyan-white edges, small brass accents; maze lines (a circular labyrinth and a
rectangular one), coordinate ticks and crosshairs, folded-space glass shards, a trumpet and its sound rings, ghostly afterimages.
Most textures are white (+ alpha) so the vertex colour tints them. The ones with baked colour (pane, shard, chain, horn, ghost) carry the palette themselves and are drawn
with a white vertex colour. Nothing carries colour under a fully transparent pixel (rgb is zeroed where alpha is 0).

    trial_ring      thin crisp circle with coordinate ticks and a halo (shock rings, rims)
    trial_sound     a sound ring: a bright leading edge and fading echo rings behind it
    trial_maze      a circular labyrinth disc (the floor of the Dimensional Labyrinth)
    trial_pane      a glass pane carrying a rectangular maze, brass corner brackets (baked colour)
    trial_grid      a coordinate ring: crosshair with ticks, a dot grid, corner brackets
    trial_shard     a folded-space glass shard: angular, bright edges, facet lines (baked colour)
    trial_flare     a flare with a long horizontal streak, a short vertical one and a lens ring
    trial_mote      a tiny plus-shaped sparkle
    trial_beam      a light shaft with sound-pulse bands that cross it (seamless in V, it scrolls)
    trial_streak    heavy downward streaks, each with a bright head (seamless in V)
    trial_chain     a chain with tally-mark groups on its sides (seamless in V, baked colour)
    trial_horn      the trumpet in profile, bell to the right, with sound arcs (baked colour)
    trial_cracks    an angular ground-crack web (straight kinked fractures and a broken web), for ground planes
    trial_void      a dark sphere / disc with a carved spiral (drawn ALPHA, tinted near black)
    trial_invert    a premultiplied soft ring for the inverted-colour rim (drawn NEGATIVE)
    trial_ghost     a soft humanoid-height light column with scanlines (baked colour)
    trial_rift      a vertical tear in space with folded edge lines
    trial_dash      a dashed line with tick marks (seamless in V)
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
WHITE = (255, 255, 255, 255)
ICE = (222, 251, 255)
CYAN = (112, 232, 247)
TEAL = (30, 168, 190)
INDIGO = (74, 76, 205)
DEEP = (34, 30, 112)
VIOLET = (152, 104, 238)
BRASS = (214, 160, 62)
BRASS_HI = (255, 226, 148)
BRASS_DK = (150, 98, 30)


def save(arr_rgb, alpha, name):
    """Writes an RGBA png from float arrays (rgb 0..1, alpha 0..1). rgb is zeroed under a fully transparent pixel."""
    a = np.clip(alpha, 0, 1)
    rgb = np.clip(arr_rgb, 0, 1)
    px = np.dstack([rgb[..., 0], rgb[..., 1], rgb[..., 2], a])
    px = (px * 255 + 0.5).astype(np.uint8)
    px[px[..., 3] == 0, :3] = 0
    im = Image.fromarray(px, "RGBA")
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, name + ".png"), optimize=True)
    print("wrote", name, im.size)


def save_white(a, name, tint=(255, 255, 255)):
    h, w = a.shape
    rgb = np.dstack([np.full((h, w), tint[0] / 255.0), np.full((h, w), tint[1] / 255.0), np.full((h, w), tint[2] / 255.0)])
    save(rgb, a, name)


def alpha_of(im):
    return np.asarray(im.split()[3], dtype=np.float32) / 255.0


def smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


def grid(w, h):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    return xx, yy


def disc_coords(size):
    xx, yy = grid(size, size)
    c = (size - 1) / 2
    x, y = (xx - c) / c, (yy - c) / c
    return x, y, np.sqrt(x * x + y * y), np.arctan2(y, x)


def blur(a, radius):
    return np.asarray(Image.fromarray((np.clip(a, 0, 1) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(radius)), dtype=np.float32) / 255.0


def glow_add(a, radius, gain):
    return np.clip(a + blur(a, radius) * gain, 0, 1)


def lerp3(c0, c1, t):
    t = np.clip(t, 0, 1)[..., None]
    return (np.array(c0, np.float32) / 255.0) * (1 - t) + (np.array(c1, np.float32) / 255.0) * t


def mask_of(im):
    return np.asarray(im.convert("L"), dtype=np.float32) / 255.0


def new_mask(W, H):
    im = Image.new("L", (W, H), 0)
    return im, ImageDraw.Draw(im)


# ---------------------------------------------------------------------------------------------------- ring
def ring(size=512, ss=3):
    S = size * ss
    c = S / 2
    R = c * 0.985
    im, d = new_mask(S, S)
    for k in range(72):                                    # coordinate ticks just inside the ring: every 5 degrees, a long one every 30
        a = math.radians(k * 5)
        r1 = 0.935 if k % 6 else 0.895
        if k % 18 == 0:
            r1 = 0.85
        d.line([(c + math.cos(a) * R * 0.955, c + math.sin(a) * R * 0.955), (c + math.cos(a) * R * r1, c + math.sin(a) * R * r1)], fill=255 if k % 6 == 0 else 150, width=int((2.4 if k % 6 == 0 else 1.4) * ss))
    big = mask_of(im.resize((size, size), Image.LANCZOS))
    x, y, r, th = disc_coords(size)
    core = np.exp(-(((r - 0.965) / 0.0058) ** 2))
    halo = np.exp(-(((r - 0.965) / 0.030) ** 2)) * 0.32
    inner = np.exp(-(((r - 0.915) / 0.0034) ** 2)) * 0.28
    outer = np.exp(-(((r - 0.99) / 0.0028) ** 2)) * 0.22
    a = (core + halo + inner + outer) * (0.92 + 0.08 * np.cos(th * 8)) + big * 0.75
    a *= smooth(1.0, 0.985, r)
    save_white(a, "trial_ring")


# ---------------------------------------------------------------------------------------------------- sound ring
def sound(size=512):
    x, y, r, th = disc_coords(size)
    lead = np.exp(-(((r - 0.955) / 0.0085) ** 2))
    lead_halo = np.exp(-(((r - 0.955) / 0.034) ** 2)) * 0.45
    a = lead + lead_halo
    for k in range(1, 6):                                  # echo rings trailing inside the front, each thicker and fainter
        rk = 0.955 - 0.085 * k - 0.004 * k * k
        a = a + np.exp(-(((r - rk) / (0.008 + 0.0045 * k)) ** 2)) * (0.62 ** k) * 0.95
    band = np.where(r < 0.955, np.exp(-(0.955 - r) / 0.30) * 0.10, 0.0)
    flutter = 0.88 + 0.12 * np.cos(th * 6 + 3 * r)
    a = (a + band) * flutter
    a *= smooth(1.0, 0.975, r) * smooth(0.0, 0.12, r)
    save_white(a, "trial_sound")


# ---------------------------------------------------------------------------------------------------- circular labyrinth
def circular_maze(rng, ring_cells):
    """A perfect maze on concentric rings (ring i has ring_cells[i] sectors). Returns (passages_inward, passages_around) as sets."""
    n = len(ring_cells)
    visited = set()
    around = set()          # (ring, sector): open between sector and sector+1
    inward = set()          # (ring, sector): open between this cell and the cell inside it
    start = (n - 1, 0)
    stack = [start]
    visited.add(start)
    while stack:
        i, j = stack[-1]
        nb = []
        cj = ring_cells[i]
        nb.append(((i, (j + 1) % cj), ("a", i, j)))
        nb.append(((i, (j - 1) % cj), ("a", i, (j - 1) % cj)))
        if i > 0:
            ratio = ring_cells[i] // ring_cells[i - 1]
            nb.append(((i - 1, j // ratio), ("i", i, j)))
        if i < n - 1:
            ratio = ring_cells[i + 1] // ring_cells[i]
            for q in range(ratio):
                nb.append(((i + 1, j * ratio + q), ("i", i + 1, j * ratio + q)))
        rng.shuffle(nb)
        moved = False
        for cell, wall in nb:
            if cell in visited:
                continue
            visited.add(cell)
            (around if wall[0] == "a" else inward).add((wall[1], wall[2]))
            stack.append(cell)
            moved = True
            break
        if not moved:
            stack.pop()
    return inward, around


def maze(size=512, ss=3):
    rng = np.random.default_rng(1717)
    S = size * ss
    c = S / 2
    R = c * 0.97
    cells = [6, 12, 12, 24, 24, 24, 48, 48]
    n = len(cells)
    r_in, r_out = 0.16, 0.94
    radii = [r_in + (r_out - r_in) * k / n for k in range(n + 1)]
    inward, around = circular_maze(rng, cells)
    im, d = new_mask(S, S)
    w = int(2.3 * ss)

    def arc(rad, a0, a1):
        d.arc([c - rad * R, c - rad * R, c + rad * R, c + rad * R], math.degrees(a0), math.degrees(a1), fill=255, width=w)

    for i in range(n):
        cj = cells[i]
        for j in range(cj):
            a0, a1 = 2 * math.pi * j / cj, 2 * math.pi * (j + 1) / cj
            if i > 0 and (i, j) not in inward:             # wall between this cell and the one inside it
                arc(radii[i], a0, a1)
            if (i, j) not in around:                       # the radial wall after this cell
                a = a1
                d.line([(c + math.cos(a) * radii[i] * R, c + math.sin(a) * radii[i] * R), (c + math.cos(a) * radii[i + 1] * R, c + math.sin(a) * radii[i + 1] * R)], fill=255, width=w)
    arc(radii[n], 0, 2 * math.pi - 1e-3)                   # the outer wall, doubled
    arc(radii[n] - 0.012, 0, 2 * math.pi - 1e-3)
    arc(radii[0], 0, 2 * math.pi - 1e-3)
    d.ellipse([c - 0.07 * R, c - 0.07 * R, c + 0.07 * R, c + 0.07 * R], outline=255, width=w)      # the goal at the middle
    d.ellipse([c - 0.025 * R, c - 0.025 * R, c + 0.025 * R, c + 0.025 * R], fill=255)
    for k in range(48):                                    # tiny ticks outside the rim
        a = math.radians(k * 7.5)
        d.line([(c + math.cos(a) * R * 0.955, c + math.sin(a) * R * 0.955), (c + math.cos(a) * R * 0.985, c + math.sin(a) * R * 0.985)], fill=200, width=int(1.4 * ss))
    big = mask_of(im.resize((size, size), Image.LANCZOS))
    x, y, r, th = disc_coords(size)
    a = glow_add(big, 2.0, 0.55) * smooth(1.0, 0.975, r)
    save_white(a, "trial_maze")


# ---------------------------------------------------------------------------------------------------- pane (rectangular maze on glass)
def rect_maze(rng, cols, rows):
    """Perfect maze on a cols x rows grid. Returns walls as a set of ((x0,y0),(x1,y1)) grid-line segments to draw."""
    open_right = set()
    open_down = set()
    seen = {(0, 0)}
    stack = [(0, 0)]
    while stack:
        x, y = stack[-1]
        nb = []
        if x + 1 < cols:
            nb.append(((x + 1, y), ("r", x, y)))
        if x > 0:
            nb.append(((x - 1, y), ("r", x - 1, y)))
        if y + 1 < rows:
            nb.append(((x, y + 1), ("d", x, y)))
        if y > 0:
            nb.append(((x, y - 1), ("d", x, y - 1)))
        rng.shuffle(nb)
        moved = False
        for cell, wall in nb:
            if cell in seen:
                continue
            seen.add(cell)
            (open_right if wall[0] == "r" else open_down).add((wall[1], wall[2]))
            stack.append(cell)
            moved = True
            break
        if not moved:
            stack.pop()
    segs = []
    for x in range(cols - 1):
        for y in range(rows):
            if (x, y) not in open_right:
                segs.append(((x + 1, y), (x + 1, y + 1)))
    for x in range(cols):
        for y in range(rows - 1):
            if (x, y) not in open_down:
                segs.append(((x, y + 1), (x + 1, y + 1)))
    return segs


def pane(size=256, ss=4):
    rng = np.random.default_rng(909)
    S = size * ss
    cols, rows = 8, 12
    pad = 0.075 * S
    cw, ch = (S - 2 * pad) / cols, (S - 2 * pad) / rows
    im, d = new_mask(S, S)
    for (x0, y0), (x1, y1) in rect_maze(rng, cols, rows):
        d.line([(pad + x0 * cw, pad + y0 * ch), (pad + x1 * cw, pad + y1 * ch)], fill=255, width=int(2.4 * ss))
    lines = mask_of(im.resize((size, size), Image.LANCZOS))
    fr, fd = new_mask(S, S)                                # the frame: a double border
    fd.rectangle([pad * 0.45, pad * 0.45, S - pad * 0.45, S - pad * 0.45], outline=255, width=int(3.4 * ss))
    fd.rectangle([pad * 0.85, pad * 0.85, S - pad * 0.85, S - pad * 0.85], outline=170, width=int(1.4 * ss))
    frame = mask_of(fr.resize((size, size), Image.LANCZOS))
    br, bd = new_mask(S, S)                                # brass corner brackets
    L, t = 0.20 * S, int(5.6 * ss)
    o = pad * 0.30
    for (cx, cy, sx, sy) in ((o, o, 1, 1), (S - o, o, -1, 1), (o, S - o, 1, -1), (S - o, S - o, -1, -1)):
        bd.line([(cx, cy), (cx + sx * L, cy)], fill=255, width=t)
        bd.line([(cx, cy), (cx, cy + sy * L)], fill=255, width=t)
        bd.ellipse([cx - 4.2 * ss, cy - 4.2 * ss, cx + 4.2 * ss, cy + 4.2 * ss], fill=255)
    brackets = mask_of(br.resize((size, size), Image.LANCZOS))
    xx, yy = grid(size, size)
    u, v = xx / (size - 1), yy / (size - 1)
    glass = (0.16 + 0.07 * v) * np.ones_like(u)
    glare = np.exp(-(((u - 0.55 * v - 0.18) / 0.045) ** 2)) * 0.10 + np.exp(-(((u - 0.55 * v - 0.38) / 0.020) ** 2)) * 0.08
    edge_fade = smooth(0.0, 0.03, np.minimum.reduce([u, 1 - u, v, 1 - v]))
    line_a = np.clip(lines * 0.85 + frame, 0, 1)
    line_a = glow_add(line_a, 1.5, 0.5)
    alpha = np.clip(glass + glare + line_a + brackets, 0, 1) * edge_fade
    # colour: a deep indigo glass, teal lines, cyan-white frame, brass brackets
    base = lerp3(DEEP, INDIGO, v)
    line_w = np.clip(line_a * 1.3, 0, 1)
    rgb = base * (1 - line_w[..., None]) + lerp3(TEAL, ICE, np.clip(line_a - 0.3, 0, 1) * 1.4) * line_w[..., None]
    b = np.clip(brackets, 0, 1)[..., None]
    rgb = rgb * (1 - b) + (np.array(BRASS_HI, np.float32) / 255.0) * b
    rgb = rgb + glare[..., None] * 0.8
    save(rgb, alpha, "trial_pane")


# ---------------------------------------------------------------------------------------------------- coordinate grid ring
def grid_ring(size=256, ss=4):
    S = size * ss
    c = S / 2
    R = c * 0.985
    im, d = new_mask(S, S)

    def circle(rad, w, fill=255):
        d.ellipse([c - rad * R, c - rad * R, c + rad * R, c + rad * R], outline=fill, width=max(1, int(w * ss)))

    circle(0.985, 2.6)
    circle(0.80, 1.2, 190)
    circle(0.42, 1.2, 150)
    circle(0.065, 1.8)
    for k in range(4):                                     # the crosshair: lines with a gap at the middle, ticks every tenth
        a = math.radians(90 * k)
        ca, sa = math.cos(a), math.sin(a)
        d.line([(c + ca * R * 0.11, c + sa * R * 0.11), (c + ca * R * 0.95, c + sa * R * 0.95)], fill=255, width=int(1.8 * ss))
        for t in range(1, 10):
            rr = 0.1 * t
            if rr < 0.15:
                continue
            ln = 0.035 if t % 5 else 0.06
            px, py = c + ca * R * rr, c + sa * R * rr
            d.line([(px - sa * R * ln, py + ca * R * ln), (px + sa * R * ln, py - ca * R * ln)], fill=255 if t % 5 == 0 else 200, width=int(1.3 * ss))
    for k in range(4):                                     # corner brackets on the diagonals
        a = math.radians(45 + 90 * k)
        px, py = c + math.cos(a) * R * 0.62, c + math.sin(a) * R * 0.62
        ex, ey = math.cos(a) * 1, math.sin(a) * 1
        ln = R * 0.12
        d.line([(px, py), (px - ey * ln, py + ex * ln)], fill=255, width=int(2.2 * ss))
        d.line([(px, py), (px + ey * ln, py - ex * ln)], fill=255, width=int(2.2 * ss))
        d.ellipse([px - 2.2 * ss, py - 2.2 * ss, px + 2.2 * ss, py + 2.2 * ss], fill=255)
    for gx in range(-4, 5):                                # a dot grid inside
        for gy in range(-4, 5):
            if gx == 0 or gy == 0:
                continue
            px, py = c + gx * R * 0.17, c + gy * R * 0.17
            if math.hypot(gx, gy) * 0.17 > 0.75:
                continue
            d.ellipse([px - 1.5 * ss, py - 1.5 * ss, px + 1.5 * ss, py + 1.5 * ss], fill=170)
    big = mask_of(im.resize((size, size), Image.LANCZOS))
    save_white(glow_add(big, 1.5, 0.5), "trial_grid")


# ---------------------------------------------------------------------------------------------------- shard
def shard(size=64, ss=8):
    S = size * ss
    pts = [(0.50, 0.02), (0.80, 0.30), (0.93, 0.74), (0.58, 0.98), (0.20, 0.82), (0.10, 0.34)]
    P = [(x * S, y * S) for x, y in pts]
    fill_im, fd = new_mask(S, S)
    fd.polygon(P, fill=255)
    fill = mask_of(fill_im.resize((size, size), Image.LANCZOS))
    ed_im, ed = new_mask(S, S)
    ed.line(P + [P[0]], fill=255, width=int(2.6 * ss), joint="curve")
    for a, b in ((0, 3), (1, 4), (5, 2), (0, 2)):          # facet lines
        ed.line([P[a], P[b]], fill=120, width=int(1.2 * ss))
    edge = mask_of(ed_im.resize((size, size), Image.LANCZOS))
    xx, yy = grid(size, size)
    u, v = xx / (size - 1), yy / (size - 1)
    shade = 0.15 + 0.17 * np.clip(1.0 - (u * 0.7 + v * 0.7), 0, 1) + 0.10 * np.exp(-(((u - 0.62) / 0.12) ** 2))
    alpha = np.clip(fill * shade + edge, 0, 1)
    alpha = glow_add(alpha, 1.0, 0.35)
    rgb = lerp3(INDIGO, CYAN, 0.35 + 0.65 * (1 - v)) * (1 - edge[..., None]) + lerp3(CYAN, ICE, edge) * edge[..., None]
    save(rgb, alpha, "trial_shard")


# ---------------------------------------------------------------------------------------------------- flare / mote
def flare(size=128):
    x, y, r, th = disc_coords(size)
    ax, ay = np.abs(x), np.abs(y)
    core = np.exp(-(r * r) / 0.008)
    halo = np.exp(-(r * r) / 0.07) * 0.40
    hor = np.exp(-((ay / 0.016) ** 2)) * np.clip(1 - ax, 0, 1) ** 1.5
    ver = np.exp(-((ax / 0.018) ** 2)) * np.clip(1 - ay * 1.7, 0, 1) ** 2.2 * 0.8
    u, v = (x + y) * 0.7071, (x - y) * 0.7071
    d1 = np.exp(-((np.abs(v) / 0.020) ** 2)) * np.clip(1 - np.abs(u) * 2.2, 0, 1) ** 2.0 * 0.45
    d2 = np.exp(-((np.abs(u) / 0.020) ** 2)) * np.clip(1 - np.abs(v) * 2.2, 0, 1) ** 2.0 * 0.45
    lens = np.exp(-(((r - 0.34) / 0.012) ** 2)) * 0.30
    a = (core + halo + hor + ver + d1 + d2 + lens) * smooth(1.0, 0.92, r)
    save_white(a, "trial_flare")


def mote(size=32):
    x, y, r, th = disc_coords(size)
    ax, ay = np.abs(x), np.abs(y)
    arms = (np.exp(-((ay / 0.07) ** 2)) * np.clip(1 - ax, 0, 1) ** 1.6 + np.exp(-((ax / 0.07) ** 2)) * np.clip(1 - ay, 0, 1) ** 1.6) * 0.8
    core = np.exp(-(r * r) / 0.04)
    a = np.clip(arms + core, 0, 1) * smooth(1.0, 0.85, r)
    save_white(a, "trial_mote")


# ---------------------------------------------------------------------------------------------------- beam / streak
def beam(w=64, h=256):
    rng = np.random.default_rng(4242)
    xx, yy = grid(w, h)
    x = (xx - (w - 1) / 2) / ((w - 1) / 2)
    v = yy / h
    core = np.exp(-((x / 0.10) ** 2))
    mid = np.exp(-((x / 0.30) ** 2)) * 0.50
    halo = np.exp(-((x / 0.70) ** 2)) * 0.26
    a = core + mid + halo
    pulse = np.zeros_like(a)
    for k in range(5):                                     # sound-pulse bands that cross the shaft (periodic in V)
        v0 = (k + rng.uniform(-0.08, 0.08)) / 5.0
        pv = ((v - v0 + 0.5) % 1.0) - 0.5
        width = rng.uniform(0.012, 0.026)
        pulse = pulse + np.exp(-((pv / width) ** 2)) * np.exp(-((x / 0.62) ** 2)) * rng.uniform(0.45, 0.8)
    a = a * (0.72 + 0.28 * np.cos(v * math.tau * 3)) + pulse
    a *= smooth(1.0, 0.86, np.abs(x))
    save_white(a, "trial_beam")


def streak(w=32, h=256):
    rng = np.random.default_rng(777)
    xx, yy = grid(w, h)
    x = (xx - (w - 1) / 2) / ((w - 1) / 2)
    v = yy / h
    a = np.zeros_like(x)
    for _ in range(7):                                     # each streak: a long soft tail above a bright head (the head is the larger V = lower end)
        xk = rng.uniform(-0.68, 0.68)
        head = rng.uniform(0, 1)
        length = rng.uniform(0.30, 0.62)
        wid = rng.uniform(0.07, 0.16)
        pv = ((v - head + 0.5) % 1.0) - 0.5                # < 0 = above the head
        tail = np.where((pv <= 0) & (pv > -length), np.clip(1 + pv / length, 0, 1) ** 1.6, 0.0)
        headp = np.exp(-((pv / 0.018) ** 2)) * 1.0
        prof = np.exp(-(((x - xk) / wid) ** 2))
        a = a + prof * (tail * 0.75 + headp) * rng.uniform(0.6, 1.0)
    a += np.exp(-((x / 0.8) ** 2)) * 0.08
    a *= smooth(1.0, 0.82, np.abs(x))
    save_white(a, "trial_streak")


# ---------------------------------------------------------------------------------------------------- chain with tally marks
def chain(w=64, h=256, ss=6):
    W, H = w * ss, h * ss
    pitch = H / 8.0
    cx = W / 2
    link, ld = new_mask(W, H)
    tally, td = new_mask(W, H)
    for k in range(8):
        y0 = k * pitch
        if k % 2 == 0:                                     # a link seen face on: an upright oval
            ld.rounded_rectangle([cx - W * 0.135, y0 - pitch * 0.18, cx + W * 0.135, y0 + pitch * 1.18], radius=W * 0.135, outline=255, width=int(3.2 * ss))
        else:                                              # a link seen edge on: a thin bar
            ld.rounded_rectangle([cx - W * 0.045, y0 - pitch * 0.12, cx + W * 0.045, y0 + pitch * 1.12], radius=W * 0.04, fill=255)
    for g, (yc, side) in enumerate(((0.12, 1), (0.37, -1), (0.62, 1), (0.87, -1))):    # tally groups on alternate sides
        bx = cx + side * W * 0.34
        by = yc * H
        for s in range(4):
            sx = bx + (s - 1.5) * W * 0.055
            td.line([(sx, by - H * 0.032), (sx, by + H * 0.032)], fill=255, width=int(2.4 * ss))
        td.line([(bx - W * 0.15, by + H * 0.022), (bx + W * 0.15, by - H * 0.022)], fill=255, width=int(2.4 * ss))
    lk = mask_of(link.resize((w, h), Image.LANCZOS))
    tl = mask_of(tally.resize((w, h), Image.LANCZOS))
    alpha = np.clip(glow_add(lk, 1.2, 0.5) + glow_add(tl, 1.0, 0.45) * 0.95, 0, 1)
    rgb = np.zeros((h, w, 3), np.float32)
    rgb[...] = np.array(CYAN, np.float32) / 255.0 * (1 - lk[..., None]) + np.array(ICE, np.float32) / 255.0 * lk[..., None]
    t3 = np.clip(tl * 1.4, 0, 1)[..., None]
    rgb = rgb * (1 - t3) + np.array(BRASS_HI, np.float32) / 255.0 * t3
    save(rgb, alpha, "trial_chain")


# ---------------------------------------------------------------------------------------------------- trumpet
def horn(w=256, h=128, ss=5):
    W, H = w * ss, h * ss
    body, bd = new_mask(W, H)
    rim_im, rd = new_mask(W, H)
    mid = H * 0.50

    def tube(points, th):
        bd.line(points, fill=255, width=int(th * ss), joint="curve")
        for px, py in (points[0], points[-1]):
            bd.ellipse([px - th * ss / 2, py - th * ss / 2, px + th * ss / 2, py + th * ss / 2], fill=255)

    # mouthpiece cup and lead pipe
    bd.polygon([(W * 0.025, mid - H * 0.075), (W * 0.075, mid - H * 0.035), (W * 0.075, mid + H * 0.035), (W * 0.025, mid + H * 0.075)], fill=255)
    tube([(W * 0.07, mid), (W * 0.33, mid)], 7.5)
    # the coil: a rounded loop under the straight pipe
    lx0, lx1, ly0, ly1 = W * 0.17, W * 0.50, mid + H * 0.04, mid + H * 0.32
    tube([(lx0 + 14 * ss, ly0), (lx0, ly0 + 12 * ss), (lx0, ly1 - 12 * ss), (lx0 + 14 * ss, ly1), (lx1 - 14 * ss, ly1), (lx1, ly1 - 12 * ss), (lx1, ly0 + 12 * ss), (lx1 - 14 * ss, ly0)], 6.5)
    tube([(W * 0.33, mid), (W * 0.60, mid)], 8.5)
    # the bell: an exponential flare up to the rim
    top, bot = [], []
    for i in range(61):
        t = i / 60.0
        x = W * (0.58 + 0.22 * t)
        r = H * (0.036 + 0.40 * (t ** 2.6))
        top.append((x, mid - r))
        bot.append((x, mid + r))
    bd.polygon(top + bot[::-1], fill=255)
    # three valves on top of the pipe
    for vx in (0.30, 0.355, 0.41):
        bd.rectangle([W * vx - 5 * ss, mid - H * 0.145, W * vx + 5 * ss, mid - H * 0.03], fill=255)
        bd.rectangle([W * vx - 8 * ss, mid - H * 0.17, W * vx + 8 * ss, mid - H * 0.145], fill=255)
    # the rim, drawn brighter
    rr = H * (0.036 + 0.40)
    rd.ellipse([W * 0.80 - 5 * ss, mid - rr, W * 0.80 + 5 * ss, mid + rr], outline=255, width=int(2.6 * ss))
    for dx in (0.0, -0.075):                               # decorative bands on the bell
        t = 0.78 + dx * 2
        x = W * (0.58 + 0.22 * t)
        r = H * (0.036 + 0.40 * (t ** 2.6))
        rd.line([(x, mid - r), (x, mid + r)], fill=200, width=int(1.8 * ss))
    solid = mask_of(body.resize((w, h), Image.LANCZOS))
    rimm = mask_of(rim_im.resize((w, h), Image.LANCZOS))
    # sound arcs leaving the bell
    arcs, ad = new_mask(W, H)
    for k, (rad, wd) in enumerate(((0.060, 3.6), (0.105, 3.0), (0.150, 2.4))):
        cxk = W * 0.82
        ad.arc([cxk - rad * W, mid - rad * W * 1.5, cxk + rad * W, mid + rad * W * 1.5], -52, 52, fill=int(255 * (0.9 - 0.22 * k)), width=int(wd * ss))
    arc_m = mask_of(arcs.resize((w, h), Image.LANCZOS))
    # edge = solid minus its own erosion (an outline); the interior is a brass gradient with a highlight stripe along the top
    er = 1.0 - np.asarray(Image.fromarray((solid * 255).astype(np.uint8)).filter(ImageFilter.MinFilter(5)), dtype=np.float32) / 255.0
    outline = np.clip(er * solid * 2.0, 0, 1)
    xx, yy = grid(w, h)
    u, v = xx / (w - 1), yy / (h - 1)
    grad = np.clip(1.0 - np.abs(v - 0.46) * 2.4, 0, 1)
    interior = lerp3(BRASS_DK, BRASS_HI, grad ** 1.8) * (1 - outline[..., None]) + (np.array(ICE, np.float32) / 255.0) * outline[..., None]
    rgb = interior
    rgb = rgb * (1 - rimm[..., None]) + (np.array(ICE, np.float32) / 255.0) * rimm[..., None]
    arc_rgb = np.array(CYAN, np.float32) / 255.0
    sol = np.clip(solid * 0.92 + rimm * 0.5, 0, 1)
    alpha = np.clip(sol + arc_m * 0.9, 0, 1)
    rgb = rgb * np.clip(sol, 0, 1)[..., None] + arc_rgb * (1 - np.clip(sol, 0, 1))[..., None]
    alpha = glow_add(alpha, 2.0, 0.3)
    save(rgb, alpha, "trial_horn")


# ---------------------------------------------------------------------------------------------------- ground cracks (angular)
def cracks(size=512, ss=2):
    rng = np.random.default_rng(2929)
    S = size * ss
    c = S / 2
    R = c * 0.97
    im, d = new_mask(S, S)
    rays = 11
    base = rng.uniform(0, math.tau)
    angles = [base + math.tau * (k + rng.uniform(-0.28, 0.28)) / rays for k in range(rays)]
    web = [[] for _ in range(rays)]                        # the points of each ray, for the broken web between rays

    def walk(p, heading, length, width, depth, ray=None):
        x, y = p
        pts = [(x, y)]
        run = 0.0
        seg_i = 0
        while run < length:
            step = R * rng.uniform(0.13, 0.26)             # long straight segments and a sharp kink: glass, not earth
            nx, ny = x + math.cos(heading) * step, y + math.sin(heading) * step
            if math.hypot(nx - c, ny - c) > R:
                break
            t = run / length
            d.line([(x, y), (nx, ny)], fill=255, width=max(1, int(width * (1 - 0.72 * t) * ss)))
            pts.append((nx, ny))
            if depth > 0 and seg_i >= 1 and rng.random() < 0.40:
                walk((nx, ny), heading + rng.choice([-1, 1]) * math.radians(rng.uniform(28, 62)), length * rng.uniform(0.25, 0.42), width * 0.6, depth - 1)
            radial = math.atan2(ny - c, nx - c)            # kinks are bounded, and the heading is pulled back to the way out
            heading = radial + max(-0.6, min(0.6, (heading - radial) * -0.4 + rng.choice([-1, 1]) * math.radians(rng.uniform(12, 34))))
            x, y = nx, ny
            run += step
            seg_i += 1
        if ray is not None:
            web[ray] = pts

    for k, a in enumerate(angles):
        walk((c + math.cos(a) * R * 0.02, c + math.sin(a) * R * 0.02), a, R * rng.uniform(0.70, 1.0), 4.6, 1, ray=k)
    for k in range(rays):                                  # the web: straight chords between neighbouring rays at a few radii
        for lvl in (0.24, 0.46, 0.70):
            if rng.random() < 0.30:
                continue
            ka, kb = k, (k + 1) % rays
            pa = nearest_at(web[ka], c, R * lvl)
            pb = nearest_at(web[kb], c, R * lvl)
            if pa and pb and math.hypot(pa[0] - pb[0], pa[1] - pb[1]) < R * 0.8:
                d.line([pa, pb], fill=255, width=max(1, int(2.0 * ss)))
    big = mask_of(im.resize((size, size), Image.LANCZOS))
    xx, yy = grid(size, size)
    r = np.sqrt(((xx - size / 2) / (size / 2)) ** 2 + ((yy - size / 2) / (size / 2)) ** 2)
    a = big * smooth(1.0, 0.92, r)
    save_white(glow_add(a, 1.2, 0.30), "trial_cracks")


def nearest_at(pts, c, rad):
    best, bd = None, 1e18
    for p in pts:
        dd = abs(math.hypot(p[0] - c, p[1] - c) - rad)
        if dd < bd:
            best, bd = p, dd
    return best if bd < rad * 0.35 else None


# ---------------------------------------------------------------------------------------------------- void
def void(size=256):
    x, y, r, th = disc_coords(size)
    base = 0.97 * smooth(0.99, 0.80, r) + 0.0
    spiral = 0.5 + 0.5 * np.cos(th * 2 + r * 14.0)         # a two-armed spiral carved into the dark
    carve = np.exp(-(((spiral - 1.0) / 0.22) ** 2)) * smooth(0.10, 0.45, r) * smooth(0.97, 0.70, r)
    a = base * (1 - 0.30 * carve)
    a *= smooth(1.0, 0.94, r)
    save_white(a, "trial_void")


def invert(size=256):
    x, y, r, th = disc_coords(size)
    core = np.exp(-(((r - 0.90) / 0.030) ** 2))
    halo = np.exp(-(((r - 0.90) / 0.085) ** 2)) * 0.45
    a = np.clip(core + halo, 0, 1) * smooth(1.0, 0.96, r)
    save(np.dstack([a, a, a]), a, "trial_invert")            # premultiplied: black under the alpha (NEGATIVE ignores the alpha of the texel)


# ---------------------------------------------------------------------------------------------------- ghost (humanoid column)
def ghost(w=64, h=256, ss=6):
    W, H = w * ss, h * ss
    body, bd = new_mask(W, H)
    cx = W / 2
    hy = H * 0.032
    bd.ellipse([cx - W * 0.19, hy, cx + W * 0.19, hy + W * 0.40], fill=255)                                        # head
    bd.rectangle([cx - W * 0.08, hy + W * 0.32, cx + W * 0.08, hy + W * 0.50], fill=255)                          # neck
    bd.rounded_rectangle([cx - W * 0.47, hy + W * 0.44, cx + W * 0.47, hy + W * 0.70], radius=W * 0.12, fill=255)  # shoulders
    bd.polygon([(cx - W * 0.33, hy + W * 0.55), (cx + W * 0.33, hy + W * 0.55), (cx + W * 0.25, H * 0.56), (cx - W * 0.25, H * 0.56)], fill=255)         # torso
    bd.polygon([(cx - W * 0.26, H * 0.54), (cx + W * 0.26, H * 0.54), (cx + W * 0.22, H * 0.97), (cx + W * 0.025, H * 0.97), (cx + W * 0.02, H * 0.64), (cx - W * 0.02, H * 0.64), (cx - W * 0.025, H * 0.97), (cx - W * 0.22, H * 0.97)], fill=255)
    for sx in (-1, 1):                                     # arms hanging at the sides
        bd.polygon([(cx + sx * W * 0.40, hy + W * 0.58), (cx + sx * W * 0.50, hy + W * 0.58), (cx + sx * W * 0.45, H * 0.52), (cx + sx * W * 0.36, H * 0.52)], fill=255)
    m = mask_of(body.resize((w, h), Image.LANCZOS))
    soft = blur(m, 2.4)
    xx, yy = grid(w, h)
    v = yy / (h - 1)
    er = np.asarray(Image.fromarray((m * 255).astype(np.uint8)).filter(ImageFilter.MinFilter(5)), dtype=np.float32) / 255.0
    rim = np.clip(m - er, 0, 1)
    scan = 0.90 + 0.10 * np.cos(yy * math.pi / 2.5)       # faint scan lines
    inner = soft * (0.50 + 0.20 * np.exp(-(((v - 0.30) / 0.20) ** 2)))
    alpha = np.clip((inner + rim * 0.50) * scan, 0, 1) * (0.50 + 0.50 * smooth(0.99, 0.60, v))
    alpha = glow_add(alpha, 3.0, 0.65)
    rgb = lerp3(TEAL, ICE, np.clip(rim * 1.3 + (1 - v) * 0.35 + soft * 0.2, 0, 1))
    rgb = rgb * 0.92 + lerp3(INDIGO, VIOLET, v) * 0.12
    save(rgb, alpha, "trial_ghost")


# ---------------------------------------------------------------------------------------------------- rift (tear)
def rift(w=64, h=256, ss=6):
    rng = np.random.default_rng(31)
    W, H = w * ss, h * ss
    cx = W / 2
    ys = np.linspace(0, H, 40)
    off = np.cumsum(rng.uniform(-1, 1, len(ys))) * W * 0.035
    off -= np.linspace(off[0], off[-1], len(ys))
    off = np.clip(off, -W * 0.16, W * 0.16)
    env = np.sin(np.linspace(0, math.pi, len(ys))) ** 0.5
    pts = [(cx + o * e, y) for o, e, y in zip(off, env, ys)]
    core, cd = new_mask(W, H)
    cd.line(pts, fill=255, width=int(2.8 * ss), joint="curve")
    fold, fd = new_mask(W, H)
    for sgn, scale, wd in ((-1, 1.0, 1.4), (1, 0.8, 1.2), (-1, 2.0, 1.0), (1, 1.8, 0.9)):         # folded edges: copies of the tear shifted sideways, broken into pieces
        shift = sgn * W * 0.11 * scale
        seg = [(x + shift, y) for x, y in pts]
        for i in range(0, len(seg) - 3, 3):
            if rng.random() < 0.65:
                fd.line(seg[i:i + 4], fill=int(200 / scale), width=int(wd * ss), joint="curve")
    for _ in range(7):                                     # small notches (diamonds) along the tear
        i = int(rng.integers(3, len(pts) - 3))
        px, py = pts[i]
        r = rng.uniform(2.2, 4.2) * ss
        side = rng.choice([-1, 1])
        cd.polygon([(px, py - r * 1.8), (px + side * r * 1.4, py), (px, py + r * 1.8), (px - side * r * 0.3, py)], fill=230)
    c_m = mask_of(core.resize((w, h), Image.LANCZOS))
    f_m = mask_of(fold.resize((w, h), Image.LANCZOS))
    a = np.clip(c_m + f_m * 0.7, 0, 1)
    xx, yy = grid(w, h)
    v = yy / (h - 1)
    a *= smooth(0.0, 0.07, v) * smooth(1.0, 0.93, v)
    a = glow_add(a, 2.6, 0.7)
    rgb = lerp3(CYAN, ICE, np.clip(c_m * 1.3, 0, 1)) * (1 - 0.35 * f_m[..., None]) + lerp3(VIOLET, CYAN, 0.3) * 0.35 * f_m[..., None]
    save(rgb, a, "trial_rift")


# ---------------------------------------------------------------------------------------------------- dashed line
def dash(w=32, h=128):
    xx, yy = grid(w, h)
    x = (xx - (w - 1) / 2) / ((w - 1) / 2)
    v = yy / h
    seg = np.zeros_like(x)
    for v0, v1 in ((0.00, 0.34), (0.46, 0.60), (0.74, 0.80), (0.88, 0.92)):    # long dash, short dash, dot, dot (periodic)
        inside = ((v >= v0) & (v < v1)).astype(np.float32)
        edge = smooth(v0, v0 + 0.02, v) * smooth(v1, v1 - 0.02, v)
        seg = np.maximum(seg, inside * edge)
    line = np.exp(-((x / 0.17) ** 2)) * seg
    halo = np.exp(-((x / 0.55) ** 2)) * seg * 0.35
    tick = np.zeros_like(x)
    for vt in (0.0, 0.46, 0.74):                           # a small cross tick at the start of each dash
        pv = ((v - vt + 0.5) % 1.0) - 0.5
        tick = np.maximum(tick, np.exp(-((pv / 0.012) ** 2)) * smooth(0.95, 0.35, np.abs(x)) * 0.9)
    a = line + halo + tick
    save_white(a, "trial_dash")


if __name__ == "__main__":
    ring()
    sound()
    maze()
    pane()
    grid_ring()
    shard()
    flare()
    mote()
    beam()
    streak()
    chain()
    horn()
    cracks()
    void()
    invert()
    ghost()
    rift()
    dash()
