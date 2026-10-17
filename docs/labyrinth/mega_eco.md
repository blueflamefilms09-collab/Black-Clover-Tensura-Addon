# Mega-Dungeon: factions, monster turf wars and the economy of stones and meat (`mega.eco`, 1.54.0, builder ECO)

Package `com.newuniverse.nusmp.labyrinth.mega.eco`. **Nothing here was run in game.** The pure classes are unit tested (JUnit, 79 tests, mutation
checked), the glue classes were type-checked against the project's API stubs (which are incomplete: see section 12), the data files are checked by
`FactionDataTest`, and seven IN-GAME self-tests (`eco.*`, section 10) are registered with the shared `SelfTests` registry so the CI server smoke test
exercises the real thing. CI is the first real compile.

## 1. New, replacement, kept (owner rule 5)

| Part | Status | Notes |
|---|---|---|
| Eleven factions as data (`data/nusmp/mega/factions/*.json`) and as entity-type tags (`data/nusmp/tags/entity_type/mega/*.json`) | NEW | Nothing like it existed. |
| Native faction of every non-safe, non-circulation room (`Room.faction`), assigned when a sector is generated | NEW | Uses the field the contract added to `Room`; `Room.save/load` already persisted it. |
| Turf wars (`TurfWarGoal`, priority 3) and the same-faction target guard | NEW | Only inside `nusmp:mega_dungeon`. |
| Faction drops (`FactionDrops`, a second `LivingDropsEvent` listener) | NEW | Added NEXT TO the 1.36.0 `EconomyDrops`, which is untouched. |
| Stones and meat economy (`EconomyDrops`, `EconomyMath`, `DungeonFood`, `StoneTable`) | KEPT | It already applies in the Mega-Dungeon because `Labyrinth.in(level)` is true there. Not edited, not duplicated. |
| Cooking recipes of the five raw cuts (campfire, smoker, furnace) | KEPT | Verified: all five already have `campfire_cooking`, `smoking` and `smelting` recipes (1.19.0). **No recipe was missing, so none was added.** |
| Item tags `c:foods/raw_meat` and `c:raw_meats` listing the five raw cuts | NEW | Guesses of what Farmer's Delight consumes (section 11). |
| Nothing was replaced and nothing was removed. | | |

## 2. What it does, in one page

* A **faction** is a group of creature types (an entity-type tag `nusmp:mega/<id>`) with rivals, allies, a notice range, a liking for each region theme
  and a raw cut of meat. The eleven ids are fixed by the contract: `undead goblin ogre beast slime construct cultist demon elemental aquatic insect`.
* When a **sector is generated**, every room that is not a Landing, a safe room or a corridor-like piece gets a **native faction**: the one its template
  names with a `nusmp:faction;faction=<id>` marker, else a seeded draw weighted by the sector's theme. It is written to `Room.faction` and saved; the
  encounter director reads it to prefer "the room's own kind" when it spawns.
* When a **mob joins** the Mega-Dungeon and its type is in a faction tag, a `TurfWarGoal` is added to its target selector at priority 3. About once a
  second, if the mob has no target and no player is within 12 blocks, it looks for the nearest mob of a rival faction in range and line of sight and
  goes for it. Inside its native room it notices rivals farther away. Both sides do this, so two rival groups that meet fight each other.
* A mob **never turns on its own faction or an ally**: a target change to such a mob is cancelled (this stops a skeleton's stray arrow from starting a
  war inside the undead). A player target is never cancelled.
* **Bosses** (the Floor Warden, the Labyrinth Guardian, Wandering Bosses, anything in `c:bosses` or with the DPS budget) and **servants** (tamed or
  owned creatures, Paladins, Soul Paladins, shadow soldiers, relic summons, Underworld servants) are outside all of this, on both sides.
* A **kill by a player** of a wild faction mob adds, on top of the base economy: the faction's raw cut (chance rising a little with the sector's threat
  and with Looting, at most `maxExtraMeat`) and a depth bonus (one extra Magic Stone, or a crystal in deeper sectors, with chance `0.02 * threat`).
  A mob killed by another mob in a turf war pays nothing extra, so the wars are not a farm.

## 3. The WHY of the decisions

* **Why entity-type tags are the membership.** The encounter director (another builder) spawns "faction X" from the tag `nusmp:mega/X`, so the tag is
  the shared contract. It also means a pack author adds a creature to a faction by editing a tag (mods that are missing are skipped because every entry
  is `required: false`), and never touches Java or the faction file.
