# HD outfits for the Tier List bosses (3.38.0)

Owner request: "make the outfits HD". The Tier List bosses wear the vanilla player model with a 64x64 skin. This change lets a skin (and its glow map) be **256x256** with the
**same UV layout**, so every model pixel becomes a 4x4 block of texels, and adds the painting tools to draw smooth, detailed outfits at that size. Two characters are done as pilots
(Charlos, Mr. 2 Bon Clay); the other characters stay 64x64 until their module is written (the kit accepts both).

New or replacement (CLAUDE.md rule 5): the HD **tools** and **tests** are new; the HD **skins** of `charlos` and `mr_2_bon_clay` replace their 64x64 PNGs (owner asked for HD). The
64x64 versions are not lost: `tools/tier_bosses/remake/<id>.py` stays, and `gen_tier_looks.py --size 64` writes them again. Bon Clay also gets a glow map (the sash gem), which is new.

## 1. Engine findings: does anything depend on 64x64?

Checked in the Java sources of the player-model kit and its tests.

* **Rendering does not.** The vanilla `PlayerModel` is baked with a 64x64 `LayerDefinition`, so every UV is `u / 64`. A 128 or 256 PNG with the same layout is simply sampled finer. The kit
  never reads the GPU texture's size. The overlay layers (hat, jacket, sleeve, pants) use the cutout render type, the glow map is drawn additively; neither cares about the size.
* **One hard check did:** `PlayerModelAudit` (also run in the CI self-test `render.playermodel`) required skins and glow maps to be exactly 64x64. That was the only place.
* **Everything else is size-agnostic:** `SkinPath`, `PlayerLookTables`, the look loader, `TextureAlpha` (works on whatever image it gets), `TextureAudit`, `AdditiveTypes`, `PlayerLod` (hair and overlay
  are dropped beyond 24 blocks, which is why the HD modules draw the crisp things on **both** layers). `DungeonAudit` skips skins that are not 64 and the `dg_*` archetype skins stay 64.

Changes (the minimum):

| File | Change |
|---|---|
| `client/playermodel/SkinPath.java` | `BASE_SIZE = 64`, `MAX_SIZE = 1024`, `validSkinSize(w, h)` (square, a power-of-two multiple of 64, at most 1024), `texelsPerPixel(w)` |
| `client/playermodel/PlayerModelAudit.java` | `lookProblems` reads the skin's size with `validSkinSize` (new private `skinPng`), and requires the glow map to have **the same size as its skin** |
| `client/playermodel/PlayerModelSelfTests.java` | doc comment only |
| `tools/playermodel/pmrender.py` | the review renderer took the texture size as 64: `cube_faces(..., uv_ref=None)`, `player_faces` passes `uv_ref=(64, 64)`. Backward compatible (a 64 skin renders as before) |

Tests:

* `PlayerModelDataTest`: `aSkinMayBe64Or128Or256ButSquareAndAPowerOfTwoMultipleOf64`, `everyLookSkinAndItsGlowMapFollowTheSizeRule` (all looks of all tables).
* `TierBossesTest`: `everyTierSkinAndGlowMapIsSquareAMultipleOf64AndTheGlowMatchesItsSkin` (equal width and height, a multiple of 64, the glow map as big as its skin, and the head, body and leg
  rectangles scaled by `k = width / 64` are painted). The anime-style-v2 test is scale-aware.
* `LightYagamiDataTest`: size- and scale-aware reads.
* `tools/tier_bosses/hd/test_hdlib.py` (16 tests, `python3 -B tools/tier_bosses/hd/test_hdlib.py`, numpy and Pillow only): the same size rule on the shipped PNGs, Canvas sizes, no paint across a seam,
  binary overlay alpha, opaque base, premultiplied glow, downsample, the generic bridge, the tuple contract of every `hd/<id>.py` (256, glow as big as the skin, no hole in a base island), HD tones
  vs the 64 blueprint, the `--size` plumbing, and that the 64 remake modules are still there.

