# Plant Magic rebuild (1.93.0)

Magic type `MagicType.PLANT`, grimoire book `book_plant`, painter `Aura.PLANT`, props `PropKind.PLANT_1`, VFX layer `PlantLayer`. Self-tests `magic.tree_rebuild_pages`, `magic.tree_rebuild_assets`, `magic.tree_rebuild_props` (registered once for all six nature magics); JUnit `TreeRulesTest`.

## NEW / REPLACED / KEPT

| Part | State | Notes |
|---|---|---|
| Temporary armour (Blossom armour) | NEW | No armour set existed for this magic. Added next to everything else. |
| Summon / construct models | NEW | plant_familiar, plant_bloom |
| Page ids, order, tiers, mastery bits | KEPT | Every old id stays at its old position; new pages are appended last (`TreeRulesTest` checks this). |
| Texts and numbers of the five generic pages | REPLACED | Guidepost and Dream World are remade; the two borrowed captain spells are untouched. |
| Page list | NEW | Pages appended: seed_bloom, healing_bloom, flower_sprite, ivy_barrier, blossom_dress, petal_storm |
| VFX | NEW | Three new shapes PLANT_FX1, PLANT_FX2, PLANT_FX3 (appended to `VfxShape`); the old `WIKI_MAGIC_*` shapes stay registered and are still used by the book for the cast circle. |
| Sprites | NEW | Eight coloured particle sprites `textures/particle/plant_*.png`. |
| Time Magic files, World Tree / Briar / Cherry Blossom / Fungus files | KEPT | Not touched. |

## Canon sources

- Black Clover: Plant Magic is the magic of flowers and growing things; its best-known user is Mimosa Vermillion of the Silver Eagles, whose spells grow healing flowers and fern-like fronds and vines. Her named spells are Magic Flower Guidepost (kept) and Dream World of a Hundred Flowers (kept, remade). No wiki page was reachable; the rest is built from that.
- Look: pink and white petals, golden flower hearts, fern leaves, soft pollen.
- The wiki was unreachable from the build sandbox. What is stated above as canon is only what was known without the page.

