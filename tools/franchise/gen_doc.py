#!/usr/bin/env python3
"""Writes docs/items/franchise_engravings.md from the engraving table (tools/franchise/gen_engravings.py) and the item map
(src/main/resources/data/nusmp/relics/franchise_map.json). Re-run it after either changes: python3 -B tools/franchise/gen_doc.py"""
import glob
import importlib.util
import json
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("gen_engravings", ROOT / "tools/franchise/gen_engravings.py")
G = importlib.util.module_from_spec(spec)
spec.loader.exec_module(G)

fmap = json.loads((ROOT / "src/main/resources/data/nusmp/relics/franchise_map.json").read_text(encoding="utf-8"))
counts = fmap["franchises"]

# the mobs of the labyrinth foundry name their source work: a second place the franchises show up
foundry = Counter()
for f in glob.glob(str(ROOT / "src/main/resources/data/nusmp/mega/foundry/*.json")):
    try:
        d = json.loads(Path(f).read_text(encoding="utf-8"))
    except ValueError:
        continue
    for key in ("franchise", "series", "source", "world", "work", "origin"):
        if key in d:
            foundry[str(d[key])] += 1
            break
FOUNDRY_NAME = {"black_clover": "Black Clover", "tensura": "Tensura", "magi": "Magi", "solo_leveling": "Solo Leveling", "fairy_tail": "Fairy Tail", "shield_hero": "Shield Hero",
                "seven_deadly_sins": "Seven Deadly Sins", "rezero": "Re:Zero", "fate": "Fate", "overlord": "Overlord", "konosuba": "Konosuba", "mushoku_tensei": "Mushoku Tensei",
                "tower_of_god": "Tower of God", "fullmetal_alchemist": "Fullmetal Alchemist", "berserk": "Berserk", "jujutsu_kaisen": "Jujutsu Kaisen", "frieren": "Frieren",
                "delicious_in_dungeon": "Dungeon Meshi", "made_in_abyss": "Made in Abyss", "naruto": "Naruto", "bleach": "Bleach", "one_piece": "One Piece",
                "hunter_x_hunter": "Hunter x Hunter", "yu_gi_oh": "Yu-Gi-Oh", "dragon_ball": "Dragon Ball", "jojo": "JoJo", "exiled_heavy_knight": "Exiled Heavy Knight", "danmachi": "DanMachi", "dnd": "D&D",
                "eminence_in_shadow": "Eminence in Shadow", "sao_log_horizon": "Sword Art Online", "tomb_raider_king": "Tomb Raider King"}
dnd_foundry = sum(n for k, n in foundry.items() if k.startswith("D&D"))
known_names = {v for v in FOUNDRY_NAME.values() if v} | {"Log Horizon"}
foundry_only = sorted((k, n) for k, n in foundry.items() if not k.startswith("D&D") and k not in known_names)

