"""Hand-tuned, deterministic painters of the Spirit of Darkness (docs/magic/spirit_dark.md). Every function returns (colour RGBA, glow RGBA) float arrays of its size, the pair the atlas of
spirit_dark_geo_lib stores. The look is CEL SHADED: flat bands of dark violet and black with hard steps, a dark ink line round every region, a thin violet rim light on the lit edge, glowing parts
(eyes, runes, vein seams) only in the glow array (drawn additive and full bright by the game), and smoke drawn as banded soft shapes, never a flat white cut-out.

Palette: ink #0b0713, black-violet coat #1e1430, dark violet #2f1d4f, violet #4c2f7a, rim #8a63d8, dusky skin #6b5a8c, glow violet #b05cff, glow crimson #ff3a6e.
Everything is seeded (numpy default_rng) so a re-run writes the same pixels.
"""
import math

import numpy as np

INK = np.array([11, 7, 19], dtype=np.float32)
COAT = np.array([52, 36, 82], dtype=np.float32)
VIOLET_DK = np.array([72, 46, 118], dtype=np.float32)
VIOLET = np.array([112, 74, 178], dtype=np.float32)
RIM = np.array([184, 142, 255], dtype=np.float32)
SKIN = np.array([150, 128, 184], dtype=np.float32)
HORN = np.array([58, 46, 84], dtype=np.float32)
GLOW_V = np.array([176, 92, 255], dtype=np.float32)
GLOW_R = np.array([255, 58, 110], dtype=np.float32)

SHADE = {"up": 1.28, "front": 1.0, "side": 0.86, "back": 0.78, "down": 0.55}


def grid(w, h):
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    return x, y


def smooth(t):
    t = np.clip(t, 0.0, 1.0)
    return t * t * (3 - 2 * t)


def blank(w, h):
    return np.zeros((h, w, 4), np.float32), np.zeros((h, w, 4), np.float32)


def fill(c, rgb, alpha=255.0):
    c[..., :3] = rgb
    c[..., 3] = alpha


def band(c, mask, rgb):
    c[mask, :3] = rgb
    c[mask, 3] = 255.0


def edge_dist(w, h):
    x, y = grid(w, h)
    return np.minimum(np.minimum(x, w - 1 - x), np.minimum(y, h - 1 - y))


def outline(c, w, h, px=2, rim_side="tl", rim=RIM, rim_k=0.55):
    """A dark ink line round the region and a thin lit rim just inside it (top and left), the cel linework."""
    x, y = grid(w, h)
    d = edge_dist(w, h)
    ink = d < px
    rimmask = (d >= px) & (d < px + 1) & (((y < h / 2) | (x < w / 2)) if rim_side == "tl" else True)
    c[rimmask, :3] = c[rimmask, :3] * (1 - rim_k) + rim * rim_k
    c[ink, :3] = INK
    c[ink, 3] = 255.0


def cel(w, h, base, face="front", seed=0, steps=3, bands=(0.3, 0.74), hatch=0.0, px=2):
    """A flat cel-shaded tile: base colour times the face's light, a hard light band at the top and a hard shadow band at the bottom, ink outline, optional diagonal hatch in the shadow."""
    rng = np.random.default_rng(seed)
    c, g = blank(w, h)
    k = SHADE[face]
    fill(c, np.clip(base * k, 0, 255))
    x, y = grid(w, h)
    t = y / max(1, h - 1)
    band(c, t < bands[0], np.clip(base * k * 1.3 + 6, 0, 255))
    band(c, t > bands[1], np.clip(base * k * 0.62, 0, 255))
    if hatch > 0:
        hm = (t > bands[1]) & (((x + y) % 5) < 1)
        c[hm, :3] = np.clip(c[hm, :3] * (1 - hatch), 0, 255)
    outline(c, w, h, px)
    return c, g


