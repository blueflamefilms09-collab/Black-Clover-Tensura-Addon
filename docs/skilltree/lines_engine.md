# Unique Skill Lines: the engine (1.82.0)

The engine half of `docs/skilltree/lines.md`: what reads the generated lines, what runs them, what the player sees. Every part below says WHY it is the way it is. Nothing was run in the real game; section 9 says exactly what was proved and how.

## 1. New or replacement (CLAUDE.md rule 5)

Everything here is **NEW**, added next to the 1.62.0 engine. Nothing was removed or replaced. The records, the wire, the registry, the sync and the screen were *extended* (old call sites, old files and old routes mean exactly what they meant; old tests pass untouched, except the one data-file test that must now count 283 routes). The one deliberate semantic change: the server no longer sends a player every route; it sends the routes the player owns plus a card for every line (section 6).

## 2. Schema (part A)

| what | where | why |
|---|---|---|
| route `franchise`, `gui_background`, `book_texture`, `background_theme`; node `type`, `kind`, `params` | `SkillRoute`, `SkillNode` (extra record components) | the contract. The old 13-argument constructors stay, so every older call site compiles and means a classic route/node |
| a route with a `franchise` is **extended** | `RouteSchema` | one switch for three things a generated line needs and a classic route must not get: the wider grid (x 0..20, y 0..18; classic stays 0..12 / 0..9 so its tests and files are unchanged), forgiving texts (an over-long `desc`/`book_lore` is cut with a warning instead of refusing the line), and a strict catalogue |
| catalogue validation | `RouteSchema.checkEffect`, `line/LineCatalog` | kind must exist and match the node `type`; every enum value must be in its list; every `values` entry must be inside the kind's `[min,max]`. A line (extended route) with ANY node outside the catalogue is refused as a whole with a message per node; the others still load. A classic node with a bad kind is skipped like any bad node |
| the catalogue copy | `src/main/resources/nusmp_lines/catalog.json` | the engine reads a byte copy of `tools/lines/catalog.json` from the jar (the tool file is not shipped). `LineSchemaTest` fails if the two differ |
| `unique_skills/` alias folder | `RouteLoader` | same files, same ids, same schema as `skill_trees/`; `skill_trees` wins a clash and it is logged |
| registry caps vs payload caps | `RouteIndex.Caps`, `RouteWire` | the server holds all routes: `Caps.REGISTRY` = 1024 routes / 64 MB. `RouteWire.MAX_ROUTES` (64) and `MAX_TOTAL_BYTES` (900,000) remain what ONE sync payload holds; `Caps.CLASSIC` is the old pair and `RouteIndex.build(files, unreadable)` still uses it, so the old index tests are untouched |
| wire | `RouteWire` | the new fields are appended inside the route and node layouts; the reader decides the grid limit from the franchise it has just read, so a classic route is still refused at x 13 on the wire |
| malformed line | `RouteIndex.build` | schema never throws; a throwing parse is caught per file; a line is skipped with a report entry, never the others |

## 3. Effect runtime (part B), package `skilltree/line/`

One executor per catalogue kind, nothing per node. The files:

