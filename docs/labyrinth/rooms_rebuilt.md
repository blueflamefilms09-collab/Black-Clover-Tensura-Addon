# Rooms rebuilt, racks retired, armour stands rare

Owner request: "keep the current dungeons size just rebuild the rooms, hall/hallways, delete the weapon racks go back to use armor stand but please don't make it a set
decoration it should be a rare one."

Three parts, one change:

1. **Rooms and hallways are rebuilt on the same footprint.** 115 shipped templates (71 halls, 14 treasure rooms, 7 junctions, 20 corridors, 3 links) get four new designs each
   (460 files under `data/nusmp/structure/mega_r/`). A server switch `gen.roomsRebuilt` (default on) picks the new set; off gives the shipped rooms.
2. **Weapon racks are no longer generated.** The block, item, block entity, renderer and loot tables stay registered (old worlds, players' own racks).
3. **Armour stands are back, as a rare prop** (about 6 percent of armoury, barracks, guard, training, vault and treasure rooms, at most 2 per floor, wearing a set of the right
   grade for the depth, breakable and lootable).

Nothing here was seen in a game. See "What was not verified" at the end.

## 1. The size is unchanged (proof)

A rebuilt room is a file with the same name as a shipped room plus `_vK`, and is only ever used as a stand-in for that shipped room. The plan (which room goes where, the stairs,
the spawn points, the safe zones, the Hub, the number of floors, the connectivity) is made from the shipped shapes and is not touched: `SectorGenerator.build` swaps only the
template id of a piece after the plan exists (`RoomVariants.variantId`). What guards it:

| Guard | Where | What it proves |
|---|---|---|
| Python contract check, per file, at generation time | `tools/labyrinth/rr_check.py` | same size; same jigsaw sockets (position, orientation, every data field); same data markers; every container, mechanism and `nusmp:` block of the shipped room at the same cell; vanilla blocks only; no row above 46; at most one armour stand |
| `RoomsRebuiltFilesTest` | JUnit | for all 460 files: size, sockets and markers equal the shipped template's; functional blocks stand where they did; no rack; stands only as the one slot; the variants differ from each other and from the shipped room; kept kinds untouched; cost |
| `RoomsRebuiltPlanTest` | JUnit | 96 real-catalog floors (5152 pieces, 2202 built from a rebuilt room): every variant has the size and sockets of the shape the plan placed |
| `RoomsRebuiltClearanceTest` | JUnit | the real `Clearance7` cut applied to every variant: 1788 doorways and 84 lane cuts, all 7 x 7, no lane cut removes anything (the one exception is the shipped fungus farm's own chest, which the shipped room has too) |
| `GeometryCheck.rebuiltRooms` | in game (`/nusmp ... verify`, `dungeon.rooms_rebuilt`) | a piece built from a variant must be a rebuilt room of its own shape with the same size; the existing box, jigsaw and marker comparison runs against the template the piece was really built from |
| `dungeon.rooms_rebuilt` | server self-test | the switch and the table, every variant against its shipped template, probe plans built with variants through `GeometryCheck`, determinism |
| `python tools/labyrinth/gen_rooms_rebuilt.py --verify` and `template.py --check` | tools | `mega_check` contract on all 460 files (0 errors) and the NBT round trip of all 706 templates |

Footprint, floor height 48, module grid 9, floors, spawn points, safe zones, the Hub, stair positions and the connectivity graph are therefore unchanged by construction and by test.

## 2. How a rebuilt room is made

The generator (`tools/labyrinth/gen_rooms_rebuilt.py`, helpers `rr_*.py`) starts from the shipped room and keeps everything that does something (sockets, markers, spawners,
chests, mechanisms, `nusmp:` blocks, and their stand-clear rings) and redraws the architecture around it, with palette blocks so the theme swap still restyles it per sector theme:

1. the old roof is stripped and a height-field roof is built (groin, barrel, dome, gable, hip, or the old height coffered), closed against the walls, with bevel stairs and ribs;
2. chains, pillars and hangers are carried up to the new roof;
3. a floor pattern (grid with medallions, runner, chequerboard, rosette, border, diagonal lattice, inlay, broken tiles);
4. pilasters on every module border, corner piers, free-standing pillars at rib crossings, galleries, coffers;
5. string courses, niche feature panels by family (lantern, banner, bookcase, slit window, tomb, shrine, grate, forge, casks), door crowns;
6. lights by scheme (chandeliers, hanging lamps, rib lamps, pilaster torches, lamp posts), balanced to a minimum light of 6;
7. wear: cracks, moss, vines, cobwebs, rubble, broken ribs.

Hallways (corridors, bends, tees, crosses, links) are drawn from scratch at their native 7 x 7 lane (rows 9 to 15 free, one-layer side walls, jigsaws and markers copied) with
a vault above: barrel with pilaster strips, groin with blind arcade, timber gable with bookcase panels, coffered flat. The `_c` ruin variants get cracks and a roof breach
(a patch of the roof raised four rows that is still closed).

Every variant goes through a retry ladder `(vault height, restraint)`: if a check says a prop narrowed a 3-wide route or lowered the light, the generator tries again with a lower
vault, then no lamp posts, no rubble, flush pilasters, no piers, until the room passes. Result of the final run: 460 variants, 0 failures.

Which template gets which variant: `RoomVariants.assign` is a seeded rotation, so the first N copies of one template in a sector get N different variants, and the same seed always
gives the same sector. A missing variant file falls back to the shipped template (logged once). The sector remembers which set it was built with (saved key `rr`, written only when
true), so a repair of an old sector rebuilds it the way it was made.

## 3. Before and after, by room type

The four variants of a family are v1 (groin vault on pillars), v2 (barrel vault with galleries and lamp posts), v3 (low coffered or dome/hip, heavier wear), v4 (a special roof:
gable, dome or hip, with a mid course). Generated from the recipes (`python tools/labyrinth/rr_doc_tables.py recipes`):

| family | v1 | v2 | v3 | v4 |
|---|---|---|---|---|
| arms | groin vault (rise up to 5); floor: chequerboard in a border; standing pilasters; pillars at the rib crossings; chandeliers; wear 8% cracks | A-frame gable roof (up to 4); floor: module grid with medallions; flush pilasters; hanging lanterns; lamp posts; wear 12% cracks, moss | old roof height, coffered; chequerboard floor; standing pilasters; coffers with glowstone lights; rib lamps; wear 16% cracks, broken ribs | gable (up to 5); floor: concentric rings; flush pilasters; pillars; chandeliers; lamp posts; wear 6% cracks, moss |
| library | groin vault (up to 5); bordered field floor; standing pilasters; chandeliers | barrel vault (up to 5); wool runner from every door; galleries with shelves; hanging lanterns; lamp posts; moss | coffered, shroomlight coffers, rib lamps, broken ribs | barrel (up to 4); cross and medallion inlay; galleries with shelves; chandeliers; lamp posts |
| sacred | groin vault; module grid with medallions; pillars; chandeliers | barrel vault; runner; galleries; hanging lanterns; lamp posts | dome over the middle (up to 6); inlay floor; standing pilasters; rib lamps; broken ribs | groin (up to 6); bordered field; pillars; galleries; chandeliers; lamp posts |
| crypt | groin vault; diagonal lattice floor; pillars; chandeliers; 18% cracks | barrel vault; broken tiles; hanging lanterns; lamp posts; moss | coffered, shroomlight coffers, rib lamps, broken ribs | groin (up to 4); module grid; pillars; chandeliers; lamp posts |
| cells | as crypt (soul lights) | as crypt | as crypt | as crypt |
| hearth | groin vault; module grid; pillars; chandeliers | barrel vault; runner; galleries; hanging lanterns; lamp posts | hip (pyramid) roof (up to 5); runner; rib lamps; broken ribs | groin (up to 4); concentric rings; pillars; chandeliers; lamp posts |
| water | groin vault; diagonal lattice; pillars; chandeliers | barrel vault; runner; hanging lanterns; lamp posts | barrel (up to 4); broken tiles; rib lamps; broken ribs | gable (up to 5); concentric rings; pillars; chandeliers; lamp posts |
| lab | groin vault; module grid; pillars; chandeliers | barrel vault; runner; hanging lanterns; lamp posts | coffered, ochre froglight coffers, rib lamps | gable; concentric rings; pillars; chandeliers; lamp posts |
| other (plazas, closets, galleries, junctions) | groin vault; module grid; pillars; chandeliers | barrel vault; runner; galleries; hanging lanterns | coffered, froglight coffers, rib lamps | gable; concentric rings; chandeliers |

"Before" for every row is the shipped room: a flat-roofed hall with a plain brick floor, bare walls with scattered props, and torches. The preview sheets in
`docs/previews/dungeon_rooms/` put the shipped room on the left and the rebuilt one on the right (isometric cutaway, floor plan and a first-person view from the first door):
`hall_armory_forge_a_v1`, `hall_barracks_a_v3`, `hall_crypt_a_v1`, `hall_great_hall_v4`, `hall_holy_church_nave_v2`, `hall_library_a_v2`, `hall_shrine_a_v3`,
`treasure_closet_a_v1`, `junction_plaza_2x2_a_v1`, `corridor_gallery_2x4_a_v2`, `corridor_straight_1x3_a_v1`, `corridor_straight_1x3_b_v3`, `corridor_straight_1x4_c_v4`,
`corridor_cross_1x1_a_v2`, `corridor_bend_1x1_b_v1`.

Which shipped rooms are in which family:

| family | rooms |
|---|---|
| arms | `hall/armory_forge_a`, `hall/armory_forge_b`, `hall/barracks_a`, `hall/barracks_b`, `hall/casino_hall_a`, `hall/dwarven_forgehold_chasm_forge`, `hall/dwarven_forgehold_hall_of_pillars`, `hall/guard_post_a`, `hall/infernal_foundry_grand_furnace`, `hall/mountain_monastery_dojo`, `hall/training_hall_a`, `treasure/dwarven_forgehold_hoard_nook`, `treasure/infernal_foundry_counting_house` |
| cells | `hall/cell_block_a`, `hall/cell_block_b`, `hall/infernal_foundry_penal_forge`, `hall/torture_chamber_a`, `hall/torture_chamber_b` |
| crypt | `hall/crypt_a`, `hall/crypt_b`, `hall/drowned_crypt_flooded_nave`, `hall/drowned_crypt_sarcophagus_vault`, `hall/graveyard_hall_a`, `hall/holy_church_crypt`, `hall/necropolis_catacombs`, `hall/necropolis_mausoleum_street`, `hall/ossuary`, `hall/vampire_castle_coffin_crypt`, `treasure/drowned_crypt_pearl_reliquary`, `treasure/necropolis_burial_vault` |
| hearth | `hall/beast_den_a`, `hall/colonnade_hall_a`, `hall/colonnade_hall_b`, `hall/feywild_grove_feast_hall`, `hall/great_hall`, `hall/hatchery_a`, `hall/haunted_manor_grand_stair`, `hall/haunted_manor_kitchen`, `hall/kitchen_larder`, `hall/mirror_hall`, `hall/sky_citadel_great_hall`, `hall/spider_warrens_cocoon_larder`, `hall/spider_warrens_web_great_hall`, `hall/vampire_castle_great_hall`, `treasure/vampire_castle_mirror_gallery` |
| lab | `hall/alchemy_lab_a`, `hall/alchemy_lab_b`, `hall/fungal_underdark_drow_enclave`, `hall/fungal_underdark_fungus_farm`, `treasure/spider_warrens_egg_cache` |
| library | `hall/cursed_archive_reading_hall`, `hall/cursed_archive_scriptorium`, `hall/cursed_archive_stacks`, `hall/library_a`, `hall/library_b`, `hall/mountain_monastery_scroll_library`, `treasure/cursed_archive_restricted_section` |
| other | `corridor/gallery_2x4_a`, `corridor/gallery_2x4_b`, `hall/feywild_grove_moonlit_glade`, `hall/sky_citadel_wind_gallery`, `junction/crossroads_hall_3x3`, `junction/plaza_2x2_a`, `junction/plaza_2x2_b`, `junction/plaza_2x2_c`, `junction/plaza_3x3_a`, `junction/plaza_3x3_b`, `junction/plaza_3x3_c`, `treasure/closet_a`, `treasure/closet_b`, `treasure/closet_c`, `treasure/feywild_grove_pixie_nook`, `treasure/sky_citadel_pantry` |
| sacred | `hall/celestial_court_banquet`, `hall/celestial_court_colonnade`, `hall/celestial_court_sundial`, `hall/celestial_court_terrace`, `hall/holy_church_cloister`, `hall/holy_church_nave`, `hall/mountain_monastery_zen_garden`, `hall/shrine_a`, `hall/shrine_b`, `hall/sunken_temple_coral_atrium`, `hall/sunken_temple_coral_nave`, `hall/sunken_temple_drowned_shrines`, `hall/temple_a`, `treasure/sunken_temple_offering_nook` |
| water | `hall/cistern_a`, `hall/cistern_b`, `hall/crystal_geode_echo_gallery`, `hall/crystal_geode_prism_atrium`, `hall/lava_hall_a`, `hall/lava_hall_b`, `treasure/crystal_geode_gem_vault` |

Not rebuilt (kept as shipped, and listed with the reason in the manifest `skipped`): `hall/dwarven_forgehold_mine_works` (rock-carved, roof is eight layers of rock),
`hall/feywild_grove_hollow_tree` (a tree, its roof is the canopy), `hall/mountain_monastery_prayer_wheel_hall` (seven-layer roof of prayer wheels), `hall/round_hall_a` and
`round_hall_b` (domed round halls, the roof is the room), `treasure/holy_church_reliquary` (irregular shell). Also kept as they were: the stairwells, safe rooms, arenas,
trial, puzzle, trap, vault, antechamber, landing, onward, cap and set kinds (they carry mechanisms or fixed encounters; the brief asked for rooms, halls and hallways).

## 4. Before and after, by hallway type

| type | before | after (four variants) |
|---|---|---|
| straight corridors (1x3, 1x4 and others) | timber-ribbed tube, torches, flat roof | barrel vault with pilaster strips; groin with blind arcade; timber gable with bookcase panels; coffered flat (the order of the four variants differs by template) |
| bends, tees, crosses | timber-ribbed tube | the same four designs on the bend, tee or cross layout |
| gallery corridors 2x4 | wide hall-like corridor | treated as a hall (family `other`: pillars, galleries, chandeliers) |
| links (stair connectors) | plain | the corridor designs, sockets and markers copied |
| ruin corridors (`_c`) | cracked tube | cracks and a roof breach (four rows higher, still closed) |

Each corridor passes: lane free in rows 9 to 15, the door apron untouched, light at least 5 on the walking route, closed shell.

## 5. What changed per theme

Themed rooms keep their theme block palette (the theme swap restyles palette blocks as before). A rebuilt themed room is drawn by the recipe of its family:

| theme | themed rooms rebuilt (family) |
|---|---|
| celestial_court | banquet, colonnade, sundial, terrace (sacred) |
| crystal_geode | echo gallery, prism atrium, gem vault (water) |
| cursed_archive | reading hall, scriptorium, stacks, restricted section (library) |
| drowned_crypt | flooded nave, sarcophagus vault, pearl reliquary (crypt) |
| dwarven_forgehold | chasm forge, hall of pillars, hoard nook (arms) |
| feywild_grove | feast hall (hearth), moonlit glade, pixie nook (other); hollow tree kept |
| fungal_underdark | drow enclave, fungus farm (lab) |
| haunted_manor | grand stair, kitchen (hearth) |
| holy_church | cloister, nave (sacred), crypt (crypt); reliquary kept |
| infernal_foundry | grand furnace, counting house (arms), penal forge (cells) |
| mountain_monastery | dojo (arms), scroll library, zen garden (sacred); prayer wheel hall kept |
| necropolis | catacombs, mausoleum street, burial vault (crypt) |
| sky_citadel | great hall (hearth), wind gallery, pantry (other) |
| spider_warrens | cocoon larder, web great hall (hearth), egg cache (lab) |
| sunken_temple | coral atrium, coral nave, drowned shrines, offering nook (sacred) |
| vampire_castle | coffin crypt (crypt), great hall, mirror gallery (hearth) |

The generic rooms (crypts, libraries, shrines, cisterns, labs, plazas, corridors) are restyled by whichever theme the sector has, as before.

## 6. NEW / REPLACED / KEPT

| | What |
|---|---|
| NEW | 460 rebuilt template files `structure/mega_r/**` and the manifest `data/nusmp/mega/rooms_rebuilt.json`; the generator tools `tools/labyrinth/rr_*.py`, `gen_rooms_rebuilt.py` |
| NEW | `RoomVariants`, `RoomsSettings` (`gen.roomsRebuilt`), `RoomsSelfTests` (`dungeon.rooms_rebuilt`), the saved flag `rr` on a sector |
| NEW | the rare armour stand: `ArmourStandRarity`, `RareStandSet`, `ArmourStandGate`, `RareStands`; config `props.armourStandRarity` and `props.armourStandFloorCap`; self-test `dungeon.props.stands` |
| NEW | tests: `RoomVariantsTest`, `RoomsRebuiltFilesTest`, `RoomsRebuiltClearanceTest`, `RoomsRebuiltPlanTest`, `RoomsConfigSourceTest`, `ArmourStandRarityTest`, `RareStandSetTest`, `NoRackPlacementTest` |
| REPLACED (in play; old kept behind a switch) | the look of the 115 rooms: with `gen.roomsRebuilt` true (default) the generator uses the new designs; false gives the shipped ones |
| REPLACED (in play; old kept behind a switch) | weapon racks in the dungeon: no longer generated. `props.rackInsteadOfArmourStand` now defaults to false and the old stand-to-rack conversion also needs the new `props.legacyRackConversion` (default false) |
| REPLACED | armour stands as set decoration: the shipped templates' stands are no longer placed (every one is cancelled at placement); a stand appears only through the rarity roll |
| KEPT | every shipped template NBT (not one file rewritten), the rack block, item, block entity, renderer, loot tables, lang and recipes, the rack converter code, `SectorPlacer.clearDecor`, the old room designs, all old data component ids, saved sectors (they stay as built) |
| KEPT | footprint, floor height, module grid, floor count, spawns, safe zones, the Hub, stairs, the connectivity graph, the stairwell, safe, arena, trial, puzzle, trap, vault, antechamber, landing, onward, cap and set templates |

## 7. Racks out, armour stands rare

* The generator no longer turns template stands into racks. With default settings no code path reaches `RackConverter.convert`. `NoRackPlacementTest` pins this
  (the default flags, the guard in `RackConverter.onJoin`, no rack in any shipped or rebuilt template, the rack block still registered). The self-test `dungeon.props.flag`
  now asserts that racks are never placed by default and that the rack block and item are still registered.
* Old keys: `props.rackInsteadOfArmourStand` (default false) and `props.legacyRackConversion` (default false, both must be true to bring the old behaviour back).
* The rare stand (`ArmourStandRarity`, decided once per floor when the placer is created, from the sector seed, no per-tick work):
  * eligible templates: halls and treasure rooms whose name has armory, barracks, guard_post, training, hoard, counting, reliquary, gem_vault, sarcophagus_vault, burial_vault,
    closet or pantry (the `stand_rooms` list in the manifest is the subset whose rebuilt variants have a stand slot);
  * each eligible room rolls once with a stable SplitMix hash: `props.armourStandRarity` percent (default 6, 0 disables, 0 to 100), at most `props.armourStandFloorCap` per floor
    (default 2, 0 to 8, hard limit 8), never two touching rooms, never a corridor;
  * 0 or 1 per room: a lit niche (or a plinth with candles on the floor) holds the one slot; a room not chosen has none, a chosen room's stand is made rare;
  * the stand is breakable (not invulnerable), not locked (all slots usable), tagged `nusmp_rare_stand`, and its drops are its gear.
* What it wears, by loot band of the floor (the same cut points as `LootBand`): floors 0 to 9 iron, rarity plain; 10 to 29 Drake-Scale, uncommon; 30 to 99 Shadowsteel, rare; 100 to 299
  Shadowsteel, epic; 300 and deeper Shadowsteel, legendary. 2 to 4 pieces, always the chest piece, made by the loot system's own roll (`GearStacks.create`).
* Numbers (from `ArmourStandRarityTest` over many seeds): about 0.41 stands per floor at 6 percent with 7 eligible rooms (cap 2); about 65 percent of such floors have none; the cap
  holds on every seed; the share of eligible rooms is within the tolerance of the setting.
* If the room is built from the shipped template instead (`gen.roomsRebuilt` false), the gate still cancels all template stands and, for a chosen room, makes the first one rare.

## 8. Cost (no lag)

* Generation per floor: the rebuilt rooms hold 2,546,405 blocks against 2,285,236 in the shipped ones (`RoomsRebuiltFilesTest`): **mean x1.114, worst single file x1.42**
  (`sunken_temple_offering_nook_v4`). By folder: corridors x0.94, halls x1.12, junctions x1.13, links x0.99, treasure x1.15. Placement time follows the block count, so the
  per-floor cost grows by about 11 percent on average, under the 25 percent allowed; no single file exceeds x1.45 (the generator rejects such a file).
* Block entities: at most 10 more per room than the shipped room (banners, skulls, barrels, a furnace), enforced in the generator and checked (`BE_CAP`).
* Entities: none added by the rooms. Stands: at most 2 per floor (hard cap 8), a single entity each, no AI, no tick work of ours.
* Per tick: nothing. The stand gate runs on entity join during placement only; `PerfSourceScanTest.theRareStandAndTheRoomVariantsCostNothingPerTick` pins that no tick or
  render hook was added.
* Planning: the stand plan is one pass over the floor's pieces (O(n), n up to about 120); the variant assignment is O(n) with one hash per piece. No new packets.
* Light: the min route light is at least 5 (target 6) in every variant; bright rooms are met more often than dark ones, see `LightRules`.

## 9. Server friendliness

Everything is decided on the server at generation time. The new Java is common/server code (no client classes): `ServerSafetyTest` passes without a new entry in
`docs/server_safe_allow.txt`. `gen.roomsRebuilt` changes gameplay (which structure is built) so it is a SERVER config in its own file `nusmp-labyrinth-mega-rooms-server.toml`;
the stand rarity and the cap are SERVER config keys of the props file. Per-sector state is keyed by the sector, per-piece state is a thread local scope set around one placement.

## 10. Tests and self-tests

* JUnit (new, 8 classes): see the table in section 6. Updated: `PerfSourceScanTest` (one appended test).
* Run in the offline shim, all green: the new classes, `PerfSourceScanTest`, `ServerSafetyTest`, `ConfigFileNamesTest`, `MapBlockColourGuardTest`, `PropsLogicTest`, `RackAssetsTest`, and the 50 or so
  `labyrinth/mega/gen` classes (Clearance7, catalog, theme, floor, sector, stair and others): 413 test methods, 0 failures.
* Self-tests (server smoke test): `dungeon.rooms_rebuilt` and `dungeon.props.stands` (plus the existing `dungeon.props` prefix, whose `flag` check was rewritten for the new default).
  **`dungeon.rooms_rebuilt` must be added to the `SMOKE_EXPECT` list of `.github/workflows/smoke.yml`; that file was not touched here.**
* Tools: `python3 -B tools/labyrinth/gen_rooms_rebuilt.py [--force|--check|--verify|--manifest|--report out.md]` (deterministic: the same code gives the same bytes), previews with
  `python3 -B tools/labyrinth/rr_previews.py`.

## 11. What was not verified

* Nothing was run in a game. No dedicated-server start, no CI smoke run, no in-game walk. The "eyes" are the offline models of the repo (the walk graph, the light model, the shell
  checks) and flat-colour previews, not game renders; lighting, textures, entity rendering and the real theme swap were not seen.
* The rebuilt rooms are transformations of the shipped rooms (roof, floor, walls and lights redrawn, everything functional kept), not hand-drawn from nothing. They are plausible in the
  previews, but taste (proportions, colour mix, how busy the walls are) needs a look in the game and some tuning of the recipes in `rr_halls.py`.
* The armour-stand path (`EntityJoinLevelEvent` cancel, the NBT edit to make a stand rare, dressing with `GearStacks.create`) was only type-checked against the API stubs and unit-tested
  as pure decisions. The template-placement entity join reaching the event with the final position is read from the engine's code, not run. If it does not fire, the template stands would
  simply stay as the shipped rooms had them (not racks, not rare).
* `saveWithoutId` / `load` calls in the gate were checked against stubs only.
* The stand slot exists in 15 of the 16 eligible shipped rooms (the rebuilt barracks_b has no free spot that keeps its 3-wide route, so it never holds a stand); some variants of other rooms use
  a free-standing plinth instead of a wall niche.
* Old sectors keep the rooms and the stands they were built with; only new or repaired sectors get the new set.
