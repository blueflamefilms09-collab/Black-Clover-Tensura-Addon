# The hunger ban (round 1.99.0)

The owner's spec, verbatim: **"ABSOLUTELY NO ACCELERATED HUNGER OR EXHAUSTION MECHANICS.** The dungeon must not include any environmental hazards, curses, or
systems that make the player's hunger bar drain faster or force them to eat more frequently. D&D-style short/long rests in safe zones are strictly for recovering
HP, Mana and ability cooldowns."

The owner asked for this explicitly, so the old hunger drains were removed (the one exception to "add the new, keep the old"). Every class, id, registry entry, page,
enum value and data component stays; only the behaviour that touched the food bar was neutralised, or converted to a cost the system already has (magicules, a
non-hunger status effect). Food items still RESTORE hunger normally.

## NEW / REPLACED / KEPT

| Part | Status | What |
|---|---|---|
| `balance/HungerLaw` (pure) and `balance/HungerBan` (game side) | NEW | the arithmetic of the ban (what counts as the bar getting worse, the banned effect id) and the one question the effect-by-id paths ask (`refuses`); `guard(ServerPlayer)` is armed/checked by tests only, there is no tick handler and no event |
| `NoHungerDrainTest`, `HungerLawTest` | NEW | the source scan that keeps the writers removed; the arithmetic |
| self-test `dungeon.nohunger` (in `MechanicsSelfTests`) and food-bar asserts in `safe.rest_short`, `safe.rest_long` and the curse self-test | NEW | the old triggers driven on a stand-in player's real `FoodData` |
| the Labyrinth hunger multiplier (`DungeonHunger`, `GloomClock.extraExhaustion/settle`, config key `hungerDrain`), the hungry Slowness | REPLACED (removed) | gone; the classes, the registration and the gloom (light) clock stay |
| every other drain listed below | REPLACED | neutralised or converted, class and id kept |
| feeding, saturating, cleansing and "mention only" code | KEPT | see the second and third table |

## 1. What drains or accelerates hunger: removed or converted

