"""Builds the whole genesis_weapons upgrade: the halo sheet, every item (meshes, LOD meshes, skins, effect data), the measurements (docs/visual_upgrade/genesis_weapons_after.json) and,
with --previews, the before / after contact sheets (docs/previews/visual_upgrade/genesis_weapons/).

    python3 -B tools/visual_upgrade/genesis_weapons/build_all.py [--previews] [--only id,id]

Deterministic: running it twice gives byte-identical meshes and effect data (the skins differ only by the PNG encoder's choices, which are also deterministic).
The hook line in tools/gen_weapon_models.py (end of main) calls main() of this module, so the owner's usual generator run rebuilds the upgrade too.
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import build_item                                                         # noqa: E402
import measure                                                            # noqa: E402
import vu_halo                                                            # noqa: E402
from common import IDS                                                    # noqa: E402


def main(argv=None):
    argv = list(sys.argv[1:] if argv is None else argv)
    only = None
    if "--only" in argv:
        only = argv[argv.index("--only") + 1].split(",")
    vu_halo.build()
    for item_id in IDS:
        if only and item_id not in only:
            continue
        print(item_id)
        build_item.run(item_id)
    measure.main("after")
    if "--previews" in argv:
        import vu_preview
        import importlib
        from common import PREVIEWS
        for item_id in IDS:
            if only and item_id not in only:
                continue
            focus = getattr(importlib.import_module("item_" + item_id), "FOCUS", None)
            print("preview", vu_preview.sheet(item_id, os.path.join(PREVIEWS, item_id + ".png"), focus=focus))


if __name__ == "__main__":
    main()
