"""
Headless preview of the Decennial boss halls (docs/labyrinth/decennial_bosses.md).

Dumps the REAL plan of every hall (the object the game builds from, via tools/arena_preview/ArenaDump.java compiled against the real
labyrinth/mega/decennial/arena classes), then draws for each hall:
  - a top-down map (the highest block below the ceiling in every column, shaded by height) with the interactive elements, the hazard
    zones, the adds' spots, the doorways and the seal marked;
  - two sections through the middle of the hall (west-east and north-south) from the foundation to the ceiling.
It also writes a contact sheet of every hall. It is not a Minecraft renderer: flat block colours chosen by name, no lighting, no models.

    python tools/arena_preview.py                      # every hall -> docs/previews/arenas/<id>.png and _contact_sheet.png
    python tools/arena_preview.py graveyard cathedral   # only these halls

Needs: JDK 21 (javac/java), Python 3 with numpy + Pillow.
"""
import colorsys
import hashlib
import json
import math
import os
import shutil
import subprocess
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, ".."))
SRC = os.path.join(ROOT, "src", "main", "java")
BUILD = os.environ.get("ARENA_PREVIEW_BUILD") or os.path.join(ROOT, "build", "arena_preview")
OUT = os.path.join(ROOT, "docs", "previews", "arenas")

# ------------------------------------------------------------------------------------------------ colours
WOODS = {"oak": (170, 135, 80), "spruce": (100, 75, 45), "birch": (200, 185, 130), "dark_oak": (60, 40, 20), "crimson": (130, 50, 80), "warped": (30, 120, 110),
         "cherry": (225, 170, 175), "mangrove": (115, 50, 50), "acacia": (170, 95, 60), "jungle": (150, 110, 70), "bamboo": (200, 190, 90)}
DYES = {"white": (235, 235, 235), "orange": (220, 120, 30), "magenta": (190, 70, 180), "light_blue": (90, 170, 220), "yellow": (230, 200, 40), "lime": (120, 190, 30),
        "pink": (235, 140, 170), "gray": (80, 85, 90), "light_gray": (150, 150, 145), "cyan": (30, 130, 150), "purple": (120, 50, 170), "blue": (50, 60, 160),
        "brown": (100, 70, 45), "green": (80, 110, 40), "red": (170, 40, 40), "black": (25, 25, 30)}
