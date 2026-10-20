"""Why: writes the 55 archetype looks (archetypes.py) as real files: for each one the skin `textures/entity/playermodel/dg_<id>.png` (+ `_glow`) painted from its row and weathered by its wear
recipe, the extras `pmx_dg_<id>` (wings, tails, horns, halos, capes, hats, the held weapons) cut out of the art kit's model of the row (built in memory: an archetype is never a model of the mod, only
its extras are kept) with the extras' clips borrowed from an animation class of the same build, the head parts of the parts library (ears, tusks, beards, a snout, a crown of spikes), and the
look entry `dg_<id>` of looks_dungeon.json. The Foundry's mobs do not use these entries (they have their own, foundry.py); these are the named families, the preview sheets and the set a new
roster entry or an admin can point at.
"""
import glob
import os

import common as CM
import archetypes as A
import extract_extras as EX
import foundry_class as FC
import foundry_kit as FK
import painter as P
import recipes as RC


def class_anim(build):
    """An animation class file of the same build (any of them has the follow-through bones of every extra)."""
    names = sorted(glob.glob(os.path.join(CM.ANIM, "foundry_anim_%s__*.animation.json" % build)))
    plain = os.path.join(CM.ANIM, "foundry_anim_%s__none-none__auto-auto-auto.animation.json" % build)
    if os.path.isfile(plain):
        return plain
    return names[0] if names else None


def prop_kinds(spec):
    props = spec.get("props") or {}
    return {"prop_r": (props.get("right") or {}).get("kind"), "prop_l": (props.get("left") or {}).get("kind")}


STANCE = {"goblin": "hunch", "zombie": "zombie", "ghoul": "zombie", "mummy": "zombie", "troll": "", "lizard": "hunch", "merfolk": "hunch"}


def look_for(row, with_files=True):
    name = row["id"]
    spec, img, glow, hair_style, hair_rgb, slim = P.paint(row, name, RC.recipe(row["wear"]))
    if with_files:
        os.makedirs(CM.SKINS, exist_ok=True)
        img.save(os.path.join(CM.SKINS, name + ".png"), optimize=True)
        gp = os.path.join(CM.SKINS, name + "_glow.png")
        if glow is not None:
            glow.save(gp, optimize=True)
        elif os.path.isfile(gp):
            os.remove(gp)
    R, Sn = FK.build_model(FK.build_spec(row))
    height = round(R.d.top / 16.0, 2)
    R.m.key, R.m.name = "pmx", name
    stem = "pmx_" + name
    entry = {"skin": name, "slim": bool(slim), "scale": CM.look_scale(height)}
    extras = []
    info = None
    if with_files:
        img2, glow2 = R.finish()
        img2, glow2, hh = R.cropped(img2, glow2)
        R.m.tex_h = hh
        info = EX.extract(name, out_name=stem, height=height, geo=R.m.geo(), anim_src=class_anim(row["build"]), tex_images=(img2, glow2), kinds=prop_kinds(spec), compact=True)
    elif os.path.isfile(os.path.join(CM.GEO, stem + ".geo.json")):
        info = {"holds": {}}
    if info is not None:
        extras.append({"model": stem, "texture": stem, "glow": os.path.isfile(os.path.join(CM.TEX, stem + "_glow.png"))})
        for anchor, v in (info.get("holds") or {}).items():
            entry["hold_r" if anchor == "arm_r" else "hold_l"] = v
    extras += P.part_extras(row, spec, hair_rgb)
    if glow is not None:
        entry["glow"] = True
    if hair_style and hair_style != "bald":
        entry["hair"] = hair_style
        entry["hair_color"] = CM.hexrgb(hair_rgb)
    if extras:
        entry["extras"] = extras
    st = STANCE.get(row["wear"], "")
    if st:
        entry["stance"] = st
    return name, entry


def build(only=None, with_files=True, log=print):
    out = {}
    for row in A.rows():
        if only and row["id"] not in only:
            continue
        try:
            lid, entry = look_for(row, with_files)
            out[lid] = entry
        except Exception as ex:
            import traceback
            log("FAILED %s: %r" % (row["id"], ex))
            traceback.print_exc()
    log("archetypes: %d looks" % len(out))
    return out
