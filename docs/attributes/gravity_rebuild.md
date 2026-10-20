# Gravity Magic: the 1.93.0 rebuild

Magic: `MagicType.GRAVITY`, book `book_gravity`, grimoire colour `0x7A3CC8` (user in canon: Dante Zogratis). Part of the 1.93.0 "magic rebuild" (brief: `docs/rebuild/magic_rebuild_brief.md`).

## NEW / REPLACED / KEPT

| Part | Status | What |
|---|---|---|
| Temporary armour | NEW (replaces the empty stub `GravityAura`) | `client/aura/GravityAura` (`Aura.GRAVITY`): the **Demon King's regalia**, 8 GeoLite pieces via `SpatialArmourKit` and `EquipAnimation` |
| Summon models | NEW | `gravity_orb` (singularity, `PropKind.GRAVITY_1`) and `gravity_sword` (compressed rock sword, `PropKind.GRAVITY_2`), `client/prop/GravityPropPainter` (replaces the stub), `prop/GravityProps` |
| Effects | REPLACED (the stub `GravityLayer` drew one glow) | `vfx/client/layer/GravityLayer` for `GRAVITY_FX1..3`; 9 sprites from `tools/gen_gravity_textures.py` |
| Pages | KEPT (ids, order, mastery bits) | `heavy_infighting`, `demon_king`, `gravity_sphere`, `gravity_well`, `crushing_weight` |
| Page casts | REPLACED / wrapped | `gravity_sphere` is now an orb-model bolt (`GravityWorks.sphere`); the other four call their old cast unchanged and add the effects; every page forges the regalia for 5 s |
| Pages | NEW (appended through `GravityExt`) | `heavy_infighting_gladiator`, `evil_gods_pressure_craft`, `lightened_step`, `weighted_blows` |
| Old effects | KEPT | `ZogratisGravityLayer` and its `GRAVITY_WELL / IMPACT / LANES / SINGULARITY / AURA` shapes (the boss's), `SPATIAL_RIFT`, `EARTH_SPIKES` stay registered |

`Gravity Singularity` (the black hole itself, in the wiki's spell list) stays a forbidden ritual and is NOT a page, as `GravityBook` always said; the orb is its model and Gravity Sphere its small form.

## Canon sources and honesty

Offline: no wiki access. The repo's `.github/wiki/gravity_magic.txt` (lower gravity to float, raise it to slam, add weight to an object for damage, lighten it for speed, compress objects into shapes, warp space into small black holes; spells: Presence of the Demon King, Heavy Infighting, Heavy Infighting Gladiator, Gravity Singularity, Evil God's Pressure Craft) and `spell_gravity_singularity.txt`, `spell_evil_god_s_pressure_craft.txt` are the canon used. The regalia is an original design (violet-black stacked iron, descending pauldrons, a crown of spikes) in the spirit of Dante's presence, not a verified canon armour.

## Armour pieces (tools/gen_gravity_geo.py)

| Piece | Anchor | Content | Delay |
|---|---|---|---|
| `gravity_chest` | body | slab cuirass three layers deep, a singularity in a slate ring on the breast, hanging weight plates | 0 |
| `gravity_helm` | head | demon king's crown: band, five tall spikes, two swept horns, cheek guards, nape plate (face free) | 2 |
| `gravity_arm_r` / `_l` | arms | three descending pauldron plates and a spike, weight-studded vambrace, clenched gauntlet (5.6 px wide) | 4 / 5 |
| `gravity_leg_r` / `_l` | legs | thigh plate, knee spike, heavy greave, wide sabaton | 7 / 8 |
| `gravity_back` | body | tilted accretion ring (turning in its own plane), three dark orbs on separate orbits, five mantle plates (clip `idle`) | 10 |
| `gravity_halo` | head | tilted gyroscope of two thin rings and three small weights (clip `idle`) | 12 |

Every piece falls IN from above, weight first (`EquipAnimation` delays 0 to 12, fly-in vectors straight down), and shatters out over the last 18 ticks. Passes: pieces (cutout / translucent while forging) with glow maps; a black pool under the feet (`gravity_disc`, translucent); the pressure ring (crowding inwards), cracks and, for style 1, the sigil (additive); falling pressure streaks and glints (additive). About 190 cubes.

## Summon models

* `gravity_orb` (67 cubes): a black core of six stacked cubes (no glow, so it reads as a hole), three accretion rings of different tilt and speed, four orbiting lensing shards; clips `idle`, `fire` (core stretched along the flight). The painter adds the black disc with its bright rim (alpha) and two lens rings (additive).
* `gravity_sword` (34 cubes): eight layers of compressed rock tapering to a point, glowing fractures, rubble pressed into the flats, a rough guard and grip, shards turning round it; clips `idle`, `launch`. Param = stage | pitch (`prop/GravitySwordParam`, unit-tested): forming rises 0.045 blocks/tick with ground cracks; launched flies by its velocity.

## Spells (EP scale = `EnergyBridge.scale`, 0.9 to 1.5)

| Page | Numbers |
|---|---|
| `heavy_infighting` (kept) | 10 dmg slam within 6, short control lock (shared 12 s gate), +FX |
| `demon_king` (kept) | 8-block domain, 6 s, you are slowed too; the well drawn under you for 6 s |
| `gravity_sphere` | orb bolt: speed 1.2, radius 1.1, life 22 ticks; 11 x scale; slow III 2 s; downward pull; crush where it ends |
| `gravity_well` / `crushing_weight` (kept) | the old `ElementBook.field` / `nova` + `GRAVITY_FX2` / `FX3` on the aimed floor |
| `heavy_infighting_gladiator` (new, mid) | three slams, 8 x scale each, 5 ticks apart, slow IV 1.5 s |
| `evil_gods_pressure_craft` (new, zone) | sword rises 24 ticks, turns 8 ticks, launched at 1.6 blocks/tick, piercing, 26 x scale, 30 percent armour bypass |
| `lightened_step` (new, mid, 400 t) | you and allies within 4: slow falling, jump boost III, speed I for 8 s |
| `weighted_blows` (new, mid, 600 t) | 15 s: melee blows x1.4 (+0.05 per EP step), the foe pressed down and slowed II 1.5 s (hook via `AttributeEvents.incoming`) |

All damage through `GrimoireBook.hurt` (balance law). Lang: `tools/lang/gravity_rebuild.json`.

## Files

`tools/gen_gravity_geo.py`, `tools/gen_gravity_textures.py`, `tools/render_preview/scenes.d/gravity_*.json`, `tools/vfx_preview/scenes.d/gravity.json`;
`book/GravityBook`, `GravityWorks`, `GravityWorksPages`, `book/ext/GravityExt`, `prop/GravitySwordParam`; self-test `magic.gravity_rebuild`; unit test `GravitySwordParamTest`.

## Previews that were run

`preview.py gravity_orb_idle gravity_sword_idle gravity_aura_armour gravity_aura_detail gravity_prop_orb gravity_prop_orb_flying gravity_prop_sword gravity_prop_sword_launched` (render preview) and `vfx_preview gravity_cast gravity_cast_big gravity_zone gravity_burst` (72, 108 and 96 of 400 vertices).

## NOT verified

* Nothing was run in a game; the previews are re-implementations. Real lighting and depth sorting of the translucent black pool, first person, and the Tensura projectile that `gravity_sphere` no longer fires (the old shot was replaced by the orb bolt) are unchecked.
* The sword's orientation at launch (pitch from the aim point) and the bolt's wall stop are unplayed; the prop and the bolt follow the same path but are separate objects.
* The regalia is about 190 cubes (about 8 600 vertices with glow): above the preview's soft budget note.
* Canon: no wiki access; see above.

## 1.99.0 armour policy

Gravity Magic has no armour in the anime, so since 1.99.0 **no cast puts the temporary armour of this document on the caster** (policy kind NONE). Everything described above as the armour (the render layer, its painter, geo models, textures and generator) is KEPT on disk and registered in `AuraRegistry`, so old saves and tests still resolve; it is simply never started by a page. What the pages did besides the armour (damage cut, thorns, resistance, constructs, effects) is unchanged. Where the armour carried the magic's look, every cast of a family page now sends a small `GRAVITY_FX2` glow that follows the caster instead (no armour piece is drawn). See `docs/rebuild/armour_policy.md` for the table and the canon reasons; the code is `book.ArmourPolicy` (data: `data/nusmp/armour_policy.json`).
