# Fire VFX remake - proposal (three art directions, nothing implemented)

Read `fire_research.md` first (sources, what could not be fetched, the 11 current effects and their gaps). Images: `docs/previews/vfx_remake/fire_baseline.png` (before),
`docs/previews/vfx_remake/fire_reference_board.png` (owner art crops). This file only describes; the owner picks one direction (or a mix per effect) before any code is written.

## 1. What is shared by all three directions

**Replace/keep rule (CLAUDE.md rule 2 and 5).** Nothing is deleted. The remake is a NEW render path for the SAME `VfxShape` ids, so no packet, no enum entry, no server code changes
(rule 9). A client option picks the look:

`vfx.element.fire = V2 | V1` (CLIENT config, looks only; default V2 once approved, V1 = today's code, kept as is). During review the owner can also test `V2A | V2B | V2C`.
Mechanism: `FireSpellLayer`, `FlameFxLayer` and `FlameMagicLayer` keep their code and, at the top of `render()`, hand the instance to a new `FireV2Layer` (new file, one class per direction
behind it) when the option is not V1. The same switch is planned for Water, Wind and Earth (`vfx.element.water|wind|earth`), so the option is one family.

**Classification: NEW vs REPLACEMENT.** This is a REPLACEMENT of the look of all 11 shapes (the owner asked to remake them); per rule 2 the old look stays selectable as V1. New files only:
`FireV2Layer.java` (+ per-direction helper), `tools/gen_fire_v2_textures.py`, `textures/particle/firev2_*.png`, `docs/attributes/fire_remake.md`, a self-test, preview scenes.
Time Magic files untouched (rule 3).

**Technical constraint that drives everything (from research section 4).** All current flame textures are white alpha shapes tinted and drawn ADDITIVE. The remake needs
(a) textures with BAKED multi-tone colour, (b) drawn with `VfxBlend.ALPHA` for the flame body (so a dark red edge and shadow tone can exist), ADD only for glow/embers,
(c) flipbook frames (anime fire is redrawn every 2-3 frames), (d) no shaders (the engine has none for these layers). The engine order is ALPHA first, then ADD on top, which is what we want.

**Palette taken from the owner's Fuegoleon art** (sampled by eye, `[OWNER-ART]`): gold `#FFC21A`, orange `#FF7A12`, red-orange `#E83A10`, deep red `#9E1B0C`, ember yellow `#FFD34D`,
hot white `#FFF6D6`, heat haze `#FF8A2A` at low alpha, smoke `#3A2A24`.

**Shared timing (anime "on twos / threes").** Flipbook frame = `floor(age_ticks / 2)` for body flames (10 fps, 20 TPS game), `floor(age/3)` for big shapes; flicker offset per sprite
from `hash(seed, i)` so neighbours never sync. Motion curves from the existing `VfxAnim` (`easeOutCubic` for launch, `easeOutBack` for a pop, `easeInCubic` for fade).

**Budget rule (rule 11).** Every V2 effect <= its current maximum vertices (table in section 3). Engine cap 400 vertices/activation stays. All counts go through `ctx.seg(full, min)` (distance/detail LOD),
embers through the existing perf budget, far players skipped by the existing culling. No per-frame allocation (reuse `Vector3f` temporaries; the current layers allocate, V2 should not).
Worst case at 20 simultaneous casters in view: 20 x 292 = 5,840 vertices = 1,460 quads, same as today. No packets added: 0 extra packets per second.

**Textures to generate (Python + Pillow, numpy) in `tools/gen_fire_v2_textures.py`** (white-alpha helpers from `tools/gen_flame_fx_textures.py` and `water_fx_lib.Canvas` can be reused, fixed seeds so output is reproducible):

| File | Size | Used by | Notes |
|---|---|---|---|
| `firev2_tongue_a.png` ... `_c.png` | 128x256 x3 | A, C | three cel flame silhouettes (flipbook), baked tones |
| `firev2_ball_a.png` ... `_c.png` | 128x128 x3 | A | cel fireball, 3 frames |
| `firev2_body_a.png` ... `_c.png` | 256x128 x3 | A | horizontal flame stream (tail left, tip right) |
| `firev2_soft_*.png` | 128x128 x4 | B | soft layered flame puffs with noise-eroded edge (4 variants) |
| `firev2_halo.png` | 256x256 | B | radial glow, gold core to red rim, long falloff |
| `firev2_swoosh_a.png` ... `_c.png` | 256x64 x3 | C | brush-stroke swoosh with dry-brush edge |
| `firev2_ink.png` | 128x128 | C | black-red ink splat for impact frames |
| `firev2_spark.png`, `firev2_ember.png` | 32x32 | all | 4-point spark, round ember |
| `firev2_smoke.png` | 128x128 | all | dark puff for alpha blending |
| `firev2_vortex.png` | 256x256 | A, B | spiral band, used for Spiral Flame / Ignis Columna |
| `firev2_ring.png` | 256x256 | all | ground ring of fire with teeth (replaces thin ring) |

About 25 small PNGs, under 400 KB total. All new, none replaces an existing texture.

## 2. The three directions

### Direction A - "Anime cel" (hard-edged cel-shaded flame, bright core, dark-red outline, smear frames)

**Look.** What the owner's Fuegoleon still shows: flat shapes in 3-4 solid tones, curled sideways tongue tips, a thin dark outline on the shadow side, a bright core as a separate flat shape (not a glow),
small hard ember chips. Big spells get one or two "smear frames" (a single stretched, motion-blurred shape for 1-2 ticks) as the anime does for fast moves.

**Colours.** Outline/shadow `#5A0F08` > shade `#B3200F` > body `#F2561A` > light `#FF9A22` > core `#FFD84A` > hot spot `#FFF6D6` (hard-edged, 5 flat bands, no gradients in the texture).
Embers: chips `#FFD34D` and `#E83A10`, 3-6 px, square-ish.

**Shapes / textures.** `firev2_tongue_*`, `firev2_ball_*`, `firev2_body_*`, `firev2_vortex`, `firev2_ring`, `firev2_spark`, `firev2_smoke`. All drawn ALPHA (the dark outline needs it), plus one ADD glow quad
behind the head of big spells so the bloom still exists.

**Motion curves.** Body flames: flipbook every 2 ticks, scale pop `easeOutBack` over 4 ticks, then constant, shrink with `easeInCubic` in the last 25 percent. Smear: frames 1-2 stretched x2.5
along the velocity then snap to normal (hold 1 tick). Embers: hand-keyed arcs `y = v*t - 2.2*t^2`, life 10-14 ticks, shrink to 0, no fade (cel particles pop out).

**Particle counts and cost.** Quads (4 vertices each): body 20-40 per effect, embers 8-14. Everything <= table in section 3. No glow cards except one per effect.

**Risks.** Alpha-blended sprites can pop or sort badly against water/glass (ALPHA after opaque but before ADD: acceptable); flat style may look cheap if the textures are not hand-polished (needs preview iteration);
looks very different from the other element effects if those go soft (should be agreed across the four elements).

### Direction B - "Volumetric glow" (soft layered additive sprites, heat shimmer, embers, bloom-like halos)

**Look.** The grimoire covers and the ground-fire bloom in the owner art: saturated orange body, yellow-white centre, red rim, soft outer halo. Fire reads as a luminous volume built from 4-6 overlapping noisy puffs,
with big bloom halos, drifting embers and a faint dark smoke behind. No outlines.

**Colours.** Core `#FFF1B8`, inner `#FFC21A`, body `#FF7A12`, rim `#E83A10`, halo `#FF6A1E` at alpha 0.30-0.45, deep haze `#8E1B0C` at alpha 0.25 (alpha-blended, behind), smoke `#3A2A24` alpha 0.35,
embers `#FFB347`. Colour is baked into the texture as a radial ramp so overlapping layers do not saturate to white (use alpha 0.5-0.7 per layer, order red layer first, hot core last).

**Shapes / textures.** `firev2_soft_*` (4 noisy puff variants, scrolled UV drift of 0.02/tick for "boiling"), `firev2_halo`, `firev2_vortex`, `firev2_ring`, `firev2_ember`, `firev2_smoke`.
Heat shimmer without shaders: 2-3 large, very faint (alpha 0.06) warm ADD quads that rise and rotate slowly above the effect (a cheap "heat veil"), not real distortion.

**Motion curves.** Layers swell with `easeOutCubic` over 6-8 ticks and breathe with `1 + 0.08*sin(age*0.35 + phase)`; the halo pulses at 1.5 Hz while the effect lives; embers rise with
`vy = 0.06 + 0.03*hash` blocks/tick and sideways noise; smoke expands 0.8 to 1.6 blocks over its life.

**Particle counts and cost.** Quads: soft puffs 12-24, halo 1-3, embers 8-14, veil 2-3, smoke 2-4. Stay <= the section 3 maximum.

**Risks.** Additive stacking can still wash out against a bright sky (mitigated by baked red rim and alpha caps); the most overdraw of the three (large soft quads, fill-rate on weak GPUs), so the
`ctx.seg` LOD must drop puff counts at range; "heat veil" is a fake and could look like a dirty window at a bad camera angle; softer look may lose the anime feel.

### Direction C - "Ink and ember" (hybrid: stylised swooshes and sparks like the anime's impact frames)

**Look.** Fire drawn as bold brush strokes: S-curved swooshes that follow the motion, a black-red ink splat under every impact for 2-3 ticks (manga impact frame), white-hot flash for 1 tick,
then a handful of hard sparks. Fewer, bigger, more graphic shapes than A; the manga panels in the owner's VFX zip (Dark_Cloaked slash, Iron God of War) are the stylistic cousin.

**Colours.** Ink `#14070A` (alpha-blended), hot red `#D61F1F`, vermilion `#FF4A1C`, gold sparks `#FFD34D`, paper-white flash `#FFF4DC`. Two-colour rule per effect: ink + one of red / vermilion, gold only for sparks.

**Shapes / textures.** `firev2_swoosh_*` (3 dry-brush strokes), `firev2_ink`, `firev2_spark`, `firev2_ring` (thin, hard), `firev2_tongue_*` (reused from A, only as a few accents), `firev2_smoke`.

**Motion curves.** Swoosh grows along its path in 3 ticks (UV scroll from tail), holds 4-6 ticks, then thins to a point with `easeInCubic`; ink splat appears on tick 0 at scale 1.2 and shrinks to 0.8
by tick 3 then fades over 4; white flash exactly 1 tick; sparks fly in straight lines (no gravity) for 6 ticks then drop. Speed lines: 6-10 thin ADD quads radiating from the impact for 3 ticks.

**Particle counts and cost.** Quads: swooshes 6-14, ink 1-2, sparks 10-20, speed lines 6-10. Lowest vertex count of the three.

**Risks.** Black ink shapes can look wrong in bright daytime or against dark caves (readability), and may clash with the existing Dark Magic ink look (`Dark_Cloaked_*`) so fire could read as dark magic;
short, high-contrast effects flicker (photosensitivity: cap flashes at 1 per 5 ticks, add a client option `vfx.flashes = reduced`); needs per-effect art, less reuse than A or B.

## 3. The 11 effects under each direction (three ideas each) and the budget

Current maximum vertices (from the baseline sheet): LION 264, SPEAR 196, PILLAR 220, BURST 244, SPIRAL 268, WILD 288, CAST 180, ZONE 156, FX_BURST 216, TRAIL 164, EXPLOSION 292.
Targets below are quad counts (x4 = vertices) and are all at or below those maxima.

| # | Effect (shape) | Canon target | A cel | B volumetric glow | C ink and ember |
|---|---|---|---|---|---|
| 1 | Leo Rugiens (`FIRE_LION`) | giant lion of flame that roars fire (JA 大火炎獅子の咆哮) | Side-view lion silhouette made of 5 cel flame bodies (head, mane tongues, torso, hind, tail) following a leap arc; jaw opens on arrival with a hot-white mouth flash and 2 smear frames. <= 60 quads | Lion built of 18 soft puffs shaped by a mane/body layout, halo behind the head, roars as a big forward bloom; trail of embers. <= 62 quads | One black-red ink lion head silhouette snapped on for 3 ticks at the target plus 3 swoosh strokes forming the body path and an ink splat; embers burst. <= 40 quads |
| 2 | Sol Linea (`FIRE_SPEAR`) | thin concentrated beam from a small fireball, ignites what it pierces (JA ソル・リネア) | Small cel fireball at the hand (1 frame of 3) and a straight 2-tone beam body (outline + core) with a hot-white tip; no corkscrew. <= 40 quads | Straight beam from 6 stretched halo quads along the line + soft ball at the origin, heat veil above; embers where it passes. <= 44 quads | One long swoosh that draws in 3 ticks as a ruled line, a gold spark burst and speed lines at the hit. <= 24 quads |
| 3 | Ignis Columna / Calderos (`FIRE_PILLAR`) | defensive flame vortex around the caster (JA 螺旋火炎); Calderos not found | Two-layer cel vortex band (`vortex` texture, rotating) round a hollow core with tongues on top; kept as a rising pillar when power >= 1.5. <= 52 quads | Tall column of 14 soft puffs with a spinning vortex veil and a ground halo; embers spiralling up. <= 54 quads | 3 spiral swooshes wrapping a column, an ink ring on the ground flashing at start, sparks rising. <= 36 quads |
| 4 | Flame explosion (`FIRE_BURST`) | generic fire blast / Exploding Flame | Cel fireball (3-frame pop) with a flat shock ring, 8 tongues thrown out, scorch ring and 3 cel smoke puffs. <= 56 quads | Hot core + 10 puffs expanding, halo flash, 4 smoke layers, ember rain. <= 58 quads | One-tick white flash, black-red ink splat, 8 speed lines, gold sparks, a thin ring. <= 30 quads |
| 5 | Spiral Flame (`FIRE_SPIRAL`) | two (or one big) twisters of fire from the palms; Profound = bigger, long range | A cel twister: a stack of 6 stretched, rotating vortex segments widening to the head, tongues peeling off, tip fireball. <= 62 quads | Twister of 20 puffs along a helix with a glowing hollow centre, halo on the head, embers flung off. <= 64 quads | 4 interlocking swooshes spiralling along the path (drawn in over 3 ticks), spark spray, ink splat at the end. <= 36 quads |
| 6 | Wild Bursting Flame (`FIRE_WILD`) | ground ring of flame, then fireballs launched in all directions | Cel ground ring of fire opens on tick 0 and 8 cel fireballs (3-frame spin) fly out in a ring, 3 waves, leaving small ground flames. <= 70 quads | Big halo on the ground, 8 glowing soft fireballs with trails flying out, ember rain; shock ring. <= 68 quads | Ink ring splat on the ground, 8 swoosh-trails each ending in a spark star, one flash per wave (3 only). <= 44 quads |
| 7 | Flame cast stream (`FLAMEFX_CAST`) | Leo Rugiens / Spiral / Sol Linea / Crimson Lion King (shared) | Cel flame stream: `body` flipbook strip from caster to head, fireball head with outline, a few hooked tongues behind. <= 40 quads | A moving column of puffs with a head halo, thin heat veil, embers; sigil plane kept optional behind (`vfx.element.fire` ignores it). <= 42 quads | A single bold swoosh with a dry-brush tail and spark head, ink splat on arrival. <= 22 quads |
| 8 | Flame zone (`FLAMEFX_ZONE`) | field of fire / sun-lion circle (mod extension) | Ground disc of 12 cel flame tongues standing, burning ring, optional sun-lion sigil kept as a soft glow; tongues change every 2 ticks. <= 38 quads | Glowing disc: halo, 14 puffs at the rim, veil, ember columns; the sigil stays as in V1 under it. <= 40 quads | Ink circle drawn on the ground (brush ring), 8 vertical swooshes rising, sparks. <= 26 quads |
| 9 | Flame burst (`FLAMEFX_BURST`) | impact | Cel fireball pop with white-hot centre, 10 hooked tongues, ring. <= 52 quads | Heat flash halo (orange not white), 12 puffs expanding, smoke, embers. <= 54 quads | Impact frame: ink splat, 12 radial speed lines, 1-tick flash, sparks. <= 28 quads |
| 10 | Flame trail (`FLAME_TRAIL`) | stream of the Explosion/Magma books | Thin cel `body` strip with hooked tongues, fireball head. <= 38 quads | Chain of 10 soft puffs with halo head and ember trail. <= 40 quads | One swoosh stroke plus sparks. <= 18 quads |
| 11 | Flame explosion (`FLAME_EXPLOSION`) | fireball burst of Explosion/Magma books | Cel 3-frame fireball (core, ring, tongues), debris sparks, cel smoke. <= 70 quads | Layered fireball of 16 puffs, halo, smoke column, embers, scorch decal. <= 72 quads | Ink splat + white flash + 10 speed lines + spark star + ring. <= 36 quads |

Per-instance cap (400 vertices) and `ctx.seg` LOD apply to every row; the numbers above are the full-detail ceilings, the far-LOD counts halve them.

## 4. Replacement mapping (existing shape -> V2 path)

| Existing shape (all stay registered) | Layer that keeps it | V2 path when `vfx.element.fire != V1` | V1 |
|---|---|---|---|
| FIRE_LION, FIRE_SPEAR, FIRE_PILLAR, FIRE_BURST, FIRE_SPIRAL, FIRE_WILD | `FireSpellLayer` | `FireV2Layer.render(shape...)` | unchanged code, runs when option is V1 |
| FLAMEFX_CAST / ZONE / BURST | `FlameFxLayer` | same | same |
| FLAME_TRAIL / FLAME_EXPLOSION | `FlameMagicLayer` | same | same |
| `FireSpellLayer.burst()` (package-visible static) | no caller outside `FireSpellLayer` was found by grep | stays as the V1 burst | - |

Unchanged: all server code (`FlameArts`, `FireBook`, `WikiSpells`, `BasicArts`, `CharacterArtsBulls`, `TrueMagic`), packets, damage numbers, durations (`defaultDuration`), colours passed from the server
(`inst.color` still tints B's halo and A's glow so special fire colours, e.g. blue flame, keep working), shake calls. Old shapes: `FLAME_*`, `FIRE_*` remain in `VfxShape`.
Also not touched: `DemonFireLayer` (Demon Fire, its own textures) and the Salamander armour / `FlameAura`.

## 5. Verification plan (for phase 2)

* Preview scenes: reuse the 11 scenes with a `"look": "V2A"` key so the same camera and frames give an exact before/after sheet.
* New pure tests: texture generator determinism, vertex count per effect <= current maximum (a test reading the preview budget report), `PerfSourceScanTest` pattern for "no `new Vector3f` per frame in `FireV2Layer`" (if the owner wants zero allocation).
* Self-test prefix `vfx.fire_v2` for the smoke list; `ServerSafetyTest` unaffected (client classes only in `client/`).
* Honest caveat to repeat in the release: nothing can be seen in the real game from this environment, previews use a simplified rasteriser (no depth against blocks, no lighting).

## 6. Risk list (all directions)

1. Without the anime frames I could not fetch, the look is derived from the owner's stills and wiki text; the owner should check the reference board before approval (biggest risk).
2. Canon uncertainty: "Calderos" and "Ignis Columna as a pillar" are not in any wiki result; JA says Ignis Columna is a defensive vortex. Decide whether the page keeps the mod's pillar meaning.
3. Alpha-blended bodies (A, C) need correct draw order versus ADD glows; layer sorting is per batch (texture + blend) in `VfxVertexBuffer`, so a V2 flame may draw over transparent blocks incorrectly (water, glass).
4. Texture memory: ~25 small PNGs, far below the 512 kept-texture cap in `VfxVertexBuffer`, but flipbooks are separate files (no atlas) so batches multiply: 3 flipbook textures = 3 batches per effect.
5. Consistency: Water, Wind and Earth are being studied in parallel; picking different styles per element (A for fire, B for water ...) can be a feature or a problem, decide jointly.
6. Photosensitivity (C especially): flash frequency cap and a `reduced flashes` client option are required before shipping.
7. Overdraw on weak GPUs (B): LOD must drop puff counts at range; verify with `PerfProbe` in game.
8. The preview tool cannot prove depth, lighting or bloom of the real game; the owner must judge in the game build.

## 7. Decisions the owner needs to make

1. Which direction (A, B, C) or a mix per effect (for example B for zones and explosions, A for streams, C for impact bursts).
2. Keep the sun-lion sigil on the ground zone and the lion face in Leo Rugiens, or go fully canon (lion shaped flame, no sigil).
3. Whether Ignis Columna stays a pillar (mod meaning) or becomes the defensive vortex (JA meaning).
4. Whether one `vfx.element.*` option family for all four elements is wanted.
