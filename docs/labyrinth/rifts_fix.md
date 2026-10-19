# Rifts fix (1.94.0): "the hidden dungeon rifts do not work"

Owner request: "fix the hidden dungeons rifts so they work". The rifts are the Dimensional Rifts of 1.73.0 (`labyrinth/mega/rift/`, client `client/labyrinth/rift/`, relics `relics/wave/RiftActs.java`, doc `docs/labyrinth/rifts.md`):
shortcut rifts (Type B, 3 to 5 floors on) and pocket rifts (Type A, the instanced hidden realm with a secret boss or a puzzle vault). **This is a repair, not a new
system (CLAUDE.md rule 5): every old class, key, command, config value and saved field is kept, and everything below is an addition or a correction inside the
same classes.** The skill-tree rift books (`skilltree/loot/Rift*`) were read and left alone: they are fine (their loot modifier is listed in
`data/neoforge/loot_modifiers/global_loot_modifiers.json`, only counts chests inside a pocket cell, and swallows its own errors).

Nothing here could be played: no game, no Gradle. The findings come from reading the code end to end and are checked by a type check against the API stubs, the JUnit
tests below (run in the shim), and the new in-game self-test `rift.e2e`, which the CI smoke test runs. See "What was not verified" at the end.

## What was audited and found sound (no change)

* Registration: `RiftRegistry.init` (mod bus: block `nusmp:dimensional_rift`, block entity type, the self-tests) and `RiftRegistry.register` (game bus: spawner, waypoint sampler, pocket events) are
  called from `MegaSystem` (lines 33 and 53). Config `RiftConfig.SPEC` is registered in `NUSMP.java:213`, and `RiftConfig.get` answers the default while the spec is not loaded.
