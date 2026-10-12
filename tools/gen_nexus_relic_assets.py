#!/usr/bin/env python3
"""
1.21.0: the block assets of the Treasury (docs/living_dungeon.md section 7): the relic pedestal and the mana-locked gate. (The relic ITEMS' art
belongs to tools/gen_nexus_relic_icons.py and gen_weapon_models.py.)

  Relic Pedestal   a dark basalt plinth with a gold-inlaid plate; a small glowing relic core floats above it, in the colour of the relic it
                   holds (six block states by relic x central x taken: the central pedestal has violet-and-gold stonework)
  Mana Gate        a wall of dark stone with a glowing sigil (an animated pulse); open (a Mana Zone or Magic Sense near) it is a faint,
                   translucent outline you can walk through

    python3 tools/gen_nexus_relic_assets.py [--lang PATH]

Writes assets/nusmp/textures/block/artifact_block_{side,top}.png, artifact_block_central_{side,top}.png, artifact_block_core_<relic>.png,
mana_gate.png (+ .mcmeta), mana_gate_open.png, the block models, blockstates, the two block item models, and (with --lang) a flat JSON
fragment of the lang keys. Idempotent: running it again rewrites the same files.
"""
import argparse
import json
import math
import os
import random

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", "nusmp")

# the relics, in the order of the Relic enum: (item id, short name for files, colour of the floating core)
RELICS = [
    ("glutton_chalice", "chalice", (96, 255, 150)),
    ("mantle_of_the_scavenger", "mantle", (238, 154, 62)),
    ("band_of_the_spatial_anchor", "band", (96, 204, 255)),
    ("devil_bound_sheath", "sheath", (255, 150, 232)),
    ("gamblers_grimoire_page", "page", (255, 236, 190)),
]


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


def arr(img):
    return np.array(img.convert("RGBA"), dtype=np.int32)


def to_img(a):
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


# ------------------------------------------------------------------------------------------------------------------ the pedestal
def stone(base, seed, spread=14):
    """A 16x16 block of speckled dark stone."""
    nz = noise(16, seed)
    a = np.zeros((16, 16, 4), np.int32)
    for y in range(16):
        for x in range(16):
            k = (nz[y, x] - 0.5) * 2 * spread
            a[y, x] = (base[0] + k, base[1] + k, base[2] + k * 1.1, 255)
    return a


def pedestal_side(central):
    base = (30, 22, 46) if central else (34, 32, 40)
    trim = (232, 192, 92) if central else (196, 160, 72)
    gem = (186, 96, 255) if central else (120, 130, 150)
    a = stone(base, 11 if central else 3)
    for x in range(16):                                         # gold bands: the foot, the waist and under the plate
        for y in (0, 1, 7, 8):
            a[y, x, :3] = trim if y in (0, 7) else (np.array(trim) * 0.62).astype(int)
    for y in range(2, 7):                                       # a carved channel down the column
        a[y, 7, :3] = (np.array(base) * 0.55).astype(int)
        a[y, 8, :3] = (np.array(base) * 0.55).astype(int)
    a[4, 7, :3] = gem
    a[4, 8, :3] = gem
    for y in range(9, 16):                                      # the lower base: darker, with a gold stud
        for x in range(16):
            a[y, x, :3] = (a[y, x, :3] * 0.8).astype(int)
    for x in (2, 13):
        a[12, x, :3] = trim
    return to_img(a)


def pedestal_top(central):
    base = (26, 20, 40) if central else (40, 38, 46)
    trim = (232, 192, 92) if central else (196, 160, 72)
    glow = (186, 120, 255) if central else (150, 150, 180)
    a = stone(base, 21 if central else 5, 10)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if 5.6 <= d < 6.6:
                a[y, x, :3] = trim                              # the inlaid ring
            elif d < 3.2:
                a[y, x, :3] = (np.array(base) * 0.6).astype(int)  # the dish the relic floats over
            elif 3.2 <= d < 4.0:
                a[y, x, :3] = glow
    a[7:9, 7:9, :3] = glow
    return to_img(a)


