# The Mob Foundry (400 dungeon mobs)

Why: the owner asked "where are the custom 400 different entities, 200 for each dungeon type?". Until this work the Mega-Dungeon filled its floors from DATA tables
that name vanilla, Tensura and Ice-and-Fire mobs (about 85 ids). The Foundry adds 400 mobs that exist only in this mod: 200 for the Holy road, 200 for the
Abyssal road, 40 per tier per road (tiers 1 to 5, `DndRules.tierOf(floor)`), each one a character of the Arc art kit (`docs/arc/art_kit.md`) on the player rig.

STATUS: plan first, then the system with a small roster, then the roster in batches (every batch is committed). This file is rewritten with the real numbers at the end.

## 1. Pieces

| piece | where | what |
|---|---|---|
| roster tables | `tools/foundry/roster_holy.py`, `roster_abyssal.py` | 200 rows each: id, name, tier, faction, build, palette, kit features, stats archetype, abilities, `source` |
| generator | `tools/foundry/gen_foundry.py` | writes the kit SPEC of every mob into models (geo, texture, glow), the shared animation files, `data/nusmp/mega/foundry/<id>.json`, a report |
| guard | `tools/foundry/guard.py` (run by the generator and by `FoundryRosterTest`) | fails on a duplicate look signature, name, or (ability set + palette); on a missing or over-represented `source` |
| entity | `labyrinth/mega/foundry/FoundryMobEntity` | ONE type `nusmp:foundry_mob`; identity = synced string (pattern of `arc/ArcNpcEntity`) |
| AI | `FoundryAi` + archetype goals | brute, skirmisher, archer, caster, assassin, tank, support, summoner, flier, charger |
| abilities | `FoundryAbility`, `FoundryAbilities` (library of ~40) | pure logic (cooldowns, targets, numbers) + a thin world adapter; Light/Dark Rukh particles and sounds only |
| scaling | `FoundryScaling` | tier and floor to health, damage, armour, speed (patterns of `GuardianScaling`) |
| data | `data/nusmp/mega/foundry/*.json` | one definition per mob, read by `FoundryData` on data reload |
| integration | `FoundryEncounters` + a small hook in `EncounterDirector` / `EncounterSpawner` | a spawn of a table entry becomes a foundry mob with the config weight (default 60 %), tier of the floor, road of the sector, faction preference per theme |
| config | `FoundryConfig` (`nusmp-labyrinth-mega-foundry-server.toml`) | enabled, weight, ability switch |
| command | `/multiverse mega foundry list, spawn, count, verify` | admin |
| client | `client/labyrinth/foundry/FoundryRenderer` | reuses `ArcModels`, `ZogratisPose`, `GeoDraw`; finds the model and the SHARED animation file by identity |
| tests | `FoundrySelfTests` (in game), JUnit (pure) | roster size 200+200, ids resolve, archetype AI builds, spawn one per tier and road, stats monotone in tier |

## 2. Design decisions

* **Every mob is a kit character.** Non-human monsters use the same rig with creature parts (horns, ears, wings, tails, plates, glow). The kit has seven builds
  (slim, average, broad, huge, small, lanky, child); a mob is its build plus a palette, a face, a head feature (hair style, horns, ears, mask, hat), an outfit,
  props and accessories. The generator derives a *look signature* from the resolved spec (not from the id) and refuses two mobs with the same one.
* **Sources.** Every row has a `source`: a D&D monster or class origin, or an anime/game series. At least 30 distinct sources, none above 8 % of the roster.
  The designs and names are the mod's own, inspired by the archetype, never a copied character or text.
* **Jar size.** One Arc character is about 310 KB uncompressed (217 KB animation, 84 KB geo, 10 KB texture). Four hundred of those would be about 124 MB.
  So animation clips are SHARED. A mob does not own an animation file; it names an *animation class* (build + prop set + clip styles + a superset of
  follow-through bones). The renderer builds a `GeoSpec` from the class's animation file and the mob's own geo and texture (`GeoSpec` is a record, no engine
  change). The kit's `transform` clip is dropped (mobs do not transform), textures are written as 8-bit palette PNGs, and the generator measures every mob's
  own clips against its class animation (the report lists the worst deviation) so the sharing is checked, not assumed. The report lists the real sizes.
* **One entity, data-driven.** Stats archetype x tier x floor give the numbers; abilities are rows of a library; nothing per mob is Java.
* **The Dungeon Master is touched in a minimal way.** A foundry mob is requested by an encounter pick; existing tables keep working unchanged.
* **Faction membership** is per instance (the identity's faction), not per entity type, because there is one type. The foundry has its own small rival-seeking
  goal built on `FactionRegistry.rivals`; the eco package is not edited.

## 3. Roster shape (target)

40 mobs per tier per road, tiers 1 (floors 1-200) to 5 (801-1000). Holy: radiant hosts, knightly orders, beastfolk of the wilds, guild adventurers, fairy courts,
celestial constructs. Abyssal: demons, undead, cults, oozes, insectoids, hags, aberrations, hollow knights. Faction mapping uses the eleven contract factions
(`undead goblin ogre beast slime construct cultist demon elemental aquatic insect`).

## 4. Adding a mob

Add one row to `roster_holy.py` or `roster_abyssal.py`, run `python3 -B tools/foundry/gen_foundry.py`, read the report, commit the generated files.
(Details and the table of abilities are filled in below when the system is finished.)
