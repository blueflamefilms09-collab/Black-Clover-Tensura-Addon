# The Trial Devil's own model

Before this change the devil of the trial fight (docs/devil_contract_phase2.md, section 1 rule 4) was drawn as a vanilla humanoid with the zombie skin: a placeholder that did not meet CLAUDE.md rule 14. It now has a real model of its own: a horned brute with a painted sheet in every devil's palette, glowing eyes, brands, gems and horn tips, wings, a spade tail and three clips. Server rules are untouched (the entity is still never saved, never despawns by itself, only its caller can hurt it, at most `trial.max` exist).

## What it is, and what it is not (rule 15, plainly)

- **Made by a script, not in Blockbench.** Blockbench (and Blender) could not run in the session. `tools/blockbench/gen_devil_trial.py` writes the model in Blockbench's own source format (`tools/blockbench/devil_trial.bbmodel`: bones as groups, 110 cubes with per-face UV, the painted sheet embedded, the three clips as keyframes) and exports the game files from the same data. The `.bbmodel` was **not opened in Blockbench**: its bone and cube rotations use the sign the Bedrock codec writes (x and y negated), written from memory, so open it once and check the pose before editing by hand. Re-running the script writes the same bytes.
- **The game files are authoritative**: `geo/entity/devil_trial.geo.json`, `animations/entity/devil_trial.animation.json`, `textures/entity/devil_trial*.png`. A hand-made Blockbench model can replace them file for file (keep the bone names the clips and the entity use: `root body chest head jaw mane arm_{r,l} arm_{r,l}_low arm_{r,l}_hand leg_{r,l} leg_{r,l}_low leg_{r,l}_foot wing_{r,l} wing_{r,l}_tip tail1..3 horn_{r,l}(2,3) lock_{r,l}`).
- **Previews are software renders** (`docs/devil_looks_previews/trial_*.png`, made by `tools/blockbench/preview_trial.py` on the repo's numpy renderer): not the game. Lighting, mip-mapping, the glow blend and GeckoLib's easing differ. `trial_overview.png` shows all 26 devils; `trial_liebe.png`, `trial_lucifero.png`, `trial_fox_devil.png`, `trial_daemon_rouge.png` show front, three-quarter, side, back, a walk frame and an attack frame. Nothing was seen in a running game.

## The model

| Part | Detail |
|---|---|
| Size | 33 bones, 110 cubes = 660 quads, 256x256 sheet at 2 texels per model pixel; about 2.1 blocks tall to the crown, 2.6 with the horns; the hitbox stays 0.7 x 1.95 |
| Body | broad chest with pectoral and sternum brand, ribbed abdomen, belt with a gem, loincloth front and back, collar, spine plate |
| Head | brow ridge, nose, cheek spikes, pointed ears, jaw with tusks and chin spike, painted V-shaped glowing eyes, cheek brands, scalp with fringe tufts, mane, two long hair locks |
| Horns | three segments curving up and back, the last glows from the tip in |
| Arms | pauldrons with spikes, bracers, elbow spikes, clawed hands with thumbs; the elbow and the hand are bones |
| Legs | thigh plate, bent shin with greave, hoof with two toe claws |
| Wings | a bone arm and a second segment with a claw, five stair-stepped membrane panels and three ribs per wing, each wing two bones |
| Tail | three tapering segments and a spade |
| Sheets | one per devil (26) plus a default: skin tones, hair and accent come from `tools/bc_playermodels/dvtable.py`, so a devil reads the same here and on the player-model looks (docs/devil_looks.md); the glow colour is the accent brightened (red where the accent is grey) |
| Glow | `devil_trial_<id>_glowmask.png`, drawn by GeckoLib's `AutoGlowingGeoLayer` (eyes, cheek and chest brands, chest and belt gem, horn tips: 60 to 4,000 lit texels, tested) |
| Clips | `idle` (3.2 s: breath, head turn, wing beat, tail and hair sway, jaw), `walk` (1 s: legs and shins, arms, torso twist, wings, tail), `attack` (0.6 s, once: arm raised over the head and brought down, torso and head follow, jaw opens, tail whip) |
| Rank | the renderer scales by rank: Supreme 1.08, High 1.0, Mid 0.95, Low 0.9 |

## How it is wired

- `DevilTrialEntity` is a GeckoLib entity (`GeoEntity`). The devil's id is a **synced string** (the one new piece of synced data, set once at start) so the client picks that devil's sheet. The attack clip is started by the vanilla swing flag the mob already syncs, so there is **no packet of its own** (no `triggerAnim`).
- `client/devil/DevilTrialRenderer` now delegates: model on, it draws `DevilTrialGeoRenderer`; model off, the earlier stand-in (kept unchanged inside as `Old`, rule 2).
- Client option **`bossLooks.devilTrialModel`** (`config/nusmp-playermodel-client.toml`, CLIENT config, default **true**, appended after the Kakashi option): **false restores the earlier vanilla-humanoid look exactly**, live, no restart. Lang keys `anime_multiverse.configuration.devilTrialModel(.tooltip)`, fragment `tools/lang/devil_trial.json` (merged append-only by `tools/lang/merge_fragment.py`).
- The server still sets the glowing flag on the devil (an outline in the team colour). With the new model it is a visibility aid only; the look is the glow mask.

## Worst-case cost (rule 11)

| Where | Worst case |
|---|---|
| Server | no change: one entity per player, at most `trial.max` (default 4, hard 16) on the server, each at most 150 s, one check a second. One synced string per entity, written once. No new tick work, scan or entity. |
| Packets | none of its own. The synced id rides in the entity's spawn data (once per entity per tracking player, at most 4 entities). |
| Client draw | at most 4 devils on screen, each 660 quads plus one glow pass (about 2,640 quads and 4 additive batches in the worst case, in practice a single trial per fight). Entities beyond the vanilla render distance are skipped by the game. |
| Allocation | none per frame: the sheet locations are cached in a map bounded by the roster (26), the scale is a switch, the option is one boolean read. `DevilTrialModelTest` asserts `render` has no `new`. |
| Memory | 27 sheets and 27 glow masks of 256x256 (about 1.8 MB of PNG in the jar, each loaded only when that devil is first drawn), one geo and one animation file. |

## NEW / REPLACED / KEPT (rule 5)

- **NEW**: the trial devil's model, clips and 27 sheets with glow masks; `DevilTrialGeoRenderer`; the option `bossLooks.devilTrialModel`; the synced devil id; `tools/blockbench/` (generator, preview script, `.bbmodel`, stats); the previews; the test `DevilTrialModelTest`; the lang keys.
- **REPLACED**: nothing was removed. The zombie-skinned humanoid is no longer the default look but stays in `DevilTrialRenderer.Old` behind the option.
- **KEPT**: every server rule of the trial, the boss bar, the glowing flag, the ritual figure and the worn forms (docs/devil_looks.md), all earlier models.

## Tests

`DevilTrialModelTest` (pure, 8 checks): the geometry is well formed and within 60..160 cubes / 960 quads; every roster devil has a sheet and a glow mask of 256x256 with a plausible number of lit texels; the three clips exist, are played by the entity, loop or not as intended and move real bones; the `.bbmodel` matches the game file and says it is the fallback; the option is a client config on by default with the old stand-in kept; the entity keeps its server rules and syncs only the devil id; the betrayal floor setting exists. Also run: `GeoAssetsTest` (it checks the new geo, its sheet and its clips), `Phase2SourceScanTest`, `ServerSafetyTest`, `ConfigFileNamesTest`, `PerfSourceScanTest`, `DevilPlayerModelsTest`, `DevilSourceScanTest`, `Phase2RulesTest`, `Phase2TableTest`, `DevilTexturesV2Test` (all green, javac plus the JUnit launcher, no Gradle). The self-test `devil.phase2.trial` was extended (synced id empty on a fresh entity, GeckoLib cache present, betrayal floor bounded); it runs under the existing `devil.phase2` smoke prefix.

## Not verified

- Nothing was seen in a game. The Java (entity, both renderers, option) was written against GeckoLib 4.9.2's API (checked with `javap` on the jar) and the repo's other GeckoLib code, but **could not be compiled** (no Gradle, no mapped Minecraft jar): CI is the compiler.
- Unverified in game: the pose signs of the clips on GeckoLib (the preview renderer follows the kit's convention; an arm raised the wrong way would be a sign flip in `gen_devil_trial.py`), the `AutoGlowingGeoLayer` path convention (`<sheet>_glowmask.png` next to the sheet), the attack clip starting from the swing flag, how the glow outline looks together with the glow mask, and every colour.
- The faces and the glow marks are painted in code at 16x16 texels per head: clean but generic. Expect the owner to want touch-ups, or a hand-made Blockbench model.
- The `.bbmodel` has not been opened in Blockbench (rotation sign, see above).

## Files

Generator and sources: `tools/blockbench/{gen_devil_trial.py, preview_trial.py, devil_trial.bbmodel, devil_trial_stats.json}`. Resources: `geo/entity/devil_trial.geo.json`, `animations/entity/devil_trial.animation.json`, `textures/entity/devil_trial.png`, `devil_trial_<id>.png`, `devil_trial[_<id>]_glowmask.png`. Java: `client/devil/{DevilTrialGeoRenderer, DevilTrialRenderer}`, `devil/phase2/DevilTrialEntity`, option in `client/playermodel/PlayerModelOptions`. Lang: `tools/lang/devil_trial.json`, `tools/lang/merge_fragment.py`. Test: `DevilTrialModelTest`. Previews: `docs/devil_looks_previews/trial_*.png`. Regenerate everything: `python3 -B tools/blockbench/gen_devil_trial.py --preview`.

## Canon looks (the devil is the devil, not a zombie with its name)

**Owner report**: in the trial fight the devil was "just a zombie with the devil's name" (that was the fallback stand-in, or the 4.64.0 generic brute that all 26 devils shared), and when it was summoned "it looks like a little child, and then it has the wings and stuff above the child".

### Root cause of the child with wings above it

`DevilNpcRenderer` built its two `PlayerModel` instances without `young = false`. The game's model class starts as a **child** (`EntityModel.young` is true by default), so the body was drawn baby-proportioned while `DevilExtras` drew the horns and wings at adult size around the adult skeleton anchors: a small child under a full-size pair of wings. `AvatarPreview` already sets `young = false`; the figure never did. Fixed in both ways: both models are now adult-proportioned (also on the old path), and the default figure no longer uses the player model at all (below).

### What was built

1. **One canon model per roster devil (26)**: own geometry (`geo/entity/devil_canon_<id>.geo.json`), own painted sheet (`textures/entity/devil_canon_<id>.png`, 256x256) and own glow mask (`..._glowmask.png`), clips file (`animations/entity/devil_canon_<id>.animation.json`) with the same clip names as the brute (`animation.devil_trial.idle / walk / attack`) plus `rise` (1.5 s) and `roar` (2.0 s). Drawn by `DevilTrialGeoRenderer` for the trial; the devil's id is the synced string the entity already had, so nothing new is sent.
2. **The summoning figure is the same model**: `DevilNpc` is now a GeckoLib entity and is drawn by the new `DevilNpcGeoRenderer` (chosen by `DevilNpcRenderer` while `bossLooks.devilRitualFigure` is on). Same model, sheet, glow and rank scale (`DevilCanonRules.rankScale`, 1.08 Supreme / 1.0 High / 0.95 Mid / 0.9 Low, shared with the trial renderer) as the trial devil. Wings, horns, tail and hair are **bones of the body**, so they can not float.
3. **Rising out of the circle**: the figure starts 1.4 blocks sunk and transparent, fades in during the first half of the rise (30 ticks, `RISING`), climbs out while the `rise` clip stands it up from a crouch with its wings folding open, then idles; the offer plays the `roar` clip; merging shrinks it to 40 percent, fades it and slides it into the signer's chest; dissolving fades it. The glow layer fades with it (a faded figure does not leave bright eyes behind).
4. **Appearance effect (server side)**: every 3 ticks of the rise a ring of dust in the devil's colour and half as many dark haze particles at its feet, and a larger burst as it stands up (`DevilCanonRules.riseBurst`), sent to nearby players only, through `PerfParticles` (the per-tick particle budget).

Switches (both CLIENT configs, default on, `bossLooks.*`): `devilCanonLooks` (off = the 4.64.0 generic brute for every devil, in the trial and in the figure), `devilRitualFigure` (off = the earlier figure: the player-model look, or the Blender mesh). `devilTrialModel` (off = the zombie stand-in) still wins for the trial. A devil that has no canon model (a pack devil, a future devil) keeps the generic brute. Lang: `tools/lang/devil_canon.json` (4 keys, merged append-only).

### The 26 models (sources, rule 10)

Source order was English wiki first; the Japanese wiki could **not** be fetched in the session, so no entry comes from it. Each row's last column is the first sentence of the source note kept in `tools/blockbench/devil_canon_specs*.py` (and in `devil_canon_stats.json`), which also lists the features taken from the page.

| Devil | Body | Cubes | Quads | Height (blocks, before rank scale) | Source |
|---|---|---|---|---|---|
| `liebe` | parametric humanoid | 93 | 558 | 1.99 | Black Clover wiki (en), Liebe: Appearance |
| `lucifero` | parametric humanoid | 118 | 708 | 2.50 | Black Clover wiki (en), Lucifero: Appearance |
| `beelzebub` | parametric humanoid | 107 | 642 | 2.48 | Black Clover wiki (en), Beelzebub: Appearance |
| `astaroth` | parametric humanoid | 118 | 708 | 2.50 | NOT on the English wiki (Astaroth has no Appearance section: only 'highest-ranking devil, Time Magic') and the Japanese wiki could not be fetched: this look is the mod's own design (golden, time-themed): cream hair, a clock ring behind the head, gold feathered wings, an hourglass gem |
| `megicula` | parametric humanoid | 117 | 702 | 2.11 | Black Clover wiki (en), Megicula: Appearance |
| `lucifugus` | parametric humanoid | 96 | 576 | 2.52 | Black Clover wiki (en), Lucifugus: Appearance |
| `fox_devil` | custom: fox | 52 | 312 | 2.31 | Chainsaw Man wiki (en), Fox Devil: Appearance |
| `curse_devil` | custom: two-headed skeleton | 88 | 528 | 2.38 | Chainsaw Man wiki (en), Curse Devil: Appearance |
| `future_devil` | custom: tree body | 63 | 378 | 2.44 | Chainsaw Man wiki (en), Future Devil: Appearance |
| `ghost_devil` | custom: flower-covered giant | 147 | 882 | 2.44 | Chainsaw Man wiki (en), Ghost Devil: Appearance |
| `sebastian` | parametric humanoid | 111 | 666 | 2.31 | Kuroshitsuji wiki (en), Sebastian Michaelis: Appearance |
| `kin_of_iblis` | parametric humanoid | 56 | 336 | 1.91 | Blue Exorcist wiki (en), Iblis: Appearance (host and demon form) |
| `kin_of_egyn` | parametric humanoid | 76 | 456 | 2.04 | Blue Exorcist wiki (en), Egyn: Appearance (hosts) |
| `kin_of_amaimon` | parametric humanoid | 71 | 426 | 2.42 | Blue Exorcist wiki (en), Amaimon: Appearance (hosts) |
| `kin_of_astaroth` | parametric humanoid | 85 | 510 | 2.34 | Blue Exorcist wiki (en), Astaroth: Appearance (hosts) |
| `kin_of_lucifer` | parametric humanoid | 74 | 444 | 2.25 | Blue Exorcist wiki (en), Lucifer: Appearance (host) |
| `daemon_noir` | parametric humanoid | 105 | 630 | 2.56 | Tensura wiki (en), Diablo (Primordial Noir): Appearance |
| `daemon_blanc` | parametric humanoid | 100 | 600 | 2.44 | Tensura wiki (en), Testarossa (Primordial Blanc): Appearance |
| `daemon_jaune` | parametric humanoid | 97 | 582 | 2.34 | Tensura wiki (en), Carrera (Primordial Jaune): Appearance |
| `daemon_violet` | parametric humanoid | 100 | 600 | 2.24 | Tensura wiki (en), Ultima (Primordial Violet): Appearance |
| `daemon_rouge` | parametric humanoid | 97 | 582 | 2.54 | Tensura wiki (en), Guy Crimson (Primordial Rouge): Appearance |
| `daemon_vert` | parametric humanoid | 95 | 570 | 2.36 | Tensura wiki (en), Misery (Primordial Vert): Appearance |
| `daemon_bleu` | parametric humanoid | 90 | 540 | 2.26 | Tensura wiki (en), Rain (Primordial Bleu): Appearance |
| `hall_demon` | custom: six-armed statue | 90 | 540 | 2.44 | Dororo wiki (en), 48 Demons / Hall of Hell / Twelfth Demon (Asura): the Hall of Hell is a temple of statues of the 48 demons; in the 2019 anime the Twelfth Demon's statue has three heads and six arms, resembling Brahma; its true form is a blob under the castle (not drawn) |
| `god_hand` | parametric humanoid | 144 | 864 | 2.53 | Berserk wiki (en), God Hand and Femto/Griffith pages: the page fetched is the lead and infobox only (silver hair, blue eyes, 'Wings of Darkness'); the look is the Femto form from memory of the manga (a pale lord in white and gold, a horned crest, huge dark and white wings, a crimson cloak and the Beherit gem); the other four members are not drawn |
| `etherious` | parametric humanoid | 82 | 492 | 2.52 | Fairy Tail wiki (en), Etherious and Mard Geer: Appearance |

Shared rig, but not the same look: all humanoids share one parametric body builder (`tools/blockbench/devil_canon_lib.py`, bone names `root body chest head jaw mane arm_* leg_* wing_* tail* horn_* lock_*`), and each is told apart by build, hair, horns, wings, tail, clothing, extras (halo, third eye, crown, mushrooms, ...) and its own painted palette; five devils have their own body (the Fox Devil, the Curse Devil, the Future Devil's tree, the Ghost Devil's flower giant, the Hall of Hell statue). `DevilCanonLooksTest` asserts that no two sheets and no two geometries are equal. Software mock-ups of all 26 (turnarounds with idle, walk, attack and roar) are in `docs/previews/devil_canon_looks/` (`overview.png` first); they come from the repo's numpy renderer, not the game.

**Not fetched or not on the wiki** (the look there is the mod's own design, said plainly): Astaroth (Black Clover; the English page has no Appearance section and the Japanese wiki could not be fetched, so cream hair, a clock ring and gold wings are invented); the Blue Exorcist kin devils (the host-and-demon descriptions come from the English wiki, the bodies are the mod's own); the Hall of Hell demon (the 2019 anime's six-armed statue); the God Hand (the fetched page was the lead and infobox only, so the Femto look is from memory of the manga and the other four members are not drawn); the Etherious (the Etherious Form itself was not described on the pages fetched).

### Which tool made it (rules 14 and 15, said plainly)

**Neither Blender nor Blockbench could run in the session**, so these are the **script fallback**, not models made in an app: cubes with per-face UV in Python (`devil_canon_lib.py` + `devil_canon_specs*.py`), exported by `tools/blockbench/gen_devil_canon.py` to Blockbench's own source format (`tools/blockbench/devil_canon/<id>.bbmodel`, **not opened in Blockbench**; its rotation signs follow the Bedrock codec from memory) and, from the same data, to the game's geo, animation and PNG files. The sheets are painted in code (flat cube faces with painted faces, cloth lines, scales, brands) and are at 256x256 (density 2 texels per pixel for most, 1.75 for the statue); a hand-made Blockbench model would be better and can replace any of them one by one (the renderer only needs the file names).

### Worst-case cost (rule 11)

| Where | Worst case |
|---|---|
| Server, trial | unchanged. |
| Server, figure | one entity per ritual (at most `npc.max`, default 8, hard 64). The rise sends at most 68 dust and 34 haze particles per figure over 1.5 seconds (`DevilCanonRules.riseParticlesTotal`), at most 21 particles on any one tick (the landing burst), in 20 `sendParticles` calls (two per burst, 10 bursts), each one packet to the players near the circle; all through `PerfParticles`, which the 2000-per-tick budget caps whatever a crowd of rituals asks for. No scan, no chunk load, nothing after the rise. |
| Packets | none new for the model (the devil id and the state were already synced). The effect is at most 13 particle packets a second per nearby player during the 1.5 seconds of a rise. |
| Client draw | one model plus one glow pass per figure: at most **882 quads** (the biggest canon model, the Ghost Devil) each, 660 for the generic brute; at most 4 trial devils or 8 figures in the default setting (worst case about 3,500 quads for the trial, about 7,000 for eight figures at once, which needs eight rituals within 64 blocks of the viewer). Nothing is drawn beyond 64 blocks (the figure) or beyond the vanilla entity distance (the trial). There is no LOD copy of the geo models. |
| Allocation | none per frame in either renderer: a table of 33 fade colours is made once, the asset locations are in a map made once (26 x 3), the choice is a map lookup and one boolean read; the figure's turn uses the game's own `Axis.YP` helper (one small object per figure per frame, as before). `DevilCanonLooksTest` asserts the figure's `render` has no allocation. |
| Memory | 26 sheets + 26 glow masks (about 1.6 MB of PNG in the jar) and 26 geo + 26 clip files (about 3.7 MB of JSON on disk), each read when that devil is first drawn. The `.bbmodel` sources (about 9 MB, with the sheet embedded) and the previews (about 4 MB) are tools and docs, not in the jar. |

### NEW / REPLACED / KEPT (rule 5)

- **NEW**: 26 canon models, sheets, glow masks and clip files; `DevilCanonRules` (pure), `DevilCanonAssets`, `DevilNpcGeoRenderer`; `DevilNpc` as a GeckoLib entity with the rise effect; the clips `rise` and `roar` (also added to the generic brute's file); the options `bossLooks.devilCanonLooks` and `bossLooks.devilRitualFigure`; the generator, driver, previews and tests of this section.
- **REPLACED**: the **default look** of the trial devil (the shared generic brute is replaced by each devil's own model) and of the summoning figure (the player-model look with floating wings is replaced by the full-size canon figure). Both old looks stay: the brute for unknown devils and `devilCanonLooks = false`, the old figure behind `devilRitualFigure = false`. One real bug is fixed on the old path too: its player models are no longer a child's (`young = false`); that is a fix, not a removal, and it means `devilRitualFigure = false` no longer shows a child.
- **KEPT**: the zombie stand-in behind `devilTrialModel`, the generic brute and its 27 sheets, the Blender-mesh figure and the player-model look code, every server rule of the trial and the ritual (states, timings, caps, checks, the merge into the signer), the worn forms, all earlier models.

### Tests

`DevilCanonLooksTest` (pure, 12 checks): the canon list is the roster in roster order and nothing else; every model's bones are unique with valid parents, UVs inside its sheet, 40+ cubes and at most 1100 quads; every clips file has the five clips, loops idle only, moves only bones its model has, and the rise and roar last as long as the figure's states; every sheet is 256+ and different, every glow mask has lit texels and the sheet's size; no two geometries are equal; the `.bbmodel` sources are kept and the generator says it is the fallback; the trial and the figure read the same assets and the same rank scale; the figure is a GeoEntity renderer that faces the figure's yaw, allocates nothing in `render`, is behind the option, and the old figure is kept with `young = false`; both options are client configs, default on, with their lang keys; the fade, sink, shrink, slide and effect numbers behave and are bounded; the entity keeps its server rules, uses `PerfParticles` and no client class. `DevilTrialModelTest` now expects 5 animations in the brute's `.bbmodel`. Self-tests (`devil.phase2.trial`, `devil.npc`): the canon list matches the roster and its rank scales are bounded; the figure is a GeckoLib entity and its effect is bounded to the rise. Run in the session with javac and the JUnit launcher (no Gradle): the devil tests, `PerfSourceScanTest`, `ServerSafetyTest`, `ConfigFileNamesTest`, `GeoAssetsTest`, `DevilPlayerModelsTest`, `TextureAlphaTest`, `SafeAdditiveTextureScanTest`, `SoundsJsonTest`, the lang tests and `ParticleThemesTest` (200 + 15 tests, all green); the self-tests need a server and ran only as compiled code.

### Not verified

- **Nothing was seen in a game.** The Java compiles against the real classpath (javac on the changed classes) and the pure tests pass, but the figure, the clips on GeckoLib, the fade (translucent render type, the faded glow through the library's glow render type), the facing (the library turns a plain entity by nothing, so `applyRotations` is overridden with `180 - yaw`, from the bytecode of GeckoLib 4.9.2) and the particle look were not run. If the figure faces away from the caller, it is the one `180f -` in `DevilNpcGeoRenderer.applyRotations`.
- The clip signs on GeckoLib (a wing opening the wrong way is a sign in `extra_clips()` of `gen_devil_trial.py`), the `AutoGlowingGeoLayer` look at partial fade, the height of the rise against the circle, and every colour and face are unverified; the faces and glow marks are painted in code, clean but generic.
- The `.bbmodel` sources were not opened in Blockbench.

### Decisions for the owner

- Keep the script-made models, or have them redone by hand in Blockbench one by one (the Supreme devils first)? Any replacement needs only the same file names and clip names.
- Astaroth, the Hall of Hell demon, the God Hand and the Etherious need a better source than the wiki pages that could be fetched; send the Japanese-wiki text or reference art if you want them closer.
- Does the figure's size feel right? It uses the trial's rank scale on the canon heights (1.9 to 2.6 blocks before scale); a single number (`rankScale`) changes all of them.
