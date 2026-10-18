"""elite/items: the bookkeeping that follows gen_elite_weapons.py, touching ONLY the six elite weapons' lines of the shared tables (the older entries of the tables are left exactly as they are).

    python3 -B tools/elite/gen_elite_weapons.py                  # meshes, skins, model jsons, tools/relics/weapon_classes.json
    python3 -B tools/elite/finish_weapon_assets.py               # assets/nusmp/weapon_classes.json, the display presets of the six model jsons, their rows of the presets manifest

Why a separate step: tools/relics/audit_weapon_scale.py --write and tools/relics/display_presets.py --apply also refresh lines of OTHER weapons (the checked-in copies had drifted:
the bow class range, two manifest lengths), which is not this subsystem's business. So this script runs the presets tool, keeps the six rewritten model jsons, and merges only the six
new manifest rows into the committed manifest; and it adds the six class lines to the game's class table by hand.
"""
import json
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from art_lib import ASSETS, ROOT  # noqa: E402

IDS = ["astral_edge", "void_edge", "fenrir_sword", "runic_sword_renewed", "runic_sword_ancient", "runic_sword_mythical"]
CLASSES = {"astral_edge": "longsword", "void_edge": "longsword", "fenrir_sword": "greatsword", "runic_sword_renewed": "longsword", "runic_sword_ancient": "longsword",
           "runic_sword_mythical": "greatsword"}


def rel(p):
    return os.path.relpath(p, ROOT).replace(os.sep, "/")


def class_table():
    path = os.path.join(ASSETS, "weapon_classes.json")
    with open(path, encoding="utf-8") as f:
        text = f.read()
    doc = json.loads(text)
    added = False
    for i in IDS:
        if i not in doc["weapons"]:
            doc["weapons"][i] = CLASSES[i]
            added = True
    if added:
        with open(path, "w", newline="\n", encoding="utf-8") as f:
            f.write(json.dumps(doc, indent=1) + ("\n" if text.endswith("\n") else ""))
    print("assets weapon_classes.json:", "six lines added" if added else "already there")


def presets():
    manifest_path = os.path.join(ROOT, "tools", "relics", "display_presets_manifest.json")
    committed = subprocess.run(["git", "show", "HEAD:tools/relics/display_presets_manifest.json"], cwd=ROOT, capture_output=True, text=True, check=True).stdout
    subprocess.run([sys.executable, "-B", os.path.join(ROOT, "tools", "relics", "display_presets.py"), "--apply"], cwd=ROOT, capture_output=True, text=True, check=True)
    with open(manifest_path, encoding="utf-8") as f:
        fresh = json.load(f)
    mine = [w for w in fresh["weapons"] if w["id"] in IDS]
    doc = json.loads(committed)
    have = {w["id"] for w in doc["weapons"]}
    doc["weapons"] = [w for w in doc["weapons"] if w["id"] not in IDS] + [w for w in mine]
    doc["weapons"].sort(key=lambda w: w["id"]) if [w["id"] for w in json.loads(committed)["weapons"]] == sorted(have) else None
    with open(manifest_path, "w", newline="\n", encoding="utf-8") as f:
        f.write(json.dumps(doc, indent=2) + ("\n" if committed.endswith("\n") else ""))
    print("presets manifest:", len(mine), "rows merged")


if __name__ == "__main__":
    class_table()
    presets()