* `ActiveExecutors` (26 arts and finishers + the stance, one method each, registered by `reg("kind", ...)`), `PassiveExecutors` (25 kinds, each a bundle of hooks: dealt, post, taken, beat, kill, drops, xp), `LineAttrs` (`attr`, transient modifiers).
* `LineCast`: the server cast service. Order: engine on -> route/node exist -> **gate** (`LineGate.activeLevel`: line equipped, node bought, node is an art) -> cooldown (a game time in the player's own data) -> the executor's own check (a target) -> magicule price (`AbilityKit.pay`, a share of the max pool) -> run in a catch -> cooldown from the `cooldown` table, never below 3 s (a stance 1 s) -> `ArtState` packet so the bar can show the sweep. Damage is `AbilityKit.hit` (balance law), heals `AbilityKit.heal`, hard control only after `AbilityKit.control` and capped by `BalanceLaw.controlTicks`, <= 16 targets, no block is changed. Finisher >= 45 s is held by the generator and the data-file test (the engine floors only the 3 s of an art). Practice (casts) feeds the law's mastery term.
* **Why not a ManasCore skill per node:** the skill registry is frozen at startup; 5,000 nodes must not be 5,000 registered skills.
* `LineStances`: one stance at a time, upkeep-free; armour, speed and knockback resistance are transient modifiers with fixed ids (safe because only one stance exists), damage bonus and crit chance are read by the damage event, regeneration by the beat. Dropped on the first beat after the line is unequipped.
* `LinePassives`: THE place a passive may act: `active(player)` asks `LineGate.passiveLevel` for every switchable node of the equipped line. Executors never check the gate themselves, so none can forget it. Which nodes of a route are passives is cached per route record (weak map, not per-player state).
* `LineEvents`: `LivingIncomingDamageEvent` (dodge, resist, crit, mark, ward...), `LivingDamageEvent.Post` (lifesteal, shove, effect on hit), `LivingDeathEvent`, `LivingExperienceDropEvent`, `LivingDropsEvent`, `PlayerTickEvent.Post` beat (every 10 ticks, staggered by entity id). Every handler is wrapped in a catch. The only static is one boolean that stops a reflected blow (thorns, ward) from triggering another.
* `LineState`: all per-player state (cooldowns, rests, combo, stance, combat stamp, applied modifiers) in the player's persistent data under one compound `nusmp_line`. Values are game times and counters, so a stale entry is simply in the past.
* Delayed parts (whirlwind, barrage, meteor, leap) use the existing `FtClock` queue through `LineTimers`, which also checks the caster is still present.
* Visuals: only the mod's `VfxShape` through `AbilityKit.fx` and Light/Dark Rukh (`RukhFx`, alignment from the line's theme: void, blood, ruin, iron, storm are Dark).
* **Pinned to the catalogue:** `LineKindsTest` fails when a kind has no executor or an executor no kind, when an executor reads a number or enum that is not a parameter of ITS kind (a typo would silently read the base value forever), and when the effect/attribute/condition/category/damage-kind/shape words differ from the catalogue's.

## 4. Passive toggles (part C)

* `PassiveToggles` (pure) is the map `<route>#<node> -> boolean`; absent means ON ("default ON when bought" also covers nodes bought before the feature). Loading is tolerant. `PlayerSkillTree` stores it as NBT `toggles` (only when non-empty), `clearRoute` clears the route's switches, `copyFrom` copies it. `IPlayerSkillTree` got three methods.
* `SkillTreeApi.isPassiveEnabled` (raw switch), `setPassiveEnabled` (server: owned, node exists, has a switch, bought), default ON written on the first purchase.
* Packet `TogglePassivePayload` (C2S, rate limited 10/s); a refusal re-sends the tree so the client's optimistic flip is undone.
* **Passives panel** (`PassivesScreen`, `layout/PassiveRows`): see the class Javadoc. Rows are built once per change, fixed heights, binary search for the visible window, precomputed strings, so a frame allocates nothing for the list; searchable; equipped line first; others dimmed; hover tooltip with description, kind, numbers. Reachable from a header button of the Skill Tree screen, a second button injected next to "Skill Tree" in the Codex / menus (`MenuInjection`), and key `;`.

## 5. Arts bar

