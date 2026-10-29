# The Black Market has goods again, and every NPC shop has a type

Why: after the Skill Books were removed (`docs/skill_books_removed.md`) the witch's shelf was empty. The owner asked for NEW GOODS, picked at random from what exists in the world and the modpack (vanilla, Tensura and any other loaded mod), and for the shop system to be general, so that FUTURE NPC shops each sell only a limited TYPE of stuff.

Nothing was seen in a game (see section 9). Everything below is checked by pure tests that read the real files, and by two new self-tests that the CI server smoke test runs in the real game.

## 1. NEW and KEPT (CLAUDE.md rule 5)

Everything here is **new**, nothing replaces something that existed; no old file, id, kind or key was removed.

| | What | New or kept |
|---|---|---|
| New | `StockEntry.Kind.ITEM` (`"kind": "item"`), appended after `ROUTE_BOOK` and `SP_BOOK` (ordinal 2) | NEW |
| New | `ShopType` (pure) and `data/nusmp/black_market/types.json` (the shop types) | NEW |
| New | Per-shop files `data/nusmp/black_market/shops/<shop_id>.json` | NEW (no file ships in it yet; the witch's shelf stays in `stock.json`) |
| New | `ShopPools` (server side pool cache), `ShopLoopGuard` (the no-profit-loop rule) | NEW |
| New | The witch's 20 item lines in `stock.json`, with `"type": "general"` | NEW (the file existed with `"entries": []`) |
| New | Self-tests `skillbook.shop.items` and `skillbook.shop.buy_item` | NEW |
| New | Lang key `gui.nusmp.shop.blurb.stack` (appended) | NEW |
| Kept | `ROUTE_BOOK` and `SP_BOOK` kinds, their parsing and every old key; an old stock file with book lines still loads (book lines still make no slot) | KEPT |
| Kept | `stock.json` as the witch's file | KEPT |
| Kept | `prices.json`, the sell side, the gold purse, the packets (`BuyPayload`, `ShopSnapshot` are unchanged on the wire) | KEPT |
| Kept | The slot ids of the witch are the bare line ids, as before (so the purchase counters keep their keys) | KEPT |
| Changed (test only) | `ShopDataFilesTest.theShelfIsEmptyBecauseTheSkillBooksWereRemoved` is now `theShelfHoldsNoBookBecauseTheSkillBooksWereRemoved` (the shelf is no longer empty; it still holds no book) | updated for the new state |

## 2. File formats

### 2.1 A shop file (`stock.json` for the witch, `shops/<shop_id>.json` for any other shop)

```json
{
  "type": "general",
  "entries": [
    { "id": "ingots", "kind": "item", "item": "#c:ingots", "exclude": ["minecraft:netherite_ingot"],
      "count": [8, 32], "price_min": 3000, "price_max": 5000, "stock": [2, 4], "weight": 100 },
    { "id": "diamonds", "kind": "item", "item": "minecraft:diamond",
      "count": [1, 3], "price_min": 260000, "price_max": 330000, "stock": [1, 2], "weight": 55 }
  ]
}
```

* `type`: the shop type (section 2.2). Missing = `general`. An unknown type id is reported and all of that shop's item lines are skipped.
* `item`: ONE item id (`minecraft:diamond`) or an item TAG with a leading hash (`#c:ores`). Lower case, with the namespace.
* `count`: how many items ONE purchase hands over (a number or `[min, max]`, 1 to 64, default 1). It is rolled per restock and capped by the rolled item's own stack size.
* `price_min` / `price_max`: gold **per item**. A purchase costs the rolled per-item price times the rolled count, so a bigger stack costs more and the per-item price always stays in the band.
* `stock`: how many purchases are on the shelf (1 to 99, a number or a pair). `weight`: the percent chance (0 to 100) that the line is on the shelf at all at a restock. `band_low`, `flagship`: as for the old lines.
* `exclude`: up to 16 item ids that never come out of this line's pool.
* Keys that start with an underscore are comments. Unknown keys warn; a broken line is skipped with a sentence naming it and the rest of the file still loads. At most 64 lines are read.

### 2.2 `types.json`

```json
{ "types": {
    "general": { "all": true, "banned_ids": ["minecraft:barrier", "tensura:mithril_ingot"] },
    "food":    { "tags": ["c:foods", "c:crops"], "ids": ["minecraft:sugar"], "banned_ids": ["minecraft:enchanted_golden_apple"] }
} }
```

A type allows an item when the item is **not** in `banned_ids` and one of these holds: `all` is true, the item id is in `ids`, the item's namespace is in `namespaces`, or the item carries one of the `tags`. A tag also allows its children (`c:ores` allows `c:ores/iron`). A TAG line of a shop file is allowed when the tag itself is allowed. Shipped types: `general`, `food`, `ores`, `potions`, `tools`, `armor`, `magic`, `building`, `materials`. The general store is always present (a built-in copy is used when the file forgets it).

## 3. How a future NPC shop declares its type

1. Add a type to `types.json` if none fits (or use one that exists).
2. Create `data/nusmp/black_market/shops/<shop_id>.json` with `"type": "<type>"` and item lines. The shop id is the file name (lower-case snake_case, at most 32 characters). A line whose item or tag is outside the type is rejected at load with `... is not something a 'food' shop sells, skipped`, and it shows in `/multiverse skillbook shop status` and fails the self-test `skillbook.shop.stock` (which demands that the load had no problem).
3. The NPC asks `ShopService.shelf(server, "<shop_id>")` for its shelf. The slot ids of every shop except the witch carry the prefix `<shop_id>/`, so two shops never share a purchase counter; the roll also uses the prefixed id, so two shops with the same line stock different things.

Not built here (the owner asked for data and the type system, not a second NPC): the NPC side that opens a shop other than the witch's (`ShopHook`, `witchNear`, and the buy/sell handlers still look for the Black Market witch). `shops/witch.json`, if a datapack adds one, takes the witch's place over `stock.json`.

Honest limit of the type check: a type is checked **by tag** for tag lines, and for a single item by id/namespace at load. A single item that is allowed only by one of its tags cannot be judged at load (the world's tags are not bound yet); it passes the load check and is judged once the world exists, when its pool is built (an item outside the type gives an empty pool, the line makes no slot and the server log says why, once). The pure function takes the item's tags when the caller has them, and that case is tested.

## 4. What happens at a restock

* The roll is the existing one: a pure function of the world seed, the restock period, the operator's restock counter and the slot id (SplitMix64, no `java.util.Random`). A second mixer stream (`StockRoll.rollItem`) picks which item of the line's pool is on the shelf and the stack size, so the item does not depend on the price roll. Every player sees the same shelf, a restart rebuilds it, `/multiverse skillbook shop restock` or a new period changes it.
* The pool of a line is the items of the tag (or the one item) that: exist in this game; are not on the line's `exclude` or the type's `banned_ids`; belong to the shop's type; the witch does not buy for as much as the line's `price_min` (section 5); at most 512 (alphabetical order, so every machine builds the same list).
* Two slots never put out the same item: the later slot takes the next free member of its pool (none free = it is left off).
* An unknown or empty tag, or an item that no installed mod provides, is skipped without any error: the line makes no slot and the server log gets one `WARN` per line per data load.

## 5. The rule against buy-low-sell-high loops

The witch BUYS goods (`prices.json`) and now SELLS goods, so an item sold for less than she pays for it would print gold. Three layers:

1. **Pool**: an item whose buy price is not strictly below the line's `price_min` never enters the pool (`ShopLoopGuard.safePool`). A broad tag like `#c:ingots` therefore cannot hand out gold ingots (she pays 25,000, the line asks 3,000 or more) however the tag grows.
2. **Shelf**: `ShopService.shelf` drops any ITEM offer that, with the prices really in force (the server's `priceScale` and `sellScale` included), would let a player sell the stack straight back for at least what it cost (`ShopLoopGuard.loops`). This also covers a server owner who sets `sellScale` high or `priceScale` low.
3. **Data**: the shipped sell prices of the goods she also buys are at least 1.3 times her buy price (diamond 200,000 / 260,000 to 330,000, emerald 60,000 / 80,000+, gold ingot 25,000 / 33,000+, scrap 400,000 / 520,000+, enchanted golden apple 600,000 / 780,000+, nether star 1,500,000 / 1,950,000+, Tensura stone 6,000 / 8,500+, the three crystals 30,000 / 100,000 / 350,000 against 42,000 / 135,000 / 460,000+). Tests: `ShopLoopGuardTest` (pure functions, the real data with a 25% margin demand, a sweep of 200 restocks over every line with a pool of everything she buys).

## 6. The witch's shelf (type `general`)

20 lines (a shelf of about 14 on a typical restock, the weights decide):

| Line | Pool | Per item | Count |
|---|---|---|---|
| ingots | `#c:ingots` (netherite ingot excluded; gold falls out by itself) | 3,000 to 5,000 | 8 to 32 |
| raw_ores | `#c:raw_materials` | 2,200 to 3,800 | 8 to 32 |
| minor_gems | `#c:gems` (diamond and emerald fall out; lapis, amethyst, quartz ...) | 4,500 to 9,000 | 4 to 16 |
| dusts | `#c:dusts` | 1,200 to 2,400 | 16 to 48 |
| foods | `#c:foods` (the meats she buys fall out) | 900 to 2,000 | 8 to 24 |
| cooked_meat | `#c:foods/cooked_meat` | 1,500 to 2,800 | 6 to 16 |
| crops | `#c:crops` | 700 to 1,500 | 16 to 48 |
| logs | `#minecraft:logs` | 500 to 1,100 | 32 to 64 |
| ender_pearls, experience_bottles | one item each | 6,000 to 9,000 / 12,000 to 20,000 | 2 to 8 / 1 to 4 |
| tensura_stone | `tensura:magic_stone` | 8,500 to 11,000 | 4 to 16 |
| low / medium / high crystal | `tensura:*_quality_magic_crystal` | 42,000 / 135,000 / 460,000 and up | 1 to 3 / 1 to 2 / 1 |
| emeralds, gold_ingots, diamonds | one item each | see section 5 | 2 to 6 / 2 to 8 / 1 to 3 |
| netherite_scrap, golden_apples, nether_star | one item each, weights 15 / 12 / 6 | 520,000 / 780,000 / 1,950,000 and up | 1 to 2 / 1 / 1 |

Flagships (gold frame): the high crystal and the nether star. Only vanilla and `c:` convention tags are named, never a guessed mod tag: Tensura has no item tags in this repo and its jar is not available here, so Tensura goods are explicit ids (all of them are already in `prices.json`, which the owner priced). Other mods' items arrive through the `c:` tags they fill.

The upgrade-ritual catalysts (`tensura:low_magisteel_ingot`, `high_magisteel_ingot`, `mithril_ingot`, `adamantite_ingot`, `hihiirokane_ingot`, `nusmp:devils_core`, `nusmp:genesis_seed`) and the creative-only blocks (barrier, command blocks, spawners ...) are **banned** in the general type, so a broad tag can never put them on sale for gold.

## 7. Server friendliness (rule 9)

* All rolls and all pool resolution run on the server (`ShopPools`, `StockBoard`, `ShopService`); the client only draws the picture it is sent. `ShopPools` has no client import (`ServerSafetyTest` and `ShopItemServerSafetyTest` pin it).
* The purchase packet is unchanged: slot id (at most 96 characters) plus the price the player saw. The item and the count are **re-derived on the server** from the shelf (`StockBoard.find(shelf(server), id)`, `ShopGoods.stack(offer)`); nothing item-shaped comes from the client. The witch's reach, the shop switch, the stock left, the price match, the daily limit and the purse are judged by the existing `ShopRules.judgeBuy`, which covers the new kind without a change. The item is handed over before the gold is taken. `ShopGate` still allows one action per player per tick.
* Per-player state is untouched (the purse and the daily counter are keyed by player as before). Shared state is the world's `MarketState` (restock period, counter, bought counts per slot id), a few hundred bytes.
* The settings that change gameplay stay server config (`nusmp-skilltree-shop-server.toml`); nothing was added to a client config.

## 8. Worst-case cost (rule 11)

| What | Bound |
|---|---|
| Per tick | **nothing**. The shop has no tick handler (a test pins it). |
| Data reload (`/reload`, server start) | at most 16 shop files of at most 64 lines, plus `types.json`, `stock.json`, `prices.json`: a few hundred small parse steps, once. |
| Pool resolution | once per line after a reload or a tag rebinding (`TagsUpdatedEvent`), lazily on the first shelf use: one tag walk of at most the tag's size, at most 512 pool items kept per line, at most 32 slots per shop. 20 lines of the witch: a few thousand registry look-ups, once. Then it is read from memory. |
| One shelf build (`ShopService.shelf`) | O(slots), at most 32 slots: 32 hash rolls, a set of at most 32 item ids, at most 32 price-table look-ups. No registry walk, no chunk access, no world scan. |
| When it runs | on opening the shop and on each answered buy/sell by a player standing at the witch (one action per player per tick at most, so at most 20 per player per second, each a few microseconds), and by the status command. |
| Entities spawned | none. |
| Network | one `ShopSyncPayload`/`OpenShopPayload` to the ONE acting player per action; at most 32 offers per picture on this path (the packet cap is 128): about 60 bytes of strings plus one item stack per offer, roughly 2 to 3 KB at the very worst, typically 1 KB. No broadcast. |
| Memory | pools: at most 16 shops x 32 slots x 512 items x about 40 bytes = 10 MB as an absolute ceiling; the witch's real pools are a few hundred items, tens of KB. Cleared when the server stops. |
| Client | one extra string concatenation per drawn shelf row (the screen already draws only the visible rows). Nothing per frame was added. |

The perf budgets of the `perf` package do not apply (nothing is spawned, drawn in the world or sent to nearby players); the pinned bounds are `StockBoard.MAX_SLOTS`, `ShopPools.MAX_POOL`, `MarketData.MAX_SHOPS`, `StockSchema.MAX_ENTRIES`, `ShopType.MAX_TYPES` and `MAX_LIST`.

## 9. Tests, self-tests, honest caveats

Pure JUnit (all pass): `ShopTypeTest`, `StockItemSchemaTest`, `StockItemBoardTest`, `ShopLoopGuardTest`, `ShopItemServerSafetyTest`, and the updated `ShopDataFilesTest`; the old `StockSchemaTest`, `StockBoardTest`, `StockRollTest`, `ShopRulesTest`, `PriceTableTest`, `BlackMarketWitchTest`, `WitchDialogueTest` and the client shop tests still pass untouched.

Self-tests (real game, `/multiverse mega selftest`): `skillbook.shop.items` (every `minecraft:`, `c:` and `nusmp:` line has a non-empty pool, every pool item exists, is allowed, is not banned and sells above her buy price, the shelf is deterministic and moves with the period, no item twice, every offer is a legal stack) and `skillbook.shop.buy_item` (a moved price and a poor purse change nothing, a purchase hands over exactly the offer's stack and takes exactly its price, a sold-out offer is refused). The existing `skillbook.shop.stock` now also proves that `types.json` and the witch's file load with no problem. All three sit under the `skillbook` prefix that `SMOKE_EXPECT` already lists, so no new prefix is needed.

Caveats:

* **Nothing was seen in a game.** The sandbox cannot run Minecraft or Gradle; the game-side classes were only type-checked against stubs.
* **The `c:` tag names are written from memory of NeoForge 1.21.1's convention tags** (`c:ingots`, `c:raw_materials`, `c:gems`, `c:dusts`, `c:foods`, `c:foods/cooked_meat`, `c:crops`, plus `minecraft:logs`); the jar could not be opened here. The self-test `skillbook.shop.items` fails loudly if one of them is empty in the real game, which is the intended way to find a wrong name; in play an empty tag would simply leave that one line off the shelf.
* **A purchase hands over the item's default stack.** Goods that need data components (a brewed potion, an enchanted book with its enchantment) are not possible yet: the `potions` type exists for a future component-aware good. The witch sells plain items only.
* **Other mods' rare items in the broad tags.** `#c:ingots` and `#c:gems` can contain another mod's rare ingot or gem, which would then be sold at the line's cheap price. The ritual catalysts are banned by id, but a modpack with its own precious ingots may want to add them to `exclude` or to the general type's `banned_ids`. There is no loop (the witch does not buy them), only a cheap supply.
* **Config can still cheapen the shop.** The loop guard looks at the prices in force and never allows a profit loop, but a server owner can make everything cheap with `priceScale` and make the witch pay little with `sellScale`; that is a choice, not a loop.
* **Decision for the owner:** the price levels (for example 3,000 to 5,000 per ingot against 4,000 for a raw monster meat) are my reading of the existing scale; they are one number each in `stock.json` and can be retuned with `/reload`.
