# Visual upgrade 1.108.0, slice demon_slayer: the Genesis Demon-Slayer Sword

Owner request (the release 1.102.0 visual upgrade, "Genesis grade 20x better looking", carried to the weapon that slice left out): the **Genesis Demon-Slayer Sword** (`demon_slayer_sword`,
Asta's anti-magic blade, the owner's visual standard of CLAUDE.md rule 7) must now clearly exceed its own old model, by the proxies the brief defines. "X times better looking" cannot be
measured literally, so this document records the proxies, the formula, and what they say. **Nobody has seen this model in a game** (no Gradle, no client here): the previews are software
renders of the geometry and the skins (`tools/relics/preview_mesh.py`'s rasteriser), not the game. The measured ratios are below; the visual impression is unverified.

Everything the item does is unchanged (id, stats, acts, passives, engravings, Better Combat files, display presets, the 32 x 32 inventory icon): this is a new look, on the same model frame.

## What was built

* **Mesh** `assets/nusmp/weapon_meshes_vu/demon_slayer_sword.txt` and `demon_slayer_sword_lod.txt` (the genesis_weapons dialect: layers 0 body, 1 void, 2 core glow, 3 veins, 4 halo cards, `g <group>`
  animation groups, `fl` flares). **The old model is code** (`client/DemonSlayerMesh.java`, a pixel model of 61 body quads), so the new mesh is built in the old
  model's own frame (1/64 block units) and carries the old renderer's placement as its `place` line (`translate(0.516, 0.523, 0.5), rotate z -45, scale 0.98/64, translate(0, -11.75, 0)`):
  every display transform of the item model, the third-person and first-person sizes and the 1.6 x growth of Black Divider fit unchanged (`DemonSlayerAssetsTest`).
  * the blade: a broad slab (13 units at the guard, 11 at the point) with a chiselled slanted point as before, a framed panel on both faces, 14 hammered-out chips in the
    edges, the split from y 3 to 19 with fangs in its walls, 5 (front) and 6 (back) riveted patches and a row of studs along the border;
  * the four shards of the old fracture (y 1 | 11 | 22 | 33 | 49) are groups `s1..s3` (the base shard stays), so the Black Meteorite break is done with real parts;
  * the dark spots: jagged holes into the void (88 void quads: the split, the spots), each with a charred, glowing rim and cracks running out of it on the skin;
  * the hilt: a forged crossguard with hooked quillons and rivets, a ricasso clamp, a leather grip under two cloth wraps and four iron bands, a coin pommel with the **five-leaf clover in
    relief** on both faces (group `clover`), a crimson stone in a claw setting, a cord ring, four torn rags (group `rags`);
  * floating parts: nine shards of black glass with crimson hearts orbiting the blade (`orbit`), six chips of the blade hanging near the point (`fragments`), five clover leaves circling the
    pommel (`leaves`).
* **Skins** `textures/entity/weapon_vu/demon_slayer_sword.png` (2048 px: black iron with grime, rust, worn bright rims, an engraved lore sentence "FORGED AROUND NOTHING" / "WHERE MAGIC WANTS TO BE", rune
  rows, a clover at the foot, charred hole rims, crack networks), `_glow.png` and `_veins.png` (emissive maps at half size), `_lod.png` for the LOD mesh. The shared `halo.png` is the genesis_weapons sheet.
* **Effect data** `assets/nusmp/visual_upgrade/demon_slayer_sword.json`: 19 animation groups, 16 ambient layers, 15 idle animations, a draw animation, a swing trail,
  a signature burst for each of the three acts and the looped sound id `nusmp:item.genesis.demon_slayer.absence_hum_loop` (a string only; the audio is another task).
* **Renderer**: the Genesis weapon pipeline, `client/relics/item/vu/VuRenderer` (body, void, core glow, the vein wave, halo cards, flares, LOD, draw animation). New:
  * `client/relics/item/vu/DemonSlayerVu` (called from the top of `DemonSlayerRenderer#renderByItem`): reads the stack data the old renderer read (`MeteorUntil`, `Resonance`, `BladeGrowthUntil`) **without
    copying it** and hands the fracture and the resonance to the pose; the growth scale is applied as before. Its client tick plays the signature bursts of the three acts for the local player
    (Black Meteorite and Black Divider from the stack data the server writes, Conceptual Severance from the start of a full-charge swing) and the embers of the Resonance and of the fracture, through
    the shared client particle budget.
  * `visual/demonslayer/DsDynamics` (pure maths): the three animation kinds `VuPose` does not know. `fracture`: a shard drifts out along its axis, turns and shivers (the old `shardPose`, with the same
    30-tick curve `DemonSlayerMesh.fracture`); `fracture_glow`: the crimson light between the shards follows the fracture; `resonance`: the clover swells and the bloom behind it brightens and
    quickens with the Resonance (0..4), as the old bloom did.
* **Generators** `tools/visual_upgrade/demon_slayer/` (python3, numpy, PIL; deterministic; they import the genesis_weapons modules and edit none of them): `item_demon_slayer_sword.py` (geometry, painters,
  effect lists), `ds_build.py` (meshes, skins, spec), `ds_measure.py` (baseline and after), `ds_preview.py`, `ds_doc.py`. Hook: one `if` block at the end of `main` in `tools/gen_weapon_models.py`
  (`python3 -B tools/gen_weapon_models.py visual_upgrade_demon_slayer [--measure] [--previews]`).

## Measured: mesh
| | before (old pixel model) | after | ratio |
|---|---|---|---|
| body quads | 61 (rest), 69 (fractured) | 4 296 | 70.4x |
| welded body vertices (cap 6000) | 80 | 4 013 | 50.2x |
| void quads | 23 | 88 | |
| core glow / veins / halo-card quads | 2 (bloom) | 2036 / 1496 / 48 | |
| flares | 0 | 7 | |
| animation groups | 0 | 19 | |
| LOD body quads (beyond 12 blocks) | none | 347 (8.1 %), 373 welded vertices | |

The old model is tiny (61 quads), so the quad ratio is not bounded by the vertex cap here (it was for the other Genesis weapons, whose old meshes had 700 to 3 900 quads): the new
mesh has 70 times the quads and still sits at 4 013 of the 6 000 welded vertices the performance guard allows in the hand.

## Measured: skins
| | before | after | ratio |
|---|---|---|---|
| skin size | 64 px | 2048 px | |
| painted texels | 2 462 | 3 848 166 | 1563x |
| detail texels (not a flat fill) | 2 162 | 2 830 750 | 1309x |
| emissive maps | the bloom glow (64 px) | glow 1024 px, veins 1024 px | |

The old skin is a 64 px pixel texture. Painted = alpha above 8; detail = painted texels whose colour differs from the mean of their four neighbours by more than 6 of 765.

## Passes and effects
| | before | after |
|---|---|---|
| emissive / extra passes | 4 (void shader, clover, bloom glow, fracture crack light) | 5 (void_shader, core_glow_additive, energy_veins_translucent_wave, halo_shafts_additive, flare_billboards_additive) |
| ambient effect layers | 1 | 16 |
| idle animations | 1 | 15 |
| draw (equip) animation | no | yes (16 ticks, 6 groups unfold) |
| swing trail | no | yes (`absence_cut`) |
| signature VFX on the item per act | none on the item (the world effects `DEMON_METEOR` / `NIHILITY_ZONE` of `DemonSlayerLayer` exist) | 3 / 3 acts: black_divider, black_meteorite, conceptual_severance |
| looped sound id | none | `nusmp:item.genesis.demon_slayer.absence_hum_loop` |

Ambient layers after: anti_magic_shards, guard_rune_ring, guard_rings, absence_holes, blade_aura, void_arcs, field_rings, field_floor, clover_leaves, drifting_fragments, torn_cloth, pommel_bloom, blade_wake, hot_flares, absence_motes, ground_cast.
Act-driven states kept from the old renderer and now on the new groups: the fracture (shards `s1..s3`, light `crack_1..3`), the Resonance (`clover`, `bloom`), the Black Divider growth (the pose).

## The score V (weights fixed, the formula of genesis_weapons)
V = sum of weight x (after / before): mesh detail 0.35 (body quads), texture detail 0.20 (detail texels), emissive passes 0.15, ambient effect layers 0.20, animation 0.10 (idle + draw + swing trail).

| mesh | texture | passes | ambient | animation | **V after / V before** | V with each ratio capped at 20 | V without the texture term |
|---|---|---|---|---|---|---|---|
| 70.4 | 1309 | 1.25 | 16.0 | 17.0 | **291.6** | 16.1 | 37.2 |

**Read this honestly.** V is 292 only because the texture ratio is 1309 (a 64 px skin against a 2048 px one); with every ratio capped at 20 it is 16.1, below 20, and the
emissive-pass term is only 1.25 (the old model already had four extra passes by this count). The binary floors of the brief all hold (body_quads_ge_2500, welded_vertices_le_6000, lod_le_15_percent, skin_painted_texels_8x, emissive_passes_ge_2, ambient_layers_ge_6, draw_animation, swing_trail, signature_on_every_act, sound_loop_id), and so does the per-component
reading "20 x the baseline" for the mesh (70.4), the skin (1309), the ambient layers (16) and the animation (17); the pass count is the one place it does not (the
count of passes cannot honestly grow 20-fold). Whether the sword looks 20 times better has **not been seen in a game**.

## NEW / REPLACED / KEPT
| What | Status |
|---|---|
| `weapon_meshes_vu/demon_slayer_sword{,_lod}.txt`, `textures/entity/weapon_vu/demon_slayer_sword*.png`, `visual_upgrade/demon_slayer_sword.json` | NEW (alongside the old files) |
| The model drawn in hand, on the ground, in frames and on mobs | REPLACED by the upgraded one while the client flag `visual.upgrade` is on (default); the old model is drawn again with the flag off |
| `client/DemonSlayerMesh.java`, `client/DemonSlayerRenderer.java` (the pixel model and its drawing), `textures/entity/demon_slayer_sword.png`, `demon_slayer_bloom*.png`, `DemonSlayerLayer` | KEPT (the fallback look and the world VFX; the renderer gets two lines, see below) |
| Item id, stats, acts (Conceptual Severance, Black Meteorite, Black Divider), the Nullification Field, engravings, Better Combat files, display presets, relic grade, the inventory icon `demon_slayer_sword_icon` | KEPT, untouched |
| Black Meteorite fracture, Resonance bloom, Black Divider growth | KEPT as behaviour, reimplemented on the new groups (`DsDynamics`); the old code path still runs with the flag off |
| Ambient / swing / act particles on the item | NEW (the shared `VuFx` tick finds the item by its id; `DemonSlayerVu` adds the act bursts and embers) |
| Tooltip | one added line (lang key `nusmp.demon_slayer_vu.tooltip`, fragment `tools/lang/demon_slayer_vu.json`) |
| Time Magic VFX | not touched |

## Edits to existing files (all additions)
* `client/DemonSlayerRenderer.java`: in `renderByItem` two calls (`DemonSlayerVu.drawGui` in the GUI branch before the icon, `DemonSlayerVu.draw` before the old drawing), and `DemonSlayerVu.init()` in `init`.
* `client/relics/item/vu/VuRenderer.java`: one line in `groupMatrix` (`DsDynamics.apply(sg, ps, time)`, a no-op unless a Demon-Slayer is being drawn).
* `NUSMP.java`: `DsSelfTests.register()` next to `VuSelfTests.register()`.
* `item/DemonSlayerSwordItem.java`: one tooltip line.
* `tools/gen_weapon_models.py`: one `if` block at the end of `main` (the hook above).

## Config and performance guard
The client switches of the genesis_weapons slice apply unchanged (`config/nusmp-visual-upgrade-client.toml`): `visual.upgrade` (false = the old model), `visual.particles`, `visual.trails`, `visual.guiModel`
(false: the old icon stays in the slot; true shows this mesh, tilted). The effect quality is the performance config's `vfxQuality`: at `low` there are no halo cards, flares, animation or particles (the
fracture and the resonance still pose the shards and the clover); at `medium` no flares and half the particles.
Full mesh up to 6 000 welded vertices in the hand (4 013); the LOD mesh (347 quads, 8.1 %) beyond 12 blocks; the additive passes skipped beyond 24; the stack data is read with
`CustomData#getUnsafe` (no copy), every matrix and vector is the pipeline's scratch object; particles go through `PerfBudgets.clientParticles` and at most `VuRules.particleBudget` a tick.

