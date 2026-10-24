# Water VFX, the hybrid "BC" (3D toon cores wrapped in glass), built into the game

Owner decision after the previews (`water_new.md`, `water_bc.md`): "i love the new water vfx" = the hybrid BC. This is the in-game build of seven of the eight shapes. **The Valkyrie wings are deferred**
(the owner has them remade separately as a 3D model of the anime Valkyrie Armor): no registration, no asset, no test, no scene for them; the old Valkyrie armour and the old `WATER_DRESS` effect stay exactly
as they are. Nothing could be run in a game here; what was checked is listed in section 8.

## 1. What is built, in plain words

| Look | Fired by (shape) | What it is now |
|---|---|---|
| **Roar** | `WATERFX_CAST` (Sea Dragon's Roar, Wrath, Torrent, and every cast that flies slower than 1.5 blocks a tick) | a dragon with a real head (upper skull, a separate jaw that opens, fangs, horns, fins), a body of 4 pieces (3 at LOD1) along the wave path and a tail, wrapped in glass: sheen shells in front of and behind the body, two flow ribbons winding round it, mist over the neck, bubbles, drops, a glass lens over the skull, glints; on landing a crown of water tongues, a glass crown and a ripple ring |
| **Lance** | `WATERFX_CAST` flying fast (Aqua Javelin, Valkyrie Tide Lance: 2.2 to 2.6 blocks a tick) | a spear with a leaf head, barbs and tail fins, a wide glass shell behind it, a sheen over the shaft, two helical flow ribbons, mist, bubbles, a tip glint; a small crown where it lands |
| **Cradle** | `WATERFX_ZONE` that follows its caster (Sea Dragon's Cradle) | a glass sphere (fresnel hull) with three whirling bands, a rim shell, caustic sheets, bubbles inside, the bubble chain beside it, mist |
| **Nest** | `WATERFX_ZONE` placed on the ground (Nest, Scale Mail, the Vortex page) | a translucent dome with three wide spiral foam bands, a foam collar, glass sheets and three swirl sprites on the camera side, beads of foam round the rim |
| **Burst** | `WATERFX_BURST` | a crown of rounded water tongues with a droplet bubble at each tip, a glass crown between them, two ripple rings, flying drops and bubbles |
| **Splash** | `WATER_SPLASH` (Mist Step, blood and paint pages) | the burst, smaller |
| **Ring** | `WATER_RING` (Mist Veil / Prison, basic arts, paint pages) | ridged glass rings with a ripple sprite under each, bubbles |

The seven looks come from four payload facts only (nothing new is sent): the shape, the distance from to, the duration, whether the effect follows an entity (`WaterBc.kindOf`). A cast is the **Lance**
when `length / duration >= 1.5` blocks a tick (the javelin model flies 26 blocks in 12 ticks, 33 in 16; the dragon at most 0.9, the fan of three 0.92, the Torrent 0.56), else the **Roar**. A zone is the
**Cradle** when it follows its caster (the Cradle is sent that way) else the **Nest**.

**Tint (Splash and Ring are reused by blood and paint pages).** The page passes its colour. A water blue (hue 165 to 265 degrees, e.g. `4FA8FF`, `3A7BD5`) draws the native textures and the water palette. Any
other colour (Hemorrhage, Crimson Rain, paint pages ...) draws the **grey twin** of the same sprite (`wbc_<name>_t.png`, 8 of them: exactly the sprites those two shapes use) multiplied by that colour and
turns the toon palette of the cores to that colour (red example in the sheet, last two rows). The five other looks are only fired by water pages and always use the native look.

## 2. How a frame is put together

1. `VfxManager.render` plans as before (distance, level of detail, vertex allowance), then calls `WaterBcFrame.plan`: every water effect of the hybrid is admitted **nearest first** against the frame
   ceilings and gets a tier: **LOD0** (full cores, only inside 16 blocks past the effect's reach, only for the 3 nearest, only while the mesh total stays under 70 percent of the ceiling), **LOD1** (the `_lo`
   cores: fewer sides, segments, horns, orbs, bubbles; beyond 16 blocks, from the 4th effect on, or on "Decreased" particles), **SPRITE** (the flat anime "A" sprites, no mesh at all, 3 to 22 quads) when the mesh
   ceiling is spent, or nothing when even those do not fit.
2. Each effect's layer call builds its geometry with the pure `WaterBcScene`: the 3D cores go to `WaterBcMeshBuffer.FRAME` through `WaterBcToon` (placed by a basis from a direction and a hint, shaded **per
   quad on the CPU**: three tones by the light from the upper left, one hard white specular, a pale rim at the grazing edge, two sided lighting, glass hulls with a fresnel alpha, emissive quads additive),
   the glass sprites go to the existing `VfxVertexBuffer`.
3. `WaterBcDraw.flush` (called just before the sprite buffer's flush) draws the cores in **three passes**: solid quads with the depth buffer written, translucent hulls blended without depth write, glow additive.
4. The sprite buffer then draws the glass depth-TESTED (never written), so a ribbon behind the body is hidden by it and a shell in front covers it: the core is seen through glass.

**Fixed draw order, no sort, no allocation.** The glass is drawn in the order the scene makes it: back to front by authored layer (shells behind the core first, then ribbons, mist, bubbles, drops, the lens and
the glints last). The vertex buffer keeps the first-use order of its (texture, blend) batches and draws ALPHA before ADD, so nothing is sorted per frame. The translucent hulls are likewise in the order
the scene made them. **Blades keep their back-face quads** (the files are two-sided; the toon light is two-sided too); the mesh pass also sets culling off, so nothing depends on the winding or on a render type
(an asset test checks that the winding of every quad agrees with its stored normal).

## 3. Setting

CLIENT config `config/nusmp-water-bc-client.toml`, key `vfx.element.water = BC | V1`, default **BC**. **V1 = exactly the old code for all shapes**: `WaterBcLayer` is registered after `WaterFxLayer`,
`WaterMagicLayer` and `WaterSpellLayer` (the manager keeps the last layer registered for a shape) and hands every call (render, default duration and colour, budget, spawn with its screen shake) back to the old
layer that owned the shape. Checked: the preview runs `WaterBcLayer` with V1 and the old layers on the same scenes and the quads are identical for all seven (`water_ingame_budget.json`, `V1_equals_old_layer`).
The setting only changes what this client draws; the server never reads or registers it.

## 4. NEW / REPLACED / KEPT (rule 5)

- **NEW:** `WaterBc` (rules), `WaterBcScene` (geometry), `WaterBcToon` (toon shading), `WaterBcMeshes` (loader), `WaterBcMeshBuffer`, `WaterBcBudget`, `WaterBcSelfTests` (`magic.water_bc`), the client
  `WaterBcLayer`, `WaterBcFrame` (plan), `WaterBcDraw` (3 passes), `WaterBcConfig`; the first mesh path of the VFX layers; 22 mesh files `assets/nusmp/wbc_meshes/bc_*.txt` (11 pieces x full and `_lo`);
  34 textures `wbc_*.png` (25 sprites, 8 grey twins, 1 solid cell); the Lance as its own look (it was the same thin spark as the Roar); the flat-sprite fall-back tier; the tests, the preview tool.
- **REPLACED (look only, behind V1):** Roar, Lance (both `WATERFX_CAST`), Cradle, Nest (`WATERFX_ZONE`), Burst, Splash, Ring. The old code is not deleted or edited.
- **KEPT:** `WaterFxLayer`, `WaterMagicLayer`, `WaterSpellLayer` (all six old shapes WATER_DRAGON .. WATER_DRESS, the Valkyrie Dress among them), every old texture, `VfxShape` (no new entry), the Valkyrie
  armour and its 3D model, all sounds and screen shakes, all gameplay numbers, the Time Magic VFX (not touched), the preview files of A / B / C / BC.
- **DEFERRED:** the Valkyrie wings (owner decision, a separate 3D model of the anime Valkyrie Armor). The two wing meshes of the preview are not shipped (`gen_water_bc_meshes.py` skips them for the game).

## 5. Cost: worst case (rules 9 and 11)

Server: **0 new packets, 0 server work, 0 per-player state.** All of it is client drawing of the existing effect packet.

**One effect** (peaks over every tick of its life, measured by running the real layer; the caps in `WaterBc` are these peaks, rounded up a little for the Roar):

| Look | before (old layer), quads | **BC LOD0** mesh + sprite quads | **BC LOD1** mesh + sprite | **A fall-back** sprite | sprite vertices of one activation (cap 400) |
|---|---|---|---|---|---|
| Roar | 51 | 335 + 54 | 169 + 32 | 20 | 216 |
| Lance | 51 | 110 + 39 | 66 + 20 | 21 | 156 |
| Cradle | 39 | 176 + 23 | 120 + 14 | 12 | 92 |
| Nest | 39 | 120 + 26 | 79 + 14 | 10 | 104 |
| Burst | 30 | 208 + 30 | 96 + 18 | 13 | 120 |
| Splash | 23 | 144 + 17 | 60 + 11 | 8 | 68 |
| Ring | 100 | 132 + 7 | 48 + 4 | 3 | 28 |

(vertices = 4 x quads; `water_ingame_budget.json`). The mesh quads never go through the 400-vertex activation buffer; they have their own capped buffer.

**All water effects in view together (the frame ceiling): 1200 mesh quads (4800 vertices) + 500 sprite quads (2000 vertices) = 6800 vertices at the very most, 3 mesh draw calls + one per sprite
texture and blend.** On medium and low quality the ceilings shrink with the quality's vertex allowance (14000 / 40000 and 6000 / 40000 of them, never below a tenth). The sum of the per-effect worst cases of the
admitted effects can never pass the ceilings (a unit test with 400 random crowds at three quality levels); the mesh buffer itself refuses a quad over the ceiling and counts it. Examples (`WaterBc.admit`, all at
once in view, nearest first):

| Crowd | LOD0 | LOD1 | sprite fall-back | not drawn | worst mesh quads (vertices) | worst sprite quads (vertices) |
|---|---|---|---|---|---|---|
| 1 roar | 1 | 0 | 0 | 0 | 340 (1360) | 56 (224) |
| 3 roars | 2 | 1 | 0 | 0 | 850 (3400) | 146 (584) |
| 10 roars | 2 | 3 | 5 | 0 | 1190 (4760) | 324 (1296) |
| 64 roars (the manager's own limit) | 2 | 3 | 13 | 46 | 1190 (4760) | 500 (2000) |
| 64 mixed looks | 3 | 6 | 21 | 34 | 1170 (4680) | 498 (1992) |
| 10 roars, medium quality | 0 | 2 | 4 | 4 | 340 (1360) | 156 (624) |
| 10 roars, low quality | 0 | 1 | 1 | 8 | 170 (680) | 56 (224) |

CPU: per mesh quad a basis product, a cross product, a handful of dot products and one colour (about 60 float operations, no allocation); 1200 quads about 70 thousand operations a frame; the sprite scenes
call `Math.sin` / `Math.cos` a few hundred times an effect. Nothing is allocated per frame: the scene, the toon instance, the vectors and the plan arrays are fields or statics; the 25 + 8 `ResourceLocation`s are
built once; no string, no list, no random object, no reflection, no world scan, no chunk load, no config read per frame (the option is a volatile field mirrored from the config). Effects of far players
follow the manager's distance cull and level of detail like every effect (not drawn beyond the far limit of the quality); the plan counts distance past the effect's reach, so a big effect counts as near while
its edge is near. Cleanup: the frame plan and the mesh buffer are emptied every frame, the effect expires like every other, `VfxManager.clear()` on logout. `PerfProbe`: the manager already times every
layer: `c.vfx.layer.WaterBcLayer`. Textures: 34 PNGs, about 0.8 MB in the jar; meshes: 22 text files of about 170 KB (loaded once, 732 quads).

## 6. Files

New: `vfx/WaterBc.java`, `WaterBcScene.java`, `WaterBcToon.java`, `WaterBcMeshes.java`, `WaterBcMeshBuffer.java`, `WaterBcBudget.java`, `WaterBcSelfTests.java`, `vfx/client/WaterBcConfig.java`, `WaterBcFrame.java`,
`WaterBcDraw.java`, `vfx/client/layer/WaterBcLayer.java`, `assets/nusmp/wbc_meshes/` (22), `assets/nusmp/textures/particle/wbc_*.png` (34), `tools/vfx_preview/water_bc_ingame.py`,
`tools/vfx_preview/scenes.d/water_bc.json`, `docs/previews/vfx_remake/water_new/water_ingame.png`, `water_ingame_budget.json`, this doc, the tests `WaterBcTest`, `WaterBcAssetTest`, `WaterBcSourceScanTest`.
Appended to (shared files, small): `vfx/client/VfxManager.java` (one registration, one plan line after the manager's own plan, one mesh flush line before the sprite flush), `NUSMP.java` (two lines),
`tools/gen_water_bc_meshes.py` and `tools/gen_water_bc_textures.py` (also write the game set; the preview output is byte for byte as before), `tools/vfx_preview/preview.py` and `Preview.java` (the water classes,
`--water`, `--tier`, `--follow`, the mesh JSON), `PerfSourceScanTest` (the three new render files are in the hot-file list). `AbstractVfxLayer`, `VfxVertexBuffer` and `PerfSourceScanTest`'s rules were NOT
changed (the existing `handles` hook is not even needed: no shape of the hybrid loses its layer under V1).

## 7. Not seen in a game, honest caveats

- **Nothing here was run in Minecraft.** The previews are a headless software rasteriser fed by the real Java classes (the scene, the toon shading, the buffers, the layer); the GL calls of `WaterBcDraw`
  (three passes, depth write on the solid one, state restore) were only type-checked, never executed. The first thing to look at in a game: the depth interplay of the cores with terrain and the glass sprites,
  and whether the translucent hulls and the sprites blend in the order the sheet shows.
- The glass is not sorted against the translucent hulls by depth (fixed order, as decided), so from some angles a glass sprite that belongs behind a hull is drawn in the batch order instead.
- GPU cost was not measured. The biggest unknown is overdraw: every glass layer is a full-size alpha-blended sprite over the core, 6 to 16 per effect; a particle-heavy fight could be fill-rate bound before it is
  vertex bound. The ceilings bound the vertices, not the pixels.
- The Lance is thin and edge-on from a side camera; the Roar's head is darker than its body (core palette); the Ring's mesh reads as a faceted kerb from close up (all as in the BC preview).
- The look of a payload is a rule on blocks per tick and on "follows its caster": a future page that fires `WATERFX_CAST` fast or `WATERFX_ZONE` placed gets the Lance or the Nest. The pages of today were
  checked (section 1). A caster inside a following zone is drawn from `from`, which follows it.
- Tinting is only for Splash and Ring (see section 1); a very dark page colour (for example `1A1A1A`) multiplies the greys to almost black.
- **The old `WaterSpellLayer` shapes are NOT taken over, but some of them are still fired**: `BasicArts` fires `WATER_CRADLE`, `WATER_JAVELIN`, `WATER_BURST`; `WaterBook`'s old static spells `WATER_DRAGON`, `WATER_NEST`;
  `TrueMagic` `WATER_NEST`; `CharacterArts` `WATER_DRAGON`; the Sea Dragon God's Tail page `WATER_BURST` (`water_new.md` called them unused: only the grimoire pages are). They keep the old look.
  See the decisions below.
- The CI smoke list in `.github/workflows/smoke.yml` (`SMOKE_EXPECT`) must get the prefix `magic.water_bc` (the self-test is registered, the workflow file was not touched here).

## 8. What was checked

Pure rules test `WaterBcTest` (shapes, the look from a payload, the real scene against every cap at every tier, tinted sprites all have a twin, admission of a crowd, 400 random crowds at three qualities,
the option, the tint choice, finite and deterministic geometry at awkward cameras and sizes, the camera frame and light, a piece placed and sorted into its pass, the tint, the buffer ceiling), asset test
`WaterBcAssetTest` (25 sprites, 8 twins with the silhouette of their original, no stray `wbc_` file, the old water textures, 22 meshes well formed with the preview's quad counts, no wing file), source scan
`WaterBcSourceScanTest` (server safety, no allocation per frame, the three-pass draw, the order of the manager's calls, one registration, V1 hand-back, the old layers unedited), the in-game self-test
`magic.water_bc` (runs on the dedicated server in CI), the V1 equivalence through the preview (7 of 7), the type check of every client file, and the existing guards (`ServerSafetyTest`, `ConfigFileNamesTest`,
`PerfSourceScanTest`, `FxSourceScanTest`, the particle / texture / lang tests, the Wind tests that read `VfxManager`): see the report of the change for the results.

## 9. Decisions left for the owner

1. The five old `WaterSpellLayer` shapes that are still fired (section 7) keep the old look: take them over too (map WATER_DRAGON to the Roar, WATER_JAVELIN to the Lance, WATER_CRADLE, WATER_NEST, WATER_BURST,
   one line each in `WaterBc.kindOf` and `WaterBc.SHAPES`, the layer needs no other change)?
2. The Roar / Lance split by speed (1.5 blocks a tick) and the Cradle / Nest split by "follows its caster" are rules of today's pages: keep them, or give the Lance its own marker?
3. The Valkyrie wings: when the 3D model of the armour exists, its effect can be a seventh-and-a-half tier of this same mesh path (the buffer, the plan and the ceilings are ready for one more piece).
4. Should the effects beyond the sprite ceiling (46 of 64 roars at once) be drawn as dust instead of nothing? (Today: not drawn; the manager's own limit is 64 effects.)
