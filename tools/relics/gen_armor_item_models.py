"""Turns the item model of every armour item that has worn models into a builtin/entity model for the 3D item renderer (client/relics/worn/lib/ArmorItemRenderer), and keeps the
old model under the name <id>_icon (the inventory icon, the 16x16 sprite, unchanged).

    python3 tools/relics/gen_armor_item_models.py [--check]

Items: the robe sets of NUItems (hood, robe, leggings, boots) and the HEAD and FEET armour relics (a CHEST relic is ornaments over a painted body, it keeps its sprite).
Idempotent: an item whose json is already builtin/entity, or whose icon json exists, is left alone. Display transforms are those of item/generated, so the sprite fallback
(a mob, a missing model, relicfx.models off) sits in the hand exactly as before.
"""
import glob
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp")
MODELS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "models", "item")

DISPLAY = {
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
    "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
    "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
}


def robe_ids():
    src = open(os.path.join(JAVA, "item", "NUItems.java"), encoding="utf8").read()
    ids = []
    for m in re.finditer(r'robe\("([a-z0-9_]+)",\s*RobeItem\.Kind', src):
        i = m.group(1)
        ids += [i + "_hood", i, i + "_leggings", i + "_boots"]
    return ids


def relic_armour_ids():
    consts = {}
    files = glob.glob(os.path.join(JAVA, "relics", "**", "*.java"), recursive=True)
    for f in files:
        for m in re.finditer(r'\b(ID_\w+)\s*=\s*"([a-z0-9_]+)"', open(f, encoding="utf8").read()):
            consts[m.group(1)] = m.group(2)
    pat = re.compile(r'RelicDef\.of\(\s*(?:"([a-z0-9_]+)"|[\w.]*?(ID_\w+))\s*,\s*"[^"]*"\s*,\s*Grade\.\w+\s*,\s*RelicSlot\.(HEAD|FEET)\s*\)')
    out = []
    for f in files:
        for m in pat.finditer(open(f, encoding="utf8").read()):
            out.append(m.group(1) or consts[m.group(2)])
    return sorted(set(out))


def convert(i, check):
    path = os.path.join(MODELS, i + ".json")
    icon = os.path.join(MODELS, i + "_icon.json")
    if not os.path.exists(path):
        return "missing"
    old = json.load(open(path, encoding="utf8"))
    if old.get("parent") == "minecraft:builtin/entity":
        return "done"
    if os.path.exists(icon):
        return "done"
    tex = (old.get("textures") or {}).get("layer0") or "nusmp:item/" + i
    if check:
        return "todo"
    with open(icon, "w", newline="\n") as fh:
        json.dump(old, fh, indent=2)
        fh.write("\n")
    new = {"parent": "minecraft:builtin/entity", "gui_light": "front", "textures": {"particle": tex}, "display": DISPLAY}
    with open(path, "w", newline="\n") as fh:
        json.dump(new, fh, indent=2)
        fh.write("\n")
    return "converted"


def main(argv):
    check = "--check" in argv
    counts = {}
    for i in robe_ids() + relic_armour_ids():
        r = convert(i, check)
        counts[r] = counts.get(r, 0) + 1
        if r == "missing":
            print("no item model json for", i)
    print(counts)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
