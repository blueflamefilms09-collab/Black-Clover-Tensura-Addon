# Elsdocia rebuild (1.29.0)

Rebuilt from the owner's reference render (`66.png`, 405 x 493, the sword hanging tip down on black). This is a replacement of the 1.2.0 Elsdocia look
(same item, same abilities, same Legacy engraving); the old assets stay in the repo as `*_legacy` files for rollback.

## Files
| What | Path |
|---|---|
| Builder (mesh, skin, glow skin, preview) | `tools/relics/elsdocia_rebuild.py` |
| 16x16 inventory sprite | `tools/gen_elsdocia_icon.py` (`--legacy` writes the 1.2.0 sprite) |
| Mesh / skins | `weapon_meshes/elsdocia.txt`, `textures/entity/weapon/elsdocia.png` (1024 px), `elsdocia_glow.png` |
| Item model (3D in hand, vanilla-sized display) | `models/item/elsdocia.json` (parent `builtin/entity`), `elsdocia_icon.json` (flat icon, unchanged) |
| Held glitter | `client/ElsdociaGlitter.java` (pure maths, tested), `client/ElsdociaGlitterClient.java` (needs one init call) |
| Legacy (kept) | `weapon_meshes/elsdocia_legacy.txt`, `textures/entity/weapon/elsdocia_legacy[_glow].png`, `textures/item/elsdocia_legacy.png`, `models/item/elsdocia_legacy.json`; `tools/gen_weapon_models.py` now writes them under the id `elsdocia_legacy` |

Run: `python3 -B tools/gen_elsdocia_icon.py && ELSDOCIA_PREVIEW=out python3 -B tools/relics/elsdocia_rebuild.py`.

## Why the 3D mesh was never shown
`models/item/elsdocia.json` had parent `minecraft:item/handheld`, so the game drew the flat sprite and `WeaponRenderer` (which only runs for models whose parent is
`builtin/entity`) was never called for it. It now has parent `builtin/entity` with the display transforms of a normal 1.2 m sword (third person 0.85, first person
0.672 from `tools/relics/display_presets.py`); the inventory keeps the sprite through `elsdocia_icon.json`, as for every other 3D weapon.

## Measurements (step 1)
One known length converts px to cm: the whole sword is a 1.25 m longsword = 477 px (pommel top y 8 to tip y 485), so K = 125 / 477 = 0.262 cm per px.

| Part | Reference px | Model (cm) |
|---|---|---|
| Total length | 477 | 125.0 |
| Pommel bulb (round, gem on both faces) | y 8..30, 23 wide | y 0..5.8, radius 3.1 |
| Grip (slim, dark, wrapped) | y 30..120, 12 wide | y 5.8..29.3, radius 1.6 |
| Crossguard bar centre | y 128 | y 31.4 |
| Crossguard span | x 124..274 = 150 | 39.3 (half 19.65) |
| Guard bar thickness / end flares | 8 / 17 | 2.0 / 4.4 tall, 2.6 deep |
| Ricasso (bronze, with the club) | y 134..225, about 55 wide | y 32.0..57.0, 14.4 wide, 1.9 thick |
| Club (clover) cut-out | y 150..175, 33 x 25 | centre y 40.3, about 8 x 7 |
| Crystal blade | y 225..485, 65 wide | y 57.0..125.0, 17.0 wide |
| Crystal point | taper over 49 px | last 12.8 cm |

Deviation from the brief: the brief says "blade about 55 % of the length, 2.2x the grip width". Measured on the reference the blade (ricasso plus crystal) is 73 % of the
length and the crystal is 5.4x the grip width (it is a very broad, buster-like blade), so the measured values are used. Real-life size: 1.25 m
(`REAL` 1.25 in `tools/gen_weapon_models.py`, `Mesh.place_on_sprite(real_m=1.25, grip=0.15)`), so third person is 1.25 blocks and the tip is up-forward like every sword.

## Parts (28, step 2)
pommel_bulb, pommel_ring, pommel_gem, grip, grip_wraps, grip_rings, guard_bar, guard_flares, guard_boss, guard_studs, ricasso_socket, ricasso, ricasso_trim, ricasso_ridge,
clover_rim, clover_inlay, blade_collar, crystal_core, facets_front_a/b/c, facets_back_a/b/c, crack_ridges_front/back, glow_hotspots_front/back (additive layer 2).

