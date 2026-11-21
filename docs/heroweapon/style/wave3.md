# Style wave 3: spears, katanas, carriages, Yata mirror, Phoenix sword

Third and last wave of the ranked weapon-style rework in `style_guide.md` (wave 1 = bows, wave 2 = `wave2.md`). Ten weapons, within the cap of 14. Done exactly like wave 2: the old meshes and skins are copied to a classic folder first, the new ones are built with the wave-2 palette rule (3 base materials + ONE named accent, unequal spacing, one hero feature, no gold unless the lore says gold), ids / grips / engravings / Better Combat files stay.

## How these were made (read this first)

- All ten models are **scripted bpy builds** (Blender 5.3 as a Python module, Cycles bake) through `tools/blender/bl_lib.py` and `forms_kit.py`. There was **no Blender window and no Blockbench** in this session, so nothing was hand-modelled in an app. Per rule 14: Blender-made = all 10 (scripted); made in Blockbench = none; could not be made in a real tool = none hand-sculpted.
- The stills in `docs/previews/style_wave3/` are **software-rasteriser stills** (`tools/relics/preview_mesh.py`, made by `tools/heroweapon/style/wave3_previews.py`), not game screenshots. Nothing here was seen in a running game.
- Gradle is not available in the session: the two changed Java files were compiled with javac and the tests run with javac + the JUnit launcher (see Tests).

## What changed

Shared: `tools/blender/wave3_kit.py` (NEW) keeps the previous mesh's `place` / `grip` header (`use_classic_headers()`), puts the `# blender-solid 4.55.0` mark back on rebuilt spears (HeroSolidTest) and reads `W3_SKIN` (a smaller skin to stay in the 350 KB budget).

| Weapon | Hero feature | Palette (accent) | What was removed |
|---|---|---|---|
| legendary_spear | long leaf head with a fuller and TWO hooked arms of unequal length; bare ash shaft, ONE hide binding | steel + wood + iron + hide (the red gem) | ten fused ornament rows, gold bands |
| legendary_spear_curse | the same builder in black iron, a thorned vine up the shaft with a magenta line in it, a third barb, the stone turned into an eye | steel + wood + iron + hide (magenta, the sin accent) | the curse ornament rows |
| legendary_spear__portal_spear | an obsidian ring BROKEN open with uneven thickness round a void funnel (three violet arms), one pearl in the socket, slim dark blade | obsidian + blade steel + wrap (the portal void, violet) | gold, cross ring, gem rows |
| legendary_spear__dragon_minute_hand | one long clock-hand needle, dragon-scale socket with one long and one broken horn, Fitoria's feather as the one-off | scale + hand + bone + feather + cord (ember accent) | scale rows, studs |
| vassal_katana__siderite_katana | dark meteoric blade (Widmanstaetten veins painted in the iron), ONE glowing tempering line along the edge, three fused crust lumps of different size, three shards still circling the upper blade | iron + dark + wrap + crust (hamon blue) | star halo, 18 star plates, orbit ring, gem, star cord |
| vassal_katana__white_tiger_katana | pale blade, a row of eleven tiger stripes (unequal) along the edge only, three graded claw bites, a small turned paw guard (four toes of different size, four different claws, ONE amber eye), the striped tail | steel + dark + ivory (amber) | phantom blade, fur ruff, tiger face, gold |
| vassal_carriage__war_carriage | ONE big scarred oak hull open at the back (different torn edge on each side), three iron straps, two irregular rivet rows, three arrows stuck in the side, plain iron-shod wheels, ONE standard with a torn red banner planted in the hull | oak + iron + hide (lamp amber) | ram, roof, crenellations, hung shields, chains, spikes, spiked wheels, brass, ember slits |
| vassal_carriage__queens_chariot | pearl-white open chariot, thin gold rim, a wing of seven feathers on the left and one of four on the right (not twins), three crest feathers, one crown of five prongs over ONE rose stone on a mast | pearl + gold + velvet (rose) | scale plates, scrolls, lamps, jewelled wheels, feather spokes |
| vassal_mirror__yata_mirror | sacred BRONZE mirror, eight-lobed outline (three lobes proud, one chipped), heavy patinated rim, ONE engraved sun lit amber, one half-circle of straw rope with three paper shide of unequal length | bronze + patina + straw (sun amber) | gold, kaleidoscope facets, 32 rays, 8 jewels, studs |
| legendary_sword__phoenix_sword | ONE blade graded black -> red -> ember painted into the skin, flame-cut edges with unequal notches, ONE swept fan of five feathers on the left and a plain quillon on the right, tiny ruby, one pommel plume | heat steel + iron + hide + plume (fire glow) | gold heart guard, gold scrolls, two 3-tier wings, plumage plates, runes, ember cloud |

