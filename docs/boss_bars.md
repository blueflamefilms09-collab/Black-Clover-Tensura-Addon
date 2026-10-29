# Custom boss bars (release 1.106.0 builder "boss-bars")

Owner request: "also add custom boss bars to the dungeon bosses". Written without a game: nothing here was run in Minecraft. Previews are drawn by a python
mock-up from the same textures and json files (`docs/previews/boss_bars/`, see the end).

## NEW / REPLACED / KEPT

| Part | Status |
|---|---|
| Custom client-drawn bar (`client/gui/bossbar/Custom*`, `BarStyles`), 35 styles, atlas art, emblems, fill effects | NEW |
| Style data `assets/nusmp/boss_bars/*.json` + strict reader `boss/bars/BarStyle`, resolver `BarStyleResolver` | NEW |
| `BossBarSource` (one interface) + adapters `BossBarSources` (Zogratis family and arcs, Labyrinth Guardian = Decennial / D&D / pocket, Spirit King, old Zagred, generic entity) | NEW |
| `BossBarState` + `BossBarStatePayload` (+ `Preview`), `BossBarHub` (diff-based, 5/s), `/multiverse bossbar`, `/nusmp bossbar`, self-test `boss.bars` | NEW |
| Config keys `bossbar.custom / scale / showPhaseMarkers / showHp / portrait / showDps` | NEW (appended to `nusmp-bossbar-client.toml`) |
| The animated bar of 1.25.0 (`ModBossBarOverlay`, `BossBarPainter`, `BossBarAnim`, `BossBarSync`, `Style` / `Clear` packets, `BossBarThemes`) | KEPT, now the fallback: it draws every bar that has no custom style, and everything when `bossbar.custom=false` |
| Vanilla boss bar look for other mods' and vanilla bosses | KEPT (drawn by the old overlay's `paintVanilla`, inside the custom stack when one is shown) |
| Nothing was replaced or removed. | |

## How it works

* **Which bars**: every boss that owns a `ServerBossEvent` and calls `BossBarHub.track(entity, bar)` (one line, listed below). The vanilla boss event
  stays what makes a bar exist and still carries the name, the colour and the health; the custom bar reads the fill from it.
* **Server** (`boss/bars/BossBarHub`): every 4th tick per bar (5/s, bars spread over the four ticks) it asks the boss's `BossBarSource` for a
  `BossBarState`, sends it to everyone who watches the bar **only if it differs** (`BossBarState.differs`: phase, notches, shield, flags, adds,
  fighters, names, a lair action that appeared / changed / moved by more than 3 ticks, a wind-up that moved 5 %), sends the current state at once to a
  player who starts watching, a final state with `DEAD` when the boss dies (`LivingDeathEvent`) and a `BossBarNet.Clear` to everyone told when the boss is
  removed. The hub does nothing while no bar is tracked. Probe section `s.boss.bars_hub`.
* **`BossBarSource`** (pure interface, all defaults): `styleId, name, title, phase, phaseCount, phaseThresholds, phaseName, shield, invulnerable, enraged,
  lairId, lairTicks, adds, castName, castProgress, fighters`. `BossBarSources.of(entity)`: the entity itself if it implements the interface, else the
  adapter of its family, else a generic one (style of its type, shield from absorption, fighters = players within 48 blocks).
* **Client**: `CustomBarClient` keeps a `CustomBarState` per bar id; `CustomBarOverlay` listens to `RenderGuiLayerEvent.Pre` of vanilla's boss layer at
  priority HIGH (before the animated overlay). When at least one visible bar has a loaded custom style (or a bar is dying or previewed) it lays out
  all bars in one stack, draws custom ones with `CustomBarPainter`, hands every other bar to `ModBossBarOverlay.drawFallback`, and cancels the layer.
  Otherwise it does nothing. If drawing throws it disables itself for the session and the old overlay takes over. The stack is scaled by the GUI Size
  entry `boss_bar` (`GuiScale.begin`, from the top centre) and by `bossbar.scale`; three or more custom bars are drawn compact; the stack stops at 45 %
  of the screen height. No `Screen` is involved, so the blur guard conventions do not apply (the `ScreenBlurGuardTest` still passes).
