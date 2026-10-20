# Mega-rare rifts and the exclusive pocket tombs (1.99.0)

Owner brief: rifts that are rarely, violently rare tears of space with a skybox core; the rarest lead to EXCLUSIVE instanced pocket dungeons (own biome, own mobs, a bespoke mega-boss, relics that drop nowhere else),
in the canon of Black Clover dungeons: ancient tombs that appear without warning, a small tower outside and a vast space inside, rivers on ceilings and rooms without gravity, elemental traps that fire on contact,
a treasury door sealed with condensed mana (a magic, a key, or sheer force), Magic Tools and lost ancient magic inside, a dungeon that collapses when its primary artifact is taken, and a race of kingdoms for it.

## 1. NEW / REPLACED / KEPT

| Part | Status | Where |
|---|---|---|
| Rarity table (common / rare / mega-rare), depth + Luck + per-player pity | NEW | `labyrinth/mega/rift/pocket/RiftTier`, `RiftTiers`, hook in `RiftService.rollNow` |
| 13 pocket tombs as data (biome, gravity, palette, layout knobs, traps, 4-5 mobs, boss, seal, relics) | NEW | `data/nusmp/mega/pockets/*.json` (+ `bosses/`), made by `tools/rift/gen_pockets.py` from `pocket_roster.py` |
| Datapack biome per tomb | NEW | `data/nusmp/worldgen/biome/pocket_<id>.json` |
| Seeded layout generator, build plan, collapse, traps, gravity, seal, mobs, boss bridge | NEW | `labyrinth/mega/rift/pocket/*`, `labyrinth/mega/guard/PocketBossBridge` |
| Instancing | REUSED + EXTENDED | the pocket field of `PocketRealm` (192-block cells, 64 slots); exclusive tombs add fields to `Pocket` and a cap (`[rift.mega] maxTombs`, 4) |
| Old pockets (boss / puzzle), shortcuts, return and exit rifts, `PocketRealm.leaveForRescue` | KEPT | unchanged behaviour |
| Mega tear renderer | NEW (additive) | `client/labyrinth/rift/MegaRiftRenderer`, `TearGeometry`; `DimensionalRiftRenderer` hands over for exclusive tears only |
| Textures | NEW | `tools/gen_rift_textures.py` (reproducible, tileable) |
| 13 boss weapons (wave 78) and 13 primary artifacts (wave 79) | NEW | `relics/wave/Wave78*`, `Wave79`; `tools/rift/pocket_weapon_*.py`, `tools/relics/relic_rows_pockets.py` |
| Facade | NEW | `labyrinth/mega/rift/RiftAdmin` |
| Self-tests `rift.pockets`, `rift.mega_rare` | NEW | `pocket/PocketSelfTests` |

Nothing existing was replaced or removed.

## 2. How a tomb works

* **Rift tier.** For a rift that is going to open (the old shortcut/pocket chances are unchanged) the chance of MEGA-RARE is `1 / oneIn` (default 250) times a depth factor (up to `1 + depthBoost` at floor 1000) times a Luck factor
  (0.5 to 3), then a per-player PITY: the count of rifts that opened near the player since their last mega-rare one; from `pityStart` (150) the chance climbs on a square curve and at `pityHard` (600) it is certain. The counter moves only when a
  rift really opened and survives death. `/multiverse mega rift tier [player]` shows it, `rift pity <player> <n>` sets it.
* **The tear.** A mega-rare tear is a POCKET rift that carries its tomb's id and colours (accent, sky). It stays 10 minutes; after it opened a tomb it stays 3 more minutes for the rest of the party, then closes (one tear, one tomb).
  It is announced to delvers within 48 blocks and (config) to the whole server (floor only, no coordinates).
* **Instancing.** Slot `n` of the pocket field is a 192 x 192 cell in the Mega-Dungeon dimension; the tomb (up to 168 blocks across, 13 to 26 rooms on a 5 or 7 grid) is built there in slices (entry hall first and whole, then
  2,500 blocks a tick, doorways last, then decoration, the biome of each chunk, the pedestals and chests) with chunk tickets that expire by themselves. The biome is a datapack biome applied per chunk (a new dimension cannot be made at run time on NeoForge 1.21).
  An empty tomb is cleared after 2 minutes (150 s grace); a tomb lives `tombMinutes` (25). Nobody is locked in: the entry room has the exit rift, the way home is remembered, a safe-landing search puts you down, and anyone falling out
  of the world is put back in the entry hall. Death: items stay with the player (`keepItemsOnDeath`) and they respawn at the place they came in from. `PocketRealm.leaveForRescue` / `/dungeon unstuck` work as before.
