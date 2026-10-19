# Mega-Dungeon puzzle rooms, second edition (1.94.0): 29 new puzzle types

Owner request: "way more types of puzzles" in the Mega-Dungeon (floors, sectors, rooms, trials, seals, vaults). This document (1) lists every puzzle, trial, seal, lock-and-key and
mechanism room that existed before, (2) describes the 29 new puzzles, each a real server-authoritative mechanism with an in-world hint, a safe reset, a cost for failure that never traps
anybody, a reward through the existing loot path and a scaling with party size and depth, (3) explains how the room generator now chooses among old and new puzzles by theme and depth with
weights in one data file, and (4) lists exactly what was added to existing files and what could not be verified (nothing here has been run in a game).

Code: `labyrinth/mega/puzzle/**` (pure rules in `puzzle/rules/**`), one small bridge class `labyrinth/mega/mechanics/PuzzleBridge.java`. Data: `data/nusmp/mega/puzzles/weights.json`, two room
templates `structure/mega/puzzle/gallery_a.nbt` and `gallery_b.nbt` (generator `tools/labyrinth/gen_mega_puzzle_rooms.py`). Lang fragment: `tools/lang/puzzles_v2.json` (282 lines, generated
from `PuzzleText`). Tests: `src/test/java/com/newuniverse/nusmp/labyrinth/mega/puzzle/*` (45 JUnit tests, all run). In-game self-tests: `puzzle.*` (32).

## 0. New, replacement, kept (owner rule 5)

| Part | Status | Notes |
|---|---|---|
| The 29 puzzles (`plate_sequence` ... `chain_reaction`, section 3) | NEW | Nothing like them existed: the old puzzles are the four-sigil room, the library's puzzle mechanics and the pocket puzzle vault. |
| The puzzle engine (`PuzzleRule`, `PuzzleBase`, `PuzzleInstance`, `PuzzleDirector`, `PuzzleStage`, `PuzzlePlanner`, `PuzzleWeights`) | NEW | Sibling of the mechanics Director, built from the same pieces (`Journal`, `Area`, the loot table, the safety rule). |
| `data/nusmp/mega/puzzles/weights.json` | NEW | The one file that chooses among old and new puzzles. |
| Open puzzle rooms `puzzle/gallery_a`, `gallery_b` and the marker `nusmp:puzzle` | NEW | Added to the `puzzle/` folder next to the old puzzle rooms; the pool `side` got one weight line. |
| Sealed Archway trials, Gatekeeper doors, Shadow Key, Floor Warden, Peak Altar, sigil rooms, Riddle Stone, pitfall / snuffer rooms, relic pedestals, the Crucible, the rift pocket vault, the Mana Core door | KEPT, untouched | Section 1. |
| The mechanics library (1000 mechanics, 20 puzzle bases) | KEPT, untouched | One added guard line (section 6): in a room that the new plan gives a puzzle, the library's puzzle-category mechanic is not armed (the draw above decides which of the two kinds the room gets; a library puzzle in a room the draw left alone is as before). |
| Nothing was replaced and nothing was removed. | | |

## 1. Everything that existed before (the inventory)

### 1.1 Trials (a fight that locks the room until it is won)
* **Sealed Archway** (`labyrinth/trial`, `docs/labyrinth/e1_trial.md`): a keystone `nusmp:sealed_archway` with `nusmp:ward_rune` tiles in every doorway and `nusmp:spawn_glyph` tiles for the
  wave. A survival player walking in seals every doorway with `nusmp:magic_barrier`, the glyphs spawn the roster (scaled by the players inside), the barriers fall when the last monster dies,
  and an empty room resets after `reset` seconds. State machine `TrialState`, arithmetic in `TrialMath`. Mega rooms: `trial/trial_hall_a`, `trial_hall_b` and ten themed ones
  (`celestial_court_tribunal`, `crystal_geode_shatter_trial`, `drowned_crypt_drowned_font`, `dwarven_forgehold_anvil_trial`, `infernal_foundry_cage_arena`, `haunted_manor_ballroom`,
  `mountain_monastery_duel_ring`, `vampire_castle_blood_court`, `spider_warrens_brood_hatch`, `sky_citadel_thunder_court`).

