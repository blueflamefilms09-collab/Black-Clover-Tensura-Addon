# Fire VFX remake, new round: A "anime cel" | B "volumetric glow" | C "real 3D toon meshes" (PREVIEW ONLY)

Nothing in this round is wired into the game: no Java class, no `assets/` file, no lang key, no version or changelog change. Everything lives in `docs/`, `tools/` and the preview folder.
It follows `fire_research.md` and `fire_proposal.md` (A and B are the directions of that proposal, rebuilt from the new owner pack; C is new and replaces the old "ink and ember" idea).
Not Minecraft pixel style (that was Wind's pick); the `wind_mc` files were used only as a pipeline example.

## 0. Read this first (honest limits)

* **Software render only.** 2x supersampled, headless, painter's order for alpha/additive quads, a depth buffer only for the C meshes. No bloom, no world light, no depth against blocks, no sky/fog of the real game.
  **Never seen in a game.** Each effect is judged from 3 to 4 stills; motion (flip-book on twos, sway, spin) is coded but was not watched as a video.
* **BEFORE is the real thing**: the current layers (`FireSpellLayer`, `FlameFxLayer`, `FlameMagicLayer`) compiled and run through the repo's Java preview. A / B / C are Python re-implementations that copy the timelines of those layers
  (launch curves, durations, fades). For `FLAME_TRAIL` and `FLAME_EXPLOSION` the engine curves (`VfxAnim.slashHead`, `explosionBurst`) were not copied, so the new tiles use an eased approximation and the head can be a bit ahead of BEFORE at the same tick.
* The preview camera uses FOV 54 for all four columns (the repo's old preview used 70), so the pictures are bigger than `fire_baseline.png`; scenes, cameras positions and ticks are the baseline ones (except `FLAME_TRAIL`, camera moved back so the stream is in view).
* **Research source.** The look comes from the owner's pack `Fire Magic VFX Effects` (35 files, all looked at, contact sheets made). No new wiki lookup was done in this round (rule 10: nothing needed to be looked up; spell names and canon notes are those of `fire_research.md`, EN wiki via search summaries, JA names from the stills' own captions). Not fetched at all: the anime video itself.
* A, B and C are not equally finished. A and B are baked textures drawn the way the engine already draws flames. C is hand-authored low-poly geometry with a first palette; it needs real art time (section 6).

## 1. What is in the folder

| File | What |
|---|---|
| `docs/previews/vfx_remake/fire_new/fire_compare.png` | BEFORE \| A \| B \| C for all 11 shapes, same camera, same tick (the main picture) |
| `.../fire_compare_ref.png` | same, with the mapped canon still of the pack on the left |
| `.../fire_A.png`, `fire_B.png`, `fire_C.png` | one sheet per direction, 3 to 4 ticks per shape |
| `.../fire_C_meshes.png` | every authored C mesh alone, three views, with its quad / vertex count |
| `.../fire_budget.json` | peak vertices per effect and direction over the whole life of the effect |
| `.../tex/` (35 PNG + `tex_sheet.png`) | the A and B textures; `.../meshes/` (18 `.txt` + `meshes.txt`) the C meshes in the sdw3 mesh format |
| `tools/gen_fire_new_textures.py` | A + B textures (Pillow + numpy, fixed seeds) |
| `tools/gen_fire_new_meshes.py` | C meshes; imports `Mesh` and the loft helpers of `tools/gen_sdw3_models.py` (not copied, not changed) |
| `tools/vfx_preview/fire_new_{raster,out,effects,preview}.py` | rasteriser, quad / mesh collector, the 33 builders, the driver |

Rebuild: `python tools/gen_fire_new_textures.py && python tools/gen_fire_new_meshes.py && python tools/vfx_preview/fire_new_preview.py` (needs JDK 21 and `tools/vfx_preview/lib/joml-1.10.5.jar`; `fire_new_preview.py --meshes` redraws only the mesh sheet; a run with scene keys redraws only those rows and overwrites `fire_compare.png`, so run all for the final images).

## 2. The pack, and which still each fire shape maps to

The pack holds 35 files: 25 anime stills / manga panels (200 px wide), 2 wiki screenshots (the spell lists: Defensive "Ignis Columna", Supplementary "Flame Rope", "Phoenix Feathers Robe", "Duo Leo", "Leo Palma" ..., Offensive "Crimson Eruption"), and 8 tutorial / sprite sheets
(`b669a565` cel fire tutorial: silhouette, yellow highlight, blur, glow, particles; `4aa0b176` cel flame sprite sheet; `ae87718a` flame swoosh sheet; `2d2ffc50` / `bd4fe50e` flame effect animation sheet (the same image twice); `557b2983` flame crescents; `b3282f8d` "Fire and flames tutorial"; `bc67e58c` "How to draw a pixel art flame").

Style facts taken from the pack and used:
* cel fire is ONE solid red-orange silhouette with nested flat inner shapes (orange, gold, yellow, hot white) that sit low and lean with the flow; tongues hook sideways; no black outline (tutorial `b669a565`, `Burst_Javelin`, `Grand_Slam`);
* big spells are flat yellow-white with soft orange rims and radial speed lines (`Magna_Exploding_Fireball`, `Calidus_Brachium_Barrage`, `Leopold_wild_bursting_flame`);
* round fireballs have a red-orange scalloped rim and a pale centre (`Combo_Eruption`); the lion of Leo Rugiens is a huge head with open jaw and a white-hot mouth (`Fuegoleon_Leo_Rugiens`).

| # | Shape (stays registered) | Closest canon still (source: owner pack) | Why |
|---|---|---|---|
| 1 | `FIRE_LION` Leo Rugiens | `Fuegoleon_Leo_Rugiens.webp` (+ manga `Fuegoleon_easily_defeats_Carl`) | giant lion head, jaw open, white-hot mouth |
| 2 | `FIRE_SPEAR` Sol Linea | `Burst_Javelin.webp` | a lance wrapped in hooked cel flame tongues |
| 3 | `FIRE_PILLAR` Ignis Columna | `Magna_burns_his_pies.webp` (+ tutorial column / twister) | tall flame columns; canon Ignis Columna is only a name in the wiki screenshot |
| 4 | `FIRE_BURST` flame explosion | `Magna_Exploding_Fireball.webp` | white-yellow ball, radial speed lines, swirling orange ring |
| 5 | `FIRE_SPIRAL` Spiral Flame | `Leopold_spiral_flame.webp` (+ `Grand_Slam`, manga `Profound_Spiral_Flames`) | wide swirling flame sheets round a white core |
| 6 | `FIRE_WILD` Wild Bursting Flame | `Leopold_wild_bursting_flame.webp` | round fireballs and long light beams in all directions |
| 7 | `FLAMEFX_CAST` | `Calidus_Brachium.webp` (+ tutorial comet) | forward burst along the arm |
| 8 | `FLAMEFX_ZONE` | `Calidus_Brachium_Purgatory.webp` (blue in the still: recolour) (+ tutorial ring of fire) | concentric fire arcs on the ground |
| 9 | `FLAMEFX_BURST` | `Calidus_Brachium_Barrage.webp` | fat radial spokes from a white centre |
| 10 | `FLAME_TRAIL` | `Magna's_Vanishing_Fireball.webp` (manga) (+ tutorial comet) | fireball with a straight streak |
| 11 | `FLAME_EXPLOSION` | `Combo_Eruption.webp` | cluster of round fireballs with red rims |

Not mapped to an existing shape (candidates for NEW shapes, owner decision): `Salamander's_Claw`, `Salamanders_Breath` (giant sphere), `Shining_Flame_Cross`, `Flame_Spike`, `Magna_Exploding_Buckshot` / `Prison_Kill_Buckshot` / the two Lufulu scattershot panels (many small fireballs), `Mereoleona_pummels_giant_demon`.

## 3. The three directions (how each is built, what it needs)

**A anime cel.** Textures with baked flat tones (`A_tongue_a..c`, `A_ball_a..c`, `A_body_a..c` stream, `A_lion_a/b`, `A_swoosh_a/b`, `A_ring`, `A_rays`, `A_flash`, `A_smoke_a/b`, `A_chip_a/b`), drawn ALPHA, one soft additive halo behind big shapes, flip-book frame = `floor(age/2)`, per-sprite offsets from the seed.
The lion is a drawn texture (cel head + mane tongues); everything else is tongues, balls and streams. Engine need: a client option, flame textures, ALPHA draw (the engine already draws ALPHA before ADD).

**B volumetric glow.** Soft puffs with a baked radial heat ramp (white-yellow core, gold, orange, red rim), eroded by noise (`B_puff_a..d`, `B_tongue_a/b`), additive halos / rays / rings / embers, dark soft smoke. No outlines. Cheapest to build, most overdraw (large soft quads; the halo of `FIRE_BURST` is about 8 blocks wide).

**C real 3D toon meshes.** Real geometry authored in the sdw3 mesh format (`q layer (x y z u v)x4 nx ny nz r g b`): flame tongues (S-shaped lick with a hooked tip and thickness, 3 sizes of 16 / 12 / 9 quads, 3 variants each swapped every 2 ticks = a mesh flip-book), a low-poly fireball, a gem fireball, a spike, a column, a helix of two strands, a lance, a ground ring and a lion head (29 quads: skull, muzzle, dropped jaw, white-hot mouth, fangs, eye slits).
Shading is decided PER QUAD from the quad normal, like the per-quad tint that `SpiritDiveWind3Mesh` already hands to the vertex consumer: 3 flat tones from a fixed key light (lit yellow-orange, mid, shade deep red), the emissive core (pale yellow) where the quad faces the viewer, a bright rim where it grazes the silhouette, and the heat tint baked per quad along the tongue (gold root, red tip).
Per frame this is one dot product and one colour write per quad (at most 100 quads). The mesh is opaque emissive (the same render type the Spirit Dive pieces use), the glow and sparks are 1 to 5 additive cards per effect.
Engine need: a mesh loader and a draw call in the VFX layer (the Spirit Dive code lives in `client/aura`, the VFX layers only have the quad buffer), and the 400-vertex activation cap is the binding limit (section 4).

## 4. Budget per effect against the current cap (vertices; 4 vertices = 1 quad)

Engine cap per activation: 400 vertices (`VfxVertexBuffer`). "Current cap" = the peak of today's layer in the baseline sheet. Peaks are over every tick of the effect, from `fire_budget.json`.

| Shape | Current | A | B | C | C quads (mesh part) |
|---|---|---|---|---|---|
| FIRE_LION | 264 | 88 | 160 | **384** | 92 |
| FIRE_SPEAR | 196 | 68 | 84 | **248** | 60 |
| FIRE_PILLAR | 220 | 92 | 128 | **372** | 89 |
| FIRE_BURST | 244 | 116 | 144 | **376** | 88 |
| FIRE_SPIRAL | 268 | 124 | 196 | **340** | 83 |
| FIRE_WILD | 288 | 200 | 364 | **308** | 66 |
| FLAMEFX_CAST | 180 | 68 | 92 | **232** | 54 |
| FLAMEFX_ZONE | 156 | 108 | 192 | **296** | 70 |
| FLAMEFX_BURST | 216 | 104 | 140 | **332** | 78 |
| FLAME_TRAIL | 164 | 52 | 96 | **248** | 60 |
| FLAME_EXPLOSION | 292 | 116 | 164 | **368** | 86 |

* **A is below the current cap everywhere** (about 30 to 70 percent of it): a cel flame is one quad with a complex silhouette.
* **B is below the current cap except** FIRE_WILD (364 vs 288) and FLAMEFX_ZONE (192 vs 156); B stays under 400 everywhere; B is the one with real overdraw risk (large soft quads).
* **C is over the current cap in all 11 shapes (bold)** because geometry has to spell out the silhouette that a texture gets for free; it is under the engine cap of 400 everywhere (largest: lion 384, burst 376, pillar 372, explosion 368). A C release must either accept these numbers or lower them with the LOD (`ctx.seg`): far LOD would drop tongues and one ring of facets (about half).
* Worst case on a public server (rule 11), all numbers client side, 0 extra packets, 0 server work: 20 casters of the largest C effect in view = 20 x 384 = 7,680 vertices = 1,920 quads (today 20 x 292 = 5,840 = 1,460 quads); A: 20 x 124 (spiral) at most = 2,480 vertices. C adds 1 CPU dot product per quad and frame (at most 100 per effect = 2,000 for 20 casters), no allocation if the final code reuses its vectors (the preview does not promise this).
* Each effect keeps the per-activation cap of 400, the existing `ctx.seg` LOD and the existing distance culling; far players are skipped as today. Not measured in a game: `PerfProbe` numbers do not exist for these builders.

## 5. Per effect: what each direction shows (from the final images)

| Shape | A cel | B glow | C 3D toon | Pick |
|---|---|---|---|---|
| Lion | the only direction where a lion face reads (cel head, open jaw, white-hot mouth, mane tongues) | reads as a fireball with a mane of puffs, no face | low-poly lion head with mane tongues; reads as a beast head, rough | A |
| Sol Linea | cel lance with tongues and a ball head, good | clean glowing beam, good | faceted lance, flat; weakest | A or B |
| Pillar | best of the set: layered cel flame with ground ring | soft column, good | smooth horn with side licks, pale | A (C as alternative) |
| Burst | fireball + radial lines + ring, matches the still | soft cloud, good | thorny crown; reads as spikes not flame | A |
| Spiral | cel swoosh sheets round a core, busy but on style | helix of puffs round a beam, clearest | helix of facets, reads as a ribbon; readable | B (A as alternative) |
| Wild | cluster of large cel fireballs; cluttered from this camera | soft blob; weakest in readability | three pale gems; weak | needs another pass (A with fewer, larger balls, or B) |
| Cast | cel stream, good | beam + halo, good | ball with licks, ok | A or B |
| Zone | ring of cel flames, good | ring of soft flames, good | crown of flame horns; the most "3D" result, too spiky | A (C interesting) |
| FX burst | cel star + radial lines, good | soft, a bit muddy | mesh crown on a ring | A |
| Trail | cel stream | glow stream | lance + ball | B or A |
| Explosion | cluster of cel fireballs, good | soft layered fireball, good | helmet-like gem with horns; weakest | A |

## 6. Recommendation

1. **Base look: A (anime cel)** for the hero shapes (lion, pillar, burst, explosion, zone, cast, fx burst): it matches the pack best, is the cheapest, and is the only direction where Leo Rugiens looks like a lion.
2. **B for beams and the spiral/wild effects** (spiral, trail, perhaps wild): soft glow suits long light beams and the Leopold stills.
3. **C is a good idea for 2 or 3 hero pieces only** (lion head, pillar, maybe the spiral helix), behind its own option value (`V2C`), because: it costs 2 to 3 times the vertices, it needs a mesh draw path in the VFX layers, and the geometry here is a first pass (flat palette, blocky lion head, spiky tongues).
   It is also the direction closest to the Wind Spirit Dive weapons the owner liked; to reach that polish the meshes need real modelling time (more section points on the tongues, hand-placed facets, a texture atlas for flame veins).
4. A mix per shape, all behind the one client option `vfx.element.fire = V1 | V2A | V2B | V2C | V2` (V2 = the mix), exactly as in `fire_proposal.md`, old code kept as V1.

**Old effects that could be kept untouched (V1 stays default for them if the owner prefers):**
* `FLAMEFX_ZONE`: the sun-lion sigil on the ground is a mod identity none of the pack stills has; A/B only restyle the rim flames. Keep V1 for the disc, take only the new rim flames.
* `FLAME_TRAIL`: a thin stream with a head; A/B/C make it fatter, not clearly better.
* `FLAMEFX_CAST` (partly): the charge sigil at the caster stays; only the stream is restyled.
* Not touched in any case: `DemonFireLayer`, the Salamander armour / `FlameAura`, Time Magic (rule 3), all server code, packets and damage numbers.

## 7. NEW vs REPLACEMENT (rule 5)

* REPLACEMENT of the look of all 11 existing shapes (same `VfxShape` ids; nothing deleted; V1 = today's code).
* NEW (files only, nothing registered): the textures, the meshes, the preview tools, this doc and the images. NEW content candidates that would need new shapes: Salamander's Breath / Claw, Shining Flame Cross, Flame Spike, Buckshot (section 2).
* KEPT: every old shape, layer, texture, packet, config and test; `fire_research.md`, `fire_proposal.md`, `fire_baseline.png` and `scenes.d/fire_baseline.json` are unchanged.

## 8. Open decisions for the owner

1. Which direction per shape (recommendation in section 6), and whether C is worth a mesh draw path for 2 to 3 hero effects.
2. Lion: keep the cel lion head of A (a flat drawing in a 3D world; always faces the camera) or invest in the C head.
3. Zone: keep the sun-lion sigil (mod identity) or go fully canon (concentric fire arcs, pack still `Calidus_Brachium_Purgatory`).
4. Ignis Columna: stays a pillar (A/B/C pillar here) or becomes the defensive vortex (JA meaning, unverified).
5. Whether `FIRE_WILD` is redone (it is the weakest in all three directions here) and whether the unmapped pack spells should become new shapes.
6. Accept the C vertex numbers (up to 384 per effect, engine cap 400) or ask for LOD-first meshes.
7. The same `vfx.element.*` option family for Water / Wind / Earth, and photosensitivity: the cel flashes (`A_flash`, 5 ticks) and rays would need the `reduced flashes` option from the old proposal.

## 9. Rounds and what changed between them

Round 1: first full render; found: A spiral and wild cluttered, C tongues oversized and pale, C over 400 vertices in 3 shapes, rectangular edges of soft puffs and smoke.
Round 2: new S-shaped tongue meshes, new swoosh texture for the A spiral, fewer tongues so every C effect fits 400, smoke edge fix. Round 3 (partial): toon palette made more contrasty, smaller C gems / tongues, A flash made hard (no half-transparent star), A wild balls enlarged, puff radial window (rectangular edges gone).
Round 4: final render (these images). Not fixed: C palette is still pale on lit faces, C lion head is blocky, C wild is weak, A wild is cluttered.
