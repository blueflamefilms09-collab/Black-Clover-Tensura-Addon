# Dragon Ball Ki effects, wave 1 (the Saiyan techniques)

The owner asked for every skill's effects to look like the anime it comes from instead of particle sprays. This wave covers the
Saiyan techniques in `saiyan/` (Ki Blast, Rapid Barrage, Kamehameha, Rock-Scissors-Paper, Ki Barrier, Instant Transmission, Spirit Bomb,
Meteor Combination, Dragon Fist, God Bind, Ki Deflection, Energy Transfer). Until now each of them was a spray of particles only (`Fx.spray` /
`Fx.sprayTinted`, no `VfxSpawn` at all). The old Dragon Ball skills (`DbSkills` / `DbMoves`) were taken out of play in 3.89.0, so they were left alone.

## What was built

- **`vfx/client/layer/KiLayer.java`**: one layer with 13 new shapes, appended at the end of `VfxShape` after `NEN_DOMAIN`:
  `KI_BLAST, KI_BEAM, KI_CHARGE, KI_SPHERE, KI_SHOCKWAVE, KI_AFTERIMAGE, KI_RUSH, KI_STRIKE, KI_BARRIER, KI_DEFLECT, KI_BIND, KI_DRAGON, KI_TRANSFER`.
  It is registered last in `VfxManager`.
- **`tools/gen_ki_vfx_textures.py`**: makes 15 textures, `textures/particle/ki_*.png`. They are deterministic and use no fonts. Every one is white or grey,
  so the vertex colour tints it. The textures are core, halo, beam, beam_edge, ring, dust, flash, spark, streak, slash, swirl, hex, dragon_head, dragon_body and afterimage.
- **`vfx/client/layer/KiVfx.java`**: a pure class holding the texture list and the classic switch.
- **`saiyan/SaiyanKiFx.java`**: the server helper. Every Ki effect goes out through it, as one `VfxSpawn` packet each. It also holds the `saiyan.kivfx` self-test.
- **`SaiyanForms.kiColor`**: a pure function that picks the ki colour. Generic ki follows the colour of the current form: base blue-white, Super Saiyan gold,
  Kaio-ken and Super Saiyan 4 red, Blue cyan, Ultra Instinct silver. The signature techniques keep their own colour: the Kamehameha stays blue, the Spirit Bomb
  blue-white, and the Dragon Fist and God Bind gold. One exception: in a red-aura form the Kamehameha turns red, like the crimson 10x Kamehameha of Super Saiyan 4.
- **Preview scenes**: `tools/vfx_preview/scenes.d/ki.json` has 18 scenes. The renders are saved in `docs/vfx_remake/previews/ki_*.png`.

### The look (how to read it)

Dragon Ball ki is drawn as hard cel shapes, not soft smoke. The layer builds every effect from the same pieces:

- **Core**: a flat white-hot disc with a crisp edge, drawn additive.
- **Halo**: one or two flat cel steps of saturated colour around the core. It is drawn with alpha blending, so the colour stays saturated even against a bright sky.
- **Beam edge**: a thin rippling band along the outside of a beam.
- **Impact flash**: a sharp star burst.
- **Speed lines**: short streaks along the line of a movement.
- **Shock rings**: flat rings that run over the ground with a dust ring behind them.

The Time Magic layer is the quality standard, and its techniques were copied rather than its code: life ramps, eased opening, hoops (ribbons on a circle) and
seeded randomness. Seeding means every client sees the same picture. The Time Magic files were not touched.

## Per technique: anime reference, shape, and what is sent

The English Dragon Ball wiki (dragonball.fandom.com) and the Japanese fallback (ja.wikipedia.org) could not be reached from this session: the network did not resolve them.
So every reference below comes from general knowledge of the anime and **none of it is wiki-verified** (rule 10). The God Bind entry is the least certain.
If you want it checked, allow `dragonball.fandom.com` in the session network settings.

