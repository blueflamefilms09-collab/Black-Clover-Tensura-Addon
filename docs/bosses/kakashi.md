# Kakashi Hatake boss (the strongest canon version)

Status: written and wired, **never compiled and never seen in a running game** (CI is the compiler; see "Not verified"). Version bump, changelog and release are the coordinator's.

He is a stand-alone arc boss: **not** a dungeon boss (no `.floors()`), spawned by command or the arc spawn egg, self-contained. The coordinator connects him to the boss realm later.

## NEW / REPLACED / KEPT (rule 5)

| | What |
|---|---|
| NEW | The boss `kakashi_hatake` (28,000 health, tier 5, saga Allies, source "Naruto"), his 15 moves, 4 phases, own server settings file, `/multiverse kakashi_boss ...` command, spawn gate, cleanup events. |
| NEW | His Blender body `entity_meshes/kakashi.txt` (+ LOD `kakashi_lod.txt`, textures `kakashi.png`, `kakashi_glow.png`) and 5 appended mesh poses (`RAIKIRI`, `SEALS`, `THROW`, `KAMUI_EYE`, `SUSANOO`). |
| NEW | Boss bar style `naruto` (frame, Sharingan and Mangekyo emblems, motifs, 4 phases), appended to `BarStyleIds`, `BarStyle.EMBLEMS`, the resolver. |
| NEW | 13 particle themes `boss.kakashi.*`. |
| NEW | His own voice set: 62 lines in 29 groups (`tools/audio/kakashi_lines.json`, `data/anime_multiverse/kakashi/lines.json`), subtitles, sound wiring for recorded lines only. |
| NEW | Tests: `KakashiMathTest`, `KakashiAssetsTest`, `KakashiSourceScanTest`, `KakashiMeshPoseTest`, and the self-tests `kakashi.boss.*`. |
| REPLACED | Nothing. No existing boss, style, model, effect, skill or page was changed or removed. |
| KEPT | Everything else. Shared files were only appended to by one line each (see "Merge points"). |

## Canon (peak state) and sources

Rule 10 order was English wiki first, Japanese fallback. **Both hosts were unreachable from the build session** (`naruto.fandom.com`, `ja.wikipedia.org`: DNS failure), so everything below is from memory of the manga and anime and is **unverified against the wikis**. Each voice line carries its own `source` in `tools/audio/kakashi_lines.json`; only the lines marked with a chapter or episode there are canon quotes, the rest are marked "in his manner".

Peak canon state used: the Fourth Shinobi World War / Moon arc Kakashi: Mangekyo Sharingan with Kamui in both eyes, Raikiri, the Lightning Beast Tracking Fang, Perfect Susanoo (own Susanoo, War arc), the ninken pack, Sharingan copying.

