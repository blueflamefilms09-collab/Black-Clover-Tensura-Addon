# Solomon 3D models: the 72 Pillars, the Grimoire, the chain links, the signet ring, the crystals and the shards

The Solomon ultimate keeps its nine `SOLOMON_*` shapes (nothing added, nothing reordered; the packet, the server and the call doc `solomon_fx_calls.md` are unchanged). What changes is how six of the big moments are DRAWN: real 3D meshes are built inside the existing `SolomonLayer`, in the same quad budget, instead of flat sprites. Sources: the look comes from the owner's brief (a sorcerer king: indigo and violet, antique gold, parchment, the six element tints); no canon page was needed for the visuals, so rule 10 (wiki research) did not apply.

## NEW, REPLACED, KEPT (rule 5)

| | What |
| --- | --- |
| NEW | `SolomonModels` (pure geometry: 9 static meshes, the poser), `SolomonMeshDraw` (the drawer), `SolomonModelsConfig` (the CLIENT setting `visual.solomonModels`, file `nusmp-solomon-client.toml`), `textures/particle/solomon_model_atlas.png` (1024 x 1024, 39 material cells of 128 px) from `tools/gen_solomon_model_textures.py`, 23 preview scenes (`tools/vfx_preview/scenes.d/solomon_models.json`), `SolomonModelsTest`, this doc. |
| REPLACED (only while the setting is on and the thing is near) | the flat PAGE / BOOK sheets of COPY and the book and drifting pages of REALM (become the 3D Grimoire), the flat chain strip of CHAIN (becomes 3D links and a sigil head), the flat hexagram-only look of MARK (a signet ring is added over the target), the flat rune sprite on one stream in five of CONVERGENCE (becomes a rune crystal), the falling-pillar light of PILLAR (a ring of six real pillars is added; the old light is kept but capped to 3 falling + 3 landing and 4 scars while the ring is drawn), and ERASURE (falling glass shards are added). |
| KEPT | every older texture, every older draw call (they are the fallback), every duration, shape order, the 200-vertex budget of REALM, the pure `SolomonRules`, the server code, the Time Magic layer (untouched). With `solomonModels = false` the layer draws exactly what it drew before. |

## The models (all in `SolomonModels`, built once at class load)

| Model | What it is | Quads kept from any view | Used by |
| --- | --- | --- | --- |
| PILLAR | a fluted marble pillar, 19 faces: base ring with a glowing rune band, a stepped plinth, fluted shaft, a capital with a sigil band (a different element sigil per pillar, `cellShift`), a glowing inscription | at most 12 (measured 11) | PILLAR |
| GRIMOIRE | a floating open book, 21 faces on an 8-part skeleton: thick leather cover with a gold emblem, spine, a ribbon bookmark, parchment pages with glowing script, two turning page quads on each side (`grimoirePose(open, turnA, turnB, sway, ...)` closes and opens it) | at most 19 (measured 14) | COPY, REALM |
| LINK | one rune-engraved gold-bronze chain link, 16 faces (a rectangular wire loop with a violet rune inlay), alternate links are rolled by +-0.75 rad | at most 10 (measured 10) | CHAIN |
| HEAD | the faceted sigil head at the far end of the chain, 13 faces | at most 10 (measured 9) | CHAIN |
| RING | Solomon's signet ring, 47 faces: an octagonal thick gold band with rune bezels, a large faceted gem on a bezel, a flat rune halo (translucent) turning round it | at most 32 (measured 31) | MARK |
| CRYSTAL | a faceted rune crystal (two pyramids, 8 faces) | at most 5 | CONVERGENCE |
| SHARD_TRI, SHARD_QUAD, SHARD_LONG | a pane of glass of the erased magic circle (5, 6 and 5 faces: two glass faces and a bright edge) | at most 4 each | ERASURE |

How it is read in game:

- PILLAR (the 72 Pillars on one target): six real pillars stand in a ring of radius 4.2 round the ground point, rising from under the floor one after another (5 ticks apart, 9 blocks of rise, from tick 6), each with a different element tint on its capital and a different sigil cell; they sink back over the last 16 ticks. The old falling streak, landing burst, column of light and scars stay. 3 of 6 pillars at distance, `ctx.seg(6, 3)`.
- COPY: when the spell has been read, a floating open Grimoire opens beside the caster (ticks 8 to 13), two pages turn (12 to 21), it closes and sinks into the caster (21 to 26). REALM: a Grimoire hovers 1.9 blocks above the caster's crown, its pages flutter; the drifting parchment sheets are skipped when it is drawn. The Grimoire turns to face the camera (`look`), leaning 0.42 rad.
- CHAIN: links grow out of the caster along the line (`reach`), alternate in orientation, pulse hot when each circle fires, take the element tint of the nearest gate; the faceted sigil head opens at the far end (len + 0.3).
- MARK: a gold signet ring with a large violet gem spins over the marked target at 1.5 times the size, the gem on top; its heat flashes when the mark locks.
- CONVERGENCE: one stream in five carries a rune crystal in the colour of its element.
- ERASURE: up to eight glass shards fall out of the sky inside the dome and shatter on the floor (13 ticks each, staggered over 7).