* **Death**: the final `DEAD` state keeps the entry; when the vanilla bar disappears the custom bar stays 36 ticks and shatters (twelve strips fly
  apart and fall; styles with `death: dissolve` fade up instead).

## What each bar shows

Plate: boss name, title line (Decennial title, arc character title, devil), phase roman numeral at the left, health percent at the right (the
number only; with `bossbar.showHp` a small current / maximum line appears under it while the boss is loaded). Frame: the style's atlas (ornaments, name
banner, socket) and the glow layer, additive, tinted by the style and breathing; red and faster when enraged; ice-blue and pulsing while invulnerable;
red pulse below 25 % health; a flash at every phase change and on entrance. Fill: gradient, scrolling effect tile of the style (drips, embers, souls, ...),
a slower white shimmer, bright leading edge; the **damage-lag chip** (white to glow colour, holds 12 ticks, drains at 7 % of the gap per tick, instant
on a heal); **phase notches** at the thresholds of the boss (with triangles in the bezel, dimmed once passed); the **shield** band (absorption as
share of max health); a green wash while healing; **invulnerable**: ice-blue veil, chains across the fill, "Invulnerable" replaces the title.
Under the bar: a thin **wind-up bar** with the move's name while the boss casts; the **lair action** warning during its last 5 seconds (name flashes,
ring of twelve dots counts down); "Phase II: name", "Adds 3", "3 fighters", optional damage per second. Socket: the **live boss** (head and shoulders
of the entity, clipped to the socket, `bossbar.portrait`) with the style's emblem as a small badge, or the emblem alone when the boss is not loaded.
Entrance: slide / rise / fade / flash per style.

## Where each boss gets its numbers

| Boss | Style | Phases (notches) | Other |
|---|---|---|---|
| 200 Decennial bosses (Labyrinth Guardian with a `DecennialLayer`) | `BarStyleResolver.forDecennial`: archetype, hall theme, creature, side | 2-4 from the profile's `at` shares | name + title from the profile, phase name, invulnerable during the 2 s shift (`controller.invulnerable`), next lair action (new read-only `DecennialBossController.nextLair()`), adds alive |
| D&D guard bosses / floor wardens (Guardian without a profile) | `warden` | I / II at 50 % (III if mythic) | form names of the D&D profile |
| Pocket mega-bosses (tag `nusmp_tomb_boss`) | `pocket_tomb` | as D&D | |
| Dante / Vanica / Zenon / Devil Avatar | `zogratis` | 5 stages of the power curve, notches 85 / 65 / 45 / 25 | invulnerable while refilling or transforming, enraged on rage / Gates, wind-up = the move label, fighters |
| Lucius, Goliath, Charybdis, God of Order | `lucius`, `goliath`, `charybdis`, `god_of_order` | as above | |
| Arc characters (`ArcBoss`) | by name (Fana, Patolli, Rades, Rhya = `midnight_sun`; Morris, Lucifero = `spade_kingdom`; Zagred) else by saga: Seven Deadly Sins = `seven_sins`, DanMachi (Minotaur, Udaeus, ...) = `danmachi`, Spade Raid, Elf Reincarnation / allies = `magic_knights` | as above | title = the character's epithet |
| Old Zagred (`ZagredBossEntity`) | `zagred` | 4 acts (75 / 50 / 25) | |

Not covered (still the animated bar): Julius's second bar and the Lucius channel bar (they do not call `track`).

## Styles (35, `BarStyleIds.ALL`; generated by `tools/gen_boss_bar_textures.py`)

Roster column = how many of the 200 Decennial bosses wear it.

