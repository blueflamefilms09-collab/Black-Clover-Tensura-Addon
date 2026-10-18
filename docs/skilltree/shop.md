# The Black Market (`skilltree/shop/**`, `client/skilltree/shop/**`, the old witch)

The owner's text: *"The Black Market Shop: a hidden, dusty shop run by an old witch selling high-tier / rare skill books at exorbitant prices (2 to 3 million gold due to market risk). Shelf inventory:
beginner archery, magic booster, Jack-in-the-Box and elemental skills. The target: the Fang of Smoldering Insanity."* This document is the WHY of everything that was built for it, file group by file group,
then the data files, the economy, the settings, the packets, and what could not be verified. Read `docs/skilltree/design.md` section 8 for the design this implements and `docs/skilltree/core.md` for the
engine it sits on (the tree, the books, `SkillTreeApi`).

**New versus replacement (the repo rules, rule 5):** all of it is NEW. The mod had no shop, no gold, no witch and no skill books to sell; nothing existing is replaced or removed. The only existing file edited is
`arc/ArcNpcEntity` (three lines, section 2). **Canon:** the owner's text is the whole lore, so no wiki research was needed. The witch has no name in it, so she is called plainly "Old Witch" (a made-up name
could be taken for the original's), and her look (crooked hat, plum robe, eyepatch, pipe, lantern staff) is a design choice, not canon.

## 0. What the player sees

1. After somebody has beaten the first boss of the roster (Revchi Salik), an old witch stands in the Hub. A plain right click gives dialogue: she is dry, greedy and cackling, hints that she sells books
   ("A fang that smoulders ... for the right price") and tells the player to **sneak and show her their coin**.
2. **Sneak + right click** opens her shop (the screen below). Looking at her also shows a one-line hint in the action bar, so the shop can be found by someone who never read the dialogue.
3. The screen has the **shelves** (left: route books at 2 to 3 million gold, SP primers at 120 thousand to 800 thousand, each with its price, how many are left and a Buy button), **your goods** (right: what in
   your inventory she buys, with a Sell button per stack and "All stones / All meat / All materials" buttons) and your **gold** at the top, with the time to the next restock.
4. Gold has no other source: she pays it for dungeon goods (Magic Stones, crystals, monster meat, monster materials, gems, nether stars ...), or an operator gives it with `/multiverse tree gold`.
5. A bought book is an ordinary Skill Book or SP Book item from the items builder (`SkillBookItem`, `SPBookItem`); reading it is the core's job.

## 1. Map of the files

| Group | Files | Section |
|---|---|---|
| The witch | `shop/BlackMarketWitch`, `tools/arc/char_black_market_witch.py` (+ generated geo/animation/textures/scene), `data/nusmp/arc/dialogue/black_market_witch.json` | 2 |
| The door | `shop/ShopHook`, one edit in `arc/ArcNpcEntity.mobInteract` | 2 |
| Gold | `shop/GoldLedger`, `shop/GoldFormat`, `shop/MarketGold` | 3 |
| Data tables | `shop/PriceTable`, `shop/StockSchema`, `shop/StockEntry`, `shop/MarketData`, `data/nusmp/black_market/{prices,stock}.json` | 4 |
| The shelf | `shop/StockRoll`, `shop/StockBoard`, `shop/MarketState` | 5 |
| The deals | `shop/ShopRules`, `shop/ShopService`, `shop/ShopGate`, `shop/ShopGoods` | 6 |
| The network | `shop/ShopSnapshot`, `OpenShopPayload`, `ShopSyncPayload`, `BuyPayload`, `SellPayload`, `SellAllPayload`, `shop/ShopNet` | 7 |
| Settings and commands | `shop/ShopConfig`, `shop/ShopCommands` | 8 |
| Wiring and self-tests | `shop/ShopSystem`, `shop/ShopSelfTests` | 9 |
| The screen | `client/skilltree/ShopClient`, `client/skilltree/shop/{ShopScreen,ShopPainter,ShopLayout,ShopColors}`, `textures/gui/black_market/*.png`, `tools/skilltree/{gen_shop_art,preview_shop}.py` | 10 |
| Tests | `src/test/java/.../skilltree/shop/*`, `.../client/skilltree/shop/ShopLayoutTest` | 11 |

