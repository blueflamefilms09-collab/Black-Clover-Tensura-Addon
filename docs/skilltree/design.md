# The Skill Book & Skill Tree engine (1.62.0): route-unlocking skill books, SP, data-driven trees, Fairy Tail magic

This is the BINDING design of the whole job. Every builder reads it first; where a builder's brief and this file disagree, this file wins and the builder says so in the report. The contract code
is in `src/main/java/com/newuniverse/nusmp/skilltree/**` and `.../client/skilltree/**`; read it before this text. **Every class, JSON structure, screen and packet has a "Why" (class Javadoc whose first paragraph
starts with `Why:`, and the WHY index in section 11 of this file); the owner made this a strict requirement.**

## 0. What the owner asked for, and new versus replacement

The owner's request (2026-10-18), kept as the specification. A new **Skill Book & Skill Tree System** inspired by *The Exiled Heavy Knight Knows How to Game the System* (the hero reads hidden, rare skill
books for a "defective" class; in a hidden dusty Black Market shop an old witch sells them for 2 to 3 million gold; the target is the *Fang of Smoldering Insanity*): the foundation for adding anime magic
systems, starting with **Fairy Tail** magic (Dragon Slayer, Maker). Consuming a specific Skill Book permanently unlocks that whole Skill Tree ROUTE for the player, who then invests Skill Points (SP) in its
nodes. Six systems: (1) data-driven JSON skill trees with a reload listener; (2) a per-player capability (unlocked routes, SP, node levels, active class) with client sync; (3) route-unlocking Skill Books and
SP Books, consumed with an anime-style unlock; (4) integration into the EXISTING Black Clover menu (a tab / button, no separate keybind) with a `SkillTreeScreen` and a C2S packet to buy nodes; (5) Fairy Tail
content (a Fire Dragon Slayer tree with Iron Fist, Roar, Secret Art) and the ability hook into the spell keybinds; (6) isolation from the Black Clover Grimoire, and Global Loot Modifier drops in the
Mega-Dungeon (Abyssal monsters, Decennial bosses). Exclusion rule: this system never governs the Black Clover Grimoire (innate, bound items); the two share no data, no currency, no packet and no screen logic.

**New or replacement (CLAUDE.md rule 5).** Nothing like it exists: the mod has Tensura skills, Grimoire pages (mastery), the System's stats (Solo Leveling style) and the hero weapons' forms (also 1.62.0, a different
system), but no SP, no route, no skill book. The whole job is **NEW**; nothing is replaced or removed. It touches the existing menu only by ADDING a button (and keeps both existing screens as they are).

## 1. Map and ownership

Package `com.newuniverse.nusmp.skilltree` (server and shared) and `com.newuniverse.nusmp.client.skilltree` (client). One entry point, `SkillTreeSystem.init` (called from the mod constructor, already wired), starts: the
attachment and the four Fairy Tail skill registers on the mod bus, then `TreeCore.init`, `ItemsSystem.init`, `ShopSystem.init`. Each of these registers its own config file, payloads and self-tests. Client: `SkillTreeClient.init`
(starts `ItemsClient`, `ShopClient`). Commands: `/multiverse tree ...` (`SkillTreeCommand`, pre-wired). Smoke prefix `tree`.

| builder key | owns (create or replace; nobody else edits them) |
|---|---|
| `stcore` | `TreeCore`, `SkillTreeApi` (the real rules), `SkillTreeCommand`, `data/RouteRegistry` (reload listener, validation, client copy, indexes), `data/RouteSchema`, `net/**` (all payloads and handlers), the config `nusmp-skilltree-server.toml`, quest hooks, self-tests `tree.core.*`, the isolation test, docs `core.md` |
| `stitems` | `item/**` (Skill Book, SP Book, components, use logic, unlock effects), `ItemsSystem`, `loot/**` (the Global Loot Modifier `nusmp:skill_books`), `client/skilltree/ItemsClient`, item models and icons, config `nusmp-skilltree-loot-server.toml`, tests `tree.items.*`, docs `items.md` |
| `stgui` | `client/skilltree/SkillTreeClient`, `SkillTreeScreen`, `MenuInjection` (the button in the Four Kingdoms and Multiverse Status screens), widgets, previews, lang, docs `gui.md` |
| `stfa` | `ft/FireDragonSkills`, `ft/LightningDragonSkills` and the skill classes of the Fire and Lightning Dragon Slayer routes, `data/nusmp/skill_trees/fire_dragon_slayer.json`, `lightning_dragon_slayer.json`, docs `ft_fire.md`, `ft_lightning.md` |
| `stfb` | `ft/SkyDragonSkills`, `ft/IceMakeSkills` and the skill classes of the Sky Dragon Slayer and Ice-Make routes, `sky_dragon_slayer.json`, `ice_make.json`, docs `ft_sky.md`, `ft_ice.md` |
| `stshop` | `shop/**` (the Black Market shop, the gold ledger, stock, buy and sell), the witch NPC (`ArcCharacter`, dialogue, model spec), `ShopSystem`, `client/skilltree/ShopClient`, config `nusmp-skilltree-shop-server.toml`, tests `tree.shop.*`, docs `shop.md` |
| the lead | everything else: `data/SkillRoute`, `SkillNode`, `state/**`, `ability/TreeSkill`, `AbilityKit`, `SkillTreeSystem`, the `ft` registers' declarations, `NUSMP`, `NUCreativeTab`, `MultiverseCommands`, `en_us.json`, `smoke.yml`, version files, README, CHANGELOG |

