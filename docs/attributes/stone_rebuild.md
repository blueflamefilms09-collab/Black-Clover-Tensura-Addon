# Stone Magic: the 1.93.0 rebuild

Stone Magic is cut, dressed and carved stone (`MagicType.STONE`, colour `0xFF8F8C86`). A wiki attribute with the generic five-page grimoire; rebuilt with masonry armour, a Stone Sentinel, effects and spells (shared family code: `EarthKit`, `EarthArmorKit`, `EarthFamilyLayer`, `EarthPropPainter`). Distinct from Rock (rough crag) and Earth (soil): square blocks, mortar courses, a crenellated crown, pale-blue runes.

## 1. NEW / REPLACED / KEPT

| Part | Status | What |
|---|---|---|
| Temporary armour | REPLACED | 14 geo pieces (`stone_*`) replace the old wiki shards shell for this magic |
| Construct | NEW | Stone Sentinel (`stone_sentinel`) |
| Effects | REPLACED | `EARTHFAM_*` in the stone palette |
| Pages 0-4 (`attribute_*`) | KEPT ids, order, mastery bits; behaviour REPLACED | |
| Pages 5-6 (`stone_sentinel`, `stone_wall`) | NEW, appended | |
| Sprites | NEW | `earthfam_stone_*` (block, rune, circle, shard, dust) |

## 2. Canon

The wiki lists Stone Magic as an attribute; the page was not reachable from the build session, so no user and no named spell could be read or are claimed. The magic's earlier Tensura-side hook (`MagicFlavors`: STONE) and the name decide the look: dressed stone, walls, guardians, runes. The Domain's "Magic Jamming" (a seal on casting) is a design choice, not canon.

## 3. Armour: Masonry armour

| Piece | Anchor | Shape |
|---|---|---|
| helm | head | a square block helm with a brim and crenellations on top, a runed back plate |
| chest | body | a brick wall plate with a runed centre block and a runed back plate |
| belt | body | block tassets |
| mantle | body | a stone yoke across the shoulders and a hanging runed slab |
| pauldron_r / _l | arms | square stepped blocks with a rune and merlons |
| vambrace_r / _l | arms | square sleeves with a runed side block |
| gauntlet_r / _l | arms | a block fist with a rune |
| greave_r / _l | legs | block boots with kneecaps |
| back | body | two runed tablets stand behind the shoulders, tilted out, bobbing |
| extra | head | a ring of eight runed blocks turning over the head (12 s) |

Material `stone` (ashlar courses, mortar lines, chisel marks, runes in the glow map), glow `0x96E6FF`, style 26 on `Aura.WIKI`.

## 4. Pages

| Id | Name | Tier | What it does |
|---|---|---|---|
| attribute_bolt | Stone Bolt | starter | bolt, bursts r 1.2 for 9, Slowness II 3 s |
| attribute_cut | Stone Manifestation | mid | 8 slabs pushed out along the line: 10, small lift, Slowness II |
| attribute_field | Stone Field | zone | rising slabs r 4.5 for 4 s: 5 every 16 ticks, Slowness III |
| attribute_domain | Stone Domain | signature, cd 45 s | shockwave r 6.5: 16, knockback 1.0, Magic Jamming (Tensura silence, controlTicks 60) when the control lock is free; masonry armour 5 s |
| attribute_guard | Stone Guard | mid | masonry armour 9 s: Resistance II, thorns, +3 melee, 6 percent aura |
| stone_sentinel | Stone Sentinel | mid | the sentinel for 25 s: follows close, turns projectiles back (up to six per pass), hammers the nearest foe for 11 with knockback |
| stone_wall | Stone Wall | mid | wall of stone bricks 6 x 4 for 10 s |

## 5. Construct

`stone_sentinel`: 26 cubes; a masonry knight with a block hammer on the right arm and a tower shield on the left, crenellated helm; clips idle, walk, attack (hammer overhead and down), block (shield forward). The painter flashes the shield rune circle when it turns a projectile.

## 6. Effects and render layers

Cast: a spinning block with orbiting runes and a shard trail; zone: runic circle on a stained floor with stone slabs leaning out of the rim; burst: blocks rise, hover and settle with runes above them. Render types as in the Sandstone doc.

## 7. Self-tests and checks

As the Sandstone doc (scenes `ef_stone_close`, `efm_stone_sentinel_*`, `efp_sentinel`, `efx_stone_*`).

## 8. NOT verified

Nothing was run in the game. Specific to Stone: the projectile turning (Tensura and modded projectiles that are not `Projectile` are not touched), the silence effect id `"silence"` through `EnergyBridge.effect` (copied from existing use, not run), wall block placement on uneven ground.
