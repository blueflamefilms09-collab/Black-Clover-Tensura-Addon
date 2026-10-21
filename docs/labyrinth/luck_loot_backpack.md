# Luck-scaled loot, Tensura coins by Luck, armour rolls and the Backpack (`labyrinth.loot`, `labyrinth.loot.backpack`)

Release 1.107.0, lane "loot" of the dungeon gaps. Code in `com.newuniverse.nusmp.labyrinth.loot` (Luck curve, armour pick, the modifier) and
`com.newuniverse.nusmp.labyrinth.loot.backpack` (the placeable, upgradable Backpack); additions in `elite/hub` (the coin roll) and `client/loot`. Lang fragment
`tools/lang/luck_loot_backpack.json`. Art generator `tools/gen_backpack_assets.py`. Self-tests `dungeon.loot_luck`, `dungeon.loot_luck.coins`, `dungeon.loot_luck.armour`,
`dungeon.backpack`, `dungeon.backpack.drops`. Nothing here has been run in a game; section 8 is the honest list of what is therefore unverified.

## 1. NEW / REPLACED / KEPT

| Part | Status | Where |
|---|---|---|
| Luck-scaled Gaussian counts (pure `LuckGauss`, clamped) | NEW | `labyrinth/loot/LuckGauss.java` |
| Global loot modifier `nusmp:luck_loot` (counts, armour roll, backpack drop) | NEW, switched on beside the older entries | `LuckLootModifier`, `LuckLoot`, `data/nusmp/loot_modifiers/luck_loot.json`, `global_loot_modifiers.json` (one entry appended) |
| Tensura coins scale with Luck (amount and a mob's chance to pay) | EXTENDED: new overload of `CoinLoot.roll`, the old overload kept and identical draw for draw at Luck 0 | `elite/hub/CoinLoot`, `CoinLootModifier`, `CoinMobDrops`, `CoinBossDrops` |
| Randomised-NBT armour rolls for chests and lucky kills (`LootArmorItem`) | EXTENDED: the roll itself (`gear_roll` component, `GearRollMath`) already existed and is untouched; NEW is the chest / kill hand-out and the Luck-tilted rarity | `LuckArmour`, `LuckLoot.addArmour` |
| Placeable, upgradable Backpack (block, block entity, menu, screen, 5 tiers) | NEW | `labyrinth/loot/backpack/**`, `client/loot/BackpackScreen` |
| Backpack 3D model, 5 textures, 4 upgrade sprites, blockstate | NEW, generated | `tools/gen_backpack_assets.py` |
| Recipes: backpack, four upgrade items, and the "upgrade a pack, keep its contents" recipe | NEW | `data/nusmp/recipe/backpack*.json`, `BackpackUpgradeRecipe` |
| `PlacedHook` seam for the dungeon guard lane | NEW | `labyrinth/loot/backpack/PlacedHook.java` |
| The anime loot modifier (`nusmp:anime_loot`), `KillLoot`, `GearRollMath`, the gear and food items, the Hub coin tables | KEPT, unchanged | |
| Creative tab "Dungeon Loot" | KEPT; a backpack of every tier and the four upgrades are appended at the end | `LootCreative` |
| Nothing was REPLACED. | | |

Existing code that this extends (and did not duplicate): `AnimeLootModifier` / `KillLoot` already take the killer's Luck into the RARITY and the drop chances of monster parts
and gear; `CoinLootModifier` / `CoinLoot` already scaled coins by floor and Looting. What was missing, and is added, is: a bell-curve COUNT for ordinary loot, Luck in the coin
payout, a way for a CHEST to hand out a rolled `LootArmorItem`, and the Backpack.

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
backpacks, and any item of this mod that is not a monster part, a meal or an arrow (keys, books, relics, cores are never touched). It runs as a global loot modifier, so it
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

## 5. The Backpack

Block `nusmp:backpack`, block entity `nusmp:backpack`, menu `nusmp:backpack`, item `nusmp:backpack`, data component `nusmp:backpack_tier`, upgrade items
`nusmp:backpack_upgrade_iron|gold|diamond|netherite`, recipe serializer `nusmp:backpack_upgrade`.

| Tier | Slots | Rows | Upgrade item | First floor it can drop from (cap) |
|---|---|---|---|---|
| leather | 27 | 3 | (the plain backpack) | 1 |
| iron | 36 | 4 | `backpack_upgrade_iron` | 10 |
| gold | 45 | 5 | `backpack_upgrade_gold` | 40 |
| diamond | 54 | 6 | `backpack_upgrade_diamond` | 120 |
| netherite (the dungeon tier) | 63 | 7 | `backpack_upgrade_netherite` | 300 |

### How it behaves

* **In the hand.** Use it in the air (config `backpack.openInHand`): its window opens, nothing is placed. The contents are written into the stack's `minecraft:container` component on
  every change and on close; the tier is `nusmp:backpack_tier`. The slot holding the backpack cannot be picked up while its window is open.
* **Placed.** Use it on a block: it becomes the block (a 3D model, turned to face you, one model per tier). Right-click opens it (a normal container: hoppers, comparators and locks work,
  because the block entity is a vanilla randomizable container; a loot table can fill one). The block is soft (strength 0.8, breakable by hand).
* **Carries its contents.** Breaking it drops a backpack with the tier and the contents (the block's loot table `copy_components` from the block entity: `minecraft:container`,
  `minecraft:custom_name`, `nusmp:backpack_tier`). Sneak + right-click with an empty hand picks it up directly, contents included (config `backpack.sneakPickup`), only a pack a player
  placed (a flag on the block entity), and the removal goes through `DungeonGuard.internal` because taking back what you set down is not breaking the dungeon.
* **Upgrades.** Three ways, all through the pure `BackpackTier.upgraded` (an upgrade only ever makes a pack bigger; one that is not bigger is refused and not spent): (1) the crafting grid:
  a backpack and an upgrade item, nothing else, make the same backpack at the bigger tier with every component kept (`BackpackUpgradeRecipe`, a special recipe, so it can copy components);
  (2) use an upgrade item on a placed backpack: it grows in place, items kept; (3) the upgrade items themselves are ordinary shaped recipes.
* **No nesting.** A backpack (or anything that answers `canFitInsideContainerItems` with no) cannot go inside a backpack.
* **Drops from loot** (`BackpackDrops`, `LuckGauss.backpackChance`): `base * (1 + 0.25 * Luck) * (1 + d/(d+200))`, base 0.4% kill / 3% chest / 12% boss, at most 35%, times
  `backpack.dropRate`. At most ONE item per roll: a plain backpack (60%, boss 45%) or an upgrade; the upgrade tier is drawn uniformly from iron up to the floor's cap, and Luck may step it
  one tier higher (4% per point, at most 40%) as long as the cap allows. On the first floors only the plain backpack can drop. At floor 50: kill 0.5% -> 1.1% (Luck 5) -> 2.9% (Luck 20);
  chest 3.6% -> 8.1% -> 21.6%; boss 14% -> 32% -> 35% (cap).

### Recipes

| Result | Pattern |
|---|---|
| Backpack | `SLS / LCL / LLL` (S string, L leather, C chest) |
| Iron upgrade | 8 iron ingots around 1 leather |
| Gold upgrade | 8 gold ingots around 1 leather |
| Diamond upgrade | 4 diamonds at the corners, 4 gold ingots on the sides, 1 leather in the middle |
| Netherite upgrade | 6 diamonds, 2 netherite ingots, 1 leather |
| Upgrade a pack | a backpack + an upgrade item, shapeless, contents kept |

### The seam for the dungeon guard (`PlacedHook`)

The guard lane keeps the log of player-placed blocks and the exemption that makes them breakable; this lane does not edit `DungeonGuard`. `labyrinth/loot/backpack/PlacedHook` is the
tiny interface between them, in both directions: the backpack calls `PlacedHook.fire(level, pos, player)` in `setPlacedBy` (server side, only for a real player); the guard installs its
listener once with `PlacedHook.install((level, pos, by) -> PlayerPlaced.mark(level, pos))` (null removes it) and may ask `PlacedHook.isBackpack(state)` when it decides whether a block it
never saw placed may still be picked up. With nothing installed the call does nothing; an installed listener that throws is ignored (a placement never fails because of a log). The
generic `BlockEvent.EntityPlaceEvent` the guard lane logs also covers the backpack, so a double mark is harmless (it must be idempotent).

### The model and the art

`tools/gen_backpack_assets.py` is deterministic. One list of nine cuboids (body, flap, front pocket, two side pockets, two straps, buckle, top handle) in the 16-unit grid, front toward
north, with display transforms (gui, ground, fixed, first and third person, head). Each visible face gets a rectangle of a 128 x 128 atlas (four pixels per model unit) packed by a shelf
packer, and the SAME rectangles are written into the model's `uv`, so the picture and the model cannot disagree. Five textures (leather body that darkens a little per tier, trim and
buckle in leather, iron, gold, diamond and netherite colours; the netherite pack has a violet rune on the flap), four 16 x 16 upgrade sprites. The item model
`models/item/backpack.json` picks the tier model by the client item property `nusmp:backpack_tier` (registered in `LootClient`; each override matches from its value up).