A builder that needs a change in a lead-owned class states it in the report ("CONTRACT CHANGES WANTED"); it does not edit it. Exceptions: the shop builder may add a 3-line shop hook to `arc/ArcNpcEntity.mobInteract` and one
`ArcRoster.register` line is a LEAD integration line; the items builder may add the entry `nusmp:skill_books` to `data/neoforge/loot_modifiers/global_loot_modifiers.json`.

## 2. System 1: the data-driven registry (`data/nusmp/skill_trees/<name>.json`)

* **Loader**: a `SimpleJsonResourceReloadListener` on `data/<ns>/skill_trees` (added in `AddReloadListenerEvent`) builds `SkillRoute` records; the route id is `<namespace>:<file name>`. A malformed route or node is
  logged and skipped; the rest still loads; a datapack can replace any file. The validation lives in a pure class `RouteSchema` (no Minecraft types) with tests: ids unique and lower-case snake, parents exist and the
  graph has no cycle, a node's parents come before it in the file, `cost` 1..50, `maxLevel` 1..10, `costs` no longer than `maxLevel`, `values` tables of finite numbers, positions on a grid of 0..12 columns and 0..9 rows,
  at most 40 nodes per route, `book` is a lower-case resource location, `classId` lower-case snake and unique among routes, `color` a `#RRGGBB`, a node's `skill` (when present) names a ManasCore skill that exists (checked
  at server start by the self-test, not by the pure schema).
* **Schema** (the record is `SkillRoute` / `SkillNode`):

```json
{
  "magic": "fairy_tail",
  "name": "Fire Dragon Slayer",
  "desc": "The lost magic that teaches a mage to eat flame and to breathe it back.",
  "book": "nusmp:fire_dragon_slayer_book",
  "book_name": "Fang of Smoldering Insanity",
  "book_lore": "A dusty tome sought by every mage who was told his class was not enough.",
  "color": "#E8541C",
  "icon": "minecraft:blaze_powder",
  "class_id": "fire_dragon_slayer",
  "duplicate_sp": 3,
  "loot_weight": 1,
  "nodes": [
    { "id": "iron_fist", "name": "Fire Dragon's Iron Fist", "desc": "...", "parents": [], "cost": 1, "max_level": 5,
      "skill": "nusmp:ft_fire_dragons_iron_fist", "passives": [], "values": { "damage": [6, 7, 8, 9, 11], "burn_seconds": [3, 3, 4, 4, 5] }, "x": 0, "y": 4, "icon": "minecraft:blaze_rod" },
    { "id": "roar", "name": "Fire Dragon's Roar", "parents": [ { "node": "iron_fist", "level": 3 } ], "cost": 2, "costs": [2, 3, 4], "max_level": 3,
      "skill": "nusmp:ft_fire_dragons_roar", "values": { "damage": [10, 13, 16], "range": [9, 11, 13] }, "x": 2, "y": 3 }
  ]
}
```
  `parents` entries are node ids (level 1 needed) or `{ "node": id, "level": n }`. `x`/`y` place the node on the canvas (the screen draws lines from each parent). Why JSON and a reload listener: new magics are data;
  a server owner or the owner can retune or add a tree with a datapack and `/reload`, with no rebuild. Why a custom listener and not a datapack registry: the trees are small, validated in one place, indexed for lookups
  by book and by skill, and need a client copy anyway (a datapack registry would add a codec, a dynamic registry sync and freeze rules for no gain).
