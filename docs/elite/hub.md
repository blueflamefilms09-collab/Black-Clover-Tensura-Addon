# Hub economy: Tensura coins, Hub NPCs, quests, shop and the Dungeon Map (elite/hub, 1.83.0)

Package `com.newuniverse.nusmp.elite.hub` (server and shared), `com.newuniverse.nusmp.client.elite.hub` (client). Config `nusmp-elite-hub-server.toml`, master switch `hub.enabled`. Self-tests `elite.hub.*`
(`/multiverse mega selftest`). Lang fragment `tools/elite/hub_lang.json`. This is the owner's brief "Tensura Coin Universal Economy and Hub NPCs", built as the `hub` area of the 1.83.0 plan
(`docs/elite/plan.md`).

## New or replacement (CLAUDE.md rule 5)
| Part | Verdict | What happened to the old |
|---|---|---|
| Currency of the Mega-Dungeon (what mobs, chests and bosses pay) | **REPLACEMENT**, by the owner's words ("the generic Magic Stone economy is replaced") | Magic Stone and crystal items, ids and saves are untouched. `economy.currency = magic_stones` is the old behaviour, byte for byte (the three old drop listeners are reached with the same arguments). `economy.keepStoneDrops` keeps the stones dropping next to the coins. |
| Magic Stone charge in shops | There is none in the code today (see "What the repo had"), so nothing was replaced; the new shop and quests go through the same seam and can charge Magic Stones in the old mode | |
| Coin wallet (`EliteHooks.wallet`) | **NEW** | |
| Coin drops: global loot modifier, boss and table-less mob listeners | **NEW** (beside the old drops) | |
| Quest Giver and Merchant NPCs, quest data attachment, quest overlay, quest dialogue | **NEW** (the System's floor quests and the arc NPCs stay as they were) | |
| Shop screen and server-validated trading | **NEW** (the Black Market of the skill tree keeps its own gold purse and shop) | |
| Dungeon Map item and screen | **NEW** | |

## What the repo had (why the seam is where it is)
* The Mega-Dungeon PAYS Magic Stones from three places: `labyrinth/dnd/economy/EconomyDrops` (the 1.36.0 economy, every hostile non-boss monster of a Labyrinth dimension, stones and crystals graded by
  tier), `labyrinth/mega/eco/FactionDrops` (a threat-scaled stone bonus) and `labyrinth/mega/foundry/FoundryDrops` (the same bonus for the 400 foundry mobs). Chest tables
  (`data/nusmp/loot_table/chests/labyrinth_trial.json`, `dungeon_relic_vault.json`) also contain Magic Stones.
* Nothing in the Mega-Dungeon CHARGES Magic Stones: there are no "Merchant Oases" in the code (safe rooms are rest areas with anchors, `labyrinth/mega/safe`), and the Dungeon Master (`mega/enc`) is the
  encounter director, not a merchant. The Black Market (`skilltree/shop`) uses an abstract "gold" purse stored in the skill tree and never touches coin items.
* The one place the mod already pays Tensura coins is `relics/wave/Wave16Wallet`: "a gold coin is a hundred" silver coins. That is the only existing coin rule, so the coin table extends it.

## The coin table (`CoinKind`)
The unit is the lowest coin, the bronze coin. Every price, reward and balance is a whole number of bronze coins. `EliteHooks.Wallet` documents its unit as "copper coins": the lowest Tensura coin is
`tensura:bronze_coin`, which is that unit.

| Coin | Item id | Value (bronze) |
|---|---|---|
| Bronze | `tensura:bronze_coin` | 1 |
| Silver | `tensura:silver_coin` | 100 |
| Gold | `tensura:gold_coin` | 10,000 |
| Stellar Gold | `tensura:stellar_gold_coin` | 1,000,000 |

Gold = 100 silver is the repo's existing rule (a test reads `Wave16Tune.java` and fails if it changes). Bronze = silver / 100 and stellar gold = 100 gold extend the same factor; those two steps are
assumptions to check against the real mod (the Tensura jar is not available to the builder).

## The seam (`Currency`)
`Currency` is the small seam the brief asked for. `Currency.stonesSuppressed(level)` is asked by the three old drop listeners (one edit each, see the report) and is true only in the Mega-Dungeon with
`currency = tensura_coins` and `keepStoneDrops = false`; the older Labyrinth Spire dimension keeps its stones. `Currency.balance / charge / give` move money in bronze through `CoinWallet` (coins) or
`StoneWallet` (Magic Stones, `economy.stoneValue` bronze each, default 100, a charge rounded UP to whole stones, no change). `EliteHooks.wallet` is set once to `Currency.HOOK`, which follows the mode and the
master switch at every call (0 / false when the economy is off). Coins inside Tensura's coin pouches are not counted (the pouch has its own storage API, unverified): take them out first.

## Coin drops
* `CoinLoot` (pure): `roll(source, floor, looting, params, dice)` in bronze. Tier = 200 floors each, five tiers (the Mob Foundry's rule, a test compares them). Payout =
  `base x tierGrowth^(tier-1) x (1 + floorGrowth x depth in tier) x scale`, jittered 0.6 to 1.4 (boss 0.9 to 1.1), x `(1 + lootingBonus x Looting)` for mobs and chests. Mobs pay on `mobChance` (+5 points per Looting
  level); chests and bosses always pay. Floor 0 (the Hub) pays nothing: the modifier and the listeners only run in the Mega-Dungeon dimension and the roll itself returns 0 for floor 0. Defaults: mob 6 bronze, chest 80, boss 1500,
  x3 per tier, up to +50 percent over a tier (always below the step to the next tier), a rift-pocket chest x3.
* `CoinLootModifier` (global loot modifier `nusmp:hub_coins`, `data/nusmp/loot_modifiers/hub_coins.json`, listed in `data/neoforge/loot_modifiers/global_loot_modifiers.json`): pays an ordinary hostile monster killed by a
  player (via `LootVictims.kindOf`, the same test the skill books use: no pet, shadow, summon or boss) and every `chests/...` table rolled in the Mega-Dungeon (in a Secret Rift pocket the rift multiplier and the pocket's own floor).
  In coin mode the chest roll loses its Magic Stones and crystals (the economy is replaced) unless `keepStoneDrops`.
* `CoinMobDrops` (`LivingDropsEvent`, LOW): the same payout for creatures that roll no loot table at all. The 400 `nusmp:foundry_mob` are exactly that (`FoundryMobEntity.dropFromLootTable` does nothing; their loot comes from
  the economy listeners), so a modifier alone would never pay them. The modifier stamps a creature it has decided for with `nusmp_hub_coin_decided`, so the two never pay twice.
* `CoinBossDrops` (`LivingDropsEvent`, LOW): the decennial Guardian (also table-less) pays the BOSS amount at the boss floor of the sector it guards.
* Not from the Hub; not from a monster another monster killed (a turf war), a summoner's minion or a servant.

## Hub NPCs
`QuestGiverNPC` and `MerchantNPC` are `PathfinderMob`s (`HubNpcBase`), registered as `nusmp:quest_giver_npc` and `nusmp:merchant_npc` (MISC category, fire immune, spawn eggs). They persist, never despawn, are invulnerable
(except the void, `/kill` and a creative player's hand), cannot be pushed, leashed or poisoned, look at the nearest player and stay within two blocks of their post. A right click opens the server menu
(`HubMenus.QuestMenu` / `ShopMenu`, slotless, valid while the NPC is alive and within 8 blocks). `HubNpcSpawner` looks after them every five seconds in the Sanctuary dimension, only when the plaza is stamped, a player is within 96 blocks and the
plaza chunks are entity-ticking (so an NPC saved with the chunk is never mistaken for a missing one): it places a missing NPC on the walkable ground of its spot (`HubNpcLayout`: the Quest Giver in front of the notice pavilion, facing the arrival dais; the Merchant behind the
counter of the red stall) and removes duplicates. `npcs.spawn = false` switches the placing off.

Look: a clean VILLAGER-model renderer with a skin of its own each (`textures/entity/hub/*.png`, painted by `tools/elite/gen_hub_textures.py`). The arc-kit characters were not used on purpose: they are all ONE shared entity type
(`nusmp:arc_npc`) wired through the kit's roster, model specs and renderer registry, and the brief asks for two entity classes of their own.

## Quests
* Data: `data/nusmp/elite/hub/quests/*.json` (73 templates: daily kill hunts for each of the five tiers and both roads, Corrupted Foundations hunts, floor clearing, faction kin, deliveries; progression chains of
  reach-floor goals for each road, hunts, Guardians, Foundry hunters, floor clearers and faction wardens, deliveries). A quest has `type` (`daily` | `progression`), `kind` (`kill` | `clear_floors` | `deliver` | `reach_floor`),
  `target`, filters (`tier` 0 to 5, `road`, `faction`, `mob` = any | foundry | boss), `item`, `floor` (signed), `reward` (bronze), `minFloor` (deepest floor the player must have reached), `requires` (quest ids), `weight`.
  `QuestParser` skips a bad row with a sentence and keeps the rest; a later quest with the same id (a datapack) replaces an earlier one.
* State machine (`PlayerQuestData`, pure, tested): ACTIVE, then READY when the objective is met, then handed in at the Quest Giver, once. Daily: the board is `QuestRules.dailyBoard(day, ...)`, the same for every player with the same unlocks,
  new every world day; done-today is cleared and an unfinished daily of an earlier day is dropped when the day changes. The day is the overworld's day count (`dayTime / 24000`), "the real day count of the world".
  Progression: done for good, unlocked by `requires` and `minFloor`. Deliveries are counted from the inventory at hand-in and the items are taken before the reward is paid.
* The IPlayerQuestData of the brief is the data attachment `nusmp:hub_quests` (`HubAttachments`, the 1.21 form of a capability, saved with the player, copied on death), exactly how `SystemState` does it. `nusmp:hub_map` keeps the
  rooms seen and the floors mapped.
* Feeds: `QuestEvents` (death of a monster killed by a player, `FloorListener` entered/cleared, login, respawn, dimension change). Reward: coins (or Magic Stones in the old mode) through `Currency`, `quests.rewardScale`.
* Overlay: `QuestOverlay`, a GUI layer above everything, top right under the potion effects, "12/50 Tier 1 Abyssal Kills" and a bar per quest (a green "hand in" line when ready), at most five, hidden by F1, while a screen is open,
  while F3 is open and by the toggle key (apostrophe). The server sends the lines (`HubNet.HudSync`) at most every ten ticks per player.
* Dialogue: `QuestScreen` (list with one button per row: Accept, Hand in, Abandon; hover for the description; wheel scrolls). Every click sends only a quest id.
* Not done: party-shared progress (a kill counts for the killer only), a live overlay refresh when the inventory changes for a delivery (it refreshes on the next sync and when the dialogue opens).

## Shop
Data `data/nusmp/elite/hub/shop.json`: 45 offers in four categories (gear, food, utility, map: the Dungeon Map costs 15 silver). Every price is coins. `ShopService.purchase` is the only purchase path: the menu must be
open next to a living Merchant, the price is read from the table (times `shop.priceScale`), the verdict (`ShopRules.decide`: shop off, unknown offer, locked by floor, sold out for today, cannot afford) is pure and tested, the money is
charged through the seam, the goods go into the inventory (or onto the ground when it is full: a purchase never fails after the coins left), the counter is bumped and the screen gets a whole fresh picture.
The stock is PER PLAYER and PER WORLD DAY (`dailyLimit` stacks): the Hub is shared, a global stock would let one player buy out the Dungeon Map for everybody, and a per-player counter needs no shared state to save or sync, resets with
the same day clock as the dailies, and is the rule a player can predict.

## Dungeon Map (`nusmp:dungeon_map`)
A real item (stacks to 16; 3D rolled map in the hand through `neoforge:separate_transforms`, the flat sprite as the icon, the Skill Book's pattern; `tools/elite/gen_hub_models.py`). Using it inside the Mega-Dungeon asks `HubMap` to flatten
the player's sector into a `MapView` (pure): only the rooms of the CURRENT floor, only the doors (the generator's `Sector.edges`) between kept rooms, the first rooms by id up to `map.maxRooms`, a visited flag per room from the
player's `MapMemory` (the room they stand in is recorded every half second). `DungeonMapScreen` draws every room at true size, coloured by its `RoomKind`, doors as lines, unexplored rooms dim, S for the arrival stair, E for the
stair that leads on, and the player live with the way they face; drag to move, wheel to zoom. It shows the layout, not what is in the rooms.
`map.mode`: `durable` (default, never used up: a map that eats itself while you are lost punishes the player it should help), `per_floor` (the first use on a floor uses one up, reopening it there is free) or `consumable`.

## Config keys (`nusmp-elite-hub-server.toml`)
`hub.enabled`; `economy.currency` (`tensura_coins` | `magic_stones`), `economy.keepStoneDrops`, `economy.stoneValue`; `coins.scale`, `coins.mobChance`, `coins.mobBase`, `coins.chestBase`, `coins.bossBase`, `coins.riftMultiplier`,
`coins.tierGrowth`, `coins.floorGrowth`, `coins.lootingBonus`; `npcs.spawn`; `shop.enabled`, `shop.priceScale`; `quests.enabled`, `quests.maxActive`, `quests.dailyCount`, `quests.rewardScale`; `map.enabled`, `map.mode`, `map.maxRooms`.

## Tests
JUnit (pure): `CoinMathTest`, `CoinLootTest`, `QuestsTest`, `ShopTableTest`, `MapViewTest`, `HubDataFilesTest` under `src/test/java/com/newuniverse/nusmp/elite/hub`. In-game: `elite.hub.wallet`, `stones`, `loot`, `shop`, `quests`, `npcs`, `map`.

## Unverified (written against stubs and existing idioms, never run in a game)
The coin ratios of bronze and stellar gold; that the real Tensura loot pipeline calls the modifier for entities with a table (it does for the skill books, the same way); the Villager model/texture registration; the menu and screen
open flow; the `separate_transforms` item model transforms; the plaza spots against the stamped template; pouches. CI compiles the real thing.