### 1.2 Seals, keys and locks
* **Gatekeeper Door** (`labyrinth/seal/GatekeeperDoorBlock`, `e2_seal.md`, `mega_guard.md`): a block-state machine (no block entity) that opens as one door by a flood (`DoorFlood`). Kinds:
  `warden` (opened by a **Shadow Key**), `boss` (opened by the Peak Altar when its boss dies), `puzzle` (opened by the Riddle Stone, section 1.3), `sector` (opened by a **Mana Core**).
* **Shadow Key** (`ShadowKeyItem`), dropped by the **Floor Warden** (`FloorWardenEntity`) by code, one consumed per opening.
* **Peak Altar** (`PeakAltarBlock` / entity): wakes near a survival player, rolls a boss from the template's roster, opens the boss doors within 40 blocks when it dies.
* **Labyrinth Guardian, Guardian Altar, Mana Core, sector door, Way Onward** (`mega/guard`): the key of a sector is forged from the Guardian's death; the door spends it; the Way Onward is the
  rubble seal at the end of the onward corridor (marker `EXPANSION_SOCKET`).
* **Rifts** (`mega/rift`, `rifts.md`): shortcut / pocket / return rifts; **Gate Key**, **Pocket Key**; the pocket realm's **puzzle vault** (four coloured plates, the wall shows the order, the right
  order lifts the bars, a wrong press resets and wakes two vexes) and its secret-boss variant.

### 1.3 Puzzles
* **Sigil puzzle** (`labyrinth/trap`, `f1_traps.md`): four `nusmp:sigil_plate` (colour 0 to 3, a click not a step), a `nusmp:riddle_stone` that reads out the order, the `kind=puzzle` door; `PuzzleState`
  keeps order, progress and the wrong-order lock. Rooms: the Spire's `puzzle_room`, Mega `puzzle/sigil_hall_a`, `sigil_hall_b` and seven themed rooms (`crystal_geode_resonance_hall`,
  `cursed_archive_card_catalogue`, `drowned_crypt_tide_gate`, `holy_church_confessional`, `feywild_grove_fairy_ring`, `infernal_foundry_contract_hall`, `sunken_temple_tidal_gate`).
* **Pitfall field** (`PitfallTileBlock`, the Spire's pit room, Mega `trap/pit_hall_a`, `pit_hall_b` and the themed trap rooms `cursed_archive_burning_stacks`, `fungal_underdark_chasm_ledges`,
  `haunted_manor_attic`, `necropolis_grave_pits`, `sky_citadel_cloud_chasm`) and **snuffer glyph** (`SnufferGlyphBlock`, `trap/dark_room`): trap rooms with a hidden safe route fixed per template and a
  darkness that snuffs torches.
* **The mechanics library's puzzle category** (`mega/mechanics`, `mechanics.md`): 20 bases / 125 variants (`colour_order`, `mirror_demon_pattern`, `chain_spell_pillars`, `scales_of_balance`,
  `lever_gallery`, `tsuzumi_rooms`, `return_by_death_memory`, `courier_of_the_pot`, `sunlight_relay`, `sphinx_riddle`, `true_path`, `trick_tower_pairs`, `majority_door`, `rune_lock`,
  `equivalent_exchange`, `door_of_teeth`, `test_of_position`, `zodiac_gates`, `weeping_statues`, `tarokka_spread`) on 20 counter kinds (`sequence`, `mirror`, `pillars_sync`, `lever`, `carry`,
  `item_offer`, `pay_item`, `pay_xp`, `pay_hp`, `stand_plate`, `split_plates`, `all_present`, `hold_light`, `sneak_through`, `quiet`, `no_damage`, `survive`, `kill_all`, `kill_marked`, `none`),
  placed in any eligible room by the mechanics Planner, with props (coloured pedestals, a plate, a lever, a mimic or cursed chest) placed through a block journal.

### 1.4 Vaults and rewards
* **Relic Pedestal** (`labyrinth/vault`), the **hidden caches** of treasure rooms (`hidden_caches.md`), the **Crucible of Choice** (`mega/crucible`: three relics, exactly one pick per region, the rest
  forfeit, a catastrophe follows) and the trial loot table `nusmp:chests/labyrinth_trial` that every puzzle reward rolls.

## 2. How a new puzzle is built (the architecture)

