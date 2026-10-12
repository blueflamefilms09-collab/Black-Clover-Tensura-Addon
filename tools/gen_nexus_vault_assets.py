#!/usr/bin/env python3
"""
1.21.0: the assets of the Living Dungeon's hidden vaults and gates (docs/living_dungeon.md, "Hidden Mana Vaults" and "Navigation"):

  Illusion Stone and Illusion Deepslate   no new pixels at all: their models point at the vanilla stone and deepslate textures (with the same
                                          random turns the vanilla blocks have), so they look exactly like the rock they sit in
  Warp Trap                               the same vanilla rock with a barely-there rune on its top face (one overlay texture per rock);
                                          stone and deepslate variants
  Descent Gate                            an animated swirl, violet and crimson going down, pale gold and cyan going up (rising=true)
  Nutrient Bloom                          four growth stages of a glowing flower, and the Nutrient Nectar it gives

    python3 tools/gen_nexus_vault_assets.py

Writes assets/nusmp/textures/block/{descent_gate,descent_gate_up}.png (+ .mcmeta), nutrient_bloom_0..3.png, warp_trap_rune.png, warp_trap_rune_deep.png,
textures/item/nutrient_nectar.png, blockstates, block and item models, data/nusmp/tags/block/illusion_stones.json, and the lang fragment for the lead
(--lang PATH: a flat JSON fragment of the lang keys is written there; without it none is written). Idempotent: running it again rewrites the same files.
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
DATA = os.path.join(RES, "data", "nusmp")


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


# ------------------------------------------------------------------------------------------------------------------ the Descent Gate
def swirl_sheet(core, mid, edge, seed, arms=3, frames=8):
    """16 x (16 * frames): a swirl turning by one arm's width over the loop, opaque in the middle and clear at the rim (the model is translucent)."""
    n = 16
    sheet = Image.new("RGBA", (n, n * frames), (0, 0, 0, 0))
    nz = noise(n, seed)
    rng = random.Random(seed)
    sparks = [(rng.uniform(0, 1), rng.uniform(0, 1), rng.uniform(0, 2 * math.pi)) for _ in range(5)]
    for f in range(frames):
        phi = f / frames * (2 * math.pi / arms)
        img = np.zeros((n, n, 4), np.uint8)
        for y in range(n):
            for x in range(n):
                dx, dy = x - 7.5, y - 7.5
                r = math.hypot(dx, dy) / 8.0
                a = math.atan2(dy, dx) + r * 3.2 - phi
                arm = 0.5 + 0.5 * math.sin(a * arms)
                t = min(1.0, r)
                col = np.array(core, float) * (1 - t) ** 1.6 + np.array(mid, float) * (arm * 0.7 + 0.3) * (1 - abs(t - 0.5) * 1.2) + np.array(edge, float) * t ** 2
                col = col * (0.88 + 0.24 * nz[y, x])
                img[y, x, :3] = np.clip(col, 0, 255)
                img[y, x, 3] = int(255 * min(1.0, max(0.0, 1.18 - r ** 2.4)))
        for (rr, ra, rp) in sparks:                                      # a few bright motes drifting round
            ang = ra + rp + f / frames * 2 * math.pi
            rad = 0.25 + 0.55 * rr
            x, y = int(round(7.5 + math.cos(ang) * rad * 8)), int(round(7.5 + math.sin(ang) * rad * 8))
            if 0 <= x < n and 0 <= y < n:
                img[y, x] = (255, 255, 255, 255)
        sheet.paste(Image.fromarray(img, "RGBA"), (0, f * n))
    return sheet


# ------------------------------------------------------------------------------------------------------------------ the Warp Trap rune
RUNE = [
    "................",
    "......XXXX......",
    "....XX....XX....",
    "...X..........X.",
    "..X.....XX....X.",
    "..X....X..X...X.",
    ".X.....X..X....X",
    ".X......XX.....X",
    ".X.....XXXX....X",
    ".X........X....X",
    "..X.......X...X.",
    "..X...XX..X..X..",
    "...X...XXX..XX..",
    "....XX......X...",
    "......XXXXXX....",
    "................",
]