| Technique (file) | Anime reference (unverified) | Shape and what it draws | Packets per cast |
|---|---|---|---|
| Ki Blast (`SaiyanAbilities.blast`) | A small glowing energy ball fired from the palm, popping on impact | `KI_BLAST` power 1: an orb with a short tail and a pop (star flash, halo, 3 sparks) | 1 |
| Rapid Ki Barrage (`SaiyanAbilities.barrage`) | A stream of many small blasts, as in Vegeta's barrages | `KI_BLAST` power = number of rays (1-8): staggered orbs that land spread round the mean end point | 1 (the whole volley) |
| Kamehameha (`GokuAbilities`) | Blue sphere in the cupped hands, then a thick blue beam with a white core and a round head; a big flash and blast where it lands | `KI_CHARGE` while held; `KI_BEAM` on release: sphere at the hands, beam out in 3 ticks (white core, cel halo, rippling edge, rings running down it), impact flash, ring and sparks, thins out at the end | charge: 2 a second while held (14-tick copies that cross-fade); release: 1 |
| Rock, Scissors 'n' Paper (`GokuAbilities.combo`) | Goku's "Jan Ken": a rock punch, a two-finger poke or cut, an open-palm blast | `KI_STRIKE` power = stage. Rock: punch flash, a ring facing the blow, speed lines. Scissors: two crossing ki cuts. Paper: palm burst and 3 rings running forward in a cone | 1 per press |
| Ki Barrier (`SaiyanTechniques.barrier`) | A spherical ki shield round the body | `KI_BARRIER`: sphere with a hexagon net and bright rim, shimmer, ground ring, opening flash | 1, then 1 a second while it stands (24-tick copies; it fades within ~1 s of breaking) |
| Instant Transmission (`SaiyanTechniques.transmit`) | The user vanishes with a flicker ("shunk") and reappears at the target | `KI_AFTERIMAGE` sent at both ends: the figure squeezes thin and flickers out, two afterimages, vertical speed lines, a flash ring at the waist | 2 (the two ends can be up to 64 blocks apart) |
| Spirit Bomb (`SaiyanTechniques` bomb*) | A huge blue-white sphere raised overhead, gathering energy from all living things, thrown down, with a massive blast | `KI_SPHERE` gathering: solid ball, two counter-turning swirls, motes streaming in. `KI_SPHERE` thrown: flies from the head to the target. `KI_SHOCKWAVE` where it lands | gather: 2 a second; throw: 1; landing: 1 |
| Meteor Combination (`SaiyanTechniques.meteor`) | A rapid rush of teleporting blows that ends in a heavy finisher | `KI_RUSH` round the foe: a flash for each blow with the attacker's afterimage on the far side and speed lines. `KI_SHOCKWAVE` on the last blow | 1 at the start, 1 on the last blow |
| Dragon Fist (`SaiyanTechniques3.dragon`) | Goku punches through the foe while a giant golden dragon rises, coiling round it | `KI_DRAGON`: a golden serpent (shaded scale body, horned head) spirals up round the foe and bursts at the top on the last blow (star flash, ring, camera shake) | 1 |
| God Bind (`SaiyanTechniques3.bind`) | A divine technique that freezes the foe in place; drawn here as golden binding rings (least certain entry) | `KI_BIND`: three golden rings snap tight round the body one after another, a turning seal on the ground, a faint column of light | 1 |
| Ki Deflection (`SaiyanTechniques3.deflect`) | Batting incoming blasts away with a ki-charged hand | `KI_DEFLECT`: two spinning ki hoops round the body, a swirl at the waist, a faint ring on the ground at the deflection radius | 1 |
| Energy Transfer (`SaiyanTechniques3.energy`) | Handing ki to an ally, as when Goku shares energy | `KI_TRANSFER`: a waving stream with motes running along it; the ally lights up with a ring | 1 |

Not in this wave: the Goku awakening burst, the form transformations (`SaiyanAura` already draws those), flight, and the per-projectile deflection sparks.
Those keep their particles.

