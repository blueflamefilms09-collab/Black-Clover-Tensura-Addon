# Jibril 3D models (3.21.0): the holy cuffs and chain, the Spear of Heaven, the halo and law plates, the tome of law, the command crown

The Jibril ultimate keeps its eight `JIBRIL_*` shapes (nothing added, nothing reordered, no packet and no server code changed). What changes is how six of the big moments are DRAWN: real 3D meshes are built inside the existing `JibrilLayer`, in the same quad budget, next to the flat sprites. Sources: the look is the layer's own palette (pearl, rose gold, amber, azure and lavender, the prismatic rim); the shapes (hinged manacle, winged spear, open book of law, crown of seal light) come from the owner's brief for Jibril, Lord of Rigor. No wiki lookup was needed (no canon names or effects are quoted). The wings of WINGS and the wing quads of JUDGMENT / WORD are already flat feathered quads and are NOT duplicated by a model.

## NEW, REPLACED, KEPT (rule 5)

| | What |
| --- | --- |
| NEW | `JibrilModels` (pure geometry: 7 static meshes, the poser), `JibrilMeshDraw` (the drawer), `JibrilModelsConfig` (the CLIENT setting `visual.jibrilModels`, file `nusmp-jibril-client.toml`), `textures/particle/jibril_model_atlas.png` (512 x 512, 16 material cells) from `tools/gen_jibril_model_textures.py`, the scenes `tools/vfx_preview/scenes.d/jibril_models.json`, this doc, `JibrilModelsTest`. |
| REPLACED (only while the setting is on and the thing is near) | SHACKLES: the upper and the lower flat chain ring become hinged 3D cuffs, and a third strand of 3D chain links is added. JUDGMENT: nothing is removed (the thin braided spear of light stays as its trail), the 3D Spear of Heaven falls inside it and stands in the pillar. WORD: the flat scroll panel and the two nested flat halos become the 3D tome of law. COMMAND: the inner flat halo ring becomes the 3D crown. KINGDOM: the two flat halos, the orbiting feathers and the prismatic bloom over the caster become a 3D halo with three 3D law plates. WINGS: the two flat halos over the head become the 3D halo. A few little extras (petals, motes, sparks, feathers, one rays plane, one floor ring) are thinned while a model is drawn so every effect stays under its quad cap. |
| KEPT | every 3.20.0 texture, every 3.20.0 draw call (they are the fallback and are drawn when the setting is off, the camera is far, the particle setting is minimal or the model does not fit the budget), the flat feathered wings, the pillar, ribbons, glyph, seals, rings, flashes, every duration, shape order, the 200-vertex budgets of KINGDOM and WINGS, the server code, the Time Magic layer (untouched). With `jibrilModels = false` the layer draws exactly what 3.20.0 drew. |

## The models (all in `JibrilModels`, built once at class load)

| Model | What it is | Quads kept from any view | Used by |
| --- | --- | --- | --- |
| CUFF | a hinged manacle, 25 faces on 2 parts: a base half and a lid on a hinge pin, hexagonal gold bar with bevels, an azure enamel inner face, a glowing seal plate on the latch | at most 18 (limit 18) | SHACKLES |
| LINK | a flat frame of gold bars with a wall on the long sides, 6 faces | at most 5 (limit 6) | SHACKLES |
| SPEAR | the Spear of Heaven, 54 faces: a six-sided crystal head with four barbs of light, a gold collar, a pearl socket, six large and four small feathers fanned as wings, a shaft with a gold band, a crystal pommel; about 9.7 long, scaled 1.0 to 2.6 | at most 35 (limit 36) | JUDGMENT |
| HALO | a ring of golden bar, ten facets, the script band on the two outer slopes, 40 faces | at most 20 (limit 22) | KINGDOM, WINGS |
| PLATE | a small tablet with a gold edge, the law on the front and an azure back, 4 faces | at most 3 | KINGDOM |
| TOME | the open tome of law, 30 faces on 7 parts: spine, two covers with page stacks and gold corners, four loose leaves that turn over the spine one after the other (`tomePose(open, flip, ...)`) | at most 22 (limit 24) | WORD |
| CROWN | a script band under six crystal spikes alternating tall and short, 30 faces | at most 25 from the upright views the layer uses (29 from any random view, limit 30) | COMMAND |

How it is read in game:

- SHACKLES: two golden cuffs close in from wide open (lid thrown back) round the upper and lower part of the target between ticks 8 and 16 and clamp shut with the existing flash; the sprite ring stays between them. A third strand of two golden chain links swings along the arc between the two ribbon chains.
- JUDGMENT: a winged spear of light falls out of the sky inside the thin braid (the wings turn about the shaft), sinks a little into the ground at the strike while its head blazes, and stands in the heart of the pillar for about 10 ticks before it fades. Honest note: once the pillar is full it is a very bright additive column and swallows most of the spear; the spear reads best while it falls and in the first ticks, and its gold barbs and feathers show at the foot of the pillar.
- WORD: an open tome of law pops open over the glyph (pages face the camera and sway a little), its four leaves turn over one after the other while the parry rings and the flare run.
- COMMAND: a golden crown of seal light turns over the head where the inner halo ring was, flares at the lock; the outer halo, emblem and everything else stay.
- KINGDOM: a golden script halo turns over the caster with three law plates orbiting it.
- WINGS: the same halo turns over the head between the flat wings.

## Setting

CLIENT config `visual.jibrilModels` (default true, file `nusmp-jibril-client.toml`, `JibrilModelsConfig`, mirrored into `JibrilModels.setEnabled`). It only changes how things look, so it is a client config; nothing is sent over the network. Off = the flat sprites of 3.20.0. Models are also off when the particle setting is minimal (`ctx.detail < 0.5`) and beyond 64 blocks (the halo, plates and cuffs of KINGDOM / WINGS beyond 42 blocks).

## How it is drawn (and why it is cheap)

Each model is built once (class load) from tapered blocks, n-sided frusta, swept rings and leaf blades. Per frame `Posed.prepare` poses it (a small skeleton of parts, each with a bend and a spread), drops the faces that look away (a closed solid shows only the facing ones), shades each face from a fixed sun, and sorts far to near (the buffer has no depth write). All arrays are allocated once; nothing is allocated per frame, per model or per face. `JibrilMeshDraw.draw` asks `buf.hasBudget(visibleQuads * 4)` first and skips the whole model if it does not fit, then the layer draws the old sprite.

## Worst case per effect (measured in the preview at the listed ticks, over the extreme parameters; engine cap 100 quads / 400 vertices, KINGDOM and WINGS 50 quads each)

| Effect | 3.20.0 (old) | 3.21.0 models on | Cap |
| --- | --- | --- | --- |
| SHACKLES | 70 | 79 (316 vertices) | 100 |
| JUDGMENT | 69 | 76 (304 vertices) | 100 |
| COMMAND | 73 | 66 (264 vertices) | 100 |
| WORD | 59 | 53 (212 vertices) | 100 |
| KINGDOM | 41 | 44 (176 vertices) | 50 (200 vertices) |
| WINGS | 45 | 47 (188 vertices) | 50 (200 vertices) |
| PURGE, REFLECT | 60, 58 | unchanged | 100 |

Far (over 64 blocks) the counts are the old ones (JUDGMENT far: 68).

## Rule 11 cost

- Server: nothing changed, nothing runs on the server.
- Network: nothing added.
- Client: at most 36 model quads per effect instance at a time for JUDGMENT, up to 36 for SHACKLES (two cuffs of 18; links 10 on top), 22 for WORD, 30 for COMMAND, 31 for KINGDOM (halo 22 + plates 9, usually 29) and 22 for WINGS, always inside the effect's total cap above. One drawer per layer, no per-frame allocation, one extra texture (512 x 512). The model code does a few hundred multiplies per frame per model.

## Previews

`python tools/vfx_preview/preview.py jibril_models_shackles jibril_models_cuff_close jibril_models_link_close jibril_models_spear_fall jibril_models_spear_close jibril_models_judgment jibril_models_judgment_wide jibril_models_judgment_far jibril_models_word jibril_models_word_wide jibril_models_command jibril_models_command_wide jibril_models_kingdom jibril_models_kingdom_mid jibril_models_wings` writes `build/vfx_preview/<scene>.png`.

## Not seen in a game

Everything above was checked in the headless preview and the pure tests only; nothing was seen in Minecraft. Known weaknesses: the cuff is a hexagon (six segments to stay inside 18 quads), the chain links are flat frames (they read as picture frames close up), the spear is mostly hidden by the bright pillar after the strike, the plates are small, the cuffs and halo are not lit by the world light (a fixed sun).