* **Client copy**: the game does NOT send a mod's own JSON data to clients. The server sends the validated routes to a player on login and on every reload (`OnDatapackSyncEvent`) in one payload (`SkillRoute` has a stream
  codec); the client keeps them in `RouteRegistry`'s client side. The screen reads only that copy.

## 3. System 2: the per-player tree (`state/IPlayerSkillTree`, attachment `nusmp:skill_tree`) and the network

* **Storage**: a NeoForge data attachment (not a Forge-style capability: in NeoForge 21.1 attachments are the data store; capabilities expose behaviour), serialized as NBT (routes, sp, spent, node levels keyed
  `<route>#<node>`, active class, class-change time, per-system `extra`), `copyOnDeath`, empty = not saved. The interface `IPlayerSkillTree` is the contract; `PlayerSkillTree` the implementation.
* **Network** (NeoForge 21.1 has no `SimpleChannel`; the equivalent is `CustomPacketPayload` records registered in `RegisterPayloadHandlersEvent` with a `StreamCodec`, protocol version "1"): S2C `TreeSyncPayload` (the full tree
  of the player; sent on login, respawn, dimension change, and after every change, throttled to one per tick); S2C `RoutesPayload` (all routes, on login and datapack sync); C2S `UnlockNodePayload(route, node)`
  (buy one level); C2S `SetClassPayload(classId)`; S2C `RouteUnlockedPayload(route)` (owned by the items builder: the banner and flash). Handlers run in `enqueueWork`; every C2S payload is re-validated on the server (the
  client is never trusted: the route must be unlocked, the node must exist, the parents must be at their levels, the node must not be maxed, the SP must cover the price, the player must be alive and within a rate
  limit of 10 purchases per second).
* **The rules** (pure class `TreeRules` with tests, used by `SkillTreeApi`): buyability = route unlocked, not maxed, every parent at its level, SP >= price of the next level; the purchase spends SP, raises the level, adds
  to `spSpent`, learns the node's ManasCore skill when it reaches level 1, fires the quest hook, syncs. Respec (command and config `respec`, default off for players): refunds `totalCost` of the cleared nodes, forgets
  their skills. Active class: the first unlocked route sets it; a player changes it in the screen (a cooldown `classSwitchSeconds`, default 30); only the ACTIVE class's abilities can be cast (config `requireActiveClass`, default true).
* **Granting abilities**: a node's `skill` is a registered ManasCore skill (a `TreeSkill`); learning and forgetting go through `SkillAPI.getSkillsFrom(player)` (the same API the Grimoire and the Charter use). `reconcileSkills`
  makes the player's skills match the tree on login and respawn, so a crash or a datapack change never leaves a skill that the tree does not back (and the cast gate still blocks it).

## 4. System 3: the items (`item/**`, owned by `stitems`)

* **`nusmp:skill_book`** (class `SkillBookItem`): ONE registered item; the data component `nusmp:skill_book_id` (a string) says which book it is, and the route that lists that id as its `book` is the one it opens. Why one item:
  items cannot be registered by a datapack but routes can, so a new magic must not need a new item; the creative tab offers a stack per route; the name, colour and lore come from the route (client copy). Use (right click,
  main hand, server): look up the route by book id; unknown -> "The pages are blank." (nothing consumed). Not unlocked -> consume one book, `SkillTreeApi.unlockRoute(player, route, true)`: the anime-style unlock (a
  circle effect and flash of the route's colour through `VfxSpawn`, the sounds `UI_TOAST_CHALLENGE_COMPLETE` + `ENCHANTMENT_TABLE_USE` + a low chime, a title "ROUTE UNLOCKED: <name>" with the route colour sent as
  `RouteUnlockedPayload` and drawn as a banner), the first route also sets the active class. Already unlocked -> by config `duplicateBook` (`SP` default: the book is consumed and gives the route's `duplicate_sp`
  as SP with a message; `REFUSE`: nothing is consumed and the player is told). Creative players keep the book. A use cooldown of 20 ticks.
* **`nusmp:sp_book`** (class `SPBookItem`): the component `nusmp:sp_amount` (int) and an optional `nusmp:sp_kind` (a string: `beginner_archery`, `magic_booster`, `jack_in_the_box`, `elemental_primer`, `greater_primer`)
  select the name and flavour; using it gives that many SP (`SkillTreeApi.addSp`). `jack_in_the_box` is a gamble: SP between 1 and 9, a 5% chance of 20 (the amount in the component is the guaranteed floor of 1).
