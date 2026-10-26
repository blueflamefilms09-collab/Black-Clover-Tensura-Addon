#!/usr/bin/env python3
"""Draws the 32x32 icon of every stand-in skill: src/main/resources/assets/nusmp/textures/skill/line/<line id>.png

Tensura draws a skill's icon from a texture the skill names (LineStandInSkill.getSkillIcon -> nusmp:textures/skill/line/<id>.png, the same 32x32 size
as the other skill icons of the mod). A Unique Skill Line has an item icon in its route file, which is not a standalone texture, so each line gets a small
emblem instead: a dark tile, a double ring in the line's colour, the glyph of its franchise (one simple shape per franchise) and a ring of rune dots whose
count and place come from a hash of the line id, so two lines of one franchise differ at a glance. Deterministic (no randomness, no clock): running it twice
gives byte-identical files. Colours and franchises are read from assets/nusmp/skill_books.json, the same table the registry uses.

Run from the repository root:  python3 tools/tensura_menu/gen_line_icons.py
"""
import hashlib
import json
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
BOOKS = ROOT / "src/main/resources/assets/nusmp/skill_books.json"
OUT = ROOT / "src/main/resources/assets/nusmp/textures/skill/line"
SS = 8            # supersampling
N = 32


def rgb(hexcolor: str):
    h = hexcolor.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) if len(h) == 6 else (200, 200, 200)


def mix(a, b, t):
    return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3))


def poly(d, pts, fill):
    d.polygon([(x * SS, y * SS) for x, y in pts], fill=fill)


def star(cx, cy, r1, r2, n, rot=-math.pi / 2):
    pts = []
    for i in range(n * 2):
        r = r1 if i % 2 == 0 else r2
        a = rot + i * math.pi / n
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return pts


def ngon(cx, cy, r, n, rot=-math.pi / 2):
    return [(cx + r * math.cos(rot + i * 2 * math.pi / n), cy + r * math.sin(rot + i * 2 * math.pi / n)) for i in range(n)]


def glyph(d, kind, c, dark):
    cx, cy = 16, 16
    if kind == "tensura":                                   # a slime drop
        poly(d, [(16, 7), (22, 16), (22, 20), (19, 24), (13, 24), (10, 20), (10, 16)], c)
        d.ellipse([13 * SS, 17 * SS, 15 * SS, 19 * SS], fill=dark)
        d.ellipse([17 * SS, 17 * SS, 19 * SS, 19 * SS], fill=dark)
    elif kind == "black_clover":                            # a four-leaf clover
        for dx, dy in ((-3.2, -3.2), (3.2, -3.2), (-3.2, 3.2), (3.2, 3.2)):
            d.ellipse([(cx + dx - 3.4) * SS, (cy + dy - 3.4) * SS, (cx + dx + 3.4) * SS, (cy + dy + 3.4) * SS], fill=c)
        d.line([(cx * SS, cy * SS), (cx * SS + 4 * SS, (cy + 9) * SS)], fill=c, width=SS)
    elif kind == "magi":                                    # a djinn lamp / crescent
        d.ellipse([9 * SS, 9 * SS, 23 * SS, 23 * SS], fill=c)
        d.ellipse([12 * SS, 8 * SS, 26 * SS, 22 * SS], fill=dark)
    elif kind == "solo_leveling":                           # a shadow blade
        poly(d, [(16, 5), (19, 20), (16, 26), (13, 20)], c)
        poly(d, [(11, 20), (21, 20), (21, 22), (11, 22)], c)
    elif kind == "danmachi":                                # a falna star
        poly(d, star(cx, cy, 9, 3.6, 5), c)
    elif kind == "tbate":                                   # a crown
        poly(d, [(8, 22), (8, 11), (12, 16), (16, 9), (20, 16), (24, 11), (24, 22)], c)
    elif kind == "tower_of_god":                            # a tower
        poly(d, [(12, 24), (13, 12), (11, 12), (16, 5), (21, 12), (19, 12), (20, 24)], c)
    elif kind == "god_of_high_school":                      # a fist/diamond burst
        poly(d, star(cx, cy, 9.5, 5.5, 4), c)
    elif kind == "omniscient_readers_viewpoint":            # an eye
        poly(d, [(6, 16), (16, 9), (26, 16), (16, 23)], c)
        d.ellipse([13 * SS, 13 * SS, 19 * SS, 19 * SS], fill=dark)
        d.ellipse([15 * SS, 15 * SS, 17 * SS, 17 * SS], fill=c)
    elif kind == "exiled_heavy_knight":                     # a heavy helm / shield
        poly(d, [(8, 8), (24, 8), (24, 18), (16, 26), (8, 18)], c)
        d.line([(16 * SS, 10 * SS), (16 * SS, 23 * SS)], fill=dark, width=SS)
    elif kind == "fairy_tail":                              # 3.26.0: a swallow-tailed guild flag with a star
        poly(d, [(9, 8), (23, 8), (23, 24), (19, 20), (16, 24), (13, 20), (9, 24)], c)
        poly(d, star(cx, 14, 4.2, 1.8, 5), dark)
    else:                                                   # classic routes: a book
        poly(d, [(8, 9), (16, 11), (24, 9), (24, 23), (16, 25), (8, 23)], c)
        d.line([(16 * SS, 11 * SS), (16 * SS, 25 * SS)], fill=dark, width=SS)


def icon(bare: str, color: str, franchise: str) -> Image.Image:
    base = rgb(color)
    light = mix(base, (255, 255, 255), 0.45)
    dark = mix(base, (0, 0, 0), 0.82)
    im = Image.new("RGBA", (N * SS, N * SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rounded_rectangle([1 * SS, 1 * SS, 31 * SS - 1, 31 * SS - 1], radius=5 * SS, fill=mix(base, (6, 8, 16), 0.88) + (255,))
    d.rounded_rectangle([1 * SS, 1 * SS, 31 * SS - 1, 31 * SS - 1], radius=5 * SS, outline=base + (255,), width=SS)
    d.rounded_rectangle([3 * SS, 3 * SS, 29 * SS - 1, 29 * SS - 1], radius=3 * SS, outline=mix(base, (0, 0, 0), 0.5) + (255,), width=SS // 2)
    glyph(d, franchise, light + (255,), dark + (255,))
    h = int(hashlib.sha256(bare.encode()).hexdigest(), 16)
    dots = 3 + h % 5                                        # 3..7 rune dots on the inner ring
    start = (h >> 8) % 360
    for i in range(dots):
        a = math.radians(start + i * 360 / dots)
        x, y = 16 + 11.5 * math.cos(a), 16 + 11.5 * math.sin(a)
        d.ellipse([(x - 0.9) * SS, (y - 0.9) * SS, (x + 0.9) * SS, (y + 0.9) * SS], fill=base + (255,))
    return im.resize((N, N), Image.LANCZOS)


def main() -> int:
    lines = json.loads(BOOKS.read_text(encoding="utf-8"))["lines"]
    OUT.mkdir(parents=True, exist_ok=True)
    for bare, info in sorted(lines.items()):
        icon(bare, info.get("color", "#FFFFFF"), info.get("franchise", "")).save(OUT / f"{bare}.png", optimize=True)
    print(f"wrote {len(lines)} icons to {OUT.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
