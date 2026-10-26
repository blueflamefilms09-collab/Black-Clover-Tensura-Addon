# Solomon, Lord of Sorcery VFX calls ("a sorcerer's night" look)

The nine shapes are appended after `JIBRIL_WINGS` in `VfxShape` (new; nothing before them moved, the order goes over the network). Every call is one `VfxSpawn` packet with one `SOLOMON_*` shape; the packet colour is ignored (the one exception is the Elemental Sovereignty spark, which takes the tint of the projectile it drinks when the sender gives one other than the default gold). Drawn by `vfx/client/layer/SolomonLayer.java`, registered in `VfxManager` right after `JibrilLayer`; textures `textures/particle/solomon_*.png` from `tools/gen_solomon_vfx_textures.py`; previews `python tools/vfx_preview/preview.py solomon_copy ...` (`tools/vfx_preview/scenes.d/solomon.json`, 18 scenes). New work only: nothing existing was replaced or removed. Sources: this look is written from the owner's brief (no canon pages were needed for the visuals); the server side (`SolomonRules`) is the lead's.

## Look (what makes it Solomon and not Jibril, Sandalphon or Israfil)

Deep indigo and violet night, antique gold, parchment white, and a small six-element prismatic accent (flame orange, tide blue, gale green, stone amber, storm yellow, frost cyan; the pillars also use light white and dark violet-black). Motifs: grimoires with turning pages, hexagrams / Solomon's seal, nested runic magic circles, chains of linked circles, designed runes, drifting parchment pages, a honeycomb mana shell, tall pillars of elemental light with craters. It shares none of Jibril's feathers, wings and ribbons (pearl and rose-gold), none of Sandalphon's steel clock rings and black cracks, and none of Israfil's cyan maze; it never touches the Time Magic layer or its textures.

## Calls

- SOLOMON_COPY: Grand Spell Copy, sent at the caster: from = its centre, to = the centre of whoever cast the copied spell (same as from when cast from the Grimoire), power = 1, 30 ticks. A scanning ring and three glyph lines read the spell at `to`, streams of runes run from there to the caster and write themselves onto a floating page, which turns into a closed Grimoire and sinks into the caster; a ring closes on the floor.
- SOLOMON_CONVERGENCE: Arcane Convergence, sent at the caster: from = its centre, power = radius (30 in game; the picture stops at 24 blocks), 30 ticks. Two wide rings close in on the floor, light streams of the six elements, runes and motes run in from the whole radius and sink into the caster, a magic circle wakes under it; final flash, hexagram, two mana rings.
- SOLOMON_REALM: Realm of Magic, sent FOLLOWING the caster and re-sent every 80 ticks while held: from = its centre, power = radius (40 in game), 100 ticks (fades in 12, out 12). The nearest rune veils (up to five, chosen in front of the camera) and a rim ring mark the edge, a floor seal of a magic circle and a hexagram turns under the caster, up to three pages and runes float, rune lines rise, a crown of three tilted circles with six element sigils turns over its head. Vertex budget 200 (50 quads).
- SOLOMON_CHAIN: Spell Chain, sent at the caster: from = its centre, to = a point 6-12 blocks ahead, power = 1, 24 ticks. Three magic circles of three different elements open one behind the other along the line, are linked by a chain of light and fire in rapid order, each with a flash, a shock ring and a lance of light.
- SOLOMON_ERASURE: Mana Zone Erasure, sent at the caster: from = its centre, power = radius (40 in game; the picture stops at 36 blocks), 30 ticks. A hexagram seal and a circle on the floor, a void that flashes white, a rune wave that sweeps out and un-writes the glyphs it passes, a dome of three rings, a fall of rune dust.
- SOLOMON_MARK: mark of the 72 Pillars, sent FOLLOWING the marked target: from = its centre, power = size (~1.5), 40 ticks. A hexagram seal closes on the ground and a ring of six small element circles spins round it.
- SOLOMON_PILLAR: The 72 Pillars on one target, sent at the ground point under the target: from = that point, power = spread radius (~6), 120 ticks, seed = `SolomonRules.packSeed(random, count)`. The layer uses the pure functions of `SolomonRules` the server lands its impacts with: `pillarsOfSeed(seed)` pillars, pillar k starts at `pillarSpawnTick(k, m)`, lands `PILLAR_DELAY_TICKS` (12) later at `pillarOffset(seed, k, power)`, in element `pillarElement(seed, k)` (0 flame, 1 tide, 2 gale, 3 stone, 4 storm, 5 frost, 6 light, 7 dark). A pillar shows a ground telegraph, falls as a streak of light, lands with a burst, a shock ring and a column of elemental light (12 ticks) and leaves a crater scar that fades over 56 ticks. At most 6 falling and 6 landing pillars are drawn at once (thinned by the particle setting); scars take what is left of the budget, newest first. Camera shake (VfxShake, through the shake budget) on the first four landings.
- SOLOMON_SKIN: Mana Skin saved a life, sent at the wearer: from = its centre, power = size (~1.5), 16 ticks. A violet-gold honeycomb shell snaps on, cracks with light and drinks the blow as motes.
- SOLOMON_ABSORB: Elemental Sovereignty took a projectile, sent at the projectile: from = its position, to = the wearer's chest, power = 1, 14 ticks. A small tinted spark is pulled through a thin curved line into the wearer and a ring closes on the chest. The tint is the packet colour when it is not the default gold, otherwise an element chosen from the seed.

## Worst case per effect (measured in the preview at every integer tick, engine cap 100 quads / 400 vertices)

| Effect | Quads (worst frame) | Where |
| --- | --- | --- |
| COPY | 32 | tick 13 |
| CONVERGENCE | 71 | tick 20 |
| REALM | 39 (78 during the 20-tick overlap of a re-send; budget 50 each) | tick 3-8 |
| CHAIN | 37 | tick 19 |
| ERASURE | 50 | tick 13 |
| MARK | 30 | tick 9 |
| PILLAR | 34 for 18 pillars on one target, 90 for 72 pillars on one target | tick 85 / 109 |
| SKIN | 23 | tick 8 |
| ABSORB | 16 | tick 9 |

The 72-pillar case reaches 90 of 100 quads only because every pillar part first asks `buf.hasBudget(...)`: the buffer can never be pushed past the activation cap, the nearest and newest pillars are drawn first. Preview seeds: `SolomonRules.packSeed(12345L, 18)` (12944670738) and 72 (12944670792).

## Rule 11 cost

Client only: no server work, no extra packets (one `VfxSpawn` per effect, sent to nearby players only through the existing perf budgets), nothing kept between frames (so nothing to clean up on logout, death or dimension change). Per frame: at most 100 quads per activation (Realm 50), at most 6 falling + 6 landing pillars, at most 16 scars, every loop count a constant or a `ctx.seg` of at most 16 (distance and particle setting thin it); the pillar loop walks the pure schedule of at most 72 entries (integer maths, no allocation beyond the two-number offset of at most ~30 drawn pillars and scars). Decorative extras fall away with the camera distance (40-90 blocks, pillars 60-120); the Convergence and Erasure rings stop at 24 and 36 blocks; the manager already drops an effect out of range. Textures: 17 files, largest 1024x1024 (circle, hexagram, sigils), about 20 MB of VRAM in all. Covered by `SolomonShapesTest` (shape order, render cases, textures, no per-frame collections, budgets, registration, Time Magic untouched).

## Not seen in a game

Everything above was checked in the headless preview (`tools/vfx_preview`, contact sheets of 6 frames per shape) and by the source-scan test; none of it was seen in a running game, so lighting, depth against blocks and bloom may look different in game.
