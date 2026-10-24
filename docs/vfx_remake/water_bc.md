# Water VFX remake: hybrid "BC" = C's 3D toon cores wrapped in B's volumetric glass (PREVIEW ONLY, nothing wired into the game)

Owner brief: "combine version B and C and show me what they will look like" (WATER only). Phase A of `water_new.md` showed A anime cel, B volumetric glass, C real 3D toon meshes. BC keeps C's meshes as the
SOLID CORE of every effect (the dragon head with a real jaw, the spear, the sphere, the dome, the crown horns, the ridged rings, the feather wings) and wraps / trails / fills them with B's translucent glass
layers (body sheen shells with caustics, flow ribbons, bubbles, glints, mist, glass crowns), so it reads as water with shape AND thickness.
Nothing in `src/` or `assets/` was changed; no version, changelog, README, lang file or workflow was touched. Time Magic VFX untouched (rule 3). Old A / B / C files untouched (rule 2).

**Honest limits (read first).** Everything below is a headless software render (numpy, z-buffered meshes, painter-sorted sprites with a depth test against the meshes). No bloom, no world light, no terrain
beyond a checker floor, the game's WATER blend is drawn as plain alpha, nothing was seen in a game, GPU cost was not measured, the textures are procedural (Pillow), motion was judged from 3 frames per
effect. Translucent mesh hulls and glass sprites are not sorted against each other (meshes first, sprites after), so a glass sprite that should sit behind a translucent hull is drawn over it; in the game this
needs a sort or a fixed draw order. Quad counts are counted from the generated geometry, not from a running mod.

## 1. Images (`docs/previews/vfx_remake/water_new/`)

| File | What |
|---|---|
| `water_bc_compare.png` | per shape: BEFORE (the real layer class) , B volumetric glass , C real 3D toon , BC hybrid; same camera and tick as `water_compare.png` (the first three columns are cut out of that file, only BC is new) |
| `water_bc_sheet.png` | BC alone, 3 ticks per shape (the same ticks as the A / B / C sheets) |
| `water_bc_closeups.png` | roar head, nest dome, burst crown at 2x the tile size (1120 x 840 each) |
| `water_bc_lod.png` | extra: BC LOD0 (near) , BC LOD1 (far or crowded) , A sprite fall-back (over the frame ceiling) for roar, nest, burst, valkyrie |
| `water_bc_budget.json`, `meshes/counts_bc.txt` | peak quads per effect over its whole life, and per piece |
| `meshes/bc_*.txt`, `tex/bc_*.png` | the 13 new core pieces (26 files with the `_lo` versions) and the 4 new glass sprites |

Rebuild: `python tools/gen_water_bc_textures.py`, `python tools/gen_water_bc_meshes.py`, `python tools/vfx_preview/water_bc_preview.py [shape ...]` (numpy + Pillow; the BEFORE / B / C columns need
`water_compare.png`, which `tools/vfx_preview/water_new_preview.py` makes, and that one needs JDK 21 for the BEFORE column).

## 2. How a BC frame is put together (the plan for the game, not run there)

1. Meshes first: toon shaded per quad on the CPU (3 tones by the quad normal against a light from the upper left, one small white specular, a pale rim at the grazing edge), emissive layer additive.
   Solid cores use a deeper palette than C ("core") so the glass on top reads as lighter water over a dark body.
2. Translucent mesh HULLS (a slightly larger copy of the skull and neck, the Cradle sphere, the Nest dome, the crown horns, the Valkyrie feathers): glass palette, alpha 0.2 to 0.65 at the face-on parts and
   higher at the grazing edge (fresnel, from the quad normal against the view), no depth write.
