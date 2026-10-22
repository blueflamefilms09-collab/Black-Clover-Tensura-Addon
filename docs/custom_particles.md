# Custom particles (1.103.0)

The owner's request: make the mod's own particles instead of reusing the vanilla ones, and let the Rukh (the magoi) belong to the Djinn and the dungeons only (`docs/rukh/exclusive.md`).
Everything below was built without a running game: nothing here has been looked at in game (see the last section).

## What is in it

| Part | Where |
|---|---|
| 70 particle types in 21 groups, 4 to 8 frames each, 16x16 and 32x32 pixel art, additive glows | `particle/fx` (`FxTypes`), textures `assets/nusmp/textures/particle/<id>_<n>.png`, sprite lists `assets/nusmp/particles/<id>.json` |
| One particle class with a physics profile per type (gravity, drag, spin, swirl, sway, jitter, size curve, fade, light, blend, lifetime) | `client/particle/fx/FxParticle`, profiles in `data/nusmp/particle_catalog.json` (written by the generator) |
| One provider for all types, one atlas lookup list per type, nothing allocated per particle but the particle | `client/particle/fx/FxClient` |
| Tint payload (colour + size) with a codec and a stream codec | `TintedParticleOptions`, `TintedParticleType`, pure data in `FxTint` |
| Theme table: 295 themes (all 105 magic types, 7 relic grades, 21 relic elements, 100 relic waves, skills, bosses, dungeon areas), 1 to 4 particles + a colour + a dark flavour each | `data/nusmp/particle_themes.json` (written by `tools/particles/gen_themes.py`), `ParticleThemes`, `ThemeKey` |
| The facade `Fx` (spray / add / burst / stream / gather / aura / send / ring) through the perf budgets, the quality level and the distance | `particle/fx/Fx`, `FxRelic` (relics by colour hue) |
| Switches `particles.custom` (client file `nusmp-particles-client.toml`, server file `nusmp-particles-server.toml`, default true); off = the old vanilla particle of the mapping (Rukh for the calls that used the Rukh) | `FxConfig` |
| `/multiverse particles list [group]`, `spawn <id> [count] [color]`, `theme <key>`, `status` (operators) | `FxCommands` |
| The re-runnable codemod and its map | `tools/particles/codemod.py`, `tools/particles/map.json` |
| Generators | `tools/gen_custom_particles.py` (textures, jsons, catalog, sheet), `tools/particles/gen_themes.py`, `tools/particles/gen_docs.py` (this file) |
| Preview sheet (every frame, dark and light background) | `docs/previews/custom_particles/sheet.png` |
| Self-test `particles.custom` (CI server smoke), lang fragment `tools/lang/custom_particles.json` | `FxSelfTests` |

## Using it from code

```java
Fx.send(level, FxThemes.BOSS_ZENON, x, y, z, 24, 0.6);                      // a puff of the theme's particles, shared over its 2-4 particles
Fx.ring(level, ThemeKey.of("magic.FLAME"), cx, cy, cz, 3.0, 24);             // a ring of its signature particle
Fx.spray(level, FxThemes.SKILL, FxTone.DARK, x, y, z, 6, 0.2, 0.2, 0.2, 0.02); // the dark flavour of the theme
Fx.options(ThemeKey.of("relic.grade.GOD"), FxTone.LIGHT);                    // the ParticleOptions, for code that builds its own packets
```

Every call goes through `PerfBudgets` (server: `maxParticlesPerTick`; client: `clientParticlesPerTick`), thins the count by `vfxQuality` (low 0.5, medium 0.75) and, on the client, by the distance
from the camera (the quality's near/far bands, nothing past the cull distance), and caps the count per call by quality (48 / 140 / 400). The Rukh-style emitters keep the caps the Rukh had (12 per burst, 6 per stream tick, 12 per
gather, 2 per aura pulse) so swapping a Rukh call for a themed call does not change how many particles an ability shows.

## The particles

Status: every particle in this table is **NEW** (nothing like it existed). Frames and size are the files on disk; `tint` means the texture is neutral light so the theme colour dyes it.

### fire (6)

| id | status | frames | px | tint | behaviour | used by (themes) |
|---|---|---|---|---|---|---|
| `ember` | NEW | 6 | 16 | no | A small glowing coal that flickers while it drifts up; gets hot-yellow in the middle, fades to red. Rises, light drag, additive, full-bright. | `magic.FLAME`, `magic.EXPLOSION`, `magic.MAGMA`, `magic.BEAST` +34 more |
| `cinder` | NEW | 6 | 16 | no | A burnt-out lump with glowing cracks that pulse; tumbles, falls slowly, translucent, lit by its own cracks. | `magic.FLAME`, `magic.EXPLOSION`, `magic.MAGMA`, `magic.EARTH` +71 more |
| `flame_tongue` | NEW | 6 | 16 | no | A tongue of fire (red edge, orange body, white-yellow heart) that licks upwards and sways; rises, shrinks to nothing. | `magic.FLAME`, `relic.element.fire`, `relic.wave.0`, `relic.wave.21` +5 more ; replaces vanilla FLAME, SMALL_FLAME |
| `flame_soul` | NEW | 6 | 16 | yes | The same tongue in pale spirit-fire (white body, tintable): a cold flame for souls, ghosts and blue fire; drifts up with a little sway. | `magic.DEMON_FIRE`, `relic.element.demon`, `boss.dante`, `relic.wave.19` +3 more ; replaces vanilla SOUL_FIRE_FLAME |
| `spark` | NEW | 4 | 16 | no | A hot spark: a bright four-ray burst that shortens as it cools; flies fast, falls a little, drag. | `magic.FLAME`, `magic.EXPLOSION`, `magic.STORM`, `magic.LIGHTNING` +59 more |
| `lava_blob` | NEW | 6 | 16 | no | A bubble of molten rock: dark crust plates over a bright orange core with a yellow hot spot, swelling and cooling as it falls; lit by its own glow, translucent. | `magic.MAGMA` ; replaces vanilla LAVA, FALLING_LAVA, DRIPPING_LAVA |

### ash (3)

| id | status | frames | px | tint | behaviour | used by (themes) |
|---|---|---|---|---|---|---|
| `ash_flake` | NEW | 6 | 16 | no | A thin grey ash flake that tumbles and flips as it sinks; matte, no glow. | `magic.EARTH`, `magic.SAND`, `magic.SWORD`, `magic.REINFORCEMENT` +49 more ; replaces vanilla ASH |
| `soot_puff` | NEW | 6 | 16 | no | A dark soot puff that swells and thins out while rising slowly; dithered lumpy edge, translucent, never glows. | `magic.FLAME`, `magic.EXPLOSION`, `magic.MAGMA`, `magic.WATER` +219 more |
| `smoke_wisp` | NEW | 6 | 16 | yes | A curling wisp of smoke (light grey, tintable); rises in an S shape that unwinds, translucent, fades. | `magic.MIST`, `magic.WIND`, `magic.STAR`, `magic.STORM` +29 more |

### light (13)

