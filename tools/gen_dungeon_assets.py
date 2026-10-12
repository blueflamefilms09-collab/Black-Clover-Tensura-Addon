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


def gel_texture():
    """64x32 slime layout of the game's own slime model: outer cube (0,0) 32x16, inner cube (0,16) 24x12, eyes (32,0) and (32,4), mouth (32,8)."""
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    nz = noise(64, 11, 1)
    px = img.load()
    for y in range(16):                                      # the translucent outer shell, pale teal with bright edges
        for x in range(32):
            edge = min(x % 8, 7 - x % 8, y % 8, 7 - y % 8)
            a = 150 + (40 if edge == 0 else 0)
            px[x, y] = (int(120 + 50 * nz[y, x]), int(235 - 20 * nz[y, x]), int(215 + 25 * nz[y, x]), a)
    for y in range(16, 28):                                  # the dark core of magicules
        for x in range(24):
            px[x, y] = (int(36 + 40 * nz[y, x]), int(120 + 50 * nz[y, x]), int(150 + 40 * nz[y, x]), 255)
    for (u, v) in ((32, 0), (32, 4)):                        # eyes
        for y in range(4):
            for x in range(8):
                px[u + x, v + y] = (20, 40, 60, 255) if (x in (2, 3, 4, 5) and y in (1, 2)) else (190, 255, 245, 255)
    for y in range(2):
        for x in range(4):
            px[32 + x, 8 + y] = (20, 40, 60, 255)
    return img


def armor_texture():
    """64x64 humanoid layout: the whole sheet is the dark, slick flesh of the mollusks that live in the armour; two cold eyes on the face."""
    n = 64
    nz = noise(n, 21, 2)
    fine = noise(n, 22, 0)
    img = Image.new("RGBA", (n, n))
    px = img.load()
    for y in range(n):
        for x in range(n):
            base = np.array((46, 34, 64), float) * (0.7 + 0.6 * nz[y, x]) + np.array((20, 40, 50), float) * (fine[y, x] > 0.86)
            px[x, y] = tuple(int(v) for v in np.clip(base, 0, 255)) + (255,)
    for (x, y) in ((10, 12), (13, 12), (10, 13), (13, 13)):   # the face front is (8,8)-(16,16)
        px[x, y] = (150, 240, 255, 255)
    return img