## Setting

`config/nusmp-solomon-client.toml`, `[visual] solomonModels = true` (CLIENT: it only changes how things look). Off = the flat sprites of the older versions. Mirrored into the pure `SolomonModels.enabled()`. Even with it on, a model is replaced by the flat sprite when the particle setting is at minimum (`ctx.detail < 0.5`), when it is further than 56 blocks (72 for the pillars) from the camera, or when it does not fit what is left of the activation budget.

## How it is drawn (and why it is cheap)

The vertex buffer has no depth write, so a solid is drawn like `SolidParts` does it: `Posed.prepare` drops the faces that look away from the camera (back-face culling by the winding, 0 badly wound faces in every mesh, checked by the test), shades each from a fixed sun (`0.38 + 0.62 * lit`, lifted by the face's glow and by the heat of the moment) and sorts the rest far to near with an insertion sort of at most 32 indices. Everything is written into arrays inside the poser, allocated once per layer (`SolomonMeshDraw` holds one `Posed` and four `Vector3f`); no collection, array or object is created per frame, per model or per face. A model is skipped whole, never half-drawn, when `buf.hasBudget(visibleQuads * 4)` fails. The pillar ring is sorted far to near in 8 slots by a tiny key array allocated once.

## Worst case per effect (measured in the preview at EVERY tick of the effect, close camera; engine cap 100 quads / 400 vertices)

| Effect | Before | Now | Where |
| --- | --- | --- | --- |
| COPY | 32 | 41 | grimoire open with turning pages (14) + the streams |
| CONVERGENCE | 71 | 81 | tick 17 to 20, up to three crystals (5 each) + streams |
| REALM | 39 (two overlap for 20 ticks: 78) | 42 (84) | the grimoire (14) replaces the drifting pages; each instance keeps its own 200-vertex budget |
| CHAIN | 37 | 75 | 4 links (10 each at most) + head (9) + circles |
| ERASURE | 50 | 72 | up to 8 shards (3 or 4 each) |
| MARK | 30 | 53 | the signet ring (up to 31) + the seal |
| PILLAR | 34 (18 pillars) / 90 (72 pillars) | 88 / 99 | six pillars (11 each) + the old light capped to 3 + 3 + 4 scars; the buffer stops at 100 |
| SKIN | 23 | 23 | unchanged |
| ABSORB | 16 | 16 | unchanged |

Every number is below the 100-quad activation cap; every model asks `buf.hasBudget(...)` first, so even a pessimistic frame cannot push the buffer past its cap (the PILLAR with 72 pillars sits at 99 of 100: the rest is simply not drawn).

## Rule 11 cost

Client only: no server work, no extra packets (none of the numbers the pictures use are new), nothing kept between frames (nothing to clean up on logout, death or dimension change). Per frame and effect: at most six pillars (<= 72 quads) or one Grimoire (<= 19) or four links and a head (<= 50) or one ring (<= 32) or three crystals (<= 15) or eight shards (<= 32); each is posed with about 100 float operations per face and sorted with an insertion sort of at most 32 indices; all are culled beyond 56 / 72 blocks and by the particle setting. The atlas is 1024 x 1024 (4 MB, 5.5 MB with mipmaps). Covered by `SolomonModelsTest` (limits from 3000 random views of every mesh, UVs inside the cells, far-to-near order, the switch default and registration, the atlas file, no per-frame allocation, the old calls and textures kept, the budget checks, the preview scenes and this doc) and `SolomonShapesTest`.

## Previews

`python tools/vfx_preview/preview.py solomon_models_pillar_close solomon_models_pillar_detail solomon_models_pillar solomon_models_pillar_72 solomon_models_pillar_far solomon_models_copy_close solomon_models_copy solomon_models_realm_close solomon_models_realm solomon_models_realm_far solomon_models_chain_close solomon_models_chain_head solomon_models_chain solomon_models_chain_back solomon_models_chain_long solomon_models_mark_close solomon_models_mark_ring solomon_models_mark solomon_models_convergence_close solomon_models_convergence solomon_models_erasure_close solomon_models_erasure` (contact sheets in `build/vfx_preview/`). Textures: `python tools/gen_solomon_model_textures.py` (imports the palette of `tools/gen_solomon_vfx_textures.py`, which is unchanged). Note: `tools/vfx_preview/preview.py` copies the rules of the Solomon layer from a list and does not list `HerculesRules.java`, which `JudgmentRules` needs; add it to that list to build the Solomon scenes.

## Not seen in a game

The models were checked in the headless preview (a rasteriser of the same quads) and by tests; none was seen in a running game. Lighting against real blocks, bloom, the colour of the marble against a real floor (the preview is not depth-tested against blocks), the size of the Grimoire and the signet ring at 20 to 50 blocks, and whether the pillar ring clips into a real wall or slope (it is placed at the ground point of the effect) should be judged in a game. Known weaknesses: the chain links are rectangular loops rather than round rings; the ring bezel reads flat from some angles; the sigil panels of the pillars are soft at 128 px; the erasure shards are thin slivers when seen edge-on.
