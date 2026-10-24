# Wind VFX, "Minecraft style" remake, phase A (PREVIEW ONLY)

The owner said "try to make these Minecraft styles" about the Wind reference pack. This is the answer for WIND: the shapes and colours of the pack redrawn as
Minecraft, in three versions, rendered next to the current look. **Nothing is wired into the game**: no Java class under `src/`, nothing under `assets/`, no
lang key, no config, no version / changelog change. Only a texture script, a preview script, preview images and this doc.

Honest limits: headless software rasteriser of the repo preview tool (`tools/vfx_preview`), painter's-order sort instead of a depth buffer, **no bloom, no
world light, no depth against blocks, not seen in a game**. The BEFORE column is the real current layer class run through the Java preview (same camera,
same tick as the other three).

## 1. Files

| File | What |
|---|---|
| `tools/gen_wind_mc_textures.py` | Pillow + numpy, deterministic. Writes the pixel textures **to the preview folder only** and checks the colour limits (exits non-zero if a style exceeds its palette) |
| `tools/vfx_preview/mc_wind_preview.py` | preview-only quad generators for 8 effects x 3 versions, renders sheets, compare, budget json (re-uses `preview.py` for the rasteriser and the real-layer BEFORE) |
| `docs/previews/vfx_remake/mc/wind_tex/*.png` (36) + `wind_mc_textures.png` | the textures (contact sheet at 8x) |
| `docs/previews/vfx_remake/mc/wind_mc_A.png`, `_B.png`, `_C.png` | one sheet per version: row per effect, 3 to 4 ticks per row |
| `docs/previews/vfx_remake/mc/wind_mc_compare.png` | **BEFORE \| A \| B \| C**, one tick per effect, same camera |
| `docs/previews/vfx_remake/mc/wind_mc_budget.json` | peak quads / cubes per version and effect (every tick of the effect's life, capped at 120 ticks) |
| `tools/vfx_preview/scenes.d/wind_baseline.json` | phase-1 scene file for WIND_SLASH / WIND_RING, copied unchanged from the phase-1 worktree (needed for the BEFORE column) |

Run: `python tools/gen_wind_mc_textures.py` then `python tools/vfx_preview/mc_wind_preview.py` (JDK 21, numpy, Pillow).

## 2. The three versions

All three: nearest-neighbour textures, hard alpha, **no soft gradients**, 16x16 or 32x32 (strips 32x16 / 32x8), limited palette per style, stepped fades
(alpha in 4 steps, never a smooth ramp).

| | A "vanilla-faithful" | B "voxel" | C "pixel-anime" |
|---|---|---|---|
| Looks like | something Mojang could ship: grey-white `poof` / `sweep_attack` / `gust` sprites, flat billboards, two leaf greens | the effect built from real cubes (white / mint / lime / green block faces, 1 px bevel, speckles), vanilla face shading (top 1.0, sides 0.8 / 0.6, bottom 0.5) | the pack's cel look: white / mint / lime / deep green with a dark shadow-green outline, hard cel bands |
| Palette | 6 colours: `F7F7F7 D5DDD3 A3AFA1 6B786A` + leaf `8ED45A 4F9A35` | 5: `F8FBF0 CCEEBE 89F842 54D234 46A92C` | 5: outline `37641E`, `46A92C 89F842 CCEEBE F8FBF0` (pack hexes from phase 1) |
| Geometry | flat quads, camera billboards, ground decals | axis boxes (yawed), only the camera-facing faces are emitted (1 to 3 quads per cube), positions snapped to 1/8 block, sizes in 1/8 steps, spiral staircase layout | flat quads, S-ribbons, stacked cel bands |
| Motion | smooth, vanilla-particle-like | rotation smooth, positions on the 1/8 grid, wings flap in 2 steps | **on twos** (state sampled every 2 ticks) + **2-frame flip-book** swapping every 3 ticks (`_0` / `_1` textures) + held smear ghosts on crescents |
| Pack look it chases | the fantasy "if vanilla had wind magic" | Minecraft block-display look | anime frames (Towering Tornado bands, Ark of Heavenly Wind), manga crescents |

Rule 6 / 7 (engravings, 3D weapon models) do not apply: these are spell effects, not weapons.

## 3. Per effect: pack source, what each version is, quads vs the current cap

Quads = what the real `VfxVertexBuffer` would get (vertices / 4). For B the worst case is `3 x cubes` (a cube never shows more than 3 faces to one camera),
and that bound is also below the cap in every row. "cap" = the current peak (phase 1) or, for the two new shapes, the 100-quad activation cap
(`MAX_VERTICES_PER_ACTIVATION` = 400).

| Effect (shape) | Pack image(s) it comes from | A quads | B quads (cubes) | C quads | current cap |
|---|---|---|---|---|---|
| Crescent Kamaitachi (`WIND_SLASH`) | `Black_Crescent_Kamaitachi`, `Yuno_crescent_wind_sickle` | 12 | 66 (22) | 8 | 72 |
| Slicing Wind Emperor (`WIND_EMPEROR`) | `Black_Crescent_Kamaitachi` (huge moon), `Yuno_ark_of_heavenly_wind`, `Gale_White_Bow` (white S ribbons) | 14 | 73 (25) | 18 | 80 |
| Towering Tornado (`WIND_TORNADO`) | `Yuno_Towering_Tornado` (stacked white bands), `images (1).jpg` tornado, `ae85e6bf` green funnel | 51 | 93 (31) | 30 | 96 |
| Gust Lane (`WIND_GALE`, same for `FX_WIND_GUST`) | `Yuno_White_Hawk`, `Gale_White_Bow` | 16 | 66 (22) | 13 | 66 |
| Spirit of Zephyr (`WIND_ZEPHYR`) | `images (2).jpg` Spirit of Euros (wing, crown of crosses), owner's Zephyr frame | 21 | 78 (27) | 17 | 92 |
| Tempest Ring (`WIND_RING`) | `ae85e6bf` (ground ring with rim ticks, rotating rings), `images.jpg` | 18 | 84 (28) | 13 | 100 |
| **new, optional** palm beam (`WIND_BEAM`, Spirit Storm) | `ae85e6bf` (straight beam + ring + sparks), `images (2).jpg` | 14 | 84 (28) | 15 | 100 |
| **new, optional** drill (`WIND_DRILL`, Tornado Fang) | `Tornado_Fang`, `Black_Tornado_Fang` (horizontal drill, spiral debris) | 33 | 69 (23) | 13 | 100 |

Not used as a source: `Black_Wind_Blade_Shower` (manga burst of black blades; no shape asks for it, a possible later `WIND_SHOWER`: radial fan of C-style
streaks, noted not built) and the rest of the 1950 px concept art (swirling teal tornado volumes cannot be done in 16 px pixel art honestly).

Per effect and version, in short:

- **SLASH.** A: one 32x32 crescent sprite (white rim, grey inner edge) + 5 puff trail + 4 streaks + 2 sparks at the tips. B: a 22-cube blade, 11 outer cubes tapering
  0.75 to 0.375 block along the arc + 5 inner + 6 lime trail cubes. C: crescent flip-book + two held ghosts behind it (smear) + 2 S-ribbons + tip sparks + a spawn flash.
- **EMPEROR.** Same crescent language, 3.5 p to 5 p radius: A with 3 crossing ribbons and 6 sparks; B with a 14 + 7 cube double arc plus a 4-cube "sword spine"; C with 3 ghosts, 3 ribbons, 6 rays, 6 sparks. It reads at range because few big shapes.
- **TORNADO.** A: "particle tornado" (7 rings x 6 cloud puffs, foot cloud, leaves; what a vanilla server would do). B: 6 layers of yawed slabs in a spiral staircase (4, 4, 5, 5, 6, 5) + 2 lime leaf cubes. C: 6 layers x 3 white cel bands wrapped on the funnel (the anime stacked-bands look) + puff foot + leaves.
- **GALE.** A: streaks and rolling ring decals + puffs + leaves (no creature). B: **voxel hawk** (body, head, 3-step wings that flap in 2 steps, tail) + 2 cube trains + a small cube ring. C: **pixel white hawk** (Whirlwind White Hawk) + 4 S-ribbons + streaks. The old "swallows look like aeroplanes" problem is the reason the hawk is a real bird silhouette; at this size it is still hard to read (see section 5).
- **ZEPHYR.** A: two crescent wings, halo ring + 8 spark crosses, ground ring, orbiting puffs and leaves. B: two 6-cube staircase wings with hanging feather cubes + 8-cube halo. C: two flip-book crescent wings, 8 twinkling crosses (the Spirit Dive "crown of connected crosses"), ground swirl, 3 orbiting ribbons.
- **RING.** A: two flat ring decals (ring texture) + 8 puffs + sparks + streaks. B: 14 + 10 slab cubes expanding as two rings, 4 rising lime cubes. C: broken-ring decals in 2 frames + ground swirl + 6 rising ribbons + sparks.
- **BEAM (new).** A: two crossed quads of a scrolling `beam` strip + palm puff + 5 swirl rings sliding down it + end sparks. B: 11 beam cubes + 4 spiral groups of lime cubes. C: crossed `beam` strips whose chevrons flow in 2 frames + palm starburst + swirl rings + sparks.
- **DRILL (new).** A: 5 rings of puffs on a cone, narrow tip ahead, streaks and dust. B: 5 rings of cubes (6, 5, 4, 3, 1) + spine, alternating spin. C: 6 cel bands spiralling round the cone (a white drill) + 3 long fang streaks + tip flash + sparks.

## 4. Budget (rules 9 and 11, if any version ships)

- Server: **0 new packets, 0 server work**; these are client-only looks (a CLIENT config `vfx.element.wind = V1 | MC_A | MC_B | MC_C`, V1 = today's code untouched, rule 2).
- Per activation, quads: A <= 51, B <= 93, C <= 30, always below the current cap of that shape (table above); every count would still go through `ctx.seg(full, min)` (LOD) and `buf.hasBudget(n)`.
- B cost note: a cube costs one dot product per face to cull and 1 to 3 quads; no per-frame allocation is needed (3 scratch vectors); far-player LOD: drop trails / leaves / halo cubes beyond 48 blocks, keep the silhouette cubes.
- Textures: 36 PNGs of 16x16 to 32x32 (about 40 KB for all); the 2-frame sets double only the small C sprites. No shader, no blur, no new blend mode.
- C needs no extra tick work: "on twos" is `floor(age / 2) * 2` on the same age value the layer already has.

## 5. Recommendation and honest comments

**Recommended: C for the spells that carry the anime identity (Slash, Emperor, Tornado, Zephyr, Drill, Beam), A for Ring and Gale, B only as an opt-in "block" style.**

- **C is the closest to the pack and still obviously pixel-art.** The tornado (stacked bands), the crescents and the spiral drill read like the anime/manga frames, and it is the cheapest (8 to 30 quads, 2-frame flip-book is how vanilla animates fire / poof as well).
- **A is the safest vanilla look** (nothing could look out of place next to vanilla particles), but it loses the identity: it is grey-white, so a Wind spell no longer says "green wind mage". Best for the low-key effects (ring, lane) where the user does not want a loud glow. A tinted-green A would be the alternative (vertex colour, not done to keep the palette honest).
- **B is the most "Minecraft" and the heaviest** (up to 93 quads) and the least like the pack: the pack's thin hard crescents cannot be built from 0.375 to 1.25 block cubes without looking like a chunky arc. It does work for the blocky funnel and the palm beam, and the staircase wings are a fun block-display look. Keep as a style option, not the default.
- **Weak spots seen:** (1) the pixel hawk is not readable at gale scale (it reads as a white blob, the baseline problem "aeroplane" is gone but a bird is not clearly there; a 2-tick bigger hawk or a front view would be needed); (2) B tornado is sparse at the bottom because the lower cubes are small; (3) A / C ring decals use a 32 px texture over a 10 block ring so the pixels are 0.35 block big, deliberate but loud near the camera; (4) the ring and Zephyr use flat decals that vanish edge-on; (5) all fades are 4-step alpha, which is correct for the style but flickers if the game frame rate is high and the flip-book swap is not synced to ticks.
- Rule 3: the Time Magic files were not touched. Rule 2: nothing was removed; V1 stays the default. The Spirit Dive armour / spirits, Air Magic (AIR_FX1..3) and sounds are out of scope as in phase 1.

## 6. NEW vs REPLACEMENT (rule 5)

- **Replacement of look** (behind the V1 switch, nothing deleted): WIND_SLASH, WIND_EMPEROR, WIND_TORNADO, WIND_GALE (+ FX_WIND_GUST, which has no layer today and would be claimed by the Gale remake), WIND_ZEPHYR, WIND_RING.
- **New** (appended at the end of `VfxShape` if the owner agrees): `WIND_BEAM` (Spirit Storm palm beam, canon) and `WIND_DRILL` (Tornado Fang, canon). They have no BEFORE; the compare sheet shows the current Spirit Storm tornado for reference.
- **KEPT:** every old texture (`wind_*.png`), the old layers, `tools/gen_element_vfx_textures.py`, the phase-1 docs.

## 7. Open decisions for the owner

1. A, B or C (or per shape as recommended)? Is B wanted at all as a selectable style?
2. Keep the grey-white vanilla palette in A, or tint it wind green (more identity, a 7th colour)?
3. Add the two new shapes (`WIND_BEAM`, `WIND_DRILL`) or keep Spirit Storm as a tornado?
4. Drop the swallows for good (hawk or no creature)?
5. "On twos" + 2-frame flip-book as the default for C, or smooth only?
6. Which of A / B / C should the Fire and Water remakes follow, so the three elements share one style?

Sources: the Wind reference pack only (images read directly); canon names and shapes from the phase-1 research (`wind_research.md`, English wiki summaries first, Japanese
sites as fallback; the fandom page bodies could not be fetched there either). No new web research was done in this phase.
