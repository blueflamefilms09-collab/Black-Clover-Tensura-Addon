# Song Magic: the 1.93.0 rebuild

Song Magic (`MagicType.SONG`, book `nusmp:book_song`, colour 0xFFFFB4E8 rose) used to be the generic wiki attribute book (a bolt, a line, a field, a domain, a guard) plus
Kahono's five singing pages. It is now its own book, `book/SongBook`, with its own armour, summon models, effect layer and spells.

## 1. Canon and what could not be checked
The Black Clover wiki (blackclover.fandom.com) could not be reached from the build environment (the host does not resolve), and the repo has no `.github/wiki` page for Song
Magic. The look is built from what the repo itself says (`song.SongSkill`, `SongPages`: Kahono's Song Magic is sung, with a healing hymn, a lullaby, a barrier chorus, a requiem and
a crescendo) and from the idea of the name: notes, staff lines, choirs, brass. The spells are therefore original on top of Kahono's, not 1:1 copies of anime spells, and no anime
character is named for the new pages. If the owner has the wiki text, the new pages are the place to adjust.

## 2. NEW / REPLACED / KEPT
| Part | Status | What |
|---|---|---|
| Book class | REPLACED | `WikiBooks.attribute(SONG)` now returns `SongBook` (one dispatch line in `WikiBooks`, `book/SongRebuild`) |
| Pages `attribute_bolt` `attribute_cut` `attribute_field` `attribute_domain` `attribute_guard` | REPLACED (same ids, same order, same mastery bits) | Staccato Bolt, Glissando, Choir Field, Grand Concerto, Aria Armour |
| Pages `song_mode_heal` `song_mode_lullaby` `song_mode_barrier` `song_mode_requiem` `song_crescendo` | KEPT | Kahono's singing pages, still at modes 5 to 9 (`song.*` untouched) |
| Pages `song_choir` `song_metronome` `song_harmony` `song_finale` | NEW (appended, modes 10 to 13) | 14 family pages of the 24 allowed |
| Temporary armour (`Aura.SONG`, `client/aura/SongAura`) | NEW (the old armour was the shared wiki aura, `WikiAura` group TRAILS/SPIRAL) | Concert Regalia, 16 pieces, Djinn equip animation |
| Summon models `song_note`, `song_choir`, `song_metronome` | NEW | GeoLite models with glow maps and animations |
| VFX `SONG_FX1/2/3` | NEW shapes (appended to `VfxShape`) | cast, zone, impact |
| VFX `SONG_NOTES`, `SONG_RING` | REPLACED in place (same shapes, same payload meaning) | drawn by the new `SongFxLayer` code; `SongNotesLayer` / `SongRingLayer` stay the registered classes as thin subclasses |
| Song mode mechanics, singing meter, Crescendo | KEPT | untouched |

## 3. The temporary armour: the Concert Regalia (`song_armour`)
Every successful cast of a Song page puts it on for 5 seconds (`SongRebuildKit.armoured`); Aria Armour keeps it for the buff time (9 s). Drawn by `client/aura/SongArmourKit`
with `EquipAnimation` (forge in over 8 ticks, staggered 0 to 12 ticks, flying in from its own side, shattering out over the last 18 ticks): the same arithmetic as the Djinn
full equip. Brass with engraved scrolls, ivory piano keys, rose velvet, glowing strings (the glow is the Song colour).

| Piece (bone) | Anchor | Delay | Fly-in (px) | What |
|---|---|---|---|---|
| `helm` | head | 0 | 0,-16,0 | circlet, treble-clef crest, two trumpet-bell ear horns, plume (hidden in first person) |
| `chest` | body | 2 | 0,0,-14 | brass breastplate with a lyre of five glowing strings, collar, belt |
| `back` | body | 4 | 0,0,14 | the staff cape: five glowing staff lines on ivory cloth with two quavers |
| `lyre` | body | 6 | 0,-6,14 | an upright lyre behind the back with five glowing strings, swaying |
| `tails` | body | 8 | 0,6,12 | conductor's tails, swinging |
| `note_a` `note_b` `note_c` | body | 10 to 12 | 0,-14,0 | three eighth notes orbiting the torso, bobbing |
| `pauldron_r/l` | arms | 3 | -14,-4,0 | trumpet bells on brass caps |
| `vambrace_r/l` | arms | 5 | -12,2,-6 | piano-key cuffs with a tuning peg |
| `gauntlet_r/l` | arms | 7 | -10,8,-6 | brass gloves with four glowing picks |
| `greave_r/l` | legs | 9, 10 | -3,14,-6 | shin plates, knee cop, keyed ankle cuff |

Slim arms: the arm pieces are narrowed to 78 percent about the inner arm edge when the arm part's box is 3 px (checked from the model part, no skin lookup).

## 4. Summon models (all in `client/prop/SongRebuildPropPainter`)
| Model | Prop | What it is | Clips |
|---|---|---|---|
| `song_note` (16 cubes) | `SONG_1` param 0 | the flying body of the Staccato notes: glossy note, flag, grace notes, staff line | idle, strike |
| `song_choir` (31 cubes) | `SONG_2` | a hooded choir spirit in a robe with a songbook, halo and wisp tail; the jaw opens with the verse; translucent | idle, sing |
| `song_metronome` (16 cubes) | `SONG_1` param 1 | a brass pyramid metronome with a swinging pendulum and sliding weight | idle |