- The crystal is a thin lozenge core with a central ridge (the light you see in the cracks) under a mosaic of closed facet plates: a jittered grid of 3 x 7 cells, 60 % of
  them split along a diagonal, each plate with its own tilted top plane (slope of the blade's bevel plus a random tilt) and a 0.07 cm crack to its neighbours. Front and back
  use different seeds. The plates' outer vertices are notched, so the silhouette is fractured edge-on.
- The club is a real raised rim (bronze) plus a dark inlay (obsidian, faint red-violet ember in the glow skin) on both faces, not a hole through the blade (a through-hole in a
  baked cutout mesh would need a second mesh layer; the inlay reads as the hole in the reference).
- Crack lines are thin raised ridges (`crack_ridges_*`) along shared plate edges plus long fracture lines painted over several plates.
- The inner glow is separate: `crystal_core` (glow skin), the plates' own glow skin (cyan inner light, brighter at cracks), and `glow_hotspots_*`, additive decals over 5 hot plates
  per face plus the blade tip.

## Normals, texture, glow (steps 3-4)
Per-part angle-smoothed normals written as `qn` lines (the renderer's per-vertex normals); plates are flat facets (20 deg), round parts smooth (45-50 deg). The skin is 1024 px
painted in model space on the dominant-axis UV groups: blue glass gradients per plate lit by its tilt, light facet rims on the lit side and dark rims on the other, scratches,
crack lines, white-cyan hotspots with 3x3 sparkle crosses. The glow skin is cyan (`glow 5EC8FF 0.10 0.50 1.0`: the renderer pulses it between 0.5 and 1.0) with white hotspots.

## In hand
- Genesis state: `WeaponRenderer` already adds the emissive Genesis pass for any weapon whose holder is Genesis (`ItemStateVariants`), so Elsdocia needs no `_genesis` mesh.
- Glitter: `ElsdociaGlitterClient` (client only, no packets) spawns pale-blue dust and rare end-rod sparks along the blade of any player holding Elsdocia in the main hand, 24 blocks
  at most, at most 6 particles per client per tick (`ElsdociaGlitter`). Registration line: `com.newuniverse.nusmp.client.ElsdociaGlitterClient.init();` in the client section of `NUSMP`.

## Why (one paragraph per component)
- **Measured constants, not eyeballing**: every size derives from one known length so the weapon keeps its proportions if the owner changes the real size (change `TOTAL_CM`).
- **Facet plates instead of a textured prism**: normals are per face, so a different tilt per plate gives different shading from every camera angle; a flat prism with a painted
  texture can only fake it from one angle. The cost is about 3900 body quads, similar to the larger relic meshes, rendered only for the item in hand.
- **Core under the plates**: the 0.07 cm cracks show the glowing core, which is what makes the cracks read as lit from inside without translucency (the renderer is cutout only).
- **Separate additive hotspots**: the glow pass is additive and unaffected by world light, so hotspots stay white in the dark and pulse with the item's glow line at no extra draw call.
- **Legacy files kept**: owner rule 2 (rollback). Re-running `tools/gen_weapon_models.py` now writes the legacy mesh under `elsdocia_legacy`, so it can never overwrite the new one.
- **Client-only, capped glitter**: no network traffic, no server cost, and the per-tick cap bounds the particle engine even with many holders.
- **Sprite**: 16x16 on the same diagonal as every other weapon icon because the mesh is placed on that diagonal by `place_on_sprite`; a different icon axis would change the in-hand placement.

## New vs replacement (rule 5)
Replacement: Elsdocia's in-hand mesh, skins and inventory sprite (old ones kept as `_legacy`), and its item model json (it was a flat sprite). New: the held glitter VFX.
Unchanged: `MagicWeaponItem.Kind.ELSDOCIA` behaviour, the Legacy of the Wizard Kings engraving (`WeaponEngravings`), `WeaponArts` Legacy Gate, curios links.

## Not verified
Nothing was run in the game: the preview is the software rasteriser (`tools/relics/preview_mesh.py`), the Java was not compiled here (CI compiles).