def core_texture(color, seed):
    """The floating relic core: opaque, bright in the middle and the relic's own colour at the edge, a few white sparks."""
    nz = noise(16, seed, 0)
    a = np.zeros((16, 16, 4), np.int32)
    c = np.array(color, float)
    for y in range(16):
        for x in range(16):
            r = min(1.0, math.hypot(x - 7.5, y - 7.5) / 8.0)
            col = (1 - r ** 1.5) * np.array([255, 255, 255], float) * 0.85 + c * (0.55 + 0.45 * r) * (0.9 + 0.2 * nz[y, x])
            a[y, x] = (*np.clip(col, 0, 255), 255)
    return to_img(a)


def pedestal_model(central, relic_short):
    prefix = "artifact_block_central" if central else "artifact_block"
    tex = {"particle": "nusmp:block/%s_side" % prefix, "side": "nusmp:block/%s_side" % prefix, "top": "nusmp:block/%s_top" % prefix}
    if relic_short:
        tex["core"] = "nusmp:block/artifact_block_core_" + relic_short

    def face(texture, uv, cull=None, **extra):
        f = {"uv": uv, "texture": texture}
        if cull:
            f["cullface"] = cull
        f.update(extra)
        return f

    def cube(x1, y1, z1, x2, y2, z2, side_uv, top_uv, down_cull=False):
        return {"from": [x1, y1, z1], "to": [x2, y2, z2], "faces": {
            "north": face("#side", side_uv), "south": face("#side", side_uv), "west": face("#side", side_uv), "east": face("#side", side_uv),
            "up": face("#top", top_uv), "down": face("#side", [x1, z1, x2, z2], "down" if down_cull else None)}}

    elements = [
        cube(1, 0, 1, 15, 2, 15, [1, 0, 15, 2], [1, 1, 15, 15], True),         # the foot
        cube(4, 2, 4, 12, 8, 12, [4, 2, 12, 8], [4, 4, 12, 12]),               # the column
        cube(2, 8, 2, 14, 10, 14, [2, 7, 14, 9], [2, 2, 14, 14]),               # the plate
    ]
    if relic_short:                                                              # the relic's core, turned a quarter so it reads as a gem
        core_face = lambda: {"uv": [0, 0, 16, 16], "texture": "#core"}
        elements.append({"from": [6.2, 11.2, 6.2], "to": [9.8, 14.8, 9.8], "rotation": {"angle": 45, "axis": "y", "origin": [8, 13, 8]}, "shade": False,
                         "faces": {k: core_face() for k in ("north", "south", "west", "east", "up", "down")}})
    return {"parent": "minecraft:block/block", "textures": tex, "elements": elements}


# ------------------------------------------------------------------------------------------------------------------ the gate
SIGIL = [
    "................",
    "................",
    "......XXXX......",
    "....XX.XX.XX....",
    "...X...XX...X...",
    "...X...XX...X...",
    "..X....XX....X..",
    "..XXXXXXXXXXXX..",
    "..XXXXXXXXXXXX..",
    "..X....XX....X..",
    "...X...XX...X...",
    "...X...XX...X...",
    "....XX.XX.XX....",
    "......XXXX......",
    "................",
    "................",
]


def sigil_cells():
    return {(x, y) for y, row in enumerate(SIGIL) for x, ch in enumerate(row) if ch == "X"}


def gate_frame(pulse, seed=33):
    """The closed gate: dark violet stone, a lighter border with gold studs, and the sigil glowing with the given pulse (0..1)."""
    a = stone((28, 22, 50), seed, 9)
    cells = sigil_cells()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                a[y, x, :3] = (66, 54, 108)
            if (x, y) in cells:
                d = math.hypot(x - 7.5, y - 7.5) / 8.0
                k = 0.62 + 0.38 * pulse
                a[y, x, :3] = np.array([170 + 70 * (1 - d), 120 + 60 * (1 - d), 255]) * k
    for (x, y) in ((1, 1), (14, 1), (1, 14), (14, 14)):
        a[y, x, :3] = (214, 172, 80)
    return to_img(a)


def gate_sheet(frames=8):
    sheet = Image.new("RGBA", (16, 16 * frames))
    for f in range(frames):
        pulse = 0.5 + 0.5 * math.sin(f / frames * 2 * math.pi)
        sheet.paste(gate_frame(pulse), (0, f * 16))
    return sheet


