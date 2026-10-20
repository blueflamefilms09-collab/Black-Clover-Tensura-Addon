"""Writes the data files of the functional anime loot from tools/loot/roster.py (deterministic, idempotent):

    python3 -B tools/loot/gen_data.py

  src/main/resources/data/nusmp/loot/roster.json              what the game reads (items, drop tables, values, recipes' source of truth)
  src/main/resources/data/nusmp/recipe/loot_*.json            the crafting recipes
  src/main/resources/data/nusmp/tags/item/loot/shadow_cores.json   the shard and the System's Shadow Core
  src/main/resources/data/nusmp/loot_modifiers/anime_loot.json     the global loot modifier entry
  src/main/resources/assets/nusmp/models/item/<id>.json       the flat item models of everything that is not a 3D weapon or trinket
  tools/lang/squad_loot.json                                  the English text (a flat fragment the lead merges into en_us.json)
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import roster as R  # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
RES = os.path.join(ROOT, "src", "main", "resources")
DATA = os.path.join(RES, "data", "nusmp")
ASSETS = os.path.join(RES, "assets", "nusmp")
LANG = os.path.join(ROOT, "tools", "lang", "squad_loot.json")

# the weapons and trinkets are 3D (tools/gen_loot_models.py writes their item models)
THREE_D = {w[0] for w in R.WEAPONS} | {t[0] for t in R.TRINKETS}


def dump(path, obj, indent=2):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=indent, ensure_ascii=False)
        f.write("\n")


def drops(rows):
    return [{"family": f, "chance": c, "minBand": b} for (f, c, b) in rows]


def roster():
    boss_materials = [m[0] for m in R.MATERIALS if m[4] >= 3]
    boss_foods = ["greater_mana_elixir", "starlit_nectar", "sages_elixir", "aura_tonic", "healers_vial", "bulwark_tonic", "berserkers_draught"]
    out = {
        "version": 1,
        "families": R.FAMILIES,
        "foods": [{"id": f[0], "name": f[1], "kind": f[2], "nutrition": f[3], "saturation": f[4],
                   "effects": [{"effect": e[0], "seconds": e[1], "amp": e[2]} for e in f[5]], "mana": f[6], "aura": f[7],
                   "drops": drops(f[8]), "value": f[9], "lore": f[10], "eat": f[11]} for f in R.FOODS],
        "materials": [{"id": m[0], "name": m[1], "family": m[2], "value": m[3], "tier": m[4], "drops": drops([(m[2], m[5], m[6])]), "lore": m[7]} for m in R.MATERIALS],
        "crafted": [{"id": c[0], "name": c[1], "value": c[2], "lore": c[3]} for c in R.CRAFTED],
        "ammo": [{"id": a[0], "name": a[1], "value": a[2], "lore": a[3]} for a in R.AMMO],
        "weapons": [{"id": w[0], "name": w[1], "type": w[2], "speed": w[3], "durability": w[4], "value": w[5], "primary": "attack_damage",
                     "drops": drops([(w[6], w[7], w[8])]), "lore": w[9], "engravings": [{"id": e[0], "level": e[1]} for e in R.WEAPON_ENGRAVINGS[w[0]]]} for w in R.WEAPONS],
        "armour_sets": {k: dict(v) for k, v in R.ARMOUR_SETS.items()},
        "armour": [{"id": a[0], "name": a[1], "set": a[2], "piece": a[3], "defense": a[4], "value": a[5], "primary": "armor",
                    "drops": drops([(R.ARMOUR_SETS[a[2]]["family"], R.ARMOUR_SETS[a[2]]["chance"], R.ARMOUR_SETS[a[2]]["minBand"])]), "lore": a[6]} for a in R.ARMOUR],
        "trinkets": [{"id": t[0], "name": t[1], "slot": t[2], "primary": t[3], "value": t[4], "drops": drops([(t[5], t[6], t[7])]), "lore": t[8]} for t in R.TRINKETS],
        "boss": {"materials": boss_materials, "foods": boss_foods},
        "codeUses": R.CODE_USES,
    }
    return out


def recipe_json(r):
    name, kind = r[0], r[1]

    def ing(s):
        return {"tag": s[1:]} if s.startswith("#") else {"item": s}
    if kind == "shaped":
        pattern, key, result, count = r[2], r[3], r[4], r[5]
        return {"type": "minecraft:crafting_shaped", "category": "equipment" if result.split(":")[1] in THREE_D or any(result == "nusmp:" + a[0] for a in R.ARMOUR) else "misc",
                "key": {k: ing(v) for k, v in key.items()}, "pattern": list(pattern), "result": {"id": result, "count": count}}
    ingredients, result, count = r[2], r[3], r[4]
    return {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [ing(i) for i in ingredients], "result": {"id": result, "count": count}}


def all_items():
    """(id, display name, lore) of every item of the roster, in registration order."""
    out = []
    for f in R.FOODS:
        out.append((f[0], f[1], f[10]))
    for m in R.MATERIALS:
        out.append((m[0], m[1], m[7]))
    for c in R.CRAFTED:
        out.append((c[0], c[1], c[3]))
    for a in R.AMMO:
        out.append((a[0], a[1], a[3]))
    for w in R.WEAPONS:
        out.append((w[0], w[1], w[9]))
    for a in R.ARMOUR:
        out.append((a[0], a[1], a[6]))
    for t in R.TRINKETS:
        out.append((t[0], t[1], t[8]))
    return out


LANG_EXTRA = {
    "itemGroup.nusmp.dungeon_loot": "Dungeon Loot",
    "effect.nusmp.mana_surge": "Mana Surge",
    "effect.nusmp.mana_surge.description": "Magicules come back faster.",
    # squads
    "message.nusmp.squad.formed": "Squad: %1$s",
    "message.nusmp.squad.broken.leader": "The squad's leader has fallen. %1$s still stand.",
    "message.nusmp.squad.broken.losses": "The squad is cut to pieces. %1$s still stand.",
    "message.nusmp.squad.broken.last": "Only one of the squad is left standing.",
    # gear
    "tooltip.nusmp.loot.rarity.common": "Common",
    "tooltip.nusmp.loot.rarity.uncommon": "Uncommon",
    "tooltip.nusmp.loot.rarity.rare": "Rare",
    "tooltip.nusmp.loot.rarity.epic": "Epic",
    "tooltip.nusmp.loot.rarity.legendary": "Legendary",
    "tooltip.nusmp.loot.header": "%1$s - found on floor %2$s",
    "tooltip.nusmp.loot.stat.attack_damage": "+%1$s attack damage",
    "tooltip.nusmp.loot.stat.attack_speed": "+%1$s attack speed",
    "tooltip.nusmp.loot.stat.armor": "+%1$s armor",
    "tooltip.nusmp.loot.stat.max_health": "+%1$s max health",
    "tooltip.nusmp.loot.stat.move_speed": "+%1$s%% movement speed",
    "tooltip.nusmp.loot.stat.crit_chance": "+%1$s%% critical chance",
    "tooltip.nusmp.loot.stat.mana": "+%1$s%% maximum magicules",
    "tooltip.nusmp.loot.stat.mana_regen": "+%1$s%% magicule regeneration",
    "tooltip.nusmp.loot.tempered": "Shadow-tempered",
    "tooltip.nusmp.loot.venom": "Venom-coated: %1$s strikes left",
    "tooltip.nusmp.loot.worn_in": "Worn in the %1$s slot",
    "tooltip.nusmp.loot.value": "Sells for %1$s",
    "tooltip.nusmp.loot.restores_mana": "Restores %1$s%% of your magicules",
    "tooltip.nusmp.loot.restores_aura": "Restores %1$s%% of your aura",
    "tooltip.nusmp.loot.sell_hint": "Sneak and use on a Hub Merchant to sell",
    "tooltip.nusmp.loot.damage": "%1$s attack damage (%2$s grade)",
    "tooltip.nusmp.loot.speed": "%1$s attacks per second",
    "tooltip.nusmp.loot.armor": "%1$s armor",
    "tooltip.nusmp.loot.effect": "%1$s (%2$s)",
    "tooltip.nusmp.loot.use.shadow_core_shard": "Anvil: works one extra stat line into a piece of loot gear",
    "tooltip.nusmp.loot.use.golem_core": "Anvil: fully repairs a damaged tool, weapon or armour piece",
    "tooltip.nusmp.loot.use.venom_sac": "Anvil: coats a weapon, its next twenty strikes poison",
    "tooltip.nusmp.loot.grade.normal": "normal",
    "tooltip.nusmp.loot.grade.special": "special",
    "tooltip.nusmp.loot.grade.rare": "rare",
    # selling
    "message.nusmp.loot.sold": "Sold %1$s for %2$s.",
    "message.nusmp.loot.nothing_to_sell": "The Merchant has no use for that.",
    "message.nusmp.loot.shop_off": "The Merchant is not trading.",
    # anvil
    "message.nusmp.loot.tempered": "The shard sinks into the metal. Shadow-tempered.",
    "message.nusmp.loot.repaired": "The core flares and the metal is whole again.",
    "message.nusmp.loot.coated": "The venom soaks into the edge.",
}


def lang():
    out = {}
    for iid, name, lore in all_items():
        out["item.nusmp." + iid] = name
        out["item.nusmp." + iid + ".lore"] = lore
    out.update(LANG_EXTRA)
    return out


def java_text():
    """LootText.java: the English of every tooltip and message key the code uses, so a missing lang entry still reads properly."""
    rows = sorted((k, v) for k, v in LANG_EXTRA.items() if k.startswith("tooltip.") or k.startswith("message.nusmp.loot."))
    body = ",\n".join('            Map.entry("%s", "%s")' % (k, v.replace("\\", "\\\\").replace('"', '\\"')) for k, v in rows)
    return """package com.newuniverse.nusmp.labyrinth.loot;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Map;

