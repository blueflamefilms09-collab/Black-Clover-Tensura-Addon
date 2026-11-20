# Wings on the back (visual.wingsOnBack)

Owner report: "the wings are still not facing the right way. They should be on the player's back and facing towards the back, like any of the other wings in my mod. And that should be a massive fix for anything
double related or any other kind of transformations in my mod." The screenshots (Devil Union / "Supreme Devil") show dark-blue bat wings that spread at the sides and angle FORWARD past the body.

Nothing here was seen in a game. Everything below was checked with numbers (forward kinematics of the geo files), software mock-ups and unit tests.

## 1. The convention (one rule for every wing worn on a player)

Frame of a geo file: pixels, y up, the front on the -z side, so the BACK is +z. The torso's back face is at z = 2.

| Quantity | Rule | Corundum (reference) | Djinn layer | Devil wings before |
|---|---|---|---|---|
| Hinge | shoulder blade: `|x|` about 2 to 3.4, z at or behind 1 (in practice 2.4 to 3.4) | (2, 21, 3) | (1.8, 3.0, 2.6) | (2.8) |
| Centroid vs hinge (`dz`) | behind the hinge: `dz >= -1.5` (the right wings are 0 to +9) | +4.2 | back | **-7.9 to -9.1** |
| Tip vs hinge (`tipDz`) | not in front of the hinge by more than 2 px | +11.3 | back | **-16 to -18.5** |
| Spread wing | tip further out than the hinge | yes | yes | yes |
| Left wing (+x) turn | NEGATIVE y rotation (right wing positive) | wL ry = -24 | `YP.rotationDegrees(-side * sweep)` | **+y (wrong)** |

The numbers live in `client/geo/WingFrame.java` (`BACK_PLANE_Z`, `MIN_PIVOT_Z`, `FORWARD_DZ`, `FORWARD_TIP_DZ`) and are checked by `WingsOnBackTest`.

## 2. What was wrong (root cause)

The generator of the Black Clover and devil looks, `tools/bc_playermodels/bcparts3.py` `wing()`, writes each wing card with the cube rotation `(0, s*(sweep+14k), s*(rise-3k))`. For the left wing (s = +1)
that is a positive y turn, and in vanilla's rotation order a positive y turn on the +x side swings the tip toward -z, the FRONT. The generators of the Dark, Flame, Lightning and Water armour wings, of the
Spirit Dive fire and saint wings and of the Noelle Valkyrie, Dragon and Beast wings carry the same sign. So the files draw the right SHAPE leaning the wrong way. `GeoDraw` draws exactly what a file says, so it
was drawn that way, and the old numpy previews of `tools/bc_playermodels/preview_dv.py` share the same math, so the old preview PNGs showed forward wings too and nobody saw it.

The wings that already sit right (Corundum, the Djinn layer, the Weg wings, the mode armours, the bwf beast wings, the sdj djinn wings, the old devil Blender meshes, the relic wing models) use the opposite sign.

## 3. The fix (one shared helper, no per-asset hacks)

* `client/geo/WingFrame.java` (pure, no game classes): the convention, the forward kinematics, and `fix(model)`: every wing group that is hinged on the back but sweeps to the front is reflected in the plane
  `z = its hinge`. The same wing, the same size, now behind. Bone and cube `z`, the x and y turns of bones and cubes flip sign; a z turn is unchanged by a mirror in z. A wing that already follows the
  convention is not touched (no copy).
* `GeoModelData.onBack()` caches the fixed deep copy of a model (made once, on first draw; the original is never changed). `GeoModelData.copy()` is the deep copy. `Bone.zMirrored` makes the animation of a
  mirrored bone mirror with it (x and y turns, z position), so a flapping clip keeps flapping the right way.
