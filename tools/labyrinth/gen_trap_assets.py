#!/usr/bin/env python3
"""
Assets of the Labyrinth's traps and puzzles (docs/labyrinth/f1_traps.md): the pitfall tile (no texture of its own: it WEARS the Labyrinth tile
texture, that is the whole trick), the snuffer glyph, the four sigil plates (unlit and lit), the riddle stone, the puzzle Gatekeeper door,
their blockstates, models, item models and loot tables, and the additive entries in the shared pickaxe tag and the Gatekeeper blockstate.
Pillow only, deterministic (seeded), idempotent.

    python3 tools/labyrinth/gen_trap_assets.py            # write everything
    python3 tools/labyrinth/gen_trap_assets.py --check    # exit 1 unless every file on disk is what the script writes
    python3 tools/labyrinth/gen_trap_assets.py --sheet out.png   # a contact sheet of the textures, 8x (to look at them)
"""
import argparse
import io
import json
import math
import os
import random
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, ".."))
import gen_block_textures as base  # noqa: E402
import gen_labyrinth_seal_assets as seal  # noqa: E402

RES = base.RES
ASSETS = base.ASSETS
DATA = base.DATA
TEX = os.path.join(ASSETS, "textures", "block")

SEAM, GOLD, STONE_DARK, STONE_LIGHT = base.SEAM, base.GOLD, base.STONE_DARK, base.STONE_LIGHT
mix = base.mix

#: the four sigil colours: red, blue, gold, green (PuzzleState.COLOUR_NAMES order)
SIGILS = [(214, 58, 66), (64, 118, 236), (236, 188, 52), (66, 208, 118)]
SIGIL_NAMES = ["red", "blue", "gold", "green"]
TEAL = (70, 214, 186)          # the puzzle door's seal colour (warden violet, boss crimson, puzzle teal)
VOID_VIOLET = (96, 64, 150)


def px(img):
    return img.load()


def stone(seed, speckle=0.1):
    return base.stone_base(seed, speckle)


def snuffer_frame(phase):
    """16x16: dark stone, a ring of eight notches, and in the middle a snuffed flame (a teardrop of ash with a violet rim) whose smoke
    curl climbs a pixel per frame. The ring is cold violet (the glyph gives no light, the picture says so)."""
    img = stone(61, 0.06)
    p = px(img)
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if 6.3 <= d <= 7.3:
                p[x, y] = mix(SEAM, VOID_VIOLET, 0.55) + (255,)
            elif d <= 6.3:
                p[x, y] = mix(p[x, y][:3], SEAM, 0.45) + (255,)
    for k in range(8):                                    # eight notches on the ring
        a = k * math.pi / 4
        x, y = int(round(cx + 7.0 * math.cos(a))), int(round(cy + 7.0 * math.sin(a)))
        p[x, y] = mix(VOID_VIOLET, (200, 190, 240), 0.5) + (255,)
    # the flame: a teardrop, hollow, ash grey with a dim violet edge
    for y in range(4, 12):
        half = {4: 0, 5: 1, 6: 1, 7: 2, 8: 2, 9: 2, 10: 2, 11: 1}[y]
        for x in range(8 - half - 1, 8 + half + 1):
            edge = x in (8 - half - 1, 8 + half) or y in (4, 11)
            p[x, y] = (mix(VOID_VIOLET, (150, 140, 190), 0.4) if edge else (34, 32, 44)) + (255,)
    # the smoke curl drifting up with the phase (one pixel per frame, wrapping)
    for i in range(3):
        sy = (3 - phase - i) % 5 + 1
        sx = 8 + (1 if (i + phase) % 2 else -1)
        p[sx, sy] = mix((60, 56, 76), (120, 110, 150), 0.5 - 0.15 * i) + (255,)
    return img


def snuffer_top():
    sheet = Image.new("RGBA", (16, 64))
    for f in range(4):
        sheet.paste(snuffer_frame(f), (0, f * 16))
    return sheet


