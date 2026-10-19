# The active Djinn panel (themed and animated)

The Djinn HUD (shown while a Djinn is out, and for the lockout and cooldown that follow) used to be one small bar above the hotbar. It is now a
panel that looks different for every Djinn and moves. The old bar is kept as the `CLASSIC` style.

## NEW / REPLACED / KEPT

* **NEW**: `client/relics/djinn/gui/**` (the panel), `tools/gui/**` (preview), `tools/lang/djinn_gui.json`, `docs/previews/djinn_gui/**`,
  the client config `nusmp-djinn-gui-client.toml` (`style`, `anchor`), tests `GuiPureTest`, `GuiPainterTest`.
* **REPLACED (as the default)**: the small bar drawn by `DjinnScreenFx.render`. Its code is moved unchanged into `DjinnScreenFx.classicBar`.
* **KEPT**: the classic bar (config `style = "CLASSIC"`), the screen vignette, the Extreme Magic fringe, the flash, the field-of-view and camera shake,
  all keys (J, K, H), all packets, `DjinnClientState`, every Djinn table value. Nothing under `relics/djinn/**` was touched.

## How it is built

Everything the panel draws is decided by pure classes (no Minecraft types) and painted through a three-method `Canvas` (`fill`, `text`,
`textWidth`). The game's `GfxCanvas` feeds a `GuiGraphics`; the preview tool and the tests feed an image or a recorder. So the preview shows the real
code's output, not a copy.

| class | job |
|---|---|
| `DjinnGui` (game) | turns `DjinnClientState.Snap` into a reusable `GuiState`, caches the `GuiTheme` per Djinn, tracks the summon intro, reads the config |
| `DjinnGuiPainter` | orchestrates the layers, text and pips; the one entry point (`paint`) |
| `GuiTheme` / `GuiSpec` | per Djinn data copied from its `Djinn` row: name, colour, glow, `Affinity`, `Look`; palette, motif, tuning, emblem |
| `GuiPalette` / `GuiColor` | the colours, derived from (colour, glow, affinity tint, dark); text and body colours are lifted to stay readable |
| `GuiMotif` | the frame theme of each `Affinity` (a fallback for any unknown or null one) |
| `GuiAccent` | the signature shape behind the emblem; hand tuned rows for the 12 first Djinn, a hash-derived pick from the affinity's pool for any other |
| `Emblem` | the silhouette rasterised once from the `Look` (horns, wings in four beats, ears) into pixel runs |
| `GuiMedallion`, `GuiBackdrop`, `GuiParticles`, `GuiAccents`, `GuiBars` | the layers |
| `GuiAnim` | curves, deterministic noise, phase to look/energy, perimeter walk |
| `GuiLayout` | all positions; panel anchor on screen |
| `DjinnGuiConfig`, `GuiStyle` | client options |

No `switch` on a `Djinn` constant exists anywhere in the panel. A Djinn appended to the enum later is drawn from its own row: its colours give the
palette, its affinity the frame, its look the emblem, its id (a stable hash) the accent, corner ornament, rune glyphs and turning direction.
`GuiPainterTest` draws every Djinn of the table plus 48 hand-made specs (every affinity with many looks, a long title, a missing affinity and look).

## What it shows

Panel 206 x 68 GUI pixels, bottom centre above the status icons (config `anchor`: `BOTTOM_CENTER`, `BOTTOM_LEFT`, `BOTTOM_RIGHT`).

* **Medallion** (left, 60 px): the Djinn's emblem, a bust with its horns (crescent, ram, stag, spike, crown), ears and wings (feather, membrane, blade,
  veil, fire), a live tail (whip, flame, blade, tendril) and a live halo (ring, runes, spokes), all from `Djinn.Look`. A breathing glow, a ring of
  turning runes (its own glyph set), eyes that blink, a shimmer sweeping over the silhouette.
* **Phases**: the emblem shows what the phase gives: sealed outline (0), head and horns (Lesser Summon), lit horns (Half Equip), wings, tail, halo
  beating and glowing (Full Equip, Extreme Magic). Changes ease over 14 ticks and send a ring out of the medallion.
