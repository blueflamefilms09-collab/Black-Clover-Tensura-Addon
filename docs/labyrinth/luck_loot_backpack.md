# Luck-scaled loot, Tensura coins by Luck and armour rolls (`labyrinth.loot`); the Backpack was removed in 1.110.0

> **1.110.0: the Backpack was removed at the owner's request** ("remove the custom backpack items"). Everything about it in this page (section 5, its table rows, its
> recipes, art, self-tests `dungeon.backpack*`, config section `[backpack]`, drop roll, `PlacedHook`) is history and no longer exists. The Luck-scaled loot, the coin
> Luck and the armour rolls stay. The player-placed-block exemption of the Dungeon Guard (`dungeon/placed/PlayerPlaced`) stays: it still serves torches, crafting stations,
> sleeping bags and other mods' backpacks (Sophisticated Backpacks); only the backpack's own wiring line in `PlacedBoot` went.
>
> **Saves.** The block, block entity, menu, item, upgrade items, the `nusmp:backpack_tier` component and the `nusmp:backpack_upgrade` recipe serializer are no longer registered, with
> no inert legacy registration: NeoForge loads a world with a missing block or item (the block becomes air, a stack with an unknown item is dropped from the inventory, a block entity
> of an unknown type is skipped, an unknown component is ignored; the log lists the missing registry entries), so removal cannot crash a load. A backpack in an old world, chest or
> inventory is lost; the contents of a placed pack go with it. Unknown `[backpack]` keys in `nusmp-labyrinth-loot-server.toml` are ignored and dropped by the config loader.

Release 1.107.0, lane "loot" of the dungeon gaps. Code in `com.newuniverse.nusmp.labyrinth.loot` (Luck curve, armour pick, the modifier) and
`com.newuniverse.nusmp.labyrinth.loot.backpack` (the placeable, upgradable Backpack, REMOVED in 1.110.0); additions in `elite/hub` (the coin roll) and `client/loot`. Lang fragment
`tools/lang/luck_loot_backpack.json`. Self-tests `dungeon.loot_luck`, `dungeon.loot_luck.coins`, `dungeon.loot_luck.armour` (`dungeon.backpack`, `dungeon.backpack.drops` were removed in 1.110.0). Nothing here has been run in a game; section 8 is the honest list of what is therefore unverified.

## 1. NEW / REPLACED / KEPT