`book/PlantBook.java` was rewritten (it is this magic's own file): the first four page ids and tiers are exactly the old ones; the last two old pages (the borrowed captain spells) keep their position.

## Armour: Blossom armour (`Aura.PLANT`)

Eleven GeoLite pieces (`tools/gen_plant_rebuild_geo.py`) that drift in from above and the sides like falling petals. Delay 0 to 12 ticks per piece, forge-in over 8 ticks, shatter over the last 18 ticks, the same `EquipAnimation` as the Djinn Full Equip. First person hides the head pieces; the pieces drop detail at 24 and 56 blocks.

| Piece | Part | Delay (ticks) | Flies in | Model |
|---|---|---|---|---|
| helm | head | 0 | from above | flower crown base with leaf ribbons |
| crown | head | 12 | from above | halo of eight turning petals (glow, detail) |
| chest | body | 2 | from above | leaf bodice with a great golden-hearted blossom and a flaring petal skirt (glow) |
| back | body | 4 | from behind | two pairs of petal wings that beat |
| pauld_r / pauld_l | arms | 3 / 5 | from above and the sides | a half-open bloom on each shoulder |
| bracer_r / bracer_l | arms | 6 / 8 | from above | petal-cuffed leaf sleeves |
| greave_r / greave_l | legs | 9 / 11 | from above | wrapped greaves |
| aux | body | 12 | from above | six small blossoms drifting round the body (detail) |

Sprites: falling petals, rising pollen, a flower sigil on the ground (additive). Style colour 0xFFE690.

Render passes: skin overlay (cutout, translucent while fading), pieces (cutout, glow additive), skin glow (additive), sprites (additive). Softening 0.88 of every blow while it runs (10 s); each blow mends the wearer a little and the wearer regenerates.

Preview cost: about 6784 vertices for the full set in the preview (advisory budget 6000), above the advisory budget.

## Summon and construct models

| Model | Prop | Size | Clips | What it is |
|---|---|---|---|---|
| plant_familiar | PLANT_1 variant 0 | 17 bones, 28 cubes | idle, strike, heal | a flower sprite: a small body with petal wings and a bud head. Circles the caster 30 s, mends the most hurt friend (heal clip) and spits petals at foes (strike clip); can be hit. |
| plant_bloom | PLANT_1 variant 1 | 9 bones, 19 cubes | idle, open | a great bloom with a golden heart that opens (open clip); used by Healing Bloom, 8.5 s. |

GeoLite, box UV with an emissive `_glow` pass, checked by `tools/geo_check.py` and `GeoAssetsTest`. Painted by `client/prop/PlantPropPainter` (`TreePropPainter` helpers). Summons are bound to the caster, can be hit and expire; the variant is the low byte of the prop param, the phase bits 8-9, the clock bits 10-15.

## Spells

Damage numbers are the base values given to the grimoire's `hurt` (scaled by the book, mastery and EP). Control follows `BalanceLaw.controlTicks` (bosses are capped, hard controls share the 12 second lock).

| Page id | Name | State | Numbers |
|---|---|---|---|
| guidepost | Magic Flower Guidepost | KEPT id, remade | mid: flowers along the path, heals 20 percent of max health |
| hundred_flowers | Dream World of a Hundred Flowers | KEPT id, remade | signature: 8 s, friends 2 health per second, foes tangled and 2 damage per second |
| yggdrasil | World Tree Magic: Yggdrasil | KEPT (borrowed captain spell, untouched) | unchanged |
| briar_prison | Briar Magic: Briar Prison | KEPT (borrowed captain spell, untouched) | unchanged |
| seed_bloom | Seed Bloom | NEW | starter: a seed that blossoms, 7 damage and slow to foes, 3 health to friends in the blast |
| healing_bloom | Healing Bloom | NEW | mid: great bloom, heals friends about 8 s, slows foes |
| flower_sprite | Flower Sprite | NEW | signature: familiar 30 s, cooldown 50 s |
| ivy_barrier | Ivy Barrier | NEW | mid: wall of flowering leaves 12 s that dissolves bolts |
| blossom_dress | Blossom Dress | NEW | mid: Blossom armour 10 s, cooldown 15 s |
| petal_storm | Petal Storm | NEW | zone: 8 s, foes 2.5 damage and slowed, friends 1.5 health per second |

Lang: `tools/lang/plant_rebuild.json` (book description and a description for every page). Page names are literal strings in `book/PlantArts.java`.

## VFX

| Shape | Use | Look |
|---|---|---|
| PLANT_FX1 | cast | Seed Bloom: a flower mandala contracts on the caster, a golden seed flies on a thin green stem shedding petals in a spiral, a flower opens where it lands |
| PLANT_FX2 | zone | Hundred Flowers: the mandala turns on the ground, blossoms pop up over it one after another, petals and pollen rise, a soft golden ring pulses |
| PLANT_FX3 | impact | Great Bloom: a flash, a giant flower opens above the centre, a ring of petals runs out, petals fly and fall, pollen rises |

`vfx/client/layer/PlantLayer.java` on the shared blocks of `TreeFx` (ground rings, standing shapes, strands, wind, debris, flash). Each shape stays inside the 400-vertex budget (80 to 184 used). Preview: `python tools/vfx_preview/preview.py` with the scenes in `tools/vfx_preview/scenes.d/plant_rebuild.json`; armour and summons: `python tools/render_preview/preview.py` with `tools/render_preview/scenes.d/plant_rebuild.json`.

## Self-tests

- `magic.tree_rebuild_pages`: every magic resolves to its rebuilt book, whose page ids keep the old ids first in order, append the new ones, have no duplicate and stay at 24 family pages or fewer.
- `magic.tree_rebuild_assets`: every armour piece, summon model, skin and shape of the six magics resolves (66 armour models, 13 summon models, 12 skins, 18 shapes).
- `magic.tree_rebuild_props`: every variant of the six prop kinds is spawned in a scratch level and must come back with the right kind and variant byte.
- JUnit `TreeRulesTest` (6 tests): page order, limits, armour softening, param packing; a mutation check (swapped ids, changed packing) made it fail as it should.
- `GeoAssetsTest` (4 tests) covers every new model and animation.
- The smoke list of the lead should add the `magic` prefix to `SMOKE_EXPECT`.

## NOT VERIFIED

- Everything visual in the running game. The previews (`tools/render_preview`, `tools/vfx_preview`) are re-implementations of the render layers, so colours, depth sorting against real terrain, shaders and the first-person view are unchecked.
- The canon wiki could not be reached from the build sandbox (no network), so every spell is built from the canon descriptions that were known and from what the magic is, not from a page of the wiki. Names are in the canon idiom; where a name is invented it is an honest nearest version.
- The slim-arm fit of the arm pieces is only checked by arithmetic (the pieces squeeze about the inner edge to 0.75 for the slim model), not on a real slim skin.
- Prop behaviour (follow, strike, expiry, being hit) and spell balance (damage, durations, cooldowns) were never played; they follow the same helpers as the other books but the numbers are first guesses.
- The vertex count of the armour is above the advisory 6000 per player for some magics (see the armour section); it was measured in the preview, not on a frame time.
- `EquipAnimationTest` and `MagicEnumAuditTest` need Minecraft classes and could not run in the JUnit shim; `GeoAssetsTest` and `TreeRulesTest` were run and pass.
- Merge: the shared one-line registrations (Aura, PropKind, VfxShape, AuraRegistry, PropRegistry, PropPainterRegistry, VfxManager, WikiBooks) can conflict with other builders; they are listed in the lead report.

## 1.99.0 armour policy

Plant Magic has no armour in the anime, so since 1.99.0 **no cast puts the temporary armour of this document on the caster** (policy kind NONE). Everything described above as the armour (the render layer, its painter, geo models, textures and generator) is KEPT on disk and registered in `AuraRegistry`, so old saves and tests still resolve; it is simply never started by a page. What the pages did besides the armour (damage cut, thorns, resistance, constructs, effects) is unchanged. Where the armour carried the magic's look, every cast of a family page now sends a small `PLANT_FX2` glow that follows the caster instead (no armour piece is drawn). See `docs/rebuild/armour_policy.md` for the table and the canon reasons; the code is `book.ArmourPolicy` (data: `data/nusmp/armour_policy.json`).
