# Vortex Magic rebuild (1.93.0)

Pipeline of `docs/rebuild/magic_rebuild_brief.md`. Key `vortex`, `MagicType.VORTEX`, colour `59B8C9`, hit effect pull, guard effect speed.

## NEW / REPLACED / KEPT

| Part | Status | What |
|---|---|---|
| Armour `Aura.VORTEX`, `client/aura/VortexAura` | NEW (Vortex drew the shared `WikiAura` SPIRAL ribbons, untouched for the others) | ten geo pieces, rings of wind that turn |
| Summons `PropKind.VORTEX_1`, `VortexProps`, `VortexPropPainter` | NEW | maelstrom core, cyclone funnel |
| VFX `VORTEX_FX1..3`, `VortexLayer`, `textures/particle/vortex_*` | REPLACED for Vortex | |
| Pages 0-4 (`attribute_bolt`, `attribute_cut`, `attribute_field`, `attribute_domain`, `attribute_guard`) | KEPT ids / order / tiers / cooldown, behaviour REPLACED | |
| Pages 5-7 (`vortex_step`, `maelstrom_core`, `spiral_gale`) | NEW, appended | |

## Canon
The wiki is unreachable from the build environment and `.github/wiki` has no Vortex page. The spells are the nearest honest version of Vortex Magic (spinning wind that draws things in); no named user is asserted; names are generic.

## Armour (`tools/gen_vortex_rebuild.py`, delays in ticks)
chest (body, 0, front): plate, spiral emblem of concentric rings, glowing eye (glow pass); helm (head, 2, above): brow band, a cone of turning rings as crest (clip idle), two swirl fins; pauld_r / _l (arm, 3 / 4): spiral shells of stacked turning plates; vamb_r / _l (arm, 5 / 6): two tilted wind rings turning round the forearm, cyclone gauntlet; belt (body, 6, below): three tilted whirling rings; back (body, 8, behind): the mantle (three bands turning at three speeds, a tail of tapering rings); greave_r / _l (leg, 9 / 10): spiral boots with a turning ankle ring and vanes. Render types: pieces CUTOUT assembled / TRANSLUCENT while forging or shattering, glow map ADDITIVE on the chest only; dust TRANSLUCENT; ground spiral and wind streaks ADDITIVE. Slim arms narrowed to 88 percent. About 9000 vertices assembled (the rings are many small cubes; the heaviest set of the six).

## Summon models
`vortex_core` (45 cubes: glowing core, three gyroscope rings on three axes, five motes; clips idle, implode), `vortex_funnel` (96 cubes: eight rings widening upward, each turning against its neighbour). `VORTEX_1`: variant 0 core (pulls everything within 8 blocks, a boss at a third of the strength, 2 damage x EP power per 0.5 s inside 3 blocks, implodes for 10 in 4.5 blocks), 1 funnel (walks 0.3 blocks a tick along its yaw, pulls and lifts within 3.5 blocks, 3 damage x EP power per 0.5 s, throws things out at the end). Pulls are velocity changes only.

## Spells
| # | id | Name | Tier | Effect |
|---|---|---|---|---|
| 0 | `attribute_bolt` | Vortex Shot | starter | bolt, 9 damage in 1.5 blocks, pull within 2.4 |
| 1 | `attribute_cut` | Spiral Lance | mid | `ElementBook.line` 11 damage, 11 blocks, pull towards the caster |
| 2 | `attribute_field` | Whirlpool | zone | r 5 x size, 6 s: pull every 4 ticks, 2 damage and slow per second |
| 3 | `attribute_domain` | Great Vortex | signature, 900 | funnel prop 8 s (up to +50 percent), 5 s armour |
| 4 | `attribute_guard` | Vortex Mantle | mid | 9 s: projectiles within 3.5 blocks swept away (per-player tick hook), empower +4 and thorns as before, guard effect, armour set |
| 5 | `vortex_step` | Vortex Step | mid | `ElementBook.dash` 10 blocks, 6 damage, pull |
| 6 | `maelstrom_core` | Maelstrom Core | zone | core prop 8 s |
| 7 | `spiral_gale` | Spiral Gale | zone | cone 9 blocks, 10 damage, push 1.1, lift |

## VFX
FX1: charge spiral facing the shot, a drill of four spiral discs turning against each other, wind streaks winding round it, curved gust blades, whirled dust, a bloom spiral. FX2: two ground spirals turning against each other, a funnel of three discs rising and narrowing, streaks winding inward, dust, glints. FX3: ground spiral shock and ring, an unwinding spiral facing the camera, gust blades in a ring, dust. Textures: vortex_spiral, vortex_ring, vortex_streak, vortex_core, vortex_dust, vortex_arc. Previews: `vfx_preview vortex_cast vortex_zone vortex_burst`; `render_preview vortex_funnel_view vortex_aura_armour vortex_prop_core vortex_prop_funnel`.

## Not verified
Nothing was run in a game; previews are software re-implementations over stubs. The wiki could not be read. The funnel ignores terrain (it walks at its spawn height); pull strengths and the vertex cost of the rings are untested in play.

## 1.99.0 armour policy

Vortex Magic has no armour in the anime, so since 1.99.0 **no cast puts the temporary armour of this document on the caster** (policy kind NONE). Everything described above as the armour (the render layer, its painter, geo models, textures and generator) is KEPT on disk and registered in `AuraRegistry`, so old saves and tests still resolve; it is simply never started by a page. What the pages did besides the armour (damage cut, thorns, resistance, constructs, effects) is unchanged. Where the armour carried the magic's look, every cast of a family page now sends a small `VORTEX_FX2` glow that follows the caster instead (no armour piece is drawn). See `docs/rebuild/armour_policy.md` for the table and the canon reasons; the code is `book.ArmourPolicy` (data: `data/nusmp/armour_policy.json`).