def snuffer_side():
    img = stone(62, 0.06)
    p = px(img)
    for x in range(16):
        p[x, 0] = mix(SEAM, VOID_VIOLET, 0.4) + (255,)
        p[x, 15] = SEAM + (255,)
    for y in range(16):
        p[0, y] = mix(SEAM, VOID_VIOLET, 0.25) + (255,)
        p[15, y] = SEAM + (255,)
    return img


def shape_pixels(kind, c):
    """Pixels (relative to the centre) of the four distinguishing sigil shapes: 0 triangle, 1 circle, 2 diamond, 3 cross-in-square."""
    out = set()
    if kind == 0:
        for y in range(-3, 4):
            w = (y + 3) // 2
            out |= {(x, y) for x in range(-w, w + 1)}
    elif kind == 1:
        out = {(x, y) for x in range(-3, 4) for y in range(-3, 4) if math.hypot(x, y) <= 3.2}
    elif kind == 2:
        out = {(x, y) for x in range(-4, 5) for y in range(-4, 5) if abs(x) + abs(y) <= 3}
    else:
        out = {(x, y) for x in range(-3, 4) for y in range(-3, 4) if abs(x) == 3 or abs(y) == 3 or x == 0 or y == 0}
    return out


def sigil_top(colour_i, lit):
    """16x16 plate top: a bevelled stone slab with a ring and the colour's own shape in the middle (shape + colour, so the four read apart
    for a colour-blind player too). Lit: the shape floods bright and the ring glows."""
    col = SIGILS[colour_i]
    img = stone(70 + colour_i, 0.08)
    p = px(img)
    for i in range(16):
        p[i, 0] = mix(STONE_LIGHT, (255, 255, 255), 0.15) + (255,)
        p[0, i] = mix(STONE_LIGHT, (255, 255, 255), 0.1) + (255,)
        p[i, 15] = SEAM + (255,)
        p[15, i] = SEAM + (255,)
    dim = mix(SEAM, col, 0.55)
    bright = mix(col, (255, 255, 255), 0.35)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if 5.6 <= d <= 6.6:
                p[x, y] = (mix(col, bright, 0.5) if lit else dim) + (255,)
            elif lit and d < 5.6:
                p[x, y] = mix(p[x, y][:3], col, 0.35) + (255,)
    for dx, dy in shape_pixels(colour_i, col):
        x, y = 8 + dx, 8 + dy
        if 0 <= x < 16 and 0 <= y < 16:
            p[x, y] = (bright if lit else col) + (255,)
    return img


def sigil_side():
    img = stone(79, 0.08)
    p = px(img)
    for x in range(16):
        p[x, 0] = STONE_LIGHT + (255,)
        p[x, 15] = SEAM + (255,)
    return img


def riddle_side():
    """The stone's four sides: dark dressed stone, an eye carved above a row of four coloured studs (the four sigils, in colour order)."""
    img = stone(80, 0.06)
    p = px(img)
    for x in range(16):
        p[x, 0] = SEAM + (255,)
        p[x, 15] = SEAM + (255,)
    for i, col in enumerate(SIGILS):
        x = 2 + i * 4
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
            p[x + dx, 11 + dy] = col + (255,)
        p[x, 11] = mix(col, (255, 255, 255), 0.4) + (255,)
    # an almond eye
    for x in range(3, 13):
        t = (x - 7.5) / 4.8
        h = int(round(2.2 * (1 - t * t))) if abs(t) <= 1 else -1
        for y in range(5 - h, 5 + h + 1):
            if h >= 0:
                p[x, y] = mix(SEAM, VOID_VIOLET, 0.3) + (255,)
        if h >= 0:
            p[x, 5 - h] = mix(GOLD, SEAM, 0.3) + (255,)
            p[x, 5 + h] = mix(GOLD, SEAM, 0.3) + (255,)
    p[7, 5] = (230, 224, 255, 255)
    p[8, 5] = (230, 224, 255, 255)
    p[7, 4] = (150, 130, 220, 255)
    p[8, 6] = (150, 130, 220, 255)
    return img


