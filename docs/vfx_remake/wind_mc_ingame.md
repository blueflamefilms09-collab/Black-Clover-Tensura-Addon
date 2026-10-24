# Wind VFX, Minecraft style C "pixel-anime", built into the game

Owner decision after the phase A previews (`wind_mc.md`): build version **C** for the Wind effects below. This is the in-game build. Nothing could be run in a game here; what was
checked is listed in section 7.

## 1. What is built, in plain words

| Effect (shape) | What it looks like now |
|---|---|
| Crescent Kamaitachi (`WIND_SLASH`) | a 32 px pixel crescent with a dark green outline, two held smear ghosts behind it, two S ribbons, two cross sparks on the tips, a flash on the first ticks |
| Slicing Wind Emperor (`WIND_EMPEROR`) | a big crescent with three ghosts, three white ribbons across the blade, six crosses along the edge, six thin rays |
| Gale / Gust Lane (`WIND_GALE` and `FX_WIND_GUST`) | the **swallow sprites (kept, as the owner asked)** dart down the lane, each trailing a pixel ribbon, plus three streaks, two crosses ahead and three pixel leaves. `FX_WIND_GUST` had no layer at all (only vanilla dust was spawned): it now draws the same lane |
| Wind Ring (`WIND_RING`) | two broken-ring decals on the ground (two frames), a ground swirl, six rising ribbons, four sparks |

Style rules of C, all on: nearest-neighbour sampling, hard alpha stepped in four levels (never a smooth ramp), everything drawn **on twos** (state held for two ticks, the frame's partial tick is
ignored), sprites **flip between two frames every three ticks**, smear ghosts on the crescents. Palette: shadow green outline, deep green, lime, mint, white (the five pack colours).

**Not touched, in every setting:** the Wind Tornado (`WIND_TORNADO`, Spirit Storm / Tornado Fang) and Spirit of Zephyr (`WIND_ZEPHYR`) keep exactly today's code and look; they stay with
`WindSpellLayer`, whose file is not edited. Spirit Storm stays the tornado; the optional beam and drill shapes were not built. The old textures (swallow, streak, band, leaf, slash) stay.

**Tint (Crescent Slash is reused by six non-wind melee pages, and the Ring and Gale by other pages).** The book passes its colour. A wind green (hue 95 to 175 degrees, e.g. `8CFFC2`, `A6FFD6`)
draws the pack palette textures with a white vertex colour. Any other colour (Iron Fist, Palm Strike, Beast Charge, Feral Claws ...) draws the **grey twin** of the same texture
(`wmc_*_t.png`, five greys with the same silhouette) multiplied by that colour, so the effect keeps its book colour (orange example in the sheet, row 2).

## 2. Setting

CLIENT config `config/nusmp-wind-mc-client.toml`, key `vfx.element.wind = MC_C | V1`, default **MC_C**. **V1 = exactly the old code for all shapes**: `WindMcLayer` is registered after
`WindMagicLayer` / `WindSpellLayer` (the manager keeps the last layer registered for a shape) and hands every call (render, default duration and colour, budget, spawn) back to the old layer that owned
the shape; `FX_WIND_GUST` becomes a shape without a layer again (the manager falls back to dust, via the new `AbstractVfxLayer.handles`). Checked: the preview runs `WindMcLayer` with V1 and the old
layers on the same scenes and the quads are identical (slash, emperor, gale, ring). The setting only changes what this client draws; the server never reads or registers it.

## 3. NEW / REPLACED / KEPT (rule 5)

- **NEW:** `WindMcLayer` (painter), `WindMc` (pure rules), `WindMcConfig` (client option), `WindMcSelfTests` (`magic.wind_mc`), 32 textures `wmc_*.png`, `FX_WIND_GUST` is drawn at all (new),
  the `handles` hook, the preview scenes and tools, the tests.
- **REPLACED (look only, behind V1):** Crescent Slash, Slicing Wind Emperor, Gale, Wind Ring. The old code is not deleted or edited.
- **KEPT:** `WindMagicLayer`, `WindSpellLayer` (Tornado and Zephyr, untouched), every old texture, the swallow sprite, `VfxShape` (no new enum entry), the Time Magic VFX (not touched), the phase A
  preview files.

## 4. Cost: worst case (rules 9 and 11)

Server: **0 new packets, 0 server work, 0 per-player state.** Everything is client drawing of the existing effect packet.

Peak quads of one activation over **every tick of its life**, measured by running the real layers through the Java preview (`wind_mc_ingame_budget.json`); detail 1.0 is full, detail 0.5 is the
lowest level of detail (particles "Decreased" or the distance level of detail; counts go through `ctx.seg`):

