# The Skill Tree screen and its place in the existing menu (1.62.0)

Builder: `stgui` (system 4 of the owner's list). Design: `docs/skilltree/design.md` section 5 (the screen), 7 (UI isolation), 11 (the WHY index). The core it reads is documented in
`docs/skilltree/core.md`. **Every class below carries a `Why:` Javadoc; this file repeats the reasons by group and lists the alternatives that were rejected.** Nothing here has been run in the
game (see section 11): the geometry is unit-tested and previewed, the drawing and the event wiring are type-checked and reviewed.

## 1. New, replaced, kept

* **NEW**: the screen (`SkillTreeScreen` and its parts), the injection of its button into the menus (`MenuInjection`), the pure layout classes (`client/skilltree/layout/**`) with 69 unit tests,
  the preview tool (`tools/skilltree/`), the lang fragment, this document.
* **REPLACED**: only the stub `client/skilltree/SkillTreeClient` (it keeps its two calls, `ItemsClient.init` and `ShopClient.init`, and adds the screen's two hooks).
* **KEPT, untouched**: `client/ui/CodexScreen`, `client/multiverse/FourKingdomsScreen`, `MultiverseStatusScreen`, `FourKingdomsSlot`, `ThemedButton` and the whole theme kit. The screen is added
  next to them and reuses them; no existing file was edited.

## 2. Read this first: the live menu is the Codex, not the two screens the design names

The design (section 5) and the brief name `client.multiverse.FourKingdomsScreen` and `MultiverseStatusScreen` as "the existing menu". **Neither is opened by anything any more.** Since 1.11.0 ("One
menu", CHANGELOG) the Four Kingdoms slot in Tensura's magic screen, its label, and the Multiverse key all open `client.ui.CodexScreen`, and `grep "new FourKingdomsScreen"` finds no caller (the
only `new MultiverseStatusScreen` is the "Status" button of the unreachable Four Kingdoms screen). A button added to those two classes alone would never be seen by a player.

So `MenuInjection` adds the button to **three** screens, matched by exact class: the **Codex** (the one that matters), and the two named ones, kept as the design asked so that the button is
there the day either is linked again. Where it goes (GUI pixels; `ButtonPlacement`, label 45 px wide, so the button is 61 x 16):

| menu | where | why there | at 427 x 240 |
|---|---|---|---|
| Codex | the strip under the book, immediately left of "Grimoire Slot" (`x = bx + bw - 94 - 4 - w`, `y = by + bh + 4`) | the Codex reserves 20 px under its book for exactly such buttons, and "Grimoire Slot" is the one it has | (230, 210) |
| Four Kingdoms | the header row, left of "Status" (`x = left + 340 - 64 - 4 - w`, `y = top + 8`) | same row and same kind of button; the title text ends 70 px before it | (254, 10) |
| Multiverse Status | the top-right corner (`x = width - w - 8`, `y = 4`) | the title is centred, the Great-Sage line is top-left, the buttons are at the bottom | (358, 4) |

If the mirrored spot is outside the window or touches any widget the screen really has (read from its own widget list at event time), the next candidates are the four window corners, then the
top-right corner clamped into the window, so the button is always reachable. The menus' own numbers are mirrored in `ButtonPlacement`; `ButtonPlacementTest.menuSourcesStillMatchTheMirroredConstants`
reads the three menu classes and **fails the build, with an instruction, the day one of those constants changes** (the button would otherwise only be misplaced on somebody's screen).

## 3. What the player sees and does

```
+------------------------------------------------------------------------------------------+
| SKILL TREE   Class: Fire Dragon Slayer                                   (gem) SP 5  [Close] |
+--------------+---------------------------------------------+----------------------------+
| FAIRY TAIL   | Fire Dragon Slayer                [-][+][Fit] | Fire Dragon's Roar          |
| [ico] Fire.. |   ACTIVE                                    | Level 0 / 3                 |
|   ACTIVE 3/9 |  +-----------------------------------------+| Ready to learn              |
| [lock] Light.|  |  nodes, lines from parents, glow on     || Active skill                |
|   LOCKED     |  |  what can be bought, dashed + grey when || Breathes a cone of flame... |
| [lock] Sky.. |  |  the route is sealed; drag to pan       || PER LEVEL   Damage 10 13 16 |
| [lock] Ice.. |  +-----------------------------------------+| COST        Next: 2 SP      |
| [ Set active]|                                             | REQUIRES    Iron Fist, lvl 3|
+--------------+---------------------------------------------+ [      Buy 2 SP           ] -+
| Drag: pan   Ctrl+Wheel: zoom   Enter: buy                                                 |
+------------------------------------------------------------------------------------------+
```

* **Header**: title, the active class in its route's colour, the Skill Points (gem + number; tooltip: points to spend and points spent), Close.
* **Route list** (left): routes grouped by magic family (`magic` field; a family nobody translated gets its prettified id), each with icon, name (1 to 3 lines), `UNLOCKED` / `ACTIVE` / `LOCKED`, the
  count and a progress bar. A sealed route has a padlock over its icon and grey text. Under the list, **Set active** (disabled with "Active" for the current class, with "Wait Ns" during the switch cooldown).
* **Strip** (above the canvas): the route's name and state, or for a sealed route a padlock and "Requires: <book name>"; the three view chips (zoom out, zoom in, fit).
* **Canvas**: the nodes at their `x`/`y`, a one-bend line from every parent. Node looks: sealed route (grey, dimmed), locked by parents (dim), too expensive (muted), **available (pulsing glow)**,
  owned (filled, `n/max` badge), maxed (gold, `MAX`). A line is lit once its child is owned, dashed and marching when it is the next step, dim when the parent is not at the level, dashed grey in a sealed
  route. A purchase flashes the node, throws two rings and fills the line toward it. A sealed route also shows one big faint padlock behind the greyed tree.
* **Detail card** (right): the selected node (name, level, state, kind, description, the per-level numbers with the player's level in `[ ]`, the price of the next level, the requirements coloured
  by whether they are met) and the **Buy** button; for a sealed route: "Route sealed", "Requires: <book>", the book's lore, where to find it; for an unlocked route with nothing selected: a summary.
  The card scrolls when its text is longer than the window.
* **Footer**: a hint line, replaced for a few seconds by a notice (bought, refused, class set) and by a red line while the server has the Skill Tree switched off.

| input | effect |
|---|---|
| click a route | show it (remembered per world session) |
| click a node | select it (never buys) |
| double-click the selected node, Enter or Space, the Buy button | buy one level (one request at a time; see 6) |
| drag (left or middle button) | pan |
| wheel / Shift+wheel / Ctrl+wheel | scroll up-down / sideways / zoom toward the cursor (over the list and the card the wheel scrolls them) |
| arrow keys; `+` `-`; `F` or Home | pan; zoom; fit the whole tree |
| Escape or Close | back to the menu it was opened from |

Sounds: the vanilla UI click on selecting; a three-note rising chime (amethyst, pitches 1.00 / 1.26 / 1.50) **when the synced level rises**, not when the button is pressed. Both looks work: the anime
look uses `UiPanel`, `MagicCircle`, `UiBar`; the classic look is flat dark panels (`TreeSkin`).

## 4. The files, in groups, with the WHY of each group

| group | files | WHY |
|---|---|---|
| entry and injection | `SkillTreeClient`, `MenuInjection` | A NeoForge **screen event** (`ScreenEvent.Init.Post`) is the only way to add a button to a screen without editing it (the owner's "add the new, keep the old"), it fires again after every rebuild of the widget list (so a resize re-adds the button on the fresh list), and it hands over the screen's own widgets so the button can be kept off them. Matching is by **exact class**, so another mod's screen or a subclass is never touched. The handler never throws (an exception during a screen's init would crash the menu): it logs once. Two guards for the one fragility of an injected widget (a rebuild without the event): the old button is removed before the new one is added, and a `Render.Pre` check puts the button back at once if it is missing. Only the open host screen is tracked (one field), cleared on logout. `SkillTreeClient` also clears the screen's static memory on `LoggingOut` (a world's routes and icons must not leak into the next). |
| the screen | `SkillTreeScreen` | A pure **client view**: it owns no game state and decides nothing. Every number is re-read each frame from two synced copies (the player's tree attachment, which the core's sync packet overwrites **in place**, and the route registry), so a purchase, an unlock, an SP Book or a datapack reload changes the picture on the next frame and no reply packet or refresh call exists. It builds the model of a frame once (`refresh`), draws the panels and wells in `renderBackground` (under the widgets) and the content in `render` (over them), so no drawing can cover a button. |
| its parts | `RouteList`, `NodeCanvas`, `RouteShape`, `DetailPanel` | One class per region, so each can be read and changed alone. The canvas only turns the numbers of the pure layout into `GuiGraphics` calls; `RouteShape` caches a route's layout and routed lines under the route record's identity (a sync that delivers a new record rebuilds it, nothing else does); the detail card builds its word-wrapped blocks once per (route, node, level, SP, parent levels, width, look) because wrapping is the expensive part and the card must scroll, which needs the total height first. |
| the look | `TreeSkin`, `ItemIcons`, `SkillTreeLang`, `TreeSounds`, `TreeActions` | `TreeSkin` is the one place that knows both looks (the config switch must take effect at once, and the look must not be spread as `if`s). `ItemIcons` resolves the item ids of the data once and falls back to a book (a typo in a datapack must not hide a node). `SkillTreeLang` holds every key as a constant (a typo is a compile error, the fragment is generated from the same list). `TreeSounds` queues the three chime notes. `TreeActions` is the only class that names the two request payloads and never lets a send throw into a click handler. |
| the pure layout (`client/skilltree/layout/`) | `Rect`, `ScreenLayout`, `TreeLayout`, `LineRouter`, `Polyline`, `NodeState`, `ButtonPlacement`, `RouteGrouping`, `TreeText`, `PurchaseGate`, `ClickTracker` | No Minecraft import, so the arithmetic that decides where things are is **unit-tested at every GUI size the game can produce** and is the same code the preview tool runs. `ScreenLayout` computes every rectangle from the window size alone (a resize cannot leave anything stale; the panel is capped at 640 x 400; columns are clamped and give way before the canvas does). `TreeLayout` is an affine map `screen = pan + content * zoom` (panning is addition, zooming about the cursor is one formula, clamps are per axis) plus hit testing and edge routing. `LineRouter` makes orthogonal lines with one bend (they read as a tree diagram, share their first stretch when children hang off one parent, and need only `fill` calls), tries horizontal-first, then vertical-first, then a line through the gap, so a line avoids unrelated nodes whenever a clean path exists. `Polyline` answers the three questions the draw code has (length, the first fraction, the dashes with a phase). `NodeState` wraps the **core's** `TreeRules` (section 5). `PurchaseGate` lets exactly one request through until the synced level rises or a timeout passes (a nervous double click must not buy two levels), and the timeout is also how the screen learns the server said no. `ClickTracker` pairs two clicks on the same node (Minecraft gives no double-click event). |
| the tools | `tools/skilltree/preview_skilltree.py`, `LayoutDump.java`, `sample_fire_dragon_slayer.json` | A mock that re-implements the layout numbers by hand drifts from the Java the day a constant changes. `LayoutDump` runs the real pure classes and prints every rectangle, node, state and line as JSON; the Python side only draws what Java computed. |
| the tests | `src/test/java/.../client/skilltree/layout/*Test.java` | Each edge that a screen goes wrong on has a test: the last pixel of a box, a pan that leaves a gap, a zoom that drifts, a line that ends in the wrong node, a button over a widget at 3840 x 2160. |

## 5. What the screen reads from the core (aligned with the finished core, `docs/skilltree/core.md`)

| need | the core's API that the screen uses | notes |
|---|---|---|
| the player's tree, live | `SkillTreeApi.of(player)` wrapped in `new PlayerTreeState(...)` (a `TreeState`) | nothing is copied; `TreeNet.receiveTree` overwrites the same object in place |
| the routes | `RouteRegistry.all()`, `byId`, `byClass` | on the client these are the routes the server sent (id order); in single player the server's own set |
| a node's state | `TreeRules.status(route, node, state)` -> `NodeStatus` (`ROUTE_LOCKED`, `PARENTS_LOCKED`, `TOO_EXPENSIVE`, `AVAILABLE`, `OWNED`, `MAXED`) | through `NodeState.of`; the screen uses the core's enum, it has none of its own |
| can the next level be bought, and why not | `TreeRules.check(...)` -> `Verdict` | the Buy button and the refusal notice are worded from it, in the server's own order and words |
| the price of the next level | `TreeRules.priceOfNext(node, level)` (-1 at the maximum) | |
| the per-level numbers | `RouteRegistry.value(routeId, nodeId, key, level, fallback)` | the very function the abilities read, so the card cannot show a number the ability will not use |
| may this class be made active | `TreeRules.classSwitch(state, route, gameTime, SkillTreeApi.classSwitchSeconds() * 20, creative)` -> `ClassVerdict` | the button is disabled exactly while the server would refuse; the seconds come from `SkillTreeApi.classCooldownTicks(player)` |
| the master switch | `SkillTreeApi.isEnabled()` | buttons are disabled and the footer says so when the server has the Skill Tree off |
| buy / set class | `net.UnlockNodePayload(String route, String node)`, `net.SetClassPayload(String classId)` via `PacketDistributor.sendToServer` | ids only; the server decides |

`TreeConfig` is a SERVER config that NeoForge copies to a connecting client, so `classSwitchSeconds()` and `isEnabled()` on the client are the numbers the server holds the player to (the core's
doc, section 7). The core's `TreeCoreClient` (forgets the synced routes on logout) is the core's; the screen does not touch it.

## 6. The refresh model, and what a click does

Every frame, `refresh()` re-reads the tree and the registry, rebuilds the per-node states, compares each level with the previous frame's (a **rise is a purchase that went through**: chime, pulse,
line fill, notice), and settles the requests in flight. A click on Buy sends **one** `UnlockNodePayload` and arms the `PurchaseGate`: further clicks are ignored until the node's synced level
rises (the gate opens, the chime plays) or 800 ms pass (the footer says "The server did not accept that"). The screen never assumes success. Selecting a node never spends anything: SP is scarce and
respec is off by default, so the fast path is a **double-click on the node that is already selected**, Enter, or the button. Switching class works the same way with `SetClassPayload`.

## 7. Previews (`$S/previews/stgui/`, made by `tools/skilltree/preview_skilltree.py`)

`python3 tools/skilltree/preview_skilltree.py OUT_DIR [--scale N]` needs a JDK 21, Pillow and numpy. It compiles `LayoutDump.java` against `src/main/java` (set `SKILLTREE_SRC` to use another source
root), runs it for the size and the player state, and draws. **What is real**: every rectangle, node position, node state, price, verdict and routed line, and the three buttons' places. **What is a
mock**: colours and panels (the theme kit of `tools/ui_preview/preview_ui.py`), the item icons (coloured squares), and the text, which uses the game's glyph widths (a table in the script, from memory
of the font's ascii page) but a monospace face. It is not the game.

Stills: `skilltree_{locked,unlocked,buyable}_{427x240,854x480}.png`, `skilltree_buyable_320x240.png` (the smallest GUI the game makes), `menu_codex_427x240.png`, `menu_codex_854x480.png`,
`menu_four_kingdoms_427x240.png`, `menu_status_427x240.png` (the new button outlined in gold). The route is a SAMPLE of the shape of the Fire Dragon Slayer tree (9 nodes, two branches, a Secret Art
that needs one node of each): Iron Fist, Roar and Crimson Lotus: Exploding Flame Blade are the names of the design; Claw, Wing Attack, Brilliant Flame and Dragon Force are canon technique names from
memory, used only here. The real tree is the Fairy Tail builder's.

## 8. Decisions and alternatives rejected

| decision | rejected | why |
|---|---|---|
| a screen event adds a `ThemedButton` | edit the three menus; a mixin; a keybind | the owner's rules (keep the old, one menu, no separate key); an event is one removable class and survives a redesign because it matches by class |
| a real widget, with a self-heal | a button drawn and hit-tested by `Render.Post` / `MouseButtonPressed.Pre`, like `FourKingdomsSlot` does | a widget gets focus, narration and the theme's button for free; the render-event way is the right tool for another mod's screen, but these are the mod's own screens. The self-heal covers the case a widget approach is weak at |
| inject into the Codex too | only the two named screens | the two are dead code; a button there is invisible |
| the node state comes from the core's `TreeRules` | a local copy of the rule (the brief's fallback) | two copies of a rule drift, and a drifted copy on the client is a button that does nothing or a purchase that is offered and refused |
| select, then buy (double-click, Enter, button) | click to buy at once | SP is scarce and respec is off by default; a stray click must not spend it |
| "Set active" under the route list | in the strip next to the name | the strip (191 px at 427 x 240) cannot hold the name, a 70 px button and the view chips; the action belongs to a route, and the list is where routes are |
| the three view chips in the strip | over a corner of the canvas | an overlay covers whatever node sits in that corner (it did, in the first preview: the bottom-right node of the Fire tree) |
| a sealed route shows its tree, greyed, with one faint padlock | hide the tree; a padlock on every node | the owner wants the route visible ("greyed with a padlock, dashed lines, Requires: <book>"); a padlock on nine nodes was noise |
| orthogonal lines with one bend | diagonals; curves | diagonals alias and need rotated quads; curves need a texture per angle; one-bend lines are `fill` calls and read as a tree |
| route rows as tall as the wrapped name needs (1 to 3 lines) | fixed rows with ellipsised names | names are data ("Lightning Dragon Slayer"); a cut name is ambiguous |
| fixed GUI-pixel fonts, no scaled text | 0.75 / 0.85 text scale to fit more | scaled bitmap text is uneven; the layout makes room instead |
| pan and zoom remembered in static maps, cleared on logout | saved in a client config | UI state, not a setting; it must not outlive a world's routes |
| the class cooldown comes from the core and the same rule the server runs | a client constant or a guessed 30 s | a guessed number is wrong the day the owner changes the config |

