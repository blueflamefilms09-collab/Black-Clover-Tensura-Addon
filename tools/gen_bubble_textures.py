"""Generates the Bubble Magic VFX textures (BubbleLayer).

Look reference (Black Clover anime, Bubble Refresher): a giant clam shell heaped with soft white foam made of countless round bubbles,
cel-shaded: flat white discs, a flat blue-grey crescent shadow on the lower side, a thin dark-blue linework outline, a pale highlight;
small clear soap bubbles with a rim line and a glint float around it.

Deterministic (fixed seeds). Output: src/main/resources/assets/nusmp/textures/particle/bubble_*.png

    python3 -B tools/gen_bubble_textures.py
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 4
LINE = (58, 92, 148)       # linework
SHADE = (176, 200, 232)    # cel shadow
SHADE2 = (150, 178, 220)   # deeper shadow
WHITE = (255, 255, 255)


def save(img, name):
    os.makedirs(OUT, exist_ok=True)
    img.save(os.path.join(OUT, name + ".png"))


def down(img, size):
    return img.resize((size, size), Image.LANCZOS)


def cel_bubble(img, cx, cy, r, rng, lw=None, shade_col=SHADE):
    """One cel-shaded foam bubble: outline, white fill, crescent shadow low-right, small highlight upper-left."""
    d = ImageDraw.Draw(img)
    lw = lw or max(1.5, r * 0.07)
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=LINE + (255,))
    ri = r - lw
    d.ellipse([cx - ri, cy - ri, cx + ri, cy + ri], fill=WHITE + (255,))
    # crescent shadow = disc minus the same disc shifted up-left, clipped to the inner disc
    m = Image.new("L", img.size, 0)
    md = ImageDraw.Draw(m)
    md.ellipse([cx - ri, cy - ri, cx + ri, cy + ri], fill=255)
    cut = Image.new("L", img.size, 0)
    cd = ImageDraw.Draw(cut)
    ox, oy = -ri * 0.26, -ri * 0.30
    rr = ri * 0.97
    cd.ellipse([cx + ox - rr, cy + oy - rr, cx + ox + rr, cy + oy + rr], fill=255)
    arr = np.minimum(np.array(m), 255 - np.array(cut))
    sh = Image.new("RGBA", img.size, shade_col + (255,))
    sh.putalpha(Image.fromarray(arr.astype(np.uint8)))
    img.alpha_composite(sh)
    d = ImageDraw.Draw(img)
    # glint
    hr = ri * 0.17
    hx, hy = cx - ri * 0.42, cy - ri * 0.44
    d.ellipse([hx - hr, hy - hr * 0.7, hx + hr, hy + hr * 0.7], fill=WHITE + (255,))


def foam_ball():
    n = 128 * SS
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    cel_bubble(img, n / 2, n / 2, n * 0.47, None)
    return down(img, 128)


def foam_cluster(seed, size=256, count=70, flat=0.72):
    """A heap of overlapping cel bubbles inside a dome / ellipse: the foam of the Bubble Refresher."""
    rng = np.random.RandomState(seed)
    n = size * SS
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    items = []
    for _ in range(count):
        a = rng.uniform(0, math.pi * 2)
        rad = math.sqrt(rng.uniform(0, 1))
        x = n / 2 + math.cos(a) * rad * n * 0.36
        y = n * 0.52 + math.sin(a) * rad * n * 0.36 * flat
        # bubbles near the rim are smaller, the middle ones are the big round ones
        r = n * (0.07 + 0.11 * (1 - rad) * rng.uniform(0.5, 1.0) + 0.02 * rng.uniform(0, 1))
        items.append((y, x, r))
    # a couple of big ones on top
    for _ in range(3):
        x = n / 2 + rng.uniform(-0.18, 0.18) * n
        y = n * 0.45 + rng.uniform(-0.1, 0.12) * n
        items.append((y, x, n * rng.uniform(0.10, 0.14)))
    items.sort(key=lambda t: t[0])  # back (higher) first, front (lower) last
    for y, x, r in items:
        cel_bubble(img, x, y, r, rng, shade_col=SHADE if rng.rand() > 0.3 else SHADE2)
    return down(img, size)


def smoothstep(a, b, x):
    t = np.clip((x - a) / (b - a), 0, 1)
    return t * t * (3 - 2 * t)


def hsv_rgb(h, s, v):
    h = (h % 1.0) * 6
    i = np.floor(h).astype(int)
    f = h - i
    p, q, t = v * (1 - s), v * (1 - s * f), v * (1 - s * (1 - f))
    r = np.choose(i % 6, [v, q, p, p, t, v])
    g = np.choose(i % 6, [t, v, v, q, p, p])
    b = np.choose(i % 6, [p, p, t, v, v, q])
    return r, g, b


def clear_bubble(size=128, seed=3):
    """Transparent soap bubble: thin outline, faint iridescent film bands near the rim, two glints. Colour (the film) is the point."""
    rng = np.random.RandomState(seed)
    y, x = np.mgrid[0:size, 0:size].astype(np.float32)
    x = (x + 0.5) / size * 2 - 1
    y = (y + 0.5) / size * 2 - 1
    r = np.sqrt(x * x + y * y)
    ang = np.arctan2(y, x)
    inside = 1 - smoothstep(0.93, 0.95, r)
    film = smoothstep(0.35, 0.95, r)
    hue = 0.55 + 0.35 * np.sin(ang * 2 + r * 6.0 + 0.8) + 0.25 * r
    cr, cg, cb = hsv_rgb(hue, 0.45 * film, 1.0)
    # film mixes toward white/pale blue so the bubble stays airy
    base = np.array([0.80, 0.92, 1.0])
    R = base[0] * (1 - film * 0.6) + cr * film * 0.6
    G = base[1] * (1 - film * 0.6) + cg * film * 0.6
    B = base[2] * (1 - film * 0.6) + cb * film * 0.6
    a = (0.06 + 0.36 * film ** 1.6) * inside
    # outline
    ol = smoothstep(0.86, 0.91, r) * (1 - smoothstep(0.945, 0.975, r))
    R = R * (1 - ol) + LINE[0] / 255 * ol
    G = G * (1 - ol) + LINE[1] / 255 * ol
    B = B * (1 - ol) + LINE[2] / 255 * ol
    a = np.maximum(a, ol * 0.95)
    # glint 1: crescent upper-left; glint 2: faint lower-right
    def glint(cx, cy, rx, ry, strength):
        d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2
        return strength * (1 - smoothstep(0.55, 1.0, d))
    g1 = glint(-0.40, -0.42, 0.20, 0.11, 1.0)
    g2 = glint(0.45, 0.48, 0.12, 0.06, 0.55)
    gl = np.clip(g1 + g2, 0, 1) * inside
    R, G, B = (np.clip(c * (1 - gl) + gl, 0, 1) for c in (R, G, B))
    a = np.clip(np.maximum(a, gl * 0.95), 0, 1)
    out = np.stack([R, G, B, a], -1)
    return Image.fromarray((out * 255).astype(np.uint8), "RGBA")


def shell():
    """Scallop / clam shell seen from above, opened, fanned ribs and a hinge; the cradle of the Bubble Refresher. White-grey so tint works."""
    n = 256 * SS
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = n / 2, n * 0.62
    R = n * 0.42
    ribs = 13
    # scalloped outer edge polygon: lobes
    pts = []
    steps = 360
    for i in range(steps + 1):
        a = math.pi + math.pi * i / steps   # upper half fan (screen up = -y)
        lobe = 1 + 0.045 * abs(math.sin(a * ribs / 2 * 1.0 * 1.0 - math.pi * ribs / 2))
        rr = R * lobe
        pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr * 1.0))
    # mirror lower lip (shallow)
    low = []
    for i in range(steps + 1):
        a = math.pi * i / steps
        rr = R * (0.30 + 0.0 * i)
        low.append((cx + math.cos(a) * R * 0.98, cy + math.sin(a) * R * 0.30))
    poly = pts + low
    d.polygon(poly, fill=LINE + (255,))
    # inner fill, inset
    inner = [(cx + (px - cx) * 0.965, cy + (py - cy) * 0.94) for px, py in poly]
    d.polygon(inner, fill=(236, 240, 248, 255))
    # shading gradient on the inner face: darker toward the lower lip
    arr = np.array(img).astype(np.float32)
    yy = np.mgrid[0:n, 0:n][0].astype(np.float32)
    t = np.clip((yy - cy) / (R * 0.30), 0, 1)
    inner_mask = (arr[..., 0] > 200)
    for c, v in enumerate((60, 48, 28)):
        arr[..., c] = np.where(inner_mask, arr[..., c] - v * t, arr[..., c])
    img = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")
    d = ImageDraw.Draw(img)
    # radial ribs from the hinge
    hx, hy = cx, cy + R * 0.18
    for i in range(ribs + 1):
        a = math.pi + math.pi * (i + 0.0) / ribs
        x2, y2 = cx + math.cos(a) * R * 0.95, cy + math.sin(a) * R * 0.95
        d.line([(hx, hy), (x2, y2)], fill=LINE + (255,), width=int(n * 0.0055))
    # growth rings
    for k in (0.35, 0.58, 0.8):
        d.arc([cx - R * k, cy - R * k, cx + R * k, cy + R * k], 200, 340, fill=SHADE2 + (255,), width=int(n * 0.004))
    # hinge knob
    d.ellipse([cx - R * 0.09, hy - R * 0.07, cx + R * 0.09, hy + R * 0.07], fill=LINE + (255,))
    d.ellipse([cx - R * 0.07, hy - R * 0.055, cx + R * 0.07, hy + R * 0.055], fill=(226, 232, 244, 255))
    return down(img, 256)


def bubble_band():
    """Ring band (512 x 64): a row of small outlined bubbles of mixed size on a faint film line; wraps seamlessly in U."""
    w, h = 512 * SS, 64 * SS
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    rng = np.random.RandomState(11)
    d.rectangle([0, h * 0.42, w, h * 0.58], fill=(205, 232, 255, 70))
    x = 0.0
    while x < w:
        r = h * rng.uniform(0.14, 0.40)
        cy = h * 0.5 + rng.uniform(-0.1, 0.1) * h
        for dx in (0, -w, w):  # wrap
            cb = Image.new("RGBA", (w, h), (0, 0, 0, 0))
            cel_bubble(cb, x + r + dx, cy, r, rng)
            img.alpha_composite(cb)
        x += r * 2 * rng.uniform(0.7, 1.05)
    return img.resize((512, 64), Image.LANCZOS)


def pop_burst():
    """A popping bubble: broken rim arcs flying outward, little droplets and spray lines (white, tinted by vertex colour)."""
    rng = np.random.RandomState(5)
    n = 256 * SS
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = n / 2
    # broken rim arcs
    for i in range(9):
        a0 = i * 40 + rng.uniform(-6, 6)
        sweep = rng.uniform(16, 28)
        for rad, wd, col in ((0.40, 0.020, (255, 255, 255, 235)), (0.405, 0.032, (255, 255, 255, 90))):
            R = n * rad
            d.arc([c - R, c - R, c + R, c + R], a0, a0 + sweep, fill=col, width=int(n * wd))
    # spray lines + droplets
    for i in range(22):
        a = rng.uniform(0, math.tau)
        r0 = n * rng.uniform(0.12, 0.27)
        r1 = r0 + n * rng.uniform(0.04, 0.12)
        wd = int(n * rng.uniform(0.004, 0.009))
        d.line([(c + math.cos(a) * r0, c + math.sin(a) * r0), (c + math.cos(a) * r1, c + math.sin(a) * r1)], fill=(255, 255, 255, 220), width=max(2, wd))
        rr = n * rng.uniform(0.008, 0.02)
        px, py = c + math.cos(a) * (r1 + n * 0.03), c + math.sin(a) * (r1 + n * 0.03)
        d.ellipse([px - rr, py - rr, px + rr, py + rr], fill=(255, 255, 255, 255))
    # small ring bubbles
    for i in range(8):
        a = rng.uniform(0, math.tau)
        rd = n * rng.uniform(0.28, 0.44)
        r = n * rng.uniform(0.012, 0.026)
        px, py = c + math.cos(a) * rd, c + math.sin(a) * rd
        d.ellipse([px - r, py - r, px + r, py + r], outline=(255, 255, 255, 255), width=int(n * 0.005))
    img = img.filter(ImageFilter.GaussianBlur(SS * 0.4))
    return down(img, 256)


def sparkle():
    """Four-point soap glint with soft core."""
    size = 64
    y, x = np.mgrid[0:size, 0:size].astype(np.float32)
    x = (x + 0.5) / size * 2 - 1
    y = (y + 0.5) / size * 2 - 1
    r = np.sqrt(x * x + y * y)
    core = np.exp(-(r / 0.16) ** 2)
    h = np.exp(-(np.abs(y) / 0.035)) * np.clip(1 - np.abs(x), 0, 1) ** 2
    v = np.exp(-(np.abs(x) / 0.035)) * np.clip(1 - np.abs(y), 0, 1) ** 2
    a = np.clip(core + h * 0.9 + v * 0.9, 0, 1)
    out = np.stack([np.ones_like(a), np.ones_like(a), np.ones_like(a), a], -1)
    return Image.fromarray((out * 255).astype(np.uint8), "RGBA")


def trail():
    """Vertical soapy streak (64 x 128): a soft film core with small bubbles strung along it; V scrolls along the beam."""
    rng = np.random.RandomState(21)
    w, h = 64 * SS, 128 * SS
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    dx = (x / w - 0.5) * 2
    core = np.exp(-(dx / 0.32) ** 2) * (0.35 + 0.25 * np.sin(y / h * math.tau * 3))
    arr = np.zeros((h, w, 4), np.float32)
    arr[..., :3] = 1.0
    arr[..., 3] = np.clip(core, 0, 1) * 0.7
    img = Image.fromarray((arr * 255).astype(np.uint8), "RGBA")
    for i in range(10):
        r = w * rng.uniform(0.07, 0.16)
        cx = w * (0.5 + rng.uniform(-0.2, 0.2))
        cy = h * (i + rng.uniform(0.2, 0.8)) / 10
        d = ImageDraw.Draw(img)
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(255, 255, 255, 70), outline=(255, 255, 255, 255), width=max(2, int(r * 0.22)))
        d.ellipse([cx - r * 0.5, cy - r * 0.55, cx - r * 0.2, cy - r * 0.3], fill=(255, 255, 255, 255))
    return img.resize((64, 128), Image.LANCZOS)


def halo():
    """Soft pale-blue aura disc with a faint brighter rim (the glow behind foam)."""
    size = 128
    y, x = np.mgrid[0:size, 0:size].astype(np.float32)
    r = np.sqrt(((x + 0.5) / size * 2 - 1) ** 2 + ((y + 0.5) / size * 2 - 1) ** 2)
    a = np.exp(-(r / 0.55) ** 2) * 0.8 + 0.25 * np.exp(-((r - 0.82) / 0.09) ** 2)
    a = np.clip(a * (1 - smoothstep(0.92, 1.0, r)), 0, 1)
    out = np.stack([np.ones_like(a), np.ones_like(a), np.ones_like(a), a], -1)
    return Image.fromarray((out * 255).astype(np.uint8), "RGBA")


if __name__ == "__main__":
    save(foam_ball(), "bubble_foam_ball")
    save(foam_cluster(7, 256, 40), "bubble_foam_cluster")
    save(foam_cluster(19, 256, 22, 0.95), "bubble_foam_puff")
    save(clear_bubble(), "bubble_clear")
    save(shell(), "bubble_shell")
    save(bubble_band(), "bubble_band")
    save(pop_burst(), "bubble_pop")
    save(sparkle(), "bubble_glint")
    save(trail(), "bubble_trail")
    save(halo(), "bubble_halo")
    print("bubble textures written")
