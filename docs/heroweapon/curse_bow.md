# The Bow of Pride gets its own stave (4.68.0)

Owner decision (answering the open point of 4.60.0, `bow_designs.md` decision 1): `legendary_bow_curse` no longer shares the base bow's stave.

Honesty notes. The model is a scripted `bpy` build (Blender as a Python module, no Blender window, no Blockbench): `tools/blender/forms_legendary_bow_curse.py`, on the same pipeline as the third-look bows (`forms_legendary_bow.py` driver and bake, `forms_legendary_bow_v3.py` stave function and connectivity check, `bl_lib.py`). The stills in `docs/previews/curse_bow_v2/` are software-rasteriser stills (`tools/relics/preview_mesh.py`), not Cycles renders and not game screenshots. Nothing was seen in a game. No canon lookup was needed (the lore is the existing Bow of Pride: pride, violet sin-light, an eye, horns), so no wiki source applies.

## What it is

| | Before (4.64.0) | Now |
|---|---|---|
| Stave | the base bow's slim symmetric arc, violet | riser in a notch (x 24); each limb swells forward into a razor-edged horn blade (x 49 at y +-40) and falls back to the tip (x 6): an M-shaped, horned bow |
| Limbs | flat stave, steel fin | blade section (sharp forward edge, thick spine), 3 collars (tarnished gold, iron, steel), a violet sin-vein of light on both faces, 5 graded barbs per limb on the outer edge |
| Tips | small horn curling back | steel nock and a long horn that hooks forward past the tip |
| Riser | gold, eye jewel, five crest spikes, dark feather wings | black iron, almond eye in a steel bezel (magenta stone, slit pupil), crown of three blades pointing forward into the notch, violet-corded grip |
| Palette / glow | black and violet | same: black-violet horn, violet and magenta emission, light-violet string; glow skin baked from the emissive parts |

Kept: id `legendary_bow_curse`, the place line (`0.92426 0.92426 225 0.00625 -84.239`, so the size is unchanged: guard length 1.128 blocks, BowDisplayGuardTest 0.9 to 1.4), lore, engravings (`WeaponEngravings` keys the item, not the mesh), Better Combat entry (ranged: not melee), weapon class `bow`, pull-stage structure (`limb*` flexes, `string*` and `arrow*` move; rest + `_pull1..3`, the solid barbed arrow).

## Outline overlap (IoU of the profile masks, `similarity_after.txt`)

Curse bow against every other bow: arrow_shower 0.17, frozen_rain 0.15, sagittarius 0.15, hunters_bow 0.14, base bow 0.14, meteor_bow 0.14, bind_arrow 0.14, eagle_piercer 0.13, bird_hunting 0.12, scouts_bow 0.04. Highest 0.17 (limit 0.46). Before: base bow 0.88, arrow_shower 0.44. Mean over all 55 pairs 0.258 (4.60.0) -> 0.222; the highest pair of the whole set is now 0.46 (bird_hunting ~ eagle_piercer, meteor_bow ~ eagle_piercer, unchanged).

## Connectivity

`connectivity()` (BVH overlap or < 0.8 units, union-find anchored on `riser_core`) runs in the builder and fails the build on a floater: 64 objects, floating: none (the string and the nocked arrow are free by design).

## Cost (rules 9 and 11)

Client drawing only; no server code, no packet, no tick work, no config. Worst case per bow in hand: the draw stages, **3683 quad lines** (rest 3558; body plus glow copies; limit 5000). One bow is drawn per player; far players' items are skipped by the existing perf budgets. Skin + glow skin 306 KB together (limit 350 KB), one 1024 px skin in memory. Detail objects (rivets are not used here; the vein, collars and barbs are real geometry) cost the quads above.

## Keeping the old look (rule 2)

The `classic/` folder already holds the pre-3.83 hand-made curse bow, so the shared-stave look has its own folders: `weapon_meshes/shared_stave/legendary_bow_curse{,_pull1..3}.txt` and `textures/entity/weapon/shared_stave/legendary_bow_curse{,_glow}.png` (copied byte for byte by `tools/heroweapon/install_bow_curse.py`, never overwriting). `WeaponRenderer.readFlatOld` (the path of `visual.heroSolidParts = false`) reads them for the ids in the new `SHARED_STAVE_BOWS` set and falls back to the new mesh if the skin is missing; `classicHeroWeaponModels` still shows the pre-3.83 look. `legendary_bow_curse` is also appended to `THIRD_LOOK_BOWS`, same pattern as 4.59.0 (the id was already in `RETIRED_OLD_SKINS`, so the flat-part meshes of before 4.55 are not drawn for it either way).

## Files

- NEW: `tools/blender/forms_legendary_bow_curse.py`, `tools/heroweapon/install_bow_curse.py`, `weapon_meshes/shared_stave/*` (4), `textures/entity/weapon/shared_stave/*` (2), `docs/previews/curse_bow_v2/` (before, after, all bows, similarity), this document.
- REPLACED: `weapon_meshes/legendary_bow_curse{,_pull1..3}.txt`, `textures/entity/weapon/legendary_bow_curse{,_glow}.png`; `tools/weapon_grip/grip_golden.json`, `hold_golden.json` (regenerated: only the curse bow entries moved; the pinned `gripxyz` of the old riser was dropped, the grip is found from the new mesh like the other third-look bows).
- KEPT: the older `builders` (`legendary.py` `bow(curse=True)` and `art_hart1.py` still run), classic copies, flat_parts_old meshes, all other bows.
- Edited (append only): `client/WeaponRenderer.java` (`SHARED_STAVE_BOWS`, one branch in `readFlatOld`, one id in `THIRD_LOOK_BOWS`).

## Checks run

`register_form_meshes.py`, `bettercombat/inventory.py` (manifest unchanged), `weapon_grip/grip_table.py --write`, `hold_audit.py --write` (flags only the two unrelated vassal items). JUnit through the console-less launcher over `javac` of the real test classes, 119 tests, all passed: HeroSolidTest, BowDisplayGuardTest, BowFormsFileTest, FormMeshFilesTest, WeaponAttributesTest, ServerSafetyTest, PerfSourceScanTest, GripTableTest, HoldAuditTest, WeaponSizesTest, ClassicVassalFilesTest, HeroAssetsTest, ConfigFileNamesTest, WeaponMeshesTest, SolidCoresTest, ThinKatanasTest. Not run: Gradle build, MeshCapTest (needs the Minecraft jars), the game. `WeaponRenderer.java` was only parse-checked with `javac` (no syntax errors; the remaining errors are the missing Minecraft symbols), not compiled.
