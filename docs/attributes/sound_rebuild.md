# Sound Magic: the 1.93.0 rebuild

Sound Magic (`MagicType.SOUND`, book `nusmp:book_sound`, colour 0xFF70C8F0 cyan) used to be the generic wiki attribute book. It is now `book/SoundBook` with its own armour,
summon models, effect layer and spells.

## 1. Canon and what could not be checked
The Black Clover wiki could not be reached from the build environment and the repo has no wiki page for Sound Magic. No anime user of Sound Magic is named in the repo and none is
claimed here. The magic is built on what sound is: a wave front, a speaker, a tuning fork, resonance (a vibration that builds on a body until it rings). The spells are original.

## 2. NEW / REPLACED / KEPT
| Part | Status | What |
|---|---|---|
| Book class | REPLACED | `WikiBooks.attribute(SOUND)` returns `SoundBook` (`book/SongRebuild` dispatch) |
| Pages `attribute_bolt` `attribute_cut` `attribute_field` `attribute_domain` `attribute_guard` | REPLACED (same ids, order, mastery bits) | Sonic Bolt, Sound Wave, Resonance Field, Sonic Boom, Echo Plate |
| Pages `sound_speaker` `sound_silence` `sound_resonance` `sound_boom` | NEW (appended, modes 5 to 8) | 9 family pages of the 24 allowed |
| Damage type | REPLACED | wind elemental (was generic magic) |
| Armour `Aura.SOUND` (`client/aura/SoundAura`) | NEW | Resonance Plate, 14 pieces |
| Models `sound_speaker`, `sound_fork` | NEW | |
| VFX `SOUND_FX1/2/3` | NEW (appended to `VfxShape`) | |

## 3. The temporary armour: the Resonance Plate (`sound_armour`)
Every successful cast puts it on for 5 seconds; Echo Plate keeps it for 9 s. Same forge-in and shatter-out as the Djinn full equip (`SongArmourKit` + `EquipAnimation`).
Brushed steel, black rubber, cyan light.

| Piece | Anchor | Delay | What |
|---|---|---|---|
| `helm` | head | 0 | headphones with speaker-cone ear cups, a mic boom, two antenna fins (hidden in first person) |
| `chest` | body | 2 | amp plate: speaker grille, VU meter strip, knobs, vented back plate |
| `wave_a` `wave_b` `wave_c` | body | 6, 8, 10 | three arcs of sound behind the back, each pulsing outward in turn |
| `cable` | body | 9 | a coiled cable tail with a plug |
| `pauldron_r/l` | arms | 3 | speaker stacks with cones |
| `vambrace_r/l` | arms | 5 | tuning forks along the forearm with a glowing vibration |
| `gauntlet_r/l` | arms | 7 | resonator rings on the knuckles |
| `greave_r/l` | legs | 9, 10 | bass-drum shin plates, a grille knee, an emissive ankle ring |

## 4. Summon models (`client/prop/SoundRebuildPropPainter`)
| Model | Prop | What | Clips |
|---|---|---|---|
| `sound_speaker` (22 cubes) | `SOUND_1` | a speaker stack: cabinet, woofer, mid driver, tweeter, horn, VU strip, antenna; cones with a glowing dust cap | idle, pulse (the woofer thumps, rings run out) |
| `sound_fork` (32 cubes) | `SOUND_2` | a tuning fork with three sound rings, the body of the Sonic Bolt | idle, strike |

