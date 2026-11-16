"""Generates the Black Clover boss player models (docs/bc_playermodels.md): for every look of bcchars.CHARS

  textures/entity/playermodel/bc_<id>.png (+ _glow)       the retouched 256x256 skin (existing skin, new face, hat layer cleared)
  textures/entity/pmx_bc_<id>.png (+ _glow)               the atlas of the cube parts
  geo/entity/pmx_bc_<id>.geo.json, _fine, _fx             the parts (always drawn / only within the detail distance / the glowing ones)
  animations/entity/pmx_bc_<id>*.animation.json           idle, walk, run, cast, cast_big
  playermodel/looks_bc.json                               the looks (id bc_<old id>), the old look's extras kept in front of the new ones

    python3 -B tools/bc_playermodels/gen_bc.py            everything
    python3 -B tools/bc_playermodels/gen_bc.py lucius     only the looks whose id contains 'lucius'
Deterministic. Nothing of the old looks is touched (rule 2): looks.json keeps every entry, the new ones are separate ids in a separate table.
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.dont_write_bytecode = True

import bclib as L  # noqa: E402
import bcparts as P  # noqa: E402
import bcskin as S  # noqa: E402
import bcchars as CH  # noqa: E402
import bcchars2 as CH2  # noqa: E402   (wave 2: docs/bosses_bc_wave2.md)

A = L.ASSETS
TEX = os.path.join(A, "textures", "entity")
PMTEX = os.path.join(TEX, "playermodel")
GEO = os.path.join(A, "geo", "entity")
ANI = os.path.join(A, "animations", "entity")
LOOKS_OLD = os.path.join(A, "playermodel")
LOOKS_BC = os.path.join(LOOKS_OLD, "looks_bc.json")

STATS = {}


def old_looks():
    out = {}
    for fn in ("looks.json", "looks_abyssal.json", "looks_dungeon.json", "looks_tier.json"):
        p = os.path.join(LOOKS_OLD, fn)
        if os.path.isfile(p):
            for k, v in json.load(open(p))["looks"].items():
                out.setdefault(k, v)
    return out


def keep_old_bones(new_id, old_extras, keep):
    """The old extras reduced to the bones named in `keep` (and their children and parents), written as pmx_bc_<id>_keep (geo, animation and a copy of the old atlas under the same name) so the new look
    can keep a held prop or a cape and drop the crude horns, wings and halo it replaces. Returns the extras entries."""
    out = []
    for k, x in enumerate(old_extras):
        stem = "pmx_bc_%s_keep" % new_id[3:] if k == 0 else "pmx_bc_%s_keep%d" % (new_id[3:], k)
        g = json.load(open(os.path.join(GEO, x["model"] + ".geo.json")))
        geom = g["minecraft:geometry"][0]
        bones = {b["name"]: b for b in geom["bones"]}
        want = {n for n in keep if n in bones}
        changed = True
        while changed:                                                           # first the children of a kept bone ...
            changed = False
            for n, b in bones.items():
                if b.get("parent") in want and n not in want:
                    want.add(n)
                    changed = True
        for n in list(want):                                                     # ... then the parents a kept bone hangs from (not their other children)
            p = bones[n].get("parent")
            while p and p not in want:
                want.add(p)
                p = bones[p].get("parent")
        if not any(bones[n].get("cubes") for n in want):
            continue
        geom["bones"] = [b for b in geom["bones"] if b["name"] in want]
        geom["description"]["identifier"] = "geometry." + stem
        L.write_json(os.path.join(GEO, stem + ".geo.json"), g)
        ap = os.path.join(ANI, x["model"] + ".animation.json")
        if os.path.isfile(ap):
            an = json.load(open(ap))
            for clip in an.get("animations", {}).values():
                clip["bones"] = {n: v for n, v in clip.get("bones", {}).items() if n in want}
            L.write_json(os.path.join(ANI, stem + ".animation.json"), an)
        import shutil
        for suf in (".png", "_glow.png"):
            src = os.path.join(TEX, x["texture"] + suf)
            if os.path.isfile(src):
                shutil.copyfile(src, os.path.join(TEX, stem + suf))
        out.append({"model": stem, "texture": stem, "glow": bool(x.get("glow", False))})
    return out


def build_one(spec, olds):
    cid = spec["id"]
    old = olds[cid]
    new_id = "bc_" + cid
    slim = bool(old.get("slim"))
    # skin
    skin_png = os.path.join(PMTEX, new_id + ".png")
    skin_glow = os.path.join(PMTEX, new_id + "_glow.png")
    if os.path.isfile(skin_glow):
        os.remove(skin_glow)
    img, glow = S.build_skin(old["skin"], slim, spec["face"], skin_png, skin_glow)
    # parts
    b = P.Build(cid)
    b.slim = slim
    spec["build"](b)
    atlas_png = os.path.join(TEX, "pmx_" + new_id + ".png")
    atlas_glow = os.path.join(TEX, "pmx_" + new_id + "_glow.png")
    if os.path.isfile(atlas_glow):
        os.remove(atlas_glow)
    has_glow = b.atlas.save(atlas_png, atlas_glow)
    names = {}
    for layer, suffix in (("main", ""), ("fine", "_fine"), ("fx", "_fx")):
        g = b.layers[layer]
        if not g.order:
            continue
        stem = "pmx_" + new_id + suffix
        L.write_json(os.path.join(GEO, stem + ".geo.json"), g.to_json())
        a = b.anims[layer]
        if a.tracks:
            L.write_json(os.path.join(ANI, stem + ".animation.json"), a.to_json())
        names[layer] = stem
    for suffix in ("", "_fine", "_fx"):                                         # a stale file of an earlier run
        stem = "pmx_" + new_id + suffix
        layer = {"": "main", "_fine": "fine", "_fx": "fx"}[suffix]
        if layer not in names:
            for d, ext in ((GEO, ".geo.json"), (ANI, ".animation.json")):
                p = os.path.join(d, stem + ext)
                if os.path.isfile(p):
                    os.remove(p)
    look = {"skin": new_id, "slim": slim, "scale": old.get("scale", 1.0)}
    if glow is not None:
        look["glow"] = True
    for k in ("hold_r", "hold_l", "alpha", "stance", "cape"):
        if k in old:
            look[k] = old[k]
    extras = list(old.get("extras", [])) if spec.get("keep_extras", True) else []
    if spec.get("keep_bones"):                                                   # wave 2: only some bones of the old extras (props, cape), the rest is replaced by new parts
        extras = keep_old_bones(new_id, old.get("extras", []), spec["keep_bones"])
    for layer in ("main", "fine", "fx"):
        if layer in names:
            extras.append({"model": names[layer], "texture": "pmx_" + new_id, "glow": bool(layer == "fx" and has_glow)})
    look["extras"] = extras
    STATS[new_id] = dict(cubes={k: b.layers[k].cubes for k in b.layers}, quads={k: b.layers[k].quads for k in b.layers},
                         glow_quads={k: b.layers[k].glow_quads for k in b.layers}, bones={k: len(b.layers[k].bones) for k in b.layers}, atlas_tiles=len(b.atlas.tiles),
                         atlas_fill=round(float((b.atlas.img[..., 3] > 0).mean()), 3), glow=bool(glow is not None), atlas_glow=bool(has_glow))
    return new_id, look


def main(argv):
    flt = [a for a in argv if not a.startswith("-")]
    olds = old_looks()
    looks = {}
    if os.path.isfile(LOOKS_BC) and flt:
        looks = json.load(open(LOOKS_BC))["looks"]
    for spec in CH.CHARS + CH2.CHARS:
        if flt and not any(f in spec["id"] for f in flt):
            continue
        nid, look = build_one(spec, olds)
        looks[nid] = look
        s = STATS[nid]
        print("%-34s cubes %s quads %s glow_quads %s tiles %d fill %.2f" % (nid, s["cubes"], s["quads"], s["glow_quads"], s["atlas_tiles"], s["atlas_fill"]))
    import specs_npc as NPC                                      # docs/npcs_bc.md: the NPC and Midnight Sun looks (own table looks_bc_npc.json, own stats_npc.json)
    NPC.run(flt, olds, build_one, STATS)
    doc = {"version": 1, "note": "Black Clover boss player models (docs/bc_playermodels.md), written by tools/bc_playermodels/gen_bc.py. Every id is 'bc_' + the id of the look it replaces; the old looks stay in looks.json.", "looks": dict(sorted(looks.items()))}
    with open(LOOKS_BC, "w") as f:
        json.dump(doc, f, indent=1)
        f.write("\n")
    sp = os.path.join(HERE, "stats.json")
    allstats = json.load(open(sp)) if os.path.isfile(sp) else {}
    allstats.update(STATS)                                       # a partial run only refreshes its own looks
    with open(sp, "w") as f:
        json.dump(allstats, f, indent=1, sort_keys=True)


if __name__ == "__main__":
    main(sys.argv[1:])
