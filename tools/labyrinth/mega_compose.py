#!/usr/bin/env python3
"""
Stitches the REAL template files of a sector plan into one top-down picture (a floor plan like the reference tabletop dungeon map), without the game.

    python3 tools/labyrinth/mega_compose.py plan.json map.png                 # floor plan, 4 px per block
    python3 tools/labyrinth/mega_compose.py plan.json map.png --scale 6 --rows 9:14
    python3 tools/labyrinth/mega_compose.py plan.json map.png --root <resources root>   # templates of another worktree

`plan.json` is what the Java `MegaSim` / `/multiverse mega plan` writes: `{"region": [x1, z1, x2, z2], "pieces": [{"template": "nusmp:mega/hall/crypt_a",
"rotation": 0..3, "x": ox, "y": oy, "z": oz}, ...], "entry": [x, y, z, "east"]}`; a piece's world cell for local cell c is `origin + rotate(rotation, c)` with
the vanilla CLOCKWISE_90 transform (x, z) -> (-z, x), applied about the template origin (so a rotated piece reaches into negative local coordinates, exactly as
`StructureTemplate.transform` does with a zero pivot).

Why: the layout is the point of this feature ("tightly packed, interlinked, like the tabletop map") and nothing can be walked here. This draws every placed block
of the floor-level rows (default rows 9..12: the floor-level furniture, walls and doorways, not the ceilings) of every piece, so doorways that line up, loops, niches
and caps, dead ends and the density of the whole sector can be judged by eye, and compared with the reference map.
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from PIL import Image, ImageDraw  # noqa: E402
import mega_lib as L  # noqa: E402
from mega_preview import color_of  # noqa: E402
from template import load  # noqa: E402

BG = (44, 46, 56)


def rotate(rot, x, z):
    """The vanilla transform of Rotation (0 NONE, 1 CLOCKWISE_90, 2 CLOCKWISE_180, 3 COUNTERCLOCKWISE_90) about the origin."""
    r = rot % 4
    if r == 0:
        return x, z
    if r == 1:
        return -z, x
    if r == 2:
        return -x, -z
    return z, -x


def load_template(root, name, cache):
    key = name
    if key not in cache:
        rel = name.split(":", 1)[1] if ":" in name else name
        path = os.path.join(root, "data", "nusmp", "structure", *rel.split("/")) + ".nbt"
        cache[key] = load(path, rel) if os.path.exists(path) else None
    return cache[key]


def floor_color(block):
    n = block.split(":", 1)[1]
    if "tile" in n or "polished" in n or n == "deepslate":
        return (176, 150, 120)       # floors read tan like the tabletop map
    return color_of(block)


def compose(plan, root, scale, rows):
    cache = {}
    x1, z1, x2, z2 = plan["region"]
    m = 14
    ox, oz = x1 - m, z1 - m
    w, h = (x2 - x1 + 1 + 2 * m) * scale, (z2 - z1 + 1 + 2 * m) * scale
    top = {}
    missing = set()
    for p in plan["pieces"]:
        t = load_template(root, p["template"], cache)
        if t is None:
            missing.add(p["template"])
            continue
        for (cx, cy, cz), c in t.cells.items():
            if not (rows[0] <= cy <= rows[1]) or c.block in ("minecraft:air", "minecraft:structure_block"):
                continue
            rx, rz = rotate(p["rotation"], cx, cz)
            wx, wz = p["x"] + rx, p["z"] + rz
            prev = top.get((wx, wz))
            if prev is None or cy > prev[0]:
                top[(wx, wz)] = (cy, c.block)
    img = Image.new("RGB", (w, h), BG)
    d = ImageDraw.Draw(img)
    for (wx, wz), (cy, b) in top.items():
        if cy == L.FLOOR_ROW:
            col = floor_color(b)
        elif cy <= L.FLOOR_ROW + 1:
            col = color_of(b)[:3]
        else:
            col = tuple(int(v * 0.55) for v in color_of(b)[:3])    # walls and tall things darker, so rooms read as lighter areas inside dark outlines
        px, pz = (wx - ox) * scale, (wz - oz) * scale
        d.rectangle([px, pz, px + scale - 1, pz + scale - 1], fill=col)
    for dx, dz, face in plan.get("doors", []):
        pass
    if "entry" in plan:
        ex, ey, ez = plan["entry"][:3]
        px, pz = (ex - ox) * scale, (ez - oz) * scale
        d.ellipse([px - 6, pz - 6, px + 6, pz + 6], outline=(255, 255, 255), width=2)
    return img, missing


def main(argv):
    args = list(argv)
    opts = {}
    for key in ("--scale", "--rows", "--root"):
        if key in args:
            i = args.index(key)
            opts[key] = args[i + 1]
            del args[i:i + 2]
    scale = int(opts.get("--scale", 4))
    lo, hi = (int(v) for v in opts.get("--rows", "%d:%d" % (L.FLOOR_ROW, L.FLOOR_ROW + 4)).split(":"))
    root = opts.get("--root", L.RES)
    plan = json.load(open(args[0]))
    img, missing = compose(plan, root, scale, (lo, hi))
    img.save(args[1])
    print("wrote %s (%dx%d, %d pieces%s)" % (args[1], img.width, img.height, len(plan["pieces"]),
                                            ("; missing templates: " + ", ".join(sorted(missing))) if missing else ""))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
