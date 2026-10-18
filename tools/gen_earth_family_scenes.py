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

# the prop painter (EarthPropPainter) with every construct: param = the type (state idle), a short life so the rise and the crumble show
props = {}
for name, typ, scale, ages in (("golem", 0, 1.0, [2, 6, 12, 40, 80, 92]), ("titan", 1, 1.0, [2, 6, 12, 40, 80, 92]), ("sentinel", 2, 1.0, [2, 12, 40, 92]), ("sphinx", 3, 1.0, [2, 12, 40, 92]),
                               ("cage", 4, 1.0, [1, 6, 10, 20, 60, 85, 95]), ("totem", 5, 1.0, [2, 12, 40, 92]), ("spire", 6, 1.0, [1, 5, 9, 14, 40, 92]),
                               ("boulder", 7, 1.0, [10, 30]), ("lance", 8, 1.0, [10, 30])):
    props["efp_%s" % name] = {"kind": "prop", "attr": "Earth", "propKind": "EARTH_CONSTRUCT", "param": typ, "scale": scale, "maxLife": 100, "ages": ages, "tile": [340, 420],
                              "views": [{"name": "3/4", "yaw": 35, "pitch": 14}, {"name": "back", "yaw": 200, "pitch": 14}]}
with open(os.path.join(HERE, "render_preview", "scenes.d", "earth_family_props.json"), "w") as f:
    json.dump(props, f, indent=1)
print(len(props), "prop scenes")

# the effect layer (EarthFamilyLayer): three shapes for each of the six magics, run with tools/vfx_preview/preview.py <scene> ...
vfx = {"_comment": "Earth family effect scenes (EarthFamilyLayer): efx_<magic>_cast / _zone / _burst for Sandstone, Rock, Red Ochre, Stone, Earth, Mineral."}
for key, color in (("sandstone", "FFD5A76C"), ("rock", "FF777C86"), ("red_ochre", "FFD64C37"), ("stone", "FF8F8C86"), ("earth", "FFB08850"), ("mineral", "FF78A8C8")):
    vfx["efx_%s_cast" % key] = {"layer": "EarthFamilyLayer", "shape": "EARTHFAM_CAST", "color": color, "from": [0, 1.5, 0], "to": [0, 1.3, 10], "power": 1.0, "duration": 16,
                                "cam": [4.5, 2.0, 5.5], "target": [0, 1.4, 5.5], "ages": [2, 6, 9, 12, 14, 15], "figures": [[0, 0, -0.4, 1.8], [0, 0, 10.4, 1.8]]}
    vfx["efx_%s_zone" % key] = {"layer": "EarthFamilyLayer", "shape": "EARTHFAM_ZONE", "color": color, "from": [0, 0, 0], "to": [0, 1, 0], "power": 4.0, "duration": 80,
                                "cam": [7.5, 5.5, 8.5], "target": [0, 0.8, 0], "ages": [6, 14, 30, 60, 72], "figures": [[0, 0, 0, 1.8]]}
    vfx["efx_%s_burst" % key] = {"layer": "EarthFamilyLayer", "shape": "EARTHFAM_BURST", "color": color, "from": [0, 0, 0], "to": [0, 1, 0], "power": 1.4, "duration": 28,
                                 "cam": [5.5, 3.2, 6.5], "target": [0, 1.0, 0], "ages": [2, 5, 9, 16, 24], "figures": [[2.2, 0, 0.6, 1.8]]}
with open(os.path.join(HERE, "vfx_preview", "scenes.d", "earth_family.json"), "w") as f:
    json.dump(vfx, f, indent=1)
print(len(vfx) - 1, "effect scenes")
