# Dungeon Map GUI fix (1.105.0, builder map-gui)

Owner report: "fix the dungeon map item, the GUI is bugged" (screenshot: the whole Dungeon Map screen blurred, the HUD showing round a smaller dark rectangle).

## The real defects (line numbers are of the 1.104.0 file `client/elite/hub/DungeonMapScreen.java`)

1. **The blur (the reported bug), `render`, lines 71-110.** `render` drew the dark backdrop (line 73), every door and room (75-89), the markers, the player, the header, the legend (100-107), and only THEN called
   `super.render(...)` (line 109). Vanilla 1.21 `Screen.render` starts with `renderBackground`, which blurs the frame buffer: everything the screen had just drawn was blurred, text included. The class had no
   `renderBackground` override, so the blur was always applied last. Same family as the questionnaire (1.15.0) and Avatar menu (1.51.0) blurs. `ScreenBlurGuardTest` did not catch it because it only looked
   for a literal `renderBackground(` call inside `render`, and the Dungeon Map never calls it by hand: vanilla's `super.render` does.
2. **The backdrop did not cover the window under the GUI size option (1.95.0), line 73.** `g.fill(0, 0, width, height, 0xF00C0A10)` is drawn inside the pose that `GuiScale` scales round the window's middle,
   so at a size below 100% (about 85% in the screenshot) the dark rectangle is smaller than the window and the HUD, chat and toast show round it. Its alpha (`F0`) also let the HUD show through.
3. **Resize reset the view, `init`, lines 32-39.** `init()` runs again on every window resize and re-fitted the floor, so a player's pan and zoom were lost.
4. **No clip.** Rooms, doors and markers were drawn without a scissor, so a zoomed map ran over the header, the legend and the screen edges.
5. **No limits.** Pan and zoom had a hard-coded zoom range only (0.2 to 12.0, lines 38 and 156; a big floor could not even be fitted at 0.2), and the pan was unbounded: the floor could be dragged completely out of sight with no way back.
6. **Legend (lines 112-123).** One line that ran off the right edge of narrow windows, drawn with the muted grey, no swatch outline, and sharing the bottom 14 pixels with nothing else (it sat on top of the map). No hint text at the bottom, only a top-right hint that collided with the header on narrow windows.
7. **Drag started anywhere.** A drag that began on the header or legend moved the map.

Checked and found NOT a defect: the mouse is already divided by the GUI scale. `GuiScale.onPress / onRelease / onDrag / onScroll` (`client/ui/GuiScale.java`) convert the mouse position, the drag distance and the
scroll position into the screen's own coordinates before calling the screen, so the screen's own maths is correct; `onRenderPre` pushes and pops the pose in a try/finally, so no matrix is left pushed. The text colours already
had full alpha (`HubGui.GOLD/TEXT/MUTED` are `0xFF......`), the "1 scattered" in the screenshot is the blurred "1 explored" (the number of rooms the player has stood in), and showing the whole layout with the unexplored rooms dimmed is
the documented design of the item (docs/elite/hub.md), so it was kept. The "Goal Reached!" toast is drawn by the game over every screen (it is an advancement toast in the top-right corner) and cannot be moved by a screen; the legend/hotbar
overlap is gone because the screen now covers the window opaquely.

## What was done

* `DungeonMapScreen.renderBackground` is overridden: one flat, near-opaque (`0xFA`) backdrop, no `super` call, so nothing blurs; its rectangle comes from `DungeonMapMath.backdrop(width, height, GuiScale.screenScale(this))`
  and covers the whole window at any GUI size. `render` calls `super.render` FIRST (backdrop once, Close button), then draws the map clipped with `enableScissor` (in a try/finally), then the bars.