| Effect | before, full | before, 0.5 | **C, full** | **C, 0.5** | vertex budget of one activation (cap in quads x 4) |
|---|---|---|---|---|---|
| Slash | 72 | 37 | **8** | 5 | 288 |
| Emperor | 80 | 48 | **18** | 10 | 320 |
| Gale | 68 | 40 | **16** | 9 | 272 |
| Gust Lane (`FX_WIND_GUST`) | 0 (dust only) | 0 | **16** | 9 | 272 |
| Ring | 100 | 86 | **13** | 8 | 400 |

(Phase 1 had counted 66 for the Gale; every tick of the real layer reaches 68, so the cap is 68.) The worst case per frame stays bounded by what was already bounded: at most 64 running
effects (`MAX_ACTIVE`, fewer on medium and low), the distance cull and level of detail of `VfxManager`, the frame vertex allowance of `VfxQuality`; the vertex budget of an activation is now the old
peak or less and is enforced by `VfxVertexBuffer.hasBudget`. Even 64 emperors at once would be 64 x 18 = 1,152 quads, where 64 old rings were 6,400. Per frame there is no allocation (every vector is a static
scratch, the 32 `ResourceLocation`s are built once in a static initialiser), no string, no reflection, no world scan, no chunk load, no config read (the style is a volatile field mirrored from the
config on load / reload). The nearest-neighbour switch is one `setFilter` per texture batch (not per quad). `PerfProbe`: `VfxManager` already times every layer, so the new layer reports as
`c.vfx.layer.WindMcLayer`. An effect is cleaned up like every other (it expires with its duration; `VfxManager.clear()` on logout); nothing is held per player.
Textures: 32 PNGs of 16x16 to 32x32, about 12 KB in total.

## 5. Files

New: `vfx/WindMc.java`, `vfx/WindMcSelfTests.java`, `vfx/client/WindMcConfig.java`, `vfx/client/layer/WindMcLayer.java`, `assets/nusmp/textures/particle/wmc_*.png` (32),
`tools/vfx_preview/wind_mc_ingame.py`, `tools/vfx_preview/scenes.d/wind_mc.json`, `docs/previews/vfx_remake/mc/wind_mc_ingame.png`, `wind_mc_ingame_budget.json`, this doc, the tests
`WindMcTest`, `WindMcAssetTest`, `WindMcSourceScanTest`.
Appended to (shared files): `vfx/client/AbstractVfxLayer.java` (`handles`), `vfx/client/VfxManager.java` (one registration, one `handles` line), `vfx/client/VfxVertexBuffer.java` (nearest filter for
`wmc_` textures), `NUSMP.java` (two lines), `tools/gen_wind_mc_textures.py` (writes the game set; the preview output is byte for byte as before), `tools/vfx_preview/preview.py` and `Preview.java`
(copy `WindMc.java`, `--style`), `PerfSourceScanTest` (the new layer is in the hot-file list).

## 6. Not seen in a game, honest caveats

- **Nothing here was run in Minecraft.** The previews are a headless software rasteriser (painter's-order sort, no depth buffer, no bloom, no world light, no depth against blocks). The real game
  renders with the vertex buffer's depth test (no depth write), so overlapping quads of the same batch can look different. The nearest-neighbour switch (`setFilter(false, false)`) is the standard call
  and is unseen in a game; if a pixel texture ever looked blurry, that line in `VfxVertexBuffer.flush` is the place.
- The swallow sprites are the old smooth sprites (kept on request) and still read as small aeroplanes next to the pixel ribbons.
- The Ring is a flat decal: edge-on it vanishes; with a big power the 32 px texture means big pixels (a ring of power 4 has pixels of about 1.2 blocks).
- Emperor: radius `(2.6 + 2.2 t) x power`, i.e. up to 8.6 blocks at the power 1.8 the page passes (the old layer drew up to 7.6).
- A very dark book colour (for example `1A1A1A`) multiplies the greys to almost black: the tint works, the outline disappears.
- The CI smoke list in `.github/workflows/smoke.yml` (`SMOKE_EXPECT`) must get the prefix `magic.wind_mc` (the self-test is registered, the workflow file was not touched here).

## 7. What was checked

Pure rules test `WindMcTest`, texture test `WindMcAssetTest`, source scan `WindMcSourceScanTest`, the in-game self-test `magic.wind_mc` (runs on the dedicated server in CI), the V1 equivalence
through the preview, and the existing guards (ServerSafetyTest, ConfigFileNamesTest, PerfSourceScanTest, FxSourceScanTest, the particle / texture / lang tests): see the report of the change for the
results.

## 8. Decisions left for the owner

1. Keep the swallow sprites as they are, or later give the Gale a pixel creature (the hawk of phase A was dropped because of this decision)?
2. Tornado and Zephyr stay old; should they get the pixel look later (separate decision)?
3. The six melee pages and other users of the Ring / Gale keep their book colour through the grey twins: is the tinted look wanted, or should every magic get the wind green?
