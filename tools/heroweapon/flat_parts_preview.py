#!/usr/bin/env python3
"""flat_parts_preview: close-up software renders of one named part of a weapon mesh, to compare before / after a flat part was made solid.

Uses the rasteriser of tools/item_preview/preview_weapons.py (the way WeaponRenderer draws a mesh: skin, item lighting, glow layer additive) with a camera
aimed at the part. A pull-stage mesh without a skin of its own uses the skin of its rest mesh, as the game does.

    python3 -B tools/heroweapon/flat_parts_preview.py <mesh id> <part> <out.png> [--mesh-dir <dir>] [--dist 0.5]

The picture is five views of the part (front, three-quarter, side, top-ish, back), 220 px each.
"""
import argparse
import os
import re
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "item_preview"))
import preview_weapons as pw  # noqa: E402


def part_bounds(path, part):
    """The quad numbers of a part, or of several parts separated by commas (the parts need not be next to each other)."""
    ids = []
    with open(path, encoding="utf-8") as f:
        for line in f:
            v = line.split()
            if v[:2] == ["#", "part"] and v[2] in part.split(","):
                ids += range(int(v[3]), int(v[3]) + int(v[4]))
    return ids


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("mesh_id")
    ap.add_argument("part")
    ap.add_argument("out")
    ap.add_argument("--mesh-dir", default=os.path.join(pw.ASSETS, "weapon_meshes"))
    ap.add_argument("--dist", type=float, default=0.45)
    ap.add_argument("--no-glow", action="store_true", help="skin only: the additive glow layer of a white part saturates the shading")
    a = ap.parse_args(argv)
    path = os.path.join(a.mesh_dir, a.mesh_id + ".txt")
    ids = part_bounds(path, a.part)
    if not ids:
        sys.exit(f"no part {a.part} in {path}")
    quads, glow = load(path)
    sel = [quads[i] for i in ids]
    base = re.sub(r"_(pull[123]|charged|spent)$", "", a.mesh_id)
    d = os.path.join(pw.ASSETS, "textures", "entity", "weapon")
    skin = next((pw.tex(os.path.join(d, n + ".png")) for n in (a.mesh_id, base) if pw.tex(os.path.join(d, n + ".png")) is not None), None)
    glowtex = next((pw.tex(os.path.join(d, n + "_glow.png")) for n in (a.mesh_id, base) if pw.tex(os.path.join(d, n + "_glow.png")) is not None), None)
    if a.no_glow:
        glowtex = None
    k = skin.shape[1] / 128.0
    sel = [(ly, np.concatenate([p[:, :3], p[:, 3:5] * k], axis=1), n) for (ly, p, n) in sel]
    allq = [(ly, np.concatenate([p[:, :3], p[:, 3:5] * k], axis=1), n) for (ly, p, n) in quads]
    allp = np.vstack([p[:, :3] for (ly, p, n) in sel if ly == 0] or [p[:, :3] for (ly, p, n) in sel])
    centre = (allp.min(0) + allp.max(0)) / 2
    tiles = []
    for yaw, pitch in ((0, 0), (40, 20), (90, 0), (0, 70), (180, 0)):
        cam = pw.view(yaw, pitch, dist=a.dist, target=centre)
        tiles.append(pw.render(sel, skin, glowtex, glow, cam))
    Image.fromarray((np.concatenate(tiles, 1) * 255).astype(np.uint8)).save(a.out)
    print("wrote", a.out, "quads", len(sel))


def load(path):
    """The loader of preview_weapons, reading a mesh file anywhere (the shipped one or a candidate in a scratch folder)."""
    name = os.path.basename(path)
    real = pw.os.path.join

    def patched(*parts):
        if len(parts) == 3 and parts[1] == "weapon_meshes" and parts[2] == name:
            return path
        return real(*parts)
    pw.os.path.join = patched
    try:
        quads, glow = pw.load(name[:-4])
    finally:
        pw.os.path.join = real
    return quads, glow


if __name__ == "__main__":
    main()