## Before / after

Sameness = `tools/heroweapon/style/sameness_report.py` (rank-normalised across all Blender-made weapons, so a score also moves when other weapons change; lower = less generic). Before = baseline at the start of this wave, after = after the final builds. `legendary_spear_curse` is not in the scored set (curse look).

| id | before | after | note |
|---|---|---|---|
| legendary_spear__dragon_minute_hand | 66.0 | 26.7 | |
| legendary_spear | 56.8 | 35.0 | profile IoU 0.96 with its own curse look (same builder on purpose) |
| legendary_spear__portal_spear | 56.7 | 46.0 | profile IoU 0.59 with other spears: same length and ring-head class |
| vassal_katana__siderite_katana | 55.6 | 50.5 | see the katana note |
| vassal_katana__white_tiger_katana | 51.0 | 56.5 | WORSE; see the katana note |
| vassal_carriage__war_carriage | 62.2 | 43.0 | |
| vassal_carriage__queens_chariot | 47.0 | 49.3 | slightly worse: profile IoU 0.44 with the war carriage (same hull box and wheels, the size class) |
| vassal_mirror__yata_mirror | 58.3 | 40.6 | |
| legendary_sword__phoenix_sword | 61.1 | 42.3 | |

Katana note: `HeroKatanasTest` (4.61.0) fixes a true katana for both: blade 1/60 to 1/25 as wide as the mesh is long, the whole outline at most 1/13 across, and the lore parts `menuki`, `tassel`, `kashira_gem` (tiger) / `menuki`, `orbit`, `tassel` (siderite) as real parts. That makes the two silhouettes close to each other (IoU 0.64) and near-symmetric, which the metric counts against them (mirror 0.46 for the tiger), and the grip, gem and stripe parts are the family's. The scores of the two katanas did not get under 45; the tiger got worse by rank (the other weapons improved around it). A different guard outline would need that test's rules changed, which is the owner's call (see the decision below).

## NEW / REPLACED / KEPT (rule 5)

REPLACED (same id, same place line, same length class; the old mesh and skin kept as classic copies): all ten weapons above. New builders: `forms_spear_w3.py`, `forms_katana_w3.py`, `forms_carriage_w3.py`, `forms_mirror_w3.py`, `forms_phoenix_w3.py`. The old builders (`forms_legendary_spear.py`, `forms_vassal_katana.py`, `forms_vassal_carriage.py`, `forms_vassal_mirror.py`, `forms_exemplars.py`) are not touched.

NEW: `tools/blender/wave3_kit.py`, `tools/blender/forms_manifest_style_wave3.json`, `tools/heroweapon/style/wave3_previews.py`, the setting `vassalForms.classicStyleWave3` (client config, default false), the folders `weapon_meshes/classic_style3/` and `textures/entity/weapon/classic_style3/` (10 meshes, 10 skins, 10 glow skins; snapshot of HEAD before this wave), the stills, this document.

KEPT (not touched): all ids, engravings (`WeaponEngravings`), Better Combat files (no new ids, no swing rig among the ten, `build_swing.py --check` all ok), the Time Magic VFX, version, changelog.

The new option mirrors `classicStyleWave2` exactly (`WeaponRenderer.plainMesh`: same re-read when switched, a missing copy falls back to the new mesh). `classicVassalForms` and `classicHeroWeaponModels` still win first (they draw the earlier carriages / katanas / pre-3.79 meshes).

## Grips and hold audit

