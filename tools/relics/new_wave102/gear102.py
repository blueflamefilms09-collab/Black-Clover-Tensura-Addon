"""The sprites and item models of the 20 gear relics of Wave102 (4 God, 16 Special): the 16 x 16 inventory sprite (relic_icon_lib: a motif in the relic's own palette, its own
variation drawn from its id), `models/item/<id>.json` (builtin/entity: the hand / worn renderer draws it) and `<id>_icon.json` (the flat sprite for the inventory), exactly the
shapes gen_relic_waves.py writes for the generated waves (that tool is NOT run here: it also rewrites en_us.json, which this release does not touch).
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, ".."))
import relic_icon_lib as ICONS  # noqa: E402
from roster102 import GEAR  # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
RES = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")


def write(path, data, binary=False):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb" if binary else "w", **({} if binary else {"encoding": "utf-8", "newline": "\n"})) as f:
        f.write(data)


def main():
    taken = set()
    for r in GEAR:
        for bump in range(50):
            icon = ICONS.render(r["icon"], r["pal"][0], r["pal"][1], r["pal"][2], r["id"], bump)
            if icon not in taken:
                break
        else:
            raise SystemExit("no distinct sprite for " + r["id"])
        taken.add(icon)
        write(os.path.join(RES, "textures", "item", r["id"] + ".png"), icon, binary=True)
        model = json.dumps({"parent": "minecraft:builtin/entity", "gui_light": "front", "textures": {"particle": "nusmp:item/" + r["id"]}}, indent=2) + "\n"
        write(os.path.join(RES, "models", "item", r["id"] + ".json"), model)
        icon_model = json.dumps({"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/" + r["id"]}}, indent=2) + "\n"
        write(os.path.join(RES, "models", "item", r["id"] + "_icon.json"), icon_model)
    print("wrote %d sprites and item models" % len(GEAR))


if __name__ == "__main__":
    main()
