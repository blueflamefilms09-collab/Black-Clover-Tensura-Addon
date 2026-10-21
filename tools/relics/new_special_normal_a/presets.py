"""First-person / ground display of the 8 weapons of wave 103: the oversized-weapon rule of tools/relics/display_presets.py applied to these ids only, rows added to display_presets_manifest.json.

    python3 -B tools/relics/new_special_normal_a/presets.py"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))
import display_presets as dp  # noqa: E402
from new_special_normal_a.weapons import WEAPONS  # noqa: E402


def main():
    ids = [w["id"] for w in WEAPONS]
    entries = []
    for item_id in ids:
        mesh = dp.read_mesh(os.path.join(dp.MESHES, item_id + ".txt"))
        mjson = os.path.join(dp.MODELS, item_id + ".json")
        raw = open(mjson, encoding="utf-8").read()
        model = json.loads(raw)
        plan = dp.plan_for(item_id, model, mesh)
        entry = {"id": item_id, "action": plan["action"], "length_blocks": round(mesh.length, 3)}
        if plan["action"] == "preset":
            entry.update(first_person_scale=plan["scale"], first_person_translation_px=plan["translation"], tip_out_of_view=not plan["tip_in_view"],
                         ground_scale=plan["views"][0], fixed_scale=plan["views"][1])
        if plan["action"] in ("preset", "restore"):
            text = json.dumps(dp.apply_plan(model, plan), indent=2)
            if text != raw:
                open(mjson, "w", encoding="utf-8", newline="\n").write(text)
        entries.append(entry)
        print(item_id, entry["action"])
    man = json.load(open(dp.MANIFEST, encoding="utf-8"))
    rows = [r for r in man["weapons"] if r["id"] not in set(ids)] + entries
    man["weapons"] = sorted(rows, key=lambda r: r["id"])
    open(dp.MANIFEST, "w", encoding="utf-8", newline="\n").write(json.dumps(man, indent=2) + "\n")


if __name__ == "__main__":
    main()
