"""Black Clover skill icons (0.23): three icons for every grimoire magic, after the "Black Clover Skill Icon Generator" brief.

    python tools/gen_skill_icons.py

Style (from the brief): dark fantasy anime spell icon, glossy and clean-lined, centred and tightly framed, embossed metal or stone
borders with clover runes, glowing magic in the attribute's colour, floating particles. Three variants per magic:

  1. Active / attack    textures/skill/grimoire/<magic>.png        the book's skill icon; midnight-blue glowing aura, energy
                                                                    streaks, gold embossed frame with clover-rune corners
  2. Buff / rune        textures/skill/icons/<magic>_buff.png      ancient parchment, a glowing rune circle around the symbol,
                                                                    chain links, silver stone frame
  3. Ultimate/forbidden textures/skill/icons/<magic>_ultimate.png  black void, radial burst, layered magic circles with a star,
                                                                    heavy particles, black-iron frame lit in the magic colour

Also written: bg_<aura|ancient|void>.png and sym_<magic>.png (layers), mod_<offense|defense|buff|debuff>.png (archetype badges
used on the spell card), sheet.png (every icon), build/skill_icons/sheet_preview.png (labelled, not shipped) and
docs/skill_icon_prompts.md (image-generator prompts, three per magic, if you want hand-painted versions to drop in instead).

Everything is drawn at 8x (256 px) with Pillow + numpy and reduced to 32 px. Deterministic.
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.join(os.path.dirname(__file__), "..")
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "skill")
OUT = os.path.join(TEX, "icons")
PREVIEW = os.path.join(ROOT, "build", "skill_icons")
DOCS = os.path.join(ROOT, "docs")
N = 32
SS = 8
S = N * SS          # 256 working canvas
U = SS              # one icon pixel in canvas pixels

# magic -> (display name, glow colour, body colour, prompt colour words, signature spell for the prompts)
MAGIC = {
    "flame":         ("Flame Magic", 0xFF5A1F, 0xFFB040, "blazing crimson and orange", "Calderos, a lance of roaring flame"),
    "explosion":     ("Explosion Magic", 0xFF8A2A, 0xFFD060, "bursting orange and white-hot", "Exploding Fireball"),
    "magma":         ("Magma Magic", 0xFF4A10, 0xFF8A30, "molten red and black obsidian", "a magma geyser"),
    "water":         ("Water Magic", 0x3F8CFF, 0x9AD8FF, "deep water azure", "Sea Dragon's Roar"),
    "ice":           ("Ice Magic", 0x9FE8FF, 0xE6FBFF, "frozen cyan and white", "Frost Lance"),
    "mercury":       ("Mercury Magic", 0xC8D0E0, 0xF2F4FA, "liquid silver chrome", "Silver Blade"),
    "mist":          ("Mist Magic", 0xD0DCEC, 0xF4F8FF, "pale misty blue-grey", "a veil of mist"),
    "wind":          ("Wind Magic", 0x4DFF9A, 0xC8FFDC, "emerald green", "Crescent Kamaitachi"),
    "star":          ("Star Magic", 0x7A8CFF, 0xE0E6FF, "starlit indigo and white", "a falling star"),
    "storm":         ("Storm Magic", 0x38E0D0, 0xC0FFF6, "storm teal with lightning white", "Spirit Storm"),
    "earth":         ("Earth Magic", 0xD09040, 0xE8C080, "earthen amber and stone brown", "Earth Spikes"),
    "plant":         ("Plant Magic", 0x7CE04A, 0xD0FF9A, "living leaf green", "Magic Flower Guidepost"),
    "sand":          ("Sand Magic", 0xE8C878, 0xFFF0C0, "desert gold", "a sand vortex"),
    "light":         ("Light Magic", 0xFFE680, 0xFFFBE0, "radiant gold and white", "Light Sword of Judgment"),
    "lightning":     ("Lightning Magic", 0x5AE6FF, 0xE0FCFF, "flashing lightning gold and cyan", "Thunder God's Boots"),
    "sword":         ("Sword Magic", 0xC8D2E6, 0xF4F8FF, "steel silver", "a storm of magic blades"),
    "dark":          ("Dark Magic", 0xA04CFF, 0xDCB0FF, "midnight purple", "Dark Cloaked Avidya Slash"),
    "shadow":        ("Shadow Magic", 0x8A7AD8, 0xC8C0F0, "smoky violet-grey", "a grasping shadow"),
    "poison":        ("Poison Magic", 0xB4E040, 0xE8FF9A, "toxic green", "a venom mist"),
    "spatial":       ("Spatial Magic", 0xB070FF, 0xE6D0FF, "spatial silver and purple", "Dimension Slash"),
    "mirror":        ("Mirror Magic", 0xC0F0FF, 0xF4FDFF, "prismatic silver-blue", "Reflect Refrain"),
    "gravity":       ("Gravity Magic", 0x8A4CD0, 0xD0B0FF, "crushing deep violet", "Gravity Sphere"),
    "time":          ("Time Magic", 0xE8C050, 0xFFF0C0, "antique gold and pale blue", "Chrono Stasis"),
    "sealing":       ("Sealing Magic", 0xFF5A78, 0xFFC0CC, "seal crimson and pink", "Grand Seal"),
    "reinforcement": ("Reinforcement Magic", 0xFFC040, 0xFFE8A0, "burning gold", "Heavy Infighting"),
    "beast":         ("Beast Magic", 0xFFB060, 0xFFE0B0, "wild amber", "a beast's claw strike"),
    "bone":          ("Bone Magic", 0xF0EAD0, 0xFFFDF0, "bone white and ash", "a spear of bone"),
    "blood":         ("Blood Magic", 0xE0182C, 0xFF7080, "deep blood crimson", "Crimson Lance"),
    "creation":      ("Creation Magic", 0xFFFFFF, 0xFFFFFF, "brilliant white and gold", "Creation Magic: Giant Iron Golem"),
    "copy":          ("Copy Magic", 0xA0B0D0, 0xE0E8F8, "cool slate blue", "Real Double"),
    "illusion":      ("Illusion Magic", 0xD070FF, 0xF0C8FF, "dreamlike violet", "a hall of illusions"),
    "dream":         ("Dream Magic", 0xFFA8E0, 0xFFE0F4, "soft dream pink and lilac", "Dream World of a Hundred Flowers"),
    "anti_magic":    ("Anti-Magic", 0xFF1E5A, 0x141014, "iridescent neon anti-magic black and magenta", "Black Divider, the Demon-Slayer Sword"),
    "steel":         ("Steel Magic", 0xB8C4D8, 0xEEF2FA, "gunmetal steel", "Blazing Steel Shot"),
    "thread":        ("Thread Magic", 0xFF4060, 0xFFB0C0, "scarlet thread red", "Red Thread"),
    "forbidden":     ("Forbidden Magic", 0xC01030, 0x1A0A0E, "abyssal black and blood red", "Underworld Gate"),
    # 0.34: attributes from the wiki (appended after forbidden so every existing icon keeps its seed)
    "transmutation": ("Transmutation Magic", 0x7AF0D8, 0xD8FFF4, "alchemical teal and silver", "Magic Convert"),
    "ash":           ("Ash Magic", 0xB0AAA2, 0xE8E4DE, "ash grey and ember", "Ash Absorbing Formation"),
    "cotton":        ("Cotton Magic", 0xFFE8F4, 0xFFFFFF, "soft cotton white and pink", "Sleeping Sheep Strike"),
    "recombination": ("Recombination Magic", 0xFF9A3C, 0xFFD8B0, "magic-house timber and orange", "The Raging Black Bull"),
}


# ---------------------------------------------------------------------------------------------------- helpers
def save(im, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path, optimize=True)


def rgb(c):
    return ((c >> 16) & 255, (c >> 8) & 255, c & 255)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


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


def P(seq, scale=1.0, dx=0.0, dy=0.0):
    """Glyph units (icon pixels, origin at the centre, y down) -> canvas points."""
    return [(S / 2 + (x * scale + dx) * U, S / 2 + (y * scale + dy) * U) for x, y in seq]


def box(x0, y0, x1, y1):
    a, b = P([(x0, y0), (x1, y1)])
    return [a[0], a[1], b[0], b[1]]


def L():
    return Image.new("L", (S, S), 0)


def heart(d, cx, cy, s, ang, fill):
    seq = []
    for i in range(48):
        t = i / 48 * 2 * math.pi
        x = 16 * math.sin(t) ** 3
        y = -(13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t))
        x, y = x / 17 * s, (y / 17 + 0.3) * s
        c, sn = math.cos(ang), math.sin(ang)
        seq.append((cx + x * c - y * sn, cy + x * sn + y * c))
    d.polygon(P(seq), fill=fill)


def clover_mask(leaves, size=1.0, seams=True):
    im = L(); d = ImageDraw.Draw(im)
    off = 5.0 * size if leaves == 3 else 5.4 * size
    hs = 6.4 * size if leaves == 3 else 5.4 * size
    cy = -1 * size if leaves == 3 else -0.5 * size
    for k in range(leaves):
        a = -math.pi / 2 + k * 2 * math.pi / leaves
        heart(d, math.cos(a) * off, math.sin(a) * off + cy, hs, a + math.pi / 2, 255)
    if seams:
        for k in range(leaves):
            a = -math.pi / 2 + (k + 0.5) * 2 * math.pi / leaves
            d.line(P([(0, cy), (math.cos(a) * 11 * size, math.sin(a) * 11 * size + cy)]), fill=0, width=int(1.1 * U * size))
    if leaves == 3:
        d.line(P([(0.6 * size, 1.5 * size), (2.6 * size, 10 * size)]), fill=255, width=int(1.4 * U * size))
    return im


# ---------------------------------------------------------------------------------------------------- glyphs (masks: 255 body, ~140 facet)
def g_flame(d):
    d.polygon(P([(0, -10), (2.4, -5.2), (5.6, -6.6), (7, -1), (6.4, 4.4), (3.2, 8), (-3.2, 8), (-6.4, 4.4), (-6.8, -1.6), (-4.4, -4.6), (-3.2, -2), (-1.4, -6.8)]), fill=255)
    d.polygon(P([(0, -3), (1.8, 0.2), (3.2, 2.8), (2.1, 6.3), (-2.1, 6.3), (-3.2, 2.8), (-1.8, 0.2)]), fill=140)


def g_explosion(d):
    pts = []
    for i in range(20):
        a = i * math.pi / 10 - math.pi / 2
        r = 10.2 if i % 2 == 0 else (4.6 if i % 4 == 1 else 5.6)
        pts.append((math.cos(a) * r, math.sin(a) * r))
    d.polygon(P(pts), fill=255)
    d.ellipse(box(-3.4, -3.4, 3.4, 3.4), fill=140)


def g_magma(d):
    d.polygon(P([(-9.5, 8), (-3.4, -2.4), (3.4, -2.4), (9.5, 8)]), fill=255)
    d.ellipse(box(-3.8, -8.2, 3.8, -1.4), fill=140)
    for (x, y0, y1) in ((-2.2, -2, 3.4), (1.6, -2, 5.6)):
        d.line(P([(x, y0), (x, y1)]), fill=140, width=int(1.3 * U))
        d.ellipse(box(x - 1.0, y1 - 0.8, x + 1.0, y1 + 1.2), fill=140)


def g_water(d):
    r, cy, tip = 6.2, 2.6, -10.0
    k = math.asin(r / (cy - tip))
    seq = [(0, tip)] + [(r * math.cos(a), cy + r * math.sin(a)) for a in np.linspace(-k, math.pi + k, 40)]
    d.polygon(P(seq), fill=255)
    d.ellipse(box(-3.6, 0.6, -1.6, 4.0), fill=140)


def g_ice(d):
    w = int(1.7 * U)
    for i in range(6):
        a = i * math.pi / 3
        c, s = math.cos(a), math.sin(a)
        d.line(P([(0, 0), (c * 10, s * 10)]), fill=255, width=w)
        for f in (5.6, 8.0):
            for side in (-1, 1):
                b = a + side * math.pi / 4
                d.line(P([(c * f, s * f), (c * f + math.cos(b) * 2.6, s * f + math.sin(b) * 2.6)]), fill=255, width=int(1.2 * U))
    hexagon = [(math.cos(i * math.pi / 3 + math.pi / 6) * 3.2, math.sin(i * math.pi / 3 + math.pi / 6) * 3.2) for i in range(6)]
    d.polygon(P(hexagon), fill=140)


def g_mercury(d):
    d.ellipse(box(-7, -6, 5, 6), fill=255)
    d.ellipse(box(3, -9, 9, -3), fill=255)
    d.ellipse(box(3.4, 3.6, 8.6, 8.8), fill=255)
    d.ellipse(box(-4.6, -3.6, -1.0, -0.6), fill=140)


def g_mist(d):
    for k, y in enumerate((-5.5, 0, 5.5)):
        seq = [(x, y + math.sin(x * 0.7 + k) * 1.6) for x in np.linspace(-9.5 + k, 9.5 - k * 0.5, 40)]
        d.line(P(seq), fill=255 if k != 1 else 200, width=int(2.4 * U), joint="curve")


def g_wind(d):
    w = int(1.6 * U)
    for k, (y, length, r) in enumerate([(-5, 14, 3.0), (0.5, 16, 2.6), (6, 11, 2.2)]):
        x0 = -9 + k * 1.2
        x1 = x0 + length - r
        d.line(P([(x0, y), (x1, y)]), fill=255, width=w)
        bb = P([(x1 - r, y - 2 * r), (x1 + r, y)])
        d.arc([bb[0], bb[1]], start=90, end=360, fill=255, width=w)


def star_pts(r_out, r_in, n=5, rot=-math.pi / 2, cx=0, cy=0):
    return [(cx + math.cos(rot + i * math.pi / n) * (r_out if i % 2 == 0 else r_in),
             cy + math.sin(rot + i * math.pi / n) * (r_out if i % 2 == 0 else r_in)) for i in range(2 * n)]


def g_star(d):
    d.polygon(P(star_pts(10, 4.2)), fill=255)
    d.polygon(P(star_pts(4.6, 2.0)), fill=140)


def g_storm(d):
    for (x, y, r) in ((-4.6, -3, 4.2), (1.2, -5.2, 5.0), (5.6, -2.2, 3.8)):
        d.ellipse(box(x - r, y - r, x + r, y + r), fill=255)
    d.rectangle(box(-8.8, -3, 9.4, 1.4), fill=255)
    d.polygon(P([(1, 1), (-3, 6.2), (0, 6.2), (-2.4, 10.4), (4.2, 4.2), (1.2, 4.2), (3.6, 1)]), fill=140)


def g_earth(d):
    d.polygon(P([(-9.5, 7.5), (-3.4, -3.5), (-0.6, 0.2), (2.8, -8.5), (9.5, 7.5)]), fill=255)
    d.polygon(P([(2.8, -8.5), (4.9, -3.6), (3.5, -4.3), (2.4, -2.5), (1.3, -4.7)]), fill=140)
    d.polygon(P([(-3.4, -3.5), (-2.2, -1.4), (-3.1, -1.8), (-4.0, -1.2)]), fill=140)


def g_plant(d):
    leaf = [(math.sin(t) * 6.0 * (1 - abs(math.cos(t)) * 0.0), -math.cos(t) * 9.0) for t in np.linspace(0, math.pi, 30)]
    leaf = [(0, -9.5)] + [(math.sin(t) * 6.2, -9.5 + t / math.pi * 15) for t in np.linspace(0.15, math.pi - 0.15, 24)] + [(0, 5.5)] + \
           [(-math.sin(t) * 6.2, -9.5 + t / math.pi * 15) for t in np.linspace(math.pi - 0.15, 0.15, 24)]
    d.polygon(P(leaf, dx=1.0, dy=-0.5), fill=255)
    d.line(P([(1.0, -7.5), (1.0, 5)]), fill=140, width=int(1.0 * U))
    for y in (-4, -0.5, 3):
        d.line(P([(1.0, y + 2), (4.4, y - 0.4)]), fill=140, width=int(0.8 * U))
        d.line(P([(1.0, y + 2), (-2.4, y - 0.4)]), fill=140, width=int(0.8 * U))
    d.line(P([(1.0, 5), (-1.8, 10)]), fill=255, width=int(1.5 * U))


def g_sand(d):
    for k, y in enumerate((2.0, 6.5)):
        seq = [(x, y - 4.2 * math.exp(-((x - (2 if k == 0 else -3)) / 4.2) ** 2)) for x in np.linspace(-10, 10, 40)]
        d.polygon(P(seq + [(10, y + 2.2), (-10, y + 2.2)]), fill=255 if k == 0 else 200)
    for (x, y) in ((-6, -6), (-2, -8.5), (3, -6.8), (7, -8.8), (5.5, -3.4), (-7.5, -2.4)):
        d.ellipse(box(x - 0.9, y - 0.9, x + 0.9, y + 0.9), fill=255)


def g_light(d):
    d.ellipse(box(-4.2, -4.2, 4.2, 4.2), fill=255)
    for i in range(8):
        a = i * math.pi / 4
        Lr = 10 if i % 2 == 0 else 7.6
        c, s = math.cos(a), math.sin(a)
        d.polygon(P([(c * 5.2 - s * 1.3, s * 5.2 + c * 1.3), (c * Lr, s * Lr), (c * 5.2 + s * 1.3, s * 5.2 - c * 1.3)]), fill=255)
    d.ellipse(box(-2.0, -2.0, 2.0, 2.0), fill=140)


def g_lightning(d):
    d.polygon(P([(2.6, -10.5), (-6.2, 1.2), (-0.6, 1.2), (-3.4, 10.5), (6.6, -2.4), (0.8, -2.4), (4.6, -10.5)]), fill=255)
    d.polygon(P([(2.6, -8.6), (-3.6, 0.0), (-1.2, 0.0)]), fill=140)


def g_sword(d):
    d.polygon(P([(0, -11), (1.9, -8.4), (1.9, 3.6), (-1.9, 3.6), (-1.9, -8.4)]), fill=255)
    d.line(P([(0, -8.6), (0, 3.2)]), fill=140, width=int(0.8 * U))
    d.rectangle(box(-5.6, 3.6, 5.6, 5.4), fill=255)
    d.rectangle(box(-1.1, 5.4, 1.1, 9.0), fill=200)
    d.ellipse(box(-1.8, 8.6, 1.8, 11.4), fill=255)


def g_dark(d):
    d.ellipse(box(-8.5, -8.5, 8.5, 8.5), fill=255)
    d.ellipse(box(-4.0, -10.4, 11.4, 5.0), fill=0)
    for (x, y, r) in ((5.4, 5.4, 1.1), (7.4, -7.6, 0.9), (2.4, -2.6, 0.6)):
        d.ellipse(box(x - r, y - r, x + r, y + r), fill=200)


def g_shadow(d):
    d.ellipse(box(-6.5, -9.5, 6.5, 3.5), fill=255)
    for (x, sway) in ((-4.6, -2.4), (0, 1.4), (4.6, 2.4)):
        d.polygon(P([(x - 2.4, 1), (x + 2.4, 1), (x + sway, 10.5)]), fill=255)
    d.ellipse(box(-4.0, -4.4, -1.2, -2.0), fill=0)
    d.ellipse(box(1.2, -4.4, 4.0, -2.0), fill=0)


def g_poison(d):
    d.ellipse(box(-7.5, -3.0, 7.5, 10.5), fill=255)
    d.rectangle(box(-2.2, -9, 2.2, -1.5), fill=255)
    d.rectangle(box(-3.4, -10.2, 3.4, -8.2), fill=200)
    d.ellipse(box(-5.2, 1.4, 5.2, 8.6), fill=140)
    for (x, y, r) in ((-1.6, 3.6, 1.1), (2.0, 5.6, 0.8), (0.4, 1.8, 0.6)):
        d.ellipse(box(x - r, y - r, x + r, y + r), fill=255)


def g_spatial(d):
    """A portal ring with a vertical rift torn open inside it."""
    d.ellipse(box(-9.5, -9.5, 9.5, 9.5), outline=255, width=int(2.2 * U))
    rift = [(math.sin(t) * 3.2, -7.5 + 15 * t / math.pi) for t in np.linspace(0, math.pi, 20)] + \
           [(-math.sin(t) * 3.2, -7.5 + 15 * t / math.pi) for t in np.linspace(math.pi, 0, 20)]
    d.polygon(P(rift), fill=140)
    d.line(P([(0, -6.6), (0.6, -2), (-0.6, 2), (0, 6.6)]), fill=255, width=int(0.9 * U))


def g_mirror(d):
    d.polygon(P([(0, -10.5), (8, 0), (0, 10.5), (-8, 0)]), fill=255)
    d.polygon(P([(0, -7), (5.2, 0), (0, 7), (-5.2, 0)]), fill=140)
    d.line(P([(-2.6, -2.6), (1.4, -6.4)]), fill=255, width=int(1.0 * U))
    d.line(P([(-1.4, 1.6), (3.2, -3.0)]), fill=255, width=int(0.8 * U))


def g_gravity(d):
    d.ellipse(box(-6, -6, 6, 6), fill=255)
    bb = box(-10.5, -3.2, 10.5, 3.2)
    d.ellipse(bb, outline=200, width=int(1.4 * U))
    d.ellipse(box(-6, -6, 6, 6), fill=255)
    d.pieslice(box(-10.5, -3.2, 10.5, 3.2), 0, 180, fill=0)
    d.ellipse(box(-6, -6, 6, 6), fill=255)
    d.arc(box(-10.5, -3.2, 10.5, 3.2), 0, 180, fill=200, width=int(1.4 * U))
    d.ellipse(box(-3.4, -4.0, -0.6, -1.4), fill=140)


def g_time(d):
    d.rectangle(box(-7.5, -10.5, 7.5, -8.6), fill=255)
    d.rectangle(box(-7.5, 8.6, 7.5, 10.5), fill=255)
    d.polygon(P([(-6, -8.6), (6, -8.6), (0.9, 0), (6, 8.6), (-6, 8.6), (-0.9, 0)]), fill=255)
    d.polygon(P([(-3.6, -6.6), (3.6, -6.6), (0, -2.4)]), fill=140)
    d.polygon(P([(0, 3.4), (4.4, 8.0), (-4.4, 8.0)]), fill=140)


def g_sealing(d):
    d.ellipse(box(-9.5, -9.5, 9.5, 9.5), outline=255, width=int(1.6 * U))
    d.polygon(P(star_pts(7.4, 7.4 * 0.38)), fill=255)
    d.ellipse(box(-2.4, -2.4, 2.4, 2.4), fill=0)
    d.ellipse(box(-1.3, -1.3, 1.3, 1.3), fill=140)


def g_reinforcement(d):
    d.rounded_rectangle(box(-7.5, -3.5, 6.5, 9.5), radius=3 * U, fill=255)
    for i in range(4):
        x = -6 + i * 3.6
        d.ellipse(box(x - 2.0, -7.5, x + 2.0, -1.5), fill=255)
    d.rounded_rectangle(box(-10, 0, -5, 6), radius=2 * U, fill=255)
    for i in range(3):
        x = -4.2 + i * 3.6
        d.line(P([(x, -3.4), (x, -0.4)]), fill=140, width=int(0.8 * U))
    d.line(P([(-6.5, 3.6), (2.0, 3.6)]), fill=140, width=int(0.9 * U))


def g_beast(d):
    w = int(2.0 * U)
    for k in range(3):
        x = -6 + k * 5.4
        seq = [(x + 2.6 * math.sin(t * 1.4) - t * 1.6, -10 + t * 6.6) for t in np.linspace(0, 3, 20)]
        d.line(P(seq), fill=255, width=w, joint="curve")
        d.polygon(P([(seq[0][0] - 1, seq[0][1]), (seq[0][0] + 1, seq[0][1]), (seq[0][0] + 0.2, seq[0][1] - 1.6)]), fill=255)


def g_bone(d):
    d.polygon(P([(-6.2, 5.0), (5.0, -6.2), (6.6, -4.6), (-4.6, 6.6)]), fill=255)
    for (x, y) in ((-7.6, 4.2), (-4.2, 7.6), (7.6, -4.2), (4.2, -7.6)):
        d.ellipse(box(x - 2.6, y - 2.6, x + 2.6, y + 2.6), fill=255)
    d.line(P([(-3.0, 3.0), (3.0, -3.0)]), fill=140, width=int(0.9 * U))


def g_blood(d):
    g_water(d)
    d.ellipse(box(4.6, 5.6, 8.0, 9.6), fill=255)


def g_creation(d):
    hexa = [(math.cos(i * math.pi / 3 - math.pi / 2) * 10, math.sin(i * math.pi / 3 - math.pi / 2) * 10) for i in range(6)]
    d.polygon(P(hexa), fill=255)
    for i in range(0, 6, 2):
        d.line(P([(0, 0), hexa[i]]), fill=140, width=int(1.0 * U))
    inner = [(p[0] * 0.42, p[1] * 0.42) for p in hexa]
    d.polygon(P(inner), fill=140)


def g_copy(d):
    d.rounded_rectangle(box(-9, -9, 3, 3), radius=2 * U, fill=200)
    d.rounded_rectangle(box(-3, -3, 9, 9), radius=2 * U, fill=255)
    d.rounded_rectangle(box(-0.6, -0.6, 6.6, 6.6), radius=1 * U, fill=140)


def g_illusion(d):
    eye = [(x, -6.2 * math.cos(x / 10 * math.pi / 2)) for x in np.linspace(-10, 10, 30)] + \
          [(x, 6.2 * math.cos(x / 10 * math.pi / 2)) for x in np.linspace(10, -10, 30)]
    d.polygon(P(eye), fill=255)
    seq = [(math.cos(t) * t * 0.55, math.sin(t) * t * 0.55) for t in np.linspace(0, 4 * math.pi, 80)]
    d.line(P(seq), fill=0, width=int(1.0 * U))


def g_dream(d):
    """A crescent moon over a cloud, with two sparkles."""
    d.ellipse(box(-8.6, -11.0, -0.6, -3.0), fill=255)
    d.ellipse(box(-5.6, -12.6, 2.4, -4.6), fill=0)
    for (x, y, r) in ((-4.8, 6.4, 3.2), (0.2, 4.8, 4.0), (5.2, 6.4, 3.4)):
        d.ellipse(box(x - r, y - r, x + r, y + r), fill=200)
    d.rounded_rectangle(box(-8.0, 6.2, 8.6, 9.6), radius=int(1.6 * U), fill=200)
    for (cx, cy, r) in ((6.0, -6.4, 2.6), (2.4, -9.6, 1.4)):
        d.polygon(P(star_pts(r, r * 0.36, 4, 0, cx, cy)), fill=255)


def g_anti_magic(d):
    pass        # clover_mask(5) is pasted in below


def g_steel(d):
    teeth = []
    for i in range(16):
        a = i * math.pi / 8
        r = 10 if i % 2 == 0 else 7.6
        for da in (-0.17, 0.17):
            teeth.append((math.cos(a + da) * r, math.sin(a + da) * r))
    d.polygon(P(teeth), fill=255)
    d.ellipse(box(-3.4, -3.4, 3.4, 3.4), fill=0)
    d.ellipse(box(-5.4, -5.4, 5.4, 5.4), outline=140, width=int(1.0 * U))


def g_thread(d):
    d.line(P([(-9, -9), (5, 5)]), fill=255, width=int(1.6 * U))
    d.polygon(P([(5, 5), (8.6, 8.6), (4.2, 6.6)]), fill=255)
    d.ellipse(box(-9.6, -9.6, -6.6, -6.6), outline=255, width=int(0.9 * U))
    seq = [(-8 + t * 1.1, -8 + t * 1.6 + 3.2 * math.sin(t * 0.9)) for t in np.linspace(0, 14, 60)]
    d.line(P(seq), fill=200, width=int(1.1 * U), joint="curve")


def g_forbidden(d):
    pts = [(math.cos(-math.pi / 2 + k * 4 * math.pi / 5) * 10, math.sin(-math.pi / 2 + k * 4 * math.pi / 5) * 10) for k in range(6)]
    d.line(P(pts), fill=200, width=int(1.3 * U), joint="curve")
    d.ellipse(box(-10, -10, 10, 10), outline=200, width=int(1.1 * U))
    eye = [(x, -3.4 * math.cos(x / 6 * math.pi / 2)) for x in np.linspace(-6, 6, 20)] + \
          [(x, 3.4 * math.cos(x / 6 * math.pi / 2)) for x in np.linspace(6, -6, 20)]
    d.polygon(P(eye), fill=255)
    d.ellipse(box(-1.2, -3.0, 1.2, 3.0), fill=0)


def g_transmutation(d):
    """A transmutation circle: ring, inscribed triangle and a small inner ring."""
    d.ellipse(box(-10, -10, 10, 10), outline=255, width=int(1.6 * U))
    tri = [(math.cos(-math.pi / 2 + k * 2 * math.pi / 3) * 8.6, math.sin(-math.pi / 2 + k * 2 * math.pi / 3) * 8.6) for k in range(3)]
    d.polygon(P(tri), outline=255, width=int(1.4 * U))
    d.ellipse(box(-3, -1.5, 3, 4.5), fill=200)


def g_ash(d):
    """A swirl of ash flakes round a dark ember."""
    for k in range(7):
        a = k * 2 * math.pi / 7
        r = 4.5 + k * 0.8
        x, y = math.cos(a) * r, math.sin(a) * r
        d.polygon(P([(x - 1.6, y), (x, y - 1.2), (x + 1.8, y + 0.2), (x, y + 1.4)]), fill=255 if k % 2 else 180)
    d.ellipse(box(-3.2, -3.2, 3.2, 3.2), fill=140)


def g_cotton(d):
    """A cotton sheep: a puffy cloud body, a face and little legs."""
    for (x, y, r) in ((-4, -1, 4.4), (0.5, -3, 4.8), (4.5, -0.5, 4.2), (-1.5, 2.5, 4.2), (3, 2.5, 4)):
        d.ellipse(box(x - r, y - r, x + r, y + r), fill=255)
    d.ellipse(box(6.5, -3.5, 10.5, 1.5), fill=170)
    for x in (-4, -1, 2.5, 5.5):
        d.rectangle(box(x - 0.7, 5.5, x + 0.7, 9.5), fill=170)


def g_recombination(d):
    """A bull's head made of blocks: a square face, two horns."""
    d.rectangle(box(-5.5, -4, 5.5, 8), fill=255)
    d.rectangle(box(-3.5, 1, -1, 3.5), fill=120)
    d.rectangle(box(1, 1, 3.5, 3.5), fill=120)
    d.polygon(P([(-5.5, -4), (-10, -9), (-8.5, -3)]), fill=220)
    d.polygon(P([(5.5, -4), (10, -9), (8.5, -3)]), fill=220)
    for y in (-1, 6):
        d.line(P([(-5.5, y), (5.5, y)]), fill=140, width=int(0.8 * U))