# first match wins: (keyword in the block name, rgb)
RULES = [
    ("lava", (235, 105, 20)), ("water", (60, 100, 215)), ("magma", (190, 70, 20)), ("fire", (240, 150, 40)),
    ("glowstone", (255, 210, 120)), ("shroomlight", (245, 160, 90)), ("sea_lantern", (190, 230, 230)), ("lantern", (255, 200, 110)), ("torch", (255, 190, 90)),
    ("candle", (245, 220, 160)), ("lamp", (255, 205, 120)), ("beacon", (130, 240, 240)), ("end_rod", (240, 240, 255)), ("redstone_lamp", (255, 180, 90)),
    ("light", (255, 240, 180)),
    ("crying_obsidian", (70, 25, 125)), ("obsidian", (28, 18, 45)), ("amethyst", (150, 100, 210)), ("calcite", (225, 225, 218)), ("quartz", (236, 230, 224)),
    ("purpur", (170, 125, 170)), ("end_stone", (225, 225, 160)), ("prismarine", (95, 165, 150)), ("packed_ice", (150, 190, 240)), ("blue_ice", (110, 170, 240)),
    ("ice", (160, 200, 245)), ("snow", (245, 248, 250)), ("powder", (245, 248, 250)),
    ("gold", (240, 200, 60)), ("iron_bars", (160, 160, 165)), ("iron", (205, 205, 205)), ("copper", (195, 115, 85)), ("diamond", (100, 230, 220)), ("emerald", (60, 200, 100)),
    ("netherite", (60, 50, 58)), ("deepslate", (72, 72, 80)), ("blackstone", (42, 38, 46)), ("basalt", (62, 62, 68)), ("tuff", (105, 108, 98)), ("cobblestone", (112, 112, 112)),
    ("mossy", (95, 125, 85)), ("andesite", (135, 135, 135)), ("diorite", (205, 205, 205)), ("granite", (155, 105, 90)), ("sandstone", (215, 200, 150)),
    ("red_sand", (190, 100, 40)), ("soul_sand", (82, 66, 56)), ("soul_soil", (75, 60, 50)), ("sand", (220, 205, 150)), ("gravel", (140, 135, 135)),
    ("red_nether", (92, 22, 28)), ("nether_brick", (52, 26, 32)), ("netherrack", (112, 42, 42)), ("nether_wart", (140, 25, 30)), ("crimson_nylium", (140, 30, 60)),
    ("warped_nylium", (30, 125, 115)), ("bone", (228, 222, 202)), ("terracotta", (150, 95, 70)), ("clay", (160, 165, 180)), ("mud", (62, 52, 52)), ("dirt", (130, 95, 60)),
    ("podzol", (100, 75, 40)), ("grass", (95, 160, 60)), ("moss", (90, 122, 50)), ("leaves", (48, 120, 40)), ("azalea", (80, 140, 60)), ("vine", (50, 110, 40)),
    ("sculk", (14, 60, 62)), ("cobweb", (232, 232, 232)), ("web", (232, 232, 232)), ("slime", (110, 200, 90)), ("honey", (240, 170, 40)), ("hay", (210, 180, 40)),
    ("pumpkin", (210, 120, 20)), ("bookshelf", (140, 105, 65)), ("chain", (90, 95, 105)), ("glass", (170, 215, 235)), ("barrier", (255, 0, 0)),
    ("stone", (125, 125, 125)), ("brick", (150, 90, 75)), ("anvil", (60, 60, 65)), ("coal", (30, 30, 34)), ("bell", (240, 200, 70)), ("skull", (225, 220, 205)),
    ("head", (225, 220, 205)), ("banner", (170, 40, 40)), ("flower", (230, 90, 140)), ("rose", (200, 40, 50)), ("mushroom", (190, 80, 70)), ("fungus", (160, 70, 60)),
    ("coral", (230, 90, 160)), ("kelp", (60, 130, 60)), ("seagrass", (70, 140, 70)), ("gate", (140, 110, 70)), ("altar", (90, 230, 240)),
]


def colour_of(spec):
    """Flat colour of a block text like minecraft:stone_bricks[...]; None for air."""
    name = spec.split("[")[0].split(":")[-1]
    if name in ("air", "cave_air", "void_air", "") or spec == "":
        return None
    for wood, c in WOODS.items():
        if name.startswith(wood + "_") or name.startswith("stripped_" + wood):
            return c
    for dye, c in sorted(DYES.items(), key=lambda kv: -len(kv[0])):
        if name.startswith(dye + "_") and any(k in name for k in ("wool", "carpet", "concrete", "terracotta", "glass", "banner", "bed", "candle", "stained")):
            return c
    for key, c in RULES:
        if key in name:
            return c
    h = int(hashlib.md5(name.encode()).hexdigest()[:6], 16)
    r, g, b = colorsys.hsv_to_rgb((h % 360) / 360.0, 0.35, 0.65)
    return (int(r * 255), int(g * 255), int(b * 255))


KIND_COLOURS = {"TOMBSTONE": (255, 120, 255), "PILLAR": (255, 255, 90), "COFFIN": (255, 150, 60), "ROOT_NODE": (120, 255, 120), "BRAZIER": (255, 90, 60), "CRYSTAL": (120, 255, 255),
                "MIRROR": (200, 220, 255), "STATUE": (255, 255, 255), "EGG_SAC": (255, 200, 120), "SHELF": (230, 170, 90), "VALVE": (255, 120, 120), "CHANDELIER": (255, 230, 120),
                "URN": (230, 130, 230), "SHROOM": (255, 140, 200), "TOTEM": (255, 190, 90), "GATE": (255, 60, 60), "BLOOM": (255, 150, 190)}


# ------------------------------------------------------------------------------------------------ build and dump
def run(cmd, **kw):
    r = subprocess.run(cmd, capture_output=True, text=True, **kw)
    if r.returncode != 0:
        sys.stderr.write(r.stdout + r.stderr)
        raise SystemExit("command failed: " + " ".join(cmd))
    return r.stdout


