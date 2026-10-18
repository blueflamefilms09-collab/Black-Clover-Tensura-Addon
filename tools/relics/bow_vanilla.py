"""bow_vanilla: every bow of the mod is held exactly like vanilla's Bow (1.63.0, owner: "make all bow base items use the same as base minecraft bow").

Vanilla's bow sprite lies on the item's diagonal and its model json carries these display transforms; a bow mesh that is placed on the same diagonal (place angle 45,
its middle at the cube's centre, where the sprite's grip is) can take them unchanged, so the draw pose, the arm and the sizes match the vanilla bow. For every id of
class "bow" in weapon_classes.json this tool (1) rewrites the `place` line of the rest mesh and of all its stage meshes (<id>_pull1..3, <id>_curse...) to angle 45 about
the middle of the rest mesh, keeping the scale and ymin, and (2) writes vanilla's display block into the model json (marked "manual" so display_presets.py leaves it).
Idempotent. Run:  python tools/relics/bow_vanilla.py [--check]   (--check exits 1 when it would change something; BowDisplayGuardTest is the CI guard).
"""
import glob
import json
import math
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")

VANILLA_DISPLAY = {
    "thirdperson_righthand": {"rotation": [-80, 260, -40], "translation": [-1, -2, 2.5], "scale": [0.9, 0.9, 0.9]},
    "thirdperson_lefthand": {"rotation": [-80, -280, 40], "translation": [-1, -2, 2.5], "scale": [0.9, 0.9, 0.9]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "gui": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
}


def bow_ids():
    with open(os.path.join(ASSETS, "weapon_classes.json"), encoding="utf-8") as f:
        return [k for k, v in json.load(f)["weapons"].items() if v == "bow"]


def y_range(path):
    ys = []
    for line in open(path, encoding="utf-8"):
        v = line.split()
        if v and v[0] in ("q", "qn"):
            stride = 5 if v[0] == "q" else 8
            ys += [float(v[2 + k * stride + 1]) for k in range(4)]
    return min(ys), max(ys)


def main(check):
    changed = []
    for bid in bow_ids():
        base = os.path.join(ASSETS, "weapon_meshes", bid + ".txt")
        if not os.path.exists(base):
            continue
        place = next(l.split() for l in open(base, encoding="utf-8") if l.startswith("place "))
        sc, ymin = float(place[4]), float(place[5])
        lo, hi = y_range(base)
        mid = 0.5 * (hi - lo) * sc
        ox, oy = 0.5 - mid * math.cos(math.radians(45)), 0.5 - mid * math.sin(math.radians(45))
        line = "place %.5f %.5f 45.000 %s %s" % (ox, oy, place[4], place[5])
        own = set(bow_ids())
        stages = [q for q in sorted(glob.glob(os.path.join(ASSETS, "weapon_meshes", bid + "_*.txt")))
                  if not any((os.path.basename(q) == o + ".txt" or os.path.basename(q).startswith(o + "_")) and len(o) > len(bid) for o in own)]   # legendary_bow_curse_* belongs to legendary_bow_curse
        for path in [base] + stages:
            text = open(path, encoding="utf-8").read().split("\n")
            for i, l in enumerate(text):
                if l.startswith("place "):
                    if l != line:
                        text[i] = line
                        changed.append(os.path.relpath(path, ROOT))
                    break
            if not check:
                open(path, "w", encoding="utf-8", newline="\n").write("\n".join(text))
        mj = os.path.join(ASSETS, "models", "item", bid + ".json")
        if os.path.exists(mj):
            d = json.load(open(mj, encoding="utf-8"))
            if d.get("display") != VANILLA_DISPLAY or d.get("nusmp_display_preset") != "manual":
                changed.append(os.path.relpath(mj, ROOT))
                d["display"] = json.loads(json.dumps(VANILLA_DISPLAY))
                d["nusmp_display_preset"] = "manual"
                if not check:
                    json.dump(d, open(mj, "w", encoding="utf-8"), indent=2)
    print("\n".join(changed) or "nothing to change")
    return 1 if (check and changed) else 0


if __name__ == "__main__":
    sys.exit(main("--check" in sys.argv))
