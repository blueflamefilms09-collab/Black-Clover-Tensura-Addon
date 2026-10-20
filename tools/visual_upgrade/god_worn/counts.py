"""Quad counts of every builder at the three detail levels against the targets (5x the baseline quads, floor 900; held <= 625; lod <= 15%).

    python3 -B tools/visual_upgrade/god_worn/counts.py [id ...]
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import _paths  # noqa: F401,E402
import gw_lib as G  # noqa: E402
import build as B  # noqa: E402
import items as IT  # noqa: E402
import metrics as M  # noqa: E402


def counts(rid, builder):
    out = {}
    for lv in ("full", "held", "lod"):
        D = G.Detail(lv)
        it = G.Item(rid, D)
        builder(it, D)
        out[lv] = it.quads()
    return out


def main(argv):
    base = M.load_json(os.path.join(M.ROOT, "docs", "visual_upgrade", "god_worn_baseline.json"))["items"]
    builders = B.all_builders()
    for rid in IT.IDS:
        if argv and rid not in argv:
            continue
        if rid not in builders:
            continue
        c = counts(rid, builders[rid])
        target = max(900, int(5.05 * base[rid]["quads"]) + 1)
        flag = []
        if c["full"] < target:
            flag.append("LOW(%d)" % target)
        if c["held"] > 625:
            flag.append("HELD>625")
        if c["lod"] > 0.15 * c["full"]:
            flag.append("LOD>15%")
        if c["full"] > 3500:
            flag.append("BIG")
        print("%-30s base %4d target %4d | full %5d held %4d lod %4d (%.1f%%) %s" % (rid, base[rid]["quads"], target, c["full"], c["held"], c["lod"], 100.0 * c["lod"] / max(1, c["full"]), " ".join(flag)))


if __name__ == "__main__":
    main(sys.argv[1:])