**Why the tier is in the block state.** The model, the size and the texture are then one fact (`tier=0..4` times four facings = 20 variants), so a world edit or a command that changes
the state resizes the pack and never leaves a model that disagrees with its size. The block entity grows its list when the tier grows and never shrinks it.

**Why the contents ride in the vanilla container component.** A backpack that is dropped, traded, stored in a chest or lost to a creeper and picked up again is exactly what it was,
and every vanilla tool that knows `minecraft:container` (the loot table function, commands) works on it.

## 6. Config (`nusmp-labyrinth-loot-server.toml`)

`[luck]` `scaleCounts`, `gainPerLuck`, `depthGain`, `maxFactor`, `scaleCoins`, `coinGainPerLuck`, `coinMaxFactor`, `armourRolls`, `armourRate`;
`[backpack]` `drops`, `dropRate`, `openInHand`, `sneakPickup`. Every one is read through `LootConfig.get` (the default while the spec is not loaded).

## 7. Self-tests and unit tests

* `dungeon.loot_luck`: the modifier type is registered and listed; a made-up chest of four rotten flesh, rolled through the REAL pipeline (all global modifiers act), averages about four with
  no Luck and clearly more with Luck 15, never fewer than four, never past a stack; Luck raises the number of backpacks.
* `dungeon.loot_luck.coins`: the no-Luck roll equals the old roll; Luck raises a mob's chance and the amount; through `CoinLootModifier.add` on a chest in the Mega-Dungeon.
* `dungeon.loot_luck.armour`: a Luck 20 boss on floor 400 yields armour pieces carrying a real roll (at least two lines), randomised, rarer with Luck.
* `dungeon.backpack`: five tiers 27/36/45/54/63; place one with a fake player; the block entity; an upgrade used on the block grows it in place and keeps the items, an upgrade that is not bigger
  is not spent; breaking it through the real loot table drops a backpack with tier and contents; a block entity made from that item has the contents back; the held container writes
  into the stack; the window refuses a backpack and takes a diamond; all six recipes load and the upgrade recipe keeps contents and refuses a smaller upgrade and a stray item; the hook
  reaches a listener.
