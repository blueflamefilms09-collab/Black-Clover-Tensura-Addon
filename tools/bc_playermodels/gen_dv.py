"""Generates the devil player-model looks (docs/devil_looks.md): for every devil of dvtable.DEVILS (the 26 devils of the Devil Contract roster, WorldDevil.java)

  textures/entity/playermodel/dv_<id>.png (+ _glow)        the painted 256x256 skin (dvskin: body from the outfit spec, face from bcskin)
  textures/entity/pmx_dv_<id>.png (+ _glow)                the atlas of the cube parts
  geo/entity/pmx_dv_<id>[_fine|_fx|_hair|_hair_fine].geo.json   the parts (see dvlib.LAYERS)
  animations/entity/pmx_dv_<id>*.animation.json            idle, walk, run, cast, cast_big
  playermodel/looks_dv.json                                the looks, id dv_<devil id>

    python3 -B tools/bc_playermodels/gen_dv.py            everything
    python3 -B tools/bc_playermodels/gen_dv.py liebe      only the devils whose id contains 'liebe'
Deterministic, built on gen_bc's helpers and bclib / bcparts (the Black Clover builder is used as it is; nothing of it is rewritten). Nothing of the old looks is touched (rule 2).
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.dont_write_bytecode = True

import bclib as L  # noqa: E402
import dvlib as D  # noqa: E402
import dvskin as DS  # noqa: E402
import dvtable as T  # noqa: E402

A = L.ASSETS
TEX = os.path.join(A, "textures", "entity")
PMTEX = os.path.join(TEX, "playermodel")
GEO = os.path.join(A, "geo", "entity")
ANI = os.path.join(A, "animations", "entity")
LOOKS_DV = os.path.join(A, "playermodel", "looks_dv.json")
STATS_JSON = os.path.join(HERE, "stats_dv.json")

STATS = {}


def build_one(spec):
    cid = spec["id"]
    new_id = "dv_" + cid
    slim = bool(spec["slim"])
    skin_png = os.path.join(PMTEX, new_id + ".png")
    skin_glow = os.path.join(PMTEX, new_id + "_glow.png")
    if os.path.isfile(skin_glow):
        os.remove(skin_glow)
    img, glow = DS.build_skin(spec, slim, skin_png, skin_glow)
    b = D.DvBuild(cid)
    b.slim = slim
    spec["build"](b)
    atlas_png = os.path.join(TEX, "pmx_" + new_id + ".png")
    atlas_glow = os.path.join(TEX, "pmx_" + new_id + "_glow.png")
    if os.path.isfile(atlas_glow):
        os.remove(atlas_glow)
    has_glow = b.atlas.save(atlas_png, atlas_glow)
    names = {}
    for layer, suffix in D.LAYERS:
        stem = "pmx_" + new_id + suffix
        g = b.layers[layer]
        if g.order:
            L.write_json(os.path.join(GEO, stem + ".geo.json"), g.to_json())
            a = b.anims[layer]
            if a.tracks:
                L.write_json(os.path.join(ANI, stem + ".animation.json"), a.to_json())
            elif os.path.isfile(os.path.join(ANI, stem + ".animation.json")):
                os.remove(os.path.join(ANI, stem + ".animation.json"))
            names[layer] = stem
        else:                                                                    # a stale file of an earlier run
            for d, ext in ((GEO, ".geo.json"), (ANI, ".animation.json")):
                p = os.path.join(d, stem + ext)
                if os.path.isfile(p):
                    os.remove(p)
    look = {"skin": new_id, "slim": slim, "scale": spec["scale"]}
    if glow is not None:
        look["glow"] = True
    extras = []
    for layer, suffix in D.LAYERS:
        if layer in names:
            extras.append({"model": names[layer], "texture": "pmx_" + new_id, "glow": bool(layer == "fx" and has_glow)})
    look["extras"] = extras
    hair_w = 8.0 + 2 * (D.HG + D.HT)
    STATS[new_id] = dict(cubes={k: b.layers[k].cubes for k in b.layers}, quads={k: b.layers[k].quads for k in b.layers}, glow_quads={k: b.layers[k].glow_quads for k in b.layers},
                         bones={k: len(b.layers[k].bones) for k in b.layers}, atlas_tiles=len(b.atlas.tiles), atlas_fill=round(float((b.atlas.img[..., 3] > 0).mean()), 3),
                         glow=bool(glow is not None), atlas_glow=bool(has_glow), hair_shell_px=round(hair_w, 2), scale=spec["scale"])
    return new_id, look


def main(argv):
    flt = [a for a in argv if not a.startswith("-")]
    looks = {}
    if os.path.isfile(LOOKS_DV) and flt:
        looks = json.load(open(LOOKS_DV))["looks"]
    for spec in T.DEVILS:
        if flt and not any(f in spec["id"] for f in flt):
            continue
        nid, look = build_one(spec)
        looks[nid] = look
        s = STATS[nid]
        print("%-26s cubes %s quads %s glow %s tiles %d fill %.2f" % (nid, s["cubes"], s["quads"], s["glow_quads"], s["atlas_tiles"], s["atlas_fill"]))
    doc = {"version": 1, "note": "Devil player models (docs/devil_looks.md), written by tools/bc_playermodels/gen_dv.py. Every id is 'dv_' + the WorldDevil id; nothing here replaces an older look.", "looks": dict(sorted(looks.items()))}
    with open(LOOKS_DV, "w") as f:
        json.dump(doc, f, indent=1)
        f.write("\n")
    allstats = json.load(open(STATS_JSON)) if os.path.isfile(STATS_JSON) else {}
    allstats.update(STATS)
    with open(STATS_JSON, "w") as f:
        json.dump(allstats, f, indent=1, sort_keys=True)


if __name__ == "__main__":
    main(sys.argv[1:])
