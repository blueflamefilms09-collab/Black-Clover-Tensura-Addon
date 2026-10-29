# GUI Move: every custom GUI of the mod can be moved

The owner's ask: "add a way to move the djinn gui like the solo leveling menu, actually just make that a base thing for any and all custom on screen GUI".
The System (Solo Leveling) window could already be dragged with the mouse. That model (a corner or middle anchor plus an offset, kept on any window size) is now the
base of a framework, `client/ui/move/`, and every custom GUI of the mod goes through it, the Djinn panel first. All of it is client only.

## How the player uses it

* Press **F9** (rebindable: Controls > Multiverse of Anime > "Move GUIs (edit mode)") or type `/nusmp gui move`, or press **Move GUIs** on the GUI Size screen (F8).
  (The command is `/nusmp gui ...` and not `/multiverse gui ...` on purpose: the word `multiverse` belongs to the server's command tree, see `RelicWearClient`.)
* Every HUD element that was on screen gets a dashed outline and its name; the ones that are not on screen right now (a Djinn panel without a Djinn) are shown dimmed
  (the **Show all** switch), so they can be placed too.
* **Drag** to move (snaps to the screen's edges and middle and to the other GUIs; **Snap** switch, hold **Alt** for free placement; pink guide lines show what it snapped to).
  **Wheel** or **+ / -** to size it (0.5 to 2.5, Shift for big steps), **arrow keys** to nudge 1 px (Shift: 8), **R** resets the selected one, **H** or a right click hides or
  shows it, **Tab** selects the next one, **Reset all** and **Done** below. A GUI can never be dragged off the screen: at least 24 GUI pixels always stay in reach.
* Panels of the mod's screens (codex, forge ...) are all centred, so they are picked one at a time: turn **Panels** on (key P) and press Tab. The picked panel takes its
  place the next time its screen opens, at its own size times the GUI Size.
* `/nusmp gui reset` puts every GUI back. Nothing moves unless the player moves it: an element without a saved place is drawn exactly as before.

## The framework

| Class | Job |
|---|---|
| `GuiMoveMath` (pure) | anchors, the 24 px clamp, snapping, scale limits, wheel and nudge, the order of the two sizes, mouse transform, migration of the old System offsets |
| `GuiMoveLayout` (pure) | the text kept per element (`p=1;a=3;cx=0;cy=0;ox=12;oy=4;s=1.25;h=0`), forgiving parse, exact round trip |
| `GuiMoveRegistry` (pure) | the list of elements (id, name, HUD or panel, default place, margin, size, source files) and the list of **fixed** GUIs with their reason |
| `GuiMoveOptions` | CLIENT config `nusmp-gui-layout-client.toml`: one text per element under `[elements]`, plus `snap`, `showAll`, `showPanels`. Parsed once when the file loads or changes |
| `GuiMove` | the API (below), the key, the commands, the layer and screen hooks used by `GuiScale` |
| `GuiMoveScreen` | edit mode; flat dim, no blur; draws only while open |

### Register a new GUI in 5 lines

```java
// 1. GuiMoveRegistry:  hud("my_hud", "My HUD", How.BEGIN, BC /* default place */, 6 /* margin */, 120, 30 /* size */, "client/my/MyHud.java");
// 2. tools/lang/gui_move.json:  "gui_move.element.my_hud": "My HUD"
// 3. in the draw code:
if (GuiMove.hidden("my_hud")) return;
boolean moved = GuiMove.begin(g, "my_hud", defX, defY, w, h, pivotX, pivotY);   // where it is drawn anyway, its size, the point GUI Size grows it from
try { /* draw exactly as before, at defX/defY */ } finally { GuiMove.end(g, moved); }
// 4. mouse hit tests of a moved element:  double lx = GuiMove.toLocalX("my_hud", mouseX), ly = GuiMove.toLocalY("my_hud", mouseY);
// 5. a screen panel needs no code: add panel("my_screen", "My screen", w, h, "client/.../MyScreen.java") (it must have a GuiScaleRegistry entry of the same id)
```
`GuiMove.apply(g, id, x, y, w, h)` is the same with the pivot at the top-left corner. A HUD layer that already has a GUI Size layer entry needs only
`GuiMove.report(id, x, y, w, h)` in its draw code: the layer hook moves it. `GuiMoveRegistryTest` fails the build when a custom GUI class is neither registered nor fixed.

### Saved place and window changes

A place is one of nine anchors (corner bits + "from the middle" flag per axis) and an offset in the element's own pixels, the System window's model since 1.58.0.
A right-anchored element keeps its distance from the right edge on any window size or GUI scale, a centred one stays centred, a bottom one stays at the bottom, and the
24 px clamp is applied on every read, so a window that shrinks (or a hand-edited file) can never leave an element out of reach. The edge-or-middle choice uses a band of a
sixth of the screen around the middle (the System edit screen uses a third; both write the same kind of place).
Offsets are in own pixels, not fractions of the screen: the element and its margin scale together, which is what keeps an element on its edge. (The brief said "normalised
offset": this is the anchor-relative form of it; a fraction of the screen would make a right-hand element drift off its edge when the window grows.)

### GUI Size and GUI Move: the order

`total = move size (0.5..2.5, set in edit mode) x GUI Size size (global or per GUI, from GuiScale)`, held to 0.25..5, applied **once** (`GuiMoveMath.total`).
`GuiMove.begin` applies the GUI Size itself (not moved: around the pivot as `GuiScale.begin` always did, bit for bit; moved: at the saved place), so a caller must not call
`GuiScale.begin` for the same id (`GuiMoveRegistryTest` pins that). Screen panels: `GuiScale`'s screen hooks now draw the screen through one transform
`screen = t * local + o` (plain GUI Size about the window's middle when not moved, the saved place when moved) and send every mouse event back through it.

## The audit: every custom GUI of the mod

| GUI (id) | Kind | Decision | How |
|---|---|---|---|
| Djinn panel, themed and classic bar (`djinn_panel`) | HUD | **movable** (the owner's ask) | `DjinnGui.render`, `DjinnScreenFx` classic bar: `begin/end`, `hidden` |
| System / Solo Leveling window (`system_hud`) | HUD | **movable**, old offsets migrated | self-placing: `SystemClientConfig` reads/writes the store, `SystemOverlay` reports where it drew; its own edit screen is kept and edits the same place |
| System pop-up lines (toasts) | HUD | follow the window | they slide in at the left and keep clear of the window |
| System floor report (`floor_card`) | HUD | **movable** | layer hook + `report` |
| Hero weapon HUD (`hero_hud`) | HUD | **movable** | `begin/end`; its own x / y / scale config stay the default |
| Hero curse corruption bar (`curse_bar`) | HUD | **movable** | `apply/end`; `barX` / `barBottom` stay the default |
| Boss bars, animated and custom (`boss_bar`) | HUD | **movable** | both overlays: `begin/end`, hidden cancels vanilla's layer too |
| Quest tracker (`quest_tracker`) | HUD | **movable** | layer hook + `report` |
| Relic key hints (`relic_keys`) | HUD | **movable** | layer hook + `report` |
| Song meter (`song_meter`) | HUD | **movable** | layer hook + `report` |
| Scale weights readout (`scale_readout`) | HUD | **movable** | `apply/end` |
| Page unlocked toast (`page_toast`) | HUD | **movable** | layer hook + `report` (the slide-in is part of the drawing) |
| Four Kingdoms menu, Codex, Compat codex, Engravement codex, Avatar actions, Tower Wizard questions | screen panel | **movable** | screen hooks of `GuiScale` |
| Avatar creator, Soul forge, Avatar inventory, Hub quests, Hub shop, Relic forge, Grimoire slot | container panel | **movable** | the same; the panel is the container's own image rectangle |
| Multiverse status book, Skill shop, Weapon forms book, Dungeon map, System stat screen | full-window screens | fixed: nothing to move | listed in `GuiMoveRegistry.fixed()` |
| Multiverse status compact panel, Four Kingdoms slot button | parts of another screen | fixed: they follow that screen | |
| Unique skill acquired banner, mana-zone camera tint, Djinn vignette and flash, curse vignette and offer prompt | full-screen effects and announcements | fixed | |
| GUI Size screen, System edit screen, GUI Move edit screen | tools | fixed: raw pixels | |
| Tooltips | follow the mouse | fixed | |
| Mana / aura bars, skill and ability bars (left slots), minimap, subtitle overlays | not drawn by this mod | nothing to move | no such HUD class exists in this mod (they belong to Tensura and other mods); the voice system only plays sound |

The GUI Size entry `arts_bar` has no layer behind it anywhere in the mod, so it is listed as fixed with that reason.

## NEW / REPLACED / KEPT

* NEW: the whole framework, the edit mode, the key, `/nusmp gui move|reset`, `nusmp-gui-layout-client.toml`, the Move GUIs button on the GUI Size screen, movability of every
  element in the table, hide switch per HUD element, `lang` fragment `tools/lang/gui_move.json`, tests, preview tool.
* REPLACED: nothing was removed. The System window's place and size are now stored in the layout file (it is one of the elements); the old keys stay in
  `nusmp-labyrinth-mega-system-client.toml` and are the fallback until the window is first placed, so every saved position still applies; Reset forgets them.
  `GuiScale`'s five screen handlers and its layer hook were rewritten to go through the shared transform (same result when nothing was moved).
* KEPT: GuiScale and its config, its sliders and entries; the Djinn `anchor` option, the Hero HUD x / y / scale, the curse bar x / y, the boss bar scale as the defaults;
  the System edit screen; every enum, id and registry order.

## Cost (rule 11)

Per frame, per visible movable element: one `HashMap` lookup by a constant id, a few field writes, and (only for a moved element) one pose push; nothing is allocated, no
string is built, no config is read (the layout is parsed into the elements when the file loads or changes). Hidden elements cost one lookup. Edit mode draws only while its
screen is open (worst case about 25 outlines of a few dozen fills each). No packets, no server work: nothing is sent or decided on a server (`ServerSafetyTest`).
Pinned by `PerfSourceScanTest.theGuiMoveFrameCostIsOneLookupPerElementAndNoAllocation`.

## Tests

`GuiMoveMathTest` (anchors, clamp, resize stability, snapping, scale limits and the order of the sizes, mouse transform, migration equals the old System placement),
`GuiMoveLayoutTest`, `GuiMoveRegistryTest` (names and lang keys, every custom GUI class registered or fixed with a reason, wiring, no double GUI Size, config name),
`ScreenBlurGuardTest` (edit screen is flat), `ServerSafetyTest`, `ConfigFileNamesTest`, `PerfSourceScanTest`. No `ui.gui_move` self-test: the pure classes live under
`client/` (the brief), and common code may not name them (rule 9 guard), so a server self-test would need an allow-list exception for no real gain.

Preview: `python3 -B tools/gui/preview_gui_move.py` writes `docs/previews/gui_move/` (edit mode, Djinn dragged with the snap guide, the moved Djinn in play, size 150%,
reset, and one saved layout on three window sizes). The Djinn panel and System window are drawn by the game's own code and every rectangle comes from `GuiMoveMath`; the
surface, the world and the edit screen's outlines are stand-ins.

## NOT verified (nothing was run in a game)

* Nothing compiled against the real Minecraft / NeoForge jars: only the stub type-check (only the known `KeyMapping is abstract` stub artefacts remain) and the pure
  JUnit shim. The pose math of `GuiMove.begin`, the `RenderGuiLayerEvent` cancel for hidden layers, `Screen.removed`, `font.split`, the client command and `ModConfigEvent`
  use are unseen in a real client. Whether a hidden layer's `Post` event is skipped as assumed is unverified.
* Moved container screens (item slots, hover, drag-and-drop, 3D entity previews of the avatar screens) rely on the same mouse remap as GUI Size; JEI and other mods'
  side panels do not follow a moved screen.
* LAYER elements (quest tracker, relic keys, song meter, floor report, page toast) are moved from the rectangle of the previous frame: a layer whose size changes lags one
  frame, and the first frame after the first appearance is drawn unmoved. A hidden layer does not run, so a page toast queued while hidden waits.
* The edit screen's behaviour with real fonts (hint wrapping on small windows) and the F9 default key conflicting with another mod are unseen.
* The System window under edit mode is redrawn by `SystemOverlay` while the screen is open (three layers were taught `GuiMove.editing()`); other HUD layers that hide
  themselves behind a screen show only their outline.
* The System window's migration is exact in the pure tests; the live read-through (store first, old keys as fallback) was not exercised against a real config file.