## 9. Tests

`src/test/java/com/newuniverse/nusmp/client/skilltree/layout/`, 69 tests, run locally with `jt.sh` (the JUnit shim) against the merge of this branch and the core's: `ScreenLayoutTest` (6: every
GUI size from 320 x 240 to 4K at every scale gives disjoint regions inside the panel with a usable canvas; the 427 x 240 numbers are pinned; the 640 x 400 cap; tiny windows), `TreeLayoutTest` (16: grid to
pixels, hit testing at the edges of a box, pan clamps, zoom about the cursor, the first view, edges of the Fire shape start and end on the right boxes, nothing crosses an unrelated node), `LineRouterTest`
(8: each of the three shapes and its fallback order, and a sweep over a 4 x 4 grid), `PolylineTest` (8), `NodeStateTest` (10: the wrapper against the core's rules over every state of the sample
tree), `SmallRulesTest` (12: the countdown, the gate, the double click, number and name text, grouping), `ButtonPlacementTest` (9: every GUI size x every menu, off every widget and the menus' art, the
corner fallback, and the source guard). **Mutation check** (15 mutants applied one at a time to a copy of the final code, spread over 10 of the 11 pure classes, every one compiled; `Rect` is a plain record and has none): all 15
killed; earlier mutants that did not compile or were equivalent were replaced by valid ones. The tests that read the menus' sources run from the project folder.

