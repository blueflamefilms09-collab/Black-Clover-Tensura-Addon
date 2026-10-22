#!/usr/bin/env python3
"""
Rooms rebuilt, previews (needs Pillow): draws a template, a rebuilt variant or a block map without the game, with the shape-aware renderer of
mega_shape_preview.py (stairs, slabs, chains, lanterns ... as the boxes they occupy).

    python3 tools/labyrinth/rr_view.py mega_r/hall/library_a_v1 out.png              # automatic cutaway (near walls and roof removed)
    python3 tools/labyrinth/rr_view.py mega/hall/library_a out.png --rot 1           # the shipped room, seen from another corner
    python3 tools/labyrinth/rr_view.py mega_r/hall/library_a_v1 out.png --top        # plan view, highest block per column
    python3 tools/labyrinth/rr_view.py mega_r/hall/library_a_v1 out.png --section 13 # a cross-section at z = 13 (looking from the south)

The cutaway keeps the rows up to the roof's lowest row (the roof is shown with --roof). Nothing here is a game screenshot: colours are a keyword table.
"""
import collections
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
import mega_preview as P  # noqa: E402
import mega_shape_preview as SP  # noqa: E402
from template import load  # noqa: E402


def open_template(name):
    if name.endswith(".nbt"):
        return load(name, "preview/" + os.path.basename(name)[:-4].lower().replace(" ", "_"))
    return load(os.path.join(L.RES, "data", "nusmp", "structure", name + ".nbt"), name)


def roof_rows(t):
    """(lowest roof row, highest row with any block over the interior): used to cut the roof away."""
    W, H, D = t.size
    cnt = collections.Counter(y for (x, y, z) in t.cells)
    ceil = None
    for y in range(9, H):
        if cnt[y] >= W * D * 0.5:
            ceil = y
            break
    return ceil if ceil is not None else min(H - 1, 30)


def cutaway(t, out, scale=8, rot=0, roof=False, ytop=None, x=None, z=None):
    W, H, D = t.size
    cnt = collections.Counter(y for (xx, y, zz) in t.cells)
    top = max(y for (xx, y, zz) in t.cells)
    ceil = roof_rows(t)
    yt = (top if roof else ceil - 1) if ytop is None else ytop
    # near walls depend on the rotation: the viewer stands at the south-east for rot 0
    if rot == 0:
        xr, zr = (0, W - 3), (0, D - 3)
    elif rot == 1:
        xr, zr = (2, W - 1), (0, D - 3)
    elif rot == 2:
        xr, zr = (2, W - 1), (2, D - 1)
    else:
        xr, zr = (0, W - 3), (2, D - 1)
    if x is not None:
        xr = x
    if z is not None:
        zr = z
    SP.render(t, out, xr, (8, yt), zr, scale, top=False, light=False, markers=True, lit=False, rot=rot)


def plan(t, out, scale=14):
    W, H, D = t.size
    SP.render(t, out, (0, W - 1), (0, H - 1), (0, D - 1), scale, top=True, light=False, markers=True, lit=False, rot=0)


def section(t, out, zrow, scale=14):
    W, H, D = t.size
    top = max(y for (xx, y, zz) in t.cells)
    for pos in [p for p, c in t.cells.items() if c.block.endswith("_carpet")]:         # the elevation renderer cannot draw the thin box of a carpet
        del t.cells[pos]
    last = None
    for z in (zrow, zrow + 1, zrow - 1, zrow + 2, zrow - 2):           # the shape renderer chokes on a few block shapes: try the next row
        try:
            SP.render_elevation(t, out, (0, W - 1), (8, top), (z, z), scale, "z", lit=False)
            return
        except ValueError as e:
            last = e
    raise last


_SCENES = {}


def firstperson(t, out, cam, look, fov=85.0, size=(640, 360), res=3, key=None):
    """A first-person picture (ray-marched, block light shaded) from `cam` (x, y, z) looking at `look`; see mega_walk_preview.py. Heavy: use size 640x360."""
    import mega_walk_preview as WP
    k = key or id(t)
    scene = _SCENES.get(k)
    if scene is None:
        scene = WP.Scene(t, res=res, light=True)
        _SCENES.clear()
        _SCENES[k] = scene
    img = WP.render(scene, cam, look, fov, size, gamma=1.5)
    img.save(out)
    return out


def main(argv):
    args = list(argv)
    opts = {}
    for key in ("--scale", "--rot", "--section", "--ytop", "--cam", "--look", "--fov", "--size"):
        if key in args:
            i = args.index(key)
            opts[key] = args[i + 1]
            del args[i:i + 2]
    flags = {f for f in ("--top", "--roof") if f in args}
    args = [a for a in args if a not in flags]
    t = open_template(args[0])
    out = args[1]
    scale = int(opts.get("--scale", 8))
    if "--cam" in opts:
        cam = tuple(float(v) for v in opts["--cam"].split(","))
        look = tuple(float(v) for v in opts["--look"].split(","))
        w, h = (int(v) for v in opts.get("--size", "640x360").split("x"))
        firstperson(t, out, cam, look, float(opts.get("--fov", 85)), (w, h))
    elif "--top" in flags:
        plan(t, out, scale)
    elif "--section" in opts:
        section(t, out, int(opts["--section"]), scale)
    else:
        cutaway(t, out, scale, int(opts.get("--rot", 0)), roof="--roof" in flags, ytop=int(opts["--ytop"]) if "--ytop" in opts else None)
    print("wrote", out)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
