#!/usr/bin/env python3
"""3.32.0: look at what the mesh detail limit does. Draws each mesh twice (full source mesh | cut mesh) from three sides.

    python tools/mesh_cap/preview_cap.py <cut dir> [ids...] [--out file.png]

<cut dir> holds the cut meshes as plain text, written by the packer with -Dnusmp.meshCapDump=<dir> (gradle packMeshes -PmeshCapDump=<dir>);
ids are paths relative to assets/nusmp without .txt, e.g. weapon_meshes_vu/blackrazor. No ids: every mesh found in the cut dir (max 6).
Quads are flat shaded; body = grey, core glow = orange, veins = cyan, void = dark violet. Halo cards (layer 4) are left out.
"""
import math
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2] / "src/main/resources/assets/nusmp"
COLORS = {0: (150, 160, 175), 1: (90, 50, 140), 2: (255, 150, 60), 3: (80, 220, 230)}
CELL = 260


def read_quads(path):
    quads = []
    for line in path.read_text(encoding="utf-8").split("\n"):
        t = line.split()
        if not t or t[0] not in ("qn", "q"):
            continue
        qn = t[0] == "qn"
        if len(t) != (34 if qn else 25):
            continue
        try:
            layer = int(t[1])
            stride = 8 if qn else 5
            p = [[float(t[2 + k * stride + a]) for a in range(3)] for k in range(4)]
        except ValueError:
            continue
        quads.append((layer, p))
    return quads


def render(quads, view, box, size):
    """view 0 = front (x,y), 1 = side (z,y), 2 = turned 45 degrees about y. box = (centre xyz, scale) shared by both pictures."""
    centre, scale = box
    img = Image.new("RGB", (size, size), (22, 24, 30))
    d = ImageDraw.Draw(img)
    ang = [0.0, math.pi / 2, math.pi / 4][view]
    ca, sa = math.cos(ang), math.sin(ang)
    items = []
    for layer, p in quads:
        if layer == 4:
            continue
        pts = []
        for x, y, z in p:
            x, y, z = x - centre[0], y - centre[1], z - centre[2]
            rx, rz = x * ca + z * sa, -x * sa + z * ca             # turn about y
            pts.append((rx, y, rz))
        a = np.array(pts)
        n = np.cross(a[1] - a[0], a[2] - a[0])
        ln = np.linalg.norm(n)
        shade = 0.55 + 0.45 * abs(n[2] / ln) if ln > 1e-9 else 0.5   # facing the viewer = brighter
        items.append((a[:, 2].mean(), layer, a, shade))
    items.sort(key=lambda i: i[0])                                    # far first
    for _, layer, a, shade in items:
        c = COLORS.get(layer, (200, 200, 200))
        col = tuple(int(v * shade) for v in c)
        xy = [(size / 2 + v[0] * scale, size / 2 - v[1] * scale) for v in a]
        d.polygon(xy, fill=col, outline=tuple(int(v * 0.6) for v in col))
    return img


def box_of(quads):
    pts = np.array([v for layer, p in quads if layer != 4 for v in p])
    lo, hi = pts.min(axis=0), pts.max(axis=0)
    centre = (lo + hi) / 2
    ext = max(hi[1] - lo[1], math.hypot(hi[0] - lo[0], hi[2] - lo[2]), 1e-6)
    return centre, (CELL - 24) / ext


def main(argv):
    out = "mesh_cap.png"
    args = []
    i = 0
    while i < len(argv):
        if argv[i] == "--out":
            out = argv[i + 1]
            i += 2
        else:
            args.append(argv[i])
            i += 1
    if not args:
        sys.exit(__doc__)
    cut_dir = Path(args[0])
    ids = args[1:] or sorted(str(p.relative_to(cut_dir))[:-4] for p in cut_dir.rglob("*.txt"))[:6]
    rows = []
    for mid in ids:
        full = read_quads(ROOT / (mid + ".txt"))
        cut = read_quads(cut_dir / (mid + ".txt"))
        if not full or not cut:
            print("skipped (missing):", mid)
            continue
        box = box_of(full)
        row = Image.new("RGB", (CELL * 6 + 40, CELL + 22), (10, 10, 14))
        dr = ImageDraw.Draw(row)
        dr.text((6, 4), f"{mid}   FULL {len(full)} quads  |  CUT {len(cut)} quads ({100 * len(cut) // len(full)}%)", fill=(240, 240, 240))
        for v in range(3):
            row.paste(render(full, v, box, CELL), (v * CELL, 22))
            row.paste(render(cut, v, box, CELL), (3 * CELL + 40 + v * CELL, 22))
        rows.append(row)
    if not rows:
        sys.exit("nothing to draw")
    sheet = Image.new("RGB", (rows[0].width, sum(r.height for r in rows)))
    y = 0
    for r in rows:
        sheet.paste(r, (0, y))
        y += r.height
    sheet.save(out, optimize=True)
    print("wrote", out, sheet.size)


if __name__ == "__main__":
    main(sys.argv[1:])
