#!/usr/bin/env python3
"""Writes everything the 13 exclusive pocket dungeons are made of that is DATA, from tools/rift/pocket_roster.py:

    python3 -B tools/rift/gen_pockets.py            # writes the files
    python3 -B tools/rift/gen_pockets.py --check    # exit 1 when a file on disk differs from what the roster says

  data/nusmp/mega/pockets/<id>.json                    the pocket (look, gravity, palette, layout, traps, exclusive mobs, seal, relics ...)       read by labyrinth/mega/rift/pocket/PocketDef
  data/nusmp/mega/pockets/bosses/<boss>.json           the mega-boss: a D&D profile of the Guardian (guard/DndProfile) plus the "mega" extras   read by PocketBossExtra
  data/nusmp/worldgen/biome/pocket_<id>.json           the biome of the pocket (fog, sky, water, ambient particles, sounds, music)               applied to the pocket's cell by PocketBiomes
  data/nusmp/loot_table/pocket/<id>/{boss,treasury,secondary,chest,minor}.json
                                                       the HARD-GATED loot: every pool carries the condition nusmp:in_pocket, the relic entry is nusmp:pocket_relic
  tools/lang/rift_pockets.json                         the English of the pocket names, lore, seals, mobs, bosses (merged into en_us.json by the lead)

Why generated: thirteen tombs of the same shape are one table of choices; a typo in one file would otherwise be thirteen different bugs. The tests (PocketDataTest) read the files back.
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import pocket_roster as R  # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
DATA = os.path.join(ROOT, "src", "main", "resources", "data", "nusmp")
LANG = os.path.join(ROOT, "tools", "lang", "rift_pockets.json")


def dump(o):
    return json.dumps(o, indent=2, ensure_ascii=False) + "\n"


def hexint(h):
    return int(h.lstrip("#"), 16)


def pocket_json(p):
    return {
        "id": p["id"], "name": p["name"], "series": p["series"], "lore": p["lore"], "accent": p["accent"], "sky": p["sky"], "biome": p["biome"], "gravity": p["gravity"],
        "palette": p["palette"], "layout": p["layout"], "traps": p["traps"], "mobs": p["mobs"], "boss": p["boss"], "theme": p["theme"], "seal": p["seal"], "relics": p["relics"],
        "resonance": p["resonance"], "collapse_seconds": p["collapse_seconds"], "weight": p["weight"], "min_depth": p["min_depth"], "floor_min": p["floor_min"],
    }


def boss_json(b):
    o = {"side": b["side"], "tier": b["tier"], "variant": b["variant"], "name": b["name"], "anime": b["anime"], "dnd": b["dnd"], "forms": b["forms"], "resistances": b["resistances"],
         "legendary_points": b["legendary_points"], "legendary_actions": [{"id": a, "cost": c} for a, c in b["legendary_actions"]], "lair_actions": b["lair_actions"],
         "anti_magic_aura": b["anti_magic_aura"], "frightful_aura": b["frightful_aura"], "absorb_magic": b["absorb_magic"], "mythic": b["mythic"], "execution": b["execution"],
         "breath": b["breath"], "reflective": b["reflective"], "look": b["look"], "mega": b["mega"]}
    return o


def biome_json(p):
    b = p["biome"]
    return {
        "has_precipitation": False, "temperature": 0.5, "downfall": 0.0,
        "effects": {
            "fog_color": hexint(b["fog"]), "sky_color": hexint(b["sky"]), "water_color": hexint(b["water"]), "water_fog_color": hexint(b["water_fog"]),
            "ambient_sound": b["ambient"],
            "mood_sound": {"sound": b["mood"], "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0},
            "additions_sound": {"sound": b["additions"], "tick_chance": 0.0111},
            "music": {"sound": b["music"], "min_delay": 12000, "max_delay": 24000, "replace_current_music": False},
            "particle": {"probability": b["particle_chance"], "options": {"type": b["particle"]}},
        },
        "spawners": {"monster": [], "creature": [], "ambient": [], "axolotls": [], "underground_water_creature": [], "water_creature": [], "water_ambient": [], "misc": []},
        "spawn_costs": {}, "carvers": {}, "features": [],
    }


def cond(p, stage):
    return {"condition": "nusmp:in_pocket", "pocket": p["id"], "stage": stage}


def item(name, lo, hi, weight):
    e = {"type": "minecraft:item", "name": name, "weight": weight}
    if hi > 1:
        e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return e


def relic_with_fallback(p, relic):
    """The relic, or when the world has it already (a unique relic alive) a handful of magic crystals instead: the table never rolls an empty hand."""
    return {"type": "minecraft:alternatives", "children": [
        {"type": "nusmp:pocket_relic", "relic": relic, "pocket": p["id"]},
        item("tensura:high_quality_magic_crystal", 3, 5, 1)]}


def loot_boss(p):
    pools = [{"rolls": 1, "conditions": [cond(p, "boss")], "entries": [relic_with_fallback(p, r)]} for r in p["relics"]["boss"]]
    return {"type": "minecraft:chest", "pools": pools + [
        {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 3}, "conditions": [cond(p, "boss")],
         "entries": [item("tensura:magic_stone", 4, 8, 10), item("tensura:high_quality_magic_crystal", 1, 2, 4), item("tensura:mithril_ingot", 1, 3, 3), item("minecraft:amethyst_shard", 6, 12, 5),
                     item("minecraft:diamond", 1, 3, 4), item("minecraft:gold_block", 1, 2, 3)]}]}


def loot_treasury(p):
    return {"type": "minecraft:chest", "pools": [
        {"rolls": 1, "conditions": [cond(p, "treasury")], "entries": [relic_with_fallback(p, p["relics"]["primary"])]}]}


def loot_secondary(p):
    return {"type": "minecraft:chest", "pools": [
        {"rolls": 1, "conditions": [cond(p, "treasury")],
         "entries": [item("nusmp:magic_tool_sword", 1, 1, 4), item("nusmp:magic_tool_bow", 1, 1, 3), item("nusmp:magic_tool_spear", 1, 1, 3)]}]}


def loot_chest(p):
    return {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 5}, "conditions": [cond(p, "treasury")],
         "entries": [item("tensura:magic_stone", 4, 9, 12), item("tensura:high_quality_magic_crystal", 1, 3, 6), item("tensura:mithril_ingot", 1, 4, 5), item("tensura:hihiirokane_ingot", 1, 2, 2),
                     item("minecraft:diamond", 2, 5, 6), item("minecraft:emerald", 3, 8, 6), item("minecraft:gold_block", 1, 3, 5), item("minecraft:netherite_scrap", 1, 2, 2),
                     item("minecraft:amethyst_shard", 6, 14, 6)]}]}


def loot_minor(p):
    return {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 4}, "conditions": [cond(p, "any")],
         "entries": [item("tensura:magic_stone", 2, 5, 12), item("tensura:low_quality_magic_crystal", 1, 3, 8), item("tensura:medium_quality_magic_crystal", 1, 2, 4),
                     item("minecraft:gold_ingot", 3, 8, 8), item("minecraft:emerald", 2, 5, 6), item("minecraft:amethyst_shard", 4, 9, 6), item("minecraft:enchanted_book", 1, 1, 3)]}]}


def files():
    out = {}
    for p in R.POCKETS:
        out["src/main/resources/data/nusmp/mega/pockets/%s.json" % p["id"]] = dump(pocket_json(p))
        out["src/main/resources/data/nusmp/worldgen/biome/pocket_%s.json" % p["id"]] = dump(biome_json(p))
        base = "src/main/resources/data/nusmp/loot_table/pocket/%s/" % p["id"]
        out[base + "boss.json"] = dump(loot_boss(p))
        out[base + "treasury.json"] = dump(loot_treasury(p))
        out[base + "secondary.json"] = dump(loot_secondary(p))
        out[base + "chest.json"] = dump(loot_chest(p))
        out[base + "minor.json"] = dump(loot_minor(p))
    for bid, b in R.BOSSES.items():
        out["src/main/resources/data/nusmp/mega/pockets/bosses/%s.json" % bid] = dump(boss_json(b))
    return out


def lang():
    m = {}
    for p in R.POCKETS:
        k = "pocket.nusmp." + p["id"]
        m[k + ".name"] = p["name"]
        m[k + ".lore"] = p["lore"]
        m[k + ".seal"] = p["seal"]["label"]
        m[k + ".sigil"] = p["seal"]["sigil"]
        for mob in p["mobs"]:
            m[k + ".mob." + mob["id"]] = mob["name"]
        b = R.BOSSES[p["boss"]]
        m[k + ".boss"] = b["name"]
        m[k + ".boss.intro"] = b["mega"]["intro"]
        m[k + ".boss.death"] = b["mega"]["death"]
    return m


def main(check):
    bad = 0
    for rel, text in sorted(files().items()):
        path = os.path.join(ROOT, rel)
        cur = None
        if os.path.exists(path):
            with open(path, "r", encoding="utf-8") as f:
                cur = f.read()
        if cur == text:
            continue
        if check:
            print("STALE", rel)
            bad += 1
            continue
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write(text)
    # the lang fragment: merged with whatever the other generator of this feature (gen_pocket_relics.py) wrote under its own keys
    frag = {}
    if os.path.exists(LANG):
        with open(LANG, "r", encoding="utf-8") as f:
            frag = json.load(f)
    frag.update(lang())
    text = dump(dict(sorted(frag.items())))
    cur = open(LANG, "r", encoding="utf-8").read() if os.path.exists(LANG) else None
    if cur != text:
        if check:
            print("STALE", os.path.relpath(LANG, ROOT))
            bad += 1
        else:
            os.makedirs(os.path.dirname(LANG), exist_ok=True)
            with open(LANG, "w", encoding="utf-8", newline="\n") as f:
                f.write(text)
    if check:
        sys.exit(1 if bad else 0)
    print("wrote %d pockets, %d bosses" % (len(R.POCKETS), len(R.BOSSES)))


if __name__ == "__main__":
    main("--check" in sys.argv)
