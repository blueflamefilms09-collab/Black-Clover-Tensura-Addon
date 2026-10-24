# Star Magic VFX remake: A "anime cel" | B "volumetric glow" | C "real 3D toon meshes" (PREVIEW ONLY)

Nothing in this round is wired into the game: no Java class, no `assets/` file, no lang key, no version or changelog change. Everything lives in `docs/`, `tools/` and the preview folder.
The design (lore, node mechanics, ability list, combat loop, server rules) is in `docs/vfx_remake/star_magic_design.md`; this file is the look.
Same process as `fire_new.md` / `water_new.md`: BEFORE comes from the real Java layer, A / B / C are Python re-implementations drawn on the same cameras. Time Magic VFX untouched (rule 3).

## 0. Read this first (honest limits)

* **Software render only.** 2x supersampled, headless, painter's order for alpha / additive quads, a depth buffer for the C meshes (alpha and additive quads are depth-tested against it). No bloom, no world light, no depth against blocks, no sky or fog of the real game.
  **Never seen in a game.** Each effect is judged from 3 stills (plus a 3x3 close-up sheet); motion (stepped twos on A, pulses on B, spin on C) is coded but was not watched as a video.
* **BEFORE is the real thing**: `ElementIdentityLayer` (the layer that draws `VfxShape.STAR_MAGIC`) and, for Star Shield, `AuraLayer` (`SPIRIT_AURA`), compiled and run through the repo's Java preview, called the way the nearest existing page calls them (`tools/vfx_preview/scenes.d/star_baseline.json`).
  There is no placed star, no link line, no barrier today, so the "BEFORE" of those rows is the closest real page, not the same effect (e.g. the placed star's BEFORE is one Stardust Field step; the barrier's BEFORE is the Star Shield aura, which is not even a star effect).
