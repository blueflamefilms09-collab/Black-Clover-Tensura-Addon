"""elite/items: the Common, Rare and Elite crate blocks: cube models (closed and open), palette textures (128x128), blockstates, item models, loot tables and recipes.

    python3 -B tools/elite/gen_crates.py [--sheet] [--check]

Per tier <t> in (common, rare, elite) this writes
  assets/nusmp/models/block/crate/<t>_crate.json and <t>_crate_open.json   closed: a banded chest of planks with a lock plate (rare / elite: a gem); open: the lid tilted back 45 degrees,
                                                                            a glowing light in the box (neoforge_data block/sky light 15)
  assets/nusmp/textures/block/crate/<t>_crate.png                          128x128
  assets/nusmp/blockstates/<t>_crate.json                                  facing x open
  assets/nusmp/models/item/<t>_crate.json                                  the closed model with the block display transforms
  data/nusmp/loot_table/blocks/<t>_crate.json                              the crate drops itself
  data/nusmp/recipe/<t>_crate.json                                         common: planks and iron; rare: a Common Crate, diamonds, iron; elite: a Rare Crate, gold blocks, netherite
  data/nusmp/recipe/health_potion_<size>.json                              the three Health Potions (they chain: two of one size and a golden food make the next)
(the keys are loot only, they have no recipe). Deterministic.
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from art_lib import ASSETS, BUILD, ROOT, ensure, write_text  # noqa: E402
import plush_lib  # noqa: E402
from plush_lib import Plush, hexc, shade  # noqa: E402

TIERS = {
    "common": dict(wood=hexc("A67C4A"), plank=hexc("7A5632"), band=hexc("9A9EAA"), band_dark=hexc("5E626E"), plate=hexc("C9A24A"), gem=None, glow=hexc("FFE9A0")),
    "rare": dict(wood=hexc("5A4A66"), plank=hexc("3A2E48"), band=hexc("D4DEEE"), band_dark=hexc("7A88A8"), plate=hexc("B8C6E0"), gem=hexc("4C9CF0"), glow=hexc("9AD0FF")),
    "elite": dict(wood=hexc("2A2430"), plank=hexc("16121C"), band=hexc("E8BC50"), band_dark=hexc("9A6E1E"), plate=hexc("F0CC60"), gem=hexc("B058F2"), glow=hexc("E0A8FF")),
}


def build(tier, opened):
    t = TIERS[tier]
    P = Plush("crate/%s_crate" % tier, t["wood"], ts=128)
    base = P.cube("base", (14, 8, 14), (1, 0, 1), t["wood"], noise=8)
    lid_pos = (1, 8, 1)
    rot = ("x", 45.0, (8, 8, 15)) if opened else None
    lid = P.cube("lid", (14, 4, 14), lid_pos, t["wood"], noise=8, rot=rot)
    for c in (base, lid):
        for f in ("north", "south", "east", "west"):
            rx, ry, rw, rh = c.rects[f]
            for j in range(rh):
                if j % 3 == 2:
                    for i in range(rw):
                        old = P.tex.px[rx + i, ry + j]
                        P.put(c, f, i, j, shade(old[:3], 0.8))
        for f in ("up",):
            rx, ry, rw, rh = c.rects[f]
            for j in range(rh):
                for i in range(rw):
                    if i % 4 == 3:
                        P.put(c, f, i, j, shade(P.tex.px[rx + i, ry + j][:3], 0.82))
    # corner posts and the band round the lid's lower edge
    posts = []
    for k, (x, z) in enumerate(((0.5, 0.5), (13.5, 0.5), (0.5, 13.5), (13.5, 13.5))):
        posts.append(P.cube("post%d" % k, (2, 8, 2), (x, 0, z), t["band"], noise=5))
    ring = P.cube("ring", (15, 1, 15), (0.5, 7.5, 0.5), t["band_dark"], noise=4)
    if opened:
        ring.rot = None
    # straps over the lid (two vertical bands on the lid's front and back faces + top)
    lidposts = []
    for k, x in enumerate((3.5, 10.5)):
        lidposts.append(P.cube("strap%d" % k, (2, 4, 15), (x, 8.0, 0.5), t["band"], noise=5, rot=rot))
    for c in posts + lidposts:
        for f in ("north", "south", "east", "west", "up"):
            rx, ry, rw, rh = c.rects[f]
            for j in range(rh):
                for i in range(rw):
                    if (i + j) % 5 == 0 and rw > 1 and rh > 1:
                        P.put(c, f, i, j, shade(P.tex.px[rx + i, ry + j][:3], 1.18))
    # the lock plate on the front, over the lid / base seam
    plate = P.cube("plate", (4, 4, 1), (6, 5.5, 0.0), t["plate"], noise=4)
    P.rect(plate, "north", 1, 1, 2, 2, shade(t["plate"], 0.6))
    P.put(plate, "north", 1, 1, (20, 16, 16))
    P.put(plate, "north", 1, 2, (20, 16, 16))
    if t["gem"]:
        g = P.cube("gem", (2, 2, 1), (7, 8.2, -0.5), t["gem"], noise=0, rot=rot)
        P.put(g, "north", 0, 0, (255, 255, 255))
    if opened:
        glow = P.cube("light", (12, 1, 12), (2, 7.2, 2), t["glow"], noise=0)
        P.cube("shine", (6, 6, 6), (5, 8.0, 5), shade(t["glow"], 1.0), noise=0)
        for c in (glow, P.named["shine"]):
            c.glow = True
    return P


def model(P, opened, tier):
    m = P.model_json(display=False)
    m["textures"] = {"0": "nusmp:block/crate/%s_crate" % tier, "particle": "nusmp:block/crate/%s_crate" % tier}
    m["render_type"] = "minecraft:cutout"
    m["display"] = {
        "gui": {"rotation": [30, 225, 0], "translation": [0, -1, 0], "scale": [0.6, 0.6, 0.6]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.4, 0.4, 0.4]},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.55, 0.55, 0.55]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.33, 0.33, 0.33]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 3, 0], "scale": [0.4, 0.4, 0.4]},
    }
    for e, c in zip(m["elements"], P.cubes):
        if getattr(c, "glow", False):
            for f in e["faces"].values():
                f["neoforge_data"] = {"block_light": 15, "sky_light": 15}
            e["shade"] = False
    return m


def jdump(o):
    return json.dumps(o, indent=2) + "\n"


def files_for(tier):
    out = {}
    closed, opened = build(tier, False), build(tier, True)
    out["assets/nusmp/models/block/crate/%s_crate.json" % tier] = jdump(model(closed, False, tier))
    out["assets/nusmp/models/block/crate/%s_crate_open.json" % tier] = jdump(model(opened, True, tier))
    out["assets/nusmp/models/item/%s_crate.json" % tier] = jdump({"parent": "nusmp:block/crate/%s_crate" % tier})
    var = {}
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for op in ("false", "true"):
            v = {"model": "nusmp:block/crate/%s_crate%s" % (tier, "_open" if op == "true" else "")}
            if y:
                v["y"] = y
            var["facing=%s,open=%s" % (facing, op)] = v
    out["assets/nusmp/blockstates/%s_crate.json" % tier] = jdump({"variants": var})
    out["data/nusmp/loot_table/blocks/%s_crate.json" % tier] = jdump({
        "type": "minecraft:block",
        "pools": [{"rolls": 1.0, "bonus_rolls": 0.0, "entries": [{"type": "minecraft:item", "name": "nusmp:%s_crate" % tier}], "conditions": [{"condition": "minecraft:survives_explosion"}]}],
        "random_sequence": "nusmp:blocks/%s_crate" % tier})
    return out, closed


RECIPES = {
    "common": {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["PPP", "PIP", "PPP"],
               "key": {"P": {"tag": "minecraft:planks"}, "I": {"item": "minecraft:iron_ingot"}}, "result": {"id": "nusmp:common_crate", "count": 1}},
    "rare": {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["DID", "ICI", "DID"],
             "key": {"D": {"item": "minecraft:diamond"}, "I": {"item": "minecraft:iron_ingot"}, "C": {"item": "nusmp:common_crate"}}, "result": {"id": "nusmp:rare_crate", "count": 1}},
    "elite": {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["NGN", "GCG", "NGN"],
              "key": {"N": {"item": "minecraft:netherite_ingot"}, "G": {"item": "minecraft:gold_block"}, "C": {"item": "nusmp:rare_crate"}}, "result": {"id": "nusmp:elite_crate", "count": 1}},
}


def shapeless(result, *ingredients):
    return {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [{"item": i} for i in ingredients], "result": {"id": "nusmp:" + result, "count": 1}}


# the Health Potions (the three sizes chain: two of one size and a golden food make the next; the sums keep the value: 8 + 8 = 16, 16 + 16 = 32 >= 28 and a golden apple's absorption)
POTION_RECIPES = {
    "health_potion_small": shapeless("health_potion_small", "minecraft:glass_bottle", "minecraft:glistering_melon_slice", "minecraft:redstone"),
    "health_potion_medium": shapeless("health_potion_medium", "nusmp:health_potion_small", "nusmp:health_potion_small", "minecraft:golden_carrot"),
    "health_potion_large": shapeless("health_potion_large", "nusmp:health_potion_medium", "nusmp:health_potion_medium", "minecraft:golden_apple"),
}


def main(argv):
    check = "--check" in argv
    dirty = 0
    sheets = []
    for pid, rec in POTION_RECIPES.items():
        path = os.path.join(ROOT, "src", "main", "resources", "data", "nusmp", "recipe", pid + ".json")
        if check:
            if not os.path.exists(path) or open(path, encoding="utf-8").read() != jdump(rec):
                dirty += 1
        else:
            write_text(path, jdump(rec))
    for tier in TIERS:
        files, closed = files_for(tier)
        files["data/nusmp/recipe/%s_crate.json" % tier] = jdump(RECIPES[tier])
        for rel, data in files.items():
            path = os.path.join(ROOT, "src", "main", "resources", *rel.split("/"))
            if check:
                if not os.path.exists(path) or open(path, encoding="utf-8").read() != data:
                    dirty += 1
            else:
                write_text(path, data)
        tpath = os.path.join(ASSETS, "textures", "block", "crate", tier + "_crate.png")
        # the open model has more cubes (its glow cubes): its texture is the one the blockstate's two models share, so the OPEN build's texture is written
        opened = build(tier, True)
        if not check:
            ensure(os.path.dirname(tpath))
            opened.tex.im.save(tpath, optimize=True)
            print("%s crate: texture %d px used" % (tier, opened.tex.used()))
        sheets += [plush_lib.render(closed, yaw=-30, size=200), plush_lib.render(opened, yaw=-30, size=200)]
    if check:
        print("dirty files:", dirty)
        sys.exit(1 if dirty else 0)
    if "--sheet" in argv:
        from PIL import Image
        out = Image.new("RGBA", (200 * 6, 200), (70, 76, 96, 255))
        for i, im in enumerate(sheets):
            out.alpha_composite(im, (i * 200, 0))
        ensure(BUILD)
        out.save(os.path.join(BUILD, "crates.png"))
        print("sheet:", os.path.join(BUILD, "crates.png"))


if __name__ == "__main__":
    main(sys.argv[1:])
