# The Labyrinth Guardian, its Altar, the Mana Core and the sector Gatekeeper (builder GUARD, 1.54.0)

Part of the Singular, Expanding Mega-Dungeon (`docs/labyrinth/mega.md`). This file says WHY each class, template and file of the guard builder exists, how the
state machines work, what the numbers are, and, honestly, what has never been run. Nothing here was played: the author had no game and no whole-project compiler.
What stands behind it is listed in section 9.

The sector's story in four sentences: the Guardian Altar in the middle of the Guardian's hall wakes the **Labyrinth Guardian** when a survival player comes near;
when it dies the altar forges a **Mana Core** that names the sector; the core, and nothing else, opens the sector's **Gatekeeper** (a `gatekeeper_door` of
`kind=sector`) in the hall's north wall; behind it lie the **antechamber** with its two roads (the Crucible Vault and the **Way Onward**), and the Way Onward ends in
a rubble seal where the next sector is attached.

---------------------------------------------------------------------------------------------------------------------------------------------

## 1. New, replacement, kept (rule 5)

| Part | Status |
|---|---|
| `GatekeeperDoorBlock.Kind.SECTOR` (+ two short branches), `blockstates/gatekeeper_door.json` four variants | NEW, appended; the three older kinds (warden, boss, puzzle) behave byte for byte as before |
| `FloorWardenEntity`: three protected hooks (`slamDamage`, `slamRadius`, `onGroundSlam`) and the `nusmp_servant` tag on the Armors it summons | NEW, additive; a Floor Warden slams, summons and drops exactly as before |
| Mana Core item, `SectorCore` data component, sprite, 3D in-hand model, class table and Better Combat entries | NEW |
| Labyrinth Guardian entity, Guardian Altar block / block entity / item, `GuardianScaling`, traits, config, command, self-tests | NEW |
| `arena/guardian_hall_{a,b}`, `antechamber/antechamber_a`, `onward/{straight,left,right}_{a,b}` | NEW (nothing like them existed; the Spire's Peak Altar room stays registered) |
| `WeaponRenderer.BASE_WEAPONS` | one entry appended (the Mana Core) |
| Replacements | NONE. Nothing was removed. |

---------------------------------------------------------------------------------------------------------------------------------------------

## 2. The numbers (`GuardianScaling`, `GuardConfig`)

| What | Formula | Config key (file `nusmp-labyrinth-mega-guard-server.toml`, section `[guard]`) |
|---|---|---|
| Health | `guardianBaseHealth * (1 + threatHealthScale * (threat - 1))`, the attribute stops at 1024, the rest is a damage divisor | `guardianBaseHealth` 400 (20..5000), `threatHealthScale` 0.35 |
| Melee damage | `14 * (1 + threatDamageScale * (threat - 1))`, at most 150 | `threatDamageScale` 0.12 |
| Armour | `12 + 0.75 * (threat - 1)`, at most 30 | (fixed) |
| **Size (colossal)** | `Attributes.SCALE = guardianScale * min(1.25, 1 + 0.02 * (threat - 1))`, never above 4 | `guardianScale` **1.8** (1.0..4.0) |
| Approach | a survival player this close (blocks) wakes it; the fight is abandoned when nobody is within this + 16 for `abandonSeconds` | `approachRadius` 16 (6..48), `abandonSeconds` 120 (20..1800) |
| Re-forge | minutes after a forge before a lost core may be forged again | `reforgeMinutes` 5 (0..120) |
| Switches | the nine theme traits, the leash to the hall | `traits` true, `leash` true |

`threat` is `Sector.threat` (how it is computed per road is the lead's business). The Guardian is a Floor Warden with the SCALE attribute raised, so the **hitbox**
grows by itself (`LivingEntity.getDimensions` is final and multiplies the type's 1.4 x 3.2 by the attribute: 2.5 x 5.8 at the default) and so does the **model**:
`LivingEntityRenderer.render` applies `entity.getScale()` before it calls the renderer's own `scale()` hook, where `FloorWardenRenderer` has its fixed 1.75, so
the two multiply (1.75 x 1.8). No renderer change is needed. The Ground Slam radius is `4 * scale` blocks (+ the gravity trait's bonus).

Keys other modules read (the owner's brief for the decennial boss): the scoreboard tag **`nusmp_sector_guardian`** and the persistent-data int **`nusmp_sector`**
(the sector id, -1 for a test Guardian that belongs to none). Both are written at construction, when the Guardian is configured and when it is loaded. The two older
tags stay (`nusmp_peak_boss` keeps the economy, the HP buffer and Vital Strike off it; `nusmp_mega_guardian` is ours).

**Traits** (one per theme; the two roads of 1.55.0 borrow: `radiant` has ASTRAL, `abyss` has GRAVITY). Each is five numbers (cooldown, range, power, duration, count),
the second phase (under half health) multiplies cooldown by 0.7, range by 1.2, power by 1.4 and adds to the count, and hard caps clamp the result (all tested):
GRAVITY a well that pulls players 3..10 blocks away in and slows them; BLOOD an absorption shield fed by its own hits (the brief said "lifesteal", but the boss rules
drop every heal in combat, so it is a shield, never health: a documented deviation); SPATIAL a blink behind its target (never into a wall, never out of the hall,
at most every 5 s); FLAME a fire trail of damage cells and fire immunity (no block is ever changed); FROST a slowing aura; VERDANT a one-second root through
`BalanceLaw.beginControl`; STORM chain lightning after each Ground Slam; SHADOW a darkness pulse (blindness at most 2 s, only within 6 blocks); ASTRAL a ring of
falling stars (damage only, no push).

---------------------------------------------------------------------------------------------------------------------------------------------

## 3. Classes (package `labyrinth/mega/guard`) and WHY

| Class | Why it exists |
|---|---|
| `GuardianScaling` (pure) | Every curve, the trait table, the caps and the re-forge rule in one place with no Minecraft types, so JUnit pins them and a mutation check proves the tests bite. |
| `TraitMath` (pure) | The geometry of the traits (blink spots behind a target, chain order and decay, the star ring, the fire trail's cell book) so it is testable. |
| `AltarRules` (pure) | The altar's decisions (summon, finish, reset) as one function of what the block entity sees. A boss that VANISHED is a reset, never a victory: finishing forges a core and unseals a sector, which must only follow a real death. |
| `SectorDoorRules` (pure) | The five answers of the door to a core (open, dormant, not here, wrong sector, not ready). |
| `CoreHint` (pure) | The compass for the Mana Core's right-click (eight directions, rounded distance). |
| `GuardConfig` | The `[guard]` config with a `get()` that answers defaults while the spec is not loaded. |
| `SectorCore` | The data component `{sector, theme}` (codec and stream codec) that makes one core the key of one sector. |
| `ManaCoreItem` | The key: epic, foil, never despawns, invulnerable, glowing, rescued from the void by its sector's altar, tooltip in the theme's colour, a compass on right click. |
| `SectorDoor` | The whole behaviour of `kind=sector`, kept out of `GatekeeperDoorBlock`: Shadow Key refused, a core checked by `SectorDoorRules`, the 24-block slab opened by the existing flood, the core spent (not in creative), `mega.sector_cleared` fired, `MegaDungeon.unseal` called. |
| `LabyrinthGuardianEntity` | The boss: a Floor Warden that scales, has a theme trait, obeys the boss rules (`BudgetedBoss`, no heals), is leashed to its hall, drops nothing, is colossal and carries the two keys above. |
| `GuardianTraits` | What the nine traits do in the world; players only, no block ever changed, hard control through `BalanceLaw`. |
| `GuardianAltarBlock` / `GuardianAltarBlockEntity` | The altar and its three-state machine (below); forges the core; re-forges a lost one. |
| `GuardCommand` | `core give <player> [sector]`, `core forge`, `core guardian` (the lead attaches it under `/multiverse mega`). |
| `GuardRegistry` | Blocks, block entity, entity type and attributes, items, the data component; one game-bus handler that makes every Mana Core item entity indestructible the moment it exists. |
| `GuardLog` | Every entry point that runs inside the server's own loops (the altar's ticker and click, the Guardian's `aiStep`, the Mana Core's item-entity update and right click, the join handler, the call into `MegaDungeon.unseal`) catches `RuntimeException` and reports it here: the first three failures of each place are logged with their stack, the rest stay quiet. During a self-test `GuardLog.strict` is set and the failure is rethrown, so a swallowed error fails the test. |
| `GuardSelfTests` | The in-game self-tests (section 7). |

**The altar's state machine** (saved in NBT; polled every 10 ticks on the server):
`IDLE` (a survival player within `approachRadius` and the sector not GENERATING/FAILED -> builds the Guardian for the sector under it, `ACTIVE`) ->
`ACTIVE` (the Guardian told the altar it died, or the altar sees it dead -> `DONE`; the party stayed away for `abandonSeconds`, or the Guardian vanished without dying
-> back to `IDLE`) -> `DONE` (forged one Mana Core of the sector above itself, called `MegaDungeon.guardianDefeated`; a click with an empty hand re-forges a lost core
while the sector is still GUARDIAN_DEAD, no core of the sector exists within 64 blocks or in an online player's pack, and `reforgeMinutes` passed). The state machine never
depends on the sector's state: an altar in a sector that is not ACTIVE (an admin test, a regenerated sector) works the same way, and `guardianDefeated` itself ignores it.

**The door** (`kind=sector`): a closed slab of 3 x 4 x 2 = 24 blocks (rows 9-10 lower half, 11-12 upper half, both wall layers). Right click with a core of the sector that holds
the door and the sector not GENERATING/FAILED: the flood (`DoorFlood`, limit 64) opens all 24, the core is spent, `mega.sector_cleared` (argument: the sector id) is fired, the
sound and particles play, `MegaDungeon.unseal` raises the expansion level and asks the generator. The Shadow Key is refused with a message; a blank core, a core of another
sector and an empty hand do nothing. Redstone, pistons and explosions can do nothing: the block is unbreakable, pushes nothing and the Dungeon Guard holds the dimension.

---------------------------------------------------------------------------------------------------------------------------------------------

## 4. The Mana Core's look (the owner's rule: a real 3D model, the sprite stays the inventory icon)

`tools/labyrinth/gen_mega_items.py` draws the 16 x 16 sprite (a glowing teal-violet crystal in a ring of runes), the block textures of the sector door (a 16 x 64 animated
strip for the lower half, 16 x 16 for the upper half, both with the sector's own look), the altar textures (side, top, pillar) and every model JSON, and merges the four
`kind=sector` states into `blockstates/gatekeeper_door.json` (additions only). `tools/gen_weapon_models.py` gained `mana_core` (a lathe-turned hex crystal, a horizontal and a
vertical ring of runes, four glowing motes), appended to `RELICS` right after the hourglass; the mesh, body and glow skins are generated into `assets/nusmp/weapon_meshes`
and `textures/entity/weapon`, the 128 px sources into `tools/weapon_tex_128`. The item is class `handitem` in both copies of `weapon_classes.json` (so the size rule treats it as a
hand item), excluded from Better Combat in `tools/bettercombat/manifest.json` (regenerated by `tools/bettercombat/inventory.py`), and listed in `WeaponRenderer.BASE_WEAPONS`
(without that the held model is never drawn; `GuardAssetsTest` pins it).
Preview without the game: `python tools/item_preview/preview_weapons.py` (writes `build/item_preview/weapons.png`).

---------------------------------------------------------------------------------------------------------------------------------------------

## 5. The templates (`tools/labyrinth/gen_mega_guard.py`, helpers in `tools/labyrinth/mega_guard_lib.py`)

All nine are built with `mega_lib.MT`, deterministic (`gen_mega_guard.py --check` rebuilds every one in memory and compares bytes with the files), checked by `mega_check.py`
(0 errors, 0 warnings with stub pools; see the note on `mega_lib` below), exported by `mega_catalog.py` and rendered with `mega_preview.py`. Vanilla blocks only, except the two
the contract allows: `nusmp:guardian_altar` and `nusmp:gatekeeper_door`. Architecture uses the palette (deepslate and dark oak families) so the Region Theme restyles it.

| Template | Size | Sockets | Markers | Look |
|---|---|---|---|---|
| `arena/guardian_hall_a` "The Nave of the Sealed Reliquary" | 45 x 45 | south offset 22 `mega_door`, pool `minecraft:empty`, open; north offset 22 `mega_chain`, pool `mega/antechamber`, final state the door block, the 3 x 4 x 2 hole filled with the Gatekeeper slab | `guardian_spawn` above the altar, `gatekeeper` (facing north) in front of the slab; no spawn or patrol marker | A square nave 26 rows high ringed by twelve columns with flared capitals and a stepped arcade, lit lancet windows (stained glass over sea lanterns), a three-tier dais with the altar, a crying-obsidian rune ring, a chequerboard floor, a halo of soul lanterns, portal porches round both doorways. |
| `arena/guardian_hall_b` "The Chapter of the Eight Stars" | 45 x 45 | same as A | same as A | The same contract in an octagon (the footprint's corners cut off), eight columns on a ring joined by a lintel, octagonal rings in the floor, a stepped faceted vault rising to a lantern shaft with a hanging soul-lantern cluster. |
| `antechamber/antechamber_a` "The Hall of the Two Roads" | 27 x 27 | south offset 13 `mega_chain` (pool empty, open); east offset 13 `mega_vault` (pool `mega/vault`); west offset 13 `mega_onward` (pool `mega/onward`) | one `patrol` | A calm lobby: sigil floor, four columns and a beam grid, two armoured statues, braziers, a red wool-and-candle plaque over the Vault road and a blue one over the Way Onward. |
| `onward/straight_{a,b}` | 9 x 36 | entry south offset 4 `mega_onward`; sealed end north offset 4 `mega_door`, final state `cobbled_deepslate` | `patrol`; `expansion_socket` (facing north, w=3 h=4 d=2) directly above the end jigsaw | A 5-wide vaulted corridor. `a`: chains, lanterns, banners, a black runner, a collapsed end (rubble, patched roof, stalactites, cobwebs). `b`: an ossuary (skull niches with candles, soul lanterns, bone). |
| `onward/right_{a,b}` | 27 x 27 | entry south offset 4; end EAST offset 4 | same, facing east | The L: north along the west module column, then east along the north row. |
| `onward/left_{a,b}` | 27 x 27 | entry south offset **22**; end WEST offset 4 | same, facing west | The mirror image (the entry is on the east side so the corridor can turn west; the brief said offset 4 for all, which cannot turn west). |

The expansion marker stands in the outer layer of the seal, row 10, so that one cell is a dimple in the rubble until the generator clears the seal (a structure block is never placed).

`mega_guard_lib.py` also holds the checks a game would have done for us, run by the generator on every build (it fails the build when one fails): a flood fill that proves every room is
closed except through its declared open doorways (no gap in a wall, floor or roof; the octagon's void corners never touch the hall), that every open doorway is reachable from
inside, that the 24-block slab and its states are right, that the altar has eight free cells above it (the colossal Guardian), that the markers and their arguments are what the
Java reads, and that a clear 3 x 11 lane of floor with eight free rows exists (the trait self-test needs one).

**A bug in the shared `tools/labyrinth/mega_lib.py` (not mine to edit): its block-state table reads `nusmp:gatekeeper_door` as a wooden door** (the `.*_door` rule matches the path
`gatekeeper_door` before the `nusmp:.*` rule), so `MT.set` and `mega_check.py` reject `kind=sector`. The fix is one line at the top of `_check_cached`:
`if block.startswith("nusmp:"): return None`. Until it is applied `mega_check.py` reports the 46 Gatekeeper cells of the two arenas as errors; my generator writes them on the underlying
`Template` for that reason, and with the one-line fix in a scratch copy the checker reports 0 errors on all nine templates.

---------------------------------------------------------------------------------------------------------------------------------------------

## 6. Commands and quest keys

`/multiverse mega core give <player> [sector]` (a core of the named sector, else of the sector the player stands in, else of the newest), `core forge` (the nearest loaded altar
forges a core now, no rules), `core guardian` (a test Guardian at your feet: no altar, so no leash, no core, nothing it does can unseal a sector). Permission 2.
Quest key fired: `mega.sector_cleared` (argument: the sector id), by the door, once per opening, for real players only (`NusmpEvents.fire` ignores fake ones).

---------------------------------------------------------------------------------------------------------------------------------------------

## 7. In-game self-tests (`/multiverse mega selftest [key]`, run by the CI smoke test)

Registered from `GuardRegistry.init`, all against the REAL sector 0 the smoke test generates: `guard.registry` (everything registered, the component survives the stack and network codecs),
`guard.altar` (one arena, an antechamber, a Way Onward, the altar block and its block entity inside the arena, eight clear cells above it, the 24-block slab next to the GATEKEEPER marker,
both markers, the expansion landmark), `guard.approach` (the altar sleeps for a person outside the radius, a creative one and a spectator, and wakes the Guardian, bound to the sector and the
altar, for a survival one inside), `guard.spawn` (bound to the sector, the formula's health, the config's colossal size and a hitbox that followed it, the three tags and the sector key, a
title naming the floor, a save-and-reload that keeps all of it, the boss rules on, a blow of a million taking only a sliver), `guard.traits` (all nine traits run against a person and do what
they say), `guard.core` (a confirmed death makes exactly one core of the sector, indestructible and glowing; a second death makes none), `guard.gatekeeper` (the door refuses an empty hand, the
Shadow Key, a blank core and another sector's core, and opens to its own: 24 blocks, the core spent, the sector UNSEALED) and `guard.idempotent` (a core on an open door does nothing, a second
core on the re-closed door never unseals twice).

How they stay safe: each test snapshots what it will change (the altar's NBT, the sector's state and the expansion level, the door slab, the Expander) and restores it in a `finally`; it only
removes the entities it made; **the Expander is replaced by a recording spy while a door is clicked, so no real sector is generated** (`MegaDungeon.unseal` calls `Expander.requestNext`
synchronously inside the click, the spy records it, the real one is asked nothing; the tests accept 0 or 1 calls so a later change that defers growth until the Crucible does not break them).
What they cannot undo is the other systems' reaction to `guardianDefeated` and `unsealed`: run them on a throw-away world.

**Why the tests do not use `SelfTests.player` (the FakePlayer).** A FakePlayer is a `ServerPlayer` with no connection: the first potion effect given to it calls `connection.send` and throws, and
the traits exist to give effects. It is also in no level's player list, so nothing that scans `level.players()` (the altar's approach check, the Guardian's aggro) ever sees it. The tests use a plain
`Player` subclass (`GuardSelfTests.Dummy`: invulnerable, survival, never added to a level) and two seams that are `null` outside a test: `GuardianTraits.testPlayers` and
`GuardianAltarBlockEntity.testPlayers`. The door is simply handed the Dummy as the clicking player.

Pure tests (no game): `GuardianScalingTest`, `TraitMathTest`, `AltarRulesTest`, `SectorDoorRulesTest`, `CoreHintTest`, `GuardAssetsTest` (the resource files: door states and models, the
altar, the Mana Core's icon, mesh, skins, class table, Better Combat exclusion and the renderer list). Mutation checks done: removing the SHADOW range cap, turning the vanished-boss reset into a
victory, removing the size growth cap, and mapping `radiant` to the wrong trait were each caught by a test; so were a missing class-table entry and a missing door model.

---------------------------------------------------------------------------------------------------------------------------------------------

## 8. Integration (what the lead does; the exact lines are in the builder's report)

Config registration, the command attachment, the Guardian's renderer (`FloorWardenRenderer::new`), the creative tab ids `mana_core` and `guardian_altar`, the lang fragment, the one-line fix of
`mega_lib.py`, regenerating the pools and `src/test/resources/mega/catalog.json` after the merge (the pools are derived data: `python3 tools/labyrinth/gen_mega_pools.py` fills `arena`, `antechamber`,
`onward` and `onward_straight` / `onward_left` / `onward_right` from the folders and the `straight_` / `left_` / `right_` file-name prefixes, which `mega_pools.json` already knows; the catalog lists
every template and pool, so it must be rebuilt from the merged tree). A dry run of exactly that sequence on a scratch copy of the lead's tree with these nine templates, this package and the one-line
`mega_lib` fix gave every one of the six pools its templates and `mega_check.py` no error or warning in any of them (the only errors left were the empty vault pools, which are not this builder's).
This builder depends on three things of the 1.55.0 contract that exist on the lead's branch and not in this worktree: `FloorMap.label(int)`, `Sector.lastFloor()` (the Guardian's boss bar
names its floor, "Guardian of Floor +10") and the two appended themes; the theme code matches themes by id, so it compiles and passes with or without them in the enum.

---------------------------------------------------------------------------------------------------------------------------------------------

## 9. Not verified (honest list)

Nothing ran in the game. Checked: every Minecraft/NeoForge call against the API dump (the classes the dump does not contain are listed in the builder's report, with where the repo already calls them), the whole guard
package against API stubs with a negative control (55 errors remain in the stubs' output, every one a missing stub class or method that the repo's compiled code uses in the same way), 41 pure
tests (with and without the 1.55.0 themes in `RegionTheme`), the nine templates by the generator's own checks and by `mega_check.py`.
Never run: the altar's ticker and spawn, the colossal hitbox and model together, the entity's attributes registration, the Mana Core as an item entity (void rescue, glow), the sector door's
click path with a real `ServerPlayer`, the traits against real players (the tests use a stand-in), the in-hand model, every template as placed by the generator (the chain socket's jigsaw
final state becoming the door block, the clear of the seal, rotation of the markers' `facing`), the windows' light, the look of anything beyond what a coloured voxel preview shows, and the
whole of `GuardSelfTests` (written to be read in a CI log: a failing line says what it expected).