## 5. Spells (costs are the usual tiers: starter 8 percent of the magicule pool, mid 12, zone 20, signature 35)
| Id | Name | Tier | What | Notes |
|---|---|---|---|---|
| `attribute_bolt` | Staccato Bolt | starter | three note bolts 4 ticks apart, raw 4.2 each in a burst of 1.8 blocks, Weakness | bolt = `SpellRuntime.bolt` + the note prop |
| `attribute_cut` | Glissando | mid | staff line of 18 blocks, raw 8, Slowness II | |
| `attribute_field` | Choir Field | zone | radius 5 for 5 s, three choir spirits, per second raw 4.5 + Slowness on foes, heal 2 percent on allies | |
| `attribute_domain` | Grand Concerto | signature | radius 7, raw 15, knock-back, allies Absorption; cooldown 900 | |
| `attribute_guard` | Aria Armour | mid | Regeneration, melee +4, thorns, 9 s, armour for as long | |
| `song_choir` | Choir of Spirits | mid | three spirits for 20 s; a verse every 1.5 s: allies heal 1.5 percent, the nearest foe raw 3.5 and Weakness | cap 3 |
| `song_metronome` | Metronome | mid | field of radius 6 for 10 s: allies Speed and Haste, foes Slowness | |
| `song_harmony` | Harmony | mid | up to four allied players within 12 blocks: Resistance, Speed, Regeneration, 10 s | |
| `song_finale` | Grand Finale | signature | radius 14: raw 20 + spiritual damage on foes, allies heal 25 percent (never bosses); cooldown 1800 | |

Every number goes through the balance law: damage through `GrimoireBook.hurt` (EP scaling), bosses take half of the raw value, healing through `BalanceLaw.heal` (never on
bosses), no hard control at all (no `beginControl`). Allies and foes use `song.SongTargets` (the singer, their team, other players without a team, tamed animals are allies).
Spiritual damage is `EnergyBridge.spirit`. Sizes scale with `GrimoireBook.size`; summon time with `SongRebuildKit.span` (up to +50 percent); summon size with the caster's EP.

## 6. VFX (Time Magic quality bar; nothing of the Time files is touched)
`vfx/client/layer/SongFxLayer` (textures `tools/gen_song_rebuild_textures.py` -> `textures/particle/song_*.png`): FX1 Staccato (a small staff ring and clef contract on the
caster, a head of light with two crossed staff-line ribbons and three spinning notes, a note burst and a sound ring on impact), FX2 Choir Field (a turning staff ring on the
ground, rolling sound rings, eight standing staff lines on the rim, a hovering clef, notes rising in spirals), FX3 Finale (flash, a turning note burst, two sound rings, a
staff ring flaring, notes flung along arcs, a clef rising). SONG_NOTES and SONG_RING keep their payload meaning and use the new glyph sprites. 40 to 108 vertices of the 400.
Preview: `VFX_PREVIEW_BUILD=<dir> python3 -B tools/vfx_preview/preview.py song_cast song_zone song_finale song_notes song_ring` (scenes in `tools/vfx_preview/scenes.d/song_rebuild.json`).

## 7. Render layers (every pass names its render type)
| What | Render type | Why |
|---|---|---|
| armour pieces, fully on | `AuraRender.cutout` (entityCutoutNoCull) | solid brass, drawn first |
| armour pieces while forging or shattering | `AuraRender.translucent` | cutout cannot fade |
| glow map of every piece | `AuraRender.additive` (RenderType.eyes), full bright | after every solid, so nothing covers it |
| rose motes | additive, camera-facing | spectral light, last |
| note prop | cutout model, additive trail and glows | model first |
| choir spirit | translucent model, additive glow, additive note glints | a spirit |
| metronome | cutout model, additive ground rings | |

## 8. Files
New: `book/SongBook`, `SongRebuild`, `SongRebuildKit`, `SongRules`, `SongRebuildSelfTests`; `prop/SongRebuildProps`; `client/aura/SongAura`, `SongArmourKit`;
`client/prop/SongRebuildPropPainter`; `vfx/client/layer/SongFxLayer`; `tools/gen_song_rebuild_geo.py`, `gen_song_rebuild_textures.py`, `song_rebuild_kit.py`,
`song_rebuild_texkit.py`; assets `geo/entity/song_*`, `animations/entity/song_*`, `textures/entity/song_*`, `textures/particle/song_*`; preview scenes; `tools/lang/song_rebuild.json`;
tests `MagicRebuildRulesTest`, `MagicRebuildAssetsTest`. Edited (additions only): see the report (`Aura`, `PropKind`, `VfxShape`, `AuraRegistry`, `PropRegistry`,
`PropPainterRegistry`, `VfxManager`, `WikiBooks`).

## 9. Not verified
Nothing can be run in a game here. Checked: the type check of every new file against the API stubs (with a negative control), the pure rules by JUnit, every geo model with
`tools/geo_check.py` and a glow-size check, and the armour, the models, the VFX scenes by the preview tools, which are re-implementations of the render path. Not seen in the
real game: the real light and blending of the armour, the pose of the pieces on every animation (sneaking, swimming, swinging), the slim-arm fit on a real skin, the network
timing of the props, the choir's movement against real terrain. The page names are literal English strings in code (as the older pages are); the descriptions are in the lang fragment.

## 1.99.0 armour policy

Song Magic has no armour in the anime, so since 1.99.0 **no cast puts the temporary armour of this document on the caster** (policy kind NONE). Everything described above as the armour (the render layer, its painter, geo models, textures and generator) is KEPT on disk and registered in `AuraRegistry`, so old saves and tests still resolve; it is simply never started by a page. What the pages did besides the armour (damage cut, thorns, resistance, constructs, effects) is unchanged. Where the armour carried the magic's look, every cast of a family page now sends a small `SONG_FX2` glow that follows the caster instead (no armour piece is drawn). See `docs/rebuild/armour_policy.md` for the table and the canon reasons; the code is `book.ArmourPolicy` (data: `data/nusmp/armour_policy.json`).