| id | status | frames | px | tint | behaviour | used by (themes) |
|---|---|---|---|---|---|---|
| `mote` | NEW | 6 | 16 | yes | The basic light mote: a soft round glow with a hard white core that breathes; drifts, slowly rising, additive, the theme colour tints it. | `magic.MIST`, `magic.WIND`, `magic.STAR`, `magic.LIGHT` +50 more |
| `mote_soft` | NEW | 4 | 16 | yes | A large dim glow without a core: an air-filling haze mote; very slow, long fade in and out, additive. | `magic.WATER`, `magic.ICE`, `magic.MERCURY`, `magic.MIST` +130 more |
| `glint` | NEW | 6 | 16 | yes | A lens glint: a thin four-armed cross that swells and snaps shut, with a tilt; sits where it is spawned, additive. | `magic.WATER`, `magic.ICE`, `magic.MERCURY`, `magic.WIND` +118 more |
| `star4` | NEW | 6 | 16 | yes | A solid four-point star that turns a quarter turn while it twinkles and shrinks; slow rise, additive. | `magic.STAR`, `magic.LIGHT`, `magic.SWORD`, `magic.MIRROR` +44 more |
| `gem_sparkle` | NEW | 6 | 16 | yes | A tiny cut gem with three shaded facets that flashes a cross glint at its brightest frame; hovers, additive. | `magic.CREATION`, `magic.TRANSMUTATION`, `magic.RECOMBINATION`, `magic.GAME` +26 more |
| `mana_mote` | NEW | 8 | 16 | yes | A mote of raw mana: a glow with a tiny satellite circling it; the satellite leaves a faint ring. Rises gently, additive. | `magic.MERCURY`, `magic.SPATIAL`, `magic.GRAVITY`, `magic.COPY` +40 more |
| `aura_wisp` | NEW | 6 | 16 | yes | A comet-shaped aura wisp: a round head and a tail that waves; rises and bends, additive. | `magic.VORTEX` |
| `spirit_firefly` | NEW | 8 | 16 | yes | A spirit firefly: a small warm body whose glow blinks on and off in a slow rhythm and whose wings flick; wanders on a lazy path, additive. | `magic.LEGION`, `magic.SOUL`, `relic.element.soul`, `dungeon.safe` +4 more |
| `dream_star` | NEW | 6 | 16 | no | A pastel five-point star with two tiny sparkles that trade places; floats up and sways, twinkles, additive. Native lavender-pink. | `magic.STAR`, `magic.CREATION`, `magic.COPY`, `magic.ILLUSION` +31 more |
| `holy_ray` | NEW | 6 | 32 | yes | A sunburst of holy rays: eight shafts that turn slowly and breathe around a white core; stays in place, additive, long soft fade. | (new code: `/multiverse particles spawn holy_ray`) |
| `nebula_puff` | NEW | 6 | 32 | no | A cloud of star dust in violet, blue and pink: three drifting lobes of noise with bright pin stars; swells and thins, slow spin, additive. | `magic.SPATIAL`, `magic.MIRROR`, `magic.GRAVITY`, `magic.TIME` +66 more |
| `void_speck` | NEW | 4 | 16 | no | A speck of nothing: a black core with a thin violet rim that pulses; sucks slowly sideways, translucent so it stays dark. | `magic.FLAME`, `magic.EXPLOSION`, `magic.MAGMA`, `magic.WATER` +266 more |
| `soul_wisp` | NEW | 8 | 16 | yes | A ghostly wisp with a round head, two dark eyes that blink and a wavy tail; floats up with a lazy sway, additive. | `magic.WIND`, `magic.STAR`, `magic.STORM`, `magic.LIGHT` +132 more |

### pulse (5)

| id | status | frames | px | tint | behaviour | used by (themes) |
|---|---|---|---|---|---|---|
| `ring_pulse` | NEW | 8 | 32 | yes | A thin ring that leaves the spot and widens fast while it thins and fades: the shape of a pulse or a cast. Stays in place, additive. | (new code: `/multiverse particles spawn ring_pulse`) |
| `halo_ring` | NEW | 6 | 32 | yes | A halo: a thick glowing ring with a thin echo inside and eight sparkles on it; holds its size and pulses, then dims. Stays in place, additive. | (new code: `/multiverse particles spawn halo_ring`) |
| `shock_ring` | NEW | 6 | 32 | yes | A hard shock ring: a ragged thick band that races outward and thins to a line; the hit of a stomp or a blast. Stays in place, additive. | the codemod's fixed replacements ; replaces vanilla SONIC_BOOM |
| `impact_burst` | NEW | 6 | 32 | yes | A comic impact star: a ten-spike burst that flashes at full size and opens into a ring of spikes as it fades. Stays in place, additive. | the codemod's fixed replacements ; replaces vanilla EXPLOSION, EXPLOSION_EMITTER |
| `circle_flash` | NEW | 8 | 32 | yes | A magic circle that snaps open: two rings, a hexagram and tick marks that turn a little while they flash and fade. Lies where it is spawned, additive. | the codemod's fixed replacements ; replaces vanilla FLASH |

### storm (2)

| id | status | frames | px | tint | behaviour | used by (themes) |
|---|---|---|---|---|---|---|
| `lightning_arc` | NEW | 6 | 32 | yes | A jagged bolt that cracks across the sprite and re-rolls its path every frame (flicker), with a short branch and a pale halo. Stays in place, additive, very short. | (new code: `/multiverse particles spawn lightning_arc`) |
| `electric_orb` | NEW | 8 | 16 | yes | A small orb of static with short jagged arcs that jump to new places every frame; hovers and jitters, additive, very bright. | `magic.STORM`, `magic.LIGHTNING`, `magic.COPPER`, `relic.element.storm` +7 more |

### rune (8)

| id | status | frames | px | tint | behaviour | used by (themes) |
|---|---|---|---|---|---|---|
| `rune_sol` | NEW | 6 | 16 | yes | Rune glyph, the sun glyph: a ringed dot with eight ticks. Fades in, glows steady, fades out while rising a hair; a faint ring of dashes around it. Stays in place, additive. | `magic.SEALING`, `magic.COMPASS`, `magic.KOTODAMA`, `magic.CURSE_WARDING` +20 more |
| `rune_luna` | NEW | 6 | 16 | yes | Rune glyph, the moon glyph: a crescent with a star. Fades in, glows steady, fades out while rising a hair; a faint ring of dashes around it. Stays in place, additive. | `magic.LIGHT`, `magic.LIGHTNING`, `magic.DARK`, `magic.KOTODAMA` +41 more |
| `rune_eye` | NEW | 6 | 16 | yes | Rune glyph, the eye glyph: an almond with a pupil and a brow tick. Fades in, glows steady, fades out while rising a hair; a faint ring of dashes around it. Stays in place, additive. | `magic.SPATIAL`, `magic.MIRROR`, `magic.GRAVITY`, `magic.TIME` +39 more |
| `rune_wave` | NEW | 6 | 16 | yes | Rune glyph, the water glyph: three waves. Fades in, glows steady, fades out while rising a hair; a faint ring of dashes around it. Stays in place, additive. | `magic.PERMEATION`, `magic.SOUND` |
| `rune_flame` | NEW | 6 | 16 | yes | Rune glyph, the fire glyph: a triangle with a flame bar. Fades in, glows steady, fades out while rising a hair; a faint ring of dashes around it. Stays in place, additive. | `dungeon.crucible` |
| `rune_leaf` | NEW | 6 | 16 | yes | Rune glyph, the life glyph: a leaf with a midrib. Fades in, glows steady, fades out while rising a hair; a faint ring of dashes around it. Stays in place, additive. | `magic.WORLD_TREE` |
| `rune_bolt` | NEW | 6 | 16 | yes | Rune glyph, the storm glyph: a zigzag bolt. Fades in, glows steady, fades out while rising a hair; a faint ring of dashes around it. Stays in place, additive. | `magic.STORM`, `magic.LIGHTNING`, `relic.element.storm`, `dungeon.trial` +7 more |
| `rune_key` | NEW | 6 | 16 | yes | Rune glyph, the key glyph: a looped key with two teeth. Fades in, glows steady, fades out while rising a hair; a faint ring of dashes around it. Stays in place, additive. | `magic.SPATIAL`, `magic.SEALING`, `magic.CURSE_WARDING`, `magic.KEY` +10 more |

### combat (1)

| id | status | frames | px | tint | behaviour | used by (themes) |
|---|---|---|---|---|---|---|
| `slash_streak` | NEW | 6 | 32 | yes | A sword slash: a tapering crescent that sweeps open over the first frames and then thins out; white edge, pale body. Stays in place, additive, very short. | the codemod's fixed replacements ; replaces vanilla SWEEP_ATTACK |

### air (1)

| id | status | frames | px | tint | behaviour | used by (themes) |
|---|---|---|---|---|---|---|
| `wind_swirl` | NEW | 8 | 32 | yes | A double-armed spiral of thin wind lines that turns while it opens up and fades. Stays in place, additive. | (new code: `/multiverse particles spawn wind_swirl`) |

### shard (3)

| id | status | frames | px | tint | behaviour | used by (themes) |
|---|---|---|---|---|---|---|
| `shard_ice` | NEW | 6 | 16 | no | A sliver of ice with three facets that spins end over end and flashes a glint on one frame; falls with a little bounce, translucent. | `magic.ICE`, `magic.DEMON_ICE`, `magic.ICE_WEDGE`, `relic.element.ice` +8 more |
| `shard_crystal` | NEW | 6 | 16 | yes | A faceted crystal chunk (six-sided, light neutral facets so the theme colour reads) that turns slowly; drifts down, glints, translucent. | `magic.EARTH`, `magic.CRYSTAL`, `magic.CORUNDUM`, `magic.MINERAL` +7 more |
| `shard_glass` | NEW | 6 | 16 | yes | A broken pane fragment: a see-through triangle with bright edges and a diagonal shine that slides across it; tumbles and falls, translucent. | `magic.MERCURY`, `magic.MIRROR`, `magic.STEEL`, `magic.SLASH` +11 more |

### feather (3)

