# Weapons are held on the grip (round 1.99.0, key `weapon_grip`)

Owner report (1.110.0 build, third person): "weapons are not being held on the hilt". One screenshot: a dark, long, curved blade held by the middle of the blade, near the
guard, with its chain pommel hanging far behind the hand. The other: a big crystal greatsword standing in front of the chest, its hilt down at the belt, the hand not on it.

Before / after sheets (hand = red ring, grip point the game now uses = green cross, same camera in both columns): `docs/weapon_grip/before_after.png` (16 weapons: the Genesis
Demon-Slayer, Elsdocia, a katana, a dagger, a spear, a hammer, a staff, a greatsword, an axe, two scythes, a hammer with a chain pommel, a rapier, a bow, a held mirror, a
Djinn lance), `docs/weapon_grip/left_and_first_person.png` (left hand and first person). Made with `python tools/weapon_grip/preview_grip.py` (see "Tools").

## 1. Root cause

A weapon mesh is drawn by `client/WeaponRenderer.renderByItem`, which does `translate(ox, oy, 0.5); rotateZ(angle - 90); scale(scale); translate(0, -ymin, 0)` (1.110.0 lines
214 to 217): the mesh is laid on the *old 16 x 16 sprite's diagonal*, butt at the sprite's low corner. The hand never enters into it. Where the hand is comes from the model json
alone: vanilla applies the `display` block as `T(t/16) * R * S(s) * T(-0.5)`, so the point of the item's unit cube that sits exactly at the hand is

    H = 0.5 - (1/s) * R^-1 * (t/16)            (the left hand mirrors the entry: x translation, y and z rotation change sign)