## 10. Lang keys

Prefix `gui.nusmp.skilltree.` (59 keys, plus the dynamic `magic.<family>` headings; only `magic.fairy_tail` is translated, others fall back to the prettified id). The fragment is
`$S/hero/frag/stgui.lang.json` (flat JSON, ASCII-escaped); the lead merges it into `en_us.json`. The constants of `SkillTreeLang` and the fragment were generated from one list and checked against
each other (no unused, no undefined key).

## 11. What was verified, and what was not

**Done**: the pure classes compile with `-Xlint:all` and pass their 69 tests, mutation-checked; the screen, injection and parts were **type-checked against the API stubs together with the finished
core's real source** (a merge preview of this branch and the core's branch: 0 errors in my files; the stubs flagged 15 deliberately wrong calls on the classes used, so they are strict); the previews
were rendered and looked at, and three layout faults found that way were fixed (a chip over a node, route names overflowing, a heavy padlock).

**NOT done, cannot be from here**:
* nothing was started in the game: the screen, the event, the sounds, the item rendering and the packets have never run;
* **the exact button position in the real menu at real GUI scales is the least tested part**: it is arithmetic mirrored from the menus' source and tested against my reading of those menus, not
  against a rendered screen; the source guard keeps the mirror honest, the widget check and the corner fallback keep the button reachable;
