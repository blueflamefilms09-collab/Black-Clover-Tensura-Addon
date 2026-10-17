#!/usr/bin/env python3
"""
Previews for the Mega-Dungeon, without the game (needs Pillow).

  1. A template as an isometric voxel picture (what you would see from above and to the south-east), optionally sliced:

        python3 tools/labyrinth/mega_preview.py mega/hall/crypt out.png                 # whole piece
        python3 tools/labyrinth/mega_preview.py mega/hall/crypt out.png --y 8:20        # rows 8..20 only (a cutaway)
        python3 tools/labyrinth/mega_preview.py mega/hall/crypt out.png --x 0:30 --z 0:30 --scale 7
        python3 tools/labyrinth/mega_preview.py mega/hall/crypt out.png --top           # top-down plan, highest block per column

  2. A sector plan as a map in the style of the reference tabletop dungeon (rooms as coloured rectangles, doors as gaps):

        python3 tools/labyrinth/mega_preview.py --plan plan.json map.png

Why: nothing here can run in game, and a room that is "correct" by the checker can still be ugly. The colours are a keyword table (stone grey, deepslate
dark grey, nether brick red, wood brown, glass pale blue, lava orange ...), good enough to judge shape, symmetry, light and clutter, not texture.
"""
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from PIL import Image, ImageDraw  # noqa: E402
import mega_lib as L  # noqa: E402
from template import load  # noqa: E402

COLORS = [  # (regex on the block path, (r, g, b, a))
    (r".*lava.*|magma_block|shroomlight|glowstone|.*campfire|fire|.*torch|.*lantern|sea_lantern|redstone_lamp|light", (250, 160, 40, 255)),
    (r".*water.*|.*prismarine.*|conduit", (60, 110, 200, 200)),
    (r".*stained_glass.*|glass|tinted_glass|.*pane", (150, 200, 220, 120)),
    (r".*nether_brick.*|.*nether_wart.*|crimson.*|.*red_.*|netherrack|.*bed", (120, 40, 40, 255)),
    (r".*blackstone.*|.*obsidian|.*basalt.*", (45, 38, 55, 255)),
    (r".*deepslate.*|.*tuff.*", (70, 72, 78, 255)),
    (r".*copper.*", (60, 150, 130, 255)),
    (r".*moss.*|.*leaves|.*vine.*|.*azalea.*|.*grass.*|.*fern.*", (60, 120, 50, 255)),
    (r".*end_stone.*|.*purpur.*|.*amethyst.*", (150, 120, 190, 255)),
    (r".*quartz.*|.*calcite.*|.*snow.*|.*ice.*|.*diorite.*|.*birch.*|.*white.*", (225, 225, 220, 255)),
    (r".*gold.*|.*honey.*|.*sand.*|.*hay.*", (210, 180, 70, 255)),
    (r".*iron.*|.*chain.*|.*anvil|.*cauldron|.*bars|.*grindstone|.*furnace|.*smoker", (150, 150, 155, 255)),
    (r".*dark_oak.*|.*spruce.*|.*oak.*|.*planks.*|.*log.*|.*wood.*|.*barrel|.*chest|.*bookshelf|.*table|.*fence.*|.*trapdoor|.*door|.*ladder|.*lectern|.*loom", (110, 80, 50, 255)),
    (r".*skull.*|.*head|.*bone.*|.*candle.*|.*cobweb", (230, 225, 200, 255)),
    (r"structure_block|jigsaw", (255, 0, 255, 255)),
]
KIND_COLORS = {"LANDING": (90, 160, 120), "SAFE": (90, 170, 130), "CORRIDOR": (176, 150, 120), "JUNCTION": (190, 160, 125), "HALL": (200, 170, 130),
               "SET_PIECE": (210, 120, 90), "CAVERN": (95, 140, 80), "TRIAL": (190, 100, 100), "TRAP": (200, 120, 60), "PUZZLE": (110, 140, 200),
               "TREASURE": (220, 190, 70), "ARENA": (180, 60, 60), "ANTECHAMBER": (150, 90, 160), "VAULT": (230, 200, 80), "ONWARD": (120, 120, 200),
               "LINK": (150, 150, 150), "CAP": (110, 110, 110), "OTHER": (170, 170, 170),
               "STAIRWELL": (240, 215, 120)}                                  # 1.58.0: the two stairwells of a floor stand out on its map


def color_of(block):
    path = block.split(":", 1)[1] if ":" in block else block
    for pat, c in COLORS:
        if re.fullmatch(pat, path):
            return c
    return (140, 130, 120, 255)


def shade(c, f):
    return (min(255, int(c[0] * f)), min(255, int(c[1] * f)), min(255, int(c[2] * f)), c[3])


def parse_range(s, lo, hi):
    if not s:
        return lo, hi
    a, b = s.split(":")
    return int(a or lo), int(b or hi)


