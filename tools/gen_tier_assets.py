#!/usr/bin/env python3
"""
Icons and item models of the two tier-ascension catalysts (docs/relic_tiers_and_scaling.md): the Devil's Core (to God grade) and the Genesis Seed
(to Genesis grade). 16x16, idempotent.

    python3 tools/gen_tier_assets.py
"""
import json
import math
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, "..", "src", "main", "resources", "assets", "nusmp")


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def shade(base, k):
    return tuple(max(0, min(255, int(c * k))) for c in base) + (255,)


def orb(center, radius, base, light=(-0.5, -0.6)):
    """A shaded sphere with a facet cut: returns a 16x16 pixel dict."""
    px = {}
    cx, cy = center
    for y in range(16):
        for x in range(16):
            dx, dy = (x + 0.5 - cx) / radius, (y + 0.5 - cy) / radius
            d = dx * dx + dy * dy
            if d > 1.0:
                continue
            nz = math.sqrt(1.0 - d)
            lam = max(0.0, -(dx * light[0] + dy * light[1]) * 0.7 + nz * 0.7)
            facet = 0.88 if ((x + y) % 5 == 0) else 1.0
            px[(x, y)] = shade(base, (0.35 + 0.85 * lam) * facet)
    return px


def devils_core():
    px = orb((8, 9), 5.2, (190, 24, 30))
    for x, y in [(7, 6), (8, 6), (7, 7)]:
        px[(x, y)] = (255, 170, 150, 255)                       # the glint
    for x, y in [(8, 10), (8, 11), (9, 10)]:
        px[(x, y)] = (255, 90, 40, 255)                         # the ember inside
    horn = (40, 8, 14, 255)
    for x, y in [(4, 5), (3, 4), (3, 3), (2, 2), (4, 4), (11, 5), (12, 4), (12, 3), (13, 2), (11, 4), (5, 5), (10, 5), (5, 4), (10, 4)]:
        px[(x, y)] = horn                                       # two small horns
    for (x, y), c in list(px.items()):
        if (x, y) not in [(7, 6), (8, 6), (7, 7)] and (abs(x - 8) + abs(y - 9) >= 7):
            px[(x, y)] = shade((60, 6, 10), 1.0)
    return px


def genesis_seed():
    px = {}
    # a teardrop seed, white-gold, with a prismatic rim
    for y in range(16):
        for x in range(16):
            dx, dy = (x + 0.5 - 8) / 4.6, (y + 0.5 - 8.5) / 6.4
            tip = max(0.0, (8.5 - (y + 0.5)) / 6.4)
            d = dx * dx / (1.0 - 0.55 * tip * tip) + dy * dy
            if d > 1.0:
                continue
            lam = max(0.0, 0.55 - dx * 0.5 - dy * 0.35 + math.sqrt(max(0.0, 1.0 - d)) * 0.6)
            px[(x, y)] = shade((255, 240, 205), 0.55 + 0.6 * lam)
    rim = [(255, 120, 160), (255, 200, 90), (120, 255, 190), (110, 190, 255), (200, 140, 255)]
    n = 0
    for (x, y) in sorted(px):
        edge = any(((x + dx, y + dy) not in px) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if edge:
            px[(x, y)] = rim[n % len(rim)] + (255,)
            n += 1
    for x, y in [(7, 6), (7, 7), (8, 6)]:
        if (x, y) in px:
            px[(x, y)] = (255, 255, 255, 255)
    return px


def save(name, px):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for (x, y), c in px.items():
        img.putpixel((x, y), c)
    path = os.path.join(ASSETS, "textures", "item", name + ".png")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)
    write_json(os.path.join(ASSETS, "models", "item", name + ".json"),
               {"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/" + name}})


if __name__ == "__main__":
    save("devils_core", devils_core())
    save("genesis_seed", genesis_seed())
    print("wrote devils_core and genesis_seed")
