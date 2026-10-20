# Visual upgrade: God grade charms and Genesis grade non-weapon relics (slice `charms_genesis_worn`)

Owner request of release 1.102.0: "Upgrade God Grade weapons/relics to look 5x better and Genesis grade to look 20x better." This slice is 27 relics:
the 18 God grade charms (the six Infinity Stones, the Deck of Many Things, Warren's Key, the Rose Petal, the Administrator's Authority Key, the Archivist's
Starbound Codex, the Behelit Egg, Kamish's Fang Charm, the Moon-Tower Black Sigil, the Orrery of Drowned Hours, the Pocket Watch of Return, Serie's Ancient
Grimoire, the Vaultmaker's Pocketwatch) and the 9 Genesis grade non-weapons (the Holy Grail Fragment, the Philosopher's Stone Shard, the Prison Realm Cube, the
Seal of the Sealed God, Sukuna's Twenty Fingers, the Infinity Gauntlet, the Absolute Being's and Zahard's crown pieces, the Gate of Babylon Crest).

**Nothing here has been seen in a game.** Everything below is measured from the files; "5x" and "20x" are claims about the proxy score V of this document, never
about how the items look in play.

## How "better" was made measurable

The baseline of every item was measured first and committed alone (`docs/visual_upgrade/charms_genesis_worn_baseline.json`, tool
`tools/visual_upgrade/charms_genesis_worn/baseline.py`), then the same code measured the result (`..._after.json`, `report.py`; the table is
`docs/visual_upgrade/charms_genesis_worn_ratios.md`). An item is seen in two views, **hand** (what `HandForms` draws held, on the ground, in a frame: its signature hand
model, else the models it wears) and **worn** (what `WornLibrary` draws on the body). Per view the tool records pieces, quads, vertices, render passes, painted texels,
animation groups and ambient layers (`metrics.py`).

    V = 0.35 mesh + 0.20 texture + 0.15 passes + 0.20 ambient + 0.10 animation
    each component = (after, summed over the two views) / max(before, summed over the two views, 1)         V_before = 1 by definition

| component | what is counted | before | after |
|---|---|---|---|
| mesh | quads of the models of the view | the library models: the shared parts of the older look | the upgrade models of `assets/nusmp/visual_upgrade/models` |
| texture | painted texels | the atlas cells the quads' UVs touch x 32 x 32 (the shared 128 x 256 atlas, tinted by the palette) | the opaque texels of the item's own skin (God 512 x 512, Genesis 1024 x 1024) |
| passes | draw passes the models use | solid, emissive (if a quad glows), shell rim (if a quad is shell) | skin, glow map, nebula/starfield (STAR quads), additive halo (HALO quads) |
| ambient | ambient effect layers | none of the item's own; the grade layers every worn relic gets (rim, shimmer, motes, rune ring, particles, head halo, wearer shimmer; the halo band for Genesis) = 7 / 8 | those, plus the layers of the item's own fx file (geometry groups and particle layers) |
| animation | animation groups of the models | as in the files | as in the files |

### What the score says and does not say (read this before quoting a number)

* Measured: every God item has V >= 7.2 (mean 13.0), every Genesis item V >= 28.7 (mean 34.8) against the targets 5 and 20.
* **The texture term does most of that.** The texel ratios are 26x to 82x (God) and 124x to 192x (Genesis) because the old looks borrow tiles of one shared
  128 x 256 atlas and the new ones each have their own 512 / 1024 skin. With the texture term left out of V (the other four weights rescaled) the God items
  reach 2.3 to 4.9 (mean 3.8) and the Genesis items 3.2 to 6.1 (mean 4.7): **the geometry and effects alone are 3 to 5 times the baseline, not 20 times.**
  The report prints both numbers (`V_without_texture`).
* **The mesh ratio is bounded by the owner's performance guard**, and the brief's quad floors cannot all be met under it. A hand item may draw at most 2500 (God) /
  6000 (Genesis) vertices at full detail, and the glow-map pass draws a quad a second time, so the God items sit at 392 to 586 quads and the Genesis items at 897 to 1350
  (floors of the brief: 600 / 2500, "900 / 3500 for a worn piece set"). 20x the quads of the old models (up to 621) would need 12 000+ quads. The guard was kept; the mesh
  ratios are 1.6x to 3.6x (God) and 2.6x to 5.7x (Genesis).
* The ambient ratio is 3.6x to 5.6x and the pass ratio 1x to 2x: the grade layers are already there for every worn relic of the grade, so only the item's own layers add.
* The proxy counts quantity. A larger skin is not by itself a better skin: the detail on it (engraved lettering, filigree, enamel, veins, wear) is what was authored, and the
  previews (`docs/previews/visual_upgrade/charms_genesis_worn/<id>.png`, before and after side by side) are from a software rasteriser, not from the game.

## What was built

Per item (`items_*.py` in `tools/visual_upgrade/charms_genesis_worn/`): a hand model, a worn model, a lod model for each, a skin, a glow map, a star texture (nebula) for the
item, an fx file (ambient layers, ambient sound id), a line for each act in the act effect table. All shapes follow the lore text of the item's RelicDef.

| item | what the model is | its own motion |
|---|---|---|
| The Space Stone (Tesseract) | a cage of folded space (bars, corner crystals, face jewels) round two cubes of stars, gyroscope rings, shard orbits, a turned silver foot | core and inner cube tumble, gyro rings, two shard orbits, ripple, rune ring |
| The Mind Stone | a tall crystal of thought in a gold lotus of claws on a spiral-wired sceptre, thought arcs with memory beads | arcs tumble, beads orbit, crystal pulses |
| The Power Stone | a burst of crystal spikes round a jagged heart in an iron war cage, rock debris, lightning arcs | core and burst pulse, two debris orbits, cage sway, shock ripple |
| The Time Stone (Eye of Agamotto) | the amulet: three dial rings, eyelids, an iris of stars, sun rays, a chain | dials turn against each other, eyelids bob, rays pulse, time rings spin |
| The Reality Stone (the Aether) | two spheres of liquid red in three writhing tendrils, liquid ribbons, droplets | heart pulses, core tumbles, tendrils sway, ribbons spin, distortion web |
| The Soul Stone | an orange soul gem cupped in a hand of bone on a gold cuff, wisps and souls | gem pulses, souls and wisps orbit, smoke pulses |
| Deck of Many Things | a gold-edged enamel case with a fan of nine cards, five and four fate cards orbiting, a starry sky and a seal behind | fan sways, two card orbits, sky and seal pulse |
| Warren's Key | a bronze key whose bow is a labyrinth, a maze-wall bit, an open door with a ripple, lost keys circling | door bobs and ripples, keys orbit, maze glow spins |
| Rose Petal | a ghost-white rose of three whorls on a thorned stem, a thorn vine, a pale halberd of the nebula behind it, falling petals | whorls turn at three speeds, vine and petals spin |
| Administrator's Authority Key | a hexagonal-ring key round a root core, console segments, a comb of permission blocks, command panels, a wireframe | panels orbit, wireframe tumbles, ring spins, seal pulses |
| Archivist's Starbound Codex | a book whose cover is a sky, seven eyes, torn pages and bookmarks circling, a zodiac ring | pages orbit twice, eyes pulse, ribbons sway, zodiac ring |
| Behelit Egg | a gold egg with a closed face, blood tears, a chain; a black eclipse with a corona behind, black feathers | corona spins, eclipse pulses, feathers orbit, tears bob |
| Kamish's Fang Charm | a curved ridged fang with lava scales on an iron root cap, scale shards and embers, a fire ring | fang sways, heart fire pulses, shards and embers orbit |
| Moon-Tower Black Sigil | an obsidian disc round a void, a crescent moon holding a tower, eight moon phases, shadow flames, a seal | phases orbit, flames pulse, two seals counter-rotate |
| Orrery of Drowned Hours | a verdigris brass orrery: pedestal with barnacles and coral, a drowned sun, ecliptic rings, seven planets on seven arms | seven planet groups at seven speeds, ecliptic tumbles, bubbles bob |
| Pocket Watch of Return | a silver watch, lid open, dial with twelve lit markers, three hands that run backwards, two gears, a chain | hands turn backwards at three speeds, gears, rewind ripple |
| Serie's Ancient Grimoire | an open ivory book with fluttering pages, silver vines, a column of mana, floating runes, two spell circles | pages flutter, runes tumble, circles counter-rotate |
| Vaultmaker's Pocketwatch | a hexagonal brass skeleton watch, vault bolts, three meshing gears, a day hand and a hand behind the day | three gears, two hands, bolts pulse, vault seal |
| Holy Grail Fragment | a broken golden chalice, jewelled, wire-wound, light pooled in it, a pillar, angel wings, halo, shards and wish orbs | halo, two shard orbits, wish orbs, wings, feathers, ground seal |
| Philosopher's Stone Shard | a cluster of red crystal in a ritual bowl, a gold wire cage, element plates, rising souls, a transmutation circle | circle and hexagram counter-rotate, souls, plates, lightning |
| Prison Realm Cube | a white-framed cube, black cords, an eye, sutra tags, binding chains, a shimenawa rope with shide and bells, tiny sealed cubes | tags and chains sway, cubes tumble, two seals |
| Seal of the Sealed God | an octagonal slab, four seal rings (one broken), a void socket with a slit eye, six anchor chains and spikes, shackles | four rings at four speeds, chains sway, shackles tumble |
| Sukuna's Twenty Fingers | twenty mummified fingers in a bone cuff with a mouth of teeth and four eyes, cleave blades, curse cloth | bouquet sways, inner fingers pulse, two blade orbits |
| The Infinity Gauntlet | black and gold armour for the arm, ribcage palm, six sockets with the six stones in their colours, wires, spikes, wrist rings | wrist rings, stone pulse, snap arcs, palm vortex, finger flex |
| Absolute Being's Crown Shard | a white crown of seven broken blades, small shards, two halos, a chain of thrones, a void behind the head | two halos, two shard orbits, chains, runes, ribbons |
| Zahard's Crown Fragment | gold pointed arches (two rows) on a band, jewel pendants, the Tower as four turning rings with a spire, children circling | four tower floors at four speeds, two orbits, pendants swing |
| Gate of Babylon Crest | pauldrons, gorget, a gold mantle, the great gate ring and nine lesser gates each with a treasure half out of it, chains of light | gate ring, three ripples, lesser gates pulse, weapons sway, rays |

### Passes, layers, animation, sound, act effects, equip animation

* **Skin pass**: the item's own skin, lit, no palette tint (`WornMesh.skin`). **Glow map pass**: the same UVs, additive, pulsing. **Nebula pass**: the STAR quads (cores,
  skies, voids) over the item's own tileable star texture, additive, scrolled by the clock. **Halo pass**: the HALO quads (rune rings, seals, ripples, flares, light shafts,
  portals; 16 soft-light shapes in the shared `textures/visual_upgrade/vu_fx.png`), additive, tinted by the colour slot of the quad, breathing. The tone of a halo quad is
  chosen by the generator from the palette so the sum of the layers does not burn white (`VU.tone`).