* `GeoDraw.draw(...)`: `if (space == Space.PLAYER && WingFrame.isOnBack()) m = m.onBack();`. That is the single hook. Everything worn goes through it: the Devil Union / devil form look path
  (`DevilFormLayer` -> `DevilExtras` -> `GeoDraw`), the devil NPC and ritual figure (same `DevilExtras` path, so the other workers' devil-model files needed no change), the boss and NPC looks (`ExtrasDraw`),
  the avatar screen, and the aura pieces (`NoelleFormRig`, `SongArmourKit`, `ShakudoPieceKit`, `SpiritDiveAnime`). Props (`Space.PROP`) are never touched.
* Which bones are wings: a bone named `wing...`, and in a wing piece file (`dark_arm_wing_l`, `nvl_arm_wings2`, `spiritdive_arm_fire_wingbud_r`) the first non-anchor bone under the file's bare `root`.
  A piece not at the back (hinge z below 1: a chest emblem, an arm blade) is never turned.
* Option `visual.wingsOnBack` (CLIENT, default true) in `config/nusmp-wingsonback-client.toml`: `WingsOnBackConfig`, mirrored into `WingFrame.setOnBack` on load/reload like `FlatPropsConfig`. **Off = the
  files exactly as drawn before** (the old behaviour, kept; nothing was deleted or regenerated).
* Why not fix the files: the generators and ~50 geo files stay as they are (rule 2: add, keep the old). The runtime fix is idempotent, so the day the files are regenerated with the right sign nothing
  double-flips. See owner decision 1.

## 4. Forms

48 geo files fixed (of 183 worn files that have wings, 377 wing groups, 104 of them forward):

| Family | Files | Count |
|---|---|---|
| Devil roster looks (the Devil Union / devil form, every double-magic devil with a look of its own) | `pmx_dv_` liebe, lucifero, beelzebub, astaroth, megicula, lucifugus, sebastian, daemon_noir / blanc / jaune / violet / rouge / vert / bleu, kin_of_lucifer, god_hand, etherious | 17 |
| Black Clover looks that use the same generator | `pmx_bc_` lucifero, lucifero_form2, devil_avatar (+ form2, megicula, megicula_form2), zagred_form2, zagred_legacy_form2, heath_grice_form2, morris_libardirt_form2, dante / vanica / zenon_zogratis_form2 | 13 |
| Armour wings of the auras | dark, flame, lightning, water `_arm_wing_l/r` | 8 |
| Spirit Dive | fire wing and wingbud, saint wing, `_l/r` | 6 |
| Noelle Valkyrie, Dragon, Beast | `nvl_arm_wings`, `nvl_arm_wings2`, `ndg_arm_wings`, `nbk_arm_wings` | 4 |

Already correct, drawn as the file says (checked by `WingsOnBackTest.theWingsThatWereRightAreNotTouched` and the survey): `pmx_lucifero`, `pmx_lucifero_form2`, `pmx_king`, `pmx_zagred_legacy`, the `pmx_dg_*` and
`pmx_abyssal_*` and `pmx_foundry_*` wing bearers, `corundum_wings`, `wing_back`, `paladin_wings`, `avatar_wing_*`, `bwf_*_arm_wings`, `sdj_*_arm_wing_*`, `spiritdive_arm_wind_wing_*`, `ndg_arm_wings2`,
`nbk_arm_wings2`, `soul_armour`.

Already correct, checked by data or by reading the code only (no drawing hook needed):

* Weg wings (the generic bat wings of a double-magic devil without a look of its own): `DevilModels.Wings` hinge z 2.6, yaw `-(0.2 + 0.3 spread)`; `WegLayer` hangs them on the body part.
* `DjinnArmorLayer` (hinge z 2.6, yaw `-side * sweep`), `ModeArmorWindV2` (z 2.7), `ModeArmorLayer.wings` (z 2.3, yaw `-sweep`), Noelle Valkyrie V2 meshes (`ModeArmorValkyrieMesh`).
* The old devil Blender meshes (`entity_meshes/devil_*.txt`, the option `devilPlayerModel=false`): mesh frame, wings behind the torso.
* The worn relic wing models (`relic_worn/wing_demon`, `wings_dragon`, `wings_feathered`, `wings_moth`, `wings_valk`, `wings_wind`): hinge z 2.4 to 2.8, mass behind the hinge.
* Outside those families no other geo file of the mod has a forward wing group (194 files, 433 groups, 0 forward).

## 5. How it was checked

* `src/test/java/.../client/geo/WingsOnBackTest.java` (pure JUnit): the generator sign (+y forward, -y back) on a synthetic card; the fix is an exact mirror (same size, same x and height, z mirrored about the hinge)
  and idempotent; the original is untouched; a chest piece is left alone; every worn wing file of the mod follows the convention after the fix (at least 120 files, 250 wing groups); the named devils (liebe,
  lucifero, beelzebub, astaroth, megicula, lucifugus, sebastian, the seven daemons, kin_of_lucifer, god_hand, etherious), the Black Clover looks and the armour wings are forward in the file and on the back after;
  the Weg, Djinn, mode-armour and relic wings and the old devil meshes hinge behind the torso; the option is a CLIENT config (default on, file name, hook line in `GeoDraw`); no non-client class uses the helper;
  `WingFrame` has no game classes and the bone loop allocates nothing.
* Existing tests run alongside (all green): `ConfigFileNamesTest`, `ServerSafetyTest`, `PerfSourceScanTest`, `GeoAssetsTest`, `GeoDrawCacheTest`, `DevilPlayerModelsTest`, `BcPlayerModelsTest`,
  `BossesBcWave2Test`, `BcNpcPlayerModelsTest`, `PlayerModelDataTest`, the Djinn tests (`DjinnLookTest`, `DjinnBoneSetTest`, `DjinnDesignReworkTest`, `DjinnUniqueAssetsTest`), `WornPlanTest`,
  `WornPlanCoverageTest`, the relic hand tests (`HandTransformsTest`, `HandCatalogTest`, `HandHoldTest`), `DevilFormAssetsTest`, `DevilFigureFacingTest`, `DevilSourceScanTest`, `BossBarLangTest`,
  `CompanionLangTest`. They were run with javac and the JUnit console launcher, not Gradle.
* `tools/wings_on_back/wing_frame.py` is the Python twin of `WingFrame` (`python tools/wings_on_back/wing_frame.py [regex]` prints dz and tipDz before and after for every worn wing; its numbers equal the
  Java ones).
* Previews: `python tools/wings_on_back/preview_wings.py` writes `docs/previews/wings_on_back/`: `overview.png`, `reference_corundum_wings.png` and one PNG per fixed form
  (`pmx_dv_lucifero.png` is the devil hero). **Software mock-ups of the cube corners (flat side and top silhouettes next to a torso outline), not the game**: red = before, green = after, dot = hinge.

## 6. Cost (rule 11)

Client only. No server code, no packets, no new entities, no new quads (the fixed copy has exactly the same cubes). A one-time deep copy of a wing model on its first draw (about 100 to 300 cubes, a few tens of
KB each; at most the 48 fixed models, under 2 MB in the worst case). Per draw call: one volatile boolean read and one cached field. No per-frame allocation. The existing GeoDraw budgets and distance culling
stay as they were.

## 7. Caveats

* Nothing was seen in a game. Classes were compiled with javac against the cached classpath, not built with Gradle (none available in the session).
* The mirror is about the hinge plane, so the wing keeps its size and height and only flips front/back. An animation that moves a wing in x/y rotation or z position is mirrored with it (no wing clip was
  watched in a game, so a flapping clip is the first thing to look at).
* Wing cards are single-face and use no-cull render types (`entityCutoutNoCull`, `entityTranslucent`), so a mirrored card is visible from both sides. A render type that culls would show them one-sided.
* `ndg_arm_wings` / `nbk_arm_wings` lean only about 2.5 px at a 27 px span; they are mirrored too (consistency with the rule), the visible change is small.
* Arm-anchored pieces and props are left alone (they are not on the back).

## 8. Owner decisions

1. Fix the generators (`bcparts3.py` `wing()`, the armour and Spirit Dive wing generators: flip the sign of the y sweep) and regenerate the geo files? Not done (rule 2, and the runtime fix covers it); the
   Java fix is idempotent so it is safe to do later, then `visual.wingsOnBack` can be retired.
2. Keep `visual.wingsOnBack` as a permanent option, or drop it once the files are right.

## NEW / REPLACED / KEPT

* NEW: `WingFrame`, `WingsOnBackConfig`, `GeoModelData.onBack()/copy()/zMirrored`, the hook in `GeoDraw.draw`, `WingsOnBackTest`, `tools/wings_on_back/`, `docs/previews/wings_on_back/`, this doc.
* REPLACED: nothing.
* KEPT: every geo file, generator, texture, animation and layer as it was; the old drawing is one option away (`visual.wingsOnBack=false`).
