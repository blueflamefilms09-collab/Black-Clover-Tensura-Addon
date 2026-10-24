# The Great Spirit & Grimoire Fusion: three directions (phase 1, mock-ups only)

Owner request: remake the GUI as "The Great Spirit & Grimoire Fusion" (Great Sage / Raphael holographic-blue sci-fi UI fused with Grimoire medieval filigree,
ink and parchment). The request text is written for HTML/CSS; this is a NeoForge 1.21.1 mod, so everything below is Minecraft `Screen`s and HUD layers drawn
with `GuiGraphics` (textured quads, additive quads, scissor, one small `BufferBuilder` mesh), **no shaders**. Phase 1 = audit + pictures; nothing in `src/` or
`assets/` was touched. Audit: `docs/gui_redesign/gui_audit.md`.

**All pictures are software mock-ups** (Pillow + numpy, `tools/gui_redesign/`, re-run with `python3 tools/gui_redesign/make_gui_redesign.py`). They are made
from generated textures at the resolution they would ship at, drawn with the vanilla bitmap font and the repo's real rune-circle and skill PNGs, over a fake
world. They are stills (no motion), at window 1280x720: **GUI scale 2** = 640x360 GUI px, **GUI scale 3** = 427x240. They are **not the game**: nothing was seen
in Minecraft, Tensura's own HUD is not drawn, and the ink font is a stand-in (FreeSerif Italic).

| Sheet | File |
|---|---|
| Compare: BEFORE / A / B / C (HUD, grimoire page, clovers, alerts, hover) | `docs/previews/gui_redesign/gui_compare.png` |
| BEFORE (the shipped anime theme, today) | `docs/previews/gui_redesign/gui_before.png` |
| A Holo Grimoire | `docs/previews/gui_redesign/gui_dir_A_holo_grimoire.png` |
| B Parchment & Ink | `docs/previews/gui_redesign/gui_dir_B_parchment_ink.png` |
| C Slime Crest | `docs/previews/gui_redesign/gui_dir_C_slime_crest.png` |
| Textures as they would ship | `docs/previews/gui_redesign/<direction>/textures/*.png`, `texture_budget.md/json` |

Each direction sheet shows: (1) the HUD at both scales with the frame (neon line + filigree corners) and the concentric gauge, and the gauge in four element tints and four fill levels;
(2) the grimoire menu open on "Time Stop" in three frames (ink script; compile line halfway; fully compiled "Analysis complete."), at scale 2, plus scale 3 and a page-flip frame;
(3) the clover icon set (three tiers, idle / hover / HUD size / locked) at both scales; (4) the alert widget with two notices (one typing) at both scales, and the hover
ripple / click snap in four frames.

## The four features mapped to this mod

