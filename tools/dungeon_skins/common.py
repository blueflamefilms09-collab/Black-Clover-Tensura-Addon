"""Why: the paths, imports and small helpers every module of the dungeon-skin pipeline shares (tools/gen_dungeon_skins.py): where the assets live, how the Mob Foundry's 400 rows are
loaded (the same rows the foundry generator reads, so a skin is painted from the very sheet its old model was built from), the foundry asset index (the height and size a mob is drawn at),
and the writer of an 8-bit palette PNG. Nothing here paints. Run python as `python3 -B`.
"""
import json
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
TOOLS = os.path.abspath(os.path.join(HERE, ".."))
ROOT = os.path.abspath(os.path.join(TOOLS, ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
SKINS = os.path.join(ASSETS, "textures", "entity", "playermodel")
GEO = os.path.join(ASSETS, "geo", "entity")
ANIM = os.path.join(ASSETS, "animations", "entity")
TEX = os.path.join(ASSETS, "textures", "entity")
LOOKS_DUNGEON = os.path.join(ASSETS, "playermodel", "looks_dungeon.json")
DOCS_PREVIEW = os.path.join(ROOT, "docs", "previews", "humanoids")

for _p in (TOOLS, os.path.join(TOOLS, "arc"), os.path.join(TOOLS, "foundry"), os.path.join(TOOLS, "playermodel"), HERE):
    if _p not in sys.path:
        sys.path.insert(0, _p)

BASE_HEIGHT = 1.875          # the kit's reference height (client/playermodel/PlayerLook.BASE_HEIGHT)


def seed_of(name):
    return sum((i + 1) * ord(c) for i, c in enumerate(name)) % 9973


def foundry_rows():
    """All 400 rows of the Mob Foundry's roster, in file order (the generator's own loader)."""
    import importlib
    import roster_common as RC
    rows = []
    for road in ("holy", "abyssal"):
        mod = importlib.import_module("roster_" + road)
        for tier in RC.TIERS:
            rows += RC.parse_block(mod.TIERS.get(tier, ""), road, tier, "roster_%s t%d" % (road, tier))
    return rows


def foundry_index():
    """assets/nusmp/foundry/index.json 'mobs': id -> {anim, glow, height, size, tier}."""
    with open(os.path.join(ASSETS, "foundry", "index.json")) as f:
        return json.load(f)["mobs"]


def look_scale(height_blocks):
    return round(height_blocks / BASE_HEIGHT, 4)


def hexrgb(rgb):
    return "#%02x%02x%02x" % tuple(max(0, min(255, int(v))) for v in rgb)


def write_png(img, path):
    """Saves an RGBA image as a plain RGBA PNG (64x64 skins are tiny; palette tricks would only make the audit harder)."""
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path, optimize=True)


def write_json(path, obj, indent=1):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=indent)
        f.write("\n")
