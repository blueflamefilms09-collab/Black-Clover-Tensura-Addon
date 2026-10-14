# The Anime Multiverse GUI theme

The menus of the mod have two looks, picked by the client config `ui.theme` (file `config/nusmp-ui-client.toml`):

| value | look |
|---|---|
| `anime` (default) | sleek translucent glass panels with a thin glowing edge and corner brackets, Great-Sage analysis text ("Analysis complete." "Result: ..." "Notice: ..."), animated rotating rune circles, animated mastery bars, hover highlights |
| `classic` | the drawing the mod had before (the parchment book, the dark flat panels). The old code paths are kept as they were; the anime branch sits next to them |

Other options of the same file: `ui.motion` (off: circles and bars stand still), `ui.typing` (off: the analysis text appears at once), `ui.pageToast` (off: no page-unlock panel).
Nothing about behaviour changes between the looks: every rectangle that is clickable keeps its place, every handler is the old one; only drawing and layout differ.

## What is restyled

| Screen / element | File | Anime look |
|---|---|---|
| Codex (the book) | `client/ui/CodexScreen`, `CodexPages` (colours only), skin in `theme/CodexSkin` | glass pages with a faint circle turning behind the text, glowing frame, chip tabs, summoning circle on the floor under the player figure, Sage line in the footer, glass list rows with hover |
| Multiverse Status | `client/multiverse/MultiverseStatusScreen` | analysis panels, bars under Grimoire (mastery), Spirit Lord (trust) and Mana (corruption), typed Sage readout, a circle behind everything. The compact panel inside Tensura's menu (`drawCompact`) and the "Four Kingdoms" label (`MultiverseStatusClient`) follow |
| Four Kingdoms | `client/multiverse/FourKingdomsScreen`, `FourKingdomsSlot` | glass frame and pages, a circle behind each page, a mastery bar in the header, a cost bar per page; the slot in Tensura's magic screen gets a spinning gold circle on hover |
| Grimoire Slot | `client/grimoire/GrimoireSlotScreen` | glass window, a gold circle behind the grimoire slot, glowing slot frames, hover highlight, a Sage readout panel under the window |
| Tower Wizard questionnaire | `client/wizard/QuestionnaireScreen` | analysis panel, progress bar, circle, Sage hint |
| Mastery page unlock | `client/ui/theme/PageUnlockOverlay` | a panel at the top of the screen: a circle that builds up, the page's name, "Notice: a new page has opened.", the description, "Result: cost ..." and a mastery bar. Fed from the synced status (see below); several unlocks queue |
| Ability selection window | `client/ui/theme/TensuraScreenTheme` | Tensura draws this window (`AbilitySelectionScreen`), so it is an overlay: corner brackets round Tensura's panel, a faint circle, and below it a Sage readout of the selected ability (name, "Analysis complete.", "Result: mastery 62 / 100", a mastery bar; a grimoire also "Pages open: 3/12"). `SubAbilityModeSelectionScreen` and `AbilityCategoriesScreen` get the circle, brackets and a one-line notice |
| Relic tooltip, relic key line | `client/relics/RelicTooltip` (`onColor`, header hairline, "Notice: page 2 of 4. Scroll to turn."), `RelicActKeys.renderHud` | glass tooltip with a grade-coloured border; the key hint line is a glass chip |
| Buttons of those screens | `theme/ThemedButton` | vanilla `Button` (same clicks and narration) drawn as a glass chip with a hover glow in the anime look; vanilla drawing in the classic look |

The mastery notification does not touch the server. `MasteryPages.tryUnlock` still sends its chat lines and marks the status dirty; the overlay watches the client's copy of that status (`MultiverseStatusClient.status()`, grimoire `PageList`), and `PageUnlockTracker` announces a page that was not open in the previous status of the same grimoire. The first status after joining (or after taking a different grimoire) is only a baseline, so nothing is announced on login. In the classic look the chat message remains the whole notification.

## The toolkit (`client/ui/theme`)

All of it is additive; `UiDraw` keeps every old method and constant and gets a theme section (`ink()`, `inkDim()`, `goldInk()`, `rule()`, `warn()`, `accent()`, `anime()`); `UiDraw.header` draws the anime heading when the theme is anime.

| Class | Job |
|---|---|
| `UiThemeOptions` | the client config spec (`ui.theme`, `ui.motion`, `ui.typing`, `ui.pageToast`) |
| `UiTheme` | palette, `anime()`, `motion()`, the clock `now()` (ticks, continuous, frozen when motion is off) and `realNow()` (never frozen) |
| `UiMath` | pure: easing, pulse, rotation angle, typewriter count, colour mixing, lang slugs (unit tested) |
| `UiPanel` | `panel` (halo, translucent gradient body, sheen, inner line, glowing edge, corner brackets), `panelScan` (adds a scan sweep), `well`, `brackets`, `sweep`, `heading`, `hover`, `chip`, `scrim` |
| `UiBar` | animated bar: eased value, gradient fill, travelling shimmer, glowing leading edge, ten ticks; instance per bar, static `draw` for a fixed value |
| `MagicCircle` | the rune circle painter, see below |
| `SageBlock` | the Great-Sage text block: header, wrapped body, result; typewriter with a blinking cursor; `set` re-wraps only when the text or width changed |
| `SageLayout` | pure: word wrap, typewriter bookkeeping, `clamp` to N lines (unit tested) |
| `ThemedButton` | the themed vanilla button |
| `CodexSkin` | the Codex's anime drawing |
| `PageUnlockOverlay`, `ToastTimeline`, `PageUnlockTracker` | the page-unlock panel, its pure timeline and its pure unlock detector |
| `TensuraScreenTheme` | the overlay on Tensura's ability windows |
| `UiThemeClient` | one call that registers the overlay, the Tensura hook and the relic tooltip colours |
| `UiLang` | lang keys of the Sage voice |

