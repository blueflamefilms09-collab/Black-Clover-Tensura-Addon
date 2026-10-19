# Soul Magic: the 1.93.0 rebuild

Soul Magic (`MagicType.SOUL`, book `nusmp:book_soul`, colour 0xFF72D9CC teal) used to be the generic wiki attribute book plus Lucius Zogratis's two Paladin pages. It is now
`book/SoulBook` with its own armour, summon models, effect layer and spells.

## 1. Canon and what could not be checked
The Black Clover wiki could not be reached from the build environment and the repo has no wiki page for Soul Magic. The repo itself gives Soul Magic to Lucius Zogratis (the Paladin
Conversion pages, the Soul Paladins of his boss arc); the new pages are original on that theme: the soul as something that can be struck (Spiritual Damage), drained, bound and
harvested, with spirits as the visible summons. No anime character is named for the new pages.

## 2. NEW / REPLACED / KEPT
| Part | Status | What |
|---|---|---|
| Book class | REPLACED | `WikiBooks.attribute(SOUL)` returns `SoulBook` (`book/SongRebuild` dispatch) |
| Pages `attribute_bolt` `attribute_cut` `attribute_field` `attribute_domain` `attribute_guard` | REPLACED (same ids, order, mastery bits) | Soul Flame Bolt, Soul Sever, Soul Vortex, Soul Release, Spirit Mantle |
| Pages `paladin_conversion`, `paladin_recall` | KEPT | modes 5 and 6, `soul.*` untouched (Paladins, their roster, their chat) |
| Pages `soul_wisps` `soul_bind` `soul_harvest` `soul_ascension` | NEW (appended, modes 7 to 10) | 11 family pages of the 24 allowed |
| Armour `Aura.SOUL` (`client/aura/SoulAura`) | NEW | Spirit Raiment, 14 pieces |
| Models `soul_wisp`, `soul_orb` | NEW | |
| VFX `SOUL_FX1/2/3` | NEW (appended to `VfxShape`) | |
| `SOUL_VORTEX` (Lucius's older shape), `PaladinFx` | KEPT | |

## 3. The temporary armour: the Spirit Raiment (`soul_armour`)
Every successful cast puts it on for 5 seconds; Spirit Mantle keeps it for 9 s. Same forge-in and shatter-out as the Djinn full equip. Pale teal soul fire over bone white.

| Piece | Anchor | Delay | Look | What |
|---|---|---|---|---|
| `hood` | head | 0 | ghost | spectral hood, bone mask, hollow glowing eyes, two flame horns and a flame crest (hidden in first person) |
| `ribs` | body | 2 | solid | a ribcage of bone bars round a glowing soul core, spine, mantle |
| `wing_r` `wing_l` | body | 6 | ghost | wings of soul fire, four flame feathers each, beating |
| `wisps` | body | 10 | ghost | three small souls orbiting the torso |
| `tail` | body | 8 | ghost | a tail of soul fire |
| `pauldron_r/l` | arms | 3 | solid | a flame head on a bone cap |
| `vambrace_r/l` | arms | 5 | solid | cloth with three chain rings |
| `gauntlet_r/l` | arms | 7 | solid | spectral claws |
| `greave_r/l` | legs | 9, 10 | ghost | shins that burn away into a flame under the feet |

## 4. Summon models (`client/prop/SoulRebuildPropPainter`)
| Model | Prop | What | Clips |
|---|---|---|---|
| `soul_wisp` (43 cubes) | `SOUL_1` | a soul spirit: skull face with hollow eyes, stepped soul-cloth body tapering to a tail, trailing clawed arms, flame core, flame horns, a halo of six embers; translucent | idle, attack (lunge, jaw wide) |
| `soul_orb` (15 cubes) | `SOUL_2` | the soul-flame orb: a bright core in a shell of flame cubes, a tail, three orbiting small souls | idle, strike |

## 5. Spells
| Id | Name | Tier | What | Notes |
|---|---|---|---|---|
| `attribute_bolt` | Soul Flame Bolt | starter | orb, raw 7 + spiritual 2 in a burst of 1.6, 1.5 percent of the foe's magicules drained to you | |
| `attribute_cut` | Soul Sever | mid | line of 14 blocks, raw 6 + spiritual 2.5, 3 percent drained to you | |
| `attribute_field` | Soul Vortex | zone | radius 5.5 for 5 s: per second raw 3 + spiritual 1.2, 1.5 percent drained, Slowness | |
| `attribute_domain` | Soul Release | signature | radius 8: raw 12 + spiritual 4, Weakness, allies heal 10 percent; cooldown 900 | |
| `attribute_guard` | Spirit Mantle | mid | Absorption II, Regeneration, melee +4, thorns, 9 s | |
| `soul_wisps` | Soul Wisps | mid | three spirits circle you for 20 s and lunge at foes within 10 blocks every 28 ticks: raw 5 + spiritual 1.5 | cap 3 |
| `soul_bind` | Soul Bind | mid | the foe in sight: three pulses 1 s apart of raw 3 + spiritual 2, Weakness II and Glowing 6 s, 2 percent of your health back per pulse | |
| `soul_harvest` | Soul Harvest | zone | 10 s: every foe dying within 16 blocks returns 6 percent of your magicules and 8 percent of your health (first 6 souls) | hook `AttributeEvents.death` |
| `soul_ascension` | Soul Ascension | signature | three wisps rise round you and detonate after 2 s: raw 10 + spiritual 3 within 6 blocks of each, 15 percent of your health back; cooldown 1800 | |

Spiritual Damage is `EnergyBridge.spirit` (it bypasses armour; at the spiritual floor the mod's existing Soul Annihilation rule applies, nothing new), scaled by `SoulRules.spirit`
(EP scale, capped at 80, halved on bosses). Body damage through `GrimoireBook.hurt` (EP law), bosses half the raw value, healing through `BalanceLaw.heal`, no hard control.

## 6. VFX
`vfx/client/layer/SoulFxLayer`, textures `tools/gen_soul_rebuild_textures.py` -> `textures/particle/soul_*.png`: FX1 Soul Flame Bolt (rune ring and a soul contract, a flame head with
two ribbons and three peeling souls, a skull flash and a rising soul on impact), FX2 Soul Vortex (a turning five-armed vortex and a counter-turning rune ring, souls spiralling in,
a flame column, rim flames), FX3 Soul Release (flash, a skull opening, rune ring and vortex flaring, souls leaving upward, ribbons rising). 40 to 96 vertices. Scenes
`tools/vfx_preview/scenes.d/soul_rebuild.json`.

## 7. Render layers
Armour: solid pieces cutout (translucent while forging or shattering), ghost pieces translucent, glow maps additive full bright, extras additive (rune ring, flames, core glow). Wisp:
translucent model, additive glow and flames. Orb: cutout model, additive flames and glows. Order: solid, translucent, additive.

## 8. Files
New: `book/SoulBook`, `SoulRules`; `prop/SoulRebuildProps`; `client/aura/SoulAura`; `client/prop/SoulRebuildPropPainter`; `vfx/client/layer/SoulFxLayer`; `tools/gen_soul_rebuild_geo.py`,
`gen_soul_rebuild_textures.py`; assets `*/entity/soul_*`, `textures/particle/soul_*`; scenes; `tools/lang/soul_rebuild.json`.

## 9. Not verified
No game run. Verified: type check with negative control, JUnit of `SoulRules`, geo checks, preview tools. Not seen in the real game: the ghost pieces' blending on top of a real skin and
other layers, the wisps' movement against terrain, the real size of Spiritual Damage numbers against Tensura's spiritual health pools (the numbers are small and capped on purpose).
