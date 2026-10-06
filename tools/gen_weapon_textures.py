"""Black Clover weapons (0.28), after the owner's weapons art pack (tools/art/weapons):

    python tools/gen_weapon_textures.py   ->  textures/item/<weapon>.png (128x128) + models/item/<weapon>.json
                                               build/weapons/preview.png (not shipped)

  demon_slayer_sword     Asta's first sword: a huge, broad, rusted black greatsword with chipped edges, a pointed tip, the base of
                         the blade angled inward to the guard, a narrow fuller joining hilt and blade, a long grip, round pommel.
  demon_dweller_sword    long and slender black blade with a stepped central fuller and black markings (they glow with the colour of
                         the magic it absorbed), a four-sided ornate guard, a spiral grip and a sphere pommel.
  demon_destroyer_sword  broad black blade with the ragged bite torn out of one edge near the guard; the blade end flares wide and
                         carries a clover symbol.
  licht_dweller_sword    Licht's white Demon-Dweller (Sword Magic): same silhouette in white steel with gold-white markings.
  licht_destroyer_sword  Licht's white Demon-Destroyer: same silhouette in white steel with a pale clover on the blade end.
  demon_slasher_katana   Asta's katana: a slim curved black blade with a crimson anti-magic edge, round guard, red-wrapped grip.
  miasma_infused_katana  Yami's katana: a curved steel blade with a wavy temper line, dark round guard, black-and-white wrapped
                         grip (Dark Cloaked Blade spells coat it in darkness).

Each is a single 128x128 sprite on the vanilla diagonal (grip bottom-left, tip top-right). Minecraft extrudes item sprites
into 3D, so every chip, bite and curve keeps real thickness. Drawn at 4x, reduced, alpha snapped. Deterministic.
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.join(os.path.dirname(__file__), "..")
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "item")
MODELS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "models", "item")
PREVIEW = os.path.join(ROOT, "build", "weapons")
N = 128
SS = 4
S = N * SS

# the diagonal: u runs from the pommel (bottom-left) to the tip (top-right), v across the blade (+v = towards bottom-right)
START = np.array([0.07 * S, 0.93 * S])
DIR = np.array([1.0, -1.0]) / math.sqrt(2)
PERP = np.array([1.0, 1.0]) / math.sqrt(2)
LEN = math.dist(START, (0.93 * S, 0.07 * S))


def P(u, v):
    """Weapon units (u: 0..1 along the length, v: blade widths in fractions of the length) -> canvas point."""
    p = START + DIR * (u * LEN) + PERP * (v * LEN)
    return float(p[0]), float(p[1])


def poly(points):
    return [P(u, v) for u, v in points]


YY, XX = np.mgrid[0:S, 0:S].astype(np.float32)
_rel = np.dstack([XX - START[0], YY - START[1]])
U = (_rel @ DIR) / LEN            # per-pixel weapon coordinates
V = (_rel @ PERP) / LEN


def noise(cell, seed):
    rng = np.random.default_rng(seed)
    g = rng.random((S // cell + 2, S // cell + 2)).astype(np.float32)
    fx, fy = XX / cell, YY / cell
    x0, y0 = fx.astype(int), fy.astype(int)
    tx, ty = fx - x0, fy - y0
    tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
    a = g[y0, x0] * (1 - tx) + g[y0, x0 + 1] * tx
    b = g[y0 + 1, x0] * (1 - tx) + g[y0 + 1, x0 + 1] * tx
    return a * (1 - ty) + b * ty


def mask(points):
    im = Image.new("L", (S, S), 0)
    ImageDraw.Draw(im).polygon(poly(points), fill=255)
    return np.asarray(im).astype(np.float32) / 255


def layer(canvas, m, rgb):
    """Paints rgb (HxWx3 float or a colour tuple 0..1) where mask m is set."""
    c = np.broadcast_to(np.asarray(rgb, np.float32), (S, S, 3)) if np.ndim(rgb) == 1 else rgb
    canvas[..., :3] = canvas[..., :3] * (1 - m[..., None]) + c * m[..., None]
    canvas[..., 3] = np.maximum(canvas[..., 3], m)


def bevel_shade(base, v0, v1, ridge=0.5, light=1.25, dark=0.6):
    """Blade cross-section shading between v0 and v1: bright near the ridge (fraction 'ridge'), darker at the edges."""
    t = np.clip((V - v0) / max(1e-6, v1 - v0), 0, 1)
    d = np.abs(t - ridge) / max(ridge, 1 - ridge)
    f = light - (light - dark) * d
    return np.clip(np.asarray(base, np.float32) * f[..., None], 0, 1)


def grip(canvas, u0, u1, w, wrap=(0.12, 0.1, 0.1), cord=(0.55, 0.5, 0.45), seed=1):
    m = mask([(u0, -w), (u1, -w), (u1, w), (u0, w)])
    diamonds = (np.sin((U * 120 + V * 120)) > 0.35) ^ (np.sin((U * 120 - V * 120)) > 0.35)
    col = np.where(diamonds[..., None], np.asarray(cord, np.float32), np.asarray(wrap, np.float32))
    col = col * (0.75 + 0.35 * noise(6, seed))[..., None]
    layer(canvas, m, bevel_shade(np.ones(3), -w, w, light=1.1, dark=0.55) * col)


def spiral_grip(canvas, u0, u1, w, dark=(0.1, 0.09, 0.1), light=(0.42, 0.4, 0.42), seed=3):
    """A grip wound in a spiral (Demon-Dweller): diagonal bands running round the handle."""
    m = mask([(u0, -w), (u1, -w), (u1, w), (u0, w)])
    band = 0.5 + 0.5 * np.sin(U * 260 + V * 900)
    col = np.asarray(dark, np.float32) * (1 - band[..., None]) + np.asarray(light, np.float32) * band[..., None]
    col = col * (0.8 + 0.3 * noise(5, seed))[..., None]
    layer(canvas, m, np.clip(bevel_shade(np.ones(3), -w, w, light=1.15, dark=0.55) * col, 0, 1))


def clover(canvas, u, r, rgb, inner=None):
    """A three-leaf clover symbol centred on the blade axis at u (Demon-Destroyer's blade end)."""
    cx, cy = P(u, 0)
    im = Image.new("L", (S, S), 0)
    d = ImageDraw.Draw(im)
    R = r * LEN
    for k in range(3):                                                   # three leaves around the centre, one pointing tipward
        a = math.radians(-45 + k * 120)
        lx, ly = cx + math.cos(a) * R * 0.95, cy + math.sin(a) * R * 0.95
        d.ellipse([lx - R * 0.72, ly - R * 0.72, lx + R * 0.72, ly + R * 0.72], fill=255)
    a = math.radians(135)                                                # the stem, towards the guard
    d.line([(cx, cy), (cx + math.cos(a) * R * 2.0, cy + math.sin(a) * R * 2.0)], fill=255, width=max(2, int(R * 0.35)))
    m = np.asarray(im).astype(np.float32) / 255
    layer(canvas, m, np.asarray(rgb, np.float32) * (0.85 + 0.25 * noise(3, 77))[..., None])
    if inner is not None:
        layer(canvas, m * (np.asarray(im.filter(ImageFilter.MinFilter(9))).astype(np.float32) / 255), inner)


def disc(canvas, u, r, rgb, hole=0.0, seed=2):
    cx, cy = P(u, 0)
    im = Image.new("L", (S, S), 0)
    d = ImageDraw.Draw(im)
    R = r * LEN
    d.ellipse([cx - R, cy - R, cx + R, cy + R], fill=255)
    if hole > 0:
        h = hole * LEN
        d.ellipse([cx - h, cy - h, cx + h, cy + h], fill=0)
    m = np.asarray(im).astype(np.float32) / 255
    shade = np.clip(1.15 - 0.6 * ((YY - cy) - (XX - cx)) / (2 * R + 1e-6), 0.5, 1.3)
    layer(canvas, m, np.asarray(rgb, np.float32) * shade[..., None] * (0.85 + 0.25 * noise(4, seed))[..., None])


def chips(points_top, points_bottom, depth, count, seed, u0, u1):
    """Edge outlines with irregular bites: returns (top edge list, bottom edge list) of (u, v)."""
    rng = np.random.default_rng(seed)
    cuts = sorted(rng.uniform(u0, u1, count))
    def edge(fn, sign):
        out = []
        steps = 90
        for i in range(steps + 1):
            u = u0 + (u1 - u0) * i / steps
            v = fn(u)
            for j, c in enumerate(cuts):
                wdt = 0.008 + 0.014 * (0.5 + 0.5 * math.sin(c * 53))
                k = abs(u - c)
                if k < wdt:                                         # a jagged triangular bite
                    jag = 0.6 + 0.4 * math.sin(u * 900 + j)
                    v -= sign * depth * (1 - k / wdt) * jag * (0.6 + 0.4 * math.sin(c * 97))
            out.append((u, v))
        return out
    return edge(points_top, -1), edge(points_bottom, 1)


def rust(canvas, m, seed, amount=0.35):
    n = noise(10, seed) * 0.6 + noise(3, seed + 1) * 0.4
    spots = np.clip((n - 0.55) * 4, 0, 1) * amount
    rustc = np.array([0.32, 0.17, 0.10], np.float32)
    canvas[..., :3] = canvas[..., :3] * (1 - (spots * m)[..., None]) + rustc * (spots * m)[..., None]


def scratches(canvas, m, seed, n=24, col=(0.55, 0.55, 0.58)):
    rng = np.random.default_rng(seed)
    im = Image.new("L", (S, S), 0)
    d = ImageDraw.Draw(im)
    for _ in range(n):
        u = rng.uniform(0.3, 0.95)
        v = rng.uniform(-0.06, 0.06)
        L = rng.uniform(0.01, 0.04)
        a = rng.uniform(-0.6, 0.6)
        d.line([P(u, v), P(u + L * math.cos(a), v + L * math.sin(a))], fill=int(rng.uniform(90, 200)), width=2)
    sm = np.asarray(im).astype(np.float32) / 255 * m
    canvas[..., :3] = canvas[..., :3] * (1 - sm[..., None]) + np.asarray(col, np.float32) * sm[..., None]


# ---------------------------------------------------------------------------------------------------- the weapons
BLACK = (0.13, 0.13, 0.15)
EDGE = (0.42, 0.42, 0.46)


def demon_slayer():
    c = np.zeros((S, S, 4), np.float32)
    grip(c, 0.05, 0.30, 0.028, seed=11)
    disc(c, 0.04, 0.042, (0.16, 0.15, 0.16), seed=12)
    # short, thick square guard
    layer(c, mask([(0.29, -0.09), (0.345, -0.09), (0.345, 0.09), (0.29, 0.09)]), bevel_shade((0.2, 0.19, 0.2), -0.09, 0.09, light=1.2, dark=0.5))
    # broad blade, the base angled inward to the guard, very slightly widening, a pointed tip; edges chipped
    def top(u):
        return -0.085 - 0.012 * (u - 0.34) + max(0.0, 0.42 - u) * 0.55       # base points inward
    def bot(u):
        return 0.085 + 0.008 * (u - 0.34) - max(0.0, 0.42 - u) * 0.55
    t, b = chips(top, bot, 0.014, 14, 21, 0.345, 0.9)
    outline = t + [(1.0, 0.0)] + b[::-1]
    m = mask(outline)
    layer(c, m, bevel_shade(BLACK, -0.09, 0.09, ridge=0.45, light=1.5, dark=0.75))
    # worn steel along both edges
    edge_band = m * ((np.abs(V) > 0.078) | (U > 0.93)).astype(np.float32)
    layer(c, edge_band, bevel_shade(EDGE, -0.1, 0.1, light=1.2, dark=0.8))
    # the narrow fuller that joins hilt and blade
    layer(c, m * (np.abs(V) < 0.008).astype(np.float32) * (U < 0.62).astype(np.float32), (0.07, 0.07, 0.08))
    rust(c, m, 23, 0.45)
    scratches(c, m, 24)
    return c


def demon_dweller(blade=BLACK, edge=EDGE, metal=(0.2, 0.2, 0.22), mark=(0.04, 0.04, 0.05), grip_cols=((0.1, 0.09, 0.1), (0.42, 0.4, 0.42)),
                  rusty=True):
    c = np.zeros((S, S, 4), np.float32)
    spiral_grip(c, 0.07, 0.30, 0.018, dark=grip_cols[0], light=grip_cols[1], seed=31)
    disc(c, 0.05, 0.03, metal, seed=32)                                  # sphere pommel
    # four-sided ornate guard: a diamond with flared points across the blade and a short point either way along it
    layer(c, mask([(0.315, -0.15), (0.335, -0.06), (0.36, 0.0), (0.335, 0.06), (0.315, 0.15), (0.295, 0.06), (0.27, 0.0), (0.295, -0.06)]),
          bevel_shade(metal, -0.15, 0.15, light=1.25, dark=0.55))
    # long slender blade tapering to a sharp point
    outline = [(0.355, -0.032), (0.9, -0.026), (1.0, 0.0), (0.9, 0.026), (0.355, 0.032)]
    m = mask(outline)
    layer(c, m, bevel_shade(blade, -0.032, 0.032, light=1.5, dark=0.7))
    # stepped central fuller (the notched groove)
    groove = m * (np.abs(V) < 0.009).astype(np.float32) * ((U > 0.38) & (U < 0.86)).astype(np.float32)
    notch = (np.sin(U * 260) > 0.6).astype(np.float32)
    layer(c, groove, np.asarray(blade, np.float32) * 2.2 * (0.8 + 0.4 * notch)[..., None] if not rusty else np.array([0.3, 0.3, 0.34]) * (0.8 + 0.4 * notch)[..., None])
    # the markings: thin chevrons either side of the fuller
    chev = (np.abs(np.abs(V) * 9 - ((U * 40) % 1.0)) < 0.09) & (np.abs(V) > 0.011) & (np.abs(V) < 0.02) & (U > 0.4) & (U < 0.84)
    layer(c, m * chev.astype(np.float32), mark)
    layer(c, m * (np.abs(V) > 0.024).astype(np.float32), bevel_shade(edge, -0.035, 0.035))
    if rusty:
        rust(c, m, 33, 0.25)
    return c


def licht_dweller():
    return demon_dweller(blade=(0.82, 0.84, 0.88), edge=(0.97, 0.97, 0.99), metal=(0.85, 0.8, 0.62), mark=(0.98, 0.9, 0.55),
                         grip_cols=((0.78, 0.76, 0.72), (0.98, 0.96, 0.9)), rusty=False)


def demon_destroyer(blade=BLACK, edge=EDGE, metal=(0.2, 0.19, 0.2), symbol=(0.05, 0.05, 0.06), rusty=True):
    c = np.zeros((S, S, 4), np.float32)
    grip(c, 0.06, 0.30, 0.024, seed=41)
    disc(c, 0.05, 0.036, metal, seed=42)
    layer(c, mask([(0.29, -0.11), (0.335, -0.125), (0.345, 0.0), (0.335, 0.125), (0.29, 0.11)]),
          bevel_shade(metal, -0.125, 0.125, light=1.2, dark=0.5))
    # broad blade whose end flares wide; a ragged bite torn out of the top edge near the guard
    top = []
    for i in range(121):
        u = 0.345 + 0.5 * i / 120
        v = -0.075 - 0.05 * max(0.0, (u - 0.62) / 0.23) ** 1.5             # the blade end widens
        if 0.40 < u < 0.54:                                             # the bite: a ragged chunk torn out of the edge
            k = (u - 0.40) / 0.14
            v += 0.062 * math.sin(math.pi * k) ** 0.7 * (0.7 + 0.3 * math.sin(u * 400))
        top.append((u, v))
    bottom = [(0.845, 0.125)] + [(0.845 - 0.5 * i / 30, 0.075 + 0.05 * max(0.0, (0.845 - 0.5 * i / 30 - 0.62) / 0.23) ** 1.5) for i in range(1, 31)]
    outline = top + [(0.92, -0.11), (1.0, 0.0), (0.92, 0.11)] + bottom
    m = mask(outline)
    layer(c, m, bevel_shade(blade, -0.13, 0.13, ridge=0.5, light=1.45, dark=0.7))
    layer(c, m * ((np.abs(V) > 0.062 + 0.05 * np.clip((U - 0.62) / 0.23, 0, 1) ** 1.5) | (U > 0.95)).astype(np.float32), bevel_shade(edge, -0.13, 0.13))
    # a dark channel down the middle, ending at the clover on the wide blade end
    layer(c, m * (np.abs(V) < 0.006).astype(np.float32) * (U < 0.7).astype(np.float32), symbol)
    clover(c, 0.8, 0.032, symbol)
    if rusty:
        rust(c, m, 43, 0.35)
        scratches(c, m, 44, 18)
    return c


def licht_destroyer():
    return demon_destroyer(blade=(0.84, 0.86, 0.9), edge=(0.98, 0.98, 1.0), metal=(0.86, 0.8, 0.6), symbol=(0.95, 0.88, 0.55), rusty=False)


def katana(blade_rgb, edge_rgb, temper=None, wrap=(0.12, 0.1, 0.1), cord=(0.55, 0.12, 0.12), guard=(0.16, 0.15, 0.16), seed=50):
    c = np.zeros((S, S, 4), np.float32)
    grip(c, 0.08, 0.34, 0.02, wrap=wrap, cord=cord, seed=seed)
    disc(c, 0.355, 0.05, guard, hole=0.0, seed=seed + 1)                 # round tsuba
    curve = lambda u: -0.075 * ((u - 0.37) / 0.63) ** 2                  # sori: the back curves up towards the tip
    w = 0.022
    back = [(0.37 + 0.6 * i / 40, curve(0.37 + 0.6 * i / 40) - w) for i in range(41)]
    edge = [(0.37 + 0.6 * i / 40, curve(0.37 + 0.6 * i / 40) + w * (1 - 0.3 * i / 40)) for i in range(41)]
    tip = (1.0, curve(1.0) - 0.004)
    m = mask(back + [tip] + edge[::-1])
    cv = curve(np.clip(U, 0.37, 1.0))
    t = np.clip((V - (cv - w)) / (2 * w), 0, 1)
    col = np.asarray(blade_rgb, np.float32) * (1.35 - 0.6 * np.abs(t - 0.35))[..., None]
    layer(c, m, np.clip(col, 0, 1))
    if temper is not None:                                               # hamon: a wavy line between body and edge
        wave = cv + w * (0.25 + 0.18 * np.sin(U * 140))
        band = m * (V > wave).astype(np.float32)
        layer(c, band, np.asarray(temper, np.float32) * (0.95 + 0.1 * noise(3, seed + 2))[..., None])
    layer(c, m * (V > cv + w * 0.62).astype(np.float32), np.asarray(edge_rgb, np.float32) * np.ones((S, S, 1)))
    return c


def demon_slasher():
    return katana((0.11, 0.1, 0.12), (0.78, 0.08, 0.12), temper=None, cord=(0.6, 0.08, 0.1), seed=51)


def yami_katana():
    return katana((0.36, 0.38, 0.42), (0.86, 0.88, 0.9), temper=(0.7, 0.72, 0.76), wrap=(0.08, 0.08, 0.09),
                  cord=(0.82, 0.82, 0.8), guard=(0.12, 0.11, 0.1), seed=61)


WEAPONS = {
    # item id: (painter, third-person scale, first-person scale)
    "demon_slayer_sword": (demon_slayer, 1.9, 1.25),
    "demon_dweller_sword": (demon_dweller, 1.65, 1.1),
    "demon_destroyer_sword": (demon_destroyer, 1.75, 1.15),
    "demon_slasher_katana": (demon_slasher, 1.45, 1.0),
    "miasma_infused_katana": (yami_katana, 1.45, 1.0),
    "licht_dweller_sword": (licht_dweller, 1.65, 1.1),
    "licht_destroyer_sword": (licht_destroyer, 1.75, 1.15),
}


def reduce(c):
    im = Image.fromarray((np.clip(c, 0, 1) * 255).astype(np.uint8), "RGBA")
    # outline: a 1 px darker rim so the silhouette reads at 128 px
    a = np.asarray(im)[..., 3].astype(np.float32) / 255
    small = im.resize((N, N), Image.LANCZOS)
    arr = np.asarray(small).copy()
    arr[..., 3] = np.where(arr[..., 3] > 110, 255, 0)
    return Image.fromarray(arr, "RGBA")


def model(name, tp, fp):
    """handheld-style model with a bigger in-hand size for these large weapons."""
    lift = 4 + (tp - 1) * 3.2
    return {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"nusmp:item/{name}"},
        "display": {
            "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, lift, 0.5], "scale": [tp * 0.85] * 3},
            "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, lift, 0.5], "scale": [tp * 0.85] * 3},
            "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2 + (fp - 1) * 2, 1.13], "scale": [fp * 0.68] * 3},
            "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2 + (fp - 1) * 2, 1.13], "scale": [fp * 0.68] * 3},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
            "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
            "gui": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
        },
    }


def main():
    import json
    os.makedirs(PREVIEW, exist_ok=True)
    tiles = []
    for name, (fn, tp, fp) in WEAPONS.items():
        im = reduce(fn())
        im.save(os.path.join(TEX, name + ".png"), optimize=True)
        with open(os.path.join(MODELS, name + ".json"), "w", newline="\n") as f:
            json.dump(model(name, tp, fp), f, indent=2)
            f.write("\n")
        tiles.append((name, im))
    Z = 3
    sheet = Image.new("RGB", (len(tiles) * (N * Z + 8) + 8, N * Z + 30), (40, 44, 52))
    d = ImageDraw.Draw(sheet)
    for i, (name, im) in enumerate(tiles):
        x = 8 + i * (N * Z + 8)
        sheet.paste(im.resize((N * Z, N * Z), Image.NEAREST), (x, 22), im.resize((N * Z, N * Z), Image.NEAREST))
        d.text((x, 6), name, fill=(235, 215, 160))
    sheet.save(os.path.join(PREVIEW, "preview.png"))
    print("wrote", len(tiles), "weapons")


if __name__ == "__main__":
    main()