| Part | Status | Where |
|---|---|---|
| Luck-scaled Gaussian counts (pure `LuckGauss`, clamped) | NEW | `labyrinth/loot/LuckGauss.java` |
| Global loot modifier `nusmp:luck_loot` (counts, armour roll; the backpack drop was removed in 1.110.0) | NEW, switched on beside the older entries | `LuckLootModifier`, `LuckLoot`, `data/nusmp/loot_modifiers/luck_loot.json`, `global_loot_modifiers.json` (one entry appended) |
| Tensura coins scale with Luck (amount and a mob's chance to pay) | EXTENDED: new overload of `CoinLoot.roll`, the old overload kept and identical draw for draw at Luck 0 | `elite/hub/CoinLoot`, `CoinLootModifier`, `CoinMobDrops`, `CoinBossDrops` |
| Randomised-NBT armour rolls for chests and lucky kills (`LootArmorItem`) | EXTENDED: the roll itself (`gear_roll` component, `GearRollMath`) already existed and is untouched; NEW is the chest / kill hand-out and the Luck-tilted rarity | `LuckArmour`, `LuckLoot.addArmour` |
| Placeable, upgradable Backpack (block, block entity, menu, screen, 5 tiers) | REMOVED in 1.110.0 (owner's request) | `labyrinth/loot/backpack/**`, `client/loot/BackpackScreen` |
| Backpack 3D model, 5 textures, 4 upgrade sprites, blockstate | REMOVED in 1.110.0 (owner's request) | `tools/gen_backpack_assets.py` |
| Recipes: backpack, four upgrade items, and the "upgrade a pack, keep its contents" recipe | REMOVED in 1.110.0 (owner's request) | `data/nusmp/recipe/backpack*.json`, `BackpackUpgradeRecipe` |
| `PlacedHook` seam for the dungeon guard lane | REMOVED in 1.110.0 (owner's request) | `labyrinth/loot/backpack/PlacedHook.java` |
| The anime loot modifier (`nusmp:anime_loot`), `KillLoot`, `GearRollMath`, the gear and food items, the Hub coin tables | KEPT, unchanged | |
| Creative tab "Dungeon Loot" | KEPT; the backpack entries added in 1.107.0 were removed in 1.110.0 | `LootCreative` |
| Nothing was REPLACED in 1.107.0. In 1.110.0 the Backpack rows above were removed on the owner's explicit request. | | |

Existing code that this extends (and did not duplicate): `AnimeLootModifier` / `KillLoot` already take the killer's Luck into the RARITY and the drop chances of monster parts
and gear; `CoinLootModifier` / `CoinLoot` already scaled coins by floor and Looting. What was missing, and is added, is: a bell-curve COUNT for ordinary loot, Luck in the coin
payout and a way for a CHEST to hand out a rolled `LootArmorItem` (the Backpack of 1.107.0 was removed in 1.110.0).

## 2. Luck-scaled Gaussian loot

`LuckGauss` (pure, no Minecraft type). The number of items in a stack follows a bell curve whose middle rises with Luck and with the floor.

```
mean(luck, floor)  = gainPerLuck * min(max(luck,0), 20)  +  depthGain * d / (d + 200)        d = |floor|
extra              = clamp( mean + sigma * N(0,1),  0,  maxFactor - 1 )      sigma = 0.5 * mean
count(base)        = base + stochasticRound(extra * base), at most max(base, stack size)
```

Defaults (config `nusmp-labyrinth-loot-server.toml`, section `[luck]`): `gainPerLuck 0.06`, `depthGain 0.5`, `maxFactor 3.0`. The middle of the extra, as a fraction of the base count:

| Luck \ floor | 1 | 50 | 200 | 1000 |
|---|---|---|---|---|
| 0 | 0.00 | 0.10 | 0.25 | 0.42 |
| 2 | 0.12 | 0.22 | 0.37 | 0.54 |
| 5 | 0.30 | 0.40 | 0.55 | 0.72 |
| 10 | 0.60 | 0.70 | 0.85 | 1.02 |
| 20 | 1.20 | 1.30 | 1.45 | 1.62 |

Where it applies (`LuckLoot`): in a dimension of the mod that is not the Hub; a monster kill needs a PLAYER's last blow (a mob grinder with nobody fighting adds nothing), the
victim is not a pet and not a summoned minion; a chest is any loot table under `chests/`. It enlarges every stackable stack except gear, Tensura coins (they have their own Luck),
and any item of this mod that is not a monster part, a meal or an arrow (keys, books, relics, cores are never touched). It runs as a global loot modifier, so it
sees the loot of every source (vanilla table, a Lootr chest, another mod's monster, a data pack edit, a mob with no table).

The Luck read is the largest of the loot context's Luck and the player's `Attributes.LUCK` value (vanilla already puts the killer's Luck into a kill's context and the opener's
into a chest's), clamped to 0..20.

### Why (the spec's "WHY" for each decision)

* **Why a Gaussian and not a flat bonus.** A flat "+2 per Luck" makes every lucky run the same and every unlucky run the same. A bell curve makes the ordinary haul the common one
  and a great haul a rare, believable one. The middle moves with Luck (a lucky player gets a BETTER typical haul) and the spread moves with it (their best hauls are much better
  than their typical ones), which is how luck feels in play.
* **Why it can only add.** The extra is clamped at zero. Luck never takes an item the table gave; a player with Luck 0 sees the old tables exactly (add the new, keep the old).
* **Why no random number is drawn at Luck 0 and depth 0.** The old results stay bit-for-bit what they were; the test `withNoLuckAndNoDepthNothingChangesAndNoNumberIsDrawn` fails if
  the dice is touched.
* **Why negative Luck counts as zero.** A cursed player is not punished with emptier chests (that would read as a bug), they just are not rewarded.
* **Why the depth term is `d / (d + 200)`.** Floor scaling in this mod is the saturating curve on purpose: deeper is richer, never without end. It is not exponential.
* **Why the cap and the factor.** A stack never goes over its size, and `maxFactor` (3) is the most any base count is multiplied by, so a stack of arrows cannot become a
  server-wide item flood however much Luck stacks (potions, relics, heritage).
* **Why the same curve for coins, without the depth term.** Coins already scale with the floor (tiers of 200 floors); adding depth again would double count.

## 3. Tensura coins scale with Luck

`CoinLoot.roll(source, floor, looting, luck, luckParams, params, dice)` (the old five-argument `roll` delegates to it with Luck 0 and is draw-for-draw identical, pinned by
`CoinLootLuckTest`). Luck adds one point per point to a MOB's chance of paying (at most 15 points) and multiplies the amount by `1 + extra` of the Luck-only curve
(`LuckGauss.COINS`: 0.05 per point, at most x2.5). Applied for mobs and chests (killer's / opener's Luck), rift chests, and the decennial Guardian (the Guardian ignores Looting, but
its killer's Luck counts). Switch `luck.scaleCoins`; amounts `luck.coinGainPerLuck`, `luck.coinMaxFactor`. The Hub (floor 0) still pays nothing.

**Why Luck and not only Looting.** Looting is a weapon enchantment; Luck is the character's own (potions, the Heritage lines, relics, Tensura's probability skills through the attribute).
The owner wants the lucky character to be the rich one, whatever they swing.

## 4. Randomised armour rolls

The roll of the stat lines (`GearRollMath`, `nusmp:gear_roll`: rarity, seed, floor, two to four lines, armour first, each inside the range its rarity and depth give) is unchanged. New:

* `LuckArmour.pick`: WHICH piece (any piece of a set whose first depth band the floor has reached), HOW GOOD (the existing rarity weights at the band and Luck; a boss counts one band
  deeper) and the SEED (drawn from the dice, so two chests give different pieces; the same dice gives the same piece, which is what the tests use).
* `LuckGauss.armourChance`: `(base + 0.015 * Luck) * (1 + d/(d+200))`, base 0.01 kill / 0.25 boss / 0.06 chest, at most 0.5. At most ONE piece per roll, and none when the loot already holds a
  piece of gear (the monster's own table already gave one).
* Config `luck.armourRolls`, `luck.armourRate`.

At floor 50: kill 1.2% -> 10% (Luck 5) -> 37% (Luck 20); boss 30% -> 50%; chest 7% -> 43%.

**Why chests.** The monsters already dropped `LootArmorItem` through `KillLoot`, but a chest never did; the dungeon's chests gave only the standard gear (`nusmp:gear_chests`).
Armour with a rolled line set is a reason to open chests on every floor.

## 5. The Backpack (removed in 1.110.0)

Removed at the owner's request. Its code (`labyrinth/loot/backpack/**`, `client/loot/BackpackScreen`), recipes, loot table, models, blockstate, textures, art generator,
lang keys, config section, drop roll (`BackpackDrops`, `LuckGauss.backpackChance`), self-tests and tests are gone. See the note at the top for save behaviour.

## 6. Config (`nusmp-labyrinth-loot-server.toml`)

`[luck]` `scaleCounts`, `gainPerLuck`, `depthGain`, `maxFactor`, `scaleCoins`, `coinGainPerLuck`, `coinMaxFactor`, `armourRolls`, `armourRate`;
(The `[backpack]` section was removed in 1.110.0.) Every one is read through `LootConfig.get` (the default while the spec is not loaded).

## 7. Self-tests and unit tests

* `dungeon.loot_luck`: the modifier type is registered and listed; a made-up chest of four rotten flesh, rolled through the REAL pipeline (all global modifiers act), averages about four with
  no Luck and clearly more with Luck 15, never fewer than four, never past a stack.
* `dungeon.loot_luck.coins`: the no-Luck roll equals the old roll; Luck raises a mob's chance and the amount; through `CoinLootModifier.add` on a chest in the Mega-Dungeon.
* `dungeon.loot_luck.armour`: a Luck 20 boss on floor 400 yields armour pieces carrying a real roll (at least two lines), randomised, rarer with Luck.
* `dungeon.backpack` and `dungeon.backpack.drops`: removed in 1.110.0 with the Backpack.
* JUnit: `LuckGaussTest`, `LuckArmourTest`, `CoinLootLuckTest` (`BackpackLogicTest` and `BackpackAssetsTest` were removed with the Backpack) (plus the existing `CoinLootTest`, `KillLootTest`, `LootAssetsTest`, `GearRollMathTest`,
  `HubDataFilesTest`, `ScreenBlurGuardTest`, which still pass).

## 8. What could NOT be verified (nothing was run in a game)

* No game run: the self-tests, the screen, the placed model, the item model overrides and the loot table copy were written against the API stubs (type-checked) and checked by pure tests and file
  tests only. The first CI smoke run is the first real test of `dungeon.loot_luck*` `dungeon.loot_luck*`.
* Whether another global loot modifier runs before or after `nusmp:luck_loot` is the loader's order; the modifier only touches eligible stacks, so either order gives the same result, but a
  modifier of another mod that adds a stackable item AFTER ours is not enlarged.
* The numbers (0.06 per Luck, 0.5 depth, x3 cap, the armour chances) are a first pass nobody has played; every one is a config value.
* Tensura's probability skills reach the character's Luck only if they modify `Attributes.LUCK`; the avatar-creation `LuckPoller` total (skills, fate, dice) is a different, creation-time figure and is not read here.