## The classic setting

The setting is `saiyan.classicVfx` in `config/nusmp-saiyan-look-client.toml`. It is a CLIENT setting (looks only) and defaults to `false`.

The server always sends the new shapes, and it still sends the old particle sprays exactly as before; nothing on the server was removed. With `classicVfx = true`,
`KiLayer` draws nothing, so the player sees exactly the 3.89 look. This is the simplest correct way to do it:

- A per-player server switch would need a new packet. This needs none.
- Gameplay is unchanged either way.

With the setting off (the default), the old sprays stay as a light accent under the new shapes.

## Cost (rule 11)

**Server**

- No entity, no block change and no new query.
- Each cast adds 1 or 2 effect packets, about 60 bytes each. They go through `VfxSpawn.send`, which applies the per-tick effect budget, to players within 96 blocks only.
- The three held or standing effects re-send a short copy:

  | Effect | Re-sends while active | Hard bound |
  |---|---|---|
  | Kamehameha charge | 2 packets a second | bounded by the 5-second safety release after a full charge |
  | Spirit Bomb gather | 2 packets a second | the same safety release |
  | Ki Barrier | 1 packet a second | `barrierSeconds`, at most 60 |

- Nothing is ever sent per tick per target.

**Client**

- Each effect is one activation under the engine's 400-vertex (100-quad) cap. The layer keeps the default cap.
- Worst-case quads per effect, counted from the code (the preview measured at most 70):

  | Effect | Quads |
  |---|---|
  | BLAST | 42 (8 orbs) |
  | BEAM | 75 |
  | CHARGE | 18 |
  | SPHERE | 46 |
  | SHOCKWAVE | 72 |
  | AFTERIMAGE | 32 |
  | RUSH | 44 |
  | STRIKE | 60 |
  | BARRIER | 26 |
  | DEFLECT | 54 |
  | BIND | 68 |
  | DRAGON | 64 |
  | TRANSFER | 31 |

- At most 2 copies of a cross-faded effect overlap, for 4 ticks.
- The manager's existing caps apply: `maxActive` per quality level, distance level of detail, culling, and the per-frame vertex allowance.
- Textures are static final fields, so no `ResourceLocation` is built per frame (`PerfSourceScanTest` lists `KiLayer` now). No state is kept between frames, so nothing needs cleaning up on logout, death or dimension change.
- A followed effect whose creature is gone draws nothing.

## NEW / REPLACED / KEPT

**NEW**

- `KiLayer`, `KiVfx`, `SaiyanKiFx`, `SaiyanForms.kiColor`
- the 13 `KI_*` shapes and the 15 `ki_*` textures, with their generator
- the preview scenes and the preview PNGs
- the client setting `saiyan.classicVfx`
- the self-test `saiyan.kivfx` (added to `SMOKE_EXPECT`)
- the tests `KiVfxAssetTest` and `SaiyanKiColorTest`

**REPLACED**: nothing was removed. The Ki effects are a new look on top of the techniques. The particle-only look is still there, behind `classicVfx = true`.

**KEPT**

- every old `Fx` spray in the Saiyan code, every old shape and texture, and every old layer
- the Time Magic files
- `DbSkills` / `DbMoves`, untouched

`NenShapesTest` was updated: `NEN_DOMAIN` is no longer the last shape, so the test now pins it to be followed by `KI_BLAST`.

## Caveats

- None of this has been seen in the game. It has only been rendered by the headless preview, which has no world lighting and no depth against blocks.
- Gradle could not be run here, so the Minecraft-side code was checked only by reading it. `KiLayer` itself was compiled by the preview against stubs.
- Meteor Combination: the timing of the blows is reproduced on the client (hits × interval), so a blow that lands late on the server, after a catch-up teleport, can look a tick or two off.
- God Bind: if the binder dies while the foe is held, the rings stay until their time is up. They disappear at once if the foe dies.
