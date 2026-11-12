# Legendary weapons, model pass 2 (3.79.0)

The four BASE looks of the Legendary Weapons (sword, shield, spear, bow and the bow's three draw states) were rebuilt in
`tools/heroweapon/art_hart1.py` to read as hero weapons from a block away: bolder outlines, heavier proportions, more layers of
ornament and a larger glowing gem each. The four curse looks (`legendary_*_curse*`) were not touched: the generator still gives
the same curse meshes line for line (checked against git), and their committed files are unchanged.

Sheet (before on the left, after on the right, the bow at full draw below): `docs/previews/legendary_weapons/before_after.png`.

## What changed, per weapon

| Weapon | Before | After |
|---|---|---|
| Sword (blue gem) | slim 6.8 cm blade, small winged guard, round pommel | blade half as broad again (10.2 cm at the guard) with a wide fuller, a glowing blue spine, rune strokes and lit edges; guard 54 cm across with three feathers a side cradling a gem 5.0 cm in radius (was 3.6); gold langets up both blade faces; stouter wrapped grip with four rings; faceted pommel |
| Shield (green gem) | flat plate, four thin ribs, bead ring, small gem | gem 6.6 cm in radius (was 5.0) on a fuller boss with a gold shoulder ring and a green halo on the glow skin; four steel leaf plates with gold spines on the diagonals (an eight-pointed star with the broader cardinal ribs); a second gold rim inside the rolled rim; a ring of 40 engraved runes that glow green; a small green stone at each rib tip |
| Spear (red gem) | 21 cm wide leaf head, thin shaft | head 78 cm long and 27 cm broad with a glowing channel and lit edges, two steel crescent lugs (47 cm across), two gold feathers a side at the socket, a larger orb, a shaft a third thicker with ten gold bands and a wrap, a faceted counterweight butt spike |
| Bow (yellow gem) | thin limbs, two feathers a side, small gem | limbs half as thick again with broader gold inlay, a gold blade fin on the front edge of each limb root, three feathers a side, two forward blades crowning the riser, gem 4.7 cm in radius (was 3.1) in a broader riser, a forward talon on each tip. The fins and talons are in `limb_` parts, so they flex with the limbs in `pull1..3`; the string and the arrow of light are unchanged |

Unchanged on purpose: every file name and id, each weapon's length and y range (so the `place` lines are the same as before: the
sword, shield and spear lines come out of the generator identical; the bow's four files carry the committed, later-tuned line
`place 0.92426 0.92426 225.000 0.007123 -84.239`), `orient manual`, the item models and display transforms
(`models/item/legendary_*.json`, including `legendary_shield.json` and `legendary_shield_blocking.json`), the 16x16 icons, the
`glow` lines, the skin size (512 px).

The `grip` line (written by `mesh_lib` from the part names, the share of the length where the handle parts lie) follows the new
models: sword 0.084 -> 0.092 (12.6 cm -> 13.8 cm from the pommel), spear 0.224 -> 0.233. `tools/weapon_grip/grip_golden.json`
has the four new entries (sword, spear, and the grip points of the shield z 4.175 -> 4.85 and the bow x 16.83 -> 17.175).

## NEW / REPLACED / KEPT

- REPLACED (owner asked for better models): `weapon_meshes/legendary_{sword,shield,spear,bow,bow_pull1,bow_pull2,bow_pull3}.txt`,
  `textures/entity/weapon/legendary_{sword,shield,spear,bow}.png` and `..._glow.png`.
- KEPT (the old look, exact copies of the files before this pass):
  `weapon_meshes/classic/legendary_{sword,shield,spear,bow,bow_pull1,bow_pull2,bow_pull3}.txt`,
  `textures/entity/weapon/classic/legendary_{sword,shield,spear,bow}.png` and `..._glow.png`.
- KEPT untouched: the four curse looks, the icons, the item models, `classes_hart1.json`.
- NEW: the client setting below; `SH2_*`, `SW2_*`, `SP2_*`, `BW2_*` numbers and `shield2_layers`, `spear2_*`, `bow2_flourish` in
  `art_hart1.py` (the old numbers stay, the curse looks still use them).

## The setting

`nusmp-heroweapon-client.toml`, section `models`: `classicHeroWeaponModels` (default `false`). It is a CLIENT setting (looks only,
nothing is sent to the server). When `true`, `WeaponRenderer.plainMesh` reads `weapon_meshes/classic/<id>.txt` with the skins of
`textures/entity/weapon/classic/` for the base Legendary ids (ids starting `legendary_` without `_curse`), under the SAME mesh
id, so the class, grip, shield-frame and display tables apply as before. A missing classic file falls back to the normal mesh.
Switching the setting clears the mesh caches once (the same clear a resource reload does); there is no per-frame cost beyond one
boolean read.

## Cost (quad lines per mesh; the runtime cap `MeshCap` is 3000, the generator's own check 2800)

| Mesh | Before | After |
|---|---|---|
| legendary_sword | 1204 | 1448 |
| legendary_shield | 2664 | 2784 |
| legendary_spear | 1006 | 1224 |
| legendary_bow (rest) | 1446 | 1748 |
| legendary_bow_pull1..3 | 1650 | 1952 |

The shield's count includes its glow shell (the rune ring and halo add glow quads); its body went down from 2520 to 2400 quads
(32-sided plate and boss, lighter bead and rim) to pay for the new layers. No new entities, packets or per-tick work: these are
static meshes drawn by the existing renderer under its existing budgets. Skins were palette-shrunk like the committed ones
(`tools/shrink_pngs.py`, 48 dB or better) and the glow skins cleaned for the safe-additive rule (no colour under alpha 0).

## Checks run

- `python3 -B tools/heroweapon/art_hart1.py --check`: all eight looks build, "determinism: 44 files, identical on a second run",
  "self check: all fine". Note: `--check` WRITES every file of all eight looks; after it, restore the tuned files with
  `git checkout` (icons, `models/item/legendary_shield*.json`, the curse skins and bow curse meshes), re-run the shrink and the
  glow clean on the four base skins, and put the bow's committed `place` line back in its four base files.
- Pure tests offline (52 classes that read the weapon files, 332 test methods): all pass except two failures that name only
  Vassal files of the parallel Vassal pass (`SafeAdditiveTextureScanTest`: `vassal_*_glow.png`; `GripTableTest`: `vassal_mirror`).
  `HeroAssetsTest`, `ShieldFrameTest`, `WeaponMeshesTest`, `BowDisplayGuardTest`, `ServerSafetyTest`, `ConfigFileNamesTest`,
  `PerfSourceScanTest` pass. `MeshCapTest` and `MeshPackTest` do not compile without the game's classes and were not run.

## Not seen in a game

Everything above was checked on the offline preview renderer and by the file tests only. Not seen in a running game: the models
in first and third person, the shield while blocking, the bow's draw animation in hand, the glow strength of the new rune ring,
halo, spine and channel under the game's bloom, and the `classicHeroWeaponModels` switch itself (the Java change was read, not
compiled: no Minecraft classes are available offline).
