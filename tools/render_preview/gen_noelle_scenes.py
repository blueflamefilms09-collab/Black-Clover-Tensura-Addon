import json
out = 'tools/render_preview/scenes.d/noelle_forms.json'
REAL = ["book/NoelleFormRules.java", "client/aura/NoelleFormRig.java", "client/aura/NoelleFormsAura.java", "client/aura/NoelleFormsOptions.java"]
scenes = {}
scenes["_comment"] = ("Noelle Silva's armour forms (docs/magic/noelle_forms.md): Aura.WATER style = form (low four bits) + 16 for the character tier. 1 Valkyrie / Saint Valkyrie, 2 Mermaid, "
                      "3 Sea Dragon God, 4 Black Dragon. Run: python3 -B tools/render_preview/preview.py noelle_val_char ... (noelle_<form>_<tier> draw front / back 3/4 / side, noelle_<form>_<tier>_close the upper "
                      "body, noelle_<form>_<tier>_forge the first ticks, nvl_piece_* single models; regenerate this file with tools/render_preview/gen_noelle_scenes.py)")
forms = [("val", 1, 6.0, 0.1), ("mer", 2, 5.4, 0.4), ("drg", 3, 6.0, 0.4), ("blk", 4, 6.0, 0.4)]


def aura(style, views, ages=(60,), total=140, tile=(440, 600)):
    return {"kind": "aura", "attr": "Water", "realFiles": REAL, "style": style, "total": total, "ages": list(ages), "tile": list(tile), "views": views}


for f, st, dist, tz in forms:
    for tier, bit in (("std", 0), ("char", 16)):
        d = dist + (0.4 if bit else 0.0)
        scenes["noelle_%s_%s" % (f, tier)] = aura(st + bit, [
            {"name": "front", "yaw": 14, "pitch": 6, "target": [0, 1.0, tz], "dist": d},
            {"name": "back 3/4", "yaw": 150, "pitch": 12, "target": [0, 1.0, tz + 0.2], "dist": d + 0.2},
            {"name": "side", "yaw": 90, "pitch": 6, "target": [0, 1.0, tz + 0.4], "dist": d + 0.2}])
        scenes["noelle_%s_%s_close" % (f, tier)] = aura(st + bit, [
            {"name": "close front", "yaw": 18, "pitch": 4, "target": [0, 1.25, 0.0], "dist": 2.5},
            {"name": "close 3/4", "yaw": 55, "pitch": 8, "target": [0, 1.25, 0.1], "dist": 2.7}], tile=(520, 520))
        scenes["noelle_%s_%s_forge" % (f, tier)] = aura(st + bit, [{"name": "3/4", "yaw": 35, "pitch": 10, "target": [0, 1.0, tz], "dist": d + 0.4}],
                                                        ages=(1, 5, 9, 14, 20, 30, 48), total=140, tile=(260, 380))
scenes["nvl_piece_wings"] = {"kind": "geo", "model": "nvl_arm_wings", "space": "PLAYER", "layer": "TRANSLUCENT", "glow": "FFBFF4FF", "anim": "idle", "ages": [0], "tile": [420, 520],
                             "views": [{"name": "back", "yaw": 180, "pitch": 8}, {"name": "front", "yaw": 0, "pitch": 8}, {"name": "top", "yaw": 0, "pitch": 80}]}
scenes["nml_piece_train"] = {"kind": "geo", "model": "nml_arm_train", "space": "PLAYER", "layer": "TRANSLUCENT", "glow": "FF9CF8FF", "anim": "idle", "ages": [0], "tile": [420, 400],
                             "views": [{"name": "side", "yaw": 90, "pitch": 4}, {"name": "back", "yaw": 180, "pitch": 20}, {"name": "top", "yaw": 0, "pitch": 80}]}
with open(out, 'w') as fh:
    fh.write("{\n")
    items = list(scenes.items())
    for i, (k, v) in enumerate(items):
        fh.write(" %s: %s%s\n" % (json.dumps(k), json.dumps(v, separators=(", ", ": ")), "," if i + 1 < len(items) else ""))
    fh.write("}\n")
print("scenes", len(scenes))
