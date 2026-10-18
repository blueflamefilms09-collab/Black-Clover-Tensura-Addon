"""Writes the render-preview scenes of the earth family (tools/render_preview/scenes.d/earth_family_models.json): every construct in its clips.

    python3 -B tools/gen_earth_family_scenes.py
    RENDER_PREVIEW_BUILD=<scratch> python3 -B tools/render_preview/preview.py efm_earth_golem_idle efm_earth_golem_attack ...

(The aura scenes live in earth_family_aura.json; scene names starting efm_ are the models, ef_ the armours, efp_ the prop painter.)
"""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
sc = {}


def add(name, model, anim, glow, ages, tile=(520, 520), yaws=(0, 40, 150)):
    sc[name] = {"kind": "geo", "model": model, "anim": anim, "glow": glow, "ages": ages, "tile": list(tile), "space": "PROP", "layer": "CUTOUT",
                "views": [{"name": "v%d" % y, "yaw": y, "pitch": 10} for y in yaws]}


for m, g in (("earth_golem", "FFD2E65A"), ("rock_titan", "FFFF9640"), ("stone_sentinel", "FF96E6FF"), ("sandstone_sphinx", "FFFFE08C"), ("red_ochre_totem", "FFFF7840"),
             ("mineral_spire", "FF78DCFF"), ("sandstone_cage", "FFFFE08C")):
    add("efm_%s_idle" % m, m, "idle", g, [0], yaws=(0, 40, 150))
for m, g, an, ages in (("earth_golem", "FFD2E65A", "attack", [0, 8, 13, 18, 26]), ("earth_golem", "FFD2E65A", "walk", [0, 5, 10, 15]),
                       ("rock_titan", "FFFF9640", "attack", [0, 8, 13, 18, 26]), ("stone_sentinel", "FF96E6FF", "attack", [0, 10, 15, 19, 30]),
                       ("sandstone_sphinx", "FFFFE08C", "attack", [0, 8, 12, 18]), ("sandstone_cage", "FFFFE08C", "form", [0, 10, 16, 21]),
                       ("red_ochre_totem", "FFFF7840", "attack", [0, 6, 10, 16]), ("mineral_spire", "FF78DCFF", "grow", [0, 4, 8, 12, 17])):
    add("efm_%s_%s" % (m, an), m, an, g, ages, tile=(380, 380), yaws=(40,))
add("efm_rock_boulder_idle", "rock_boulder", "idle", "FFFF9640", [0], tile=(380, 380), yaws=(40,))
add("efm_mineral_lance_idle", "mineral_lance", "idle", "FF78DCFF", [0], tile=(380, 380), yaws=(40,))

with open(os.path.join(HERE, "render_preview", "scenes.d", "earth_family_models.json"), "w") as f:
    json.dump(sc, f, indent=1)
print(len(sc), "scenes")
