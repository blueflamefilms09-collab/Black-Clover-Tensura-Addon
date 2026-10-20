# Snow Magic rebuild (1.93.0)

Pipeline of `docs/rebuild/magic_rebuild_brief.md`. Key `snow`, `MagicType.SNOW`, colour `E7F7FF`, hit effect freeze, guard effect damage resistance.

## NEW / REPLACED / KEPT

| Part | Status | What |
|---|---|---|
| Armour `Aura.SNOW`, `client/aura/SnowAura` | NEW (Snow drew the shared `WikiAura` MOTES veil, untouched for the others) | ten geo pieces forged / shattered by `EquipAnimation` |
| Summons `PropKind.SNOW_1`, `SnowProps`, `SnowPropPainter` | NEW | snow golem, frost crystal |
| VFX `SNOW_FX1..3`, `SnowLayer`, `textures/particle/snow_*` | REPLACED for Snow (the `WIKI_MAGIC_*` layer is untouched) | |
| Pages 0-4 (`attribute_bolt`, `attribute_cut`, `attribute_field`, `attribute_domain`, `attribute_guard`) | KEPT ids / order / tiers / cooldown, behaviour REPLACED | |
| Pages 5-7 (`snow_drift`, `frost_crystal`, `whiteout`) | NEW, appended | |

## Canon
The wiki is unreachable from the build environment and `.github/wiki` has no Snow page. The spells are the nearest honest version of Snow Magic (snow and cold made into bullets, blades, storms and soldiers); no named user is asserted and the spell names are generic.

## Armour (`tools/gen_snow_rebuild.py`, delays in ticks)
chest (body, 0, front): drift plate, ice snowflake with glowing core, fur collar; helm (head, 2, above): snow hood, fur brow, crown of five icicles; pauld_r / _l (arm, 3 / 4): snow drift with three icicle spears; vamb_r / _l (arm, 5 / 6): fur cuff, ice gauntlet, frost thorns; belt (body, 6, below): fur belt with icicles; back (body, 8, behind): turning snowflake halo (clip idle) and icicle cape; greave_r / _l (leg, 9 / 10, below): snow boots, fur cuffs, ice spikes. Render types: pieces CUTOUT when assembled, TRANSLUCENT while forging / shattering, glow maps ADDITIVE (chest and back only); falling flakes TRANSLUCENT; frost ring TRANSLUCENT flat 3 cm up; glints and a second ring ADDITIVE last. Slim arms: arm pieces narrowed to 88 percent. About 8100 vertices assembled.

## Summon models
`snow_golem` (62 cubes: chunky snow body, fur yoke, ice plate and heart, ice horns, shoulder spears; clips idle, strike, crumble), `snow_crystal` (40 cubes: tall ice crystal, six satellites, ring of drifts; clips idle, grow). `SNOW_1` variants: 0 golem (follows the owner, slams the nearest hostile within 12 blocks: 8 damage x EP power in 2.2 blocks, slow II 3 s, frostbite; hittable, 40 health; crumbles over its last 24 ticks), 1 crystal (every second 3 damage and slow within 4.5 blocks, shatters at the end for 6).

## Spells
| # | id | Name | Tier | Effect |
|---|---|---|---|---|
| 0 | `attribute_bolt` | Snow Bullet | starter | `ElementBook.bolt` 9 damage, burst 1.3, slow II 3.5 s + frostbite |
| 1 | `attribute_cut` | Snow Crescent | mid | `ElementBook.line` 10 damage, 9 blocks, slow + frostbite |
| 2 | `attribute_field` | Blizzard | zone | r 4.8 x size, 6 s: slow, frostbite, 2 damage per 0.8 s, pushed out |
| 3 | `attribute_domain` | Snow Golem | signature, 900 | golem 20 s (up to +50 percent), 5 s armour |
| 4 | `attribute_guard` | Snow Armour | mid | 9 s: resistance (guard effect), empower +4 and thorns as before, melee attackers chilled, armour set |
| 5 | `snow_drift` | Snow Drift | mid | `ElementBook.dash` 10 blocks, 5 damage, chill |
| 6 | `frost_crystal` | Frost Crystal | zone | crystal prop 10 s |
| 7 | `whiteout` | Whiteout | zone | r 7: 8 damage, blind, slow III |
Damage via `GrimoireBook.hurt` / `BalanceLaw.damage`; durations of effects via `BalanceLaw.controlTicks` (bosses shortened).

## VFX
FX1 flake charge, packed head with flake halo, flake helix, streak and shards, bloom flake. FX2 great flake sigil and tick ring on the ground, low clouds, falling flakes on slanted lines, wind streaks, glints. FX3 flash, ring, powder puffs, ice shards, glints. Textures: snow_puff, snow_flake, snow_streak, snow_ring, snow_glint, snow_shard. Previews: `vfx_preview snow_cast snow_zone snow_burst`; `render_preview snow_golem_view snow_aura_armour snow_prop_golem snow_prop_crystal`.

## Not verified
Nothing was run in a game; previews are software re-implementations over stubs. The wiki could not be read. Numbers are untested in play.

## 1.99.0 armour policy

Snow Magic has no armour in the anime, so since 1.99.0 **no cast puts the temporary armour of this document on the caster** (policy kind NONE). Everything described above as the armour (the render layer, its painter, geo models, textures and generator) is KEPT on disk and registered in `AuraRegistry`, so old saves and tests still resolve; it is simply never started by a page. What the pages did besides the armour (damage cut, thorns, resistance, constructs, effects) is unchanged. Where the armour carried the magic's look, every cast of a family page now sends a small `SNOW_FX2` glow that follows the caster instead (no armour piece is drawn). See `docs/rebuild/armour_policy.md` for the table and the canon reasons; the code is `book.ArmourPolicy` (data: `data/nusmp/armour_policy.json`).
