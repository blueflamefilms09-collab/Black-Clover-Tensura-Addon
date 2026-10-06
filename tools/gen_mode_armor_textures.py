"""Transformation armour overlays (0.38): pixel-art textures for the five mode overlays drawn by ModeArmorLayer.

    python tools/gen_mode_armor_textures.py  ->  textures/entity/mode/<mode>_<part>.png

Per mode (wind = Yuno's Wind Spirit Dive, fire = Fire Spirit Dive / Salamander, demon = Asta's Anti-Magic Demon Mode,
lightning = Luck's Lightning God Mode, valkyrie = Noelle's Valkyrie Dress):
    _plate  32x32  opaque armour / coat panel, used on every face of the overlay boxes
    _glow   32x32  emissive lines over the plate (alpha), drawn full-bright in a second pass on the same boxes
    _wing   64x64  alpha-cut wing / wing frame (wind, fire, demon, valkyrie), drawn translucent
    _crest  32x32  alpha-cut crown blade (wind), flame strand (fire), horn plate (demon), bolt (lightning), helmet wing (valkyrie)
plus wind_fur (fur trim, 32x32), valkyrie_lance (spiral drill, 32x32) and demon_wisp (8x8).
"""
import math
import os

import numpy as np
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "entity", "mode")


def rng(seed):
    return np.random.default_rng(seed)


def img(rgb, a):
    return Image.fromarray(np.dstack([np.clip(rgb, 0, 255).astype(np.uint8), np.clip(a, 0, 255).astype(np.uint8)]), "RGBA")


def grid(s):
    return np.mgrid[0:s, 0:s].astype(np.float32)