## 2. How it works

```
gen_tier_looks.py <id>
  1. tools/tier_bosses/hd/<id>.py    paint(spec, cid, masks) -> (skin 256, glow 256 or None, hair id, hair rgb, slim)
  2. tools/tier_bosses/remake/<id>.py   the 64x64 hand-painted remake (same tuple, 64)
  3. the generic anime painter          (64)
```

* **`--size 256`** (default): HD modules are used as drawn. **`--size 128`**: they are averaged down by 2 (`hd_core.downsample`, exact blocks, alpha binary). **`--size 64`**: the HD modules
  are skipped, so the 64x64 version stays reachable. `--flat`, `--plain`, `--generic` behave as before (they never use HD modules).
* **`--auto-hd`**: also runs the generic bridge (section 4) over every id without an HD module (smooth 256 upscale, outlines, material detail, no character art). Off by default; ids without
  an HD module stay 64x64.
* An HD module normally starts from its own 64x64 remake as a **blueprint**: `H.blueprint(cid, spec, masks)` runs `remake/<id>.py`. The bridge `from64` upscales it smoothly (so layout and colour
  blocking stay the owner-approved ones), and the module then **redraws the things that must be crisp** (face, trims, gems, frills) at 4x detail. Anything the module does not redraw still
  looks better than a plain upscale.
* Output: `assets/nusmp/textures/entity/playermodel/<id>.png` (+ `<id>_glow.png`), and `looks_tier.json` (`glow: true` when there is a glow map; the hair id comes from the module).

Run: `python3 -B tools/tier_bosses/gen_tier_looks.py charlos mr_2_bon_clay` (about 1.2 s per character, numpy and Pillow).
Preview: `python3 -B tools/tier_bosses/preview_tier.py out.png --trio charlos mr_2_bon_clay` (front, back, three-quarter bust; `--close`, `--back`, `--aa N` supersample, default 2 for an HD skin;
`--skin-dir DIR` reads the skins from another folder, e.g. the 64 version, for a side-by-side).

### The library (`tools/tier_bosses/hd/`)

| File | Role |
|---|---|
| `hdlib.py` | the one import for a character module: re-exports everything below; `blueprint(cid, spec, masks)`, `hd_module_path(cid)` |
| `hd_core.py` | `Canvas`, `Face` (one UV rectangle), signed-distance masks, paints (`lin`, `rad`), compositing, colour helpers, `downsample`, `downsample_glow` |
| `hd_detail.py` | detail helpers: `bevel`, `folds`, `sheen`, `scallops`, `stitch`, `fringe`, `stud`, `gem`, `eye`, `brow`, `blush`, `mouth_smile`, `nose`, `trim_line`, `trim_band`, `skin_face`, `cloth`, `heart_pts`, `sparkle`, `metal_paint`, `shadow_under`, `rim`, `weave` |
| `hd_bridge.py` | `from64`, `hd_paint`, `Materials`, `family`, `near`, `exact` |
| `charlos.py`, `mr_2_bon_clay.py` | the two pilots |
| `test_hdlib.py` | the tests |

The Canvas mirrors `tools/playermodel/skin_lib.py`: same parts, same rectangles (head `0,0` hat `32,0`; body `16,16` jacket `16,32`; right arm `40,16` sleeve `40,32`; left arm `32,48` sleeve
`48,48`; right leg `0,16` pants `0,32`; left leg `16,48` pants `0,48`), slim arms 3 wide, faces `front back west east top bottom`. Coordinates are **model pixels** (floats), so a character module is
written the same way at any size. Everything drawn is clipped to its island, so colour never crosses a seam. The overlay layers are exported fully opaque or fully transparent (the game draws them
as a cutout), the base layer fully opaque, the glow map premultiplied.

### The generic bridge (`from64`, `hd_paint`)