* **Why a goal at priority 3 and not a brain rewrite.** Almost every hostile mob of vanilla, Tensura and Ice and Fire picks targets with TARGET goals.
  A goal at priority 3 sits below the "hit me, I hit back" goal (1) and the "nearest player" goal (2) that vanilla gives hostile mobs, so a player or a
  blow always wins on the next tick, and no goal the mob already has is removed. Rewriting a brain would break every mod's AI and would not survive an
  update. Mobs that are driven by a brain (piglins, hoglins, and any SmartBrainLib mob) ignore goals: they stay out of the wars, which is safe.
* **Why players keep priority.** The dungeon is for the party; a monster that keeps arguing with a goblin while a player stands next to it would be
  broken. Three layers: vanilla priorities (above), the explicit `playerYieldRadius` (a player within 12 blocks stops a hunt from starting and ends a
  running one), and the rule that a mob starts a hunt only when it has no live target.
* **Why the target guard.** Mobs of one kind accidentally fight each other all the time (arrows, explosions, area spells). Inside a faction that is a
  bug, not a war, and the guard cancels it. It never touches player targets, and never a servant or a boss (a Paladin zombie must be able to fight a
  hostile zombie).
* **Why bosses and servants are excluded, and in one list.** A boss has its own balance and loot, and a quarrel must not start or end its fight. A
  servant answers to a player. The mod marks both in a dozen places (scoreboard tags, persistent-data keys, interfaces); `TurfMarks` is the single
  list of those strings, `TurfEligibility` the single decision. A new servant system sets the tag `nusmp_servant` (or adds its key to `TurfMarks`).