* **Full Equip is a distinct state**: wings beat at full strength, the second rune ring turns the other way, a light runs round the border, the rim and
  border go double and bright, more particles, the pips sparkle. **Timer sweep**: a ring of 72 dots round the medallion lights for the time left; in
  the Half Equip a bright dot chases round; in an Extreme Magic the ring fills towards white as it charges and the border flickers; a strike flashes
  the panel.
* **Summon intro** (28 ticks when the panel first shows or the Djinn changes): the panel unfolds from its middle line, a flash and a ring of light
  leave the medallion, the emblem is scanned in from the top.
* **Bars** (restyled per magic): Magoi, assimilation, timer (Lesser, Full, Extreme charge), thin Maso lockout and Extreme cooldown bars, a footer with
  Trust and the recovery times. Each magic gives the bars their own surface and head (below).
* **Dark Djinn**: black-violet panel, gold rim, the Djinn's colour kept in the glow; "Dark" before the name.

### Per affinity

| affinity | frame | background | border | particles | bar surface |
|---|---|---|---|---|---|
| Heat | embers | heat haze rising from the bottom | flame tongues along the bottom edge | rising embers | flame tongues, a spark over the head |
| Lightning | arcs | flashes, faint grid | jagged arcs flicker on top and bottom | bolts, jittering sparks | white pulse, jagged head |
| Water | ripple | two moving waves | rippling bottom edge | bubbles | wavy surface |
| Wind | streaks | swept streaks crossing | chevrons run along the border | streaks and leaves | moving diagonal stripes |
| Life | vines | green growth at the bottom | a vine along the bottom and right with leaves | falling petals | leaf notches |
| Strength | cracks | stone speckle, cracks glowing with a slow heavy beat | stone border with broken gaps | falling chips, dust | cracked gaps |
| Sound | rings | rings leaving the medallion | nodes pulsing in turn | rising notes | rhythm segments with a travelling beat |
| Light | rays | rotating rays | gold filigree, twinkling stars | twinkles | light sweep, star head |

### Per Djinn (hand tuned: accent, tempo, corner ornament, rune set, spin)

Baal blood ring (turning, beads); Amon sun arch of rays; Vinea two water ribbons; Focalor two counter-turning cyclone rings; Zagan crown of leaves;
Zepar circling crystal shards; Furfur sound arcs opening to both sides; Marchosias a mane of flame; Sitri a dawn fan of rays; Bifron flickering storm
arcs; Paimon a still full moon; Vassago a breathing reaper crescent. The colours, glow, horns, wings, tail and halo come from each Djinn's row, so
no two of the 12 (or of the 48 test specs) draw the same picture. Other Djinn pick two or three accents per affinity by a hash of their id.

## Cost

Allocation free per frame (state, canvas, painter and pen are reused; strings are cached per language, per timer second). A Full Equip frame is
about 1000 to 1700 rectangles, idle about 1000 to 1400 (measured over the 12 Djinn; tested under 2600); animation is a pure function of `gameTime + partialTick`.

## Lang

`tools/lang/djinn_gui.json` (flat fragment, nine keys). Read with a fallback (`I18n.exists` else the English text), so the panel works before the
fragment is merged into `en_us.json`.

## Preview

`python3 -B tools/gui/preview_djinn_gui.py` compiles `tools/gui/DjinnGuiPreview.java` against the pure classes and writes `docs/previews/djinn_gui/`:
`roster_full.png`, `roster_dark_full.png`, `affinities.png`, `stand_ins.png` (24 not yet in the table, invented colours), `phases_<id>.png`,
`intro_<id>.png`, and `anim_<id>.gif` for four Djinn (`--all-gifs` for all). Text metrics in the preview are approximate.

## Not verified in the game

Nothing here was run in Minecraft. Unverified: how the panel sits with other mods' HUDs and Tensura's, the real font widths (long titles are hidden if
they do not fit), `GuiGraphics.fill` cost at 1000 rectangles (expected small), the client config registration (`ModLoadingContext` inside
`DjinnClient.init`, guarded by a try/catch so a failure only means default options), and the final merged `en_us`.