| id | status | frames | px | tint | behaviour | used by (themes) |
|---|---|---|---|---|---|---|
| `feather_white` | NEW | 6 | 16 | yes | A white feather that turns over as it sways down; flat shaded, a grey spine and finely notched barbs, slow fall. | `magic.WIND`, `magic.AIR`, `magic.WING`, `relic.element.wind` +14 more |
| `feather_black` | NEW | 6 | 16 | no | A black feather with a blue-violet sheen on the edge; same lazy sway and fall as the white one, never glows. | `magic.WIND`, `magic.STAR`, `magic.STORM`, `magic.AIR` +18 more |
| `feather_fire` | NEW | 6 | 16 | no | A feather of living fire (red edge, orange vane, yellow spine) that trails a soft glow; floats up with a flutter and burns out, additive. | (new code: `/multiverse particles spawn feather_fire`) |

### flora (6)

| id | status | frames | px | tint | behaviour | used by (themes) |
|---|---|---|---|---|---|---|
| `petal` | NEW | 6 | 16 | yes | A soft petal in a neutral light tone so the theme colour dyes it; flips as it flutters down, a gentle sway, translucent. | `magic.PLANT`, `magic.COTTON`, `magic.BUTOH`, `magic.CHERRY_BLOSSOM` +10 more |
| `petal_cherry` | NEW | 6 | 16 | no | A cherry blossom petal (pink, notched at the tip, darker at the base) that spins and sways down; the Cherry Blossom magic's own. | `magic.CHERRY_BLOSSOM` |
| `leaf_green` | NEW | 6 | 16 | no | A green leaf with a pale midrib and veins; turns over as it tumbles down, quicker fall than a petal, translucent. | `magic.PLANT`, `magic.WORLD_TREE`, `magic.BRIAR`, `magic.POISON_PLANT` +11 more |
| `leaf_autumn` | NEW | 6 | 16 | no | An autumn leaf (red, orange and gold) with dark veins, same tumble as the green one but drier and slower. | `magic.PLANT`, `magic.WORLD_TREE`, `magic.BRIAR`, `magic.CHERRY_BLOSSOM` +15 more |
| `seed` | NEW | 6 | 16 | no | A winged tree seed (maple key): a seed and one long wing that whirls around the seed like a helicopter blade while it sinks. | `magic.PLANT`, `magic.WORLD_TREE`, `magic.BRIAR`, `magic.FOOD` +10 more |
| `spore` | NEW | 6 | 16 | no | A drifting spore: a pale yellow-green puff with a few dust specks orbiting it; floats up and wobbles, additive, soft. | `magic.PLANT`, `magic.POISON`, `magic.WORLD_TREE`, `magic.BRIAR` +23 more |

### water (4)

| id | status | frames | px | tint | behaviour | used by (themes) |
|---|---|---|---|---|---|---|
| `bubble` | NEW | 6 | 16 | no | A soap bubble: a thin blue ring with a bright glint, wobbling and growing slightly; the last frame is the pop (a ring of droplets). Rises slowly, translucent. | `magic.WATER`, `magic.POISON`, `magic.DEMON_WATER`, `magic.FUNGUS` +20 more ; replaces vanilla BUBBLE, BUBBLE_POP, BUBBLE_COLUMN_UP |
| `droplet` | NEW | 4 | 16 | no | A water drop: a teardrop that stretches as it falls and, on the last frame, flattens into a splash ring with two satellites. Falls fast, translucent. | `magic.WATER`, `magic.ICE`, `magic.MERCURY`, `magic.MIST` +33 more ; replaces vanilla CURRENT_DOWN, SPLASH |
| `water_splash` | NEW | 6 | 32 | yes | A splash crown: nine arcs of water that fly out and up from the spot with a bead at each tip, then fall back; a thin ring on the ground; stays at its spot, translucent, short. | (new code: `/multiverse particles spawn water_splash`) |
| `snowflake` | NEW | 6 | 16 | yes | A six-armed snow crystal that turns slowly while it sinks, with a little sway; crisp white, translucent. | `magic.ICE`, `magic.DEMON_ICE`, `magic.ICE_WEDGE`, `magic.SNOW` +9 more ; replaces vanilla SNOWFLAKE |

### earth (3)

| id | status | frames | px | tint | behaviour | used by (themes) |
|---|---|---|---|---|---|---|
| `sand_grain` | NEW | 4 | 16 | no | A pinch of sand grains that tumble: five to six single-texel specks in warm tans that swap places each frame; falls fast, collides, translucent. | `magic.EARTH`, `magic.SAND`, `magic.MUD`, `magic.ROCK` +8 more |
| `earth_chip` | NEW | 6 | 16 | no | A chunk of dirt and rock with a light top and dark underside that tumbles; heavy, bounces on the ground, opaque. | `magic.EARTH`, `magic.SAND`, `magic.GRAVITY`, `magic.REINFORCEMENT` +27 more |
| `bone_chip` | NEW | 6 | 16 | no | A sliver of bone: an off-white splinter with a knobbed end that tumbles; falls and bounces, opaque. | `magic.BEAST`, `magic.BONE`, `magic.DEMON_BEAST`, `magic.SOUL_CORPSE` +2 more |

### mechanism (3)

| id | status | frames | px | tint | behaviour | used by (themes) |
|---|---|---|---|---|---|---|
| `cog` | NEW | 6 | 16 | no | A brass cog with eight teeth and an axle hole that turns steadily; drifts down, translucent. The gear of clockwork and machines. | `magic.TRANSMUTATION`, `magic.BRONZE`, `magic.COPPER`, `magic.MODIFICATION` +7 more |
| `clock_tick` | NEW | 6 | 16 | no | A small brass clock face whose hand jumps one step each frame, with a brief ring pulse on every tick; hovers and fades, translucent. (The Time Magic effects are untouched; this is a plain tick glyph.) | `magic.TIME`, `relic.element.time`, `relic.wave.12`, `relic.wave.42` +2 more |
| `chain_link` | NEW | 6 | 16 | no | An iron chain link that turns about its long axis: a full oval ring, a narrow ring, then the edge-on bar; dark steel with a bright edge. Falls with a spin, translucent, hits the ground. | `magic.CHAIN`, `magic.IRON`, `magic.NAIL`, `relic.element.metal` +7 more |

### paper (2)

| id | status | frames | px | tint | behaviour | used by (themes) |
|---|---|---|---|---|---|---|
| `card` | NEW | 6 | 16 | no | A playing card flipping through the air: front with a red diamond, back with a blue lattice; turns about its long axis, falls with a sway, translucent. | `magic.DICE`, `magic.GAME` |
| `page` | NEW | 6 | 16 | no | A loose page: parchment with lines of script and a curling corner that flaps as it spirals down; slow, swaying, translucent. | `magic.COPY`, `magic.MEMORY`, `arena.chronicle` |

### ink (2)

| id | status | frames | px | tint | behaviour | used by (themes) |
|---|---|---|---|---|---|---|
| `ink_splat` | NEW | 6 | 16 | no | A blot of black ink: starts as a dot, spreads into an irregular splat with satellite droplets and a drip, with a wet highlight; hardly moves, translucent, fades as it dries. | `magic.WATER`, `magic.ICE`, `magic.MERCURY`, `magic.MIST` +26 more |
| `paint_splat` | NEW | 6 | 16 | yes | A splat of paint in a neutral light tone (the theme colour dyes it) with satellite drops and a drip; spreads and settles, translucent. | `magic.PAINTING`, `magic.RED_OCHRE` |

### thread (1)

| id | status | frames | px | tint | behaviour | used by (themes) |
|---|---|---|---|---|---|---|
| `thread` | NEW | 6 | 16 | yes | A loose thread: a thin curling strand with a bright edge that wriggles while it drifts; tintable, translucent. | `magic.THREAD`, `magic.COTTON`, `magic.PAINTING`, `magic.HAIR` +2 more |

### music (1)

| id | status | frames | px | tint | behaviour | used by (themes) |
|---|---|---|---|---|---|---|
| `music_note` | NEW | 6 | 16 | yes | A music note (a single eighth on three frames, a beamed pair on three, picked at random per particle) that bobs while it floats up and fades; tintable, translucent. | `magic.BUTOH`, `magic.SONG`, `magic.SOUND`, `relic.element.music` +4 more |

### blood (1)

| id | status | frames | px | tint | behaviour | used by (themes) |
|---|---|---|---|---|---|---|
| `blood_drop` | NEW | 4 | 16 | no | A stylised blood drop: a deep-red teardrop with a wet highlight that falls fast and, on its last frame, flattens into a small splat with two satellites. Opaque look, translucent render. | `magic.SWORD`, `magic.REINFORCEMENT`, `magic.BEAST`, `magic.BONE` +30 more |

### poison (1)

