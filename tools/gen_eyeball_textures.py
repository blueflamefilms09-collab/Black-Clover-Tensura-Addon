"""Generates the Eyeball Magic VFX textures (wet fleshy eyeballs, capillary veins, eyelids, optic-nerve tendrils, gaze reticles).

Deterministic (fixed seeds, no fonts, no external images, numpy + Pillow only).
Output: src/main/resources/assets/nusmp/textures/particle/eyeball_*.png

    python3 -B tools/gen_eyeball_textures.py

Look reference: the Eyeball Magic wiki page (free-floating eyeballs that turn and move in midair, share vision, track the target's
magic, stamina and muscle movement) and the owner's brief: fleshy or ethereal eyeballs with veins and a roving pupil, a different
flavour from the stylised Eye Magic.  So: wet cream sclera with red capillaries and a pink limb, a fibrous iris (grey-scale so the
vertex colour paints it), a glossy slit pupil, fleshy eyelids with creases, optic-nerve tendrils with spiralling vessels, vein webs
for the ground and walls, and line-art gaze sigils for the zone.  Palette: cream, blood red, deep maroon flesh (cover 5A1A2A),
glow red (FF3A3A).

Texture convention (as the rest of textures/particle): grey-scale or white with alpha where the vertex colour tints it (iris, veins,
rings, sigil, scan, beam, corona, gloss, reticle, ripple); real colours where the colour is the point (sclera, lids, nerves, fragments,
the small baked eyeball).  The NEGATIVE-blend ring (eyeball_warp) carries its intensity in RGB because that blend ignores alpha.
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 4  # supersampling for line work


# ---------------------------------------------------------------------------------------------------- toolkit
def save(arr_or_img, name):
    """arr: float RGBA (h, w, 4) in 0..255 or a PIL image."""
    if isinstance(arr_or_img, np.ndarray):
        im = Image.fromarray(np.clip(arr_or_img, 0, 255).astype(np.uint8), "RGBA")
    else:
        im = arr_or_img
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, name + ".png"), optimize=True)
    print("wrote", name, im.size)


def grid(w, h):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    return xx, yy


def smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0.0, 1.0)
    return t * t * (3 - 2 * t)


def lattice(u, v, seed, wrap_u=0, wrap_v=0):
    """Value noise at float lattice coordinates (arrays); wrap_* = period in cells so the result tiles on that axis."""
    rng = np.random.default_rng(seed)
    nu = (wrap_u if wrap_u else int(np.ceil(u.max())) + 2) + 1
    nv = (wrap_v if wrap_v else int(np.ceil(v.max())) + 2) + 1
    g = rng.random((nv, nu)).astype(np.float32)
    if wrap_u:
        g[:, -1] = g[:, 0]
    if wrap_v:
        g[-1, :] = g[0, :]
    u0, v0 = np.floor(u).astype(int), np.floor(v).astype(int)
    tu, tv = u - u0, v - v0
    tu, tv = tu * tu * (3 - 2 * tu), tv * tv * (3 - 2 * tv)
    if wrap_u:
        u0 = u0 % wrap_u
    if wrap_v:
        v0 = v0 % wrap_v
    u1, v1 = u0 + 1, v0 + 1
    a = g[v0, u0] * (1 - tu) + g[v0, u1] * tu
    b = g[v1, u0] * (1 - tu) + g[v1, u1] * tu
    return a * (1 - tv) + b * tv


def fbm_at(u, v, seed, octaves=4, wrap_u=0, wrap_v=0):
    n, amp, tot = 0.0, 1.0, 0.0
    for o in range(octaves):
        n = n + lattice(u * (1 << o), v * (1 << o), seed + 31 * o, wrap_u << o if wrap_u else 0, wrap_v << o if wrap_v else 0) * amp
        tot += amp
        amp *= 0.5
    return n / tot


def fbm(w, h, cw, ch, seed, octaves=4, tile_x=True, tile_y=True):
    xx, yy = grid(w, h)
    return fbm_at(xx / w * cw, yy / h * ch, seed, octaves, cw if tile_x else 0, ch if tile_y else 0)


def blur_mask(mask, radius):
    im = Image.fromarray((np.clip(mask, 0, 1) * 255).astype(np.uint8), "L").filter(ImageFilter.GaussianBlur(radius))
    return np.asarray(im, np.float32) / 255.0


def down(im, size):
    return im.resize(size, Image.LANCZOS)


def mask_of(im):
    return np.asarray(im, np.float32) / 255.0


def mix(a, b, t):
    return a + (b - a) * t[..., None]


def grow_veins(rng, roots, step, curv, branch, max_depth, shrink, min_w, inside, child_len=(0.30, 0.60), child_w=0.62, spread=(0.45, 1.05)):
    """Random-walk vein growth. roots = [(x, y, angle, length, width)] in px; returns [(x0, y0, x1, y1, w)]; 'inside(x, y)' bounds the walk."""
    segs = []
    stack = [(x, y, a, ln, w, 0) for (x, y, a, ln, w) in roots]
    while stack:
        x, y, a, ln, w, d = stack.pop()
        n = max(1, int(ln / step))
        turn = 0.0
        for i in range(n):
            turn = turn * 0.78 + rng.normal(0.0, curv)
            a += turn * 0.5
            nx, ny = x + math.cos(a) * step, y + math.sin(a) * step
            if not inside(nx, ny):
                break
            segs.append((x, y, nx, ny, w))
            if d < max_depth and rng.random() < branch:
                side = 1 if rng.random() < 0.5 else -1
                stack.append((nx, ny, a + side * rng.uniform(*spread), ln * rng.uniform(*child_len) * (1 - 0.5 * i / n) + step * 3, w * child_w, d + 1))
            x, y = nx, ny
            w = max(min_w, w * shrink)
    return segs


def draw_segments(size, segs, scale=SS, wrap_w=0):
    """Rasterise vein segments (px at the final size) into a float mask 0..1 at the final size (supersampled, round joints)."""
    im = Image.new("L", (size[0] * scale, size[1] * scale), 0)
    d = ImageDraw.Draw(im)
    shifts = (0, -wrap_w, wrap_w) if wrap_w else (0,)
    for (x0, y0, x1, y1, w) in segs:
        wp = max(1.0, w * scale)
        for sx in shifts:
            ax, ay, bx, by = (x0 + sx) * scale, y0 * scale, (x1 + sx) * scale, y1 * scale
            d.line([(ax, ay), (bx, by)], fill=255, width=int(round(wp)))
            if wp > 2.4:
                rr = wp / 2
                d.ellipse([bx - rr, by - rr, bx + rr, by + rr], fill=255)
    return mask_of(down(im, size))


# ---------------------------------------------------------------------------------------------------- the eyeball
def sclera_arrays(size):
    """Wet cream-pink eyeball seen head-on with red capillaries and a darker pink limb. Returns rgb (h, w, 3) 0..255 and alpha 0..1."""
    xx, yy = grid(size, size)
    c = (size - 1) / 2
    nx, ny = (xx - c) / c, (yy - c) / c
    r = np.sqrt(nx ** 2 + ny ** 2)
    z = np.sqrt(np.clip(1 - r ** 2, 0, 1))
    light = np.array([-0.42, -0.52, 0.74], np.float32)
    light /= np.linalg.norm(light)
    diff = np.clip(nx * light[0] + ny * light[1] + z * light[2], 0, 1)
    shade = 0.60 + 0.40 * diff ** 0.8
    cream = np.array([247, 234, 224], np.float32)
    pink = np.array([228, 152, 154], np.float32)
    maroon = np.array([122, 40, 58], np.float32)
    col = cream[None, None, :] + (pink - cream)[None, None, :] * np.clip((1 - z) ** 1.5 * 0.95, 0, 1)[..., None]
    mott = fbm(size, size, 5, 5, 11, 4)
    yellow = fbm(size, size, 3, 3, 23, 3)
    col = col * (1 + (mott - 0.5) * 0.12)[..., None]
    col = col + np.array([8, -6, -20], np.float32)[None, None, :] * (yellow - 0.5)[..., None]
    col = col * shade[..., None]
    col = col + (maroon - col) * np.clip((1 - z) ** 3.0 * 0.62, 0, 1)[..., None]
    # capillaries: thin red vessels creeping in from the limb, a few fat ones, hair-fine branches
    rng = np.random.default_rng(1101)
    roots = []
    for k in range(34):
        a = rng.uniform(0, 2 * math.pi)
        rr = c * rng.uniform(0.80, 0.97)
        wd = rng.uniform(1.4, 3.0) * size / 256
        roots.append((c + math.cos(a) * rr, c + math.sin(a) * rr, a + math.pi + rng.normal(0, 0.28), c * rng.uniform(0.30, 0.78), wd))
    inside = lambda x, y: (x - c) ** 2 + (y - c) ** 2 < (c * 0.985) ** 2
    segs = grow_veins(rng, roots, 3.0 * size / 256, 0.20, 0.055, 4, 0.985, 0.55 * size / 256, inside)
    m = draw_segments((size, size), segs)
    halo = blur_mask(m, 2.4 * size / 256)
    vein_col = np.array([188, 28, 46], np.float32)
    deep_col = np.array([120, 14, 34], np.float32)
    col = mix(col, np.array([232, 112, 120], np.float32)[None, None, :] * np.ones_like(col), halo * 0.38)
    col = mix(col, vein_col[None, None, :] * np.ones_like(col), np.clip(m * 0.92, 0, 1))
    col = mix(col, deep_col[None, None, :] * np.ones_like(col), np.clip((m - 0.8) * 2.0, 0, 1) * 0.5)
    # wet sheen: a soft window highlight and a faint rim light low right
    sheen = np.exp(-(((nx + 0.40) / 0.22) ** 2 + ((ny + 0.44) / 0.15) ** 2)) * 0.30
    rim = np.exp(-((r - 0.90) / 0.05) ** 2) * smooth(0.15, 0.75, nx * 0.6 + ny * 0.8) * 0.18
    col = col + (sheen + rim)[..., None] * 255 * 0.55
    alpha = smooth(1.0, 0.972, r)
    return np.clip(col, 0, 255), alpha


def sclera(size=256):
    rgb, a = sclera_arrays(size)
    save(np.dstack([rgb, a * 255]), "eyeball_sclera")


def iris_arrays(size):
    """Fibrous iris in grey-scale (the vertex colour paints it): dark limbal ring, radial fibres, a jagged bright collarette,
    crypts, a dark core. Returns value 0..1 and alpha 0..1."""
    xx, yy = grid(size, size)
    c = (size - 1) / 2
    nx, ny = (xx - c) / c, (yy - c) / c
    r = np.sqrt(nx ** 2 + ny ** 2)
    th = np.arctan2(ny, nx)
    u = (th + math.pi) / (2 * math.pi)
    fib = fbm_at(u * 72, r * 3.0, 5, 3, wrap_u=72)
    fine = fbm_at(u * 190, r * 5.0, 9, 2, wrap_u=190)
    lumpy = fbm_at(u * 14, r * 2.0, 21, 3, wrap_u=14)
    v = 0.34 + 0.56 * fib + 0.20 * (fine - 0.5) + 0.16 * (lumpy - 0.5)
    ring_r = 0.37 + 0.035 * (lumpy - 0.5) * 2 + 0.02 * np.sin(th * 11)
    collar = np.exp(-((r - ring_r) / 0.032) ** 2)
    v = v + 0.55 * collar
    v = v * (0.72 + 0.60 * smooth(0.95, 0.20, r))                      # brighter towards the pupil
    crypt = fbm_at(u * 30, r * 9.0, 41, 2, wrap_u=30)
    v = v * (1 - 0.55 * smooth(0.60, 0.72, crypt) * smooth(0.40, 0.55, r) * smooth(0.90, 0.72, r))
    v = v * (1 - 0.82 * smooth(0.80, 0.94, r))                         # the dark limbal ring
    v = v + 0.35 * np.exp(-((r - 0.205) / 0.025) ** 2)               # bright rim round the pupil
    v = np.where(r < 0.17, 0.04, v)
    v = v * (0.84 + 0.16 * smooth(-0.9, 0.35, ny))                     # upper lid shadow
    alpha = smooth(1.0, 0.975, r)
    return np.clip(v, 0, 1), alpha


def iris(size=256):
    v, a = iris_arrays(size)
    g = v * 255
    save(np.dstack([g, g, g, a * 255]), "eyeball_iris")


def pupil_round(size=128):
    xx, yy = grid(size, size)
    c = (size - 1) / 2
    r = np.sqrt(((xx - c) / c) ** 2 + ((yy - c) / c) ** 2)
    a = smooth(1.0, 0.80, r)
    z = np.zeros_like(a)
    save(np.dstack([z, z, z, a * 255]), "eyeball_pupil")


def pupil_slit(size=128):
    """A vertical pointed lens with a soft black edge (the predator's pupil)."""
    xx, yy = grid(size, size)
    c = (size - 1) / 2
    nx, ny = (xx - c) / c, (yy - c) / c
    w = 0.40 * np.clip(1 - np.abs(ny) ** 1.7, 0, 1) ** 0.9
    d = (np.abs(nx) - w) / 0.12
    a = np.clip(1 - d, 0, 1) * smooth(1.0, 0.94, np.abs(ny))
    z = np.zeros_like(a)
    save(np.dstack([z, z, z, a * 255]), "eyeball_slit")


def gloss(size=128):
    """The wet glint: a rounded window highlight, a small round one, a thin crescent of rim light. White, drawn additively."""
    xx, yy = grid(size, size)
    c = (size - 1) / 2
    nx, ny = (xx - c) / c, (yy - c) / c
    r = np.sqrt(nx ** 2 + ny ** 2)
    ca, sa = math.cos(-0.45), math.sin(-0.45)
    px, py = (nx + 0.36) * ca - (ny + 0.40) * sa, (nx + 0.36) * sa + (ny + 0.40) * ca
    box = np.clip(1 - (np.maximum(np.abs(px) / 0.20, np.abs(py) / 0.11) ** 4), 0, 1)
    box = np.clip(box * 1.4, 0, 1) ** 0.8
    soft = np.exp(-((px / 0.34) ** 2 + (py / 0.2) ** 2)) * 0.35
    dot = np.exp(-(((nx - 0.34) / 0.075) ** 2 + ((ny - 0.30) / 0.075) ** 2)) * 0.8
    ang = np.arctan2(ny, nx)
    crescent = np.exp(-((r - 0.90) / 0.028) ** 2) * smooth(0.25, 1.1, np.cos(ang - 0.95)) * 0.65
    a = np.clip(box + soft + dot + crescent, 0, 1) * smooth(1.0, 0.95, r)
    o = np.full_like(a, 255)
    save(np.dstack([o, o, o, a * 255]), "eyeball_gloss")


def corona(size=256):
    """A hollow soft ring of light with faint rays, for glowing round an eyeball without washing it out. White, additive."""
    xx, yy = grid(size, size)
    c = (size - 1) / 2
    nx, ny = (xx - c) / c, (yy - c) / c
    r = np.sqrt(nx ** 2 + ny ** 2)
    th = np.arctan2(ny, nx)
    u = (th + math.pi) / (2 * math.pi)
    ring = np.exp(-((r - 0.52) / 0.17) ** 2)
    rays = fbm_at(u * 40, r * 1.2, 77, 3, wrap_u=40)
    rays = np.clip((rays - 0.42) * 2.4, 0, 1) ** 1.5
    spikes = rays * np.exp(-(r - 0.35) ** 2 / 0.16) * 0.55
    a = (ring * 0.85 + spikes) * smooth(0.14, 0.44, r) * smooth(1.0, 0.72, r)
    o = np.full_like(a, 255)
    save(np.dstack([o, o, o, np.clip(a, 0, 1) * 255]), "eyeball_corona")


# ---------------------------------------------------------------------------------------------------- the lids
def lid(w=256, h=128):
    """Fleshy eyelids round an almond opening (transparent, so the eyeball shows through): wet pink margin, deep creases, wrinkles,
    a caruncle in the inner corner, ragged torn edge so it floats like a wound in the air. Baked colour."""
    xx, yy = grid(w, h)
    x = (xx - (w - 1) / 2) / ((w - 1) / 2)
    y = (yy - (h - 1) / 2) / ((h - 1) / 2)
    half = 0.72
    xa = x / half
    prof = np.clip(1 - np.abs(xa) ** 1.7, 0, 1) ** 0.85
    up_ap, lo_ap = 0.47 * prof, 0.31 * prof
    dy_up, dy_lo = (-y) - up_ap, y - lo_ap
    d_vert = np.maximum(dy_up, dy_lo)
    tipx = np.clip(np.abs(x) - half, 0, None)
    d = np.where(np.abs(x) <= half, d_vert, np.sqrt(tipx ** 2 + y ** 2))
    noise = fbm(w, h, 8, 4, 31, 4)
    fine = fbm(w, h, 32, 16, 37, 3)
    hole = smooth(-0.012, 0.014, d)
    reach = 0.42 * (1 - 0.30 * x ** 2) * (0.85 + 0.30 * noise)
    outer = 1 - smooth(reach * 0.52, reach, d + 0.05 * (noise - 0.5))
    edge = smooth(1.0, 0.88, np.abs(x)) * smooth(1.0, 0.82, np.abs(y))
    t = smooth(0.0, 0.40, d)
    flesh = np.array([168, 58, 76], np.float32)
    deep = np.array([92, 26, 44], np.float32)
    col = flesh[None, None, :] + (deep - flesh)[None, None, :] * t[..., None]
    col = col * (0.92 + 0.16 * fine)[..., None]
    ridge = (1 - np.abs(2 * fbm(w, h, 12, 6, 53, 3) - 1)) ** 7
    col = col * (1 - 0.34 * ridge)[..., None]
    crease_up = np.exp(-((dy_up - 0.13 - 0.04 * (noise - 0.5)) / 0.013) ** 2) * (y < 0) * smooth(0.95, 0.55, np.abs(xa))
    crease_lo = np.exp(-((dy_lo - 0.11 - 0.03 * (noise - 0.5)) / 0.011) ** 2) * (y > 0) * smooth(0.9, 0.5, np.abs(xa)) * 0.6
    col = mix(col, np.array([52, 12, 26], np.float32)[None, None, :] * np.ones_like(col), np.clip(crease_up * 0.85 + crease_lo, 0, 0.9))
    margin = np.exp(-(np.clip(d, 0, None) / 0.026) ** 2) * (d > -0.02)
    col = mix(col, np.array([238, 150, 160], np.float32)[None, None, :] * np.ones_like(col), np.clip(margin * 0.9, 0, 1))
    wet = np.exp(-(((d - 0.012) / 0.012) ** 2)) * smooth(0.2, 0.9, -y + 0.3 * np.sin(xa * 3)) * 0.35
    col = col + wet[..., None] * 255 * 0.6
    cx, cy = -0.665, 0.02
    carun = np.exp(-(((x - cx) / 0.075) ** 2 + ((y - cy) / 0.068) ** 2))
    col = mix(col, np.array([212, 92, 106], np.float32)[None, None, :] * np.ones_like(col), np.clip(carun * 1.6, 0, 1) * (hole > 0.5))
    col = col + (np.exp(-(((x - cx + 0.02) / 0.02) ** 2 + ((y - cy + 0.02) / 0.02) ** 2)) * 90)[..., None]
    # veins on the lids
    rng = np.random.default_rng(1303)
    roots = []
    for k in range(16):
        ax = rng.uniform(-0.78, 0.78)
        top = rng.random() < 0.6
        roots.append(((ax + 1) * (w - 1) / 2, (h - 1) / 2 + (-1 if top else 1) * rng.uniform(0.34, 0.50) * (h - 1) / 2, rng.uniform(-0.5, 0.5) + (-math.pi / 2 if not top else math.pi / 2), rng.uniform(10, 28), rng.uniform(0.7, 1.4)))
    segs = grow_veins(rng, roots, 2.0, 0.25, 0.08, 2, 0.98, 0.5, lambda px, py: 2 < px < w - 2 and 2 < py < h - 2)
    vm = draw_segments((w, h), segs) * (d > 0.05) * outer
    col = mix(col, np.array([196, 40, 58], np.float32)[None, None, :] * np.ones_like(col), np.clip(vm * 0.7, 0, 1))
    alpha = hole * outer * edge
    save(np.dstack([np.clip(col, 0, 255), alpha * 255]), "eyeball_lid")


# ---------------------------------------------------------------------------------------------------- tendrils
def nerve(w=64, h=256):
    """An optic-nerve tendril: wet pink strand with red vessels spiralling round it, thick at the eyeball end (v = 0), thinning to
    a point (v = 1). Baked colour; drawn as a ribbon whose V runs along the strand."""
    xx, yy = grid(w, h)
    u = xx / (w - 1)
    v = yy / (h - 1)
    cu = 0.5 + 0.045 * np.sin(v * 9.0 + 0.6) * (0.25 + v)
    hw = 0.34 * (1 - 0.74 * v ** 0.85) + 0.035
    s = (u - cu) / hw
    inside = np.abs(s) < 1
    z = np.sqrt(np.clip(1 - s ** 2, 0, 1))
    diff = np.clip(-0.55 * s + 0.85 * z, 0, 1)
    shade = 0.50 + 0.50 * diff
    base = np.array([238, 184, 178], np.float32)
    dark = np.array([128, 48, 66], np.float32)
    col = base[None, None, :] * shade[..., None] + dark[None, None, :] * (1 - shade)[..., None] * 0.9
    n = fbm(w, h, 4, 16, 61, 3, tile_x=False)
    col = col * (0.92 + 0.16 * n)[..., None]
    vess = np.zeros_like(s)
    for k in range(3):
        ph = v * 15.0 + k * 2.1
        sk = 0.72 * np.sin(ph)
        front = np.clip(np.cos(ph) * 2.2 + 0.4, 0, 1)
        vess = np.maximum(vess, np.exp(-((s - sk) / 0.10) ** 2) * front)
    col = mix(col, np.array([172, 22, 44], np.float32)[None, None, :] * np.ones_like(col), np.clip(vess * 0.85, 0, 1) * inside)
    glint = np.exp(-((s + 0.38) / 0.10) ** 2) * (0.5 + 0.5 * np.sin(v * 31.0)) ** 2 * 0.45
    col = col + glint[..., None] * 255 * 0.6
    alpha = smooth(1.0, 0.80, np.abs(s)) * smooth(1.0, 0.86, v) * smooth(0.0, 0.025, v)
    save(np.dstack([np.clip(col, 0, 255), alpha * 255]), "eyeball_nerve")


def beam(w=32, h=128):
    """A hairline gaze beam: a hot thin core, a soft halo, and beads of brighter light along it (scroll V to make them travel). White."""
    xx, yy = grid(w, h)
    s = (xx - (w - 1) / 2) / ((w - 1) / 2)
    v = yy / h
    core = np.exp(-(s / 0.11) ** 2)
    halo = np.exp(-(s / 0.46) ** 2) * 0.38
    bead = (0.5 + 0.5 * np.cos(v * 2 * math.pi * 2 + 0.7)) ** 3
    jitter = fbm(w, h, 2, 8, 91, 2, tile_x=False)
    a = np.clip((core * (0.55 + 0.55 * bead) + halo * (0.6 + 0.4 * bead)) * (0.88 + 0.24 * jitter), 0, 1)
    o = np.full_like(a, 255)
    save(np.dstack([o, o, o, a * 255]), "eyeball_beam")


# ---------------------------------------------------------------------------------------------------- vein webs
def vein_web(size=512):
    """A capillary web spreading from the centre across a disc (ground membrane, burst veins). Grey, tinted by the vertex colour."""
    rng = np.random.default_rng(404)
    c = (size - 1) / 2
    inside = lambda x, y: (x - c) ** 2 + (y - c) ** 2 < (c * 0.975) ** 2
    roots = []
    for k in range(10):
        a = 2 * math.pi * k / 10 + rng.normal(0, 0.18)
        roots.append((c + math.cos(a) * 6, c + math.sin(a) * 6, a, c * rng.uniform(0.78, 1.0), rng.uniform(5.0, 6.8) * size / 512))
    segs = grow_veins(rng, roots, 5.0 * size / 512, 0.17, 0.085, 4, 0.9935, 0.8 * size / 512, inside, child_len=(0.38, 0.70))
    for k in range(70):                                                # capillary bits linking the trunks
        a = rng.uniform(0, 2 * math.pi)
        rr = c * math.sqrt(rng.uniform(0.04, 0.80))
        roots = [(c + math.cos(a) * rr, c + math.sin(a) * rr, rng.uniform(0, 2 * math.pi), c * rng.uniform(0.08, 0.26), rng.uniform(1.0, 2.0) * size / 512)]
        segs += grow_veins(rng, roots, 4.0 * size / 512, 0.30, 0.10, 2, 0.99, 0.6 * size / 512, inside)
    m = draw_segments((size, size), segs, scale=3)
    xx, yy = grid(size, size)
    r = np.sqrt(((xx - c) / c) ** 2 + ((yy - c) / c) ** 2)
    membrane = fbm(size, size, 4, 4, 17, 4)
    membrane = np.clip((membrane - 0.30) * 1.6, 0, 1) * 0.30
    glow = blur_mask(m, 3.0 * size / 512)
    a = np.clip(m + glow * 0.30 + membrane, 0, 1) * smooth(1.0, 0.90, r)
    g = np.clip(150 + 105 * m, 0, 255)
    save(np.dstack([g, g, g, a * 255]), "eyeball_vein_web")


def vein_band(w=512, h=64):
    """A tileable band of veins running up from the bottom edge (a wall of flesh): thick at the base, thinning and fading upward. Grey."""
    rng = np.random.default_rng(505)
    segs = []
    for k in range(15):
        x = (k + rng.uniform(0.1, 0.9)) * w / 15
        roots = [(x, h - 1, -math.pi / 2 + rng.normal(0, 0.15), h * rng.uniform(0.55, 1.05), rng.uniform(2.6, 3.8))]
        segs += grow_veins(rng, roots, 2.5, 0.22, 0.10, 3, 0.985, 0.6, lambda px, py: -4 < py < h + 2)
    for k in range(12):                                                # cross links
        x = rng.uniform(0, w)
        y = rng.uniform(h * 0.25, h * 0.9)
        segs += grow_veins(rng, [(x, y, rng.choice([0, math.pi]) + rng.normal(0, 0.3), rng.uniform(20, 60), 1.0)], 2.5, 0.22, 0.06, 1, 0.99, 0.5, lambda px, py: 0 < py < h)
    m = draw_segments((w, h), segs, wrap_w=w)
    xx, yy = grid(w, h)
    v = 1 - yy / (h - 1)                                               # 0 at the bottom, 1 at the top
    membrane = np.clip((fbm(w, h, 8, 2, 71, 4, tile_y=False) - 0.3) * 1.6, 0, 1) * 0.32
    a = np.clip(m * 1.05 + membrane, 0, 1) * smooth(1.0, 0.45, v) * smooth(0.0, 0.05, v + 0.04)
    g = np.clip(160 + 95 * m, 0, 255)
    save(np.dstack([g, g, g, a * 255]), "eyeball_vein_band")


# ---------------------------------------------------------------------------------------------------- line-art: reticle, scan, sigil
def _lines_canvas(size):
    im = Image.new("L", (size * SS, size * SS), 0)
    return im, ImageDraw.Draw(im), size * SS / 2


def _arc_ring(d, c, r, w, start=0, end=360):
    d.arc([c - r, c - r, c + r, c + r], start, end, fill=255, width=int(round(w)))


def _poly_almond(cx, cy, hw, hu, hl, n=48):
    pts = []
    for i in range(n + 1):
        t = i / n
        x = -hw + 2 * hw * t
        s = max(0.0, 1 - abs(x / hw) ** 1.7) ** 0.85
        pts.append((cx + x, cy - hu * s))
    for i in range(n, -1, -1):
        t = i / n
        x = -hw + 2 * hw * t
        s = max(0.0, 1 - abs(x / hw) ** 1.7) ** 0.85
        pts.append((cx + x, cy + hl * s))
    return pts


def _glow_compose(im, size, glow_r=3.0, glow_k=0.55):
    m = mask_of(down(im, (size, size)))
    g = blur_mask(m, glow_r * size / 256)
    return np.clip(m + g * glow_k, 0, 1)


def reticle(size=256):
    """A lock-on reticle shaped like an eye: broken outer ring, iris scale ticks, an almond with a slit pupil, corner brackets. White."""
    im, d, c = _lines_canvas(size)
    S = size * SS / 256
    _arc_ring(d, c, c * 0.93, 3.2 * S, 8, 82)
    _arc_ring(d, c, c * 0.93, 3.2 * S, 98, 172)
    _arc_ring(d, c, c * 0.93, 3.2 * S, 188, 262)
    _arc_ring(d, c, c * 0.93, 3.2 * S, 278, 352)
    _arc_ring(d, c, c * 0.64, 1.6 * S)
    _arc_ring(d, c, c * 0.60, 1.0 * S, 20, 160)
    _arc_ring(d, c, c * 0.60, 1.0 * S, 200, 340)
    for k in range(48):
        a = math.radians(k * 7.5)
        long_ = k % 6 == 0
        r0, r1 = c * (0.66 if long_ else 0.69), c * 0.74
        d.line([(c + math.cos(a) * r0, c + math.sin(a) * r0), (c + math.cos(a) * r1, c + math.sin(a) * r1)], fill=255, width=int(round((2.4 if long_ else 1.3) * S)))
    d.line(_poly_almond(c, c, c * 0.52, c * 0.25, c * 0.21), fill=255, width=int(round(3.6 * S)), joint="curve")
    d.ellipse([c - c * 0.17, c - c * 0.17, c + c * 0.17, c + c * 0.17], outline=255, width=int(round(2.2 * S)))
    d.polygon([(c, c - c * 0.15), (c + c * 0.045, c), (c, c + c * 0.15), (c - c * 0.045, c)], fill=255)
    for sx in (-1, 1):
        for sy in (-1, 1):
            bx, by = c + sx * c * 0.80, c + sy * c * 0.80
            L = c * 0.10
            d.line([(bx, by - sy * L), (bx, by), (bx - sx * L, by)], fill=255, width=int(round(3.0 * S)), joint="curve")
    for a in (0, 90, 180, 270):
        ar = math.radians(a)
        px, py = c + math.cos(ar) * c * 0.99, c + math.sin(ar) * c * 0.99
        tx, ty = -math.sin(ar), math.cos(ar)
        ix, iy = -math.cos(ar), -math.sin(ar)
        d.polygon([(px + tx * c * 0.035, py + ty * c * 0.035), (px - tx * c * 0.035, py - ty * c * 0.035), (px + ix * c * 0.08, py + iy * c * 0.08)], fill=255)
    a = _glow_compose(im, size)
    o = np.full_like(a, 255)
    save(np.dstack([o, o, o, a * 255]), "eyeball_reticle")


def sigil(size=512):
    """The gaze sigil that lies on the ground: notched rings, a ring of twelve small eyes, inward thorns and a big eye in the middle
    (almond, fibrous iris, slit pupil, brow with lashes). White line-art with a soft glow."""
    im, d, c = _lines_canvas(size)
    S = size * SS / 512
    _arc_ring(d, c, c * 0.985, 2.4 * S)
    _arc_ring(d, c, c * 0.935, 7.0 * S)
    for k in range(72):
        a = math.radians(k * 5)
        long_ = k % 6 == 0
        r0, r1 = c * (0.875 if long_ else 0.90), c * 0.925
        d.line([(c + math.cos(a) * r0, c + math.sin(a) * r0), (c + math.cos(a) * r1, c + math.sin(a) * r1)], fill=255, width=int(round((3.0 if long_ else 1.6) * S)))
    _arc_ring(d, c, c * 0.865, 1.8 * S)
    _arc_ring(d, c, c * 0.58, 2.2 * S)
    _arc_ring(d, c, c * 0.555, 1.2 * S, 0, 360)
    for k in range(12):                                                  # twelve small eyes between the rings
        a = math.radians(k * 30 + 15)
        ex, ey = c + math.cos(a) * c * 0.72, c + math.sin(a) * c * 0.72
        ca, sa = math.cos(a + math.pi / 2), math.sin(a + math.pi / 2)
        pts = [(ex + (px - 0) * ca - (py) * sa, ey + (px) * sa + (py) * ca) for (px, py) in _poly_almond(0, 0, c * 0.092, c * 0.05, c * 0.04, 24)]
        d.line(pts, fill=255, width=int(round(3.0 * S)), joint="curve")
        d.ellipse([ex - c * 0.030, ey - c * 0.030, ex + c * 0.030, ey + c * 0.030], fill=255)
        d.ellipse([ex - c * 0.012, ey - c * 0.012, ex + c * 0.012, ey + c * 0.012], fill=0)
        a2 = math.radians(k * 30)
        mx, my = c + math.cos(a2) * c * 0.72, c + math.sin(a2) * c * 0.72
        dd = c * 0.032
        d.polygon([(mx, my - dd), (mx + dd * 0.6, my), (mx, my + dd), (mx - dd * 0.6, my)], fill=255)
        d.line([(c + math.cos(a2) * c * 0.585, c + math.sin(a2) * c * 0.585), (c + math.cos(a2) * c * 0.64, c + math.sin(a2) * c * 0.64)], fill=255, width=int(round(2.0 * S)))
        d.line([(c + math.cos(a2) * c * 0.80, c + math.sin(a2) * c * 0.80), (c + math.cos(a2) * c * 0.86, c + math.sin(a2) * c * 0.86)], fill=255, width=int(round(2.0 * S)))
    for k in range(16):                                                  # thorns pointing in
        a = math.radians(k * 22.5)
        tipr, baser = c * 0.40, c * 0.535
        w = math.radians(5.5)
        d.polygon([(c + math.cos(a - w) * baser, c + math.sin(a - w) * baser), (c + math.cos(a + w) * baser, c + math.sin(a + w) * baser),
                   (c + math.cos(a) * tipr, c + math.sin(a) * tipr)], fill=255)
    _arc_ring(d, c, c * 0.375, 1.6 * S, 0, 360)
    d.line(_poly_almond(c, c, c * 0.34, c * 0.165, c * 0.125), fill=255, width=int(round(5.0 * S)), joint="curve")
    d.ellipse([c - c * 0.135, c - c * 0.135, c + c * 0.135, c + c * 0.135], outline=255, width=int(round(3.4 * S)))
    for k in range(28):                                                  # iris fibres
        a = math.radians(k * 360 / 28)
        d.line([(c + math.cos(a) * c * 0.055, c + math.sin(a) * c * 0.055), (c + math.cos(a) * c * 0.125, c + math.sin(a) * c * 0.125)], fill=255, width=int(round(1.4 * S)))
    d.polygon([(c, c - c * 0.115), (c + c * 0.032, c), (c, c + c * 0.115), (c - c * 0.032, c)], fill=0)
    d.arc([c - c * 0.30, c - c * 0.40, c + c * 0.30, c - c * 0.05], 205, 335, fill=255, width=int(round(3.0 * S)))        # the brow
    for k in range(-3, 4):
        a = math.radians(270 + k * 11)
        r0, r1 = c * 0.225, c * (0.290 - 0.012 * abs(k))
        d.line([(c + math.cos(a) * r0, c + math.sin(a) * r0 * 1.3 - c * 0.04), (c + math.cos(a) * r1, c + math.sin(a) * r1 * 1.3 - c * 0.04)], fill=255, width=int(round(2.0 * S)))
    a = _glow_compose(im, size, glow_r=2.4, glow_k=0.5)
    o = np.full_like(a, 255)
    save(np.dstack([o, o, o, a * 255]), "eyeball_sigil")


def scan(size=512):
    """A radar sweep over a disc: concentric rings, spokes, rim ticks, a bright leading edge with a long fading wedge behind it
    (head at angle 0, trailing clockwise on screen), and little eye-shaped blips (the magicule signatures it has found). White."""
    xx, yy = grid(size, size)
    c = (size - 1) / 2
    nx, ny = (xx - c) / c, (yy - c) / c
    r = np.sqrt(nx ** 2 + ny ** 2)
    th = np.arctan2(ny, nx)
    ang = np.mod(-th, 2 * math.pi)                                          # 0 at the head, growing opposite to the screen angle
    wedge = np.exp(-ang / 0.80) * smooth(0.05, 0.25, r) * smooth(1.0, 0.93, r) * (ang > 0.0)
    head = np.exp(-((np.minimum(ang, 2 * math.pi - ang)) / 0.012) ** 2) * smooth(0.04, 0.12, r) * smooth(1.0, 0.97, r)
    rings = np.zeros_like(r)
    for rr, wd, k in ((0.25, 0.004, 0.35), (0.5, 0.004, 0.40), (0.75, 0.004, 0.40), (0.97, 0.010, 0.85), (0.90, 0.003, 0.30)):
        rings = np.maximum(rings, np.exp(-((r - rr) / wd) ** 2) * k)
    spokes = np.zeros_like(r)
    for k in range(12):
        a = k * math.pi / 6
        dd = np.abs(np.sin(th - a)) * r
        spokes = np.maximum(spokes, np.exp(-(dd / 0.003) ** 2) * (np.cos(th - a) > 0) * smooth(0.06, 0.12, r) * smooth(0.98, 0.9, r) * 0.22)
    ticks = (np.cos(th * 90) > 0.62) * smooth(0.915, 0.925, r) * smooth(0.975, 0.965, r) * 0.55
    im = Image.new("L", (size * 2, size * 2), 0)
    dr = ImageDraw.Draw(im)
    rng = np.random.default_rng(606)
    for k in range(16):
        a = rng.uniform(0, 2 * math.pi)
        rr = math.sqrt(rng.uniform(0.04, 0.78)) * 0.95 * size
        bx, by = size + math.cos(a) * rr, size + math.sin(a) * rr
        pts = _poly_almond(bx, by, size * 0.026, size * 0.014, size * 0.011, 16)
        dr.line(pts, fill=255, width=3, joint="curve")
        dr.ellipse([bx - 3.2, by - 3.2, bx + 3.2, by + 3.2], fill=255)
    blips = mask_of(down(im, (size, size)))
    blips = np.clip(blips + blur_mask(blips, 2.5) * 0.7, 0, 1)
    haze = 0.05 * smooth(1.0, 0.2, r) * (r < 1)
    a = np.clip(wedge * 0.62 + head * 1.0 + rings + spokes + ticks + blips * 0.8 + haze, 0, 1) * smooth(1.0, 0.985, r)
    o = np.full_like(a, 255)
    save(np.dstack([o, o, o, a * 255]), "eyeball_scan")


# ---------------------------------------------------------------------------------------------------- rings and bits
def ripple(size=256):
    """An iris-wave ring: a crisp bright rim, radial fibres streaming inward behind it, a faint outer glow. White, additive."""
    xx, yy = grid(size, size)
    c = (size - 1) / 2
    nx, ny = (xx - c) / c, (yy - c) / c
    r = np.sqrt(nx ** 2 + ny ** 2)
    th = np.arctan2(ny, nx)
    u = (th + math.pi) / (2 * math.pi)
    fib = fbm_at(u * 56, r * 5.0, 14, 3, wrap_u=56)
    fib = np.clip((fib - 0.25) * 1.7, 0, 1)
    edge = np.exp(-((r - 0.90) / 0.020) ** 2)
    inner = smooth(0.40, 0.90, r) ** 2.2 * (0.25 + 0.75 * fib) * (r < 0.9)
    outer = np.exp(-(np.clip(r - 0.90, 0, None) / 0.035)) * 0.45 * (r >= 0.9)
    a = np.clip(edge * (0.75 + 0.25 * fib) + inner * 0.8 + outer, 0, 1) * smooth(1.0, 0.965, r)
    o = np.full_like(a, 255)
    save(np.dstack([o, o, o, a * 255]), "eyeball_ripple")


def warp(size=256):
    """The gaze-distortion ring for the NEGATIVE blend (which ignores alpha): the intensity lives in RGB. Grey."""
    xx, yy = grid(size, size)
    c = (size - 1) / 2
    r = np.sqrt(((xx - c) / c) ** 2 + ((yy - c) / c) ** 2)
    u = (np.arctan2((yy - c), (xx - c)) + math.pi) / (2 * math.pi)
    wob = fbm_at(u * 12, r * 1.0, 33, 3, wrap_u=12)
    v = np.exp(-((r - 0.84 - 0.03 * (wob - 0.5)) / 0.075) ** 2) * smooth(1.0, 0.92, r)
    v = np.clip(v, 0, 1)
    g = v * 255
    save(np.dstack([g, g, g, np.clip(v * 6, 0, 1) * 255]), "eyeball_warp")


def drop(w=64, h=64):
    """A glossy teardrop (a tear, a bead of blood), point up, in grey so the vertex colour tints it."""
    xx, yy = grid(w, h)
    nx = (xx - (w - 1) / 2) / ((w - 1) / 2)
    t = yy / (h - 1)                                                      # 0 top (point) .. 1 bottom
    hw = np.sin(np.clip(t, 0, 1) ** 0.62 * math.pi * 0.5) ** 1.15 * 0.78 * np.clip(1.0 - np.clip((t - 0.92) / 0.08, 0, 1) ** 2, 0, 1)
    hw = np.where(t < 0.86, hw, 0.78 * np.sqrt(np.clip(1 - ((t - 0.64) / 0.34) ** 2, 0, 1)))
    d = (np.abs(nx) - hw) / 0.12
    a = np.clip(1 - d, 0, 1) * (t < 0.99)
    s = nx / np.maximum(hw, 0.05)
    shade = 0.50 + 0.50 * np.clip(-0.6 * s + 0.8 * np.sqrt(np.clip(1 - np.minimum(s ** 2, 1), 0, 1)), 0, 1)
    hl = np.exp(-(((nx + 0.30) / 0.10) ** 2 + ((t - 0.62) / 0.10) ** 2)) * 0.9
    g = np.clip((shade * 0.80 + hl) * 255, 0, 255)
    save(np.dstack([g, g, g, a * 255]), "eyeball_drop")


def frag(size=64):
    """A fleshy vein fragment, forked, with a wet end: the debris of a burst eye. Baked colour."""
    im = Image.new("L", (size * 6, size * 6), 0)
    d = ImageDraw.Draw(im)
    k = 6
    d.line([(8 * k, 54 * k), (26 * k, 40 * k), (38 * k, 30 * k)], fill=255, width=int(5.2 * k), joint="curve")
    d.line([(38 * k, 30 * k), (48 * k, 14 * k), (56 * k, 8 * k)], fill=255, width=int(3.6 * k), joint="curve")
    d.line([(38 * k, 30 * k), (52 * k, 32 * k), (58 * k, 40 * k)], fill=255, width=int(3.2 * k), joint="curve")
    for (x, y, rr) in ((8, 54, 4.4), (56, 8, 2.8), (58, 40, 2.6)):
        d.ellipse([(x - rr) * k, (y - rr) * k, (x + rr) * k, (y + rr) * k], fill=255)
    m = mask_of(down(im, (size, size)))
    edge = blur_mask(m, 1.6)
    inner = blur_mask(m, 3.2)
    xx, yy = grid(size, size)
    col = np.zeros((size, size, 3), np.float32)
    col[:] = np.array([112, 30, 48], np.float32)
    core = np.clip((inner - 0.35) * 1.7, 0, 1)
    col = mix(col, np.array([226, 138, 146], np.float32)[None, None, :] * np.ones_like(col), core)
    col = mix(col, np.array([176, 28, 48], np.float32)[None, None, :] * np.ones_like(col), np.clip(fbm(size, size, 6, 6, 81, 3), 0, 1) * 0.35 * core)
    col = col + (np.exp(-(((xx - 20) / 3.0) ** 2 + ((yy - 46) / 2.0) ** 2)) * 150)[..., None]
    save(np.dstack([np.clip(col, 0, 255), np.clip(m * 1.0 + edge * 0.0, 0, 1) * 255]), "eyeball_frag")


def mini(size=128):
    """A small whole eyeball with its colours baked in (cream, red iris, black slit, glint), the iris set off to the right so that
    turning the quad turns the gaze: one quad per flying or floating eye."""
    big = size * 2
    rgb, a = sclera_arrays(big)
    iv, ia = iris_arrays(big)
    out = np.dstack([rgb, a * 255]).astype(np.float32)
    img = Image.fromarray(np.clip(out, 0, 255).astype(np.uint8), "RGBA")
    # coloured iris: orange-red body, bright collarette, black limbal ring
    tint = np.array([255, 78, 62], np.float32)
    ir = np.dstack([iv[..., None] * tint[None, None, :], ia * 255]).astype(np.float32)
    ir_img = Image.fromarray(np.clip(ir, 0, 255).astype(np.uint8), "RGBA")
    d = int(big * 0.50)
    ir_img = ir_img.resize((d, d), Image.LANCZOS)
    ox, oy = int(big * 0.5 + big * 0.185 - d / 2), int(big * 0.5 - d / 2)
    img.alpha_composite(ir_img, (ox, oy))
    pup = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    pd = ImageDraw.Draw(pup)
    pcx, pcy = big * 0.5 + big * 0.185, big * 0.5
    pw, ph = big * 0.040, big * 0.115
    pd.polygon([(pcx, pcy - ph), (pcx + pw, pcy), (pcx, pcy + ph), (pcx - pw, pcy)], fill=(0, 0, 0, 255))
    pup = pup.filter(ImageFilter.GaussianBlur(1.1))
    img.alpha_composite(pup)
    gl = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    gd = ImageDraw.Draw(gl)
    gx, gy = big * 0.5 + big * 0.12, big * 0.5 - big * 0.07
    gd.ellipse([gx - big * 0.032, gy - big * 0.022, gx + big * 0.032, gy + big * 0.022], fill=(255, 255, 255, 230))
    gl = gl.filter(ImageFilter.GaussianBlur(0.9))
    img.alpha_composite(gl)
    save(img.resize((size, size), Image.LANCZOS), "eyeball_mini")


if __name__ == "__main__":
    sclera()
    iris()
    pupil_round()
    pupil_slit()
    gloss()
    corona()
    lid()
    nerve()
    beam()
    vein_web()
    vein_band()
    reticle()
    sigil()
    scan()
    ripple()
    warp()
    drop()
    frag()
    mini()