| Style | Family | Emblem | Fill effect | Roster |
|---|---|---|---|---|
| holy (gold-white laurel, winged crest) | theme | wing | shimmer | 9 |
| evil (black iron, thorns, dripping red fill) | theme | thorn_crown | drip | 2 |
| undead (bone frame, green soulfire) | theme | skull | souls | 7 |
| fire | theme | flame | embers | 8 |
| ice | theme | snowflake | frost | 8 |
| storm | theme | bolt | sparks | 4 |
| void | theme | void_eye | static | 21 |
| forest | theme | leaf | leaves | 10 |
| water | theme | wave | wave | 6 |
| blood | theme | blood_drop | drip | 7 |
| clockwork | theme | gear | sparks | 10 |
| mirror | theme | mirror | glitch | 6 |
| underworld (Qliphoth root; abyss rift ring) | theme | root | veins | 0 (fallback of two halls) |
| qliphoth_gate (the seven gates) | theme | gate | runes | 7 |
| devil | theme | horns | embers | 12 |
| supreme_devil (crown and horns) | theme | crown_horns | embers | 8 |
| dragon | theme | dragon | embers | 18 |
| angel | theme | feather | shimmer | 17 |
| archon (halo, rays) | theme | halo | shimmer | 16 |
| lich (phylactery gem) | theme | lich | souls | 6 |
| fallen_king (broken crown) | theme | broken_crown | static | 4 |
| pocket_tomb (gold tomb frame, hieroglyph border) | theme | ankh | shimmer | pocket mega-bosses |
| plague | theme | plague | veins | 10 |
| zogratis | arc | triad | embers | the Zogratis family |
| midnight_sun | arc | eclipse | shimmer | |
| seven_sins | arc | sins (seven gems) | embers | |
| spade_kingdom | arc | spade | static | |
| magic_knights | arc | knight | shimmer | |
| danmachi | arc | labyrinth | embers | |
| lucius | arc | crown | shimmer | |
| god_of_order | boss | scales | shimmer | |
| goliath | boss | anvil | embers | 4 via archetype (stone colossus, tarrasque) |
| charybdis | boss | whirlpool | wave | |
| warden | boss | key | runes | D&D guard bosses |
| zagred | boss | rune | runes | |

## Files

* Art generator: `tools/gen_boss_bar_textures.py` (+ `tools/boss_bar_art/{lib,styles,motifs,emblems}.py`). Deterministic. `--json` rewrites the style
  json files from the table; the committed json is the source of truth after that (edit it by hand). Output: `textures/gui/boss_bar/frame_<id>.png`
  (+ `.mcmeta` blur; 512x268: back / frame / glow layers of 512x84 = the 256x42 widget at 2x), `emblems.png` (32x32 x 36), `fillfx.png` (13 tileable
  effects), `chains.png`; data `assets/nusmp/boss_bars/<id>.json` and `_index.json` (ids, emblem names, effect names, layout numbers).
* Pure Java (`boss/bars`): `BarStyleIds`, `BarStyle`, `BarStyleResolver`, `BossBarState` (+ byte layout), `BossBarSource`, `GhostBar`, `PhaseMarkers`,
  `BarLayoutMath`, `DpsMeter`. Server: `BossBarHub`, `BossBarSources`, `BossBarStatePayload`, `BossBarCommands`, `BossBarSelfTests`.
* Client (`client/gui/bossbar`): `BarStyles` (reload listener), `CustomBarClient`, `CustomBarState`, `CustomBarOverlay`, `CustomBarPainter`.
* Previews: `python3 tools/boss_bar_preview.py` -> `docs/previews/boss_bars/sheet_01..09.png` (four styles, four states each: full health / 58 % with
  shield, ghost chip, wind-up; 17 % enraged with adds and lair warning; invulnerable) and `overview.png` (all 36).

## Edits to existing files (additions only)

