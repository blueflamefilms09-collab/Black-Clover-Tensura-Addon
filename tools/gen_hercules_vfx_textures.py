"""Generates the Veldanava "Hercules, Lord of Martial Prowess" VFX textures (vfx.client.layer.HerculesLayer).

Deterministic and font-free (every machine writes the same pixels). Written from scratch for this mod; shares no code or asset with any other mod.
Output: src/main/resources/assets/nusmp/textures/particle/hercules_*.png

    python tools/gen_hercules_vfx_textures.py             # all
    python tools/gen_hercules_vfx_textures.py fist leaf   # only these

Identity: HEAVY AND PHYSICAL. Burnished bronze, bright gold, warm ivory / marble white, ember orange and a little deep crimson; white-gold flashes. Motifs: the clenched FIST
and the LAUREL wreath (the owner-approved icon), cracked ground that glows from inside, shock rings, slabs and chips of rock, dust, speed lines, a heavy chain of bronze links,
a web of cracks in the air like broken glass, overlapping bronze plates. Nothing here is a sparkle, a rune or a feather: it deliberately shares nothing with Sandalphon (cold white
and red judgement), Israfil (violet labyrinth), Jibril (white / blue wings, feathers) or Solomon (indigo / gold runes and hexagrams), and never touches the Time Magic textures.

Several textures carry their light baked in (the fist, the hand, the laurel, the plates, the chain, the chips are lit from the upper left like metal and stone); the shock ring, wave,
streak, flash, flame and mote are light shapes the layer tints. Nothing carries colour under a fully transparent pixel. Sizes are powers of two, at most 512 px, aspect at most 8:1.

    hercules_fist     a clenched fist (knuckles up) in lit bronze with a gold rim, over a forearm cuff                       256
    hercules_hand     the open hand that closes: five spread fingers and a thumb in lit bronze                               256
    hercules_laurel   a laurel wreath: two branches of leaves and berries that meet in a crimson ribbon at the foot           512
    hercules_leaf     one golden laurel leaf, pointing up-right (it is rotated in the layer)                                 128
    hercules_ring     a thick, ragged shock ring (bright band at 0.93 of the half size, a trail behind it)                    512
    hercules_wave     a shock-front disc: a hard edge at 0.95 and a streaked gradient behind it (light and dust)             512
    hercules_cracks   cracks that radiate over the ground, dark outside with a glowing ember / gold core (alpha-blended)     512
    hercules_web      a web of cracks in the air like broken glass: bright lines, shards that are void (alpha-blended)       512
    hercules_dust     four dust puffs (2 x 2), warm tan, lit from above (alpha-blended)                                      256
    hercules_chips    top row: four chips / slabs of rock with a hot rim; bottom row: four shards of cracked space (4 x 2)    512 x 256
    hercules_streak   a speed line: a tapered streak, faint at V = 0 and a bright blunt head at V = 1                         64 x 512
    hercules_chain    a heavy chain of bronze links, face-on and edge-on, seamless in V                                      128 x 512
    hercules_flash    an impact frame: a jagged white-gold burst with long cross spikes                                       256
    hercules_mote     a small ember (tinted)                                                                                  64
    hercules_plates   overlapping bronze plates in a body-shaped capsule with gold rims                                      256 x 512
    hercules_flame    a cluster of licking flame tongues, white-gold core, orange and crimson tips (V = 0 tip, V = 1 foot)  128 x 256
    hercules_crater   a crushed crater scar with a broken rim and spatter (alpha-blended)                                    512
"""
import math
import os
import random
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")

SS = 3   # supersampling of the line art

BRONZE_D = (74, 36, 14)       # the deepest shadow of the bronze
BRONZE = (176, 104, 40)       # burnished bronze
BRONZE_L = (222, 148, 62)
GOLD = (255, 214, 96)         # bright gold
GOLD_L = (255, 238, 170)
IVORY = (255, 246, 222)       # warm ivory / marble white
EMBER = (255, 122, 42)        # ember orange
CRIMSON = (150, 30, 36)       # a little deep crimson
WHITE = (255, 255, 255)
STONE = (128, 112, 94)
STONE_D = (74, 62, 52)


# ---------------------------------------------------------------------------------------------------- io
def save(rgb, alpha, name):
    """Writes an RGBA png from float arrays (rgb 0..1, alpha 0..1). rgb is zeroed under a fully transparent pixel."""
    a = np.clip(alpha, 0, 1)
    rgb = np.clip(rgb, 0, 1)
    px = np.dstack([rgb[..., 0], rgb[..., 1], rgb[..., 2], a])
    px = (px * 255 + 0.5).astype(np.uint8)
    px[px[..., 3] == 0, :3] = 0
    im = Image.fromarray(px, "RGBA")
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, name + ".png"), optimize=True)
    print("wrote", name, im.size)


def col(c):
    return np.array(c, np.float32) / 255.0