def riddle_top():
    img = stone(81, 0.06)
    p = px(img)
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            if d > 6.5:
                p[x, y] = SEAM + (255,)
            elif d > 5.5:
                p[x, y] = GOLD + (255,)
    for i, col in enumerate(SIGILS):                        # four studs round a dark core, clockwise from the top
        a = i * math.pi / 2 - math.pi / 2
        x, y = int(round(7.5 + 3.0 * math.cos(a))), int(round(7.5 + 3.0 * math.sin(a)))
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
            p[min(15, x + dx), min(15, y + dy)] = col + (255,)
    return img


def puzzle_door_bottom():
    return seal.door_bottom(TEAL)


def puzzle_door_top():
    return seal.door_top(TEAL)


# ---------------------------------------------------------------------------------------------------------------- the file set
def png_bytes(img):
    b = io.BytesIO()
    img.save(b, format="PNG", optimize=True)
    return b.getvalue()


def js(obj):
    return (json.dumps(obj, indent=2) + "\n").encode()


def cube_model(parent, textures):
    return {"parent": parent, "textures": textures}


def plate_model(c, lit):
    tex = "nusmp:block/sigil_plate_%d%s" % (c, "_lit" if lit else "")
    face = lambda t: {"texture": t, "uv": [1, 1, 15, 15]}  # noqa: E731
    side = lambda t: {"texture": t}  # noqa: E731
    return {
        "parent": "minecraft:block/block",
        "textures": {"particle": "nusmp:block/sigil_plate_side", "top": tex, "side": "nusmp:block/sigil_plate_side"},
        "elements": [{"from": [1, 0, 1], "to": [15, 2, 15],
                      "faces": {"up": {"texture": "#top", "uv": [1, 1, 15, 15]}, "down": {"texture": "#side", "uv": [1, 1, 15, 15]},
                                "north": {"texture": "#side", "uv": [1, 14, 15, 16]}, "south": {"texture": "#side", "uv": [1, 14, 15, 16]},
                                "east": {"texture": "#side", "uv": [1, 14, 15, 16]}, "west": {"texture": "#side", "uv": [1, 14, 15, 16]}}}],
    }


def open_pit_model():
    """The hole: a 3-pixel rim of floor tile round the cell's top edge, open in the middle, with the tile's own texture."""
    t = "#tile"
    def el(a, b):
        return {"from": a, "to": b, "faces": {f: {"texture": t} for f in ("north", "south", "east", "west", "up", "down")}}
    return {"parent": "minecraft:block/block",
            "textures": {"particle": "nusmp:block/labyrinth_tiles", "tile": "nusmp:block/labyrinth_tiles"},
            "elements": [el([0, 13, 0], [16, 16, 2]), el([0, 13, 14], [16, 16, 16]), el([0, 13, 2], [2, 16, 14]), el([14, 13, 2], [16, 16, 14])]}


def loot_block(block_id):
    return {"type": "minecraft:block",
            "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": "nusmp:" + block_id}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}],
            "random_sequence": "nusmp:blocks/" + block_id}