## 2. The witch and the door

**WHY a plain `ArcCharacter.npc` record and not a new entity.** Every friendly face of the arcs is one record for the ONE shared entity `nusmp:arc_npc` (`ArcNpcEntity`), with its model, dialogue and
roster post picked up by id. A new entity class would need its own registration, renderer, spawn rules and hub post for no gain. `BlackMarketWitch.CHARACTER` is
`ArcCharacter.npc("black_market_witch", "Old Witch")` with plum and amber colours, magic `curse`, saga ALLIES and `after("revchi_salik")`. The prerequisite is the owner's "hidden": the roster brings her to the
Hub only once the dungeon has proven itself. `BlackMarketWitchTest` checks the record without the game.

**WHY the model is an art-kit spec.** `tools/arc/char_black_market_witch.py` is a normal character spec (`gen_arc_characters.py black_market_witch` builds the geo, the animation set, the texture, the glow layer
and the preview scenes): a stooped crone, crooked pointed hat with a gold band, plum robe patched with soot-grey cloth, a ragged shawl, stringy ash-white hair, a hooked nose, one amber eye and an eyepatch
over the other, a clay pipe with an ember, a belt of books and coin pouches, and a gnarled staff with a lantern on a chain. Two things go beyond the kit's vocabulary and are done in the spec itself:
the **staff** is a custom prop (`kit.register_prop("witch_staff", ...)`, authored upright, a zig-zag shaft, a burl with three claws, a hook, a three-link chain and a glowing lantern) and the **stoop** is a
hump built from the shawl's own cloth in `extra()` plus a constant forward lean added to every clip in `post()` (so every loop still ends where it began and the arms keep their hold on the staff).
The lantern, her one visible eye and the pipe's ember are the only glow, amber, the colour the shop screen uses for gold. 82 cubes, 441 quads, well inside the kit's budget.

**WHY the dialogue file has four topics.** `greeting` (first meetings, including ones that react to Revchi Salik being down and to progress), `hint` (14 lines: how to open the shop, the books by name, the
restock, the dungeon as the source of gold, the primers; a conversation mixes them and never repeats one back to back), `shop` (six lines: the words on her lips when the screen opens, picked server-side
so the screen shows a real line of the same book) and `farewell`. Farewell lines are written to be dual-use, because `ArcNpcEntity` says its farewell 80 ticks after a conversation starts, which
can be while the shop is still open. The keys come from `tools/gen_arc_dialogue_lang.py --write-keys`; `WitchDialogueTest` demands: 30 lines, ASCII, readable lengths, a hint that tells players to sneak, one
that names the Fang, conditions that name real bosses, no numbers in lines (prices move), no sentence shared with any other character, and none of the forbidden words.

**WHY a hook method and not shop code in the entity.** `ShopHook.tryOpen(ServerPlayer, ArcNpcEntity)` is the only connection. `ArcNpcEntity` must not learn what a shop is; the hook answers true only for
`black_market_witch` (no other character can ever open a shop), only while the shop is enabled and only while the player sneaks (otherwise the dialogue runs as before). It never throws: any exception is
logged and answered with false, so an error in the shop becomes "she talks" and not a kicked player. **The one edit outside my files**, in `ArcNpcEntity.mobInteract`, right after the client-side early return:

```java
if (player instanceof ServerPlayer shopper && com.newuniverse.nusmp.skilltree.shop.ShopHook.tryOpen(shopper, this)) {      // 1.62.0: the Black Market ...
    return InteractionResult.CONSUME;
}
```

## 3. Gold

**WHY a long inside the player's tree.** Gold is "a number in the player's tree" (design section 8): `SkillTreeApi.of(p).extra("market")`, key `gold`. It travels with the tree (one save, one copy on death, one
sync packet) and needs no attachment of its own. Two more numbers live in the same compound: `limit_day` and `limit_n` (the per-day purchase counter). Reading never creates the compound (`hasExtra`), so a
player who never saw the shop carries no extra bytes.

* `GoldLedger` (pure): the rules in one place so every caller obeys them. Never negative; cap `Long.MAX_VALUE / 4` (so adding two capped purses can never overflow); `add` saturates, handles
  `Long.MIN_VALUE`, and is clamped both ways; `take` refuses what cannot be afforded; `room` and `maxCount` say how many items of a price still fit under the cap; `mul` saturates.