```
weights.json --PuzzleWeights--> PuzzlePlanner.plan(floor, rooms, theme, road, depth)   pure, seeded: which room gets which puzzle
                                         |  (PuzzleDirector.planFor, cached per sector)
 player enters the room -------> PuzzleDirector.scan -> PuzzleInstance.arm -> PuzzleStage.plan (real blocks) -> drawn through a Journal
                                         |
 right click / step on a tile / chosen answer --> PuzzleInstance --> PuzzleRule.act(Act) -> List<Fx> --> sound, Rukh sparks, text, penalty, tokens, reward
                                         |
 end: Journal.restore() (every block back), monsters and tokens removed; PuzzleData mirrors the journal so a crash is repaired at the next start
```

* **`PuzzleRule`** (pure, no Minecraft types): `Params(seed, party, tier)` fixes the board; `act(Act)` and `tick(long)` answer with `Fx` effects; `face(cell, layer)` is what the cell shows; `suggest()` is
  the next CORRECT action from the current state. `PuzzleBase` holds the plumbing every puzzle needs: the seeded deal (same seed, same board), the lock-out after too many mistakes (a 40-tick
  cool-down, then the puzzle starts over), the move budget of free-form puzzles (a mashed puzzle "jams": small cost, re-deal), and the guarantee that `suggest()` never lies about time.
* **`Layout`**: the cells as groups (row, grid, ring, scatter, explicit points) of roles `PILLAR` (an air cell above the floor, clicked), `PLATE` (a floor tile, clicked or stepped on), `DISPLAY` (shows),
  `CHEST`. `PuzzleGeometry` (pure) arranges the groups; `PuzzleStage` checks the real blocks (flat solid floor with solid ground under it, headroom, no creature in a column, two blocks clear of
  the walls) and tries other anchors; if nothing fits the puzzle is simply not armed. Compass arrows are never rotated.
* **`Palettes`**: the face-to-block tables, as text (vanilla blocks only); the dark base tile follows the theme's flavour (deepslate tiles for the grave, nether bricks for fire, packed ice for frost ...).
* **`PuzzleInstance`**: the live puzzle (stage, journal, party actors, effects). **`PuzzleDirector`**: scans the players every 10 ticks, arms the planned puzzle of the room they stand in, ticks live ones, ends
  them (solved and admired 8 s, nobody inside for 10 s, wound down, cleared, switched off, server stopping). **`PuzzleEvents`**: the right-click hook (cancelled on our blocks, so a chest never opens and a block
  is never placed on a tile) and the server-stopping cleanup. **`PuzzleCommand`**: the admin tree and the player command the answer buttons run.

### 2.1 The rules that keep it safe (the lock rule)
* **Nothing is ever sealed.** A puzzle never closes a door, never places a wall, never traps. Pillars stand two blocks apart (a one-block gap to walk through), tiles are floor, pits are ONE block deep over
  solid ground. The lectern of every puzzle lets the party give up at once (crouch and right-click it twice), and every puzzle winds down by itself after `bypassSeconds` (default 420) of players
  being inside: it pays nothing, it cools down for 10 minutes, nothing stays locked. (Test: `PuzzleGeometryTest.cellsNeverShareABlockAndPillarsKeepAGapToWalkThrough`.)
* **Reset cannot soft-lock.** `reset()` and every restart deal the same board from the same seed, and `suggest()` always has a next step. The JUnit sweep (`PuzzleRulesTest`) proves, for all 29 puzzles over
  5 tiers x 5 party sizes x 12 seeds, that the rule's own hint solves it; that it still does after a storm of random wrong actions (clicks, steps, offers, answers, with time passing), a reset in the middle and a
  restart; that it does so when the world's clock starts at tick 777777 and not at 0; and that a reset puts back exactly the fresh board. Memory and rune puzzles reshuffle or fade by design and are checked
  for solvability instead of equality.
* **Failure costs, never kills.** Penalties are `hurt` (1.5 x severity x (1 + 0.5 x (tier - 1)) magic damage, never the last 3 health points), `dark` (Darkness), `drain` (a few experience points and Weakness),
  `slow` (Slowness), `hunger` (Hunger) and `ambush` (1 to 3 weak dungeon monsters, at most 4 alive). Severity is 1 to 3 (tests). Creative and spectating players are never affected.