def render_iso(t, out, xr, yr, zr, scale, top):
    cells = {p: c for p, c in t.cells.items() if xr[0] <= p[0] <= xr[1] and yr[0] <= p[1] <= yr[1] and zr[0] <= p[2] <= zr[1]
             and c.block not in ("minecraft:air",)}
    if top:
        best = {}
        for (x, y, z), c in cells.items():
            if (x, z) not in best or y > best[(x, z)][0]:
                best[(x, z)] = (y, c)
        img = Image.new("RGBA", ((xr[1] - xr[0] + 1) * scale, (zr[1] - zr[0] + 1) * scale), (18, 18, 24, 255))
        d = ImageDraw.Draw(img)
        for (x, z), (y, c) in best.items():
            col = shade(color_of(c.block), 0.6 + 0.4 * (y - L.FLOOR_ROW) / 30.0 if y > L.FLOOR_ROW else 0.55)
            d.rectangle([(x - xr[0]) * scale, (z - zr[0]) * scale, (x - xr[0] + 1) * scale - 1, (z - zr[0] + 1) * scale - 1], fill=col)
        img.save(out)
        return
    tw, th, vh = 2 * scale, scale, scale
    xs = [(x - z) * tw // 2 for (x, y, z) in cells] or [0]
    ys = [(x + z) * th // 2 - y * vh for (x, y, z) in cells] or [0]
    ox, oy = -min(xs) + tw, -min(ys) + vh * 2
    img = Image.new("RGBA", (max(xs) - min(xs) + tw * 2 + 2, max(ys) - min(ys) + vh * 3 + th + 4), (18, 18, 24, 255))
    d = ImageDraw.Draw(img, "RGBA")
    for (x, y, z) in sorted(cells, key=lambda p: (p[0] + p[1] + p[2], p[1])):
        if all((x + dx, y + dy, z + dz) in cells for dx, dy, dz in ((1, 0, 0), (0, 0, 1), (0, 1, 0))):
            continue
        c = color_of(cells[(x, y, z)].block)
        sx = (x - z) * tw // 2 + ox
        sy = (x + z) * th // 2 - y * vh + oy
        top_pts = [(sx, sy - th // 2), (sx + tw // 2, sy), (sx, sy + th // 2), (sx - tw // 2, sy)]
        left_pts = [(sx - tw // 2, sy), (sx, sy + th // 2), (sx, sy + th // 2 + vh), (sx - tw // 2, sy + vh)]
        right_pts = [(sx + tw // 2, sy), (sx, sy + th // 2), (sx, sy + th // 2 + vh), (sx + tw // 2, sy + vh)]
        d.polygon(left_pts, fill=shade(c, 0.72))
        d.polygon(right_pts, fill=shade(c, 0.55))
        d.polygon(top_pts, fill=shade(c, 1.0))
    img.save(out)


def render_plan(plan, out, scale=4):
    x1, z1, x2, z2 = plan["region"]
    w, h = (x2 - x1 + 1) * scale, (z2 - z1 + 1) * scale
    img = Image.new("RGB", (w, h), (44, 46, 56))
    d = ImageDraw.Draw(img)
    for r in plan["rooms"]:
        bx1, bz1, bx2, bz2 = r["box"]
        col = KIND_COLORS.get(r.get("kind", "OTHER"), (170, 170, 170))
        d.rectangle([(bx1 - x1) * scale, (bz1 - z1) * scale, (bx2 - x1 + 1) * scale - 1, (bz2 - z1 + 1) * scale - 1], fill=col, outline=(30, 30, 30))
    for dx, dz, f in plan.get("doors", []):
        px, pz = (dx - x1) * scale, (dz - z1) * scale
        d.rectangle([px, pz, px + 3 * scale - 1 if f in ("north", "south") else px + scale, pz + scale if f in ("north", "south") else pz + 3 * scale - 1], fill=(235, 220, 160))
    for e in plan.get("loops", []):
        px, pz = (e[0] - x1) * scale, (e[1] - z1) * scale
        d.ellipse([px - 3, pz - 3, px + 3, pz + 3], outline=(255, 255, 255))
    img.save(out)


def main(argv):
    args = list(argv)
    opts = {}
    for key in ("--x", "--y", "--z", "--scale", "--plan"):
        if key in args:
            i = args.index(key)
            opts[key] = args[i + 1]
            del args[i:i + 2]
    top = "--top" in args
    if top:
        args.remove("--top")
    if "--plan" in opts:
        render_plan(json.load(open(opts["--plan"])), args[0])
        print("wrote", args[0])
        return 0
    name, out = args[0], args[1]
    path = os.path.join(L.RES, "data", "nusmp", "structure", name + ".nbt")
    t = load(path, name)
    scale = int(opts.get("--scale", 5))
    render_iso(t, out, parse_range(opts.get("--x"), 0, t.size[0] - 1), parse_range(opts.get("--y"), 0, t.size[1] - 1), parse_range(opts.get("--z"), 0, t.size[2] - 1), scale, top)
    print("wrote", out)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