| Where | What it did | What it does now |
|---|---|---|
| `labyrinth/dnd/economy/DungeonHunger.java` | Labyrinth hunger x`hungerDrain` (1.5) through `causeFoodExhaustion`; Slowness I at 3 shanks or fewer | does not read or write the food bar. Only the gloom (Darkness + Weakness after 20 s in the dark with no light) is left. New public `sample(player, level)` is the one-second step the self-test drives |
| `labyrinth/dnd/economy/GloomClock.java` | the hunger meter (`extraExhaustion`, `settle`, `starving`, `STARVING_FOOD`) | removed; `GloomClockTest` pins that they stay removed |
| `labyrinth/dnd/DndConfig.java` | `hungerDrain` key | removed (an old toml with the key is simply ignored) |
| `labyrinth/mega/mechanics/PrimsCreature.java` op `starve` | `setFoodLevel(food - amount)` | drains magicules: 0.5% of the maximum per point of the old amount, at most 10% (`STARVE_MANA_SHARE`). The op id and the 1000-mechanics library stay |
| `labyrinth/mega/mechanics/PrimUtil.addEffect` | (used by every `effect` line of the library) | refuses `minecraft:hunger` for a player. 26 library mechanics name the Hunger effect or `starve` (`floor.meshi_hunger` x6, `floor.rim_curse` x6, `floor.shinsu_density` x5, `floor.hyperbolic_hours.e_rush`, `party.party_pack` x7, `puzzle.majority_door.e_breathless`); their JSON is untouched (the behaviour signatures stay valid), the engine neutralises them. The 6 `meshi_hunger` descriptions were reworded |
| `labyrinth/mega/puzzle/PuzzleInstance.java` penalty kind `hunger` | Hunger effect 4 s x severity | Mining Fatigue (same duration); the text is "A hollow weariness takes you." |
| `labyrinth/mega/safe/RestEvents.java` (long rest) | `setExhaustion(0f)` (a small feed) | removed: a rest leaves the bar exactly as it was |
| `dungeon/DungeonEffects.java` `MAGICULE_SICKNESS` | a food exhaustion every second | no tick; keeps its -25% attack penalty and its id |
| `dungeon/DungeonFood.java` `RUINED_MEAT` | gave the Hunger effect | gives only magicule sickness |
| `dungeon/nexus/relic/ChaliceHandler.java` + `HungerClock.foodLoss` | a food point every 4th second once starving | `foodLoss` is 0; the Chalice drinks magicules only (its clock and phases are unchanged) |
| `heroweapon/curse/` Gluttony (`SinMath.foodExhaust`, `CurseEffects.gluttony`) | +0.4 exhaustion a second down to a floor of 3 food | `SinMath.magiculeBite`: devours 0.2% of the magicule pool a second at normal strength, never under the arena floor of 10% (`StarvationMath`); config keys kept (`gluttonyFoodFloor` unused) |
| `relics/djinn/DjinnEngine.applyBacklash` | Hunger II + food points on the Maso backlash | not applied; Slowness, Weakness, Nausea, Blindness and the max-health strain remain |
| `book/FoodArts.spoil` (spoiled dishes thrown at a target) | Hunger III on the target (a player in PvP) | Mining Fatigue |
| `skilltree/line/LineLookups.give` (data word `hunger`, 10 skill lines) | Hunger effect on any target | refused for players, still given to mobs (they have no food bar) |
| `heroweapon/weapon/mirror/MirrorSkills` (Release Rebound) | sent a cleansed Hunger back at a foe | not sent to a player foe |
| `relics/Fx.addEffect` | (every relic act that names an effect by id) | refuses `minecraft:hunger` for a player |
| relic Wave 2 (Hungry cloak, `Wave2Events`) | 4 food points a day, then starve damage | magicules first (unchanged); with none it weighs the wearer down (Weakness + Slowness) |
| relic Wave 6 (Suit of the Astral Overseer, `Wave6Events/Acts/Tune`) | flight cost exhaustion and needed food > 4 | flight costs 0.06% of the magicule pool per half second (about 14 min from full) and needs 1%; the old constants stay, unused |
| relic Wave 8 (Corpse Eater frenzy) | Hunger effect | 1% of the magicules per frenzy |
| relic Wave 9 (Axe of the Exile, Hunger greataxe) | exile: 0.8 exhaustion every 5 s (a doubling of the hunger rate); gnaw: +1 exhaustion | exile: Mining Fatigue while held inside the circle; gnaw: the health bite only |
| relic Wave 11 (geas of the fast) | Hunger when the fast is broken | Weakness |
| relic Wave 18 (Bag of Colding) | Hunger on a foe | dropped (Mining Fatigue and Weakness remain) |
| relic Wave 19 (Mug of the Boss) | 0.05 exhaustion every tick of the title | dropped |
| relic Wave 24 (Filth Fever disease, Ring of Endless Rage) | Hunger | Mining Fatigue in the disease; dropped from the rage's aftermath |
| relic Wave 28 (three-hours collapse, necrotic mush, wolf form price) | Hunger | dropped |
| relic Wave 30 (X-ray, Vecna's Whisper, the Snap toll, Paradox) | exhaustion per X-ray pulse; Hunger | the unpaired X-ray costs 0.15% of the magicules; the Hunger effects are dropped |

Texts that described these drains were reworded in `tools/lang/hunger_ban.json` (24 keys that ALREADY exist in `en_us.json`: merge it with a dict update so its values win)
and in the sources that generate them (`tools/lang/food.json`, `food.pages.json`, `puzzles_v2.json`, `tools/mechanics/lang_fragment.json`, `tools/mechanics/tables/floor.py`).

## 2. What feeds or restores: kept

`FoodArts.feed` and the full-stomach pages, `WikiSpells` and `DreamWorld` meals, `InstrumentSkills` (Glutton), Gluttony's kill feed, the Chalice's stew, the
hunger-saver passive (removed with the Skill Books), the cupcake, the Sentinel's half hunger, the Belt, the Locket, the Safeguard record, the Ring of Rock Eating,
Fey Honey, Wave 16 chalk and saturation, Wave 17 meals, Wave 18 top-up, Wave 22 lich floor, Wave 30 wishes, the monster meals of `DungeonFood`, `MealBuffs`.

## 3. Cleanses and mentions only: kept

* Cleanses (they only remove the Hunger effect): `FoodArts.cleanse`, Antivenom Graft, the shield's drain-null passive, `RestEvents.FATIGUE` (a long rest clears the effect, it does not touch the bar),
  Waves 3, 14, 16, 17, the pair of Vecna, Timelessness (Wave 6).
* A creature's own 0-100 hunger meter (not a player): `DungeonCreature`, `DungeonSlimeEntity`, `DungeonGoals`, the Warden's pinned hunger (`FloorWardenEntity`).
* Names and flavour only: Lucius' Magicule Starvation (a magicule drain with a floor), the `STARVED` Tensura skill given to some bosses, the Hunger greataxe's name, the
  acts called "Hunger" in Waves 31, 32 and 45 (Wither drains), Vecna's Hunger (darkness and a spectral hand), the Aegis' Arcane Hunger (a spell shield), the Mad King's
  "exhaustion" (Weakness and Mining Fatigue), skill-line names and descriptions (Orc Lord's Famine and the like), whisper lines, `message.nusmp.labyrinth_hunger` (no longer shown).
* Not ours: vanilla Hunger from rotten flesh and husks, and effects of other mods. The ban covers this mod's systems.

## 4. How it is kept out

* `NoHungerDrainTest` (a source scan of `src/main/java`, comments stripped): no `causeFoodExhaustion` / `addExhaustion` anywhere; `setFoodLevel`, `setSaturation` and
  `setExhaustion` only in an allowlist of files that RESTORE the bar, with a pinned call count per file and no subtraction in a food or saturation argument; the Hunger
  effect may only be removed, detected or refused; the removed clock's names and the `hungerDrain` key are gone from code and resources; starve damage only on the creature
  with its own hunger meter; no negative meals; no shipped damage type with an `exhaustion` above zero (every hit it landed would drain food; `conceptual_severance` is 0.0). A new file that needs a
  food writer must be added to `WRITERS` with its reason.
* `dungeon.nohunger` (server self-test): sets a stand-in player's bar to food 12 / saturation 3.0 / exhaustion 1.25, arms `HungerBan.guard`, then drives 40 seconds of the gloom
  clock, a trap mechanic made of `starve` and an `effect` line for `minecraft:hunger`, the effect-by-id paths of puzzles, relics and skill lines, the raw-meat sickness effect
  and the monster-meat food components; after each step the guard says whether the bar got worse. The rest and curse triggers are asserted in `safe.rest_short`, `safe.rest_long`
  (food 13 / 1.5 / 2.0 unchanged after the rest) and the curse self-test (Gluttony leaves food 15 / exhaustion 0 alone).

## 5. Could not be verified

Nothing was run in the game. The scan, the numbers and the pure rules run as JUnit; the game-side edits were type-checked against API stubs. The self-tests have not run on a server.
The Wave 6 flight cost (0.06% of the pool per half second) and the other conversions to magicules are tuning guesses, not tested balance. Rests restore health and mana; the
"ability cooldowns" part of the owner's sentence is not implemented by the existing rest system and was not added here.
