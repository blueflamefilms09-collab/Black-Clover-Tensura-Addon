# Wind VFX remake, phase 1: three art directions (nothing is built yet)

Inputs: `wind_research.md` (canon, current look, gaps), `docs/previews/vfx_remake/wind_baseline.png` (current), `docs/previews/vfx_remake/wind_reference_board.png` (owner's pack + clips).
Pick one direction (or mix per shape, the table in section 5 allows it). Nothing below is implemented.

## 1. Common rules for all three

**New vs replacement (rule 5):** this is a **replacement of the look** of existing shapes, not new gameplay. Following rule 2 the old look is kept and a switch added:
- Client config `vfx.element.wind = V2 | V1`, default V2 after the owner approves, **V1 = today's code, untouched**. It is a CLIENT config (looks only, rule 9). Place: a new `client/aura`-style `ElementVfxClientConfig` (new file), name checked by `ConfigFileNamesTest`.
- No `VfxShape` enum change needed: the same shapes (`WIND_TORNADO, WIND_GALE, FX_WIND_GUST, WIND_EMPEROR, WIND_ZEPHYR, WIND_SLASH, WIND_RING`) render through a V2 branch. The V2 code goes in NEW classes (`WindV2Fx`, `WindV2Spell`, `WindV2Magic`) that the old layers call at the top of `render` when the option is V2; the V1 bodies are not edited.
- Optional new shapes **appended at the end** of `VfxShape` (only if the owner wants them): `WIND_BEAM` (Spirit Storm palm beam), `WIND_DRILL` (Tornado Fang). Without them Spirit Storm keeps the tornado.
- New textures are NEW files `textures/particle/wind2_*.png` from a NEW script `tools/gen_wind2_textures.py` (Pillow + numpy). Old `wind_*.png` and `tools/gen_element_vfx_textures.py` stay.
- Helpers are appended to `ElementFx` (e.g. `ribbon(...)`), existing ones unchanged.

**Budget (rules 9, 11):** at most the CURRENT peak quads per shape (96 / 66 / 80 / 92 / 72 / 100), target 15 to 30 percent lower; hard cap stays 400 vertices per activation (`MAX_VERTICES_PER_ACTIVATION`), all counts go through `ctx.seg(full, min)` (LOD) and `buf.hasBudget(n)`. **No shaders**, no per-frame allocation (reuse 3 `Vector3f` scratch fields), deterministic `hash(seed, i, salt)` instead of `inst.random()` (fixes the slash's per-frame random). No new packets, no server change: worst case network cost is unchanged (0 new packets). Far players: skip all decoration (leaves, sparks) beyond 48 blocks, keep only the main silhouette (existing `ctx.seg` min values).

**Blend and colour:** base tint stays `inst.color` (book default 0xFF8CFFC2 / layer 0xFFA6FFD6) so every spell that passes its own colour still works; the palette below is the *default* when colour is the default. Palette sampled from the owner's art:

| Name | Hex | From |
|---|---|---|
| core white | #F8FBF0 | anime Zephyr ribbons |
| cream glow | #EEFAA1 | manga burst highlights |
| mint rim | #CCEEBE | anime sword / body |
| wind green | #89F842 | manga lime |
| mid green | #54D234 | manga |
| deep green | #46A92C | manga shade |
| shadow green | #37641E | manga shade, outlines |
| teal rim | #7FE8D0 (eyeballed) | other-mod clip |

**Textures (all Pillow, white-on-alpha unless stated, so vertex colour tints them):**

| File | Size | Recipe |
|---|---|---|
| `wind2_crescent` | 256x128 | outer circle R=1.0 minus inner circle R=0.82 offset +0.28 on x, both supersampled 4x; alpha hard step with 1.5 px AA; grey 255 core band, 205 on the outer 12 percent (cel rim) |
| `wind2_ribbon_s` | 256x64 | centre line y = 0.22 sin(2 pi u); half width w(u)=0.5 sin(pi u)^0.7; pointed tails; hard alpha, 1 px AA; core 255, edge 215 |
| `wind2_ribbon_thin` | 256x16 | gaussian line sigma 1.6 px along centre, ends taper by u^0.5 and (1-u)^0.5; alpha peak 255 |
| `wind2_wisp` | 256x64, tileable | domain-warped fbm (3 octaves) stretched 6:1 along U; streak mask sin^4; soft edge in V; alpha max 230 |
| `wind2_puff` | 128x128 | radial falloff x fbm 2 octaves, alpha max 200 (dust, soft cloud) |
| `wind2_leaf` | 32x32 | lancet leaf with midrib, hard edge; colour baked light green #A8E06A so it ignores tint |
| `wind2_spark` | 32x32 | 4-point star, thin arms, white |
| `wind2_ray` | 64x256 | thin tapered ray, bright at the root |

## 2. Direction A: "Anime cel" (flat, hard-edged, on twos)

**Reads like** the owner's anime frame: white S-ribbon blades and a pale crescent with sharp tails, flat colours, 4-point sparkles, nothing soft.

- **Blend / look:** `ALPHA` for shapes (no WIND 0.75 softening), `ADD` only for the one-frame flash. Two tones per shape: core white #F8FBF0 on top of a mint #CCEEBE slab 1.25x bigger and 0.55 alpha as the "rim" (2 stacked quads share one mesh). A dark underlay is NOT used (keeps sky readable).
- **Motion:** "animate on twos": all age-driven values use `aq = floor(age / 2) * 2`; smear frame: first 2 ticks the crescent is stretched 1.7x along the flight and thinned 0.6x, then snaps to normal; ribbons wobble by a fixed 3-keyframe pose cycle (not smooth sine). Easing for travel: `easeOutCubic` over 0.7 of the duration.
- **Per shape:**
  - SLASH (Kamaitachi): 1 `wind2_crescent` (R = 1.6 p), 2 `wind2_ribbon_s` trailing 0.6 and 1.1 blocks behind (scaled 0.8, 0.6; alpha 0.8, 0.5), 3 sparks at the tips, 1 flash billboard on spawn. **8 quads** (vs 72). Hold-frame fade in the last 4 ticks (alpha steps 1, 0.6, 0.3, 0).
  - EMPEROR: 1 giant crescent (R = 3.5 p growing to 6 p), 3 S-ribbons across it (rotating 25 degrees apart), 6 sparks, optional 6 thin rays. **16 quads** (vs 80). Because it is few and big, it reads at range.
  - TORNADO: 5 stacked cel funnel sections, each a hoop of `wind2_ribbon_s` x 6 around (rotation on twos, 0.5 rad per 2 ticks near the base, 0.25 at the top), top ring has 8 `wind2_leaf`; ground dust ring 1 hoop `wind2_puff`. 5 x 6 + 8 + 12 = **50 quads**. Drill version (Tornado Fang): same stack laid along `dir`, narrow at the tip, pitch 0.9 rad.
  - GALE / FX_WIND_GUST: 5 S-ribbons flying down the lane with 3-keyframe wobble + 4 sparks + 3 rolling rings (hoop with 12 segs of thin crescent texture) = **~40 quads**. No swallows.
  - ZEPHYR: 2 big hard crescent wings (`wind2_crescent` R = 1.4 p, mirrored, flap on twos +-0.25 rad), halo ring of 12 small crosses (spark texture) above the head, 3 S-ribbons orbiting the body (counter-rotating), 6 leaves = **~34 quads**.
  - RING: 2 ground rings (hoop `wind2_ribbon_s` x 16 segs), 1 puff ring, 4 sparks = **~44 quads** (vs 100).
- **Particle counts (client, no entities):** sparks 3 to 6, leaves 6 to 8 per effect, no more.
- **Risks:** looks flat in dark caves (fix: ADD flash + glow billboard); "on twos" can look like lag at low FPS (config toggle `smooth`); hard alpha edges alias at distance (mipmaps blur them: keep 1.5 px AA and LOD); 6 non-wind melee pages share SLASH, hard white would look odd for e.g. Iron Fist (tint by `inst.color`, drop sparks when colour is not wind-green).
- **Cost:** lowest of the three (8 to 50 quads, 0 extra packets).

## 3. Direction B: "Volumetric gale" (additive layered wisps)

**Reads like** the manga AMV's glow: lots of translucent layers, soft light, dust, depth; closer to the current look but richer.

- **Blend / look:** `ADD` for wisps and glows, `ALPHA` for dust/puffs. Colour ramp by layer: inner layer #F8FBF0 alpha 0.55, middle #CCEEBE alpha 0.40, outer #54D234 alpha 0.25 (outer = the green halo of the manga). Soft `wind2_wisp` and `wind2_puff`; billboards `glow` at the hot spot.
- **Motion:** continuous, smooth. Layers counter-rotate (k-th layer angular speed `w_k = (0.10 - 0.025 k) * (1 + 0.8 (1 - f))` rad/tick, f = height fraction 0..1, so the base spins faster), radial breathing `r *= 1 + 0.06 sin(0.2 age + k)`, uScroll `+0.05 age`. Fade-in 6 ticks easeOutCubic, fade-out 12 ticks linear.
- **Per shape:**
  - TORNADO: 3 funnel layers x 3 height sections as `wisp` hoops (9 hoops x 8 segs = 72 quads is too much, so 8 segs for the inner layer, 6 for the others: 3 sections x (8 + 6 + 6) = **60 quads**), 6 streaks, dust hoop 6 segs, 4 leaves, 1 glow at the foot = ~**78 quads** (vs 96).
  - SLASH: 3 stacked crescent arcs (`wisp`, wide 0.9, 0.6, 0.3 of R), 4 afterimage arcs fading 0.35 / k, 6 streaks, 1 glow = **~50 quads**.
  - EMPEROR: 5 layered arcs of `wisp` + 1 bright inner arc + 8 streaks + glow = **~64 quads**.
  - GALE: 6 wind "tubes" (beams of `wisp` with taper) weaving along the lane + 10 streaks + 2 rings = **~40 quads**.
  - ZEPHYR: wind mantle (2 hoops), 4 soft wings (arcs `wisp`), 6 spiral streaks, 8 motes, ground ring = **~70 quads**.
  - RING: 3 ground ripples (`wisp` rings 16 segs, 1 quad row) + dust puff ring 12 + 4 spiral beams = **~78 quads**.
- **Particle counts:** leaves 4 to 6, motes 6 to 8, dust 12 puffs.
- **Risks:** highest overdraw (large additive quads): washes out over bright sky and snow (clamp ADD alpha to 0.55, check `VfxSafeBlendConfig`); closest to the current look so the owner may say "not a remake"; hard to make a readable silhouette (the exact complaint about the baseline); soft textures need good fbm to avoid tiling lines.
- **Cost:** about 50 to 78 quads, highest fill rate (not vertex) cost.

## 4. Direction C: "Hybrid ribbon cage + anime crescents" (recommended)

**Reads like** the owner's clip (thin white-teal spiral ribbon cages) combined with the anime frame's hard crescents and S ribbons: the crescents and S ribbons carry the silhouette, the thin ribbons carry the motion.

- **Blend / look:** crescents `ALPHA` (cel, as A); ribbons `ADD` with `wind2_ribbon_thin` (white core, tint teal #7FE8D0 at the ends via vertex colours: root = core white #F8FBF0, tip = teal alpha 0). One soft `glow` billboard per effect. Green body (#89F842 at 0.25 alpha) only on Zephyr.
- **Ribbon geometry (the new helper `ElementFx.ribbon`):** a strip of `n` segments along a parametric curve `P(f)`, camera-facing width `w(f) = w0 * sin(pi f)^0.7` (min 0.02 block), U along length, 1 quad per segment. Spiral cage ribbon `j` of `J`:
  `theta(f, t) = theta_j + omega * t + 2.6 f`, `omega = 0.32 rad/tick at the base falling to 0.18 at the top`, `radius(f) = R (0.3 + 0.7 f)`, `y(f) = H f`, plus a tilt of the loop plane `tilt_j = 0.25 + 0.35 hash_j` rad, so loops cross like the clip's lattice. Each ribbon fades `alpha(f) = sin(pi f)` and is born staggered `t_j = 2 j` ticks.
- **Motion:** smooth for ribbons, "on twos" for crescents and the smear frame (as A), so two timing languages (like Pierrot: smooth FX layers, held cel shapes).
- **Per shape:**
  - SLASH: crescent (cel, R = 1.6 p) + 2 thin ribbons spiralling around the flight line (radius 0.5, 1 turn, 5 segs each) + 2 trailing S-ribbons + tip sparks = **~20 quads** (vs 72).
  - EMPEROR: giant crescent + 3 S-ribbons + a **cage of 6 thin ribbons x 6 segs** wrapped around the flight line (a "wind sword swing" read) + glow = **~48 quads** (vs 80).
  - TORNADO: cage of **10 thin spiral ribbons x 6 segs** = 60 quads forms the funnel (that is the whole body), 1 puff foot ring (8) + 6 leaves + 4 white cel S-ribbons hugging the walls (4 x 2) = **~82 quads** (vs 96). Tornado Fang drill: same cage laid horizontally, pitch 0.9 rad, narrowing toward the tip.
  - GALE / FX_WIND_GUST: 4 S-ribbons lane + 6 thin ribbons weaving (6 x 5 = 30) + 3 rings = **~54 quads** (vs 66).
  - ZEPHYR: cast burst (first 12 ticks): 8 thin ribbons x 5 segs spiralling up (40 quads, fading out over 10 ticks) then steady: 2 hard crescent wings (flap 0.2 sin), 12-spark halo, 3 slow ribbons x 5 around the body (15) = **~48 steady, ~70 peak** (vs 92).
  - RING: 2 ground rings (cel) + 6 thin ribbons rising in a spiral (6 x 4) + puff ring = **~50 quads** (vs 100).
- **Particle counts:** leaves 4 to 6, sparks 4 to 8; no entities.
- **Risks:** very thin lines (1 to 3 px) **shimmer / vanish at distance** (min width 0.02 block and fade by distance past 24 blocks; mip bias); ribbon helper is new geometry (needs a pure test for normals and segment counts and a perf self-test); two timing languages need a toggle for smooth-only; clip's look is partly bloom, which the game may not replicate.
- **Cost:** 20 to 82 quads, moderate overdraw, ribbons add the most vertices per effect but stay below today's cap.

## 5. Which existing shape each direction replaces (V1 stays behind the option)

| Shape (V1 kept) | A cel | B volumetric | C hybrid | Quads now |
|---|---|---|---|---|
| WIND_TORNADO | 50 | 78 | 82 | 96 |
| WIND_GALE | 40 | 40 | 54 | 66 |
| FX_WIND_GUST (no layer today) | 40 | 40 | 54 | 0 |
| WIND_EMPEROR | 16 | 64 | 48 | 80 |
| WIND_ZEPHYR | 34 | 70 | 48 to 70 | 92 |
| WIND_SLASH | 8 | 50 | 20 | 72 |
| WIND_RING | 44 | 78 | 50 | 100 |

New files (all NEW, none edited except one-line V2 switches in the 2 old layers): `tools/gen_wind2_textures.py`, `textures/particle/wind2_*.png` (8), `vfx/client/layer/WindV2Fx.java`, `WindV2Spell.java`, `WindV2Magic.java`, config class, preview scenes `tools/vfx_preview/scenes.d/wind_v2.json`, tests (pure geometry + texture-file test + `PerfSourceScanTest` extension for no per-frame `new` in the V2 classes, SelfTests prefix for the smoke list). Air (AIR_FX1..3) stays as is.

## 6. Out of scope / not decided
- Spirit Dive armour and spirit meshes (`SpiritDiveWind3*`, `ModeArmorWindV2*`): separate systems, not touched.
- AIR_FX1..3 (Air Magic): different attribute, kept.
- Sound, particles (`wind_swirl_*`), gameplay of any page: unchanged.
- A real in-game look: nothing here has been seen in Minecraft; the preview tool is a headless rasteriser (no world lighting, no depth).

## 7. Decisions the owner has to make
1. Direction A, B, C, or mixed per shape (recommended: C for Tornado, Gale, Zephyr, Ring; A for Slash and Emperor, so the crescents are hard cel and the cages are ribbons).
2. Add the two optional canon shapes (`WIND_BEAM` for Spirit Storm palm beam, `WIND_DRILL` for Tornado Fang), or keep Spirit Storm as a tornado?
3. Treat "Swallow's Gale" / "Gust Lane" as mod-invented (no canon found) and drop the swallow sprites in V2?
4. "On twos" timing on or off by default.
