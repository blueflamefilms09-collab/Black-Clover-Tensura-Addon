# Israfil 3D models (3.21.0): the Grand Trumpet, the weight plate, the maze walls, the reality shards and the coordinate gates

The Israfil ultimate (Lord of Trials) keeps its six `TRIAL_*` shapes (nothing added, nothing reordered; the packet and the server are unchanged). What changes is how five of its big moments are DRAWN: real 3D meshes are built in
`IsrafilModels` (pure geometry, no Minecraft classes), posed and drawn by `IsrafilMeshDraw` and called from `TrialLayer`. They sit next to the old flat parts: with the setting on and the effect near, the model is drawn and the flat part it
replaces is skipped; with the setting off, far away, or with no room in the budget, `TrialLayer` draws exactly what it drew in 3.20.0.

## NEW, REPLACED, KEPT (rule 5)

| | What |
| --- | --- |
| NEW | `IsrafilModels` (8 static meshes, the poser), `IsrafilMeshDraw` (the drawer), `IsrafilModelsConfig` (the CLIENT setting `visual.israfilModels`, file `nusmp-israfil-client.toml`), `textures/particle/israfil_model_atlas.png` (512 x 512, 16 cells, from `tools/gen_israfil_model_textures.py`), the preview scenes `tools/vfx_preview/scenes.d/israfil_models.json`, the test `IsrafilModelsTest`, this doc, a 3.21.0 note in `trial_fx_calls.md`. The maze walls and pillars, the 3D core and the two coordinate gates are new things that did not exist as flat parts (the panes, the void sphere and the hoop planes stay). |
| REPLACED (only while the setting is on and the thing is near) | the flat HORN sprite and its glow (TRUMPET), the flat coordinate plate plane (GRAVITY), the first eight billboard shards of the pull-in and four of the orbiting shards (COLLAPSE), the two VOID billboards of the dark sphere (COLLAPSE), the hoop planes shrink to sit inside the gates (SHIFT: radius R becomes 0.62 R when the gate is drawn). Of the billboard glass shards and motes of the trumpet one third and none are drawn while the horn is up (the 3D shards take their share). |
| KEPT | every 3.20.0 texture, every 3.20.0 draw call (they are the fallback), the panes, floor discs, seals, rings, rays, beams, sound rings, ghosts, the rift, every duration, the shape order, the server code, the Time Magic VFX. |

## The models (all in `IsrafilModels`, built once at class load)

| Model | What it is | Faces | Quads kept from any view (random views, 20000 each) | Used by |
| --- | --- | --- | --- | --- |
| HORN | the Grand Trumpet: an 8-sided flared bell with a gold rim band and cyan gems, a swept diamond-section tube with one tightening coil, brass ferrules, a mouthpiece cup, two fans of feathers, a glowing mouth disc | 99 | at most 68 (measured 66; about 45 from below) | TRUMPET |
| PLATE | the weight: a hexagonal washer slab (engraved coordinate top, brass-trimmed sides) with a crossed-quad chain at each of three attach points, and a rune ring on its own part that turns | 42 | at most 36 (measured 33) | GRAVITY |
| WALL_A / WALL_B | a maze wall slab (1.9 x 1 x 1.9 scaled), maze relief on both faces, a capped top; B is the mirrored relief | 9 each | at most 8 (measured 5) | LABYRINTH |
| PILLAR | a square pillar with maze relief on all four sides, cap and foot | 13 | at most 10 (measured 8) | LABYRINTH |
| SHARD | a faceted crystal spike (tetrahedron on a long axis) in teal or violet, half-transparent facets | 4 | at most 3 (measured 3) | COLLAPSE, TRUMPET (horn break) |
| CORE | the dark core: an icosahedron of dark facets with bright edges | 20 | at most 12 (measured 10) | COLLAPSE |
| GATE | a coordinate gate: an arch with two posts, a brass cornice and a rune strip | 24 | at most 14 (measured 13) | SHIFT |

How it is read in game:

- TRUMPET: the horn swings up from the caster's side in 6 ticks (overshoot, the bell ends up over the head, leaning a little toward the camera), a flash of cyan leaves the bell at tick 4 to 8 and the existing cone of rays, shaft, flare and
  rings now start at the horn's own mouth. It blares from tick 5 (it shakes a little and glows), cracks from tick 9 to 13 (its faces light up and jitter), and at tick 13 to 15 it breaks: it fades out while up to four 3D crystal
  shards fly out of it, tumble and fall. Far away (over 80 blocks) the old flat horn is drawn.
- GRAVITY: an engraved hexagonal weight hangs on three chains under a turning rune ring, wobbles as it falls (it speeds up, as the old plate did), slams onto the target's feet (the target stands in the hole of the washer), rebounds a
  little and flashes at the thud; the old dark disc, cracks, light pillars, tally chains, thud ring and dust are all still there. The flat plate and its ring are only drawn when the slab is not.
