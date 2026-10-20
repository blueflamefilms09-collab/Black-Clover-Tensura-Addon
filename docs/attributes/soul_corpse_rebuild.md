# Soul Corpse Magic: the 1.93.0 rebuild

Soul Corpse Magic (`MagicType.SOUL_CORPSE`, book `nusmp:book_soul_corpse`, colour 0xFF6C7899 slate) used to be the generic wiki attribute book plus Rades Spirito's five pages
(Reanimation, the three named puppets, True Soul Corpse Magic). It is now `book/SoulCorpseBook` with its own armour, summon models, effect layer and spells.

## 1. Canon and what could not be checked
The Black Clover wiki could not be reached from the build environment and the repo has no wiki page for Soul Corpse Magic. The repo gives it to Rades Spirito (`corpse` package:
Reanimation raises the dead of the last minute, three named puppets Jimmy, Alfredo and Carl, a pact, True Soul Corpse Magic); the new pages are original on that theme (a
puppeteer's craft: threads, stitched marionettes, hands out of the grave). No anime character is named for the new pages.

## 2. NEW / REPLACED / KEPT
| Part | Status | What |
|---|---|---|
| Book class | REPLACED | `WikiBooks.attribute(SOUL_CORPSE)` returns `SoulCorpseBook` (`book/SongRebuild` dispatch) |
| Pages `attribute_bolt` `attribute_cut` `attribute_field` `attribute_domain` `attribute_guard` | REPLACED (same ids, order, mastery bits) | Marionette Thread, Puppet Strings, Grave Field, Necropolis, Corpse Plate |
| Pages `corpse_reanimate` `corpse_jimmy` `corpse_alfredo` `corpse_carl` `true_soul_corpse` | KEPT | modes 5 to 9, the whole `corpse` package (corpses, pacts, named corpses, their renderer) untouched |
| Pages `corpse_marionette` `corpse_swap` `corpse_legion` | NEW (appended, modes 10 to 12) | 13 family pages of the 24 allowed |
| Armour `Aura.SOUL_CORPSE` (`client/aura/SoulCorpseAura`) | NEW | Marionette Shell, 13 pieces |
| Models `soulcorpse_puppet`, `soulcorpse_hand`, `soulcorpse_nail` | NEW | props, not entities |
| VFX `SOUL_CORPSE_FX1/2/3` | NEW (appended to `VfxShape`; `CORPSE_RAISE` and `CORPSE_PACT` stay) | |

## 3. The temporary armour: the Marionette Shell (`soulcorpse_armour`)
Every successful cast puts it on for 5 seconds; Corpse Plate keeps it for 9 s. Same forge-in and shatter-out as the Djinn full equip. A puppet's body of stitched leather, burlap,
bone and rusted iron under slate blue-grey, with strings of pale violet light.

| Piece | Anchor | Delay | What |
|---|---|---|---|
| `cowl` | head | 0 | stitched leather cowl over a bone skull mask with hollow glowing eyes, a nail through the crown (hidden in first person) |
| `corset` | body | 2 | leather corset with bone ribs and coffin nails |
| `lid` | body | 5 | a coffin lid on the back |
| `bar` | body | 11 | the puppeteer's control bar floating above the head with strings to the head and shoulders, swaying |
| `chain` | body | 8 | a hanging chain with a hook |
| `pauldron_r/l` | arms | 3 | slate pads with iron spikes |
| `vambrace_r/l` | arms | 5 | bandaged forearms with coffin nails |
| `gauntlet_r/l` | arms | 7 | skeletal hands |
| `greave_r/l` | legs | 9, 10 | bandaged greaves with bone knee caps and chain loops |

## 4. Summon models (`client/prop/SoulCorpseRebuildPropPainter`)
| Model | Prop | What | Clips |
|---|---|---|---|
| `soulcorpse_puppet` (29 cubes) | `SOUL_CORPSE_1` param 0 / 1 | the marionette and the decoy: a stitched corpse with button eyes, jointed limbs and strings up to a small control bar | idle, walk, attack, collapse |
| `soulcorpse_nail` (6 cubes) | `SOUL_CORPSE_1` param 2 | a coffin nail on a thread: the body of the Marionette Thread bolt | idle, strike |
| `soulcorpse_hand` (24 cubes) | `SOUL_CORPSE_2` | a decayed hand bursting out of the ground with a torn forearm; it rises over 14 ticks and sinks over 10 | idle, grasp |

The existing corpses of the `corpse` package are entities with their own renderer and are not changed.

## 5. Spells
| Id | Name | Tier | What | Notes |
|---|---|---|---|---|
| `attribute_bolt` | Marionette Thread | starter | nail bolt, raw 7 in a burst of 1.4, Slowness II, Weakness, pulled 1 block toward you | |
| `attribute_cut` | Puppet Strings | mid | line of 14 blocks, raw 5, Slowness, pulled 1.5 blocks toward you | |
| `attribute_field` | Grave Field | zone | radius 5 for 6 s, five hands that close on foes within 1.8 blocks every 24 ticks: raw 3.5, Slowness III | |
| `attribute_domain` | Necropolis | signature | radius 8: raw 11 and Wither 4 s, two marionettes for 10 s; cooldown 900 | |
| `attribute_guard` | Corpse Plate | mid | Resistance, Absorption, melee +4, thorns, 9 s | |
| `corpse_marionette` | Marionette | mid | a puppet for 25 s: it walks (0.2 blocks per tick, jumps a step) to the nearest foe within 14 blocks and strikes every second for raw 6 and Weakness | cap 4 puppets |
| `corpse_swap` | Corpse Swap | mid | a decoy takes your place, you step up to 6 blocks back, hunting mobs lose their target, the decoy bursts after 3 s (raw 8, shove) | |
| `corpse_legion` | Corpse Legion | signature | four marionettes for 20 s and hands under up to six foes within 8 blocks; cooldown 1800 | |

Numbers in `book/SoulCorpseRules` (pure, unit-tested). Damage through `GrimoireBook.hurt` (EP law), bosses half the raw value and only Slowness I, no hard control (no
`beginControl`). Allies and foes by `song.SongTargets`. Puppets and hands are bound to their caster (`SongRebuildKit.bind`) and end when the caster dies or logs out. Corpse
types and the Mana cost of the existing pages are not changed.

## 6. VFX
`vfx/client/layer/SoulCorpseFxLayer`, textures `tools/gen_soulcorpse_rebuild_textures.py` -> `textures/particle/scorpse_*.png`: FX1 Marionette Thread (a stitch ring and a hand close on the
caster, a nail runs paying out a thread that stays tied to the caster, splinters peel off, the stitch ring snaps shut on the target), FX2 Grave Field (a stitched sigil, a stitch ring,
hands rising and sinking, grave markers on the rim, threads rising), FX3 Necropolis (flash, sigil flaring, a crown of hands thrusting up, bone splinters, threads snapping outward like
a star). 36 to 112 vertices. Scenes `tools/vfx_preview/scenes.d/soulcorpse_rebuild.json`.

## 7. Render layers
Armour: cutout pieces (translucent while forging or shattering), glow maps additive full bright, extras additive (a stitched sigil, strings of light from the bar to both hands, motes). Puppet:
cutout model with the glow map, additive stitch ring. Hand: cutout model, additive stitch ring and grave marker. Order: solid, translucent, additive.

## 8. Files
New: `book/SoulCorpseBook`, `SoulCorpseRules`; `prop/SoulCorpseRebuildProps`; `client/aura/SoulCorpseAura`; `client/prop/SoulCorpseRebuildPropPainter`; `vfx/client/layer/SoulCorpseFxLayer`;
`tools/gen_soulcorpse_rebuild_geo.py`, `gen_soulcorpse_rebuild_textures.py`; assets `*/entity/soulcorpse_*`, `textures/particle/scorpse_*`; scenes; `tools/lang/soul_corpse_rebuild.json`.
(The geo models are keyed `soulcorpse`, without an underscore, because the preview tool and `GeoSpec` split model ids at the first underscore.)

## 9. Not verified
No game run. Verified: type check with negative control, JUnit of `SoulCorpseRules`, geo checks, preview tools. Not seen in the real game: the puppet's walking on real terrain (it uses
`Entity.move` with its own gravity and a one-block jump, so slabs, stairs and water behave as for any prop), the strings' fit to the moving hands, the Wither and slowness against real
mobs. Rades's pages keep their lock lines (`book.CharacterSpells`); the new pages are open to anyone with the magic.

## 1.99.0 armour policy

Soul Corpse Magic has no armour in the anime, so since 1.99.0 **no cast puts the temporary armour of this document on the caster** (policy kind NONE). Everything described above as the armour (the render layer, its painter, geo models, textures and generator) is KEPT on disk and registered in `AuraRegistry`, so old saves and tests still resolve; it is simply never started by a page. What the pages did besides the armour (damage cut, thorns, resistance, constructs, effects) is unchanged. Where the armour carried the magic's look, every cast of a family page now sends a small `SOUL_CORPSE_FX2` glow that follows the caster instead (no armour piece is drawn). See `docs/rebuild/armour_policy.md` for the table and the canon reasons; the code is `book.ArmourPolicy` (data: `data/nusmp/armour_policy.json`).