def blur(a, radius):
    return np.asarray(Image.fromarray((np.clip(a, 0, 1) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(radius)), dtype=np.float32) / 255.0


def blurf(a, radius):
    """Gaussian blur of an unclipped float array (heights): through a 16-bit round trip would clip, so use a separable kernel."""
    r = max(0.5, float(radius))
    n = int(math.ceil(r * 3))
    k = np.exp(-0.5 * (np.arange(-n, n + 1) / r) ** 2)
    k /= k.sum()
    p = np.pad(a, ((0, 0), (n, n)), mode="edge")
    t = sum(k[i] * p[:, i:i + a.shape[1]] for i in range(2 * n + 1))
    p = np.pad(t, ((n, n), (0, 0)), mode="edge")
    return sum(k[i] * p[i:i + a.shape[0], :] for i in range(2 * n + 1))


def smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


def grid(W, H):
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    return xx + 0.5, yy + 0.5


def vnoise(W, H, cell, seed):
    """Smooth value noise in 0..1 (bicubic upsampled random grid)."""
    rs = np.random.RandomState(seed)
    g = rs.rand(H // cell + 3, W // cell + 3).astype(np.float32)
    im = Image.fromarray((g * 255).astype(np.uint8)).resize(((W // cell + 3) * cell, (H // cell + 3) * cell), Image.BICUBIC)
    a = np.asarray(im, dtype=np.float32) / 255.0
    return a[cell:cell + H, cell:cell + W]


def fbm(W, H, seed, cells=(64, 32, 16, 8), gains=(0.5, 0.3, 0.14, 0.06)):
    out = np.zeros((H, W), np.float32)
    for i, (c, g) in enumerate(zip(cells, gains)):
        out += vnoise(W, H, c, seed + i * 31) * g
    return out / sum(gains)


# ---------------------------------------------------------------------------------------------------- line art
class Mask:
    """A mask drawn at SS x the final size; all coordinates are in final pixels."""

    def __init__(self, W, H):
        self.W, self.H = W, H
        self.im = Image.new("L", (W * SS, H * SS), 0)
        self.d = ImageDraw.Draw(self.im)

    def final(self):
        return np.asarray(self.im.resize((self.W, self.H), Image.LANCZOS), dtype=np.float32).clip(0, 255) / 255.0

    def _w(self, w):
        return max(1, int(round(w * SS)))

    def line(self, pts, w=1.5, v=255, round_ends=True):
        p = [(x * SS, y * SS) for x, y in pts]
        ww = self._w(w)
        self.d.line(p, fill=v, width=ww, joint="curve")
        if round_ends and ww > 2:
            r = ww / 2.0
            for x, y in (p[0], p[-1]):
                self.d.ellipse([x - r, y - r, x + r, y + r], fill=v)

    def circle(self, cx, cy, r, w=1.5, v=255):
        if r > 0:
            self.d.ellipse([(cx - r) * SS, (cy - r) * SS, (cx + r) * SS, (cy + r) * SS], outline=v, width=self._w(w))

    def disc(self, cx, cy, r, v=255):
        self.d.ellipse([(cx - r) * SS, (cy - r) * SS, (cx + r) * SS, (cy + r) * SS], fill=v)

    def ellipse(self, x0, y0, x1, y1, v=255, outline=None, w=1.5):
        if outline is None:
            self.d.ellipse([x0 * SS, y0 * SS, x1 * SS, y1 * SS], fill=v)
        else:
            self.d.ellipse([x0 * SS, y0 * SS, x1 * SS, y1 * SS], outline=outline, width=self._w(w))

    def poly(self, pts, w=1.5, v=255, close=True):
        p = list(pts) + ([pts[0]] if close else [])
        self.line(p, w, v, round_ends=False)

    def fillpoly(self, pts, v=255):
        self.d.polygon([(x * SS, y * SS) for x, y in pts], fill=v)

    def rrect(self, x0, y0, x1, y1, r, v=255):
        self.d.rounded_rectangle([x0 * SS, y0 * SS, x1 * SS, y1 * SS], radius=r * SS, fill=v)

    def capsule(self, a, b, w, v=255):
        """A thick segment from a to b with round ends (a finger, a thumb)."""
        self.line([a, b], w, v)


def ngon(cx, cy, r, n, rot=-math.pi / 2):
    return [(cx + math.cos(rot + 2 * math.pi * i / n) * r, cy + math.sin(rot + 2 * math.pi * i / n) * r) for i in range(n)]


# ---------------------------------------------------------------------------------------------------- lighting
RAMP = [(0.00, (44, 20, 8)), (0.28, (104, 56, 20)), (0.52, (178, 104, 38)), (0.76, (236, 160, 64)), (0.90, (255, 208, 110)), (1.00, (255, 244, 208))]


def ramp(t):
    t = np.clip(t, 0, 1)
    out = np.zeros(t.shape + (3,), np.float32)
    for (t0, c0), (t1, c1) in zip(RAMP[:-1], RAMP[1:]):
        m = (t >= t0) & (t <= t1)
        u = ((t - t0) / (t1 - t0))[..., None]
        out = np.where(m[..., None], col(c0) * (1 - u) + col(c1) * u, out)
    return out


def lit_metal(height, k=5.0, light=(-0.55, -0.62, 0.56), ambient=0.30, spec_pow=26.0, spec_gain=0.5, tint=None):
    """Bronze / gold metal from a height map: a lambert term mapped through the bronze ramp plus a specular edge."""
    gy, gx = np.gradient(height)
    nx, ny, nz = -gx * k, -gy * k, np.ones_like(height)
    n = np.sqrt(nx * nx + ny * ny + nz * nz)
    nx, ny, nz = nx / n, ny / n, nz / n
    L = np.array(light, np.float32)
    L /= np.linalg.norm(L)
    lam = np.clip(nx * L[0] + ny * L[1] + nz * L[2], 0, 1)
    spec = np.clip(lam, 0, 1) ** spec_pow
    t = ambient + (1 - ambient) * lam
    rgb = ramp(t) + col(IVORY)[None, None, :] * (spec * spec_gain)[..., None]
    if tint is not None:
        rgb = rgb * col(tint)[None, None, :]
    return rgb


def rim_of(mask, width=2.0):
    """A thin bright rim just inside the silhouette."""
    inner = blur(mask, width)
    return np.clip(mask - inner, 0, 1) * 1.8


def finish_emblem(mask, rgb, name, halo_r=7.0, halo_gain=0.55, halo_col=EMBER, rim_col=GOLD_L, rim_w=1.6):
    """Silhouette + lit colour + a gold rim + an ember halo outside (the alpha carries the halo, so it also glows when drawn additively)."""
    rim = rim_of(mask, rim_w)[..., None]
    rgb = rgb * (1 - np.clip(rim, 0, 0.85)) + col(rim_col)[None, None, :] * np.clip(rim, 0, 0.85)
    halo = blur(mask, halo_r) * halo_gain * (1 - mask)
    a = np.clip(mask + halo, 0, 1)
    out = (rgb * mask[..., None] + col(halo_col)[None, None, :] * halo[..., None]) / np.maximum(a, 1e-4)[..., None]
    save(out, a, name)


# ---------------------------------------------------------------------------------------------------- fist and hand
def fist_parts(W):
    """The parts of the clenched fist (knuckles up) as masks in a W x W canvas drawn for 256 px: a squat, heavy block, not a glove."""
    s = W / 256.0
    block, fingers, tips, thumb, cuff, band, grooves = (Mask(W, W) for _ in range(7))
    block.rrect(44 * s, 66 * s, 212 * s, 192 * s, 32 * s)
    offs = [12, 2, 0, 8]                                       # the knuckle row is a little uneven
    for i in range(4):
        x0 = 48 + 40 * i
        fingers.rrect((x0 + 1) * s, (44 + offs[i]) * s, (x0 + 39) * s, 112 * s, 19 * s)
        tips.rrect((x0 + 4) * s, 100 * s, (x0 + 36) * s, (140 - 4 * (i % 2)) * s, 14 * s)
    thumb.capsule((54 * s, 158 * s), (178 * s, 146 * s), 46 * s)
    cuff.fillpoly([(80 * s, 188 * s), (176 * s, 188 * s), (184 * s, 256 * s), (72 * s, 256 * s)])
    band.rrect(70 * s, 204 * s, 186 * s, 232 * s, 7 * s)
    for i in range(1, 4):                                      # the grooves between the fingers
        x = (48 + 40 * i) * s
        grooves.line([(x, (54 + offs[i - 1] * 0.2) * s), (x, 140 * s)], 3.6 * s)
    grooves.line([(50 * s, 108 * s), (206 * s, 108 * s)], 2.8 * s)                  # where the fingers fold
    return block, fingers, tips, thumb, cuff, band, grooves


def tex_fist():
    W = 256
    block, fingers, tips, thumb, cuff, band, grooves = (m.final() for m in fist_parts(W))
    sil = np.clip(np.maximum.reduce([block, fingers, tips, thumb, cuff, band]), 0, 1)
    h = blurf(block, 6) * 0.45 + blurf(fingers, 5) * 0.95 + blurf(tips, 4) * 0.8 + blurf(thumb, 4.5) * 1.15 + blurf(cuff, 6) * 0.5 + blurf(band, 3) * 0.75
    h = h - blurf(grooves, 1.6) * 0.9
    xx, yy = grid(W, W)
    h = h + (fbm(W, W, 5) - 0.5) * 0.10                                 # a little hammered unevenness
    rgb = lit_metal(h, k=11.0, ambient=0.10, spec_gain=0.6)
    rgb = rgb * (1 - 0.5 * blur(grooves, 1.4)[..., None])
    band_m = blur(band, 1.0)[..., None]
    rgb = rgb * (1 - band_m * 0.5) + (rgb * col(GOLD_L)[None, None, :] * 1.15) * band_m * 0.5       # the cuff band is brighter gold
    finish_emblem(sil, rgb, "hercules_fist", halo_r=9.0, halo_gain=0.6)


def tex_hand():
    W = 256
    s = 1.0
    palm, fingers, thumb, cuff, band, grooves = (Mask(W, W) for _ in range(6))
    palm.rrect(70, 118, 186, 214, 34)
    # five fingers fanned out (knuckle base, tip), widths taper
    base = [(86, 134), (108, 124), (130, 122), (152, 126), (172, 138)]
    tipP = [(60, 56), (92, 26), (130, 18), (166, 30), (198, 64)]
    wid = [26, 28, 29, 27, 24]
    for (bx, by), (tx, ty), w in zip(base, tipP, wid):
        fingers.capsule((bx, by), (tx, ty), w)
    thumb.capsule((78, 190), (28, 148), 30)
    cuff.fillpoly([(94, 206), (162, 206), (170, 256), (86, 256)])
    band.rrect(88, 214, 168, 238, 6)
    for (bx, by), (tx, ty) in zip(base, tipP):                      # the finger joints: two grooves across each finger
        for f in (0.45, 0.72):
            jx, jy = bx + (tx - bx) * f, by + (ty - by) * f
            dx, dy = ty - by, -(tx - bx)
            n = math.hypot(dx, dy)
            dx, dy = dx / n * 11, dy / n * 11
            grooves.line([(jx - dx, jy - dy), (jx + dx, jy + dy)], 2.4)
    grooves.line([(72, 124), (184, 124)], 2.2)
    pm, fm, tm, cm, bm, gm = (m.final() for m in (palm, fingers, thumb, cuff, band, grooves))
    sil = np.clip(np.maximum.reduce([pm, fm, tm, cm, bm]), 0, 1)
    h = blurf(pm, 6) * 0.5 + blurf(fm, 4.5) * 1.0 + blurf(tm, 4.5) * 1.05 + blurf(cm, 6) * 0.5 + blurf(bm, 3) * 0.75 - blurf(gm, 1.4) * 0.7
    h = h + (fbm(W, W, 9) - 0.5) * 0.10
    rgb = lit_metal(h, k=11.0, ambient=0.10, spec_gain=0.6)
    rgb = rgb * (1 - 0.45 * blur(gm, 1.4)[..., None])
    band_m = blur(bm, 1.0)[..., None]
    rgb = rgb * (1 - band_m * 0.5) + (rgb * col(GOLD_L)[None, None, :] * 1.15) * band_m * 0.5
    finish_emblem(sil, rgb, "hercules_hand", halo_r=9.0, halo_gain=0.6)


# ---------------------------------------------------------------------------------------------------- laurel
def leaf_polygon(cx, cy, ang, length, width, n=14):
    """A pointed leaf (a lens) from its base (cx, cy) along ang: the polygon and its two halves."""
    ca, sa = math.cos(ang), math.sin(ang)
    left, right = [], []
    for i in range(n + 1):
        t = i / n
        w = width * 0.5 * math.sin(math.pi * t ** 0.85) * (1 - 0.2 * t)
        px, py = t * length, w
        left.append((cx + px * ca - py * sa, cy + px * sa + py * ca))
        right.append((cx + px * ca + py * sa, cy + px * sa - py * ca))
    return left, right


def draw_leaf(img_d, cx, cy, ang, length, width, light_dir, ss):
    """One leaf onto an RGBA PIL canvas (drawn at ss x): two lit halves, a midrib, a thin gold edge."""
    left, right = leaf_polygon(cx, cy, ang, length, width)
    # which half faces the light (the upper left) is the brighter one
    nl = (-math.sin(ang), math.cos(ang))                   # the left half's outward normal
    d = nl[0] * light_dir[0] + nl[1] * light_dir[1]
    bright, dark = (255, 222, 112, 255), (196, 124, 44, 255)
    cl, cr = (bright, dark) if d > 0 else (dark, bright)
    mid = [(cx + math.cos(ang) * length * i / 14, cy + math.sin(ang) * length * i / 14) for i in range(15)]
    rib = mid[1:13]
    for half, c in ((left, cl), (right, cr)):
        img_d.polygon([(x * ss, y * ss) for x, y in half + mid[::-1]], fill=c)
    img_d.line([(x * ss, y * ss) for x, y in rib], fill=(255, 244, 200, 255), width=max(1, int(length * 0.035 * ss)))
    for half in (left, right):
        img_d.line([(x * ss, y * ss) for x, y in half], fill=(255, 238, 170, 255), width=max(1, int(0.9 * ss)))


def wreath_rgba(W, ss=3):
    """The laurel wreath as a PIL RGBA canvas: the branches climb either side of a circle from the foot; leaves in pairs, berries, a crimson ribbon."""
    im = Image.new("RGBA", (W * ss, W * ss), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    c = W / 2
    R = W * 0.355
    rng = random.Random(11)
    light = (-0.6, -0.8)
    for side in (-1, 1):
        n = 13
        for i in range(n):
            t = i / (n - 1)
            a = math.pi / 2 + side * (0.22 + t * 2.62)                       # from the foot (down) round to the top
            px, py = c + math.cos(a) * R, c + math.sin(a) * R
            tang = a + side * math.pi / 2                                      # the branch runs this way (towards the top)
            ll = W * (0.150 - 0.055 * t) * (0.94 + 0.12 * rng.random())
            lw = ll * 0.46
            # an inner leaf and an outer leaf, both swept forward along the branch
            draw_leaf(d, px, py, tang - side * 0.62, ll, lw, light, ss)       # inward
            draw_leaf(d, px, py, tang + side * 0.62, ll * 0.96, lw, light, ss)  # outward
        # the stem
        pts = []
        for i in range(40):
            t = i / 39
            a = math.pi / 2 + side * (0.12 + t * 2.74)
            pts.append(((c + math.cos(a) * R) * ss, (c + math.sin(a) * R) * ss))
        d.line(pts, fill=(176, 104, 40, 255), width=int(W * 0.011 * ss))
        for i in range(5):                                                    # berries along the lower half
            a = math.pi / 2 + side * (0.46 + i * 0.36)
            bx, by = c + math.cos(a) * (R - W * 0.034), c + math.sin(a) * (R - W * 0.034)
            r = W * 0.0125
            d.ellipse([(bx - r) * ss, (by - r) * ss, (bx + r) * ss, (by + r) * ss], fill=(255, 238, 170, 255), outline=(176, 104, 40, 255))
    # the ribbon at the foot: a knot and two tails in crimson with a gold edge
    kx, ky = c, c + R + W * 0.015
    tails = [[(kx, ky), (kx - W * 0.085, ky + W * 0.07), (kx - W * 0.045, ky + W * 0.12), (kx - W * 0.012, ky + W * 0.07)],
             [(kx, ky), (kx + W * 0.085, ky + W * 0.07), (kx + W * 0.045, ky + W * 0.12), (kx + W * 0.012, ky + W * 0.07)]]
    for tl in tails:
        d.polygon([(x * ss, y * ss) for x, y in tl], fill=(158, 32, 38, 255), outline=(255, 214, 96, 255))
    r = W * 0.024
    d.ellipse([(kx - r) * ss, (ky - r) * ss, (kx + r) * ss, (ky + r) * ss], fill=(190, 44, 48, 255), outline=(255, 214, 96, 255), width=max(1, int(1.2 * ss)))
    return im.resize((W, W), Image.LANCZOS)


def tex_laurel():
    W = 512
    im = np.asarray(wreath_rgba(W), dtype=np.float32) / 255.0
    a = im[..., 3]
    rgb = im[..., :3]
    # a soft form shading over the whole wreath (lit from the upper left) and a hammered unevenness
    xx, yy = grid(W, W)
    shade = 0.86 + 0.28 * (1 - ((xx / W) * 0.5 + (yy / W) * 0.5))
    rgb = np.clip(rgb * shade[..., None] * (0.94 + 0.12 * fbm(W, W, 21)[..., None]), 0, 1)
    halo = blur(a, 6.0) * 0.5 * (1 - a)
    ao = np.clip(a + halo, 0, 1)
    out = (rgb * a[..., None] + col(EMBER)[None, None, :] * halo[..., None]) / np.maximum(ao, 1e-4)[..., None]
    save(out, ao, "hercules_laurel")


def tex_leaf():
    W = 128
    ss = 4
    im = Image.new("RGBA", (W * ss, W * ss), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    draw_leaf(d, 20, 108, -math.pi / 4, 128, 46, (-0.6, -0.8), ss)
    a = np.asarray(im.resize((W, W), Image.LANCZOS), dtype=np.float32) / 255.0
    halo = blur(a[..., 3], 3.0) * 0.5 * (1 - a[..., 3])
    ao = np.clip(a[..., 3] + halo, 0, 1)
    out = (a[..., :3] * a[..., 3:4] + col(EMBER)[None, None, :] * halo[..., None]) / np.maximum(ao, 1e-4)[..., None]
    save(out, ao, "hercules_leaf")


# ---------------------------------------------------------------------------------------------------- shock ring, wave
def tex_ring():
    W = 512
    c = W / 2
    xx, yy = grid(W, W)
    dx, dy = xx - c, yy - c
    r = np.sqrt(dx * dx + dy * dy) / c
    ang = np.arctan2(dy, dx)
    n1 = fbm(W, W, 3)
    # the band is ragged: its radius wobbles with the angle (torn ground, not a clean circle)
    wob = 0.012 * np.sin(ang * 7 + 1.3) + 0.008 * np.sin(ang * 13 + 0.4) + 0.006 * np.sin(ang * 29)
    rr = r - wob
    core = np.exp(-((rr - 0.93) / 0.018) ** 2)                      # the white-hot edge
    band = np.exp(-((rr - 0.915) / 0.045) ** 2)                      # the thick gold band
    trail = smooth(0.62, 0.92, rr) * (1 - smooth(0.935, 0.975, rr)) * (0.22 + 0.2 * n1)          # a gradient behind it
    outer = np.exp(-((rr - 0.955) / 0.012) ** 2) * 0.5               # a thin sharp lip in front of the band
    grain = 0.78 + 0.45 * np.clip((fbm(W, W, 7, cells=(16, 8, 4), gains=(0.5, 0.3, 0.2)) - 0.5) * 2 + 0.5, 0, 1)
    a = np.clip((core * 1.0 + band * 0.85 * grain + trail + outer) * (1 - smooth(0.975, 1.0, rr)), 0, 1)
    # rubble: bright flecks thrown off the band
    rs = np.random.RandomState(4)
    fl = np.zeros((W, W), np.float32)
    for _ in range(170):
        th = rs.uniform(0, 2 * math.pi)
        rad = 0.93 + rs.normal(0, 0.035)
        px, py = int(c + math.cos(th) * rad * c), int(c + math.sin(th) * rad * c)
        if 2 <= px < W - 2 and 2 <= py < W - 2:
            fl[py - 1:py + 1, px - 1:px + 1] = rs.uniform(0.5, 1.0)
    a = np.clip(a + fl * (1 - smooth(0.975, 1.0, rr)), 0, 1)
    hot = np.clip(core * 1.2 + fl * 0.5, 0, 1)[..., None]
    rgb = col(EMBER)[None, None, :] * (1 - np.clip(band, 0, 1)[..., None]) + col(GOLD)[None, None, :] * np.clip(band, 0, 1)[..., None]
    rgb = rgb * (1 - hot) + col(IVORY)[None, None, :] * hot
    save(rgb, a, "hercules_ring")


def tex_wave():
    W = 512
    c = W / 2
    xx, yy = grid(W, W)
    dx, dy = xx - c, yy - c
    r = np.sqrt(dx * dx + dy * dy) / c
    ang = np.arctan2(dy, dx)
    rs = np.random.RandomState(9)
    # radial streaks: the gradient behind the front is combed outwards
    streak = np.zeros_like(r)
    for _ in range(90):
        th = rs.uniform(0, 2 * math.pi)
        wd = rs.uniform(0.012, 0.05)
        d_ang = np.angle(np.exp(1j * (ang - th)))
        streak += np.exp(-(d_ang / wd) ** 2) * rs.uniform(0.3, 1.0)
    streak = np.clip(streak, 0, 1.4)
    front = np.exp(-((r - 0.95) / 0.012) ** 2) + np.exp(-((r - 0.95) / 0.035) ** 2) * 0.4
    body = smooth(0.30, 0.95, r) ** 1.6 * (1 - smooth(0.952, 0.985, r))
    a = np.clip(front + body * (0.18 + 0.30 * streak) + body * 0.10, 0, 1) * (1 - smooth(0.975, 1.0, r))
    hot = np.clip(front * 1.1, 0, 1)[..., None]
    rgb = col((255, 228, 176))[None, None, :] * (1 - hot) + col(WHITE)[None, None, :] * hot
    save(rgb, a, "hercules_wave")


# ---------------------------------------------------------------------------------------------------- ground cracks
def crack_paths(rng, cx, cy, n_main, R, jag=0.12, branch=0.45, depth=2, straight=False):
    """Crack polylines radiating from (cx, cy): main cracks with a random walk, side branches that split off. Returns [(points, weight)]."""
    out = []

    def walk(x, y, ang, length, w, d):
        pts = [(x, y)]
        step = length / 9.0 if not straight else length / 5.0
        a = ang
        for i in range(9 if not straight else 5):
            a += rng.uniform(-jag, jag) * (1.0 if not straight else 2.2)
            x += math.cos(a) * step * rng.uniform(0.8, 1.2)
            y += math.sin(a) * step * rng.uniform(0.8, 1.2)
            pts.append((x, y))
            if d > 0 and i > 1 and rng.random() < branch * 0.5:
                sgn = rng.choice([-1, 1])
                walk(x, y, a + sgn * rng.uniform(0.45, 1.0), length * rng.uniform(0.28, 0.5) * (1 - i / 14), w * 0.55, d - 1)
        out.append((pts, w))

    for i in range(n_main):
        ang = 2 * math.pi * (i + rng.uniform(-0.3, 0.3)) / n_main
        walk(cx + math.cos(ang) * 6, cy + math.sin(ang) * 6, ang, R * rng.uniform(0.55, 0.98), 1.0, depth)
    return out


def tex_cracks():
    W = 512
    c = W / 2
    rng = random.Random(21)
    dark, ember, core = Mask(W, W), Mask(W, W), Mask(W, W)
    paths = crack_paths(rng, c, c, 9, c * 0.97, jag=0.2, branch=0.42, depth=1, straight=True)
    for pts, w in paths:
        n = len(pts)
        for i in range(n - 1):
            f = 1 - i / n                                                    # thick at the centre, thin at the tip
            seg = [pts[i], pts[i + 1]]
            dark.line(seg, (5.0 + 17.0 * f) * w, 255)
            ember.line(seg, (2.4 + 9.0 * f) * w, 255)
            core.line(seg, (1.0 + 3.8 * f) * w, 255)
    # crushed rings round the centre: broken arcs
    for rad, wd in ((0.14, 7.0), (0.30, 5.0), (0.50, 3.2)):
        a0 = rng.uniform(0, 6.28)
        for _ in range(rng.randint(3, 5)):
            a1 = a0 + rng.uniform(0.3, 0.8)
            pts = [(c + math.cos(a) * rad * c * rng.uniform(0.95, 1.05), c + math.sin(a) * rad * c * rng.uniform(0.95, 1.05)) for a in np.linspace(a0, a1, 5)]
            dark.line(pts, wd * 1.9)
            ember.line(pts, wd * 0.9)
            core.line(pts, wd * 0.35)
            a0 = a1 + rng.uniform(0.4, 1.1)
    dm, em, cm = dark.final(), ember.final(), core.final()
    xx, yy = grid(W, W)
    r = np.sqrt((xx - c) ** 2 + (yy - c) ** 2) / c
    fade = 1 - smooth(0.9, 1.0, r)
    dm = np.clip(dm + blur(dm, 3.0) * 0.5, 0, 1)
    flick = 0.55 + 0.45 * fbm(W, W, 3, cells=(48, 24, 12), gains=(0.5, 0.3, 0.2))
    rgb = col((46, 30, 22))[None, None, :] * (1 - em[..., None])
    rgb = rgb + (col(EMBER) * flick[..., None] * (1 - cm[..., None]) + col(GOLD_L) * cm[..., None]) * em[..., None]
    a = np.clip(dm * 0.94 + em * 0.1, 0, 1) * fade
    save(rgb, a, "hercules_cracks")



# ---------------------------------------------------------------------------------------------------- the web of cracks in the air
def tex_web():
    W = 512
    c = W / 2
    rng = random.Random(31)
    NS = 15                                                     # radial cracks, unevenly spaced
    ring_r = [0.075, 0.15, 0.25, 0.37, 0.52, 0.70]
    ang, a = [], rng.uniform(0, 1)
    gaps = [rng.uniform(0.35, 1.7) for _ in range(NS)]
    tot = sum(gaps)
    for g in gaps:
        a += g / tot * 2 * math.pi
        ang.append(a)
    reach = [rng.choice([2, 3, 3, 4, 4, 5, 5, 6, 6, 6]) for _ in range(NS)]      # how many rings each radial crack crosses
    V = [[None] * (len(ring_r) + 1) for _ in range(NS)]
    for i in range(NS):
        aa = ang[i]
        for j, rr in enumerate(ring_r):
            ja = aa + rng.uniform(-0.035, 0.035)
            r0 = rr * c * rng.uniform(0.86, 1.14)
            V[i][j] = (c + math.cos(ja) * r0, c + math.sin(ja) * r0)
        V[i][len(ring_r)] = (c + math.cos(aa + rng.uniform(-0.03, 0.03)) * c * rng.uniform(0.9, 0.99), c + math.sin(aa) * c * rng.uniform(0.9, 0.99))
    lines, glow = Mask(W, W), Mask(W, W)
    fill_void, fill_frost, fill_hole = Mask(W, W), Mask(W, W), Mask(W, W)
    for i in range(NS):
        i2 = (i + 1) % NS
        top = min(reach[i], reach[i2])
        for j in range(len(ring_r)):
            if j + 1 > top:
                continue
            cell = [V[i][j], V[i2][j], V[i2][j + 1], V[i][j + 1]]
            roll = rng.random()
            if roll < 0.30:
                fill_void.fillpoly(cell)
            elif roll < 0.42 and j > 1:
                fill_hole.fillpoly(cell)
            elif roll < 0.80:
                fill_frost.fillpoly(cell)
        tri = [(c, c), V[i][0], V[i2][0]]
        (fill_void if rng.random() < 0.5 else fill_frost).fillpoly(tri)
    for i in range(NS):
        pts = [(c, c)] + [V[i][j] for j in range(reach[i] + 1)]
        w = 2.6 if reach[i] > 3 else 2.0
        lines.line(pts, w)
        glow.line(pts, w * 3.0)
        ex, ey = pts[-1]
        ang_o = math.atan2(ey - c, ex - c)
        L = rng.uniform(0.04, 0.16) * c * (1.0 if reach[i] >= 5 else 0.5)
        lines.line([(ex, ey), (ex + math.cos(ang_o) * L, ey + math.sin(ang_o) * L)], 1.5)
        for j in range(1, reach[i], 2):                                  # hairlines that split off a radial crack
            if rng.random() < 0.7:
                bx, by = V[i][j]
                ba = ang_o + rng.choice([-1, 1]) * rng.uniform(0.5, 1.1)
                bl = rng.uniform(0.03, 0.08) * c
                lines.line([(bx, by), (bx + math.cos(ba) * bl, by + math.sin(ba) * bl)], 1.2)
    for j in range(len(ring_r)):
        for i in range(NS):
            i2 = (i + 1) % NS
            if j + 1 <= min(reach[i], reach[i2]) + 0 and rng.random() < 0.72:
                lines.line([V[i][j], V[i2][j]], 2.0 if j % 2 else 1.5)
                glow.line([V[i][j], V[i2][j]], 4.0)
    lm, gm = lines.final(), glow.final()
    vm, fm, hm = fill_void.final(), fill_frost.final(), fill_hole.final()
    xx, yy = grid(W, W)
    r = np.sqrt((xx - c) ** 2 + (yy - c) ** 2) / c
    edge = 1 - smooth(0.93, 1.0, r)
    glowb = blur(gm, 4.0) * 0.5
    # fills: a cell that fell out is a black void with a crimson depth, a frosted cell keeps a faint ivory sheen
    a_void = np.clip(vm * 0.62 + hm * 0.88, 0, 1)
    a_frost = fm * 0.07 * (1 - a_void)
    void_rgb = col((22, 8, 14))[None, None, :] + col(CRIMSON)[None, None, :] * (0.7 * (1 - smooth(0.0, 0.8, r)))[..., None] * (hm[..., None] * 0.0 + 1.0)
    fill_a = np.clip(a_void + a_frost, 0, 1)
    fill_rgb = (void_rgb * a_void[..., None] + col(IVORY)[None, None, :] * a_frost[..., None]) / np.maximum(fill_a, 1e-4)[..., None]
    line_a = np.clip(lm + glowb * 0.55, 0, 1)
    line_rgb = col(WHITE)[None, None, :] * np.clip(lm, 0, 1)[..., None] + col(GOLD)[None, None, :] * (1 - np.clip(lm, 0, 1))[..., None]
    ao = np.clip(line_a + fill_a * (1 - line_a), 0, 1) * edge
    rgb = (line_rgb * line_a[..., None] + fill_rgb * (fill_a * (1 - line_a))[..., None]) / np.maximum(line_a + fill_a * (1 - line_a), 1e-4)[..., None]
    save(np.clip(rgb, 0, 1), ao, "hercules_web")



# ---------------------------------------------------------------------------------------------------- dust, chips
def puff(S, seed, tint_top=(238, 218, 182), tint_bot=(138, 112, 82)):
    """One dust puff: a heap of soft blobs eroded by noise into a cauliflower edge, lit from above and shaded below."""
    rs = np.random.RandomState(seed)
    xx, yy = grid(S, S)
    dens = np.zeros((S, S), np.float32)
    for _ in range(16):
        ang = rs.uniform(0, 2 * math.pi)
        dist = S * rs.uniform(0.0, 0.2)
        cx, cy = S * 0.5 + math.cos(ang) * dist, S * 0.55 + math.sin(ang) * dist * 0.8
        rad = S * rs.uniform(0.08, 0.2)
        dens += np.exp(-(((xx - cx) ** 2 + (yy - cy) ** 2) / (rad * rad)) * 1.3)
    n = fbm(S, S, seed + 3, cells=(S // 3, S // 6, S // 12, S // 24), gains=(0.35, 0.3, 0.2, 0.15))
    dens = np.clip(dens * (0.35 + 1.25 * n) - 0.12, 0, 1)
    a = smooth(0.05, 0.5, dens) * 0.95
    a = a * (1 - smooth(0.40, 0.49, np.sqrt((xx - S / 2) ** 2 + (yy - S / 2) ** 2) / S))
    top = np.clip((S * 0.6 - yy) / (S * 0.5), -1, 1) * 0.5 + 0.5
    rgb = col(tint_bot)[None, None, :] * (1 - top[..., None]) + col(tint_top)[None, None, :] * top[..., None]
    rgb = rgb * (0.70 + 0.5 * np.clip(dens, 0, 1)[..., None] ** 0.8 * (0.8 + 0.4 * n[..., None]))
    return np.clip(rgb, 0, 1), a


def tex_dust():
    S = 128
    P = np.zeros((2 * S, 2 * S, 3), np.float32)
    A = np.zeros((2 * S, 2 * S), np.float32)
    for k in range(4):
        rgb, a = puff(S, 40 + k * 7)
        x, y = (k % 2) * S, (k // 2) * S
        P[y:y + S, x:x + S] = rgb
        A[y:y + S, x:x + S] = a
    save(P, A, "hercules_dust")


def rock_chip(S, seed):
    """One chip or slab of rock in an S x S cell: a thick slab (a lit top face and a dark side face), cracks and a hot rim where it was torn from the ground."""
    rng = random.Random(seed)
    top_m, all_m = Mask(S, S), Mask(S, S)
    n = rng.randint(6, 8)
    cx, cy = S / 2, S / 2 - 6
    base = rng.uniform(0.0, 6.28)
    squash = rng.choice([0.62, 0.78, 0.95])
    thick = rng.uniform(9, 15)
    pts = []
    for i in range(n):
        a = base + 2 * math.pi * i / n + rng.uniform(-0.18, 0.18)
        r = S * rng.uniform(0.34, 0.47)
        pts.append((cx + math.cos(a) * r, cy + math.sin(a) * r * squash))
    top_m.fillpoly(pts)
    for dy in np.linspace(0, thick, 5):
        all_m.fillpoly([(x, y + dy) for x, y in pts])
    top, sil = top_m.final(), all_m.final()
    xx, yy = grid(S, S)
    tilt = rng.uniform(-0.5, 0.5)
    h = (xx - cx) * tilt * 0.01 + blurf(top, 3.5) * 1.0 + (fbm(S, S, seed, cells=(S // 4, S // 8, S // 16), gains=(0.5, 0.3, 0.2)) - 0.5) * 0.4
    gy, gx = np.gradient(h)
    nx, ny, nz = -gx * 9, -gy * 9, np.ones_like(h)
    nn = np.sqrt(nx * nx + ny * ny + nz * nz)
    lam = np.clip((nx * -0.5 + ny * -0.65 + nz * 0.57) / nn, 0, 1)
    grain = 0.8 + 0.4 * fbm(S, S, seed + 1)
    topc = (col(STONE_D)[None, None, :] * (1 - lam[..., None]) + col((228, 206, 170))[None, None, :] * lam[..., None]) * grain[..., None]
    sidec = col((58, 46, 38))[None, None, :] * (0.7 + 0.6 * grain[..., None])
    rgb = np.where((top > 0.5)[..., None], topc, sidec)
    cr = Mask(S, S)
    x0, y0 = rng.uniform(0.3, 0.5) * S, rng.uniform(0.28, 0.4) * S
    cr.line([(x0, y0), (x0 + rng.uniform(-0.2, 0.2) * S, y0 + 0.14 * S), (x0 + rng.uniform(-0.2, 0.2) * S, y0 + 0.3 * S)], 1.6)
    crm = cr.final()
    rgb = rgb * (1 - 0.6 * crm[..., None])
    sidem = np.clip(sil - top, 0, 1)
    rimhot = np.clip(sil - blur(sil, 1.8), 0, 1) * (0.4 + 0.6 * np.clip((yy - cy) / S * 3, 0, 1))
    rgb = rgb + col(EMBER)[None, None, :] * (rimhot * 1.2 + sidem * 0.22)[..., None] + col(GOLD_L)[None, None, :] * (np.clip(top - blur(top, 1.3), 0, 1) * np.clip((cy - yy) / S * 4 + 0.4, 0, 1) * 0.9)[..., None]
    return np.clip(rgb, 0, 1), sil


def glass_shard(S, seed):
    """One shard of cracked space: a long thin triangle, bright rim, a faint ivory heart (drawn additively)."""
    rng = random.Random(seed)
    m, edge = Mask(S, S), Mask(S, S)
    cx, cy = S / 2, S / 2
    a0 = rng.uniform(0, 6.28)
    L = S * rng.uniform(0.40, 0.5)
    Wd = S * rng.uniform(0.13, 0.24)
    p0 = (cx + math.cos(a0) * L, cy + math.sin(a0) * L)
    p1 = (cx + math.cos(a0 + 2.4) * Wd + math.cos(a0 + math.pi) * L * 0.2, cy + math.sin(a0 + 2.4) * Wd + math.sin(a0 + math.pi) * L * 0.2)
    p2 = (cx + math.cos(a0 - 2.2) * Wd * 1.2 + math.cos(a0 + math.pi) * L * 0.25, cy + math.sin(a0 - 2.2) * Wd * 1.2 + math.sin(a0 + math.pi) * L * 0.25)
    tri = [p0, p1, p2] if rng.random() < 0.5 else [p0, p1, (p2[0] * 0.5 + p1[0] * 0.5, p2[1] * 0.5 + p1[1] * 0.5), p2]
    m.fillpoly(tri)
    edge.poly(tri, 2.4)
    sil, em = m.final(), edge.final()
    xx, yy = grid(S, S)
    g = blur(em, 3.0) * 0.8
    a = np.clip(sil * 0.45 + em + g * 0.6, 0, 1)
    rgb = col(IVORY)[None, None, :] * (np.clip(em, 0, 1) + sil * 0.45)[..., None] + col(GOLD)[None, None, :] * (g * 0.6)[..., None]
    rgb = rgb / np.maximum(a, 1e-4)[..., None]
    return np.clip(rgb, 0, 1), a


def tex_chips():
    S = 128
    P = np.zeros((2 * S, 4 * S, 3), np.float32)
    A = np.zeros((2 * S, 4 * S), np.float32)
    for k in range(4):
        rgb, a = rock_chip(S, 100 + k * 13)
        P[0:S, k * S:(k + 1) * S] = rgb
        A[0:S, k * S:(k + 1) * S] = a
        rgb, a = glass_shard(S, 200 + k * 17)
        P[S:2 * S, k * S:(k + 1) * S] = rgb
        A[S:2 * S, k * S:(k + 1) * S] = a
    save(P, A, "hercules_chips")


# ---------------------------------------------------------------------------------------------------- streak, chain
def tex_streak():
    W, H = 64, 512
    xs = (np.arange(W, dtype=np.float32) + 0.5) / W * 2 - 1
    ys = (np.arange(H, dtype=np.float32) + 0.5) / H           # 0 = tail ... 1 = head
    X, Y = np.meshgrid(xs, ys)
    half = 0.10 + 0.82 * Y ** 0.7                                # the streak widens towards its head
    prof = np.exp(-(X / half) ** 2 * 2.6)
    core = np.exp(-(X / (half * 0.35)) ** 2 * 2.0)
    head = smooth(0.90, 1.0, Y)                                  # a blunt bright head
    taper = Y ** 1.4
    a = np.clip((prof * 0.8 + core * 0.5) * taper + head * prof * 0.4, 0, 1) * (1 - smooth(0.985, 1.0, Y) * 0.0)
    a = a * (1 - smooth(0.96, 1.0, Y) * smooth(0.55, 1.0, np.abs(X)))                 # the corners of the head are rounded
    hot = np.clip(core * 1.2 + head * 0.5, 0, 1)[..., None]
    rgb = col(EMBER)[None, None, :] * (1 - np.clip(prof, 0, 1)[..., None]) + col(GOLD)[None, None, :] * np.clip(prof, 0, 1)[..., None]
    rgb = rgb * (1 - hot) + col(IVORY)[None, None, :] * hot
    save(rgb, a, "hercules_streak")


def tex_chain():
    W, H = 128, 512
    per = 128
    # draw on a canvas of three tiles so the lighting wraps in V, then keep the middle one
    TH = H * 3
    face, edge = Mask(W, TH), Mask(W, TH)
    for t in range(12):
        y0 = t * per
        cy = y0 + 32
        face.d.ellipse([(64 - 40) * SS, (cy - 52) * SS, (64 + 40) * SS, (cy + 52) * SS], outline=255, width=24 * SS)
        cy2 = y0 + 96
        edge.d.ellipse([(64 - 17) * SS, (cy2 - 52) * SS, (64 + 17) * SS, (cy2 + 52) * SS], fill=255)
    fm, em = face.final(), edge.final()
    h = blurf(fm, 6.0) * 1.3 + blurf(em, 4.0) * 1.1
    h = np.where(em > 0.5, blurf(em, 4.0) * 1.4 + 0.2, h)         # the edge-on links sit in front of the face-on ones
    sil = np.clip(np.maximum(fm, em), 0, 1)
    rgb = lit_metal(h, k=9.0, ambient=0.24, spec_gain=0.6)
    rgb = rgb * (0.86 + 0.24 * fbm(W, TH, 3, cells=(32, 16, 8), gains=(0.5, 0.3, 0.2))[..., None])
    halo = blur(sil, 5.0) * 0.45 * (1 - sil)
    ao = np.clip(sil + halo, 0, 1)
    out = (rgb * sil[..., None] + col(EMBER)[None, None, :] * halo[..., None]) / np.maximum(ao, 1e-4)[..., None]
    save(out[H:2 * H], ao[H:2 * H], "hercules_chain")



# ---------------------------------------------------------------------------------------------------- flash, mote
def tex_flash():
    W = 256
    c = W / 2
    xx, yy = grid(W, W)
    dx, dy = xx - c, yy - c
    r = np.sqrt(dx * dx + dy * dy) / c
    ang = np.arctan2(dy, dx)
    rs = np.random.RandomState(17)
    # a jagged burst: the radius alternates between long and short spikes with a little random
    n = 18
    spike = np.zeros_like(r)
    # a periodic function of the angle through 18 sectors
    sect = (ang / (2 * math.pi) * n) % 1.0
    k = np.abs(sect - 0.5) * 2
    rad_vals = rs.uniform(0.38, 0.62, n)
    idx = (np.floor((ang / (2 * math.pi) * n) % n)).astype(int)
    edge_r = rad_vals[idx] * (0.55 + 0.45 * (1 - k))
    burst = (1 - smooth(edge_r - 0.03, edge_r + 0.01, r))
    inner = np.exp(-(r / 0.28) ** 2 * 2.0)
    # four long cross spikes and four short diagonals
    cross = np.zeros_like(r)
    for a0, L, w in ((0, 0.98, 0.030), (math.pi / 2, 0.98, 0.030), (math.pi, 0.98, 0.030), (-math.pi / 2, 0.98, 0.030), (math.pi / 4, 0.62, 0.022), (3 * math.pi / 4, 0.62, 0.022), (-math.pi / 4, 0.62, 0.022), (-3 * math.pi / 4, 0.62, 0.022)):
        d_ang = np.angle(np.exp(1j * (ang - a0)))
        cross += np.exp(-(d_ang / (w * (1.0 + 0.2 * (1 - r)) / np.maximum(r, 0.06) ** 0.5)) ** 2) * (1 - smooth(L * 0.5, L, r))
    cross = np.clip(cross, 0, 1) * smooth(0.0, 0.1, r)
    a = np.clip(burst * 0.92 + inner * 0.8 + cross * 0.9, 0, 1) * (1 - smooth(0.96, 1.0, r))
    hot = np.clip(inner * 1.2 + burst * smooth(0.45, 0.0, r) * 0.9, 0, 1)[..., None]
    rgb = col(GOLD)[None, None, :] * (1 - hot) + col(WHITE)[None, None, :] * hot
    rgb = rgb * (1 - 0.35 * smooth(0.25, 0.6, r)[..., None] * (1 - cross[..., None])) + col(EMBER)[None, None, :] * 0.35 * (smooth(0.25, 0.6, r)[..., None] * (1 - cross[..., None]))
    save(rgb, a, "hercules_flash")


def tex_mote():
    W = 64
    xx, yy = grid(W, W)
    r = np.sqrt((xx - W / 2) ** 2 + (yy - W / 2) ** 2) / (W / 2)
    a = np.clip(np.exp(-(r / 0.36) ** 2 * 1.7) * 1.0 + np.exp(-(r / 0.8) ** 2 * 2.5) * 0.3, 0, 1) * (1 - smooth(0.9, 1.0, r))
    hot = np.exp(-(r / 0.2) ** 2)[..., None]
    rgb = col(EMBER)[None, None, :] * (1 - hot) + col((255, 240, 190))[None, None, :] * hot
    save(rgb, a, "hercules_mote")


# ---------------------------------------------------------------------------------------------------- plates
def tex_plates():
    W, H = 256, 512
    xx, yy = grid(W, H)
    cx = W / 2
    yn = yy / H
    halfw = W * (0.43 * np.sqrt(np.clip(1 - ((yn - 0.52) / 0.50) ** 2, 0, 1)) * (0.80 + 0.22 * smooth(0.0, 0.45, yn)))
    body = 1 - smooth(halfw - 3, halfw + 1, np.abs(xx - cx))
    body = body * smooth(0.02 * H, 0.05 * H, yy) * (1 - smooth(0.96 * H, 0.985 * H, yy))
    # overlapping lamellar scales: each plate is a domed shield with a rounded lower edge, a lower row overlaps nothing above it but is painted first so the row above lies over it
    P = np.zeros((H, W, 3), np.float32)
    A = np.zeros((H, W), np.float32)
    rows, pw, ph = 12, 54, 54
    plates = []
    for rw in range(rows):
        y = H * 0.02 + rw * (H * 0.96 - ph) / (rows - 1)
        off = (pw / 2) if rw % 2 else 0
        x = -pw + off
        while x < W + pw:
            plates.append((x, y))
            x += pw - 2
    for rw in range(rows - 1, -1, -1):                                    # the lowest row first
        for (x, y) in [q for q in plates if abs(q[1] - (H * 0.02 + rw * (H * 0.96 - ph) / (rows - 1))) < 1e-3]:
            x0, y0, x1, y1 = int(max(0, x)), int(max(0, y)), int(min(W, x + pw)), int(min(H, y + ph))
            if x1 - x0 < 4 or y1 - y0 < 4:
                continue
            gx, gy = np.meshgrid(np.arange(x0, x1, dtype=np.float32) + 0.5, np.arange(y0, y1, dtype=np.float32) + 0.5)
            u = (gx - (x + pw / 2)) / (pw / 2 - 1.5)                       # -1..1 across the plate
            v = (gy - y) / ph                                              # 0..1 down the plate
            shape = np.where(v < 0.55, 1.0, np.clip(1 - ((v - 0.55) / 0.45) ** 2 * 0 - (np.abs(u) ** 2.2 + 0) * 0, 0, 1))
            inside = (np.abs(u) < 1) & (v >= 0) & (v < 1) & (u * u + np.maximum(v - 0.5, 0) ** 2 * 4 < 1.0)
            dome = np.sqrt(np.clip(1 - u * u * 0.8 - np.maximum(v - 0.45, 0) ** 2 * 2.5, 0, 1))
            lum = np.clip(0.30 + 0.55 * dome - 0.22 * u + 0.18 * (0.5 - v), 0, 1)
            rim = np.clip((u * u + np.maximum(v - 0.5, 0) ** 2 * 4 - 0.72) / 0.28, 0, 1)
            pc = ramp(lum * 0.95 + (rim * 0.0))
            pc = pc * (1 - 0.2 * rim[..., None]) + col(GOLD_L)[None, None, :] * (rim * 0.75)[..., None]
            sub = (slice(y0, y1), slice(x0, x1))
            P[sub] = np.where(inside[..., None], pc, P[sub])
            A[sub] = np.where(inside, 1.0, A[sub])
    # a dark joint where one plate lies over another
    a = A * body
    rgb = P * (0.93 + 0.16 * fbm(W, H, 4)[..., None])
    rgb = rgb * (0.7 + 0.45 * (1 - (np.abs(xx - cx) / (W * 0.46)) ** 2)[..., None])
    halo = blur(body, 7.0) * 0.45 * (1 - body)
    ao = np.clip(a + halo, 0, 1)
    out = (rgb * a[..., None] + col(EMBER)[None, None, :] * halo[..., None]) / np.maximum(ao, 1e-4)[..., None]
    save(np.clip(out, 0, 1), ao, "hercules_plates")



# ---------------------------------------------------------------------------------------------------- flame
def tex_flame():
    W, H = 128, 256
    xs = (np.arange(W, dtype=np.float32) + 0.5) / W * 2 - 1
    ys = (np.arange(H, dtype=np.float32) + 0.5) / H           # 0 = the tip, 1 = the foot
    X, Y = np.meshgrid(xs, ys)
    tongues = [(0.0, 0.04, 0.5, 0.05), (-0.34, 0.30, 0.55, -0.22), (0.36, 0.22, 0.5, 0.26), (-0.14, 0.42, 0.45, -0.12), (0.18, 0.46, 0.42, 0.10)]     # centre x, tip y, half width at the foot, lean
    cover = np.zeros_like(X)
    hot = np.zeros_like(X)
    for k, (x0, tip, hw, lean) in enumerate(tongues):
        t = np.clip((Y - tip) / (1 - tip), 0, 1)                    # 0 at the tip, 1 at the foot
        xc = x0 + lean * (1 - t) ** 1.6 + 0.04 * np.sin(t * 9 + k * 2.1)
        wid = hw * np.sin(np.pi * np.clip(t, 0, 1) ** 0.62 * 0.5 + 0.0) ** 1.3 * (0.42 + 0.58 * t)
        wid = wid * (1 + 0.12 * np.sin(t * 17 + k * 1.3))
        d = np.abs(X - xc)
        body = (1 - smooth(wid * 0.55, wid + 0.012, d)) * (Y > tip)
        cover = np.maximum(cover, body)
        hot = np.maximum(hot, (1 - smooth(wid * 0.1, wid * 0.62, d)) * (Y > tip + 0.05) * t ** 0.5 * (1.0 - 0.3 * k / 5))
    foot = 1 - smooth(0.80, 1.0, Y) * 0.7
    a = np.clip(cover * 0.9 + hot * 0.3, 0, 1) * foot * smooth(1.0, 0.85, np.abs(X))
    edge = np.clip(1 - hot * 1.1, 0, 1)[..., None]
    tipc = smooth(0.0, 0.35, Y)[..., None]
    rgb = col(GOLD)[None, None, :] * (1 - edge) + col(EMBER)[None, None, :] * edge
    rgb = rgb * (0.55 + 0.45 * tipc) + col(CRIMSON)[None, None, :] * (1 - tipc) * 0.55
    hh = (hot * smooth(0.12, 0.9, Y) * (1 - smooth(0.7, 0.95, Y)))[..., None]
    rgb = rgb * (1 - hh) + col(IVORY)[None, None, :] * hh
    save(np.clip(rgb, 0, 1), a, "hercules_flame")



# ---------------------------------------------------------------------------------------------------- crater
def tex_crater():
    W = 512
    c = W / 2
    xx, yy = grid(W, W)
    dx, dy = xx - c, yy - c
    r = np.sqrt(dx * dx + dy * dy) / c
    ang = np.arctan2(dy, dx)
    wob = 0.05 * np.sin(ang * 5 + 0.7) + 0.035 * np.sin(ang * 9 + 2.0) + 0.02 * np.sin(ang * 17)
    rr = r - wob
    pit = 1 - smooth(0.34, 0.50, rr)
    rim = np.exp(-((rr - 0.56) / 0.07) ** 2)
    n = fbm(W, W, 6)
    rs = np.random.RandomState(8)
    # radial fractures in the crushed ground
    fr = np.zeros_like(r)
    for _ in range(22):
        th = rs.uniform(0, 2 * math.pi)
        d_ang = np.angle(np.exp(1j * (ang - th)))
        w = rs.uniform(0.012, 0.03)
        fr += np.exp(-(d_ang * np.maximum(r, 0.1) / w) ** 2) * smooth(0.3, 0.45, r) * (1 - smooth(0.55, rs.uniform(0.7, 0.98), r))
    fr = np.clip(fr, 0, 1)
    # spatter
    sp = np.zeros_like(r)
    for _ in range(160):
        th = rs.uniform(0, 2 * math.pi)
        rad = rs.uniform(0.5, 0.95)
        px, py = int(c + math.cos(th) * rad * c), int(c + math.sin(th) * rad * c)
        s = rs.randint(1, 4)
        sp[max(0, py - s):py + s, max(0, px - s):px + s] = rs.uniform(0.4, 1.0)
    edgefade = 1 - smooth(0.88, 1.0, r)
    a = np.clip(pit * 0.9 + rim * (0.55 + 0.3 * n) + fr * 0.55 + sp * 0.5 * smooth(0.4, 0.6, r), 0, 1) * edgefade
    rgb = col((34, 24, 18))[None, None, :] * (pit * (1 - 0.3 * n))[..., None]
    rgb = rgb + col((136, 116, 92))[None, None, :] * (rim * 0.95)[..., None] + col((46, 32, 24))[None, None, :] * (fr * 0.9)[..., None] + col((116, 96, 76))[None, None, :] * (sp * 0.7)[..., None]
    # ember coals deep in the pit
    coal = np.exp(-(rr / 0.16) ** 2) * (0.4 + 0.6 * n) * 0.55
    rgb = rgb + col(EMBER)[None, None, :] * coal[..., None]
    a = np.clip(a + coal * 0.3, 0, 1)
    rgb = rgb / np.maximum(pit * 0.9 + rim * 0.8 + fr * 0.5 + sp * 0.4 + coal, 1e-4)[..., None] * np.clip(pit * 0.9 + rim * 0.8 + fr * 0.5 + sp * 0.4 + coal, 0, 1)[..., None]
    save(np.clip(rgb, 0, 1), a, "hercules_crater")


ALL = {
    "fist": tex_fist, "hand": tex_hand, "laurel": tex_laurel, "leaf": tex_leaf, "ring": tex_ring, "wave": tex_wave, "cracks": tex_cracks, "web": tex_web,
    "dust": tex_dust, "chips": tex_chips, "streak": tex_streak, "chain": tex_chain, "flash": tex_flash, "mote": tex_mote, "plates": tex_plates,
    "flame": tex_flame, "crater": tex_crater,
}

if __name__ == "__main__":
    wanted = sys.argv[1:] or list(ALL)
    for n in wanted:
        ALL[n]()
