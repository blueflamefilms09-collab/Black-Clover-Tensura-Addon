# Fire VFX V2, built into the game (anime cel mixed with volumetric glow)

Owner decisions after the preview round (`fire_new.md`): "mix of A and B" per effect (A = anime cel, B = volumetric glow), "keep the cel lion head", "keep the sun-lion sigil on the zone",
Ignis Columna "try a mix" (pillar plus a defensive flame vortex round the caster in one effect), "redo Wild Bursting Flame, as close to the anime image as possible", and a reduced-flashes option.
No C (3D meshes). This is the in-game build. **Nothing here could be run in Minecraft**; what was checked is in section 8.

## 1. What is built, in plain words

| Effect (shape) | Style | What it looks like now |
|---|---|---|
| Leo Rugiens (`FIRE_LION`) | A cel | the **cel lion head kept** (open jaw, white-hot mouth) leaping from caster to target, a cel flame stream behind it, seven hooked flame tongues, a halo, embers; on arrival a flash star and speed lines |
| Sol Linea (`FIRE_SPEAR`) | A cel | a cel flame lance wrapped in hooked tongues, a round fireball head with a star, chips |
| Ignis Columna (`FIRE_PILLAR`) | A cel | the flame pillar (five layered cel flames, loose tongues, embers, a ring of fire) **plus a defensive flame vortex** round it: a spinning flame swirl on the ground and three strands of flame sheets winding up the pillar |
| Fire burst (`FIRE_BURST`) | A cel | fireball, radial speed lines, flame ring, ten tongues, embers, smoke |
| Spiral Flame (`FIRE_SPIRAL`) | B glow | three helices of soft heat puffs round a hot core streak, a glowing head, embers |
| Wild Bursting Flame (`FIRE_WILD`) | A cel, **redone** | a ground circle of flame (cel flame carpet with a rim of flame teeth, a ring and a wall of tongues) and, in three waves, round pale-yellow fireballs thrown in every direction, each with a long light beam back to the burst and a soft glow (see 2) |
| Flame cast (`FLAMEFX_CAST`) | B glow | **the old charge sigil kept** (sun-lion + ring at the caster), a glowing stream of puffs with embers, an arrival flare and ring |
| Flame zone (`FLAMEFX_ZONE`) | A cel | **the old sun-lion sigil, its ring and heat glow exactly as before** (same sprites, size, spin); the flames standing round them are new cel tongues (outer ring, small inner ring, rising chips) |
| Flame burst (`FLAMEFX_BURST`) | A cel | cel star, radial lines, flame ring, tongues, chips, smoke |
| Flame trail (`FLAME_TRAIL`) | B glow | a glowing stream (soft streak, puffs, hot core) with a bright head and embers; drawn with the engine's own head / tail / flicker curves |
| Flame blast (`FLAME_EXPLOSION`) | A cel | a cluster of six cel fireballs, ring, tongues, star, chips, smoke; engine curve `explosionBurst` |

Mapping of the mix (my own per-shape choice from `fire_new.md` section 6): **A for the hero shapes** (lion, spear, pillar, burst, wild, zone, fx burst, explosion), **B for the beam / spiral / trail style effects** (spiral, cast, trail).

Palette and tint: the effect colour decides. A fire colour (hue red to yellow: the default `FF8A2A`, GOLD, EMBER, ...) draws the baked textures with a white vertex colour. Any other colour (a page that reuses a
shape with a blue, green or black flame) draws the **grey twin** (`fnw_*_t.png`, luminance only, same silhouette) multiplied by that colour, so the tint keeps working (blue example in `fire_ingame_extras.png`).
Halos use the grey halo in the effect's colour (as the old bloom did); the smoke stays brown-grey. About 50 files in the mod spawn these shapes (bosses, Escanor / Meliodas moves, Flame and Fire books, weapon spells,
relic waves ...); they pass their own colour and power, nothing about their calls changed.

## 2. Wild Bursting Flame, compared with the anime still

Source: `Leopold_wild_bursting_flame` (owner pack, `docs/previews/vfx_remake/fire_new/fire_wild_closeup.png`, left tile). What the still shows and what was matched: a flat yellow fire floor with a wavy flame edge
(the `A_floor` carpet and its tooth rim), big round fireballs with a pale-yellow core and a thin orange rim (new `A_orb` frames, rim made thin in round 2), long thin yellow light beams crossing the picture (new `A_beam`
from the burst to each ball), a warm glow (additive halo under the floor and round each ball). Rounds: 1 balls clustered in the middle and the beams were hidden, the rim was thick; 2 beams from the burst to the ball,
farther reach, thinner rim, lower wall of tongues, camera moved to the side; 3 a glow on the floor and round every ball, wider beams. The weakness noted in `fire_new.md` (cluttered, unreadable) is fixed by fewer, larger balls
(5 per wave instead of a big cluster) and the beams that give each ball a direction. What is still different from the still: it is a clean cel look, the still is soft and blurry; the still has the whole scene tinted yellow
(an anime colour grade, not an effect).

## 3. Settings

CLIENT config `config/nusmp-fire-v2-client.toml` (own file name, registered only on a client; the server never reads it and nothing is sent):