* Title bar (floor, road, counts, the item's note) with a Close button; bottom bar with the legend (colour swatches with an outline, bright text, wraps onto more rows instead of running off the window) and a hint row.
* Pan is held to the floor plus 8 blocks, zoom to [min(0.2, fit), 16] (a huge floor can always be fitted), the wheel zooms about the pointer, dragging only starts inside the map frame.
* Keys: **C** centre on me (zooms in to at least 2 px/block), **F** or Home fit the whole floor, **+** / **-** zoom, **Esc** closes (vanilla).
* The view survives window resizes. Rooms outside the frame are culled.
* All of the numbers are in the new pure class `client/elite/hub/DungeonMapMath.java`.

## NEW / REPLACED / KEPT

| | |
|---|---|
| NEW | `DungeonMapMath` (pure maths), `DungeonMapSelfTests` (self-test `ui.dungeon_map`), title bar + Close button, legend wrapping + swatch outline + start/exit entries, hint row, keys C / F / + / -, zoom and pan limits, scissor clip, culling, `tools/lang/map_gui.json` (keys `gui.nusmp.dungeon_map.*`, all with English fallbacks in code), `DungeonMapMathTest`, two new tests in `ScreenBlurGuardTest`, this doc |
| REPLACED | `DungeonMapScreen.render` / `init` / input handlers (the blurred draw order, the fixed backdrop, the unbounded camera); the private `colour` / `dim` bodies moved into `DungeonMapMath` (`DungeonMapScreen.colour(int)` still exists and delegates) |
| KEPT | The item (`DungeonMapItem`), `HubClient.map`, the `MapSync` packet and `MapView` (data unchanged), the registry id `dungeon_map` in `GuiScaleRegistry`, room colours, fog of war (dim unexplored rooms), markers S / E, the player marker with facing line, drag and wheel zoom, drawing nothing about room contents, not a pause screen |

## Scan of every other screen

`ScreenBlurGuardTest.noScreenOfTheModDrawsBeforeTheBlur` scans every class of the mod (anywhere under `src/main/java`) that extends `Screen`, `AbstractContainerScreen` or a `*Screen`: its `render` may not draw (use its
`GuiGraphics` parameter) before `super.render(...)`, unless the class overrides `renderBackground` without calling the blurring vanilla one. Result: the only screen with the problem was the Dungeon Map. The 19 others
either override `renderBackground` flat / transparent (Codex, Compat Codex, GUI size, Questionnaire (calls super, but draws only after it), Shop, Weapon book, System edit / stats, Four Kingdoms, Multiverse status, Avatar action), draw in `renderBg`
(all the container screens), or never call `super.render` (Soul Forge). The scan has a self-check (`theScanCatchesTheDungeonMapMistake`) that feeds it the broken shape and the legal shapes, and
`theDungeonMapScreenIsFlatAndDrawsAfterSuper` pins the Dungeon Map by name.

## Tests

* `ScreenBlurGuardTest` (4 tests), `DungeonMapMathTest` (14 tests: frame layout, fit, screen/world inverse, drag direction, pan clamp, zoom about pointer and limits, huge floors, NaN/empty, centre on me, GuiScale mouse conversion round trip, backdrop covering the window at scales 0.5 to 2.0, legend wrapping, culling, colours): run in the JUnit shim, all pass.
* Self-test `ui.dungeon_map` (CI server smoke test): builds a synthetic 31-room floor through `MapView.build` and runs the fit, pan, zoom and centre maths at four window sizes, every room kind's colour, and the backdrop at every GUI size. Not run here (no server in this session).

## What could NOT be verified

* Nothing was seen in a game. The blur is explained from the draw order of the source and from vanilla 1.21.1's `Screen.render` (renderBackground first), not from a render; the new draw order (flat `renderBackground`, then content) is what the mod's working screens such as the
  Shop and the Weapon book use.
* Whether `GuiGraphics.enableScissor` composes with the `GuiScale` pose as expected is from the 1.21.1 source (the scissor rectangle is transformed by the current pose), the same call the questionnaire and other mod screens make; not seen.
* The exact look of the title bar, Close button and wrapped legend at odd window sizes and GUI scales (layout is tested as numbers only).
* The "Goal Reached!" toast overlap is not something the screen can change.
* The type-check was done against the API stubs only (1813 known stub errors in the closure, 0 in the touched files, one negative control caught).