For the `[0, -90, 55]`, `[0, 4, 0.5]`, 0.85 block that every weapon json started from, H is (0.238, 0.361, 0.5): the spot where a *sprite* sword's hilt is. For a flat sprite
that is the hilt. For a 3D mesh stretched along the same diagonal it is whatever part of the mesh happens to lie there. The size rule (1.34.0, `weapon_classes.json`) and the
1.110.0 size pass (`tools/relics/weapon_sizes.py`, `WeaponRenderer.sizedResource`) then made the meshes 2.2 to 3.6 blocks long, but they grow the mesh *about a fixed fraction of
its length* (`OversizeGuard.GRIP_FRACTION = 0.25`, `WeaponClasses` `grip` 0.20 to 0.40, `tools/relics/display_presets.py:75` and `:148`), which assumes the hilt is a quarter of the
way up. The json translation (`4`, `5.6`, `6.88` ... px) was scaled with the size so H stays at the same quarter to third of the length. The real hilt is in the lowest tenth
(a greatsword's grip is 10 to 15 percent up), so the hand lands on the guard or the lower blade:

* `tools/weapon_grip/grip_report.py` (348 meshes, third person, right hand): the old hand lies at a median 0.29 of the length from the butt (range -0.28 to 0.59); the grip is at a
  median 0.12 for the swords. The hand is a median 0.21 blocks from the grip as a viewer sees it, over 0.2 blocks off for 191 of the 348 weapons and over 0.4 blocks off for 43.
* The worst cases are the long ones: the Djinn sabre of Seere (hand at 0.51 of 2.8 blocks: the middle of the blade, 1.09 blocks from the hilt), the Overprotective Halberd (0.83),
  Demon / Licht Destroyer (0.76), Elsdocia legacy (0.66), Blackrazor (0.59). Elsdocia itself: hand at 0.245 of 3.11 blocks, on the crossguard (the owner's second screenshot).
* The same placement is repeated in `client/relics/item/vu/VuRenderer.place` (`pose.translate(mesh.ox, mesh.oy, 0.5f)`, the upgraded Genesis weapons, which are the default look:
  Elsdocia, Blackrazor, Starsplitter, ...), and in `DemonSlayerMesh.draw` (`translate(0.516, 0.523, 0.5)`, the pixel Demon-Slayer).
* First person is the same story with a different camera: `display_presets.py` keeps "the point 25 percent up" at the spot a sword's hilt occupies.

The 1.110.0 size changes did not move a pivot; they made an old, hidden mismatch large (a 0.25 fraction of 1 block is 25 cm, of 3 blocks 75 cm).

## 2. What changed

The renderer now asks "where is the grip of this mesh" and "where is the hand in this context", and translates the mesh so the first lands on the second. Scale does not matter:
the grip point stays put, the mesh grows or shrinks around it.

| Piece | What |
|---|---|
| `visual/grip/GripFinder` (pure) | the grip point of a mesh, sources in order: a `gripxyz x y z` line of the mesh; a per-weapon fraction of `weapon_grips.json`; the mesh's own `grip <fraction>` line (from the part names, `tools/relics/mesh_lib.py`); for sword-like classes (dagger, shortsword, longsword, rapier, katana, sabre, dadao, greatsword, cleaver) the middle of the handle read from 64 cross-sections (the narrow stretch between the pommel and the first section 1.8 times wider, the guard); else the class grip fraction (`weapon_classes.json`: 0.20 sword, 0.33 spear, 0.40 staff, 0.50 bow and held items). A sword grip is kept between 0.06 and 0.5 of the length. |
| `visual/grip/GripMath` (pure) | `handPoint` (the formula above), `firstPersonTarget` (where a sword's hilt sits in first person, the rule of `display_presets.py`), `cubePoint`/`shift` (mesh to unit cube and the translation that lands the grip), `firstPersonShare` (see below) |
| `visual/grip/GripState` (pure) | the grip of one loaded mesh plus the cached shift for the four hand contexts (third / first person, right / left hand) |
| `visual/grip/GripTable` (pure) | `assets/nusmp/weapon_grips.json`: `classOf` (grip class of weapons `weapon_classes.json` does not list: the 72 Djinn weapons by kind), `points` (grip point of the 17 upgraded meshes, their effect groups would blur a profile), `fractions` (14 per-weapon fractions read off the sheets) |
| `client/WeaponGrip` | glue: reads the table and the `display` block of the item model json, maps `ItemDisplayContext` to the four contexts; kill switch `-Dnusmp.weaponGrip=off` (client JVM) restores the 1.110.0 placement exactly |
| `client/WeaponRenderer` | `Mesh.grip`; one `pose.translate(shift)` before the old placement in the hand contexts; the oversize guard of first person now shrinks about the point the grip lands on; `gripxyz` parsed; remembers when the normaliser turned the mesh over (its `grip` line then counts from the other end) |
| `client/relics/item/vu/VuRenderer.place` | the same shift for the upgraded meshes (static body group, lod mesh uses the same point) |
| `client/DemonSlayerRenderer` | the same for the pixel Demon-Slayer (middle of the cloth grip, mesh y -12) |
| `visual/grip/WeaponGripSelfTests` | self-test `item.weapon_grip` (pure; runs on the dedicated server), registered by one line in `NUSMP` |

Context rules. Third person: the grip lands exactly on H, right and left hand, any size. First person: vanilla draws no arm for a held item, so only the item's place matters;
the grip goes to the spot a sword's hilt occupies, as far as the weapon stays in view (the camera bounds of `display_presets.py`: corners in front of the near plane, tip in the
view cone). `firstPersonShare` takes the whole shift when that holds, else the largest share that does (bisection), else none; so a first-person weapon is never worse placed
than in 1.110.0. Ground, item frame, head and inventory slot are untouched (they have no hand). Shields keep vanilla's frame (`ShieldFrame`), the two worn-on-the-hand weapons
(`HandWrap`) keep their own origin.

Relic hand models (`client/relics/hand/HandItemRenderer`, `relic_hand/transforms.txt`): these already hold the object by a named point (`hold=hx,hy,hz`, fractions of the object's
half extent, put on the hand origin whatever the size), so they needed no change; `HandHoldTest` now checks that every hold point of every style and use line lies within the
object. `applySpritePreset` (sprite fallback) is the flat-sprite transform and is kept.

## 3. NEW / REPLACED / KEPT

| | |
|---|---|
| NEW | `visual/grip/*` (5 classes), `client/WeaponGrip`, `assets/nusmp/weapon_grips.json`, `tools/weapon_grip/` (`grip_lib.py`, `grip_table.py`, `grip_golden.json`, `preview_grip.py`, `grip_report.py`), `docs/weapon_grip.md`, `docs/weapon_grip/*.png`, tests `GripFinderTest`, `GripMathTest`, `GripTableTest`, `HandHoldTest`, self-test `item.weapon_grip`, mesh directive `gripxyz`, `WeaponClasses.isListed/named` |
| REPLACED | the placement of a 3D weapon in a hand context (old: mesh on the sprite diagonal, hand wherever the json puts it; new: grip on the hand). Only in code; every mesh, texture, json and registered id is untouched |
| KEPT | all meshes and model jsons (display blocks included), `display_presets.py` and its manifest, `OversizeGuard` (still the safety net; its pivot is now the grip), `WeaponNormaliser`, `HandWrap`, shield frame, `HandItemRenderer`/`applySpritePreset`/hold styles, the old placement itself as the fallback (no grip found, a mesh the finder rejects, `-Dnusmp.weaponGrip=off`) |

No version bump, no changelog/README/lang edits (no new player text).

## 4. Checks

* `GripFinderTest` (8, synthetic meshes), `GripMathTest` (9: vanilla's chain written out step by step, both hands, three sizes, first-person view), `GripTableTest` (5, every shipped
  mesh of `weapon_meshes`, `weapon_meshes/up`, `_sz`, `_vu`, `_vu_sz`, 659 files): every mesh has a grip class (a new weapon with only a keyword guess **fails**, negative control done with a copied mesh), every
  upgraded mesh states its point, every grip lies inside the mesh box, and in all four hand contexts the hand lands on the grip under the real display block (about 2,500
  placements); the Java finder equals `tools/weapon_grip/grip_golden.json` (written by the Python twin) on 536 meshes. `HandHoldTest` (3). `item.weapon_grip` logic was run
  once against stubbed server classes: "10 weapons, 40 hand placements land on the grip".
* Existing tests re-run and passing: `WeaponClassesTest`, `OversizeGuardTest`, `WeaponNormaliserTest`, `SizedMeshNormaliserTest`, `UpgradedMeshNormaliserTest`,
  `HandTransformsTest`, `ServerSafetyTest`, `ScreenBlurGuardTest`. `WeaponMeshesTest.theRuntimeNormaliserHasNothingToDoOnAShippedMesh` fails, and did before this work: it names
  `delvers_pickaxe` and `hunter_exam_machete` (meshes of waves 104 and 105 that the runtime mirrors); not touched here.
* Rule 9: everything decided is pure and in `visual/grip` (no client class); the client glue only draws; no packet, no config, no per-player state.

## 5. Tools

    python tools/weapon_grip/preview_grip.py [--views 0,1,2,3,4] [--grid 2] [--tile 190] [--out x.png] <ids...>   # before / after side / front / left hand / first person
    python tools/weapon_grip/grip_table.py [--write|--check|--list]    # weapon_grips.json + golden; --check fails when a mesh has no grip class (CI)
    python tools/weapon_grip/grip_report.py [--top 25]                  # how far the 1.110.0 hand was from the grip, per weapon

To add a weapon: its mesh needs a class (`weapon_classes.json`, or `classOf` via `EXTRA_CLASS` in `grip_table.py`); an upgraded mesh needs its point (`grip_table.py --write`
computes it, look at the sheet, then freeze a correction in `POINT_OVERRIDES`); a mesh whose handle the finder cannot read gets a fraction in `OVERRIDES`, or a `gripxyz` line from
its generator. Then `grip_table.py --write` and run the three tests.

## 6. Not verified (nothing visual could be run)

* No game was started: every picture is the project's headless rasteriser (`tools/item_preview`) with the display block applied by `grip_lib.py`. The vanilla order of operations
  (`T R S T(-0.5)`, `Quaternionf.rotationXYZ` turning z first, left hand mirrored) is the one `display_presets.py` and the existing hand/shield code already rely on; the tests check
  the maths against it, not against a running renderer. The hand ring is a schematic 0.26 block palm, the arm is not drawn.
* The grip points of the 17 upgraded meshes and the 14 `fractions` were chosen from these sheets by eye; a weapon whose hilt I misjudged will sit a few centimetres off. The 238
  weapons held by their class fraction (spears 0.33, staffs 0.40, axes and hammers 0.25, bows and held items 0.50) are a "hand on the shaft", not an exact handle.
* The runtime normaliser (`WeaponNormaliser`) can turn or mirror a mesh at load; the grip is found after it, but a mesh that it flips and that also states a `gripxyz` ignores the
  point (the shipped meshes are not flipped: `WeaponMeshesTest`). The Python golden is computed on the files as shipped.
* First person: the weapon follows the hilt spot only as far as its tip stays in view; for long weapons the share is below 1 (Elsdocia, bows), so there the grip is a little off the
  spot. Vanilla shows no hand there, so nothing visibly misses.
* Which weapon is the owner's first screenshot (dark curved blade, chain pommel) was not identifiable from the description; the sheet includes both chain weapons
  (`nekrathuls_pact_scythe`, `starsplitter_hammer`), and the same rule covers every class.
* Not covered: poses that move the arm (Better Combat attack animations, the draw pose of a bow, `SwordDrawClient`) keep their own arm motion; they carry the grip with the hand
  because the mesh is now on the hand.