- LABYRINTH: up to five maze walls and pillars (3, at reduced detail) rise round the trapped creature, their maze faces toward it, at the same ring as the panes. At every 20-tick re-shuffle they slide to new angles, radii and
  heights (and change kind) with a small dip, and flash with the panes. Drawn far to near. The ten panes, the floor, the seal and the corner shards stay.
- COLLAPSE: during the pull-in the first eight shards are 3D crystals (tip along their path, rolling) and a small dark core grows at the centre, ending in the pinch; in the field phase the faceted core (turning, pulsing, with the existing
  inverted rim and glow billboards over it) replaces the two VOID billboards, and four of the orbiting shards are 3D. Everything else (rings, cracks, grid, motes) is unchanged.
- SHIFT: a coordinate gate stands at the start and at the arrival, its face turned toward the line, flipping with the old hoop (the hoop shrinks to sit inside the arch). It fades with the ring. Low priority, so it is small.

## Setting

`config/nusmp-israfil-client.toml`, `[visual] israfilModels = true` (CLIENT: it only changes how things look). Off = the flat sprites of 3.20.0, nothing else changes. Mirrored into the pure `IsrafilModels.enabled()` like
`HerculesModels`. Even with it on, the models are skipped when the particle setting is minimal (`ctx.detail < 0.5`) and beyond their range (horn 80, plate 52, maze 60, core 64, shards 44, gates 44 blocks).

## How it is drawn (and why it is cheap)

The vertex buffer has no depth write, so a solid is drawn like `SolidParts` / the Hercules models do it: `Posed.prepare` drops the faces that look away from the camera (back-face culling by the winding), shades each from a fixed
sun (`0.38 + 0.62 * lit`, lifted by the emissive amount and the `heat` of the moment), and sorts the rest far to near (an insertion sort on arrays that exist once). All faces are alpha-blended quads of the one atlas in one batch.
Triangle facets use the lower-right half of an atlas cell. Nothing is allocated per frame, per model or per face. A model first asks `buf.hasBudget((visible + reserve) * 4)`; if it does not fit together with the room the rest of the effect
needs (the horn keeps 44 quads free for the rays and rings), nothing of it is drawn and the layer falls back to the flat part.

## Worst case per effect (measured in the preview every tick or two with a raised budget so nothing is cut, also with big radii; engine cap 100 quads / 400 vertices)

| Effect | 3.20.0 | 3.21.0 | Where |
| --- | --- | --- | --- |
| LABYRINTH | 43 | 74 (radius 4.5 and 9) | five walls and pillars on top of the ten panes, at a re-shuffle |
| GRAVITY | 31 | 60 (size 1.5 and 4) | the slab 33 + chains of the old effect near the thud |
| COLLAPSE | 35 | 50 (radius 6.5 and 12) | pull-in: 8 shards + core; field: core 10 + 4 shards |
| SHIFT | 35 | 54 (a 12-block and a 48-block shift) | both gates 13 + 13 over the line, ghosts and rings |
| TRUMPET | 62 | 98 (from the side and from behind) | tick 11 to 15: horn 45, break shards, rays, rings; the billboard shards and motes are thinned while the horn is up |
| RIFT | 27 | 27 | unchanged |

Every number is below the 100-quad activation cap. The trumpet peak is close to it, so the horn is drawn only when it plus 44 free quads fit (an unusual view from above that keeps more than 56 faces gets the flat horn), and the
optional billboards drop out first.

## Rule 11 cost

Client only: no server work, no extra packets, nothing kept between frames (nothing to clean up on logout, death or dimension change). The only new per-frame work is posing and sorting at most 68 faces (horn), 36 (plate) or
five times 10 (maze) with arrays that exist once; the insertion sort is on at most 68 elements. Far effects (beyond the ranges), minimal particle settings and a full budget draw the flat parts of 3.20.0.

## Previews (`python tools/vfx_preview/preview.py <scene>` writes `build/vfx_preview/<scene>.png`)

`israfil_trumpet_close`, `israfil_trumpet_break`, `israfil_trumpet_view`, `israfil_plate_close`, `israfil_plate_view`, `israfil_maze_close`, `israfil_maze_view`, `israfil_shards_close`, `israfil_core_close`,
`israfil_collapse_view`, `israfil_gate_close`, `israfil_gate_view` (all in `tools/vfx_preview/scenes.d/israfil_models.json`). `tools/vfx_preview/preview.py` also compiles the new `Israfil*.java` files when `TrialLayer` is in a scene.

## Not seen in a game (honest caveats)

Everything here was checked in the headless preview rasteriser and with pure tests, not in a running Minecraft client. Not verified: how the alpha-blended mesh sorts against the real translucent world (water, glass),
the real shader's brightness of the sun shade, the look at night and in the Nether sky, the look at a real frame rate while the camera moves, and the real camera shake against the horn. The horn's bell looks washed out in the preview when
the light bursts from it (the additive glow sits right over it). The coordinate gates are small and plain on purpose.