* that NeoForge fires `ScreenEvent.Init.Post` again when a screen is rebuilt (a resize, or returning from the Skill Tree to its parent): it is the documented behaviour and the injection removes the
  old button first, but the self-heal exists because this was not seen running;
* item icons are drawn at a depth of about 150 and everything that must lie over them is lifted to 200 (the standard way in this GUI toolkit); the order was reasoned, not seen;
* the glyph widths in the preview are from memory of the vanilla font; wrapping in the game may differ by a word here and there;
* `SkillTreeApi.classCooldownTicks` and `TreeConfig` are read as the core documents them (a server config copied to the client); I did not run a dedicated server;
* sounds: `SoundEvents.UI_BUTTON_CLICK` (a holder) and `AMETHYST_BLOCK_CHIME` (a sound event) go through the two `SimpleSoundInstance.forUI` overloads that other screens of this repository use.

## 12. For the lead

* **Depends on the core** (all present in its branch): `skilltree/rules/TreeRules`, `TreeState`, `skilltree/PlayerTreeState`, `SkillTreeApi` (`of`, `isEnabled`, `classSwitchSeconds`,
  `classCooldownTicks`), `RouteRegistry` (`all`, `byId`, `byClass`, `value`), `net/UnlockNodePayload(String, String)`, `net/SetClassPayload(String)`. The tests also use `rules/SimpleTreeState`.
  Merge the core first, or together.
* **No contract change is needed.** `SkillTreeClient.init` is already called from `NUSMP` on the client.
* Merge the lang fragment. Nothing else is shared: no edit of `NUSMP`, `en_us.json`, the version files or the changelog was made.
* The three menu classes' layout constants are now guarded by a test (section 2): changing the Codex's book size, the "Grimoire Slot" button or the Four Kingdoms panel fails
  `ButtonPlacementTest` until `ButtonPlacement` follows. That is deliberate.
