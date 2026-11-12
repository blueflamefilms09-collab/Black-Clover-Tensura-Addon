# Seven Star weapons: model pass v2 (`tools/heroweapon/art_hart2.py`)

Owner request: "I want to make the weapons look better". This pass rebuilds the BASE look of the seven weapons of `art_hart2.py`
(hammer, claw, axe, whip, staff, gauntlet, projectile): bolder silhouettes, heavier proportions, more layered ornament, a bigger gem
with a lit ring, and edges / channels that are lit on the glow skin. Ids, file names, grip / orientation meaning, the 16 x 16 sprites
and the `_icon` models are unchanged. Nothing here runs on the server: the change is assets only (meshes + skins).

Sheets (before on top, after below): `docs/previews/seven_star_weapons/before_after_1_hammer_axe_staff_claw.png`,
`before_after_2_gauntlet_whip_projectile.png`, and one `vassal_<id>_v2.png` per weapon.

## What changed, per weapon

| Weapon | Parts before -> after | Quads before -> after | What changed |
|---|---|---|---|
| `vassal_hammer` | 13 -> 14 | 2181 -> 2582 | Larger head blocks and hub, thicker haft, grip, collars, langets and pommel; brass corner straps on both blocks; three crown studs on the hub; a larger medallion gem; NEW part `face_fire_rings` (a lit amber ring on the striking face and on the rear face); stronger face-rim and Big Dipper engraving. |
| `vassal_axe` | 11 -> 12 | 1250 -> 1498 | Broader, taller bit and a larger hub and poll; a longer back spike with two fangs; a longer finial with two barbs; thicker haft and butt cap; NEW part `edge_fire` (a lit emerald bead along the whole cutting edge); six lit runes on each face of the bit; larger gem. |
| `vassal_staff` | 11 -> 12 | 1698 -> 2071 | Larger crown ring and gem (cut gem, brighter); a larger crescent with six gold rays; NEW part `orbit_ring` (a second gold ring at a right angle to the first); two silver wings under the crown; thicker shaft, bands, foot and collar; brighter stars. |
| `vassal_claw` | 10 -> 10 | 2320 -> 2415 | Longer, broader talons (106 / 90 / 74 long, were 92 / 78 / 62) and thumb spur; larger blade sockets; a gold knuckle spike on each face between the sockets; three swept steel fins down each side of the bracer; a larger gem with a lit ring; edge glow doubled. The bracer, the hand plate and the origin (y = 0 at the cuff mouth) are unchanged, so `HandWrap` still fits it on the arm. In-hand length 1.00 -> 1.08 blocks so the hand part keeps its size. |
| `vassal_gauntlet` | 12 -> 12 | 1862 -> 2496 | Tiger-claw knuckle studs (12.5 long, were 7; lit); eight rivets round each lame; three swept fins down the back of the forearm and two on each side; a second orange roll at the wrist; a guard bar across the knuckles; a larger gem; stronger engraving. Vambrace and hand shell dimensions unchanged (`HandWrap`). The class limit (0.90 blocks) is kept, so the longer studs make the rest about 5 percent smaller in first person. |
| `vassal_whip` | 10 -> 10 | 1686 -> 1885 | Thicker handle and cord; a seven-pointed star guard; a larger pommel disc and gems with a butt spike; the tip is a real morning star (cage and gem x1.5, nine steel spikes); steel barbs along the free lash; the violet thread is clearly lit. In-hand length 1.62 -> 1.69. |
| `vassal_projectile` | 8 -> 9 | 1138 -> 1706 | Blade 1.3x wider with a lit channel each side of the ridge and five lit runes per face; winged quillons (up and down) on a wider guard; a second core gem in the blade's foot; a larger ring pommel and gem; NEW part `shards` (two small lit crystal shards beside the blade); brighter edge glint. The thickened copy `weapon_meshes_sz/vassal_projectile.txt` was rebuilt from the new mesh by `tools/relics/weapon_sizes.py --apply --ids vassal_projectile` (1774 quads). |

Helpers added to `art_hart2.py` (not to a shared library): `halo()` (a lit engraved ring round a gem) and `runes()` (seeded rune glyphs on a face).

## NEW / REPLACED / KEPT

- NEW: parts `face_fire_rings` (hammer), `edge_fire` (axe), `orbit_ring` (staff), `shards` (projectile); the sheets under `docs/previews/seven_star_weapons/`; this doc.
- REPLACED (owner asked for better looks): the mesh, skin and glow skin of all seven weapons; `weapon_meshes_sz/vassal_projectile.txt`; the first-person entries of
  `models/item/vassal_hammer.json`, `vassal_axe.json`, `vassal_staff.json`, `vassal_whip.json` (the script's own display preset, recomputed for the new mesh length;
  those four files were always plain script output).
- KEPT: the old look of every weapon, as copies of the committed files:
  `weapon_meshes/classic/vassal_<id>.txt`, `textures/entity/weapon/classic/vassal_<id>.png`, `textures/entity/weapon/classic/vassal_<id>_glow.png`
  for `<id>` in hammer, claw, axe, whip, staff, gauntlet, projectile, plus `weapon_meshes_sz/classic/vassal_projectile.txt`. They sit in a `classic/` folder (the layout
  the Legendary pass uses) because `GripTableTest` reads every `.txt` directly inside `weapon_meshes/`. No setting is wired to them in this pass.
  Also kept as committed: the 16 x 16 sprites, every `_icon` model, the hand-tuned item models of claw, gauntlet and projectile, `classes_hart2.json`.

## Cost (rule 11)

Client only. Worst case per drawn weapon: 2582 quads (hammer; the old worst case was 2320, the script's own limit is 2800 and the runtime cap is 3000).
No new textures sizes (512 x 512 skins, body skins palette-reduced by `tools/shrink_pngs.py`), no per-frame allocation, no server work, no packets.

## Checked

- `python3 -B tools/heroweapon/art_hart2.py --selfcheck`: all seven "identical on a second run".
- The script's audit: every weapon inside its size class, flags `-`.
- Offline test runs: `GripTableTest` (after `tools/weapon_grip/grip_table.py --write`), `WeaponSizesTest`, `HeroAssetsTest`, `HandWrapTest`, `WeaponClassesTest`,
  `WeaponMeshesTest`, `SizedMeshNormaliserTest`, `RelicRenderCoverageTest`: pass. `SafeAdditiveTextureScanTest`: none of these seven glow skins is reported.

## Not seen in a game

Everything was judged on the script's own preview (four views, the held pose, the sprite). Not seen in Minecraft: the first-person view, the claw and gauntlet worn
on the arm (`HandWrap`, third person), the glow layer's real brightness, the thickened projectile copy, and the upgraded (Genesis / God) states if they draw over these meshes.