* `vfx.element.fire = V2 | V1`, default **V2**. **V1 = exactly the old code for all eleven shapes**: `FireV2Layer` is registered after `FlameMagicLayer`, `FireSpellLayer` and `FlameFxLayer` (the manager keeps the
  last layer registered for a shape) and hands every call (render, default duration and colour, vertex budget, spawn shake) back to the old layer that owned the shape. Checked: the preview runs `FireV2Layer` with V1 and
  the old layers on the same scenes at every second tick and the quads are identical for all eleven shapes (`v1_equals_old_layer: true` in `fire_ingame_budget.json`).
* `vfx.element.fireReducedFlashes = false | true`, default **false**. With true: no hard flash stars and no radial speed lines (a soft halo fades instead, 0.5 at most and only falling), no pulsing light (the Wild glow is constant, the
  flame flicker is slowed to 40 percent), no spinning flash sprites, the flame flip-book holds a frame for 7 ticks instead of 2 (at most 2.9 changes a second, the rule "no more than three flashes a second"), and the Flame trail loses its
  flicker. The kept sigils of the zone and the cast are unchanged (they were never flashing). Pure rules: `FireV2.hardFlash / softFlash / pulse / spin / frame`. The option works per effect; it cannot limit how many fire
  effects many players cast at the same time (that is bounded by the existing 64-effect cap and the quality caps, not by this option).

## 4. NEW / REPLACED / KEPT (rule 5)

* **NEW:** `FireV2Layer` (painter), `FireV2` (pure rules), `FireV2Config` (client options), `FireV2SelfTests` (`magic.fire_v2`), 66 textures `fnw_*.png` (34 baked + 32 grey twins; five of the baked ones are new shapes made for this build:
  the round fireball `a_orb` (two frames), the light beam `a_beam`, the fire carpet `a_floor`, the vortex swirl `a_swirl`), the **vortex** of Ignis Columna, the redone Wild Bursting Flame, the reduced-flashes option, the preview tools and scenes,
  the tests `FireV2Test`, `FireV2AssetTest`, `FireV2SourceScanTest`, this doc.
* **REPLACED (look only, behind V1):** the look of `FIRE_LION`, `FIRE_SPEAR`, `FIRE_PILLAR`, `FIRE_BURST`, `FIRE_SPIRAL`, `FIRE_WILD`, `FLAMEFX_CAST`, `FLAMEFX_ZONE`, `FLAMEFX_BURST`, `FLAME_TRAIL`, `FLAME_EXPLOSION`. No old code is deleted or edited.
* **KEPT:** `FireSpellLayer`, `FlameFxLayer`, `FlameMagicLayer` (unedited, they draw V1), every old texture (`fire_*`, `flamefx_*`, `flame_trail`, the cel lion sprite `fire_lion`), the sun-lion sigil of the zone and the charge sigil of the cast
  (drawn from the old sprites with the old size and spin), the camera shake and sounds, `VfxShape` (no new enum entry), `DemonFireLayer` / Salamander armour / `FX_FIRE_ERUPTION` (never in this layer), the Time Magic VFX (not touched),
  the preview round files (`fire_new.md`, `fire_proposal.md`, the A / B / C sheets).

## 5. Cost: worst case (rules 9 and 11)

Server: **0 new packets, 0 server work, 0 per-player state.** Everything is client drawing of the existing effect packet. Peak quads of one activation over **every tick of its life**, measured by running the real
layers through the Java preview (`docs/previews/vfx_remake/fire_new/fire_ingame_budget.json`); detail 1.0 is full, detail 0.5 is the lowest level of detail (particles "Decreased" or the distance level of detail; every count goes through `ctx.seg`):

| Effect | old peak, full | old, 0.5 | **V2 peak, full** | **V2, 0.5** | vertex budget of one activation (old peak x 4) |
|---|---|---|---|---|---|
| Leo Rugiens | 66 | 41 | **22** | 15 | 264 |
| Sol Linea | 49 | 28 | **17** | 12 | 196 |
| Ignis Columna (+ vortex) | 55 | 34 | **33** | 21 | 220 |
| Fire burst | 61 | 36 | **29** | 17 | 244 |
| Spiral Flame | 67 | 39 | **49** | 28 | 268 |
| Wild Bursting Flame | 93 | 57 | **69** | 42 | 372 |
| Flame cast | 45 | 27 | **26** | 18 | 180 |
| Flame zone | 39 | 21 | **31** | 17 | 156 |
| Flame burst | 54 | 30 | **26** | 15 | 216 |
| Flame trail | 41 | 22 | **24** | 15 | 164 |
| Flame blast | 73 | 38 | **29** | 19 | 292 |