* **Canon rules.** Inverted-gravity rooms (walk on the ceiling, entered through tall corridors, a landing tile and a fall cushion), weightless rooms, rivers on ceilings (glass under water), elemental trap rooms (12 elements; contact trigger, a share of
  max health, capped at 30%, never lethal, 3 s cooldown per tile; no hunger/exhaustion anywhere). Rooms wake when first entered. The primary relic's removal starts the collapse (below).
* **The seal** (crying obsidian, 5 x 6) opens three ways: a mage whose grimoire magic is on the tomb's list unravels it (right click); the tomb's Sigil (echo shard dropped by the key room's keeper, spent on use); or force: every fully charged
  swing adds its attack damage (at most 25% of the seal's strength per blow) and holding the button trickles. Force per tomb 2,000-ish points (`seal.force`). Ancient magic: when the seal opens, a mage inside whose magic is in the tomb's resonance
  list has a 45% chance (once per tomb) of the grimoire mastery growing by 20% of its bar, which opens the next page.
* **Mega-boss.** The Labyrinth Guardian at threat floor+8, wearing its own D&D profile (`mega/pockets/bosses/<boss>.json`: phases, legendary/lair actions, mythic life, look) plus a `mega` block (damage, size, intro/death lines, court at half
  health). `PocketBossBridge` (a new file in the guard package) sets it up and puts the profile back after a reload; a missing boss is replaced twice, then the way is opened so nobody is locked out.
* **Collapse.** Taking the primary relic starts a timer (`collapseSeconds`, 60-900). Rooms fall deepest first between 18% and 85% of it (so the way out is always through standing rooms), each warned 5 s ahead with dust and shaking; delvers in a falling room
  are carried to the entry hall with 20% of their health taken (never lethal); telegraphed stones (the Crucible's catastrophe clocks, capped at 60% of max health per person); corridors are cleared and the standing side re-walled. At the end everyone is
  thrown out and a sliced sweep (section by section, empty sections skipped) clears the cell, resets the biome and frees the slot.
* **Hard loot gate.** Two layers: the loot condition `nusmp:in_pocket {pocket, stage}` on every pool of every table under `loot_table/pocket/`, and the entry `nusmp:pocket_relic {relic, pocket}`. Both ask `PocketGate`: the roll must be inside the cell of a LIVE
  tomb with that id, the roller in the same cell, the boss dead for the `boss` stage, the seal open for the `treasury` stage. Unique relics go through the God of Order's ledger; a copy alive elsewhere falls back to magic crystals. The 26 relics are also
  kept out of every random relic roll (`RelicLoot.candidates`, `PocketRelics`) and are not Djinn vessels.

## 3. The 13 tombs and their relics (26 relics, 2 per tomb)

| Tomb | Boss weapon (wave 78) | Primary artifact (wave 79) |
|---|---|---|
| ember_sanctum | Ignareth's Forge Hammer (Legend) | Ashen Forgeheart (Legend) |
| verdant_reliquary | Thornmother's Briar Bow (Legend) | Reliquary Seedheart (Legend) |
| frozen_cathedral | Rimeheart Greatblade (God) | Silent Winter Mantle (God) |
| drowned_observatory | Deepwatch Harpoon (God) | Orrery of Drowned Hours (God) |
| stormspire_aerie | Valkarin's Stormlance (God) | Aerie Courier Gloves (God) |
| gilded_crypt | Anhetep's Sunscourge (Legend) | Sun-Eater's Locket (Legend) |
| umbral_catacombs | Nekrathul's Pact Scythe (Genesis) | Pactbreaker Shroud (God) |
| starfall_archive | Oriel's Starfall Staff (God) | Archivist's Starbound Codex (God) |
| crimson_abattoir | Sanguine Warden's Cleaver (God) | Bloodbound Gauntlets (God) |
| clockwork_vault | Chronarch's Pendulum Sabre (God) | Vaultmaker's Pocketwatch (God) |
| mirrored_halls | Reflected King's Mirrorblade (God) | Silvered Sash of Reflections (God) |
| radiant_basilica | Auriel's Seraph Halberd (God) | Basilica Halo (God) |
| venom_hive | Brood-Queen's Stinger (Legend) | Hivemind Carapace Pendant (Legend) |

Each tomb has 4-5 exclusive mobs (Mob Foundry bodies with a vanilla fallback, no new entity types; one tier above the floor of the rift), a secondary pedestal and chests (Tensura crystals, mithril, minerals). The weapons are real 3D models in hand
(`tools/rift/pocket_weapon_models.py`, the kit of the 1.97.0 weapons; the Genesis scythe carries void parts), with a skin, a glow skin, a 16 x 16 sprite, Better Combat files, display presets, Tensura engravings (relic engravings, the same marker as
`WeaponEngravings`) and two acts each (the shapes, balance law and safe-cast guard of wave 71).
**Cut:** the brief allowed about 30 relics; this has 26 (2 per tomb). A third relic per tomb was cut for time; the secondary pedestal's loot (Magic Tools in the form of crystals and minerals) and the grimoire resonance stand in.

## 4. Numbers and config (`[rift.mega]`)
oneIn 250 (min 20), pityStart 150, pityHard 600, depthBoost 1.0, luckStep 0.08, riftSeconds 600, tombMinutes 25, maxTombs 4, buildPerTick 2500, broadcast, keepItemsOnDeath, traps, gravityZones.
Renderer: at most 1,536 vertices a frame (`TearGeometry.BUDGET`; the full level uses 1,484), levels of detail under 18 / 40 / 84 blocks, nothing beyond; the mesh is cached by seed (24 entries).

## 5. Sound ids for the audio builder (soft strings, vanilla fallback until real)
`nusmp:rift.crackle` (the tear's ambient crackle, played by the rift's client tick) and `nusmp:pocket.<tomb>.ambient` per tomb (13). Biome JSONs use vanilla ids only (an unknown id in a biome would break world load).

## 6. Admin
`/multiverse mega rift open pocket mega [id]`, `rift pocket types | make <id> | seal <slot> | fall <slot> | list`, `rift tier`, `rift pity`. Facade `RiftAdmin`: `spawnRift(level, at, pocket)`, `pocketIds()`, `enter(player, id)`, `collapseAll(level)`.
Self-tests: `rift.pockets`, `rift.mega_rare`. Lang fragment: `tools/lang/rift_pockets.json` (the lead merges it; the generated block of `en_us.json` is untouched, so `gen_relic_waves.py --check` reports the lang block stale until the merge).

## 7. Notes on shared files (append-only)
`RiftService.rollNow` (overload with a mega flag, the old 3-argument form delegates), `RiftRegistry` (init/register one-liners), `PocketRealm` (new fields, widened visibility of `move/returnSpot/leave/playersIn`, routes for exclusive tombs),
`DimensionalRiftBlockEntity` (pocketId/accent/sky, `aimExclusive`, `extendLife`, `limitLife`, crackle), `DimensionalRiftRenderer` (hand-over), `RiftCommand` (new subcommands), `RiftConfig` (`[rift.mega]`), `RelicLoot.candidates` (one line),
`gen_relic_waves.py` (one BATCHES entry), `weapon_classes.json` x2, `relic_themes.json`, `display_presets_manifest.json`, `bettercombat/manifest.json`, Curios tags (generated), `WaypointService.say` and `RiftService.title` made public.
`CollapseManager` no longer exists in the code base (pruned), so the collapse is its own runner that reuses `CatastropheRules`.

## 8. What could not be verified (nothing here was run in a game)
* The whole runtime (building, gravity zones, traps, seal, boss, collapse, sweep, loot gate) is type-checked against API stubs and covered by pure JUnit tests (layout, plan flood fill, rules, tiers, data, gate arithmetic, tear geometry) and by the in-game
  self-tests `rift.pockets` / `rift.mega_rare`, which have NOT been run. Expect first-run fixes.
* The ceiling-walking feel (attribute-based gravity), the biome per chunk applied to a flat dimension, the weapon models and sprites, the tear's look and the parallax core are unseen. The shimmer is a scrolling ripple, not a screen-space refraction.
* Mob outline colours (`glow`) are data only; elites glow white. A mob's `role` is informational.
* A crash in the middle of a sweep after the record is lost can leave stray blocks in a cell (the next tomb in it builds over them).
* The Sigil is a vanilla echo shard with custom data.
* Follow-ups not done: a third relic per tomb, a Genesis-grade look pass on the other weapons, real audio, a shader refraction pass.