(a) Region-aware smooth upscale: each colour of an island is a class, the one-hot class maps are interpolated with a Catmull-Rom filter and the strongest class wins per texel, so stair steps
become smooth curves and no colour that was not in the blueprint appears. (b) A same-material blur (tone steps of a ramp become cloth gradients) with crisp edges between materials, and thin
outlines in a darker cooler hue on the darker side of every material edge. (c) Per-material detail: cloth folds and weave, a bevel and specular for metal, roundness for skin, strands for hair,
facets for gems, plus a shadow the second layer throws on the layer below.

## 3. HOW TO PAINT A CHARACTER IN HD (hand-over for the next agents: follow this exactly)

**Files.** Create `tools/tier_bosses/hd/<id>.py` (the same id as the sheet in `tier_specs.py` and the remake). Do not edit `hd_core.py`, `hd_detail.py`, `hd_bridge.py` or `hdlib.py` (shared);
if you need a helper, write it inside your own module. Never remove the remake module.

**Skeleton** (this is the whole contract):

```python
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import hdlib as H

def paint(spec, cid, masks):
    img64, glow64, hair, hrgb, slim = H.blueprint(cid, spec, masks)      # the 64x64 remake: colours, layout, hair id, hair rgb, slim
    cv = H.Canvas(256, slim)
    mats = {"metal": [H.family("#e3b232", 20, 0.5)], "gem": [H.near("#ff6a78", 0.2)]}   # name the materials by colour; everything else is cloth
    H.from64(cv, img64, None, mats, skip=lambda part, name, ov: part == "head" and name == "front" and not ov, soft=0.75)
    head(cv, hrgb); body(cv); arms(cv); legs(cv)                         # your redraws at 4x detail
    skin, glow = cv.finish()                                             # PIL RGBA images; glow is None when nothing glows
    return skin, glow, hair, hrgb, slim                                   # the same 5-tuple as the remake modules
```

Hair: return the hair id you want (a style of `hair_styles.json`); Bon Clay does `if "anime_afro_wide" in masks: hair = "anime_afro_wide"`.

**Step by step**

1. Read the remake module and its entry in `docs/bosses/remake_*.md`: the colour blocking and the outfit stay. HD adds detail, it does not redesign the character.
2. `from64(...)` with `skip=` for the islands you redraw completely (usually the front of the head; `skip` takes `(part, face_name, overlay)`). `materials` names matchers: `family(hex, hue_tol,
   sat_tol)` (every tone of a hue), `near(hex, dist)` (colours near one colour), `exact(*hexes)`. Names that have a pass: `cloth` (the default), `metal`, `skin`, `hair`, `gem`.
3. Get a face with `cv.face(part, name, overlay)`; parts `head body arm_r arm_l leg_r leg_l`, faces `front back west east top bottom`. **Draw the crisp things on BOTH layers**: the game drops the
   second layer (and the hair) beyond 24 blocks (`PlayerLod`), so the base layer must look right alone. Pilot helper:
   ```python
   def lay(cv, part, name):
       return (cv.face(part, name, False), cv.face(part, name, True))
   for f in lay(cv, "body", "front"):
       if f.overlay:
           f.clear(f.m_band(2.5, 7.0))      # the bodice shows between the boa and the sash
       sash_and_tutu(f); boa(f)
   ```
4. Shapes are masks: `f.m_poly(pts)`, `m_ellipse(cx, cy, rx, ry, rot)`, `m_rect`, `m_line(p0, p1, w0, w1)`, `m_polyline`, `m_ring`, `m_band(y0, y1)`, combined with `*`, `H.union`, `H.minus`. Paint
   them with `f.fill(mask, paint, a=1.0, mode="over"|"erase"|"mul"|"add"|"under"|"replace", glow=None|True|colour)` or the shortcuts `f.poly / ellipse / rect / line / polyline / clear / shade`.
   A paint is `'#rrggbb'`, `H.lin((x0, y0), (x1, y1), [(0, c0), (1, c1)])`, `H.rad((cx, cy), r, stops)` or a function. Colour helpers: `H.tone(color, k)` (k < 1 shadow, cooler; k > 1 light,
   warmer), `H.ramp`, `H.mix`.
