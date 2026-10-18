# Unique Skill Lines (1.82.0): 275+ data-driven lines on the Skill Tree engine

The owner's new brief (3D skill books, 275+ lines of 15 to 30 nodes, Tensura-style passive toggles, thematic tree backgrounds, fog of war with an admin override, one active line) is an EXTENSION of
the 1.62.0 Skill Book & Skill Tree engine (`docs/skilltree/design.md`, `core.md`, `items.md`, `gui.md`), not a second engine. What the engine already has and keeps: the data-driven route files under
`data/<ns>/skill_trees/`, the reload listener, SP, books that unlock a route, the single-active-route rule, the Black Clover menu tab, the Black Market, the dungeon loot modifier. What this job adds:

| Part | What |
|---|---|
| A. Schema | route: `franchise`, `gui_background`, `book_texture`, `background_theme`; node: `type`, `kind`, `params` (string map). Old routes (no such fields) keep loading and working. |
| B. Effect runtime | ONE executor per effect KIND (`tools/lines/catalog.py`): 26 active kinds, 25 passive kinds, `stance`, `attr`. A node says "kind + numbers", so 275 lines need no per-node Java. |
| C. Passive toggles | every `passive_toggle` and `attribute_modifier` node, once bought, adds a switch to a Tensura-style scrollable panel; its state lives in the player's skill-tree data; passives run only if the line is equipped AND the switch is on. |
| D. Visibility | players see only the lines they own; an operator (`hasPermissions(2)`) or a creative player gets an ADMIN INSPECT MODE toggle that lists every registered line. |
| E. Backgrounds | each route names `gui_background`; 283 generated textures (`tools/lines/gen_backgrounds.py`, 15 themes). |
| F. 3D book | `skill_book_3d` model (NeoForge `separate_transforms`: 3D in hand and on the ground, the old sprite stays the inventory icon), tinted by the line's colour, one cover per franchise. |
| G. Content | 275 NEW lines in 11 franchises (25 each) from `tools/lines/rows/<franchise>.py` -> `data/nusmp/skill_trees/<id>.json`. With the 8 hand-built routes that is 283. |
| H. Loot | skill books of ANY line from Abyssal mobs, rift chests and Decennial bosses through the existing `SkillBookLootModifier`/`BossBookDrops`. |

The dungeon parts of the same brief (rifts, last-location waypoint, the 5 room categories, anti-repetition, mobs per tier, the D&D boss, the Mana Core) already exist (1.54 to 1.80): see the table at the end.

## 1. The JSON (what the generator writes, what the engine must read)

Route (new fields in bold): `magic` (= franchise key), **`franchise`**, `name`, `desc`, `book` (`nusmp:<id>_book`), `book_name`, `book_lore`, `color` (`#RRGGBB`), `icon`, `class_id` (= the line id),
`duplicate_sp`, `loot_weight`, **`gui_background`** (`nusmp:textures/gui/skill_trees/<id>.png`), **`book_texture`** (`nusmp:item/skill_book_3d/<franchise>`), **`background_theme`**, `nodes`.

Node: `id`, `name`, `desc`, `parents` [{node, level}], `cost`, `costs`, `max_level`, **`type`** (`active_ability` | `passive_toggle` | `attribute_modifier`; an old node without it is inferred: has `skill` -> active, has `passives` -> passive),
**`kind`** (a catalogue kind; absent on old nodes), **`params`** (string enums such as `{"vfx":"WIND_SLASH","effect":"strength"}`), `values` (numeric tables by level, e.g. `{"damage":[6,7.5,9,10.5],"cooldown":[100,92,84,76]}`),
`x`, `y`, `icon`. Old nodes keep `skill` (a registered ManasCore skill) and `passives` (hand-written passive ids).

The identity of a toggle is `<route id>#<node id>` (a `ResourceLocation` form `nusmp:<line>/<node>` is fine for the capability map). The catalogue (`tools/lines/catalog.json`, generated) lists every kind with its numeric params
(`base`, `min`, `max`, `grow`) and enum params. A test must fail when a kind has no executor or an executor has no kind.

## 2. Rules the runtime must keep
* One equipped line (the existing rule): an active art of an unequipped line cannot be cast; a passive of an unequipped line does nothing. Passives and attribute modifiers also need their toggle ON (default ON when bought).
* Transient attribute modifiers only (never saved), re-synced on a beat, removed when the line is unequipped or the switch is off.
* Active arts are cast through an "Arts" bar of the equipped line (no ManasCore skill per node: the registry is frozen at startup and 5,000 nodes must not be 5,000 registered skills). Cooldowns and magicule cost use the node's `cooldown`/`cost` tables and go through `BalanceLaw`; a Finisher is at least 45 s, an art at least 3 s.
* Particles: only Light/Dark Rukh and the mod's `VfxShape` effects (the vfx names of the catalogue). Sounds are free.
* Server authority: toggles, equips and casts are packets validated on the server; the client never decides.
* Scale: with 283 routes, sync to a client only the lines it owns (and everything for an admin who turned on inspect mode, on request); the screen builds a tree's layout only when it is opened; no per-frame allocation of all nodes.

## 3. Authoring a franchise file
`tools/lines/rows/<franchise>.py` defines `LINES = [Line(...), ...]` (see `lines_lib.py`): 2 stances, 3 to 4 stats, 6 to 8 arts, 6 to 8 passives, 2 finishers per line (19 to 24 nodes). Run `python3 tools/lines/gen_lines.py --only <franchise>`;
it validates (kinds, enums, ranges, unique names, variety: two lines never share the same ordered art kinds nor the same set of passive kinds, a line uses at least 5 different art kinds and 5 different passive kinds) and writes the JSON.
`--check` is the CI guard.

## 4. What already exists for the dungeon half of the brief
| Brief | Where |
|---|---|
| Dimensional rifts, pocket dungeons, shortcut rifts, waypoint, Return Crystal/Gate Key | 1.73.0, `labyrinth/mega/rift/`, `docs/labyrinth/rifts.md` |
| 5 functional room categories, anti-repetition solver, floor layout types | 1.54.0 to 1.59.0, `docs/labyrinth/mega*.md` |
| 15+ mobs per tier, turf wars, Magic Stones | 1.22.0, 1.80.0 (400-mob Mob Foundry, 40 per tier per road), `docs/labyrinth/foundry.md` |
| Decennial boss: legendary actions, lair actions, phases, Mana Core, Gatekeeper | `labyrinth/mega/guard/` (`DndBoss`, `DndProfile`), `docs/labyrinth/dnd_boss.md` |
| Skill books in loot | 1.62.0 `skilltree/loot/` |
