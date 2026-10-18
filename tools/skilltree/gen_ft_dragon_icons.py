"""1.62.0 Skill Tree: the 15 skill icons of the Fire Dragon Slayer and Lightning Dragon Slayer routes (Fairy Tail).

    python tools/skilltree/gen_ft_dragon_icons.py              # every icon
    python tools/skilltree/gen_ft_dragon_icons.py ft_fire_roar  # only these
    python tools/skilltree/gen_ft_dragon_icons.py --sheet       # also write build/skill_icons/ft_sheet.png (labelled preview, not shipped)

Why a script and not hand-made PNGs: the owner's skill icons (textures/skill/grimoire/*.png) are all drawn by tools/gen_skill_icons.py, and these have to sit next to
them in Tensura's skill menu without looking like a different game. So this reuses that framework (its glossy glyph layer, particles, streaks, void background and
embossed frame) and only adds what is new: one glyph per technique and a frame of its own (ember bronze with spike corners for fire, steel with spike corners for
lightning), NOT the clover-rune gold frame, which belongs to the Black Clover grimoire pages. Each icon is 32 px, written to
src/main/resources/assets/nusmp/textures/skill/ft/<skill file name>.png, which is the path DragonSkill.getSkillIcon() returns. Deterministic (fixed seeds).
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))
import gen_skill_icons as G  # noqa: E402

OUT = os.path.join(G.TEX, "ft")
P, U, box = G.P, G.U, G.box

FIRE = (0xFF5A1E, 0xFFC060)          # glow, body
BOLT = (0xFFD84A, 0xFFF6C0)
EMBER_METAL = (168, 92, 52)
STEEL_METAL = (176, 184, 198)

FLAME = [(0, -10), (2.4, -5.2), (5.6, -6.6), (7, -1), (6.4, 4.4), (3.2, 8), (-3.2, 8), (-6.4, 4.4), (-6.8, -1.6), (-4.4, -4.6), (-3.2, -2), (-1.4, -6.8)]
FLAME_CORE = [(0, -3), (1.8, 0.2), (3.2, 2.8), (2.1, 6.3), (-2.1, 6.3), (-3.2, 2.8), (-1.8, 0.2)]
BOLTP = [(2.6, -10.5), (-6.2, 1.2), (-0.6, 1.2), (-3.4, 10.5), (6.6, -2.4), (0.8, -2.4), (4.6, -10.5)]
BOLT_CORE = [(2.6, -8.6), (-3.6, 0.0), (-1.2, 0.0)]


def rot(seq, deg, cx=0.0, cy=0.0):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    return [(cx + (x - cx) * c - (y - cy) * s, cy + (x - cx) * s + (y - cy) * c) for x, y in seq]


def flame(d, cx, cy, s, ang=0.0):
    d.polygon(P(rot(FLAME, ang), s, cx, cy), fill=255)
    d.polygon(P(rot(FLAME_CORE, ang), s, cx, cy), fill=140)


def bolt(d, cx, cy, s, ang=0.0, facet=True):
    d.polygon(P(rot(BOLTP, ang), s, cx, cy), fill=255)
    if facet:
        d.polygon(P(rot(BOLT_CORE, ang), s, cx, cy), fill=140)


def fist(d, cx=0.0, cy=0.0, s=1.0):
    """A raised clenched fist seen from the front: the wrist, a block of four curled fingers (seams between them) and the thumb across the lower half."""
    d.rounded_rectangle(box(cx - 3.6 * s, cy + 4.6 * s, cx + 3.6 * s, cy + 11.0 * s), radius=int(1.2 * U * s), fill=200)
    d.rounded_rectangle(box(cx - 7.2 * s, cy - 6.8 * s, cx + 7.2 * s, cy + 6.6 * s), radius=int(3.0 * U * s), fill=255)
    for k in range(1, 4):
        x = cx + (-7.2 + k * 3.6) * s
        d.line(P([(x, cy - 6.4 * s), (x, cy + 0.2 * s)]), fill=140, width=int(1.0 * U))
    d.rounded_rectangle(box(cx - 6.4 * s, cy + 0.8 * s, cx + 4.8 * s, cy + 4.8 * s), radius=int(2.0 * U * s), fill=140)


# ---------------------------------------------------------------------------------------------------- fire glyphs
def g_fire_iron_fist(d):
    fist(d, 0.0, -0.4, 0.86)
    flame(d, -6.6, -6.6, 0.4, ang=-18)
    flame(d, 6.6, -6.6, 0.4, ang=18)
    flame(d, 0.0, -9.0, 0.34)


def g_fire_roar(d):
    d.polygon(P([(-10.6, -1.6), (-4.0, -5.8), (-1.0, -4.2), (3.0, -9.0), (4.6, -4.4), (9.0, -3.2), (6.4, -0.6), (10.0, 2.0), (5.6, 3.2), (6.6, 8.0), (2.0, 4.8), (-1.0, 8.4), (-3.2, 4.6), (-10.6, 1.8)]), fill=255)
    d.polygon(P([(-8.4, -0.8), (-2.4, -2.4), (3.4, -1.6), (7.0, 0.4), (3.0, 1.6), (-2.6, 2.4), (-8.4, 1.0)]), fill=140)


def g_fire_claw(d):
    G.g_beast(d)
    flame(d, 6.8, 6.4, 0.34)


def _wing(d, sign):
    pts = [(-1.5, 5.8), (-3.0, -1.0), (-5.6, -7.0), (-11.2, -10.6), (-8.8, -5.6), (-12.6, -5.4), (-9.2, -1.2), (-12.8, 0.6), (-8.6, 2.8), (-11.2, 5.8), (-5.8, 6.4)]
    d.polygon(P([(sign * x, y) for x, y in pts]), fill=255)
    d.polygon(P([(sign * x, y) for x, y in [(-2.4, 4.6), (-3.6, -0.4), (-6.0, -4.6), (-7.2, 0.2), (-6.2, 3.8)]]), fill=140)


def g_fire_wing_attack(d):
    _wing(d, 1)
    _wing(d, -1)
    flame(d, 0.0, 1.2, 0.34)


def g_fire_sword_horn(d):
    d.polygon(P([(-9.4, 9.6), (-4.2, 10.4), (-2.0, 5.0), (2.0, -1.2), (7.6, -6.4), (10.0, -10.6), (4.4, -9.0), (-1.2, -3.6), (-5.6, 3.0)]), fill=255)
    d.polygon(P([(-5.4, 8.0), (-3.6, 4.6), (1.0, -1.6), (6.4, -7.6), (2.6, -2.4), (-2.2, 4.0)]), fill=140)
    flame(d, -5.6, 3.6, 0.42, ang=-35)


def g_fire_brilliant_flame(d):
    d.ellipse(box(-1.8, -9.6, 9.8, 2.0), fill=255)
    d.ellipse(box(1.0, -6.8, 7.0, -0.8), fill=140)
    for (x0, y0, x1, y1, w) in ((-1.0, -2.0, -10.6, 8.2, 2.4), (1.6, 1.2, -6.4, 10.4, 2.0), (-2.4, -5.6, -11.0, 1.6, 1.6)):
        d.polygon(P([(x0 + w * 0.5, y0 - w * 0.5), (x0 - w * 0.5, y0 + w * 0.5), (x1, y1)]), fill=255)


def _petal(length, width, n=14):
    return [((width / 2.0) * math.sin(t), -length * (1 - math.cos(t)) / 2.0) for t in (i * 2 * math.pi / n for i in range(n))]


def g_fire_crimson_lotus(d):
    for k in range(8):
        d.polygon(P(rot(_petal(10.6, 4.0), k * 45)), fill=255)
    for k in range(8):
        d.polygon(P(rot(_petal(6.6, 2.6), k * 45 + 22.5)), fill=140)
    d.ellipse(box(-2.4, -2.4, 2.4, 2.4), fill=255)
    d.ellipse(box(-1.1, -1.1, 1.1, 1.1), fill=140)


# ---------------------------------------------------------------------------------------------------- lightning glyphs
def g_lightning_iron_fist(d):
    fist(d, 0.0, -0.4, 0.86)
    bolt(d, -7.6, -5.6, 0.42, ang=-14, facet=False)
    bolt(d, 7.6, -5.6, 0.42, ang=14, facet=False)
    bolt(d, 0.0, -9.4, 0.3, ang=0, facet=False)


def g_lightning_roar(d):
    d.ellipse(box(-11.6, -1.8, -7.6, 2.2), fill=255)
    for k, (ex, ey) in enumerate(((10.4, -8.4), (11.6, 0.2), (10.4, 8.4))):
        pts, x0, y0 = [], -8.4, 0.2
        n = 5
        for i in range(n + 1):
            t = i / n
            off = (1.6 if i % 2 else -1.6) * (1 - t * 0.4) if 0 < i < n else 0
            pts.append((x0 + (ex - x0) * t + off * 0.4, y0 + (ey - y0) * t + off))
        d.line(P(pts), fill=255, width=int(1.7 * U), joint="curve")
        d.line(P(pts), fill=140, width=int(0.6 * U), joint="curve")


def g_lightning_breakdown_fist(d):
    d.ellipse(box(-10.6, -10.6, 10.6, 10.6), outline=255, width=int(1.5 * U))
    for k in range(12):
        a = k * math.pi / 6
        c, s = math.cos(a), math.sin(a)
        d.line(P([(c * 8.2, s * 8.2), (c * 9.6, s * 9.6)]), fill=140, width=int(0.9 * U))
    fist(d, 0.0, -0.8, 0.6)


def g_lightning_halberd(d):
    ang = 42
    d.polygon(P(rot([(-0.9, 12), (0.9, 12), (0.9, -3.2), (-0.9, -3.2)], ang)), fill=255)
    d.polygon(P(rot([(-5.0, -2.6), (5.0, -2.6), (5.0, -4.2), (-5.0, -4.2)], ang)), fill=255)
    for px, hgt in ((-4.4, 6.0), (0.0, 9.0), (4.4, 6.0)):
        d.polygon(P(rot([(px - 1.0, -3.6), (px + 1.0, -3.6), (px, -3.6 - hgt)], ang)), fill=255)
    d.polygon(P(rot([(-0.4, 10.0), (0.4, 10.0), (0.4, -2.0), (-0.4, -2.0)], ang)), fill=140)
    bolt(d, -6.2, 4.6, 0.36, ang=20, facet=False)


def g_lightning_jaw(d):
    d.rounded_rectangle(box(-7.0, -10.6, 7.0, -1.6), radius=int(3.4 * U), fill=255)
    d.line(P([(-2.6, -9.2), (-2.6, -3.0)]), fill=140, width=int(0.7 * U))
    d.line(P([(0.8, -9.6), (0.8, -3.0)]), fill=140, width=int(0.7 * U))
    d.line(P([(4.2, -9.2), (4.2, -3.0)]), fill=140, width=int(0.7 * U))
    for k, a in enumerate((-70, -40, -10, 10, 40, 70)):
        c, sn = math.cos(math.radians(a)), math.sin(math.radians(a))
        d.line(P([(c * 4.0 * 0 + sn * 2.0, 2.4), (sn * 11.0, 2.4 + c * 8.6)]), fill=255 if k % 2 == 0 else 200, width=int(1.1 * U))
    d.arc(box(-11.0, 5.2, 11.0, 12.4), 190, 350, fill=255, width=int(1.2 * U))


def g_lightning_flash(d):
    bolt(d, 2.0, 0.0, 1.0, ang=-18)
    bolt(d, -3.6, 0.6, 0.8, ang=-18, facet=False)
    for (x0, x1, y) in ((-11.0, -5.8, -6.8), (-11.6, -6.4, 0.6), (-10.4, -5.4, 7.0)):
        d.line(P([(x0, y), (x1, y)]), fill=200, width=int(0.9 * U))


def g_lightning_thunder_palace(d):
    for k in range(5):
        a = math.radians(-160 + k * 35)
        x, y = math.cos(a) * 8.6, math.sin(a) * 4.6 - 5.4
        d.ellipse(box(x - 2.3, y - 2.3, x + 2.3, y + 2.3), fill=255)
        d.ellipse(box(x - 1.0, y - 1.0, x + 1.0, y + 1.0), fill=140)
    bolt(d, -3.8, 4.4, 0.36, ang=10, facet=False)
    bolt(d, 0.0, 6.2, 0.44, ang=0)
    bolt(d, 3.8, 4.4, 0.36, ang=-10, facet=False)


def g_lightning_roaring_thunder(d):
    pts = []
    for i in range(24):
        a = i * math.pi / 12 - math.pi / 2
        r = 11.2 if i % 2 == 0 else 6.2
        pts.append((math.cos(a) * r, math.sin(a) * r))
    d.polygon(P(pts), fill=255)
    d.ellipse(box(-6.4, -6.4, 6.4, 6.4), fill=140)
    bolt(d, 0.0, 0.0, 0.62, ang=-8, facet=False)
    d.arc(box(-11.6, -11.6, 11.6, 11.6), 200, 340, fill=255, width=int(1.2 * U))


SKILLS = [
    # file name,                       colors, glyph,                          metal,       seed
    ("ft_fire_iron_fist",              FIRE, g_fire_iron_fist,                EMBER_METAL, 401),
    ("ft_fire_roar",                   FIRE, g_fire_roar,                     EMBER_METAL, 402),
    ("ft_fire_claw",                   FIRE, g_fire_claw,                     EMBER_METAL, 403),
    ("ft_fire_wing_attack",            FIRE, g_fire_wing_attack,              EMBER_METAL, 404),
    ("ft_fire_sword_horn",             FIRE, g_fire_sword_horn,               EMBER_METAL, 405),
    ("ft_fire_brilliant_flame",        FIRE, g_fire_brilliant_flame,          EMBER_METAL, 406),
    ("ft_fire_crimson_lotus",          FIRE, g_fire_crimson_lotus,            EMBER_METAL, 407),
    ("ft_lightning_iron_fist",         BOLT, g_lightning_iron_fist,           STEEL_METAL, 411),
    ("ft_lightning_roar",              BOLT, g_lightning_roar,                STEEL_METAL, 412),
    ("ft_lightning_breakdown_fist",    BOLT, g_lightning_breakdown_fist,      STEEL_METAL, 413),
    ("ft_lightning_halberd",           BOLT, g_lightning_halberd,             STEEL_METAL, 414),
    ("ft_lightning_jaw",               BOLT, g_lightning_jaw,                 STEEL_METAL, 415),
    ("ft_lightning_flash",             BOLT, g_lightning_flash,               STEEL_METAL, 416),
    ("ft_lightning_thunder_palace",    BOLT, g_lightning_thunder_palace,      STEEL_METAL, 417),
    ("ft_lightning_roaring_thunder",   BOLT, g_lightning_roaring_thunder,     STEEL_METAL, 418),
]


def make(name, colors, glyph, metal, seed):
    G.MAGIC[name] = (name, colors[0], colors[1], "", "")
    G.GLYPHS[name] = glyph
    img = G.bg_void(colors[0])
    G.streaks(img, colors[0], seed)
    G.particles(img, colors[0], 14, seed + 1)
    G.over(img, G.symbol_layer(name, 0.92))
    return G.reduce(G.frame(img, metal, rim_glow=colors[0], corner="spike"))


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    sheet = "--sheet" in sys.argv
    todo = [s for s in SKILLS if not args or s[0] in args]
    os.makedirs(OUT, exist_ok=True)
    made = []
    for name, colors, glyph, metal, seed in todo:
        im = make(name, colors, glyph, metal, seed)
        G.save(im, os.path.join(OUT, name + ".png"))
        made.append((name, im))
        print("icon:", name)
    if sheet:
        cell, pad = 96, 8
        cols = 5
        rows = (len(made) + cols - 1) // cols
        sh = Image.new("RGBA", (cols * (cell + pad) + pad, rows * (cell + 18 + pad) + pad), (24, 22, 28, 255))
        d = ImageDraw.Draw(sh)
        for i, (name, im) in enumerate(made):
            x, y = pad + (i % cols) * (cell + pad), pad + (i // cols) * (cell + 18 + pad)
            sh.alpha_composite(im.resize((cell, cell), Image.NEAREST), (x, y))
            d.text((x, y + cell + 2), name.replace("ft_", ""), fill=(220, 220, 220, 255))
        os.makedirs(G.PREVIEW, exist_ok=True)
        sh.save(os.path.join(G.PREVIEW, "ft_sheet.png"))
        print("sheet:", os.path.join(G.PREVIEW, "ft_sheet.png"))


if __name__ == "__main__":
    main()