def build():
    """published relative path (under src/main/resources) -> bytes, for every file this script owns outright."""
    out = {}
    a = "assets/nusmp/"
    t = a + "textures/block/"
    out[t + "snuffer_glyph_top.png"] = png_bytes(snuffer_top())
    out[t + "snuffer_glyph_top.png.mcmeta"] = js({"animation": {"frametime": 10, "interpolate": True}})
    out[t + "snuffer_glyph_side.png"] = png_bytes(snuffer_side())
    for c in range(4):
        out[t + "sigil_plate_%d.png" % c] = png_bytes(sigil_top(c, False))
        out[t + "sigil_plate_%d_lit.png" % c] = png_bytes(sigil_top(c, True))
    out[t + "sigil_plate_side.png"] = png_bytes(sigil_side())
    out[t + "riddle_stone_side.png"] = png_bytes(riddle_side())
    out[t + "riddle_stone_top.png"] = png_bytes(riddle_top())
    out[t + "gatekeeper_door_puzzle_bottom.png"] = png_bytes(puzzle_door_bottom())
    out[t + "gatekeeper_door_puzzle_bottom.png.mcmeta"] = js({"animation": {"frametime": 6}})
    out[t + "gatekeeper_door_puzzle_top.png"] = png_bytes(puzzle_door_top())
    m = a + "models/block/"
    out[m + "pitfall_tile.json"] = js(cube_model("minecraft:block/cube_all", {"all": "nusmp:block/labyrinth_tiles"}))
    out[m + "pitfall_tile_open.json"] = js(open_pit_model())
    out[m + "snuffer_glyph.json"] = js(cube_model("minecraft:block/cube_bottom_top", {"top": "nusmp:block/snuffer_glyph_top", "bottom": "nusmp:block/snuffer_glyph_side", "side": "nusmp:block/snuffer_glyph_side"}))
    for c in range(4):
        out[m + "sigil_plate_%d.json" % c] = js(plate_model(c, False))
        out[m + "sigil_plate_%d_lit.json" % c] = js(plate_model(c, True))
    out[m + "riddle_stone.json"] = js(cube_model("minecraft:block/cube_bottom_top", {"top": "nusmp:block/riddle_stone_top", "bottom": "nusmp:block/riddle_stone_top", "side": "nusmp:block/riddle_stone_side"}))
    for half, tex in (("lower", "gatekeeper_door_puzzle_bottom"), ("upper", "gatekeeper_door_puzzle_top")):
        out[m + "gatekeeper_door_puzzle_%s.json" % half] = js({"parent": "minecraft:block/cube", "textures": {
            "particle": "nusmp:block/" + tex, "down": "nusmp:block/gatekeeper_door_end", "up": "nusmp:block/gatekeeper_door_end",
            "north": "nusmp:block/" + tex, "south": "nusmp:block/" + tex, "east": "nusmp:block/" + tex, "west": "nusmp:block/" + tex}})
    s = a + "blockstates/"
    out[s + "pitfall_tile.json"] = js({"variants": {
        "safe=false,open=false": {"model": "nusmp:block/pitfall_tile"}, "safe=true,open=false": {"model": "nusmp:block/pitfall_tile"},
        "safe=false,open=true": {"model": "nusmp:block/pitfall_tile_open"}, "safe=true,open=true": {"model": "nusmp:block/pitfall_tile_open"}}})
    out[s + "snuffer_glyph.json"] = js({"variants": {"active=false": {"model": "nusmp:block/snuffer_glyph"}, "active=true": {"model": "nusmp:block/snuffer_glyph"}}})
    out[s + "sigil_plate.json"] = js({"variants": {"color=%d,lit=%s" % (c, "true" if lit else "false"): {"model": "nusmp:block/sigil_plate_%d%s" % (c, "_lit" if lit else "")}
                                                   for c in range(4) for lit in (False, True)}})
    out[s + "riddle_stone.json"] = js({"variants": {"": {"model": "nusmp:block/riddle_stone"}}})
    i = a + "models/item/"
    out[i + "pitfall_tile.json"] = js({"parent": "nusmp:block/pitfall_tile"})
    out[i + "snuffer_glyph.json"] = js({"parent": "nusmp:block/snuffer_glyph"})
    out[i + "sigil_plate.json"] = js({"parent": "nusmp:block/sigil_plate_0"})
    out[i + "riddle_stone.json"] = js({"parent": "nusmp:block/riddle_stone"})
    for b in ("pitfall_tile", "snuffer_glyph", "sigil_plate", "riddle_stone"):
        out["data/nusmp/loot_table/blocks/%s.json" % b] = js(loot_block(b))
    return out