* **Ambient layers** (`assets/nusmp/visual_upgrade/fx/<id>.json`): geometry layers are named animation groups of the model; particle layers are spawned by `VuFxClient`.
  Every item has 9 to 16 (God) and 12 to 17 (Genesis); the tests require >= 3 (God) / >= 6 (Genesis), geometry and particles both.
* **Idle animation**: 8 to 15 groups (God), 10 to 15 (Genesis) of the kinds bob, spin, sway, pulse, tumble (a new kind, `WornAnim`), orbit, wave.
* **Equip (draw) animation**: the first-person model swells into the hand over 12 ticks (`VuFxClient.equipProgress`).
* **Ambient sound id**: `nusmp:relic.ambient.<relic id>` in every fx file (a string only; the sound itself is another task).
* **Signature act effect**: `data/nusmp/visual_upgrade/act_vfx.txt`, one line per act (or `*`), up to three effects of the mod's own VFX shapes with the item's colours,
  fired by `RelicEngine.activate` after the act ran (`VuActVfx`; through `VfxSpawn`, so it is inside the effect-packet budget). Only shapes of a vetted list are allowed; none is a Time Magic
  or zone shape. The generator and the self-test check every key against the relic's real acts.
* Swing trails do not apply: these are charms, a gauntlet, crowns and a crest, not weapons (Better Combat, engravings (rule 6) are weapon things).

