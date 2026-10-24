# Water VFX remake: three directions seen side by side (phase A, PREVIEW ONLY, nothing wired into the game)

Owner brief: remake WATER VFX, explicitly not Minecraft pixel style. Show A "anime cel", B "volumetric glass" (both from `water_proposal.md`) and a new C "real 3D toon meshes"
(real geometry with thickness, 3-tone toon shading, rim light, emissive core, like the real 3D weapons of the Wind Spirit Dive).
Nothing in `src/` or `assets/` was changed; no version, changelog, README, lang file or workflow was touched. Time Magic VFX untouched (rule 3).

**Honest limits.** Everything below is a headless software render (numpy, painter's-order sprites, a z-buffer for the meshes). No bloom, no world light, no terrain, no real blend modes
(the game's WATER blend is drawn as plain alpha here), no screen-space anything. Nothing was seen in a game. The textures are procedural (Pillow), not hand-drawn; the head and wing
silhouettes are my reading of the stills, not a trace. The toon shader of C is a CPU per-quad tone lookup (see 4); it is a plan that fits "no shaders", not something I ran in Minecraft.

## 1. Images

| File | What |
|---|---|
| `docs/previews/vfx_remake/water_new/water_compare.png` | per shape: BEFORE (real layer class) , A, B, C, same camera, same tick |
| `water_new_A.png`, `water_new_B.png`, `water_new_C.png` | one sheet per direction, 3 ticks per shape |
| `water_closeups.png` | roar head, burst crown, nest dome at 2x size, A / B / C |
| `water_reference_sheet_1.png`, `_2.png` | contact sheets of the 24 spell stills of the pack (the big tutorial / sheet images were looked at one by one, not sheeted) |
| `water_new_textures.png`, `tex/` | the 26 generated sprites of A and B (preview folder only) |
| `meshes/*.txt`, `meshes/counts.txt` | the C pieces in the repo's `q` mesh format + quad counts per piece and layer |
| `water_budget.json` | peak quads per effect over its whole life, hi and lo LOD |

Rebuild: `python tools/gen_water_new_textures.py`, `python tools/gen_water_new_meshes.py`, `python tools/vfx_preview/water_new_preview.py [shape ...]` (JDK 21 + numpy + Pillow).
The BEFORE column is produced by the repo's own Java preview from the real `WaterFxLayer` / `WaterMagicLayer` / `WaterSpellLayer`.

## 2. Pack source per effect (every still was looked at; all 26 files of `Water VFX Effects/`)

Source tags: [still] = an anime/manga still in the pack, [sheet] = the tutorial / sprite / concept sheets in the pack (Water Tutorial, the gif of 20 splash sprites, the Avatar-style
pose sheet, the dark lance/wave concept sheet). Wiki text was not re-fetched for this phase (the canon text is in `water_research.md`, English first, Japanese as fallback, same limits as listed there).

| Shape (what fires it) | Closest canon still | What was taken from it |
|---|---|---|
| **ROAR** WATERFX_CAST (roar, wrath, tide lance, waterball; old WATER_DRAGON unused) | [still] Roar_of_the_Sea_Dragon; second head: Tainted_Culvert_Wyrm_Rhya | open jaw with big fangs top and bottom, spiky ridge on the neck, body in pale/blue bands, glowing eye; the head is big, the body trails thinner |
| **LANCE** WATERFX_CAST (Aqua Javelin; old WATER_JAVELIN unused) | [still] Aqua_Javelin_Rhya; [sheet] the dark lance concept (ee700f44) | one long pointed spear, spiral water wisps winding round it, small barbs |
| **CRADLE** WATERFX_ZONE (old WATER_CRADLE unused) | [still] Cradle_of_the_Sea_Dragon; Sea_Dragon's_Waterball | one huge pale sphere, a white hot-spot at the upper left, a chain of small bubbles beside it, lighter blotches inside the outlined spheres of the Waterball |
| **NEST** WATERFX_ZONE (old WATER_NEST unused) | [still] Nest_of_the_Sea_Dragon (alt: Aqua_Curtain_Rhya) | a dark-blue dome with three large spiral eddies and a white foam rim |
| **BURST** WATERFX_BURST (old WATER_BURST unused) | [still] Sea_Dragon_God's_Tail, Surging_Waves; [sheet] splash gif (Water 03/04/12), Avatar-style crown and column | a crown of tall rounded water horns with a droplet at each tip, foam at the base, ripples |
| **SPLASH** WATER_SPLASH (Mist Step, blood and paint pages) | [sheet] splash gif crowns; Water Tutorial (lighter rim, droplets) | the small crown + droplets |
| **RING** WATER_RING (Mist Veil / Prison, basic arts, paint pages) | [still] the foam ring at the foot of the Nest; Sea_Dragon_God's_Vortex; gif Water 09 | wobbling concentric rings with foam beads on the outer one |
| **VALKYRIE** add-on (new, optional; the 3D armour set stays; old WATER_DRESS unused) | [still] Valkyrie_Dress, Saint_Valkyrie_Dress | translucent crystal wings, a halo, bubbles/drips round the body |

Not used (no shape in the mod for them): Sea God's Hammer, Sea God Shield, Qualle Operation, Climbing Coelacanth, Solid's water bird, Ludic Sanctuary, Asta/Aquenelle pages. They are the obvious
candidates for later spells; the meshes `sphere`, `horn`, `ripple` already cover a shield disc / hammer head with a little more work.

## 3. The three directions as built

* **A anime cel.** Silhouette masks filled with three hard bands (shade / body / light) from a blurred "pillow" height field, a crisp white highlight, a cyan outline, stripes on the dragon.
  Sprites on camera-facing strips / planes (`Out.strip2/plane/bb`). Reads exactly like the stills (Nest and Cradle are the best matches of all three directions). Flat: no volume.
* **B volumetric glass.** The same silhouettes as translucent glass: depth gradient, pale fresnel rim, two baked tileable caustic networks, white speculars, 3 stacked body sheets scrolling
  against each other, bubbles. Richest as "water", the weakest in contrast on pale backgrounds (not shown here: daylight, snow, water blocks).
* **C real 3D toon meshes.** Dragon head (upper skull + separate lower jaw hinged and opened, fangs, two horns, dorsal fins, glowing crest and eyes), body pieces laid along the sine path, tail fin,
  spear with leaf head and wisps, sphere, dome with 3 raised spiral ridges, curl horns for crown/crest, ridged ripple rings, feather blades, droplet gems. Authored with the `Mesh` class of
  `tools/gen_sdw3_models.py` (loft / quad vocabulary, `q` lines, layers 0 body / 1 glow / 2 foam), instanced and posed like the Spirit Dive pieces (basis from a direction + hint).
  Look = 3 tones by the quad normal against a light from the upper left on the viewer's side, one hard white specular, a pale rim where the quad is edge-on, emissive layer additive, a soft glow sprite
  in the maw. Translucent pieces (cradle shell) are sorted far to near.

## 4. How C would be drawn (plan, not run)

Per quad, once per frame on the CPU: n.L -> one of 3 vertex tints, n.H > threshold -> white, |n.V| < threshold -> rim tint; solid layers through the existing emissive type, the glow layer
through the additive type (exactly what `SpiritDiveWind3Draw` does). No shader, no texture beyond one solid atlas cell, no allocation per frame (static scratch). The renderer needs two-sided
quads for the flat blades (fangs, fins, wisps): the Wind pieces' render type must be checked for culling (open point).

## 5. Budgets per effect (quads; verts = 4 x quads)

Cap today = 100 quads (400 vertices) per activation (`MAX_VERTICES_PER_ACTIVATION`). "Today live" = what the current shape emits at its worst. Peaks are taken over every tick of the effect's life.
A and B are sprites in the existing buffer; C is mesh geometry (LOD0 near, LOD1 = the `_lo` pieces, fewer sides, segments, horns, orbs).

| Effect | Today live | A | B | C LOD0 (mesh + sprites) | C LOD1 | Cap 100 |
|---|---|---|---|---|---|---|
| ROAR | 51 | 20 | 42 | 250 (248 + 2) | 163 | A, B fit; C does not |
| LANCE | 51 | 21 | 15 | 153 | 78 | C LOD1 fits |
| CRADLE | 39 | 12 | 13 | 268 | 163 | C does not (sphere 80 + bands + bubbles) |
| NEST | 39 | 10 | 13 | 238 | 123 | C does not (dome 120) |
| BURST | 30 | 13 | 16 | 177 | 113 | C does not |
| SPLASH | 23 | 8 | 9 | 120 | 68 | C LOD1 fits |
| RING | 100 (at the cap) | 3 | 6 | 84 | 56 | all fit, A and B far under |
| VALKYRIE add-on | 74 (old shape, unused) | 10 | 12 | 205 | 125 | C does not |

* **A and B are cheaper than today everywhere** (the proposal allowed 34 / 35 quads for the cast, these are 20 / 42 because the scenes here are simpler). No new packets, no server work.
* **C is 2 to 5 times today's quads** at LOD0. It cannot go through the 400-vertex VfxVertexBuffer cap; it needs its own mesh path (like the Spirit Dive pieces) with its own bounds. Proposal:
  mesh effects at most 270 quads each at LOD0, switch to the `_lo` pieces beyond 16 blocks or when more than 3 mesh effects are in view, a global ceiling of 1200 mesh quads per frame (above it the
  effect falls back to its A sprites), skip beyond the existing distance cull. Worst case then: 1200 quads = 4800 vertices per frame, per-quad work is a handful of multiplies; 10 casters of the
  Roar all in view would be 10 x 163 (LOD1) = 1630 quads, which the ceiling cuts to 1200. A sprite fall-back means the worst case is never worse than A.
* Network: zero change in all three (same shapes, same params). Server: zero. Effects of far players: skipped by the existing cull. Time Magic untouched.
* Not measured: real GPU cost, real draw-call count, overdraw of the translucent shells. Everything above is counted from the generated geometry.

## 6. My recommendation (for you to overrule)

Honest reading of the renders: A is the closest to the stills and the cheapest; C is the only one that has real volume and the one that matches "like the 3D weapons you liked"; B is the most "water".
Both A and C are not smooth: C is faceted (low-poly sides to stay near the budget), and that is the price of the per-quad shading.

* **ROAR, LANCE, VALKYRIE wings: C** (the dragon head with a real jaw is the hero of the element and the thing the old effect lacks most). Use C LOD1 or the A sprite head at distance.
* **NEST, CRADLE: A** (the stills are flat pale discs / a dome with spirals; A is nearly a copy of them at 10 to 12 quads, C costs 120 to 270). B only if you want the zones to be see-through volumes.
* **BURST: A crown, optionally C horns on top for big power values** (the C horns read as spikes; the A crown reads as water).
* **SPLASH, RING: A** (cheapest, and they are also used by blood and paint pages, so the cel bands must be tinted from `inst.color`, the preview did not test that).
* B: keep as a variant for a "glass" setting or for the Valkyrie wings; do not use it as the only look (contrast).

One `vfx.element.water = V1 | A | B | C` client option would let all of this live next to V1 (rule 2).

## 7. Old effects that could stay untouched (V1)

* The six old `WaterSpellLayer` shapes (WATER_DRAGON .. WATER_DRESS) are not sent by any page: leave them registered and unchanged either way.
* WATER_SPLASH and WATER_RING (shared with blood / paint / Mist pages): keep V1 until a tint-safe V2 is shown on the blood page colours; they are the cheapest to postpone.
* WATERFX_BURST: today's ripples and crests are acceptable at small power; V2 could apply only above a power threshold.
* WATERFX_CAST / ZONE are the ones with the biggest gap (thin spark, ground sigil); these are the ones worth remaking first.

## 8. New vs replacement (rule 5)

REPLACEMENT (same effect, new look, V1 stays behind the option): ROAR, CAST (lance), CRADLE, NEST, BURST, SPLASH, RING. NEW: the Valkyrie add-on (wings / halo / drips beside the 3D armour; today only the unused
WATER_DRESS and the armour exist), and the lance as its own look of the CAST shape. KEPT: everything in `src/`, the six unused shapes, the Demon Water / Tide boss looks, all sounds, all gameplay numbers.

## 9. Open decisions for you

1. Which direction per effect (section 6), or one direction for all.
2. May C use its own mesh path with its own budget (270 quads per effect, 1200 per frame, A sprite fall-back), or must everything stay under the 400-vertex cap (then C only for LANCE, SPLASH, RING at LOD1)?
3. Faceted toon (this preview) or a rounder, more expensive C (more sides), or an outline hull (about +60 percent quads for the hero pieces, not in the numbers above)?
4. Is the dragon's long banded body wanted (Roar still) or only a head plus a short wake?
5. Bring Sea God's Hammer / Shield / Qualle / Coelacanth / the water bird in as new spells later (the pack has stills for them)?
6. Flash on impact (drawn in A and B burst as a 2-tick glow): wanted or not (light-sensitivity)?

## 10. Caveats seen in the renders (so you do not discover them later)

* A: the dragon body strip meets the head with a visible seam / width step (the head's neck edge is straight); the stripes on the head are busy.
* B: the glass body is faint at a distance; the head's back edge is also straight; over pale ground it will be weaker than shown on the grey checker floor.
* C: facets are visible, the dome's spiral ridges read less clearly than A's painted eddies, the cradle's bubble chain is boxy (5-sided bubbles), the Valkyrie feathers fan like a star from behind,
  the lance is seen mostly edge-on from the side camera.
* All: motion was judged from 3 frames per effect, not from a video; the wave of the dragon body, jaw opening and ring pulses were never seen moving.