| File | Edit |
|---|---|
| `boss/bar/BossBarNet.java` | `registerPayloads`: the `Clear` handler also tells `CustomBarClient`; registers `BossBarStatePayload` and `.Preview` |
| `boss/bar/BossBarSync.java` | `init()` ends with `BossBarHub.init()` |
| `client/gui/bossbar/BossBarOptions.java` | six config keys + getters |
| `client/gui/bossbar/ModBossBarOverlay.java` | `drawFallback(...)` (one bar with the old code); `draw` untouched |
| `NUSMP.java` | `CustomBarClient.init(modEventBus)` after the boss bar config line |
| `entity/zogratis/ZogratisBoss.java` | `castLabel()`; `BossBarHub.track(this, bar)` next to `bar.setProgress` |
| `labyrinth/seal/FloorWardenEntity.java` (= Labyrinth Guardian, Decennial, pocket bosses) | `BossBarHub.track(this, bar)` in `aiStep` |
| `celestial/SpiritKingEntity.java`, `entity/ZagredBossEntity.java` | `BossBarHub.track(this, bar)` where they refresh the bar |
| `labyrinth/mega/decennial/DecennialBossController.java` | `NextLair` record + `nextLair()` (read only) |

## Tests

`BossBarStateTest` (byte round trip, clamps, diff), `BarMathTest` (notches, phase from health, ghost easing, fill and layout, shatter, ring, damage ticker),
`BarStyleResolverTest` (all 200 Decennial bosses, every hall theme, every saga, named bosses, every style used), `BarStylesAssetsTest` (every json parses
and has its atlas, registered emblem, window and socket clear, emblem and effect sheets, layout block equals the Java constants), `NextLairTest`.
Self-test `boss.bars` (server smoke): styles load from the jar, the roster of 200 resolves, the state packet through the real `FriendlyByteBuf` codec,
`/multiverse bossbar preview` registered.

## Config and commands

`config/nusmp-bossbar-client.toml`, group `bossbar`: `custom` (true; false = the animated bar / vanilla), `scale` (0.5-2.0), `showPhaseMarkers`, `showHp`,
`portrait`, `showDps`, plus the 1.25.0 keys `animations`, `shake` (also switches the hit shake of the custom bar), `percent`.
`/multiverse bossbar styles` and `/multiverse bossbar preview <style> [hp%] [phase]` (operators, sends a preview to the caller);
`/nusmp bossbar styles` and `/nusmp bossbar preview <style> [hp%] [phase]` (client side, no rights needed). A preview lasts 12 seconds and shows three
phases, a 30 % shield, a wind-up, a looping lair warning, three adds and three fighters.

## Honest list: not verified

* Nothing was run in the game. All Java was type-checked against the stub API dump and the pure logic is covered by JUnit; the drawing code, the
  event wiring, the packet registration and the live portrait have never run. Expect to fix small things on first launch.
* The atlas layers, colours and positions were checked only in the python mock-up, whose font is not the game's. Text widths, the 0.7 scale small text
  and the placement of the title line against the ornaments (some top ornaments reach into the title line) may need a tweak.
* `RenderSystem.clear(256, ...)` after a drawn portrait and the `translate z 150` ordering are the standard recipe but untested; a failure disables the
  portrait for the session (logged) and shows the emblem.
* The portrait temporarily sets the entity's body / head rotation while drawing and puts them back; for entities with their own special pose code this
  may look odd. `bossbar.portrait=false` turns it off.
* `bossbar.showHp` draws "current / maximum" under the percentage only while the boss entity is loaded on the client (it reads the entity's health);
  out of range only the percentage shows.
* Blur on the atlas relies on `.png.mcmeta` `texture.blur` being read for GUI blits (the standard behaviour); if not, the 2x art is drawn with nearest
  filtering and looks sharper-aliased at GUI scale 1.
* Julius's bar and the Lucius channel bar are not tracked (still the animated bar).
* `Generic` fighters = players within 48 blocks (an estimate); Zogratis bosses use their real fighter set.
* The `death` shatter and `dissolve` animations, the entrance and the low-health pulse could not be seen in motion.
