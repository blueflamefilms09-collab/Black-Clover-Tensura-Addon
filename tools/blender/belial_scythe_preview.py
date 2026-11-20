"""Software preview of the Belial scythe mesh (docs/belial/previews/scythe_full.png and scythe_blade_root_emblem.png): the game's rasteriser, per-vertex normals.

    python tools/blender/belial_scythe_preview.py [--out docs/belial/previews] [--mesh djinn_weapon_belial_scythe]
"""
import argparse
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "tools", "relics"))
import preview_mesh as PM  # noqa: E402


def main(argv):
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=os.path.join(ROOT, "docs", "belial", "previews"))
    ap.add_argument("--mesh", default="djinn_weapon_belial_scythe")
    a = ap.parse_args(argv)
    PM.render_preview(a.mesh, None, os.path.join(a.out, "scythe_full.png"), views=("profile", "three_quarter"), panel=(520, 900), normals="vertex")
    PM.render_preview(a.mesh, None, os.path.join(a.out, "scythe_blade_root_emblem.png"), views=("profile", "three_quarter"), panel=(900, 800), normals="vertex", focus=(196, 266))


if __name__ == "__main__":
    main(sys.argv[1:])
