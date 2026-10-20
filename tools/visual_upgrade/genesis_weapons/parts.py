"""Prints the quad count of every part of an item at full detail and at the LOD detail (a tuning aid: python3 -B tools/visual_upgrade/genesis_weapons/parts.py blackrazor)."""
import importlib
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import build_item as b                                                    # noqa: E402

if __name__ == "__main__":
    i = sys.argv[1]
    mod = importlib.import_module("item_" + i)
    for lod, d in ((False, None), (True, 0.12)):
        it = b.build_state(mod, i, i, "", lod, d)
        q = it.stats()
        body = sum(len(p.F) for p in it.mesh.parts if p.layer == 0)
        print("LOD" if lod else "FULL", "body quads", body, "all", sum(q.values()))
        for k, v in sorted(q.items(), key=lambda kv: -kv[1])[:14]:
            print("   ", k, v)
