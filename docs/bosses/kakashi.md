# Kakashi Hatake boss (the strongest canon version)

Status: written and wired, **never compiled with Gradle and never seen in a running game** (CI is the compiler; see "Not verified"). Version bump, changelog and release are the coordinator's.

Second pass (the "second pass" rows and sections below): Kakashi was rebuilt on the Black Clover player-model kit (four looks), his boss bar was remade (`naruto_leaf`), and the canon audit against the owner's English-wiki rundown added 14 moves and four canon limits. Everything older stays and sits behind a setting.

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
| NEW (second pass) | Kakashi on the player-model kit: 4 looks `bc_kakashi_hatake` (covered eye), `_open`, `_mangekyo`, `_susanoo` (glowing Perfect Susanoo as an extras model), painted 256x256 skins, cube hair and extras, shimmer themes, pose patterns `raikiri`, `seals`, `throwing`, `kamuiEye`, `spread`, `slash`, `PlayerBodies.Kakashi`, `KakashiLooks`, `KakashiLooksAudit`; generator `tools/bc_playermodels/gen_kakashi.py` (+ `kkskin.py`, `kkparts.py`). |
| NEW (second pass) | 14 canon moves (see "Canon audit"), the charge / strain / element-reading / Six Paths rules (`KakashiMath.Charges`, `Strain`, `Elements`), 5 particle themes (`CORE`, `GATES`, `SABRE`, `FIRE`, `THUNDER_GOD`), the jagged `KakashiFx.bolt` and `KakashiFx.chidori`, 39 voice lines in 19 groups (101 lines in 48 groups now), server setting `canonKit`. |
| NEW (second pass) | Boss bar style `naruto_leaf` (scroll frame, clean Sharingan and Mangekyo emblems, tomoe phase marks, lightning fill `chidori`), emblems `sharingan_v2`, `mangekyo_v2`, `tomoe`, client setting `bossbar.narutoLeafBar`, client setting `bossLooks.kakashiPlayerModel`. |
| REPLACED | **Kakashi's body**: the kit's player model is now what draws him (the Blender body is no longer the default). **Kakashi's bar**: `naruto_leaf` is what the client draws for the style `naruto` (the server still names `naruto`). Both old versions are kept and one setting each brings them back (`bossLooks.kakashiPlayerModel = false`: the Blender body; `bossbar.narutoLeafBar = false`: the first Naruto bar, art, emblems and all). |
| KEPT | The Blender body files, the `naruto` style json, frame, emblems (`sharingan`, `mangekyo`), `shuriken_row` / `tomoe_ring` motifs, all 15 first moves with their numbers and wind-ups (`canonKit = false` restores exactly them), every other boss, style, model, effect, skill and page. Shared files were only appended to (the emblem and fx lists at the end, the look table after `looks_bc.json`). |

## Canon (peak state) and sources

Rule 10 order was English wiki first, Japanese fallback. In the first pass **both hosts were unreachable from the build session** (`naruto.fandom.com`, `ja.wikipedia.org`: DNS failure), so that part is from memory. For the second pass the canon source is the **English Naruto wiki rundown of Kakashi's techniques that the owner supplied** (the audit below compares the boss against it); nothing was fetched from a wiki in the session, and the Japanese wiki was not needed. Each voice line carries its own `source` in `tools/audio/kakashi_lines.json`; only the lines marked with a chapter or episode there are canon quotes, the rest are marked "in his manner".

Peak canon state used: the Fourth Shinobi World War / Moon arc Kakashi: Mangekyo Sharingan with Kamui in both eyes, Raikiri, the Lightning Beast Tracking Fang, Perfect Susanoo (own Susanoo, War arc), the ninken pack, Sharingan copying.