NEW_PICKAXE = ["nusmp:pitfall_tile", "nusmp:snuffer_glyph", "nusmp:sigil_plate", "nusmp:riddle_stone"]
DOOR_STATES = {"kind=puzzle,open=false,half=lower": {"model": "nusmp:block/gatekeeper_door_puzzle_lower"},
               "kind=puzzle,open=true,half=lower": {"model": "nusmp:block/gatekeeper_door_open"},
               "kind=puzzle,open=false,half=upper": {"model": "nusmp:block/gatekeeper_door_puzzle_upper"},
               "kind=puzzle,open=true,half=upper": {"model": "nusmp:block/gatekeeper_door_open"}}


def shared_files():
    """The two files other builders own, with this script's additions merged in (only appended, nothing removed): path -> bytes."""
    out = {}
    p = os.path.join(RES, "data", "minecraft", "tags", "block", "mineable", "pickaxe.json")
    tag = json.load(open(p))
    for v in NEW_PICKAXE:
        if v not in tag["values"]:
            tag["values"].append(v)
    out["data/minecraft/tags/block/mineable/pickaxe.json"] = js(tag)
    p = os.path.join(RES, "assets", "nusmp", "blockstates", "gatekeeper_door.json")
    st = json.load(open(p))
    for k, v in DOOR_STATES.items():
        st["variants"].setdefault(k, v)
    out["assets/nusmp/blockstates/gatekeeper_door.json"] = js(st)
    return out


def write():
    n = 0
    for rel, data in {**build(), **shared_files()}.items():
        path = os.path.join(RES, *rel.split("/"))
        os.makedirs(os.path.dirname(path), exist_ok=True)
        if os.path.exists(path) and open(path, "rb").read() == data:
            continue
        with open(path, "wb") as f:
            f.write(data)
        n += 1
    print("%d file(s) written" % n)
    return 0


def check():
    bad = 0
    for rel, data in {**build(), **shared_files()}.items():
        path = os.path.join(RES, *rel.split("/"))
        if not os.path.exists(path):
            print("missing " + rel)
            bad += 1
        elif open(path, "rb").read() != data:
            print("stale   " + rel)
            bad += 1
    for rel, data in build().items():
        if rel.endswith(".png"):
            im = Image.open(io.BytesIO(data))
            want = (16, 64) if rel.endswith(("snuffer_glyph_top.png", "gatekeeper_door_puzzle_bottom.png")) else (16, 16)
            if im.size != want or im.mode != "RGBA":
                print("bad size/mode %s %s %s" % (rel, im.size, im.mode))
                bad += 1
    print("check:", "ok" if not bad else "%d problem(s)" % bad)
    return 1 if bad else 0


def sheet(path):
    names = [r for r in build() if r.endswith(".png")]
    cell = 16 * 8
    cols = 6
    rows = (len(names) + cols - 1) // cols
    img = Image.new("RGBA", (cols * (cell + 8), rows * (cell + 8)), (40, 40, 48, 255))
    for k, rel in enumerate(names):
        im = Image.open(io.BytesIO(build()[rel])).convert("RGBA").crop((0, 0, 16, 16)).resize((cell, cell), Image.NEAREST)
        img.paste(im, ((k % cols) * (cell + 8) + 4, (k // cols) * (cell + 8) + 4), im)
    img.save(path)
    print(path, len(names), "textures")
    return 0


if __name__ == "__main__":
    ap = argparse.ArgumentParser(description=__doc__.strip().splitlines()[0])
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--sheet", metavar="PNG")
    a = ap.parse_args()
    sys.exit(check() if a.check else sheet(a.sheet) if a.sheet else write())