* `GoldFormat` (pure): `full` (2,347,000), `compact` (2.35M, truncating to 3 significant digits so a price is never shown higher than it is), `clock` ("2d 4h", "5h", "<1h") for the restock time.
* `MarketGold`: the thin bridge to `extra("market")` (`get/set/add/take/boughtToday/recordPurchase/clearCounter`). Every write goes through `GoldLedger`; after a change the caller calls
  `SkillTreeApi.resync(player)` (a queued, coalesced sync, one packet per tick).

## 4. The data tables

**WHY data files.** The owner will tune prices and the shelf by hand, and a new route (a new JSON in `data/nusmp/skill_trees/`) should appear on the shelf without a code change. Both files are read by a
reload listener (`MarketData`, so `/reload` works) through pure validators that never throw: a bad row is dropped with a sentence, the good rows stay, and `/multiverse tree shop status` lists the
sentences. Throwing on a bad row would kill the whole reload for one typo. **Both files explain themselves**: JSON has no comments, so, as in the other data files of this mod, a key that starts with an
underscore (`_why`, `_why_meat`, ...) is a comment, ignored by both validators at every level (file, table, line, row) and not counted towards a limit; every line and every group of rows says why it is there.

### `data/nusmp/black_market/prices.json` (what the witch pays)

```json
{
  "bulk": ["stones", "meat", "materials"],
  "prices": {
    "nusmp:magic_stone":    { "gold": 50000, "category": "stones" },
    "minecraft:diamond":    { "gold": 200000, "category": "gems" },
    "minecraft:nether_star": 1500000
  }
}
```

A row is `{ "gold": n, "category": "name" }` or just a number (category `goods`). `bulk` lists the categories that "All <category>" may sweep: valuables (gems, metals, treasure) are deliberately NOT
in it, so one stray click can never sell a nether star. Limits: 512 rows, a price between 1 and 10^12. Rows naming an item the game does not have are dropped at load (`unknownItems`), so a price for
another mod's item is harmless. `PriceTable` is the pure table, `MarketData.priceOf(ItemStack)` the lookup.

### `data/nusmp/black_market/stock.json` (what she sells)

```json
{ "entries": [
  { "id": "fang_of_smoldering_insanity", "kind": "route_book", "route": "nusmp:smoldering_fang_of_insanity",
    "price_min": 2000000, "price_max": 3000000, "band_low": 0.6667, "stock": 1, "weight": 100, "flagship": true },
  { "id": "route_books", "kind": "route_book", "routes": "all", "price_min": 2000000, "price_max": 3000000, "stock": 1, "weight": 100 },
  { "id": "beginner_archery_primer", "kind": "sp_book", "sp_kind": "beginner_archery", "price_min": 120000, "price_max": 180000, "stock": [3, 5], "weight": 100 }
] }
```

`kind` is `route_book` (needs `route`, or `"routes": "all"`) or `sp_book` (needs `sp_kind`); `stock` is a number or `[min, max]`; `weight` is the percent chance the line is on the shelf at a restock (100 =
always); `band_low` lifts the floor of the price band (0.6667 = the top third); `flagship` frames the row in gold. **The all-routes line skips every route that has a line of its own**, so the Fang
is not on the shelf twice, and **a route added later appears with no edit** (the shelf is built from `RouteRegistry.all()` at roll time). Limits: 64 lines, ids unique, prices up to 10^12, stock up to
99; unknown keys only warn. `StockSchema` is the pure validator, `StockEntry` the parsed line.

## 5. The shelf

