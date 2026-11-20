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