* `dungeon.backpack.drops`: ten thousand rolls: only plain packs on floor 3, never above the floor's cap, Luck raises the rate.
* JUnit: `LuckGaussTest`, `LuckArmourTest`, `BackpackLogicTest`, `BackpackAssetsTest`, `CoinLootLuckTest` (plus the existing `CoinLootTest`, `KillLootTest`, `LootAssetsTest`, `GearRollMathTest`,
  `HubDataFilesTest`, `ScreenBlurGuardTest`, which still pass).

## 8. What could NOT be verified (nothing was run in a game)

* No game run: the self-tests, the screen, the placed model, the item model overrides and the loot table copy were written against the API stubs (type-checked) and checked by pure tests and file
  tests only. The first CI smoke run is the first real test of `dungeon.loot_luck*` and `dungeon.backpack*`.
* The rendered look of the 3D model in the world, in the hand and in the inventory was checked only with a small software rasteriser of the model's own uv (front, back views of two tiers), not with
  the game's renderer. Display transforms (hand angles, scale, translation) are a first guess and may need a nudge.
* The item property `nusmp:backpack_tier` is registered in `FMLClientSetupEvent`; if a future NeoForge moves item properties, the item would show the leather model for every tier (the block is
  not affected).
* The loot table's `copy_components` of `minecraft:container` from the block entity relies on the block entity writing it in `collectImplicitComponents` (done); a mod that replaces the loot table
  of `nusmp:backpack` would lose the contents on break (the sneak pick-up does not depend on it).
* Whether another global loot modifier runs before or after `nusmp:luck_loot` is the loader's order; the modifier only touches eligible stacks, so either order gives the same result, but a
  modifier of another mod that adds a stackable item AFTER ours is not enlarged.
* The dungeon guard's side: this lane exposes `PlacedHook` and uses `DungeonGuard.internal` for its own pick-up; whether the guard lane installs its listener, and whether it lets a player place and
  break the block, is that lane's work and is not tested here. A backpack placed inside the dungeon before the guard lane lands is not breakable by hand there (the sneak pick-up still works).
* The numbers (0.06 per Luck, 0.5 depth, x3 cap, the backpack and armour chances) are a first pass nobody has played; every one is a config value.
* "Wearable-by-hand" was read as "usable in the hand": the pack opens when used in the air. It is not an armour-slot item and it has no worn model; if a back-slot or Curios wearable was meant, that is a
  separate piece of work.
* Tensura's probability skills reach the character's Luck only if they modify `Attributes.LUCK`; the avatar-creation `LuckPoller` total (skills, fate, dice) is a different, creation-time figure and is not read here.
