# The Skill Tree core: the route registry, the player's tree rules, the network, the commands (1.62.0)

This is the builder document of `stcore`, the core of the engine in `docs/skilltree/design.md`. It covers systems 1 and 2 of the owner's list (the data-driven route files with their reload listener,
and the per-player tree with its client sync) plus the rules, the API, the events, the config, the commands, the self-tests and the isolation test that every other system calls. Every class,
JSON structure and packet has a WHY here (the owner made that a strict requirement); each class repeats its own in the first paragraph of its Javadoc. **Nothing in this document was run in the
game**; section 12 says exactly what was proved, how, and what was not.

## 1. New, replaced, kept

* **NEW**: everything listed in section 2, except the four stubs below.
* **REPLACED** (the stubs of the contract; every public signature is kept): `skilltree/TreeCore`, `skilltree/SkillTreeApi`, `skilltree/SkillTreeCommand`, `skilltree/data/RouteRegistry`.
* **KEPT untouched**: the records `SkillRoute`, `SkillNode`; `state/**` (the interface, the implementation, the attachment); `ability/TreeSkill`, `ability/AbilityKit`; `SkillTreeSystem`; the four
  `ft/*Skills` registers and every stub of another builder; `NUSMP`, `MultiverseCommands`, `NUCreativeTab`, `en_us.json` and all the other shared files. Nothing outside `skilltree/**`,
  `client/skilltree/TreeCoreClient`, `docs/skilltree/core.md` and the tests was edited.

## 2. The files, in groups, with the WHY of each group

| group | files |
|---|---|
| the route file and its reader | `data/RouteSchema` (pure), `data/RouteWire` (pure), `data/RouteIndex` (pure), `data/RouteLoader`, `data/RouteRegistry`, `data/RouteCodecs` |
| the rules | `rules/TreeState`, `rules/SimpleTreeState`, `rules/TreeRules`, `rules/RateLimiter`, `rules/SyncQueue` (all pure), `PlayerTreeState` |
| the door | `SkillTreeApi`, `RouteUnlockListener`, `SkillGrants`, `TreeConfig` |
| the network | `net/TreeSyncPayload`, `net/RoutesPayload`, `net/UnlockNodePayload`, `net/SetClassPayload`, `net/TreeNet` |
| the wiring | `TreeCore`, `TreeEvents`, `client/skilltree/TreeCoreClient` |
| the commands and the tests | `SkillTreeCommand`, `TreeSelfTests`; `src/test/.../skilltree/**` |

### 2.1 The route file `data/<namespace>/skill_trees/<name>.json`

**Why JSON and a reload listener.** The owner wants new magics to be data: a server owner or the mod's author retunes or adds a tree with a datapack and `/reload`, with no rebuild. **Why a
custom listener and not a datapack registry**: the trees are small, validated in one place, indexed for lookups by book and by skill, and need a client copy anyway; a datapack registry would
add a codec, a dynamic-registry sync and freeze rules for no gain. **Why a hand-written schema reader and not a Codec**: a Codec gives one machine-style error for the whole file and no partial
result, and the owner's rule is the opposite: a bad route is skipped while the other routes load, and a bad node is skipped together with the nodes that need it while the rest of the tree
still works, with every problem named in plain words.

The route id is `<namespace>:<file name>` (the id the game gives the file), so `data/nusmp/skill_trees/fire_dragon_slayer.json` is `nusmp:fire_dragon_slayer`, a datapack can replace any file,
and a file in a sub-folder cannot be a route.

| field | meaning | default | limit | why |
|---|---|---|---|---|
| `magic` | family of magic, groups the routes in the screen | `"other"` | lower-case snake, 32 | the screen groups by family; a typo must not hide a route, so it has a default |
| `name` | display name | **required** | 1..64 chars, not blank | a route without a name cannot be shown |
| `desc` | what the route is about | `""` | 400 | tooltip text |
| `book` | the id of the Skill Book that opens the route | **required** | lower-case `namespace:path`, 64 | the book is ONE registered item whose component says which book it is; this is the join between the item and the route |
| `book_name` | the book's display name | `"<name> Skill Book"` | 64 | the owner's flagship is "Fang of Smoldering Insanity"; the item reads this, so a new magic needs no Java |
| `book_lore` | one line under the book's name | `""` | 200 | flavour |
| `color` | accent of the route and the tint of its book | **required** | `#RRGGBB` | one cover sprite is tinted per route; a missing colour would tint nothing, so it is required |
| `icon` | item id used as the route's icon | `minecraft:book` | resource location | the screen's list entry |
| `class_id` | the class a player is while this route is active | **required** | lower-case snake, 40, unique among routes | the active class is chosen by this id and checked by the cast gate |
| `duplicate_sp` | SP a second copy of the book is worth | `3` | 0..100 | the owner's "duplicate book" rule |
| `loot_weight` | weight among routes in a dungeon drop | `1` | 0..1000 (0 never drops) | later routes can be rarer |
| `nodes` | the nodes, parents before children | **required** | 1..40 | the tree |