3. Glass sprites (B's layers, drawn after the meshes, depth TEST on, depth write off): body sheen shells, flow ribbons, bubbles, glints, mist puffs, crown sheets, ground ripples. A shell behind the body is hidden
   by it, a shell in front covers it, so the core is seen through glass; this is what gives the volume.
4. Far LOD and fall-back as in C, see 5.

## 3. What changed against B and against C

| Effect | From C (kept) | From B (added around it) | New in BC |
|---|---|---|---|
| ROAR | head with open jaw, fangs, horns, body pieces along the sine path, tail | body sheen shells (wide pale envelope behind + clear caustic shell in front, overlapped so their ends do not show as seams), 2 flow ribbons winding round the body, bubbles in the wake, mist over the neck join, drops, eye glint, soft lens over the skull | landing: horns + glass crown + ripples |
| LANCE | spear with leaf head, barbs, tail fins, glowing core | wide glass shell behind it, caustic sheen over the shaft, 2 helical flow ribbons (replace C's mesh wisps), mist trail, bubbles, tip glint | |
| CRADLE | whirl bands turning on their own axes, ground ring | the sphere itself is a glass hull (fresnel rim) with B's rim shell sprite, caustic sphere and 2 crossed caustic sheets, bubbles inside, the bubble chain as glass bubbles, mist | C's boxy 5-sided bubble chain is gone (sprites) |
| NEST | dome with spiral foam bands, foam collar, ring | translucent dome (players inside stay visible), B's dome sheets, 3 swirl sprites as the eddies of the still, bubbles, mist, glint | |
| BURST | crown horns, ripples | the glass crown sheets that fill the gaps between the horns, a droplet bubble at every tip, flying drops, bubbles, mist, glint | |
| SPLASH | small crown | same as burst, smaller | |
| RING | ridged rings | B's ripple sprite under each ring, bubbles | |
| VALKYRIE | feather blades | B's wing membrane behind them, halo ring + glow + glint, drips and bubbles, mist at the shoulder blades | |

### The weak spots listed for C in `water_new.md` section 10

| Weak spot | Fix | Seen in the preview |
|---|---|---|
| Nest dome: spiral ridges unreadable | `bc_dome`: smooth hemisphere, three wide spiral foam BANDS lying on the hull (no raised tent), a thin foam collar, plus 3 swirl sprites on the camera side; the dome is translucent | yes: dome with swirls and bands reads, the bands are still slightly shard-like at 2x |
| Valkyrie feathers fan like a star from behind | one wing piece per side (`bc_wing_r/l`): an arm bone and six closed diamond feathers rooted along it and fanned from nearly horizontal (long, low) to nearly vertical (short, high), each leaning on its neighbour, with B's membrane behind | improved: reads as two wings, still busy from straight behind |
| A/B head-body seam | skull has a round occiput and a neck that runs back into the first body piece, a foam collar over the join, mist puffs over it, and the glass shell of the body runs on under the head | seam gone in C parts; the glass shell strips still show faint rectangular ends at 2x |
| Two-sided quads (culling) | every flat blade (fangs, fins, tail fins, barbs) is written as a front AND a back quad; feathers are closed diamonds. Costs about 5 percent more quads (roar about 17 of 320). If the real render type has no culling the back faces can be dropped | not visible in a software render (it is two-sided anyway); this is a file-format change, not a look |
| Also: ripple read as a concrete kerb | `bc_ripple`: lower, thinner ridge, glass palette, translucent | yes |
| Also: crown horns read as flat spikes | `bc_horn`: five sections, rounded, broad tongues that curl outward, tongue palette, droplet bubble at the tip | partly: still plank-like when seen along their length (closeup 3) |

## 4. Budgets (quads; vertices = 4 x quads)

Today's cap is 100 quads (400 vertices) per activation (`MAX_VERTICES_PER_ACTIVATION`). BC cannot go through that buffer; it needs C's own mesh path, as proposed in `water_new.md` section 5.
Peaks are taken over every tick of the effect's life (the Roar and Burst peaks are at the landing, when the crown and the ripples appear on top of the head).

| Effect | Today live | B | C LOD0 | **BC LOD0 mesh + sprites = total** | **BC LOD1 mesh + sprites = total** | A fall-back | Proposed mesh cap LOD0 / LOD1 |
|---|---|---|---|---|---|---|---|
| ROAR | 51 | 42 | 250 | 320 + 54 = **374** | 169 + 32 = **201** | 20 | 330 / 170 |
| LANCE | 51 | 15 | 153 | 110 + 39 = **149** | 66 + 20 = **86** | 21 | 115 / 70 |
| CRADLE | 39 | 13 | 268 | 176 + 23 = **199** | 120 + 14 = **134** | 12 | 180 / 125 |
| NEST | 39 | 13 | 238 | 120 + 26 = **146** | 79 + 14 = **93** | 10 | 125 / 80 |
| BURST | 30 | 16 | 177 | 208 + 30 = **238** | 96 + 18 = **114** | 13 | 220 / 100 |
| SPLASH | 23 | 9 | 120 | 144 + 17 = **161** | 60 + 11 = **71** | 8 | 150 / 65 |
| RING | 100 (at the cap) | 6 | 84 | 132 + 7 = **139** | 48 + 4 = **52** | 3 | 135 / 50 |
| VALKYRIE | 74 (unused shape) | 12 | 205 | 136 + 16 = **152** | 104 + 9 = **113** | 10 | 140 / 105 |

* Against C (total quads at LOD0): BC is cheaper for CRADLE (199 vs 268), NEST (146 vs 238), VALKYRIE (152 vs 205), equal for LANCE (149 vs 153), because the glass sprites cover the facets, so the cores could be
  low-poly and the bubble chains, wisps and gems became 1-quad sprites. It is MORE for ROAR (374 vs 250: six-sided skull with neck, hull, collar, back faces, landing crown), BURST (238 vs 177: broader horns, hi-res ripples),
  SPLASH (161 vs 120) and RING (139 vs 84: hi-res ripple rings; the lo ring would give about 60). Versus today's cap of 100 quads: 1.4 to 3.7 times at LOD0, about 0.5 to 2 times at LOD1; only the A fall-back stays under it.
* Mesh quads per effect: caps in the last column. Worst single effect on screen: ROAR 374 quads = 1496 vertices at the landing frame only (about 270 mesh quads while it flies).
* **Total per-frame ceiling (proposal): 1200 mesh quads (4800 vertices) + 500 sprite quads (2000 vertices) for all water effects in view together.** Effects are admitted nearest first: LOD0 inside 16 blocks and
  while the running total stays under 70 percent of the ceiling, LOD1 beyond that or above it, the **A sprite fall-back (3 to 21 quads, the A cel sprites of `water_new.md`) once the ceiling is reached**; beyond the existing
  distance cull nothing is drawn. Worst case is therefore bounded at 1200 + 500 quads whatever the number of casters: ten Roars in view would be 7 at LOD1 (1183 mesh quads, 224 sprite) and 3 at A (60 sprite quads).
* Network: zero change (same shapes, same params, same packets). Server: zero (all of it is client drawing; no gameplay, no state). Per-frame allocation: none planned (static scratch arrays like the Spirit Dive painter).
  Effects of far players are skipped by the existing cull and the LOD rule above. Time Magic untouched.
* Not measured: real GPU cost, draw-call count, overdraw of the glass sprites and translucent hulls (the biggest unknown: every glass layer is a full-size alpha blended sprite over the core, 6 to 16 per effect
  covering the same pixels; a particle-heavy fight could be fill-rate bound before it is vertex bound).

## 5. Reading the result (my honest view, for you to overrule)

* Best: CRADLE, NEST, RING, BURST: they read as glass water with a form inside, clearly richer than C and with more shape than B.
* ROAR: the head with its real jaw and the glass body is the strongest "hero" of the set; the body shell still shows faint rectangular strip ends when zoomed, and the head is darker than the body (core palette).
* LANCE: from the side camera the spear is thin and edge-on, so the hybrid adds little there; it needs a 3/4 camera to show.
* VALKYRIE: readable as two wings but busy; the mesh feathers and the B membrane do not follow the same silhouette exactly.
* Compared with A: A is still the cheapest and the closest to the stills; BC is the one that keeps the "real 3D like the weapons" volume while looking like water. A is the planned fall-back.

## 6. New vs replacement (rule 5)

NEW: the BC look (an additional variant); 13 core pieces and 4 glass sprites (all `bc_` files); the BC preview scripts. REPLACEMENT: none in the repo (if BC is chosen it replaces today's look only behind the
option `vfx.element.water = V1 | A | B | C | BC`, V1 stays). KEPT: all A / B / C files, all of `src/` and `assets/`, the six unused shapes, gameplay numbers, sounds.

## 7. Open decisions

1. BC as the look for which shapes (all, or only ROAR / BURST / CRADLE / NEST with A elsewhere)?
2. May the mesh path have its own budget (1200 + 500 per frame, LOD1 beyond 16 blocks, A fall-back above the ceiling)?
3. Keep the dragon's body in 4 pieces (cheap) or lengthen it (the Roar still has a long banded body)?
4. Back faces for blades (about 5 percent more quads) or rely on a no-cull render type (needs checking in the game)?
5. Glass draw order: fixed (meshes, then glass sprites, as in this preview) or sorted (needs a per-effect sort)?