def dump(ids):
    classes = os.path.join(BUILD, "classes")
    dumps = os.path.join(BUILD, "dumps")
    shutil.rmtree(classes, ignore_errors=True)
    os.makedirs(classes, exist_ok=True)
    os.makedirs(dumps, exist_ok=True)
    env = dict(os.environ, JAVA_TOOL_OPTIONS="")
    run(["javac", "-Xlint:none", "-d", classes, "-sourcepath", SRC, os.path.join(HERE, "arena_preview", "ArenaDump.java")], env=env)
    run(["java", "-cp", classes, "ArenaDump", dumps] + ids, env=env)
    return dumps


# ------------------------------------------------------------------------------------------------ drawing
def load(path):
    d = json.load(open(path, encoding="utf-8"))
    size, height, found = d["size"], d["height"], d["foundation"]
    flat = np.zeros(size * size * (height + found), dtype=np.uint16)
    pos = 0
    for run_len, idx in d["rle"]:
        flat[pos:pos + run_len] = idx
        pos += run_len
    d["cells"] = flat.reshape((height + found, size, size))       # y (from -foundation), z, x
    pal = []
    for spec in d["palette"]:
        pal.append(colour_of(spec))
    d["colours"] = pal
    return d


def top_down(d):
    size, height, found = d["size"], d["height"], d["foundation"]
    img = np.zeros((size, size, 3), dtype=np.float32)
    top = height - 5                                                # below the ceiling and its cornice
    cells = d["cells"]
    cols = d["colours"]
    for z in range(size):
        for x in range(size):
            shown = None
            for y in range(top, -found - 1, -1):
                idx = int(cells[y + found, z, x])
                c = cols[idx] if idx else None
                if c is not None:
                    shown = (c, y)
                    break
            if shown is None:
                img[z, x] = (12, 12, 16)
            else:
                (c, y) = shown
                k = 0.55 + 0.45 * max(0.0, min(1.0, (y + found) / float(top + found)))
                img[z, x] = np.array(c, dtype=np.float32) * k
    return np.clip(img, 0, 255).astype(np.uint8)


def section(d, axis):
    """axis 'x': a cut at x = middle (columns are z); axis 'z': a cut at z = middle (columns are x)."""
    size, height, found = d["size"], d["height"], d["foundation"]
    c = size // 2
    cells, cols = d["cells"], d["colours"]
    img = np.zeros((height + found, size, 3), dtype=np.uint8)
    for yi in range(height + found):
        for i in range(size):
            idx = int(cells[yi, i, c] if axis == "x" else cells[yi, c, i])
            col = cols[idx] if idx else None
            img[height + found - 1 - yi, i] = col if col is not None else (14, 14, 20)
    return img