5. Use the detail helpers instead of hand-rolling them. Examples from the pilots:
   * Face (Charlos): `H.skin_face(base, SKIN, hair=H.tone(hair, 0.92), hairline=1.9, wave=0.45)`, `H.brow(base, (3.4, 3.35), (1.0, 3.0), "#a67a18", 0.26, 0.12, arch=0.10)`,
     `H.eye(base, 2.3, 4.95, 2.55, 2.25, iris, lash="#3a1c0c", tilt=0.10)` (`iris` is dark top, middle, light bottom), `H.blush(...)`, `H.nose(...)`, `H.mouth_smile(base, 4.2, 7.15, 2.1, color=..., lip=...)`.
   * Metal: `H.trim_band(f, 2.0, 2.95, GOLD)`, `H.trim_line(f, pts, GOLD, width=0.62)`, `H.stud(f, x, y, 0.16, GOLD)`, `H.gem(f, 4.0, 2.48, 0.46, "#e0183c", setting=GOLD, glow=True)` (glow=True also
     lights the glow map), `H.stitch(f, pts, "#fff0b8", width=0.06, dash=(0.3, 0.2))`, `H.fringe(...)`.
   * Cloth: `H.cloth(f, mask, WHITE, top=1.02, bottom=0.90, fold_xs=[...])`, `H.folds`, `H.sheen(f, mask, [xs], ...)`, `H.shadow_under(f, y, width, strength)`, and frills and feathers with
     `H.scallops(f, mask, y0, rows=2, cell=1.6, depth=1.5, base=..., tip="#ffffff", edge=..., edge_a=0.55, shadow=0.26)`.
   * Bon Clay's heart: `H.heart_pts(cx, cy, w)` into `f.m_poly`, then `H.bevel(f, m, depth=0.32, gain=0.9, spec=0.5, spec_pow=10)` and `H.sparkle(...)`.
6. Glow: draw with `glow=True` (or a colour) on gems, studs, eyes, magic marks. Only what really glows. `finish()` returns `glow = None` when nothing does (then no `_glow.png` is written).
7. Run `python3 -B tools/tier_bosses/gen_tier_looks.py <id>` and check the preview: `python3 -B tools/tier_bosses/preview_tier.py out.png --trio <id>` and `--skin-dir` of a 64 export for the comparison.
   **Iterate at least 4 rounds** and look at the front, the back, the sides and the close-up; compare with the owner references and with the 64 version. Fix what reads worse, not only what is new.
8. Run `python3 -B tools/tier_bosses/hd/test_hdlib.py` (it picks up your module by itself) and the Java test classes (`TierBossesTest`, `PlayerModelDataTest`, `LightYagamiDataTest`).

**Rules and lessons from the pilots**

