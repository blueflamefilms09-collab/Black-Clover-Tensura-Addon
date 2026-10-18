# The Dungeon Mechanics Library (1000 mechanics)

A layer ON TOP of the Mega-Dungeon: **1000 data-defined mechanics** that the dungeon attaches to a floor, a room, a hazard zone or a boss arena, built from about 150 hand-designed
base designs and their variants, drawn from more than 45 anime and from the main D&D rulebooks. Code: `labyrinth/mega/mechanics/`; data: `data/nusmp/mega/mechanics/*.json`
(generated); generator: `tools/mechanics/gen_mechanics.py` with the design cards in `tools/mechanics/tables/*.py`; lang: `tools/mechanics/lang_fragment.json`.

**New or replacement (owner's rule 5).** Everything here is NEW. Nothing existing was replaced or removed: the D&D traps (`labyrinth/trap`), the riddle rooms, the room kinds, the
Dungeon Master, the safe zones, the rifts and the Guardians work exactly as before. The library only adds a layer the Director can put on top of them; with
`[mechanics] enabled = false` the dungeon is as it was.

**State of this document.** Written without a compiler or a game (CI compiles, runs the JUnit tests and the headless server smoke test). The pure part (catalogue, parser, signature
guard, planner, scaling) is pinned by JUnit tests that were run; the engine and the 80 primitives were type-checked against the API stubs and are covered by in-game self-tests that
nobody has run yet. Section 9 lists what is unverified.

---------------------------------------------------------------------------------------------------------------------------------------------

## 1. What a mechanic is

| field | meaning |
|---|---|
| `id` | `<category>.<base>[.<variant>]`, for example `hazard.spore_cloud.e_poison` |
| `name`, `desc`, `flavor` | English texts, also the fallbacks of the lang keys `mechanic.nusmp.<id>.name / .desc / .flavor`; they say in the mod's own words what the mechanic is and where it comes from |
| `cat` | one of 8 categories (below) |
| `src` | the series or rulebook it is inspired by (never empty; guarded) |
| `base` | the hand-designed card it was expanded from |
| `scope` | `floor` (every player of the floor), `room` (one room), `zone` (a disc round an anchor in a room), `boss` (the arena of a boss floor) |
| `tiers` | `[lo, hi]` of the five tiers of 200 floors (`Scaling.tierOf`); dearer mechanics start deeper, trivial ones end sooner |
| `road` | `both`, `holy` or `abyssal` |
| `themes` | the themes it is at home in (weight x3 there, x0.35 elsewhere; empty = no preference) |
| `cost`, `weight` | threat cost against the floor's budget, and the weight in the draw |
| `trigger` | WHEN it fires: one of 18 kinds with its arguments |
| `effects` | WHAT it does: a list of calls of the 80 primitives. Reserved keys of an effect: `delay`, `chance`, `times` + `every`, `if` (a gate on the instance's counters, for example `"wrong>=1"`), `on` (`"solve"`: the effect is a reward that waits for the counter) |
| `counter` | HOW a player beats it: one of 20 kinds (`none` = survive it or leave) |
| `reward` | what beating it pays: `none`, `loot` (the dungeon's trial table, never a relic), `xp`, `heal`, `mana` |
| `sig` | the first 12 hex digits of the SHA-1 of the behaviour signature (the generator writes it, a test recomputes it in Java) |

A number written `[a, b]` is SCALED by depth: `a + b*s` where `s` runs from 0 at floor 1 to 4.995 at floor 1000 (`Scaling.value`); plain numbers are constants. Damage-like numbers are also
multiplied by the config difficulty.

## 2. Architecture

```
tools/mechanics/tables/*.py  --gen_mechanics.py-->  data/nusmp/mega/mechanics/{floor,puzzle,hazard,combat,risk,boss,travel,party}.json + ops.json
                                                                  |  (reload listener: MechanicLibrary.Reload)
                                                        MechanicLibrary (immutable snapshot) + OpCatalog (ops.json)
                                                                  |
 Planner.plan(library, floor, config)  <-- pure, seeded -- MechanicsDirector.planFor(sector)   (cached per sector, rebuilt when the config or library changes)
                                                                  |  scan of the players every 10 ticks
                                                        Instance (one live mechanic: anchor, Area, props, wardens, flags, running effects, journals)
                                                                  |  every tick
              Triggers.poll  ->  fire  ->  Prims (80 primitives)  ->  Run (arguments, targets, Journal)      Counters.poll / onUse / onMobDeath -> solve -> Rewards
                                                                  |
                                                  Instance.end(): blocks restored from the journals, monsters removed, flags dropped
```

* **Pure and testable (no Minecraft types):** `MechCategory`, `Scope`, `Road`, `Scaling`, `Args`, `MechanicDef`, `OpCatalog`, `MechanicParser`, `Signature`, `Channels`, `Weights`, `Seeds`, `Planner`, `Shapes`,
  `Cond`, `Blacklist`, `MobGroups`.
* **Engine (server thread):** `MechanicLibrary`, `MechanicsDirector`, `Instance`, `Run`, `Prim`/`Prims*`, `Triggers`, `Counters`, `Props`, `Mobs`, `Journal`, `Rules`, `Rewards`, `Messages`, `Rukh`, `Safety`, `Area`, `MechanicsData`
  (SavedData), `MechanicsEvents`, `MechanicsCommand`, `MechanicsConfig`, `MechanicsSelfTests`, `MechanicsRegistry`.

## 3. The primitives, triggers and counters

The vocabulary lives in `tools/mechanics/tables/ops.py` and is shipped as `ops.json` (the Java `OpCatalog` validates every mechanic against it; the self-test demands an executor for every id).

<!-- OPS:BEGIN -->

Generated by `tools/mechanics/gen_mechanics.py` from `tools/mechanics/tables/ops.py`; do not edit by hand.

### The 80 effect primitives

| id | group | arguments (defaults) | channels | sustained | what it does |
|---|---|---|---|---|---|
| `effect` | creature | id=minecraft:slowness, secs=5, amp=0, who=players | - |  | apply a mob effect |
| `damage` | creature | amount=2, pct=0, kind=magic, who=players | - |  | hurt |
| `heal` | creature | amount=4, pct=0, who=players | - |  | heal |
| `knockback` | creature | power=1, up=0.3, from=anchor, who=players | - |  | push away |
| `pull` | creature | power=0.8, who=players | - |  | drag towards the anchor |
| `launch` | creature | power=1, who=players | - |  | throw upward |
| `dash` | creature | power=1, dir=random, who=players | - |  | a sideways shove |
| `ignite` | creature | secs=4, who=players | - |  | set on fire |
| `freeze` | creature | secs=5, who=players | - |  | powder-snow chill |
| `starve` | creature | amount=4 | - |  | drain hunger |
| `drain_mana` | creature | pct=10 | mana |  | drain magicules |
| `restore_mana` | creature | pct=20 | - |  | restore magicules |
| `xp` | creature | levels=-1 | - |  | give or take experience levels |
| `gravity` | creature | mode=up, secs=4, who=players | gravity | yes | flip or weigh gravity |
| `root` | creature | secs=3, who=players | movement |  | hold in place |
| `tp_random` | creature | radius=6, who=players | teleport |  | blink to a random safe spot of the area |
| `tp_anchor` | creature | who=players | teleport |  | to the anchor |
| `tp_swap` | creature |  | teleport |  | two players trade places |
| `tp_entry` | creature |  | teleport |  | back to where they came in |
| `tp_room` | creature | mode=random | teleport |  | to another room of the floor |
| `mark` | creature | secs=10, who=players | - |  | glowing |
| `shuffle_hotbar` | creature |  | - |  | swap two hotbar slots |
| `item_cooldown` | creature | secs=6 | - |  | every item on cooldown |
| `curse_hand` | creature | secs=20, dmg=1 | curse | yes | attacking hurts the attacker |
| `take_item` | creature | item=minecraft:gold_nugget, count=1 | - |  | remove items from the inventory |
| `give_item` | creature | item=minecraft:gold_nugget, count=1 | - |  | give items |
| `give_loot` | creature | table=nusmp:chests/labyrinth_trial, rolls=1 | - |  | roll a loot table into the inventory |
| `xp_orbs` | creature | amount=20 | - |  | drop experience |
| `burden` | creature | secs=6, per=6 | movement | yes | slowness by how full the inventory is |
| `speed` | creature | factor=-0.4, secs=6, who=players | time | yes | walk speed modifier (time dilation) |
| `disorient` | creature |  | - |  | spin the view |
| `link` | creature | radius=8, dmg=1, secs=20 | tether | yes | players tied together |
| `share_damage` | creature | pct=50, secs=20 | burden | yes | a blow is shared by the party |
| `downed` | creature | secs=12 | revive | yes | a lethal blow leaves a downed player a mate can lift |
| `no_regen` | creature | secs=20 | regen | yes | no natural healing |
| `place` | world | block=minecraft:stone, shape=ring, radius=4, height=1, count=12, at=auto, secs=0 | - |  | place blocks into free air |
| `floor_swap` | world | block=minecraft:magma_block, shape=disc, radius=3, at=auto, secs=0 | floor |  | replace the floor layer |
| `pit` | world | radius=2, depth=1, at=auto, secs=8 | floor |  | open a shallow pit |
| `flood` | world | height=2, block=minecraft:water, at=room, secs=10 | water |  | raise liquid over the floor |
| `cage` | world | radius=3, height=3, block=minecraft:iron_bars, at=auto, secs=0 | cage |  | bars round the party |
| `seal_doors` | world | block=minecraft:iron_bars, secs=0 | doors |  | bar every doorway of the room |
| `unseal` | world |  | - |  | lift the seal and cages of this mechanic |
| `lights_out` | world | radius=10, at=auto, secs=0 | light |  | snuff the light sources |
| `lights_on` | world | count=6, level=15, at=auto, secs=0 | light |  | invisible light blocks |
| `wall_blink` | world | block=minecraft:stone_bricks, period=40, radius=4, secs=30, at=auto | walls | yes | walls that come and go |
| `slider` | world | block=minecraft:smooth_stone_slab, axis=x, range=6, period=10, radius=2, secs=30, at=auto | platform | yes | a platform that travels |
| `ceiling` | world | block=minecraft:stone, period=12, radius=4, secs=12, dmg=2, at=auto | ceiling | yes | a ceiling that comes down and goes up again |
| `trail` | world | block=minecraft:glass, life=40, secs=20 | trail | yes | a bridge that forms under the feet and fades |
| `prop` | world | kind=pedestal, count=3, at=auto | - |  | interactive props of this mechanic |
| `spawn_group` | mob | group=undead, count=3, ring=6, hunt=true, hp=1 | summon |  | a band of monsters round the anchor |
| `spawn_ambush` | mob | group=undead, count=3, hp=1 | summon |  | monsters from the dark edge of the room |
| `spawn_guard` | mob | group=construct, count=2, still=true, hp=1.5 | summon |  | guards that hold their post |
| `spawn_swarm` | mob | kind=silverfish, count=6 | summon |  | a swarm of small things |
| `spawn_elite` | mob | group=knight, hp=3 | summon |  | one big monster |
| `spawn_chest` | world | loot=nusmp:chests/labyrinth_trial, mimic=0.3, cursed=false, at=auto | - |  | a chest, maybe a mimic, maybe cursed |
| `buff_mobs` | mob | id=minecraft:strength, secs=10, amp=0 | - |  | effect on the monsters of the area |
| `shield_mobs` | mob | secs=0 | shield |  | monsters take no damage until unshielded |
| `unshield` | mob |  | - |  | lift the shield |
| `kill_mobs` | mob |  | - |  | remove the mechanic's monsters |
| `heal_mobs` | mob | pct=50 | - |  | heal the monsters |
| `enrage` | mob | secs=15, speed=0.3 | - |  | faster and stronger |
| `tether` | mob | pct=80 | shield | yes | followers take less damage while their leader lives |
| `reflect` | mob | pct=30, secs=20 | reflect | yes | blows are returned to the attacker |
| `summon_wave` | mob | group=undead, count=3, waves=3, period=200 | summon | yes | waves on a timer |
| `split_on_death` | mob | count=2 | - | yes | a slain monster leaves smaller ones |
| `swap_mob` | mob |  | teleport |  | a monster trades places with a player |
| `doppelganger` | mob | count=1 | summon |  | a hostile double of a player |
| `call_dm` | mob |  | summon |  | ask the Dungeon Master for an ambush |
| `weather` | env | mode=rain, secs=20 | weather | yes | weather for the players of the area only |
| `lightning` | env | count=2, dmg=4, spread=5 | - |  | visual bolts that hurt |
| `projectile` | env | kind=arrow, count=3, speed=1.2, dmg=2 | - |  | shots from the walls |
| `shockwave` | env | radius=6, dmg=3, power=0.8, secs=2 | - | yes | a ring that runs outward |
| `blast` | env | radius=4, dmg=4, power=1.2 | - |  | a burst with no block damage |
| `wind` | env | dir=north, power=0.12, secs=6 | wind | yes | a steady push |
| `quake` | env | secs=4, dmg=0 | - | yes | the room shakes |
| `rukh` | env | align=auto, shape=burst, count=10 | - |  | Rukh particles |
| `sound` | env | id=minecraft:block.beacon.ambient, vol=1, pitch=1 | - |  | a sound |
| `title` | env | text=, mode=actionbar | - |  | a message to the players of the area |
| `counter_add` | env | name=count, n=1 | - |  | count something for the counter / trigger |
| `reveal` | env | secs=6 | - |  | show the props of the mechanic |

### The 18 triggers

| id | arguments (defaults) | fires when |
|---|---|---|
| `floor_enter` |  | once, when the first player steps into the mechanic's area |
| `area_enter` | cooldown=200 | every time a player comes into the area |
| `timer` | every=200 | every N ticks while a player is inside |
| `proximity` | r=4, cooldown=100 | a player within r blocks of the anchor |
| `sight` | range=14, cooldown=100 | a player looks at the anchor |
| `kill` | n=3 | n monsters die in the area |
| `hp_below` | frac=0.5, cooldown=200 | a player under a fraction of their health |
| `item_use` | item=any, cooldown=100 | a player uses an item (any, food, light, weapon, magic, or an id) |
| `interact` | cooldown=60 | a player uses a prop block of the mechanic |
| `damage_taken` | min=1, cooldown=60 | a player is hurt in the area |
| `sprint` | ticks=40, cooldown=100 | a player sprints for a while |
| `idle` | ticks=100, cooldown=100 | a player stands still for a while |
| `sneak` | ticks=60, cooldown=100 | a player sneaks for a while |
| `jump` | n=3, cooldown=100 | a player jumps n times |
| `boss_hp` | frac=0.5 | the boss of the arena falls under a fraction of its health |
| `party_size` | n=2, cooldown=200 | n or more players in the area |
| `death` | cooldown=100 | a player dies in the area |
| `dark` | light=4, ticks=60, cooldown=100 | a player stays in the dark |

### The 20 counters

| id | arguments (defaults) | the engine places | how it is beaten |
|---|---|---|---|
| `none` |  | none | cannot be beaten: it must be survived or left behind |
| `kill_all` | group=undead, count=3 | mobs | kill every warden the mechanic spawns |
| `kill_marked` | group=knight | mobs | kill the one marked warden |
| `item_offer` | item=minecraft:gold_ingot, count=1 | pedestal | give an item to the altar |
| `stand_plate` | secs=6 | plate | stand on the plate for a while |
| `sneak_through` | secs=8 | none | sneak for a while in the area |
| `hold_light` | secs=8 | none | carry a light for a while |
| `sequence` | steps=4 | pedestal | use the props in the right order |
| `lever` |  | lever | pull the lever |
| `survive` | secs=30 | none | last out the time |
| `all_present` | secs=4 | plate | every living party member in the area at once |
| `split_plates` | n=2 | plate | n different players on n plates together |
| `carry` | item=minecraft:amethyst_shard | pedestal | carry the item from one pedestal to the other |
| `no_damage` | secs=20 | none | take no damage for a while |
| `quiet` | secs=15 | none | neither sprint nor jump for a while |
| `pay_xp` | levels=3 | pedestal | pay experience levels at the altar |
| `pay_hp` | hearts=3 | pedestal | pay health at the altar |
| `pay_item` | item=minecraft:iron_ingot, count=4 | pedestal | pay a price in items |
| `pillars_sync` | secs=4, steps=3 | pedestal | light every pillar within seconds of each other |
| `mirror` | steps=3 | pedestal | repeat a shown pattern |

Exclusive channels (two mechanics sharing one are never stacked): `burden`, `cage`, `ceiling`, `curse`, `doors`, `floor`, `gravity`, `light`, `mana`, `movement`, `platform`, `reflect`, `regen`, `revive`, `shield`, `tether`, `time`, `trail`, `vision`, `walls`, `water`, `weather`, `wind`.

<!-- OPS:END -->

## 4. How the Director chooses (the rules of the brief)

* **Seeded and reproducible.** `Seeds.floorSeed(sector.seed, floor)`; the plan is a pure function of the sector, its rooms, the library and the config, so it needs no saved state; it is cached in memory.
* **By weight.** `Weights.weight`: 0 outside the mechanic's tier range or on the other road; x3 for a theme it likes, x0.35 for one it does not; times a bias by room kind (`puzzle` rooms draw puzzles, `trap` rooms hazards,
  `trial` rooms combat, `treasure` rooms risk-and-reward, corridors traversal).
* **By budget.** A floor has a threat budget of `8 + 4*tier` (times the difficulty); each mechanic costs `cost`; a boss arena has `4 + 2*tier` of its own.
* **Caps.** `[mechanics] perFloor` (default 4: floor-wide plus room/zone ones; a boss arena may add up to two boss mechanics) and `perRoom` (default 1).
* **No stacking of conflicts.** `Channels.of(def)`: every primitive declares the exclusive channels it occupies (`gravity`, `light`, `water`, `weather`, `time`, `doors`, `shield`, `reflect`, `revive`, `burden`, `vision`, `floor`,
  `movement` ...), a mob effect may too (darkness is `vision`); two mechanics sharing a channel, or two variants of one base, are never planned together. Floor-wide mechanics clash with everything on the floor, room ones with the
  mechanics of their room.
* **Never in the wrong place.** `Safety.allowedAt`: only the Mega-Dungeon dimension (the Hub, Floor 0, is another dimension), never in a rift pocket (`RiftRules.slotAt >= 0`), never in a safe room (Landing, Safe Zone, stairwell).
  The planner also never plans a mechanic in a safe room, the Guardian's chain (antechamber, vault, onward) or a room smaller than 9 blocks; creative and spectating players are never affected.
* **Cleaning up.** An instance ends when its area has been empty for five seconds, when the Guardian of a boss floor dies, when an admin clears it, when the switch is turned off or the server stops. Ending restores every block
  from the journals, discards the monsters it spawned and drops its flags. A mechanic that was beaten is remembered in `MechanicsData` (`nusmp_mechanics`) so it does not re-arm. The journals of running instances are mirrored
  in the same SavedData, so a crash cannot leave a block changed: the next start restores them before any player is let in.

## 5. Config, command, lang, tests

* `world/serverconfig/nusmp-labyrinth-mega-mechanics-server.toml`, `[mechanics]`: `enabled`, `perFloor`, `perRoom`, `difficulty`, one switch per category (`floorModifiers`, `roomPuzzles`, `hazards`, `combatGimmicks`,
  `riskReward`, `bossMechanics`, `traversal`, `party`), `blacklist` (a comma-separated string of ids; `base.*` bans a base and all its variants), `creativeActivates`.
* `/multiverse mega mechanic list [category] [page]`, `info <id>`, `apply <id> here`, `clear`, `reset`, `count`, `verify` (permission 2).
* Lang: every key is `Component.translatableWithFallback`; `tools/mechanics/lang_fragment.json` holds the 3000 mechanic keys and the message keys (`message.nusmp.mega.mech.*`); merge it into `en_us.json`.
* Monster groups: `data/nusmp/mega/mechanic_groups.json` (which entity stands for `undead`, `swarm` ... at which tier; the foundry's `nusmp:foundry_mob` is named there as an `optional` entry, skipped when the type does not exist).
* In-game self-tests (run by the CI smoke test): `mech.library`, `mech.resolve`, `mech.prims` (every primitive executes on a scratch room and leaves it block for block as it was), `mech.triggers`, `mech.counters`, `mech.director`, `mech.apply`.
* JUnit 5 (pure): `MechanicsDataTest` (parses cleanly, no two mechanics share a behaviour signature, name or description, the generator's and Java's signature agree, every source non-empty and spread: at least 30 anime,
  none above 8 %, every theme exists, every primitive/trigger/counter is used), `PlannerTest` (determinism per seed, caps, budget, conflicts, no safe rooms, categories and blacklist), `MechanicsCoreTest` (scaling, buckets, seeds,
  weights, channels, blacklist, conditions, shapes, monster groups, catalogue).

## 6. How to add a mechanic

1. A new VARIANT of an existing design: add an element, trigger or counter alternative to its card in `tools/mechanics/tables/<category>.py`. Alternatives are taken in the order element 0, trigger 0, counter 0, element 1 ... so a new
   alternative only appears in the expansion if the card's variant count reaches it; raise the count by moving it earlier in the list.
2. A new BASE: add a `base(...)` card. Each category must expand to exactly 125 (`PER_CATEGORY` in the generator) and the generator spreads that over the cards of the category; it fails when a card has too few alternatives.
3. Run `python3 tools/mechanics/gen_mechanics.py` (writes) and `--check` (CI). The guards fail on a duplicate id, name, description or behaviour signature, an argument outside the catalogue, a missing source, a source above 8 %, fewer than
   30 anime sources, or a primitive/trigger/counter nobody uses.
4. A new PRIMITIVE: add it to `tools/mechanics/tables/ops.py` AND an executor in one of `PrimsCreature` / `PrimsWorld` / `PrimsMob` / `PrimsEnv` (`Prims.once` / `Prims.timed`); `mech.library` and `mech.prims` fail until both exist.
   Block changes must go through `r.journal()` (or the instance's named journals) so they are restored.

## 7. Wiring (the lead adds these lines; this branch already has them)

* `MegaSystem.init`: `com.newuniverse.nusmp.labyrinth.mega.mechanics.MechanicsRegistry.init(modBus);` and `MegaSystem.init` end: `...MechanicsRegistry.register(bus);`
* `MegaCommands.node()`: `mega.then(com.newuniverse.nusmp.labyrinth.mega.mechanics.MechanicsRegistry.command());`
* `NUSMP`: `modContainer.registerConfig(ModConfig.Type.SERVER, com.newuniverse.nusmp.labyrinth.mega.mechanics.MechanicsConfig.SPEC, "nusmp-labyrinth-mega-mechanics-server.toml");`

## 8. The counts

Category and source counts, and every base design, are in the generated section below.

## 9. Not verified (honest list)

Nothing of the engine was run in the game. Unverified: every primitive's feel (timings, numbers, how a platform carries a rider, how water behaves round the doors), the 1.21.1 API calls that the stub type-checker cannot check (the lever and
button block states, the packet constructors, `ClipContext`), the interplay of the incoming-damage hooks with the mod's other damage systems (the HP buffer, the Guardians' damage budget, the Rukh flow), the shared-SavedData journal
recovery after a crash, the floor-wide mechanics' anchor (chosen among the rooms within 56 blocks of the first player to enter, not at a fixed room), and the balance of all the numbers. The foundry's variant names in `mechanic_groups.json`
are guesses to be replaced by the foundry's real ones.

---------------------------------------------------------------------------------------------------------------------------------------------

## 10. The base designs

<!-- BASES:BEGIN -->

Generated by `tools/mechanics/gen_mechanics.py`; do not edit by hand.

### Counts

| category | bases | mechanics |
|---|---|---|
| floor | 20 | 125 |
| puzzle | 20 | 125 |
| hazard | 20 | 125 |
| combat | 20 | 125 |
| risk | 20 | 125 |
| boss | 20 | 125 |
| travel | 20 | 125 |
| party | 19 | 125 |
| total | 159 | 1000 |

### Sources (48)

D&D: Dungeon Master's Guide (60), D&D: Monster Manual (47), Black Clover (45), D&D: Player's Handbook (45), D&D: Tomb of Horrors (44), Fairy Tail (38), Tensura (36), Tower of God (36), Hunter x Hunter (31), Naruto (31), Solo Leveling (30), Made in Abyss (27), Sword Art Online (27), D&D: Xanathar's Guide to Everything (26), Fate (26), Frieren (26), Overlord (25), D&D: Planescape (24), D&D: Ravenloft (24), Re:Zero (24), Attack on Titan (19), Berserk (19), Dungeon Meshi (18), Jujutsu Kaisen (18), KonoSuba (18), Magi (18), Dragon Ball (14), Akame ga Kill (13), Code Geass (13), Goblin Slayer (13), Bleach (12), Chainsaw Man (12), DanMachi (12), Demon Slayer (12), Dr. Stone (12), Fullmetal Alchemist (12), Hell's Paradise (12), Puella Magi Madoka Magica (12), Soul Eater (12), Log Horizon (7), Mushoku Tensei (7), No Game No Life (7), D&D: Out of the Abyss (6), Gurren Lagann (6), My Hero Academia (6), One Piece (6), Seven Deadly Sins (6), The Eminence in Shadow (6)

### The base designs

#### floor

| base | name | source | scope | trigger | counter | primitives |
|---|---|---|---|---|---|---|
| `rim_curse` | The Curse of the Rim | Made in Abyss | floor | timer | none | effect, starve |
| `mana_tide` | The Ebb and Flow of Magic | Black Clover | floor | timer | none | drain_mana, rukh |
| `gravity_change` | Gravity Change | Fairy Tail | floor | timer | none | gravity, sound |
| `silence_field` | Field of Silence | D&D: Player's Handbook | floor | timer | none | item_cooldown, sound |
| `hyperbolic_hours` | The Hours of the Hyperbolic Room | Dragon Ball | floor | timer | none | speed, effect |
| `living_labyrinth` | The Labyrinth Is Alive | DanMachi | floor | timer | none | wall_blink, rukh |
| `eclipse_dark` | The Eclipse Dark | Berserk | floor | timer | none | lights_out, effect |
| `weeping_sky` | The Weeping Sky | Frieren | floor | floor_enter | none | weather, effect |
| `red_gate_magma` | Red Gate Magma | Solo Leveling | floor | timer | none | floor_swap, sound |
| `rukh_current` | The Current of the Rukh | Magi | floor | timer | none | rukh, heal, effect |
| `wild_surge` | Wild Magic Surge | D&D: Player's Handbook | floor | timer | none | effect, tp_random, effect, give_item, lightning |
| `meshi_hunger` | The Hunger of the Depths | Dungeon Meshi | floor | timer | none | starve, effect |
| `aura_pressure` | Pressure of the Heavy Aura | Hunter x Hunter | floor | floor_enter | none | burden, effect |
| `listening_walls` | The Listening Walls | Goblin Slayer | floor | sprint | none | spawn_ambush, sound |
| `witchs_carpet` | The Witch's Labyrinth Carpet | Puella Magi Madoka Magica | floor | timer | none | floor_swap, disorient |
| `great_wind` | The Great Wind | Tensura | floor | timer | none | wind, rukh |
| `anti_magic_fog` | The Anti-Magic Fog | Black Clover | floor | area_enter | none | drain_mana, item_cooldown, rukh |
| `witch_miasma` | The Witch's Miasma | Re:Zero | floor | floor_enter | none | no_regen, effect, rukh |
| `shinsu_density` | Shinsu Density | Tower of God | floor | area_enter | none | speed, starve |
| `ice_hell` | Cocytus | Overlord | floor | timer | none | freeze, effect, floor_swap |

#### puzzle

| base | name | source | scope | trigger | counter | primitives |
|---|---|---|---|---|---|---|
| `colour_order` | The Rooms of Colour | D&D: Tomb of Horrors | room | area_enter | sequence | seal_doors, spawn_ambush |
| `mirror_demon_pattern` | The Mirror Demon's Pattern | Frieren | room | area_enter | mirror | seal_doors, rukh, doppelganger |
| `chain_spell_pillars` | The Pillars of the Chain Spell | Black Clover | room | area_enter | pillars_sync | seal_doors, lights_out, drain_mana |
| `scales_of_balance` | The Scales of Balance | D&D: Dungeon Master's Guide | room | area_enter | item_offer | seal_doors, ceiling |
| `lever_gallery` | The Gallery of Levers | Sword Art Online | room | area_enter | lever | seal_doors, wall_blink, spawn_ambush |
| `tsuzumi_rooms` | The Drum Rooms | Demon Slayer | room | floor_enter | mirror | seal_doors, wall_blink, sound |
| `return_by_death_memory` | The Memory of the Last Death | Re:Zero | room | floor_enter | mirror | seal_doors, rukh, tp_entry |
| `courier_of_the_pot` | The Courier of the Pot | Dungeon Meshi | room | area_enter | carry | seal_doors, burden, spawn_ambush |
| `sunlight_relay` | The Sunlight Relay | Dr. Stone | room | floor_enter | hold_light | seal_doors, lights_out, effect |
| `sphinx_riddle` | The Sphinx's Riddle | D&D: Monster Manual | room | area_enter | item_offer | seal_doors, spawn_elite, rukh |
| `true_path` | The True Path | D&D: Tomb of Horrors | room | sprint | sneak_through | pit, sound |
| `trick_tower_pairs` | The Tower of Two Doors | Hunter x Hunter | room | area_enter | split_plates | seal_doors, flood |
| `majority_door` | The Majority Door | Hunter x Hunter | room | area_enter | all_present | seal_doors, effect |
| `rune_lock` | The Runic Lock | Fate | room | area_enter | sequence | seal_doors, drain_mana, lightning |
| `equivalent_exchange` | The Law of Equivalent Exchange | Fullmetal Alchemist | room | area_enter | pay_item | seal_doors, place, spawn_guard |
| `door_of_teeth` | The Door of Teeth | D&D: Tomb of Horrors | room | area_enter | pay_hp | seal_doors, damage, sound |
| `test_of_position` | The Test of Position | Tower of God | room | floor_enter | all_present | seal_doors, swap_mob, spawn_group |
| `zodiac_gates` | The Zodiac Gates | Fairy Tail | room | area_enter | sequence | seal_doors, rukh, buff_mobs |
| `weeping_statues` | The Statues That Move | D&D: Ravenloft | room | sight | kill_all | seal_doors, spawn_guard, effect |
| `tarokka_spread` | The Tarokka Spread | D&D: Ravenloft | room | floor_enter | sequence | seal_doors, rukh, effect |

#### hazard

| base | name | source | scope | trigger | counter | primitives |
|---|---|---|---|---|---|---|
| `needle_lock` | The Poisoned Needle Lock | D&D: Dungeon Master's Guide | zone | proximity | none | effect, damage, sound |
| `rolling_boulder` | The Rolling Boulder | D&D: Xanathar's Guide to Everything | zone | proximity | none | dash, damage, quake |
| `flooding_vault` | The Flooding Vault | D&D: Dungeon Master's Guide | room | area_enter | none | seal_doors, flood, sound |
| `dart_wall` | The Dart Wall | D&D: Dungeon Master's Guide | zone | sight | none | projectile, effect |
| `fire_jets` | The Fire Jets | D&D: Xanathar's Guide to Everything | zone | timer | none | blast, ignite, sound |
| `sphere_of_annihilation` | The Sphere of Annihilation | D&D: Tomb of Horrors | zone | proximity | none | pull, damage, rukh |
| `crushing_ceiling` | The Crushing Ceiling | D&D: Tomb of Horrors | room | area_enter | none | seal_doors, ceiling |
| `poison_gas_room` | The Poison Gas Chamber | D&D: Xanathar's Guide to Everything | room | area_enter | none | seal_doors, effect, effect, rukh |
| `glyph_of_warding` | The Glyph of Warding | D&D: Player's Handbook | zone | proximity | none | spawn_group, rukh, sound |
| `spore_cloud` | The Spore Cloud | D&D: Out of the Abyss | zone | proximity | none | effect, effect, rukh, place |
| `explosive_tags` | The Explosive Tags | Naruto | zone | proximity | none | blast, sound |
| `wire_trap` | The Assassin's Wire | Akame ga Kill | zone | sprint | none | root, damage, spawn_ambush |
| `thorn_garden` | The Thorn Garden | Hell's Paradise | zone | proximity | none | place, effect, root |
| `domain_sure_hit` | The Sure-Hit Domain | Jujutsu Kaisen | room | area_enter | none | cage, damage, rukh |
| `bomb_devil` | The Bomb Devil's Gift | Chainsaw Man | zone | proximity | none | sound, blast, effect |
| `madness_wavelength` | The Wavelength of Madness | Soul Eater | zone | timer | none | disorient, effect, damage, rukh |
| `cursed_spirit_nest` | The Cursed Spirit Nest | Jujutsu Kaisen | zone | sprint | none | spawn_group, rukh, sound |
| `collapsing_cavern` | The Collapsing Stalactites | Made in Abyss | zone | timer | none | projectile, quake, sound |
| `reiatsu_pressure` | The Weight of Reiatsu | Bleach | room | area_enter | none | burden, effect, damage |
| `gravity_sink` | The Gravity Sink | Tensura | zone | proximity | none | gravity, pull, knockback |

#### combat

| base | name | source | scope | trigger | counter | primitives |
|---|---|---|---|---|---|---|
| `leader_first` | Kill the Shaman First | Goblin Slayer | room | area_enter | kill_all | tether, mark, rukh |
| `shield_switch` | The Barrier and the Switch | Sword Art Online | room | area_enter | lever | shield_mobs, spawn_guard, rukh |
| `spell_turning` | Spell Turning | D&D: Monster Manual | room | area_enter | kill_all | reflect, rukh |
| `wall_waves` | The Wave Over the Wall | Attack on Titan | room | area_enter | survive | seal_doors, summon_wave, title |
| `cursed_spear` | The Cursed Spear | Fate | room | area_enter | kill_all | curse_hand, spawn_guard, rukh |
| `turf_war_spill` | The Turf War Spills Over | Tensura | room | kill | kill_all | spawn_group, call_dm, buff_mobs |
| `dungeon_ambush` | The Ambush | D&D: Dungeon Master's Guide | room | area_enter | kill_all | lights_out, spawn_ambush, sound |
| `shadow_clone_duel` | The Clone Duel | Naruto | room | area_enter | kill_all | doppelganger, rukh, title |
| `gel_splitting` | The Splitting Gel | Tensura | room | area_enter | kill_all | split_on_death, spawn_elite, rukh |
| `wounded_prey` | The Frenzy of Wounded Prey | Berserk | room | hp_below | kill_all | enrage, buff_mobs, sound |
| `homunculus_core` | The Homunculus Core | Fullmetal Alchemist | room | timer | kill_marked | heal_mobs, rukh |
| `gate_guardian` | The Gate Guardian | Solo Leveling | room | proximity | kill_marked | spawn_elite, spawn_guard, sound |
| `nen_hunt` | The Nen Beast Hunt | Hunter x Hunter | room | area_enter | kill_all | mark, spawn_ambush, enrage |
| `floor_guardian_aura` | The Floor Guardian's Aura | Overlord | room | party_size | kill_all | buff_mobs, buff_mobs, rukh |
| `initiative_round` | The Rounds of Initiative | D&D: Player's Handbook | room | area_enter | kill_all | buff_mobs, buff_mobs, sound, spawn_guard |
| `witch_cult_horde` | The Witch Cult's Horde | Re:Zero | room | death | kill_all | spawn_ambush, rukh, sound |
| `planar_gate` | The Planar Gate | D&D: Planescape | room | timer | lever | summon_wave, rukh |
| `thrall_court` | The Court of Thralls | D&D: Ravenloft | room | proximity | kill_marked | spawn_group, spawn_elite, drain_mana |
| `shadow_garden_raid` | The Raid from the Shadows | The Eminence in Shadow | room | floor_enter | kill_all | lights_out, spawn_ambush, effect |
| `knights_formation` | The Magic Knights' Formation | Black Clover | room | proximity | kill_marked | spawn_guard, spawn_elite, tether |

#### risk

| base | name | source | scope | trigger | counter | primitives |
|---|---|---|---|---|---|---|
| `cursed_chest` | The Cursed Chest | D&D: Dungeon Master's Guide | zone | floor_enter | none | spawn_chest, rukh |
| `mimic_chest` | The Mimic | D&D: Monster Manual | zone | floor_enter | none | spawn_chest, rukh |
| `devils_bargain` | The Devil's Bargain | Black Clover | zone | floor_enter | pay_hp | spawn_chest, rukh |
| `coin_flip_altar` | The Coin-Flip Altar | No Game No Life | zone | interact | none | give_loot, lightning, rukh |
| `sacrifice_shrine` | The Sacrifice Shrine | Berserk | zone | floor_enter | pay_hp | spawn_chest, xp_orbs, rukh |
| `closing_vault` | The Closing Vault | DanMachi | room | area_enter | none | spawn_chest, spawn_chest, seal_doors, spawn_elite |
| `felt_toll` | The Loot Cellar's Toll | Re:Zero | zone | area_enter | pay_item | spawn_chest, spawn_guard |
| `wishing_well` | The Wishing Well | Fairy Tail | zone | interact | pay_item | xp_orbs, restore_mana, spawn_elite |
| `djinn_wish` | The Djinn's Lamp | Magi | zone | floor_enter | pay_xp | spawn_chest, give_loot, rukh |
| `geass_contract` | The Geass Contract | Code Geass | zone | interact | none | curse_hand, give_loot, buff_mobs |
| `poisoned_feast` | The Poisoned Feast | Dungeon Meshi | zone | item_use | none | heal, effect, give_item |
| `gem_heap` | The Heap of Gems | D&D: Tomb of Horrors | zone | proximity | none | pit, place, spawn_swarm |
| `chainsaw_contract` | The Contract with a Devil | Chainsaw Man | zone | interact | none | damage, effect, effect |
| `steal_skill` | The Thief's Game | KonoSuba | zone | proximity | none | take_item, spawn_elite, sound |
| `black_ledger` | The Black Ledger | D&D: Ravenloft | zone | floor_enter | pay_xp | spawn_chest, rukh |
| `vile_tome` | The Vile Tome | D&D: Planescape | zone | interact | none | xp_orbs, effect, drain_mana, rukh |
| `dragon_hoard` | The Sleeping Hoard | D&D: Monster Manual | zone | sprint | none | spawn_elite, blast, spawn_chest |
| `wand_of_wonder` | The Wand of Wonder | D&D: Dungeon Master's Guide | zone | interact | none | heal, lightning, spawn_swarm, launch, give_item |
| `double_dungeon_choice` | The Double Dungeon's Choice | Solo Leveling | room | floor_enter | kill_all | spawn_chest, spawn_guard, title |
| `rank_bet` | The Ranker's Bet | Tower of God | zone | interact | none | xp, xp_orbs, effect |

#### boss

| base | name | source | scope | trigger | counter | primitives |
|---|---|---|---|---|---|---|
| `super_form` | The Awakened Form | Dragon Ball | boss | boss_hp | none | shockwave, buff_mobs, buff_mobs, rukh |
| `lair_actions` | Lair Actions | D&D: Monster Manual | boss | timer | none | quake, lightning, knockback |
| `mythic_return` | The Mythic Return | D&D: Monster Manual | boss | boss_hp | none | spawn_group, rukh, sound |
| `frightful_presence` | Frightful Presence | D&D: Monster Manual | boss | area_enter | none | effect, effect, rukh |
| `minion_tether` | The Tethered Retinue | Overlord | boss | floor_enter | none | spawn_guard, tether, rukh |
| `commandment_patience` | The Commandment of Patience | Seven Deadly Sins | boss | idle | none | damage, knockback, sound |
| `titan_throw` | The Beast Titan's Volley | Attack on Titan | boss | timer | none | projectile, sound |
| `infinity_shuffle` | The Infinity Castle Shuffle | Demon Slayer | boss | timer | none | tp_random, sound, rukh |
| `charybdis_flood` | The Flooded Arena | Tensura | boss | boss_hp | none | flood, sound, rukh |
| `magma_phase` | The Red Gate's Floor | Solo Leveling | boss | boss_hp | none | floor_swap, sound, rukh |
| `bankai_dark` | The Bankai Dark | Bleach | boss | boss_hp | none | lights_out, effect, buff_mobs, rukh |
| `shifting_arena` | The Shifting Arena | Tower of God | boss | floor_enter | none | wall_blink, rukh |
| `gehenna_collapse` | The Collapse of Gehenna | D&D: Planescape | boss | boss_hp | none | ceiling, quake, rukh |
| `tailed_bomb` | The Tailed Beast Bomb | Naruto | boss | timer | none | sound, rukh, blast |
| `explosion_magic` | Explosion! | KonoSuba | boss | timer | none | sound, title, blast, rukh |
| `boss_domain` | The Boss's Domain | Jujutsu Kaisen | boss | boss_hp | none | cage, damage, rukh |
| `conquerors_haki` | The Conqueror's Haki | One Piece | boss | area_enter | none | shockwave, effect, effect |
| `quirk_awakening` | The Quirk Awakening | My Hero Academia | boss | boss_hp | none | buff_mobs, enrage, shockwave, rukh |
| `walpurgis_flight` | The Walpurgis Flight | Puella Magi Madoka Magica | boss | timer | none | gravity, rukh, sound |
| `dragon_king_roar` | The Dragon King's Roar | Fairy Tail | boss | timer | none | sound, wind, blast, drain_mana |

#### travel

| base | name | source | scope | trigger | counter | primitives |
|---|---|---|---|---|---|---|
| `abyss_elevator` | The Elevator of the Abyss | Made in Abyss | zone | area_enter | none | slider, sound |
| `sky_updraft` | The Sky Dragon's Updraft | Fairy Tail | zone | proximity | none | wind, gravity, rukh |
| `one_way_door` | The One-Way Door | D&D: Tomb of Horrors | room | area_enter | lever | seal_doors, sound |
| `gate_network` | The Gate Network | Log Horizon | zone | interact | none | rukh, tp_room, sound |
| `abyss_shaft` | The Vertical Shaft | Made in Abyss | zone | proximity | none | gravity, wind, sound |
| `steam_conveyor` | The Steam Conveyor | Dr. Stone | zone | area_enter | none | wind, sound |
| `frozen_lake` | The Frozen Lake | Frieren | zone | area_enter | none | floor_swap, dash |
| `slime_bounce` | The Slime Bounce Pads | Tensura | zone | jump | none | place, launch |
| `kamui_warp` | The Spiral Warp | Naruto | zone | proximity | none | rukh, tp_random, sound |
| `rainbow_bridge` | The Bridge of Light | Fate | zone | area_enter | none | pit, trail, rukh |
| `underground_river` | The Underground River | D&D: Xanathar's Guide to Everything | zone | area_enter | none | flood, wind, sound |
| `teleportation_circle` | The Teleportation Circle | D&D: Dungeon Master's Guide | zone | area_enter | none | rukh, tp_room, sound |
| `shifting_stairs` | The Shifting Stairs | Tower of God | room | floor_enter | none | wall_blink, rukh |
| `flying_carpet` | The Flying Carpet | Magi | zone | interact | none | slider, rukh |
| `anywhere_gate` | The Gate of Nazarick | Overlord | zone | interact | none | rukh, tp_anchor, sound |
| `floating_islands` | The Floating Islands | Sword Art Online | room | floor_enter | none | place, gravity, rukh |
| `trapdoor_chute` | The Trapdoor Chute | D&D: Player's Handbook | zone | proximity | none | pit, tp_room, sound |
| `airship_rails` | The Airship Rails | Black Clover | zone | interact | none | slider, rukh |
| `odm_anchors` | The Anchor Posts | Attack on Titan | zone | jump | none | pull, launch, dash |
| `portal_maze` | The Portal Maze | D&D: Planescape | room | area_enter | none | rukh, tp_random, tp_room |

#### party

| base | name | source | scope | trigger | counter | primitives |
|---|---|---|---|---|---|---|
| `master_servant_link` | The Master and Servant Link | Fate | floor | floor_enter | none | share_damage, rukh |
| `chain_jail` | The Chain Jail | Hunter x Hunter | floor | area_enter | none | link, rukh |
| `revival_item` | The Revival Window | Sword Art Online | floor | floor_enter | none | downed, rukh |
| `death_saves` | The Death Saves | D&D: Player's Handbook | floor | area_enter | none | downed, effect |
| `party_pack` | The Party's Pack | Frieren | floor | floor_enter | none | burden, effect |
| `fallen_comrade` | The Fallen Comrade's Rage | Akame ga Kill | room | death | none | effect, effect, effect |
| `four_key_door` | The Four-Key Door | D&D: Dungeon Master's Guide | room | area_enter | pillars_sync | seal_doors, effect |
| `lantern_bearer` | The Lantern-Bearer | D&D: Player's Handbook | room | floor_enter | hold_light | seal_doors, lights_out, effect, link |
| `bell_test` | The Bell Test | Naruto | room | area_enter | all_present | seal_doors, spawn_guard, link |
| `decoy_mark` | The Decoy Mark | Code Geass | room | area_enter | none | mark, spawn_ambush, buff_mobs |
| `relay_labyrinth` | The Relay Labyrinth | Mushoku Tensei | room | area_enter | carry | seal_doors, burden, spawn_group |
| `shared_mana_pool` | The Shared Mana Pool | Black Clover | floor | timer | none | drain_mana, restore_mana, rukh |
| `friendly_fire` | The Friendly Fire | KonoSuba | room | area_enter | none | blast, sound |
| `lone_hunter` | The Lone Hunter's Handicap | Solo Leveling | room | area_enter | none | effect, effect, title |
| `spiral_crowd` | The Spiral Crowd | Gurren Lagann | room | party_size | none | shockwave, rukh, sound |
| `guild_masters_mark` | The Guild Master's Mark | Fairy Tail | room | area_enter | none | mark, effect, rukh |
| `executioner_pairs` | The Executioner and the Convict | Hell's Paradise | floor | area_enter | none | link, tp_swap, rukh |
| `soul_resonance` | The Soul Resonance | Soul Eater | room | area_enter | pillars_sync | seal_doors, shockwave |
| `trust_fall` | The Trust Fall | Tower of God | room | area_enter | split_plates | seal_doors, pit, effect |

<!-- BASES:END -->
