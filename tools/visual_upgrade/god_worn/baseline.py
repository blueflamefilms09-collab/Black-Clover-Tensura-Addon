"""STEP 0 of the slice `god_worn`: measures every item as it is in the repo BEFORE the upgrade and writes docs/visual_upgrade/god_worn_baseline.json.

    python3 -B tools/visual_upgrade/god_worn/baseline.py

Reads assets/nusmp/relic_worn/catalog.txt (the `relic <id>` entry, else the look of the relic) and the model files, through metrics.py. Run it BEFORE build.py has written
anything: build.py never touches catalog.txt or the old model files (it adds new ones), so the numbers do not change afterwards, but the file is committed first anyway.
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import items  # noqa: E402
import metrics as M  # noqa: E402

OUT = os.path.join(M.ROOT, "docs", "visual_upgrade", "god_worn_baseline.json")


def main():
    cat = M.read_catalog()
    out = {
        "slice": "god_worn", "state": "baseline (before the 1.102.0 visual upgrade)", "grade": "GOD",
        "kind": "worn relic (3D worn models of the worn relic library, plus painted armour layer for chest relics)",
        "score": {"weights": M.WEIGHTS, "references": M.REFS, "formula": "V = sum(weight * count / reference)"},
        "items": {},
    }
    for rid, name, slot, look, pal, _lore in items.ITEMS:
        m = M.measure_old(rid, slot, look, cat)
        m.update({"name": name, "slot": slot, "look": look, "grade": "GOD", "kind": "worn relic"})
        out["items"][rid] = m
    tot = {k: sum(i[k] for i in out["items"].values()) for k in ("quads", "vertices", "painted_texels", "ambient_layers", "animation_groups", "glow_passes")}
    tot["V"] = round(sum(i["V"] for i in out["items"].values()), 4)
    out["totals"] = tot
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf8", newline="\n") as fh:
        json.dump(out, fh, indent=1, sort_keys=False)
        fh.write("\n")
    for rid, m in out["items"].items():
        print("%-30s pieces %d quads %4d texels %6d glow %d ambient %d anim %d V %.3f" % (rid, m["pieces"], m["quads"], m["painted_texels"], m["glow_passes"], m["ambient_layers"], m["animation_groups"], m["V"]))
    print("total", tot)


if __name__ == "__main__":
    main()