Every rebuilt mesh keeps the old `place` line and `grip` line. After the rebuild the finder moved the grip point of eight meshes (spears, katanas, mirror, phoenix sword), so `install_solid_meshes.pin_grips()` gave them a `gripxyz` line with the committed HEAD point. The two carriages needed no pin: the war carriage keeps its `HOLD_FIXES` fraction 0.09, the queen's chariot 0.68. Two carriage changes were made for the hold audit's NONE rule (the item display's pivot, the centre of the unit cube, must lie within 0.15 of a surface): the war carriage's standard is planted in the middle of the hull and the queen's crown mast stands from the floor at f = 70. Hold audit after: 145 meshes, **0 flagged** (at HEAD `vassal_carriage__queens_chariot` was flagged tp_right NONE, now fixed). `tools/weapon_grip/grip_golden.json` and `hold_golden.json` were regenerated (19 and 30 changed entries, all ten ids of this wave plus nothing else).

## Budgets and worst-case cost (rule 11)

Quad lines (body + glow copies) and skin + glow PNG, before -> after. All under the budgets (5000 lines, 350 KB):

| id | quad lines | skin+glow |
|---|---|---|
| legendary_spear__dragon_minute_hand | 4962 -> 1718 | 335 -> 330 KB |
| legendary_spear | 1179 -> 1640 | 160 -> 340 KB |
| legendary_spear_curse | 2247 -> 2092 | 166 -> 323 KB |
| legendary_spear__portal_spear | 4833 -> 2307 | 339 -> 322 KB |
| vassal_katana__siderite_katana | 3594 -> 2091 | 331 -> 263 KB |
| vassal_katana__white_tiger_katana | 3536 -> 2532 | 278 -> 226 KB |
| vassal_carriage__war_carriage | 3937 -> 1923 | 289 -> 271 KB |
| vassal_carriage__queens_chariot | 4598 -> 2065 | 305 -> 232 KB |
| vassal_mirror__yata_mirror | 4068 -> 2909 | 333 -> 195 KB |
| legendary_sword__phoenix_sword | 4941 -> 1857 | 325 -> 222 KB |

The base spear and its curse look grew (1179 -> 1640 lines, 160 -> 340 KB skin), the other eight shrank. Nothing new at run time: no new packets, no per-tick work, no entities, no particles. The only Java change is one client config boolean and one extra `Set.contains` per mesh load in `WeaponRenderer.plainMesh` (cached afterwards in `MESHES`); worst case the same one extra mesh read per id when the option is switched.

## Tests (javac + JUnit launcher, no Gradle)

Run and green (all in one sweep, 111 + 16 tests): HeroSolidTest, GripTableTest, WeaponSizesTest, HeroAssetsTest, FormMeshFilesTest, HoldAuditTest, WeaponAttributesTest, ClassicVassalFilesTest, SwingSourceScanTest, WeaponClassesTest, ConfigFileNamesTest, ServerSafetyTest, HeroKatanasTest, ThinKatanasTest, PerfSourceScanTest, CarriageMeshContractTest, FormModelsSourceScanTest, SpearFormsFileTest, RegistryScanTest, plus MeshCapTest, SwingMeshFilesTest, WeaponMeshesTest, SizedMeshNormaliserTest, UpgradedMeshNormaliserTest, RelicRenderCoverageTest, RelicHandCoverageTest. `HeroClientConfig.java` and `WeaponRenderer.java` compile with javac (`-proc:none`, full classpath). `build_swing.py --check`: all ok.

Not run: the full Gradle build, the CI server smoke test, any in-game check (hand pose, scale, glow, the new option switching live), `MeshPackTest` and the other tests that need the NeoForge classpath beyond what javac had.

## Risks and decisions for the owner

- Katanas: `HeroKatanasTest` forces thin silhouettes and lore parts, so their sameness scores stay above 45 (tiger 56.5, siderite 50.5). Decision: keep the 4.61.0 rules, or relax the guard-outline rule so the paw guard can be bolder.
- Queen's chariot score rose 47.0 -> 49.3 (carriage hull box and wheels are the size class and are shared with the war carriage).
- `legendary_spear` and `legendary_spear_curse` are one builder (IoU 0.96): intended (curse look of the same spear).
- Heavy bakes were made at 1536 px (carriages, katanas, mirror, phoenix) or 1024 px (curse, portal, dragon) to fit the session; the base spear at 2048. All skins are palette PNGs.
