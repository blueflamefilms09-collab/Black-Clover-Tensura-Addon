# Labyrinth, builder D: the Treasure Vault (relic exclusivity, the `nusmp:relic` loot entry, the Relic Pedestal)

Code: `labyrinth/vault/**` (`VaultRegistry`, `RelicExclusivity`, `RelicLootEntry`, `RelicPedestalBlock`, `RelicPedestalBlockEntity`), client
`client/labyrinth/vault/**` (`VaultClient`, `RelicPedestalRenderer`); two additive edits in `relics/RelicLootModifier.java` and `relics/RelicLoot.java`;
data `data/nusmp/loot_table/chests/{dungeon_relic_vault,labyrinth_pedestal,labyrinth_trial}.json`, `loot_table/blocks/relic_pedestal.json`; assets
`blockstates/relic_pedestal.json`, `models/block/relic_pedestal.json`, `models/item/relic_pedestal.json`, `textures/block/relic_pedestal{,_top}.png`
(made by `tools/gen_relic_pedestal_textures.py`, `--check` verifies the shipped PNGs); tests `src/test/.../labyrinth/vault/RelicExclusivityRulesTest`,
`RelicExclusivityTest`. Registration: `LabyrinthSystem` calls `labyrinth.vault.VaultRegistry.init(modBus)`; `LabyrinthClient` calls
`client.labyrinth.vault.VaultClient.init(modBus)`. Creative tab: the block item `nusmp:relic_pedestal` (lead adds the line). Nothing here was run in game.

## 1. Exclusivity: relics come only from the Labyrinth

Before 1.35.0 a relic could reach a player by three paths: the global loot modifier `nusmp:relic_chests` (a small chance in any `chests/` table with
a dungeon-like word in its path: vanilla dungeons, strongholds, bastions, ancient cities, trial chambers, and every structure mod that names its tables
that way), a dying Mimic (`RelicLoot.onDrops`), and nothing else: there has never been a recipe, and creative / `/multiverse relic` are admin paths.
The owner wants them exclusive to the Labyrinth: its vault chests, its Relic Pedestals, its bosses. `labyrinth.relicsExclusive` (C's `LabyrinthConfig`,
default true) switches it; false is the 1.24 behaviour, bit for bit.

The rules are one class, `RelicExclusivity`, with pure static methods over strings and booleans (so `RelicExclusivityRulesTest` pins them without a
server) and two Minecraft-facing overloads that only gather inputs. Why each layer:

* **The loot modifier gate** (`RelicLootModifier.apply` asks `RelicExclusivity.allowsChest(table)` right after the Socket cores and before the tier
  words). This was the only path that ever put a relic in a vanilla or third-party chest, so it is the layer that makes relics disappear from the
  Overworld. While exclusive, only `nusmp:chests/dungeon_relic_vault` and `nusmp:chests/labyrinth_pedestal` pass; the modifier's old `tierOf` words
  then run unchanged, so the vault chest ("vault" word, RARE..LEGEND at 5%) can still get its bonus relic on top of the table's own relic entries and
  the pedestal table (no tier word) gets none. The namespace is checked too: another mod's `chests/dungeon_relic_vault` is not ours. The Socket cores
  (Spirit / Devil Cores, 1.29.0) are not relics and keep their old chest behaviour: they are added before the gate. The modifier stays registered and
  `global_loot_modifiers.json` still lists it (`RelicExclusivityTest` checks): removing it would also remove the vault's bonus roll and the Lootr path.
