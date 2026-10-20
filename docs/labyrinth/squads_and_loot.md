# Advanced squads and functional anime loot (`labyrinth.squad`, `labyrinth.loot`)

Round 1.99.0, builder "squad-loot". Two packages: `com.newuniverse.nusmp.labyrinth.squad` (coordinated 5 to 8 member squads for the Dungeon Master) and
`com.newuniverse.nusmp.labyrinth.loot` (a global loot modifier, thirty foods and elixirs, twenty-four pieces of pre-rolled gear, twenty-four monster parts, their
recipes and their sale at the Hub). Configs `nusmp-labyrinth-squad-server.toml` and `nusmp-labyrinth-loot-server.toml`. Lang fragment `tools/lang/squad_loot.json`.
Nothing here has been run in a game; section 9 is the honest list of what is therefore unverified.

## 1. What it does, in one page

```
Dungeon Master rolls an encounter  ->  the old plan (which monsters, where)          [kept, unchanged]
  squads.enabled?  ->  SquadBridge.shape: the plan is fitted to a squad of 5 to 8      [new]
                       SquadPlan: roles by band, depth, party, theme                   [new, pure]
  each monster is stamped with the squad id BEFORE it joins the world                  [new, one line in the spawner]
  EntityJoinLevelEvent: the monster is attached, tagged, given the squad goals         [new]
  SquadHook.end: roles are handed to the monsters that FIT them (RoleFit)              [new]
  every 10 ticks Squad.think: target, ward, leader, morale                             [new]
  goals: intercept (tank) / flank / support-retreat / formation / focus fire / flee    [new, added beside the mob's own]

a monster dies to a player  ->  AnimeLootModifier (global loot modifier, nusmp:anime_loot)
  KillLoot (pure): family x depth band x luck x rates x caps  ->  parts, a meal or elixir, pre-rolled gear
  LootDrops: turns the answer into stacks (gear rolled once, stats written as vanilla attribute modifiers)
  then: foods are eaten, parts are crafted / worked at the anvil / sold to the Hub Merchant, gear is worn, wielded or put in a Curios slot
```

## 2. Part 1: squads

### 2.1 Why each decision

* **Why a squad and not more mobs.** A horde is N mobs that each pick the nearest player: N identical answers, so the fight is a wall of bodies. A squad is the same
  number of bodies with different jobs, so the fight has a shape (a tank holds the line, flankers go round, a healer has to be found). The Director still decides which
  monsters come and where; the squad only decides how many (5 to 8) and what each does. No entity type is ever changed.