`ArtsBarModel` (pure) + `ArtsBarClient` (keys `[` previous, `]` next, `\` use; rebindable) + `CastArtPayload`. The bar lists exactly what the server's gate accepts, remembers the selection by art key, and draws cooldown sweeps from `ArtState`. The client decides nothing.

## 6. Visibility lock and admin (part D)

* `LineSyncPlan` (pure): owned routes, packets (<= 64 routes and 800 KB each), an owned-set signature. `LineSync`: first packet is the old `RoutesPayload` (replace), more packets `More` (add); the owned routes are re-sent only when the owned set changes (hook: `TreeNet.sendTree` -> `LineSync.afterTree`).
* **Contract deviation, deliberate:** every client also gets a `Cards` payload (identity and look of every line: name, book, colour, icon, class, cover, theme, node count; about 190 bytes a line, 56 KB for 300 lines, never a node). Skill Books (name, tint), the Black Market and the creative tab ask the route registry by id/book for lines the player does not own, so the client turns a card into a node-less **stub** route. A stub is never shown as a tree. If you want even the NAMES hidden, drop `sendCards` and give the books/shop their own list.
* The screen filters by ownership itself (`LineVisibility`), because in single player the client sees the server's whole registry. Lists are cached by (registry list identity, owned signature, inspect flag); `RouteList` rebuilds only when the grouped list or width changes; a tree layout is built only for the route that is opened.
* **Admin inspect:** a toggle "ADMIN INSPECT MODE" for `hasPermissions(2)` or creative (`InspectMode`). It lists every line (from the cards or, in single player, the registry) and, when a line is opened, `InspectPayload(route)` asks the server (which re-checks the permission) for the tree, delivered as `More`. A not-owned line is read-only: nodes can be selected and read, nothing can be bought, the server owns purchases. Op + the existing `/multiverse tree` admin command still edit.
* Equip: `SetClassPayload` was already server-validated (`switchClass` needs the route owned, the cooldown, etc.); the line gate is strict: no creative or `requireActiveClass=false` bypass for lines, otherwise a creative player would run every owned line's passives at once.

## 7. Backgrounds (part E)

`TreeBackground`: the picture of `gui_background`, else the default `nusmp:textures/gui/skill_trees/<route id>.png` (the 8 hand-built routes declare none). The PNG size is read from its header once per route; the picture covers the canvas, shifts a little with the pan, is darkened with a veil and a bottom fade; a missing or non-PNG file draws nothing (the old canvas). One placeholder, `fire_dragon_slayer.png`, was made by `tools/lines/placeholder_bg.py`; the background job's file will overwrite it.

## 8. Language (part G)

`LineText` is the one list of strings, read with `translatableWithFallback`; `tools/lines/lang_engine.json` (generated by `tools/lines/gen_lang_engine.py`) is what the lead merges into `en_us.json`; `LineTextTest` keeps them equal and checks `en_us.json` was not touched. Key-binding names (`key.nusmp.line_*`) cannot use a fallback and are in the file too.

## 9. What was proved, and how

* Run with the JUnit shim (pure code): `LineSchemaTest`, `ToggleAndGateTest`, `LineKindsTest`, `LineScaleTest`, `ArtsBarModelTest`, `LineTextTest`, `BackgroundAssetTest`, plus the older data/rules/layout/isolation tests (213 pass). Mutation-checked: removing an executor, mistyping a param, dropping the equipped check, flipping the default switch, and dropping the ownership filter each make a test fail.
* Scale (`LineScaleTest`, 300 generated lines in the generator's shape): read + validated in 150 to 290 ms; whole set 2.65 MB in 5 packets; an owner of 3 lines 26 KB in one packet; the cards of 300 lines 56 KB; the Passives model of 3,900 rows builds in about 10 ms and finds a row in O(log n); the library never looks at the nodes of a line it does not list.
* Type-checked only against API stubs (not run): every executor, the events, the cast service, the payloads, the self-tests, `PlayerSkillTree`, the screens and the overlay. The two in-game self-tests `tree.lines.kinds` and `tree.lines.engine` are written but have never run.
* Not verified anywhere: the real rendering of the Arts bar, Passives panel, inspect mode and background; the NeoForge event signatures and `GuiGraphics.blit` overload were only checked against stubs; balance of the 51 executors in play.