* A / B / C timelines are the ones proposed in the design doc (ticks); they are NOT the BEFORE tick, so each tile is the same PHASE of the effect, not the same tick: the label of every tile shows its own tick. BEFORE tiles show `104 verts` because 104 is one activation; Constellation and Orbit Barrage show 520 because five volley bolts (five activations) are merged.
* **No canon still of Star Magic was available** (the wiki is not fetchable here and the owner's earlier packs contain none). The looks are built from the text of the canon descriptions (four stars, thin straight lines of light, rectangular barrier, whip-thin line, laser from the middle of the quartet, teleport beneath a star) and the general four-point-star look of the series. The gold-white / starlight-blue colours are a design choice. Say so to the art team: it needs a real reference pass before anything ships.
* A, B and C are not equally finished. A and B are baked textures; C is code-authored low-poly geometry with a first palette. A and B were the faster ones to make good; C needs real modelling time.

## 1. What is in the folder (`docs/previews/vfx_remake/star/`)

| File | What |
|---|---|
| `star_baseline.png` | BEFORE: the real current layer, the 7 effects x 3 ticks |
| `star_compare.png` | BEFORE \| A \| B \| C, the 7 effects, same camera (the main picture) |
| `star_A.png`, `star_B.png`, `star_C.png` | one sheet per direction, 3 ticks per effect (start, middle, late) |
| `star_closeups.png` | placed star, Quartile Scutum and Quartile Hasta at 860 x 645 per tile, A \| B \| C |
| `star_budget.json` | peak vertices per effect and direction over the whole life of the effect, and the vertices of ONE primitive per direction |
| `tex/` (28 PNG + `tex_sheet.png`) | the A and B textures (C uses code geometry plus the small glow sprites) |

| Tool | What |
|---|---|
| `tools/gen_star_new_textures.py` | A + B textures (Pillow + numpy, fixed seeds) |
| `tools/vfx_preview/star_new_geo.py` | the C geometry kit (3D four-point star, tube, annulus, lance, panel) and its per-quad toon shader |
| `tools/vfx_preview/star_new_effects.py` | the 3 sets of primitives and the 7 timelines + camera / tick table |
| `tools/vfx_preview/star_new_preview.py` | the driver: Java BEFORE, rasteriser, sheets, budget JSON |
| `tools/vfx_preview/scenes.d/star_baseline.json` | the 7 BEFORE scenes (usable with `preview.py`, e.g. `python tools/vfx_preview/preview.py star_hasta`) |
| reused, unchanged | `preview.py`, `fire_new_raster.py`, `fire_new_out.py` (the rasteriser and the quad collector of the Fire round) |

Rebuild: `python tools/gen_star_new_textures.py && python tools/vfx_preview/star_new_preview.py` (JDK 21, numpy, Pillow; the first run needs `tools/vfx_preview/lib/joml-1.10.5.jar`, which is git-ignored; `star_new_preview.py node scutum` redraws only those rows and overwrites the compare / sheets, so run all for the final images).

## 2. Each effect: the closest canon description it is mapped to

| # | Effect | Canon description it is built from | Source |
|---|---|---|---|
| 1 | Star node (placed star) | "create and manipulate stars at will"; the stars hang in the air; a spot beneath it is the teleport point | EN Star Magic |
| 2 | Constellation lines | connecting several stars forms a constellation and gives stronger defence and attack; thin straight lines of light | EN Star Magic; JA ja.wikipedia (点を繋げて) |
| 3 | Conjunction | with an open grimoire the user holds up two fingers, gestures a circle and teleports to beneath one of their stars (ch. 309 / 310) | EN Conjunction; JA 合天 |
| 4 | Quartile Scutum | four stars form a rectangle, beams between them make a barrier that holds against devil-powered magic (ch. 309 / 310) | EN Quartile Scutum; JA 四天・盾 |
| 5 | Quartile Hasta | light gathered in the middle of four linked stars is fired as a high-penetration laser / spear | JA 四天・槍; EN list (no text); FAN ch. 310 |
| 6 | Quartile Flagellum | starlight links four stars into a line so thin and sharp it cuts through people and walls; swung like a whip (ch. 310) | EN Quartile Flagellum; JA 四天・鞭 |
| 7 | Halation | no canon found; a mod-original finisher: several stars fire beams at one point, the impact blooms | owner's brief |

## 3. The three directions (how each is built, what it needs)

**A anime cel.** Flat baked textures: a gold four-point star with a pale inner star and a white core (`A_star4`), hard-edged lines (`A_line`), a ground sigil of rings and four small stars (`A_sigil`), a flat barrier pane with two glare bands and a border (`A_pane`), a banded spear (`A_beam`), a tapered whip ribbon (`A_whip`, drawn segment by segment with one texture), flat impact stars (`A_burst`, `A_crack`) and speed lines (`A_rays`). Stepped timing: the star alternates two frames every 2 ticks, the sigil turns in steps. One faint soft halo behind each star. Engine need: a client option, the textures, ALPHA draw (the engine already draws ALPHA before ADD).

**B volumetric glow.** Soft bloom: a bloom star with a lens-flare streak (`B_star`, `B_flare`), glowing filaments (`B_line`), nebula dust (`B_dust_*`), a fresnel-edged barrier with aurora bands and a fine grid (`B_pane`), a soft laser (`B_beam`), god rays (`B_rays`), a ring (`B_ring`). All additive. The direction that literally draws the "halation" of the owner's name. Cheapest quads, most overdraw (the largest glow card is about 6 blocks wide).

**C real 3D toon meshes.** Geometry built in code: a 3D four-point star solid (8 ring points + two apexes = 16 triangles), 4-sided tubes for lines and the whip, a flat annulus for sigils and rings, a hex lance with a pointed head and three fins, a 3 x 2 faceted glass pane. Shading is per QUAD like the Fire / Water C rounds: three flat tones from a fixed key light, an emissive white where the quad faces the viewer, a pale blue rim where it grazes; emissive parts (lines, pane, sigil) are flat. A few additive glow cards per effect. Engine need: a mesh path in the VFX layers (the Spirit Dive code lives in `client/aura`), and a distance LOD that swaps C for B sprites.

## 4. Budgets against the current layer (vertices; 4 vertices = 1 quad)

Engine cap: **400 vertices per activation** (`VfxVertexBuffer`). The current Star layer peaks at **104** per activation (140 for the Star Shield aura). The numbers below are whole-effect peaks over every tick; the real game would send the parts (stars, links, pane, lance, whip, flash) as separate activations or entities, each far under 400.

| Effect | BEFORE (one activation) | A peak | B peak | C peak | C mesh quads at the peak |
|---|---|---|---|---|---|
| Star node (placed star + sigil + line) | 104 | 44 | 56 | 380 | 90 |
| Constellation (4 stars, 5 lines, shimmer) | 104 (x5 volley = 520) | 104 | 144 | 676 | 156 |
| Conjunction | 104 | 56 | 80 | 488 | 114 |
| Quartile Scutum | 140 (aura) | 120 | 164 | 784 | 182 |
| Quartile Hasta | 104 | 104 | 144 | 768 | 177 |
| Quartile Flagellum | 104 | 140 | 184 | 556 | 128 |
| Halation (5 stars, 5 beams, flash) | 104 (x5 volley = 520) | 144 | 196 | 860 | 200 |

**One primitive, LOD0 (vertices)** (what the game would spend per node entity / per link / per pane; from `star_budget.json`):

| Primitive | A | B | C |
|---|---|---|---|
| star node | 16 | 20 | 72 |
| link (5 blocks) | 4 | 12 | 52 |
| ground sigil | 4 | 8 | 84 |
| barrier pane | 4 | 8 | 28 |
| spear / lance (4 blocks) | 8 | 16 | 132 |
| thin ray (4 blocks) | 8 | 16 | 24 |
| whip (16 segments) | 64 | 92 | 168 |
| flash | 12 | 12 | 100 |
| ring | 4 | 4 | 40 |

* **A** is at or under the current 104 in 4 of the 7 whole effects (node 44, lines 104, Conjunction 56, Hasta 104); Scutum 120 (the aura it replaces is 140), Flagellum 140 and Halation 144 are sums of several activations. **B** is 56 to 196 as a whole effect. **C** is 4 to 8 times A: whole-effect peaks of 380 to 860; every single primitive is at most 168, so every activation fits the engine cap of 400, but C needs an LOD: beyond about 24 blocks, or when more than 16 stars are in view, draw the C star as the B sprite (20 vertices instead of 72).
* **Worst case on a public server (rule 11)**, all client side, 0 extra packets for stars at rest (vanilla entity tracking): 20 Star mages in view x 6 stars = 120 stars: A 1,920 vertices (480 quads), B 2,400 (600), C with the LOD (16 near at 72 + 104 far as B at 20) = 3,232 vertices (808 quads); without the LOD C would be 8,640 (2,160 quads). Per cast the extra is one whole effect on screen for 1 to 2 seconds: Quartile Scutum A 120 / B 164 / C 784 vertices (C is 7 activations). No per-frame allocation is promised by the preview; the final code must reuse its vectors.
* Each effect keeps the per-activation cap of 400, the existing `ctx.seg` LOD and the existing distance culling; far players are skipped as today. **Not measured in a game**: no `PerfProbe` numbers exist for these builders. The server side (entities, caps, packets, queries) is costed in the design doc section 2.6.

## 5. Per effect: what each direction shows (from the final images)

| Effect | A cel | B glow | C 3D toon | Pick |
|---|---|---|---|---|
| Star node | iconic gold four-point star, sigil of rings; the series' sparkle reads best here | brightest, a clear bloom star with halo; the ground sigil is soft | faceted star reads as a gem / paper star, rotating; ok close up | A (B as the far-LOD look) |
| Constellation lines | clean white lines, small sparkles on the stars; very readable | soft filaments, strong glow, lines a bit thicker | thin tubes with diamond beads, strong silhouette, plain | A or B |
| Conjunction | flat burst with speed lines at the arrival, a ground ring each end; clear | a large lens flare and soft ring, nice but covers the figure | gold ring + faceted star; the ring dominates, weakest | B (A as alternative) |
| Quartile Scutum | rectangle of four stars with a flat glare pane and a hit ring; reads as a "barrier" at once | glowing fresnel pane, beautiful but busy | faceted glass pane with a frame: the best "3D thing" of the set | A for readability, C as the hero version |
| Quartile Hasta | a flat banded plank with a spark head: the weakest (reads as a bar, not a spear) | thin clean beam with a flare head; good | lance with fins and a ring: the only one that reads as a SPEAR | C for the lance (small 132 vertices), B for the glow trail; **needs another pass** |
| Quartile Flagellum | a smooth white tapered whip through four stars; clear | glowing whip, soft, good | segmented tube whip in two blues, good silhouette | A (B alternative) |
| Halation | five flat beams into a ringed flash; busy rays | five soft beams converging on a bloom: the best, the name's meaning | beams + a faceted star and a gold ring: heavy | B |

## 6. Recommendation

1. **Base look: A (anime cel)** for the node, the lines, Scutum and Flagellum: it matches the series' flat four-point sparkle star, is the cheapest, and every effect reads at a glance.
2. **B for Conjunction and Halation** (flash and bloom suit a teleport and a "halation" finisher) and as the far-LOD / low-quality look of the node.
3. **C only for two hero pieces: the Scutum glass pane and the Hasta lance**, behind its own option value (`V2C`), with the LOD above. It is 4 to 8 times A, needs a mesh path, and the geometry here is a first pass (the star solid is a plain bipyramid, the palette is pale).
4. One client option `vfx.element.star = V1 | V2A | V2B | V2C | V2` (V2 = the mix above), exactly as in `fire_proposal.md`; `V1` keeps `STAR_MAGIC` and `ElementIdentityLayer` as they are. The new look needs NEW shapes appended to `VfxShape` (STAR_NODE, STAR_LINK, STAR_CONJ, STAR_PANE, STAR_LANCE, STAR_WHIP, STAR_HALO) and one new layer, because `STAR_MAGIC` is also the beam of hero weapons, celestial props and boss moves (about 24 source files).

**Old effects that can stay untouched:**
* `STAR_MAGIC` / `ElementIdentityLayer`: stays for all 13 old pages and every other caller (it is a decent generic "sparkle beam"; its only fault is that it is the only look of the book).
* The Star Shield aura (`SPIRIT_AURA`): stays with its page; Quartile Scutum is a new effect next to it.
* `never_never_land` swirl: stays until its union spell gets its own redo.

## 7. NEW vs REPLACEMENT (rule 5)

* **REPLACEMENT of the look** of the Star book's single visual only for the casts that change behaviour (Conjunction, Flagellum), behind the option; V1 is today's code.
* **NEW (files only, nothing registered):** textures, geometry kit, builders, preview tools, this doc, the design doc, the images. NEW content that would need new shapes: the placed star, constellation lines, the pane, the lance, the whip, Halation.
* **KEPT:** every old shape, layer, texture, packet, config and test; `fire_*`, `water_*`, `wind_*` files; `scenes.d/*.json` other than the new `star_baseline.json`.

## 8. Open decisions for the owner

1. Which direction per effect (recommendation in 6); is C worth a mesh path for the pane and the lance only?
2. A real reference pass: please provide canon stills of Star Magic (Conjunction circle, the Scutum rectangle, the Flagellum line, ch. 309 / 310 / 355) so the palette (gold-white / starlight blue) and the star shape can be matched.
3. The Hasta spear is weak in A (plank) and B (thin beam); decide whether it should be a SPEAR shape (C lance) or a LASER (B beam). The canon description says laser; the owner's brief says spear.
4. Photosensitivity: the A / B flashes and flares need the `reduced flashes` option from the Fire proposal.
5. Design-doc decisions (heir gate, consumed stars, Halation name, caps) are in `star_magic_design.md` section 7.

## 9. Rounds and what changed between them

Round 1: first full render. Found: flashes and rings far too big (conjunction, hasta, halation tiles filled by one flash), C over 1,000 vertices (constellation shimmer used the full flash), C whip glow strip drawn as one straight line, cameras cutting the whip and the star arc.
Round 2: flashes and rings scaled down, a cheap `spark` primitive for shimmers, C flash and sigil slimmed, caster-and-destination camera for Conjunction. Round 3: cameras re-aimed (Hasta from behind-left so the four stars are in frame, whip and Halation from higher), A / C whip widened, Hasta stars kept up to the launch. Round 4: B / C whip segments use the middle of the line texture (no dashed look), labels, close-ups, budgets.
Not fixed: the A spear is a flat plank, the C star solid is a plain bipyramid and its palette is pale, B lens flares can cover the figures, C Conjunction's ring dominates, the BEFORE rows for the node, lines and barrier are stand-ins.
