# GUI Size

Players can change the size of the mod's GUIs. Client only, nothing is sent to the server.

## Changing it in game

* **Multiverse status screen**: the "GUI Size" button, bottom left.
* **Key**: "GUI Size" under the Multiverse of Anime controls (F8 by default), opens the screen while playing.
* **Mod list**: Mods > the mod > Config opens the same screen ("More options" there leads to the plain config editor, including the other client files).

The screen has a global slider (50% to 200%, steps of 5%), "Fit to window", "HUD follows global", and a list with one slider per GUI (Auto = follow the
global value). Values apply at once and are saved when the screen closes. The preview on the right (shown when the window is wide enough) draws the
box of the selected GUI at the asked and at the shown size inside a miniature window.

## Config: `nusmp-gui-client.toml`

| key | meaning |
|---|---|
| `guiScale` | global size, 0.5 to 2.0, default 1.0 |
| `fitToWindow` | never draw a screen bigger than the window (default true) |
| `hudFollowsGlobal` | HUD elements follow the global size (default true); off = they only change with their own entry |
| `[overrides] <id>` | size of one GUI, 0 = follow the global value |
| `[anchors] <id>` | the point of the screen a layer-hooked HUD element grows from (TOP_LEFT ... BOTTOM_RIGHT) |

## How it is applied (`client/ui/GuiScale`, pure rules in `client/ui/scale/GuiScaleMath`)

* **Screens**: `ScreenEvent.Render.Pre` (highest priority) draws the screen itself inside a pose scaled around the window centre, with the mouse position
  carried into the screen's own coordinates, then cancels the plain draw. `MouseButtonPressed/Released`, `MouseDragged` and `MouseScrolled` (`.Pre`)
  call the screen with transformed coordinates (drag distances are divided by the scale) and cancel the plain event, so clicks, hovers, tooltips,
  container slots and drags line up. Only classes under `com.newuniverse.nusmp` that are listed in `GuiScaleRegistry` are touched; vanilla and other
  mods' screens never are. The System edit screen and the GUI Size screen are never scaled.
* **Fit to window**: the scale is held to `min(guiW / designW, guiH / designH)` (never below 1), rounded down to 5%. `designW/H` is the panel size the layout
  was built for (registry), 0 = the whole window (such a screen can shrink but not grow), -1 = the container's own image size.
* **Text**: scales are rounded to 5% steps so the bitmap font is not drawn at awkward fractions.
* **HUD layers** (`quest_tracker`, `arts_bar`, `relic_keys`, `song_meter`, `floor_card`, `page_toast`): `RenderGuiLayerEvent.Pre/Post` scales the layer around
  its anchor. **HUD elements that place themselves** call `GuiScale.hud(id)` / `GuiScale.begin(...)` at their own anchor: `system_hud` (times its own
  size, same place), `hero_hud` (times its own size, same corner), `djinn_panel` (grows from the bottom edge of where the Djinn `anchor` option put it),
  `boss_bar` (from the top centre).

## Adding a new GUI

Add one line to `GuiScaleRegistry` (screen: class name and design size; HUD: layer path and anchor, or `site` and call `GuiScale.begin` in its drawing),
and one lang key `nusmp.gui_scale.gui.<id>` (the English name is also the fallback). The config entry, the list row and the preview appear by themselves.
`GuiScaleTest` checks that every listed class and layer exists in the sources.

## Not covered

Full-screen effects (Djinn screen fx, curse overlay, route-unlocked flash) are effects, not windows, and stay as they are. Other mods' screens, the Tensura
status and magic screens (where a Multiverse panel is drawn) are not touched. A screen that fills the window and draws its own full-window dim is shrunk
with its dim, so a margin shows the world un-dimmed at sizes below 100%.
