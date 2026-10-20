"""Why: the dungeon's own bodies that are not Mob Foundry mobs and not archetypes: the Living Armor, the Floor Warden (the Guardian without a look is the same body, grown), the Goliath (and the
Goliath with the Gates open), and the three soul-corpse puppets Jimmy, Alfredo and Carl in their four stages of decay.
  * Living Armor, Floor Warden, Goliath: their sheets already exist (tools/arc/char_living_armor.py, char_floor_warden.py, char_goliath.py, written for the art kit): the player-model kit's
    painter paints them (the phase-1 table of characters left these three out as "belongs to the labyrinth builder"), the extras (the notched sword, the Warden's cloak and blade) are cut from
    the arc models, and the wear of the dungeon is laid on top (recipes.py: mollusk, warden, colossus, colossus_open).
  * The puppets: written here as rows in the roster's language (a lanky brawler, a barrel-chested tank, a thin hooded caster with a bandolier of bone shards: docs/exclusive/rs.md), painted once
    per decay stage (client/corpse/NamedCorpseRenderer picked one of four skins by the Soul Corpse mark's stage; the kit's body does the same by look id `corpse_<name>_s<stage>`), each stage the
    one before it and more: stitches, rot blotches, exposed flesh and bone, glowing veins and cracks.
"""
import os

import characters as CH
import common as CM
import extract_extras as EX
import gen_skins as GS
import painter as P
import recipes as RC
import roster_common as RCM

# (look id, character id, form, wear recipe)
ARC = (("living_armor", "living_armor", 1, "mollusk"), ("floor_warden", "floor_warden", 1, "warden"), ("goliath", "goliath", 1, "colossus"), ("goliath_form2", "goliath", 2, "colossus_open"))

PUPPETS = """
Jimmy | undead | lanky | mud ink bone | Soul Corpse | skin=undead hair=messy/umber face=dead/spirit/stitches top=tunic/thigh/cloth ragged sleeves=torn legs=baggy boots=bare gloves=wraps glow=spirit eglow | brute | decay
Alfredo | undead | broad | mud iron bone | Soul Corpse | skin=undead hair=bald face=dead/spirit/stitches mouth=fangs nose=wide top=bare legs=shorts boots=bare gloves=gauntlets bandage=chest+arm_l+arm_r armor=pauldrons/bone glow=spirit eglow | brute | decay
Carl | undead | slim | ink grape bone | Soul Corpse | skin=undead hair=hood/ink face=dead/spirit/stitches top=robe/knee/cloth ragged cape=hooded_cloak/ragged/hood neck=belts boots=bare gloves=wraps glow=spirit eglow nglow | caster | decay
"""
PUPPET_HEIGHT = {"jimmy": 1.95, "alfredo": 2.55, "carl": 1.9}
STAGES = 4


def puppet_rows():
    out = []
    for n, line in enumerate(PUPPETS.strip().splitlines(), 1):
        r = RCM.parse_row(line.strip(), "abyssal", 1, "puppets line %d" % n)
        out.append(r)
    return out


def hold_keys(info, entry):
    for anchor, v in ((info or {}).get("holds") or {}).items():
        entry["hold_r" if anchor == "arm_r" else "hold_l"] = v


def arc_looks(with_files=True, log=print):
    out = {}
    for lid, cid, form, recipe in ARC:
        spec = CH.normalized(cid, form)
        img, glow, hair_style, hair_rgb, slim = GS.paint_character(spec, lid, P.hair_masks(), post=P.make_post(RC.recipe(recipe)))
        entry = {"skin": lid, "slim": bool(slim), "scale": CM.look_scale(GS.height_of(spec))}
        extras = []
        info = None
        if with_files:
            img.save(os.path.join(CM.SKINS, lid + ".png"), optimize=True)
            gp = os.path.join(CM.SKINS, lid + "_glow.png")
            if glow is not None:
                glow.save(gp, optimize=True)
            elif os.path.isfile(gp):
                os.remove(gp)
            model = cid + ("_form2" if form == 2 else "")
            info = EX.extract(model, out_name="pmx_" + lid, compact=True)
        elif os.path.isfile(os.path.join(CM.GEO, "pmx_" + lid + ".geo.json")):
            info = {"holds": {}}
        if info is not None:
            extras.append({"model": "pmx_" + lid, "texture": "pmx_" + lid, "glow": os.path.isfile(os.path.join(CM.TEX, "pmx_" + lid + "_glow.png"))})
            hold_keys(info, entry)
        if glow is not None:
            entry["glow"] = True
        if hair_style and hair_style != "bald":
            entry["hair"] = hair_style
            entry["hair_color"] = CM.hexrgb(hair_rgb)
        if extras:
            entry["extras"] = extras
        out[lid] = entry
    log("arc bodies: %s" % ", ".join(out))
    return out


def puppet_looks(with_files=True, log=print):
    import foundry_kit as FK
    out = {}
    for row in puppet_rows():
        short = row["id"]
        height = PUPPET_HEIGHT[short]
        for stage in range(STAGES):
            lid = "corpse_%s_s%d" % (short, stage)
            spec, img, glow, hair_style, hair_rgb, slim = P.paint(dict(row, id=lid), lid, RC.recipe("decay_%d" % stage))
            entry = {"skin": lid, "slim": bool(slim), "scale": CM.look_scale(height)}
            extras = []
            info = None
            stem = "pmx_" + lid
            if with_files:
                img.save(os.path.join(CM.SKINS, lid + ".png"), optimize=True)
                gp = os.path.join(CM.SKINS, lid + "_glow.png")
                if glow is not None:
                    glow.save(gp, optimize=True)
                elif os.path.isfile(gp):
                    os.remove(gp)
                R, Sn = FK.build_model(FK.build_spec(dict(row, id=lid)))
                R.m.key, R.m.name = "pmx", lid
                img2, glow2 = R.finish()
                img2, glow2, hh = R.cropped(img2, glow2)
                R.m.tex_h = hh
                from archetypes_gen import class_anim, prop_kinds
                info = EX.extract(lid, out_name=stem, height=round(R.d.top / 16.0, 2), geo=R.m.geo(), anim_src=class_anim(row["build"]), tex_images=(img2, glow2), kinds=prop_kinds(spec), compact=True)
            elif os.path.isfile(os.path.join(CM.GEO, stem + ".geo.json")):
                info = {"holds": {}}
            if info is not None:
                extras.append({"model": stem, "texture": stem, "glow": os.path.isfile(os.path.join(CM.TEX, stem + "_glow.png"))})
                hold_keys(info, entry)
            if glow is not None:
                entry["glow"] = True
            if hair_style and hair_style != "bald":
                entry["hair"] = hair_style
                entry["hair_color"] = CM.hexrgb(hair_rgb)
            if extras:
                entry["extras"] = extras
            if short in ("jimmy", "alfredo"):
                entry["stance"] = "zombie"
            out[lid] = entry
    log("puppets: %d looks" % len(out))
    return out
