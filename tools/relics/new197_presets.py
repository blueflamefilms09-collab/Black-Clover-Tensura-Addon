"""Applies the oversized-weapon display rule (tools/relics/display_presets.py, docs/asset_pipeline.md) to the 35 weapons of 1.97.0 only, and adds their rows to
tools/relics/display_presets_manifest.json (the rows of every other weapon are left as they are):

    python3 -B tools/relics/new197_presets.py [--check]
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import display_presets as dp                                         # noqa: E402
from new197_roster import ROSTER                                     # noqa: E402


def main(check):
    ids = [e["id"] for e in ROSTER]
    entries, changed = [], []
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
