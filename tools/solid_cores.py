"""Solid cores: repairs the 24 shipped rank Mana Cores WITHOUT the owner's GLBs (docs/solid_cores.md), and checks them.

Cause: tools/gen_mana_core_models.py wrote every sphere quad clockwise seen from outside. The body is drawn with RenderType.entityCutout (back-face culling ON), so the near half of every orb was
culled and the far half's inside showed through: a hollow, see-through orb; the additive glow pass (culling on too) had the same flip. The 16x16 icons also had a soft (partly clear) rim.

    python3 -B tools/solid_cores.py            # repair (idempotent: a quad that already faces out is left alone)
    python3 -B tools/solid_cores.py --check    # report only, exit 1 if anything is still hollow / soft

What it does to src/main/resources/assets/nusmp: weapon_meshes/core_*.txt: quads that face inward get their vertex order reversed, the glow layer (2) is moved out to a 1.015 shell once
(marker line in the header); textures/item/core_*.png: re-shaded from the skin as a solid icon. Skins are untouched (they are already opaque RGB).
"""
import glob
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_mana_core_models as g  # noqa: E402

MARK = "# solid-cores: outward winding, glow shell x%.3f" % g.SHELL


def facing_out(p):
    """True when the quad's counter-clockwise normal (the way the game culls) agrees with its stated outward normal."""
    v = [list(map(float, p[2 + 8 * k:2 + 8 * k + 8])) for k in range(4)]
    P = np.array([x[:3] for x in v])
    n = np.mean([x[5:8] for x in v], axis=0)
    geo = np.cross(P[1] - P[0], P[2] - P[0]) + np.cross(P[2] - P[0], P[3] - P[0])
    return float(np.dot(geo, n)) > 0


def repair_mesh(path, check):
    lines = open(path).read().splitlines()
    marked = any(l.startswith("# solid-cores") for l in lines)
    out, flipped, hollow = [], 0, 0
    for l in lines:
        p = l.split()
        if p and p[0] == "qn":
            if not facing_out(p):
                hollow += 1
                parts = [p[2 + 8 * k:2 + 8 * k + 8] for k in range(4)]
                p = p[:2] + sum([parts[k] for k in (0, 3, 2, 1)], [])
                flipped += 1
            if p[1] == "2" and not marked:
                for k in range(4):
                    for c in range(3):
                        p[2 + 8 * k + c] = "%.3f" % (float(p[2 + 8 * k + c]) * g.SHELL)
            l = " ".join(p)
        out.append(l)
    if not marked:
        out.insert(1, MARK)
    if not check:
        open(path, "w").write("\n".join(out) + "\n")
    return hollow


def soft_pixels(arr):
    return int(((arr[..., 3] > 0) & (arr[..., 3] < 255)).sum())


def main():
    check = "--check" in sys.argv
    assets = g.ASSETS
    bad = 0
    for path in sorted(glob.glob(os.path.join(assets, "weapon_meshes", "core_*.txt"))):
        h = repair_mesh(path, check)
        bad += h
        if h:
            print("hollow quads" if check else "flipped", h, os.path.basename(path))
    for path in sorted(glob.glob(os.path.join(assets, "textures", "entity", "weapon", "core_*.png"))):
        if path.endswith("_glow.png"):
            continue
        cid = os.path.basename(path)[:-4]
        skin = np.asarray(Image.open(path).convert("RGB"), np.float64) / 255.0
        ip = os.path.join(assets, "textures", "item", cid + ".png")
        if check:
            s = soft_pixels(np.asarray(Image.open(ip).convert("RGBA")))
            bad += s
            if s:
                print("soft icon pixels", s, cid)
        else:
            g.icon(skin).save(ip, optimize=True)
    print("done;", "problems left:" if check else "repaired; problems seen:", bad)
    return 1 if check and bad else 0


if __name__ == "__main__":
    sys.exit(main())