## 5. Spells
| Id | Name | Tier | What | Notes |
|---|---|---|---|---|
| `attribute_bolt` | Sonic Bolt | starter | fork bolt, raw 9, burst 1.6 blocks, a resonance mark, Magic Jamming (Ultimate skills caught with chance 0.3, 2 s) | |
| `attribute_cut` | Sound Wave | mid | line of 20 blocks, raw 8, knock-back, a mark | |
| `attribute_field` | Resonance Field | zone | radius 5 for 5 s, two speaker stacks, raw 4 + Slowness + mark per second, `Nullification.shatterBarriers` each second | |
| `attribute_domain` | Sonic Boom | signature | radius 8, raw 14, knock-back, Ultimate skills jammed 3 s, mark; cooldown 900 | |
| `attribute_guard` | Echo Plate | mid | Speed, melee +4, thorns, 9 s | |
| `sound_speaker` | Speaker Stack | mid | two stacks at the sides for 15 s; every 1.5 s a pulse at the nearest foe: raw 4.5 and jamming | |
| `sound_silence` | Silence | mid | Magic Jamming over radius 9: `Nullification.jamAll` (6 s, bosses 2 s) and the Tensura silence effect, no damage | status: Magic Jamming |
| `sound_resonance` | Resonant Shatter | zone | rings every mark within 24 blocks: raw 5 per stack (up to 5) and barriers shattered | fails with a message when nothing rings |
| `sound_boom` | Mach Step | mid | dash of 9 blocks, raw 12 on the way, a mark, a shock ring | |

RESONANCE (`book/SoundResonance`): each sonic hit adds a stack to the foe (persistent data, 10 s from the latest hit, at most 5). Damage goes through `GrimoireBook.hurt` (EP law),
bosses take half the raw value, no hard control. Magic Jamming and barrier shattering use `Nullification`, the silence effect `EnergyBridge.effect`.

## 6. VFX
`vfx/client/layer/SoundFxLayer`, textures `tools/gen_sound_rebuild_textures.py` -> `textures/particle/sound_*.png`: FX1 Sonic Bolt (cone and wave rings contract on the caster, a
shock-front head with a waveform ribbon and four wave rings left on its path, a shock ring and cone flash at the end), FX2 Resonance Field (turning ripple ring, pulsing cone,
rolling wave rings, a row of dancing equalizer bars on the rim, glints), FX3 Sonic Boom (flash, three shock rings flat / upright / tilted, a cone disc, sparks). 44 to 108 vertices.
Scenes `tools/vfx_preview/scenes.d/sound_rebuild.json`.

## 7. Render layers
Armour: cutout pieces (translucent while forging or shattering), glow maps additive full bright, extras additive (ground sound rings, glints). Speaker: cutout model then additive
rings. Fork: cutout model then additive rings and glow. Same order rule everywhere: solid, translucent, additive last.

## 8. Files
New: `book/SoundBook`, `SoundResonance`; `prop/SoundRebuildProps`; `client/aura/SoundAura`; `client/prop/SoundRebuildPropPainter`; `vfx/client/layer/SoundFxLayer`;
`tools/gen_sound_rebuild_geo.py`, `gen_sound_rebuild_textures.py`; assets `*/entity/sound_*`, `textures/particle/sound_*`; scenes; `tools/lang/sound_rebuild.json`. Shared kit:
`SongRebuildKit`, `SongArmourKit`, `song_rebuild_kit.py`, `song_rebuild_texkit.py` (see docs/attributes/song_rebuild.md).

## 9. Not verified
No game run. Verified by type check (negative control), JUnit of the pure rules (`MagicRebuildRulesTest`), geo checks, the preview tools. Not seen in the real game: light and blending of the
armour, how the speaker stacks sit on slopes, the jamming against real Tensura ultimate skills (the logic is `Nullification`'s, unchanged), network timing. The Tensura silence effect is
looked up by id through `EnergyBridge.effect` and does nothing if Tensura has no effect of that id.

## 1.99.0 armour policy

Sound Magic has no armour in the anime, so since 1.99.0 **no cast puts the temporary armour of this document on the caster** (policy kind NONE). Everything described above as the armour (the render layer, its painter, geo models, textures and generator) is KEPT on disk and registered in `AuraRegistry`, so old saves and tests still resolve; it is simply never started by a page. What the pages did besides the armour (damage cut, thorns, resistance, constructs, effects) is unchanged. Where the armour carried the magic's look, every cast of a family page now sends a small `SOUND_FX2` glow that follows the caster instead (no armour piece is drawn). See `docs/rebuild/armour_policy.md` for the table and the canon reasons; the code is `book.ArmourPolicy` (data: `data/nusmp/armour_policy.json`).