GLYPHS = {k: globals()["g_" + k] for k in MAGIC}


def glyph_mask(name):
    if name == "anti_magic":
        return clover_mask(5, 0.98)
    im = L()
    GLYPHS[name](ImageDraw.Draw(im))
    return im


# ---------------------------------------------------------------------------------------------------- symbol rendering (canvas)
def to_arr(im):
    return np.asarray(im).astype(np.float32) / 255.0


def blur(a, r):
    return to_arr(Image.fromarray((np.clip(a, 0, 1) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(r)))


def symbol_layer(name, scale=1.0, glow_strength=1.0):
    """RGBA float canvas: glow halo, dark outline, glossy body (light top, darker bottom, facets), top highlight."""
    _, glow, body, _, _ = MAGIC[name]
    m = glyph_mask(name)
    if scale != 1.0:
        sz = int(S * scale)
        m2 = m.resize((sz, sz), Image.LANCZOS)
        m = L()
        m.paste(m2, ((S - sz) // 2, (S - sz) // 2))
    a = to_arr(m)
    solid = (a > 0.25).astype(np.float32)
    facet = ((a > 0.25) & (a < 0.85)).astype(np.float32)
    outline = np.clip(blur(solid, 0.9 * U) * 3.0, 0, 1)
    outline = np.clip(np.maximum(outline, solid), 0, 1)
    halo = np.clip(blur(solid, 2.6 * U) * 2.2, 0, 1)
    yy = np.mgrid[0:S, 0:S][0].astype(np.float32) / S
    g, b = np.array(rgb(glow), np.float32) / 255, np.array(rgb(body), np.float32) / 255
    dark_body = name in ("anti_magic", "forbidden")
    top = np.clip(b * 1.15 + 0.12, 0, 1) if not dark_body else np.array([0.16, 0.12, 0.14])
    bottom = (b * 0.55 + g * 0.25) if not dark_body else np.array([0.03, 0.02, 0.03])
    t = np.clip((yy - 0.25) / 0.5, 0, 1)[..., None]
    col = top * (1 - t) + bottom * t
    col = col * (1 - facet[..., None] * 0.38) + (g * 0.5 * facet[..., None] if not dark_body else g * 0.55 * facet[..., None])
    # gloss: a bright band along the top inner edge of the body
    shifted = np.zeros_like(solid); shifted[int(1.2 * U):, :] = solid[:-int(1.2 * U), :]
    gloss = np.clip(solid - shifted, 0, 1) * solid
    col = col + gloss[..., None] * (0.5 if not dark_body else 0.25)
    line = np.array([0.04, 0.03, 0.05]) if not dark_body else g * 0.95          # anti-magic: neon rim instead of an ink line
    out = np.zeros((S, S, 4), np.float32)
    out[..., :3] = g
    out[..., 3] = halo * 0.85 * glow_strength
    ol = outline * (1 - solid)
    out[..., :3] = out[..., :3] * (1 - ol[..., None]) + line * ol[..., None]
    out[..., 3] = np.maximum(out[..., 3], ol)
    out[..., :3] = out[..., :3] * (1 - solid[..., None]) + col * solid[..., None]
    out[..., 3] = np.maximum(out[..., 3], solid)
    return out


def over(dst, src):
    a = src[..., 3:4]
    dst[..., :3] = dst[..., :3] * (1 - a) + src[..., :3] * a
    dst[..., 3:4] = dst[..., 3:4] + a * (1 - dst[..., 3:4])
    return dst


def add_glow(dst, mask, colour, strength=1.0):
    c = np.array(rgb(colour), np.float32) / 255
    dst[..., :3] = np.clip(dst[..., :3] + mask[..., None] * c * strength, 0, 1)
    return dst


# ---------------------------------------------------------------------------------------------------- backgrounds and frames
YY, XX = np.mgrid[0:S, 0:S].astype(np.float32)
CX = CY = (S - 1) / 2
R = np.sqrt((XX - CX) ** 2 + (YY - CY) ** 2) / (S / 2)
ANG = np.arctan2(YY - CY, XX - CX)


def bg_aura(glow):
    """Active: midnight blue with a soft aura in the magic colour."""
    g = np.array(rgb(glow), np.float32) / 255
    base = np.array([0.03, 0.05, 0.13]) * (1 - R[..., None] * 0.4)
    n = fbm(S, S, 64, 3)[..., None]
    aura = np.exp(-(R / 0.62) ** 2)[..., None]
    col = base + aura * (0.25 * g + np.array([0.04, 0.08, 0.22])) + (n - 0.5) * 0.06
    return np.dstack([np.clip(col, 0, 1), np.ones((S, S))]).astype(np.float32)


def bg_ancient(glow):
    """Buff / rune: aged parchment, burnt darker edges."""
    n = fbm(S, S, 48, 21)
    grain = fbm(S, S, 6, 37, 2)
    v = 0.78 + 0.18 * (n - 0.5) + 0.06 * (grain - 0.5)
    col = np.dstack([v * 0.86, v * 0.74, v * 0.52])
    col *= np.clip(1.18 - R * 0.62, 0.35, 1.05)[..., None]
    rng = np.random.default_rng(5)
    img = Image.fromarray((np.clip(col, 0, 1) * 255).astype(np.uint8))
    d = ImageDraw.Draw(img)
    for _ in range(5):                                           # faint cracks
        x, y = rng.integers(30, S - 30), rng.integers(30, S - 30)
        pts = [(x, y)]
        for _ in range(rng.integers(4, 8)):
            x += rng.integers(-18, 19); y += rng.integers(-18, 19)
            pts.append((x, y))
        d.line(pts, fill=(96, 70, 44), width=3)
    return np.dstack([np.asarray(img).astype(np.float32) / 255, np.ones((S, S))]).astype(np.float32)


def bg_void(glow):
    """Ultimate: black void with a radial burst of the magic colour."""
    g = np.array(rgb(glow), np.float32) / 255
    rays = (0.5 + 0.5 * np.cos(ANG * 12)) ** 6 * np.clip(1.1 - R, 0, 1)
    core = np.exp(-(R / 0.45) ** 2)
    col = np.array([0.012, 0.01, 0.02]) + g * (core * 0.32 + rays * 0.2)[..., None]
    return np.dstack([np.clip(col, 0, 1), np.ones((S, S))]).astype(np.float32)


def frame(dst, metal, rim_glow=None, corner="clover"):
    """Embossed frame 3 px wide: bevel lit from the top-left, an inner bright lip, rune ornaments in the corners."""
    m = np.array(metal, np.float32) / 255
    w = 3 * U
    edge = np.minimum.reduce([XX, YY, S - 1 - XX, S - 1 - YY])
    band = edge < w
    t = edge / w
    light = np.where((XX + YY) < S, 1.0, 0.0)
    # bevel: outer half dark/light by side, inner half the reverse, centre line bright
    lit = np.where(t < 0.5, 0.75 + 0.45 * light, 1.15 - 0.45 * light)
    col = m * lit[..., None]
    col = np.where((np.abs(t - 0.5) < 0.12)[..., None], np.clip(m * 1.45 + 0.08, 0, 1), col)
    col = np.where((t < 0.12)[..., None], m * 0.25, col)                    # dark outer rim
    dst[band, :3] = col[band]
    dst[band, 3] = 1.0
    inner = (edge >= w) & (edge < w + U)                                      # thin shadow / glow inside the frame
    if rim_glow is not None:
        add_glow(dst, inner.astype(np.float32) * 0.9, rim_glow, 1.0)
    else:
        dst[inner, :3] *= 0.55
    # cut the corners round
    cr = ((XX < U) | (XX > S - 1 - U)) & ((YY < U) | (YY > S - 1 - U))
    dst[cr, 3] = 0
    # corner runes: small clover (3 dots + stem) or spikes, embossed in the frame colour
    orn = Image.new("L", (S, S), 0)
    d = ImageDraw.Draw(orn)
    for (sx, sy) in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
        cx = S / 2 + sx * (S / 2 - 5.2 * U)
        cy = S / 2 + sy * (S / 2 - 5.2 * U)
        if corner == "clover":
            for k in range(3):
                a = -math.pi / 2 + k * 2 * math.pi / 3
                d.ellipse([cx + math.cos(a) * 1.1 * U - 0.95 * U, cy + math.sin(a) * 1.1 * U - 0.95 * U,
                           cx + math.cos(a) * 1.1 * U + 0.95 * U, cy + math.sin(a) * 1.1 * U + 0.95 * U], fill=255)
        elif corner == "rune":
            d.rectangle([cx - 1.2 * U, cy - 1.2 * U, cx + 1.2 * U, cy + 1.2 * U], outline=255, width=int(0.7 * U))
            d.line([cx - 1.2 * U, cy, cx + 1.2 * U, cy], fill=255, width=int(0.5 * U))
        else:   # spike
            d.polygon([(cx - sx * 2.6 * U, cy - sy * 2.6 * U), (cx + sx * 1.6 * U, cy - sy * 0.4 * U), (cx - sx * 0.4 * U, cy + sy * 1.6 * U)], fill=255)
    o = to_arr(orn)
    sh = np.zeros_like(o); sh[U // 2:, U // 2:] = o[:-(U // 2), :-(U // 2)]
    dst[..., :3] = dst[..., :3] * (1 - sh[..., None] * 0.6)
    dst[..., :3] = dst[..., :3] * (1 - o[..., None]) + np.clip(m * 1.35, 0, 1) * o[..., None]
    return dst


# ---------------------------------------------------------------------------------------------------- effects
def particles(dst, colour, count, seed, size=(0.5, 1.2), area=0.85):
    rng = np.random.default_rng(seed)
    im = Image.new("L", (S, S), 0)
    d = ImageDraw.Draw(im)
    for _ in range(count):
        a = rng.random() * 2 * math.pi
        r = (0.25 + rng.random() * 0.75) * area * S / 2
        x, y = S / 2 + math.cos(a) * r, S / 2 + math.sin(a) * r
        s = (size[0] + rng.random() * (size[1] - size[0])) * U
        d.ellipse([x - s, y - s, x + s, y + s], fill=255)
    m = to_arr(im)
    add_glow(dst, np.clip(blur(m, 1.2 * U) * 1.5 + m, 0, 1), colour, 0.9)
    return dst


def streaks(dst, colour, seed):
    """Energy discharge: diagonal speed lines sweeping up-right behind the symbol."""
    rng = np.random.default_rng(seed)
    im = Image.new("L", (S, S), 0)
    d = ImageDraw.Draw(im)
    for _ in range(9):
        off = (rng.random() - 0.5) * S * 1.1
        Ln = (0.25 + rng.random() * 0.35) * S
        x0 = S / 2 + off * 0.7 - Ln * 0.5
        y0 = S / 2 + off * 0.7 + Ln * 0.5
        d.line([x0, y0, x0 + Ln * 0.8, y0 - Ln * 0.8], fill=int(120 + rng.random() * 135), width=int((0.4 + rng.random() * 0.6) * U))
    add_glow(dst, blur(to_arr(im), 0.5 * U) * 0.7, colour, 1.0)
    return dst


def magic_circle(dst, colour, r_out, star=0, ticks=24, width=0.8, strength=1.0, rot=0.0):
    im = Image.new("L", (S, S), 0)
    d = ImageDraw.Draw(im)
    ro, ri = r_out * U, (r_out - 1.6) * U
    d.ellipse([S / 2 - ro, S / 2 - ro, S / 2 + ro, S / 2 + ro], outline=255, width=int(width * U))
    d.ellipse([S / 2 - ri, S / 2 - ri, S / 2 + ri, S / 2 + ri], outline=200, width=max(1, int(width * 0.6 * U)))
    for i in range(ticks):
        a = rot + i * 2 * math.pi / ticks
        c, s = math.cos(a), math.sin(a)
        if i % 3 == 0:
            d.rectangle([S / 2 + c * (ro - 0.8 * U) - 0.45 * U, S / 2 + s * (ro - 0.8 * U) - 0.45 * U,
                         S / 2 + c * (ro - 0.8 * U) + 0.45 * U, S / 2 + s * (ro - 0.8 * U) + 0.45 * U], fill=255)
        else:
            d.line([S / 2 + c * ri, S / 2 + s * ri, S / 2 + c * (ri + 0.8 * U), S / 2 + s * (ri + 0.8 * U)], fill=200, width=max(1, int(0.4 * U)))
    if star:
        pts = [(S / 2 + math.cos(rot - math.pi / 2 + k * 2 * math.pi * (star // 2) / star) * ri,
                S / 2 + math.sin(rot - math.pi / 2 + k * 2 * math.pi * (star // 2) / star) * ri) for k in range(star + 1)]
        d.line(pts, fill=220, width=max(1, int(0.5 * U)))
    m = to_arr(im)
    add_glow(dst, np.clip(m + blur(m, 1.0 * U) * 0.8, 0, 1), colour, strength)
    return dst


def chains(dst, metal):
    """Two chain links crossing behind the lower half (buff / binding)."""
    im = Image.new("L", (S, S), 0)
    d = ImageDraw.Draw(im)
    for k in range(5):
        x = 3.5 * U + k * 5.6 * U
        y = S - 8.5 * U - k * 1.2 * U
        if k % 2 == 0:
            d.ellipse([x - 2.6 * U, y - 1.5 * U, x + 2.6 * U, y + 1.5 * U], outline=255, width=int(0.9 * U))
        else:
            d.ellipse([x - 1.4 * U, y - 1.9 * U, x + 1.4 * U, y + 1.9 * U], outline=255, width=int(0.9 * U))
    m = to_arr(im)
    c = np.array(metal, np.float32) / 255
    dst[..., :3] = dst[..., :3] * (1 - m[..., None]) + c * m[..., None]
    return dst


# ---------------------------------------------------------------------------------------------------- the three variants
GOLD, SILVER, IRON = (214, 170, 78), (176, 184, 198), (44, 40, 46)


def variant_active(name, seed):
    _, glow, *_ = MAGIC[name]
    img = bg_aura(glow if name not in ("anti_magic", "forbidden") else 0x5A0A2A)
    streaks(img, glow, seed)
    particles(img, glow, 14, seed + 1)
    over(img, symbol_layer(name, 0.92))
    return frame(img, GOLD, corner="clover")


def variant_buff(name, seed):
    _, glow, *_ = MAGIC[name]
    img = bg_ancient(glow)
    chains(img, (112, 96, 80))
    magic_circle(img, glow if name != "creation" else 0xE8C050, 11.8, star=0, ticks=24, width=0.9, strength=1.0, rot=seed * 0.1)
    over(img, symbol_layer(name, 0.62, 0.8))
    particles(img, glow, 6, seed + 2, size=(0.4, 0.8), area=0.7)
    return frame(img, SILVER, corner="rune")


def variant_ultimate(name, seed):
    _, glow, *_ = MAGIC[name]
    img = bg_void(glow)
    magic_circle(img, glow, 12.2, star=5, ticks=30, width=0.7, strength=0.9, rot=seed * 0.13)
    magic_circle(img, glow, 7.6, star=6 if name != "forbidden" else 5, ticks=18, width=0.6, strength=0.7, rot=-seed * 0.21)
    particles(img, glow, 22, seed + 3, size=(0.3, 0.9), area=0.95)
    over(img, symbol_layer(name, 0.86, 1.4))
    return frame(img, IRON, rim_glow=glow, corner="spike")


def reduce(img):
    a = (np.clip(img, 0, 1) * 255).astype(np.uint8)
    im = Image.fromarray(a, "RGBA").resize((N, N), Image.LANCZOS)
    im = im.filter(ImageFilter.UnsharpMask(radius=0.8, percent=60, threshold=1))
    arr = np.asarray(im).copy()
    arr[..., 3] = np.where(arr[..., 3] > 128, 255, 0)            # clean silhouette (cut corners) for the 32 px icon
    return Image.fromarray(arr, "RGBA")


# ---------------------------------------------------------------------------------------------------- archetype badges (unchanged look)
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
    col = solid[..., None] * np.array(colour, np.float32) + ring[..., None] * np.array((12, 10, 14), np.float32)
    a = solid * 215 + ring * 190
    tile = np.zeros((N, N, 4), np.float32)
    tile[N - 13:N - 1, N - 13:N - 1] = np.dstack([col, a])
    return Image.fromarray(np.clip(tile, 0, 255).astype(np.uint8), "RGBA")


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


MODIFIERS = {"offense": (d_offense, (255, 96, 80)), "defense": (d_defense, (150, 200, 255)),
             "buff": (d_buff, (110, 240, 120)), "debuff": (d_debuff, (196, 120, 255))}


# ---------------------------------------------------------------------------------------------------- prompts document
def prompts_doc():
    lines = ["# Black Clover skill icon prompts", "",
             "Generated by `tools/gen_skill_icons.py` from the \"Black Clover Skill Icon Generator\" brief: three image-generator prompts "
             "(Midjourney / DALL-E) for every grimoire magic in the mod. The mod ships procedural icons in the same three roles; to use a "
             "hand-made image instead, export it as a square PNG, scale it to 32x32 and overwrite the file named under each magic.", "",
             "Template: `[Spell/Magic description] in Black Clover anime magic spell icon style, RPG user interface asset, centered "
             "composition, dark fantasy aesthetic, glowing [Color] magic aura, intricate rune borders, clean lines, highly detailed "
             "digital painting, vibrant magical particle effects --ar 1:1`", ""]
    tail = ("in Black Clover anime magic spell icon style, RPG user interface asset, centered composition, dark fantasy aesthetic, "
            "glowing {c} magic aura, intricate rune borders, clean lines, highly detailed digital painting, vibrant magical particle effects --ar 1:1")
    for key, (display, _, _, colours, spell) in MAGIC.items():
        lines += [f"## {display}", "",
                  f"1. **Active spell / attack** (`textures/skill/grimoire/{key}.png`)", "",
                  "   ```", f"   {spell} unleashed as a dynamic {display.lower()} attack, energy discharge bursting from an open grimoire, "
                  f"motion streaks, gold embossed frame with clover runes {tail.format(c=colours)}", "   ```", "",
                  f"2. **Buff / grimoire page rune** (`textures/skill/icons/{key}_buff.png`)", "",
                  "   ```", f"   a glowing {display.lower()} rune sigil on an ancient parchment grimoire page, mystical glyph circle, binding "
                  f"chains and a faint defensive shield, silver stone border {tail.format(c=colours)}", "   ```", "",
                  f"3. **Ultimate / forbidden** (`textures/skill/icons/{key}_ultimate.png`)", "",
                  "   ```", f"   ultimate {display.lower()}: {spell} at full power, complex multi-layered magic circles with a star "
                  f"seal, celestial and abyssal energy, intense particle storm, black iron border lit from within "
                  f"{tail.format(c=colours)}", "   ```", ""]
    path = os.path.join(DOCS, "skill_icon_prompts.md")
    os.makedirs(DOCS, exist_ok=True)
    with open(path, "w", newline="\n") as f:
        f.write("\n".join(lines))


# ---------------------------------------------------------------------------------------------------- main
def main():
    os.makedirs(OUT, exist_ok=True)
    for f in os.listdir(OUT):                                   # a clean set every run (replaced, not piled up)
        if f.endswith(".png"):
            os.remove(os.path.join(OUT, f))
    icons = {}
    for i, name in enumerate(MAGIC):
        act, buf, ult = reduce(variant_active(name, i * 7 + 1)), reduce(variant_buff(name, i * 7 + 2)), reduce(variant_ultimate(name, i * 7 + 3))
        save(act, os.path.join(TEX, "grimoire", name + ".png"))
        save(buf, os.path.join(OUT, name + "_buff.png"))
        save(ult, os.path.join(OUT, name + "_ultimate.png"))
        sym = symbol_layer(name)
        save(reduce(sym), os.path.join(OUT, "sym_" + name + ".png"))
        icons[name] = (act, buf, ult)
    for key, fn in (("aura", bg_aura), ("ancient", bg_ancient), ("void", bg_void)):
        save(reduce(fn(0x5080FF)), os.path.join(OUT, "bg_" + key + ".png"))
    mods = {k: badge(*v) for k, v in MODIFIERS.items()}
    for k, im in mods.items():
        save(im, os.path.join(OUT, "mod_" + k + ".png"))

    names = list(MAGIC)
    G = 2
    sheet = Image.new("RGBA", (len(names) * (N + G) + G, 4 * (N + G) + G), (0, 0, 0, 0))
    for c, name in enumerate(names):
        for r, im in enumerate(icons[name]):
            sheet.alpha_composite(im, (G + c * (N + G), G + r * (N + G)))
    for c, im in enumerate(list(mods.values())):
        sheet.alpha_composite(im, (G + c * (N + G), G + 3 * (N + G)))
    save(sheet, os.path.join(OUT, "sheet.png"))

    # labelled preview (not shipped): 12 magics per block, rows active / buff / ultimate
    Z, per = 4, 12
    cell = N * Z
    blocks = [names[i:i + per] for i in range(0, len(names), per)]
    pv = Image.new("RGB", (per * (cell + 6) + 6, len(blocks) * (3 * (cell + 6) + 18) + 6), (22, 22, 28))
    dr = ImageDraw.Draw(pv)
    font = ImageFont.load_default()
    y = 6
    for blk in blocks:
        for c, name in enumerate(blk):
            dr.text((6 + c * (cell + 6), y), name, fill=(235, 215, 160), font=font)
        y += 14
        for r in range(3):
            for c, name in enumerate(blk):
                pv.paste(icons[name][r].resize((cell, cell), Image.NEAREST), (6 + c * (cell + 6), y), icons[name][r].resize((cell, cell), Image.NEAREST))
            y += cell + 6
        y += 4
    save(pv, os.path.join(PREVIEW, "sheet_preview.png"))
    prompts_doc()
    print("wrote", len(names), "magics x 3 icons, sheet, preview, docs/skill_icon_prompts.md")


if __name__ == "__main__":
    main()