def gate_open():
    """The open gate: only the sigil and the border remain, faint, and the rest is clear."""
    a = np.zeros((16, 16, 4), np.int32)
    cells = sigil_cells()
    for y in range(16):
        for x in range(16):
            if (x, y) in cells:
                a[y, x] = (150, 200, 255, 120)
            elif x in (0, 15) or y in (0, 15):
                a[y, x] = (120, 100, 190, 70)
    return to_img(a)


# ------------------------------------------------------------------------------------------------------------------ main
def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--lang", default=None, help="where to write the lang fragment (flat JSON); omitted: not written")
    args = ap.parse_args()
    tex = os.path.join(ASSETS, "textures", "block")
    mdl = os.path.join(ASSETS, "models", "block")
    imdl = os.path.join(ASSETS, "models", "item")
    bs = os.path.join(ASSETS, "blockstates")
    for d in (tex, mdl, imdl, bs):
        os.makedirs(d, exist_ok=True)

    # ---- the pedestal
    for central in (False, True):
        prefix = "artifact_block_central" if central else "artifact_block"
        pedestal_side(central).save(os.path.join(tex, prefix + "_side.png"))
        pedestal_top(central).save(os.path.join(tex, prefix + "_top.png"))
    for i, (rid, short, color) in enumerate(RELICS):
        core_texture(color, 70 + i).save(os.path.join(tex, "artifact_block_core_%s.png" % short))
    variants = {}
    for central in (False, True):
        prefix = "artifact_block_central" if central else "artifact_block"
        write_json(os.path.join(mdl, prefix + "_empty.json"), pedestal_model(central, None))
        for rid, short, color in RELICS:
            write_json(os.path.join(mdl, "%s_%s.json" % (prefix, short)), pedestal_model(central, short))
        for taken in (False, True):
            for rid, short in [("empty", None)] + [(r[0], r[1]) for r in RELICS]:
                key = "central=%s,relic=%s,taken=%s" % (str(central).lower(), rid, str(taken).lower())
                bare = taken or short is None
                variants[key] = {"model": "nusmp:block/%s_%s" % (prefix, "empty" if bare else short)}
    write_json(os.path.join(bs, "artifact_block.json"), {"variants": dict(sorted(variants.items()))})
    write_json(os.path.join(imdl, "artifact_block.json"), {"parent": "nusmp:block/artifact_block_central_chalice"})

    # ---- the gate
    gate_sheet().save(os.path.join(tex, "mana_gate.png"))
    write_json(os.path.join(tex, "mana_gate.png.mcmeta"), {"animation": {"frametime": 6, "interpolate": True}})
    gate_open().save(os.path.join(tex, "mana_gate_open.png"))
    write_json(os.path.join(mdl, "mana_gate.json"), {"parent": "minecraft:block/cube_all", "textures": {"all": "nusmp:block/mana_gate"}})
    write_json(os.path.join(mdl, "mana_gate_open.json"), {"parent": "minecraft:block/cube_all", "render_type": "minecraft:translucent",
                                                           "textures": {"all": "nusmp:block/mana_gate_open"}})
    write_json(os.path.join(bs, "mana_gate.json"), {"variants": {"open=false": {"model": "nusmp:block/mana_gate"}, "open=true": {"model": "nusmp:block/mana_gate_open"}}})
    write_json(os.path.join(imdl, "mana_gate.json"), {"parent": "nusmp:block/mana_gate"})

    # ---- the words
    if args.lang:
        write_json(args.lang, {
            "block.nusmp.artifact_block": "Relic Pedestal",
            "block.nusmp.mana_gate": "Mana Gate",
            "item.nusmp.glutton_chalice": "The Glutton's Chalice",
            "item.nusmp.mantle_of_the_scavenger": "Mantle of the Scavenger",
            "item.nusmp.band_of_the_spatial_anchor": "Band of the Spatial Anchor",
            "item.nusmp.devil_bound_sheath": "Devil-Bound Weapon Sheath",
            "item.nusmp.gamblers_grimoire_page": "The Gambler's Grimoire Page",
        })


if __name__ == "__main__":
    main()
