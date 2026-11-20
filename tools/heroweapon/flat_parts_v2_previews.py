#!/usr/bin/env python3
"""flat_parts_v2_previews: before / after stills of the solid parts of every weapon the Blender rebuild changed (4.55.0, docs/heroweapon/flat_parts_blender.md).

before_<id>.png = the old mesh of weapon_meshes/flat_parts_old with the skin it was laid out for (textures/entity/weapon/flat_parts_old); after_<id>.png = the shipped mesh and skin.
Each picture is five close views (front, three-quarter, side, top, back) of the arrow (the draw stage pull3 of a bow) or of the spear / harpoon head, drawn by the repo's software
rasteriser (tools/item_preview/preview_weapons.py, the way WeaponRenderer draws a mesh; low resolution, no Blender render). The glow layer is left off so the shape reads.

    python3 -B tools/heroweapon/flat_parts_v2_previews.py [OUT_DIR]      # default docs/previews/flat_parts_v2
"""
import glob
import os
import re
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse")
OLD_MESHES = os.path.join(ASSETS, "weapon_meshes", "flat_parts_old")
OLD_SKINS = os.path.join(ASSETS, "textures", "entity", "weapon", "flat_parts_old")
ARROW = "arrow_shaft,arrow_stem,arrow_head,arrow_fletch"
HEAD = "spearhead,head_blade,head_prongs,head_barbs"


def targets():
    """-> [(mesh id, parts, distance)]: pull3 of every bow, the head of every spear and the harpoon, that has an old mesh and an old skin."""
    out = []
    for p in sorted(glob.glob(os.path.join(OLD_MESHES, "*.txt"))):
        mid = os.path.basename(p)[:-4]
        if re.search(r"_pull[12]$", mid):
            continue
        base = re.sub(r"_pull3$", "", mid)
        if not os.path.exists(os.path.join(OLD_SKINS, base + ".png")):
            continue
        out.append((mid, ARROW if mid.endswith("_pull3") else HEAD, "0.5" if mid.endswith("_pull3") else "0.8"))
    return out


def main(argv):
    out = argv[0] if argv else os.path.join(ROOT, "docs", "previews", "flat_parts_v2")
    os.makedirs(out, exist_ok=True)
    tool = os.path.join(HERE, "flat_parts_preview.py")
    n = 0
    for mid, parts, dist in targets():
        for tag, extra in (("before", ["--mesh-dir", OLD_MESHES, "--skin-dir", OLD_SKINS]), ("after", [])):
            png = os.path.join(out, "%s_%s.png" % (tag, mid))
            r = subprocess.run([sys.executable, "-B", tool, mid, parts, png, "--no-glow", "--dist", dist] + extra, capture_output=True, text=True)
            if r.returncode != 0:
                print("FAILED %s %s: %s" % (tag, mid, (r.stderr or r.stdout).strip().splitlines()[-1:]))
            else:
                n += 1
    print("%d previews in %s" % (n, out))


if __name__ == "__main__":
    main(sys.argv[1:])
