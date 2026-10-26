# Hercules 3D models (3.20.0): the hand, the fist, the boulder, the rocks and the laurel

The Hercules ultimate keeps its ten `HERCULES_*` shapes (nothing added, nothing reordered, the packet and the server are unchanged except the two numbers named below). What changes is how five of the big moments are DRAWN: real 3D meshes are built inside the existing `HerculesLayer`, in the same quad budget, instead of one flat sprite. Sources: the look comes from the owner's brief (clenched fist, laurel wreath, bronze Titan); no canon page was needed for the visuals, so rule 10 (wiki research) did not apply.

## NEW, REPLACED, KEPT (rule 5)

| | What |
| --- | --- |
| NEW | `HerculesModels` (pure geometry: 6 static meshes, the poser), `HerculesMeshDraw` (the drawer), `HerculesModelsConfig` (the CLIENT setting `visual.herculesModels`, file `nusmp-hercules-client.toml`), `textures/particle/hercules_model_atlas.png` (512 x 512, 16 material cells) and `hercules_model_fissure.png` (64 x 256) from `tools/gen_hercules_model_textures.py`, ground fissures (LEAP, TREMOR, ROAR, SHOCKWAVE), the boulder (THROW power 2), the aftershock variant (TREMOR power <= 10), 7 preview scenes, `HerculesModelsTest`. |
| REPLACED (only while the setting is on and the thing is near) | the flat HAND and FIST sprites of GRIP and STRIKE, the flat LAUREL wreath plane and RING plane over the head and the flat LAUREL plane on the floor of INVICTUS, the flat stone sprites ("slabs") of TREMOR, THROW, LEAP and SHOCKWAVE (the first up to eight / four / four / three of them become 3D rock chunks). |
| KEPT | every 3.19.0 texture, every 3.19.0 draw call (they are the fallback), the three chains, rings, flashes, cracks, web, dust and embers, the hit-stop, every duration, shape order, the 200-vertex budget of INVICTUS, the server code, the Time Magic layer (untouched). With `herculesModels = false` the layer draws exactly what 3.19.0 drew. |

## The models (all in `HerculesModels`, built once at class load)

| Model | What it is | Quads kept from any view | Used by |
| --- | --- | --- | --- |
| HAND | the Titan's hand, 58 faces on an 18-part skeleton: armoured cuff with a chevron laurel band, a back-of-hand plate with a gold inlay, four ridged fingers of three segments each, the thumb; the fingers curl (`handPose(close, splay, ...)`) from fanned open to a fist | at most 40 (measured 38 open, 36 closed) | GRIP |
| FIST | a clenched fist, 41 faces: back-of-hand plate, flared wrist guard with a laurel band and two leaves, four curled fingers with knuckle bosses, the thumb across them, a burning wrist | at most 30 (measured 29) | STRIKE |
| BOULDER | a faceted rock of 24 faces with ember-cracked patches | at most 16 (measured 15) | THROW with power 2 |
| CHUNK | a small slab of ground, 6 faces | at most 4 | TREMOR, THROW, LEAP, SHOCKWAVE, ROAR |
| CROWN | a laurel crown: a bronze band, two branches of five leaves, a crimson tie | at most 24 (measured 19) | INVICTUS |
| RING | a flat laurel ring of 14 leaves | 14 (limit 16) | INVICTUS (floor) |

How it is read in game:

- GRIP: a huge bronze hand, fingers pointing from the caster to the target, grows in 7 ticks out of the caster's side with the fingers fanned, holds a little above the target, then curls into a fist that lands on it (ticks 8 to 13) with the existing flash, ring and web of cracks (the flash and web are softened while the 3D fist is drawn so it stays readable) and the three chains.
- STRIKE: a clenched fist forms behind the shoulder, trembles (a small roll) for 18 ticks while it glows brighter, crosses the gap in 2 ticks with the smear and speed lines, and holds on the impact frame (hit-stop unchanged); the white flash is a little softer while it is drawn so the fist shows through the impact.
- THROW: the server sends `power = 2` when a chunk of ground is hurled and `power = 1` when a creature is. With power 2 a spinning boulder rides the head of the bronze streak along the arc and vanishes into the landing; at the landing up to four 3D chunks fly out (plus three flat slabs). With power 1 nothing changes (the creature flies by itself) except the landing chunks.
- INVICTUS: a 3D laurel crown turns over the head (leaning toward the camera), a 3D laurel ring turns on the floor; the flame, the wreath-with-fist icon behind the body and the embers stay. When the crown is drawn one flame tongue less is drawn, to stay in the 50-quad budget.
- TREMOR: up to eight 3D rock chunks lifted and falling back, up to six ground fissures (glowing split with dark lips, two quads each) that open with the cracks and cool to a dark red scar. The AFTERSHOCK (power <= 10, the 14-tick echo the server sends) is the lighter variant: half the flash, one ring and wave, one layer of cracks, no column of light, three chunks and three fissures, fewer dust puffs and a camera shake of 0.45 for 10 ticks (the full tremor shakes with 0.8 to 2.6 for 18).
- LEAP: up to four 3D chunks and three fissures at the take-off. ROAR: up to four fissures round the feet and three chunks thrown up under the dome. SHOCKWAVE: up to three small chunks and two fissures. BODY and MOMENTUM are unchanged (they have no solid to model; the budget is better spent elsewhere).

