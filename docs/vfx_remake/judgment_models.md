# Sandalphon 3D models (3.21.0): the guillotine, the scales, the law tablet, the lance head and the sword of judgment

SANDALPHON, Lord of Judgment keeps its six `JUDGMENT_*` shapes (BULLET, SIGIL, EDICT, CLEAVE, FIELD, CROSS: nothing added, nothing reordered, the packet and the server are unchanged). What changes is how five of the big moments are DRAWN: real 3D meshes are built inside the existing `JudgmentLayer`, in the same quad budget, instead of one flat part. Same architecture as the Hercules pass of 3.20.0 (`HerculesModels`, `HerculesMeshDraw`, `HerculesModelsConfig`). Sources: the look comes from the owner's brief (pale gold, white, hairline black; a guillotine, balance scales, a tablet of law, a lance and a great sword); no canon page was needed for the visuals, so rule 10 (wiki research) did not apply.

## NEW, REPLACED, KEPT (rule 5)

| | What |
| --- | --- |
| NEW | `JudgmentModels` (pure geometry: 6 static meshes, the poser), `JudgmentMeshDraw` (the drawer), `JudgmentModelsConfig` (the CLIENT setting `visual.judgmentModels`, file `nusmp-judgment-client.toml`), `textures/particle/judgment_model_atlas.png` (512 x 512, 16 material cells) from `tools/gen_judgment_model_textures.py`, 11 preview scenes (`tools/vfx_preview/scenes.d/judgment_models.json`), `JudgmentModelsTest`. |
| REPLACED (only while the setting is on, the particle setting is not minimal and the thing is within 64 blocks) | the flat four-stroke blade loop of CLEAVE (the guillotine blade stands in for it; the slash arc stays). Dimmed or thinned, not removed: the flare on the head of the BULLET (55 % brightness), the glyph-band segments (24 / 14 instead of 30 / 20) and motes of EDICT, the motes of FIELD and CROSS. EDICT, FIELD and CROSS only gain the model next to their old parts. |
| KEPT | every 3.20.0 texture, every 3.20.0 draw call (they are the fallback and are still in the code), the slash arcs, rings, glow, cracks, clock faces, motes, durations, shape order, the server code, the Time Magic layer (untouched). With `judgmentModels = false` the layer draws exactly what 3.20.0 drew. SIGIL got no model (optional in the brief, not built). |

## The models (all in `JudgmentModels`, built once at class load)

| Model | What it is | Faces / parts | Quads kept from any view | Used by |
| --- | --- | --- | --- | --- |
| GUILLOTINE | a huge slanted-edge blade: trapezoid body with a bevel that glows, gold bolt-plates and straps, a weight at the back and a short tang | 26 / 1 | at most 18 (measured 16) | CLEAVE |
| SCALES | golden balance: central pillar with a ring, a beam, two pans hanging on chains (a feather on the left pan, a burning-heart diamond on the right) | 77 / 4 (base, beam, two pans) | at most 56 (measured 55) | FIELD |
| TABLET | a tall stone tablet with a pointed top, a scales crest and two panels of glowing glyph lines | 19 / 1 | at most 14 (measured 13) | EDICT |
| SHARD | a small dark-lined stone shard of the shattered tablet | 5 / 1 | at most 4 | EDICT |
| LANCE | a faceted spear head: diamond section, two barbs, a gold socket | 38 / 1 | at most 22 (measured 22) | BULLET |
| SWORD | a vertical great sword: long fullered blade, winged crossguard with a crest, grip and pommel | 54 / 1 | at most 36 (measured 35) | CROSS |

How it is read in game:

- CLEAVE: a huge angled blade (slanted edge, glowing bevel) swings down through the 130-degree sweep arc (reach 5.5), edge toward the target, fading in over the first 0.7 ticks and out with the effect; the old slash arc stays and the arc is drawn a little coarser while the blade is on.
- FIELD: golden scales hover above the field centre (bigger with the radius, 0.75x to 1.35x), rise and fade in over 14 ticks, tilt and settle (the right pan, with the burning heart, ends lower than the left one with the feather), breathe and turn slowly, and fade out with the field (it follows a creature when the field does). The floor, rings and clock faces are unchanged; the rim beams and motes stay (fewer motes while the scales are drawn).
- EDICT: a stone tablet rises out of the ground at the caster in 4.5 ticks, flashes white as the wave leaves (tick 5), trembles, and at tick 8.5 shatters into up to eight 3D shards that fly out and fall; the glyph bands are capped at 24 / 14 segments and the motes thinned while it is on.
- BULLET: the lance head rides the head of the 64-block needle, spinning slowly about the line, scaled up a little with the distance so it still reads far away; the flare behind it is dimmed so it shows.
- CROSS: during the charge (about 38 ticks) a great sword descends onto the target from above, hovering point-down; at the boom it plunges into the target and then stands in the ground while the cross glows and fades. The motes are fewer while it is drawn.

