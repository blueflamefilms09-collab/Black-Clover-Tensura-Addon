"""Generates the Dragon Ball Ki VFX textures (KiLayer: Ki Blast, Kamehameha, Spirit Bomb, Instant Transmission, Dragon Fist, ...).

Deterministic and font-free (numpy maths + Pillow polygons, a fixed seed), in the Time Magic style: every texture is white or grey so the
vertex colour of the layer tints it (the ki colour of the caster's form), shapes are drawn supersampled and downsampled for clean edges, and
the colour of a clear texel is bled in from its visible neighbours so linear filtering never draws a dark fringe.
Output: src/main/resources/assets/anime_multiverse/textures/particle/ki_*.png

    python tools/gen_ki_vfx_textures.py

Look (Dragon Ball anime, cel animation): ki is drawn as HARD bright shapes, not soft smoke: a flat white-hot core with a crisp edge, one or two
flat cel steps of saturated halo round it, a thin rippling outer edge on beams, sharp star-burst impact flashes, speed lines, flat shock rings
that run over the ground with a dust ring behind them, and the golden dragon of the Dragon Fist as a shaded serpent ribbon with a horned head.
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "anime_multiverse", "textures", "particle")
PREFIX = "ki_"
SS = 4
RNG = np.random.RandomState(7071)

# name -> (width, height); KiVfx.TEXTURE_NAMES and KiVfxAssetTest read the same list
SIZES = {
    "core": (64, 64),
    "halo": (128, 128),
    "beam": (64, 128),
    "beam_edge": (64, 128),
    "ring": (128, 32),
    "dust": (128, 32),
    "flash": (128, 128),
    "spark": (32, 32),
    "streak": (16, 128),
    "slash": (128, 32),
    "swirl": (128, 128),
    "hex": (128, 128),
    "dragon_head": (64, 128),
    "dragon_body": (64, 128),
    "afterimage": (64, 128),
}


# ---------------------------------------------------------------------------------------------------- helpers
def grid(w, h):
    """Supersampled pixel-centre coordinates: x, y in [0, 1) across the image (y down)."""
    ys, xs = np.mgrid[0:h * SS, 0:w * SS].astype(np.float64)
    return (xs + 0.5) / (w * SS), (ys + 0.5) / (h * SS)


def down(a, w, h):
    return a.reshape(h, SS, w, SS).mean(axis=(1, 3))


def smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


def cel(r, steps):
    """Flat cel bands: steps = [(radius, alpha), ...] from the inside out, each edge 1.5 % soft."""
    a = np.zeros_like(r)
    for rad, al in steps:
        a = np.maximum(a, al * (1 - smooth(rad - 0.015, rad + 0.015, r)))
    return a


def finish(name, alpha, grey=None):
    """alpha (and optional grey 0..1) at supersampled size -> RGBA, bleed colour into clear texels, save."""
    w, h = SIZES[name]
    a = np.clip(down(alpha, w, h), 0, 1)
    g = np.ones_like(a) if grey is None else np.clip(down(grey, w, h), 0, 1)
    a8 = np.round(a * 255).astype(np.uint8)
    rgb = np.repeat((g * 255)[..., None], 3, axis=2)
    # bleed: a clear texel takes the alpha-weighted mean colour of its neighbourhood (white when nothing is near)
    vis = (a8 > 0).astype(np.float64)
    im = Image.fromarray(np.clip(rgb * vis[..., None], 0, 255).astype(np.uint8))
    wim = Image.fromarray((vis * 255).astype(np.uint8))
    num = np.asarray(im.filter(ImageFilter.BoxBlur(2)), dtype=np.float64)
    den = np.asarray(wim.filter(ImageFilter.BoxBlur(2)), dtype=np.float64)[..., None] / 255.0
    bled = np.where(den > 1e-3, num / np.maximum(den, 1e-3), 255.0)
    rgb = np.where(vis[..., None] > 0, rgb, bled)
    out = np.dstack([np.clip(np.round(rgb), 0, 255).astype(np.uint8), a8])
    os.makedirs(OUT, exist_ok=True)
    Image.fromarray(out, "RGBA").save(os.path.join(OUT, PREFIX + name + ".png"), optimize=True)
    print("wrote", PREFIX + name, (w, h))


def polar(x, y):
    dx, dy = x * 2 - 1, y * 2 - 1
    return np.sqrt(dx * dx + dy * dy), np.arctan2(dy, dx)


def tile_noise(n, period_cells, seed):
    """1D smooth periodic noise sampled at n points over [0,1)."""
    r = np.random.RandomState(seed)
    k = r.uniform(-1, 1, period_cells)
    t = np.arange(n) / n * period_cells
    i0 = np.floor(t).astype(int) % period_cells
    i1 = (i0 + 1) % period_cells
    f = t - np.floor(t)
    f = f * f * (3 - 2 * f)
    return k[i0] * (1 - f) + k[i1] * f


def poly_mask(name, polys, blur=0, width=None, lines=()):
    """Polygons (in 0..1 image units) and thick polylines -> supersampled alpha array."""
    w, h = SIZES[name]
    W, H = w * SS, h * SS
    im = Image.new("L", (W, H), 0)
    d = ImageDraw.Draw(im)
    for p, v in polys:
        d.polygon([(px * W, py * H) for px, py in p], fill=int(v * 255))
    for pts, lw, v in lines:
        d.line([(px * W, py * H) for px, py in pts], fill=int(v * 255), width=max(1, int(lw * W)), joint="curve")
    if blur:
        im = im.filter(ImageFilter.GaussianBlur(blur * SS))
    return np.asarray(im, dtype=np.float64) / 255.0


# ---------------------------------------------------------------------------------------------------- the textures
def core():
    x, y = grid(*SIZES["core"])
    r, _ = polar(x, y)
    # the white-hot heart: flat white disc with a crisp edge and a thin glow lip
    a = cel(r, [(0.62, 1.0), (0.78, 0.45)]) + 0.25 * np.exp(-((r - 0.8) / 0.08) ** 2) * (r < 0.98)
    a *= 1 - smooth(0.9, 0.99, r)
    finish("core", np.clip(a, 0, 1))


def halo():
    x, y = grid(*SIZES["halo"])
    r, th = polar(x, y)
    # three flat cel steps of saturated ki round the core, the outer step licked by a few soft flame tongues
    lick = 0.05 * np.sin(th * 7 + 0.6) + 0.03 * np.sin(th * 13 + 2.1)
    a = cel(r, [(0.30, 0.92), (0.52, 0.62)]) + cel(r - lick, [(0.78, 0.30)])
    a = np.maximum(a, 0.18 * np.exp(-(r / 0.8) ** 2))
    a *= 1 - smooth(0.86, 0.99, r)
    finish("halo", np.clip(a, 0, 1))


def beam():
    x, y = grid(*SIZES["beam"])
    u = np.abs(x * 2 - 1)
    # cross-section: flat white plateau, a cel step, a soft fringe; faint lengthwise flow lines (periodic in v so it tiles)
    a = cel(u, [(0.38, 1.0), (0.66, 0.66), (0.90, 0.28)])
    flow = 0.10 * np.sin(x * 2 * math.pi * 9 + np.sin(y * 2 * math.pi * 2) * 1.4) * (u > 0.36) * (u < 0.9)
    a = np.clip(a + flow, 0, 1) * (1 - smooth(0.92, 1.0, u))
    finish("beam", a)


def beam_edge():
    x, y = grid(*SIZES["beam_edge"])
    w, h = SIZES["beam_edge"]
    u = np.abs(x * 2 - 1)
    rows = y[:, 0]
    # the rippling edge: on each side a band whose outer boundary waves along the beam (periodic, so the scroll never shows a seam)
    nl = tile_noise(h * SS, 6, 11)[:, None]
    nr = tile_noise(h * SS, 6, 12)[:, None]
    wave = np.where(x < 0.5, nl, nr) * 0.12 + 0.05 * np.sin(rows[:, None] * 2 * math.pi * 4 + (x < 0.5) * 1.7)
    edge = 0.80 + wave
    band = (1 - smooth(edge - 0.02, edge + 0.02, u)) * smooth(0.46, 0.56, u)
    inner = 0.22 * (1 - smooth(0.50, 0.56, u))
    finish("beam_edge", np.clip(0.85 * band + inner, 0, 1))


def ring():
    x, y = grid(*SIZES["ring"])
    w, h = SIZES["ring"]
    # v = 0 (top row) is the OUTER edge of VfxVertexBuffer.ring: a hard bright leading edge with the shock fading in behind it,
    # broken into a few arcs along u (the anime shock ring is never a perfect line); periodic in u
    gap = tile_noise(w * SS, 8, 21)[None, :]
    lead = 1 - smooth(0.16, 0.24, np.abs(y - 0.12) + 0.05)
    trail = np.clip(1 - (y - 0.12) / 0.88, 0, 1) ** 2 * 0.55 * (y > 0.12)
    a = (lead + trail) * (0.75 + 0.25 * smooth(-0.6, 0.2, gap))
    a *= smooth(0.0, 0.06, y) * (1 - smooth(0.9, 1.0, y))
    finish("ring", np.clip(a, 0, 1))


def dust():
    x, y = grid(*SIZES["dust"])
    w, h = SIZES["dust"]
    # a band of lumpy dust puffs (light grey), the outer edge (top) fuzzy and lumpy; periodic in u
    top = 0.18 + 0.14 * tile_noise(w * SS, 10, 31)[None, :] + 0.06 * tile_noise(w * SS, 23, 32)[None, :]
    body = smooth(top - 0.06, top + 0.10, y) * (1 - smooth(0.70, 0.98, y))
    lumps = 0.65 + 0.35 * np.sin(x * 2 * math.pi * 14 + np.sin(x * 2 * math.pi * 3) * 2) * np.sin(y * math.pi * 3)
    grey = 0.78 + 0.18 * (1 - y)
    finish("dust", np.clip(body * lumps, 0, 1) * 0.9, grey)


def flash():
    x, y = grid(*SIZES["flash"])
    r, th = polar(x, y)
    # the anime impact star: 14 hard spikes of uneven length round a flat white disc
    n = 14
    lens = 0.55 + 0.42 * RNG.uniform(0, 1, n)
    k = (th + math.pi) / (2 * math.pi) * n
    i = np.floor(k).astype(int) % n
    f = k - np.floor(k)
    spike = lens[i] * (1 - np.abs(f - 0.5) * 2) ** 1.6
    a = (r < spike).astype(np.float64)
    a = np.maximum(a, cel(r, [(0.24, 1.0), (0.36, 0.55)]))
    a *= 1 - smooth(0.95, 1.0, r)
    finish("flash", a)


def spark():
    x, y = grid(*SIZES["spark"])
    dx, dy = np.abs(x * 2 - 1), np.abs(y * 2 - 1)
    # a hard four-point star with a tiny round heart
    star = (np.sqrt(dx) + np.sqrt(dy) < 0.78).astype(np.float64)
    r = np.sqrt(dx * dx + dy * dy)
    a = np.maximum(star, cel(r, [(0.18, 1.0)])) * (1 - smooth(0.9, 1.0, np.maximum(dx, dy)))
    finish("spark", a)


def streak():
    x, y = grid(*SIZES["streak"])
    u = np.abs(x * 2 - 1)
    # a speed line: thin hard centre, tapering to points at both ends
    taper = np.sin(np.clip(y, 0, 1) * math.pi) ** 0.7
    a = (1 - smooth(0.25 * taper, 0.25 * taper + 0.12, u)) * taper
    finish("streak", np.clip(a, 0, 1))


def slash():
    x, y = grid(*SIZES["slash"])
    v = np.abs(y * 2 - 1)
    # a ki cut (u along the arc, v across): a white blade line with one cel step of glow, the two ends trimmed
    a = cel(v, [(0.22, 1.0), (0.55, 0.45)]) * (1 - smooth(0.85, 1.0, v))
    a *= smooth(0.0, 0.08, x) * (1 - smooth(0.92, 1.0, x))
    finish("slash", a)


def swirl():
    x, y = grid(*SIZES["swirl"])
    r, th = polar(x, y)
    # three spiral arms of energy winding in to the middle (the Spirit Bomb's churning surface, the deflection spin)
    arms = 0.5 + 0.5 * np.cos(3 * th + np.log(np.maximum(r, 0.02)) * 4.5)
    a = smooth(0.35, 0.9, arms) * smooth(0.05, 0.25, r) * (1 - smooth(0.75, 0.98, r))
    a = np.maximum(a * 0.9, 0.15 * (1 - smooth(0.6, 0.98, r)))
    finish("swirl", np.clip(a, 0, 1))


def hex_():
    x, y = grid(*SIZES["hex"])
    r, _ = polar(x, y)
    # a ki shield seen as a sphere: dim face, a bright rim (fresnel) and a hexagon net bent round the ball
    dx, dy = x * 2 - 1, y * 2 - 1
    zz = np.sqrt(np.clip(1 - r * r, 0, 1))
    sx, sy = dx / (zz + 0.6), dy / (zz + 0.6)                         # the net crowds towards the rim like a sphere
    s = 7.0
    qx, qy = sx * s, sy * s
    # hex distance via the three axes
    ax = np.abs(qx)
    d1 = np.abs((qx * 0.5 + qy * 0.8660254) % 1.0 - 0.5)
    d2 = np.abs((qx * 0.5 - qy * 0.8660254) % 1.0 - 0.5)
    d3 = np.abs(qx % 1.0 - 0.5)
    edge = np.minimum(np.minimum(d1, d2), d3)
    net = 1 - smooth(0.035, 0.08, edge)
    rim = smooth(0.55, 0.97, r) ** 2
    a = (0.12 + 0.55 * rim + 0.42 * net * (0.4 + 0.6 * rim)) * (1 - smooth(0.965, 0.995, r))
    grey = 0.82 + 0.18 * rim
    del ax
    finish("hex", np.clip(a, 0, 1), grey)


def dragon_head():
    # Shenron-style head seen from above, snout at the TOP (v = 0) so VfxVertexBuffer.beam(neck -> snout) draws it pointing forward:
    # a long muzzle with the jaw open, two antler horns swept back to the neck, whiskers trailing, a bright eye ridge
    P = []
    head = [(0.5, 0.04), (0.64, 0.10), (0.70, 0.24), (0.76, 0.42), (0.82, 0.58), (0.74, 0.74), (0.62, 0.86), (0.56, 1.0), (0.44, 1.0), (0.38, 0.86),
            (0.26, 0.74), (0.18, 0.58), (0.24, 0.42), (0.30, 0.24), (0.36, 0.10)]
    P.append((head, 0.80))
    jaw_l = [(0.36, 0.12), (0.22, 0.02), (0.30, 0.22)]
    jaw_r = [(0.64, 0.12), (0.78, 0.02), (0.70, 0.22)]
    P += [(jaw_l, 0.9), (jaw_r, 0.9)]
    for s in (-1, 1):
        cx = 0.5 + s * 0.16
        horn = [(cx, 0.50), (0.5 + s * 0.44, 0.66), (0.5 + s * 0.48, 0.90), (0.5 + s * 0.38, 0.72), (0.5 + s * 0.22, 0.62)]
        tine = [(0.5 + s * 0.36, 0.70), (0.5 + s * 0.48, 0.58), (0.5 + s * 0.40, 0.74)]
        P += [(horn, 0.95), (tine, 0.95)]
    whisk = []
    for s in (-1, 1):
        pts = [(0.5 + s * (0.16 + 0.02 * k + 0.25 * math.sin(k * 0.45) * (k / 10)), 0.14 + 0.085 * k) for k in range(11)]
        whisk.append((pts, 0.03, 0.85))
    a = poly_mask("dragon_head", P, blur=0.4, lines=whisk)
    # shading: bright brow and snout ridge, darker cheeks, white eyes
    x, y = grid(*SIZES["dragon_head"])
    ridge = np.exp(-((x - 0.5) / 0.08) ** 2)
    grey = 0.62 + 0.30 * ridge + 0.08 * (1 - y)
    for s in (-1, 1):
        eye = ((x - (0.5 + s * 0.15)) / 0.05) ** 2 + ((y - 0.40) / 0.035) ** 2 < 1
        grey = np.where(eye, 1.0, grey)
        a = np.where(eye, 1.0, a)
    finish("dragon_head", np.clip(a, 0, 1), np.clip(grey, 0, 1))


def dragon_body():
    x, y = grid(*SIZES["dragon_body"])
    u = x * 2 - 1
    au = np.abs(u)
    # a serpent body tile (v along the body, 4 rows of scales per tile): a round tube, overlapping scale arcs, fins on both flanks
    per = 4.0
    fy = (y * per) % 1.0
    fin = 0.12 * np.clip(1 - fy * 1.6, 0, 1)                           # a little back-swept fin per scale row
    body = 1 - smooth(0.70 + fin - 0.03, 0.70 + fin + 0.03, au)
    shade = np.sqrt(np.clip(1 - (au / 0.8) ** 2, 0, 1))
    sx = (u * 3.0 + (np.floor(y * per * 2) % 2) * 0.5) % 1.0 - 0.5   # scales staggered row by row
    sy = (y * per * 2) % 1.0
    scale = np.sqrt(sx * sx + (sy * 0.8) ** 2)
    edge = smooth(0.40, 0.48, scale) * (1 - smooth(0.48, 0.56, scale))
    spine = np.exp(-(u / 0.06) ** 2)
    grey = 0.50 + 0.42 * shade - 0.22 * edge + 0.20 * spine
    finish("dragon_body", body * 0.98, np.clip(grey, 0, 1))


def afterimage():
    # a standing figure (the shape a fighter leaves behind for a frame): head, shoulders, two arms, two legs, soft edge, broken by scan lines
    P = []
    P.append(([(0.36, 0.20), (0.64, 0.20), (0.70, 0.27), (0.66, 0.55), (0.60, 0.58), (0.40, 0.58), (0.34, 0.55), (0.30, 0.27)], 1.0))   # torso
    P.append(([(0.31, 0.22), (0.24, 0.26), (0.17, 0.44), (0.20, 0.52), (0.26, 0.46), (0.34, 0.32)], 1.0))                             # arm l
    P.append(([(0.69, 0.22), (0.76, 0.26), (0.83, 0.44), (0.80, 0.52), (0.74, 0.46), (0.66, 0.32)], 1.0))                             # arm r
    P.append(([(0.40, 0.56), (0.50, 0.58), (0.47, 0.78), (0.44, 0.97), (0.36, 0.97), (0.37, 0.78)], 1.0))                             # leg l
    P.append(([(0.50, 0.58), (0.60, 0.56), (0.63, 0.78), (0.64, 0.97), (0.56, 0.97), (0.53, 0.78)], 1.0))                             # leg r
    a = poly_mask("afterimage", P, blur=0.8)
    x, y = grid(*SIZES["afterimage"])
    head = (((x - 0.5) / 0.11) ** 2 + ((y - 0.12) / 0.075) ** 2) < 1
    a = np.maximum(a, head * 1.0)
    hair = (np.abs(x - 0.5) < 0.14) & (y < 0.10) & (y > 0.02 + 0.06 * np.abs(np.sin((x - 0.5) * 40)))        # spiky hair
    a = np.maximum(a, hair * 1.0)
    scan = 0.72 + 0.28 * (np.sin(y * 2 * math.pi * 22) > -0.3)
    finish("afterimage", np.clip(a * scan * 0.9, 0, 1))


MAKERS = {"core": core, "halo": halo, "beam": beam, "beam_edge": beam_edge, "ring": ring, "dust": dust, "flash": flash, "spark": spark,
          "streak": streak, "slash": slash, "swirl": swirl, "hex": hex_, "dragon_head": dragon_head, "dragon_body": dragon_body, "afterimage": afterimage}

if __name__ == "__main__":
    assert set(MAKERS) == set(SIZES)
    for name in SIZES:
        MAKERS[name]()