/**
 * GENERATED by tools/loot/gen_data.py (edit the python, not this file). Why: every line the loot says (a tooltip, a message) must be a TRANSLATABLE component, so each player
 * reads it in their own language, and must still read properly before the lang fragment is merged or when a key is missing. {@code translatableWithFallback} does both
 * if it is given the English text; this map is that text, the same one the lang fragment tools/lang/squad_loot.json is written from, so no call site repeats an English string.
 */
final class LootText {
    private LootText() {}

    static final Map<String, String> ENGLISH = Map.ofEntries(
%s);

    static MutableComponent text(String key, Object... args) {
        return Component.translatableWithFallback(key, ENGLISH.getOrDefault(key, key), args);
    }

    static String english(String key) { return ENGLISH.getOrDefault(key, key); }
}
""" % body


def main():
    java = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "labyrinth", "loot", "LootText.java")
    with open(java, "w", encoding="utf-8", newline="\n") as f:
        f.write(java_text())
    dump(os.path.join(DATA, "loot", "roster.json"), roster())
    for r in R.RECIPES:
        dump(os.path.join(DATA, "recipe", "loot_" + r[0] + ".json"), recipe_json(r))
    dump(os.path.join(DATA, "tags", "item", "loot", "shadow_cores.json"), {"values": ["nusmp:shadow_core_shard", {"id": "nusmp:shadow_core", "required": False}]})
    dump(os.path.join(DATA, "loot_modifiers", "anime_loot.json"), {"type": "nusmp:anime_loot", "conditions": []})
    n = 0
    for iid, _, _ in all_items():
        if iid in THREE_D:
            continue
        dump(os.path.join(ASSETS, "models", "item", iid + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/" + iid}})
        n += 1
    dump(LANG, lang())
    print("roster.json, %d recipes, %d flat item models, %d lang keys" % (len(R.RECIPES), n, len(lang())))


if __name__ == "__main__":
    main()