* **Why a second drop listener.** `EconomyDrops` is shared with the Spire, has its own switch, tests and balance, and the brief says not to edit it.
  The Mega-Dungeon-specific part (what depends on the faction and on the sector's threat) is added by a second `LivingDropsEvent` listener at `LOW`
  priority, so it runs after the first. The extras are added, never substituted.
* **Why the depth bonus is rarer the better the item is.** A medium crystal is a Juggernaut's drop in the base economy. If the threat bonus gave
  one per five kills the base balance would be gone, so each grade divides the chance (stone 1, low 4, medium 16, high 64): a deep sector pays better
  items, not more of them.
* **Why a splitmix draw for the native faction.** `new Random(sectorSeed ^ roomId)` gives nearly identical first draws for neighbouring room ids (its
  first output moves by one part in ten thousand per unit of seed), so a whole region would get one faction. `Mix` spreads one flipped bit over all 64;
  `FactionGraphTest` proves neighbouring ids give unrelated draws and the mutation check shows it fails with a naive hash.
* **Why the draw walks factions in sorted-id order.** The files are read in whatever order the resource manager lists them; the same sector must
  give the same rooms on every machine and after every restart.
* **Why the listener is idempotent.** The generator re-runs a saved plan after a crash; a re-run must not re-roll, and an admin's `faction set` must not
  be overwritten. A room that already has a KNOWN faction is left alone; the sector is marked dirty only when something changed.
* **Why range and nativeBonus are in the file and in the config.** The file's `aggro` numbers win (each faction has its own character); the config's
  `range` and `nativeBonus` are the fallback for a file without them. A datapack reload runs before the server config is read, so the config is never
  baked into the loaded data: the rules read it at the moment of use. Hard ceilings (48 and 32 blocks) stop a datapack from asking for a world-sized
  entity query.
* **Why the goal keeps time in the mob's own ticks.** A scan is due after N ticks OF THAT MOB (`Entity.tickCount`), not after N ticks of the level
  clock. A mob in a slowly ticking area scans at its own pace, the random start offset spreads a room's scans over a second, and the in-game
  self-test can drive a mob one tick at a time inside a single command (the level clock does not move while a command runs).
* **Why `ResourceManagerReloadListener` and not `SimpleJsonResourceReloadListener`.** The brief named the latter, but its constructor and `apply`
  signatures are not in the project's API dump and must not be guessed. The former is the pattern this repo already compiles in CI
  (`AvatarRaces`, `ChatterReloadListener`) and does the same job.

## 4. Classes (the WHY of each)

| Class | Pure? | Why it exists |
|---|---|---|
| `FactionDef` | pure | One faction as plain values, so the graph, the rules and the drops are tested without Minecraft. Membership is NOT in it (the tag is). |
| `FactionParser` | pure | Turns the parsed JSON tree into a `FactionDef`, clamps numbers, fills defaults and returns plain-English problems instead of throwing, so a typo is reported at reload and not swallowed. |
| `FactionGraph` | pure | Settles the relations once: rivalry mirrored unless `oneWay`, alliance symmetric and stronger than rivalry, unknown references dropped and reported; also the seeded, theme-weighted pick. |
| `Mix` | pure | splitmix64, the reason neighbouring room ids do not collapse onto one faction. |
| `TurfRules` | pure | The comparisons of the turf wars in one place: relation (none / rival / intruder), reach, the native bonus, the territorial rule, range fallbacks, the give-up distance. |
| `NativeFactions` | pure | Which room kinds get a native faction and which faction: marker first, seeded draw second. |
| `FactionLoot` | pure | The faction cut chance and count, the depth bonus bands and rarity, and the whole roll of one death. Reuses `EconomyMath.fleshless`, `EconomyMath` cut kinds and `StoneTable.Haul`. |
| `TurfMarks` | pure | The single list of the boss and servant marker strings, copied from where each system sets them. |
| `FactionIds` | pure | The eleven contract ids, for the data test. |
| `EcoConfig` | | Server config `nusmp-labyrinth-mega-eco-server.toml`, section `[eco]`. |
| `FactionReload` | | The `ResourceManagerReloadListener` that reads `data/<ns>/mega/factions/*.json`, hands each tree to the parser, builds the graph and installs it. One bad file never stops the others. |
| `FactionRegistry` | | `factionOf(EntityType)` by tag membership with a cache (flushed on tag/data reload and server stop), `rivals`, `allies`, `themeWeight`, names and member counts. |
| `TurfEligibility` | | Decides boss / servant / wild from the game objects (interfaces, tags, persistent data, the Floor Warden class and id). |
| `TurfWarGoal` | | The `TargetGoal` at priority 3: a bounded scan once a second of the mob's own ticks, the rules of `TurfRules`, player yield, line of sight, clean stop; every method swallows a runtime exception (logged once) because an exception out of a goal would crash the server tick. |
| `TurfWars` | | The two events: `EntityJoinLevelEvent` installs the goal (reflection on the protected `targetSelector`, as `soul.PaladinGoals` does; logged once on failure), `LivingChangeTargetEvent` cancels same-faction targets. |
| `FactionAssigner` | | The `SectorListener` that writes `Room.faction` when a sector was generated. |
| `FactionDrops` | | The second `LivingDropsEvent` listener (section 8). |
| `FactionCommand` | | `list`, `of`, `room`, `set` for the owner. |
| `EcoSelfTests` | | The in-game self-tests (section 10), registered with `SelfTests` from `EcoRegistry.init`. |
| `EcoRegistry` | | `init` registers the sector listener and the self-tests, `register` the game-bus handlers; `command()` returns the command node. |

## 5. Data files (the WHY of each)

### 5.1 `data/nusmp/mega/factions/<id>.json` (11 files)
One file per faction because the owner wants the factions tunable without a mod update. The file name is the id. Every file carries a `why` string (JSON
has no comments, and the owner's rule is a WHY for every file). Schema (all keys optional except what the parser defaults):

```json
{ "why": "...", "id": "goblin", "name": "Goblin Warrens", "color": "#5C8A3A", "tag": "nusmp:mega/goblin",
  "rivals": ["undead", "ogre", "beast"], "allies": [],
  "aggro": { "range": 14, "nativeBonus": 8 },
  "themeWeights": { "gravity": 5, "blood": 1, "spatial": 0, "flame": 2, "frost": 2, "verdant": 4, "storm": 3, "shadow": 1, "astral": 0 },
  "drops": { "meat": "raw_monster_meat", "meatChance": 0.5 },
  "territorial": false, "oneWay": false }
```

* `rivals`: factions whose members this faction's members hunt. **Mirrored by default** (if the goblins list the ogres, the ogres hunt goblins too);
  `oneWay: true` switches the mirroring off for this faction. The shipped files list every edge on both sides, and a test insists on it.
* `allies`: never fight each other; symmetric; **an alliance beats a rivalry** if a pair is listed as both (reported as a problem).
* `aggro.range` (0..48) / `aggro.nativeBonus` (0..32): notice range in blocks, and the extra range when the rival stands inside the room that is native
  to the hunter. Absent = the config fallback.
* `territorial: true`: also attacks any non-allied intruder standing in its own native room (the constructs), not only rivals.
* `themeWeights`: the weight of this faction when a room needs a native faction in a region of that theme (`gravity blood spatial flame frost verdant
  storm shadow astral`); a missing theme counts 1, 0 excludes the faction from that theme. 0..100.
* `drops.meat`: one of `raw_slime_jelly raw_armor_flank raw_basilisk_fillet raw_direwolf_haunch raw_monster_meat` (the five items of `DungeonFood`);
  `drops.meatChance`: base chance per player kill, 0..1.
* Unknown keys, bad colours, unknown themes, wrong types are reported in the log at reload (`[nusmp] Mega faction file ...`), never thrown.

The relations of the shipped data (every row is mirrored; `slime` has none by design; `cultist` and `demon` are allied):

| Faction | Name | Colour | Hunts (rivals, mirrored) | Allies | range / nativeBonus | Territorial |
|---|---|---|---|---|---|---|
| undead | The Restless Dead | `#8FA08C` | goblin, beast, demon | - | 16 / 8 | no |
| goblin | Goblin Warrens | `#5C8A3A` | undead, ogre, beast | - | 14 / 8 | no |
| ogre | Ogre Holds | `#9C6B3F` | goblin, demon, cultist | - | 12 / 10 | no |
| beast | Beast Dens | `#B8863B` | undead, goblin, aquatic, insect | - | 18 / 6 | no |
| slime | Slime Pools | `#58D1A5` | - | - | 6 / 4 | no |
| construct | Animate Constructs | `#8892A0` | elemental | - | 8 / 12 | yes |
| cultist | Cult Conclaves | `#7A4FA3` | ogre | demon | 14 / 8 | no |
| demon | Infernal Court | `#B3262E` | undead, ogre | cultist | 16 / 8 | no |
| elemental | Elemental Wilds | `#E0803A` | construct | - | 12 / 6 | no |
| aquatic | Drowned Depths | `#3A8FC9` | beast | - | 12 / 6 | no |
| insect | Hive Swarms | `#B0B22C` | beast | - | 14 / 8 | no |

Lore, from the brief: undead vs goblin and beast; ogres vs goblins; cultists summon demons so they are allies; slimes tolerate everyone; constructs ignore
everyone except intruders in their own room (a native bonus only); aquatic and insect fight beasts; elementals ignore all but constructs. Added on top (the
brief called them "sensible lore"): undead vs demon, ogre vs demon and cultist, beast vs goblin, so that demon and cultist rooms see wars too.

Theme weights (what a room of that region favours; the draw uses the weights of the sector's theme):

| Faction | gravity | blood | spatial | flame | frost | verdant | storm | shadow | astral |
|---|---|---|---|---|---|---|---|---|---|
| undead | 3 | 5 | 1 | 1 | 3 | 1 | 1 | 5 | 0 |
| goblin | 5 | 1 | 0 | 2 | 2 | 4 | 3 | 1 | 0 |
| ogre | 4 | 3 | 0 | 4 | 3 | 2 | 2 | 1 | 0 |
| beast | 2 | 1 | 0 | 1 | 5 | 5 | 2 | 1 | 0 |
| slime | 3 | 1 | 1 | 0 | 2 | 4 | 2 | 2 | 1 |
| construct | 4 | 0 | 3 | 3 | 1 | 0 | 5 | 1 | 5 |
| cultist | 1 | 4 | 5 | 1 | 1 | 1 | 1 | 4 | 4 |
| demon | 1 | 5 | 3 | 5 | 0 | 0 | 1 | 4 | 1 |
| elemental | 1 | 0 | 3 | 5 | 3 | 1 | 5 | 0 | 3 |
| aquatic | 1 | 0 | 1 | 0 | 2 | 3 | 2 | 1 | 3 |
| insect | 2 | 1 | 0 | 1 | 0 | 5 | 2 | 3 | 0 |

A test requires every theme to let at least six factions in, and 600 draws per theme to use at least five factions with none above 45 percent.

### 5.2 `data/nusmp/tags/entity_type/mega/<id>.json` (11 files)
The membership, as `{"replace": false, "values": [{"id": "...", "required": false}, ...]}`. `replace: false` so other packs can add to a faction;
`required: false` on every entry so an absent mod never breaks the tag. 109 ids in all (each in exactly one tag, enforced by a test):

| Faction | ids | vanilla | Tensura | ours | Ice and Fire |
|---|---|---|---|---|---|
| undead | 19 | 10 | 2 | 0 | 7 |
| goblin | 4 | 3 | 1 | 0 | 0 |
| ogre | 7 | 2 | 3 | 0 | 2 |
| beast | 24 | 2 | 15 | 2 | 5 |
| slime | 6 | 2 | 3 | 1 | 0 |
| construct | 4 | 2 | 1 | 1 | 0 |
| cultist | 4 | 3 | 0 | 1 | 0 |
| demon | 3 | 2 | 1 | 0 | 0 |
| elemental | 8 | 3 | 4 | 0 | 1 |
| aquatic | 13 | 2 | 8 | 0 | 3 |
| insect | 17 | 4 | 7 | 0 | 6 |
| **total** | **109** | **35** | **45** | **5** | **24** |

How each id was checked (`FactionDataTest.everyCreatureIdExists`): vanilla ids against the `EntityType` constants of the API dump (a test resource
`mega/eco/vanilla_entities.txt`, 130 ids); Tensura ids against the owner's id reference (`mega/eco/tensura_entities.txt`, 207 ids, with the stray
dot of the reference removed: `tensura:.barghest` is `tensura:barghest`); our own ids against the `ENTITIES.register("...")` calls in the sources;
Ice and Fire ids only if they already appear in the Living Dungeon tags (`dungeon/*.json`), which is the project's trusted list. The membership is
generous but only holds creatures that are hostile or hunt: grazers (`dungeon/grazers`: cattledeer, pegasus, pegacorn, unicorn, phantaspore,
hippogryph) are not in any faction, and a test requires every creature of `dungeon/tier1..3` to be in a faction or a grazer. Left out on purpose:
every boss, the `dungeon/excluded` roster veto (arch and greater daemons, Orc Lord and Disaster, Ifrit, Charybdis, the colossus, serpents, eggs, statues), the
legendary dragons, named NPCs, neutral animals (wolf, polar bear), pets and minions of bosses. **Aquatic creatures need water**: a flooded room is
where they belong; the encounter director should place them there.

### 5.3 `data/c/tags/item/foods/raw_meat.json` and `data/c/tags/item/raw_meats.json`
Both list the five raw cuts (`nusmp:raw_slime_jelly`, `raw_armor_flank`, `raw_basilisk_fillet`, `raw_direwolf_haunch`, `raw_monster_meat`) with
`replace: false`. WHY: so a recipe of another mod (Farmer's Delight's cutting board and cooking pot) that takes "raw meat" by tag can take them. **Both tag ids
are guesses** of what those recipes consume (section 11); an unused tag is harmless. No `farmersdelight:*` recipe JSON was written: its formats cannot be
verified here.

### 5.4 Cooking: what was checked
The five raw cuts already have a `campfire_cooking`, a `smoking` and a `smelting` recipe each in `data/nusmp/recipe` (1.19.0: `candied_slime_jelly_*`,
`grilled_armor_flank_*`, `roast_basilisk_fillet_*`, `roast_direwolf_haunch_*`, `seared_monster_meat_*`). Farmer's Delight's stove and skillet cook
with the vanilla `minecraft:campfire_cooking` recipes, so the cuts cook there with no FD-specific file. `FactionDataTest.everyRawCutCooksOnACampfireAndInASmoker`
keeps it true. **Missing recipes found: none. Recipes added: none.**

## 6. Config (`nusmp-labyrinth-mega-eco-server.toml`, section `[eco]`)

| Key | Default | Range | Meaning |
|---|---|---|---|
| `turfWarsEnabled` | true | | Master switch of the wars and of the target guard. |
| `scanEveryTicks` | 20 | 10..200 | Ticks between two looks for a rival by one mob (a random 0..4 ticks are added per look so a room does not scan on one tick). |
| `range` | 14 | 4..48 | Fallback notice range for a faction file without `aggro.range`. |
| `nativeBonus` | 8 | 0..32 | Fallback extra range inside the native room for a faction file without `aggro.nativeBonus`. |
| `playerYieldRadius` | 12 | 0..48 | A mob with a (survival or adventure) player this close does not start, and ends, a quarrel. 0 = never yields. |
| `factionDropsEnabled` | true | | The faction drops (also needs `dnd.drops`, the base economy's switch). |
| `maxExtraMeat` | 2 | 0..8 | The most raw cuts the faction adds to one kill. |
| `stoneThreatBonus` | 0.02 | 0.0..0.25 | Chance per threat level of the extra stone / crystal. |

All values are plain `define` / `defineInRange` and are read through `EcoConfig.get`, which answers the default while the spec is not loaded.

## 7. Turf-war rules in detail

* Relation of hunter faction A to a mob of faction B: **none** if B is unknown, B is A, or they are allies; **rival** if A hunts B (see the table);
  **intruder** if not a rival but A is territorial and B stands inside A's native room.
* Reach in blocks: `range` for a rival outside A's native room, `range + nativeBonus` for a rival or intruder inside it. "A's native room" is the room the
  hunter itself stands in, when `Room.faction` is the hunter's faction; a rival standing inside that same room gets the bonus.
* Scan (once per `scanEveryTicks` ticks of the hunter's own `tickCount`, plus 0..4): a box of `range + nativeBonus` blocks (vertical half-size at most 10), one entity query for `Mob`s, the nearest allowed one wins. Cheap
  tests first (faction, reach, distance), then, only for a candidate that is the nearest so far: wild (not boss, not servant), `canAttack`,
  `canAttackType`, not on the same scoreboard team, line of sight.
* The hunt ends when: the target is dead, removed, in another level, farther than `1.5 * (range + bonus) + 4`, retargeted by someone else (the goal then
  does not clear the foreign target), no longer a rival (or no longer an intruder in the room), not seen for 4 seconds (checked every 10 ticks), a player
  comes within the yield radius, or the wars are switched off.
* A hunter has a wild check at every look, so a mob tagged as a boss after it joined (a Wandering Boss) or turned into a servant stops at once.

## 8. Faction drops in detail

Applies when: the level is the Mega-Dungeon, the dying mob is an `Enemy`, wild (see above), of a faction, `dnd.drops` and `eco.factionDropsEnabled` are on,
and a player killed it (the damage source's player, else the kill credit, as the base economy decides it). Then `FactionLoot.roll`:

* **Cut**: not for a creature `EconomyMath.fleshless` says has no flesh (skeleton, blaze, golem, spirit ...). Chance = `base * (1 + 0.03 * (threat - 1)) + 0.05 *
  Looting`, at most 0.90 (`base` is the faction's `meatChance`; 0 never drops). Count = 1; +1 with chance `min(0.5, 0.04 * (threat - 5))` from threat 6;
  +1 with chance `min(0.9, 0.25 * Looting)`; never above `maxExtraMeat`. The item is the faction's cut (`DungeonFood.rawFor(kind)`).
* **Depth bonus**: one item, chance `min(0.5, stoneThreatBonus * threat) / rarity(band)`; band by threat: 1-5 stone (rarity 1), 6-13 low quality crystal (4),
  14-25 medium (16), 26+ high (64). Item ids are the base economy's (`tensura:magic_stone`, `low_`, `medium_`, `high_quality_magic_crystal`); a missing
  crystal falls back to the stone, a missing stone to nothing, as in the base economy.
* The threat is `MegaDungeon.threatAt(level, mobPosition)`: the sector's threat (1 + id), 0 in the gaps between sectors (then no bonus, and the cut uses
  the base chance).

Numbers (default `stoneThreatBonus` 0.02, a faction with base 0.5, `maxExtraMeat` 2). **First-pass numbers nobody has played**:

| Threat | Bonus item | Bonus chance | Cut chance, no Looting | Cut chance, Looting III | Mean cuts, no Looting | Mean cuts, Looting III |
|---|---|---|---|---|---|---|
| 1 | stone | 2.00 % | 50.0 % | 65.0 % | 1.00 | 1.75 |
| 3 | stone | 6.00 % | 53.0 % | 68.0 % | 1.00 | 1.75 |
| 5 | stone | 10.00 % | 56.0 % | 71.0 % | 1.00 | 1.75 |
| 6 | low crystal | 3.00 % | 57.5 % | 72.5 % | 1.04 | 1.76 |
| 10 | low crystal | 5.00 % | 63.5 % | 78.5 % | 1.20 | 1.80 |
| 14 | medium crystal | 1.75 % | 69.5 % | 84.5 % | 1.36 | 1.84 |
| 20 | medium crystal | 2.50 % | 78.5 % | 90.0 % | 1.50 | 1.87 |
| 26 | high crystal | 0.78 % | 87.5 % | 90.0 % | 1.50 | 1.87 |
| 40 | high crystal | 0.78 % | 90.0 % | 90.0 % | 1.50 | 1.87 |

"Territory pressure" (extra drops for a faction mob that kills a rival in its native room) was NOT built, as the brief allows: a kill by another mob pays nothing extra.

## 9. Command (the lead attaches it under `/multiverse mega`)
`EcoRegistry.command()` (= `FactionCommand.command()`) returns `literal("faction")`, permission 2:

* `faction list`: every faction with rivals, allies, and how many creature types of this game its tag lists.
* `faction of`: the faction of the creature you look at (ray trace, 32 blocks), and whether the turf rules apply to it or skip it (boss / servant).
* `faction room`: sector, room and native faction of the room you stand in.
* `faction set <faction|none>`: sets (or clears) the native faction of the room you stand in; saved at once. The way to test a quarrel is to make
  two rooms native to two rival factions and let the encounter director fill them, or to spawn two rival mobs in one hall.

Text uses lang keys `message.nusmp.mega.mec.cmd.*` and `message.nusmp.mega.mec.faction.<id>` with English fallbacks in the code.

## 10. Tests

### 10.1 JUnit (`src/test/java/com/newuniverse/nusmp/labyrinth/mega/eco`)
`FactionParserTest` (11), `FactionGraphTest` (14), `TurfRulesTest` (12), `FactionLootTest` (11), `NativeFactionsTest` (7), `TurfMarksTest` (4),
`FactionDataTest` (20: the real files). Helpers `MiniJson` (a tiny JSON reader: no JSON library is on the test class path) and `Fx` (factories and an
independent-per-seed dice). Resources `mega/eco/tensura_entities.txt` and `mega/eco/vanilla_entities.txt`. Mutation-checked: removing the mirroring,
the alliance rule, the count cap, the hash, each data guard (a typo'd id, a missing recipe, a one-sided rivalry, a boss in a tag, a changed stone id)
makes a test fail.

### 10.2 In-game self-tests (`EcoSelfTests`, run by `/multiverse mega selftest`, which the CI server smoke test calls after it generated sector 0)
The shared registry is `labyrinth/mega/SelfTests` (a lead contract file: brought into this worktree, not edited, not committed). Keys registered:

| Key | What it drives and asserts |
|---|---|
| `eco.tags` | The eleven tags `nusmp:mega/<faction>` resolve in the real registry and each holds at least one registered creature; the detail lists the count per faction. |
| `eco.factions` | The eleven faction files loaded through the real data reload; every rivalry is mirrored and every alliance mutual in the loaded graph; no graph problem; `factionOf(zombie)` is `undead`, `slime` is `slime`, spider `insect`, vindicator `goblin`, witch `cultist`, blaze `elemental`, a pig and the player have none. |
| `eco.native` | After sector 0 was generated, every room that should have a native faction has a KNOWN one, a room whose template has a `faction` marker is native to exactly that faction, safe rooms and corridors have none, and a sector of 12 or more such rooms uses at least two factions. |
| `eco.target_guard` | The real `LivingChangeTargetEvent`: `setTarget` zombie -> skeleton (one faction) and witch -> vex (allied) are cancelled; zombie -> vindicator (rivals), zombie -> player, clearing a target, and a servant zombie (`nusmp_shadow`) -> skeleton all pass. |
| `eco.turfwar` | A zombie (undead) and a vindicator (goblin), ten blocks apart in the arena with no player within 64 blocks, are added with `addFreshEntity` (so the join event must install the goal: `TurfWars.hasGoal`), then ticked by hand for up to 200 ticks until one has the other as its target. A control pair, zombie and skeleton (one faction), must stay at peace for 100 ticks. |
| `eco.player_priority` | The same pair with a (fake, invulnerable, invisible) player about 7 blocks from each, listed in `ServerLevel.players()` for the test: 100 ticks of peace; then the player is removed, targets cleared, and the pair must fight within 200 ticks, so the quiet before is proven to come from the yield and not from a pair that never fights. |
| `eco.drops` | 1500 zombie kills by a fake player in sector 0 (threat read from the room) are posted to `FactionDrops.onDrops` as real `LivingDropsEvent`s: only the undead cut and the four stone items appear, never more than `maxExtraMeat` cuts or one bonus item, at least one cut and one bonus occur; 300 deaths with no player drop nothing; 600 skeleton kills drop no raw cut. |

How the arena works (and why): the mobs are spawned in the AIR (y 110, above every room) over the chunk of sector 0's entry, with no gravity, so the test
changes no block and touches no room, encounter or sector state, and does not depend on what the templates put where. The chunk is loaded with
`getChunk`; the status of a chunk that was just loaded is applied to the entity storage by a task QUEUED on the server thread, which cannot run inside
one command, so `Arena.ensureVisible` loads neighbouring (and, last, a few far) chunks, because the server works its queue while it waits for a chunk,
until entity queries can see the probe mob. The mobs are ticked by hand (`tickCount++` then `tick()`, as `ServerLevel.tickNonPassenger` does). A fake player
is not in `ServerLevel.players()` (nothing adds it), so mobs would never see it; the test lists it there and takes it out again in a `finally`.
Every spawned mob is discarded and the fake player's flags are reset whatever happens. Each test skips (an OK line that says why) when its precondition is
a configuration choice (`eco.turfWarsEnabled` or the drop switches off, peaceful difficulty, a real player near the arena) and FAILS when the world is wrong
(no Mega dimension, no sector 0, still generating).

## 11. What is NOT verified (honest list)

1. **Nothing ran in game.** The whole behaviour of the wars (does a goblin really chase an ogre through a doorway, do two rival groups sort themselves
   out, does the yield feel right) is unplayed. The notice ranges, the weights, the cut chances and the depth bonus are first-pass numbers.
2. `TurfWarGoal extends TargetGoal`: the project's API dump and stubs do not contain `TargetGoal`. The class uses only the members the repo's own
   CI-compiled `NamedCorpseEntity.FoeGoal` uses (constructor `(Mob, boolean)`, `start`, `canUse`, `canContinueToUse`, `setFlags`) and nothing else of it.
3. `Mob.targetSelector` is read by reflection under its official name (as `PaladinGoals` and `AvatarAi` do). If a runtime used other names the wars
   would be off and the log says so once.
4. Calls type-checked only by their appearance elsewhere in the CI-compiled repo (the stubs lack them): `Mob.getSensing().hasLineOfSight`,
   `CommandSourceStack.getServer`, `LivingDropsEvent.getDrops`, the Looting lookup (copied from `EconomyDrops`), `ServerStoppedEvent`, the reload listener
   types (copied from `AvatarRaces`).
5. Brain-driven mobs (piglin, hoglin, SmartBrainLib mobs) ignore target goals, so they do not take part in wars; their `setTarget` is not used.
6. The creature ids of Ice and Fire are only those the Living Dungeon tags already trusted; the Tensura ids were checked against the owner's id
   reference, not against the jar. Which Tensura creatures are hostile is a judgement: `tensura:bone_golem` was left out because it may be a player's
   summon, and `tensura:akash` (its role is not known here) was put in `elemental`, the faction that is least affected by a wrong guess.
7. The tags `c:foods/raw_meat` and `c:raw_meats` are guesses of what Farmer's Delight consumes. Harmless if wrong; delete the two files to take the
   feature out.
8. The encounter director must put aquatic creatures only where there is water, and must read `Room.faction` itself (it is the only channel).
9. `nusmp_wanderer` (the encounter director's boss tag) is in `TurfMarks.BOSS_TAGS` because the brief names it; the data test cannot check that the
   director sets it, because that code lives in another builder's package. After integration confirm that `EncRegistry` tags wanderers with exactly
   `nusmp_wanderer`, or add the real tag to `TurfMarks`.
10. The in-game self-tests were never run: they type-check (against stubs completed locally for `Vindicator`, `Witch`, the `LivingDropsEvent`
    constructor and `ItemEntity.getItem`, all of which exist in the API dump), but these assumptions can only be settled by the CI run:
    (a) `ServerLevel.players()` is the live list, so `add`/`remove` work; (b) a chunk loaded by `getChunk` becomes visible to entity queries once other
    chunks have been loaded (the pump), otherwise `eco.turfwar` and `eco.player_priority` fail with "stay invisible to entity queries" and the pump
    needs another approach; (c) `Mob.setTarget` fires `LivingChangeTargetEvent` and a cancel keeps the old target (`eco.target_guard` proves it, or
    shows that the guard is dead); (d) hand-ticking a mob with `tickCount++; tick()` runs its goal selector; (e) `Mob.targetSelector` can be read by
    reflection in the CI runtime (`eco.turfwar` fails with "did not give the turf-war goal" otherwise).
11. A Paladin released later never gets the goal until its chunk reloads (the join check skips servants); a boss or servant that stops being one mid-life
    keeps its goal but the goal's own wild check lets it act.

## 12. How it was checked
Pure classes: JUnit through the local shim (79 tests passed, mutation checks above). Glue: `javac` against the project's API stubs with a deliberate
negative control (six wrong calls were reported); the only errors left in this package are members/packages the stubs lack, listed in item 4 above.
The data test reads the real files. Nothing else.

## 13. Lead integration
```java
// config (wherever the other builders' configs are registered)
modContainer.registerConfig(ModConfig.Type.SERVER, com.newuniverse.nusmp.labyrinth.mega.eco.EcoConfig.SPEC, "nusmp-labyrinth-mega-eco-server.toml");
// command, under the /multiverse mega node (permission 2 is set inside)
mega.then(com.newuniverse.nusmp.labyrinth.mega.eco.EcoRegistry.command());
```
Lang: merge `scratchpad/mega/frag/mec.lang.json` (28 keys) into `en_us.json`. No quest keys, no creative tab entries, no client renderers, no
blocks or items. `MegaSystem` already calls `EcoRegistry.init` and `register`; `init` also registers the seven `eco.*` self-tests, so the lead adds
nothing for them. They need the lead's `SelfTests` and `SelfTestCommand` (not part of this builder's files) to be on the merged tree.