* **Why the plan is pure.** `SquadPlan` (the composition for a depth, theme, band and party), `SquadFormation`, `SquadGeometry` (intercept point, flank point, healer
  distance), `RoleMotion` (what a role does in a given moment), `SquadMind` (the ward, the focus target, the morale) and `RoleFit` (which monster gets which role) take
  numbers and return numbers. A squad cannot be played by its author, so every rule of the brief ("the intercept point is 1.5 blocks from the ward on the segment to
  the target", "a flank point is 110 to 150 degrees off the target's facing", "a healer keeps 8 to 12 blocks from the nearest threat") is a unit test to the decimal.
* **Why the roles are handed out after the monsters exist.** The Director already chose the monsters from its tables and factions; a squad must not override that. So
  `RoleFit` reads what each monster IS (health, armour, speed, ranged attack, Foundry archetype) and gives the tank role to the tankiest body, the healer to a caster, the
  shooter to the one with a ranged attack, the flank to the quickest light body. Greedy in role order, ties to the lowest index: the same mobs always get the same roles.
* **Why shared state.** "Who leads, who is the target, who needs covering, has the tank engaged, did the nerve break" is decided once per think by `Squad`, and the goals
  only read it. Six mobs that each decide for themselves are a horde again. The squad holds its members weakly (it must never keep a mob alive or a chunk loaded).
* **Why 10 ticks, staggered.** The whole cost of all squads is one tick handler. Each squad thinks every `thinkEveryTicks` (10), offset by a hash of its id, so forty
  squads do four passes a tick, not forty every tenth tick.
* **Why stamp before join.** Setting the squad id in the monster's persistent data before it enters the world means the join event is the one place that attaches it. A
  monster that comes back from a saved chunk takes the same path, so a reloaded squad member is not a second-class member (`SquadManager.adopt`).
* **Why goals are added, never replaced.** A vanilla zombie, a Tensura monster and a Mob Foundry mob (six style goals of its own) keep their own way of fighting; the squad
  goals sit at priorities just above the usual melee and wander goals and answer "no" for a role that is not theirs.
* **Why a fallback driver.** Goal lists are protected fields and the mod has no access transformer, so they are read by name with reflection, like the Dungeon Master's
  wanderers and the shadow minions already do. A mob whose lists cannot be read (a brain-driven Tensura mob) is steered by `SquadManager.drive` from the very same pure
  decisions, so a squad degrades to "coordinated without goals", never to "uncoordinated".
* **Why the healer is bound by the mod's own law.** `SupportCast` heals through `BalanceLaw.heal`, so the 50 percent cap per cast and the decay of repeated heals bind
  it, and it never heals a boss (`BalanceLaw.isBoss`): a squad that heals its boss would make an instakill-proof boss. Its only effects are strength and resistance.
* **Why morale.** A fight with no end except death is a grind. When the leader falls, the squad is down to its last member, or its survivors are fewer than
  `moraleFraction` of its size, the survivors react by config `moraleBreak`: `flee` (the highest-priority goal runs them 14 blocks away from the party), `enrage` (an
  attribute bonus for `enrageTicks`), or `mixed` (front line and melee enrage, shooters and healers flee).

### 2.2 The six roles and what they do

| Role | Where it stands | What it does |
| --- | --- | --- |
| TANK | front arc | steps to the point 1.5 blocks from the ward on the segment to the target, then holds |
| MELEE | behind the tanks | hits the squad's focus target |
| FLANKER | the wings | waits until a tank has engaged, then circles to a point 110 to 150 degrees off the target's facing, staggered by slot; gives up when watched from close or when hurt |
| RANGED | behind the line | shoots the focus target |
| SUPPORT | rear | keeps 8 to 12 blocks from the nearest threat, runs to the squad's centre, heals the most wounded ally under 70 percent, else buffs up to three allies |
| LEADER (optional, harder or deeper fights) | just behind the tanks | the others follow it while nothing is in sight; its death shakes the squad's morale |

Composition (`SquadPlan`): never below 5 or above 8 members; one or two tanks and one or two supports; at least one shooter and one flanker once there is room; the faction
tilts the mix (ogres bring tanks, goblins bring flankers and archers, cultists bring healers, spirits bring casters). Same seed, same squad.

### 2.3 Hooks and the shared files they touch

* `EncounterDirector`: `SquadBridge.shape(...)` after the plan is made, `SquadHook.abort()` when no monster could be placed, `SquadHook.end(level, made)` once the monsters
  exist (a squad of fewer than three is dissolved: two monsters are not a formation), the squad in the encounter's log line, and `if (SquadHook.followsLeader(m)) continue;`
  in `hunt()` so followers do not each run their own hunt.
* `EncounterSpawner.finish`: `SquadHook.stamp(mob)`.
* The System panel gets one toast line, "Squad: N: roles", through the same channel the floor quests use (`SquadAnnounce`), plus one when a squad's nerve breaks.
* With `squads.enabled = false` none of it runs and the Director spawns the groups its tables give, exactly as before.

### 2.4 Config `[squads]`

`enabled` (true), `moraleBreak` (mixed), `moraleFraction` (0.34), `thinkEveryTicks` (10), `healCooldownTicks` (80), `buffCooldownTicks` (200), `healFraction` (0.15),
`enrageTicks` (600), `maxSquads` (96), `announce` (true).

## 3. Part 2: functional anime loot

### 3.1 Why each decision

* **Why one roster.** Eighty items are each named in a dozen places (registry, drop tables, sell prices, recipes, icons, models, lang, tests). Writing them in a dozen
  places is how one gets lost. `tools/loot/roster.py` is the one list; `tools/loot/gen_data.py` writes `data/nusmp/loot/roster.json`, the recipes, the tag, the flat item
  models, the lang fragment and the generated `LootText.java`; the Java reads `roster.json` from the class path (like `GradeDamage` reads its table), so it works when the
  registry is built at mod construction, long before a data pack loads. Add a row to the python and every consumer gets it.
* **Why a global loot modifier.** It works on loot that has ALREADY been rolled, whatever rolled it: a monster of another mod, one whose table a data pack edited, one with no
  table, a Foundry mob. It adds beside what the table gave (add the new, keep the old). The place rules are code, not a condition list, because a condition list cannot say
  "a monster of ours". Registered as `nusmp:anime_loot`, switched on in `data/neoforge/loot_modifiers/global_loot_modifiers.json`.
* **Why the kill rule.** Only a kill whose last blow was a player's counts (the vanilla `LAST_DAMAGE_PLAYER` parameter), so a mob grinder nobody fights drops nothing extra;
  tamed pets, players and Foundry summons never drop. The place counts: the mod's dungeon dimensions (not the Hub), or, by `oursOutsideDungeon`, the mod's own monsters
  anywhere.
* **Why the drop decision is pure.** `KillLoot.roll` is one function of a dice, a family, a depth, a luck and the rates, with the rules an owner will rely on: at most
  `maxDropsPerKill` (3) per kill (a boss may leave three more), never the same item twice, at most one piece of gear and one food, only the monster's family plus the
  generic rows, a hard ceiling of 50 percent on any row, deeper floors and luck a little richer. A boss ALWAYS leaves two rare parts, a piece of gear one tier better and a
  strong tonic or elixir.
* **Why families and bands.** The dungeon has 2000 floors and a table cannot have 2000 rows, so depth is cut into five bands (floors 1-10, to 30, to 100, to 300, deeper).
  The family comes from the Dungeon Master's faction tag first, then the entity-type tag `nusmp:mega/<faction>`, then the words of the registry path (`ghoul` is undead);
  anything else is `any` and only the generic rows serve it. Eleven families: goblin, ogre, undead, beast, slime, construct, cultist, demon, elemental, aquatic, insect.
* **Why gear is pre-rolled.** The stats are decided once, when the piece is made, and are the same forever. `GearRollMath` is one pure function of (seed, floor, rarity,
  kind, primary stat, damage headroom): two to four lines by rarity, a primary stat first, the others distinct, each inside the range for the rarity and depth, rounded to the
  stat's step and never over its cap. The same seed gives the same piece (a test, an admin command and a bug report all need that).
* **Why a weapon's damage follows the owner's grade table.** A weapon's attack damage is a share of the headroom between its base damage and the top of its grade's range
  (`relics/GradeDamage`: normal 20-30, special 35-55, rare 60-85). Common and uncommon weapons are normal grade, rare and epic special, legendary rare. A rolled weapon can
  be better than another of its tier, never a different tier.
* **Why the roll is written as vanilla attribute modifiers.** In 1.21 an attribute-modifier component is the one thing the game applies in any slot, so the numbers in the
  tooltip are the numbers on the wearer. Setting the component REPLACES an item's own modifiers, so the factory starts from the item's default list; every modifier id
  carries the slot group so four armour pieces worn together each keep their line.
* **Why trinkets need a bridge.** Vanilla applies no modifier for an item in a Curios slot. `TrinketBridge` reads the worn trinkets (Curios by reflection, the off hand
  without it) once a second and sets one transient modifier per stat, replacing it when the sum changed and removing it when the trinket came off.
* **Why a critical-chance handler.** There is no critical-chance attribute, so that line stays a number on the stack that `CritHandler` reads from the weapon, the armour
  and the worn trinkets on `CriticalHitEvent` (capped at 60 percent; a vanilla critical is never turned off).
* **Why anvil uses.** A part that is only an ingredient is half a part. Three work at the vanilla anvil through `AnvilUpdateEvent`: the Shadow Core Shard (or the System's
  Shadow Core) works one extra stat line into a piece of gear, once; the Golem Core fully mends anything damaged; the Venom Sac coats a weapon for twenty poisoning blows.
* **Why selling at the Hub.** The Hub shop only sells. Sneak and use on the Merchant with loot in hand buys the stack for its roster value (a piece of gear for its base value
  times its rarity's factor) in the same Tensura coins as everything else (`Currency.give`), times `sellFactor`. A plain click opens the shop as before.
* **THE HUNGER BAN.** Nothing here drains hunger. Foods are real `FOOD` components that only restore (nutrition never negative); no roster effect is a hunger effect, and the
  factory skips `minecraft:hunger` even if one were written; no stat kind touches the food bar; no code in the loot or squad packages uses exhaustion or sets the food level.
  Elixirs restore a share of the maximum magicules or aura through Tensura's storage, never past it. Two tests enforce it (`LootRosterTest`, `LootAssetsTest`) and so does
  the self-test.

### 3.2 What is in the roster (80 items)

| Kind | Count | Items |
| --- | --- | --- |
| Foods and elixirs | 30 | Dungeon Ration Stew, Goblin-Ear Jerky, Adventurer's Hardtack, Slime Jelly Pudding, Mandrake Stew, Walking Mushroom Skewer, Orc-Haunch Roast, Wyvern Steak, Bone Broth, Kraken Fritter, Dryad Honey Cake, Frostbite Sorbet, Ember Chili, Cave Mushroom Soup, Falna Cutlet, Hearth Bread, Delver's Trail Mix, Golem-Stone Biscuit, Spirit Tea, Healer's Vial, Fortitude Flask, Swiftfoot Tonic, Berserker's Draught, Bulwark Tonic; the elixirs (they restore magicules or aura): Mana Elixir, Greater Mana Elixir, Aura Tonic, Wisp Draught, Starlit Nectar, Sage's Elixir |
| Monster parts | 24 | Goblin Ear, Goblin Tooth, Orc Tusk, Troll Hide, Bone Shard, Ghoul Claw, Lich Dust, Wraith Essence, Slime Gel, Gel Core, Rusted Cog, Golem Core, Cursed Cloth, Cultist Sigil, Imp Horn, Shadow Core Shard, Spirit Wisp, Ember Heart, Beast Fang, Drake Scale, Tide Pearl, Kraken Ink, Chitin Plate, Venom Sac |
| Crafted | 2 | Shadowsteel Ingot, Bone Arrow (a real arrow: one more base damage than flint) |
| Weapons | 8 | Goblin-Fang Dagger, Delver's Longsword, Bone Lance, Orc Cleaver, Shadowsteel Katana, Drakefang Greatsword, Wisp Staff, Golem Maul |
| Armour | 8 | Drake-Scale and Shadowsteel sets, helm / chest / legs / boots each |
| Trinkets | 8 | Goblin Ear Ward, Orc Tusk Ring, Lich Dust Pendant, Wisp Lantern, Golem Core Belt, Wraith Cloak, Cultist Sigil Ring, Tide Pearl Amulet |

Thirty-seven recipes in `data/nusmp/recipe/loot_*.json`. The stat kinds a roll can produce: attack damage, attack speed, armour, max health, movement speed, critical
chance, maximum magicules, magicule regeneration. Five rarities (common to legendary), two to four lines.

The one part that shares a word with an older item: the System's `shadow_core` is kept, and the monster part is "Shadow Core Shard". A first draft named a slime part
`slime_core`, which is an existing item of the sentient dungeon; it is `gel_core` ("Gel Core") here, and the id audit that found it checked all eighty ids against every
model, texture, mesh, lang key and registry string of the mod.

### 3.3 Weapons, trinkets and armour as models (the owner's repo rules 6 and 7)

* Every weapon is listed in `item/WeaponEngravings.java` (engravings that fit its lore, applied once), is a real 3D model built in the owner's model method
  (`tools/gen_loot_models.py` on the `tools/relics` kit; the hook `python3 -B tools/gen_weapon_models.py loot` runs it), keeps a flat sprite as its inventory icon
  (`<id>_icon.json`), has a painted skin and an additive glow skin, a class row in both `weapon_classes.json` copies, a display preset where it is long, and a Better Combat
  attributes file. `LootItems.renderables()` hands the sixteen 3D pieces to `WeaponRenderer.WEAPONS`.
* The eight trinkets are also 3D (a ring on the hand, an amulet, a cloak), as `handitem` class rows, and sit in the Curios tags (`ring`, `necklace`, `charm`, `belt`,
  `back`). The weapons are in the `belt` and `back` tags like every other weapon of the mod, so they sheathe.
* Armour is an ordinary `ArmorItem` and is drawn by the worn-model library (the mod's armour renderer draws any armour of its namespace) as a generic look; the vanilla
  box in the set's colours (four layer textures) is hidden on a player by `client/loot/LootClient` and shows on an armour stand or a mob.
* Foods and monster parts are flat item sprites (one hand-painted 16 x 16 icon each, `tools/gen_loot_icons.py`).

### 3.4 Config `[loot]`

`enabled` (true), `dropRate` (1.0), `gearRate` (1.0), `foodRate` (1.0), `maxDropsPerKill` (3), `bossBonus` (3.0), `oursOutsideDungeon` (true), `sellToMerchant` (true),
`sellFactor` (1.0), `criticalChanceLines` (true), `trinketsApply` (true).

## 4. New files by area

| Area | Where |
| --- | --- |
| Squad core and runtime | `src/main/java/com/newuniverse/nusmp/labyrinth/squad/` (roles, plan, formation, geometry, motion, mind, fit, config, squad, manager, hook, events, announce, goal installer, six goals, support cast, self-test, registry) and `labyrinth/mega/enc/SquadBridge.java` |
| Loot core and runtime | `src/main/java/com/newuniverse/nusmp/labyrinth/loot/` (pure: rarity, stat kinds, roll, roll maths, bands, families, roster, tables, kill loot, value; game: config, components, effects, mana sips, gear stacks and tooltip, the item classes, registry, drops, the modifier, anvil uses, crit handler, trinket bridge, sale, creative tab, self-test, generated text) |
| Client | `src/main/java/com/newuniverse/nusmp/client/loot/LootClient.java` |
| Tests | `src/test/java/com/newuniverse/nusmp/labyrinth/squad/` (4 classes), `src/test/java/com/newuniverse/nusmp/labyrinth/loot/` (4 classes) |
| Tools | `tools/loot/roster.py`, `tools/loot/gen_data.py`, `tools/gen_loot_icons.py`, `tools/gen_loot_models.py` |
| Generated data | `data/nusmp/loot/roster.json`, `data/nusmp/recipe/loot_*.json`, `data/nusmp/tags/item/loot/shadow_cores.json`, `data/nusmp/loot_modifiers/anime_loot.json`, `data/nusmp/weapon_attributes/` (8 files) |
| Generated assets | 80 flat item models and icons, 16 meshes with skins, glow skins, sprites and `_icon` models, 4 armour layer textures, the `mana_surge` effect icon |
| Lang | `tools/lang/squad_loot.json` (merge into `en_us.json` at integration) |

## 5. Edits to existing files (all append-only)

| File | Edit |
| --- | --- |
| `labyrinth/mega/MegaSystem.java` | an init line and a register line each for `SquadRegistry` and `LootRegistry` |
| `NUSMP.java` | two `registerConfig` lines (squad, loot), one `LootClient.init` line in the client block |
| `labyrinth/mega/enc/EncounterDirector.java` | the hooks of section 2.3 |
| `labyrinth/mega/enc/EncounterSpawner.java` | `SquadHook.stamp(mob);` in `finish()` |
| `item/WeaponEngravings.java` | eight map entries appended (and a comma on the line before) |
| `client/WeaponRenderer.java` | `LootItems.renderables().stream()` appended to the `WEAPONS` stream |
| `client/relics/worn/lib/ArmorItemRenderer.java` | the loot armour is left out of its extension registration (one condition), because NeoForge allows one extension per item and `LootClient` registers the loot armour's own |
| `data/neoforge/loot_modifiers/global_loot_modifiers.json` | `"nusmp:anime_loot"` appended |
| `data/curios/tags/item/{ring,necklace,charm,belt,back}.json` | the eight trinkets and the eight weapons appended |
| `assets/nusmp/weapon_classes.json`, `tools/relics/weapon_classes.json`, `tools/relics/display_presets_manifest.json`, `tools/bettercombat/manifest.json` | sixteen class rows, the display presets and the Better Combat rows appended |
| `tools/gen_weapon_models.py` | a four-line branch at the top of `main` for the `loot` argument |

## 6. NEW / REPLACED / KEPT

| | What |
| --- | --- |
| NEW | the squads (all of `labyrinth/squad`, `SquadBridge`, the config, the System line, the tests, the self-test `enc.squads`); the global loot modifier and everything under `labyrinth/loot`; 30 foods and elixirs, 24 monster parts, 2 crafted items, 8 weapons, 8 armour pieces, 8 trinkets (80 items); the `nusmp:gear_roll` data component, the `nusmp:mana_surge` effect, two armour materials, the "Dungeon Loot" creative tab; 37 recipes; the anvil uses, critical-chance lines, trinket bridge and Hub sale; the self-test `loot.functional` |
| REPLACED | nothing. The owner's spec asked for squads "instead of random horde spam"; the random path is not removed, it is behind `squads.enabled = false` |
| KEPT | the Director's planning, tables, faction picker, spawner, hunt and sweep; the old spawn path; the System's `shadow_core` item and the sentient dungeon's `slime_core` (my slime part is `gel_core`); the existing chest, relic, skill-book, rift and Hub-coin loot modifiers; the Hub shop; every existing weapon, engraving, class row, Better Combat file and Curios tag entry; the Time Magic VFX (untouched) |

## 7. Tests

* JUnit (run with the dev shim, 79 passed): `SquadPlanTest` (every depth 1 to 1000, every party size), `SquadGeometryTest`, `RoleMotionTest`, `SquadMindTest`,
  `GearRollMathTest` (determinism, line counts, ranges and caps at every depth and rarity, the primary line first, grade headroom, rarity weights, tempering), `KillLootTest` (caps, duplicates, families, depth, luck, bosses), `LootRosterTest` (the counts,
  unique ids, the hunger ban, drop rows, engravings, sets, a use for every part), `LootAssetsTest` (model, sprite, lang, mesh, skins, class rows, Better Combat, engravings,
  recipes name real items, every part has a use, the generated text equals the lang fragment, the modifier is switched on, Curios tags, no hunger call in the code, no tool
  names). The hunger scan was checked with a negative control (a file containing `addExhaustion` fails it).
* Existing tests re-run and passing: `WeaponMeshesTest`, `WeaponClassesTest`, `WeaponAttributesTest`, `BowDisplayGuardTest`, `AlignmentDataTest`, `EncounterDataTest`.
* In-game self-tests, run by `/multiverse mega selftest` and the CI server smoke test: `enc.squads` (a forced ambush spawns a squad of 5 to 8 with tags, roles and goals;
  a member is killed; the leader is killed and every survivor has a reaction) and `loot.functional` (all 80 items are registered, every food restores and carries no hunger, every
  gear piece rolls and keeps its grade, the modifier drops in the dungeon and not outside it, the anvil uses work, a trinket applies and comes off, the recipes are loaded).

## 8. Regenerating

```
python3 -B tools/loot/gen_data.py          roster.json, recipes, tag, flat models, lang fragment, LootText.java
python3 -B tools/gen_loot_icons.py         the 80 icons, the armour layers, the effect icon
python3 -B tools/gen_weapon_models.py loot the 16 meshes, skins, sprites, class rows, display presets (LOOT_PREVIEW=<dir> also renders previews)
```

## 9. What could NOT be verified, and what was cut or interpreted

Could not verify (nothing was run in a game; the build here has no NeoForge or Minecraft jars):

* The whole thing compiled only against an API stub set (a few "abstract; cannot be instantiated" errors in the type-check come from the stubs and also occur in old code);
  there was no `gradlew build`, so the first real compile is the owner's.
* Goal installation by reflection on the protected `goalSelector` / `targetSelector` fields of Tensura's brain-driven mobs; the fallback driver is written for it but was
  not seen to run. Whether a squad's flank and intercept read well in a real fight (speeds, stagger, the 110 to 150 degree arc) is a feel the numbers cannot prove.
* The Curios attribute path: Curios is read by reflection (the build cannot reach its repository), so the slot listing, the `render` flag and the once-a-second modifier
  application of the trinkets are unverified against a real Curios.
* `RegisterClientExtensionsEvent`: that one extension per item is the rule (assumed from the API's `isItemRegistered`), and that returning the empty armour model hides the
  vanilla box exactly when the worn library draws the piece.
* The Better Combat files and the display presets were generated by the mod's own scripts and checked by its tests, not seen in game; the previews are software renders of
  the meshes and do not show the additive glow.
* Tensura's storage read and write for the elixirs, the `mana_surge` attribute on the magicule regeneration multiplier, and the engraving ids on the installed Tensura (an id
  Tensura does not have is skipped by `WeaponEngravings`).
* The recipe, loot-modifier and item-model JSON parsing in a real data load; the Hub Merchant click order (the handler runs at high priority before the Merchant's own).
* Performance with ninety-six live squads.

Cut or interpreted:

* Rule 7 (real 3D models) is applied to every weapon and every trinket. Armour is 3D through the generic worn-model library with a sprite icon, not a bespoke mesh per piece;
  foods and monster parts are flat sprites, because a bowl of stew has no hand model worth building.
* Trinkets need Curios; without it the off hand stands in for the slot.
* The Hub shop's own offers were not extended with the foods; the loot is sold TO the Merchant, not bought from it.
* A weapon's engravings are the lore ones in the roster; the level cap of an engraving on the installed Tensura clamps them.
