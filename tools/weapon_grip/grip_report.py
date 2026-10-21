"""
grip_report: how far from its grip the 1.110.0 hand was, for every weapon mesh (docs/weapon_grip.md).

    python tools/weapon_grip/grip_report.py [--top 25] [--csv out.csv]

For each mesh of weapon_meshes (the thickened copy when there is one) and its model json, in third person, right hand: the hand point H of the display block (the point of the item's
unit cube that lies at the hand, grip_lib.hand_point), where it falls along the weapon (share of the length from the butt) and how far off the weapon's axis, against the grip
point the finder gives. `miss` is the distance between the old hand and the grip in blocks as a viewer sees it (cube units times the display scale).
"""
import argparse
import json
import math
import os
import statistics
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(ROOT, "tools", "relics"))
import grip_lib as gl      # noqa: E402
import grip_table as gt    # noqa: E402


def rows():
    cj = gl.classes()
    table, golden, _needed = gt.build()
    out = []
    for key, i, path, kind in gt.mesh_files():
        if kind != "old" or key.startswith("sz/"):
            continue
        sz = os.path.join(gl.ASSETS, "weapon_meshes_sz", i + ".txt")
        g = golden.get("sz/" + i) if os.path.exists(sz) else golden.get(key)
        if g is None or g["class"] in ("shield", "item", "handitem"):
            continue
        mj = os.path.join(gl.ASSETS, "models", "item", i + ".json")
        if not os.path.exists(mj):
            mj = os.path.join(gl.ASSETS, "models", "item", gl.STATE_SUFFIX.sub("", i) + ".json")
            if not os.path.exists(mj):
                continue
        model = json.load(open(mj))
        place, _d, _x, body = gl.load_mesh(sz if os.path.exists(sz) else path)
        if not body or not place:
            continue
        disp = gl.read_display(model, "thirdperson_righthand")
        h = gl.hand_point(*disp, left=False)
        ox, oy, ang, sc, ymin = place
        ys = [v[1] for v in body]
        length = (max(ys) - ymin) * sc
        a = math.radians(ang)
        along = (h[0] - ox) * math.cos(a) + (h[1] - oy) * math.sin(a)
        side = -(h[0] - ox) * math.sin(a) + (h[1] - oy) * math.cos(a)
        grip_cube = gl.cube_point(place, g["x"], g["y"], g["z"])
        miss = math.dist(h, grip_cube) * disp[2]
        out.append({"id": i, "class": g["class"], "length": length * disp[2], "old_frac": along / length if length else 0.0, "old_side": side * disp[2], "grip_frac": (g["y"] - ymin) / ((max(ys) - ymin) if max(ys) != ymin else 1.0),
                    "miss": miss, "src": g["source"]})
    return out


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("--top", type=int, default=25)
    ap.add_argument("--csv")
    args = ap.parse_args(argv)
    r = rows()
    print("%d meshes (third person, right hand)" % len(r))
    print("old hand along the weapon: median %.3f, min %.3f, max %.3f   (share of the length from the butt)" % (statistics.median(x["old_frac"] for x in r), min(x["old_frac"] for x in r), max(x["old_frac"] for x in r)))
    print("new grip along the weapon: median %.3f" % statistics.median(x["grip_frac"] for x in r))
    print("miss (blocks): median %.2f, mean %.2f, max %.2f; over 0.20 blocks: %d, over 0.40: %d" % (statistics.median(x["miss"] for x in r), statistics.mean(x["miss"] for x in r),
                                                                                                     max(x["miss"] for x in r), sum(1 for x in r if x["miss"] > 0.2), sum(1 for x in r if x["miss"] > 0.4)))
    for x in sorted(r, key=lambda x: -x["miss"])[:args.top]:
        print("  %-34s %-11s L=%.2f old hand at %.2f  grip at %.2f (%s)  miss %.2f blocks" % (x["id"], x["class"], x["length"], x["old_frac"], x["grip_frac"], x["src"], x["miss"]))
    if args.csv:
        import csv
        with open(args.csv, "w", newline="") as f:
            w = csv.DictWriter(f, fieldnames=list(r[0]))
            w.writeheader()
            w.writerows(r)
    return 0


if __name__ == "__main__":
    sys.exit(main())