## Tests
`DsDynamicsTest` (the fracture, the glow, the resonance, the hook, the old curve), `DemonSlayerAssetsTest` (the assets and the kept old ones, the caps, the placement of the old renderer, the effect data and
the groups the renderer reads, that the shards hold exactly the blade between the old cuts, the ratios and floors of `demon_slayer_after.json`, the baseline), the self-test `visual.demon_slayer`
(`DsSelfTests`: the assets in the booted mod, the old model kept, the flag fallback, the distance rules, the maths). The existing `VuAssetsTest`, `VuRulesTest`, `WeaponMeshesTest`,
`BowDisplayGuardTest` and `PerfSourceScanTest` pass.

## Previews (software renders, not the game)
`docs/previews/visual_upgrade/demon_slayer/demon_slayer_sword.png`: row 1 the old model (held, side, front, hilt detail), row 2 the new model at rest, row 3 the emissive passes alone, the new
model in Black Meteorite (shards drifted apart, the light between them), at Resonance 4, and the LOD mesh. `demon_slayer_sword_faces.png`: both faces of the new blade close up.

## What could NOT be verified
* Nothing was seen in a game: not the look, the lighting, the additive passes, the void shader on the new holes and split, the halo cards and rings against real backgrounds, the flares, the draw
  animation, the group animation, the swing trail (an approximation of the swing arc, not Better Combat's pose), the act bursts (played for the local player only), the particle budget in a crowd, the
  GPU and CPU cost of about 4 296 body quads plus the emissive passes (the old model drew 88 quads in all).
* That a 2048 px skin does not shimmer at 3 to 12 blocks (entity skins are sampled nearest without mipmaps; beyond 12 blocks the 256 px LOD skin is used); if it does, lower `lod.near` in
  `demon_slayer_sword.json`.
* That the glow and vein maps (half size) sample cleanly at the seams of the packed islands.
* The 16 MB of the skin plus 4 MB of the half-size emissive maps in video memory.
* That the Black Divider growth (1.6 x about the item's centre) leaves the floating parts (the shards, the leaves, the rings reach about 19 units from the blade) inside the view in first person.
* The inventory model (`visual.guiModel`, off by default).
* The sounds: the ids are strings; the audio files are another task.

## Cut / not done
Nothing of the slice was cut. Not part of it: the inventory icon (kept by rule 7), the world VFX of the acts (`DemonSlayerLayer`: kept), the other Anti-Magic weapons.
