# Unique Skill Lines in Tensura's "Ability Selection" menu

What the owner asked: a Unique Skill Line the player owns is a top-level **Unique** entry in the left-hand list of Tensura's Ability Selection menu, next to Tensura's own skills. The
line's arts and passives are shown **nested** under that entry when it is selected. The passive switch is a checkbox like Tensura's (ticked = "ON", empty = "OFF"). Players see only the
lines they own; operators get an ADMIN INSPECT bypass that lists all 283. Many lines may be owned, only one is equipped.

Nothing here could be run in a game (see "Not verified" at the end). Everything that can be decided without a game is a pure class with unit tests; everything that needs Tensura's own screen
is isolated in four small client classes that fail soft.

## NEW / REPLACED / KEPT (CLAUDE.md rule 5)

| Part | Status | Where |
|---|---|---|
| One stand-in Tensura **UNIQUE** skill per route (283: 275 generated lines + 8 classic routes), registered at mod construction | NEW | `skilltree/menu/LineStandIns`, `LineStandInSkill`, `LineIndex` |
| Teaching / forgetting the stand-in as the line is owned / lost (login, respawn, unlock, tick check) | NEW | `skilltree/menu/StandInGrants`, `StandInPlan` |
| Fog of war in the screen's list + ADMIN INSPECT synthetic entries (client side) | NEW | `client/skilltree/TensuraMenuClient`, `layout/MenuEntries` |
| Nested panel (arts / passives / stat bonuses), Equip button, Skill Tree button | NEW | `client/skilltree/TensuraMenuPanel`, `layout/MenuNesting`, `layout/EquipRule` |
| Tensura-style ON/OFF checkbox widget | NEW | `client/skilltree/TensuraCheckbox`, `layout/MenuCheckbox` |
| Panel placement from values read off the live screen (never guessed Tensura pixels) | NEW | `layout/MenuPlacement`, `client/skilltree/TensuraScreenAccess` |
| Config file `nusmp-tensura-menu-common.toml`, lang fragment, 283 icons, self-test `tree.tensura_menu` | NEW | `MenuConfig`, `tools/tensura_menu/*`, `MenuSelfTests` |
| Our own Passives panel, Skill Tree screen, Arts bar, inspect mode in the Codex/Status menus | **KEPT** (nothing replaced; the owner said "in addition to") | `client/skilltree/PassivesScreen`, `SkillTreeScreen`, `MenuInjection`, `InspectMode` |
| Toggle payload, inspect payload, set-class payload and their server checks | **KEPT, reused as they are** | `LinePayloads.TogglePassive`, `Inspect`, `SetClassPayload`, `LineNet` |
| Single active line (`LineGate.equipped` = owns the route AND its class is the active class) | **KEPT, reused** | `skilltree/line/LineGate` |
| Fog of war on the wire (full trees only for owned lines, node-less cards for the rest), admin check `hasPermissions(2)` or creative | **KEPT, reused** | `LineSync`, `LineCard`, `LineSyncPlan` |
| Data-driven registry, `ReloadListener`, JSON schema (`skill_line_id`, nodes, parents, costs, node types) | **KEPT** (already built: 283 routes) | `skilltree/data/*`, `data/nusmp/skill_trees/*.json` |
| Capability of toggle states and its sync | **KEPT** (the player's skill-tree record holds the `route#node` switch map) | `PassiveToggles`, `state/PlayerSkillTree` |
| Execution wrappers (equipped line + switch ON before any passive acts) | **KEPT** (`LineGate.passiveLevel` / `activeLevel`, run before every executor) | `skilltree/line/LineGate`, `LinePassives` |
| 3D skill books, loot modifiers (books, rift books), Hub / Tensura coin economy, cyclic anti-dead-end WFC, dimensional rifts, bow display transforms | **KEPT / exists or handled by someone else; not touched** | `skilltree/item`, `skilltree/loot`, `labyrinth/**`, `client/labyrinth/rift/DimensionalRiftRenderer`, bow models |

Nothing was replaced and nothing was deleted. Existing files got exactly two one-line additions (below).

## The design and WHY of each choice

### 1. A stand-in skill per line, not a hacked list

Tensura builds the left-hand list from the ManasCore skills the player has **learned**, filtered by `Skill.SkillType`. The only way to be "a top-level Unique Skill next to Bewilder" is to be a
real Tensura `Skill` of type `UNIQUE` that the player has learned, so each line gets a `LineStandInSkill`. It carries the line's name (lang key `nusmp.skill.line_<id>` with the line's own
name as fallback), description (the route's text when the route is known on that side), and a 32x32 icon (`textures/skill/line/<id>.png`). Tensura then draws it with its own list code, scroll bar, search
box and detail view, which is as close to "inside Tensura's menu" as an add-on can get without a mixin into a class whose source cannot be read.

**Why registered at mod construction from `assets/nusmp/skill_books.json`:** the ManasCore skill registry freezes at startup, so a line can never be registered when a datapack loads or a book
is read. The route files are server data and do not exist yet at that point; the one list of every line that is a plain jar resource is `skill_books.json` (generated from the same route files;
`LineIndexTest` fails the build if the two differ). The registry is filled in id order from a `TreeMap`, so every machine registers the same skills in the same order (the registry sync refuses a
client whose registry differs). It is a **separate `DeferredRegister`** registered after `NUSkills.SKILLS`, so the numeric position of every skill the mod had before does not move.

**Why 283 and not 275:** the spec says "all 283" for admin inspect, and the 8 classic routes (Fire Dragon Slayer, ...) are routes too. They get a stand-in as well; their nested view lists the
nodes by type (a classic passive has no switch, so no checkbox).

**Why the stand-in is harmless:** `canInteractSkill`, `canActivateSkill`, `canBeToggled`, `canTick`, `canScroll`, `canBeSlotted` all answer false and it keeps no state (no mastery, no cooldown, no
tag). Pressing a Tensura skill key can never run an art a second time; the arts stay where they were (Arts bar and `LineCast`, gated by `LineGate`). It cannot be put in a skill slot.

### 2. Teaching is one comparison of two sets, run often

`StandInPlan.diff(held, owned, standInsThatExist)` says which to learn and which to forget. `StandInGrants.reconcile` applies it on login, respawn, the unlock listener (so the entry appears the
moment a book is read), a half-second check of the owned set's signature on the player's own tick, and a full compare every 5 seconds. Event-by-event add/remove drifts; a set comparison cannot, and it
also heals a stand-in that another Tensura skill copied or removed. A stand-in held without ownership is always forgotten, which is the server half of the fog of war. It touches only
`LineStandInSkill`s (`SkillGrants.reconcile` only touches `TreeSkill`s, so the two never fight). Teaching is silent. With `tensuraMenu.enabled = false` the wanted set is empty, so the stand-ins are
cleaned away.

### 3. Events, not a mixin

`ScreenEvent` gives the three hand-holds needed from outside, and the owner's rule is "add, never replace":

* **Render.Pre / Init.Post: reconcile the list.** `TensuraMenuClient.reconcile` works on the screen's own public `abilities` list: the stand-ins of owned lines are there, no other stand-in is, and
  in ADMIN INSPECT a synthetic stand-in (`createDefaultInstance`) of every other line is appended, so all 283 are in the left-hand list. It then calls the screen's own protected
  `updateFilteredAbilities()` (reflection) so Tensura's search and ordering apply to our entries. It only does anything when `MenuEntries.diff` is non-empty, never every frame.
* **Render.Post:** draws the nested panel and the admin switch on top (z = 400).
* **MouseButtonPressed.Pre / MouseScrolled.Pre:** clicks and the wheel on the panel are handled and cancelled so they never reach a Tensura widget underneath.

Every handler catches and logs once. If the screen's lists turn out to be read-only, the integration switches itself off for the session and Tensura's screen behaves as it always did.

### 4. Fog of war and ADMIN INSPECT

Server: only owned lines' stand-ins exist in a player's skill list; only owned lines' full trees are sent (`LineSync`, existing). Client: `MenuEntries.wanted` = owned lines, plus every line **only**
when `InspectMode.active` (operator `hasPermissions(2)` or creative, AND the switch on, the same rule as `LineSync.isAdmin`). The screen enforces it too because a single-player world runs the server in the
same JVM and the client can see the whole registry there. An inspected line the client has only a card for is requested once with the existing `Inspect` payload (the server re-checks admin on each
request); it is shown dimmed, with no checkboxes and no Equip button, read-only by construction.
The switch is a small button drawn by the panel next to Tensura's frame ("ADMIN INSPECT"), shown only to those who may use it and only on the Unique/unfiltered skill list.

### 5. Nested view

`MenuNesting.build` turns a line into three sections in a fixed order (Arts, Passives, Stat bonuses), each a header with its entries in route-file order. An **owned** line lists only what is bought (the
Skill Tree is where you shop); an **unowned** (inspect) line lists everything dimmed. A node that only unlocks others is never listed. `dim` marks entries of a line that is not equipped (their effects are off).

### 6. Checkbox

`TensuraCheckbox` is an `AbstractWidget`; ticked = ON (tooltip "ON"), empty = OFF (tooltip "OFF"; `MenuCheckbox` maps state to key and click to next state). A click sends the existing
`TogglePassive` payload through `TreeActions.toggle` and flips the client's own copy at once (same as the Passives panel); the server (`SkillTreeApi.setPassiveEnabled`: owned, bought, has a switch) is the
only authority and re-sends the tree on a refusal. **Skin:** the default is the vanilla widget sprites (always correct). `checkboxSkin = "tensura"` tries Tensura's own `CHECKBOX` texture, but only if its size
says it is a vertical strip of square frames (off on top, on below); any other shape falls back to vanilla instead of drawing a wrong part of an image nobody here can inspect.

### 7. Single active line

Already enforced by the engine: `LineGate.equipped` = the player owns the route and the route's class id is the active class, and no art or passive runs otherwise. The new Equip button sends the existing
`SetClassPayload` (server: ownership and class-switch cooldown). `EquipRuleTest` proves on all 283 real route files that class ids are unique, so with every line owned exactly one is equipped for any active
class. Nothing needed to be added server-side.

### 8. Placement without guessing

`MenuPlacement` chooses the panel rectangle from values the screen can be **asked** at runtime: the template's static `guiLeft/guiTop/guiRight/guiBottom` (reflection on `SimpleScreen`), the scroll-bar getters
`getScrollBarX/Y/TotalSpace/Width` (where the list ends), and the window size. Order tried (config `panelPlacement` picks the first): right of the frame, left, inside (right of Tensura's list), below, and
finally the window edge. Each is checked to fit the window and not to cover the frame (inside must lie within it). The frame is trusted only when `SimpleScreen.scale == 1`, and a missing / zero /
oversized frame gives the window-edge fallback, which is always valid. Invariants are tested over seven window sizes, six frame sizes and every mode word.

## Edits to existing files (additions only)

* `skilltree/SkillTreeSystem.java`: one line `LineStandIns.init(modBus, modContainer);` before `TreeCore.init`.
* `client/skilltree/SkillTreeClient.java`: one line `TensuraMenuClient.init();` after `ArtsBarClient.init(modBus);`.

No change to `en_us.json`, CHANGELOG, README, LIBRARIES, `gradle.properties`, `neoforge.mods.toml`; the version is not bumped.

## Config (`nusmp-tensura-menu-common.toml`, section `[tensuraMenu]`)

| Key | Default | Meaning |
|---|---|---|
| `enabled` | `true` | Master switch: teach stand-ins and hook Tensura's screen. Off = stand-ins cleaned away, screen untouched. |
| `panelPlacement` | `auto` | `auto`, `right`, `left`, `below`, `inside`: which docking is tried first. |
| `panelWidth` | `210` | Width the panel asks for (150 to 320), shrinks to fit. |
| `checkboxSkin` | `vanilla` | `vanilla` or `tensura` (see 6). |
| `adminButton` | `true` | Show ADMIN INSPECT to operators / creative players. |

## Language

All strings go through `Component.translatableWithFallback` (`skilltree/menu/MenuText`). The flat fragment to merge into `en_us.json`, generated by `python3 tools/tensura_menu/gen_lang.py`, is
`tools/tensura_menu/lang_tensura_menu.json` (26 UI strings + 283 stand-in names `nusmp.skill.line_<id>`). `MenuTextTest` compares it with the code in both directions.
Icons: `python3 tools/tensura_menu/gen_line_icons.py` writes `assets/nusmp/textures/skill/line/<id>.png` (32x32, deterministic).

## Tests

JUnit: `LineIndexTest`, `StandInPlanTest`, `MenuTextTest` (skilltree/menu); `MenuNestingTest` (also over all real routes), `EquipRuleTest` (single active line on the real data), `MenuEntriesTest` (fog of war and
admin bypass at 283), `MenuCheckboxTest`, `MenuPlacementTest` (client/skilltree/layout). In-game self-test, key **`tree.tensura_menu`** (run by the CI server smoke test): one stand-in per route of the live
registry and no extra, unique ids, every one resolves in the ManasCore registry to a `LineStandInSkill` of type UNIQUE, names present, and inert.

## Not verified (nothing visual or in-game could be run)

* **Everything visual.** How the nested panel, the admin switch and the checkbox look and sit next to Tensura's real screen has never been seen.
* That Tensura's `AbilitySelectionScreen` really lists a learned `UNIQUE` skill (assumed from the `skillFilter` / `updateAllAbilities` signatures), that `abilities` and `filtered` are mutable lists, and that
  `updateFilteredAbilities()` rebuilds `filtered` from `abilities` without dropping entries (if a list is read-only the integration turns itself off and logs once; a missing method falls back to adding to `filtered`
  directly).
* The meaning of `SimpleScreen.guiLeft/..` (assumed unscaled GUI pixels of the frame) and of the scroll-bar getters (assumed: the list's scroll bar). If wrong the panel is mis-docked or pinned to the window edge;
  `panelPlacement` and `panelWidth` are the escape hatches.
* Tensura's checkbox texture layout (so `checkboxSkin = tensura` is opt-in and shape-guarded) and that vanilla `widget/checkbox*` sprites look right inside Tensura's art style.
* What Tensura itself does with a learned Unique skill it did not register (e.g. a Unique-skill limit, a "skill obtained" toast, evolution lists). Teaching is silent; the stand-in has no mastery.
* That Tensura's own detail view for the selected stand-in shows only name / description / icon and offers no usable checkbox (we answered `canBeToggled = false`).
* The `ScreenEvent` timing (Render.Pre reconcile before Tensura draws) and that cancelling `MouseButtonPressed.Pre` on the panel blocks Tensura's own handler.
* The registry sync with a client that has a different mod build (283 extra registry entries; the dedicated server and the client must be the same jar, as for every other skill).
* Gradle was not available, so the full test suite and a real build were not run; the new files were type-checked against the API stubs and the new and neighbouring JUnit tests were run with the offline harness.
