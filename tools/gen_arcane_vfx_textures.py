"""Textures for the 0.34 magic VFX (Thread, Seal, Lightning, Transmutation, Poison, Spatial, Ash, Mirror, Cotton / Food, Shadow,
Recombination). Deterministic, no fonts, no external images.

    python tools/gen_arcane_vfx_textures.py   ->  src/main/resources/assets/nusmp/textures/particle/arc_*.png

Convention (same as the rest of textures/particle): white / greyscale with alpha, so one vertex colour tints each quad.
Fire, Water, Wind, Earth and Time keep their own textures (tools/gen_element_vfx_textures.py, tools/gen_time_vfx_textures.py).
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 4


def save(im, name):
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, name + ".png"), optimize=True)
    print("wrote", name, im.size)


def grey(a, g=None):
    """Alpha array (0..1) [+ grey array 0..1] -> RGBA image."""
    a = np.clip(a, 0, 1)
    g = np.ones_like(a) if g is None else np.clip(g, 0, 1)
    arr = np.dstack([g * 255, g * 255, g * 255, a * 255]).astype(np.uint8)
    return Image.fromarray(arr, "RGBA")


def noise(w, h, cell, seed, tile=True):
    rng = np.random.default_rng(seed)
    gw, gh = max(1, w // cell), max(1, h // cell)
    g = rng.random((gh + 1, gw + 1)).astype(np.float32)
    if tile:
        g[:, -1] = g[:, 0]
        g[-1, :] = g[0, :]
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    fx, fy = xx / w * gw, yy / h * gh
    x0, y0 = np.floor(fx).astype(int), np.floor(fy).astype(int)
    tx, ty = fx - x0, fy - y0
    tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
    a = g[y0, x0] * (1 - tx) + g[y0, np.minimum(x0 + 1, gw)] * tx
    b = g[np.minimum(y0 + 1, gh), x0] * (1 - tx) + g[np.minimum(y0 + 1, gh), np.minimum(x0 + 1, gw)] * tx
    return a * (1 - ty) + b * ty


def fbm(w, h, cell, seed, octaves=4):
    n, amp, tot = 0, 1.0, 0
    for o in range(octaves):
        n = n + noise(w, h, max(1, cell >> o), seed + o) * amp
        tot += amp
        amp *= 0.5
    return n / tot


def mask_draw(w, h, fn, blur=0):
    im = Image.new("L", (w * SS, h * SS), 0)
    fn(ImageDraw.Draw(im), SS)
    im = im.resize((w, h), Image.LANCZOS)
    if blur:
        im = im.filter(ImageFilter.GaussianBlur(blur))
    return np.asarray(im).astype(np.float32) / 255


# ------------------------------------------------------------------------------------------------ thread / seal
def arc_web(size=256):
    """A spider web disc: radial spokes and a spiral of sagging threads (Arachne's Web)."""
    def draw(d, s):
        c = size * s / 2
        spokes = 14
        for k in range(spokes):
            a = 2 * math.pi * k / spokes
            d.line([(c, c), (c + math.cos(a) * c * 0.98, c + math.sin(a) * c * 0.98)], fill=255, width=int(1.6 * s))
        for ring in range(1, 10):
            r = c * ring / 10
            pts = []
            for k in range(spokes + 1):
                a = 2 * math.pi * k / spokes
                for t in np.linspace(0, 1, 6)[:-1] if k < spokes else [0]:
                    aa = a + t * 2 * math.pi / spokes
                    sag = 1 - 0.08 * math.sin(t * math.pi)
                    pts.append((c + math.cos(aa) * r * sag, c + math.sin(aa) * r * sag))
            d.line(pts, fill=230, width=int(1.3 * s))
    a = mask_draw(size, size, draw)
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    r = np.hypot(xx - size / 2, yy - size / 2) / (size / 2)
    save(grey(a * np.clip((1.05 - r) * 6, 0, 1)), "arc_web")


def arc_chain(w=256, h=64):
    """A band of interlocking chain links running along U (tileable): seal chains."""
    def draw(d, s):
        n = 6
        lw, lh = w * s / n, h * s
        for k in range(n + 1):
            x = k * lw
            if k % 2 == 0:
                d.ellipse([x - lw * 0.62, lh * 0.22, x + lw * 0.62, lh * 0.78], outline=255, width=int(5 * s))
            else:
                d.rounded_rectangle([x - lw * 0.62, lh * 0.42, x + lw * 0.62, lh * 0.58], radius=int(4 * s), fill=210)
    a = mask_draw(w, h, draw)
    shade = 0.7 + 0.3 * np.cos(np.linspace(-math.pi / 2, math.pi / 2, h))[:, None]
    save(grey(a, shade * np.ones((h, w))), "arc_chain")


def arc_rune_band(w=256, h=64):
    """A band of angular glyphs between two lines (tileable): seal barriers, trinity seal rings."""
    rng = np.random.default_rng(34)
    def draw(d, s):
        d.line([(0, 6 * s), (w * s, 6 * s)], fill=255, width=int(3 * s))
        d.line([(0, (h - 6) * s), (w * s, (h - 6) * s)], fill=255, width=int(3 * s))
        n = 10
        for k in range(n):
            x0 = (k + 0.15) * w * s / n
            gw = 0.7 * w * s / n
            for _ in range(3):
                pts = [(x0 + rng.random() * gw, (14 + rng.random() * (h - 28)) * s) for _ in range(3)]
                d.line(pts, fill=255, width=int(2.4 * s), joint="curve")
            d.ellipse([x0 + gw * 0.35, h * s / 2 - 3 * s, x0 + gw * 0.35 + 6 * s, h * s / 2 + 3 * s], fill=255)
    save(grey(mask_draw(w, h, draw)), "arc_rune_band")


# ------------------------------------------------------------------------------------------------ lightning
def arc_bolt(w=256, h=64):
    """A jagged lightning strip along U with a hot core and soft glow (tileable)."""
    rng = np.random.default_rng(7)
    pts = [(0, h / 2)]
    x = 0
    while x < w:
        x += rng.uniform(8, 20)
        pts.append((min(x, w), h / 2 + rng.uniform(-h * 0.3, h * 0.3)))
    pts[-1] = (w, h / 2)
    def core(d, s):
        d.line([(px * s, py * s) for px, py in pts], fill=255, width=int(4 * s), joint="curve")
        for i in range(2, len(pts) - 1, 3):                            # little forks
            px, py = pts[i]
            d.line([(px * s, py * s), ((px + rng.uniform(6, 14)) * s, (py + rng.uniform(-14, 14)) * s)], fill=200, width=int(2 * s))
    a = mask_draw(w, h, core)
    glow = np.asarray(Image.fromarray((a * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(6))).astype(np.float32) / 255
    save(grey(np.clip(a + glow * 0.9, 0, 1)), "arc_bolt")


# ------------------------------------------------------------------------------------------------ transmutation
def arc_alchemy(size=256):
    """A transmutation circle: double ring, inscribed triangle and square, tick marks, a small inner ring."""
    def draw(d, s):
        c, R = size * s / 2, size * s / 2 * 0.96
        lw = int(3 * s)
        d.ellipse([c - R, c - R, c + R, c + R], outline=255, width=lw)
        d.ellipse([c - R * 0.88, c - R * 0.88, c + R * 0.88, c + R * 0.88], outline=255, width=int(2 * s))
        tri = [(c + math.cos(-math.pi / 2 + k * 2 * math.pi / 3) * R * 0.86, c + math.sin(-math.pi / 2 + k * 2 * math.pi / 3) * R * 0.86) for k in range(3)]
        d.polygon(tri, outline=255, width=lw)
        sq = [(c + math.cos(math.pi / 4 + k * math.pi / 2) * R * 0.6, c + math.sin(math.pi / 4 + k * math.pi / 2) * R * 0.6) for k in range(4)]
        d.polygon(sq, outline=230, width=int(2 * s))
        d.ellipse([c - R * 0.3, c - R * 0.3, c + R * 0.3, c + R * 0.3], outline=255, width=lw)
        for k in range(48):
            a = 2 * math.pi * k / 48
            r0 = R * (0.9 if k % 4 else 0.86)
            d.line([(c + math.cos(a) * r0, c + math.sin(a) * r0), (c + math.cos(a) * R * 0.94, c + math.sin(a) * R * 0.94)], fill=255, width=int(1.5 * s))
    save(grey(mask_draw(size, size, draw)), "arc_alchemy")


# ------------------------------------------------------------------------------------------------ poison / ash / smoke
def arc_smoke(size=128, seed=11, name="arc_smoke"):
    """A soft billowing smoke blob (poison mist, ash clouds)."""
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    r = np.hypot(xx - size / 2, yy - size / 2) / (size / 2)
    n = fbm(size, size, 32, seed)
    a = np.clip((1 - r) * 1.6 - 0.25 + (n - 0.5) * 1.2, 0, 1) ** 1.3
    g = 0.75 + 0.25 * fbm(size, size, 16, seed + 9)
    save(grey(a, g), name)


def arc_curtain(w=128, h=256):
    """A hanging liquid curtain (Violett Schirm): vertical drips, wavy top, brighter streaks (U across, V down)."""
    xx = np.linspace(0, 1, w)[None, :] * np.ones((h, 1))
    yy = np.linspace(0, 1, h)[:, None] * np.ones((1, w))
    streak = fbm(w, h, 16, 3)[:, :] * 0.6 + 0.4 * np.abs(np.sin(xx * math.pi * 9 + fbm(w, h, 32, 4) * 3))
    drip = np.clip(1.0 - (yy - (0.85 + 0.12 * noise(w, 1, 8, 5)[0][None, :])) * 8, 0, 1)
    edge = np.clip(yy * 12, 0, 1)
    a = np.clip(0.35 + 0.55 * streak, 0, 1) * drip * edge
    save(grey(a, 0.75 + 0.25 * streak), "arc_curtain")


def arc_ember(size=32):
    """A tiny ash flake: an irregular grey chip with a soft edge."""
    def draw(d, s):
        c = size * s / 2
        pts = [(c + math.cos(a) * c * (0.55 + 0.35 * ((k * 37) % 7) / 7), c + math.sin(a) * c * (0.55 + 0.35 * ((k * 53) % 7) / 7))
               for k, a in enumerate(np.linspace(0, 2 * math.pi, 9)[:-1])]
        d.polygon(pts, fill=255)
    save(grey(mask_draw(size, size, draw, blur=0.6)), "arc_ember")


# ------------------------------------------------------------------------------------------------ spatial / mirror
def arc_portal(size=256):
    """A portal disc: dark swirling inside, bright ragged rim (tinted by the spell colour)."""
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    dx, dy = (xx - size / 2) / (size / 2), (yy - size / 2) / (size / 2)
    r = np.hypot(dx, dy)
    ang = np.arctan2(dy, dx)
    swirl = 0.5 + 0.5 * np.sin(ang * 5 + r * 14)
    rim = np.exp(-((r - 0.9) / 0.06) ** 2) * (0.8 + 0.2 * np.sin(ang * 23))
    inside = np.clip((0.92 - r) * 10, 0, 1)
    a = np.clip(inside * (0.55 + 0.25 * swirl) + rim, 0, 1)
    g = np.clip(0.35 + 0.35 * swirl * (1 - r) + rim, 0, 1)
    save(grey(a, g), "arc_portal")


def arc_glass(w=128, h=192):
    """A mirror pane: soft fill, a bright bevelled frame and two diagonal glints."""
    def draw(d, s):
        d.rounded_rectangle([3 * s, 3 * s, (w - 3) * s, (h - 3) * s], radius=int(10 * s), outline=255, width=int(6 * s))
        d.polygon([(w * 0.25 * s, h * 0.15 * s), (w * 0.42 * s, h * 0.15 * s), (w * 0.2 * s, h * 0.55 * s), (w * 0.1 * s, h * 0.55 * s)], fill=200)
        d.polygon([(w * 0.55 * s, h * 0.3 * s), (w * 0.62 * s, h * 0.3 * s), (w * 0.38 * s, h * 0.78 * s), (w * 0.31 * s, h * 0.78 * s)], fill=150)
    a = mask_draw(w, h, draw)
    fill = np.zeros((h, w), np.float32)
    fill[6:-6, 6:-6] = 0.32
    save(grey(np.clip(a + fill, 0, 1), 0.85 + 0.15 * a), "arc_glass")


def arc_silhouette(w=96, h=192):
    """A person-shaped silhouette (head, shoulders, body, legs): mirror doubles and shadow figures."""
    def draw(d, s):
        cx = w * s / 2
        d.ellipse([cx - 16 * s, 6 * s, cx + 16 * s, 38 * s], fill=255)
        d.rounded_rectangle([cx - 28 * s, 42 * s, cx + 28 * s, 112 * s], radius=int(10 * s), fill=255)
        d.rounded_rectangle([cx - 40 * s, 44 * s, cx - 26 * s, 108 * s], radius=int(6 * s), fill=255)
        d.rounded_rectangle([cx + 26 * s, 44 * s, cx + 40 * s, 108 * s], radius=int(6 * s), fill=255)
        d.rounded_rectangle([cx - 24 * s, 108 * s, cx - 3 * s, 186 * s], radius=int(6 * s), fill=255)
        d.rounded_rectangle([cx + 3 * s, 108 * s, cx + 24 * s, 186 * s], radius=int(6 * s), fill=255)
    save(grey(mask_draw(w, h, draw, blur=0.8)), "arc_silhouette")


# ------------------------------------------------------------------------------------------------ cotton / food
def arc_cotton(size=128):
    """A fluffy cotton puff: overlapping soft balls with a bright top."""
    rng = np.random.default_rng(5)
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    a = np.zeros((size, size), np.float32)
    g = np.zeros((size, size), np.float32)
    for _ in range(9):
        cx, cy = size / 2 + rng.uniform(-0.25, 0.25) * size, size / 2 + rng.uniform(-0.18, 0.2) * size
        r = rng.uniform(0.18, 0.28) * size
        d = np.hypot(xx - cx, yy - cy) / r
        ball = np.clip(1 - d, 0, 1) ** 0.6
        a = np.maximum(a, ball)
        g = np.maximum(g, ball * (0.75 + 0.25 * np.clip((cy - yy) / r + 0.5, 0, 1)))
    a = np.clip(a * 1.4, 0, 1)
    save(grey(a, np.clip(0.7 + g * 0.35, 0, 1)), "arc_cotton")


def arc_sheep(size=128):
    """A cotton sheep seen side-on: puffy body, dark-able face and legs left as a lighter grey (tinted together)."""
    def body(d, s):
        for (x, y, r) in ((44, 60, 24), (64, 52, 26), (84, 60, 24), (58, 72, 22), (76, 72, 22), (64, 66, 26)):
            d.ellipse([(x - r) * s, (y - r) * s, (x + r) * s, (y + r) * s], fill=255)
    def face(d, s):
        d.ellipse([92 * s, 50 * s, 116 * s, 78 * s], fill=255)
        for x in (48, 62, 78, 90):
            d.rounded_rectangle([x * s, 86 * s, (x + 7) * s, 112 * s], radius=int(3 * s), fill=255)
    b = mask_draw(size, size, body, blur=0.6)
    f = mask_draw(size, size, face, blur=0.4)
    a = np.clip(b + f, 0, 1)
    g = np.where(f > b, 0.45, 1.0) * (0.85 + 0.15 * fbm(size, size, 8, 6))
    save(grey(a, g), "arc_sheep")


def arc_maw(size=256):
    """A giant open mouth seen head-on: dark throat, a ring of teeth, rounded lips (Glutton's Banquet)."""
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    dx, dy = (xx - size / 2) / (size / 2), (yy - size / 2) / (size / 2) * 1.25
    r = np.hypot(dx, dy)
    ang = np.arctan2(dy, dx)
    lip = np.exp(-((r - 0.86) / 0.09) ** 2)
    teeth_r = 0.72 - 0.12 * np.clip(np.abs(np.sin(ang * 9)) - 0.25, 0, 1)
    teeth = ((r > teeth_r) & (r < 0.8)).astype(np.float32)
    throat = np.clip((0.75 - r) * 4, 0, 1)
    a = np.clip(throat * 0.85 + teeth + lip, 0, 1)
    g = np.clip(throat * (0.15 + 0.2 * (1 - r)) + teeth * 1.0 + lip * 0.7, 0, 1)
    save(grey(a, g), "arc_maw")


# ------------------------------------------------------------------------------------------------ shadow / recombination
def arc_hand(w=96, h=160):
    """A reaching shadow hand, fingers up, smoky at the wrist."""
    def draw(d, s):
        d.rounded_rectangle([26 * s, 70 * s, 70 * s, 150 * s], radius=int(14 * s), fill=255)        # palm + wrist
        for k, (x, top, lean) in enumerate(((20, 22, -8), (34, 8, -3), (50, 4, 2), (64, 14, 6))):
            d.line([((x + 8) * s, 80 * s), ((x + 8 + lean) * s, top * s)], fill=255, width=int(11 * s))
        d.line([(28 * s, 110 * s), (8 * s, 76 * s)], fill=255, width=int(11 * s))                   # thumb
    a = mask_draw(w, h, draw, blur=1.0)
    fade = np.clip((h - np.arange(h)[:, None]) / (h * 0.25), 0, 1)
    smoke = 0.8 + 0.2 * fbm(w, h, 16, 21)
    save(grey(a * fade * smoke), "arc_hand")


def arc_plank(size=64):
    """A wooden board face (Henry's magic house): planks with grain and a bevelled edge."""
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    plank = (yy // 16).astype(int)
    grain = 0.75 + 0.15 * np.sin(xx * 0.35 + plank * 2.1 + 3 * fbm(size, size, 16, 41)) + 0.1 * fbm(size, size, 8, 42)
    seam = (yy % 16 < 1.2).astype(np.float32)
    edge = np.minimum.reduce([xx, yy, size - 1 - xx, size - 1 - yy]) < 2
    g = np.clip(grain - seam * 0.45 - edge * 0.25, 0, 1)
    save(grey(np.ones((size, size)), g), "arc_plank")


def arc_rouge(w=128, h=160):
    """Rouge (Red Thread of Fate): a sitting cat seen from the front, pointed ears, tail curling up one side, a collar; the body
    is woven - thin diagonal thread grooves cross it - so it reads as made of thread, not clay (see docs/rouge_spec.md)."""
    def body(d, s):
        cx = w / 2
        d.ellipse([(cx - 26) * s, 18 * s, (cx + 26) * s, 64 * s], fill=255)                          # head
        d.polygon([((cx - 24) * s, 34 * s), ((cx - 22) * s, 4 * s), ((cx - 6) * s, 22 * s)], fill=255)     # ears
        d.polygon([((cx + 24) * s, 34 * s), ((cx + 22) * s, 4 * s), ((cx + 6) * s, 22 * s)], fill=255)
        d.ellipse([(cx - 30) * s, 56 * s, (cx + 30) * s, 150 * s], fill=255)                         # body
        d.rounded_rectangle([(cx - 22) * s, 100 * s, (cx - 8) * s, 156 * s], radius=int(6 * s), fill=255)   # front legs
        d.rounded_rectangle([(cx + 8) * s, 100 * s, (cx + 22) * s, 156 * s], radius=int(6 * s), fill=255)
        tail = [((cx + 28 + 18 * math.sin(t)) * s, (140 - 70 * t / math.pi) * s) for t in np.linspace(0, math.pi * 1.1, 24)]
        d.line(tail, fill=255, width=int(9 * s), joint="curve")
    def detail(d, s):
        cx = w / 2
        d.line([((cx - 16) * s, 40 * s), ((cx - 8) * s, 37 * s)], fill=255, width=int(3 * s))       # closed happy eyes
        d.line([((cx + 8) * s, 37 * s), ((cx + 16) * s, 40 * s)], fill=255, width=int(3 * s))
        d.arc([(cx - 7) * s, 44 * s, (cx + 7) * s, 54 * s], 0, 180, fill=255, width=int(2 * s))      # mouth
        d.rounded_rectangle([(cx - 20) * s, 60 * s, (cx + 20) * s, 66 * s], radius=int(3 * s), fill=255)   # collar
    a = mask_draw(w, h, body, blur=0.5)
    det = mask_draw(w, h, detail, blur=0.3)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    weave = 0.5 + 0.5 * np.sin((xx + yy) * 0.9) * np.sin((xx - yy) * 0.9)            # crossing thread grain
    shade = np.clip(0.78 + 0.22 * (1 - yy / h) - 0.12 * (weave > 0.85), 0, 1) - det * 0.45
    save(grey(a, shade), "arc_rouge")


def main():
    arc_web(); arc_chain(); arc_rune_band(); arc_bolt(); arc_alchemy()
    arc_smoke(); arc_curtain(); arc_ember(); arc_portal(); arc_glass(); arc_silhouette()
    arc_cotton(); arc_sheep(); arc_maw(); arc_hand(); arc_plank(); arc_rouge()


if __name__ == "__main__":
    main()
