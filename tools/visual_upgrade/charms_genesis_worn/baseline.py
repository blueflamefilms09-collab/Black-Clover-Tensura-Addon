"""STEP 0 of the visual upgrade: measures the current look of every item of the slice and writes docs/visual_upgrade/charms_genesis_worn_baseline.json.

    python3 -B tools/visual_upgrade/charms_genesis_worn/baseline.py

Run it BEFORE the upgrade files exist (it reads the catalogs without the override table). The result is committed; build.py reads it again to write the
_after.json and the ratio table, so the baseline is never recomputed from upgraded files.
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import metrics as M  # noqa: E402
import roster as R  # noqa: E402

OUT = os.path.join(M.ROOT, "docs", "visual_upgrade", "charms_genesis_worn_baseline.json")


def main():
    cats = M.Catalogs(upgrade=False)
    items = [M.measure_item(it, cats, upgrade=False) for it in R.ITEMS]
    doc = {"slice": "charms_genesis_worn", "what": "baseline measured from the files of release 1.101.x before the visual upgrade (tools/visual_upgrade/charms_genesis_worn/baseline.py)",
           "weights": M.WEIGHTS, "items": items}
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", newline="\n") as fh:
        json.dump(doc, fh, indent=1)
        fh.write("\n")
    print("%-42s %-8s %5s %5s %6s %6s %3s" % ("item", "grade", "hQ", "wQ", "hTex", "wTex", "grp"))
    for it in items:
        print("%-42s %-8s %5d %5d %6d %6d %3d" % (it["id"], it["grade"], it["hand"]["quads"], it["worn"]["quads"], it["hand"]["painted_texels"], it["worn"]["painted_texels"],
                                              it["hand"]["groups"] + it["worn"]["groups"]))
    print("written", OUT)


if __name__ == "__main__":
    main()
