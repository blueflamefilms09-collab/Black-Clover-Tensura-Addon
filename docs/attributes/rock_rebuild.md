# Rock Magic: the 1.93.0 rebuild

Rock Magic is raw, jagged crag and boulder (`MagicType.ROCK`, colour `0xFF777C86`). A wiki attribute whose grimoire was the generic five-page book; rebuilt with its own armour, a titan, thrown boulders, effects and spells, sharing the family code (`EarthKit`, `EarthArmorKit`, `EarthFamilyLayer`, `EarthPropPainter`). It is told apart from Stone (cut masonry) and Earth (soil) by silhouette, material and palette: dark slate, angular, with amber seams.

## 1. NEW / REPLACED / KEPT

| Part | Status | What |
|---|---|---|
| Temporary armour | REPLACED | the old WIKI shards shell for this magic becomes 14 geo pieces (`rock_*`, set `rock`) driven by `EquipAnimation` |
| Constructs | NEW | Boulder Titan (`rock_titan`), the boulder of Rock Bolt (`rock_boulder`) |
| Effects | REPLACED | `EARTHFAM_*` in the rock palette instead of `WIKI_MAGIC_*` |
| Pages 0-4 (`attribute_*`) | KEPT ids, order, mastery bits; behaviour REPLACED | |
| Pages 5-6 (`boulder_titan`, `boulder_barrage`) | NEW, appended | |
| Sprites | NEW | `earthfam_rock_*` (chunk, dust, crack, ring, sigil) |

## 2. Canon

The wiki lists Rock Magic as an attribute (the page was not reachable in the build session, so its users and spells could not be read). No spell name is claimed as canon. What is built is what the attribute name and the mod's colour say: boulders thrown, ground broken into crags, rock dropped on an area, a body wrapped in stone. Where the mod's own earlier text promised a bolt, a line, a field, a domain and a guard, those five roles are kept.

## 3. Armour: Boulder armour

| Piece | Anchor | Shape |
|---|---|---|
| helm | head | heavy brow block over the eyes, cheek chunks, a crag ridge, a nose ridge |
| chest | body | a cuirass of stacked chunks with amber seams |
| belt | body | hanging tassets |
| mantle | body | a spine of crags down the back and a rough collar |
| pauldron_r / _l | arms | a boulder cluster on a flat base plate |
| vambrace_r / _l | arms | slab sleeve with rock knuckles |
| gauntlet_r / _l | arms | an oversized boulder fist hanging below the arm |
| greave_r / _l | legs | boulders over the shin, a knee crag, a heavy foot |
| back | body | four boulders orbit the body (one 9 s turn) |
| extra | head | (one hidden cube; Rock has no head halo) |

`tools/gen_earth_family_armor.py` (`rock_set`), material `rock` (slate ramp, dark fissures, one lit amber seam per face), glow `0xFF9640`, style 11 on `Aura.WIKI`.

## 4. Pages

| Id | Name | Tier | What it does |
|---|---|---|---|
| attribute_bolt | Rock Bolt | starter | a boulder prop thrown at 1.45 blocks per tick (arcs), bursts on the first foe or block: r 2.6, 11 x size, knockback |
| attribute_cut | Rock Manifestation | mid | 8 crags erupt along the line, 11 each, lift 0.6, knockback |
| attribute_field | Rock Field | zone | rockfall r 5 for 4 s: 6 every 14 ticks, Slowness II, knockback |
| attribute_domain | Rock Domain | signature, cd 45 s | quake nova r 7: 17, knockback 1.0, Weakness II 4 s, boulder armour 5 s |
| attribute_guard | Rock Guard | mid | boulder armour 9 s: Resistance II, thorns, +4 melee, 6 percent aura |
| boulder_titan | Boulder Titan | signature, cd 45 s | the titan for 30 s (+EP) with a 9 point shock where it rises: stomps (13, r 3.6, lift, slow), roars every 8 s (knockback and Weakness II; hard control, shared lock) |
| boulder_barrage | Boulder Barrage | mid | three boulders in a 34 degree fan |

## 5. Constructs

`rock_titan`: 59 cubes, skeleton pelvis / legs / chest (hunched 14 degrees) / head / arms with fists, an `orbit` bone of five rocks; clips idle, walk, attack (a two-fisted slam), roar (fists beat the chest, head back). `rock_boulder`: a tumbling boulder (9 cubes, glow cracks). Painter extras: grit falling off the crags, an expanding debris ring on the roar, a dust trail behind the boulder.

## 6. Effects and render layers

Cast: a spinning chunk with a hot seam and dust puffs; zone: fissure sigil (jagged triangle), a rim of stone spikes, pulse rings; burst: a glowing fissure decal on the ground, a debris ring, chunks in a heavy arc. Render types as in the Sandstone doc (cutout, translucent while forging or shattering, additive glow, translucent shadow and dust). Vertex budget: zone 376 of 400 before the trim to eight spikes, 316 after.

## 7. Self-tests and checks

As the Sandstone doc (`EarthFamilyRebuildTest`, the two self-tests, geo check, scenes `ef_rock_*`, `efm_rock_*`, `efp_titan`, `efp_boulder`, `efx_rock_*`).

## 8. NOT verified

Nothing was run in the game; see the Sandstone doc for what the previews cannot show. Specific to Rock: the boulder's flight arc and its collision with blocks (`move` plus `horizontalCollision`), the titan's step height, the orbit rocks' look on a real skin.

## 1.99.0 armour policy

Rock Magic has no armour in the anime, so since 1.99.0 **no cast puts the temporary armour of this document on the caster** (policy kind NONE). Everything described above as the armour (the render layer, its painter, geo models, textures and generator) is KEPT on disk and registered in `AuraRegistry`, so old saves and tests still resolve; it is simply never started by a page. What the pages did besides the armour (damage cut, thorns, resistance, constructs, effects) is unchanged. Where the armour carried the magic's look, every cast of a family page now sends a small `EARTHFAM_ZONE` glow that follows the caster instead (no armour piece is drawn). See `docs/rebuild/armour_policy.md` for the table and the canon reasons; the code is `book.ArmourPolicy` (data: `data/nusmp/armour_policy.json`).