* Units are model pixels, never texels. A cell, a line width or an offset of 1.0 is one model pixel (4 texels).
* **Do not draw at 1-pixel period.** Frills, feathers and tulle with a 1 px cell read as a lattice or a checker from a few blocks away. Use `cell` 1.3 to 2.0 and few rows (Charlos' collar:
  `cell=1.6, rows=2`; Bon Clay's boa: `cell=1.34`, tutu `cell=2.0`). Keep the edge line alpha low (`edge_a` 0.3 to 0.55) and the cast shadow light (`shadow` 0.14 to 0.26) on white cloth, or it turns grey.
* Satin, gloves and tights read best as one clean gradient and a sheen, with no folds (`fold_xs=[]`).
* The overlay must be a clean cutout: `finish()` thresholds at 0.5. Do not rely on a half-transparent overlay.
* The base layer of every island must be fully painted (the test fails on a hole). `from64` paints all of them; only `clear()` the overlay.
* Slim arms are 3 model pixels wide: use `f.w` (and `f.mx(x)` to mirror), not 4.
* Keep the character the owner-approved colour scheme; take hair colour from `hrgb` so the hairline matches the kit hair.
* Original art only. Look from memory/canon; no traced images.
* Cost: stay in these helpers (numpy, about 1 to 2 seconds per character). Nothing is random; the output is deterministic.

## 4. Sizes, bounds and cost (CLAUDE.md rules 9 and 11)

* **Server:** nothing changes. No packet, no tick work, no entity; the skins are client resource-pack assets and the look table is the same JSON. Dedicated servers never load them.
* **Texture memory (client):** 256x256 RGBA is 256 KB per skin, plus 256 KB when there is a glow map (64x64 is 16 KB). Worst case one HD boss is 0.5 MB of GPU memory; the textures are loaded
  by the vanilla texture manager on first use (as far as the kit's loader shows; not measured in a game), so only the bosses a player actually meets count (a typical session: under 10, so under 5 MB). If every one of ~200 Tier List bosses were HD and all
  seen in one session: about 100 MB, which is why the unpainted ones stay 64 and `--size 128` exists (a quarter of the memory).
* **Draw cost:** identical geometry (same quads, same draw calls); only texture sampling changes. NEAREST filtering without mipmaps (the kit's choice) means HD detail shows when near, and a far
  HD character may shimmer slightly more than its 64 version; `PlayerLod` already drops the overlay and hair beyond 24 blocks.
* **Jar size:** an HD PNG is about 25 to 55 KB (Charlos 36 KB + 3 KB glow, Bon Clay 27 KB + 1 KB glow); ~200 HD characters would add about 8 MB.
* **Tool cost:** about 1.2 s per character, under 100 MB of RAM.
* **Cap:** `SkinPath.MAX_SIZE = 1024` is a sanity limit; the rule is a power-of-two multiple of 64.

## 5. What is NOT seen in a game

* No in-game screenshot was taken (no client could run here). All previews are from the repo's software renderer (`tools/playermodel/pmrender.py`): flat shading, no real light, no shaders, and a
  different anti-aliasing than the game. How the GPU filters a 256 skin in the game (NEAREST, no mipmaps, shimmer at distance) is reasoned, not observed.
* `render.playermodel` (the CI self-test) and the Java build were not run here (gradle is not available); the changed Java classes were compiled and the unit tests run with the stubbed quick runner.
* The hair is another agent's work: the previews show whatever hair the table had at the time (Charlos still has the spiky `anime_short_open`, Bon Clay shows `anime_afro_wide`).
* The look of both characters is from memory and the owner's references (see `remake_a.md`); no wiki page was fetched for this task (English wiki first, Japanese as a fallback is the rule for
  canon research; nothing was looked up, so nothing could be fetched or failed).

## 6. Files

New: `tools/tier_bosses/hd/{hdlib,hd_core,hd_detail,hd_bridge,charlos,mr_2_bon_clay,test_hdlib}.py`, `docs/bosses/hd_outfits.md`, `docs/previews/tier_hd/*`,
`assets/nusmp/textures/entity/playermodel/mr_2_bon_clay_glow.png`.
Changed: `gen_tier_looks.py` (`load_hd`, `--size`, `--auto-hd`), `preview_tier.py` (`--trio`, `--aa`, `--skin-dir`), `tools/playermodel/pmrender.py`, `SkinPath.java`, `PlayerModelAudit.java`,
`PlayerModelSelfTests.java` (comment), the three Java test classes, the PNGs `charlos.png`, `charlos_glow.png`, `mr_2_bon_clay.png` (now 256x256), `looks_tier.json` (Bon Clay: `glow`, hair).
Kept: every `tools/tier_bosses/remake/*.py`, all other skins (64x64).
Not touched: the hair files, the Time Magic VFX, the version number (the release is the integrator's).