* **Never in the wrong place.** Same rule as the mechanics: only the Mega-Dungeon dimension, never a safe room, the Hub or a rift pocket (`PuzzleBridge.allowedAt`).
* **Server authoritative.** The client sends nothing: clicks come from `PlayerInteractEvent.RightClickBlock`, steps from the players' positions in the server tick, answers from a command that names the
  puzzle and the question number (a button of an old question cannot answer a new one, and the player must be inside the room).
* **Carried items cannot leak.** Tokens (keys, shards, bowl offerings) are ordinary vanilla items with a custom name and two data tags; they only count for the puzzle that issued them, a bowl issues one
  whenever asked (a lost key never blocks a puzzle), at most 64 per puzzle, and every token is taken out of every pack when the puzzle ends.
* **Crash safety.** The journal of every live puzzle is mirrored into `PuzzleData` (`nusmp_puzzles`) with the world's save; at the next start the leftover blocks are put back before anyone arrives
  (self-test `puzzle.restore`).

### 2.2 Reward, party scaling, depth scaling
* **Reward**: through the existing path. Each affected player present gets a roll of `nusmp:chests/labyrinth_trial` (never a relic) popped at the room, two rolls and double experience for a flawless
  solve (no mistakes), plus experience that grows with depth (`14 + 9 x s` per player, `s` the mechanics' depth coordinate). Solved puzzles are remembered (`MechanicsData.markSolved("puzzle:<sector>:<room>:<id>")`),
  so they do not re-arm; a given-up or wound-down puzzle is only cooled down.
* **Party**: `Params.party` is the number of affected players inside when the puzzle arms (1 to 8), and the board is rebuilt for a changed party size until the first action. Boards get bigger (more cells,
  more rounds, shorter timers) with the party, but every board stays solvable by ONE delver at every depth (tests sweep party 1, 2, 3, 5 and 8 at all five tiers). `PuzzleGeometryTest` also proves
  the work to solve at tier 5 with eight delvers is never less than at tier 1 alone, and larger for at least 22 puzzles.
* **Depth**: `tier` 1 to 5 (200 floors each). Longer sequences, bigger grids, deeper searches, quicker shadows and blades, more lanes.
* **Per-theme flavour**: on entering, a delver gets the name of the puzzle (action bar), a line of flavour for the sector's THEME (27 theme lines + a default; the puzzle's name is its argument) and the hint.
* **Anti-frustration**: from the third mistake on, the cell of the next correct action glows (Light Rukh) once per further mistake; after `nudgeTicks` (30 s by default, 20 s for the escort) without any
  action a line reminds the party that the lectern gives the hint.

## 3. The 29 new puzzles

The owner's suggested families, and where each is: pressure-plate sequences `plate_sequence` (and `escort_spirit`, `pit_jump`) - rotating-pillar alignment `rotating_pillars` - mirror and beam routing `beam_mirrors` -
weight balance `weight_scales` - lever and switch permutations with sound cues `simon_levers` - memory tiles `memory_tiles` - colour matching `colour_mix` - tile-flip grids `lights_out` - sliding blocks
`sliding_block` - rune copy `rune_copy` - riddle doors with chat buttons `riddle_door` and with item offering `riddle_offering` - sundial / time of day `sundial` - water-level control `water_level` -
lighting torches in an order `torch_order` - gravity flip `gravity_maze` - shadow-cast pattern `shadow_cast` - music and melodies `melody_notes` - pipe connecting `pipe_connect` - key and lock with carried
items `key_and_lock` - escort a spirit `escort_spirit` - stealth and line-of-sight sentries `sentry_stealth` - plate-and-pit jumping `pit_jump` - pendulum timing `pendulum_crossing` - number lock from clues
scattered in a room `number_lock` - constellation connect-the-stars `constellation` - mimic chests `mimic_chests` - elemental affinity pedestals `elemental_mix` - chain reaction `chain_reaction`.

Cells are shown for one delver at tier 1 / a full party at tier 5 (`StatsDump`-style sweep of 30 boards). "Costs" is what a mistake costs.