* Assets: blockstate `blockstates/dimensional_rift.json`, model `models/block/dimensional_rift.json` (particle texture only; the block is `RenderShape.INVISIBLE`, drawn by the renderer),
  lang `block.nusmp.dimensional_rift`, `textures/particle/glow.png` (the renderer's glow). No block item, on purpose (a rift is made by the dungeon, a relic or an admin).
* Client split: the renderer is registered only through `LabyrinthClient.init` -> `RiftClient.init` (client section of `NUSMP.java:149`); `DimensionalRiftBlockEntity` and `RiftService` touch no client
  class (the aura uses `Level.addParticle`, which is common). No custom payload is needed: the block entity's own update packet carries type, life, seed and stability.
* Loot tables of the vault (`nusmp:chests/dungeon_relic_vault`, `nusmp:chests/labyrinth_trial`) exist, have guaranteed rolls, and are listed for the rift book modifier.
* Persistence: `PocketRealm` is a `SavedData` in the overworld's storage; the rift block entity saves type, age, life, seed, destination, slot, stability.
* The pocket field (x = 4,000,000 + 192 per slot, z = 4,000,000) is far from every sector lane (`MegaGrid.LANE_Z` = 300,000, 256 parties of 4,096), inside the world border, and between y = 93 and 112 of
  the dimension's 0..127.
* Dungeon Guard: it cancels breaking, pistons, explosions, mob griefing; it does not cancel right clicks, so the plates and the chests work.

## Bugs found, with the fix and the test

Line numbers are those of the code as it was at the start of 1.94.0 (commit 6748afb).

| # | Where (old) | What was wrong | Fix | Test |
|---|---|---|---|---|
| 1 | `PocketRealm.java:148` (`r.putDouble("X", m.getX())`) and `:199` (`p.teleportTo(to, r.getDouble("X") ...)` in `leave`) | The way out put the player back **exactly where they stood going in, which is inside the tear** (the player was inside the tear's box when it took them). 3 seconds later (the tear's cooldown) the tear took them in again, as long as they stood still and the rift lived (8 minutes). "Exit and get the reward" ended in a loop. | `RETURN` also remembers the rift (`RX/RY/RZ`). `PocketRealm.returnSpot` puts the player on a ring of 2.2 blocks round the rift (`RiftRules.exitRing`: 8 points, the one on the side they came from first, each outside the tear), on a free cell with solid ground. | `RiftFixTest.everyPlaceAPlayerIsPutDownAfterAPocketIsOutsideTheTearTheyWentInBy`, `theFirstPlace...`, `aPlayerStandingExactlyInTheMiddle...`; `rift.e2e` step 6 asserts the player is not in the tear and within 4 blocks of the rift |
| 2 | `RiftSelfTests.java:45` and `:60` | `rift.block` opened its rifts at y = `POCKET_Y + 40` = 136, but the dungeon's dimension type is `min_y 0, height 128` (`data/nusmp/dimension_type/mega_dungeon.json`): `setBlock` refuses a position outside the build height and `getBlockEntity` answers null, so `RiftService.open` returned null and the test reported "did not open". The CI check that was meant to guard the rifts failed (or was ignored) on its first line. | y = `POCKET_Y + 20` (116). | `rift.block` itself; `rift.e2e` builds its site at the same height |
| 3 | `DimensionalRiftBlockEntity.java:111-122` | The block entity's tick (stability refresh, expansion queueing, and the whole entry: pocket build of about 25,000 blocks, teleports, quest events) ran with no guard. Any exception there is a "ticking block entity" crash report and a dead server, repeated on every restart while the rift lived (it is saved). | `tick` catches `RuntimeException`/`LinkageError`; `RiftService.act` catches errors of the step itself, tells the player ("the tear shudders and lets go of you"), logs once per failure, and after `RiftRules.FAIL_LIMIT` (3) failures the rift closes itself. A pocket whose build fails clears what it built. | `rift.e2e` reports a rift's last error in its failure detail |
| 4 | `PocketBuilder.java:94` (`if (g == null) return;`), `PocketEvents.java:28` (`addListener(PocketEvents::onDeath)`), `:89` | If the secret boss could not be created, or was removed by anything else than a death (the void, a command, another mod), nothing ever opened the vault: **the reward stayed behind iron bars for good**. The death handler also ran at normal priority, before listeners that may cancel the death. | `arena` opens the vault at once when no boss could be made; `PocketRealm.tick` -> `watchBoss` opens it when delvers are inside and no live boss stands in the arena; the death handler runs at LOW priority; every way of clearing (boss, plates, lost boss, admin) goes through the new `PocketRealm.clear`, which opens the bars, tells the delvers inside (action bar, System pop-up) and fires the quest event `mega.pocket_cleared`. | `rift.e2e` (the boss death is posted on the game bus; the plates are pressed in order); `RiftFixTest` for the pure parts |
| 5 | `PocketRealm.java:136-137` | A rift remembers its pocket's slot. When the pocket collapsed and the slot was given to another rift, the first rift (still open: it lives 8 minutes, a pocket 12) took players into **someone else's realm**. | The pocket is used only when its seed is the rift's seed. | `rift.e2e` (the slot of the rift and the seed of the pocket are compared); `RiftFixTest.theFirstFreeSlot...` |
| 6 | `PocketRealm.java:133` and `:171`, `RiftCommand.java:72` | The sector (polarity lock, floor, theme of the secret boss) was read from the rift's coordinates in whatever dimension the rift was in; the sectors are a map of the Mega-Dungeon's coordinates and mean nothing elsewhere. | Only read when the level is the Mega-Dungeon (`RiftService.inMega`). | by reading |
| 7 | `PocketEvents.java:49` | A plate counted only when the click hit the 2-pixel button; a click on the big coloured block under it did nothing and the delver thought the puzzle was broken. | `RiftRules.plateHit`: the button or the block under it; the result of the cancelled click is SUCCESS. | `RiftFixTest.aClickOnThePlateOrTheBlockUnderItIsAPress` |
| 8 | `RiftService.java:158`, `:165`, `:168`, `:263` | The set of open rifts held bare positions: a Hub Return Portal and a dungeon rift at the same coordinates counted as neighbours (the spawner held back), `/rift closeall` looked in every dimension for every position, the Rift Compass listed rifts of other dimensions. | Keyed by dimension and position (`openRifts(Level)`; the old no-dimension methods stay). | `rift.block`, `rift.e2e` |
| 9 | `RiftService.java:100` | **Discovery.** A new rift was announced only to the one player who was rolled for, as a line on the action bar. A rift lives four minutes; nothing else gave it away. | `RiftService.announce` (below), `beacon` (below), and right click to enter. | `rift.e2e` (the spawner part) |
| 10 | `RiftService.refreshStability` (old `:183-206`), `RiftRules.shortcutTarget` | The shortcut jumps 3 to 5 floors, but the generator builds one floor ahead of the party (`ExpansionQueue.onFloorReached`). The rift asked for the floors behind its target one at a time, drew itself dim, and lived 240 s: usually it **collapsed before its floor existed, so it was a wall**. And on a dungeon with expansion switched off it could never open. | After `RiftRules.FORM_PATIENCE_TICKS` (45 s) a rift whose target is not built **settles for the farthest floor that is** (`RiftRules.forming`, `settleTarget`) and says so; a shortcut is not even opened where nothing lies beyond and nothing will be built (`canLeadAnywhere`). The design (3 to 5, never past a boss, builds its floors) is unchanged. | `RiftFixTest.aShortcutWaitsThenSettles...`, `settlingPicksTheFarthestBuiltFloor...`, `aShortcutThatCanNeverLeadAnywhere...`; `rift.e2e` shortcut part |
| 11 | `DimensionalRiftBlockEntity.java:118` | The tear was found with `getEntitiesOfClass(ServerPlayer...)`, which a self-test cannot drive (a stand-in is not in the level's entity list), so no test had ever walked through a rift. Walking in was also the only way in. | `RiftRules.inTear` (the same box: the block, 0.45 wider, 1.7 taller, against the player's box) asked of the level's players by `RiftService.step`; a right click on the tear's outline also steps through (`DimensionalRiftBlock.useWithoutItem`). | `RiftFixTest.theTearIsTheBlock...`; `rift.e2e` |
| 12 | `PocketRealm.java:132` and `:204` | No dimension: the delver was silently not taken. No waypoint and no Hub: the exit left them standing in the cell that collapses (the void under it). | A message for the first; for the second the labyrinth's own way out (`LabyrinthTravel.leave`: origin, bed or world spawn). | by reading |
| 13 | `PocketRealm.java:219` | A cleared pocket stayed for its full 12 minutes even when everyone had left; with 64 cells that is "Every hidden realm is in use". | An empty pocket collapses 20 s after it was cleared (`RiftRules.collapseEmpty`); an uncleared one still waits 90 s. | `RiftFixTest.anEmptyPocketCollapses...` |
| 14 | `.github/workflows/smoke.yml:37` | The smoke test did not expect any `rift.` self-test, so a registry that stopped registering them would have passed. And no test walked a player through a rift. | `rift` added to `SMOKE_EXPECT`; the new `rift.e2e`. | CI |

## The discovery path (hidden is not invisible)

* **Announcement** (`RiftService.announce`), to every delver within 48 blocks of a new rift: the action-bar line, a chat line "[SYSTEM] A rift opened N blocks to the north-east. Walk into the tear, or right-click it,
  to step through; it closes in a few minutes.", the System's pop-up ("A RIFT HAS OPENED NEARBY", `SystemText.TOAST_RIFT_NEAR`), and the map mods' waypoint line (`MapShare.rift`, opt-in, rate limited, as before).
* **Beacon** (`RiftService.beacon`, every 2 seconds per open rift, only when a delver is near): a faint amethyst chime (heard within about 20 blocks; deeper for a pocket) and a rising wisp of Light/Dark Rukh
  particles sent to delvers up to 64 blocks away (a normal particle packet stops at 32), so a tear can be followed in the dark.
* The old client aura, the Rift Compass relic and `/multiverse mega rift list` are unchanged.

## Admin tooling: `/multiverse mega rift ...` (permission 2)

Old: `open <shortcut|pocket|return>`, `list`, `closeall`, `status`, `pocket list`, `pocket collapse <slot>`, `waypoint show|save|clear|return [player]`. New:

| Command | What it does |
|---|---|
| `open pocket boss` / `open pocket puzzle` | Opens a pocket rift of that kind (looks for a seed that gives it; says so if `bossShare` forbids it). Opening also announces it. |
| `list` | Now lists every loaded rift with kind, age, life left, the floors it leads between, steady or forming, pocket slot, dimension. |
| `info` | The nearest rift within 24 blocks, its seed, and its pocket (kind, floor, vault open or barred; says when its slot was given to another rift). |
| `enter [player]` | Steps a player through the nearest rift within 16 blocks at once (no cooldown, no waiting for it to open). |
| `roll [shortcut\|pocket]` | Runs the spawner's own code at your feet (natural chances with no argument, forced with one) and says why nothing opened (`crowded`, `safe_room`, `no_spot`, `dead_end`, `boss_floor` ...). |
| `why` | The spawner's gate at your feet: which condition holds (enabled, on the ground, floor, built, safe room, rifts nearby), the time to your next natural roll and the chances. |
| `pocket clear <slot>` | Skips a pocket: the vault opens as if the boss had fallen / the plates had been pressed. |
| `pocket enter <slot>` | Takes you into a pocket (you come out through its exit rift to where you stood). |

## The tests

* `src/test/java/com/newuniverse/nusmp/labyrinth/mega/rift/RiftFixTest.java` (JUnit, pure): the tear box, forming/settling, dead ends, the free slot, the spawner gate order, the exit ring (outside the tear, side they came from, behind their facing),
  plate clicks, empty-pocket collapse, the compass, the constants. `RiftRulesTest` (the old one) still passes.
* `rift.e2e` (in game, `RiftSelfTests`, run by the CI smoke test; `/multiverse mega selftest rift.e2e`). With a stand-in for the delver, for each pocket kind: open a rift, tick it 40 times, a player five blocks away is
  left alone, a player in the tear is taken (`RiftService.step`, the very code the tick runs), the realm is built with 9 bars, 3 chests and an exit rift, the player is in the antechamber with the way back remembered, the
  realm is cleared (plates in order / the boss death posted on the game bus), the bars lift, clearing twice does nothing, the three chests give loot, the exit is held back by its cooldown and then takes the
  player out **beside** the rift and not inside it, the realm collapses and frees its slot and clears its cell. Then a shortcut between two finished floors (steady jump to the arrival pad, closes when used; aimed past the last
  floor it holds the player back, then settles), and the spawner (a roll on a real floor opens a shortcut aimed forward; a second roll is `crowded`). Parts that need a finished dungeon say "skipped" in the detail line
  when this world has none (the smoke world has three sectors). Everything it builds is cleaned up in `finally`.
* The other `rift.*` self-tests are unchanged apart from the height of `rift.block`.

## Config (unchanged; for tuning)

`world/serverconfig/nusmp-labyrinth-mega-rift-server.toml`: `checkSeconds` 90, `shortcutChance` 0.18, `pocketChance` 0.06, `maxNear` 1, `riftSeconds` 240 (a pocket rift twice that), `pocketMinutes` 12. Roughly, a delver
standing on floors sees a rift every 6 minutes. Raise `pocketChance` / lower `checkSeconds` for more.

## What was not verified

* Nothing was run in a game. The chat/pop-up/chime/particle hints, the right click on the tear, the renderer (End field / crimson void window, arcs) and the feel of the exit ring are by reading only.
* That `rift.e2e` passes in the CI world. It assumes: a stand-in (fake player) can be moved with `moveTo` in its level, `Chest.unpackLootTable(player)` works with a fake player, the boss entity can be made at x = 4,000,000
  (the existing `rift.pocket.boss` test does the same), and `NeoForge.EVENT_BUS.post(new LivingDeathEvent(...))` reaches the listener. If a part of it fails, the detail line says which step.
* That the old `rift.block` test really failed on y = 136 (by reading the dimension type: build height 0..127). If the CI log of 1.73.0 shows it passing, the dimension is taller than its JSON says.
* The balance of the settle patience (45 s) and of the spawn chances.
* A real client was never connected, so what a player sees of `SystemText.TOAST_RIFT_NEAR` is the System's normal pop-up path (its text falls back to `SystemText`, so it reads even before the lang fragment is merged).

## Files

Code: `labyrinth/mega/rift/` (`DimensionalRiftBlock`, `DimensionalRiftBlockEntity`, `PocketBuilder`, `PocketEvents`, `PocketRealm`, `RiftCommand`, `RiftRules`, `RiftSelfTests`, `RiftService`), `relics/wave/RiftActs.java`
(one line: the compass reads the rifts of the player's own dimension), `labyrinth/mega/system/SystemText.java` (two pop-up texts). Test: `RiftFixTest`. CI: `.github/workflows/smoke.yml` (`rift` in `SMOKE_EXPECT`).
Lang fragment: `tools/lang/rifts_fix.json` (flat, 25 keys; merge into `en_us.json`; the code falls back to the same English before it is merged).