| Used in the boss | Canon status |
|---|---|
| Sharingan copy, Raikiri, Kamui (warp, intangibility, Kamui Raikiri, Kamui Shuriken), Water Dragon Bullet, ninken (Pakkun, Bull, Guruko, Uhei, Shiba, Bisuke, Akino, Urushi), Shadow Clone, Hidden Mist, Earth Wall, Kawarimi | canon techniques or characters (names and roles from memory) |
| Lightning Beast Tracking Fang | canon (Kakashi, Sasuke-style lightning wolf); the 44-tick homing rules are original |
| Perfect Susanoo with sword slashes | canon (Kakashi's own Susanoo, war arc); the shield, the 7 percent number and the "broken" window are original game rules |
| Purple Lightning | **not canon for Kakashi** (it is Raikiri taken further, in his manner); original ability |
| Kamui Vortex pull, Clone Ambush marks, Kamui Shuriken lane layout | original arrangements of canon ideas |
| Water Dragon then Raikiri conductivity (wet bonus 1.35) | original rule, matches how the series plays water and lightning |

## Canon audit (second pass, source: the owner's English-wiki rundown)

| Canon item in the rundown | State before | Now |
|---|---|---|
| Lightning Cutter (Raikiri used as a blade, limited charges) | missing | `lightning_cutter`: at most 4 charges per fight, a 40-tick window in which his lightning cuts a lightning attack aimed at him (it cancels `LIGHTNING_BOLT` damage) |
| Lightning Release: Shadow Clone | missing (only the plain clone ambush) | `lightning_clone`: at most 2 clones for 100 ticks, touching one paralyses for 36 ticks (slowness 6 plus no digging) |
| Purple Electricity (Chakra Mode style, Obito's gift) | one ultimate only | three more: `purple_strike`, `purple_burst`, `purple_chain` (chain of at most 5 timed hits, wet targets conduct further); the old ultimate stays |
| Kamui: distant warp (Kakashi can send things away and move himself) | only the short step | `kamui_warp`: 3 charges per fight, strain 16 |
| Earth Release: Tracking Fang (ninken summoning) | missing | `earth_fang`: claws from the ground that root for 44 ticks |
| Eight Gates (taught by Guy, hinted in the rundown) | missing | `eight_gates`: 200 ticks, damage x1.25, strain 28 |
| Kubikiribocho (Zabuza's blade, borrowed in the war) | missing | `kubikiribocho` |
| White Light Chakra Sabre (his father's blade) | missing | `chakra_sabre`, 3 cuts, short wind-up |
| Kunai on a wire | missing | `kunai_wire`: pulls the target |
| Flying Thunder God (Minato's technique, used with Kamui in the war) | missing | `thunder_god`: 3 timed blinks, strain 6 |
| Great Fireball (copied) | missing | `great_fireball`: sets the target on fire |
| Six Paths awakening (the Six Paths power Obito passed on) | missing | `six_paths`: once per fight below 20 percent health, 400 ticks, he hovers, the Susanoo shield is x1.6, damage x1.2, then 100 ticks spent (open) |
| Canon limit: Kamui and the Mangekyo strain his eye | no limit | `Strain` meter (100): Copy 8, Kamui Raikiri 12, Kamui warp 16, Kamui shuriken 10, Vortex 22, Eight Gates 28, Cutter 5, Thunder God 6; full = exhausted for 80 ticks (open, no Kamui), the meter falls back to 35, and it also rises slowly every 40 ticks |
| Canon limit: Lightning Cutter and Kamui are charge-limited | unlimited | `Charges`: 4 cutters, 3 Kamui per fight, refilled when a phase begins |
| Sharingan reads what you do | copy of the strongest hit only | `Elements`: after 360 ticks of watching, if one element (fire, lightning, projectile, direct) makes at least 3 hits and 45 percent of the recent damage he picks its counter at 50 percent of his move choices |
| Peak canon (all of the above together) | 15 moves | 29 moves; `canonKit = false` gives exactly the first 15 and the old rules |

Canon facts used in the voice lines are marked with their source in `tools/audio/kakashi_lines.py` (`WIKI`, `WIKI_LINES`); lines "in his manner" are not canon quotes. The canon limits above are balance rules (numbers of this mod, not of the series).

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

The 14 moves of the second pass (they open from the phase in the column; `canonKit = false` removes them):

| Move | Opens | Wind-up | Notes |
|---|---|---|---|
| White Light Chakra Sabre | 1 | 12 | 3 cuts, `.sabre` |
| Kunai Wire | 1 | 14 | wire line, pull |
| Lightning Cutter | 2 | 26 | `.core` chidori in hand, charge limited |
| Lightning Release: Shadow Clone | 2 | 16 | 2 clones, paralysing touch |
| Earth Release: Tracking Fang | 2 | 26 | roots |
| Kubikiribocho | 2 | 28 | heavy swing |
| Great Fireball | 2 | 26 | fire spiral, burning |
| Kamui: Distant Warp | 3 | 34 | 3 charges |
| Eight Gates | 3 | 30 | 200 ticks of x1.25 damage, green flash |
| Flying Thunder God Kunai | 3 | 20 | 3 timed blinks |
| Purple Electricity: Empowered Strike | 4 | 24 | |
| Purple Electricity: Burst | 4 | 30 | |
| Purple Electricity: Chain | 4 | 28 | at most 5 timed hits |
| Six Paths Awakening | 4 | 40 | once per fight below 20 percent |

Defence rules: Kawarimi (a single hit of at least 6 percent of his health becomes a log, cooldown 260), Kamui intangibility from phase 3 only (window 14, cooldown 90, never while he is winding up or recovering, and solid for 20 ticks after a move starts so a combo after the first swing lands), the broken Susanoo is open.

## Boss bar, voice, model

* Bar (first version, kept): style `naruto` (white and blue lightning fill, red glow, Sharingan emblem, four phases with notches at 75, 50 and 25; Susanoo shield as the shield strip, the Kamui window as "invulnerable"). Preview: `docs/previews/boss_bars/naruto.png`.
* Bar (second version, the default now): style `naruto_leaf`, generated by `tools/boss_bar_art/naruto_v2.py` (frame base), `emblems.py` (`sharingan_v2`, `mangekyo_v2`, `tomoe`) and the `chidori` fill tile of `tools/gen_boss_bar_textures.py`. A ninja-scroll frame (rolled paper ends with a red cord, steel bezel, leaf-village swirl on the cap), a tight palette (ink navy, slate steel, ice highlight, paper cream, Sharingan red only on the tomoe and emblem), one light direction (top left), a 1 px ink outline, a lightning-blue fill gradient (cyan on top, electric blue below) with forking arcs and a bright leading edge (the painter's existing 2 px glow edge), a clean Sharingan in the socket that becomes the Mangekyo from phase III, three tomoe on mounting plates under the 75 / 50 / 25 notches (they go dark when that phase is behind), and a subtle shimmer. The server still names `naruto`; the client swaps `naruto_leaf` in while `bossbar.narutoLeafBar` is on (`BarStyles.effective`). Previews: `docs/boss_showcase/kakashi/boss_bar_v2_health.png` (100 / 60 / 30 percent), `boss_bar_v2_phases.png` (each phase, the Kamui window, the Susanoo shield, a wind-up), `boss_bar_v2_compare.png` (old above new), `boss_bar_v2_detail.png` (the emblems at 6x and at the sizes the game draws them). These are `tools/boss_bar_preview.py` composites of the generated textures: no animation, no game font, no live portrait.
* Voice: all lines are data (`data/anime_multiverse/kakashi/lines.json`), subtitles `subtitles.anime_multiverse.kakashi.<id>`, sound events `kakashi.voice.<id>` only for ids listed in `recorded.json` (empty now: a line with no file shows its subtitle or chat text only). Drop-in: the coordinator records offline with the Kokoro profile `kakashi`, puts one `.ogg` per id at the path in the manifest, then runs `python tools/audio/kakashi_voice.py`; it rewrites `recorded.json`, `sounds.json` and nothing else is needed. Edit lines in `tools/audio/kakashi_lines.py`, then regenerate the JSON files.
* Model (first version, kept behind `bossLooks.kakashiPlayerModel = false`): the Blender body, built by `tools/blender/boss_kakashi.py` with `tools/blender/bl_lib.py` (8 parts, 2,137 quad lines, LOD 913). Renders are in `docs/boss_showcase/kakashi/` (`model_*.png`, `pose_*.png`).
* Model (second version, the default): the Black Clover player-model kit, as the owner asked ("any boss models should use the existing one that the Black Clover bosses are using"): the vanilla player model with a painted 256x256 skin and cube-model extras on bones, one atlas `pmx_bc_<id>.png` per look (+ `_glow`), layers main / `_fine` (distance detail) / `_fx` (the only glowing, additive layer). Built by `tools/bc_playermodels/gen_kakashi.py` through the kit's own generator libraries (`bclib`, `bcparts`, `bcskin`, `hd_core`, `hd_detail`); **no Blender was used and no Blockbench** (the kit generator is the real tool of this kit; it writes the geo json, skin and atlas directly). The four looks are picked by state in `KakashiLooks.pick` (hitai-ate over the left eye with the Sharingan showing; the band pushed up while a move is in hand; the Mangekyo from phase 3; the glowing Susanoo model while the Susanoo or the Six Paths stands). Poses per move family: Raikiri hand drawn back, seals, throw, Kamui hand to the eye, spread arms for the Susanoo, slash, and a slump while his eye is strained. Silhouette and palette follow the reference the owner described (silver spiky hair, protector with the leaf plate over the left eye, mask, dark-blue shirt, green flak vest, bandaged forearms and legs); **the reference image itself was not in the session** (the file that was supposed to be it was a different character), so the likeness was built from the description. Sheets: `docs/boss_showcase/kakashi/kakashi_sheet.png`, `kakashi_open_sheet.png`, `kakashi_mangekyo_sheet.png`, `kakashi_susanoo_sheet.png` (turnarounds from the same software renderer the Black Clover sheets use).
* Chidori, Lightning Cutter and Purple Electricity read as a bright white-cyan core with branching jagged arcs and a blue glow: `KakashiFx.chidori` (core puff plus up to 5 forking arcs) and `KakashiFx.bolt` (jagged, at most 6 segments), through the particle budgets, with the shapes the mod already has (`LIGHTNING_FIEND`, `LIGHTNING_SPEAR`, `FX_LIGHTNING_ARC`) and the new `boss.kakashi` themes. No new client VFX layer was written, so this is the particle-level look, not the Time Magic layer quality.

## Settings (`nusmp-kakashi-boss-server.toml`, section `[kakashi_boss]`)

All server configs (they change gameplay): `enabled`, `healthMultiplier`, `damageMultiplier`, `maxHitPercent` (0.30), `kamui`, `susanoo`, `copy`, `speak`, `globalCap` (default 2, at most 4), and since the second pass `canonKit` (default true; false = exactly the first 15 moves and the behaviour of the first pass). Turning a switch off restores the plainer behaviour; nothing else changes.

Client configs (looks only): `bossLooks.kakashiPlayerModel` (default true; false = the Blender body) and `bossbar.narutoLeafBar` (default true; false = the first Naruto bar).

## Cost (rule 11), worst case

* Entities spawned: 0 (clones, hounds, the wolf, the Susanoo are effects and timers). Blocks changed: 0.
* Server work: at most 12 foes looked at per move; Beast Fang 1 small box query per tick for 44 ticks; Vortex 15 pulls on at most 12 foes; Purple 3 foe queries; Ledger 8 players, 300 ticks. Everything is dropped on logout, death, dimension change and server stop (`KakashiEvents`). One boss per arena (24 blocks) and a global cap.
* Network: at most 6 effect packets per move (`MAX_VFX_PER_MOVE`), at most 24 particles per call, all through `PerfBudgets.particles` and the shared boss-effect budget, sent to nearby players only; the voice line is one chat/subtitle message per line, paced (50 ticks between lines, 400 between groups).
* Client draw, model: the kit body is about 321 main quads plus 10 fine-detail quads (the vanilla player model draws on top), one draw per boss; the Susanoo look adds 82 glowing quads (look cap 700 quads, glow cap 360, enforced by `KakashiLooksAudit`; the Black Clover cap is 700 and 120). The Blender body (only when the setting is off) is 2,137 quad lines up close, 913 in the distance. No per-frame allocation either way.
* Client draw, bar: the same quads as every custom bar (back layer, fill pieces, frame layer, glow layer, texts) plus three 6x6 blits for the tomoe; the emblem is one blit (26 px, or 11 px as a badge beside the live portrait); the fill's tile is one scrolling blit. No per-frame allocation.
* Server, second pass: at most 2 lightning clones (one foe query every 4 ticks each, at most 100 ticks); the Purple chain at most 5 timed hits, no entity spawn, no block change; Earth Fang, Great Fireball, Kubikiribocho, Sabre, Wire and Thunder God at most 6 effect packets per move and the same 12-foe cap; Eight Gates is a 200-tick flag (no scan); Six Paths is 400 ticks, one `noGravity` set and one velocity write per tick, no queries; element reading is a small array of decaying counts per fight; strain and charges are two ints. Everything is dropped on logout, death, dimension change and server stop like the first pass.
* Tests that guard this (the second pass added `KakashiCanonKitTest`, `KakashiPlayerModelsTest`, `NarutoLeafBarTest` and extended `KakashiMathTest`, `KakashiMeshPoseTest`, `BarStyleResolverTest`): `KakashiSourceScanTest` (no client classes, no `addFreshEntity`, `setBlock`, `.explode`; particles only through `KakashiFx`), self-tests `kakashi.boss.moves` fires every telegraph and run on the server.

## Self-tests and smoke prefix

`kakashi.boss` was added to `SMOKE_EXPECT` in `.github/workflows/smoke.yml`. Keys: `kakashi.boss.data`, `.voice`, `.spawn`, `.moves`, `.kamui`, `.cap`.

## Merge points (one-line additions to shared files)

`ArcCharacters` (`KakashiBosses.registerAll()` after Sinbad), `ArcSystem` (`KakashiBoot.init`), `ArcSpawner` (the Kakashi gate), `MultiverseCommands` (`KakashiCommand.node()`), `BossBarSources.of`, `smoke.yml`, `lang/en_us.json` (appended), `BarStyleIds` / `BarStyle.EMBLEMS` / `BarStyleResolver`, `MeshPose` and `BossMeshRouting` (appended), `tools/boss_bar_art/*`, `tools/particles/gen_themes.py`, `particle_themes.json`.

## Not verified

Everything below is not verified in a game or against the wikis:

* Nothing was compiled by Gradle and nothing was seen in a running game; no gameplay balance was tried. The pure classes and tests (`KakashiMath`, `KakashiLooks`, `KakashiLooksAudit`, `BarStyle*`, the asset tests) were compiled and run with plain `javac`; the files that need the Minecraft API (`KakashiBoss`, `KakashiMoves`, `KakashiFx`, `PlayerBodies`, `PlayerModelOptions`, `BossMeshRouting`, the bar painter and options) were only read through, CI is their compiler.
* Not mine but found: `BcPlayerModelsTest.theSelfTestIsRegisteredAndInTheSmokeListWithTheTailUnchanged` already fails on the branch because the end of the `smoke.yml` list changed with the first Kakashi release; the second pass did not touch `smoke.yml` (the new kit checks run under the existing `bcmodel.looks` prefix).
* The model previews are software renders of the kit (not the game): the Susanoo look is boxy, the Mangekyo pattern is tiny at the kit's 4 texels per pixel, the hair is cards (not truly swept back in 3D). The reference image was missing, so the likeness is from the owner's description.
* Chidori / Cutter / Purple look: particle themes and shapes only, never seen; no new client VFX layer.
* Lightning Clone "breaks" by proximity (no entity is spawned, by rule); it is not an entity you can hit.
* The bar previews are composites: the fill's scroll, the shimmer, the entrance and the live portrait were not seen.
* Wiki lookups failed (both hosts unreachable) in the first pass; the second pass used the owner's English-wiki rundown as the canon source. Nothing was verified live against a wiki.
* No audio exists; the lines show their text only.
* With `bossLooks.kakashiPlayerModel` on (default) the kit body draws him and the Blender mesh is only the classic option; if the mesh is chosen and fails to load, the fallback is the kit's Kakashi body.
* In the Blender renders the head reads as Kakashi from the front but looks slightly cluttered in the three-quarter view and the Mangekyo eye is small; the `SUSANOO` runtime pose (arms spread) differs from the hands-together Blender showcase pose.
* The phase markers are tomoe on the new bar (`naruto_leaf`); the first bar (`naruto`) keeps its standard notches.
* No new item, trophy or drops table was made (`BossDropAssetsTest` pins the table count); he drops Magic Stone, a gold coin and, half the time, the existing `shinobi_smoke_pellet_pouch` or `chunin_exam_scroll_case_charm`.
* `kakashi_lines.json` is the single source of truth for line ids and sources.