| id | family | what the party sees and how it is solved | costs | scaling |
|---|---|---|---|---|
| `plate_sequence` | floor | A ring of coloured floor plates and a row of coloured tiles on the wall that spells the order. Step on the plates of the same colours in that order (a right plate lights). | wrong plate: `hurt`, all lights out; lights also fade after 15 s | 4 to 7 plates, 3 to 7 steps (7 to 14 cells) |
| `rotating_pillars` | mechanism | Glazed pillars whose pattern turns a quarter per click; gears drag a neighbour. Turn all to face the compass stone. Dealt by scrambling from solved, so the answer is "4 minus what was dealt". | mashing jams the gears (`slow`, re-deal) | 3 to 6 pillars |
| `beam_mirrors` | light | A beam leaves a golden block heading east across a floor grid; right-click mirrors ('/' or '\') to turn them until the beam, drawn as lit floor, hits the red crystal. Real optics, no redstone. | jam budget | 5x4 to 6x5 grid, 2 to 4 bends, decoy mirrors |
| `weight_scales` | mechanism | Stone pillars whose HEIGHT is their weight; a click sends a stone to the blue pan, the red pan or off; three lamps show which pan is heavier. Use every stone and balance (a split always exists, weights all different). | jam | 4 to 6 stones |
| `simon_levers` | memory | Coloured levers flash and sound a growing melody; repeat it. Input during the show is ignored; the round replays after 20 s idle. | wrong lever: `dark`, replay; 4 mistakes restart | 4 to 5 levers, 3 to 8 rounds |
| `memory_tiles` | memory | Identical floor tiles hide pairs of symbols; step to see, find the twin; matches stay lit. | more mismatches than pairs + 3: `hurt`, the floor reshuffles | 4x3 to 5x4 |
| `colour_mix` | colour | Pairs of pillars (empty, red, blue, yellow) feed lamps by paint rules (red + blue = purple ...); match the colour above each lamp. | jam | 2 to 4 lamps |
| `lights_out` | grid | A right click flips a tile and its four neighbours; light every tile. Dealt from solved by known presses. | jam | 3x3 to 5x5 |
| `sliding_block` | grid | Numbered floor tiles with one hole; a click slides the line toward the hole; order them like the picture beside the grid. Dealt by sliding backwards (always solvable); the hint is the undo of the history. | jam | 3x3 / 4x3, 18 to 50 scramble moves |
| `rune_copy` | memory | A master row of runes and a row of copy stones; right-click cycles a rune (crouch goes back); touch the seal. Deeper down the master fades and the oracle stone shows it again for 10 s (30 s rest). | wrong seal: `drain`, the copy clears, tells how many were right | 3 to 7 runes, fade from tier 2 |
| `riddle_door` | riddle | A sphinx asks riddles from the bank (16, data); three answers as clickable lines in chat in a new order each time. | wrong answer: `drain`, a DIFFERENT riddle | 2 to 4 riddles |
| `riddle_offering` | riddle | Altars speak a riddle whose answer is a thing (coal, bone, gold, lapis, amethyst, redstone, iron, quartz, emerald, diamond, copper); bowls of blocks of those things hand out the item; offer the right one. A delver's own gold ingot also counts. | wrong offering refused (`drain`), item kept | 2 to 4 altars, 2 decoy bowls |
| `sundial` | timing | A shadow creeps round a ring of twelve hour stones; right-click the gnomon when it lies on the gold stone. The shadow is a function of the game tick, the same for everyone. | miss: `slow`, a hit lost | 3 to 5 hits, 2.5 s to 1.7 s per hour |
| `water_level` | mechanism | Glass tanks as tall as they are big, a spring, a drain: pick a tank, then spring / drain / another tank (pour). Put exactly the level of the glass column into one tank. Goals are checked reachable by a search; never a single fill. | jam | 2 to 3 tanks, 6 to 34 clicks |
| `torch_order` | logic | Coloured braziers round a lectern that recites rules ("the red flame must wake before the blue one"); the rules fix one order. | out of order: `hurt`, all braziers out | 4 to 7 braziers |
| `gravity_maze` | mechanism | A lit marble in a floor maze; four arrow pillars tip the room; it slides until a wall, or drops into the gold hole. | too many tips: the room lurches (`slow`), the marble returns | 5x5 to 7x7 |
| `shadow_cast` | light | Turnable pillars cast shades on a wall row; make the shadow row equal the wanted row. Several answers accepted. | jam | 3 to 5 pillars |
| `melody_notes` | sound | A bell plays the tune; note pillars (light = low, dark = high) are tried freely; the stave stone judges the last notes. | off key: `dark` | 5 to 6 notes, 3 to 8 long |
| `pipe_connect` | grid | Glazed pipe tiles (straight, bend, tee) turn on a click; join the golden inlet to the drain; wet pipes change colour. A route is laid first, then decoys, then everything is turned. | jam | 4x3 to 5x5 |
| `key_and_lock` | carry | Take a coloured key from its pedestal (a real tagged item), carry it to the lock of the same colour; opening a lock unseals the next key. | wrong key bites (`hurt`) | 2 to 4 locks |
| `escort_spirit` | floor | Stand on the tile ahead of a pale spirit to light its way (2 s); a shade creeps behind it; stand on the shade's tile to push it back. | caught: the spirit flees to the start (`dark`) | 6 to 10 tiles, shade 1.7 s to 3.7 s a tile |
| `sentry_stealth` | timing | Cross lanes to the far row; a lane is watched one phase in four (red), amber a phase before. | caught: `hurt`, back to the start row | 2 to 4 lanes |
| `pendulum_crossing` | timing | Cross lanes swept by blades (red where it is, amber where next); one tile of five at a time. | cut: `hurt`, back to the start row | 2 to 4 lanes, faster blades |
| `pit_jump` | floor | A column of colours beside the field names the safe colour of each row (level with the row); step only on that colour from one end to the other; any other tile gives way (a one-block pit for 6 s). | pit: `hurt`; own progress per delver | 3x4 to 4x8 |
| `number_lock` | logic | Plaques scattered about the room each state a fact about the code; set the dials (crouch turns back), touch the lock. The facts are drawn until exactly one code fits (checked by trying every code). A wrong try says how many digits are right. | wrong code: `drain` | 3 to 4 digits |
| `constellation` | logic | Draw ONE closed figure through all stars without the thread crossing itself; click the last star to take a thread back; close on the first star. Stars never three in a line. | a crossing thread snaps (`dark`), the figure clears | 5 to 7 stars |
| `mimic_chests` | logic | Chests with inscriptions ("the treasure is (not) in chest X"); exactly one inscription is true, which fixes the real chest. A plain click reads, a crouching click opens. | mimic bites (`ambush`, severity 2) and is spent | 3 to 4 chests |
| `elemental_mix` | carry | Four crystals hand out shards of fire, water, earth, air; each pedestal wants a compound of two (steam, mud, lava, dust, storm, lightning); offer two shards in either order; crouch empties a pedestal. | wrong pair backlashes (`hurt`) | 2 to 4 pedestals |
| `chain_reaction` | mechanism | Charge crystals burst at as many charges as they have neighbours and charge their neighbours; burst the golden core. The deal is accepted only if a search finds a way in 2 to 4 clicks; clicks are limited. | out of clicks: fizzle (`slow`), same deal | 3x3 to 4x4 |

(The families are used for variety: two puzzles of one family on a floor are down-weighted.)

## 4. How the generator chooses among old and new puzzles

`data/nusmp/mega/puzzles/weights.json` (a datapack may replace it; `/reload` re-reads it; keys starting with `_` are comments):

| key | meaning |
|---|---|
| `enabled`, `maxPerFloor` (3), `minSpan` (13), `bypassSeconds` (420) | master switch, puzzles per floor (one fewer on a boss floor), the smaller side of a room in blocks, the wind-down time |
| `roomChance` | probability a room of this kind holds a new puzzle at all: `puzzle` 1.0, `hall` 0.16, `treasure` 0.10, `set` 0.20, `cavern` 0.10 |
| `legacy` | the weight the OLD puzzles (the library's puzzle mechanics) keep in the draw of a room of that kind (`puzzle` 30). Drawing it leaves the room to them |
| `openRooms` | template prefixes with no puzzle apparatus of their own (`mega/puzzle/gallery`); a `puzzle`-kind room NOT in this list is a Spire sigil room and always keeps its own puzzle |
| `themeTags` | the flavour tags of each of the 27 themes (`grave`, `fire`, `water`, `nature`, `arcane`, `holy`, `frost`, `machine`, `sky`, `blood`, `shadow`, `mind`, `crystal`) |
| `types.<id>` | `weight`, `tiers` [lo, hi] (the puzzle never appears outside), `road` {holy, abyssal}, `likes` / `dislikes` (x2.5 / x0.4 for a theme with the tag), `rooms` {kind: factor} |
| `repeatDecay` (0.3), `familyDecay` (0.6), `likeBoost`, `dislikeCut` | the decays for puzzles already on the floor |

A room's puzzle weight is `weight x road x like/dislike x rooms[kind] x repeatDecay^same x familyDecay^family`. `PuzzlePlanner` is seeded by the sector seed and the floor, so the plan is the same for
every player and after a restart. Unless a room is a sigil room, is "busy" (the mechanics library planned a mechanic of another category there that seals, cages, floods or crushes the room: `seal_doors`, `cage`, `flood`, `ceiling`) or is too small, it rolls
`roomChance[kind]` and then draws among the 29 puzzles and the legacy entry. Tests: `PuzzlePlanTest` (the shipped file parses with no problem, names every puzzle and theme, every tier has at least 18
puzzles; determinism and caps; protected rooms stay protected; a marker room always gets one; tiers; the old puzzles keep their stated share within 5 points; a liking theme draws a puzzle more often;
depth changes the menu; a broken file never breaks the game).

**Templates.** The gallery rooms (`structure/mega/puzzle/gallery_a`, `gallery_b`, 27 x 18, three doors, lit to block light 7+, no spawn markers, one `nusmp:puzzle` marker in the middle) are puzzle-kind
rooms with a clear floor. They join the `side` pool as templates of the `puzzle/` folder (relative weight 4 each, the sigil halls 3) and the folder's share in that pool was raised from 1 to 2, so the old puzzle rooms keep nearly their old weight (sigil halls 10 to 9) and the galleries add about twice as many puzzle rooms as there were; they are drawn like the other puzzle rooms. The marker is in `MarkerKind` (appended), the template
checker's list (`tools/labyrinth/mega_lib.py` `MARKER_KINDS`) and the catalog (`src/test/resources/mega/catalog.json`, regenerated). A future template can carry the marker with `type=<id>` to force one
puzzle. `PuzzleGeometryTest` proves that every puzzle at every tier and party size fits a 23 x 14 interior (what a 27 x 18 room leaves two blocks clear of its walls).

## 5. Lang, config, commands, self-tests

* **Lang**: every string is `Component.translatableWithFallback` with the English from `PuzzleText`, so nothing is needed to run. `tools/lang/puzzles_v2.json` (flat, 282 lines, keys `message.nusmp.mega.puzzle.*`)
  is generated from the same table (`PuzzleTextDump`) and a JUnit test fails when it drifts. Per puzzle: `<id>.name`, `<id>.hint`, its lines; `theme.<theme id>` (the flavour), `riddle.<id>.q/a0..a2`,
  `item.<token>.q`, `token.<token>`, `colour.<name>`, `compound.<n>`, `common.*`, `cmd.*`. No `en_us.json` edit.
* **Config**: none; the weights file is the configuration.
* **Commands**: admin `/multiverse mega puzzle list | info <id> | apply <id> here | status | plan | clear | reset | verify` (permission 2). Player `/nusmp_puzzle pick <puzzle> <question> <answer>` (what the answer
  buttons in chat run; permission 0, refused outside the room or for a stale question).
* **Self-tests** (`com.newuniverse.nusmp.labyrinth.mega.SelfTests`, run by the CI smoke test; add the prefix `puzzle` to `SMOKE_EXPECT`): `puzzle.plate_sequence` ... `puzzle.chain_reaction` (29, one per puzzle:
  lays a scratch room in a far corner, arms the puzzle through the same entry point as the Director, checks that the shown blocks are the rule's faces, plays it to the end with a stand-in player through the real
  click / step / answer paths following the rule's hint, asserts solved + reward paid + no exception, ends it and asserts the room is block for block as it was), `puzzle.catalog` (the run-time check:
  all rules solvable, palettes, texts, weights loaded), `puzzle.plan` (the planner against the loaded weights), `puzzle.restore` (crash recovery).

## 6. Edits to existing files (all additions)

| File | Edit |
|---|---|
| `labyrinth/mega/MarkerKind.java` | appended `PUZZLE` |
| `labyrinth/mega/MegaSystem.java` | `PuzzleRegistry.init(modBus)` and `PuzzleRegistry.register(bus)` (two lines) |
| `labyrinth/mega/MegaCommands.java` | `mega.then(PuzzleRegistry.command())` (one line) |
| `labyrinth/mega/mechanics/MechanicsDirector.java` | one guard line in `scan`: a library puzzle mechanic is not armed in a room the new plan gave a puzzle |
| `tools/labyrinth/mega_lib.py` | `"puzzle"` added to `MARKER_KINDS` |
| `tools/labyrinth/mega_pools.json` | `"mega/puzzle/gallery": 4` in `relative`, the `puzzle` folder share in the `side` pool 1 to 2; `side.json` regenerated by `gen_mega_pools.py` |
| `src/test/resources/mega/catalog.json` | regenerated by `mega_catalog.py` (244 templates) |
| New files only otherwise | `labyrinth/mega/puzzle/**`, `labyrinth/mega/mechanics/PuzzleBridge.java`, `data/nusmp/mega/puzzles/weights.json`, the two templates, `tools/labyrinth/gen_mega_puzzle_rooms.py`, `tools/lang/puzzles_v2.json`, this document, the tests |

## 7. How to add a puzzle

1. A class in `puzzle/rules` extending `PuzzleBase`: `makeLayout`, `init` (deal from `rng`, set `jamBudget` if clicks are free-form), `handle`, `face`, `next` (the next correct action). Time may only come from
   `now` / the tick passed in. Report a mistake with `fail(actor, severity, kind, sayKey)`.
2. Register it in `PuzzleCatalog` (the id never changes afterwards), give it a line in `weights.json`, its `name` / `hint` and lines in `PuzzleText`, run `PuzzleTextDump` to rewrite `tools/lang/puzzles_v2.json`.
3. Run `PuzzleRulesTest`, `PuzzleGeometryTest` and `PuzzlePlanTest`: the sweep proves it is solvable, fits, scales and has all its texts. Add a bot test to `PuzzleSpecificTest` if it is won by reading.
   Its self-test `puzzle.<id>` is registered automatically.

## 8. What could not be verified (honest list)

Nothing was run in a game; Gradle is not available offline. Verified: the Java of every new file was type-checked against the project's API stubs (with a negative control that proves the check can fail; the only
errors left are the known stub artefacts "X is abstract; cannot be instantiated"); the pure code (29 rules, planner, weights, geometry, text) is covered by 45 JUnit tests that were run, and mutation checks
(a deliberately broken clue generator, lights-out bookkeeping, mimic inscriptions, torch rules, sliding history, pit colours and mirror reflection) were each caught.

Not verified, because it needs the real game:
* Everything visual and audible: how the palettes look (the glazed terracotta "arrows" that show pillar, mirror and pipe directions are the least certain), whether the note-block pitches sound right, the Light and
  Dark Rukh sparks and the thread lines between stars.
* The exact 1.21.1 behaviour of the calls the stub checker cannot judge: `PlayerInteractEvent.RightClickBlock` cancellation and `ItemStack` custom data on a held token, `Style.withClickEvent` with a run-command
  (the answer buttons), `Player.giveExperiencePoints` with a negative number (the `drain` penalty), the title packets, `ServerPlayer.connection` on a `FakePlayer` in the self-tests.
* That a real gallery room (and the halls, caverns and set pieces that can host a puzzle) leaves a flat clear floor of the needed size at the anchor: `PuzzleStage` tries other anchors and the other stacking
  direction, but a furnished hall may reject a big puzzle and simply not arm it. `PuzzleGeometryTest` only proves the size fits the gallery interior (23 x 14), not the furnished halls.
* Balance: all numbers (penalty strengths, timers, the room chances and weights) are first guesses; they are in `weights.json` and in the rules' constants. The chance a hall or cavern gets a puzzle is low
  (0.05 to 0.10) on purpose; most puzzles will be met in the gallery rooms and the pool weight of those two rooms is the lever.
* The interplay with the mechanics library at run time (a library mechanic in a room the new plan did not claim is left alone and may seal the room as it always could), with the Dungeon Master's ambushes, and
  with other mods' right-click handlers.
* The pocket-realm and rift rooms are not touched; the puzzles do not run there (the safety rule refuses rift pockets).
* The gallery templates were produced and checked by the repository's own generators and checkers (`mega_check.py`: 0 errors; `mega_arch_check.py`; `gen_mega_pools.py --check`; `MegaCatalogTest`), not seen in the game.
