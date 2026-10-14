# Multiverse of Anime x Tensura No Index (data pack `nusmp_tni_compat`)

A small data pack that joins two mods:

- Multiverse of Anime (`nusmp`)
- Tensura No Index (`tensuranoindex`, a Toaru Majutsu no Index crossover for Tensura Reincarnated)

Built against Minecraft 1.21.1 / NeoForge 21.1 (`pack_format` 48). Everything in it is optional: it only
refers to ids that are checked, tags use `"required": false`, and the loot files carry a `neoforge:mod_loaded`
condition for `tensuranoindex`. Nothing in it can make either mod fail to start. It has never been run in a
game (see "Honest status").

## Install

1. With the Multiverse of Anime jar and Tensura No Index both installed, do nothing: the jar carries this same
   pack and switches it on by itself (pack id `mod/nusmp_tni_compat`). It is not offered when Tensura No Index
   is missing.
2. Without the built-in copy (an older Multiverse jar, or you want to edit it): copy
   `multiverse_x_tensura_no_index.zip` (as it is, do not unzip) into `<world>/datapacks/`, then run `/reload`, or pick
   it under "Data Packs" when creating the world. Check with `/datapack list`.
   Use one or the other, not both: with both on, the three modifiers are listed twice and may roll twice (whether
   NeoForge removes the duplicate entries was not checked).

## What it does

| Part | File | Effect |
|---|---|---|
| Chest loot | `loot_modifiers/library_chests.json` + `loot_table/chests/tni_shelf.json` | The chests of our Library Ruins (`nusmp:chests/library_ruins`) and Grimoire Tower (`nusmp:chests/grimoire_tower`) get one extra 25% roll: Phase Dust (1-2), Name Slip or Canalis Relic. |
| Tower stone | `loot_modifiers/tower_stone.json` + `loot_table/chests/tni_stone.json` | Grimoire Tower chests also have a 3% chance of The Philosopher's Stone. |
| Boss loot | `loot_modifiers/boss_extras.json` + `loot_table/entities/boss_extras.json` | When one of our eight bosses (tag `nusmp_tni_compat:bosses`) rolls a loot table: 15% Phase Dust (1-4) or Canalis Relic, and 4% The Philosopher's Stone. |
| Loot registration | `data/neoforge/loot_modifiers/global_loot_modifiers.json` | Lists the three modifiers. NeoForge only runs a loot modifier that is listed in this file. |
| Tags | `data/tensura/tags/entity_type/no_max_ep_plunder.json` | Our eight bosses and the seven bosses of Tensura No Index are added to `tensura:no_max_ep_plunder`. |
| Tag | `data/nusmp_tni_compat/tags/entity_type/bosses.json` | The list of our bosses used by the boss loot condition. |

Our bosses: `nusmp:dante_zogratis`, `vanica_zogratis`, `zenon_zogratis`, `lucius_zogratis`, `devil_avatar`,
`goliath`, `charybdis_spawn`, `zagred`. Theirs: `tensuranoindex:coronzon`, `fuse_kazakiri`, `sasha_kruschev`,
`auriel`, `nokth`, `warden_of_the_even`, `innocentius`. Items added: `tensuranoindex:phase_dust`, `name_slip`,
`canalis_relic`, `relic_philosophers_stone` (all four exist in Tensura No Index 0.71.0).

Boss loot caveats: our bosses mostly drop through code, not loot tables. The extra roll only happens where a boss
rolls a loot table at all, and the Sentient Dungeon (and the Charybdis fight) clear boss drops inside the Dungeon.
Expect the boss extras on bosses killed outside it. Whether NeoForge applies a loot modifier to a boss that has no
loot table of its own is unverified; if it does not, only the chest loot works.

## What it does NOT do

- No Toaru skills in our codex. Tensura No Index ships `skills/codex/toaru.json` for an "Anime Skill Codex" and a boss
  Riven that draws from it. Multiverse of Anime has no data-driven skill codex and no Riven boss (the repo only has
  a Riven design note), so there is nothing to attach it to. Not done, no Java invented for it.
- No Index grimoires in loot. In that mod a grimoire is one item whose book number is a data component; a plain loot
  entry would give a blank one. Only plain stackable items are added.
- Nothing is added to the loot of their bosses: they have no loot tables (their drops are made in code).
- Nothing is added to Curios tags. Their wearables (Harmonizer Prism, Diffusion Field Dampener) are already in
  `curios:charm` and `curios:curio` through their own tags; the Philosopher's Stone is a carried relic, not a worn
  one, so it does not belong in our `dungeon_relic` slot.
- Our relics are not put into their tables: their only chest table (`tensuranoindex:chests/croce_di_pietro`) is
  for a sword that their own file never switches on.
- Mimic drops and the relic chest modifier of Multiverse of Anime are code, not data; this pack does not change them.

## Overlap with the bridge pack Tensura No Index carries

Tensura No Index 0.71.0 already ships `resourcepacks/multiverse_bridge` (pack `mod/tensuranoindex_multiverse_bridge`),
active when `nusmp` is loaded. Read against the current mod, it has these problems, found by reading its files:

1. It writes loot modifiers under `data/tni_multiverse/loot_modifiers/` but has no
   `data/neoforge/loot_modifiers/global_loot_modifiers.json`, so NeoForge never runs them. (The mod's own
   `global_loot_modifiers.json` is also an empty list, so its Croce di Pietro modifier is off too.)
2. Its boss modifier targets `nusmp:entities/riven_remake` and `nusmp:entities/marquis_remake`. Those loot tables do
   not exist in Multiverse of Anime.
3. Its chest modifier targets `nusmp:chests/library_ruins` and `nusmp:chests/grimoire_tower`, which do exist, but the
   table it adds (`tni_multiverse:chests/index_shelf`) names `tensuranoindex:church_manuscript`, an item removed
   from that mod in 0.51; a table with an unknown item fails to load.
4. Its boss tag sits in `data/tensura/tags/entity_types/`. Minecraft 1.21 reads `entity_type` (singular), so that
   tag file is ignored.
5. Its 15 Toaru codex skills have no reader on our side (see above).

This pack does not edit that pack. It replaces the working parts with its own ids (`nusmp_tni_compat:*`) and does
not list the other pack's modifiers, so nothing is rolled twice today. If the other mod later fixes its pack
(adds the global list and a valid `index_shelf`), chests would get two independent low-chance rolls; that is
harmless but you may then delete `library_chests.json` from this pack. The boss tag overlap is harmless: tags merge.

## Honest status

Written and checked by reading files only: JSON validity, ids against the unpacked Tensura No Index 0.71.0 jar and
against this repo, and a Python build check. Not run in Minecraft. Unverified: that NeoForge honours
`neoforge:conditions` in loot tables and modifiers (if it does not, the files simply load unconditionally), and
whether boss loot modifiers fire for a boss with no loot table.

Rebuild: `python tools/compat/build_compat_pack.py` (validates, then writes the zip and the in-jar copy).