| id | status | frames | px | tint | behaviour | used by (themes) |
|---|---|---|---|---|---|---|
| `poison_cloud` | NEW | 6 | 32 | no | A toxic cloud: lumpy green fog with rising bubbles and a sick yellow rim, billowing slowly; spreads while it thins, translucent but full-bright so it reads in the dark. | `magic.PLANT`, `magic.POISON`, `magic.WORLD_TREE`, `magic.BRIAR` +21 more |

### shadow (1)

| id | status | frames | px | tint | behaviour | used by (themes) |
|---|---|---|---|---|---|---|
| `shadow_tendril` | NEW | 6 | 32 | no | A curling tendril of shadow that reaches up from the bottom and coils at the tip, with a violet edge light; sways and retracts, translucent, never lit. | `magic.DARK`, `magic.SHADOW`, `magic.POISON`, `magic.ANTI_MAGIC` +46 more |

## Replaced, kept, new

| What | Status |
|---|---|
| The 70 custom particle types and their textures, jsons, catalog | NEW |
| Vanilla `ParticleTypes` calls (`sendParticles`, `addParticle`, helper arguments, switch results) in 76 files outside the Djinn, relic and rift lanes | REPLACED by `Fx` calls (the old particle stays as the `particles.custom=false` fallback in the generated `FxOld`) |
| Light/Dark Rukh calls (`RukhFx`, `RukhClientFx`, `RukhParticles`, `RukhFx.Alignment`) outside the Djinn and the dungeons | REPLACED by `Fx` / `FxTone` / `FxRelic` |
| The Rukh types, textures, emitters, purge scope and tests | KEPT (the Djinn and the dungeons use them; `particles.custom=false` falls back to them) |
| Rukh in `relics/djinn/**`, `client/relics/djinn/**`, `relics/Fx.java` (the Djinn's shared helper), `labyrinth/**`, `client/labyrinth/**`, `dungeon/**`, `client/dungeon/**` | KEPT |
| Vanilla particles that carry data: block and falling-dust options (`BlockParticleOption`), item particles, dust options held in a variable or built by a helper | KEPT (reasons below) |
| Time Magic: `book/TimeBook.java`, `vfx/client/layer/TimeMagicLayer.java`, `textures/particle/time_*.png`, `tools/gen_time_vfx_textures.py` | KEPT, untouched (the codemod's skip list; a test pins it) |
| The textured-quad VFX layers (`vfx/**`) | KEPT (not particle types) |
| `MagicType.particle()` / `MagicProfile.particle()` | CHANGED type: `SimpleParticleType` became `ParticleOptions`; each of the 105 magics now answers with its own theme's particle |

## The themes

A theme is `[mote, accent, spark, ambient]` (`FxSlot`) plus a colour and a dark flavour. The codemod maps `END_ROD`, `GLOW`... to the MOTE slot, `ENCHANT`, `PORTAL`... to ACCENT, `CRIT`, `ELECTRIC_SPARK`... to SPARK,
`SMOKE`, `CLOUD`, `POOF`... to AMBIENT; elemental ones keep their element whatever the theme (a `FLAME` is a `flame_tongue`, `SNOWFLAKE` a `snowflake`) and the one-shot shapes become `impact_burst`, `circle_flash`, `slash_streak`,
`shock_ring` in the theme colour (`tools/particles/map.json`, section `particles`). Counts: arena 2, boss 1, boss.abyssal 1, boss.avatar 1, boss.celestial 1, boss.charybdis 1, boss.dante 1, boss.elsdocia 1, boss.goliath 1, boss.lucius 1, boss.vanica 1, boss.zagred 1, boss.zenon 1, corpse 1, dungeon 1, dungeon.crucible 1, dungeon.decennial 1, dungeon.gate 1, dungeon.guardian 1, dungeon.humanoid 1, dungeon.lock 1, dungeon.rift 1, dungeon.safe 1, dungeon.seal 1, dungeon.squad 1, dungeon.trap 1, dungeon.trial 1, dungeon.vault 1, grimoire 1, item 4, magic 105, relic 1, relic.element 21, relic.forge 1, relic.grade 7, relic.order 1, relic.pair 1, relic.socket 1, relic.wave 111, skill 1, skill.blackwings 1, skill.celestial 1, skill.god 1, skill.grimoire 1, skill.paladin 1, skill.requip 1, skill.shishou 1, skill.sword 1, skill.tree 1, wizard 1, world 1.

### The 105 magic types

| magic | particles (mote, accent, spark, ambient) | colour |
|---|---|---|
| FLAME | ember, flame_tongue, spark, soot_puff | `#FF7A2E` |
| EXPLOSION | spark, cinder, ember, soot_puff | `#FFB02E` |
| MAGMA | lava_blob, ember, cinder, soot_puff | `#FF4A1C` |
| WATER | droplet, bubble, glint, mote_soft | `#3F8FFF` |
| ICE | snowflake, shard_ice, glint, mote_soft | `#9FE0FF` |
| MERCURY | mana_mote, shard_glass, glint, mote_soft | `#C7D2E8` |
| MIST | mote_soft, smoke_wisp, droplet, mote | `#CFE3F2` |
| WIND | mote, feather_white, glint, smoke_wisp | `#BFF5E0` |
| STAR | star4, glint, dream_star, mote | `#FFF2A8` |
| STORM | electric_orb, rune_bolt, spark, smoke_wisp | `#7AA8FF` |
| EARTH | earth_chip, sand_grain, shard_crystal, ash_flake | `#9A7A4E` |
| PLANT | leaf_green, spore, seed, petal | `#5FCB4F` |
| SAND | sand_grain, earth_chip, glint, ash_flake | `#E6C887` |
| LIGHT | mote, glint, star4, mote_soft | `#FFF6C8` |
| LIGHTNING | electric_orb, rune_bolt, spark, smoke_wisp | `#FFE45E` |
| SWORD | glint, star4, spark, mote_soft | `#DDE6F5` |
| DARK | void_speck, rune_luna, soul_wisp, soot_puff | `#7A3FCB` |
| SHADOW | void_speck, shadow_tendril, soot_puff, smoke_wisp | `#4A2F7A` |
| POISON | spore, bubble, droplet, poison_cloud | `#7BD13A` |
| SPATIAL | mana_mote, rune_key, glint, nebula_puff | `#8F5BFF` |
| MIRROR | glint, shard_glass, star4, mote_soft | `#D9ECFF` |
| GRAVITY | void_speck, earth_chip, mana_mote, soot_puff | `#5A3FA0` |
| TIME | mote, clock_tick, glint, mote_soft | `#BFA4FF` |
| SEALING | rune_key, rune_sol, glint, mote_soft | `#E8D28C` |
| REINFORCEMENT | mote, spark, earth_chip, smoke_wisp | `#FFB84D` |
| BEAST | ember, bone_chip, spark, ash_flake | `#D98A3A` |
| BONE | bone_chip, ash_flake, cinder, soot_puff | `#E8E0CC` |
| BLOOD | blood_drop, void_speck, ember, soot_puff | `#A8182B` |
| CREATION | mote, gem_sparkle, star4, mote_soft | `#FFD27A` |
| COPY | mana_mote, page, glint, mote_soft | `#7FD6FF` |
| ILLUSION | dream_star, soul_wisp, glint, nebula_puff | `#C48BFF` |
| DREAM | dream_star, nebula_puff, star4, mote_soft | `#F5B8FF` |
| ANTI_MAGIC | void_speck, rune_eye, ash_flake, soot_puff | `#3A3A44` |
| STEEL | glint, shard_glass, spark, ash_flake | `#B8C2D2` |
| THREAD | thread, mote, glint, mote_soft | `#F2D4A0` |
| TRANSMUTATION | mana_mote, cog, gem_sparkle, smoke_wisp | `#D9A441` |
| ASH | ash_flake, cinder, ember, soot_puff | `#9A9AA6` |
| COTTON | mote_soft, thread, petal, smoke_wisp | `#F4F0FF` |
| RECOMBINATION | mana_mote, gem_sparkle, spark, mote_soft | `#FF8FD0` |
| PAINTING | paint_splat, thread, glint, ink_splat | `#FF6FB0` |
| WORLD_TREE | leaf_green, rune_leaf, seed, spore | `#6FE08A` |
| DICE | glint, card, spark, mote_soft | `#FFE08A` |
| SLASH | glint, spark, shard_glass, mote_soft | `#E0E6F0` |
| COMPASS | rune_sol, glint, mana_mote, mote_soft | `#9FD6FF` |
| KOTODAMA | mote, rune_sol, rune_luna, mote_soft | `#FFFFFF` |
| GAME | card, gem_sparkle, spark, mote_soft | `#6FFFA8` |
| DEMON_BEAST | bone_chip, ember, void_speck, soot_puff | `#9B2A2A` |
| BODY | mote, earth_chip, spark, smoke_wisp | `#FFD08A` |
| EYE | rune_eye, glint, mote, mote_soft | `#7FE8FF` |
| EYEBALL | rune_eye, void_speck, blood_drop, soot_puff | `#C84060` |
| CURSE | void_speck, rune_luna, soul_wisp, soot_puff | `#9B3FCB` |
| CURSE_WARDING | rune_sol, rune_key, glint, mote_soft | `#D9C8FF` |
| DEMON_FIRE | ember, flame_soul, void_speck, soot_puff | `#9B4BFF` |
| DEMON_ICE | snowflake, shard_ice, void_speck, mote_soft | `#5A6FFF` |
| DEMON_LIGHT | glint, void_speck, star4, soot_puff | `#D94BFF` |
| DEMON_WATER | bubble, droplet, void_speck, soot_puff | `#2A4FA8` |
| BARRIER | shard_glass, rune_sol, glint, mote_soft | `#7FD6C8` |
| KEY | rune_key, glint, mote, mote_soft | `#FFD36A` |
| CHAIN | chain_link, spark, glint, ash_flake | `#9AA3B8` |
| BUTOH | music_note, mote, petal, mote_soft | `#FF9AC8` |
| BRIAR | spore, leaf_green, seed, ash_flake | `#3FA05A` |
| CHERRY_BLOSSOM | petal_cherry, petal, mote, mote_soft | `#FF9FC4` |
| FUNGUS | spore, mote, bubble, smoke_wisp | `#9FD060` |
| FOOD | mote, seed, gem_sparkle, smoke_wisp | `#FFB36B` |
| CRYSTAL | shard_crystal, gem_sparkle, glint, mote_soft | `#B98CFF` |
| CORUNDUM | gem_sparkle, shard_crystal, glint, mote_soft | `#FF4A7A` |
| BRONZE | cog, spark, earth_chip, ash_flake | `#C9863C` |
| COPPER | electric_orb, cog, spark, ash_flake | `#E0743A` |
| IRON | spark, chain_link, earth_chip, ash_flake | `#A9B0BE` |
| BLACK_OIL | ink_splat, void_speck, ember, soot_puff | `#1A1424` |
| GEL | bubble, droplet, mote_soft, mote | `#8FE8C0` |
| GLASS | shard_glass, glint, mote, mote_soft | `#CDEBFF` |
| BUBBLE | bubble, glint, mote_soft, droplet | `#A8E0FF` |
| ICE_WEDGE | shard_ice, snowflake, glint, mote_soft | `#7FC4FF` |
| LEGION | mana_mote, spirit_firefly, spark, mote_soft | `#FFD27A` |
| AIR | mote_soft, feather_white, smoke_wisp, glint | `#D7F5FF` |
| HAIR | thread, mote, spark, smoke_wisp | `#FFD0A0` |
| MEMORY | page, mote, dream_star, mote_soft | `#B8D0FF` |
| MINERAL | gem_sparkle, earth_chip, shard_crystal, ash_flake | `#8FE0D0` |
| MODIFICATION | cog, gem_sparkle, spark, mote_soft | `#FF9A5A` |
| MUCUS | bubble, droplet, spore, mote_soft | `#B8F07A` |
| MUD | earth_chip, droplet, sand_grain, soot_puff | `#7A5A3A` |
| NAIL | spark, shard_glass, chain_link, ash_flake | `#D0D4DC` |
| PERMEATION | void_speck, mote, rune_wave, mote_soft | `#8FA8FF` |
| POISON_PLANT | spore, leaf_green, bubble, poison_cloud | `#9AD03A` |
| RED_OCHRE | paint_splat, earth_chip, ember, ash_flake | `#C4503A` |
| ROCK | earth_chip, sand_grain, spark, ash_flake | `#8A8478` |
| SANDSTONE | sand_grain, earth_chip, glint, ash_flake | `#D8B878` |
| SCALE | shard_crystal, gem_sparkle, glint, mote_soft | `#6FD0B8` |
| SHAKUDO | glint, spark, cinder, soot_puff | `#4A5A88` |
| SKIN | petal, mote, thread, smoke_wisp | `#FFC9A8` |
| SMOKE | smoke_wisp, soot_puff, ember, ash_flake | `#8A8A96` |
| SNOW | snowflake, mote_soft, glint, smoke_wisp | `#F4FAFF` |
| SONG | music_note, mote, star4, mote_soft | `#FFE08A` |
| SOUL_CORPSE | soul_wisp, bone_chip, void_speck, soot_puff | `#7FD0C0` |
| SOUL | soul_wisp, spirit_firefly, void_speck, mote_soft | `#9FE8FF` |
| SOUND | music_note, rune_wave, glint, mote_soft | `#9FD0FF` |
| SPIKE | shard_crystal, earth_chip, spark, ash_flake | `#B0B8C8` |
| SWITCHING | mana_mote, rune_key, glint, mote_soft | `#FFA8FF` |
| TONGUE | droplet, blood_drop, bubble, mote_soft | `#E86A8A` |
| TREE | leaf_green, seed, leaf_autumn, spore | `#6FB04F` |
| STONE | earth_chip, sand_grain, glint, ash_flake | `#9A9A9A` |
| VINE | leaf_green, thread, seed, spore | `#4FA04A` |
| VORTEX | aura_wisp, mana_mote, glint, smoke_wisp | `#8FC8FF` |
| WING | feather_white, mote, glint, mote_soft | `#FFFFFF` |

### Relic grades and elements

| theme | particles | colour |
|---|---|---|
| `relic.grade.NORMAL` | mote_soft, mote, ash_flake | `#AAAAAA` |
| `relic.grade.SPECIAL` | mote, spore, glint | `#55FF55` |
| `relic.grade.RARE` | mote, glint, mana_mote | `#55FFFF` |
| `relic.grade.UNIQUE` | glint, gem_sparkle, dream_star, mote | `#FF77FF` |
| `relic.grade.LEGEND` | star4, glint, ember, gem_sparkle | `#FFAA00` |
| `relic.grade.GOD` | glint, star4, rune_sol, mote | `#FFE9A0` |
| `relic.grade.GENESIS` | star4, mana_mote, dream_star, nebula_puff | `#E8D8FF` |
| `relic.element.fire` | ember, flame_tongue, spark, soot_puff | `#FF7A2E` |
| `relic.element.water` | bubble, droplet, mana_mote, mote_soft | `#3F8FFF` |
| `relic.element.ice` | snowflake, shard_ice, glint, mote_soft | `#9FE0FF` |
| `relic.element.wind` | mote, feather_white, glint, smoke_wisp | `#BFF5E0` |
| `relic.element.earth` | earth_chip, sand_grain, glint, ash_flake | `#9A7A4E` |
| `relic.element.storm` | electric_orb, rune_bolt, spark, smoke_wisp | `#7AA8FF` |
| `relic.element.nature` | leaf_green, petal, spore, seed | `#5FCB4F` |
| `relic.element.light` | glint, star4, mote, mote_soft | `#FFF6C8` |
| `relic.element.holy` | glint, feather_white, star4, mote_soft | `#FFF0B8` |
| `relic.element.dark` | void_speck, soul_wisp, rune_luna, soot_puff | `#7A3FCB` |
| `relic.element.demon` | ember, void_speck, flame_soul, soot_puff | `#B03A5A` |
| `relic.element.arcane` | mana_mote, rune_sol, gem_sparkle, mote_soft | `#8F7BFF` |
| `relic.element.blood` | blood_drop, void_speck, ember, soot_puff | `#A8182B` |
| `relic.element.metal` | spark, shard_glass, chain_link, ash_flake | `#B8C2D2` |
| `relic.element.time` | clock_tick, cog, glint, mote_soft | `#BFA4FF` |
| `relic.element.space` | mana_mote, rune_key, glint, nebula_puff | `#8F5BFF` |
| `relic.element.soul` | soul_wisp, spirit_firefly, void_speck, mote_soft | `#9FE8FF` |
| `relic.element.music` | music_note, mote, star4, mote_soft | `#FFE08A` |
| `relic.element.poison` | spore, bubble, droplet, poison_cloud | `#7BD13A` |
| `relic.element.dream` | dream_star, soul_wisp, star4, nebula_puff | `#F5B8FF` |
| `relic.element.gem` | gem_sparkle, shard_crystal, glint, mote_soft | `#FF6FB0` |

A relic helper that only knows a colour (`relics.Fx.ring/burst/dust`) is moved to `FxRelic`, which picks `relic.element.<x>` from the hue of the colour (`FxHue`: red fire, dark red blood, gold light, green nature, cyan ice, blue water, violet arcane, pink dream, dark, grey metal, white holy) and dyes the particles with it.

## The codemod

```
python3 -B tools/particles/codemod.py                  # rewrite in place, print the report
python3 -B tools/particles/codemod.py --check          # change nothing, exit 1 if a run would change something
python3 -B tools/particles/codemod.py --diff --only 'item/**'
python3 -B tools/particles/codemod.py --include-relics --include-lanes   # AFTER the builders of relics/wave, client/relics, relics/djinn and the rift pockets merged
```

It skips: Time Magic, the particle libraries, `NUSMP.java`, the lanes (`relics/wave`, `client/relics` outside the Djinn, `relics/socket`, `relics/order`, `relics/pair`, `relics/forge` unless `--include-relics`; `labyrinth/mega/rift` unless
`--include-lanes`), Rukh calls in the Djinn and dungeon paths, anything that carries data it cannot map. It is idempotent. It also writes the two generated files `FxOld.java` and `FxThemes.java`.
After the flagged run: create `docs/rukh/exclusive_enforced` (turns the tolerance of `RukhExclusiveScopeTest` into a failure).

### Measured run (on the tree before the codemod, default flags)

81 existing files edited (76 by the first run, 5 more when the relic helpers were added), 426 call sites replaced plus 66 type and comparison tokens (`Alignment` to `FxTone`):
vanilla `sendParticles` 150, vanilla `addParticle` 14, vanilla particles as values 81, dust options 11, `RukhFx`/`RukhClientFx` calls 56, `RukhParticles` values 105 and `sendParticles(RukhParticles...)` 3, relic helpers 6.
With `--include-relics --include-lanes` on top: 126 more files (relic waves 113, client relics 4, sockets 2, order 2, pair 1, forge 1, rift pockets 3), 1567 more edits; the result type-checks against the API stubs and a second run changes nothing.

### Call sites left, with the reason (now)

| reason | count |
|---|---|
| dust options held in a variable, field or helper (the declared type would change) | 29 |
| lane: relics (other builders; run with --include-relics after they merge) | 25 |
| carries data (block state / item / colour) | 22 |
| lane: rift pockets (other builders; run with --include-lanes after they merge) | 5 |

Not in a lane, left on purpose:

| file | line | reason |
|---|---|---|
| `arc/spade/SpadeFx.java` | 91 | dust options held in a variable, field or helper (the declared type would change) |
| `arc/zagred/ZagredDecrees.java` | 124 | dust options held in a variable, field or helper (the declared type would change) |
| `boss/refill/RefillFx.java` | 20 | dust options held in a variable, field or helper (the declared type would change) |
| `corpse/Corpses.java` | 322 | carries data (block state / item / colour) |
| `corpse/NamedCorpseEntity.java` | 305 | carries data (block state / item / colour) |
| `dungeon/floor/charybdis/CharybdisMinions.java` | 118 | carries data (block state / item / colour) |
| `dungeon/floor/charybdis/CharybdisMoves.java` | 394 | dust options held in a variable, field or helper (the declared type would change) |
| `dungeon/floor/charybdis/MagiculeScaleEntity.java` | 46 | dust options held in a variable, field or helper (the declared type would change) |
| `dungeon/floor/charybdis/MagiculeScaleEntity.java` | 47 | dust options held in a variable, field or helper (the declared type would change) |
| `dungeon/floor/goliath/GoliathBoss.java` | 184 | carries data (block state / item / colour) |
| `dungeon/floor/goliath/GoliathBoss.java` | 186 | carries data (block state / item / colour) |
| `dungeon/floor/goliath/GoliathMoves.java` | 72 | carries data (block state / item / colour) |
| `dungeon/floor/goliath/GoliathMoves.java` | 74 | carries data (block state / item / colour) |
| `entity/GrimoireDaemonEntity.java` | 275 | dust options held in a variable, field or helper (the declared type would change) |
| `entity/GrimoireDaemonEntity.java` | 284 | dust options held in a variable, field or helper (the declared type would change) |
| `entity/ZagredAttacks.java` | 85 | dust options held in a variable, field or helper (the declared type would change) |
| `entity/ZagredAttacks.java` | 147 | dust options held in a variable, field or helper (the declared type would change) |
| `entity/ZagredAttacks.java` | 166 | dust options held in a variable, field or helper (the declared type would change) |
| `entity/avatar/AvatarFx.java` | 78 | dust options held in a variable, field or helper (the declared type would change) |
| `entity/zogratis/BloodBeastEntity.java` | 350 | dust options held in a variable, field or helper (the declared type would change) |
| `entity/zogratis/DanteMoves.java` | 545 | carries data (block state / item / colour) |
| `entity/zogratis/DanteMoves.java` | 555 | carries data (block state / item / colour) |
| `entity/zogratis/DarkDiscipleEntity.java` | 374 | dust options held in a variable, field or helper (the declared type would change) |
| `entity/zogratis/LuciusFx.java` | 159 | dust options held in a variable, field or helper (the declared type would change) |
| `entity/zogratis/SpellSeal.java` | 281 | dust options held in a variable, field or helper (the declared type would change) |
| `entity/zogratis/Tells.java` | 16 | dust options held in a variable, field or helper (the declared type would change) |
| `entity/zogratis/VanicaMoves.java` | 460 | carries data (block state / item / colour) |
| `entity/zogratis/VanicaMoves.java` | 454 | dust options held in a variable, field or helper (the declared type would change) |
| `entity/zogratis/VanicaMoves.java` | 455 | dust options held in a variable, field or helper (the declared type would change) |
| `entity/zogratis/ZenonBoss.java` | 230 | carries data (block state / item / colour) |
| `entity/zogratis/ZenonBoss.java` | 635 | dust options held in a variable, field or helper (the declared type would change) |
| `entity/zogratis/ZenonMoves.java` | 100 | carries data (block state / item / colour) |
| `entity/zogratis/ZenonMoves.java` | 243 | carries data (block state / item / colour) |
| `entity/zogratis/ZenonMoves.java` | 584 | carries data (block state / item / colour) |
| `entity/zogratis/ZenonMoves.java` | 585 | carries data (block state / item / colour) |
| `entity/zogratis/ZenonMoves.java` | 591 | carries data (block state / item / colour) |
| `entity/zogratis/ZogratisBoss.java` | 496 | dust options held in a variable, field or helper (the declared type would change) |
| `labyrinth/mega/admin/WfcDebugService.java` | 67 | dust options held in a variable, field or helper (the declared type would change) |
| `labyrinth/mega/crucible/CatastropheRunner.java` | 305 | carries data (block state / item / colour) |
| `labyrinth/mega/crucible/CatastropheRunner.java` | 312 | carries data (block state / item / colour) |
| `labyrinth/mega/crucible/CatastropheRunner.java` | 344 | carries data (block state / item / colour) |
| `labyrinth/mega/crucible/CatastropheRunner.java` | 300 | dust options held in a variable, field or helper (the declared type would change) |
| `labyrinth/mega/crucible/CatastropheRunner.java` | 380 | dust options held in a variable, field or helper (the declared type would change) |
| `labyrinth/mega/crucible/CrucibleOffer.java` | 249 | carries data (block state / item / colour) |
| `labyrinth/mega/guard/GuardianTraits.java` | 216 | carries data (block state / item / colour) |
| `requip/RequipParticles.java` | 23 | dust options held in a variable, field or helper (the declared type would change) |
| `shishou/ShishouEffects.java` | 41 | dust options held in a variable, field or helper (the declared type would change) |
| `shishou/ShishouSpells.java` | 351 | carries data (block state / item / colour) |
| `shishou/ShishouSpells.java` | 60 | dust options held in a variable, field or helper (the declared type would change) |
| `shishou/ShishouSpells.java` | 61 | dust options held in a variable, field or helper (the declared type would change) |
| `vfx/client/VfxManager.java` | 426 | dust options held in a variable, field or helper (the declared type would change) |

### Existing files edited, by package (git, against `51fdb09`)

| package | files | |
|---|---|---|
| `(root)` | 3 | `NUConfig.java`, `NUCreativeTab.java`, `NUSMP.java` |
| `anim` | 1 | `CastAnim.java` |
| `arc/arena` | 1 | `ChronicleSigilItem.java` |
| `arena` | 1 | `ZogratisHourglassItem.java` |
| `balance` | 1 | `BalanceLaw.java` |
| `blackclover` | 3 | `GrimoireAcceptance.java`, `GrimoireItem.java`, `MagicType.java` |
| `book` | 14 | `BlackOilArts.java`, `CharacterSpells.java`, `CompassBook.java`, `EarthBook.java`, `ElementBooks.java`, `GrimoireBook.java`, `GrimoireKeeper.java`, `LightBook.java`, `RecombinationArts.java`, `ScaleSpells.java`, `SoulCorpseBook.java`, `SpellRuntime.java`, `TensuraShots.java`, `WindBook.java` |
| `book/ext` | 1 | `SoulCorpseExt.java` |
| `boss` | 1 | `LuciusSpells.java` |
| `boss/bar` | 2 | `BossBarNet.java`, `BossBarSync.java` |
| `celestial` | 4 | `CelestialKeyItem.java`, `CelestialProps.java`, `SpiritKingEntity.java`, `SpiritKingSceptreItem.java` |
| `client` | 11 | `DemonSlayerRenderer.java`, `ElementalSpiritRenderer.java`, `ElsdociaGlitterClient.java`, `GrimoireDaemonRenderer.java`, `MirrorDoubleRenderer.java`, `NURenderTypes.java`, `PaintedConstructRenderer.java`, `SpiritOrbRenderer.java`, `SwordDrawClient.java`, `WeaponRenderer.java`, `ZagredRenderer.java` |
| `client/aura` | 1 | `AuraRender.java` |
| `client/avatar` | 1 | `PlayerAvatarClient.java` |
| `client/corpse` | 2 | `CorpseClient.java`, `CorpseDecayLayer.java` |
| `client/elite` | 2 | `DungeonMapScreen.java`, `QuestOverlay.java` |
| `client/grimoire` | 2 | `GrimoireBakedModel.java`, `GrimoireFloatClient.java` |
| `client/gui` | 2 | `BossBarOptions.java`, `ModBossBarOverlay.java` |
| `client/heroweapon` | 3 | `HeroHud.java`, `CarriageRenderer.java`, `CurseOverlay.java` |
| `client/labyrinth` | 4 | `LabyrinthClient.java`, `DimensionalRiftRenderer.java`, `MegaRiftRenderer.java`, `RelicPedestalRenderer.java` |
| `client/mega` | 3 | `FloorCardClient.java`, `SystemClientConfig.java`, `SystemOverlay.java` |
| `client/mode` | 1 | `ModeArmorLayer.java` |
| `client/perf` | 1 | `PerfClient.java` |
| `client/playermodel` | 3 | `DungeonBodies.java`, `KitRelicLayer.java`, `ZagredAuraLayer.java` |
| `client/prop` | 4 | `CompassPropPainter.java`, `PropPainterRegistry.java`, `RecombinationPropPainter.java`, `SoulCorpseRebuildPropPainter.java` |
| `client/relics` | 35 | `ArmorPainter.java`, `HumanoidRelicLayer.java`, `RelicActKeys.java`, `RelicClient.java`, `RelicGradeFx.java`, `RelicLayerGuard.java`, `RelicTextures.java`, `RelicWornLayer.java`, `DjinnArmorLayer.java`, `DjinnClient.java`, `DjinnDraw.java`, `DjinnScreenFx.java`, `DjinnGui.java`, `RelicFxClient.java`, `RelicFxOptions.java`, `HandForms.java`, `HandItemRenderer.java`, `HandTransforms.java`, `SocketFx.java`, `WeaponClasses.java`, `DanteGravityCrownRenderer.java`, `WornDebugClient.java`, `WornPlan.java`, `WornTrace.java`, `ZenonSpatialRingRenderer.java`, `ArmorFx.java`, `ArmorGlow.java`, `ArmorItemRenderer.java`, `WornAnim.java`, `WornLibClient.java`, `WornLibDraw.java`, `WornLibrary.java`, `WornMesh.java`, `WornModels.java`, `WornPlacer.java` |
| `client/remodel` | 2 | `AbyssalRenderer.java`, `KitZagredRenderer.java` |
| `client/song` | 1 | `SongClient.java` |
| `client/thread` | 1 | `RougeRenderer.java` |
| `client/ui` | 5 | `GuiScale.java`, `GuiSizeScreen.java`, `GuiScaleRegistry.java`, `PageUnlockOverlay.java`, `UiThemeClient.java` |
| `compat/create` | 1 | `TrapRuntime.java` |
| `core/magic` | 4 | `EnumMagicProfile.java`, `GenericFallbackFx.java`, `MagicProfile.java`, `ProfileOverride.java` |
| `corpse` | 8 | `CorpseConfig.java`, `CorpseEntities.java`, `CorpseHooks.java`, `CorpseRoster.java`, `Corpses.java`, `NamedCorpseEntity.java`, `NamedCorpses.java`, `SoulCorpseSpells.java` |
| `dungeon` | 1 | `DungeonGuard.java` |
| `dungeon/floor` | 6 | `CharybdisMinions.java`, `CharybdisMoves.java`, `CharybdisSpawnBoss.java`, `MagiculeScaleEntity.java`, `GoliathBoss.java`, `GoliathMoves.java` |
| `elite/hub` | 8 | `CoinBossDrops.java`, `CoinLoot.java`, `CoinLootModifier.java`, `CoinMobDrops.java`, `MerchantNPC.java`, `QuestGiverNPC.java`, `QuestService.java`, `ShopService.java` |
| `entity` | 6 | `CottonCloudEntity.java`, `CottonSheepEntity.java`, `ElementalSpiritLordEntity.java`, `GrimoireDaemonEntity.java`, `SpiritLordEntity.java`, `ZagredBossEntity.java` |
| `entity/avatar` | 2 | `AvatarMoves.java`, `HexVolley.java` |
| `entity/zogratis` | 5 | `DanteMoves.java`, `LuciusFx.java`, `SpellSeal.java`, `ZenonMoves.java`, `ZogratisBoss.java` |
| `grimoire` | 2 | `BookMotif.java`, `GrimoireBookPlan.java` |
| `heroweapon` | 1 | `HeroWeaponItem.java` |
| `item` | 8 | `ArmorEngravings.java`, `BroochPinArts.java`, `CuriosLink.java`, `DemonSlayerSwordItem.java`, `HandOfProvidenceArts.java`, `MagicWeaponItem.java`, `RotDoomArts.java`, `WeaponEngravings.java` |
| `labyrinth` | 1 | `LabyrinthCreative.java` |
| `labyrinth/gate` | 2 | `ExitGateBlock.java`, `GateBlock.java` |
| `labyrinth/loot` | 2 | `LootConfig.java`, `LootRegistry.java` |
| `labyrinth/mega` | 56 | `MegaCommands.java`, `MegaSystem.java`, `CatastropheRunner.java`, `CrucibleHook.java`, `CrucibleOffer.java`, `DecennialArenas.java`, `DecennialBossController.java`, `DecennialFx.java`, `DecennialLayer.java`, `Signatures.java`, `EcoConfig.java`, `EcoSelfTests.java`, `TurfWarGoal.java`, `TurfWars.java`, `EncounterDirector.java`, `FloorTracker.java`, `FoundryAbilityRunner.java`, `FoundryBrain.java`, `FoundryConfig.java`, `FoundryGoals.java`, `FoundryMobEntity.java`, `CoreSelfTests.java`, `ExpansionQueue.java`, `GenData.java`, `GeometryCheck.java`, `MegaCommand.java`, `MegaGenRegistry.java`, `SectorGenerator.java`, `SectorPlacer.java`, `DndBoss.java`, `GuardianAltarBlockEntity.java`, `GuardianTraits.java`, `LabyrinthGuardianEntity.java`, `ManaCoreItem.java`, `SectorDoor.java`, `LockRunes.java`, `PrimsWorld.java`, `DimensionalRiftBlockEntity.java`, `PocketBuilder.java`, `PocketEvents.java`, `PocketRealm.java`, `RiftAdmin.java`, `RiftCommand.java`, `RiftConfig.java`, `RiftRegistry.java`, `RiftSelfTests.java`, `RiftService.java`, `ExclusiveEvents.java`, `ExclusivePockets.java`, `PocketBuild.java`, `PocketClear.java`, `PocketCollapse.java`, `PocketPlan.java`, `PocketRuntime.java`, `PocketSelfTests.java`, `RestEvents.java` |
| `labyrinth/seal` | 3 | `FloorWardenEntity.java`, `GatekeeperDoorBlock.java`, `PeakAltarBlockEntity.java` |
| `labyrinth/squad` | 3 | `GoalInstaller.java`, `SquadHook.java`, `SupportCast.java` |
| `labyrinth/trial` | 6 | `MagicBarrierBlock.java`, `SealedArchwayBlock.java`, `SealedArchwayBlockEntity.java`, `SpawnGlyphBlock.java`, `TrialMath.java`, `WardRuneBlock.java` |
| `labyrinth/vault` | 1 | `RelicPedestalBlockEntity.java` |
| `mana` | 1 | `ManaLaw.java` |
| `mixin` | 1 | `LevelGuardMixin.java` |
| `multiverse` | 1 | `MultiverseCommands.java` |
| `perf` | 2 | `PerfBudgets.java`, `PerfSelfTests.java` |
| `prop` | 5 | `CompassProps.java`, `PropKind.java`, `PropRegistry.java`, `RecombinationProps.java`, `ScaleProps.java` |
| `relics` | 15 | `MobRelics.java`, `RelicBowItem.java`, `RelicCatalog.java`, `RelicCommands.java`, `RelicEngine.java`, `RelicItemSupport.java`, `RelicPayloads.java`, `RelicRangedSupport.java`, `RelicSummons.java`, `RelicSystem.java`, `RelicWeaponItem.java`, `Relics.java`, `UnbindingSealItem.java`, `WornDebug.java`, `WornSync.java` |
| `relics/djinn` | 10 | `DjinnBinding.java`, `DjinnCommand.java`, `DjinnEngine.java`, `DjinnNet.java`, `DjinnSealItem.java`, `DjinnSelfTests.java`, `VesselAccess.java`, `DarkDjinn.java`, `DarkRukhShardItem.java`, `DjinnWeaponItem.java` |
| `relics/forge` | 1 | `ForgeEngraving.java` |
| `relics/order` | 2 | `OrderFx.java`, `OrderMoves.java` |
| `relics/pair` | 1 | `PairHooks.java` |
| `relics/socket` | 2 | `SocketActs.java`, `SocketService.java` |
| `relics/tier` | 1 | `TierRitual.java` |
| `relics/wave` | 115 | `Wave0Fusion.java`, `Wave0Hooks.java`, `Wave1.java`, `Wave10Acts.java`, `Wave10Events.java`, `Wave11Acts.java`, `Wave11Events.java`, `Wave12Heavy.java`, `Wave12Light.java`, `Wave12Magic.java`, `Wave13Acts.java`, `Wave13Events.java`, `Wave13Util.java`, `Wave14Feet.java`, `Wave14Head.java`, `Wave14Util.java`, `Wave14Zones.java`, `Wave15Events.java`, `Wave15Field.java`, `Wave15Hands.java`, `Wave15Util.java`, `Wave15Wear.java`, `Wave16Acts.java`, `Wave16Chalk.java`, `Wave16Events.java`, `Wave16God.java`, `Wave16Kit.java`, `Wave16Swarm.java`, `Wave17Acts.java`, `Wave17Dragon.java`, `Wave17Events.java`, `Wave17Kit.java`, `Wave18Acts.java`, `Wave18Bags.java`, `Wave18Camp.java`, `Wave18Events.java`, `Wave18Kit.java`, `Wave19Book.java`, `Wave19Events.java`, `Wave19Field.java`, `Wave19Goods.java`, `Wave19Scrolls.java`, `Wave19Surge.java`, `Wave19Util.java`, `Wave1Aim.java`, `Wave1Casts.java`, `Wave20Acts.java`, `Wave20Events.java`, `Wave20Util.java`, `Wave21Acts.java`, `Wave21Events.java`, `Wave22Acts.java`, `Wave22Events.java`, `Wave23.java`, `Wave23Acts.java`, `Wave23Events.java`, `Wave24Acts.java`, `Wave24Events.java`, `Wave24Util.java`, `Wave25Acts.java`, `Wave25Events.java`, `Wave25Kit.java`, `Wave26Acts.java`, `Wave26Frank.java`, `Wave26Mind.java`, `Wave26Util.java`, `Wave26Wear.java`, `Wave27Hands.java`, `Wave27Head.java`, `Wave27Util.java`, `Wave28Belt.java`, `Wave28Hands.java`, `Wave28Neck.java`, `Wave28Potions.java`, `Wave28Util.java`, `Wave29Brews.java`, `Wave29Portal.java`, `Wave2Acts.java`, `Wave2Events.java`, `Wave30Deck.java`, `Wave30Events.java`, `Wave30Key.java`, `Wave30Paradox.java`, `Wave30Stones.java`, `Wave30Vecna.java`, `Wave31Acts.java`, `Wave3Belt.java`, `Wave3Field.java`, `Wave3Goods.java`, `Wave3Hands.java`, `Wave3Head.java`, `Wave3Util.java`, `Wave4BeastActs.java`, `Wave4ChargeActs.java`, `Wave4CovenActs.java`, `Wave4ForestActs.java`, `Wave4Handlers.java`, `Wave4MiscActs.java`, `Wave5Aim.java`, `Wave5Dragon.java`, `Wave5Hemlock.java`, `Wave5Locket.java`, `Wave5Safeguard.java`, `Wave5Souls.java`, `Wave5Warmage.java`, `Wave6Acts.java`, `Wave6Events.java`, `Wave6Zones.java`, `Wave71Acts.java`, `Wave7Events.java`, `Wave8Acts.java`, `Wave8Events.java`, `Wave9Acts.java`, `Wave9Events.java`, `WaveBossActs.java` |
| `requip` | 2 | `RequipMagicFx.java`, `RequipParticles.java` |
| `skill` | 3 | `GodTier.java`, `GrimoireMagicSkill.java`, `SkillFx.java` |
| `skilltree/line` | 1 | `LineFx.java` |
| `song` | 1 | `SongEffects.java` |
| `soul` | 1 | `PaladinFx.java` |
| `thread` | 3 | `RougeEntity.java`, `RougeFateEvents.java`, `ThreadTrapEntity.java` |
| `vfx` | 1 | `VfxShape.java` |
| `vfx/client` | 11 | `VfxClientEvents.java`, `VfxDefinitions.java`, `VfxManager.java`, `VfxTextures.java`, `VfxVertexBuffer.java`, `ArcaneSpellLayer2.java`, `AuraLayer.java`, `MirrorLayer.java`, `RecombinationLayer.java`, `SlashCompassMercuryLayer.java`, `PortalClient.java` |
| `wizard` | 2 | `MagicFlavors.java`, `WizardSystem.java` |

Shared files touched with append-only lines: `NUSMP.java` (two lines: `FxBoot.init`, `FxClient.init`). Hand edits after the codemod: `core/magic/profile/*` (`particle()` returns `ParticleOptions`, `themeKey()`), `blackclover/MagicType`, `skill/GrimoireMagicSkill`, `labyrinth/mega/guard/GuardianTraits` (ring helper takes any options), comments in `skill/SkillFx`, `skilltree/line/LineFx`, `vfx/client/portal/PortalClient`.

## Tests

* `FxLibraryAssetsTest`: every catalog entry has a sprite json listing exactly its frames, every frame is a non-empty PNG of the catalog size, the families the owner asked for exist, the 8 rune glyphs differ, additive particles are full-bright, the type loop and the provider loop are in place.
* `ParticleThemesTest`: themes name only registered particles, cover all 105 magic types (read from `MagicType.java`), the 7 grades, the relic elements and every theme the map names; MOTE/AMBIENT slots are small; resolution rules.
* `FxMathTest`: size/alpha curves, frame selection, thinning by quality and distance, the share of a burst, the Rukh caps, the profile parser, the tint payload round trip (bytes), colour parsing.
* `FxHueTest`: the colour wheel to relic element.
* `FxCodemodMapTest`: `FxOld` and `FxThemes` match `map.json`; the skip list keeps Time Magic out.
* `FxSourceScanTest`: no mapped vanilla particle (`END_ROD` above all) outside the allow-list, the facade keeps its budgets and quality scaling, the provider allocates only the particle, the command's lang keys exist.
* `RukhExclusiveScopeTest`: the Rukh are for the Djinn and the dungeons only (lanes tolerated until `docs/rukh/exclusive_enforced`); `RukhPurgeScopeTest` and `RukhBudgetTest` still pass.
* Self-test `particles.custom` (in the real game, CI server smoke): every type registered and bound, the real stream codec round trip, the theme table valid, the facade with the switch on and off, the budget respected.

## What could not be verified

* Nothing was seen in game. The textures were reviewed on the generated sheet only; how each particle moves and reads at its in-game size (0.15 to 1 block) is reasoned, not observed.
* The additive render type (`FxRenderTypes.ADDITIVE`: source-alpha plus one, depth write off) follows the vanilla translucent sheet with another blend function; if a custom render type misbehaves in the engine of this version, set `blend` to `translucent` in the generator's profiles.
* `RenderSystem.setShader`, `TextureAtlas.LOCATION_PARTICLES` and `RegistryFriendlyByteBuf` are reported by the API stub type-check as unknown or abstract (the stubs are incomplete; the same lines exist in the existing renderers); the whole tree type-checks with the same error set as before this change plus those three stub artefacts.
* Gradle was not available: the JUnit tests ran in the offline shim (pure and file-scanning classes only); tests that need Minecraft classes (`MagicType` users) were not run.
* The particle atlas now holds 422 more small sprites; nothing was measured.