lines = []
w = lines.append
w("# Franchise engravings (1.111.0)")
w("")
w("The owner's request: *\"also add custom engravements based on the animes the relic, magic and etc come from\"*.")
w("")
w("**Nothing here has been seen in a game.** It was written without a running game; the pure rules and the data are unit-tested, the handlers are type-checked and a CI self-test (`item.franchise_engravings`) exercises the real registry and a real damage event, but no engraving has been felt in play (see the last section).")
w("")
w("## What it is")
w("")
w("Tensura Reincarnated's engravings are enchantments (the tag `#tensura:engraving`: severance, barrier piercing, holy weapon ...). This release adds the mod's OWN engravings, themed by franchise: **%d franchises, %d engravings** (2 or 3 each). Every one is a real 1.21.1 datapack enchantment `nusmp:fr_<name>` (`data/nusmp/enchantment/`), listed in `data/tensura/tags/enchantment/engraving.json` (an additions tag, `replace: false`; the two older entries stay) so Tensura's engraving screens and the grindstone rules treat them like its own." % (len(G.FRANCHISES), len(G.ENGRAVINGS)))
w("")
w("What an engraving DOES is code, driven by the numbers of `data/nusmp/franchise/engravings.json`: the datapack `effects` of each enchantment are `{}` (the same shape as the two older `nusmp:` engravings). Vanilla enchantment effect components were not used on purpose: the effects are stateful (cooldowns, stacks, charges, per-player memory), cannot be tested offline, and a malformed component would fail the whole datapack load; the code path is capped, tested and cannot break a world.")
w("")
w("An item of the mod that has a franchise carries that franchise's engravings, put on once by `franchise/FranchiseEngravings` (called from the once-a-second pass of `WeaponEngravings`; own marker `NUFranchise`, so the older marker `NUEngraved` and the older engravings are untouched). Level: a relic's grade (1 up to rare, 2 unique and legend, 3 god and genesis), level 1 for every other item. Food, potions and spawn eggs are never engraved. The unified tooltip shows a line `Franchise: <name>` (in the franchise colour) under the engraving list.")
w("")
w("## The franchises, with counts")
w("")
w("Derived from: the relic waves (the class comment of a generated wave says what it is inspired by; waves 0 to 30 are the Dungeons and Dragons magic-item cards; wave 71 and the weapon rosters name a source per weapon), the item groups of the mod (Black Clover robes, grimoires and swords, the Magi Djinn, the Fairy Tail keys and Requip, the Shield Hero weapons), the labyrinth foundry (mob roster; it names a work per mob), the loot roster (`data/nusmp/loot/roster.json`: dungeon materials and food, no franchise) and the magic enum (all 105 `MagicType` entries are Black Clover attributes).")
w("")
w("| franchise | mapped items | foundry mobs | engravings |")
w("|---|---:|---:|---|")
rev = {v: k for k, v in FOUNDRY_NAME.items() if v}
fcount = {FOUNDRY_NAME.get(f["id"], ""): foundry.get(FOUNDRY_NAME.get(f["id"], ""), 0) for f in G.FRANCHISES}
for f in G.FRANCHISES:
    n = counts.get(f["id"], 0)
    fm = foundry.get(FOUNDRY_NAME.get(f["id"], ""), 0) if FOUNDRY_NAME.get(f["id"]) != "D&D" else dnd_foundry
    engs = ", ".join(e["name"] for e in G.ENGRAVINGS if e["franchise"] == f["id"])
    w("| %s | %d | %d | %s |" % (f["name"], n, fm, engs))
w("")
w("Total mapped items: %d in %d franchises (the map is `data/nusmp/relics/franchise_map.json`)." % (len(fmap["items"]), len(counts)))
none = [f for f in G.FRANCHISES if counts.get(f["id"], 0) == 0]
w("")
w("**Franchises with engravings but no mapped item yet (%d):** %s. They appear in the owner's list and in skills / the labyrinth foundry, but no relic, weapon or armour of the mod comes from them today, so their engravings exist (anvil, Tensura's engraving screen, commands) and are put on automatically the day an item is mapped (add it to `tools/relics/franchise_overrides.json` or a wave that says `inspired by <work>`, then re-run the tool)." % (len(none), ", ".join(f["name"] for f in none)))
w("")
w("**Works seen only as labyrinth mobs (no item, no engraving):** %s. Items of unknown origin get no franchise." % ", ".join("%s (%d)" % kv for kv in foundry_only))
w("")
w("Dungeons and Dragons: waves 0 to 30 are the cards of the D&D magic-item sheets (the Infinity Stones / Gauntlet of wave 30 are in that sheet too, so they count as D&D here, not as a separate franchise). The mobs of the foundry marked `D&D: <monster>` are %d in all." % dnd_foundry)
w("")
w("## Every engraving")
w("")
w("`main` = works while the item is in the main hand; `held` = either hand; `any` = held, worn as armour or worn as a Curio (re-read once a second). Percentages are a share of the hit that was going to land (never a flat number, never a share of the target's maximum health). All numbers are for level 1; level 2 is +25%, level 3 +50% on shares, chances and heals (never on a cooldown, never past a cap).")
w("")
for f in G.FRANCHISES:
    w("### %s" % f["name"])
    w("")
    w("| engraving | scope | effect |")
    w("|---|---|---|")
    for e in G.ENGRAVINGS:
        if e["franchise"] != f["id"]:
            continue
        fx = " ".join(G.effect_text(x) for x in e["effects"])
        w("| **%s** (`nusmp:%s`) | %s | %s %s |" % (e["name"], e["id"], e["scope"], e["flavour"], fx))
    w("")