## Setting

`config/nusmp-hercules-client.toml`, `[visual] herculesModels = true` (CLIENT: it only changes how things look). Off = the flat sprites of 3.19.0, nothing else changes. Mirrored into the pure `HerculesModels.enabled()` like `FlatProps`. Even with it on, a model is replaced by the 3.19.0 sprite when the particle setting is at minimum, when it is further than 64 blocks (rocks 44, the wreath 36) from the camera, or when it does not fit what is left of the activation budget.

## How it is drawn (and why it is cheap)

The vertex buffer has no depth write, so a solid is drawn like `SolidParts` does it: `Posed.prepare` drops the faces that look away from the camera (back-face culling by the winding), shades each from a fixed sun (`0.38 + 0.62 * lit`, lifted by the face's glow and by the heat of the moment), and sorts the rest far to near with an insertion sort of at most 40 indices. Everything is written into arrays inside the poser that are allocated once per layer (`HerculesMeshDraw` holds one `Posed` and four `Vector3f`); no collection, array or object is created per frame, per model or per face (checked by `HerculesModelsTest` on the source). A model is skipped whole, never half-drawn, when `buf.hasBudget(visibleQuads * 4)` fails.

## Worst case per effect (measured in the preview at every half tick, Invictus every tick, over the extreme parameters; engine cap 100 quads / 400 vertices)

| Effect | 3.19.0 | 3.20.0 | Where |
| --- | --- | --- | --- |
| LEAP | 33 | 45 | take-off, 4 chunks + 3 fissures |
| TREMOR | 50 | 76 (radius 14 and 24; 34 for the aftershock and any radius <= 10) | tick 8 to 10, 8 chunks + 6 fissures |
| INVICTUS | 33 (65 overlapping) | 50 (100 overlapping, each is held to its own 200-vertex budget) | crown 19 + ring 14 + the old parts, the optional embers and leaves drop out first |
| GRIP | 41 | 70 (target 3, 10, 24, 40 blocks) | tick 11 to 16, hand 36 to 38 + chains |
| THROW | 64 | 70 (6, 20, 40 blocks, power 1 and 2) | landing, 4 chunks |
| ROAR | 46 | 63 (radius 6, 18, 30) | tick 10 to 12, 4 fissures + 3 chunks |
| STRIKE | 54 | 70 (4 and 8 blocks) | tick 24, fist 29 at the gather, the web and rings after the blow |
| SHOCKWAVE | 15 | 24 | tick 3 |
| BODY | 20 | 20 | unchanged |
| MOMENTUM | 17 / 26 / 36 | 17 / 26 / 36 | unchanged |

Every number is below the 100-quad activation cap; every optional group still asks `buf.hasBudget(...)` first, and every model asks for room for all of its visible faces, so even a pessimistic frame cannot push the buffer past its cap. Frames of two overlapping INVICTUS instances are bounded by 2 x 50 = 100 quads.

## Rule 11 cost

Client only: no server work, no extra packets, nothing kept between frames (nothing to clean up on logout, death or dimension change). The only server-side facts the pictures use are two numbers that already travel in the existing `VfxSpawn`: `power = 2` (THROW, a hurled chunk of ground) and `power = 9` (TREMOR, the aftershock); no packet was added. Per frame: at most one hand or fist (<= 40 / 30 quads, about 3000 float operations to pose and sort), one boulder or crown and ring, and at most eight chunks per effect (<= 32 quads), all culled beyond 64 / 44 / 36 blocks; fissures are two quads each, at most six. The atlas is 512 x 512 (1 MB, 1.4 MB with mipmaps), the fissure strip 64 x 256. Covered by `HerculesModelsTest` (limits from 3000 random views of every model, UVs inside the cells, far-to-near order, the pose of the hand, the switch default and registration, the atlas and fissure files, no per-frame allocation, the old sprite calls and textures kept, the budget checks, the preview scenes and this doc) and `HerculesShapesTest`.

## Previews

`python tools/vfx_preview/preview.py hercules_grip_close hercules_strike_close hercules_strike_back hercules_throw_chunk hercules_throw_close hercules_invictus_close hercules_tremor_close hercules_aftershock` (contact sheets in `build/vfx_preview/`). The close-ups use a low camera near the model; `hercules_throw_chunk` is power 2; `hercules_aftershock` is power 9. Textures: `python tools/gen_hercules_model_textures.py` (imports the palette of `tools/gen_hercules_vfx_textures.py`, which is unchanged).

## Not seen in a game

The models were checked in the headless preview (a rasteriser of the same quads) and by tests; none was seen in a running game. Lighting against real blocks, bloom, the colour of the stone against a real floor, the roll of the fist against the real body of the caster and whether the hand is large enough at 40 blocks (the sprite is used beyond 64) should be judged in a game. The aftershock and the boulder depend on the server sending `power = 9` and `power = 2`; with the old numbers they simply draw the full tremor and no boulder.