## NEW / REPLACED / KEPT (rule 5)

| | |
|---|---|
| NEW | the 27 upgrade models x (hand, worn, lods) in `assets/nusmp/visual_upgrade/models`; 27 skins, glow maps, star textures and 54 lod skins in `textures/visual_upgrade`; the shared fx sheet; `overrides.txt`; the fx files; `act_vfx.txt`; the caps table; the client classes `client/relics/vu/*`, `relics/vu/*`; the draw passes of `WornLibDraw`; the `TUMBLE` animation; the options `visual.upgrade` and `visual.guiModel`; the generator `tools/visual_upgrade/charms_genesis_worn`; the tests; this doc |
| REPLACED | the way these 27 relics are held and worn: the override table points them at the new models while `visual.upgrade` is on (default). The older models are not removed |
| KEPT | every older model, the hand and worn catalogs (never edited by hand or by the new generator), the atlas, the hold-style table, ids, stats, acts, passives, the inventory sprites (the sprite stays the icon, rule 7), the item jsons. `visual.upgrade=false` is exactly the 1.101.x look |

## How it works (code)

* `visual_upgrade/overrides.txt`, read by `VuModels` through `VuOverrides` (pure): `hand <id> <pieces> | size=.. style=..` and `worn <id> <pieces>`; `HandForms` and `WornLibrary` ask it first,
  and rebuild their caches when `visual.upgrade` flips or the resources reload (`VuModels.generation`).