def draw_theme(d, scale=8):
    size, height, found = d["size"], d["height"], d["foundation"]
    plan = Image.fromarray(top_down(d)).resize((size * scale, size * scale), Image.NEAREST)
    pd = ImageDraw.Draw(plan)
    for z in d["zones"]:                                            # hazard zones: rings
        kind, x, zz, r = z
        cx, cy = (x + 0.5) * scale, (zz + 0.5) * scale
        h = int(hashlib.md5(kind.encode()).hexdigest()[:2], 16)
        col = (60 + h % 190, 255 - (h * 3) % 150, 90 + (h * 7) % 160)
        pd.ellipse([cx - r * scale, cy - r * scale, cx + r * scale, cy + r * scale], outline=col, width=1)
    for sx, sz in d["spots"]:                                       # adds' spots: crosses
        cx, cy = (sx + 0.5) * scale, (sz + 0.5) * scale
        pd.line([cx - 4, cy - 4, cx + 4, cy + 4], fill=(255, 255, 255), width=1)
        pd.line([cx - 4, cy + 4, cx + 4, cy - 4], fill=(255, 255, 255), width=1)
    for e in d["elements"]:                                         # interactive elements: boxed in their kind's colour
        col = KIND_COLOURS.get(e["kind"], (255, 255, 255))
        xs = [c[0] for c in e["cells"]]
        zs = [c[2] for c in e["cells"]]
        pd.rectangle([min(xs) * scale - 1, min(zs) * scale - 1, (max(xs) + 1) * scale, (max(zs) + 1) * scale], outline=col, width=2 if not e["locked"] else 3)
    for x, y, z in d["seal"]:                                       # the entry seal
        pd.rectangle([x * scale, z * scale, (x + 1) * scale - 1, (z + 1) * scale - 1], outline=(255, 235, 60))
    gx, gz = d["gate"]
    ex, ez = d["entry"]
    pd.polygon([((gx + 0.5) * scale, 0), ((gx - 1.5) * scale, 3 * scale), ((gx + 2.5) * scale, 3 * scale)], outline=(255, 60, 60))
    pd.polygon([((ex + 0.5) * scale, size * scale), ((ex - 1.5) * scale, (size - 3) * scale), ((ex + 2.5) * scale, (size - 3) * scale)], outline=(60, 255, 90))
    sx = Image.fromarray(section(d, "x")).resize((size * scale, (height + found) * scale), Image.NEAREST)
    sz = Image.fromarray(section(d, "z")).resize((size * scale, (height + found) * scale), Image.NEAREST)
    for im, label in ((sx, "section through the middle, north to south (left = north gate)"), (sz, "section through the middle, west to east")):
        dr = ImageDraw.Draw(im)
        ground = (height) * scale
        dr.line([0, ground, size * scale, ground], fill=(255, 255, 255), width=1)
        dr.text((6, 4), label, fill=(255, 255, 255))
    w = size * scale * 2 + 30
    hh = max(size * scale + 70, 2 * (height + found) * scale + 90)
    out = Image.new("RGB", (w, hh), (24, 24, 30))
    dr = ImageDraw.Draw(out)
    dr.text((10, 8), "%s  [%s]   %s   %dx%dx%d" % (d["title"], d["id"], d["side"], size, size, height), fill=(255, 255, 255))
    kinds = sorted({e["kind"] for e in d["elements"]})
    x0 = 10
    for k in kinds:
        col = KIND_COLOURS.get(k, (255, 255, 255))
        dr.rectangle([x0, 26, x0 + 10, 36], outline=col, width=2)
        n = sum(1 for e in d["elements"] if e["kind"] == k)
        dr.text((x0 + 14, 25), "%s x%d" % (k.lower(), n), fill=(220, 220, 220))
        x0 += 14 + 8 * (len(k) + 4) + 10
    dr.text((10, 42), "top-down (highest block below the ceiling); rings = hazard zones, crosses = adds' spots, yellow = entry seal, red = Gatekeeper, green = entry", fill=(170, 170, 180))
    out.paste(plan, (10, 60))
    out.paste(sx, (size * scale + 20, 60))
    out.paste(sz, (size * scale + 20, 60 + (height + found) * scale + 20))
    return out, plan


def contact_sheet(plans, path, cols=6, cell=190):
    rows = (len(plans) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * (cell + 8) + 8, rows * (cell + 26) + 8), (20, 20, 26))
    dr = ImageDraw.Draw(sheet)
    for i, (name, im) in enumerate(plans):
        thumb = im.copy()
        thumb.thumbnail((cell, cell), Image.NEAREST)
        cx = 8 + (i % cols) * (cell + 8)
        cy = 8 + (i // cols) * (cell + 26)
        sheet.paste(thumb, (cx, cy))
        dr.text((cx, cy + cell + 4), name, fill=(230, 230, 230))
    sheet.save(path)


def main():
    ids = [a for a in sys.argv[1:] if not a.startswith("-")]
    dumps = dump(ids)
    os.makedirs(OUT, exist_ok=True)
    names = ids or sorted(f[:-5] for f in os.listdir(dumps) if f.endswith(".json"))
    plans = []
    for name in names:
        d = load(os.path.join(dumps, name + ".json"))
        img, plan = draw_theme(d)
        img.save(os.path.join(OUT, name + ".png"), optimize=True)
        plans.append((name, plan))
        print("drew", name)
    if not ids:
        contact_sheet(plans, os.path.join(OUT, "_contact_sheet.png"))
        print("drew the contact sheet of", len(plans), "halls")


if __name__ == "__main__":
    main()
