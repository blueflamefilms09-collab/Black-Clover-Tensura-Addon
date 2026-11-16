"""Generates the Black Sword Greed (Berserk of Gluttony) and devil-hit VFX textures (GreedLayer + GreedDevilHits).

Deterministic and font-free (numpy maths + Pillow polygons and lines, a fixed seed), in the style of tools/gen_ki_vfx_textures.py: every texture
is white or grey so the layer's vertex colour tints it (ink-black with the alpha blend, crimson / the devil's colour with the additive blend),
shapes are drawn supersampled and downsampled for clean edges, and the colour of a clear texel is bled in from its visible neighbours so linear
filtering never draws a dark fringe.
Output: src/main/resources/assets/anime_multiverse/textures/particle/greed_*.png

    python tools/gen_greed_vfx_textures.py

Look (Berserk of Gluttony anime): Greed is ink-black with a crimson edge. Blade cuts are hard black crescents with a thin blood-red rim, black
lightning is a jagged black core in a red glow, the hunger is a black void ringed with teeth, the shield a dome of black scales, the eyes two
red glints. The devil hits use the same kit plus one signature piece per anime world: a cracked sigil (Black Clover), blood drops (Chainsaw Man),
the contract pentagram and black feathers (Black Butler), flames (Blue Exorcist, Dororo), the Brand of Sacrifice (Berserk), curse runes (Fairy Tail).
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "anime_multiverse", "textures", "particle")
PREFIX = "greed_"
SS = 4
RNG = np.random.RandomState(8085)

# name -> (width, height); GreedVfx.TEXTURE_NAMES and GreedVfxAssetTest read the same list
SIZES = {
    "core": (64, 64),
    "glow": (128, 128),
    "void": (128, 128),
    "slash": (128, 32),
    "bolt": (16, 128),
    "streak": (16, 128),
    "ring": (128, 32),
    "scales": (128, 128),
    "scale": (32, 32),
    "teeth": (128, 32),
    "glint": (128, 32),
    "flame": (64, 128),
    "feather": (32, 64),
    "pentagram": (128, 128),
    "crack": (128, 128),
    "runes": (256, 32),
    "brand": (64, 128),
    "blood": (32, 32),
    "flash": (128, 128),
}


# ---------------------------------------------------------------------------------------------------- helpers
def grid(w, h):
    ys, xs = np.mgrid[0:h * SS, 0:w * SS].astype(np.float64)
    return (xs + 0.5) / (w * SS), (ys + 0.5) / (h * SS)


def down(a, w, h):
    return a.reshape(h, SS, w, SS).mean(axis=(1, 3))


def smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


def cel(r, steps):
    a = np.zeros_like(r)
    for rad, al in steps:
        a = np.maximum(a, al * (1 - smooth(rad - 0.015, rad + 0.015, r)))
    return a


def finish(name, alpha, grey=None):
    w, h = SIZES[name]
    a = np.clip(down(alpha, w, h), 0, 1)
    g = np.ones_like(a) if grey is None else np.clip(down(grey, w, h), 0, 1)
    a8 = np.round(a * 255).astype(np.uint8)
    rgb = np.repeat((g * 255)[..., None], 3, axis=2)
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
    r = np.random.RandomState(seed)
    k = r.uniform(-1, 1, period_cells)
    t = np.arange(n) / n * period_cells
    i0 = np.floor(t).astype(int) % period_cells
    i1 = (i0 + 1) % period_cells
    f = t - np.floor(t)
    f = f * f * (3 - 2 * f)
    return k[i0] * (1 - f) + k[i1] * f


def canvas(name):
    w, h = SIZES[name]
    im = Image.new("L", (w * SS, h * SS), 0)
    return im, ImageDraw.Draw(im), w * SS, h * SS


def arr(im, blur=0.0):
    if blur:
        im = im.filter(ImageFilter.GaussianBlur(blur * SS))
    return np.asarray(im, dtype=np.float64) / 255.0


def edge_clear(a, x, y, m=0.04):
    """Forces a clear frame round the image (strip textures and sprites must have clear texels at their border)."""
    return a * smooth(0, m, x) * smooth(0, m, y) * (1 - smooth(1 - m, 1, x)) * (1 - smooth(1 - m, 1, y))


# ---------------------------------------------------------------------------------------------------- the textures
def core():
    x, y = grid(*SIZES["core"])
    r, _ = polar(x, y)
    a = cel(r, [(0.6, 1.0), (0.78, 0.45)]) * (1 - smooth(0.9, 0.99, r))
    finish("core", np.clip(a, 0, 1))


def glow():
    x, y = grid(*SIZES["glow"])
    r, _ = polar(x, y)
    a = np.maximum(0.85 * np.exp(-(r / 0.42) ** 2), cel(r, [(0.22, 0.6)])) * (1 - smooth(0.85, 0.99, r))
    finish("glow", np.clip(a, 0, 1))


def void():
    x, y = grid(*SIZES["void"])
    r, th = polar(x, y)
    # the hungry dark: a solid ink disc whose rim is ragged like torn cloth, three faint spiral arms of a lighter grey drawn in
    edge = 0.74 + 0.05 * np.sin(th * 9 + 0.4) + 0.035 * np.sin(th * 17 + 2.0) + 0.02 * np.sin(th * 31)
    a = 1 - smooth(edge - 0.035, edge + 0.035, r)
    arms = 0.5 + 0.5 * np.cos(3 * th + np.log(np.maximum(r, 0.02)) * 5.0)
    grey = 0.35 + 0.65 * smooth(0.6, 0.95, arms) * smooth(0.1, 0.4, r) * (1 - smooth(0.55, 0.75, r))
    a *= 1 - smooth(0.9, 0.99, r)
    finish("void", np.clip(a, 0, 1), grey)


def slash():
    x, y = grid(*SIZES["slash"])
    # an arc of VfxVertexBuffer.arc: v = 0 (top) is the OUTER edge. A hard bright cutting edge on the outside, the blade body fading in behind it
    lead = 1 - smooth(0.10, 0.18, np.abs(y - 0.14))
    body = np.clip(1 - (y - 0.14) / 0.8, 0, 1) ** 1.5 * 0.7 * (y > 0.14)
    a = np.maximum(lead, body) * smooth(0.0, 0.06, y) * (1 - smooth(0.9, 1.0, y))
    a *= smooth(0.0, 0.05, x) * (1 - smooth(0.95, 1.0, x))
    grey = 0.7 + 0.3 * lead
    finish("slash", np.clip(a, 0, 1), np.clip(grey, 0, 1))


def bolt():
    x, y = grid(*SIZES["bolt"])
    u = np.abs(x * 2 - 1)
    # a lightning strand across: a hard thin core and a glow round it; constant along v (the zigzag is drawn by the layer)
    a = np.maximum(1 - smooth(0.16, 0.26, u), 0.45 * np.exp(-(u / 0.5) ** 2)) * (1 - smooth(0.85, 1.0, u))
    finish("bolt", np.clip(a, 0, 1))


def streak():
    x, y = grid(*SIZES["streak"])
    u = np.abs(x * 2 - 1)
    taper = np.sin(np.clip(y, 0, 1) * math.pi) ** 0.7
    a = (1 - smooth(0.25 * taper, 0.25 * taper + 0.12, u)) * taper
    finish("streak", np.clip(a, 0, 1))


def ring():
    x, y = grid(*SIZES["ring"])
    w, h = SIZES["ring"]
    gap = tile_noise(w * SS, 9, 41)[None, :]
    lead = 1 - smooth(0.16, 0.24, np.abs(y - 0.12) + 0.05)
    trail = np.clip(1 - (y - 0.12) / 0.88, 0, 1) ** 2 * 0.5 * (y > 0.12)
    a = (lead + trail) * (0.7 + 0.3 * smooth(-0.6, 0.2, gap))
    a *= smooth(0.0, 0.06, y) * (1 - smooth(0.9, 1.0, y))
    finish("ring", np.clip(a, 0, 1))


def scales():
    x, y = grid(*SIZES["scales"])
    r, _ = polar(x, y)
    # the shield dome seen as a ball: rows of overlapping fish-scale arcs bent round the sphere, a dim face, a strong rim
    dx, dy = x * 2 - 1, y * 2 - 1
    zz = np.sqrt(np.clip(1 - r * r, 0, 1))
    sx, sy = dx / (zz + 0.55) * 4.2, dy / (zz + 0.55) * 4.2
    row = np.floor(sy)
    cx = (sx + (row % 2) * 0.5) % 1.0 - 0.5
    cy = sy - row
    d = np.sqrt(cx * cx + (cy * 1.1) ** 2)
    arcl = smooth(0.40, 0.47, d) * (1 - smooth(0.47, 0.55, d)) * (cy > 0.05)
    rim = smooth(0.55, 0.97, r) ** 2
    a = (0.45 + 0.45 * rim + 0.5 * arcl * (0.5 + 0.5 * rim)) * (1 - smooth(0.965, 0.995, r))
    grey = 0.75 + 0.25 * np.maximum(arcl, rim)
    finish("scales", np.clip(a, 0, 1), np.clip(grey, 0, 1))


def scale():
    im, d, W, H = canvas("scale")
    # one scale shard: a rounded shield shape with a pointed tip and a ridge
    pts = [(0.5, 0.06), (0.82, 0.22), (0.86, 0.55), (0.5, 0.95), (0.14, 0.55), (0.18, 0.22)]
    d.polygon([(px * W, py * H) for px, py in pts], fill=230)
    d.line([(0.5 * W, 0.12 * H), (0.5 * W, 0.86 * H)], fill=255, width=int(0.08 * W))
    finish("scale", arr(im, 0.3))


def teeth():
    im, d, W, H = canvas("teeth")
    # a ring band of fangs for VfxVertexBuffer.ring: v = 0 is the OUTER edge (the gum), the fangs point in, uneven lengths; 12 per tile, periodic
    n = 12
    d.rectangle([0, int(0.06 * H), W, int(0.2 * H)], fill=200)
    lens = 0.55 + 0.35 * RNG.uniform(0, 1, n)
    for i in range(n):
        x0, x1 = i / n, (i + 1) / n
        tip = (x0 + x1) / 2 + (RNG.uniform(-0.15, 0.15)) / n
        d.polygon([(x0 * W + 1, 0.18 * H), (x1 * W - 1, 0.18 * H), (tip * W, (0.2 + lens[i] * 0.74) * H)], fill=255)
    a = arr(im, 0.25)
    x, y = grid(*SIZES["teeth"])
    a *= smooth(0.0, 0.05, y) * (1 - smooth(0.96, 1.0, y))
    finish("teeth", np.clip(a, 0, 1))


def glint():
    x, y = grid(*SIZES["glint"])
    dx, dy = np.abs(x * 2 - 1), np.abs(y * 2 - 1)
    # a red eye glint: a long thin horizontal flare with a sharp diamond heart
    flare = np.exp(-(dy / (0.10 + 0.25 * (1 - dx))) ** 2) * (1 - dx) ** 2.2
    diamond = 1 - smooth(0.0, 0.08, dx * 0.25 + dy * 0.9 - 0.18)
    a = np.maximum(flare, diamond) * (1 - smooth(0.92, 1.0, dx)) * (1 - smooth(0.9, 1.0, dy))
    finish("glint", np.clip(a, 0, 1))


def flame():
    x, y = grid(*SIZES["flame"])
    t = 1 - y                                                    # height above the base, 0 at the bottom row
    # an upright flame tongue (anime cel fire): a round base, a wavy licking outline with a side tongue on each flank, a tip leaning a little,
    # a brighter inner tongue
    sway = 0.07 * np.sin(t * 6.0 + 0.5) * t
    base = np.where(t < 0.25, 0.34 * np.sqrt(np.clip(1 - ((0.25 - t) / 0.25) ** 2, 0, 1)), 0.34 * np.clip((1 - t) / 0.75, 0, 1) ** 0.85)
    right = x > 0.5 + sway
    hw = base * (1 + 0.14 * np.sin(t * 17.0 + 1.0) * smooth(0.15, 0.35, t))
    hw = hw + np.where(right, 0.09, 0.06) * np.exp(-((t - np.where(right, 0.62, 0.48)) / 0.07) ** 2)
    hw = np.minimum(hw, 0.42)
    u = np.abs(x - 0.5 - sway)
    outer = 1 - smooth(hw - 0.02, hw + 0.02, u)
    ihw = base * 0.5 * smooth(0.02, 0.12, t) * (1 - smooth(0.5, 0.75, t))
    inner = 1 - smooth(ihw - 0.02, ihw + 0.02, u)
    inner *= ihw > 0.01
    a = np.clip(outer * 0.8 + inner * 0.2, 0, 1) * (1 - smooth(0.96, 1.0, t))
    grey = 0.7 + 0.3 * inner
    finish("flame", a, np.clip(grey, 0, 1))


def feather():
    im, d, W, H = canvas("feather")
    # a black feather (Black Butler): a long vane with slits along both sides and a quill; drawn grey-white, tinted ink by the layer
    vane = []
    for k in range(21):
        t = k / 20
        vane.append((0.5 + 0.36 * math.sin(t * math.pi) ** 0.8 * (1 - 0.3 * t), 0.06 + 0.78 * t))
    for k in range(20, -1, -1):
        t = k / 20
        vane.append((0.5 - 0.30 * math.sin(t * math.pi) ** 0.8 * (1 - 0.3 * t), 0.06 + 0.78 * t))
    d.polygon([(px * W, py * H) for px, py in vane], fill=235)
    for k in range(6):
        yy = 0.25 + 0.1 * k
        d.line([(0.5 * W, yy * H), (0.95 * W, (yy - 0.06) * H)], fill=0, width=max(1, int(0.05 * W)))
        d.line([(0.5 * W, (yy + 0.04) * H), (0.05 * W, (yy - 0.03) * H)], fill=0, width=max(1, int(0.05 * W)))
    d.line([(0.5 * W, 0.04 * H), (0.5 * W, 0.97 * H)], fill=255, width=max(1, int(0.07 * W)))
    finish("feather", arr(im, 0.25))


def pentagram():
    im, d, W, H = canvas("pentagram")
    # the contract seal of Black Butler: a double circle, a five-pointed star, small marks between the rings
    lw = int(0.022 * W)
    for rr in (0.46, 0.40):
        d.ellipse([(0.5 - rr) * W, (0.5 - rr) * H, (0.5 + rr) * W, (0.5 + rr) * H], outline=255, width=lw)
    pts = [(0.5 + 0.40 * math.cos(-math.pi / 2 + k * 4 * math.pi / 5), 0.5 + 0.40 * math.sin(-math.pi / 2 + k * 4 * math.pi / 5)) for k in range(6)]
    d.line([(px * W, py * H) for px, py in pts], fill=255, width=lw, joint="curve")
    for k in range(10):
        ang = k * math.pi / 5 + 0.3
        cx, cy = 0.5 + 0.43 * math.cos(ang), 0.5 + 0.43 * math.sin(ang)
        d.ellipse([(cx - 0.012) * W, (cy - 0.012) * H, (cx + 0.012) * W, (cy + 0.012) * H], fill=255)
    rr = 0.16
    d.ellipse([(0.5 - rr) * W, (0.5 - rr) * H, (0.5 + rr) * W, (0.5 + rr) * H], outline=200, width=int(lw * 0.7))
    a = arr(im, 0.3)
    x, y = grid(*SIZES["pentagram"])
    r, _ = polar(x, y)
    finish("pentagram", np.clip(a * (1 - smooth(0.96, 0.995, r)), 0, 1))


def crack():
    im, d, W, H = canvas("crack")
    rng = np.random.RandomState(515)
    # a crack in space (the Black Clover devil's sigil breaking through): jagged lines out from a bright centre, with forks
    def branch(x, y, ang, length, width, depth):
        pts = [(x, y)]
        steps = 6
        for _ in range(steps):
            ang += rng.uniform(-0.45, 0.45)
            x += math.cos(ang) * length / steps
            y += math.sin(ang) * length / steps
            pts.append((x, y))
        d.line([(px * W, py * H) for px, py in pts], fill=255, width=max(2, int(width * W)), joint="curve")
        if depth > 0:
            k = rng.randint(2, 5)
            branch(pts[k][0], pts[k][1], ang + rng.choice([-1, 1]) * rng.uniform(0.5, 0.9), length * 0.5, width * 0.6, depth - 1)
    n = 8
    for i in range(n):
        ang = i / n * 2 * math.pi + rng.uniform(-0.2, 0.2)
        branch(0.5, 0.5, ang, rng.uniform(0.28, 0.42), 0.022, 1)
    d.ellipse([0.43 * W, 0.43 * H, 0.57 * W, 0.57 * H], fill=255)
    a = arr(im, 0.35)
    x, y = grid(*SIZES["crack"])
    r, _ = polar(x, y)
    finish("crack", np.clip(a * (1 - smooth(0.9, 0.99, r)), 0, 1))


def runes():
    im, d, W, H = canvas("runes")
    rng = np.random.RandomState(616)
    # a band of curse glyphs (Fairy Tail's Etherious curses), font-free: each glyph is 3-5 strokes between the nodes of a 3x4 grid
    n = 12
    lw = max(2, int(0.022 * H * 3))
    for i in range(n):
        x0 = (i + 0.18) / n
        cw = 0.64 / n
        nodes = [(x0 + cw * c / 2, 0.18 + 0.64 * r / 3) for r in range(4) for c in range(3)]
        cur = nodes[rng.randint(0, 12)]
        for _ in range(rng.randint(3, 6)):
            nxt = nodes[rng.randint(0, 12)]
            if nxt == cur:
                continue
            d.line([(cur[0] * W, cur[1] * H), (nxt[0] * W, nxt[1] * H)], fill=255, width=lw)
            cur = nxt
        d.ellipse([(x0 + cw / 2 - 0.004) * W, 0.86 * H, (x0 + cw / 2 + 0.004) * W, 0.92 * H], fill=220)
    a = arr(im, 0.2)
    x, y = grid(*SIZES["runes"])
    finish("runes", np.clip(a * smooth(0.0, 0.08, y) * (1 - smooth(0.92, 1.0, y)), 0, 1))


def brand():
    im, d, W, H = canvas("brand")
    # the Brand of Sacrifice (Berserk): a long vertical stroke, two arms swept up from its middle like a Y, and a tail that hooks round at the foot
    lw = int(0.09 * W)
    d.line([(0.5 * W, 0.08 * H), (0.5 * W, 0.78 * H)], fill=255, width=lw)
    for s in (-1, 1):
        pts = [(0.5, 0.48), (0.5 + s * 0.14, 0.36), (0.5 + s * 0.26, 0.2), (0.5 + s * 0.3, 0.1)]
        d.line([(px * W, py * H) for px, py in pts], fill=255, width=int(lw * 0.8), joint="curve")
    tail = [(0.5, 0.76), (0.42, 0.86), (0.3, 0.88), (0.24, 0.82), (0.32, 0.76)]
    d.line([(px * W, py * H) for px, py in tail], fill=255, width=int(lw * 0.75), joint="curve")
    d.polygon([(0.5 * W, 0.02 * H), (0.56 * W, 0.1 * H), (0.44 * W, 0.1 * H)], fill=255)
    finish("brand", arr(im, 0.5))


def blood():
    im, d, W, H = canvas("blood")
    rng = np.random.RandomState(717)
    # a splash of blood: one fat drop and a few satellite droplets
    d.ellipse([0.28 * W, 0.28 * H, 0.66 * W, 0.66 * H], fill=255)
    d.polygon([(0.62 * W, 0.34 * H), (0.86 * W, 0.18 * H), (0.6 * W, 0.5 * H)], fill=255)
    for _ in range(5):
        cx, cy, rr = rng.uniform(0.2, 0.8), rng.uniform(0.2, 0.8), rng.uniform(0.03, 0.07)
        d.ellipse([(cx - rr) * W, (cy - rr) * H, (cx + rr) * W, (cy + rr) * H], fill=255)
    a = arr(im, 0.3)
    x, y = grid(*SIZES["blood"])
    r, _ = polar(x, y)
    grey = 0.8 + 0.2 * (1 - y)
    finish("blood", np.clip(a * (1 - smooth(0.9, 0.99, r)), 0, 1), grey)


def flash():
    x, y = grid(*SIZES["flash"])
    r, th = polar(x, y)
    n = 12
    lens = 0.5 + 0.45 * RNG.uniform(0, 1, n)
    k = (th + math.pi) / (2 * math.pi) * n
    i = np.floor(k).astype(int) % n
    f = k - np.floor(k)
    spike = lens[i] * (1 - np.abs(f - 0.5) * 2) ** 2.0
    a = np.maximum((r < spike).astype(np.float64), cel(r, [(0.2, 1.0), (0.32, 0.5)]))
    a *= 1 - smooth(0.95, 1.0, r)
    finish("flash", a)


MAKERS = {"core": core, "glow": glow, "void": void, "slash": slash, "bolt": bolt, "streak": streak, "ring": ring, "scales": scales, "scale": scale,
          "teeth": teeth, "glint": glint, "flame": flame, "feather": feather, "pentagram": pentagram, "crack": crack, "runes": runes, "brand": brand,
          "blood": blood, "flash": flash}

if __name__ == "__main__":
    assert set(MAKERS) == set(SIZES)
    for name in SIZES:
        MAKERS[name]()
