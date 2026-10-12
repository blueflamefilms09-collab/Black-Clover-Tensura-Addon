#!/usr/bin/env python3
"""
Assets of the Living Dungeon (docs/living_dungeon.md): block textures, blockstates, block and item models, the three effect icons and the
axe-mineable tag. Idempotent: running it again rewrites the same files.

    python3 tools/gen_dungeon_assets.py
"""
import json
import math
import os
import random

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", "nusmp")
DATA = os.path.join(RES, "data")


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def noise(n, seed, blur=1):
    rng = np.random.default_rng(seed)
    a = rng.random((n, n))
    for _ in range(blur):
        a = (a + np.roll(a, 1, 0) + np.roll(a, -1, 0) + np.roll(a, 1, 1) + np.roll(a, -1, 1)) / 5.0
    return (a - a.min()) / (a.max() - a.min() + 1e-9)


def swirl(core, mid, edge, seed, arms=3):
    """A radial swirl: bright core, coloured arms, dark rim (the Maw and the Root Gate)."""
    n = 16
    img = np.zeros((n, n, 4), np.uint8)
    nz = noise(n, seed)
    for y in range(n):
        for x in range(n):
            dx, dy = x - 7.5, y - 7.5
            r = math.hypot(dx, dy) / 8.0
            a = math.atan2(dy, dx) + r * 3.2
            arm = 0.5 + 0.5 * math.sin(a * arms)
            t = min(1.0, r)
            col = np.array(core, float) * (1 - t) ** 1.6 + np.array(mid, float) * (arm * 0.7 + 0.3) * (1 - abs(t - 0.5) * 1.2) + np.array(edge, float) * t ** 2
            col = col * (0.85 + 0.3 * nz[y, x])
            img[y, x, :3] = np.clip(col, 0, 255)
            img[y, x, 3] = 255
    return Image.fromarray(img, "RGBA")


def root(seed):
    n = 16
    rng = random.Random(seed)
    img = Image.new("RGBA", (n, n))
    px = img.load()
    nz = noise(n, seed, 2)
    for x in range(n):
        for y in range(n):
            groove = 0.5 + 0.5 * math.sin((x + 2 * nz[y, x]) * 1.9)
            base = np.array((78, 56, 38), float) * (0.55 + 0.6 * groove) * (0.85 + 0.3 * nz[y, x])
            px[x, y] = tuple(int(v) for v in np.clip(base, 0, 255)) + (255,)
    for _ in range(3):                                      # glowing veins of mana
        x = rng.randrange(2, 14)
        for y in range(n):
            x = max(1, min(14, x + rng.choice((-1, 0, 0, 1))))
            px[x, y] = (95, 227, 192, 255)
            if rng.random() < 0.3:
                px[x + 1, y] = (60, 150, 130, 255)
    return img


def veil(seed):
    n = 16
    rng = random.Random(seed)
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for _ in range(9):                                      # pale threads
        x0, y0 = rng.randrange(n), rng.randrange(n)
        x1, y1 = x0 + rng.randrange(-7, 8), y0 + rng.randrange(-7, 8)
        d.line((x0, y0, x1, y1), fill=(214, 236, 226, 255))
    px = img.load()
    for _ in range(7):                                      # glowing dots
        x, y = rng.randrange(n), rng.randrange(n)
        px[x, y] = (110, 255, 214, 255)
    return img


def icon(draw_fn):
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    draw_fn(ImageDraw.Draw(img))
    return img


def icon_suffocation(d):
    d.ellipse((3, 3, 14, 14), outline=(150, 175, 195, 255), width=2)          # a bubble of air...
    d.line((5, 5, 12, 12), fill=(210, 60, 60, 255), width=2)                    # ...crossed out
    d.line((12, 5, 5, 12), fill=(210, 60, 60, 255), width=2)


def icon_poisoning(d):
    d.ellipse((4, 6, 13, 15), fill=(226, 180, 60, 255), outline=(130, 90, 20, 255))
    d.polygon([(8, 1), (5, 8), (12, 8)], fill=(226, 180, 60, 255))
    d.ellipse((6, 8, 8, 10), fill=(255, 245, 190, 255))


def icon_sickness(d):
    d.ellipse((2, 2, 15, 15), fill=(140, 90, 168, 255), outline=(70, 40, 90, 255))
    d.line((5, 6, 7, 8), fill=(30, 20, 40, 255), width=1)
    d.line((7, 6, 5, 8), fill=(30, 20, 40, 255), width=1)
    d.line((10, 6, 12, 8), fill=(30, 20, 40, 255), width=1)
    d.line((12, 6, 10, 8), fill=(30, 20, 40, 255), width=1)
    d.arc((5, 10, 12, 15), 200, 340, fill=(150, 220, 120, 255), width=1)


def main():
    tex = os.path.join(ASSETS, "textures", "block")
    os.makedirs(tex, exist_ok=True)
    swirl((235, 255, 250), (140, 70, 220), (20, 12, 40), 1).save(os.path.join(tex, "dungeon_maw.png"))
    swirl((255, 245, 200), (90, 200, 120), (14, 30, 20), 2, arms=2).save(os.path.join(tex, "dungeon_gate.png"))
    root(3).save(os.path.join(tex, "maw_root.png"))
    veil(4).save(os.path.join(tex, "dungeon_mycelium.png"))
    eff = os.path.join(ASSETS, "textures", "mob_effect")
    os.makedirs(eff, exist_ok=True)
    icon(icon_suffocation).save(os.path.join(eff, "suffocation.png"))
    icon(icon_poisoning).save(os.path.join(eff, "magicule_poisoning.png"))
    icon(icon_sickness).save(os.path.join(eff, "magicule_sickness.png"))

    for name in ("dungeon_maw", "dungeon_gate", "maw_root"):
        write_json(os.path.join(ASSETS, "blockstates", name + ".json"), {"variants": {"": {"model": "nusmp:block/" + name}}})
        write_json(os.path.join(ASSETS, "models", "block", name + ".json"), {"parent": "minecraft:block/cube_all", "textures": {"all": "nusmp:block/" + name}})
        write_json(os.path.join(ASSETS, "models", "item", name + ".json"), {"parent": "nusmp:block/" + name})
    write_json(os.path.join(ASSETS, "blockstates", "dungeon_mycelium.json"), {"variants": {"": {"model": "nusmp:block/dungeon_mycelium"}}})
    write_json(os.path.join(ASSETS, "models", "block", "dungeon_mycelium.json"),
               {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout", "textures": {"cross": "nusmp:block/dungeon_mycelium"}})
    write_json(os.path.join(ASSETS, "models", "item", "dungeon_mycelium.json"),
               {"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:block/dungeon_mycelium"}})

    write_json(os.path.join(DATA, "minecraft", "tags", "block", "mineable", "axe.json"), {"replace": False, "values": ["nusmp:maw_root"]})

    # the lang entries are appended to en_us.json once
    lang_path = os.path.join(ASSETS, "lang", "en_us.json")
    with open(lang_path) as f:
        lang = json.load(f)
    lang.update({
        "block.nusmp.dungeon_maw": "Dungeon Maw", "block.nusmp.dungeon_gate": "Root Gate", "block.nusmp.maw_root": "Maw Root",
        "block.nusmp.dungeon_mycelium": "Dungeon Mycelium",
        "effect.nusmp.suffocation": "Suffocation", "effect.nusmp.magicule_poisoning": "Magicule Poisoning", "effect.nusmp.magicule_sickness": "Magicule Sickness",
    })
    with open(lang_path, "w") as f:
        json.dump(lang, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print("done")


if __name__ == "__main__":
    main()