**WHY a pure function.** "Market risk" means the price moves, and it must be the same for every player of a world and survive restarts. Instead of storing rolled prices (which would need migration and
could disagree with the data file after an edit), every number on the shelf is a **pure function** of: the world seed, the restock period, a restock counter (the operator's forced restock) and the slot id.
Only what cannot be computed is stored: the period, the counter and the number bought of each slot (`MarketState`, a `SavedData` named `nusmp_black_market` in the overworld's data storage, a few dozen
bytes; an old world has nothing to migrate, it starts at period 0 with nothing bought).

* `StockRoll.roll(seed, period, epoch, slotId, entry)` gives `Roll(present, price, count)`. The mixer is **SplitMix64**, written out in six lines instead of `java.util.Random`, so a world's shelf cannot change
  when a Java version changes an implementation detail; names hash with FNV-1a. The price is rolled in `[priceMin + ceil((priceMax - priceMin) * bandLow), priceMax]` and rounded to **market numbers** (a trader
  says 2.35 million, not 2,347,812): step 1 under 1,000, 10 under 10,000, 100 under 100,000, 1,000 above; the floor rounds UP and the result is always inside the band.
* `StockBoard`: turns the lines and the real route ids into **slots** (`entryId`, or `entryId@routeId` for the all-routes line), rolls them, subtracts what was bought and gives `Offer`s; it also owns the clock:
  the **restock period is overworld game time divided by `restockDays * 24000`**. Game time, not day time, because day time wraps and `/time set` and sleeping move it both ways, which would re-roll the shelf or
  freeze it; game time only goes forward.
* A restock happens when the period changes (everything bought is forgotten with the old period) or when an operator runs `/multiverse tree shop restock` (the counter goes up, so prices and counts re-roll).

## 6. The deals

**WHY pure rules and one service.** `ShopRules` is a pure judge: given the facts of a purchase or a sale it returns one `Reply` (the verdict). The order of the checks is part of the contract and is tested:
shop closed, too far, unknown offer, sold out, price moved, daily limit, too poor (a player who is both far and poor is told "too far"). `ShopService` gathers the facts from the game, asks the judge, and only
then changes anything, in an order chosen so that a failure leaves the player whole: **the item is handed over before the gold is taken** (an error can lose a sale, never gold), and a sale removes the items
and adds the gold with nothing that can fail between. It does not send packets (`ShopNet` does, through `reply`), which keeps it callable from a self-test with a stand-in player.

* **Reach** is measured by looking for the witch within 8 blocks, every time. There is no "open shop" session to forge, to leak, or to forget to clear when a player teleports.
* **Price moved**: the client sends the price it saw; if a restock happened between the render and the click, the buy is refused with a fresh picture instead of silently charging another price.
* **The item in a slot is compared with the item the client named** (`SellPayload.item`), so an inventory that shifted between the last picture and the packet sells nothing wrong.
* Only the hotbar and main inventory (36 slots) can be sold from; armour and the off hand never.
* **Sell all** only touches the categories listed as `bulk`, stops cleanly when the purse is full (`FULL_PURSE`), and answers `NOTHING` when nothing matched.
* Scaling and rounding (`ShopRules.buyPrice` / `sellUnit`): what she pays per item is floored after `sellScale`, so a fraction is never paid out of nothing; what a book costs is rounded to the nearest
  market number after `priceScale` (so a scaled price still looks like a trader's number), never below 1 and never above the ledger's cap.
* `ShopGate` allows one shop action per player per server tick (a modified client cannot flood the server with buys), `ShopGoods` is the adapter to the items builder:
  `SkillBookItem.stack(route.book())` and `SPBookItem.stack(kindId)` (the only coupling to that code, in one place; an unknown SP kind gives an EMPTY stack, which the shop treats as "cannot be made":
  never shown, never charged for). It never imports a Grimoire class.
* After a purchase the quest door fires `tree.shop.buy` with the offer id.

## 7. The network

**WHY these packets.** NeoForge 21.1 has no `SimpleChannel`; typed `CustomPacketPayload` records with `StreamCodec`s registered on the mod bus are the supported, versioned way, and a bad packet fails loudly
at decode. Exactly like the core's `TreeNet`: `event.registrar("1").optional()` (so a client without the mod can still join), handlers hop to the main thread with `ctx.enqueueWork` and never throw (NeoForge
disconnects a player whose handler throws, far too harsh for a refused purchase). The two server-to-client handlers call the client class `ShopClient` only inside lambdas, so a dedicated server never loads it.

| Packet | Id | Direction | Fields |
|---|---|---|---|
| `OpenShopPayload` | `nusmp:shop_open` | S2C | the whole `ShopSnapshot` |
| `ShopSyncPayload` | `nusmp:shop_sync` | S2C | `ShopSnapshot`, `reply` (an ordinal of `ShopRules.Reply`), `variant` (a random byte so her two lines for a reply alternate) |
| `BuyPayload` | `nusmp:shop_buy` | C2S | `offer` (slot id), `price` (the price the player saw) |
| `SellPayload` | `nusmp:shop_sell` | C2S | `slot`, `item` (the item id the player saw there), `count` |
| `SellAllPayload` | `nusmp:shop_sell_all` | C2S | `category` |

**WHY the whole picture each time (`ShopSnapshot`), not deltas.** The picture is tiny (capped: 128 offers, 64 goods, 16 bulk categories; the codec rejects more) and a full snapshot cannot drift: after any
deal the screen shows exactly what the server holds, even when the deal was refused. The client never merges. `ShopSyncPayload` is separate from `OpenShopPayload` so that a late "after a deal" packet
with no screen open is dropped instead of opening a shop. Only append to `ShopRules.Reply` (its ordinals go on the wire).

## 8. Settings and commands

`nusmp-skilltree-shop-server.toml` (registered in `ShopSystem.init`; plain `define` / `defineInRange` only, with a `get` helper that falls back to the default so a read during early loading is safe):

| Key | Default | Range | Meaning |
|---|---|---|---|
| `enabled` | `true` | | Off: she only talks (sneaking opens nothing). |
| `restockDays` | `3` | 1 to 365 | In-game days between two restocks (prices re-roll at each). |
| `priceScale` | `1.0` | 0.01 to 100 | Multiplier on what the player PAYS. |
| `sellScale` | `1.0` | 0 to 100 | Multiplier on what the witch PAYS. 0 stops her buying. |
| `maxBuyPerDay` | `0` | 0 to 100 | Books per player per in-game day. 0 = no limit. |

Commands (permission 2, because they make value out of nothing); `ShopCommands.node()` is the `gold` node and `ShopCommands.shopNode()` the `shop` node, both for `/multiverse tree`:

* `/multiverse tree gold <player>`: shows the purse. `... gold <player> add <n>` (negative takes), `... gold <player> set <n>`: clamped by the ledger, the client is re-synced so an open screen is true.
* `/multiverse tree shop status`: open or closed, the restock clock and period, the number of lines and prices, **every data problem** and every priced item the game does not have.
* `/multiverse tree shop stock`: the shelf right now (id, kind, price after `priceScale`, left of total). `/multiverse tree shop restock`: a new shelf at once.

## 9. Wiring and self-tests

`ShopSystem.init(modBus, modContainer)` (called by `SkillTreeSystem`, safe to call twice) registers the config, `ShopNet.register` on the mod bus, the reload listener `MarketData`, the cleanup on
`ServerStoppedEvent` (data and gate, so a second world in one session starts clean) and `PlayerLoggedOutEvent` (the gate entry), and the self-tests. **WHY self-tests:** nothing of the shop can be played by its
author, so `/multiverse mega selftest` (and the CI smoke test) runs two checks in the real game:

* `tree.shop.stock`: both files loaded with no problem; every `minecraft:` / `nusmp:` item she buys exists; the shelf from the REAL route registry is deterministic, differs between periods, offers every
  route its line promises, every offer turns into a real item stack (a wrong factory name or an unknown SP kind shows here) and the flagship sits in the top third.
* `tree.shop.buy`: a stand-in player (`FakePlayer`) and a real witch entity: refusals when poor, far, price moved, unknown (each leaving gold, stock and inventory untouched), a real purchase that moves exactly
  the right gold, stock and item, a sold-out refusal, a sale, a bulk sale, and the refusal of a bulk sale of a valuable. Both pass with an empty route registry (and say so) and put the world back as they found it.

## 10. The screen

**WHY hand-drawn, and WHY it decides nothing.** `ShopScreen` is a plain `Screen` drawn by hand, because both lists scroll and every row is custom (an item, a name, a price, a button) and vanilla widgets would
only be harder to keep in step with the layout. It uses the mod's own panel kit (`UiPanel`, `MagicCircle`, `UiTheme`), tinted plum and amber, with cobweb corners and drifting dust, so it reads as part of the
same UI and as the witch's back room. Every number it shows comes from the last `ShopSnapshot`; a click only sends a request, and the next picture shows what really happened; the Buy button is greyed
from the same numbers purely as a courtesy. It does not pause the game and closes when the witch is out of reach (the server measures too). The witch **talks** in the bubble at the bottom: her opening line
(a `shop` dialogue line), a typed-out reply to every deal (two variants per reply, `gui.nusmp.shop.reply.<name>.1/.2`), and a comment on whatever the mouse is over.

* `ShopLayout` (pure, tested across 12 window sizes): every rectangle and hit test, so drawing and clicking cannot disagree; a bulk-button planner that falls back from "All stones" to "Stones" to tight padding
  before dropping a button; `wrap`, a greedy text wrapper that cuts the last line with "..." so speech can never leave the bubble.
* `ShopColors` (pure, tested): a route book is named in its route's colour, and route colours are chosen for icons, not for dark glass; `lift` keeps the hue and raises the brightness (a deep purple
  must not come out grey-blue, which a plain "every channel at least 0x70" would do), and leaves an already bright colour exactly as it is.
