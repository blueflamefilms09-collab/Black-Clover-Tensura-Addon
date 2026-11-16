# Roblox-style VFX pass

Owner request: "Rework the VFX. Some of them look like they're PNGs ... look up Roblox GIF effects and try to do that kind of style. Even if it's particle heavy, try to reduce it so it isn't particle heavy."

What this is: ten effects that read as flat camera-facing sprites are redrawn with a small shared toolkit that builds them the way Roblox VFX artists do (a few layered textured meshes with moving UVs, expanding shock rings, flip-book sprites, timed anticipation / burst / dissipate). Every one of them is cheaper in quads than the drawing it replaces (one documented exception), and the old drawing stays, selectable by a client setting.

Not in this pass: the Time Magic VFX (untouched, it is the quality bar), Greed, Nen, the devil forms, the Star / Fire / Wind-MC / Water-BC looks (already reworked in earlier waves), anything server side.

## 1. Audit: which effects look like flat PNGs

Method: which layer actually draws each shape (last registration wins in `VfxManager`; the old Flame / Wind / Water V1 layers are overridden by FireV2 / WindMc / WaterBc, so they are not what players see), then what that layer builds (count of camera-facing billboards vs real geometry) and how it was previewed (`tools/vfx_preview`). Ranked by how flat it reads and how often it is on screen:

| # | Shape | Layer file | Why it reads flat |
|---|-------|-----------|-------------------|
| 1 | KI_BEAM (Kamehameha) | `vfx/client/layer/KiLayer.java` | a stack of billboard strips and about 69 quads; the jagged sprite fringe is the "PNG" look |
| 2 | ANTI_MAGIC_SLASH | `vfx/client/layer/AntiMagicLayer.java` | 83 quads of billboard smoke and sparkle crosses; the crescent is a sprite chain |
| 3 | MAGIC_CIRCLE_EXPLOSION (burst) | `vfx/client/layer/MagicCircleLayer.java` | a starburst of sprite spikes and a cloud of "+" sparks, no shock ring or ground read |
| 4 | WIND_TORNADO | `vfx/client/layer/WindSpellLayer.java` | 96 quads of crossed strips, no real funnel |
| 5 | WATER_BURST | `vfx/client/layer/WaterSpellLayer.java` | 85 quads of billboard droplets |
| 6 | ICE_FX3 (Shatter) | `vfx/client/layer/IceLayer.java` | billboard shards, no rings |
| 7 | KI_SHOCKWAVE | `vfx/client/layer/KiLayer.java` | spiky starburst flash, one thin ring |
| 8 | LIGHTNING_SPEAR | `vfx/client/layer/ElementShapesLayer.java` | flat bolt strip, little glow structure |
| 9 | EARTH_SPIKES | `vfx/client/layer/ElementShapesLayer.java` | 11 flat quads, spikes are painted triangles |
| 10 | KI_BLAST | `vfx/client/layer/KiLayer.java` | small orbs with a sprite pop |

Items 1-10 were upgraded. Candidates left for a later pass (not touched): the remaining ElementShapes shapes, the Greed / devil particles, Nen shapes.

## 2. Research (honest summary)

YouTube cannot be watched here, so this is from written breakdowns found by web search. Page fetches of the forum hosts were mostly blocked in this environment, so most of what follows is from search-result snippets, not full pages. Treat it as vocabulary, not as verified numbers.

Sources reached (snippet or page): Roblox DevForum "How to create VFX" (devforum.roblox.com/t/how-to-create-vfx/4029502), "How to make animated slash effects with only Blender" (.../1794034), "Scrollable textures for MeshParts" (.../2820890), the tornado threads (.../723959, .../768500, .../3668521), the Roblox VFX tutorial site (robloxvfxtutorial.pages.dev), the Packt "Roblox Game Building Cookbook" flip-book explosion section, and RealtimeVFX breakdown threads (mesh "soulercoasters" 1987, stylised explosion 1767 / 1894, pulse cannon 3161, tornado meshes 20420).

Could not be fetched in full: the DevForum threads and RealtimeVFX threads above (blocked or truncated), YouTube and any video.

What the sources agree on, and what the toolkit copies:

- Effects are a few layered meshes (swoosh, slash, cone, ring) with a scrolling or panned UV, not hundreds of particles. A slash is a curved mesh with a texture that scrolls along it.
- A shockwave is 2-3 expanding rings with offset timing, a fast ease-out (quartic) and a fade; plus a ground decal.
- Tornado / vortex: textured cones counter-rotating, with twist and a panned swirl texture.
- Flip-book sheets (4x4 cells) with a margin and a cross-fade replace many particles for a puff or burst.
- Additive white-hot core plus a tinted alpha shell plus a halo gives the "glow" without bloom.
- Tapered gradient trails; secondary stretched sparks; timing is anticipation, burst, dissipate with ease curves.

## 3. The toolkit

Pure half, `vfx/RobloxMath.java` (no game types; unit tested): the switch, texture list, ease and timing curves (`easeOut`, `shockRadius`, `easeOutBack`, `burstEnvelope`, `fadeInOut`), flip-book cell maths, ribbon `taper`, deterministic `hash`, the quad cost of each primitive, and the `MAX_QUADS` / `OLD_QUADS` tables.

Client half, `vfx/client/RobloxStyle.java` (allocation-free, static scratch, camera-relative): `swoosh` (scrolling-UV tapered ribbon), `sideRibbon` (crossed second plane), `ring`, `shock` (2-3 layered rings, quartic), `cone` (twist, spin, scrolling UV), `flipbook` (cell + cross-fade), `coreShell` (halo + white-hot core + optional alpha shell), `decal` (ground ring), `streak` (stretched spark), and path helpers (`line`, `jagged`, `spiral`, `arcPath`).

Effects, `vfx/client/RobloxFx.java`: one method per shape; each layer calls `RobloxFx.draw(inst, ctx, buf)` first and carries on with its own, unchanged code when it returns false.

Textures, `tools/gen_roblox_vfx_textures.py` (deterministic numpy + PIL), ten `rbx_*.png` in `textures/particle`: `rbx_swoosh`, `rbx_bolt`, `rbx_swirl`, `rbx_ring`, `rbx_decal_ring`, `rbx_glow`, `rbx_streak`, `rbx_flip_burst`, `rbx_flip_fire` (4x4 cells of 64 px), `rbx_rock`. White RGB under alpha, real alpha falloff, alpha exactly 0 on every border that ends on air (docs/vfx_square_bug.md), every flip-book cell clear on its border.

Setting: `vfx.robloxStyle` (default true) in the CLIENT config `nusmp-roblox-vfx-client.toml` (`vfx/client/RobloxStyleConfig.java`; one init line in `NUSMP.java`). false = the old layers, exactly. Looks only; the server never reads it and nothing is sent.

## 4. What changed per effect

| Shape | New drawing | Quads old -> new (worst case, full detail) |
|-------|------------|------------------------------------------|
| KI_BEAM | scrolling-UV shell, crossed shell, mid and white-hot bolt core, two spiral arms, two rings running down it, core + shell ball, flip-book impact, back-flying sparks | 69 -> 65 |
| KI_SHOCKWAVE | flip-book flash, two layered ground rings, dust ring, rising swirl dome, decal, ground rays | 70 -> 63 |
| KI_BLAST (power 8) | tapered trail + core orb per shot, flip-book pop | 26 -> 20 |
| LIGHTNING_SPEAR | jagged bolt as shell + hot core ribbons, fork, core + halo and flip-book at the end | 18 -> 17 |
| EARTH_SPIKES | real 3D three-sided stone spikes (rock texture) erupting along the line, decal, glow | 11 -> 22 (the exception: real geometry instead of flat triangles) |
| ANTI_MAGIC_SLASH | black swoosh crescent with rim and hot edge, ghost crescents, smoke flip-books | 83 -> 47 |
| WIND_TORNADO | two counter-turning twisting cones with a scrolling swirl, two soft spiral arms, foot ring | 96 -> 60 |
| ICE_FX3 | flip-book flash, two layered shock rings, five ice-blade cones, frost decal, mist | 58 -> 54 |
| WATER_BURST | two crown cones with scrolling streaks, two ripple rings, stretched droplets, flip-book splash | 85 -> 48 |
| MAGIC_CIRCLE_EXPLOSION (burst) | flash, two layered shock rings, flip-book burst, decal of the broken circle, stretched sparks | 69 -> 53 |