* **The mob-drop gate** (`RelicLoot.onDrops` asks `RelicExclusivity.allowsMobDrop(level)`): while exclusive a Mimic drops a relic (or the tame Cloak
  Mimic) only inside the Labyrinth dimension or the legacy Living Dungeon. Mimics are other mods' creatures that spawn anywhere; a relic from one in a
  plains biome would be a leak. Boss drops of our own (the Floor Warden's `entities/floor_warden`, E2) are the third allowed source and are not gated:
  they only exist inside the Labyrinth.
* **No recipes**: none exist, and `RelicExclusivityTest.noRecipeMakesOrUsesARelic` keeps it so: it collects every id from `RelicDef.of("...")` in
  `relics/**` (plus the `_left` half of every pair) and fails if any recipe JSON names `nusmp:<id>`. The same test fails if any loot table other than the
  two vault tables and `entities/*` names a relic item directly (a table naming an item would bypass the ledger).
* **Creative and commands stay**: an admin handing out a relic is the admin's decision; the God of Order's ledger still judges the copy.
* The config is read through `LabyrinthConfig.get`, which returns the default before the config is loaded; a roll during world load is therefore
  already exclusive. If the Labyrinth classes are missing (a partial build) the gate answers "exclusive": leaking a relic is the worse failure.
* The Djinn Seal in vault chests (`DjinnSources.vaultSeal`) is called from the Nexus vault's Java (`VaultDirector`), not from loot; it is not a relic and
  was not touched.

## 2. Why a loot pool entry type (`nusmp:relic`) instead of Java placing items

`RelicLootEntry extends LootPoolSingletonContainer`, registered in `Registries.LOOT_POOL_ENTRY_TYPE` as `nusmp:relic`; its codec is vanilla's
`singletonFields` (weight, quality, conditions, functions) plus `min_grade` (default `normal`) and `max_grade` (default `genesis`), lower-case
`Grade` names (`Grade.byName`, so "Rare" and "Rare Grade" also parse). `createItemStack` is one line: every stack of `RelicLoot.rollUnissuedAll`
goes to the consumer.

* **Every relic ever added is eligible without editing JSON.** A relic is not a fixed item: it is "one relic of these grades that nobody in this world
  has yet". Naming relic items in a table would need the JSON edited for every wave, and a table could hand out a relic that is already alive; the ledger
  would then hunt the duplicate down. The entry goes through the same `rollUnissuedAll` as the modifier and the Mimics, so the ledger stamps the copy
  it hands out and uniqueness holds across chests, Lootr openings, pedestals and Mimics alike.
* **Pairs yield both halves** from one entry (`rollUnissuedAll` returns both; the left half is never rolled alone), which a JSON item list cannot express.
* **Datapack-tunable**: grades, weights, rolls, conditions and the mundane filler are data. A server owner can make the vault stingier or add a
  `minecraft:killed_by_player`-style condition without a Java change.
* **Lootr-compatible**: Lootr replaces structure chests with per-player containers and rolls the chest's table once per opener through the normal loot
  code; the entry runs inside that roll, so every opener gets their own unissued relic (and never a duplicate, because of the ledger). Rolling the vault
  in Java when the structure is placed would give every opener the same stacks and would stamp relics for chests nobody has reached.
* **Exhausted ranges fall through.** `expand` is overridden to answer "nothing here" when no unissued relic of the range exists (`RelicLoot.anyUnissued`,
  the dry run of the roll: nothing is stamped). Vanilla's composites then work as designed: a `minecraft:group` of ranges inside a
  `minecraft:alternatives` falls through to a mundane fallback child, and an exhausted range simply drops out of a pool's weighting instead of eating a
  roll. Nothing in `createItemStack` can throw out of the table: loot must never fail because of a relic.

## 3. The data files

* **`loot_table/chests/dungeon_relic_vault.json`** (the grand vault chests): pool 1 rolls 2-3 among `nusmp:relic` NORMAL..SPECIAL (w20), SPECIAL..RARE (w12),
  RARE..UNIQUE (w6): the bulk of a vault is low and middle grades, the way the Tensura rarity curve reads; pool 2 rolls once at `random_chance` 0.6
  among UNIQUE..LEGEND (w8), LEGEND..GOD (w3), GOD..GENESIS (w1): the chance of a headline relic, weighted so a Genesis relic is a once-a-world event; pool 3
  rolls 3-5 of treasure filler (gold ingots 3-7, emeralds 2-5, diamonds 1-3, a gold block, amethyst shards 4-9, an enchanted book with
  `minecraft:enchant_randomly`, netherite scrap, and the Dungeon's Magic Stones: `tensura:magic_stone` 2-4, low 1-3 / medium 1-2 / high 1 quality magic
  crystals, the same ids `dungeon/sentient/StoneDrops` drops; Tensura is a required dependency so the ids always resolve). The modifier's bonus roll
  (section 1) sits on top. If the ledger has no relic left in a range, that range's entries drop out of the pool (section 2) and the filler still comes.
* **`loot_table/chests/labyrinth_pedestal.json`** (one relic): one roll of a `minecraft:alternatives` whose first child is a `minecraft:group` of
  `nusmp:relic` RARE..UNIQUE (w50), UNIQUE..LEGEND (w30), LEGEND..GOD (w15), GOD..GENESIS (w4), and whose second child is 3-6 emeralds. The group puts
  every range that still has an unissued relic into the weighted pick; only when every range is exhausted does the alternatives entry reach the emeralds,
  so a pedestal is never empty-handed and the fallback never dilutes a relic roll (a second pool would have dropped emeralds beside every relic).
* **`loot_table/chests/labyrinth_trial.json`** (trial chambers, mundane): rolls 2-4 of iron ingots 2-5, gold ingots 1-3, arrows 4-12, bread 2-4, a golden apple
  (w2), experience bottles 1-3, Magic Stones 1-2 and a low-quality crystal. NO relic entries, and the modifier never adds one while exclusive (the path
  has the word "trial", which the old modifier rewarded; the gate rejects it because it is not one of the two vault tables). With `relicsExclusive=false`
  the old 5% bonus applies to it again, as to every "trial" table: that is the 1.24 behaviour the switch restores. No `ominous_bottle`: it would make a
  trial chest a vanilla trial-chamber vault key and tie our trial to vanilla's ominous events.
* **`loot_table/blocks/relic_pedestal.json`**: the pedestal drops itself (`survives_explosion`), so outside the dungeon it is an ordinary decorative block.
  No `requiresCorrectToolForDrops` / `mineable` tag: appending to a shared vanilla tag file would collide with the other builders' files, and 50 hardness
  already makes it a deliberate dig.
* **`blockstates/relic_pedestal.json`**: one variant, no properties: the pedestal has no facing (the relic turns).
* **`models/block/relic_pedestal.json`**: three elements: a 16x2 base, a 10x9x10 column (y 2-11), a 12x2 top plate (y 11-13). The side faces of all three
  map to the rows of ONE texture at the same heights (`relic_pedestal.png`: rows 2-4 plate, 5-13 column, 14-15 base), so the gold trim lines meet across
  element edges and the pedestal reads as one carved piece from any side; the top faces use `relic_pedestal_top.png` (the plate its centre 12x12, the base
  the full 16x16 with the outer rim). The block is `noOcclusion` (not a full cube) and solid, so no `render_type` is needed. The block's `VoxelShape` is
  the union of the same three boxes.
* **`models/item/relic_pedestal.json`**: the block model as the inventory icon (the block's own shape reads better than a flat sprite).
* **`textures/block/relic_pedestal.png`, `relic_pedestal_top.png`** (16x16, Pillow, deterministic hash grain, no random module so the bytes are stable):
  dark stone in four shades, gold trim (light edge, body, dark seam), a rune line with cyan magicule glyph dots, a gold ring and a centre diamond with a
  cyan heart on top where the relic floats. `tools/gen_relic_pedestal_textures.py --check` fails if a shipped PNG differs from what the script makes.
* **Lang** (`$S/frag_lab/d.lang.json`, merged by the lead): `block.nusmp.relic_pedestal`, `nusmp.relic_pedestal.taken` (`%s` = the item name),
  `nusmp.relic_pedestal.empty`, `nusmp.relic_pedestal.exhausted`.

## 4. The Relic Pedestal

`nusmp:relic_pedestal`, a `BaseEntityBlock` (render shape `MODEL`: the stone is the baked model, the block entity renderer only draws the floating relic
and its glow). Strength 50/1200: inside the dungeon the guard makes it unbreakable; outside it is a hard decorative block. Light 7: a relic is a light.
No menu, no container, no hopper access. A template places it exactly like a chest:
`{id:"nusmp:relic_pedestal", LootTable:"nusmp:chests/labyrinth_pedestal", LootTableSeed:0}` (the BE reads the chest's NBT names).

* **Rolls on first use, with a seed.** At placement there is no player (no luck, no `THIS_ENTITY`), the chunk may be generated long before anyone arrives,
  and a roll at generation would stamp relics into the ledger for pedestals nobody has reached, draining the unissued pool for the whole world. So
  `unpackLoot` runs when the first player clicks: `LootParams` with `ORIGIN`, the player's luck and the player as optional `THIS_ENTITY`, param set
  `CHEST`, `getRandomItems(params, seed)` with seed 0 = fresh random (map makers can set a seed to make a pedestal reproducible). The table is cleared
  before the roll, like a chest's, so a pedestal rolls once, ever, even if it yields nothing. A pedestal is deliberately NOT per-player (Lootr does not
  touch mod blocks either): it is one visible item and whoever takes it has it; the ledger makes another player's pedestal roll a different relic.
* **The first non-empty stack is held, the rest drops at the pedestal.** A pair comes as two stacks from one entry: the pedestal shows one half and the
  other lands beside it, so a pair never loses its left half. Anything else the table rolled beside the relic drops the same way: nothing a table rolls
  is thrown away.
* **Take**: `useWithoutItem` on the server calls `take(player)`: roll if needed, hand the stack to the inventory (dropped at the player's feet if full),
  mark `taken`, sync, End Rod and Enchant particles, the beacon chime pitched up plus the pickup sound, an action-bar line naming the relic. A taken
  pedestal says "empty"; one whose roll found no relic and no fallback says "exhausted" (only possible if a datapack removed the emerald fallback).
* **Client sees the stack**: `getUpdateTag` is the full save, `getUpdatePacket` is `ClientboundBlockEntityDataPacket.create(this)`; every change calls
  `setChanged` + `sendBlockUpdated`, so the renderer always has the current stack.
* **Never lose an issued relic**: `onRemove` drops the held stack when the block is broken or replaced (the ledger already counts that copy as alive; a
  relic lost in a broken block would be one nobody can ever find, and the Order would never release it).
* **Ticker**: client only, a float phase for the bob and the turn; the server never ticks a pedestal.
* **Renderer** (`RelicPedestalRenderer`): the item at 0.9 (+0.12, the GROUND transform's own lift) above the block's bottom, bobbing 5/100 over 80 ticks,
  one turn every 120 ticks, drawn with `ItemRenderer.renderStatic(stack, GROUND, light, NO_OVERLAY, pose, buffers, level, 0)` so every relic's existing
  model (the 3D weapon models included) looks right; behind it two camera-facing quads on the mod's existing soft glow texture
  (`textures/particle/glow.png`, `RenderType.entityTranslucentEmissive`, full-bright, pulsing alpha) tinted by the held relic's grade colour
  (`Relics.defOf(stack).grade().rgb()`, white for a non-relic): the grade glow IS the anime vault reveal, seen from across the vault before the name.

## 5. Not done / to verify in game

* `MinecraftServer.reloadableRegistries().getLootTable(ResourceKey<LootTable>)` and `BlockEntityType.Builder.of(Ctor::new, block)` are not in the
  type-check stubs (the loot package is missing; `BlockEntitySupplier` is package-private in vanilla and made public by NeoForge's access transformer,
  which the stub dump lacks). Both are the 1.21.1 / NeoForge 21.1 calls and compile in CI.
* The glow quads use the dispatcher camera's rotation; if a shader pack renders block entities in a pass where the camera is not set, they are skipped
  (the null check), the item still draws.