* `ShopPainter` (stateless): the palette, the cobwebs, the dust (a function of the clock, so it stands still with UI motion off), the coin, buttons, rows, scrollbars and the bubble.
* `ShopClient`: where the two packets land, and the action-bar hint while the crosshair rests on the witch (sneak and right click; right click once sneaking). **Decision:** the owner wants no separate
  key, and a hidden shop nobody can find is worse than one with a hint; to hide it completely, remove the one `ClientTickEvent` listener.
* **Art** (`tools/skilltree/gen_shop_art.py`): `cobwebs.png` (96x96, four 48x48 corner webs, drawn deterministically) and `witch.png` (40x42: her REAL model rendered by the repo's headless renderer, cut out of
  the sky by flood fill and reduced), so the portrait follows the model by construction: change her spec and run the script again.
* **Preview** (`python3 -B tools/skilltree/preview_shop.py OUT_DIR`): a Pillow mock of the screen from the same numbers (it reads the layout constants out of `ShopLayout.java` and stops when they differ),
  eight scenes (normal, hovering a book, hovering a good, hovering the Sell button, a poor player, 854x480, the tightest 320x180, empty). It is a mock, not the game: item sprites are stand-ins and the font
  is an approximation of Minecraft's.

## 11. Tests

Pure unit tests (run with `jt.sh`, all pass): `GoldLedgerTest`, `GoldFormatTest`, `PriceTableTest`, `StockSchemaTest`, `StockRollTest` (determinism, bounds, the Fang in the top third, different days differ,
the weight is the chance, the whole stock range is used), `StockBoardTest`, `ShopRulesTest`, `ShopDataFilesTest` (the shipped JSON files against the owner's bands and stock), `BlackMarketWitchTest`,
`WitchDialogueTest`, `ShopLayoutTest`, `ShopColorsTest`; plus the repo's own `DialogueFilesTest` with the new file in the tree. Mutation-checked: removing the band floor from `StockRoll` fails the Fang test; swapping the
daily-limit and poor checks in `ShopRules` fails the order test.

## 12. The economy: the numbers and why

| What | Number | Why |
|---|---|---|
| Route books | 2,000,000 to 3,000,000, rolled per restock | The owner's band. "Market risk" = the roll. One copy of each per restock, so a book is a race between players ("whoever is quicker with their coin"). |
| Fang of Smoldering Insanity | top third of the band: 2,666,700 to 3,000,000 | The owner's "the target": the book everyone wants costs the most. |
| Beginner Archery Primer (+2 SP) | 120,000 to 180,000 | The owner's band: 60k to 90k per point, the cheapest point on the shelf. |
| Elemental Primer (+3 SP) | 250,000 to 350,000 | About 83k to 117k per point. |
| Magic Booster (+5 SP) | 600,000 to 800,000 | About 120k to 160k per point: the lump sum costs more per point, which is what a "booster for the greedy" is. |
| Jack-in-the-Box | 300,000 to 450,000 | The gamble (the items builder decides the points). |
| SP stock | 3 to 5 per restock | The owner's range. |
| Restock | every 3 in-game days | The owner's default; configurable. |
| Magic Stone (`nusmp:`) | 50,000 | The dungeon's staple; 40 to 60 stones buy a route book. |
| Magic crystals (Tensura) | low 30,000, medium 100,000, high 350,000 | Rarer tiers pay more than the commons. |
| Raw monster meat | 4,000 (all five raw kinds) | Bulk food: cheap, so selling it is a chore and not a fortune. |
| Cooked monster meat | 8,000 to 10,000 | "Cooked fetches more": the witch's own hint says so, and cooking costs fire and patience, so it should pay about twice. |
| Monster materials (Tensura) | bat wing 12k, spider fang 15k, leather D 20k, horn 40k, leather C 60k, leather B 180k, unicorn horn 150k | A handful, graded by the rarity of the mob. |
| Diamond / emerald | 200,000 / 60,000 | The owner's numbers. |
| Gold ingot | 25,000 | The owner's number. |
| Netherite scrap / ancient debris | 400,000 / **300,000** | The owner's debris number was 800,000, which would make debris worth MORE than the scrap it smelts into; debris is set a little under the scrap so smelting adds value. One number in `prices.json`. |
| Enchanted golden apple / nether star | 600,000 / 1,500,000 | The owner's numbers. |
| Relic fragments | none | The mod has no relic fragments (its relics are finished items). Add a row when one exists. |

**Balance observation for the owner (not changed, these are his numbers):** at 200,000 gold per diamond a route book is about 10 to 15 diamonds, which is cheap against the value of a whole new magic.
If that is too cheap, the knobs are `sellScale` (what she pays) and `priceScale` (what books cost), both per server, or the numbers in the two JSON files.

## 13. Decisions and alternatives rejected

* **Gold in the tree's `extra("market")`** (design). Rejected: a new attachment (one more sync and copy-on-death to keep right), and a scoreboard (any command block could edit it).
* **The shelf is a pure function of seed / period / counter / slot, only counts are stored.** Rejected: storing rolled prices (drifts from the data file, needs migration) and per-player stock (the owner
  wants one market for the world).
* **Restock clock on overworld game time.** Rejected: day time (wraps, and `/time set` or sleeping would re-roll or freeze the shelf).
* **A whole snapshot after every deal.** Rejected: delta packets (can drift, more code, for lists of at most a few dozen rows).
* **`BuyPayload(offer, price)` and `SellPayload(slot, item, count)`** instead of the brief's `BuyPayload(entryId)` / `SellPayload(slot, count)`. The extra fields make a race harmless: a restock or a shifted
  inventory between the render and the click is refused with a fresh picture instead of charging another price or selling another item.
* **Reach measured by searching for the witch every time.** Rejected: a server-side "open shop" session (state to forge, leak, and clear on teleport, death, dimension change and logout).
* **Item before gold on a purchase.** Rejected: gold first (an exception in between would take gold and give nothing).
* **"Sell all" limited to a `bulk` list.** Rejected: sell everything sellable (one misclick sells a nether star).
* **Bad data rows are dropped with a sentence.** Rejected: failing the reload (one typo would take down the whole shop) and silent skipping (nobody would find the typo).
* **Sneak + right click, no keybind** (the owner's rule), plus the dialogue hint and the action-bar hint. Rejected: a keybind, and a shop with no hint at all.
* **A hand-drawn screen.** Rejected: vanilla widget trees (scrolling custom rows fight the layout).
* **Shift (sell the whole stack) only counts after shift has been seen released once since the screen opened.** The shop opens on a SNEAKING click, so shift is still down when it appears; honouring it
  from the first frame would make the first click on Sell sell a whole stack by accident. Right click on Sell always sells the stack.
* **Pure layout, wrapper and bulk planner.** Rejected: computing sizes inline in `render` (they only break at window sizes nobody tried).
* **Portrait generated from the model by script.** Rejected: hand-painting a 40x42 picture (it would drift from the 3D figure the first time she is edited).
* **Replies as language keys with two variants.** Rejected: server-sent text (the server may speak another language than the client) and a single line (it grates by the third deal).

## 14. What was aligned with the core (`stcore`)

Gold is read and written only through `SkillTreeApi.of(p).extra("market")`, and the tree (which holds it) goes to the client in `TreeSyncPayload`; after every change `SkillTreeApi.resync(serverPlayer)`
is called. Payloads are registered on the shop's own `RegisterPayloadHandlersEvent` listener exactly like `net/TreeNet` (`registrar("1").optional()`, `enqueueWork`, handlers never throw). `SkillTreeApi.isEnabled()`
is checked, so the shop closes when the engine is off (a book that cannot be read must not be sold). The goods are made by the items builder's own factories, so a bought book is exactly the book a
dungeon drop or the creative tab gives. The quest hook key is `tree.shop.buy`, in the `tree.*` family. `ShopCommands.node()` has the shape the
core's `SkillTreeCommand` expects (a `LiteralArgumentBuilder<CommandSourceStack>`, the lead adds `tree.then(...)`). Isolation: nothing under `skilltree/**` or `client/skilltree/**` imports a Grimoire class.

## 15. What could NOT be verified (nothing here has run in the game)

* The mod does not run in this environment: no screen, packet, entity or world test has been executed. Everything was type-checked against API stubs of Minecraft and NeoForge together with the REAL sources
  of the core and the items builder, unit-tested in the local JUnit shim for the pure parts, and the screen and the model were looked at only through the Pillow mock and the headless renderer.
* **Items builder:** the factories and SP kind ids were checked against the items builder's committed code (read only): `SkillBookItem.stack(String bookId)`, `SPBookItem.stack(String kindId)`, and the kinds
  `beginner_archery`, `elemental_primer`, `magic_booster`, `jack_in_the_box` exist (the fifth, `greater_primer`, is dungeon loot only and is not on the shelf). Not run together in the game; `tree.shop.stock` will say so if they ever diverge.
* **Tensura item ids** (`giant_bat_wing`, `spider_fang`, `monster_leather_b/c/d`, `beast_horn`, `unicorn_horn`, the magic stone and the three crystals) come from the Tensura API dump's field names; only the
  stone and the crystals are also used elsewhere in this repo. A wrong id costs nothing: the row is dropped at load and listed by `shop status`.
* **API behaviour not checked at runtime:** `SavedData.Factory` with a null `DataFixTypes`, `ItemHandlerHelper.giveItemToPlayer` dropping the overflow at the feet, `FakePlayer` as a customer in the
  self-test, `GuiGraphics` scissor and `renderItem` inside the scrolling lists, `ClientTickEvent.Post` and `Gui.setOverlayMessage` for the hint, the sound events chosen for her noises.
* **Look in the game:** how the hump reads in the game's light, how the portrait reads at 1x GUI scale, and that the item icons of the real books look right beside the names (the mock uses stand-ins).
* **`ArcNpcEntity` interplay:** the witch re-arms her conversation (`beginTalk`) after every deal so her farewell does not fire mid-shopping; this relies on `beginTalk`'s 80-tick timer as read, not as run.
* The generated `tools/arc/manifest.json` entry was NOT left in the file: that file is shared by every character job, so it is regenerated once at integration (`python3 -B tools/gen_arc_characters.py`).

## 16. Integration lines (for whoever merges this job)

1. `ArcCharacters.bootstrap()`: `com.newuniverse.nusmp.skilltree.shop.BlackMarketWitch.registerAll();`
2. `SkillTreeCommand`, where the local variable `tree` has its comment:
   `tree.then(com.newuniverse.nusmp.skilltree.shop.ShopCommands.node());` and `tree.then(com.newuniverse.nusmp.skilltree.shop.ShopCommands.shopNode());`
3. The language file gets the shop's keys: every `gui.nusmp.shop.*` key (screen texts, the witch's reply lines and the comments she makes about what the mouse touches) and the 30
   `arc.dialogue.black_market_witch.*` lines that `python3 tools/gen_arc_dialogue_lang.py` prints for her dialogue file (86 keys together; none clashes with another job's).
4. Regenerate `tools/arc/manifest.json` (`python3 -B tools/gen_arc_characters.py`) so the witch has her entry.
5. Optional: so that meeting her is an advancement like the other friends, append `black_market_witch|Old Witch|allies|npc` to `tools/quests/arc_roster.txt` and run `tools/quests/gen_arc_adv.py` and
   `tools/quests/merge_lang.py` (see `docs/arc/world.md`).
6. Nothing to add for `SkillTreeSystem` / `SkillTreeClient`: they already call `ShopSystem.init(modBus, modContainer)` and `ShopClient.init(modBus)`, which this job fills in.