def vfolds(c, w, h, seed, n=3, strength=0.35, px=2):
    """Dark vertical fold lines (cloth) with a lit line beside each."""
    rng = np.random.default_rng(seed)
    x, y = grid(w, h)
    for i in range(n):
        fx = int(rng.integers(px + 6, max(px + 7, w - px - 6)))
        wob = (np.sin(y * 0.18 + i * 2.1) * 1.4)
        d = np.abs(x - fx - wob)
        m = (d < 1.2) & (y > px + 2) & (y < h - px - 3 - rng.integers(0, h // 5))
        c[m, :3] = c[m, :3] * (1 - strength * 1.6)
        m2 = (np.abs(x - fx - wob - 2.4) < 0.8) & (y > px + 2) & (y < h - px - 6)
        c[m2, :3] = np.clip(c[m2, :3] * (1 + strength * 0.9) + 4, 0, 255)


def strip(c, y0, y1, rgb, w, px=2):
    c[y0:y1, px:w - px, :3] = rgb
    c[y0:y1, px:w - px, 3] = 255.0


# ======================================================================================================================= materials (one tile per face kind)
def mat(name, w, h, face, seed):
    """The tile of a material for a face kind (up, front, side, back, down)."""
    if name == "coat":
        c, g = cel(w, h, COAT, face, seed, hatch=0.3)
        vfolds(c, w, h, seed + 5, n=3)
        outline(c, w, h)
        return c, g
    if name == "hem":
        c, g = cel(w, h, COAT, face, seed, hatch=0.3)
        vfolds(c, w, h, seed + 7, n=2)
        strip(c, h - 10, h - 6, np.clip(VIOLET * SHADE[face], 0, 255), w)
        strip(c, h - 6, h - 4, INK, w)
        outline(c, w, h)
        return c, g
    if name == "skin":
        return cel(w, h, SKIN, face, seed, hatch=0.25)
    if name == "horn":
        c, g = cel(w, h, HORN, face, seed, bands=(0.2, 0.8))
        x, y = grid(w, h)
        for yy in range(6, h - 3, 7):
            m = (np.abs(y - yy) < 0.9) & (x > 2) & (x < w - 3)
            c[m, :3] = INK * 1.2
        hl = (np.abs(x - w * 0.3) < 1.6) & (y > 3) & (y < h - 3)
        c[hl, :3] = np.clip(c[hl, :3] + 38, 0, 255)
        return c, g
    if name == "boot":
        c, g = cel(w, h, np.array([40, 28, 62], np.float32), face, seed, bands=(0.25, 0.8))
        return c, g
    if name == "violet":
        return cel(w, h, VIOLET_DK, face, seed, hatch=0.3)
    if name == "bone":
        c, g = cel(w, h, np.array([62, 42, 98], np.float32), face, seed, bands=(0.22, 0.78))
        return c, g
    if name == "lace":
        c, g = cel(w, h, np.array([96, 78, 138], np.float32), face, seed, bands=(0.28, 0.7))
        x, y = grid(w, h)
        for xx in range(4, w - 3, 6):
            m = (np.abs(x - xx) < 0.8) & (y > 3) & (y < h - 3)
            c[m, :3] = c[m, :3] * 0.6
        return c, g
    if name == "ink":
        c, g = blank(w, h)
        fill(c, INK * 1.4)
        return c, g
    raise KeyError(name)


# ======================================================================================================================= special regions
def face_upper(w, h, seed=1):
    """The powered form's upper face: dusky skin, thin slanted brows, long narrow eye sockets (the glow draws the burning eyes), a shadow under the brow; glow array: the eyes."""
    c, g = cel(w, h, SKIN, "front", seed, bands=(0.14, 0.86), hatch=0.0)
    x, y = grid(w, h)
    u, v = x / (w - 1), y / (h - 1)
    for s in (-1, 1):
        cx = 0.5 + s * 0.24
        slope = s * (u - cx) * -0.34
        brow = np.abs(v - (0.27 + slope)) < 0.05
        brow &= np.abs(u - cx) < 0.19
        c[brow, :3] = INK
        shade = (v > 0.27 + slope + 0.04) & (v < 0.34 + slope) & (np.abs(u - cx) < 0.19)
        c[shade, :3] = c[shade, :3] * 0.7
        ex = (u - cx) / 0.17
        ey = (v - (0.56 + slope * 0.7)) / 0.085
        sock = ex * ex + ey * ey < 1.0
        c[sock, :3] = INK * 1.1
        core = (ex * ex + ey * ey * 1.7) < 0.66
        g[core, :3] = GLOW_R
        g[core, 3] = 255.0
        hot = (ex * ex + ey * ey * 3.2) < 0.2
        g[hot, :3] = np.array([255, 200, 220], np.float32)
        halo = (ex * ex + ey * ey < 2.8) & ~sock
        a = smooth(1 - (ex * ex + ey * ey) / 2.8) * 80
        g[halo, :3] = GLOW_V
        g[halo, 3] = a[halo]
    nose = (np.abs(u - 0.5) < 0.014) & (v > 0.5)
    c[nose, :3] = c[nose, :3] * 0.6
    outline(c, w, h, 2)
    return c, g


def face_lower(w, h, seed=11):
    """The lower face: the nose tip, a long thin smirk with a glowing seam and two fangs, a pointed chin."""
    c, g = cel(w, h, SKIN, "front", seed, bands=(0.1, 0.9), hatch=0.0)
    x, y = grid(w, h)
    u, v = x / (w - 1), y / (h - 1)
    nose = ((u - 0.5) / 0.1) ** 2 + ((v - 0.12) / 0.16) ** 2 < 1.0
    c[nose, :3] = c[nose, :3] * 0.75
    gy = 0.56 + 0.07 * (u - 0.5) * 2.0 - 0.06 * np.cos((u - 0.5) * math.pi * 2.0)
    line = (np.abs(v - gy) < 0.045) & (np.abs(u - 0.5) < 0.40)
    c[line, :3] = INK
    seam = (np.abs(v - gy) < 0.016) & (np.abs(u - 0.5) < 0.36)
    g[seam, :3] = GLOW_V
    g[seam, 3] = 210.0
    for fx in (-0.2, 0.2):
        fang = (np.abs(u - 0.5 - fx) < 0.03) & (v > gy) & (v < gy + 0.2 - abs(u - 0.5 - fx) * 4)
        c[fang, :3] = np.array([226, 212, 240], np.float32)
        c[fang, 3] = 255.0
    chin = (np.abs(u - 0.5) < 0.012) & (v > 0.78)
    c[chin, :3] = c[chin, :3] * 0.7
    outline(c, w, h, 2)
    return c, g


def head_side(w, h, seed=2, mirror=False):
    c, g = cel(w, h, SKIN, "side", seed, bands=(0.2, 0.8))
    x, y = grid(w, h)
    u, v = x / (w - 1), y / (h - 1)
    # a dark ear hollow and a cheek line
    ear = ((u - 0.62) / 0.12) ** 2 + ((v - 0.45) / 0.14) ** 2 < 1.0
    c[ear, :3] = c[ear, :3] * 0.5
    cheek = (np.abs(v - (0.62 + (u - 0.5) * 0.4)) < 0.02) & (u < 0.5)
    c[cheek, :3] = c[cheek, :3] * 0.6
    if mirror:
        c = c[:, ::-1].copy()
        g = g[:, ::-1].copy()
    outline(c, w, h, 2)
    return c, g


def hair_top(w, h, seed=3):
    c, g = cel(w, h, np.array([20, 14, 30], np.float32), "up", seed, bands=(0.2, 0.8))
    x, y = grid(w, h)
    for i in range(5):
        m = (np.abs(x - (6 + i * (w - 12) / 4.0) - (y * 0.15)) < 0.9) & (y > 3) & (y < h - 3)
        c[m, :3] = np.clip(c[m, :3] + 20, 0, 255)
    return c, g


def chest_front(w, h, seed=4):
    """The coat's chest front: black-violet cloth, a violet lapel edge, two buttons and a glowing sigil on the left breast (glow array)."""
    c, g = cel(w, h, COAT, "front", seed, bands=(0.25, 0.8), hatch=0.3)
    x, y = grid(w, h)
    u, v = x / (w - 1), y / (h - 1)
    lap = (np.abs(u - 0.5) < 0.035) & (v > 0.05)
    c[lap, :3] = INK
    for s in (-1, 1):
        edge = (np.abs((u - 0.5) - s * (0.12 + 0.18 * v)) < 0.016) & (v > 0.04) & (v < 0.94)
        c[edge, :3] = np.clip(VIOLET * 1.1, 0, 255)
    for by in (0.55, 0.78):
        bm = ((u - 0.43) ** 2 + ((v - by) * (h / w)) ** 2) < 0.0016
        c[bm, :3] = RIM
        bm2 = ((u - 0.43) ** 2 + ((v - by) * (h / w)) ** 2) < 0.0006
        c[bm2, :3] = INK
    # sigil: a ring with an inverted triangle, glowing
    sx, sy, r = 0.72, 0.38, 0.13
    ring = np.abs(np.sqrt((u - sx) ** 2 + ((v - sy) * (h / w)) ** 2) - r) < 0.012
    tri = np.zeros_like(ring)
    for ang in (90, 210, 330):
        a0, a1 = math.radians(ang), math.radians(ang + 120)
        p0 = (sx + r * 0.95 * math.cos(a0), sy + r * 0.95 * math.sin(a0) * (w / h))
        p1 = (sx + r * 0.95 * math.cos(a1), sy + r * 0.95 * math.sin(a1) * (w / h))
        for t in np.linspace(0, 1, 40):
            px_, py_ = p0[0] + (p1[0] - p0[0]) * t, p0[1] + (p1[1] - p0[1]) * t
            tri |= ((u - px_) ** 2 + ((v - py_) * (h / w)) ** 2) < 0.00005
    sig = ring | tri
    g[sig, :3] = GLOW_V
    g[sig, 3] = 255.0
    c[sig, :3] = RIM
    outline(c, w, h, 2)
    return c, g


def back_coat(w, h, seed=5):
    c, g = mat("hem", w, h, "back", seed)
    x, y = grid(w, h)
    u = x / (w - 1)
    split = (np.abs(u - 0.5) < 0.02) & (y > 4)
    c[split, :3] = INK
    return c, g


def jabot(w, h, seed=6):
    """A lace cravat: pale violet frills in three hard-edged scallops."""
    c, g = cel(w, h, np.array([96, 78, 138], np.float32), "front", seed, bands=(0.28, 0.7))
    x, y = grid(w, h)
    u, v = x / (w - 1), y / (h - 1)
    for k, vy in enumerate((0.3, 0.55, 0.8)):
        m = np.abs(v - (vy + 0.04 * np.cos(u * math.pi * 5))) < 0.02
        c[m, :3] = np.array([52, 38, 86], np.float32)
    outline(c, w, h, 2)
    return c, g


def flat_glow(w, h, rgb, a=255.0):
    c, g = blank(w, h)
    c[..., :3] = INK
    c[..., 3] = 255.0
    g[..., :3] = rgb
    g[..., 3] = a
    return c, g


def rune_arm(w, h, seed=7):
    """A cuff with a glowing violet band and a row of small rune ticks."""
    c, g = mat("violet", w, h, "front", seed)
    x, y = grid(w, h)
    u, v = x / (w - 1), y / (h - 1)
    band_ = (np.abs(v - 0.5) < 0.12)
    c[band_, :3] = INK
    ticks = band_ & ((np.floor(u * 7) % 2) < 1) & (np.abs(v - 0.5) < 0.07)
    g[ticks, :3] = GLOW_V
    g[ticks, 3] = 255.0
    c[ticks, :3] = VIOLET
    outline(c, w, h, 2)
    return c, g


# ======================================================================================================================= wings
def membrane(w, h, tri, seed=8, mirror=False, glow_edge=True):
    """A bat-wing membrane card: only the triangle `tri` (three (u, v) points in 0..1; the first is the wrist apex) is opaque, its far edge scalloped between the two finger tips; dark violet in hard
    bands that run from the apex, ink veins, a lit rim on the leading edge, a glowing seam on the finger edges (glow array). The rest of the card is transparent (alpha tested out)."""
    c, g = blank(w, h)
    x, y = grid(w, h)
    u, v = (x + 0.5) / w, (y + 0.5) / h
    (ax, ay), (bx, by), (cx, cy) = tri

    def edge(p0, p1, px, py):
        return (p1[0] - p0[0]) * (py - p0[1]) - (p1[1] - p0[1]) * (px - p0[0])

    e0 = edge((ax, ay), (bx, by), u, v)
    e1 = edge((bx, by), (cx, cy), u, v)
    e2 = edge((cx, cy), (ax, ay), u, v)
    sign = 1.0 if edge((ax, ay), (bx, by), cx, cy) > 0 else -1.0
    inside0, inside1, inside2 = e0 * sign, e1 * sign, e2 * sign
    # the far edge (b - c) is scalloped inwards
    flen = math.hypot(cx - bx, cy - by) + 1e-6
    tparam = ((u - bx) * (cx - bx) + (v - by) * (cy - by)) / (flen * flen)
    scallop = 0.045 * np.abs(np.sin(np.clip(tparam, 0, 1) * math.pi * 3.0))
    inside1 = inside1 / flen - scallop
    inside0 = inside0 / (math.hypot(bx - ax, by - ay) + 1e-6)
    inside2 = inside2 / (math.hypot(ax - cx, ay - cy) + 1e-6)
    inside = (inside0 > 0) & (inside1 > 0) & (inside2 > 0)
    # distance from apex drives the cel bands
    dist = np.sqrt((u - ax) ** 2 + (v - ay) ** 2)
    dmax = max(math.hypot(bx - ax, by - ay), math.hypot(cx - ax, cy - ay))
    t = dist / dmax
    base = np.where(t[..., None] < 0.45, VIOLET_DK, np.where(t[..., None] < 0.78, np.array([60, 36, 104], np.float32), np.array([42, 24, 76], np.float32)))
    c[..., :3] = base
    c[..., 3] = np.where(inside, 255.0, 0.0)
    # veins radiating from the apex between the two edges
    ang_b, ang_c = math.atan2(by - ay, bx - ax), math.atan2(cy - ay, cx - ax)
    ang = np.arctan2(v - ay, u - ax)
    da = (ang_c - ang_b + math.pi) % (2 * math.pi) - math.pi
    for k in (0.35, 0.68):
        target = ang_b + da * k
        dd = np.abs(((ang - target + math.pi) % (2 * math.pi)) - math.pi) * dist
        m = inside & (dd < 0.012)
        c[m, :3] = INK
    # the edge rows: a lit rim along the two finger edges, ink border everywhere else
    border = inside & ((inside0 < 0.02) | (inside1 < 0.015) | (inside2 < 0.02))
    c[border, :3] = INK
    seam = inside & (((inside0 > 0.02) & (inside0 < 0.05)) | ((inside2 > 0.02) & (inside2 < 0.05)))
    c[seam, :3] = RIM
    if glow_edge:
        g[seam, :3] = GLOW_V
        g[seam, 3] = 215.0
        for k in (0.35, 0.68):
            target = ang_b + da * k
            dd = np.abs(((ang - target + math.pi) % (2 * math.pi)) - math.pi) * dist
            m = inside & (dd < 0.011) & (t > 0.12)
            g[m, :3] = GLOW_V
            g[m, 3] = 170.0
    if mirror:
        c = c[:, ::-1].copy()
        g = g[:, ::-1].copy()
    c[c[..., 3] == 0, :3] = 0
    return c, g


# ======================================================================================================================= smoke (companion)
def fbm(rng, w, h, base=4, octaves=3):
    out = np.zeros((h, w), np.float32)
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        n = base * (2 ** o)
        gr = rng.random((n + 2, n + 2)).astype(np.float32)
        ys = np.linspace(0, n, h, endpoint=False)
        xs = np.linspace(0, n, w, endpoint=False)
        y0 = ys.astype(int)[:, None]
        x0 = xs.astype(int)[None, :]
        fy = smooth((ys - y0[:, 0])[:, None])
        fx = smooth((xs - x0[0, :])[None, :])
        a = gr[y0, x0] * (1 - fx) + gr[y0, x0 + 1] * fx
        b = gr[y0 + 1, x0] * (1 - fx) + gr[y0 + 1, x0 + 1] * fx
        out += (a * (1 - fy) + b * fy) * amp
        tot += amp
        amp *= 0.5
    return out / tot


def smoke(w, h, face="front", seed=11, tint=0.0):
    """A banded smoke tile: noise posterised into four flat tones from black to violet, an ink outline, a lit rim; the cel look of a shadow."""
    rng = np.random.default_rng(seed)
    c, g = blank(w, h)
    n = fbm(rng, w, h, 3, 3)
    x, y = grid(w, h)
    t = y / max(1, h - 1)
    v = np.clip(n * 0.9 + (1 - t) * 0.25 + (SHADE[face] - 0.8) * 0.4, 0, 1)
    tones = [np.array([26, 17, 44], np.float32), np.array([48, 31, 82], np.float32), np.array([80, 52, 134], np.float32), np.array([122, 86, 188], np.float32)]
    idx = np.clip((v * 4.0).astype(int), 0, 3)
    for i, tn in enumerate(tones):
        m = idx == i
        c[m, :3] = tn * (1.0 + tint)
    c[..., 3] = 255.0
    outline(c, w, h, 2, rim_k=0.4)
    return c, g


def puff_glow(w, h, rgb=GLOW_V, peak=130.0, seed=0):
    """A soft additive blob (a smoke wisp): a smooth radial falloff with a little noise, alpha 0 at the border, never a flat edge."""
    rng = np.random.default_rng(seed)
    c, g = blank(w, h)
    x, y = grid(w, h)
    u, v = (x + 0.5) / w * 2 - 1, (y + 0.5) / h * 2 - 1
    r = np.sqrt(u * u + v * v)
    n = fbm(rng, w, h, 3, 3)
    a = smooth(1.0 - r) ** 1.6 * (0.55 + 0.9 * n)
    a = np.clip(a, 0, 1)
    g[..., :3] = rgb
    g[..., 3] = a * peak
    g[g[..., 3] < 1.0] = 0
    return c, g


def tendril_glow(w, h, rgb=GLOW_V, peak=150.0):
    """A soft vertical wisp: bright at the top, fading down and out, alpha 0 at every border."""
    c, g = blank(w, h)
    x, y = grid(w, h)
    u, v = (x + 0.5) / w * 2 - 1, (y + 0.5) / h
    a = smooth(1 - np.abs(u)) ** 1.5 * smooth(1 - v) ** 1.2 * smooth(v * 6)
    g[..., :3] = rgb
    g[..., 3] = a * peak
    g[g[..., 3] < 1.0] = 0
    return c, g


def eye_glow(w, h, rgb=GLOW_V, hot=(255, 220, 255), slit=True):
    """A glowing slit eye card: a hot core and a coloured halo, soft edge (alpha 0 at the border)."""
    c, g = blank(w, h)
    x, y = grid(w, h)
    u, v = (x + 0.5) / w * 2 - 1, (y + 0.5) / h * 2 - 1
    r = (u / 1.0) ** 2 + (v / (0.55 if slit else 0.9)) ** 2
    halo = smooth(1.0 - r / 1.0) * 0.8
    core = smooth(1.0 - r / 0.42)
    hotc = smooth(1.0 - r / 0.14)
    a = np.clip(halo * 0.6 + core, 0, 1)
    col = np.zeros((h, w, 3), np.float32)
    col[:] = rgb
    col = col * (1 - hotc[..., None] * 0.7) + np.array(hot, np.float32) * hotc[..., None] * 0.7
    g[..., :3] = col
    g[..., 3] = a * 255.0
    g[g[..., 3] < 1.0] = 0
    return c, g