| Used in the boss | Canon status |
|---|---|
| Sharingan copy, Raikiri, Kamui (warp, intangibility, Kamui Raikiri, Kamui Shuriken), Water Dragon Bullet, ninken (Pakkun, Bull, Guruko, Uhei, Shiba, Bisuke, Akino, Urushi), Shadow Clone, Hidden Mist, Earth Wall, Kawarimi | canon techniques or characters (names and roles from memory) |
| Lightning Beast Tracking Fang | canon (Kakashi, Sasuke-style lightning wolf); the 44-tick homing rules are original |
| Perfect Susanoo with sword slashes | canon (Kakashi's own Susanoo, war arc); the shield, the 7 percent number and the "broken" window are original game rules |
| Purple Lightning | **not canon for Kakashi** (it is Raikiri taken further, in his manner); original ability |
| Kamui Vortex pull, Clone Ambush marks, Kamui Shuriken lane layout | original arrangements of canon ideas |
| Water Dragon then Raikiri conductivity (wet bonus 1.35) | original rule, matches how the series plays water and lightning |

## Abilities

Phase 1 `The Copy Ninja` (100 to 75 percent), 2 `The Pack` (75), 3 `The Mangekyo` (50), 4 `The Perfect Susanoo` (25). Every blow shows where it will land first, and the place is frozen for the last 10 ticks of the wind-up.

| Move | Opens | Wind-up | Telegraph | VFX theme |
|---|---|---|---|---|
| Kunai Barrage | 1 | 14 | aim line, 5 timed volleys | `boss.kakashi.kunai` |
| Raikiri | 1 | 22 | lane on the ground, dash plus thrust | `.raikiri` |
| Sharingan Copy | 1 | 16 | red ring, throws 90 percent of the strongest recent hit (at most 2 percent of his health) | `.copy` |
| Lightning Beast Tracking Fang | 2 | 24 | charge at his hand, then a 44-tick homing wolf (turn limited to 11 degrees a tick) | `.fang` |
| Water Dragon Bullet | 2 | 26 | coil path on the ground, soaks (lightning then 1.35x) | `.water` |
| Ninken Pursuit | 2 | 20 | 8 hounds 4 ticks apart, at most 4 foes | `.ninken` |
| Shadow Clone Ambush | 2 | 18 | 3 marks 120 degrees apart | `.clone`, `.log` |
| Hidden Mist | 2 | 14 | mist field, ranged hits on him take 0.7x beyond 8 blocks | `.mist` |
| Earth Wall | 2 | 12 | wall, hits from beyond 5 blocks take 0.3x | `.earth` |
| Kamui Step | 3 | 10 | warp ring at the target spot, then a strike | `.kamui` |
| Kamui Raikiri | 3 | 26 | intangible dash lane | `.kamui`, `.raikiri` |
| Kamui Shuriken | 3 | 34 | 4 lanes from north, east, south, west; the diagonals are safe | `.kamui` |
| Kamui Vortex (ultimate) | 3 | 40 | pull for 30 ticks then 2.2x base, 70 ticks open afterwards | `.kamui` |
| Perfect Susanoo | 4 | 30 | shield of 7 percent of his health, slash every 64 ticks, 360 ticks; broken: 50 ticks open at 1.3x damage taken | `.susanoo` |
| Purple Lightning (ultimate) | 4 | 40 | up to 3 ground marks on the nearest foes, 3.0x base lightning, 70 ticks open afterwards | `.purple` |

Defence rules: Kawarimi (a single hit of at least 6 percent of his health becomes a log, cooldown 260), Kamui intangibility from phase 3 only (window 14, cooldown 90, never while he is winding up or recovering, and solid for 20 ticks after a move starts so a combo after the first swing lands), the broken Susanoo is open.

## Boss bar, voice, model

* Bar: style `naruto` (white and blue lightning fill, red glow, Sharingan emblem, four phases with notches at 75, 50 and 25; Susanoo shield as the shield strip, the Kamui window as "invulnerable"). Preview: `docs/previews/boss_bars/naruto.png`.
* Voice: all lines are data (`data/anime_multiverse/kakashi/lines.json`), subtitles `subtitles.anime_multiverse.kakashi.<id>`, sound events `kakashi.voice.<id>` only for ids listed in `recorded.json` (empty now: a line with no file shows its subtitle or chat text only). Drop-in: the coordinator records offline with the Kokoro profile `kakashi`, puts one `.ogg` per id at the path in the manifest, then runs `python tools/audio/kakashi_voice.py`; it rewrites `recorded.json`, `sounds.json` and nothing else is needed. Edit lines in `tools/audio/kakashi_lines.py`, then regenerate the JSON files.
* Model: the Blender body, built by `tools/blender/boss_kakashi.py` with `tools/blender/bl_lib.py` (8 parts, 2,137 quad lines, LOD 913). Showcase renders are in `docs/boss_showcase/kakashi/`.

## Settings (`nusmp-kakashi-boss-server.toml`, section `[kakashi_boss]`)

All server configs (they change gameplay): `enabled`, `healthMultiplier`, `damageMultiplier`, `maxHitPercent` (0.30), `kamui`, `susanoo`, `copy`, `speak`, `globalCap` (default 2, at most 4). Turning a switch off restores the plainer behaviour; nothing else changes.

## Cost (rule 11), worst case

* Entities spawned: 0 (clones, hounds, the wolf, the Susanoo are effects and timers). Blocks changed: 0.
* Server work: at most 12 foes looked at per move; Beast Fang 1 small box query per tick for 44 ticks; Vortex 15 pulls on at most 12 foes; Purple 3 foe queries; Ledger 8 players, 300 ticks. Everything is dropped on logout, death, dimension change and server stop (`KakashiEvents`). One boss per arena (24 blocks) and a global cap.
* Network: at most 6 effect packets per move (`MAX_VFX_PER_MOVE`), at most 24 particles per call, all through `PerfBudgets.particles` and the shared boss-effect budget, sent to nearby players only; the voice line is one chat/subtitle message per line, paced (50 ticks between lines, 400 between groups).
* Client draw: the body is 2,137 body-plus-glow quad lines up close, 913 in the distance, one draw per boss, no per-frame allocation.
* Tests that guard this: `KakashiSourceScanTest` (no client classes, no `addFreshEntity`, `setBlock`, `.explode`; particles only through `KakashiFx`), self-tests `kakashi.boss.moves` fires every telegraph and run on the server.

## Self-tests and smoke prefix

`kakashi.boss` was added to `SMOKE_EXPECT` in `.github/workflows/smoke.yml`. Keys: `kakashi.boss.data`, `.voice`, `.spawn`, `.moves`, `.kamui`, `.cap`.

## Merge points (one-line additions to shared files)

`ArcCharacters` (`KakashiBosses.registerAll()` after Sinbad), `ArcSystem` (`KakashiBoot.init`), `ArcSpawner` (the Kakashi gate), `MultiverseCommands` (`KakashiCommand.node()`), `BossBarSources.of`, `smoke.yml`, `lang/en_us.json` (appended), `BarStyleIds` / `BarStyle.EMBLEMS` / `BarStyleResolver`, `MeshPose` and `BossMeshRouting` (appended), `tools/boss_bar_art/*`, `tools/particles/gen_themes.py`, `particle_themes.json`.

## Not verified

Everything below is not verified in a game or against the wikis:

* Nothing was compiled and nothing was seen in a running game; no gameplay balance was tried.
* Wiki lookups failed (both hosts unreachable); canon details are from memory.
* No audio exists; the lines show their text only.
* The client renderer uses the Blender body; the fallback (shared `npc_default` body without a Kakashi skin) was not skinned, so if the mesh fails to load he looks like the default person.
* In the Blender renders the head reads as Kakashi from the front but looks slightly cluttered in the three-quarter view and the Mangekyo eye is small; the `SUSANOO` runtime pose (arms spread) differs from the hands-together Blender showcase pose.
* The phase markers are the style's standard notches, not tomoe.
* No new item, trophy or drops table was made (`BossDropAssetsTest` pins the table count); he drops Magic Stone, a gold coin and, half the time, the existing `shinobi_smoke_pellet_pouch` or `chunin_exam_scroll_case_charm`.
* `kakashi_lines.json` is the single source of truth for line ids and sources.