Measured with the preview harness on a half-tick sweep of the whole life of each scene (the scenes in `tools/vfx_preview/scenes.d/roblox.json`). All ten together: 585 old -> 449 new quads at their worst moment (about 23 percent fewer), and nine of ten are individually cheaper or equal.

## 5. Previews

`docs/vfx_roblox_previews/rbx_<name>_old.png` (the old effect, setting off) next to `rbx_<name>.png` (new) for all ten, rendered by `python tools/vfx_preview/preview.py rbx_<name>` (the real layer classes against stubs, software rasteriser, vertex-budget report). A still sheet cannot show motion: the scrolling UVs, the counter-rotation of the tornado cones, the shock ring expansion and the flip-book playing are invisible in it, and the software raster has no real GL blending or depth. The sheets show shape, layering and budget only.

## 6. NEW / REPLACED / KEPT

- NEW: `RobloxMath`, `RobloxStyle`, `RobloxFx`, `RobloxStyleConfig`, the ten `rbx_*` textures and their generator, the scenes file, three tests, the config file `nusmp-roblox-vfx-client.toml`, the previews.
- REPLACED (visually, with the setting on; the code is kept): the ten shapes above.
- KEPT: every old drawing, in its own layer, reached when `vfx.robloxStyle = false`; all old textures; the Time Magic VFX; every other shape; save data and packets (nothing changed on the wire).
- Shared files appended: one line in `NUSMP.java`; `tools/vfx_preview/preview.py` and `Preview.java` gained the new classes and a `--roblox` switch for the before sheets.

## 7. Worst-case cost (rule 11)

- Client draw: at most 68 quads (272 vertices) for one effect at full detail (KI_BEAM table value), always below the engine's 100-quad cap, which `VfxVertexBuffer` enforces on top. `ctx.seg` thins every ring, cone and ribbon with distance and the particle setting. Far effects are culled by the existing `VfxManager` distance rule and `VfxQuality.maxActive`.
- No per-frame allocation (static scratch vectors, render thread only), no string or location building, textures resolved once in static fields. Every random is `RobloxMath.hash` of the effect's seed.
- Server: nothing. No packets, no ticks, no state. Effects are the same instances as before, so the packets-per-second and server work are unchanged.
- Setting off: exactly the old cost.

## 8. Tests

- `RobloxMathTest`: curves, burst continuity, flip-book cells and frame order, taper, hash determinism and spread, `seg`, quad-cost formulas, shape list and switch.
- `RobloxStyleAssetTest`: every texture exists, power-of-two size, real alpha falloff, clear borders exactly 0, every flip-book cell clear on its border, no black clear texel next to a visible one, no stray `rbx_` file, the generator makes every name, the old textures and Time Magic ones are still there.
- `RobloxStyleBudgetTest`: every effect under 100 quads and not dearer than the old one (except EARTH_SPIKES, capped at 30), pure class names no client or game class, the setting is a CLIENT config with its own file, no allocation in the toolkit outside static fields, every layer hooks the toolkit and keeps its old drawing, Time Magic not touched.
- `PerfSourceScanTest` was not extended: no new pattern appeared (the hooks add no location, reflection or allocation to the layers).
- Run here: the three new tests (20 + 5 tests) and `ConfigFileNamesTest` passed with the JUnit console jar; the full Gradle sweep was not run (no local Gradle).

## 9. Caveats

- Nothing was seen in a game. The Java was compiled only by the preview harness against stubs; Gradle has not built it. Expect to fix a stub-vs-real signature mismatch if there is one.
- Earth spikes cost 22 quads against 11 before; they are real 3D stone now.
- The previews are stills; motion, true blending and the flip-book timing are unverified.
- Research was snippet level (see section 2).
- The effect quad tables are measured with the preview scenes' parameters (KI_BLAST at power 8); other powers or sizes can differ but remain bounded by the engine's cap.
