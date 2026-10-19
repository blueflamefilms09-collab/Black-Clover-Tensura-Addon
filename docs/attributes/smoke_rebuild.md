# Smoke Magic rebuild (1.93.0)

Written by the rebuild pipeline of `docs/rebuild/magic_rebuild_brief.md`. Key `smoke`, `MagicType.SMOKE`, colour `82788F`, hit effect blindness, guard effect invisibility.

## NEW / REPLACED / KEPT

| Part | Status | What |
|---|---|---|
| Armour (`Aura.SMOKE`, `client/aura/SmokeAura`) | NEW (Smoke had the shared `WikiAura` MOTES veil; that painter stays for the other wiki magics) | ten geo pieces forged in / shattered out by `EquipAnimation` |
| Summons (`PropKind.SMOKE_1`, `SmokeProps`, `SmokePropPainter`) | NEW | smoke clone, smoke serpent, coiling serpent (geo models) |
| VFX (`SMOKE_FX1..3`, `SmokeLayer`, `textures/particle/smoke_*`) | REPLACED for Smoke (it drew the shared `WIKI_MAGIC_*` layer; that layer is untouched) | |
| Pages 0-4 (`attribute_bolt`, `attribute_cut`, `attribute_field`, `attribute_domain`, `attribute_guard`) | KEPT ids, order, tiers, cooldown; behaviour REPLACED | canon-style names and mechanics |
| Pages 5-7 (`smoke_step`, `coiling_serpent`, `smoke_wave`) | NEW, appended | |
| `WikiBooks.attribute` generic five pages | KEPT for the other wiki magics | |

## Canon sources
The Black Clover wiki could not be reached from the build environment (the proxy refuses `blackclover.fandom.com`), and the repo's `.github/wiki` holds no Smoke page. The spells are therefore the nearest honest version of what Smoke Magic is in the story (creating and manipulating smoke to blind, hide and mislead), not a 1:1 copy of named anime spells; no named user is asserted. Names are generic and meant to be replaced if the wiki text is later supplied.

## Armour pieces (`tools/gen_smoke_rebuild.py`)
Delay = stagger in ticks, fly-in = model pixels in the anchor part's frame. Pieces never touch an inventory or armour slot.

| Piece | Anchor | Delay | Fly-in | Contents |
|---|---|---|---|---|
| chest | body | 0 | front | cloud collar, banded plate, ember core (glow pass) |
| helm | head | 2 | above | hood of billows, brow band, ember eyes, two plumes (glow pass, clip idle) |
| pauld_r / pauld_l | arms | 3 / 4 | outside | heaped billows with an ember |
| vamb_r / vamb_l | arms | 5 / 6 | front | puff cuff and a smoke fist |
| belt | body | 6 | below | sash of puffs, tassets (clip idle) |
| back | body | 8 | behind | the mantle (three tiers of billows to the knees), two plumes (clip idle) |
| greave_r / greave_l | legs | 9 / 10 | below | smoke boots, knee billow |

Render layers in order: pieces CUTOUT once assembled, TRANSLUCENT while forging or shattering, glow maps ADDITIVE; smoke puff sprites TRANSLUCENT; ground ring TRANSLUCENT (flat, 3 cm up); ember sparks ADDITIVE last. Arm pieces are narrowed to 88 percent for slim skins; head / body pieces are skipped for the local player in first person. About 7400 vertices at full assembly.

## Summon models
`smoke_clone` (57 cubes: stacked billow torso, head with ember eyes, arms, plume instead of legs; clips idle, strike, vanish), `smoke_serpent` (44 cubes: ten segments, head with jaw and fangs; clips idle, strike). `SMOKE_1` variants (low byte of `param`): 0 clone, 1 serpent, 2 coil (three small serpents circling the held foe).

## Spells and numbers
| # | id | Name | Tier / cd | Effect |
|---|---|---|---|---|
| 0 | `attribute_bolt` | Smoke Bullet | starter / 80 | 9 damage in a 1.7 block burst, blind 3 s (shortened on bosses by `BalanceLaw.controlTicks`), a cloud for 2.5 s (r 2.2): blind and slow every 0.5 s |
| 1 | `attribute_cut` | Smoke Serpent | mid / 160 | prop lunges 10 blocks, 10 damage x EP power (1 to 2) once per foe, blindness 2.5 s |
| 2 | `attribute_field` | Smoke Screen | zone / 300 | r 4.5 x size, 5 s: blind + slow, 2 damage per second, mobs lose the caster, allies in it invisible |
| 3 | `attribute_domain` | Smoke Clones | signature / 900 | 3 clones for 10 s (up to +50 percent with EP): 5 damage per strike, blind aura r 3.5, burst r 4; caster invisible 5 s, 5 s armour |
| 4 | `attribute_guard` | Smoke Armour | mid / 160 | 9 s: 30 percent of blows cancelled (not void damage), empower +4 and thorns as before, speed I, guard effect, armour set |
| 5 | `smoke_step` | Smoke Step | mid | `ElementBook.dash` 11 blocks, 6 damage along the path, blind, invisible 2 s |
| 6 | `coiling_serpent` | Coiling Serpent | zone | `GrimoireBook.control` gate, `controlTicks(target, 60)`, 5 + 3 per 0.5 s damage |
| 7 | `smoke_wave` | Smoke Wave | zone | cone 9 blocks, 11 damage, push, blind |

All damage through `GrimoireBook.hurt` (balance law, boss cap) or `AirPropKit.hit` (`BalanceLaw.damage`). Props only auto-target hostile mobs.

## VFX (`SmokeLayer`, textures made by `tools/gen_smoke_rebuild.py`)
FX1 cast: ember flare + sigil gather, dense smoke head with ember core, fourteen swelling puffs, three curling wisps, sparks, bloom at the end. FX2 zone: smoke sigil on the ground with a counter-turning ember sigil, wall of billows on the rim, billows rolling up, curls, sparks. FX3 burst: ember flash, ground ring, ten billows thrown out, four lingering puffs, curls, sparks. Textures: smoke_puff, smoke_billow, smoke_wisp, smoke_ring, smoke_sigil, smoke_curl, smoke_ember. Preview: `python3 -B tools/vfx_preview/preview.py smoke_cast smoke_zone smoke_burst`, `python3 -B tools/render_preview/preview.py smoke_clone_view smoke_aura_armour` (scenes in `scenes.d/smoke*.json`).

## Not verified
Nothing was run in a game. The previews are re-implementations (a software rasteriser over stub Minecraft classes): shading, translucency sorting and the exact look of the pieces on a real player are unseen. The wiki could not be read. Balance numbers are untested in play. The `EquipAnimation` anchoring was checked in the preview only.
