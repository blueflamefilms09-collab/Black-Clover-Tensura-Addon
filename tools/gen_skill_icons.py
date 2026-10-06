"""Black Clover skill icon generator (32x32 pixel art).

    python tools/gen_skill_icons.py

Writes, deterministic, no fonts or images needed:
  textures/skill/icons/bg_<aura|ancient>.png          background bases (dark blue glowing aura, dark brown ancient texture)
  textures/skill/icons/sym_<magic>.png                 central glowing mana-attribute symbols
  textures/skill/icons/mod_<archetype>.png             subtle spell-archetype overlays (corner badge)
  textures/skill/icons/sheet.png                       the asset sheet: row 1 backgrounds, row 2 symbols, row 3 modifiers,
                                                       row 4 example composites (32 px cells, 2 px gutter)
  textures/skill/grimoire/<water|flame|wind|earth|light|dark|anti_magic>.png
                                                       the grimoire skill icons of those magics, rebuilt in this style
  build/skill_icons/sheet_preview.png                  the sheet at 6x with labels, for looking at (not shipped)

Style after the two references: (1) a deep-blue glowing aura behind a gold symbol, (2) an old, cracked brown textured plate
behind a red clover. Symbols are drawn at 8x and reduced, then alpha is snapped so edges stay crisp like pixel art.
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.join(os.path.dirname(__file__), "..")
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "skill")
OUT = os.path.join(TEX, "icons")
PREVIEW = os.path.join(ROOT, "build", "skill_icons")
N = 32
SS = 8
S = N * SS


def save(im, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path, optimize=True)
    print("wrote", os.path.relpath(path, ROOT), im.size)


def noise(w, h, cell, seed):
    rng = np.random.default_rng(seed)
    gw, gh = max(1, w // cell), max(1, h // cell)
    g = rng.random((gh + 1, gw + 1)).astype(np.float32)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    fx, fy = xx / w * gw, yy / h * gh
    x0, y0 = np.floor(fx).astype(int), np.floor(fy).astype(int)
    tx, ty = fx - x0, fy - y0
    tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
    a = g[y0, x0] * (1 - tx) + g[y0, x0 + 1] * tx
    b = g[y0 + 1, x0] * (1 - tx) + g[y0 + 1, x0 + 1] * tx
    return a * (1 - ty) + b * ty


def fbm(w, h, cell, seed, octaves=4):
    n, amp, tot = 0, 1.0, 0
    for o in range(octaves):
        n = n + noise(w, h, max(1, cell >> o), seed + o) * amp
        tot += amp
        amp *= 0.5
    return n / tot


def rgba(arr):
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")


def radial():
    yy, xx = np.mgrid[0:N, 0:N].astype(np.float32)
    c = (N - 1) / 2
    return np.sqrt((xx - c) ** 2 + (yy - c) ** 2) / c          # 0 centre .. ~1.41 corners


def frame(arr, outer, inner):
    """1 px outer rim + 1 px inner rim, square corners trimmed for a plate look."""
    for i in range(N):
        for (x, y) in ((i, 0), (i, N - 1), (0, i), (N - 1, i)):
            arr[y, x, :3] = outer
        for (x, y) in ((i, 1), (i, N - 2), (1, i), (N - 2, i)):
            if 1 <= i <= N - 2:
                arr[y, x, :3] = inner
    for (x, y) in ((0, 0), (N - 1, 0), (0, N - 1), (N - 1, N - 1)):
        arr[y, x, 3] = 0
    return arr


# ---------------------------------------------------------------- backgrounds
def bg_aura():
    d = radial()
    n = fbm(N, N, 8, 11)
    swirl = np.sin(np.arctan2(*np.mgrid[0:N, 0:N].astype(np.float32) - (N - 1) / 2) * 3 + d * 6) * 0.5 + 0.5
    glow = np.clip(1.0 - d * 0.85, 0, 1) ** 1.6
    ring = np.exp(-((d - 0.62) / 0.16) ** 2) * (0.6 + 0.4 * swirl)
    r = 6 + 20 * glow + 10 * ring + 8 * n
    g = 12 + 50 * glow + 60 * ring + 14 * n
    b = 34 + 120 * glow + 110 * ring + 20 * n
    arr = np.dstack([r, g, b, np.full_like(r, 255)])
    vign = np.clip(1.15 - d * 0.55, 0.35, 1)[..., None]
    arr[..., :3] *= vign
    return frame(arr, (14, 18, 46), (196, 160, 72))


def bg_ancient():
    d = radial()
    n = fbm(N, N, 8, 21)
    grain = fbm(N, N, 2, 37, 2)
    base = 0.55 + 0.35 * n + 0.15 * (grain - 0.5)
    r, g, b = 70 * base, 46 * base, 26 * base
    arr = np.dstack([r, g, b, np.full_like(r, 255)])
    # cracks: a few random-walk dark lines
    rng = np.random.default_rng(5)
    for k in range(4):
        x, y = rng.integers(3, N - 3), rng.integers(3, N - 3)
        for step in range(rng.integers(6, 12)):
            if 2 <= x < N - 2 and 2 <= y < N - 2:
                arr[y, x, :3] *= 0.55
            x += rng.integers(-1, 2)
            y += rng.integers(0, 2) if k % 2 else rng.integers(-1, 1)
    # worn lighter centre, dark burnt edges
    arr[..., :3] *= np.clip(1.25 - d * 0.6, 0.45, 1.15)[..., None]
    return frame(arr, (28, 18, 10), (122, 88, 46))


# ---------------------------------------------------------------- symbols (drawn at 8x, white mask, then coloured + glow)
def canvas():
    return Image.new("L", (S, S), 0)


def pts(seq, cx=S / 2, cy=S / 2, scale=SS):
    return [(cx + x * scale, cy + y * scale) for x, y in seq]


def m_water():
    """Droplet: a round bottom and two tangent sides meeting in a sharp tip."""
    im = canvas(); d = ImageDraw.Draw(im)
    r, cy, tip = 5.6, 2.6, -9.0
    k = math.asin(r / (cy - tip))
    seq = [(0, tip)] + [(r * math.cos(a), cy + r * math.sin(a)) for a in np.linspace(-k, math.pi + k, 33)]
    d.polygon(pts(seq), fill=255)
    d.ellipse(pts([(-3.2, 1.2), (-1.6, 3.8)]), fill=150)   # highlight
    return im


def m_fire():
    im = canvas(); d = ImageDraw.Draw(im)
    outer = [(0, -9.5), (2.2, -5), (5.2, -6.2), (6.6, -1), (6, 4), (3, 7.6), (-3, 7.6), (-6, 4), (-6.4, -1.5), (-4.2, -4.4), (-3, -2), (-1.4, -6.5)]
    d.polygon(pts(outer), fill=255)
    inner = [(0, -3), (1.6, 0), (3, 2.5), (2, 6), (-2, 6), (-3, 2.5), (-1.6, 0)]
    d.polygon(pts(inner), fill=150)
    return im


def m_wind():
    """Three gust lines, each ending in a curl."""
    im = canvas(); d = ImageDraw.Draw(im)
    w = int(1.5 * SS)
    for k, (y, length, r) in enumerate([(-4.5, 13, 3.0), (0.5, 15, 2.6), (5.5, 10, 2.2)]):
        x0 = -8 + k * 1.2
        x1 = x0 + length - r
        d.line(pts([(x0, y), (x1, y)]), fill=255, width=w)
        box = pts([(x1 - r, y - 2 * r), (x1 + r, y)])
        d.arc([box[0], box[1]], start=90, end=360, fill=255, width=w)
    return im


def m_earth():
    im = canvas(); d = ImageDraw.Draw(im)
    d.polygon(pts([(-8.5, 6.5), (-3, -3.5), (-0.5, 0), (2.5, -7.5), (8.5, 6.5)]), fill=255)
    d.polygon(pts([(2.5, -7.5), (4.4, -3.2), (3.2, -3.8), (2.2, -2.2), (1.2, -4.2)]), fill=150)   # snow cap / facet
    d.line(pts([(-8.5, 6.5), (8.5, 6.5)]), fill=255, width=int(1.2 * SS))
    return im


def m_light():
    im = canvas(); d = ImageDraw.Draw(im)
    d.ellipse(pts([(-4, -4), (4, 4)]), fill=255)
    for i in range(8):
        a = i * math.pi / 4
        L = 9.2 if i % 2 == 0 else 7
        c, s = math.cos(a), math.sin(a)
        d.polygon(pts([(c * 5 - s * 1.2, s * 5 + c * 1.2), (c * L, s * L), (c * 5 + s * 1.2, s * 5 - c * 1.2)]), fill=255)
    return im


def m_dark():
    im = canvas(); d = ImageDraw.Draw(im)
    d.ellipse(pts([(-7.5, -7.5), (7.5, 7.5)]), fill=255)
    d.ellipse(pts([(-3.5, -9), (10, 4.5)]), fill=0)                # crescent
    for (x, y) in ((4.6, 4.8), (6.4, -6.8)):
        d.ellipse(pts([(x - 0.9, y - 0.9), (x + 0.9, y + 0.9)]), fill=200)
    return im


def m_light_dark():
    im = canvas(); d = ImageDraw.Draw(im)
    d.ellipse(pts([(-7.5, -7.5), (7.5, 7.5)]), fill=90)
    d.pieslice(pts([(-7.5, -7.5), (7.5, 7.5)]), start=90, end=270, fill=255)
    d.ellipse(pts([(-3.75, -7.5), (3.75, 0)]), fill=255)
    d.ellipse(pts([(-3.75, 0), (3.75, 7.5)]), fill=90)
    return im


def heart(d, cx, cy, s, ang, fill):
    seq = []
    for i in range(40):
        t = i / 40 * 2 * math.pi
        x = 16 * math.sin(t) ** 3
        y = -(13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t))
        x, y = x / 17 * s, (y / 17 + 0.3) * s
        c, sn = math.cos(ang), math.sin(ang)
        seq.append((cx + x * c - y * sn, cy + x * sn + y * c))
    d.polygon(pts(seq), fill=fill)


def m_anti_magic():
    im = canvas(); d = ImageDraw.Draw(im)
    for k in range(5):
        a = -math.pi / 2 + k * 2 * math.pi / 5
        heart(d, math.cos(a) * 5.4, math.sin(a) * 5.4 - 0.5, 5.4, a + math.pi / 2, 255)
    for k in range(5):
        a = -math.pi / 2 + (k + 0.5) * 2 * math.pi / 5
        d.line(pts([(0, -0.5), (math.cos(a) * 11, math.sin(a) * 11 - 0.5)]), fill=0, width=int(1.4 * SS))
    # cracks through the leaves
    d.line(pts([(-6, -4), (-1, 0), (-3, 4)]), fill=0, width=SS)
    d.line(pts([(5, -6), (1.5, -1)]), fill=0, width=SS)
    return im


def m_infinity():
    im = canvas(); d = ImageDraw.Draw(im)
    w = int(1.8 * SS)
    seq = [(8 * math.cos(t) / (1 + math.sin(t) ** 2), 8 * math.sin(t) * math.cos(t) / (1 + math.sin(t) ** 2))
           for t in np.linspace(0, 2 * math.pi, 80)]
    d.line(pts(seq), fill=255, width=w, joint="curve")
    return im


def m_clover():
    im = canvas(); d = ImageDraw.Draw(im)
    for k in range(3):
        a = -math.pi / 2 + k * 2 * math.pi / 3
        heart(d, math.cos(a) * 5.0, math.sin(a) * 5.0 - 1, 6.4, a + math.pi / 2, 255)
    for k in range(3):                                                  # dark seams so the leaves read apart at 32 px
        a = math.pi / 2 + k * 2 * math.pi / 3
        d.line(pts([(0, -1), (math.cos(a) * 11, math.sin(a) * 11 - 1)]), fill=0, width=int(0.9 * SS))
    d.line(pts([(0.6, 1.5), (2.6, 10)]), fill=255, width=int(1.4 * SS))
    return im


# name -> (mask fn, core colour, glow colour, dark outline)
SYMBOLS = {
    "water":      (m_water,      (120, 200, 255), (40, 120, 255), (8, 22, 60)),
    "fire":       (m_fire,       (255, 196, 64),  (255, 80, 20),  (60, 14, 4)),
    "wind":       (m_wind,       (190, 255, 210), (60, 220, 140), (8, 48, 30)),
    "earth":      (m_earth,      (214, 164, 96),  (170, 110, 40), (40, 24, 8)),
    "light":      (m_light,      (255, 248, 200), (255, 220, 90), (70, 54, 10)),
    "dark":       (m_dark,       (186, 120, 255), (110, 30, 200), (16, 4, 34)),
    "light_dark": (m_light_dark, (250, 236, 200), (150, 110, 230), (20, 10, 36)),
    "anti_magic": (m_anti_magic, (24, 18, 20),    (220, 40, 50),  (230, 60, 60)),
    "infinity":   (m_infinity,   (255, 216, 110), (255, 190, 60), (60, 40, 6)),
    "clover":     (m_clover,     (232, 52, 52),   (180, 20, 20),  (40, 4, 4)),
}


def reduce_mask(mask):
    small = mask.resize((N, N), Image.LANCZOS)
    a = np.asarray(small).astype(np.float32)
    return a


def symbol(name):
    fn, core, glow, outline = SYMBOLS[name]
    m = reduce_mask(fn())
    solid = (m > 70).astype(np.float32)                         # crisp pixel-art body
    shade = np.clip(m / 255.0, 0, 1)                            # inner shading from grey detail (150 = darker facet)
    body = solid[..., None] * np.array(core, np.float32) * (0.2 + 0.8 * shade)[..., None]
    # 1 px outline around the body
    sp = np.pad(solid, 1)
    ring = np.zeros_like(solid)
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        ring = np.maximum(ring, sp[1 + dy:1 + dy + N, 1 + dx:1 + dx + N])
    ring = ring * (1 - solid)
    # soft glow halo beyond the outline
    halo = np.asarray(Image.fromarray((solid * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(2.2))).astype(np.float32) / 255
    halo = np.clip(halo * 1.6, 0, 1) * (1 - solid) * (1 - ring)
    rgb = body + ring[..., None] * np.array(outline, np.float32) + halo[..., None] * np.array(glow, np.float32)
    a = np.clip(solid * 255 + ring * 235 + halo * 170, 0, 255)
    return rgba(np.dstack([rgb, a]))


# ---------------------------------------------------------------- spell archetype modifiers (bottom-right badge, subtle)
def badge(draw_fn, colour):
    big = Image.new("L", (12 * SS, 12 * SS), 0)
    draw_fn(ImageDraw.Draw(big), 12 * SS)
    m = np.asarray(big.resize((12, 12), Image.LANCZOS)).astype(np.float32)
    solid = (m > 90).astype(np.float32)
    sp = np.pad(solid, 1)
    ring = np.zeros_like(solid)
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, -1), (1, -1), (-1, 1)):
        ring = np.maximum(ring, sp[1 + dy:1 + dy + 12, 1 + dx:1 + dx + 12])
    ring = ring * (1 - solid)
    rgb = solid[..., None] * np.array(colour, np.float32) + ring[..., None] * np.array((12, 10, 14), np.float32)
    a = solid * 215 + ring * 190
    tile = np.zeros((N, N, 4), np.float32)
    tile[N - 13:N - 1, N - 13:N - 1] = np.dstack([rgb, a])
    return rgba(tile)


def d_offense(d, s):
    c = s / 2; w = s // 12
    d.ellipse([s * 0.18, s * 0.18, s * 0.82, s * 0.82], outline=255, width=w)
    for (x0, y0, x1, y1) in ((c, 0, c, s * 0.36), (c, s * 0.64, c, s), (0, c, s * 0.36, c), (s * 0.64, c, s, c)):
        d.line([x0, y0, x1, y1], fill=255, width=w)
    d.ellipse([c - w, c - w, c + w, c + w], fill=255)


def d_defense(d, s):
    d.polygon([(s * 0.12, s * 0.12), (s * 0.88, s * 0.12), (s * 0.86, s * 0.5), (s * 0.5, s * 0.92), (s * 0.14, s * 0.5)], fill=255)
    d.polygon([(s * 0.5, s * 0.24), (s * 0.74, s * 0.24), (s * 0.72, s * 0.5), (s * 0.5, s * 0.76)], fill=0)


def d_buff(d, s):
    d.polygon([(s * 0.5, s * 0.06), (s * 0.94, s * 0.5), (s * 0.66, s * 0.5), (s * 0.66, s * 0.94), (s * 0.34, s * 0.94), (s * 0.34, s * 0.5), (s * 0.06, s * 0.5)], fill=255)


def d_debuff(d, s):
    d.polygon([(s * 0.5, s * 0.94), (s * 0.94, s * 0.5), (s * 0.66, s * 0.5), (s * 0.66, s * 0.06), (s * 0.34, s * 0.06), (s * 0.34, s * 0.5), (s * 0.06, s * 0.5)], fill=255)


MODIFIERS = {
    "offense": (d_offense, (255, 96, 80)),
    "defense": (d_defense, (150, 200, 255)),
    "buff":    (d_buff,    (110, 240, 120)),
    "debuff":  (d_debuff,  (196, 120, 255)),
}

# grimoire skill icons rebuilt in this style: file name -> (background, symbol)
GRIMOIRE_ICONS = {
    "water": ("ancient", "water"),
    "flame": ("ancient", "fire"),
    "wind": ("ancient", "wind"),
    "earth": ("ancient", "earth"),
    "light": ("aura", "light"),
    "dark": ("aura", "dark"),
    "anti_magic": ("ancient", "anti_magic"),
}


def compose(*layers):
    out = layers[0].copy()
    for l in layers[1:]:
        out = Image.alpha_composite(out, l)
    return out


def main():
    bgs = {"aura": rgba(bg_aura()), "ancient": rgba(bg_ancient())}
    syms = {k: symbol(k) for k in SYMBOLS}
    mods = {k: badge(*v) for k, v in MODIFIERS.items()}
    for k, im in bgs.items():
        save(im, os.path.join(OUT, "bg_" + k + ".png"))
    for k, im in syms.items():
        save(im, os.path.join(OUT, "sym_" + k + ".png"))
    for k, im in mods.items():
        save(im, os.path.join(OUT, "mod_" + k + ".png"))
    for name, (bg, sym) in GRIMOIRE_ICONS.items():
        save(compose(bgs[bg], syms[sym]), os.path.join(TEX, "grimoire", name + ".png"))

    examples = [compose(bgs["aura"], syms["infinity"]), compose(bgs["ancient"], syms["clover"]),
                compose(bgs["ancient"], syms["fire"], mods["offense"]), compose(bgs["ancient"], syms["water"], mods["defense"]),
                compose(bgs["aura"], syms["light"], mods["buff"]), compose(bgs["aura"], syms["dark"], mods["debuff"]),
                compose(bgs["ancient"], syms["anti_magic"], mods["offense"]), compose(bgs["ancient"], syms["earth"], mods["defense"])]
    rows = [list(bgs.items()), list(syms.items()), list(mods.items()), [("", e) for e in examples]]
    cols = max(len(r) for r in rows)
    G = 2
    sheet = Image.new("RGBA", (cols * (N + G) + G, len(rows) * (N + G) + G), (0, 0, 0, 0))
    for ry, row in enumerate(rows):
        for cx, (_, im) in enumerate(row):
            sheet.alpha_composite(im, (G + cx * (N + G), G + ry * (N + G)))
    save(sheet, os.path.join(OUT, "sheet.png"))

    # labelled preview for humans (not shipped)
    Z = 6
    cell = N * Z
    lab = 14
    pv = Image.new("RGBA", (cols * (cell + 8) + 8, len(rows) * (cell + lab + 10) + 8), (24, 24, 30, 255))
    dr = ImageDraw.Draw(pv)
    font = ImageFont.load_default()
    names = [[k for k, _ in rows[0]], [k for k, _ in rows[1]], [k for k, _ in rows[2]],
             ["aura+infinity", "ancient+clover", "fire offense", "water defense", "light buff", "dark debuff", "anti offense", "earth defense"]]
    checker = Image.new("RGBA", (cell, cell), (60, 60, 70, 255))
    for ry, row in enumerate(rows):
        for cx, (_, im) in enumerate(row):
            x, y = 8 + cx * (cell + 8), 8 + ry * (cell + lab + 10)
            pv.alpha_composite(checker, (x, y))
            pv.alpha_composite(im.resize((cell, cell), Image.NEAREST), (x, y))
            dr.text((x, y + cell + 2), names[ry][cx], fill=(230, 230, 230, 255), font=font)
    save(pv, os.path.join(PREVIEW, "sheet_preview.png"))


if __name__ == "__main__":
    main()