w("## Balance")
w("")
w("The weapon damage is already set by `relics/GradeDamage` (normal 20-30, special 35-55, rare 60-85, unique 90-115, legend 130-175, god 190-240, genesis 260-350 per hit by weapon type). An engraving is a flavour bonus, not a second weapon:")
w("")
w("* every attack bonus is a SHARE of the hit (so it scales with the table and never makes a dagger out-hit a greatsword); all franchise attack bonuses of one hit together are capped at **15%** (server config `attackCapPercent`, 0 to 30) and count **half against a player**. A legend sword hit of 175 gains at most 26; a genesis hit of 350 at most 52.")
w("* every damage reduction (shield guard, wards) together is capped at **20%**.")
w("* every heal is at most **2** per proc; lifesteal and kill heals have their own cooldowns.")
w("* mana: a spell refund is a share of the maximum magicule, at most **5% per minute** in all; a trickle pulse (every 5 seconds) at most **0.6%**.")
w("* every chance is at most **25%** whatever the level; every proc has a cooldown (20 ticks to 3 minutes); a dodge happens at most once per 15 to 30 seconds; a second wind is once per 3 minutes and never saves a dead bearer; Equivalent Exchange / Nen Vow cost a flat 1 health and stop at 8 / 10 health, so they can never kill.")
w("* the short-lived ally (Shadow Extraction) is at most one per 45 seconds, 5% chance, 20 seconds, and shares the 6-allies-per-player limit and the driver of the relic summons (`relics/Fx.summon`).")
w("* debuffs are Slowness or Weakness for at most 5 seconds. **No engraving touches hunger, saturation or exhaustion** (`FranchiseRules.effectAllowed` refuses those effects; the data test refuses the words).")
w("* effects of mob effects are applied without particles; the proc puffs use the mod's custom particles through `particle/fx/Fx` (the franchise's theme: for a generated wave its own `relic.wave.<n>` theme), so they go through the perf budgets.")
w("")
w("## How items map")
w("")
w("`tools/relics/franchise_map.py` (deterministic, re-runnable, tolerates a def without a description, a wave without a header, an unreadable file) writes `franchise_map.json`; the order of authority:")
w("")
w("1. `tools/relics/franchise_overrides.json` (the manual layer: exact ids, id prefixes such as `charter_grimoire_` or `djinn_weapon_`, the D&D wave range, `null` to force none);")
w("2. a roster that names a weapon's source (`tools/relics/new197_roster.py`, wave 71);")
w("3. the wave: `inspired by <work>` in the class comment of a generated wave (an alias of the franchise table, whole words); the cards of waves 0 to 30 are D&D;")
w("4. for items that are no relic: the franchise whose words occur most in the item's name and lang text (a tie gives none);")
w("5. otherwise none (the pocket-tomb weapons and relics are original, the myth weapons Gungnir, Kusanagi and Gae Bolg are not an anime; consumables are never engraved).")
w("")
w("The run prints how many items each rule decided (`--report --why` lists every item with the rule). The lead re-runs it after waves 101 to 103 are merged: `python3 -B tools/relics/franchise_map.py`; `--check` exits 1 when the file on disk is out of date. A pair's left half follows its right half; at run time `FranchiseMap` also resolves by longest prefix, so a new `charter_grimoire_*` or `djinn_weapon_*` item is engraved without a re-run.")
w("")
w("| item class | gets | what the engraving does |")
w("|---|---|---|")
w("| melee weapons (swords, spears, axes, katanas, hammers, Djinn weapons ...) | its franchise's engravings | the `main` hit kinds: bonus damage by condition, combos, charges, kill stacks, lifesteal, debuffs |")
w("| grimoires, magic books, staves, keys, Charter grimoires (mapped) | the engravings as a display tag | the `held` spell kinds (`spell_refund`, `spell_power`) for a spell cast from a grimoire (see the limit below) |")
w("| armour, robes, mantles, Curios relics (mapped) | its franchise's engravings | the `any` kinds: wards, shield guard, dodge, second wind, mana trickle, ally regeneration |")
w("| shields | `shield_guard` / `ward` engravings | the reduction while blocking or holding a shield |")
w("| skill books | none: skill books have no franchise | display only |")
w("")
w("**Limit, honestly:** the spell hook is one line in `book/GrimoireBook` after a finished cast, so `spell_refund` and `spell_power` work for grimoire casts only; the Fairy Tail, Celestial, Djinn and hero-weapon casters use their own cast paths and do not call it (their engravings still act through the melee, guard and aura kinds). Spell damage is recognised as a non-melee hit whose attacker is the caster within the engraving's window (3 seconds) of a cast; a spell that damages without naming its caster (vanilla `magic` damage with no entity) cannot be told from the caster and gets no bonus.")
w("")
w("## Files")
w("")
w("* `src/main/java/com/newuniverse/nusmp/franchise/`: `FranchiseData` (table, pure), `FranchiseMap` (item map, pure), `FranchiseRules` (the numbers and caps, pure), `FranchiseEngravings` (puts them on, tooltip line), `FranchiseEvents` (handlers, per-player state keyed by UUID, cleared on logout and server stop), `FranchiseMana`, `FranchiseConfig` (SERVER config `nusmp-franchise-server.toml`: `enabled`, `allies`, `aura`, `attackCapPercent`), `FranchiseBoot`, `FranchiseSelfTests`.")
w("* data: `data/nusmp/enchantment/fr_*.json` (%d), `data/tensura/tags/enchantment/engraving.json` (additions appended), `data/nusmp/franchise/engravings.json`, `data/nusmp/relics/franchise_map.json`." % len(G.ENGRAVINGS))
w("* tools: `tools/franchise/gen_engravings.py` (the table; writes the enchantments, the tag additions, the data file and the lang fragment), `tools/franchise/gen_doc.py` (this page), `tools/relics/franchise_map.py` and `tools/relics/franchise_overrides.json`.")
w("* lang fragment: `tools/lang/franchise_engravings.json` (names, descriptions `.desc`, `franchise.nusmp.<id>`, `tooltip.nusmp.franchise`).")
w("* tests: `FranchiseRulesTest` (every effect with its caps), `FranchiseDataTest` (every json parses with the fields 1.21.1 requires, lang name and description, in the tag, every franchise of the map has 2 or 3 engravings, the numbers are inside their caps, nothing mentions hunger, the older `WeaponEngravings` entries are unchanged against a baseline taken before this release), CI self-test `item.franchise_engravings`.")
w("")
w("Append-only edits to shared files: `NUSMP.java` (one line: `FranchiseBoot.init`), `item/WeaponEngravings.java` (one line in the once-a-second pass: `FranchiseEngravings.engrave`), `book/GrimoireBook.java` (one line after `ManaLaw.onCast`: `FranchiseEvents.onSpellCast`), `client/ui/tooltip/UnifiedTooltip.java` (two lines: the franchise line), `data/tensura/tags/enchantment/engraving.json` (values appended).")
w("")
w("## NEW / REPLACED / KEPT")
w("")
w("| part | status |")
w("|---|---|")
w("| %d franchise engravings and their enchantments, tag entries, lang | **NEW** (nothing like them existed; the two older `nusmp:` engravings are a different thing) |" % len(G.ENGRAVINGS))
w("| franchise item map, its tool and overrides | **NEW** |")
w("| franchise tooltip line | **NEW** (added under the engraving list) |")
w("| `WeaponEngravings` table, `RelicEngravings`, `ArmorEngravings`, the `NUEngraved` marker, `nusmp:anti_magic`, `nusmp:legacy_of_the_wizard_kings`, every Tensura engraving on every weapon | **KEPT** unchanged |")
w("| REPLACED | nothing |")
w("")
w("## What could not be verified")
w("")
w("* Nothing was seen or felt in a game. The handlers were type-checked against API stubs and the CI self-test was not run here; the rules and the data are unit-tested only.")
w("* The datapack enchantments follow the shape of the two older `nusmp:` enchantments (known to load); the `supported_items` tag `#minecraft:enchantable/durability` and `slots: [\"any\"]` were not loaded by a real server here. If a version rejects them the fix is in `enchantment_json` of the generator.")
w("* `LivingIncomingDamageEvent` / `LivingDamageEvent.Post` are used as in NeoForge 1.21.1; whether other mods' handlers at normal priority change the amount before or after the engraving was not checked. Better Combat's attacks are assumed to arrive as `player_attack` damage; a weapon whose hits arrive as another damage type gets no melee bonus.")
w("* Day / night uses `Level.isDay()`: in a dimension with a fixed time (the Nether, perhaps the mega dimensions) it reads as night, so Sunshine never works there and Cloak of Night always does.")
w("* `Fx.summon` allies are vanilla skeletons driven by the relic-summon sweep; their behaviour was not watched.")
w("* Items worn only in a Curios slot are engraved when they sit in the inventory (as every older engraving): an item that never touched an inventory after this release stays unengraved until it does.")
w("* The map is as good as the wave headers, the roster `src` fields and the overrides: the D&D default for waves 0 to 30 is the documented origin of those cards, not checked card by card; the manual overrides (the Black Clover swords, the Fairy Tail keys, Wave 45's two works) are the author's reading of the names. Wrong ones are fixed in `franchise_overrides.json`.")
w("* %d franchises have no mapped item (above), so their engravings are untested on a real item." % len(none))
w("* Balance numbers are first estimates against the grade tables; nothing was played.")
w("")
out = ROOT / "docs/items/franchise_engravings.md"
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text("\n".join(lines) + "\n", encoding="utf-8", newline="\n")
print("wrote", out.relative_to(ROOT))