def plate(base, hi, lo, seed, style, s=32):
    """An armour panel: 1-texel dark rim, 1-texel highlight inside it, a style motif, light noise."""
    yy, xx = grid(s)
    r = rng(seed)
    col = np.ones((s, s, 3), np.float32) * np.array(base, np.float32)
    col *= (0.92 + 0.08 * r.random((s, s)))[..., None]
    shade = 1.0 + 0.18 * (1 - (xx + yy) / (2 * s)) - 0.09
    col *= shade[..., None]
    if style == "coat":            # Yuno's coat: vertical seams and a gold button line
        col[(xx.astype(int) % 11) == 5] = np.array(lo, np.float32)
        col[((xx.astype(int) == 15) | (xx.astype(int) == 16)) & (yy.astype(int) % 6 == 2)] = hi
    elif style == "scale":         # dragon scales
        for y in range(0, s, 4):
            for x in range(-2 if (y // 4) % 2 else 0, s, 4):
                for dx in range(4):
                    if 0 <= x + dx < s and y + 3 < s:
                        col[y + 3 - (1 if dx in (0, 3) else 0), x + dx] = lo
    elif style == "obsidian":      # cracked black plate
        for _ in range(7):
            x, y = r.integers(2, s - 2, 2)
            for _ in range(10):
                col[y % s, x % s] = lo
                x += r.integers(-1, 2); y += r.integers(0, 2)
    elif style == "runic":         # angular chevrons
        m = (np.abs(xx - s / 2 + 0.5) + yy) % 10 < 1.2
        col[m] = lo
    elif style == "crystal":       # faceted crystal: diagonal facets
        f = ((xx + yy) // 6 + (xx - yy) // 7) % 2 == 0
        col[f] *= 1.15
        col[((xx + yy) % 6 < 1)] = hi
    rim = (xx < 1) | (yy < 1) | (xx > s - 2) | (yy > s - 2)
    inner = ((xx == 1) | (yy == 1)) & ~rim
    col[inner] = col[inner] * 0.5 + np.array(hi, np.float32) * 0.5
    col[rim] *= 0.55
    return img(col, np.full((s, s), 255))


def glow(color, style, s=32):
    """Emissive lines (alpha) over the plate."""
    yy, xx = grid(s)
    a = np.zeros((s, s), np.float32)
    if style == "runes":           # Luck: runic circuit lines
        a[((yy.astype(int) % 8) == 4) & (xx > 3) & (xx < s - 4)] = 255
        a[((xx.astype(int) % 10) == 6) & ((yy.astype(int) % 8) >= 4)] = 255
        a[(np.abs(xx - s / 2 + 0.5) < 1) & (yy < 12)] = 255
    elif style == "veins":         # fire / demon: molten cracks
        r = rng(len(color) * 7 + sum(color))
        for _ in range(5):
            x, y = r.integers(3, s - 3, 2)
            for _ in range(14):
                a[y % s, x % s] = 255
                x += r.integers(-1, 2); y += r.integers(-1, 2)
    elif style == "trim":          # wind / valkyrie: edge glow
        a[(xx == 2) | (yy == 2) | (xx == s - 3) | (yy == s - 3)] = 200
        a[(np.abs(xx - yy) < 0.6) & (xx > 6) & (xx < s - 6)] = 140
    rgb = np.ones((s, s, 3), np.float32) * np.array(color, np.float32)
    return img(rgb, a)


def wing(kind, s=64):
    """Wing shapes in a 64x64 alpha-cut sprite, root at the bottom-left corner (u=0, v=1), tip toward the top right."""
    yy, xx = grid(s)
    a = np.zeros((s, s), np.float32)
    col = np.zeros((s, s, 3), np.float32)
    # polar coordinates around the root
    rx, ry = xx, (s - 1) - yy
    rad = np.sqrt(rx * rx + ry * ry) / s
    ang = np.arctan2(ry, rx)                                    # 0 = along the bottom, pi/2 = up the left side
    if kind == "wind":             # Yuno: translucent green blade-feathers, fanned
        feathers = 5
        k = np.clip(ang / (math.pi / 2), 0, 0.999)
        fi = np.floor(k * feathers)
        fk = k * feathers - fi
        length = 0.62 + 0.36 * np.sin((fi + 1) / (feathers + 1) * math.pi)
        inside = (rad < length * (1 - 0.5 * np.abs(fk - 0.5) ** 1.5)) & (rad > 0.06)
        a[inside] = 150 + 90 * (rad[inside] / length[inside])
        col[...] = [120, 255, 170]
        a[inside & ((fk < 0.08) | (fk > 0.92))] = 245                  # bright feather edges
        col[inside & ((fk < 0.08) | (fk > 0.92))] = [225, 255, 235]
    elif kind == "fire":           # Salamander: bone frame with a flame membrane
        k = ang / (math.pi / 2)
        bones = [0.12, 0.42, 0.72, 0.95]
        for b in bones:
            m = (np.abs(k - b) < 0.035 / np.maximum(rad, 0.1)) & (rad < 0.95) & (rad > 0.04)
            a[m] = 255; col[m] = [120, 30, 18]
        memb = (rad < 0.92 - 0.12 * np.abs(np.sin(k * math.pi * 3))) & (k > 0.12) & (k < 0.95) & (a == 0) & (rad > 0.05)
        heat = np.clip(1 - rad, 0, 1)
        col[memb] = (np.array([255, 200, 60]) * heat[memb][..., None] + np.array([255, 70, 20]) * (1 - heat[memb][..., None]))
        a[memb] = 170
    elif kind == "demon":          # Asta: one tattered black wing (bat-like membrane between 4 bones, ragged scallops)
        k = ang / (math.pi / 2)
        r = rng(11)
        scallop = 0.96 - 0.3 * np.abs(np.sin(k * math.pi * 4)) ** 0.7
        ragged = 0.06 * r.random((s, s))
        m = (rad < scallop - ragged) & (rad > 0.04) & (k > 0.03) & (k < 0.99)
        m &= ~((r.random((s, s)) < 0.06) & (rad > 0.45))           # torn holes
        a[m] = 255
        col[m] = [24, 20, 26]
        bone = (np.abs(np.sin(k * math.pi * 4)) < 0.09 + 0.03 / np.maximum(rad, 0.1)) & (rad < 0.98) & (rad > 0.04)
        a[bone] = 255
        col[bone] = [62, 44, 54]
        rim = m & (rad > scallop - ragged - 0.06)
        col[rim] = [96, 12, 22]
    elif kind == "valkyrie":       # Noelle: avian water wing, layered feathers
        k = ang / (math.pi / 2)
        layers = [(0.98, [150, 220, 255]), (0.75, [90, 180, 245]), (0.5, [60, 140, 230])]
        for reach, c in layers:
            fe = (k * 7) % 1
            length = reach * (0.75 + 0.25 * np.sin(np.clip(k, 0, 1) * math.pi))
            m = (rad < length - 0.08 * np.abs(fe - 0.5)) & (rad > 0.05) & (k > 0.02) & (k < 0.98)
            col[m] = c
            a[m] = 200
            col[m & (fe < 0.1)] = [235, 250, 255]
    return img(col, a)


def crest(kind, s=32):
    yy, xx = grid(s)
    a = np.zeros((s, s), np.float32)
    col = np.zeros((s, s, 3), np.float32)
    cx = s / 2 - 0.5
    t = 1 - yy / (s - 1)                                   # 0 bottom .. 1 top
    if kind == "wind":             # crown blade with a cross guard and a star tip
        m = np.abs(xx - cx) < 3.2 * (1 - t) + 0.6
        m |= (np.abs(yy - 20) < 1) & (np.abs(xx - cx) < 7)
        star = (np.abs(xx - cx) + np.abs(yy - 4) * 2.2 < 4) | (np.abs(yy - 4) + np.abs(xx - cx) * 2.2 < 4)
        m |= star
        a[m] = 255; col[m] = [150, 255, 180]
        col[m & (np.abs(xx - cx) < 1)] = [235, 255, 240]
    elif kind == "fire":           # flame strand
        wob = 3.0 * np.sin(t * 7)
        m = np.abs(xx - cx - wob) < 5 * (1 - t) ** 0.8 + 0.5
        a[m] = 230
        col[m] = np.array([255, 230, 120]) * (1 - t[m][..., None]) + np.array([255, 90, 30]) * t[m][..., None]
    elif kind == "demon":          # horn segment: black with a red inner ridge
        m = np.abs(xx - cx) < 5.5 * (1 - t * 0.6)
        a[m] = 255; col[m] = [26, 22, 28]
        col[m & (np.abs(xx - cx) < 1.2)] = [120, 16, 24]
        col[m & ((yy.astype(int) % 6) == 0)] = [50, 44, 52]
    elif kind == "lightning":      # zig-zag bolt
        zig = np.where((yy // 6) % 2 == 0, (yy % 6) - 3, 3 - (yy % 6)) * 1.6
        m = np.abs(xx - cx - zig) < 2.2
        a[m] = 255; col[m] = [255, 250, 190]
        a[(np.abs(xx - cx - zig) < 4) & ~m] = 110
        col[(np.abs(xx - cx - zig) < 4) & ~m] = [255, 210, 60]
    elif kind == "valkyrie":       # small helmet wing
        rad = np.sqrt((xx) ** 2 + (s - 1 - yy) ** 2) / s
        k = np.arctan2(s - 1 - yy, xx) / (math.pi / 2)
        m = (rad < 0.95 - 0.2 * np.abs(((k * 4) % 1) - 0.5)) & (rad > 0.1)
        a[m] = 230; col[m] = [140, 215, 255]
        col[m & (((k * 4) % 1) < 0.12)] = [235, 250, 255]
    return img(col, a)


def fur(s=32):
    yy, xx = grid(s)
    r = rng(5)
    col = np.ones((s, s, 3), np.float32) * np.array([236, 246, 238], np.float32)
    tufts = r.random((s, s))
    col *= (0.82 + 0.18 * tufts)[..., None]
    col[(yy.astype(int) + (xx.astype(int) * 3) % 5) % 4 == 0] *= 0.86
    return img(col, np.full((s, s), 255))


def lance(s=32):
    yy, xx = grid(s)
    stripe = ((xx + yy * 0.9) % 10) < 4
    col = np.ones((s, s, 3), np.float32) * np.array([80, 170, 240], np.float32)
    col[stripe] = [190, 235, 255]
    col[(xx + yy * 0.9) % 10 < 1] = [250, 255, 255]
    return img(col, np.full((s, s), 225))


def wisp(s=8):
    yy, xx = grid(s)
    col = np.ones((s, s, 3), np.float32) * np.array([24, 16, 28], np.float32)
    col[(xx == 1) & (yy == 1)] = [90, 30, 60]
    return img(col, np.full((s, s), 255))


def main():
    os.makedirs(OUT, exist_ok=True)
    plates = {
        "wind": ([70, 150, 80], [230, 200, 90], [40, 95, 50], "coat"),
        "fire": ([170, 40, 30], [240, 180, 70], [90, 18, 14], "scale"),
        "demon": ([26, 22, 30], [90, 20, 30], [8, 6, 10], "obsidian"),
        "lightning": ([220, 220, 230], [255, 230, 120], [120, 120, 140], "runic"),
        "valkyrie": ([90, 175, 235], [210, 245, 255], [40, 110, 200], "crystal"),
    }
    glows = {"wind": ([170, 255, 190], "trim"), "fire": ([255, 170, 50], "veins"), "demon": ([200, 20, 40], "veins"),
             "lightning": ([255, 235, 120], "runes"), "valkyrie": ([200, 245, 255], "trim")}
    for i, (m, (base, hi, lo, style)) in enumerate(plates.items()):
        plate(base, hi, lo, 31 + i, style).save(os.path.join(OUT, f"{m}_plate.png"))
        glow(*glows[m]).save(os.path.join(OUT, f"{m}_glow.png"))
        crest(m).save(os.path.join(OUT, f"{m}_crest.png"))
        if m != "lightning":
            wing(m).save(os.path.join(OUT, f"{m}_wing.png"))
    fur().save(os.path.join(OUT, "wind_fur.png"))
    lance().save(os.path.join(OUT, "valkyrie_lance.png"))
    wisp().save(os.path.join(OUT, "demon_wisp.png"))
    print("wrote", OUT)


if __name__ == "__main__":
    main()
