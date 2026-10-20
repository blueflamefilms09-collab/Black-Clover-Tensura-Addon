# Visual upgrade 1.102.0, slice genesis_weapons: the Genesis grade hand weapons

Owner request: "Upgrade God Grade weapons/relics to look 5x better and Genesis grade to look 20x better." "X times better looking" cannot be measured literally, so this document
records proxies, the formula behind them, and what they say. **Nobody has seen these models in a game** (nothing here can be run: no Gradle, no client); the previews below are
software renders of the geometry and the skins (`tools/relics/preview_mesh.py`'s rasteriser), not the game. The measured ratios are below; the visual impression is unverified.

Slice: the 12 Genesis hand weapons named in the brief plus Elsdocia (13 items, 16 meshes with the bow's three draw stages). Items in the slice:
`astralis_the_eye_of_the_universe`, `blackrazor`, `cardinal_halo_chakram`, `fairy_glitter_gauntlets`, `gae_bolg_heartseeker`, `magic_emperors_black_halberd`, `primordial_hearth_blade`, `starforged_aeon_bow`, `starsplitter_hammer`, `sword_of_the_first_dawn`, `void_origin_katana`, `nekrathuls_pact_scythe`, `elsdocia`.
Not touched: the Genesis Demon-Slayer (see "Cut" below), the worn Genesis items (other slices), the Time Magic VFX.

## What was built

* **Meshes** `assets/nusmp/weapon_meshes_vu/<mesh id>.txt` and `<mesh id>_lod.txt` (the old `weapon_meshes/<id>.txt` stay untouched): a superset of the game's mesh dialect with five layers
  (0 body, 1 void, 2 core glow, 3 energy veins, 4 halo cards), `g <group>` animation groups and `fl` flares. Same unit frame and the same `place` line as the old mesh (checked by
  `VuAssetsTest`), so every display transform, the oversize rule, the bow display and the two-hand poses fit unchanged.
* **Skins** `textures/entity/weapon_vu/<id>.png` (2048 px, hand-authored look from maths: hammered / brushed / glassy materials, enamel inlays, engraved lore text and runes, caustics, wear),
  `<id>_glow.png` and `<id>_veins.png` (emissive maps at half size), `<id>_lod.png` for the LOD mesh and one shared `halo.png` (16 additive sprites).
* **Effect data** `assets/nusmp/visual_upgrade/<id>.json`: the animation groups (spin, bob, sway, pulse, breathe, flicker with pivots), the ambient effect layers, the idle animations,
  the draw animation, the swing trail and a signature burst for every act, and the looped sound id (a string only: `nusmp:item.genesis.<item>.<loop>`; the audio files are another task).
* **Renderer** `client/relics/item/vu/VuRenderer` (called once from `WeaponRenderer.renderByItem`): body, void, core glow, a vein wave flowing along the weapon, halo cards with their own
  tint and breathing alpha, camera-facing flares; groups posed by the pure `visual/genesis/VuPose`; a draw animation when a weapon first appears; LOD by distance; every matrix and
  vector is a scratch object. `client/relics/item/vu/VuFx` is the client tick: ambient particles, the swing trail and the signature burst of an act (from the cooldown packet).
* **Generators** `tools/visual_upgrade/genesis_weapons/` (python3, numpy, PIL; deterministic): `vu_tex` (the painter), `vu_geo` / `vu_geo2` (blades, rings, feathers, chains, star plates ...),
  `vu_item` (parts, groups, atlas with a MaxRects packer, the writer), `item_<id>.py` (one per weapon), `build_item`, `build_all`, `measure`, `score`, `vu_preview`, `make_doc`.
  Hook: one `if` block at the end of `main` in `tools/gen_weapon_models.py` (`python3 -B tools/gen_weapon_models.py visual_upgrade_genesis_weapons`).

### Passes and effect layers per weapon
| Weapon | emissive passes (before -> after) | ambient layers | idle animations | draw | swing trail | signature on acts / acts | looped sound id |
|---|---|---|---|---|---|---|---|
| Astralis - The Eye of the Universe | 2 -> 4 | 2 -> 10 | 1 -> 9 | yes | yes | 2 / 2 | `nusmp:item.genesis.astralis.hum_loop` |
| Blackrazor | 3 -> 5 | 2 -> 8 | 1 -> 7 | yes | yes | 3 / 3 | `nusmp:item.genesis.blackrazor.hunger_loop` |
| Cardinal Halo Chakram | 2 -> 4 | 2 -> 8 | 1 -> 7 | yes | yes | 3 / 3 | `nusmp:item.genesis.cardinal_halo.ring_loop` |
| Fairy Glitter Gauntlets | 2 -> 4 | 2 -> 9 | 1 -> 8 | yes | yes | 3 / 3 | `nusmp:item.genesis.fairy_glitter.shimmer_loop` |
| Gae Bolg, Heart-Seeker | 2 -> 4 | 2 -> 9 | 1 -> 8 | yes | yes | 3 / 3 | `nusmp:item.genesis.gae_bolg.heartbeat_loop` |
| Magic Emperor's Black Halberd | 3 -> 5 | 2 -> 8 | 1 -> 7 | yes | yes | 3 / 3 | `nusmp:item.genesis.black_halberd.void_hum_loop` |
| Primordial Hearth Blade | 2 -> 4 | 2 -> 9 | 1 -> 8 | yes | yes | 3 / 3 | `nusmp:item.genesis.hearth_blade.fire_loop` |
| Starforged Aeon Bow | 2 -> 4 | 2 -> 8 | 1 -> 8 | yes | yes | 3 / 3 | `nusmp:item.genesis.aeon_bow.starsong_loop` |
| Starsplitter Hammer | 3 -> 5 | 2 -> 10 | 1 -> 8 | yes | yes | 3 / 3 | `nusmp:item.genesis.starsplitter.night_loop` |
| Sword of the First Dawn | 2 -> 4 | 2 -> 9 | 1 -> 8 | yes | yes | 3 / 3 | `nusmp:item.genesis.first_dawn.choir_loop` |
| Void Origin Katana | 3 -> 5 | 2 -> 9 | 1 -> 8 | yes | yes | 3 / 3 | `nusmp:item.genesis.void_origin.absence_loop` |
| Nekrathul's Pact Scythe | 3 -> 5 | 2 -> 10 | 1 -> 8 | yes | yes | 2 / 2 | `nusmp:item.genesis.pact_scythe.whisper_loop` |
| Elsdocia | 3 -> 4 | 3 -> 8 | 1 -> 7 | yes | yes | 2 / 2 | `nusmp:item.genesis.elsdocia.crystal_hum_loop` |

Emissive passes after: core glow (additive, pulsing), energy veins (additive, a travelling wave of light along the weapon), halo cards (additive sprites: rings, shafts, arcs, runes), flares
(camera-facing stars), and the dimensional void shader where the weapon has rifts (Blackrazor, the Black Halberd, the Starsplitter Hammer, the Void Origin Katana, Nekrathul's Scythe).
"Before" counts what the old renderer drew for a Genesis stack (the glow pass, the translucent emissive pass of a Genesis stack, Elsdocia's halo) and nothing else.

## Measured: mesh
| Weapon | body quads before | after | ratio | welded vertices (cap 6000) | LOD body quads | glow / veins / halo / void quads |
|---|---|---|---|---|---|---|
| Astralis - The Eye of the Universe | 2 124 | 5 360 | 2.52x | 4 874 | 724 (13.5 %) | 1577 / 1174 / 18 / 0 |
| Blackrazor | 854 | 4 822 | 5.65x | 4 517 | 604 (12.5 %) | 879 / 1035 / 18 / 42 |
| Cardinal Halo Chakram | 1 224 | 4 560 | 3.73x | 4 490 | 550 (12.1 %) | 1964 / 1018 / 24 / 0 |
| Fairy Glitter Gauntlets | 1 200 | 5 184 | 4.32x | 4 702 | 640 (12.3 %) | 1058 / 476 / 24 / 0 |
| Gae Bolg, Heart-Seeker | 1 202 | 3 865 | 3.22x | 3 576 | 443 (11.5 %) | 1494 / 446 / 18 / 0 |
| Magic Emperor's Black Halberd | 823 | 3 534 | 4.29x | 3 616 | 462 (13.1 %) | 1075 / 1149 / 20 / 28 |
| Primordial Hearth Blade | 692 | 3 338 | 4.82x | 3 214 | 478 (14.3 %) | 1580 / 865 / 32 / 0 |
| Starforged Aeon Bow | 1 100 | 4 282 | 3.89x | 4 072 | 411 (9.6 %) | 2246 / 338 / 32 / 0 |
| Starsplitter Hammer | 840 | 4 286 | 5.10x | 4 577 | 447 (10.4 %) | 482 / 307 / 24 / 28 |
| Sword of the First Dawn | 1 368 | 4 388 | 3.21x | 3 889 | 546 (12.4 %) | 1956 / 622 / 36 / 0 |
| Void Origin Katana | 842 | 3 302 | 3.92x | 3 219 | 442 (13.4 %) | 1776 / 1277 / 20 / 36 |
| Nekrathul's Pact Scythe | 1 144 | 4 225 | 3.69x | 4 366 | 489 (11.6 %) | 754 / 572 / 18 / 40 |
| Elsdocia | 3 896 | 5 014 | 1.29x | 4 999 | 408 (8.1 %) | 2666 / 1529 / 26 / 0 |

"Welded vertices" = distinct positions (rounded to 1/1000), what an index buffer would hold. The brief gives two numbers that cannot both be read as submitted corners: 2500 quads would be
10 000 corners against a cap of 6000 vertices. The cap is therefore applied to the welded count (every weapon is between 3214 and 4999); the renderer submits
4 corners per quad per pass (see `VuMesh#submittedVertices`), and the LOD mesh (at most 15 % of the quads) is drawn beyond 12 blocks, the additive passes are skipped beyond 24.
**The "20x the quads" target is not reachable under the vertex cap** for any weapon that already had about 300 quads or more (20 x 850 quads would be 17 000 welded vertices): the
ratios above are what fits.

## Measured: skins
| Weapon | skin before | after | painted texels before | after | ratio | detail texels before | after |
|---|---|---|---|---|---|---|---|
| Astralis - The Eye of the Universe | 1024 px | 2048 px | 334 760 | 3 761 918 | 11.2x | 196 086 | 2 347 276 |
| Blackrazor | 512 px | 2048 px | 121 722 | 4 010 910 | 33.0x | 39 138 | 1 997 130 |
| Cardinal Halo Chakram | 512 px | 2048 px | 189 770 | 2 514 468 | 13.2x | 83 238 | 1 779 452 |
| Fairy Glitter Gauntlets | 512 px | 2048 px | 202 059 | 3 841 752 | 19.0x | 81 490 | 2 211 394 |
| Gae Bolg, Heart-Seeker | 512 px | 2048 px | 90 584 | 1 579 648 | 17.4x | 28 619 | 1 125 334 |
| Magic Emperor's Black Halberd | 512 px | 2048 px | 56 548 | 1 570 852 | 27.8x | 14 778 | 962 919 |
| Primordial Hearth Blade | 512 px | 2048 px | 114 144 | 2 249 206 | 19.7x | 34 372 | 1 254 950 |
| Starforged Aeon Bow | 512 px | 2048 px | 170 261 | 3 804 275 | 22.3x | 82 543 | 2 450 234 |
| Starsplitter Hammer | 512 px | 2048 px | 104 943 | 3 814 485 | 36.4x | 55 080 | 3 152 943 |
| Sword of the First Dawn | 512 px | 2048 px | 86 160 | 2 177 585 | 25.3x | 36 471 | 1 249 989 |
| Void Origin Katana | 512 px | 2048 px | 74 723 | 1 987 186 | 26.6x | 13 755 | 1 033 984 |
| Nekrathul's Pact Scythe | 512 px | 2048 px | 88 489 | 1 780 973 | 20.1x | 38 434 | 1 094 500 |
| Elsdocia | 1024 px | 2048 px | 715 167 | 3 833 232 | 5.4x | 184 547 | 1 765 273 |

Painted = alpha above 8 (the packed UV islands, including their 1 px bleed); detail = painted texels whose colour differs from the mean of their four neighbours by more than 6 of 765
(a flat fill is painted but is not detail). The old skins were 512 px (Astralis and Elsdocia 1024 px) and partly flat fills; the new ones are 2048 px. Elsdocia's baseline already
filled 715 000 texels of a 1024 px skin, so 8x would need 5.7 million of the 4.2 million a 2048 px skin has: its ratio is the most the format allows.

## The score V (weights fixed, documented, the same for every item)
V = sum of weight x (after / before) over: mesh detail 0.35 (body quads), texture detail 0.20 (detail texels), emissive passes 0.15, ambient effect layers 0.20, animation 0.10 (idle animations +
draw animation + swing trail). V_before = 1 by construction. Ratios are not clamped.

| Weapon | mesh | texture | passes | ambient | animation | **V after / V before** | all floors of the brief met |
|---|---|---|---|---|---|---|---|
| Astralis - The Eye of the Universe | 2.52 | 11.97 | 2.00 | 5.00 | 11.00 | **5.7** | yes |
| Blackrazor | 5.65 | 51.03 | 1.67 | 4.00 | 9.00 | **14.1** | yes |
| Cardinal Halo Chakram | 3.73 | 21.38 | 2.00 | 4.00 | 9.00 | **7.6** | yes |
| Fairy Glitter Gauntlets | 4.32 | 27.14 | 2.00 | 4.50 | 10.00 | **9.1** | yes |
| Gae Bolg, Heart-Seeker | 3.22 | 39.32 | 2.00 | 4.50 | 10.00 | **11.2** | yes |
| Magic Emperor's Black Halberd | 4.29 | 65.16 | 1.67 | 4.00 | 9.00 | **16.5** | yes |
| Primordial Hearth Blade | 4.82 | 36.51 | 2.00 | 4.50 | 10.00 | **11.2** | yes |
| Starforged Aeon Bow | 3.89 | 29.68 | 2.00 | 4.00 | 10.00 | **9.4** | yes |
| Starsplitter Hammer | 5.10 | 57.24 | 1.67 | 5.00 | 10.00 | **15.5** | yes |
| Sword of the First Dawn | 3.21 | 34.27 | 2.00 | 4.50 | 10.00 | **10.2** | yes |
| Void Origin Katana | 3.92 | 75.17 | 1.67 | 4.50 | 10.00 | **18.6** | yes |
| Nekrathul's Pact Scythe | 3.69 | 28.48 | 1.67 | 5.00 | 10.00 | **9.2** | yes |
| Elsdocia | 1.29 | 9.57 | 1.33 | 2.67 | 9.00 | **4.0** | yes |

**Result: V_after lies between 4.0 and 18.6 times V_before; no weapon reaches the 20x target on this formula.** The reasons are measured, not excuses: the mesh ratio is bounded by the
vertex cap (3x to 6x here), the baseline already had two ambient layers and a glow pass, and most of the large ratios (texture 7x to 64x, animation 9x to 11x) come from resolution and from
items that had no idle animation at all. The floors of the brief that are binary (at least 2500 body quads, at most 6000 welded vertices, LOD at most 15 %, at least 6 ambient layers, at
least 2 emissive passes, a draw animation, a swing trail, a signature burst on every act, a looped sound id) hold for every item (`VuAssetsTest`), and so does the skin floor in the
form "8x the baseline, or all a 2048 px skin can hold". Whether this looks 20 times better has not been seen in a game.

## NEW / REPLACED / KEPT
| What | Status |
|---|---|
| `weapon_meshes_vu/*`, `textures/entity/weapon_vu/*`, `visual_upgrade/*.json` | NEW (all alongside the old files) |
| Meshes and skins of the 13 weapons as drawn in hand, on the ground, in frames, on mobs | REPLACED by the upgraded ones while the client flag `visual.upgrade` is on (default); the old files stay on disk and are drawn again with the flag off |
| Old `weapon_meshes/<id>.txt`, `textures/entity/weapon/<id>*.png`, model jsons, display presets, Better Combat files, relic defs, acts, passives, engravings, stats | KEPT, untouched |
| Inventory icons (`textures/item/<id>.png`, the `_icon` models) | KEPT (rule 7). The tilted 3D inventory model exists behind `visual.guiModel` and is OFF by default for that reason |
| Ambient / swing / act particles | NEW (client tick, budgeted); the shared grade shimmer of `RelicGradeFx` is KEPT |
| Elsdocia held glitter (`ElsdociaGlitterClient`), its halo pass | KEPT; the old renderer path (flag off) still draws them |
| `HandWrap`, `OversizeGuard`, `WeaponNormaliser`, bow stage selection | KEPT; the upgraded path reuses the oversize rule and the stage selection of `ItemStateVariants` |

## Edits to existing files (all additions)
* `client/WeaponRenderer.java`, in `renderByItem`: two lines before `Mesh mesh = ...` (`VuRenderer.draw(...)`, `VuRenderer.drawGui(...)`), each returning when it draws.
* `NUSMP.java`: the client config registration of `VuOptions.SPEC` (next to the relic fx options), `VuClient.init(modEventBus)` (next to `WeaponRenderer.init`), `VuSelfTests.register()` (next to `PerfBoot.init`).
* `relics/RelicPayloads.java`: the cooldown packet handler also calls `VuFx.onAct(...)` (one call inside the existing lambda).
* `tools/gen_weapon_models.py`: one `if` block at the end of `main` (the hook above).

## Config (client, `config/nusmp-visual-upgrade-client.toml`)
`visual.upgrade = true` (false draws the old meshes), `visual.particles = true`, `visual.trails = true`, `visual.guiModel = false`. The effect quality is the existing
`nusmp-performance-*.toml` `vfxQuality`: at `low` there are no halo cards, flares, animation or particles; at `medium` no flares and half the particles; `high` is everything.

## Performance guard
Full mesh up to 6000 welded vertices in the hand; the LOD mesh (at most 15 %) beyond 12 blocks; additive passes skipped beyond 24 blocks; no per-frame allocation (the mesh is flat float
arrays, every matrix / vector / pose is a scratch object, the halo sheet is one texture); ambient particles through `PerfBudgets.clientParticles` (at most 6 per holder per tick, 8 holders
within 32 blocks, none at low quality). Pinned by `VuAssetsTest`, `VuRulesTest` and the existing `PerfSourceScanTest`.

## Tests
`VuRulesTest` (distance, quality, draw easing, wave / flicker ranges, the pose maths, the burst patterns, the spec parser), `VuAssetsTest` (every asset exists, the old mesh stays, the caps, the
placement of the old mesh, the draw stages share the bow skin, the effect data floors and every act of every RelicDef has a burst, the ratios and floors of `genesis_weapons_after.json`),
self-test `visual.genesis_weapons` (assets in the booted mod, the flag fallback, the distance rules). Existing `WeaponMeshesTest`, `BowDisplayGuardTest`, `PerfSourceScanTest` pass.

## Previews (software renders, not the game)
`docs/previews/visual_upgrade/genesis_weapons/<id>.png`: row 1 the old mesh (held, side, front, detail), row 2 the new mesh at rest (the animation groups at t = 0), row 3 the emissive passes
alone and the LOD mesh. The bow's sheet is the full-draw stage.

## What could NOT be verified
* Nothing was seen in a game: not the look, the lighting, the additive passes, the void shader on the new meshes, the halo cards against real backgrounds, the flares, the draw animation,
  the group animation, the swing trail (an approximation of the swing arc, not Better Combat's pose), the act bursts (played for the local player from the cooldown packet only; other players
  see the existing `VfxShape`), the particle budget in a crowd, the GPU and CPU cost of the upgraded meshes (about 3 000 to 5 400 quads each; the old ones had 700 to 3 900).
* The texture memory: a 2048 px skin is 16 MB on the GPU, an extra 4 MB for the half-size emissive maps, per weapon in use.
* That the glow and vein maps (half size, UVs normalised) sample cleanly at the seams of the packed islands.
* Shimmer: entity skins are sampled nearest with no mipmaps, so a 2048 px skin may sparkle on a weapon seen from 3 to 12 blocks (beyond 12 the 256 px LOD skin is used). If it does, lower
  `lod.near` in the weapon's `visual_upgrade/<id>.json` (the renderer reads it per weapon) or the noise amplitude in `tools/visual_upgrade/genesis_weapons/vu_tex.py`.
* `visual.guiModel`: the tilted inventory model was written but not run.
* The Genesis Demon-Slayer and every other weapon: not changed.

## Cut / not done
* The Genesis Demon-Slayer (the owner's standard, "LAST, only if the rest is done"): not upgraded in this slice.
* The Infinity Gauntlet (Wave 30, Genesis, slot HANDS) is a worn item, left to the worn slices.