Every V2 effect is under the peak of the code it replaces (the Wild peak of 93 is the real old peak at every tick; the preview round had read 72 from sampled ticks); the vertex budget of an activation is that old peak, enforced by
`VfxVertexBuffer.hasBudget`, and the lion head is drawn before its decoration (the other effects are far under their budget, so a cut never happens in practice). The worst case per frame stays bounded by what was already bounded: at most 64 running effects
(`MAX_ACTIVE`, fewer on medium and low), the distance cull and level of detail of `VfxManager`, the frame vertex allowance of `VfxQuality`. Twenty casters of the largest effect in view: 20 x 69 = 1,380 quads
(the old Wild at 93 was 1,860). Per frame there is no allocation (every vector is a static scratch, the 66 `ResourceLocation`s are built once in a static initialiser, the sigil planes are laid out with floats instead of `VfxPose`
objects), no string, no reflection, no world scan, no chunk load, no config read (the options are volatile fields mirrored from the config on load / reload). `PerfProbe`: `VfxManager` already times every layer, so the layer reports as
`c.vfx.layer.FireV2Layer`. An effect is cleaned up like every other (it expires with its duration; `VfxManager.clear()` on logout); nothing is held per player.
Textures: 66 PNGs, 1.9 MB in total (the cel lion at 512 x 320 in two frames and two sets is the biggest part), sampled linear and mip-less like every other VFX texture, colour bled into the clear texels so no dark fringe shows.
Overdraw: the largest additive cards are the Spiral halo (6.2 x power blocks), the Flame cast head halo (4.6 x power), the Flame explosion halo and the Wild floor glow; the worst ones were not measured in a game.

## 6. Files

New: `vfx/FireV2.java`, `vfx/FireV2SelfTests.java`, `vfx/client/FireV2Config.java`, `vfx/client/layer/FireV2Layer.java`, `assets/nusmp/textures/particle/fnw_*.png` (66), `tools/gen_fire_v2_textures.py`,
`tools/vfx_preview/fire_ingame.py`, `tools/vfx_preview/scenes.d/fire_v2.json`, `docs/previews/vfx_remake/fire_new/fire_ingame.png`, `fire_wild_closeup.png`, `fire_ingame_extras.png`, `fire_ingame_budget.json`, this doc,
the tests `FireV2Test`, `FireV2AssetTest`, `FireV2SourceScanTest`.
Appended to (shared files): `vfx/client/VfxManager.java` (one registration), `NUSMP.java` (two lines), `tools/vfx_preview/preview.py` and `Preview.java` (copy `FireV2.java`, `--fire-style`, `--reduced`), `PerfSourceScanTest` (the new
layer is in the hot-file list). Not touched: `AbstractVfxLayer`, `VfxVertexBuffer`, the three old fire layers, version, changelog, README, lang files, `.github`.

## 7. Not seen in a game, honest caveats

* **Nothing here was run in Minecraft.** The previews are a headless software rasteriser (painter's-order sort, no depth buffer, no bloom, no world light, no depth against blocks, 70 degree preview camera). The real game draws
  with the vertex buffer's depth test and batches (alpha batches first, additive last, batches ordered by their first quad), so overlapping cards can sort differently from the previews.
* Motion (flip-book every 2 ticks, flicker, spin) is coded but was judged from stills, not watched.
* The cel flames are flat cards that always face the camera about the vertical axis; seen from straight above they thin out (as the old tongues did). The Ignis Columna vortex strips are camera-facing ribbons, not a real 3D cylinder.
* The Wild Bursting Flame balls fly out to 0.9 to 1.7 x the radius, so a ball that flies at the camera becomes big on screen (the camera of the close-up is on the side for that reason).
* The grey twin of a very dark colour (for example `1A1A1A`) multiplies to almost black; the cel bands then read as shades of that colour, not as red-orange-yellow.
* The Flame cast arrival uses the soft B rays and a ring (the old arrival ring was not kept; the charge sigil was).
* The CI smoke list (`SMOKE_EXPECT` in `.github/workflows/smoke.yml`) must get the prefix **`magic.fire_v2`** (the self-test is registered; the workflow file was not touched here).

## 8. What was checked

`FireV2Test` (pure rules), `FireV2AssetTest` (66 files, sizes, corners, bled colour, grey twins, silhouettes, no stray or dead file, old textures kept), `FireV2SourceScanTest` (server safety, client config, no allocation in the painter,
alpha / additive only, registration order, V1 hand-back, old layers unedited, kept sigils, reduced-flash gates), the in-game self-test `magic.fire_v2` (runs on the dedicated server in CI), the V1 equivalence through the preview,
the type-check of every new and edited main class against the API stubs, and the existing guards (ServerSafetyTest, ConfigFileNamesTest, PerfSourceScanTest, FxSourceScanTest, the particle / texture / additive / lang tests, and the
sweep of every test that reads the repo's files): see the report of the change for the results.

## 9. Decisions left for the owner

1. The Flame cast keeps its charge sigil (the owner named only the zone sigil): keep it, or give the cast the plain glow start?
2. Wild Bursting Flame is clean cel; if the still's soft yellow look is wanted, a yellow-wash halo layer (one more additive card) can be added.
3. Salamander's Claw / Breath, Shining Flame Cross, Flame Spike and the buckshot spells of the pack are still unbuilt (new shapes, not part of this change).
4. Vortex: the vortex sits where the pillar is (the pillar's `from`); for a cast on the caster that is the caster. Should it follow the caster for casts elsewhere? (needs a new shape or a page change, not done.)