## Setting

`config/nusmp-judgment-client.toml`, `[visual] judgmentModels = true` (CLIENT: it only changes how things look). Off = the flat parts of 3.20.0, nothing else changes. Mirrored into the pure `JudgmentModels.enabled()` like `HerculesModels`. Even with it on, a model is replaced by the 3.20.0 part when the particle setting is at minimum (`ctx.detail < 0.5`), when it is further than 64 blocks from the camera, or when it does not fit what is left of the activation budget.

## How it is drawn (and why it is cheap)

The vertex buffer has no depth write, so a solid is drawn the way `SolidParts` does it: `Posed.prepare` drops the faces that look away from the camera (back-face culling by the winding; two-sided faces are flipped and mirrored instead), shades each from a fixed sun (`0.42 + 0.58 * lit`, lifted by the face's glow and by the heat of the moment), and sorts the rest far to near with an insertion sort of at most 100 indices. Everything is written into arrays inside the poser that are allocated once per layer (`JudgmentMeshDraw` holds one `Posed`); no collection, array or object is created per frame, per model or per face (checked by `JudgmentModelsTest` on the source). A model is skipped whole, never half-drawn, when `buf.hasBudget(visibleQuads * 4)` fails. A part skeleton (pivot, bend, spread and, new for this pass, roll about the part's own Z) drives the scales' beam and pans.

## Worst case per effect (measured in the preview over dense sweeps of 40 to 70 ages, close and wide cameras; engine cap 100 quads / 400 vertices)

| Effect | 3.20.0 | 3.21.0 (models on) | Where |
| --- | --- | --- | --- |
| BULLET | 37 | 49 | tick 1 to 3, lance 22 + needle, ghosts, muzzle |
| CLEAVE | 78 | 73 | blade 16 + a thinner arc |
| FIELD | 40 | 90 | scales 55 + floor, rings, clock faces, cracks, rim beams |
| EDICT | 81 | 71 | tablet 13 (or up to 4 shards of 4 quads) + capped bands |
| CROSS | 55 | 76 | sword 35 + the cross, rings, cracks, thinner motes |
| SIGIL | 17 | 17 | unchanged |

Every number is below the 100-quad activation cap (400 vertices; the highest measured is 360, FIELD). Every model asks `buf.hasBudget(...)` for all of its visible faces first, so even a pessimistic frame cannot push the buffer past its cap; the FIELD scales are the closest to it (90) and are the first thing that would be skipped (the flat field then draws alone).

## Rule 11 cost

Client only: no server work, no extra packets, nothing kept between frames (nothing to clean up on logout, death or dimension change). Per frame: at most one big model per effect (<= 56 quads, about 5000 float operations to pose and sort) plus at most eight 4-quad shards for one tick range, all culled beyond 64 blocks and under the particle setting; no allocation. The atlas is 512 x 512 (1 MB, 1.4 MB with mipmaps). Covered by `JudgmentModelsTest` (limits from 3000 random views of every model, UVs inside the cells, far-to-near order, the tilt of the scales, the switch default and registration, the atlas file, no per-frame allocation, the old calls and textures kept, the budget checks, the preview scenes and this doc) and the CI self-test of the existing VFX smoke list.

## Previews

`python tools/vfx_preview/preview.py judgment_cleave_close judgment_cleave_side judgment_field_close judgment_field_scales judgment_edict_close judgment_edict_shards judgment_bullet_close judgment_bullet_head judgment_cross_close judgment_cross_stand judgment_cross_guard` (contact sheets of six frames in `build/vfx_preview/`). The close-ups use a low camera near the model; the normal-camera scenes are still in `judgment.json`. Textures: `python tools/gen_judgment_model_textures.py` (imports the palette and the pseudo-script of `tools/gen_judgment_vfx_textures.py`, which is unchanged).

## Not seen in a game

The models were checked in the headless preview (a rasteriser of the same quads) and by tests; none was seen in a running game. Lighting against real blocks, bloom, the gold against a real sky, the size of the sword and the tablet next to a real player and whether the lance head is large enough at 40 blocks (the flat part is used beyond 64) should be judged in a game. The scales, the tablet and the sword turn toward the camera (a fixed yaw towards it, not toward the caster), which can read as a billboard when walking around them. The rising tablet is not clipped by terrain (it rises from a point below the floor; the vertex buffer has no depth test against the world, so on a slope part of it can show under the ground). The blade's tang is a little long and the steel slightly flat; the lance's gold barbs are the readable part against the white needle, its body disappears into the beam's core. With a minimal particle setting or an old save nothing changes.
