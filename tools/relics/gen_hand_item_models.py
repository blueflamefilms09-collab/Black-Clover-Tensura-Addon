"""Turns the item model of every non-weapon relic into a builtin/entity model for the hand renderer (client/relics/hand/HandItemRenderer), and keeps the old sprite model under
the name <id>_icon (the inventory icon, the 16x16 sprite, unchanged).

    python3 tools/relics/gen_hand_item_models.py [--check]

Items: every relic def of the wave sources that is not a weapon (MAIN_HAND) and not an adopted older item: the Curios relics (neck, back, belt, hands, charm), the relic armour
(head, chest, feet) and the consumables. The new <id>.json is `builtin/entity` with NO display block: what a display block did (how a flat sprite sits in a hand, on the ground, in
a frame) is now the `sprite` style of assets/nusmp/relic_hand/transforms.txt, and where the 3D form sits is the style of the relic in the same file.
Idempotent: an item that is already builtin/entity keeps its json (only the preset display block that tools/relics/gen_armor_item_models.py wrote is taken out of it), and an
item whose _icon json exists is left alone. Run it again after any wave icon script (those write the flat model).
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import relic_inventory as RI  # noqa: E402

ROOT = RI.ROOT
MODELS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "models", "item")

# the display block gen_armor_item_models.py gave the armour relics (a vanilla generated item's): it moves to the `sprite` style of transforms.txt
ARMOUR_PRESET = {
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
    "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
    "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
}


def write(path, obj):
    with open(path, "w", newline="\n") as fh:
        json.dump(obj, fh, indent=2)
        fh.write("\n")


def convert(i, check):
    path = os.path.join(MODELS, i + ".json")
    icon = os.path.join(MODELS, i + "_icon.json")
    if not os.path.exists(path):
        return "missing"
    old = json.load(open(path, encoding="utf8"))
    if old.get("parent") == "minecraft:builtin/entity":
        if old.get("display") == ARMOUR_PRESET:
            if check:
                return "todo"
            del old["display"]
            write(path, old)
            return "display moved"
        return "done"
    if os.path.exists(icon):
        return "done"
    tex = (old.get("textures") or {}).get("layer0") or "nusmp:item/" + i
    if check:
        return "todo"
    write(icon, old)
    write(path, {"parent": "minecraft:builtin/entity", "gui_light": "front", "textures": {"particle": tex}})
    return "converted"


def ids():
    rs, _ = RI.relics()
    return sorted({r["id"] for r in rs if not r["adopted"] and r["slot"] != "MAIN_HAND"})


def main(argv):
    check = "--check" in argv
    counts = {}
    for i in ids():
        r = convert(i, check)
        counts[r] = counts.get(r, 0) + 1
        if r == "missing":
            print("no item model json for", i)
    print(counts)
    return 1 if check and counts.get("todo") else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