def rune_overlay(color, drop=0.0, seed=1):
    """A 16x16 overlay: only the rune's pixels (a ring broken in places, with a mark inside), everything else clear."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(RUNE):
        for x, ch in enumerate(row):
            if ch == "X" and rng.random() >= drop:
                px[x, y] = color + (255,)
    return img


# ------------------------------------------------------------------------------------------------------------------ the Nutrient Bloom
STEM = (52, 138, 84)
STEM_DARK = (34, 96, 62)
LEAF = (86, 196, 116)
OUT = (20, 40, 36)


def put(px, pts, col):
    for (x, y) in pts:
        if 0 <= x < 16 and 0 <= y < 16:
            px[(x, y)] = col


def outline(px):
    cells = set(px)
    for (x, y) in list(cells):
        for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            q = (x + dx, y + dy)
            if q not in cells and 0 <= q[0] < 16 and 0 <= q[1] < 16:
                px[q] = OUT
    return px


def bloom(stage):
    px = {}
    top = {0: 11, 1: 8, 2: 6, 3: 7}[stage]
    for y in range(top + (2 if stage < 3 else 3), 16):                       # the stem
        put(px, [(7, y)], STEM)
        put(px, [(8, y)], STEM_DARK)
    put(px, [(5, 13), (4, 12), (10, 12), (11, 11)], LEAF)                    # leaves
    put(px, [(6, 14), (9, 13)], LEAF)
    if stage == 0:                                                           # a bud no bigger than a seed
        put(px, [(7, 10), (8, 10), (7, 11), (8, 11)], (120, 230, 205))
        put(px, [(7, 10)], (210, 255, 240))
    elif stage == 1:                                                         # a closed teal bud
        put(px, [(7, 7), (8, 7), (6, 8), (7, 8), (8, 8), (9, 8), (6, 9), (7, 9), (8, 9), (9, 9), (7, 10), (8, 10)], (96, 214, 196))
        put(px, [(7, 7), (7, 8), (6, 8)], (190, 255, 238))
    elif stage == 2:                                                         # the bud opening, a warm glow showing
        put(px, [(6, 5), (9, 5), (5, 6), (10, 6), (5, 7), (10, 7), (6, 8), (9, 8), (6, 9), (9, 9)], (130, 232, 212))
        put(px, [(7, 6), (8, 6), (7, 7), (8, 7), (7, 8), (8, 8)], (255, 236, 150))
        put(px, [(7, 6)], (255, 252, 210))
        put(px, [(7, 9), (8, 9)], (96, 214, 196))
    else:                                                                    # in bloom: five rounded petals round a glowing heart
        cx, cy, big = 7.5, 5.0, 4.9
        for y in range(16):
            for x in range(16):
                dx, dy = x - cx, y - cy
                r = math.hypot(dx, dy)
                th = math.atan2(dy, dx)
                if r <= big * (0.72 + 0.28 * math.cos(5 * th)):
                    if r < 1.5:
                        px[(x, y)] = (255, 224, 110)
                    elif r < 2.6:
                        px[(x, y)] = (240, 255, 250)
                    else:
                        px[(x, y)] = (170, 252, 232) if (x + y) % 2 == 0 else (150, 238, 220)
        px[(7, 4)] = (255, 250, 200)
        put(px, [(2, 1), (13, 6), (1, 9), (14, 2)], (190, 255, 220))         # spores drifting off
    return outline(px)


def image_from(px):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    q = img.load()
    for (x, y), c in px.items():
        q[x, y] = c + (255,)
    return img


def nectar():
    """A small stoppered vial of glowing green-gold nectar."""
    px = {}
    glass = (186, 214, 226)
    glass_dark = (120, 150, 168)
    cork = (150, 104, 60)
    put(px, [(6, 1), (7, 1), (8, 1), (9, 1)], cork)
    put(px, [(7, 2), (8, 2)], glass)
    put(px, [(7, 3), (8, 3)], glass_dark)
    for y in range(4, 14):
        w = {4: 2, 5: 3, 6: 4, 7: 5, 8: 6, 9: 6, 10: 6, 11: 5, 12: 4, 13: 3}[y]
        x0 = 8 - w // 2 - (0 if w % 2 == 0 else 0)
        for x in range(x0, x0 + w):
            t = (y - 4) / 9.0
            c = (int(170 + 70 * t), int(255 - 25 * t), int(150 - 20 * t))
            px[(x, y)] = c
        px[(x0 - 1, y)] = glass_dark
        px[(x0 + w, y)] = glass_dark
    put(px, [(6, 14), (7, 14), (8, 14), (9, 14)], glass_dark)
    put(px, [(6, 6), (6, 7), (6, 8)], (240, 255, 240))                      # the glint
    put(px, [(9, 12), (8, 11)], (255, 250, 190))
    return outline(px)


# ------------------------------------------------------------------------------------------------------------------ models
def column(side_tex, end_tex, mirrored=False, overlay=None):
    """A full cube with a different end texture, as one self-contained model (the vanilla rock textures by reference). The overlay is a second cube face on top."""
    uv = [16, 0, 0, 16] if mirrored else [0, 0, 16, 16]
    faces = {
        "down": {"uv": [0, 0, 16, 16], "texture": "#end", "cullface": "down"},
        "up": {"uv": [0, 0, 16, 16], "texture": "#end", "cullface": "up"},
        "north": {"uv": uv, "texture": "#side", "cullface": "north"},
        "south": {"uv": uv, "texture": "#side", "cullface": "south"},
        "west": {"uv": uv, "texture": "#side", "cullface": "west"},
        "east": {"uv": uv, "texture": "#side", "cullface": "east"},
    }
    textures = {"particle": side_tex, "side": side_tex, "end": end_tex}
    elements = [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces}]
    model = {"parent": "minecraft:block/block", "textures": textures, "elements": elements}
    if overlay:
        textures["rune"] = overlay
        elements.append({"from": [0, 0, 0], "to": [16, 16, 16], "faces": {"up": {"uv": [0, 0, 16, 16], "texture": "#rune", "cullface": "up"}}})
        model["render_type"] = "minecraft:cutout"
    return model


def cube_with_rune(all_tex, overlay):
    return {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
            "textures": {"particle": all_tex, "all": all_tex, "rune": overlay},
            "elements": [
                {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {d: {"uv": [0, 0, 16, 16], "texture": "#all", "cullface": d} for d in ("down", "up", "north", "south", "west", "east")}},
                {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {"up": {"uv": [0, 0, 16, 16], "texture": "#rune", "cullface": "up"}}}]}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--lang", default=None, help="where to write the lang fragment (flat JSON); omitted: not written")
    args = ap.parse_args()
    tex = os.path.join(ASSETS, "textures", "block")
    itex = os.path.join(ASSETS, "textures", "item")
    mdl = os.path.join(ASSETS, "models", "block")
    imdl = os.path.join(ASSETS, "models", "item")
    bs = os.path.join(ASSETS, "blockstates")
    for d in (tex, itex, mdl, imdl, bs):
        os.makedirs(d, exist_ok=True)

    # ---- the gate: animated swirls (down: violet to crimson, up: pale gold and cyan)
    for name, core, mid, edge, seed, arms in (("descent_gate", (255, 226, 244), (206, 44, 128), (52, 8, 66), 41, 3),
                                              ("descent_gate_up", (255, 250, 224), (74, 204, 232), (8, 38, 72), 42, 2)):
        swirl_sheet(core, mid, edge, seed, arms).save(os.path.join(tex, name + ".png"))
        write_json(os.path.join(tex, name + ".png.mcmeta"), {"animation": {"frametime": 3, "interpolate": True}})
        write_json(os.path.join(mdl, name + ".json"), {"parent": "minecraft:block/cube_all", "render_type": "minecraft:translucent", "textures": {"all": "nusmp:block/" + name}})
    write_json(os.path.join(bs, "descent_gate.json"), {"variants": {"rising=false": {"model": "nusmp:block/descent_gate"}, "rising=true": {"model": "nusmp:block/descent_gate_up"}}})
    write_json(os.path.join(imdl, "descent_gate.json"), {"parent": "nusmp:block/descent_gate"})

    # ---- the rock illusions: vanilla textures by reference, with the same random turns as vanilla stone and deepslate
    stone = "minecraft:block/stone"
    write_json(os.path.join(mdl, "illusion_stone.json"), {"parent": "minecraft:block/cube_all", "textures": {"all": stone}})
    turns = [{"model": "nusmp:block/illusion_stone"}] + [{"model": "nusmp:block/illusion_stone", "y": y} for y in (90, 180, 270)]
    write_json(os.path.join(bs, "illusion_stone.json"), {"variants": {"": turns}})
    write_json(os.path.join(imdl, "illusion_stone.json"), {"parent": "nusmp:block/illusion_stone"})
    ds, dt = "minecraft:block/deepslate", "minecraft:block/deepslate_top"
    write_json(os.path.join(mdl, "illusion_deepslate.json"), column(ds, dt))
    write_json(os.path.join(mdl, "illusion_deepslate_mirrored.json"), column(ds, dt, mirrored=True))
    write_json(os.path.join(bs, "illusion_deepslate.json"), {"variants": {"": [{"model": "nusmp:block/illusion_deepslate"}, {"model": "nusmp:block/illusion_deepslate_mirrored"}]}})
    write_json(os.path.join(imdl, "illusion_deepslate.json"), {"parent": "nusmp:block/illusion_deepslate"})

    # ---- the warp trap: the same rock, a rune on top. The rune is a hair off the rock's own tone.
    rune_overlay((111, 115, 124), drop=0.2, seed=7).save(os.path.join(tex, "warp_trap_rune.png"))
    rune_overlay((46, 50, 58), drop=0.2, seed=7).save(os.path.join(tex, "warp_trap_rune_deep.png"))
    write_json(os.path.join(mdl, "warp_trap.json"), cube_with_rune(stone, "nusmp:block/warp_trap_rune"))
    write_json(os.path.join(mdl, "warp_trap_deep.json"), column(ds, dt, overlay="nusmp:block/warp_trap_rune_deep"))
    write_json(os.path.join(mdl, "warp_trap_deep_mirrored.json"), column(ds, dt, mirrored=True, overlay="nusmp:block/warp_trap_rune_deep"))
    write_json(os.path.join(bs, "warp_trap.json"), {"variants": {
        "deep=false": [{"model": "nusmp:block/warp_trap"}] + [{"model": "nusmp:block/warp_trap", "y": y} for y in (90, 180, 270)],
        "deep=true": [{"model": "nusmp:block/warp_trap_deep"}, {"model": "nusmp:block/warp_trap_deep_mirrored"}]}})
    write_json(os.path.join(imdl, "warp_trap.json"), {"parent": "nusmp:block/warp_trap"})

    # ---- the nutrient bloom and its nectar
    for stage in range(4):
        image_from(bloom(stage)).save(os.path.join(tex, "nutrient_bloom_%d.png" % stage))
        write_json(os.path.join(mdl, "nutrient_bloom_%d.json" % stage), {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout", "textures": {"cross": "nusmp:block/nutrient_bloom_%d" % stage}})
    write_json(os.path.join(bs, "nutrient_bloom.json"), {"variants": {"age=%d" % a: {"model": "nusmp:block/nutrient_bloom_%d" % a} for a in range(4)}})
    write_json(os.path.join(imdl, "nutrient_bloom.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:block/nutrient_bloom_3"}})
    image_from(nectar()).save(os.path.join(itex, "nutrient_nectar.png"))
    write_json(os.path.join(imdl, "nutrient_nectar.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/nutrient_nectar"}})

    # ---- the tag the Ki-Resonance Lantern reads
    write_json(os.path.join(DATA, "tags", "block", "illusion_stones.json"), {"replace": False, "values": ["nusmp:illusion_stone", "nusmp:illusion_deepslate"]})

    # ---- the lang fragment (merged into en_us.json by the lead)
    if args.lang:
        write_json(args.lang, {
            "block.nusmp.illusion_stone": "Illusion Stone", "block.nusmp.illusion_deepslate": "Illusion Deepslate", "block.nusmp.descent_gate": "Descent Gate",
            "block.nusmp.warp_trap": "Warp Trap", "block.nusmp.nutrient_bloom": "Nutrient Bloom", "item.nusmp.nutrient_nectar": "Nutrient Nectar"})
    print("done")


if __name__ == "__main__":
    main()