### The magic circle

`MagicCircle.draw(g, cx, cy, radius, time, rgb, alpha[, style, build])`. Six prebuilt white-on-transparent layers (made by `tools/gen_ui_theme_textures.py`) rotate about one centre at different speeds and directions, tinted by one colour, under an additive pulsing glow:

```
glow     additive bloom, breathes
outer    double ring, 72 tick marks, a band of 24 rune glyphs         0.30 deg/tick
sweep    two broken arcs (one outside the ring, one just inside)     +2.2 and -1.5 deg/tick
star     ring with a hexagram and vertex nodes                       -0.55
poly     octagon, eight-pointed star, dashed ring, cross             +0.85
inner    arc dashes, diamonds, centre dot                            -1.30
```

Cheap: at most eight textured quads per circle, one reused rotation quaternion, no allocation of its own; each layer comes in three size tiers (256 / 128 / 64 px, fatter strokes in the small ones) and the nearest tier is picked from the drawn size, with linear filtering (`.mcmeta` `blur`), so rotating lines do not shimmer. The angle is a function of the clock only (`UiMath.angle(time, degPerTick, start)`), so it is smooth at any frame rate; callers pass `ticks + partial` or `UiTheme.now()`. `build` (0..1) makes the layers appear one after another (used by the toast). Styles `STANDARD`, `CALM` (for backgrounds behind text), `FAST`. A flat floor circle is a squashed pose (`CodexSkin.floorCircle`).

### The Great Sage voice

Terse and analytical, like Tensura's Great Sage / Raphael: a header ("Analysis complete.", "Analyzing..."), a body, a result ("Result: ...") and now and then "Notice: ..." or "Warning: ...". All of it is lang keys (`nusmp.ui.*`, see the lang fragment); code never writes the lines literally. A Codex page gets its footer line from `nusmp.ui.codex.sage.<page title as a slug>` (`UiMath.slug`), and a page without its own key gets `nusmp.ui.codex.sage.default`, so a page registered later needs no change in the code.

## Writing a screen in the theme

1. Draw with `UiPanel` / `UiBar` / `MagicCircle` / `SageBlock`; read colours from `UiTheme` (or `UiDraw.ink()` etc. if the code also has a classic path).
2. Keep the classic drawing: `if (UiTheme.anime()) { ...new...; return; }` at the top of the drawing method, the old lines below untouched.
3. Buttons: `new ThemedButton(x, y, w, h, Component, onPress)` instead of `Button.builder(...)`.
4. Clock: `UiTheme.now()` for animation (it respects `ui.motion`); timers that must run out use `UiTheme.realNow()`.
5. Text with alpha below 4/255 is drawn opaque by the vanilla font: skip text that faded out (`SageBlock` does).

## Textures and previews

* `python3 tools/gen_ui_theme_textures.py [--sheet out.png]` writes `assets/nusmp/textures/gui/theme/circle_{outer,star,poly,inner,arcs}{,_128,_64}.png`, `circle_glow.png` and their `.mcmeta` (idempotent; the runes come from a fixed seed, an invented script).
* `python3 tools/ui_preview/preview_ui.py OUT_DIR [--scale N]` renders mock-ups of every restyled screen (Codex pages, Multiverse Status, Four Kingdoms, Grimoire Slot with and without a book, questionnaire, the page-unlock panel at four moments, the overlay on Tensura's ability window, a relic tooltip and key line) with PIL. It is a re-implementation of the draw calls and the layout numbers, over a fake world backdrop, with a monospace stand-in font; it is not the game, and the Java is the truth.

## Tests

`src/test/java/.../client/ui/theme`: `UiMathTest` (easing, pulse, angles stay in one turn, frame-rate independence of the easing, typewriter bounds, colour mixing, slugs, theme parsing), `SageLayoutTest` (wrapping, cutting a long word, typewriter bookkeeping, cursor line), `ToastTimelineTest` (the toast's timeline, and `PageUnlockTracker`: the first snapshot is a baseline, a new page is announced once and in order, another book or a logout starts over).

## Known limits

* Nothing here has been run in the game. The numbers were checked with the mock-ups only.
* The overlay on Tensura's windows reads the panel's rectangle from Tensura's protected statics (`SimpleScreen.guiLeft`...) by reflection; if that fails the brackets are skipped and the readout sits under the screen's centre. It is drawn in `ScreenEvent.Render.Post`, over Tensura's own drawing, so the circle is kept very faint.
* A page-turn leaf in the anime Codex is translucent over the page under it (the old leaf was opaque paper), so a turning page looks a little denser while it moves.
* The vanilla font has no italic and few symbols: the Sage lines use plain letters only.
