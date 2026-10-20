# Air Magic rebuild (1.93.0)

Pipeline of `docs/rebuild/magic_rebuild_brief.md`. Key `air`, `MagicType.AIR`, colour `9FE8FF`, hit effect push, guard effect speed.

## NEW / REPLACED / KEPT

| Part | Status | What |
|---|---|---|
| Armour `Aura.AIR`, `client/aura/AirAura` | NEW (Air drew the shared `WikiAura` MOTES veil, untouched for the others) | ten translucent geo pieces |
| Summons `PropKind.AIR_1`, `AirProps`, `AirPropPainter` | NEW | air blade (flying and orbiting), pressure orb |
| VFX `AIR_FX1..3`, `AirLayer`, `textures/particle/air_*` | REPLACED for Air | |
| Pages 0-4 (`attribute_bolt`, `attribute_cut`, `attribute_field`, `attribute_domain`, `attribute_guard`) | KEPT ids / order / tiers / cooldown, behaviour REPLACED | |
| Pages 5-7 (`air_step`, `pressure_burst`, `vacuum_seal`) | NEW, appended | |
| `book/AirBook` | NEW class; also the one dispatch `AirBook.rebuilt(magic)` that `WikiBooks.attribute` calls first (Smoke, Snow, Wing, Vortex, Air) | |

## Canon
The wiki is unreachable from the build environment and `.github/wiki` has no Air page. The spells are the nearest honest version of Air Magic (compressed and cut air); no named user is asserted; names are generic.

## Armour (`tools/gen_air_rebuild.py`, delays in ticks)
The air armour is glass: the textures carry an alpha channel (80 to 190 for the panes) and every piece is drawn TRANSLUCENT, assembled or not (`RebuildSetKit.paint(..., translucent = true)`). chest (body, 0, front): layered plates, four compression cores (glow); helm (head, 2, above): a blade fin and cheek vanes; pauld_r / _l (arm, 3 / 4): three fanned blades; vamb_r / _l (arm, 5 / 6): two long vanes and a pressure orb fist; belt (body, 6, below): band and five turning orbs; back (body, 8, behind): a halo of vanes turning slowly, five orbiting orbs, two pairs of blade vanes (glow); greave_r / _l (leg, 9 / 10): vaned boots, a cone of rushing air under the sole. Extra passes ADDITIVE: ripples on the ground, speed lines racing up the body, glints. Slim arms narrowed to 88 percent. About 4200 vertices assembled (the lightest set).

## Summon models
`air_blade` (13 cubes, a crescent with a bright edge and a glowing hub; clip idle spin), `air_orb` (38 cubes: core, glassy ball, two shell rings, six vanes; clips idle, swell). `AIR_1`: variant 0 flying blade (0.9 blocks a tick along yaw and pitch for 20 ticks, once per foe 11 damage x EP power and a push aside, stops at walls), 1 pressure orb (30 ticks, then 12 damage x EP power within 4.5 blocks and a push of 1.4, 0.4 on a boss), 2 orbiting blade (circles the owner, flies at a hostile within 10 once a second: 5 damage x EP power).

## Spells
| # | id | Name | Tier | Effect |
|---|---|---|---|---|
| 0 | `attribute_bolt` | Air Bullet | starter | `ElementBook.bolt` 8 damage, speed 2.6, burst 1.2, push 1.1 |
| 1 | `attribute_cut` | Air Blade | mid | blade prop (scale 1.3) |
| 2 | `attribute_field` | Dead Air | zone | r 4.5 x size, 5 s: slow II, weakness, 2 damage per second, silence (control gate, `controlTicks`), bolts and enemy projectiles removed |
| 3 | `attribute_domain` | Air Blade Storm | signature, 900 | five orbiting blades 15 s (up to +50 percent), 5 s armour |
| 4 | `attribute_guard` | Air Cushion Armour | mid | 9 s: no fall damage, 35 percent of projectile damage cancelled, empower +4 and thorns as before, guard effect, armour set |
| 5 | `air_step` | Air Step | mid | `ElementBook.dash` 9 blocks, 5 damage, +0.45 up |
| 6 | `pressure_burst` | Pressure Burst | zone | orb prop |
| 7 | `vacuum_seal` | Vacuum Seal | zone | control gate, pull in front (not a boss), silence and slow III for `controlTicks(t, 80)`, 6 damage |
The silence is `EnergyBridge.effect(t, "silence", ticks, 0)` as in the authoring guide.

## VFX
FX1: a swirl winding up facing the shot, six ripple rings forming the shock cone, bright core, speed lines, two cut crescents, a ripple at the end. FX2: three ripples pulsing out, two vertical swirls, speed lines racing inward and up, glints. FX3: flash, three ground rings and one facing the camera, radial speed lines, crescents thrown out, glints. All ADDITIVE. Textures: air_ring, air_streak, air_swirl, air_orb, air_slash, air_glint. Previews: `vfx_preview air_cast air_zone air_burst`; `render_preview air_orb_view air_aura_armour air_prop_blade air_prop_orb`.

## Not verified
Nothing was run in a game; previews are software re-implementations over stubs. The translucent sorting of the pieces over the player is unseen in the real renderer. The wiki could not be read. Numbers are untested in play.

## 1.99.0 armour policy

Air Magic has no armour in the anime, so since 1.99.0 **no cast puts the temporary armour of this document on the caster** (policy kind NONE). Everything described above as the armour (the render layer, its painter, geo models, textures and generator) is KEPT on disk and registered in `AuraRegistry`, so old saves and tests still resolve; it is simply never started by a page. What the pages did besides the armour (damage cut, thorns, resistance, constructs, effects) is unchanged. Where the armour carried the magic's look, every cast of a family page now sends a small `AIR_FX2` glow that follows the caster instead (no armour piece is drawn). See `docs/rebuild/armour_policy.md` for the table and the canon reasons; the code is `book.ArmourPolicy` (data: `data/nusmp/armour_policy.json`).