* `WornMesh` got the records `skin`, `glowtex`, `startex`, `lod` and the quad flags GLOWMAP (8), STAR (16), HALO (32). `WornLibDraw.customPasses` draws a model with a skin in its four passes. Beyond 12
  blocks the lod model is drawn; beyond 24 blocks (or with `relicfx.quality` low) the additive passes are skipped. The draw loops allocate nothing (scratch matrices are reused, texture locations cached).
* `VuFxClient` spawns the particle layers: players within 24 blocks (at most six), the client tick budget (`FxQuality.particlesPerTick`, `PerfSettings.clientParticlesPerTick`, halved at the
  low vfx quality), nothing with `relicfx.particles` off.
* `visual.guiModel` (default off): the upgraded relic drawn as a tilted 3D model in inventory slots instead of the sprite; off, the sprite is the icon as before.

### Performance guard (numbers are enforced by `VuCharmsBudgetTest`)

| | God | Genesis |
|---|---|---|
| mesh vertices of a hand or worn item | 392..586 quads = at most 2344 (cap 2500) | 897..1350 quads = at most 5400 (cap 6000) |
| drawn vertices including the glow-map pass | at most 2500 | at most 6000 |
| lod model | at most 15% of the vertices (measured 6% to 15%) | at most 15% (measured 6% to 14%) |
| skin | 512 x 512 | 1024 x 1024 (lod skins 64 / 128) |

The per-grade caps are `assets/nusmp/visual_upgrade/caps_charms_genesis_worn.txt` (this slice's own table; the shared 2600-vertex caps of `gen_worn_all.py` and `HandCatalogTest` are untouched
because the upgrade models live outside `relic_worn` and `relic_hand`; the other worn builder keeps its own table in its own file).

## Hooks in shared files (append-only)

| file | line |
|---|---|
| `tools/relics/gen_worn_all.py` | one line at the end of `main`: runs this slice's `build.py` (own files only) |
| `NUSMP.java` | registers `VuCharmsOptions.SPEC` (`nusmp-visual-charms-client.toml`) next to `RelicFxOptions.SPEC` |
| `RelicSystem.java` | `VuSelfTests.register()` (self-test `visual.charms_genesis_worn`) |
| `RelicEngine.java` | `VuActVfx.fire(p, def, act)` after the act body |
| `WornLibClient.java` | `VuFxClient.init()` and the reload clears (`VuModels`, `VuFxClient`, `WornLibDraw.clearCustom`) |
| `WornMesh`, `WornModels`, `WornAnim`, `WornLibDraw`, `WornLibrary`, `HandForms`, `HandTransforms`, `HandItemRenderer` | small additions described above; behaviour for the library models is unchanged |

## Tests

`VuCharmsBudgetTest` (vertex caps, lods under 15%, skins, glow maps, idle groups, passes, fx files name real groups, fallback, library directories untouched),
`VuCharmsRatioTest` (the data test of the ratio files: 27 items, V recomputes from its components and meets the target of its grade, the after file matches the model files, texel floors),
`VuOverridesTest`, `VuActTableTest` (shapes are real, none is Time Magic, every act key exists), `WornMeshUpgradeTest` (the records, the flags, the lod, TUMBLE).
Self-test `visual.charms_genesis_worn` (server smoke): all 27 relics are registered with their models, textures, fx, act effects, every act of each relic has an effect, the fallback answers nothing.

## Not verified / honest list

* Nothing was run in the game: no jar built, no frame drawn, no particle seen. Type checks (the repo's stub checker) and the JUnit shim pass for the files above; Gradle was not available.
* The previews come from `vupreview.py`, an orthographic software rasteriser: no perspective, no light map, no bloom, back-face culling of the additive passes only. In the game the additive layers may read brighter or dimmer.
* Floors of the brief not met (cap conflict above): 600 / 900 quads for God, 2500 / 3500 for Genesis; the quad count of the God items is 392..586.
* The in-hand placement of each model (`size`, `style` of the override line) was set by eye from the generator's bounds, not seen; the first thing to check in the game is the size of the stones, the hang of the watches and the Gate crest on the back.
* The worn model of a charm hangs at the old charm's place (right hip); a second charm is moved by `WornPlacer` as before.
* The draw animation is a swell of the model; the individual orbiting parts do not "unfold".
* `visual.guiModel` draws the model with `RenderType.eyes` passes inside a GUI slot; not tried.
* Sounds: the ids only.
* Cut items: none.
