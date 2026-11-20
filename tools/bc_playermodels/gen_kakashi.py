"""Generates Kakashi Hatake's player-model looks (docs/bosses/kakashi.md) with the Black Clover kit's own builder (bclib / bcparts / bcskin / hd_core, nothing of it forked):

  textures/entity/playermodel/bc_kakashi_hatake*.png (+ _glow)   the painted 256x256 skins (kkskin: body, anime face with the Sharingan or the Mangekyo)
  textures/entity/pmx_bc_kakashi_hatake*.png (+ _glow)           the atlas of the cube parts
  geo/entity/pmx_bc_kakashi_hatake*[_fine|_fx].geo.json          the parts (always drawn / detail distance only / glowing = the Susanoo)
  animations/entity/pmx_bc_kakashi_hatake*.animation.json        idle, walk, run, cast, cast_big (tails, tufts and the band sway)
  playermodel/looks_bc_kakashi.json                              the four looks

The looks (picked by client/playermodel/KakashiLooks.java from the boss's state):
  bc_kakashi_hatake            hitai-ate over the left eye (idle, phases 1 and 2)
  bc_kakashi_hatake_open       band pushed up, the Sharingan open (any move of phases 1 and 2)
  bc_kakashi_hatake_mangekyo   band up, the Mangekyo Sharingan open and glowing (phases 3 and 4)
  bc_kakashi_hatake_susanoo    the Mangekyo look wrapped in the translucent glowing Perfect Susanoo

    python3 -B tools/bc_playermodels/gen_kakashi.py
Deterministic. The old Blender body (entity_meshes/kakashi.txt, tools/blender/boss_kakashi.py) is not touched (rule 2): it is the classic look behind the client setting.
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.dont_write_bytecode = True

import bclib as L  # noqa: E402
import bcparts as P  # noqa: E402
import kkparts as K  # noqa: E402
import kkskin as SK  # noqa: E402

A = L.ASSETS
TEX = os.path.join(A, "textures", "entity")
PMTEX = os.path.join(TEX, "playermodel")
GEO = os.path.join(A, "geo", "entity")
ANI = os.path.join(A, "animations", "entity")
LOOKS = os.path.join(A, "playermodel", "looks_bc_kakashi.json")
STATS = os.path.join(HERE, "stats_kakashi.json")

# id (after 'kakashi_hatake'), eye of the skin, hitai-ate covers the left eye, Susanoo
LOOKS_SPEC = [
    ("", "sharingan", True, False),
    ("_open", "sharingan", False, False),
    ("_mangekyo", "mangekyo", False, False),
    ("_susanoo", "mangekyo", False, True),
]


def build(b, covered, susanoo):
    K.hair(b)
    K.hitai(b, covered)
    K.mask_cloth(b)
    K.flak(b)
    K.guards(b)
    K.legwear(b)
    if susanoo:
        K.susanoo(b)


def build_one(suffix, eye, covered, susanoo):
    cid = "kakashi_hatake" + suffix
    new_id = "bc_" + cid
    skin_png = os.path.join(PMTEX, new_id + ".png")
    skin_glow = os.path.join(PMTEX, new_id + "_glow.png")
    if os.path.isfile(skin_glow):
        os.remove(skin_glow)
    img, glow = SK.build_skin(eye, skin_png, skin_glow)
    b = P.Build(cid)
    build(b, covered, susanoo)
    atlas_png = os.path.join(TEX, "pmx_" + new_id + ".png")
    atlas_glow = os.path.join(TEX, "pmx_" + new_id + "_glow.png")
    if os.path.isfile(atlas_glow):
        os.remove(atlas_glow)
    has_glow = b.atlas.save(atlas_png, atlas_glow)
    names = {}
    for layer, suf in (("main", ""), ("fine", "_fine"), ("fx", "_fx")):
        g = b.layers[layer]
        stem = "pmx_" + new_id + suf
        if g.order:
            L.write_json(os.path.join(GEO, stem + ".geo.json"), g.to_json())
            a = b.anims[layer]
            if a.tracks:
                L.write_json(os.path.join(ANI, stem + ".animation.json"), a.to_json())
            names[layer] = stem
        else:                                                                    # a stale file of an earlier run
            for d, ext in ((GEO, ".geo.json"), (ANI, ".animation.json")):
                p = os.path.join(d, stem + ext)
                if os.path.isfile(p):
                    os.remove(p)
    look = {"skin": new_id, "slim": False, "scale": 1.0}
    if glow is not None:
        look["glow"] = True
    look["extras"] = [{"model": names[l], "texture": "pmx_" + new_id, "glow": bool(l == "fx" and has_glow)} for l in ("main", "fine", "fx") if l in names]
    st = dict(cubes={k: b.layers[k].cubes for k in b.layers}, quads={k: b.layers[k].quads for k in b.layers}, glow_quads={k: b.layers[k].glow_quads for k in b.layers},
              bones={k: len(b.layers[k].bones) for k in b.layers}, atlas_tiles=len(b.atlas.tiles), atlas_fill=round(float((b.atlas.img[..., 3] > 0).mean()), 3),
              glow=bool(glow is not None), atlas_glow=bool(has_glow))
    return new_id, look, st


def main(argv):
    looks, stats = {}, {}
    for suffix, eye, covered, susanoo in LOOKS_SPEC:
        nid, look, st = build_one(suffix, eye, covered, susanoo)
        looks[nid] = look
        stats[nid] = st
        print("%-30s cubes %s quads %s glow_quads %s tiles %d fill %.2f" % (nid, st["cubes"], st["quads"], st["glow_quads"], st["atlas_tiles"], st["atlas_fill"]))
    doc = {"version": 1, "note": "Kakashi Hatake player models (docs/bosses/kakashi.md), written by tools/bc_playermodels/gen_kakashi.py. Ids are bc_kakashi_hatake + a state suffix; the Blender body stays as the classic look.", "looks": looks}
    with open(LOOKS, "w") as f:
        json.dump(doc, f, indent=1)
        f.write("\n")
    with open(STATS, "w") as f:
        json.dump(stats, f, indent=1, sort_keys=True)


if __name__ == "__main__":
    main(sys.argv[1:])