def carcass_texture(kind):
    n = 16
    nz = noise(n, 30 + kind, 1)
    rng = random.Random(kind)
    cols = {0: ((90, 200, 170), (40, 120, 110)), 1: ((92, 86, 110), (50, 44, 66)), 2: ((60, 96, 56), (20, 34, 24)),
            3: ((120, 118, 126), (66, 64, 72)), 4: ((150, 70, 70), (86, 36, 40))}[kind]
    img = Image.new("RGBA", (n, n))
    px = img.load()
    for y in range(n):
        for x in range(n):
            t = nz[y, x]
            c = np.array(cols[0], float) * t + np.array(cols[1], float) * (1 - t)
            if kind == 1 and (x + y) % 7 == 0: c = np.array((170, 170, 190), float)     # flecks of armour
            if kind == 2 and (x // 2 + y // 2) % 2 == 0: c = c * 0.8                        # scales
            if kind == 3 and rng.random() < 0.25: c = c * 1.15                              # fur
            px[x, y] = tuple(int(v) for v in np.clip(c, 0, 255)) + (255,)
    return img


FOODS = {   # id: (base colour, shade colour, kind) ; kind raw = marbled steak, cooked = seared steak with grill marks, ruined = grey mush, purified = pale
    "raw_slime_jelly": ((120, 220, 190), (60, 150, 140), "jelly"), "raw_armor_flank": ((150, 70, 90), (90, 40, 66), "raw"),
    "raw_basilisk_fillet": ((120, 150, 90), (60, 90, 50), "raw"), "raw_direwolf_haunch": ((170, 60, 60), (110, 36, 40), "bone"),
    "raw_monster_meat": ((160, 80, 80), (100, 46, 50), "raw"), "ruined_meat": ((110, 104, 100), (70, 66, 64), "mush"),
    "purified_monster_meat": ((226, 170, 150), (180, 120, 104), "raw"),
    "candied_slime_jelly": ((90, 230, 200), (40, 150, 150), "cooked_jelly"), "grilled_armor_flank": ((176, 110, 80), (110, 66, 46), "cooked"),
    "roast_basilisk_fillet": ((170, 140, 70), (110, 90, 40), "cooked"), "roast_direwolf_haunch": ((170, 100, 60), (104, 60, 36), "cookedbone"),
    "seared_monster_meat": ((176, 120, 90), (110, 72, 54), "cooked"),
}


def food_texture(name):
    base, shade, kind = FOODS[name]
    n = 16
    nz = noise(n, sum(map(ord, name)), 1)
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    px = img.load()
    for y in range(n):
        for x in range(n):
            dx, dy = (x - 7.5) / 6.4, (y - 8.0) / 4.8
            inside = dx * dx + dy * dy <= 1.0 + 0.25 * (nz[y, x] - 0.5)
            if kind in ("jelly", "cooked_jelly"):
                inside = (x - 7.5) ** 2 + (y - 9) ** 2 <= 22 + 8 * nz[y, x]
            if not inside:
                continue
            t = nz[y, x]
            c = np.array(base, float) * (0.75 + 0.5 * t) + np.array(shade, float) * (1 - t) * 0.4
            if kind == "raw" and (x + y * 2) % 7 == 0: c = c * 0.7 + np.array((240, 220, 210), float) * 0.3      # marbling
            if kind in ("cooked", "cookedbone") and (y % 4 == 1): c = c * 0.6                                      # grill marks
            if kind in ("jelly", "cooked_jelly") and (x + y) % 5 == 0: c = c * 0.8 + np.array((255, 255, 255), float) * 0.2
            if kind == "mush": c = c * 0.8
            px[x, y] = tuple(int(v) for v in np.clip(c, 0, 255)) + (255,)
    if kind in ("bone", "cookedbone"):                                                                            # a bone sticking out
        for x in range(11, 15): px[x, 4 + (x - 11)] = (236, 228, 208, 255)
        px[14, 7] = (236, 228, 208, 255); px[15, 7] = (236, 228, 208, 255)
    # a dark outline
    out = img.copy(); op = out.load()
    for y in range(n):
        for x in range(n):
            if px[x, y][3] == 0:
                for ddx, ddy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    xx, yy = x + ddx, y + ddy
                    if 0 <= xx < n and 0 <= yy < n and px[xx, yy][3] > 0:
                        op[x, y] = (30, 20, 20, 255)
    return out


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
    ent = os.path.join(ASSETS, "textures", "entity")
    os.makedirs(ent, exist_ok=True)
    gel_texture().save(os.path.join(ent, "dungeon_slime.png"))
    armor_texture().save(os.path.join(ent, "living_armor.png"))
    faces = {f: {"texture": "#t"} for f in ("down", "up", "north", "south", "west", "east")}
    variants = {}
    for k in range(5):
        carcass_texture(k).save(os.path.join(tex, "carcass_%d.png" % k))
        write_json(os.path.join(ASSETS, "models", "block", "carcass_%d.json" % k),
                   {"parent": "minecraft:block/block", "textures": {"particle": "nusmp:block/carcass_%d" % k, "t": "nusmp:block/carcass_%d" % k},
                    "elements": [{"from": [1, 0, 1], "to": [15, 6, 15], "faces": faces}]})
        variants["kind=%d" % k] = {"model": "nusmp:block/carcass_%d" % k}
    write_json(os.path.join(ASSETS, "blockstates", "carcass.json"), {"variants": variants})
    itex = os.path.join(ASSETS, "textures", "item")
    os.makedirs(itex, exist_ok=True)
    for fid in FOODS:
        food_texture(fid).save(os.path.join(itex, fid + ".png"))
        write_json(os.path.join(ASSETS, "models", "item", fid + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/" + fid}})
    # cooking and purifying (data/nusmp/recipe, tags)
    cooked = {"raw_slime_jelly": "candied_slime_jelly", "raw_armor_flank": "grilled_armor_flank", "raw_basilisk_fillet": "roast_basilisk_fillet",
              "raw_direwolf_haunch": "roast_direwolf_haunch", "raw_monster_meat": "seared_monster_meat"}
    for raw_id, meal_id in cooked.items():
        for kind, ticks in (("smelting", 200), ("smoking", 100), ("campfire_cooking", 600)):
            write_json(os.path.join(DATA, "nusmp", "recipe", "%s_%s.json" % (meal_id, kind)),
                       {"type": "minecraft:" + kind, "category": "food", "ingredient": {"item": "nusmp:" + raw_id}, "result": {"id": "nusmp:" + meal_id},
                        "experience": 0.35, "cookingtime": ticks})
        write_json(os.path.join(DATA, "nusmp", "recipe", "purify_%s.json" % raw_id),
                   {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [{"item": "nusmp:" + raw_id}, {"tag": "nusmp:purifiers"}],
                    "result": {"id": "nusmp:purified_monster_meat", "count": 1}})
    write_json(os.path.join(DATA, "nusmp", "tags", "item", "purifiers.json"),
               {"replace": False, "values": [{"id": "tensura:holy_water", "required": False}, "minecraft:honey_bottle"]})
    write_json(os.path.join(DATA, "nusmp", "tags", "item", "butchering_blades.json"),
               {"replace": False, "values": ["#minecraft:swords", "#minecraft:axes", {"id": "nusmp:butcher_cleaver", "required": False}]})
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
        "block.nusmp.dungeon_mycelium": "Dungeon Mycelium", "block.nusmp.carcass": "Carcass",
        "entity.nusmp.dungeon_slime": "Dungeon Slime", "entity.nusmp.living_armor": "Living Armor", "entity.nusmp.dungeon_direwolf": "Direwolf",
        "entity.nusmp.basilisk": "Basilisk", "item.nusmp.butcher_cleaver": "Butcher Cleaver",
        "item.nusmp.raw_slime_jelly": "Raw Slime Jelly", "item.nusmp.raw_armor_flank": "Raw Armor Flank", "item.nusmp.raw_basilisk_fillet": "Raw Basilisk Fillet",
        "item.nusmp.raw_direwolf_haunch": "Raw Direwolf Haunch", "item.nusmp.raw_monster_meat": "Raw Monster Meat", "item.nusmp.ruined_meat": "Ruined Meat",
        "item.nusmp.purified_monster_meat": "Purified Monster Meat", "item.nusmp.candied_slime_jelly": "Candied Slime Jelly",
        "item.nusmp.grilled_armor_flank": "Grilled Armor Flank", "item.nusmp.roast_basilisk_fillet": "Roast Basilisk Fillet",
        "item.nusmp.roast_direwolf_haunch": "Roast Direwolf Haunch", "item.nusmp.seared_monster_meat": "Seared Monster Meat",
        "effect.nusmp.suffocation": "Suffocation", "effect.nusmp.magicule_poisoning": "Magicule Poisoning", "effect.nusmp.magicule_sickness": "Magicule Sickness",
    })
    with open(lang_path, "w") as f:
        json.dump(lang, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print("done")


if __name__ == "__main__":
    main()