Node fields:

| field | meaning | default | limit | why |
|---|---|---|---|---|
| `id` | unique within the route | **required** | lower-case snake, 40 | a node is stored as `<route>#<node>`; `#` and `:` must be impossible in it |
| `name`, `desc` | texts | title-cased id, `""` | 64, 300 | the screen's tooltip |
| `parents` | nodes that must be at a level first (all of them) | `[]` | at most 6 | an entry is `"node_id"` (level 1) or `{ "node": id, "level": n }` (n in 1..10, not above the parent's `max_level`) |
| `cost` | SP price of one level | `1` | 1..50 | the owner's price |
| `costs` | price per level (index 0 is level 1; the last repeats) | `[]` | no longer than `max_level`, each 1..50 | dearer later levels without repeating a number |
| `max_level` | how many times it can be bought | `1` | 1..10 | the owner's range |
| `skill` | the ManasCore skill granted at level 1 | `""` | lower-case `namespace:path`, no other node grants it | the ability hook; existence in the registry is checked by the self-test, not by the pure schema |
| `passives` | ids of passive effects switched on while level >= 1 | `[]` | at most 8, lower-case snake | implemented by the route's content code |
| `values` | named tables of numbers by level | `{}` | at most 16 tables, 1..10 finite numbers each, magnitude <= 1e9; a lone number is a one-entry table | balance is data: the SP invested changes the technique |
| `x`, `y` | cell on the canvas | `0`, `0` | `x` 0..12, `y` 0..9 | the screen lays nodes out on this grid |
| `icon` | item id for the node | `""` (the route's icon) | resource location | the screen |

Unknown fields are not errors but **warnings** with a "did you mean" for a camel-case slip (`maxLevel` for `max_level`), because the records use camelCase and an author will write it; two nodes
in the same cell and a `cost` that disagrees with `costs[0]` are warnings too. Everything else in the table is an error of its scope:

* **A node error** skips the node AND every node that needs it (its children, found by the parent rule), and the rest of the tree loads. A repeated node id skips the later node only.
* **A route error** skips the route: not an object, a bad id, a missing or invalid `name`/`book`/`class_id`/`color`, no `nodes`, more than 40 nodes, or no root node left after the skips.
* **A parent must come EARLIER in the file** (and still be loaded). This one rule also makes a cycle impossible (a node that names itself, or two nodes that name each other, are refused
  with the sentence "comes later in the file ... this also rules out cycles"), so no separate graph search is needed.
* **Rules that need all the files** (in `RouteIndex`): two routes may not share a `book`, a `class_id` or a granted `skill`; at most 64 routes; one route at most 65,536 bytes on the wire; all routes
  together at most 900,000 bytes (a clientbound payload carries 1 MiB). The LATER route in id order loses, deterministically, with a sentence naming the route it clashed with.

### 2.2 The reader, the layout, the snapshot (all pure except the loader)

* **`RouteSchema`** - Why: described above. Pure (Gson and the records), so 26 tests prove every rule and every scope of damage; never throws (an unforeseen shape becomes a problem line).
* **`RouteWire`** - Why: the sync packet's byte layout is a contract between a server and a client that may be different builds, so it is written ONCE as a sequence of three primitives
  (variable-length integer, bounded string, double) on two tiny interfaces. The game binds them to a `FriendlyByteBuf` (`RouteCodecs`), the tests to byte arrays, and a counting sink gives the exact
  size, which is how the registry refuses a too-big route at LOAD time with a readable message instead of kicking every joining player. The reader fails loudly on any count, number or text
  outside the schema's ranges (the screen relies on those ranges). No version byte inside: the payload protocol version "1" already refuses a peer with another layout.
* **`RouteIndex`** - Why: an immutable snapshot built once gives lock-free reads from the server thread, the client thread and the reload thread, and a reload is one reference swap (a reader never
  sees half of a reload). It is also where the all-files rules live, and it carries the per-file report that `/multiverse tree routes` prints and `tree.core.routes` asserts on.
* **`RouteLoader`** - Why: a `SimpleJsonResourceReloadListener` on `skill_trees` already lists every datapack in priority order and parses off the main thread; the only thing added is the list of
  files the game dropped because they were not valid JSON (the files found minus the files parsed), so they appear in the report. `apply` builds the snapshot on the main thread and publishes it
  with one write; if anything unexpected is thrown the previous routes stay in force. `reloadNow` is the same scan for `/multiverse tree reload`.
* **`RouteRegistry`** - Why: a static facade (the contract's signatures) over two snapshots, SERVER and CLIENT. The lookups read the server snapshot while a server runs in this JVM (a dedicated
  server, or the integrated server of a single-player world, where the screen and the rules then share one set and cannot disagree) and the client copy otherwise. Cleared on `ServerStoppedEvent`
  and, for the client copy, on leaving a world.
* **`RouteCodecs`** - Why: `StreamCodec.composite` stops at six fields and a route has 40 nodes of lists and tables, so the codec is `StreamCodec.of` over `RouteWire`; it never writes more routes than
  the reader accepts.

### 2.3 The rules (pure)

* **`TreeRules` + `TreeState`** - Why: the rules (buy, price, parents, maximum, respec refund, class switch, cast gate) must give the same answer to the purchase handler, the commands, the
  self-tests and the client's "can I cast this?"; a wrong rule lets a modified client buy what it cannot afford. They are static functions over the pure records and a six-method state interface,
  with no Minecraft type, and a refused call changes nothing (tested by comparing the whole state before and after).
  * Order of checks: route unlocked, node exists, not maxed, every parent at its level, SP >= price of the NEXT level (`costOf(level + 1)`).
  * A respec refunds `totalCost(level)` per node but **never more than `spSpent`**, so a datapack that made nodes dearer, or an admin who set levels by hand, cannot mint SP out of a respec; SP
    in the system (`sp + spSpent`) is conserved or lower after any respec.
  * `addSp`: never below 0, a gain never above `maxSp`, and a gain never LOWERS a total that is already above a lowered cap.
  * Class: the first class (none active) is free; creative players and commands skip the cooldown; the same class is `SAME`; a route the player does not own is `NOT_UNLOCKED` even with a bypass;
    a stored change time later than the game clock (the world time was set back) does not lock the player.
  * Cast gate: route owned AND node level >= 1 AND (when `requireActiveClass`) the route's class is active; creative passes the class check only; a route the registry does not know cannot have
    its class checked, so it is refused whenever the class matters.
* **`SimpleTreeState`** - Why: an in-memory state so the rules run on a player-shaped object in unit tests and in the self-test, with the same `<route>#<node>` keys as the real tree.
* **`PlayerTreeState`** - Why: the one-to-one adapter from the real `IPlayerSkillTree` to `TreeState` (no logic, no copy: a purchase made through the rules changes the saved data). Public so the screen
  or the shop can ask a rule about a real player without rewriting it.
* **`RateLimiter`** - Why: the owner asked for "10 purchases per second". A sliding window over the SERVER'S TICK COUNT (not the wall clock: same on a lagging server, deterministic in a test),
  refused attempts are not recorded (a flood does not extend its own lock-out), a clock that goes backwards drops the stale stamps.
* **`SyncQueue`** - Why: "one sync per tick per player": a change only marks the player, one flush at the end of the tick sends each marked player once in first-marked order; draining empties the
  queue BEFORE sending, so a mark made during the send lands in the next flush instead of being lost or looping.

### 2.4 The door

* **`SkillTreeApi`** - Why: the one door every system uses. The arithmetic is in `TreeRules`; this class adds the attachment, the registry, the skills, the sync queue, the quest hooks and the unlock
  listeners. Every write is server-thread only, re-checks what it is given, changes nothing when it refuses, never throws (a failure is logged and answered as a refusal). See section 5.
* **`RouteUnlockListener`** - Why: effects belong to the items system, rules to the core; unlocking calls the listeners so the circle, the flash and the banner are the items builder's to draw.
* **`SkillGrants`** - Why: the only code that touches a player's skill storage for tree skills. LEARNING needs the registry; FORGETTING is decided from the player's saved tree alone, never from the
  registry, because a skill instance carries mastery: a datapack mistake that made a route vanish for one reload must not strip every player's techniques. It only ever touches a `TreeSkill`:
  it refuses to TEACH a node's skill that is not one (a route file must not be able to hand out a Tensura unique skill or a Grimoire page, which the cast gate would not guard) and never FORGETS anything a
  player earned another way; a skill id that is not registered, or is not a `TreeSkill`, is logged once per session, not once per login.
* **`TreeConfig`** - Why: see section 7.

### 2.5 The network (section 6), the wiring (section 8), the commands (section 9), the tests (section 11).

## 3. Decisions and alternatives rejected

| decision | rejected alternative | why |
|---|---|---|
| a `SimpleJsonResourceReloadListener` + an immutable `RouteIndex` swapped by one volatile write | a datapack registry with a codec | the trees are small, need indexes and a client copy anyway; a registry adds a codec, a dynamic sync and freeze rules for nothing |
| a hand-written schema reader returning problems | a Codec / `DataResult` | one error for the whole file and no partial result; the owner's rule is partial loading with every problem named |
| the later route (by id) loses a clash | the first one listed by the game | the game's listing order is not stable; id order is |
| a parent must come earlier in the file | a graph search for cycles | the ordering rule makes cycles impossible by construction and is easier to explain to an author |
| one snapshot per side, the server's read while a server runs in the JVM | one shared static set | a client on a remote server must not see the integrated server's old set, and the integrated server's client must not lag a reload |
| forget tree skills from the saved tree, learn from the registry | forget whatever the registry does not back | a registry hiccup would destroy mastery |
| full-tree sync, coalesced per tick | delta packets | the tree is a few hundred bytes; a full snapshot cannot drift and the client never merges |
| tick-based rate limit | wall clock | deterministic, same on a lagging server |
| `optional()` payloads, protocol "1" | required channel | like every other payload of this mod; a peer without the channel simply does not get them |
| handlers touch only common classes | a `ClientProxy` class | nothing in the handlers needs a client class (they store into the registry and the attachment), so no proxy is needed; the single client-only listener (forget the routes on leaving a world) is `TreeCoreClient`, started only when the dist is a client |
| the config switch for a duplicate book is a boolean | a list-validated "SP"/"REFUSE" | a list-validated value throws while the server is still loading |
| `show` (own tree) and `respec` (own tree, config-gated) are open to players | all of `/multiverse tree` at permission 2 | reading your own tree and the owner's "respec, default off for players" need a player-facing path |
| no version byte in the wire layout | a layout version | the payload protocol version already refuses another layout; a second number has two places to forget |

## 4. The data flow in one paragraph

Server start or `/reload`: `AddReloadListenerEvent` adds a `RouteLoader`; the game parses `data/*/skill_trees/*.json`; `RouteLoader.apply` builds a `RouteIndex` (validation, cross-file rules,
caps) and `RouteRegistry.publish` swaps it in; `OnDatapackSyncEvent` sends the routes to the joining player (or everyone after a reload) and, after a reload, checks everyone's skills. A client
stores the routes it receives in its own copy. A player joins: `PlayerLoggedInEvent` heals a missing class, reconciles the skills and queues a sync; at the end of the tick the whole tree goes to
the client, which overwrites its copy in place. The screen asks to buy a node: `UnlockNodePayload(route, node)` -> rate limit -> `SkillTreeApi.upgradeNode` -> `TreeRules.buy` on the player's own
tree -> the node's skill is taught at level 1 -> hooks -> a queued sync.

## 5. `SkillTreeApi`

Reads (both sides, unchanged from the contract): `of`, `hasRoute`, `nodeLevel`, `isNodeActive`, `sp`. Config reads: `isEnabled`, `duplicateBookGivesSp`, `classSwitchSeconds`, `maxSp`,
`classCooldownTicks(player)` (works on the client too).

| method | rules | hook |
|---|---|---|
| `canCast(player, route, node)` | engine on, `TreeRules.canCast` with the config `requireActiveClass` and creative | - |
| `addSp(player, amount, why)` | ignored when the engine is off or the amount is 0; `TreeRules.addSp` with `maxSp`; queues a sync | `tree.sp` (the gain) |
| `unlockRoute(player, route, announce)` | engine on (else `DISABLED`), route in the registry (else `UNKNOWN_ROUTE`), not owned (else `ALREADY`); adds it, sets the active class when none, teaches the skills of nodes already bought, queues a sync, tells every `RouteUnlockListener` (with `announce`) | `tree.route` |
| `upgradeNode(player, route, node)` | `TreeRules.buy`: `NO_ROUTE`, `UNKNOWN_NODE`, `MAXED`, `PARENTS`, `NO_SP`; at level 1 teaches the node's skill (with the "Technique learned" line) | `tree.node`, `tree.node.level` |
| `setActiveClass(player, classId)` / `switchClass(player, classId, force)` | `TreeRules.classSwitch` with the config cooldown; creative and `force` skip it; `force` does not start one | `tree.class` |
| `resync(player)` | marks the player; the end-of-tick flush sends one packet | - |
| `reconcileSkills(player)` | `SkillGrants.reconcile` | - |
| `respec(player, routeOrNull)` | `TreeRules.respec` per route, the skills of cleared nodes are forgotten | - |
| `lockRoute(player, route)` | respec of that route, then removes it; the active class moves to a route that is left | - |
| `setNodeLevel(player, route, node, level)` | an administrator's free set: clamped to the maximum, parents NOT checked, SP untouched | - |
| `addUnlockListener(l)` | register once at start | - |

## 6. The network

Registered in `RegisterPayloadHandlersEvent` on the mod bus: `event.registrar("1").optional()` (protocol "1").

| id | direction | record | fields on the wire | why |
|---|---|---|---|---|
| `nusmp:tree_sync` | server -> client | `TreeSyncPayload(CompoundTag tree)` | the whole tree as `toTag()` | NeoForge 21.1 does not sync attachments; one full snapshot cannot drift and the saved form and the wire form cannot differ |
| `nusmp:tree_routes` | server -> client | `RoutesPayload(List<SkillRoute> routes)` | count, then each route (`RouteWire`) | the game does not send a mod's datapack JSON; the screen needs names, nodes, prices; one payload so a client never holds half a reload |
| `nusmp:tree_unlock_node` | client -> server | `UnlockNodePayload(String route, String node)` | two bounded strings (80, 40) | a purchase carries ids and nothing else; the server decides |
| `nusmp:tree_set_class` | client -> server | `SetClassPayload(String classId)` | one bounded string (40) | the same for the class choice |

* **Trust.** The client never says a price, a level or a point count. The handler requires a live `ServerPlayer`, applies the rate limit, then calls `SkillTreeApi`, which re-validates
  everything. A refusal sends one red action-bar line (`tree.msg.refused.*`, `tree.msg.class.*`) because it can be an honest race of two clicks. Handlers run inside `enqueueWork` and never
  throw (NeoForge disconnects a player whose handler throws, which is far too harsh for a refused purchase).
* **When the tree is sent.** Login, respawn, dimension change, `PlayerEvent.Clone` and after every change; all of it goes through `resync`, which marks the player, and `TreeNet.onServerTick`
  (`ServerTickEvent.Post`) sends each marked player once. `FakePlayer`s are never queued. A player who logs out is forgotten.
* **When the routes are sent.** `OnDatapackSyncEvent` (the joining player, or everyone after a reload) and `/multiverse tree reload`.
* **Client side.** `TreeNet.receiveTree` replaces the local player's tree IN PLACE (`PlayerSkillTree.copyFrom`), so a screen holding the object sees the new state; `setClient` replaces the whole
  client copy of the routes. Both use only common classes.

## 7. Config `nusmp-skilltree-server.toml`, section `[skilltree]`

Plain `define` / `defineInRange` only, read through `TreeConfig.get(...)` which answers the default while the spec is not loaded (copied from `labyrinth/dnd/DndConfig`). It is a SERVER config so
NeoForge copies it to a connecting client and the screen reads the numbers it will be held to.

| key | default | range | meaning |
|---|---|---|---|
| `enabled` | `true` | | master switch: nothing can be unlocked, bought or cast; SP gains are ignored; saved trees are kept |
| `requireActiveClass` | `true` | | only the active class's techniques can be cast |
| `classSwitchSeconds` | `30` | 0..3600 | wait between two class changes |
| `respec` | `false` | | players may use `/multiverse tree respec` |
| `duplicateBookGivesSp` | `true` | | a book of a route the player has: SP (`duplicate_sp`) or refused |
| `maxSp` | `9999` | 1..1,000,000 | SP cap for gains |
| `purchaseRateLimit` | `10` | 1..100 | packets per player per second |

## 8. Events (`TreeEvents`, `TreeCore`)

| event | what | why |
|---|---|---|
| `AddReloadListenerEvent` | add a `RouteLoader` | server start and every `/reload` |
| `OnDatapackSyncEvent` | send the routes; after a reload reconcile everyone's skills | join and reload |
| `PlayerLoggedInEvent`, `PlayerRespawnEvent` | `SkillTreeApi.onJoin`: heal a missing class, reconcile skills, queue a sync | a crash, a datapack change or a death never leaves a skill the tree does not back |
| `PlayerChangedDimensionEvent`, `PlayerEvent.Clone` | queue a sync | the client's player object can be replaced |
| `PlayerLoggedOutEvent` | forget the player's rate-limit history and queued sync | static state must not grow |
| `ServerStoppedEvent` | clear the server routes, the limiter, the queue, the skill-warning memory | nothing outlives the server |
| `ServerTickEvent.Post` | the sync flush | one packet per player per tick |
| `ClientPlayerNetworkEvent.LoggingOut` (client only, `TreeCoreClient`) | forget the routes of that server | the next server must not show the old one's trees |

Death keeps everything because the attachment is `copyOnDeath` (lead-owned).

## 9. Commands `/multiverse tree ...` (permission 2 unless stated)

`show [player]` (no argument: anyone, own tree), `sp <player> add|set <n>`, `route <player> unlock|lock <route>`, `node <player> <route> <node> <level>`, `class <player> <class>`,
`reset <player> [route]`, `respec [route]` (anyone, only when `skilltree.respec` is true; an operator can always), `reload`, `routes`. The `gold <player> add|set <n>` node belongs to the shop
builder: the lead adds `.then(com.newuniverse.nusmp.skilltree.shop.ShopCommands.node())` to `SkillTreeCommand.node()` when `ShopCommands` exists. Route ids use the game's resource-location
argument (a plain word cannot contain the colon); node and class ids are suggested from the registry. `routes` prints, per file: loaded or SKIPPED, the route's name, node count, book, class, full
price, then each problem (up to 8) and warning (up to 4).

## 10. Quest hooks (`NusmpEvents.fire(player, key, arg)`; the lead wires the advancements)

| key | argument | when |
|---|---|---|
| `tree.route` | the route id | a route is unlocked |
| `tree.node` | `<route>#<node>` | once per level bought (the counter of `tree.node|<route>#<node>` is the levels bought) |
| `tree.node.level` | `<route>#<node>@<level>` | the same purchase, for "reached level n" |
| `tree.sp` | the SP gained | a real gain through `addSp` |
| `tree.class` | the class id | the class changed |

(`tree.shop.buy` belongs to the shop builder.) `NusmpEvents` ignores `FakePlayer`s, so the self-tests fire nothing.

## 11. Tests

**Unit tests** (`src/test/java/com/newuniverse/nusmp/skilltree/**`, run locally with `jt.sh`, run for real by CI): `RouteSchemaTest` (26: the owner's example loads clean, defaults, every
refusal from a good base, scope of damage, boundaries, warnings), `RouteIndexTest` (12), `RouteWireTest` (9), `RouteRegistryTest` (5: the server set beats the client copy, clearing each side, the helpers), `SkillTreeFilesTest` (3: runs the real schema over every shipped route file, and
passes on an empty folder), `TreeRulesTest` (17), `RateLimiterTest` (8), `SyncQueueTest` (4), `IsolationTest` (6, section below). Mutation checks (one change to the main code, the suite must go red,
then the file restored byte-for-byte): `RouteSchema` x3, `TreeRules` x4, `IsolationTest` x3.

**Isolation** (`IsolationTest`, design.md section 7): fails when any file under `skilltree/**` or `client/skilltree/**` names `com.newuniverse.nusmp.grimoire.`, `...blackclover.`,
`...client.grimoire.`, `MultiverseProfile`, `MasteryPages`, `GrimoireBook` or `GrimoireSummon`, or when any file under `grimoire/**`, `blackclover/**`, `book/**` or `client/grimoire/**` names
`...skilltree.` / `...client.skilltree.`. It scans the CODE with comments removed (text blocks and strings kept), for the full name ANYWHERE in the file, not only in imports, because this repository
often writes a class by its full name inline. Negative controls feed the same scanner function strings: an import, an inline name, a wildcard, a static import, a name in a string
(`Class.forName`), and the things it must ignore (comments, neighbouring packages, a package that merely starts with the same letters).

**Self-tests** (in the game, `/multiverse mega selftest`, prefix `tree`): `tree.core.routes` (every installed route file loaded clean; books and classes unique; for every node with a `skill` the id is in
the ManasCore registry, is a `TreeSkill`, and agrees with the node; every registered tree skill in the four Fairy Tail registers belongs to a loaded node), `tree.core.rules` (a stand-in player and two
synthetic routes put in the registry for the length of the test: unlock, ALREADY, buy with and without SP, parents, maximum, per-level prices, SP accounting, the cast gate with and without the active
class, the class switch with its cooldown, forced switch, respec refund and conservation, lock and the class that moves; then, with a real route that grants a skill, buying its root teaches the skill
and a respec takes it back), `tree.core.sync` (the routes, `RoutesPayload`, `TreeSyncPayload`, both requests through a real `FriendlyByteBuf`, the exact byte count, a truncated packet and an
oversized text refused, the tree through NBT, an independent copy, negative numbers clamped). **All three pass on an empty registry and say so**; a route file that is installed but broken, or a
tree skill with no route to hold it, fails.

## 12. What was verified, and what was not

Done here: the pure classes compile and their 90 tests (93 with the contract's own `SkillRouteTest`) pass under the local runner (`jt.sh`, the JUnit shim), mutation-checked as listed; the whole tree
(main and test sources) was type-checked once with `tc_full.sh` against the API stubs and real JUnit 5.10.2: the only errors in my 31 files are the 8 stub artifacts below; a deliberately wrong call
was flagged by the same checker (negative control).

**NOT done, and cannot be from here:**
* nothing was started in the game; the three self-tests, the reload listener, the payloads on a real connection, the commands and the events have never run;
* the stubs make `FriendlyByteBuf` abstract, so `new FriendlyByteBuf(Unpooled.buffer())` in `TreeSelfTests` is flagged by the checker (8 lines); in the real game it is a concrete class with that
  constructor (an artifact of the stubs, not a bug);
* the stubs have no bodies, so how `Skills.learnSkill(ManasSkill)` (the quiet overload used at login) behaves, whether it prints a message by itself, and whether `forgetSkill` can be vetoed by an
  event, is read from the signatures and from the repository's own use, not from running it;
* whether a `FakePlayer` has a ManasCore skill storage is assumed (the real-skill step of `tree.core.rules` is wrapped so that an exception there is reported as "skipped", not as a failure);
* the default thread a NeoForge payload handler runs on is not assumed: every handler goes through `enqueueWork`, as the rest of this mod does;
* the 1 MiB limit of a clientbound custom payload is from memory of the vanilla constant; the registry cap (900,000 bytes) leaves a margin;
* `ResourceLocationArgument` is a vanilla argument type and is not used elsewhere in this repository (its use in the commands is from the stubs).

## 13. For the other builders and the lead

* Classes to call: `SkillTreeApi` (all of section 5), `RouteRegistry` (`all`, `byId`, `byBook`, `byClass`, `bySkill`, `report`, `snapshot`, and for the abilities `node(route, node)` and
  `value(route, node, key, level, fallback)`, which answer null / the fallback instead of throwing when a route is missing), `TreeRules` and `PlayerTreeState` (to ask a rule about a
  real player: `TreeRules.status(route, node, new PlayerTreeState(SkillTreeApi.of(player)))` gives the screen its node state; `TreeRules.priceOfNext`, `TreeRules.canBuy`), `RouteUnlockListener`.
* Payload classes the screen sends: `com.newuniverse.nusmp.skilltree.net.UnlockNodePayload(String route, String node)` and `SetClassPayload(String classId)`, with
  `PacketDistributor.sendToServer(...)`.
* `SkillTreeApi.isEnabled()` should be checked by anything that consumes an item on the engine's behalf (an SP Book) before consuming.
* Lang keys used by the core: `$S/hero/frag/stcore.lang.json` (`tree.msg.*`).
