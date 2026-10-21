"""Applies the oversized-weapon display rule (tools/relics/display_presets.py, docs/asset_pipeline.md) to the 9 weapons of Wave102 only, and adds their rows to
tools/relics/display_presets_manifest.json (the rows of every other weapon are left as they are):

    python3 -B tools/relics/new_wave102/presets102.py [--check]
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))
sys.path.insert(0, HERE)
import display_presets as dp                                         # noqa: E402
from roster102 import WEAPONS                                        # noqa: E402


def main(check):
    ids = [e["id"] for e in WEAPONS]
    entries, changed = [], []
    shields = {e["id"] for e in WEAPONS if e["kind"] == "shield"}
    for item_id in ids:
        mesh = dp.read_mesh(os.path.join(dp.MESHES, item_id + ".txt"))
        if item_id in shields:                                       # a shield's display is vanilla's (tools/gen_shield_models.py, ShieldModelsTest): this tool never touches it
            entries.append({"id": item_id, "action": "skip_manual", "length_blocks": round(mesh.length, 3)})
            continue
        mjson = os.path.join(dp.MODELS, item_id + ".json")
        raw = open(mjson, encoding="utf-8").read()
        model = json.loads(raw)
        plan = dp.plan_for(item_id, model, mesh)
        entry = {"id": item_id, "action": plan["action"], "length_blocks": round(mesh.length, 3)}
        if plan["action"] == "preset":
            entry.update(first_person_scale=plan["scale"], first_person_translation_px=plan["translation"], tip_out_of_view=not plan["tip_in_view"],
                         ground_scale=plan["views"][0], fixed_scale=plan["views"][1])
        if plan["action"] in ("preset", "restore"):
            new = dp.apply_plan(model, plan)
            text = json.dumps(new, indent=2)
            if text != raw:
                changed.append(item_id)
                entry["rewritten"] = True
                if not check:
                    open(mjson, "w", encoding="utf-8", newline="\n").write(text)
        entries.append(entry)
    print("%d weapons; %d model jsons %s" % (len(entries), len(changed), "would change" if check else "rewritten"))
    for e in entries:
        if e["action"] == "preset":
            print("  %-34s L=%.2f s=%.3f%s" % (e["id"], e["length_blocks"], e["first_person_scale"], "  OUT-OF-VIEW" if e["tip_out_of_view"] else ""))
    if not check:
        man = json.load(open(dp.MANIFEST, encoding="utf-8"))
        mine = set(ids)
        rows = [r for r in man["weapons"] if r["id"] not in mine] + [{k: v for k, v in e.items() if k != "rewritten"} for e in entries]
        man["weapons"] = sorted(rows, key=lambda r: r["id"])
        open(dp.MANIFEST, "w", encoding="utf-8", newline="\n").write(json.dumps(man, indent=2) + "\n")
    return 1 if (check and changed) else 0


if __name__ == "__main__":
    sys.exit(main("--check" in sys.argv))