* Recipes: none (books are loot and shop stock). Creative tab: one `skill_book` per route (from the registry; on a world with no routes none) and one `sp_book` per kind.
* Names and tooltips: translatable keys `item.nusmp.skill_book`, `.sp_book`, `tooltip.nusmp.skill_book.*`; the route's own text comes from the registry.

## 5. System 4: the GUI (`client/skilltree/**`, owned by `stgui`)

* **Integration**: NOT a standalone screen with a keybind. The existing Black Clover menus are `client/multiverse/FourKingdomsScreen` (the Four Kingdoms menu, opened from the slot in Tensura's magic screen) and
  `MultiverseStatusScreen`. `MenuInjection` listens to `ScreenEvent.Init.Post` (NeoForge event bus, client) and, for exactly those two screen classes, adds a themed "Skill Tree" button (the mod's `ThemedButton`)
  at a position computed from the screen's panel (never over an existing widget; the event hands the screen's `width`/`height` and its widget list, and the panel constants are public or mirrored), which opens
  `SkillTreeScreen(parent = the current screen)`; closing returns to the menu. Also a button for the same in the compact status panel only if that is cheap. Why an event and not an edit of `FourKingdomsScreen`:
  the existing classes stay exactly as they are (rule "add the new, keep the old"), the injection is one removable class, and the button survives a redesign of the menu because it is found by class, not by line.
* **`SkillTreeScreen`**: a themed panel (the mod's UI theme, `docs/gui_theme.md`): header (title, SP counter, the active class), left column: the routes grouped by magic family (Fairy Tail), each an entry with an icon, name, state
  (UNLOCKED / LOCKED) and a mini progress; centre: the selected route's node canvas (nodes at their `x`/`y`, lines from parents, scrolling/panning by drag or wheel, zoom optional); right: the selected node's detail (text,
  per-level numbers from `values`, price of the next level, parents with the levels they need, "Buy" button). A **locked** route is drawn greyed with a padlock, dashed lines, and the text "Requires: <book name>" + a hint where
  to find it ("Black Market, Mega-Dungeon Abyssal floors"); its nodes cannot be clicked. An **unlocked** route is fully coloured in its route colour; each node shows a state (locked by parents, available (glowing), owned
  level n/max, maxed); clicking an available node sends `UnlockNodePayload`; a purchase animates (a pulse and the line filling). A "Set active" button on the route sets the class. All numbers displayed come from the synced copy;
  the screen decides nothing.
* The screen is created and opened client-side only; it never touches Grimoire data and has its own widgets (no reuse of the grimoire page widgets).

## 6. System 5: Fairy Tail content and the ability hook (`ft/**`, `data/nusmp/skill_trees/*.json`)

* **Routes shipped** (4, to prove the engine): `nusmp:fire_dragon_slayer` (book *Fang of Smoldering Insanity*, the named target of the owner's text), `nusmp:lightning_dragon_slayer`, `nusmp:sky_dragon_slayer` (support:
  healing and wind), `nusmp:ice_make` (Maker magic: static and dynamic ice constructs). Each has 7 to 10 nodes in a branching tree (a root, two or three branches, a Secret Art at the end that needs two branches),
  some nodes passive. The Fire tree's required shape (the owner's example): Fire Dragon's Iron Fist -> Fire Dragon's Roar -> Secret Art (Crimson Lotus: Exploding Flame Blade), with more nodes around.
* **Abilities**: each active node's `skill` is a `TreeSkill` subclass registered in the route's register (`FireDragonSkills.SKILLS.register("ft_fire_dragons_roar", ...)`), so it appears in Tensura's skill menu and is cast with
  Tensura's own skill keys, exactly like every other spell of the mod. `TreeSkill` is the hook: `canInteractSkill` and `onRelease` ask `SkillTreeApi.canCast(player, route, node)` = route unlocked AND node level >= 1
  AND (config) the route's class is the active class. A skill learned by hand or kept after a refund still cannot be cast. The node's LEVEL scales the technique through `values` (the code reads
  `node.value("damage", level, default)` from the registry), so the SP invested changes damage, range, duration and cost.
* **Balance**: damage only through `AbilityKit.hit` (BalanceLaw); raw per hit 4..14 ordinary, <= 30 Secret Art; cooldown >= 3 s (a Secret Art >= 45 s); cost 1..4% of the maximum magicules (a Secret Art <= 8%); at most 12 targets;
  heals through `AbilityKit.heal` (<= 25% of max health per cast); crowd control only after `AbilityKit.control`; no block changes; effects only `VfxSpawn` shapes (238), sounds. Never vanilla particle spam. Never edit the
  Time Magic VFX files (CLAUDE.md rule 3).

## 7. System 6: isolation from the Black Clover Grimoire, and the Mega-Dungeon drops

* **Data isolation**: the tree lives in `nusmp:skill_tree` and nothing else; the Grimoire lives in `GrimoireComponents`, `GrimoireSlot` and `MultiverseProfile` (mastery pages). No field, key or id is shared, SP is only
  in `IPlayerSkillTree`, a Skill Book's component is read only by `SkillBookItem`.
* **Code isolation**: a pure source-scan unit test (`IsolationTest`) fails the build if any file under `skilltree/**` or `client/skilltree/**` imports the Grimoire or profile classes
  (`com.newuniverse.nusmp.grimoire.*`, `com.newuniverse.nusmp.blackclover.*`, `com.newuniverse.nusmp.book.GrimoireBook`'s data classes, `MultiverseProfile`, `MasteryPages`), and if any Grimoire class imports `skilltree`. The only shared
  APIs are neutral ones both use: `BalanceLaw`, `VfxSpawn`, the Tensura skill API (`SkillAPI`), and the neutral geometry helpers copied into `AbilityKit`.
* **UI isolation**: the screens share only the menu they are opened from. `SkillTreeScreen` is a different class with different widgets and different packets; the Four Kingdoms grimoire pages cannot receive a click meant for
  the tree, and a tree packet cannot name a grimoire page (its payload carries a route and a node id and nothing else). SP cannot be spent on a Grimoire anything: there is no code path.
* **Loot (stitems)**: a Global Loot Modifier `nusmp:skill_books` (type registered in `ItemsSystem`, entry in `global_loot_modifiers.json`, definition `data/nusmp/loot_modifiers/skill_books.json`) adds to a mob's drops
  when ALL hold: the level is the Mega-Dungeon dimension, the killer is a player, and the victim is (a) a monster on an ABYSSAL floor (any `Enemy` of a sector whose polarity is Abyssal; find the polarity of the
  position with the dungeon's own API: `labyrinth/mega/MegaGrid`, `MegaData`, the floor model of `docs/labyrinth/mega_v2.md`) at a low chance (config `abyssalBookChance`, default 0.4% for a route book, 4% for an SP
  Book), or (b) a DECENNIAL boss (the Guardian of a boss floor; the floor is a multiple of 10) at a high chance (config `bossBookChance` default 30% for a route book, 100% for 1 to 3 SP Books). Which route: random among the
  routes of the registry weighted by `weight` (a field the loader reads, default 1; later routes can be rarer); no duplicates within one drop. Drops go through the loot context only (no `setBlock`), per killer, honour the
  party credit (`SystemParty`/party of the killer gets nothing extra). On Holy (good) floors nothing drops: those floors are for the other loot.

## 8. The Black Market (`shop/**`, owned by `stshop`)

* **The witch**: an old witch NPC (`ArcCharacter.npc`, one more friend of the Hub in the Chronicle's posts; the dusty shop is her post in the back of the Promenade) with her own model (the art kit), lines (dialogue file: greeting, hint,
  `shop`, farewell). She appears once any player has beaten the first boss of the roster (a prerequisite), because the shop is "hidden". Sneak + right click opens the shop; a plain right click talks.
* **Gold**: a number in the player's tree (`extra("market")`, key `gold`, a long), shown in the shop. Sources: the witch BUYS dungeon goods for gold (Magic Stones, monster meat, gems, diamonds, nether stars, relic
  fragments: a data table `data/nusmp/black_market/prices.json`) and `/multiverse tree gold`. Nothing else makes gold in this version.
* **Stock** (`data/nusmp/black_market/stock.json`): every route book at a price rolled per restock in a band ("market risk"): high-tier books 2,000,000 to 3,000,000 gold (the *Fang of Smoldering Insanity* is the
  flagship at the top of the band); shelves of cheaper SP Books: Beginner Archery Primer (+2 SP), Elemental Primer (+3 SP), Magic Booster (+5 SP), Jack-in-the-Box (the gamble, 1 to 9 SP, a rare 20); stock is limited per
  restock (1 of each route book, 3 to 5 of each SP book); restock every `restockDays` in-game days; the roll uses the world seed and the day so it is the same for everyone. Purchases are validated on the server.
* A screen of the shop (own class, own packets: `OpenShopPayload`, `BuyPayload(entry)`, `SellPayload(item slot, count)`), themed like the tree. The shop is isolated from the Grimoire by the same rules.

## 9. Config, commands, quest hooks, self-tests

* Configs (plain `define`/`defineInRange` only, a `get` helper that falls back to the default: copy `labyrinth/dnd/DndConfig.java`): `nusmp-skilltree-server.toml` (core: `enabled`, `requireActiveClass`, `classSwitchSeconds`, `respec`,
  `duplicateBook`, `maxSp`, `purchaseRateLimit`), `nusmp-skilltree-loot-server.toml`, `nusmp-skilltree-shop-server.toml`.
* Commands (permission 2): `/multiverse tree show [player]`, `sp <player> add|set <n>`, `route <player> unlock|lock <route>`, `node <player> <route> <node> <level>`, `class <player> <class>`, `reset <player> [route]`, `reload`, `routes`
  (the registry with the validation report), `gold <player> add|set <n>` (shop).
* Quest hooks: `tree.route` (route id), `tree.node` (route, node, level), `tree.sp` (amount), `tree.class` (class), `tree.shop.buy` (entry).
* Self-tests (prefix `tree`): `tree.core.routes` (every shipped route loads and validates; every node skill exists in the ManasCore registry; every `TreeSkill` agrees with its node), `tree.core.rules` (purchase flow on a stand-in
  player), `tree.core.sync` (the payload codecs round trip), `tree.items.use` (unlock, duplicate, SP book), `tree.items.loot` (the modifier on a fake kill), `tree.shop.buy` (the ledger), `tree.ft.<route>` (each route's abilities
  cast once on a stand-in player). Unit tests for the pure parts (`RouteSchema`, `TreeRules`, `PlayerSkillTree` NBT, price tables, the stock roll, the loot weights) with `jt.sh`, mutation-checked once.

## 10. Art and assets

16x16 sprites (generated by a script under `tools/skilltree/`): the Skill Book (a book silhouette with a gem and a tintable cover layer: the cover is tinted by the route colour on the client, so one sprite serves every route), the SP Book
(several covers), the node icons use item ids. The witch has a model spec for the arc art kit. No 3D weapon models in this system (no weapons).

## 11. The WHY index (the owner's strict requirement; each class repeats its own in its Javadoc)

| Decision | Why |
|---|---|
| Data attachment `nusmp:skill_tree`, interface `IPlayerSkillTree` | NeoForge 21.1 stores per-player data in attachments (saved, copyable on death); capabilities expose behaviour. The interface keeps every caller off the raw maps. |
| Payloads (`CustomPacketPayload` + `StreamCodec`) instead of `SimpleChannel` | `SimpleChannel` is gone in NeoForge 21.x; typed records with codecs are the supported, versioned, bounded way and fail loudly on a bad packet. |
| Whole-tree S2C sync, on login / respawn / change | The tree is small (a few hundred bytes); one full snapshot is simpler and cannot drift; the client never merges. |
| Routes shipped to clients in a payload | A mod's own datapack JSON is not sent to clients; the screen needs the names, nodes and prices. |
| Reload listener + validation, not a datapack registry | Trees are small and need a client copy and indexes; a datapack registry adds a codec/freeze/sync layer for nothing. |
| One `skill_book` item with a component | Items cannot be registered by data; routes can. A new magic = a JSON file, no new item or Java class. |
| Route unlock server-side in `SkillTreeApi` | Every path (book, command, shop, loot) must announce and sync; one door makes that impossible to forget. |
| C2S purchase re-validated, rate limited | The client is never trusted; a modified client must not buy a node it cannot afford. |
| Skills are ManasCore `TreeSkill`s | The mod's spells are cast through Tensura's skill keys and menu; reusing that gives keybinds, cooldown display and mastery for free and keeps one casting path. |
| Cast gate in `canInteractSkill` + `onRelease` | The menu hides what cannot be used and the cast re-checks; a stale skill cannot fire. |
| `values` tables in JSON by node level | Balance is data; the SP a player invests visibly changes the technique. |
| Event-injected button, no keybind | The owner wants one menu; an event leaves the existing screens untouched and the injection removable. |
| Isolation test (source scan) | The owner's exclusion rule is enforced by the build, not by a comment. |
| Global Loot Modifier | It works on the rolled loot, so it behaves the same for every mob and in every dimension rule, without touching mob classes. |
| Gold ledger in `extra("market")` | The shop's number lives with the player's tree (one save, one copy on death) without a new attachment. |