1. **Dual-layer HUD.** Layer 1 = a frame: a 1 px neon line with a soft bloom (`UiPanel` style) and an ornate filigree corner texture at the four corners plus a small diamond at the middle of the top and bottom edges.
   Layer 2 = the gauge: a bezel (ticks, four cardinal gems), an **outer lane = Mana** (flowing particles, tinted by the grimoire's element, arc length = fill, particles scroll along the arc, a bright head at the fill end),
   an **inner core = Magicules** (a glass disc with swirling liquid, a wavy meniscus at the fill level, bubbles, a specular, in C two eyes watch). **What "Mana" and "Magicule" read is decision 1 below**; in this mod the two names are the same Tensura pool.
2. **Great Sage analysis menu.** The Four Kingdoms / Codex screen gets a "floating book" look: a bobbing cover with filigree corners, two translucent pages (A, C) or parchment (B), a projection ring under it, motes, scanlines.
   Text starts as **ink script** (a TTF font provider, italic serif) and a **compile line** sweeps down the page: ink fades into **glowing data lines** (vanilla bitmap font + bloom pass) with scrambled glyphs inside a 22 px band, matrix-rain columns in the margins (A, C), "Analysis complete." at the end.
   The existing leaf page flip (450 ms, `cos` foreshortening) stays; the leaf is restyled (frame "page flip" on each sheet).
3. **Clover icons.** Frames only; **the old art is kept** and shown in a round medallion (the 24x24 picture inside the old 4 px baked frame). Three-leaf = Tensura `EXTRA` / grimoire cover tier 3 (flat, thin outline); four-leaf = `UNIQUE` / tier 4 (bright, pulsing glow, travelling sparkle); five-leaf = `ULTIMATE` and Anti-Magic / tier 5 (black-violet slime, glossy highlights, drips that sway, violet glow). Plus locked and hover states.
4. **Alert widget + micro-interactions.** A corner crest (A: winged shield with a four-leaf clover; B: gilded round seal; C: corrupt slime face) with a ping ring and a stack counter; below/left of it up to three notices slide out, headed `[Notice... Great Sage has ...]`, a result line, a kind colour (ok / info / warn / bad), typed text with a cursor. Hover = slime ripple (up to three expanding rings clipped to the button, a lens glow under the pointer); click = a flash and a 0.6 px press. Click audio uses existing events (`BOOK_PUT` low pitch for the book snap, `PLAYER_ATTACK_SWEEP` high pitch + `AMETHYST_BLOCK_CHIME` for the unsheathe); no new sound file.

## The three directions

| | A Holo Grimoire | B Parchment & Ink | C Slime Crest |
|---|---|---|---|
| Idea | Raphael's cool cyan hologram dominates; the grimoire is a translucent glass book with silver filigree | a real grimoire: leather, gold filigree, parchment pages; blue hologram appears **only on data** (data plates, the compile line, the Sage chip) | Tensura's corrupt slime: black-violet glass, violet/magenta neon, toxic-green accent, drips on corners and buttons |
| Palette | navy `#06101f`, cyan `#54deff`, blue `#4084ff`, mint `#7cf0c8`, silver filigree | leather `#4a2c1a`, gold `#deb252`, parchment `#e8d9b0`, ink `#3a2616`, hologram blue `#28aaff` | black-violet `#0b0612`, violet `#a65aff`, magenta `#ff50d6`, toxic `#82ff60`, iron/bone |
| Frames | cyan neon + silver filigree | gold line + gold filigree on brown | violet neon + blackened filigree with slime drips |
| Gauge | silver bezel, cyan gems | brass bezel, gold gems | iron bezel, magenta gems, core liquid violet with eyes |
| Grimoire page | translucent (alpha 150) with a rune circle behind, matrix rain | opaque parchment texture (1 extra texture), data in dark-blue plates | translucent dark violet, five-leaf watermark, magenta data |
| Clovers | cyan 3-leaf, white-cyan glowing 4-leaf | emerald enamel, gilded glowing 4-leaf | grey-violet 3-leaf, violet glowing 4-leaf |
| Crest | winged shield (squad insignia) | gilded seal | slime face |
| Fits today's look | **closest** (same cyan family as the anime theme, System window, boss bars) | furthest from today, most "grimoire", best ink legibility | strongest identity, clashes with the blue Tensura/System look |
| Readability risk | translucent pages over a bright sky: dim the scrim (done in the mock-up) | none (opaque) | magenta text on violet is the least comfortable for long text |

## Recommendation

**A as the engine and default look of the new theme; C's styling reserved for the five-leaf tier, corruption and Anti-Magic states (it is already in all three); B kept as an optional skin if the owner wants a warmer book.**
Reasons: A matches the System window, boss bars and the cyan Sage voice the mod already has, so the new theme reads as the same family rather than a second UI; it is the literal "floating translucent blue holographic Grimoire" of the request; all three directions share one engine (same quads, same timelines), only the palette and about 12 textures differ, so B or C can later be added as `ui.greatSpirit.skin` without new code (cost per skin: 120 to 250 KB PNG, 0.5 to 1.0 MB VRAM, loaded lazily). The most distinct direction from today is B; if the owner values "feels like a different book" over "fits the rest", pick B.

## Proposed client settings (all CLIENT config, `config/nusmp-ui-client.toml`, rule 2: old screens stay)

| key | values | default | what |
|---|---|---|---|
| `ui.theme` | `classic`, `anime`, **`great_spirit`** (enum appended, `UiThemeMode.GREAT_SPIRIT`) | `anime` until the owner flips it | `UiTheme.anime()` stays true for `great_spirit` (every existing anime branch still runs); new `UiTheme.greatSpirit()` adds the new branches on top. The owner's `gui.theme = V1 \| GREAT_SPIRIT` maps to `classic/anime` (V1) and `great_spirit` |
| `ui.greatSpirit.skin` | `holo`, `parchment`, `slime` | `holo` | direction A / B / C (palette + texture set) |
| `ui.greatSpirit.gauge` | on/off | on | the concentric gauge layer (`great_spirit_gauge`), registered in `GuiScale`/`GuiMove` |
| `ui.greatSpirit.cloverIcons` | on/off | on | clover frames in the mod's own screens |
| `ui.greatSpirit.notices` | on/off | on | the crest widget; off = today's toasts only |
| `ui.greatSpirit.inkScript` | on/off | on | ink font + compile animation; off = plain text appears as today (also the fallback if the font fails to load) |
| `ui.greatSpirit.quality` | `high`, `low` | `high` | low: gauge arc 24 segments, no bloom pass, no matrix rain, no motes |
| existing `ui.motion`, `ui.typing` | | | still honoured (motion off = the frozen frame 3 of each animation) |
| server config | one key only if the gauge feed becomes a packet: `perf.gaugeSyncHz` (cap 1 to 4) | 2 | bounds traffic |

## Budgets (worst case, per frame, one local player; "quad" = one textured or filled rectangle)

| Element | Quads | Draw-call / state changes | Per-frame allocation | Animation cost | Needs |
|---|---|---|---|---|---|
| Frame (neon line + 4 corners + 2 edge gems + halo) | 18 | 2 (normal, additive) | none | none (pulse by `UiMath`) | textures: `corner_filigree` 96x96 |
| Gauge ring | outer lane arc mesh <= 48 quads (24 on `low`) in **one** `BufferBuilder`, + base 1 + head spark 1 + bezel 1; core: disc 1, liquid 1 (scissor), swirl 1 (rotated), meniscus 1, specular 1, bubbles <= 4 | <= 6 | none (pooled builder; the 3 label strings rebuilt only when a value changes by >= 1 %) | trig for 48 vertices + 4 bubbles; O(1) | `gauge_bezel` 192x192, `gauge_flow_strip` 256x24 (tiled), `gauge_core_swirl` 128x128; **no shader** |
| Whole HUD plate | ~110 quads incl. ~30 glyphs | <= 8 | none | O(1) | one new layer; scaled by `GuiScale.hud("great_spirit_gauge")`, movable by `GuiMove` |
| Grimoire menu | cover 6 + pages 2 + spine 1 + watermark 2 + corners 4 + scan overlay 1 (one tiled quad, not per line) + sweep 3 + motes <= 16; text <= **600 glyph quads** (ink <= 8 lines x 28, data <= 12 lines x 28) with the bloom as a 2nd pass over data lines only (cap total glyphs at 900 incl. rain <= 40) | <= 12 (font pages are batched) | none per frame: strings cached; only the <= 3 lines inside the compile band are rebuilt, every 2 ticks (`scramble` is seeded) | O(lines); the compile timeline is a pure function of age | ink font provider (TTF, JSON); `page_parchment` 326x344 (B only) |
| Page flip | existing leaf: 2 quads + shading gradient | unchanged | none | existing | none new |
| Clover icon | frame 1 + art 1 (+ glow 1 for 4/5, + drips 1 for 5, + sparkle 1 for 4) = 2 to 5 | 1 to 3 | none | pulse + 1 sin each; **only the hovered + <= 24 nearest visible icons animate**, the rest draw static (2 quads) | 3 frames 96x96, 2 glows, 1 drips |
| Alert crest | 3 (crest, glow, ping ring) | 2 | none | O(1) | `alert_crest` 96x96, `ripple_ring` 64x64 |
| One notice | panel 18 + <= 3 lines x 34 glyphs = ~120 | 3 | none (typed prefix = substring of a cached string) | typewriter by `ToastTimeline` | none new |
| Notice column | **<= 3 visible** (same cap as System `TOAST_MAX`), <= 16 queued, dedupe by key, <= 4 new per second | | | | |
| Hover ripple | <= 3 ring quads + 1 lens, scissored to the hovered button; click flash 1 | 2 | none | only the hovered button | `ripple_ring` |

Network: **no new packet needed for the menu, clovers, alerts or ripple** (client only; they read data the mod already syncs). The gauge needs a live magicule and aura fraction for the local player (decision 1): either read Tensura's already-synced capability on the client (0 packets, needs a check against the real jar, not available in this session) or one **self-only** packet of three bytes at most `perf.gaugeSyncHz` (default 2/s) and only when a value moved by >= 1 % (<= 2 packets/s per player, never broadcast), validated on the server (sender = the player, values clamped). Server tick work: the three values are already computed for spell costs; one read per second per player. Everything is cleaned up on logout (`ClientPlayerNetworkEvent.LoggingOut`) like `DjinnClientState`.

Texture sizes (generated, `texture_budget.md`; GUI textures are not mip-mapped, so VRAM = w x h x 4):

| Direction | textures | PNG | VRAM |
|---|---|---|---|
| A | 12 | 121 KB | 536 KB |
| B | 13 (adds the parchment page) | 252 KB | 974 KB |
| C | 12 | 122 KB | 536 KB |

For scale: the existing `gui/theme` circle set is 18 PNGs and a 256x256 `circle_glow`. Textures are shipped at 2 texels per GUI pixel with linear filtering (`.mcmeta` `blur`, `clamp`), like the circles, so they stay crisp at GUI scale 2 and 3.

**What needs shaders: nothing.** Plain textures: frame corners, bezel, strip, swirl, clover frames, crests. Plain quads with additive blending: glows, sweep, ripples. One small custom mesh (`BufferBuilder`, `POSITION_TEX_COLOR`): the gauge arc, which can fall back to 24 rotated segments. Custom font provider (resource JSON, no code): the ink script. A real refraction ripple or a true hologram flicker would need a shader; the mock-ups deliberately do not, to stay inside Iris/Sodium-safe vanilla drawing and the rule-11 budget.

## Tests to write in phase 2 (pure first, then scans, then the server self-test)

* `GaugeMathTest`: arc segment count, vertex/UV continuity, fill quantisation (>= 1/48 steps), clamping and NaN/negative input, liquid level and wave bounds.
* `CompileTimelineTest`: per-line state (ink / band / data) is monotonic in age, frame 0 is pure ink, the last frame pure data, at most 3 lines in the band, `scramble` deterministic for a seed.
* `CloverTierTest`: `Skill.SkillType` and `GrimoireCover.tier` to leaves (3/4/5), `BLACK_MAGIC` and `GOD_TIER` to 5, unknown to 3, `RESISTANCE`/`COMMON` mapping.
* `NoticeQueueTest`: 3 visible / 16 queued / dedupe / 4 per second, order, logout clear, the `[Notice... Great Sage has ...]` prefix comes from a lang key.
* `RippleTimelineTest`: at most 3 rings, ages and fade bounded; `UiThemeModeTest`: `great_spirit` parses, default stays `anime`, `anime()` is true for it.
* `GreatSpiritTextureTest` (reads the repo's files like the particle/texture tests): every `gui/gs_*` PNG exists with `.mcmeta`, sizes within budget, each is referenced by code.
* `PerfSourceScanTest` extended with the new client classes (no allocation patterns in render methods, no per-frame `String.format`), `ServerSafetyTest` unchanged (all new classes live under `client/`; allow-list untouched), `ConfigFileNamesTest` (same file `nusmp-ui-client.toml`, new keys only).
* `SelfTests` prefix `gs.` for the server smoke test: gauge feed payload bounds and sender check (only if the packet is chosen), config parse; the draw code is client-only and is never loaded on the dedicated server.
* A determinism check of the texture generator (`tools/gui_redesign`) like the other `gen_*` tools, then promote it to write into `assets/` in phase 2.

## Open decisions for the owner

1. **What do the two rings read?** Mana and Magicule are the same Tensura pool in this mod. Options: (a) **outer = Aura, inner = Magicule** (two real pools; Anti-Magic and the slash/light/body/earth families already pay Aura; the ring would be labelled AURA in the Sage voice but drawn as the flowing "mana" lane; **recommended**); (b) outer = the spendable fraction of magicule labelled MANA and inner = corruption / negative mana / Mana Zone time left; (c) both labels, both reading magicule (redundant). Also: draw next to Tensura's own bars, or hide them (needs a check of Tensura's HUD layers against the real jar).
2. Which direction: A (recommended), B, C, or A with B/C as skins.
3. Where the crest sits (bottom right in the mock-up; it is movable with GUI Move anyway) and whether chat lines may be mirrored into notices (recommended: **only** messages sent through a new `GreatSageNotice` client call, the page-unlock and System toasts, plus an allow-list of lang-key prefixes; chat itself is never cancelled).
4. The ink font: a real file is needed (an OFL italic serif). The mock-ups use FreeSerif Italic only as a picture; a licence-clean choice has to be made and listed in `LIBRARIES.md`.
5. Clover plates around skill art: only inside the mod's own screens (recommended first), or also pre-composited copies of the ~160 skill PNGs for **Tensura's** windows (a generator step, off by default, needs a client-only icon holder because `getSkillIcon` is also called on the server side by common code).
6. Five-leaf covers: `BLACK_MAGIC` and `GOD_TIER` are tier 5 too; suggest a gold-glow variant for `GOD_TIER` (appended later).
7. Whether the book should get true 3D tilt (pose-matrix quads) instead of the flat bob; it costs nothing in quads but needs care with the existing GUI Size scaling and was not mocked.
8. Wording: `[Notice... Great Sage has ...]` versus today's "Notice:" / "Result:" lines; propose one voice, all in lang keys (`nusmp.ui.*`, new `nusmp.gs.*`).

## What was not seen

Nothing in this document was seen in a running game. Motion (ripple, compile line, flow, bob, drips) is shown as stills; real frame cost was not measured (the budgets are counted from the design, to be checked with `PerfProbe` in phase 2); Tensura's own HUD and ability windows were not reproduced; the ink font is a placeholder; colours of translucent panels depend on the real world behind them.

## Phase 2 order (after the owner picks)

1. `UiThemeMode.GREAT_SPIRIT` + `UiTheme.greatSpirit()` + options + tests. 2. Move `tools/gui_redesign` generators into a `tools/gen_gs_gui_textures.py` that writes `assets/nusmp/textures/gui/gs/` (additive). 3. Frame corners in `UiPanel` behind the switch. 4. Gauge layer + feed. 5. Grimoire menu skin + `CompileTimeline`. 6. Clover frames. 7. Alert widget + `GreatSageNotice`. 8. Ripple in `ThemedButton`. Each step: lang fragment, GUI Size/GUI Move registration, perf note, tests, release notes (NEW / REPLACED / KEPT).
