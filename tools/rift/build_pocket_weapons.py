"""Builds the meshes, skins, icons and item models of the 13 boss weapons of the exclusive pocket tombs (1.99.0, wave 78):

    python3 -B tools/rift/build_pocket_weapons.py [id ...]          (no ids = all; idempotent, deterministic)
    N197_PREVIEW=<dir> python3 -B tools/rift/build_pocket_weapons.py ignareths_forge_hammer      (also renders the preview sheet of each)

The weapons are in pocket_weapon_models.py, the table is pocket_weapon_roster.py. After the models: the bow lies on vanilla's diagonal (tools/relics/bow_vanilla.py) and the long weapons get
their first-person display (the rule of tools/relics/display_presets.py, the rows of these weapons only are added to display_presets_manifest.json).
"""
import json
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "..", "relics"))
from pocket_weapon_roster import ROSTER                            # noqa: E402


def presets(check=False, only=None):
    """The oversized-weapon display rule applied to the 13 weapons only (the rows of every other weapon stay as they are)."""
    import display_presets as dp
    ids = [e["id"] for e in ROSTER if not only or e["id"] in only]
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


def main(only):
    import pocket_weapon_models as models
    n = 0
    for e in ROSTER:
        if only and e["id"] not in only:
            continue
        fn = models.BUILDERS.get(e["id"])
        if fn is None:
            print("no builder yet for", e["id"])
            continue
        fn()
        n += 1
    print("built", n)
    subprocess.run([sys.executable, "-B", os.path.join(HERE, "..", "relics", "bow_vanilla.py")], check=False)
    presets(False, only)
    subprocess.run([sys.executable, "-B", os.path.join(HERE, "..", "visual_upgrade", "god_melee_b", "build.py")] + sorted(only), check=False)     # 1.102.0 visual upgrade (slice god_melee_b): the upgraded twins of the God weapons, new files under up/ (the old ones stay)


if __name__ == "__main__":
    main(set(sys.argv[1:]))
